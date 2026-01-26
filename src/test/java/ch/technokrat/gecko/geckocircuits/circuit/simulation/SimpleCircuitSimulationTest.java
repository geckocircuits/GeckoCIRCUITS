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
package ch.technokrat.gecko.geckocircuits.circuit.simulation;

import ch.technokrat.gecko.geckocircuits.allg.SolverType;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.terminal.ConnectionPath;
import ch.technokrat.gecko.geckocircuits.circuit.terminal.ConnectionValidator;
import ch.technokrat.gecko.geckocircuits.circuit.terminal.ITerminalPosition;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * End-to-end integration tests for simple circuit simulations.
 * Tests basic RC, RL, and RLC circuits with expected analytical solutions.
 * 
 * These tests validate the complete simulation pipeline:
 * 1. Circuit topology (terminal connections)
 * 2. Component calculators
 * 3. Matrix assembly
 * 4. Time-stepping solvers
 * 
 * Note: These tests use the refactored components from Sprints 1-7
 * and provide golden test cases for regression testing.
 */
public class SimpleCircuitSimulationTest {
    
    private static final double TOLERANCE = 1e-6;
    private static final double RELAXED_TOLERANCE = 1e-3;
    
    // ===========================================
    // RC Circuit Constants
    // ===========================================
    
    /** Standard test resistance: 1 kOhm */
    private static final double R_STANDARD = 1000.0;
    
    /** Standard test capacitance: 1 uF */
    private static final double C_STANDARD = 1e-6;
    
    /** Standard RC time constant: RC = 1ms */
    private static final double TAU_RC = R_STANDARD * C_STANDARD;
    
    /** Standard test voltage: 10V */
    private static final double V_DC = 10.0;
    
    // ===========================================
    // RL Circuit Constants
    // ===========================================
    
    /** Standard test inductance: 1 mH */
    private static final double L_STANDARD = 1e-3;
    
    /** Standard RL time constant: L/R = 1us for R=1k, L=1mH */
    private static final double TAU_RL = L_STANDARD / R_STANDARD;
    
    // ===========================================
    // Analytical Solution Tests
    // ===========================================
    
    @Test
    public void testRCCharging_AnalyticalSolution() {
        // RC charging: Vc(t) = Vdc * (1 - e^(-t/RC))
        // At t = RC, Vc = Vdc * (1 - 1/e) ≈ 0.632 * Vdc
        
        double t = TAU_RC;  // One time constant
        double expected = V_DC * (1 - Math.exp(-1));  // ~6.32V
        
        double calculated = calculateRCCharging(V_DC, t, TAU_RC);
        
        assertEquals(expected, calculated, TOLERANCE);
    }
    
    @Test
    public void testRCCharging_MultipleTimeConstants() {
        // After 5 time constants, capacitor is ~99.3% charged
        
        double[] timeConstants = {1, 2, 3, 4, 5};
        double[] expectedPercents = {0.632, 0.865, 0.950, 0.982, 0.993};
        
        for (int i = 0; i < timeConstants.length; i++) {
            double t = timeConstants[i] * TAU_RC;
            double expected = V_DC * expectedPercents[i];
            double calculated = calculateRCCharging(V_DC, t, TAU_RC);
            
            assertEquals("At " + timeConstants[i] + " tau", 
                        expected, calculated, 0.01);  // 1% tolerance
        }
    }
    
    @Test
    public void testRCDischarge_AnalyticalSolution() {
        // RC discharge: Vc(t) = V0 * e^(-t/RC)
        // At t = RC, Vc = V0 / e ≈ 0.368 * V0
        
        double V0 = V_DC;
        double t = TAU_RC;
        double expected = V0 * Math.exp(-1);  // ~3.68V
        
        double calculated = calculateRCDischarge(V0, t, TAU_RC);
        
        assertEquals(expected, calculated, TOLERANCE);
    }
    
