package ch.technokrat.gecko.rest.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for HealthController endpoints.
 * Uses MockMvc to test the controller layer in isolation.
 */
@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void health_returnsUpStatus() throws Exception {
        mockMvc.perform(get("/api/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").exists());
    }

    @Test
    void info_returnsApiMetadata() throws Exception {
        mockMvc.perform(get("/api/info")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("GeckoCIRCUITS REST API"))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.status").value("running"));
    }

    @Test
    void docs_returnsDocumentationLinks() throws Exception {
        mockMvc.perform(get("/api/docs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.swagger-ui").value("/swagger-ui.html"))
                .andExpect(jsonPath("$.api-docs").value("/api-docs"));
    }
}
