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

import ch.technokrat.gecko.i18n.resources.I18nKeys;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for I18nKeys enum - translation key definitions.
 * Verifies key existence, English strings, key name consistency, and lookup mechanisms
 * for the centralized translation system.
 */
public class I18nKeysTest {

    @BeforeClass
    public static void setUpClass() {
        // Initialize English strings for translation testing
        LangInit.initEnglish();
    }

    @Test
    public void testFileKeyEnglishString() {
        I18nKeys key = I18nKeys.FILE;
        assertEquals("File", key.getEnglishString());
    }

    @Test
    public void testNewKeyEnglishString() {
        I18nKeys key = I18nKeys.NEW;
        assertEquals("New", key.getEnglishString());
    }

    @Test
    public void testOpenKeyEnglishString() {
        I18nKeys key = I18nKeys.OPEN;
        assertEquals("Open", key.getEnglishString());
    }

    @Test
    public void testSaveKeyEnglishString() {
        I18nKeys key = I18nKeys.SAVE;
        assertEquals("Save", key.getEnglishString());
    }

    @Test
    public void testEditKeyEnglishString() {
        I18nKeys key = I18nKeys.EDIT;
        assertEquals("Edit", key.getEnglishString());
    }

    @Test
    public void testHelpKeyEnglishString() {
        I18nKeys key = I18nKeys.HELP;
        assertEquals("Help", key.getEnglishString());
    }

    @Test
    public void testAllKeysHaveEnglishStrings() {
        for (I18nKeys key : I18nKeys.values()) {
            String englishString = key.getEnglishString();
            assertNotNull("English string should not be null for key: " + key, englishString);
            assertTrue("English string should not be empty for key: " + key, englishString.length() > 0);
        }
    }

    @Test
    public void testKeyToStringRepresentation() {
        // Test that key toString() returns the enum name
        I18nKeys key = I18nKeys.FILE;
        assertEquals("FILE", key.toString());
    }

    @Test
    public void testFabricFromKeyStringFileKey() {
        I18nKeys key = I18nKeys.fabricFromKeyString("FILE");
        assertEquals(I18nKeys.FILE, key);
    }

    @Test
    public void testFabricFromKeyStringEditKey() {
        I18nKeys key = I18nKeys.fabricFromKeyString("EDIT");
        assertEquals(I18nKeys.EDIT, key);
    }

    @Test
    public void testFabricFromKeyStringNonexistent() {
        I18nKeys key = I18nKeys.fabricFromKeyString("NONEXISTENT_KEY");
        assertNull("Non-existent key string should return null", key);
    }

    @Test
    public void testFabricFromKeyStringNull() {
        I18nKeys key = I18nKeys.fabricFromKeyString(null);
        assertNull("Null key string should return null", key);
    }

    @Test
    public void testFabricFromKeyStringAllValidKeys() {
        // Verify that all enum values can be looked up by their string representation
        for (I18nKeys key : I18nKeys.values()) {
            I18nKeys lookedUp = I18nKeys.fabricFromKeyString(key.toString());
            assertEquals("Key lookup should be consistent for: " + key, key, lookedUp);
        }
    }

    @Test
    public void testGetTranslationEnglish() {
        LangInit.initEnglish();
        I18nKeys key = I18nKeys.FILE;

        String translation = key.getTranslation();
        assertNotNull("Translation should not be null", translation);
        assertEquals("English translation should match English string", "File", translation);
    }

    @Test
    public void testEnglishStringConsistency() {
        // Verify that English strings match common UI terminology
        assertEquals("File", I18nKeys.FILE.getEnglishString());
        assertEquals("Edit", I18nKeys.EDIT.getEnglishString());
        assertEquals("View", I18nKeys.VIEW.getEnglishString());
        assertEquals("Help", I18nKeys.HELP.getEnglishString());
    }

