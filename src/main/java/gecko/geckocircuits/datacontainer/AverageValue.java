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
package gecko.geckocircuits.datacontainer;

public final class AverageValue {
    private final double _intervalStart;
    private double _intervalStop;
    private double _avgValue;
    
    AverageValue(final AverageValue copy) {
        assert copy != null;
        _avgValue = copy._avgValue;
        _intervalStart = copy._intervalStart;
        _intervalStop = copy._intervalStop;
    }
    
    public double getIntervalStart() {
        return _intervalStart;
    }
    
    public double getIntervalStop() {
        return _intervalStop;
    }
    
    AverageValue(final double avgValue, final double intervalStart, final double intervalStop) {
        _intervalStart = intervalStart;
        _intervalStop = intervalStop;
        _avgValue = avgValue;
    }
    
    public void appendAverage(final AverageValue append) {
        if(append == null) {
            return;
        }
        // do this in double precision
        final double newInterval = getAverageSpan() + append.getAverageSpan();
        double newValue = (_avgValue * getAverageSpan() + append.getAverageSpan() * append._avgValue); 
        newValue /=  newInterval;
        _avgValue = newValue;
        
        // this helps for debugging: the intervals should be continuous!
        //assert append._intervalStart == _intervalStop : "this start stop: "  + _intervalStart + " " + _intervalStop 
        //        + " other start: " + append._intervalStart + " " + append._intervalStop;
        _intervalStop = append._intervalStop;
        
    }

    public double getAverageValue() {
        return _avgValue;
    }
    
    public double getAverageSpan() {
        return _intervalStop - _intervalStart;
    }

    @Override
    public String toString() {
        return _avgValue + " " + _intervalStart + " " + _intervalStop;
    }
    
    
    
}
