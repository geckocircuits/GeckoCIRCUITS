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
 * A DataContainerValuesSettable is a data container where values can be inserted,
 * in contradiction to any of the wrappers which only provide read access.
 *
 * GUI-free version for use in headless simulation core.
 */
public interface DataContainerValuesSettable {

    /**
     * Add another row of data points at the end of the container.
     *
     * @param values the values to insert (one per signal row)
     * @param timeValue the time value for this data point
     */
    void insertValuesAtEnd(final float[] values, final double timeValue);

    /**
     * Gets the current used RAM size for this container.
     *
     * @return size in megabytes
     */
    int getUsedRAMSizeInMB();

    /**
     * Gets the cached RAM size for this container.
     *
     * @return size in megabytes
     */
    long getCachedRAMSizeInMB();
}
