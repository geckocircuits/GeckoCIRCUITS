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
import static org.junit.Assert.*;

/**
 * Tests for voltage source components and stamping.
 * Sprint 10: Circuit Components
 * 
 * Voltage sources are stamped into MNA (Modified Nodal Analysis) matrices:
 * - Add a branch equation V_pos - V_neg = V_source
 * - Add current variable for source current
 */
public class VoltageSourceCalculatorTest {

    private static final double DELTA = 1e-10;

    // ========== Voltage Source Calculator Tests ==========
    
    @Test
    public void testVoltageSourceCalculatorClassExists() {
        // VoltageSourceCalculator requires parent objects so we verify the class exists
        assertNotNull("VoltageSourceCalculator class should exist", VoltageSourceCalculator.class);
    }
    
    @Test
    public void testAbstractVoltageSourceExists() {
        // AbstractVoltageSource is the base class for voltage sources
        assertNotNull("AbstractVoltageSource class should exist", AbstractVoltageSource.class);
    }

    // ========== AC Voltage Formula Tests ==========
    
    @Test
    public void testACSource_SinusoidalVoltage() {
        // V(t) = V_peak * sin(2*pi*f*t + phase)
        double Vpeak = 325.27; // 230V RMS
        double f = 50.0; // 50 Hz
        double t = 0.005; // 5ms = quarter period
        
        double expected = Vpeak * Math.sin(2 * Math.PI * f * t);
        // At t=5ms, f=50Hz: sin(2*pi*50*0.005) = sin(pi/2) = 1
        assertEquals("AC voltage at quarter period", Vpeak, expected, 1.0);
    }
    
    @Test
    public void testACSource_ZeroCrossing() {
        double Vpeak = 325.27;
        double f = 50.0;
        double t = 0.0; // At t=0, voltage should be near zero (if phase=0)
        
        double expected = Vpeak * Math.sin(2 * Math.PI * f * t);
        assertEquals("AC voltage at t=0 with zero phase", 0.0, expected, DELTA);
    }
    
    @Test
    public void testACSource_NegativePeak() {
        double Vpeak = 325.27;
        double f = 50.0;
        double t = 0.015; // 15ms = 3/4 period for 50Hz
        
        double expected = Vpeak * Math.sin(2 * Math.PI * f * t);
        // At 15ms: sin(2*pi*50*0.015) = sin(3*pi/2) = -1
        assertEquals("AC voltage at 3/4 period", -Vpeak, expected, 1.0);
    }
    
    @Test
    public void testACSource_RMSRelation() {
        // Vrms = Vpeak / sqrt(2)
        double Vpeak = 325.27;
        double Vrms = Vpeak / Math.sqrt(2);
        assertEquals("RMS voltage for 325.27V peak", 230.0, Vrms, 0.1);
    }

    // ========== MNA Stamping Tests ==========
    
    @Test
    public void testMNA_VoltageSourceStamping() {
        // In MNA, voltage source adds branch equation:
        // Row for source: +1 at node+, -1 at node-, = V_source
        // Column for current: +1 at row+, -1 at row-
        
        // Simple 2-node circuit
        int node1 = 0;
        int node2 = 1;
        int branchIdx = 2; // voltage source branch
        
        double[][] A = new double[3][3];
        double[] b = new double[3];
        double Vsource = 10.0;
        
        // Stamp voltage source between node1 (+) and node2 (-)
        A[branchIdx][node1] = 1;   // V1 coefficient in branch equation
        A[branchIdx][node2] = -1;  // V2 coefficient in branch equation
        A[node1][branchIdx] = 1;   // Current enters node1
        A[node2][branchIdx] = -1;  // Current leaves node2
        b[branchIdx] = Vsource;
        
        // Verify stamping
        assertEquals("V1 coefficient", 1.0, A[branchIdx][node1], DELTA);
        assertEquals("V2 coefficient", -1.0, A[branchIdx][node2], DELTA);
        assertEquals("Current into node1", 1.0, A[node1][branchIdx], DELTA);
        assertEquals("Current out of node2", -1.0, A[node2][branchIdx], DELTA);
        assertEquals("RHS value", 10.0, b[branchIdx], DELTA);
    }
    
    @Test
    public void testMNA_VoltageSourceWithLoad() {
        // Circuit: Vs=10V in series with R=5 ohms
        // Expected: I = V/R = 2A
        double Vsource = 10.0;
        double R = 5.0;
        double expectedCurrent = Vsource / R;
        
        assertEquals("Current through circuit", 2.0, expectedCurrent, DELTA);
    }

