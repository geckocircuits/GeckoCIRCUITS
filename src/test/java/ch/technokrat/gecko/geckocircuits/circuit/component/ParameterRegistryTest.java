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
package ch.technokrat.gecko.geckocircuits.circuit.component;

import ch.technokrat.gecko.geckocircuits.circuit.component.ParameterRegistry.SimpleParameter;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

/**
 * Unit tests for ParameterRegistry.
 * Tests parameter registration, lookup, and value management.
 */
public class ParameterRegistryTest {
    
    private ParameterRegistry<SimpleParameter> registry;
    
    @Before
    public void setUp() {
        registry = new ParameterRegistry<>();
    }
    
    // ===== Registration Tests =====
    
    @Test
    public void testRegister() {
        SimpleParameter r = new SimpleParameter("R", 1000.0);
        registry.register(r);
        
        assertEquals(1, registry.size());
        assertFalse(registry.isEmpty());
    }
    
    @Test
    public void testRegisterMultiple() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.register(new SimpleParameter("L", 0.001));
        registry.register(new SimpleParameter("C", 1e-6));
        
        assertEquals(3, registry.size());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNull() {
        registry.register(null);
    }
    
    @Test
    public void testUnregister() {
        SimpleParameter r = new SimpleParameter("R", 1000.0);
        registry.register(r);
        
        assertTrue(registry.unregister(r));
        assertEquals(0, registry.size());
        assertTrue(registry.isEmpty());
    }
    
    @Test
    public void testUnregisterNotFound() {
        SimpleParameter r = new SimpleParameter("R", 1000.0);
        assertFalse(registry.unregister(r));
    }
    
    @Test
    public void testClear() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.register(new SimpleParameter("L", 0.001));
        
        registry.clear();
        
