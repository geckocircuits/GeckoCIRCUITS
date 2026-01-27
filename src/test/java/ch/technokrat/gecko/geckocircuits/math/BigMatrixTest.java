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

public class BigMatrixTest {
    private static final BigDecimal TOLERANCE = new BigDecimal("1e-10");

    // ==================== CONSTRUCTOR TESTS ====================

    @Test
    public void testConstructor_ZeroMatrix_2x3() {
        BigMatrix m = new BigMatrix(2, 3);
        assertEquals(2, m.getRowDimension());
        assertEquals(3, m.getColumnDimension());
        assertEquals(BigDecimal.ZERO, m.getArray()[0][0]);
        assertEquals(BigDecimal.ZERO, m.getArray()[1][2]);
    }

    @Test
    public void testConstructor_ZeroMatrix_1x1() {
        BigMatrix m = new BigMatrix(1, 1);
        assertEquals(1, m.getRowDimension());
        assertEquals(1, m.getColumnDimension());
        assertEquals(BigDecimal.ZERO, m.getArray()[0][0]);
    }

    @Test
    public void testConstructor_ZeroMatrix_5x5() {
        BigMatrix m = new BigMatrix(5, 5);
        assertEquals(5, m.getRowDimension());
        assertEquals(5, m.getColumnDimension());
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                assertEquals(BigDecimal.ZERO, m.getArray()[i][j]);
            }
        }
    }

    @Test
    public void testConstructor_From2DArray() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0")},
            {new BigDecimal("4.0"), new BigDecimal("5.0"), new BigDecimal("6.0")}
        };
        BigMatrix m = new BigMatrix(data);
        assertEquals(2, m.getRowDimension());
        assertEquals(3, m.getColumnDimension());
        assertEquals(new BigDecimal("1.0"), m.getArray()[0][0]);
        assertEquals(new BigDecimal("6.0"), m.getArray()[1][2]);
    }

    @Test
    public void testConstructor_From2DArray_SingleRow() {
        BigDecimal[][] data = {{new BigDecimal("7.0"), new BigDecimal("8.0")}};
        BigMatrix m = new BigMatrix(data);
        assertEquals(1, m.getRowDimension());
        assertEquals(2, m.getColumnDimension());
        assertEquals(new BigDecimal("7.0"), m.getArray()[0][0]);
        assertEquals(new BigDecimal("8.0"), m.getArray()[0][1]);
    }

    @Test
    public void testConstructor_From2DArray_SingleColumn() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0")},
            {new BigDecimal("2.0")},
            {new BigDecimal("3.0")}
        };
        BigMatrix m = new BigMatrix(data);
        assertEquals(3, m.getRowDimension());
        assertEquals(1, m.getColumnDimension());
    }

    @Test
    public void testConstructor_FromPackedArray() {
        double[] vals = {1, 2, 3, 4, 5, 6}; // packed by columns for 2x3 matrix
        BigMatrix m = new BigMatrix(vals, 2);
        assertEquals(2, m.getRowDimension());
        assertEquals(3, m.getColumnDimension());
        assertEquals(1.0, m.getArray()[0][0].doubleValue(), 1e-10);
        assertEquals(2.0, m.getArray()[1][0].doubleValue(), 1e-10);
        assertEquals(3.0, m.getArray()[0][1].doubleValue(), 1e-10);
    }

    @Test
    public void testConstructor_FromPackedArray_1x1() {
        double[] vals = {5.5};
        BigMatrix m = new BigMatrix(vals, 1);
        assertEquals(1, m.getRowDimension());
        assertEquals(1, m.getColumnDimension());
        assertEquals(new BigDecimal("5.5"), m.getArray()[0][0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_From2DArray_InconsistentRowLength() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0")} // Different length - should fail
        };
        new BigMatrix(data);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_FromPackedArray_InvalidLength() {
        double[] vals = {1, 2, 3, 4, 5}; // Not divisible by m=2
        new BigMatrix(vals, 2);
    }

    // ==================== ELEMENT ACCESS TESTS ====================

    @Test
    public void testGetArray() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0"), new BigDecimal("4.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigDecimal[][] array = m.getArray();
        assertEquals(new BigDecimal("1.0"), array[0][0]);
        assertEquals(new BigDecimal("4.0"), array[1][1]);
    }

    @Test
    public void testGetArrayCopy() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0"), new BigDecimal("4.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigDecimal[][] copy = m.getArrayCopy();
        
        // Verify copy has correct values
        assertEquals(new BigDecimal("1.0"), copy[0][0]);
        assertEquals(new BigDecimal("4.0"), copy[1][1]);
        
        // Verify it's a deep copy by modifying original
        data[0][0] = new BigDecimal("99.0");
        assertNotEquals(new BigDecimal("99.0"), m.getArray()[0][0]);
    }

    @Test
    public void testGetColumnPackedCopy() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("3.0")},
            {new BigDecimal("2.0"), new BigDecimal("4.0")}
        };
        BigMatrix m = new BigMatrix(data);
        double[] packed = m.getColumnPackedCopy();
        
        // Packed by columns: [1.0, 2.0, 3.0, 4.0]
        assertEquals(1.0, packed[0], 1e-10);
        assertEquals(2.0, packed[1], 1e-10);
        assertEquals(3.0, packed[2], 1e-10);
        assertEquals(4.0, packed[3], 1e-10);
    }

    // ==================== DIMENSION TESTS ====================

    @Test
    public void testGetRowDimension() {
        BigMatrix m = new BigMatrix(7, 3);
        assertEquals(7, m.getRowDimension());
    }

    @Test
    public void testGetColumnDimension() {
        BigMatrix m = new BigMatrix(3, 9);
        assertEquals(9, m.getColumnDimension());
    }

    @Test
    public void testGetDimensions_NonSquare() {
        BigMatrix m = new BigMatrix(4, 6);
        assertEquals(4, m.getRowDimension());
        assertEquals(6, m.getColumnDimension());
    }

    @Test
    public void testGetDimensions_Square() {
        BigMatrix m = new BigMatrix(5, 5);
        assertEquals(5, m.getRowDimension());
        assertEquals(5, m.getColumnDimension());
    }

    // ==================== NORM CALCULATION TESTS ====================

    @Test
    public void testNorm1_SimpleMatrix() {
        // Column sums: col 0 = 3, col 1 = 7, max = 7
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("2.0"), new BigDecimal("5.0")}
        };
        BigMatrix m = new BigMatrix(data);
        assertEquals(7.0, m.norm1(), 1e-10);
    }

    @Test
    public void testNorm1_IdentityMatrix() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("1.0")}
        };
        BigMatrix m = new BigMatrix(data);
        assertEquals(1.0, m.norm1(), 1e-10);
    }

    @Test
    public void testNorm1_ZeroMatrix() {
        BigMatrix m = new BigMatrix(3, 3);
        assertEquals(0.0, m.norm1(), 1e-10);
    }

    @Test
    public void testNormInf_SimpleMatrix() {
        // Row sums: row 0 = 3, row 1 = 7, max = 7
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("2.0"), new BigDecimal("5.0")}
        };
        BigMatrix m = new BigMatrix(data);
        assertEquals(7.0, m.normInf(), 1e-10);
    }

    @Test
    public void testNormInf_IdentityMatrix() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("1.0")}
        };
        BigMatrix m = new BigMatrix(data);
        assertEquals(1.0, m.normInf(), 1e-10);
    }

    @Test
    public void testNormInf_NegativeValues() {
        BigDecimal[][] data = {
            {new BigDecimal("-1.0"), new BigDecimal("-2.0")},
            {new BigDecimal("-3.0"), new BigDecimal("-4.0")}
        };
        BigMatrix m = new BigMatrix(data);
        // Row sums: row 0 = 3, row 1 = 7, max = 7
        assertEquals(7.0, m.normInf(), 1e-10);
    }

    // ==================== SUBMATRIX EXTRACTION TESTS ====================

    @Test
    public void testGetMatrix_SubmatrixByRows() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0")},
            {new BigDecimal("4.0"), new BigDecimal("5.0"), new BigDecimal("6.0")},
            {new BigDecimal("7.0"), new BigDecimal("8.0"), new BigDecimal("9.0")}
        };
        BigMatrix m = new BigMatrix(data);
        
        // Extract rows [0, 2], columns [0, 1]
        int[] rows = {0, 2};
        BigMatrix sub = m.getMatrix(rows, 0, 1);
        
        assertEquals(2, sub.getRowDimension());
        assertEquals(2, sub.getColumnDimension());
        assertEquals(new BigDecimal("1.0"), sub.getArray()[0][0]);
        assertEquals(new BigDecimal("2.0"), sub.getArray()[0][1]);
        assertEquals(new BigDecimal("7.0"), sub.getArray()[1][0]);
        assertEquals(new BigDecimal("8.0"), sub.getArray()[1][1]);
    }

    @Test
    public void testGetMatrix_SingleRow() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0")},
            {new BigDecimal("4.0"), new BigDecimal("5.0"), new BigDecimal("6.0")}
        };
        BigMatrix m = new BigMatrix(data);
        
        int[] rows = {1};
        BigMatrix sub = m.getMatrix(rows, 0, 2);
        
        assertEquals(1, sub.getRowDimension());
        assertEquals(3, sub.getColumnDimension());
        assertEquals(new BigDecimal("4.0"), sub.getArray()[0][0]);
        assertEquals(new BigDecimal("6.0"), sub.getArray()[0][2]);
    }

    @Test
    public void testGetMatrix_SingleColumn() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0"), new BigDecimal("4.0")},
            {new BigDecimal("5.0"), new BigDecimal("6.0")}
        };
        BigMatrix m = new BigMatrix(data);
        
        int[] rows = {0, 1, 2};
        BigMatrix sub = m.getMatrix(rows, 1, 1);
        
        assertEquals(3, sub.getRowDimension());
        assertEquals(1, sub.getColumnDimension());
        assertEquals(new BigDecimal("2.0"), sub.getArray()[0][0]);
        assertEquals(new BigDecimal("6.0"), sub.getArray()[2][0]);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetMatrix_InvalidRowIndex() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0"), new BigDecimal("4.0")}
        };
        BigMatrix m = new BigMatrix(data);
        
        int[] rows = {0, 5}; // Index 5 is out of bounds
        m.getMatrix(rows, 0, 1);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetMatrix_InvalidColumnIndex() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0"), new BigDecimal("4.0")}
        };
        BigMatrix m = new BigMatrix(data);
        
        int[] rows = {0};
        m.getMatrix(rows, 0, 5); // Column index 5 is out of bounds
    }

    // ==================== SOLVE METHOD TESTS ====================

    @Test
    public void testSolve_2x2System() {
        // Solve: [2 1; 1 3] * [x; y] = [5; 6]
        // Solution: x = 1.8, y = 1.4 approximately
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
        BigMatrix X = A.solve(B);
        
        assertNotNull(X);
        assertEquals(2, X.getRowDimension());
        assertEquals(1, X.getColumnDimension());
        // Verify solution: A * X ≈ B
        BigMatrix result = new BigMatrix(2, 1);
        BigDecimal[][] resultData = result.getArray();
        resultData[0][0] = aData[0][0].multiply(X.getArray()[0][0])
                            .add(aData[0][1].multiply(X.getArray()[1][0]));
        resultData[1][0] = aData[1][0].multiply(X.getArray()[0][0])
                            .add(aData[1][1].multiply(X.getArray()[1][0]));
        
        assertTrue(resultData[0][0].subtract(new BigDecimal("5.0")).abs().doubleValue() < 0.1);
        assertTrue(resultData[1][0].subtract(new BigDecimal("6.0")).abs().doubleValue() < 0.1);
    }

    @Test
    public void testSolve_3x3System() {
        // Solve a simple 3x3 system
        BigDecimal[][] aData = {
            {new BigDecimal("1.0"), new BigDecimal("0.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("2.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("3.0")}
        };
        BigDecimal[][] bData = {
            {new BigDecimal("4.0")},
            {new BigDecimal("6.0")},
            {new BigDecimal("9.0")}
        };
        
        BigMatrix A = new BigMatrix(aData);
        BigMatrix B = new BigMatrix(bData);
        BigMatrix X = A.solve(B);
        
        assertNotNull(X);
        assertEquals(3, X.getRowDimension());
        assertEquals(1, X.getColumnDimension());
        // Solution should be [4, 3, 3]
        assertEquals(4.0, X.getArray()[0][0].doubleValue(), 0.1);
        assertEquals(3.0, X.getArray()[1][0].doubleValue(), 0.1);
        assertEquals(3.0, X.getArray()[2][0].doubleValue(), 0.1);
    }

    @Test
    public void testSolve_NonSquareReturnsNull() {
        // Non-square matrix should return null
        BigDecimal[][] aData = {
            {new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0")},
            {new BigDecimal("4.0"), new BigDecimal("5.0"), new BigDecimal("6.0")}
        };
        BigDecimal[][] bData = {
            {new BigDecimal("7.0")},
            {new BigDecimal("8.0")}
        };
        
        BigMatrix A = new BigMatrix(aData);
        BigMatrix B = new BigMatrix(bData);
        BigMatrix X = A.solve(B);
        
        assertNull(X);
    }

    @Test
    public void testSolve_MultipleRightHandSides() {
        // Solve AX = B where B has multiple columns
        BigDecimal[][] aData = {
            {new BigDecimal("2.0"), new BigDecimal("1.0")},
            {new BigDecimal("1.0"), new BigDecimal("3.0")}
        };
        BigDecimal[][] bData = {
            {new BigDecimal("5.0"), new BigDecimal("2.0")},
            {new BigDecimal("6.0"), new BigDecimal("4.0")}
        };
        
        BigMatrix A = new BigMatrix(aData);
        BigMatrix B = new BigMatrix(bData);
        BigMatrix X = A.solve(B);
        
        assertNotNull(X);
        assertEquals(2, X.getRowDimension());
        assertEquals(2, X.getColumnDimension());
    }

    // ==================== LU DECOMPOSITION TESTS ====================

    @Test
    public void testLU_Decomposition() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0"), new BigDecimal("4.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigLUDecomposition lu = m.lu();
        
        assertNotNull(lu);
    }

    @Test
    public void testResetLUDecomp() {
        BigDecimal[][] data = {
            {new BigDecimal("2.0"), new BigDecimal("1.0")},
            {new BigDecimal("1.0"), new BigDecimal("3.0")}
        };
        BigMatrix A = new BigMatrix(data);
        
        // Create LU decomposition
        BigLUDecomposition lu1 = A.lu();
        assertNotNull(lu1);
        
        // Reset it
        A.ResetLUDecomp();
        
        // Create new one - should work without error
        BigLUDecomposition lu2 = A.lu();
        assertNotNull(lu2);
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    public void testLargeMatrix() {
        // Test with a larger matrix
        BigMatrix m = new BigMatrix(100, 100);
        assertEquals(100, m.getRowDimension());
        assertEquals(100, m.getColumnDimension());
        
        // All elements should be zero
        BigDecimal[][] array = m.getArray();
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                assertEquals(BigDecimal.ZERO, array[i][j]);
            }
        }
    }

    @Test
    public void testMatrixWithVerySmallNumbers() {
        BigDecimal[][] data = {
            {new BigDecimal("0.0000001"), new BigDecimal("0.0000002")},
            {new BigDecimal("0.0000003"), new BigDecimal("0.0000004")}
        };
        BigMatrix m = new BigMatrix(data);
        
        // norm1: max column sum = max(0.0000001+0.0000003, 0.0000002+0.0000004) = 0.0000006
        assertEquals(0.0000006, m.norm1(), 1e-10);
        // normInf: max row sum = max(0.0000001+0.0000002, 0.0000003+0.0000004) = 0.0000007
        assertEquals(0.0000007, m.normInf(), 1e-10);
    }

    @Test
    public void testMatrixWithVeryLargeNumbers() {
        BigDecimal[][] data = {
            {new BigDecimal("1000000.0"), new BigDecimal("2000000.0")},
            {new BigDecimal("3000000.0"), new BigDecimal("4000000.0")}
        };
        BigMatrix m = new BigMatrix(data);
        
        // norm1: max column sum = max(1000000+3000000, 2000000+4000000) = 6000000
        assertEquals(6000000.0, m.norm1(), 1e-1);
        // normInf: max row sum = max(1000000+2000000, 3000000+4000000) = 7000000
        assertEquals(7000000.0, m.normInf(), 1e-1);
    }

    @Test
    public void testPackedArrayWith_AllZeros() {
        double[] vals = {0, 0, 0, 0};
        BigMatrix m = new BigMatrix(vals, 2);
        
        assertEquals(2, m.getRowDimension());
        assertEquals(2, m.getColumnDimension());
        assertEquals(BigDecimal.ZERO, m.getArray()[0][0]);
        assertEquals(BigDecimal.ZERO, m.getArray()[1][1]);
    }

}
