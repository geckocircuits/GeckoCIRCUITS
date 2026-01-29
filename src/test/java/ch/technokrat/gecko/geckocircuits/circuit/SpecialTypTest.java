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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for SpecialTyp enum.
 * Tests special component type enumeration, ID mappings, and type conversions.
 */
public class SpecialTypTest {

    @Test
    public void testEnumValuesExist() {
        SpecialTyp[] values = SpecialTyp.values();
        assertNotNull(values);
        assertTrue(values.length >= 2);
    }

    @Test
    public void testEnumContainsSubcircuit() {
        boolean found = false;
        for (SpecialTyp typ : SpecialTyp.values()) {
            if (typ == SpecialTyp.SUBCIRCUIT) {
                found = true;
                break;
            }
        }
        assertTrue("SUBCIRCUIT should exist in enum", found);
    }

    @Test
    public void testEnumContainsTextfield() {
        boolean found = false;
        for (SpecialTyp typ : SpecialTyp.values()) {
            if (typ == SpecialTyp.TEXTFIELD) {
                found = true;
                break;
            }
        }
        assertTrue("TEXTFIELD should exist in enum", found);
    }

    @Test
    public void testSubcircuitTypeNumber() {
        assertEquals(27, SpecialTyp.SUBCIRCUIT.getTypeNumber());
    }

    @Test
    public void testTextfieldTypeNumber() {
        assertEquals(70, SpecialTyp.TEXTFIELD.getTypeNumber());
    }

    @Test
    public void testGetFromIntNumber_Subcircuit() {
        SpecialTyp typ = SpecialTyp.getFromIntNumber(27);
        assertEquals(SpecialTyp.SUBCIRCUIT, typ);
    }

    @Test
    public void testGetFromIntNumber_Textfield() {
        SpecialTyp typ = SpecialTyp.getFromIntNumber(70);
        assertEquals(SpecialTyp.TEXTFIELD, typ);
    }

