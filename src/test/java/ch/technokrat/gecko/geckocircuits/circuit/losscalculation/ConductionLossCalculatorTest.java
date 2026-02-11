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
 * Unit tests for ConductionLossCalculator.
 * Tests conduction loss formulas, temperature effects, and edge cases.
 */
public class ConductionLossCalculatorTest {
    
    private static final double TOLERANCE = 1e-10;
    
    // ===========================================
    // Resistive Model Tests (P = I² × R)
    // ===========================================
    
    @Test
    public void testResistiveModel_BasicCalculation() {
        ConductionLossCalculator calc = ConductionLossCalculator.resistiveModel(0.01);  // 10 mΩ
        
        // P = I² × R = 10² × 0.01 = 1 W
        double loss = calc.calculateLoss(10.0);
        assertEquals(1.0, loss, TOLERANCE);
    }
    
    @Test
    public void testResistiveModel_ZeroCurrent() {
        ConductionLossCalculator calc = ConductionLossCalculator.resistiveModel(0.01);
        
        double loss = calc.calculateLoss(0.0);
        assertEquals(0.0, loss, TOLERANCE);
    }
    
    @Test
    public void testResistiveModel_NegativeCurrent() {
        ConductionLossCalculator calc = ConductionLossCalculator.resistiveModel(0.01);
        
        // P = I² × R - same for negative current
        double loss = calc.calculateLoss(-10.0);
        assertEquals(1.0, loss, TOLERANCE);
    }
    
    @Test
    public void testResistiveModel_HighCurrent() {
        ConductionLossCalculator calc = ConductionLossCalculator.resistiveModel(0.001);  // 1 mΩ
        
        // P = 100² × 0.001 = 10 W
        double loss = calc.calculateLoss(100.0);
        assertEquals(10.0, loss, TOLERANCE);
    }
    
    @Test
    public void testResistiveModel_IsResistiveOnly() {
        ConductionLossCalculator calc = ConductionLossCalculator.resistiveModel(0.01);
        assertTrue(calc.isResistiveOnly());
    }
    
    // ===========================================
    // Threshold + Resistive Model Tests
    // ===========================================
    
    @Test
    public void testThresholdResistiveModel_BasicCalculation() {
        // V_th = 0.7V (diode), R_on = 0.01Ω
        ConductionLossCalculator calc = ConductionLossCalculator.thresholdResistiveModel(0.7, 0.01);
        
        // P = V_th × |I| + R_on × I² = 0.7 × 10 + 0.01 × 100 = 7 + 1 = 8 W
        double loss = calc.calculateLoss(10.0);
        assertEquals(8.0, loss, TOLERANCE);
    }
    
    @Test
    public void testThresholdResistiveModel_ZeroCurrent() {
        ConductionLossCalculator calc = ConductionLossCalculator.thresholdResistiveModel(0.7, 0.01);
        
        double loss = calc.calculateLoss(0.0);
        assertEquals(0.0, loss, TOLERANCE);
    }
    
    @Test
    public void testThresholdResistiveModel_NegativeCurrent() {
        ConductionLossCalculator calc = ConductionLossCalculator.thresholdResistiveModel(0.7, 0.01);
        
        // Uses absolute value for threshold term
        double loss = calc.calculateLoss(-10.0);
        assertEquals(8.0, loss, TOLERANCE);
    }
    
    @Test
    public void testThresholdResistiveModel_IsNotResistiveOnly() {
        ConductionLossCalculator calc = ConductionLossCalculator.thresholdResistiveModel(0.7, 0.01);
        assertFalse(calc.isResistiveOnly());
    }
    
    // ===========================================
    // On-State Voltage Tests
    // ===========================================
    
    @Test
    public void testOnStateVoltage_Resistive() {
        ConductionLossCalculator calc = ConductionLossCalculator.resistiveModel(0.01);
        
        // V_on = R × I = 0.01 × 10 = 0.1 V
        double voltage = calc.calculateOnStateVoltage(10.0);
        assertEquals(0.1, voltage, TOLERANCE);
    }
    
