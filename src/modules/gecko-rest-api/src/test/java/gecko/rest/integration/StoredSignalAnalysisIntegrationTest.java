package gecko.rest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gecko.rest.model.SimulationRequest;
import gecko.rest.model.SimulationResponse.SimulationStatus;
import gecko.rest.service.SimulationService;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StoredSignalAnalysisIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private SimulationService simulations;
    private String id;
    private double[] recordedTime;
    private String varyingSignal;

    @BeforeAll
    void runRealCircuit() {
        String circuit = Path.of("../gecko-simulation-core/src/test/resources/ipes/buck_simple.ipes").toAbsolutePath().toString();
        id = simulations.submitSimulation(new SimulationRequest(circuit, 0.002, 0.000002)).getSimulationId();
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertEquals(SimulationStatus.COMPLETED, simulations.getSimulation(id).getStatus()));
        recordedTime = simulations.getSignalData(id, "time");
        varyingSignal = simulations.getSimulation(id).getResults().entrySet().stream()
                .filter(e -> !e.getKey().equals("time") && e.getValue().length == recordedTime.length)
                .filter(e -> java.util.Arrays.stream(e.getValue()).distinct().count() > 2)
                .map(Map.Entry::getKey).findFirst().orElseThrow();
    }

    @BeforeEach
    void restoreRecordedClock() { simulations.addResults(id, "time", recordedTime); }

    private Map<String, Object> request(String signal) {
        return new LinkedHashMap<>(Map.of("simulationId", id, "signalName", signal));
    }

    private JsonNode analyze(String endpoint, Object request) throws Exception {
        String json = mvc.perform(post("/api/v1/analysis/" + endpoint).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(json);
    }

    private void retain(double[] clock, double[] values) {
        simulations.addResults(id, "time", clock);
        simulations.addResults(id, "known", values);
    }

    private void twoLevels() {
        double[] time = new double[257];
        double[] values = new double[257];
        for (int i = 0; i <= 256; i++) {
            time[i] = 10 + i / 256.0;
            values[i] = i < 128 ? 2 : 6;
        }
        retain(time, values);
    }

    @Test
    void realStoredSignalUsesItsClockWithoutASuppliedRate() throws Exception {
        double[] data = simulations.getSignalData(id, varyingSignal);
        double integral = 0, squared = 0;
        for (int i = 0; i < data.length - 1; i++) {
            double dt = recordedTime[i + 1] - recordedTime[i];
            integral += data[i] * dt;
            squared += data[i] * data[i] * dt;
        }
        double duration = recordedTime[recordedTime.length - 1] - recordedTime[0];
        JsonNode result = analyze("characteristics", request(varyingSignal));
        assertEquals(integral / duration, result.get("average").asDouble(), 1e-5);
        assertEquals(Math.sqrt(squared / duration), result.get("rms").asDouble(), 1e-5);
        assertEquals(data.length, result.get("sampleCount").asInt());
        assertEquals(Math.sqrt(squared / duration), analyze("rms", request(varyingSignal)).asDouble(), 1e-5);
    }

    @Test
    void nonuniformRecordedIntervalsWeightAverageAndRms() throws Exception {
        retain(new double[]{10, 11, 14, 16}, new double[]{2, 4, 8, 99});
        var input = request("known");
        input.put("sampleRate", 1000);
        JsonNode result = analyze("characteristics", input);
        assertEquals(5, result.get("average").asDouble(), 1e-6);
        assertEquals(Math.sqrt(30), result.get("rms").asDouble(), 1e-6);
    }

    @Test
    void characteristicsAndRmsHonorDistinctWindows() throws Exception {
        twoLevels();
        for (double start : new double[]{10, 10.5}) {
            var input = request("known");
            input.put("startTime", start); input.put("endTime", start + 0.5);
            double expected = start == 10 ? 2 : 6;
            JsonNode result = analyze("characteristics", input);
            assertEquals(expected, result.get("average").asDouble(), 1e-6);
            assertEquals(expected, result.get("min").asDouble(), 1e-6);
            assertEquals(expected, result.get("max").asDouble(), 1e-6);
            assertEquals(129, result.get("sampleCount").asInt());
            assertEquals(expected, analyze("rms", input).asDouble(), 1e-6);
        }
    }

    @Test
    void fractionalBoundariesUseOnlyTheRequestedDuration() throws Exception {
        retain(new double[]{10, 11, 14, 16}, new double[]{2, 4, 8, 99});
        var input = request("known");
        input.put("startTime", 10.5); input.put("endTime", 15);
        JsonNode result = analyze("characteristics", input);
        assertEquals(21.0 / 4.5, result.get("average").asDouble(), 1e-6);
        assertEquals(Math.sqrt(114.0 / 4.5), result.get("rms").asDouble(), 1e-6);
        assertEquals(4, result.get("sampleCount").asInt());
    }

    @Test
    void omittedBoundsDefaultIndependentlyToTheRecordedEndpoints() throws Exception {
        twoLevels();
        var first = request("known"); first.put("endTime", 10.5);
        var last = request("known"); last.put("startTime", 10.5);
        assertEquals(2, analyze("characteristics", first).get("average").asDouble(), 1e-6);
        assertEquals(6, analyze("characteristics", last).get("average").asDouble(), 1e-6);
        assertEquals(4, analyze("characteristics", request("known")).get("average").asDouble(), 1e-6);
    }

    @Test
    void fourierUsesRecordedTimeAndTheSelectedWindowEvenWithAConflictingRate() throws Exception {
        twoLevels();
        for (double start : new double[]{10, 10.5}) {
            var input = request("known");
            input.put("startTime", start); input.put("endTime", start + 0.5); input.put("sampleRate", 12345);
            JsonNode result = analyze("fourier?harmonics=3", input);
            assertEquals(2, result.get("baseFrequency").asDouble(), 1e-9);
            assertEquals(start == 10 ? 2 : 6, result.get("dcComponent").asDouble(), 1e-6);
            assertEquals(0, result.get("fundamentalAmplitude").asDouble(), 1e-6);
        }
    }

    @Test
    void recordedSineKeepsItsPhysicalFrequencyAndAmplitude() throws Exception {
        double[] sine = new double[recordedTime.length];
        double duration = recordedTime[recordedTime.length - 1] - recordedTime[0];
        for (int i = 0; i < sine.length; i++) sine[i] = 2 * Math.sin(2 * Math.PI * (recordedTime[i] - recordedTime[0]) / duration);
        simulations.addResults(id, "known", sine);
        JsonNode result = analyze("fourier?harmonics=3", request("known"));
        assertEquals(1 / duration, result.get("baseFrequency").asDouble(), 1e-6);
        assertEquals(2, result.get("fundamentalAmplitude").asDouble(), 0.01);
    }

    @Test
    void shortWindowCanProduceCharacteristicsButRejectsUnresolvableHarmonics() throws Exception {
        retain(new double[]{10, 11, 14, 16}, new double[]{2, 4, 8, 99});
        var input = request("known"); input.put("startTime", 11.5); input.put("endTime", 12);
        assertEquals(4, analyze("characteristics", input).get("average").asDouble(), 1e-6);
        mvc.perform(post("/api/v1/analysis/fourier?harmonics=3").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(input))).andExpect(status().isBadRequest());
    }

    @Test
    void aRealVaryingSignalHonorsAnInteriorRecordedWindow() throws Exception {
        double[] data = simulations.getSignalData(id, varyingSignal);
        int first = data.length / 4, last = 3 * data.length / 4;
        double integral = 0;
        for (int i = first; i < last; i++) integral += data[i] * (recordedTime[i + 1] - recordedTime[i]);
        var input = request(varyingSignal);
        input.put("startTime", recordedTime[first]); input.put("endTime", recordedTime[last]);
        JsonNode result = analyze("characteristics", input);
        assertEquals(integral / (recordedTime[last] - recordedTime[first]), result.get("average").asDouble(), 1e-5);
        assertEquals(last - first + 1, result.get("sampleCount").asInt());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY})
    void storedClockRemainsAuthoritativeWhenAnUnusedRateIsSupplied(double ignoredRate) throws Exception {
        twoLevels();
        var input = request("known"); input.put("sampleRate", ignoredRate);
        assertEquals(4, analyze("characteristics", input).get("average").asDouble(), 1e-6);
        assertEquals(1, analyze("fourier?harmonics=3", input).get("baseFrequency").asDouble(), 1e-6);
    }

    @Test
    void rawArraysKeepTheirExplicitRateAndTakePrecedenceOverStoredReferences() throws Exception {
        var input = request("missing"); input.put("data", new double[]{3, 3, 3, 3, 3, 3, 3, 3, 3});
        input.put("sampleRate", 8); input.put("startTime", 999); input.put("endTime", -999);
        assertEquals(3, analyze("characteristics", input).get("average").asDouble(), 1e-6);
        JsonNode result = analyze("fourier?harmonics=3", input);
        assertEquals(1, result.get("baseFrequency").asDouble(), 1e-6);
        assertEquals(3, result.get("dcComponent").asDouble(), 1e-6);
    }

    @Test
    void analysisDoesNotMutateTheRetainedClockOrSignal() throws Exception {
        twoLevels();
        double[] time = simulations.getSignalData(id, "time"), values = simulations.getSignalData(id, "known");
        var input = request("known"); input.put("startTime", 10.25); input.put("endTime", 10.75);
        analyze("characteristics", input); analyze("fourier?harmonics=3", input);
        assertArrayEquals(time, simulations.getSignalData(id, "time"));
        assertArrayEquals(values, simulations.getSignalData(id, "known"));
    }

    static Stream<double[]> invalidClocks() {
        return Stream.of(new double[]{}, new double[]{10}, new double[]{10, 11},
                new double[]{10, 10, 11, 12}, new double[]{10, 12, 11, 13},
                new double[]{10, Double.NaN, 12, 13}, new double[]{10, 11, 12, Double.POSITIVE_INFINITY},
                new double[]{-Double.MAX_VALUE, -1, 1, Double.MAX_VALUE});
    }

    @ParameterizedTest
    @MethodSource("invalidClocks")
    void corruptOrMissingRecordedClockIsNotReplacedByASuppliedRate(double[] clock) throws Exception {
        retain(clock, new double[]{1, 2, 3, 4});
        var input = request("known"); input.put("sampleRate", 1000);
        for (String endpoint : new String[]{"characteristics", "rms", "fourier?harmonics=0"}) {
            mvc.perform(post("/api/v1/analysis/" + endpoint).contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(input))).andExpect(status().isBadRequest());
        }
    }

    static Stream<double[]> invalidWindows() {
        return Stream.of(new double[]{10.5, 10.5}, new double[]{11, 10}, new double[]{9, 10.5},
                new double[]{10, 12}, new double[]{Double.NaN, 11}, new double[]{10, Double.POSITIVE_INFINITY});
    }

    @ParameterizedTest
    @MethodSource("invalidWindows")
    void invalidWindowsReturnClientErrorsOnEveryAnalysisEndpoint(double[] window) throws Exception {
        twoLevels();
        var input = request("known"); input.put("startTime", window[0]); input.put("endTime", window[1]);
        input.put("sampleRate", 1000);
        for (String endpoint : new String[]{"characteristics", "rms", "fourier?harmonics=0"}) {
            mvc.perform(post("/api/v1/analysis/" + endpoint).contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(input))).andExpect(status().isBadRequest());
        }
    }
    @ParameterizedTest
    @ValueSource(strings = {"{\"data\":[],\"sampleRate\":100}", "{\"simulationId\":\"missing\"}", "{\"signalName\":\"missing\"}"})
    void incompleteInputFormsReturnClientErrors(String input) throws Exception {
        for (String endpoint : new String[]{"characteristics", "rms", "fourier?harmonics=0"}) {
            mvc.perform(post("/api/v1/analysis/" + endpoint).contentType(MediaType.APPLICATION_JSON)
                    .content(input)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void missingAndEmptyRetainedSignalsRemainNotFound() throws Exception {
        simulations.addResults(id, "empty", new double[]{});
        for (String signal : new String[]{"missing", "empty"}) {
            mvc.perform(post("/api/v1/analysis/characteristics").contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(request(signal)))).andExpect(status().isNotFound());
        }
    }
}
