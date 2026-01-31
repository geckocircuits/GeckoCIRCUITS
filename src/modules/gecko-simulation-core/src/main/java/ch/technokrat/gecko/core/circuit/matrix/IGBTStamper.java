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
 * Matrix stamper implementation for IGBT (Insulated Gate Bipolar Transistor) components.
 *
 * The IGBT is modeled as a gate-controlled switch with series diode characteristics:
 * - ON state: Low resistance (rOn) with forward voltage drop, only conducts forward current
 * - OFF state: High resistance (rOff) - blocking
 *
 * Key characteristics (from LKMatrices.java):
 * - Gate-controlled turn-on: Requires gate=1 AND forward voltage > threshold
 * - Gate-controlled turn-off: Immediately turns OFF when gate=0
 * - Forward voltage drop (uf): Acts like a diode in series with switch
 * - Unidirectional: Only conducts forward current when ON
 * - Turns OFF automatically if current tries to reverse while ON with gate=1
 *
 * Parameter mapping (from LKMatrices):
 * - parameter[0] = current resistance (rD)
 * - parameter[1] = forward voltage (uf)
 * - parameter[2] = ON resistance (rON)
 * - parameter[3] = OFF resistance (rOFF)
 * - parameter[4] = current (i)
 * - parameter[5] = voltage (u)
 * - parameter[8] = gate signal (0 or 1)
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
public class IGBTStamper implements IStatefulStamper {

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

    /** Default ON resistance (very low) */
    public static final double DEFAULT_R_ON = 1e-3;

    /** Default OFF resistance (very high) */
    public static final double DEFAULT_R_OFF = 1e9;

    /** Default forward voltage threshold */
    public static final double DEFAULT_U_FORWARD = 1.5;

    /** Resistance threshold to determine if IGBT is ON (from LKMatrices: rD < 10000) */
    public static final double ON_STATE_THRESHOLD = 10000.0;

    /** Minimum resistance to avoid numerical issues */
    private static final double MIN_RESISTANCE = 1e-12;

    /** Maximum resistance to avoid numerical issues */
    private static final double MAX_RESISTANCE = 1e15;

    /** Perturbation factor for switching threshold (from LKMatrices: stoergroesse) */
    private static final double PERTURBATION_FACTOR = 0.99;

    /** Acceptance threshold for voltage comparison */
    private static final double ACCEPTANCE_THRESHOLD = 1e-6;

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

    /**
     * Creates an IGBT stamper with default parameters.
     */
    public IGBTStamper() {
        this(DEFAULT_R_ON, DEFAULT_R_OFF, DEFAULT_U_FORWARD);
    }

    /**
     * Creates an IGBT stamper with specified parameters.
     *
     * @param rOn ON state resistance in Ohms
     * @param rOff OFF state resistance in Ohms
     * @param uForward forward voltage drop in Volts
     */
    public IGBTStamper(double rOn, double rOff, double uForward) {
        this.rOn = Math.max(rOn, MIN_RESISTANCE);
        this.rOff = Math.min(Math.max(rOff, MIN_RESISTANCE), MAX_RESISTANCE);
        this.uForward = uForward;
        this.isOn = false;
        this.stateChanged = false;
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
        if (resistance < ON_STATE_THRESHOLD) {
            double uf = getUForward(parameter);
            if (uf > 0) {
                double admittance = 1.0 / resistance;
                double compensation = admittance * uf;

                // Forward voltage compensation (like in diode)
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
            // Current through conducting IGBT: I = (V - Uf) / rOn
            return (vDiff - uf) / resistance;
        } else {
            // Leakage current through blocking IGBT
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
        // IGBT state logic is more complex - primarily controlled by gate
        // but also has voltage/current conditions
        // This method can be called for automatic switching detection
        // but main control is via setGateSignal()

        boolean previousState = isOn;
        double vForward = vx - vy;

        if (isOn) {
            // Currently ON: turn OFF if forward voltage drops below threshold
            // This happens when current tries to reverse
            if (vForward < (PERTURBATION_FACTOR * uForward + ACCEPTANCE_THRESHOLD)) {
                isOn = false;
            }
        }
        // Note: Turn ON is handled by updateStateWithGate() since it requires gate signal

        stateChanged = (isOn != previousState);
    }

    /**
     * Updates the IGBT state based on gate signal and circuit conditions.
     * This is the primary method for controlling the IGBT.
     *
     * @param gateSignal gate signal value (0 = OFF, 1 = ON)
     * @param vx voltage at node X
     * @param vy voltage at node Y
     */
    public void updateStateWithGate(double gateSignal, double vx, double vy) {
        boolean previousState = isOn;
        double vForward = vx - vy;
        boolean gateOn = (gateSignal == 1);

        if (isOn) {
            // Currently ON
            if (!gateOn) {
                // Gate turned OFF -> IGBT turns OFF immediately
                isOn = false;
            } else if (vForward < (PERTURBATION_FACTOR * uForward + ACCEPTANCE_THRESHOLD)) {
                // Forward voltage dropped below threshold (current trying to reverse)
                isOn = false;
            }
        } else {
            // Currently OFF
            if (gateOn && vForward > (PERTURBATION_FACTOR * uForward - ACCEPTANCE_THRESHOLD)) {
                // Gate ON and forward voltage exceeds threshold
                isOn = true;
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
     */
    public void updateStateFromParameters(double[] parameter, double vx, double vy) {
        if (parameter != null && parameter.length > PARAM_GATE) {
            updateStateWithGate(parameter[PARAM_GATE], vx, vy);
        }
    }

    /**
     * Sets the gate signal. Note: IGBT requires both gate AND voltage condition.
     *
     * @param gateSignal gate signal (0 or 1)
     */
    public void setGateSignal(double gateSignal) {
        boolean previousState = isOn;

        if (gateSignal == 0) {
            // Gate OFF -> IGBT turns OFF unconditionally
            isOn = false;
        }
        // Gate ON alone doesn't turn on IGBT - needs voltage condition too
        // Use updateStateWithGate() for full control

        stateChanged = (isOn != previousState);
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
     * Creates parameter array for an IGBT.
     *
     * @param rCurrent current resistance
     * @param uForward forward voltage
     * @param rOn ON state resistance
     * @param rOff OFF state resistance
     * @param gateSignal gate control signal (0 = OFF, 1 = ON)
     * @return parameter array for use with this stamper
     */
    public static double[] createParameters(double rCurrent, double uForward,
                                            double rOn, double rOff, double gateSignal) {
        double[] params = new double[PARAM_GATE + 1];
        params[PARAM_R_CURRENT] = rCurrent;
        params[PARAM_U_FORWARD] = uForward;
        params[PARAM_R_ON] = rOn;
        params[PARAM_R_OFF] = rOff;
        params[PARAM_GATE] = gateSignal;
        return params;
    }

    /**
     * Creates parameter array with default values and specified gate signal.
     *
     * @param gateSignal gate control signal (0 = OFF, 1 = ON)
     * @return parameter array with default values
     */
    public static double[] createDefaultParameters(double gateSignal) {
        double rCurrent = (gateSignal != 0) ? DEFAULT_R_ON : DEFAULT_R_OFF;
        return createParameters(rCurrent, DEFAULT_U_FORWARD, DEFAULT_R_ON, DEFAULT_R_OFF, gateSignal);
    }

    @Override
    public String toString() {
        return "IGBTStamper[" + (isOn ? "ON" : "OFF") +
               ", rOn=" + rOn +
               ", rOff=" + rOff +
               ", uFwd=" + uForward + "]";
    }
}
