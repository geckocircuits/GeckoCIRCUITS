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

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Additional comprehensive tests for SwitchingLossCurve.
 * Focuses on curve data management and parameter access.
 */
public class SwitchingLossCurveAdditionalTest {

    private static final double DELTA = 1e-10;
    private static final double TEMPERATURE_25C = 25.0;
    private static final double TEMPERATURE_50C = 50.0;
    private static final double TEMPERATURE_100C = 100.0;

    private SwitchingLossCurve curve;

    @Before
    public void setUp() {
        curve = new SwitchingLossCurve(TEMPERATURE_25C, 300.0);
    }

    // ====================================================
    // Constructor and Basic Initialization
    // ====================================================

    @Test
    public void testConstructor_InitializesTemperature() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(TEMPERATURE_50C, 300.0);
        assertEquals(TEMPERATURE_50C, testCurve.tj.getDoubleValue(), DELTA);
    }

    @Test
    public void testConstructor_ZeroTemperature() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(0.0, 300.0);
        assertEquals(0.0, testCurve.tj.getDoubleValue(), DELTA);
    }

    @Test
    public void testConstructor_NegativeTemperature() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(-40.0, 300.0);
        assertEquals(-40.0, testCurve.tj.getDoubleValue(), DELTA);
    }

    @Test
    public void testConstructor_HighTemperature() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(200.0, 300.0);
        assertEquals(200.0, testCurve.tj.getDoubleValue(), DELTA);
    }

    // ====================================================
    // getCurveData() Tests
    // ====================================================

    @Test
    public void testGetCurveData_CreatesDeepCopy() {
        // Create curve data
        double[][] originalData = {{1, 2, 3}, {10, 20, 30}};
        curve.setCurveData(originalData);

        // Get data
        double[][] retrievedData = curve.getCurveData();

        // Modify retrieved data
        retrievedData[0][0] = 999;
        retrievedData[1][0] = 999;

        // Original should be unchanged
        double[][] verifyData = curve.getCurveData();
        assertEquals(1.0, verifyData[0][0], DELTA);
        assertEquals(10.0, verifyData[1][0], DELTA);
    }

    @Test
    public void testGetCurveData_DifferentInstance() {
        double[][] data = {{1, 2}, {10, 20}};
        curve.setCurveData(data);

        double[][] retrieved1 = curve.getCurveData();
        double[][] retrieved2 = curve.getCurveData();

        assertNotSame(retrieved1, retrieved2);
    }

    @Test
    public void testGetCurveData_CorrectValues() {
        double[][] data = {{1, 2, 3}, {100, 200, 300}};
        curve.setCurveData(data);

        double[][] retrieved = curve.getCurveData();

        assertArrayEquals(new double[]{1, 2, 3}, retrieved[0], DELTA);
        assertArrayEquals(new double[]{100, 200, 300}, retrieved[1], DELTA);
    }

    @Test
    public void testGetCurveData_SingleRow() {
        double[][] data = {{5}};
        curve.setCurveData(data);

        double[][] retrieved = curve.getCurveData();

        assertEquals(1, retrieved.length);
        assertEquals(1, retrieved[0].length);
        assertEquals(5.0, retrieved[0][0], DELTA);
    }

    @Test
    public void testGetCurveData_LargeArray() {
        double[][] data = new double[10][];
        for (int i = 0; i < 10; i++) {
            data[i] = new double[100];
            for (int j = 0; j < 100; j++) {
                data[i][j] = i * 100 + j;
            }
        }
        curve.setCurveData(data);

        double[][] retrieved = curve.getCurveData();

        assertEquals(10, retrieved.length);
        for (int i = 0; i < 10; i++) {
            assertEquals(100, retrieved[i].length);
            assertEquals((double)(i * 100 + 50), retrieved[i][50], DELTA);
        }
    }

    // ====================================================
    // setCurveData() Tests
    // ====================================================

    @Test
    public void testSetCurveData_SimpleArray() {
        double[][] data = {{1, 2, 3}, {10, 20, 30}};
        curve.setCurveData(data);

        double[][] retrieved = curve.getCurveData();
        assertArrayEquals(new double[]{1, 2, 3}, retrieved[0], DELTA);
        assertArrayEquals(new double[]{10, 20, 30}, retrieved[1], DELTA);
    }

    @Test
    public void testSetCurveData_Overwrites() {
        double[][] data1 = {{1, 2}, {10, 20}};
        curve.setCurveData(data1);

        double[][] data2 = {{5, 6, 7}, {50, 60, 70}};
        curve.setCurveData(data2);

        double[][] retrieved = curve.getCurveData();
        assertArrayEquals(new double[]{5, 6, 7}, retrieved[0], DELTA);
        assertArrayEquals(new double[]{50, 60, 70}, retrieved[1], DELTA);
    }

    @Test
    public void testSetCurveData_UnequalRowLengths() {
        double[][] data = {{1, 2, 3}, {10, 20}}; // Unequal lengths
        curve.setCurveData(data);

        double[][] retrieved = curve.getCurveData();
        assertEquals(3, retrieved[0].length);
        assertEquals(2, retrieved[1].length);
    }

    // ====================================================
    // getName() Tests
    // ====================================================

    @Test
    public void testGetName_Temperature25C() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(25.0, 300.0);
        assertEquals("25°C", testCurve.getName());
    }

    @Test
    public void testGetName_Temperature0C() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(0.0, 300.0);
        assertEquals("0°C", testCurve.getName());
    }

    @Test
    public void testGetName_Temperature100C() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(100.0, 300.0);
        assertEquals("100°C", testCurve.getName());
    }

    @Test
    public void testGetName_NegativeTemperature() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(-40.0, 300.0);
        assertEquals("-40°C", testCurve.getName());
    }

    @Test
    public void testGetName_LargeTemperature() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(1000.0, 300.0);
        assertEquals("1000°C", testCurve.getName());
    }

    @Test
    public void testGetName_FractionalTemperature() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(25.5, 300.0);
        // Name uses integer cast
        assertEquals("25°C", testCurve.getName());
    }

    @Test
    public void testGetName_VerySmallTemperature() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(0.1, 300.0);
        assertEquals("0°C", testCurve.getName());
    }

    // ====================================================
    // Temperature Parameter Tests
    // ====================================================

    @Test
    public void testTjParameter_CanBeModified() {
        double initialTemp = 25.0;
        SwitchingLossCurve testCurve = new SwitchingLossCurve(initialTemp, 300.0);

        testCurve.tj.setValueWithoutUndo(75.0);
        assertEquals(75.0, testCurve.tj.getDoubleValue(), DELTA);
        assertEquals("75°C", testCurve.getName());
    }

    @Test
    public void testTjParameter_ReflectedInName() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(25.0, 300.0);

        testCurve.tj.setValueWithoutUndo(100.0);
        assertEquals("100°C", testCurve.getName());

        testCurve.tj.setValueWithoutUndo(50.0);
        assertEquals("50°C", testCurve.getName());
    }

    // ====================================================
    // Data Integrity Tests
    // ====================================================

    @Test
    public void testDataIntegrity_AfterMultipleCalls() {
        double[][] originalData = {{1, 2, 3}, {10, 20, 30}};
        curve.setCurveData(originalData);

        // Multiple get calls should return consistent values
        for (int i = 0; i < 5; i++) {
            double[][] retrieved = curve.getCurveData();
            assertArrayEquals(new double[]{1, 2, 3}, retrieved[0], DELTA);
            assertArrayEquals(new double[]{10, 20, 30}, retrieved[1], DELTA);
        }
    }

    @Test
    public void testDataIntegrity_ModifyRetrievedData_DoesNotAffectStored() {
        double[][] data = {{1, 2, 3}, {10, 20, 30}};
        curve.setCurveData(data);

        double[][] retrieved1 = curve.getCurveData();
        retrieved1[0][1] = 999;
        retrieved1[1][1] = 999;

        double[][] retrieved2 = curve.getCurveData();
        assertEquals(2.0, retrieved2[0][1], DELTA);
        assertEquals(20.0, retrieved2[1][1], DELTA);
    }

    // ====================================================
    // Edge Cases with Data
    // ====================================================

    @Test
    public void testCurveData_VerySmallValues() {
        double[][] data = {{1e-10, 2e-10}, {1e-9, 2e-9}};
        curve.setCurveData(data);

        double[][] retrieved = curve.getCurveData();
        assertEquals(1e-10, retrieved[0][0], 1e-20);
        assertEquals(1e-9, retrieved[1][0], 1e-18);
    }

    @Test
    public void testCurveData_VeryLargeValues() {
        double[][] data = {{1e10, 2e10}, {1e11, 2e11}};
        curve.setCurveData(data);

        double[][] retrieved = curve.getCurveData();
        assertEquals(1e10, retrieved[0][0], 1e5);
        assertEquals(1e11, retrieved[1][0], 1e6);
    }

    @Test
    public void testCurveData_MixedSignValues() {
        double[][] data = {{-5, 0, 5}, {-50, 0, 50}};
        curve.setCurveData(data);

        double[][] retrieved = curve.getCurveData();
        assertArrayEquals(new double[]{-5, 0, 5}, retrieved[0], DELTA);
        assertArrayEquals(new double[]{-50, 0, 50}, retrieved[1], DELTA);
    }

    // ====================================================
    // Multiple Curve Instances
    // ====================================================

    @Test
    public void testMultipleInstances_Independent() {
        SwitchingLossCurve curve1 = new SwitchingLossCurve(25.0, 300.0);
        SwitchingLossCurve curve2 = new SwitchingLossCurve(75.0, 300.0);

        double[][] data1 = {{1, 2}, {10, 20}};
        double[][] data2 = {{3, 4}, {30, 40}};

        curve1.setCurveData(data1);
        curve2.setCurveData(data2);

        double[][] retrieved1 = curve1.getCurveData();
        double[][] retrieved2 = curve2.getCurveData();

        assertArrayEquals(new double[]{1, 2}, retrieved1[0], DELTA);
        assertArrayEquals(new double[]{3, 4}, retrieved2[0], DELTA);
    }

    // ====================================================
    // Serialization/Export Path Tests
    // ====================================================

    @Test
    public void testExportPath_TemperatureIncluded() {
        curve.setCurveData(new double[][]{{1, 2}, {10, 20}});

        // Temperature should be accessible
        assertEquals(TEMPERATURE_25C, curve.tj.getDoubleValue(), DELTA);
    }

    // ====================================================
    // Stress Tests
    // ====================================================

    @Test
    public void testStressTest_LargeDataArray() {
        double[][] largeData = new double[1000][];
        for (int i = 0; i < 1000; i++) {
            largeData[i] = new double[1000];
            for (int j = 0; j < 1000; j++) {
                largeData[i][j] = Math.sqrt(i * 1000 + j);
            }
        }

        curve.setCurveData(largeData);
        double[][] retrieved = curve.getCurveData();

        assertEquals(1000, retrieved.length);
        assertEquals(Math.sqrt(500500), retrieved[500][500], 0.1);
    }

    @Test
    public void testStressTest_SequentialSetGet() {
        for (int iteration = 0; iteration < 100; iteration++) {
            double[][] data = {{iteration, iteration + 1}, {iteration * 10, (iteration + 1) * 10}};
            curve.setCurveData(data);

            double[][] retrieved = curve.getCurveData();
            assertEquals((double)iteration, retrieved[0][0], DELTA);
        }
    }

}
