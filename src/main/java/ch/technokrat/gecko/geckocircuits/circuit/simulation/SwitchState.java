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
 * Represents the state of a power semiconductor switch.
 * Extracted from SimulationsKern to centralize switch state management.
 * 
 * <p>This enum models the finite state machine for power switches:
 * <ul>
 *   <li>OFF - Switch is blocking (high resistance)</li>
 *   <li>ON - Switch is conducting (low resistance)</li>
 *   <li>TURNING_ON - Transitioning from OFF to ON</li>
 *   <li>TURNING_OFF - Transitioning from ON to OFF</li>
 * </ul>
 * 
 * <p>Different switch types have different transition rules:
 * <ul>
 *   <li>Ideal Switch: Instant transitions based on gate signal</li>
 *   <li>MOSFET: Gate-controlled, instant transitions</li>
 *   <li>IGBT: Gate-controlled turn-on, current-zero turn-off</li>
 *   <li>Thyristor: Gate-triggered turn-on, current-zero turn-off</li>
 *   <li>Diode: Voltage-controlled (forward biased = ON)</li>
 * </ul>
 * 
 * @author Sprint 4 refactoring
 */
public enum SwitchState {
    
    /**
     * Switch is OFF (blocking state).
     * High resistance (rOff), no current flows.
     */
    OFF(false, "Blocking"),
    
    /**
     * Switch is ON (conducting state).
     * Low resistance (rOn), current can flow.
     */
    ON(true, "Conducting"),
    
    /**
     * Switch is transitioning from OFF to ON.
     * Used for switches with turn-on delay or soft switching.
     */
    TURNING_ON(false, "Turning On"),
    
    /**
     * Switch is transitioning from ON to OFF.
     * Used for switches with turn-off delay or recovery time.
     */
    TURNING_OFF(true, "Turning Off");
    
    private final boolean conducting;
    private final String displayName;
    
    SwitchState(boolean conducting, String displayName) {
        this.conducting = conducting;
        this.displayName = displayName;
    }
    
    /**
     * Checks if the switch is in a conducting state.
     * Both ON and TURNING_OFF are considered conducting.
     * 
     * @return true if current can flow through the switch
     */
    public boolean isConducting() {
        return conducting;
    }
    
    /**
     * Checks if the switch is in a blocking state.
     * Both OFF and TURNING_ON are considered blocking.
     * 
     * @return true if switch is blocking current
     */
    public boolean isBlocking() {
        return !conducting;
    }
    
    /**
     * Checks if the switch is in a stable state (ON or OFF).
     * 
     * @return true if not transitioning
     */
    public boolean isStable() {
        return this == ON || this == OFF;
    }
    
    /**
     * Checks if the switch is in a transitional state.
     * 
     * @return true if turning on or off
     */
    public boolean isTransitioning() {
        return this == TURNING_ON || this == TURNING_OFF;
    }
    
    /**
     * Gets the display name for UI purposes.
     * 
     * @return Human-readable state name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the opposite stable state.
     * ON → OFF, OFF → ON, transitional states return their target.
     * 
     * @return The opposite or target state
     */
    public SwitchState getOpposite() {
        switch (this) {
            case ON: return OFF;
            case OFF: return ON;
            case TURNING_ON: return OFF;  // Return to original if canceled
            case TURNING_OFF: return ON;   // Return to original if canceled
            default: return OFF;
        }
    }
    
    /**
     * Gets the target state for a transition.
     * TURNING_ON → ON, TURNING_OFF → OFF.
     * 
     * @return Target state or self if stable
     */
    public SwitchState getTargetState() {
        switch (this) {
            case TURNING_ON: return ON;
            case TURNING_OFF: return OFF;
            default: return this;
        }
    }
    
    /**
     * Gets the state from a boolean conducting flag.
     * 
     * @param conducting true for ON, false for OFF
     * @return ON if conducting, OFF otherwise
     */
    public static SwitchState fromConducting(boolean conducting) {
        return conducting ? ON : OFF;
    }
    
    /**
     * Gets the state from a gate signal value.
     * Uses 0.5 threshold as per original GeckoCIRCUITS logic.
     * 
     * @param gateSignal Gate signal value (0-1)
     * @return ON if gateSignal > 0.5, OFF otherwise
     */
    public static SwitchState fromGateSignal(double gateSignal) {
        return gateSignal > 0.5 ? ON : OFF;
    }
    
    /**
     * Gets the state from a resistance value.
     * 
     * @param resistance Current resistance
     * @param rOn On-state resistance threshold
     * @param rOff Off-state resistance threshold  
     * @return ON if resistance closer to rOn, OFF if closer to rOff
     */
    public static SwitchState fromResistance(double resistance, double rOn, double rOff) {
        // Use geometric mean as threshold
        double threshold = Math.sqrt(rOn * rOff);
        return resistance < threshold ? ON : OFF;
    }
    
    /**
     * Gets the state from parameter array index value (legacy support).
     * In original code, par[8] stored gate status (1=ON, 0=OFF).
     * 
     * @param gateStatus Gate status value from parameter array
     * @return ON if gateStatus >= 1, OFF otherwise
     */
    public static SwitchState fromGateStatus(double gateStatus) {
        return gateStatus >= 1.0 ? ON : OFF;
    }
    
    /**
     * Converts state to resistance value.
     * 
     * @param rOn On-state resistance
     * @param rOff Off-state resistance
     * @return rOn if conducting, rOff if blocking
     */
    public double toResistance(double rOn, double rOff) {
        return isConducting() ? rOn : rOff;
    }
    
    /**
     * Converts state to gate status value (legacy support).
     * 
     * @return 1.0 if ON/TURNING_OFF, 0.0 if OFF/TURNING_ON
     */
    public double toGateStatus() {
        return isConducting() ? 1.0 : 0.0;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
