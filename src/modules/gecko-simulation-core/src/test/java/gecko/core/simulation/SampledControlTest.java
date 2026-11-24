package gecko.core.simulation;

import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.preparation.PreparedCircuit;
import gecko.core.preparation.SolverFeatures;
import gecko.core.preparation.SolverOverrides;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

/** Optional global CONTROL sample period: zero-order hold between samples at the first step with t >= k*Ts. */
class SampledControlTest {
    /** Integrator of a constant one: its output advances by the elapsed time between executions. */
    private static CircuitModel rampModel() {
        var model = new CircuitModel();
        model.addControlComponent(component(3, "one", new double[]{1}, new String[0], "one"));
        model.addControlComponent(component(3, "zero", new double[]{0}, new String[0], "zero"));
        model.addControlComponent(component(64, "int", new double[]{1, 0, -1e9, 1e9},
                new String[]{"one", "zero"}, "ramp"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"ramp"}));
        return model;
    }

    private static SolverSettingsCore settings(double dt, double duration, double samplePeriod) {
        return new SolverSettingsCore(SolverType.SOLVER_BE, dt, duration).withControlSamplePeriod(samplePeriod);
    }

    private static float[] run(CircuitModel model, SolverSettingsCore settings) {
        try (var session = SimulationSession.fromModel(model, settings, ExecutionOptions.defaults())) {
            session.run();
            return session.observe(0, 10000).values()[0];
        }
    }

    @Test
    void anIntegratorOutputIsAStaircaseWithOneRiseEveryHundredSteps() {
        float[] ramp = run(rampModel(), settings(1e-6, 1e-3, 100e-6));
        assertEquals(1000, ramp.length);
        // Trapezoid from a zero previous input: 0.5*Ts at the t=0 sample, then exactly Ts per sample.
        for (int step = 0; step < ramp.length; step++) {
            double expected = 100e-6 * (Math.floorDiv(step, 100) + 0.5);
            assertEquals(expected, ramp[step], 1e-9, "step " + step);
            if (step % 100 != 0) assertEquals(ramp[step - 1], ramp[step], "held at step " + step);
        }
        assertTrue(ramp[100] > ramp[99]);
    }

    @Test
    void anUnsetAndAZeroPeriodRunTheSameStepByStepPath() {
        float[] unset = run(rampModel(), new SolverSettingsCore(SolverType.SOLVER_BE, 1e-6, 1e-4));
        float[] zero = run(rampModel(), settings(1e-6, 1e-4, 0));
        assertArrayEquals(unset, zero);
        assertEquals(1e-6 * 100.5, unset[100], 1e-9);
    }

    @Test
    void aNonIntegralRatioSamplesAtTheFirstStepAtOrAfterEachMultipleWithoutDrift() {
        // dt = 3 us, Ts = 10 us: whole-microsecond arithmetic gives an exact reference schedule.
        float[] ramp = run(rampModel(), settings(3e-6, 3e-4, 10e-6));
        long ticks = -1;
        long next = 0;
        for (int step = 0; step < ramp.length; step++) {
            long micros = 3L * step;
            if (micros >= next * 10) {
                ticks++;
                next = micros / 10 + 1;
            }
            // The controller integrates its nominal Ts per tick, so the rounding of sample instants onto the
            // 3 us grid never accumulates: after n ticks the output is exactly Ts*(n + 0.5).
            assertEquals(10e-6 * (ticks + 0.5), ramp[step], 1e-9, "step " + step);
        }
    }

    @Test
    void aPeriodBelowTheStepRunsEveryStep() {
        float[] ramp = run(rampModel(), settings(1e-6, 1e-5, 0.25e-6));
        for (int step = 1; step < ramp.length; step++) assertEquals(1e-6, ramp[step] - ramp[step - 1], 1e-9);
    }

    @Test
    void aSplitSessionKeepsTheSampleClock() {
        var settings = settings(1e-6, 1e-4, 2.5e-6);
        float[] whole = run(rampModel(), settings);
        for (int split : new int[]{1, 2, 3, 37}) {
            try (var session = SimulationSession.fromModel(rampModel(), settings, ExecutionOptions.defaults())) {
                for (int index = 0; index < split; index++) session.step();
                session.run();
                assertArrayEquals(whole, session.observe(0, 10000).values()[0], "split at " + split);
            }
        }
    }

    @Test
    void anExtendedSegmentOnAFinerGridKeepsSamplingOnTheAbsoluteClock() {
        try (var session = SimulationSession.fromModel(rampModel(), settings(2e-6, 1e-5, 5e-6),
                ExecutionOptions.defaults())) {
            session.run();
            session.extend(3e-5, 1e-6);
            session.run();
            var observed = session.observe(0, 10000);
            double previous = Double.NaN;
            long next = 0;
            for (int index = 0; index < observed.times().length; index++) {
                long nanos = Math.round(observed.times()[index] * 1e9);
                boolean due = nanos >= next * 5000;
                if (due) next = nanos / 5000 + 1;
                float value = observed.values()[0][index];
                if (!due) assertEquals(previous, value, "held at " + nanos + " ns");
                else if (index > 0) assertNotEquals(previous, value, "sampled at " + nanos + " ns");
                previous = value;
            }
        }
    }

    @Test
    void aSamplePeriodMustBeFiniteAndNotNegative() {
        for (double invalid : new double[]{-1e-6, Double.NaN, Double.POSITIVE_INFINITY}) {
            var failure = assertThrows(IllegalArgumentException.class,
                    () -> SimulationSession.fromModel(rampModel(), settings(1e-6, 1e-4, invalid),
                            ExecutionOptions.defaults()));
            assertTrue(failure.getMessage().contains("sample period"), failure.getMessage());
        }
    }

    private static CircuitModel delayModel(double delay) {
        var model = new CircuitModel();
        model.addControlComponent(component(58, "time", new double[0], new String[0], "clock"));
        model.addControlComponent(component(25, "delay", new double[]{delay}, new String[]{"clock"}, "delayed"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"clock", "delayed"}));
        return model;
    }

    @Test
    void aDelayCountsSampledTicksWhenTheRatioIsNotIntegral() {
        // dt = 3 us, Ts = 10 us, delay = 20 us = two ticks. Sample instants are 0, 12, 21, 30, 42 ... us.
        try (var session = SimulationSession.fromModel(delayModel(20e-6), settings(3e-6, 3e-4, 10e-6),
                ExecutionOptions.defaults())) {
            session.run();
            var observed = session.observe(0, 10000);
            var sampleTimes = new ArrayList<Long>();
            long next = 0;
            for (int step = 0; step < observed.times().length; step++) {
                long micros = 3L * step;
                if (micros >= next * 10) {
                    sampleTimes.add(micros);
                    next = micros / 10 + 1;
                }
                int tick = sampleTimes.size() - 1;
                assertEquals(sampleTimes.getLast() * 1e-6, observed.values()[0][step], 1e-9, "clock " + step);
                assertEquals(tick < 2 ? 0 : sampleTimes.get(tick - 2) * 1e-6, observed.values()[1][step], 1e-9,
                        "delayed " + step);
            }
            assertTrue(sampleTimes.size() > 20);
        }
    }

    @Test
    void aDelayKeepsItsHistoryWhenASessionContinuesOnAnotherElectricalStep() {
        // Ts = 10 us stays the controller step while the electrical step changes from 4 us to 2 us and 3 us.
        try (var session = SimulationSession.fromModel(delayModel(20e-6), settings(4e-6, 6e-5, 10e-6),
                ExecutionOptions.defaults())) {
            session.run();
            session.extend(1.2e-4, 2e-6);
            session.run();
            session.extend(2.0e-4, 3e-6);
            session.run();
            var observed = session.observe(0, 10000);
            var sampleTimes = new ArrayList<Long>();
            long next = 0;
            for (int index = 0; index < observed.times().length; index++) {
                long nanos = Math.round(observed.times()[index] * 1e9);
                if (nanos >= next * 10000) {
                    sampleTimes.add(nanos);
                    next = nanos / 10000 + 1;
                }
                int tick = sampleTimes.size() - 1;
                assertEquals(sampleTimes.getLast() * 1e-9, observed.values()[0][index], 1e-9, "clock " + nanos);
                assertEquals(tick < 2 ? 0 : sampleTimes.get(tick - 2) * 1e-9, observed.values()[1][index], 1e-9,
                        "delayed " + nanos);
            }
            assertTrue(sampleTimes.size() > 15);
        }
    }

    @Test
    void aPeriodShorterThanTheStepBecomesAFullRateTickWhenTheStepShrinks() {
        try (var session = SimulationSession.fromModel(rampModel(), settings(4e-6, 2.4e-5, 3e-6),
                ExecutionOptions.defaults())) {
            session.run();
            session.extend(5.2e-5, 2e-6);
            session.run();
            float[] ramp = session.observe(0, 10000).values()[0];
            double[] times = session.observe(0, 10000).times();
            for (int index = 1; index < ramp.length; index++) {
                double rise = ramp[index] - ramp[index - 1];
                // At 4 us the period is below the step, so CONTROL ticks every step with 4 us. After
                // the step shrinks to 2 us it ticks on the 3 us period at the first step at or after each multiple.
                if (times[index] <= 2.4e-5) assertEquals(4e-6, rise, 1e-9, "t=" + times[index]);
                else assertTrue(rise == 0 || Math.abs(rise - 3e-6) < 1e-9, "t=" + times[index] + " rise " + rise);
            }
        }
    }

    @Test
    void configAndSettingsCopiesCarryThePeriod() {
        var config = SimulationConfig.builder().controlSamplePeriod(1e-4).build();
        assertEquals(1e-4, config.getSolverSettings().getControlSamplePeriod());
        assertEquals(1e-4, config.getSolverSettings().copy().getControlSamplePeriod());
        assertEquals(0, SimulationConfig.builder().build().getSolverSettings().getControlSamplePeriod());
        assertEquals(1e-4, SimulationConfig.builder().solverSettings(settings(1e-6, 1e-3, 1e-4)).build()
                .getSolverSettings().getControlSamplePeriod());
    }

    private static CircuitNetlist electricalGate() {
        var netlist = new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_S}, new int[]{1}, new int[]{0}, new int[]{-1},
                new double[][]{new double[]{1e9, 0.01, 1e9, 0, 0, 0, 0, 0, 0}}, 2, 0, 1);
        netlist.setComponentNameToIndex(Map.of("S.1", 0));
        return netlist;
    }

    @Test
    void aProbeWiredStraightToAGateOnlyReachesTheSwitchAtSamples() {
        var model = new CircuitModel();
        var probe = component(1, "VOLT.1", new double[0], new String[0], "gate");
        probe.setParameterStrings(new String[]{"1", "0"});
        model.addControlComponent(probe);
        var gate = component(6, "GATE.1", new double[0], new String[]{"gate"});
        gate.setParameterStrings(new String[]{"S.1"});
        model.addControlComponent(gate);
        var netlist = electricalGate();
        var seen = new ArrayList<Double>();
        try (var runtime = ControlRuntime.fromDeferred(model, 1e-6, null, 3e-6)) {
            for (int step = 0; step < 7; step++) {
                netlist.storeResults(new double[]{0, step % 2 == 0 ? 1 : 0}, new double[]{0});
                runtime.advanceAfterElectricalStep(netlist, step * 1e-6);
                seen.add(netlist.getParameter(0)[8]);
            }
        }
        // Samples at steps 0, 3 and 6 see 1, 0 and 1. Other steps hold the previous gate.
        assertEquals(List.of(1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 1.0), seen);
    }

    @Test
    void aProbeFedSourceKeepsItsValueBetweenSamples() {
        var model = new CircuitModel();
        var source = component(4, "U.2", new double[]{400, -0.02}, new String[]{"1"}, "0");
        source.setParameterStrings(new String[]{"v"});
        model.addCircuitComponent(source);
        var probe = component(1, "VOLT.1", new double[0], new String[0], "v");
        probe.setParameterStrings(new String[]{"1", "0"});
        model.addControlComponent(probe);
        var netlist = new CircuitNetlist();
        double[] parameters = new double[21];
        parameters[0] = 400;
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U}, new int[]{1}, new int[]{0}, new int[]{1},
                new double[][]{parameters}, 1, 1, 1);
        netlist.setComponentNameToIndex(Map.of("U.2", 0));
        var applied = new ArrayList<Double>();
        try (var runtime = ControlRuntime.fromDeferred(model, 1e-6, null, 3e-6)) {
            runtime.seedSources(netlist);
            for (int step = 0; step < 7; step++) {
                netlist.storeResults(new double[]{0, 5 + 2 * step}, new double[]{0});
                runtime.advanceAfterElectricalStep(netlist, step * 1e-6);
                applied.add(netlist.getParameter(0)[1]);
            }
        }
        // The source a solve sees is the sampled measurement from the step after which CONTROL last ran.
        assertEquals(List.of(0.0, 5.0, 5.0, 5.0, 11.0, 11.0, 11.0), applied);
    }

    @Test
    void aZeroPeriodOnBuckSimpleIsBitIdenticalToAnUnsetPeriod() throws Exception {
        String circuit = Path.of(Objects.requireNonNull(
                getClass().getResource("/ipes/buck_simple.ipes")).toURI()).toString();
        var unset = trace(SimulationConfig.builder().circuitFile(circuit).simulationDuration(2e-4).build());
        var zero = trace(SimulationConfig.builder().circuitFile(circuit).simulationDuration(2e-4)
                .controlSamplePeriod(0).build());
        assertEquals(unset.size(), zero.size());
        assertTrue(unset.size() > 100);
        for (int index = 0; index < unset.size(); index++) {
            assertArrayEquals(unset.get(index), zero.get(index), "trace row " + index);
        }
        var sampled = trace(SimulationConfig.builder().circuitFile(circuit).simulationDuration(2e-4)
                .controlSamplePeriod(50e-6).build());
        assertFalse(java.util.Arrays.deepEquals(unset.toArray(), sampled.toArray()),
                "a positive period must reach the engine and change the PWM");
    }

    @Test
    void aScopedProbeStaysLiveWhileEveryControllerOutputItFeedsIsHeld() {
        var model = new CircuitModel();
        var probe = component(1, "VOLT.1", new double[0], new String[0], "v");
        probe.setParameterStrings(new String[]{"1", "0"});
        model.addControlComponent(probe);
        model.addControlComponent(component(7, "gain", new double[]{2}, new String[]{"v"}, "doubled"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"v", "doubled"}));
        var netlist = new CircuitNetlist();
        var live = new ArrayList<Float>();
        var held = new ArrayList<Float>();
        try (var runtime = ControlRuntime.fromDeferred(model, 1e-6, null, 3e-6)) {
            for (int step = 0; step < 7; step++) {
                netlist.storeResults(new double[]{0, 5 + 2 * step}, new double[]{0});
                runtime.advanceAfterElectricalStep(netlist, step * 1e-6);
                float[] scope = runtime.sampleScope();
                live.add(scope[0]);
                held.add(scope[1]);
            }
        }
        // Measurement probes are the physical signal. The gain executes only at the samples 0, 3 and 6.
        assertEquals(List.of(5f, 7f, 9f, 11f, 13f, 15f, 17f), live);
        assertEquals(List.of(10f, 10f, 10f, 22f, 22f, 22f, 34f), held);
    }

    @Test
    void aPeriodEqualToTheStepPublishesGatesAndSourcesExactlyLikeNoPeriod() {
        var gateModel = new CircuitModel();
        var probe = component(1, "VOLT.1", new double[0], new String[0], "gate");
        probe.setParameterStrings(new String[]{"1", "0"});
        gateModel.addControlComponent(probe);
        var gate = component(6, "GATE.1", new double[0], new String[]{"gate"});
        gate.setParameterStrings(new String[]{"S.1"});
        gateModel.addControlComponent(gate);
        var sourceModel = new CircuitModel();
        var source = component(4, "U.2", new double[]{400, -0.02}, new String[]{"1"}, "0");
        source.setParameterStrings(new String[]{"v"});
        sourceModel.addCircuitComponent(source);
        var sourceProbe = component(1, "VOLT.1", new double[0], new String[0], "v");
        sourceProbe.setParameterStrings(new String[]{"1", "0"});
        sourceModel.addControlComponent(sourceProbe);
        for (double period : new double[]{0, 1e-6}) {
            assertEquals(List.of(1.0, 0.0, 1.0, 0.0, 1.0), gateValues(gateModel, period), "gate, Ts=" + period);
            assertEquals(List.of(0.0, 5.0, 7.0, 9.0, 11.0), sourceValues(sourceModel, period), "source, Ts=" + period);
        }
    }

    private static List<Double> gateValues(CircuitModel model, double period) {
        var netlist = electricalGate();
        var seen = new ArrayList<Double>();
        try (var runtime = ControlRuntime.fromDeferred(model, 1e-6, null, period)) {
            for (int step = 0; step < 5; step++) {
                netlist.storeResults(new double[]{0, step % 2 == 0 ? 1 : 0}, new double[]{0});
                runtime.advanceAfterElectricalStep(netlist, step * 1e-6);
                seen.add(netlist.getParameter(0)[8]);
            }
        }
        return seen;
    }

    private static List<Double> sourceValues(CircuitModel model, double period) {
        var netlist = new CircuitNetlist();
        double[] parameters = new double[21];
        parameters[0] = 400;
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U}, new int[]{1}, new int[]{0}, new int[]{1},
                new double[][]{parameters}, 1, 1, 1);
        netlist.setComponentNameToIndex(Map.of("U.2", 0));
        var seen = new ArrayList<Double>();
        try (var runtime = ControlRuntime.fromDeferred(model, 1e-6, null, period)) {
            runtime.seedSources(netlist);
            for (int step = 0; step < 5; step++) {
                netlist.storeResults(new double[]{0, 5 + 2 * step}, new double[]{0});
                runtime.advanceAfterElectricalStep(netlist, step * 1e-6);
                seen.add(netlist.getParameter(0)[1]);
            }
        }
        return seen;
    }

    @Test
    void aPeriodEqualToTheStepOnBuckSimpleIsBitIdenticalToNoPeriod() throws Exception {
        String circuit = Path.of(Objects.requireNonNull(
                getClass().getResource("/ipes/buck_simple.ipes")).toURI()).toString();
        var none = trace(SimulationConfig.builder().circuitFile(circuit).simulationDuration(2e-4).build());
        var everyStep = trace(SimulationConfig.builder().circuitFile(circuit).simulationDuration(2e-4)
                .controlSamplePeriod(1e-6).build());
        assertEquals(none.size(), everyStep.size());
        for (int index = 0; index < none.size(); index++) assertArrayEquals(none.get(index), everyStep.get(index));
    }

    private static PreparedCircuit prepare(CircuitModel model, SolverOverrides overrides) {
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), new PreparationRequest(overrides, Map.of()));
        return assertInstanceOf(PreparationResult.Ready.class, result).execution();
    }

    private static CircuitModel timedRamp() {
        var model = rampModel();
        model.setTimeStep(1e-6);
        model.setSimulationDuration(3e-4);
        return model;
    }

    @Test
    void aPreparedCircuitCarriesTheFeatureIntoEveryIndependentSession() {
        var prepared = prepare(timedRamp(), new SolverOverrides(null, null, null, new SolverFeatures(100e-6)));
        assertEquals(100e-6, prepared.solverSettings().getControlSamplePeriod());
        prepared.solverSettings().withControlSamplePeriod(5);
        assertEquals(100e-6, prepared.solverSettings().getControlSamplePeriod(), "settings are detached");
        for (int run = 0; run < 2; run++) {
            try (var session = SimulationSession.open(prepared, ExecutionOptions.defaults())) {
                session.run();
                float[] ramp = session.observe(0, 10000).values()[0];
                assertEquals(0.5e-4, ramp[99], 1e-9, "run " + run);
                assertEquals(1.5e-4, ramp[100], 1e-9, "run " + run);
            }
        }
    }

    @Test
    void aPreparedCircuitInheritsNoPeriodUnlessRequested() {
        var inherited = prepare(timedRamp(), SolverOverrides.none());
        assertEquals(0, inherited.solverSettings().getControlSamplePeriod());
        var legacy = prepare(timedRamp(), new SolverOverrides(null, 2e-6, null));
        assertEquals(0, legacy.solverSettings().getControlSamplePeriod());
        assertEquals(2e-6, legacy.solverSettings().getStepWidth());
        var overridden = prepare(timedRamp(), new SolverOverrides(null, 2e-6, 1e-4, new SolverFeatures(1e-5)));
        assertEquals(1e-5, overridden.solverSettings().getControlSamplePeriod());
        assertEquals(2e-6, overridden.solverSettings().getStepWidth());
        try (var session = SimulationSession.open(inherited, ExecutionOptions.defaults())) {
            session.run();
            float[] ramp = session.observe(0, 10000).values()[0];
            assertEquals(1e-6, ramp[5] - ramp[4], 1e-9, "no period runs every step");
        }
    }

    @Test
    void preparationRejectsAnInvalidPeriodBeforeAnySessionExists() {
        for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            var preparation = new CircuitPreparation();
            var result = preparation.prepare(preparation.capture(timedRamp()), new PreparationRequest(
                    new SolverOverrides(null, null, null, new SolverFeatures(invalid)), Map.of()));
            var rejected = assertInstanceOf(PreparationResult.Rejected.class, result);
            assertTrue(rejected.report().diagnostics().stream()
                    .anyMatch(item -> "controlSamplePeriod".equals(item.parameterPath())), "period " + invalid);
        }
    }

    @Test
    void anInvalidPeriodFailsBeforeCalculatorsAreBuilt() {
        for (double invalid : new double[]{-1e-6, Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY}) {
            var failure = assertThrows(IllegalArgumentException.class,
                    () -> ControlRuntime.fromDeferred(delayModel(2e-6), 1e-6, null, invalid), "period " + invalid);
            assertTrue(failure.getMessage().contains("sample period"), failure.getMessage());
        }
    }

    private static final double[] INVALID_STEPS = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, -1e-6};

    @Test
    void aRejectedStepChangeBeforeStartKeepsTheStagedPlanCommittable() {
        for (double period : new double[]{0, 4e-6}) {
            try (var runtime = ControlRuntime.fromDeferred(delayModel(4e-6), 1e-6, null, period)) {
                Runnable staged = runtime.prepareTimeStepChange(2e-6);
                for (double invalid : INVALID_STEPS) {
                    var failure = assertThrows(IllegalArgumentException.class,
                            () -> runtime.prepareTimeStepChange(invalid), "step " + invalid);
                    assertTrue(failure.getMessage().contains("Time step"), failure.getMessage());
                }
                assertDoesNotThrow(staged::run);
                runtime.advanceAfterElectricalStep(new CircuitNetlist(), 0);
                runtime.advanceAfterElectricalStep(new CircuitNetlist(), 2e-6);
                // With a 4 us period the second electrical step is not a sample, so the clock holds its tick-zero value.
                assertEquals(period == 0 ? 2e-6f : 0f, runtime.sampleScope()[0], 1e-12f);
            }
        }
    }

    @Test
    void aRejectedStepChangeAfterStartLeavesHistoryAndLaterContinuationIntact() {
        for (double period : new double[]{0, 4e-6}) {
            try (var plain = ControlRuntime.fromDeferred(delayModel(4e-6), 1e-6, null, period);
                    var rejected = ControlRuntime.fromDeferred(delayModel(4e-6), 1e-6, null, period)) {
                var electrical = new CircuitNetlist();
                for (int step = 0; step < 6; step++) {
                    plain.advanceAfterElectricalStep(electrical, step * 1e-6);
                    rejected.advanceAfterElectricalStep(electrical, step * 1e-6);
                }
                for (double invalid : INVALID_STEPS) {
                    assertThrows(IllegalArgumentException.class, () -> rejected.prepareTimeStepChange(invalid));
                }
                // A legitimate continuation on a finer step still commits on both runtimes and agrees.
                plain.prepareTimeStepChange(0.5e-6).run();
                rejected.prepareTimeStepChange(0.5e-6).run();
                for (int step = 0; step < 12; step++) {
                    double time = 6e-6 + step * 0.5e-6;
                    plain.advanceAfterElectricalStep(electrical, time);
                    rejected.advanceAfterElectricalStep(electrical, time);
                    assertArrayEquals(plain.sampleScope(), rejected.sampleScope(), "step " + step);
                }
            }
        }
    }

    @Test
    void theEnginePassesTheConfiguredPeriodThroughPreparation() throws Exception {
        String circuit = Path.of(Objects.requireNonNull(
                getClass().getResource("/ipes/buck_simple.ipes")).toURI()).toString();
        var config = SimulationConfig.builder().circuitFile(circuit).simulationDuration(2e-4)
                .controlSamplePeriod(50e-6).build();
        assertEquals(50e-6, config.explicitSolverOverrides().features().controlSamplePeriod());
        assertEquals(0, SimulationConfig.builder().build().explicitSolverOverrides().features().controlSamplePeriod());
        var first = trace(config);
        var second = trace(config);
        assertEquals(first.size(), second.size());
        for (int index = 0; index < first.size(); index++) assertArrayEquals(first.get(index), second.get(index));
    }

    /** Raw IEEE-754 bits of every node voltage and current, so equality is exact rather than tolerant. */
    private static List<long[]> trace(SimulationConfig config) {
        var rows = new ArrayList<long[]>();
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, volts, currents, circuit) -> {
            long[] row = new long[1 + volts.length + currents.length];
            row[0] = Double.doubleToRawLongBits(time);
            for (int index = 0; index < volts.length; index++) row[1 + index] = Double.doubleToRawLongBits(volts[index]);
            for (int index = 0; index < currents.length; index++) {
                row[1 + volts.length + index] = Double.doubleToRawLongBits(currents[index]);
            }
            rows.add(row);
        });
        var result = engine.runSimulation(config);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return rows;
    }
}
