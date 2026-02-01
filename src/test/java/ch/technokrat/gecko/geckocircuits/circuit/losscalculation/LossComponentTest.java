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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for LossComponent enum - differentiates between loss types.
 * LossComponent allows display of switching, conduction, or total losses.
 */
public class LossComponentTest {

    // ====================================================
    // toString() Tests
    // ====================================================

    @Test
    public void testToString_Total() {
        assertEquals("Total losses", LossComponent.TOTAL.toString());
    }

    @Test
    public void testToString_Conduction() {
        assertEquals("Conduction losses", LossComponent.CONDUCTION.toString());
    }

    @Test
    public void testToString_Switching() {
        assertEquals("Switching losses", LossComponent.SWITCHING.toString());
    }

    // ====================================================
    // getSaveString() Tests
    // ====================================================

    @Test
    public void testGetSaveString_Total() {
        assertEquals("total", LossComponent.TOTAL.getSaveString());
    }

    @Test
    public void testGetSaveString_Conduction() {
        assertEquals("conduction", LossComponent.CONDUCTION.getSaveString());
    }

    @Test
    public void testGetSaveString_Switching() {
        assertEquals("switching", LossComponent.SWITCHING.getSaveString());
    }

    // ====================================================
    // getEnumFromSaveString() Tests
    // ====================================================

    @Test
    public void testGetEnumFromSaveString_Total() {
        LossComponent result = LossComponent.getEnumFromSaveString("total");
        assertEquals(LossComponent.TOTAL, result);
    }

    @Test
    public void testGetEnumFromSaveString_Conduction() {
        LossComponent result = LossComponent.getEnumFromSaveString("conduction");
        assertEquals(LossComponent.CONDUCTION, result);
    }

    @Test
    public void testGetEnumFromSaveString_Switching() {
        LossComponent result = LossComponent.getEnumFromSaveString("switching");
        assertEquals(LossComponent.SWITCHING, result);
    }

    @Test
    public void testGetEnumFromSaveString_InvalidString_DefaultsToTotal() {
        LossComponent result = LossComponent.getEnumFromSaveString("invalid");
        assertEquals(LossComponent.TOTAL, result);
    }

    @Test
    public void testGetEnumFromSaveString_EmptyString_DefaultsToTotal() {
        LossComponent result = LossComponent.getEnumFromSaveString("");
        assertEquals(LossComponent.TOTAL, result);
    }

    @Test
    public void testGetEnumFromSaveString_NullString_DefaultsToTotal() {
        LossComponent result = LossComponent.getEnumFromSaveString(null);
        assertEquals(LossComponent.TOTAL, result);
    }

    @Test
    public void testGetEnumFromSaveString_CaseSensitive_InvalidIfDifferentCase() {
        LossComponent result = LossComponent.getEnumFromSaveString("TOTAL");
        // Case mismatch should default to TOTAL (but for different reason)
        assertEquals(LossComponent.TOTAL, result);
    }

    // ====================================================
    // Enum Values Tests
    // ====================================================

    @Test
    public void testEnumValues_HasThreeValues() {
        LossComponent[] values = LossComponent.values();
        assertEquals(3, values.length);
    }

    @Test
    public void testEnumValues_Contains_Total() {
        boolean found = false;
        for (LossComponent component : LossComponent.values()) {
            if (component == LossComponent.TOTAL) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testEnumValues_Contains_Conduction() {
        boolean found = false;
        for (LossComponent component : LossComponent.values()) {
            if (component == LossComponent.CONDUCTION) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testEnumValues_Contains_Switching() {
        boolean found = false;
        for (LossComponent component : LossComponent.values()) {
            if (component == LossComponent.SWITCHING) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    // ====================================================
    // Round-Trip Serialization Tests
    // ====================================================

    @Test
    public void testRoundTrip_Total() {
        String saved = LossComponent.TOTAL.getSaveString();
        LossComponent restored = LossComponent.getEnumFromSaveString(saved);
        assertEquals(LossComponent.TOTAL, restored);
    }

    @Test
    public void testRoundTrip_Conduction() {
        String saved = LossComponent.CONDUCTION.getSaveString();
        LossComponent restored = LossComponent.getEnumFromSaveString(saved);
        assertEquals(LossComponent.CONDUCTION, restored);
    }

    @Test
    public void testRoundTrip_Switching() {
        String saved = LossComponent.SWITCHING.getSaveString();
        LossComponent restored = LossComponent.getEnumFromSaveString(saved);
        assertEquals(LossComponent.SWITCHING, restored);
    }

    // ====================================================
    // Uniqueness Tests
    // ====================================================

    @Test
    public void testSaveStrings_AllUnique() {
        String totalStr = LossComponent.TOTAL.getSaveString();
        String conductionStr = LossComponent.CONDUCTION.getSaveString();
        String switchingStr = LossComponent.SWITCHING.getSaveString();

        assertNotEquals(totalStr, conductionStr);
        assertNotEquals(totalStr, switchingStr);
        assertNotEquals(conductionStr, switchingStr);
    }

    @Test
    public void testToStrings_AllUnique() {
        String totalStr = LossComponent.TOTAL.toString();
        String conductionStr = LossComponent.CONDUCTION.toString();
        String switchingStr = LossComponent.SWITCHING.toString();

        assertNotEquals(totalStr, conductionStr);
        assertNotEquals(totalStr, switchingStr);
        assertNotEquals(conductionStr, switchingStr);
    }

}
