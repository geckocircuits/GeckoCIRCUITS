package gecko.rest.model.loss;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request for detailed loss calculation with temperature-dependent curves.
 * Uses bilinear interpolation from manufacturer datasheet curves.
 */
@Schema(description = "Request for detailed loss calculation with temperature-dependent curves")
public record DetailedLossRequest(
    @Schema(description = "Operating current [A]", example = "10.0")
    @NotNull
    Double current,

    @Schema(description = "Junction temperature [°C]", example = "125.0")
    @NotNull
    Double temperature,

    @Schema(description = "Switching loss curves (multiple temperature points)")
    @NotEmpty
    List<LossCurveData> switchingCurves,

    @Schema(description = "Conduction loss curves (multiple temperature points)")
    @NotEmpty
    List<LossCurveData> conductionCurves
) {}
