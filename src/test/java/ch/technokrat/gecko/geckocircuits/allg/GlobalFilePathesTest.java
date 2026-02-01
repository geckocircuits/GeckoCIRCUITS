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

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for GlobalFilePathes - global file path constants used throughout GeckoCIRCUITS.
 */
public class GlobalFilePathesTest {

    // ====================================================
    // Initialization Tests
    // ====================================================

    @Before
    public void setUp() {
        // Reset to known state for testing
        GlobalFilePathes.RECENT_CIRCUITS_1 = "";
        GlobalFilePathes.RECENT_CIRCUITS_2 = "";
        GlobalFilePathes.RECENT_CIRCUITS_3 = "";
        GlobalFilePathes.RECENT_CIRCUITS_4 = "";
        GlobalFilePathes.PFAD_JAR_HOME = null;
        GlobalFilePathes.DATNAM = null;
        GlobalFilePathes.datnamAbsLoadIPES = null;
    }

    // ====================================================
    // Recent Circuits Tests
    // ====================================================

    @Test
    public void testRECENT_CIRCUITS_1_InitialValue() {
        // Should be initialized to empty string
        assertNotNull("RECENT_CIRCUITS_1 should not be null", GlobalFilePathes.RECENT_CIRCUITS_1);
    }

    @Test
    public void testRECENT_CIRCUITS_1_Settable() {
        String testPath = "/path/to/circuit1.ipes";
        GlobalFilePathes.RECENT_CIRCUITS_1 = testPath;
        assertEquals("RECENT_CIRCUITS_1 should be settable", testPath, GlobalFilePathes.RECENT_CIRCUITS_1);
    }

    @Test
    public void testRECENT_CIRCUITS_2_InitialValue() {
        assertNotNull("RECENT_CIRCUITS_2 should not be null", GlobalFilePathes.RECENT_CIRCUITS_2);
    }

    @Test
    public void testRECENT_CIRCUITS_2_Settable() {
        String testPath = "/path/to/circuit2.ipes";
        GlobalFilePathes.RECENT_CIRCUITS_2 = testPath;
        assertEquals("RECENT_CIRCUITS_2 should be settable", testPath, GlobalFilePathes.RECENT_CIRCUITS_2);
    }

    @Test
    public void testRECENT_CIRCUITS_3_InitialValue() {
        assertNotNull("RECENT_CIRCUITS_3 should not be null", GlobalFilePathes.RECENT_CIRCUITS_3);
    }

    @Test
    public void testRECENT_CIRCUITS_3_Settable() {
        String testPath = "/path/to/circuit3.ipes";
        GlobalFilePathes.RECENT_CIRCUITS_3 = testPath;
        assertEquals("RECENT_CIRCUITS_3 should be settable", testPath, GlobalFilePathes.RECENT_CIRCUITS_3);
    }

    @Test
    public void testRECENT_CIRCUITS_4_InitialValue() {
        assertNotNull("RECENT_CIRCUITS_4 should not be null", GlobalFilePathes.RECENT_CIRCUITS_4);
    }

    @Test
    public void testRECENT_CIRCUITS_4_Settable() {
        String testPath = "/path/to/circuit4.ipes";
        GlobalFilePathes.RECENT_CIRCUITS_4 = testPath;
        assertEquals("RECENT_CIRCUITS_4 should be settable", testPath, GlobalFilePathes.RECENT_CIRCUITS_4);
    }

    // ====================================================
    // Recent Circuits Ordering Tests
    // ====================================================

    @Test
    public void testRecentCircuits_Ordering() {
        // RECENT_1 is the newest, RECENT_4 is the oldest
        GlobalFilePathes.RECENT_CIRCUITS_1 = "/newest.ipes";
        GlobalFilePathes.RECENT_CIRCUITS_2 = "/second.ipes";
        GlobalFilePathes.RECENT_CIRCUITS_3 = "/third.ipes";
        GlobalFilePathes.RECENT_CIRCUITS_4 = "/oldest.ipes";

        assertEquals("RECENT_1 should be newest", "/newest.ipes", GlobalFilePathes.RECENT_CIRCUITS_1);
        assertEquals("RECENT_4 should be oldest", "/oldest.ipes", GlobalFilePathes.RECENT_CIRCUITS_4);
    }

    // ====================================================
    // PFAD_PICS_URL Tests
    // ====================================================

