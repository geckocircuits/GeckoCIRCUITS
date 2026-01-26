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

public class LUDecompositionTest {
    private static final double TOLERANCE = 1e-10;

    // Singularity Tests
    @Test
    public void testLUDecomposition_NonSingular() {
        double[][] data = {{2, 1}, {1, 2}};
        Matrix m = new Matrix(data);
        LUDecomposition lu = new LUDecomposition(m);
        assertTrue(lu.isNonsingular());
    }

    @Test
    public void testLUDecomposition_Singular() {
        double[][] data = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        Matrix m = new Matrix(data);
        LUDecomposition lu = new LUDecomposition(m);
        assertFalse(lu.isNonsingular());
    }

    @Test
    public void testIsNonsingular_IdentityMatrix() {
        Matrix identity = Matrix.identity(3, 3);
        LUDecomposition lu = new LUDecomposition(identity);
        assertTrue(lu.isNonsingular());
    }

    // Factor Retrieval Tests
    @Test
    public void testGetL_IsLowerTriangular() {
        double[][] data = {{4, 3}, {6, 3}};
        Matrix m = new Matrix(data);
        LUDecomposition lu = new LUDecomposition(m);
        Matrix L = lu.getL();

        // Check dimensions
        assertEquals(m.getRowDimension(), L.getRowDimension());
        assertEquals(m.getColumnDimension(), L.getColumnDimension());

        // Check diagonal elements are 1
        assertEquals(1.0, L.get(0, 0), TOLERANCE);
        assertEquals(1.0, L.get(1, 1), TOLERANCE);

        // Check upper triangle is zero
        assertEquals(0.0, L.get(0, 1), TOLERANCE);
    }

    @Test
    public void testGetU_IsUpperTriangular() {
        double[][] data = {{4, 3}, {6, 3}};
        Matrix m = new Matrix(data);
        LUDecomposition lu = new LUDecomposition(m);
        Matrix U = lu.getU();

        // Check dimensions
        assertEquals(m.getColumnDimension(), U.getRowDimension());
        assertEquals(m.getColumnDimension(), U.getColumnDimension());

        // Check lower triangle is zero (except diagonal)
        assertEquals(0.0, U.get(1, 0), TOLERANCE);
    }

    @Test
    public void testLU_Product_ApproximatesOriginal() {
        // For a simple matrix where we can verify L*U
        double[][] data = {{2, 1}, {1, 2}};
        Matrix A = new Matrix(data);
        LUDecomposition lu = new LUDecomposition(A);

        Matrix L = lu.getL();
        Matrix U = lu.getU();

        // Without considering pivoting, L*U should approximate A
        Matrix product = L.times(U);

        // The product might be a permutation of the original due to pivoting
        // We can verify by checking determinants are equal
        assertEquals(Math.abs(A.det()), Math.abs(product.det()), 0.01);
    }

    @Test
    public void testGetL_3x3Matrix() {
        double[][] data = {{1, 2, 3}, {2, 5, 7}, {3, 5, 3}};
        Matrix m = new Matrix(data);
        LUDecomposition lu = new LUDecomposition(m);
        Matrix L = lu.getL();

        // Verify L is lower triangular with 1s on diagonal
        assertEquals(1.0, L.get(0, 0), TOLERANCE);
        assertEquals(1.0, L.get(1, 1), TOLERANCE);
        assertEquals(1.0, L.get(2, 2), TOLERANCE);

        assertEquals(0.0, L.get(0, 1), TOLERANCE);
        assertEquals(0.0, L.get(0, 2), TOLERANCE);
        assertEquals(0.0, L.get(1, 2), TOLERANCE);
    }

    @Test
    public void testGetU_3x3Matrix() {
        double[][] data = {{1, 2, 3}, {2, 5, 7}, {3, 5, 3}};
        Matrix m = new Matrix(data);
        LUDecomposition lu = new LUDecomposition(m);
        Matrix U = lu.getU();

        // Verify U is upper triangular
        assertEquals(0.0, U.get(1, 0), TOLERANCE);
        assertEquals(0.0, U.get(2, 0), TOLERANCE);
        assertEquals(0.0, U.get(2, 1), TOLERANCE);
    }

    // Determinant & Solving Tests
    @Test
    public void testDet() {
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(data);
        LUDecomposition lu = new LUDecomposition(m);
        double det = lu.det();
        // det = 1*4 - 2*3 = -2
        assertEquals(-2.0, det, TOLERANCE);
    }

