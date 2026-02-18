package gecko.rest.controller;

import gecko.rest.model.circuit.*;
import gecko.rest.service.CircuitFileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for circuit management endpoints (clone and parameter update).
 */
@WebMvcTest(CircuitFileController.class)
class CircuitManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CircuitFileService circuitFileService;

    // ========== Clone Circuit Tests ==========

    @Test
    void testCloneCircuit_success() throws Exception {
        // Mock service response
        CircuitLoadResponse mockResponse = CircuitLoadResponse.success(
            "circuit-clone-123",
            "test.ipes",
            42
        );
        when(circuitFileService.cloneCircuit("circuit-original", null))
            .thenReturn(mockResponse);

        // Test POST /api/v1/circuits/{id}/clone with empty body
        mockMvc.perform(post("/api/v1/circuits/circuit-original/clone")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.circuitId").value("circuit-clone-123"))
            .andExpect(jsonPath("$.status").value("loaded"))
            .andExpect(jsonPath("$.filename").value("test.ipes"))
            .andExpect(jsonPath("$.componentCount").value(42));
    }

    @Test
    void testCloneCircuit_notFound() throws Exception {
        // Mock service throwing 404
        when(circuitFileService.cloneCircuit(eq("nonexistent"), any()))
            .thenThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Circuit not found"
            ));

        // Test POST /api/v1/circuits/{id}/clone with non-existent ID
        mockMvc.perform(post("/api/v1/circuits/nonexistent/clone")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testCloneCircuit_withOverrides() throws Exception {
        // Mock service response
        CircuitLoadResponse mockResponse = CircuitLoadResponse.success(
            "circuit-clone-456",
            "test.ipes",
            42
        );
        when(circuitFileService.cloneCircuit(
            eq("circuit-original"),
            argThat(m -> m != null && m.containsKey("R1.resistance") && m.get("R1.resistance") == 50.0)
        )).thenReturn(mockResponse);

        // Test POST /api/v1/circuits/{id}/clone with overrides
        mockMvc.perform(post("/api/v1/circuits/circuit-original/clone")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "overrides": {
                            "R1.resistance": 50.0,
                            "C1.capacitance": 2e-6
                        }
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.circuitId").value("circuit-clone-456"));
    }

    @Test
    void testCloneCircuit_noBody() throws Exception {
        // Mock service response when no body provided
        CircuitLoadResponse mockResponse = CircuitLoadResponse.success(
            "circuit-clone-789",
            "test.ipes",
            42
        );
        when(circuitFileService.cloneCircuit("circuit-original", null))
            .thenReturn(mockResponse);

        // Test POST /api/v1/circuits/{id}/clone with no body
        mockMvc.perform(post("/api/v1/circuits/circuit-original/clone")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.circuitId").value("circuit-clone-789"));
    }

    @Test
    void testCloneCircuit_differentIds() throws Exception {
        // Verify cloned circuit has different ID than original
        CircuitLoadResponse mockResponse = CircuitLoadResponse.success(
            "circuit-new-id-999",  // Different from source circuit-original
            "test.ipes",
            42
        );
        when(circuitFileService.cloneCircuit(eq("circuit-original"), isNull()))
            .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/circuits/circuit-original/clone")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.circuitId").value("circuit-new-id-999"))
            .andExpect(jsonPath("$.circuitId").value(org.hamcrest.Matchers.not("circuit-original")));
    }

    // ========== Update Circuit Parameters Tests ==========

    @Test
    void testUpdateCircuitParameters_success() throws Exception {
        // Mock service response
        CircuitInfo mockInfo = new CircuitInfo(
            "circuit-123",
            "test.ipes",
            124,
            new CircuitInfo.SimulationParameters(0.05, 5e-7, "trapezoidal", 0.0, 1e-7),
            new CircuitInfo.ComponentCounts(15, 20, 5, 42),
            new CircuitInfo.DisplaySettings(1400, 900, 12),
            new CircuitInfo.Metadata("2026-02-15", 12345)
        );
        when(circuitFileService.updateCircuitParameters(eq("circuit-123"), any()))
            .thenReturn(mockInfo);

        // Test PUT /api/v1/circuits/{id}/parameters
        mockMvc.perform(put("/api/v1/circuits/circuit-123/parameters")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "simulationDuration": 0.05,
                        "timeStep": 5e-7,
                        "solverType": "trapezoidal"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.circuitId").value("circuit-123"))
            .andExpect(jsonPath("$.simulationParameters.endTime").value(0.05))
            .andExpect(jsonPath("$.simulationParameters.timeStep").value(5e-7))
            .andExpect(jsonPath("$.simulationParameters.solverType").value("trapezoidal"));
    }

    @Test
    void testUpdateCircuitParameters_notFound() throws Exception {
        // Mock service throwing 404
        when(circuitFileService.updateCircuitParameters(eq("nonexistent"), any()))
            .thenThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Circuit not found"
            ));

        // Test PUT /api/v1/circuits/{id}/parameters with non-existent ID
        mockMvc.perform(put("/api/v1/circuits/nonexistent/parameters")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "simulationDuration": 0.05
                    }
                    """))
            .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateCircuitParameters_withSimulationDuration() throws Exception {
        // Mock response with updated duration
        CircuitInfo mockInfo = new CircuitInfo(
            "circuit-123",
            "test.ipes",
            124,
            new CircuitInfo.SimulationParameters(0.1, 1e-6, "backward-euler", 0.0, 1e-7),
            new CircuitInfo.ComponentCounts(15, 20, 5, 42),
            new CircuitInfo.DisplaySettings(1400, 900, 12),
            new CircuitInfo.Metadata("2026-02-15", 12345)
        );
        when(circuitFileService.updateCircuitParameters(eq("circuit-123"), any()))
            .thenReturn(mockInfo);

        mockMvc.perform(put("/api/v1/circuits/circuit-123/parameters")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "simulationDuration": 0.1
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulationParameters.endTime").value(0.1));
    }

    @Test
    void testUpdateCircuitParameters_withSolverType() throws Exception {
        // Mock response with updated solver type
        CircuitInfo mockInfo = new CircuitInfo(
            "circuit-123",
            "test.ipes",
            124,
            new CircuitInfo.SimulationParameters(0.02, 1e-6, "gear-shichman", 0.0, 1e-7),
            new CircuitInfo.ComponentCounts(15, 20, 5, 42),
            new CircuitInfo.DisplaySettings(1400, 900, 12),
            new CircuitInfo.Metadata("2026-02-15", 12345)
        );
        when(circuitFileService.updateCircuitParameters(eq("circuit-123"), any()))
            .thenReturn(mockInfo);

        mockMvc.perform(put("/api/v1/circuits/circuit-123/parameters")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "solverType": "gear-shichman"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulationParameters.solverType").value("gear-shichman"));
    }

    @Test
    void testUpdateCircuitParameters_withTimeStep() throws Exception {
        // Mock response with updated time step
        CircuitInfo mockInfo = new CircuitInfo(
            "circuit-123",
            "test.ipes",
            124,
            new CircuitInfo.SimulationParameters(0.02, 1e-5, "backward-euler", 0.0, 1e-7),
            new CircuitInfo.ComponentCounts(15, 20, 5, 42),
            new CircuitInfo.DisplaySettings(1400, 900, 12),
            new CircuitInfo.Metadata("2026-02-15", 12345)
        );
        when(circuitFileService.updateCircuitParameters(eq("circuit-123"), any()))
            .thenReturn(mockInfo);

        mockMvc.perform(put("/api/v1/circuits/circuit-123/parameters")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "timeStep": 1e-5
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulationParameters.timeStep").value(1e-5));
    }

    @Test
    void testUpdateCircuitParameters_partialUpdate() throws Exception {
        // Mock response - only specified fields updated
        CircuitInfo mockInfo = new CircuitInfo(
            "circuit-123",
            "test.ipes",
            124,
            new CircuitInfo.SimulationParameters(0.1, 1e-6, "trapezoidal", 0.0, 1e-7),
            new CircuitInfo.ComponentCounts(15, 20, 5, 42),
            new CircuitInfo.DisplaySettings(1400, 900, 12),
            new CircuitInfo.Metadata("2026-02-15", 12345)
        );
        when(circuitFileService.updateCircuitParameters(eq("circuit-123"), any()))
            .thenReturn(mockInfo);

        // Update only simulationDuration and solverType, not timeStep
        mockMvc.perform(put("/api/v1/circuits/circuit-123/parameters")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "simulationDuration": 0.1,
                        "solverType": "trapezoidal"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulationParameters.endTime").value(0.1))
            .andExpect(jsonPath("$.simulationParameters.solverType").value("trapezoidal"))
            .andExpect(jsonPath("$.simulationParameters.timeStep").value(1e-6));
    }
}
