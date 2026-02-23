/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import gecko.core.math.Matrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MatrixSolver class, focusing on matrix initialization and
 * node potential history shifting.
 */
class MatrixSolverTest {
    private MatrixSolver solver;
    private static final int NODE_COUNT = 5;
    private static final int VOLTAGE_SOURCE_COUNT = 1;
    private static final int ELEMENT_COUNT = 8;

    @BeforeEach
    void setUp() {
        solver = new MatrixSolver(SolverType.SOLVER_BE);
        solver.initializeMatrices(NODE_COUNT, VOLTAGE_SOURCE_COUNT, ELEMENT_COUNT);
    }

    @Test
    void testMatrixSolverInitialization() {
        // Verify matrix size calculation: nodes + voltage sources + 1 (ground)
        assertEquals(NODE_COUNT + VOLTAGE_SOURCE_COUNT + 1, solver.getMatrixSize());

        // Verify system matrix allocation
        assertNotNull(solver.getSystemMatrix());
        assertEquals(solver.getMatrixSize(), solver.getSystemMatrix().length);
        assertEquals(solver.getMatrixSize(), solver.getSystemMatrix()[0].length);

        // Verify vectors allocation
        assertNotNull(solver.getb());
        assertEquals(solver.getMatrixSize(), solver.getb().length);

        // Verify node potential arrays
        assertNotNull(solver.getP());
        assertEquals(solver.getMatrixSize(), solver.getP().length);
        assertNotNull(solver.getPALT());
        assertEquals(solver.getMatrixSize(), solver.getPALT().length);
        assertNotNull(solver.getPALTALT());
        assertEquals(solver.getMatrixSize(), solver.getPALTALT().length);
        assertNotNull(solver.getPALTALTALT());
        assertEquals(solver.getMatrixSize(), solver.getPALTALTALT().length);

        // Verify component current arrays
        assertNotNull(solver.getIALT());
        assertEquals(ELEMENT_COUNT, solver.getIALT().length);
        assertNotNull(solver.getIALTALT());
        assertEquals(ELEMENT_COUNT, solver.getIALTALT().length);
        assertNotNull(solver.getIALTALTALT());
        assertEquals(ELEMENT_COUNT, solver.getIALTALTALT().length);
    }

    @Test
    void testUpdateNodePotentialsHistoryShifting() {
        double[] p = solver.getP();
        double[] pALT = solver.getPALT();
        double[] pALTALT = solver.getPALTALT();
        double[] pALTALTALT = solver.getPALTALTALT();

        // Initialize each potential array with distinctive test values
        for (int i = 0; i < solver.getMatrixSize(); i++) {
            p[i] = 1.0 * (i + 1);          // p[i] = 1, 2, 3, ...
            pALT[i] = 10.0 * (i + 1);      // pALT[i] = 10, 20, 30, ...
            pALTALT[i] = 100.0 * (i + 1);  // pALTALT[i] = 100, 200, 300, ...
            pALTALTALT[i] = 0.0;            // pALTALTALT[i] = 0 (will be overwritten)
        }

        // Call updateNodePotentials()
        solver.updateNodePotentials(1.0e-4, 0.0);

        // Verify history shifting:
        // After update:
        // - pALTALTALT should equal old pALTALT (100, 200, 300, ...)
        // - pALTALT should equal old pALT (10, 20, 30, ...)
        // - pALT should equal old p (1, 2, 3, ...)
        for (int i = 0; i < solver.getMatrixSize(); i++) {
            // pALTALTALT <- pALTALT
            assertEquals(100.0 * (i + 1), pALTALTALT[i], 1.0e-12,
                    "pALTALTALT[" + i + "] should equal old pALTALT[" + i + "]");
            // pALTALT <- pALT
            assertEquals(10.0 * (i + 1), pALTALT[i], 1.0e-12,
                    "pALTALT[" + i + "] should equal old pALT[" + i + "]");
            // pALT <- p
            assertEquals(1.0 * (i + 1), pALT[i], 1.0e-12,
                    "pALT[" + i + "] should equal old p[" + i + "]");
        }
    }

    @Test
    void testUpdateComponentCurrentsHistoryShifting() {
        double[] iALT = solver.getIALT();
        double[] iALTALT = solver.getIALTALT();
        double[] iALTALTALT = solver.getIALTALTALT();

        // Initialize component current arrays with distinctive test values
        for (int i = 0; i < ELEMENT_COUNT; i++) {
            iALT[i] = 5.0 * (i + 1);        // iALT[i] = 5, 10, 15, ...
            iALTALT[i] = 50.0 * (i + 1);    // iALTALT[i] = 50, 100, 150, ...
            iALTALTALT[i] = 0.0;            // iALTALTALT[i] = 0 (will be overwritten)
        }

        // Call updateNodePotentials()
        solver.updateNodePotentials(1.0e-4, 0.0);

        // Verify component current history shifting:
        // - iALTALTALT should equal old iALTALT (50, 100, 150, ...)
        // - iALTALT should equal old iALT (5, 10, 15, ...)
        for (int i = 0; i < ELEMENT_COUNT; i++) {
            // iALTALTALT <- iALTALT
            assertEquals(50.0 * (i + 1), iALTALTALT[i], 1.0e-12,
                    "iALTALTALT[" + i + "] should equal old iALTALT[" + i + "]");
            // iALTALT <- iALT
            assertEquals(5.0 * (i + 1), iALTALT[i], 1.0e-12,
                    "iALTALT[" + i + "] should equal old iALT[" + i + "]");
        }
    }

