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
 * @author xtibi
 */
public class DataBlock {

    private final double _startValue;
    private double _dt;
    private int _size;
    private final int _startIndex;
    static final double TOLERANCE = 1e-10;

    DataBlock(final double startValue, final double dtime, final int size, final int startIndex) {
        _startValue = startValue;
        _dt = dtime;
        _size = size;
        _startIndex = startIndex;
    }

    public final int getSize() {
        return _size;
    }

    public final double getBlockValue(final int index) {
        final int blockIndex = index - _startIndex;
        
        assert 0 <= blockIndex && blockIndex < _size;
        return _startValue + blockIndex * _dt;
    }

    public final boolean setBlockValue(final double value) {
        assert value > _startValue + (_size - 1) * _dt;

        if (checkValueFit(value)) {
            _dt = (value - _startValue) / _size;
            _size++;
            return true;
        }
        return false;
    }

    /**
     * A value fits in a block either if the block has one element, or value =
     * start + n*dt, with a tolerance respective to dt
     *
     * @param value
     * @return true if it fits, false otherwise
     */
    private boolean checkValueFit(final double value) {
        return _size == 1 || (_startValue + _size * (1 - TOLERANCE) * _dt <= value
                && value <= _startValue + _size * (1 + TOLERANCE) * _dt);
    }
    
    public final int findTimeIndex(final double time) {
        final int blockIndex = (int) ((time - _startValue)/_dt);
        return _startIndex + blockIndex;
    }

    /**
     * Class containing the start and end indices of the block
     */
    public static class IndexLimit implements Comparable<IndexLimit> {

        private final int _startIndex;
        private final int _endIndex;

        public IndexLimit(final int startIndex, final int endIndex) {
            _startIndex = startIndex;
            _endIndex = endIndex;
        }

        @Override
        public final int compareTo(final IndexLimit ilim) {
            if (this._startIndex < ilim._startIndex && this._endIndex < ilim._endIndex) {
                return 1;
            }
            if (this._startIndex > ilim._startIndex && this._endIndex > ilim._endIndex) {
                return -1;
            }
            return 0;
        }
    }

    /**
     * Class containing start and end values of the block
     */
    public static class TimeLimit implements Comparable<TimeLimit> {

        private final double _startTime;
        private final double _endTime;

        public TimeLimit(final double startTime, final double endTime) {
            _startTime = startTime;
            _endTime = endTime;
        }

        @Override
        public final int compareTo(final TimeLimit tlim) {
            if (this._startTime < tlim._startTime && this._endTime < tlim._endTime) {
                return 1;
            }
            if (this._startTime > tlim._startTime && this._endTime > tlim._endTime) {
                return -1;
            }
            return 0;
        }
    }
    
    public final IndexLimit getIndexLimit() {
        return new IndexLimit(_startIndex, _startIndex + _size - 1);
    }
    
    public final double getStartValue() {
        return _startValue;
    }
}
