/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.datacontainer;

import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Comprehensive test suite for DataContainerSimple and related classes.
 * Covers basic operations, edge cases, and special values.
 */
public class DataContainerSimpleTest {

    private static final double DELTA = 1e-6;
    private static final float EPSILON = 1e-6f;

    private DataContainerSimple container;
    private static final int ROWS = 5;
    private static final int COLS = 100;

    @Before
    public void setUp() {
        container = new DataContainerSimple(ROWS, COLS);
    }

    // ==================== Basic Functionality Tests ====================

    @Test
    public void testContainerCreation() {
        assertNotNull(container);
        assertEquals(ROWS, container.getRowLength());
    }

    @Test
    public void testSetAndGetValue() {
        float testValue = 123.45f;
        container.setValue(testValue, 1, 0);
        assertEquals(testValue, container.getValue(1, 0), EPSILON);
    }

    @Test
    public void testSetAndGetValueMultiple() {
        float[] testValues = {1.0f, 2.5f, -3.7f, 0.0f, 100.5f};
        for (int i = 0; i < testValues.length; i++) {
            container.setValue(testValues[i], 1, i);
        }
        for (int i = 0; i < testValues.length; i++) {
            assertEquals(testValues[i], container.getValue(1, i), EPSILON);
        }
    }

    @Test
    public void testSetAndGetValueMultipleRows() {
        for (int row = 1; row < ROWS; row++) {
            for (int col = 0; col < 10; col++) {
                float value = row * 10 + col;
                container.setValue(value, row, col);
                assertEquals(value, container.getValue(row, col), EPSILON);
            }
        }
    }

    // ==================== Special Values Tests ====================

    @Test
    public void testZeroValue() {
        container.setValue(0.0f, 1, 0);
        assertEquals(0.0f, container.getValue(1, 0), EPSILON);
    }

    @Test
    public void testNegativeValue() {
        container.setValue(-123.45f, 1, 0);
        assertEquals(-123.45f, container.getValue(1, 0), EPSILON);
    }

    @Test
    public void testVerySmallValue() {
        float smallValue = 1e-10f;
        container.setValue(smallValue, 1, 0);
        assertEquals(smallValue, container.getValue(1, 0), smallValue * 0.1f);
    }

    @Test
    public void testVeryLargeValue() {
        float largeValue = 1e10f;
        container.setValue(largeValue, 1, 0);
        assertEquals(largeValue, container.getValue(1, 0), largeValue * EPSILON);
    }

    @Test
    public void testNegativeLargeValue() {
        float negLarge = -1e10f;
        container.setValue(negLarge, 1, 0);
        assertEquals(negLarge, container.getValue(1, 0), Math.abs(negLarge) * EPSILON);
    }

    // ==================== Min/Max Tests ====================

    @Test
    public void testGetHiLoValue() {
        container.setValue(1.0f, 1, 0);
        container.setValue(5.0f, 1, 1);
        container.setValue(3.0f, 1, 2);
        container.setValue(8.0f, 1, 3);
        container.setValue(2.0f, 1, 4);

        HiLoData hiLo = container.getHiLoValue(1, 0, 5);
        assertNotNull(hiLo);
        assertEquals(1.0f, hiLo._yLo, EPSILON);
        assertEquals(8.0f, hiLo._yHi, EPSILON);
    }

    @Test
    public void testGetHiLoValueWithNegatives() {
        container.setValue(-10.0f, 1, 0);
        container.setValue(5.0f, 1, 1);
        container.setValue(-3.0f, 1, 2);
        container.setValue(8.0f, 1, 3);
        container.setValue(-5.0f, 1, 4);

        HiLoData hiLo = container.getHiLoValue(1, 0, 5);
        assertNotNull(hiLo);
        assertEquals(-10.0f, hiLo._yLo, EPSILON);
        assertEquals(8.0f, hiLo._yHi, EPSILON);
    }

