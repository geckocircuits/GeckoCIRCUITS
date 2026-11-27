package gecko.core.simulation;

import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.ExecutionOptions;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * CONTROL subsystems on their own sample clocks. Each circuit has the sheets "fast" and "slow" under the root and
 * TIME blocks, whose output is the time of their last execution, so a trace shows exactly when a group ran.
 */
class ControlSubsystemRateTest {
    private static final long FAST = 101;
    private static final long SLOW = 202;

    static CircuitModel parse(String blocks) throws Exception {
        return new CircuitFileParser().parse(new BufferedReader(new StringReader(
                "dt 1.0E-6\ntDURATION 3.0E-4\n" + blocks)), "rates.ipes");
    }

    static String sheet(long id, long parent, String name) {
        return "sp\n<ElementSPECIAL>\ntyp 27\nidStringDialog " + name + "\nuniqueObjectIdentifier " + id
                + "\nparentSheetIdentifier " + parent + "\nenabledShorted 1\n<" + (char) 92 + "ElementSPECIAL>\n";
    }

    static String block(int type, String name, long id, long sheet, String parameters, String strings, String inputs,
            String outputs) {
        return "c\n<ElementCONTROL>\ntyp " + type + "\nidStringDialog " + name
                + "\nuniqueObjectIdentifier " + id + "\nparentSheetIdentifier " + sheet
                + "\nparameter[] " + parameters + "\nparameterString[] " + strings + "\nlabelAnfangsKnoten[] " + inputs
                + "\nlabelEndKnoten[] " + outputs + "\n<" + (char) 92 + "ElementCONTROL>\n";
    }

    static String block(int type, String name, long id, long sheet, String parameters, String inputs, String outputs) {
        return block(type, name, id, sheet, parameters, "/", inputs, outputs);
    }

    private static String twoSheets() { return sheet(FAST, 0, "fast") + sheet(SLOW, 0, "slow"); }

    static SolverSettingsCore settings(double dt, double duration, double global, Map<String, Double> periods) {
        return new SolverSettingsCore(SolverType.SOLVER_BE, dt, duration).withControlSamplePeriod(global)
                .withControlSubsystemPeriods(periods);
    }

    /** Prepares the model like a caller would, which also resolves boundary blocks, and opens a session. */
    static SimulationSession open(CircuitModel model, SolverSettingsCore settings) {
        var preparation = new gecko.core.preparation.CircuitPreparation();
        var features = new gecko.core.preparation.SolverFeatures(settings.getControlSamplePeriod(), false, false, null, null,
                null, settings.getControlSubsystemPeriods());
        var result = preparation.prepare(preparation.capture(model), new gecko.core.preparation.PreparationRequest(
                new gecko.core.preparation.SolverOverrides(null, settings.getStepWidth(), settings.getSimulationDuration(),
                        features), Map.of()));
        if (result instanceof gecko.core.preparation.PreparationResult.Ready ready) {
            return SimulationSession.open(ready.execution(), ExecutionOptions.defaults());
        }
        throw new IllegalArgumentException(result.report().toString());
    }

    static float[] row(SimulationSession session, String name) {
        var names = java.util.Arrays.asList(session.signalNames());
        assertTrue(names.contains(name), names.toString());
        return session.observe(0, session.status().committedSteps()).values()[names.indexOf(name)];
    }

    /** The first grid index at or after each multiple of the period, in whole nanoseconds, as a reference clock. */
    private static long[] lastSampleNanos(int steps, long dtNanos, long periodNanos) {
        long[] last = new long[steps];
        long next = 0;
        long held = 0;
        for (int step = 0; step < steps; step++) {
            long now = step * dtNanos;
            if (now >= next * periodNanos) {
                held = now;
                next = now / periodNanos + 1;
            }
            last[step] = held;
        }
        return last;
    }

    private static CircuitModel twoClocks() throws Exception {
        return parse(twoSheets()
                + block(58, "fastTime", 1, FAST, "", "", "/fastclock")
                + block(5, "fastScope", 2, FAST, "", "/fastclock", "")
                + block(58, "slowTime", 3, SLOW, "", "", "/slowclock")
                + block(5, "slowScope", 4, SLOW, "", "/slowclock", ""));
    }