    @Test
    public void testOnStateVoltage_ThresholdResistive() {
        ConductionLossCalculator calc = ConductionLossCalculator.thresholdResistiveModel(0.7, 0.01);
        
        // V_on = V_th + R × I = 0.7 + 0.01 × 10 = 0.8 V
        double voltage = calc.calculateOnStateVoltage(10.0);
        assertEquals(0.8, voltage, TOLERANCE);
    }
    
    @Test
    public void testOnStateVoltage_ZeroCurrent() {
        ConductionLossCalculator calc = ConductionLossCalculator.thresholdResistiveModel(0.7, 0.01);
        
        // V_on = V_th at zero current
        double voltage = calc.calculateOnStateVoltage(0.0);
        assertEquals(0.7, voltage, TOLERANCE);
    }
    
    // ===========================================
    // Temperature Effect Tests
    // ===========================================
    
    @Test
    public void testTemperatureEffect_IncreasedResistance() {
        // α = 0.004 per °C (typical for silicon)
        ConductionLossCalculator calc = new ConductionLossCalculator(0.0, 0.01, 0.004);
        
        // At 25°C: R = 0.01 Ω
        // At 125°C: R = 0.01 × (1 + 0.004 × 100) = 0.01 × 1.4 = 0.014 Ω
        double R_125 = calc.getTemperatureAdjustedResistance(125.0);
        assertEquals(0.014, R_125, TOLERANCE);
    }
    
    @Test
    public void testTemperatureEffect_ReferenceTemperature() {
        ConductionLossCalculator calc = new ConductionLossCalculator(0.0, 0.01, 0.004);
        
        // At reference temperature (25°C), no adjustment
        double R_25 = calc.getTemperatureAdjustedResistance(25.0);
        assertEquals(0.01, R_25, TOLERANCE);
    }
    
    @Test
    public void testTemperatureEffect_LossCalculation() {
        ConductionLossCalculator calc = new ConductionLossCalculator(0.0, 0.01, 0.004);
        
        double loss_25 = calc.calculateLoss(10.0, 25.0);   // P = 100 × 0.01 = 1 W
        double loss_125 = calc.calculateLoss(10.0, 125.0); // P = 100 × 0.014 = 1.4 W
        
        assertEquals(1.0, loss_25, TOLERANCE);
        assertEquals(1.4, loss_125, TOLERANCE);
    }
    
    @Test
    public void testTemperatureEffect_ColdCondition() {
        ConductionLossCalculator calc = new ConductionLossCalculator(0.0, 0.01, 0.004);
        
        // At -25°C: R = 0.01 × (1 + 0.004 × (-50)) = 0.01 × 0.8 = 0.008 Ω
        double R_minus25 = calc.getTemperatureAdjustedResistance(-25.0);
        assertEquals(0.008, R_minus25, TOLERANCE);
    }
    
    // ===========================================
    // Average Loss Tests
    // ===========================================
    
    @Test
    public void testAverageLoss_Calculation() {
        ConductionLossCalculator calc = ConductionLossCalculator.thresholdResistiveModel(0.7, 0.01);
        
        // P_avg = V_th × I_avg + R × I_rms²
        // = 0.7 × 5 + 0.01 × 7² = 3.5 + 0.49 = 3.99 W
        double avgLoss = calc.calculateAverageLoss(5.0, 7.0);
        assertEquals(3.99, avgLoss, TOLERANCE);
    }
    
    @Test
    public void testAverageLoss_ZeroAvgCurrent() {
        ConductionLossCalculator calc = ConductionLossCalculator.thresholdResistiveModel(0.7, 0.01);
        
        // AC current with zero average: P = R × I_rms²
        double avgLoss = calc.calculateAverageLoss(0.0, 10.0);
        assertEquals(1.0, avgLoss, TOLERANCE);
    }
    
    // ===========================================
    // Energy Calculation Tests
    // ===========================================
    
    @Test
    public void testEnergyCalculation() {
        ConductionLossCalculator calc = ConductionLossCalculator.resistiveModel(0.01);
        
        // E = P × Δt = 1 W × 0.001 s = 1 mJ
        double energy = calc.calculateEnergy(10.0, 0.001);
        assertEquals(0.001, energy, TOLERANCE);
    }
    
