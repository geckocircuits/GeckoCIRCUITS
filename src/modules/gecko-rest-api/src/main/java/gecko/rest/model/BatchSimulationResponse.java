package gecko.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Response for a batch simulation submission.
 * Contains the batch identifier, list of individual simulation IDs, and metadata.
 */
@Schema(description = "Response for a batch simulation submission")
public class BatchSimulationResponse {

    @Schema(description = "Unique batch identifier")
    private String batchId;

    @Schema(description = "Total number of simulations in the batch")
    private int totalSimulations;

    @Schema(description = "IDs of submitted simulations")
    private List<String> simulationIds;

    @Schema(description = "Batch submission time")
    private Instant submittedAt;

    @Schema(description = "Maximum concurrent simulations")
    private int maxConcurrent;

    @Schema(description = "Status message")
    private String message;

    public BatchSimulationResponse() {}

    public BatchSimulationResponse(String batchId, List<String> simulationIds, int maxConcurrent) {
        this.batchId = batchId;
        this.simulationIds = simulationIds;
        this.totalSimulations = simulationIds.size();
        this.maxConcurrent = maxConcurrent;
        this.submittedAt = Instant.now();
        this.message = "Batch of " + simulationIds.size() + " simulations submitted successfully";
    }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public int getTotalSimulations() { return totalSimulations; }
    public void setTotalSimulations(int totalSimulations) { this.totalSimulations = totalSimulations; }
    public List<String> getSimulationIds() { return simulationIds; }
    public void setSimulationIds(List<String> simulationIds) { this.simulationIds = simulationIds; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public int getMaxConcurrent() { return maxConcurrent; }
    public void setMaxConcurrent(int maxConcurrent) { this.maxConcurrent = maxConcurrent; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
