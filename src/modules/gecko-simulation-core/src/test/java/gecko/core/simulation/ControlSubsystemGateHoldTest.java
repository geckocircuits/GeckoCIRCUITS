package gecko.core.simulation;

import static gecko.core.simulation.ControlElectricalEngineContractTest.element;
import static gecko.core.simulation.ControlSubsystemRateTest.block;
import static gecko.core.simulation.ControlSubsystemRateTest.parse;
import static gecko.core.simulation.ControlSubsystemRateTest.row;
import static gecko.core.simulation.ControlSubsystemRateTest.sheet;
import static org.junit.jupiter.api.Assertions.*;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * A gate outside every configured subsystem belongs to the default group. Its clock is the global period.
 * Its producer runs on a faster subsystem clock. The gate must follow its own clock.
 * The switch stays off until the first default-group sample that sees the command.
 * The first physical solve after that sample sees the switch on.
 * A producer sample between two default-group samples must not reach the switch.
 *
 * <p>The circuit is a 10 V source, the switch S.1 and a 1 ohm load.
 * The global period is 100 us and the grid step is 1 us.
 * A fast sheet of 10 us gives TIME times 1e6, which is 0 at time zero and 10 or more afterwards.
 * So the command is above the gate threshold from the first fast sample after time zero.
 */
class ControlSubsystemGateHoldTest {
    private static final long FAST = 101;
    private static final double STEP = 1e-6;
    private static final double GLOBAL = 100e-6;
    private static final double R_ON = 0.01;
    /** The same off resistance as the {@code gateSwitch()} fixture, so the direct and the public case agree. */
    private static final double R_OFF = 1e9;
    private static final Map<String, Double> FAST_PERIOD = Map.of("fast", 10e-6);

    private static String electrical() {
        // The switch parameters are the off resistance, the on resistance and the off resistance again.
        String switchParameters = (long) R_OFF + " " + R_ON + " " + (long) R_OFF + " 0 0 0 0 0 0 0 0 0";
        return element("LK", 4, "U.1", "401 10", "/1", "/0", "")
                + element("LK", CircuitTypCore.LK_S.getTypeNumber(), "S.1", switchParameters, "/1", "/2", "")
                + element("LK", 1, "R.1", "1.0", "/2", "/0", "");
    }

    private static String probes() {
        return block(1, "VOLT.1", 40, 0, "", "/2/0", "", "/vload") + block(5, "scope", 41, 0, "", "/vload", "");
    }

    /** The command is produced on the fast sheet and read by a gate on the root sheet. */
    private static CircuitModel grouped() throws Exception {
        return parse(electrical() + sheet(FAST, 0, "fast")
                + block(58, "time", 1, FAST, "", "", "/clock")
                + block(7, "gain", 2, FAST, "1000000", "/clock", "/fastcmd")
                + block(78, "out", 3, FAST, "", "/fastcmd", "/cmd")
                + block(6, "GATE.1", 4, 0, "", "/S.1", "/cmd", "") + probes());
    }

    /** The same blocks on the root sheet: every block runs on the global clock, as in #61. */
    private static CircuitModel flat() throws Exception {
        return parse(electrical()
                + block(58, "time", 1, 0, "", "", "/clock")
                + block(7, "gain", 2, 0, "1000000", "/clock", "/cmd")
                + block(6, "GATE.1", 4, 0, "", "/S.1", "/cmd", "") + probes());
    }

    private static PreparedCircuit prepare(CircuitModel model, Map<String, Double> periods) {
        return prepare(model, GLOBAL, periods);
    }

