package ch.technokrat.gecko.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Health", description = "Health check and API information endpoints")
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
    @Operation(summary = "Health check", description = "Check API health status")
    @ApiResponse(responseCode = "200", description = "API is healthy",
            content = @Content(schema = @Schema(example = "{\"status\":\"UP\",\"version\":\"1.0.0\"}")))
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
    @Operation(summary = "API information", description = "Get API metadata and version info")
    @ApiResponse(responseCode = "200", description = "API information")
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
    @Operation(summary = "Documentation links", description = "Get links to API documentation")
    @ApiResponse(responseCode = "200", description = "Documentation endpoints")
    public Map<String, String> docs() {
        Map<String, String> response = new HashMap<>();
        response.put("swagger-ui", "/swagger-ui.html");
        response.put("api-docs", "/api-docs");
        response.put("version", appVersion);
        return response;
    }
}
