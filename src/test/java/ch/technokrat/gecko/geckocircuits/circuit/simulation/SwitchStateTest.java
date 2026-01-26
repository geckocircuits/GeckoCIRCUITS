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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for SwitchState enum.
 * Tests state properties, transitions, and conversion methods.
 */
public class SwitchStateTest {
    
    // ===== Basic State Property Tests =====
    
    @Test
    public void testOnStateIsConducting() {
        assertTrue(SwitchState.ON.isConducting());
        assertFalse(SwitchState.ON.isBlocking());
    }
    
    @Test
    public void testOffStateIsBlocking() {
        assertFalse(SwitchState.OFF.isConducting());
        assertTrue(SwitchState.OFF.isBlocking());
    }
    
    @Test
    public void testTurningOnIsBlocking() {
        // Still blocking during turn-on transition
        assertFalse(SwitchState.TURNING_ON.isConducting());
        assertTrue(SwitchState.TURNING_ON.isBlocking());
    }
    
    @Test
    public void testTurningOffIsConducting() {
        // Still conducting during turn-off transition
        assertTrue(SwitchState.TURNING_OFF.isConducting());
        assertFalse(SwitchState.TURNING_OFF.isBlocking());
    }
    
    // ===== Stable vs Transitioning Tests =====
    
    @Test
    public void testOnIsStable() {
        assertTrue(SwitchState.ON.isStable());
        assertFalse(SwitchState.ON.isTransitioning());
    }
    
    @Test
    public void testOffIsStable() {
        assertTrue(SwitchState.OFF.isStable());
        assertFalse(SwitchState.OFF.isTransitioning());
    }
    
    @Test
    public void testTurningOnIsTransitioning() {
        assertFalse(SwitchState.TURNING_ON.isStable());
        assertTrue(SwitchState.TURNING_ON.isTransitioning());
    }
    
    @Test
    public void testTurningOffIsTransitioning() {
        assertFalse(SwitchState.TURNING_OFF.isStable());
        assertTrue(SwitchState.TURNING_OFF.isTransitioning());
    }
    
    // ===== Opposite State Tests =====
    
    @Test
    public void testOnOppositeIsOff() {
        assertEquals(SwitchState.OFF, SwitchState.ON.getOpposite());
    }
    
    @Test
    public void testOffOppositeIsOn() {
        assertEquals(SwitchState.ON, SwitchState.OFF.getOpposite());
    }
    
    @Test
    public void testTurningOnOppositeReturnsToOff() {
        assertEquals(SwitchState.OFF, SwitchState.TURNING_ON.getOpposite());
    }
    
    @Test
    public void testTurningOffOppositeReturnsToOn() {
        assertEquals(SwitchState.ON, SwitchState.TURNING_OFF.getOpposite());
    }
    
    // ===== Target State Tests =====
    
    @Test
    public void testOnTargetIsSelf() {
        assertEquals(SwitchState.ON, SwitchState.ON.getTargetState());
    }
    
    @Test
    public void testOffTargetIsSelf() {
        assertEquals(SwitchState.OFF, SwitchState.OFF.getTargetState());
    }
    
    @Test
    public void testTurningOnTargetIsOn() {
        assertEquals(SwitchState.ON, SwitchState.TURNING_ON.getTargetState());
    }
    
    @Test
    public void testTurningOffTargetIsOff() {
        assertEquals(SwitchState.OFF, SwitchState.TURNING_OFF.getTargetState());
    }
    
    // ===== fromConducting Tests =====
    
    @Test
    public void testFromConductingTrue() {
        assertEquals(SwitchState.ON, SwitchState.fromConducting(true));
    }
    
    @Test
    public void testFromConductingFalse() {
        assertEquals(SwitchState.OFF, SwitchState.fromConducting(false));
    }
    
    // ===== fromGateSignal Tests =====
    
    @Test
    public void testFromGateSignalHigh() {
        assertEquals(SwitchState.ON, SwitchState.fromGateSignal(1.0));
        assertEquals(SwitchState.ON, SwitchState.fromGateSignal(0.6));
        assertEquals(SwitchState.ON, SwitchState.fromGateSignal(0.51));
    }
    
    @Test
    public void testFromGateSignalLow() {
        assertEquals(SwitchState.OFF, SwitchState.fromGateSignal(0.0));
        assertEquals(SwitchState.OFF, SwitchState.fromGateSignal(0.4));
        assertEquals(SwitchState.OFF, SwitchState.fromGateSignal(0.5)); // Exactly 0.5 is OFF
    }
    
    @Test
    public void testFromGateSignalThreshold() {
        // Test around 0.5 threshold (legacy GeckoCIRCUITS uses > 0.5)
        assertEquals(SwitchState.OFF, SwitchState.fromGateSignal(0.5));
        assertEquals(SwitchState.ON, SwitchState.fromGateSignal(0.500001));
    }
    
    // ===== fromResistance Tests =====
    
    @Test
    public void testFromResistanceLow() {
        double rOn = 0.01;
        double rOff = 1e6;
        assertEquals(SwitchState.ON, SwitchState.fromResistance(0.01, rOn, rOff));
        assertEquals(SwitchState.ON, SwitchState.fromResistance(0.1, rOn, rOff));
    }
    
    @Test
    public void testFromResistanceHigh() {
        double rOn = 0.01;
        double rOff = 1e6;
        assertEquals(SwitchState.OFF, SwitchState.fromResistance(1e6, rOn, rOff));
        assertEquals(SwitchState.OFF, SwitchState.fromResistance(1e4, rOn, rOff));
    }
    
