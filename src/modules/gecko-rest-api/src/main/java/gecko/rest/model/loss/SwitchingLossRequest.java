package gecko.rest.model.loss;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request for calculating switching losses in power semiconductors.
 * Uses simple voltage/energy scaling method.
 */
@Schema(description = "Request for calculating switching losses")
public record SwitchingLossRequest(
    @Schema(description = "Switching current [A]", example = "10.0")
    @NotNull @Positive
    Double current,

    @Schema(description = "Blocking voltage [V]", example = "600.0")
    @NotNull @Positive
    Double voltage,

    @Schema(description = "Junction temperature [°C]", example = "125.0")
    @NotNull
    Double temperature,

    @Schema(description = "Reference voltage [V]", example = "600.0")
    @NotNull @Positive
    Double referenceVoltage,

    @Schema(description = "Turn-on energy at reference [J]", example = "0.001")
    @NotNull @Positive
    Double turnOnEnergy,

    @Schema(description = "Turn-off energy at reference [J]", example = "0.0015")
    @NotNull @Positive
    Double turnOffEnergy
) {}
