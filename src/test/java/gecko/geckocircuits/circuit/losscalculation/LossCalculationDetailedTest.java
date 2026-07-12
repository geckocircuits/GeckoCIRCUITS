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

import gecko.core.circuit.losscalculation.ConductionLossMeasurementCurve;
import gecko.core.circuit.losscalculation.SwitchingLossCurve;
import gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import gecko.geckocircuits.circuit.circuitcomponents.Diode;
import gecko.core.circuit.losscalculation.AbstractLossCalculator;
import gecko.core.circuit.losscalculation.LossCalculationSplittable;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for LossCalculationDetailed and its inner class LossCalculatorDetailed.
 * Tests the detailed loss calculation with measured loss curves (conduction and switching).
 *
 * Tests cover:
 * - Constructor initialization with default curves
 * - Copy methods for loss curves (deep copy verification)
 * - setzeNeueParameter for updating curves
 * - copyPropertiesFrom for cloning instances
 * - exportASCII for serialization
 * - getFiles() for file management
 * - LossCalculatorDetailed.calcLosses with temperature handling
 * - LossCalculatorDetailed switching loss calculations
 * - LossCalculatorDetailed conduction loss calculations
 * - calculateRelativeVoltageFactor
 */
public class LossCalculationDetailedTest {

    private static final double TOLERANCE = 1e-10;

    private Diode testDiode;
    private LossCalculationDetailed verlustBerechnung;

    @Before
    public void setUp() {
        // Create a Diode instance (simplest semiconductor)
        testDiode = new Diode();

        // Get LossCalculationDetailed through the LossProperties
        verlustBerechnung = testDiode.getVerlustBerechnung().getDetailedLosses();
    }

    // ===========================================
    // Constructor and Initialization Tests
    // ===========================================

    @Test
    public void testConstructor_InitializesDefaultSwitchingCurves() {
        List<SwitchingLossCurve> switchCurves = verlustBerechnung.getCopyOfSwitchingLossMeasurementCurvesArray();

        assertNotNull("Switching curves should not be null", switchCurves);
        assertEquals("Should have 2 default switching curves", 2, switchCurves.size());
    }

    @Test
    public void testConstructor_InitializesDefaultConductionCurves() {
        List<ConductionLossMeasurementCurve> condCurves = verlustBerechnung.getCopyOfConductionLossMeasurementCurvesArray();

        assertNotNull("Conduction curves should not be null", condCurves);
        assertEquals("Should have 2 default conduction curves", 2, condCurves.size());
    }

    @Test
    public void testConstructor_SwitchingCurve25C() {
        List<SwitchingLossCurve> switchCurves = verlustBerechnung.getCopyOfSwitchingLossMeasurementCurvesArray();

        SwitchingLossCurve curve25 = switchCurves.get(0);
        assertEquals("First curve should be at 25°C", 25.0, curve25.tj.getValue(), TOLERANCE);
        assertEquals("First curve should be at 400V", 400.0, curve25._uBlock.getValue(), TOLERANCE);
    }

    @Test
    public void testConstructor_SwitchingCurve110C() {
        List<SwitchingLossCurve> switchCurves = verlustBerechnung.getCopyOfSwitchingLossMeasurementCurvesArray();

        SwitchingLossCurve curve110 = switchCurves.get(1);
        assertEquals("Second curve should be at 110°C", 110.0, curve110.tj.getValue(), TOLERANCE);
        assertEquals("Second curve should be at 400V", 400.0, curve110._uBlock.getValue(), TOLERANCE);
    }

