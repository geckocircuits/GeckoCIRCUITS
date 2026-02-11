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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Common test utilities for GeckoCIRCUITS tests.
 *
 * Provides assertion helpers and common test patterns used across the test suite.
 */
public final class GeckoTestUtils {

    /** Default tolerance for double comparisons in circuit simulations */
    public static final double DEFAULT_TOLERANCE = 1e-9;

    /** Typical simulation time step */
    public static final double DEFAULT_DT = 1e-6;

    /** Relaxed tolerance for numerical computations with accumulated error */
    public static final double RELAXED_TOLERANCE = 1e-6;

    private GeckoTestUtils() {
        // Utility class - no instantiation
    }

    /**
     * Assert two doubles are equal within the default tolerance.
     *
     * @param message Description of what is being compared
     * @param expected Expected value
     * @param actual Actual value
     */
    public static void assertEqualsWithTolerance(String message, double expected, double actual) {
        assertEquals(message, expected, actual, DEFAULT_TOLERANCE);
    }

    /**
     * Assert two doubles are equal within a specified tolerance.
     *
     * @param message Description of what is being compared
     * @param expected Expected value
     * @param actual Actual value
     * @param tolerance Maximum allowed difference
     */
    public static void assertEqualsWithTolerance(String message, double expected, double actual, double tolerance) {
        assertEquals(message, expected, actual, tolerance);
    }

    /**
     * Assert a value is within expected range (inclusive).
     *
     * @param message Description of the assertion
     * @param min Minimum expected value (inclusive)
     * @param max Maximum expected value (inclusive)
     * @param actual Actual value to check
     */
    public static void assertInRange(String message, double min, double max, double actual) {
        assertTrue(message + " - expected in range [" + min + ", " + max + "] but was " + actual,
                actual >= min && actual <= max);
    }

    /**
     * Assert a value is positive (greater than zero).
     *
     * @param message Description of the assertion
     * @param value Value to check
     */
    public static void assertPositive(String message, double value) {
        assertTrue(message + " - expected positive but was " + value, value > 0);
    }

    /**
     * Assert a value is non-negative (zero or positive).
     *
     * @param message Description of the assertion
     * @param value Value to check
     */
    public static void assertNonNegative(String message, double value) {
        assertTrue(message + " - expected non-negative but was " + value, value >= 0);
    }

    /**
     * Assert two arrays are equal within tolerance.
     *
     * @param message Description of what is being compared
     * @param expected Expected array
     * @param actual Actual array
     * @param tolerance Maximum allowed difference per element
     */
    public static void assertArrayEquals(String message, double[] expected, double[] actual, double tolerance) {
        if (expected == null && actual == null) {
            return;
        }
        if (expected == null || actual == null) {
            fail(message + " - one array is null");
        }
        assertEquals(message + " - array lengths differ", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(message + " at index " + i, expected[i], actual[i], tolerance);
        }
    }

    /**
     * Assert a 2D array has the expected dimensions.
     *
     * @param message Description of the assertion
     * @param expectedRows Expected number of rows
     * @param expectedCols Expected number of columns
     * @param array Array to check
     */
    public static void assertArrayDimensions(String message, int expectedRows, int expectedCols, double[][] array) {
        if (array == null) {
            fail(message + " - array is null");
        }
        assertEquals(message + " - row count", expectedRows, array.length);
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) {
                fail(message + " - row " + i + " is null");
            }
            assertEquals(message + " - column count at row " + i, expectedCols, array[i].length);
        }
    }

    /**
     * Create a 1D input signal array for control calculator tests.
     *
     * @param value The signal value
     * @return A double[1] array containing the value
     */
    public static double[] createSignal(double value) {
        return new double[]{value};
    }

    /**
     * Create a 2D input signal array for control calculator tests.
     *
     * @param values The signal values
     * @return A double[n][1] array where each row contains one value
     */
    public static double[][] createInputSignals(double... values) {
        double[][] signals = new double[values.length][1];
        for (int i = 0; i < values.length; i++) {
            signals[i][0] = values[i];
        }
        return signals;
    }
}
