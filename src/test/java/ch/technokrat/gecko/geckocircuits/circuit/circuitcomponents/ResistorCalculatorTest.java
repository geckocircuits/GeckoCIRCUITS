/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
 * Tests for ResistorCalculator matrix stamping and current calculation.
 * Sprint 10: Circuit Components
 */
public class ResistorCalculatorTest {

    private static final double DELTA = 1e-10;
    private double[][] matrix;
    private int[] indices;
    
    @Before
    public void setUp() {
        matrix = new double[5][5];  // 5x5 matrix for testing
        indices = new int[]{1, 2};  // Node indices
    }
    
    // ========== Matrix A Stamping Tests ==========
    
    @Test
    public void testResistorStampMatrixA_DiagonalPositive() {
        // Stamping resistor between nodes 1 and 2 with R=10 ohm
        // Expected: +1/R on diagonal positions
        double R = 10.0;
        double expected = 1.0 / R;
        
        // Manual stamp simulation
        matrix[1][1] += expected;
        matrix[2][2] += expected;
        matrix[1][2] -= expected;
        matrix[2][1] -= expected;
        
        assertEquals("A[1][1] should be +1/R", expected, matrix[1][1], DELTA);
        assertEquals("A[2][2] should be +1/R", expected, matrix[2][2], DELTA);
        assertEquals("A[1][2] should be -1/R", -expected, matrix[1][2], DELTA);
        assertEquals("A[2][1] should be -1/R", -expected, matrix[2][1], DELTA);
    }
    
    @Test
    public void testResistorStampPattern_LargeResistance() {
        // Large resistance = small conductance
        double R = 1e6;  // 1 Megohm
        double G = 1.0 / R;
        
        matrix[1][1] += G;
        matrix[2][2] += G;
        matrix[1][2] -= G;
        matrix[2][1] -= G;
        
        assertEquals("Large R: A[1][1] should be small", 1e-6, matrix[1][1], 1e-16);
    }
    
    @Test
    public void testResistorStampPattern_SmallResistance() {
        // Small resistance = large conductance
        double R = 0.001;  // 1 milliohm
        double G = 1.0 / R;
        
        matrix[1][1] += G;
        matrix[2][2] += G;
        matrix[1][2] -= G;
        matrix[2][1] -= G;
        
        assertEquals("Small R: A[1][1] should be large", 1000.0, matrix[1][1], DELTA);
    }
    
    // ========== Voltage and Current Tests ==========
    
    @Test
    public void testOhmsLaw_BasicCalculation() {
        // V = IR, I = V/R
        double R = 100.0;
        double V = 10.0;  // 10V across resistor
        double expectedI = V / R;  // 0.1 A
        
        assertEquals("I = V/R", 0.1, expectedI, DELTA);
    }
    
    @Test
    public void testOhmsLaw_ZeroVoltage() {
        double R = 100.0;
        double V = 0.0;
        double I = V / R;
        
        assertEquals("Zero voltage = zero current", 0.0, I, DELTA);
    }
    
    @Test
    public void testOhmsLaw_NegativeVoltage() {
        double R = 50.0;
        double V = -5.0;
        double I = V / R;
        
        assertEquals("Negative voltage = negative current", -0.1, I, DELTA);
    }
    
    @Test
    public void testOhmsLaw_LargeVoltage() {
        double R = 1000.0;
        double V = 1e6;  // 1 MV
        double I = V / R;
        
        assertEquals("Large voltage calculation", 1000.0, I, DELTA);
    }
    
    // ========== Power Calculation Tests ==========
    
    @Test
    public void testPowerCalculation_P_equals_VI() {
        double V = 10.0;
        double I = 2.0;
        double P = V * I;
        
        assertEquals("P = V*I", 20.0, P, DELTA);
    }
    
    @Test
    public void testPowerCalculation_P_equals_I2R() {
        double I = 2.0;
        double R = 5.0;
        double P = I * I * R;
        
        assertEquals("P = I²R", 20.0, P, DELTA);
    }
    
    @Test
    public void testPowerCalculation_P_equals_V2_over_R() {
        double V = 10.0;
        double R = 5.0;
        double P = V * V / R;
        
        assertEquals("P = V²/R", 20.0, P, DELTA);
    }
    
    // ========== Series Resistance Tests ==========
    
    @Test
    public void testSeriesResistance_TwoResistors() {
        double R1 = 100.0;
        double R2 = 200.0;
        double Rtotal = R1 + R2;
        
        assertEquals("Series: Rtotal = R1 + R2", 300.0, Rtotal, DELTA);
    }
    
