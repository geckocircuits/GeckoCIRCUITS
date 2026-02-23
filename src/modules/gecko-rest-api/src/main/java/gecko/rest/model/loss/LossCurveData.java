package gecko.rest.model.loss;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Loss curve measured at a specific temperature.
 * Used for detailed loss calculations with interpolation.
 */
@Schema(description = "Loss curve measured at specific temperature")
public record LossCurveData(
    @Schema(description = "Junction temperature [°C]", example = "25.0")
    @NotNull
    Double temperature,

    @Schema(description = "Blocking voltage for switching curves [V]", example = "600.0",
            nullable = true)
    Double blockingVoltage,

    @Schema(description = "Curve data: rows are [current/voltage], [energy/voltage], [energy/voltage] for switching or [voltage], [current] for conduction",
            example = "[[0, 10, 20], [0, 0.001, 0.002]]")
    @NotNull
    double[][] data
) {}
