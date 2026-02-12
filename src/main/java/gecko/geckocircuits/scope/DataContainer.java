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

package gecko.geckocircuits.scope;

/**
 * This is a deprecated class, in future, replace with datacontainer from "newscope" package
 * @author andy
 */
@Deprecated
public interface DataContainer {

    double getValue(final int row, final int column);
    HiLoData getHiLoValue(final int row, final int column, final int columnOld);

    void setValue(final double value, final int row, final int column);

    int getRowLength();

    int getColumnLength();

    void setColumn(final double[] data, final int index);

    double[] getColumn(final int index);

    double getTimeIntervalResolution();

    double getEstimatedTimeValue(final int index);
    int getMaximumTimeIndex();
   
    /*
     * add another row of data points at the end of the container
     */
    void insertValuesAtEnd(final double timeValue, final double[] values);
    
}
