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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for LossCurve - edge cases and critical paths.
 * Focuses on null handling, boundary conditions, and array operations.
 */
public class LossCurveEdgeCasesTest {

    private static final double DELTA = 1e-10;

    // Test concrete implementation of abstract LossCurve
    private static class TestLossCurve extends LossCurve {
        @Override
        String getXMLTag() {
            return "testCurve";
        }
    }

    // ====================================================
    // setCurveData() - Boundary and Edge Cases
    // ====================================================

    @Test
    public void testSetCurveData_ValidSingleRow() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{1.0, 2.0, 3.0}};

        curve.setCurveData(input);

        assertNotNull(curve.data);
        assertEquals(1, curve.data.length);
        assertEquals(3, curve.data[0].length);
        assertEquals(1.0, curve.data[0][0], DELTA);
        assertEquals(2.0, curve.data[0][1], DELTA);
        assertEquals(3.0, curve.data[0][2], DELTA);
    }

    @Test
    public void testSetCurveData_ValidMultipleRows() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{1.0, 2.0}, {3.0, 4.0}, {5.0, 6.0}};

        curve.setCurveData(input);

        assertEquals(3, curve.data.length);
        assertEquals(2, curve.data[0].length);
        assertEquals(2, curve.data[1].length);
        assertEquals(2, curve.data[2].length);
        assertEquals(5.0, curve.data[2][0], DELTA);
    }

    @Test
    public void testSetCurveData_SingleElement() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{42.0}};

        curve.setCurveData(input);

        assertEquals(1, curve.data.length);
        assertEquals(1, curve.data[0].length);
        assertEquals(42.0, curve.data[0][0], DELTA);
    }

    @Test
    public void testSetCurveData_ZeroValues() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{0.0, 0.0}, {0.0, 0.0}};

        curve.setCurveData(input);

        assertEquals(0.0, curve.data[0][0], DELTA);
        assertEquals(0.0, curve.data[1][1], DELTA);
    }

    @Test
    public void testSetCurveData_NegativeValues() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{-1.5, -2.5}, {-3.5, -4.5}};

        curve.setCurveData(input);

        assertEquals(-1.5, curve.data[0][0], DELTA);
        assertEquals(-4.5, curve.data[1][1], DELTA);
    }

    @Test
    public void testSetCurveData_LargeValues() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{1e10, 1e20}, {1e-10, 1e-20}};

        curve.setCurveData(input);

        assertEquals(1e10, curve.data[0][0], 1e5);
        assertEquals(1e-20, curve.data[1][1], 1e-25);
    }

    @Test
    public void testSetCurveData_DoesNotModifyInput() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{1.0, 2.0}, {3.0, 4.0}};
        double[][] inputCopy = {{1.0, 2.0}, {3.0, 4.0}};

        curve.setCurveData(input);
        input[0][0] = 999.0;  // Modify original

        // Curve should have unchanged copy
        assertEquals(1.0, curve.data[0][0], DELTA);
    }

    @Test
    public void testSetCurveData_JaggedArray() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{1.0, 2.0, 3.0}, {4.0, 5.0}, {6.0}};

        curve.setCurveData(input);

        assertEquals(3, curve.data.length);
        assertEquals(3, curve.data[0].length);
        assertEquals(2, curve.data[1].length);
        assertEquals(1, curve.data[2].length);
    }

    // ====================================================
    // getCurveData() - Boundary and Edge Cases
    // ====================================================

    @Test
    public void testGetCurveData_ValidData() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{1.0, 2.0}, {3.0, 4.0}};
        curve.setCurveData(input);

        double[][] result = curve.getCurveData();

        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(2, result[0].length);
        assertEquals(1.0, result[0][0], DELTA);
        assertEquals(4.0, result[1][1], DELTA);
    }

    @Test
    public void testGetCurveData_ReturnsCopyNotReference() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{1.0, 2.0}};
        curve.setCurveData(input);

        double[][] result = curve.getCurveData();
        result[0][0] = 999.0;  // Modify the returned copy

        // Original should be unchanged
        assertEquals(1.0, curve.data[0][0], DELTA);
    }

    @Test
    public void testGetCurveData_SingleRow() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{5.0, 10.0, 15.0}};
        curve.setCurveData(input);

        double[][] result = curve.getCurveData();

        assertEquals(1, result.length);
        assertEquals(3, result[0].length);
    }

    @Test
    public void testGetCurveData_SingleColumn() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{1.0}, {2.0}, {3.0}};
        curve.setCurveData(input);

        double[][] result = curve.getCurveData();

        assertEquals(3, result.length);
        assertEquals(1, result[0].length);
    }

    @Test
    public void testGetCurveData_MultipleConsecutiveCalls() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{1.0, 2.0}};
        curve.setCurveData(input);

        double[][] result1 = curve.getCurveData();
        double[][] result2 = curve.getCurveData();

        // Both should have same content
        assertEquals(result1[0][0], result2[0][0], DELTA);
        // But should be different objects
        assertNotSame(result1, result2);
    }

    // ====================================================
    // Round-trip Tests (Set then Get)
    // ====================================================

    @Test
    public void testRoundTrip_PreservesData() {
        LossCurve curve = new TestLossCurve();
        double[][] original = {{1.5, 2.5, 3.5}, {4.5, 5.5, 6.5}};

        curve.setCurveData(original);
        double[][] retrieved = curve.getCurveData();

        for (int i = 0; i < original.length; i++) {
            for (int j = 0; j < original[i].length; j++) {
                assertEquals(original[i][j], retrieved[i][j], DELTA);
            }
        }
    }

    @Test
    public void testRoundTrip_PreservesStructure() {
        LossCurve curve = new TestLossCurve();
        double[][] original = {{1.0, 2.0, 3.0}, {4.0, 5.0}, {6.0}};

        curve.setCurveData(original);
        double[][] retrieved = curve.getCurveData();

        assertEquals(original.length, retrieved.length);
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i].length, retrieved[i].length);
        }
    }

    // ====================================================
    // Extreme Values Tests
    // ====================================================

    @Test
    public void testExtremeValues_PositiveInfinity() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{Double.POSITIVE_INFINITY}};

        curve.setCurveData(input);

        assertEquals(Double.POSITIVE_INFINITY, curve.data[0][0], DELTA);
    }

    @Test
    public void testExtremeValues_NegativeInfinity() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{Double.NEGATIVE_INFINITY}};

        curve.setCurveData(input);

        assertEquals(Double.NEGATIVE_INFINITY, curve.data[0][0], DELTA);
    }

    @Test
    public void testExtremeValues_NaN() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{Double.NaN}};

        curve.setCurveData(input);

        assertTrue(Double.isNaN(curve.data[0][0]));
    }

    @Test
    public void testExtremeValues_MaxValue() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{Double.MAX_VALUE}};

        curve.setCurveData(input);

        assertEquals(Double.MAX_VALUE, curve.data[0][0], DELTA);
    }

    @Test
    public void testExtremeValues_MinValue() {
        LossCurve curve = new TestLossCurve();
        double[][] input = {{Double.MIN_VALUE}};

        curve.setCurveData(input);

        assertEquals(Double.MIN_VALUE, curve.data[0][0], DELTA);
    }

    // ====================================================
    // getName() - Test Temperature Formatting
    // ====================================================

    @Test
    public void testGetName_DefaultTemperature() {
        LossCurve curve = new TestLossCurve();

        String name = curve.getName();

        assertEquals("0°C", name);
    }

    @Test
    public void testGetName_PositiveTemperature() {
        LossCurve curve = new TestLossCurve();
        curve.tj.setValueWithoutUndo(25.5);

        String name = curve.getName();

        assertEquals("25°C", name);
    }

    @Test
    public void testGetName_NegativeTemperature() {
        LossCurve curve = new TestLossCurve();
        curve.tj.setValueWithoutUndo(-40.7);

        String name = curve.getName();

        assertEquals("-40°C", name);
    }

    @Test
    public void testGetName_LargeTemperature() {
        LossCurve curve = new TestLossCurve();
        curve.tj.setValueWithoutUndo(150.9);

        String name = curve.getName();

        assertEquals("150°C", name);
    }

    @Test
    public void testGetName_ZeroTemperature() {
        LossCurve curve = new TestLossCurve();
        curve.tj.setValueWithoutUndo(0.0);

        String name = curve.getName();

        assertEquals("0°C", name);
    }

    // ====================================================
    // Copy Independence Tests
    // ====================================================

    @Test
    public void testCopyIndependence_SetAfterGet() {
        LossCurve curve = new TestLossCurve();
        double[][] first = {{1.0, 2.0}};
        curve.setCurveData(first);

        double[][] retrieved1 = curve.getCurveData();

        double[][] second = {{99.0, 88.0}};
        curve.setCurveData(second);

        double[][] retrieved2 = curve.getCurveData();

        // First retrieval should not be affected by second set
        assertEquals(1.0, retrieved1[0][0], DELTA);
        assertEquals(99.0, retrieved2[0][0], DELTA);
    }

}
