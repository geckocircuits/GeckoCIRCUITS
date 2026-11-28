package gecko.rest.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import gecko.rest.config.ApiIdentity;
import gecko.rest.config.JwtService;
import gecko.rest.service.CircuitFileService;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"gecko.api.auth-enabled=true",
        "gecko.api.keys=role-admin,role-user,role-readonly,role-default",
        "gecko.api.jwt.enabled=true", "gecko.api.jwt.secret=test-only-role-secret-over-32-bytes"})
@AutoConfigureMockMvc
class RoleAuthorizationIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private CircuitFileService circuits;
    @Autowired private JwtService tokens;
    @Autowired private gecko.rest.config.ApiKeyProperties properties;

    @DynamicPropertySource static void roles(DynamicPropertyRegistry registry) {
        for (String role : List.of("ADMIN", "USER", "READONLY")) {
            registry.add("gecko.api.roles." + ApiIdentity.keyId("role-" + role.toLowerCase(java.util.Locale.ROOT)), () -> role);
        }
    }

    static Stream<Arguments> matrix() {
        return Stream.of("ADMIN", "USER", "READONLY", "DEFAULT").flatMap(role ->
                Stream.of("GET", "POST", "DELETE").flatMap(method -> Stream.of(false, true)
                        .map(bearer -> Arguments.of(role, method, bearer))));
    }

    private String fixture() throws Exception {
        try (var stream = getClass().getResourceAsStream("/test-circuit.ipes")) {
            assertNotNull(stream);
            return Base64.getEncoder().encodeToString(stream.readAllBytes());
        }
    }

    private String bearer(String key) throws Exception {
        var result = mvc.perform(post("/api/v1/auth/token").header("X-API-Key", key))
                .andExpect(status().isOk()).andReturn();
        String token = mapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        assertEquals(ApiIdentity.keyId(key), tokens.decode(token, "access").getSubject());
        assertFalse(tokens.decode(token, "access").getClaimAsStringList("roles").isEmpty());
        return token;
    }

    @ParameterizedTest @MethodSource("matrix")
    void apiKeysAndBearerTokensEnforceTheSameRoleMatrix(String role, String method, boolean useBearer) throws Exception {
        String key = "role-" + role.toLowerCase(java.util.Locale.ROOT);
        String source = fixture();
        var loaded = circuits.loadCircuit(source, "role-matrix.ipes");
        String id = loaded.circuitId();
        var request = switch (method) {
            case "GET" -> get("/api/v1/circuits");
            case "POST" -> post("/api/v1/circuits/parse").contentType("application/json")
                    .content(mapper.writeValueAsString(java.util.Map.of("content", source, "filename", "role-post.ipes")));
            default -> delete("/api/v1/circuits/" + id);
        };
        if (useBearer) request.header("Authorization", "Bearer " + bearer(key));
        else request.header("X-API-Key", key);
        boolean allowed = method.equals("GET") || role.equals("ADMIN") || role.equals("DEFAULT")
                || (method.equals("POST") && role.equals("USER"));
        int expected = allowed ? (method.equals("GET") ? 200 : method.equals("POST") ? 201 : 204) : 403;
        mvc.perform(request).andExpect(status().is(expected));
        // This registry is in memory. Test cleanup does not alter filesystem data.
        circuits.deleteCircuit(id);
    }

    @Test void readonlyCannotUpdateRunAnalyzeOrCancel() throws Exception {
        for (var request : List.of(put("/api/v1/circuits/missing/parameters"),
                post("/api/v1/simulations"), post("/api/v1/analysis/fourier"),
                delete("/api/v1/simulations/missing"))) {
            mvc.perform(request.header("X-API-Key", "role-readonly").contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test void disabledAuthenticationActsAsAdminEvenWithAReadonlyDefault() throws Exception {
        String id = circuits.loadCircuit(fixture(), "open-mode.ipes").circuitId();
        var previous = properties.getDefaultRole();
        properties.setDefaultRole(gecko.rest.config.ApiKeyProperties.Role.READONLY);
        properties.setAuthEnabled(false);
        try {
            mvc.perform(delete("/api/v1/circuits/" + id)).andExpect(status().isNoContent());
        } finally {
            properties.setDefaultRole(previous);
            properties.setAuthEnabled(true);
        }
    }
}
