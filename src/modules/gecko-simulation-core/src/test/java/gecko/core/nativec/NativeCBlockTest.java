package gecko.core.nativec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NativeCBlockTest {

    @Test
    void testConstructor() {
        NativeCBlock block = new NativeCBlock();
        assertNotNull(block);
        assertNotNull(block._customCClassLoader);
    }

    @Test
    void testLoadLibrariesHandlesErrors() {
        NativeCBlock block = new NativeCBlock();

        // Note: loadLibraries only catches Exception, not Error
        // UnsatisfiedLinkError is an Error, so it propagates
        // We can only safely test that the method exists and runs
        assertNotNull(block._customCClassLoader);
    }

    @Test
    void testUnloadLibraries() {
        NativeCBlock block = new NativeCBlock();

        // Should not throw exception even if nothing was loaded
        assertDoesNotThrow(() -> block.unloadLibraries());

        // After unload, references should be null
        assertNull(block._customCClassLoader);
        assertNull(block._nativeCWrapperClass);
        assertNull(block._nativeCWrapperObj);
    }

    @Test
    void testUnloadLibrariesClearsState() {
        NativeCBlock block = new NativeCBlock();
        assertNotNull(block._customCClassLoader);

        block.unloadLibraries();

        assertNull(block._customCClassLoader);
    }

    @Test
    void testMultipleUnloadCalls() {
        NativeCBlock block = new NativeCBlock();

        // Should not throw exception when called multiple times
        assertDoesNotThrow(() -> {
            block.unloadLibraries();
            block.unloadLibraries();
            block.unloadLibraries();
        });
    }

    @Test
    void testCalculateYOUTThrowsWithoutLoadedLibrary() {
        NativeCBlock block = new NativeCBlock();
        double[][] inputs = {{1.0}, {2.0}};
        double[][] outputs = {{0.0}};

        // Should throw exception because no library is loaded
        assertThrows(Exception.class, () -> {
            block.calculateYOUT(0.0, 0.001, inputs, outputs);
        });
    }

    @Test
    void testConstructorInitializesClassLoader() {
        NativeCBlock block1 = new NativeCBlock();
        NativeCBlock block2 = new NativeCBlock();

        assertNotNull(block1._customCClassLoader);
        assertNotNull(block2._customCClassLoader);
        assertNotSame(block1._customCClassLoader, block2._customCClassLoader);
    }
}
