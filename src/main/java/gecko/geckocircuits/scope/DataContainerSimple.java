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
public final class DataContainerSimple implements DataContainer {

    private final double[][] _data;
    /*
     * the highest index where values are written into the container
     */
    private int _maximumIndex = -1;

    public DataContainerSimple(final int rows, final int columns) {
        _data = new double[rows][columns];
    }

    @Override
    public double getValue(final int row, final int column) {
        return _data[row][column];
    }

    @Override
    public void setValue(final double value, final int row, final int column) {
        assert row < _data.length : "size: " + _data.length + " " + row;

        if (column >= _data[row].length || column < 0) {
            return;
        }

        if (row == 0) {
            _maximumIndex = Math.max(_maximumIndex, column);
        }
        if (column < _data[row].length) {
            _data[row][column] = value;
        }
    }

    public int getRowLength() {
        return _data.length;
    }

    public int getColumnLength() {
        return _data[0].length;
    }

    public void setColumn(final double[] data, final int index) {
        _data[index] = data;
    }

    public double[] getColumn(final int index) {
        return _data[index];
    }

    public double getTimeIntervalResolution() {
        return (_data[0][2] - _data[0][1]);
    }

    public HiLoData getHiLoValue(final int row, final int columnStart, final int columnStop) {
        assert columnStart <= columnStop;

        final HiLoData hiLoData = new HiLoData();

        for (int index = columnStart; index < columnStop; index++) {
            hiLoData.insertCompare((float) _data[row][index]);
        }

        return hiLoData;

    }

    public double getEstimatedTimeValue(final int column) {
        return _data[0][column];
    }

    public int getMaximumTimeIndex() {
        return _maximumIndex;
    }

    @Override
    public void insertValuesAtEnd(final double timeValue, final double[] values) {
        try {
            _maximumIndex++;
            assert values.length == _data.length - 1;
            _data[0][_maximumIndex] = timeValue;

            for (int i = 0; i < values.length; i++) {
                _data[1 + i][_maximumIndex] = values[i];
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            ex.printStackTrace();
            throw new ArrayIndexOutOfBoundsException("Array index " + _maximumIndex + " is too large for data storage!");
        }
    }
}
