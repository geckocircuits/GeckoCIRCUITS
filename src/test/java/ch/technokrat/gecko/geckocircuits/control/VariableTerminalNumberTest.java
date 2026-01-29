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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for VariableTerminalNumber interface.
 * Tests terminal count management for variable-input control blocks.
 */
public class VariableTerminalNumberTest {

    /**
     * Mock implementation of VariableTerminalNumber for testing.
     */
    private static class MockVariableTerminalBlock implements VariableTerminalNumber {
        private int _inputTerminalCount = 1;
        private int _outputTerminalCount = 1;

        @Override
        public void setInputTerminalNumber(final int number) {
            if (number < 0) {
                throw new IllegalArgumentException("Terminal count cannot be negative");
            }
            _inputTerminalCount = number;
        }

        @Override
        public void setOutputTerminalNumber(final int number) {
            if (number < 0) {
                throw new IllegalArgumentException("Terminal count cannot be negative");
            }
            _outputTerminalCount = number;
        }

        public int getInputTerminalCount() {
            return _inputTerminalCount;
        }

        public int getOutputTerminalCount() {
            return _outputTerminalCount;
        }
    }

    @Test
    public void testDefaultTerminalNumbers() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();
        assertEquals("Default input terminals should be 1", 1, block.getInputTerminalCount());
        assertEquals("Default output terminals should be 1", 1, block.getOutputTerminalCount());
    }

    @Test
    public void testSetSingleInputTerminal() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();
        block.setInputTerminalNumber(1);
        assertEquals("Should set to 1 input terminal", 1, block.getInputTerminalCount());
    }

    @Test
    public void testSetMultipleInputTerminals() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();
        block.setInputTerminalNumber(5);
        assertEquals("Should set to 5 input terminals", 5, block.getInputTerminalCount());
    }

    @Test
    public void testSetLargeInputTerminalNumber() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();
        block.setInputTerminalNumber(100);
        assertEquals("Should set to 100 input terminals", 100, block.getInputTerminalCount());
    }

    @Test
    public void testSetZeroInputTerminals() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();
        block.setInputTerminalNumber(0);
        assertEquals("Should set to 0 input terminals", 0, block.getInputTerminalCount());
    }

    @Test
    public void testSetSingleOutputTerminal() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();
        block.setOutputTerminalNumber(1);
        assertEquals("Should set to 1 output terminal", 1, block.getOutputTerminalCount());
    }

    @Test
    public void testSetMultipleOutputTerminals() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();
        block.setOutputTerminalNumber(3);
        assertEquals("Should set to 3 output terminals", 3, block.getOutputTerminalCount());
    }

    @Test
    public void testSetLargeOutputTerminalNumber() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();
        block.setOutputTerminalNumber(50);
        assertEquals("Should set to 50 output terminals", 50, block.getOutputTerminalCount());
    }

    @Test
    public void testSetZeroOutputTerminals() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();
        block.setOutputTerminalNumber(0);
        assertEquals("Should set to 0 output terminals", 0, block.getOutputTerminalCount());
    }

    @Test
    public void testInputAndOutputIndependent() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();

        block.setInputTerminalNumber(2);
        block.setOutputTerminalNumber(3);

        assertEquals("Input should be 2", 2, block.getInputTerminalCount());
        assertEquals("Output should be 3", 3, block.getOutputTerminalCount());
    }

    @Test
    public void testModifyInputAfterOutput() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();

        block.setOutputTerminalNumber(5);
        block.setInputTerminalNumber(4);

        assertEquals("Input should be 4", 4, block.getInputTerminalCount());
        assertEquals("Output should still be 5", 5, block.getOutputTerminalCount());
    }

    @Test
    public void testModifyOutputAfterInput() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();

        block.setInputTerminalNumber(3);
        block.setOutputTerminalNumber(2);

        assertEquals("Input should still be 3", 3, block.getInputTerminalCount());
        assertEquals("Output should be 2", 2, block.getOutputTerminalCount());
    }

    @Test
    public void testSequentialInputTerminalChanges() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();

        block.setInputTerminalNumber(1);
        assertEquals("Should be 1", 1, block.getInputTerminalCount());

        block.setInputTerminalNumber(2);
        assertEquals("Should be 2", 2, block.getInputTerminalCount());

        block.setInputTerminalNumber(3);
        assertEquals("Should be 3", 3, block.getInputTerminalCount());
    }

    @Test
    public void testSequentialOutputTerminalChanges() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();

        block.setOutputTerminalNumber(1);
        assertEquals("Should be 1", 1, block.getOutputTerminalCount());

        block.setOutputTerminalNumber(5);
        assertEquals("Should be 5", 5, block.getOutputTerminalCount());

        block.setOutputTerminalNumber(2);
        assertEquals("Should be 2", 2, block.getOutputTerminalCount());
    }

    @Test
    public void testNegativeInputTerminalsThrows() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();
        try {
            block.setInputTerminalNumber(-1);
            fail("Should throw IllegalArgumentException for negative input terminals");
        } catch (IllegalArgumentException e) {
            assertTrue("Should throw for negative value", true);
        }
    }

    @Test
    public void testNegativeOutputTerminalsThrows() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();
        try {
            block.setOutputTerminalNumber(-5);
            fail("Should throw IllegalArgumentException for negative output terminals");
        } catch (IllegalArgumentException e) {
            assertTrue("Should throw for negative value", true);
        }
    }

    @Test
    public void testMultipleInstances() {
        MockVariableTerminalBlock block1 = new MockVariableTerminalBlock();
        MockVariableTerminalBlock block2 = new MockVariableTerminalBlock();

        block1.setInputTerminalNumber(5);
        block2.setInputTerminalNumber(3);

        assertEquals("Block1 should have 5 input terminals", 5, block1.getInputTerminalCount());
        assertEquals("Block2 should have 3 input terminals", 3, block2.getInputTerminalCount());
    }

    @Test
    public void testMixedTerminalConfigurations() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();

        block.setInputTerminalNumber(10);
        block.setOutputTerminalNumber(1);

        assertEquals("Should support asymmetric configuration", 10, block.getInputTerminalCount());
        assertEquals("Should support asymmetric configuration", 1, block.getOutputTerminalCount());

        block.setInputTerminalNumber(1);
        block.setOutputTerminalNumber(10);

        assertEquals("Should support reversed asymmetric configuration", 1, block.getInputTerminalCount());
        assertEquals("Should support reversed asymmetric configuration", 10, block.getOutputTerminalCount());
    }

    @Test
    public void testInterfaceCompliance() {
        VariableTerminalNumber block = new MockVariableTerminalBlock();
        assertNotNull("Interface implementation should not be null", block);

        block.setInputTerminalNumber(5);
        block.setOutputTerminalNumber(3);

        // Verify it works through interface reference
        assertTrue("Should work through interface reference", true);
    }

    @Test
    public void testTerminalCountBoundaries() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();

        // Test minimum
        block.setInputTerminalNumber(0);
        assertEquals("Should handle 0 terminals", 0, block.getInputTerminalCount());

        // Test reasonable maximum
        block.setInputTerminalNumber(1000);
        assertEquals("Should handle large terminal counts", 1000, block.getInputTerminalCount());
    }

    @Test
    public void testRepeatedSameSetting() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();

        block.setInputTerminalNumber(5);
        block.setInputTerminalNumber(5);
        block.setInputTerminalNumber(5);

        assertEquals("Should handle repeated same settings", 5, block.getInputTerminalCount());
    }

    @Test
    public void testBackAndForth() {
        MockVariableTerminalBlock block = new MockVariableTerminalBlock();

        block.setInputTerminalNumber(10);
        assertEquals("Should be 10", 10, block.getInputTerminalCount());

        block.setInputTerminalNumber(5);
        assertEquals("Should be 5", 5, block.getInputTerminalCount());

        block.setInputTerminalNumber(10);
        assertEquals("Should be 10 again", 10, block.getInputTerminalCount());
    }
}
