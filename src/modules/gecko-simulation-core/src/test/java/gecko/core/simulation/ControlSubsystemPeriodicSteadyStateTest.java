package gecko.core.simulation;

import static gecko.core.simulation.ControlSubsystemRateTest.block;
import static gecko.core.simulation.ControlSubsystemRateTest.parse;
import static gecko.core.simulation.ControlSubsystemRateTest.row;
import static gecko.core.simulation.ControlSubsystemRateTest.sheet;
import static org.junit.jupiter.api.Assertions.*;

import gecko.core.allg.PeriodicSteadyStateSettings;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Periodic steady-state search on a decaying circuit that two CONTROL subsystems drive on their own clocks.
 * A 10 kHz sine on a 10 us clock drives one RC branch. A 5 kHz sine on a 100 us clock drives another.
 * The period of 200 us holds two and one sine cycles, 20 fast ticks and 2 slow ticks.
 */
class ControlSubsystemPeriodicSteadyStateTest {
    private static final long FAST = 101;
    private static final long SLOW = 202;
    private static final Map<String, Double> PERIODS = Map.of("fast", 10e-6, "slow", 100e-6);
    private static final int STEPS_PER_PERIOD = 200;

    private static String lk(int type, String name, long id, String parameters, String strings, String from, String to) {
        return "e\n<ElementLK>\ntyp " + type + "\nidStringDialog " + name + "\nuniqueObjectIdentifier " + id
                + "\nx 1\ny 1\norientierung 503\nparentSheetIdentifier 0\nenabledShorted 1\nparameter[] " + parameters
                + "\n" + (strings.isEmpty() ? "" : "parameterString[] " + strings + "\n")
                + "labelAnfangsKnoten[] /" + from + "\nlabelEndKnoten[] /" + to + "\n<" + (char) 92 + "ElementLK>\n";
    }

    private static String branch(String name, long id, String signal, String drive, String mid) {
        String capacitor = "2.0E-8 0.0 0.0 0.0 0.0 0.0 2.0E-8 2.0E-8 0.0 0.0 0.0";
        return lk(4, "U" + name, id, "400.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0", "/" + signal + "/NIX_NIX_NIX/0", drive, "0")
                + lk(1, "R" + name, id + 1, "1000.0 0.0 0.0 0.0", "", drive, mid)
                + lk(3, "C" + name, id + 2, capacitor, "", mid, "0");
    }

    private static CircuitModel model() throws Exception {
        return parse(sheet(FAST, 0, "fast") + sheet(SLOW, 0, "slow")
                + branch("f", 1001, "vf", "1", "2") + branch("s", 1011, "vs", "3", "4")
                + block(4, "fastSine", 1, FAST, "400 5 10000 0.3 0 0.5 0", "", "/fastsig")
                + block(78, "fastOut", 2, FAST, "", "/fastsig", "/vf")
                + block(4, "slowSine", 3, SLOW, "400 2 5000 0.9 0 0.5 0", "", "/slowsig")
                + block(78, "slowOut", 4, SLOW, "", "/slowsig", "/vs")
                + block(1, "probeF", 30, 0, "", "/2/0", "", "/pf") + block(1, "probeS", 31, 0, "", "/4/0", "", "/ps")
                + block(5, "rootScope", 32, 0, "", "/pf/ps", ""));
    }

    private static PreparationResult prepare(PeriodicSteadyStateSettings search, Map<String, Double> periods, double duration)
            throws Exception {
        var features = new SolverFeatures(0, false, false, search, null, null, periods);
        var preparation = new CircuitPreparation();
        return preparation.prepare(preparation.capture(model()), new PreparationRequest(
                new SolverOverrides(null, 1e-6, duration, features), Map.of()));
    }

    @Test
    void aDecayingCircuitDrivenByTwoClocksConvergesAndTheMainPhaseContinuesTheTransient() throws Exception {
        var search = new PeriodicSteadyStateSettings(200e-6, 1e-4, 200);
        var ready = assertInstanceOf(PreparationResult.Ready.class, prepare(search, PERIODS, 4e-4));
        float[] fast;
        float[] slow;
        double[] times;
        SteadyStateReport report;
        try (var session = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
            session.run();
            report = session.steadyStateReport();
            assertTrue(report.converged(), report.toString());
            assertTrue(report.periods() < 200, report.toString());
            fast = row(session, "pf");
            slow = row(session, "ps");
            times = session.observe(0, session.status().committedSteps()).times();
        }
        assertEquals(2 * STEPS_PER_PERIOD, fast.length);
        assertEquals(report.startTime() + 1e-6, times[0], 1e-12, "the main phase continues the absolute clock");
        assertEquals(0, Math.round(times[0] * 1e6) % STEPS_PER_PERIOD, "the main phase starts on a period boundary");
        double fastPeak = 0;
        double slowPeak = 0;
        for (int index = 0; index < fast.length; index++) {
            fastPeak = Math.max(fastPeak, Math.abs(fast[index]));
            slowPeak = Math.max(slowPeak, Math.abs(slow[index]));
        }
        assertTrue(fastPeak > 1 && slowPeak > 0.5, "the branches carry a signal: " + fastPeak + " " + slowPeak);
        for (int index = 0; index + STEPS_PER_PERIOD < fast.length; index++) {
            assertEquals(fast[index], fast[index + STEPS_PER_PERIOD], 1e-3 * fastPeak, "fast repeats " + index);
            assertEquals(slow[index], slow[index + STEPS_PER_PERIOD], 1e-3 * slowPeak, "slow repeats " + index);
        }
        // A long plain transient from rest reaches the same periodic response at the same absolute times.
        var plain = assertInstanceOf(PreparationResult.Ready.class, prepare(null, PERIODS, 3e-3));
        try (var session = SimulationSession.open(plain.execution(), ExecutionOptions.defaults())) {
            session.run();
            float[] plainFast = row(session, "pf");
            float[] plainSlow = row(session, "ps");
            int origin = (int) Math.round(times[0] * 1e6) % STEPS_PER_PERIOD + 10 * STEPS_PER_PERIOD;
            for (int index = 0; index < STEPS_PER_PERIOD; index++) {
                assertEquals(plainFast[origin + index], fast[index], 2e-3 * fastPeak, "fast against the plain run " + index);
                assertEquals(plainSlow[origin + index], slow[index], 2e-3 * slowPeak, "slow against the plain run " + index);
            }
        }
    }

    @Test
    void aPeriodThatIsNotAWholeMultipleOfEveryClockIsRejected() throws Exception {
        var search = new PeriodicSteadyStateSettings(200e-6, 1e-4, 60);
        var rejected = assertInstanceOf(PreparationResult.Rejected.class,
                prepare(search, Map.of("fast", 10e-6, "slow", 70e-6), 6e-4));
        assertTrue(rejected.report().diagnostics().stream().anyMatch(item -> item.code().equals("INVALID_SETTING")
                && "periodicSteadyState.period".equals(item.parameterPath())), rejected.report().toString());
    }
}
