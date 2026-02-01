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
package ch.technokrat.gecko.geckocircuits.circuit.simulation;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Edge case tests for SwitchState enum.
 * Tests boundary conditions, extreme values, and corner cases.
 * Targets 100% code coverage for the enum.
 */
public class SwitchStateEdgeCasesTest {

    // ===== fromResistance Boundary Tests =====

    @Test
    public void testFromResistanceGeometricMeanCalculation() {
        // Test the geometric mean threshold calculation
        double rOn = 1.0;
        double rOff = 100.0;
        double threshold = Math.sqrt(rOn * rOff); // sqrt(100) = 10.0

        assertEquals(SwitchState.ON, SwitchState.fromResistance(9.99, rOn, rOff));
        assertEquals(SwitchState.OFF, SwitchState.fromResistance(10.01, rOn, rOff));
    }

    @Test
    public void testFromResistanceWithVerySmallOnResistance() {
        // Very small on-resistance (superconductor-like)
        double rOn = 1e-12;
        double rOff = 1e6;
        double threshold = Math.sqrt(rOn * rOff);

        assertEquals(SwitchState.ON, SwitchState.fromResistance(1e-10, rOn, rOff));
        assertEquals(SwitchState.OFF, SwitchState.fromResistance(1e4, rOn, rOff));
    }

    @Test
    public void testFromResistanceWithVeryLargeOffResistance() {
        // Very large off-resistance
        double rOn = 0.1;
        double rOff = 1e12;
        double threshold = Math.sqrt(rOn * rOff);

        assertEquals(SwitchState.ON, SwitchState.fromResistance(1e4, rOn, rOff));
        assertEquals(SwitchState.OFF, SwitchState.fromResistance(1e8, rOn, rOff));
    }

    @Test
    public void testFromResistanceEqualResistances() {
        // When rOn = rOff, threshold should be rOn = rOff
        double r = 100.0;
        assertEquals(SwitchState.OFF, SwitchState.fromResistance(r, r, r));
        assertEquals(SwitchState.ON, SwitchState.fromResistance(r - 0.1, r, r));
    }

    @Test
    public void testFromResistanceAtExactThreshold() {
        double rOn = 1.0;
        double rOff = 100.0;
        double threshold = Math.sqrt(rOn * rOff); // 10.0

        SwitchState state = SwitchState.fromResistance(threshold, rOn, rOff);
        // At exact threshold, < should be false, so OFF
        assertEquals(SwitchState.OFF, state);
    }

    // ===== fromGateSignal Boundary Tests =====

    @Test
    public void testFromGateSignalNegativeValues() {
        // Negative gate signal (physically impossible)
        assertEquals(SwitchState.OFF, SwitchState.fromGateSignal(-1.0));
        assertEquals(SwitchState.OFF, SwitchState.fromGateSignal(-0.5));
    }

    @Test
    public void testFromGateSignalValuesGreaterThanOne() {
        // Gate signal > 1.0 (physically impossible)
        assertEquals(SwitchState.ON, SwitchState.fromGateSignal(1.5));
        assertEquals(SwitchState.ON, SwitchState.fromGateSignal(100.0));
    }

    // ===== fromGateStatus Tests (Legacy Support) =====

    @Test
    public void testFromGateStatusNegativeValues() {
        // Negative gate status
        assertEquals(SwitchState.OFF, SwitchState.fromGateStatus(-1.0));
        assertEquals(SwitchState.OFF, SwitchState.fromGateStatus(-0.1));
    }

    @Test
    public void testFromGateStatusGreaterThanOne() {
        // Gate status > 1.0
        assertEquals(SwitchState.ON, SwitchState.fromGateStatus(2.0));
        assertEquals(SwitchState.ON, SwitchState.fromGateStatus(100.0));
    }

    @Test
    public void testFromGateStatusAtThreshold() {
        // Exactly at 1.0
        assertEquals(SwitchState.ON, SwitchState.fromGateStatus(1.0));
        // Just below 1.0
        assertEquals(SwitchState.OFF, SwitchState.fromGateStatus(0.9999));
    }

    // ===== toResistance Edge Cases =====

    @Test
    public void testToResistanceWithZeroResistances() {
        // Edge case: both resistances are zero
        assertEquals(0.0, SwitchState.ON.toResistance(0.0, 0.0), 1e-10);
        assertEquals(0.0, SwitchState.OFF.toResistance(0.0, 0.0), 1e-10);
    }

    @Test
    public void testToResistanceWithNegativeResistances() {
        // Physically impossible but test it
        double rOn = -1.0;
        double rOff = -100.0;
        assertEquals(rOn, SwitchState.ON.toResistance(rOn, rOff), 1e-10);
        assertEquals(rOff, SwitchState.OFF.toResistance(rOn, rOff), 1e-10);
    }

