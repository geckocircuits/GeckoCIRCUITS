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
package ch.technokrat.gecko.geckocircuits.circuit.matrix;

/**
 * Matrix stamper implementation for current source components.
 *
 * An ideal current source between nodes x and y contributes only to the b vector
 * (no A matrix contribution since it has infinite impedance).
 *
 * Current convention: Current flows FROM nodeY TO nodeX (enters at nodeX).
 *
 * B vector stamps:
 * - b[nodeX] += I  (current entering node X)
 * - b[nodeY] -= I  (current leaving node Y)
 *
 * Supports DC and AC (sinusoidal) source types.
 *
 * @author GeckoCIRCUITS Team
 */
public class CurrentSourceStamper implements IMatrixStamper {

    /** Index for source type in parameter array (e.g., DC, AC, signal-controlled) */
    private static final int PARAM_SOURCE_TYPE = 0;

    /** Index for current amplitude in parameter array */
    private static final int PARAM_CURRENT = 1;

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
        // Ideal current source has infinite impedance - no A matrix contribution
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt, double time,
                             double[] previousValues) {
        double current = calculateSourceCurrent(parameter, time);

        // Current enters at nodeX, leaves at nodeY
        b[nodeX] += current;
        b[nodeY] -= current;
    }

    @Override
    public double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                                   double[] parameter, double dt, double previousCurrent) {
        // For an ideal current source, the current is specified (not dependent on voltage)
        // Return the source current value
        return calculateSourceCurrent(parameter, 0.0);
    }

    @Override
    public double getAdmittanceWeight(double parameterValue, double dt) {
        // Ideal current source has zero admittance (infinite impedance)
        return 0.0;
    }

    /**
     * Calculates the source current at a given time based on source type.
     *
     * @param parameter the component parameters
     * @param time current simulation time
     * @return the current value at the given time
     */
    public double calculateSourceCurrent(double[] parameter, double time) {
        if (parameter == null || parameter.length < 2) {
            return 0.0;
        }

        int sourceType = (int) parameter[PARAM_SOURCE_TYPE];
        double amplitude = parameter[PARAM_CURRENT];

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
     * Stamps the b vector for a grounded current source (one terminal at ground).
     * This is a simplified version when nodeY = 0 (ground).
     *
     * @param b the b vector to stamp into
     * @param nodeX the non-ground node
     * @param parameter component parameters array
     * @param time current simulation time
     */
    public void stampVectorBGrounded(double[] b, int nodeX, double[] parameter, double time) {
        double current = calculateSourceCurrent(parameter, time);
        // Current enters at nodeX from ground
        b[nodeX] += current;
    }

    /**
     * Creates parameter array for a DC current source.
     *
     * @param current DC current value in Amperes
     * @return parameter array for use with this stamper
     */
    public static double[] createDCParameters(double current) {
        return new double[]{SOURCE_DC, current, 0.0, 0.0};
    }

    /**
     * Creates parameter array for an AC current source.
     *
     * @param amplitude peak current in Amperes
     * @param frequency frequency in Hz
     * @param phase phase angle in radians
     * @return parameter array for use with this stamper
     */
    public static double[] createACParameters(double amplitude, double frequency, double phase) {
        return new double[]{SOURCE_AC, amplitude, frequency, phase};
    }
}
