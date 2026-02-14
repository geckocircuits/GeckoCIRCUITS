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
package gecko.geckocircuits.circuit.losscalculation;

import gecko.core.circuit.TokenMap;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive test suite for SwitchingLossCurve.
 * Tests constructor initialization, deep copy operations, data management,
 * XML serialization, and the 4-column to 3-column data repair logic.
 */
public class SwitchingLossCurveFullTest {

    private static final double DELTA = 1e-9;
    private static final double TEMPERATURE_25C = 25.0;
    private static final double TEMPERATURE_125C = 125.0;
    private static final double VOLTAGE_300V = 300.0;
    private static final double VOLTAGE_600V = 600.0;

    private SwitchingLossCurve curve;

    @Before
    public void setUp() {
        curve = new SwitchingLossCurve(TEMPERATURE_25C, VOLTAGE_300V);
    }

    // ====================================================
    // Constructor Tests - Verify tj and uBlock initialization
    // ====================================================

    @Test
    public void testConstructor_SetsTjCorrectly() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(TEMPERATURE_125C, VOLTAGE_600V);

        assertEquals("Junction temperature should be set correctly",
                TEMPERATURE_125C, testCurve.tj.getValue(), DELTA);
    }

    @Test
    public void testConstructor_SetsUBlockCorrectly() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(TEMPERATURE_125C, VOLTAGE_600V);

        assertEquals("Blocking voltage should be set correctly",
                VOLTAGE_600V, testCurve._uBlock.getValue(), DELTA);
    }

    @Test
    public void testConstructor_BothParametersSetCorrectly() {
        double expectedTj = 75.0;
        double expectedUBlock = 400.0;

        SwitchingLossCurve testCurve = new SwitchingLossCurve(expectedTj, expectedUBlock);

        assertEquals("Temperature should match constructor parameter",
                expectedTj, testCurve.tj.getValue(), DELTA);
        assertEquals("Voltage should match constructor parameter",
                expectedUBlock, testCurve._uBlock.getValue(), DELTA);
    }

    @Test
    public void testConstructor_ZeroValues() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(0.0, 0.0);

        assertEquals(0.0, testCurve.tj.getValue(), DELTA);
        assertEquals(0.0, testCurve._uBlock.getValue(), DELTA);
    }

    @Test
    public void testConstructor_NegativeTemperature() {
        double negativeTemp = -40.0;
        SwitchingLossCurve testCurve = new SwitchingLossCurve(negativeTemp, VOLTAGE_300V);

        assertEquals("Should accept negative temperature for cryogenic operation",
                negativeTemp, testCurve.tj.getValue(), DELTA);
    }

    @Test
    public void testConstructor_HighTemperatureAndVoltage() {
        double highTemp = 200.0;
        double highVoltage = 1200.0;

        SwitchingLossCurve testCurve = new SwitchingLossCurve(highTemp, highVoltage);

        assertEquals(highTemp, testCurve.tj.getValue(), DELTA);
        assertEquals(highVoltage, testCurve._uBlock.getValue(), DELTA);
    }

    // ====================================================
    // Copy Tests - Verify independent deep copy behavior
    // ====================================================

    @Test
    public void testCopy_CreatesIndependentInstance() {
        curve.setCurveData(new double[][] {
            {0, 10, 20},
            {0, 0.001, 0.004},
            {0, 0.0005, 0.002}
        });

        SwitchingLossCurve copy = curve.copy();

        assertNotNull("Copy should not be null", copy);
        assertNotSame("Copy should be different instance", curve, copy);
    }

    @Test
    public void testCopy_CopiesTjParameter() {
        double originalTj = 100.0;
        curve.tj.setValueWithoutUndo(originalTj);
        // Initialize data to avoid NullPointerException in copy()
        curve.setCurveData(new double[][]{{0}, {0}, {0}});

        SwitchingLossCurve copy = curve.copy();

        assertEquals("Copy should have same temperature",
                originalTj, copy.tj.getValue(), DELTA);
    }

    @Test
    public void testCopy_CopiesUBlockParameter() {
        double originalUBlock = 500.0;
        curve._uBlock.setValueWithoutUndo(originalUBlock);
        // Initialize data to avoid NullPointerException in copy()
        curve.setCurveData(new double[][]{{0}, {0}, {0}});

        SwitchingLossCurve copy = curve.copy();

        assertEquals("Copy should have same blocking voltage",
                originalUBlock, copy._uBlock.getValue(), DELTA);
    }

    @Test
    public void testCopy_CopiesDataDeep() {
        double[][] originalData = {
            {0, 5, 10, 15},
            {0, 0.5, 2.0, 4.5},
            {0, 0.3, 1.2, 2.7}
        };
        curve.setCurveData(originalData);

        SwitchingLossCurve copy = curve.copy();
        double[][] copyData = copy.getCurveData();

        // Verify dimensions
        assertEquals(originalData.length, copyData.length);
        for (int i = 0; i < originalData.length; i++) {
            assertArrayEquals("Row " + i + " should match",
                    originalData[i], copyData[i], DELTA);
        }
    }

    @Test
    public void testCopy_ModifyingCopyDoesNotAffectOriginal() {
        double[][] originalData = {{0, 10}, {0, 1.0}, {0, 0.5}};
        curve.setCurveData(originalData);

        SwitchingLossCurve copy = curve.copy();

        // Modify copy
        copy.tj.setValueWithoutUndo(999.0);
        copy._uBlock.setValueWithoutUndo(888.0);
        copy.setCurveData(new double[][]{{99, 99}, {99, 99}, {99, 99}});

        // Verify original unchanged
        assertEquals(TEMPERATURE_25C, curve.tj.getValue(), DELTA);
        assertEquals(VOLTAGE_300V, curve._uBlock.getValue(), DELTA);
        double[][] originalDataCheck = curve.getCurveData();
        assertEquals(10.0, originalDataCheck[0][1], DELTA);
    }

    @Test
    public void testCopy_ModifyingOriginalDoesNotAffectCopy() {
        double[][] originalData = {{0, 10}, {0, 1.0}, {0, 0.5}};
        curve.setCurveData(originalData);

        SwitchingLossCurve copy = curve.copy();

        // Store copy values
        double copyTj = copy.tj.getValue();
        double copyUBlock = copy._uBlock.getValue();
        double[][] copyData = copy.getCurveData();

        // Modify original
        curve.tj.setValueWithoutUndo(999.0);
        curve._uBlock.setValueWithoutUndo(888.0);
        curve.setCurveData(new double[][]{{99, 99}, {99, 99}, {99, 99}});

        // Verify copy unchanged
        assertEquals(copyTj, copy.tj.getValue(), DELTA);
        assertEquals(copyUBlock, copy._uBlock.getValue(), DELTA);
        double[][] copyDataCheck = copy.getCurveData();
        assertEquals(10.0, copyDataCheck[0][1], DELTA);
    }

    // ====================================================
    // getXMLTag Tests
    // ====================================================

    @Test
    public void testGetXMLTag_ReturnsCorrectTag() {
        String tag = curve.getXMLTag();

        assertNotNull("XML tag should not be null", tag);
        assertEquals("SchaltverlusteMesskurve", tag);
    }

    @Test
    public void testGetXMLTag_ConsistentAcrossInstances() {
        SwitchingLossCurve curve1 = new SwitchingLossCurve(25.0, 300.0);
        SwitchingLossCurve curve2 = new SwitchingLossCurve(125.0, 600.0);

        assertEquals("All instances should return same XML tag",
                curve1.getXMLTag(), curve2.getXMLTag());
    }

    // ====================================================
    // setCurveData/getCurveData Deep Copy Tests
    // ====================================================

    @Test
    public void testSetCurveData_CreatesDeepCopy() {
        double[][] originalData = {
            {0, 5, 10},
            {0, 0.5, 2.0},
            {0, 0.3, 1.2}
        };

        curve.setCurveData(originalData);

        // Modify original array
        originalData[0][1] = 999.0;
        originalData[1][1] = 888.0;

        // Curve data should be unchanged
        double[][] curveData = curve.getCurveData();
        assertEquals(5.0, curveData[0][1], DELTA);
        assertEquals(0.5, curveData[1][1], DELTA);
    }

    @Test
    public void testGetCurveData_ReturnsDeepCopy() {
        double[][] originalData = {
            {0, 5, 10},
            {0, 0.5, 2.0},
            {0, 0.3, 1.2}
        };
        curve.setCurveData(originalData);

        double[][] retrieved = curve.getCurveData();

        // Modify retrieved array
        retrieved[0][1] = 999.0;
        retrieved[1][1] = 888.0;

        // Get data again - should be unchanged
        double[][] secondRetrieval = curve.getCurveData();
        assertEquals(5.0, secondRetrieval[0][1], DELTA);
        assertEquals(0.5, secondRetrieval[1][1], DELTA);
    }

    @Test
    public void testGetCurveData_MultipleCalls_ReturnDifferentInstances() {
        curve.setCurveData(new double[][]{{1, 2}, {10, 20}, {100, 200}});

        double[][] first = curve.getCurveData();
        double[][] second = curve.getCurveData();

        assertNotSame("Each call should return new instance", first, second);
    }

    @Test
    public void testSetGetCurveData_TypicalIGBTData() {
        // Typical IGBT switching loss curve: I[A], Eon[mJ], Eoff[mJ]
        double[][] igbtData = {
            {0, 25, 50, 75, 100, 125},       // Current
            {0, 2.5, 6.0, 10.5, 16.0, 22.5},  // Eon
            {0, 1.5, 4.0, 7.5, 12.0, 17.5}   // Eoff
        };

        curve.setCurveData(igbtData);
        double[][] retrieved = curve.getCurveData();

        assertEquals("Should have 3 rows", 3, retrieved.length);
        assertEquals("Should have 6 columns", 6, retrieved[0].length);

        for (int i = 0; i < igbtData.length; i++) {
            assertArrayEquals("Row " + i + " should match",
                    igbtData[i], retrieved[i], DELTA);
        }
    }

    @Test
    public void testCurveData_EmptyArray() {
        double[][] emptyData = new double[3][0];

        curve.setCurveData(emptyData);
        double[][] retrieved = curve.getCurveData();

        assertEquals(3, retrieved.length);
        assertEquals(0, retrieved[0].length);
    }

    @Test
    public void testCurveData_SinglePoint() {
        double[][] singlePoint = {{0}, {0}, {0}};

        curve.setCurveData(singlePoint);
        double[][] retrieved = curve.getCurveData();

        assertEquals(3, retrieved.length);
        assertEquals(1, retrieved[0].length);
        assertEquals(0.0, retrieved[0][0], DELTA);
    }

    @Test
    public void testCurveData_JaggedArray() {
        double[][] jaggedData = {
            {0, 10, 20, 30},
            {0, 1.0},
            {0, 0.5, 1.5}
        };

        curve.setCurveData(jaggedData);
        double[][] retrieved = curve.getCurveData();

        assertEquals(4, retrieved[0].length);
        assertEquals(2, retrieved[1].length);
        assertEquals(3, retrieved[2].length);
    }

    // ====================================================
    // getName Tests - Temperature string formatting
    // ====================================================

    @Test
    public void testGetName_FormatsTemperatureCorrectly() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(25.0, 300.0);

        String name = testCurve.getName();

        assertNotNull("Name should not be null", name);
        assertEquals("25°C", name);
    }

    @Test
    public void testGetName_TruncatesDecimalPart() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(125.7, 300.0);

        String name = testCurve.getName();

        assertEquals("125°C", name);
    }

    @Test
    public void testGetName_RoundsDown() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(99.9, 300.0);

        String name = testCurve.getName();

        assertEquals("99°C", name);
    }

    @Test
    public void testGetName_NegativeTemperature() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(-40.0, 300.0);

        String name = testCurve.getName();

        assertEquals("-40°C", name);
    }

    @Test
    public void testGetName_ZeroTemperature() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(0.0, 300.0);

        String name = testCurve.getName();

        assertEquals("0°C", name);
    }

    @Test
    public void testGetName_HighTemperature() {
        SwitchingLossCurve testCurve = new SwitchingLossCurve(150.0, 300.0);

        String name = testCurve.getName();

        assertEquals("150°C", name);
    }

    @Test
    public void testGetName_UpdatesWhenTemperatureChanged() {
        curve.tj.setValueWithoutUndo(50.0);
        assertEquals("50°C", curve.getName());

        curve.tj.setValueWithoutUndo(100.0);
        assertEquals("100°C", curve.getName());
    }

    // ====================================================
    // Data Repair Logic Tests - 4-column to 3-column
    // ====================================================

    @Test
    public void testImportIndividual_Repairs4ColumnDataTo3Column() {
        // Create a TokenMap with 4-column data (legacy format)
        // Format: I, Eon, Eoff, Etotal (4th column needs to be removed)
        String[] asciiLines = {
            "uBlock 400.0",
            "data 0.0 0.0 0.0 0.0",
            "data 10.0 1.0 0.5 1.5",
            "data 20.0 2.0 1.0 3.0",
            "tj 125.0"
        };

        // Manually set up 4-column data
        curve.data = new double[4][3];
        curve.data[0] = new double[]{0.0, 10.0, 20.0};
        curve.data[1] = new double[]{0.0, 1.0, 2.0};
        curve.data[2] = new double[]{0.0, 0.5, 1.0};
        curve.data[3] = new double[]{0.0, 1.5, 3.0};  // 4th row (Etotal)

        TokenMap tokenMap = new TokenMap(asciiLines);

        // Call the protected importIndividual method via reflection or through importASCII
        curve.importIndividual(tokenMap);

        // After repair, should have only 3 rows
        assertNotNull("Data should not be null after import", curve.data);
        assertEquals("Should have 3 rows after repair", 3, curve.data.length);
        assertEquals("First row should have 3 columns", 3, curve.data[0].length);

        // Verify first 3 rows are preserved
        assertEquals(0.0, curve.data[0][0], DELTA);
        assertEquals(10.0, curve.data[0][1], DELTA);
        assertEquals(20.0, curve.data[0][2], DELTA);

        assertEquals(0.0, curve.data[1][0], DELTA);
        assertEquals(1.0, curve.data[1][1], DELTA);
        assertEquals(2.0, curve.data[1][2], DELTA);

        assertEquals(0.0, curve.data[2][0], DELTA);
        assertEquals(0.5, curve.data[2][1], DELTA);
        assertEquals(1.0, curve.data[2][2], DELTA);
    }

    @Test
    public void testImportIndividual_Leaves3ColumnDataUnchanged() {
        // Create a TokenMap with correct 3-column data
        String[] asciiLines = {
            "uBlock 400.0",
            "data 0.0 0.0 0.0",
            "data 10.0 1.0 0.5",
            "data 20.0 2.0 1.0",
            "tj 125.0"
        };

        // Set up correct 3-column data
        curve.data = new double[3][3];
        curve.data[0] = new double[]{0.0, 10.0, 20.0};
        curve.data[1] = new double[]{0.0, 1.0, 2.0};
        curve.data[2] = new double[]{0.0, 0.5, 1.0};

        TokenMap tokenMap = new TokenMap(asciiLines);

        curve.importIndividual(tokenMap);

        // Should remain 3 rows
        assertEquals("Should still have 3 rows", 3, curve.data.length);
        assertEquals("First row should still have 3 columns", 3, curve.data[0].length);

        // Data should be unchanged
        assertEquals(0.0, curve.data[0][0], DELTA);
        assertEquals(10.0, curve.data[0][1], DELTA);
        assertEquals(20.0, curve.data[0][2], DELTA);
    }

    @Test
    public void testImportIndividual_ReadsUBlockParameter() {
        String[] asciiLines = {
            "uBlock 550.0",
            "tj 75.0"
        };

        curve.data = new double[3][2];  // Initialize with valid 3-column data

        TokenMap tokenMap = new TokenMap(asciiLines);

        curve.importIndividual(tokenMap);

        assertEquals("uBlock should be read from TokenMap",
                550.0, curve._uBlock.getValue(), DELTA);
    }

    @Test
    public void testDataRepair_LargeDataset() {
        // Test with larger 4-column dataset
        int numPoints = 10;
        curve.data = new double[4][numPoints];

        // Populate 4 rows
        for (int col = 0; col < numPoints; col++) {
            curve.data[0][col] = col * 10.0;      // Current
            curve.data[1][col] = col * 1.0;       // Eon
            curve.data[2][col] = col * 0.5;       // Eoff
            curve.data[3][col] = col * 1.5;       // Etotal (to be removed)
        }

        String[] asciiLines = {"uBlock 300.0", "tj 100.0"};
        TokenMap tokenMap = new TokenMap(asciiLines);

        curve.importIndividual(tokenMap);

        assertEquals("Should have 3 rows after repair", 3, curve.data.length);
        assertEquals("Should have " + numPoints + " columns", numPoints, curve.data[0].length);

        // Verify first 3 rows preserved correctly
        for (int col = 0; col < numPoints; col++) {
            assertEquals(col * 10.0, curve.data[0][col], DELTA);
            assertEquals(col * 1.0, curve.data[1][col], DELTA);
            assertEquals(col * 0.5, curve.data[2][col], DELTA);
        }
    }

    // ====================================================
    // Multiple Blocking Voltage Tests
    // ====================================================

    @Test
    public void testMultipleBlockingVoltages_LowVoltage() {
        SwitchingLossCurve lowVoltageCurve = new SwitchingLossCurve(25.0, 100.0);

        assertEquals(100.0, lowVoltageCurve._uBlock.getValue(), DELTA);
    }

    @Test
    public void testMultipleBlockingVoltages_MediumVoltage() {
        SwitchingLossCurve mediumVoltageCurve = new SwitchingLossCurve(25.0, 600.0);

        assertEquals(600.0, mediumVoltageCurve._uBlock.getValue(), DELTA);
    }

    @Test
    public void testMultipleBlockingVoltages_HighVoltage() {
        SwitchingLossCurve highVoltageCurve = new SwitchingLossCurve(25.0, 1200.0);

        assertEquals(1200.0, highVoltageCurve._uBlock.getValue(), DELTA);
    }

    @Test
    public void testMultipleBlockingVoltages_IndependentInstances() {
        SwitchingLossCurve curve300V = new SwitchingLossCurve(25.0, 300.0);
        SwitchingLossCurve curve600V = new SwitchingLossCurve(25.0, 600.0);
        SwitchingLossCurve curve900V = new SwitchingLossCurve(25.0, 900.0);

        assertEquals(300.0, curve300V._uBlock.getValue(), DELTA);
        assertEquals(600.0, curve600V._uBlock.getValue(), DELTA);
        assertEquals(900.0, curve900V._uBlock.getValue(), DELTA);
    }

    // ====================================================
    // Null/Edge Case Handling
    // ====================================================

    @Test
    public void testCurveData_NullSafety_AfterConstruction() {
        SwitchingLossCurve newCurve = new SwitchingLossCurve(25.0, 300.0);

        // Data may be null initially - this is OK
        // Just verify we can set data without error
        double[][] data = {{0, 10}, {0, 1.0}, {0, 0.5}};
        newCurve.setCurveData(data);

        assertNotNull("Data should not be null after setting", newCurve.data);
    }

    @Test
    public void testExportASCII_ProducesValidOutput() {
        curve.setCurveData(new double[][]{
            {0, 10, 20},
            {0, 1.0, 2.0},
            {0, 0.5, 1.0}
        });
        curve.tj.setValueWithoutUndo(100.0);
        curve._uBlock.setValueWithoutUndo(400.0);

        StringBuffer ascii = new StringBuffer();
        curve.exportASCII(ascii);

        String output = ascii.toString();

        assertNotNull("Output should not be null", output);
        assertTrue("Should contain opening tag",
                output.contains("<SchaltverlusteMesskurve>"));
        assertTrue("Should contain closing tag",
                output.contains("<\\SchaltverlusteMesskurve>"));
        assertTrue("Should contain data section",
                output.contains("data"));
    }

    // ====================================================
    // Physical Realism Tests
    // ====================================================

    @Test
    public void testPhysicalRealism_TypicalIGBTLosses() {
        // 600V IGBT at 125°C junction temperature
        SwitchingLossCurve igbtCurve = new SwitchingLossCurve(125.0, 600.0);

        // Realistic data: I[A], Eon[mJ], Eoff[mJ]
        double[][] igbtData = {
            {0, 50, 100, 150, 200},
            {0, 5.0, 12.0, 21.0, 32.0},   // Eon increases with current
            {0, 3.0, 8.0, 15.0, 24.0}     // Eoff increases with current
        };

        igbtCurve.setCurveData(igbtData);

        assertEquals(125.0, igbtCurve.tj.getValue(), DELTA);
        assertEquals(600.0, igbtCurve._uBlock.getValue(), DELTA);
        assertEquals("125°C", igbtCurve.getName());

        double[][] retrieved = igbtCurve.getCurveData();

        // Verify Eon > Eoff (typical for IGBTs due to tail current)
        for (int i = 1; i < retrieved[0].length; i++) {
            assertTrue("Eon should be >= Eoff at current index " + i,
                    retrieved[1][i] >= retrieved[2][i]);
        }
    }

    @Test
    public void testPhysicalRealism_TypicalMOSFETLosses() {
        // 100V MOSFET at room temperature
        SwitchingLossCurve mosfetCurve = new SwitchingLossCurve(25.0, 100.0);

        // MOSFETs typically have lower switching losses than IGBTs
        double[][] mosfetData = {
            {0, 10, 20, 30},
            {0, 0.2, 0.8, 1.8},    // Eon in mJ
            {0, 0.15, 0.6, 1.35}   // Eoff in mJ
        };

        mosfetCurve.setCurveData(mosfetData);

        assertEquals(25.0, mosfetCurve.tj.getValue(), DELTA);
        assertEquals(100.0, mosfetCurve._uBlock.getValue(), DELTA);

        double[][] retrieved = mosfetCurve.getCurveData();

        // Verify losses are relatively low (typical for MOSFETs)
        assertTrue("MOSFET Eon at 30A should be < 2mJ",
                retrieved[1][3] < 2.0);
    }

    @Test
    public void testTemperatureEffect_HigherTempMayHaveHigherLosses() {
        // Create two curves at different temperatures
        SwitchingLossCurve coldCurve = new SwitchingLossCurve(25.0, 600.0);
        SwitchingLossCurve hotCurve = new SwitchingLossCurve(150.0, 600.0);

        assertNotEquals("Curves should have different temperatures",
                coldCurve.getName(), hotCurve.getName());

        assertEquals("25°C", coldCurve.getName());
        assertEquals("150°C", hotCurve.getName());
    }
}