    @Test
    public void testConstructor_SwitchingCurveData25C() {
        List<SwitchingLossCurve> switchCurves = verlustBerechnung.getCopyOfSwitchingLossMeasurementCurvesArray();

        SwitchingLossCurve curve25 = switchCurves.get(0);
        double[][] data = curve25.getCurveData();

        assertNotNull("Curve data should not be null", data);
        assertEquals("Should have 3 rows (I, Eon, Eoff)", 3, data.length);
        assertEquals("Should have 3 columns", 3, data[0].length);

        // Check current values
        assertArrayEquals(new double[]{0, 10, 12}, data[0], TOLERANCE);
        // Check Eon values
        assertArrayEquals(new double[]{0, 10e-3, 12e-3}, data[1], TOLERANCE);
        // Check Eoff values
        assertArrayEquals(new double[]{0, 5e-3, 7e-3}, data[2], TOLERANCE);
    }

    @Test
    public void testConstructor_SwitchingCurveData110C() {
        List<SwitchingLossCurve> switchCurves = verlustBerechnung.getCopyOfSwitchingLossMeasurementCurvesArray();

        SwitchingLossCurve curve110 = switchCurves.get(1);
        double[][] data = curve110.getCurveData();

        assertNotNull("Curve data should not be null", data);
        assertEquals("Should have 3 rows (I, Eon, Eoff)", 3, data.length);
        assertEquals("Should have 3 columns", 3, data[0].length);

        // Check current values
        assertArrayEquals(new double[]{0, 5, 9}, data[0], TOLERANCE);
        // Check Eon values
        assertArrayEquals(new double[]{0, 6e-3, 12e-3}, data[1], TOLERANCE);
        // Check Eoff values
        assertArrayEquals(new double[]{0, 4e-3, 7e-3}, data[2], TOLERANCE);
    }

    @Test
    public void testConstructor_ConductionCurve25C() {
        List<ConductionLossMeasurementCurve> condCurves = verlustBerechnung.getCopyOfConductionLossMeasurementCurvesArray();

        ConductionLossMeasurementCurve curve25 = condCurves.get(0);
        assertEquals("First curve should be at 25°C", 25.0, curve25.tj.getValue(), TOLERANCE);
    }

    @Test
    public void testConstructor_ConductionCurve115C() {
        List<ConductionLossMeasurementCurve> condCurves = verlustBerechnung.getCopyOfConductionLossMeasurementCurvesArray();

        ConductionLossMeasurementCurve curve115 = condCurves.get(1);
        assertEquals("Second curve should be at 115°C", 115.0, curve115.tj.getValue(), TOLERANCE);
    }

    @Test
    public void testConstructor_ConductionCurveData25C() {
        List<ConductionLossMeasurementCurve> condCurves = verlustBerechnung.getCopyOfConductionLossMeasurementCurvesArray();

        ConductionLossMeasurementCurve curve25 = condCurves.get(0);
        double[][] data = curve25.getCurveData();

        assertNotNull("Curve data should not be null", data);
        assertEquals("Should have 2 rows (V, I)", 2, data.length);
        assertEquals("Should have 3 columns", 3, data[0].length);

        // Check voltage values
        assertArrayEquals(new double[]{0, 0.7, 1.7}, data[0], TOLERANCE);
        // Check current values
        assertArrayEquals(new double[]{0, 0.5, 7}, data[1], TOLERANCE);
    }

    @Test
    public void testConstructor_ConductionCurveData115C() {
        List<ConductionLossMeasurementCurve> condCurves = verlustBerechnung.getCopyOfConductionLossMeasurementCurvesArray();

        ConductionLossMeasurementCurve curve115 = condCurves.get(1);
        double[][] data = curve115.getCurveData();

        assertNotNull("Curve data should not be null", data);
        assertEquals("Should have 2 rows (V, I)", 2, data.length);
        assertEquals("Should have 3 columns", 3, data[0].length);

        // Check voltage values
        assertArrayEquals(new double[]{0, 0.8, 2.1}, data[0], TOLERANCE);
        // Check current values
        assertArrayEquals(new double[]{0, 0.6, 8}, data[1], TOLERANCE);
    }

    // ===========================================
    // Copy Methods Tests
    // ===========================================

