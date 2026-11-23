package gecko.rest.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * HTTP filter that validates the {@code X-API-Key} header on API requests.
 *
 * <p>Public paths are allowed without authentication:
 * health endpoints, Swagger UI, and OpenAPI docs.</p>
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";
    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);

    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/api/health",
            "/api-docs",
            "/actuator/health",
            "/api/v1/health",
            "/swagger-ui",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/ws",
            "/ws-raw"
    );

    private final ApiKeyProperties apiKeyProperties;
    private final JwtService tokens;

    public ApiKeyAuthFilter(ApiKeyProperties apiKeyProperties) {
        this(apiKeyProperties, null);
    }

    public ApiKeyAuthFilter(ApiKeyProperties apiKeyProperties, JwtService tokens) {
        this.apiKeyProperties = apiKeyProperties;
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = requestPath(request);

        // Allow public paths
        if (isPublicPath(path) || path.equals("/api/v1/auth/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }

        // If auth is disabled, pass through
        if (!apiKeyProperties.isAuthEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate API key
        String authorization = request.getHeader("Authorization");
        if (authorization != null) {
            try {
                if (tokens == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    throw new org.springframework.security.oauth2.jwt.BadJwtException("A bearer token is required.");
                }
                var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
                context.setAuthentication(tokens.authenticate(authorization.substring(7)));
                org.springframework.security.core.context.SecurityContextHolder.setContext(context);
                filterChain.doFilter(request, response);
                return;
            } catch (org.springframework.security.oauth2.jwt.JwtException invalid) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"A valid access token is required.\"}");
                return;
            }
        }
        String providedKey = request.getHeader(API_KEY_HEADER);
        if (apiKeyProperties.isValidKey(providedKey)) {
            var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
            context.setAuthentication(ApiIdentity.forKey(providedKey));
            org.springframework.security.core.context.SecurityContextHolder.setContext(context);
            filterChain.doFilter(request, response);
        } else {
            log.debug("Rejected request to {} - invalid or missing API key", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Valid X-API-Key header required\"}");
        }
    }

    private boolean isPublicPath(String path) {
        for (String prefix : PUBLIC_PATH_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    static String requestPath(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.isEmpty() ? request.getRequestURI().substring(request.getContextPath().length()) : path;
    }
}
