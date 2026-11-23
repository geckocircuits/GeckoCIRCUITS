package gecko.rest.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** Configures fixed-window request limits per client. */
@Component
@Validated
@ConfigurationProperties(prefix = "gecko.api")
public class RateLimitProperties {
    @Min(1) private int rateLimit = 100;
    @Min(1) private int rateLimitSimulation = 20;

    public int getRateLimit() { return rateLimit; }
    public void setRateLimit(int value) { rateLimit = value; }
    public int getRateLimitSimulation() { return rateLimitSimulation; }
    public void setRateLimitSimulation(int value) { rateLimitSimulation = value; }
}