    @Test
    public void testToResistanceTransitioningStates() {
        double rOn = 0.01;
        double rOff = 1e6;

        // TURNING_ON is blocking, so should return rOff
        assertEquals(rOff, SwitchState.TURNING_ON.toResistance(rOn, rOff), 1e-10);

        // TURNING_OFF is conducting, so should return rOn
        assertEquals(rOn, SwitchState.TURNING_OFF.toResistance(rOn, rOff), 1e-10);
    }

    // ===== toGateStatus Edge Cases =====

    @Test
    public void testToGateStatusReturnValues() {
        // ON and TURNING_OFF should return 1.0
        assertEquals(1.0, SwitchState.ON.toGateStatus(), 1e-10);
        assertEquals(1.0, SwitchState.TURNING_OFF.toGateStatus(), 1e-10);

        // OFF and TURNING_ON should return 0.0
        assertEquals(0.0, SwitchState.OFF.toGateStatus(), 1e-10);
        assertEquals(0.0, SwitchState.TURNING_ON.toGateStatus(), 1e-10);
    }

    // ===== getOpposite with All States =====

    @Test
    public void testGetOppositeSymmetry() {
        // ON <-> OFF should be symmetric: ON -> OFF -> ON
        assertEquals(SwitchState.ON, SwitchState.ON.getOpposite().getOpposite());
        assertEquals(SwitchState.OFF, SwitchState.OFF.getOpposite().getOpposite());

        // TURNING_ON opposite is OFF
        assertEquals(SwitchState.OFF, SwitchState.TURNING_ON.getOpposite());
        // OFF opposite is ON (so not symmetric with TURNING_ON)
        assertEquals(SwitchState.ON, SwitchState.OFF.getOpposite());

        // TURNING_OFF opposite is ON
        assertEquals(SwitchState.ON, SwitchState.TURNING_OFF.getOpposite());
        // ON opposite is OFF (so not symmetric with TURNING_OFF)
        assertEquals(SwitchState.OFF, SwitchState.ON.getOpposite());
    }

    // ===== getTargetState with All States =====

    @Test
    public void testGetTargetStateTransitions() {
        // TURNING_ON targets ON
        assertEquals(SwitchState.ON, SwitchState.TURNING_ON.getTargetState());
        // TURNING_OFF targets OFF
        assertEquals(SwitchState.OFF, SwitchState.TURNING_OFF.getTargetState());
        // ON targets ON (already stable)
        assertEquals(SwitchState.ON, SwitchState.ON.getTargetState());
        // OFF targets OFF (already stable)
        assertEquals(SwitchState.OFF, SwitchState.OFF.getTargetState());
    }

    // ===== State Combination Tests =====

    @Test
    public void testAllStateCombinationsOfIsConducting() {
        assertTrue(SwitchState.ON.isConducting());
        assertFalse(SwitchState.OFF.isConducting());
        assertFalse(SwitchState.TURNING_ON.isConducting());
        assertTrue(SwitchState.TURNING_OFF.isConducting());
    }

    @Test
    public void testAllStateCombinationsOfIsBlocking() {
        assertFalse(SwitchState.ON.isBlocking());
        assertTrue(SwitchState.OFF.isBlocking());
        assertTrue(SwitchState.TURNING_ON.isBlocking());
        assertFalse(SwitchState.TURNING_OFF.isBlocking());
    }

    @Test
    public void testAllStateCombinationsOfIsStable() {
        assertTrue(SwitchState.ON.isStable());
        assertTrue(SwitchState.OFF.isStable());
        assertFalse(SwitchState.TURNING_ON.isStable());
        assertFalse(SwitchState.TURNING_OFF.isStable());
    }

    @Test
    public void testAllStateCombinationsOfIsTransitioning() {
        assertFalse(SwitchState.ON.isTransitioning());
        assertFalse(SwitchState.OFF.isTransitioning());
        assertTrue(SwitchState.TURNING_ON.isTransitioning());
        assertTrue(SwitchState.TURNING_OFF.isTransitioning());
    }

    // ===== Display Name Tests =====

    @Test
    public void testDisplayNameConsistency() {
        for (SwitchState state : SwitchState.values()) {
            String displayName = state.getDisplayName();
            assertNotNull("Display name should not be null", displayName);
            assertFalse("Display name should not be empty", displayName.isEmpty());
        }
    }

    @Test
    public void testToStringEqualsDisplayName() {
        for (SwitchState state : SwitchState.values()) {
            assertEquals(state.getDisplayName(), state.toString());
        }
    }

    // ===== Enum Iteration Tests =====

