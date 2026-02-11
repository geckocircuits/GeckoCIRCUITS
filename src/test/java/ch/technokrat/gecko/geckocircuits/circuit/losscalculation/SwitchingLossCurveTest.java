/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for SwitchingLossCurve - measured switching loss curves at specific temperatures.
 * Switching loss curves store Eon/Eoff energy vs current at specific voltage and temperature.
 */
public class SwitchingLossCurveTest {

    private static final double DELTA = 1e-6;

    // ====================================================
    // Constructor Tests
    // ====================================================

    @Test
    public void testConstructor_BasicParameters() {
        SwitchingLossCurve curve = new SwitchingLossCurve(25.0, 300.0);
        
        assertEquals(25.0, curve.tj.getValue(), DELTA);
        assertEquals(300.0, curve._uBlock.getValue(), DELTA);
    }

    @Test
    public void testConstructor_HighTemperature() {
        SwitchingLossCurve curve = new SwitchingLossCurve(150.0, 600.0);
        
        assertEquals(150.0, curve.tj.getValue(), DELTA);
        assertEquals(600.0, curve._uBlock.getValue(), DELTA);
    }

    @Test
    public void testConstructor_LowVoltage() {
        SwitchingLossCurve curve = new SwitchingLossCurve(25.0, 50.0);
        
        assertEquals(50.0, curve._uBlock.getValue(), DELTA);
    }

    // ====================================================
    // Copy Tests
    // ====================================================

    @Test
    public void testCopy_BasicProperties() {
        SwitchingLossCurve original = new SwitchingLossCurve(100.0, 400.0);
        original.setCurveData(new double[][] {
            {0, 10, 20},
            {0, 0.001, 0.004},
            {0, 0.0005, 0.002}
        });
        
        SwitchingLossCurve copy = original.copy();
        
        assertEquals(original.tj.getValue(), copy.tj.getValue(), DELTA);
        assertEquals(original._uBlock.getValue(), copy._uBlock.getValue(), DELTA);
    }

    @Test
    public void testCopy_DataCopied() {
        SwitchingLossCurve original = new SwitchingLossCurve(25.0, 300.0);
        double[][] data = {
            {0, 10, 20, 30},
            {0, 0.001, 0.004, 0.009},
            {0, 0.0005, 0.002, 0.0045}
        };
        original.setCurveData(data);
        
        SwitchingLossCurve copy = original.copy();
        double[][] copyData = copy.getCurveData();
        
        assertEquals(data.length, copyData.length);
        for (int i = 0; i < data.length; i++) {
            assertArrayEquals(data[i], copyData[i], DELTA);
        }
    }

    @Test
    public void testCopy_Independence() {
        SwitchingLossCurve original = new SwitchingLossCurve(25.0, 300.0);
        double[][] data = {{0, 10}, {0, 0.001}, {0, 0.0005}};
        original.setCurveData(data);
        
        SwitchingLossCurve copy = original.copy();
        
        // Modify copy
        double[][] modifiedData = {{0, 20}, {0, 0.002}, {0, 0.001}};
        copy.setCurveData(modifiedData);
        
        // Original should be unchanged
        double[][] originalData = original.getCurveData();
        assertEquals(10.0, originalData[0][1], DELTA);
    }

    // ====================================================
    // CurveData Tests
    // ====================================================

    @Test
    public void testSetGetCurveData_Basic() {
        SwitchingLossCurve curve = new SwitchingLossCurve(25.0, 300.0);
        
        // Typical data: I, Eon, Eoff
        double[][] data = {
            {0, 5, 10, 15, 20},      // Current [A]
            {0, 0.5, 2.0, 4.5, 8.0}, // Eon [mJ]
            {0, 0.3, 1.2, 2.7, 4.8}  // Eoff [mJ]
        };
        
        curve.setCurveData(data);
        double[][] retrieved = curve.getCurveData();
        
        assertEquals(3, retrieved.length);
        assertEquals(5, retrieved[0].length);
        
        for (int i = 0; i < data.length; i++) {
            assertArrayEquals(data[i], retrieved[i], DELTA);
        }
    }

    @Test
    public void testSetCurveData_DeepCopy() {
        SwitchingLossCurve curve = new SwitchingLossCurve(25.0, 300.0);
        double[][] data = {{0, 10}, {0, 1.0}, {0, 0.5}};
        
        curve.setCurveData(data);
        
        // Modify original array
        data[0][1] = 999;
        
        // Curve data should be unchanged
        double[][] retrieved = curve.getCurveData();
        assertEquals(10.0, retrieved[0][1], DELTA);
    }

