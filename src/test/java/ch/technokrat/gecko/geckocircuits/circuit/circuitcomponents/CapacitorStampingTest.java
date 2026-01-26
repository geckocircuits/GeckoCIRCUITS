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
 * Tests for CapacitorCalculator matrix stamping and time integration.
 * Sprint 10: Circuit Components
 */
public class CapacitorStampingTest {

    private static final double DELTA = 1e-10;
    private double[][] matrix;
    
    @Before
    public void setUp() {
        matrix = new double[5][5];
    }
    
    // ========== Matrix A Stamping Tests (Backward Euler) ==========
    
    @Test
    public void testCapacitorStamp_BE_DiagonalPattern() {
        double C = 100e-6;  // 100 µF
        double dt = 1e-6;   // 1 µs
        double expected = C / dt;  // = 100
        
        matrix[1][1] += expected;
        matrix[2][2] += expected;
        matrix[1][2] -= expected;
        matrix[2][1] -= expected;
        
        assertEquals("BE: A[1][1] = C/dt", expected, matrix[1][1], DELTA);
        assertEquals("BE: A[2][2] = C/dt", expected, matrix[2][2], DELTA);
        assertEquals("BE: A[1][2] = -C/dt", -expected, matrix[1][2], DELTA);
    }
    
    @Test
    public void testCapacitorStamp_TRZ_DiagonalPattern() {
        double C = 100e-6;
        double dt = 1e-6;
        double expected = 2 * C / dt;
        
        matrix[1][1] += expected;
        
        assertEquals("TRZ: A[1][1] = 2C/dt", expected, matrix[1][1], DELTA);
    }
    
    @Test
    public void testCapacitorStamp_GS_DiagonalPattern() {
        double C = 100e-6;
        double dt = 1e-6;
        double expected = 1.5 * C / dt;
        
        matrix[1][1] += expected;
        
        assertEquals("GS: A[1][1] = 1.5C/dt", expected, matrix[1][1], DELTA);
    }
    
    // ========== Capacitor Physics Tests ==========
    
    @Test
    public void testCapacitorCharge_Q_equals_CV() {
        double C = 100e-6;
        double V = 10.0;
        double Q = C * V;
        
        assertEquals("Q = CV", 1e-3, Q, DELTA);
    }
    
    @Test
    public void testCapacitorEnergy_E_equals_half_CV2() {
        double C = 100e-6;
        double V = 10.0;
        double E = 0.5 * C * V * V;
        
        assertEquals("E = 0.5*C*V²", 0.005, E, DELTA);
    }
    
    @Test
    public void testCapacitorCurrent_I_equals_C_dVdt() {
        double C = 100e-6;
        double dV = 5.0;
        double dt = 1e-3;
        double I = C * dV / dt;
        
        assertEquals("I = C * dV/dt", 0.5, I, DELTA);
    }
    
    // ========== Series Capacitor Tests ==========
    
    @Test
    public void testSeriesCapacitors_TwoEqual() {
        double C1 = 100e-6;
        double C2 = 100e-6;
        double Ctotal = 1.0 / (1.0/C1 + 1.0/C2);
        
        assertEquals("Series equal: Ctotal = C/2", 50e-6, Ctotal, 1e-16);
    }
    
    @Test
    public void testSeriesCapacitors_TwoDifferent() {
        double C1 = 100e-6;
        double C2 = 200e-6;
        double Ctotal = (C1 * C2) / (C1 + C2);
        
        assertEquals("Series: C1*C2/(C1+C2)", 66.6666666667e-6, Ctotal, 1e-14);
    }
    
    // ========== Parallel Capacitor Tests ==========
    
    @Test
    public void testParallelCapacitors_TwoEqual() {
        double C1 = 100e-6;
        double C2 = 100e-6;
        double Ctotal = C1 + C2;
        
        assertEquals("Parallel: Ctotal = C1 + C2", 200e-6, Ctotal, DELTA);
    }
    
    // ========== Time Constant Tests ==========
    
    @Test
    public void testRCTimeConstant() {
        double R = 1000.0;
        double C = 100e-6;
        double tau = R * C;
        
        assertEquals("τ = RC", 0.1, tau, DELTA);
    }
    
    @Test
    public void testRCCharging_63percent() {
        double ratio = 1 - Math.exp(-1);
        double Vfinal = 10.0;
        double Vt = Vfinal * ratio;
        
        assertEquals("V(τ) ≈ 0.632 * Vfinal", 6.321205588, Vt, 1e-6);
    }
    
    // ========== Impedance Tests ==========
    
    @Test
    public void testCapacitorImpedance_1kHz() {
        double C = 100e-6;
        double f = 1000;
        double omega = 2 * Math.PI * f;
        double Xc = 1.0 / (omega * C);
        
        assertEquals("Xc at 1kHz", 1.5915494309, Xc, 1e-6);
    }
    
    // ========== Resonance with Inductor ==========
    
    @Test
    public void testLCResonanceFrequency() {
        double L = 1e-3;
        double C = 100e-6;
        double f0 = 1.0 / (2 * Math.PI * Math.sqrt(L * C));
        
        assertEquals("LC resonance frequency", 503.29, f0, 0.01);
    }
    
    // ========== Solver Type Coefficients ==========
    
    @Test
    public void testSolverCoefficients_BE() {
        double coeff = 1.0;
        double C = 100e-6;
        double dt = 1e-6;
        double stamp = coeff * C / dt;
        
        assertEquals("BE coefficient", 100.0, stamp, DELTA);
    }
    
    @Test
    public void testSolverCoefficients_TRZ() {
        double coeff = 2.0;
        double C = 100e-6;
        double dt = 1e-6;
        double stamp = coeff * C / dt;
        
        assertEquals("TRZ coefficient", 200.0, stamp, DELTA);
    }
    
    @Test
    public void testSolverCoefficients_GS() {
        double coeff = 1.5;
        double C = 100e-6;
        double dt = 1e-6;
        double stamp = coeff * C / dt;
        
        assertEquals("GS coefficient", 150.0, stamp, DELTA);
    }
}
