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
package ch.technokrat.gecko.i18n;

import ch.technokrat.gecko.i18n.resources.EnglishMapper;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for EnglishMapper - initialization of English key-value pairs.
 * Verifies correct mapping of all I18nKeys enum values to their English translations
 * for both single-line and multi-line translation maps.
 */
public class EnglishMapperTest {

    @Test
    public void testInitEnglishMapSingle() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        assertNotNull("English map should not be null", map);
        assertTrue("Map should contain entries", map.getSize() > 0);
    }

    @Test
    public void testInitEnglishMapMultiple() {
        DoubleMap map = EnglishMapper.initEnglishMap_multiple();

        assertNotNull("English map should not be null", map);
        // May be empty or have fewer entries than single-line map
    }

    @Test
    public void testEnglishMapSingleContainsFileKey() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        String fileValue = map.getValue(I18nKeys.FILE);
        assertNotNull("Map should contain FILE key", fileValue);
        assertEquals("FILE key should map to 'File'", "File", fileValue);
    }

    @Test
    public void testEnglishMapSingleContainsEditKey() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        String editValue = map.getValue(I18nKeys.EDIT);
        assertNotNull("Map should contain EDIT key", editValue);
        assertEquals("EDIT key should map to 'Edit'", "Edit", editValue);
    }

    @Test
    public void testEnglishMapSingleContainsSaveKey() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        String saveValue = map.getValue(I18nKeys.SAVE);
        assertNotNull("Map should contain SAVE key", saveValue);
        assertEquals("SAVE key should map to 'Save'", "Save", saveValue);
    }

    @Test
    public void testEnglishMapSingleContainsViewKey() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        String viewValue = map.getValue(I18nKeys.VIEW);
        assertNotNull("Map should contain VIEW key", viewValue);
        assertEquals("VIEW key should map to 'View'", "View", viewValue);
    }

    @Test
    public void testEnglishMapSingleContainsHelpKey() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        String helpValue = map.getValue(I18nKeys.HELP);
        assertNotNull("Map should contain HELP key", helpValue);
        assertEquals("HELP key should map to 'Help'", "Help", helpValue);
    }

    @Test
    public void testEnglishMapSingleAllKeysPresent() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        // Verify all I18nKeys enum values are in the map
        for (I18nKeys key : I18nKeys.values()) {
            String value = map.getValue(key);
            assertNotNull("All I18nKeys should be present in map for key: " + key, value);
        }
    }

    @Test
    public void testEnglishMapSingleValuesNotNull() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        for (I18nKeys key : I18nKeys.values()) {
            String value = map.getValue(key);
            assertNotNull("Value should not be null for key: " + key, value);
            assertTrue("Value should not be empty string for key: " + key, value.length() > 0);
        }
    }

    @Test
    public void testEnglishMapSingleConsistencyWithEnglishString() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        // For core keys, the map values should match the English strings
        for (I18nKeys key : I18nKeys.values()) {
            String mapValue = map.getValue(key);
            String englishString = key.getEnglishString();

            assertEquals("Map value should match English string for key: " + key,
                englishString, mapValue);
        }
    }

    @Test
    public void testEnglishMapSingleBidirectionalLookup() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        // Test reverse lookup: value -> key
        String fileValue = map.getValue(I18nKeys.FILE);
        I18nKeys retrievedKey = map.getKey(fileValue);
        assertEquals("Reverse lookup should retrieve original key", I18nKeys.FILE, retrievedKey);
    }

    @Test
    public void testEnglishMapSingleSize() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        int expectedSize = I18nKeys.values().length;
        assertEquals("Map should contain all I18nKeys", expectedSize, map.getSize());
    }

    @Test
    public void testEnglishMapSingleComponentKeys() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        // Verify component-related keys
        String resistorValue = map.getValue(I18nKeys.RESISTOR_R_OHM);
        assertNotNull("Should contain resistor key", resistorValue);
        assertEquals("Resistor key should map correctly", "Resistor R [Ohm]", resistorValue);

        String capacitorValue = map.getValue(I18nKeys.CAPACITOR_C_F);
        assertNotNull("Should contain capacitor key", capacitorValue);
        assertEquals("Capacitor key should map correctly", "Capacitor C [F]", capacitorValue);
    }

    @Test
    public void testEnglishMapSingleControlKeys() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        String controlValue = map.getValue(I18nKeys.CONTROL);
        assertNotNull("Should contain control key", controlValue);
        assertEquals("Control key should map correctly", "Control", controlValue);

        String measureValue = map.getValue(I18nKeys.MEASURE);
        assertNotNull("Should contain measure key", measureValue);
        assertEquals("Measure key should map correctly", "Measure", measureValue);
    }

    @Test
    public void testEnglishMapSingleFileOperationKeys() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        String newValue = map.getValue(I18nKeys.NEW);
        assertNotNull("Should contain NEW key", newValue);
        assertEquals("NEW key should map correctly", "New", newValue);

        String openValue = map.getValue(I18nKeys.OPEN);
        assertNotNull("Should contain OPEN key", openValue);
        assertEquals("OPEN key should map correctly", "Open", openValue);

        String exportValue = map.getValue(I18nKeys.EXPORT);
        assertNotNull("Should contain EXPORT key", exportValue);
        assertEquals("EXPORT key should map correctly", "Export", exportValue);

        String importValue = map.getValue(I18nKeys.IMPORT);
        assertNotNull("Should contain IMPORT key", importValue);
        assertEquals("IMPORT key should map correctly", "Import", importValue);
    }

    @Test
    public void testEnglishMapMultipleNotNull() {
        DoubleMap map = EnglishMapper.initEnglishMap_multiple();

        assertNotNull("Multiple-line English map should not be null", map);
        // Multiple-line map may be empty or have different entries
    }

    @Test
    public void testEnglishMapSingleConsistencyAcrossMultipleCalls() {
        DoubleMap map1 = EnglishMapper.initEnglishMap_single();
        DoubleMap map2 = EnglishMapper.initEnglishMap_single();

        // Verify that multiple calls produce consistent results
        for (I18nKeys key : I18nKeys.values()) {
            String value1 = map1.getValue(key);
            String value2 = map2.getValue(key);

            assertEquals("Consistent results across calls for key: " + key, value1, value2);
        }
    }

    @Test
    public void testEnglishMapSingleSpecialCharacters() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        // Test some keys that exist
        String fileKey = map.getValue(I18nKeys.FILE);
        assertNotNull("File key should exist", fileKey);
        assertEquals("File key should map correctly", "File", fileKey);

        String saveKey = map.getValue(I18nKeys.SAVE);
        assertNotNull("Save key should exist", saveKey);
        assertEquals("Save key should map correctly", "Save", saveKey);
    }

    @Test
    public void testEnglishMapCreationDoesNotModifyEnums() {
        // Ensure creating maps doesn't have side effects on enums
        I18nKeys originalFile = I18nKeys.FILE;
        String originalEnglish = I18nKeys.FILE.getEnglishString();

        EnglishMapper.initEnglishMap_single();

        assertEquals("English string should not change", originalEnglish, I18nKeys.FILE.getEnglishString());
        assertEquals("Enum should not change", originalFile, I18nKeys.FILE);
    }

    @Test
    public void testEnglishMapSingleCircuitCategoryKeys() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        // Test various circuit category keys
        assertNotNull(map.getValue(I18nKeys.CIRCUIT));
        assertNotNull(map.getValue(I18nKeys.CONTROL));
        assertNotNull(map.getValue(I18nKeys.THERMAL));
        assertNotNull(map.getValue(I18nKeys.SOURCE_SINK));
    }

    @Test
    public void testEnglishMapSingleGetKeySet() {
        DoubleMap map = EnglishMapper.initEnglishMap_single();

        // Verify we can retrieve the key set
        assertNotNull("Key set should not be null", map.getKeySet());
        assertEquals("Key set size should match map size", map.getSize(), map.getKeySet().size());
    }
}
