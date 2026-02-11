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
 * Unit tests for ControlInputTwoTerminalStateable interface.
 * Tests state management for control input terminals.
 */
public class ControlInputTwoTerminalStateableTest {

    /**
     * Mock implementation of ControlInputTwoTerminalStateable for testing.
     */
    private static class MockControlInputTwoTerminal implements ControlInputTwoTerminalStateable {
        private boolean _folded = false;
        private boolean _externalUsed = false;

        @Override
        public void setFolded() {
            _folded = true;
        }

        @Override
        public void setExpanded() {
            _folded = false;
        }

        @Override
        public boolean isExternalSet() {
            return _externalUsed;
        }

        @Override
        public void setExternalUsed(final boolean value) {
            _externalUsed = value;
        }

        public boolean isFolded() {
            return _folded;
        }
    }

    @Test
    public void testFoldState() {
        MockControlInputTwoTerminal terminal = new MockControlInputTwoTerminal();
        assertFalse("Initial state should be expanded", terminal.isFolded());

        terminal.setFolded();
        assertTrue("After setFolded(), should be folded", terminal.isFolded());
    }

    @Test
    public void testExpandState() {
        MockControlInputTwoTerminal terminal = new MockControlInputTwoTerminal();
        terminal.setFolded();
        assertTrue("Should be folded after setFolded()", terminal.isFolded());

        terminal.setExpanded();
        assertFalse("After setExpanded(), should be expanded", terminal.isFolded());
    }

    @Test
    public void testFoldExpandCycle() {
        MockControlInputTwoTerminal terminal = new MockControlInputTwoTerminal();

        terminal.setFolded();
        assertTrue("Should be folded", terminal.isFolded());

        terminal.setExpanded();
        assertFalse("Should be expanded", terminal.isFolded());

        terminal.setFolded();
        assertTrue("Should be folded again", terminal.isFolded());
    }

    @Test
    public void testExternalNotUsedInitially() {
        MockControlInputTwoTerminal terminal = new MockControlInputTwoTerminal();
        assertFalse("External should not be set initially", terminal.isExternalSet());
    }

    @Test
    public void testSetExternalUsed() {
        MockControlInputTwoTerminal terminal = new MockControlInputTwoTerminal();

        terminal.setExternalUsed(true);
        assertTrue("External should be set after setExternalUsed(true)", terminal.isExternalSet());
    }

    @Test
    public void testUnsetExternalUsed() {
        MockControlInputTwoTerminal terminal = new MockControlInputTwoTerminal();
        terminal.setExternalUsed(true);
        assertTrue("Should be set", terminal.isExternalSet());

        terminal.setExternalUsed(false);
        assertFalse("External should be unset after setExternalUsed(false)", terminal.isExternalSet());
    }

    @Test
    public void testExternalUsedToggling() {
        MockControlInputTwoTerminal terminal = new MockControlInputTwoTerminal();

        terminal.setExternalUsed(true);
        assertTrue("Should be set", terminal.isExternalSet());

        terminal.setExternalUsed(false);
        assertFalse("Should be unset", terminal.isExternalSet());

        terminal.setExternalUsed(true);
        assertTrue("Should be set again", terminal.isExternalSet());
    }

    @Test
    public void testFoldAndExternalIndependent() {
        MockControlInputTwoTerminal terminal = new MockControlInputTwoTerminal();

        terminal.setFolded();
        terminal.setExternalUsed(true);

        assertTrue("Should be folded", terminal.isFolded());
        assertTrue("External should be set", terminal.isExternalSet());
    }

    @Test
    public void testExpandAndExternalIndependent() {
        MockControlInputTwoTerminal terminal = new MockControlInputTwoTerminal();

        terminal.setFolded();
        terminal.setExternalUsed(true);
        terminal.setExpanded();

        assertFalse("Should be expanded", terminal.isFolded());
        assertTrue("External should still be set", terminal.isExternalSet());
    }

    @Test
    public void testMultipleSetFoldedCalls() {
        MockControlInputTwoTerminal terminal = new MockControlInputTwoTerminal();

        terminal.setFolded();
        terminal.setFolded();
        terminal.setFolded();

        assertTrue("Should be folded after multiple calls", terminal.isFolded());
    }

    @Test
    public void testMultipleSetExpandedCalls() {
        MockControlInputTwoTerminal terminal = new MockControlInputTwoTerminal();
        terminal.setFolded();

        terminal.setExpanded();
        terminal.setExpanded();
        terminal.setExpanded();

        assertFalse("Should be expanded after multiple calls", terminal.isFolded());
    }

    @Test
    public void testInterfaceCompliance() {
        // Verify that the interface can be instantiated and used polymorphically
        ControlInputTwoTerminalStateable terminal = new MockControlInputTwoTerminal();
        assertNotNull("Interface instance should not be null", terminal);

        terminal.setFolded();
        assertTrue("Interface should work polymorphically", ((MockControlInputTwoTerminal) terminal).isFolded());
    }
}
