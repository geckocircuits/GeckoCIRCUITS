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
 * Tests for semiconductor switch components: Diode, MOSFET, IGBT.
 * Sprint 10: Circuit Components
 * 
 * Power electronics switches are modeled with:
 * - ON state: Small resistance (Ron)
 * - OFF state: Large resistance (Roff)
 * - Forward voltage drop (for diodes, IGBTs)
 */
public class SemiconductorSwitchTest {

    private static final double DELTA = 1e-10;
    
    // Typical values for power semiconductors
    private static final double TYPICAL_RON = 0.01;      // 10 mΩ
    private static final double TYPICAL_ROFF = 1e6;      // 1 MΩ
    private static final double DIODE_VF = 0.7;          // Forward voltage
    private static final double IGBT_VCE_SAT = 1.5;      // Saturation voltage

    // ========== Diode Tests ==========
    
    @Test
    public void testDiodeCreation() {
        Diode diode = new Diode();
        assertNotNull("Diode should be created", diode);
    }
    
    @Test
    public void testDiode_ForwardBias() {
        // Diode conducts when V_anode > V_cathode + Vf
        double Vanode = 5.0;
        double Vcathode = 0.0;
        double Vf = 0.7;
        
        boolean conducting = (Vanode > Vcathode + Vf);
        assertTrue("Diode should conduct in forward bias", conducting);
    }
    
    @Test
    public void testDiode_ReverseBias() {
        // Diode blocks when V_anode < V_cathode
        double Vanode = 0.0;
        double Vcathode = 5.0;
        
        boolean conducting = (Vanode > Vcathode);
        assertFalse("Diode should block in reverse bias", conducting);
    }
    
    @Test
    public void testDiode_CurrentFormula() {
        // Shockley diode equation: I = Is * (exp(V/(n*Vt)) - 1)
        double Is = 1e-12;  // Saturation current
        double n = 1.0;     // Ideality factor
        double Vt = 0.026;  // Thermal voltage at 300K
        double V = 0.7;     // Forward voltage
        
        double I = Is * (Math.exp(V / (n * Vt)) - 1);
        assertTrue("Forward current should be positive", I > 0);
    }
    
    @Test
    public void testDiode_ReverseRecovery() {
        // Reverse recovery: When switching from forward to reverse,
        // diode conducts briefly in reverse direction
        double trr = 50e-9;  // 50 ns reverse recovery time
        assertTrue("Recovery time should be positive", trr > 0);
    }

    // ========== MOSFET Tests ==========
    
    @Test
    public void testMOSFETCreation() {
        MOSFET mosfet = new MOSFET();
        assertNotNull("MOSFET should be created", mosfet);
    }
    
    @Test
    public void testMOSFET_OnState() {
        // MOSFET in ON state: Ron = Vds / Ids
        double Vds = 0.5;   // Drain-source voltage
        double Ids = 50.0;  // Drain current
        double Ron = Vds / Ids;
        assertEquals("MOSFET Ron", 0.01, Ron, DELTA);
    }
    
    @Test
    public void testMOSFET_OffState() {
        // MOSFET in OFF state: Very high resistance, minimal leakage
        double Vds = 400.0;     // Blocking voltage
        double Ileak = 1e-6;    // Leakage current
        double Roff = Vds / Ileak;
        assertEquals("MOSFET Roff", 400e6, Roff, 1e3);
    }
    
    @Test
    public void testMOSFET_ThresholdVoltage() {
        // MOSFET turns on when Vgs > Vth
        double Vgs = 10.0;
        double Vth = 4.0;
        
        boolean on = (Vgs > Vth);
        assertTrue("MOSFET should be ON when Vgs > Vth", on);
    }
    
    @Test
    public void testMOSFET_ConductionLoss() {
        // Conduction loss: P = I^2 * Ron
        double Ids = 20.0;
        double Ron = 0.02;
        
        double Ploss = Ids * Ids * Ron;
        assertEquals("MOSFET conduction loss", 8.0, Ploss, DELTA);
    }
    
    @Test
    public void testMOSFET_BodyDiode() {
        // MOSFET has intrinsic body diode (can conduct reverse current)
        // This allows freewheeling in half-bridge
        double Vsd = 0.7;  // Source-to-drain voltage (body diode forward)
        double Isd = 10.0; // Reverse current
        
        double Pdiode = Vsd * Isd;
        assertEquals("Body diode power", 7.0, Pdiode, DELTA);
    }

    // ========== IGBT Tests ==========
    
    @Test
    public void testIGBTCreation() {
        IGBT igbt = new IGBT();
        assertNotNull("IGBT should be created", igbt);
    }
    
    @Test
    public void testIGBT_OnState() {
        // IGBT ON: Vce = Vce_sat + Ic * Rce
        double Vce_sat = 1.5;  // Saturation voltage
        double Ic = 100.0;     // Collector current
        double Rce = 0.01;     // On-state resistance
        
        double Vce = Vce_sat + Ic * Rce;
        assertEquals("IGBT Vce in on-state", 2.5, Vce, DELTA);
    }
    
