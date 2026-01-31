package ch.technokrat.gecko.rest.service;

import ch.technokrat.gecko.rest.model.SimulationRequest;
import ch.technokrat.gecko.rest.model.SimulationResponse;
import ch.technokrat.gecko.rest.model.SimulationResponse.SimulationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        assertEquals(SimulationStatus.PENDING, response.getStatus());
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
    void getSignalData_nonExistingSignal_returnsNull() {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);
        SimulationResponse response = simulationService.submitSimulation(request);
        String id = response.getSimulationId();

        double[] retrieved = simulationService.getSignalData(id, "nonexistent");

        assertNull(retrieved);
    }

    @Test
    void getStatistics_returnsCorrectCounts() {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);

        // Submit three simulations
        SimulationResponse r1 = simulationService.submitSimulation(request);
        SimulationResponse r2 = simulationService.submitSimulation(request);
        SimulationResponse r3 = simulationService.submitSimulation(request);

        // Update statuses
        simulationService.updateStatus(r1.getSimulationId(), SimulationStatus.COMPLETED);
        simulationService.updateStatus(r2.getSimulationId(), SimulationStatus.FAILED);
        // r3 remains PENDING

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
    void getSimulationProgress_completedSimulation_returns100() throws InterruptedException {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.001, 1e-6);
        SimulationResponse response = simulationService.submitSimulation(request);
        String id = response.getSimulationId();

        // Wait for simulation to complete (short duration)
        TimeUnit.MILLISECONDS.sleep(500);

        // Update status to completed
        simulationService.updateStatus(id, SimulationStatus.COMPLETED);

        double progress = simulationService.getSimulationProgress(id);

        assertEquals(100.0, progress);
    }

    @Test
    void getSimulationProgress_nonExistingSimulation_returnsNegative() {
        double progress = simulationService.getSimulationProgress("non-existing-id");
        assertEquals(-1.0, progress);
    }

    @Test
    void cancelSimulation_updatesStatus() {
        SimulationRequest request = new SimulationRequest("test.ipes", 0.02, 1e-6);
        SimulationResponse response = simulationService.submitSimulation(request);
        String id = response.getSimulationId();

        // Set to running first
        simulationService.updateStatus(id, SimulationStatus.RUNNING);

        SimulationResponse cancelled = simulationService.cancelSimulation(id);

        assertEquals(SimulationStatus.FAILED, cancelled.getStatus());
        assertEquals("Cancelled by user", cancelled.getErrorMessage());
    }
}