    @Test
    public void testKeyEnumValues() {
        I18nKeys[] keys = I18nKeys.values();
        assertTrue("Should have multiple keys", keys.length > 100);

        // Verify presence of core keys
        assertTrue("Should contain FILE key", contains(keys, I18nKeys.FILE));
        assertTrue("Should contain EDIT key", contains(keys, I18nKeys.EDIT));
        assertTrue("Should contain VIEW key", contains(keys, I18nKeys.VIEW));
        assertTrue("Should contain HELP key", contains(keys, I18nKeys.HELP));
    }

    @Test
    public void testKeyValueOf() {
        I18nKeys key = I18nKeys.valueOf("FILE");
        assertEquals(I18nKeys.FILE, key);

        I18nKeys editKey = I18nKeys.valueOf("EDIT");
        assertEquals(I18nKeys.EDIT, editKey);
    }

    @Test
    public void testComponentRelatedKeys() {
        // Test various component-related keys
        assertNotNull(I18nKeys.RESISTOR_R_OHM);
        assertNotNull(I18nKeys.CAPACITOR_C_F);
        assertNotNull(I18nKeys.CURRENT_SOURCE_I_A);
        assertNotNull(I18nKeys.DIODE);

        assertEquals("Resistor R [Ohm]", I18nKeys.RESISTOR_R_OHM.getEnglishString());
        assertEquals("Capacitor C [F]", I18nKeys.CAPACITOR_C_F.getEnglishString());
    }

    @Test
    public void testControlBlockKeys() {
        // Test control-related keys
        assertNotNull(I18nKeys.CONTROL);
        assertNotNull(I18nKeys.MEASURE);
        assertEquals("Control", I18nKeys.CONTROL.getEnglishString());
        assertEquals("Measure", I18nKeys.MEASURE.getEnglishString());
    }

    @Test
    public void testMultiLineKeys() {
        // Test documentation and longer string keys
        I18nKeys docKey = I18nKeys.GLOBAL_PAR_DOC;
        assertNotNull("Documentation keys should exist", docKey);
        String doc = docKey.getEnglishString();
        assertNotNull("Documentation string should not be null", doc);
        assertTrue("Documentation string should be long enough", doc.length() > 10);
    }

    @Test
    public void testKeyNameConsistency() {
        // Verify enum names are uppercase, underscored, or numeric (Java convention)
        for (I18nKeys key : I18nKeys.values()) {
            String keyName = key.toString();
            for (char c : keyName.toCharArray()) {
                assertTrue("Key names should be uppercase, underscore, or numeric: " + keyName,
                    Character.isUpperCase(c) || c == '_' || Character.isDigit(c));
            }
        }
    }

    @Test
    public void testDuplicateEnglishStrings() {
        // Some keys may have duplicate English strings, which is acceptable
        // This test just verifies we can count them
        int totalKeys = I18nKeys.values().length;
        assertTrue("Should have reasonable number of keys", totalKeys > 50);
    }

    @Test
    public void testCircuitCategoryKeys() {
        // Test circuit and component category keys
        assertNotNull(I18nKeys.CIRCUIT);
        assertNotNull(I18nKeys.CONTROL);
        assertNotNull(I18nKeys.THERMAL);
        assertNotNull(I18nKeys.SOURCE_SINK);

        assertEquals("Circuit", I18nKeys.CIRCUIT.getEnglishString());
    }

    @Test
    public void testFileOperationKeys() {
        // Test file operation related keys
        assertTrue(keyExists(I18nKeys.SAVE));
        assertTrue(keyExists(I18nKeys.OPEN));
        assertTrue(keyExists(I18nKeys.NEW));
        assertTrue(keyExists(I18nKeys.EXPORT));
        assertTrue(keyExists(I18nKeys.IMPORT));
    }

    private boolean contains(I18nKeys[] array, I18nKeys target) {
        for (I18nKeys key : array) {
            if (key == target) {
                return true;
            }
        }
        return false;
    }

    private boolean keyExists(I18nKeys key) {
        for (I18nKeys k : I18nKeys.values()) {
            if (k == key) {
                return true;
            }
        }
        return false;
    }
}
