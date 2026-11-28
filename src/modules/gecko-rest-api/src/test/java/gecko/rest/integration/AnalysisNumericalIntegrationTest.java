package gecko.rest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gecko.rest.model.SimulationRequest;
import gecko.rest.model.SimulationResponse;
import gecko.rest.service.SimulationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisNumericalIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private SimulationService simulations;

    private JsonNode analyze(String path, Object request) throws Exception {
        String response = mvc.perform(post("/api/v1/analysis/" + path).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response);
    }

    @Test
    void constantSignalHasKnownAverageRmsAndExtremaThroughTheRealService() throws Exception {
        JsonNode result = analyze("characteristics", Map.of("data", new double[]{3, 3, 3, 3, 3}, "sampleRate", 100));
        assertEquals(3, result.get("average").asDouble(), 1e-6);
        assertEquals(3, result.get("rms").asDouble(), 1e-6);
        assertEquals(3, result.get("min").asDouble(), 1e-6);
        assertEquals(3, result.get("max").asDouble(), 1e-6);
        assertEquals(0, result.get("peakToPeak").asDouble(), 1e-6);
    }

    @Test
    void sampledSineHasTheExpectedFourierAmplitude() throws Exception {
        double[] signal = new double[1001];
        for (int i = 0; i < signal.length; i++) { signal[i] = 2 * Math.sin(2 * Math.PI * i / 1000); }
        JsonNode result = analyze("fourier?harmonics=3", Map.of("data", signal, "sampleRate", 1000));
        assertEquals(1, result.get("baseFrequency").asDouble(), 1e-6);
        assertEquals(2, result.get("fundamentalAmplitude").asDouble(), 0.01);
        assertEquals(0, result.get("dcComponent").asDouble(), 0.01);
        assertEquals(0, result.get("cnAmplitudes").get(2).asDouble(), 0.01);
    }

    @Test
    void storedSimulationSignalCanBeAnalyzedWithoutMockingAnalysisService() throws Exception {
        String circuit = Path.of("../gecko-simulation-core/src/test/resources/ipes/buck_simple.ipes").toAbsolutePath().toString();
        String id = simulations.submitSimulation(new SimulationRequest(circuit, 0.002, 0.00025)).getSimulationId();
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertEquals(
                SimulationResponse.SimulationStatus.COMPLETED, simulations.getSimulation(id).getStatus()));
        double[] known = new double[simulations.getSignalData(id, "time").length];
        java.util.Arrays.fill(known, 4);
        simulations.addResults(id, "known", known);
        JsonNode result = analyze("characteristics", Map.of("simulationId", id, "signalName", "known"));
        assertEquals(4, result.get("average").asDouble(), 1e-6);
        assertEquals("known", result.get("signalName").asText());
    }

    @Test
    void invalidAnalysisInputsReturnClientErrors() throws Exception {
        for (Object request : new Object[]{Map.of(), Map.of("data", new double[]{1}, "sampleRate", 100),
                Map.of("data", new double[]{1, 2}, "sampleRate", 0),
                Map.of("data", new double[]{1, 2}, "sampleRate", Double.NaN),
                Map.of("data", new double[]{1, 2}, "sampleRate", Double.POSITIVE_INFINITY),
                Map.of("data", new double[]{1, 2}, "sampleRate", Double.MIN_VALUE),
                Map.of("data", new double[]{1, Double.NaN}, "sampleRate", 100),
                Map.of("data", new double[]{1, Double.MAX_VALUE}, "sampleRate", 100)}) {
            for (String endpoint : new String[]{"characteristics", "fourier?harmonics=3"}) {
                mvc.perform(post("/api/v1/analysis/" + endpoint).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))).andExpect(status().isBadRequest());
            }
        }
        mvc.perform(post("/api/v1/analysis/fourier?harmonics=-1").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/analysis/characteristics").contentType(MediaType.APPLICATION_JSON)
                .content("{\"simulationId\":\"missing\",\"signalName\":\"missing\",\"sampleRate\":100}"))
                .andExpect(status().isNotFound());
    }
}
