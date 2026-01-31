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
 * Matrix stamper implementation for diode components.
 *
 * The diode is modeled as a variable resistor with two states:
 * - ON state: Low resistance (rOn), with optional forward voltage drop
 * - OFF state: High resistance (rOff)
 *
 * The diode conducts (turns ON) when:
 * - Forward voltage (Vx - Vy) >= forward threshold voltage (uForward), AND
 * - Current through diode > 0 (or attempting to flow forward)
 *
 * The diode blocks (turns OFF) when:
 * - Current tries to flow reverse (i < 0), OR
 * - Forward voltage < threshold
 *
 * This stamper implements IStatefulStamper for iterative state convergence.
 *
 * A matrix stamps (same pattern as resistor, but with state-dependent R):
 * - a[x][x] += G (where G = 1/R, R is rOn or rOff)
 * - a[y][y] += G
 * - a[x][y] -= G
 * - a[y][x] -= G
 *
 * B vector stamps (for forward voltage compensation when ON):
 * - b[x] += G * uForward
 * - b[y] -= G * uForward
 *
 * @author GeckoCIRCUITS Team
 */
public class DiodeStamper implements IStatefulStamper {

    /** Parameter index for ON resistance */
    public static final int PARAM_R_ON = 0;

    /** Parameter index for OFF resistance */
    public static final int PARAM_R_OFF = 1;

    /** Parameter index for forward voltage threshold */
    public static final int PARAM_U_FORWARD = 2;

    /** Default ON resistance (very low) */
    public static final double DEFAULT_R_ON = 1e-3;

    /** Default OFF resistance (very high) */
    public static final double DEFAULT_R_OFF = 1e9;

    /** Default forward voltage threshold */
    public static final double DEFAULT_U_FORWARD = 0.7;

    /** Minimum resistance to avoid numerical issues */
    private static final double MIN_RESISTANCE = 1e-12;

    /** Maximum resistance to avoid numerical issues */
    private static final double MAX_RESISTANCE = 1e15;

    /** Current ON/OFF state */
    private boolean isOn;

    /** Flag indicating state changed in last updateState call */
    private boolean stateChanged;

    /** ON resistance */
    private double rOn;

    /** OFF resistance */
    private double rOff;

    /** Forward voltage threshold */
    private double uForward;

    /**
     * Creates a diode stamper with default parameters.
     */
    public DiodeStamper() {
        this(DEFAULT_R_ON, DEFAULT_R_OFF, DEFAULT_U_FORWARD);
    }

    /**
     * Creates a diode stamper with specified parameters.
     *
     * @param rOn ON state resistance in Ohms
     * @param rOff OFF state resistance in Ohms
     * @param uForward forward voltage threshold in Volts
     */
    public DiodeStamper(double rOn, double rOff, double uForward) {
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
        // When ON, compensate for forward voltage drop
        if (isOn) {
            double uf = getUForward(parameter);
            if (uf > 0) {
                double resistance = getCurrentResistance(parameter);
                double admittance = 1.0 / resistance;
                double compensation = admittance * uf;

                // Forward voltage makes it easier for current to flow
                // Equivalent to a voltage source of uForward in series
                b[nodeX] += compensation;
                b[nodeY] -= compensation;
            }
        }
    }

    @Override
    public double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                                   double[] parameter, double dt, double previousCurrent) {
        double resistance = getCurrentResistance(parameter);
        double vDiff = nodeVoltageX - nodeVoltageY;

        if (isOn) {
            double uf = getUForward(parameter);
            // Current through conducting diode: I = (V - Uf) / rOn
            return (vDiff - uf) / resistance;
        } else {
            // Leakage current through blocking diode
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
        boolean previousState = isOn;
        double vForward = vx - vy;

        // Determine new state based on voltage and current
        if (isOn) {
            // Currently ON: turn OFF if current goes negative (reverse)
            if (current < 0) {
                isOn = false;
            }
        } else {
            // Currently OFF: turn ON if forward voltage exceeds threshold
            if (vForward >= uForward) {
                isOn = true;
            }
        }

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
     * @param parameter optional parameter array (may override instance values)
     * @return current effective resistance
     */
    private double getCurrentResistance(double[] parameter) {
        if (parameter != null && parameter.length > PARAM_R_OFF) {
            double r = isOn ? parameter[PARAM_R_ON] : parameter[PARAM_R_OFF];
            return Math.max(r, MIN_RESISTANCE);
        }
        return getCurrentResistance();
    }

    /**
     * Gets forward voltage threshold from parameter array.
     *
     * @param parameter parameter array
     * @return forward voltage threshold
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
     * Gets the forward voltage threshold.
     *
     * @return forward voltage threshold in Volts
     */
    public double getUForward() {
        return uForward;
    }

    /**
     * Sets the forward voltage threshold.
     *
     * @param uForward forward voltage threshold in Volts
     */
    public void setUForward(double uForward) {
        this.uForward = uForward;
    }

    /**
     * Creates parameter array for a diode.
     *
     * @param rOn ON state resistance
     * @param rOff OFF state resistance
     * @param uForward forward voltage threshold
     * @return parameter array for use with this stamper
     */
    public static double[] createParameters(double rOn, double rOff, double uForward) {
        return new double[]{rOn, rOff, uForward};
    }

    /**
     * Creates parameter array for a diode with default values.
     *
     * @return parameter array with default values
     */
    public static double[] createDefaultParameters() {
        return createParameters(DEFAULT_R_ON, DEFAULT_R_OFF, DEFAULT_U_FORWARD);
    }

    @Override
    public String toString() {
        return "DiodeStamper[" + (isOn ? "ON" : "OFF") +
               ", rOn=" + rOn +
               ", rOff=" + rOff +
               ", uFwd=" + uForward + "]";
    }
}
