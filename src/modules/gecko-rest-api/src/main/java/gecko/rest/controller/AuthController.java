package gecko.rest.controller;

import gecko.rest.config.ApiIdentity;
import gecko.rest.config.ApiKeyProperties;
import gecko.rest.config.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Exchanges configured API keys and refresh tokens for signed token pairs. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final ApiKeyProperties keys;
    private final JwtService tokens;

    public AuthController(ApiKeyProperties keys, JwtService tokens) {
        this.keys = keys;
        this.tokens = tokens;
    }

    @PostMapping("/token")
    @Operation(summary = "Issue an access token and refresh token for an API key",
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "ApiKey"))
    public ResponseEntity<JwtService.TokenPair> token(@RequestHeader(value = "X-API-Key", required = false) String key) {
        if (key == null || !keys.getKeys().contains(key)) throw unauthorized();
        return pair(ApiIdentity.keyId(key), List.of());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new token pair")
    public ResponseEntity<JwtService.TokenPair> refresh(@Valid @RequestBody RefreshRequest request) {
        try {
            var jwt = tokens.decode(request.refreshToken(), "refresh");
            if (keys.getKeys().stream().noneMatch(key -> ApiIdentity.keyId(key).equals(jwt.getSubject()))) {
                throw unauthorized();
            }
            return pair(jwt.getSubject(), jwt.getClaimAsStringList("roles"));
        } catch (JwtException invalid) {
            throw unauthorized();
        }
    }

    private ResponseEntity<JwtService.TokenPair> pair(String subject, List<String> roles) {
        if (!tokens.isEnabled()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "JWT authentication is disabled.");
        return ResponseEntity.ok().header("Cache-Control", "no-store").header("Pragma", "no-cache")
                .body(tokens.issue(subject, roles));
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A valid API key or refresh token is required.");
    }

    public record RefreshRequest(@NotBlank String refreshToken) { }
}
