package gecko.core.simulation;

import gecko.core.allg.AdaptiveStepSettings;
import gecko.core.allg.PeriodicSteadyStateSettings;
import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Shares timing validation between preparation and persistent execution. It covers scalar timing settings and the
 * periodic steady-state settings, including the forcing check on an assembled circuit.
 */
public final class SimulationTiming {
    private static final double WHOLE_STEP = 1e-9;
    private static final int MAXIMUM_STEPS = Integer.MAX_VALUE - 2;
    private static final String STEP_MESSAGE = "Step width must be finite and positive.";
    private static final String DURATION_MESSAGE = "Simulation duration must be finite and positive.";
    /** The reason the adaptive step rejects the Gear-Shichman solver, shared by settings validation and the solver. */
    static final String GS_ADAPTIVE_MESSAGE = "The adaptive step does not support the Gear-Shichman solver.";

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
        issues.addAll(periodicSteadyStateIssues(settings.getPeriodicSteadyState(), step, duration, preTime, samplePeriod));
        issues.addAll(adaptiveStepIssues(settings.getAdaptiveStep(), settings.getSolverType(), step, preTime, preStep));
        return List.copyOf(issues);
    }

    /**
     * Checks the adaptive-step settings against the grid steps, before any allocation.
     * An explicit clamp must be finite and positive, the smallest step cannot exceed the largest, and the
     * largest cannot exceed the main grid step or, when a warmup exists, the warmup grid step.
     *
     * @param adaptive the settings, or null for off
     * @param solver the solver type, which cannot be Gear-Shichman
     * @param step the main grid step in seconds
     * @param preTime the pre-calculation time, zero or negative for none
     * @param preStep the warmup grid step in seconds
     * @return one issue per violation, each with its parameter path
     */
    public static List<SettingIssue> adaptiveStepIssues(AdaptiveStepSettings adaptive, SolverType solver, double step,
            double preTime, double preStep) {
        var issues = new ArrayList<SettingIssue>();
        if (adaptive == null) return issues;
        if (solver == SolverType.SOLVER_GS) issues.add(new SettingIssue("adaptiveStep", GS_ADAPTIVE_MESSAGE));
        if (!(adaptive.tolerance() > 0 && adaptive.tolerance() <= AdaptiveStepSettings.MAX_TOLERANCE)) {
            issues.add(new SettingIssue("adaptiveStep.tolerance", "Tolerance must be above 0 and at most 0.1."));
        }
        boolean minValid = Double.isNaN(adaptive.minStep()) || positive(adaptive.minStep());
        boolean maxValid = Double.isNaN(adaptive.maxStep()) || positive(adaptive.maxStep());
        if (!minValid) issues.add(new SettingIssue("adaptiveStep.minStep", "Smallest step must be finite and positive."));
        if (!maxValid) issues.add(new SettingIssue("adaptiveStep.maxStep", "Largest step must be finite and positive."));
        if (!positive(step)) return issues;
        if (minValid && maxValid && adaptive.resolvedMinStep(step) > adaptive.resolvedMaxStep(step)) {
            issues.add(new SettingIssue("adaptiveStep.minStep", "Smallest step must not exceed the largest step."));
        }
        if (maxValid && !Double.isNaN(adaptive.maxStep())) {
            if (adaptive.maxStep() > step) {
                issues.add(new SettingIssue("adaptiveStep.maxStep", "Largest step must not exceed the main step."));
            } else if (preTime > 0 && positive(preStep) && adaptive.maxStep() > preStep) {
                issues.add(new SettingIssue("adaptiveStep.maxStep", "Largest step must not exceed the warmup step."));
            }
        }
        return issues;
    }

    /**
     * Checks the circuit parts of the periodic steady-state settings on an execution model, before any session exists.
     * It assembles the CONTROL chain only to inspect it and closes it again. A model that cannot be assembled has
     * no issues here, because preparation reports its own findings.
     *
     * @param model the execution model
     * @param step the main electrical step in seconds
     * @param samplePeriod the CONTROL sample period, zero for every step
     * @param pss the settings, or null
     * @return one issue per violation, each with its parameter path
     */
    public static List<SettingIssue> periodicSteadyStateCircuitIssues(gecko.core.io.CircuitModel model, double step,
            double samplePeriod, PeriodicSteadyStateSettings pss) {
        if (pss == null || !positive(pss.period()) || !positive(step) || !Double.isFinite(samplePeriod) || samplePeriod < 0) {
            return List.of();
        }
        ControlRuntime runtime = null;
        try {
            var netlist = gecko.core.circuit.netlist.NetlistBuilder.buildFromCircuitModel(model);
            runtime = ControlRuntime.fromDeferred(model, step, netlist, samplePeriod);
            return PeriodicSteadyStateValidation.circuitIssues(pss.period(), netlist, runtime.calculators());
        } catch (RuntimeException unassembled) {
            return List.of();
        } finally {
            if (runtime != null) runtime.close();
        }
    }

    /**
     * Checks the scalar periodic steady-state settings against the resolved step, duration and sample period.
     * A null setting is off and has no issues.
     *
     * @param pss the settings, or null
     * @param step the main electrical step in seconds
     * @param duration the main duration in seconds
     * @param preTime the pre-calculation time, which cannot combine with the search
     * @param samplePeriod the CONTROL sample period, zero for every step
     * @return one issue per violation, each with its parameter path
     */
    public static List<SettingIssue> periodicSteadyStateIssues(PeriodicSteadyStateSettings pss, double step,
            double duration, double preTime, double samplePeriod) {
        var issues = new ArrayList<SettingIssue>();
        if (pss == null) return issues;
        double period = pss.period();
        boolean periodValid = positive(period);
        if (!periodValid) issues.add(new SettingIssue("periodicSteadyState.period", "Period must be finite and positive."));
        if (!(pss.tolerance() > 0 && pss.tolerance() <= PeriodicSteadyStateSettings.MAX_TOLERANCE)) {
            issues.add(new SettingIssue("periodicSteadyState.tolerance", "Tolerance must be above 0 and at most 0.1."));
        }
        if (pss.maxPeriods() < PeriodicSteadyStateSettings.MIN_PERIODS || pss.maxPeriods() > PeriodicSteadyStateSettings.MAX_PERIODS) {
            issues.add(new SettingIssue("periodicSteadyState.maxPeriods", "Period limit must be from 3 to 1000000."));
        }
        if (preTime > 0) {
            issues.add(new SettingIssue("preSimulationTime", "Pre-calculation time cannot combine with the periodic steady-state search."));
        }
        if (periodValid && positive(step)) {
            double steps = Math.rint(period / step);
            if (steps < 2 || Math.abs(steps * step - period) > WHOLE_STEP * period) {
                issues.add(new SettingIssue("periodicSteadyState.period", "Period must be a whole number of steps, at least 2."));
            } else if (positive(duration) && pss.maxPeriods() >= PeriodicSteadyStateSettings.MIN_PERIODS
                    && pss.maxPeriods() * steps + Math.ceil(duration / step) + 1 > MAXIMUM_STEPS) {
                issues.add(new SettingIssue("periodicSteadyState.maxPeriods", "Search and main step count is too large."));
            }
        }
        if (periodValid && positive(samplePeriod)) {
            double ratio = period / samplePeriod;
            if (Math.rint(ratio) < 1 || Math.abs(ratio - Math.rint(ratio)) > WHOLE_STEP * ratio) {
                issues.add(new SettingIssue("periodicSteadyState.period", "Period must be a whole multiple of the CONTROL sample period."));
            }
        }
        return issues;
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
