package gecko.rest.config;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {
    private final Clock clock = mock(Clock.class);
    private final RateLimitProperties properties = new RateLimitProperties();
    private RateLimitFilter filter;

    @BeforeEach void prepare() {
        when(clock.instant()).thenReturn(Instant.parse("2026-11-23T12:00:00Z"));
        properties.setRateLimit(3);
        properties.setRateLimitSimulation(2);
        filter = new RateLimitFilter(properties, clock);
    }

    private MockHttpServletResponse request(String method, String path, String key) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        if (key != null) request.addHeader("X-API-Key", key);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(200));
        return response;
    }

    @Test void countsClientsIndependentlyAndResetsWithInjectedClock() throws Exception {
        for (int i = 1; i <= 3; i++) {
            var response = request("GET", "/api/v1/circuits", "a");
            assertEquals(200, response.getStatus());
            assertEquals(Integer.toString(3 - i), response.getHeader("X-RateLimit-Remaining"));
        }
        var exceeded = request("GET", "/api/v1/circuits", "a");
        assertEquals(429, exceeded.getStatus());
        assertEquals("60", exceeded.getHeader("Retry-After"));
        assertNotNull(exceeded.getHeader("X-RateLimit-Reset"));
        assertEquals(200, request("GET", "/api/v1/circuits", "b").getStatus());
        when(clock.instant()).thenReturn(Instant.parse("2026-11-23T12:01:01Z"));
        assertEquals("2", request("GET", "/api/v1/circuits", "a").getHeader("X-RateLimit-Remaining"));
    }

    @Test void expensiveEndpointsShareAStricterSubmissionBudget() throws Exception {
        assertEquals(200, request("POST", "/api/v1/simulations", "a").getStatus());
        assertEquals(200, request("POST", "/api/v1/simulations/batch", "a").getStatus());
        assertEquals(429, request("POST", "/api/v1/simulations", "a").getStatus());
        assertEquals(200, request("GET", "/api/v1/circuits", "a").getStatus());
    }

    @Test void noKeyUsesClientIpAndPublicPathsStayExempt() throws Exception {
        for (String path : List.of("/api/health", "/api/v1/health", "/api-docs", "/swagger-ui/index.html")) {
            for (int i = 0; i < 5; i++) assertEquals(200, request("GET", path, null).getStatus());
        }
        for (int i = 0; i < 3; i++) assertEquals(200, request("GET", "/api/v1/circuits", null).getStatus());
        assertEquals(429, request("GET", "/api/v1/circuits", null).getStatus());
    }

    @Test void concurrentRequestsCannotExceedTheBudget() throws Exception {
        try (var executor = Executors.newFixedThreadPool(8)) {
            var tasks = java.util.stream.IntStream.range(0, 20).mapToObj(i ->
                    (java.util.concurrent.Callable<Integer>) () -> request("GET", "/api/v1/circuits", "a").getStatus()).toList();
            int accepted = 0;
            for (var result : executor.invokeAll(tasks)) if (result.get() == 200) accepted++;
            assertEquals(3, accepted);
        }
    }
}
