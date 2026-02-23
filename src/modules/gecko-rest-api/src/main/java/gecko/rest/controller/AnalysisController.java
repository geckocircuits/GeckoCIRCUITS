package gecko.rest.controller;

import gecko.rest.model.analysis.CharacteristicsResponse;
import gecko.rest.model.analysis.FourierResponse;
import gecko.rest.model.analysis.SignalAnalysisRequest;
import gecko.rest.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for signal analysis operations.
 * Provides endpoints for Fourier analysis, characteristics computation, and related signal processing.
 *
 * Uses classes from gecko-simulation-core for all calculations (GUI-free).
 */
@RestController
@RequestMapping("/api/v1/analysis")
@Tag(name = "Signal Analysis", description = "FFT, characteristics, and harmonic analysis of simulation signals")
public class AnalysisController {

    private final AnalysisService analysisService;

    @Autowired
    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * Compute signal characteristics.
     *
     * Endpoint for calculating fundamental signal metrics: average, RMS, THD,
     * min/max, ripple factor, distortion factor, and shape factor.
     *
     * @param request Analysis request with raw data or simulation signal reference
     * @return CharacteristicsResponse with computed metrics
     */
    @PostMapping("/characteristics")
    @Operation(
        summary = "Compute signal characteristics",
        description = "Calculate AVG, RMS, THD, MIN, MAX, RIPPLE, KLIRR, SHAPE for a signal"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Characteristics computed successfully",
            content = @Content(schema = @Schema(implementation = CharacteristicsResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or insufficient data"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Simulation or signal not found"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Calculation error or out of memory"
        )
    })
    public ResponseEntity<CharacteristicsResponse> computeCharacteristics(
            @RequestBody SignalAnalysisRequest request) {
        CharacteristicsResponse response = analysisService.computeCharacteristics(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Fourier harmonic decomposition.
     *
     * Endpoint for computing harmonic coefficients (An cosine, Bn sine, Cn amplitude, Jn phase)
     * for a specified number of harmonics. Useful for analyzing periodic signals and
     * computing Total Harmonic Distortion (THD).
     *
     * @param request Analysis request with raw data or simulation signal reference
     * @param harmonics Number of harmonics to compute (0-based, default 10)
     * @return FourierResponse with An, Bn, Cn, Jn coefficients
     */
    @PostMapping("/fourier")
    @Operation(
        summary = "Fourier harmonic decomposition",
        description = "Compute An, Bn, Cn, Jn coefficients for N harmonics"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Fourier analysis completed",
            content = @Content(schema = @Schema(implementation = FourierResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request, time range, or harmonic count"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Simulation or signal not found"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Calculation error or out of memory"
        )
    })
    public ResponseEntity<FourierResponse> computeFourier(
            @RequestBody SignalAnalysisRequest request,
            @RequestParam(defaultValue = "10") int harmonics) {
        FourierResponse response = analysisService.computeFourier(request, harmonics);
        return ResponseEntity.ok(response);
    }

    /**
     * Quick RMS value computation.
     *
     * Convenience endpoint for fast RMS-only calculation.
     * Internally computes full characteristics and returns only the RMS value.
     *
     * @param request Analysis request with raw data or simulation signal reference
     * @return RMS value as a double
     */
    @PostMapping("/rms")
    @Operation(
        summary = "Compute RMS value",
        description = "Quick RMS calculation for a signal"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "RMS computed"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request or insufficient data"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Simulation or signal not found"
        )
    })
    public ResponseEntity<Double> computeRms(@RequestBody SignalAnalysisRequest request) {
        CharacteristicsResponse response = analysisService.computeCharacteristics(request);
        return ResponseEntity.ok(response.getRms());
    }
}
