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
package ch.technokrat.gecko;

import ch.technokrat.gecko.geckocircuits.allg.OperatingMode;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for OperatingMode enum.
 * Tests all operating mode values and enum behavior.
 */
public class OperatingModeTest {

    @Test
    public void testOperatingModeValues() {
        OperatingMode[] values = OperatingMode.values();
        assertEquals(6, values.length);
    }

    @Test
    public void testStandaloneMode() {
        OperatingMode mode = OperatingMode.STANDALONE;
        assertNotNull(mode);
        assertEquals(OperatingMode.STANDALONE, mode);
    }

    @Test
    public void testSimulinkMode() {
        OperatingMode mode = OperatingMode.SIMULINK;
        assertNotNull(mode);
        assertEquals(OperatingMode.SIMULINK, mode);
    }

    @Test
    public void testExternalMode() {
        OperatingMode mode = OperatingMode.EXTERNAL;
        assertNotNull(mode);
        assertEquals(OperatingMode.EXTERNAL, mode);
    }

    @Test
    public void testRemoteMode() {
        OperatingMode mode = OperatingMode.REMOTE;
        assertNotNull(mode);
        assertEquals(OperatingMode.REMOTE, mode);
    }

    @Test
    public void testMMFMode() {
        OperatingMode mode = OperatingMode.MMF;
        assertNotNull(mode);
        assertEquals(OperatingMode.MMF, mode);
    }

    @Test
    public void testHeadlessMode() {
        OperatingMode mode = OperatingMode.HEADLESS;
        assertNotNull(mode);
        assertEquals(OperatingMode.HEADLESS, mode);
    }

    @Test
    public void testOperatingModeToString() {
        OperatingMode mode = OperatingMode.STANDALONE;
        String str = mode.toString();
        assertNotNull(str);
        assertFalse(str.isEmpty());
    }

    @Test
    public void testEnumValueOf() {
        OperatingMode mode = OperatingMode.valueOf("REMOTE");
        assertEquals(OperatingMode.REMOTE, mode);
    }

    @Test
    public void testEnumOrdinal() {
        OperatingMode mode = OperatingMode.STANDALONE;
        assertTrue(mode.ordinal() >= 0);
        assertTrue(mode.ordinal() < OperatingMode.values().length);
    }

    @Test
    public void testModeComparison() {
        OperatingMode mode1 = OperatingMode.STANDALONE;
        OperatingMode mode2 = OperatingMode.REMOTE;
        assertNotEquals(mode1, mode2);
    }

    @Test
    public void testModeSelfEquality() {
        OperatingMode mode = OperatingMode.MMF;
        assertEquals(mode, mode);
    }

    @Test
    public void testModeHashCode() {
        OperatingMode mode1 = OperatingMode.STANDALONE;
        OperatingMode mode2 = OperatingMode.STANDALONE;
        assertEquals(mode1.hashCode(), mode2.hashCode());
    }

    @Test
    public void testHeadlessModeName() {
        OperatingMode mode = OperatingMode.HEADLESS;
        String name = mode.name();
        assertEquals("HEADLESS", name);
    }

    @Test
    public void testAllModesHaveDistinctNames() {
        OperatingMode[] modes = OperatingMode.values();
        for (int i = 0; i < modes.length; i++) {
            for (int j = i + 1; j < modes.length; j++) {
                assertNotEquals(modes[i].name(), modes[j].name());
            }
        }
    }

    @Test
    public void testModeNameFromOrdinal() {
        int standAloneOrdinal = OperatingMode.STANDALONE.ordinal();
        assertEquals("STANDALONE", OperatingMode.values()[standAloneOrdinal].name());
    }

    @Test
    public void testInvalidOperatingMode() {
        try {
            OperatingMode.valueOf("INVALID_MODE");
            fail("Should throw IllegalArgumentException for invalid mode");
        } catch (IllegalArgumentException e) {
            assertNotNull(e);
        }
    }
}
