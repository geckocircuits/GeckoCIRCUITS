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
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for LangInit - core language initialization and translation lookup.
 * Verifies language selection, map initialization, and translation string retrieval.
 * Note: Some functionality depends on GUI dialogs and threading, so only core
 * non-GUI behavior is tested here.
 */
public class LangInitTest {

    @Test
    public void testInitEnglish() {
        LangInit.initEnglish();

        // After initializing English, the language should be set
        assertNotNull("Language should be set after initEnglish", LangInit.language);
        assertEquals("Language should be ENGLISH", SelectableLanguages.ENGLISH, LangInit.language);
    }

    @Test
    public void testInitEnglishMapsNotNull() {
        LangInit.initEnglish();

        assertNotNull("englishMap_single should not be null", LangInit.englishMap_single);
        assertNotNull("englishMap_multiple should not be null", LangInit.englishMap_multiple);
        assertNotNull("transMap_single should not be null", LangInit.transMap_single);
        assertNotNull("transMap_multiple should not be null", LangInit.transMap_multiple);
    }

    @Test
    public void testInitEnglishTransMapsPointToEnglishMaps() {
        LangInit.initEnglish();

        // For English, translation maps should point to English maps
        assertSame("transMap_single should be englishMap_single for English",
            LangInit.englishMap_single, LangInit.transMap_single);
        assertSame("transMap_multiple should be englishMap_multiple for English",
            LangInit.englishMap_multiple, LangInit.transMap_multiple);
    }

    @Test
    public void testGetTranslatedStringWithValidKey() {
        LangInit.initEnglish();

        String translated = LangInit.getTranslatedString(I18nKeys.FILE);
        assertNotNull("Translated string should not be null", translated);
        assertEquals("Should get English translation", "File", translated);
    }

    @Test
    public void testGetTranslatedStringAllCommonKeys() {
        LangInit.initEnglish();

        assertEquals("FILE key", "File", LangInit.getTranslatedString(I18nKeys.FILE));
        assertEquals("EDIT key", "Edit", LangInit.getTranslatedString(I18nKeys.EDIT));
        assertEquals("VIEW key", "View", LangInit.getTranslatedString(I18nKeys.VIEW));
        assertEquals("HELP key", "Help", LangInit.getTranslatedString(I18nKeys.HELP));
    }

    @Test
    public void testGetTranslatedStringNullTransMap() {
        // Manually set transMap_single to null to test fallback
        LangInit.initEnglish();
        DoubleMap savedMap = LangInit.transMap_single;
        LangInit.transMap_single = null;

        try {
            String translated = LangInit.getTranslatedString(I18nKeys.FILE);
            assertNotNull("Should return English string when transMap is null", translated);
            assertEquals("Should return English translation", "File", translated);
        } finally {
            // Restore the map
            LangInit.transMap_single = savedMap;
        }
    }

    @Test
    public void testGetTranslatedStringReturnsEnglishStringAsDefault() {
        // Test the fallback mechanism
        LangInit.initEnglish();

        // When transMap is properly initialized, should return mapped value
        String value = LangInit.getTranslatedString(I18nKeys.SAVE);
        assertNotNull("Should get translation", value);
        assertEquals("Should match key's English string", I18nKeys.SAVE.getEnglishString(), value);
    }

    @Test
    public void testGetTranslatedStringComponentKeys() {
        LangInit.initEnglish();

        String resistor = LangInit.getTranslatedString(I18nKeys.RESISTOR_R_OHM);
        assertNotNull("Resistor translation should not be null", resistor);
        assertEquals("Resistor translation", "Resistor R [Ohm]", resistor);

        String capacitor = LangInit.getTranslatedString(I18nKeys.CAPACITOR_C_F);
        assertNotNull("Capacitor translation should not be null", capacitor);
        assertEquals("Capacitor translation", "Capacitor C [F]", capacitor);
    }

    @Test
    public void testLanguageStaticVariable() {
        LangInit.initEnglish();

        assertNotNull("Static language variable should not be null", LangInit.language);
        assertTrue("Language should be a SelectableLanguages enum value",
            LangInit.language instanceof SelectableLanguages);
    }

    @Test
    public void testEnglishMapSingleSize() {
        LangInit.initEnglish();

        assertNotNull("englishMap_single should not be null", LangInit.englishMap_single);
        int size = LangInit.englishMap_single.getSize();
        assertTrue("English map should have entries", size > 0);
    }

