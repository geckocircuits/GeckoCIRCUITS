package gecko.rest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration properties for API key authentication.
 *
 * <p>Configure in application.properties:</p>
 * <pre>
 * gecko.api.keys=key1,key2,key3
 * gecko.api.auth-enabled=true
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "gecko.api")
public class ApiKeyProperties {

    /** Set of valid API keys. Empty set = no keys configured (auth effectively bypassed if authEnabled=false). */
    private Set<String> keys = new HashSet<>();

    /** Whether API key authentication is enforced. Default true. Set false to disable for testing. */
    private boolean authEnabled = true;

    public Set<String> getKeys() {
        return keys;
    }

    public void setKeys(Set<String> keys) {
        this.keys = keys;
    }

    // Support comma-separated values from properties
    public void setKeys(String keysStr) {
        this.keys = new HashSet<>();
        if (keysStr != null && !keysStr.isBlank()) {
            for (String k : keysStr.split(",")) {
                String trimmed = k.trim();
                if (!trimmed.isEmpty()) {
                    this.keys.add(trimmed);
                }
            }
        }
    }

    public boolean isAuthEnabled() {
        return authEnabled;
    }

    public void setAuthEnabled(boolean authEnabled) {
        this.authEnabled = authEnabled;
    }

    /**
     * Returns true if the given key is valid.
     *
     * @param key the API key to validate
     * @return true if key is valid, false otherwise
     */
    public boolean isValidKey(String key) {
        if (!authEnabled) {
            return true;
        }
        if (keys.isEmpty()) {
            return true; // No keys configured = open
        }
        return key != null && keys.contains(key);
    }
}
