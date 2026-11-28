package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class CoupledCutsetInitializationTest {
    @ParameterizedTest
    @EnumSource(SolverType.class)
    void coupledCutsetRestoresConfiguredWindingCurrentAndFiniteVoltageHistories(SolverType method) {
        var circuit = new CircuitNetlist();
        double[] voltage = new double[13], current = new double[13];
        double[] first = new double[13], second = new double[13];
        voltage[0] = current[0] = 401;
        voltage[1] = 1;
        current[1] = first[1] = 0.5;
        first[0] = 0.001;
        second[0] = 0.002;
        circuit.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_LKOP2,
                        CircuitTypCore.LK_LKOP2, CircuitTypCore.LK_I, CircuitTypCore.LK_R},
                new int[]{1, 1, 3, 2, 3}, new int[]{0, 2, 0, 0, 0}, new int[]{1, 2, 3, -1, -1},
                new double[][]{voltage, first, second, current, new double[]{10, 0, 0}}, 3, 3, 5);
        circuit.registerMutualCoupling(1, 2, 0.5, first[0], second[0]);
        var matrix = new MatrixSolver(method);
        matrix.initializeMatrices(3, 3, 5);
        new InitialConditionSolver(method).setDialogInitialConditions(matrix, circuit);
        assertEquals(0.5, matrix.getIALT()[1]);
        assertArrayEquals(matrix.getIALT(), matrix.getIALTALT());
        assertArrayEquals(matrix.getIALT(), matrix.getIALTALTALT());
        for (double value : matrix.getP()) assertTrue(Double.isFinite(value));
        assertEquals(1, matrix.getP()[1] - matrix.getP()[0], 1e-9);
    }
}