    private static PreparedCircuit prepare(CircuitModel model, double global, Map<String, Double> periods) {
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), new PreparationRequest(
                new SolverOverrides(null, STEP, 2e-4, new SolverFeatures(global, false, false, null, null, null, periods)),
                Map.of()));
        return assertInstanceOf(PreparationResult.Ready.class, result, result.report().toString()).execution();
    }

    /** What a runtime does to the switch of the fixture netlist: applied resistance, and the parameter a sample publishes. */
    private record Trace(double[] resistance, double[] published) { }

    private static Trace trace(CircuitModel model, Map<String, Double> periods, int steps) {
        return trace(model, GLOBAL, periods, steps);
    }

    private static Trace trace(CircuitModel model, double global, Map<String, Double> periods, int steps) {
        var run = prepare(model, global, periods).newRunModel();
        var netlist = ControlSubsystemRateTest.gateSwitch();
        double[] resistance = new double[steps];
        double[] published = new double[steps];
        try (var runtime = ControlRuntime.fromDeferred(run, STEP, null, global, null, periods)) {
            runtime.initializeAtSimulationStart();
            for (int step = 0; step < steps; step++) {
                // The session order: gates before the electrical solve, CONTROL after it.
                runtime.applyPendingGates(netlist);
                resistance[step] = netlist.getParameter(0)[0];
                runtime.advanceAfterElectricalStep(netlist, step * STEP);
                published[step] = netlist.getParameter(0)[8];
            }
        }
        return new Trace(resistance, published);
    }

    private static void assertSwitchFollowsTheGlobalClock(Trace trace, String what) {
        for (int step = 0; step < trace.resistance.length; step++) {
            // The sample at step 100 publishes. The first solve that applies it is step 101.
            assertEquals(step >= 101 ? R_ON : R_OFF, trace.resistance[step], 0, what + ": applied resistance at step " + step);
            if (step < 100) assertEquals(0, trace.published[step], what + ": nothing published at step " + step);
            else assertTrue(trace.published[step] > 0.5, what + ": published gate at step " + step + " = " + trace.published[step]);
        }
    }

    @Test
    void aDefaultGroupGateAppliesAndPublishesOnlyAtItsOwnSampleWhileItsProducerRunsFaster() throws Exception {
        // Positive control: no subsystem period, every block on the global clock.
        assertSwitchFollowsTheGlobalClock(trace(flat(), Map.of(), 130), "global clock only");
        // The producer samples every 10 us. The gate belongs to the default group and samples every 100 us.
        assertSwitchFollowsTheGlobalClock(trace(grouped(), FAST_PERIOD, 130), "fast producer, default-group gate");
    }

    private static void assertLoadVoltage(float[] load, String what) {
        for (int step = 0; step < load.length; step++) {
            if (step <= 100) assertTrue(Math.abs(load[step]) < 1e-3, what + ": switch is off at step " + step + ", load " + load[step]);
            else assertTrue(load[step] > 9, what + ": switch is on at step " + step + ", load " + load[step]);
        }
        assertTrue(Math.abs(load[50]) < 1e-3 && Math.abs(load[99]) < 1e-3 && Math.abs(load[100]) < 1e-3);
        assertTrue(load[101] > 9, "the first solve after the sample sees the switch on");
    }

    @Test
    void theSolvedLoadVoltageShowsTheSwitchOnlyAfterTheDefaultGroupSampleThroughPreparationAndSession() throws Exception {
        // Fixed order: the positive control runs first, so a failure of the grouped case never hides it.
        assertLoadVoltageOfRun(flat(), Map.of(), "global clock only");
        assertLoadVoltageOfRun(grouped(), FAST_PERIOD, "fast producer, default-group gate");
    }

    private static void assertLoadVoltageOfRun(CircuitModel model, Map<String, Double> periods, String what) {
        try (var session = SimulationSession.open(prepare(model, periods), ExecutionOptions.defaults())) {
            session.run();
            assertLoadVoltage(row(session, "vload"), what);
        }
    }

    @Test
    void aDefaultGroupGateWithPeriodZeroStillFollowsItsFasterProducerAtOnceWhenSubsystemsAreConfigured() throws Exception {
        // The default group runs every step. Its gate keeps the live read: the 10 us sample at step 10 reaches the
        // switch in the next solve, which is step 11, and the gate publishes at every step.
        var trace = trace(grouped(), 0, FAST_PERIOD, 40);
        for (int step = 0; step < 40; step++) {
            assertEquals(step >= 11 ? R_ON : R_OFF, trace.resistance[step], 0, "applied resistance at step " + step);
            if (step < 10) assertEquals(0, trace.published[step], "nothing published at step " + step);
            else assertTrue(trace.published[step] > 0.5, "published gate at step " + step + " = " + trace.published[step]);
        }
        try (var session = SimulationSession.open(prepare(grouped(), 0, FAST_PERIOD), ExecutionOptions.defaults())) {
            session.run();
            float[] load = row(session, "vload");
            for (int step = 0; step < 40; step++) {
                if (step <= 10) assertTrue(Math.abs(load[step]) < 1e-3, "switch is off at step " + step + ", load " + load[step]);
                else assertTrue(load[step] > 9, "switch is on at step " + step + ", load " + load[step]);
            }
        }
    }
}
