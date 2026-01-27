package ch.technokrat.gecko.rest.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check and API info endpoints.
 * Provides basic status monitoring for the GeckoCIRCUITS REST API.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.description:GeckoCIRCUITS REST API}")
    private String appDescription;

    /**
     * Health check endpoint.
     * @return Health status and version information
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("version", appVersion);
        return response;
    }

    /**
     * API information endpoint.
     * @return Application metadata
     */
    @GetMapping("/info")
    public Map<String, String> info() {
        Map<String, String> response = new HashMap<>();
        response.put("name", "GeckoCIRCUITS REST API");
        response.put("version", appVersion);
        response.put("description", appDescription);
        response.put("status", "running");
        return response;
    }

    /**
     * API documentation endpoint.
     * @return OpenAPI/Swagger documentation info
     */
    @GetMapping("/docs")
    public Map<String, String> docs() {
        Map<String, String> response = new HashMap<>();
        response.put("swagger-ui", "/swagger-ui.html");
        response.put("api-docs", "/api-docs");
        response.put("version", appVersion);
        return response;
    }
}
