package gecko.core.simulation;

import static org.junit.jupiter.api.Assertions.*;

import gecko.core.allg.PeriodicSteadyStateSettings;
import gecko.core.allg.SolverSettingsCore;
import gecko.core.io.CircuitFileParser;
import gecko.core.preparation.ExecutionOptions;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/** Periodic steady state of the buck_simple converter against a long plain transient. */
class PeriodicSteadyStateBuckTest {
    private static final double DT = 1e-6;
    /** The carrier clamps at 35 kHz and 1 us per step to 30 us. The nominal period would be 28.57 us. */
    private static final double PERIOD = 30e-6;
    private static final int OUTPUT = 2;

    private static String circuit() throws Exception {
        return Path.of(Objects.requireNonNull(PeriodicSteadyStateBuckTest.class.getResource("/ipes/buck_simple.ipes")).toURI()).toString();
    }

    private static double average(float[] values, int from, int to) {
        double sum = 0;
        for (int index = from; index < to; index++) sum += values[index];
        return sum / (to - from);
    }

    @Test
    void theSimulatedCarrierPeriodIsThirtyMicrosecondsNotTheNominalPeriod() {
        double carrierSteps = 2 * Math.ceil(1 / (2 * 35e3 * DT));
        assertEquals(PERIOD, carrierSteps * DT, 1e-15);
        assertNotEquals(1 / 35e3, PERIOD, 1e-6);
    }

    @Test
    void theSearchAveragesWithinOnePercentOfALongPlainTransient() throws Exception {
        var engine = new HeadlessSimulationEngine();
        var plain = engine.runSimulation(SimulationConfig.builder().circuitFile(circuit()).simulationDuration(6e-3).build());
        assertTrue(plain.isSuccess(), plain.getErrorMessage());
        float[] reference = plain.getSignalData(OUTPUT);
        assertTrue(reference.length >= 6000, "at least 200 carrier periods");
        double plainAverage = average(reference, reference.length - 30, reference.length);

        var steady = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder().circuitFile(circuit())
                .simulationDuration(PERIOD).periodicSteadyState(new PeriodicSteadyStateSettings(PERIOD, 1e-4, 1000)).build());
        assertTrue(steady.isSuccess(), steady.getErrorMessage());
        var report = (SteadyStateReport) steady.getMetadata().get("steadyState");
        assertTrue(report.converged(), report.toString());
        assertTrue(report.periods() < 1000, "converged before the limit: " + report.periods());
        float[] main = steady.getSignalData(OUTPUT);
        assertEquals(30, main.length, "one carrier period of main samples");
        assertEquals(plainAverage, average(main, 0, main.length), 0.01 * plainAverage);
    }

    @Test
    void aNullSettingGivesTheSameBitsAsNoSetting() throws Exception {
        var plain = trace(SimulationConfig.builder().circuitFile(circuit()).simulationDuration(2e-4).build());
        var explicitNull = trace(SimulationConfig.builder().circuitFile(circuit()).simulationDuration(2e-4)
                .periodicSteadyState(null).build());
        assertEquals(plain.size(), explicitNull.size());
        assertTrue(plain.size() > 100);
        for (int index = 0; index < plain.size(); index++) assertArrayEquals(plain.get(index), explicitNull.get(index), "row " + index);
    }

    /** Raw IEEE-754 bits of every node voltage and current, so equality is exact rather than tolerant. */
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
    void theFirstSearchStepUsesTheSameStartAsADcOperatingPointSessionAndLeavesTheCallersSettingsAlone() throws Exception {
        var model = new CircuitFileParser().parse(circuit());
        var dcOnly = new SolverSettingsCore(model.getSolverType(), DT, 1e-3).withDcOperatingPoint(true);
        var search = new SolverSettingsCore(model.getSolverType(), DT, 1e-3)
                .withPeriodicSteadyState(new PeriodicSteadyStateSettings(PERIOD));
        float[] expected;
        try (var session = SimulationSession.fromModel(new CircuitFileParser().parse(circuit()), dcOnly, ExecutionOptions.defaults())) {
            expected = session.step().values();
        }
        try (var session = SimulationSession.fromModel(new CircuitFileParser().parse(circuit()), search, ExecutionOptions.defaults())) {
            assertArrayEquals(expected, session.step().values());
        }
        assertFalse(search.isDcOperatingPoint(), "the search must not change the caller's settings");
    }

    @Test
    void theSearchAlsoConvergesWithASampledControllerAndRejectsAPeriodOffTheSampleGrid() throws Exception {
        var model = new CircuitFileParser().parse(circuit());
        var good = new SolverSettingsCore(model.getSolverType(), DT, PERIOD).withControlSamplePeriod(3e-6)
                .withPeriodicSteadyState(new PeriodicSteadyStateSettings(PERIOD, 1e-4, 2000));
        try (var session = SimulationSession.fromModel(model, good, ExecutionOptions.defaults())) {
            session.run();
            assertTrue(session.steadyStateReport().converged(), session.steadyStateReport().toString());
        }
        var bad = new SolverSettingsCore(model.getSolverType(), DT, PERIOD).withControlSamplePeriod(3e-6)
                .withPeriodicSteadyState(new PeriodicSteadyStateSettings(31e-6));
        assertTrue(SimulationTiming.settingsIssues(bad).stream()
                .anyMatch(issue -> issue.parameterPath().equals("periodicSteadyState.period")));
    }

    @Test
    void theSearchIsCompatibleWithZeroCrossingEventLocation() throws Exception {
        var result = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder().circuitFile(circuit())
                .simulationDuration(PERIOD).zeroCrossing(true)
                .periodicSteadyState(new PeriodicSteadyStateSettings(PERIOD, 1e-4, 2000)).build());
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertTrue(((SteadyStateReport) result.getMetadata().get("steadyState")).converged());
    }

    @Test
    void anUnreachableToleranceFailsTheEngineRunWithAReport() throws Exception {
        var result = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder().circuitFile(circuit())
                .simulationDuration(PERIOD).periodicSteadyState(new PeriodicSteadyStateSettings(PERIOD, 1e-12, 3)).build());
        assertEquals(SimulationResult.Status.FAILED, result.getStatus());
        assertFalse(((SteadyStateReport) result.getMetadata().get("steadyState")).converged());
    }

    @Test
    void cancellingTheSearchOnTheBuckGivesCancelled() throws Exception {
        int[] polls = {0};
        var result = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder().circuitFile(circuit())
                .simulationDuration(PERIOD).periodicSteadyState(new PeriodicSteadyStateSettings(PERIOD, 1e-12, 1000)).build(),
                () -> ++polls[0] > 100);
        assertEquals(SimulationResult.Status.CANCELLED, result.getStatus());
        assertFalse(((SteadyStateReport) result.getMetadata().get("steadyState")).converged());
    }
}