    @Test
    void testMultipleTimeStepHistoryShifting() {
        double[] p = solver.getP();
        double[] pALT = solver.getPALT();
        double[] pALTALT = solver.getPALTALT();
        double[] pALTALTALT = solver.getPALTALTALT();

        // Initialize at time step 0
        for (int i = 0; i < solver.getMatrixSize(); i++) {
            p[i] = 1.0;
            pALT[i] = 0.0;
            pALTALT[i] = 0.0;
            pALTALTALT[i] = 0.0;
        }

        // Time step 1: update and set new p values
        solver.updateNodePotentials(1.0e-4, 1.0e-4);
        // p becomes pALT (1.0), so set new p to 2.0
        for (int i = 0; i < solver.getMatrixSize(); i++) {
            p[i] = 2.0;
        }

        // Time step 2: update and set new p values
        solver.updateNodePotentials(1.0e-4, 2.0e-4);
        // Verify: pALTALT should be 1.0 (from step 0), pALT should be 2.0 (from step 1)
        for (int i = 0; i < solver.getMatrixSize(); i++) {
            assertEquals(1.0, pALTALT[i], 1.0e-12,
                    "At time step 2: pALTALT[" + i + "] should be 1.0");
            assertEquals(2.0, pALT[i], 1.0e-12,
                    "At time step 2: pALT[" + i + "] should be 2.0");
        }

        // Set new p to 3.0
        for (int i = 0; i < solver.getMatrixSize(); i++) {
            p[i] = 3.0;
        }

        // Time step 3: update and verify full history
        solver.updateNodePotentials(1.0e-4, 3.0e-4);
        // Verify: pALTALTALT should be 1.0 (from step 1)
        for (int i = 0; i < solver.getMatrixSize(); i++) {
            assertEquals(1.0, pALTALTALT[i], 1.0e-12,
                    "At time step 3: pALTALTALT[" + i + "] should be 1.0");
            assertEquals(2.0, pALTALT[i], 1.0e-12,
                    "At time step 3: pALTALT[" + i + "] should be 2.0");
            assertEquals(3.0, pALT[i], 1.0e-12,
                    "At time step 3: pALT[" + i + "] should be 3.0");
        }
    }

    @Test
    void testSolverTypePreservation() {
        // Test with different solver types
        MatrixSolver beeSolver = new MatrixSolver(SolverType.SOLVER_BE);
        assertEquals(SolverType.SOLVER_BE, beeSolver.getSolverType());

        MatrixSolver trzSolver = new MatrixSolver(SolverType.SOLVER_TRZ);
        assertEquals(SolverType.SOLVER_TRZ, trzSolver.getSolverType());

        MatrixSolver gsSolver = new MatrixSolver(SolverType.SOLVER_GS);
        assertEquals(SolverType.SOLVER_GS, gsSolver.getSolverType());
    }

    @Test
    void testMatrixInitializationWithDifferentDimensions() {
        // Test with different circuit topologies
        MatrixSolver smallSolver = new MatrixSolver(SolverType.SOLVER_BE);
        smallSolver.initializeMatrices(2, 0, 3);
        assertEquals(3, smallSolver.getMatrixSize()); // 2 nodes + 0 sources + 1

        MatrixSolver largeSolver = new MatrixSolver(SolverType.SOLVER_BE);
        largeSolver.initializeMatrices(50, 10, 100);
        assertEquals(61, largeSolver.getMatrixSize()); // 50 nodes + 10 sources + 1
        assertEquals(100, largeSolver.getIALT().length);
    }

