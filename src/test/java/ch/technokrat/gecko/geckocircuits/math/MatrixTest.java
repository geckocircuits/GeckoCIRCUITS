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

public class MatrixTest {
    private static final double TOLERANCE = 1e-10;

    // Constructor Tests
    @Test
    public void testConstructor_ZeroMatrix() {
        Matrix m = new Matrix(3, 4);
        assertEquals(3, m.getRowDimension());
        assertEquals(4, m.getColumnDimension());
        assertEquals(0.0, m.get(0, 0), TOLERANCE);
        assertEquals(0.0, m.get(2, 3), TOLERANCE);
    }

    @Test
    public void testConstructor_From2DArray() {
        double[][] data = {{1, 2, 3}, {4, 5, 6}};
        Matrix m = new Matrix(data);
        assertEquals(2, m.getRowDimension());
        assertEquals(3, m.getColumnDimension());
        assertEquals(1.0, m.get(0, 0), TOLERANCE);
        assertEquals(6.0, m.get(1, 2), TOLERANCE);
    }

    @Test
    public void testConstructor_ConstantMatrix() {
        Matrix m = new Matrix(2, 3, 5.0);
        assertEquals(2, m.getRowDimension());
        assertEquals(3, m.getColumnDimension());
        assertEquals(5.0, m.get(0, 0), TOLERANCE);
        assertEquals(5.0, m.get(1, 2), TOLERANCE);
    }

    @Test
    public void testConstructor_FromPackedArray() {
        double[] vals = {1, 2, 3, 4, 5, 6}; // packed by columns
        Matrix m = new Matrix(vals, 2);
        assertEquals(2, m.getRowDimension());
        assertEquals(3, m.getColumnDimension());
        assertEquals(1.0, m.get(0, 0), TOLERANCE);
        assertEquals(2.0, m.get(1, 0), TOLERANCE);
        assertEquals(3.0, m.get(0, 1), TOLERANCE);
        assertEquals(4.0, m.get(1, 1), TOLERANCE);
    }

    // Basic Operations Tests
    @Test
    public void testGetSet() {
        Matrix m = new Matrix(3, 3);
        m.set(1, 2, 7.5);
        assertEquals(7.5, m.get(1, 2), TOLERANCE);
    }

    @Test
    public void testCopy() {
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m1 = new Matrix(data);
        Matrix m2 = m1.copy();
        assertEquals(m1.get(0, 0), m2.get(0, 0), TOLERANCE);
        assertEquals(m1.get(1, 1), m2.get(1, 1), TOLERANCE);

        // Verify it's a deep copy
        m2.set(0, 0, 99);
        assertNotEquals(m1.get(0, 0), m2.get(0, 0), TOLERANCE);
    }

    @Test
    public void testGetArray() {
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(data);
        double[][] array = m.getArray();
        assertEquals(1.0, array[0][0], TOLERANCE);
        assertEquals(4.0, array[1][1], TOLERANCE);
    }

    @Test
    public void testGetArrayCopy() {
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(data);
        double[][] copy = m.getArrayCopy();
        assertEquals(1.0, copy[0][0], TOLERANCE);

        // Verify it's a copy
        copy[0][0] = 99;
        assertEquals(1.0, m.get(0, 0), TOLERANCE);
    }

    // Arithmetic Operations Tests
    @Test
    public void testPlus() {
        double[][] data1 = {{1, 2}, {3, 4}};
        double[][] data2 = {{5, 6}, {7, 8}};
        Matrix m1 = new Matrix(data1);
        Matrix m2 = new Matrix(data2);
        Matrix result = m1.plus(m2);

        assertEquals(6.0, result.get(0, 0), TOLERANCE);
        assertEquals(8.0, result.get(0, 1), TOLERANCE);
        assertEquals(10.0, result.get(1, 0), TOLERANCE);
        assertEquals(12.0, result.get(1, 1), TOLERANCE);
    }

    @Test
    public void testMinus() {
        double[][] data1 = {{5, 6}, {7, 8}};
        double[][] data2 = {{1, 2}, {3, 4}};
        Matrix m1 = new Matrix(data1);
        Matrix m2 = new Matrix(data2);
        Matrix result = m1.minus(m2);

        assertEquals(4.0, result.get(0, 0), TOLERANCE);
        assertEquals(4.0, result.get(0, 1), TOLERANCE);
        assertEquals(4.0, result.get(1, 0), TOLERANCE);
        assertEquals(4.0, result.get(1, 1), TOLERANCE);
    }

