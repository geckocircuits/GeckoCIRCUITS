package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import gecko.core.circuit.SourceType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static gecko.core.circuit.circuitcomponents.CircuitTypCore.*;
import static org.junit.jupiter.api.Assertions.*;

/** The DC operating point on hand-built netlists: states, orientation, errors and immutability. */
class DcOperatingPointSolverTest {
    private static double[] dc(double value) {
        double[] values = new double[21];
        values[0] = SourceType.QUELLE_DC_NEW;
        values[1] = value;
        return values;
    }

    private static double[] padded(double... values) { return Arrays.copyOf(values, 13); }

    private static double[] resistor(double ohms) { return new double[]{ohms, 0, 0}; }

    private static double[] reactive(double value) { return new double[]{value, 0, 0, 0, 0, 0, value, value, 0, 0, 0}; }

    /** Diode-family device: current resistance, forward voltage, on and off resistance, gate at slot 8. */
    private static double[] diode(double current, double forward, double gate) {
        double[] values = new double[13];
        values[0] = current;
        values[1] = forward;
        values[2] = 0.01;
        values[3] = 1e9;
        values[8] = gate;
        return values;
    }

    private record Case(CircuitNetlist netlist, MatrixSolver matrix, int nodes, int sources) { }

    private static Case build(SolverType method, CircuitTypCore[] types, int[] x, int[] y, int[] aux,
            double[][] parameters, int nodes, int sources) {
        var netlist = new CircuitNetlist();
        netlist.initNetlist(types, x, y, aux, parameters, nodes, sources, types.length);
        netlist.setWireReference(true);
        var matrix = new MatrixSolver(method);
        matrix.initializeMatrices(nodes, sources, types.length);
        return new Case(netlist, matrix, nodes, sources);
    }

    private static void solve(SolverType method, Case circuit) {
        new InitialConditionSolver(method).setDcOperatingPoint(circuit.matrix(), circuit.netlist());
    }

