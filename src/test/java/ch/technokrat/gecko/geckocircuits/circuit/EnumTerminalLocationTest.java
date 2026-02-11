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
 * Unit tests for EnumTerminalLocation enum.
 * Tests all terminal locations and ordinal-based lookups.
 */
public class EnumTerminalLocationTest {

    @Test
    public void testEnumValuesExist() {
        assertEquals(4, EnumTerminalLocation.values().length);
        assertNotNull(EnumTerminalLocation.UP);
        assertNotNull(EnumTerminalLocation.LEFT);
        assertNotNull(EnumTerminalLocation.RIGHT);
        assertNotNull(EnumTerminalLocation.BOTTOM);
    }

    @Test
    public void testEnumOrdinals() {
        assertEquals(0, EnumTerminalLocation.UP.ordinal());
        assertEquals(1, EnumTerminalLocation.LEFT.ordinal());
        assertEquals(2, EnumTerminalLocation.RIGHT.ordinal());
        assertEquals(3, EnumTerminalLocation.BOTTOM.ordinal());
    }

    @Test
    public void testGetFromOrdinalValidOrdinals() {
        assertEquals(EnumTerminalLocation.UP, EnumTerminalLocation.getFromOrdinal(0));
        assertEquals(EnumTerminalLocation.LEFT, EnumTerminalLocation.getFromOrdinal(1));
        assertEquals(EnumTerminalLocation.RIGHT, EnumTerminalLocation.getFromOrdinal(2));
        assertEquals(EnumTerminalLocation.BOTTOM, EnumTerminalLocation.getFromOrdinal(3));
    }

    @Test
    public void testGetFromOrdinalRoundTrip() {
        for (EnumTerminalLocation location : EnumTerminalLocation.values()) {
            EnumTerminalLocation retrieved = EnumTerminalLocation.getFromOrdinal(location.ordinal());
            assertEquals(location, retrieved);
        }
    }

    @Test
    public void testAllLocationsDistinct() {
        EnumTerminalLocation[] values = EnumTerminalLocation.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals("Locations should be distinct",
                        values[i].ordinal(), values[j].ordinal());
            }
        }
    }

    @Test
    public void testLocationNames() {
        assertEquals("UP", EnumTerminalLocation.UP.name());
        assertEquals("LEFT", EnumTerminalLocation.LEFT.name());
        assertEquals("RIGHT", EnumTerminalLocation.RIGHT.name());
        assertEquals("BOTTOM", EnumTerminalLocation.BOTTOM.name());
    }

    @Test
    public void testValueOf() {
        assertEquals(EnumTerminalLocation.UP, EnumTerminalLocation.valueOf("UP"));
        assertEquals(EnumTerminalLocation.LEFT, EnumTerminalLocation.valueOf("LEFT"));
        assertEquals(EnumTerminalLocation.RIGHT, EnumTerminalLocation.valueOf("RIGHT"));
        assertEquals(EnumTerminalLocation.BOTTOM, EnumTerminalLocation.valueOf("BOTTOM"));
    }
}
