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
import ch.technokrat.gecko.geckocircuits.control.calculators.ConstantCalculator;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for basic control block functionality.
 * Sprint 9: Control Package Core
 */
public class RegelBlockBasicsTest {

    /**
     * Helper method to initialize all inputs of a calculator with dummy arrays.
     */
    private void initializeInputs(AbstractControlCalculatable calc) {
        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc.checkInputWithoutConnectionAndFill(i);
        }
    }

    // ========== ReglerConstant Tests ==========
    
    @Test
    public void testConstantBlockCreation() {
        ReglerConstant block = new ReglerConstant();
        assertNotNull("Block should be created", block);
    }
    
    @Test
    public void testConstantBlockDefaultValue() {
        ReglerConstant block = new ReglerConstant();
        assertEquals("Default constant value should be 1.0", 1.0, block._constValue.getValue(), 1e-10);
    }
    
    @Test
    public void testConstantBlockSetValue() {
        ReglerConstant block = new ReglerConstant();
        block._constValue.setValueWithoutUndo(5.0);
        assertEquals("Constant value should be set", 5.0, block._constValue.getValue(), 1e-10);
    }
    
    @Test
    public void testConstantBlockCalculator() {
        ReglerConstant block = new ReglerConstant();
        block._constValue.setValueWithoutUndo(3.14);
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        assertNotNull("Calculator should be created", calc);
        assertTrue("Calculator should be ConstantCalculator", calc instanceof ConstantCalculator);
    }
    
    @Test
    public void testConstantCalculatorOutput() {
        ConstantCalculator calc = new ConstantCalculator(42.0);
        assertEquals("Output should equal constant", 42.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testConstantCalculatorSetConst() {
        ConstantCalculator calc = new ConstantCalculator(1.0);
        calc.setConst(99.9);
        assertEquals("Output should update when constant changes", 99.9, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testConstantCalculatorNegativeValue() {
        ConstantCalculator calc = new ConstantCalculator(-123.456);
        assertEquals("Should handle negative values", -123.456, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testConstantCalculatorZero() {
        ConstantCalculator calc = new ConstantCalculator(0.0);
        assertEquals("Should handle zero", 0.0, calc._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testConstantBlockOutputNames() {
        ReglerConstant block = new ReglerConstant();
        String[] outputs = block.getOutputNames();
        assertNotNull("Output names should not be null", outputs);
        assertEquals("Should have one output", 1, outputs.length);
        assertEquals("Output name should be 'c'", "c", outputs[0]);
    }
    
    @Test
    public void testConstantBlockOutputDescription() {
        ReglerConstant block = new ReglerConstant();
        assertNotNull("Output description should not be null", block.getOutputDescription());
        assertEquals("Should have one description", 1, block.getOutputDescription().length);
    }
    
    @Test
    public void testConstantBlockTypeInfo() {
        assertNotNull("Type info should exist", ReglerConstant.tinfo);
        assertEquals("ID string should be CONST", "CONST", ReglerConstant.tinfo._fixedIDString);
    }
    
    // ========== Large Value Tests ==========
    
    @Test
    public void testConstantCalculatorLargePositive() {
        ConstantCalculator calc = new ConstantCalculator(1e12);
        assertEquals("Should handle large positive values", 1e12, calc._outputSignal[0][0], 1e2);
    }
    
    @Test
    public void testConstantCalculatorLargeNegative() {
        ConstantCalculator calc = new ConstantCalculator(-1e12);
        assertEquals("Should handle large negative values", -1e12, calc._outputSignal[0][0], 1e2);
    }
    
    @Test
    public void testConstantCalculatorSmallPositive() {
        ConstantCalculator calc = new ConstantCalculator(1e-12);
        assertEquals("Should handle small positive values", 1e-12, calc._outputSignal[0][0], 1e-22);
    }
}
