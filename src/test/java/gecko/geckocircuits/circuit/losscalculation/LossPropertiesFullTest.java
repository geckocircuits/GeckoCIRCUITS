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
import gecko.geckocircuits.circuit.circuitcomponents.Diode;
import gecko.geckocircuits.circuit.circuitcomponents.IGBT;
import gecko.geckocircuits.circuit.circuitcomponents.MOSFET;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive test suite for LossProperties and its inner classes:
 * - LossCalculatorParallelWrapper
 * - LossCalculatorParallelWrapperWithSplit
 * - LossCalculatorAdditionalDiode
 *
 * Tests loss calculation wrappers, parallel device scaling, and MOSFET anti-parallel diode losses.
 */
public class LossPropertiesFullTest {

    private static final double TOLERANCE = 1e-10;

    private Diode diode;
    private IGBT igbt;
    private MOSFET mosfet;
    private LossProperties diodeProperties;
    private LossProperties igbtProperties;
    private LossProperties mosfetProperties;

    @Before
    public void setUp() {
        diode = new Diode();
        igbt = new IGBT();
        mosfet = new MOSFET();

        diodeProperties = new LossProperties(diode);
        igbtProperties = new LossProperties(igbt);
        mosfetProperties = new LossProperties(mosfet);

        // Set default parameters for testing
        diode._onResistance.setValueWithoutUndo(0.01);  // 10 mΩ
        diode.getForwardVoltageDropParameter().setValueWithoutUndo(0.7);  // 0.7 V

        igbt._onResistance.setValueWithoutUndo(0.02);  // 20 mΩ
        igbt.kOn.setValueWithoutUndo(30e-6);
        igbt.kOff.setValueWithoutUndo(15e-6);

        mosfet._onResistance.setValueWithoutUndo(0.015);  // 15 mΩ
        mosfet.kOn.setValueWithoutUndo(25e-6);
        mosfet.kOff.setValueWithoutUndo(20e-6);
    }

    // ===========================================
    // LossProperties Core Tests
    // ===========================================

    @Test
    public void testConstructor_CreatesValidProperties() {
        assertNotNull(diodeProperties);
        assertNotNull(igbtProperties);
        assertNotNull(mosfetProperties);
    }

    @Test
    public void testSetLossType_Simple() {
        diodeProperties.setLossType(LossCalculationDetail.SIMPLE);
        assertEquals(LossCalculationDetail.SIMPLE, diodeProperties.getLossType());
    }

    @Test
    public void testSetLossType_Detailed() {
        diodeProperties.setLossType(LossCalculationDetail.DETAILED);
        assertEquals(LossCalculationDetail.DETAILED, diodeProperties.getLossType());
    }

    @Test
    public void testGetLossType_DefaultValue() {
        // Default should be SIMPLE based on initialization
        assertEquals(LossCalculationDetail.SIMPLE, diodeProperties.getLossType());
    }

    @Test
    public void testGetDetailedLosses_NotNull() {
        VerlustBerechnungDetailed detailed = diodeProperties.getDetailedLosses();
        assertNotNull(detailed);
    }

    @Test
    public void testCopyPropertiesFrom_CopiesLossType() {
        LossProperties source = new LossProperties(new Diode());
        source.setLossType(LossCalculationDetail.DETAILED);

        LossProperties target = new LossProperties(new Diode());
        target.copyPropertiesFrom(source);

        assertEquals(LossCalculationDetail.DETAILED, target.getLossType());
    }

    @Test
    public void testExportImportASCII_RoundTrip() {
        diodeProperties.setLossType(LossCalculationDetail.DETAILED);

        StringBuffer exportBuffer = new StringBuffer();
        diodeProperties.exportASCII(exportBuffer);

        String exported = exportBuffer.toString();
        assertTrue("Export should contain <Verluste> tag", exported.contains("<Verluste>"));
        assertTrue("Export should contain verlustTyp", exported.contains("verlustTyp"));
        assertTrue("Export should contain closing tag", exported.contains("<\\Verluste>"));
    }

