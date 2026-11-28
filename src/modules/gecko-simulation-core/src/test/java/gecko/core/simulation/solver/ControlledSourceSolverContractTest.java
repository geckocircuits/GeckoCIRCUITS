package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class ControlledSourceSolverContractTest {
    @ParameterizedTest
    @EnumSource(SolverType.class)
    void voltageControlledCurrentSourceDrivesItsLoadAndReportsTheDesktopBranchCurrent(SolverType integration) {
        for (int mode : new int[]{5, 399}) {
            double[][] parameters = new double[3][21];
            parameters[0][1] = 2;
            parameters[1][0] = 10;
            parameters[2][0] = mode;
            parameters[2][11] = 0.5;
            var netlist = new CircuitNetlist();
            netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_I},
                    new int[]{1, 2, 2}, new int[]{0, 0, 0}, new int[]{1, -1, -1}, parameters, 2, 1, 3);
            netlist.setControlElementIndex(2, 0);
            var solver = new MatrixSolver(integration);
            solver.initializeMatrices(2, 1, 3);
            solver.buildMatrixA(netlist, 0.125, 0, false);
            solver.buildVectorB(netlist, 0.125, 0, false);
            solver.solve();

            assertEquals(2, solver.getP()[1], 1e-12);
            assertEquals(-10, solver.getP()[2], 1e-12);
            double[] currents = new double[3];
            new ComponentCurrentCalculator().calculateComponentCurrents(solver, netlist, currents,
                    0, 0.125, 0, true);
            assertEquals(0, currents[2], 0);
            assertEquals(-1, currents[1], 1e-12);
        }
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void voltageControlledVoltageSourceDrivesItsLoad(SolverType integration) {
        for (int mode : new int[]{5, 399}) {
            double[][] parameters = new double[3][21];
            parameters[0][1] = 2;
            parameters[1][0] = 10;
            parameters[2][0] = mode;
            parameters[2][11] = 3;
            parameters[2][12] = -Double.MAX_VALUE;
            parameters[2][13] = Double.MAX_VALUE;
            var netlist = new CircuitNetlist();
            netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_U},
                    new int[]{1, 2, 2}, new int[]{0, 0, 0}, new int[]{1, -1, 2}, parameters, 2, 2, 3);
            netlist.setControlElementIndex(2, 0);
            var solver = new MatrixSolver(integration);
            solver.initializeMatrices(2, 2, 3);
            solver.buildMatrixA(netlist, 0.125, 0, false);
            solver.buildVectorB(netlist, 0.125, 0, false);
            solver.solve();

            assertEquals(6, solver.getP()[2], 1e-12);
            double[] currents = new double[3];
            new ComponentCurrentCalculator().calculateComponentCurrents(solver, netlist, currents,
                    0, 0.125, 0, true);
            assertEquals(-0.6, currents[2], 1e-12);
            assertEquals(0.6, currents[1], 1e-12);
        }
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void currentControlledVoltageSourceUsesTheReferencedBranchDespiteSourceOrdering(SolverType integration) {
        for (int mode : new int[]{3, 396}) {
            double[][] parameters = new double[5][21];
            parameters[0][1] = 5;
            parameters[1][0] = 10;
            parameters[2][1] = 7;
            parameters[3][0] = 2;
            parameters[4][0] = mode;
            parameters[4][11] = 4;
            var netlist = new CircuitNetlist();
            netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_R,
                    CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_U},
                    new int[]{1, 1, 3, 2, 2}, new int[]{0, 0, 0, 0, 0},
                    new int[]{3, -1, 1, -1, 2}, parameters, 3, 3, 5);
            netlist.setControlElementIndex(4, 0);
            var solver = new MatrixSolver(integration);
            solver.initializeMatrices(3, 3, 5);
            solver.buildMatrixA(netlist, 0.125, 0, false);
            solver.buildVectorB(netlist, 0.125, 0, false);
            solver.solve();

            assertEquals(-0.5, solver.getP()[6], 1e-12);
            assertEquals(7, solver.getP()[3], 1e-12);
            assertEquals(-2, solver.getP()[2], 1e-12);
            double[] currents = new double[5];
            new ComponentCurrentCalculator().calculateComponentCurrents(solver, netlist, currents,
                    0, 0.125, 0, true);
            assertEquals(1, currents[4], 1e-12);
            assertEquals(-1, currents[3], 1e-12);
        }
    }
}
