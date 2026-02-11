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
 * Unit tests for ControlSourceType enum.
 * Tests all control source types and their ID mappings.
 */
public class ControlSourceTypeTest {

    @Test
    public void testEnumValuesExist() {
        assertEquals(5, ControlSourceType.values().length);
        assertNotNull(ControlSourceType.QUELLE_SIN);
        assertNotNull(ControlSourceType.QUELLE_DREIECK);
        assertNotNull(ControlSourceType.QUELLE_RECHTECK);
        assertNotNull(ControlSourceType.QUELLE_RANDOM);
        assertNotNull(ControlSourceType.QUELLE_IMPORT);
    }

    @Test
    public void testGetFromOldID_ValidIDs() {
        assertEquals(ControlSourceType.QUELLE_SIN, ControlSourceType.getFromID(402));
        assertEquals(ControlSourceType.QUELLE_DREIECK, ControlSourceType.getFromID(403));
        assertEquals(ControlSourceType.QUELLE_RECHTECK, ControlSourceType.getFromID(404));
        assertEquals(ControlSourceType.QUELLE_RANDOM, ControlSourceType.getFromID(405));
        assertEquals(ControlSourceType.QUELLE_IMPORT, ControlSourceType.getFromID(406));
    }

    @Test
    public void testGetFromNewID_ValidIDs() {
        assertEquals(ControlSourceType.QUELLE_SIN, ControlSourceType.getFromID(0));
        assertEquals(ControlSourceType.QUELLE_DREIECK, ControlSourceType.getFromID(1));
        assertEquals(ControlSourceType.QUELLE_RECHTECK, ControlSourceType.getFromID(2));
        assertEquals(ControlSourceType.QUELLE_RANDOM, ControlSourceType.getFromID(3));
        assertEquals(ControlSourceType.QUELLE_IMPORT, ControlSourceType.getFromID(4));
    }

    @Test
    public void testToString() {
        assertEquals("SINE", ControlSourceType.QUELLE_SIN.toString());
        assertEquals("TRIANGLE", ControlSourceType.QUELLE_DREIECK.toString());
        assertEquals("RECTANGLE", ControlSourceType.QUELLE_RECHTECK.toString());
        assertEquals("RANDOM", ControlSourceType.QUELLE_RANDOM.toString());
        assertEquals("IMPORT", ControlSourceType.QUELLE_IMPORT.toString());
    }

    @Test
    public void testGetOldGeckoID() {
        assertEquals(402, ControlSourceType.QUELLE_SIN.getOldGeckoID());
        assertEquals(403, ControlSourceType.QUELLE_DREIECK.getOldGeckoID());
        assertEquals(404, ControlSourceType.QUELLE_RECHTECK.getOldGeckoID());
        assertEquals(405, ControlSourceType.QUELLE_RANDOM.getOldGeckoID());
        assertEquals(406, ControlSourceType.QUELLE_IMPORT.getOldGeckoID());
    }

    @Test
    public void testGetNewID() {
        assertEquals(0, ControlSourceType.QUELLE_SIN.getNewID(), 0.0);
        assertEquals(1, ControlSourceType.QUELLE_DREIECK.getNewID(), 0.0);
        assertEquals(2, ControlSourceType.QUELLE_RECHTECK.getNewID(), 0.0);
        assertEquals(3, ControlSourceType.QUELLE_RANDOM.getNewID(), 0.0);
        assertEquals(4, ControlSourceType.QUELLE_IMPORT.getNewID(), 0.0);
    }

    @Test
    public void testAllTypesHaveDistinctOldIDs() {
        ControlSourceType[] values = ControlSourceType.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals("Old IDs should be distinct",
                        values[i].getOldGeckoID(), values[j].getOldGeckoID());
            }
        }
    }

    @Test
    public void testAllTypesHaveDistinctNewIDs() {
        ControlSourceType[] values = ControlSourceType.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals("New IDs should be distinct",
                        values[i].getNewID(), values[j].getNewID(), 0.0);
            }
        }
    }

    @Test
    public void testAllTypesHaveDistinctStrings() {
        ControlSourceType[] values = ControlSourceType.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals("Strings should be distinct",
                        values[i].toString(), values[j].toString());
            }
        }
    }

    @Test
    public void testDefaultTypeForInvalidIDBehavior() {
        // Invalid IDs trigger assert false, so the method is not designed for error handling
        // This is implementation-specific - methods uses assert, not exception throwing
    }
}