    /** Source 5 V into R=10 then one device to ground. */
    private static Case deviceCircuit(SolverType method, CircuitTypCore type, double volts, double[] device) {
        return build(method, new CircuitTypCore[]{LK_U, LK_R, type}, new int[]{1, 1, 2}, new int[]{0, 2, 0},
                new int[]{1, -1, -1}, new double[][]{dc(volts), resistor(10), device}, 2, 1);
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void aDiodeTakesTheStateItsBiasDemandsWhateverItsSavedResistance(SolverType method) {
        for (double saved : new double[]{0.01, 1e9}) {
            var forward = deviceCircuit(method, LK_D, 5, diode(saved, 0.7, 0));
            solve(method, forward);
            assertEquals((5 - 0.7) / 10.01, forward.matrix().getIALT()[2], 1e-9, "saved " + saved);
            var reverse = deviceCircuit(method, LK_D, -5, diode(saved, 0.7, 0));
            solve(method, reverse);
            // The legacy diode current convention keeps the forward voltage in the blocking current.
            double blocked = -5 * 1e9 / (10 + 1e9);
            assertEquals((blocked - 0.7) / 1e9, reverse.matrix().getIALT()[2], 1e-15, "saved " + saved);
            assertEquals(saved, forward.netlist().getParameter(2)[0], "the netlist state is untouched");
        }
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void controlledDevicesNeedTheirGateToConduct(SolverType method) {
        for (var type : new CircuitTypCore[]{LK_THYR, LK_IGBT}) {
            var gated = deviceCircuit(method, type, 5, diode(1e9, 0.7, 1));
            solve(method, gated);
            assertEquals((5 - 0.7) / 10.01, gated.matrix().getIALT()[2], 1e-9, type + " gated");
            var open = deviceCircuit(method, type, 5, diode(0.01, 0.7, 0));
            double[] stored = open.netlist().getParameter(2).clone();
            solve(method, open);
            // A stored conducting thyristor stays latched without a gate. An IGBT without a gate blocks.
            double expected = type == LK_THYR ? (5 - 0.7) / 10.01 : 5.0 / (10 + 1e9);
            assertEquals(expected, open.matrix().getIALT()[2], 1e-9, type + " stored on, gate off");
            assertArrayEquals(stored, open.netlist().getParameter(2), "the caller's device state is untouched");
            var reversed = deviceCircuit(method, type, -5, diode(0.01, 0.7, 0));
            solve(method, reversed);
            assertEquals((-5 * 1e9 / (10 + 1e9)) / 1e9, reversed.matrix().getIALT()[2], 1e-15,
                    type + " stored on, reverse biased, unlatches");
            var cold = deviceCircuit(method, type, 5, diode(1e9, 0.7, 0));
            solve(method, cold);
            assertEquals(5.0 / (10 + 1e9), cold.matrix().getIALT()[2], 1e-12, type + " blocking, gate off");
        }
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void aSwitchKeepsItsInitialGateState(SolverType method) {
        var closed = deviceCircuit(method, LK_S, 5, padded(0.5, 0.5, 1e6));
        solve(method, closed);
        assertEquals(5 / 10.5, closed.matrix().getIALT()[2], 1e-9);
        var opened = deviceCircuit(method, LK_S, 5, padded(1e6, 0.5, 1e6));
        solve(method, opened);
        assertEquals(5 / (10 + 1e6), opened.matrix().getIALT()[2], 1e-12);
        var mosfet = deviceCircuit(method, LK_MOSFET, 5, padded(0.5, 0, 0.5, 1e6));
        solve(method, mosfet);
        assertEquals(5 / 10.5, mosfet.matrix().getIALT()[2], 1e-9);
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void everyHistoryReceivesTheSolvedStateAndTheCircuitIsUnchanged(SolverType method) {
        var circuit = build(method, new CircuitTypCore[]{LK_U, LK_R, LK_L, LK_C},
                new int[]{1, 1, 2, 2}, new int[]{0, 2, 0, 0}, new int[]{1, -1, -1, -1},
                new double[][]{dc(10), resistor(2), reactive(0.5), reactive(1)}, 2, 1);
        double[][] before = new double[4][];
        for (int i = 0; i < 4; i++) before[i] = circuit.netlist().getParameter(i).clone();
        solve(method, circuit);
        var matrix = circuit.matrix();
        for (double[] history : new double[][]{matrix.getP(), matrix.getPALT(), matrix.getPALTALT(),
                matrix.getPALTALTALT()}) {
            assertEquals(10, history[1], 1e-9);
            assertEquals(0, history[2], 1e-9, "the short pulls the junction to ground");
            assertEquals(-5, history[3], 1e-9, "the source current sits in its auxiliary slot");
        }
        for (double[] history : new double[][]{matrix.getIALT(), matrix.getIALTALT(), matrix.getIALTALTALT()}) {
            assertArrayEquals(new double[]{-5, 5, 5, 0}, history, 1e-9);
        }
        for (int i = 0; i < 4; i++) assertArrayEquals(before[i], circuit.netlist().getParameter(i), "element " + i);
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void coupledWindingsAreShortsWithTheirOwnBranchCurrents(SolverType method) {
        double first = 1e-3, second = 2e-3;
        var circuit = build(method, new CircuitTypCore[]{LK_U, LK_R, LK_LKOP2, LK_U, LK_R, LK_LKOP2},
                new int[]{1, 1, 2, 4, 4, 3}, new int[]{0, 2, 0, 0, 3, 0}, new int[]{1, -1, 2, 4, -1, 3},
                new double[][]{dc(4), resistor(2), reactive(first), dc(3), resistor(1.5), reactive(second)}, 4, 4);
        circuit.netlist().registerMutualCoupling(2, 5, 0.5, first, second);
        solve(method, circuit);
        assertEquals(2, circuit.matrix().getIALT()[2], 1e-9, "first winding");
        assertEquals(2, circuit.matrix().getIALT()[5], 1e-9, "second winding");
        assertEquals(2, circuit.matrix().getP()[4 + 2], 1e-9, "first winding auxiliary slot");
        assertEquals(2, circuit.matrix().getP()[4 + 3], 1e-9, "second winding auxiliary slot");
    }

    @Test
    void anOrdinaryInductorLoopWithoutAUniqueCurrentIsReported() {
        var circuit = build(SolverType.SOLVER_BE, new CircuitTypCore[]{LK_U, LK_L, LK_L, LK_R},
                new int[]{1, 1, 1, 2}, new int[]{0, 2, 2, 0}, new int[]{1, -1, -1, -1},
                new double[][]{dc(5), reactive(1e-3), reactive(1e-3), resistor(1)}, 2, 1);
        var failure = assertThrows(IllegalStateException.class, () -> solve(SolverType.SOLVER_BE, circuit));
        assertTrue(failure.getMessage().contains("no unique solution"), failure.getMessage());
    }

    @Test
    void diodeStatesThatCannotAgreeWithTheirBiasAreNotClaimedAsDc() {
        // A blocking resistance below the on/off split reads as conducting, so the state never matches the bias.
        double[] unstable = diode(1e9, 0.7, 0);
        unstable[3] = 100;
        var circuit = build(SolverType.SOLVER_BE, new CircuitTypCore[]{LK_U, LK_R, LK_D},
                new int[]{1, 1, 2}, new int[]{0, 2, 0}, new int[]{1, -1, -1},
                new double[][]{dc(-5), resistor(2), unstable}, 2, 1);
        var failure = assertThrows(IllegalStateException.class, () -> solve(SolverType.SOLVER_BE, circuit));
        assertTrue(failure.getMessage().contains("did not converge"), failure.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = CircuitTypCore.class, names = {"TH_RTH", "REL_MMF", "LK_MOTOR", "LK_BJT", "NONLIN_REL"})
    void unsupportedElementTypesAreRejectedByName(CircuitTypCore type) {
        var circuit = build(SolverType.SOLVER_BE, new CircuitTypCore[]{LK_U, LK_R, type},
                new int[]{1, 1, 2}, new int[]{0, 2, 0}, new int[]{1, -1, type == REL_MMF ? 2 : -1},
                new double[][]{dc(5), resistor(2), new double[21]}, 2, type == REL_MMF ? 2 : 1);
        var failure = assertThrows(IllegalArgumentException.class, () -> solve(SolverType.SOLVER_BE, circuit));
        assertTrue(failure.getMessage().contains(type.name()), failure.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value = CircuitTypCore.class, names = {"LK_U", "LK_I"})
    void controlledSourcesAreRejected(CircuitTypCore type) {
        double[] controlled = dc(1);
        controlled[0] = SourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY_NEW;
        var circuit = build(SolverType.SOLVER_BE, new CircuitTypCore[]{type, LK_R},
                new int[]{1, 1}, new int[]{0, 0}, new int[]{type == LK_U ? 1 : -1, -1},
                new double[][]{controlled, resistor(1)}, 1, type == LK_U ? 1 : 0);
        var failure = assertThrows(IllegalArgumentException.class, () -> solve(SolverType.SOLVER_BE, circuit));
        assertTrue(failure.getMessage().contains("controlled"), failure.getMessage());
    }

    @Test
    void sinusoidalAndSignalSourcesTakeTheirTimeZeroValue() {
        double[] sine = new double[21];
        sine[0] = SourceType.QUELLE_SIN_NEW;
        sine[2] = 50;
        sine[3] = 3;
        sine[4] = 0;
        sine[20] = 100;
        var circuit = build(SolverType.SOLVER_BE, new CircuitTypCore[]{LK_U, LK_R, LK_I},
                new int[]{1, 1, 1}, new int[]{0, 0, 0}, new int[]{1, -1, -1},
                new double[][]{sine, resistor(1), signal(2)}, 1, 1);
        solve(SolverType.SOLVER_BE, circuit);
        assertEquals(3, circuit.matrix().getP()[1], 1e-12, "offset only at t = 0");
        assertEquals(2, circuit.matrix().getIALT()[2], 1e-12);
    }

    private static double[] signal(double value) {
        double[] values = dc(value);
        values[0] = SourceType.QUELLE_SIGNALGESTEUERT_NEW;
        return values;
    }

    @Test
    void malformedCircuitsFailBeforeTheHistoriesChange() {
        var solver = new InitialConditionSolver(SolverType.SOLVER_BE);
        assertThrows(IllegalStateException.class, () -> solver.setDcOperatingPoint(null, null));
        var noAux = build(SolverType.SOLVER_BE, new CircuitTypCore[]{LK_U, LK_R}, new int[]{1, 1},
                new int[]{0, 0}, new int[]{0, -1}, new double[][]{dc(1), resistor(1)}, 1, 1);
        Arrays.fill(noAux.matrix().getPALT(), 9);
        assertThrows(IllegalArgumentException.class,
                () -> solver.setDcOperatingPoint(noAux.matrix(), noAux.netlist()));
        assertEquals(9, noAux.matrix().getPALT()[0]);
        for (var type : new CircuitTypCore[]{LK_D, LK_THYR}) {
            var thin = build(SolverType.SOLVER_BE, new CircuitTypCore[]{LK_U, LK_R, type},
                    new int[]{1, 1, 2}, new int[]{0, 2, 0}, new int[]{1, -1, -1},
                    new double[][]{dc(1), resistor(1), new double[type == LK_D ? 3 : 8]}, 2, 1);
            assertThrows(IllegalArgumentException.class,
                    () -> solver.setDcOperatingPoint(thin.matrix(), thin.netlist()), type.name());
        }
    }

    @Test
    void aCapacitorBetweenSeparateGroupsLeavesTheirRelativePotentialUndetermined() {
        var circuit = build(SolverType.SOLVER_BE, new CircuitTypCore[]{LK_U, LK_R, LK_C, LK_R},
                new int[]{1, 1, 1, 2}, new int[]{0, 0, 2, 0}, new int[]{1, -1, -1, -1},
                new double[][]{dc(1), resistor(1), reactive(1), resistor(1)}, 2, 1);
        assertDoesNotThrow(() -> solve(SolverType.SOLVER_BE, circuit), "groups joined to ground through resistors");
        var separate = build(SolverType.SOLVER_BE, new CircuitTypCore[]{LK_U, LK_R, LK_C},
                new int[]{1, 1, 2}, new int[]{0, 0, 3}, new int[]{1, -1, -1},
                new double[][]{dc(1), resistor(1), reactive(1)}, 3, 1);
        assertThrows(IllegalStateException.class, () -> solve(SolverType.SOLVER_BE, separate));
    }
}