    @Test
    public void testGetFromIntNumber_InvalidNumber() {
        try {
            SpecialTyp.getFromIntNumber(999);
            fail("Should throw IllegalArgumentException for invalid ID");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("999"));
        }
    }

    @Test
    public void testGetFromIntNumber_NegativeNumber() {
        try {
            SpecialTyp.getFromIntNumber(-1);
            fail("Should throw IllegalArgumentException for negative ID");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testGetTypeInfo_Subcircuit() {
        AbstractTypeInfo info = SpecialTyp.SUBCIRCUIT.getTypeInfo();
        assertNotNull(info);
    }

    @Test
    public void testGetTypeInfo_Textfield() {
        AbstractTypeInfo info = SpecialTyp.TEXTFIELD.getTypeInfo();
        assertNotNull(info);
    }

    @Test
    public void testTypeInfoNotNull_AllTypes() {
        for (SpecialTyp typ : SpecialTyp.values()) {
            AbstractTypeInfo info = typ.getTypeInfo();
            assertNotNull("TypeInfo should not be null for " + typ, info);
        }
    }

    @Test
    public void testImplementsAbstractComponentTyp() {
        assertTrue(SpecialTyp.SUBCIRCUIT instanceof AbstractComponentTyp);
        assertTrue(SpecialTyp.TEXTFIELD instanceof AbstractComponentTyp);
    }

    @Test
    public void testGetTypeNumber_ReturnsExpectedValues() {
        SpecialTyp[] values = SpecialTyp.values();
        for (SpecialTyp typ : values) {
            assertTrue("Type number should be positive", typ.getTypeNumber() > 0);
        }
    }

    @Test
    public void testAllTypeNumbersAreUnique() {
        SpecialTyp[] values = SpecialTyp.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals("Type numbers should be unique",
                        values[i].getTypeNumber(), values[j].getTypeNumber());
            }
        }
    }

    @Test
    public void testGetFromIntNumber_AllValidNumbers() {
        SpecialTyp[] values = SpecialTyp.values();
        for (SpecialTyp typ : values) {
            int typeNum = typ.getTypeNumber();
            SpecialTyp retrieved = SpecialTyp.getFromIntNumber(typeNum);
            assertEquals("Should retrieve same type", typ, retrieved);
        }
    }

    @Test
    public void testOrdinalValues() {
        SpecialTyp[] values = SpecialTyp.values();
        for (int i = 0; i < values.length; i++) {
            assertEquals(i, values[i].ordinal());
        }
    }

    @Test
    public void testEnumValueOf_Subcircuit() {
        SpecialTyp typ = SpecialTyp.valueOf("SUBCIRCUIT");
        assertEquals(SpecialTyp.SUBCIRCUIT, typ);
    }

    @Test
    public void testEnumValueOf_Textfield() {
        SpecialTyp typ = SpecialTyp.valueOf("TEXTFIELD");
        assertEquals(SpecialTyp.TEXTFIELD, typ);
    }

    @Test
    public void testEnumValueOf_InvalidValue() {
        try {
            SpecialTyp.valueOf("INVALID");
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testEnumValueOf_CaseSensitive() {
        try {
            SpecialTyp.valueOf("subcircuit");
            fail("Should be case-sensitive");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testEnumName_Subcircuit() {
        assertEquals("SUBCIRCUIT", SpecialTyp.SUBCIRCUIT.name());
    }

    @Test
    public void testEnumName_Textfield() {
        assertEquals("TEXTFIELD", SpecialTyp.TEXTFIELD.name());
    }

    @Test
    public void testEnumComparison_SameInstance() {
        SpecialTyp typ1 = SpecialTyp.SUBCIRCUIT;
        SpecialTyp typ2 = SpecialTyp.SUBCIRCUIT;
        assertEquals(typ1, typ2);
        assertSame(typ1, typ2);
    }

    @Test
    public void testEnumComparison_DifferentTypes() {
        assertNotEquals(SpecialTyp.SUBCIRCUIT, SpecialTyp.TEXTFIELD);
    }

    @Test
    public void testEnumHashCode_Consistent() {
        int hash1 = SpecialTyp.SUBCIRCUIT.hashCode();
        int hash2 = SpecialTyp.SUBCIRCUIT.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void testEnumHashCode_Different() {
        int hash1 = SpecialTyp.SUBCIRCUIT.hashCode();
        int hash2 = SpecialTyp.TEXTFIELD.hashCode();
        assertNotEquals(hash1, hash2);
    }

    @Test
    public void testTypeNumberRange() {
        for (SpecialTyp typ : SpecialTyp.values()) {
            int typeNum = typ.getTypeNumber();
            assertTrue("Type number should be positive", typeNum > 0);
            assertTrue("Type number should be reasonable", typeNum < 1000);
        }
    }

    @Test
    public void testGetFromIntNumber_BoundaryValues() {
        // Test with the actual known boundary values
        assertEquals(SpecialTyp.SUBCIRCUIT, SpecialTyp.getFromIntNumber(27));
        assertEquals(SpecialTyp.TEXTFIELD, SpecialTyp.getFromIntNumber(70));
    }

    @Test
    public void testGetFromIntNumber_InvalidBoundary() {
        try {
            SpecialTyp.getFromIntNumber(26);
            fail("Should throw for invalid boundary");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testSpecialTypAsAbstractComponentTyp() {
        AbstractComponentTyp typ1 = SpecialTyp.SUBCIRCUIT;
        AbstractComponentTyp typ2 = SpecialTyp.TEXTFIELD;

        assertNotNull(typ1.getTypeInfo());
        assertNotNull(typ2.getTypeInfo());
        assertNotEquals(typ1.getTypeNumber(), typ2.getTypeNumber());
    }

    @Test
    public void testEnumIterationAndValidation() {
        for (SpecialTyp typ : SpecialTyp.values()) {
            // Verify round-trip: type -> number -> type
            int number = typ.getTypeNumber();
            SpecialTyp retrieved = SpecialTyp.getFromIntNumber(number);
            assertEquals("Round-trip conversion should preserve type", typ, retrieved);
        }
    }

    @Test
    public void testEnumToString_IsNotNull() {
        String str1 = SpecialTyp.SUBCIRCUIT.toString();
        String str2 = SpecialTyp.TEXTFIELD.toString();
        assertNotNull(str1);
        assertNotNull(str2);
    }

    @Test
    public void testEnumToString_ContainsName() {
        String str = SpecialTyp.SUBCIRCUIT.toString();
        assertTrue(str.contains("SUBCIRCUIT"));
    }
}
