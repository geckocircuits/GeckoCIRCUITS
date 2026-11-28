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
package gecko.core.circuit.matrix;

import gecko.core.allg.SolverType;

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
    public void stampMatrixA(MatrixCoefficients a, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt, SolverType solverType) {
        double conductance = getAdmittanceWeight(parameter[PARAM_CAPACITANCE], dt, solverType);

        // Stamp the standard two-terminal conductance pattern
        a.add(nodeX, nodeX, conductance);
        a.add(nodeY, nodeY, conductance);
        a.add(nodeX, nodeY, -conductance);
        a.add(nodeY, nodeX, -conductance);
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
        // Backward Euler (default): G = C/dt
        double safeCapacitance = Math.max(capacitance, MIN_CAPACITANCE);
        return safeCapacitance / dt;
    }

    /**
     * Calculates solver-type-aware equivalent conductance for capacitor.
     *
     * @param capacitance the capacitance value
     * @param dt time step size
     * @param solverType the numerical integration method
     * @return equivalent conductance matching the solver type
     */
    public double getAdmittanceWeight(double capacitance, double dt, SolverType solverType) {
        double safeCapacitance = Math.max(capacitance, MIN_CAPACITANCE);
        return switch (solverType) {
            case SOLVER_TRZ -> 2.0 * safeCapacitance / dt;
            case SOLVER_GS  -> 1.5 * safeCapacitance / dt;
            default         -> safeCapacitance / dt;  // BE
        };
    }
}
