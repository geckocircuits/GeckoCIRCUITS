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
package gecko.core.io;

/**
 * Utility methods for serializing values to ASCII format in circuit files.
 *
 * <p>These methods append values to a StringBuffer with a space prefix,
 * used for writing .ipes circuit file format.
 *
 * <p>Extracted from ProjectData for headless/API use.
 *
 * @since Core Module Extraction Sprint
 */
public final class SerializationUtils {

    private SerializationUtils() {
        // Utility class - no instantiation
    }

    /**
     * Appends an integer value to the buffer with a space prefix.
     *
     * @param buffer the string buffer to append to
     * @param value the integer value
     */
    public static void appendAsString(StringBuffer buffer, int value) {
        buffer.append(' ');
        buffer.append(value);
    }

    /**
     * Appends a double value to the buffer with a space prefix.
     *
     * @param buffer the string buffer to append to
     * @param value the double value
     */
    public static void appendAsString(StringBuffer buffer, double value) {
        buffer.append(' ');
        buffer.append(value);
    }

    /**
     * Appends a long value to the buffer with a space prefix.
     *
     * @param buffer the string buffer to append to
     * @param value the long value
     */
    public static void appendAsString(StringBuffer buffer, long value) {
        buffer.append(' ');
        buffer.append(value);
    }

    /**
     * Appends a boolean value to the buffer with a space prefix.
     *
     * @param buffer the string buffer to append to
     * @param value the boolean value
     */
    public static void appendAsString(StringBuffer buffer, boolean value) {
        buffer.append(' ');
        buffer.append(value);
    }

    /**
     * Appends a String value to the buffer with a space prefix.
     *
     * @param buffer the string buffer to append to
     * @param value the string value
     */
    public static void appendAsString(StringBuffer buffer, String value) {
        buffer.append(' ');
        buffer.append(value);
    }

    /**
     * Appends an array of objects to the buffer.
     * Each element is converted to string and separated by spaces.
     *
     * @param buffer the string buffer to append to
     * @param values the array of values
     */
    public static void appendAsString(StringBuffer buffer, Object[] values) {
        for (Object value : values) {
            buffer.append(' ');
            buffer.append(value);
        }
    }

    /**
     * Appends a byte array to the buffer.
     * Each byte is converted to string and separated by spaces.
     *
     * @param buffer the string buffer to append to
     * @param values the byte array
     */
    public static void appendAsString(StringBuffer buffer, byte[] values) {
        for (byte value : values) {
            buffer.append(' ');
            buffer.append(value);
        }
    }

    /**
     * Appends a named parameter to the buffer.
     * Format: " identifier value"
     *
     * @param buffer the string buffer to append to
     * @param identifier the parameter identifier
     * @param value the parameter value
     */
    public static void appendAsString(StringBuffer buffer, String identifier, Object value) {
        buffer.append(' ');
        buffer.append(identifier);
        buffer.append(' ');
        buffer.append(value);
    }

    /**
     * Appends a 2D double array to the buffer.
     * Format: "[][] rows cols value1 value2 ..."
     * This matches the format expected by TokenMap.readDataLine(String, double[][]).
     * The identifier "[][]" is appended first to match the TokenMap convention.
     *
     * @param buffer the string buffer to append to
     * @param values the 2D double array
     */
    public static void appendAsString(StringBuffer buffer, double[][] values) {
        if (values == null || values.length == 0) {
            buffer.append("[][] 0 0");
            return;
        }

        // Write "[][]" identifier first (TokenMap convention)
        buffer.append("[][]");

        // Write dimensions
        buffer.append(' ');
        buffer.append(values.length);
        buffer.append(' ');
        buffer.append(values[0].length);

        // Write values
        for (double[] row : values) {
            for (double value : row) {
                buffer.append(' ');
                buffer.append(value);
            }
        }
    }
}
