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
 * Unit tests for SwitchingLossCalculator.
 * Tests switching energy formulas, scaling, turn-on/off detection, and edge cases.
 */
public class SwitchingLossCalculatorTest {
    
    private static final double TOLERANCE = 1e-10;
    
    // Reference values for typical IGBT (from datasheet)
    private static final double E_ON_REF = 1e-3;      // 1 mJ turn-on at reference
    private static final double E_OFF_REF = 0.5e-3;   // 0.5 mJ turn-off at reference
    private static final double I_REF = 10.0;          // 10 A reference current
    private static final double V_REF = 600.0;         // 600 V reference voltage
    
    // ===========================================
    // Factory Method Tests
    // ===========================================
    
    @Test
    public void testFromEnergies_Creation() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        assertEquals(E_ON_REF, calc.getTurnOnEnergyRef(), TOLERANCE);
        assertEquals(E_OFF_REF, calc.getTurnOffEnergyRef(), TOLERANCE);
        assertEquals(I_REF, calc.getReferenceCurrent(), TOLERANCE);
        assertEquals(V_REF, calc.getReferenceVoltage(), TOLERANCE);
    }
    
    @Test
    public void testFromPowerAndFrequency_Creation() {
        // P_sw = 100W at 10kHz -> E_total = 10 mJ -> E_on = E_off = 5 mJ each
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromPowerAndFrequency(
            100.0, 10000.0, I_REF, V_REF);
        
        assertEquals(5e-3, calc.getTurnOnEnergyRef(), TOLERANCE);
        assertEquals(5e-3, calc.getTurnOffEnergyRef(), TOLERANCE);
    }
    
    // ===========================================
    // Turn-On Energy Tests
    // ===========================================
    
    @Test
    public void testTurnOnEnergy_AtReferenceConditions() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        // At reference I and V, energy should equal reference
        double energy = calc.calculateTurnOnEnergy(I_REF, V_REF);
        assertEquals(E_ON_REF, energy, TOLERANCE);
    }
    
    @Test
    public void testTurnOnEnergy_DoubleCurrent() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        // E = E_ref × (I/I_ref) × (V/V_ref) = 1mJ × 2 × 1 = 2 mJ
        double energy = calc.calculateTurnOnEnergy(2 * I_REF, V_REF);
        assertEquals(2 * E_ON_REF, energy, TOLERANCE);
    }
    
    @Test
    public void testTurnOnEnergy_HalfVoltage() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        // E = E_ref × (I/I_ref) × (V/V_ref) = 1mJ × 1 × 0.5 = 0.5 mJ
        double energy = calc.calculateTurnOnEnergy(I_REF, V_REF / 2);
        assertEquals(E_ON_REF / 2, energy, TOLERANCE);
    }
    
    @Test
    public void testTurnOnEnergy_ScaledConditions() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        // E = 1mJ × (15/10) × (400/600) = 1mJ × 1.5 × 0.667 = 1 mJ
        double energy = calc.calculateTurnOnEnergy(15.0, 400.0);
        assertEquals(E_ON_REF, energy, TOLERANCE);
    }
    
    // ===========================================
    // Turn-Off Energy Tests
    // ===========================================
    
    @Test
    public void testTurnOffEnergy_AtReferenceConditions() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        double energy = calc.calculateTurnOffEnergy(I_REF, V_REF);
        assertEquals(E_OFF_REF, energy, TOLERANCE);
    }
    
    @Test
    public void testTurnOffEnergy_Scaled() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        // E_off = 0.5mJ × 3 × 2 = 3 mJ
        double energy = calc.calculateTurnOffEnergy(3 * I_REF, 2 * V_REF);
        assertEquals(6 * E_OFF_REF, energy, TOLERANCE);
    }
    
    // ===========================================
    // Total Energy Tests
    // ===========================================
    
    @Test
    public void testTotalEnergy_Sum() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        double total = calc.calculateTotalEnergy(I_REF, V_REF);
        assertEquals(E_ON_REF + E_OFF_REF, total, TOLERANCE);
    }
    
    @Test
    public void testTurnOnToTurnOffRatio() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        // E_on / E_off = 1mJ / 0.5mJ = 2
        assertEquals(2.0, calc.getTurnOnToTurnOffRatio(), TOLERANCE);
    }
    
    // ===========================================
    // Switching Power Tests
    // ===========================================
    
    @Test
    public void testSwitchingPower_Calculation() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        // P_sw = (E_on + E_off) × f = 1.5mJ × 10kHz = 15 W
        double power = calc.calculateSwitchingPower(I_REF, V_REF, 10000);
        assertEquals(15.0, power, TOLERANCE);
    }
    
    @Test
    public void testSwitchingPower_HighFrequency() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        // P_sw = 1.5mJ × 100kHz = 150 W
        double power = calc.calculateSwitchingPower(I_REF, V_REF, 100000);
        assertEquals(150.0, power, TOLERANCE);
    }
    
    // ===========================================
    // Instantaneous Power Tests
    // ===========================================
    
    @Test
    public void testInstantaneousPower_Calculation() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        // P = E / Δt = 1mJ / 1µs = 1000 W
        double power = calc.calculateInstantaneousPower(E_ON_REF, 1e-6);
        assertEquals(1000.0, power, TOLERANCE);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInstantaneousPower_ZeroTimeStep() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        calc.calculateInstantaneousPower(E_ON_REF, 0.0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInstantaneousPower_NegativeTimeStep() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        calc.calculateInstantaneousPower(E_ON_REF, -1e-6);
    }
    
    // ===========================================
    // Temperature Effect Tests
    // ===========================================
    
    @Test
    public void testTemperatureEffect_TurnOnEnergy() {
        // α = 0.003 per °C (typical for IGBT)
        SwitchingLossCalculator calc = new SwitchingLossCalculator(
            E_ON_REF, E_OFF_REF, I_REF, V_REF, 0.003);
        
        // At 125°C: E = E_ref × (1 + 0.003 × 100) = E_ref × 1.3
        double energy = calc.calculateTurnOnEnergy(I_REF, V_REF, 125.0);
        assertEquals(1.3 * E_ON_REF, energy, TOLERANCE);
    }
    
    @Test
    public void testTemperatureEffect_ReferenceTemperature() {
        SwitchingLossCalculator calc = new SwitchingLossCalculator(
            E_ON_REF, E_OFF_REF, I_REF, V_REF, 0.003);
        
        // At 25°C (reference), no temperature correction
        double energy = calc.calculateTurnOnEnergy(I_REF, V_REF, 25.0);
        assertEquals(E_ON_REF, energy, TOLERANCE);
    }
    
    @Test
    public void testTemperatureCoefficient_Getter() {
        SwitchingLossCalculator calc = new SwitchingLossCalculator(
            E_ON_REF, E_OFF_REF, I_REF, V_REF, 0.003);
        
        assertEquals(0.003, calc.getTemperatureCoefficient(), TOLERANCE);
    }
    
    // ===========================================
    // Turn-On/Off Detection Tests
    // ===========================================
    
    @Test
    public void testDetectTurnOn_FromZero() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        assertTrue(calc.detectTurnOn(0.0, 1.0));
    }
    
    @Test
    public void testDetectTurnOn_FromNearZero() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        assertTrue(calc.detectTurnOn(0.001, 1.0));  // 1mA < threshold
    }
    
    @Test
    public void testDetectTurnOn_NotDetected_AlreadyOn() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        assertFalse(calc.detectTurnOn(5.0, 10.0));  // Already conducting
    }
    
    @Test
    public void testDetectTurnOff_ToZero() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        assertTrue(calc.detectTurnOff(1.0, 0.0));
    }
    
    @Test
    public void testDetectTurnOff_ToNearZero() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        assertTrue(calc.detectTurnOff(1.0, 0.001));  // 1mA < threshold
    }
    
    @Test
    public void testDetectTurnOff_NotDetected_AlreadyOff() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        assertFalse(calc.detectTurnOff(0.0, 0.0));  // Already off
    }
    
    @Test
    public void testDetection_NegativeCurrent() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        // Turn-on with negative current (e.g., diode in reverse)
        assertTrue(calc.detectTurnOn(0.0, -1.0));
        
        // Turn-off from negative current
        assertTrue(calc.detectTurnOff(-1.0, 0.0));
    }
    
    // ===========================================
    // Validation Tests
    // ===========================================
    
    @Test(expected = IllegalArgumentException.class)
    public void testValidation_NegativeTurnOnEnergy() {
        SwitchingLossCalculator.fromEnergies(-1e-3, E_OFF_REF, I_REF, V_REF);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testValidation_NegativeTurnOffEnergy() {
        SwitchingLossCalculator.fromEnergies(E_ON_REF, -1e-3, I_REF, V_REF);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testValidation_ZeroReferenceCurrent() {
        SwitchingLossCalculator.fromEnergies(E_ON_REF, E_OFF_REF, 0.0, V_REF);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testValidation_NegativeReferenceVoltage() {
        SwitchingLossCalculator.fromEnergies(E_ON_REF, E_OFF_REF, I_REF, -600.0);
    }
    
    @Test
    public void testValidation_ZeroEnergy() {
        // Zero energy is valid (lossless switch)
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            0.0, 0.0, I_REF, V_REF);
        
        assertEquals(0.0, calc.calculateTotalEnergy(I_REF, V_REF), TOLERANCE);
    }
    
    // ===========================================
    // Real-World Component Tests
    // ===========================================
    
    @Test
    public void testRealWorld_IGBT600V() {
        // Infineon IGBT: E_on = 2.5 mJ, E_off = 1.5 mJ at 25A, 400V
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            2.5e-3, 1.5e-3, 25.0, 400.0);
        
        // At 50A, 600V: E = E_ref × (50/25) × (600/400) = E_ref × 3
        double E_on = calc.calculateTurnOnEnergy(50.0, 600.0);
        assertEquals(7.5e-3, E_on, 1e-6);  // 7.5 mJ
    }
    
    @Test
    public void testRealWorld_MOSFET() {
        // Typical MOSFET: E_on = E_off = 50 µJ at 10A, 400V
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            50e-6, 50e-6, 10.0, 400.0);
        
        // Switching power at 100 kHz, 10A, 400V: P = 100 µJ × 100 kHz = 10 W
        double power = calc.calculateSwitchingPower(10.0, 400.0, 100000);
        assertEquals(10.0, power, 1e-6);
    }
    
    @Test
    public void testRealWorld_SiCMOSFET() {
        // SiC MOSFET: much lower switching losses
        // E_on = 10 µJ, E_off = 5 µJ at 20A, 800V
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            10e-6, 5e-6, 20.0, 800.0);
        
        // Switching power at 200 kHz
        double power = calc.calculateSwitchingPower(20.0, 800.0, 200000);
        assertEquals(3.0, power, 1e-6);  // 15 µJ × 200 kHz = 3 W
    }
    
    // ===========================================
    // Numerical Stability Tests
    // ===========================================
    
    @Test
    public void testNumericalStability_VerySmallEnergy() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            1e-12, 1e-12, I_REF, V_REF);  // 1 pJ
        
        double energy = calc.calculateTurnOnEnergy(I_REF, V_REF);
        assertTrue(energy >= 0);
        assertFalse(Double.isNaN(energy));
    }
    
    @Test
    public void testNumericalStability_VeryHighVoltage() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        double energy = calc.calculateTurnOnEnergy(I_REF, 10000.0);  // 10 kV
        double expected = E_ON_REF * (10000.0 / V_REF);
        assertEquals(expected, energy, 1e-6);
    }
    
    @Test
    public void testNumericalStability_VeryHighCurrent() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        double energy = calc.calculateTurnOnEnergy(1000.0, V_REF);  // 1 kA
        double expected = E_ON_REF * (1000.0 / I_REF);
        assertEquals(expected, energy, 1e-6);
    }
    
    @Test
    public void testNumericalStability_VerySmallCurrent() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        double energy = calc.calculateTurnOnEnergy(1e-6, V_REF);  // 1 µA
        assertTrue(energy >= 0);
        assertFalse(Double.isNaN(energy));
    }
    
    // ===========================================
    // Edge Case Tests
    // ===========================================
    
    @Test
    public void testEdgeCase_NegativeCurrentScaling() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        // Energy uses absolute value of current
        double energyPos = calc.calculateTurnOnEnergy(10.0, V_REF);
        double energyNeg = calc.calculateTurnOnEnergy(-10.0, V_REF);
        assertEquals(energyPos, energyNeg, TOLERANCE);
    }
    
    @Test
    public void testEdgeCase_NegativeVoltageScaling() {
        SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(
            E_ON_REF, E_OFF_REF, I_REF, V_REF);
        
        // Energy uses absolute value of voltage
        double energyPos = calc.calculateTurnOnEnergy(I_REF, 400.0);
        double energyNeg = calc.calculateTurnOnEnergy(I_REF, -400.0);
        assertEquals(energyPos, energyNeg, TOLERANCE);
    }
}
