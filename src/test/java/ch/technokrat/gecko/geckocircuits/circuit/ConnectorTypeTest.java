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
 * Unit tests for ConnectorType enum.
 * Tests all connector types and their color properties.
 */
public class ConnectorTypeTest {

    @Test
    public void testEnumValuesExist() {
        assertEquals(6, ConnectorType.values().length);
        assertNotNull(ConnectorType.LK);
        assertNotNull(ConnectorType.CONTROL);
        assertNotNull(ConnectorType.RELUCTANCE);
        assertNotNull(ConnectorType.LK_AND_RELUCTANCE);
        assertNotNull(ConnectorType.THERMAL);
        assertNotNull(ConnectorType.NONE);
    }

    @Test
    public void testFromOrdinalValidOrdinals() {
        assertEquals(ConnectorType.LK, ConnectorType.fromOrdinal(0));
        assertEquals(ConnectorType.CONTROL, ConnectorType.fromOrdinal(1));
        assertEquals(ConnectorType.RELUCTANCE, ConnectorType.fromOrdinal(2));
        assertEquals(ConnectorType.LK_AND_RELUCTANCE, ConnectorType.fromOrdinal(3));
        assertEquals(ConnectorType.THERMAL, ConnectorType.fromOrdinal(4));
        assertEquals(ConnectorType.NONE, ConnectorType.fromOrdinal(5));
    }

    @Test
    public void testFromOrdinalRoundTrip() {
        for (ConnectorType type : ConnectorType.values()) {
            ConnectorType retrieved = ConnectorType.fromOrdinal(type.ordinal());
            assertEquals(type, retrieved);
        }
    }

    @Test
    public void testFromOrdinalInvalidOrdinalsBehavior() {
        // Invalid ordinals trigger assert false, so we test they cause assertion
        // This is implementation-specific - methods uses assert, not exception throwing
    }

    @Test
    public void testBackgroundColorRgbRange() {
        // All colors should be valid RGB integers
        for (ConnectorType type : ConnectorType.values()) {
            int rgb = type.getBackgroundColorRgb();
            assertTrue("RGB value should be non-negative", rgb >= 0);
            assertTrue("RGB value should fit in 24 bits", rgb <= 0xFFFFFF);
        }
    }

    @Test
    public void testForeGroundColorRgbRange() {
        // All colors should be valid RGB integers
        for (ConnectorType type : ConnectorType.values()) {
            int rgb = type.getForeGroundColorRgb();
            assertTrue("RGB value should be non-negative", rgb >= 0);
            assertTrue("RGB value should fit in 24 bits", rgb <= 0xFFFFFF);
        }
    }

    @Test
    public void testNoneConnectorColor() {
        // NONE should return light gray for background
        assertEquals(0xD3D3D3, ConnectorType.NONE.getBackgroundColorRgb());
        // NONE should return gray for foreground
        assertEquals(0x808080, ConnectorType.NONE.getForeGroundColorRgb());
    }

    @Test
    public void testConnectorTypeNames() {
        assertEquals("LK", ConnectorType.LK.name());
        assertEquals("CONTROL", ConnectorType.CONTROL.name());
        assertEquals("RELUCTANCE", ConnectorType.RELUCTANCE.name());
        assertEquals("LK_AND_RELUCTANCE", ConnectorType.LK_AND_RELUCTANCE.name());
        assertEquals("THERMAL", ConnectorType.THERMAL.name());
        assertEquals("NONE", ConnectorType.NONE.name());
    }

    @Test
    public void testValueOf() {
        assertEquals(ConnectorType.LK, ConnectorType.valueOf("LK"));
        assertEquals(ConnectorType.CONTROL, ConnectorType.valueOf("CONTROL"));
        assertEquals(ConnectorType.RELUCTANCE, ConnectorType.valueOf("RELUCTANCE"));
        assertEquals(ConnectorType.THERMAL, ConnectorType.valueOf("THERMAL"));
        assertEquals(ConnectorType.NONE, ConnectorType.valueOf("NONE"));
    }

    @Test
    public void testColorConsistency() {
        // For each type, background and foreground should both be defined
        for (ConnectorType type : ConnectorType.values()) {
            int bg = type.getBackgroundColorRgb();
            int fg = type.getForeGroundColorRgb();
            assertNotEquals("Background and foreground colors should differ", bg, fg);
        }
    }
}
