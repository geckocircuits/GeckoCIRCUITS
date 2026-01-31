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
 * Matrix stamper implementation for MOSFET components.
 *
 * The MOSFET is modeled as a gate-controlled variable resistor with two states:
 * - ON state: Low resistance (rOn) - conducting bidirectionally
 * - OFF state: High resistance (rOff) - blocking
 *
 * Key characteristics:
 * - Gate-controlled turn-on AND turn-off (like ideal switch)
 * - Bidirectional current flow when ON (unlike IGBTs)
 * - No forward voltage drop (pure resistor model)
 * - Inherent body diode can conduct in reverse when OFF (if modeled)
 *
 * This stamper is essentially the same as IdealSwitchStamper since the
 * GeckoCIRCUITS LKMatrices code treats LK_S and LK_MOSFET identically.
 *
 * A matrix stamps (same pattern as resistor, but with state-dependent R):
 * - a[x][x] += G (where G = 1/R, R is rOn or rOff)
 * - a[y][y] += G
 * - a[x][y] -= G
 * - a[y][x] -= G
 *
 * B vector: No contribution (no forward voltage drop)
 *
 * @author GeckoCIRCUITS Team
 */
public class MOSFETStamper implements IStatefulStamper {

    /** Parameter index for current resistance (rD) */
    public static final int PARAM_R_CURRENT = 0;

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

    /**
     * Creates a MOSFET stamper with default parameters.
     */
    public MOSFETStamper() {
        this(DEFAULT_R_ON, DEFAULT_R_OFF);
    }

    /**
     * Creates a MOSFET stamper with specified parameters.
     *
     * @param rOn ON state resistance in Ohms
     * @param rOff OFF state resistance in Ohms
     */
    public MOSFETStamper(double rOn, double rOff) {
        this.rOn = Math.max(rOn, MIN_RESISTANCE);
        this.rOff = Math.min(Math.max(rOff, MIN_RESISTANCE), MAX_RESISTANCE);
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
        // MOSFET has no forward voltage drop - no B vector contribution
    }

    @Override
    public double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                                   double[] parameter, double dt, double previousCurrent) {
        double resistance = getCurrentResistance(parameter);
        double vDiff = nodeVoltageX - nodeVoltageY;
        // Simple Ohm's law: I = V / R
        return vDiff / resistance;
    }

    @Override
    public double getAdmittanceWeight(double parameterValue, double dt) {
        double resistance = isOn ? rOn : rOff;
        resistance = Math.max(resistance, MIN_RESISTANCE);
        return 1.0 / resistance;
    }

    @Override
    public void updateState(double vx, double vy, double current, double time) {
        // MOSFET state is controlled externally via gate signal
        // State update happens via setGateSignal() method
        // This method is kept for interface compliance but doesn't change state
        // based on voltage/current (unlike diodes or thyristors)
    }

    /**
     * Updates the MOSFET state based on gate signal.
     * This is the primary way to control the MOSFET.
     *
     * @param gateSignal gate signal value (0 = OFF, non-zero = ON)
     */
    public void setGateSignal(double gateSignal) {
        boolean previousState = isOn;
        isOn = (gateSignal != 0);
        stateChanged = (isOn != previousState);
    }

    /**
     * Updates state based on parameter array gate signal.
     *
     * @param parameter parameter array containing gate signal at PARAM_GATE
     */
    public void updateStateFromParameters(double[] parameter) {
        if (parameter != null && parameter.length > PARAM_GATE) {
            setGateSignal(parameter[PARAM_GATE]);
        }
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
        if (parameter != null && parameter.length > PARAM_R_CURRENT) {
            double r = parameter[PARAM_R_CURRENT];
            return Math.max(r, MIN_RESISTANCE);
        }
        return getCurrentResistance();
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
     * Creates parameter array for a MOSFET.
     *
     * @param rCurrent current resistance (rOn or rOff depending on state)
     * @param rOn ON state resistance
     * @param rOff OFF state resistance
     * @param gateSignal gate control signal (0 = OFF, 1 = ON)
     * @return parameter array for use with this stamper
     */
    public static double[] createParameters(double rCurrent, double rOn, double rOff, double gateSignal) {
        double[] params = new double[PARAM_GATE + 1];
        params[PARAM_R_CURRENT] = rCurrent;
        params[PARAM_R_ON] = rOn;
        params[PARAM_R_OFF] = rOff;
        params[PARAM_GATE] = gateSignal;
        return params;
    }

    /**
     * Creates parameter array with default values and specified gate signal.
     *
     * @param gateSignal gate control signal (0 = OFF, 1 = ON)
     * @return parameter array with default resistances
     */
    public static double[] createDefaultParameters(double gateSignal) {
        double rCurrent = (gateSignal != 0) ? DEFAULT_R_ON : DEFAULT_R_OFF;
        return createParameters(rCurrent, DEFAULT_R_ON, DEFAULT_R_OFF, gateSignal);
    }

    @Override
    public String toString() {
        return "MOSFETStamper[" + (isOn ? "ON" : "OFF") +
               ", rOn=" + rOn +
               ", rOff=" + rOff + "]";
    }
}
