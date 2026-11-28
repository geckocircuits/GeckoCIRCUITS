package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class InitialConditionSolverTest {
    @ParameterizedTest
    @EnumSource(value = CircuitTypCore.class, names = {"LK_C", "TH_CTH", "REL_MMF", "TH_TEMP", "LK_L",
            "NONLIN_REL", "LK_LKOP2", "LK_I", "TH_FLOW", "LK_R"})
    void restoresSavedStateAndSeedsEveryHistory(CircuitTypCore type) {
        double[] parameters = new double[10];
        parameters[2] = 3;
        parameters[4] = 4;
        parameters[5] = 5;
        parameters[6] = 6;
        parameters[8] = 8;
        parameters[9] = 9;
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{type}, new int[]{1}, new int[]{2}, new int[]{1},
                new double[][]{parameters}, 2, 1, 1);
        MatrixSolver matrix = new MatrixSolver(SolverType.SOLVER_BE);
        matrix.initializeMatrices(2, 1, 1);
        for (double[] history : new double[][]{matrix.getPALT(), matrix.getPALTALT(), matrix.getPALTALTALT(),
                matrix.getIALT(), matrix.getIALTALT(), matrix.getIALTALTALT()}) {
            Arrays.fill(history, 99);
        }

        new InitialConditionSolver(SolverType.SOLVER_BE).setInitialConditions(matrix, netlist, SolverType.SOLVER_BE);

        double[] expectedPotentials = switch (type) {
            case LK_C, TH_CTH -> new double[]{0, 4, 5, 0};
            case REL_MMF, TH_TEMP -> new double[]{0, 8, 9, 0};
            case LK_LKOP2 -> new double[]{0, 0, 0, 3};
            default -> new double[4];
        };
        double expectedCurrent = switch (type) {
            case LK_C, TH_CTH, LK_L, NONLIN_REL, LK_LKOP2 -> 3;
            case LK_I, TH_FLOW -> 6;
            default -> 0;
        };
        assertArrayEquals(expectedPotentials, matrix.getPALT());
        assertArrayEquals(expectedPotentials, matrix.getP());
        assertArrayEquals(expectedPotentials, matrix.getPALTALT());
        assertArrayEquals(expectedPotentials, matrix.getPALTALTALT());
        assertArrayEquals(new double[]{expectedCurrent}, matrix.getIALT());
        assertArrayEquals(new double[]{expectedCurrent}, matrix.getIALTALT());
        assertArrayEquals(new double[]{expectedCurrent}, matrix.getIALTALTALT());
    }

    @ParameterizedTest
    @CsvSource({"SOLVER_BE, 0.5, 10000000", "SOLVER_TRZ, 0.25, 5000000",
            "SOLVER_GS, 0.3333333333333333, 6666666.666666667"})
    void inductanceWeightsRespectMethodAndZeroInductanceGuard(SolverType method, double normal, double guarded) {
        InitialConditionSolver solver = new InitialConditionSolver(method);
        assertEquals(normal, solver.getAWForInductance(0.002, 0.001), 1e-12);
        assertEquals(guarded, solver.getAWForInductance(0, 0.001), 1e-8);
    }

    @Test
    void missingDependenciesAreRejectedBeforeInitialization() {
        InitialConditionSolver solver = new InitialConditionSolver(SolverType.SOLVER_BE);
        assertThrows(IllegalStateException.class, () -> solver.setInitialConditions(null, new CircuitNetlist(), SolverType.SOLVER_BE));
        assertThrows(IllegalStateException.class, () -> solver.setInitialConditions(new MatrixSolver(SolverType.SOLVER_BE), null, SolverType.SOLVER_BE));
    }
}
