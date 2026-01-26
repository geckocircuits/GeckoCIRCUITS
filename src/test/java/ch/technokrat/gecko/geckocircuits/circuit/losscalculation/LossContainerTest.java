/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for LossContainer - container for semiconductor loss results.
 * LossContainer packages conduction and switching losses separately.
 */
public class LossContainerTest {

    private static final double DELTA = 1e-10;

    // ====================================================
    // Constructor and Basic Getters Tests
    // ====================================================

    @Test
    public void testConstructor_BasicValues() {
        LossContainer container = new LossContainer(5.0, 3.0);
        
        assertEquals(5.0, container.getConductionLosses(), DELTA);
        assertEquals(3.0, container.getSwitchingLosses(), DELTA);
    }

    @Test
    public void testConstructor_ZeroValues() {
        LossContainer container = new LossContainer(0.0, 0.0);
        
        assertEquals(0.0, container.getConductionLosses(), DELTA);
        assertEquals(0.0, container.getSwitchingLosses(), DELTA);
    }

    @Test
    public void testConstructor_OnlyConductionLosses() {
        LossContainer container = new LossContainer(10.0, 0.0);
        
        assertEquals(10.0, container.getConductionLosses(), DELTA);
        assertEquals(0.0, container.getSwitchingLosses(), DELTA);
    }

    @Test
    public void testConstructor_OnlySwitchingLosses() {
        LossContainer container = new LossContainer(0.0, 7.5);
        
        assertEquals(0.0, container.getConductionLosses(), DELTA);
        assertEquals(7.5, container.getSwitchingLosses(), DELTA);
    }

    // ====================================================
    // Total Losses Tests
    // ====================================================

    @Test
    public void testTotalLosses_Sum() {
        LossContainer container = new LossContainer(5.0, 3.0);
        
        assertEquals(8.0, container.getTotalLosses(), DELTA);
    }

    @Test
    public void testTotalLosses_ZeroConduction() {
        LossContainer container = new LossContainer(0.0, 10.0);
        
        assertEquals(10.0, container.getTotalLosses(), DELTA);
    }

    @Test
    public void testTotalLosses_ZeroSwitching() {
        LossContainer container = new LossContainer(10.0, 0.0);
        
        assertEquals(10.0, container.getTotalLosses(), DELTA);
    }

    @Test
    public void testTotalLosses_BothZero() {
        LossContainer container = new LossContainer(0.0, 0.0);
        
        assertEquals(0.0, container.getTotalLosses(), DELTA);
    }

    // ====================================================
    // Physical Value Tests (Semiconductor Loss Scenarios)
    // ====================================================

    @Test
    public void testTypicalDiodeLosses() {
        // Diode: mostly conduction, some reverse recovery switching
        double conductionLoss = 15.0;   // I²R + Vf*I
        double switchingLoss = 2.0;     // Reverse recovery
        
        LossContainer diode = new LossContainer(conductionLoss, switchingLoss);
        
        assertEquals(17.0, diode.getTotalLosses(), DELTA);
        assertTrue(diode.getConductionLosses() > diode.getSwitchingLosses());
    }

    @Test
    public void testTypicalMOSFETLosses_HighFrequency() {
        // High frequency MOSFET: significant switching losses
        double conductionLoss = 5.0;    // Rds(on) * I²
        double switchingLoss = 15.0;    // High frequency switching
        
        LossContainer mosfet = new LossContainer(conductionLoss, switchingLoss);
        
        assertEquals(20.0, mosfet.getTotalLosses(), DELTA);
        assertTrue(mosfet.getSwitchingLosses() > mosfet.getConductionLosses());
    }

    @Test
    public void testTypicalIGBTLosses_PowerInverter() {
        // IGBT in power inverter: balanced losses
        double conductionLoss = 50.0;   // Vce(sat) * I
        double switchingLoss = 40.0;    // Turn-on + Turn-off
        
        LossContainer igbt = new LossContainer(conductionLoss, switchingLoss);
        
        assertEquals(90.0, igbt.getTotalLosses(), DELTA);
    }

    // ====================================================
    // Edge Cases
    // ====================================================

    @Test
    public void testVerySmallLosses() {
        LossContainer container = new LossContainer(1e-10, 1e-10);
        
        assertEquals(2e-10, container.getTotalLosses(), 1e-15);
    }

    @Test
    public void testLargeLosses() {
        // 1 MW losses
        LossContainer container = new LossContainer(500000.0, 500000.0);
        
        assertEquals(1000000.0, container.getTotalLosses(), DELTA);
    }

    @Test
    public void testPrecisionWithDifferentMagnitudes() {
        // Very different magnitudes
        LossContainer container = new LossContainer(0.001, 1000.0);
        
        assertEquals(1000.001, container.getTotalLosses(), 1e-6);
    }

    // ====================================================
    // Immutability Tests
    // ====================================================

    @Test
    public void testImmutability_ValuesUnchanged() {
        LossContainer container = new LossContainer(5.0, 3.0);
        
        // Call getters multiple times
        container.getTotalLosses();
        container.getConductionLosses();
        container.getSwitchingLosses();
        
        // Values should be unchanged
        assertEquals(5.0, container.getConductionLosses(), DELTA);
        assertEquals(3.0, container.getSwitchingLosses(), DELTA);
    }

    // ====================================================
    // Percentage Breakdown Tests
    // ====================================================

    @Test
    public void testPercentageBreakdown_EqualSplit() {
        LossContainer container = new LossContainer(5.0, 5.0);
        
        double total = container.getTotalLosses();
        double conductionPercent = container.getConductionLosses() / total * 100;
        double switchingPercent = container.getSwitchingLosses() / total * 100;
        
        assertEquals(50.0, conductionPercent, DELTA);
        assertEquals(50.0, switchingPercent, DELTA);
    }

    @Test
    public void testPercentageBreakdown_ConductionDominant() {
        LossContainer container = new LossContainer(90.0, 10.0);
        
        double total = container.getTotalLosses();
        double conductionPercent = container.getConductionLosses() / total * 100;
        
        assertEquals(90.0, conductionPercent, DELTA);
    }

    @Test
    public void testPercentageBreakdown_SwitchingDominant() {
        LossContainer container = new LossContainer(10.0, 90.0);
        
        double total = container.getTotalLosses();
        double switchingPercent = container.getSwitchingLosses() / total * 100;
        
        assertEquals(90.0, switchingPercent, DELTA);
    }
}
