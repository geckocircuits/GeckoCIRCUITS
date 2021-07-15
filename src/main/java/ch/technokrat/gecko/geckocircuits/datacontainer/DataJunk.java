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
/**
 * a data junk is a block of data, that can be used within a DataContainer.
 * It is useful to split the whole data into smaller junks, since then
 * we can grow the Data size dynamically, or we can compress the single
 * data junks.
 */

package ch.technokrat.gecko.geckocircuits.datacontainer;

import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;

/**
 *
 * @author andy
 */
public interface DataJunk {

    /**
     * 
     * @param value actual value to write into the container
     * @param row index to data row
     * @param column index to data column
     */
    void setValue(float value, int row, int column);
    
    /**
     * 
     * @param values actual value to write into the container
     * @param column index to data column
     */
    void setValues(final float[] values, final int column);
    
    /**
     * 
     * @param row
     * @param column
     * @return
     */
    float getValue(int row, int column);

    
    /**
     * 
     * @param row
     * @param columnStart
     * @param columnStop
     * @return
     */
    HiLoData getHiLoValue(int row, int columnStart, int columnStop);
        
    
    AverageValue getAverageValue(final int row, final int colStart, final int colStop, 
            final double totalMinTime, final double totalMaxTime);
    
    
    int getJunkSizeInBytes();
    int getCacheSizeInBytes();
    
    float getIntegralValue(final int row, final int index);
}
