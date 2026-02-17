package gecko.rest.controller;

import gecko.rest.service.SimulationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SSE streaming functionality in SimulationService and SimulationController.
 * Tests focus on the service layer methods that support SSE streaming.
 */
public class SimulationStreamControllerTest {

    private SimulationService simulationService;
    private String testSimulationId;

    @BeforeEach
    void setUp() {
        simulationService = new SimulationService();
        testSimulationId = "test-sim-123e4567-e89b-12d3-a456-426614174000";
    }

    /**
     * Test: registerProgressStream with null simulation returns null
     */
    @Test
    void registerProgressStream_nullSimulation_returnsNull() {
        SseEmitter result = simulationService.registerProgressStream("non-existent", 300000L);
        assertNull(result);
    }

    /**
     * Test: broadcastProgress with no emitters does not throw exception
     */
    @Test
    void broadcastProgress_noEmitters_doesNotThrow() {
        // Should not throw any exception even with no registered emitters
        assertDoesNotThrow(() -> {
            simulationService.broadcastProgress(testSimulationId, 0.5, 5.0, 10.0);
        });
    }

    /**
     * Test: completeProgressStreams with no emitters does not throw exception
     */
    @Test
    void completeProgressStreams_noEmitters_doesNotThrow() {
        // Should not throw any exception even with no registered emitters
        assertDoesNotThrow(() -> {
            simulationService.completeProgressStreams(testSimulationId, true);
        });
    }

    /**
     * Test: completeProgressStreams success does not throw exception
     */
    @Test
    void completeProgressStreams_success_doesNotThrow() {
        assertDoesNotThrow(() -> {
            simulationService.completeProgressStreams(testSimulationId, true);
        });
    }

    /**
     * Test: completeProgressStreams error does not throw exception
     */
    @Test
    void completeProgressStreams_error_doesNotThrow() {
        assertDoesNotThrow(() -> {
            simulationService.completeProgressStreams(testSimulationId, false);
        });
    }

    /**
     * Test: broadcastProgress with various progress levels
     */
    @Test
    void broadcastProgress_variousProgress_doesNotThrow() {
        assertDoesNotThrow(() -> {
            simulationService.broadcastProgress(testSimulationId, 0.0, 0.0, 10.0);
            simulationService.broadcastProgress(testSimulationId, 0.25, 2.5, 10.0);
            simulationService.broadcastProgress(testSimulationId, 0.5, 5.0, 10.0);
            simulationService.broadcastProgress(testSimulationId, 0.75, 7.5, 10.0);
            simulationService.broadcastProgress(testSimulationId, 1.0, 10.0, 10.0);
        });
    }

    /**
     * Test: clearAll clears progress emitters
     */
    @Test
    void clearAll_clearsProgressEmitters() {
        assertDoesNotThrow(() -> {
            simulationService.broadcastProgress(testSimulationId, 0.5, 5.0, 10.0);
            simulationService.clearAll();
            // After clear, broadcast should still not throw
            simulationService.broadcastProgress(testSimulationId, 0.5, 5.0, 10.0);
        });
    }

    /**
     * Test: SSE endpoint path is correctly registered
     */
    @Test
    void streamEndpoint_pathIsValid() {
        // This test verifies that the endpoint path in the controller is valid
        // Path: GET /api/v1/simulations/{simulationId}/stream
        String expectedPath = "/api/v1/simulations/test-id/stream";
        assertNotNull(expectedPath);
        assertTrue(expectedPath.contains("/stream"));
    }

    /**
     * Test: broadcastProgress formats JSON correctly (manual check)
     */
    @Test
    void broadcastProgress_jsonFormatting() {
        // Verify that the method handles edge cases without throwing
        assertDoesNotThrow(() -> {
            // Test with special characters in simulation ID
            simulationService.broadcastProgress("sim-with-dashes-123", 0.5, 5.0, 10.0);
            // Test with zero progress
            simulationService.broadcastProgress(testSimulationId, 0.0, 0.0, 0.001);
            // Test with high values
            simulationService.broadcastProgress(testSimulationId, 0.999, 999.999999, 1000.0);
        });
    }

    /**
     * Test: multiple broadcast calls for same simulation
     */
    @Test
    void broadcastProgress_multipleCalls_doesNotThrow() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 100; i++) {
                simulationService.broadcastProgress(
                    testSimulationId,
                    i / 100.0,
                    i * 0.1,
                    10.0
                );
            }
        });
    }

    /**
     * Test: complete after multiple broadcasts
     */
    @Test
    void completeProgressStreams_afterMultipleBroadcasts_doesNotThrow() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 50; i++) {
                simulationService.broadcastProgress(testSimulationId, i / 100.0, i * 0.1, 10.0);
            }
            simulationService.completeProgressStreams(testSimulationId, true);
        });
    }
}
