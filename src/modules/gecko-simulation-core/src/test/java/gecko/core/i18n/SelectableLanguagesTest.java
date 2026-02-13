package gecko.core.i18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class SelectableLanguagesTest {

    @Test
    void testEnumValues() {
        SelectableLanguages[] languages = SelectableLanguages.values();
        assertEquals(43, languages.length, "Should have 43 supported languages");
    }

    @Test
    void testEnglishLanguage() {
        assertEquals("en", SelectableLanguages.ENGLISH.getLanguageCode());
        assertEquals("English", SelectableLanguages.ENGLISH.toString());
        assertEquals(new Locale("en"), SelectableLanguages.ENGLISH.getLocale());
    }

    @Test
    void testGermanLanguage() {
        assertEquals("de", SelectableLanguages.GERMAN.getLanguageCode());
        assertEquals("Deutsch", SelectableLanguages.GERMAN.toString());
        assertEquals(new Locale("de"), SelectableLanguages.GERMAN.getLocale());
    }

    @Test
    void testJapaneseLanguage() {
        assertEquals("ja", SelectableLanguages.JAPANESE.getLanguageCode());
        assertEquals("日本語", SelectableLanguages.JAPANESE.toString());
    }

    @Test
    void testChineseLanguage() {
        assertEquals("zh", SelectableLanguages.CHINESE.getLanguageCode());
        assertEquals("中文", SelectableLanguages.CHINESE.toString());
    }

    @Test
    void testArabicLanguage() {
        assertEquals("ar", SelectableLanguages.ARABIC.getLanguageCode());
        assertEquals("العربية", SelectableLanguages.ARABIC.toString());
    }

    @Test
    void testRussianLanguage() {
        assertEquals("ru", SelectableLanguages.RUSSIAN.getLanguageCode());
        assertEquals("русский", SelectableLanguages.RUSSIAN.toString());
    }

    @Test
    void testHebrewLanguage() {
        assertEquals("iw", SelectableLanguages.HEBREW.getLanguageCode());
        assertEquals("עברית", SelectableLanguages.HEBREW.toString());
    }

    @Test
    void testValueOf() {
        assertEquals(SelectableLanguages.ENGLISH, SelectableLanguages.valueOf("ENGLISH"));
        assertEquals(SelectableLanguages.FRENCH, SelectableLanguages.valueOf("FRENCH"));
        assertEquals(SelectableLanguages.SPANISH, SelectableLanguages.valueOf("SPANISH"));
    }

    @Test
    void testValueOfInvalidLanguage() {
        assertThrows(IllegalArgumentException.class, () -> {
            SelectableLanguages.valueOf("KLINGON");
        });
    }

    @Test
    void testLocaleCreation() {
        for (SelectableLanguages lang : SelectableLanguages.values()) {
            assertNotNull(lang.getLocale(), "Locale should not be null for " + lang);

            // Java converts deprecated language codes internally:
            // "iw" -> "he" (Hebrew), "in" -> "id" (Indonesian)
            String expectedCode = lang.getLanguageCode();
            String actualCode = lang.getLocale().getLanguage();
            if ("iw".equals(expectedCode)) {
                assertEquals("he", actualCode,
                        "Hebrew locale code should be converted from iw to he");
            } else if ("in".equals(expectedCode)) {
                assertEquals("id", actualCode,
                        "Indonesian locale code should be converted from in to id");
            } else {
                assertEquals(expectedCode, actualCode,
                        "Locale language code should match for " + lang);
            }
        }
    }

    @Test
    void testToStringReturnsNativeName() {
        assertEquals("English", SelectableLanguages.ENGLISH.toString());
        assertEquals("français", SelectableLanguages.FRENCH.toString());
        assertEquals("italiano", SelectableLanguages.ITALIAN.toString());
        assertEquals("español", SelectableLanguages.SPANISH.toString());
    }

    @Test
    void testAllLanguagesHaveUniqueCode() {
        SelectableLanguages[] languages = SelectableLanguages.values();
        java.util.Set<String> codes = new java.util.HashSet<>();

        for (SelectableLanguages lang : languages) {
            String code = lang.getLanguageCode();
            assertFalse(codes.contains(code),
                    "Duplicate language code detected: " + code);
            codes.add(code);
        }
    }

    @Test
    void testSampleEuropeanLanguages() {
        assertEquals("it", SelectableLanguages.ITALIAN.getLanguageCode());
        assertEquals("pt", SelectableLanguages.PORTUGESE.getLanguageCode());
        assertEquals("tr", SelectableLanguages.TURKISH.getLanguageCode());
        assertEquals("pl", SelectableLanguages.POLISH.getLanguageCode());
        assertEquals("hu", SelectableLanguages.HUNGARIAN.getLanguageCode());
    }

    @Test
    void testSampleAsianLanguages() {
        assertEquals("ko", SelectableLanguages.KOREAN.getLanguageCode());
        assertEquals("th", SelectableLanguages.THAI.getLanguageCode());
        assertEquals("vi", SelectableLanguages.VIETNAMESE.getLanguageCode());
        assertEquals("in", SelectableLanguages.INDONESIAN.getLanguageCode());
        assertEquals("ms", SelectableLanguages.MALAY.getLanguageCode());
    }

    @Test
    void testLanguageCodeNotNull() {
        for (SelectableLanguages lang : SelectableLanguages.values()) {
            assertNotNull(lang.getLanguageCode(), "Language code should not be null for " + lang);
            assertFalse(lang.getLanguageCode().isEmpty(),
                    "Language code should not be empty for " + lang);
        }
    }

    @Test
    void testForeignNameNotNull() {
        for (SelectableLanguages lang : SelectableLanguages.values()) {
            assertNotNull(lang.toString(), "Foreign name should not be null for " + lang);
            assertFalse(lang.toString().isEmpty(),
                    "Foreign name should not be empty for " + lang);
        }
    }
}
