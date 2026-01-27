package ch.technokrat.gecko.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point for GeckoCIRCUITS REST API.
 * Provides HTTP endpoints for circuit simulation and analysis.
 */
@SpringBootApplication
public class GeckoRestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeckoRestApiApplication.class, args);
    }
}
