package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NumericalReferenceContractTest {
    @Test
    void internalReferencePreservesPublicVoltagesAndRespondsToResistanceChanges() {
        var circuit = new CircuitNetlist();
        double[] source = new double[13];
        source[0] = 401;
        source[1] = 6;
        circuit.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_R},
                new int[]{1, 1}, new int[]{0, 0}, new int[]{1, -1},
                new double[][]{source, new double[]{10, 0, 0}}, 1, 1, 2);
        circuit.setNumericalNodeOrder(new int[]{1, 0});
        var step = new ElectricalTimeStep(circuit, SolverType.SOLVER_BE);
        step.advance(0.001, 0, null);
        assertArrayEquals(new double[]{0, 6}, java.util.Arrays.copyOf(circuit.getLastNodeVoltagesRef(), 2), 1e-12);
        assertEquals(0.6, circuit.getLastComponentCurrents()[1], 1e-12);

        circuit.getParameter(1)[0] = 20;
        step.advance(0.001, 0.001, null);
        assertEquals(6, circuit.getLastNodeVoltagesRef()[1], 1e-12);
        assertEquals(0.3, circuit.getLastComponentCurrents()[1], 1e-12);
        assertEquals(0, circuit.getNodeY(0));
    }

    /** Grounded 6 V loop on node 1; floating 4 V island on nodes 2 (low) and 3, as a transformer secondary. */
    private static CircuitNetlist floatingIsland() {
        var circuit = new CircuitNetlist();
        double[] grounded = new double[13], island = new double[13];
        grounded[0] = island[0] = 401;
        grounded[1] = 6;
        island[1] = 4;
        circuit.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_R,
                        CircuitTypCore.LK_U, CircuitTypCore.LK_R},
                new int[]{1, 1, 3, 3}, new int[]{0, 0, 2, 2}, new int[]{1, -1, 2, -1},
                new double[][]{grounded, new double[]{10, 0, 0}, island, new double[]{2, 0, 0}}, 3, 2, 4);
        return circuit;
    }

    @Test
    void floatingIslandIsPinnedAtItsLowestNumericalNodeLikeTheDesktop() {
        var circuit = floatingIsland();
        circuit.setNumericalNodeOrder(new int[]{0, 1, 2, 3});
        new ElectricalTimeStep(circuit, SolverType.SOLVER_BE).advance(0.001, 0, null);
        // Desktop LKMatrices stamps 1 S at the lowest potential of each ungrounded group; no current flows through it.
        assertArrayEquals(new double[]{0, 6, 0, 4}, java.util.Arrays.copyOf(circuit.getLastNodeVoltagesRef(), 4), 1e-12);
        assertEquals(2, Math.abs(circuit.getLastComponentCurrents()[3]), 1e-12);
        assertEquals(0.6, Math.abs(circuit.getLastComponentCurrents()[1]), 1e-12);
    }

    @Test
    void floatingIslandIsSolvableWithoutTheDesktopNodeOrder() {
        var circuit = floatingIsland();
        new ElectricalTimeStep(circuit, SolverType.SOLVER_BE).advance(0.001, 0, null);
        double[] voltages = circuit.getLastNodeVoltagesRef();
        assertEquals(6, voltages[1], 1e-9);
        assertEquals(4, voltages[3] - voltages[2], 1e-9);
        assertEquals(2, Math.abs(circuit.getLastComponentCurrents()[3]), 1e-9);
    }

    @Test
    void changedStepWidthRebuildsTheCapacitorCompanionMatrix() {
        var circuit = new CircuitNetlist();
        double[] source = new double[13], capacitor = new double[13];
        source[0] = 401;
        source[1] = 6;
        capacitor[0] = capacitor[6] = capacitor[7] = 0.3;
        circuit.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_C},
                new int[]{1, 1, 2}, new int[]{0, 2, 0}, new int[]{1, -1, -1},
                new double[][]{source, new double[]{10, 0, 0}, capacitor}, 2, 1, 3);
        circuit.setNumericalNodeOrder(new int[]{1, 0, 2});
        var step = new ElectricalTimeStep(circuit, SolverType.SOLVER_BE);
        step.advance(1, 0, null);
        assertEquals(1.5, circuit.getLastNodeVoltagesRef()[2], 1e-12);
        step.advance(2, 1, null);
        assertEquals(3.3, circuit.getLastNodeVoltagesRef()[2], 1e-12);
    }
}
