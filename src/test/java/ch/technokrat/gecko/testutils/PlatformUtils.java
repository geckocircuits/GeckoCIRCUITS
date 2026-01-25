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
package ch.technokrat.gecko.testutils;

import org.junit.Assume;

/**
 * Platform detection utilities for conditional test execution.
 *
 * Use these methods with JUnit's Assume to skip tests that require specific platforms.
 * Tests skipped via Assume are reported as "ignored" rather than "failed".
 *
 * Example usage:
 * <pre>
 * {@code
 * @Test
 * public void testWindowsSpecificFeature() {
 *     PlatformUtils.assumeWindows("This test requires Windows DLL");
 *     // ... test code that requires Windows
 * }
 * }
 * </pre>
 */
public final class PlatformUtils {

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    public static final boolean IS_WINDOWS = OS_NAME.contains("windows");
    public static final boolean IS_LINUX = OS_NAME.contains("linux");
    public static final boolean IS_MAC = OS_NAME.contains("mac") || OS_NAME.contains("darwin");
    public static final boolean IS_UNIX = IS_LINUX || IS_MAC || OS_NAME.contains("nix") || OS_NAME.contains("aix");

    private PlatformUtils() {
        // Utility class - no instantiation
    }

    /**
     * Skip the current test if not running on Windows.
     *
     * @param reason Description of why Windows is required
     */
    public static void assumeWindows(String reason) {
        Assume.assumeTrue("Skipping: " + reason + " (requires Windows)", IS_WINDOWS);
    }

    /**
     * Skip the current test if not running on Linux.
     *
     * @param reason Description of why Linux is required
     */
    public static void assumeLinux(String reason) {
        Assume.assumeTrue("Skipping: " + reason + " (requires Linux)", IS_LINUX);
    }

    /**
     * Skip the current test if not running on macOS.
     *
     * @param reason Description of why macOS is required
     */
    public static void assumeMac(String reason) {
        Assume.assumeTrue("Skipping: " + reason + " (requires macOS)", IS_MAC);
    }

    /**
     * Skip the current test if not running on a Unix-like system.
     *
     * @param reason Description of why Unix is required
     */
    public static void assumeUnix(String reason) {
        Assume.assumeTrue("Skipping: " + reason + " (requires Unix-like OS)", IS_UNIX);
    }

    /**
     * Skip the current test if running in a headless environment.
     *
     * @param reason Description of why a display is required
     */
    public static void assumeDisplay(String reason) {
        boolean hasDisplay = !java.awt.GraphicsEnvironment.isHeadless();
        Assume.assumeTrue("Skipping: " + reason + " (requires display)", hasDisplay);
    }

    /**
     * Get the current operating system name.
     *
     * @return Lowercase OS name
     */
    public static String getOsName() {
        return OS_NAME;
    }
}
