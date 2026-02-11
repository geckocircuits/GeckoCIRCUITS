package ch.technokrat.gecko.rest.controller;

import ch.technokrat.gecko.rest.model.SimulationRequest;
import ch.technokrat.gecko.rest.model.SimulationResponse;
import ch.technokrat.gecko.rest.service.SimulationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SimulationController endpoints.
 * Uses MockMvc with a mocked SimulationService.
 */
@WebMvcTest(SimulationController.class)
@Import(GlobalExceptionHandler.class)
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SimulationService simulationService;

    private String testSimulationId;
    private SimulationResponse testResponse;

    @BeforeEach
    void setUp() {
        testSimulationId = UUID.randomUUID().toString();
        testResponse = new SimulationResponse(testSimulationId);
        testResponse.setStatus(SimulationResponse.SimulationStatus.PENDING);
        testResponse.setStartTime(Instant.now());
    }

    @Test
    void submitSimulation_validRequest_returnsCreated() throws Exception {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);

        when(simulationService.submitSimulation(any(SimulationRequest.class)))
                .thenReturn(testResponse);

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.simulationId").value(testSimulationId))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void submitSimulation_missingCircuitFile_returnsBadRequest() throws Exception {
        SimulationRequest request = new SimulationRequest();
        request.setSimulationTime(0.02);
        request.setTimeStep(1e-6);

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitSimulation_negativeTimeStep_returnsBadRequest() throws Exception {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, -1e-6);

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSimulation_existingId_returnsOk() throws Exception {
        when(simulationService.getSimulation(testSimulationId))
                .thenReturn(testResponse);

        mockMvc.perform(get("/api/v1/simulations/{id}", testSimulationId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").value(testSimulationId));
    }

    @Test
    void getSimulation_nonExistingId_returnsNotFound() throws Exception {
        when(simulationService.getSimulation("non-existing-id"))
                .thenReturn(null);

        mockMvc.perform(get("/api/v1/simulations/{id}", "non-existing-id")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSimulationResults_completedSimulation_returnsResults() throws Exception {
        testResponse.setStatus(SimulationResponse.SimulationStatus.COMPLETED);
        testResponse.addResult("V_out", new double[]{1.0, 2.0, 3.0});
        testResponse.addResult("time", new double[]{0.0, 1e-6, 2e-6});

        when(simulationService.getSimulation(testSimulationId))
                .thenReturn(testResponse);

        mockMvc.perform(get("/api/v1/simulations/{id}/results", testSimulationId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.V_out").isArray())
                .andExpect(jsonPath("$.time").isArray());
    }

    @Test
    void getSimulationResults_pendingSimulation_returnsTooEarly() throws Exception {
        testResponse.setStatus(SimulationResponse.SimulationStatus.PENDING);

        when(simulationService.getSimulation(testSimulationId))
                .thenReturn(testResponse);

        mockMvc.perform(get("/api/v1/simulations/{id}/results", testSimulationId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooEarly());
    }

    @Test
    void getSignalData_existingSignal_returnsData() throws Exception {
        testResponse.setStatus(SimulationResponse.SimulationStatus.COMPLETED);
        double[] signalData = new double[]{1.0, 2.0, 3.0, 4.0, 5.0};

        when(simulationService.getSimulation(testSimulationId))
                .thenReturn(testResponse);
        when(simulationService.getSignalData(testSimulationId, "V_out"))
                .thenReturn(signalData);

        mockMvc.perform(get("/api/v1/simulations/{id}/results/{signal}", testSimulationId, "V_out")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signalName").value("V_out"))
                .andExpect(jsonPath("$.dataPoints").value(5))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getSignalData_nonExistingSignal_returnsNotFound() throws Exception {
        testResponse.setStatus(SimulationResponse.SimulationStatus.COMPLETED);

        when(simulationService.getSimulation(testSimulationId))
                .thenReturn(testResponse);
        when(simulationService.getSignalData(testSimulationId, "nonexistent"))
                .thenReturn(null);

        mockMvc.perform(get("/api/v1/simulations/{id}/results/{signal}", testSimulationId, "nonexistent")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSignalData_nonCompletedSimulation_returnsTooEarly() throws Exception {
        testResponse.setStatus(SimulationResponse.SimulationStatus.RUNNING);

        when(simulationService.getSimulation(testSimulationId))
                .thenReturn(testResponse);

        mockMvc.perform(get("/api/v1/simulations/{id}/results/{signal}", testSimulationId, "V_out")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooEarly());
    }

    @Test
    void getSimulationProgress_runningSimulation_returnsProgress() throws Exception {
        testResponse.setStatus(SimulationResponse.SimulationStatus.RUNNING);

        when(simulationService.getSimulation(testSimulationId))
                .thenReturn(testResponse);
        when(simulationService.getSimulationProgress(testSimulationId))
                .thenReturn(50.0);
        when(simulationService.isRunning(testSimulationId))
                .thenReturn(true);

        mockMvc.perform(get("/api/v1/simulations/{id}/progress", testSimulationId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationId").value(testSimulationId))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.progress").value(50.0))
                .andExpect(jsonPath("$.isRunning").value(true));
    }

    @Test
    void listSimulations_returnsAllSimulations() throws Exception {
        Map<String, SimulationResponse> allSimulations = new HashMap<>();
        allSimulations.put(testSimulationId, testResponse);

        Map<SimulationResponse.SimulationStatus, Long> stats = new HashMap<>();
        stats.put(SimulationResponse.SimulationStatus.PENDING, 1L);
        stats.put(SimulationResponse.SimulationStatus.COMPLETED, 0L);

        when(simulationService.getAllSimulations())
                .thenReturn(allSimulations);
        when(simulationService.getStatistics())
                .thenReturn(stats);

        mockMvc.perform(get("/api/v1/simulations")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.simulations").exists())
                .andExpect(jsonPath("$.statistics").exists());
    }

    @Test
    void listSimulations_withStatusFilter_returnsFilteredResults() throws Exception {
        Map<String, SimulationResponse> allSimulations = new HashMap<>();
        allSimulations.put(testSimulationId, testResponse);

        Map<SimulationResponse.SimulationStatus, Long> stats = new HashMap<>();
        stats.put(SimulationResponse.SimulationStatus.PENDING, 1L);

        when(simulationService.getAllSimulations())
                .thenReturn(allSimulations);
        when(simulationService.getStatistics())
                .thenReturn(stats);

        mockMvc.perform(get("/api/v1/simulations")
                        .param("status", "PENDING")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void cancelSimulation_runningSimulation_returnsOk() throws Exception {
        testResponse.setStatus(SimulationResponse.SimulationStatus.RUNNING);
        SimulationResponse cancelledResponse = new SimulationResponse(testSimulationId);
        cancelledResponse.setStatus(SimulationResponse.SimulationStatus.FAILED);
        cancelledResponse.setErrorMessage("Cancelled by user");

        when(simulationService.getSimulation(testSimulationId))
                .thenReturn(testResponse);
        when(simulationService.cancelSimulation(testSimulationId))
                .thenReturn(cancelledResponse);

        mockMvc.perform(delete("/api/v1/simulations/{id}", testSimulationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void cancelSimulation_pendingSimulation_returnsOk() throws Exception {
        testResponse.setStatus(SimulationResponse.SimulationStatus.PENDING);
        SimulationResponse cancelledResponse = new SimulationResponse(testSimulationId);
        cancelledResponse.setStatus(SimulationResponse.SimulationStatus.FAILED);
        cancelledResponse.setErrorMessage("Cancelled by user");

        when(simulationService.getSimulation(testSimulationId))
                .thenReturn(testResponse);
        when(simulationService.cancelSimulation(testSimulationId))
                .thenReturn(cancelledResponse);

        mockMvc.perform(delete("/api/v1/simulations/{id}", testSimulationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("Cancelled by user"));
    }

    @Test
    void cancelSimulation_completedSimulation_returnsConflict() throws Exception {
        testResponse.setStatus(SimulationResponse.SimulationStatus.COMPLETED);

        when(simulationService.getSimulation(testSimulationId))
                .thenReturn(testResponse);

        mockMvc.perform(delete("/api/v1/simulations/{id}", testSimulationId))
                .andExpect(status().isConflict());
    }

    @Test
    void exportResults_completedSimulation_returnsCsv() throws Exception {
        testResponse.setStatus(SimulationResponse.SimulationStatus.COMPLETED);
        testResponse.addResult("time", new double[]{0.0, 1e-6, 2e-6});
        testResponse.addResult("V_out", new double[]{0.0, 1.0, 2.0});

        when(simulationService.getSimulation(testSimulationId))
                .thenReturn(testResponse);

        mockMvc.perform(post("/api/v1/simulations/{id}/export", testSimulationId)
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"simulation_" + testSimulationId + ".csv\""));
    }

    @Test
    void exportResults_completedSimulation_ordersAndEscapesCsvHeaders() throws Exception {
        testResponse.setStatus(SimulationResponse.SimulationStatus.COMPLETED);
        Map<String, double[]> results = new LinkedHashMap<>();
        results.put("sig,1", new double[]{3.0, 4.0});
        results.put("time", new double[]{0.0, 1.0});
        results.put("sig\"2", new double[]{5.0});
        testResponse.setResults(results);

        when(simulationService.getSimulation(testSimulationId))
                .thenReturn(testResponse);

        String expectedCsv = "time,\"sig\"\"2\",\"sig,1\"\n"
                + "0.0,5.0,3.0\n"
                + "1.0,,4.0\n";

        mockMvc.perform(post("/api/v1/simulations/{id}/export", testSimulationId)
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedCsv));
    }

    @Test
    void exportResults_invalidFormat_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/simulations/{id}/export", testSimulationId)
                        .param("format", "json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSimulation_internalException_hidesExceptionDetails() throws Exception {
        String internalMessage = "db connection string leaked";
        when(simulationService.getSimulation(testSimulationId))
                .thenThrow(new RuntimeException(internalMessage));

        mockMvc.perform(get("/api/v1/simulations/{id}", testSimulationId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString(internalMessage))));
    }
}
