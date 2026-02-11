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
package ch.technokrat.gecko.geckocircuits.newscope;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Tests for AxisTickSettings - axis tick configuration.
 */
public class AxisTickSettingsTest {

    private AxisTickSettings settings;

    @Before
    public void setUp() {
        settings = new AxisTickSettings();
    }

    // ====================================================
    // Default Values Tests
    // ====================================================

    @Test
    public void testDefaultAutoTickSpacing_IsTrue() {
        assertTrue("Default auto tick spacing should be true", settings.isAutoTickSpacing());
    }

    @Test
    public void testDefaultShowLabelsMaj_IsTrue() {
        assertTrue("Default show major labels should be true", settings.isShowLabelsMaj());
    }

    @Test
    public void testDefaultShowLabelsMin_IsFalse() {
        assertFalse("Default show minor labels should be false", settings.isShowLabelsMin());
    }

    @Test
    public void testDefaultTickLengthMaj_IsEight() {
        assertEquals("Default major tick length should be 8", 8, settings.getTickLengthMaj());
    }

    @Test
    public void testDefaultTickLengthMin_IsFive() {
        assertEquals("Default minor tick length should be 5", 5, settings.getTickLengthMin());
    }

    @Test
    public void testDefaultAnzTicksMinor_IsTwo() {
        assertEquals("Default number of minor ticks should be 2", 2, settings.getAnzTicksMinor());
    }

    // ====================================================
    // Auto Tick Spacing Tests
    // ====================================================

    @Test
    public void testSetAutoTickSpacing_True() {
        settings.setAutoTickSpacing(true);
        assertTrue("Auto tick spacing should be true after setting", settings.isAutoTickSpacing());
    }

    @Test
    public void testSetAutoTickSpacing_False() {
        settings.setAutoTickSpacing(false);
        assertFalse("Auto tick spacing should be false after setting", settings.isAutoTickSpacing());
    }

    @Test
    public void testSetAutoTickSpacing_Toggle() {
        assertTrue("Initial auto tick spacing should be true", settings.isAutoTickSpacing());
        settings.setAutoTickSpacing(false);
        assertFalse("After toggle to false", settings.isAutoTickSpacing());
        settings.setAutoTickSpacing(true);
        assertTrue("After toggle back to true", settings.isAutoTickSpacing());
    }

    // ====================================================
    // Show Labels Tests
    // ====================================================

    @Test
    public void testSetShowLabelsMaj_True() {
        settings.setShowLabelsMaj(true);
        assertTrue("Major labels should be shown", settings.isShowLabelsMaj());
    }

    @Test
    public void testSetShowLabelsMaj_False() {
        settings.setShowLabelsMaj(false);
        assertFalse("Major labels should be hidden", settings.isShowLabelsMaj());
    }

    @Test
    public void testSetShowLabelsMin_True() {
        settings.setShowLabelsMin(true);
        assertTrue("Minor labels should be shown", settings.isShowLabelsMin());
    }

    @Test
    public void testSetShowLabelsMin_False() {
        settings.setShowLabelsMin(false);
        assertFalse("Minor labels should be hidden", settings.isShowLabelsMin());
    }

    // ====================================================
    // Tick Length Tests
    // ====================================================

    @Test
    public void testSetTickLengthMaj_CustomValue() {
        settings.setTickLengthMaj(12);
        assertEquals("Major tick length should be 12", 12, settings.getTickLengthMaj());
    }

    @Test
    public void testSetTickLengthMaj_Zero() {
        settings.setTickLengthMaj(0);
        assertEquals("Major tick length should be 0", 0, settings.getTickLengthMaj());
    }

    @Test
    public void testSetTickLengthMin_CustomValue() {
        settings.setTickLengthMin(3);
        assertEquals("Minor tick length should be 3", 3, settings.getTickLengthMin());
    }

    @Test
    public void testSetTickLengthMin_Zero() {
        settings.setTickLengthMin(0);
        assertEquals("Minor tick length should be 0", 0, settings.getTickLengthMin());
    }

    @Test
    public void testSetTickLengthMaj_LargeValue() {
        settings.setTickLengthMaj(100);
        assertEquals("Major tick length should be 100", 100, settings.getTickLengthMaj());
    }

    // ====================================================
    // Number of Minor Ticks Tests
    // ====================================================

