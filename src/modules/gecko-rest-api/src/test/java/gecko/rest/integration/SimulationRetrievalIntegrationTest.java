package gecko.rest.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import gecko.rest.model.SimulationRequest;
import gecko.rest.model.SimulationResponse.SimulationStatus;
import gecko.rest.service.SimulationService;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimulationRetrievalIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private SimulationService simulations;
    private String id;

    @BeforeAll
    void runRealCircuit() {
        String circuit = Path.of("../gecko-simulation-core/src/test/resources/ipes/buck_simple.ipes")
                .toAbsolutePath().toString();
        id = simulations.submitSimulation(new SimulationRequest(circuit, 0.002, 0.000002)).getSimulationId();
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                org.junit.jupiter.api.Assertions.assertEquals(SimulationStatus.COMPLETED,
                        simulations.getSimulationStatus(id).getStatus()));
        simulations.addResults(id, "known", new double[]{0.25, 0.5, 0.75});
        simulations.addResults(id, "empty", new double[0]);
    }

    @Test
    void progressKeepsItsMetadataOnlyHttpResponse() throws Exception {
        mvc.perform(get("/api/v1/simulations/{id}/progress", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.simulationId").value(id))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progress").value(100.0))
                .andExpect(jsonPath("$.isRunning").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"known", "time", "empty"})
    void singleSignalKeepsItsHttpShapeAndSamples(String name) throws Exception {
        double[] expected = simulations.getSignalData(id, name);
        String json = mvc.perform(get("/api/v1/simulations/{id}/results/{signal}", id, name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(3)))
                .andExpect(jsonPath("$.signalName").value(name))
                .andExpect(jsonPath("$.dataPoints").value(expected.length))
                .andReturn().getResponse().getContentAsString();
        assertArrayEquals(expected, mapper.treeToValue(mapper.readTree(json).get("data"), double[].class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/progress", "/results/known"})
    void unknownJobsKeepTheirNotFoundHttpResponse(String suffix) throws Exception {
        mvc.perform(get("/api/v1/simulations/missing" + suffix)).andExpect(status().isNotFound());
    }

    @Test
    void unknownSignalKeepsItsNotFoundHttpResponse() throws Exception {
        mvc.perform(get("/api/v1/simulations/{id}/results/missing", id)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/results"})
    void explicitFullResultReadsStillIncludeAllWaveforms(String suffix) throws Exception {
        String prefix = suffix.isEmpty() ? "$.results" : "$";
        mvc.perform(get("/api/v1/simulations/" + id + suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath(prefix + ".known[0]").value(0.25))
                .andExpect(jsonPath(prefix + ".known[2]").value(0.75))
                .andExpect(jsonPath(prefix + ".time").isArray())
                .andExpect(jsonPath(prefix + ".empty").isEmpty());
    }
}
