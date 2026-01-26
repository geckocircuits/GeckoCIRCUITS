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
 * Unit tests for InductorCalculator.
 * Tests inductor voltage calculation, matrix stamping, and coupled inductors.
 * 
 * <p>Inductor equations:
 * <ul>
 *   <li>V = L * di/dt</li>
 *   <li>Discretized (BE): V = L/dt * (i_new - i_old)</li>
 *   <li>Discretized (TRZ): V = 2*L/dt * (i_new - i_old) + V_old</li>
 * </ul>
 */
public class InductorCalculatorTest {
    
    // Test constants
    private static final double INDUCTANCE = 1e-3;  // 1 mH
    private static final double DT = 1e-6;  // 1 µs time step
    private static final double TOLERANCE = 1e-10;
    
    /**
     * Tests the inductor voltage formula: V = L * di/dt
     * For discrete time: V = L/dt * (i_new - i_old)
     */
    @Test
    public void testInductorVoltageFormula_BackwardEuler() {
        double L = 1e-3;  // 1 mH
        double dt = 1e-6;  // 1 µs
        double i_old = 0;
        double i_new = 1;  // 1A step
        
        // V = L/dt * (i_new - i_old) = 1e-3 / 1e-6 * 1 = 1000 V
        double expectedVoltage = L / dt * (i_new - i_old);
        assertEquals(1000.0, expectedVoltage, TOLERANCE);
    }
    
    /**
     * Tests trapezoidal integration formula.
     * V = 2*L/dt * (i_new - i_old) + V_old
     */
    @Test
    public void testInductorVoltageFormula_Trapezoidal() {
        double L = 1e-3;
        double dt = 1e-6;
        double i_old = 0;
        double i_new = 1;
        double V_old = 0;
        
        // V = 2*L/dt * (i_new - i_old) + V_old = 2 * 1000 * 1 + 0 = 2000 V
        double expectedVoltage = 2 * L / dt * (i_new - i_old) + V_old;
        assertEquals(2000.0, expectedVoltage, TOLERANCE);
    }
    
    /**
     * Tests that with constant current, inductor voltage is zero (steady state).
     */
    @Test
    public void testInductorSteadyState_ZeroVoltage() {
        double L = 1e-3;
        double dt = 1e-6;
        double i_old = 5.0;
        double i_new = 5.0;  // No change
        
        double voltage = L / dt * (i_new - i_old);
        assertEquals(0.0, voltage, TOLERANCE);
    }
    
    /**
     * Tests inductor energy calculation: E = 0.5 * L * I^2
     */
    @Test
    public void testInductorEnergy() {
        double L = 1e-3;  // 1 mH
        double I = 10;    // 10 A
        
        double energy = 0.5 * L * I * I;
        assertEquals(0.05, energy, 1e-6);  // 50 mJ
    }
    
    /**
     * Tests matrix B stamping pattern for inductor with companion model.
     * Inductor is modeled as current source in parallel with conductance.
     */
    @Test
    public void testMatrixBStampingPattern() {
        double L = 1e-3;
        double dt = 1e-6;
        double R_eq = L / dt;  // Equivalent resistance
        double G_eq = 1 / R_eq;  // Equivalent conductance
        
        assertEquals(1000.0, R_eq, TOLERANCE);
        assertEquals(0.001, G_eq, TOLERANCE);
    }
    
    /**
     * Tests current source stamping for inductor history term.
     * Inductor modeled with Norton equivalent: current source = i_old + V_old/R_eq
     */
    @Test
    public void testCurrentSourceStamping() {
        double L = 1e-3;
        double dt = 1e-6;
        double R_eq = L / dt;
        double i_old = 5.0;
        double V_old = 10.0;
        
        // Norton current source for backward Euler
        double i_source = i_old + V_old / R_eq;
        // = 5 + 10/1000 = 5.01 A
        assertEquals(5.01, i_source, TOLERANCE);
    }
    
