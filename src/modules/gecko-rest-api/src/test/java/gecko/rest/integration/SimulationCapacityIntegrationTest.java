package gecko.rest.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import gecko.rest.model.SimulationRequest;
import gecko.rest.service.SimulationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import gecko.rest.model.SimulationResponse.SimulationStatus;

@SpringBootTest(properties = "gecko.jobs.retained=1")
@AutoConfigureMockMvc
class SimulationCapacityIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private SimulationService simulations;

    @Test
    void configuredCapacityReturns429AndKeepsCompletedResultsReadable() throws Exception {
        String circuit = Path.of("../gecko-simulation-core/src/test/resources/ipes/buck_simple.ipes").toAbsolutePath().toString();
        String request = mapper.writeValueAsString(new SimulationRequest(circuit, 0.002, 0.00025));
        String response = mvc.perform(post("/api/v1/simulations").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = mapper.readTree(response).get("simulationId").asText();
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertEquals(SimulationStatus.COMPLETED,
                simulations.getSimulation(id).getStatus()));
        mvc.perform(post("/api/v1/simulations").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("status").value(429));
        mvc.perform(get("/api/v1/simulations/{id}/results", id)).andExpect(status().isOk())
                .andExpect(jsonPath("time.length()").value(9));
        assertEquals(1, simulations.getAllSimulations().size());
    }
}
