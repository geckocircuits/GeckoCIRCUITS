package ch.technokrat.gecko.rest.controller;

import ch.technokrat.gecko.rest.model.SimulationRequest;
import ch.technokrat.gecko.rest.model.SimulationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST endpoints for circuit simulation.
 * Provides API for submitting simulations and retrieving results.
 */
@RestController
@RequestMapping("/api/v1/simulations")
@Tag(name = "Simulations", description = "Circuit simulation endpoints")
public class SimulationController {

    // Simple in-memory storage for demo purposes
    private final Map<String, SimulationResponse> simulations = new ConcurrentHashMap<>();

    /**
     * Submit a new circuit simulation.
     *
     * @param request Simulation request with circuit file and parameters
     * @return Created simulation response with ID
     */
    @PostMapping
    @Operation(summary = "Submit simulation", description = "Submit a new circuit simulation job")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Simulation submitted successfully",
                    content = @Content(schema = @Schema(implementation = SimulationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<SimulationResponse> submitSimulation(
            @Valid @RequestBody SimulationRequest request) {

        String simulationId = UUID.randomUUID().toString();
        SimulationResponse response = new SimulationResponse(simulationId);
        response.setStatus(SimulationResponse.SimulationStatus.PENDING);
        response.setStartTime(Instant.now());

        // Store for later retrieval
        simulations.put(simulationId, response);

        // Simulate execution (in real implementation, this would queue async job)
        executeSimulation(response, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get simulation status and results.
     *
     * @param simulationId ID of the simulation
     * @return Simulation response with current status and results
     */
    @GetMapping("/{simulationId}")
    @Operation(summary = "Get simulation result", description = "Retrieve status and results of a simulation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Simulation found",
                    content = @Content(schema = @Schema(implementation = SimulationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Simulation not found")
    })
    public ResponseEntity<SimulationResponse> getSimulation(
            @Parameter(description = "Simulation ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String simulationId) {

        SimulationResponse response = simulations.get(simulationId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * List all simulations (with optional status filter).
     *
     * @param status Optional status filter
     * @return Map of all simulations
     */
    @GetMapping
    @Operation(summary = "List simulations", description = "List all simulations with optional status filter")
    @ApiResponse(responseCode = "200", description = "List of simulations")
    public ResponseEntity<Map<String, Object>> listSimulations(
            @Parameter(description = "Filter by status", example = "COMPLETED")
            @RequestParam(required = false) String status) {

        Map<String, Object> response = new HashMap<>();
        Map<String, SimulationResponse> filtered = new HashMap<>();

        if (status != null && !status.isEmpty()) {
            simulations.forEach((key, value) -> {
                if (value.getStatus().toString().equals(status.toUpperCase())) {
                    filtered.put(key, value);
                }
            });
        } else {
            filtered.putAll(simulations);
        }

        response.put("total", simulations.size());
        response.put("count", filtered.size());
        response.put("simulations", filtered);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a pending simulation.
     *
     * @param simulationId ID of the simulation to cancel
     * @return Cancelled simulation response
     */
    @DeleteMapping("/{simulationId}")
    @Operation(summary = "Cancel simulation", description = "Cancel a pending simulation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Simulation cancelled"),
            @ApiResponse(responseCode = "404", description = "Simulation not found")
    })
    public ResponseEntity<SimulationResponse> cancelSimulation(
            @Parameter(description = "Simulation ID")
            @PathVariable String simulationId) {

        SimulationResponse response = simulations.get(simulationId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        // Only allow cancellation of pending/running simulations
        if (response.getStatus() == SimulationResponse.SimulationStatus.COMPLETED ||
            response.getStatus() == SimulationResponse.SimulationStatus.FAILED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        response.setStatus(SimulationResponse.SimulationStatus.FAILED);
        response.setErrorMessage("Simulation cancelled by user");
        response.setEndTime(Instant.now());

        return ResponseEntity.ok(response);
    }

    /**
     * Mock simulation execution (synchronous for MVP).
     * In production, this would queue async job and return immediately.
     */
    private void executeSimulation(SimulationResponse response, SimulationRequest request) {
        try {
            response.setStatus(SimulationResponse.SimulationStatus.RUNNING);

            // Mock execution: simulate computation time
            Thread.sleep(100);

            // Generate mock results
            int timePoints = (int) (request.getSimulationTime() / request.getTimeStep());
            double[] timeData = new double[timePoints];
            double[] voltageData = new double[timePoints];
            double[] currentData = new double[timePoints];

            for (int i = 0; i < timePoints; i++) {
                timeData[i] = i * request.getTimeStep();
                // Mock signal: sinusoidal voltage
                voltageData[i] = 5.0 * Math.sin(2 * Math.PI * timeData[i]);
                // Mock signal: sinusoidal current with phase shift
                currentData[i] = 1.0 * Math.sin(2 * Math.PI * timeData[i] - Math.PI / 4);
            }

            response.addResult("time", timeData);
            response.addResult("voltage", voltageData);
            response.addResult("current", currentData);

            response.setStatus(SimulationResponse.SimulationStatus.COMPLETED);
            response.setEndTime(Instant.now());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.setStatus(SimulationResponse.SimulationStatus.FAILED);
            response.setErrorMessage("Simulation interrupted: " + e.getMessage());
            response.setEndTime(Instant.now());
        } catch (Exception e) {
            response.setStatus(SimulationResponse.SimulationStatus.FAILED);
            response.setErrorMessage("Simulation failed: " + e.getMessage());
            response.setEndTime(Instant.now());
        }
    }
}
