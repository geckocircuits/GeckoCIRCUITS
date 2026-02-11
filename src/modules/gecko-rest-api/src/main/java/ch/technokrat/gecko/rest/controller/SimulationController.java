package ch.technokrat.gecko.rest.controller;

import ch.technokrat.gecko.rest.model.SimulationRequest;
import ch.technokrat.gecko.rest.model.SimulationResponse;
import ch.technokrat.gecko.rest.service.SimulationService;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for circuit simulation.
 * Provides API for submitting simulations and retrieving results.
 *
 * Uses HeadlessSimulationEngine via SimulationService for actual
 * circuit simulation without any GUI dependencies.
 */
@RestController
@RequestMapping("/api/v1/simulations")
@Tag(name = "Simulations", description = "Circuit simulation endpoints")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

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

        SimulationResponse response = simulationService.submitSimulation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get simulation status and results.
     *
     * @param simulationId ID of the simulation
     * @return Simulation response with current status and results
     */
    @GetMapping("/{simulationId}")
    @Operation(summary = "Get simulation", description = "Retrieve status and results of a simulation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Simulation found",
                    content = @Content(schema = @Schema(implementation = SimulationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Simulation not found")
    })
    public ResponseEntity<SimulationResponse> getSimulation(
            @Parameter(description = "Simulation ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String simulationId) {

        SimulationResponse response = simulationService.getSimulation(simulationId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Get all results from a completed simulation.
     *
     * @param simulationId ID of the simulation
     * @return Map of signal names to their data arrays
     */
    @GetMapping("/{simulationId}/results")
    @Operation(summary = "Get simulation results", description = "Retrieve all signal data from a completed simulation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Results retrieved"),
            @ApiResponse(responseCode = "404", description = "Simulation not found"),
            @ApiResponse(responseCode = "425", description = "Simulation not yet complete")
    })
    public ResponseEntity<Map<String, double[]>> getSimulationResults(
            @Parameter(description = "Simulation ID")
            @PathVariable String simulationId) {

        SimulationResponse response = simulationService.getSimulation(simulationId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        if (response.getStatus() != SimulationResponse.SimulationStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.TOO_EARLY).build();
        }

        return ResponseEntity.ok(response.getResults());
    }

    /**
     * Get specific signal data from a completed simulation.
     *
     * @param simulationId ID of the simulation
     * @param signalName Name of the signal to retrieve
     * @return Signal data array
     */
    @GetMapping("/{simulationId}/results/{signalName}")
    @Operation(summary = "Get signal data", description = "Retrieve specific signal data from a simulation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Signal data retrieved"),
            @ApiResponse(responseCode = "425", description = "Simulation not yet complete"),
            @ApiResponse(responseCode = "404", description = "Simulation or signal not found")
    })
    public ResponseEntity<Map<String, Object>> getSignalData(
            @Parameter(description = "Simulation ID") @PathVariable String simulationId,
            @Parameter(description = "Signal name") @PathVariable String signalName) {

        SimulationResponse response = simulationService.getSimulation(simulationId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        if (response.getStatus() != SimulationResponse.SimulationStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.TOO_EARLY).build();
        }

        double[] data = simulationService.getSignalData(simulationId, signalName);
        if (data == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("signalName", signalName);
        result.put("dataPoints", data.length);
        result.put("data", data);
        return ResponseEntity.ok(result);
    }

    /**
     * Get simulation progress.
     *
     * @param simulationId ID of the simulation
     * @return Progress information
     */
    @GetMapping("/{simulationId}/progress")
    @Operation(summary = "Get simulation progress", description = "Get progress percentage for a running simulation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Progress retrieved"),
            @ApiResponse(responseCode = "404", description = "Simulation not found")
    })
    public ResponseEntity<Map<String, Object>> getSimulationProgress(
            @Parameter(description = "Simulation ID") @PathVariable String simulationId) {

        SimulationResponse response = simulationService.getSimulation(simulationId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("simulationId", simulationId);
        result.put("status", response.getStatus().toString());
        result.put("progress", simulationService.getSimulationProgress(simulationId));
        result.put("isRunning", simulationService.isRunning(simulationId));
        return ResponseEntity.ok(result);
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
        Map<String, SimulationResponse> allSimulations = simulationService.getAllSimulations();
        Map<String, SimulationResponse> filtered = new HashMap<>();

        if (status != null && !status.isEmpty()) {
            allSimulations.forEach((key, value) -> {
                if (value.getStatus().toString().equalsIgnoreCase(status)) {
                    filtered.put(key, value);
                }
            });
        } else {
            filtered.putAll(allSimulations);
        }

        response.put("total", allSimulations.size());
        response.put("count", filtered.size());
        response.put("simulations", filtered);
        response.put("statistics", simulationService.getStatistics());
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a pending/running simulation.
     *
     * @param simulationId ID of the simulation to cancel
     * @return Cancelled simulation response
     */
    @DeleteMapping("/{simulationId}")
    @Operation(summary = "Cancel simulation", description = "Cancel a pending or running simulation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Simulation cancelled"),
            @ApiResponse(responseCode = "404", description = "Simulation not found"),
            @ApiResponse(responseCode = "409", description = "Simulation already completed")
    })
    public ResponseEntity<SimulationResponse> cancelSimulation(
            @Parameter(description = "Simulation ID")
            @PathVariable String simulationId) {

        SimulationResponse response = simulationService.getSimulation(simulationId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        // Only allow cancellation of pending/running simulations
        if (response.getStatus() == SimulationResponse.SimulationStatus.COMPLETED ||
                response.getStatus() == SimulationResponse.SimulationStatus.FAILED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        response = simulationService.cancelSimulation(simulationId);
        return ResponseEntity.ok(response);
    }

    /**
     * Export simulation results as CSV.
     *
     * @param simulationId ID of the simulation
     * @return CSV formatted string
     */
    @PostMapping("/{simulationId}/export")
    @Operation(summary = "Export results", description = "Export simulation results in CSV format")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "CSV export generated"),
            @ApiResponse(responseCode = "400", description = "Invalid export format"),
            @ApiResponse(responseCode = "404", description = "Simulation not found"),
            @ApiResponse(responseCode = "425", description = "Simulation not yet complete")
    })
    public ResponseEntity<String> exportResults(
            @Parameter(description = "Simulation ID") @PathVariable String simulationId,
            @Parameter(description = "Export format") @RequestParam(defaultValue = "csv") String format) {

        if (!"csv".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest().build();
        }

        SimulationResponse response = simulationService.getSimulation(simulationId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        if (response.getStatus() != SimulationResponse.SimulationStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.TOO_EARLY).build();
        }

        // Generate CSV
        StringBuilder csv = new StringBuilder();
        Map<String, double[]> results = response.getResults();
        List<String> columns = new ArrayList<>(results.keySet());
        columns.sort(Comparator.comparing((String column) -> !"time".equals(column))
                .thenComparing(Comparator.naturalOrder()));

        // Header row
        csv.append(columns.stream()
                        .map(SimulationController::escapeCsvHeaderValue)
                        .reduce((left, right) -> left + "," + right)
                        .orElse(""))
                .append("\n");

        // Data rows
        int maxLength = results.values().stream()
                .mapToInt(arr -> arr.length)
                .max()
                .orElse(0);

        for (int i = 0; i < maxLength; i++) {
            StringBuilder row = new StringBuilder();
            boolean first = true;
            for (String key : columns) {
                if (!first) row.append(",");
                double[] data = results.get(key);
                if (i < data.length) {
                    row.append(data[i]);
                }
                first = false;
            }
            csv.append(row).append("\n");
        }

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"simulation_" + simulationId + ".csv\"")
                .body(csv.toString());
    }

    private static String escapeCsvHeaderValue(String headerValue) {
        if (headerValue == null) {
            return "";
        }
        if (headerValue.contains(",")
                || headerValue.contains("\"")
                || headerValue.contains("\n")
                || headerValue.contains("\r")) {
            return "\"" + headerValue.replace("\"", "\"\"") + "\"";
        }
        return headerValue;
    }
}
