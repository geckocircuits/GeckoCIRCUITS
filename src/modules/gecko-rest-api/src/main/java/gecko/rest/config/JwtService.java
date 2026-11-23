package gecko.rest.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

/** Issues purpose-bound tokens and validates signature, issuer, and expiry. */
@Service
public class JwtService {
    private final JwtProperties properties;
    private final Clock clock;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        if (!properties.isEnabled()) {
            encoder = null;
            decoder = null;
            return;
        }
        byte[] secret = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32 || properties.getAccessTtl().getSeconds() < 1
                || properties.getRefreshTtl().getSeconds() < 1) {
            throw new IllegalStateException("JWT requires a secret of at least 32 bytes and positive token lifetimes.");
        }
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secret));
        var verifier = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(secret, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setClock(clock);
        verifier.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps,
                new JwtIssuerValidator("geckocircuits")));
        decoder = verifier;
    }

    public boolean isEnabled() { return properties.isEnabled(); }

    public TokenPair issue(String subject, List<String> roles) {
        requireEnabled();
        return new TokenPair(encode(subject, roles, "access", properties.getAccessTtl()),
                encode(subject, roles, "refresh", properties.getRefreshTtl()), properties.getAccessTtl().getSeconds());
    }

    private String encode(String subject, List<String> roles, String purpose, Duration lifetime) {
        var now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("geckocircuits").subject(subject).issuedAt(now)
                .expiresAt(now.plus(lifetime)).id(UUID.randomUUID().toString())
                .claim("roles", roles).claim("token_use", purpose).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public Jwt decode(String token, String purpose) {
        requireEnabled();
        Jwt jwt = decoder.decode(token);
        if (!purpose.equals(jwt.getClaimAsString("token_use")) || jwt.getSubject() == null
                || jwt.getSubject().isBlank() || jwt.getExpiresAt() == null
                || !clock.instant().isBefore(jwt.getExpiresAt())) {
            throw new BadJwtException("Token purpose, subject, or expiry is invalid.");
        }
        return jwt;
    }

    public UsernamePasswordAuthenticationToken authenticate(String token) {
        Jwt jwt = decode(token, "access");
        return UsernamePasswordAuthenticationToken.authenticated(jwt.getSubject(), null, List.of());
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) throw new BadJwtException("JWT authentication is disabled.");
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresIn) { }
}
