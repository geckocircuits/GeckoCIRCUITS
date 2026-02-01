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
 * Tests for logic control blocks (AND, OR, NOT, XOR, comparators).
 * Sprint 9: Control Package Core
 */
public class LogicBlocksTest {

    private static final double DELTA = 1e-10;
    private static final double HIGH = 1.0;
    private static final double LOW = 0.0;
    
    /**
     * Helper method to initialize all inputs of a calculator with dummy arrays.
     * This simulates what happens in NetzlisteCONTROL when inputs aren't connected.
     */
    private void initializeInputs(AbstractControlCalculatable calc) {
        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc.checkInputWithoutConnectionAndFill(i);
        }
    }

    // ========== AND Gate Tests ==========
    
    @Test
    public void testAndBlockCreation() {
        ReglerAnd block = new ReglerAnd();
        assertNotNull("AND block should be created", block);
    }
    
    @Test
    public void testAnd_TrueTrue() {
        ReglerAnd block = new ReglerAnd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = HIGH;
        calc._inputSignal[1][0] = HIGH;
        calc.berechneYOUT(0.001);
        assertEquals("1 AND 1 = 1", HIGH, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testAnd_TrueFalse() {
        ReglerAnd block = new ReglerAnd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = HIGH;
        calc._inputSignal[1][0] = LOW;
        calc.berechneYOUT(0.001);
        assertEquals("1 AND 0 = 0", LOW, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testAnd_FalseTrue() {
        ReglerAnd block = new ReglerAnd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = LOW;
        calc._inputSignal[1][0] = HIGH;
        calc.berechneYOUT(0.001);
        assertEquals("0 AND 1 = 0", LOW, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testAnd_FalseFalse() {
        ReglerAnd block = new ReglerAnd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = LOW;
        calc._inputSignal[1][0] = LOW;
        calc.berechneYOUT(0.001);
        assertEquals("0 AND 0 = 0", LOW, calc._outputSignal[0][0], DELTA);
    }

    // ========== OR Gate Tests ==========
    
    @Test
    public void testOrBlockCreation() {
        ReglerOr block = new ReglerOr();
        assertNotNull("OR block should be created", block);
    }
    
    @Test
    public void testOr_TrueTrue() {
        ReglerOr block = new ReglerOr();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = HIGH;
        calc._inputSignal[1][0] = HIGH;
        calc.berechneYOUT(0.001);
        assertEquals("1 OR 1 = 1", HIGH, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testOr_TrueFalse() {
        ReglerOr block = new ReglerOr();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = HIGH;
        calc._inputSignal[1][0] = LOW;
        calc.berechneYOUT(0.001);
        assertEquals("1 OR 0 = 1", HIGH, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testOr_FalseTrue() {
        ReglerOr block = new ReglerOr();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = LOW;
        calc._inputSignal[1][0] = HIGH;
        calc.berechneYOUT(0.001);
        assertEquals("0 OR 1 = 1", HIGH, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testOr_FalseFalse() {
        ReglerOr block = new ReglerOr();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = LOW;
        calc._inputSignal[1][0] = LOW;
        calc.berechneYOUT(0.001);
        assertEquals("0 OR 0 = 0", LOW, calc._outputSignal[0][0], DELTA);
    }

    // ========== NOT Gate Tests ==========
    
    @Test
    public void testNotBlockCreation() {
        ReglerNOT block = new ReglerNOT();
        assertNotNull("NOT block should be created", block);
    }
    
    @Test
    public void testNot_True() {
        ReglerNOT block = new ReglerNOT();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = HIGH;
        calc.berechneYOUT(0.001);
        assertEquals("NOT 1 = 0", LOW, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testNot_False() {
        ReglerNOT block = new ReglerNOT();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = LOW;
        calc.berechneYOUT(0.001);
        assertEquals("NOT 0 = 1", HIGH, calc._outputSignal[0][0], DELTA);
    }

    // ========== XOR Gate Tests ==========
    
    @Test
    public void testXorBlockCreation() {
        ReglerExclusiveOr block = new ReglerExclusiveOr();
        assertNotNull("XOR block should be created", block);
    }
    
    @Test
    public void testXor_TrueTrue() {
        ReglerExclusiveOr block = new ReglerExclusiveOr();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = HIGH;
        calc._inputSignal[1][0] = HIGH;
        calc.berechneYOUT(0.001);
        assertEquals("1 XOR 1 = 0", LOW, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testXor_TrueFalse() {
        ReglerExclusiveOr block = new ReglerExclusiveOr();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = HIGH;
        calc._inputSignal[1][0] = LOW;
        calc.berechneYOUT(0.001);
        assertEquals("1 XOR 0 = 1", HIGH, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testXor_FalseTrue() {
        ReglerExclusiveOr block = new ReglerExclusiveOr();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = LOW;
        calc._inputSignal[1][0] = HIGH;
        calc.berechneYOUT(0.001);
        assertEquals("0 XOR 1 = 1", HIGH, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testXor_FalseFalse() {
        ReglerExclusiveOr block = new ReglerExclusiveOr();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = LOW;
        calc._inputSignal[1][0] = LOW;
        calc.berechneYOUT(0.001);
        assertEquals("0 XOR 0 = 0", LOW, calc._outputSignal[0][0], DELTA);
    }

    // ========== Greater Than Tests ==========
    
    @Test
    public void testGreaterThanBlockCreation() {
        ReglerGreaterThan block = new ReglerGreaterThan();
        assertNotNull("GT block should be created", block);
    }
    
    @Test
    public void testGreaterThan_True() {
        ReglerGreaterThan block = new ReglerGreaterThan();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 3.0;
        calc.berechneYOUT(0.001);
        assertEquals("5 > 3 = true", HIGH, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testGreaterThan_False() {
        ReglerGreaterThan block = new ReglerGreaterThan();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 3.0;
        calc._inputSignal[1][0] = 5.0;
        calc.berechneYOUT(0.001);
        assertEquals("3 > 5 = false", LOW, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testGreaterThan_Equal() {
        ReglerGreaterThan block = new ReglerGreaterThan();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 5.0;
        calc.berechneYOUT(0.001);
        assertEquals("5 > 5 = false", LOW, calc._outputSignal[0][0], DELTA);
    }

    // ========== Greater or Equal Tests ==========
    
    @Test
    public void testGreaterEqualBlockCreation() {
        ReglerGreaterEqual block = new ReglerGreaterEqual();
        assertNotNull("GE block should be created", block);
    }
    
    @Test
    public void testGreaterEqual_Greater() {
        ReglerGreaterEqual block = new ReglerGreaterEqual();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 3.0;
        calc.berechneYOUT(0.001);
        assertEquals("5 >= 3 = true", HIGH, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testGreaterEqual_Equal() {
        ReglerGreaterEqual block = new ReglerGreaterEqual();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 5.0;
        calc.berechneYOUT(0.001);
        assertEquals("5 >= 5 = true", HIGH, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testGreaterEqual_Less() {
        ReglerGreaterEqual block = new ReglerGreaterEqual();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 3.0;
        calc._inputSignal[1][0] = 5.0;
        calc.berechneYOUT(0.001);
        assertEquals("3 >= 5 = false", LOW, calc._outputSignal[0][0], DELTA);
    }

    // ========== Equal Tests ==========
    
    @Test
    public void testEqualBlockCreation() {
        ReglerEqual block = new ReglerEqual();
        assertNotNull("EQ block should be created", block);
    }
    
    @Test
    public void testEqual_True() {
        ReglerEqual block = new ReglerEqual();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 5.0;
        calc.berechneYOUT(0.001);
        assertEquals("5 == 5 = true", HIGH, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testEqual_False() {
        ReglerEqual block = new ReglerEqual();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 3.0;
        calc.berechneYOUT(0.001);
        assertEquals("5 == 3 = false", LOW, calc._outputSignal[0][0], DELTA);
    }

    // ========== Not Equal Tests ==========
    
    @Test
    public void testNotEqualBlockCreation() {
        ReglerNE block = new ReglerNE();
        assertNotNull("NE block should be created", block);
    }
    
    @Test
    public void testNotEqual_True() {
        ReglerNE block = new ReglerNE();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 3.0;
        calc.berechneYOUT(0.001);
        assertEquals("5 != 3 = true", HIGH, calc._outputSignal[0][0], DELTA);
    }
    
    @Test
    public void testNotEqual_False() {
        ReglerNE block = new ReglerNE();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 5.0;
        calc.berechneYOUT(0.001);
        assertEquals("5 != 5 = false", LOW, calc._outputSignal[0][0], DELTA);
    }
}
