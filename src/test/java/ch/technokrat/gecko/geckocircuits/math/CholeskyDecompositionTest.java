/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.math;

import org.junit.Test;
import static org.junit.Assert.*;

public class CholeskyDecompositionTest {
    private static final double TOLERANCE = 1e-10;

    // SPD Check Tests
    @Test
    public void testCholesky_SPD_Matrix() {
        // [[4,2],[2,3]] is symmetric positive definite
        double[][] data = {{4, 2}, {2, 3}};
        Matrix m = new Matrix(data);
        CholeskyDecomposition chol = new CholeskyDecomposition(m);
        assertTrue(chol.isSPD());
    }

    @Test
    public void testCholesky_NonSPD_Matrix() {
        // [[1,2],[3,4]] is not symmetric
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(data);
        CholeskyDecomposition chol = new CholeskyDecomposition(m);
        assertFalse(chol.isSPD());
    }

    @Test
    public void testCholesky_SymmetricButNotPositiveDefinite() {
        // [[1,2],[2,1]] is symmetric but not positive definite
        double[][] data = {{1, 2}, {2, 1}};
        Matrix m = new Matrix(data);
        CholeskyDecomposition chol = new CholeskyDecomposition(m);
        assertFalse(chol.isSPD());
    }

    @Test
    public void testCholesky_IdentityMatrix() {
        Matrix identity = Matrix.identity(3, 3);
        CholeskyDecomposition chol = new CholeskyDecomposition(identity);
        assertTrue(chol.isSPD());
    }

    // Decomposition Verification Tests
    @Test
    public void testGetL_LowerTriangular() {
        double[][] data = {{4, 2}, {2, 3}};
        Matrix m = new Matrix(data);
        CholeskyDecomposition chol = new CholeskyDecomposition(m);
        Matrix L = chol.getL();

        // Check dimensions
        assertEquals(2, L.getRowDimension());
        assertEquals(2, L.getColumnDimension());

        // Check upper triangle is zero
        assertEquals(0.0, L.get(0, 1), TOLERANCE);

        // Check diagonal elements are positive
        assertTrue(L.get(0, 0) > 0);
        assertTrue(L.get(1, 1) > 0);
    }

    @Test
    public void testL_TimesL_Transpose_EqualsA() {
        // For SPD matrix A = L*L'
        double[][] data = {{4, 2}, {2, 3}};
        Matrix A = new Matrix(data);
        CholeskyDecomposition chol = new CholeskyDecomposition(A);

        if (chol.isSPD()) {
            Matrix L = chol.getL();
            Matrix LT = L.transpose();
            Matrix product = L.times(LT);

            // Verify L*L' = A
            assertEquals(A.get(0, 0), product.get(0, 0), TOLERANCE);
            assertEquals(A.get(0, 1), product.get(0, 1), TOLERANCE);
            assertEquals(A.get(1, 0), product.get(1, 0), TOLERANCE);
            assertEquals(A.get(1, 1), product.get(1, 1), TOLERANCE);
        }
    }

    @Test
    public void testGetL_IdentityMatrix() {
        Matrix identity = Matrix.identity(3, 3);
        CholeskyDecomposition chol = new CholeskyDecomposition(identity);
        Matrix L = chol.getL();

        // For identity matrix, L should also be identity
        assertEquals(1.0, L.get(0, 0), TOLERANCE);
        assertEquals(1.0, L.get(1, 1), TOLERANCE);
        assertEquals(1.0, L.get(2, 2), TOLERANCE);
        assertEquals(0.0, L.get(0, 1), TOLERANCE);
        assertEquals(0.0, L.get(1, 0), TOLERANCE);
    }

