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
package ch.technokrat.gecko.geckocircuits.control;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

/**
 * Unit tests for FileNameGenerator utility class.
 */
public class FileNameGeneratorTest {
    
    private FileNameGenerator generator;
    private Path tempDir;

    @Before
    public void setUp() throws IOException {
        generator = new FileNameGenerator();
        tempDir = Files.createTempDirectory("gecko_test");
    }

    @After
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir).sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // ignore
                        }
                    });
        }
    }

    @Test
    public void testFindFreeFileNameNonExistent() {
        String nonExistentFile = tempDir.toString() + "/nonexistent.txt";
        String result = generator.findFreeFileName(nonExistentFile);
        assertEquals(nonExistentFile, result);
    }

    @Test
    public void testFindFreeFileNameExistent() throws IOException {
        String baseFile = tempDir.toString() + "/test.txt";
        Files.createFile(Path.of(baseFile));
        
        String result = generator.findFreeFileName(baseFile);
        assertNotEquals(baseFile, result);
        assertTrue(result.contains("test_0.txt"));
    }

    @Test
    public void testFindFreeFileNameMultipleExistents() throws IOException {
        String baseFile = tempDir.toString() + "/test.txt";
        Files.createFile(Path.of(baseFile));
        Files.createFile(Path.of(tempDir.toString() + "/test_0.txt"));
        
        String result = generator.findFreeFileName(baseFile);
        assertTrue(result.contains("test_1.txt"));
    }

    @Test
    public void testParseFileNameSimple() {
        String fileName = "/path/to/file.txt";
        FileNameGenerator.FileNameParts parts = generator.parseFileName(fileName);
        
        assertEquals("/path/to/file", parts.baseName);
        assertEquals(".txt", parts.extension);
    }

    @Test
    public void testParseFileNameNoExtension() {
        String fileName = "/path/to/file";
        FileNameGenerator.FileNameParts parts = generator.parseFileName(fileName);
        
        assertEquals("/path/to/file", parts.baseName);
        assertEquals("", parts.extension);
    }

    @Test
    public void testParseFileNameWithUnderscore() {
        String fileName = "test_file.txt";
        FileNameGenerator.FileNameParts parts = generator.parseFileName(fileName);
        
        assertEquals("test", parts.baseName);
        assertEquals(".txt", parts.extension);
    }

    @Test
    public void testParseFileNameMultipleExtensions() {
        String fileName = "/path/to/archive.tar.gz";
        FileNameGenerator.FileNameParts parts = generator.parseFileName(fileName);
        
        assertEquals("/path/to/archive.tar", parts.baseName);
        assertEquals(".gz", parts.extension);
    }

    @Test
    public void testParseFileNameWindowsPath() {
        String fileName = "C:\\Users\\test\\file.txt";
        FileNameGenerator.FileNameParts parts = generator.parseFileName(fileName);
        
        assertEquals("C:\\Users\\test\\file", parts.baseName);
        assertEquals(".txt", parts.extension);
    }

    @Test
    public void testGenerateNumberedFileName() {
        FileNameGenerator.FileNameParts parts = new FileNameGenerator.FileNameParts("/path/to/file", ".txt");
        
        String result = generator.generateNumberedFileName(parts, 5);
        assertEquals("/path/to/file_5.txt", result);
    }

    @Test
    public void testGenerateNumberedFileNameZero() {
        FileNameGenerator.FileNameParts parts = new FileNameGenerator.FileNameParts("test", ".dat");
        
        String result = generator.generateNumberedFileName(parts, 0);
        assertEquals("test_0.dat", result);
    }

    @Test
    public void testGenerateNumberedFileNameLargeNumber() {
        FileNameGenerator.FileNameParts parts = new FileNameGenerator.FileNameParts("file", "");
        
        String result = generator.generateNumberedFileName(parts, 999);
        assertEquals("file_999", result);
    }

    @Test
    public void testParseFileNameWithoutPath() {
        String fileName = "myfile.log";
        FileNameGenerator.FileNameParts parts = generator.parseFileName(fileName);
        
        assertEquals("myfile", parts.baseName);
        assertEquals(".log", parts.extension);
    }

    @Test
    public void testParseFileNameJustDot() {
        String fileName = "/path/.hidden";
        FileNameGenerator.FileNameParts parts = generator.parseFileName(fileName);
        
        // Hidden files in Unix style start with dot, so the dot is index 0
        assertNotNull(parts.extension);
    }

    @Test
    public void testFindFreeFileNameReturnedFileDoesNotExist() throws IOException {
        String baseFile = tempDir.toString() + "/test.txt";
        Files.createFile(Path.of(baseFile));
        
        String result = generator.findFreeFileName(baseFile);
        assertFalse(new File(result).exists());
    }
}
