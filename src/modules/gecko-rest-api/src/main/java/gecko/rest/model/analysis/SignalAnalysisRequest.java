package gecko.rest.model.analysis;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Request for signal analysis on raw data or a stored simulation signal.
 *
 * <p>Either supply raw {@code data} with {@code sampleRate}, or reference a stored
 * simulation via {@code simulationId} + {@code signalName} with optional time bounds.</p>
 * <p>Stored signals use their recorded timestamps. A supplied sample rate does not replace that clock.</p>
 */
@Schema(description = "Signal analysis request")
public class SignalAnalysisRequest {

    @Schema(description = "Raw signal data array", example = "[0.0, 1.0, 0.0, -1.0]")
    @Size(min = 2, max = 1_000_000)
    private double[] data;

    @Schema(description = "Sample rate in Hz (required for raw data, ignored for stored signals)", example = "1000000.0")
    private Double sampleRate;

    @Schema(description = "Simulation ID for server-side signal lookup", example = "550e8400-e29b-41d4-a716-446655440000")
    private String simulationId;

    @Schema(description = "Signal name for server-side lookup", example = "V_out")
    private String signalName;

    @Schema(description = "Stored-signal window start in seconds (defaults to the first recorded timestamp)", example = "0.0")
    private Double startTime;

    @Schema(description = "Stored-signal window end in seconds (defaults to the last recorded timestamp)", example = "0.02")
    private Double endTime;

    // getters/setters
    public double[] getData() { return data; }
    public void setData(double[] data) { this.data = data; }
    public Double getSampleRate() { return sampleRate; }
    public void setSampleRate(Double sampleRate) { this.sampleRate = sampleRate; }
    public String getSimulationId() { return simulationId; }
    public void setSimulationId(String simulationId) { this.simulationId = simulationId; }
    public String getSignalName() { return signalName; }
    public void setSignalName(String signalName) { this.signalName = signalName; }
    public Double getStartTime() { return startTime; }
    public void setStartTime(Double startTime) { this.startTime = startTime; }
    public Double getEndTime() { return endTime; }
    public void setEndTime(Double endTime) { this.endTime = endTime; }
}
