package ch.technokrat.gecko.geckocircuits.control;

import java.io.File;

/**
 * Utility class for generating unique file names.
 * When a file already exists, appends a number before the extension to create a unique name.
 */
public final class FileNameGenerator {
    
    private static final int MAX_FILE_COUNTER = 1000;
    
    /**
     * Finds a free (non-existing) file name by appending numbers if necessary.
     * 
     * @param origFile the original file path
     * @return a file path that doesn't exist, or the original if max attempts exceeded
     */
    public String findFreeFileName(final String origFile) {
        if (!new File(origFile).exists()) {
            return origFile;
        }
        
        FileNameParts parts = parseFileName(origFile);
        
        for (int counter = 0; counter < MAX_FILE_COUNTER; counter++) {
            final String newFileName = generateNumberedFileName(parts, counter);
            if (!new File(newFileName).exists()) {
                return newFileName;
            }
        }
        return origFile;
    }
    
    /**
     * Generates a numbered file name by inserting a counter before the extension.
     * 
     * @param parts the parsed file name parts
     * @param counter the number to insert
     * @return the numbered file name
     */
    String generateNumberedFileName(final FileNameParts parts, final int counter) {
        return parts.baseName + "_" + counter + parts.extension;
    }
    
    /**
     * Parses a file name into base name and extension parts.
     * 
     * @param fileName the file name to parse
     * @return the parsed parts
     */
    FileNameParts parseFileName(final String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 1) {
            dotIndex = fileName.length();
        }

        int lastSepIndex = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        int filenameStart = lastSepIndex + 1;

        int underscoreIndex = fileName.lastIndexOf('_');
        if (underscoreIndex < filenameStart || underscoreIndex >= dotIndex) {
            underscoreIndex = dotIndex;
        }
        
        String baseName = fileName.substring(0, underscoreIndex);
        String extension = fileName.substring(dotIndex);
        
        return new FileNameParts(baseName, extension);
    }
    
    /**
     * Value object holding the parts of a file name.
     */
    static class FileNameParts {
        final String baseName;
        final String extension;
        
        FileNameParts(String baseName, String extension) {
            this.baseName = baseName;
            this.extension = extension;
        }
    }
}
