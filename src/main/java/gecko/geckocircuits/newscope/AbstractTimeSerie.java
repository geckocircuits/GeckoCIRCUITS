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

package gecko.geckocircuits.newscope;

/* A time series is just a container which holds the actual
 * simulation time steps. It could be saved in an array, or
 * more easily for constant time steps in a calculation routine
 * with start and stop time and number of steps in between.
 */
public abstract class AbstractTimeSerie {
    protected static final int FIND_OVER_STEP = 5;
    public abstract double getValue(final int index);
    public abstract void setValue(final int index, final double value);
    /**
     * 
     * @return bigest index, for which data is already written into container
     */
    public abstract int getMaximumIndex();
    
    /**
     * 
     * @param time value to search the index for
     * @param maxIndex maximum index for which the worksheet data is valid
     * @return worksheet index of the given time value
     */
    public abstract int findTimeIndex(final double time);

    /**
     * 
     * @return the difference between the last two inserted time values.
     */
    public abstract double getLastTimeInterval();
    
}
