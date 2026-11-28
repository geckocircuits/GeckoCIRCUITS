package gecko.core.circuit.netlist;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CircuitNetlistSnapshotTest {
    private static CircuitNetlist liveCircuit() {
        double[][] parameters = new double[4][21];
        parameters[0][1] = 10;
        parameters[1][0] = 0.01;
        parameters[2][0] = 0.04;
        parameters[3][0] = 3;
        parameters[3][11] = 2;
        var circuit = new CircuitNetlist();
        circuit.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_LKOP2,
                CircuitTypCore.LK_LKOP2, CircuitTypCore.LK_U},
                new int[]{1, 1, 2, 3}, new int[]{0, 2, 0, 0}, new int[]{1, 3, 2, 4},
                parameters, 3, 4, 4);
        circuit.setControlElementIndex(3, 0);
        var names = new LinkedHashMap<String, Integer>();
        names.put("U.source", 0);
        names.put("L.primary", 1);
        names.put("L.secondary", 2);
        names.put("U.dependent", 3);
        circuit.setComponentNameToIndex(names);
        circuit.registerMutualCoupling(1, 2, -0.5, 0.01, 0.04);
        circuit.setSimulationTime(0.25);
        circuit.storeResults(new double[]{0, 10, 20, 30, 40, 50, 60, 70}, new double[]{-1, 2, 3, 4});
        return circuit;
    }

    private static void assertCapturedValues(CircuitNetlist snapshot) {
        assertEquals(4, snapshot.getElementCount());
        assertEquals(3, snapshot.getNodeMax());
        assertEquals(4, snapshot.getVoltageSourceMax());
        CircuitTypCore[] types = {CircuitTypCore.LK_U, CircuitTypCore.LK_LKOP2,
                CircuitTypCore.LK_LKOP2, CircuitTypCore.LK_U};
        int[] x = {1, 1, 2, 3}, y = {0, 2, 0, 0}, branches = {1, 3, 2, 4};
        for (int index = 0; index < 4; index++) {
            assertEquals(types[index], snapshot.getType(index));
            assertEquals(x[index], snapshot.getNodeX(index));
            assertEquals(y[index], snapshot.getNodeY(index));
            assertEquals(branches[index], snapshot.getVoltageSourceNumber(index));
        }
        assertEquals(10, snapshot.getParameter(0)[1]);
        assertEquals(0.01, snapshot.getParameter(1)[0]);
        assertEquals(0.04, snapshot.getParameter(2)[0]);
        assertEquals(3, snapshot.getParameter(3)[0]);
        assertEquals(2, snapshot.getParameter(3)[11]);
        assertEquals(0, snapshot.getControlElementIndex(3));
        assertEquals(-1, snapshot.getControlElementIndex(0));
        assertEquals(List.of("U.source", "L.primary", "L.secondary", "U.dependent"),
                List.copyOf(snapshot.getComponentNameToIndex().keySet()));
        assertEquals(2, snapshot.getElementIndexByName("L.secondary"));
        assertEquals(-1, snapshot.getElementIndexByName("changed"));
        assertEquals(0.25, snapshot.getSimulationTime());
        assertArrayEquals(new double[]{0, 10, 20, 30, 40, 50, 60, 70}, snapshot.getLastNodeVoltages());
        assertArrayEquals(new double[]{-1, 2, 3, 4}, snapshot.getLastComponentCurrents());
        assertEquals(1, snapshot.getAllCouplings().size());
        var coupling = snapshot.getAllCouplings().get(0);
        assertEquals(1, coupling.getInductor1Index());
        assertEquals(2, coupling.getInductor2Index());
        assertEquals(-0.5, coupling.getCouplingCoefficient());
        assertEquals(0.01, coupling.getInductance1());
        assertEquals(0.04, coupling.getInductance2());
        assertEquals(-0.01, coupling.getMutualInductance(), 1e-15);
        assertEquals(List.of(2), snapshot.getCouplingPartners(1));
        assertArrayEquals(new double[]{2}, snapshot.buildCouplingArrays()[0][1]);
        assertArrayEquals(new double[]{-0.01}, snapshot.buildCouplingArrays()[1][1], 1e-15);
    }

    @Test
    void capturedElectricalStateSurvivesLiveUpdatesAndCircuitReplacement() {
        var live = liveCircuit();
        var snapshot = live.detachedSnapshot();
        live.getParameter(0)[1] = 77;
        live.setControlElementIndex(3, 2);
        live.getLastNodeVoltagesRef()[1] = 88;
        live.getLastComponentCurrentsRef()[0] = 99;
        assertCapturedValues(snapshot);

        live.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_R}, new int[]{1}, new int[]{0},
                new int[]{-1}, new double[][]{{99}}, 1, 0, 1);
        live.setComponentNameToIndex(Map.of("changed", 0));
        live.setSimulationTime(9);
        live.storeResults(new double[]{0, 99}, new double[]{0.3});
        assertCapturedValues(snapshot);
    }

    @Test
    void callbackChangesCannotAlterTheLiveCircuitOrAnotherSnapshot() {
        var live = liveCircuit();
        var callback = live.detachedSnapshot();
        var other = live.detachedSnapshot();
        callback.getParameter(0)[1] = 123;
        callback.getParameter(1)[0] = 456;
        callback.setControlElementIndex(3, 2);
        callback.setComponentNameToIndex(Map.of("changed", 0));
        callback.setSimulationTime(9);
        callback.getLastNodeVoltagesRef()[1] = -99;
        callback.getLastComponentCurrentsRef()[2] = 900;
        callback.clear();

        assertCapturedValues(live);
        assertCapturedValues(other);
        assertThrows(UnsupportedOperationException.class,
                () -> other.getComponentNameToIndex().put("changed", 0));
    }

    @Test
    void anEmptySnapshotCanBeInitializedWithoutChangingTheOriginal() {
        var original = new CircuitNetlist();
        var snapshot = original.detachedSnapshot();
        snapshot.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_R}, new int[]{1}, new int[]{0},
                new int[]{-1}, new double[][]{{10}}, 1, 0, 1);
        assertEquals(0, original.getElementCount());
        assertEquals(0, original.getNodeMax());
        assertEquals(0, original.getVoltageSourceMax());
        assertTrue(original.getComponentNameToIndex().isEmpty());
        assertTrue(original.getAllCouplings().isEmpty());
        assertArrayEquals(new double[0], original.getLastNodeVoltages());
    }
}
