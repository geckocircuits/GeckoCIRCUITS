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
 * Tests for addition control block.
 * Sprint 9: Control Package Core
 */
public class ReglerAddTest {

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
    public void testAddBlockCreation() {
        ReglerAdd block = new ReglerAdd();
        assertNotNull("Block should be created", block);
    }
    
    @Test
    public void testAddBlockTypeInfo() {
        assertNotNull("Type info should exist", ReglerAdd.tinfo);
        assertEquals("ID string should be ADD", "ADD", ReglerAdd.tinfo._fixedIDString);
    }
    
    @Test
    public void testAddBlockOutputNames() {
        ReglerAdd block = new ReglerAdd();
        String[] outputs = block.getOutputNames();
        assertNotNull("Output names should not be null", outputs);
        assertEquals("Should have one output", 1, outputs.length);
        assertEquals("Output name should be 'sum'", "sum", outputs[0]);
    }
    
    // ========== Calculator Tests (2 inputs) ==========
    
    @Test
    public void testAddCalculatorTwoInputs() {
        ReglerAdd block = new ReglerAdd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        assertNotNull("Calculator should be created", calc);
    }
    
    @Test
    public void testAddCalculatorBasicSum() {
        ReglerAdd block = new ReglerAdd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 3.0;
        calc._inputSignal[1][0] = 5.0;
        calc.berechneYOUT(0.001);
        assertEquals("3 + 5 = 8", 8.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testAddCalculatorZeroSum() {
        ReglerAdd block = new ReglerAdd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 0.0;
        calc._inputSignal[1][0] = 0.0;
        calc.berechneYOUT(0.001);
        assertEquals("0 + 0 = 0", 0.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testAddCalculatorNegativeInputs() {
        ReglerAdd block = new ReglerAdd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = -3.0;
        calc._inputSignal[1][0] = -5.0;
        calc.berechneYOUT(0.001);
        assertEquals("-3 + -5 = -8", -8.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testAddCalculatorMixedSign() {
        ReglerAdd block = new ReglerAdd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 10.0;
        calc._inputSignal[1][0] = -3.0;
        calc.berechneYOUT(0.001);
        assertEquals("10 + -3 = 7", 7.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testAddCalculatorOppositesCancelOut() {
        ReglerAdd block = new ReglerAdd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = -5.0;
        calc.berechneYOUT(0.001);
        assertEquals("5 + -5 = 0", 0.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testAddCalculatorDecimalValues() {
        ReglerAdd block = new ReglerAdd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 1.5;
        calc._inputSignal[1][0] = 2.5;
        calc.berechneYOUT(0.001);
        assertEquals("1.5 + 2.5 = 4.0", 4.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testAddCalculatorLargeValues() {
        ReglerAdd block = new ReglerAdd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 1e10;
        calc._inputSignal[1][0] = 1e10;
        calc.berechneYOUT(0.001);
        assertEquals("1e10 + 1e10 = 2e10", 2e10, calc._outputSignal[0][0], 1e0);
    }
    
    @Test
    public void testAddCalculatorSmallValues() {
        ReglerAdd block = new ReglerAdd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 1e-10;
        calc._inputSignal[1][0] = 2e-10;
        calc.berechneYOUT(0.001);
        assertEquals("1e-10 + 2e-10 = 3e-10", 3e-10, calc._outputSignal[0][0], 1e-20);
    }
    
    // ========== Time Step Independence ==========
    
    @Test
    public void testAddCalculatorTimeStepIndependent() {
        ReglerAdd block = new ReglerAdd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 3.0;
        
        calc.berechneYOUT(0.001);
        double result1 = calc._outputSignal[0][0];
        
        calc.berechneYOUT(0.0001);
        double result2 = calc._outputSignal[0][0];
        
        assertEquals("Sum should be time step independent", result1, result2, 1e-10);
    }
    
    // ========== Multiple Computation Steps ==========
    
    @Test
    public void testAddCalculatorMultipleSteps() {
        ReglerAdd block = new ReglerAdd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        
        // Step 1
        calc._inputSignal[0][0] = 1.0;
        calc._inputSignal[1][0] = 2.0;
        calc.berechneYOUT(0.001);
        assertEquals("Step 1: 1 + 2 = 3", 3.0, calc._outputSignal[0][0], 1e-10);
        
        // Step 2 - change inputs
        calc._inputSignal[0][0] = 10.0;
        calc._inputSignal[1][0] = 20.0;
        calc.berechneYOUT(0.001);
        assertEquals("Step 2: 10 + 20 = 30", 30.0, calc._outputSignal[0][0], 1e-10);
        
        // Step 3 - back to original
        calc._inputSignal[0][0] = 1.0;
        calc._inputSignal[1][0] = 2.0;
        calc.berechneYOUT(0.001);
        assertEquals("Step 3: 1 + 2 = 3", 3.0, calc._outputSignal[0][0], 1e-10);
    }
}