    @Test
    void aFastAndASlowLoopHoldTheirOutputsBetweenTheirOwnSamples() throws Exception {
        var periods = Map.of("fast", 10e-6, "slow", 100e-6);
        try (var session = open(twoClocks(), settings(1e-6, 3e-4, 0, periods))) {
            session.run();
            float[] fast = row(session, "fastclock");
            float[] slow = row(session, "slowclock");
            assertEquals(300, fast.length);
            for (int step = 0; step < fast.length; step++) {
                assertEquals(10e-6 * (step / 10), fast[step], 1e-9, "fast " + step);
                assertEquals(100e-6 * (step / 100), slow[step], 1e-9, "slow " + step);
            }
        }
    }

    @Test
    void longRunsSampleTheExpectedGridStepsWithGlobalAndSubsystemClocks() throws Exception {
        int first = 10_000;
        int count = 2_000;
        var global = parse(block(58, "time", 1, 0, "", "", "/clock")
                + block(5, "scope", 2, 0, "", "/clock", ""));
        try (var grouped = open(twoClocks(), settings(1e-6, 0.012, 0, Map.of("fast", 10e-6, "slow", 100e-6)));
                var single = open(global, settings(1e-6, 0.012, 10e-6, Map.of()))) {
            grouped.run();
            single.run();
            for (var session : List.of(grouped, single)) {
                float[][] rows = session.observe(first, count).values();
                String[] names = session.signalNames();
                for (int row = 0; row < names.length; row++) {
                    long period = names[row].equals("slowclock") ? 100_000 : 10_000;
                    long[] expected = lastSampleNanos(first + count, 1_000, period);
                    assertEquals(count, rows[row].length);
                    for (int index = 0; index < count; index++) {
                        int step = first + index;
                        assertEquals(expected[step] * 1e-9, rows[row][index], 1e-9, names[row] + " step " + step);
                    }
                }
            }
        }
    }

    @Test
    void continuationKeepsThePendingGridPointAndSamplesOnTheNewGrid() throws Exception {
        var model = parse(block(58, "time", 1, 0, "", "", "/clock")
                + block(5, "scope", 2, 0, "", "/clock", ""));
        try (var session = open(model, settings(1e-6, 0.0101545, 10e-6, Map.of()))) {
            session.run();
            int first = session.status().committedSteps();
            assertEquals(10_155, first);
            session.extend(0.01019, 2e-6);
            session.run();
            var observation = session.observe(first, 6);
            assertEquals(10_155e-6, observation.times()[0], 1e-14);
            assertArrayEquals(new float[]{0.01015f, 0.01015f, 0.01015f, 0.010161f, 0.010161f, 0.010161f},
                    observation.values()[0], 1e-9f);
        }
    }

    @Test
    void aBlockOutsideEverySubsystemKeepsTheGlobalPeriod() throws Exception {
        var model = parse(twoSheets() + block(58, "rootTime", 5, 0, "", "", "/rootclock")
                + block(5, "rootScope", 6, 0, "", "/rootclock", "")
                + block(58, "fastTime", 1, FAST, "", "", "/fastclock")
                + block(5, "fastScope", 2, FAST, "", "/fastclock", ""));
        try (var session = open(model, settings(1e-6, 2e-4, 50e-6, Map.of("fast", 10e-6)))) {
            session.run();
            float[] root = row(session, "rootclock");
            float[] fast = row(session, "fastclock");
            for (int step = 0; step < root.length; step++) {
                assertEquals(50e-6 * (step / 50), root[step], 1e-9, "root " + step);
                assertEquals(10e-6 * (step / 10), fast[step], 1e-9, "fast " + step);
            }
        }
    }

