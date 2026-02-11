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
package ch.technokrat.gecko.geckocircuits.circuit.netlist;

import ch.technokrat.gecko.geckocircuits.circuit.netlist.MutualCouplingRegistry.Coupling;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

/**
 * Unit tests for MutualCouplingRegistry.
 * Tests mutual inductance coupling management and calculations.
 */
public class MutualCouplingRegistryTest {
    
    private MutualCouplingRegistry registry;
    
    // Test inductance values
    private static final double L1 = 1e-3;  // 1 mH
    private static final double L2 = 2e-3;  // 2 mH
    private static final double L3 = 4e-3;  // 4 mH
    private static final double K = 0.9;    // 90% coupling
    
    private static final double TOLERANCE = 1e-12;
    
    @Before
    public void setUp() {
        registry = new MutualCouplingRegistry();
    }
    
    // ===== Registration Tests =====
    
    @Test
    public void testRegisterCoupling() {
        Coupling coupling = registry.registerCoupling(0, 1, K, L1, L2);
        
        assertNotNull(coupling);
        assertEquals(0, coupling.getInductor1Index());
        assertEquals(1, coupling.getInductor2Index());
        assertEquals(K, coupling.getCouplingCoefficient(), TOLERANCE);
    }
    
    @Test
    public void testMutualInductanceCalculation() {
        Coupling coupling = registry.registerCoupling(0, 1, K, L1, L2);
        
        // M = k * sqrt(L1 * L2) = 0.9 * sqrt(1e-3 * 2e-3)
        double expectedM = K * Math.sqrt(L1 * L2);
        assertEquals(expectedM, coupling.getMutualInductance(), TOLERANCE);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterSelfCoupling() {
        registry.registerCoupling(0, 0, K, L1, L2);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNegativeIndex1() {
        registry.registerCoupling(-1, 1, K, L1, L2);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNegativeIndex2() {
        registry.registerCoupling(0, -1, K, L1, L2);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNaNCoefficient() {
        registry.registerCoupling(0, 1, Double.NaN, L1, L2);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterInfiniteCoefficient() {
        registry.registerCoupling(0, 1, Double.POSITIVE_INFINITY, L1, L2);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterZeroInductance() {
        registry.registerCoupling(0, 1, K, 0, L2);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNegativeInductance() {
        registry.registerCoupling(0, 1, K, -L1, L2);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterDuplicateCoupling() {
        registry.registerCoupling(0, 1, K, L1, L2);
        registry.registerCoupling(0, 1, K, L1, L2); // Duplicate
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterDuplicateCouplingReversed() {
        registry.registerCoupling(0, 1, K, L1, L2);
        registry.registerCoupling(1, 0, K, L2, L1); // Same coupling, reversed order
    }
    
    // ===== Coupling Lookup Tests =====
    
    @Test
    public void testGetCouplingsFor() {
        registry.registerCoupling(0, 1, K, L1, L2);
        registry.registerCoupling(0, 2, K, L1, L3);
        
        List<Coupling> couplings0 = registry.getCouplingsFor(0);
        assertEquals(2, couplings0.size());
        
        List<Coupling> couplings1 = registry.getCouplingsFor(1);
        assertEquals(1, couplings1.size());
        
        List<Coupling> couplings3 = registry.getCouplingsFor(3);
        assertTrue(couplings3.isEmpty());
    }
    
    @Test
    public void testGetCouplingsForUnmodifiable() {
        registry.registerCoupling(0, 1, K, L1, L2);
        List<Coupling> couplings = registry.getCouplingsFor(0);
        
        try {
            couplings.add(new Coupling(2, 3, K, L2, L3));
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }
    
    @Test
    public void testGetCouplingPartners() {
        registry.registerCoupling(0, 1, K, L1, L2);
        registry.registerCoupling(0, 2, K, L1, L3);
        registry.registerCoupling(1, 2, K, L2, L3);
        
        List<Integer> partners0 = registry.getCouplingPartners(0);
        assertEquals(2, partners0.size());
        assertTrue(partners0.contains(1));
        assertTrue(partners0.contains(2));
        
        List<Integer> partners1 = registry.getCouplingPartners(1);
        assertEquals(2, partners1.size());
        assertTrue(partners1.contains(0));
        assertTrue(partners1.contains(2));
    }
    
    @Test
    public void testGetCouplingPartnersNoCoupling() {
        List<Integer> partners = registry.getCouplingPartners(99);
        assertTrue(partners.isEmpty());
    }
    
    // ===== Value Retrieval Tests =====
    
    @Test
    public void testGetMutualInductance() {
        registry.registerCoupling(0, 1, K, L1, L2);
        
        double expectedM = K * Math.sqrt(L1 * L2);
        assertEquals(expectedM, registry.getMutualInductance(0, 1), TOLERANCE);
        assertEquals(expectedM, registry.getMutualInductance(1, 0), TOLERANCE);
    }
    
    @Test
    public void testGetMutualInductanceNotCoupled() {
        assertEquals(0.0, registry.getMutualInductance(0, 1), TOLERANCE);
    }
    
    @Test
    public void testGetCouplingCoefficient() {
        registry.registerCoupling(0, 1, K, L1, L2);
        
        assertEquals(K, registry.getCouplingCoefficient(0, 1), TOLERANCE);
        assertEquals(K, registry.getCouplingCoefficient(1, 0), TOLERANCE);
    }
    
    @Test
    public void testGetCouplingCoefficientNotCoupled() {
        assertEquals(0.0, registry.getCouplingCoefficient(0, 1), TOLERANCE);
    }
    
    // ===== Coupling Check Tests =====
    
    @Test
    public void testAreCoupled() {
        registry.registerCoupling(0, 1, K, L1, L2);
        
        assertTrue(registry.areCoupled(0, 1));
        assertTrue(registry.areCoupled(1, 0));
        assertFalse(registry.areCoupled(0, 2));
        assertFalse(registry.areCoupled(1, 2));
    }
    
    @Test
    public void testIsCoupled() {
        registry.registerCoupling(0, 1, K, L1, L2);
        
        assertTrue(registry.isCoupled(0));
        assertTrue(registry.isCoupled(1));
        assertFalse(registry.isCoupled(2));
    }
    
    // ===== Collection Access Tests =====
    
    @Test
    public void testGetAllCoupledInductors() {
        registry.registerCoupling(0, 1, K, L1, L2);
        registry.registerCoupling(2, 3, K, L2, L3);
        
        Set<Integer> coupled = registry.getAllCoupledInductors();
        assertEquals(4, coupled.size());
        assertTrue(coupled.containsAll(Arrays.asList(0, 1, 2, 3)));
    }
    
    @Test
    public void testGetAllCouplings() {
        registry.registerCoupling(0, 1, K, L1, L2);
        registry.registerCoupling(2, 3, K, L2, L3);
        
        List<Coupling> all = registry.getAllCouplings();
        assertEquals(2, all.size());
    }
    
    @Test
    public void testGetCouplingCount() {
        assertEquals(0, registry.getCouplingCount());
        
        registry.registerCoupling(0, 1, K, L1, L2);
        assertEquals(1, registry.getCouplingCount());
        
        registry.registerCoupling(2, 3, K, L2, L3);
        assertEquals(2, registry.getCouplingCount());
    }
    
    // ===== Remove Tests =====
    
    @Test
    public void testRemoveCoupling() {
        registry.registerCoupling(0, 1, K, L1, L2);
        registry.registerCoupling(0, 2, K, L1, L3);
        
        assertTrue(registry.removeCoupling(0, 1));
        
        assertFalse(registry.areCoupled(0, 1));
        assertTrue(registry.areCoupled(0, 2));
        assertEquals(1, registry.getCouplingCount());
    }
    
    @Test
    public void testRemoveCouplingReversedOrder() {
        registry.registerCoupling(0, 1, K, L1, L2);
        assertTrue(registry.removeCoupling(1, 0)); // Reversed order
        
        assertFalse(registry.areCoupled(0, 1));
    }
    
    @Test
    public void testRemoveCouplingNotExists() {
        assertFalse(registry.removeCoupling(0, 1));
    }
    
    @Test
    public void testRemoveCouplingUpdatesCoupledSet() {
        registry.registerCoupling(0, 1, K, L1, L2);
        registry.removeCoupling(0, 1);
        
        assertFalse(registry.isCoupled(0));
        assertFalse(registry.isCoupled(1));
    }
    
    @Test
    public void testRemoveCouplingPartiallyUpdatesCoupledSet() {
        registry.registerCoupling(0, 1, K, L1, L2);
        registry.registerCoupling(0, 2, K, L1, L3);
        
        registry.removeCoupling(0, 1);
        
        assertTrue(registry.isCoupled(0));  // Still coupled to 2
        assertFalse(registry.isCoupled(1)); // No longer coupled
        assertTrue(registry.isCoupled(2));
    }
    
    @Test
    public void testClear() {
        registry.registerCoupling(0, 1, K, L1, L2);
        registry.registerCoupling(2, 3, K, L2, L3);
        
        registry.clear();
        
        assertEquals(0, registry.getCouplingCount());
        assertTrue(registry.getAllCoupledInductors().isEmpty());
        assertFalse(registry.areCoupled(0, 1));
    }
    
    // ===== Coupling Class Tests =====
    
    @Test
    public void testCouplingInvolves() {
        Coupling c = new Coupling(0, 1, K, L1, L2);
        
        assertTrue(c.involves(0));
        assertTrue(c.involves(1));
        assertFalse(c.involves(2));
    }
    
    @Test
    public void testCouplingGetPartner() {
        Coupling c = new Coupling(0, 1, K, L1, L2);
        
        assertEquals(1, c.getPartner(0));
        assertEquals(0, c.getPartner(1));
        assertEquals(-1, c.getPartner(2));
    }
    
    @Test
    public void testCouplingEquals() {
        Coupling c1 = new Coupling(0, 1, K, L1, L2);
        Coupling c2 = new Coupling(0, 1, K, L1, L2);
        Coupling c3 = new Coupling(1, 0, K, L2, L1); // Same inductors, reversed
        Coupling c4 = new Coupling(0, 2, K, L1, L3);
        
        assertEquals(c1, c2);
        assertEquals(c1, c3); // Order doesn't matter for equality
        assertNotEquals(c1, c4);
        assertNotEquals(c1, null);
    }
    
    @Test
    public void testCouplingHashCode() {
        Coupling c1 = new Coupling(0, 1, K, L1, L2);
        Coupling c2 = new Coupling(1, 0, K, L2, L1);
        
        assertEquals(c1.hashCode(), c2.hashCode()); // Order-independent
    }
    
    @Test
    public void testCouplingToString() {
        Coupling c = new Coupling(0, 1, K, L1, L2);
        String str = c.toString();
        
        assertTrue(str.contains("Coupling"));
        assertTrue(str.contains("L0"));
        assertTrue(str.contains("L1"));
    }
    
    // ===== Build Coupling Arrays Tests =====
    
    @Test
    public void testBuildCouplingArrays() {
        registry.registerCoupling(0, 1, 0.8, L1, L2);
        registry.registerCoupling(0, 2, 0.9, L1, L3);
        
        int[] voltageSourceNumbers = {1, 2, 3, 4, 5};
        double[][][] result = registry.buildCouplingArrays(voltageSourceNumbers);
        
        assertNotNull(result);
        assertEquals(2, result.length); // spgQnr and kWerte
        
        double[][] spgQnr = result[0];
        double[][] kWerte = result[1];
        
        // Inductor 0 has two partners
        assertNotNull(spgQnr[0]);
        assertEquals(2, spgQnr[0].length);
        
        // Check voltage source numbers (partners 1 and 2 have spgQnr 2 and 3)
        assertTrue(Arrays.asList(2.0, 3.0).containsAll(
            Arrays.asList(spgQnr[0][0], spgQnr[0][1])));
    }
    
    @Test
    public void testBuildCouplingArraysEmpty() {
        int[] voltageSourceNumbers = {1, 2, 3};
        double[][][] result = registry.buildCouplingArrays(voltageSourceNumbers);
        
        assertNotNull(result);
        assertEquals(2, result.length);
    }
    
    // ===== Integration Tests =====
    
    @Test
    public void testTransformerModel() {
        // Model a transformer with k=0.99
        double kTransformer = 0.99;
        double Lprimary = 10e-3;   // 10 mH
        double Lsecondary = 40e-3; // 40 mH (turns ratio 1:2)
        
        Coupling c = registry.registerCoupling(0, 1, kTransformer, Lprimary, Lsecondary);
        
        // M = k * sqrt(L1 * L2)
        double expectedM = kTransformer * Math.sqrt(Lprimary * Lsecondary);
        assertEquals(expectedM, c.getMutualInductance(), 1e-10);
        
        // Verify coupling retrieval
        assertEquals(expectedM, registry.getMutualInductance(0, 1), 1e-10);
        assertTrue(registry.areCoupled(0, 1));
    }
    
    @Test
    public void testMultipleWindingTransformer() {
        // Three-winding transformer: primary coupled to two secondaries
        double Lpri = 10e-3;
        double Lsec1 = 20e-3;
        double Lsec2 = 5e-3;
        double k = 0.95;
        
        registry.registerCoupling(0, 1, k, Lpri, Lsec1);
        registry.registerCoupling(0, 2, k, Lpri, Lsec2);
        // Note: Secondaries might also be coupled in a real transformer
        registry.registerCoupling(1, 2, k * k, Lsec1, Lsec2);
        
        assertEquals(3, registry.getCouplingCount());
        assertEquals(2, registry.getCouplingPartners(0).size());
        assertEquals(2, registry.getCouplingPartners(1).size());
        assertEquals(2, registry.getCouplingPartners(2).size());
    }
    
    @Test
    public void testLooseCoupling() {
        // Loosely coupled inductors (e.g., proximity coupling)
        double k = 0.1;  // 10% coupling
        
        registry.registerCoupling(0, 1, k, L1, L2);
        
        double M = registry.getMutualInductance(0, 1);
        double maxM = Math.sqrt(L1 * L2); // Perfect coupling would give this
        
        assertTrue(M < 0.2 * maxM); // Much less than max
        assertEquals(k, registry.getCouplingCoefficient(0, 1), TOLERANCE);
    }
    
    // ===== ToString Tests =====
    
    @Test
    public void testToString() {
        registry.registerCoupling(0, 1, K, L1, L2);
        
        String str = registry.toString();
        assertTrue(str.contains("MutualCouplingRegistry"));
        assertTrue(str.contains("count=1"));
    }
    
    @Test
    public void testToStringEmpty() {
        String str = registry.toString();
        assertTrue(str.contains("count=0"));
    }
}
