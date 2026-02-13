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
package gecko.core.circuit;

/**
 * Constants for .ipes circuit file parsing and serialization.
 *
 * <p>These constants define the ASCII format used in GeckoCIRCUITS circuit files:
 * <ul>
 *   <li>Separator character for array elements</li>
 *   <li>Null/undefined value marker</li>
 * </ul>
 *
 * @since Core Module Extraction Sprint
 */
public final class CircuitFileConstants {

    /** Separator used in ASCII string arrays within circuit files */
    public static final String SEPARATOR_ASCII_STRINGARRAY = "/";

    /** Marker for null/undefined values in circuit files */
    public static final String NIX = "NIX_NIX_NIX";

    private CircuitFileConstants() {
        // Utility class - no instantiation
    }
}
