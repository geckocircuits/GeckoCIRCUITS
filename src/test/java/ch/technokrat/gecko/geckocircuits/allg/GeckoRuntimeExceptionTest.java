/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.allg;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for GeckoRuntimeException - custom runtime exception for GeckoCIRCUITS.
 */
public class GeckoRuntimeExceptionTest {

    // ====================================================
    // Constructor Tests
    // ====================================================

    @Test
    public void testConstructor_WithMessage() {
        String message = "Test error message";
        GeckoRuntimeException exception = new GeckoRuntimeException(message);

        assertNotNull("Exception should be created", exception);
        assertEquals("Message should match", message, exception.getMessage());
    }

    @Test
    public void testConstructor_WithEmptyMessage() {
        String message = "";
        GeckoRuntimeException exception = new GeckoRuntimeException(message);

        assertNotNull("Exception should be created", exception);
        assertEquals("Message should be empty string", message, exception.getMessage());
    }

    @Test
    public void testConstructor_WithNullMessage() {
        // Note: RuntimeException allows null messages
        GeckoRuntimeException exception = new GeckoRuntimeException(null);
        assertNotNull("Exception should be created", exception);
    }

    // ====================================================
    // Inheritance Tests
    // ====================================================

    @Test
    public void testIsRuntimeException() {
        GeckoRuntimeException exception = new GeckoRuntimeException("Test");
        assertTrue("GeckoRuntimeException should be a RuntimeException", exception instanceof RuntimeException);
    }

    @Test
    public void testIsThrowable() {
        GeckoRuntimeException exception = new GeckoRuntimeException("Test");
        assertTrue("GeckoRuntimeException should be Throwable", exception instanceof Throwable);
    }

    // ====================================================
    // Exception Throwing and Catching Tests
    // ====================================================

    @Test
    public void testThrowAndCatch_RuntimeException() {
        try {
            throw new GeckoRuntimeException("Caught as RuntimeException");
        } catch (RuntimeException e) {
            assertEquals("Message should match", "Caught as RuntimeException", e.getMessage());
        }
    }

    @Test
    public void testThrowAndCatch_GeckoRuntimeException() {
        try {
            throw new GeckoRuntimeException("Caught as GeckoRuntimeException");
        } catch (GeckoRuntimeException e) {
            assertEquals("Message should match", "Caught as GeckoRuntimeException", e.getMessage());
        }
    }

    @Test
    public void testThrowAndCatch_Throwable() {
        try {
            throw new GeckoRuntimeException("Caught as Throwable");
        } catch (Throwable e) {
            assertEquals("Message should match", "Caught as Throwable", e.getMessage());
        }
    }

    // ====================================================
    // StackTrace Tests
    // ====================================================

    @Test
    public void testStackTrace_NotNull() {
        GeckoRuntimeException exception = new GeckoRuntimeException("Test");
        StackTraceElement[] stackTrace = exception.getStackTrace();

        assertNotNull("Stack trace should not be null", stackTrace);
        assertTrue("Stack trace should have elements", stackTrace.length > 0);
    }

    @Test
    public void testStackTrace_ContainsTestClass() {
        GeckoRuntimeException exception = new GeckoRuntimeException("Test");
        StackTraceElement[] stackTrace = exception.getStackTrace();

        // At least one element should be from this test class
        boolean foundThisClass = false;
        for (StackTraceElement element : stackTrace) {
            if (element.getClassName().contains("GeckoRuntimeExceptionTest")) {
                foundThisClass = true;
                break;
            }
        }
        assertTrue("Stack trace should include current test class", foundThisClass);
    }

    // ====================================================
    // Message Tests
    // ====================================================

    @Test
    public void testMessage_Retrieval() {
        String originalMessage = "This is a test error message";
        GeckoRuntimeException exception = new GeckoRuntimeException(originalMessage);
        assertEquals("getMessage() should return original message", originalMessage, exception.getMessage());
    }

    @Test
    public void testMessage_WithSpecialCharacters() {
        String message = "Error: Invalid character '@' at position 5";
        GeckoRuntimeException exception = new GeckoRuntimeException(message);
        assertEquals("Message with special characters should match", message, exception.getMessage());
    }

