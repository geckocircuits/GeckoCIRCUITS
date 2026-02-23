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
            "/actuator/health",
            "/api/v1/health",
            "/swagger-ui",
            "/v3/api-docs",
            "/ws",
            "/ws-raw"
    );

    private final ApiKeyProperties apiKeyProperties;

    public ApiKeyAuthFilter(ApiKeyProperties apiKeyProperties) {
        this.apiKeyProperties = apiKeyProperties;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // Allow public paths
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // If auth is disabled, pass through
        if (!apiKeyProperties.isAuthEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate API key
        String providedKey = request.getHeader(API_KEY_HEADER);
        if (apiKeyProperties.isValidKey(providedKey)) {
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
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
