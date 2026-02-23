package gecko.rest.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApiKeyAuthFilter.
 */
class ApiKeyAuthFilterTest {

    private ApiKeyAuthFilter filter;
    private ApiKeyProperties properties;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        properties = new ApiKeyProperties();
        filter = new ApiKeyAuthFilter(properties);
        filterChain = new MockFilterChain();
    }

    // Test 1: Valid API key in header → filter chain continues
    @Test
    void testValidApiKeyAllowsRequest() throws Exception {
        properties.setAuthEnabled(true);
        properties.setKeys("valid-key-123,another-key");
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/simulate");
        request.setServletPath("/api/v1/simulate");
        request.addHeader(ApiKeyAuthFilter.API_KEY_HEADER, "valid-key-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        // Chain should have been called (no 401)
        assertNotEquals(401, response.getStatus());
    }

    // Test 2: Missing API key → 401 response
    @Test
    void testMissingApiKeyReturns401() throws Exception {
        properties.setAuthEnabled(true);
        properties.setKeys("valid-key-123");
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/simulate");
        request.setServletPath("/api/v1/simulate");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
    }

    // Test 3: Invalid API key → 401 response
    @Test
    void testInvalidApiKeyReturns401() throws Exception {
        properties.setAuthEnabled(true);
        properties.setKeys("valid-key-123");
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/simulate");
        request.setServletPath("/api/v1/simulate");
        request.addHeader(ApiKeyAuthFilter.API_KEY_HEADER, "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
    }

    // Test 4: Public path /api/v1/health → passes without key
    @Test
    void testPublicPathHealthPassesWithoutKey() throws Exception {
        properties.setAuthEnabled(true);
        properties.setKeys("valid-key-123");
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        request.setServletPath("/api/v1/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNotEquals(401, response.getStatus());
    }

    // Test 5: Public path /swagger-ui/index.html → passes without key
    @Test
    void testPublicPathSwaggerUiPassesWithoutKey() throws Exception {
        properties.setAuthEnabled(true);
        properties.setKeys("valid-key-123");
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        request.setServletPath("/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNotEquals(401, response.getStatus());
    }

    // Test 6: Public path /v3/api-docs/swagger.json → passes without key
    @Test
    void testPublicPathApiDocsPassesWithoutKey() throws Exception {
        properties.setAuthEnabled(true);
        properties.setKeys("valid-key-123");
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs/swagger.json");
        request.setServletPath("/v3/api-docs/swagger.json");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNotEquals(401, response.getStatus());
    }

    // Test 7: Public path /actuator/health → passes without key
    @Test
    void testPublicPathActuatorHealthPassesWithoutKey() throws Exception {
        properties.setAuthEnabled(true);
        properties.setKeys("valid-key-123");
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setServletPath("/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNotEquals(401, response.getStatus());
    }

    // Test 8: Public path /ws → passes without key
    @Test
    void testPublicPathWebSocketPassesWithoutKey() throws Exception {
        properties.setAuthEnabled(true);
        properties.setKeys("valid-key-123");
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ws");
        request.setServletPath("/ws");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNotEquals(401, response.getStatus());
    }

    // Test 9: authEnabled=false → all requests pass regardless of key
    @Test
    void testAuthDisabledAllowsAllRequests() throws Exception {
        properties.setAuthEnabled(false);
        properties.setKeys("valid-key-123");
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/simulate");
        request.setServletPath("/api/v1/simulate");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNotEquals(401, response.getStatus());
    }

    // Test 10: Empty keys set (no keys configured) → all requests pass (open mode)
    @Test
    void testEmptyKeysSetAllowsAllRequests() throws Exception {
        properties.setAuthEnabled(true);
        properties.setKeys("");  // Empty keys
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/simulate");
        request.setServletPath("/api/v1/simulate");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertNotEquals(401, response.getStatus());
    }

    // Test 11: 401 response has JSON content-type
    @Test
    void test401ResponseHasJsonContentType() throws Exception {
        properties.setAuthEnabled(true);
        properties.setKeys("valid-key-123");
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/simulate");
        request.setServletPath("/api/v1/simulate");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
    }

    // Test 12: 401 response body contains "Unauthorized"
    @Test
    void test401ResponseBodyContainsUnauthorized() throws Exception {
        properties.setAuthEnabled(true);
        properties.setKeys("valid-key-123");
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/simulate");
        request.setServletPath("/api/v1/simulate");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains("Unauthorized"), 
            "Response body should contain 'Unauthorized', but got: " + responseBody);
    }
}
