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
 * Unit tests for CircuitSourceType enum.
 * Tests all circuit source types, ID mappings, and string representations.
 */
public class CircuitSourceTypeTest {

    @Test
    public void testEnumValuesExist() {
        assertEquals(7, CircuitSourceType.values().length);
        assertNotNull(CircuitSourceType.QUELLE_DC);
        assertNotNull(CircuitSourceType.QUELLE_SIN);
        assertNotNull(CircuitSourceType.QUELLE_SIGNALGESTEUERT);
        assertNotNull(CircuitSourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY);
        assertNotNull(CircuitSourceType.QUELLE_DIDTCURRENTCONTROLLED);
        assertNotNull(CircuitSourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY);
        assertNotNull(CircuitSourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER);
    }

    @Test
    public void testGetFromOldID_ValidIDs() {
        assertEquals(CircuitSourceType.QUELLE_DC, CircuitSourceType.getFromID(401));
        assertEquals(CircuitSourceType.QUELLE_SIN, CircuitSourceType.getFromID(402));
        assertEquals(CircuitSourceType.QUELLE_SIGNALGESTEUERT, CircuitSourceType.getFromID(400));
    }

    @Test
    public void testGetFromNewID_ValidIDs() {
        assertEquals(CircuitSourceType.QUELLE_DC, CircuitSourceType.getFromID(0));
        assertEquals(CircuitSourceType.QUELLE_SIN, CircuitSourceType.getFromID(1));
        assertEquals(CircuitSourceType.QUELLE_SIGNALGESTEUERT, CircuitSourceType.getFromID(2));
    }

    @Test
    public void testToString() {
        assertEquals("DC", CircuitSourceType.QUELLE_DC.toString());
        assertEquals("SIN", CircuitSourceType.QUELLE_SIN.toString());
        assertEquals("SIGNAL", CircuitSourceType.QUELLE_SIGNALGESTEUERT.toString());
        assertEquals("CURRENT_CONTROLLED", CircuitSourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY.toString());
        assertEquals("DI_DT_CONTROLLED", CircuitSourceType.QUELLE_DIDTCURRENTCONTROLLED.toString());
        assertEquals("VOLTAGE_CONTROLLED", CircuitSourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY.toString());
        assertEquals("TRANSFORMER", CircuitSourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER.toString());
    }

    @Test
    public void testGetOldGeckoID() {
        assertEquals(401, CircuitSourceType.QUELLE_DC.getOldGeckoID());
        assertEquals(402, CircuitSourceType.QUELLE_SIN.getOldGeckoID());
        assertEquals(400, CircuitSourceType.QUELLE_SIGNALGESTEUERT.getOldGeckoID());
        assertEquals(396, CircuitSourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY.getOldGeckoID());
        assertEquals(397, CircuitSourceType.QUELLE_DIDTCURRENTCONTROLLED.getOldGeckoID());
        assertEquals(399, CircuitSourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY.getOldGeckoID());
        assertEquals(398, CircuitSourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER.getOldGeckoID());
    }

    @Test
    public void testGetNewID() {
        assertEquals(0, CircuitSourceType.QUELLE_DC.getNewID(), 0.0);
        assertEquals(1, CircuitSourceType.QUELLE_SIN.getNewID(), 0.0);
        assertEquals(2, CircuitSourceType.QUELLE_SIGNALGESTEUERT.getNewID(), 0.0);
        assertEquals(3, CircuitSourceType.QUELLE_CURRENTCONTROLLED_DIRECTLY.getNewID(), 0.0);
        assertEquals(4, CircuitSourceType.QUELLE_DIDTCURRENTCONTROLLED.getNewID(), 0.0);
        assertEquals(5, CircuitSourceType.QUELLE_VOLTAGECONTROLLED_DIRECTLY.getNewID(), 0.0);
        assertEquals(6, CircuitSourceType.QUELLE_VOLTAGECONTROLLED_TRANSFORMER.getNewID(), 0.0);
    }

    @Test
    public void testAllTypesHaveDistinctIDs() {
        CircuitSourceType[] values = CircuitSourceType.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals("Old IDs should be distinct",
                        values[i].getOldGeckoID(), values[j].getOldGeckoID());
                assertNotEquals("New IDs should be distinct",
                        values[i].getNewID(), values[j].getNewID(), 0.0);
            }
        }
    }

    @Test
    public void testAllTypesHaveDistinctStrings() {
        CircuitSourceType[] values = CircuitSourceType.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals("Strings should be distinct",
                        values[i].toString(), values[j].toString());
            }
        }
    }
}