    @Test
    public void testL_TimesL_Transpose_3x3() {
        // Test with 3x3 SPD matrix
        double[][] data = {{4, 2, 1}, {2, 5, 3}, {1, 3, 6}};
        Matrix A = new Matrix(data);
        CholeskyDecomposition chol = new CholeskyDecomposition(A);

        assertTrue(chol.isSPD());

        Matrix L = chol.getL();
        Matrix LT = L.transpose();
        Matrix product = L.times(LT);

        // Verify L*L' = A
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals(A.get(i, j), product.get(i, j), 1e-9);
            }
        }
    }

    // Solving Tests
    @Test
    public void testSolve() {
        // Solve I*x = b where I is identity (simple SPD case)
        Matrix I = Matrix.identity(2, 2);
        double[][] dataB = {{5}, {3}};
        Matrix b = new Matrix(dataB);

        CholeskyDecomposition chol = new CholeskyDecomposition(I);
        assertTrue(chol.isSPD());

        Matrix x = chol.solve(b);

        // For identity matrix, x = b
        assertEquals(5.0, x.get(0, 0), TOLERANCE);
        assertEquals(3.0, x.get(1, 0), TOLERANCE);
    }

    @Test
    public void testSolve_3x3System() {
        // Solve a diagonal (SPD) system
        double[][] dataA = {{2, 0, 0}, {0, 3, 0}, {0, 0, 4}};
        double[][] dataB = {{6}, {9}, {8}};
        Matrix A = new Matrix(dataA);
        Matrix b = new Matrix(dataB);

        CholeskyDecomposition chol = new CholeskyDecomposition(A);
        assertTrue(chol.isSPD());

        Matrix x = chol.solve(b);

        // For diagonal matrix, x = [b/A_ii] = [6/2, 9/3, 8/4] = [3, 3, 2]
        assertEquals(3.0, x.get(0, 0), TOLERANCE);
        assertEquals(3.0, x.get(1, 0), TOLERANCE);
        assertEquals(2.0, x.get(2, 0), TOLERANCE);
    }

    @Test(expected = RuntimeException.class)
    public void testSolve_NonSPD_ThrowsException() {
        // Try to solve with non-SPD matrix
        double[][] dataA = {{1, 2}, {3, 4}};
        double[][] dataB = {{1}, {2}};
        Matrix A = new Matrix(dataA);
        Matrix b = new Matrix(dataB);

        CholeskyDecomposition chol = new CholeskyDecomposition(A);
        assertFalse(chol.isSPD());
        chol.solve(b); // Should throw RuntimeException
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSolve_DimensionMismatch_ThrowsException() {
        // Try to solve with mismatched dimensions
        double[][] dataA = {{4, 2}, {2, 3}};
        double[][] dataB = {{1}, {2}, {3}};
        Matrix A = new Matrix(dataA);
        Matrix b = new Matrix(dataB);

        CholeskyDecomposition chol = new CholeskyDecomposition(A);
        chol.solve(b); // Should throw IllegalArgumentException
    }

    @Test
    public void testSolve_MultipleRHS() {
        // Solve I*X = B with multiple right-hand sides (identity matrix)
        Matrix I = Matrix.identity(2, 2);
        double[][] dataB = {{5, 7}, {3, 9}};
        Matrix B = new Matrix(dataB);

        CholeskyDecomposition chol = new CholeskyDecomposition(I);
        assertTrue(chol.isSPD());

        Matrix X = chol.solve(B);

        // For identity matrix, X = B
        assertEquals(5.0, X.get(0, 0), TOLERANCE);
        assertEquals(7.0, X.get(0, 1), TOLERANCE);
        assertEquals(3.0, X.get(1, 0), TOLERANCE);
        assertEquals(9.0, X.get(1, 1), TOLERANCE);
    }

    @Test
    public void testCholesky_DiagonalMatrix() {
        // Diagonal matrices with positive entries are SPD
        double[][] data = {{2, 0, 0}, {0, 3, 0}, {0, 0, 4}};
        Matrix m = new Matrix(data);
        CholeskyDecomposition chol = new CholeskyDecomposition(m);
        assertTrue(chol.isSPD());

        Matrix L = chol.getL();
        // L should be diagonal with sqrt of diagonal entries
        assertEquals(Math.sqrt(2), L.get(0, 0), TOLERANCE);
        assertEquals(Math.sqrt(3), L.get(1, 1), TOLERANCE);
        assertEquals(Math.sqrt(4), L.get(2, 2), TOLERANCE);
    }

    @Test
    public void testGetL_NonSPDMatrix() {
        // Even for non-SPD matrices, getL should return a matrix
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(data);
        CholeskyDecomposition chol = new CholeskyDecomposition(m);

        assertFalse(chol.isSPD());

        Matrix L = chol.getL();
        assertNotNull(L);
        assertEquals(2, L.getRowDimension());
        assertEquals(2, L.getColumnDimension());
    }
}
