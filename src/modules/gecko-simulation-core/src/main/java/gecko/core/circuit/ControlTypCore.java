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
 * Control component type IDs used in .ipes files.
 * Maps the integer {@code typ} field in {@code <ElementCONTROL>} blocks to semantic names.
 *
 * <p>Note: SIGNAL sources use sub-type encoding in {@code parameter[0]}:
 * 400=sine, 401=triangle, 404=rectangle.</p>
 */
public enum ControlTypCore {

    VOLT(1, "Voltage probe"),
    AMP(2, "Current probe"),
    CONST(3, "Constant source"),
    SIGNAL(4, "Signal generator"),
    SCOPE(5, "Oscilloscope"),
    GATE(6, "Gate driver"),
    GAIN(7, "Gain block");

    private final int typeNumber;
    private final String description;

    ControlTypCore(int typeNumber, String description) {
        this.typeNumber = typeNumber;
        this.description = description;
    }

    public int getTypeNumber() {
        return typeNumber;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns the enum constant for the given .ipes type number.
     *
     * @param typeNumber the integer type from the {@code typ} field
     * @return the matching enum constant
     * @throws IllegalArgumentException if no constant matches
     */
    public static ControlTypCore fromTypeNumber(int typeNumber) {
        for (ControlTypCore t : values()) {
            if (t.typeNumber == typeNumber) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown control type number: " + typeNumber);
    }

    /**
     * Returns true if the given type number is a known control type.
     *
     * @param typeNumber the integer type from the {@code typ} field
     * @return true if recognized
     */
    public static boolean isKnown(int typeNumber) {
        for (ControlTypCore t : values()) {
            if (t.typeNumber == typeNumber) {
                return true;
            }
        }
        return false;
    }
}