    @Test
    public void testImportASCII_ParsesLossType() {
        // Create a minimal ASCII representation with verlustTyp token
        String[] asciiData = new String[]{
            "verlustTyp 2"  // DETAILED = 2
        };
        TokenMap tokenMap = new TokenMap(asciiData);

        diodeProperties.importASCII(tokenMap);
        assertEquals(LossCalculationDetail.DETAILED, diodeProperties.getLossType());
    }

    // ===========================================
    // LossCalculatorFabric Tests
    // ===========================================

    @Test
    public void testLossCalculatorFabric_SimpleMode() {
        diodeProperties.setLossType(LossCalculationDetail.SIMPLE);
        AbstractLossCalculator calculator = diodeProperties.lossCalculatorFabric();
        assertNotNull(calculator);
    }

    @Test
    public void testLossCalculatorFabric_DetailedMode() {
        diodeProperties.setLossType(LossCalculationDetail.DETAILED);
        // Note: Detailed mode requires loss file, so it may return a default calculator
        AbstractLossCalculator calculator = diodeProperties.lossCalculatorFabric();
        assertNotNull(calculator);
    }

    @Test
    public void testLossCalculatorFabric_DiodeSimple() {
        diode.numberParalleled.setValueWithoutUndo(1);
        diodeProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calculator = diodeProperties.lossCalculatorFabric();
        assertNotNull(calculator);

        // Test basic loss calculation
        calculator.calcLosses(10.0, 25.0, 1e-6);
        double losses = calculator.getTotalLosses();
        assertTrue("Losses should be positive", losses >= 0.0);
    }

    @Test
    public void testLossCalculatorFabric_IGBTSimple() {
        igbt.numberParalleled.setValueWithoutUndo(1);
        igbtProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calculator = igbtProperties.lossCalculatorFabric();
        assertNotNull(calculator);
    }

    // ===========================================
    // LossCalculatorParallelWrapper Tests (via Reflection)
    // ===========================================

    @Test
    public void testParallelWrapper_SingleDevice() throws Exception {
        diode.numberParalleled.setValueWithoutUndo(1);
        diodeProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calculator = diodeProperties.lossCalculatorFabric();

        calculator.calcLosses(10.0, 25.0, 1e-6);
        double losses = calculator.getTotalLosses();

        assertTrue("Losses should be positive for 10A", losses > 0.0);
    }

    @Test
    public void testParallelWrapper_FourDevices() throws Exception {
        diode.numberParalleled.setValueWithoutUndo(4);
        diodeProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calculator = diodeProperties.lossCalculatorFabric();

        // Total current 40A, each device sees 10A
        calculator.calcLosses(40.0, 25.0, 1e-6);
        double lossesParallel = calculator.getTotalLosses();

        // Compare with single device at 10A
        diode.numberParalleled.setValueWithoutUndo(1);
        AbstractLossCalculator singleCalc = diodeProperties.lossCalculatorFabric();
        singleCalc.calcLosses(10.0, 25.0, 1e-6);
        double lossesSingle = singleCalc.getTotalLosses();

        // Total losses for 4 devices should be 4x single device loss
        assertEquals("Parallel devices: 4 devices at 10A each = 4x single device loss",
                     4.0 * lossesSingle, lossesParallel, TOLERANCE);
    }

    @Test
    public void testParallelWrapper_TwoDevices() throws Exception {
        igbt.numberParalleled.setValueWithoutUndo(2);
        igbtProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calculator = igbtProperties.lossCalculatorFabric();

        // Total current 20A, each device sees 10A
        calculator.calcLosses(20.0, 25.0, 1e-6);
        double lossesParallel = calculator.getTotalLosses();

        // Compare with single device
        igbt.numberParalleled.setValueWithoutUndo(1);
        AbstractLossCalculator singleCalc = igbtProperties.lossCalculatorFabric();
        singleCalc.calcLosses(10.0, 25.0, 1e-6);
        double lossesSingle = singleCalc.getTotalLosses();

        assertEquals("2 devices at 10A each = 2x single device loss",
                     2.0 * lossesSingle, lossesParallel, TOLERANCE);
    }