    @Test
    public void testGetCopyOfLeitverlusteMesskurvenArray_ReturnsDeepCopy() {
        List<ConductionLossMeasurementCurve> copy1 = verlustBerechnung.getCopyOfConductionLossMeasurementCurvesArray();
        List<ConductionLossMeasurementCurve> copy2 = verlustBerechnung.getCopyOfConductionLossMeasurementCurvesArray();

        assertNotSame("Should return different list instances", copy1, copy2);
        assertEquals("Should have same number of curves", copy1.size(), copy2.size());
    }

    @Test
    public void testGetCopyOfLeitverlusteMesskurvenArray_ModifyingCopyDoesNotAffectOriginal() {
        List<ConductionLossMeasurementCurve> copy = verlustBerechnung.getCopyOfConductionLossMeasurementCurvesArray();

        // Modify the copy
        ConductionLossMeasurementCurve firstCurve = copy.get(0);
        double originalTj = firstCurve.tj.getValue();
        firstCurve.tj.setValueWithoutUndo(999.0);

        // Get a new copy and verify original is unchanged
        List<ConductionLossMeasurementCurve> newCopy = verlustBerechnung.getCopyOfConductionLossMeasurementCurvesArray();
        assertEquals("Original should not be affected", originalTj, newCopy.get(0).tj.getValue(), TOLERANCE);
    }

    @Test
    public void testGetCopyOfLeitverlusteMesskurvenArray_DataIsDeepCopied() {
        List<ConductionLossMeasurementCurve> copy = verlustBerechnung.getCopyOfConductionLossMeasurementCurvesArray();

        // Modify the data in the copy
        double[][] copyData = copy.get(0).getCurveData();
        double originalValue = copyData[0][0];
        copyData[0][0] = 999.0;

        // Get a new copy and verify original data is unchanged
        List<ConductionLossMeasurementCurve> newCopy = verlustBerechnung.getCopyOfConductionLossMeasurementCurvesArray();
        double[][] newCopyData = newCopy.get(0).getCurveData();
        assertEquals("Original data should not be affected", originalValue, newCopyData[0][0], TOLERANCE);
    }

    @Test
    public void testGetCopyOfSchaltverlusteMesskurvenArray_ReturnsDeepCopy() {
        List<SwitchingLossCurve> copy1 = verlustBerechnung.getCopyOfSwitchingLossMeasurementCurvesArray();
        List<SwitchingLossCurve> copy2 = verlustBerechnung.getCopyOfSwitchingLossMeasurementCurvesArray();

        assertNotSame("Should return different list instances", copy1, copy2);
        assertEquals("Should have same number of curves", copy1.size(), copy2.size());
    }

    @Test
    public void testGetCopyOfSchaltverlusteMesskurvenArray_ModifyingCopyDoesNotAffectOriginal() {
        List<SwitchingLossCurve> copy = verlustBerechnung.getCopyOfSwitchingLossMeasurementCurvesArray();

        // Modify the copy
        SwitchingLossCurve firstCurve = copy.get(0);
        double originalTj = firstCurve.tj.getValue();
        firstCurve.tj.setValueWithoutUndo(999.0);

        // Get a new copy and verify original is unchanged
        List<SwitchingLossCurve> newCopy = verlustBerechnung.getCopyOfSwitchingLossMeasurementCurvesArray();
        assertEquals("Original should not be affected", originalTj, newCopy.get(0).tj.getValue(), TOLERANCE);
    }

    @Test
    public void testGetCopyOfSchaltverlusteMesskurvenArray_DataIsDeepCopied() {
        List<SwitchingLossCurve> copy = verlustBerechnung.getCopyOfSwitchingLossMeasurementCurvesArray();

        // Modify the data in the copy
        double[][] copyData = copy.get(0).getCurveData();
        double originalValue = copyData[0][0];
        copyData[0][0] = 999.0;

        // Get a new copy and verify original data is unchanged
        List<SwitchingLossCurve> newCopy = verlustBerechnung.getCopyOfSwitchingLossMeasurementCurvesArray();
        double[][] newCopyData = newCopy.get(0).getCurveData();
        assertEquals("Original data should not be affected", originalValue, newCopyData[0][0], TOLERANCE);
    }

