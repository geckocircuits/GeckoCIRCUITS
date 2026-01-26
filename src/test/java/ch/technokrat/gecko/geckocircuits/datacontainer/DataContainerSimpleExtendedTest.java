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
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Additional tests for DataContainerSimple - extended coverage.
 */
public class DataContainerSimpleExtendedTest {

    private static final double DELTA = 1e-5;

    // ====================================================
    // Factory Method Tests
    // ====================================================

    @Test
    public void testFabricConstantDtTimeSeries() {
        DataContainerSimple container = DataContainerSimple.fabricConstantDtTimeSeries(3, 100);
        
        assertNotNull(container);
        assertEquals(3, container.getRowLength());
    }

    @Test
    public void testFabricArrayTimeSeries() {
        DataContainerSimple container = DataContainerSimple.fabricArrayTimeSeries(3, 100);
        
        assertNotNull(container);
        assertEquals(3, container.getRowLength());
    }

    // ====================================================
    // Constructor Tests
    // ====================================================

    @Test
    public void testConstructor_SmallContainer() {
        DataContainerSimple container = new DataContainerSimple(2, 10);
        
        assertEquals(2, container.getRowLength());
    }

    @Test
    public void testConstructor_LargeContainer() {
        DataContainerSimple container = new DataContainerSimple(100, 10000);
        
        assertEquals(100, container.getRowLength());
    }

    @Test
    public void testConstructor_MinimalContainer() {
        DataContainerSimple container = new DataContainerSimple(1, 1);
        
        assertEquals(1, container.getRowLength());
    }

    // ====================================================
    // SetValue/GetValue Tests
    // ====================================================

    @Test
    public void testSetGetValue_SingleValue() {
        DataContainerSimple container = new DataContainerSimple(3, 10);
        
        container.setValue(42.5f, 1, 0);  // row > 0 required
        
        assertEquals(42.5f, container.getValue(1, 0), DELTA);
    }

    @Test
    public void testSetGetValue_MultipleValues() {
        DataContainerSimple container = new DataContainerSimple(5, 20);
        
        for (int row = 1; row < 5; row++) {
            for (int col = 0; col < 20; col++) {
                container.setValue((float)(row * 100 + col), row, col);
            }
        }
        
        // Verify
        for (int row = 1; row < 5; row++) {
            for (int col = 0; col < 20; col++) {
                assertEquals(row * 100 + col, container.getValue(row, col), DELTA);
            }
        }
    }

    @Test
    public void testSetValue_NegativeValues() {
        DataContainerSimple container = new DataContainerSimple(3, 10);
        
        container.setValue(-100.5f, 1, 5);
        
        assertEquals(-100.5f, container.getValue(1, 5), DELTA);
    }

    @Test
    public void testSetValue_ZeroValue() {
        DataContainerSimple container = new DataContainerSimple(3, 10);
        
        container.setValue(0.0f, 1, 5);
        
        assertEquals(0.0f, container.getValue(1, 5), DELTA);
    }

    @Test
    public void testSetValue_VerySmallValue() {
        DataContainerSimple container = new DataContainerSimple(3, 10);
        
        container.setValue(1e-30f, 1, 5);
        
        assertEquals(1e-30f, container.getValue(1, 5), 1e-35);
    }

    @Test
    public void testSetValue_VeryLargeValue() {
        DataContainerSimple container = new DataContainerSimple(3, 10);
        
        container.setValue(1e30f, 1, 5);
        
        assertEquals(1e30f, container.getValue(1, 5), 1e25);
    }

    // ====================================================
    // HiLoData Tests
    // ====================================================

    @Test
    public void testGetHiLoValue_BasicRange() {
        DataContainerSimple container = new DataContainerSimple(3, 10);
        
        container.setValue(1.0f, 1, 0);
        container.setValue(5.0f, 1, 1);
        container.setValue(3.0f, 1, 2);
        container.setValue(8.0f, 1, 3);
        container.setValue(2.0f, 1, 4);
        
        HiLoData hiLo = container.getHiLoValue(1, 0, 5);
        
        assertNotNull(hiLo);
        assertEquals(1.0f, hiLo._yLo, DELTA);
        assertEquals(8.0f, hiLo._yHi, DELTA);
    }

    @Test
    public void testGetHiLoValue_SingleValue() {
        DataContainerSimple container = new DataContainerSimple(3, 10);
        
        container.setValue(5.0f, 1, 0);
        
        HiLoData hiLo = container.getHiLoValue(1, 0, 1);
        
        assertNotNull(hiLo);
        assertEquals(5.0f, hiLo._yLo, DELTA);
        assertEquals(5.0f, hiLo._yHi, DELTA);
    }

