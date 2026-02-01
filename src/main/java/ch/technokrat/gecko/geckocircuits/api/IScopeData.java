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
package ch.technokrat.gecko.geckocircuits.api;

import ch.technokrat.gecko.geckocircuits.datacontainer.ContainerStatus;
import ch.technokrat.gecko.geckocircuits.newscope.AbstractTimeSerie;
import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;

/**
 * Interface for scope data access.
 *
 * Defines the contract for accessing simulation data displayed in scope views.
 * This interface decouples scope rendering components from the concrete data
 * container implementations.
 *
 * <p>Implementations provide access to time series data, signal values, and
 * metadata about the data being displayed. The interface supports efficient
 * range queries through HiLoData for rendering compressed views.
 *
 * @see ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer
 * @see ch.technokrat.gecko.geckocircuits.newscope.DataContainerManyTimeSeries
 */
public interface IScopeData {

    /**
     * Gets the min/max values for a data row within a column range.
     *
     * This method is critical for efficient rendering of large datasets
     * where pixel-level compression requires knowing the min/max values
     * within a time interval.
     *
     * @param row The signal/row index
     * @param columnMin Start column index (inclusive)
     * @param columnMax End column index (inclusive)
     * @return HiLoData containing the minimum and maximum values in the range
     */
    HiLoData getHiLoValue(int row, int columnMin, int columnMax);

    /**
     * Gets a single data value at a specific row and column.
     *
     * @param row The signal/row index
     * @param column The time/column index
     * @return The data value
     */
    float getValue(int row, int column);

    /**
     * Gets the number of data rows (signals) in this container.
     *
     * @return Number of rows/signals
     */
    int getRowLength();

    /**
     * Gets the time value at a specific index for a row.
     *
     * @param index The column/time index
     * @param row The signal/row index
     * @return The time value in seconds
     */
    double getTimeValue(int index, int row);

    /**
     * Gets the maximum valid time index for a row.
     *
     * @param row The signal/row index
     * @return Maximum time index
     */
    int getMaximumTimeIndex(int row);

    /**
     * Gets data values within a time interval.
     *
     * @param intervalStart Start time in seconds
     * @param intervalStop End time in seconds
     * @param columnIndex The column/signal index
     * @return Data values within the interval (implementation-specific type)
     */
    Object getDataValueInInterval(double intervalStart, double intervalStop, int columnIndex);

    /**
     * Gets the absolute min/max values for an entire row.
     *
     * @param row The signal/row index
     * @return HiLoData containing the absolute minimum and maximum values
     */
    HiLoData getAbsoluteMinMaxValue(int row);

    /**
     * Finds the time index closest to a given time value.
     *
     * @param time The time value to search for
     * @param row The signal/row index
     * @return The index closest to the given time
     */
    int findTimeIndex(double time, int row);

    /**
     * Gets the name of a signal.
     *
     * @param row The signal/row index
     * @return The signal name
     */
    String getSignalName(int row);

    /**
     * Gets the name of the X-axis data (typically "time").
     *
     * @return The X-axis data name
     */
    String getXDataName();

    /**
     * Gets the current container status (running, paused, etc.).
     *
     * @return Current container status
     */
    ContainerStatus getContainerStatus();

    /**
     * Sets the container status.
     *
     * @param containerStatus New status to set
     */
    void setContainerStatus(ContainerStatus containerStatus);

    /**
     * Checks if a row contains invalid numbers (NaN or Infinity).
     *
     * @param row The signal/row index
     * @return true if the row contains invalid numbers
     */
    boolean isInvalidNumbers(int row);

    /**
     * Gets the time series object for a specific row.
     *
     * @param row The signal/row index
     * @return The time series for that row
     */
    AbstractTimeSerie getTimeSeries(int row);

    /**
     * Gets the underlying raw data array.
     *
     * @return The raw float data array
     */
    float[] getDataArray();

    /**
     * Gets the subcircuit signal path for a row.
     *
     * @param row The signal/row index
     * @return The subcircuit path, or empty string if not applicable
     */
    default String getSubcircuitSignalPath(int row) {
        return "";
    }
}
