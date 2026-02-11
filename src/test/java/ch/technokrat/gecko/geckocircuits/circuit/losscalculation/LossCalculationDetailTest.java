/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for LossCalculationDetail enum - loss calculation mode selection.
 */
public class LossCalculationDetailTest {

    // ====================================================
    // Enum Values Tests
    // ====================================================

    @Test
    public void testEnumValues_Exist() {
        LossCalculationDetail[] values = LossCalculationDetail.values();
        
        assertEquals(2, values.length);
    }

    @Test
    public void testSimpleValue_Exists() {
        LossCalculationDetail simple = LossCalculationDetail.SIMPLE;
        assertNotNull(simple);
    }

    @Test
    public void testDetailedValue_Exists() {
        LossCalculationDetail detailed = LossCalculationDetail.DETAILED;
        assertNotNull(detailed);
    }

    @Test
    public void testValueOf_Simple() {
        LossCalculationDetail simple = LossCalculationDetail.valueOf("SIMPLE");
        assertEquals(LossCalculationDetail.SIMPLE, simple);
    }

    @Test
    public void testValueOf_Detailed() {
        LossCalculationDetail detailed = LossCalculationDetail.valueOf("DETAILED");
        assertEquals(LossCalculationDetail.DETAILED, detailed);
    }

    // ====================================================
    // ToString Tests
    // ====================================================

    @Test
    public void testToString_Simple() {
        assertEquals("simple", LossCalculationDetail.SIMPLE.toString());
    }

    @Test
    public void testToString_Detailed() {
        assertEquals("detailed", LossCalculationDetail.DETAILED.toString());
    }

    // ====================================================
    // Old Ordinal Mapping Tests (File Compatibility)
    // ====================================================

    @Test
    public void testGetOldOrdinal_Simple() {
        assertEquals(1, LossCalculationDetail.SIMPLE.getOldGeckoCIRCUITSOrdinal());
    }

    @Test
    public void testGetOldOrdinal_Detailed() {
        assertEquals(2, LossCalculationDetail.DETAILED.getOldGeckoCIRCUITSOrdinal());
    }

    // ====================================================
    // Deprecated File Version Import Tests
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

    // ====================================================
    // Round-Trip Tests
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

    @Test
    public void testRoundTrip_AllValues() {
        for (LossCalculationDetail value : LossCalculationDetail.values()) {
            int ordinal = value.getOldGeckoCIRCUITSOrdinal();
            LossCalculationDetail restored = LossCalculationDetail.getFromDeprecatedFileVersion(ordinal);
            assertEquals(value, restored);
        }
    }

    // ====================================================
    // Uniqueness Tests
    // ====================================================

    @Test
    public void testOrdinals_Unique() {
        int simpleOrdinal = LossCalculationDetail.SIMPLE.getOldGeckoCIRCUITSOrdinal();
        int detailedOrdinal = LossCalculationDetail.DETAILED.getOldGeckoCIRCUITSOrdinal();
        
        assertNotEquals(simpleOrdinal, detailedOrdinal);
    }

    @Test
    public void testDisplayStrings_Unique() {
        String simpleString = LossCalculationDetail.SIMPLE.toString();
        String detailedString = LossCalculationDetail.DETAILED.toString();
        
        assertNotEquals(simpleString, detailedString);
    }
}
