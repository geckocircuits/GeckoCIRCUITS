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
package gecko.core.math;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.io.PrintWriter;

class BigMatrixTest {
    private static final BigDecimal TOLERANCE = new BigDecimal("1e-10");

    // ==================== CONSTRUCTOR TESTS ====================

    @Test
    void testConstructor_ZeroMatrix_2x3() {
        BigMatrix m = new BigMatrix(2, 3);
        assertEquals(2, m.getRowDimension());
        assertEquals(3, m.getColumnDimension());
        assertEquals(BigDecimal.ZERO, m.getArray()[0][0]);
        assertEquals(BigDecimal.ZERO, m.getArray()[1][2]);
    }

    @Test
    void testConstructor_ZeroMatrix_1x1() {
        BigMatrix m = new BigMatrix(1, 1);
        assertEquals(1, m.getRowDimension());
        assertEquals(1, m.getColumnDimension());
        assertEquals(BigDecimal.ZERO, m.getArray()[0][0]);
    }

    @Test
    void testConstructor_ZeroMatrix_5x5() {
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
    void testConstructor_From2DArray() {
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
    void testConstructor_From2DArray_SingleRow() {
        BigDecimal[][] data = {{new BigDecimal("7.0"), new BigDecimal("8.0")}};
        BigMatrix m = new BigMatrix(data);
        assertEquals(1, m.getRowDimension());
        assertEquals(2, m.getColumnDimension());
        assertEquals(new BigDecimal("7.0"), m.getArray()[0][0]);
        assertEquals(new BigDecimal("8.0"), m.getArray()[0][1]);
    }

    @Test
    void testConstructor_From2DArray_SingleColumn() {
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
    void testConstructor_FromPackedArray() {
        double[] vals = {1, 2, 3, 4, 5, 6}; // packed by columns for 2x3 matrix
        BigMatrix m = new BigMatrix(vals, 2);
        assertEquals(2, m.getRowDimension());
        assertEquals(3, m.getColumnDimension());
        assertEquals(1.0, m.getArray()[0][0].doubleValue(), 1e-10);
        assertEquals(2.0, m.getArray()[1][0].doubleValue(), 1e-10);
        assertEquals(3.0, m.getArray()[0][1].doubleValue(), 1e-10);
    }

    @Test
    void testConstructor_FromPackedArray_1x1() {
        double[] vals = {5.5};
        BigMatrix m = new BigMatrix(vals, 1);
        assertEquals(1, m.getRowDimension());
        assertEquals(1, m.getColumnDimension());
        assertEquals(new BigDecimal("5.5"), m.getArray()[0][0]);
    }

    @Test
    void testConstructor_From2DArray_InconsistentRowLength() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0")} // Different length - should fail
        };
        assertThrows(IllegalArgumentException.class, () -> new BigMatrix(data));
    }

    @Test
    void testConstructor_FromPackedArray_InvalidLength() {
        double[] vals = {1, 2, 3, 4, 5}; // Not divisible by m=2
        assertThrows(IllegalArgumentException.class, () -> new BigMatrix(vals, 2));
    }

    // ==================== ELEMENT ACCESS TESTS ====================

    @Test
    void testGetArray() {
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
    void testGetArrayCopy() {
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
    void testGetColumnPackedCopy() {
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
    void testGetRowDimension() {
        BigMatrix m = new BigMatrix(7, 3);
        assertEquals(7, m.getRowDimension());
    }

    @Test
    void testGetColumnDimension() {
        BigMatrix m = new BigMatrix(3, 9);
        assertEquals(9, m.getColumnDimension());
    }

    @Test
    void testGetDimensions_NonSquare() {
        BigMatrix m = new BigMatrix(4, 6);
        assertEquals(4, m.getRowDimension());
        assertEquals(6, m.getColumnDimension());
    }

    @Test
    void testGetDimensions_Square() {
        BigMatrix m = new BigMatrix(5, 5);
        assertEquals(5, m.getRowDimension());
        assertEquals(5, m.getColumnDimension());
    }

    // ==================== NORM CALCULATION TESTS ====================

    @Test
    void testNorm1_SimpleMatrix() {
        // Column sums: col 0 = 3, col 1 = 7, max = 7
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("2.0"), new BigDecimal("5.0")}
        };
        BigMatrix m = new BigMatrix(data);
        assertEquals(7.0, m.norm1(), 1e-10);
    }

    @Test
    void testNorm1_IdentityMatrix() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("1.0")}
        };
        BigMatrix m = new BigMatrix(data);
        assertEquals(1.0, m.norm1(), 1e-10);
    }

    @Test
    void testNorm1_ZeroMatrix() {
        BigMatrix m = new BigMatrix(3, 3);
        assertEquals(0.0, m.norm1(), 1e-10);
    }

    @Test
    void testNormInf_SimpleMatrix() {
        // Row sums: row 0 = 3, row 1 = 7, max = 7
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("2.0"), new BigDecimal("5.0")}
        };
        BigMatrix m = new BigMatrix(data);
        assertEquals(7.0, m.normInf(), 1e-10);
    }

    @Test
    void testNormInf_IdentityMatrix() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("1.0")}
        };
        BigMatrix m = new BigMatrix(data);
        assertEquals(1.0, m.normInf(), 1e-10);
    }

    @Test
    void testNormInf_NegativeValues() {
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
    void testGetMatrix_SubmatrixByRows() {
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
    void testGetMatrix_SingleRow() {
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
    void testGetMatrix_SingleColumn() {
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

    @Test
    void testGetMatrix_InvalidRowIndex() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0"), new BigDecimal("4.0")}
        };
        BigMatrix m = new BigMatrix(data);

        int[] rows = {0, 5}; // Index 5 is out of bounds
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.getMatrix(rows, 0, 1));
    }

    @Test
    void testGetMatrix_InvalidColumnIndex() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0"), new BigDecimal("4.0")}
        };
        BigMatrix m = new BigMatrix(data);

        int[] rows = {0};
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> m.getMatrix(rows, 0, 5));
    }

    // ==================== SOLVE METHOD TESTS ====================

    @Test
    void testSolve_2x2System() {
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
        // Verify solution: A * X ~ B
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
    void testSolve_3x3System() {
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
    void testSolve_NonSquareReturnsNull() {
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
    void testSolve_MultipleRightHandSides() {
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
    void testLU_Decomposition() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0"), new BigDecimal("4.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigLUDecomposition lu = m.lu();

        assertNotNull(lu);
    }

    @Test
    void testResetLUDecomp() {
        BigDecimal[][] data = {
            {new BigDecimal("2.0"), new BigDecimal("1.0")},
            {new BigDecimal("1.0"), new BigDecimal("3.0")}
        };
        BigMatrix A = new BigMatrix(data);

        // Create LU decomposition
        BigLUDecomposition lu1 = A.lu();
        assertNotNull(lu1);

        // Reset it
        A.resetLUDecomp();

        // Create new one - should work without error
        BigLUDecomposition lu2 = A.lu();
        assertNotNull(lu2);
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void testLargeMatrix() {
        BigMatrix m = new BigMatrix(100, 100);
        assertEquals(100, m.getRowDimension());
        assertEquals(100, m.getColumnDimension());

        BigDecimal[][] array = m.getArray();
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                assertEquals(BigDecimal.ZERO, array[i][j]);
            }
        }
    }

    @Test
    void testMatrixWithVerySmallNumbers() {
        BigDecimal[][] data = {
            {new BigDecimal("0.0000001"), new BigDecimal("0.0000002")},
            {new BigDecimal("0.0000003"), new BigDecimal("0.0000004")}
        };
        BigMatrix m = new BigMatrix(data);

        assertEquals(0.0000006, m.norm1(), 1e-10);
        assertEquals(0.0000007, m.normInf(), 1e-10);
    }

    @Test
    void testMatrixWithVeryLargeNumbers() {
        BigDecimal[][] data = {
            {new BigDecimal("1000000.0"), new BigDecimal("2000000.0")},
            {new BigDecimal("3000000.0"), new BigDecimal("4000000.0")}
        };
        BigMatrix m = new BigMatrix(data);

        assertEquals(6000000.0, m.norm1(), 1e-1);
        assertEquals(7000000.0, m.normInf(), 1e-1);
    }

    @Test
    void testPackedArrayWith_AllZeros() {
        double[] vals = {0, 0, 0, 0};
        BigMatrix m = new BigMatrix(vals, 2);

        assertEquals(2, m.getRowDimension());
        assertEquals(2, m.getColumnDimension());
        assertEquals(BigDecimal.ZERO, m.getArray()[0][0]);
        assertEquals(BigDecimal.ZERO, m.getArray()[1][1]);
    }

    // ==================== ADDITIONAL EDGE CASES & COVERAGE ====================

    @Test
    void testPrint_WithIntParameters() {
        BigDecimal[][] data = {
            {new BigDecimal("1.23"), new BigDecimal("4.56")},
            {new BigDecimal("7.89"), new BigDecimal("2.34")}
        };
        BigMatrix m = new BigMatrix(data);
        // Just verify this doesn't throw exception
        m.print(10, 2);
    }

    @Test
    void testPrint_WithPrintWriter() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0"), new BigDecimal("4.0")}
        };
        BigMatrix m = new BigMatrix(data);
        PrintWriter writer = new PrintWriter(System.out, true);
        // Verify doesn't throw
        m.print(writer, 5, 2);
        writer.flush();
    }

    @Test
    void testPrint_WithNumberFormat() {
        BigDecimal[][] data = {
            {new BigDecimal("1.234"), new BigDecimal("5.678")}
        };
        BigMatrix m = new BigMatrix(data);
        NumberFormat format = new DecimalFormat("0.00");
        // Verify doesn't throw
        m.print(format, 8);
    }

    @Test
    void testConstructor_PackedArray_SingleElement() {
        double[] vals = {42.0};
        BigMatrix m = new BigMatrix(vals, 1);
        assertEquals(1, m.getRowDimension());
        assertEquals(1, m.getColumnDimension());
        assertEquals(42.0, m.getArray()[0][0].doubleValue(), 1e-10);
    }

    @Test
    void testGetMatrix_AllRows_AllColumns() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0"), new BigDecimal("4.0")}
        };
        BigMatrix m = new BigMatrix(data);
        int[] rows = {0, 1};
        BigMatrix sub = m.getMatrix(rows, 0, 1);

        assertEquals(2, sub.getRowDimension());
        assertEquals(2, sub.getColumnDimension());
        assertEquals(new BigDecimal("1.0"), sub.getArray()[0][0]);
        assertEquals(new BigDecimal("4.0"), sub.getArray()[1][1]);
    }

    @Test
    void testNormInf_LargeMatrix() {
        BigDecimal[][] data = new BigDecimal[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                data[i][j] = new BigDecimal("1.0");
            }
        }
        BigMatrix m = new BigMatrix(data);
        assertEquals(10.0, m.normInf(), 1e-10);
    }

    @Test
    void testNorm1_LargeMatrix() {
        BigDecimal[][] data = new BigDecimal[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                data[i][j] = new BigDecimal("2.0");
            }
        }
        BigMatrix m = new BigMatrix(data);
        assertEquals(20.0, m.norm1(), 1e-10);
    }

    @Test
    void testSolve_DiagonalMatrix() {
        BigDecimal[][] aData = {
            {new BigDecimal("2.0"), new BigDecimal("0.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("3.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("4.0")}
        };
        BigDecimal[][] bData = {
            {new BigDecimal("2.0")},
            {new BigDecimal("6.0")},
            {new BigDecimal("8.0")}
        };

        BigMatrix A = new BigMatrix(aData);
        BigMatrix B = new BigMatrix(bData);
        BigMatrix X = A.solve(B);

        assertNotNull(X);
        assertEquals(3, X.getRowDimension());
        assertEquals(1, X.getColumnDimension());
        assertEquals(1.0, X.getArray()[0][0].doubleValue(), 0.1);
        assertEquals(2.0, X.getArray()[1][0].doubleValue(), 0.1);
        assertEquals(2.0, X.getArray()[2][0].doubleValue(), 0.1);
    }

    @Test
    void testConstructor_From2DArray_NegativeNumbers() {
        BigDecimal[][] data = {
            {new BigDecimal("-1.5"), new BigDecimal("-2.5")},
            {new BigDecimal("-3.5"), new BigDecimal("-4.5")}
        };
        BigMatrix m = new BigMatrix(data);
        assertEquals(2, m.getRowDimension());
        assertEquals(2, m.getColumnDimension());
        assertEquals(new BigDecimal("-1.5"), m.getArray()[0][0]);
    }

    @Test
    void testNorm1_Mixed_Positive_Negative() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("-5.0")},
            {new BigDecimal("-2.0"), new BigDecimal("3.0")}
        };
        BigMatrix m = new BigMatrix(data);
        assertEquals(8.0, m.norm1(), 1e-10);
    }

    @Test
    void testNormInf_Mixed_Positive_Negative() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("-5.0")},
            {new BigDecimal("-2.0"), new BigDecimal("3.0")}
        };
        BigMatrix m = new BigMatrix(data);
        assertEquals(6.0, m.normInf(), 1e-10);
    }

    @Test
    void testGetArrayCopy_Independence() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0"), new BigDecimal("4.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigDecimal[][] copy1 = m.getArrayCopy();
        BigDecimal[][] copy2 = m.getArrayCopy();

        copy1[0][0] = new BigDecimal("99.0");
        assertEquals(new BigDecimal("1.0"), copy2[0][0]);
    }

    @Test
    void testLU_Singular_Behavior() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("2.0"), new BigDecimal("4.0")}
        };
        BigMatrix m = new BigMatrix(data);
        BigLUDecomposition lu = m.lu();
        assertNotNull(lu);
    }

    @Test
    void testResetLUDecomp_ClearsCache() {
        BigDecimal[][] data = {
            {new BigDecimal("2.0"), new BigDecimal("1.0")},
            {new BigDecimal("1.0"), new BigDecimal("3.0")}
        };
        BigMatrix m = new BigMatrix(data);

        m.lu();
        m.resetLUDecomp();

        BigLUDecomposition lu2 = m.lu();
        assertNotNull(lu2);
    }

    @Test
    void testConstructor_LargePackedArray() {
        double[] vals = new double[100];
        for (int i = 0; i < 100; i++) {
            vals[i] = i * 1.5;
        }
        BigMatrix m = new BigMatrix(vals, 10);
        assertEquals(10, m.getRowDimension());
        assertEquals(10, m.getColumnDimension());
        assertEquals(0.0, m.getArray()[0][0].doubleValue(), 1e-10);
        assertEquals(1.5, m.getArray()[1][0].doubleValue(), 1e-10);
    }

    @Test
    void testGetColumnPackedCopy_Tall_Matrix() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("4.0")},
            {new BigDecimal("2.0"), new BigDecimal("5.0")},
            {new BigDecimal("3.0"), new BigDecimal("6.0")}
        };
        BigMatrix m = new BigMatrix(data);
        double[] packed = m.getColumnPackedCopy();

        assertEquals(1.0, packed[0], 1e-10);
        assertEquals(2.0, packed[1], 1e-10);
        assertEquals(3.0, packed[2], 1e-10);
        assertEquals(4.0, packed[3], 1e-10);
        assertEquals(5.0, packed[4], 1e-10);
        assertEquals(6.0, packed[5], 1e-10);
    }

    @Test
    void testSolve_Identity_Matrix() {
        BigDecimal[][] aData = {
            {new BigDecimal("1.0"), new BigDecimal("0.0")},
            {new BigDecimal("0.0"), new BigDecimal("1.0")}
        };
        BigDecimal[][] bData = {
            {new BigDecimal("5.0")},
            {new BigDecimal("7.0")}
        };

        BigMatrix A = new BigMatrix(aData);
        BigMatrix B = new BigMatrix(bData);
        BigMatrix X = A.solve(B);

        assertNotNull(X);
        assertEquals(5.0, X.getArray()[0][0].doubleValue(), 0.1);
        assertEquals(7.0, X.getArray()[1][0].doubleValue(), 0.1);
    }

    // ==================== ADDITIONAL TARGETED TESTS FOR FINAL COVERAGE ====================

    @Test
    void testConstructorZeroByZero() {
        BigMatrix m = new BigMatrix(0, 0);
        assertEquals(0, m.getRowDimension());
        assertEquals(0, m.getColumnDimension());
    }

    @Test
    void testSetGetElement() {
        BigDecimal[][] data = new BigDecimal[2][2];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                data[i][j] = new BigDecimal(String.valueOf(i * 10 + j));
            }
        }
        BigMatrix m = new BigMatrix(data);
        assertEquals(0, m.getArray()[0][0].intValue());
        assertEquals(11, m.getArray()[1][1].intValue());
    }

    @Test
    void testNormInfWithSingleColumn() {
        BigDecimal[][] data = {
            {new BigDecimal("2.0")},
            {new BigDecimal("3.0")},
            {new BigDecimal("5.0")}
        };
        BigMatrix m = new BigMatrix(data);
        assertEquals(5.0, m.normInf(), 1e-10);
    }

    @Test
    void testNorm1WithSingleRow() {
        BigDecimal[][] data = {{new BigDecimal("2.0"), new BigDecimal("3.0"), new BigDecimal("4.0")}};
        BigMatrix m = new BigMatrix(data);
        assertEquals(4.0, m.norm1(), 1e-10);
    }

    @Test
    void testGetMatrix_AllRows() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0")},
            {new BigDecimal("3.0"), new BigDecimal("4.0")},
            {new BigDecimal("5.0"), new BigDecimal("6.0")}
        };
        BigMatrix m = new BigMatrix(data);
        int[] rows = {0, 1, 2};
        BigMatrix sub = m.getMatrix(rows, 0, 1);

        assertEquals(3, sub.getRowDimension());
        assertEquals(2, sub.getColumnDimension());
        assertEquals(new BigDecimal("5.0"), sub.getArray()[2][0]);
    }

    @Test
    void testConstructor_From2DArray_Large() {
        BigDecimal[][] data = new BigDecimal[50][50];
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 50; j++) {
                data[i][j] = new BigDecimal(String.valueOf((i + 1) * (j + 1)));
            }
        }
        BigMatrix m = new BigMatrix(data);
        assertEquals(50, m.getRowDimension());
        assertEquals(50, m.getColumnDimension());
    }

    @Test
    void testGetArrayCopy_AllElements() {
        BigDecimal[][] data = {
            {new BigDecimal("1.1"), new BigDecimal("2.2"), new BigDecimal("3.3")},
            {new BigDecimal("4.4"), new BigDecimal("5.5"), new BigDecimal("6.6")},
            {new BigDecimal("7.7"), new BigDecimal("8.8"), new BigDecimal("9.9")}
        };
        BigMatrix m = new BigMatrix(data);
        BigDecimal[][] copy = m.getArrayCopy();

        assertEquals(new BigDecimal("1.1"), copy[0][0]);
        assertEquals(new BigDecimal("9.9"), copy[2][2]);
        copy[0][0] = new BigDecimal("0.0");
        assertEquals(new BigDecimal("1.1"), m.getArray()[0][0]);
    }

    @Test
    void testGetColumnPackedCopy_Wide() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0"), new BigDecimal("4.0")},
            {new BigDecimal("5.0"), new BigDecimal("6.0"), new BigDecimal("7.0"), new BigDecimal("8.0")}
        };
        BigMatrix m = new BigMatrix(data);
        double[] packed = m.getColumnPackedCopy();

        assertEquals(1.0, packed[0], 1e-10);
        assertEquals(5.0, packed[1], 1e-10);
        assertEquals(2.0, packed[2], 1e-10);
        assertEquals(6.0, packed[3], 1e-10);
        assertEquals(8.0, packed[7], 1e-10);
    }

    @Test
    void testSolveWithCaching() {
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

        BigMatrix X1 = A.solve(B);
        assertNotNull(X1);

        BigMatrix X2 = A.solve(B);
        assertNotNull(X2);

        assertEquals(X1.getArray()[0][0].doubleValue(), X2.getArray()[0][0].doubleValue(), 0.01);
    }

    @Test
    void testGetMatrix_SpecificRows_SpecificCols() {
        BigDecimal[][] data = {
            {new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("3.0"), new BigDecimal("4.0")},
            {new BigDecimal("5.0"), new BigDecimal("6.0"), new BigDecimal("7.0"), new BigDecimal("8.0")},
            {new BigDecimal("9.0"), new BigDecimal("10.0"), new BigDecimal("11.0"), new BigDecimal("12.0")},
            {new BigDecimal("13.0"), new BigDecimal("14.0"), new BigDecimal("15.0"), new BigDecimal("16.0")}
        };
        BigMatrix m = new BigMatrix(data);
        int[] rows = {0, 3};
        BigMatrix sub = m.getMatrix(rows, 1, 2);

        assertEquals(2, sub.getRowDimension());
        assertEquals(2, sub.getColumnDimension());
        assertEquals(new BigDecimal("2.0"), sub.getArray()[0][0]);
        assertEquals(new BigDecimal("15.0"), sub.getArray()[1][1]);
    }

    @Test
    void testNormOperations_ConsistentWithZeroMatrix() {
        BigMatrix zero = new BigMatrix(10, 10);
        assertEquals(0.0, zero.norm1(), 1e-10);
        assertEquals(0.0, zero.normInf(), 1e-10);
    }

    @Test
    void testLU_WithMultipleCalls() {
        BigDecimal[][] data = {
            {new BigDecimal("2.0"), new BigDecimal("3.0")},
            {new BigDecimal("4.0"), new BigDecimal("5.0")}
        };
        BigMatrix m = new BigMatrix(data);

        BigLUDecomposition lu1 = m.lu();
        assertNotNull(lu1);

        BigLUDecomposition lu2 = m.lu();
        assertNotNull(lu2);
    }

}