    @Test
    void aSimultaneousTickRunsProducersBeforeConsumersAcrossGroups() throws Exception {
        // fast clock -> slow gain -> fast gain, through boundary blocks on the root sheet.
        var model = parse(twoSheets()
                + block(58, "fastTime", 1, FAST, "", "", "/clock")
                + block(78, "fastOut", 2, FAST, "", "/clock", "/fastclock")
                + block(78, "slowIn", 3, SLOW, "", "/inFast", "/fastclock")
                + block(7, "slowGain", 4, SLOW, "1", "/inFast", "/slowvalue")
                + block(5, "slowScope", 5, SLOW, "", "/slowvalue", "")
                + block(78, "slowOut", 6, SLOW, "", "/slowvalue", "/slowout")
                + block(78, "fastIn", 7, FAST, "", "/inSlow", "/slowout")
                + block(7, "fastGain", 8, FAST, "1", "/inSlow", "/fastvalue")
                + block(5, "fastScope", 9, FAST, "", "/fastvalue", ""));
        try (var session = open(model, settings(1e-6, 3e-4, 0, Map.of("fast", 10e-6, "slow", 100e-6)))) {
            session.run();
            float[] slowGain = row(session, "slowvalue");
            float[] fastGain = row(session, "fastvalue");
            for (int step = 0; step < slowGain.length; step++) {
                // At t = 100 us both groups are due and the fast gain sees the slow gain's new sample.
                assertEquals(100e-6 * (step / 100), slowGain[step], 1e-9, "slow gain " + step);
                assertEquals(100e-6 * (step / 100), fastGain[step], 1e-9, "fast gain " + step);
            }
            assertEquals(100e-6, fastGain[100], 1e-9);
            assertEquals(100e-6, fastGain[110], 1e-9, "a faster consumer reads the held slow output");
        }
    }

    @Test
    void aDelayCountsTicksOfItsOwnGroup() throws Exception {
        // A clock that runs every step feeds an 80 us delay of 8 fast ticks and a 400 us delay of 4 slow ticks.
        // The delay times divide exactly, so no fractional delay position blurs the tick count.
        var model = parse(twoSheets() + block(58, "rootTime", 1, 0, "", "", "/rootclock")
                + block(78, "fastIn", 2, FAST, "", "/inclock", "/rootclock")
                + block(25, "fastDelay", 3, FAST, "8.0E-5", "/inclock", "/fastdelayed")
                + block(5, "fastScope", 4, FAST, "", "/fastdelayed", "")
                + block(78, "slowIn", 5, SLOW, "", "/inclock", "/rootclock")
                + block(25, "slowDelay", 6, SLOW, "4.0E-4", "/inclock", "/slowdelayed")
                + block(5, "slowScope", 7, SLOW, "", "/slowdelayed", ""));
        try (var session = open(model, settings(1e-6, 1.2e-3, 0, Map.of("fast", 10e-6, "slow", 100e-6)))) {
            session.run();
            float[] fast = row(session, "fastdelayed");
            float[] slow = row(session, "slowdelayed");
            for (int step = 0; step < fast.length; step++) {
                int fastTick = step / 10;
                int slowTick = step / 100;
                assertEquals(fastTick < 8 ? 0 : 10e-6 * (fastTick - 8), fast[step], 1e-9, "fast " + step);
                assertEquals(slowTick < 4 ? 0 : 100e-6 * (slowTick - 4), slow[step], 1e-9, "slow " + step);
            }
            assertTrue(fast[90] > 0 && fast[89] == 0, "first fast sample after 9 ticks");
            assertTrue(slow[500] > 0 && slow[499] == 0, "first slow sample after 5 ticks");
        }
    }

    @Test
    void aNonIntegerRatioSamplesTheFirstStepAtOrAfterEachMultipleWithoutDrift() throws Exception {
        var periods = Map.of("fast", 10e-6, "slow", 25e-6);
        int steps = 8400;
        try (var session = open(twoClocks(), settings(3e-6, 3e-6 * (steps - 1) + 1e-9, 0, periods))) {
            session.run();
            float[] fast = row(session, "fastclock");
            float[] slow = row(session, "slowclock");
            assertEquals(steps, fast.length);
            long[] fastReference = lastSampleNanos(steps, 3000, 10000);
            long[] slowReference = lastSampleNanos(steps, 3000, 25000);
            for (int step = 0; step < steps; step++) {
                assertEquals(fastReference[step] * 1e-9, fast[step], 1e-8, "fast " + step);
                assertEquals(slowReference[step] * 1e-9, slow[step], 1e-8, "slow " + step);
            }
            assertTrue(slowReference[steps - 1] > 0.024, "more than 1000 slow samples");
        }
    }