    // ===========================================
    // setzeNeueParameter Tests
    // ===========================================

    @Test
    public void testSetzeNeueParameter_ReplacesSwitchingCurves() {
        SwitchingLossCurve newCurve = new SwitchingLossCurve(50.0, 600.0);
        newCurve.setCurveData(new double[][]{{0, 5}, {0, 5e-3}, {0, 3e-3}});
        List<SwitchingLossCurve> newCurves = List.of(newCurve);

        verlustBerechnung.setzeNeueParameter(newCurves, verlustBerechnung.getCopyOfConductionLossMeasurementCurvesArray());

        List<SwitchingLossCurve> result = verlustBerechnung.getCopyOfSwitchingLossMeasurementCurvesArray();
        assertEquals("Should have 1 switching curve", 1, result.size());
        assertEquals("Temperature should be 50°C", 50.0, result.get(0).tj.getValue(), TOLERANCE);
        assertEquals("Voltage should be 600V", 600.0, result.get(0)._uBlock.getValue(), TOLERANCE);
    }

    @Test
    public void testSetzeNeueParameter_ReplacesConductionCurves() {
        ConductionLossMeasurementCurve newCurve = new ConductionLossMeasurementCurve(75.0);
        newCurve.setCurveData(new double[][]{{0, 1.0}, {0, 2.0}});
        List<ConductionLossMeasurementCurve> newCurves = List.of(newCurve);

        verlustBerechnung.setzeNeueParameter(verlustBerechnung.getCopyOfSwitchingLossMeasurementCurvesArray(), newCurves);

        List<ConductionLossMeasurementCurve> result = verlustBerechnung.getCopyOfConductionLossMeasurementCurvesArray();
        assertEquals("Should have 1 conduction curve", 1, result.size());
        assertEquals("Temperature should be 75°C", 75.0, result.get(0).tj.getValue(), TOLERANCE);
    }

    @Test
    public void testSetzeNeueParameter_UpdatesBothCurveSets() {
        SwitchingLossCurve switchCurve1 = new SwitchingLossCurve(50.0, 600.0);
        switchCurve1.setCurveData(new double[][]{{0, 5}, {0, 5e-3}, {0, 3e-3}});
        SwitchingLossCurve switchCurve2 = new SwitchingLossCurve(150.0, 600.0);
        switchCurve2.setCurveData(new double[][]{{0, 5}, {0, 6e-3}, {0, 4e-3}});
        List<SwitchingLossCurve> newSwitchCurves = List.of(switchCurve1, switchCurve2);

        ConductionLossMeasurementCurve condCurve1 = new ConductionLossMeasurementCurve(75.0);
        condCurve1.setCurveData(new double[][]{{0, 1.0}, {0, 2.0}});
        ConductionLossMeasurementCurve condCurve2 = new ConductionLossMeasurementCurve(125.0);
        condCurve2.setCurveData(new double[][]{{0, 1.1}, {0, 2.2}});
        ConductionLossMeasurementCurve condCurve3 = new ConductionLossMeasurementCurve(175.0);
        condCurve3.setCurveData(new double[][]{{0, 1.2}, {0, 2.4}});
        List<ConductionLossMeasurementCurve> newCondCurves = List.of(condCurve1, condCurve2, condCurve3);

        verlustBerechnung.setzeNeueParameter(newSwitchCurves, newCondCurves);

        assertEquals("Should have 2 switching curves", 2,
            verlustBerechnung.getCopyOfSwitchingLossMeasurementCurvesArray().size());
        assertEquals("Should have 3 conduction curves", 3,
            verlustBerechnung.getCopyOfConductionLossMeasurementCurvesArray().size());
    }

