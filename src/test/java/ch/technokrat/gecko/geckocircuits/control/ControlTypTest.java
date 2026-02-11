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
package ch.technokrat.gecko.geckocircuits.control;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for ControlTyp enum.
 * Tests the enum values, type numbers, and conversion methods.
 */
public class ControlTypTest {

    @Test
    public void testControlTypEnumExists() {
        assertNotNull(ControlTyp.C_VOLTMETER);
        assertNotNull(ControlTyp.C_CONST);
        assertNotNull(ControlTyp.C_GAIN);
    }

    @Test
    public void testVoltmeterTypeNumber() {
        assertEquals(1, ControlTyp.C_VOLTMETER.getTypeNumber());
    }

    @Test
    public void testAmmeterTypeNumber() {
        assertEquals(2, ControlTyp.C_AMPMETER.getTypeNumber());
    }

    @Test
    public void testConstantTypeNumber() {
        assertEquals(3, ControlTyp.C_CONST.getTypeNumber());
    }

    @Test
    public void testSignalSourceTypeNumber() {
        assertEquals(4, ControlTyp.C_SIGNALSOURCE.getTypeNumber());
    }

    @Test
    public void testGainTypeNumber() {
        assertEquals(7, ControlTyp.C_GAIN.getTypeNumber());
    }

    @Test
    public void testGetFromIntNumberVoltmeter() {
        ControlTyp typ = ControlTyp.getFromIntNumber(1);
        assertEquals(ControlTyp.C_VOLTMETER, typ);
    }

    @Test
    public void testGetFromIntNumberGain() {
        ControlTyp typ = ControlTyp.getFromIntNumber(7);
        assertEquals(ControlTyp.C_GAIN, typ);
    }

    @Test
    public void testGetFromIntNumberPT1() {
        ControlTyp typ = ControlTyp.getFromIntNumber(8);
        assertEquals(ControlTyp.C_PT1, typ);
    }

    @Test
    public void testGetFromIntNumberPI() {
        ControlTyp typ = ControlTyp.getFromIntNumber(10);
        assertEquals(ControlTyp.C_PI, typ);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFromIntNumberInvalid() {
        ControlTyp.getFromIntNumber(999);
    }

    @Test
    public void testTypeInfoNotNull() {
        assertNotNull(ControlTyp.C_VOLTMETER.getTypeInfo());
        assertNotNull(ControlTyp.C_CONST.getTypeInfo());
        assertNotNull(ControlTyp.C_GAIN.getTypeInfo());
    }

    @Test
    public void testEnumValues() {
        ControlTyp[] values = ControlTyp.values();
        assertTrue(values.length > 0);
    }

    @Test
    public void testValueOf() {
        assertEquals(ControlTyp.C_VOLTMETER, ControlTyp.valueOf("C_VOLTMETER"));
        assertEquals(ControlTyp.C_CONST, ControlTyp.valueOf("C_CONST"));
        assertEquals(ControlTyp.C_GAIN, ControlTyp.valueOf("C_GAIN"));
    }

    @Test
    public void testTypeNumberUniqueness() {
        ControlTyp[] values = ControlTyp.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                // Special types that override getTypeNumber() may have the same number
                // so we only check for absolute uniqueness in normal cases
                if (!isSpecialOverride(values[i]) && !isSpecialOverride(values[j])) {
                    assertNotEquals("Type numbers should be unique",
                            values[i].getTypeNumber(), values[j].getTypeNumber());
                }
            }
        }
    }

    @Test
    public void testGetFromIntNumberRoundTrip() {
        ControlTyp original = ControlTyp.C_GAIN;
        int typeNumber = original.getTypeNumber();
        ControlTyp retrieved = ControlTyp.getFromIntNumber(typeNumber);
        assertEquals(original, retrieved);
    }

    @Test
    public void testControlMultiplicityTypeNumber() {
        // C_ADD type number
        int addType = ControlTyp.C_ADD.getTypeNumber();
        assertTrue(addType > 0);
    }

    @Test
    public void testLogicalOperatorsExist() {
        assertNotNull(ControlTyp.C_AND);
        assertNotNull(ControlTyp.C_OR);
        assertNotNull(ControlTyp.C_XOR);
        assertNotNull(ControlTyp.C_NOT);
    }

    @Test
    public void testComparisonOperatorsExist() {
        assertNotNull(ControlTyp.C_GE);
        assertNotNull(ControlTyp.C_GT);
        assertNotNull(ControlTyp.C_EQ);
        assertNotNull(ControlTyp.C_NE);
    }

    @Test
    public void testMathOperatorsExist() {
        assertNotNull(ControlTyp.C_SIN);
        assertNotNull(ControlTyp.C_COS);
        assertNotNull(ControlTyp.C_TAN);
        assertNotNull(ControlTyp.C_EXP);
        assertNotNull(ControlTyp.C_LN);
        assertNotNull(ControlTyp.C_SQR);
        assertNotNull(ControlTyp.C_SQRT);
        assertNotNull(ControlTyp.C_POW);
    }

    /**
     * Check if a type has overridden the getTypeNumber method (special cases).
     */
    private boolean isSpecialOverride(ControlTyp typ) {
        // C_SOURCE_IMPORT_DATA and C_SOURCE_RANDOM override getTypeNumber()
        return typ == ControlTyp.C_SOURCE_IMPORT_DATA || typ == ControlTyp.C_SOURCE_RANDOM;
    }
}