    @Test
    public void testGetCurveData_DeepCopy() {
        SwitchingLossCurve curve = new SwitchingLossCurve(25.0, 300.0);
        double[][] data = {{0, 10}, {0, 1.0}, {0, 0.5}};
        curve.setCurveData(data);
        
        // Get and modify
        double[][] retrieved = curve.getCurveData();
        retrieved[0][1] = 999;
        
        // Original should be unchanged
        double[][] secondRetrieval = curve.getCurveData();
        assertEquals(10.0, secondRetrieval[0][1], DELTA);
    }

    // ====================================================
    // GetName Tests
    // ====================================================

    @Test
    public void testGetName_RoomTemperature() {
        SwitchingLossCurve curve = new SwitchingLossCurve(25.0, 300.0);
        
        String name = curve.getName();
        
        assertNotNull(name);
        assertTrue(name.contains("25"));
        assertTrue(name.contains("°C"));
    }

    @Test
    public void testGetName_HighTemperature() {
        SwitchingLossCurve curve = new SwitchingLossCurve(150.0, 300.0);
        
        String name = curve.getName();
        
        assertTrue(name.contains("150"));
    }

    @Test
    public void testGetName_IntegerTruncation() {
        // Temperature 125.7 should display as 125°C
        SwitchingLossCurve curve = new SwitchingLossCurve(125.7, 300.0);
        
        String name = curve.getName();
        
        assertTrue(name.contains("125"));
    }

    // ====================================================
    // Physical Value Tests
    // ====================================================

    @Test
    public void testTypicalIGBTCurve() {
        // Typical IGBT switching loss curve
        SwitchingLossCurve curve = new SwitchingLossCurve(125.0, 600.0);
        
        // Data: Current, Eon, Eoff (at 600V, 125°C)
        double[][] data = {
            {0, 25, 50, 75, 100, 125, 150},          // I [A]
            {0, 2.5, 6.0, 10.5, 16.0, 22.5, 30.0},   // Eon [mJ]
            {0, 1.5, 4.0, 7.5, 12.0, 17.5, 24.0}     // Eoff [mJ]
        };
        
        curve.setCurveData(data);
        
        double[][] retrieved = curve.getCurveData();
        
        // Verify Eon > Eoff for this typical IGBT (turn-on usually higher)
        for (int i = 1; i < retrieved[0].length; i++) {
            assertTrue("Eon should be > Eoff at index " + i,
                retrieved[1][i] >= retrieved[2][i]);
        }
    }

    @Test
    public void testTypicalMOSFETCurve() {
        // Typical MOSFET switching loss curve (lower losses than IGBT)
        SwitchingLossCurve curve = new SwitchingLossCurve(25.0, 100.0);
        
        double[][] data = {
            {0, 5, 10, 15, 20},       // I [A]
            {0, 0.05, 0.2, 0.45, 0.8}, // Eon [mJ]
            {0, 0.03, 0.12, 0.27, 0.48} // Eoff [mJ]
        };
        
        curve.setCurveData(data);
        
        // MOSFET losses are typically much lower than IGBT
        double[][] retrieved = curve.getCurveData();
        assertTrue(retrieved[1][4] < 1.0);  // Eon < 1mJ at 20A
    }

    // ====================================================
    // XML Tag Test
    // ====================================================

    @Test
    public void testGetXMLTag() {
        SwitchingLossCurve curve = new SwitchingLossCurve(25.0, 300.0);
        
        String tag = curve.getXMLTag();
        
        assertNotNull(tag);
        assertEquals("SchaltverlusteMesskurve", tag);
    }

    // ====================================================
    // Edge Cases
    // ====================================================

    @Test
    public void testEmptyData() {
        SwitchingLossCurve curve = new SwitchingLossCurve(25.0, 300.0);
        double[][] data = new double[3][0];
        
        curve.setCurveData(data);
        double[][] retrieved = curve.getCurveData();
        
        assertEquals(3, retrieved.length);
        assertEquals(0, retrieved[0].length);
    }

    @Test
    public void testNegativeTemperature() {
        // Cold operation
        SwitchingLossCurve curve = new SwitchingLossCurve(-40.0, 300.0);
        
        assertEquals(-40.0, curve.tj.getValue(), DELTA);
    }

    @Test
    public void testZeroVoltage() {
        SwitchingLossCurve curve = new SwitchingLossCurve(25.0, 0.0);
        
        assertEquals(0.0, curve._uBlock.getValue(), DELTA);
    }
}