    // ===========================================
    // copyPropertiesFrom Tests
    // ===========================================

    @Test
    public void testCopyPropertiesFrom_CopiesSwitchingCurves() throws Exception {
        // Create another diode with different curves
        Diode sourceDiode = new Diode();
        LossCalculationDetailed source = sourceDiode.getVerlustBerechnung().getDetailedLosses();

        SwitchingLossCurve newCurve = new SwitchingLossCurve(50.0, 600.0);
        newCurve.setCurveData(new double[][]{{0, 5}, {0, 5e-3}, {0, 3e-3}});
        List<SwitchingLossCurve> newCurves = List.of(newCurve);
        source.setzeNeueParameter(newCurves, source.getCopyOfConductionLossMeasurementCurvesArray());

        // Note: copyPropertiesFrom ADDS curves to existing ones (doesn't clear first)
        int originalCount = verlustBerechnung.getCopyOfSwitchingLossMeasurementCurvesArray().size();

        // Copy properties
        verlustBerechnung.copyPropertiesFrom(source);

        List<SwitchingLossCurve> result = verlustBerechnung.getCopyOfSwitchingLossMeasurementCurvesArray();
        assertEquals("Should have added 1 curve to existing curves", originalCount + 1, result.size());

        // Find the copied curve (last one added)
        SwitchingLossCurve lastCurve = result.get(result.size() - 1);
        assertEquals("Temperature should match", 50.0, lastCurve.tj.getValue(), TOLERANCE);
    }

    @Test
    public void testCopyPropertiesFrom_CopiesConductionCurves() throws Exception {
        // Create another diode with different curves
        Diode sourceDiode = new Diode();
        LossCalculationDetailed source = sourceDiode.getVerlustBerechnung().getDetailedLosses();

        ConductionLossMeasurementCurve newCurve = new ConductionLossMeasurementCurve(75.0);
        newCurve.setCurveData(new double[][]{{0, 1.0}, {0, 2.0}});
        List<ConductionLossMeasurementCurve> newCurves = List.of(newCurve);
        source.setzeNeueParameter(source.getCopyOfSwitchingLossMeasurementCurvesArray(), newCurves);

        // Note: copyPropertiesFrom ADDS curves to existing ones (doesn't clear first)
        int originalCount = verlustBerechnung.getCopyOfConductionLossMeasurementCurvesArray().size();

        // Copy properties
        verlustBerechnung.copyPropertiesFrom(source);

        List<ConductionLossMeasurementCurve> result = verlustBerechnung.getCopyOfConductionLossMeasurementCurvesArray();
        assertEquals("Should have added 1 curve to existing curves", originalCount + 1, result.size());

        // Find the copied curve (last one added)
        ConductionLossMeasurementCurve lastCurve = result.get(result.size() - 1);
        assertEquals("Temperature should match", 75.0, lastCurve.tj.getValue(), TOLERANCE);
    }

    // ===========================================
    // lossCalculatorFabric Tests
    // ===========================================

    @Test
    public void testLossCalculatorFabric_ReturnsLossCalculatorDetailed() {
        AbstractLossCalculator calculator = verlustBerechnung.lossCalculatorFabric();

        assertNotNull("Calculator should not be null", calculator);
        assertTrue("Should be instance of LossCalculatorDetailed",
            calculator.getClass().getName().contains("LossCalculatorDetailed"));
    }

    @Test
    public void testLossCalculatorFabric_ReturnsNewInstanceEachTime() {
        AbstractLossCalculator calc1 = verlustBerechnung.lossCalculatorFabric();
        AbstractLossCalculator calc2 = verlustBerechnung.lossCalculatorFabric();

        assertNotSame("Should return new instance each time", calc1, calc2);
    }

