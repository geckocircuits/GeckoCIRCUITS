package gecko.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * WebSocket message sent to /topic/simulations/{id} for real-time progress updates.
 */
@Schema(description = "Real-time simulation progress message (WebSocket/STOMP)")
public class SimulationProgressMessage {

    @Schema(description = "Simulation identifier")
    private String simulationId;

    @Schema(description = "Progress percentage 0-100", example = "45.0")
    private double progress;

    @Schema(description = "Current simulation time [s]", example = "0.009")
    private double currentTime;

    @Schema(description = "End time [s]", example = "0.02")
    private double endTime;

    @Schema(description = "Current time step number")
    private int step;

    @Schema(description = "Total expected time steps")
    private int totalSteps;

    @Schema(description = "Current simulation status")
    private String status;

    @Schema(description = "Message timestamp")
    private Instant timestamp;

    @Schema(description = "Error message, if status is FAILED")
    private String errorMessage;

    public SimulationProgressMessage() {
        this.timestamp = Instant.now();
    }

    public SimulationProgressMessage(String simulationId, double progress,
                                      double currentTime, double endTime,
                                      int step, int totalSteps, String status) {
        this();
        this.simulationId = simulationId;
        this.progress = progress;
        this.currentTime = currentTime;
        this.endTime = endTime;
        this.step = step;
        this.totalSteps = totalSteps;
        this.status = status;
    }

    // Getters and setters
    public String getSimulationId() { return simulationId; }
    public void setSimulationId(String simulationId) { this.simulationId = simulationId; }
    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }
    public double getCurrentTime() { return currentTime; }
    public void setCurrentTime(double currentTime) { this.currentTime = currentTime; }
    public double getEndTime() { return endTime; }
    public void setEndTime(double endTime) { this.endTime = endTime; }
    public int getStep() { return step; }
    public void setStep(int step) { this.step = step; }
    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
