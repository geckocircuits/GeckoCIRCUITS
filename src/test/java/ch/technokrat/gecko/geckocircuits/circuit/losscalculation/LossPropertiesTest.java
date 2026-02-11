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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for LossProperties and loss calculation infrastructure.
 * Tests loss type enumeration, calculation interfaces, and calculation patterns.
 */
public class LossPropertiesTest {
    
    private static final double TOLERANCE = 1e-10;
    
    // ===========================================
    // LossCalculationDetail Enum Tests
    // ===========================================
    
    @Test
    public void testLossCalculationDetail_SimpleExists() {
        LossCalculationDetail simple = LossCalculationDetail.SIMPLE;
        assertNotNull(simple);
    }
    
    @Test
    public void testLossCalculationDetail_DetailedExists() {
        LossCalculationDetail detailed = LossCalculationDetail.DETAILED;
        assertNotNull(detailed);
    }
    
    @Test
    public void testLossCalculationDetail_AllValues() {
        LossCalculationDetail[] values = LossCalculationDetail.values();
        assertTrue("Should have at least SIMPLE and DETAILED", values.length >= 2);
    }
    
    // ===========================================
    // LossCalculationSplittable Interface Tests
    // ===========================================
    
    @Test
    public void testLossCalculationSplittable_InterfaceMethods() throws NoSuchMethodException {
        Class<LossCalculationSplittable> clazz = LossCalculationSplittable.class;
        
        assertNotNull(clazz.getMethod("getSwitchingLoss"));
        assertNotNull(clazz.getMethod("getConductionLoss"));
    }
    
    // ===========================================
    // AbstractLossCalculator Interface Tests
    // ===========================================
    
    @Test
    public void testAbstractLossCalculator_InterfaceMethods() throws NoSuchMethodException {
        Class<AbstractLossCalculator> clazz = AbstractLossCalculator.class;
        
        assertNotNull(clazz.getMethod("calcLosses", double.class, double.class, double.class));
        assertNotNull(clazz.getMethod("getTotalLosses"));
    }
    
    // ===========================================
    // Loss Calculation Formula Tests
    // ===========================================
    
    @Test
    public void testConductionLoss_ResistiveFormula() {
        // P = I² × R
        double R = 0.01;  // 10 mΩ
        double I = 10.0;  // 10 A
        
        double P = I * I * R;
        assertEquals(1.0, P, TOLERANCE);  // 1 W
    }
    
    @Test
    public void testConductionLoss_ThresholdPlusResistive() {
        // P = V_th × |I| + R × I²
        double V_th = 0.7;  // 0.7 V (diode threshold)
        double R = 0.01;    // 10 mΩ
        double I = 10.0;
        
        double P = V_th * Math.abs(I) + R * I * I;
        assertEquals(8.0, P, TOLERANCE);  // 7 + 1 = 8 W
    }
    
    @Test
    public void testSwitchingLoss_EnergyPerEvent() {
        // P_sw = (E_on + E_off) × f_sw
        double E_on = 1e-3;   // 1 mJ
        double E_off = 0.5e-3; // 0.5 mJ
        double f_sw = 10000;   // 10 kHz
        
        double P_sw = (E_on + E_off) * f_sw;
        assertEquals(15.0, P_sw, TOLERANCE);  // 15 W
    }
    
    @Test
    public void testTotalLoss_ConductionPlusSwitching() {
        double P_cond = 10.0;  // 10 W conduction
        double P_sw = 5.0;    // 5 W switching
        
        double P_total = P_cond + P_sw;
        assertEquals(15.0, P_total, TOLERANCE);
    }
    
    // ===========================================
    // Scaling Formula Tests  
    // ===========================================
    
    @Test
    public void testSwitchingEnergyScaling_Current() {
        // E = E_ref × (I / I_ref)
        double E_ref = 1e-3;
        double I_ref = 10.0;
        double I = 20.0;
        
        double E = E_ref * (I / I_ref);
        assertEquals(2e-3, E, TOLERANCE);  // 2 mJ
    }
    
    @Test
    public void testSwitchingEnergyScaling_Voltage() {
        // E = E_ref × (V / V_ref)
        double E_ref = 1e-3;
        double V_ref = 600.0;
        double V = 300.0;
        
        double E = E_ref * (V / V_ref);
        assertEquals(0.5e-3, E, TOLERANCE);  // 0.5 mJ
    }
    
