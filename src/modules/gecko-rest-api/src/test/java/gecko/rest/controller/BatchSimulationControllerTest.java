package gecko.rest.controller;

import gecko.rest.model.BatchSimulationRequest;
import gecko.rest.model.BatchSimulationResponse;
import gecko.rest.service.SimulationService;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for the batch simulation endpoint in SimulationController.
 * Uses MockMvc with a mocked SimulationService.
 */
@WebMvcTest(SimulationController.class)
@Import({GlobalExceptionHandler.class, gecko.rest.config.TestSecurityConfig.class})
class BatchSimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SimulationService simulationService;

    private BatchSimulationResponse batchResponse3;
    private BatchSimulationResponse batchResponse5;
    private BatchSimulationResponse batchResponse10;

    @BeforeEach
    void setUp() {
        List<String> ids3 = Arrays.asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
        batchResponse3 = new BatchSimulationResponse(UUID.randomUUID().toString(), ids3, 4);

        List<String> ids5 = Arrays.asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
        batchResponse5 = new BatchSimulationResponse(UUID.randomUUID().toString(), ids5, 4);

        List<String> ids10 = Arrays.asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );
        batchResponse10 = new BatchSimulationResponse(UUID.randomUUID().toString(), ids10, 2);
    }

    // --- Test 1: explicit parameterSets returns 201 ---

    @Test
    void submitBatch_withExplicitParameterSets_returnsCreated() throws Exception {
        BatchSimulationRequest request = buildRequestWithParameterSets("circuit.ipes", 3);

        when(simulationService.submitBatch(any(BatchSimulationRequest.class)))
                .thenReturn(batchResponse3);

        mockMvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    // --- Test 2: linearSweep returns 201 with correct count ---

    @Test
    void submitBatch_withLinearSweep_returnsCreatedWithCorrectCount() throws Exception {
        BatchSimulationRequest request = buildRequestWithLinearSweep("circuit.ipes", 5);

        when(simulationService.submitBatch(any(BatchSimulationRequest.class)))
                .thenReturn(batchResponse5);

        mockMvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalSimulations").value(5));
    }

    // --- Test 3: logSweep returns 201 with correct count ---

    @Test
    void submitBatch_withLogSweep_returnsCreatedWithCorrectCount() throws Exception {
        BatchSimulationRequest request = buildRequestWithLogSweep("circuit.ipes", 10);

        when(simulationService.submitBatch(any(BatchSimulationRequest.class)))
                .thenReturn(batchResponse10);

        mockMvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalSimulations").value(10));
    }

    // --- Test 4: missing circuitFile returns 400 ---

    @Test
    void submitBatch_missingCircuitFile_returnsBadRequest() throws Exception {
        BatchSimulationRequest request = buildRequestWithParameterSets(null, 2);

        mockMvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- Test 5: blank circuitFile returns 400 ---

    @Test
    void submitBatch_blankCircuitFile_returnsBadRequest() throws Exception {
        BatchSimulationRequest request = buildRequestWithParameterSets("   ", 2);

        mockMvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- Test 6: response contains batchId, totalSimulations, simulationIds ---

    @Test
    void submitBatch_responseContainsRequiredFields() throws Exception {
        BatchSimulationRequest request = buildRequestWithParameterSets("circuit.ipes", 3);

        when(simulationService.submitBatch(any(BatchSimulationRequest.class)))
                .thenReturn(batchResponse3);

        mockMvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchId").exists())
                .andExpect(jsonPath("$.totalSimulations").exists())
                .andExpect(jsonPath("$.simulationIds").isArray());
    }

    // --- Test 7: totalSimulations matches parameterSets size ---

    @Test
    void submitBatch_totalSimulationsMatchesParameterSetsSize() throws Exception {
        BatchSimulationRequest request = buildRequestWithParameterSets("circuit.ipes", 3);

        when(simulationService.submitBatch(any(BatchSimulationRequest.class)))
                .thenReturn(batchResponse3);

        mockMvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalSimulations").value(3))
                .andExpect(jsonPath("$.simulationIds.length()").value(3));
    }

    // --- Test 8: totalSimulations matches linearSweep points ---

    @Test
    void submitBatch_totalSimulationsMatchesLinearSweepPoints() throws Exception {
        BatchSimulationRequest request = buildRequestWithLinearSweep("circuit.ipes", 5);

        when(simulationService.submitBatch(any(BatchSimulationRequest.class)))
                .thenReturn(batchResponse5);

        mockMvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalSimulations").value(5))
                .andExpect(jsonPath("$.simulationIds.length()").value(5));
    }

    // --- Test 9: totalSimulations matches logSweep points ---

    @Test
    void submitBatch_totalSimulationsMatchesLogSweepPoints() throws Exception {
        BatchSimulationRequest request = buildRequestWithLogSweep("circuit.ipes", 10);

        when(simulationService.submitBatch(any(BatchSimulationRequest.class)))
                .thenReturn(batchResponse10);

        mockMvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalSimulations").value(10))
                .andExpect(jsonPath("$.simulationIds.length()").value(10));
    }

    // --- Test 10: maxConcurrent is reflected in response ---

    @Test
    void submitBatch_maxConcurrentReflectedInResponse() throws Exception {
        BatchSimulationRequest request = buildRequestWithLogSweep("circuit.ipes", 10);
        request.setMaxConcurrent(2);

        when(simulationService.submitBatch(any(BatchSimulationRequest.class)))
                .thenReturn(batchResponse10);

        mockMvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maxConcurrent").value(2));
    }

    // --- Test 11: submittedAt is not null ---

    @Test
    void submitBatch_submittedAtIsNotNull() throws Exception {
        BatchSimulationRequest request = buildRequestWithParameterSets("circuit.ipes", 3);

        when(simulationService.submitBatch(any(BatchSimulationRequest.class)))
                .thenReturn(batchResponse3);

        mockMvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.submittedAt").exists());
    }

    // --- Test 12: message is not null or blank ---

    @Test
    void submitBatch_messageIsNotNullOrBlank() throws Exception {
        BatchSimulationRequest request = buildRequestWithParameterSets("circuit.ipes", 3);

        when(simulationService.submitBatch(any(BatchSimulationRequest.class)))
                .thenReturn(batchResponse3);

        mockMvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // --- Test 13: batch with simulationSettings uses them ---

    @Test
    void submitBatch_withSimulationSettings_returnsCreated() throws Exception {
        BatchSimulationRequest request = buildRequestWithParameterSets("circuit.ipes", 3);
        BatchSimulationRequest.SimulationSettings settings = new BatchSimulationRequest.SimulationSettings();
        settings.setSimulationTime(0.01);
        settings.setTimeStep(1e-7);
        settings.setSolverType("trapezoidal");
        request.setSimulationSettings(settings);

        when(simulationService.submitBatch(any(BatchSimulationRequest.class)))
                .thenReturn(batchResponse3);

        mockMvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchId").exists());
    }

    // --- Helper methods ---

    private BatchSimulationRequest buildRequestWithParameterSets(String circuitFile, int count) {
        BatchSimulationRequest request = new BatchSimulationRequest();
        request.setCircuitFile(circuitFile);
        request.setMaxConcurrent(4);

        List<Map<String, Double>> parameterSets = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Double> params = new HashMap<>();
            params.put("R1.resistance", (double) (i + 1) * 10.0);
            parameterSets.add(params);
        }
        request.setParameterSets(parameterSets);
        return request;
    }

    private BatchSimulationRequest buildRequestWithLinearSweep(String circuitFile, int points) {
        BatchSimulationRequest request = new BatchSimulationRequest();
        request.setCircuitFile(circuitFile);
        request.setMaxConcurrent(4);

        BatchSimulationRequest.LinearSweep sweep = new BatchSimulationRequest.LinearSweep();
        sweep.setParameterPath("R1.resistance");
        sweep.setStartValue(1.0);
        sweep.setEndValue(100.0);
        sweep.setPoints(points);
        request.setLinearSweep(sweep);
        return request;
    }

    private BatchSimulationRequest buildRequestWithLogSweep(String circuitFile, int points) {
        BatchSimulationRequest request = new BatchSimulationRequest();
        request.setCircuitFile(circuitFile);
        request.setMaxConcurrent(2);

        BatchSimulationRequest.LogSweep sweep = new BatchSimulationRequest.LogSweep();
        sweep.setParameterPath("C1.capacitance");
        sweep.setStartValue(1e-9);
        sweep.setEndValue(1e-3);
        sweep.setPoints(points);
        request.setLogSweep(sweep);
        return request;
    }
}
