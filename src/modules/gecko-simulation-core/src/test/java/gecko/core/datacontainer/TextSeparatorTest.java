package gecko.core.datacontainer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextSeparatorTest {

    @Test
    void testEnumValuesCount() {
        assertEquals(4, TextSeparator.values().length);
    }

    @Test
    void testEnumOrdinals() {
        assertEquals(0, TextSeparator.SPACE.ordinal());
        assertEquals(1, TextSeparator.TABULATOR.ordinal());
        assertEquals(2, TextSeparator.SEMICOLON.ordinal());
        assertEquals(3, TextSeparator.COMMA.ordinal());
    }

    @Test
    void testCharValueSpace() {
        assertEquals(' ', TextSeparator.SPACE.charValue());
    }

    @Test
    void testCharValueTabulator() {
        assertEquals('\t', TextSeparator.TABULATOR.charValue());
    }

    @Test
    void testCharValueSemicolon() {
        assertEquals(';', TextSeparator.SEMICOLON.charValue());
    }

    @Test
    void testCharValueComma() {
        assertEquals(',', TextSeparator.COMMA.charValue());
    }

    @Test
    void testCodeSpace() {
        assertEquals((int) ' ', TextSeparator.SPACE.code());
    }

    @Test
    void testCodeTabulator() {
        assertEquals((int) '\t', TextSeparator.TABULATOR.code());
    }

    @Test
    void testCodeSemicolon() {
        assertEquals((int) ';', TextSeparator.SEMICOLON.code());
    }

    @Test
    void testCodeComma() {
        assertEquals((int) ',', TextSeparator.COMMA.code());
    }

    @Test
    void testStringValueSpace() {
        assertEquals(" ", TextSeparator.SPACE.stringValue());
    }

    @Test
    void testStringValueTabulator() {
        assertEquals("\t", TextSeparator.TABULATOR.stringValue());
    }

    @Test
    void testStringValueSemicolon() {
        assertEquals(";", TextSeparator.SEMICOLON.stringValue());
    }

    @Test
    void testStringValueComma() {
        assertEquals(",", TextSeparator.COMMA.stringValue());
    }

    @Test
    void testStringValueMatchesCharValue() {
        for (TextSeparator sep : TextSeparator.values()) {
            assertEquals(String.valueOf(sep.charValue()), sep.stringValue());
        }
    }

    @Test
    void testGetFromOrdinalSpace() {
        assertEquals(TextSeparator.SPACE, TextSeparator.getFromOrdinal(0));
    }

    @Test
    void testGetFromOrdinalTabulator() {
        assertEquals(TextSeparator.TABULATOR, TextSeparator.getFromOrdinal(1));
    }

    @Test
    void testGetFromOrdinalSemicolon() {
        assertEquals(TextSeparator.SEMICOLON, TextSeparator.getFromOrdinal(2));
    }

    @Test
    void testGetFromOrdinalComma() {
        assertEquals(TextSeparator.COMMA, TextSeparator.getFromOrdinal(3));
    }

    @Test
    void testGetFromOrdinalInvalidReturnsSPACE() {
        assertEquals(TextSeparator.SPACE, TextSeparator.getFromOrdinal(-1));
        assertEquals(TextSeparator.SPACE, TextSeparator.getFromOrdinal(99));
    }

    @Test
    void testGetFromOrdinalRoundTrip() {
        for (TextSeparator sep : TextSeparator.values()) {
            assertEquals(sep, TextSeparator.getFromOrdinal(sep.ordinal()));
        }
    }

    @Test
    void testGetFromCodeSpace() {
        assertEquals(TextSeparator.SPACE, TextSeparator.getFromCode((int) ' '));
    }

    @Test
    void testGetFromCodeTabulator() {
        assertEquals(TextSeparator.TABULATOR, TextSeparator.getFromCode((int) '\t'));
    }

    @Test
    void testGetFromCodeSemicolon() {
        assertEquals(TextSeparator.SEMICOLON, TextSeparator.getFromCode((int) ';'));
    }

    @Test
    void testGetFromCodeComma() {
        assertEquals(TextSeparator.COMMA, TextSeparator.getFromCode((int) ','));
    }

    @Test
    void testGetFromCodeInvalidReturnsSPACE() {
        assertEquals(TextSeparator.SPACE, TextSeparator.getFromCode(0));
    }

    @Test
    void testGetFromCodeRoundTrip() {
        for (TextSeparator sep : TextSeparator.values()) {
            assertEquals(sep, TextSeparator.getFromCode(sep.code()));
        }
    }

    @Test
    void testValueOf() {
        assertEquals(TextSeparator.SPACE, TextSeparator.valueOf("SPACE"));
        assertEquals(TextSeparator.TABULATOR, TextSeparator.valueOf("TABULATOR"));
        assertEquals(TextSeparator.SEMICOLON, TextSeparator.valueOf("SEMICOLON"));
        assertEquals(TextSeparator.COMMA, TextSeparator.valueOf("COMMA"));
    }

    @Test
    void testValueOfInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> TextSeparator.valueOf("INVALID"));
    }
}
