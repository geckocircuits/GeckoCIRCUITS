package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class TransformerSourceSolverContractTest {
    @ParameterizedTest
    @EnumSource(SolverType.class)
    void transformerModeConstrainsTheReferencedCurrentWithoutFixingItsVoltage(SolverType integration) {
        double[] gains = {2, -2};
        double[] expectedVoltages = {-10, 10};
        double[] expectedCurrents = {1, -1};
        for (int mode : new int[]{6, 398}) {
            for (int polarity = 0; polarity < gains.length; polarity++) {
                double[][] parameters = new double[5][21];
                parameters[0][1] = 5;
                parameters[1][0] = 10;
                parameters[2][1] = 7;
                parameters[3][0] = 10;
                parameters[4][0] = mode;
                parameters[4][11] = gains[polarity];
                var circuit = new CircuitNetlist();
                circuit.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_R,
                        CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_U},
                        new int[]{1, 1, 3, 2, 2}, new int[]{0, 0, 0, 0, 0},
                        new int[]{3, -1, 1, -1, 2}, parameters, 3, 3, 5);
                circuit.setControlElementIndex(4, 0);
                var solver = new MatrixSolver(integration);
                solver.initializeMatrices(3, 3, 5);
                solver.buildMatrixA(circuit, 0.125, 0, false);
                solver.buildVectorB(circuit, 0.125, 0, false);
                solver.solve();

                assertEquals(-0.5, solver.getP()[6], 1e-12);
                assertEquals(expectedVoltages[polarity], solver.getP()[2], 1e-12);
                assertEquals(expectedCurrents[polarity], solver.getP()[5], 1e-12);
                assertEquals(5, solver.getP()[1], 1e-12);
                assertEquals(7, solver.getP()[3], 1e-12);
            }
        }
    }
}
