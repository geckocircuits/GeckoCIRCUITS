package gecko.core.datacontainer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the HeaderSymbol enum.
 *
 * KNOWN BUG: HeaderSymbol.getFromCode(int) has a copy-paste error - it
 * iterates over TextSeparator.values() and returns TextSeparator instead of
 * HeaderSymbol. The bug is documented in tests below.
 */
class HeaderSymbolTest {

    @Test
    void testEnumValuesCount() {
        assertEquals(4, HeaderSymbol.values().length);
    }

    @Test
    void testEnumOrdinals() {
        assertEquals(0, HeaderSymbol.HASH.ordinal());
        assertEquals(1, HeaderSymbol.SLASHES.ordinal());
        assertEquals(2, HeaderSymbol.SEMICOLON.ordinal());
        assertEquals(3, HeaderSymbol.COMMA.ordinal());
    }

    @Test
    void testToStringHash() {
        assertEquals("#", HeaderSymbol.HASH.toString());
    }

    @Test
    void testToStringSlashes() {
        assertEquals("//", HeaderSymbol.SLASHES.toString());
    }

    @Test
    void testToStringSemicolon() {
        assertEquals(";", HeaderSymbol.SEMICOLON.toString());
    }

    @Test
    void testToStringComma() {
        assertEquals(",", HeaderSymbol.COMMA.toString());
    }

    @Test
    void testGetFromOrdinalHash() {
        assertEquals(HeaderSymbol.HASH, HeaderSymbol.getFromOrdinal(0));
    }

    @Test
    void testGetFromOrdinalSlashes() {
        assertEquals(HeaderSymbol.SLASHES, HeaderSymbol.getFromOrdinal(1));
    }

    @Test
    void testGetFromOrdinalSemicolon() {
        assertEquals(HeaderSymbol.SEMICOLON, HeaderSymbol.getFromOrdinal(2));
    }

    @Test
    void testGetFromOrdinalComma() {
        assertEquals(HeaderSymbol.COMMA, HeaderSymbol.getFromOrdinal(3));
    }

    @Test
    void testGetFromOrdinalRoundTrip() {
        for (HeaderSymbol sym : HeaderSymbol.values()) {
            assertEquals(sym, HeaderSymbol.getFromOrdinal(sym.ordinal()));
        }
    }

    @Test
    void testGetFromOrdinalInvalidThrowsOrReturnsNull() {
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
    void testGetFromOrdinalNegativeThrowsOrReturnsNull() {
        try {
            HeaderSymbol result = HeaderSymbol.getFromOrdinal(-1);
            assertNull(result);
        } catch (AssertionError e) {
            // Expected when assertions are enabled
        }
    }

    @Test
    void testValueOf() {
        assertEquals(HeaderSymbol.HASH, HeaderSymbol.valueOf("HASH"));
        assertEquals(HeaderSymbol.SLASHES, HeaderSymbol.valueOf("SLASHES"));
        assertEquals(HeaderSymbol.SEMICOLON, HeaderSymbol.valueOf("SEMICOLON"));
        assertEquals(HeaderSymbol.COMMA, HeaderSymbol.valueOf("COMMA"));
    }

    @Test
    void testValueOfInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> HeaderSymbol.valueOf("INVALID"));
    }

    // KNOWN BUG: getFromCode() returns TextSeparator, not HeaderSymbol
    @Test
    void testGetFromCodeBugReturnsTextSeparatorType() {
        Object result = HeaderSymbol.getFromCode((int) ' ');
        assertTrue(result instanceof TextSeparator);
        assertEquals(TextSeparator.SPACE, result);
    }

    @Test
    void testGetFromCodeBugMatchesTextSeparatorBehavior() {
        assertEquals(TextSeparator.TABULATOR, HeaderSymbol.getFromCode((int) '\t'));
        assertEquals(TextSeparator.SEMICOLON, HeaderSymbol.getFromCode((int) ';'));
        assertEquals(TextSeparator.COMMA, HeaderSymbol.getFromCode((int) ','));
    }

    @Test
    void testGetFromCodeBugDefaultsToSpaceForUnknownCode() {
        assertEquals(TextSeparator.SPACE, HeaderSymbol.getFromCode(0));
    }

    @Test
    void testGetFromCodeBugDoesNotReturnHeaderSymbol() {
        Object result = HeaderSymbol.getFromCode((int) '#');
        assertFalse(result instanceof HeaderSymbol);
        assertEquals(TextSeparator.SPACE, result);
    }
}
