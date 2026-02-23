package gecko.rest.model.loss;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request for calculating conduction losses in power semiconductors.
 * Uses resistance + threshold voltage model: P = I * (Vth + I * Ron)
 */
@Schema(description = "Request for calculating conduction losses")
public record ConductionLossRequest(
    @Schema(description = "Conduction current [A]", example = "10.0")
    @NotNull
    Double current,

    @Schema(description = "On-state resistance [Ω]", example = "0.05")
    @NotNull @Positive
    Double onResistance,

    @Schema(description = "Threshold voltage [V]", example = "0.7")
    @NotNull
    Double thresholdVoltage
) {}
