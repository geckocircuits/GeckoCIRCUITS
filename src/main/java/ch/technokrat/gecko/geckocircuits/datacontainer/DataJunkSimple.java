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
package ch.technokrat.gecko.geckocircuits.datacontainer;

import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;

/**
 *
 * @author andy
 */
public final class DataJunkSimple implements DataJunk {

    private float[][] _data;
    private final int _startIndex;
    private static final int FLOAT_BYTES = 4;

    public DataJunkSimple(final int startIndex, final int rows, final int columns) {
        _data = new float[rows][columns];
        _startIndex = startIndex;
        assert false;
    }

    @Override
    public float getValue(final int row, final int index) {
        assert index - _startIndex < _data[0].length : _startIndex + " " + index;
        assert index - _startIndex >= 0;

        return _data[row][index - _startIndex];
    }

    @Override
    public void setValue(final float value, final int row, final int column) {
        _data[row][column - _startIndex] = value;
    }

    @SuppressWarnings("PMD")
    @Override
    public void setValues(final float[] values, final int column) {
        for(int row = 0; row < values.length; row++) {
            _data[row][column - _startIndex] = values[row];
        }
    }

    @Override
    public HiLoData getHiLoValue(final int row, final int columnStart, final int columnStop) {
        throw new UnsupportedOperationException("HiLo Value Not supported yet.");
    }


    public AverageValue getAverageValue(final int row, final int colStart, final int colStop) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public AverageValue getAverageValue(final int row, final int colStart,
            final int colStop, final double totalMinTime, final double totalMaxTime) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getJunkSizeInBytes() {
        return _data.length * _data[0].length * FLOAT_BYTES;
    }

    @Override
    public int getCacheSizeInBytes() {
        return getJunkSizeInBytes();
    }

    @Override
    public float getIntegralValue(int row, int index) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


}
