package gecko.core.nativec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompileStatusTest {

    @Test
    void testEnumValues() {
        CompileStatus[] statuses = CompileStatus.values();
        assertEquals(3, statuses.length, "Should have 3 compile statuses");
    }

    @Test
    void testNotCompiledStatus() {
        assertEquals(CompileStatus.NOT_COMPILED, CompileStatus.valueOf("NOT_COMPILED"));
        assertEquals(0, CompileStatus.NOT_COMPILED.ordinal());
    }

    @Test
    void testCompiledSuccessfulStatus() {
        assertEquals(CompileStatus.COMPILED_SUCCESSFULL, CompileStatus.valueOf("COMPILED_SUCCESSFULL"));
        assertEquals(1, CompileStatus.COMPILED_SUCCESSFULL.ordinal());
    }

    @Test
    void testCompileErrorStatus() {
        assertEquals(CompileStatus.COMPILE_ERROR, CompileStatus.valueOf("COMPILE_ERROR"));
        assertEquals(2, CompileStatus.COMPILE_ERROR.ordinal());
    }

    @Test
    void testGetFromOrdinal() {
        assertEquals(CompileStatus.NOT_COMPILED, CompileStatus.getFromOrdinal(0));
        assertEquals(CompileStatus.COMPILED_SUCCESSFULL, CompileStatus.getFromOrdinal(1));
        assertEquals(CompileStatus.COMPILE_ERROR, CompileStatus.getFromOrdinal(2));
    }

    @Test
    void testGetFromInvalidOrdinalAsserts() {
        // The method has assert false for invalid ordinals
        // In production (without assertions), it returns null
        // With assertions enabled, it throws AssertionError
        try {
            CompileStatus result = CompileStatus.getFromOrdinal(999);
            // If assertions disabled, should return null
            assertNull(result, "Invalid ordinal should return null");
        } catch (AssertionError e) {
            // If assertions enabled, should throw AssertionError
            assertTrue(true, "AssertionError expected for invalid ordinal");
        }
    }

    @Test
    void testValueOfInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> {
            CompileStatus.valueOf("INVALID_STATUS");
        });
    }

    @Test
    void testEnumOrdering() {
        CompileStatus[] statuses = CompileStatus.values();
        assertEquals(CompileStatus.NOT_COMPILED, statuses[0]);
        assertEquals(CompileStatus.COMPILED_SUCCESSFULL, statuses[1]);
        assertEquals(CompileStatus.COMPILE_ERROR, statuses[2]);
    }
}
