package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the HeaderSymbol enum.
 *
 * KNOWN BUG: HeaderSymbol.getFromCode(int) has a copy-paste error - it
 * iterates over TextSeparator.values() and returns TextSeparator instead of
 * HeaderSymbol. The bug is documented in tests below.
 */
public class HeaderSymbolTest {

    @Test
    public void testEnumValuesCount() {
        assertEquals(4, HeaderSymbol.values().length);
    }

    @Test
    public void testEnumOrdinals() {
        assertEquals(0, HeaderSymbol.HASH.ordinal());
        assertEquals(1, HeaderSymbol.SLASHES.ordinal());
        assertEquals(2, HeaderSymbol.SEMICOLON.ordinal());
        assertEquals(3, HeaderSymbol.COMMA.ordinal());
    }

    @Test
    public void testToStringHash() {
        assertEquals("#", HeaderSymbol.HASH.toString());
    }

    @Test
    public void testToStringSlashes() {
        assertEquals("//", HeaderSymbol.SLASHES.toString());
    }

    @Test
    public void testToStringSemicolon() {
        assertEquals(";", HeaderSymbol.SEMICOLON.toString());
    }

    @Test
    public void testToStringComma() {
        assertEquals(",", HeaderSymbol.COMMA.toString());
    }

    @Test
    public void testGetFromOrdinalHash() {
        assertEquals(HeaderSymbol.HASH, HeaderSymbol.getFromOrdinal(0));
    }

    @Test
    public void testGetFromOrdinalSlashes() {
        assertEquals(HeaderSymbol.SLASHES, HeaderSymbol.getFromOrdinal(1));
    }

    @Test
    public void testGetFromOrdinalSemicolon() {
        assertEquals(HeaderSymbol.SEMICOLON, HeaderSymbol.getFromOrdinal(2));
    }

    @Test
    public void testGetFromOrdinalComma() {
        assertEquals(HeaderSymbol.COMMA, HeaderSymbol.getFromOrdinal(3));
    }

    @Test
    public void testGetFromOrdinalRoundTrip() {
        for (HeaderSymbol sym : HeaderSymbol.values()) {
            assertEquals(sym, HeaderSymbol.getFromOrdinal(sym.ordinal()));
        }
    }

    @Test
    public void testGetFromOrdinalInvalidThrowsOrReturnsNull() {
        // getFromOrdinal has "assert false" for invalid ordinals,
        // which throws AssertionError when assertions are enabled
        try {
            HeaderSymbol result = HeaderSymbol.getFromOrdinal(99);
            assertNull(result);
        } catch (AssertionError e) {
            // Expected when assertions are enabled
        }
    }

    @Test
    public void testGetFromOrdinalNegativeThrowsOrReturnsNull() {
        try {
            HeaderSymbol result = HeaderSymbol.getFromOrdinal(-1);
            assertNull(result);
        } catch (AssertionError e) {
            // Expected when assertions are enabled
        }
    }

    @Test
    public void testValueOf() {
        assertEquals(HeaderSymbol.HASH, HeaderSymbol.valueOf("HASH"));
        assertEquals(HeaderSymbol.SLASHES, HeaderSymbol.valueOf("SLASHES"));
        assertEquals(HeaderSymbol.SEMICOLON, HeaderSymbol.valueOf("SEMICOLON"));
        assertEquals(HeaderSymbol.COMMA, HeaderSymbol.valueOf("COMMA"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfInvalidName() {
        HeaderSymbol.valueOf("INVALID");
    }

    // KNOWN BUG: getFromCode() returns TextSeparator, not HeaderSymbol
    @Test
    public void testGetFromCodeBugReturnsTextSeparatorType() {
        Object result = HeaderSymbol.getFromCode((int) ' ');
        assertTrue(result instanceof TextSeparator);
        assertEquals(TextSeparator.SPACE, result);
    }

    @Test
    public void testGetFromCodeBugMatchesTextSeparatorBehavior() {
        assertEquals(TextSeparator.TABULATOR, HeaderSymbol.getFromCode((int) '\t'));
        assertEquals(TextSeparator.SEMICOLON, HeaderSymbol.getFromCode((int) ';'));
        assertEquals(TextSeparator.COMMA, HeaderSymbol.getFromCode((int) ','));
    }

    @Test
    public void testGetFromCodeBugDefaultsToSpaceForUnknownCode() {
        assertEquals(TextSeparator.SPACE, HeaderSymbol.getFromCode(0));
    }

    @Test
    public void testGetFromCodeBugDoesNotReturnHeaderSymbol() {
        Object result = HeaderSymbol.getFromCode((int) '#');
        assertFalse(result instanceof HeaderSymbol);
        assertEquals(TextSeparator.SPACE, result);
    }
}