    @Test
    void aContinuationOnTheSameGridEqualsAnUninterruptedRun() throws Exception {
        var periods = Map.of("fast", 10e-6, "slow", 100e-6);
        var settings = settings(1e-6, 2e-4, 0, periods);
        float[] fast;
        float[] slow;
        try (var whole = open(twoClocks(), settings)) {
            whole.run();
            whole.extend(4e-4);
            whole.run();
            fast = row(whole, "fastclock");
            slow = row(whole, "slowclock");
        }
        try (var direct = open(twoClocks(), settings(1e-6, 4e-4, 0, periods))) {
            direct.run();
            assertArrayEquals(row(direct, "fastclock"), fast);
            assertArrayEquals(row(direct, "slowclock"), slow);
        }
        for (int split : new int[]{1, 7, 100}) {
            try (var session = open(twoClocks(), settings(1e-6, 4e-4, 0, periods))) {
                for (int index = 0; index < split; index++) session.step();
                session.run();
                assertArrayEquals(fast, row(session, "fastclock"), "split at " + split);
                assertArrayEquals(slow, row(session, "slowclock"), "split at " + split);
            }
        }
    }

    @Test
    void aFinerGridKeepsEachGroupOnItsAbsoluteClockAndALongerOneRestagesOnlyTheFastGroup() throws Exception {
        var model = parse(twoSheets() + block(58, "rootTime", 1, 0, "", "", "/rootclock")
                + block(78, "fastIn", 2, FAST, "", "/inclock", "/rootclock")
                + block(25, "fastDelay", 3, FAST, "8.0E-5", "/inclock", "/fastdelayed")
                + block(5, "fastScope", 4, FAST, "", "/fastdelayed", "")
                + block(58, "fastTime", 8, FAST, "", "", "/fastclock")
                + block(5, "fastClockScope", 9, FAST, "", "/fastclock", "")
                + block(78, "slowIn", 5, SLOW, "", "/inclock", "/rootclock")
                + block(25, "slowDelay", 6, SLOW, "4.0E-4", "/inclock", "/slowdelayed")
                + block(5, "slowScope", 7, SLOW, "", "/slowdelayed", ""));
        var periods = Map.of("fast", 10e-6, "slow", 100e-6);
        try (var session = open(model, settings(4e-6, 6.0e-4, 0, periods))) {
            session.run();
            session.extend(1.2e-3, 2e-6);
            session.run();
            session.extend(2.4e-3, 20e-6);
            session.run();
            var fastClock = row(session, "fastclock");
            var slowDelayed = row(session, "slowdelayed");
            var fastDelayed = row(session, "fastdelayed");
            double[] times = session.observe(0, session.status().committedSteps()).times();
            long nextFast = 0;
            long nextSlow = 0;
            long slowTicks = 0;
            var slowSamples = new ArrayList<Double>();
            for (int index = 0; index < times.length; index++) {
                long nanos = Math.round(times[index] * 1e9);
                boolean fastDue = nanos >= nextFast * 10000 || times[index] > 1.2e-3;
                if (nanos >= nextFast * 10000) nextFast = nanos / 10000 + 1;
                if (nanos >= nextSlow * 100000) {
                    slowSamples.add(nanos * 1e-9);
                    nextSlow = nanos / 100000 + 1;
                }
                if (fastDue) assertEquals(nanos * 1e-9, fastClock[index], 1e-8, "fast runs at " + nanos);
                else assertNotEquals(nanos * 1e-9, fastClock[index], "fast holds at " + nanos);
                int tick = slowSamples.size() - 1;
                assertEquals(tick < 4 ? 0 : slowSamples.get(tick - 4), slowDelayed[index], 1e-8,
                        "the slow delay keeps counting slow ticks across both step changes at " + nanos);
            }
            // The slow group step never changed, so its delay was not resampled. The fast delay was, to 20 us ticks,
            // and it keeps the 80 us transport delay of the linear clock.
            int last = times.length - 1;
            assertEquals(times[last] - 80e-6, fastDelayed[last], 1e-7);
            assertTrue(slowSamples.size() > 20);
        }
    }