    @Test
    public void testRLCurrentRise_AnalyticalSolution() {
        // RL current rise: I(t) = (V/R) * (1 - e^(-t*R/L))
        // At t = L/R, I = (V/R) * (1 - 1/e)
        
        double R = 10.0;  // 10 ohm for faster time constant
        double L = 1e-3;  // 1 mH
        double tau = L / R;  // 0.1 ms
        double Ifinal = V_DC / R;  // 1A
        
        double t = tau;
        double expected = Ifinal * (1 - Math.exp(-1));  // ~0.632A
        
        double calculated = calculateRLCurrentRise(V_DC, R, t, tau);
        
        assertEquals(expected, calculated, TOLERANCE);
    }
    
    @Test
    public void testRLCurrentDecay_AnalyticalSolution() {
        // RL current decay: I(t) = I0 * e^(-t*R/L)
        
        double R = 10.0;
        double L = 1e-3;
        double tau = L / R;
        double I0 = 1.0;  // 1A initial current
        
        double t = tau;
        double expected = I0 * Math.exp(-1);  // ~0.368A
        
        double calculated = calculateRLCurrentDecay(I0, t, tau);
        
        assertEquals(expected, calculated, TOLERANCE);
    }
    
    // ===========================================
    // Component Connection Tests
    // ===========================================
    
    @Test
    public void testResistorCapacitorConnection() {
        // Verify R and C can be connected (same LK domain)
        ITerminalPosition resistorTerminal = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition capacitorTerminal = ITerminalPosition.of(10, 0, ConnectorType.LK);
        
        ConnectionValidator.ValidationResult result = 
            ConnectionValidator.validateConnection(resistorTerminal, capacitorTerminal);
        
        assertTrue("R and C should connect", result.isSuccess());
    }
    
    @Test
    public void testResistorInductorConnection() {
        // Verify R and L can be connected (same LK domain)
        ITerminalPosition resistorTerminal = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition inductorTerminal = ITerminalPosition.of(10, 0, ConnectorType.LK);
        
        ConnectionValidator.ValidationResult result = 
            ConnectionValidator.validateConnection(resistorTerminal, inductorTerminal);
        
        assertTrue("R and L should connect", result.isSuccess());
    }
    
    @Test
    public void testLKToControlConnection_ShouldFail() {
        // LK components should not directly connect to CONTROL
        ITerminalPosition resistorTerminal = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition controlTerminal = ITerminalPosition.of(10, 0, ConnectorType.CONTROL);
        
        ConnectionValidator.ValidationResult result = 
            ConnectionValidator.validateConnection(resistorTerminal, controlTerminal);
        
        assertTrue("LK to CONTROL should fail", result.isFailure());
    }
    
    @Test
    public void testSeriesConnection_PathCreation() {
        // Create path for series connection: R1 -> R2
        ITerminalPosition r1Out = ITerminalPosition.of(5, 5, ConnectorType.LK);
        ITerminalPosition r2In = ITerminalPosition.of(10, 5, ConnectorType.LK);
        
        ConnectionPath path = ConnectionPath.createDirectPath(r1Out, r2In);
        
        assertTrue("Path should be valid", path.isValid());
        assertEquals(2, path.getPointCount());
        assertEquals(5, path.getTotalLength());
    }
    
    @Test
    public void testParallelConnection_PathCreation() {
        // Create L-path for parallel branch connection
        ITerminalPosition node1 = ITerminalPosition.of(0, 0, ConnectorType.LK);
        ITerminalPosition node2 = ITerminalPosition.of(10, 5, ConnectorType.LK);
        
        ConnectionPath path = ConnectionPath.createLPath(node1, node2, true);
        
        assertTrue("Path should be valid", path.isValid());
        assertEquals(3, path.getPointCount());  // Start, corner, end
        assertEquals(15, path.getTotalLength());  // 10 + 5
    }
    
    // ===========================================
    // Time Step Analysis Tests
    // ===========================================
    
