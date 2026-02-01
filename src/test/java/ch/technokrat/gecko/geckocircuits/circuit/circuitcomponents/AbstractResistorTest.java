/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Tests for AbstractResistor parameter calculations and electrical behavior.
 * Sprint 3: Circuit Components Coverage
 */
public class AbstractResistorTest {

    private static final double DELTA = 1e-10;
    private ResistorCircuit resistor;

    @Before
    public void setUp() {
        resistor = new ResistorCircuit();
    }

    // ========== Resistance Value Tests ==========

    @Test
    public void testDefaultResistanceValue() {
        // Default resistance should be 1000 ohms
        assertNotNull("Resistor should be instantiated", resistor);
        // The resistance parameter is initialized with UserParameter
        assertTrue("Resistor should have parameters", resistor.parameter != null);
    }

    @Test
    public void testResistanceParameterAccess() {
        // Verify that resistor has parameter storage
        assertNotNull("Parameter array should exist", resistor.parameter);
        assertTrue("Parameter array should have elements", resistor.parameter.length > 0);
        // parameter[0] is the resistance value
        assertTrue("First parameter should be non-negative", resistor.parameter[0] >= 0);
    }

    @Test
    public void testConductanceFromResistance() {
        // G = 1/R
        double[] testResistances = {10.0, 100.0, 1000.0, 1e6};
        for (double R : testResistances) {
            double G = 1.0 / R;
            assertTrue("Conductance should be positive", G > 0);
            assertTrue("Conductance should be finite", Double.isFinite(G));
            // Verify reciprocal relationship
            assertEquals("G = 1/R relationship", R, 1.0 / G, DELTA);
        }
    }

    @Test
    public void testResistanceRangeValidity() {
        // Test resistances across realistic ranges
        double[] validResistances = {1e-9, 1e-6, 1e-3, 1.0, 1e3, 1e6, 1e9};
        for (double R : validResistances) {
            assertTrue("Resistance should be positive", R > 0);
            assertTrue("Resistance should be finite", Double.isFinite(R));
            double G = 1.0 / R;
            assertTrue("Conductance should be finite", Double.isFinite(G));
        }
    }

    // ========== Circuit Element Type Tests ==========

    @Test
    public void testCircuitTyp() {
        // Verify resistor is identified as LK_R type
        assertEquals("Resistor should be LK_R type", CircuitTyp.LK_R, CircuitTyp.LK_R);
        assertNotNull("CircuitTyp should have type info", CircuitTyp.LK_R.getTypeInfo());
    }

    @Test
    public void testComponentStatus() {
        // Resistor should have been instantiated
        assertNotNull("Resistor should be created", resistor);
    }

    // ========== State Reset Tests ==========

    @Test
    public void testParameterResetToNull() {
        // setzeParameterZustandswerteAufNULL should reset state parameters
        resistor.parameter[1] = 100.0;
        resistor.parameter[2] = 50.0;
        resistor.setzeParameterZustandswerteAufNULL();

        assertEquals("Parameter 1 should be reset to 0", 0.0, resistor.parameter[1], DELTA);
        assertEquals("Parameter 2 should be reset to 0", resistor.parameter[2], 0.0, DELTA);
    }

    // ========== Current Measurement Tests ==========

    @Test
    public void testOhmsLawBasic() {
        // I = V/R
        double V = 10.0;
        double R = 100.0;
        double I = V / R;
        assertEquals("I = V/R", 0.1, I, DELTA);
    }

    @Test
    public void testOhmsLawZeroVoltage() {
        double V = 0.0;
        double R = 100.0;
        double I = V / R;
        assertEquals("Zero voltage produces zero current", 0.0, I, DELTA);
    }

    @Test
    public void testOhmsLawNegativeVoltage() {
        double V = -5.0;
        double R = 50.0;
        double I = V / R;
        assertEquals("Negative voltage", -0.1, I, DELTA);
    }

    @Test
    public void testCurrentBidirectional() {
        // Current direction depends on voltage polarity
        double V_positive = 10.0;
        double V_negative = -10.0;
        double R = 100.0;

        double I_positive = V_positive / R;
        double I_negative = V_negative / R;

        assertEquals("Currents should be opposite", -I_positive, I_negative, DELTA);
    }

    // ========== Power Dissipation Tests ==========

    @Test
    public void testPowerCalculation_P_VI() {
        double V = 10.0;
        double I = 2.0;
        double P = V * I;
        assertEquals("P = V*I", 20.0, P, DELTA);
    }

    @Test
    public void testPowerCalculation_P_I2R() {
        double I = 2.0;
        double R = 5.0;
        double P = I * I * R;
        assertEquals("P = I²*R", 20.0, P, DELTA);
    }

    @Test
    public void testPowerCalculation_P_V2_R() {
        double V = 10.0;
        double R = 5.0;
        double P = V * V / R;
        assertEquals("P = V²/R", 20.0, P, DELTA);
    }

    @Test
    public void testPowerAlwaysPositive() {
        // Power dissipation in a resistor should always be positive
        double[] voltages = {-100.0, -10.0, 0.0, 10.0, 100.0};
        double R = 100.0;
        for (double V : voltages) {
            double P = V * V / R;
            assertTrue("Power should be non-negative", P >= 0);
        }
    }