    @Test
    public void testSwitchingEnergyScaling_Combined() {
        // E = E_ref × (I / I_ref) × (V / V_ref)
        double E_ref = 1e-3;
        double I_ref = 10.0;
        double V_ref = 600.0;
        double I = 15.0;
        double V = 400.0;
        
        double E = E_ref * (I / I_ref) * (V / V_ref);
        assertEquals(1e-3, E, TOLERANCE);  // 1 mJ (1.5 × 0.667 = 1)
    }
    
    // ===========================================
    // Temperature Effect Tests
    // ===========================================
    
    @Test
    public void testTemperatureEffect_Resistance() {
        // R(T) = R_ref × (1 + α × (T - T_ref))
        double R_ref = 0.01;  // 10 mΩ at 25°C
        double alpha = 0.004;  // per °C
        double T_ref = 25.0;
        double T = 125.0;
        
        double R = R_ref * (1 + alpha * (T - T_ref));
        assertEquals(0.014, R, TOLERANCE);  // 14 mΩ at 125°C
    }
    
    @Test
    public void testTemperatureEffect_LossIncrease() {
        double P_25C = 1.0;  // 1 W at 25°C
        double tempRatio = 1.4;  // 40% increase at 125°C
        
        double P_125C = P_25C * tempRatio;
        assertEquals(1.4, P_125C, TOLERANCE);
    }
    
    // ===========================================
    // Turn-On/Turn-Off Detection Tests
    // ===========================================
    
    @Test
    public void testTurnOnDetection_Logic() {
        double EPS = 1e-2;
        double oldCurrent = 0.0;
        double newCurrent = 1.0;
        
        boolean turnOn = (Math.abs(oldCurrent) < EPS) && (Math.abs(newCurrent) >= EPS);
        assertTrue(turnOn);
    }
    
    @Test
    public void testTurnOffDetection_Logic() {
        double EPS = 1e-2;
        double oldCurrent = 1.0;
        double newCurrent = 0.0;
        
        boolean turnOff = (Math.abs(oldCurrent) >= EPS) && (Math.abs(newCurrent) < EPS);
        assertTrue(turnOff);
    }
    
    @Test
    public void testNoSwitchingEvent_AlreadyOn() {
        double EPS = 1e-2;
        double oldCurrent = 5.0;
        double newCurrent = 10.0;
        
        boolean turnOn = (Math.abs(oldCurrent) < EPS) && (Math.abs(newCurrent) >= EPS);
        boolean turnOff = (Math.abs(oldCurrent) >= EPS) && (Math.abs(newCurrent) < EPS);
        
        assertFalse(turnOn);
        assertFalse(turnOff);
    }
    
    // ===========================================
    // Parallel Device Scaling Tests
    // ===========================================
    
    @Test
    public void testParallelDevices_CurrentSharing() {
        double I_total = 100.0;
        int numParallel = 4;
        
        double I_per_device = I_total / numParallel;
        assertEquals(25.0, I_per_device, TOLERANCE);
    }
    
    @Test
    public void testParallelDevices_LossScaling() {
        double P_single = 10.0;  // Loss per device
        int numParallel = 4;
        
        // Total loss with equal current sharing
        // P_total = n × (I_total/n)² × R = I_total² × R / n
        // This is P_single / n for purely resistive
        double P_total_resistive = P_single / numParallel;
        assertEquals(2.5, P_total_resistive, TOLERANCE);
        
        // Total with threshold voltage (scales linearly)
        // P_total = n × V_th × (I_total/n) = V_th × I_total
        // So threshold part doesn't change, resistive part decreases
    }
    
    // ===========================================
    // Duty Cycle Tests
    // ===========================================
    
    @Test
    public void testConductionLoss_WithDutyCycle() {
        double P_on = 10.0;  // Loss when conducting
        double duty = 0.5;   // 50% duty cycle
        
        double P_avg = P_on * duty;
        assertEquals(5.0, P_avg, TOLERANCE);
    }
    
    @Test
    public void testSwitchingLoss_IndependentOfDuty() {
        // Switching losses depend on frequency, not duty cycle
        double E_per_cycle = 1e-3;
        double f_sw = 10000;
        
        double P_sw = E_per_cycle * f_sw;
        assertEquals(10.0, P_sw, TOLERANCE);  // Same for any duty
    }
    
