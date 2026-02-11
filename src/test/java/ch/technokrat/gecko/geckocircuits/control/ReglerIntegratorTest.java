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

import ch.technokrat.gecko.geckocircuits.control.calculators.IntegratorCalculation;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for integrator control block.
 * Sprint 9: Control Package Core
 */
public class ReglerIntegratorTest {

    /**
     * Helper method to initialize all inputs of a calculator with dummy arrays.
     */
    private void initializeInputs(AbstractControlCalculatable calc) {
        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc.checkInputWithoutConnectionAndFill(i);
        }
    }

    // ========== Block Creation Tests ==========
    
    @Test
    public void testIntegratorBlockCreation() {
        ReglerIntegrator block = new ReglerIntegrator();
        assertNotNull("Block should be created", block);
    }
    
    @Test
    public void testIntegratorBlockDefaultValues() {
        ReglerIntegrator block = new ReglerIntegrator();
        assertEquals("Default a1 value should be 1.0", 1.0, block._a1Val.getValue(), 1e-10);
        assertEquals("Default y0 value should be 0.0", 0.0, block._y0Val.getValue(), 1e-10);
        assertEquals("Default min value should be -1.0", -1.0, block._minLimit.getValue(), 1e-10);
        assertEquals("Default max value should be 1.0", 1.0, block._maxLimit.getValue(), 1e-10);
    }
    
    @Test
    public void testIntegratorBlockTypeInfo() {
        assertNotNull("Type info should exist", ReglerIntegrator.tinfo);
        assertEquals("ID string should be INT", "INT", ReglerIntegrator.tinfo._fixedIDString);
    }
    
    @Test
    public void testIntegratorBlockOutputNames() {
        ReglerIntegrator block = new ReglerIntegrator();
        String[] outputs = block.getOutputNames();
        assertNotNull("Output names should not be null", outputs);
        assertEquals("Should have one output", 1, outputs.length);
        assertEquals("Output name should be 'integral'", "integral", outputs[0]);
    }
    
    // ========== Calculator Creation Tests ==========
    
    @Test
    public void testIntegratorCalculatorCreation() {
        IntegratorCalculation calc = new IntegratorCalculation(1.0, 0.0, -10.0, 10.0);
        assertNotNull("Calculator should be created", calc);
    }
    
    @Test
    public void testIntegratorCalculatorInitialValue() {
        IntegratorCalculation calc = new IntegratorCalculation(1.0, 5.0, -10.0, 10.0);
        calc.initializeAtSimulationStart(0.001);
        assertEquals("Initial value should be set", 5.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testIntegratorCalculatorZeroInitial() {
        IntegratorCalculation calc = new IntegratorCalculation(1.0, 0.0, -10.0, 10.0);
        calc.initializeAtSimulationStart(0.001);
        assertEquals("Zero initial value", 0.0, calc._outputSignal[0][0], 1e-10);
    }
    
    // ========== Integration Behavior Tests ==========
    
    @Test
    public void testIntegratorBasicIntegration() {
        IntegratorCalculation calc = new IntegratorCalculation(1.0, 0.0, -100.0, 100.0);
        initializeInputs(calc);
        calc.initializeAtSimulationStart(0.001);
        
        double dt = 0.001;
        calc._inputSignal[0][0] = 1.0;  // Constant input
        calc._inputSignal[1][0] = 0.0;  // No reset
        
        // Integrate for several steps
        for (int i = 0; i < 100; i++) {
            calc.berechneYOUT(dt);
        }
        
        // Expected: integral of 1.0 over 0.1s = approximately 0.1
        // Using trapezoidal rule with some deviation expected
        assertTrue("Integral should increase", calc._outputSignal[0][0] > 0.05);
        assertTrue("Integral should be reasonable", calc._outputSignal[0][0] < 0.15);
    }
    
    @Test
    public void testIntegratorWithCoefficientA1() {
        IntegratorCalculation calc = new IntegratorCalculation(2.0, 0.0, -100.0, 100.0);
        initializeInputs(calc);
        calc.initializeAtSimulationStart(0.001);
        
        double dt = 0.001;
        calc._inputSignal[0][0] = 1.0;
        calc._inputSignal[1][0] = 0.0;
        
        for (int i = 0; i < 100; i++) {
            calc.berechneYOUT(dt);
        }
        
        // With a1=2.0, integral should be twice as large
        assertTrue("Integral with a1=2 should be larger", calc._outputSignal[0][0] > 0.1);
    }
    
    @Test
    public void testIntegratorNegativeInput() {
        IntegratorCalculation calc = new IntegratorCalculation(1.0, 0.0, -100.0, 100.0);
        initializeInputs(calc);
        calc.initializeAtSimulationStart(0.001);
        
        double dt = 0.001;
        calc._inputSignal[0][0] = -1.0;  // Negative input
        calc._inputSignal[1][0] = 0.0;
        
        for (int i = 0; i < 100; i++) {
            calc.berechneYOUT(dt);
        }
        
        assertTrue("Integral should be negative", calc._outputSignal[0][0] < 0);
    }
    
    // ========== Saturation Tests ==========
    
    @Test
    public void testIntegratorUpperSaturation() {
        IntegratorCalculation calc = new IntegratorCalculation(1.0, 0.0, -1.0, 1.0);
        initializeInputs(calc);
        calc.initializeAtSimulationStart(0.001);
        
        double dt = 0.001;
        calc._inputSignal[0][0] = 100.0;  // Large input to hit saturation quickly
        calc._inputSignal[1][0] = 0.0;
        
        for (int i = 0; i < 1000; i++) {
            calc.berechneYOUT(dt);
        }
        
        assertEquals("Should saturate at max", 1.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testIntegratorLowerSaturation() {
        IntegratorCalculation calc = new IntegratorCalculation(1.0, 0.0, -1.0, 1.0);
        initializeInputs(calc);
        calc.initializeAtSimulationStart(0.001);
        
        double dt = 0.001;
        calc._inputSignal[0][0] = -100.0;  // Large negative input
        calc._inputSignal[1][0] = 0.0;
        
        for (int i = 0; i < 1000; i++) {
            calc.berechneYOUT(dt);
        }
        
        assertEquals("Should saturate at min", -1.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testIntegratorSetMinMax() {
        IntegratorCalculation calc = new IntegratorCalculation(1.0, 0.0, -10.0, 10.0);
        initializeInputs(calc);
        calc.setMinMax(-5.0, 5.0);
        calc.initializeAtSimulationStart(0.001);
        
        double dt = 0.001;
        calc._inputSignal[0][0] = 1000.0;
        calc._inputSignal[1][0] = 0.0;
        
        for (int i = 0; i < 1000; i++) {
            calc.berechneYOUT(dt);
        }
        
        assertEquals("Should saturate at new max", 5.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testIntegratorInvalidMinMax() {
        IntegratorCalculation calc = new IntegratorCalculation(1.0, 0.0, -10.0, 10.0);
        calc.setMinMax(10.0, -10.0);  // min > max should throw
    }
    
    // ========== Reset Tests ==========
    
    @Test
    public void testIntegratorReset() {
        IntegratorCalculation calc = new IntegratorCalculation(1.0, 0.0, -100.0, 100.0);
        initializeInputs(calc);
        calc.initializeAtSimulationStart(0.001);
        
        double dt = 0.001;
        
        // First, integrate up
        calc._inputSignal[0][0] = 100.0;
        calc._inputSignal[1][0] = 0.0;
        for (int i = 0; i < 100; i++) {
            calc.berechneYOUT(dt);
        }
        assertTrue("Should have integrated", calc._outputSignal[0][0] > 1.0);
        
        // Now reset (input 1 >= 1)
        calc._inputSignal[1][0] = 1.0;
        calc.berechneYOUT(dt);
        assertEquals("Reset should return to initial value", 0.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testIntegratorResetToNonZeroInitial() {
        IntegratorCalculation calc = new IntegratorCalculation(1.0, 5.0, -100.0, 100.0);
        initializeInputs(calc);
        calc.initializeAtSimulationStart(0.001);
        
        double dt = 0.001;
        
        // Integrate
        calc._inputSignal[0][0] = 100.0;
        calc._inputSignal[1][0] = 0.0;
        for (int i = 0; i < 100; i++) {
            calc.berechneYOUT(dt);
        }
        
        // Reset
        calc._inputSignal[1][0] = 1.0;
        calc.berechneYOUT(dt);
        assertEquals("Reset should return to initial value 5.0", 5.0, calc._outputSignal[0][0], 1e-10);
    }
    
    // ========== Coefficient Update Tests ==========
    
    @Test
    public void testIntegratorSetA1() {
        IntegratorCalculation calc = new IntegratorCalculation(1.0, 0.0, -100.0, 100.0);
        initializeInputs(calc);
        calc.setA1Val(3.0);
        calc.initializeAtSimulationStart(0.001);
        
        double dt = 0.001;
        calc._inputSignal[0][0] = 1.0;
        calc._inputSignal[1][0] = 0.0;
        
        for (int i = 0; i < 100; i++) {
            calc.berechneYOUT(dt);
        }
        
        // With a1=3.0, integral should be 3x
        assertTrue("Integral with a1=3 should be larger", calc._outputSignal[0][0] > 0.15);
    }
    
    // ========== Zero Input Tests ==========
    
    @Test
    public void testIntegratorZeroInput() {
        IntegratorCalculation calc = new IntegratorCalculation(1.0, 5.0, -100.0, 100.0);
        initializeInputs(calc);
        calc.initializeAtSimulationStart(0.001);
        
        double dt = 0.001;
        calc._inputSignal[0][0] = 0.0;  // Zero input
        calc._inputSignal[1][0] = 0.0;
        
        for (int i = 0; i < 100; i++) {
            calc.berechneYOUT(dt);
        }
        
        assertEquals("Zero input should maintain initial value", 5.0, calc._outputSignal[0][0], 1e-6);
    }
}