    @Test
    public void testMessage_WithNewlines() {
        String message = "Error line 1\nError line 2";
        GeckoRuntimeException exception = new GeckoRuntimeException(message);
        assertEquals("Message with newlines should match", message, exception.getMessage());
    }

    // ====================================================
    // Use Case Tests - Common Error Scenarios
    // ====================================================

    @Test
    public void testUseCase_ParseError() {
        GeckoRuntimeException exception = new GeckoRuntimeException("Cannot parse circuit file format");
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Cannot parse"));
    }

    @Test
    public void testUseCase_SimulationError() {
        GeckoRuntimeException exception = new GeckoRuntimeException("Simulation diverged at time=1.5s");
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Simulation"));
    }

    @Test
    public void testUseCase_FileNotFound() {
        GeckoRuntimeException exception = new GeckoRuntimeException("Circuit file not found: /path/to/file.ipes");
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    public void testUseCase_ConfigurationError() {
        GeckoRuntimeException exception = new GeckoRuntimeException("Invalid solver configuration: timestep too large");
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("Invalid"));
    }

    // ====================================================
    // Cause Chain Tests
    // ====================================================

    @Test
    public void testCause_GetCause() {
        GeckoRuntimeException exception = new GeckoRuntimeException("Test");
        Throwable cause = exception.getCause();

        // Constructor without cause parameter should have null cause
        assertNull("Cause should be null", cause);
    }

    // ====================================================
    // ToString Tests
    // ====================================================

    @Test
    public void testToString_Format() {
        String message = "Test error";
        GeckoRuntimeException exception = new GeckoRuntimeException(message);
        String toString = exception.toString();

        assertNotNull("toString() should not be null", toString);
        assertTrue("toString() should contain exception class name", toString.contains("GeckoRuntimeException"));
        assertTrue("toString() should contain message", toString.contains(message));
    }

    // ====================================================
    // Multiple Instances Tests
    // ====================================================

    @Test
    public void testMultipleInstances_Independent() {
        GeckoRuntimeException exception1 = new GeckoRuntimeException("Error 1");
        GeckoRuntimeException exception2 = new GeckoRuntimeException("Error 2");

        assertNotEquals("Different messages should be independent", exception1.getMessage(), exception2.getMessage());
        assertNotEquals("Different instances should not be equal", exception1, exception2);
    }

    // ====================================================
    // Message Immutability Tests
    // ====================================================

    @Test
    public void testMessage_Immutable() {
        String originalMessage = "Original message";
        GeckoRuntimeException exception = new GeckoRuntimeException(originalMessage);
        String retrievedMessage = exception.getMessage();

        // Message should be the same
        assertEquals("Retrieved message should match original", originalMessage, retrievedMessage);

        // Getting it again should still work
        String retrievedAgain = exception.getMessage();
        assertEquals("Multiple retrievals should return same message", originalMessage, retrievedAgain);
    }

    // ====================================================
    // Real-World Scenario Tests
    // ====================================================

    @Test
    public void testScenario_ThrowInMethod() {
        assertThrows("Should throw GeckoRuntimeException",
            GeckoRuntimeException.class,
            () -> throwTestException());
    }

    private void throwTestException() {
        throw new GeckoRuntimeException("Simulated error in method");
    }

    @Test
    public void testScenario_CatchAndRethrow() {
        try {
            throw new GeckoRuntimeException("Original error");
        } catch (GeckoRuntimeException e) {
            // Simulate rethrowing with additional context
            String contextMessage = "While processing: " + e.getMessage();
            GeckoRuntimeException rethrown = new GeckoRuntimeException(contextMessage);

            assertNotNull(rethrown);
            assertTrue(rethrown.getMessage().contains("While processing"));
        }
    }

    @Test
    public void testScenario_ErrorLogging() {
        String error = "Circuit analysis failed";
        GeckoRuntimeException exception = new GeckoRuntimeException(error);

        // Simulate error logging
        String logMessage = "GeckoRuntimeException: " + exception.getMessage();
        assertTrue("Logging should include message", logMessage.contains(error));
    }
}
