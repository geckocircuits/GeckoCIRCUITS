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
 * Enumeration of connector types for circuit connections.
 *
 * <p>This is the GUI-free core version containing only the enum values.
 * Display properties (colors, etc.) are handled in the GUI layer.
 *
 * @since Core Module Extraction Sprint
 */
public enum ConnectorType {

    /** Power circuit connections (LK = Leistungskreis) */
    LK,

    /** Control signal connections */
    CONTROL,

    /** Magnetic reluctance connections */
    RELUCTANCE,

    /** Combined power and reluctance connection */
    LK_AND_RELUCTANCE,

    /** Thermal connections */
    THERMAL,

    /** No specific connection type */
    NONE;

    /**
     * Gets a ConnectorType from its ordinal value.
     *
     * @param ord the ordinal value
     * @return the corresponding ConnectorType
     * @throws IllegalArgumentException if ordinal is invalid
     */
    public static ConnectorType fromOrdinal(int ord) {
        for (ConnectorType type : values()) {
            if (type.ordinal() == ord) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid ConnectorType ordinal: " + ord);
    }
}
