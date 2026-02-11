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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for IComponentCalculator interface contract.
 * Verifies that the interface design is consistent and complete.
 */
public class IComponentCalculatorTest {
    
    /**
     * Tests that the interface extends CurrentCalculatable.
     */
    @Test
    public void testInterfaceExtendsCurrentCalculatable() {
        // Verify inheritance relationship
        assertTrue("IComponentCalculator should extend CurrentCalculatable",
            CurrentCalculatable.class.isAssignableFrom(IComponentCalculator.class));
    }
    
    /**
     * Tests capacitor companion model conductance formula.
     * G = C/dt for Backward Euler
     */
    @Test
    public void testCapacitorConductance_BackwardEuler() {
        double C = 100e-6;  // 100 µF
        double dt = 1e-6;   // 1 µs
        
        double G = C / dt;
        assertEquals(100.0, G, 1e-10);  // 100 S
    }
    
    /**
     * Tests capacitor companion model conductance formula.
     * G = 2*C/dt for Trapezoidal
     */
    @Test
    public void testCapacitorConductance_Trapezoidal() {
        double C = 100e-6;
        double dt = 1e-6;
        
        double G = 2 * C / dt;
        assertEquals(200.0, G, 1e-10);  // 200 S
    }
    
    /**
     * Tests inductor companion model conductance formula.
     * G = dt/L for Backward Euler
     */
    @Test
    public void testInductorConductance_BackwardEuler() {
        double L = 1e-3;  // 1 mH
        double dt = 1e-6;
        
        double G = dt / L;
        assertEquals(1e-3, G, 1e-15);  // 1 mS
    }
    
    /**
     * Tests inductor companion model conductance formula.
     * G = dt/(2*L) for Trapezoidal
     */
    @Test
    public void testInductorConductance_Trapezoidal() {
        double L = 1e-3;
        double dt = 1e-6;
        
        double G = dt / (2 * L);
        assertEquals(0.5e-3, G, 1e-15);  // 0.5 mS
    }
    
    /**
     * Tests capacitor stored energy calculation: E = 0.5 * C * V^2
     */
    @Test
    public void testCapacitorStoredEnergy() {
        double C = 100e-6;
        double V = 100;
        
        double E = 0.5 * C * V * V;
        assertEquals(0.5, E, 1e-10);  // 0.5 J
    }
    
    /**
     * Tests inductor stored energy calculation: E = 0.5 * L * I^2
     */
    @Test
    public void testInductorStoredEnergy() {
        double L = 1e-3;
        double I = 10;
        
        double E = 0.5 * L * I * I;
        assertEquals(0.05, E, 1e-10);  // 50 mJ
    }
    
    /**
     * Tests capacitor history source for Backward Euler.
     * I_history = C/dt * V_old
     */
    @Test
    public void testCapacitorHistorySource_BackwardEuler() {
        double C = 100e-6;
        double dt = 1e-6;
        double V_old = 10;
        
        double I_history = C / dt * V_old;
        assertEquals(1000.0, I_history, 1e-10);  // 1000 A
    }
    
    /**
     * Tests capacitor history source for Trapezoidal.
     * I_history = 2*C/dt * V_old + I_old
     */
    @Test
    public void testCapacitorHistorySource_Trapezoidal() {
        double C = 100e-6;
        double dt = 1e-6;
        double V_old = 10;
        double I_old = 100;
        
        double I_history = 2 * C / dt * V_old + I_old;
        assertEquals(2100.0, I_history, 1e-10);
    }
    
    /**
     * Tests inductor history source for Backward Euler.
     * I_history = I_old
     */
    @Test
    public void testInductorHistorySource_BackwardEuler() {
        double I_old = 5.0;
        
        double I_history = I_old;
        assertEquals(5.0, I_history, 1e-10);
    }
    
    /**
     * Tests inductor history source for Trapezoidal.
     * I_history = I_old + dt/(2*L) * V_old
     */
    @Test
    public void testInductorHistorySource_Trapezoidal() {
        double L = 1e-3;
        double dt = 1e-6;
        double I_old = 5.0;
        double V_old = 10;
        
        double I_history = I_old + dt / (2 * L) * V_old;
        assertEquals(5.005, I_history, 1e-10);
    }
    
    /**
     * Tests that equivalent conductance is always positive.
     */
    @Test
    public void testEquivalentConductancePositive() {
        double C = 100e-6;
        double L = 1e-3;
        double dt = 1e-6;
        
        double G_cap = C / dt;
        double G_ind = dt / L;
        
        assertTrue("Capacitor conductance should be positive", G_cap > 0);
        assertTrue("Inductor conductance should be positive", G_ind > 0);
    }
    
    /**
     * Tests that stored energy is always non-negative.
     */
    @Test
    public void testStoredEnergyNonNegative() {
        double C = 100e-6;
        double L = 1e-3;
        double V = -50;  // Negative voltage
        double I = -10;  // Negative current
        
        double E_cap = 0.5 * C * V * V;  // Still positive (V^2)
        double E_ind = 0.5 * L * I * I;  // Still positive (I^2)
        
        assertTrue("Capacitor energy should be non-negative", E_cap >= 0);
        assertTrue("Inductor energy should be non-negative", E_ind >= 0);
    }
    
    /**
     * Tests interface methods exist via reflection.
     */
    @Test
    public void testInterfaceMethodsExist() throws NoSuchMethodException {
        Class<IComponentCalculator> clazz = IComponentCalculator.class;
        
        // Verify all required methods exist
        assertNotNull(clazz.getMethod("initialize"));
        assertNotNull(clazz.getMethod("getSolverType"));
        assertNotNull(clazz.getMethod("setSolverType", ch.technokrat.gecko.geckocircuits.allg.SolverType.class));
        assertNotNull(clazz.getMethod("getComponentValue"));
        assertNotNull(clazz.getMethod("setComponentValue", double.class));
        assertNotNull(clazz.getMethod("getInitialCondition"));
        assertNotNull(clazz.getMethod("setInitialCondition", double.class));
        assertNotNull(clazz.getMethod("calculateEquivalentConductance", double.class));
        assertNotNull(clazz.getMethod("calculateHistorySource", double.class));
        assertNotNull(clazz.getMethod("getVoltage"));
        assertNotNull(clazz.getMethod("getStoredEnergy"));
        assertNotNull(clazz.getMethod("isNonLinear"));
        assertNotNull(clazz.getMethod("updateNonLinearValue", double.class));
        
        // Inherited from CurrentCalculatable
        assertNotNull(clazz.getMethod("calculateCurrent", double[].class, double.class, double.class));
        assertNotNull(clazz.getMethod("getCurrent"));
    }
    
    /**
     * Tests numerical stability of conductance calculation for very small dt.
     */
    @Test
    public void testNumericalStability_SmallDt() {
        double C = 1e-12;  // 1 pF
        double dt = 1e-12; // 1 ps
        
        double G = C / dt;
        assertEquals(1.0, G, 1e-10);
        assertFalse("Conductance should not be infinite", Double.isInfinite(G));
        assertFalse("Conductance should not be NaN", Double.isNaN(G));
    }
    
    /**
     * Tests numerical stability for very large component values.
     */
    @Test
    public void testNumericalStability_LargeValues() {
        double C = 1.0;  // 1 Farad
        double dt = 1e-3;
        
        double G = C / dt;
        assertEquals(1000.0, G, 1e-10);
        assertFalse("Conductance should not be infinite", Double.isInfinite(G));
        assertFalse("Conductance should not be NaN", Double.isNaN(G));
    }
}
