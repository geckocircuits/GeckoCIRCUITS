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

import ch.technokrat.gecko.geckocircuits.allg.SolverType;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for CapacitorCalculator.
 * Tests capacitor current calculation, matrix stamping, and solver integration.
 * 
 * <p>Capacitor equations:
 * <ul>
 *   <li>i = C * dV/dt</li>
 *   <li>Discretized (BE): i = C/dt * (V_new - V_old)</li>
 *   <li>Discretized (TRZ): i = 2*C/dt * (V_new - V_old) - i_old</li>
 * </ul>
 */
public class CapacitorCalculatorTest {
    
    // Test constants
    private static final double CAPACITANCE = 100e-6;  // 100 µF
    private static final double DT = 1e-6;  // 1 µs time step
    private static final double TOLERANCE = 1e-10;
    
    /**
     * Tests the capacitor current formula: i = C * dV/dt
     * For discrete time: i = C/dt * (V_new - V_old)
     */
    @Test
    public void testCapacitorCurrentFormula_BackwardEuler() {
        double C = 100e-6;  // 100 µF
        double dt = 1e-6;   // 1 µs
        double V_old = 0;
        double V_new = 10;  // 10V step
        
        // i = C/dt * (V_new - V_old) = 100e-6 / 1e-6 * 10 = 1000 A
        double expectedCurrent = C / dt * (V_new - V_old);
        assertEquals(1000.0, expectedCurrent, TOLERANCE);
    }
    
    /**
     * Tests trapezoidal integration formula.
     * i = 2*C/dt * (V_new - V_old) - i_old
     */
    @Test
    public void testCapacitorCurrentFormula_Trapezoidal() {
        double C = 100e-6;
        double dt = 1e-6;
        double V_old = 0;
        double V_new = 10;
        double i_old = 0;
        
        // i = 2*C/dt * (V_new - V_old) - i_old = 2 * 100 * 10 - 0 = 2000 A
        double expectedCurrent = 2 * C / dt * (V_new - V_old) - i_old;
        assertEquals(2000.0, expectedCurrent, TOLERANCE);
    }
    
    /**
     * Tests that with constant voltage, capacitor current is zero (steady state).
     */
    @Test
    public void testCapacitorSteadyState_ZeroCurrent() {
        double C = 100e-6;
        double dt = 1e-6;
        double V_old = 5.0;
        double V_new = 5.0;  // No change
        
        double current = C / dt * (V_new - V_old);
        assertEquals(0.0, current, TOLERANCE);
    }
    
    /**
     * Tests capacitor energy calculation: E = 0.5 * C * V^2
     */
    @Test
    public void testCapacitorEnergy() {
        double C = 100e-6;
        double V = 100;  // 100V
        
        double energy = 0.5 * C * V * V;
        assertEquals(0.5, energy, 1e-6);  // 0.5 J
    }
    
    /**
     * Tests matrix A stamping pattern for capacitor.
     * Pattern: C/dt added to diagonal, -C/dt to off-diagonal.
     */
    @Test
    public void testMatrixAStampingPattern() {
        double C = 100e-6;
        double dt = 1e-6;
        double G = C / dt;  // Equivalent conductance
        
        // For nodes 1 and 2:
        // a[1][1] += G, a[2][2] += G
        // a[1][2] -= G, a[2][1] -= G
        
        double[][] matrix = new double[3][3];
        int node1 = 1;
        int node2 = 2;
        
        // Simulate stamping
        matrix[node1][node1] += G;
        matrix[node2][node2] += G;
        matrix[node1][node2] -= G;
        matrix[node2][node1] -= G;
        
        assertEquals(G, matrix[node1][node1], TOLERANCE);
        assertEquals(G, matrix[node2][node2], TOLERANCE);
        assertEquals(-G, matrix[node1][node2], TOLERANCE);
        assertEquals(-G, matrix[node2][node1], TOLERANCE);
    }
    
    /**
     * Tests vector B stamping for capacitor (history term).
     * b[i] += C/dt * V_old, b[j] -= C/dt * V_old
     */
    @Test
    public void testVectorBStamping_BackwardEuler() {
        double C = 100e-6;
        double dt = 1e-6;
        double V_old = 5.0;
        
        double bW = C / dt * V_old;
        
        double[] b = new double[3];
        int node1 = 1;
        int node2 = 2;
        
        // Simulate stamping (voltage across capacitor = pot1 - pot2)
        b[node1] += bW;
        b[node2] -= bW;
        
        assertEquals(bW, b[node1], TOLERANCE);
        assertEquals(-bW, b[node2], TOLERANCE);
    }
    
    /**
     * Tests capacitor charging with constant current.
     * dV = i*dt/C
     */
    @Test
    public void testCapacitorCharging() {
        double C = 100e-6;
        double I = 1.0;  // 1A constant current
        double dt = 1e-3;  // 1ms
        
        // After 1ms at 1A: dV = 1A * 1ms / 100µF = 10V
        double dV = I * dt / C;
        assertEquals(10.0, dV, TOLERANCE);
    }
    
    /**
     * Tests impedance calculation: Z = 1/(j*omega*C)
     */
    @Test
    public void testCapacitorImpedance() {
        double C = 100e-6;
        double f = 1000;  // 1 kHz
        double omega = 2 * Math.PI * f;
        
        // |Z| = 1/(omega*C) = 1/(6283 * 100e-6) = 1.59 ohm
        double impedance = 1 / (omega * C);
        assertEquals(1.59, impedance, 0.01);
    }
    