    @Test
    public void testTimesScalar() {
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(data);
        Matrix result = m.times(2.0);

        assertEquals(2.0, result.get(0, 0), TOLERANCE);
        assertEquals(4.0, result.get(0, 1), TOLERANCE);
        assertEquals(6.0, result.get(1, 0), TOLERANCE);
        assertEquals(8.0, result.get(1, 1), TOLERANCE);
    }

    @Test
    public void testTimesMatrix() {
        double[][] data1 = {{1, 2}, {3, 4}};
        double[][] data2 = {{2, 0}, {1, 2}};
        Matrix m1 = new Matrix(data1);
        Matrix m2 = new Matrix(data2);
        Matrix result = m1.times(m2);

        // [[1,2],[3,4]] * [[2,0],[1,2]] = [[4,4],[10,8]]
        assertEquals(4.0, result.get(0, 0), TOLERANCE);
        assertEquals(4.0, result.get(0, 1), TOLERANCE);
        assertEquals(10.0, result.get(1, 0), TOLERANCE);
        assertEquals(8.0, result.get(1, 1), TOLERANCE);
    }

    @Test
    public void testTranspose() {
        double[][] data = {{1, 2, 3}, {4, 5, 6}};
        Matrix m = new Matrix(data);
        Matrix result = m.transpose();

        assertEquals(3, result.getRowDimension());
        assertEquals(2, result.getColumnDimension());
        assertEquals(1.0, result.get(0, 0), TOLERANCE);
        assertEquals(4.0, result.get(0, 1), TOLERANCE);
        assertEquals(2.0, result.get(1, 0), TOLERANCE);
        assertEquals(5.0, result.get(1, 1), TOLERANCE);
        assertEquals(3.0, result.get(2, 0), TOLERANCE);
        assertEquals(6.0, result.get(2, 1), TOLERANCE);
    }

    // Advanced Linear Algebra Tests
    @Test
    public void testDeterminant_2x2() {
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(data);
        double det = m.det();
        // det = 1*4 - 2*3 = 4 - 6 = -2
        assertEquals(-2.0, det, TOLERANCE);
    }

    @Test
    public void testDeterminant_3x3() {
        double[][] data = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        Matrix m = new Matrix(data);
        double det = m.det();
        // This is a singular matrix
        assertEquals(0.0, det, TOLERANCE);
    }

    @Test
    public void testDeterminant_Identity() {
        Matrix identity = Matrix.identity(3, 3);
        double det = identity.det();
        assertEquals(1.0, det, TOLERANCE);
    }

    @Test
    public void testInverse_2x2() {
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(data);
        Matrix inv = m.inverse();

        // Inverse of [[1,2],[3,4]] is [[-2,1],[1.5,-0.5]]
        assertEquals(-2.0, inv.get(0, 0), TOLERANCE);
        assertEquals(1.0, inv.get(0, 1), TOLERANCE);
        assertEquals(1.5, inv.get(1, 0), TOLERANCE);
        assertEquals(-0.5, inv.get(1, 1), TOLERANCE);
    }

    @Test
    public void testInverse_TimesOriginal_IsIdentity() {
        double[][] data = {{2, 1}, {1, 2}};
        Matrix m = new Matrix(data);
        Matrix inv = m.inverse();
        Matrix result = m.times(inv);

        // Result should be identity matrix
        assertEquals(1.0, result.get(0, 0), 1e-9);
        assertEquals(0.0, result.get(0, 1), 1e-9);
        assertEquals(0.0, result.get(1, 0), 1e-9);
        assertEquals(1.0, result.get(1, 1), 1e-9);
    }

    @Test
    public void testTrace() {
        double[][] data = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        Matrix m = new Matrix(data);
        double trace = m.trace();
        // trace = 1 + 5 + 9 = 15
        assertEquals(15.0, trace, TOLERANCE);
    }

    @Test
    public void testSolve() {
        // Solve Ax = b
        double[][] dataA = {{2, 1}, {1, 2}};
        double[][] dataB = {{3}, {3}};
        Matrix A = new Matrix(dataA);
        Matrix b = new Matrix(dataB);
        Matrix x = A.solve(b);

        // Solution should be x = [1, 1]
        assertEquals(1.0, x.get(0, 0), TOLERANCE);
        assertEquals(1.0, x.get(1, 0), TOLERANCE);

        // Verify: A*x = b
        Matrix check = A.times(x);
        assertEquals(3.0, check.get(0, 0), TOLERANCE);
        assertEquals(3.0, check.get(1, 0), TOLERANCE);
    }

