package gecko.rest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Admission limits preserve existing results by rejecting additional work at capacity. */
@Component
@ConfigurationProperties(prefix = "gecko.jobs")
public class SimulationJobLimits {
    private int workers = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors() - 1));
    private int queued = 128;
    private int retained = 1024;
    private int batches = 128;
    private int streamsPerJob = 16;
    private int deliveryWorkers = 4;

    private int positive(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("Simulation job limits must be positive");
        }
        return value;
    }

    public int getWorkers() { return workers; }
    public void setWorkers(int value) { workers = positive(value); }
    public int getQueued() { return queued; }
    public void setQueued(int value) { queued = positive(value); }
    public int getRetained() { return retained; }
    public void setRetained(int value) { retained = positive(value); }
    public int getBatches() { return batches; }
    public void setBatches(int value) { batches = positive(value); }
    public int getStreamsPerJob() { return streamsPerJob; }
    public void setStreamsPerJob(int value) { streamsPerJob = positive(value); }
    public int getDeliveryWorkers() { return deliveryWorkers; }
    public void setDeliveryWorkers(int value) { deliveryWorkers = positive(value); }
}
