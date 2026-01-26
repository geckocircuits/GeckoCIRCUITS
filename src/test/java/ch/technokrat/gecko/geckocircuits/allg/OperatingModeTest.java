/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.allg;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for OperatingMode enum - GeckoCIRCUITS operating mode selection.
 */
public class OperatingModeTest {

    // ====================================================
    // Enum Values Tests
    // ====================================================

    @Test
    public void testAllEnumValues_Exist() {
        OperatingMode[] values = OperatingMode.values();
        assertEquals(5, values.length);
    }

    @Test
    public void testStandalone_Exists() {
        assertNotNull(OperatingMode.STANDALONE);
    }

    @Test
    public void testSimulink_Exists() {
        assertNotNull(OperatingMode.SIMULINK);
    }

    @Test
    public void testExternal_Exists() {
        assertNotNull(OperatingMode.EXTERNAL);
    }

    @Test
    public void testRemote_Exists() {
        assertNotNull(OperatingMode.REMOTE);
    }

    @Test
    public void testMMF_Exists() {
        assertNotNull(OperatingMode.MMF);
    }

    // ====================================================
    // ValueOf Tests
    // ====================================================

    @Test
    public void testValueOf_Standalone() {
        assertEquals(OperatingMode.STANDALONE, OperatingMode.valueOf("STANDALONE"));
    }

    @Test
    public void testValueOf_Simulink() {
        assertEquals(OperatingMode.SIMULINK, OperatingMode.valueOf("SIMULINK"));
    }

    @Test
    public void testValueOf_External() {
        assertEquals(OperatingMode.EXTERNAL, OperatingMode.valueOf("EXTERNAL"));
    }

    @Test
    public void testValueOf_Remote() {
        assertEquals(OperatingMode.REMOTE, OperatingMode.valueOf("REMOTE"));
    }

    @Test
    public void testValueOf_MMF() {
        assertEquals(OperatingMode.MMF, OperatingMode.valueOf("MMF"));
    }

    // ====================================================
    // Ordinal Tests
    // ====================================================

    @Test
    public void testOrdinal_StandaloneIsZero() {
        assertEquals(0, OperatingMode.STANDALONE.ordinal());
    }

    @Test
    public void testOrdinal_SimulinkIsOne() {
        assertEquals(1, OperatingMode.SIMULINK.ordinal());
    }

    @Test
    public void testOrdinal_ExternalIsTwo() {
        assertEquals(2, OperatingMode.EXTERNAL.ordinal());
    }

    @Test
    public void testOrdinal_RemoteIsThree() {
        assertEquals(3, OperatingMode.REMOTE.ordinal());
    }

    @Test
    public void testOrdinal_MMFIsFour() {
        assertEquals(4, OperatingMode.MMF.ordinal());
    }

    // ====================================================
    // Name Tests
    // ====================================================

    @Test
    public void testName_Standalone() {
        assertEquals("STANDALONE", OperatingMode.STANDALONE.name());
    }

    @Test
    public void testName_Simulink() {
        assertEquals("SIMULINK", OperatingMode.SIMULINK.name());
    }

    @Test
    public void testName_External() {
        assertEquals("EXTERNAL", OperatingMode.EXTERNAL.name());
    }

    @Test
    public void testName_Remote() {
        assertEquals("REMOTE", OperatingMode.REMOTE.name());
    }

    @Test
    public void testName_MMF() {
        assertEquals("MMF", OperatingMode.MMF.name());
    }

    // ====================================================
    // Use Case Tests
    // ====================================================

    @Test
    public void testDefaultMode_ShouldBeStandalone() {
        // Standalone is the default mode for normal desktop operation
        assertEquals(0, OperatingMode.STANDALONE.ordinal());
    }

    @Test
    public void testModeSwitch() {
        // Simulate mode switching
        OperatingMode mode = OperatingMode.STANDALONE;
        
        // Switch to remote mode
        mode = OperatingMode.REMOTE;
        assertEquals(OperatingMode.REMOTE, mode);
        
        // Switch back to standalone
        mode = OperatingMode.STANDALONE;
        assertEquals(OperatingMode.STANDALONE, mode);
    }

    // ====================================================
    // Iteration Tests
    // ====================================================

    @Test
    public void testValues_IterateAll() {
        int count = 0;
        for (OperatingMode mode : OperatingMode.values()) {
            assertNotNull(mode);
            count++;
        }
        assertEquals(5, count);
    }

    // ====================================================
    // Comparison Tests
    // ====================================================

    @Test
    public void testEquals_SameValue() {
        assertEquals(OperatingMode.STANDALONE, OperatingMode.STANDALONE);
    }

    @Test
    public void testNotEquals_DifferentValues() {
        assertNotEquals(OperatingMode.STANDALONE, OperatingMode.SIMULINK);
    }

    @Test
    public void testCompareTo_Ordinal() {
        assertTrue(OperatingMode.STANDALONE.compareTo(OperatingMode.SIMULINK) < 0);
        assertTrue(OperatingMode.REMOTE.compareTo(OperatingMode.STANDALONE) > 0);
        assertEquals(0, OperatingMode.EXTERNAL.compareTo(OperatingMode.EXTERNAL));
    }
}
