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
 * Unit tests for SpecialNameVisible interface.
 * Tests name visibility state management for control components.
 */
public class SpecialNameVisibleTest {

    /**
     * Mock implementation of SpecialNameVisible for testing.
     */
    private static class MockSpecialNameVisible implements SpecialNameVisible {
        private boolean _nameVisible = true;

        @Override
        public boolean isNameVisible() {
            return _nameVisible;
        }

        @Override
        public void setNameVisible(final boolean newValue) {
            _nameVisible = newValue;
        }
    }

    @Test
    public void testNameVisibleByDefault() {
        MockSpecialNameVisible component = new MockSpecialNameVisible();
        assertTrue("Name should be visible by default", component.isNameVisible());
    }

    @Test
    public void testSetNameVisible() {
        MockSpecialNameVisible component = new MockSpecialNameVisible();
        component.setNameVisible(true);
        assertTrue("Name should be visible after setNameVisible(true)", component.isNameVisible());
    }

    @Test
    public void testSetNameInvisible() {
        MockSpecialNameVisible component = new MockSpecialNameVisible();
        component.setNameVisible(false);
        assertFalse("Name should be invisible after setNameVisible(false)", component.isNameVisible());
    }

    @Test
    public void testToggleNameVisibility() {
        MockSpecialNameVisible component = new MockSpecialNameVisible();

        component.setNameVisible(false);
        assertFalse("Should be invisible", component.isNameVisible());

        component.setNameVisible(true);
        assertTrue("Should be visible again", component.isNameVisible());
    }

    @Test
    public void testMultipleToggles() {
        MockSpecialNameVisible component = new MockSpecialNameVisible();

        component.setNameVisible(false);
        component.setNameVisible(true);
        component.setNameVisible(false);
        component.setNameVisible(true);

        assertTrue("Should be visible after multiple toggles", component.isNameVisible());
    }

    @Test
    public void testSetNameVisibleMultipleTimes() {
        MockSpecialNameVisible component = new MockSpecialNameVisible();

        component.setNameVisible(false);
        component.setNameVisible(false);
        component.setNameVisible(false);

        assertFalse("Should be invisible after multiple false calls", component.isNameVisible());
    }

    @Test
    public void testSetNameVisibleTrueManyTimes() {
        MockSpecialNameVisible component = new MockSpecialNameVisible();
        component.setNameVisible(false);

        component.setNameVisible(true);
        component.setNameVisible(true);
        component.setNameVisible(true);

        assertTrue("Should be visible after multiple true calls", component.isNameVisible());
    }

    @Test
    public void testInterfacePolymorphism() {
        // Test that the interface can be used polymorphically
        SpecialNameVisible component = new MockSpecialNameVisible();
        assertNotNull("Interface instance should not be null", component);

        component.setNameVisible(false);
        assertFalse("Should work polymorphically", component.isNameVisible());

        component.setNameVisible(true);
        assertTrue("Should work polymorphically", component.isNameVisible());
    }

    @Test
    public void testIndependentInstances() {
        MockSpecialNameVisible component1 = new MockSpecialNameVisible();
        MockSpecialNameVisible component2 = new MockSpecialNameVisible();

        component1.setNameVisible(false);
        component2.setNameVisible(true);

        assertFalse("First component should be invisible", component1.isNameVisible());
        assertTrue("Second component should be visible", component2.isNameVisible());
    }

    @Test
    public void testDefaultState() {
        MockSpecialNameVisible component1 = new MockSpecialNameVisible();
        MockSpecialNameVisible component2 = new MockSpecialNameVisible();

        assertTrue("First component should default to visible", component1.isNameVisible());
        assertTrue("Second component should default to visible", component2.isNameVisible());
    }

    @Test
    public void testStateAfterMultipleModifications() {
        MockSpecialNameVisible component = new MockSpecialNameVisible();

        for (int i = 0; i < 10; i++) {
            component.setNameVisible(i % 2 == 0);
        }

        assertFalse("After 10 iterations, should be invisible (last was false)", component.isNameVisible());
    }

    @Test
    public void testBoundaryConditions() {
        MockSpecialNameVisible component = new MockSpecialNameVisible();

        // Test extreme state changes
        component.setNameVisible(false);
        assertFalse("Should be invisible", component.isNameVisible());

        component.setNameVisible(true);
        assertTrue("Should be visible", component.isNameVisible());

        // Verify state remains consistent
        assertTrue("State should be consistent", component.isNameVisible());
        assertTrue("State should remain consistent on second check", component.isNameVisible());
    }
}
