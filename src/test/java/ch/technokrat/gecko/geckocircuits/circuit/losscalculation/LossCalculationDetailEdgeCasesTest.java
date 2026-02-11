/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for LossCalculationDetail enum - edge cases and critical paths.
 * Focuses on conversion errors, null handling, and boundary conditions.
 */
public class LossCalculationDetailEdgeCasesTest {

    // ====================================================
    // getFromDeprecatedFileVersion() - Edge Cases
    // ====================================================

    @Test
    public void testGetFromDeprecatedFileVersion_Simple() {
        LossCalculationDetail result = LossCalculationDetail.getFromDeprecatedFileVersion(1);
        assertEquals(LossCalculationDetail.SIMPLE, result);
    }

    @Test
    public void testGetFromDeprecatedFileVersion_Detailed() {
        LossCalculationDetail result = LossCalculationDetail.getFromDeprecatedFileVersion(2);
        assertEquals(LossCalculationDetail.DETAILED, result);
    }

    @Test(expected = AssertionError.class)
    public void testGetFromDeprecatedFileVersion_InvalidZero() {
        // 0 is not a valid ordinal - should trigger assert
        LossCalculationDetail result = LossCalculationDetail.getFromDeprecatedFileVersion(0);
        assertEquals(LossCalculationDetail.DETAILED, result);
    }

    @Test(expected = AssertionError.class)
    public void testGetFromDeprecatedFileVersion_InvalidNegative() {
        // Negative numbers are invalid - should trigger assert
        LossCalculationDetail result = LossCalculationDetail.getFromDeprecatedFileVersion(-1);
        assertEquals(LossCalculationDetail.DETAILED, result);
    }

    @Test(expected = AssertionError.class)
    public void testGetFromDeprecatedFileVersion_InvalidTooLarge() {
        // Number > 2 is invalid - should trigger assert
        LossCalculationDetail result = LossCalculationDetail.getFromDeprecatedFileVersion(3);
        assertEquals(LossCalculationDetail.DETAILED, result);
    }

    @Test(expected = AssertionError.class)
    public void testGetFromDeprecatedFileVersion_InvalidHugeNumber() {
        // Very large number is invalid - should trigger assert
        LossCalculationDetail result = LossCalculationDetail.getFromDeprecatedFileVersion(Integer.MAX_VALUE);
        assertEquals(LossCalculationDetail.DETAILED, result);
    }

    // ====================================================
    // getOldGeckoCIRCUITSOrdinal() - Verification
    // ====================================================

    @Test
    public void testGetOldGeckoCIRCUITSOrdinal_Simple() {
        int result = LossCalculationDetail.SIMPLE.getOldGeckoCIRCUITSOrdinal();
        assertEquals(1, result);
    }

    @Test
    public void testGetOldGeckoCIRCUITSOrdinal_Detailed() {
        int result = LossCalculationDetail.DETAILED.getOldGeckoCIRCUITSOrdinal();
        assertEquals(2, result);
    }

    @Test
    public void testGetOldGeckoCIRCUITSOrdinal_AllValuesUnique() {
        int ordinalSimple = LossCalculationDetail.SIMPLE.getOldGeckoCIRCUITSOrdinal();
        int ordinalDetailed = LossCalculationDetail.DETAILED.getOldGeckoCIRCUITSOrdinal();

        assertNotEquals(ordinalSimple, ordinalDetailed);
    }

    // ====================================================
    // Round-Trip Conversions
    // ====================================================

    @Test
    public void testRoundTrip_Simple() {
        LossCalculationDetail original = LossCalculationDetail.SIMPLE;
        int ordinal = original.getOldGeckoCIRCUITSOrdinal();
        LossCalculationDetail restored = LossCalculationDetail.getFromDeprecatedFileVersion(ordinal);

        assertEquals(original, restored);
    }

    @Test
    public void testRoundTrip_Detailed() {
        LossCalculationDetail original = LossCalculationDetail.DETAILED;
        int ordinal = original.getOldGeckoCIRCUITSOrdinal();
        LossCalculationDetail restored = LossCalculationDetail.getFromDeprecatedFileVersion(ordinal);

        assertEquals(original, restored);
    }

    // ====================================================
    // toString() Tests
    // ====================================================

    @Test
    public void testToString_Simple() {
        String result = LossCalculationDetail.SIMPLE.toString();
        assertEquals("simple", result);
    }

    @Test
    public void testToString_Detailed() {
        String result = LossCalculationDetail.DETAILED.toString();
        assertEquals("detailed", result);
    }

    @Test
    public void testToString_NotNull() {
        String resultSimple = LossCalculationDetail.SIMPLE.toString();
        String resultDetailed = LossCalculationDetail.DETAILED.toString();

        assertNotNull(resultSimple);
        assertNotNull(resultDetailed);
    }

    @Test
    public void testToString_ConsistentWithValues() {
        String simpleStr = LossCalculationDetail.SIMPLE.toString();
        String detailedStr = LossCalculationDetail.DETAILED.toString();

        // Strings should be different
        assertNotEquals(simpleStr, detailedStr);
        // And consistent (lowercase)
        assertEquals(simpleStr.toLowerCase(), simpleStr);
        assertEquals(detailedStr.toLowerCase(), detailedStr);
    }

    // ====================================================
    // Enum Iteration Tests
    // ====================================================

    @Test
    public void testEnumValues_ExactlyTwoValues() {
        LossCalculationDetail[] values = LossCalculationDetail.values();
        assertEquals(2, values.length);
    }

    @Test
    public void testEnumValues_ContainsSimple() {
        boolean found = false;
        for (LossCalculationDetail val : LossCalculationDetail.values()) {
            if (val == LossCalculationDetail.SIMPLE) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testEnumValues_ContainsDetailed() {
        boolean found = false;
        for (LossCalculationDetail val : LossCalculationDetail.values()) {
            if (val == LossCalculationDetail.DETAILED) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testEnumValues_Iterate() {
        int count = 0;
        for (LossCalculationDetail val : LossCalculationDetail.values()) {
            count++;
            assertNotNull(val);
        }
        assertEquals(2, count);
    }

    // ====================================================
    // Enum Comparison Tests
    // ====================================================

    @Test
    public void testEquality_Same() {
        LossCalculationDetail simple1 = LossCalculationDetail.SIMPLE;
        LossCalculationDetail simple2 = LossCalculationDetail.SIMPLE;

        assertEquals(simple1, simple2);
        assertTrue(simple1 == simple2);
    }

    @Test
    public void testInequality_Different() {
        LossCalculationDetail simple = LossCalculationDetail.SIMPLE;
        LossCalculationDetail detailed = LossCalculationDetail.DETAILED;

        assertNotEquals(simple, detailed);
        assertFalse(simple == detailed);
    }

    // ====================================================
    // Ordinal Coverage Tests
    // ====================================================

    @Test
    public void testOrdinalCoverage_AllValidNumbers() {
        // Test all valid ordinals (1 and 2)
        for (int i = 1; i <= 2; i++) {
            LossCalculationDetail result = LossCalculationDetail.getFromDeprecatedFileVersion(i);
            assertNotNull(result);
        }
    }

    @Test
    public void testOrdinalCoverage_SimpleOrdinalIsOne() {
        assertEquals(1, LossCalculationDetail.SIMPLE.getOldGeckoCIRCUITSOrdinal());
    }

    @Test
    public void testOrdinalCoverage_DetailedOrdinalIsTwo() {
        assertEquals(2, LossCalculationDetail.DETAILED.getOldGeckoCIRCUITSOrdinal());
    }

}