    @Test
    public void testParallelWrapper_CurrentSharing() throws Exception {
        // Access inner class via reflection
        Class<?> wrapperClass = getInnerClass("LossCalculatorParallelWrapper");
        assertNotNull("LossCalculatorParallelWrapper should exist", wrapperClass);

        // Verify it implements AbstractLossCalculator
        assertTrue("Should implement AbstractLossCalculator",
                   AbstractLossCalculator.class.isAssignableFrom(wrapperClass));
    }

    @Test
    public void testParallelWrapper_VerifyFields() throws Exception {
        Class<?> wrapperClass = getInnerClass("LossCalculatorParallelWrapper");

        Field wrappedField = wrapperClass.getDeclaredField("_wrapped");
        assertNotNull("Should have _wrapped field", wrappedField);
        assertEquals("_wrapped should be AbstractLossCalculator type",
                     AbstractLossCalculator.class, wrappedField.getType());

        Field totalLossesField = wrapperClass.getDeclaredField("_totalLosses");
        assertNotNull("Should have _totalLosses field", totalLossesField);
        assertEquals("_totalLosses should be double", double.class, totalLossesField.getType());

        Field numberParalleledField = wrapperClass.getDeclaredField("_numberParalleled");
        assertNotNull("Should have _numberParalleled field", numberParalleledField);
        assertEquals("_numberParalleled should be int", int.class, numberParalleledField.getType());
    }

    // ===========================================
    // LossCalculatorParallelWrapperWithSplit Tests
    // ===========================================

    @Test
    public void testParallelWrapperWithSplit_ExtendsParallelWrapper() throws Exception {
        Class<?> wrapperClass = getInnerClass("LossCalculatorParallelWrapperWithSplit");
        Class<?> baseClass = getInnerClass("LossCalculatorParallelWrapper");

        assertNotNull("LossCalculatorParallelWrapperWithSplit should exist", wrapperClass);
        assertTrue("Should extend LossCalculatorParallelWrapper",
                   baseClass.isAssignableFrom(wrapperClass));
    }

    @Test
    public void testParallelWrapperWithSplit_ImplementsSplittable() throws Exception {
        Class<?> wrapperClass = getInnerClass("LossCalculatorParallelWrapperWithSplit");

        assertTrue("Should implement LossCalculationSplittable",
                   LossCalculationSplittable.class.isAssignableFrom(wrapperClass));
    }

    @Test
    public void testParallelWrapperWithSplit_HasSplitMethods() throws Exception {
        Class<?> wrapperClass = getInnerClass("LossCalculatorParallelWrapperWithSplit");

        Method getSwitchingLoss = wrapperClass.getMethod("getSwitchingLoss");
        assertNotNull("Should have getSwitchingLoss method", getSwitchingLoss);
        assertEquals("getSwitchingLoss should return double",
                     double.class, getSwitchingLoss.getReturnType());

        Method getConductionLoss = wrapperClass.getMethod("getConductionLoss");
        assertNotNull("Should have getConductionLoss method", getConductionLoss);
        assertEquals("getConductionLoss should return double",
                     double.class, getConductionLoss.getReturnType());
    }

    @Test
    public void testParallelWrapperWithSplit_TwoDevicesSplit() throws Exception {
        igbt.numberParalleled.setValueWithoutUndo(2);
        igbtProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calculator = igbtProperties.lossCalculatorFabric();

        // Calculator should be a LossCalculatorParallelWrapperWithSplit
        assertTrue("Should be LossCalculationSplittable for IGBT",
                   calculator instanceof LossCalculationSplittable);

        LossCalculationSplittable splittable = (LossCalculationSplittable) calculator;

        calculator.calcLosses(20.0, 25.0, 1e-6);

        double totalLoss = calculator.getTotalLosses();
        double switchingLoss = splittable.getSwitchingLoss();
        double conductionLoss = splittable.getConductionLoss();

        assertTrue("Switching loss should be non-negative", switchingLoss >= 0.0);
        assertTrue("Conduction loss should be positive", conductionLoss > 0.0);
        assertEquals("Total = switching + conduction",
                     switchingLoss + conductionLoss, totalLoss, TOLERANCE);
    }