    @Test
    public void testFromResistanceThreshold() {
        double rOn = 0.01;
        double rOff = 1e6;
        // Geometric mean = sqrt(0.01 * 1e6) = sqrt(1e4) = 100
        assertEquals(SwitchState.ON, SwitchState.fromResistance(99, rOn, rOff));
        assertEquals(SwitchState.OFF, SwitchState.fromResistance(101, rOn, rOff));
    }
    
    // ===== fromGateStatus Tests =====
    
    @Test
    public void testFromGateStatusOn() {
        assertEquals(SwitchState.ON, SwitchState.fromGateStatus(1.0));
        assertEquals(SwitchState.ON, SwitchState.fromGateStatus(1.5));
    }
    
    @Test
    public void testFromGateStatusOff() {
        assertEquals(SwitchState.OFF, SwitchState.fromGateStatus(0.0));
        assertEquals(SwitchState.OFF, SwitchState.fromGateStatus(0.9));
    }
    
    // ===== toResistance Tests =====
    
    @Test
    public void testToResistanceOn() {
        double rOn = 0.01;
        double rOff = 1e6;
        assertEquals(rOn, SwitchState.ON.toResistance(rOn, rOff), 1e-10);
    }
    
    @Test
    public void testToResistanceOff() {
        double rOn = 0.01;
        double rOff = 1e6;
        assertEquals(rOff, SwitchState.OFF.toResistance(rOn, rOff), 1e-10);
    }
    
    @Test
    public void testToResistanceTurningOn() {
        double rOn = 0.01;
        double rOff = 1e6;
        // TURNING_ON is blocking, so should return rOff
        assertEquals(rOff, SwitchState.TURNING_ON.toResistance(rOn, rOff), 1e-10);
    }
    
    @Test
    public void testToResistanceTurningOff() {
        double rOn = 0.01;
        double rOff = 1e6;
        // TURNING_OFF is conducting, so should return rOn
        assertEquals(rOn, SwitchState.TURNING_OFF.toResistance(rOn, rOff), 1e-10);
    }
    
    // ===== toGateStatus Tests =====
    
    @Test
    public void testToGateStatusOn() {
        assertEquals(1.0, SwitchState.ON.toGateStatus(), 1e-10);
    }
    
    @Test
    public void testToGateStatusOff() {
        assertEquals(0.0, SwitchState.OFF.toGateStatus(), 1e-10);
    }
    
    @Test
    public void testToGateStatusTurningOn() {
        assertEquals(0.0, SwitchState.TURNING_ON.toGateStatus(), 1e-10);
    }
    
    @Test
    public void testToGateStatusTurningOff() {
        assertEquals(1.0, SwitchState.TURNING_OFF.toGateStatus(), 1e-10);
    }
    
    // ===== Display Name Tests =====
    
    @Test
    public void testDisplayNames() {
        assertEquals("Conducting", SwitchState.ON.getDisplayName());
        assertEquals("Blocking", SwitchState.OFF.getDisplayName());
        assertEquals("Turning On", SwitchState.TURNING_ON.getDisplayName());
        assertEquals("Turning Off", SwitchState.TURNING_OFF.getDisplayName());
    }
    
    @Test
    public void testToString() {
        assertEquals("Conducting", SwitchState.ON.toString());
        assertEquals("Blocking", SwitchState.OFF.toString());
    }
    
    // ===== All States Enumeration Tests =====
    
    @Test
    public void testAllStatesExist() {
        SwitchState[] states = SwitchState.values();
        assertEquals(4, states.length);
    }
    
    @Test
    public void testValueOf() {
        assertEquals(SwitchState.ON, SwitchState.valueOf("ON"));
        assertEquals(SwitchState.OFF, SwitchState.valueOf("OFF"));
        assertEquals(SwitchState.TURNING_ON, SwitchState.valueOf("TURNING_ON"));
        assertEquals(SwitchState.TURNING_OFF, SwitchState.valueOf("TURNING_OFF"));
    }
    
    // ===== Integration Tests =====
    
    @Test
    public void testTypicalSwitchingCycle() {
        // Start OFF
        SwitchState state = SwitchState.OFF;
        assertTrue(state.isBlocking());
        
        // Gate signal goes high -> ON
        state = SwitchState.fromGateSignal(1.0);
        assertEquals(SwitchState.ON, state);
        assertTrue(state.isConducting());
        
        // Gate signal goes low -> OFF
        state = SwitchState.fromGateSignal(0.0);
        assertEquals(SwitchState.OFF, state);
        assertTrue(state.isBlocking());
    }
    
    @Test
    public void testResistanceRoundTrip() {
        double rOn = 0.01;
        double rOff = 1e6;
        
        // ON state -> resistance -> back to state
        double r = SwitchState.ON.toResistance(rOn, rOff);
        SwitchState recovered = SwitchState.fromResistance(r, rOn, rOff);
        assertEquals(SwitchState.ON, recovered);
        
        // OFF state -> resistance -> back to state
        r = SwitchState.OFF.toResistance(rOn, rOff);
        recovered = SwitchState.fromResistance(r, rOn, rOff);
        assertEquals(SwitchState.OFF, recovered);
    }
    
    @Test
    public void testLegacyParameterArrayCompatibility() {
        // Simulate legacy par[8] usage from SimulationsKern
        double par8 = 1.0; // Gate ON
        assertEquals(SwitchState.ON, SwitchState.fromGateStatus(par8));
        
        par8 = 0.0; // Gate OFF
        assertEquals(SwitchState.OFF, SwitchState.fromGateStatus(par8));
    }
}
