package ch.technokrat.gecko.rest.integration;

import ch.technokrat.gecko.rest.model.SimulationRequest;
import ch.technokrat.gecko.rest.model.SimulationResponse;
import ch.technokrat.gecko.rest.service.SimulationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for the simulation REST API.
 * Tests the full request/response flow through all layers.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SimulationE2ETest {

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

    @Test
    void healthEndpoint_isAccessible() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void fullSimulationWorkflow_submitAndRetrieve() throws Exception {
        // 1. Submit a simulation
        SimulationRequest request = new SimulationRequest("test.ipes", 0.001, 1e-6);

        MvcResult submitResult = mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.simulationId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        // Extract simulation ID
        String responseJson = submitResult.getResponse().getContentAsString();
        SimulationResponse submitResponse = objectMapper.readValue(responseJson, SimulationResponse.class);
        String simulationId = submitResponse.getSimulationId();

        assertNotNull(simulationId);

        // 2. Check simulation status
        mockMvc.perform(get("/api/v1/simulations/{id}", simulationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").value(simulationId));

        // 3. Wait for simulation to complete (short simulation)
        TimeUnit.MILLISECONDS.sleep(500);

        // 4. Check progress
        mockMvc.perform(get("/api/v1/simulations/{id}/progress", simulationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").value(simulationId));

        // 5. List all simulations
        mockMvc.perform(get("/api/v1/simulations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.simulations").exists());
    }

    @Test
    void submitSimulation_withParameters_acceptsRequest() throws Exception {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.01, 1e-6);
        request.setParameters(java.util.Map.of(
                "R1", 100.0,
                "C1", 1e-6
        ));

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.simulationId").exists());
    }

    @Test
    void validationErrors_returnBadRequest() throws Exception {
        // Missing required fields
        String invalidJson = "{}";

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonExistentSimulation_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/simulations/{id}", "non-existent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelSimulation_workflow() throws Exception {
        // Submit a longer simulation
        SimulationRequest request = new SimulationRequest("test.ipes", 1.0, 1e-6);

        MvcResult submitResult = mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = submitResult.getResponse().getContentAsString();
        SimulationResponse response = objectMapper.readValue(responseJson, SimulationResponse.class);
        String simulationId = response.getSimulationId();

        // Brief wait to let simulation start
        TimeUnit.MILLISECONDS.sleep(100);

        // Cancel the simulation
        mockMvc.perform(delete("/api/v1/simulations/{id}", simulationId))
                .andExpect(status().isOk());
    }

    @Test
    void listSimulations_withStatusFilter() throws Exception {
        // Submit multiple simulations
        SimulationRequest request = new SimulationRequest("test.ipes", 0.001, 1e-6);

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // List with status filter
        mockMvc.perform(get("/api/v1/simulations")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulations").exists());
    }

    @Test
    void apiDocumentation_isAccessible() throws Exception {
        mockMvc.perform(get("/api/docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.swagger-ui").exists())
                .andExpect(jsonPath("$.api-docs").exists());
    }

    @Test
    void apiInfo_returnsMetadata() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("GeckoCIRCUITS REST API"))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.status").value("running"));
    }
}