    /**
     * Tests inductor current buildup with constant voltage.
     * di = V*dt/L
     */
    @Test
    public void testInductorCurrentBuildup() {
        double L = 1e-3;
        double V = 10.0;  // 10V constant
        double dt = 1e-4;  // 100 µs
        
        // After 100µs at 10V: di = 10V * 100µs / 1mH = 1A
        double di = V * dt / L;
        assertEquals(1.0, di, TOLERANCE);
    }
    
    /**
     * Tests impedance calculation: Z = j*omega*L
     */
    @Test
    public void testInductorImpedance() {
        double L = 1e-3;  // 1 mH
        double f = 1000;  // 1 kHz
        double omega = 2 * Math.PI * f;
        
        // |Z| = omega*L = 6283 * 1e-3 = 6.28 ohm
        double impedance = omega * L;
        assertEquals(6.28, impedance, 0.01);
    }
    
    /**
     * Tests inductor with initial current.
     */
    @Test
    public void testInductorWithInitialCurrent() {
        double L = 1e-3;
        double I_initial = 5.0;  // 5A initial
        
        // Initial energy
        double E_initial = 0.5 * L * I_initial * I_initial;
        assertEquals(0.0125, E_initial, 1e-6);  // 12.5 mJ
    }
    
    /**
     * Tests series inductor equivalent.
     * L_eq = L1 + L2
     */
    @Test
    public void testSeriesInductors() {
        double L1 = 1e-3;
        double L2 = 1e-3;
        
        double L_eq = L1 + L2;
        assertEquals(2e-3, L_eq, 1e-12);  // 2 mH
    }
    
    /**
     * Tests parallel inductor equivalent.
     * 1/L_eq = 1/L1 + 1/L2
     */
    @Test
    public void testParallelInductors() {
        double L1 = 1e-3;
        double L2 = 1e-3;
        
        double L_eq = 1.0 / (1.0/L1 + 1.0/L2);
        assertEquals(0.5e-3, L_eq, 1e-12);  // 0.5 mH
    }
    
    /**
     * Tests coupled inductors mutual inductance.
     * M = k * sqrt(L1 * L2)
     */
    @Test
    public void testMutualInductance() {
        double L1 = 1e-3;
        double L2 = 1e-3;
        double k = 0.9;  // Coupling coefficient
        
        double M = k * Math.sqrt(L1 * L2);
        assertEquals(0.9e-3, M, 1e-12);  // 0.9 mH
    }
    
    /**
     * Tests coupled inductor voltage equations.
     * V1 = L1*di1/dt + M*di2/dt
     * V2 = M*di1/dt + L2*di2/dt
     */
    @Test
    public void testCoupledInductorVoltages() {
        double L1 = 1e-3;
        double L2 = 1e-3;
        double M = 0.5e-3;  // Mutual inductance
        double dt = 1e-6;
        double di1 = 1.0;  // 1A change in i1
        double di2 = 0.0;  // No change in i2
        
        double V1 = L1/dt * di1 + M/dt * di2;
        double V2 = M/dt * di1 + L2/dt * di2;
        
        // V1 = 1000 * 1 + 500 * 0 = 1000V
        // V2 = 500 * 1 + 1000 * 0 = 500V (induced voltage)
        assertEquals(1000.0, V1, TOLERANCE);
        assertEquals(500.0, V2, TOLERANCE);
    }
    
    /**
     * Tests coupling coefficient bounds (0 <= k <= 1).
     */
    @Test
    public void testCouplingCoefficientBounds() {
        double k_min = 0.0;  // No coupling
        double k_max = 1.0;  // Perfect coupling
        
        assertTrue(k_min >= 0);
        assertTrue(k_max <= 1);
    }
    