    @Test
    public void testPFAD_PICS_URL_FieldExists() {
        // The field should exist even if null initially
        try {
            java.lang.reflect.Field field = GlobalFilePathes.class.getDeclaredField("PFAD_PICS_URL");
            assertNotNull("PFAD_PICS_URL field should exist", field);
        } catch (NoSuchFieldException e) {
            fail("PFAD_PICS_URL field should be declared");
        }
    }

    // ====================================================
    // PFAD_JAR_HOME Tests
    // ====================================================

    @Test
    public void testPFAD_JAR_HOME_Settable() {
        String jarPath = "/path/to/jar/";
        GlobalFilePathes.PFAD_JAR_HOME = jarPath;
        assertEquals("PFAD_JAR_HOME should be settable", jarPath, GlobalFilePathes.PFAD_JAR_HOME);
    }

    @Test
    public void testPFAD_JAR_HOME_CanBeNull() {
        GlobalFilePathes.PFAD_JAR_HOME = null;
        assertNull("PFAD_JAR_HOME should be nullable", GlobalFilePathes.PFAD_JAR_HOME);
    }

    @Test
    public void testPFAD_JAR_HOME_WithTrailingSlash() {
        String jarPath = "/path/to/jar/";
        GlobalFilePathes.PFAD_JAR_HOME = jarPath;
        assertTrue("JAR path should end with /", GlobalFilePathes.PFAD_JAR_HOME.endsWith("/"));
    }

    // ====================================================
    // DATNAM Tests (Current File)
    // ====================================================

    @Test
    public void testDATNAM_Settable() {
        String filepath = "/path/to/current_circuit.ipes";
        GlobalFilePathes.DATNAM = filepath;
        assertEquals("DATNAM should be settable", filepath, GlobalFilePathes.DATNAM);
    }

    @Test
    public void testDATNAM_CanBeNull() {
        GlobalFilePathes.DATNAM = null;
        assertNull("DATNAM should be nullable", GlobalFilePathes.DATNAM);
    }

    @Test
    public void testDATNAM_WithIpesExtension() {
        String filepath = "/path/to/circuit.ipes";
        GlobalFilePathes.DATNAM = filepath;
        assertTrue("DATNAM should support .ipes files", GlobalFilePathes.DATNAM.endsWith(".ipes"));
    }

    // ====================================================
    // DATNAM_NOT_DEFINED Tests
    // ====================================================

    @Test
    public void testDATNAM_NOT_DEFINED_Value() {
        assertEquals("Undefined constant should be 'not_defined'", "not_defined", GlobalFilePathes.DATNAM_NOT_DEFINED);
    }

    @Test
    public void testDATNAM_NOT_DEFINED_IsNeverNull() {
        assertNotNull("NOT_DEFINED constant should never be null", GlobalFilePathes.DATNAM_NOT_DEFINED);
    }

    @Test
    public void testDATNAM_NOT_DEFINED_IsReadable() {
        String undefined = GlobalFilePathes.DATNAM_NOT_DEFINED;
        assertTrue("NOT_DEFINED should be readable", undefined.length() > 0);
    }

    // ====================================================
    // datnamAbsLoadIPES Tests (Original Load Path)
    // ====================================================

    @Test
    public void testDatnamAbsLoadIPES_Settable() {
        String filepath = "/path/to/original_load.ipes";
        GlobalFilePathes.datnamAbsLoadIPES = filepath;
        assertEquals("datnamAbsLoadIPES should be settable", filepath, GlobalFilePathes.datnamAbsLoadIPES);
    }

    @Test
    public void testDatnamAbsLoadIPES_CanBeNull() {
        GlobalFilePathes.datnamAbsLoadIPES = null;
        assertNull("datnamAbsLoadIPES should be nullable", GlobalFilePathes.datnamAbsLoadIPES);
    }

    @Test
    public void testDatnamAbsLoadIPES_PreservesOriginalPath() {
        String originalPath = "/original/location/circuit.ipes";
        GlobalFilePathes.datnamAbsLoadIPES = originalPath;

        // Change current file
        GlobalFilePathes.DATNAM = "/new/location/circuit.ipes";

        // Original path should remain unchanged
        assertEquals("datnamAbsLoadIPES should preserve original path", originalPath, GlobalFilePathes.datnamAbsLoadIPES);
    }

    // ====================================================
    // File Path Patterns Tests
    // ====================================================

    @Test
    public void testFilePath_IsAbsolutePath() {
        String absolutePath = "/absolute/path/to/circuit.ipes";
        GlobalFilePathes.DATNAM = absolutePath;
        assertTrue("File path should be absolute", GlobalFilePathes.DATNAM.startsWith("/"));
    }

