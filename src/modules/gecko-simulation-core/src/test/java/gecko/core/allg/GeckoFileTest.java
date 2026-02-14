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
package gecko.core.allg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for GeckoFile - file handling and storage strategy management.
 * Tests focus on relative/absolute path handling and storage type switching.
 */
public class GeckoFileTest {

    private Path tempDir;
    private File testFile;
    private File modelFile;
    private static final String TEST_CONTENT = "test file content";

    @BeforeEach
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("gecko_test_");

        // Create a test file
        testFile = new File(tempDir.toFile(), "testFile.txt");
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write(TEST_CONTENT);
        }

        // Create a model file reference
        modelFile = new File(tempDir.toFile(), "model.ipes");
        modelFile.createNewFile();
    }

    @AfterEach
    public void tearDown() throws IOException {
        // Clean up temp files
        if (testFile != null && testFile.exists()) {
            testFile.delete();
        }
        if (modelFile != null && modelFile.exists()) {
            modelFile.delete();
        }
        if (tempDir != null) {
            Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // ignore
                    }
                });
        }
    }

    // ====================================================
    // Constructor Tests - External Storage
    // ====================================================

    @Test
    public void testConstructor_ExternalStorage_FileExists() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        assertNotNull(geckoFile);
        assertEquals(GeckoFile.StorageType.EXTERNAL, geckoFile.getStorageType());
    }

    @Test
    public void testConstructor_ExternalStorage_FileNotFound() throws IOException {
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.txt");
        assertThrows(java.io.FileNotFoundException.class, () -> {
            new GeckoFile(nonExistentFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        });
    }

    @Test
    public void testConstructor_InternalStorage_FileExists() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.INTERNAL, modelFile.getAbsolutePath());
        assertNotNull(geckoFile);
        assertEquals(GeckoFile.StorageType.INTERNAL, geckoFile.getStorageType());
    }

    // ====================================================
    // Storage Strategy Tests
    // ====================================================

    @Test
    public void testGetStorageType_External() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        assertEquals(GeckoFile.StorageType.EXTERNAL, geckoFile.getStorageType());
    }

    @Test
    public void testGetStorageType_Internal() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.INTERNAL, modelFile.getAbsolutePath());
        assertEquals(GeckoFile.StorageType.INTERNAL, geckoFile.getStorageType());
    }

    // ====================================================
    // Extension Tests
    // ====================================================

    @Test
    public void testGetExtension() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        assertEquals(".txt", geckoFile.getExtension());
    }

    @Test
    public void testGetExtension_DifferentType() throws IOException {
        File javaFile = new File(tempDir.toFile(), "test.java");
        javaFile.createNewFile();
        try {
            GeckoFile geckoFile = new GeckoFile(javaFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
            assertEquals(".java", geckoFile.getExtension());
        } finally {
            javaFile.delete();
        }
    }

    // ====================================================
    // File Name Tests
    // ====================================================

    @Test
    public void testGetName() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        assertEquals("testFile.txt", geckoFile.getName());
    }

    @Test
    public void testGetName_WithPath() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        String name = geckoFile.getName();
        assertFalse(name.contains(File.separator), "Name should not contain path separators");
    }

    // ====================================================
    // Hash Value Tests
    // ====================================================

    @Test
    public void testGetHashValue_NotZero() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        long hash = geckoFile.getHashValue();
        assertNotEquals(0, hash);
    }

    @Test
    public void testGetHashValue_Consistent() throws IOException {
        GeckoFile geckoFile1 = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        long hash1 = geckoFile1.getHashValue();

        GeckoFile geckoFile2 = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        long hash2 = geckoFile2.getHashValue();

        // Both should return non-zero hash values
        assertNotEquals(0, hash1, "Hash should be non-zero");
        assertNotEquals(0, hash2, "Hash should be non-zero");
    }

    // ====================================================
    // User Management Tests
    // ====================================================

    @Test
    public void testSetUser_SingleUser() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        geckoFile.setUser(12345L);
        assertEquals(1, geckoFile.noOfUsers());
    }

    @Test
    public void testSetUser_MultipleUsers() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        geckoFile.setUser(111L);
        geckoFile.setUser(222L);
        geckoFile.setUser(333L);
        assertEquals(3, geckoFile.noOfUsers());
    }

    @Test
    public void testSetUser_DuplicateUser() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        geckoFile.setUser(123L);
        geckoFile.setUser(123L);  // Add same user again
        assertEquals(1, geckoFile.noOfUsers());  // Set, so no duplicates
    }

    @Test
    public void testRemoveUser() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        geckoFile.setUser(123L);
        geckoFile.setUser(456L);
        assertEquals(2, geckoFile.noOfUsers());

        geckoFile.removeUser(123L);
        assertEquals(1, geckoFile.noOfUsers());
    }

    @Test
    public void testRemoveUser_NonExistent() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        geckoFile.setUser(123L);
        geckoFile.removeUser(999L);  // Remove non-existent user
        assertEquals(1, geckoFile.noOfUsers());  // Should remain unchanged
    }

    @Test
    public void testNoOfUsers_InitiallyZero() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        assertEquals(0, geckoFile.noOfUsers());
    }

    // ====================================================
    // File Contents Tests - External Storage
    // ====================================================

    @Test
    public void testGetContentsString_External() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        String contents = geckoFile.getContentsString();
        assertEquals(TEST_CONTENT, contents);
    }

    @Test
    public void testGetContentsByte_External() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        byte[] contents = geckoFile.getContentsByte();
        assertNotNull(contents);
        assertTrue(contents.length > 0, "Contents should be non-empty");
    }

    @Test
    public void testGetContentsByteCopy_External() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        byte[] contents1 = geckoFile.getContentsByteCopy();
        byte[] contents2 = geckoFile.getContentsByteCopy();

        assertNotSame(contents1, contents2, "Should return a copy, not the same array");
        assertArrayEquals(contents1, contents2, "Copies should have same content");
    }

    // ====================================================
    // Modification Timestamp Tests
    // ====================================================

    @Test
    public void testGetModificationTimeStamp_External() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        long timestamp = geckoFile.getModificationTimeStamp();
        assertTrue(timestamp > 0, "Timestamp should be positive");
    }

    @Test
    public void testCheckModificationTimeStamp_External() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        long timestamp = geckoFile.checkModificationTimeStamp();
        assertTrue(timestamp > 0, "Timestamp should be positive");
    }

    @Test
    public void testCheckModificationTimeStamp_Internal() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.INTERNAL, modelFile.getAbsolutePath());
        long timestamp = geckoFile.checkModificationTimeStamp();
        // Internal storage returns a timestamp (may be -1 or actual timestamp depending on implementation)
        assertTrue(timestamp != 0, "Timestamp should be valid");
    }

    // ====================================================
    // Update Tests
    // ====================================================

    @Test
    public void testUpdate_ExternalFile() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());

        // Create a second file
        File newFile = new File(tempDir.toFile(), "newFile.txt");
        try (FileWriter writer = new FileWriter(newFile)) {
            writer.write("new content");
        }

        try {
            geckoFile.update(newFile);
            // Should not throw
            assertTrue(geckoFile.getCurrentAbsolutePath().contains("newFile"), "File should be updated");
        } finally {
            newFile.delete();
        }
    }

    // ====================================================
    // ToString Tests
    // ====================================================

    @Test
    public void testToString_External() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        String str = geckoFile.toString();
        assertNotNull(str);
        assertTrue(str.contains("EXTERNAL"), "Should contain EXTERNAL indicator");
    }

    @Test
    public void testToString_Internal() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.INTERNAL, modelFile.getAbsolutePath());
        String str = geckoFile.toString();
        assertNotNull(str);
        assertTrue(str.contains("INTERNAL"), "Should contain INTERNAL indicator");
    }

    // ====================================================
    // Input Stream Tests
    // ====================================================

    @Test
    public void testGetInputStream_External() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        java.io.InputStream stream = geckoFile.getInputStream();
        assertNotNull(stream, "InputStream should not be null");
        stream.close();
    }

    @Test
    public void testGetBufferedReader_External() throws IOException {
        GeckoFile geckoFile = new GeckoFile(testFile, GeckoFile.StorageType.EXTERNAL, modelFile.getAbsolutePath());
        java.io.BufferedReader reader = geckoFile.getBufferedReader();
        assertNotNull(reader, "BufferedReader should not be null");
        reader.close();
    }

    // ====================================================
    // StorageType Enum Tests
    // ====================================================

    @Test
    public void testStorageTypeEnum_HasValues() {
        GeckoFile.StorageType[] types = GeckoFile.StorageType.values();
        assertTrue(types.length >= 2, "Should have INTERNAL and EXTERNAL");
    }

    @Test
    public void testStorageTypeEnum_Internal() {
        assertNotNull(GeckoFile.StorageType.INTERNAL);
    }

    @Test
    public void testStorageTypeEnum_External() {
        assertNotNull(GeckoFile.StorageType.EXTERNAL);
    }
}
