package ch.technokrat.gecko.geckocircuits.control;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for FileNameGenerator.
 */
public class FileNameGeneratorTest {
    
    private FileNameGenerator generator;
    private List<File> tempFiles;
    private Path tempDir;
    
    @Before
    public void setUp() throws IOException {
        generator = new FileNameGenerator();
        tempFiles = new ArrayList<>();
        tempDir = Files.createTempDirectory("gecko_test_");
    }
    
    @After
    public void tearDown() {
        // Clean up temporary files
        for (File file : tempFiles) {
            if (file.exists()) {
                file.delete();
            }
        }
        tempDir.toFile().delete();
    }
    
    private File createTempFile(String name) throws IOException {
        File file = new File(tempDir.toFile(), name);
        file.createNewFile();
        tempFiles.add(file);
        return file;
    }
    
    @Test
    public void testFindFreeFileName_fileDoesNotExist_returnsSame() {
        String nonExistentFile = tempDir.toString() + "/nonexistent.txt";
        String result = generator.findFreeFileName(nonExistentFile);
        
        assertEquals("Should return the same filename when file doesn't exist", 
                     nonExistentFile, result);
    }
    
    @Test
    public void testFindFreeFileName_fileExists_addsNumber() throws IOException {
        File existingFile = createTempFile("data.txt");
        String result = generator.findFreeFileName(existingFile.getAbsolutePath());
        
        assertTrue("Should add number to filename", result.contains("_0"));
        assertTrue("Should preserve extension", result.endsWith(".txt"));
        assertFalse("Generated filename should not exist", new File(result).exists());
    }
    
    @Test
    public void testFindFreeFileName_multipleExist_findsNextFree() throws IOException {
        // Create base file with underscore naming pattern
        File file0 = createTempFile("report_data.txt");
        // Create numbered versions that already exist
        createTempFile("report_0.txt");
        createTempFile("report_1.txt");
        
        String result = generator.findFreeFileName(file0.getAbsolutePath());
        
        // The algorithm extracts basename before last underscore
        // For "report_data.txt", it extracts "report" and creates "report_0.txt" onwards
        // It will find "report_2.txt" as the next free name
        assertTrue("Should create a numbered filename", result.contains("report_"));
        assertFalse("Generated filename should not exist", new File(result).exists());
    }
    
    @Test
    public void testFindFreeFileName_noExtension_handlesCorrectly() throws IOException {
        File file = createTempFile("datafile");
        String result = generator.findFreeFileName(file.getAbsolutePath());
        
        assertTrue("Should add number even without extension", result.contains("_0"));
    }
    
    @Test
    public void testGenerateNumberedFileName_correctFormat() {
        FileNameGenerator.FileNameParts parts = 
            new FileNameGenerator.FileNameParts("test/data", ".txt");
        
        String result = generator.generateNumberedFileName(parts, 5);
        
        assertEquals("Should generate correct numbered filename", 
                     "test/data_5.txt", result);
    }
    
    @Test
    public void testParseFileName_withExtension() {
        FileNameGenerator.FileNameParts parts = generator.parseFileName("/path/to/file.txt");
        
        assertEquals("Should extract base name", "/path/to/file", parts.baseName);
        assertEquals("Should extract extension", ".txt", parts.extension);
    }
    
    @Test
    public void testParseFileName_withoutExtension() {
        FileNameGenerator.FileNameParts parts = generator.parseFileName("/path/to/file");
        
        assertEquals("Should use full name as base", "/path/to/file", parts.baseName);
        assertEquals("Should have empty extension", "", parts.extension);
    }
    
    @Test
    public void testParseFileName_withUnderscore_removesTrailing() {
        FileNameGenerator.FileNameParts parts = generator.parseFileName("/path/data_5.txt");
        
        assertEquals("Should remove existing number", "/path/data", parts.baseName);
        assertEquals("Should preserve extension", ".txt", parts.extension);
    }
    
    @Test
    public void testParseFileName_multipleExtensions() {
        FileNameGenerator.FileNameParts parts = generator.parseFileName("/path/archive.tar.gz");
        
        // Should use last dot
        assertTrue("Should handle multiple dots", parts.baseName.contains(".tar"));
        assertEquals("Should use last extension", ".gz", parts.extension);
    }
}