    // ===========================================
    // exportASCII Tests
    // ===========================================

    @Test
    public void testExportASCII_ProducesNonEmptyOutput() {
        StringBuffer buffer = new StringBuffer();
        verlustBerechnung.exportASCII(buffer);

        assertTrue("Should produce non-empty output", buffer.length() > 0);
    }

    @Test
    public void testExportASCII_ContainsDatnamGemesseneVerluste() {
        StringBuffer buffer = new StringBuffer();
        verlustBerechnung.exportASCII(buffer);

        assertTrue("Should contain _measuredLossesFilename",
            buffer.toString().contains("_measuredLossesFilename"));
    }

    @Test
    public void testExportASCII_ContainsLossFileHashValue() {
        StringBuffer buffer = new StringBuffer();
        verlustBerechnung.exportASCII(buffer);

        assertTrue("Should contain lossFileHashValue",
            buffer.toString().contains("lossFileHashValue"));
    }

    @Test
    public void testExportASCII_WithNullLossFile() {
        // Default state has null lossFile
        StringBuffer buffer = new StringBuffer();
        verlustBerechnung.exportASCII(buffer);

        // Should complete without exception
        assertTrue("Should produce output", buffer.length() > 0);
    }

    // ===========================================
    // getFiles Tests
    // ===========================================

    @Test
    public void testGetFiles_ReturnsListWithNullLossFile() {
        List<?> files = verlustBerechnung.getFiles();

        assertNotNull("Should return non-null list", files);
        assertEquals("Should have one element", 1, files.size());
    }

    @Test
    public void testGetFiles_InitiallyContainsNull() {
        List<?> files = verlustBerechnung.getFiles();

        assertNull("Initial lossFile should be null", files.get(0));
    }

    // ===========================================
    // LossCalculatorDetailed Tests
    // ===========================================

    @Test
    public void testLossCalculatorDetailed_CalcLossesWithNormalTemperature() throws Exception {
        AbstractLossCalculator calculator = verlustBerechnung.lossCalculatorFabric();

        // Set voltage using reflection (parent._voltage)
        setParentVoltage(testDiode, 100.0);

        double current = 5.0;
        double temperature = 70.0;  // Normal temperature between 25°C and 115°C
        double deltaT = 0.0001;

        // Should not throw exception
        calculator.calcLosses(current, temperature, deltaT);

        double totalLoss = calculator.getTotalLosses();
        assertTrue("Total loss should be non-negative", totalLoss >= 0);
    }

    @Test
    public void testLossCalculatorDetailed_CalcLossesWithLargeTemperature() throws Exception {
        AbstractLossCalculator calculator = verlustBerechnung.lossCalculatorFabric();

        setParentVoltage(testDiode, 100.0);

        double current = 5.0;
        double temperature = 1e5;  // Temperature > 1E4 should be clamped
        double deltaT = 0.0001;

        calculator.calcLosses(current, temperature, deltaT);

        // Should complete without exception and use default temperature
        double totalLoss = calculator.getTotalLosses();
        assertTrue("Should calculate losses with clamped temperature", totalLoss >= 0);
    }

    @Test
    public void testLossCalculatorDetailed_CalcLossesWithNaNTemperature() throws Exception {
        AbstractLossCalculator calculator = verlustBerechnung.lossCalculatorFabric();

        setParentVoltage(testDiode, 100.0);

        double current = 5.0;
        double temperature = Double.NaN;  // NaN should be clamped to default
        double deltaT = 0.0001;

        calculator.calcLosses(current, temperature, deltaT);

        // Should complete without exception
        double totalLoss = calculator.getTotalLosses();
        assertFalse("Total loss should not be NaN", Double.isNaN(totalLoss));
    }