    @Test
    public void testIdentity() {
        Matrix identity = Matrix.identity(3, 3);
        assertEquals(3, identity.getRowDimension());
        assertEquals(3, identity.getColumnDimension());
        assertEquals(1.0, identity.get(0, 0), TOLERANCE);
        assertEquals(1.0, identity.get(1, 1), TOLERANCE);
        assertEquals(1.0, identity.get(2, 2), TOLERANCE);
        assertEquals(0.0, identity.get(0, 1), TOLERANCE);
        assertEquals(0.0, identity.get(1, 0), TOLERANCE);
    }

    // Norms Tests
    @Test
    public void testNorm1() {
        double[][] data = {{1, -2, 3}, {-4, 5, -6}};
        Matrix m = new Matrix(data);
        double norm = m.norm1();
        // norm1 = max column sum = max(|1|+|-4|, |-2|+|5|, |3|+|-6|) = max(5, 7, 9) = 9
        assertEquals(9.0, norm, TOLERANCE);
    }

    @Test
    public void testNormInf() {
        double[][] data = {{1, -2, 3}, {-4, 5, -6}};
        Matrix m = new Matrix(data);
        double norm = m.normInf();
        // normInf = max row sum = max(|1|+|-2|+|3|, |-4|+|5|+|-6|) = max(6, 15) = 15
        assertEquals(15.0, norm, TOLERANCE);
    }

    // Additional Tests
    @Test
    public void testPlusEquals() {
        double[][] data1 = {{1, 2}, {3, 4}};
        double[][] data2 = {{1, 1}, {1, 1}};
        Matrix m1 = new Matrix(data1);
        Matrix m2 = new Matrix(data2);
        Matrix result = m1.plusEquals(m2);

        assertEquals(2.0, result.get(0, 0), TOLERANCE);
        assertEquals(3.0, result.get(0, 1), TOLERANCE);
        assertEquals(4.0, result.get(1, 0), TOLERANCE);
        assertEquals(5.0, result.get(1, 1), TOLERANCE);
        // Should modify original
        assertEquals(2.0, m1.get(0, 0), TOLERANCE);
    }

    @Test
    public void testMinusEquals() {
        double[][] data1 = {{5, 6}, {7, 8}};
        double[][] data2 = {{1, 2}, {3, 4}};
        Matrix m1 = new Matrix(data1);
        Matrix m2 = new Matrix(data2);
        Matrix result = m1.minusEquals(m2);

        assertEquals(4.0, result.get(0, 0), TOLERANCE);
        assertEquals(4.0, result.get(0, 1), TOLERANCE);
        // Should modify original
        assertEquals(4.0, m1.get(0, 0), TOLERANCE);
    }

    @Test
    public void testTimesEquals() {
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(data);
        Matrix result = m.timesEquals(3.0);

        assertEquals(3.0, result.get(0, 0), TOLERANCE);
        assertEquals(6.0, result.get(0, 1), TOLERANCE);
        // Should modify original
        assertEquals(3.0, m.get(0, 0), TOLERANCE);
    }

    @Test
    public void testGetMatrix_Submatrix() {
        double[][] data = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        Matrix m = new Matrix(data);
        Matrix sub = m.getMatrix(0, 1, 0, 1);

        assertEquals(2, sub.getRowDimension());
        assertEquals(2, sub.getColumnDimension());
        assertEquals(1.0, sub.get(0, 0), TOLERANCE);
        assertEquals(2.0, sub.get(0, 1), TOLERANCE);
        assertEquals(4.0, sub.get(1, 0), TOLERANCE);
        assertEquals(5.0, sub.get(1, 1), TOLERANCE);
    }

    @Test
    public void testSetMatrix_Submatrix() {
        Matrix m = new Matrix(3, 3);
        double[][] subData = {{1, 2}, {3, 4}};
        Matrix sub = new Matrix(subData);
        m.setMatrix(0, 1, 0, 1, sub);

        assertEquals(1.0, m.get(0, 0), TOLERANCE);
        assertEquals(2.0, m.get(0, 1), TOLERANCE);
        assertEquals(3.0, m.get(1, 0), TOLERANCE);
        assertEquals(4.0, m.get(1, 1), TOLERANCE);
        assertEquals(0.0, m.get(2, 2), TOLERANCE);
    }

