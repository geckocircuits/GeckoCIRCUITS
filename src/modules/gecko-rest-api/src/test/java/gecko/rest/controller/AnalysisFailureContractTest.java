package gecko.rest.controller;

import gecko.rest.service.AnalysisService;
import gecko.rest.service.SimulationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AnalysisFailureContractTest {
    @ParameterizedTest
    @ValueSource(strings = {"characteristics", "rms", "fourier?harmonics=3"})
    void unexpectedRetainedStorageFailureRemainsAServerError(String endpoint) throws Exception {
        var storage = mock(SimulationService.class);
        when(storage.getTimedSignalData("stored", "known"))
                .thenThrow(new IllegalStateException("Retained storage unavailable"));
        var mvc = MockMvcBuilders.standaloneSetup(new AnalysisController(new AnalysisService(storage)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/api/v1/analysis/" + endpoint).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"simulationId\":\"stored\",\"signalName\":\"known\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Analysis failed: Retained storage unavailable"));
    }
}
