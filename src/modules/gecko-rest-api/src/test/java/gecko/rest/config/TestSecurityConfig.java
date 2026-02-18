package gecko.rest.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test configuration that disables Spring Security for unit tests.
 *
 * <p>Use this configuration in @WebMvcTest tests to avoid authentication failures.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * @WebMvcTest(MyController.class)
 * @Import(TestSecurityConfig.class)
 * class MyControllerTest { ... }
 * </pre>
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
