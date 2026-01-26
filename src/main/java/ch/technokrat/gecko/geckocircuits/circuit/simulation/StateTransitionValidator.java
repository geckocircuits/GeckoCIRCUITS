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

/**
 * Validates and manages state transitions for power semiconductor switches.
 * Implements the state machine rules for different switch types.
 * 
 * <p>Transition rules vary by switch type:
 * <ul>
 *   <li>Ideal Switch: Gate-only control, instant transitions</li>
 *   <li>MOSFET: Gate-controlled both directions</li>
 *   <li>IGBT: Gate turn-on, requires current-zero for turn-off</li>
 *   <li>Thyristor: Gate-triggered turn-on, natural commutation turn-off</li>
 *   <li>Diode: Automatic based on forward voltage/current</li>
 * </ul>
 * 
 * <p>Thread-safety: This class is immutable and thread-safe.
 * 
 * @author Sprint 4 refactoring
 */
public class StateTransitionValidator {
    
    /** Gate signal threshold for ON/OFF decision. */
    public static final double GATE_THRESHOLD = 0.5;
    
    /** Current threshold for zero-current detection. */
    public static final double CURRENT_ZERO_THRESHOLD = 1e-9;
    
    /** Voltage threshold for forward bias detection. */
    public static final double FORWARD_VOLTAGE_THRESHOLD = 0.0;
    
    /**
     * Switch type enumeration for transition rule selection.
     */
    public enum SwitchType {
        /** Ideal controllable switch - gate controls both on and off */
        IDEAL_SWITCH,
        
        /** MOSFET - gate controlled, can conduct in both directions */
        MOSFET,
        
        /** IGBT - gate controlled turn-on, current-zero turn-off */
        IGBT,
        
        /** Thyristor/SCR - gate triggered turn-on, natural commutation */
        THYRISTOR,
        
        /** Diode - voltage controlled, no gate */
        DIODE
    }
    
    private final SwitchType switchType;
    private final double forwardVoltage;
    private final double recoveryTime;
    
    /**
     * Creates a validator for the specified switch type.
     * 
     * @param switchType Type of switch
     */
    public StateTransitionValidator(SwitchType switchType) {
        this(switchType, 0.0, 0.0);
    }
    
    /**
     * Creates a validator with full configuration.
     * 
     * @param switchType Type of switch
     * @param forwardVoltage Forward voltage drop (for diodes/thyristors)
     * @param recoveryTime Recovery time for turn-off (for thyristors)
     */
    public StateTransitionValidator(SwitchType switchType, double forwardVoltage, double recoveryTime) {
        this.switchType = switchType;
        this.forwardVoltage = forwardVoltage;
        this.recoveryTime = recoveryTime;
    }
    
    /**
     * Validates whether a state transition is allowed.
     * 
     * @param currentState Current switch state
     * @param requestedState Requested new state
     * @param context Transition context with electrical conditions
     * @return true if transition is valid
     */
    public boolean isTransitionAllowed(SwitchState currentState, SwitchState requestedState, 
                                        TransitionContext context) {
        if (currentState == requestedState) {
            return true; // No transition needed
        }
        
        switch (switchType) {
            case IDEAL_SWITCH:
                return validateIdealSwitchTransition(currentState, requestedState, context);
            case MOSFET:
                return validateMosfetTransition(currentState, requestedState, context);
            case IGBT:
                return validateIgbtTransition(currentState, requestedState, context);
            case THYRISTOR:
                return validateThyristorTransition(currentState, requestedState, context);
            case DIODE:
                return validateDiodeTransition(currentState, requestedState, context);
            default:
                return false;
        }
    }
    
    /**
     * Computes the next state based on current conditions.
     * 
     * @param currentState Current switch state
     * @param context Transition context with electrical conditions
     * @return The next state (may be same as current)
     */
    public SwitchState computeNextState(SwitchState currentState, TransitionContext context) {
        SwitchState requestedState = determineRequestedState(context);
        
        if (isTransitionAllowed(currentState, requestedState, context)) {
            return requestedState;
        }
        
        return currentState;
    }
    
