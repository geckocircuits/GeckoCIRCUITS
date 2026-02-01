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
 * Unit tests for StateTransitionValidator.
 * Tests transition rules for different switch types.
 */
public class StateTransitionValidatorTest {
    
    // ===== Constructor Tests =====
    
    @Test
    public void testConstructorSwitchTypeOnly() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IDEAL_SWITCH);
        assertEquals(SwitchType.IDEAL_SWITCH, validator.getSwitchType());
        assertEquals(0.0, validator.getForwardVoltage(), 1e-10);
        assertEquals(0.0, validator.getRecoveryTime(), 1e-10);
    }
    
    @Test
    public void testConstructorFullConfig() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR, 0.7, 1e-6);
        assertEquals(SwitchType.THYRISTOR, validator.getSwitchType());
        assertEquals(0.7, validator.getForwardVoltage(), 1e-10);
        assertEquals(1e-6, validator.getRecoveryTime(), 1e-10);
    }
    
    // ===== Static Helper Method Tests =====
    
    @Test
    public void testIsGateOn() {
        assertTrue(StateTransitionValidator.isGateOn(1.0));
        assertTrue(StateTransitionValidator.isGateOn(0.6));
        assertFalse(StateTransitionValidator.isGateOn(0.5));
        assertFalse(StateTransitionValidator.isGateOn(0.0));
    }
    
    @Test
    public void testIsCurrentZero() {
        assertTrue(StateTransitionValidator.isCurrentZero(0.0));
        assertTrue(StateTransitionValidator.isCurrentZero(1e-10));
        assertTrue(StateTransitionValidator.isCurrentZero(-1e-10));
        assertFalse(StateTransitionValidator.isCurrentZero(1e-8));
        assertFalse(StateTransitionValidator.isCurrentZero(1.0));
    }
    
    // ===== Ideal Switch Tests =====
    
    @Test
    public void testIdealSwitchTurnOn() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IDEAL_SWITCH);
        TransitionContext ctx = TransitionContext.forGate(1.0);
        
        assertTrue(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.ON, ctx));
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.OFF, ctx));
    }
    
    @Test
    public void testIdealSwitchTurnOff() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IDEAL_SWITCH);
        TransitionContext ctx = TransitionContext.forGate(0.0);
        
        assertTrue(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));
        assertEquals(SwitchState.OFF, validator.computeNextState(SwitchState.ON, ctx));
    }
    
    @Test
    public void testIdealSwitchStayOn() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IDEAL_SWITCH);
        TransitionContext ctx = TransitionContext.forGate(1.0);
        
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.ON, ctx));
    }
    
    @Test
    public void testIdealSwitchStayOff() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IDEAL_SWITCH);
        TransitionContext ctx = TransitionContext.forGate(0.0);
        
        assertEquals(SwitchState.OFF, validator.computeNextState(SwitchState.OFF, ctx));
    }
    
    // ===== MOSFET Tests =====
    
    @Test
    public void testMosfetGateTurnOn() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.MOSFET);
        TransitionContext ctx = TransitionContext.forGate(1.0);
        
        assertTrue(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.ON, ctx));
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.OFF, ctx));
    }
    
    @Test
    public void testMosfetGateTurnOff() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.MOSFET);
        TransitionContext ctx = TransitionContext.forGate(0.0);
        
        assertTrue(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));
        assertEquals(SwitchState.OFF, validator.computeNextState(SwitchState.ON, ctx));
    }
    
    @Test
    public void testMosfetRejectTurnOnWithLowGate() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.MOSFET);
        TransitionContext ctx = TransitionContext.forGate(0.3);
        
        assertFalse(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.ON, ctx));
        assertEquals(SwitchState.OFF, validator.computeNextState(SwitchState.OFF, ctx));
    }
    
    // ===== IGBT Tests =====
    
    @Test
    public void testIgbtGateTurnOn() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IGBT);
        TransitionContext ctx = TransitionContext.forGate(1.0);
        
        assertTrue(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.ON, ctx));
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.OFF, ctx));
    }
    
    @Test
    public void testIgbtTurnOffWithCurrentZero() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IGBT);
        TransitionContext ctx = TransitionContext.forGateAndCurrent(0.0, 0.0);
        
        assertTrue(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));
        assertEquals(SwitchState.OFF, validator.computeNextState(SwitchState.ON, ctx));
    }
    
    @Test
    public void testIgbtRejectTurnOffWithCurrent() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IGBT);
        TransitionContext ctx = TransitionContext.forGateAndCurrent(0.0, 10.0); // 10A current
        
        assertFalse(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.ON, ctx));
    }
    
    @Test
    public void testIgbtStaysOnWithGateHighAndCurrent() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.IGBT);
        TransitionContext ctx = TransitionContext.forGateAndCurrent(1.0, 10.0);
        
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.ON, ctx));
    }
    
    // ===== Thyristor Tests =====
    
    @Test
    public void testThyristorGateTriggerWithForwardVoltage() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR, 0.7, 1e-6);
        TransitionContext ctx = TransitionContext.builder()
            .gateSignal(1.0)
            .voltage(1.0)  // Forward biased
            .build();
        
        assertTrue(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.ON, ctx));
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.OFF, ctx));
    }
    
    @Test
    public void testThyristorRejectTurnOnWithReverseVoltage() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR, 0.7, 1e-6);
        TransitionContext ctx = TransitionContext.builder()
            .gateSignal(1.0)
            .voltage(-1.0)  // Reverse biased
            .build();
        
        assertFalse(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.ON, ctx));
        assertEquals(SwitchState.OFF, validator.computeNextState(SwitchState.OFF, ctx));
    }
    
    @Test
    public void testThyristorNaturalCommutation() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR, 0.7, 1e-6);
        TransitionContext ctx = TransitionContext.builder()
            .gateSignal(0.0)
            .current(0.0)  // Current reversed to zero
            .time(1e-3)
            .lastTransitionTime(0.0)  // Long time since last transition
            .build();
        
        assertTrue(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));
        assertEquals(SwitchState.OFF, validator.computeNextState(SwitchState.ON, ctx));
    }
    
    @Test
    public void testThyristorRejectTurnOffWithCurrent() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR, 0.7, 1e-6);
        TransitionContext ctx = TransitionContext.builder()
            .gateSignal(0.0)
            .current(10.0)  // Current still flowing
            .build();
        
        assertFalse(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.ON, ctx));
    }
    
    @Test
    public void testThyristorStaysOnWithGateLow() {
        // Thyristor latches - stays on even with gate low if current flows
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR, 0.7, 1e-6);
        TransitionContext ctx = TransitionContext.builder()
            .gateSignal(0.0)
            .current(5.0)  // Current still flowing
            .voltage(1.0)
            .build();
        
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.ON, ctx));
    }
    
    // ===== Diode Tests =====
    
    @Test
    public void testDiodeForwardBiased() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.DIODE, 0.7, 0.0);
        TransitionContext ctx = TransitionContext.forVoltageAndCurrent(1.0, 0.0);
        
        assertTrue(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.ON, ctx));
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.OFF, ctx));
    }
    
    @Test
    public void testDiodeReverseBiased() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.DIODE, 0.7, 0.0);
        TransitionContext ctx = TransitionContext.forVoltageAndCurrent(-1.0, 0.0);
        
        assertEquals(SwitchState.OFF, validator.computeNextState(SwitchState.OFF, ctx));
    }
    
    @Test
    public void testDiodeTurnOffCurrentReversal() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.DIODE, 0.7, 0.0);
        TransitionContext ctx = TransitionContext.forVoltageAndCurrent(-1.0, 0.0);
        
        assertTrue(validator.isTransitionAllowed(SwitchState.ON, SwitchState.OFF, ctx));
        assertEquals(SwitchState.OFF, validator.computeNextState(SwitchState.ON, ctx));
    }
    
    @Test
    public void testDiodeStaysOnWithCurrent() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.DIODE, 0.7, 0.0);
        TransitionContext ctx = TransitionContext.forVoltageAndCurrent(1.0, 5.0);
        
        assertEquals(SwitchState.ON, validator.computeNextState(SwitchState.ON, ctx));
    }
    
    // ===== Same State Transition Tests =====
    
    @Test
    public void testSameStateAlwaysAllowed() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR);
        TransitionContext ctx = TransitionContext.forGate(0.0);
        
        assertTrue(validator.isTransitionAllowed(SwitchState.ON, SwitchState.ON, ctx));
        assertTrue(validator.isTransitionAllowed(SwitchState.OFF, SwitchState.OFF, ctx));
    }
    
    // ===== TransitionContext Tests =====
    
    @Test
    public void testTransitionContextBuilder() {
        TransitionContext ctx = TransitionContext.builder()
            .gateSignal(0.8)
            .voltage(100.0)
            .current(5.0)
            .time(0.001)
            .lastTransitionTime(0.0005)
            .build();
        
        assertEquals(0.8, ctx.getGateSignal(), 1e-10);
        assertEquals(100.0, ctx.getVoltage(), 1e-10);
        assertEquals(5.0, ctx.getCurrent(), 1e-10);
        assertEquals(0.001, ctx.getTime(), 1e-10);
        assertEquals(0.0005, ctx.getTimeSinceLastTransition(), 1e-10);
    }
    
    @Test
    public void testTransitionContextForGate() {
        TransitionContext ctx = TransitionContext.forGate(0.75);
        assertEquals(0.75, ctx.getGateSignal(), 1e-10);
        assertEquals(0.0, ctx.getVoltage(), 1e-10);
        assertEquals(0.0, ctx.getCurrent(), 1e-10);
    }
    
    @Test
    public void testTransitionContextForGateAndCurrent() {
        TransitionContext ctx = TransitionContext.forGateAndCurrent(1.0, 10.5);
        assertEquals(1.0, ctx.getGateSignal(), 1e-10);
        assertEquals(10.5, ctx.getCurrent(), 1e-10);
    }
    
    @Test
    public void testTransitionContextForVoltageAndCurrent() {
        TransitionContext ctx = TransitionContext.forVoltageAndCurrent(325.0, 15.0);
        assertEquals(325.0, ctx.getVoltage(), 1e-10);
        assertEquals(15.0, ctx.getCurrent(), 1e-10);
    }
    
    @Test
    public void testTransitionContextToString() {
        TransitionContext ctx = TransitionContext.builder()
            .gateSignal(1.0)
            .voltage(100.0)
            .current(5.0)
            .time(0.001)
            .build();
        
        String str = ctx.toString();
        assertTrue(str.contains("gate=1.0"));
        assertTrue(str.contains("100"));
        assertTrue(str.contains("5"));
    }
    
    // ===== ToString Tests =====
    
    @Test
    public void testValidatorToString() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR, 0.7, 1e-6);
        String str = validator.toString();
        assertTrue(str.contains("THYRISTOR"));
        assertTrue(str.contains("0.7"));
    }
    
    // ===== Integration Tests =====
    
    @Test
    public void testTypicalPWMCycle() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.MOSFET);
        
        // Start OFF
        SwitchState state = SwitchState.OFF;
        
        // PWM high -> turn on
        TransitionContext ctx = TransitionContext.forGate(1.0);
        state = validator.computeNextState(state, ctx);
        assertEquals(SwitchState.ON, state);
        
        // PWM still high -> stay on
        state = validator.computeNextState(state, ctx);
        assertEquals(SwitchState.ON, state);
        
        // PWM low -> turn off
        ctx = TransitionContext.forGate(0.0);
        state = validator.computeNextState(state, ctx);
        assertEquals(SwitchState.OFF, state);
        
        // PWM still low -> stay off
        state = validator.computeNextState(state, ctx);
        assertEquals(SwitchState.OFF, state);
    }
    
    @Test
    public void testThyristorPhaseControlCycle() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.THYRISTOR, 0.7, 1e-6);
        SwitchState state = SwitchState.OFF;
        
        // Wait for forward voltage and gate pulse
        TransitionContext ctx = TransitionContext.builder()
            .gateSignal(1.0)
            .voltage(1.0)
            .current(0.0)
            .build();
        state = validator.computeNextState(state, ctx);
        assertEquals(SwitchState.ON, state);
        
        // Current flows, gate can go low - thyristor latches
        ctx = TransitionContext.builder()
            .gateSignal(0.0)
            .voltage(0.8)
            .current(10.0)
            .build();
        state = validator.computeNextState(state, ctx);
        assertEquals(SwitchState.ON, state);  // Still ON (latched)
        
        // Natural commutation - current reverses
        ctx = TransitionContext.builder()
            .gateSignal(0.0)
            .voltage(-0.5)
            .current(0.0)
            .time(1e-3)
            .lastTransitionTime(0.0)
            .build();
        state = validator.computeNextState(state, ctx);
        assertEquals(SwitchState.OFF, state);  // Finally OFF
    }
    
    @Test
    public void testDiodeRectifierCycle() {
        StateTransitionValidator validator = new StateTransitionValidator(SwitchType.DIODE, 0.7, 0.0);
        SwitchState state = SwitchState.OFF;
        
        // Positive half cycle - forward biased
        TransitionContext ctx = TransitionContext.forVoltageAndCurrent(1.0, 0.0);
        state = validator.computeNextState(state, ctx);
        assertEquals(SwitchState.ON, state);
        
        // Peak current
        ctx = TransitionContext.forVoltageAndCurrent(0.8, 10.0);
        state = validator.computeNextState(state, ctx);
        assertEquals(SwitchState.ON, state);
        
        // Current decreasing but still positive
        ctx = TransitionContext.forVoltageAndCurrent(0.75, 5.0);
        state = validator.computeNextState(state, ctx);
        assertEquals(SwitchState.ON, state);
        
        // Zero crossing - turn off
        ctx = TransitionContext.forVoltageAndCurrent(-0.5, 0.0);
        state = validator.computeNextState(state, ctx);
        assertEquals(SwitchState.OFF, state);
        
        // Negative half cycle - reverse biased, stays off
        ctx = TransitionContext.forVoltageAndCurrent(-1.0, 0.0);
        state = validator.computeNextState(state, ctx);
        assertEquals(SwitchState.OFF, state);
    }
}
