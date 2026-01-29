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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for LabelPriority enum.
 * Tests priority ordering and comparison methods.
 */
public class LabelPriorityTest {

    @Test
    public void testEnumValuesExist() {
        assertEquals(4, LabelPriority.values().length);
        assertNotNull(LabelPriority.EMPTY_STRING);
        assertNotNull(LabelPriority.LOW);
        assertNotNull(LabelPriority.NORMAL);
        assertNotNull(LabelPriority.FORCE_NAME);
    }

    @Test
    public void testPriorityNumericValues() {
        assertEquals(0, LabelPriority.EMPTY_STRING.ordinal());
        assertEquals(1, LabelPriority.LOW.ordinal());
        assertEquals(2, LabelPriority.NORMAL.ordinal());
        assertEquals(3, LabelPriority.FORCE_NAME.ordinal());
    }

    @Test
    public void testGetHighestPriorityEqualValues() {
        LabelPriority result = LabelPriority.getHighesPriority(LabelPriority.LOW, LabelPriority.LOW);
        assertEquals(LabelPriority.LOW, result);
    }

    @Test
    public void testGetHighestPriorityFirstHigher() {
        LabelPriority result = LabelPriority.getHighesPriority(LabelPriority.FORCE_NAME, LabelPriority.LOW);
        assertEquals(LabelPriority.FORCE_NAME, result);
    }

    @Test
    public void testGetHighestPrioritySecondHigher() {
        LabelPriority result = LabelPriority.getHighesPriority(LabelPriority.LOW, LabelPriority.FORCE_NAME);
        assertEquals(LabelPriority.FORCE_NAME, result);
    }

    @Test
    public void testGetHighestPriorityExtreme() {
        LabelPriority result = LabelPriority.getHighesPriority(LabelPriority.EMPTY_STRING, LabelPriority.FORCE_NAME);
        assertEquals(LabelPriority.FORCE_NAME, result);
    }

    @Test
    public void testIsBiggerThanTrue() {
        assertTrue(LabelPriority.FORCE_NAME.isBiggerThan(LabelPriority.LOW));
        assertTrue(LabelPriority.NORMAL.isBiggerThan(LabelPriority.EMPTY_STRING));
    }

    @Test
    public void testIsBiggerThanFalse() {
        assertFalse(LabelPriority.LOW.isBiggerThan(LabelPriority.FORCE_NAME));
        assertFalse(LabelPriority.EMPTY_STRING.isBiggerThan(LabelPriority.NORMAL));
    }

    @Test
    public void testIsBiggerThanEqual() {
        assertFalse(LabelPriority.LOW.isBiggerThan(LabelPriority.LOW));
        assertFalse(LabelPriority.FORCE_NAME.isBiggerThan(LabelPriority.FORCE_NAME));
    }

    @Test
    public void testIsBiggerThanAllCombinations() {
        LabelPriority[] values = LabelPriority.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values.length; j++) {
                boolean isBigger = values[i].isBiggerThan(values[j]);
                if (i > j) {
                    assertTrue(values[i].name() + " should be bigger than " + values[j].name(), isBigger);
                } else {
                    assertFalse(values[i].name() + " should not be bigger than " + values[j].name(), isBigger);
                }
            }
        }
    }

    @Test
    public void testPriorityOrdering() {
        // Verify the order is as expected
        assertTrue(LabelPriority.LOW.isBiggerThan(LabelPriority.EMPTY_STRING));
        assertTrue(LabelPriority.NORMAL.isBiggerThan(LabelPriority.LOW));
        assertTrue(LabelPriority.FORCE_NAME.isBiggerThan(LabelPriority.NORMAL));
    }

    @Test
    public void testPriorityNames() {
        assertEquals("EMPTY_STRING", LabelPriority.EMPTY_STRING.name());
        assertEquals("LOW", LabelPriority.LOW.name());
        assertEquals("NORMAL", LabelPriority.NORMAL.name());
        assertEquals("FORCE_NAME", LabelPriority.FORCE_NAME.name());
    }

    @Test
    public void testValueOf() {
        assertEquals(LabelPriority.EMPTY_STRING, LabelPriority.valueOf("EMPTY_STRING"));
        assertEquals(LabelPriority.LOW, LabelPriority.valueOf("LOW"));
        assertEquals(LabelPriority.NORMAL, LabelPriority.valueOf("NORMAL"));
        assertEquals(LabelPriority.FORCE_NAME, LabelPriority.valueOf("FORCE_NAME"));
    }
}
