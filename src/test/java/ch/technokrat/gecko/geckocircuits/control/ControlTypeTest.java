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
 * Unit tests for ControlType enum.
 */
public class ControlTypeTest {

    @Test
    public void testAllValuesExist() {
        ControlType[] values = ControlType.values();
        assertEquals(3, values.length);
    }

    @Test
    public void testTransferValue() {
        assertNotNull(ControlType.TRANSFER);
        assertEquals("TRANSFER", ControlType.TRANSFER.toString());
    }

    @Test
    public void testSinkValue() {
        assertNotNull(ControlType.SINK);
        assertEquals("SINK", ControlType.SINK.toString());
    }

    @Test
    public void testSourceValue() {
        assertNotNull(ControlType.SOURCE);
        assertEquals("SOURCE", ControlType.SOURCE.toString());
    }

    @Test
    public void testOrdinalValues() {
        assertEquals(0, ControlType.TRANSFER.ordinal());
        assertEquals(1, ControlType.SINK.ordinal());
        assertEquals(2, ControlType.SOURCE.ordinal());
    }

    @Test
    public void testValueOf() {
        assertEquals(ControlType.TRANSFER, ControlType.valueOf("TRANSFER"));
        assertEquals(ControlType.SINK, ControlType.valueOf("SINK"));
        assertEquals(ControlType.SOURCE, ControlType.valueOf("SOURCE"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfInvalidName() {
        ControlType.valueOf("INVALID_TYPE");
    }

    @Test
    public void testEnumEquality() {
        ControlType type1 = ControlType.TRANSFER;
        ControlType type2 = ControlType.TRANSFER;
        assertEquals(type1, type2);
        assertSame(type1, type2);
    }

    @Test
    public void testEnumInequality() {
        assertNotEquals(ControlType.TRANSFER, ControlType.SINK);
        assertNotEquals(ControlType.SINK, ControlType.SOURCE);
        assertNotEquals(ControlType.SOURCE, ControlType.TRANSFER);
    }
}