        assertEquals(0, registry.size());
        assertTrue(registry.isEmpty());
    }
    
    // ===== Lookup Tests =====
    
    @Test
    public void testFindByShortName() {
        SimpleParameter r = new SimpleParameter("R", 1000.0);
        registry.register(r);
        
        assertSame(r, registry.findByShortName("R"));
        assertSame(r, registry.findByShortName("r")); // Case insensitive
        assertNull(registry.findByShortName("L"));
    }
    
    @Test
    public void testFindByAlternativeName() {
        SimpleParameter r = new SimpleParameter("R", 1000.0).withAlternativeName("Res");
        registry.register(r);
        
        assertSame(r, registry.findByAlternativeName("Res"));
        assertSame(r, registry.findByAlternativeName("RES")); // Case insensitive
        assertNull(registry.findByAlternativeName("R")); // Short name doesn't work here
    }
    
    @Test
    public void testFindByAnyName() {
        SimpleParameter r = new SimpleParameter("R", 1000.0).withAlternativeName("Res");
        registry.register(r);
        
        assertSame(r, registry.findByAnyName("R"));
        assertSame(r, registry.findByAnyName("Res"));
        assertSame(r, registry.findByAnyName("r"));
        assertNull(registry.findByAnyName("Unknown"));
    }
    
    @Test
    public void testFindByShortNameNull() {
        assertNull(registry.findByShortName(null));
    }
    
    @Test
    public void testHasParameter() {
        registry.register(new SimpleParameter("R", 1000.0));
        
        assertTrue(registry.hasParameter("R"));
        assertTrue(registry.hasParameter("r"));
        assertFalse(registry.hasParameter("L"));
    }
    
    // ===== Value Access Tests =====
    
    @Test
    public void testGetValue() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.register(new SimpleParameter("L", 0.001));
        
        assertEquals(1000.0, registry.getValue("R"), 1e-10);
        assertEquals(0.001, registry.getValue("L"), 1e-10);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testGetValueNotFound() {
        registry.getValue("Unknown");
    }
    
    @Test
    public void testSetValue() {
        registry.register(new SimpleParameter("R", 1000.0));
        
        registry.setValue("R", 2000.0);
        
        assertEquals(2000.0, registry.getValue("R"), 1e-10);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetValueNotFound() {
        registry.setValue("Unknown", 100.0);
    }
    
    // ===== Collection Access Tests =====
    
    @Test
    public void testGetAll() {
        SimpleParameter r = new SimpleParameter("R", 1000.0);
        SimpleParameter l = new SimpleParameter("L", 0.001);
        registry.register(r);
        registry.register(l);
        
        List<SimpleParameter> all = registry.getAll();
        assertEquals(2, all.size());
        assertSame(r, all.get(0));
        assertSame(l, all.get(1));
    }
    
    @Test
    public void testGetAllUnmodifiable() {
        registry.register(new SimpleParameter("R", 1000.0));
        
        List<SimpleParameter> all = registry.getAll();
        try {
            all.add(new SimpleParameter("L", 0.001));
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }
    
    @Test
    public void testGetAllShortNames() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.register(new SimpleParameter("L", 0.001));
        registry.register(new SimpleParameter("C", 1e-6));
        
        List<String> names = registry.getAllShortNames();
        assertEquals(3, names.size());
        assertEquals("R", names.get(0));
        assertEquals("L", names.get(1));
        assertEquals("C", names.get(2));
    }
    
    @Test
    public void testGetAllLongNames() {
        registry.register(new SimpleParameter("R", "Resistance", "Ohm", 1000.0));
        registry.register(new SimpleParameter("L", "Inductance", "H", 0.001));
        
        List<String> names = registry.getAllLongNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("Resistance"));
        assertTrue(names.contains("Inductance"));
    }
    
    @Test
    public void testGetAllUnits() {
        registry.register(new SimpleParameter("R", "Resistance", "Ohm", 1000.0));
        registry.register(new SimpleParameter("L", "Inductance", "H", 0.001));
        
        List<String> units = registry.getAllUnits();
        assertEquals(2, units.size());
        assertEquals("Ohm", units.get(0));
        assertEquals("H", units.get(1));
    }
    
    @Test
    public void testGetAllValues() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.register(new SimpleParameter("L", 0.001));
        registry.register(new SimpleParameter("C", 1e-6));
        
        double[] values = registry.getAllValues();
        assertEquals(3, values.length);
        assertEquals(1000.0, values[0], 1e-10);
        assertEquals(0.001, values[1], 1e-10);
        assertEquals(1e-6, values[2], 1e-10);
    }
    
    @Test
    public void testSetAllValues() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.register(new SimpleParameter("L", 0.001));
        
        registry.setAllValues(new double[]{2000.0, 0.002});
        
        assertEquals(2000.0, registry.getValue("R"), 1e-10);
        assertEquals(0.002, registry.getValue("L"), 1e-10);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testSetAllValuesSizeMismatch() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.setAllValues(new double[]{1000.0, 2000.0}); // Too many
    }
    
    @Test
    public void testToMap() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.register(new SimpleParameter("L", 0.001));
        
        Map<String, Double> map = registry.toMap();
        
        assertEquals(2, map.size());
        assertEquals(1000.0, map.get("R"), 1e-10);
        assertEquals(0.001, map.get("L"), 1e-10);
    }
    
    // ===== Validation Tests =====
    
    @Test
    public void testValidateRequired() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.register(new SimpleParameter("L", 0.001));
        
        List<String> missing = registry.validateRequired(Arrays.asList("R", "L", "C"));
        
        assertEquals(1, missing.size());
        assertEquals("C", missing.get(0));
    }
    
    @Test
    public void testValidateRequiredAllPresent() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.register(new SimpleParameter("L", 0.001));
        
        List<String> missing = registry.validateRequired(Arrays.asList("R", "L"));
        
        assertTrue(missing.isEmpty());
    }
    
    // ===== ToString Tests =====
    
    @Test
    public void testToString() {
        registry.register(new SimpleParameter("R", "Resistance", "Ohm", 1000.0));
        
        String str = registry.toString();
        assertTrue(str.contains("ParameterRegistry"));
        assertTrue(str.contains("count=1"));
        assertTrue(str.contains("R"));
        assertTrue(str.contains("1000"));
    }
    
    // ===== Integration Tests =====
    
    @Test
    public void testTypicalCircuitComponent() {
        // Simulate a typical RLC circuit component
        registry.register(new SimpleParameter("R", "Resistance", "Ohm", 100.0)
            .withAlternativeName("Res"));
        registry.register(new SimpleParameter("L", "Inductance", "H", 0.01)
            .withAlternativeName("Ind"));
        registry.register(new SimpleParameter("C", "Capacitance", "F", 1e-6)
            .withAlternativeName("Cap"));
        
        // Access by short name
        assertEquals(100.0, registry.getValue("R"), 1e-10);
        
        // Access by alternative name
        assertEquals(0.01, registry.getValue("Ind"), 1e-10);
        
        // Modify and verify
        registry.setValue("C", 2.2e-6);
        assertEquals(2.2e-6, registry.getValue("Cap"), 1e-10);
        
        // Get descriptions
        List<String> names = registry.getAllShortNames();
        assertArrayEquals(new String[]{"R", "L", "C"}, names.toArray());
    }
    
    @Test
    public void testSwitchParameters() {
        // Simulate switch/semiconductor parameters
        registry.register(new SimpleParameter("rOn", "On Resistance", "Ohm", 0.01));
        registry.register(new SimpleParameter("rOff", "Off Resistance", "Ohm", 1e6));
        registry.register(new SimpleParameter("uF", "Forward Voltage", "V", 0.7));
        registry.register(new SimpleParameter("tRec", "Recovery Time", "s", 1e-6));
        
        assertEquals(4, registry.size());
        
        // Validate all parameters exist
        List<String> missing = registry.validateRequired(
            Arrays.asList("rOn", "rOff", "uF", "tRec"));
        assertTrue(missing.isEmpty());
    }
    
    @Test
    public void testCaseInsensitiveLookup() {
        registry.register(new SimpleParameter("Resistance", 1000.0));

        // All these should find the same parameter
        assertEquals(1000.0, registry.getValue("Resistance"), 1e-10);
        assertEquals(1000.0, registry.getValue("RESISTANCE"), 1e-10);
        assertEquals(1000.0, registry.getValue("resistance"), 1e-10);
        assertEquals(1000.0, registry.getValue("ReSiStAnCe"), 1e-10);
    }

    // ===== Edge Cases and Boundary Tests =====

    @Test
    public void testRegisterMultipleSameName() {
        SimpleParameter r1 = new SimpleParameter("R", 100.0);
        SimpleParameter r2 = new SimpleParameter("R", 200.0);
        registry.register(r1);
        registry.register(r2);

        assertEquals(2, registry.size());
        // Second registration overwrites the first in the map
        assertEquals(200.0, registry.getValue("R"), 1e-10);
    }

    @Test
    public void testFindByAlternativeNameNull() {
        assertNull(registry.findByAlternativeName(null));
    }

    @Test
    public void testFindByAnyNameNull() {
        assertNull(registry.findByAnyName(null));
    }

    @Test
    public void testParameterWithEmptyShortName() {
        SimpleParameter p = new SimpleParameter("", 100.0);
        registry.register(p);

        assertEquals(1, registry.size());
        assertNull(registry.findByShortName(""));
    }

    @Test
    public void testParameterWithEmptyAlternativeName() {
        SimpleParameter p = new SimpleParameter("R", 100.0).withAlternativeName("");
        registry.register(p);

        assertEquals(1, registry.size());
        assertNull(registry.findByAlternativeName(""));
    }

    @Test
    public void testGetAllValuesEmpty() {
        double[] values = registry.getAllValues();
        assertEquals(0, values.length);
    }

    @Test
    public void testGetAllShortNamesWithEmptyNames() {
        registry.register(new SimpleParameter("", 100.0));
        registry.register(new SimpleParameter("L", 50.0));

        List<String> names = registry.getAllShortNames();
        assertEquals(1, names.size()); // Empty name filtered out
        assertEquals("L", names.get(0));
    }

    @Test
    public void testGetAllLongNamesNull() {
        registry.register(new SimpleParameter("R", 100.0)); // No long name

        List<String> names = registry.getAllLongNames();
        assertEquals(0, names.size());
    }

    @Test
    public void testGetAllUnitsWithNulls() {
        registry.register(new SimpleParameter("R", "Resistance", null, 100.0));
        registry.register(new SimpleParameter("L", "Inductance", "H", 50.0));

        List<String> units = registry.getAllUnits();
        assertEquals(2, units.size());
        assertEquals("", units.get(0)); // Null converted to empty string
        assertEquals("H", units.get(1));
    }

    @Test
    public void testToMapEmpty() {
        Map<String, Double> map = registry.toMap();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testToMapWithEmptyShortNames() {
        registry.register(new SimpleParameter("", 100.0));
        registry.register(new SimpleParameter("L", 50.0));

        Map<String, Double> map = registry.toMap();
        assertEquals(1, map.size()); // Empty name filtered out
        assertEquals(50.0, map.get("L"), 1e-10);
    }

    @Test
    public void testValidateRequiredEmpty() {
        List<String> missing = registry.validateRequired(new ArrayList<>());
        assertTrue(missing.isEmpty());
    }

    @Test
    public void testValidateRequiredAllMissing() {
        List<String> missing = registry.validateRequired(Arrays.asList("R", "L", "C"));
        assertEquals(3, missing.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetAllValuesEmpty() {
        registry.register(new SimpleParameter("R", 100.0));
        registry.setAllValues(new double[0]); // Size mismatch
    }

    @Test
    public void testClearMultipleTimes() {
        registry.register(new SimpleParameter("R", 100.0));
        registry.clear();
        assertTrue(registry.isEmpty());

        registry.clear(); // Clear again
        assertTrue(registry.isEmpty());
    }

    @Test
    public void testUnregisterAndReregister() {
        SimpleParameter r = new SimpleParameter("R", 100.0);
        registry.register(r);
        registry.unregister(r);

        // Re-register
        registry.register(r);
        assertEquals(1, registry.size());
        assertEquals(100.0, registry.getValue("R"), 1e-10);
    }

    @Test
    public void testMultipleParametersSameLongName() {
        registry.register(new SimpleParameter("R1", "Resistance", "Ohm", 100.0));
        registry.register(new SimpleParameter("R2", "Resistance", "Ohm", 200.0));

        assertEquals(2, registry.size());
        List<String> longNames = registry.getAllLongNames();
        assertEquals(2, longNames.size());
    }

    @Test
    public void testSetValueByAlternativeName() {
        registry.register(new SimpleParameter("R", 100.0).withAlternativeName("Res"));
        registry.setValue("Res", 200.0);

        assertEquals(200.0, registry.getValue("R"), 1e-10);
    }

    @Test
    public void testToStringEmpty() {
        String str = registry.toString();
        assertTrue(str.contains("ParameterRegistry"));
        assertTrue(str.contains("count=0"));
    }

    @Test
    public void testToStringMultipleWithUnits() {
        registry.register(new SimpleParameter("R", "Res", "Ohm", 100.0));
        registry.register(new SimpleParameter("L", "Ind", "H", 0.01));

        String str = registry.toString();
        assertTrue(str.contains("R"));
        assertTrue(str.contains("100"));
        assertTrue(str.contains("Ohm"));
        assertTrue(str.contains("L"));
    }
}