    @Test
    public void testGetHiLoValueAllSame() {
        float sameValue = 5.0f;
        for (int i = 0; i < 10; i++) {
            container.setValue(sameValue, 1, i);
        }

        HiLoData hiLo = container.getHiLoValue(1, 0, 10);
        assertNotNull(hiLo);
        assertEquals(sameValue, hiLo._yLo, EPSILON);
        assertEquals(sameValue, hiLo._yHi, EPSILON);
    }

    @Test
    public void testGetHiLoValueSinglePoint() {
        container.setValue(7.5f, 1, 0);

        HiLoData hiLo = container.getHiLoValue(1, 0, 1);
        assertNotNull(hiLo);
        assertEquals(7.5f, hiLo._yLo, EPSILON);
        assertEquals(7.5f, hiLo._yHi, EPSILON);
    }

    @Test
    public void testGetAbsoluteMinMaxValue() {
        container.setValue(-10.0f, 1, 0);
        container.setValue(5.0f, 1, 1);
        container.setValue(20.0f, 1, 2);
        container.setValue(-5.0f, 1, 3);
        container.setValue(0.0f, 1, 4);

        HiLoData absMinMax = container.getAbsoluteMinMaxValue(1);
        assertNotNull(absMinMax);
        assertEquals(-10.0f, absMinMax._yLo, EPSILON);
        assertEquals(20.0f, absMinMax._yHi, EPSILON);
    }

    @Test
    public void testGetAbsoluteMinMaxValueAllPositive() {
        container.setValue(1.0f, 1, 0);
        container.setValue(5.0f, 1, 1);
        container.setValue(20.0f, 1, 2);
        container.setValue(3.0f, 1, 3);
        container.setValue(10.0f, 1, 4);

        HiLoData absMinMax = container.getAbsoluteMinMaxValue(1);
        assertNotNull(absMinMax);
        assertEquals(1.0f, absMinMax._yLo, EPSILON);
        assertEquals(20.0f, absMinMax._yHi, EPSILON);
    }

    @Test
    public void testGetAbsoluteMinMaxValueAllNegative() {
        container.setValue(-20.0f, 1, 0);
        container.setValue(-5.0f, 1, 1);
        container.setValue(-1.0f, 1, 2);
        container.setValue(-15.0f, 1, 3);
        container.setValue(-8.0f, 1, 4);

        HiLoData absMinMax = container.getAbsoluteMinMaxValue(1);
        assertNotNull(absMinMax);
        assertEquals(-20.0f, absMinMax._yLo, EPSILON);
        assertEquals(-1.0f, absMinMax._yHi, EPSILON);
    }

    @Test
    public void testGetAbsoluteMinMaxValueSingleValue() {
        container.setValue(42.0f, 1, 0);

        HiLoData absMinMax = container.getAbsoluteMinMaxValue(1);
        assertNotNull(absMinMax);
        assertEquals(42.0f, absMinMax._yLo, EPSILON);
        assertEquals(42.0f, absMinMax._yHi, EPSILON);
    }

    // ==================== Signal Name Tests ====================

    @Test
    public void testGetSignalName() {
        String name = container.getSignalName(0);
        assertNotNull(name);
        assertFalse(name.isEmpty());
    }

    @Test
    public void testGetSignalNameMultipleRows() {
        for (int row = 0; row < ROWS; row++) {
            String name = container.getSignalName(row);
            assertNotNull(name);
            assertFalse(name.isEmpty());
        }
    }

    @Test
    public void testSetSignalName() {
        String newName = "TestSignal";
        container.setSignalName(newName, 0);
        assertEquals(newName, container.getSignalName(0));
    }

    @Test
    public void testSetSignalNameMultiple() {
        String[] names = {"Voltage", "Current", "Power", "Frequency", "Phase"};
        for (int row = 0; row < names.length && row < ROWS; row++) {
            container.setSignalName(names[row], row);
            assertEquals(names[row], container.getSignalName(row));
        }
    }

