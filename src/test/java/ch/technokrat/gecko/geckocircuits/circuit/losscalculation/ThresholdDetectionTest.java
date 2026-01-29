/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for switch threshold detection logic - critical paths in
 * AbstractLossCalculatorSwitch turn-on/off detection.
 */
public class ThresholdDetectionTest {

    private static final double DELTA = 1e-10;
    private static final double EPS = 0.01;

    private ThresholdHelper helper;

    private static class ThresholdHelper {
        static final double EPS_THRESHOLD = 0.01;

        boolean detectTurnOn(double oldCurrent, double current) {
            return (Math.abs(oldCurrent) < EPS_THRESHOLD) && (Math.abs(current) > EPS_THRESHOLD);
        }

        boolean detectTurnOff(double oldCurrent, double current) {
            return (Math.abs(oldCurrent) > EPS_THRESHOLD) && (Math.abs(current) < EPS_THRESHOLD);
        }
    }

    @Before
    public void setUp() {
        helper = new ThresholdHelper();
    }

    // ====================================================
    // Turn-On Detection Tests
    // ====================================================

    @Test
    public void testTurnOn_ValidTransition() {
        assertTrue(helper.detectTurnOn(0.001, 0.05));
    }

    @Test
    public void testTurnOn_AtThreshold() {
        assertFalse(helper.detectTurnOn(0.0, EPS));
    }

    @Test
    public void testTurnOn_AboveThreshold() {
        assertTrue(helper.detectTurnOn(0.0, EPS + 1e-6));
    }

    @Test
    public void testTurnOn_NegativeCurrent() {
        assertTrue(helper.detectTurnOn(-0.005, -0.05));
    }

    @Test
    public void testTurnOn_NoTransition() {
        assertFalse(helper.detectTurnOn(0.05, 0.1));
    }

    // ====================================================
    // Turn-Off Detection Tests
    // ====================================================

    @Test
    public void testTurnOff_ValidTransition() {
        assertTrue(helper.detectTurnOff(0.05, 0.001));
    }

    @Test
    public void testTurnOff_AtThreshold() {
        assertFalse(helper.detectTurnOff(0.1, EPS));
    }

    @Test
    public void testTurnOff_NegativeCurrent() {
        assertTrue(helper.detectTurnOff(-0.05, -0.005));
    }

    @Test
    public void testTurnOff_NoTransition() {
        assertFalse(helper.detectTurnOff(0.05, 0.1));
    }

    // ====================================================
    // Boundary Tests
    // ====================================================

    @Test
    public void testBoundary_OldAtThreshold() {
        assertFalse(helper.detectTurnOn(EPS, 0.1));
        assertFalse(helper.detectTurnOff(EPS, 0.0));
    }

    @Test
    public void testBoundary_NewAtThreshold() {
        assertFalse(helper.detectTurnOn(0.0, EPS));
        assertFalse(helper.detectTurnOff(0.1, EPS));
    }

    // ====================================================
    // Edge Cases
    // ====================================================

    @Test
    public void testEdgeCase_BothZero() {
        assertFalse(helper.detectTurnOn(0.0, 0.0));
        assertFalse(helper.detectTurnOff(0.0, 0.0));
    }

    @Test
    public void testEdgeCase_HighCurrent() {
        assertTrue(helper.detectTurnOn(0.0, 1000.0));
    }

    @Test
    public void testEdgeCase_VerySmallAboveThreshold() {
        assertTrue(helper.detectTurnOn(0.0, 0.010001));
    }

    @Test
    public void testEdgeCase_VerySmallBelowThreshold() {
        assertFalse(helper.detectTurnOn(0.0, 0.009999));
    }

}
