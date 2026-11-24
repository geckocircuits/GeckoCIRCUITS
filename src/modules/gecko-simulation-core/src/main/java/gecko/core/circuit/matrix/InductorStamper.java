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
 * Matrix stamper implementation for inductor components.
 *
 * An inductor L between nodes x and y is modeled using the companion model.
 * Using Backward Euler integration:
 *   v(t) = L * (i(t) - i(t-dt)) / dt
 *   i(t) = i(t-dt) + (dt/L) * v(t)
 *   i(t) = (dt/L) * v(t) + i_prev
 *
 * This gives an equivalent conductance G = dt/L and a current source I = i_prev.
 *
 * A matrix stamps (conductance):
 * - a[x][x] += dt/L
 * - a[y][y] += dt/L
 * - a[x][y] -= dt/L
 * - a[y][x] -= dt/L
 *
 * B vector stamps (history current source):
 * - b[x] += i_prev
 * - b[y] -= i_prev
 *
 * Current through inductor: I = G * (Vx - Vy) + I_prev
 *
 * GUI-free version for use in headless simulation core.
 */
public class InductorStamper implements IMatrixStamper {

    /** Minimum inductance to avoid numerical issues */
    private static final double MIN_INDUCTANCE = 1e-15;

    /** Index for inductance in parameter array */
    private static final int PARAM_INDUCTANCE = 0;

    /** Index for previous current in previousValues array */
    private static final int PREV_CURRENT = 0;

    /** Index for previous voltage in previousValues array */
    private static final int PREV_VOLTAGE = 1;

    @Override
    public void stampMatrixA(MatrixCoefficients a, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt, SolverType solverType) {
        double conductance = getAdmittanceWeight(parameter[PARAM_INDUCTANCE], dt, solverType);

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
        // Get previous current through inductor
        double iPrev = 0.0;
        if (previousValues != null && previousValues.length > PREV_CURRENT) {
            iPrev = previousValues[PREV_CURRENT];
        }

        // History current source: the previous current continues to flow
        // Stamp as current source from node Y to node X
        b[nodeX] += iPrev;
        b[nodeY] -= iPrev;
    }

    @Override
    public double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                                   double[] parameter, double dt, double previousCurrent) {
        double inductance = Math.max(parameter[PARAM_INDUCTANCE], MIN_INDUCTANCE);
        double conductance = dt / inductance;
        double voltage = nodeVoltageX - nodeVoltageY;

        // i(t) = i_prev + (dt/L) * v(t) = i_prev + G * v(t)
        return previousCurrent + conductance * voltage;
    }

    @Override
    public double getAdmittanceWeight(double inductance, double dt) {
        // Backward Euler (default): G = dt/L
        double safeInductance = Math.max(inductance, MIN_INDUCTANCE);
        return dt / safeInductance;
    }

    /**
     * Calculates solver-type-aware equivalent conductance for inductor.
     *
     * @param inductance the inductance value
     * @param dt time step size
     * @param solverType the numerical integration method
     * @return equivalent conductance matching the solver type
     */
    public double getAdmittanceWeight(double inductance, double dt, SolverType solverType) {
        double safeInductance = Math.max(inductance, MIN_INDUCTANCE);
        return switch (solverType) {
            case SOLVER_TRZ -> dt / (2.0 * safeInductance);
            case SOLVER_GS  -> (2.0 / 3.0) * dt / safeInductance;
            default         -> dt / safeInductance;  // BE
        };
    }
}