    /**
     * Determines the requested state based on context.
     */
    private SwitchState determineRequestedState(TransitionContext context) {
        switch (switchType) {
            case IDEAL_SWITCH:
            case MOSFET:
                // Pure gate control
                return SwitchState.fromGateSignal(context.getGateSignal());
                
            case IGBT:
            case THYRISTOR:
                // Gate can turn on, but turn-off depends on current
                if (context.getGateSignal() > GATE_THRESHOLD) {
                    return SwitchState.ON;
                }
                // Off requested, but need to check if current allows it
                return SwitchState.OFF;
                
            case DIODE:
                // Voltage controlled
                return context.getVoltage() > forwardVoltage ? SwitchState.ON : SwitchState.OFF;
                
            default:
                return SwitchState.OFF;
        }
    }
    
    // ===== Switch-specific validation methods =====
    
    private boolean validateIdealSwitchTransition(SwitchState current, SwitchState requested,
                                                   TransitionContext context) {
        // Ideal switch: any transition allowed based on gate
        return true;
    }
    
    private boolean validateMosfetTransition(SwitchState current, SwitchState requested,
                                              TransitionContext context) {
        // MOSFET: gate controls both directions
        // Body diode can conduct in reverse even when gate is off
        if (requested == SwitchState.ON) {
            return context.getGateSignal() > GATE_THRESHOLD;
        }
        return context.getGateSignal() <= GATE_THRESHOLD;
    }
    
    private boolean validateIgbtTransition(SwitchState current, SwitchState requested,
                                            TransitionContext context) {
        // IGBT: gate turn-on, needs current-zero for turn-off
        if (current == SwitchState.OFF && requested == SwitchState.ON) {
            // Turn-on: gate must be high
            return context.getGateSignal() > GATE_THRESHOLD;
        }
        
        if (current == SwitchState.ON && requested == SwitchState.OFF) {
            // Turn-off: gate must be low AND current must be near zero
            return context.getGateSignal() <= GATE_THRESHOLD &&
                   Math.abs(context.getCurrent()) < CURRENT_ZERO_THRESHOLD;
        }
        
        return true;
    }
    
    private boolean validateThyristorTransition(SwitchState current, SwitchState requested,
                                                 TransitionContext context) {
        // Thyristor: gate-triggered turn-on, natural commutation turn-off
        if (current == SwitchState.OFF && requested == SwitchState.ON) {
            // Turn-on: gate pulse AND forward voltage
            return context.getGateSignal() > GATE_THRESHOLD &&
                   context.getVoltage() > forwardVoltage;
        }
        
        if (current == SwitchState.ON && requested == SwitchState.OFF) {
            // Turn-off: current must reverse (natural commutation)
            // Also check recovery time has elapsed
            return context.getCurrent() < CURRENT_ZERO_THRESHOLD &&
                   context.getTimeSinceLastTransition() >= recoveryTime;
        }
        
        return true;
    }
    
    private boolean validateDiodeTransition(SwitchState current, SwitchState requested,
                                             TransitionContext context) {
        // Diode: automatic based on voltage when off, current when on
        if (current == SwitchState.OFF && requested == SwitchState.ON) {
            // Turn-on: forward biased
            return context.getVoltage() > forwardVoltage;
        }
        
        if (current == SwitchState.ON && requested == SwitchState.OFF) {
            // Turn-off: current reverses
            return context.getCurrent() < CURRENT_ZERO_THRESHOLD;
        }
        
        return true;
    }
    
    /**
     * Checks if a gate signal requests turn-on.
     * 
     * @param gateSignal Gate signal value
     * @return true if gate requests ON state
     */
    public static boolean isGateOn(double gateSignal) {
        return gateSignal > GATE_THRESHOLD;
    }
    
