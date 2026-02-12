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

/**
 *
 * @author andy
 */
public final class TimeSeriesConstantDt extends AbstractTimeSerie {

    private int _maxDefinedIndex = Integer.MIN_VALUE;
    private double _dt = DEFAULT_DT;
    private double _maxDt = -Double.MAX_VALUE;
    private double _minTimeValue = Double.MAX_VALUE;
    private double _minDt = Double.MAX_VALUE;
    private int _maximumIndex = -1;
    
    private static final double MAX_DT_CHECK = 1.05;
    private static final double DEFAULT_DT = 1e-10;
    private static final int ADAPT_THRESHOLD = 100;
   
    @Override
    public void setValue(final int index, final double value) {        
        _maxDefinedIndex = Math.max(_maxDefinedIndex, index);
        if (_maxDefinedIndex == 0) {
            _minTimeValue = value;
        } else {
            assert _minTimeValue < Double.MAX_VALUE : "setting value: " + _minTimeValue;
            if(_maxDefinedIndex < ADAPT_THRESHOLD) { // adapt dt only for the first steps, otherwise the table view will show
                // varying results of the time column during the simulation!
                _dt = (value - _minTimeValue) / _maxDefinedIndex;
            }
            

            // just enshure that nobody uses this class with variable step-width in the future...
            if (index > 1) {
                _maxDt = Math.max(_maxDt, _dt);
                _minDt = Math.min(_minDt, _dt);
                assert _dt * MAX_DT_CHECK > _maxDt : _dt + " " + _maxDt + " " + index + " " + value;
                assert _dt / MAX_DT_CHECK < _minDt : _dt + " " + MAX_DT_CHECK + " " + _minDt;
            }
        }
        _maximumIndex = Math.max(index, _maximumIndex);
    }

    @Override
    public double getValue(final int index) {
        assert index >= 0 : index;
        if (index <= _maxDefinedIndex) {            
            return _minTimeValue + index * _dt;
        } else { // this is nonsense, but Uwe's scope needs this probably! Improve later...
            return 0;
        }
    }

    @Override
    public int getMaximumIndex() {
        return _maximumIndex;        
    }        

    @Override
    public int findTimeIndex(final double time) {        
        final int returnValue = (int) ((time - _minTimeValue) / _dt);
        return Math.max(0, Math.min(returnValue, _maximumIndex));
    }

    @Override
    public double getLastTimeInterval() {
        return _dt;
    }
    
}