    @Test
    public void testEnglishMapSingleContainsBasicKeys() {
        LangInit.initEnglish();

        DoubleMap map = LangInit.englishMap_single;
        assertNotNull("FILE key should exist", map.getValue(I18nKeys.FILE));
        assertNotNull("EDIT key should exist", map.getValue(I18nKeys.EDIT));
        assertNotNull("SAVE key should exist", map.getValue(I18nKeys.SAVE));
        assertNotNull("HELP key should exist", map.getValue(I18nKeys.HELP));
    }

    @Test
    public void testMultipleInitEnglishCalls() {
        LangInit.initEnglish();
        SelectableLanguages firstLanguage = LangInit.language;

        LangInit.initEnglish();
        SelectableLanguages secondLanguage = LangInit.language;

        assertEquals("Multiple init calls should maintain language", firstLanguage, secondLanguage);
        assertEquals("Language should remain ENGLISH", SelectableLanguages.ENGLISH, secondLanguage);
    }

    @Test
    public void testEnglishMapMultipleNotNull() {
        LangInit.initEnglish();

        assertNotNull("englishMap_multiple should not be null", LangInit.englishMap_multiple);
        // englishMap_multiple may have 0 or more entries
    }

    @Test
    public void testInitEnglishLocaleHandling() {
        LangInit.initEnglish();

        // Verify English locale is used
        assertEquals("Language should be ENGLISH", SelectableLanguages.ENGLISH, LangInit.language);
        String languageCode = LangInit.language.getLanguageCode();
        assertEquals("Language code should be 'en'", "en", languageCode);
    }

    @Test
    public void testGetTranslatedStringFileOperationKeys() {
        LangInit.initEnglish();

        assertEquals("NEW", "New", LangInit.getTranslatedString(I18nKeys.NEW));
        assertEquals("OPEN", "Open", LangInit.getTranslatedString(I18nKeys.OPEN));
        assertEquals("SAVE", "Save", LangInit.getTranslatedString(I18nKeys.SAVE));
        assertEquals("SAVE_AS", "Save As", LangInit.getTranslatedString(I18nKeys.SAVE_AS));
        assertEquals("EXPORT", "Export", LangInit.getTranslatedString(I18nKeys.EXPORT));
        assertEquals("IMPORT", "Import", LangInit.getTranslatedString(I18nKeys.IMPORT));
    }

    @Test
    public void testGetTranslatedStringEditOperationKeys() {
        LangInit.initEnglish();

        assertEquals("UNDO", "Undo", LangInit.getTranslatedString(I18nKeys.UNDO));
        assertEquals("REDO", "Redo", LangInit.getTranslatedString(I18nKeys.REDO));
        assertEquals("COPY_ELEMENTS", "Copy Elements", LangInit.getTranslatedString(I18nKeys.COPY_ELEMENTS));
        assertEquals("DELETE_ELEMENTS", "Delete Elements", LangInit.getTranslatedString(I18nKeys.DELETE_ELEMENTS));
    }

    @Test
    public void testTransMapSingleAfterEnglishInit() {
        LangInit.initEnglish();

        DoubleMap transMap = LangInit.transMap_single;
        assertNotNull("transMap_single should not be null", transMap);

        // For English, transMap_single should equal englishMap_single
        assertSame("transMap_single should reference englishMap_single",
            LangInit.englishMap_single, transMap);
    }

    @Test
    public void testEnglishMapKeysetSize() {
        LangInit.initEnglish();

        DoubleMap map = LangInit.englishMap_single;
        int keysetSize = map.getKeySet().size();
        int mapSize = map.getSize();

        assertEquals("Keyset size should match map size", mapSize, keysetSize);
    }

    @Test
    public void testEnglishMapAllKeysPresent() {
        LangInit.initEnglish();

        DoubleMap map = LangInit.englishMap_single;
        I18nKeys[] allKeys = I18nKeys.values();

        for (I18nKeys key : allKeys) {
            String value = map.getValue(key);
            assertNotNull("Key should be present in English map: " + key, value);
        }
    }

    @Test
    public void testInitEnglishEnglishStringConsistency() {
        LangInit.initEnglish();

        // Verify that translations for English match the English strings
        for (I18nKeys key : I18nKeys.values()) {
            String translated = LangInit.getTranslatedString(key);
            String englishString = key.getEnglishString();

            assertEquals("Translation should match English string for: " + key,
                englishString, translated);
        }
    }
}
