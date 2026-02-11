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
package ch.technokrat.gecko;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for GeckoRuntimeException.
 * Tests exception construction and runtime exception behavior.
 */
public class GeckoRuntimeExceptionTest {

    @Test
    public void testConstructorWithMessage() {
        String message = "Test runtime error message";
        GeckoRuntimeException exception = new GeckoRuntimeException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    public void testExceptionIsSubclassOfRuntimeException() {
        GeckoRuntimeException exception = new GeckoRuntimeException("test");
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testExceptionCanBeThrown() {
        GeckoRuntimeException exception = new GeckoRuntimeException("Test exception");
        try {
            throw exception;
        } catch (GeckoRuntimeException e) {
            assertEquals("Test exception", e.getMessage());
        }
    }

    @Test
    public void testExceptionCanBeCaughtAsRuntimeException() {
        GeckoRuntimeException exception = new GeckoRuntimeException("Test");
        try {
            throw exception;
        } catch (RuntimeException e) {
            assertEquals("Test", e.getMessage());
        }
    }

    @Test
    public void testMultipleExceptionsWithDifferentMessages() {
        GeckoRuntimeException ex1 = new GeckoRuntimeException("Error 1");
        GeckoRuntimeException ex2 = new GeckoRuntimeException("Error 2");
        assertNotEquals(ex1.getMessage(), ex2.getMessage());
    }

    @Test
    public void testNullMessageConstructor() {
        GeckoRuntimeException exception = new GeckoRuntimeException(null);
        assertNull(exception.getMessage());
    }

    @Test
    public void testEmptyMessageConstructor() {
        GeckoRuntimeException exception = new GeckoRuntimeException("");
        assertEquals("", exception.getMessage());
    }

    @Test
    public void testLongErrorMessage() {
        String longMessage = "This is a very long error message " + "x".repeat(100);
        GeckoRuntimeException exception = new GeckoRuntimeException(longMessage);
        assertEquals(longMessage, exception.getMessage());
    }

    @Test
    public void testExceptionStackTrace() {
        GeckoRuntimeException exception = new GeckoRuntimeException("Stack trace test");
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
    }
}
