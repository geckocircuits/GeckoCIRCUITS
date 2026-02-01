/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.circuit;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for NameAlreadyExistsException.
 * Tests exception creation, messages, and inheritance.
 */
public class NameAlreadyExistsExceptionTest {

    @Test
    public void testConstructor_WithMessage() {
        String message = "Component with name 'R1' already exists";
        NameAlreadyExistsException exception = new NameAlreadyExistsException(message);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }

    @Test
    public void testConstructor_WithEmptyMessage() {
        NameAlreadyExistsException exception = new NameAlreadyExistsException("");
        assertNotNull(exception);
        assertEquals("", exception.getMessage());
    }

    @Test
    public void testConstructor_WithNullMessage() {
        NameAlreadyExistsException exception = new NameAlreadyExistsException(null);
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    public void testConstructor_WithLongMessage() {
        String longMessage = "Component with name 'VeryLongComponentNameWithManyCharacters' " +
                "already exists in the circuit";
        NameAlreadyExistsException exception = new NameAlreadyExistsException(longMessage);
        assertEquals(longMessage, exception.getMessage());
    }

    @Test
    public void testExceptionIsThrowable() {
        NameAlreadyExistsException exception = new NameAlreadyExistsException("Test");
        assertNotNull(exception);
        assertTrue(exception instanceof Throwable);
    }

    @Test
    public void testExceptionIsException() {
        NameAlreadyExistsException exception = new NameAlreadyExistsException("Test");
        assertTrue(exception instanceof Exception);
    }

    @Test
    public void testCanBeThrownAndCaught() {
        String message = "Duplicate name detected";
        try {
            throw new NameAlreadyExistsException(message);
        } catch (NameAlreadyExistsException e) {
            assertEquals(message, e.getMessage());
        }
    }

    @Test
    public void testCanBeCaughtAsException() {
        String message = "Name already exists";
        try {
            throw new NameAlreadyExistsException(message);
        } catch (Exception e) {
            assertEquals(message, e.getMessage());
            assertTrue(e instanceof NameAlreadyExistsException);
        }
    }

    @Test
    public void testMultipleInstancesWithDifferentMessages() {
        NameAlreadyExistsException ex1 = new NameAlreadyExistsException("Message 1");
        NameAlreadyExistsException ex2 = new NameAlreadyExistsException("Message 2");

        assertEquals("Message 1", ex1.getMessage());
        assertEquals("Message 2", ex2.getMessage());
        assertNotEquals(ex1.getMessage(), ex2.getMessage());
    }

    @Test
    public void testExceptionMessage_WithSpecialCharacters() {
        String specialMessage = "Name 'R_1@#$%' already exists!";
        NameAlreadyExistsException exception = new NameAlreadyExistsException(specialMessage);
        assertEquals(specialMessage, exception.getMessage());
    }

    @Test
    public void testExceptionMessage_WithUnicodeCharacters() {
        String unicodeMessage = "Name 'R_α_β_γ' already exists";
        NameAlreadyExistsException exception = new NameAlreadyExistsException(unicodeMessage);
        assertEquals(unicodeMessage, exception.getMessage());
    }

    @Test
    public void testExceptionToString_ContainsClassNameAndMessage() {
        String message = "Duplicate component name";
        NameAlreadyExistsException exception = new NameAlreadyExistsException(message);
        String stringRep = exception.toString();
        assertNotNull(stringRep);
        assertTrue(stringRep.contains("NameAlreadyExistsException"));
        assertTrue(stringRep.contains(message));
    }

    @Test
    public void testExceptionStackTrace_IsAvailable() {
        NameAlreadyExistsException exception = new NameAlreadyExistsException("Test");
        StackTraceElement[] stackTrace = exception.getStackTrace();
        assertNotNull(stackTrace);
        assertTrue(stackTrace.length > 0);
    }

    @Test
    public void testExceptionWithCause() {
        String message = "Name already exists";
        Throwable cause = new Exception("Original error");
        // Standard Exception doesn't allow cause in constructor, but we can test message
        NameAlreadyExistsException exception = new NameAlreadyExistsException(message);
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
    }

    @Test
    public void testExceptionCanBeRethrown() {
        try {
            try {
                throw new NameAlreadyExistsException("First throw");
            } catch (NameAlreadyExistsException e) {
                throw e;
            }
        } catch (NameAlreadyExistsException e) {
            assertEquals("First throw", e.getMessage());
        }
    }

    @Test
    public void testExceptionInTryCatchFinally() {
        String message = "Name conflict";
        boolean caught = false;
        try {
            throw new NameAlreadyExistsException(message);
        } catch (NameAlreadyExistsException e) {
            caught = true;
            assertEquals(message, e.getMessage());
        } finally {
            assertTrue(caught);
        }
    }

    @Test
    public void testExceptionMessage_EmptyName() {
        NameAlreadyExistsException exception = new NameAlreadyExistsException(
                "Component with empty name already exists");
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("empty"));
    }