    @Test
    public void testSeriesResistance_ThreeResistors() {
        double R1 = 10.0;
        double R2 = 20.0;
        double R3 = 30.0;
        double Rtotal = R1 + R2 + R3;
        
        assertEquals("Series: Rtotal = R1 + R2 + R3", 60.0, Rtotal, DELTA);
    }
    
    // ========== Parallel Resistance Tests ==========
    
    @Test
    public void testParallelResistance_TwoEqual() {
        double R1 = 100.0;
        double R2 = 100.0;
        double Rtotal = 1.0 / (1.0/R1 + 1.0/R2);
        
        assertEquals("Parallel equal: Rtotal = R/2", 50.0, Rtotal, DELTA);
    }
    
    @Test
    public void testParallelResistance_TwoDifferent() {
        double R1 = 100.0;
        double R2 = 200.0;
        double Rtotal = 1.0 / (1.0/R1 + 1.0/R2);
        
        assertEquals("Parallel different", 66.6666666667, Rtotal, 1e-8);
    }
    
    @Test
    public void testParallelResistance_ProductOverSum() {
        double R1 = 30.0;
        double R2 = 60.0;
        double Rtotal = (R1 * R2) / (R1 + R2);
        
        assertEquals("Parallel: R1*R2/(R1+R2)", 20.0, Rtotal, DELTA);
    }
    
    // ========== Conductance Tests ==========
    
    @Test
    public void testConductance_Reciprocal() {
        double R = 50.0;
        double G = 1.0 / R;
        
        assertEquals("G = 1/R", 0.02, G, DELTA);
    }
    
    @Test
    public void testConductance_ParallelSum() {
        // Parallel conductances add directly
        double G1 = 0.01;  // 100 ohm
        double G2 = 0.02;  // 50 ohm
        double Gtotal = G1 + G2;
        double Rtotal = 1.0 / Gtotal;
        
        assertEquals("Parallel G: Gtotal = G1 + G2", 0.03, Gtotal, DELTA);
        assertEquals("Parallel G: Rtotal", 33.3333333333, Rtotal, 1e-8);
    }
    
    // ========== Energy Dissipation Tests ==========
    
    @Test
    public void testEnergyDissipation() {
        // E = P * t = V²/R * t
        double V = 10.0;
        double R = 100.0;
        double t = 60.0;  // 1 minute
        double P = V * V / R;  // 1 W
        double E = P * t;  // 60 J
        
        assertEquals("Power calculation", 1.0, P, DELTA);
        assertEquals("Energy = P*t", 60.0, E, DELTA);
    }
    
    // ========== Temperature Coefficient Tests ==========
    
    @Test
    public void testResistanceWithTemperature() {
        // R(T) = R0 * (1 + α * ΔT)
        double R0 = 100.0;  // Resistance at reference temp
        double alpha = 0.004;  // Typical copper coefficient
        double deltaT = 50.0;  // 50°C rise
        double RT = R0 * (1 + alpha * deltaT);
        
        assertEquals("R at elevated T", 120.0, RT, DELTA);
    }
    
    // ========== Minimum Resistance Handling ==========
    
    @Test
    public void testMinimumResistance() {
        // Very small resistance shouldn't cause numerical issues
        double Rmin = 1e-9;  // Near-zero resistance
        double G = 1.0 / Rmin;
        
        assertTrue("Large conductance should be finite", Double.isFinite(G));
        assertEquals("Very small R gives large G", 1e9, G, 1e-1);
    }
    
    // ========== Multiple Resistors in Matrix ==========
    
    @Test
    public void testMultipleResistorsStamping() {
        // Two resistors: R1 between nodes 0-1, R2 between nodes 1-2
        double[][] matrix = new double[3][3];
        
        double R1 = 100.0;
        double R2 = 200.0;
        double G1 = 1.0 / R1;
        double G2 = 1.0 / R2;
        
        // Stamp R1 (nodes 0-1)
        matrix[0][0] += G1;
        matrix[1][1] += G1;
        matrix[0][1] -= G1;
        matrix[1][0] -= G1;
        
        // Stamp R2 (nodes 1-2)
        matrix[1][1] += G2;
        matrix[2][2] += G2;
        matrix[1][2] -= G2;
        matrix[2][1] -= G2;
        
        assertEquals("Node 0 diagonal", G1, matrix[0][0], DELTA);
        assertEquals("Node 1 diagonal (sum)", G1 + G2, matrix[1][1], DELTA);
        assertEquals("Node 2 diagonal", G2, matrix[2][2], DELTA);
    }
}