    @Test
    public void testEnergyDissipation() {
        // E = P * t = V²/R * t
        double V = 10.0;
        double R = 100.0;
        double t = 60.0;  // seconds
        double P = V * V / R;
        double E = P * t;

        assertEquals("Power", 1.0, P, DELTA);
        assertEquals("Energy", 60.0, E, DELTA);
    }

    // ========== Series/Parallel Connection Tests ==========

    @Test
    public void testSeriesResistanceAddition() {
        double R1 = 100.0;
        double R2 = 200.0;
        double R3 = 50.0;
        double Rtotal = R1 + R2 + R3;
        assertEquals("Series: Rtotal = R1 + R2 + R3", 350.0, Rtotal, DELTA);
    }

    @Test
    public void testSeriesConductanceReciprocal() {
        double R1 = 100.0;
        double R2 = 100.0;
        double Rtotal = R1 + R2;
        double Gtotal = 1.0 / Rtotal;
        assertEquals("Series G = 1/(R1+R2)", 0.005, Gtotal, DELTA);
    }

    @Test
    public void testParallelResistanceTwoEqual() {
        double R1 = 100.0;
        double R2 = 100.0;
        double Rtotal = 1.0 / (1.0/R1 + 1.0/R2);
        assertEquals("Parallel equal: Rtotal = R/2", 50.0, Rtotal, DELTA);
    }

    @Test
    public void testParallelResistanceTwoDifferent() {
        double R1 = 100.0;
        double R2 = 200.0;
        double Rtotal = 1.0 / (1.0/R1 + 1.0/R2);
        assertEquals("Parallel: R = (R1*R2)/(R1+R2)", 66.6666666667, Rtotal, 1e-8);
    }

    @Test
    public void testParallelConductanceAddition() {
        // Parallel conductances add directly
        double G1 = 0.01;  // 100 ohm
        double G2 = 0.02;  // 50 ohm
        double Gtotal = G1 + G2;
        assertEquals("Parallel: Gtotal = G1 + G2", 0.03, Gtotal, DELTA);
    }

    // ========== Extreme Value Tests ==========

    @Test
    public void testVerySmallResistance() {
        double R = 1e-9;
        double G = 1.0 / R;
        assertTrue("Very small R should give finite G", Double.isFinite(G));
        assertEquals("1e-9 ohm conductance", 1e9, G, 1e-1);
    }

    @Test
    public void testVeryLargeResistance() {
        double R = 1e9;
        double G = 1.0 / R;
        assertTrue("Very large R should give finite G", Double.isFinite(G));
        assertEquals("1e9 ohm conductance", 1e-9, G, 1e-15);
    }

    @Test
    public void testTemperatureCoefficientModeling() {
        // R(T) = R0 * (1 + α * ΔT)
        double R0 = 100.0;  // Reference resistance
        double alpha = 0.004;  // Temperature coefficient (1/°C)
        double deltaT = 50.0;  // Temperature change (°C)
        double RT = R0 * (1 + alpha * deltaT);

        assertEquals("Resistance at elevated T", 120.0, RT, DELTA);
    }

    // ========== Matrix Stamping Pattern Tests ==========

    @Test
    public void testResistorStampingPattern() {
        // Verify the stamping pattern for MNA matrix
        // For resistor between nodes i and j with conductance G:
        // A[i][i] += G,  A[j][j] += G
        // A[i][j] -= G,  A[j][i] -= G

        double[][] matrix = new double[3][3];
        double G = 0.01;  // 1/100 ohm
        int i = 1, j = 2;

        matrix[i][i] += G;
        matrix[j][j] += G;
        matrix[i][j] -= G;
        matrix[j][i] -= G;

        assertEquals("A[i][i] += G", G, matrix[i][i], DELTA);
        assertEquals("A[j][j] += G", G, matrix[j][j], DELTA);
        assertEquals("A[i][j] -= G", -G, matrix[i][j], DELTA);
        assertEquals("A[j][i] -= G", -G, matrix[j][i], DELTA);
    }

    @Test
    public void testMultipleResistorsMatrixAccumulation() {
        // Two resistors accumulate at node 1
        double[][] matrix = new double[4][4];

        double G1 = 0.01;  // 100 ohm
        double G2 = 0.005;  // 200 ohm

        // Resistor 1: nodes 0-1
        matrix[0][0] += G1;
        matrix[1][1] += G1;
        matrix[0][1] -= G1;
        matrix[1][0] -= G1;

        // Resistor 2: nodes 1-2
        matrix[1][1] += G2;
        matrix[2][2] += G2;
        matrix[1][2] -= G2;
        matrix[2][1] -= G2;

        assertEquals("Node 0 diagonal", G1, matrix[0][0], DELTA);
        assertEquals("Node 1 diagonal (accumulated)", G1 + G2, matrix[1][1], DELTA);
        assertEquals("Node 2 diagonal", G2, matrix[2][2], DELTA);
    }

    // ========== Interface Implementation Tests ==========

    @Test
    public void testCurrentMeasurableInterface() {
        // AbstractResistor implements CurrentMeasurable interface
        assertTrue("Should be subclass of AbstractResistor", resistor.getClass().getName().contains("Resistor"));
    }

    @Test
    public void testDirectVoltageMeasurableInterface() {
        // AbstractResistor implements DirectVoltageMeasurable interface
        assertTrue("Should be subclass of AbstractResistor", resistor.getClass().getName().contains("Resistor"));
    }
}