    @Test
    public void testEnumValues() {
        SwitchState[] states = SwitchState.values();
        assertEquals(4, states.length);

        // Verify all expected states exist
        assertArrayContains(states, SwitchState.ON);
        assertArrayContains(states, SwitchState.OFF);
        assertArrayContains(states, SwitchState.TURNING_ON);
        assertArrayContains(states, SwitchState.TURNING_OFF);
    }

    @Test
    public void testValueOfAllStates() {
        assertEquals(SwitchState.ON, SwitchState.valueOf("ON"));
        assertEquals(SwitchState.OFF, SwitchState.valueOf("OFF"));
        assertEquals(SwitchState.TURNING_ON, SwitchState.valueOf("TURNING_ON"));
        assertEquals(SwitchState.TURNING_OFF, SwitchState.valueOf("TURNING_OFF"));
    }

    // ===== roundTrip Conversion Tests =====

    @Test
    public void testFromConductingRoundTrip() {
        // Note: fromConducting only distinguishes ON/OFF, not transitional states
        // TURNING_ON (blocking) -> false -> OFF
        // TURNING_OFF (conducting) -> true -> ON
        assertEquals(SwitchState.ON, SwitchState.fromConducting(SwitchState.ON.isConducting()));
        assertEquals(SwitchState.OFF, SwitchState.fromConducting(SwitchState.OFF.isConducting()));
        assertEquals(SwitchState.OFF, SwitchState.fromConducting(SwitchState.TURNING_ON.isConducting()));
        assertEquals(SwitchState.ON, SwitchState.fromConducting(SwitchState.TURNING_OFF.isConducting()));
    }

    @Test
    public void testGateSignalRoundTripEdgeCases() {
        // Test that converting back and forth maintains state
        double[] testValues = {0.0, 0.25, 0.4999, 0.5, 0.5001, 0.75, 1.0};

        for (double signal : testValues) {
            SwitchState state = SwitchState.fromGateSignal(signal);
            double gateStatus = state.toGateStatus();
            SwitchState recovered = SwitchState.fromGateStatus(gateStatus);
            assertEquals("Round trip for signal " + signal, state, recovered);
        }
    }

    @Test
    public void testResistanceRoundTripWithExtremesROn() {
        double rOn = 1e-6;  // Very small
        double rOff = 1e6;  // Very large

        // Test ON -> resistance -> back to ON
        double rOnResistance = SwitchState.ON.toResistance(rOn, rOff);
        assertEquals(SwitchState.ON, SwitchState.fromResistance(rOnResistance, rOn, rOff));

        // Test OFF -> resistance -> back to OFF
        double rOffResistance = SwitchState.OFF.toResistance(rOn, rOff);
        assertEquals(SwitchState.OFF, SwitchState.fromResistance(rOffResistance, rOn, rOff));
    }

    // ===== Transitional State Sequences =====

    @Test
    public void testTurnOnSequence() {
        // Sequence: OFF -> TURNING_ON -> ON
        SwitchState state = SwitchState.OFF;
        assertTrue(state.isBlocking());
        assertFalse(state.isConducting());

        state = SwitchState.TURNING_ON;
        assertTrue(state.isBlocking());
        assertFalse(state.isConducting());
        assertTrue(state.isTransitioning());
        assertEquals(SwitchState.ON, state.getTargetState());

        state = SwitchState.ON;
        assertFalse(state.isBlocking());
        assertTrue(state.isConducting());
        assertFalse(state.isTransitioning());
    }

    @Test
    public void testTurnOffSequence() {
        // Sequence: ON -> TURNING_OFF -> OFF
        SwitchState state = SwitchState.ON;
        assertFalse(state.isBlocking());
        assertTrue(state.isConducting());

        state = SwitchState.TURNING_OFF;
        assertFalse(state.isBlocking());
        assertTrue(state.isConducting());
        assertTrue(state.isTransitioning());
        assertEquals(SwitchState.OFF, state.getTargetState());

        state = SwitchState.OFF;
        assertTrue(state.isBlocking());
        assertFalse(state.isConducting());
        assertFalse(state.isTransitioning());
    }

    // ===== Equality and Comparison Tests =====

    @Test
    public void testStateEquality() {
        // Same states should be equal
        assertEquals(SwitchState.ON, SwitchState.ON);
        assertEquals(SwitchState.OFF, SwitchState.OFF);

        // Different states should not be equal
        assertNotEquals(SwitchState.ON, SwitchState.OFF);
        assertNotEquals(SwitchState.TURNING_ON, SwitchState.TURNING_OFF);
    }

    // ===== Helper Methods =====

    private void assertArrayContains(SwitchState[] array, SwitchState element) {
        for (SwitchState state : array) {
            if (state == element) {
                return;
            }
        }
        fail("Array does not contain element: " + element);
    }
}
