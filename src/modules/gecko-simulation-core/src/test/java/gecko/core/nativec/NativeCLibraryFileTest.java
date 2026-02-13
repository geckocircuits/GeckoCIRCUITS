package gecko.core.nativec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NativeCLibraryFileTest {

    @TempDir
    Path tempDir;

    @Test
    void testDefaultConstructor() {
        NativeCLibraryFile libFile = new NativeCLibraryFile();
        assertNotNull(libFile);
    }

    @Test
    void testConstructorWithExistingFile(@TempDir Path tempDir) throws IOException, FileNotFoundException {
        Path testFile = tempDir.resolve("test.so");
        Files.createFile(testFile);

        NativeCLibraryFile libFile = new NativeCLibraryFile(testFile.toString());

        assertEquals(testFile.toString(), libFile.getFileName());
        assertTrue(libFile.getTimeStamp() > 0);
    }

    @Test
    void testConstructorWithNonExistentFile() {
        assertThrows(FileNotFoundException.class, () -> {
            new NativeCLibraryFile("/nonexistent/path/library.so");
        });
    }

    @Test
    void testConstructorWithFileObject(@TempDir Path tempDir) throws IOException, FileNotFoundException {
        Path testFile = tempDir.resolve("library.dll");
        Files.createFile(testFile);
        File file = testFile.toFile();

        NativeCLibraryFile libFile = new NativeCLibraryFile(file);

        assertEquals(file.getAbsolutePath(), libFile.getFileName());
        assertNotNull(libFile.getFile());
    }

    @Test
    void testSetFileWithString(@TempDir Path tempDir) throws IOException, FileNotFoundException {
        Path testFile = tempDir.resolve("test.dylib");
        Files.createFile(testFile);

        NativeCLibraryFile libFile = new NativeCLibraryFile();
        libFile.setFile(testFile.toString());

        assertEquals(testFile.toString(), libFile.getFileName());
        assertNotNull(libFile.getFile());
    }

    @Test
    void testSetFileThrowsExceptionForNonExistent() {
        NativeCLibraryFile libFile = new NativeCLibraryFile();

        assertThrows(FileNotFoundException.class, () -> {
            libFile.setFile("/fake/path/lib.so");
        });
    }

    @Test
    void testSetFileWithFileObject(@TempDir Path tempDir) throws IOException, FileNotFoundException {
        Path testFile = tempDir.resolve("mylib.so");
        Files.createFile(testFile);

        NativeCLibraryFile libFile = new NativeCLibraryFile();
        libFile.setFile(testFile.toFile());

        assertEquals(testFile.toAbsolutePath().toString(), libFile.getFileName());
    }

    @Test
    void testSetFileNoArgs() {
        NativeCLibraryFile libFile = new NativeCLibraryFile();
        libFile.setFile();

        assertNull(libFile.getFile());
        assertNull(libFile.getFileName());
        assertEquals(0, libFile.getTimeStamp());
    }

    @Test
    void testSavegetFile(@TempDir Path tempDir) throws IOException, FileNotFoundException {
        Path testFile = tempDir.resolve("library.so");
        Files.createFile(testFile);

        NativeCLibraryFile libFile = new NativeCLibraryFile(testFile.toString());
        File retrieved = libFile.savegetFile();

        assertNotNull(retrieved);
        assertEquals(testFile.toFile(), retrieved);
    }

    @Test
    void testSavegetFileThrowsWhenNotSet() {
        NativeCLibraryFile libFile = new NativeCLibraryFile();

        assertThrows(FileNotFoundException.class, () -> {
            libFile.savegetFile();
        });
    }

    @Test
    void testSavegetFileName(@TempDir Path tempDir) throws IOException, FileNotFoundException {
        Path testFile = tempDir.resolve("test.dll");
        Files.createFile(testFile);

        NativeCLibraryFile libFile = new NativeCLibraryFile(testFile.toString());
        String fileName = libFile.savegetFileName();

        assertEquals(testFile.toString(), fileName);
    }

    @Test
    void testSavegetFileNameThrowsWhenNotSet() {
        NativeCLibraryFile libFile = new NativeCLibraryFile();

        assertThrows(FileNotFoundException.class, () -> {
            libFile.savegetFileName();
        });
    }

    @Test
    void testGetTimeStamp(@TempDir Path tempDir) throws IOException, FileNotFoundException {
        Path testFile = tempDir.resolve("timestamped.so");
        Files.createFile(testFile);

        NativeCLibraryFile libFile = new NativeCLibraryFile(testFile.toString());
        long timestamp = libFile.getTimeStamp();

        assertTrue(timestamp > 0, "Timestamp should be positive");
        assertEquals(testFile.toFile().lastModified(), timestamp);
    }

    @Test
    void testUpdateTimeStamp(@TempDir Path tempDir) throws IOException, FileNotFoundException, InterruptedException {
        Path testFile = tempDir.resolve("updateable.so");
        Files.createFile(testFile);

        NativeCLibraryFile libFile = new NativeCLibraryFile(testFile.toString());
        long originalTimestamp = libFile.getTimeStamp();

        // Wait a bit to ensure timestamp changes
        Thread.sleep(10);

        // Modify the file
        Files.write(testFile, new byte[]{1, 2, 3});

        boolean updated = libFile.updateTimeStamp();

        assertTrue(updated, "Timestamp should have been updated");
        assertTrue(libFile.getTimeStamp() > originalTimestamp, "New timestamp should be greater");
    }

    @Test
    void testUpdateTimeStampReturnsFalseWhenUnchanged(@TempDir Path tempDir) throws IOException, FileNotFoundException {
        Path testFile = tempDir.resolve("unchanged.so");
        Files.createFile(testFile);

        NativeCLibraryFile libFile = new NativeCLibraryFile(testFile.toString());

        // Update without modifying file
        boolean updated = libFile.updateTimeStamp();

        assertFalse(updated, "Timestamp should not have been updated");
    }

    @Test
    void testUpdateTimeStampThrowsWhenNotSet() {
        NativeCLibraryFile libFile = new NativeCLibraryFile();

        assertThrows(FileNotFoundException.class, () -> {
            libFile.updateTimeStamp();
        });
    }
}
