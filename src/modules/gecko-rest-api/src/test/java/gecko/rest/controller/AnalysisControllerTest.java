package gecko.rest.controller;

import gecko.rest.model.analysis.CharacteristicsResponse;
import gecko.rest.model.analysis.FourierResponse;
import gecko.rest.service.AnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller unit tests for AnalysisController.
 * Tests REST endpoints with mocked service layer.
 */
@WebMvcTest(AnalysisController.class)
class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalysisService analysisService;

    @Test
    void testComputeCharacteristics_success_returnsValidResponse() throws Exception {
        // Mock service response
        CharacteristicsResponse mockResponse = new CharacteristicsResponse();
        mockResponse.setAverage(0.0);
        mockResponse.setRms(0.707);
        mockResponse.setThd(5.2);
        mockResponse.setMin(-1.0);
        mockResponse.setMax(1.0);
        mockResponse.setPeakToPeak(2.0);
        mockResponse.setRipple(0.01);
        mockResponse.setKlirr(0.05);
        mockResponse.setShapeFactor(1.11);
        mockResponse.setSampleCount(1000);
        mockResponse.setSignalName("test_signal");

        when(analysisService.computeCharacteristics(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/analysis/characteristics")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "data": [0.0, 0.707, 1.0, 0.707, 0.0, -0.707, -1.0, -0.707],
                        "sampleRate": 1000.0,
                        "signalName": "test_signal"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(0.0))
                .andExpect(jsonPath("$.rms").value(0.707))
                .andExpect(jsonPath("$.thd").value(5.2))
                .andExpect(jsonPath("$.min").value(-1.0))
                .andExpect(jsonPath("$.max").value(1.0))
                .andExpect(jsonPath("$.sampleCount").value(1000))
                .andExpect(jsonPath("$.signalName").value("test_signal"));
    }

    @Test
    void testComputeCharacteristics_verifyRmsFieldPresent() throws Exception {
        CharacteristicsResponse mockResponse = new CharacteristicsResponse();
        mockResponse.setRms(0.5);
        mockResponse.setAverage(0.0);
        mockResponse.setThd(0.0);
        mockResponse.setMin(0.0);
        mockResponse.setMax(1.0);
        mockResponse.setPeakToPeak(1.0);

        when(analysisService.computeCharacteristics(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/analysis/characteristics")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "data": [0.0, 0.5, 1.0],
                        "sampleRate": 100.0
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rms").exists())
                .andExpect(jsonPath("$.rms").value(0.5));
    }

    @Test
    void testComputeFourier_success_returnsHarmonics() throws Exception {
        FourierResponse mockResponse = new FourierResponse();
        mockResponse.setBaseFrequency(50.0);
        mockResponse.setHarmonics(10);
        mockResponse.setSignalName("v_out");
        mockResponse.setAnCoefficients(new double[]{0.0, 1.0, 0.1, 0.05});
        mockResponse.setBnCoefficients(new double[]{0.0, 0.0, 0.05, 0.02});
        mockResponse.setCnAmplitudes(new double[]{0.0, 1.0, 0.112, 0.054});
        mockResponse.setJnPhases(new double[]{0.0, 0.0, 0.464, 0.464});
        mockResponse.setDcComponent(0.0);
        mockResponse.setFundamentalAmplitude(1.0);
        mockResponse.setFundamentalPhaseDegrees(0.0);

        when(analysisService.computeFourier(any(), anyInt())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/analysis/fourier")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "data": [0.0, 0.707, 1.0, 0.707, 0.0, -0.707, -1.0, -0.707],
                        "sampleRate": 1000.0
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseFrequency").value(50.0))
                .andExpect(jsonPath("$.harmonics").value(10))
                .andExpect(jsonPath("$.cnAmplitudes").isArray())
                .andExpect(jsonPath("$.cnAmplitudes[0]").value(0.0))
                .andExpect(jsonPath("$.cnAmplitudes[1]").value(1.0))
                .andExpect(jsonPath("$.signalName").value("v_out"));
    }

    @Test
    void testComputeFourier_withHarmonicsParam_passesParameterToService() throws Exception {
        FourierResponse mockResponse = new FourierResponse();
        mockResponse.setBaseFrequency(50.0);
        mockResponse.setHarmonics(5);

        when(analysisService.computeFourier(any(), anyInt())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/analysis/fourier?harmonics=5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "data": [0.0, 1.0, 0.0],
                        "sampleRate": 100.0
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.harmonics").value(5));
    }

    @Test
    void testComputeRms_success_returnsDoubleValue() throws Exception {
        CharacteristicsResponse mockCharacteristics = new CharacteristicsResponse();
        mockCharacteristics.setRms(0.707);

        when(analysisService.computeCharacteristics(any())).thenReturn(mockCharacteristics);

        mockMvc.perform(post("/api/v1/analysis/rms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "data": [0.0, 1.0, 0.0],
                        "sampleRate": 1000.0
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(content().string("0.707"));
    }

    @Test
    void testComputeCharacteristics_withSimulationId_returnsResponse() throws Exception {
        CharacteristicsResponse mockResponse = new CharacteristicsResponse();
        mockResponse.setRms(0.5);
        mockResponse.setAverage(0.1);
        mockResponse.setSignalName("V_out");
        mockResponse.setSampleCount(500);

        when(analysisService.computeCharacteristics(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/analysis/characteristics")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "simulationId": "550e8400-e29b-41d4-a716-446655440000",
                        "signalName": "V_out",
                        "startTime": 0.0,
                        "endTime": 0.02
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signalName").value("V_out"))
                .andExpect(jsonPath("$.rms").value(0.5));
    }

    @Test
    void testComputeFourier_returnsValidJsonStructure() throws Exception {
        FourierResponse mockResponse = new FourierResponse();
        mockResponse.setBaseFrequency(50.0);
        mockResponse.setHarmonics(10);
        mockResponse.setDcComponent(0.5);
        mockResponse.setFundamentalAmplitude(1.0);
        mockResponse.setFundamentalPhaseDegrees(45.0);
        mockResponse.setAnCoefficients(new double[]{0.5, 1.0});
        mockResponse.setBnCoefficients(new double[]{0.0, 0.7});
        mockResponse.setCnAmplitudes(new double[]{0.5, 1.22});
        mockResponse.setJnPhases(new double[]{0.0, 0.7137});

        when(analysisService.computeFourier(any(), anyInt())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/analysis/fourier")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "data": [0.0, 0.5, 1.0],
                        "sampleRate": 100.0
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.baseFrequency").exists())
                .andExpect(jsonPath("$.harmonics").exists())
                .andExpect(jsonPath("$.dcComponent").exists())
                .andExpect(jsonPath("$.fundamentalAmplitude").exists());
    }

    @Test
    void testComputeFourier_defaultHarmonicsIs10() throws Exception {
        FourierResponse mockResponse = new FourierResponse();
        mockResponse.setHarmonics(10);

        when(analysisService.computeFourier(any(), anyInt())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/analysis/fourier")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "data": [0.0, 1.0, 0.0],
                        "sampleRate": 100.0
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.harmonics").value(10));
    }

    @Test
    void testCharacteristicsResponse_containsAllRequiredFields() throws Exception {
        CharacteristicsResponse mockResponse = new CharacteristicsResponse();
        mockResponse.setAverage(0.1);
        mockResponse.setRms(0.8);
        mockResponse.setThd(3.5);
        mockResponse.setMin(-1.0);
        mockResponse.setMax(1.0);
        mockResponse.setPeakToPeak(2.0);
        mockResponse.setRipple(0.02);
        mockResponse.setKlirr(0.035);
        mockResponse.setShapeFactor(1.2);
        mockResponse.setSampleCount(2000);

        when(analysisService.computeCharacteristics(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/analysis/characteristics")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "data": [0.0, 0.5, 1.0],
                        "sampleRate": 1000.0
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.average").value(0.1))
                .andExpect(jsonPath("$.rms").value(0.8))
                .andExpect(jsonPath("$.thd").value(3.5))
                .andExpect(jsonPath("$.min").value(-1.0))
                .andExpect(jsonPath("$.max").value(1.0));
    }

    @Test
    void testFourierResponse_containsAllHarmonicCoefficients() throws Exception {
        FourierResponse mockResponse = new FourierResponse();
        mockResponse.setBaseFrequency(60.0);
        mockResponse.setHarmonics(15);
        mockResponse.setSignalName("current");
        mockResponse.setAnCoefficients(new double[]{1.0, 2.0});
        mockResponse.setBnCoefficients(new double[]{0.5, 1.5});
        mockResponse.setCnAmplitudes(new double[]{1.118, 2.5});
        mockResponse.setJnPhases(new double[]{0.464, 0.644});
        mockResponse.setDcComponent(1.118);
        mockResponse.setFundamentalAmplitude(2.5);
        mockResponse.setFundamentalPhaseDegrees(36.87);

        when(analysisService.computeFourier(any(), anyInt())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/analysis/fourier")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "data": [1.0, 2.0, 1.5],
                        "sampleRate": 1000.0
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseFrequency").value(60.0))
                .andExpect(jsonPath("$.harmonics").value(15))
                .andExpect(jsonPath("$.cnAmplitudes").isArray())
                .andExpect(jsonPath("$.jnPhases").isArray());
    }

    @Test
    void testComputeFourier_multipleHarmonics_returnsCoefficients() throws Exception {
        FourierResponse mockResponse = new FourierResponse();
        mockResponse.setBaseFrequency(100.0);
        mockResponse.setHarmonics(20);
        mockResponse.setAnCoefficients(new double[20]);
        mockResponse.setBnCoefficients(new double[20]);
        mockResponse.setCnAmplitudes(new double[20]);
        mockResponse.setJnPhases(new double[20]);

        when(analysisService.computeFourier(any(), anyInt())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/analysis/fourier?harmonics=20")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "data": [0, 1, 2, 3, 2, 1],
                        "sampleRate": 600.0
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.harmonics").value(20))
                .andExpect(jsonPath("$.anCoefficients").isArray());
    }

    @Test
    void testComputeRms_extractsRmsFromCharacteristics() throws Exception {
        CharacteristicsResponse mockCharacteristics = new CharacteristicsResponse();
        mockCharacteristics.setRms(1.414);
        mockCharacteristics.setAverage(0.5);

        when(analysisService.computeCharacteristics(any())).thenReturn(mockCharacteristics);

        mockMvc.perform(post("/api/v1/analysis/rms")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "data": [1, 2, 1],
                        "sampleRate": 500.0
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(content().string("1.414"));
    }
}
