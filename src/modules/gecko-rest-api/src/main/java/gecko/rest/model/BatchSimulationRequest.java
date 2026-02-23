package gecko.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Request to submit a batch of simulations with parameter variations.
 *
 * Supports:
 * - Explicit list of parameter sets (each map is one simulation)
 * - Linear sweep of a single parameter
 * - Logarithmic sweep of a single parameter
 */
@Schema(description = "Batch simulation request with parameter variations")
public class BatchSimulationRequest {

    @NotBlank
    @Schema(description = "Circuit file path (.ipes)", example = "circuit.ipes")
    private String circuitFile;

    @NotNull @Min(0) @Max(100)
    @Schema(description = "Max concurrent simulations", example = "4", defaultValue = "4")
    private Integer maxConcurrent = 4;

    // One of three variation modes:

    @Size(min = 1, max = 100)
    @Schema(description = "Explicit list of parameter sets (each entry = one simulation)")
    private List<Map<String, Double>> parameterSets;

    @Schema(description = "Linear sweep configuration")
    private LinearSweep linearSweep;

    @Schema(description = "Logarithmic sweep configuration")
    private LogSweep logSweep;

    @Schema(description = "Simulation settings applied to all runs")
    private SimulationSettings simulationSettings;

    public String getCircuitFile() { return circuitFile; }
    public void setCircuitFile(String circuitFile) { this.circuitFile = circuitFile; }
    public Integer getMaxConcurrent() { return maxConcurrent; }
    public void setMaxConcurrent(Integer maxConcurrent) { this.maxConcurrent = maxConcurrent; }
    public List<Map<String, Double>> getParameterSets() { return parameterSets; }
    public void setParameterSets(List<Map<String, Double>> parameterSets) { this.parameterSets = parameterSets; }
    public LinearSweep getLinearSweep() { return linearSweep; }
    public void setLinearSweep(LinearSweep linearSweep) { this.linearSweep = linearSweep; }
    public LogSweep getLogSweep() { return logSweep; }
    public void setLogSweep(LogSweep logSweep) { this.logSweep = logSweep; }
    public SimulationSettings getSimulationSettings() { return simulationSettings; }
    public void setSimulationSettings(SimulationSettings simulationSettings) { this.simulationSettings = simulationSettings; }

    @Schema(description = "Linear parameter sweep")
    public static class LinearSweep {
        @NotBlank
        @Schema(description = "Parameter name", example = "R1.resistance")
        private String parameterPath;
        @Schema(description = "Start value", example = "1.0")
        private double startValue;
        @Schema(description = "End value", example = "100.0")
        private double endValue;
        @Min(2) @Max(1000)
        @Schema(description = "Number of points", example = "10")
        private int points;
        @Schema(description = "Additional fixed parameters for all sweep runs")
        private Map<String, Double> fixedParameters;

        public String getParameterPath() { return parameterPath; }
        public void setParameterPath(String p) { parameterPath = p; }
        public double getStartValue() { return startValue; }
        public void setStartValue(double v) { startValue = v; }
        public double getEndValue() { return endValue; }
        public void setEndValue(double v) { endValue = v; }
        public int getPoints() { return points; }
        public void setPoints(int p) { points = p; }
        public Map<String, Double> getFixedParameters() { return fixedParameters; }
        public void setFixedParameters(Map<String, Double> m) { fixedParameters = m; }
    }

    @Schema(description = "Logarithmic parameter sweep")
    public static class LogSweep {
        @NotBlank
        @Schema(description = "Parameter name", example = "C1.capacitance")
        private String parameterPath;
        @Schema(description = "Start value (must be > 0)", example = "1e-9")
        private double startValue;
        @Schema(description = "End value (must be > 0)", example = "1e-3")
        private double endValue;
        @Min(2) @Max(1000)
        @Schema(description = "Number of points", example = "10")
        private int points;
        @Schema(description = "Additional fixed parameters for all sweep runs")
        private Map<String, Double> fixedParameters;

        public String getParameterPath() { return parameterPath; }
        public void setParameterPath(String p) { parameterPath = p; }
        public double getStartValue() { return startValue; }
        public void setStartValue(double v) { startValue = v; }
        public double getEndValue() { return endValue; }
        public void setEndValue(double v) { endValue = v; }
        public int getPoints() { return points; }
        public void setPoints(int p) { points = p; }
        public Map<String, Double> getFixedParameters() { return fixedParameters; }
        public void setFixedParameters(Map<String, Double> m) { fixedParameters = m; }
    }

    @Schema(description = "Shared simulation settings for all runs in the batch")
    public static class SimulationSettings {
        @Schema(description = "Simulation duration [s]", example = "0.02")
        private Double simulationTime;
        @Schema(description = "Time step [s]", example = "1e-6")
        private Double timeStep;
        @Schema(description = "Solver type", example = "backward-euler",
                allowableValues = {"backward-euler", "trapezoidal", "gear-shichman"})
        private String solverType;

        public Double getSimulationTime() { return simulationTime; }
        public void setSimulationTime(Double v) { simulationTime = v; }
        public Double getTimeStep() { return timeStep; }
        public void setTimeStep(Double v) { timeStep = v; }
        public String getSolverType() { return solverType; }
        public void setSolverType(String v) { solverType = v; }
    }
}
