package gecko.core.simulation;

import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

import gecko.core.allg.PeriodicSteadyStateSettings;
import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.GateCalculator;
import gecko.core.control.calculators.SignalCalculatorImport;
import gecko.core.control.calculators.SignalCalculatorSinus;
import gecko.core.control.calculators.TimeCalculator;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitResource;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.preparation.SolverFeatures;
import gecko.core.preparation.SolverOverrides;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Absolute-time forcing that the periodic state does not contain must repeat in the period, or be rejected. */
class PeriodicSteadyStateForcingTest {
    private static final String FLAT_PREFIX = "0 0\n1 0\n1.1 1\n1.9 1\n2 0\n";

    private static CircuitModel importModel(String table, double dt, double duration) {
        var model = new CircuitModel();
        model.setTimeStep(dt);
        model.setSimulationDuration(duration);
        var imported = component(4, "imported", new double[]{406, 1, 1, 0, 0, .5, 0}, new String[0], "wave");
        imported.getImportTokens().put("externalDataFileHashValue", "42");
        model.getResources().put(42L, new CircuitResource(42, "wave.txt", "test",
                table.getBytes(StandardCharsets.UTF_8), List.of()));
        model.addControlComponent(imported);
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"wave"}));
        return model;
    }

    private static PreparationResult prepare(CircuitModel model, double period) {
        var preparation = new CircuitPreparation();
        var features = new SolverFeatures(0, false, false, new PeriodicSteadyStateSettings(period, 1e-4, 20));
        return preparation.prepare(preparation.capture(model),
                new PreparationRequest(new SolverOverrides(SolverType.SOLVER_BE, null, null, features), Map.of()));
    }

    private static void assertRejectedWithPath(PreparationResult result, String path) {
        var rejected = assertInstanceOf(PreparationResult.Rejected.class, result);
        assertTrue(rejected.report().diagnostics().stream().anyMatch(item ->
                "INVALID_SETTING".equals(item.code()) && path.equals(item.parameterPath())), rejected.report().toString());
    }

    @Test
    void theConfirmedFlatPrefixPeriodIsRejectedByPreparationAndByTheSession() {
        assertRejectedWithPath(prepare(importModel(FLAT_PREFIX, .1, 1.5), .2), "periodicSteadyState.period");
        var settings = new SolverSettingsCore(SolverType.SOLVER_BE, .1, 1.5)
                .withPeriodicSteadyState(new PeriodicSteadyStateSettings(.2, 1e-4, 10));
        var failure = assertThrows(IllegalArgumentException.class, () -> SimulationSession.fromModel(
                importModel(FLAT_PREFIX, .1, 1.5), settings, ExecutionOptions.defaults()));
        assertTrue(failure.getMessage().contains("periodicSteadyState.period"), failure.getMessage());
    }

    @Test
    void theTableDurationAndItsMultiplesConvergeWithTheFullRangeInTheMainPhase() {
        for (double period : new double[]{2, 4}) {
            var ready = assertInstanceOf(PreparationResult.Ready.class, prepare(importModel(FLAT_PREFIX, .1, 2), period));
            try (var session = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
                session.run();
                var report = session.steadyStateReport();
                assertTrue(report.converged(), "period " + period + " " + report);
                double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
                for (double value : session.observe(0, 100).values()[0]) {
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
                assertEquals(0, min, 1e-6);
                assertEquals(1, max, 1e-6);
            }
        }
    }

    @Test
    void aConstantTableRepeatsInAnyPeriod() {
        assertInstanceOf(PreparationResult.Ready.class, prepare(importModel("0 5\n1 5\n", .1, 1), .3));
    }

    @Test
    void repeatsEveryAcceptsOnlyWholeMultiplesOfTheDuration() {
        var table = new SignalCalculatorImport(new double[][]{{0, 1, 1.1, 1.9, 2}, {0, 0, 1, 1, 0}});
        assertTrue(table.repeatsEvery(2));
        assertTrue(table.repeatsEvery(4));
        assertTrue(table.repeatsEvery(2 * (1 + 1e-10)));
        assertFalse(table.repeatsEvery(1));
        assertFalse(table.repeatsEvery(2 * (1 + 1e-6)));
        assertFalse(table.repeatsEvery(1e-12));
        assertFalse(table.repeatsEvery(0));
        assertFalse(table.repeatsEvery(-2));
        assertFalse(table.repeatsEvery(Double.NaN));
        assertFalse(table.repeatsEvery(Double.POSITIVE_INFINITY));
    }

    @Test
    void aShiftedFirstTimeHoldsAFlatPrefixAndIsRejectedEvenAtTheDuration() {
        var shifted = new SignalCalculatorImport(new double[][]{{1, 2, 3}, {0, 0, 1}});
        assertFalse(shifted.repeatsEvery(2));
        assertFalse(shifted.repeatsEvery(4));
        var issues = PeriodicSteadyStateValidation.circuitIssues(2, null, List.of(shifted));
        assertEquals("periodicSteadyState.period", issues.getFirst().parameterPath());
        assertTrue(new SignalCalculatorImport(new double[][]{{-1, 0, 1}, {0, 0, 1}}).repeatsEvery(2));
    }

    @Test
    void theImporterOutputIsUnchangedWhenTheSearchIsOff() {
        var reference = new SignalCalculatorImport(new double[][]{{0, 1, 1.1, 1.9, 2}, {0, 0, 1, 1, 0}});
        var settings = new SolverSettingsCore(SolverType.SOLVER_BE, .1, 3);
        try (var session = SimulationSession.fromModel(importModel(FLAT_PREFIX, .1, 3), settings, ExecutionOptions.defaults())) {
            session.run();
            var observed = session.observe(0, 100);
            for (int index = 0; index < observed.times().length; index++) {
                reference.setSimulationTime(observed.times()[index]);
                reference.berechneYOUT(.1);
                assertEquals((float) reference._outputSignal[0][0], observed.values()[0][index], 1e-6, "row " + index);
            }
        }
    }

    @Test
    void aTimeBlockMayFeedAScopeButNotControlLogic() throws Exception {
        var model = new CircuitModel();
        model.addControlComponent(component(58, "clock", new double[0], new String[0], "t"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"t"}));
        try (var session = SimulationSession.fromModel(model, new SolverSettingsCore(SolverType.SOLVER_BE, 1e-6, 1e-4)
                .withPeriodicSteadyState(new PeriodicSteadyStateSettings(10e-6)), ExecutionOptions.defaults())) {
            session.run();
            assertTrue(session.steadyStateReport().converged());
        }
        var compared = new CircuitModel();
        compared.addControlComponent(component(58, "clock", new double[0], new String[0], "t"));
        compared.addControlComponent(component(3, "limit", new double[]{1e9}, new String[0], "big"));
        compared.addControlComponent(component(46, "compare", new double[0], new String[]{"t", "big"}, "flag"));
        compared.addControlComponent(component(5, "scope", new double[0], new String[]{"flag"}));
        var failure = assertThrows(IllegalArgumentException.class, () -> SimulationSession.fromModel(compared,
                new SolverSettingsCore(SolverType.SOLVER_BE, 1e-6, 1e-4).withPeriodicSteadyState(
                        new PeriodicSteadyStateSettings(10e-6)), ExecutionOptions.defaults()));
        assertTrue(failure.getMessage().contains("feeds another CONTROL block"), failure.getMessage());
        var time = new TimeCalculator();
        var gate = new GateCalculator();
        gate.setInputSignal(0, time, 0);
        var issues = PeriodicSteadyStateValidation.circuitIssues(10e-6, null, List.<AbstractControlCalculatable>of(time, gate));
        assertEquals(1, issues.size(), issues.toString());
        assertEquals("periodicSteadyState", issues.getFirst().parameterPath());
    }

    private static CircuitModel modulated(int subtype, double frequency) {
        var model = new CircuitModel();
        double[] values = {1, frequency, 0, 0, 0.5};
        String[] names = {"a", "f", "o", "p", "d"};
        for (int index = 0; index < 5; index++) {
            model.addControlComponent(component(3, names[index], new double[]{values[index]}, new String[0], names[index]));
        }
        model.addControlComponent(component(4, "wave", new double[]{subtype, 1, frequency, 0, 0, 0.5, 1}, names, "w"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"w"}));
        return model;
    }

    @Test
    void aModulatedSineIsRejectedAndAModulatedTriangleConverges() {
        // 125 kHz at 1 us gives a clamped triangle step of exactly 0.5, so the carrier period is exactly 8 steps.
        var settings = new SolverSettingsCore(SolverType.SOLVER_BE, 1e-6, 2e-5)
                .withPeriodicSteadyState(new PeriodicSteadyStateSettings(8e-6));
        var failure = assertThrows(IllegalArgumentException.class,
                () -> SimulationSession.fromModel(modulated(400, 125e3), settings, ExecutionOptions.defaults()));
        assertTrue(failure.getMessage().contains("input frequency"), failure.getMessage());
        try (var session = SimulationSession.fromModel(modulated(401, 125e3), settings, ExecutionOptions.defaults())) {
            session.run();
            assertTrue(session.steadyStateReport().converged(), session.steadyStateReport().toString());
        }
    }

    @Test
    void aMatrixConverterBlockIsRejected() {
        var data = new CircuitModel.ComponentData(59, "matrix");
        data.setNumericParameters(new double[8]);
        data.setTerminalXLabels(new String[8]);
        data.setTerminalYLabels(new String[9]);
        var matrix = ControlBlockDescriptor.find(59).createCalculator(data, null);
        var issues = PeriodicSteadyStateValidation.circuitIssues(10e-6, null, List.of(matrix));
        assertEquals(1, issues.size(), issues.toString());
        assertTrue(issues.getFirst().message().contains("absolute time"));
    }

    @Test
    void aFixedSineNeedsAWholeNonzeroCycleCountExceptExactlyZeroFrequency() {
        assertFalse(sineIssues(2e-10, 1).isEmpty(), "a tiny nonzero cycle count rounds to zero");
        assertTrue(sineIssues(10e-6, 100e3).isEmpty());
        assertTrue(sineIssues(10e-6, 200e3).isEmpty());
        assertTrue(sineIssues(10e-6, -100e3).isEmpty(), "negative frequency counts absolute cycles");
        assertFalse(sineIssues(10e-6, 130e3).isEmpty());
        assertFalse(sineIssues(10e-6, 50e3).isEmpty(), "half a cycle is not whole");
        assertTrue(sineIssues(10e-6, 0).isEmpty(), "a zero-frequency sine is constant");
    }

    private static List<SimulationTiming.SettingIssue> sineIssues(double period, double frequency) {
        return PeriodicSteadyStateValidation.circuitIssues(period, null,
                List.of(new SignalCalculatorSinus(0, 1, frequency, 0, 0, .5)));
    }

    @Test
    void everyAbsoluteTimeSineSourceFamilyIsChecked() {
        for (var type : new gecko.core.circuit.circuitcomponents.CircuitTypCore[]{
                gecko.core.circuit.circuitcomponents.CircuitTypCore.LK_U, gecko.core.circuit.circuitcomponents.CircuitTypCore.LK_I,
                gecko.core.circuit.circuitcomponents.CircuitTypCore.TH_FLOW, gecko.core.circuit.circuitcomponents.CircuitTypCore.TH_TEMP,
                gecko.core.circuit.circuitcomponents.CircuitTypCore.REL_MMF}) {
            for (double[] frequency : new double[][]{{100e3, 0}, {130e3, 1}}) {
                double[] sine = new double[21];
                sine[0] = gecko.core.circuit.SourceType.QUELLE_SIN_NEW;
                sine[1] = sine[20] = 1;
                sine[2] = frequency[0];
                var netlist = new gecko.core.circuit.netlist.CircuitNetlist();
                netlist.initNetlist(new gecko.core.circuit.circuitcomponents.CircuitTypCore[]{type},
                        new int[]{1}, new int[]{0}, new int[]{-1}, new double[][]{sine}, 1, 0, 1);
                assertEquals((int) frequency[1], PeriodicSteadyStateValidation.circuitIssues(10e-6, netlist, List.of()).size(),
                        type + " at " + frequency[0]);
            }
        }
    }
}
