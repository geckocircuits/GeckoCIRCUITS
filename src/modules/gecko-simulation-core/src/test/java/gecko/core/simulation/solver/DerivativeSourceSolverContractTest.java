package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class DerivativeSourceSolverContractTest {
    @ParameterizedTest
    @EnumSource(SolverType.class)
    void currentDerivativeUsesItsExplicitTargetHistoryAcrossStepChanges(SolverType integration) {
        for (int mode : new int[]{4, 397}) {
            double[][] parameters = new double[5][21];
            parameters[0][1] = 5;
            parameters[1][0] = 10;
            parameters[2][1] = 7;
            parameters[3][0] = 2;
            parameters[4][0] = mode;
            parameters[4][11] = 0.25;
            parameters[4][15] = 0.2;
            var circuit = new CircuitNetlist();
            circuit.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_R,
                    CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_U},
                    new int[]{1, 1, 3, 2, 2}, new int[]{0, 0, 0, 0, 0},
                    new int[]{1, -1, 2, -1, 3}, parameters, 3, 3, 5);
            circuit.setControlElementIndex(4, 0);
            var solver = new MatrixSolver(integration);
            solver.initializeMatrices(3, 3, 5);
            double[] stepWidths = {0.125, 0.125, 0.25, 0.0625};
            double[] controlVoltages = {5, 10, 10, 2};
            double[] expectedTargetCurrents = {-0.5, -1, -1, -0.2};
            double[] expectedOutputVoltages = {1.8, 1, 0, -3.2};
            double time = 0;

            for (int index = 0; index < stepWidths.length; index++) {
                double dt = stepWidths[index];
                circuit.getParameter(0)[1] = controlVoltages[index];
                solver.buildMatrixA(circuit, dt, time, false);
                solver.buildVectorB(circuit, dt, time, false);
                solver.solve();

                assertEquals(expectedTargetCurrents[index], solver.getP()[4], 1e-12);
                assertEquals(expectedOutputVoltages[index], solver.getP()[2], 1e-12);
                assertEquals(7, solver.getP()[3], 1e-12);
                solver.shiftHistory();
                time += dt;
            }
        }
    }
}