    @Test
    public void testGetHiLoValue_AllSameValue() {
        DataContainerSimple container = new DataContainerSimple(3, 10);
        
        for (int i = 0; i < 10; i++) {
            container.setValue(7.0f, 1, i);
        }
        
        HiLoData hiLo = container.getHiLoValue(1, 0, 10);
        
        assertEquals(7.0f, hiLo._yLo, DELTA);
        assertEquals(7.0f, hiLo._yHi, DELTA);
    }

    @Test
    public void testGetHiLoValue_NegativeValues() {
        DataContainerSimple container = new DataContainerSimple(3, 5);
        
        container.setValue(-10.0f, 1, 0);
        container.setValue(-5.0f, 1, 1);
        container.setValue(-8.0f, 1, 2);
        
        HiLoData hiLo = container.getHiLoValue(1, 0, 3);
        
        assertEquals(-10.0f, hiLo._yLo, DELTA);
        assertEquals(-5.0f, hiLo._yHi, DELTA);
    }

    // ====================================================
    // Absolute Min/Max Tests
    // ====================================================

    @Test
    public void testGetAbsoluteMinMaxValue_Basic() {
        DataContainerSimple container = new DataContainerSimple(3, 10);
        
        container.setValue(-20.0f, 1, 0);
        container.setValue(5.0f, 1, 1);
        container.setValue(30.0f, 1, 2);
        container.setValue(-5.0f, 1, 3);
        
        HiLoData absMinMax = container.getAbsoluteMinMaxValue(1);
        
        assertNotNull(absMinMax);
        assertEquals(-20.0f, absMinMax._yLo, DELTA);
        assertEquals(30.0f, absMinMax._yHi, DELTA);
    }

    // ====================================================
    // Signal Name Tests
    // ====================================================

    @Test
    public void testGetSignalName_Default() {
        DataContainerSimple container = new DataContainerSimple(3, 10);
        
        String name = container.getSignalName(0);
        
        // Should return some name (null or default string)
        // Actual behavior depends on implementation
    }

    // ====================================================
    // Row Length Tests
    // ====================================================

    @Test
    public void testGetRowLength_VariousSizes() {
        assertEquals(1, new DataContainerSimple(1, 10).getRowLength());
        assertEquals(5, new DataContainerSimple(5, 10).getRowLength());
        assertEquals(100, new DataContainerSimple(100, 10).getRowLength());
    }

    // ====================================================
    // Data Deletion Tests
    // ====================================================

    @Test
    public void testDeleteDataReference() {
        DataContainerSimple container = new DataContainerSimple(3, 100);
        
        // Set some values
        container.setValue(10.0f, 1, 0);
        
        // Delete reference
        container.deleteDataReference();
        
        // Should not throw during deletion
    }

    // ====================================================
    // IScopeData Interface Tests
    // ====================================================

    @Test
    public void testImplementsIScopeData() {
        DataContainerSimple container = new DataContainerSimple(2, 10);
        
        assertTrue(container instanceof ch.technokrat.gecko.geckocircuits.api.IScopeData);
    }

    // ====================================================
    // Boundary Value Tests
    // ====================================================

    @Test
    public void testBoundaryValues_MaxFloat() {
        DataContainerSimple container = new DataContainerSimple(3, 10);
        
        container.setValue(Float.MAX_VALUE, 1, 0);
        
        assertEquals(Float.MAX_VALUE, container.getValue(1, 0), 1e30);
    }

    @Test
    public void testBoundaryValues_MinFloat() {
        DataContainerSimple container = new DataContainerSimple(3, 10);
        
        container.setValue(-Float.MAX_VALUE, 1, 0);
        
        assertEquals(-Float.MAX_VALUE, container.getValue(1, 0), 1e30);
    }

    @Test
    public void testBoundaryValues_PositiveInfinity() {
        DataContainerSimple container = new DataContainerSimple(3, 10);
        
        container.setValue(Float.POSITIVE_INFINITY, 1, 0);
        
        assertTrue(Float.isInfinite(container.getValue(1, 0)));
    }

    @Test
    public void testBoundaryValues_NegativeInfinity() {
        DataContainerSimple container = new DataContainerSimple(3, 10);
        
        container.setValue(Float.NEGATIVE_INFINITY, 1, 0);
        
        assertTrue(Float.isInfinite(container.getValue(1, 0)));
    }

    @Test
    public void testBoundaryValues_NaN() {
        DataContainerSimple container = new DataContainerSimple(3, 10);
        
        container.setValue(Float.NaN, 1, 0);
        
        assertTrue(Float.isNaN(container.getValue(1, 0)));
    }
}