    @Test
    public void testLossCalculatorDetailed_CalculateRelativeVoltageFactor_PositiveVoltage() throws Exception {
        AbstractLossCalculator calculator = verlustBerechnung.lossCalculatorFabric();

        // Use reflection to call calculateRelativeVoltageFactor
        double voltage = 400.0;
        double factor = calculateRelativeVoltageFactor(calculator, voltage);

        assertEquals("Should return absolute voltage", 400.0, factor, TOLERANCE);
    }

    @Test
    public void testLossCalculatorDetailed_CalculateRelativeVoltageFactor_NegativeVoltage() throws Exception {
        AbstractLossCalculator calculator = verlustBerechnung.lossCalculatorFabric();

        double voltage = -300.0;
        double factor = calculateRelativeVoltageFactor(calculator, voltage);

        assertEquals("Should return absolute value", 300.0, factor, TOLERANCE);
    }

    @Test
    public void testLossCalculatorDetailed_CalculateRelativeVoltageFactor_ZeroVoltage() throws Exception {
        AbstractLossCalculator calculator = verlustBerechnung.lossCalculatorFabric();

        double voltage = 0.0;
        double factor = calculateRelativeVoltageFactor(calculator, voltage);

        assertEquals("Should return zero", 0.0, factor, TOLERANCE);
    }

    @Test
    public void testLossCalculatorDetailed_ConductionLoss() throws Exception {
        AbstractLossCalculator calculator = verlustBerechnung.lossCalculatorFabric();

        setParentVoltage(testDiode, 100.0);

        // Use small current in the range of the default curve
        double current = 1.0;
        double temperature = 25.0;  // Use 25°C (first curve)
        double deltaT = 0.0001;

        calculator.calcLosses(current, temperature, deltaT);

        // Check if it implements LossCalculationSplittable
        if (calculator instanceof LossCalculationSplittable) {
            LossCalculationSplittable splittable = (LossCalculationSplittable) calculator;
            double conductionLoss = splittable.getConductionLoss();
            assertTrue("Conduction loss should be non-negative", conductionLoss >= 0);
        }
    }

    @Test
    public void testLossCalculatorDetailed_TurnOnSwitchingLoss() throws Exception {
        AbstractLossCalculator calculator = verlustBerechnung.lossCalculatorFabric();

        setParentVoltage(testDiode, 400.0);

        double temperature = 25.0;
        double deltaT = 0.0001;

        // Simulate turn-on: oldCurrent ~ 0, newCurrent > 0
        calculator.calcLosses(0.0, temperature, deltaT);  // Initialize with zero
        calculator.calcLosses(5.0, temperature, deltaT);  // Turn on with 5A

        if (calculator instanceof LossCalculationSplittable) {
            LossCalculationSplittable splittable = (LossCalculationSplittable) calculator;
            double switchingLoss = splittable.getSwitchingLoss();
            assertTrue("Switching loss should be non-negative", switchingLoss >= 0);
        }
    }

    @Test
    public void testLossCalculatorDetailed_TurnOffSwitchingLoss() throws Exception {
        AbstractLossCalculator calculator = verlustBerechnung.lossCalculatorFabric();

        setParentVoltage(testDiode, 400.0);

        double temperature = 25.0;
        double deltaT = 0.0001;

        // Simulate turn-off: oldCurrent > 0, newCurrent ~ 0
        calculator.calcLosses(5.0, temperature, deltaT);  // Initialize with 5A
        calculator.calcLosses(0.0, temperature, deltaT);  // Turn off

        if (calculator instanceof LossCalculationSplittable) {
            LossCalculationSplittable splittable = (LossCalculationSplittable) calculator;
            double switchingLoss = splittable.getSwitchingLoss();
            assertTrue("Switching loss should be non-negative", switchingLoss >= 0);
        }
    }

