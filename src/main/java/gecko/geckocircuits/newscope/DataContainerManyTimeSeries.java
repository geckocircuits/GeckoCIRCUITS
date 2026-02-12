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

import gecko.geckocircuits.datacontainer.AbstractDataContainer;
import gecko.geckocircuits.datacontainer.ContainerStatus;
import gecko.geckocircuits.datacontainer.DataContainerValuesSettable;

/**
 *
 * @author andy
 */
public class DataContainerManyTimeSeries extends AbstractDataContainer implements DataContainerValuesSettable {

    // CHECKSTYLE:OFF
    protected final float[][] _data;
    // CHECKSTYLE:ON
    private final TimeSeriesArray[] _timeSerieArray;
    private String[] _signalNames;
    /*
     * the highest index where values are written into the container
     */
    private final HiLoData[] _abMinMaxValues;
    private ContainerStatus _containerStatus;

    public DataContainerManyTimeSeries(final int rows, final int maximumColumns) {
        _data = new float[rows][maximumColumns];
        _abMinMaxValues = new HiLoData[rows];
        _timeSerieArray = new TimeSeriesArray[rows];
        for (int i = 0; i < rows; i++) {
            _timeSerieArray[i] = new TimeSeriesArray(maximumColumns);
        }
        _signalNames = new String[rows];
    }

    @Override
    public final float getValue(final int row, final int column) {
        return _data[row][column];
    }

    public final void setValue(final float value, final int row, final int column) {
        assert row < _data.length : "size: " + _data.length + " " + row;
        assert row > 0;
        _abMinMaxValues[row] = HiLoData.mergeFromValue(_abMinMaxValues[row], value);
        if (column >= _data[row].length || column < 0) {
            return;
        }

        if (column < _data[row].length) {
            _data[row][column] = value;
        }
    }

    @Override
    public final int getRowLength() {
        return _data.length;
    }

    @Override
    public final HiLoData getHiLoValue(final int row, final int columnStart, final int columnStop) {
        assert columnStart <= columnStop;

        HiLoData hiLoData = null;

        for (int index = columnStart; index < columnStop; index++) {
            hiLoData = HiLoData.mergeFromValue(hiLoData, (float) _data[row][index]);
        }

        return hiLoData;

    }

    @Override
    public final double getTimeValue(final int column, final int row) {
        return _timeSerieArray[row].getValue(column);
//        return _timeValues[column];
    }

    @Override
    public final int getMaximumTimeIndex(final int row) {        
        return _timeSerieArray[row].getMaximumIndex();                
    }

    @SuppressWarnings("PMD")
    @Override
    public void insertValuesAtEnd(final float[] values, final double timeValue) {        

        for (int j = 0; j < _timeSerieArray.length; j++) {
            int index = _timeSerieArray[j].getMaximumIndex()+1;
            _timeSerieArray[j].setValue(index, timeValue);
            for (int i = 0; i < values.length; i++) {
                _data[i][index] = values[i];
                _abMinMaxValues[i] = HiLoData.mergeFromValue(_abMinMaxValues[i], values[i]);
            }
        }


    }

    @SuppressWarnings("PMD")
    public void insertValueAtEnd(final float value, final double timeValue, final int row) {
        TimeSeriesArray timeSerie = _timeSerieArray[row];
        int index = timeSerie.getMaximumIndex() + 1;
        timeSerie.setValue(index, timeValue);
        _data[row][index] = value;
        _abMinMaxValues[row] = HiLoData.mergeFromValue(_abMinMaxValues[row], value);

    }

    @Override
    public final HiLoData getAbsoluteMinMaxValue(final int row) {
        try {
            HiLoData returnValue = _abMinMaxValues[row];
            return returnValue;
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println("ArrayIndexOutOfBoundsException!! At index : " + row + ", _abMinMaxValues = " + _abMinMaxValues.length);
        }
        return _abMinMaxValues[0];
    }
    static int clearCounter = 0;

    @Override
    public final int findTimeIndex(final double time, final int row) {
        int returnValue = 0;

        for (int i = 0; i < this.getMaximumTimeIndex(row); i++) {
            if (_timeSerieArray[row].getValue(i) < time) {
                returnValue++;
            } else {
                return returnValue;
            }
        }
        return returnValue;
    }

    @Override
    public final int getUsedRAMSizeInMB() {
        assert false;
        return 0;
    }

    @Override
    public final long getCachedRAMSizeInMB() {
        return getUsedRAMSizeInMB();
    }

    @Override
    public Object getDataValueInInterval(double intervalStart, double intervalStop, int rowIndex) {        
//        int index = _timeSerieArray.findTimeIndex(intervalStop, _maximumIndex);
//        return getValue(0, index);
//        System.out.println("Interval Start:" + intervalStart + "\tIntervallStop: " + intervalStop + "\t columnIndex: " + columnIndex);
        final int startIndex = _timeSerieArray[rowIndex].findTimeIndex(intervalStart);
        final int stopIndex = _timeSerieArray[rowIndex].findTimeIndex(intervalStop);

        if (startIndex == 0 && stopIndex == 0) {
            // to get the first datapoint drawn properly:
            double firstTimeValue = _timeSerieArray[rowIndex].getValue(0);
            if (intervalStart <= firstTimeValue && intervalStop >= firstTimeValue) {
                return getValue(rowIndex, 0);
            }
        }

        if (startIndex == stopIndex) { // in this case, there was no data point in the given interval.   
            return null;
        }
        if (startIndex + 1 == stopIndex) { // we have exactly one datapoint in the interval.
            final double timeValue = _timeSerieArray[rowIndex].getValue(stopIndex);
            if (timeValue > intervalStop || timeValue < intervalStart) {
                return null;
            } else {
                return getValue(rowIndex, startIndex + 1);
            }
        }

        return getHiLoValue(rowIndex, startIndex + 1, stopIndex);


    }

    @Override
    public String getSignalName(final int row) {
        if (_signalNames.length <= row) {
            return "";
        }
        if (_signalNames[row] != null) {
            return _signalNames[row];
        }
        return "";
    }

    public final void setSignalName(final String name, final int row) {
        _signalNames[row] = name;
    }

    @Override
    public String getXDataName() {
        return "Sample";
    }

    @Override
    public ContainerStatus getContainerStatus() {
        return _containerStatus;
    }

    @Override
    public void setContainerStatus(ContainerStatus containerStatus) {
        _containerStatus = containerStatus;
    }

    @Override
    public boolean isInvalidNumbers(int row) {
        return true;
    }

    @Override
    public AbstractTimeSerie getTimeSeries(final int row) {
        return _timeSerieArray[row];
    }

    public double getNiceMaximumXValue() {
        double maxXValue = 0;
        for(AbstractTimeSerie ts : _timeSerieArray) {
            maxXValue = Math.max(maxXValue, ts.getValue(ts.getMaximumIndex()));
        }
        
        NiceScale niceScale = new NiceScale(HiLoData.hiLoDataFabric(0, (float) maxXValue));
        return niceScale.getNiceLimits()._yHi;
    }

    @Override
    public float[] getDataArray() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
