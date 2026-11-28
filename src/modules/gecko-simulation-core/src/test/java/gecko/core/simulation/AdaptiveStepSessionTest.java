package gecko.core.simulation;

import gecko.core.allg.AdaptiveStepSettings;
import gecko.core.allg.PeriodicSteadyStateSettings;
import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.InitializableAtSimulationStart;
import gecko.core.control.calculators.NothingToDoCalculator;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

/** Settings, prepared-circuit, session and engine paths of the adaptive electrical step. */
class AdaptiveStepSessionTest {
    private static final AdaptiveStepSettings ON = new AdaptiveStepSettings(1e-3, 1e-9, Double.NaN);

    private static String buck() throws Exception {
        return Path.of(Objects.requireNonNull(AdaptiveStepSessionTest.class.getResource("/ipes/buck_simple.ipes")).toURI()).toString();
    }

    private static PreparationResult prepareBuck(SolverOverrides overrides) throws Exception {
        var preparation = new CircuitPreparation();
        return preparation.prepare(preparation.capture(new CircuitFileParser().parse(buck())), new PreparationRequest(overrides, Map.of()));
    }

    private static SolverOverrides features(SolverFeatures features) { return new SolverOverrides(null, null, null, features); }

    private static SolverFeatures adaptive(AdaptiveStepSettings settings) { return new SolverFeatures(0, false, false, null, settings); }

