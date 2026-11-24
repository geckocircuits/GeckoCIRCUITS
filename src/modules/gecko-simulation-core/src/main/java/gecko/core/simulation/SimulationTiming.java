package gecko.core.simulation;

import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shares scalar timing validation between preparation and persistent execution. */
public final class SimulationTiming {
    private static final int MAXIMUM_STEPS = Integer.MAX_VALUE - 2;
    private static final String STEP_MESSAGE = "Step width must be finite and positive.";
    private static final String DURATION_MESSAGE = "Simulation duration must be finite and positive.";

    private SimulationTiming() { }

    /**
     * Describes one invalid scalar setting without changing a circuit.
     *
     * @param parameterPath the setting name used by preparation diagnostics
     * @param message the reason the setting cannot execute
     */
    public record SettingIssue(String parameterPath, String message) { }

    /**
     * Checks both configured phases before calculator assembly or initialization.
     * A finite, nonpositive warmup time disables its unused step setting.
     *
     * @param settings the captured execution settings
     * @return immutable findings, or an empty list for valid scalar settings
     * @throws NullPointerException if settings is null
     */
    public static List<SettingIssue> settingsIssues(SolverSettingsCore settings) {
        Objects.requireNonNull(settings, "Solver settings are required.");
        var issues = new ArrayList<SettingIssue>();
        double step = settings.getStepWidth();
        double duration = settings.getSimulationDuration();
        double preTime = settings.getPreCalculationTime();
        double preStep = settings.getPreCalculationStepWidth();
        if (!positive(step)) issues.add(new SettingIssue("timeStep", STEP_MESSAGE));
        if (!positive(duration)) issues.add(new SettingIssue("duration", DURATION_MESSAGE));
        double samplePeriod = settings.getControlSamplePeriod();
        if (!Double.isFinite(samplePeriod) || samplePeriod < 0) {
            issues.add(new SettingIssue("controlSamplePeriod", "CONTROL sample period must be finite and non-negative."));
        }
        if (settings.getSolverType() == null) issues.add(new SettingIssue("solver", "A solver type is required."));
        else if (settings.isZeroCrossing() && settings.getSolverType() == SolverType.SOLVER_GS) {
            issues.add(new SettingIssue("zeroCrossing", GS_ZERO_CROSSING_MESSAGE));
        }
        int mainCapacity = capacityIssue(step, duration, "duration", issues);
        int warmupCapacity = 0;
        if (!Double.isFinite(preTime)) {
            issues.add(new SettingIssue("preSimulationTime", "Pre-calculation time must be finite."));
        } else if (preTime > 0) {
            if (!positive(preStep)) issues.add(new SettingIssue("preSimulationTimeStep", "Pre-calculation step width must be finite and positive."));
            warmupCapacity = capacityIssue(preStep, preTime, "preSimulationTime", issues);
            if (positive(duration) && !Double.isFinite(preTime + duration)) {
                issues.add(new SettingIssue("duration", "The simulation end time must be finite."));
            }
        }
        if (mainCapacity > 0 && (long) mainCapacity + warmupCapacity > MAXIMUM_STEPS) {
            issues.add(new SettingIssue("duration", "Combined simulation step count is too large."));
        }
        return List.copyOf(issues);
    }

    /**
     * Rejects invalid scalar settings without starting calculators.
     *
     * @param settings the captured execution settings
     * @throws IllegalArgumentException if a timing or solver setting is invalid
     * @throws NullPointerException if settings is null
     */
    public static void validateSettings(SolverSettingsCore settings) {
        var issues = settingsIssues(settings);
        if (!issues.isEmpty()) throw new IllegalArgumentException(issues.getFirst().message());
    }

    static void validateInitial(double step, double duration) {
        if (!positive(step)) throw new IllegalArgumentException(STEP_MESSAGE);
        if (!positive(duration)) throw new IllegalArgumentException(DURATION_MESSAGE);
        recordingCapacity(step, duration);
    }

    static int recordingCapacity(double step, double duration) {
        double steps = Math.ceil(duration / step);
        if (!Double.isFinite(steps) || steps > MAXIMUM_STEPS) {
            throw new IllegalArgumentException("Simulation step count is too large.");
        }
        return Math.max(1, (int) steps) + 1;
    }

    static int remainingSteps(double start, double end, double step, int committedSteps) {
        if (!positive(step)) throw new IllegalArgumentException(STEP_MESSAGE);
        if (!Double.isFinite(start) || start < 0 || !Double.isFinite(end) || end < 0) {
            throw new IllegalArgumentException("Simulation timestamps must be finite and non-negative.");
        }
        if (start > end) return 0;
        nextTime(start, step);
        double steps = Math.ceil((end - start) / step) + 1;
        if (!Double.isFinite(steps) || steps > MAXIMUM_STEPS - (long) committedSteps) {
            throw new IllegalArgumentException("Cumulative simulation step count is too large.");
        }
        return (int) steps;
    }

    static void validateTotalSteps(long steps) {
        if (steps > MAXIMUM_STEPS) throw new IllegalArgumentException("Combined simulation step count is too large.");
    }

    static double nextTime(double time, double step) {
        double next = time + step;
        if (!Double.isFinite(next) || !(next > time)) {
            throw new IllegalArgumentException("Step width cannot advance the absolute simulation timestamp.");
        }
        return next;
    }

    private static int capacityIssue(double step, double duration, String path, List<SettingIssue> issues) {
        if (!positive(step) || !positive(duration)) return 0;
        try { return recordingCapacity(step, duration); }
        catch (IllegalArgumentException failure) {
            issues.add(new SettingIssue(path, failure.getMessage()));
            return 0;
        }
    }

    public static final String GS_ZERO_CROSSING_MESSAGE = "Zero-crossing location does not support the Gear-Shichman solver.";

    private static boolean positive(double value) { return Double.isFinite(value) && value > 0; }
}
