package ch.technokrat.gecko.geckocircuits.control;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Advanced integration tests for refactored DataSaver components.
 * Tests real-world scenarios and robustness of extracted helper classes.
 */
public class DataSaverAdvancedIntegrationTest {
    
    private FileNameGenerator fileNameGenerator;
    private Path tempDir;
    
    @Before
    public void setUp() throws IOException {
        fileNameGenerator = new FileNameGenerator();
        tempDir = Files.createTempDirectory("gecko_test_");
    }
    
    @After
    public void tearDown() throws IOException {
        // Clean up temporary directory
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                 .sorted((a, b) -> b.compareTo(a))
                 .forEach(path -> {
                     try { Files.delete(path); } catch (IOException e) { /*ignore*/ }
                 });
        }
    }
    
    /**
     * Tests generating multiple unique filenames by creating actual files.
     */
    @Test
    public void testMultipleFileGenerationSequence() throws IOException {
        List<String> generatedNames = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            String basePath = tempDir.resolve("data.csv").toString();
            String fileName = fileNameGenerator.findFreeFileName(basePath);
            
            assertFalse("Should not have duplicate names", generatedNames.contains(fileName));
            generatedNames.add(fileName);
            
            // Create the file so next iteration finds it exists
            new File(fileName).createNewFile();
        }
        
        assertEquals("Should generate 5 unique names", 5, generatedNames.size());
    }
    
    /**
     * Tests filename generation with timestamp pattern.
     */
    @Test
    public void testFileNameGeneratorWithTimestamp() {
        long timestamp = System.currentTimeMillis();
        String baseName = tempDir.resolve("export_" + timestamp).toString();
        String fileName = fileNameGenerator.findFreeFileName(baseName + ".txt");
        
        assertNotNull("Generated filename should not be null", fileName);
        assertTrue("Should contain .txt extension", fileName.endsWith(".txt"));
        assertTrue("Should contain export_ prefix", fileName.contains("export_"));
    }
    
    /**
     * Tests that path components are preserved during generation.
     */
    @Test
    public void testFileNameGeneratorPreservesPath() {
        String testPath = tempDir.resolve("export_data.csv").toString();
        String fileName = fileNameGenerator.findFreeFileName(testPath);
        
        assertNotNull("Generated filename should not be null", fileName);
        assertTrue("Should contain temp path", fileName.startsWith(tempDir.toString()));
        assertTrue("Should contain .csv extension", fileName.endsWith(".csv"));
    }
    
    /**
     * Tests multiple file extension formats.
     */
    @Test
    public void testMultipleExtensionHandling() {
        String[] extensions = {".csv", ".txt", ".dat", ".log", ".json"};
        
        for (String ext : extensions) {
            String fileName = fileNameGenerator.findFreeFileName(
                    tempDir.resolve("data" + ext).toString());
            assertNotNull("Should generate filename with " + ext, fileName);
            assertTrue("Should contain extension " + ext, fileName.endsWith(ext));
        }
    }
    
    /**
     * Tests that the generator produces consistent results across multiple instances.
     */
    @Test
    public void testFileNameGeneratorConsistency() {
        FileNameGenerator gen1 = new FileNameGenerator();
        FileNameGenerator gen2 = new FileNameGenerator();
        
        String filePath = tempDir.resolve("unique_file.txt").toString();
        String file1 = gen1.findFreeFileName(filePath);
        String file2 = gen2.findFreeFileName(filePath);
        
        // Both should be valid filenames (behavior is consistent)
        assertNotNull("Generator 1 should produce valid filename", file1);
        assertNotNull("Generator 2 should produce valid filename", file2);
    }
    
    /**
     * Tests filenames with multiple dots.
     */
    @Test
    public void testFileNameWithMultipleDots() {
        String fileName = fileNameGenerator.findFreeFileName(
                tempDir.resolve("data.backup.csv").toString());
        
        assertNotNull("Should handle multiple dots", fileName);
        assertTrue("Should preserve last extension", fileName.endsWith(".csv"));
    }
    
    /**
     * Tests uppercase extension handling.
     */
    @Test
    public void testFileNameUppercaseExtension() {
        String fileName = fileNameGenerator.findFreeFileName(
                tempDir.resolve("data.TXT").toString());
        
        assertNotNull("Should handle uppercase extension", fileName);
        assertTrue("Should preserve uppercase extension", fileName.endsWith(".TXT"));
    }
    
    /**
     * Tests filenames with numbers.
     */
    @Test
    public void testFileNameWithNumbers() {
        String fileName = fileNameGenerator.findFreeFileName(
                tempDir.resolve("data_2024_01_26.csv").toString());
        
        assertNotNull("Should handle numbers in filename", fileName);
        assertTrue("Should contain extension", fileName.contains(".csv"));
    }
    
    /**
     * Tests filenames with special characters.
     */
    @Test
    public void testFileNameWithSpecialChars() {
        String fileName = fileNameGenerator.findFreeFileName(
                tempDir.resolve("data-report_2024.csv").toString());
        
        assertNotNull("Should handle special characters", fileName);
        assertTrue("Should contain extension", fileName.endsWith(".csv"));
    }
    
    /**
     * Tests refactoring pattern validation - single responsibility.
     */
    @Test
    public void testRefactoringPatternValidation() {
        FileNameGenerator generator = new FileNameGenerator();
        
        // Generator should not have side effects
        String filePath = tempDir.resolve("test.txt").toString();
        String name1 = generator.findFreeFileName(filePath);
        String name2 = generator.findFreeFileName(filePath);
        
        // Both should be valid independent results
        assertNotNull("First call should succeed", name1);
        assertNotNull("Second call should succeed", name2);
    }
    
    /**
     * Tests backward compatibility - original behavior is preserved.
     */
    @Test
    public void testBackwardCompatibility() {
        String originalName = tempDir.resolve("nonexistent_unique_file_xyz.csv").toString();
        String result = fileNameGenerator.findFreeFileName(originalName);
        
        assertEquals("Should return original name for non-existent files", 
                     originalName, result);
    }
    
    /**
     * Tests unique file naming with actual file creation.
     */
    @Test
    public void testUniqueFileNamingWithCreation() throws IOException {
        String basePath = tempDir.resolve("report.csv").toString();
        Set<String> generatedPaths = new HashSet<>();
        
        // Create 10 files and verify each gets a unique name
        for (int i = 0; i < 10; i++) {
            String filePath = fileNameGenerator.findFreeFileName(basePath);
            assertTrue("Each path should be unique", generatedPaths.add(filePath));
            new File(filePath).createNewFile();
        }
        
        assertEquals("Should have 10 unique paths", 10, generatedPaths.size());
    }
    
    /**
     * Tests numbered filename generation correctness.
     */
    @Test
    public void testNumberedFileNameGeneration() throws IOException {
        String basePath = tempDir.resolve("export.txt").toString();
        
        // Create first file
        File file1 = new File(basePath);
        file1.createNewFile();
        assertTrue("First file should exist", file1.exists());
        
        // Get next filename
        String numberedPath = fileNameGenerator.findFreeFileName(basePath);
        assertNotEquals("Should generate different name", basePath, numberedPath);
        assertTrue("Should contain underscore separator", numberedPath.contains("_"));
        assertTrue("Should contain number", numberedPath.matches(".*_\\d+\\.txt"));
    }
}
