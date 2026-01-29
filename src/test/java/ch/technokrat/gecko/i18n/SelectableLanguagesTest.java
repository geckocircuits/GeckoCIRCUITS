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

import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for SelectableLanguages enum - supported language definitions.
 * Verifies language codes, display names, and Locale generation for all
 * supported languages in GeckoCIRCUITS.
 */
public class SelectableLanguagesTest {

    @Test
    public void testEnglishLanguage() {
        SelectableLanguages lang = SelectableLanguages.ENGLISH;
        assertEquals("en", lang.getLanguageCode());
        assertEquals("English", lang.toString());
        assertNotNull(lang.getLocale());
        assertEquals("en", lang.getLocale().getLanguage());
    }

    @Test
    public void testGermanLanguage() {
        SelectableLanguages lang = SelectableLanguages.GERMAN;
        assertEquals("de", lang.getLanguageCode());
        assertEquals("Deutsch", lang.toString());
        assertEquals("de", lang.getLocale().getLanguage());
    }

    @Test
    public void testFrenchLanguage() {
        SelectableLanguages lang = SelectableLanguages.FRENCH;
        assertEquals("fr", lang.getLanguageCode());
        assertEquals("français", lang.toString());
        assertEquals("fr", lang.getLocale().getLanguage());
    }

    @Test
    public void testJapaneseLanguage() {
        SelectableLanguages lang = SelectableLanguages.JAPANESE;
        assertEquals("ja", lang.getLanguageCode());
        assertEquals("日本語", lang.toString());
        assertEquals("ja", lang.getLocale().getLanguage());
    }

    @Test
    public void testPortugueseLanguage() {
        SelectableLanguages lang = SelectableLanguages.PORTUGESE;
        assertEquals("pt", lang.getLanguageCode());
        assertEquals("português", lang.toString());
        assertEquals("pt", lang.getLocale().getLanguage());
    }

    @Test
    public void testItalianLanguage() {
        SelectableLanguages lang = SelectableLanguages.ITALIAN;
        assertEquals("it", lang.getLanguageCode());
        assertEquals("italiano", lang.toString());
        assertEquals("it", lang.getLocale().getLanguage());
    }

    @Test
    public void testSpanishLanguage() {
        SelectableLanguages lang = SelectableLanguages.SPANISH;
        assertEquals("es", lang.getLanguageCode());
        assertEquals("español", lang.toString());
        assertEquals("es", lang.getLocale().getLanguage());
    }

    @Test
    public void testTurkishLanguage() {
        SelectableLanguages lang = SelectableLanguages.TURKISH;
        assertEquals("tr", lang.getLanguageCode());
        assertEquals("Türkçe", lang.toString());
        assertEquals("tr", lang.getLocale().getLanguage());
    }

    @Test
    public void testHungarianLanguage() {
        SelectableLanguages lang = SelectableLanguages.HUNGARIAN;
        assertEquals("hu", lang.getLanguageCode());
        assertEquals("magyar", lang.toString());
        assertEquals("hu", lang.getLocale().getLanguage());
    }

    @Test
    public void testPolishLanguage() {
        SelectableLanguages lang = SelectableLanguages.POLISH;
        assertEquals("pl", lang.getLanguageCode());
        assertEquals("polski", lang.toString());
        assertEquals("pl", lang.getLocale().getLanguage());
    }

    @Test
    public void testChineseLanguage() {
        SelectableLanguages lang = SelectableLanguages.CHINESE;
        assertEquals("zh", lang.getLanguageCode());
        assertEquals("中文", lang.toString());
        assertEquals("zh", lang.getLocale().getLanguage());
    }

    @Test
    public void testThaiLanguage() {
        SelectableLanguages lang = SelectableLanguages.THAI;
        assertEquals("th", lang.getLanguageCode());
        assertEquals("ไทย", lang.toString());
        assertEquals("th", lang.getLocale().getLanguage());
    }

    @Test
    public void testRomanianLanguage() {
        SelectableLanguages lang = SelectableLanguages.ROMANIC;
        assertEquals("ro", lang.getLanguageCode());
        assertEquals("română", lang.toString());
        assertEquals("ro", lang.getLocale().getLanguage());
    }

