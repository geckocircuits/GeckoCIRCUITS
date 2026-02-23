package gecko.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Status summary for a previously submitted batch of simulations.
 */
@Schema(description = "Batch job status summary")
public class BatchJobStatus {

    @Schema(description = "Batch identifier")
    private String batchId;

    @Schema(description = "Total simulations in batch")
    private int total;

    @Schema(description = "Number completed successfully")
    private int completed;

    @Schema(description = "Number failed")
    private int failed;

    @Schema(description = "Number currently running")
    private int running;

    @Schema(description = "Number pending")
    private int pending;

    @Schema(description = "Overall progress 0-100")
    private double overallProgress;

    @Schema(description = "True when all simulations have finished (COMPLETED or FAILED)")
    private boolean done;

    @Schema(description = "Batch submission time")
    private Instant submittedAt;

    @Schema(description = "Per-simulation status: simulationId -> status string")
    private Map<String, String> simulationStatuses;

    @Schema(description = "IDs of simulations that failed, if any")
    private List<String> failedIds;

    public BatchJobStatus() {}

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getCompleted() { return completed; }
    public void setCompleted(int completed) { this.completed = completed; }

    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }

    public int getRunning() { return running; }
    public void setRunning(int running) { this.running = running; }

    public int getPending() { return pending; }
    public void setPending(int pending) { this.pending = pending; }

    public double getOverallProgress() { return overallProgress; }
    public void setOverallProgress(double overallProgress) { this.overallProgress = overallProgress; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Map<String, String> getSimulationStatuses() { return simulationStatuses; }
    public void setSimulationStatuses(Map<String, String> simulationStatuses) { this.simulationStatuses = simulationStatuses; }

    public List<String> getFailedIds() { return failedIds; }
    public void setFailedIds(List<String> failedIds) { this.failedIds = failedIds; }
}