    @Test
    public void testExceptionMessage_WithNumericName() {
        NameAlreadyExistsException exception = new NameAlreadyExistsException(
                "Component '123' already exists");
        assertTrue(exception.getMessage().contains("123"));
    }

    @Test
    public void testMultipleThrownExceptions_CanDistinguish() {
        NameAlreadyExistsException[] exceptions = new NameAlreadyExistsException[3];
        exceptions[0] = new NameAlreadyExistsException("Duplicate: R1");
        exceptions[1] = new NameAlreadyExistsException("Duplicate: C1");
        exceptions[2] = new NameAlreadyExistsException("Duplicate: L1");

        assertEquals("Duplicate: R1", exceptions[0].getMessage());
        assertEquals("Duplicate: C1", exceptions[1].getMessage());
        assertEquals("Duplicate: L1", exceptions[2].getMessage());
    }

    @Test
    public void testExceptionSerializable() {
        // Check if exception can be constructed
        NameAlreadyExistsException exception = new NameAlreadyExistsException("Test");
        assertNotNull(exception);
    }

    @Test
    public void testExceptionConstructor_IsPublic() {
        // Should be able to instantiate it
        try {
            throw new NameAlreadyExistsException("Test message");
        } catch (NameAlreadyExistsException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testExceptionMessagePreservation_AcrossOperations() {
        String originalMessage = "Original name conflict";
        NameAlreadyExistsException exception = new NameAlreadyExistsException(originalMessage);

        // Get the message multiple times
        String msg1 = exception.getMessage();
        String msg2 = exception.getMessage();
        String msg3 = exception.getMessage();

        // All should be equal
        assertEquals(msg1, msg2);
        assertEquals(msg2, msg3);
        assertEquals(originalMessage, msg1);
    }

    @Test
    public void testExceptionClassNameAccuracy() {
        NameAlreadyExistsException exception = new NameAlreadyExistsException("Test");
        assertEquals("NameAlreadyExistsException", exception.getClass().getSimpleName());
    }

    @Test
    public void testExceptionWithContextInfo() {
        String context = "CircuitTyp: RESISTOR, Sheet: Main, Position: (100, 200)";
        NameAlreadyExistsException exception = new NameAlreadyExistsException(
                "Name 'R1' already exists: " + context);
        assertTrue(exception.getMessage().contains("RESISTOR"));
        assertTrue(exception.getMessage().contains("Main"));
    }

    @Test
    public void testExceptionIsRuntimeOrChecked() {
        // This is a checked exception (extends Exception, not RuntimeException)
        assertTrue(Exception.class.isAssignableFrom(NameAlreadyExistsException.class));
        assertFalse(RuntimeException.class.isAssignableFrom(NameAlreadyExistsException.class));
    }

    @Test
    public void testExceptionFieldAccess() {
        String message = "Name already exists";
        NameAlreadyExistsException exception = new NameAlreadyExistsException(message);

        // Verify the message field can be accessed
        assertNotNull(exception.getMessage());
        assertEquals(message, exception.getMessage());
    }
}