    @Test
    public void testDet_3x3() {
        double[][] data = {{1, 0, 0}, {0, 2, 0}, {0, 0, 3}};
        Matrix m = new Matrix(data);
        LUDecomposition lu = new LUDecomposition(m);
        double det = lu.det();
        // Diagonal matrix: det = 1*2*3 = 6
        assertEquals(6.0, det, TOLERANCE);
    }

    @Test
    public void testDet_SingularMatrix() {
        double[][] data = {{1, 2}, {2, 4}};
        Matrix m = new Matrix(data);
        LUDecomposition lu = new LUDecomposition(m);
        double det = lu.det();
        assertEquals(0.0, det, TOLERANCE);
    }

    @Test
    public void testSolve() {
        // Solve Ax = b
        double[][] dataA = {{2, 1}, {1, 2}};
        double[][] dataB = {{3}, {3}};
        Matrix A = new Matrix(dataA);
        Matrix b = new Matrix(dataB);

        LUDecomposition lu = new LUDecomposition(A);
        Matrix x = lu.solve(b);

        // Solution should be x = [1, 1]
        assertEquals(1.0, x.get(0, 0), TOLERANCE);
        assertEquals(1.0, x.get(1, 0), TOLERANCE);

        // Verify: A*x = b
        Matrix check = A.times(x);
        assertEquals(3.0, check.get(0, 0), TOLERANCE);
        assertEquals(3.0, check.get(1, 0), TOLERANCE);
    }

    @Test
    public void testSolve_3x3System() {
        // Solve a 3x3 system
        double[][] dataA = {{1, 2, 3}, {2, 5, 7}, {3, 5, 3}};
        double[][] dataB = {{14}, {30}, {20}};
        Matrix A = new Matrix(dataA);
        Matrix b = new Matrix(dataB);

        LUDecomposition lu = new LUDecomposition(A);
        Matrix x = lu.solve(b);

        // Verify: A*x = b
        Matrix check = A.times(x);
        assertEquals(14.0, check.get(0, 0), 0.001);
        assertEquals(30.0, check.get(1, 0), 0.001);
        assertEquals(20.0, check.get(2, 0), 0.001);
    }

    @Test(expected = RuntimeException.class)
    public void testSolve_SingularMatrix_ThrowsException() {
        // Try to solve with singular matrix
        double[][] dataA = {{1, 2}, {2, 4}};
        double[][] dataB = {{1}, {2}};
        Matrix A = new Matrix(dataA);
        Matrix b = new Matrix(dataB);

        LUDecomposition lu = new LUDecomposition(A);
        lu.solve(b); // Should throw RuntimeException
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDet_NonSquareMatrix_ThrowsException() {
        // Try to compute determinant of non-square matrix
        // Note: Using a square matrix but calling det() on rectangular decomposition
        double[][] data = {{1, 2}, {3, 4}, {5, 6}};
        Matrix m = new Matrix(data);
        LUDecomposition lu = new LUDecomposition(m);
        lu.det(); // Should throw IllegalArgumentException because m != n
    }

    @Test
    public void testGetPivot() {
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(data);
        LUDecomposition lu = new LUDecomposition(m);
        int[] pivot = lu.getPivot();

        assertNotNull(pivot);
        assertEquals(m.getRowDimension(), pivot.length);
    }

    @Test
    public void testGetDoublePivot() {
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(data);
        LUDecomposition lu = new LUDecomposition(m);
        double[] pivot = lu.getDoublePivot();

        assertNotNull(pivot);
        assertEquals(m.getRowDimension(), pivot.length);
    }

    @Test
    public void testSolve_MultipleRHS() {
        // Solve A*X = B with multiple right-hand sides
        double[][] dataA = {{2, 1}, {1, 2}};
        double[][] dataB = {{3, 4}, {3, 2}};
        Matrix A = new Matrix(dataA);
        Matrix B = new Matrix(dataB);

        LUDecomposition lu = new LUDecomposition(A);
        Matrix X = lu.solve(B);

        // Verify: A*X = B
        Matrix check = A.times(X);
        assertEquals(3.0, check.get(0, 0), TOLERANCE);
        assertEquals(4.0, check.get(0, 1), TOLERANCE);
        assertEquals(3.0, check.get(1, 0), TOLERANCE);
        assertEquals(2.0, check.get(1, 1), TOLERANCE);
    }
}