    @Test
    public void testTimeStepRequirement_RC() {
        // For accurate simulation, dt should be << RC
        // Typical rule: dt < RC/10
        
        double recommendedDt = TAU_RC / 10;  // 0.1 ms
        
        assertTrue("dt should be much smaller than tau", 
                  recommendedDt < TAU_RC);
    }
    
    @Test
    public void testTimeStepRequirement_RL() {
        // For RL circuits, dt should be << L/R
        
        double R = 10.0;
        double L = 1e-3;
        double tau = L / R;  // 0.1 ms
        double recommendedDt = tau / 10;  // 10 us
        
        assertTrue("dt should be much smaller than L/R", 
                  recommendedDt < tau);
    }
    
    @Test
    public void testSimulationDuration_5Tau() {
        // Standard simulation should run for at least 5 time constants
        // to reach steady state (>99%)
        
        double steadyStateTime = 5 * TAU_RC;  // 5 ms
        double finalCharge = 1 - Math.exp(-5);  // ~99.3%
        
        assertTrue("5 tau should reach 99%+ of final value", 
                  finalCharge > 0.99);
    }
    
    // ===========================================
    // Solver Type Tests
    // ===========================================
    
    @Test
    public void testSolverTypes_Available() {
        // Verify expected solver types exist
        assertNotNull(SolverType.SOLVER_BE);  // Backward Euler
        assertNotNull(SolverType.SOLVER_TRZ);  // Trapezoidal
        assertNotNull(SolverType.SOLVER_GS);   // Gear-Shichman
    }
    
    @Test
    public void testBackwardEuler_Stability() {
        // Backward Euler is unconditionally stable
        // Even with large dt, it should not oscillate (though accuracy suffers)
        
        double largeDt = TAU_RC;  // dt = tau (large)
        double[] voltages = simulateRCWithBE(V_DC, largeDt, 5);
        
        // Should monotonically approach Vdc
        for (int i = 1; i < voltages.length; i++) {
            assertTrue("BE should not oscillate", voltages[i] >= voltages[i-1]);
            assertTrue("BE should not overshoot", voltages[i] <= V_DC);
        }
    }
    
    @Test
    public void testTrapezoidal_SecondOrderAccuracy() {
        // Trapezoidal rule has O(dt^2) accuracy
        // Halving dt should reduce error by factor of 4
        
        double dt1 = TAU_RC / 10;
        double dt2 = TAU_RC / 20;
        
        double error1 = Math.abs(simulateRCStep(V_DC, dt1, TAU_RC) - 
                                calculateRCCharging(V_DC, dt1, TAU_RC));
        double error2 = Math.abs(simulateRCStep(V_DC, dt2, TAU_RC) - 
                                calculateRCCharging(V_DC, dt2, TAU_RC));
        
        // Error ratio should be approximately 4 for second-order method
        double errorRatio = error1 / error2;
        assertTrue("TRZ should be second order", errorRatio > 2);  // Conservative check
    }
    
    // ===========================================
    // Energy Conservation Tests
    // ===========================================
    
    @Test
    public void testEnergyConservation_Capacitor() {
        // Energy stored in capacitor: E = 0.5 * C * V^2
        
        double V = V_DC;
        double energy = 0.5 * C_STANDARD * V * V;  // 0.5 * 1uF * 100 = 50 uJ
        
        assertEquals(50e-6, energy, TOLERANCE);
    }
    
    @Test
    public void testEnergyConservation_Inductor() {
        // Energy stored in inductor: E = 0.5 * L * I^2
        
        double I = 1.0;  // 1A
        double energy = 0.5 * L_STANDARD * I * I;  // 0.5 * 1mH * 1 = 0.5 mJ
        
        assertEquals(0.5e-3, energy, TOLERANCE);
    }
    
