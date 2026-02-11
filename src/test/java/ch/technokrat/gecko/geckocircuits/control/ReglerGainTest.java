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
import ch.technokrat.gecko.geckocircuits.control.calculators.GainCalculator;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for gain control block.
 * Sprint 9: Control Package Core
 */
public class ReglerGainTest {

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
    public void testGainBlockCreation() {
        ReglerGain block = new ReglerGain();
        assertNotNull("Block should be created", block);
    }
    
    @Test
    public void testGainBlockDefaultValue() {
        ReglerGain block = new ReglerGain();
        assertEquals("Default gain value should be 1.0", 1.0, block._gain.getValue(), 1e-10);
    }
    
    @Test
    public void testGainBlockSetValue() {
        ReglerGain block = new ReglerGain();
        block._gain.setValueWithoutUndo(10.0);
        assertEquals("Gain value should be set", 10.0, block._gain.getValue(), 1e-10);
    }
    
    @Test
    public void testGainBlockTypeInfo() {
        assertNotNull("Type info should exist", ReglerGain.tinfo);
        assertEquals("ID string should be GAIN", "GAIN", ReglerGain.tinfo._fixedIDString);
    }
    
    // ========== Calculator Tests ==========
    
    @Test
    public void testGainCalculatorCreation() {
        GainCalculator calc = new GainCalculator(2.0);
        assertNotNull("Calculator should be created", calc);
    }
    
    @Test
    public void testGainCalculatorUnitGain() {
        GainCalculator calc = new GainCalculator(1.0);
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc.berechneYOUT(0.001);
        assertEquals("Unit gain should pass through", 5.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testGainCalculatorDoubleGain() {
        GainCalculator calc = new GainCalculator(2.0);
        initializeInputs(calc);
        calc._inputSignal[0][0] = 3.0;
        calc.berechneYOUT(0.001);
        assertEquals("Output should be 2x input", 6.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testGainCalculatorHalfGain() {
        GainCalculator calc = new GainCalculator(0.5);
        initializeInputs(calc);
        calc._inputSignal[0][0] = 10.0;
        calc.berechneYOUT(0.001);
        assertEquals("Output should be 0.5x input", 5.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testGainCalculatorNegativeGain() {
        GainCalculator calc = new GainCalculator(-1.0);
        initializeInputs(calc);
        calc._inputSignal[0][0] = 7.0;
        calc.berechneYOUT(0.001);
        assertEquals("Output should be inverted", -7.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testGainCalculatorZeroInput() {
        GainCalculator calc = new GainCalculator(100.0);
        initializeInputs(calc);
        calc._inputSignal[0][0] = 0.0;
        calc.berechneYOUT(0.001);
        assertEquals("Zero input should give zero output", 0.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testGainCalculatorZeroGain() {
        GainCalculator calc = new GainCalculator(0.0);
        initializeInputs(calc);
        calc._inputSignal[0][0] = 999.0;
        calc.berechneYOUT(0.001);
        assertEquals("Zero gain should give zero output", 0.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testGainCalculatorSetGain() {
        GainCalculator calc = new GainCalculator(1.0);
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc.setGain(3.0);
        calc.berechneYOUT(0.001);
        assertEquals("Should use new gain", 15.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testGainCalculatorNegativeInput() {
        GainCalculator calc = new GainCalculator(2.0);
        initializeInputs(calc);
        calc._inputSignal[0][0] = -4.0;
        calc.berechneYOUT(0.001);
        assertEquals("Should handle negative input", -8.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testGainCalculatorLargeGain() {
        GainCalculator calc = new GainCalculator(1e6);
        initializeInputs(calc);
        calc._inputSignal[0][0] = 1.0;
        calc.berechneYOUT(0.001);
        assertEquals("Should handle large gain", 1e6, calc._outputSignal[0][0], 1e-4);
    }
    
    @Test
    public void testGainCalculatorSmallGain() {
        GainCalculator calc = new GainCalculator(1e-6);
        initializeInputs(calc);
        calc._inputSignal[0][0] = 1e6;
        calc.berechneYOUT(0.001);
        assertEquals("Should handle small gain", 1.0, calc._outputSignal[0][0], 1e-10);
    }
    
    // ========== Block to Calculator Integration ==========
    
    @Test
    public void testGainBlockCalculator() {
        ReglerGain block = new ReglerGain();
        block._gain.setValueWithoutUndo(5.0);
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        assertNotNull("Calculator should be created", calc);
        assertTrue("Calculator should be GainCalculator", calc instanceof GainCalculator);
    }
    
    @Test
    public void testGainBlockCalculatorValue() {
        ReglerGain block = new ReglerGain();
        block._gain.setValueWithoutUndo(7.0);
        GainCalculator calc = (GainCalculator) block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 3.0;
        calc.berechneYOUT(0.001);
        assertEquals("Calculator should use block's gain value", 21.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testGainBlockOutputNames() {
        ReglerGain block = new ReglerGain();
        String[] outputs = block.getOutputNames();
        assertNotNull("Output names should not be null", outputs);
        assertEquals("Should have one output", 1, outputs.length);
        assertEquals("Output name should be 'p'", "p", outputs[0]);
    }
    
    // ========== Time Step Independence ==========
    
    @Test
    public void testGainCalculatorTimeStepIndependent() {
        GainCalculator calc = new GainCalculator(2.0);
        initializeInputs(calc);
        calc._inputSignal[0][0] = 10.0;
        
        calc.berechneYOUT(0.001);
        double result1 = calc._outputSignal[0][0];
        
        calc.berechneYOUT(0.0001);
        double result2 = calc._outputSignal[0][0];
        
        calc.berechneYOUT(0.01);
        double result3 = calc._outputSignal[0][0];
        
        assertEquals("Gain should be time step independent", result1, result2, 1e-10);
        assertEquals("Gain should be time step independent", result1, result3, 1e-10);
    }
}
