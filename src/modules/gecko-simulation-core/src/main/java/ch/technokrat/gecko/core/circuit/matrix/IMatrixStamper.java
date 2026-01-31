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
 * Strategy interface for stamping component contributions into MNA matrices.
 *
 * In Modified Nodal Analysis (MNA), each circuit component "stamps" its
 * contribution into the system matrices. This interface defines the
 * contract for different stamping strategies.
 *
 * The MNA equation is: A * x = b
 * where:
 * - A is the admittance/conductance matrix
 * - x is the vector of unknowns (node voltages, branch currents)
 * - b is the source/excitation vector
 *
 * Each component type (resistor, capacitor, inductor, etc.) has a different
 * stamping pattern for how it contributes to A and b.
 *
 * GUI-free version for use in headless simulation core.
 */
public interface IMatrixStamper {

    /**
     * Stamps the component's contribution into the A matrix (conductance/admittance).
     *
     * For a resistor R between nodes x and y:
     * - a[x][x] += 1/R
     * - a[y][y] += 1/R
     * - a[x][y] -= 1/R
     * - a[y][x] -= 1/R
     *
     * @param a the A matrix to stamp into
     * @param nodeX first node index
     * @param nodeY second node index
     * @param nodeZ auxiliary node index (for voltage sources, inductors with LKOP2)
     * @param parameter component parameters array
     * @param dt time step size
     */
    void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ, double[] parameter, double dt);

    /**
     * Stamps the component's contribution into the b vector (sources).
     *
     * For a current source I from node x to y:
     * - b[x] -= I
     * - b[y] += I
     *
     * @param b the b vector to stamp into
     * @param nodeX first node index
     * @param nodeY second node index
     * @param nodeZ auxiliary node index
     * @param parameter component parameters array
     * @param dt time step size
     * @param time current simulation time
     * @param previousValues array of previous state values (voltages, currents)
     */
    void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ, double[] parameter,
                      double dt, double time, double[] previousValues);

    /**
     * Calculates the component current after matrix solution.
     *
     * @param nodeVoltageX voltage at node X
     * @param nodeVoltageY voltage at node Y
     * @param parameter component parameters array
     * @param dt time step size
     * @param previousCurrent previous current value
     * @return calculated component current
     */
    double calculateCurrent(double nodeVoltageX, double nodeVoltageY, double[] parameter,
                           double dt, double previousCurrent);

    /**
     * Gets the admittance weight factor for this component type.
     * This is the coefficient used in the A-matrix stamping.
     *
     * @param parameterValue primary parameter value (e.g., resistance, capacitance)
     * @param dt time step size
     * @return admittance weight factor (e.g., 1/R for resistor, C/dt for capacitor)
     */
    double getAdmittanceWeight(double parameterValue, double dt);
}
