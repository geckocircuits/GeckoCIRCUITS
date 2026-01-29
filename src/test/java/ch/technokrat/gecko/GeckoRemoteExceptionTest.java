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

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for GeckoRemoteException.
 * Tests exception construction and inheritance.
 */
public class GeckoRemoteExceptionTest {

    @Test
    public void testDefaultConstructor() {
        GeckoRemoteException exception = new GeckoRemoteException();
        assertNotNull(exception);
        assertNull(exception.getMessage());
    }

    @Test
    public void testConstructorWithMessage() {
        String message = "Test remote exception message";
        GeckoRemoteException exception = new GeckoRemoteException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    public void testConstructorWithMessageAndCause() {
        String message = "Test remote exception with cause";
        Throwable cause = new RuntimeException("Original cause");
        GeckoRemoteException exception = new GeckoRemoteException(message, cause);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    public void testExceptionIsSubclassOfException() {
        GeckoRemoteException exception = new GeckoRemoteException("test");
        assertTrue(exception instanceof Exception);
    }

    @Test
    public void testExceptionCanBeThrown() {
        GeckoRemoteException exception = new GeckoRemoteException("Test exception");
        try {
            throw exception;
        } catch (GeckoRemoteException e) {
            assertEquals("Test exception", e.getMessage());
        }
    }

    @Test
    public void testExceptionCause() {
        IOException ioException = new IOException("IO error");
        GeckoRemoteException exception = new GeckoRemoteException("Wrapped IO error", ioException);
        assertSame(ioException, exception.getCause());
    }

    @Test
    public void testMultipleExceptionsWithDifferentMessages() {
        GeckoRemoteException ex1 = new GeckoRemoteException("Message 1");
        GeckoRemoteException ex2 = new GeckoRemoteException("Message 2");
        assertNotEquals(ex1.getMessage(), ex2.getMessage());
    }

    @Test
    public void testNullMessageConstructor() {
        GeckoRemoteException exception = new GeckoRemoteException((String) null);
        assertNull(exception.getMessage());
    }

    @Test
    public void testEmptyMessageConstructor() {
        GeckoRemoteException exception = new GeckoRemoteException("");
        assertEquals("", exception.getMessage());
    }
}