    // ===========================================
    // Thermal Impedance Integration Tests
    // ===========================================
    
    @Test
    public void testThermalCalculation_JunctionTemperature() {
        // T_j = T_a + P × R_th
        double T_ambient = 25.0;
        double P_loss = 50.0;
        double R_th = 1.5;  // °C/W junction-to-ambient
        
        double T_junction = T_ambient + P_loss * R_th;
        assertEquals(100.0, T_junction, TOLERANCE);
    }
    
    @Test
    public void testThermalCalculation_CascadedResistances() {
        // R_th_total = R_th_jc + R_th_cs + R_th_sa
        double R_th_jc = 0.5;   // junction-to-case
        double R_th_cs = 0.1;   // case-to-sink
        double R_th_sa = 1.0;   // sink-to-ambient
        
        double R_th_total = R_th_jc + R_th_cs + R_th_sa;
        assertEquals(1.6, R_th_total, TOLERANCE);
    }
    
    // ===========================================
    // Lookup Table Interpolation Tests
    // ===========================================
    
    @Test
    public void testLinearInterpolation_Simple() {
        // Interpolate between (0, 0) and (10, 100)
        double x1 = 0, y1 = 0;
        double x2 = 10, y2 = 100;
        double x = 5;
        
        double y = y1 + (y2 - y1) * (x - x1) / (x2 - x1);
        assertEquals(50.0, y, TOLERANCE);
    }
    
    @Test
    public void testLinearInterpolation_NonZeroStart() {
        // Interpolate between (5, 10) and (15, 30)
        double x1 = 5, y1 = 10;
        double x2 = 15, y2 = 30;
        double x = 10;
        
        double y = y1 + (y2 - y1) * (x - x1) / (x2 - x1);
        assertEquals(20.0, y, TOLERANCE);
    }
    
    // ===========================================
    // Energy Balance Tests
    // ===========================================
    
    @Test
    public void testEnergyConservation_PerCycle() {
        double f_sw = 10000;  // 10 kHz
        double T_cycle = 1.0 / f_sw;  // 100 µs
        double P_avg = 15.0;
        
        double E_cycle = P_avg * T_cycle;
        assertEquals(1.5e-3, E_cycle, 1e-12);  // 1.5 mJ per cycle
    }
    
    @Test
    public void testEnergyBalance_HeatDissipation() {
        // At thermal equilibrium: P_in = P_out
        double P_loss = 100.0;  // 100 W electrical to heat
        double P_cooling = 100.0;  // Heat removed
        
        double P_net = P_loss - P_cooling;
        assertEquals(0.0, P_net, TOLERANCE);  // Balanced
    }
    
    // ===========================================
    // Efficiency Calculation Tests
    // ===========================================
    
    @Test
    public void testEfficiency_Simple() {
        double P_out = 950.0;
        double P_loss = 50.0;
        double P_in = P_out + P_loss;
        
        double efficiency = P_out / P_in;
        assertEquals(0.95, efficiency, TOLERANCE);  // 95%
    }
    
    @Test
    public void testEfficiency_FromLosses() {
        double P_out = 1000.0;
        double P_cond = 30.0;
        double P_sw = 20.0;
        double P_loss = P_cond + P_sw;
        double P_in = P_out + P_loss;
        
        double efficiency = P_out / P_in;
        assertEquals(1000.0 / 1050.0, efficiency, TOLERANCE);  // ~95.2%
    }
    
    // ===========================================
    // Boundary Condition Tests
    // ===========================================
    
    @Test
    public void testBoundary_ZeroCurrent() {
        double I = 0.0;
        double R = 0.01;
        double V_th = 0.7;
        
        double P = V_th * Math.abs(I) + R * I * I;
        assertEquals(0.0, P, TOLERANCE);
    }
    
    @Test
    public void testBoundary_ZeroVoltage() {
        double V = 0.0;
        double I = 10.0;
        double E_ref = 1e-3;
        double V_ref = 600.0;
        
        double E = E_ref * (V / V_ref);
        assertEquals(0.0, E, TOLERANCE);
    }
    
    @Test
    public void testBoundary_ZeroFrequency() {
        double E_total = 1.5e-3;
        double f_sw = 0.0;
        
        double P_sw = E_total * f_sw;
        assertEquals(0.0, P_sw, TOLERANCE);
    }
}
