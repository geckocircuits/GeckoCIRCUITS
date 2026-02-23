package gecko.core.nativec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NativeCClassLoaderTest {

    @Test
    void testConstructor() {
        NativeCClassLoader loader = new NativeCClassLoader();
        assertNotNull(loader);
    }

    @Test
    void testToString() {
        NativeCClassLoader loader = new NativeCClassLoader();
        String result = loader.toString();

        assertEquals("gecko.core.nativec.NativeCClassLoader", result);
    }

    @Test
    void testFindClassThrowsForNonExistentClass() {
        NativeCClassLoader loader = new NativeCClassLoader();

        assertThrows(ClassNotFoundException.class, () -> {
            loader.findClass("com.nonexistent.FakeClass");
        });
    }

    @Test
    void testFindClassProhibitsJavaLangClasses() {
        NativeCClassLoader loader = new NativeCClassLoader();

        // Custom classloaders cannot load java.lang classes
        assertThrows(SecurityException.class, () -> {
            loader.findClass("java.lang.String");
        });
    }

    @Test
    void testMultipleInstances() {
        NativeCClassLoader loader1 = new NativeCClassLoader();
        NativeCClassLoader loader2 = new NativeCClassLoader();

        assertNotSame(loader1, loader2);
        assertEquals(loader1.toString(), loader2.toString());
    }

    @Test
    void testFindClassThrowsForNull() {
        NativeCClassLoader loader = new NativeCClassLoader();

        assertThrows(Exception.class, () -> {
            loader.findClass(null);
        });
    }

    @Test
    void testFindClassThrowsForEmptyString() {
        NativeCClassLoader loader = new NativeCClassLoader();

        assertThrows(ClassNotFoundException.class, () -> {
            loader.findClass("");
        });
    }
}
