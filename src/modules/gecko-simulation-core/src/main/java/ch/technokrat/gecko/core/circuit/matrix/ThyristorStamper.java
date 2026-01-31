/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.core.circuit.matrix;

/**
 * Matrix stamper implementation for Thyristor (SCR) components.
 *
 * The Thyristor is modeled as a latching switch with these characteristics:
 * - ON state: Low resistance (rOn) with forward voltage drop, only conducts forward current
 * - OFF state: High resistance (rOff) - blocking
 *
 * Key characteristics (from LKMatrices.java):
 * - Gate-triggered turn-on: Requires gate pulse AND forward voltage > threshold
 * - Current-zero turn-off: Cannot be turned off by gate, only turns OFF when current reaches zero
 * - Reverse recovery time: Stays OFF for a minimum time after turning off
 * - Latching: Once triggered ON, stays ON regardless of gate signal (until current zero)
 *
 * Parameter mapping (from LKMatrices):
 * - parameter[0] = current resistance (rD)
 * - parameter[1] = forward voltage (uf)
 * - parameter[2] = ON resistance (rON)
 * - parameter[3] = OFF resistance (rOFF)
 * - parameter[4] = current (i)
 * - parameter[5] = voltage (u)
 * - parameter[8] = gate signal (0 or 1)
 * - parameter[9] = reverse recovery time (tq)
 * - parameter[11] = last switch-off time
 *
 * A matrix stamps (same pattern as diode):
 * - a[x][x] += G (where G = 1/R)
 * - a[y][y] += G
 * - a[x][y] -= G
 * - a[y][x] -= G
 *
 * B vector stamps (for forward voltage compensation when ON):
 * - b[x] += G * uf
 * - b[y] -= G * uf
 *
 * @author GeckoCIRCUITS Team
 */
public class ThyristorStamper implements IStatefulStamper {

    /** Parameter index for current resistance (rD) */
    public static final int PARAM_R_CURRENT = 0;

    /** Parameter index for forward voltage */
    public static final int PARAM_U_FORWARD = 1;

    /** Parameter index for ON resistance */
    public static final int PARAM_R_ON = 2;

    /** Parameter index for OFF resistance */
    public static final int PARAM_R_OFF = 3;

    /** Parameter index for current through component */
    public static final int PARAM_CURRENT = 4;

    /** Parameter index for voltage across component */
    public static final int PARAM_VOLTAGE = 5;

    /** Parameter index for gate signal (0 or 1) */
    public static final int PARAM_GATE = 8;

    /** Parameter index for reverse recovery time */
    public static final int PARAM_RECOVERY_TIME = 9;

    /** Parameter index for last switch-off time */
    public static final int PARAM_LAST_SWITCH_TIME = 11;

    /** Default ON resistance (very low) */
    public static final double DEFAULT_R_ON = 1e-3;

    /** Default OFF resistance (very high) - from AbstractSwitch.RD_OFF_DEFAULT */
    public static final double DEFAULT_R_OFF = 1e9;

    /** Default forward voltage threshold */
    public static final double DEFAULT_U_FORWARD = 1.5;

    /** Default reverse recovery time in seconds */
    public static final double DEFAULT_RECOVERY_TIME = 10e-6;

    /** Minimum resistance to avoid numerical issues */
    private static final double MIN_RESISTANCE = 1e-12;

    /** Maximum resistance to avoid numerical issues */
    private static final double MAX_RESISTANCE = 1e15;

    /** Perturbation factor for switching threshold (from LKMatrices: stoergroesse) */
    private static final double PERTURBATION_FACTOR = 0.99;

    /** Acceptance threshold for voltage comparison */
    private static final double ACCEPTANCE_THRESHOLD = 1e-6;

    /** Resistance threshold determining ON state (from LKMatrices: 0.5 * rOff) */
    private static final double ON_STATE_FACTOR = 0.5;

    /** Current ON/OFF state */
    private boolean isOn;

    /** Flag indicating state changed in last updateState call */
    private boolean stateChanged;

