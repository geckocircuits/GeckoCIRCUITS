package gecko.rest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gecko.rest.support.PreparationCircuitFixtures;
import gecko.core.preparation.PreparationResult;
import gecko.core.preparation.ReadinessReport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CircuitPreparationHttpIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @TempDir Path directory;

    @Test
    void uploadedCloneExecutesByExplicitIdWithIndependentResistorCurrent() throws Exception {
        String filename = "upload-only-" + UUID.randomUUID() + ".ipes";
        assertFalse(Files.exists(Path.of(filename)));
        MockMultipartFile file = new MockMultipartFile("file", filename, "application/gzip",
                PreparationCircuitFixtures.resistorCircuit(10));
        JsonNode uploaded = mapper.readTree(mvc.perform(multipart("/api/v1/circuits/parse").file(file))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String originalId = uploaded.path("circuitId").asText();
        assertFalse(originalId.isBlank(), uploaded.toString());
        JsonNode cloned = mapper.readTree(mvc.perform(post("/api/v1/circuits/{id}/clone", originalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("overrides", Map.of("R.1.resistance", 20.0)))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String cloneId = cloned.path("circuitId").asText();
        assertFalse(cloneId.isBlank(), cloned.toString());
        assertNotEquals(originalId, cloneId);

        assertCurrent(submit(Map.of("circuitId", cloneId, "simulationTime", 0.25, "timeStep", 0.125)), 0.3);
        assertCurrent(submit(Map.of("circuitId", originalId, "simulationTime", 0.25, "timeStep", 0.125)), 0.6);
        assertFalse(Files.exists(Path.of(filename)));
    }


    @Test
    void legacyStoredIdAndUuidNamedFilePathExecuteTheirSelectedCircuits() throws Exception {
        String storedId = upload(PreparationCircuitFixtures.resistorCircuit(10), "legacy-upload.ipes");
        assertCurrent(submit(Map.of("circuitFile", storedId, "simulationTime", 0.25, "timeStep", 0.125)), 0.6);
        Path circuit = directory.resolve(UUID.randomUUID().toString());
        Files.write(circuit, PreparationCircuitFixtures.resistorCircuit(20));
        assertCurrent(submit(Map.of("circuitFile", circuit.toString(), "simulationTime", 0.25, "timeStep", 0.125)), 0.3);
        String relative = "./" + Path.of("").toAbsolutePath().relativize(circuit);
        assertCurrent(submit(Map.of("circuitFile", relative, "simulationTime", 0.25, "timeStep", 0.125)), 0.3);
    }

    @Test
    void invalidSourceSelectionReturnsTypedErrorsWithoutPublishingJobs() throws Exception {
        String storedId = upload(PreparationCircuitFixtures.resistorCircuit(10), "selection-upload.ipes");
        Path realFile = directory.resolve(UUID.randomUUID().toString());
        Files.write(realFile, PreparationCircuitFixtures.resistorCircuit(20));
        int before = simulationCount();
        assertSourceError(Map.of("circuitId", storedId, "circuitFile", realFile.toString(),
                "simulationTime", 0.25, "timeStep", 0.125), 400, "INVALID_SOURCE");
        assertSourceError(Map.of("simulationTime", 0.25, "timeStep", 0.125), 400, "INVALID_SOURCE");
        assertSourceError(Map.of("circuitId", UUID.randomUUID().toString(),
                "simulationTime", 0.25, "timeStep", 0.125), 404, "SOURCE_NOT_FOUND");
        assertSourceError(Map.of("circuitId", realFile.toString(),
                "simulationTime", 0.25, "timeStep", 0.125), 404, "SOURCE_NOT_FOUND");
        assertSourceError(Map.of("circuitFile", UUID.randomUUID().toString(),
                "simulationTime", 0.25, "timeStep", 0.125), 404, "SOURCE_NOT_FOUND");
        assertEquals(before, simulationCount());
    }


    @Test
    void floatingValidationAndAdmissionShareTheSameComponentDiagnostic() throws Exception {
        byte[] content = PreparationCircuitFixtures.floatingResistorCircuit();
        assertEquals(ReadinessReport.Outcome.INVALID, PreparationCircuitFixtures.prepare(content).report().outcome());
        String id = upload(content, "floating.ipes");
        int before = simulationCount();
        JsonNode validation = validate(id, "INVALID");
        JsonNode submitted = rejectedJob(id, "INVALID");
        assertEquals("R.1", diagnostic(validation, "UNCONNECTED_GROUND").path("component").asText());
        assertEquals(diagnostic(validation, "UNCONNECTED_GROUND").path("component"),
                diagnostic(submitted, "UNCONNECTED_GROUND").path("component"));
        assertEquals(before, simulationCount());
    }

    @Test
    void algebraicControlCyclesHaveMatchingValidationAndAdmissionDiagnostics() throws Exception {
        String id = upload(PreparationCircuitFixtures.algebraicControlCycle(), "control-cycle.ipes");
        int before = simulationCount();
        JsonNode validation = validate(id, "INVALID");
        JsonNode submitted = rejectedJob(id, "INVALID");
        assertEquals(diagnostic(validation, "CONTROL_DEPENDENCY_CYCLE"),
                diagnostic(submitted, "CONTROL_DEPENDENCY_CYCLE"));
        assertEquals("REFERENCES", diagnostic(validation, "CONTROL_DEPENDENCY_CYCLE").path("category").asText());
        assertEquals(before, simulationCount());
    }

    @Test
    void unsupportedUploadsAndClonesRemainInspectableWithoutAcceptedJobs() throws Exception {
        byte[] content = PreparationCircuitFixtures.unsupportedCircuit(false);
        assertEquals(ReadinessReport.Outcome.UNSUPPORTED, PreparationCircuitFixtures.prepare(content).report().outcome());
        String id = upload(content, "unsupported.ipes");
        int before = simulationCount();
        validate(id, "UNSUPPORTED");
        JsonNode cloned = mapper.readTree(mvc.perform(post("/api/v1/circuits/{id}/clone", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("overrides", Map.of("R.1.resistance", 20.0)))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String cloneId = cloned.path("circuitId").asText();
        mvc.perform(get("/api/v1/circuits/{id}/components", cloneId)).andExpect(status().isOk());
        JsonNode validation = validate(cloneId, "UNSUPPORTED");
        assertEquals("unknown", diagnostic(validation, "UNSUPPORTED_CONTROL_COMPONENT").path("component").asText());
        diagnostic(rejectedJob(cloneId, "UNSUPPORTED"), "UNSUPPORTED_CONTROL_COMPONENT");
        assertEquals(before, simulationCount());
    }

    @Test
    void validationRetainsInvalidAndUnsupportedFindings() throws Exception {
        byte[] content = PreparationCircuitFixtures.unsupportedCircuit(true);
        assertEquals(ReadinessReport.Outcome.INVALID, PreparationCircuitFixtures.prepare(content).report().outcome());
        String id = upload(content, "mixed.ipes");
        JsonNode validation = validate(id, "INVALID");
        assertEquals("floating", diagnostic(validation, "UNCONNECTED_GROUND").path("component").asText());
        diagnostic(validation, "UNSUPPORTED_CONTROL_COMPONENT");
        JsonNode rejected = rejectedJob(id, "INVALID");
        diagnostic(rejected, "UNCONNECTED_GROUND");
        diagnostic(rejected, "UNSUPPORTED_CONTROL_COMPONENT");
    }

    @Test
    void rejectedOverridesPublishNoCloneOrJobAndPreserveTheOriginalTrace() throws Exception {
        byte[] content = PreparationCircuitFixtures.resistorCircuit(10);
        assertInstanceOf(PreparationResult.Ready.class, PreparationCircuitFixtures.prepare(content));
        String id = upload(content, "atomic.ipes");
        int circuitsBefore = mapper.readTree(mvc.perform(get("/api/v1/circuits")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("total").asInt();
        int jobsBefore = simulationCount();
        Map<String, Double> overrides = Map.of("R.1.resistance", 20.0, "R.1.missing", 2.0);
        JsonNode cloneError = mapper.readTree(mvc.perform(post("/api/v1/circuits/{id}/clone", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("overrides", overrides))))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString());
        diagnostic(cloneError, "UNKNOWN_PARAMETER");
        JsonNode jobError = mapper.readTree(mvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("circuitId", id, "parameters", overrides))))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString());
        diagnostic(jobError, "UNKNOWN_PARAMETER");
        assertEquals(circuitsBefore, mapper.readTree(mvc.perform(get("/api/v1/circuits")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("total").asInt());
        assertEquals(jobsBefore, simulationCount());
        assertCurrent(submit(Map.of("circuitId", id)), 0.6);
    }


    @Test
    void storedSettingsInheritIndependentlyAndExplicitSolverChangesTheTrace() throws Exception {
        byte[] content = PreparationCircuitFixtures.capacitorCircuit();
        assertInstanceOf(PreparationResult.Ready.class, PreparationCircuitFixtures.prepare(content));
        String id = upload(content, "settings.ipes");
        mvc.perform(put("/api/v1/circuits/{id}/parameters", id).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("simulationDuration", 0.2, "timeStep", 0.1,
                                "solverType", "trapezoidal"))))
                .andExpect(status().isOk());
        for (String source : new String[]{"circuitId", "circuitFile"}) {
            JsonNode inherited = completedResults(submit(Map.of(source, id)));
            assertTimeGrid(inherited, 3, 0.1, 0.2);
            assertEquals(0.48, inherited.path("current").get(0).asDouble(), 1e-6);
            JsonNode solver = completedResults(submit(Map.of(source, id, "solverType", "gs")));
            assertTimeGrid(solver, 3, 0.1, 0.2);
            assertEquals(0.45, solver.path("current").get(0).asDouble(), 1e-6);
            JsonNode duration = completedResults(submit(Map.of(source, id, "simulationTime", 0.4)));
            assertTimeGrid(duration, 5, 0.1, 0.4);
            assertEquals(0.48, duration.path("current").get(0).asDouble(), 1e-6);
            assertTimeGrid(completedResults(submit(Map.of(source, id, "timeStep", 0.05))), 5, 0.05, 0.2);
        }
    }

    @Test
    void invalidExplicitSettingsAndMalformedJsonReturnTypedErrorsWithoutFallback() throws Exception {
        String id = upload(PreparationCircuitFixtures.resistorCircuit(10), "invalid-settings.ipes");
        int before = simulationCount();
        for (String source : new String[]{"circuitId", "circuitFile"}) {
            assertRequestError(mapper.writeValueAsString(Map.of(source, id, "solverType", "unknown-solver")),
                    "INVALID_SETTING");
            assertRequestError(mapper.writeValueAsString(Map.of(source, id, "timeStep", -1.0)),
                    "INVALID_SETTING");
            assertRequestError(mapper.writeValueAsString(Map.of(source, id, "simulationTime", 0.0)),
                    "INVALID_SETTING");
        }
        assertRequestError("{\"circuitId\":", "MALFORMED_REQUEST");
        assertEquals(before, simulationCount());
    }

    @Test
    void pathSingleRequestsRetainBothTimingRequirements() throws Exception {
        Path circuit = directory.resolve("timing.ipes");
        Files.write(circuit, PreparationCircuitFixtures.resistorCircuit(10));
        int before = simulationCount();
        assertRequestError(mapper.writeValueAsString(Map.of("circuitFile", circuit.toString(),
                "timeStep", 0.125)), "INVALID_SETTING");
        assertRequestError(mapper.writeValueAsString(Map.of("circuitFile", circuit.toString(),
                "simulationTime", 0.25)), "INVALID_SETTING");
        assertEquals(before, simulationCount());
    }

    @Test
    void componentInspectionExposesOnlyVerifiedWritableConfigurationFields() throws Exception {
        String id = upload(PreparationCircuitFixtures.capacitorCircuit(), "descriptors.ipes");
        JsonNode response = mapper.readTree(mvc.perform(get("/api/v1/circuits/{id}/components", id))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode resistor = null;
        JsonNode state = null;
        for (JsonNode component : response.path("components")) {
            if ("R.1".equals(component.path("name").asText())) resistor = component;
            if ("state".equals(component.path("name").asText())) state = component;
        }
        assertNotNull(resistor, response.toString());
        assertNotNull(state, response.toString());
        assertTrue(resistor.path("configurationFields").isArray(), resistor.toString());
        assertTrue(state.path("configurationFields").isArray(), state.toString());
        JsonNode resistance = configurationField(resistor, "resistance");
        assertEquals("LK", resistance.path("domain").asText());
        assertEquals("R.1", resistance.path("component").asText());
        assertEquals("param0", resistance.path("indexedName").asText());
        configurationField(state, "capacitance");
        configurationField(state, "initVoltage");
        for (JsonNode field : state.path("configurationFields")) {
            assertFalse(java.util.Set.of("param2", "param3", "param4", "current", "voltage")
                    .contains(field.path("name").asText()), field.toString());
            assertTrue(field.path("aliases").isArray(), field.toString());
        }
    }


    @Test
    void readableMalformedPathReturnsParsingDiagnosticsWithoutPublishingAJob() throws Exception {
        Path circuit = directory.resolve("truncated.ipes");
        byte[] malformed = new byte[]{0x1f, (byte) 0x8b, 8, 0};
        Files.write(circuit, malformed);
        assertThrows(Exception.class, () -> PreparationCircuitFixtures.prepare(malformed));
        int before = simulationCount();
        assertSourceError(Map.of("circuitFile", circuit.toString(), "simulationTime", 0.25, "timeStep", 0.125),
                400, "MALFORMED_SOURCE");
        assertEquals(before, simulationCount());
    }

    @Test
    void storedBatchSettingsInheritPerFieldAndPathBatchesKeepTheirDefaults() throws Exception {
        byte[] content = PreparationCircuitFixtures.capacitorCircuit();
        assertInstanceOf(PreparationResult.Ready.class, PreparationCircuitFixtures.prepare(content));
        String id = upload(content, "batch-settings.ipes");
        mvc.perform(put("/api/v1/circuits/{id}/parameters", id).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("simulationDuration", 0.2, "timeStep", 0.1,
                                "solverType", "trapezoidal")))).andExpect(status().isOk());
        for (String source : new String[]{"circuitId", "circuitFile"}) {
            JsonNode inherited = completedResults(submitBatch(Map.of(source, id, "parameterSets", java.util.List.of(Map.of()))));
            assertTimeGrid(inherited, 3, 0.1, 0.2);
            assertEquals(0.48, inherited.path("current").get(0).asDouble(), 1e-6);
            JsonNode solver = completedResults(submitBatch(Map.of(source, id, "parameterSets", java.util.List.of(Map.of()),
                    "simulationSettings", Map.of("solverType", "gs"))));
            assertTimeGrid(solver, 3, 0.1, 0.2);
            assertEquals(0.45, solver.path("current").get(0).asDouble(), 1e-6);
            assertTimeGrid(completedResults(submitBatch(Map.of(source, id, "parameterSets", java.util.List.of(Map.of()),
                    "simulationSettings", Map.of("timeStep", 0.05)))), 5, 0.05, 0.2);
            assertTimeGrid(completedResults(submitBatch(Map.of(source, id, "parameterSets", java.util.List.of(Map.of()),
                    "simulationSettings", Map.of("simulationTime", 0.4)))), 5, 0.1, 0.4);
            int before = simulationCount();
            assertBatchRequestError(Map.of(source, id, "parameterSets", java.util.List.of(Map.of()),
                    "simulationSettings", Map.of("solverType", "unknown-solver")));
            assertBatchRequestError(Map.of(source, id, "parameterSets", java.util.List.of(Map.of()),
                    "simulationSettings", Map.of("timeStep", -1.0)));
            assertEquals(before, simulationCount());
        }
        Path path = directory.resolve("batch-defaults.ipes");
        Files.write(path, PreparationCircuitFixtures.resistorCircuit(10));
        JsonNode defaults = completedResults(submitBatch(Map.of("circuitFile", path.toString(),
                "parameterSets", java.util.List.of(Map.of()))));
        assertTimeGrid(defaults, 20001, 1e-6, 0.02);
    }

    @Test
    void numericalFailureAfterReadyValidationKeepsTheAcceptedFailedJobContract() throws Exception {
        byte[] content = PreparationCircuitFixtures.singularCircuit();
        assertInstanceOf(PreparationResult.Ready.class, PreparationCircuitFixtures.prepare(content));
        String id = upload(content, "singular.ipes");
        int before = simulationCount();
        mvc.perform(get("/api/v1/circuits/{id}/validate", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.warnings").isArray()).andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.readyForHeadless").value(true)).andExpect(jsonPath("$.outcome").value("READY"));
        assertEquals(before, simulationCount());
        String job = submit(Map.of("circuitId", id));
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> mvc.perform(get("/api/v1/simulations/{id}", job))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").isNotEmpty()));
        assertEquals(before + 1, simulationCount());
        mvc.perform(get("/api/v1/simulations/{id}/results", job)).andExpect(status().isTooEarly());
    }

    private String submitBatch(Map<String, Object> request) throws Exception {
        JsonNode response = mapper.readTree(mvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        assertEquals(1, response.path("simulationIds").size(), response.toString());
        return response.path("simulationIds").get(0).asText();
    }

    private void assertBatchRequestError(Map<String, Object> request) throws Exception {
        JsonNode response = mapper.readTree(mvc.perform(post("/api/v1/simulations/batch")
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.readyForHeadless").value(false))
                .andReturn().getResponse().getContentAsString());
        diagnostic(response, "INVALID_SETTING");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void uploadsExposeCanonicalSourceValuesForReadyAndInspectableCircuits(boolean unsupported) throws Exception {
        byte[] content = PreparationCircuitFixtures.canonicalInspectionCircuit(unsupported);
        PreparationResult prepared = PreparationCircuitFixtures.prepare(content);
        if (unsupported) {
            assertInstanceOf(PreparationResult.Inspectable.class, prepared);
            assertEquals(ReadinessReport.Outcome.UNSUPPORTED, prepared.report().outcome());
        } else {
            assertInstanceOf(PreparationResult.Ready.class, prepared);
        }
        String id = upload(content, "canonical-inspection.ipes");
        JsonNode response = mapper.readTree(mvc.perform(get("/api/v1/circuits/{id}/components", id))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode source = null;
        JsonNode resistor = null;
        for (JsonNode component : response.path("components")) {
            if ("U.1".equals(component.path("name").asText())) source = component;
            if ("R.1".equals(component.path("name").asText())) resistor = component;
        }
        assertNotNull(source, response.toString());
        assertNotNull(resistor, response.toString());
        JsonNode values = source.path("parameters");
        assertEquals(401.0, values.path("sourceType").asDouble(), source.toString());
        assertEquals(6.0, values.path("dcValue").asDouble(), source.toString());
        assertEquals(6.0, values.path("amplitude").asDouble(), source.toString());
        assertEquals(401.0, values.path("param0").asDouble(), source.toString());
        assertEquals(10.0, resistor.path("parameters").path("resistance").asDouble(), resistor.toString());
        assertEquals("param0", configurationField(source, "sourceType").path("indexedName").asText());
        configurationField(source, "dcValue");
        configurationField(source, "amplitude");
        configurationField(resistor, "resistance");
    }

    private JsonNode configurationField(JsonNode component, String name) {
        for (JsonNode field : component.path("configurationFields")) {
            if (name.equals(field.path("name").asText())) return field;
        }
        fail("Missing configuration field " + name + ": " + component);
        return null;
    }

    private void assertRequestError(String body, String code) throws Exception {
        JsonNode response = mapper.readTree(mvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.readyForHeadless").value(false))
                .andReturn().getResponse().getContentAsString());
        diagnostic(response, code);
    }

    private JsonNode completedResults(String id) throws Exception {
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> mvc.perform(get("/api/v1/simulations/{id}", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED")));
        return mapper.readTree(mvc.perform(get("/api/v1/simulations/{id}/results", id))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private void assertTimeGrid(JsonNode results, int count, double step, double duration) {
        JsonNode times = results.path("time");
        assertEquals(count, times.size(), results.toString());
        assertEquals(step, times.get(1).asDouble(), 1e-6);
        assertEquals(duration, times.get(count - 1).asDouble(), 1e-6);
    }

    private JsonNode validate(String id, String outcome) throws Exception {
        return mapper.readTree(mvc.perform(get("/api/v1/circuits/{id}/validate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.warnings").isArray())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.readyForHeadless").value(false))
                .andExpect(jsonPath("$.outcome").value(outcome))
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode rejectedJob(String id, String outcome) throws Exception {
        return mapper.readTree(mvc.perform(post("/api/v1/simulations").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("circuitId", id))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.readyForHeadless").value(false))
                .andExpect(jsonPath("$.outcome").value(outcome))
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode diagnostic(JsonNode response, String code) {
        for (JsonNode finding : response.path("diagnostics")) {
            if (code.equals(finding.path("code").asText())) return finding;
        }
        fail("Missing diagnostic " + code + ": " + response);
        return null;
    }

    private String upload(byte[] content, String filename) throws Exception {
        JsonNode uploaded = mapper.readTree(mvc.perform(multipart("/api/v1/circuits/parse")
                        .file(new MockMultipartFile("file", filename, "application/gzip", content)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String id = uploaded.path("circuitId").asText();
        assertFalse(id.isBlank(), uploaded.toString());
        return id;
    }

    private int simulationCount() throws Exception {
        return mapper.readTree(mvc.perform(get("/api/v1/simulations")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("total").asInt();
    }

    private void assertSourceError(Map<String, Object> request, int expectedStatus, String code) throws Exception {
        mvc.perform(post("/api/v1/simulations").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.readyForHeadless").value(false))
                .andExpect(jsonPath("$.diagnostics[0].code").value(code));
    }

    private String submit(Map<String, Object> request) throws Exception {
        JsonNode submitted = mapper.readTree(mvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String id = submitted.path("simulationId").asText();
        assertFalse(id.isBlank(), submitted.toString());
        return id;
    }

    private void assertCurrent(String id, double expected) throws Exception {
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> mvc.perform(get("/api/v1/simulations/{id}", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED")));
        JsonNode results = mapper.readTree(mvc.perform(get("/api/v1/simulations/{id}/results", id))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals(3, results.path("time").size(), results.toString());
        JsonNode current = results.path("current");
        assertEquals(3, current.size(), results.toString());
        for (JsonNode sample : current) assertEquals(expected, sample.asDouble(), 1e-6, results.toString());
    }
}
