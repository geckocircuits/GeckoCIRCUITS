package gecko.core.allg;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class LaunchBrowserTest {

    @Test
    void testUtilityClassCannotBeInstantiated() {
        // Verify the private constructor exists
        Constructor<?>[] constructors = LaunchBrowser.class.getDeclaredConstructors();
        assertEquals(1, constructors.length, "Should have exactly one constructor");
        assertFalse(constructors[0].isAccessible(), "Constructor should be private");
    }

    @Test
    void testLaunchWithNullUrl() {
        // ProcessBuilder will throw NullPointerException if URL is null
        assertThrows(NullPointerException.class, () -> LaunchBrowser.launch(null));
    }

    @Test
    void testLaunchWithEmptyUrl() {
        // Test defensive behavior - should not throw exception
        assertDoesNotThrow(() -> LaunchBrowser.launch(""));
    }

    @Test
    void testLaunchWithValidUrl() {
        // Note: This will attempt to launch a browser, but won't fail if browser unavailable
        // In CI/headless environments, this may fail silently which is acceptable
        assertDoesNotThrow(() -> LaunchBrowser.launch("https://github.com/tinix84/GeckoCIRCUITS"));
    }

    @Test
    void testLaunchWithFileUrl() {
        // Test with file:// protocol
        assertDoesNotThrow(() -> LaunchBrowser.launch("file:///tmp/test.html"));
    }
}
