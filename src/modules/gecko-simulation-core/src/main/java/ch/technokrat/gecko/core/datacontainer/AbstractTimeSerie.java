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
 * A time series is a container which holds the actual simulation time steps.
 * It could be saved in an array, or more easily for constant time steps
 * in a calculation routine with start and stop time and number of steps in between.
 *
 * GUI-free version for use in headless simulation core.
 */
public abstract class AbstractTimeSerie {

    protected static final int FIND_OVER_STEP = 5;

    /**
     * Gets the time value at the specified index.
     *
     * @param index the index to retrieve
     * @return the time value at that index
     */
    public abstract double getValue(final int index);

    /**
     * Sets the time value at the specified index.
     *
     * @param index the index to set
     * @param value the time value to store
     */
    public abstract void setValue(final int index, final double value);

    /**
     * Gets the biggest index for which data is already written into container.
     *
     * @return maximum valid index
     */
    public abstract int getMaximumIndex();

    /**
     * Finds the worksheet index of the given time value.
     *
     * @param time value to search the index for
     * @return worksheet index of the given time value
     */
    public abstract int findTimeIndex(final double time);

    /**
     * Gets the difference between the last two inserted time values.
     *
     * @return the last time interval (dt)
     */
    public abstract double getLastTimeInterval();
}
