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
package ch.technokrat.gecko.core.control;

/**
 * Output data types for frequency analysis.
 *
 * Defines what type of frequency data to output from DFT/FFT operations.
 *
 * @author GeckoCIRCUITS Team
 */
public enum OutputDataType {
    ABS(1, "Magnitude"),
    REAL(2, "Real"),
    IMAG(3, "Imag"),
    PHASE(4, "Phase");

    private final int _integerCode;
    private final String _outputString;

    OutputDataType(final int code, final String outputString) {
        _integerCode = code;
        _outputString = outputString;
    }

    /**
     * Gets the integer code for serialization.
     *
     * @return the integer code
     */
    public int getIntegerCode() {
        return _integerCode;
    }

    /**
     * Gets the OutputDataType from its integer code.
     *
     * @param code the integer code
     * @return the corresponding OutputDataType, or ABS if not found
     */
    public static OutputDataType getFromIntCode(final int code) {
        for (OutputDataType compare : OutputDataType.values()) {
            if (compare.getIntegerCode() == code) {
                return compare;
            }
        }
        return OutputDataType.ABS;
    }

    @Override
    public String toString() {
        return _outputString;
    }
}
