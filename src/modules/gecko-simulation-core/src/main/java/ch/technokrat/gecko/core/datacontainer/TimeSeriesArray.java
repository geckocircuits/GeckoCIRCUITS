/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.core.datacontainer;

/**
 * Time series implementation for variable time steps.
 * Stores all time values in an array, suitable for adaptive time stepping.
 *
 * GUI-free version for use in headless simulation core.
 */
public final class TimeSeriesArray extends AbstractTimeSerie {

    private double[] _timeSeries;
    private int _maximumIndex = -1;

    /**
     * Creates a time series array with the specified capacity.
     *
     * @param arraySize maximum number of time steps to store
     */
    public TimeSeriesArray(final int arraySize) {
        super();
        _timeSeries = new double[arraySize];
    }

    @Override
    public void setValue(final int index, final double value) {
        _timeSeries[index] = value;
        _maximumIndex = Math.max(index, _maximumIndex);
    }

    @Override
    public double getValue(final int index) {
        assert index >= 0;
        assert index < _timeSeries.length;
        return _timeSeries[index];
    }

    @Override
    public int getMaximumIndex() {
        return _maximumIndex;
    }

    @Override
    public int findTimeIndex(final double time) {
        final int maxIndex = _maximumIndex;
        if (maxIndex < 0) {
            return 0;
        }

        final double maxTime = getValue(maxIndex);
        final double minTime = getValue(0);

        int estimatedIndex = (int) (maxIndex * (time - minTime) / (maxTime - minTime));
        if (estimatedIndex > maxIndex) {
            estimatedIndex = maxIndex;
        }
        if (estimatedIndex < 0) {
            estimatedIndex = 0;
        }

        // Search backwards if estimate is too high
        while (getValue(estimatedIndex) > time) {
            estimatedIndex -= FIND_OVER_STEP;
            if (estimatedIndex < 0) {
                estimatedIndex = 0;
                break;
            }
        }

        // Search forward to find exact position
        int returnValue = estimatedIndex;
        for (int i = estimatedIndex; i < maxIndex; i++) {
            if (getValue(i) < time) {
                returnValue = i;
            } else {
                return Math.max(returnValue, 0);
            }
        }

        return Math.max(returnValue, 0);
    }

    @Override
    public double getLastTimeInterval() {
        if (_maximumIndex < 1) {
            return 0;
        }
        return _timeSeries[_maximumIndex] - _timeSeries[_maximumIndex - 1];
    }

    /**
     * Gets the capacity of this time series array.
     *
     * @return the array size
     */
    public int getCapacity() {
        return _timeSeries.length;
    }
}