    @Test
    public void testEnergyCalculation_LongDuration() {
        ConductionLossCalculator calc = ConductionLossCalculator.resistiveModel(0.01);
        
        // E = 1 W × 3600 s = 3600 J = 1 Wh
        double energy = calc.calculateEnergy(10.0, 3600);
        assertEquals(3600, energy, TOLERANCE);
    }
    
    // ===========================================
    // Getter Tests
    // ===========================================
    
    @Test
    public void testGetters() {
        ConductionLossCalculator calc = new ConductionLossCalculator(0.7, 0.01, 0.004);
        
        assertEquals(0.7, calc.getThresholdVoltage(), TOLERANCE);
        assertEquals(0.01, calc.getOnResistance(), TOLERANCE);
        assertEquals(0.004, calc.getTemperatureCoefficient(), TOLERANCE);
    }
    
    // ===========================================
    // Validation Tests
    // ===========================================
    
    @Test(expected = IllegalArgumentException.class)
    public void testValidation_NegativeResistance() {
        ConductionLossCalculator.resistiveModel(-0.01);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testValidation_NegativeThreshold() {
        ConductionLossCalculator.thresholdResistiveModel(-0.7, 0.01);
    }
    
    @Test
    public void testValidation_ZeroResistance() {
        // Zero resistance is allowed (ideal switch)
        ConductionLossCalculator calc = ConductionLossCalculator.resistiveModel(0.0);
        assertEquals(0.0, calc.calculateLoss(10.0), TOLERANCE);
    }
    
    // ===========================================
    // Real-World Component Tests
    // ===========================================
    
    @Test
    public void testRealWorld_MOSFET() {
        // Typical MOSFET: R_ds(on) = 10 mΩ at 25°C, α = 0.004/°C
        ConductionLossCalculator calc = new ConductionLossCalculator(0.0, 0.010, 0.004);
        
        // At 10A, 25°C: P = 1 W
        assertEquals(1.0, calc.calculateLoss(10.0, 25.0), 0.01);
        
        // At 10A, 100°C: P = 1.3 W (30% increase)
        assertEquals(1.3, calc.calculateLoss(10.0, 100.0), 0.01);
    }
    
    @Test
    public void testRealWorld_Diode() {
        // Typical power diode: V_f = 0.7V, R_d = 5 mΩ
        ConductionLossCalculator calc = ConductionLossCalculator.thresholdResistiveModel(0.7, 0.005);
        
        // At 20A: P = 0.7 × 20 + 0.005 × 400 = 14 + 2 = 16 W
        assertEquals(16.0, calc.calculateLoss(20.0), TOLERANCE);
    }
    
    @Test
    public void testRealWorld_IGBT() {
        // Typical IGBT: V_ce(sat) = 1.5V, R = 10 mΩ
        ConductionLossCalculator calc = ConductionLossCalculator.thresholdResistiveModel(1.5, 0.010);
        
        // At 50A: P = 1.5 × 50 + 0.01 × 2500 = 75 + 25 = 100 W
        assertEquals(100.0, calc.calculateLoss(50.0), TOLERANCE);
    }
    
    // ===========================================
    // Numerical Stability Tests
    // ===========================================
    
    @Test
    public void testNumericalStability_VerySmallCurrent() {
        ConductionLossCalculator calc = ConductionLossCalculator.resistiveModel(0.01);
        
        double loss = calc.calculateLoss(1e-9);  // 1 nA
        assertTrue(loss >= 0);
        assertFalse(Double.isNaN(loss));
    }
    
    @Test
    public void testNumericalStability_VeryLargeCurrent() {
        ConductionLossCalculator calc = ConductionLossCalculator.resistiveModel(0.001);
        
        double loss = calc.calculateLoss(1000);  // 1 kA
        assertEquals(1000.0, loss, TOLERANCE);  // P = 1e6 × 1e-3 = 1000 W
    }
    
    @Test
    public void testNumericalStability_VerySmallResistance() {
        ConductionLossCalculator calc = ConductionLossCalculator.resistiveModel(1e-9);
        
        double loss = calc.calculateLoss(100);
        assertEquals(1e-5, loss, 1e-15);  // P = 10000 × 1e-9 = 1e-5 W
    }
}