    /** ON resistance */
    private double rOn;

    /** OFF resistance */
    private double rOff;

    /** Forward voltage drop */
    private double uForward;

    /** Reverse recovery time */
    private double recoveryTime;

    /** Last switch-off time */
    private double lastSwitchOffTime;

    /**
     * Creates a thyristor stamper with default parameters.
     */
    public ThyristorStamper() {
        this(DEFAULT_R_ON, DEFAULT_R_OFF, DEFAULT_U_FORWARD, DEFAULT_RECOVERY_TIME);
    }

    /**
     * Creates a thyristor stamper with specified parameters.
     *
     * @param rOn ON state resistance in Ohms
     * @param rOff OFF state resistance in Ohms
     * @param uForward forward voltage drop in Volts
     * @param recoveryTime reverse recovery time in seconds
     */
    public ThyristorStamper(double rOn, double rOff, double uForward, double recoveryTime) {
        this.rOn = Math.max(rOn, MIN_RESISTANCE);
        this.rOff = Math.min(Math.max(rOff, MIN_RESISTANCE), MAX_RESISTANCE);
        this.uForward = uForward;
        this.recoveryTime = recoveryTime;
        this.isOn = false;
        this.stateChanged = false;
        this.lastSwitchOffTime = Double.NEGATIVE_INFINITY;
    }