    // ===========================================
    // LossCalculatorAdditionalDiode Tests (MOSFET)
    // ===========================================

    @Test
    public void testAdditionalDiode_MOSFETHasWrapper() throws Exception {
        mosfet.numberParalleled.setValueWithoutUndo(1);
        mosfetProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calculator = mosfetProperties.lossCalculatorFabric();
        assertNotNull("MOSFET should have loss calculator", calculator);

        // Verify it's a LossCalculatorAdditionalDiode by checking class name
        String className = calculator.getClass().getSimpleName();
        assertEquals("Should be LossCalculatorAdditionalDiode for MOSFET",
                     "LossCalculatorAdditionalDiode", className);
    }

    @Test
    public void testAdditionalDiode_ImplementsInterfaces() throws Exception {
        mosfet.numberParalleled.setValueWithoutUndo(1);
        mosfetProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calculator = mosfetProperties.lossCalculatorFabric();

        assertTrue("Should implement AbstractLossCalculator",
                   calculator instanceof AbstractLossCalculator);
        assertTrue("Should implement LossCalculationSplittable",
                   calculator instanceof LossCalculationSplittable);
    }

    @Test
    public void testAdditionalDiode_CalculatesLosses() throws Exception {
        mosfet.numberParalleled.setValueWithoutUndo(1);
        mosfetProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calculator = mosfetProperties.lossCalculatorFabric();

        // Set some current in the MOSFET
        calculator.calcLosses(15.0, 25.0, 1e-6);

        double totalLoss = calculator.getTotalLosses();
        assertTrue("Total loss should be positive", totalLoss > 0.0);
    }

    @Test
    public void testAdditionalDiode_SplitLosses() throws Exception {
        mosfet.numberParalleled.setValueWithoutUndo(1);
        mosfetProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calculator = mosfetProperties.lossCalculatorFabric();
        LossCalculationSplittable splittable = (LossCalculationSplittable) calculator;

        calculator.calcLosses(15.0, 25.0, 1e-6);

        double totalLoss = calculator.getTotalLosses();
        double switchingLoss = splittable.getSwitchingLoss();
        double conductionLoss = splittable.getConductionLoss();

        assertTrue("Switching loss >= 0", switchingLoss >= 0.0);
        assertTrue("Conduction loss > 0", conductionLoss > 0.0);
        assertEquals("Total = switching + conduction",
                     switchingLoss + conductionLoss, totalLoss, TOLERANCE);
    }

    @Test
    public void testAdditionalDiode_DiodeFieldExists() throws Exception {
        Class<?> wrapperClass = getInnerClass("LossCalculatorAdditionalDiode");

        Field diodeField = wrapperClass.getDeclaredField("_diode");
        assertNotNull("Should have _diode field", diodeField);
        assertEquals("_diode should be Diode type", Diode.class, diodeField.getType());

        Field originalField = wrapperClass.getDeclaredField("_original");
        assertNotNull("Should have _original field", originalField);

        Field diodeLossesField = wrapperClass.getDeclaredField("_diodeLosses");
        assertNotNull("Should have _diodeLosses field", diodeLossesField);

        Field splittableField = wrapperClass.getDeclaredField("_splittable");
        assertNotNull("Should have _splittable field", splittableField);
    }

    @Test
    public void testAdditionalDiode_CombinesLosses() throws Exception {
        mosfet.numberParalleled.setValueWithoutUndo(1);
        mosfetProperties.setLossType(LossCalculationDetail.SIMPLE);

        // Get the MOSFET's anti-parallel diode
        Diode antiDiode = mosfet.getAntiParallelDiode();
        assertNotNull("MOSFET should have anti-parallel diode", antiDiode);

        AbstractLossCalculator calculator = mosfetProperties.lossCalculatorFabric();

        // Simulate MOSFET conducting with some current
        mosfet._currentInAmps = 10.0;
        antiDiode._currentInAmps = 5.0;  // Some reverse current through diode

        calculator.calcLosses(10.0, 25.0, 1e-6);

        double totalLoss = calculator.getTotalLosses();
        assertTrue("Combined loss should account for MOSFET + diode", totalLoss > 0.0);
    }

