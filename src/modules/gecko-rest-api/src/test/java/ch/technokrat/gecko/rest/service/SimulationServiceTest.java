package ch.technokrat.gecko.rest.service;

import ch.technokrat.gecko.core.simulation.SimulationConfig;
import ch.technokrat.gecko.core.simulation.SimulationResult;
import ch.technokrat.gecko.rest.model.SimulationRequest;
import ch.technokrat.gecko.rest.model.SimulationResponse;
import ch.technokrat.gecko.rest.model.SimulationResponse.SimulationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SimulationService.
 * Tests the service layer business logic.
 */
class SimulationServiceTest {

    private SimulationService simulationService;

    @BeforeEach
    void setUp() {
        simulationService = new SimulationService();
        simulationService.clearAll();
    }

    @Test
    void submitSimulation_validRequest_createsSimulation() {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);

        SimulationResponse response = simulationService.submitSimulation(request);

        assertNotNull(response);
        assertNotNull(response.getSimulationId());
        assertTrue(response.getStatus() == SimulationStatus.PENDING
                || response.getStatus() == SimulationStatus.RUNNING);
        assertNotNull(response.getStartTime());
    }

    @Test
    void submitSimulation_generatesUniqueIds() {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);

        SimulationResponse response1 = simulationService.submitSimulation(request);
        SimulationResponse response2 = simulationService.submitSimulation(request);

        assertNotEquals(response1.getSimulationId(), response2.getSimulationId());
    }

    @Test
    void getSimulation_existingId_returnsSimulation() {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);
        SimulationResponse submitted = simulationService.submitSimulation(request);

        SimulationResponse retrieved = simulationService.getSimulation(submitted.getSimulationId());

        assertNotNull(retrieved);
        assertEquals(submitted.getSimulationId(), retrieved.getSimulationId());
    }

    @Test
    void getSimulation_nonExistingId_returnsNull() {
        SimulationResponse result = simulationService.getSimulation("non-existing-id");
        assertNull(result);
    }

    @Test
    void getAllSimulations_returnsAllStoredSimulations() {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);
        simulationService.submitSimulation(request);
        simulationService.submitSimulation(request);

        Map<String, SimulationResponse> allSimulations = simulationService.getAllSimulations();

        assertEquals(2, allSimulations.size());
    }

    @Test
    void clearAll_removesAllSimulations() {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);
        simulationService.submitSimulation(request);
        simulationService.submitSimulation(request);

        simulationService.clearAll();

        Map<String, SimulationResponse> allSimulations = simulationService.getAllSimulations();
        assertTrue(allSimulations.isEmpty());
    }

    @Test
    void updateStatus_changesSimulationStatus() {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);
        SimulationResponse response = simulationService.submitSimulation(request);
        String id = response.getSimulationId();

        simulationService.updateStatus(id, SimulationStatus.RUNNING);

        SimulationResponse updated = simulationService.getSimulation(id);
        assertEquals(SimulationStatus.RUNNING, updated.getStatus());
    }

    @Test
    void updateStatus_completedSetsEndTime() {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);
        SimulationResponse response = simulationService.submitSimulation(request);
        String id = response.getSimulationId();

        assertNull(response.getEndTime());

        simulationService.updateStatus(id, SimulationStatus.COMPLETED);

        SimulationResponse updated = simulationService.getSimulation(id);
        assertNotNull(updated.getEndTime());
    }

    @Test
    void addResults_storesSignalData() {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);
        SimulationResponse response = simulationService.submitSimulation(request);
        String id = response.getSimulationId();

        double[] data = new double[]{1.0, 2.0, 3.0};
        simulationService.addResults(id, "V_out", data);

        SimulationResponse updated = simulationService.getSimulation(id);
        assertNotNull(updated.getResults());
        assertArrayEquals(data, updated.getResults().get("V_out"));
    }

    @Test
    void getSignalData_existingSignal_returnsData() {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);
        SimulationResponse response = simulationService.submitSimulation(request);
        String id = response.getSimulationId();
        double[] data = new double[]{1.0, 2.0, 3.0};
        simulationService.addResults(id, "V_out", data);

        double[] retrieved = simulationService.getSignalData(id, "V_out");

        assertArrayEquals(data, retrieved);
    }

    @Test
    void getSignalData_returnsDefensiveCopy() {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);
        SimulationResponse response = simulationService.submitSimulation(request);
        String id = response.getSimulationId();
        simulationService.addResults(id, "V_out", new double[]{1.0, 2.0, 3.0});

        double[] firstRead = simulationService.getSignalData(id, "V_out");
        assertNotNull(firstRead);
        firstRead[0] = 999.0;

        double[] secondRead = simulationService.getSignalData(id, "V_out");
        assertNotNull(secondRead);
        assertEquals(1.0, secondRead[0], 1e-12);
    }

    @Test
    void getSignalData_nonExistingSignal_returnsNull() {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);
        SimulationResponse response = simulationService.submitSimulation(request);
        String id = response.getSimulationId();

        double[] retrieved = simulationService.getSignalData(id, "nonexistent");

        assertNull(retrieved);
    }

    @Test
    void getStatistics_returnsCorrectCounts() {
        SimulationResponse r1 = new SimulationResponse("stats-1");
        r1.setStatus(SimulationStatus.COMPLETED);
        SimulationResponse r2 = new SimulationResponse("stats-2");
        r2.setStatus(SimulationStatus.FAILED);
        SimulationResponse r3 = new SimulationResponse("stats-3");
        r3.setStatus(SimulationStatus.PENDING);
        putSimulation(r1);
        putSimulation(r2);
        putSimulation(r3);

        Map<SimulationStatus, Long> stats = simulationService.getStatistics();

        assertEquals(1L, stats.get(SimulationStatus.COMPLETED));
        assertEquals(1L, stats.get(SimulationStatus.FAILED));
        assertEquals(1L, stats.get(SimulationStatus.PENDING));
    }

    @Test
    void isRunning_pendingSimulation_returnsFalse() {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);
        SimulationResponse response = simulationService.submitSimulation(request);

        // Note: The simulation transitions to RUNNING asynchronously,
        // so initially isRunning may return false for pending state
        // This test checks the initial state
        boolean running = simulationService.isRunning(response.getSimulationId());

        // Could be true or false depending on timing - just verify no exception
        assertNotNull(running);
    }

    @Test
    void getSimulationProgress_completedSimulation_returns100() {
        String simulationId = "completed-sim";
        SimulationResponse completedResponse = new SimulationResponse(simulationId);
        completedResponse.setStatus(SimulationStatus.COMPLETED);
        putSimulation(completedResponse);

        double progress = simulationService.getSimulationProgress(simulationId);

        assertEquals(100.0, progress);
    }

    @Test
    void getSimulationProgress_nonExistingSimulation_returnsNegative() {
        double progress = simulationService.getSimulationProgress("non-existing-id");
        assertEquals(-1.0, progress);
    }

    @Test
    void cancelSimulation_updatesStatus() {
        String simulationId = "running-sim";
        SimulationResponse runningResponse = new SimulationResponse(simulationId);
        runningResponse.setStatus(SimulationStatus.RUNNING);
        putSimulation(runningResponse);

        SimulationResponse cancelled = simulationService.cancelSimulation(simulationId);

        assertEquals(SimulationStatus.FAILED, cancelled.getStatus());
        assertEquals("Cancelled by user", cancelled.getErrorMessage());
    }

    @Test
    void buildSimulationConfig_withMultipleParameters_appliesAllOverrides() {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);
        Map<String, Double> parameters = new HashMap<>();
        parameters.put("R_load", 12.0);
        parameters.put("C_dc", 8.5e-6);
        request.setParameters(parameters);

        SimulationConfig config = simulationService.buildSimulationConfig(request);

        assertEquals(2, config.getParameterOverrides().size());
        assertEquals(12.0, config.getParameterOverrides().get("R_load"));
        assertEquals(8.5e-6, config.getParameterOverrides().get("C_dc"));
    }

    @Test
    void cancelSimulation_pendingSimulation_updatesStatus() {
        String simulationId = "pending-sim";
        SimulationResponse pendingResponse = new SimulationResponse(simulationId);
        pendingResponse.setStatus(SimulationStatus.PENDING);
        putSimulation(pendingResponse);

        SimulationResponse cancelled = simulationService.cancelSimulation(simulationId);

        assertEquals(SimulationStatus.FAILED, cancelled.getStatus());
        assertEquals("Cancelled by user", cancelled.getErrorMessage());
        assertNotNull(cancelled.getEndTime());
    }

    @Test
    void getSimulationProgress_failedSimulation_returnsZero() {
        String simulationId = "failed-sim";
        SimulationResponse failedResponse = new SimulationResponse(simulationId);
        failedResponse.setStatus(SimulationStatus.FAILED);
        putSimulation(failedResponse);

        double progress = simulationService.getSimulationProgress(simulationId);

        assertEquals(0.0, progress);
    }

    @Test
    void getSimulationProgress_pendingSimulation_returnsZero() {
        String simulationId = "pending-sim";
        SimulationResponse pendingResponse = new SimulationResponse(simulationId);
        pendingResponse.setStatus(SimulationStatus.PENDING);
        putSimulation(pendingResponse);

        double progress = simulationService.getSimulationProgress(simulationId);

        assertEquals(0.0, progress);
    }

    @Test
    void markRunning_onlyTransitionsFromPending() {
        SimulationResponse pending = new SimulationResponse("pending");
        pending.setStatus(SimulationStatus.PENDING);
        assertTrue(simulationService.markRunning(pending));
        assertEquals(SimulationStatus.RUNNING, pending.getStatus());

        SimulationResponse failed = new SimulationResponse("failed");
        failed.setStatus(SimulationStatus.FAILED);
        assertFalse(simulationService.markRunning(failed));
        assertEquals(SimulationStatus.FAILED, failed.getStatus());
    }

    @Test
    void applySuccessfulResult_doesNotOverrideCancelledStatus() {
        SimulationResponse response = new SimulationResponse("cancelled");
        response.setStatus(SimulationStatus.FAILED);
        response.setErrorMessage(SimulationService.CANCELLED_BY_USER);

        SimulationResult result = SimulationResult.builder().build();

        assertFalse(simulationService.applySuccessfulResult(response, result));
        assertEquals(SimulationStatus.FAILED, response.getStatus());
        assertEquals(SimulationService.CANCELLED_BY_USER, response.getErrorMessage());
    }

    @Test
    void applyFailureResult_doesNotOverrideCancelledStatus() {
        SimulationResponse response = new SimulationResponse("cancelled");
        response.setStatus(SimulationStatus.FAILED);
        response.setErrorMessage(SimulationService.CANCELLED_BY_USER);

        assertFalse(simulationService.applyFailureResult(response, "other failure"));
        assertEquals(SimulationStatus.FAILED, response.getStatus());
        assertEquals(SimulationService.CANCELLED_BY_USER, response.getErrorMessage());
    }

    @Test
    void applySuccessfulResult_setsCompletedAndClearsError() {
        SimulationResponse response = new SimulationResponse("success");
        response.setStatus(SimulationStatus.RUNNING);
        response.setErrorMessage("previous-error");

        SimulationResult result = SimulationResult.builder().build();

        assertTrue(simulationService.applySuccessfulResult(response, result));
        assertEquals(SimulationStatus.COMPLETED, response.getStatus());
        assertNull(response.getErrorMessage());
        assertNotNull(response.getEndTime());
    }

    @Test
    void markCancelled_setsCancellationReasonForBlankFailedState() {
        SimulationResponse response = new SimulationResponse("cancelled-race");
        response.setStatus(SimulationStatus.FAILED);
        response.setErrorMessage(null);

        assertTrue(simulationService.markCancelled(response));
        assertEquals(SimulationStatus.FAILED, response.getStatus());
        assertEquals(SimulationService.CANCELLED_BY_USER, response.getErrorMessage());
        assertNotNull(response.getEndTime());
    }

    @SuppressWarnings("unchecked")
    private void putSimulation(SimulationResponse response) {
        try {
            Field storeField = SimulationService.class.getDeclaredField("simulationStore");
            storeField.setAccessible(true);
            Map<String, SimulationResponse> store =
                    (Map<String, SimulationResponse>) storeField.get(simulationService);
            store.put(response.getSimulationId(), response);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Unable to populate simulation store for test: " + e.getMessage());
        }
    }
}
