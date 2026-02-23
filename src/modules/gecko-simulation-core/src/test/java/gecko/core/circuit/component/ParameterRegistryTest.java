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
package gecko.core.circuit.component;

import gecko.core.circuit.component.ParameterRegistry.SimpleParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

/**
 * Unit tests for ParameterRegistry.
 * Tests parameter registration, lookup, and value management.
 */
class ParameterRegistryTest {

    private ParameterRegistry<SimpleParameter> registry;

    @BeforeEach
    void setUp() {
        registry = new ParameterRegistry<>();
    }

    // ===== Registration Tests =====

    @Test
    void testRegister() {
        SimpleParameter r = new SimpleParameter("R", 1000.0);
        registry.register(r);

        assertEquals(1, registry.size());
        assertFalse(registry.isEmpty());
    }

    @Test
    void testRegisterMultiple() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.register(new SimpleParameter("L", 0.001));
        registry.register(new SimpleParameter("C", 1e-6));

        assertEquals(3, registry.size());
    }

    @Test
    void testRegisterNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.register(null);
        });
    }

    @Test
    void testUnregister() {
        SimpleParameter r = new SimpleParameter("R", 1000.0);
        registry.register(r);

        assertTrue(registry.unregister(r));
        assertEquals(0, registry.size());
        assertTrue(registry.isEmpty());
    }

    @Test
    void testUnregisterNotFound() {
        SimpleParameter r = new SimpleParameter("R", 1000.0);
        assertFalse(registry.unregister(r));
    }

    @Test
    void testClear() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.register(new SimpleParameter("L", 0.001));

        registry.clear();

        assertEquals(0, registry.size());
        assertTrue(registry.isEmpty());
    }

    // ===== Lookup Tests =====

    @Test
    void testFindByShortName() {
        SimpleParameter r = new SimpleParameter("R", 1000.0);
        registry.register(r);

        assertSame(r, registry.findByShortName("R"));
        assertSame(r, registry.findByShortName("r")); // Case insensitive
        assertNull(registry.findByShortName("L"));
    }

    @Test
    void testFindByAlternativeName() {
        SimpleParameter r = new SimpleParameter("R", 1000.0).withAlternativeName("Res");
        registry.register(r);

        assertSame(r, registry.findByAlternativeName("Res"));
        assertSame(r, registry.findByAlternativeName("RES")); // Case insensitive
        assertNull(registry.findByAlternativeName("R")); // Short name doesn't work here
    }

    @Test
    void testFindByAnyName() {
        SimpleParameter r = new SimpleParameter("R", 1000.0).withAlternativeName("Res");
        registry.register(r);

        assertSame(r, registry.findByAnyName("R"));
        assertSame(r, registry.findByAnyName("Res"));
        assertSame(r, registry.findByAnyName("r"));
        assertNull(registry.findByAnyName("Unknown"));
    }

    @Test
    void testFindByShortNameNull() {
        assertNull(registry.findByShortName(null));
    }

    @Test
    void testHasParameter() {
        registry.register(new SimpleParameter("R", 1000.0));

        assertTrue(registry.hasParameter("R"));
        assertTrue(registry.hasParameter("r"));
        assertFalse(registry.hasParameter("L"));
    }

    // ===== Value Access Tests =====

    @Test
    void testGetValue() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.register(new SimpleParameter("L", 0.001));

        assertEquals(1000.0, registry.getValue("R"), 1e-10);
        assertEquals(0.001, registry.getValue("L"), 1e-10);
    }

    @Test
    void testGetValueNotFound() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.getValue("Unknown");
        });
    }

    @Test
    void testSetValue() {
        registry.register(new SimpleParameter("R", 1000.0));

        registry.setValue("R", 2000.0);

        assertEquals(2000.0, registry.getValue("R"), 1e-10);
    }

    @Test
    void testSetValueNotFound() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.setValue("Unknown", 100.0);
        });
    }

    // ===== Collection Access Tests =====

    @Test
    void testGetAll() {
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
    void testGetAllUnmodifiable() {
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
    void testGetAllShortNames() {
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
    void testGetAllLongNames() {
        registry.register(new SimpleParameter("R", "Resistance", "Ohm", 1000.0));
        registry.register(new SimpleParameter("L", "Inductance", "H", 0.001));

        List<String> names = registry.getAllLongNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("Resistance"));
        assertTrue(names.contains("Inductance"));
    }

    @Test
    void testGetAllUnits() {
        registry.register(new SimpleParameter("R", "Resistance", "Ohm", 1000.0));
        registry.register(new SimpleParameter("L", "Inductance", "H", 0.001));

        List<String> units = registry.getAllUnits();
        assertEquals(2, units.size());
        assertEquals("Ohm", units.get(0));
        assertEquals("H", units.get(1));
    }

    @Test
    void testGetAllValues() {
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
    void testSetAllValues() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.register(new SimpleParameter("L", 0.001));

        registry.setAllValues(new double[]{2000.0, 0.002});

        assertEquals(2000.0, registry.getValue("R"), 1e-10);
        assertEquals(0.002, registry.getValue("L"), 1e-10);
    }

    @Test
    void testSetAllValuesSizeMismatch() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.register(new SimpleParameter("R", 1000.0));
            registry.setAllValues(new double[]{1000.0, 2000.0}); // Too many
        });
    }

    @Test
    void testToMap() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.register(new SimpleParameter("L", 0.001));

        Map<String, Double> map = registry.toMap();

        assertEquals(2, map.size());
        assertEquals(1000.0, map.get("R"), 1e-10);
        assertEquals(0.001, map.get("L"), 1e-10);
    }

    // ===== Validation Tests =====

    @Test
    void testValidateRequired() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.register(new SimpleParameter("L", 0.001));

        List<String> missing = registry.validateRequired(Arrays.asList("R", "L", "C"));

        assertEquals(1, missing.size());
        assertEquals("C", missing.get(0));
    }

    @Test
    void testValidateRequiredAllPresent() {
        registry.register(new SimpleParameter("R", 1000.0));
        registry.register(new SimpleParameter("L", 0.001));

        List<String> missing = registry.validateRequired(Arrays.asList("R", "L"));

        assertTrue(missing.isEmpty());
    }

    // ===== ToString Tests =====

    @Test
    void testToString() {
        registry.register(new SimpleParameter("R", "Resistance", "Ohm", 1000.0));

        String str = registry.toString();
        assertTrue(str.contains("ParameterRegistry"));
        assertTrue(str.contains("count=1"));
        assertTrue(str.contains("R"));
        assertTrue(str.contains("1000"));
    }

    // ===== Integration Tests =====

    @Test
    void testTypicalCircuitComponent() {
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
    void testSwitchParameters() {
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
    void testCaseInsensitiveLookup() {
        registry.register(new SimpleParameter("Resistance", 1000.0));

        // All these should find the same parameter
        assertEquals(1000.0, registry.getValue("Resistance"), 1e-10);
        assertEquals(1000.0, registry.getValue("RESISTANCE"), 1e-10);
        assertEquals(1000.0, registry.getValue("resistance"), 1e-10);
        assertEquals(1000.0, registry.getValue("ReSiStAnCe"), 1e-10);
    }

    // ===== Edge Cases and Boundary Tests =====

    @Test
    void testRegisterMultipleSameName() {
        SimpleParameter r1 = new SimpleParameter("R", 100.0);
        SimpleParameter r2 = new SimpleParameter("R", 200.0);
        registry.register(r1);
        registry.register(r2);

        assertEquals(2, registry.size());
        // Second registration overwrites the first in the map
        assertEquals(200.0, registry.getValue("R"), 1e-10);
    }

    @Test
    void testFindByAlternativeNameNull() {
        assertNull(registry.findByAlternativeName(null));
    }

    @Test
    void testFindByAnyNameNull() {
        assertNull(registry.findByAnyName(null));
    }

    @Test
    void testParameterWithEmptyShortName() {
        SimpleParameter p = new SimpleParameter("", 100.0);
        registry.register(p);

        assertEquals(1, registry.size());
        assertNull(registry.findByShortName(""));
    }

    @Test
    void testParameterWithEmptyAlternativeName() {
        SimpleParameter p = new SimpleParameter("R", 100.0).withAlternativeName("");
        registry.register(p);

        assertEquals(1, registry.size());
        assertNull(registry.findByAlternativeName(""));
    }

    @Test
    void testGetAllValuesEmpty() {
        double[] values = registry.getAllValues();
        assertEquals(0, values.length);
    }

    @Test
    void testGetAllShortNamesWithEmptyNames() {
        registry.register(new SimpleParameter("", 100.0));
        registry.register(new SimpleParameter("L", 50.0));

        List<String> names = registry.getAllShortNames();
        assertEquals(1, names.size()); // Empty name filtered out
        assertEquals("L", names.get(0));
    }

    @Test
    void testGetAllLongNamesNull() {
        registry.register(new SimpleParameter("R", 100.0)); // No long name

        List<String> names = registry.getAllLongNames();
        assertEquals(0, names.size());
    }

    @Test
    void testGetAllUnitsWithNulls() {
        registry.register(new SimpleParameter("R", "Resistance", null, 100.0));
        registry.register(new SimpleParameter("L", "Inductance", "H", 50.0));

        List<String> units = registry.getAllUnits();
        assertEquals(2, units.size());
        assertEquals("", units.get(0)); // Null converted to empty string
        assertEquals("H", units.get(1));
    }

    @Test
    void testToMapEmpty() {
        Map<String, Double> map = registry.toMap();
        assertTrue(map.isEmpty());
    }

    @Test
    void testToMapWithEmptyShortNames() {
        registry.register(new SimpleParameter("", 100.0));
        registry.register(new SimpleParameter("L", 50.0));

        Map<String, Double> map = registry.toMap();
        assertEquals(1, map.size()); // Empty name filtered out
        assertEquals(50.0, map.get("L"), 1e-10);
    }

    @Test
    void testValidateRequiredEmpty() {
        List<String> missing = registry.validateRequired(new ArrayList<>());
        assertTrue(missing.isEmpty());
    }

    @Test
    void testValidateRequiredAllMissing() {
        List<String> missing = registry.validateRequired(Arrays.asList("R", "L", "C"));
        assertEquals(3, missing.size());
    }

    @Test
    void testSetAllValuesEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.register(new SimpleParameter("R", 100.0));
            registry.setAllValues(new double[0]); // Size mismatch
        });
    }

    @Test
    void testClearMultipleTimes() {
        registry.register(new SimpleParameter("R", 100.0));
        registry.clear();
        assertTrue(registry.isEmpty());

        registry.clear(); // Clear again
        assertTrue(registry.isEmpty());
    }

    @Test
    void testUnregisterAndReregister() {
        SimpleParameter r = new SimpleParameter("R", 100.0);
        registry.register(r);
        registry.unregister(r);

        // Re-register
        registry.register(r);
        assertEquals(1, registry.size());
        assertEquals(100.0, registry.getValue("R"), 1e-10);
    }

    @Test
    void testMultipleParametersSameLongName() {
        registry.register(new SimpleParameter("R1", "Resistance", "Ohm", 100.0));
        registry.register(new SimpleParameter("R2", "Resistance", "Ohm", 200.0));

        assertEquals(2, registry.size());
        List<String> longNames = registry.getAllLongNames();
        assertEquals(2, longNames.size());
    }

    @Test
    void testSetValueByAlternativeName() {
        registry.register(new SimpleParameter("R", 100.0).withAlternativeName("Res"));
        registry.setValue("Res", 200.0);

        assertEquals(200.0, registry.getValue("R"), 1e-10);
    }

    @Test
    void testToStringEmpty() {
        String str = registry.toString();
        assertTrue(str.contains("ParameterRegistry"));
        assertTrue(str.contains("count=0"));
    }

    @Test
    void testToStringMultipleWithUnits() {
        registry.register(new SimpleParameter("R", "Res", "Ohm", 100.0));
        registry.register(new SimpleParameter("L", "Ind", "H", 0.01));

        String str = registry.toString();
        assertTrue(str.contains("R"));
        assertTrue(str.contains("100"));
        assertTrue(str.contains("Ohm"));
        assertTrue(str.contains("L"));
    }
}
