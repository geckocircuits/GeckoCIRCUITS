package gecko.rest.controller;

import gecko.rest.model.loss.LossResponse;
import gecko.rest.service.LossCalculationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller unit tests for LossCalculationController.
 * Tests REST endpoints with mocked service layer.
 */
@WebMvcTest(LossCalculationController.class)
@Import(gecko.rest.config.TestSecurityConfig.class)
class LossCalculationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LossCalculationService lossCalculationService;

    @Test
    void testCalculateSwitchingLoss_success() throws Exception {
        // Mock service response
        LossResponse mockResponse = new LossResponse(0.0025, 0.0025, 0.0, "simple_switching");
        when(lossCalculationService.calculateSwitchingLoss(any())).thenReturn(mockResponse);

        // Test endpoint
        mockMvc.perform(post("/api/v1/loss/switching")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "current": 10.0,
                        "voltage": 600.0,
                        "temperature": 125.0,
                        "referenceVoltage": 600.0,
                        "turnOnEnergy": 0.001,
                        "turnOffEnergy": 0.0015
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLoss").value(0.0025))
                .andExpect(jsonPath("$.switchingLoss").value(0.0025))
                .andExpect(jsonPath("$.conductionLoss").value(0.0))
                .andExpect(jsonPath("$.method").value("simple_switching"));
    }

    @Test
    void testCalculateSwitchingLoss_invalidRequest_missingField() throws Exception {
        // Missing required field "current"
        mockMvc.perform(post("/api/v1/loss/switching")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "voltage": 600.0,
                        "temperature": 125.0,
                        "referenceVoltage": 600.0,
                        "turnOnEnergy": 0.001,
                        "turnOffEnergy": 0.0015
                    }
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCalculateConductionLoss_success() throws Exception {
        // Mock service response
        LossResponse mockResponse = new LossResponse(12.0, null, 12.0, "simple_conduction");
        when(lossCalculationService.calculateConductionLoss(any())).thenReturn(mockResponse);

        // Test endpoint
        mockMvc.perform(post("/api/v1/loss/conduction")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "current": 10.0,
                        "onResistance": 0.05,
                        "thresholdVoltage": 0.7
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLoss").value(12.0))
                .andExpect(jsonPath("$.conductionLoss").value(12.0))
                .andExpect(jsonPath("$.method").value("simple_conduction"));
    }

    @Test
    void testCalculateConductionLoss_invalidRequest_negativeResistance() throws Exception {
        // Negative resistance should fail validation
        mockMvc.perform(post("/api/v1/loss/conduction")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "current": 10.0,
                        "onResistance": -0.05,
                        "thresholdVoltage": 0.7
                    }
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCalculateDetailedLoss_success() throws Exception {
        // Mock service response
        LossResponse mockResponse = new LossResponse(7.0025, 0.0025, 7.0, "detailed_interpolation");
        when(lossCalculationService.calculateDetailedLoss(any())).thenReturn(mockResponse);

        // Test endpoint with minimal curve data
        mockMvc.perform(post("/api/v1/loss/detailed")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "current": 10.0,
                        "temperature": 25.0,
                        "switchingCurves": [{
                            "temperature": 25.0,
                            "blockingVoltage": 600.0,
                            "data": [[0, 10], [0, 0.001], [0, 0.0015]]
                        }],
                        "conductionCurves": [{
                            "temperature": 25.0,
                            "data": [[0, 0.7], [0, 10]]
                        }]
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLoss").value(7.0025))
                .andExpect(jsonPath("$.method").value("detailed_interpolation"));
    }

    @Test
    void testCalculateDetailedLoss_invalidRequest_emptyCurves() throws Exception {
        // Empty curves should fail validation
        mockMvc.perform(post("/api/v1/loss/detailed")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "current": 10.0,
                        "temperature": 25.0,
                        "switchingCurves": [],
                        "conductionCurves": []
                    }
                    """))
                .andExpect(status().isBadRequest());
    }
}
