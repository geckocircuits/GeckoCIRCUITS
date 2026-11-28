package gecko.rest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.rest.model.SimulationProgressMessage;
import gecko.rest.model.SimulationRequest;
import gecko.rest.model.SimulationResponse.SimulationStatus;
import java.lang.reflect.Type;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.servlet.context-path=", "gecko.api.auth-enabled=false"
})
class StompProgressIntegrationTest {
    @LocalServerPort private int port;
    @Autowired private ObjectMapper mapper;
    @Autowired private WebSocketProgressService webSocket;
    private final ArrayBlockingQueue<SimulationProgressMessage> frames = new ArrayBlockingQueue<>(16);
    private final CountDownLatch releaseWorker = new CountDownLatch(1);
    private ExecutorService workers;
    private SimulationService simulations;
    private WebSocketStompClient client;
    private StompSession session;
    private String id;

    @BeforeEach
    void connectToPendingJob() throws Exception {
        workers = Executors.newSingleThreadExecutor();
        workers.submit(() -> {
            try { releaseWorker.await(); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        });
        simulations = new SimulationService(workers, HeadlessSimulationEngine::new, SseEmitter::new, webSocket);
        id = simulations.submitSimulation(new SimulationRequest(
                "../gecko-simulation-core/src/test/resources/ipes/buck_simple.ipes", 0.02, 1e-6)).getSimulationId();
        client = new WebSocketStompClient(new StandardWebSocketClient());
        var converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(mapper);
        client.setMessageConverter(converter);
        session = client.connectAsync("ws://localhost:" + port + "/ws-raw", new WebSocketHttpHeaders(),
                new StompHeaders(), new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
        session.subscribe("/topic/simulations/" + id, new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return SimulationProgressMessage.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                frames.offer((SimulationProgressMessage) payload);
            }
        });
    }

    @AfterEach
    void closeJobAndClient() throws InterruptedException {
        if (session != null && session.isConnected()) session.disconnect();
        if (client != null) client.stop();
        if (simulations != null) simulations.shutdown();
        releaseWorker.countDown();
        if (workers != null) {
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @ParameterizedTest
    @CsvSource({"0.0, 0.0", "0.005, 0.5", "0.25, 25.0", "0.5, 50.0", "0.999, 99.9", "1.0, 100.0"})
    void jobProgressReachesStompSubscribersAsAPercentage(double ratio, double expectedPercentage) {
        SimulationProgressMessage frame = receiveProgress(ratio);
        assertEquals(expectedPercentage, frame.getProgress(), 1e-12);
        assertEquals(id, frame.getSimulationId());
        assertEquals(ratio * 10.0, frame.getCurrentTime());
        assertEquals(10.0, frame.getEndTime());
        assertEquals(0, frame.getStep());
        assertEquals(0, frame.getTotalSteps());
        assertEquals("RUNNING", frame.getStatus());
        assertNotNull(frame.getTimestamp());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void terminalFramesRetainPercentageUnits(boolean success) {
        receiveProgress(0.5);
        if (success) simulations.updateStatus(id, SimulationStatus.COMPLETED);
        else simulations.cancelSimulation(id);
        String status = success ? "COMPLETED" : "FAILED";
        var received = new AtomicReference<SimulationProgressMessage>();
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            SimulationProgressMessage next = frames.poll();
            if (next != null && status.equals(next.getStatus())) received.set(next);
            return received.get() != null;
        });
        SimulationProgressMessage frame = received.get();
        assertEquals(success ? 100.0 : 0.0, frame.getProgress());
        assertEquals(id, frame.getSimulationId());
        assertEquals(status, frame.getStatus());
        if (success) assertNull(frame.getErrorMessage());
        else assertEquals("Cancelled by user", frame.getErrorMessage());
    }

    private SimulationProgressMessage receiveProgress(double ratio) {
        var received = new AtomicReference<SimulationProgressMessage>();
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            SimulationProgressMessage next = frames.poll();
            if (next != null) {
                received.set(next);
                return true;
            }
            simulations.broadcastProgress(id, ratio, ratio * 10.0, 10.0);
            return false;
        });
        return received.get();
    }
}