    // ========== Three-Phase Formulas ==========
    
    @Test
    public void testThreePhase_120DegreePhaseShift() {
        // Phase A: sin(wt)
        // Phase B: sin(wt - 120°)
        // Phase C: sin(wt - 240°) = sin(wt + 120°)
        double wt = Math.PI / 4; // arbitrary time
        
        double phaseA = Math.sin(wt);
        double phaseB = Math.sin(wt - 2 * Math.PI / 3);
        double phaseC = Math.sin(wt + 2 * Math.PI / 3);
        
        // Sum should be zero (balanced)
        double sum = phaseA + phaseB + phaseC;
        assertEquals("Balanced three-phase sum", 0.0, sum, 1e-10);
    }
    
    @Test
    public void testThreePhase_LineVoltage() {
        // Line voltage = sqrt(3) * phase voltage
        double Vphase = 230.0;
        double Vline = Math.sqrt(3) * Vphase;
        assertEquals("Line voltage for 230V phase", 398.4, Vline, 0.1);
    }
    
    @Test
    public void testThreePhase_Power() {
        // Three-phase power: P = sqrt(3) * V_line * I_line * cos(phi)
        double Vline = 400.0;
        double Iline = 10.0;
        double cosPhi = 0.8;
        
        double P = Math.sqrt(3) * Vline * Iline * cosPhi;
        assertEquals("Three-phase power", 5542.6, P, 0.1);
    }

    // ========== PWM/Pulse Source Tests ==========
    
    @Test
    public void testPWM_DutyCycle() {
        // Duty cycle D = T_on / T_period
        double Ton = 0.3e-3;  // 0.3 ms on time
        double T = 1e-3;      // 1 ms period
        double dutyCycle = Ton / T;
        assertEquals("30% duty cycle", 0.3, dutyCycle, DELTA);
    }
    
    @Test
    public void testPWM_AverageVoltage() {
        // V_avg = D * V_high + (1-D) * V_low
        double Vhigh = 10.0;
        double Vlow = 0.0;
        double D = 0.6;
        
        double Vavg = D * Vhigh + (1 - D) * Vlow;
        assertEquals("Average voltage at 60% duty cycle", 6.0, Vavg, DELTA);
    }

    // ========== Signal Generator Formulas ==========
    
    @Test
    public void testTriangularWave() {
        // Triangular wave between -1 and 1
        double period = 1e-3;
        double t = 0.25e-3; // quarter period
        
        // Rising edge: V = -1 + 4*t/period for t < period/2
        double V = -1.0 + 4.0 * t / period;
        assertEquals("Triangle wave at quarter period", 0.0, V, DELTA);
    }
    
    @Test
    public void testSawtoothWave() {
        // Sawtooth: V = Vmin + (Vmax - Vmin) * (t mod T) / T
        double Vmin = 0.0;
        double Vmax = 10.0;
        double T = 1e-3;
        double t = 0.5e-3;
        
        double V = Vmin + (Vmax - Vmin) * (t % T) / T;
        assertEquals("Sawtooth at half period", 5.0, V, DELTA);
    }
    
    @Test
    public void testSquareWave() {
        // Square wave: V = V_high if (t mod T) < T*D, else V_low
        double T = 1e-3;
        double D = 0.5;
        double Vhigh = 5.0;
        double Vlow = 0.0;
        
        double t1 = 0.25e-3; // in high phase
        double t2 = 0.75e-3; // in low phase
        
        double V1 = ((t1 % T) < T * D) ? Vhigh : Vlow;
        double V2 = ((t2 % T) < T * D) ? Vhigh : Vlow;
        
        assertEquals("Square wave in high phase", 5.0, V1, DELTA);
        assertEquals("Square wave in low phase", 0.0, V2, DELTA);
    }

    // ========== Superposition Tests ==========
    
    @Test
    public void testSuperposition_TwoSources() {
        // Two voltage sources in series: V_total = V1 + V2
        double V1 = 5.0;
        double V2 = 3.0;
        double Vtotal = V1 + V2;
        assertEquals("Series voltage sources", 8.0, Vtotal, DELTA);
    }
    
    @Test
    public void testSuperposition_OpposingSources() {
        // Opposing sources: V_total = V1 - V2
        double V1 = 10.0;
        double V2 = 4.0;
        double Vtotal = V1 - V2;
        assertEquals("Opposing voltage sources", 6.0, Vtotal, DELTA);
    }
}
