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
package gecko.geckocircuits.circuit.losscalculation;

import gecko.core.circuit.losscalculation.LeitverlusteMesskurve;
import gecko.core.circuit.losscalculation.SwitchingLossCurve;
import gecko.core.allg.GeckoFile;
import gecko.geckocircuits.general.GlobalFilePathes;
import gecko.geckocircuits.circuit.circuitcomponents.Diode;
import gecko.core.circuit.losscalculation.LossFileAccessor;
import gecko.core.circuit.losscalculation.LossCalculationDetail;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for LossCalculationDetailed file I/O methods using a mock LossFileAccessor.
 * These methods were previously untestable because they accessed MainWindow._fileManager
 * and MainWindow.getOpenFileName() directly.
 */
public class LossCalculationDetailedFileOpsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private LossFileAccessor mockAccessor;
    private Diode testDiode;
    private LossProperties lossProperties;
    private LossCalculationDetailed verlust;

    @Before
    public void setUp() {
        mockAccessor = mock(LossFileAccessor.class);
        testDiode = new Diode();
        lossProperties = testDiode.getVerlustBerechnung();
        // Create instance with injected mock accessor
        verlust = new LossCalculationDetailed(testDiode, lossProperties, mockAccessor);
    }

    // ===========================================
    // removeLossFile Tests
    // ===========================================

    @Test
    public void testRemoveLossFile_WhenLossFileIsNull_DoesNotCallMaintain() {
        assertNull("lossFile should initially be null", verlust.lossFile);

        verlust.removeLossFile();

        verify(mockAccessor, never()).maintain(any(GeckoFile.class));
        assertNull("lossFile should still be null", verlust.lossFile);
    }

    @Test
    public void testRemoveLossFile_WhenLossFileExists_CallsMaintainAndNulls() throws Exception {
        // Set up a loss file on the instance
        File tmpFile = tempFolder.newFile("test.scl");
        writeLossFileContent(tmpFile);
        when(mockAccessor.getOpenFileName()).thenReturn("Untitled");
        GeckoFile geckoFile = new GeckoFile(tmpFile, GeckoFile.StorageType.INTERNAL, "Untitled");
        verlust.lossFile = geckoFile;

        verlust.removeLossFile();

        verify(mockAccessor).maintain(geckoFile);
        assertNull("lossFile should be null after removal", verlust.lossFile);
    }

    // ===========================================
    // leseDetailVerlusteVonDatei Tests
    // ===========================================

    @Test
    public void testLeseDetailVerlusteVonDatei_PlainAsciiFile_ParsesCurves() throws Exception {
        File tmpFile = tempFolder.newFile("losses.scl");
        writeLossFileContent(tmpFile);
        GeckoFile newLossFile = new GeckoFile(tmpFile, GeckoFile.StorageType.INTERNAL, "Untitled");

        boolean result = verlust.leseDetailVerlusteVonDatei(newLossFile);

        assertTrue("Should return true on success", result);
        assertSame("lossFile should be set to newLossFile", newLossFile, verlust.lossFile);
        verify(mockAccessor).addFile(newLossFile);
    }

    @Test
    public void testLeseDetailVerlusteVonDatei_ReplacesOldLossFile() throws Exception {
        // Set up an existing loss file
        File oldFile = tempFolder.newFile("old.scl");
        writeLossFileContent(oldFile);
        GeckoFile oldGeckoFile = new GeckoFile(oldFile, GeckoFile.StorageType.INTERNAL, "Untitled");
        oldGeckoFile.setUser(testDiode.getUniqueObjectIdentifier());
        verlust.lossFile = oldGeckoFile;

        // Now read a new loss file
        File newFile = tempFolder.newFile("new.scl");
        writeLossFileContent(newFile);
        GeckoFile newGeckoFile = new GeckoFile(newFile, GeckoFile.StorageType.INTERNAL, "Untitled");

        boolean result = verlust.leseDetailVerlusteVonDatei(newGeckoFile);

        assertTrue("Should return true on success", result);
        verify(mockAccessor).maintain(oldGeckoFile);
        verify(mockAccessor).addFile(newGeckoFile);
        assertSame("lossFile should be the new file", newGeckoFile, verlust.lossFile);
    }

    @Test
    public void testLeseDetailVerlusteVonDatei_ParsesSwitchingCurves() throws Exception {
        File tmpFile = tempFolder.newFile("losses_with_curves.scl");
        writeLossFileContentWithCurves(tmpFile);
        GeckoFile newLossFile = new GeckoFile(tmpFile, GeckoFile.StorageType.INTERNAL, "Untitled");

        verlust.leseDetailVerlusteVonDatei(newLossFile);

        List<SwitchingLossCurve> switchCurves = verlust.getCopyOfSchaltverlusteMesskurvenArray();
        assertFalse("Should have switching curves", switchCurves.isEmpty());
    }

    @Test
    public void testLeseDetailVerlusteVonDatei_ParsesConductionCurves() throws Exception {
        File tmpFile = tempFolder.newFile("losses_cond.scl");
        writeLossFileContentWithCurves(tmpFile);
        GeckoFile newLossFile = new GeckoFile(tmpFile, GeckoFile.StorageType.INTERNAL, "Untitled");

        verlust.leseDetailVerlusteVonDatei(newLossFile);

        List<LeitverlusteMesskurve> condCurves = verlust.getCopyOfLeitverlusteMesskurvenArray();
        assertFalse("Should have conduction curves", condCurves.isEmpty());
    }

    // ===========================================
    // initLossFile Tests
    // ===========================================

    @Test
    public void testInitLossFile_WithZeroHash_DoesNothing() throws Exception {
        // Default hash is 0 — should do nothing
        verlust.initLossFile();

        verify(mockAccessor, never()).getFile(anyLong());
    }

    @Test
    public void testInitLossFile_WithValidHash_LooksUpFile() throws Exception {
        // Set up a hash value via importASCII
        File tmpFile = tempFolder.newFile("hashed.scl");
        writeLossFileContent(tmpFile);
        GeckoFile geckoFile = new GeckoFile(tmpFile, GeckoFile.StorageType.INTERNAL, "Untitled");
        long hash = geckoFile.getHashValue();

        // Export with a lossFile to capture the hash, then import it
        verlust.lossFile = geckoFile;
        StringBuffer ascii = new StringBuffer();
        verlust.exportASCII(ascii);
        verlust.lossFile = null;

        // Parse back the hash
        String[] lines = ascii.toString().split("\n");
        gecko.core.circuit.TokenMap tokenMap = new gecko.core.circuit.TokenMap(lines);
        verlust.importASCII(tokenMap);

        // Set up mock to return the file
        when(mockAccessor.getFile(hash)).thenReturn(geckoFile);

        verlust.initLossFile();

        verify(mockAccessor).getFile(hash);
        verify(mockAccessor).addFile(geckoFile);
    }

    @Test
    public void testInitLossFile_WhenFileNotFound_FallsBackToFilenameRecovery() throws Exception {
        // Set a non-zero hash by simulating import
        StringBuffer ascii = new StringBuffer();
        ascii.append("\ndatnamGemesseneVerluste not_defined");
        ascii.append("\nlossFileHashValue 12345");
        String[] lines = ascii.toString().split("\n");
        gecko.core.circuit.TokenMap tokenMap = new gecko.core.circuit.TokenMap(lines);
        verlust.importASCII(tokenMap);

        when(mockAccessor.getFile(12345L)).thenThrow(new FileNotFoundException("not found"));
        when(mockAccessor.getOpenFileName()).thenReturn("Untitled");

        // Should not throw — falls back to readLossesFromFileAndSetDetailedLossType
        verlust.initLossFile();

        verify(mockAccessor).getFile(12345L);
    }

    // ===========================================
    // readLossesFromFileAndSetDetailedLossType Tests
    // ===========================================

    @Test
    public void testReadLossesFromFileAndSetDetailedLossType_AbsolutePath() throws Exception {
        File tmpFile = tempFolder.newFile("absolute.scl");
        writeLossFileContent(tmpFile);
        when(mockAccessor.getOpenFileName()).thenReturn("Untitled");

        verlust.readLossesFromFileAndSetDetailedLossType(tmpFile.getAbsolutePath());

        assertNotNull("lossFile should be set", verlust.lossFile);
        verify(mockAccessor).addFile(any(GeckoFile.class));
        assertEquals("Loss type should be set to DETAILED",
                LossCalculationDetail.DETAILED, lossProperties._lossType.getValue());
    }

    @Test
    public void testReadLossesFromFileAndSetDetailedLossType_RelativePath() throws Exception {
        File subDir = tempFolder.newFolder("model");
        File modelFile = new File(subDir, "test.ipes");
        modelFile.createNewFile();
        File lossFile = new File(subDir, "relative.scl");
        writeLossFileContent(lossFile);

        when(mockAccessor.getOpenFileName()).thenReturn(modelFile.getAbsolutePath());

        // Pass just the filename as a relative path — the first absolute attempt will fail,
        // then it should try relative to model file location
        verlust.readLossesFromFileAndSetDetailedLossType("relative.scl");

        assertNotNull("lossFile should be set via relative path recovery", verlust.lossFile);
    }

    @Test
    public void testReadLossesFromFileAndSetDetailedLossType_FileNotFound_NoException() throws Exception {
        when(mockAccessor.getOpenFileName()).thenReturn("Untitled");

        // Should not throw even when file doesn't exist
        verlust.readLossesFromFileAndSetDetailedLossType("/nonexistent/path/losses.scl");

        assertNull("lossFile should remain null", verlust.lossFile);
    }

    // ===========================================
    // schreibeDetailVerlusteAufDatei Tests
    // ===========================================

    @Test
    public void testSchreibeDetailVerlusteAufDatei_WritesFile() throws Exception {
        File outFile = new File(tempFolder.getRoot(), "output.scl");
        when(mockAccessor.getOpenFileName()).thenReturn("Untitled");

        List<SwitchingLossCurve> switchCurves = verlust.getCopyOfSchaltverlusteMesskurvenArray();
        List<LeitverlusteMesskurve> condCurves = verlust.getCopyOfLeitverlusteMesskurvenArray();

        boolean result = verlust.schreibeDetailVerlusteAufDatei(
                outFile.getAbsolutePath(), switchCurves, condCurves, GeckoFile.StorageType.INTERNAL);

        assertTrue("Should return true on success", result);
        assertTrue("Output file should exist", outFile.exists());
        assertTrue("Output file should have content", outFile.length() > 0);
        verify(mockAccessor).addFile(any(GeckoFile.class));
    }

    @Test
    public void testSchreibeDetailVerlusteAufDatei_ReplacesOldLossFile() throws Exception {
        // Set up existing loss file
        File oldFile = tempFolder.newFile("old_output.scl");
        writeLossFileContent(oldFile);
        GeckoFile oldGeckoFile = new GeckoFile(oldFile, GeckoFile.StorageType.INTERNAL, "Untitled");
        oldGeckoFile.setUser(testDiode.getUniqueObjectIdentifier());
        verlust.lossFile = oldGeckoFile;

        File outFile = new File(tempFolder.getRoot(), "new_output.scl");
        when(mockAccessor.getOpenFileName()).thenReturn("Untitled");

        List<SwitchingLossCurve> switchCurves = verlust.getCopyOfSchaltverlusteMesskurvenArray();
        List<LeitverlusteMesskurve> condCurves = verlust.getCopyOfLeitverlusteMesskurvenArray();

        boolean result = verlust.schreibeDetailVerlusteAufDatei(
                outFile.getAbsolutePath(), switchCurves, condCurves, GeckoFile.StorageType.INTERNAL);

        assertTrue("Should return true", result);
        verify(mockAccessor).maintain(oldGeckoFile);
        verify(mockAccessor).addFile(any(GeckoFile.class));
        assertNotSame("lossFile should be updated", oldGeckoFile, verlust.lossFile);
    }

    @Test
    public void testSchreibeDetailVerlusteAufDatei_InvalidPath_ReturnsFalse() {
        when(mockAccessor.getOpenFileName()).thenReturn("Untitled");

        boolean result = verlust.schreibeDetailVerlusteAufDatei(
                "/nonexistent/directory/output.scl",
                verlust.getCopyOfSchaltverlusteMesskurvenArray(),
                verlust.getCopyOfLeitverlusteMesskurvenArray(),
                GeckoFile.StorageType.INTERNAL);

        assertFalse("Should return false on I/O error", result);
    }

    // ===========================================
    // pruefeLinkAufHalbleiterDatei Tests
    // ===========================================

    @Test
    public void testPruefeLinkAufHalbleiterDatei_NullLossFile_ReturnsFalse() {
        assertNull(verlust.lossFile);

        boolean result = verlust.pruefeLinkAufHalbleiterDatei();

        assertFalse("Should return false when lossFile is null", result);
    }

    @Test
    public void testPruefeLinkAufHalbleiterDatei_ExternalFile_Exists_ReturnsTrue() throws Exception {
        File tmpFile = tempFolder.newFile("external.scl");
        writeLossFileContent(tmpFile);
        GeckoFile geckoFile = new GeckoFile(tmpFile, GeckoFile.StorageType.EXTERNAL, "Untitled");
        verlust.lossFile = geckoFile;

        boolean result = verlust.pruefeLinkAufHalbleiterDatei();

        assertTrue("Should return true for existing external file", result);
    }

    @Test
    public void testPruefeLinkAufHalbleiterDatei_ExternalFile_NotExists_ReturnsFalse() throws Exception {
        File tmpFile = tempFolder.newFile("willdelete.scl");
        writeLossFileContent(tmpFile);
        GeckoFile geckoFile = new GeckoFile(tmpFile, GeckoFile.StorageType.EXTERNAL, "Untitled");
        verlust.lossFile = geckoFile;
        // Delete the file after creating GeckoFile
        tmpFile.delete();

        boolean result = verlust.pruefeLinkAufHalbleiterDatei();

        assertFalse("Should return false for non-existing external file", result);
    }

    @Test
    public void testPruefeLinkAufHalbleiterDatei_InternalFile_FoundInManager_ReturnsTrue() throws Exception {
        File tmpFile = tempFolder.newFile("internal.scl");
        writeLossFileContent(tmpFile);
        GeckoFile geckoFile = new GeckoFile(tmpFile, GeckoFile.StorageType.INTERNAL, "Untitled");
        verlust.lossFile = geckoFile;

        // Mock the file manager returning this file in the extension list
        when(mockAccessor.getFilesByExtension(".scl")).thenReturn(Collections.singletonList(geckoFile));

        boolean result = verlust.pruefeLinkAufHalbleiterDatei();

        assertTrue("Should return true when internal file found in manager", result);
        verify(mockAccessor).getFilesByExtension(".scl");
    }

    @Test
    public void testPruefeLinkAufHalbleiterDatei_InternalFile_NotInManager_ReturnsFalse() throws Exception {
        File tmpFile = tempFolder.newFile("internal_missing.scl");
        writeLossFileContent(tmpFile);
        GeckoFile geckoFile = new GeckoFile(tmpFile, GeckoFile.StorageType.INTERNAL, "Untitled");
        verlust.lossFile = geckoFile;

        // Mock empty list — file not found in manager
        when(mockAccessor.getFilesByExtension(".scl")).thenReturn(new ArrayList<>());

        boolean result = verlust.pruefeLinkAufHalbleiterDatei();

        assertFalse("Should return false when internal file not in manager", result);
    }

    // ===========================================
    // Constructor injection Tests
    // ===========================================

    @Test
    public void testDefaultConstructor_CreatesMainWindowAccessor() {
        // The default constructor creates a MainWindowLossFileAccessor.
        // We can only verify that the object is created successfully.
        Diode diode = new Diode();
        LossCalculationDetailed defaultInstance = diode.getVerlustBerechnung().getDetailedLosses();
        assertNotNull("Default constructor should create valid instance", defaultInstance);
    }

    @Test
    public void testInjectedConstructor_UsesProvidedAccessor() throws Exception {
        // Verify that our test instance uses the mock accessor
        File tmpFile = tempFolder.newFile("verify_injection.scl");
        writeLossFileContent(tmpFile);
        GeckoFile geckoFile = new GeckoFile(tmpFile, GeckoFile.StorageType.INTERNAL, "Untitled");
        verlust.lossFile = geckoFile;

        verlust.removeLossFile();

        // Verifies mock was called, proving injection works
        verify(mockAccessor).maintain(geckoFile);
    }

    // ===========================================
    // Helper Methods
    // ===========================================

    /**
     * Writes valid loss file content using the actual export methods to ensure correct format.
     * Uses a single switching curve + single conduction curve.
     */
    private void writeLossFileContent(File file) throws Exception {
        String content = generateLossFileContent(
                verlust.getCopyOfSchaltverlusteMesskurvenArray().subList(0, 1),
                verlust.getCopyOfLeitverlusteMesskurvenArray().subList(0, 1));
        try (BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            out.write(content);
        }
    }

    /**
     * Writes loss file content with all default curves (2 switching + 2 conduction).
     */
    private void writeLossFileContentWithCurves(File file) throws Exception {
        String content = generateLossFileContent(
                verlust.getCopyOfSchaltverlusteMesskurvenArray(),
                verlust.getCopyOfLeitverlusteMesskurvenArray());
        try (BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            out.write(content);
        }
    }

    /**
     * Generates loss file content using the actual curve export methods,
     * matching the format that LossCalculationDetailed.schreibeDetailVerlusteAufDatei produces.
     */
    private String generateLossFileContent(List<SwitchingLossCurve> switchCurves,
            List<LeitverlusteMesskurve> condCurves) {
        StringBuffer ascii = new StringBuffer();
        gecko.geckocircuits.general.ProjectData.appendAsString(
                ascii.append("\nanzMesskurvenPvSWITCH"), switchCurves.size());
        for (SwitchingLossCurve curve : switchCurves) {
            curve.exportASCII(ascii);
        }
        gecko.geckocircuits.general.ProjectData.appendAsString(
                ascii.append("\nanzMesskurvenPvCOND"), condCurves.size());
        for (LeitverlusteMesskurve curve : condCurves) {
            curve.exportASCII(ascii);
        }
        return ascii.toString();
    }
}
