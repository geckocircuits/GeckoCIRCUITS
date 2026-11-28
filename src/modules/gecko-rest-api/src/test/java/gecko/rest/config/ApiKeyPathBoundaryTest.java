package gecko.rest.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyPathBoundaryTest {
    @Test
    void aPublicPrefixDoesNotExemptUnrelatedRoutes() throws Exception {
        ApiKeyProperties properties = new ApiKeyProperties();
        properties.setKeys("valid-key");
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter(properties);
        for (String path : new String[]{"/ws-private", "/api/v1/health-private", "/swagger-ui-private"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
            request.setServletPath(path);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            filter.doFilter(request, response, chain);
            assertEquals(401, response.getStatus(), path);
            assertNull(chain.getRequest());
        }
    }
}
