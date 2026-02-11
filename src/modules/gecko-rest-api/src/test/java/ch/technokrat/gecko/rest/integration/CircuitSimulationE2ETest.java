package ch.technokrat.gecko.rest.integration;

import ch.technokrat.gecko.rest.model.SimulationRequest;
import ch.technokrat.gecko.rest.model.SimulationResponse;
import ch.technokrat.gecko.rest.service.SimulationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for circuit simulation workflows.
 * Tests realistic circuit simulation scenarios through the REST API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Circuit Simulation E2E Tests")
class CircuitSimulationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SimulationService simulationService;

    @BeforeEach
    void setUp() {
        simulationService.clearAll();
    }

    @Nested
    @DisplayName("RC Circuit Simulations")
    class RCCircuitTests {

        @Test
        @DisplayName("RC circuit charging simulation accepts parameters")
        void rcCircuitChargingSimulation() throws Exception {
            SimulationRequest request = new SimulationRequest("rc_charging.ipes", 0.01, 1e-6);
            request.setParameters(Map.of(
                "R1", 1000.0,    // 1kΩ resistor
                "C1", 1e-6,     // 1µF capacitor
                "Vs", 10.0      // 10V source
            ));

            MvcResult result = mockMvc.perform(post("/api/v1/simulations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.simulationId").exists())
                    .andReturn();

            String response = result.getResponse().getContentAsString();
            SimulationResponse simResponse = objectMapper.readValue(response, SimulationResponse.class);
            assertNotNull(simResponse.getSimulationId());
        }

        @Test
        @DisplayName("RC circuit with different time constants")
        void rcCircuitDifferentTimeConstants() throws Exception {
            // Small time constant (fast response)
            SimulationRequest fastRC = new SimulationRequest("rc.ipes", 0.001, 1e-7);
            fastRC.setParameters(Map.of("R", 100.0, "C", 1e-9));

            mockMvc.perform(post("/api/v1/simulations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(fastRC)))
                    .andExpect(status().isCreated());

            // Large time constant (slow response)
            SimulationRequest slowRC = new SimulationRequest("rc.ipes", 1.0, 1e-4);
            slowRC.setParameters(Map.of("R", 10000.0, "C", 1e-3));

            mockMvc.perform(post("/api/v1/simulations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(slowRC)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("Simulation Cancellation")
    class CancellationTests {

        @Test
        @DisplayName("Can cancel running simulation")
        void cancelRunningSimulation() throws Exception {
            // Submit long simulation
            SimulationRequest request = new SimulationRequest("complex.ipes", 10.0, 1e-6);

            MvcResult submitResult = mockMvc.perform(post("/api/v1/simulations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            SimulationResponse response = objectMapper.readValue(
                    submitResult.getResponse().getContentAsString(), SimulationResponse.class);

            // Brief wait then cancel
            TimeUnit.MILLISECONDS.sleep(50);

            mockMvc.perform(delete("/api/v1/simulations/{id}", response.getSimulationId()))
                    .andExpect(status().isOk());

            // Verify cancelled state
            mockMvc.perform(get("/api/v1/simulations/{id}", response.getSimulationId()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Cancel non-existent simulation returns 404")
        void cancelNonExistent() throws Exception {
            mockMvc.perform(delete("/api/v1/simulations/{id}", "fake-id-12345"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Progress Reporting")
    class ProgressTests {

        @Test
        @DisplayName("Progress endpoint returns valid data")
        void progressEndpointWorks() throws Exception {
            SimulationRequest request = new SimulationRequest("test.ipes", 0.1, 1e-6);

            MvcResult submitResult = mockMvc.perform(post("/api/v1/simulations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            SimulationResponse response = objectMapper.readValue(
                    submitResult.getResponse().getContentAsString(), SimulationResponse.class);

            // Check progress
            mockMvc.perform(get("/api/v1/simulations/{id}/progress", response.getSimulationId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.simulationId").value(response.getSimulationId()));
        }

        @Test
        @DisplayName("Progress for non-existent simulation returns 404")
        void progressNonExistent() throws Exception {
            mockMvc.perform(get("/api/v1/simulations/{id}/progress", "non-existent"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Batch Simulations")
    class BatchTests {

        @Test
        @DisplayName("Submit multiple simulations")
        void submitMultipleSimulations() throws Exception {
            for (int i = 0; i < 3; i++) {
                SimulationRequest request = new SimulationRequest(
                        "circuit" + i + ".ipes", 0.001, 1e-6);

                mockMvc.perform(post("/api/v1/simulations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated());
            }

            // List all
            mockMvc.perform(get("/api/v1/simulations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(3));
        }
    }

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("Invalid time step rejected")
        void invalidTimeStepRejected() throws Exception {
            String invalidRequest = "{\"circuitFile\":\"test.ipes\",\"simulationTime\":0.01,\"timeStep\":-1e-6}";

            mockMvc.perform(post("/api/v1/simulations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Zero simulation time rejected")
        void zeroSimulationTimeRejected() throws Exception {
            SimulationRequest request = new SimulationRequest("test.ipes", 0.0, 1e-6);

            mockMvc.perform(post("/api/v1/simulations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Empty circuit file rejected")
        void emptyCircuitFileRejected() throws Exception {
            SimulationRequest request = new SimulationRequest("", 0.01, 1e-6);

            mockMvc.perform(post("/api/v1/simulations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
