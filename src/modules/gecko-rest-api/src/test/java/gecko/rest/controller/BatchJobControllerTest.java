package gecko.rest.controller;

import gecko.rest.model.BatchJobStatus;
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
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for batch job tracking endpoints.
 * Tests getBatchStatus and cancelBatch endpoints.
 */
@WebMvcTest(SimulationController.class)
@Import(GlobalExceptionHandler.class)
class BatchJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SimulationService simulationService;

    private String testBatchId;
    private BatchJobStatus testStatus;
    private BatchSimulationResponse testBatch;

    @BeforeEach
    void setUp() {
        testBatchId = UUID.randomUUID().toString();
        
        // Create test batch
        testBatch = new BatchSimulationResponse(
            testBatchId,
            Arrays.asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
            ),
            4
        );

        // Create test status
        testStatus = new BatchJobStatus();
        testStatus.setBatchId(testBatchId);
        testStatus.setTotal(3);
        testStatus.setCompleted(2);
        testStatus.setFailed(0);
        testStatus.setRunning(1);
        testStatus.setPending(0);
        testStatus.setOverallProgress(66.67);
        testStatus.setDone(false);
        testStatus.setSubmittedAt(Instant.now());
        
        Map<String, String> statuses = new HashMap<>();
        statuses.put(testBatch.getSimulationIds().get(0), "COMPLETED");
        statuses.put(testBatch.getSimulationIds().get(1), "COMPLETED");
        statuses.put(testBatch.getSimulationIds().get(2), "RUNNING");
        testStatus.setSimulationStatuses(statuses);
    }

    @Test
    void getBatchStatus_validBatchId_returns200() throws Exception {
        when(simulationService.getBatchStatus(testBatchId))
            .thenReturn(testStatus);

        mockMvc.perform(get("/api/v1/simulations/batch/{batchId}", testBatchId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value(testBatchId));
    }

    @Test
    void getBatchStatus_unknownBatchId_returns404() throws Exception {
        when(simulationService.getBatchStatus("unknown-batch"))
            .thenReturn(null);

        mockMvc.perform(get("/api/v1/simulations/batch/{batchId}", "unknown-batch")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelBatch_validBatchId_returns204() throws Exception {
        when(simulationService.getBatch(testBatchId))
            .thenReturn(testBatch);
        when(simulationService.cancelBatch(testBatchId))
            .thenReturn(true);

        mockMvc.perform(delete("/api/v1/simulations/batch/{batchId}", testBatchId))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelBatch_unknownBatchId_returns404() throws Exception {
        when(simulationService.getBatch("unknown-batch"))
            .thenReturn(null);

        mockMvc.perform(delete("/api/v1/simulations/batch/{batchId}", "unknown-batch"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getBatchStatus_hasBatchIdField() throws Exception {
        when(simulationService.getBatchStatus(testBatchId))
            .thenReturn(testStatus);

        mockMvc.perform(get("/api/v1/simulations/batch/{batchId}", testBatchId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").exists())
                .andExpect(jsonPath("$.batchId").value(testBatchId));
    }

    @Test
    void getBatchStatus_hasTotalField() throws Exception {
        when(simulationService.getBatchStatus(testBatchId))
            .thenReturn(testStatus);

        mockMvc.perform(get("/api/v1/simulations/batch/{batchId}", testBatchId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").exists())
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void getBatchStatus_hasCompletedField() throws Exception {
        when(simulationService.getBatchStatus(testBatchId))
            .thenReturn(testStatus);

        mockMvc.perform(get("/api/v1/simulations/batch/{batchId}", testBatchId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").exists())
                .andExpect(jsonPath("$.completed").value(2));
    }

    @Test
    void getBatchStatus_hasOverallProgressField() throws Exception {
        when(simulationService.getBatchStatus(testBatchId))
            .thenReturn(testStatus);

        mockMvc.perform(get("/api/v1/simulations/batch/{batchId}", testBatchId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallProgress").exists())
                .andExpect(jsonPath("$.overallProgress", closeTo(66.67, 0.01)));
    }

    @Test
    void getBatchStatus_hasDoneField() throws Exception {
        when(simulationService.getBatchStatus(testBatchId))
            .thenReturn(testStatus);

        mockMvc.perform(get("/api/v1/simulations/batch/{batchId}", testBatchId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").exists())
                .andExpect(jsonPath("$.done").value(false));
    }

    @Test
    void getBatchStatus_whenDone_overallProgressIs100() throws Exception {
        testStatus.setDone(true);
        testStatus.setCompleted(3);
        testStatus.setRunning(0);
        testStatus.setOverallProgress(100.0);
        
        when(simulationService.getBatchStatus(testBatchId))
            .thenReturn(testStatus);

        mockMvc.perform(get("/api/v1/simulations/batch/{batchId}", testBatchId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true))
                .andExpect(jsonPath("$.overallProgress").value(100.0));
    }

    @Test
    void getBatchStatus_includesSimulationStatuses() throws Exception {
        when(simulationService.getBatchStatus(testBatchId))
            .thenReturn(testStatus);

        mockMvc.perform(get("/api/v1/simulations/batch/{batchId}", testBatchId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationStatuses").exists())
                .andExpect(jsonPath("$.simulationStatuses", aMapWithSize(3)));
    }

}
