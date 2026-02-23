package gecko.rest.model.loss;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Loss calculation result.
 * Contains total loss and breakdown into switching/conduction components.
 */
@Schema(description = "Loss calculation result")
public record LossResponse(
    @Schema(description = "Total power loss [W]", example = "12.5")
    double totalLoss,

    @Schema(description = "Switching power loss [W]", example = "2.5", nullable = true)
    Double switchingLoss,

    @Schema(description = "Conduction power loss [W]", example = "10.0", nullable = true)
    Double conductionLoss,

    @Schema(description = "Calculation method used", example = "detailed_interpolation")
    String method
) {}
