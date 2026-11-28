package gecko.rest.controller;

import gecko.rest.model.SimulationResponse;
import gecko.rest.model.circuit.CircuitListResponse;
import gecko.rest.service.CircuitFileService;
import gecko.rest.service.SimulationService;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({CircuitFileController.class, SimulationController.class})
@Import(gecko.rest.config.TestSecurityConfig.class)
class ListPaginationTest {
    @Autowired private MockMvc mvc;
    @MockBean private CircuitFileService circuits;
    @MockBean private SimulationService simulations;

    private void seed(int count) {
        List<CircuitListResponse.CircuitSummary> items = IntStream.range(0, count)
                .mapToObj(i -> new CircuitListResponse.CircuitSummary("id-%03d".formatted(i),
                        "example.ipes", 1, Instant.EPOCH.plusSeconds(i).toString())).toList();
        when(circuits.getAllCircuits()).thenReturn(new CircuitListResponse(items.reversed(), count));
        Map<String, SimulationResponse> jobs = new HashMap<>();
        items.forEach(item -> {
            SimulationResponse job = new SimulationResponse(item.circuitId());
            job.setStartTime(Instant.parse(item.loadedAt()));
            job.setStatus(item.circuitId().endsWith("0") ? SimulationResponse.SimulationStatus.COMPLETED
                    : SimulationResponse.SimulationStatus.PENDING);
            jobs.put(job.getSimulationId(), job);
        });
        when(simulations.getAllSimulations()).thenReturn(jobs);
        when(simulations.getStatistics()).thenReturn(Map.of());
    }

    @BeforeEach void prepare() { seed(25); }

    @ParameterizedTest @ValueSource(strings = {"circuits", "simulations"})
    void pagesKeepExistingEnvelopeAndReturnRequestedSlice(String resource) throws Exception {
        for (String query : List.of("", "?page=0", "?page=1", "?page=99")) {
            int expected = query.contains("99") ? 0 : query.contains("=1") ? 5 : 20;
            var result = mvc.perform(get("/api/v1/" + resource + query)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(25))
                    .andExpect(jsonPath("$.page.totalElements").value(25))
                    .andExpect(jsonPath("$.page.totalPages").value(2))
                    .andExpect(jsonPath("$.page.size").value(20));
            if (resource.equals("circuits")) result.andExpect(jsonPath("$.circuits.length()").value(expected));
            else result.andExpect(jsonPath("$.simulations", aMapWithSize(expected)))
                    .andExpect(jsonPath("$.count").value(25)).andExpect(jsonPath("$.statistics").exists());
        }
    }

    @ParameterizedTest @ValueSource(strings = {"circuits", "simulations"})
    void capsPageSizeAtOneHundred(String resource) throws Exception {
        seed(125);
        var result = mvc.perform(get("/api/v1/" + resource + "?size=500"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.size").value(100));
        if (resource.equals("circuits")) result.andExpect(jsonPath("$.circuits.length()").value(100));
        else result.andExpect(jsonPath("$.simulations", aMapWithSize(100)));
    }

    @ParameterizedTest @ValueSource(strings = {"size=0", "size=-1", "page=-1", "page=bad",
            "sort=unknown,asc", "sort=id,wrong", "sort=id", "page=2147483648"})
    void invalidQueriesReturnBadRequest(String query) throws Exception {
        for (String resource : List.of("circuits", "simulations")) {
            mvc.perform(get("/api/v1/" + resource + "?" + query)).andExpect(status().isBadRequest());
        }
    }

    @Test void filterPrecedesPaginationAndSortHasStableOrder() throws Exception {
        mvc.perform(get("/api/v1/simulations?status=completed&size=2&sort=createdAt,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.simulations", aMapWithSize(2)))
                .andExpect(jsonPath("$.simulations.id-020").exists())
                .andExpect(jsonPath("$.simulations.id-010").exists());
        mvc.perform(get("/api/v1/circuits?size=1&sort=createdAt,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.circuits[0].circuitId").value("id-024"));
    }

    @Test void createdAtSortComparesInstantsWithDifferentFractionalPrecision() throws Exception {
        when(circuits.getAllCircuits()).thenReturn(new CircuitListResponse(List.of(
                new CircuitListResponse.CircuitSummary("whole", "a.ipes", 1, "2026-11-23T12:00:00Z"),
                new CircuitListResponse.CircuitSummary("fractional", "b.ipes", 1, "2026-11-23T12:00:00.1Z")), 2));
        mvc.perform(get("/api/v1/circuits?sort=createdAt,asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.circuits[0].circuitId").value("whole"));
    }
}