    /**
     * Checks if current is effectively zero.
     * 
     * @param current Current value
     * @return true if current is below threshold
     */
    public static boolean isCurrentZero(double current) {
        return Math.abs(current) < CURRENT_ZERO_THRESHOLD;
    }
    
    /**
     * Gets the switch type for this validator.
     */
    public SwitchType getSwitchType() {
        return switchType;
    }
    
    /**
     * Gets the forward voltage threshold.
     */
    public double getForwardVoltage() {
        return forwardVoltage;
    }
    
    /**
     * Gets the recovery time.
     */
    public double getRecoveryTime() {
        return recoveryTime;
    }
    
    @Override
    public String toString() {
        return String.format("StateTransitionValidator[type=%s, Uf=%.3f, tRec=%.2e]",
            switchType, forwardVoltage, recoveryTime);
    }
    
    // ===== Inner class for transition context =====
    
    /**
     * Context information for state transition validation.
     * Contains all electrical conditions needed to determine valid transitions.
     */
    public static class TransitionContext {
        private final double gateSignal;
        private final double voltage;
        private final double current;
        private final double time;
        private final double lastTransitionTime;
        
        private TransitionContext(Builder builder) {
            this.gateSignal = builder.gateSignal;
            this.voltage = builder.voltage;
            this.current = builder.current;
            this.time = builder.time;
            this.lastTransitionTime = builder.lastTransitionTime;
        }
        
        /**
         * Gets the gate signal value (0-1).
         */
        public double getGateSignal() {
            return gateSignal;
        }
        
        /**
         * Gets the voltage across the switch.
         */
        public double getVoltage() {
            return voltage;
        }
        
        /**
         * Gets the current through the switch.
         */
        public double getCurrent() {
            return current;
        }
        
        /**
         * Gets the current simulation time.
         */
        public double getTime() {
            return time;
        }
        
        /**
         * Gets the time since the last state transition.
         */
        public double getTimeSinceLastTransition() {
            return time - lastTransitionTime;
        }
        
        /**
         * Creates a new builder for TransitionContext.
         */
        public static Builder builder() {
            return new Builder();
        }
        
        /**
         * Creates a simple context with just gate signal.
         */
        public static TransitionContext forGate(double gateSignal) {
            return builder().gateSignal(gateSignal).build();
        }
        
        /**
         * Creates a context with gate and current (for IGBTs/thyristors).
         */
        public static TransitionContext forGateAndCurrent(double gateSignal, double current) {
            return builder().gateSignal(gateSignal).current(current).build();
        }
        
        /**
         * Creates a context for voltage-controlled devices (diodes).
         */
        public static TransitionContext forVoltageAndCurrent(double voltage, double current) {
            return builder().voltage(voltage).current(current).build();
        }
        
        /**
         * Builder for TransitionContext.
         */
        public static class Builder {
            private double gateSignal = 0.0;
            private double voltage = 0.0;
            private double current = 0.0;
            private double time = 0.0;
            private double lastTransitionTime = 0.0;
            
            public Builder gateSignal(double gateSignal) {
                this.gateSignal = gateSignal;
                return this;
            }
            
            public Builder voltage(double voltage) {
                this.voltage = voltage;
                return this;
            }
            
            public Builder current(double current) {
                this.current = current;
                return this;
            }
            
            public Builder time(double time) {
                this.time = time;
                return this;
            }
            
            public Builder lastTransitionTime(double lastTransitionTime) {
                this.lastTransitionTime = lastTransitionTime;
                return this;
            }
            
            public TransitionContext build() {
                return new TransitionContext(this);
            }
        }
        
        @Override
        public String toString() {
            return String.format("TransitionContext[gate=%.2f, V=%.3f, I=%.3f, t=%.6f]",
                gateSignal, voltage, current, time);
        }
    }
}
