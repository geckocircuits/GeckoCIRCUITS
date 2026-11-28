package gecko.rest.integration;

import gecko.rest.model.SimulationRequest;
import gecko.rest.model.SimulationResponse;
import gecko.rest.service.SimulationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for parameter sweep functionality - running multiple simulations
 * with varying parameters to analyze circuit behavior.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Parameter Sweep Tests")
class ParameterSweepTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SimulationService simulationService;

    @TempDir
    Path temporaryDirectory;

    private String circuitFile;
    private String acCircuitFile;
    private String thermalCircuitFile;
    private String complexCircuitFile;

    @BeforeEach
    void setUp() throws Exception {
        simulationService.clearAll();
        circuitFile = ExistingSimulationFixtures.rc(temporaryDirectory);
        acCircuitFile = ExistingSimulationFixtures.ac(temporaryDirectory);
        thermalCircuitFile = ExistingSimulationFixtures.thermal(temporaryDirectory);
        complexCircuitFile = ExistingSimulationFixtures.complex(temporaryDirectory);
    }

    @Test
    @DisplayName("Resistance sweep for RC circuit")
    void resistanceSweep() throws Exception {
        List<String> simulationIds = new ArrayList<>();
        double[] resistanceValues = {100.0, 500.0, 1000.0, 5000.0, 10000.0};

        for (double resistance : resistanceValues) {
            SimulationRequest request = new SimulationRequest(circuitFile, 0.01, 1e-6);
            request.setParameters(Map.of(
                "R1.resistance", resistance,
                "C1.capacitance", 1e-6
            ));

            MvcResult result = mockMvc.perform(post("/api/v1/simulations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            SimulationResponse response = objectMapper.readValue(
                    result.getResponse().getContentAsString(), SimulationResponse.class);
            simulationIds.add(response.getSimulationId());
        }

        assertEquals(resistanceValues.length, simulationIds.size());

        // Verify all simulations are tracked
        mockMvc.perform(get("/api/v1/simulations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(resistanceValues.length));
    }

    @Test
    @DisplayName("Capacitance sweep")
    void capacitanceSweep() throws Exception {
        double[] capacitorValues = {1e-9, 1e-8, 1e-7, 1e-6, 1e-5};

        for (double capacitance : capacitorValues) {
            SimulationRequest request = new SimulationRequest(circuitFile, 0.01, 1e-7);
            request.setParameters(Map.of(
                "R1.resistance", 1000.0,
                "C1.capacitance", capacitance
            ));

            mockMvc.perform(post("/api/v1/simulations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/simulations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(capacitorValues.length));
    }

    @Test
    @DisplayName("Multi-parameter sweep")
    void multiParameterSweep() throws Exception {
        double[] resistances = {100.0, 1000.0};
        double[] capacitances = {1e-6, 1e-5};
        int expectedCount = resistances.length * capacitances.length;

        for (double r : resistances) {
            for (double c : capacitances) {
                SimulationRequest request = new SimulationRequest(circuitFile, 0.01, 1e-6);
                Map<String, Double> params = new HashMap<>();
                params.put("R1.resistance", r);
                params.put("C1.capacitance", c);
                request.setParameters(params);

                mockMvc.perform(post("/api/v1/simulations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated());
            }
        }

        mockMvc.perform(get("/api/v1/simulations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(expectedCount));
    }

    @Test
    @DisplayName("Voltage source sweep")
    void voltageSourceSweep() throws Exception {
        double[] voltages = {1.0, 5.0, 10.0, 12.0, 24.0};

        for (double voltage : voltages) {
            SimulationRequest request = new SimulationRequest(circuitFile, 0.005, 1e-6);
            request.setParameters(Map.of("Vs.dcValue", voltage));

            mockMvc.perform(post("/api/v1/simulations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @DisplayName("Frequency sweep for AC analysis")
    void frequencySweep() throws Exception {
        double[] frequencies = {50.0, 100.0, 500.0, 1000.0, 5000.0};

        for (double freq : frequencies) {
            double period = 1.0 / freq;
            double simTime = 10 * period; // 10 periods
            double timeStep = period / 100; // 100 points per period

            SimulationRequest request = new SimulationRequest(acCircuitFile, simTime, timeStep);
            request.setParameters(Map.of("Vs.frequency", freq));

            mockMvc.perform(post("/api/v1/simulations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @DisplayName("Temperature sweep for thermal analysis")
    void temperatureSweep() throws Exception {
        double[] temperatures = {-40.0, 0.0, 25.0, 85.0, 125.0};

        for (double temp : temperatures) {
            SimulationRequest request = new SimulationRequest(thermalCircuitFile, 0.1, 1e-5);
            request.setParameters(Map.of("ambient.dcValue", temp));

            mockMvc.perform(post("/api/v1/simulations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @DisplayName("Empty parameters accepted")
    void emptyParametersAccepted() throws Exception {
        SimulationRequest request = new SimulationRequest(circuitFile, 0.01, 1e-6);
        // No parameters set

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Multiple parameters in single request")
    void multipleParametersSingleRequest() throws Exception {
        SimulationRequest request = new SimulationRequest(complexCircuitFile, 0.01, 1e-6);
        request.setParameters(Map.of(
            "R1.resistance", 1000.0,
            "R2.resistance", 2000.0,
            "C1.capacitance", 1e-6,
            "C2.capacitance", 2e-6,
            "L1.inductance", 1e-3,
            "Vs.dcValue", 12.0
        ));

        mockMvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.simulationId").exists());
    }
}
