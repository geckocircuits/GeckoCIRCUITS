package gecko.rest.integration;

import gecko.rest.service.WebSocketProgressService;
import gecko.rest.model.SimulationProgressMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ArrayBlockingQueue;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.servlet.context-path=", "gecko.api.auth-enabled=true", "gecko.api.keys=socket-test-key,socket-other-key"
})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class WebSocketAuthenticationIntegrationTest {
    @LocalServerPort
    private int port;
    private WebSocketStompClient client;
    @Autowired
    private WebSocketProgressService progressService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired private org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired private gecko.rest.config.ApiKeyProperties properties;
    @Autowired private gecko.rest.service.SimulationService simulations;

    @BeforeEach
    void startClient() {
        client = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        client.setMessageConverter(converter);
    }

    @AfterEach
    void stopClient() {
        client.stop();
    }

    private StompSession connect(String key) throws Exception {
        StompHeaders headers = new StompHeaders();
        if (key != null) {
            headers.add("X-API-Key", key);
        }
        return client.connectAsync("ws://localhost:" + port + "/ws-raw",
                new WebSocketHttpHeaders(), headers, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
    }

    @Test
    void missingKeyCannotOpenAStompSession() {
        assertThrows(ExecutionException.class, () -> connect(null));
    }

    @Test
    void invalidKeyCannotOpenAStompSession() {
        assertThrows(ExecutionException.class, () -> connect("wrong-key"));
    }

    @Test
    void validKeyCanSubscribeAndReceiveProgress() throws Exception {
        String id = submit();
        assertTrue(simulations.isOwnedBy(id, gecko.rest.config.ApiIdentity.keyId("socket-test-key")));
        receiveProgress(connect("socket-test-key"), id);
    }

    private String submit() throws Exception {
        String body = objectMapper.writeValueAsString(new gecko.rest.model.SimulationRequest(
                "../gecko-simulation-core/src/test/resources/ipes/buck_simple.ipes", 0.002, 1e-5));
        var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/simulations").header("X-API-Key", "socket-test-key")
                .contentType("application/json").content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("simulationId").asText();
    }

    @Test void anotherKeyCannotSubscribeToTheOwnersTopic() throws Exception {
        String id = submit();
        StompSession session = connect("socket-other-key");
        session.subscribe("/topic/simulations/" + id, new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return SimulationProgressMessage.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                fail("A foreign client received simulation progress.");
            }
        });
        await().atMost(5, TimeUnit.SECONDS).until(() -> !session.isConnected());
    }

    @Test void batchChildrenKeepTheSubmittingOwnersIdentity() throws Exception {
        var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/simulations/batch").header("X-API-Key", "socket-test-key")
                .contentType("application/json").content("""
                    {"circuitFile":"../gecko-simulation-core/src/test/resources/ipes/buck_simple.ipes",
                     "parameterSets":[{},{}],
                     "simulationSettings":{"simulationTime":0.002,"timeStep":0.00001}}
                    """))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andReturn();
        var ids = objectMapper.readTree(result.getResponse().getContentAsString()).get("simulationIds");
        assertEquals(2, ids.size());
        for (var id : ids) {
            assertTrue(simulations.isOwnedBy(id.asText(), gecko.rest.config.ApiIdentity.keyId("socket-test-key")));
            assertFalse(simulations.isOwnedBy(id.asText(), gecko.rest.config.ApiIdentity.keyId("socket-other-key")));
        }
    }

    @Test void disabledAuthenticationAllowsAnotherKeyToSubscribe() throws Exception {
        String id = submit();
        properties.setAuthEnabled(false);
        try {
            receiveProgress(connect("socket-other-key"), id);
        } finally {
            properties.setAuthEnabled(true);
        }
    }

    private void receiveProgress(StompSession session, String id) throws Exception {
        assertTrue(session.isConnected());
        ArrayBlockingQueue<SimulationProgressMessage> frames = new ArrayBlockingQueue<>(10);
        session.subscribe("/topic/simulations/" + id, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return SimulationProgressMessage.class;
            }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                frames.offer((SimulationProgressMessage) payload);
            }
        });
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            progressService.broadcastProgress(id, 50, 0.5, 1, 5, 10, "RUNNING");
            assertTrue(session.isConnected());
            SimulationProgressMessage frame = frames.poll();
            assertNotNull(frame);
            assertEquals(id, frame.getSimulationId());
        });
        session.disconnect();
    }
}
