package gecko.rest.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configures optional HS256 token authentication. */
@Component
@ConfigurationProperties(prefix = "gecko.api.jwt")
public class JwtProperties {
    private boolean enabled;
    private String secret = "";
    private Duration accessTtl = Duration.ofHours(1);
    private Duration refreshTtl = Duration.ofDays(7);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public String getSecret() { return secret; }
    public void setSecret(String value) { secret = value; }
    public Duration getAccessTtl() { return accessTtl; }
    public void setAccessTtl(Duration value) { accessTtl = value; }
    public Duration getRefreshTtl() { return refreshTtl; }
    public void setRefreshTtl(Duration value) { refreshTtl = value; }
}