    @Test
    void testSolveSimple3x3System() {
        // Create a 3x3 system: Ax = b where x = [2, 2, 5]
        // A = [2  0  0]     b = [4]      x = [2]
        //     [0  3  0]         [6]          [2]
        //     [0  0  1]         [5]         [5]
        // Verification: A*x = b
        // Row 0: 2*2 + 0*2 + 0*5 = 4 ✓
        // Row 1: 0*2 + 3*2 + 0*5 = 6 ✓
        // Row 2: 0*2 + 0*2 + 1*5 = 5 ✓

        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        solver.initializeMatrices(2, 0, 2);  // 2x3 matrix system (matrixSize = 2 + 0 + 1 = 3)

        // Set up matrix A (diagonal - non-singular)
        double[][] a = solver.getSystemMatrix();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                a[i][j] = 0.0;
            }
        }
        a[0][0] = 2.0;
        a[1][1] = 3.0;
        a[2][2] = 1.0;

        // Set up vector b
        double[] b = solver.getb();
        b[0] = 4.0;
        b[1] = 6.0;
        b[2] = 5.0;

        // Solve the system
        solver.solve();

        // Verify the solution p = [2, 2, 5]
        double[] p = solver.getP();
        assertEquals(2.0, p[0], 1.0e-10, "p[0] should be 2.0");
        assertEquals(2.0, p[1], 1.0e-10, "p[1] should be 2.0");
        assertEquals(5.0, p[2], 1.0e-10, "p[2] should be 5.0");
    }

    @Test
    void testSolveNonDiagonalSystem() {
        // Create a 2x2 system: Ax = b where x = [1, 2]
        // A = [1  2]     b = [5]      x = [1]
        //     [3  4]         [11]         [2]
        // Verification:
        // Row 0: 1*1 + 2*2 = 1 + 4 = 5 ✓
        // Row 1: 3*1 + 4*2 = 3 + 8 = 11 ✓

        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        solver.initializeMatrices(1, 0, 1);  // 2x2 matrix system (matrixSize = 1 + 0 + 1 = 2)

        // Set up matrix A (initialize all to 0 first to avoid garbage)
        double[][] a = solver.getSystemMatrix();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                a[i][j] = 0.0;
            }
        }
        a[0][0] = 1.0;
        a[0][1] = 2.0;
        a[1][0] = 3.0;
        a[1][1] = 4.0;

        // Set up vector b
        double[] b = solver.getb();
        b[0] = 5.0;
        b[1] = 11.0;

        // Solve the system
        solver.solve();

        // Verify the solution p = [1, 2]
        double[] p = solver.getP();
        assertEquals(1.0, p[0], 1.0e-10, "p[0] should be 1.0");
        assertEquals(2.0, p[1], 1.0e-10, "p[1] should be 2.0");
    }

    @Test
    void testSolveMultipleTimes() {
        // Test that solve() can be called multiple times with changing b vectors
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        solver.initializeMatrices(1, 0, 1);  // 2x2 matrix system

        // Set up matrix A (fixed and non-singular)
        double[][] a = solver.getSystemMatrix();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                a[i][j] = 0.0;
            }
        }
        a[0][0] = 2.0;
        a[0][1] = 1.0;
        a[1][0] = 1.0;
        a[1][1] = 3.0;

        // First solve: b = [5, 8]
        // A*x = b => [2  1][x0]   [5]
        //             [1  3][x1] = [8]
        // Using Gaussian elimination: x0 = (5*3 - 8*1)/(2*3 - 1*1) = (15-8)/5 = 7/5 = 1.4
        // x1 = (2*8 - 1*5)/(2*3 - 1*1) = (16-5)/5 = 11/5 = 2.2
        double[] b = solver.getb();
        b[0] = 5.0;
        b[1] = 8.0;
        solver.solve();
        double[] p = solver.getP();
        double firstX0 = p[0];
        double firstX1 = p[1];

        // Verify first solution satisfies Ax = b
        assertEquals(5.0, 2.0 * firstX0 + 1.0 * firstX1, 1.0e-10);
        assertEquals(8.0, 1.0 * firstX0 + 3.0 * firstX1, 1.0e-10);

        // Second solve: b = [7, 11] (same matrix A)
        b[0] = 7.0;
        b[1] = 11.0;
        solver.solve();
        p = solver.getP();
        double secondX0 = p[0];
        double secondX1 = p[1];

        // Verify second solution is different and satisfies Ax = b
        assertNotEquals(firstX0, secondX0, "Solution should change with different b");
        assertEquals(7.0, 2.0 * secondX0 + 1.0 * secondX1, 1.0e-10);
        assertEquals(11.0, 1.0 * secondX0 + 3.0 * secondX1, 1.0e-10);
    }

    @Test
    void testSolveThrowsWhenNotInitialized() {
        // Test that solve() throws an exception if matrices are not initialized
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        // Don't call initializeMatrices()

        // Attempting to solve without initialization should throw
        assertThrows(IllegalStateException.class, solver::solve,
                "solve() should throw IllegalStateException when matrices not initialized");
    }

    @Test
    void testSetMatrixChangedFlag() {
        // Test that setMatrixChanged() properly marks matrix for refactorization
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        solver.initializeMatrices(1, 0, 1);

        // Set up a simple 2x2 system (diagonal)
        double[][] a = solver.getSystemMatrix();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                a[i][j] = 0.0;
            }
        }
        a[0][0] = 2.0;
        a[1][1] = 3.0;

        double[] b = solver.getb();
        b[0] = 4.0;
        b[1] = 6.0;

        // First solve
        solver.solve();
        double[] p = solver.getP();
        assertEquals(2.0, p[0], 1.0e-10);
        assertEquals(2.0, p[1], 1.0e-10);

        // Modify matrix A and mark as changed
        a[0][0] = 4.0;  // Change diagonal element
        solver.setMatrixChanged();
        b[0] = 8.0;     // Also change b

        // Second solve should use new matrix
        solver.solve();
        p = solver.getP();
        assertEquals(2.0, p[0], 1.0e-10, "Solution should be 8/4 = 2 with new matrix");
        assertEquals(2.0, p[1], 1.0e-10);
    }
}
