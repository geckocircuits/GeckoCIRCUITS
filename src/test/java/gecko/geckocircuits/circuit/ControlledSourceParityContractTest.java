package gecko.geckocircuits.circuit;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.simulation.solver.MatrixSolver;
import gecko.core.simulation.solver.ComponentCurrentCalculator;
import gecko.geckocircuits.allg.SolverType;
import gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;
import gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import gecko.geckocircuits.math.Matrix;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class ControlledSourceParityContractTest {
    @ParameterizedTest
    @EnumSource(SolverType.class)
    void voltageControlMatchesDesktopPotentialsAndReportedCurrents(SolverType integration) {
        for (int mode : new int[]{5, 399}) {
            double[][] parameters = new double[3][21];
            parameters[0][1] = 2;
            parameters[1][0] = 10;
            parameters[2][0] = mode;
            parameters[2][11] = 0.5;
            int[] x = {1, 2, 2};
            int[] y = {0, 0, 0};
            int[] branches = {1, -1, -1};
            var desktop = NetListLK.fabricExcludingSubcircuits(List.of(), List.of());
            desktop.typ = new CircuitTyp[]{CircuitTyp.LK_U, CircuitTyp.LK_R, CircuitTyp.LK_I};
            desktop.knotenX = x.clone();
            desktop.knotenY = y.clone();
            desktop.spgQuelleNr = branches.clone();
            desktop.parameter = parameters;
            desktop.nodePairDirVoltContSrc = new int[][]{{0, 0}, {0, 0}, {1, 0}};
            desktop.knotenMAX = 2;
            desktop.spgQuelleMAX = 1;
            desktop.elementANZAHL = 3;
            desktop.elementANZAHLneu = 3;
            desktop.eLKneu = new AbstractCircuitBlockInterface[3];
            for (int index = 0; index < 3; index++) {
                desktop.eLKneu[index] = (AbstractCircuitBlockInterface) desktop.typ[index].getTypeInfo().fabric();
            }
            desktop._singularityEntries = new int[]{0};
            var legacySolver = new LKMatrices(integration);
            legacySolver.initMatrizen(desktop, false, integration);
            legacySolver.schreibeMatrix_A(0.125, 0, false);
            legacySolver.schreibeMatrix_B(0.125, 0, false);
            double[] legacy = new Matrix(legacySolver.a).solve(
                    new Matrix(legacySolver.bVector, legacySolver.bVector.length)).getColumnPackedCopy();

            var core = new CircuitNetlist();
            core.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_I},
                    x, y, branches, parameters, 2, 1, 3);
            core.setControlElementIndex(2, 0);
            var shared = new MatrixSolver(gecko.core.allg.SolverType.valueOf(integration.name()));
            shared.initializeMatrices(2, 1, 3);
            shared.buildMatrixA(core, 0.125, 0, false);
            shared.buildVectorB(core, 0.125, 0, false);
            shared.solve();

            assertEquals(-10, legacy[2], 1e-12);
            assertEquals(-10, shared.getP()[2], 1e-12);
            assertArrayEquals(legacy, shared.getP(), 1e-12);
            System.arraycopy(legacy, 0, legacySolver.p, 0, legacy.length);
            assertFalse(legacySolver.berechneBauteilStroeme(1, 0.125, 0, true, 0));
            double[] sharedCurrents = new double[3];
            new ComponentCurrentCalculator().calculateComponentCurrents(shared, core, sharedCurrents,
                    0, 0.125, 0, true);
            assertEquals(-1, desktop.eLKneu[1]._currentInAmps, 1e-12);
            assertEquals(0, desktop.eLKneu[2]._currentInAmps, 0,
                    "The legacy calculation reports zero for a voltage-controlled current source");
            assertEquals(desktop.eLKneu[2]._currentInAmps, sharedCurrents[2], 0,
                    "The shared calculation preserves the desktop reported source current");
            assertEquals(-1, sharedCurrents[1], 1e-12);
        }
    }
}
