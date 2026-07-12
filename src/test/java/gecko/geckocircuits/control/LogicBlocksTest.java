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
        ControlAnd block = new ControlAnd();
        assertNotNull("AND block should be created", block);
    }

    @Test
    public void testAnd_TrueTrue() {
        ControlAnd block = new ControlAnd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = HIGH;
        calc._inputSignal[1][0] = HIGH;
        calc.calculateYOUT(0.001);
        assertEquals("1 AND 1 = 1", HIGH, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testAnd_TrueFalse() {
        ControlAnd block = new ControlAnd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = HIGH;
        calc._inputSignal[1][0] = LOW;
        calc.calculateYOUT(0.001);
        assertEquals("1 AND 0 = 0", LOW, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testAnd_FalseTrue() {
        ControlAnd block = new ControlAnd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = LOW;
        calc._inputSignal[1][0] = HIGH;
        calc.calculateYOUT(0.001);
        assertEquals("0 AND 1 = 0", LOW, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testAnd_FalseFalse() {
        ControlAnd block = new ControlAnd();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = LOW;
        calc._inputSignal[1][0] = LOW;
        calc.calculateYOUT(0.001);
        assertEquals("0 AND 0 = 0", LOW, calc._outputSignal[0][0], DELTA);
    }

    // ========== OR Gate Tests ==========

    @Test
    public void testOrBlockCreation() {
        ControlOr block = new ControlOr();
        assertNotNull("OR block should be created", block);
    }

    @Test
    public void testOr_TrueTrue() {
        ControlOr block = new ControlOr();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = HIGH;
        calc._inputSignal[1][0] = HIGH;
        calc.calculateYOUT(0.001);
        assertEquals("1 OR 1 = 1", HIGH, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testOr_TrueFalse() {
        ControlOr block = new ControlOr();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = HIGH;
        calc._inputSignal[1][0] = LOW;
        calc.calculateYOUT(0.001);
        assertEquals("1 OR 0 = 1", HIGH, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testOr_FalseTrue() {
        ControlOr block = new ControlOr();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = LOW;
        calc._inputSignal[1][0] = HIGH;
        calc.calculateYOUT(0.001);
        assertEquals("0 OR 1 = 1", HIGH, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testOr_FalseFalse() {
        ControlOr block = new ControlOr();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = LOW;
        calc._inputSignal[1][0] = LOW;
        calc.calculateYOUT(0.001);
        assertEquals("0 OR 0 = 0", LOW, calc._outputSignal[0][0], DELTA);
    }

    // ========== NOT Gate Tests ==========

    @Test
    public void testNotBlockCreation() {
        ControlNOT block = new ControlNOT();
        assertNotNull("NOT block should be created", block);
    }

    @Test
    public void testNot_True() {
        ControlNOT block = new ControlNOT();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = HIGH;
        calc.calculateYOUT(0.001);
        assertEquals("NOT 1 = 0", LOW, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testNot_False() {
        ControlNOT block = new ControlNOT();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        initializeInputs(calc);
        calc._inputSignal[0][0] = LOW;
        calc.calculateYOUT(0.001);
        assertEquals("NOT 0 = 1", HIGH, calc._outputSignal[0][0], DELTA);
    }

    // ========== XOR Gate Tests ==========

    @Test
    public void testXorBlockCreation() {
        ControlExclusiveOr block = new ControlExclusiveOr();
        assertNotNull("XOR block should be created", block);
    }

    @Test
    public void testXor_TrueTrue() {
        ControlExclusiveOr block = new ControlExclusiveOr();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = HIGH;
        calc._inputSignal[1][0] = HIGH;
        calc.calculateYOUT(0.001);
        assertEquals("1 XOR 1 = 0", LOW, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testXor_TrueFalse() {
        ControlExclusiveOr block = new ControlExclusiveOr();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = HIGH;
        calc._inputSignal[1][0] = LOW;
        calc.calculateYOUT(0.001);
        assertEquals("1 XOR 0 = 1", HIGH, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testXor_FalseTrue() {
        ControlExclusiveOr block = new ControlExclusiveOr();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = LOW;
        calc._inputSignal[1][0] = HIGH;
        calc.calculateYOUT(0.001);
        assertEquals("0 XOR 1 = 1", HIGH, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testXor_FalseFalse() {
        ControlExclusiveOr block = new ControlExclusiveOr();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = LOW;
        calc._inputSignal[1][0] = LOW;
        calc.calculateYOUT(0.001);
        assertEquals("0 XOR 0 = 0", LOW, calc._outputSignal[0][0], DELTA);
    }

    // ========== Greater Than Tests ==========

    @Test
    public void testGreaterThanBlockCreation() {
        ControlGreaterThan block = new ControlGreaterThan();
        assertNotNull("GT block should be created", block);
    }

    @Test
    public void testGreaterThan_True() {
        ControlGreaterThan block = new ControlGreaterThan();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 3.0;
        calc.calculateYOUT(0.001);
        assertEquals("5 > 3 = true", HIGH, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testGreaterThan_False() {
        ControlGreaterThan block = new ControlGreaterThan();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 3.0;
        calc._inputSignal[1][0] = 5.0;
        calc.calculateYOUT(0.001);
        assertEquals("3 > 5 = false", LOW, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testGreaterThan_Equal() {
        ControlGreaterThan block = new ControlGreaterThan();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 5.0;
        calc.calculateYOUT(0.001);
        assertEquals("5 > 5 = false", LOW, calc._outputSignal[0][0], DELTA);
    }

    // ========== Greater or Equal Tests ==========

    @Test
    public void testGreaterEqualBlockCreation() {
        ControlGreaterEqual block = new ControlGreaterEqual();
        assertNotNull("GE block should be created", block);
    }

    @Test
    public void testGreaterEqual_Greater() {
        ControlGreaterEqual block = new ControlGreaterEqual();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 3.0;
        calc.calculateYOUT(0.001);
        assertEquals("5 >= 3 = true", HIGH, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testGreaterEqual_Equal() {
        ControlGreaterEqual block = new ControlGreaterEqual();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 5.0;
        calc.calculateYOUT(0.001);
        assertEquals("5 >= 5 = true", HIGH, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testGreaterEqual_Less() {
        ControlGreaterEqual block = new ControlGreaterEqual();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 3.0;
        calc._inputSignal[1][0] = 5.0;
        calc.calculateYOUT(0.001);
        assertEquals("3 >= 5 = false", LOW, calc._outputSignal[0][0], DELTA);
    }

    // ========== Equal Tests ==========

    @Test
    public void testEqualBlockCreation() {
        ControlEqual block = new ControlEqual();
        assertNotNull("EQ block should be created", block);
    }

    @Test
    public void testEqual_True() {
        ControlEqual block = new ControlEqual();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 5.0;
        calc.calculateYOUT(0.001);
        assertEquals("5 == 5 = true", HIGH, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testEqual_False() {
        ControlEqual block = new ControlEqual();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 3.0;
        calc.calculateYOUT(0.001);
        assertEquals("5 == 3 = false", LOW, calc._outputSignal[0][0], DELTA);
    }

    // ========== Not Equal Tests ==========

    @Test
    public void testNotEqualBlockCreation() {
        ControlNE block = new ControlNE();
        assertNotNull("NE block should be created", block);
    }

    @Test
    public void testNotEqual_True() {
        ControlNE block = new ControlNE();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 3.0;
        calc.calculateYOUT(0.001);
        assertEquals("5 != 3 = true", HIGH, calc._outputSignal[0][0], DELTA);
    }

    @Test
    public void testNotEqual_False() {
        ControlNE block = new ControlNE();
        AbstractControlCalculatable calc = block.getInternalControlCalculatableForSimulationStart();
        initializeInputs(calc);
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 5.0;
        calc.calculateYOUT(0.001);
        assertEquals("5 != 5 = false", LOW, calc._outputSignal[0][0], DELTA);
    }
}