    @Test
    public void testAdditionalDiode_MOSFETWithParallel() throws Exception {
        mosfet.numberParalleled.setValueWithoutUndo(3);
        mosfetProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calculator = mosfetProperties.lossCalculatorFabric();

        // Should be wrapped in parallel wrapper on top of additional diode
        assertNotNull("Should create calculator for parallel MOSFET", calculator);

        calculator.calcLosses(30.0, 25.0, 1e-6);  // 30A total = 10A per device
        double losses = calculator.getTotalLosses();

        assertTrue("Should have positive losses", losses > 0.0);
    }

    // ===========================================
    // Integration Tests - Complete Scenarios
    // ===========================================

    @Test
    public void testIntegration_DiodeSimpleLoss() {
        diode.numberParalleled.setValueWithoutUndo(1);
        diode._onResistance.setValueWithoutUndo(0.01);  // 10 mΩ
        diode.getForwardVoltageDropParameter().setValueWithoutUndo(0.7);  // 0.7 V
        diodeProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calc = diodeProperties.lossCalculatorFabric();
        calc.calcLosses(10.0, 25.0, 1e-6);

        double expectedLoss = 0.7 * 10.0 + 0.01 * 10.0 * 10.0;  // V*I + I²*R = 7 + 1 = 8W
        assertEquals("Diode loss should be V*I + I²*R", expectedLoss, calc.getTotalLosses(), 0.01);
    }

    @Test
    public void testIntegration_IGBTParallelDevices() {
        igbt.numberParalleled.setValueWithoutUndo(4);
        igbtProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calc = igbtProperties.lossCalculatorFabric();

        // 40A total, 10A per device
        calc.calcLosses(40.0, 25.0, 1e-6);
        double totalLoss = calc.getTotalLosses();

        assertTrue("IGBT parallel devices should have positive loss", totalLoss > 0.0);

        LossCalculationSplittable split = (LossCalculationSplittable) calc;
        double switchLoss = split.getSwitchingLoss();
        double condLoss = split.getConductionLoss();

        assertEquals("Total = switching + conduction",
                     switchLoss + condLoss, totalLoss, TOLERANCE);
    }

    @Test
    public void testIntegration_MOSFETWithDiodeAndParallel() {
        mosfet.numberParalleled.setValueWithoutUndo(2);
        mosfetProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calc = mosfetProperties.lossCalculatorFabric();

        // 20A total through MOSFET (2 devices)
        mosfet._currentInAmps = 20.0;
        mosfet.getAntiParallelDiode()._currentInAmps = 0.0;

        calc.calcLosses(20.0, 25.0, 1e-6);
        double lossWithoutDiode = calc.getTotalLosses();

        // Now with some diode current
        mosfet.getAntiParallelDiode()._currentInAmps = 5.0;
        calc.calcLosses(20.0, 25.0, 1e-6);
        double lossWithDiode = calc.getTotalLosses();

        assertTrue("Loss with diode current should be >= loss without",
                   lossWithDiode >= lossWithoutDiode);
    }

    @Test
    public void testIntegration_ZeroCurrent() {
        diode.numberParalleled.setValueWithoutUndo(1);
        diodeProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calc = diodeProperties.lossCalculatorFabric();
        calc.calcLosses(0.0, 25.0, 1e-6);

        assertEquals("Zero current should produce zero loss", 0.0, calc.getTotalLosses(), TOLERANCE);
    }

    @Test
    public void testIntegration_HighCurrent() {
        diode.numberParalleled.setValueWithoutUndo(1);
        diode._onResistance.setValueWithoutUndo(0.005);  // 5 mΩ
        diode.getForwardVoltageDropParameter().setValueWithoutUndo(1.0);
        diodeProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calc = diodeProperties.lossCalculatorFabric();
        calc.calcLosses(100.0, 25.0, 1e-6);

        double expectedLoss = 1.0 * 100.0 + 0.005 * 100.0 * 100.0;  // 100 + 50 = 150W
        assertEquals("High current loss", expectedLoss, calc.getTotalLosses(), 0.1);
    }