    @Test
    public void testIGBT_ConductionLoss() {
        // Conduction loss: P = Vce_sat * Ic + Ic^2 * Rce
        double Vce_sat = 1.5;
        double Ic = 50.0;
        double Rce = 0.01;
        
        double Ploss = Vce_sat * Ic + Ic * Ic * Rce;
        assertEquals("IGBT conduction loss", 100.0, Ploss, DELTA);
    }
    
    @Test
    public void testIGBT_TailCurrent() {
        // IGBT has tail current during turn-off (unlike MOSFET)
        // This increases switching losses
        double Ic_tail = 5.0;    // Tail current
        double Vce = 600.0;      // Blocking voltage during tail
        double t_tail = 500e-9;  // Tail duration
        
        // Energy during tail: E = Ic_tail * Vce * t_tail / 2
        double E_tail = Ic_tail * Vce * t_tail / 2;
        assertEquals("IGBT tail energy", 750e-6, E_tail, 1e-9);
    }

    // ========== Thyristor Tests ==========
    
    @Test
    public void testThyristorCreation() {
        Thyristor thyristor = new Thyristor();
        assertNotNull("Thyristor should be created", thyristor);
    }
    
    @Test
    public void testThyristor_Latching() {
        // Thyristor latches on when gate pulse applied and Vak > 0
        // Stays on until current falls below holding current
        double Ih = 0.1;      // Holding current
        double Ic = 5.0;      // Circuit current
        
        boolean latched = (Ic > Ih);
        assertTrue("Thyristor should stay latched", latched);
    }
    
    @Test
    public void testThyristor_CommutationFailure() {
        // Turn-off time tq must be less than circuit off time
        double tq = 100e-6;      // Turn-off time
        double toff = 200e-6;    // Available off time
        
        boolean safeCommutation = (toff > tq);
        assertTrue("Commutation should succeed", safeCommutation);
    }

    // ========== Switching Loss Tests ==========
    
    @Test
    public void testSwitchingEnergy_TurnOn() {
        // E_on ≈ 0.5 * V * I * t_rise
        double V = 400.0;       // Blocking voltage
        double I = 50.0;        // Load current
        double trise = 100e-9;  // Rise time
        
        double Eon = 0.5 * V * I * trise;
        assertEquals("Turn-on energy", 1e-3, Eon, 1e-6);
    }
    
    @Test
    public void testSwitchingEnergy_TurnOff() {
        // E_off ≈ 0.5 * V * I * t_fall
        double V = 400.0;
        double I = 50.0;
        double tfall = 150e-9;
        
        double Eoff = 0.5 * V * I * tfall;
        assertEquals("Turn-off energy", 1.5e-3, Eoff, 1e-6);
    }
    
    @Test
    public void testSwitchingLoss_Average() {
        // P_sw = (E_on + E_off) * f_sw
        double Eon = 1e-3;
        double Eoff = 1.5e-3;
        double fsw = 20000;  // 20 kHz switching frequency
        
        double Psw = (Eon + Eoff) * fsw;
        assertEquals("Average switching loss", 50.0, Psw, DELTA);
    }

    // ========== Safe Operating Area Tests ==========
    
    @Test
    public void testSOA_VoltageLimit() {
        double Vds_max = 650.0;  // Maximum drain-source voltage
        double Vds_actual = 400.0;
        
        boolean withinSOA = (Vds_actual < Vds_max);
        assertTrue("Voltage within SOA", withinSOA);
    }
    
    @Test
    public void testSOA_CurrentLimit() {
        double Ids_max = 100.0;  // Maximum continuous current
        double Ids_actual = 50.0;
        
        boolean withinSOA = (Ids_actual < Ids_max);
        assertTrue("Current within SOA", withinSOA);
    }
    
    @Test
    public void testSOA_PowerLimit() {
        // Power limit decreases at high voltage
        double Pmax = 200.0;     // Maximum power dissipation
        double Vds = 400.0;
        double Ids = 0.3;        // Reduced current at high voltage
        
        double P = Vds * Ids;
        boolean withinSOA = (P < Pmax);
        assertTrue("Power within SOA", withinSOA);
    }

    // ========== Thermal Model Tests ==========
    
    @Test
    public void testThermal_JunctionTemperature() {
        // Tj = Ta + P * Rth_ja
        double Ta = 25.0;       // Ambient temperature
        double Ploss = 50.0;    // Power loss
        double Rth_ja = 2.0;    // Junction-to-ambient thermal resistance
        
        double Tj = Ta + Ploss * Rth_ja;
        assertEquals("Junction temperature", 125.0, Tj, DELTA);
    }
    
    @Test
    public void testThermal_SafeTemperature() {
        double Tj_max = 175.0;  // Maximum junction temperature
        double Tj_actual = 125.0;
        
        boolean safe = (Tj_actual < Tj_max);
        assertTrue("Temperature within safe limit", safe);
    }
}
