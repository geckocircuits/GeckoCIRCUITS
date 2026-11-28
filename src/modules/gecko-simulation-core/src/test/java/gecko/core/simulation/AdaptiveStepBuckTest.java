package gecko.core.simulation;

import gecko.core.allg.AdaptiveStepSettings;
import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.io.CircuitFileParser;
import gecko.core.preparation.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The buck_simple converter with adaptive electrical sub-steps on its fixed grid. */
class AdaptiveStepBuckTest {
    static final double GRID = 1e-6;
    static final double DURATION = 6e-3;
    static final int OUTPUT = 2;

    static PreparedCircuit prepare(SolverFeatures features, double duration) throws Exception {
        var resource = Objects.requireNonNull(AdaptiveStepBuckTest.class.getResource("/ipes/buck_simple.ipes"));
        var model = new CircuitFileParser().parse(Path.of(resource.toURI()).toString());
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), new PreparationRequest(
                new SolverOverrides(SolverType.SOLVER_BE, GRID, duration, features), Map.of()));
        return assertInstanceOf(PreparationResult.Ready.class, result).execution();
    }

    static double average(float[] values, int from, int to) {
        double sum = 0;
        for (int index = from; index < to; index++) sum += values[index];
        return sum / (to - from);
    }

    /** One grid interval of an adaptive run. */
    record Interval(double time, int subSteps, double shortest, boolean gateEdge) { }

    record Run(float[] output, List<Interval> intervals, AdaptiveStepReport report, double seconds) { }

    static Run run(SolverFeatures features, double duration) throws Exception {
        try (var session = SimulationSession.open(prepare(features, duration), ExecutionOptions.defaults())) {
            var intervals = new ArrayList<Interval>();
            int[] count = {0};
            double[] shortest = {Double.POSITIVE_INFINITY};
            session.setSubStepListenerForTest((start, width, error, forced, be) -> {
                count[0]++;
                shortest[0] = Math.min(shortest[0], width);
            });
            Boolean[] previousGate = {null};
            long begin = System.nanoTime();
            while (session.status().state() != SimulationSession.State.COMPLETED) {
                count[0] = 0;
                shortest[0] = Double.POSITIVE_INFINITY;
                boolean[] edge = {false};
                var sample = session.step((time, volts, currents, circuit) -> {
                    int device = -1;
                    for (int element = 0; element < circuit.getElementCount() && device < 0; element++) {
                        CircuitTypCore type = circuit.getType(element);
                        if (type == CircuitTypCore.LK_S || type == CircuitTypCore.LK_MOSFET || type == CircuitTypCore.LK_IGBT) device = element;
                    }
                    if (device < 0) return;
                    double[] parameters = circuit.getParameter(device);
                    boolean gate = switch (circuit.getType(device)) {
                        case LK_S -> parameters[0] == parameters[1];
                        case LK_MOSFET -> parameters[0] == parameters[2];
                        default -> parameters[8] >= 0.5;
                    };
                    edge[0] = previousGate[0] != null && previousGate[0] != gate;
                    previousGate[0] = gate;
                });
                if (sample == null) break;
                intervals.add(new Interval(sample.time(), count[0], shortest[0], edge[0]));
            }
            double seconds = (System.nanoTime() - begin) * 1e-9;
            return new Run(session.observe(0, 10000).values()[OUTPUT], intervals, session.adaptiveStepReport(), seconds);
        }
    }

    static float[] fixed(double duration) throws Exception {
        try (var session = SimulationSession.open(prepare(SolverFeatures.none(), duration), ExecutionOptions.defaults())) {
            session.run();
            return session.observe(0, 10000).values()[OUTPUT];
        }
    }

    @Test
    void theAdaptiveRunAveragesWithinOnePercentOfTheFixedRunAndShortensStepsAtGateEdges() throws Exception {
        long begin = System.nanoTime();
        float[] reference = fixed(DURATION);
        double fixedSeconds = (System.nanoTime() - begin) * 1e-9;
        var run = run(new SolverFeatures(0, false, false, null, new AdaptiveStepSettings(1e-3, 1e-9, GRID)), DURATION);
        assertEquals(reference.length, run.output().length, "recording rows stay on the fixed grid");
        assertEquals(6000, run.intervals().size());
        int last = reference.length - 300;
        double fixedAverage = average(reference, last, reference.length);
        double adaptiveAverage = average(run.output(), last, reference.length);
        var report = run.report();
        long edges = run.intervals().stream().filter(item -> item.time() > 0.3e-3 && item.gateEdge()).count();
        long steady = 0;
        long steadyOne = 0;
        for (int index = 0; index < run.intervals().size(); index++) {
            var item = run.intervals().get(index);
            if (item.time() <= 0.3e-3) continue;
            boolean near = false;
            for (int other = Math.max(0, index - 5); other <= Math.min(run.intervals().size() - 1, index + 5); other++) {
                if (run.intervals().get(other).gateEdge()) near = true;
            }
            if (!near) { steady++; if (item.subSteps() == 1) steadyOne++; }
        }
        int edgesWithShort = 0;
        for (int index = 0; index < run.intervals().size(); index++) {
            var item = run.intervals().get(index);
            if (item.time() <= 0.3e-3 || !item.gateEdge()) continue;
            boolean shorter = false;
            for (int other = index; other <= Math.min(run.intervals().size() - 1, index + 1); other++) {
                if (run.intervals().get(other).shortest() < GRID * (1 - 1e-9)) shorter = true;
            }
            if (shorter) edgesWithShort++;
        }
        System.out.printf("ADAPTIVE-BUCK fixedAvg=%.6f adaptiveAvg=%.6f relDiff=%.3e accepted=%d rejected=%d forced=%d "
                        + "minH=%.3e maxH=%.3e located=%d unlocated=%d gateEdges=%d edgesWithShortStep=%d "
                        + "steadyIntervals=%d steadyOneSubStep=%d adaptiveSeconds=%.3f fixedSeconds=%.3f%n",
                fixedAverage, adaptiveAverage, Math.abs(adaptiveAverage - fixedAverage) / Math.abs(fixedAverage),
                report.accepted(), report.rejected(), report.forced(), report.minStep(), report.maxStep(),
                report.located(), report.unlocated(), edges, edgesWithShort, steady, steadyOne, run.seconds(), fixedSeconds);
        assertEquals(fixedAverage, adaptiveAverage, 0.01 * Math.abs(fixedAverage));
        assertTrue(edges > 20, "the buck switches after 0.3 ms: " + edges);
        assertEquals(edges, edgesWithShort, "every gate edge gets a shorter accepted sub-step within two grid intervals");
        assertTrue(steady > 0 && steadyOne * 2 >= steady, steadyOne + " of " + steady);
    }
}
