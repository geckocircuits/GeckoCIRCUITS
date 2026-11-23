package gecko.rest.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/** Maps credentials to stable identifiers without retaining raw credentials. */
public final class ApiIdentity {
    private ApiIdentity() { }

    public static String keyId(String key) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable.", error);
        }
    }

    static UsernamePasswordAuthenticationToken forKey(String key) {
        return UsernamePasswordAuthenticationToken.authenticated(key == null ? "open" : keyId(key), null, List.of());
    }
}
