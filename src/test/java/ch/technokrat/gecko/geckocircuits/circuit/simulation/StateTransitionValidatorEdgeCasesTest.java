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

import ch.technokrat.gecko.geckocircuits.circuit.simulation.StateTransitionValidator.*;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Edge case tests for StateTransitionValidator.
 * Tests boundary conditions, threshold values, and corner cases.
 * Targets 100% code coverage for the package.
 */
public class StateTransitionValidatorEdgeCasesTest {

    // ===== Boundary Value Tests for Gate Threshold =====

    @Test
    public void testMosfetGateThresholdBoundary() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.MOSFET);

        // Exactly at threshold (0.5)
        TransitionContext ctxAtThreshold = TransitionContext.forGate(0.5);
        assertFalse(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.ON, ctxAtThreshold));

        // Just above threshold (0.500001)
        TransitionContext ctxAboveThreshold = TransitionContext.forGate(0.500001);
        assertTrue(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.ON, ctxAboveThreshold));

        // Just below threshold (0.499999)
        TransitionContext ctxBelowThreshold = TransitionContext.forGate(0.499999);
        assertFalse(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.ON, ctxBelowThreshold));
    }

    @Test
    public void testIgbtGateThresholdBoundary() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IGBT);

        // At threshold for turn-off (0.5 is NOT <= 0.5, so false allows turn-off with low gate)
        TransitionContext ctx = TransitionContext.forGateAndCurrent(0.5, 0.0);
        // 0.5 is not > 0.5, and for turn-off we need gate <= 0.5 AND current near zero
        // So at exactly 0.5, gate <= 0.5 is TRUE, and current is zero, so TRUE
        assertTrue(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));

        // Just above threshold - does NOT allow turn-off (gate is high)
        ctx = TransitionContext.forGateAndCurrent(0.500001, 0.0);
        assertFalse(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));
    }

    // ===== Boundary Value Tests for Current Threshold =====

    @Test
    public void testIgbtCurrentThresholdBoundary() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IGBT);

        // At threshold
        TransitionContext ctx = TransitionContext.forGateAndCurrent(0.0, 1e-9);
        assertFalse(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));

        // Just below threshold
        ctx = TransitionContext.forGateAndCurrent(0.0, 9.9e-10);
        assertTrue(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));

        // Negative current near threshold
        ctx = TransitionContext.forGateAndCurrent(0.0, -1e-9);
        assertFalse(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));

        // Negative current below threshold
        ctx = TransitionContext.forGateAndCurrent(0.0, -9.9e-10);
        assertTrue(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));
    }

    @Test
    public void testThyristorCurrentThresholdBoundary() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR, 0.7, 0.0);

        // At threshold (should not allow)
        TransitionContext ctx = TransitionContext.builder()
            .gateSignal(0.0)
            .current(1e-9)
            .time(1.0)
            .lastTransitionTime(0.0)
            .build();
        assertFalse(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));

        // Just below threshold (should allow)
        ctx = TransitionContext.builder()
            .gateSignal(0.0)
            .current(9.9e-10)
            .time(1.0)
            .lastTransitionTime(0.0)
            .build();
        assertTrue(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));
    }

    // ===== Boundary Value Tests for Forward Voltage =====

    @Test
    public void testDiodeForwardVoltageThresholdBoundary() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.DIODE, 0.7, 0.0);

        // Exactly at threshold
        TransitionContext ctx = TransitionContext.forVoltageAndCurrent(0.7, 0.0);
        assertEquals(SwitchState.OFF, validator.computeNextState(SwitchState.OFF, ctx));

        // Just above threshold
        ctx = TransitionContext.forVoltageAndCurrent(0.700001, 0.0);
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.OFF, ctx));

        // Just below threshold
        ctx = TransitionContext.forVoltageAndCurrent(0.699999, 0.0);
        assertEquals(SwitchState.OFF, validator.computeNextState(SwitchState.OFF, ctx));
    }

    @Test
    public void testThyristorForwardVoltageThresholdBoundary() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR, 0.7, 0.0);

        // At threshold with gate high (should not turn on)
        TransitionContext ctx = TransitionContext.builder()
            .gateSignal(1.0)
            .voltage(0.7)
            .build();
        assertFalse(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.ON, ctx));

        // Just above threshold with gate high (should turn on)
        ctx = TransitionContext.builder()
            .gateSignal(1.0)
            .voltage(0.700001)
            .build();
        assertTrue(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.ON, ctx));
    }

    // ===== Recovery Time Edge Cases =====

    @Test
    public void testThyristorRecoveryTimeThreshold() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR, 0.7, 1e-6);

        // Exactly at recovery time
        TransitionContext ctx = TransitionContext.builder()
            .gateSignal(0.0)
            .current(0.0)
            .time(1e-6)
            .lastTransitionTime(0.0)
            .build();
        assertTrue(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));

        // Just before recovery time
        ctx = TransitionContext.builder()
            .gateSignal(0.0)
            .current(0.0)
            .time(9.99e-7)
            .lastTransitionTime(0.0)
            .build();
        assertFalse(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));
    }

    @Test
    public void testThyristorRecoveryTimeWithLargeTimeValues() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR, 0.7, 1e-6);

        // Large simulation time
        TransitionContext ctx = TransitionContext.builder()
            .gateSignal(0.0)
            .current(0.0)
            .time(1.0)
            .lastTransitionTime(0.99999)  // 1e-5 seconds since transition - longer than recovery time
            .build();
        assertTrue(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));
    }

    @Test
    public void testThyristorZeroRecoveryTime() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR, 0.7, 0.0);

        // No recovery time required
        TransitionContext ctx = TransitionContext.builder()
            .gateSignal(0.0)
            .current(0.0)
            .time(0.0)
            .lastTransitionTime(0.0)
            .build();
        assertTrue(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));
    }

    // ===== Negative Values Edge Cases =====

    @Test
    public void testNegativeVoltageValues() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.DIODE, 0.7, 0.0);

        // Negative voltage with large magnitude
        TransitionContext ctx = TransitionContext.forVoltageAndCurrent(-100.0, 0.0);
        assertEquals(SwitchState.OFF, validator.computeNextState(SwitchState.OFF, ctx));
    }

    @Test
    public void testNegativeCurrentValues() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IGBT);

        // Negative current (reverse current) - abs(-10.0) = 10.0 which is NOT < 1e-9
        TransitionContext ctx = TransitionContext.forGateAndCurrent(0.0, -10.0);
        assertFalse(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.ON, ctx));
    }

    @Test
    public void testDiodeNegativeCurrentForTurnOff() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.DIODE, 0.7, 0.0);

        // Negative current (reverse)
        TransitionContext ctx = TransitionContext.forVoltageAndCurrent(-0.5, -0.1);
        assertEquals(SwitchState.OFF, validator.computeNextState(SwitchState.ON, ctx));
    }

    // ===== Large Value Edge Cases =====

    @Test
    public void testLargeGateSignalValues() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.MOSFET);

        // Very large gate signal (>1.0 is physically impossible but should work)
        TransitionContext ctx = TransitionContext.forGate(100.0);
        assertTrue(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.ON, ctx));
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.OFF, ctx));
    }

    @Test
    public void testLargeCurrentValues() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IGBT);

        // Very large current (1000A)
        TransitionContext ctx = TransitionContext.forGateAndCurrent(1.0, 1000.0);
        assertTrue(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.ON, ctx));
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.OFF, ctx));
    }

    @Test
    public void testLargeVoltageValues() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.DIODE, 0.7, 0.0);

        // Very large voltage (1000V)
        TransitionContext ctx = TransitionContext.forVoltageAndCurrent(1000.0, 0.0);
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.OFF, ctx));
    }

    // ===== Zero Value Edge Cases =====

    @Test
    public void testZeroForwardVoltage() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.DIODE, 0.0, 0.0);

        // Any positive voltage (even tiny) should turn on
        TransitionContext ctx = TransitionContext.forVoltageAndCurrent(1e-12, 0.0);
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.OFF, ctx));
    }

    @Test
    public void testZeroGateSignal() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IDEAL_SWITCH);

        // Gate low -> OFF
        TransitionContext ctx = TransitionContext.forGate(0.0);
        assertEquals(SwitchState.OFF, validator.computeNextState(SwitchState.ON, ctx));
    }

    // ===== Extreme Parameter Combinations =====

    @Test
    public void testIgbtRejectionBothConditions() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IGBT);

        // Turn-off requires both: gate low AND current zero
        // This tests: gate high, current near zero
        TransitionContext ctx = TransitionContext.forGateAndCurrent(0.6, 0.0);
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.ON, ctx));
    }

    @Test
    public void testThyristorTurnOnRequiresBothConditions() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR, 0.7, 0.0);

        // Turn-on requires: gate high AND forward voltage
        // Test: gate high, voltage too low
        TransitionContext ctx = TransitionContext.builder()
            .gateSignal(1.0)
            .voltage(0.6)  // Below 0.7 threshold
            .build();
        assertFalse(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.ON, ctx));

        // Test: gate low, forward voltage high
        ctx = TransitionContext.builder()
            .gateSignal(0.3)  // Below 0.5 threshold
            .voltage(1.0)
            .build();
        assertFalse(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.ON, ctx));
    }

    // ===== Transition Context Builder Edge Cases =====

    @Test
    public void testTransitionContextWithAllDefaults() {
        TransitionContext ctx = TransitionContext.builder().build();
        assertEquals(0.0, ctx.getGateSignal(), 1e-10);
        assertEquals(0.0, ctx.getVoltage(), 1e-10);
        assertEquals(0.0, ctx.getCurrent(), 1e-10);
        assertEquals(0.0, ctx.getTime(), 1e-10);
        assertEquals(0.0, ctx.getTimeSinceLastTransition(), 1e-10);
    }

    @Test
    public void testTransitionContextTimeSinceLastTransitionEdgeCases() {
        // Time goes backward (shouldn't happen, but test it)
        TransitionContext ctx = TransitionContext.builder()
            .time(0.5)
            .lastTransitionTime(1.0)
            .build();
        assertEquals(-0.5, ctx.getTimeSinceLastTransition(), 1e-10);

        // Zero elapsed time
        ctx = TransitionContext.builder()
            .time(1.0)
            .lastTransitionTime(1.0)
            .build();
        assertEquals(0.0, ctx.getTimeSinceLastTransition(), 1e-10);
    }

    // ===== Multiple State Transitions (Path Coverage) =====

    @Test
    public void testMosfetDoubleTransition() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.MOSFET);
        SwitchState state = SwitchState.OFF;

        // OFF -> ON
        state = validator.computeNextState(state, TransitionContext.forGate(1.0));
        assertEquals(SwitchState.ON, state);

        // ON -> OFF
        state = validator.computeNextState(state, TransitionContext.forGate(0.0));
        assertEquals(SwitchState.OFF, state);

        // OFF -> ON again
        state = validator.computeNextState(state, TransitionContext.forGate(1.0));
        assertEquals(SwitchState.ON, state);
    }

    @Test
    public void testIdealSwitchFastToggling() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IDEAL_SWITCH);
        SwitchState state = SwitchState.OFF;

        // Rapid on-off cycles
        for (int i = 0; i < 5; i++) {
            state = validator.computeNextState(state, TransitionContext.forGate(1.0));
            assertEquals(SwitchState.ON, state);

            state = validator.computeNextState(state, TransitionContext.forGate(0.0));
            assertEquals(SwitchState.OFF, state);
        }
    }

    // ===== Transient State Transitions =====

    @Test
    public void testTransitionThroughTurningOnState() {
        // Test WITH the TURNING_ON state
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.MOSFET);

        // From OFF with gate high
        TransitionContext ctx = TransitionContext.forGate(1.0);
        SwitchState state = validator.computeNextState(SwitchState.TURNING_ON, ctx);
        assertEquals(SwitchState.ON, state);
    }

    @Test
    public void testTransitionThroughTurningOffState() {
        // Test WITH the TURNING_OFF state
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.MOSFET);

        // From TURNING_OFF with gate low
        TransitionContext ctx = TransitionContext.forGate(0.0);
        SwitchState state = validator.computeNextState(SwitchState.TURNING_OFF, ctx);
        assertEquals(SwitchState.OFF, state);
    }

    // ===== Constructor Variations =====

    @Test
    public void testConstructorWithNegativeForwardVoltage() {
        // Physically impossible but test it
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.DIODE, -0.5, 0.0);
        assertEquals(-0.5, validator.getForwardVoltage(), 1e-10);

        // Negative voltage is always > -0.5, so diode should conduct
        TransitionContext ctx = TransitionContext.forVoltageAndCurrent(0.0, 0.0);
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.OFF, ctx));
    }

    @Test
    public void testConstructorWithNegativeRecoveryTime() {
        // Physically impossible but test it
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR, 0.7, -1e-6);
        assertEquals(-1e-6, validator.getRecoveryTime(), 1e-10);
    }

    // ===== Getter Methods =====

    @Test
    public void testAllGetterMethods() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR, 1.5, 2e-6);

        assertEquals(SwitchType.THYRISTOR, validator.getSwitchType());
        assertEquals(1.5, validator.getForwardVoltage(), 1e-10);
        assertEquals(2e-6, validator.getRecoveryTime(), 1e-10);
    }

    // ===== toString Methods =====

    @Test
    public void testValidatorToStringFormat() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IGBT, 0.5, 5e-7);
        String str = validator.toString();

        // Should contain type, forward voltage, and recovery time
        assertTrue(str.contains("IGBT"));
        assertTrue(str.contains("0.5"));
        assertTrue(str.contains("5"));
    }

    @Test
    public void testTransitionContextToStringFormat() {
        TransitionContext ctx = TransitionContext.builder()
            .gateSignal(0.75)
            .voltage(50.0)
            .current(10.0)
            .time(0.001)
            .build();

        String str = ctx.toString();
        assertTrue(str.contains("TransitionContext"));
        assertTrue(str.contains("0.75"));
        assertTrue(str.contains("50"));
        assertTrue(str.contains("10"));
    }
}
