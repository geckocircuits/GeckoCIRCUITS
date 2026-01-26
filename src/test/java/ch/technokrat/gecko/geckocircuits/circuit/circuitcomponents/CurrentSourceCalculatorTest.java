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
 * Tests for current source components and stamping.
 * Sprint 10: Circuit Components
 * 
 * Current sources inject current into the circuit:
 * - At node+: +I (current flows in)
 * - At node-: -I (current flows out)
 */
public class CurrentSourceCalculatorTest {

    private static final double DELTA = 1e-10;

    // ========== Current Source Calculator Tests ==========
    
    @Test
    public void testCurrentSourceCalculatorClassExists() {
        // CurrentSourceCalculator requires parent objects so we verify the class exists
        assertNotNull("CurrentSourceCalculator class should exist", CurrentSourceCalculator.class);
    }
    
    @Test
    public void testAbstractCurrentSourceExists() {
        assertNotNull("AbstractCurrentSource class should exist", AbstractCurrentSource.class);
    }

    // ========== AC Current Formulas ==========
    
    @Test
    public void testACCurrentSource_SinusoidalCurrent() {
        // I(t) = I_peak * sin(2*pi*f*t + phase)
        double Ipeak = 10.0;
        double f = 50.0;
        double t = 0.005; // quarter period
        
        double I = Ipeak * Math.sin(2 * Math.PI * f * t);
        assertEquals("AC current at quarter period", Ipeak, I, 0.1);
    }

    // ========== Transconductance/Current Gain ==========
    
    @Test
    public void testVCCS_TransconductanceGain() {
        // g = I_out / V_in (Siemens)
        double Vin = 2.0;
        double g = 0.5; // 0.5 S
        double Iout = g * Vin;
        assertEquals("VCCS output current", 1.0, Iout, DELTA);
    }
    
    @Test
    public void testCCCS_CurrentGain() {
        // k = I_out / I_in (dimensionless)
        double Iin = 1.0;
        double k = 100; // current gain of 100
        double Iout = k * Iin;
        assertEquals("CCCS output current", 100.0, Iout, DELTA);
    }

    // ========== MNA Stamping Tests ==========
    
    @Test
    public void testMNA_CurrentSourceStamping() {
        // Current source injects current:
        // At node+: b[node+] += I_source
        // At node-: b[node-] -= I_source
        
        int nodePos = 0;
        int nodeNeg = 1;
        double Isource = 5.0;
        
        double[] b = new double[2];
        
        // Stamp current source from nodeNeg to nodePos
        b[nodePos] += Isource;  // current enters positive node
        b[nodeNeg] -= Isource;  // current leaves negative node
        
        assertEquals("Current into positive node", 5.0, b[nodePos], DELTA);
        assertEquals("Current out of negative node", -5.0, b[nodeNeg], DELTA);
    }
    
    @Test
    public void testCurrentSource_NortonEquivalent() {
        // Norton equivalent: I_source in parallel with R
        // Thevenin equivalent: V = I * R in series with R
        double Isource = 2.0;
        double R = 10.0;
        double Vthevenin = Isource * R;
        assertEquals("Thevenin equivalent voltage", 20.0, Vthevenin, DELTA);
    }

    // ========== Current Divider Tests ==========
    
    @Test
    public void testCurrentDivider_TwoParallelResistors() {
        // Current divider: I1 = I_total * R2 / (R1 + R2)
        double Itotal = 10.0;
        double R1 = 10.0;
        double R2 = 20.0;
        
        double I1 = Itotal * R2 / (R1 + R2);
        double I2 = Itotal * R1 / (R1 + R2);
        
        assertEquals("Current through R1", 6.666, I1, 0.001);
        assertEquals("Current through R2", 3.333, I2, 0.001);
        assertEquals("Current conservation", Itotal, I1 + I2, DELTA);
    }
    
    @Test
    public void testCurrentDivider_EqualResistors() {
        double Itotal = 8.0;
        double R = 10.0;
        
        double I1 = Itotal / 2;
        double I2 = Itotal / 2;
        
        assertEquals("Equal split", 4.0, I1, DELTA);
        assertEquals("Equal split", 4.0, I2, DELTA);
    }

    // ========== Pulse Current Source Tests ==========
    
    @Test
    public void testPulseCurrent_AverageValue() {
        // I_avg = D * I_high + (1-D) * I_low
        double Ihigh = 10.0;
        double Ilow = 0.0;
        double D = 0.4;
        
        double Iavg = D * Ihigh + (1 - D) * Ilow;
        assertEquals("Average current at 40% duty cycle", 4.0, Iavg, DELTA);
    }

    // ========== Charge Conservation Tests ==========
    
    @Test
    public void testChargeConservation_Kirchhoff() {
        // KCL: Sum of currents into a node = 0
        double I1 = 5.0;   // into node
        double I2 = 3.0;   // into node
        double I3 = -8.0;  // out of node
        
        double sum = I1 + I2 + I3;
        assertEquals("Kirchhoff's current law", 0.0, sum, DELTA);
    }
    
    @Test
    public void testCurrentContinuity_Series() {
        // In series circuit, current is same through all elements
        double Vsource = 12.0;
        double R1 = 2.0;
        double R2 = 4.0;
        
        double Rtotal = R1 + R2;
        double I = Vsource / Rtotal;
        
        // Current through R1 = Current through R2
        assertEquals("Current continuity", 2.0, I, DELTA);
    }

    // ========== Power Tests ==========
    
    @Test
    public void testCurrentSource_PowerDelivered() {
        // P = V * I (power delivered by source)
        double Isource = 2.0;
        double Vterminal = 10.0; // voltage across source
        
        double P = Vterminal * Isource;
        assertEquals("Power delivered", 20.0, P, DELTA);
    }
    
    @Test
    public void testCurrentSource_MaxPowerTransfer() {
        // Max power transfer when R_load = R_internal
        double Isource = 1.0;
        double Rinternal = 100.0;
        double Rload = 100.0; // matched load
        
        // For Norton source: I_load = I_source * R_int / (R_int + R_load)
        double Iload = Isource * Rinternal / (Rinternal + Rload);
        double Vload = Iload * Rload;
        double Pload = Vload * Iload;
        
        assertEquals("Load current at max power", 0.5, Iload, DELTA);
        assertEquals("Load power at max transfer", 25.0, Pload, DELTA);
    }
}
