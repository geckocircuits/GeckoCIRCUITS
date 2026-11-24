package gecko.core.simulation;

import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

import gecko.core.allg.PeriodicSteadyStateSettings;
import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import gecko.core.control.DelayHistory;
import gecko.core.control.PeriodicStateSource;
import gecko.core.control.StateSink;
import gecko.core.control.StatelessCalculation;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.preparation.SolverFeatures;
import gecko.core.preparation.SolverOverrides;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Periodic steady-state search on CONTROL-only circuits, where every state is known exactly. */
class PeriodicSteadyStateSessionTest {
    private static final double DT = 1e-6;

    private static CircuitModel constantModel() {
        var model = new CircuitModel();
        model.addControlComponent(component(3, "one", new double[]{1}, new String[0], "one"));
        model.addControlComponent(component(7, "gain", new double[]{2}, new String[]{"one"}, "two"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"two"}));
        return model;
    }

    private static CircuitModel rampModel() {
        var model = new CircuitModel();
        model.addControlComponent(component(3, "one", new double[]{1}, new String[0], "one"));
        model.addControlComponent(component(3, "zero", new double[]{0}, new String[0], "zero"));
        model.addControlComponent(component(64, "int", new double[]{1, 0, -1e9, 1e9}, new String[]{"one", "zero"}, "ramp"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"ramp"}));
        return model;
    }

    /** The integrator drifts, but the only scoped signal is its output scaled by zero. */
    private static CircuitModel hiddenDriftModel() {
        var model = rampModel();
        model.getControlComponents().removeLast();
        model.addControlComponent(component(7, "mute", new double[]{0}, new String[]{"ramp"}, "flat"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"flat"}));
        return model;
    }

    private static CircuitModel sineThroughDelay(double frequency, double delay) {
        var model = new CircuitModel();
        model.addControlComponent(component(4, "sine", new double[]{400, 1, frequency, 0, 0, 0.5, 0}, new String[0], "wave"));
        model.addControlComponent(component(25, "delay", new double[]{delay}, new String[]{"wave"}, "late"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"late"}));
        return model;
    }

    private static SolverSettingsCore settings(double duration, PeriodicSteadyStateSettings search) {
        return new SolverSettingsCore(SolverType.SOLVER_BE, DT, duration).withPeriodicSteadyState(search);
    }

    @Test
    void aLinearDriftEndsAtTheLimitWithNoMainSamples() {
        try (var session = SimulationSession.fromModel(rampModel(),
                settings(1e-4, new PeriodicSteadyStateSettings(10e-6, 1e-4, 50)), ExecutionOptions.defaults())) {
            var status = session.run();
            assertEquals(SimulationSession.State.COMPLETED, status.state());
            assertEquals(500, status.committedSteps(), "exactly maxPeriods periods of whole steps");
            var report = session.steadyStateReport();
            assertFalse(report.converged());
            assertEquals(50, report.periods());
            assertEquals(0, session.observe(0, 10000).times().length, "the search records no rows");
            assertFalse(report.worstState().isBlank());
        }
    }

    @Test
    void aConvergedSearchStartsTheMainPhaseAtTheNextGridStepWithoutDuplicateOrMissingSamples() {
        try (var session = SimulationSession.fromModel(constantModel(),
                settings(100e-6, new PeriodicSteadyStateSettings(10e-6)), ExecutionOptions.defaults())) {
            var status = session.run();
            assertEquals(SimulationSession.State.COMPLETED, status.state());
            assertEquals(SimulationSession.Phase.MAIN, status.phase());
            var report = session.steadyStateReport();
            assertTrue(report.converged());
            assertTrue(report.periods() >= 3 && report.periods() <= 5, "periods " + report.periods());
            double[] times = session.observe(0, 10000).times();
            assertEquals(report.startTime() + DT, times[0], 1e-12, "the clock continues from the search");
            for (int index = 1; index < times.length; index++) assertEquals(DT, times[index] - times[index - 1], 1e-12);
            assertEquals(report.startTime() + 100e-6, times[times.length - 1], 1.5e-12);
            assertEquals(times.length, status.segmentSteps());
            assertEquals(report.periods() * 10 + times.length, status.committedSteps());
        }
    }

    @Test
    void aNormalContinuationIsAllowedAfterTheMainPhaseButNotDuringTheSearch() {
        try (var session = SimulationSession.fromModel(constantModel(),
                settings(20e-6, new PeriodicSteadyStateSettings(10e-6)), ExecutionOptions.defaults())) {
            assertThrows(IllegalStateException.class, () -> session.extend(1));
            session.step();
            assertThrows(IllegalStateException.class, () -> session.extend(1));
            session.run();
            double end = session.status().endTime();
            int before = session.observe(0, 10000).times().length;
            session.extend(end + 10e-6);
            session.run();
            assertTrue(session.observe(0, 10000).times().length > before);
        }
    }

    @Test
    void aFailedSearchCannotBeExtended() {
        try (var session = SimulationSession.fromModel(rampModel(),
                settings(1e-4, new PeriodicSteadyStateSettings(10e-6, 1e-4, 3)), ExecutionOptions.defaults())) {
            session.run();
            assertThrows(IllegalStateException.class, () -> session.extend(1));
        }
    }

    @Test
    void outputsThatRepeatDoNotHideADriftingPrivateState() {
        try (var session = SimulationSession.fromModel(hiddenDriftModel(),
                settings(1e-4, new PeriodicSteadyStateSettings(10e-6, 1e-4, 50)), ExecutionOptions.defaults())) {
            session.run();
            var report = session.steadyStateReport();
            assertFalse(report.converged());
            assertTrue(report.worstState().contains("IntegratorCalculation"), report.worstState());
        }
        // A test-only residual on the scoped outputs alone would have accepted the same run.
        var outputsOnly = new PeriodicStateMonitor(1e-4, 50, 10);
        try (var session = SimulationSession.fromModel(hiddenDriftModel(),
                settings(1e-3, null), ExecutionOptions.defaults())) {
            PeriodicStateMonitor.Outcome outcome = PeriodicStateMonitor.Outcome.CONTINUE;
            for (int step = 1; step <= 500 && outcome == PeriodicStateMonitor.Outcome.CONTINUE; step++) {
                var sample = session.step();
                outputsOnly.begin();
                outputsOnly.sink().group(PeriodicStateMonitor.Unit.CONTROL, "scope");
                outputsOnly.sink().value(sample.values()[0]);
                outcome = outputsOnly.commit(step, sample.time());
            }
            assertEquals(PeriodicStateMonitor.Outcome.CONVERGED, outcome);
        }
    }

    @Test
    void aDelayWhoseLengthIsCoprimeToThePeriodConvergesThroughTheChronologicalExport() {
        // 7 delay samples, 10 steps per period: the ring cursor moves by 3 slots each period.
        try (var session = SimulationSession.fromModel(sineThroughDelay(100e3, 7.5e-6),
                settings(20e-6, new PeriodicSteadyStateSettings(10e-6, 1e-4, 200)), ExecutionOptions.defaults())) {
            session.run();
            var report = session.steadyStateReport();
            assertTrue(report.converged(), () -> report.toString());
            assertTrue(report.periods() < 200);
        }
    }

    @Test
    void theChronologicalDelayExportIgnoresTheRingCursor() {
        var first = filledHistory(14);
        var second = filledHistory(11);
        assertEquals(sampleExport(first), sampleExport(second));
        var changed = filledHistory(14);
        changed.capture(99);
        assertNotEquals(sampleExport(first), sampleExport(changed));
    }

    /** Fills a history so that its last eight captures are the same whatever the total count, so only the ring cursor differs. */
    private static DelayHistory filledHistory(int captures) {
        var history = new DelayHistory(7.5e-6);
        history.restart(1e-6);
        // The value of capture n depends only on its distance from the final capture, so the contents agree.
        for (int capture = 1; capture <= captures; capture++) history.capture((captures - capture) % 8);
        return history;
    }

    private static List<Object> sampleExport(DelayHistory history) {
        var out = new ArrayList<Object>();
        history.exportChronological(new StateSink() {
            @Override public void value(double value) { out.add(value); }
            @Override public void flag(boolean value) { out.add(value); }
        });
        return out;
    }

    @Test
    void theDelayExportListsOldestToNewestThenTheSampleBeforeItThenTheFullFlag() {
        var history = new DelayHistory(3.5e-6);
        history.restart(1e-6);
        for (double value : new double[]{1, 2, 3, 4, 5}) history.capture(value);
        assertEquals(List.of(3.0, 4.0, 5.0, 2.0, true), sampleExport(history));
        var young = new DelayHistory(3.5e-6);
        young.restart(1e-6);
        young.capture(9);
        assertEquals(List.of(0.0, 0.0, 9.0, 0.0, false), sampleExport(young));
    }

    @Test
    void everyInvalidScalarSettingIsReportedWithItsPath() {
        assertPath("periodicSteadyState.period", new PeriodicSteadyStateSettings(Double.NaN));
        assertPath("periodicSteadyState.period", new PeriodicSteadyStateSettings(0));
        assertPath("periodicSteadyState.period", new PeriodicSteadyStateSettings(-1e-5));
        assertPath("periodicSteadyState.period", new PeriodicSteadyStateSettings(10.5e-6));
        assertPath("periodicSteadyState.period", new PeriodicSteadyStateSettings(DT));
        assertPath("periodicSteadyState.tolerance", new PeriodicSteadyStateSettings(10e-6, 0, 10));
        assertPath("periodicSteadyState.tolerance", new PeriodicSteadyStateSettings(10e-6, 0.11, 10));
        assertPath("periodicSteadyState.tolerance", new PeriodicSteadyStateSettings(10e-6, Double.NaN, 10));
        assertPath("periodicSteadyState.maxPeriods", new PeriodicSteadyStateSettings(10e-6, 1e-4, 2));
        assertPath("periodicSteadyState.maxPeriods", new PeriodicSteadyStateSettings(10e-6, 1e-4, 1_000_001));
        assertTrue(SimulationTiming.settingsIssues(settings(1e-4, new PeriodicSteadyStateSettings(10e-6, 0.1, 3))).isEmpty());
        assertTrue(SimulationTiming.settingsIssues(settings(1e-4, new PeriodicSteadyStateSettings(10e-6, 1e-4, 1_000_000))).isEmpty());
    }

    private static void assertPath(String path, PeriodicSteadyStateSettings search) {
        var issues = SimulationTiming.settingsIssues(settings(1e-4, search));
        assertTrue(issues.stream().anyMatch(issue -> issue.parameterPath().equals(path)), issues + " for " + search);
    }

    @Test
    void theTotalStepCountIncludingTheMainPhaseIsBounded() {
        // The main phase alone fits (2.1e9 steps). With 1e6 periods of 100 steps the total does not.
        var huge = settings(2100, new PeriodicSteadyStateSettings(100e-6, 1e-4, 1_000_000));
        var issues = SimulationTiming.settingsIssues(huge);
        assertTrue(issues.stream().anyMatch(issue -> issue.parameterPath().equals("periodicSteadyState.maxPeriods")), issues.toString());
    }

    @Test
    void aPreCalculationTimeCannotCombineWithTheSearch() {
        var combined = settings(1e-4, new PeriodicSteadyStateSettings(10e-6)).withPreCalculation(1e-4, 1e-6);
        assertTrue(SimulationTiming.settingsIssues(combined).stream()
                .anyMatch(issue -> issue.parameterPath().equals("preSimulationTime")));
        assertThrows(IllegalArgumentException.class,
                () -> SimulationSession.fromModel(constantModel(), combined, ExecutionOptions.defaults()));
    }

    @Test
    void theSearchPeriodMustBeAWholeMultipleOfTheControlSamplePeriod() {
        var good = settings(1e-4, new PeriodicSteadyStateSettings(30e-6)).withControlSamplePeriod(3e-6);
        assertTrue(SimulationTiming.settingsIssues(good).isEmpty());
        var bad = settings(1e-4, new PeriodicSteadyStateSettings(31e-6)).withControlSamplePeriod(3e-6);
        assertTrue(SimulationTiming.settingsIssues(bad).stream()
                .anyMatch(issue -> issue.parameterPath().equals("periodicSteadyState.period")));
        try (var session = SimulationSession.fromModel(constantModel(), good, ExecutionOptions.defaults())) {
            session.run();
            assertTrue(session.steadyStateReport().converged(), session.steadyStateReport().toString());
        }
    }

    @Test
    void aControlSineThatDoesNotCompleteWholeCyclesIsRejectedBeforeAllocation() {
        var failure = assertThrows(IllegalArgumentException.class, () -> SimulationSession.fromModel(
                sineThroughDelay(130e3, 7.5e-6), settings(1e-4, new PeriodicSteadyStateSettings(10e-6)), ExecutionOptions.defaults()));
        assertTrue(failure.getMessage().contains("periodicSteadyState.period"), failure.getMessage());
        assertTrue(failure.getMessage().contains("sine"), failure.getMessage());
    }

    @Test
    void anElectricalSineSourceMustCompleteWholeCyclesInThePeriod() {
        for (double[] case_ : new double[][]{{100e3, 0}, {130e3, 1}, {0, 0}}) {
            double[] capacitor = new double[13];
            double[] sine = new double[21];
            capacitor[0] = capacitor[6] = capacitor[7] = 1e-9;
            sine[0] = gecko.core.circuit.SourceType.QUELLE_SIN_NEW;
            sine[1] = sine[20] = 1;
            sine[2] = case_[0];
            var netlist = new gecko.core.circuit.netlist.CircuitNetlist();
            netlist.initNetlist(new gecko.core.circuit.circuitcomponents.CircuitTypCore[]{
                    gecko.core.circuit.circuitcomponents.CircuitTypCore.LK_C,
                    gecko.core.circuit.circuitcomponents.CircuitTypCore.LK_I},
                    new int[]{1, 1}, new int[]{0, 0}, new int[]{-1, -1},
                    new double[][]{capacitor, sine}, 1, 0, 2);
            var issues = PeriodicSteadyStateValidation.circuitIssues(10e-6, netlist, List.of());
            assertEquals((int) case_[1], issues.size(), "frequency " + case_[0] + " " + issues);
            if (!issues.isEmpty()) assertEquals("periodicSteadyState.period", issues.getFirst().parameterPath());
        }
    }

    @Test
    void aRandomSourceIsRejectedBecauseItsGeneratorStateIsHidden() {
        var model = new CircuitModel();
        model.addControlComponent(component(4, "noise", new double[]{405, 1, 1, 0, 0, 0.5, 0}, new String[0], "n"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"n"}));
        var failure = assertThrows(IllegalArgumentException.class, () -> SimulationSession.fromModel(
                model, settings(1e-4, new PeriodicSteadyStateSettings(10e-6)), ExecutionOptions.defaults()));
        assertTrue(failure.getMessage().contains("random"), failure.getMessage());
    }

    @Test
    void aCalculatorWithoutAnExportOrAMarkerIsRejectedByName() {
        var unknown = new AbstractControlCalculatable(1, 1) {
            @Override public void berechneYOUT(double dt) { }
        };
        var issues = PeriodicSteadyStateValidation.circuitIssues(10e-6, null, List.of(unknown));
        assertEquals(1, issues.size());
        assertTrue(issues.getFirst().message().contains("no periodic state export"));
        assertEquals("periodicSteadyState", issues.getFirst().parameterPath());
    }

    @Test
    void everyRegisteredCalculatorExportsItsStateOrIsMarkedStateless() {
        var classified = new ArrayList<String>();
        for (var descriptor : ControlBlockDescriptor.registered()) {
            for (var data : variants(descriptor)) {
                var calculator = descriptor.createCalculator(data, null);
                boolean random = calculator.getClass().getSimpleName().equals("SignalCalculatorRandom");
                assertTrue(random || calculator instanceof PeriodicStateSource || calculator instanceof StatelessCalculation,
                        "type " + descriptor.type() + " " + calculator.getClass().getName()
                                + " must implement PeriodicStateSource or StatelessCalculation");
                if (calculator instanceof PeriodicStateSource source) {
                    if (calculator instanceof gecko.core.control.calculators.InitializableAtSimulationStart start) {
                        start.initializeAtSimulationStart(1e-6);
                    }
                    int[] count = {0};
                    source.exportState(new StateSink() {
                        @Override public void value(double value) { count[0]++; }
                        @Override public void flag(boolean value) { count[0]++; }
                    });
                    assertTrue(count[0] > 0, calculator.getClass().getSimpleName() + " exports at least one value");
                    int[] again = {0};
                    source.exportState(new StateSink() {
                        @Override public void value(double value) { again[0]++; }
                        @Override public void flag(boolean value) { again[0]++; }
                    });
                    assertEquals(count[0], again[0], "the export has a fixed layout");
                }
                classified.add(calculator.getClass().getSimpleName());
            }
        }
        assertTrue(classified.size() > 60, "the registry was classified: " + classified.size());
    }

    @Test
    void anExternallyModulatedSineIsCheckedThroughItsWrapper() {
        var data = new CircuitModel.ComponentData(4, "sine");
        data.setNumericParameters(new double[]{400, 1, 130e3, 0, 0, 0.5, 1});
        data.setTerminalXLabels(new String[5]);
        data.setTerminalYLabels(new String[1]);
        var wrapper = ControlBlockDescriptor.find(4).createCalculator(data, null);
        var issues = PeriodicSteadyStateValidation.circuitIssues(10e-6, null, List.of(wrapper));
        assertEquals(1, issues.size(), issues.toString());
        assertTrue(issues.getFirst().message().contains("sine"));
    }

    @Test
    void aDelayHistoryThatHasNotStartedCannotBeExported() {
        assertThrows(IllegalStateException.class, () -> new DelayHistory(1e-6).exportChronological(new StateSink() {
            @Override public void value(double value) { }
            @Override public void flag(boolean value) { }
        }));
    }

    private static List<CircuitModel.ComponentData> variants(ControlBlockDescriptor descriptor) {
        var result = new ArrayList<CircuitModel.ComponentData>();
        int[] arities = descriptor.inputPorts() < 0 ? new int[]{2, 3} : new int[]{descriptor.inputPorts()};
        double[] externals = descriptor.type() == 11 || descriptor.type() == 27 || descriptor.type() == 4 ? new double[]{0, 1} : new double[]{0};
        double[] subtypes = descriptor.type() == 4 ? new double[]{400, 401, 404, 405} : new double[]{0};
        for (int arity : arities) for (double external : externals) for (double subtype : subtypes) {
            var data = new CircuitModel.ComponentData(descriptor.type(), "block");
            double[] values = new double[8];
            for (var setting : descriptor.settings()) values[setting.slot()] = setting.defaultValue();
            if (descriptor.type() == 4) { values[0] = subtype; values[6] = external; }
            if (descriptor.type() == 11) values[1] = external;
            if (descriptor.type() == 27) values[2] = external;
            data.setNumericParameters(values);
            int inputs = descriptor.type() == 4 && external != 0 ? 5 : arity;
            data.setTerminalXLabels(new String[inputs]);
            data.setTerminalYLabels(new String[Math.max(1, descriptor.outputPorts() < 0 ? 3 : descriptor.outputPorts())]);
            if (descriptor.validateConfiguration(data, null).isEmpty()) result.add(data);
        }
        return result;
    }

    @Test
    void preparationRejectsABadPeriodWithItsPathBeforeAnySessionExists() {
        var model = constantModel();
        model.setTimeStep(DT);
        model.setSimulationDuration(1e-4);
        var preparation = new CircuitPreparation();
        var features = new SolverFeatures(0, false, false, new PeriodicSteadyStateSettings(10.5e-6));
        var result = preparation.prepare(preparation.capture(model),
                new PreparationRequest(new SolverOverrides(null, null, null, features), Map.of()));
        var rejected = assertInstanceOf(PreparationResult.Rejected.class, result);
        assertTrue(rejected.report().diagnostics().stream().anyMatch(item ->
                "INVALID_SETTING".equals(item.code()) && "periodicSteadyState.period".equals(item.parameterPath())));
    }

    @Test
    void aPreparedCircuitCarriesTheSearchIntoSessionAndEngineWithoutChangingTheCallersSettings() {
        var model = constantModel();
        model.setTimeStep(DT);
        model.setSimulationDuration(5e-5);
        var preparation = new CircuitPreparation();
        var features = new SolverFeatures(0, false, false, new PeriodicSteadyStateSettings(10e-6));
        var ready = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(preparation.capture(model),
                new PreparationRequest(new SolverOverrides(null, null, null, features), Map.of())));
        var prepared = ready.execution();
        assertEquals(10e-6, prepared.solverSettings().getPeriodicSteadyState().period());
        assertFalse(prepared.solverSettings().isDcOperatingPoint(), "the search turns the DC start on per session only");
        try (var session = SimulationSession.open(prepared, ExecutionOptions.defaults())) {
            session.run();
            assertTrue(session.steadyStateReport().converged());
        }
        var engine = new HeadlessSimulationEngine();
        var result = engine.runSimulation(prepared, ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        var report = (SteadyStateReport) result.getMetadata().get("steadyState");
        assertTrue(report.converged());
        assertTrue(result.getTimeArray().length >= 49);
        assertEquals(report.startTime() + DT, result.getTimeArray()[0], 1e-12);
    }

    @Test
    void theEngineMapsAnExhaustedSearchToFailedWithTheResidualAndTheWorstState() {
        var model = rampModel();
        model.setTimeStep(DT);
        model.setSimulationDuration(1e-4);
        var preparation = new CircuitPreparation();
        var features = new SolverFeatures(0, false, false, new PeriodicSteadyStateSettings(10e-6, 1e-4, 50));
        var ready = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(preparation.capture(model),
                new PreparationRequest(new SolverOverrides(null, null, null, features), Map.of())));
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertEquals(SimulationResult.Status.FAILED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("did not converge after 50 periods"), result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("worst"), result.getErrorMessage());
        var report = (SteadyStateReport) result.getMetadata().get("steadyState");
        assertFalse(report.converged());
        assertEquals(50, report.periods());
        assertEquals(500, result.getTotalTimeSteps());
    }

    @Test
    void cancellationDuringTheSearchGivesCancelledWithAnUnconvergedReport() {
        var model = rampModel();
        model.setTimeStep(DT);
        model.setSimulationDuration(1e-4);
        var preparation = new CircuitPreparation();
        var features = new SolverFeatures(0, false, false, new PeriodicSteadyStateSettings(10e-6, 1e-4, 1000));
        var ready = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(preparation.capture(model),
                new PreparationRequest(new SolverOverrides(null, null, null, features), Map.of())));
        int[] polls = {0};
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(),
                () -> ++polls[0] > 100);
        assertEquals(SimulationResult.Status.CANCELLED, result.getStatus());
        var report = (SteadyStateReport) result.getMetadata().get("steadyState");
        assertFalse(report.converged());
        assertEquals(10, report.periods());
    }

    @Test
    void theTimeCalculatorIsNotStateSoADisplayedClockDoesNotPreventConvergence() {
        var model = new CircuitModel();
        model.addControlComponent(component(58, "clock", new double[0], new String[0], "t"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"t"}));
        try (var session = SimulationSession.fromModel(model,
                settings(1e-4, new PeriodicSteadyStateSettings(10e-6)), ExecutionOptions.defaults())) {
            session.run();
            assertTrue(session.steadyStateReport().converged(), session.steadyStateReport().toString());
        }
    }

    /** One held-source value per period, exported through the coupler into a one-step-per-period monitor. */
    private static PeriodicStateMonitor heldSourceMonitor(double... heldPerPeriod) {
        var netlist = new gecko.core.circuit.netlist.CircuitNetlist();
        double[] parameters = new double[21];
        parameters[0] = 400;
        netlist.initNetlist(new gecko.core.circuit.circuitcomponents.CircuitTypCore[]{
                gecko.core.circuit.circuitcomponents.CircuitTypCore.LK_U}, new int[]{1}, new int[]{0},
                new int[]{1}, new double[][]{parameters}, 1, 1, 1);
        netlist.setComponentNameToIndex(Map.of("U.2", 0));
        var mapping = new ControlBlockMapping();
        mapping.addSourceMapping(null, 0, "U.2");
        mapping.addSourceMapping(null, 0, "missing");
        var coupler = new DomainCoupler();
        coupler.configureFromMapping(mapping);
        var monitor = new PeriodicStateMonitor(1e-4, 1000, 1);
        for (int period = 1; period <= heldPerPeriod.length; period++) {
            netlist.getParameter(0)[1] = heldPerPeriod[period - 1];
            monitor.begin();
            coupler.exportHeldSources(netlist, monitor.sink());
            monitor.commit(period, period * 1e-6);
        }
        return monitor;
    }

    @Test
    void theHeldValueOfASignalControlledSourceIsPartOfTheState() {
        var steady = heldSourceMonitor(2, 2, 2);
        assertEquals(0, steady.report().residual());
        var drifting = heldSourceMonitor(2, 2, 3);
        assertEquals(1.0 / 3, drifting.report().residual(), 1e-12);
        assertEquals("held source value[0]", drifting.report().worstState());
    }

    @Test
    void aCouplerWithoutAMappingOrANetlistExportsNothing() {
        var monitor = new PeriodicStateMonitor(1e-4, 1000, 1);
        for (int period = 1; period <= 2; period++) {
            monitor.begin();
            new DomainCoupler().exportHeldSources(null, monitor.sink());
            var mapped = new DomainCoupler();
            mapped.configureFromMapping(new ControlBlockMapping());
            mapped.exportHeldSources(null, monitor.sink());
            monitor.commit(period, period * 1e-6);
        }
        assertEquals(0, monitor.report().residual());
    }
}
