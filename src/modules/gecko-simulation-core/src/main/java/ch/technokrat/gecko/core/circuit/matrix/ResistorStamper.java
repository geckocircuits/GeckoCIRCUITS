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
 * Matrix stamper implementation for resistor components.
 *
 * A resistor R between nodes x and y contributes to the A matrix:
 * - a[x][x] += 1/R  (self-admittance at node x)
 * - a[y][y] += 1/R  (self-admittance at node y)
 * - a[x][y] -= 1/R  (mutual admittance x to y)
 * - a[y][x] -= 1/R  (mutual admittance y to x)
 *
 * The resistor does not contribute to the b vector (no independent sources).
 *
 * Current through resistor: I = (Vx - Vy) / R
 *
 * GUI-free version for use in headless simulation core.
 */
public class ResistorStamper implements IMatrixStamper {

    /** Minimum resistance to avoid division by zero */
    private static final double MIN_RESISTANCE = 1e-9;

    @Override
    public void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt) {
        double admittance = getAdmittanceWeight(parameter[0], dt);

        // Stamp the standard two-terminal admittance pattern
        a[nodeX][nodeX] += admittance;
        a[nodeY][nodeY] += admittance;
        a[nodeX][nodeY] -= admittance;
        a[nodeY][nodeX] -= admittance;
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt, double time,
                             double[] previousValues) {
        // Resistor has no independent source contribution to b vector
    }

    @Override
    public double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                                   double[] parameter, double dt, double previousCurrent) {
        double resistance = Math.max(parameter[0], MIN_RESISTANCE);
        return (nodeVoltageX - nodeVoltageY) / resistance;
    }

    @Override
    public double getAdmittanceWeight(double resistance, double dt) {
        // Clamp resistance to minimum value to avoid division by zero
        double safeResistance = Math.max(resistance, MIN_RESISTANCE);
        return 1.0 / safeResistance;
    }
}