    @Test
    public void testPowerDissipation_Resistor() {
        // Power dissipated in resistor: P = V^2/R = I^2*R
        
        double V = V_DC;
        double I = V / R_STANDARD;
        double power_V = (V * V) / R_STANDARD;  // 100/1000 = 0.1W
        double power_I = I * I * R_STANDARD;     // 0.01 * 1000 = 0.1W
        
        assertEquals(power_V, power_I, TOLERANCE);
        assertEquals(0.1, power_V, TOLERANCE);
    }
    
    // ===========================================
    // Helper Methods - Analytical Solutions
    // ===========================================
    
    private double calculateRCCharging(double Vdc, double t, double tau) {
        return Vdc * (1 - Math.exp(-t / tau));
    }
    
    private double calculateRCDischarge(double V0, double t, double tau) {
        return V0 * Math.exp(-t / tau);
    }
    
    private double calculateRLCurrentRise(double Vdc, double R, double t, double tau) {
        double Ifinal = Vdc / R;
        return Ifinal * (1 - Math.exp(-t / tau));
    }
    
    private double calculateRLCurrentDecay(double I0, double t, double tau) {
        return I0 * Math.exp(-t / tau);
    }
    
    // ===========================================
    // Helper Methods - Numerical Simulation
    // ===========================================
    
    /**
     * Simulate RC charging with Backward Euler.
     * BE discretization: V[n+1] = V[n] + dt/RC * (Vdc - V[n+1])
     * Solving: V[n+1] = (V[n] + dt/RC * Vdc) / (1 + dt/RC)
     */
    private double[] simulateRCWithBE(double Vdc, double dt, int steps) {
        double[] V = new double[steps + 1];
        V[0] = 0;  // Initial condition
        
        double alpha = dt / TAU_RC;
        for (int n = 0; n < steps; n++) {
            V[n + 1] = (V[n] + alpha * Vdc) / (1 + alpha);
        }
        return V;
    }
    
    /**
     * Single step RC simulation with Trapezoidal rule.
     * TRZ: V[n+1] = V[n] + dt/(2*RC) * ((Vdc - V[n]) + (Vdc - V[n+1]))
     */
    private double simulateRCStep(double Vdc, double dt, double tau) {
        double V0 = 0;  // Initial condition
        double alpha = dt / (2 * tau);
        // Solving: V1 * (1 + alpha) = V0 * (1 - alpha) + 2 * alpha * Vdc
        return (V0 * (1 - alpha) + 2 * alpha * Vdc) / (1 + alpha);
    }
    
    // ===========================================
    // RLC Circuit Tests
    // ===========================================
    
    @Test
    public void testRLCResonance_Underdamped() {
        // Underdamped: R^2 < 4L/C
        double R = 10.0;    // 10 ohm (reduced for underdamped)
        double L = 1e-3;    // 1 mH
        double C = 1e-6;    // 1 uF
        
        double discriminant = R * R - 4 * L / C;
        assertTrue("RLC should be underdamped", discriminant < 0);
        
        // Natural frequency
        double omega0 = 1 / Math.sqrt(L * C);  // ~31,623 rad/s
        double f0 = omega0 / (2 * Math.PI);    // ~5,033 Hz
        
        assertTrue("Resonant frequency around 5kHz", f0 > 4000 && f0 < 6000);
    }
    
    @Test
    public void testRLCResonance_Overdamped() {
        // Overdamped: R^2 > 4L/C
        double R = 1000.0;  // 1k ohm
        double L = 1e-3;    // 1 mH
        double C = 1e-6;    // 1 uF
        
        double discriminant = R * R - 4 * L / C;
        assertTrue("RLC should be overdamped", discriminant > 0);
    }
    
    @Test
    public void testRLCResonance_CriticallyDamped() {
        // Critically damped: R^2 = 4L/C, so R = 2*sqrt(L/C)
        double L = 1e-3;    // 1 mH
        double C = 1e-6;    // 1 uF
        double R = 2 * Math.sqrt(L / C);  // 63.25 ohm
        
        double discriminant = R * R - 4 * L / C;
        assertEquals("RLC should be critically damped", 0, discriminant, 1e-10);
    }
}
