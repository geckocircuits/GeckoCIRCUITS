package gecko.rest.integration;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SimulationSettingsIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private SimulationService simulations;

    @Test
    void aRealCircuitCompletesWithTheRequestedTimeGrid() throws Exception {
        Path circuit = Path.of("../gecko-simulation-core/src/test/resources/ipes/buck_simple.ipes").toAbsolutePath();
        assertTrue(Files.isRegularFile(circuit), circuit.toString());
        SimulationRequest request = new SimulationRequest(circuit.toString(), 0.002, 0.00025);
        request.setSolverType("gear");
        String submitted = mvc.perform(post("/api/v1/simulations")
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = mapper.readValue(submitted, SimulationResponse.class).getSimulationId();
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            SimulationResponse response = simulations.getSimulation(id);
            assertEquals(SimulationResponse.SimulationStatus.COMPLETED, response.getStatus(), response.getErrorMessage());
        });
        mvc.perform(get("/api/v1/simulations/{id}/results", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.time.length()").value(9))
                .andExpect(jsonPath("$.time[1]").value(0.00025))
                .andExpect(jsonPath("$.time[8]").value(0.002));
        assertTrue(simulations.getSimulation(id).getResults().entrySet().stream()
                .filter(entry -> !entry.getKey().equals("time"))
                .anyMatch(entry -> entry.getValue().length == 9));
    }
}
