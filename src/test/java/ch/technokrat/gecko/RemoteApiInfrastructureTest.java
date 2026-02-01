/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko;

import ch.technokrat.gecko.geckocircuits.api.ISimulatorAccess;
import ch.technokrat.gecko.geckocircuits.api.SimulatorAccessException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for remote API infrastructure classes.
 *
 * These tests verify the basic structure and behavior of the remote API
 * infrastructure without requiring actual RMI or network connections.
 */
public class RemoteApiInfrastructureTest {

    @Test
    public void testMethodCategory_AllValuesExist() {
        // Verify all method categories exist
        assertNotNull(MethodCategory.SIMULATION_START);
        assertNotNull(MethodCategory.LOAD_SAVE_MODEL);
        assertNotNull(MethodCategory.SIGNAL_PROCESSING);
        assertNotNull(MethodCategory.COMPONENT_PROPERTIES);
        assertNotNull(MethodCategory.COMPONENT_CREATION_LISTING);
        assertNotNull(MethodCategory.ALL_CATEGORIES);
    }

    @Test
    public void testMethodCategory_ValuesCount() {
        assertEquals("Should have 6 method categories", 6, MethodCategory.values().length);
    }

    @Test
    public void testGeckoRemoteException_Constructor() {
        GeckoRemoteException ex = new GeckoRemoteException("test message");
        assertNotNull(ex);
        assertEquals("test message", ex.getMessage());
    }

    @Test
    public void testGeckoRemoteException_ConstructorWithCause() {
        RuntimeException cause = new RuntimeException("original cause");
        GeckoRemoteException ex = new GeckoRemoteException("test message", cause);
        assertEquals("test message", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    public void testSimulatorAccessException_Constructor() {
        SimulatorAccessException ex = new SimulatorAccessException("access error");
        assertNotNull(ex);
        assertEquals("access error", ex.getMessage());
    }

    @Test
    public void testSimulatorAccessException_WithCause() {
        Exception cause = new Exception("underlying error");
        SimulatorAccessException ex = new SimulatorAccessException("access error", cause);
        assertEquals("access error", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    public void testSimulatorAccessException_FromCause() {
        Exception cause = new Exception("underlying error");
        SimulatorAccessException ex = new SimulatorAccessException(cause);
        assertEquals(cause, ex.getCause());
    }

    @Test
    public void testISimulatorAccess_AccessMode_AllValuesExist() {
        assertNotNull(ISimulatorAccess.AccessMode.STANDALONE);
        assertNotNull(ISimulatorAccess.AccessMode.RMI);
        assertNotNull(ISimulatorAccess.AccessMode.MMF);
        assertNotNull(ISimulatorAccess.AccessMode.PIPE);
    }

    @Test
    public void testISimulatorAccess_AccessMode_Count() {
        assertEquals("Should have 4 access modes", 4, ISimulatorAccess.AccessMode.values().length);
    }

    @Test
    public void testGeckoRemoteInterface_IsRemoteInterface() {
        // Verify GeckoRemoteInterface extends java.rmi.Remote
        assertTrue("GeckoRemoteInterface should extend Remote",
                java.rmi.Remote.class.isAssignableFrom(GeckoRemoteInterface.class));
    }

    @Test
    public void testGeckoRemoteInterface_HasManyMethods() {
        // GeckoRemoteInterface should have many methods (comprehensive API)
        int methodCount = GeckoRemoteInterface.class.getDeclaredMethods().length;
        assertTrue("GeckoRemoteInterface should have many methods (got " + methodCount + ")",
                methodCount > 50);
    }

    @Test
    public void testGeckoRemoteIntWithoutExc_Exists() {
        // Verify the non-exception interface exists
        assertNotNull(GeckoRemoteIntWithoutExc.class);
    }
}
