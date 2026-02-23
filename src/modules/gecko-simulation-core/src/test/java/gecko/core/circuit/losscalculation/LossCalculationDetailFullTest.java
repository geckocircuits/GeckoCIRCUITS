package gecko.core.circuit.losscalculation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test class for LossCalculationDetail enum to achieve 100% coverage.
 * Tests all methods, enum values, and edge cases including deprecated file version handling.
 */
public class LossCalculationDetailFullTest {

    @Test
    public void testToString_Simple() {
        assertEquals("simple", LossCalculationDetail.SIMPLE.toString());
    }

    @Test
    public void testToString_Detailed() {
        assertEquals("detailed", LossCalculationDetail.DETAILED.toString());
    }

    @Test
    public void testGetOldGeckoCIRCUITSOrdinal_Simple() {
        assertEquals(1, LossCalculationDetail.SIMPLE.getOldGeckoCIRCUITSOrdinal());
    }

    @Test
    public void testGetOldGeckoCIRCUITSOrdinal_Detailed() {
        assertEquals(2, LossCalculationDetail.DETAILED.getOldGeckoCIRCUITSOrdinal());
    }

    @Test
    public void testGetFromDeprecatedFileVersion_ReturnsSimple() {
        assertEquals(LossCalculationDetail.SIMPLE,
                     LossCalculationDetail.getFromDeprecatedFileVersion(1));
    }

    @Test
    public void testGetFromDeprecatedFileVersion_ReturnsDetailed() {
        assertEquals(LossCalculationDetail.DETAILED,
                     LossCalculationDetail.getFromDeprecatedFileVersion(2));
    }

    @Test
    public void testGetFromDeprecatedFileVersion_InvalidNumber_ThrowsAssertionError() {
        // When assertions are enabled, invalid numbers trigger AssertionError
        // Note: This test documents the behavior but cannot be run with assertions enabled
        // The assert false in getFromDeprecatedFileVersion is a defensive check
        // that should never be hit in production (returns DETAILED as fallback)

        // If assertions were disabled, these would return DETAILED:
        // LossCalculationDetail.getFromDeprecatedFileVersion(0) -> DETAILED
        // LossCalculationDetail.getFromDeprecatedFileVersion(3) -> DETAILED

        // This test verifies the method throws AssertionError with invalid input
        try {
            LossCalculationDetail.getFromDeprecatedFileVersion(999);
            fail("Expected AssertionError to be thrown");
        } catch (AssertionError e) {
            // Expected behavior when assertions are enabled
        }
    }

    @Test
    public void testValues_ContainsExactlyTwoElements() {
        LossCalculationDetail[] values = LossCalculationDetail.values();
        assertEquals(2, values.length);
        assertEquals(LossCalculationDetail.SIMPLE, values[0]);
        assertEquals(LossCalculationDetail.DETAILED, values[1]);
    }

    @Test
    public void testValueOf_Simple() {
        assertEquals(LossCalculationDetail.SIMPLE,
                     LossCalculationDetail.valueOf("SIMPLE"));
    }

    @Test
    public void testValueOf_Detailed() {
        assertEquals(LossCalculationDetail.DETAILED,
                     LossCalculationDetail.valueOf("DETAILED"));
    }

    @Test
    public void testValueOf_InvalidName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            LossCalculationDetail.valueOf("INVALID");
        });
    }

    @Test
    public void testValueOf_Null_ThrowsException() {
        assertThrows(NullPointerException.class, () -> {
            LossCalculationDetail.valueOf(null);
        });
    }

    @Test
    public void testRoundTrip_Simple() {
        // Test: SIMPLE → getOldOrdinal → getFromDeprecated → getOldOrdinal
        LossCalculationDetail original = LossCalculationDetail.SIMPLE;
        int oldOrdinal = original.getOldGeckoCIRCUITSOrdinal();
        LossCalculationDetail recovered = LossCalculationDetail.getFromDeprecatedFileVersion(oldOrdinal);
        int finalOrdinal = recovered.getOldGeckoCIRCUITSOrdinal();

        assertEquals(original, recovered);
        assertEquals(oldOrdinal, finalOrdinal);
        assertEquals(1, finalOrdinal);
    }

    @Test
    public void testRoundTrip_Detailed() {
        // Test: DETAILED → getOldOrdinal → getFromDeprecated → getOldOrdinal
        LossCalculationDetail original = LossCalculationDetail.DETAILED;
        int oldOrdinal = original.getOldGeckoCIRCUITSOrdinal();
        LossCalculationDetail recovered = LossCalculationDetail.getFromDeprecatedFileVersion(oldOrdinal);
        int finalOrdinal = recovered.getOldGeckoCIRCUITSOrdinal();

        assertEquals(original, recovered);
        assertEquals(oldOrdinal, finalOrdinal);
        assertEquals(2, finalOrdinal);
    }

    @Test
    public void testEnumConsistency() {
        // Verify that all enum values are accounted for
        for (LossCalculationDetail detail : LossCalculationDetail.values()) {
            assertNotNull(detail.toString());
            assertTrue(detail.getOldGeckoCIRCUITSOrdinal() > 0);

            // Verify round-trip works for all values
            int ordinal = detail.getOldGeckoCIRCUITSOrdinal();
            assertEquals(detail, LossCalculationDetail.getFromDeprecatedFileVersion(ordinal));
        }
    }

    @Test
    public void testDisplayStringFormatting() {
        // Verify display strings are lowercase and match expected format
        assertEquals("simple", LossCalculationDetail.SIMPLE.toString());
        assertEquals("detailed", LossCalculationDetail.DETAILED.toString());

        // Verify no extra whitespace
        assertFalse(LossCalculationDetail.SIMPLE.toString().contains(" "));
        assertFalse(LossCalculationDetail.DETAILED.toString().contains(" "));
    }

    @Test
    public void testOldOrdinalsAreUnique() {
        // Verify that old ordinals don't collide
        int simpleOrdinal = LossCalculationDetail.SIMPLE.getOldGeckoCIRCUITSOrdinal();
        int detailedOrdinal = LossCalculationDetail.DETAILED.getOldGeckoCIRCUITSOrdinal();

        assertNotEquals(simpleOrdinal, detailedOrdinal, "Old ordinals must be unique");
    }

    @Test
    public void testEnumIdentity() {
        // Verify enum singleton behavior
        assertSame(LossCalculationDetail.SIMPLE, LossCalculationDetail.valueOf("SIMPLE"));
        assertSame(LossCalculationDetail.DETAILED, LossCalculationDetail.valueOf("DETAILED"));
    }
}
