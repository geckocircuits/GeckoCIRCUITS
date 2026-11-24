package gecko.rest.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import gecko.rest.model.SimulationRequest;
import gecko.rest.model.SimulationResponse;
import gecko.rest.service.SimulationService;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** REST path of the periodic steady-state search on the buck_simple fixture. */
@SpringBootTest
@AutoConfigureMockMvc
class PeriodicSteadyStateIntegrationTest {
    private static final String CIRCUIT = Path.of("../gecko-simulation-core/src/test/resources/ipes/buck_simple.ipes")
            .toAbsolutePath().toString();
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private SimulationService simulations;

    private static SimulationRequest request(Double period, Double tolerance, Integer maxPeriods) {
        var request = new SimulationRequest(CIRCUIT, 30e-6, 1e-6);
        var search = new SimulationRequest.PeriodicSteadyState();
        search.setPeriod(period);
        search.setTolerance(tolerance);
        search.setMaxPeriods(maxPeriods);
        request.setPeriodicSteadyState(search);
        return request;
    }

    private String submit(SimulationRequest request) throws Exception {
        String submitted = mvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readValue(submitted, SimulationResponse.class).getSimulationId();
    }

    private SimulationResponse finished(String id) {
        await().atMost(30, TimeUnit.SECONDS).until(() -> {
            var status = simulations.getSimulation(id).getStatus();
            return status == SimulationResponse.SimulationStatus.COMPLETED || status == SimulationResponse.SimulationStatus.FAILED;
        });
        return simulations.getSimulation(id);
    }

    @Test
    void aConvergedSearchCompletesWithTheReportAndOnePeriodOfSamples() throws Exception {
        String id = submit(request(30e-6, null, null));
        var response = finished(id);
        assertEquals(SimulationResponse.SimulationStatus.COMPLETED, response.getStatus(), response.getErrorMessage());
        assertTrue(response.getSteadyState().converged());
        assertTrue(response.getSteadyState().periods() < 1000);
        assertEquals(30, response.getResult("time").length);
        mvc.perform(get("/api/v1/simulations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steadyState.converged").value(true))
                .andExpect(jsonPath("$.steadyState.periods").value(response.getSteadyState().periods()))
                .andExpect(jsonPath("$.steadyState.startTime").isNumber());
    }

    @Test
    void aSearchThatDoesNotConvergeFailsWithTheReport() throws Exception {
        String id = submit(request(30e-6, 1e-12, 3));
        var response = finished(id);
        assertEquals(SimulationResponse.SimulationStatus.FAILED, response.getStatus());
        assertTrue(response.getErrorMessage().contains("did not converge after 3 periods"), response.getErrorMessage());
        assertFalse(response.getSteadyState().converged());
        assertEquals(3, response.getSteadyState().periods());
        assertTrue(response.getResults().isEmpty());
    }

    @Test
    void aPeriodThatIsNotAWholeNumberOfStepsIsRejectedWithItsPath() throws Exception {
        mvc.perform(post("/api/v1/simulations").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request(30.5e-6, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.diagnostics[?(@.parameterPath=='periodicSteadyState.period')]").isNotEmpty());
    }

    @Test
    void invalidPeriodicSettingsAreRejectedAsInvalidSettingWithTheirPathsAndNoJob() throws Exception {
        int before = simulations.getAllSimulations().size();
        Object[][] cases = {
                {request(-1e-6, null, null), "periodicSteadyState.period"},
                {request(null, null, null), "periodicSteadyState.period"},
                {request(30e-6, 0.5, null), "periodicSteadyState.tolerance"},
                {request(30e-6, -1e-4, null), "periodicSteadyState.tolerance"},
                {request(30e-6, null, 2), "periodicSteadyState.maxPeriods"},
                {request(30e-6, null, 1_000_001), "periodicSteadyState.maxPeriods"}};
        for (Object[] item : cases) {
            mvc.perform(post("/api/v1/simulations").contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(item[0])))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.diagnostics[?(@.code=='INVALID_SETTING' && @.parameterPath=='" + item[1] + "')]").isNotEmpty());
        }
        assertEquals(before, simulations.getAllSimulations().size(), "no job is accepted");
    }

    @Test
    void aRequestWithoutTheFieldHasNoSteadyStateInTheSnapshot() throws Exception {
        String id = submit(new SimulationRequest(CIRCUIT, 20e-6, 1e-6));
        var response = finished(id);
        assertEquals(SimulationResponse.SimulationStatus.COMPLETED, response.getStatus(), response.getErrorMessage());
        assertNull(response.getSteadyState());
    }

    @Test
    void aSineThatDoesNotCompleteWholeCyclesIsRejectedBeforeAnyJobIsAccepted(@org.junit.jupiter.api.io.TempDir Path directory) throws Exception {
        String circuit = ExistingSimulationFixtures.ac(directory);
        int before = simulations.getAllSimulations().size();
        var bad = new SimulationRequest(circuit, 0.02, 1e-4);
        var search = new SimulationRequest.PeriodicSteadyState();
        search.setPeriod(0.0125);
        bad.setPeriodicSteadyState(search);
        mvc.perform(post("/api/v1/simulations").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.diagnostics[?(@.code=='INVALID_SETTING' && @.parameterPath=='periodicSteadyState.period')]").isNotEmpty());
        assertEquals(before, simulations.getAllSimulations().size(), "no job is accepted");
        var good = new SimulationRequest(circuit, 0.02, 1e-4);
        search.setPeriod(0.02);
        good.setPeriodicSteadyState(search);
        var response = finished(submit(good));
        assertEquals(SimulationResponse.SimulationStatus.COMPLETED, response.getStatus(), response.getErrorMessage());
        assertTrue(response.getSteadyState().converged());
    }

    @Test
    void theOpenApiSnapshotDocumentsTheRequestAndResponseFields() throws Exception {
        var live = mapper.readTree(mvc.perform(get("/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("components").path("schemas");
        var snapshot = mapper.readTree(java.nio.file.Files.readString(Path.of("../../../docs/api/openapi.json")))
                .path("components").path("schemas");
        for (String schema : new String[]{"PeriodicSteadyStateRequest", "SteadyState", "SimulationRequest", "SimulationResponse"}) {
            assertFalse(live.path(schema).isMissingNode(), schema);
            assertEquals(live.path(schema), snapshot.path(schema), "snapshot of " + schema);
        }
        assertTrue(live.path("PeriodicSteadyStateRequest").path("description").asText().contains("30 microseconds"));
        assertEquals("#/components/schemas/PeriodicSteadyStateRequest",
                live.path("SimulationRequest").path("properties").path("periodicSteadyState").path("$ref").asText());
        assertEquals("#/components/schemas/SteadyState",
                live.path("SimulationResponse").path("properties").path("steadyState").path("$ref").asText());
    }
}