    @Test
    public void testLossCalculatorDetailed_TotalLosses() throws Exception {
        AbstractLossCalculator calculator = verlustBerechnung.lossCalculatorFabric();

        setParentVoltage(testDiode, 400.0);

        double current = 5.0;
        double temperature = 70.0;
        double deltaT = 0.0001;

        calculator.calcLosses(current, temperature, deltaT);

        double totalLoss = calculator.getTotalLosses();
        assertTrue("Total loss should be non-negative", totalLoss >= 0);

        if (calculator instanceof LossCalculationSplittable) {
            LossCalculationSplittable splittable = (LossCalculationSplittable) calculator;
            double conductionLoss = splittable.getConductionLoss();
            double switchingLoss = splittable.getSwitchingLoss();

            assertEquals("Total should equal conduction + switching",
                conductionLoss + switchingLoss, totalLoss, TOLERANCE);
        }
    }

    @Test
    public void testLossCalculatorDetailed_ImplementsLossCalculationSplittable() {
        AbstractLossCalculator calculator = verlustBerechnung.lossCalculatorFabric();

        assertTrue("Should implement LossCalculationSplittable",
            calculator instanceof LossCalculationSplittable);
    }

    @Test
    public void testLossCalculatorDetailed_DefaultReferenceVoltage() throws Exception {
        AbstractLossCalculator calculator = verlustBerechnung.lossCalculatorFabric();

        // Access DEFAULT_REFERENCE_VOLTAGE constant
        Class<?> innerClass = null;
        for (Class<?> declaredClass : LossCalculationDetailed.class.getDeclaredClasses()) {
            if (declaredClass.getSimpleName().equals("LossCalculatorDetailed")) {
                innerClass = declaredClass;
                break;
            }
        }

        assertNotNull("Should find LossCalculatorDetailed inner class", innerClass);

        Field field = innerClass.getDeclaredField("DEFAULT_REFERENCE_VOLTAGE");
        double defaultRefVoltage = field.getDouble(null);

        assertEquals("Default reference voltage should be 100V", 100.0, defaultRefVoltage, TOLERANCE);
    }

    @Test
    public void testLossCalculatorDetailed_ZeroCurrent() throws Exception {
        AbstractLossCalculator calculator = verlustBerechnung.lossCalculatorFabric();

        setParentVoltage(testDiode, 400.0);

        calculator.calcLosses(0.0, 70.0, 0.0001);

        if (calculator instanceof LossCalculationSplittable) {
            LossCalculationSplittable splittable = (LossCalculationSplittable) calculator;
            double conductionLoss = splittable.getConductionLoss();

            // With zero current, conduction loss should be zero
            assertEquals("Conduction loss should be zero with zero current", 0.0, conductionLoss, TOLERANCE);
        }
    }

    @Test
    public void testLossCalculatorDetailed_HighCurrent() throws Exception {
        AbstractLossCalculator calculator = verlustBerechnung.lossCalculatorFabric();

        setParentVoltage(testDiode, 400.0);

        // Use high current (within default curve range)
        calculator.calcLosses(8.0, 70.0, 0.0001);

        double totalLoss = calculator.getTotalLosses();
        assertTrue("Should handle high current", totalLoss >= 0);
    }

    // ===========================================
    // Helper Methods
    // ===========================================

    /**
     * Sets the _voltage field of the parent component using reflection.
     * This is necessary because _voltage is typically set by the circuit solver.
     */
    private void setParentVoltage(AbstractCircuitBlockInterface parent, double voltage) throws Exception {
        Field voltageField = AbstractCircuitBlockInterface.class.getDeclaredField("_voltage");
        voltageField.setAccessible(true);
        voltageField.set(parent, voltage);
    }

    /**
     * Calls the calculateRelativeVoltageFactor method using reflection.
     */
    private double calculateRelativeVoltageFactor(AbstractLossCalculator calculator, double voltage) throws Exception {
        java.lang.reflect.Method method = calculator.getClass()
            .getDeclaredMethod("calculateRelativeVoltageFactor", double.class);
        method.setAccessible(true);
        return (Double) method.invoke(calculator, voltage);
    }
}
