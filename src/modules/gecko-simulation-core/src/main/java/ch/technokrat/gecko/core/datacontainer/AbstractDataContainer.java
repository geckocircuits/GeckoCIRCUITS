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

import java.util.Observable;

/**
 * A powerful data storage object, keeps data information as e.g.
 * minimum-maximum values, signal names, etc.
 *
 * GUI-free version for use in headless simulation core.
 */
public abstract class AbstractDataContainer extends Observable {

    /**
     * Gets the minimum and maximum values in the specified column range.
     *
     * @param row the row where to search inside
     * @param columnMin minimum column value
     * @param columnMax maximum column value
     * @return the MinMax-Data from columnMin to columnMax
     */
    public abstract HiLoData getHiLoValue(final int row, final int columnMin, final int columnMax);

    /**
     * Gets a single value at the specified row and column.
     *
     * @param row the signal row index
     * @param column the time index
     * @return the value
     */
    public abstract float getValue(final int row, final int column);

    /**
     * Gets the number of signal rows in this container.
     *
     * @return the row count
     */
    public abstract int getRowLength();

    /**
     * Gets the time value at the specified index.
     *
     * @param index the time index
     * @param row the row (some containers may have different time bases per row)
     * @return the time value
     */
    public abstract double getTimeValue(final int index, final int row);

    /**
     * Gets the maximum valid time index for the specified row.
     *
     * @param row the row index
     * @return the maximum time index
     */
    public abstract int getMaximumTimeIndex(final int row);

    /**
     * Gets data value(s) in the specified time interval.
     * May return a single value or HiLoData depending on the interval contents.
     *
     * @param intervalStart start time
     * @param intervalStop stop time
     * @param columnIndex the signal row index
     * @return the data value(s) in the interval
     */
    public abstract Object getDataValueInInterval(final double intervalStart, final double intervalStop, final int columnIndex);

    /**
     * Gets the absolute minimum and maximum values for the specified row.
     *
     * @param row the signal row index
     * @return the absolute min/max values
     */
    public abstract HiLoData getAbsoluteMinMaxValue(int row);

    /**
     * Finds the time index corresponding to the specified time value.
     *
     * @param time the time value to search for
     * @param row the row index
     * @return the time index
     */
    public abstract int findTimeIndex(final double time, final int row);

    /**
     * Gets the signal name for the specified row.
     *
     * @param row the signal row index
     * @return the signal name
     */
    public abstract String getSignalName(final int row);

    /**
     * Gets the name of the X (time) data.
     *
     * @return the X data name
     */
    public abstract String getXDataName();

    /**
     * Gets the current container status.
     *
     * @return the container status
     */
    public abstract ContainerStatus getContainerStatus();

    /**
     * Sets the container status.
     *
     * @param containerStatus the new status
     */
    public abstract void setContainerStatus(final ContainerStatus containerStatus);

    /**
     * Checks if the specified row contains invalid (NaN or infinite) numbers.
     *
     * @param row the signal row index
     * @return true if the row contains invalid numbers
     */
    public abstract boolean isInvalidNumbers(final int row);

    /**
     * Gets the time series for the specified row.
     *
     * @param row the row index
     * @return the time series
     */
    public abstract AbstractTimeSerie getTimeSeries(final int row);

    /**
     * Gets the raw data array.
     *
     * @return the data array
     */
    public abstract float[] getDataArray();

    /**
     * Gets the subcircuit signal path for the specified row.
     *
     * @param row the signal row index
     * @return the subcircuit path (empty string if none)
     */
    public String getSubcircuitSignalPath(final int row) {
        return "";
    }

    /**
     * Sets the signal path name for the specified row.
     *
     * @param containerRowIndex the signal row index
     * @param subcircuitPath the subcircuit path
     */
    void setSignalPathName(int containerRowIndex, String subcircuitPath) {
    }
}
