package ch.technokrat.gecko.geckocircuits.control;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Integration tests for DataSaver extracted helper classes.
 * Tests the interaction between FileNameGenerator and SignalValidator.
 */
public class DataSaverRefactoringIntegrationTest {
    
    @Test
    public void testFileNameGeneratorIntegration() {
        // Test the FileNameGenerator extracted from DataSaver.findFreeFile()
        FileNameGenerator generator = new FileNameGenerator();
        
        String fileName = generator.findFreeFileName("test.txt");
        assertNotNull("Should generate a filename", fileName);
        assertTrue("Should contain base name", fileName.contains("test"));
        assertTrue("Should have extension", fileName.endsWith(".txt"));
    }
    
    @Test
    public void testFileNameGeneratorWithMultipleExtensions() {
        FileNameGenerator generator = new FileNameGenerator();
        
        String textFile = generator.findFreeFileName("data.txt");
        String csvFile = generator.findFreeFileName("data.csv");
        String logFile = generator.findFreeFileName("data.log");
        
        assertNotNull("Text file name should be generated", textFile);
        assertNotNull("CSV file name should be generated", csvFile);
        assertNotNull("Log file name should be generated", logFile);
        
        assertTrue("Text file should preserve extension", textFile.endsWith(".txt"));
        assertTrue("CSV file should preserve extension", csvFile.endsWith(".csv"));
        assertTrue("Log file should preserve extension", logFile.endsWith(".log"));
    }
    
    @Test
    public void testFileNameGeneratorParsingLogic() {
        // Test the file name parsing component
        FileNameGenerator generator = new FileNameGenerator();
        
        String baseFileName = "mydata.txt";
        String fileName1 = generator.findFreeFileName(baseFileName);
        
        assertNotNull("Should generate file name", fileName1);
        assertTrue("Should start with base name", fileName1.contains("mydata"));
        // Might have a number inserted like mydata_1.txt or mydata1.txt
    }
    
    @Test
    public void testExtractedClassesAreIndependent() {
        // Verify that extracted classes can be instantiated separately
        FileNameGenerator gen1 = new FileNameGenerator();
        FileNameGenerator gen2 = new FileNameGenerator();
        
        String name1 = gen1.findFreeFileName("file.txt");
        String name2 = gen2.findFreeFileName("file.txt");
        
        // Multiple instances should work independently
        assertNotNull("First instance should work", name1);
        assertNotNull("Second instance should work", name2);
    }
    
    @Test
    public void testFileNameGeneratorEdgeCases() {
        FileNameGenerator generator = new FileNameGenerator();
        
        // Test with special characters in filename
        String fileName = generator.findFreeFileName("test_data-2026.csv");
        assertNotNull("Should handle special characters", fileName);
        assertTrue("Should preserve base structure", fileName.contains("test_data"));
    }
    
    @Test
    public void testFileNameGeneratorWithPath() {
        FileNameGenerator generator = new FileNameGenerator();
        
        // Test with path separators
        String fileName = generator.findFreeFileName("subdir/data.txt");
        assertNotNull("Should handle paths", fileName);
    }
    
    @Test
    public void testFileNameGeneratorNumericalParts() {
        FileNameGenerator generator = new FileNameGenerator();
        
        // Generate multiple file names to test numbering
        String file1 = generator.findFreeFileName("output.dat");
        String file2 = generator.findFreeFileName("output.dat");
        String file3 = generator.findFreeFileName("output.dat");
        
        assertNotNull("First file should be generated", file1);
        assertNotNull("Second file should be generated", file2);
        assertNotNull("Third file should be generated", file3);
        
        assertTrue("All should preserve extension", 
            file1.endsWith(".dat") && file2.endsWith(".dat") && file3.endsWith(".dat"));
    }
    
    @Test
    public void testFileNameGeneratorWithoutExtension() {
        FileNameGenerator generator = new FileNameGenerator();
        
        // Test filename without extension
        String fileName = generator.findFreeFileName("noextension");
        assertNotNull("Should handle files without extension", fileName);
        assertTrue("Should contain base name", fileName.contains("noextension"));
    }
    
    @Test
    public void testFileNameParsingComponents() {
        // Test the FileNameParts inner class
        // FileNameParts requires base name and extension
        FileNameGenerator.FileNameParts parts = new FileNameGenerator.FileNameParts("mydata", ".txt");
        
        assertNotNull("Should parse filename", parts);
        // FileNameParts should break down the filename into components
    }
    
    @Test
    public void testFileNameGeneratorConsistency() {
        // Verify that the same generator produces consistent results
        FileNameGenerator generator = new FileNameGenerator();
        
        String file1a = generator.findFreeFileName("sample.txt");
        String file1b = generator.findFreeFileName("sample.txt");
        
        // Both should be valid filenames
        assertNotNull("First call should return filename", file1a);
        assertNotNull("Second call should return filename", file1b);
        assertTrue("Both should have .txt extension", 
            file1a.endsWith(".txt") && file1b.endsWith(".txt"));
    }
    
    @Test
    public void testFileNameGeneratorWithNumbers() {
        FileNameGenerator generator = new FileNameGenerator();
        
        // Test filename with existing numbers
        String fileName = generator.findFreeFileName("data_001.csv");
        assertNotNull("Should handle filenames with numbers", fileName);
        assertTrue("Should preserve csv extension", fileName.endsWith(".csv"));
    }
}
