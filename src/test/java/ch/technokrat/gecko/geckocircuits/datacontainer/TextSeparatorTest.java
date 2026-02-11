package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.Test;
import static org.junit.Assert.*;

public class TextSeparatorTest {

    @Test
    public void testEnumValuesCount() {
        assertEquals(4, TextSeparator.values().length);
    }

    @Test
    public void testEnumOrdinals() {
        assertEquals(0, TextSeparator.SPACE.ordinal());
        assertEquals(1, TextSeparator.TABULATOR.ordinal());
        assertEquals(2, TextSeparator.SEMICOLON.ordinal());
        assertEquals(3, TextSeparator.COMMA.ordinal());
    }

    @Test
    public void testCharValueSpace() {
        assertEquals(' ', TextSeparator.SPACE.charValue());
    }

    @Test
    public void testCharValueTabulator() {
        assertEquals('\t', TextSeparator.TABULATOR.charValue());
    }

    @Test
    public void testCharValueSemicolon() {
        assertEquals(';', TextSeparator.SEMICOLON.charValue());
    }

    @Test
    public void testCharValueComma() {
        assertEquals(',', TextSeparator.COMMA.charValue());
    }

    @Test
    public void testCodeSpace() {
        assertEquals((int) ' ', TextSeparator.SPACE.code());
    }

    @Test
    public void testCodeTabulator() {
        assertEquals((int) '\t', TextSeparator.TABULATOR.code());
    }

    @Test
    public void testCodeSemicolon() {
        assertEquals((int) ';', TextSeparator.SEMICOLON.code());
    }

    @Test
    public void testCodeComma() {
        assertEquals((int) ',', TextSeparator.COMMA.code());
    }

    @Test
    public void testStringValueSpace() {
        assertEquals(" ", TextSeparator.SPACE.stringValue());
    }

    @Test
    public void testStringValueTabulator() {
        assertEquals("\t", TextSeparator.TABULATOR.stringValue());
    }

    @Test
    public void testStringValueSemicolon() {
        assertEquals(";", TextSeparator.SEMICOLON.stringValue());
    }

    @Test
    public void testStringValueComma() {
        assertEquals(",", TextSeparator.COMMA.stringValue());
    }

    @Test
    public void testStringValueMatchesCharValue() {
        for (TextSeparator sep : TextSeparator.values()) {
            assertEquals(String.valueOf(sep.charValue()), sep.stringValue());
        }
    }

    @Test
    public void testGetFromOrdinalSpace() {
        assertEquals(TextSeparator.SPACE, TextSeparator.getFromOrdinal(0));
    }

    @Test
    public void testGetFromOrdinalTabulator() {
        assertEquals(TextSeparator.TABULATOR, TextSeparator.getFromOrdinal(1));
    }

    @Test
    public void testGetFromOrdinalSemicolon() {
        assertEquals(TextSeparator.SEMICOLON, TextSeparator.getFromOrdinal(2));
    }

    @Test
    public void testGetFromOrdinalComma() {
        assertEquals(TextSeparator.COMMA, TextSeparator.getFromOrdinal(3));
    }

    @Test
    public void testGetFromOrdinalInvalidReturnsSPACE() {
        assertEquals(TextSeparator.SPACE, TextSeparator.getFromOrdinal(-1));
        assertEquals(TextSeparator.SPACE, TextSeparator.getFromOrdinal(99));
    }

    @Test
    public void testGetFromOrdinalRoundTrip() {
        for (TextSeparator sep : TextSeparator.values()) {
            assertEquals(sep, TextSeparator.getFromOrdinal(sep.ordinal()));
        }
    }

    @Test
    public void testGetFromCodeSpace() {
        assertEquals(TextSeparator.SPACE, TextSeparator.getFromCode((int) ' '));
    }

    @Test
    public void testGetFromCodeTabulator() {
        assertEquals(TextSeparator.TABULATOR, TextSeparator.getFromCode((int) '\t'));
    }

    @Test
    public void testGetFromCodeSemicolon() {
        assertEquals(TextSeparator.SEMICOLON, TextSeparator.getFromCode((int) ';'));
    }

    @Test
    public void testGetFromCodeComma() {
        assertEquals(TextSeparator.COMMA, TextSeparator.getFromCode((int) ','));
    }

    @Test
    public void testGetFromCodeInvalidReturnsSPACE() {
        assertEquals(TextSeparator.SPACE, TextSeparator.getFromCode(0));
    }

    @Test
    public void testGetFromCodeRoundTrip() {
        for (TextSeparator sep : TextSeparator.values()) {
            assertEquals(sep, TextSeparator.getFromCode(sep.code()));
        }
    }

    @Test
    public void testValueOf() {
        assertEquals(TextSeparator.SPACE, TextSeparator.valueOf("SPACE"));
        assertEquals(TextSeparator.TABULATOR, TextSeparator.valueOf("TABULATOR"));
        assertEquals(TextSeparator.SEMICOLON, TextSeparator.valueOf("SEMICOLON"));
        assertEquals(TextSeparator.COMMA, TextSeparator.valueOf("COMMA"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfInvalidName() {
        TextSeparator.valueOf("INVALID");
    }
}
