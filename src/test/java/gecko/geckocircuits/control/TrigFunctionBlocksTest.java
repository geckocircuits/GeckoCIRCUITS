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
package gecko.geckocircuits.control;

import gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for trigonometric function control blocks (SIN, COS, TAN, ASIN, ACOS, ATAN).
 * Sprint 9: Control Package Core
 */
public class TrigFunctionBlocksTest {

    private static final double DELTA = 1e-10;
    private static final double PI = Math.PI;

    /**
     * Helper method to initialize all inputs of a calculator with dummy arrays.
     */
    private void initializeInputs(AbstractControlCalculatable calc) {
        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc.checkInputWithoutConnectionAndFill(i);
        }
    }

    // ========== Sine Tests ==========

    @Test
    public void testSinBlockCreation() {
        ControlSIN block = new ControlSIN();
        assertNotNull("SIN block should be created", block);
    }

    @Test
    public void testSinOfZero() {
        ControlSIN block = new ControlSIN();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 0.0;
        calc.calculateYOUT(0.001);
        assertEquals("sin(0) = 0", 0.0, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testSinOfPiOverTwo() {
        ControlSIN block = new ControlSIN();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = PI / 2;
        calc.calculateYOUT(0.001);
        assertEquals("sin(π/2) = 1", 1.0, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testSinOfPi() {
        ControlSIN block = new ControlSIN();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = PI;
        calc.calculateYOUT(0.001);
        assertEquals("sin(π) = 0", 0.0, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testSinOfNegativePiOverTwo() {
        ControlSIN block = new ControlSIN();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = -PI / 2;
        calc.calculateYOUT(0.001);
        assertEquals("sin(-π/2) = -1", -1.0, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testSinOfPiOverSix() {
        ControlSIN block = new ControlSIN();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = PI / 6;
        calc.calculateYOUT(0.001);
        assertEquals("sin(π/6) = 0.5", 0.5, calc._outputSignal[0][0], DELTA);
    }

    // ========== Cosine Tests ==========

    @Test
    public void testCosBlockCreation() {
        ControlCosine block = new ControlCosine();
        assertNotNull("COS block should be created", block);
    }

    @Test
    public void testCosOfZero() {
        ControlCosine block = new ControlCosine();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 0.0;
        calc.calculateYOUT(0.001);
        assertEquals("cos(0) = 1", 1.0, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testCosOfPiOverTwo() {
        ControlCosine block = new ControlCosine();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = PI / 2;
        calc.calculateYOUT(0.001);
        assertEquals("cos(π/2) = 0", 0.0, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testCosOfPi() {
        ControlCosine block = new ControlCosine();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = PI;
        calc.calculateYOUT(0.001);
        assertEquals("cos(π) = -1", -1.0, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testCosOfPiOverThree() {
        ControlCosine block = new ControlCosine();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = PI / 3;
        calc.calculateYOUT(0.001);
        assertEquals("cos(π/3) = 0.5", 0.5, calc._outputSignal[0][0], DELTA);
    }

    // ========== Tangent Tests ==========

    @Test
    public void testTanBlockCreation() {
        ControlTAN block = new ControlTAN();
        assertNotNull("TAN block should be created", block);
    }

    @Test
    public void testTanOfZero() {
        ControlTAN block = new ControlTAN();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 0.0;
        calc.calculateYOUT(0.001);
        assertEquals("tan(0) = 0", 0.0, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testTanOfPiOverFour() {
        ControlTAN block = new ControlTAN();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = PI / 4;
        calc.calculateYOUT(0.001);
        assertEquals("tan(π/4) = 1", 1.0, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testTanOfNegativePiOverFour() {
        ControlTAN block = new ControlTAN();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = -PI / 4;
        calc.calculateYOUT(0.001);
        assertEquals("tan(-π/4) = -1", -1.0, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testTanOfPi() {
        ControlTAN block = new ControlTAN();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = PI;
        calc.calculateYOUT(0.001);
        assertEquals("tan(π) = 0", 0.0, calc._outputSignal[0][0], DELTA);
    }

    // ========== Arc Sine Tests ==========

    @Test
    public void testAsinBlockCreation() {
        ControlAreaSine block = new ControlAreaSine();
        assertNotNull("ASIN block should be created", block);
    }

    @Test
    public void testAsinOfZero() {
        ControlAreaSine block = new ControlAreaSine();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 0.0;
        calc.calculateYOUT(0.001);
        assertEquals("asin(0) = 0", 0.0, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testAsinOfOne() {
        ControlAreaSine block = new ControlAreaSine();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 1.0;
        calc.calculateYOUT(0.001);
        assertEquals("asin(1) = π/2", PI / 2, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testAsinOfNegativeOne() {
        ControlAreaSine block = new ControlAreaSine();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = -1.0;
        calc.calculateYOUT(0.001);
        assertEquals("asin(-1) = -π/2", -PI / 2, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testAsinOfHalf() {
        ControlAreaSine block = new ControlAreaSine();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 0.5;
        calc.calculateYOUT(0.001);
        assertEquals("asin(0.5) = π/6", PI / 6, calc._outputSignal[0][0], DELTA);
    }

    // ========== Arc Cosine Tests ==========

    @Test
    public void testAcosBlockCreation() {
        ControlAreaCosine block = new ControlAreaCosine();
        assertNotNull("ACOS block should be created", block);
    }

    @Test
    public void testAcosOfOne() {
        ControlAreaCosine block = new ControlAreaCosine();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 1.0;
        calc.calculateYOUT(0.001);
        assertEquals("acos(1) = 0", 0.0, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testAcosOfZero() {
        ControlAreaCosine block = new ControlAreaCosine();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 0.0;
        calc.calculateYOUT(0.001);
        assertEquals("acos(0) = π/2", PI / 2, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testAcosOfNegativeOne() {
        ControlAreaCosine block = new ControlAreaCosine();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = -1.0;
        calc.calculateYOUT(0.001);
        assertEquals("acos(-1) = π", PI, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testAcosOfHalf() {
        ControlAreaCosine block = new ControlAreaCosine();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 0.5;
        calc.calculateYOUT(0.001);
        assertEquals("acos(0.5) = π/3", PI / 3, calc._outputSignal[0][0], DELTA);
    }

    // ========== Arc Tangent Tests ==========

    @Test
    public void testAtanBlockCreation() {
        ControlAreaTangens block = new ControlAreaTangens();
        assertNotNull("ATAN block should be created", block);
    }

    @Test
    public void testAtanOfZero() {
        ControlAreaTangens block = new ControlAreaTangens();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 0.0;
        calc.calculateYOUT(0.001);
        assertEquals("atan(0) = 0", 0.0, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testAtanOfOne() {
        ControlAreaTangens block = new ControlAreaTangens();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 1.0;
        calc.calculateYOUT(0.001);
        assertEquals("atan(1) = π/4", PI / 4, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testAtanOfNegativeOne() {
        ControlAreaTangens block = new ControlAreaTangens();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = -1.0;
        calc.calculateYOUT(0.001);
        assertEquals("atan(-1) = -π/4", -PI / 4, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testAtanOfLargeValue() {
        ControlAreaTangens block = new ControlAreaTangens();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 1000.0;
        calc.calculateYOUT(0.001);
        // atan approaches π/2 as input → ∞
        assertTrue("atan(1000) should be close to π/2", calc._outputSignal[0][0] > PI / 2 - 0.01);
        assertTrue("atan(1000) should be less than π/2", calc._outputSignal[0][0] < PI / 2);
    }

    // ========== Pythagorean Identity Tests ==========

    @Test
    public void testPythagoreanIdentity() {
        // sin²(x) + cos²(x) = 1
        ControlSIN sinBlock = new ControlSIN();
        ControlCosine cosBlock = new ControlCosine();
        AbstractControlCalculatable sinCalc = sinBlock.getInternalControlCalculatableForSimulationStart();
        initializeInputs(sinCalc);
        AbstractControlCalculatable cosCalc = cosBlock.getInternalControlCalculatableForSimulationStart();
        initializeInputs(cosCalc);

        double[] testAngles = {0, PI/6, PI/4, PI/3, PI/2, PI, 3*PI/2, 2*PI};

        for (double angle : testAngles) {
            sinCalc._inputSignal[0][0] = angle;
            cosCalc._inputSignal[0][0] = angle;
            sinCalc.calculateYOUT(0.001);
            cosCalc.calculateYOUT(0.001);

            double sin2 = sinCalc._outputSignal[0][0] * sinCalc._outputSignal[0][0];
            double cos2 = cosCalc._outputSignal[0][0] * cosCalc._outputSignal[0][0];

            assertEquals("sin²(" + angle + ") + cos²(" + angle + ") = 1", 1.0, sin2 + cos2, DELTA);
        }
    }

    // ========== Inverse Function Tests ==========

    @Test
    public void testSinAsinInverse() {
        ControlSIN sinBlock = new ControlSIN();
        ControlAreaSine asinBlock = new ControlAreaSine();
        AbstractControlCalculatable sinCalc = sinBlock.getInternalControlCalculatableForSimulationStart();
        initializeInputs(sinCalc);
        AbstractControlCalculatable asinCalc = asinBlock.getInternalControlCalculatableForSimulationStart();
        initializeInputs(asinCalc);

        double[] testValues = {-0.9, -0.5, 0, 0.5, 0.9};

        for (double val : testValues) {
            asinCalc._inputSignal[0][0] = val;
            asinCalc.calculateYOUT(0.001);

            sinCalc._inputSignal[0][0] = asinCalc._outputSignal[0][0];
            sinCalc.calculateYOUT(0.001);

            assertEquals("sin(asin(" + val + ")) = " + val, val, sinCalc._outputSignal[0][0], DELTA);
        }
    }
}
