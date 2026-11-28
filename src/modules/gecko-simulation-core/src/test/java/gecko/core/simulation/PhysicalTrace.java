package gecko.core.simulation;

import gecko.core.circuit.netlist.INetList;
import gecko.core.testutil.TraceEquivalence;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The complete electrical trace of one run as primitive vectors per step, split into physical unit blocks. */
final class PhysicalTrace {
    final List<Double> times = new ArrayList<>();
    final List<double[]> potentials = new ArrayList<>();
    final List<double[]> sourceCurrents = new ArrayList<>();
    final List<double[]> componentCurrents = new ArrayList<>();
    final List<double[]> switchStates = new ArrayList<>();

    /** Records one step. {@code volts} holds the node potentials and then any source branch currents. */
    void add(double time, double[] volts, double[] currents, INetList netlist) {
        int nodes = netlist.getNodeMax() + 1;
        times.add(time);
        potentials.add(Arrays.copyOfRange(volts, 0, nodes));
        sourceCurrents.add(Arrays.copyOfRange(volts, nodes, volts.length));
        componentCurrents.add(currents.clone());
        switchStates.add(switchStates(netlist));
    }

    /** Conduction resistance of every switching element and limit state of every limited source, exactly as stored. */
    static double[] switchStates(INetList netlist) {
        double[] states = new double[netlist.getElementCount()];
        int count = 0;
        for (int element = 0; element < states.length; element++) {
            double[] parameters = netlist.getParameter(element);
            switch (netlist.getType(element)) {
                case LK_D, LK_THYR, LK_IGBT, LK_S, LK_MOSFET -> states[count++] = parameters[0];
                case LK_U -> { if (parameters.length > 14) states[count++] = parameters[14]; }
                default -> { }
            }
        }
        return Arrays.copyOf(states, count);
    }

    /** @return how many steps changed any switch or limit state */
    int transitions() {
        int transitions = 0;
        for (int i = 1; i < switchStates.size(); i++) {
            if (!Arrays.equals(switchStates.get(i - 1), switchStates.get(i))) transitions++;
        }
        return transitions;
    }

    /**
     * Requires the same grid and step count, the same switch and limit states at every step, and the potentials and
     * the currents each within the tolerance as complete-trace max norms.
     */
    static void assertEquivalent(PhysicalTrace expected, PhysicalTrace actual, String label, double tolerance) {
        assertEquals(expected.times.size(), actual.times.size(), label + " step count");
        assertFalse(expected.times.isEmpty(), label);
        assertEquals(expected.times, actual.times, label + " electrical time grid");
        for (int i = 0; i < expected.times.size(); i++) {
            assertArrayEquals(expected.switchStates.get(i), actual.switchStates.get(i),
                    label + " switch and limit states must agree exactly, step " + i);
        }
        var potentials = TraceEquivalence.norm(expected.potentials, actual.potentials, 0, Integer.MAX_VALUE);
        var sources = TraceEquivalence.norm(expected.sourceCurrents, actual.sourceCurrents, 0, Integer.MAX_VALUE);
        var currents = TraceEquivalence.norm(expected.componentCurrents, actual.componentCurrents, 0, Integer.MAX_VALUE);
        System.out.printf("SPARSE-NORM %s potentials=%.3e (scale %.3e V) sourceCurrents=%.3e (scale %.3e A) "
                + "componentCurrents=%.3e (scale %.3e A, largest difference at %s)%n", label, potentials.relative(),
                potentials.scale(), sources.relative(), sources.scale(), currents.relative(), currents.scale(),
                largestDifference(expected.componentCurrents, actual.componentCurrents));
        TraceEquivalence.assertWithin(label + " node potentials [V]", potentials, tolerance);
        TraceEquivalence.assertWithin(label + " source branch currents [A]", sources, tolerance);
        TraceEquivalence.assertWithin(label + " component currents [A]", currents, tolerance);
    }

    /** @return the step, channel and both values where two traces differ most, for failure diagnosis */
    private static String largestDifference(List<double[]> expected, List<double[]> actual) {
        double worst = -1;
        String where = "none";
        for (int step = 0; step < expected.size(); step++) {
            for (int channel = 0; channel < expected.get(step).length; channel++) {
                double difference = Math.abs(expected.get(step)[channel] - actual.get(step)[channel]);
                if (difference > worst) {
                    worst = difference;
                    where = "step " + step + " channel " + channel + " dense " + expected.get(step)[channel] + " sparse "
                            + actual.get(step)[channel];
                }
            }
        }
        return where;
    }
}