    @Test
    public void testRandom() {
        Matrix m = Matrix.random(3, 3);
        assertEquals(3, m.getRowDimension());
        assertEquals(3, m.getColumnDimension());
        // All elements should be between 0 and 1
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                double val = m.get(i, j);
                assertTrue(val >= 0.0 && val < 1.0);
            }
        }
    }

    @Test
    public void testConstructWithCopy() {
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = Matrix.constructWithCopy(data);

        assertEquals(1.0, m.get(0, 0), TOLERANCE);
        assertEquals(4.0, m.get(1, 1), TOLERANCE);

        // Verify it's a copy
        data[0][0] = 99;
        assertEquals(1.0, m.get(0, 0), TOLERANCE);
    }

    @Test
    public void testArrayTimesEquals() {
        double[][] data1 = {{1, 2}, {3, 4}};
        double[][] data2 = {{2, 2}, {3, 3}};
        Matrix m1 = new Matrix(data1);
        Matrix m2 = new Matrix(data2);
        Matrix result = m1.arrayTimesEquals(m2);

        // Element-wise multiplication
        assertEquals(2.0, result.get(0, 0), TOLERANCE);
        assertEquals(4.0, result.get(0, 1), TOLERANCE);
        assertEquals(9.0, result.get(1, 0), TOLERANCE);
        assertEquals(12.0, result.get(1, 1), TOLERANCE);
    }

    @Test
    public void testArrayRightDivideEquals() {
        double[][] data1 = {{4, 6}, {8, 10}};
        double[][] data2 = {{2, 2}, {4, 5}};
        Matrix m1 = new Matrix(data1);
        Matrix m2 = new Matrix(data2);
        Matrix result = m1.arrayRightDivideEquals(m2);

        // Element-wise division
        assertEquals(2.0, result.get(0, 0), TOLERANCE);
        assertEquals(3.0, result.get(0, 1), TOLERANCE);
        assertEquals(2.0, result.get(1, 0), TOLERANCE);
        assertEquals(2.0, result.get(1, 1), TOLERANCE);
    }

    // ==================== ADDITIONAL EDGE CASE TESTS ====================

    @Test
    public void testToString_NonNull() {
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(data);
        String str = m.toString();
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    @Test
    public void testToString_ContainsData() {
        double[][] data = {{1.5, 2.5}};
        Matrix m = new Matrix(data);
        String str = m.toString();
        // Should contain some representation (just verify non-null/non-empty)
        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    @Test
    public void testInverse_3x3() {
        double[][] data = {{1, 0, 1}, {2, 1, 0}, {0, 1, 1}};
        Matrix m = new Matrix(data);
        Matrix inv = m.inverse();

        // Verify m * inv = I
        Matrix product = m.times(inv);
        Matrix identity = Matrix.identity(3, 3);
        
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals(identity.get(i, j), product.get(i, j), 1e-9);
            }
        }
    }

    @Test
    public void testTrace_2x2() {
        double[][] data = {{2, 3}, {5, 7}};
        Matrix m = new Matrix(data);
        double trace = m.trace();
        assertEquals(9.0, trace, TOLERANCE);
    }

    @Test
    public void testTrace_DiagonalMatrix() {
        double[][] data = {{1, 0, 0}, {0, 2, 0}, {0, 0, 3}};
        Matrix m = new Matrix(data);
        double trace = m.trace();
        assertEquals(6.0, trace, TOLERANCE);
    }

    @Test
    public void testTrace_Zero() {
        Matrix zero = new Matrix(3, 3);
        double trace = zero.trace();
        assertEquals(0.0, trace, TOLERANCE);
    }

    @Test
    public void testArrayTimes() {
        double[][] data1 = {{1, 2}, {3, 4}};
        double[][] data2 = {{2, 2}, {3, 3}};
        Matrix m1 = new Matrix(data1);
        Matrix m2 = new Matrix(data2);
        Matrix result = m1.arrayTimes(m2);

        assertEquals(2.0, result.get(0, 0), TOLERANCE);
        assertEquals(4.0, result.get(0, 1), TOLERANCE);
        assertEquals(9.0, result.get(1, 0), TOLERANCE);
        assertEquals(12.0, result.get(1, 1), TOLERANCE);
    }

    @Test
    public void testArrayRightDivide() {
        double[][] data1 = {{4, 6}, {8, 10}};
        double[][] data2 = {{2, 2}, {4, 5}};
        Matrix m1 = new Matrix(data1);
        Matrix m2 = new Matrix(data2);
        Matrix result = m1.arrayRightDivide(m2);

        assertEquals(2.0, result.get(0, 0), TOLERANCE);
        assertEquals(3.0, result.get(0, 1), TOLERANCE);
        assertEquals(2.0, result.get(1, 0), TOLERANCE);
        assertEquals(2.0, result.get(1, 1), TOLERANCE);
    }

    @Test
    public void testArrayLeftDivide() {
        double[][] data1 = {{2, 2}, {4, 5}};
        double[][] data2 = {{4, 6}, {8, 10}};
        Matrix m1 = new Matrix(data1);
        Matrix m2 = new Matrix(data2);
        Matrix result = m1.arrayLeftDivide(m2);

        // Element-wise: data2 / data1
        assertEquals(2.0, result.get(0, 0), TOLERANCE);
        assertEquals(3.0, result.get(0, 1), TOLERANCE);
    }

    @Test
    public void testTimes_ScalarMultiplication() {
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(data);
        Matrix result = m.times(2.0);

        assertEquals(2.0, result.get(0, 0), TOLERANCE);
        assertEquals(4.0, result.get(0, 1), TOLERANCE);
        assertEquals(6.0, result.get(1, 0), TOLERANCE);
        assertEquals(8.0, result.get(1, 1), TOLERANCE);
    }

    @Test
    public void testGetMatrix_ByRowColumnIndices() {
        double[][] data = {{1, 2, 3, 4}, {5, 6, 7, 8}, {9, 10, 11, 12}};
        Matrix m = new Matrix(data);
        int[] rows = {0, 2};
        int[] cols = {1, 3};
        Matrix sub = m.getMatrix(rows, cols);

        assertEquals(2, sub.getRowDimension());
        assertEquals(2, sub.getColumnDimension());
        assertEquals(2.0, sub.get(0, 0), TOLERANCE);
        assertEquals(4.0, sub.get(0, 1), TOLERANCE);
        assertEquals(10.0, sub.get(1, 0), TOLERANCE);
        assertEquals(12.0, sub.get(1, 1), TOLERANCE);
    }

    @Test
    public void testGetMatrix_ByRowIndices() {
        double[][] data = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        Matrix m = new Matrix(data);
        int[] rows = {1};
        Matrix sub = m.getMatrix(rows, 0, 2);

        assertEquals(1, sub.getRowDimension());
        assertEquals(3, sub.getColumnDimension());
        assertEquals(4.0, sub.get(0, 0), TOLERANCE);
        assertEquals(5.0, sub.get(0, 1), TOLERANCE);
        assertEquals(6.0, sub.get(0, 2), TOLERANCE);
    }

    @Test
    public void testGetMatrixByRowRange() {
        double[][] data = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
        Matrix m = new Matrix(data);
        Matrix sub = m.getMatrix(0, 1, 0, 2);

        assertEquals(2, sub.getRowDimension());
        assertEquals(3, sub.getColumnDimension());
    }

    @Test
    public void testGetRowPackedCopy() {
        double[][] data = {{1, 2}, {3, 4}, {5, 6}};
        Matrix m = new Matrix(data);
        double[] packed = m.getRowPackedCopy();

        assertEquals(6, packed.length);
        assertEquals(1.0, packed[0], TOLERANCE);
        assertEquals(2.0, packed[1], TOLERANCE);
        assertEquals(3.0, packed[2], TOLERANCE);
    }

    @Test
    public void testGetColumnPackedCopy() {
        double[][] data = {{1, 2}, {3, 4}, {5, 6}};
        Matrix m = new Matrix(data);
        double[] packed = m.getColumnPackedCopy();

        assertEquals(6, packed.length);
        assertEquals(1.0, packed[0], TOLERANCE);
        assertEquals(3.0, packed[1], TOLERANCE);
        assertEquals(5.0, packed[2], TOLERANCE);
    }

    @Test
    public void testDeterminant_NonSquareThrowsException() {
        double[][] data = {{1, 2, 3}, {4, 5, 6}};
        Matrix m = new Matrix(data);
        
        // Non-square matrix should throw exception
        try {
            m.det();
            fail("Should throw exception for non-square matrix");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testDeterminant_1x1() {
        double[][] data = {{5}};
        Matrix m = new Matrix(data);
        double det = m.det();
        assertEquals(5.0, det, TOLERANCE);
    }
}
