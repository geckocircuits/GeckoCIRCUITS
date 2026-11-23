package gecko.rest.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import java.time.Clock;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the REST API.
 *
 * <p>Uses a stateless API-key-based approach: no sessions, no form login,
 * CSRF disabled (not relevant for stateless APIs), CORS enabled.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ApiKeyProperties apiKeyProperties;

    @Autowired
    public SecurityConfig(ApiKeyProperties apiKeyProperties) {
        this.apiKeyProperties = apiKeyProperties;
    }

    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter(JwtService tokens) {
        return new ApiKeyAuthFilter(apiKeyProperties, tokens);
    }

    @Bean
    public Clock apiClock() { return Clock.systemUTC(); }

    @Bean
    public io.swagger.v3.oas.models.OpenAPI apiDocumentation() {
        var components = new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("ApiKey", new io.swagger.v3.oas.models.security.SecurityScheme()
                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.APIKEY)
                        .in(io.swagger.v3.oas.models.security.SecurityScheme.In.HEADER).name("X-API-Key"))
                .addSecuritySchemes("BearerAuth", new io.swagger.v3.oas.models.security.SecurityScheme()
                        .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                        .scheme("bearer").bearerFormat("JWT"));
        return new io.swagger.v3.oas.models.OpenAPI().components(components)
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("ApiKey"))
                .addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("BearerAuth"));
    }

    @Bean
    public org.springdoc.core.customizers.OpenApiCustomizer publicApiOperations() {
        return api -> {
            for (String path : java.util.List.of("/api/health", "/api/v1/auth/refresh")) {
                var item = api.getPaths().get(path);
                if (item != null) item.readOperations().forEach(operation -> operation.setSecurity(java.util.List.of()));
            }
        };
    }

    @Bean
    public RateLimitFilter rateLimitFilter(RateLimitProperties properties, Clock clock) {
        return new RateLimitFilter(properties, clock);
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyRegistration(ApiKeyAuthFilter filter) {
        FilterRegistrationBean<ApiKeyAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimitFilter rateLimitFilter,
            ApiKeyAuthFilter authenticationFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health",
                    "/api/v1/health",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/ws/**",
                    "/ws-raw/**"
                ).permitAll()
                .anyRequest().permitAll()  // Our ApiKeyAuthFilter handles actual auth
            )
            .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, ApiKeyAuthFilter.class);

        return http.build();
    }
}
