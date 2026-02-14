package gecko.rest.controller;

import gecko.rest.model.loss.ConductionLossRequest;
import gecko.rest.model.loss.DetailedLossRequest;
import gecko.rest.model.loss.LossResponse;
import gecko.rest.model.loss.SwitchingLossRequest;
import gecko.rest.service.LossCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for semiconductor loss calculations.
 * Provides endpoints for switching, conduction, and detailed interpolation-based loss calculations.
 *
 * Uses classes from gecko-simulation-core for all calculations (GUI-free).
 */
@RestController
@RequestMapping("/api/v1/loss")
@Tag(name = "Loss Calculation", description = "Semiconductor power loss calculation endpoints")
public class LossCalculationController {

    private final LossCalculationService lossCalculationService;

    public LossCalculationController(LossCalculationService lossCalculationService) {
        this.lossCalculationService = lossCalculationService;
    }

    /**
     * Calculate switching losses using voltage/energy scaling.
     *
     * Endpoint for simple switching loss calculation based on datasheet energy values
     * at a reference voltage. Scales linearly with applied voltage.
     *
     * @param request Switching loss parameters (current, voltage, energies)
     * @return Loss response with switching energy [J]
     */
    @PostMapping("/switching")
    @Operation(
        summary = "Calculate switching losses",
        description = "Calculate semiconductor switching losses using simple voltage/energy scaling. " +
                     "Returns switching energy per event [J]. Multiply by frequency for average power [W]."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Loss calculated successfully",
            content = @Content(schema = @Schema(implementation = LossResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters"
        )
    })
    public ResponseEntity<LossResponse> calculateSwitchingLoss(
            @Valid @RequestBody SwitchingLossRequest request) {

        LossResponse response = lossCalculationService.calculateSwitchingLoss(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Calculate conduction losses using resistance model.
     *
     * Endpoint for conduction loss calculation using threshold voltage + resistance model.
     * Formula: P = I * (Vth + I * Ron)
     *
     * @param request Conduction loss parameters (current, resistance, threshold)
     * @return Loss response with conduction power [W]
     */
    @PostMapping("/conduction")
    @Operation(
        summary = "Calculate conduction losses",
        description = "Calculate semiconductor conduction losses using resistance + threshold voltage model. " +
                     "Formula: P = I * (Vth + I * Ron). Returns continuous power [W]."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Loss calculated successfully",
            content = @Content(schema = @Schema(implementation = LossResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters"
        )
    })
    public ResponseEntity<LossResponse> calculateConductionLoss(
            @Valid @RequestBody ConductionLossRequest request) {

        LossResponse response = lossCalculationService.calculateConductionLoss(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Calculate detailed losses with temperature-dependent curve interpolation.
     *
     * Endpoint for advanced loss calculation using manufacturer datasheet curves
     * at multiple temperature points. Uses bilinear interpolation for accuracy.
     *
     * @param request Detailed loss parameters with multiple temperature curves
     * @return Loss response with interpolated switching and conduction losses
     */
    @PostMapping("/detailed")
    @Operation(
        summary = "Calculate detailed losses with interpolation",
        description = "Calculate losses using temperature-dependent curve interpolation from manufacturer datasheets. " +
                     "Provide switching and conduction curves at multiple temperatures for accurate interpolation. " +
                     "Uses DetailedLossLookupTable with bilinear interpolation."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Loss calculated successfully",
            content = @Content(schema = @Schema(implementation = LossResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters (missing curves, invalid data format)"
        )
    })
    public ResponseEntity<LossResponse> calculateDetailedLoss(
            @Valid @RequestBody DetailedLossRequest request) {

        LossResponse response = lossCalculationService.calculateDetailedLoss(request);
        return ResponseEntity.ok(response);
    }
}
