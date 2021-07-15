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

package ch.technokrat.gecko.geckocircuits.newscope;

/**
 *
 * @author andy
 */
public final class TimeSeriesArray extends AbstractTimeSerie {
    private double[] _timeSeries;
    private int _maximumIndex = -1;
    
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
    
    static int counter = 0;
    
    @Override
    public int findTimeIndex(final double time) {
        counter++;
        //assert counter < 20000;
        final int maxIndex = _maximumIndex;
        final double maxTime = getValue(maxIndex);
        final double minTime = getValue(0);

        int estimatedIndex = (int) (maxIndex * (time - minTime) / (maxTime - minTime));
        int firstestimation = estimatedIndex;
        if (estimatedIndex > maxIndex) {
            estimatedIndex = maxIndex;
        }
        if (estimatedIndex < 0) {
            estimatedIndex = 0;
        }
        int whilecounter = 0;
        while (getValue(estimatedIndex) > time) {
            whilecounter++;
            estimatedIndex -= FIND_OVER_STEP;
            if (estimatedIndex < 0) {
                estimatedIndex = 0;
                break;
            }
        }

        int returnValue = estimatedIndex;
        for (int i = estimatedIndex; i < maxIndex; i++) {
            whilecounter ++;
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
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
