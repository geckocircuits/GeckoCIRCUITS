/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.circuit.matrix;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;
import org.junit.Before;
import org.junit.Test;
import java.util.Set;
import static org.junit.Assert.*;

/**
 * Tests for StamperRegistry - verifies component type to stamper mapping.
 */
public class StamperRegistryTest {

    private StamperRegistry registry;

    @Before
    public void setUp() {
        registry = new StamperRegistry();
    }

    @Test
    public void testEmptyRegistry() {
        assertEquals("new registry should be empty", 0, registry.size());
    }

    @Test
    public void testRegister_SingleStamper() {
        registry.register(CircuitTyp.LK_R, new ResistorStamper());
        assertEquals("size should be 1", 1, registry.size());
        assertTrue("should have LK_R", registry.hasStamper(CircuitTyp.LK_R));
    }

    @Test
    public void testRegister_MultipleStampers() {
        registry.register(CircuitTyp.LK_R, new ResistorStamper());
        registry.register(CircuitTyp.LK_C, new CapacitorStamper());
        registry.register(CircuitTyp.LK_L, new InductorStamper());

        assertEquals("size should be 3", 3, registry.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegister_NullType_ThrowsException() {
        registry.register(null, new ResistorStamper());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegister_NullStamper_ThrowsException() {
        registry.register(CircuitTyp.LK_R, null);
    }

    @Test
    public void testGetStamper_Registered() {
        ResistorStamper resistorStamper = new ResistorStamper();
        registry.register(CircuitTyp.LK_R, resistorStamper);

        IMatrixStamper retrieved = registry.getStamper(CircuitTyp.LK_R);
        assertSame("should return same instance", resistorStamper, retrieved);
    }

    @Test
    public void testGetStamper_NotRegistered_ReturnsNull() {
        IMatrixStamper stamper = registry.getStamper(CircuitTyp.LK_R);
        assertNull("unregistered type should return null", stamper);
    }

    @Test
    public void testGetStamperRequired_Registered() {
        registry.register(CircuitTyp.LK_R, new ResistorStamper());
        IMatrixStamper stamper = registry.getStamperRequired(CircuitTyp.LK_R);
        assertNotNull("should return stamper", stamper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetStamperRequired_NotRegistered_ThrowsException() {
        registry.getStamperRequired(CircuitTyp.LK_R);
    }

    @Test
    public void testHasStamper() {
        assertFalse("should not have LK_R initially", registry.hasStamper(CircuitTyp.LK_R));
        registry.register(CircuitTyp.LK_R, new ResistorStamper());
        assertTrue("should have LK_R after registration", registry.hasStamper(CircuitTyp.LK_R));
    }

    @Test
    public void testUnregister() {
        ResistorStamper stamper = new ResistorStamper();
        registry.register(CircuitTyp.LK_R, stamper);

        IMatrixStamper removed = registry.unregister(CircuitTyp.LK_R);
        assertSame("should return removed stamper", stamper, removed);
        assertFalse("should no longer have LK_R", registry.hasStamper(CircuitTyp.LK_R));
    }

    @Test
    public void testUnregister_NotRegistered_ReturnsNull() {
        IMatrixStamper removed = registry.unregister(CircuitTyp.LK_R);
        assertNull("should return null for unregistered type", removed);
    }

    @Test
    public void testGetRegisteredTypes() {
        registry.register(CircuitTyp.LK_R, new ResistorStamper());
        registry.register(CircuitTyp.LK_C, new CapacitorStamper());

        Set<CircuitTyp> types = registry.getRegisteredTypes();
        assertEquals("should have 2 types", 2, types.size());
        assertTrue("should contain LK_R", types.contains(CircuitTyp.LK_R));
        assertTrue("should contain LK_C", types.contains(CircuitTyp.LK_C));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetRegisteredTypes_Unmodifiable() {
        registry.register(CircuitTyp.LK_R, new ResistorStamper());
        Set<CircuitTyp> types = registry.getRegisteredTypes();
        types.add(CircuitTyp.LK_C); // Should throw
    }

    @Test
    public void testClear() {
        registry.register(CircuitTyp.LK_R, new ResistorStamper());
        registry.register(CircuitTyp.LK_C, new CapacitorStamper());

        registry.clear();
        assertEquals("should be empty after clear", 0, registry.size());
    }

    @Test
    public void testCreateDefault() {
        StamperRegistry defaultRegistry = StamperRegistry.createDefault();

        assertTrue("should have resistor", defaultRegistry.hasStamper(CircuitTyp.LK_R));
        assertTrue("should have capacitor", defaultRegistry.hasStamper(CircuitTyp.LK_C));
        assertTrue("should have inductor", defaultRegistry.hasStamper(CircuitTyp.LK_L));
        assertTrue("should have voltage source", defaultRegistry.hasStamper(CircuitTyp.LK_U));
        assertTrue("should have current source", defaultRegistry.hasStamper(CircuitTyp.LK_I));
        assertTrue("should have diode", defaultRegistry.hasStamper(CircuitTyp.LK_D));
    }

    @Test
    public void testCreateDefault_CorrectStamperTypes() {
        StamperRegistry defaultRegistry = StamperRegistry.createDefault();

        assertTrue("LK_R should be ResistorStamper",
                defaultRegistry.getStamper(CircuitTyp.LK_R) instanceof ResistorStamper);
        assertTrue("LK_C should be CapacitorStamper",
                defaultRegistry.getStamper(CircuitTyp.LK_C) instanceof CapacitorStamper);
        assertTrue("LK_L should be InductorStamper",
                defaultRegistry.getStamper(CircuitTyp.LK_L) instanceof InductorStamper);
        assertTrue("LK_U should be VoltageSourceStamper",
                defaultRegistry.getStamper(CircuitTyp.LK_U) instanceof VoltageSourceStamper);
        assertTrue("LK_I should be CurrentSourceStamper",
                defaultRegistry.getStamper(CircuitTyp.LK_I) instanceof CurrentSourceStamper);
        assertTrue("LK_D should be DiodeStamper",
                defaultRegistry.getStamper(CircuitTyp.LK_D) instanceof DiodeStamper);
    }

    @Test
    public void testRegister_OverwritesExisting() {
        ResistorStamper first = new ResistorStamper();
        ResistorStamper second = new ResistorStamper();

        registry.register(CircuitTyp.LK_R, first);
        registry.register(CircuitTyp.LK_R, second);

        assertSame("should have second stamper", second, registry.getStamper(CircuitTyp.LK_R));
        assertEquals("size should still be 1", 1, registry.size());
    }

    @Test
    public void testToString() {
        registry.register(CircuitTyp.LK_R, new ResistorStamper());
        registry.register(CircuitTyp.LK_C, new CapacitorStamper());

        String str = registry.toString();
        assertTrue("should contain size", str.contains("2"));
        assertTrue("should contain LK_R", str.contains("LK_R"));
        assertTrue("should contain LK_C", str.contains("LK_C"));
    }

    @Test
    public void testDefaultRegistrySize() {
        StamperRegistry defaultRegistry = StamperRegistry.createDefault();
        assertEquals("default registry should have 6 stampers", 6, defaultRegistry.size());
    }
}