    @Test
    void anElectricalStepLongerThanAGroupPeriodRunsThatGroupEveryStep() throws Exception {
        try (var session = open(twoClocks(), settings(20e-6, 1e-3, 0, Map.of("fast", 10e-6, "slow", 100e-6)))) {
            session.run();
            float[] fast = row(session, "fastclock");
            float[] slow = row(session, "slowclock");
            for (int step = 0; step < fast.length; step++) {
                assertEquals(20e-6 * step, fast[step], 1e-9, "fast " + step);
                assertEquals(100e-6 * (step / 5), slow[step], 1e-9, "slow " + step);
            }
        }
    }

    static CircuitNetlist gateSwitch() {
        var netlist = new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_S}, new int[]{1}, new int[]{0}, new int[]{-1},
                new double[][]{new double[]{1e9, 0.01, 1e9, 0, 0, 0, 0, 0, 0}}, 2, 0, 1);
        netlist.setComponentNameToIndex(Map.of("S.1", 0));
        return netlist;
    }

    @Test
    void aGateInASlowSubsystemAppliesAHeldValueEvenWhenItsProducerChangesEveryStep() throws Exception {
        var model = parse(sheet(SLOW, 0, "slow")
                + block(1, "VOLT.1", 1, SLOW, "", "/1/0", "", "/live")
                + block(6, "GATE.1", 3, SLOW, "", "/S.1", "/live", ""));
        var netlist = gateSwitch();
        var resistance = new ArrayList<Double>();
        var published = new ArrayList<Double>();
        try (var runtime = ControlRuntime.fromDeferred(model, 1e-6, null, 0, null, Map.of("slow", 3e-6))) {
            for (int step = 0; step < 8; step++) {
                runtime.applyPendingGates(netlist);
                resistance.add(netlist.getParameter(0)[0]);
                netlist.storeResults(new double[]{0, step % 2 == 0 ? 1 : 0}, new double[]{0});
                runtime.advanceAfterElectricalStep(netlist, step * 1e-6);
                published.add(netlist.getParameter(0)[8]);
            }
        }
        // The probe reads 1, 0, 1, 0 ... The slow group samples at steps 0, 3 and 6 and sees 1, 0 and 1.
        assertEquals(List.of(1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 1.0), published);
        assertEquals(List.of(1e9, 0.01, 0.01, 0.01, 1e9, 1e9, 1e9, 0.01), resistance);
    }

    @Test
    void aSourceKeepsItsHeldValueWhileItsProducersGroupDoesNotPublish() {
        var producer = new gecko.core.control.calculators.TimeCalculator();
        producer._outputSignal[0][0] = 7;
        var mapping = new ControlBlockMapping();
        mapping.addSourceMapping(producer, 0, "U.2");
        var coupler = new DomainCoupler();
        coupler.configureFromMapping(mapping);
        var netlist = new CircuitNetlist();
        double[] parameters = new double[21];
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U}, new int[]{1}, new int[]{0}, new int[]{1},
                new double[][]{parameters}, 1, 1, 1);
        netlist.setComponentNameToIndex(Map.of("U.2", 0));
        coupler.transferControlToSources(netlist, calculator -> false);
        assertEquals(0, netlist.getParameter(0)[1]);
        coupler.transferControlToSources(netlist, calculator -> calculator == producer);
        assertEquals(7, netlist.getParameter(0)[1]);
        producer._outputSignal[0][0] = 9;
        coupler.transferControlToSources(netlist, calculator -> false);
        assertEquals(7, netlist.getParameter(0)[1], "held");
    }

    @Test
    void aSubsystemPeriodOfZeroRunsThatGroupEveryStep() throws Exception {
        try (var session = open(twoClocks(), settings(1e-6, 2e-4, 100e-6, Map.of("fast", 0.0, "slow", 50e-6)))) {
            session.run();
            float[] fast = row(session, "fastclock");
            float[] slow = row(session, "slowclock");
            for (int step = 0; step < fast.length; step++) {
                assertEquals(1e-6 * step, fast[step], 1e-9, "fast " + step);
                assertEquals(50e-6 * (step / 50), slow[step], 1e-9, "slow " + step);
            }
        }
    }

    @Test
    void anInvalidPathFailsWhenASessionOpens() throws Exception {
        var failure = assertThrows(IllegalArgumentException.class,
                () -> open(twoClocks(), settings(1e-6, 1e-4, 0, Map.of("missing", 10e-6))));
        assertTrue(failure.getMessage().contains("controlSubsystemPeriods.missing"), failure.getMessage());
    }

    private static int exportedValues(Map<String, Double> periods, double global) throws Exception {
        var monitor = new PeriodicStateMonitor(1e-3, 3, 1);
        try (var runtime = ControlRuntime.fromDeferred(twoClocks(), 1e-6, null, global, null, periods)) {
            runtime.advanceAfterElectricalStep(new CircuitNetlist(), 0);
            monitor.begin();
            runtime.exportPeriodicState(new CircuitNetlist(), 0, monitor.sink());
            monitor.commit(1, 0);
        }
        var size = PeriodicStateMonitor.class.getDeclaredField("size");
        size.setAccessible(true);
        return size.getInt(monitor);
    }

    @Test
    void aGroupedPeriodicStateExportsOnePhaseAndFlagPerSampledGroup() throws Exception {
        assertEquals(0, exportedValues(Map.of("fast", 0.0), 0), "no sampled group exports no phase");
        assertEquals(2, exportedValues(Map.of("fast", 10e-6), 0), "one sampled group");
        assertEquals(4, exportedValues(Map.of("fast", 10e-6, "slow", 100e-6), 0));
        assertEquals(6, exportedValues(Map.of("fast", 10e-6, "slow", 100e-6), 50e-6), "the global group too");
    }

    private static CircuitModel buckWithTwoClocks() throws Exception {
        var path = java.nio.file.Path.of(java.util.Objects.requireNonNull(
                ControlSubsystemRateTest.class.getResource("/ipes/buck_simple.ipes")).toURI()).toString();
        var buck = new CircuitFileParser().parse(path);
        var extra = parse(sheet(9101, 0, "fast") + sheet(9202, 0, "slow")
                + block(58, "fastTime", 9001, 9101, "", "", "/fastclock")
                + block(5, "fastScope", 9002, 9101, "", "/fastclock", "")
                + block(58, "slowTime", 9003, 9202, "", "", "/slowclock")
                + block(5, "slowScope", 9004, 9202, "", "/slowclock", ""));
        buck.getSpecialComponents().addAll(extra.getSpecialComponents());
        buck.getControlComponents().addAll(extra.getControlComponents());
        return buck;
    }

    @Test
    void adaptiveStepsAndEventLocationLeaveEveryGroupOnTheGridClock() throws Exception {
        var periods = Map.of("fast", 5e-6, "slow", 20e-6);
        float[][] rows = new float[2][];
        float[][] slows = new float[2][];
        for (int run = 0; run < 2; run++) {
            var adaptive = run == 1 ? new gecko.core.allg.AdaptiveStepSettings(1e-3, 1e-9, Double.NaN) : null;
            var features = new gecko.core.preparation.SolverFeatures(0, false, false, null, adaptive, null, periods);
            var preparation = new gecko.core.preparation.CircuitPreparation();
            var result = preparation.prepare(preparation.capture(buckWithTwoClocks()),
                    new gecko.core.preparation.PreparationRequest(new gecko.core.preparation.SolverOverrides(null, null, 1e-4,
                            features), Map.of()));
            var ready = assertInstanceOf(gecko.core.preparation.PreparationResult.Ready.class, result, result.report().toString());
            try (var session = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
                session.run();
                rows[run] = row(session, "fastclock");
                slows[run] = row(session, "slowclock");
                if (run == 1) assertTrue(session.adaptiveStepReport().located() > 0, session.adaptiveStepReport().toString());
            }
        }
        assertArrayEquals(rows[0], rows[1]);
        assertArrayEquals(slows[0], slows[1]);
        for (int step = 0; step < rows[1].length; step++) {
            assertEquals(5e-6 * (step / 5), rows[1][step], 1e-9, "fast " + step);
            assertEquals(20e-6 * (step / 20), slows[1][step], 1e-9, "slow " + step);
        }
    }
}
