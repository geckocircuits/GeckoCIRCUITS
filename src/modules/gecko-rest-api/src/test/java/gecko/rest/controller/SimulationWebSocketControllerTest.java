package gecko.rest.controller;

import gecko.rest.service.WebSocketProgressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SimulationWebSocketController.class)
@Import(gecko.rest.config.TestSecurityConfig.class)
class SimulationWebSocketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebSocketProgressService webSocketProgressService;

    @Test
    void testGetWebSocketInfoReturns200() throws Exception {
        String simulationId = "test-sim-123";
        when(webSocketProgressService.getTopicDestination(simulationId))
                .thenReturn("/topic/simulations/" + simulationId);

        mockMvc.perform(get("/api/v1/simulations/{simulationId}/ws-info", simulationId))
                .andExpect(status().isOk());
    }

    @Test
    void testResponseContainsStompEndpoint() throws Exception {
        String simulationId = "test-sim-456";
        when(webSocketProgressService.getTopicDestination(anyString()))
                .thenReturn("/topic/simulations/" + simulationId);

        mockMvc.perform(get("/api/v1/simulations/{simulationId}/ws-info", simulationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stompEndpoint", equalTo("/ws")));
    }

    @Test
    void testResponseContainsSubscribeDestinationWithSimulationId() throws Exception {
        String simulationId = "test-sim-789";
        String expectedDestination = "/topic/simulations/" + simulationId;
        when(webSocketProgressService.getTopicDestination(simulationId))
                .thenReturn(expectedDestination);

        mockMvc.perform(get("/api/v1/simulations/{simulationId}/ws-info", simulationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribeDestination", equalTo(expectedDestination)));
    }

    @Test
    void testResponseContainsRawWebSocketEndpoint() throws Exception {
        String simulationId = "test-sim-raw";
        when(webSocketProgressService.getTopicDestination(anyString()))
                .thenReturn("/topic/simulations/" + simulationId);

        mockMvc.perform(get("/api/v1/simulations/{simulationId}/ws-info", simulationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawWebSocketEndpoint", equalTo("/ws-raw")));
    }

    @Test
    void testResponseContainsSimulationIdField() throws Exception {
        String simulationId = "test-sim-field";
        when(webSocketProgressService.getTopicDestination(anyString()))
                .thenReturn("/topic/simulations/" + simulationId);

        mockMvc.perform(get("/api/v1/simulations/{simulationId}/ws-info", simulationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId", equalTo(simulationId)));
    }
}
