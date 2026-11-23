package gecko.rest.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gecko.rest.config.ApiIdentity;
import gecko.rest.config.JwtService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.servlet.context-path=", "gecko.api.auth-enabled=true", "gecko.api.keys=jwt-test-key",
        "gecko.api.jwt.enabled=true", "gecko.api.jwt.secret=test-only-jwt-secret-over-32-bytes"})
@AutoConfigureMockMvc
class JwtAuthenticationIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private JwtService tokens;
    @MockBean private Clock clock;
    @LocalServerPort private int port;
    private final Instant now = Instant.parse("2026-11-23T12:00:00Z");

    @BeforeEach void setClock() { when(clock.instant()).thenReturn(now); }

    private JsonNode issue() throws Exception {
        var result = mvc.perform(post("/api/v1/auth/token").header("X-API-Key", "jwt-test-key"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    @Test void keyIssuesTokensAndBearerCanReadCircuits() throws Exception {
        var pair = issue();
        assertEquals(3600, pair.get("expiresIn").asLong());
        String access = pair.get("accessToken").asText();
        assertEquals(ApiIdentity.keyId("jwt-test-key"), tokens.decode(access, "access").getSubject());
        mvc.perform(get("/api/v1/circuits").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/token").header("X-API-Key", "invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test void refreshWorksAndAccessAndRefreshTokensCannotExchangePurposes() throws Exception {
        var pair = issue();
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json")
                .content(mapper.writeValueAsString(java.util.Map.of("refreshToken", pair.get("refreshToken").asText()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isString());
        mvc.perform(get("/api/v1/circuits").header("Authorization", "Bearer " + pair.get("refreshToken").asText()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").contentType("application/json")
                .content(mapper.writeValueAsString(java.util.Map.of("refreshToken", pair.get("accessToken").asText()))))
                .andExpect(status().isUnauthorized());
    }

    @Test void expiredOrTamperedAccessTokensReturnUnauthorized() throws Exception {
        var pair = issue();
        String access = pair.get("accessToken").asText();
        String[] parts = access.split("\\.");
        parts[2] = (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
        mvc.perform(get("/api/v1/circuits").header("Authorization", "Bearer " + String.join(".", parts)))
                .andExpect(status().isUnauthorized());
        when(clock.instant()).thenReturn(now.plusSeconds(3601));
        mvc.perform(get("/api/v1/circuits").header("Authorization", "Bearer " + access))
                .andExpect(status().isUnauthorized());
    }

    @Test void stompAcceptsAccessTokensAndRemovesTheirNativeHeader() throws Exception {
        String access = issue().get("accessToken").asText();
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        try {
            StompHeaders headers = new StompHeaders();
            headers.add("Authorization", "Bearer " + access);
            StompSession session = client.connectAsync("ws://localhost:" + port + "/ws-raw",
                    new WebSocketHttpHeaders(), headers, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
            assertTrue(session.isConnected());
            session.disconnect();
        } finally {
            client.stop();
        }
    }

    @Test void publicOpenApiDocumentsTokenEndpointsAndPagination() throws Exception {
        var result = mvc.perform(get("/api-docs")).andExpect(status().isOk()).andReturn();
        JsonNode paths = mapper.readTree(result.getResponse().getContentAsString()).get("paths");
        assertTrue(paths.has("/api/v1/auth/token"));
        assertTrue(paths.has("/api/v1/auth/refresh"));
        for (String path : List.of("/api/v1/circuits", "/api/v1/simulations")) {
            java.util.Set<String> parameters = new java.util.HashSet<>();
            paths.get(path).get("get").get("parameters").forEach(p -> parameters.add(p.get("name").asText()));
            assertTrue(parameters.containsAll(List.of("page", "size", "sort")));
        }
    }
}