    @Test
    void theSettingFlowsFromTheBuilderThroughPreparationToTheSessionAndEngine() throws Exception {
        assertNull(SimulationConfig.builder().build().getSolverSettings().getAdaptiveStep(), "off by default");
        assertNull(SimulationConfig.builder().build().explicitSolverOverrides().features().adaptiveStep());
        var config = SimulationConfig.builder().adaptiveStep(ON).build();
        assertEquals(ON, config.getSolverSettings().getAdaptiveStep());
        assertEquals(ON, config.explicitSolverOverrides().features().adaptiveStep());
        assertEquals(ON, config.getSolverSettings().copy().getAdaptiveStep(), "copy keeps the setting");
        assertNull(SolverFeatures.none().adaptiveStep());
        assertEquals(new SolverFeatures(1e-5, false), new SolverFeatures(1e-5));
        assertEquals(1e-3, new AdaptiveStepSettings().tolerance());
        assertEquals(1e-6 / 1000, new AdaptiveStepSettings().resolvedMinStep(1e-6), 1e-18);
        assertEquals(1e-6, new AdaptiveStepSettings().resolvedMaxStep(1e-6));

        var prepared = assertInstanceOf(PreparationResult.Ready.class, prepareBuck(features(adaptive(ON)))).execution();
        assertEquals(ON, prepared.solverSettings().getAdaptiveStep());
        prepared.solverSettings().withAdaptiveStep(null);
        assertEquals(ON, prepared.solverSettings().getAdaptiveStep(), "settings are detached");
        assertNull(assertInstanceOf(PreparationResult.Ready.class, prepareBuck(SolverOverrides.none())).execution()
                .solverSettings().getAdaptiveStep());

        try (var session = SimulationSession.open(prepared, ExecutionOptions.defaults())) {
            session.run();
            var report = session.adaptiveStepReport();
            assertTrue(report.accepted() > session.status().committedSteps(), report.toString());
            assertTrue(report.located() > 0, "adaptive mode implies location: " + report);
            assertEquals(session.eventLocationReport().located(), report.located());
            assertEquals(session.status().committedSteps(), session.observe(0, 10000).times().length, "one recorded row per grid step");
        }
        var on = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder().circuitFile(buck()).adaptiveStep(ON).build());
        assertTrue(on.isSuccess(), on.getErrorMessage());
        var metadata = (AdaptiveStepReport) on.getMetadata().get("adaptiveStep");
        assertTrue(metadata.accepted() > 0 && metadata.located() > 0 && metadata.minStep() > 0, metadata.toString());
        assertTrue(metadata.maxStep() <= 1e-6 * (1 + 1e-9));
        var off = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder().circuitFile(buck()).build());
        assertFalse(off.getMetadata().containsKey("adaptiveStep"));
        try (var session = SimulationSession.open(assertInstanceOf(PreparationResult.Ready.class,
                prepareBuck(SolverOverrides.none())).execution(), ExecutionOptions.defaults())) {
            session.run();
            assertEquals(AdaptiveStepReport.empty(), session.adaptiveStepReport());
        }
    }

    private static List<String> paths(List<SimulationTiming.SettingIssue> issues) {
        return issues.stream().map(SimulationTiming.SettingIssue::parameterPath).toList();
    }

    @Test
    void everyInvalidSettingIsReportedBeforeAnyAllocation() throws Exception {
        double step = 1e-6;
        var base = new SolverSettingsCore(SolverType.SOLVER_BE, step, 1e-4);
        assertEquals(List.of(), paths(SimulationTiming.settingsIssues(base.copy().withAdaptiveStep(ON))));
        assertEquals(List.of(), paths(SimulationTiming.settingsIssues(base.copy().withAdaptiveStep(new AdaptiveStepSettings()))));
        assertEquals(List.of("adaptiveStep"), paths(SimulationTiming.settingsIssues(
                new SolverSettingsCore(SolverType.SOLVER_GS, step, 1e-4).withAdaptiveStep(ON))));
        for (double tolerance : new double[]{0, -1e-3, 0.1000001, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertEquals(List.of("adaptiveStep.tolerance"), paths(SimulationTiming.settingsIssues(
                    base.copy().withAdaptiveStep(new AdaptiveStepSettings(tolerance)))), "tolerance " + tolerance);
        }
        assertEquals(List.of(), paths(SimulationTiming.settingsIssues(base.copy().withAdaptiveStep(new AdaptiveStepSettings(0.1)))));
        for (double bad : new double[]{0, -1e-9, Double.POSITIVE_INFINITY}) {
            assertEquals(List.of("adaptiveStep.minStep"), paths(SimulationTiming.settingsIssues(
                    base.copy().withAdaptiveStep(new AdaptiveStepSettings(1e-3, bad, Double.NaN)))), "min " + bad);
            assertEquals(List.of("adaptiveStep.maxStep"), paths(SimulationTiming.settingsIssues(
                    base.copy().withAdaptiveStep(new AdaptiveStepSettings(1e-3, Double.NaN, bad)))), "max " + bad);
        }
        assertEquals(List.of("adaptiveStep.minStep"), paths(SimulationTiming.settingsIssues(
                base.copy().withAdaptiveStep(new AdaptiveStepSettings(1e-3, 5e-7, 2e-7)))));
        assertEquals(List.of("adaptiveStep.minStep"), paths(SimulationTiming.settingsIssues(
                base.copy().withAdaptiveStep(new AdaptiveStepSettings(1e-3, 2e-6, Double.NaN)))), "min above the default maximum");
        assertEquals(List.of("adaptiveStep.maxStep"), paths(SimulationTiming.settingsIssues(
                base.copy().withAdaptiveStep(new AdaptiveStepSettings(1e-3, 1e-9, 2e-6)))), "max above the main step");
        var warmup = base.copy().withPreCalculation(1e-4, 2e-7);
        assertEquals(List.of("adaptiveStep.maxStep"), paths(SimulationTiming.settingsIssues(
                warmup.copy().withAdaptiveStep(new AdaptiveStepSettings(1e-3, 1e-9, 5e-7)))), "max above the warmup step");
        assertEquals(List.of(), paths(SimulationTiming.settingsIssues(warmup.copy().withAdaptiveStep(new AdaptiveStepSettings(1e-3, 1e-9, 2e-7)))));
        assertEquals(List.of(), paths(SimulationTiming.settingsIssues(warmup.copy().withAdaptiveStep(ON))),
                "an unset maximum follows the grid of each phase");

        var invalid = base.copy().withAdaptiveStep(new AdaptiveStepSettings(0, 1e-9, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> SimulationTiming.validateSettings(invalid));
        assertThrows(IllegalArgumentException.class, () -> SimulationSession.fromModel(new CircuitFileParser().parse(buck()),
                invalid, ExecutionOptions.defaults()));

        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(new CircuitFileParser().parse(buck()));
        var rejected = preparation.prepare(snapshot, new PreparationRequest(new SolverOverrides(SolverType.SOLVER_GS, null, null,
                adaptive(ON)), Map.of()));
        var report = assertInstanceOf(PreparationResult.Rejected.class, rejected).report();
        assertTrue(report.diagnostics().stream().anyMatch(item -> item.code().equals("INVALID_SETTING")
                && "adaptiveStep".equals(item.parameterPath())), report.toString());
        var tolerance = preparation.prepare(snapshot, new PreparationRequest(features(adaptive(new AdaptiveStepSettings(0.5))), Map.of()));
        assertTrue(assertInstanceOf(PreparationResult.Rejected.class, tolerance).report().diagnostics().stream()
                .anyMatch(item -> "adaptiveStep.tolerance".equals(item.parameterPath())));
        var clamps = preparation.prepare(snapshot, new PreparationRequest(
                features(adaptive(new AdaptiveStepSettings(1e-3, 1e-3, Double.NaN))), Map.of()));
        assertInstanceOf(PreparationResult.Rejected.class, clamps);
        assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot, new PreparationRequest(
                new SolverOverrides(SolverType.SOLVER_GS, null, null, SolverFeatures.none()), Map.of())));
        var config = SimulationConfig.builder().circuitFile(buck()).solverType(SolverType.SOLVER_GS).adaptiveStep(ON).build();
        assertFalse(new HeadlessSimulationEngine().runSimulation(config).isSuccess());
        assertThrows(IllegalArgumentException.class, () -> new ElectricalTimeStep(AdaptiveStepTest.rc(), SolverType.SOLVER_GS,
                adaptive(ON)));
    }

    private static float[] output(SimulationConfig config) {
        var result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return result.getSignalData(AdaptiveStepBuckTest.OUTPUT);
    }

    @Test
    void sampledControlKeepsItsClockAndTheAveragesAgreeWithinOnePercent() throws Exception {
        double sample = 5e-6;
        var plain = SimulationConfig.builder().circuitFile(buck()).solverType(SolverType.SOLVER_BE).stepWidth(1e-6)
                .simulationDuration(6e-3).controlSamplePeriod(sample).build();
        var adaptive = SimulationConfig.builder().circuitFile(buck()).solverType(SolverType.SOLVER_BE).stepWidth(1e-6)
                .simulationDuration(6e-3).controlSamplePeriod(sample).adaptiveStep(new AdaptiveStepSettings(1e-3, 1e-9, Double.NaN)).build();
        float[] reference = output(plain);
        float[] actual = output(adaptive);
        assertEquals(reference.length, actual.length);
        int from = reference.length - 300;
        double fixedAverage = AdaptiveStepBuckTest.average(reference, from, reference.length);
        double adaptiveAverage = AdaptiveStepBuckTest.average(actual, from, reference.length);
        System.out.printf("ADAPTIVE-BUCK-TS5 fixedAvg=%.6f adaptiveAvg=%.6f relDiff=%.3e%n", fixedAverage, adaptiveAverage,
                Math.abs(adaptiveAverage - fixedAverage) / fixedAverage);
        assertEquals(fixedAverage, adaptiveAverage, 0.01 * fixedAverage);
    }

    /** DC source, resistor and capacitor in a model, so that CONTROL runs next to a real electrical circuit. */
    private static CircuitModel rcModel() {
        var model = new CircuitModel();
        model.addCircuitComponent(component(4, "V", new double[]{401, 1}, new String[]{"1"}, "0"));
        model.addCircuitComponent(component(1, "R", new double[]{1e3}, new String[]{"1"}, "2"));
        model.addCircuitComponent(component(3, "C", new double[]{1e-6, 0}, new String[]{"2"}, "0"));
        return model;
    }

    /** Runs a model through preparation, as every public path does, so its parameters are derived. */
    private static PreparedCircuit prepared(CircuitModel model, SolverType solver, double step, double duration,
            AdaptiveStepSettings settings) {
        model.setTimeStep(step);
        model.setSimulationDuration(duration);
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), new PreparationRequest(
                new SolverOverrides(solver, null, null, adaptive(settings)), Map.of()));
        return assertInstanceOf(PreparationResult.Ready.class, result).execution();
    }

    @Test
    void controlRandomDelayAndTraceSeeOnlyTheGridWhileTheElectricalStepAdapts() throws Exception {
        double grid = 100e-6;
        var spy = new SpyCalculator();
        var scope = new NothingToDoCalculator(1, 0);
        scope.setInputSignal(0, spy, 0);
        var mapping = new ControlBlockMapping();
        mapping.addScopeInfo(scope, new String[]{"value"}, "scope", 91);
        var prepared = prepared(rcModel(), SolverType.SOLVER_TRZ, grid, 5e-3, new AdaptiveStepSettings(1e-3, 1e-7, Double.NaN));
        var settings = prepared.solverSettings();
        var runtime = ControlRuntime.fromDeferred(new ControlCalculatorFactory.BuildResult(
                ControlNetlist.fromOrderedCalculators(List.of(spy, scope)), mapping), grid);
        var traced = new ArrayList<Double>();
        try (var session = SimulationSession.fromModel(prepared.newRunModel(), settings, ExecutionOptions.defaults(), runtime)) {
            while (session.status().state() != SimulationSession.State.COMPLETED) {
                session.step((time, volts, currents, circuit) -> traced.add(time));
            }
            int steps = session.status().committedSteps();
            assertEquals(51, steps);
            assertEquals(steps, spy.calls, "CONTROL and its random draws run once per grid step");
            assertTrue(spy.deltas.stream().allMatch(delta -> delta == grid), spy.deltas.toString());
            assertEquals(steps, traced.size(), "one trace row per grid step");
            assertEquals(steps, session.observe(0, 10000).times().length, "one recorded row per grid step");
            assertTrue(session.adaptiveStepReport().accepted() >= steps);
        }
    }

    private static float[][] clockAndDelay(boolean adaptiveOn) {
        var model = rcModel();
        model.addControlComponent(component(58, "time", new double[0], new String[0], "clock"));
        model.addControlComponent(component(25, "delay", new double[]{300e-6}, new String[]{"clock"}, "delayed"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"clock", "delayed"}));
        model.setTimeStep(100e-6);
        model.setSimulationDuration(2e-3);
        var preparation = new CircuitPreparation();
        var features = adaptiveOn ? adaptive(new AdaptiveStepSettings(1e-3, 1e-7, Double.NaN)) : SolverFeatures.none();
        var circuit = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(preparation.capture(model),
                new PreparationRequest(new SolverOverrides(SolverType.SOLVER_TRZ, null, null, features), Map.of()))).execution();
        try (var session = SimulationSession.open(circuit, ExecutionOptions.defaults())) {
            session.run();
            return session.observe(0, 10000).values();
        }
    }

    @Test
    void aDelayBlockCountsGridStepsWhileTheElectricalStepAdapts() {
        float[][] adaptiveRows = clockAndDelay(true);
        float[][] plainRows = clockAndDelay(false);
        for (int step = 0; step < adaptiveRows[0].length; step++) {
            assertEquals(step * 100e-6, adaptiveRows[0][step], 1e-9, "clock " + step);
        }
        assertArrayEquals(plainRows[0], adaptiveRows[0], "the clock is the grid time");
        assertArrayEquals(plainRows[1], adaptiveRows[1], "the delay line holds the same grid history");
    }

    private static final List<String> EVENT_KEYS = List.of("zeroCrossingLocated", "zeroCrossingUnlocated",
            "zeroCrossingFirstUnlocatedTime", "zeroCrossingRetained", "zeroCrossingDropped");

    private static java.util.Map<String, Object> metadata(SimulationConfig config) {
        var result = new HeadlessSimulationEngine().runSimulation(config);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return result.getMetadata();
    }

    @Test
    void theEngineReportsEventCountsWheneverEventsAreLocatedWhetherOrNotZeroCrossingIsRequested() throws Exception {
        var adaptiveOnly = metadata(SimulationConfig.builder().circuitFile(buck()).simulationDuration(2e-3)
                .zeroCrossing(false).adaptiveStep(ON).build());
        var explicit = metadata(SimulationConfig.builder().circuitFile(buck()).simulationDuration(2e-3)
                .zeroCrossing(true).adaptiveStep(ON).build());
        for (String key : EVENT_KEYS) assertTrue(adaptiveOnly.containsKey(key), "adaptive-only reports " + key);
        for (String key : EVENT_KEYS) assertEquals(explicit.get(key), adaptiveOnly.get(key), key);
        assertTrue((Long) adaptiveOnly.get("zeroCrossingLocated") > 0, "the buck diode switches inside steps");
        assertEquals(adaptiveOnly.get("adaptiveStep"), explicit.get("adaptiveStep"));
        assertEquals(((AdaptiveStepReport) adaptiveOnly.get("adaptiveStep")).located(), adaptiveOnly.get("zeroCrossingLocated"));
        var off = metadata(SimulationConfig.builder().circuitFile(buck()).simulationDuration(2e-3).build());
        for (String key : EVENT_KEYS) assertFalse(off.containsKey(key), "off reports no " + key);
        assertFalse(off.containsKey("adaptiveStep"));
        var legacy = metadata(SimulationConfig.builder().circuitFile(buck()).simulationDuration(2e-3).zeroCrossing(true).build());
        for (String key : EVENT_KEYS) assertTrue(legacy.containsKey(key), "zero-crossing alone reports " + key);
        assertFalse(legacy.containsKey("adaptiveStep"));
    }

    @Test
    void aPausedAndContinuedRunEqualsAnUninterruptedRunBitForBit() throws Exception {
        var settings = new SolverSettingsCore(SolverType.SOLVER_BE, 1e-6, 4e-4).withAdaptiveStep(ON);
        var model = new CircuitFileParser().parse(buck());
        float[] whole;
        try (var session = SimulationSession.fromModel(model, settings, ExecutionOptions.defaults())) {
            session.run();
            whole = session.observe(0, 10000).values()[AdaptiveStepBuckTest.OUTPUT];
        }
        for (int split : new int[]{1, 13, 150}) {
            try (var session = SimulationSession.fromModel(new CircuitFileParser().parse(buck()), settings, ExecutionOptions.defaults())) {
                for (int index = 0; index < split; index++) session.step();
                session.run();
                float[] parts = session.observe(0, 10000).values()[AdaptiveStepBuckTest.OUTPUT];
                assertEquals(whole.length, parts.length);
                for (int index = 0; index < whole.length; index++) {
                    assertEquals(Float.floatToRawIntBits(whole[index]), Float.floatToRawIntBits(parts[index]), "split " + split + " row " + index);
                }
            }
        }
    }

    @Test
    void anExtendedSegmentOnAFinerGridClampsTheAdaptiveStepToTheNewGrid() {
        try (var session = SimulationSession.open(prepared(rcModel(), SolverType.SOLVER_TRZ, 100e-6, 1e-3,
                new AdaptiveStepSettings(1e-3, 1e-7, 100e-6)), ExecutionOptions.defaults())) {
            var widths = new ArrayList<double[]>();
            session.setSubStepListenerForTest((start, width, error, forced, be) -> widths.add(new double[]{start, width}));
            session.run();
            widths.clear();
            session.extend(2e-3, 20e-6);
            session.run();
            assertFalse(widths.isEmpty());
            assertTrue(widths.stream().allMatch(item -> item[1] <= 20e-6 * (1 + 1e-9)), "the new grid is the largest step");
            assertEquals(1000e-6, session.observe(0, 10000).times()[10], 1e-12);
            assertThrows(IllegalArgumentException.class, () -> session.extend(1e-3, 20e-6));
        }
    }

    @Test
    void aPeriodicSteadyStateSearchConvergesWithAdaptiveSubSteps() throws Exception {
        double period = 30e-6;
        var plain = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder().circuitFile(buck())
                .simulationDuration(6e-3).build());
        float[] reference = plain.getSignalData(AdaptiveStepBuckTest.OUTPUT);
        double plainAverage = AdaptiveStepBuckTest.average(reference, reference.length - 30, reference.length);
        var steady = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder().circuitFile(buck())
                .simulationDuration(period).periodicSteadyState(new PeriodicSteadyStateSettings(period, 1e-4, 1000))
                .adaptiveStep(ON).build());
        assertTrue(steady.isSuccess(), steady.getErrorMessage());
        var report = (SteadyStateReport) steady.getMetadata().get("steadyState");
        assertTrue(report.converged(), report.toString());
        float[] main = steady.getSignalData(AdaptiveStepBuckTest.OUTPUT);
        assertEquals(30, main.length, "one carrier period of recorded rows");
        assertEquals(plainAverage, AdaptiveStepBuckTest.average(main, 0, main.length), 0.01 * plainAverage);
        System.out.printf("ADAPTIVE-PSS periods=%d adaptiveAvg=%.6f plainAvg=%.6f%n", report.periods(),
                AdaptiveStepBuckTest.average(main, 0, main.length), plainAverage);
    }

    private static List<long[]> trace(SimulationConfig config) {
        var rows = new ArrayList<long[]>();
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, volts, currents, netlist) -> {
            long[] row = new long[1 + volts.length + currents.length];
            row[0] = Double.doubleToRawLongBits(time);
            for (int index = 0; index < volts.length; index++) row[1 + index] = Double.doubleToRawLongBits(volts[index]);
            for (int index = 0; index < currents.length; index++) row[1 + volts.length + index] = Double.doubleToRawLongBits(currents[index]);
            rows.add(row);
        });
        var result = engine.runSimulation(config);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return rows;
    }

    @Test
    void aNullSettingGivesTheSameBitsAsNoSetting() throws Exception {
        var plain = trace(SimulationConfig.builder().circuitFile(buck()).simulationDuration(2e-4).build());
        var explicitNull = trace(SimulationConfig.builder().circuitFile(buck()).simulationDuration(2e-4).adaptiveStep(null).build());
        assertEquals(plain.size(), explicitNull.size());
        assertTrue(plain.size() > 100);
        for (int index = 0; index < plain.size(); index++) assertArrayEquals(plain.get(index), explicitNull.get(index), "row " + index);
        var adaptiveRows = trace(SimulationConfig.builder().circuitFile(buck()).simulationDuration(2e-4).adaptiveStep(ON).build());
        assertEquals(plain.size(), adaptiveRows.size(), "the trace stays on the grid");
        for (int index = 0; index < plain.size(); index++) assertEquals(plain.get(index)[0], adaptiveRows.get(index)[0], "time " + index);
    }

    /** A CONTROL block that counts its executions and records the step it receives. */
    private static final class SpyCalculator extends AbstractControlCalculatable implements InitializableAtSimulationStart {
        int calls;
        final List<Double> deltas = new ArrayList<>();

        SpyCalculator() { super(0, 1); }

        @Override public void initializeAtSimulationStart(double dt) { _outputSignal[0][0] = 0; }

        @Override public void berechneYOUT(double dt) {
            calls++;
            deltas.add(dt);
            _outputSignal[0][0] = calls;
        }
    }
}
