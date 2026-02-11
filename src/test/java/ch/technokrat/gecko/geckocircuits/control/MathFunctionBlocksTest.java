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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for math function control blocks (SQRT, SQR, POW, EXP, LN, ABS).
 * Sprint 9: Control Package Core
 */
public class MathFunctionBlocksTest {

    private static final double DELTA = 1e-10;
    
    /**
     * Helper method to initialize all inputs of a calculator with dummy arrays.
     */
    private void initializeInputs(AbstractControlCalculatable calc) {
        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc.checkInputWithoutConnectionAndFill(i);
        }
    }

    // ========== Square Root Tests ==========
    
    @Test
    public void testSqrtBlockCreation() {
        ReglerSQRT block = new ReglerSQRT();
        assertNotNull("SQRT block should be created", block);
    }
    
    @Test
    public void testSqrtOfFour() {
        ReglerSQRT block = new ReglerSQRT();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 4.0;
        calc.berechneYOUT(0.001);
        assertEquals("sqrt(4) = 2", 2.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testSqrtOfNine() {
        ReglerSQRT block = new ReglerSQRT();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 9.0;
        calc.berechneYOUT(0.001);
        assertEquals("sqrt(9) = 3", 3.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testSqrtOfOne() {
        ReglerSQRT block = new ReglerSQRT();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 1.0;
        calc.berechneYOUT(0.001);
        assertEquals("sqrt(1) = 1", 1.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testSqrtOfZero() {
        ReglerSQRT block = new ReglerSQRT();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 0.0;
        calc.berechneYOUT(0.001);
        assertEquals("sqrt(0) = 0", 0.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testSqrtOfDecimal() {
        ReglerSQRT block = new ReglerSQRT();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 2.0;
        calc.berechneYOUT(0.001);
        assertEquals("sqrt(2) = 1.414...", Math.sqrt(2), calc._outputSignal[0][0], DELTA);
    }
    
    // ========== Square Tests ==========
    
    @Test
    public void testSqrBlockCreation() {
        ReglerSQR block = new ReglerSQR();
        assertNotNull("SQR block should be created", block);
    }
    
    @Test
    public void testSqrOfTwo() {
        ReglerSQR block = new ReglerSQR();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 2.0;
        calc.berechneYOUT(0.001);
        assertEquals("2^2 = 4", 4.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testSqrOfThree() {
        ReglerSQR block = new ReglerSQR();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 3.0;
        calc.berechneYOUT(0.001);
        assertEquals("3^2 = 9", 9.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testSqrOfNegative() {
        ReglerSQR block = new ReglerSQR();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = -3.0;
        calc.berechneYOUT(0.001);
        assertEquals("(-3)^2 = 9", 9.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testSqrOfZero() {
        ReglerSQR block = new ReglerSQR();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 0.0;
        calc.berechneYOUT(0.001);
        assertEquals("0^2 = 0", 0.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testSqrOfDecimal() {
        ReglerSQR block = new ReglerSQR();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 1.5;
        calc.berechneYOUT(0.001);
        assertEquals("1.5^2 = 2.25", 2.25, calc._outputSignal[0][0], DELTA);
    }
    
    // ========== Power Tests ==========
    
    @Test
    public void testPowBlockCreation() {
        ReglerPOW block = new ReglerPOW();
        assertNotNull("POW block should be created", block);
    }
    
    @Test
    public void testPowTwoToThree() {
        ReglerPOW block = new ReglerPOW();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 2.0;  // base
        calc._inputSignal[1][0] = 3.0;  // exponent
        calc.berechneYOUT(0.001);
        assertEquals("2^3 = 8", 8.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testPowThreeToTwo() {
        ReglerPOW block = new ReglerPOW();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 3.0;
        calc._inputSignal[1][0] = 2.0;
        calc.berechneYOUT(0.001);
        assertEquals("3^2 = 9", 9.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testPowZeroExponent() {
        ReglerPOW block = new ReglerPOW();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 0.0;
        calc.berechneYOUT(0.001);
        assertEquals("5^0 = 1", 1.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testPowOneExponent() {
        ReglerPOW block = new ReglerPOW();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 7.0;
        calc._inputSignal[1][0] = 1.0;
        calc.berechneYOUT(0.001);
        assertEquals("7^1 = 7", 7.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testPowFractionalExponent() {
        ReglerPOW block = new ReglerPOW();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 4.0;
        calc._inputSignal[1][0] = 0.5;
        calc.berechneYOUT(0.001);
        assertEquals("4^0.5 = 2", 2.0, calc._outputSignal[0][0], DELTA);
    }
    
    // ========== Exponential Tests ==========
    
    @Test
    public void testExpBlockCreation() {
        ReglerExponential block = new ReglerExponential();
        assertNotNull("EXP block should be created", block);
    }
    
    @Test
    public void testExpOfZero() {
        ReglerExponential block = new ReglerExponential();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 0.0;
        calc.berechneYOUT(0.001);
        assertEquals("e^0 = 1", 1.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testExpOfOne() {
        ReglerExponential block = new ReglerExponential();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 1.0;
        calc.berechneYOUT(0.001);
        assertEquals("e^1 = e", Math.E, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testExpOfNegative() {
        ReglerExponential block = new ReglerExponential();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = -1.0;
        calc.berechneYOUT(0.001);
        assertEquals("e^-1 = 1/e", 1.0/Math.E, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testExpOfTwo() {
        ReglerExponential block = new ReglerExponential();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 2.0;
        calc.berechneYOUT(0.001);
        assertEquals("e^2", Math.exp(2), calc._outputSignal[0][0], DELTA);
    }
    
    // ========== Natural Logarithm Tests ==========
    
    @Test
    public void testLnBlockCreation() {
        ReglerLN block = new ReglerLN();
        assertNotNull("LN block should be created", block);
    }
    
    @Test
    public void testLnOfE() {
        ReglerLN block = new ReglerLN();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = Math.E;
        calc.berechneYOUT(0.001);
        assertEquals("ln(e) = 1", 1.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testLnOfOne() {
        ReglerLN block = new ReglerLN();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 1.0;
        calc.berechneYOUT(0.001);
        assertEquals("ln(1) = 0", 0.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testLnOfESquared() {
        ReglerLN block = new ReglerLN();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = Math.E * Math.E;
        calc.berechneYOUT(0.001);
        assertEquals("ln(e^2) = 2", 2.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testLnOfTen() {
        ReglerLN block = new ReglerLN();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 10.0;
        calc.berechneYOUT(0.001);
        assertEquals("ln(10)", Math.log(10), calc._outputSignal[0][0], DELTA);
    }
    
    // ========== Absolute Value Tests ==========
    
    @Test
    public void testAbsBlockCreation() {
        ReglerAbsolutValue block = new ReglerAbsolutValue();
        assertNotNull("ABS block should be created", block);
    }
    
    @Test
    public void testAbsOfPositive() {
        ReglerAbsolutValue block = new ReglerAbsolutValue();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc.berechneYOUT(0.001);
        assertEquals("|5| = 5", 5.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testAbsOfNegative() {
        ReglerAbsolutValue block = new ReglerAbsolutValue();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = -5.0;
        calc.berechneYOUT(0.001);
        assertEquals("|-5| = 5", 5.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testAbsOfZero() {
        ReglerAbsolutValue block = new ReglerAbsolutValue();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 0.0;
        calc.berechneYOUT(0.001);
        assertEquals("|0| = 0", 0.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testAbsOfLargeNegative() {
        ReglerAbsolutValue block = new ReglerAbsolutValue();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = -1e10;
        calc.berechneYOUT(0.001);
        assertEquals("|-1e10| = 1e10", 1e10, calc._outputSignal[0][0], 1e0);
    }
    
    // ========== Signum Tests ==========
    
    @Test
    public void testSignumBlockCreation() {
        ReglerSignum block = new ReglerSignum();
        assertNotNull("SIGN block should be created", block);
    }
    
    @Test
    public void testSignumOfPositive() {
        ReglerSignum block = new ReglerSignum();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc.berechneYOUT(0.001);
        assertEquals("sign(5) = 1", 1.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testSignumOfNegative() {
        ReglerSignum block = new ReglerSignum();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = -5.0;
        calc.berechneYOUT(0.001);
        assertEquals("sign(-5) = -1", -1.0, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testSignumOfZero() {
        ReglerSignum block = new ReglerSignum();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 0.0;
        calc.berechneYOUT(0.001);
        assertEquals("sign(0) = 0", 0.0, calc._outputSignal[0][0], DELTA);
    }
}
