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
package gecko.geckocircuits.circuit;

import gecko.geckocircuits.general.AbstractComponentType;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for SpecialType enum.
 * Tests special component type enumeration, ID mappings, and type conversions.
 */
public class SpecialTypeTest {

    @Test
    public void testEnumValuesExist() {
        SpecialType[] values = SpecialType.values();
        assertNotNull(values);
        assertTrue(values.length >= 2);
    }

    @Test
    public void testEnumContainsSubcircuit() {
        boolean found = false;
        for (SpecialType typ : SpecialType.values()) {
            if (typ == SpecialType.SUBCIRCUIT) {
                found = true;
                break;
            }
        }
        assertTrue("SUBCIRCUIT should exist in enum", found);
    }

    @Test
    public void testEnumContainsTextfield() {
        boolean found = false;
        for (SpecialType typ : SpecialType.values()) {
            if (typ == SpecialType.TEXTFIELD) {
                found = true;
                break;
            }
        }
        assertTrue("TEXTFIELD should exist in enum", found);
    }

    @Test
    public void testSubcircuitTypeNumber() {
        assertEquals(27, SpecialType.SUBCIRCUIT.getTypeNumber());
    }

    @Test
    public void testTextfieldTypeNumber() {
        assertEquals(70, SpecialType.TEXTFIELD.getTypeNumber());
    }

    @Test
    public void testGetFromIntNumber_Subcircuit() {
        SpecialType typ = SpecialType.getFromIntNumber(27);
        assertEquals(SpecialType.SUBCIRCUIT, typ);
    }

    @Test
    public void testGetFromIntNumber_Textfield() {
        SpecialType typ = SpecialType.getFromIntNumber(70);
        assertEquals(SpecialType.TEXTFIELD, typ);
    }