    @Test
    public void testSetAnzTicksMinor_CustomValue() {
        settings.setAnzTicksMinor(5);
        assertEquals("Number of minor ticks should be 5", 5, settings.getAnzTicksMinor());
    }

    @Test
    public void testSetAnzTicksMinor_One() {
        settings.setAnzTicksMinor(1);
        assertEquals("Number of minor ticks should be 1", 1, settings.getAnzTicksMinor());
    }

    @Test
    public void testSetAnzTicksMinor_Ten() {
        settings.setAnzTicksMinor(10);
        assertEquals("Number of minor ticks should be 10", 10, settings.getAnzTicksMinor());
    }

    @Test
    public void testSetAnzTicksMinor_Zero() {
        settings.setAnzTicksMinor(0);
        assertEquals("Number of minor ticks should be 0", 0, settings.getAnzTicksMinor());
    }

    // ====================================================
    // State Persistence Tests
    // ====================================================

    @Test
    public void testMultipleStateChanges_IndependentProperties() {
        settings.setAutoTickSpacing(false);
        settings.setShowLabelsMaj(false);
        settings.setShowLabelsMin(true);
        settings.setTickLengthMaj(15);
        settings.setAnzTicksMinor(4);

        assertFalse("Auto tick spacing should remain false", settings.isAutoTickSpacing());
        assertFalse("Major labels should remain hidden", settings.isShowLabelsMaj());
        assertTrue("Minor labels should remain visible", settings.isShowLabelsMin());
        assertEquals("Major tick length should remain 15", 15, settings.getTickLengthMaj());
        assertEquals("Minor ticks should remain 4", 4, settings.getAnzTicksMinor());
    }

    @Test
    public void testTickLengthIndependentFromCount() {
        settings.setTickLengthMaj(10);
        settings.setTickLengthMin(6);
        settings.setAnzTicksMinor(3);

        assertEquals("Major tick length should be independent", 10, settings.getTickLengthMaj());
        assertEquals("Minor tick length should be independent", 6, settings.getTickLengthMin());
        assertEquals("Minor tick count should be independent", 3, settings.getAnzTicksMinor());
    }

    // ====================================================
    // Boundary Tests
    // ====================================================

    @Test
    public void testSetTickLengthMaj_LargeBoundary() {
        settings.setTickLengthMaj(Integer.MAX_VALUE);
        assertEquals("Major tick length should handle max int", Integer.MAX_VALUE, settings.getTickLengthMaj());
    }

    @Test
    public void testSetAnzTicksMinor_LargeBoundary() {
        settings.setAnzTicksMinor(Integer.MAX_VALUE);
        assertEquals("Minor ticks should handle max int", Integer.MAX_VALUE, settings.getAnzTicksMinor());
    }

    @Test
    public void testMultipleInstances_Independent() {
        AxisTickSettings settings2 = new AxisTickSettings();

        settings.setTickLengthMaj(20);
        settings2.setTickLengthMaj(30);

        assertEquals("First instance should have its own value", 20, settings.getTickLengthMaj());
        assertEquals("Second instance should have its own value", 30, settings2.getTickLengthMaj());
    }

    // ====================================================
    // Getter/Setter Consistency Tests
    // ====================================================

    @Test
    public void testSetGetConsistency_AllProperties() {
        // Set various values
        settings.setAutoTickSpacing(false);
        settings.setShowLabelsMaj(false);
        settings.setShowLabelsMin(true);
        settings.setTickLengthMaj(16);
        settings.setTickLengthMin(7);
        settings.setAnzTicksMinor(6);

        // Verify all values are exactly as set
        assertFalse("Auto tick spacing consistency", settings.isAutoTickSpacing());
        assertFalse("Show major labels consistency", settings.isShowLabelsMaj());
        assertTrue("Show minor labels consistency", settings.isShowLabelsMin());
        assertEquals("Major tick length consistency", 16, settings.getTickLengthMaj());
        assertEquals("Minor tick length consistency", 7, settings.getTickLengthMin());
        assertEquals("Minor ticks count consistency", 6, settings.getAnzTicksMinor());
    }

    @Test
    public void testGetterReturnsCurrentState() {
        // Change and verify each property individually
        settings.setAutoTickSpacing(false);
        assertFalse(settings.isAutoTickSpacing());

        settings.setAutoTickSpacing(true);
        assertTrue(settings.isAutoTickSpacing());

        settings.setShowLabelsMaj(false);
        assertFalse(settings.isShowLabelsMaj());
    }
}
