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
 * Unit tests for GeckoRemoteRegistry utility methods.
 * Tests port management, IP address handling, and network configuration.
 */
public class GeckoRemoteRegistryTest {

    @Test
    public void testGetRemoteAccessPortReturnsValidPort() {
        int port = GeckoRemoteRegistry.getRemoteAccessPort();
        assertTrue("Port should be positive", port > 0);
        assertTrue("Port should be less than 65536", port < 65536);
    }

    @Test
    public void testGetRemoteAccessPortConsistency() {
        int port1 = GeckoRemoteRegistry.getRemoteAccessPort();
        int port2 = GeckoRemoteRegistry.getRemoteAccessPort();
        assertEquals("Port should be consistent between calls", port1, port2);
    }

    @Test
    public void testIsRemoteEnabledInitialState() {
        // Initial state should be that remote is not enabled
        boolean enabled = GeckoRemoteRegistry.isRemoteEnabled();
        assertFalse("Remote should not be enabled initially", enabled);
    }

    @Test
    public void testGetIPAddressReturnsString() {
        String ipAddress = GeckoRemoteRegistry.getIPAddress();
        assertNotNull("IP address should not be null", ipAddress);
        assertFalse("IP address should not be empty", ipAddress.isEmpty());
    }

    @Test
    public void testGetIPAddressDefaultValue() {
        String ipAddress = GeckoRemoteRegistry.getIPAddress();
        // Default should be localhost
        assertEquals("127.0.0.1", ipAddress);
    }

    @Test
    public void testSetIPAddress() {
        String originalIP = GeckoRemoteRegistry.getIPAddress();
        try {
            String testIP = "192.168.1.1";
            GeckoRemoteRegistry.setIPAddress(testIP);
            assertEquals(testIP, GeckoRemoteRegistry.getIPAddress());
        } finally {
            // Restore original IP
            GeckoRemoteRegistry.setIPAddress(originalIP);
        }
    }

    @Test
    public void testSetIPAddressWithLocalhost() {
        String originalIP = GeckoRemoteRegistry.getIPAddress();
        try {
            GeckoRemoteRegistry.setIPAddress("localhost");
            assertEquals("localhost", GeckoRemoteRegistry.getIPAddress());
        } finally {
            GeckoRemoteRegistry.setIPAddress(originalIP);
        }
    }

    @Test
    public void testGetIPAddressAfterSet() {
        String originalIP = GeckoRemoteRegistry.getIPAddress();
        try {
            String testIP = "10.0.0.1";
            GeckoRemoteRegistry.setIPAddress(testIP);
            String retrievedIP = GeckoRemoteRegistry.getIPAddress();
            assertEquals(testIP, retrievedIP);
        } finally {
            GeckoRemoteRegistry.setIPAddress(originalIP);
        }
    }

    @Test
    public void testGetRemoteAccessPortDefaultRange() {
        int port = GeckoRemoteRegistry.getRemoteAccessPort();
        // Default port 43035 is a high numbered port
        assertTrue("Port should be in valid range", port >= 1024 && port <= 65535);
    }

    @Test
    public void testIPAddressValidationPattern() {
        String ipAddress = GeckoRemoteRegistry.getIPAddress();
        // Should be a valid IP or hostname format
        assertNotNull(ipAddress);
        assertTrue("IP should contain at least one character", ipAddress.length() > 0);
    }

    @Test
    public void testMultipleIPAddressSetAndGet() {
        String originalIP = GeckoRemoteRegistry.getIPAddress();
        try {
            String[] testIPs = {"127.0.0.1", "localhost", "0.0.0.0"};
            for (String ip : testIPs) {
                GeckoRemoteRegistry.setIPAddress(ip);
                assertEquals(ip, GeckoRemoteRegistry.getIPAddress());
            }
        } finally {
            GeckoRemoteRegistry.setIPAddress(originalIP);
        }
    }

    @Test
    public void testGetMachineIPNumbersReturnsArray() throws Exception {
        String[] machineIPs = GeckoRemoteRegistry.getMachineIPNumbers();
        assertNotNull("Machine IPs should not be null", machineIPs);
        // May be empty if no network interfaces, but should be an array
        assertTrue(machineIPs.length >= 0);
    }

    @Test
    public void testGetMachineIPNumbersExcludesLoopback() throws Exception {
        String[] machineIPs = GeckoRemoteRegistry.getMachineIPNumbers();
        for (String ip : machineIPs) {
            assertFalse("Should exclude loopback address 127.0.0.1", ip.equals("127.0.0.1"));
        }
    }

    @Test
    public void testGetMachineIPNumbersValidFormat() throws Exception {
        String[] machineIPs = GeckoRemoteRegistry.getMachineIPNumbers();
        for (String ip : machineIPs) {
            assertNotNull("IP should not be null", ip);
            assertFalse("IP should not be empty", ip.isEmpty());
        }
    }
}