    @Test
    public void testGetFromIntNumber_InvalidNumber() {
        try {
            SpecialType.getFromIntNumber(999);
            fail("Should throw IllegalArgumentException for invalid ID");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("999"));
        }
    }

    @Test
    public void testGetFromIntNumber_NegativeNumber() {
        try {
            SpecialType.getFromIntNumber(-1);
            fail("Should throw IllegalArgumentException for negative ID");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testGetTypeInfo_Subcircuit() {
        AbstractTypeInfo info = SpecialType.SUBCIRCUIT.getTypeInfo();
        assertNotNull(info);
    }

    @Test
    public void testGetTypeInfo_Textfield() {
        AbstractTypeInfo info = SpecialType.TEXTFIELD.getTypeInfo();
        assertNotNull(info);
    }

    @Test
    public void testTypeInfoNotNull_AllTypes() {
        for (SpecialType typ : SpecialType.values()) {
            AbstractTypeInfo info = typ.getTypeInfo();
            assertNotNull("TypeInfo should not be null for " + typ, info);
        }
    }

    @Test
    public void testImplementsAbstractComponentTyp() {
        assertTrue(SpecialType.SUBCIRCUIT instanceof AbstractComponentType);
        assertTrue(SpecialType.TEXTFIELD instanceof AbstractComponentType);
    }

    @Test
    public void testGetTypeNumber_ReturnsExpectedValues() {
        SpecialType[] values = SpecialType.values();
        for (SpecialType typ : values) {
            assertTrue("Type number should be positive", typ.getTypeNumber() > 0);
        }
    }

    @Test
    public void testAllTypeNumbersAreUnique() {
        SpecialType[] values = SpecialType.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals("Type numbers should be unique",
                        values[i].getTypeNumber(), values[j].getTypeNumber());
            }
        }
    }

    @Test
    public void testGetFromIntNumber_AllValidNumbers() {
        SpecialType[] values = SpecialType.values();
        for (SpecialType typ : values) {
            int typeNum = typ.getTypeNumber();
            SpecialType retrieved = SpecialType.getFromIntNumber(typeNum);
            assertEquals("Should retrieve same type", typ, retrieved);
        }
    }

    @Test
    public void testOrdinalValues() {
        SpecialType[] values = SpecialType.values();
        for (int i = 0; i < values.length; i++) {
            assertEquals(i, values[i].ordinal());
        }
    }

    @Test
    public void testEnumValueOf_Subcircuit() {
        SpecialType typ = SpecialType.valueOf("SUBCIRCUIT");
        assertEquals(SpecialType.SUBCIRCUIT, typ);
    }

    @Test
    public void testEnumValueOf_Textfield() {
        SpecialType typ = SpecialType.valueOf("TEXTFIELD");
        assertEquals(SpecialType.TEXTFIELD, typ);
    }

    @Test
    public void testEnumValueOf_InvalidValue() {
        try {
            SpecialType.valueOf("INVALID");
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testEnumValueOf_CaseSensitive() {
        try {
            SpecialType.valueOf("subcircuit");
            fail("Should be case-sensitive");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testEnumName_Subcircuit() {
        assertEquals("SUBCIRCUIT", SpecialType.SUBCIRCUIT.name());
    }

    @Test
    public void testEnumName_Textfield() {
        assertEquals("TEXTFIELD", SpecialType.TEXTFIELD.name());
    }

    @Test
    public void testEnumComparison_SameInstance() {
        SpecialType typ1 = SpecialType.SUBCIRCUIT;
        SpecialType typ2 = SpecialType.SUBCIRCUIT;
        assertEquals(typ1, typ2);
        assertSame(typ1, typ2);
    }

    @Test
    public void testEnumComparison_DifferentTypes() {
        assertNotEquals(SpecialType.SUBCIRCUIT, SpecialType.TEXTFIELD);
    }

    @Test
    public void testEnumHashCode_Consistent() {
        int hash1 = SpecialType.SUBCIRCUIT.hashCode();
        int hash2 = SpecialType.SUBCIRCUIT.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void testEnumHashCode_Different() {
        int hash1 = SpecialType.SUBCIRCUIT.hashCode();
        int hash2 = SpecialType.TEXTFIELD.hashCode();
        assertNotEquals(hash1, hash2);
    }

    @Test
    public void testTypeNumberRange() {
        for (SpecialType typ : SpecialType.values()) {
            int typeNum = typ.getTypeNumber();
            assertTrue("Type number should be positive", typeNum > 0);
            assertTrue("Type number should be reasonable", typeNum < 1000);
        }
    }

    @Test
    public void testGetFromIntNumber_BoundaryValues() {
        // Test with the actual known boundary values
        assertEquals(SpecialType.SUBCIRCUIT, SpecialType.getFromIntNumber(27));
        assertEquals(SpecialType.TEXTFIELD, SpecialType.getFromIntNumber(70));
    }

    @Test
    public void testGetFromIntNumber_InvalidBoundary() {
        try {
            SpecialType.getFromIntNumber(26);
            fail("Should throw for invalid boundary");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testSpecialTypAsAbstractComponentTyp() {
        AbstractComponentType typ1 = SpecialType.SUBCIRCUIT;
        AbstractComponentType typ2 = SpecialType.TEXTFIELD;

        assertNotNull(typ1.getTypeInfo());
        assertNotNull(typ2.getTypeInfo());
        assertNotEquals(typ1.getTypeNumber(), typ2.getTypeNumber());
    }

    @Test
    public void testEnumIterationAndValidation() {
        for (SpecialType typ : SpecialType.values()) {
            // Verify round-trip: type -> number -> type
            int number = typ.getTypeNumber();
            SpecialType retrieved = SpecialType.getFromIntNumber(number);
            assertEquals("Round-trip conversion should preserve type", typ, retrieved);
        }
    }

    @Test
    public void testEnumToString_IsNotNull() {
        String str1 = SpecialType.SUBCIRCUIT.toString();
        String str2 = SpecialType.TEXTFIELD.toString();
        assertNotNull(str1);
        assertNotNull(str2);
    }

    @Test
    public void testEnumToString_ContainsName() {
        String str = SpecialType.SUBCIRCUIT.toString();
        assertTrue(str.contains("SUBCIRCUIT"));
    }
}
