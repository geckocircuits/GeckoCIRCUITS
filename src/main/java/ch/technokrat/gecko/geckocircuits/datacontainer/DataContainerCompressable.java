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

import ch.technokrat.gecko.geckocircuits.newscope.AbstractTimeSerie;
import ch.technokrat.gecko.geckocircuits.newscope.DefinedMeanSignals;
import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import ch.technokrat.gecko.geckocircuits.newscope.MemoryContainer;
import ch.technokrat.gecko.geckocircuits.newscope.ScopeSignalMean;
import ch.technokrat.gecko.geckocircuits.newscope.TimeSeriesConstantDt;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Compresses the stored data with a effective difference compression algorithm
 * & zip compression
 *
 * @author andy
 */
public final class DataContainerCompressable extends AbstractDataContainer implements DataContainerValuesSettable,
        DataContainerIntegralCalculatable {

    private final List<DataJunkCompressable> _data = new ArrayList<DataJunkCompressable>();
    public static final int JUNK_SIZE = 4096;
    private final int _rows;
    private int _totalDataSize = 0;
    private final AbstractTimeSerie _timeSerie;
    private final HiLoData[] _totMinMaxValues;
    private int _lastMinMaxJunk = 0;
    private static final int MEGA_BYTES = 1048000;
    private final String[] _signalNames;
    private final String[] _signalPathNames;

    private final String _xDataName;
    private ContainerStatus _containerStatus = ContainerStatus.NOT_INITIALIZED;
    private final boolean[] _containsInvalidNumbers;
    private double[] _integralValues;
    private int[] _avgIndicesArray = new int[0];
    private final Set<ScopeSignalMean> _definedAverageSignals = new HashSet<ScopeSignalMean>();
    private MemoryContainer _memoryContainer;

    public DataContainerCompressable(final int rows,
            final AbstractTimeSerie timeSerie, final String[] signalNames, final String xDataName) {
        super();

        _totMinMaxValues = new HiLoData[rows];
        _rows = rows;
        _timeSerie = timeSerie;
        assert signalNames.length == rows;
        _signalNames = new String[signalNames.length];

        _signalPathNames = new String[signalNames.length];
        for (int i = 0; i < _signalPathNames.length; i++) {
            _signalPathNames[i] = "";
        }

        System.arraycopy(signalNames, 0, _signalNames, 0, _signalNames.length);
        _xDataName = xDataName;
        setContainerStatus(ContainerStatus.RUNNING);
        _memoryContainer = MemoryContainer.getMemoryContainer(rows);
        _containsInvalidNumbers = new boolean[rows];
        _integralValues = new double[rows];
    }

    @Override
    public float getValue(final int row, final int column) {
        assert column <= getMaximumTimeIndex(0);
        final DataJunk junk = _data.get(column / JUNK_SIZE);
        return junk.getValue(row, column);
    }

    private float getIntegralValue(final int row, final int startIndex, final int stopIndex) {
        final DataJunk startJunk = _data.get(startIndex / JUNK_SIZE);
        final DataJunk stopJunk = _data.get(stopIndex / JUNK_SIZE);
        return stopJunk.getIntegralValue(row, stopIndex) - startJunk.getIntegralValue(row, startIndex);
    }

    public void setValue(final float value, final int row, final int column) {
        checkContainerSize(column);

        final DataJunk junk = _data.get(column / JUNK_SIZE);

        //assert column / JUNK_SIZE == _data.size() - 1 : column + " " + column / JUNK_SIZE + " " + _data.size();
        junk.setValue(value, row, column);
    }

    @Override
    public boolean isInvalidNumbers(final int row) {
        try { // first enshure that all data ranges are read:
            getAbsoluteMinMaxValue(row);
        } catch (ArithmeticException ex) {
            Logger.getLogger(DataContainerCompressable.class.getName()).log(Level.WARNING, ex.getMessage());
        }
        // then return the invalid number result!
        return _containsInvalidNumbers[row];
    }

    private void checkContainerSize(final int column) {
        if (column >= _totalDataSize) {
            if (!_data.isEmpty() && _data.get(0) instanceof DataJunkCompressable) {
                ((DataJunkCompressable) (_data.get(_data.size() - 1))).doCompression();
            }

            _data.add(new DataJunkCompressable(_memoryContainer, _totalDataSize, _rows, JUNK_SIZE, _timeSerie));
            _totalDataSize = _data.size() * JUNK_SIZE;
            setChanged();
            notifyObservers();
        }

    }

    @Override
    public int getRowLength() {
        return _rows;
    }

    @Override
    public double getTimeValue(final int column, final int row) {
        return _timeSerie.getValue(column);
    }

    @Override
    public HiLoData getHiLoValue(final int row, final int columnMin,
            final int columnMax) {

        assert columnMin <= columnMax : columnMin + " " + columnMax;

        final ArrayList<HiLoData> mergeList = new ArrayList<HiLoData>();

        for (int junkIndex = columnMin / JUNK_SIZE; junkIndex <= columnMax / JUNK_SIZE; junkIndex++) {
            final DataJunk junk = _data.get(junkIndex);
            final HiLoData compareValue = junk.getHiLoValue(row, columnMin, columnMax);
            if (compareValue != null) {
                mergeList.add(compareValue);
            }

        }

        assert !mergeList.isEmpty();
        return HiLoData.getMergedFromList(mergeList);
    }

    @Override
    public int getMaximumTimeIndex(final int row) {
        return _timeSerie.getMaximumIndex();
    }
    double absoluteMaxValue = -1000000;

    @Override
    public void insertValuesAtEnd(final float[] values, final double timeValue) {

        final int column = _timeSerie.getMaximumIndex() + 1;
        checkContainerSize(column);
        _data.get(column / JUNK_SIZE).setValues(values, column);

        if (_avgIndicesArray.length > 0) {
            double timeInterval = 0;
            final int maxIndex = _timeSerie.getMaximumIndex();
            if (maxIndex > 0) {
                timeInterval = timeValue - _timeSerie.getValue(maxIndex);
            }

            //  for(int row = 0; row < values.length; row++) {        
            for (int row : _avgIndicesArray) {
                _integralValues[row] += values[row] * timeInterval;
                _data.get(column / JUNK_SIZE).setIntegralValue(_integralValues[row], row, column);
            }
        }
        _timeSerie.setValue(column, timeValue);
    }

    @Override
    public HiLoData getAbsoluteMinMaxValue(final int row) {
        final int oldLastInclJunk = _lastMinMaxJunk;
        final int maximumTimeIndex = getMaximumTimeIndex(row);
        _lastMinMaxJunk = maximumTimeIndex / JUNK_SIZE;

        if (_lastMinMaxJunk > oldLastInclJunk) {
            calculateMinMax(oldLastInclJunk, _lastMinMaxJunk, maximumTimeIndex);
        }

        if (_totMinMaxValues[row] == null) {
            throw new ArithmeticException("No valid data available!");
        }

        return _totMinMaxValues[row];
    }

    @Override
    public int findTimeIndex(final double time, final int row) {
        return _timeSerie.findTimeIndex(time);
    }

    @Override
    public int getUsedRAMSizeInMB() {
        int totalBytes = 0;
        final List<DataJunk> allJunks = new ArrayList<DataJunk>();
        allJunks.addAll(_data); // this is done to avoid a concurrent-modification-exception
        for (DataJunk junk : allJunks) {
            final int junkBytes = junk.getJunkSizeInBytes();
            totalBytes += junkBytes;
        }
        int returnValue = totalBytes / MEGA_BYTES;
        returnValue += getCachedRAMSizeInMB();
        return returnValue;
    }

    @Override
    public long getCachedRAMSizeInMB() {
        long returnValue = 0;
        for (DataJunk junk : _data) {
            returnValue += junk.getCacheSizeInBytes();
        }
        returnValue /= MEGA_BYTES;

        return returnValue;
    }

    @Override
    public Object getDataValueInInterval(final double intervalStart, final double intervalStop, final int columnIndex) {

        final int startIndex = _timeSerie.findTimeIndex(intervalStart);
        final int stopIndex = _timeSerie.findTimeIndex(intervalStop);

        if (startIndex == 0 && stopIndex == 0) {
            // to get the first datapoint drawn properls:
            double firstTimeValue = _timeSerie.getValue(0);
            if (intervalStart <= firstTimeValue && intervalStop >= firstTimeValue) {
                return getValue(columnIndex, 0);
            }
        }

        if (startIndex == stopIndex) { // in this case, there was no data point in the given interval.   
            return null;
        }
        if (startIndex + 1 == stopIndex) { // we have exactly one datapoint in the interval.
            final double timeValue = _timeSerie.getValue(stopIndex);
            if (timeValue > intervalStop || timeValue < intervalStart) {
                return null;
            } else {
                return getValue(columnIndex, startIndex + 1);
            }
        }

        return getHiLoValue(columnIndex, startIndex + 1, stopIndex);

    }

    @Override
    public float getAVGValueInInterval(final double intervalStart, final double intervalStop, final int columnIndex) {
        final int startIndex = _timeSerie.findTimeIndex(intervalStart);
        final int stopIndex = _timeSerie.findTimeIndex(intervalStop);
        final float integralValue = getIntegralValue(columnIndex, startIndex, stopIndex);
        final double timeInterval = _timeSerie.getValue(stopIndex) - _timeSerie.getValue(startIndex);
        return (float) (integralValue / timeInterval);
    }

    @Override
    public String getSignalName(final int row) {
        return _signalNames[row];
    }

    @Override
    public String getXDataName() {
        return _xDataName;
    }

    @Override
    public ContainerStatus getContainerStatus() {
        return _containerStatus;
    }

    @Override
    public void setContainerStatus(final ContainerStatus containerStatus) {
        _containerStatus = containerStatus;
        if (_containerStatus == ContainerStatus.RUNNING) {
            _memoryContainer = MemoryContainer.getMemoryContainer(_rows);
        }
        try {
            if (containerStatus == ContainerStatus.PAUSED) {
                int maxIndex = getMaximumTimeIndex(0);
                calculateMinMax(_lastMinMaxJunk, _lastMinMaxJunk + 1, getMaximumTimeIndex(0));
                _memoryContainer = null;
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        setChanged();
        notifyObservers();

    }

    @Override
    public AbstractTimeSerie getTimeSeries(final int row) {
        return _timeSerie;
    }

    @Override
    public void defineAvgCalculation(final List<ScopeSignalMean> meanSignals) {
        _definedAverageSignals.addAll(meanSignals);
        _avgIndicesArray = new int[_definedAverageSignals.size()];
        int index = 0;
        for (ScopeSignalMean signal : _definedAverageSignals) {
            _avgIndicesArray[index] = signal.getConnectedScopeInputIndex();
            index++;
        }
    }

    @Override
    public DefinedMeanSignals getDefinedMeanSignals() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    private static final HiLoData NAN_HI_LO = HiLoData.hiLoDataFabric(Float.NaN, Float.NaN);

    private void calculateMinMax(int fromJunk, int toJunk, int maximumTimeIndex) {
        for (int calculateRow = 0; calculateRow < _rows; calculateRow++) {
            final List<HiLoData> mergeList = new ArrayList<HiLoData>();
            mergeList.add(NAN_HI_LO);

            if (_totMinMaxValues[calculateRow] != null) { // if value was already calculated, insert this value to mergelist
                mergeList.add(_totMinMaxValues[calculateRow]);
            }

            for (int i = fromJunk; i < toJunk; i++) {
                final DataJunk junk = _data.get(i);
                final HiLoData compare = junk.getHiLoValue(calculateRow, 0, maximumTimeIndex);
                if (compare != null) {
                    mergeList.add(compare);
                    boolean isValid = compare.isValidNumber();
                    if (!isValid) {
                        _containsInvalidNumbers[calculateRow] = false;
                    }
                }
            }

            _totMinMaxValues[calculateRow] = HiLoData.getMergedFromList(mergeList);

        }
    }

    @Override
    public float[] getDataArray() {
        return _memoryContainer.getArray();
    }

    public MemoryContainer getMemoryContainer() {
        return _memoryContainer;
    }

    @Override
    public String getSubcircuitSignalPath(final int row) {
        return _signalPathNames[row];
    }

    ;

    @Override
    void setSignalPathName(int row, String subcircuitPath) {        
        _signalPathNames[row] = subcircuitPath;
    }

}
