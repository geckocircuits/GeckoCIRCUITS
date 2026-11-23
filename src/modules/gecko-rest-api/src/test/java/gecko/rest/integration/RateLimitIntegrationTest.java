package gecko.rest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"gecko.api.rate-limit=3", "gecko.api.rate-limit-simulation=2",
        "gecko.api.auth-enabled=true", "gecko.api.keys=rate-a,rate-b"})
@AutoConfigureMockMvc
class RateLimitIntegrationTest {
    @Autowired private MockMvc mvc;

    @Test void registeredFilterCountsEachRequestOnceAndKeepsPublicEndpointsOpen() throws Exception {
        for (int i = 1; i <= 3; i++) {
            mvc.perform(get("/api/v1/circuits").header("X-API-Key", "rate-a"))
                    .andExpect(status().isOk()).andExpect(header().string("X-RateLimit-Limit", "3"))
                    .andExpect(header().string("X-RateLimit-Remaining", Integer.toString(3 - i)));
        }
        mvc.perform(get("/api/v1/circuits").header("X-API-Key", "rate-a"))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        mvc.perform(get("/api/v1/circuits").header("X-API-Key", "rate-b"))
                .andExpect(status().isOk()).andExpect(header().string("X-RateLimit-Remaining", "2"));
        for (int i = 0; i < 5; i++) mvc.perform(get("/api/health")).andExpect(status().isOk());
    }
}