    // ==================== Edge Case Tests ====================

    @Test
    public void testWriteToFirstDataRow() {
        // Row 0 is reserved for time data, first data row is row 1
        container.setValue(100.0f, 1, 0);
        assertEquals(100.0f, container.getValue(1, 0), EPSILON);
    }

    @Test
    public void testWriteToLastColumn() {
        container.setValue(99.99f, 1, COLS - 1);
        assertEquals(99.99f, container.getValue(1, COLS - 1), EPSILON);
    }

    @Test
    public void testWriteSequentialValues() {
        for (int col = 0; col < 50; col++) {
            container.setValue(col * 1.5f, 1, col);
        }

        for (int col = 0; col < 50; col++) {
            assertEquals(col * 1.5f, container.getValue(1, col), EPSILON);
        }
    }

    @Test
    public void testWriteRandomValues() {
        java.util.Random rand = new java.util.Random(12345); // Fixed seed for reproducibility
        float[] values = new float[20];
        for (int i = 0; i < values.length; i++) {
            values[i] = rand.nextFloat() * 1000 - 500; // Range: -500 to 500
            container.setValue(values[i], 1, i);
        }

        rand = new java.util.Random(12345); // Reset with same seed
        for (int i = 0; i < values.length; i++) {
            float expected = rand.nextFloat() * 1000 - 500;
            assertEquals(expected, container.getValue(1, i), EPSILON);
        }
    }

    @Test
    public void testOverwriteValue() {
        container.setValue(10.0f, 1, 0);
        assertEquals(10.0f, container.getValue(1, 0), EPSILON);

        container.setValue(20.0f, 1, 0);
        assertEquals(20.0f, container.getValue(1, 0), EPSILON);

        container.setValue(-5.0f, 1, 0);
        assertEquals(-5.0f, container.getValue(1, 0), EPSILON);
    }

    @Test
    public void testDeleteDataReference() {
        container.setValue(123.45f, 1, 0);
        container.deleteDataReference();
        // Should not crash after delete
    }

    @Test
    public void testConstantDtTimeSeries() {
        DataContainerSimple tsContainer = DataContainerSimple.fabricConstantDtTimeSeries(3, 50);
        assertNotNull(tsContainer);
        assertEquals(3, tsContainer.getRowLength());
    }

    @Test
    public void testArrayTimeSeries() {
        DataContainerSimple tsContainer = DataContainerSimple.fabricArrayTimeSeries(3, 50);
        assertNotNull(tsContainer);
        assertEquals(3, tsContainer.getRowLength());
    }

    @Test
    public void testImplementsIScopeData() {
        assertTrue("Should implement IScopeData", 
                   container instanceof ch.technokrat.gecko.geckocircuits.api.IScopeData);
    }

    // ==================== Integration Tests ====================

    @Test
    public void testMultipleRowsIndependence() {
        // Verify that different rows don't interfere with each other
        container.setValue(10.0f, 1, 0);
        container.setValue(20.0f, 2, 0);
        container.setValue(30.0f, 3, 0);

        assertEquals(10.0f, container.getValue(1, 0), EPSILON);
        assertEquals(20.0f, container.getValue(2, 0), EPSILON);
        assertEquals(30.0f, container.getValue(3, 0), EPSILON);
    }

    @Test
    public void testLargeDataSet() {
        DataContainerSimple largeContainer = new DataContainerSimple(10, 1000);
        for (int row = 1; row < 10; row++) {
            for (int col = 0; col < 1000; col++) {
                float value = row * 1000 + col;
                largeContainer.setValue(value, row, col);
            }
        }

        // Verify some random samples
        assertEquals(1000.0f, largeContainer.getValue(1, 0), EPSILON);
        assertEquals(1500.0f, largeContainer.getValue(1, 500), EPSILON);
        assertEquals(9999.0f, largeContainer.getValue(9, 999), EPSILON);
    }
}