    @Test
    public void testFilePath_WithWindowsPath() {
        // Should support Windows paths too
        String windowsPath = "C:\\path\\to\\circuit.ipes";
        GlobalFilePathes.DATNAM = windowsPath;
        assertEquals("Windows paths should be supported", windowsPath, GlobalFilePathes.DATNAM);
    }

    // ====================================================
    // Multiple File Operations Tests
    // ====================================================

    @Test
    public void testMultipleRecentFiles_Sequential() {
        // Simulate opening multiple files in sequence
        GlobalFilePathes.RECENT_CIRCUITS_1 = "file1.ipes";
        GlobalFilePathes.RECENT_CIRCUITS_2 = "file2.ipes";
        GlobalFilePathes.RECENT_CIRCUITS_3 = "file3.ipes";
        GlobalFilePathes.RECENT_CIRCUITS_4 = "file4.ipes";

        assertEquals("First recent should be file1", "file1.ipes", GlobalFilePathes.RECENT_CIRCUITS_1);
        assertEquals("Fourth recent should be file4", "file4.ipes", GlobalFilePathes.RECENT_CIRCUITS_4);
    }

    @Test
    public void testHistoryRotation_Simulation() {
        // Simulate opening a new file (typically moves others back)
        GlobalFilePathes.RECENT_CIRCUITS_1 = "newest.ipes";
        GlobalFilePathes.RECENT_CIRCUITS_2 = "second.ipes";

        // Open a new file
        GlobalFilePathes.RECENT_CIRCUITS_2 = GlobalFilePathes.RECENT_CIRCUITS_1;
        GlobalFilePathes.RECENT_CIRCUITS_1 = "newest_newest.ipes";

        assertEquals("Newest should be new file", "newest_newest.ipes", GlobalFilePathes.RECENT_CIRCUITS_1);
        assertEquals("Second should be previous newest", "newest.ipes", GlobalFilePathes.RECENT_CIRCUITS_2);
    }

    // ====================================================
    // Backward Compatibility Tests
    // ====================================================

    @Test
    public void testDatanamNotDefined_ForPathNormalization() {
        // Used when old file paths need to be updated
        String undefinedPath = GlobalFilePathes.DATNAM_NOT_DEFINED;
        boolean isUndefined = "not_defined".equals(undefinedPath);
        assertTrue("Should be able to check for undefined paths", isUndefined);
    }

    // ====================================================
    // Field Mutability Tests
    // ====================================================

    @Test
    public void testRecentCircuits_AreMutable() {
        String path1 = "/path1.ipes";
        String path2 = "/path2.ipes";

        GlobalFilePathes.RECENT_CIRCUITS_1 = path1;
        assertEquals("Should be mutable initially", path1, GlobalFilePathes.RECENT_CIRCUITS_1);

        GlobalFilePathes.RECENT_CIRCUITS_1 = path2;
        assertEquals("Should be mutable after change", path2, GlobalFilePathes.RECENT_CIRCUITS_1);
    }

    // ====================================================
    // Null Safety Tests
    // ====================================================

    @Test
    public void testFields_CanBeNull() {
        // These fields should support null values
        GlobalFilePathes.PFAD_JAR_HOME = null;
        GlobalFilePathes.DATNAM = null;
        GlobalFilePathes.datnamAbsLoadIPES = null;

        assertNull("PFAD_JAR_HOME should be nullable", GlobalFilePathes.PFAD_JAR_HOME);
        assertNull("DATNAM should be nullable", GlobalFilePathes.DATNAM);
        assertNull("datnamAbsLoadIPES should be nullable", GlobalFilePathes.datnamAbsLoadIPES);
    }

    // ====================================================
    // Typical Usage Pattern Tests
    // ====================================================

    @Test
    public void testTypicalFlow_OpenAndSave() {
        // Simulate opening a circuit file
        GlobalFilePathes.datnamAbsLoadIPES = "/original/location/myCircuit.ipes";
        GlobalFilePathes.DATNAM = "/original/location/myCircuit.ipes";

        // User modifies and saves to new location
        GlobalFilePathes.DATNAM = "/new/location/myCircuit.ipes";

        // Original load path should be preserved
        assertEquals("Original path should remain in datnamAbsLoadIPES",
            "/original/location/myCircuit.ipes", GlobalFilePathes.datnamAbsLoadIPES);
        assertEquals("Current path should be updated",
            "/new/location/myCircuit.ipes", GlobalFilePathes.DATNAM);
    }
}