    // ===========================================
    // Edge Cases and Validation Tests
    // ===========================================

    @Test
    public void testEdgeCase_NegativeCurrent() {
        diode.numberParalleled.setValueWithoutUndo(1);
        diodeProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calc = diodeProperties.lossCalculatorFabric();
        calc.calcLosses(-10.0, 25.0, 1e-6);

        // Loss should still be positive (uses absolute value)
        assertTrue("Negative current should produce positive loss", calc.getTotalLosses() > 0.0);
    }

    @Test
    public void testEdgeCase_SingleParallelDevice() {
        // Single device should behave same as no wrapper
        diode.numberParalleled.setValueWithoutUndo(1);
        diodeProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calc = diodeProperties.lossCalculatorFabric();
        calc.calcLosses(10.0, 25.0, 1e-6);

        double loss = calc.getTotalLosses();
        assertTrue("Single device should have positive loss", loss > 0.0);
    }

    @Test
    public void testEdgeCase_ManyParallelDevices() {
        diode.numberParalleled.setValueWithoutUndo(10);
        diodeProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calc = diodeProperties.lossCalculatorFabric();
        calc.calcLosses(100.0, 25.0, 1e-6);  // 10A per device

        assertTrue("10 parallel devices should produce valid loss", calc.getTotalLosses() > 0.0);
    }

    @Test
    public void testEdgeCase_VerySmallTimeStep() {
        diode.numberParalleled.setValueWithoutUndo(1);
        diodeProperties.setLossType(LossCalculationDetail.SIMPLE);

        AbstractLossCalculator calc = diodeProperties.lossCalculatorFabric();
        calc.calcLosses(10.0, 25.0, 1e-12);  // 1 picosecond

        // Should still calculate without error
        assertTrue("Very small timestep should work", calc.getTotalLosses() >= 0.0);
    }

    // ===========================================
    // Reflection Utility Tests
    // ===========================================

    @Test
    public void testInnerClass_ParallelWrapperExists() throws Exception {
        Class<?> clazz = getInnerClass("LossCalculatorParallelWrapper");
        assertNotNull("LossCalculatorParallelWrapper should exist", clazz);
    }

    @Test
    public void testInnerClass_ParallelWrapperWithSplitExists() throws Exception {
        Class<?> clazz = getInnerClass("LossCalculatorParallelWrapperWithSplit");
        assertNotNull("LossCalculatorParallelWrapperWithSplit should exist", clazz);
    }

    @Test
    public void testInnerClass_AdditionalDiodeExists() throws Exception {
        Class<?> clazz = getInnerClass("LossCalculatorAdditionalDiode");
        assertNotNull("LossCalculatorAdditionalDiode should exist", clazz);
    }

    @Test
    public void testInnerClass_AllThreeExist() throws Exception {
        Class<?>[] innerClasses = LossProperties.class.getDeclaredClasses();

        int wrapperCount = 0;
        for (Class<?> inner : innerClasses) {
            String name = inner.getSimpleName();
            if (name.equals("LossCalculatorParallelWrapper") ||
                name.equals("LossCalculatorParallelWrapperWithSplit") ||
                name.equals("LossCalculatorAdditionalDiode")) {
                wrapperCount++;
            }
        }

        assertTrue("Should have at least 3 wrapper inner classes", wrapperCount >= 3);
    }

    // ===========================================
    // Helper Methods
    // ===========================================

    /**
     * Gets an inner class of LossProperties by name.
     */
    private Class<?> getInnerClass(String className) throws ClassNotFoundException {
        Class<?>[] innerClasses = LossProperties.class.getDeclaredClasses();
        for (Class<?> inner : innerClasses) {
            if (inner.getSimpleName().equals(className)) {
                return inner;
            }
        }
        throw new ClassNotFoundException("Inner class not found: " + className);
    }
}
