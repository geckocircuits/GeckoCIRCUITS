package gecko.rest.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;

/** Applies general and submission budgets in one atomic fixed-window admission. */
public final class RateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> EXEMPT = Set.of("/api/health", "/api/v1/health", "/actuator/health",
            "/api-docs", "/v3/api-docs", "/swagger-ui", "/swagger-ui.html", "/ws", "/ws-raw");
    private final RateLimitProperties properties;
    private final Clock clock;
    private final Map<String, Budget> clients = new HashMap<>();

    public RateLimitFilter(RateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = ApiKeyAuthFilter.requestPath(request);
        return EXEMPT.stream().anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader(ApiKeyAuthFilter.API_KEY_HEADER);
        String client = key == null || key.isBlank() ? "ip:" + request.getRemoteAddr() : "key:" + ApiIdentity.keyId(key);
        String path = ApiKeyAuthFilter.requestPath(request);
        boolean submission = request.getMethod().equals("POST")
                && (path.equals("/api/v1/simulations") || path.equals("/api/v1/simulations/batch"));
        Decision decision = admit(client, submission);
        response.setHeader("X-RateLimit-Limit", Integer.toString(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(decision.remaining()));
        response.setHeader("X-RateLimit-Reset", Long.toString(decision.reset()));
        if (!decision.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(decision.retryAfter()));
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Request limit exceeded.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private synchronized Decision admit(String client, boolean submission) {
        long now = clock.instant().getEpochSecond();
        long window = Math.floorDiv(now, 60);
        // Expired clients do not retain memory after their window ends.
        clients.values().removeIf(budget -> budget.window != window);
        Budget budget = clients.computeIfAbsent(client, ignored -> new Budget(window));
        int generalLimit = properties.getRateLimit();
        int simulationLimit = properties.getRateLimitSimulation();
        boolean allowed = budget.general < generalLimit && (!submission || budget.simulations < simulationLimit);
        if (allowed) {
            budget.general++;
            if (submission) budget.simulations++;
        }
        int limit = submission ? simulationLimit : generalLimit;
        int remaining = submission ? Math.min(generalLimit - budget.general, simulationLimit - budget.simulations)
                : generalLimit - budget.general;
        long reset = (window + 1) * 60;
        return new Decision(allowed, limit, Math.max(remaining, 0), reset, Math.max(reset - now, 1));
    }

    private static final class Budget {
        private final long window;
        private int general;
        private int simulations;
        Budget(long window) { this.window = window; }
    }

    private record Decision(boolean allowed, int limit, int remaining, long reset, long retryAfter) { }
}
