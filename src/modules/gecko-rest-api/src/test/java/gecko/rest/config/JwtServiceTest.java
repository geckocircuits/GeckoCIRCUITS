package gecko.rest.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtServiceTest {
    private JwtProperties properties(String secret) {
        JwtProperties properties = new JwtProperties();
        properties.setEnabled(true);
        properties.setSecret(secret);
        return properties;
    }

    @Test void startupRejectsMissingOrShortSigningSecrets() {
        for (String secret : List.of("", "short", "x".repeat(31))) {
            new ApplicationContextRunner().withBean(JwtProperties.class, () -> properties(secret))
                    .withBean(Clock.class, Clock::systemUTC).withBean(JwtService.class)
                    .run(context -> assertNotNull(context.getStartupFailure()));
        }
    }

    @Test void signedTokensUseTheInjectedClockAndCannotExchangePurposes() {
        Clock clock = mock(Clock.class);
        Instant issued = Instant.parse("2026-11-23T12:00:00Z");
        when(clock.instant()).thenReturn(issued);
        JwtService service = new JwtService(properties("test-signing-secret-at-least-32-bytes"), clock);
        String subject = ApiIdentity.keyId("test-api-key");
        var pair = service.issue(subject, List.of());
        var access = service.decode(pair.accessToken(), "access");
        assertEquals(subject, access.getSubject());
        assertEquals("geckocircuits", access.getClaimAsString("iss"));
        assertEquals(issued.plusSeconds(3600), access.getExpiresAt());
        assertEquals(List.of(), access.getClaimAsStringList("roles"));
        assertEquals(3600, pair.expiresIn());
        assertEquals(issued.plusSeconds(7 * 86400), service.decode(pair.refreshToken(), "refresh").getExpiresAt());
        assertThrows(JwtException.class, () -> service.authenticate(pair.refreshToken()));
        assertThrows(JwtException.class, () -> service.decode(pair.accessToken(), "refresh"));
        var other = new JwtService(properties("different-test-signing-secret-over-32-bytes"), clock);
        assertThrows(JwtException.class, () -> other.authenticate(pair.accessToken()));
        when(clock.instant()).thenReturn(issued.plusSeconds(3600));
        assertThrows(JwtException.class, () -> service.authenticate(pair.accessToken()));
        assertEquals(subject, service.decode(pair.refreshToken(), "refresh").getSubject());
        when(clock.instant()).thenReturn(issued.plusSeconds(7 * 86400 + 1));
        assertThrows(JwtException.class, () -> service.decode(pair.refreshToken(), "refresh"));
    }
}
