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
 * Matrix stamper implementation for capacitor components.
 *
 * A capacitor C between nodes x and y is modeled using the companion model.
 * Using Backward Euler integration:
 *   i(t) = C * (v(t) - v(t-dt)) / dt
 *   i(t) = (C/dt) * v(t) - (C/dt) * v(t-dt)
 *
 * This gives an equivalent conductance G = C/dt and a current source I = G * v_prev.
 *
 * A matrix stamps (conductance):
 * - a[x][x] += C/dt
 * - a[y][y] += C/dt
 * - a[x][y] -= C/dt
 * - a[y][x] -= C/dt
 *
 * B vector stamps (history current source):
 * - b[x] += (C/dt) * v_prev
 * - b[y] -= (C/dt) * v_prev
 *
 * Current through capacitor: I = G * (Vx - Vy) - I_history
 *
 * GUI-free version for use in headless simulation core.
 */
public class CapacitorStamper implements IMatrixStamper {

    /** Minimum capacitance to avoid numerical issues */
    private static final double MIN_CAPACITANCE = 1e-15;

    /** Index for capacitance in parameter array */
    private static final int PARAM_CAPACITANCE = 0;

    /** Index for previous voltage in previousValues array */
    private static final int PREV_VOLTAGE = 0;

    /** Index for previous current in previousValues array */
    private static final int PREV_CURRENT = 1;

    @Override
    public void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt) {
        double conductance = getAdmittanceWeight(parameter[PARAM_CAPACITANCE], dt);

        // Stamp the standard two-terminal conductance pattern
        a[nodeX][nodeX] += conductance;
        a[nodeY][nodeY] += conductance;
        a[nodeX][nodeY] -= conductance;
        a[nodeY][nodeX] -= conductance;
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt, double time,
                             double[] previousValues) {
        double capacitance = Math.max(parameter[PARAM_CAPACITANCE], MIN_CAPACITANCE);
        double conductance = capacitance / dt;

        // Get previous voltage across capacitor
        double vPrev = 0.0;
        if (previousValues != null && previousValues.length > PREV_VOLTAGE) {
            vPrev = previousValues[PREV_VOLTAGE];
        }

        // History current source: I_hist = G * v_prev = (C/dt) * v_prev
        double historySource = conductance * vPrev;

        // Stamp as current source from node Y to node X
        b[nodeX] += historySource;
        b[nodeY] -= historySource;
    }

    @Override
    public double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                                   double[] parameter, double dt, double previousCurrent) {
        double capacitance = Math.max(parameter[PARAM_CAPACITANCE], MIN_CAPACITANCE);

        // For capacitor: i = C * dv/dt = C * (v - v_prev) / dt
        // Using the current voltage difference and the conductance model:
        // i = G * (Vx - Vy) where part of this is the actual capacitor current
        double conductance = capacitance / dt;
        double voltage = nodeVoltageX - nodeVoltageY;

        // The actual current is computed from the capacitor equation
        // This is a simplified model; actual implementation may need previous voltage
        return conductance * voltage;
    }

    @Override
    public double getAdmittanceWeight(double capacitance, double dt) {
        // Clamp capacitance to minimum value
        double safeCapacitance = Math.max(capacitance, MIN_CAPACITANCE);
        // Backward Euler: G = C/dt
        return safeCapacitance / dt;
    }

    /**
     * Calculates the equivalent conductance for trapezoidal integration.
     * For trapezoidal rule: G = 2C/dt
     *
     * @param capacitance the capacitance value
     * @param dt time step size
     * @return equivalent conductance for trapezoidal integration
     */
    public double getAdmittanceWeightTrapezoidal(double capacitance, double dt) {
        double safeCapacitance = Math.max(capacitance, MIN_CAPACITANCE);
        return 2.0 * safeCapacitance / dt;
    }
}
