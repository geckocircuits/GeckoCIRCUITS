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

import java.util.Objects;

/**
 * GUI-free frequency data configuration for sliding DFT calculations.
 *
 * This is a simple POJO that holds frequency and output type information
 * without the GUI-coupled UserParameter dependencies.
 *
 * @author GeckoCIRCUITS Team
 */
public final class FrequencyDataCore {
    private final double _frequency;
    private final OutputDataType _outputData;

    /**
     * Creates a frequency data configuration.
     *
     * @param frequency the frequency in Hz
     * @param outputData the type of output data (magnitude, real, imag, phase)
     */
    public FrequencyDataCore(final double frequency, final OutputDataType outputData) {
        _frequency = frequency;
        _outputData = outputData;
    }

    /**
     * Gets the frequency value.
     *
     * @return the frequency in Hz
     */
    public double getFrequency() {
        return _frequency;
    }

    /**
     * Gets the output data type.
     *
     * @return the output data type
     */
    public OutputDataType getOutputData() {
        return _outputData;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final FrequencyDataCore other = (FrequencyDataCore) obj;
        return Double.compare(_frequency, other._frequency) == 0
                && _outputData == other._outputData;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_frequency, _outputData);
    }
}
