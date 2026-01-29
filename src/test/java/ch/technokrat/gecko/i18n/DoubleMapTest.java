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
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for DoubleMap - bidirectional key-value translation storage.
 * DoubleMap maintains both key->value and value->key mappings for efficient
 * lookup in both directions (e.g., finding translations or reverse keys).
 */
public class DoubleMapTest {

    private DoubleMap doubleMap;

    @Before
    public void setUp() {
        doubleMap = new DoubleMap();
    }

    @Test
    public void testInsertAndRetrievePair() {
        doubleMap.insertPair(I18nKeys.FILE, "File");

        assertEquals("File", doubleMap.getValue(I18nKeys.FILE));
        assertEquals(I18nKeys.FILE, doubleMap.getKey("File"));
    }

    @Test
    public void testMultiplePairs() {
        doubleMap.insertPair(I18nKeys.FILE, "File");
        doubleMap.insertPair(I18nKeys.EDIT, "Edit");
        doubleMap.insertPair(I18nKeys.SAVE, "Save");

        assertEquals("File", doubleMap.getValue(I18nKeys.FILE));
        assertEquals("Edit", doubleMap.getValue(I18nKeys.EDIT));
        assertEquals("Save", doubleMap.getValue(I18nKeys.SAVE));

        assertEquals(I18nKeys.FILE, doubleMap.getKey("File"));
        assertEquals(I18nKeys.EDIT, doubleMap.getKey("Edit"));
        assertEquals(I18nKeys.SAVE, doubleMap.getKey("Save"));
    }

    @Test
    public void testGetValueNonexistentKey() {
        doubleMap.insertPair(I18nKeys.FILE, "File");

        assertNull(doubleMap.getValue(I18nKeys.EDIT));
    }

    @Test
    public void testGetKeyNonexistentValue() {
        doubleMap.insertPair(I18nKeys.FILE, "File");

        assertNull(doubleMap.getKey("NonExistent"));
    }

    @Test
    public void testRemovePair() {
        doubleMap.insertPair(I18nKeys.FILE, "File");
        doubleMap.insertPair(I18nKeys.EDIT, "Edit");

        assertEquals(2, doubleMap.getSize());

        doubleMap.removePair(I18nKeys.FILE, "File");

        assertNull(doubleMap.getValue(I18nKeys.FILE));
        assertNull(doubleMap.getKey("File"));
        assertEquals("Edit", doubleMap.getValue(I18nKeys.EDIT));
        assertEquals(1, doubleMap.getSize());
    }

    @Test
    public void testRemoveNonexistentPair() {
        doubleMap.insertPair(I18nKeys.FILE, "File");
        int initialSize = doubleMap.getSize();

        // Remove a pair that doesn't exist - should not throw exception
        doubleMap.removePair(I18nKeys.EDIT, "Edit");

        // Size should remain unchanged (or HashMap behavior persists)
        // File should still be there
        assertEquals("File", doubleMap.getValue(I18nKeys.FILE));
    }

    @Test
    public void testEmptyMapSize() {
        assertEquals(0, doubleMap.getSize());
    }

    @Test
    public void testKeySizeAfterInsertions() {
        doubleMap.insertPair(I18nKeys.FILE, "File");
        assertEquals(1, doubleMap.getSize());

        doubleMap.insertPair(I18nKeys.EDIT, "Edit");
        assertEquals(2, doubleMap.getSize());

        doubleMap.insertPair(I18nKeys.SAVE, "Save");
        assertEquals(3, doubleMap.getSize());
    }

    @Test
    public void testKeySetRetrieval() {
        doubleMap.insertPair(I18nKeys.FILE, "File");
        doubleMap.insertPair(I18nKeys.EDIT, "Edit");
        doubleMap.insertPair(I18nKeys.SAVE, "Save");

        assertTrue(doubleMap.getKeySet().contains(I18nKeys.FILE));
        assertTrue(doubleMap.getKeySet().contains(I18nKeys.EDIT));
        assertTrue(doubleMap.getKeySet().contains(I18nKeys.SAVE));
        assertEquals(3, doubleMap.getKeySet().size());
    }

    @Test
    public void testDuplicateInsertionOverwrite() {
        doubleMap.insertPair(I18nKeys.FILE, "File");
        assertEquals("File", doubleMap.getValue(I18nKeys.FILE));

        // Insert same key with different value
        doubleMap.insertPair(I18nKeys.FILE, "Fichier");

        // Should have the new value
        assertEquals("Fichier", doubleMap.getValue(I18nKeys.FILE));
    }

    @Test
    public void testTranslatedStringsWithSpecialCharacters() {
        doubleMap.insertPair(I18nKeys.FILE, "Datei");  // German
        doubleMap.insertPair(I18nKeys.EDIT, "Édition");  // French with accent

        assertEquals("Datei", doubleMap.getValue(I18nKeys.FILE));
        assertEquals("Édition", doubleMap.getValue(I18nKeys.EDIT));
    }

    @Test
    public void testBidirectionalLookupConsistency() {
        I18nKeys[] keys = {I18nKeys.FILE, I18nKeys.EDIT, I18nKeys.SAVE, I18nKeys.OPEN};
        String[] values = {"File", "Edit", "Save", "Open"};

        // Insert all pairs
        for (int i = 0; i < keys.length; i++) {
            doubleMap.insertPair(keys[i], values[i]);
        }

        // Verify forward and backward lookups are consistent
        for (int i = 0; i < keys.length; i++) {
            I18nKeys retrievedKey = doubleMap.getKey(values[i]);
            String retrievedValue = doubleMap.getValue(keys[i]);

            assertEquals(keys[i], retrievedKey);
            assertEquals(values[i], retrievedValue);
        }
    }

    @Test
    public void testLargeNumberOfPairs() {
        I18nKeys[] allKeys = I18nKeys.values();
        int testSize = Math.min(50, allKeys.length);

        for (int i = 0; i < testSize; i++) {
            doubleMap.insertPair(allKeys[i], "Translation_" + i);
        }

        assertEquals(testSize, doubleMap.getSize());

        for (int i = 0; i < testSize; i++) {
            assertEquals("Translation_" + i, doubleMap.getValue(allKeys[i]));
            assertEquals(allKeys[i], doubleMap.getKey("Translation_" + i));
        }
    }
}