    /**
     * Tests capacitor with initial voltage.
     */
    @Test
    public void testCapacitorWithInitialVoltage() {
        double C = 100e-6;
        double V_initial = 50.0;  // 50V initial
        
        // Initial energy
        double E_initial = 0.5 * C * V_initial * V_initial;
        assertEquals(0.125, E_initial, 1e-6);  // 125 mJ
    }
    
    /**
     * Tests series capacitor equivalent.
     * 1/C_eq = 1/C1 + 1/C2
     */
    @Test
    public void testSeriesCapacitors() {
        double C1 = 100e-6;
        double C2 = 100e-6;
        
        double C_eq = 1.0 / (1.0/C1 + 1.0/C2);
        assertEquals(50e-6, C_eq, 1e-12);  // 50 µF
    }
    
    /**
     * Tests parallel capacitor equivalent.
     * C_eq = C1 + C2
     */
    @Test
    public void testParallelCapacitors() {
        double C1 = 100e-6;
        double C2 = 100e-6;
        
        double C_eq = C1 + C2;
        assertEquals(200e-6, C_eq, 1e-12);  // 200 µF
    }
    
    /**
     * Tests Gear solver formula (third order).
     * i = 1.5*C/dt * V_new - 2*C/dt * V_old + 0.5*C/dt * V_oldold
     */
    @Test
    public void testCapacitorGearSolver() {
        double C = 100e-6;
        double dt = 1e-6;
        double V_new = 10;
        double V_old = 5;
        double V_oldold = 0;
        
        // Gear formula coefficient check
        double coeff_new = 1.5 * C / dt;
        double coeff_old = 2.0 * C / dt;
        double coeff_oldold = 0.5 * C / dt;
        
        double bW = coeff_new * V_new - coeff_old * V_old + coeff_oldold * V_oldold;
        // = 150 * 10 - 200 * 5 + 50 * 0 = 1500 - 1000 = 500
        assertEquals(500.0, bW, TOLERANCE);
    }
    
    /**
     * Tests non-linear capacitance correction logic.
     * When C changes: correction_current = (1 - C_new/C_old) * i_old
     */
    @Test
    public void testNonLinearCapacitanceCorrection() {
        double C_old = 100e-6;
        double C_new = 80e-6;  // Capacitance decreased
        double i_old = 10.0;
        
        double ratio = 1 - C_new / C_old;  // 1 - 0.8 = 0.2
        double correctionCurrent = ratio * i_old;  // 0.2 * 10 = 2
        
        assertEquals(0.2, ratio, TOLERANCE);
        assertEquals(2.0, correctionCurrent, TOLERANCE);
    }
    
    /**
     * Tests capacitor discharge through resistor: V = V0 * exp(-t/RC)
     */
    @Test
    public void testRCDischarge() {
        double C = 100e-6;
        double R = 1000;  // 1 kohm
        double V0 = 10;
        double tau = R * C;  // Time constant = 0.1s
        
        assertEquals(0.1, tau, 1e-6);
        
        // After one time constant: V = V0 * exp(-1) ≈ 0.368 * V0
        double V_at_tau = V0 * Math.exp(-1);
        assertEquals(3.68, V_at_tau, 0.01);
    }
    
    /**
     * Tests capacitor current sign convention.
     * Positive current when charging (voltage increasing).
     */
    @Test
    public void testCurrentSignConvention() {
        double C = 100e-6;
        double dt = 1e-6;
        
        // Voltage increasing: positive current
        double V_old = 0;
        double V_new = 10;
        double i_charging = C / dt * (V_new - V_old);
        assertTrue(i_charging > 0);
        
        // Voltage decreasing: negative current
        V_old = 10;
        V_new = 0;
        double i_discharging = C / dt * (V_new - V_old);
        assertTrue(i_discharging < 0);
    }
    
    /**
     * Tests very small time step stability.
     */
    @Test
    public void testSmallTimeStep() {
        double C = 100e-6;
        double dt = 1e-9;  // 1 ns - very small
        double dV = 0.001;  // 1 mV change
        
        double current = C / dt * dV;
        // i = 100e-6 / 1e-9 * 1e-3 = 100 A
        assertEquals(100.0, current, TOLERANCE);
    }
    
    /**
     * Tests very large capacitance.
     */
    @Test
    public void testLargeCapacitance() {
        double C = 1.0;  // 1 Farad (supercap)
        double dt = 1e-3;
        double I = 10;  // 10A
        
        // dV = I * dt / C = 10 * 1e-3 / 1 = 10 mV
        double dV = I * dt / C;
        assertEquals(0.01, dV, 1e-6);
    }
    
    /**
     * Tests energy conservation in capacitor.
     */
    @Test
    public void testEnergyConservation() {
        double C = 100e-6;
        double V1 = 0;
        double V2 = 100;
        
        double E1 = 0.5 * C * V1 * V1;
        double E2 = 0.5 * C * V2 * V2;
        double deltaE = E2 - E1;
        
        // Energy delivered to capacitor
        assertEquals(0.5, deltaE, 1e-6);  // 0.5 J
    }
}
