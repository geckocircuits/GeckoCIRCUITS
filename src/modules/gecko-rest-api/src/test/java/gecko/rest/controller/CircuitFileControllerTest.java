package gecko.rest.controller;

import gecko.rest.model.circuit.*;
import gecko.rest.service.CircuitFileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CircuitFileController using MockMvc.
 */
@WebMvcTest(CircuitFileController.class)
class CircuitFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CircuitFileService circuitFileService;

    @Test
    void testParseCircuit_success() throws Exception {
        // Mock service response
        CircuitLoadResponse mockResponse = CircuitLoadResponse.success(
            "circuit-123",
            "test.ipes",
            42
        );
        when(circuitFileService.loadCircuit(anyString(), anyString()))
            .thenReturn(mockResponse);

        // Test POST /api/v1/circuits/parse (JSON)
        mockMvc.perform(post("/api/v1/circuits/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "content": "SGVsbG8gV29ybGQ=",
                        "filename": "test.ipes"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.circuitId").value("circuit-123"))
            .andExpect(jsonPath("$.status").value("loaded"))
            .andExpect(jsonPath("$.filename").value("test.ipes"))
            .andExpect(jsonPath("$.componentCount").value(42))
            .andExpect(jsonPath("$.errorMessage").isEmpty());
    }

    @Test
    void testParseCircuit_failure() throws Exception {
        // Mock service failure response
        CircuitLoadResponse mockResponse = CircuitLoadResponse.failure(
            "test.ipes",
            "Invalid file format"
        );
        when(circuitFileService.loadCircuit(anyString(), anyString()))
            .thenReturn(mockResponse);

        // Test POST /api/v1/circuits/parse (JSON)
        mockMvc.perform(post("/api/v1/circuits/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "content": "invalid-content",
                        "filename": "test.ipes"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("failed"))
            .andExpect(jsonPath("$.errorMessage").value("Invalid file format"));
    }

    @Test
    void testGetCircuitInfo_found() throws Exception {
        // Mock service response
        CircuitInfo mockInfo = new CircuitInfo(
            "circuit-123",
            "test.ipes",
            124,
            new CircuitInfo.SimulationParameters(0.02, 1e-6, "backward-euler", 0.0, 1e-7),
            new CircuitInfo.ComponentCounts(15, 20, 5, 42),
            new CircuitInfo.DisplaySettings(1400, 900, 12),
            new CircuitInfo.Metadata("2026-02-15", 12345)
        );
        when(circuitFileService.getCircuitInfo("circuit-123")).thenReturn(mockInfo);

        // Test GET /api/v1/circuits/{id}/info
        mockMvc.perform(get("/api/v1/circuits/circuit-123/info"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.circuitId").value("circuit-123"))
            .andExpect(jsonPath("$.filename").value("test.ipes"))
            .andExpect(jsonPath("$.fileVersion").value(124))
            .andExpect(jsonPath("$.simulationParameters.endTime").value(0.02))
            .andExpect(jsonPath("$.simulationParameters.timeStep").value(1e-6))
            .andExpect(jsonPath("$.simulationParameters.solverType").value("backward-euler"))
            .andExpect(jsonPath("$.componentCounts.circuit").value(15))
            .andExpect(jsonPath("$.componentCounts.control").value(20))
            .andExpect(jsonPath("$.componentCounts.thermal").value(5))
            .andExpect(jsonPath("$.componentCounts.connections").value(42));
    }

    @Test
    void testGetCircuitInfo_notFound() throws Exception {
        when(circuitFileService.getCircuitInfo("nonexistent")).thenReturn(null);

        mockMvc.perform(get("/api/v1/circuits/nonexistent/info"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testGetComponents_found() throws Exception {
        // Mock service response
        ComponentInfo comp1 = new ComponentInfo(
            1, "R1", "circuit",
            new int[]{100, 200}, 0,
            java.util.Map.of("resistance", 10.0)
        );
        ComponentListResponse mockResponse = new ComponentListResponse(
            "circuit-123",
            List.of(comp1)
        );
        when(circuitFileService.getComponents("circuit-123")).thenReturn(mockResponse);

        // Test GET /api/v1/circuits/{id}/components
        mockMvc.perform(get("/api/v1/circuits/circuit-123/components"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.circuitId").value("circuit-123"))
            .andExpect(jsonPath("$.components").isArray())
            .andExpect(jsonPath("$.components[0].name").value("R1"))
            .andExpect(jsonPath("$.components[0].domain").value("circuit"))
            .andExpect(jsonPath("$.components[0].type").value(1));
    }

    @Test
    void testGetComponents_notFound() throws Exception {
        when(circuitFileService.getComponents("nonexistent")).thenReturn(null);

        mockMvc.perform(get("/api/v1/circuits/nonexistent/components"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testValidateCircuit_valid() throws Exception {
        // Mock service response
        ValidationResponse mockResponse = ValidationResponse.success();
        when(circuitFileService.validateCircuit("circuit-123")).thenReturn(mockResponse);

        // Test GET /api/v1/circuits/{id}/validate
        mockMvc.perform(get("/api/v1/circuits/circuit-123/validate"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.warnings").isEmpty())
            .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void testValidateCircuit_withWarnings() throws Exception {
        // Mock service response
        ValidationResponse mockResponse = ValidationResponse.successWithWarnings(
            List.of("Component R5 has no connections")
        );
        when(circuitFileService.validateCircuit("circuit-123")).thenReturn(mockResponse);

        // Test GET /api/v1/circuits/{id}/validate
        mockMvc.perform(get("/api/v1/circuits/circuit-123/validate"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.warnings").isArray())
            .andExpect(jsonPath("$.warnings[0]").value("Component R5 has no connections"))
            .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void testValidateCircuit_invalid() throws Exception {
        // Mock service response
        ValidationResponse mockResponse = ValidationResponse.failure(
            List.of("Circuit has no components")
        );
        when(circuitFileService.validateCircuit("circuit-123")).thenReturn(mockResponse);

        // Test GET /api/v1/circuits/{id}/validate
        mockMvc.perform(get("/api/v1/circuits/circuit-123/validate"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[0]").value("Circuit has no components"));
    }

    @Test
    void testValidateCircuit_notFound() throws Exception {
        when(circuitFileService.validateCircuit("nonexistent")).thenReturn(null);

        mockMvc.perform(get("/api/v1/circuits/nonexistent/validate"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteCircuit_success() throws Exception {
        when(circuitFileService.deleteCircuit("circuit-123")).thenReturn(true);

        mockMvc.perform(delete("/api/v1/circuits/circuit-123"))
            .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteCircuit_notFound() throws Exception {
        when(circuitFileService.deleteCircuit("nonexistent")).thenReturn(false);

        mockMvc.perform(delete("/api/v1/circuits/nonexistent"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllCircuits() throws Exception {
        // Mock service response
        CircuitListResponse mockResponse = new CircuitListResponse(
            List.of(
                new CircuitListResponse.CircuitSummary(
                    "circuit-1", "test1.ipes", 42, "2026-02-16T10:30:00Z"
                ),
                new CircuitListResponse.CircuitSummary(
                    "circuit-2", "test2.ipes", 35, "2026-02-16T10:35:00Z"
                )
            ),
            2
        );
        when(circuitFileService.getAllCircuits()).thenReturn(mockResponse);

        // Test GET /api/v1/circuits
        mockMvc.perform(get("/api/v1/circuits"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.circuits").isArray())
            .andExpect(jsonPath("$.circuits[0].circuitId").value("circuit-1"))
            .andExpect(jsonPath("$.circuits[0].filename").value("test1.ipes"))
            .andExpect(jsonPath("$.circuits[0].componentCount").value(42))
            .andExpect(jsonPath("$.circuits[1].circuitId").value("circuit-2"));
    }

    @Test
    void testGetAllCircuits_empty() throws Exception {
        // Mock service response
        CircuitListResponse mockResponse = new CircuitListResponse(List.of(), 0);
        when(circuitFileService.getAllCircuits()).thenReturn(mockResponse);

        // Test GET /api/v1/circuits
        mockMvc.perform(get("/api/v1/circuits"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(0))
            .andExpect(jsonPath("$.circuits").isEmpty());
    }
}
