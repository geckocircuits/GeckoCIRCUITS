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
 * Matrix stamper implementation for voltage source components.
 *
 * A voltage source V between nodes x and y requires an additional row/column
 * in the MNA matrix to track the branch current through the source.
 * The nodeZ parameter is used as the index for this additional equation.
 *
 * The voltage source enforces: Vx - Vy = V_source
 *
 * A matrix stamps:
 * - a[nodeZ][nodeX] = +1  (voltage equation coefficient for Vx)
 * - a[nodeZ][nodeY] = -1  (voltage equation coefficient for Vy)
 * - a[nodeX][nodeZ] = +1  (KCL at node X includes source current)
 * - a[nodeY][nodeZ] = -1  (KCL at node Y includes source current)
 *
 * B vector stamps:
 * - b[nodeZ] = V_source  (the specified voltage)
 *
 * The unknown x[nodeZ] will be the current through the voltage source.
 *
 * GUI-free version for use in headless simulation core.
 */
public class VoltageSourceStamper implements IMatrixStamper {

    /** Index for source type in parameter array (e.g., DC, AC, signal-controlled) */
    private static final int PARAM_SOURCE_TYPE = 0;

    /** Index for voltage amplitude in parameter array */
    private static final int PARAM_VOLTAGE = 1;

    /** Index for frequency in parameter array (for AC sources) */
    private static final int PARAM_FREQUENCY = 2;

    /** Index for phase in parameter array (for AC sources) */
    private static final int PARAM_PHASE = 3;

    /** Source type constant for DC */
    public static final int SOURCE_DC = 0;

    /** Source type constant for AC sinusoidal */
    public static final int SOURCE_AC = 1;

    @Override
    public void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt) {
        // nodeZ is the index for the additional current variable

        // Voltage equation row: Vx - Vy = Vsource
        a[nodeZ][nodeX] += 1.0;
        a[nodeZ][nodeY] -= 1.0;

        // KCL equations: include the source current
        // Current flows from nodeX to nodeY (positive current out of + terminal)
        a[nodeX][nodeZ] += 1.0;
        a[nodeY][nodeZ] -= 1.0;
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt, double time,
                             double[] previousValues) {
        double voltage = calculateSourceVoltage(parameter, time);

        // The voltage equation: Vx - Vy = Vsource
        b[nodeZ] += voltage;
    }

    @Override
    public double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                                   double[] parameter, double dt, double previousCurrent) {
        // For voltage source, current is determined by the circuit (not by Ohm's law)
        // The actual current comes from solving the MNA equation (x[nodeZ])
        // This method returns previousCurrent as a placeholder; actual current
        // should be read from the solution vector at nodeZ
        return previousCurrent;
    }

    @Override
    public double getAdmittanceWeight(double parameterValue, double dt) {
        // Voltage sources don't have a simple admittance weight
        // They use the extended MNA formulation instead
        return 0.0;
    }

    /**
     * Calculates the source voltage at a given time based on source type.
     *
     * @param parameter the component parameters
     * @param time current simulation time
     * @return the voltage value at the given time
     */
    public double calculateSourceVoltage(double[] parameter, double time) {
        if (parameter == null || parameter.length < 2) {
            return 0.0;
        }

        int sourceType = (int) parameter[PARAM_SOURCE_TYPE];
        double amplitude = parameter[PARAM_VOLTAGE];

        switch (sourceType) {
            case SOURCE_DC:
                return amplitude;

            case SOURCE_AC:
                if (parameter.length >= 4) {
                    double frequency = parameter[PARAM_FREQUENCY];
                    double phase = parameter[PARAM_PHASE];
                    return amplitude * Math.sin(2.0 * Math.PI * frequency * time + phase);
                }
                return amplitude * Math.sin(2.0 * Math.PI * time); // Default 1 Hz

            default:
                // Signal-controlled or other types: use amplitude directly
                return amplitude;
        }
    }

    /**
     * Stamps the A matrix for a grounded voltage source (one terminal at ground).
     * This is a simplified version when nodeY = 0 (ground).
     *
     * @param a the A matrix to stamp into
     * @param nodeX the non-ground node
     * @param nodeZ the current variable index
     */
    public void stampMatrixAGrounded(double[][] a, int nodeX, int nodeZ) {
        // Voltage equation: Vx - 0 = Vsource -> Vx = Vsource
        a[nodeZ][nodeX] += 1.0;

        // KCL at nodeX: include source current
        a[nodeX][nodeZ] += 1.0;
    }

    /**
     * Gets the required size increase for the MNA matrix.
     * Voltage sources add one row and one column per source.
     *
     * @return the number of additional unknowns (1 for current variable)
     */
    public int getAdditionalMatrixSize() {
        return 1;
    }

    /**
     * Creates parameter array for a DC voltage source.
     *
     * @param voltage DC voltage value in Volts
     * @return parameter array for use with this stamper
     */
    public static double[] createDCParameters(double voltage) {
        return new double[]{SOURCE_DC, voltage, 0.0, 0.0};
    }

    /**
     * Creates parameter array for an AC voltage source.
     *
     * @param amplitude peak voltage in Volts
     * @param frequency frequency in Hz
     * @param phase phase angle in radians
     * @return parameter array for use with this stamper
     */
    public static double[] createACParameters(double amplitude, double frequency, double phase) {
        return new double[]{SOURCE_AC, amplitude, frequency, phase};
    }
}