    @Test
    public void testDutchLanguage() {
        SelectableLanguages lang = SelectableLanguages.DUTCH;
        assertEquals("nl", lang.getLanguageCode());
        assertEquals("Nederlands", lang.toString());
        assertEquals("nl", lang.getLocale().getLanguage());
    }

    @Test
    public void testKoreanLanguage() {
        SelectableLanguages lang = SelectableLanguages.KOREAN;
        assertEquals("ko", lang.getLanguageCode());
        assertEquals("한국어", lang.toString());
        assertEquals("ko", lang.getLocale().getLanguage());
    }

    @Test
    public void testRussianLanguage() {
        SelectableLanguages lang = SelectableLanguages.RUSSIAN;
        assertEquals("ru", lang.getLanguageCode());
        assertEquals("русский", lang.toString());
        assertEquals("ru", lang.getLocale().getLanguage());
    }

    @Test
    public void testAllLanguagesHaveValidCodes() {
        for (SelectableLanguages lang : SelectableLanguages.values()) {
            String code = lang.getLanguageCode();
            assertNotNull("Language code should not be null for " + lang, code);
            assertTrue("Language code should not be empty for " + lang, code.length() > 0);
        }
    }

    @Test
    public void testAllLanguagesHaveValidDisplayNames() {
        for (SelectableLanguages lang : SelectableLanguages.values()) {
            String displayName = lang.toString();
            assertNotNull("Display name should not be null for " + lang, displayName);
            assertTrue("Display name should not be empty for " + lang, displayName.length() > 0);
        }
    }

    @Test
    public void testAllLanguagesGenerateValidLocales() {
        for (SelectableLanguages lang : SelectableLanguages.values()) {
            Locale locale = lang.getLocale();
            assertNotNull("Locale should not be null for " + lang, locale);
            String languageCode = locale.getLanguage();
            assertNotNull("Locale language code should not be null for " + lang, languageCode);
        }
    }

    @Test
    public void testLanguageEnumSize() {
        SelectableLanguages[] languages = SelectableLanguages.values();
        assertTrue("Should have multiple languages", languages.length > 20);
    }

    @Test
    public void testLanguageEnumValuesMethod() {
        SelectableLanguages[] languages = SelectableLanguages.values();
        assertTrue("Values array should contain ENGLISH",
            contains(languages, SelectableLanguages.ENGLISH));
        assertTrue("Values array should contain GERMAN",
            contains(languages, SelectableLanguages.GERMAN));
        assertTrue("Values array should contain FRENCH",
            contains(languages, SelectableLanguages.FRENCH));
    }

    @Test
    public void testLanguageEnumValueOf() {
        SelectableLanguages english = SelectableLanguages.valueOf("ENGLISH");
        assertEquals(SelectableLanguages.ENGLISH, english);

        SelectableLanguages german = SelectableLanguages.valueOf("GERMAN");
        assertEquals(SelectableLanguages.GERMAN, german);
    }

    @Test
    public void testLanguageCodeUniqueness() {
        // Verify that no two languages share the same code
        SelectableLanguages[] languages = SelectableLanguages.values();
        for (int i = 0; i < languages.length; i++) {
            for (int j = i + 1; j < languages.length; j++) {
                assertNotEquals(
                    "Language codes should be unique: " + languages[i] + " vs " + languages[j],
                    languages[i].getLanguageCode(),
                    languages[j].getLanguageCode()
                );
            }
        }
    }

    @Test
    public void testLocaleConsistency() {
        // Verify that locale language matches the code
        // Note: Java 9+ normalizes some language codes (e.g., "iw" -> "he", "in" -> "id")
        for (SelectableLanguages lang : SelectableLanguages.values()) {
            String code = lang.getLanguageCode();
            String localeLanguage = lang.getLocale().getLanguage();

            // Handle special cases where Java normalizes language codes
            String expectedLanguage = code;
            if ("iw".equals(code)) {
                expectedLanguage = "he";  // Hebrew language code changed in Java 9+
            } else if ("in".equals(code)) {
                expectedLanguage = "id";  // Indonesian language code changed in Java 9+
            }

            assertEquals("Locale language should match code for " + lang, expectedLanguage, localeLanguage);
        }
    }

    private boolean contains(SelectableLanguages[] array, SelectableLanguages target) {
        for (SelectableLanguages lang : array) {
            if (lang == target) {
                return true;
            }
        }
        return false;
    }
}
