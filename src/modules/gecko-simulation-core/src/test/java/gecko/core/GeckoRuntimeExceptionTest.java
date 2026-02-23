package gecko.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeckoRuntimeExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "Test error message";
        GeckoRuntimeException exception = new GeckoRuntimeException(message);

        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithNullMessage() {
        GeckoRuntimeException exception = new GeckoRuntimeException(null);

        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithEmptyMessage() {
        String message = "";
        GeckoRuntimeException exception = new GeckoRuntimeException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void testConstructorWithMessageAndOutOfMemoryError() {
        String message = "Memory allocation failed";
        OutOfMemoryError oomError = new OutOfMemoryError("Heap space exhausted");

        GeckoRuntimeException exception = new GeckoRuntimeException(message, oomError);

        assertEquals(message, exception.getMessage());
        assertEquals(oomError, exception.getCause());
    }

    @Test
    void testThrowAndCatchException() {
        String message = "Simulation error";

        GeckoRuntimeException thrown = assertThrows(GeckoRuntimeException.class, () -> {
            throw new GeckoRuntimeException(message);
        });

        assertEquals(message, thrown.getMessage());
    }

    @Test
    void testExceptionIsRuntimeException() {
        GeckoRuntimeException exception = new GeckoRuntimeException("Test");
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testExceptionChaining() {
        OutOfMemoryError originalError = new OutOfMemoryError("Original OOM");
        String wrapperMessage = "Wrapped memory error";

        GeckoRuntimeException exception = new GeckoRuntimeException(wrapperMessage, originalError);

        assertEquals(wrapperMessage, exception.getMessage());
        assertSame(originalError, exception.getCause());
    }

    @Test
    void testStackTracePreserved() {
        try {
            throw new GeckoRuntimeException("Test exception");
        } catch (GeckoRuntimeException e) {
            StackTraceElement[] stackTrace = e.getStackTrace();
            assertNotNull(stackTrace);
            assertTrue(stackTrace.length > 0);
            assertEquals("testStackTracePreserved", stackTrace[0].getMethodName());
        }
    }
}
