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
import java.math.BigDecimal;

public class BigLUDecompositionTest {
    private static final double TOLERANCE = 1e-8;

    // ==================== DECOMPOSITION TESTS ====================

    @Test
    public void testLUDecomposition_2x2Matrix() {
        BigDecimal[][] data = {
            {new BigDecimal("2.0"), new BigDecimal("1.0")},
            {new BigDecimal("1.0"), new BigDecimal("2.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigLUDecomposition lu = new BigLUDecomposition(m);
        
        assertNotNull(lu);
        assertTrue(lu.isNonsingular());
    }

    @Test
    public void testLUDecomposition_3x3Matrix() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0")},
            {new BigDecimal("2.0"), new BigDecimal("5.0"), new BigDecimal("7.0")},
            {new BigDecimal("3.0"), new BigDecimal("5.0"), new BigDecimal("3.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigLUDecomposition lu = new BigLUDecomposition(m);
        
        assertNotNull(lu);
        assertTrue(lu.isNonsingular());
    }

    @Test
    public void testLUDecomposition_IdentityMatrix() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("1.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigLUDecomposition lu = new BigLUDecomposition(m);
        
        assertNotNull(lu);
        assertTrue(lu.isNonsingular());
    }

    @Test
    public void testLUDecomposition_DiagonalMatrix() {
        BigDecimal[][] data = {
            {new BigDecimal("2.0"), new BigDecimal("0.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("3.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("4.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigLUDecomposition lu = new BigLUDecomposition(m);
        
        assertNotNull(lu);
        assertTrue(lu.isNonsingular());
    }

    // ==================== SINGULARITY TESTS ====================

    @Test
    public void testIsNonsingular_NonSingularMatrix() {
        BigDecimal[][] data = {
            {new BigDecimal("2.0"), new BigDecimal("1.0")},
            {new BigDecimal("1.0"), new BigDecimal("2.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigLUDecomposition lu = new BigLUDecomposition(m);
        
        assertTrue(lu.isNonsingular());
    }

    @Test
    public void testIsNonsingular_SingularMatrix() {
        // Singular matrix: rows are linearly dependent
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0")},
            {new BigDecimal("2.0"), new BigDecimal("4.0"), new BigDecimal("6.0")},
            {new BigDecimal("3.0"), new BigDecimal("6.0"), new BigDecimal("9.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigLUDecomposition lu = new BigLUDecomposition(m);
        
        assertFalse(lu.isNonsingular());
    }

    @Test
    public void testIsNonsingular_ZeroRow() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("0.0"), new BigDecimal("0.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigLUDecomposition lu = new BigLUDecomposition(m);
        
        assertFalse(lu.isNonsingular());
    }

    // ==================== PIVOT VECTOR TESTS ====================

    @Test
    public void testGetPivot_2x2Matrix() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0"), new BigDecimal("4.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigLUDecomposition lu = new BigLUDecomposition(m);
        
        int[] piv = lu.getPivot();
        assertNotNull(piv);
        assertEquals(2, piv.length);
        
        // Pivot should be a permutation of [0, 1]
        boolean contains0 = false, contains1 = false;
        for (int p : piv) {
            if (p == 0) contains0 = true;
            if (p == 1) contains1 = true;
        }
        assertTrue(contains0);
        assertTrue(contains1);
    }

    @Test
    public void testGetPivot_3x3Matrix() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0")},
            {new BigDecimal("4.0"), new BigDecimal("5.0"), new BigDecimal("6.0")},
            {new BigDecimal("7.0"), new BigDecimal("8.0"), new BigDecimal("10.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigLUDecomposition lu = new BigLUDecomposition(m);
        
        int[] piv = lu.getPivot();
        assertNotNull(piv);
        assertEquals(3, piv.length);
    }

    @Test
    public void testGetDoublePivot_2x2Matrix() {
        BigDecimal[][] data = {
            {new BigDecimal("2.0"), new BigDecimal("1.0")},
            {new BigDecimal("1.0"), new BigDecimal("3.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigLUDecomposition lu = new BigLUDecomposition(m);
        
        double[] pivDouble = lu.getDoublePivot();
        assertNotNull(pivDouble);
        assertEquals(2, pivDouble.length);
        
        // Should be double version of getPivot
        int[] pivInt = lu.getPivot();
        for (int i = 0; i < pivInt.length; i++) {
            assertEquals(pivInt[i], (int) pivDouble[i]);
        }
    }

    // ==================== SOLVE TESTS ====================

    @Test
    public void testSolve_2x2DiagonalSystem() {
        // Solve: [2 0; 0 3] * [x; y] = [4; 6]
        // Solution: [2; 2]
        BigDecimal[][] aData = {
            {new BigDecimal("2.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("3.0")}
        };
        BigDecimal[][] bData = {
            {new BigDecimal("4.0")},
            {new BigDecimal("6.0")}
        };
        
        BigMatrix A = new BigMatrix(aData);
        BigMatrix B = new BigMatrix(bData);
        BigLUDecomposition lu = new BigLUDecomposition(A);
        BigMatrix X = lu.solve(B);
        
        assertNotNull(X);
        assertEquals(2, X.getRowDimension());
        assertEquals(1, X.getColumnDimension());
        assertEquals(2.0, X.getArray()[0][0].doubleValue(), TOLERANCE);
        assertEquals(2.0, X.getArray()[1][0].doubleValue(), TOLERANCE);
    }

    @Test
    public void testSolve_2x2GeneralSystem() {
        // Solve: [2 1; 1 3] * [x; y] = [5; 6]
        BigDecimal[][] aData = {
            {new BigDecimal("2.0"), new BigDecimal("1.0")},
            {new BigDecimal("1.0"), new BigDecimal("3.0")}
        };
        BigDecimal[][] bData = {
            {new BigDecimal("5.0")},
            {new BigDecimal("6.0")}
        };
        
        BigMatrix A = new BigMatrix(aData);
        BigMatrix B = new BigMatrix(bData);
        BigLUDecomposition lu = new BigLUDecomposition(A);
        BigMatrix X = lu.solve(B);
        
        assertNotNull(X);
        assertEquals(2, X.getRowDimension());
        assertEquals(1, X.getColumnDimension());
    }

    @Test
    public void testSolve_3x3System() {
        // Solve: [1 0 0; 0 2 0; 0 0 3] * [x; y; z] = [1; 4; 9]
        // Solution: [1; 2; 3]
        BigDecimal[][] aData = {
            {new BigDecimal("1.0"), new BigDecimal("0.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("2.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("3.0")}
        };
        BigDecimal[][] bData = {
            {new BigDecimal("1.0")},
            {new BigDecimal("4.0")},
            {new BigDecimal("9.0")}
        };
        
        BigMatrix A = new BigMatrix(aData);
        BigMatrix B = new BigMatrix(bData);
        BigLUDecomposition lu = new BigLUDecomposition(A);
        BigMatrix X = lu.solve(B);
        
        assertNotNull(X);
        assertEquals(3, X.getRowDimension());
        assertEquals(1, X.getColumnDimension());
        assertEquals(1.0, X.getArray()[0][0].doubleValue(), TOLERANCE);
        assertEquals(2.0, X.getArray()[1][0].doubleValue(), TOLERANCE);
        assertEquals(3.0, X.getArray()[2][0].doubleValue(), TOLERANCE);
    }

    @Test
    public void testSolve_MultipleRightHandSides() {
        // Solve AX = B where B has 2 columns
        BigDecimal[][] aData = {
            {new BigDecimal("2.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("3.0")}
        };
        BigDecimal[][] bData = {
            {new BigDecimal("4.0"), new BigDecimal("6.0")},
            {new BigDecimal("6.0"), new BigDecimal("9.0")}
        };
        
        BigMatrix A = new BigMatrix(aData);
        BigMatrix B = new BigMatrix(bData);
        BigLUDecomposition lu = new BigLUDecomposition(A);
        BigMatrix X = lu.solve(B);
        
        assertNotNull(X);
        assertEquals(2, X.getRowDimension());
        assertEquals(2, X.getColumnDimension());
    }

    @Test(expected = RuntimeException.class)
    public void testSolve_SingularMatrix() {
        // Singular matrix should throw exception
        BigDecimal[][] aData = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("2.0"), new BigDecimal("4.0")}
        };
        BigDecimal[][] bData = {
            {new BigDecimal("1.0")},
            {new BigDecimal("2.0")}
        };
        
        BigMatrix A = new BigMatrix(aData);
        BigMatrix B = new BigMatrix(bData);
        BigLUDecomposition lu = new BigLUDecomposition(A);
        lu.solve(B);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSolve_DimensionMismatch() {
        // B must have same number of rows as A
        BigDecimal[][] aData = {
            {new BigDecimal("2.0"), new BigDecimal("1.0")},
            {new BigDecimal("1.0"), new BigDecimal("3.0")}
        };
        BigDecimal[][] bData = {
            {new BigDecimal("5.0")}
            // Only 1 row, but A has 2 rows
        };
        
        BigMatrix A = new BigMatrix(aData);
        BigMatrix B = new BigMatrix(bData);
        BigLUDecomposition lu = new BigLUDecomposition(A);
        lu.solve(B);
    }

    // ==================== PRECISION TESTS ====================

    @Test
    public void testLUDecomposition_HighPrecision() {
        // Test with high-precision BigDecimal numbers
        BigDecimal[][] data = {
            {new BigDecimal("1.23456789012345"), new BigDecimal("2.34567890123456")},
            {new BigDecimal("3.45678901234567"), new BigDecimal("4.56789012345678")}
        };
        BigMatrix m = new BigMatrix(data);
        BigLUDecomposition lu = new BigLUDecomposition(m);
        
        assertNotNull(lu);
        assertTrue(lu.isNonsingular());
    }

    @Test
    public void testSolve_SmallNumbers() {
        // Test with very small numbers
        BigDecimal[][] aData = {
            {new BigDecimal("0.000001"), new BigDecimal("0.000002")},
            {new BigDecimal("0.000003"), new BigDecimal("0.000004")}
        };
        BigDecimal[][] bData = {
            {new BigDecimal("0.000005")},
            {new BigDecimal("0.000006")}
        };
        
        BigMatrix A = new BigMatrix(aData);
        BigMatrix B = new BigMatrix(bData);
        BigLUDecomposition lu = new BigLUDecomposition(A);
        BigMatrix X = lu.solve(B);
        
        assertNotNull(X);
        assertEquals(2, X.getRowDimension());
    }

    @Test
    public void testSolve_LargeNumbers() {
        // Test with very large numbers
        BigDecimal[][] aData = {
            {new BigDecimal("1000000.0"), new BigDecimal("2000000.0")},
            {new BigDecimal("3000000.0"), new BigDecimal("4000000.0")}
        };
        BigDecimal[][] bData = {
            {new BigDecimal("5000000.0")},
            {new BigDecimal("6000000.0")}
        };
        
        BigMatrix A = new BigMatrix(aData);
        BigMatrix B = new BigMatrix(bData);
        BigLUDecomposition lu = new BigLUDecomposition(A);
        BigMatrix X = lu.solve(B);
        
        assertNotNull(X);
        assertEquals(2, X.getRowDimension());
    }

    // ==================== NUMERICAL VALIDATION TESTS ====================

    @Test
    public void testSolve_VerifyAccuracy() {
        // Verify A*X = B approximately
        BigDecimal[][] aData = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0"), new BigDecimal("4.0")}
        };
        BigDecimal[][] bData = {
            {new BigDecimal("5.0")},
            {new BigDecimal("11.0")}
        };
        
        BigMatrix A = new BigMatrix(aData);
        BigMatrix B = new BigMatrix(bData);
        BigLUDecomposition lu = new BigLUDecomposition(A);
        BigMatrix X = lu.solve(B);
        
        // Verify solution: compute A*X
        BigDecimal[] result = new BigDecimal[2];
        BigDecimal[][] aArray = A.getArray();
        BigDecimal[][] xArray = X.getArray();
        
        result[0] = aArray[0][0].multiply(xArray[0][0])
                    .add(aArray[0][1].multiply(xArray[1][0]));
        result[1] = aArray[1][0].multiply(xArray[0][0])
                    .add(aArray[1][1].multiply(xArray[1][0]));
        
        // Compare with B
        assertTrue(result[0].subtract(new BigDecimal("5.0")).abs().doubleValue() < TOLERANCE);
        assertTrue(result[1].subtract(new BigDecimal("11.0")).abs().doubleValue() < TOLERANCE);
    }

    @Test
    public void testLUDecomposition_4x4Matrix() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0"), new BigDecimal("4.0")},
            {new BigDecimal("5.0"), new BigDecimal("6.0"), new BigDecimal("7.0"), new BigDecimal("8.0")},
            {new BigDecimal("9.0"), new BigDecimal("10.0"), new BigDecimal("11.0"), new BigDecimal("12.0")},
            {new BigDecimal("13.0"), new BigDecimal("14.0"), new BigDecimal("15.0"), new BigDecimal("16.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigLUDecomposition lu = new BigLUDecomposition(m);
        
        assertNotNull(lu);
        assertFalse(lu.isNonsingular()); // This is a singular matrix (rank < 4)
    }

    @Test
    public void testLUDecomposition_4x4NonsingularMatrix() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("2.0"), new BigDecimal("0.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("3.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("4.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigLUDecomposition lu = new BigLUDecomposition(m);
        
        assertNotNull(lu);
        assertTrue(lu.isNonsingular());
    }

}