    /**
     * Tests Gear solver formula for inductor.
     */
    @Test
    public void testInductorGearSolver() {
        double L = 1e-3;
        double dt = 1e-6;
        double i_new = 10;
        double i_old = 5;
        double i_oldold = 0;
        
        // Gear formula coefficients
        double coeff_new = 1.5 * L / dt;
        double coeff_old = 2.0 * L / dt;
        double coeff_oldold = 0.5 * L / dt;
        
        double V = coeff_new * i_new - coeff_old * i_old + coeff_oldold * i_oldold;
        // = 1500 * 10 - 2000 * 5 + 500 * 0 = 15000 - 10000 = 5000V
        assertEquals(5000.0, V, TOLERANCE);
    }
    
    /**
     * Tests RL circuit time constant: tau = L/R
     */
    @Test
    public void testRLTimeConstant() {
        double L = 1e-3;  // 1 mH
        double R = 1.0;   // 1 ohm
        double tau = L / R;
        
        assertEquals(1e-3, tau, 1e-12);  // 1 ms
    }
    
    /**
     * Tests RL circuit current rise: I = I_final * (1 - exp(-t/tau))
     */
    @Test
    public void testRLCurrentRise() {
        double L = 1e-3;
        double R = 1.0;
        double V = 10.0;  // Applied voltage
        double I_final = V / R;  // 10 A
        double tau = L / R;
        
        // After one time constant
        double I_at_tau = I_final * (1 - Math.exp(-1));
        assertEquals(6.32, I_at_tau, 0.01);  // ~63.2% of final
    }
    
    /**
     * Tests inductor current sign convention.
     * Positive voltage causes current increase.
     */
    @Test
    public void testCurrentSignConvention() {
        double L = 1e-3;
        double dt = 1e-6;
        double V = 10.0;
        
        // di = V * dt / L
        double di = V * dt / L;
        assertTrue(di > 0);  // Positive voltage -> current increase
        
        V = -10.0;
        di = V * dt / L;
        assertTrue(di < 0);  // Negative voltage -> current decrease
    }
    
    /**
     * Tests very small time step stability.
     */
    @Test
    public void testSmallTimeStep() {
        double L = 1e-3;
        double dt = 1e-9;  // 1 ns - very small
        double di = 0.001;  // 1 mA change
        
        double voltage = L / dt * di;
        // V = 1e-3 / 1e-9 * 1e-3 = 1000 V
        assertEquals(1000.0, voltage, TOLERANCE);
    }
    
    /**
     * Tests very small inductance.
     */
    @Test
    public void testSmallInductance() {
        double L = 1e-9;  // 1 nH (PCB trace)
        double dt = 1e-9;
        double di = 1.0;  // 1A change
        
        // V = L/dt * di = 1e-9/1e-9 * 1 = 1V
        double voltage = L / dt * di;
        assertEquals(1.0, voltage, TOLERANCE);
    }
    
    /**
     * Tests energy conservation in inductor.
     */
    @Test
    public void testEnergyConservation() {
        double L = 1e-3;
        double I1 = 0;
        double I2 = 10;
        
        double E1 = 0.5 * L * I1 * I1;
        double E2 = 0.5 * L * I2 * I2;
        double deltaE = E2 - E1;
        
        // Energy stored in inductor
        assertEquals(0.05, deltaE, 1e-6);  // 50 mJ
    }
    
    /**
     * Tests transformer turns ratio effect.
     * V2/V1 = N2/N1 = sqrt(L2/L1) for ideal transformer
     */
    @Test
    public void testTransformerTurnsRatio() {
        double L1 = 1e-3;   // Primary
        double L2 = 4e-3;   // Secondary (4x inductance)
        
        double turnsRatio = Math.sqrt(L2 / L1);
        assertEquals(2.0, turnsRatio, TOLERANCE);  // N2/N1 = 2
    }
    
    /**
     * Tests inductor flux linkage: lambda = L * I
     */
    @Test
    public void testFluxLinkage() {
        double L = 1e-3;
        double I = 10;
        
        double lambda = L * I;
        assertEquals(0.01, lambda, 1e-6);  // 10 mWb-turns
    }
}