    @Override
    public void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt) {
        double resistance = getCurrentResistance(parameter);
        double admittance = 1.0 / resistance;

        // Stamp standard two-terminal admittance pattern
        a[nodeX][nodeX] += admittance;
        a[nodeY][nodeY] += admittance;
        a[nodeX][nodeY] -= admittance;
        a[nodeY][nodeX] -= admittance;
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt, double time,
                             double[] previousValues) {
        // When ON (low resistance), compensate for forward voltage drop
        double resistance = getCurrentResistance(parameter);
        double rOffThreshold = ON_STATE_FACTOR * getROff(parameter);

        if (resistance < rOffThreshold) {
            double uf = getUForward(parameter);
            if (uf > 0) {
                double admittance = 1.0 / resistance;
                double compensation = admittance * uf;

                // Forward voltage compensation
                b[nodeX] += compensation;
                b[nodeY] -= compensation;
            }
        }
    }

    @Override
    public double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                                   double[] parameter, double dt, double previousCurrent) {
        double resistance = getCurrentResistance(parameter);
        double uf = getUForward(parameter);
        double vDiff = nodeVoltageX - nodeVoltageY;

        if (isOn) {
            // Current through conducting thyristor: I = (V - Uf) / rOn
            return (vDiff - uf) / resistance;
        } else {
            // Leakage current through blocking thyristor
            return vDiff / resistance;
        }
    }

    @Override
    public double getAdmittanceWeight(double parameterValue, double dt) {
        double resistance = isOn ? rOn : rOff;
        resistance = Math.max(resistance, MIN_RESISTANCE);
        return 1.0 / resistance;
    }

    @Override
    public void updateState(double vx, double vy, double current, double time) {
        // Basic state update based on voltage/current only
        // For full thyristor behavior, use updateStateWithGate()

        boolean previousState = isOn;
        double vForward = vx - vy;

        if (isOn) {
            // Currently ON: turn OFF when current goes to zero (forward voltage drops)
            if (vForward < (PERTURBATION_FACTOR * uForward + ACCEPTANCE_THRESHOLD)) {
                // Check if recovery time has passed since any previous off-state
                if (time - lastSwitchOffTime > 3 * recoveryTime) {
                    lastSwitchOffTime = time;
                }

                if (time - lastSwitchOffTime >= recoveryTime) {
                    isOn = false;
                }
            }
        }
        // Note: Turn ON requires gate signal - handled by updateStateWithGate()

        stateChanged = (isOn != previousState);
    }

    /**
     * Updates the thyristor state based on gate signal and circuit conditions.
     * This is the primary method for controlling the thyristor.
     *
     * @param gateSignal gate signal value (0 = no trigger, 1 = trigger)
     * @param vx voltage at node X
     * @param vy voltage at node Y
     * @param time current simulation time
     */
    public void updateStateWithGate(double gateSignal, double vx, double vy, double time) {
        boolean previousState = isOn;
        double vForward = vx - vy;
        boolean gateTrigger = (gateSignal == 1);

        if (isOn) {
            // Currently ON: check for turn-off condition (current zero)
            // Thyristor turns OFF when forward voltage drops below threshold
            // This indicates current is trying to reverse
            if (vForward < (PERTURBATION_FACTOR * uForward + ACCEPTANCE_THRESHOLD)) {
                // Handle recovery time tracking
                if (time - lastSwitchOffTime > 3 * recoveryTime) {
                    lastSwitchOffTime = time;
                }

                if (time - lastSwitchOffTime >= recoveryTime) {
                    isOn = false;
                }
            }
            // Note: Gate signal cannot turn OFF a thyristor (latching behavior)
        } else {
            // Currently OFF: check for turn-on condition
            // Requires gate trigger AND forward voltage above threshold AND recovery time elapsed
            if (gateTrigger &&
                vForward > (PERTURBATION_FACTOR * uForward - ACCEPTANCE_THRESHOLD)) {
                // Check if recovery time has elapsed since last turn-off
                if (time - lastSwitchOffTime >= recoveryTime) {
                    isOn = true;
                }
            }
        }

        stateChanged = (isOn != previousState);
    }

    /**
     * Updates state based on parameter array.
     *
     * @param parameter parameter array
     * @param vx voltage at node X
     * @param vy voltage at node Y
     * @param time current simulation time
     */
    public void updateStateFromParameters(double[] parameter, double vx, double vy, double time) {
        if (parameter != null && parameter.length > PARAM_GATE) {
            // Update recovery time from parameters if available
            if (parameter.length > PARAM_RECOVERY_TIME) {
                this.recoveryTime = parameter[PARAM_RECOVERY_TIME];
            }
            // Update last switch time from parameters if available
            if (parameter.length > PARAM_LAST_SWITCH_TIME) {
                this.lastSwitchOffTime = parameter[PARAM_LAST_SWITCH_TIME];
            }

            updateStateWithGate(parameter[PARAM_GATE], vx, vy, time);
        }
    }

    /**
     * Fires the gate trigger.
     * Note: Thyristor requires both gate trigger AND forward voltage condition.
     *
     * @param vx voltage at node X
     * @param vy voltage at node Y
     * @param time current simulation time
     */
    public void fireTrigger(double vx, double vy, double time) {
        updateStateWithGate(1.0, vx, vy, time);
    }

    @Override
    public boolean isStateChanged() {
        return stateChanged;
    }

    @Override
    public void resetStateChange() {
        stateChanged = false;
    }

    @Override
    public boolean isOn() {
        return isOn;
    }

    @Override
    public void setState(boolean on) {
        boolean previousState = isOn;
        isOn = on;
        stateChanged = (isOn != previousState);
    }

    @Override
    public double getCurrentResistance() {
        return isOn ? rOn : rOff;
    }

    /**
     * Gets current resistance, using parameter array if provided.
     *
     * @param parameter optional parameter array
     * @return current effective resistance
     */
    private double getCurrentResistance(double[] parameter) {
        if (parameter != null && parameter.length > PARAM_R_CURRENT) {
            double r = parameter[PARAM_R_CURRENT];
            return Math.max(r, MIN_RESISTANCE);
        }
        return getCurrentResistance();
    }

    /**
     * Gets forward voltage from parameter array.
     *
     * @param parameter parameter array
     * @return forward voltage
     */
    private double getUForward(double[] parameter) {
        if (parameter != null && parameter.length > PARAM_U_FORWARD) {
            return parameter[PARAM_U_FORWARD];
        }
        return uForward;
    }

    /**
     * Gets OFF resistance from parameter array.
     *
     * @param parameter parameter array
     * @return OFF resistance
     */
    private double getROff(double[] parameter) {
        if (parameter != null && parameter.length > PARAM_R_OFF) {
            return parameter[PARAM_R_OFF];
        }
        return rOff;
    }

    /**
     * Gets the ON resistance.
     *
     * @return ON state resistance in Ohms
     */
    public double getROn() {
        return rOn;
    }

    /**
     * Sets the ON resistance.
     *
     * @param rOn ON state resistance in Ohms
     */
    public void setROn(double rOn) {
        this.rOn = Math.max(rOn, MIN_RESISTANCE);
    }

    /**
     * Gets the OFF resistance.
     *
     * @return OFF state resistance in Ohms
     */
    public double getROff() {
        return rOff;
    }

    /**
     * Sets the OFF resistance.
     *
     * @param rOff OFF state resistance in Ohms
     */
    public void setROff(double rOff) {
        this.rOff = Math.min(Math.max(rOff, MIN_RESISTANCE), MAX_RESISTANCE);
    }

    /**
     * Gets the forward voltage.
     *
     * @return forward voltage in Volts
     */
    public double getUForward() {
        return uForward;
    }

    /**
     * Sets the forward voltage.
     *
     * @param uForward forward voltage in Volts
     */
    public void setUForward(double uForward) {
        this.uForward = uForward;
    }

    /**
     * Gets the reverse recovery time.
     *
     * @return recovery time in seconds
     */
    public double getRecoveryTime() {
        return recoveryTime;
    }

    /**
     * Sets the reverse recovery time.
     *
     * @param recoveryTime recovery time in seconds
     */
    public void setRecoveryTime(double recoveryTime) {
        this.recoveryTime = Math.max(0, recoveryTime);
    }

    /**
     * Gets the last switch-off time.
     *
     * @return last switch-off time
     */
    public double getLastSwitchOffTime() {
        return lastSwitchOffTime;
    }

    /**
     * Sets the last switch-off time.
     *
     * @param lastSwitchOffTime last switch-off time
     */
    public void setLastSwitchOffTime(double lastSwitchOffTime) {
        this.lastSwitchOffTime = lastSwitchOffTime;
    }

    /**
     * Creates parameter array for a thyristor.
     *
     * @param rCurrent current resistance
     * @param uForward forward voltage
     * @param rOn ON state resistance
     * @param rOff OFF state resistance
     * @param gateSignal gate control signal (0 or 1)
     * @param recoveryTime reverse recovery time
     * @param lastSwitchTime last switch-off time
     * @return parameter array for use with this stamper
     */
    public static double[] createParameters(double rCurrent, double uForward,
                                            double rOn, double rOff, double gateSignal,
                                            double recoveryTime, double lastSwitchTime) {
        double[] params = new double[PARAM_LAST_SWITCH_TIME + 1];
        params[PARAM_R_CURRENT] = rCurrent;
        params[PARAM_U_FORWARD] = uForward;
        params[PARAM_R_ON] = rOn;
        params[PARAM_R_OFF] = rOff;
        params[PARAM_GATE] = gateSignal;
        params[PARAM_RECOVERY_TIME] = recoveryTime;
        params[PARAM_LAST_SWITCH_TIME] = lastSwitchTime;
        return params;
    }

    /**
     * Creates parameter array with default values and specified gate signal.
     *
     * @param gateSignal gate control signal (0 or 1)
     * @return parameter array with default values
     */
    public static double[] createDefaultParameters(double gateSignal) {
        double rCurrent = (gateSignal != 0) ? DEFAULT_R_ON : DEFAULT_R_OFF;
        return createParameters(rCurrent, DEFAULT_U_FORWARD, DEFAULT_R_ON, DEFAULT_R_OFF,
                               gateSignal, DEFAULT_RECOVERY_TIME, Double.NEGATIVE_INFINITY);
    }

    @Override
    public String toString() {
        return "ThyristorStamper[" + (isOn ? "ON" : "OFF") +
               ", rOn=" + rOn +
               ", rOff=" + rOff +
               ", uFwd=" + uForward +
               ", tq=" + recoveryTime + "]";
    }
}
