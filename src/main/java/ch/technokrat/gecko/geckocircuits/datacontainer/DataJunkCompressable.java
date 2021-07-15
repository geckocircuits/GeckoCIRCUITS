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

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.newscope.AbstractTimeSerie;
import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import ch.technokrat.gecko.geckocircuits.newscope.MemoryContainer;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

/**
 *
 * @author andy Compresses the data employing a zip-compression algorithm. In
 * order to achive a higher compression ratio, the difference compression
 * algorithm of order 2 (MDIFFORDER) is used, see therefore the paper: "Lossless
 * Compression of High-volume Numerical Data from Simulations, Engelson,
 * Fritzson, Fritzson... For conveniece, the float data is directly stored
 * inside a int array, therefore use the Float.floatBitsToInt and
 * Float.intBitsToFloat to access the data values. Furhermore, the double values
 * are rounded to less bits than float.
 */
public final class DataJunkCompressable implements DataJunk {

    public static void setMemoryPrecision() {
        final String lossyCompression = GeckoSim.applicationProps.getProperty("LOSSY_COMPRESSION");

        if (lossyCompression == null) { // this is just for backward-compatibility (1.54-> 1.60, maybe remove in the future!
            if (GeckoSim.applicationProps.getProperty("MEMORY_COMPRESS").equals("TRUE")) {
                precisionField = PRECISIONS[2];
            } else {
                precisionField = PRECISIONS[0];
            }
        } else {
            final int lossValue = Integer.parseInt(lossyCompression);
            precisionField = PRECISIONS[lossValue];
        }
    }

    public static void setPrecisionField(final int newPrecision) {
        precisionField = newPrecision;
    }
    private float[][] _data;
    private float[][] _avgData;
    private CompressorIntMatrix _compressor;
    private SoftReference<float[][]> _dataSoftRef;
    private final int _startIndex;
    // divide this data junk in HI_LOW_N pieces, and pre-calculate the
    // high/low data values
    private static final byte VALUE_CACHE_SIZE = 32;
    private HiLoData[][] _hiLoData;
    private AverageValue[][] _localAvgData;
    private boolean _avgCalculationOK = false;
    private final int _columns;
//    private static int _compCounter = 1;
//    private static double _compSum = 0;
    private static final int MORDER_DIFF = 2;
    private final AbstractTimeSerie _timeSerie;
    private final int _rows;
    private int _memInBytes = 0;
    /**
     * this bit-field has 1111's at the beginning, whereas the last x bits are
     * zero. And-Connection with floatIntBits will round the float value. e.g. :
     * 2 * Integer.MAX_VALUE + 1 - (1 + 2 + 4 + 8 + 16 + 32 + 64 + 128 + 256 //+
     * 512 //+ 1024 + 2048
     */
    private static final int DOUBLE_BYTES = 8;
    public static final int[] PRECISIONS = new int[]{-1, -128, -512, -2048};
    private static int precisionField = PRECISIONS[2];
    private final MemoryContainer _container;

    public DataJunkCompressable(final MemoryContainer container, final int startIndex, final int rows, final int columns,
            final AbstractTimeSerie timeSeries) {
        // the data array is not completely initialized. This will happen later,
        // when data is inserted
        _container = container;
        _data = new float[columns][];
        _avgData = new float[rows][];
        _columns = columns;
        _dataSoftRef = new SoftReference<float[][]>(_data);
        _startIndex = startIndex;
        _timeSerie = timeSeries;
        _rows = (int) rows;
    }    

    @Override
    public float getValue(final int row, final int index) {
        //assert index - _startIndex < _data[0].length : _startIndex + " " + index;
        assert index - _startIndex >= 0;

        synchronized (this) {
            final float[][] tmpData = _data;
            if (tmpData == null) {


                float[][] hardSoftLink = null;
                if (_dataSoftRef != null) {
                    hardSoftLink = _dataSoftRef.get();
                }

                if (hardSoftLink == null) {
                    hardSoftLink = deCompress();
                }
                _dataSoftRef = new SoftReference<float[][]>(hardSoftLink);
                return hardSoftLink[index - _startIndex][row];

            } else {
                assert tmpData[index - _startIndex] != null : index - _startIndex;
                return tmpData[index - _startIndex][row];
            }
        }
    }

    public float getIntegralValue(final int row, final int index) {
        try {
            return _avgData[row][index - _startIndex];
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    @Override
    public void setValue(final float value, final int row, final int column) {
        final int columnIndex = column - _startIndex;
        if (_data[columnIndex] == null) {
            _data[column] = new float[_rows];
        }
        _data[columnIndex][row] = value;

    }

    @Override
    public void setValues(final float[] values, final int column) {
        _data[column - _startIndex] = values;
    }

    void setIntegralValue(final double integralValue, final int row, final int column) {
        if (_avgData[row] == null) {
            _avgData[row] = new float[_columns];
        }
        _avgData[row][column - _startIndex] = (float) integralValue;
    }

    /**
     * tells the data-junk to compress its cached data. This is performed within
     * a new Thread, so that the simulation is not slowed down.
     */
    public void doCompression() {
        final Thread thread = new Thread(new CompressThread(this));
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    /**
     * get back the original data by de-compressing the byte array
     *
     * @return
     */
    private float[][] deCompress() {
        final int[][] returnValue = _compressor.deCompress();
        calculateDifferenceDeCompression(returnValue);
        float[][] floatArray = new float[returnValue[0].length][returnValue.length];

        // careful: dimensions are flipped here!
        for (int i = 0; i < returnValue.length; i++) {
            for (int j = 0; j < returnValue[0].length; j++) {
                floatArray[j][i] = Float.intBitsToFloat(returnValue[i][j]);
            }
        }

        return floatArray;
    }

    private static float roundFloat(final float value) {
        final int floatIntBits = Float.floatToIntBits(value);
        final int output = floatIntBits & precisionField;
        // this is for testing - don't remove. Bitstring should start with 1111, and have trailing zeros at the end.
        //System.out.println(Integer.toBinaryString(bitField));
        return Float.intBitsToFloat(output);
    }

    /**
     *
     * @param row select the data row
     * @param columnStart start index (global indices)
     * @param columnStop stop index (global indices)
     * @return the min-max value in the given row, between the columnStart and
     * columnStop-Index (global indices)
     */
    @Override
    public HiLoData getHiLoValue(final int row, final int columnStart, final int columnStop) {
        assert columnStart <= columnStop;
        // when interval is smaller than buffered HiLow-Data intervall, return
        // data that is really calculated!

        final int resolutionDifference = _columns / VALUE_CACHE_SIZE;
//        if(allowApprox) {
//            resolutionDifference /= 2;
//        }

        if ((columnStop - columnStart) < resolutionDifference) {
            return calculateRealHiLowData(row, columnStart, columnStop);
        }

        // be careful: deadlock or race conditions possible!
        synchronized (this) {
            if (_hiLoData == null) {
                return calculateRealHiLowData(row, columnStart, columnStop);
            }
        }

        final List<HiLoData> mergeList = new ArrayList<HiLoData>();
        final int stopIndex = Math.min(VALUE_CACHE_SIZE, findCachedIndex(columnStop));
        final int startIndex = findCachedIndex(columnStart);
        for (int j = startIndex; j < stopIndex && j < VALUE_CACHE_SIZE; j++) {
            final HiLoData hiLow = _hiLoData[row][j];

            if (hiLow != null) {
                mergeList.add(hiLow);
            }
        }

        if (mergeList.isEmpty()) {
            return null;
        } else {
            return HiLoData.getMergedFromList(mergeList);
        }

    }
            
    @Override
    public AverageValue getAverageValue(final int row, final int columnStart, final int columnStop,
            final double totalMinTime, final double totalMaxTime) {

        synchronized (this) {
            if (_localAvgData == null || _localAvgData[row][0] == null || !_avgCalculationOK) {
                calculateAvgData(row);
            }
        }


        AverageValue returnValue = null;
        final int stopIndex = findCachedIndex(columnStop);
        for (int j = findCachedIndex(columnStart);
                j <= stopIndex && j < VALUE_CACHE_SIZE; j++) {

            final AverageValue tmp = _localAvgData[row][j];

            if (tmp == null) {
                continue;
            }

            if (returnValue == null) {
                if (tmp.getIntervalStart() > totalMinTime) {
                    returnValue = new AverageValue(tmp);
                } else {
                    returnValue = calculateRealAverageData(row, totalMinTime, tmp.getIntervalStop());
                }
            } else {
                if (tmp.getIntervalStop() < totalMaxTime) {
                    returnValue.appendAverage(tmp);
                } else {
                    returnValue.appendAverage(calculateRealAverageData(row, returnValue.getIntervalStop(), totalMaxTime));
                }
            }
        }

        return returnValue;
    }

    public void calculateAvgData(final int row) {
        if (_localAvgData == null) {
            _localAvgData = new AverageValue[_rows][VALUE_CACHE_SIZE];
        }

        _avgCalculationOK = true;
        final int maximumIndex = _timeSerie.getMaximumIndex();

        final int intervalLength = _columns / VALUE_CACHE_SIZE;
        for (int i = 0; i < VALUE_CACHE_SIZE && i * intervalLength + _startIndex <= maximumIndex; i++) {
            double localMean = 0;
            final double startTime = _timeSerie.getValue(i * intervalLength + _startIndex);
            double stopTime = startTime;
            for (int j = 1 + i * intervalLength; j < (i + 1) * intervalLength; j++) {
                if (j + _startIndex + 1 > maximumIndex) {
                    _avgCalculationOK = false; // this enshures to re-run the calculation when the data
                    // was not yet completely written.
                    break;
                }

                stopTime = _timeSerie.getValue(j + 1 + _startIndex);
                final double deltaT = stopTime - _timeSerie.getValue(_startIndex + j);
                final double value = getValue(row, j + _startIndex);
                localMean += value * deltaT;
            }

            if (stopTime - startTime > 0) {
                localMean /= stopTime - startTime;
                _localAvgData[row][i] = new AverageValue(localMean, startTime, stopTime);
            }
        }

    }

    private void calculateHiLoData() {
        float[][] tmpData = _data;

        if (tmpData == null) {
            tmpData = deCompress();
        }

        final HiLoData[][] hiLoData = new HiLoData[_rows][VALUE_CACHE_SIZE];
        final int intervalLength = _columns / VALUE_CACHE_SIZE;
        for (int row = 0; row < _rows; row++) {
            for (int i = 0; i < VALUE_CACHE_SIZE; i++) {
                float minValue = Float.NaN;
                float maxValue = Float.NaN;
                for (int j = i * intervalLength; j < (i + 1) * intervalLength; j++) {
                    assert tmpData[j] != null;

                    float compareValue = tmpData[j][row];
                    
                    if (compareValue == compareValue) {
                        if (maxValue == maxValue) {
                            maxValue = Math.max(compareValue, maxValue);
                        } else {
                            maxValue = compareValue;
                        }
                        
                        if (minValue == minValue) {
                            minValue = Math.min(compareValue, minValue);
                        } else {
                            minValue = compareValue;
                        }                        
                    }                                                            
                }

                hiLoData[row][i] = HiLoData.hiLoDataFabric(minValue, maxValue);
            }
        }
        _hiLoData = hiLoData;
    }

    private void calculateDifferenceDeCompression(final int[][] returnValue) {
        for (int i = 0; i < returnValue.length; i++) {
            for (int k = 0; k < MORDER_DIFF; k++) {
                for (int j = MORDER_DIFF; j < returnValue[0].length; j++) {
                    returnValue[i][j] = returnValue[i][j] + returnValue[i][j - 1];
                }
            }
        }
    }

    /**
     *
     * @param row the row in the data junk
     * @param start the start column in the data junk (global indices)
     * @param stop the stop column in the data junk (global indices)
     * @return Min-Max values, that are really calculated from the original
     * data. CAREFUL: this is computational expensive, since the data may have
     * to be de-compressed!
     */
    private HiLoData calculateRealHiLowData(final int row, final int start, final int stop) {
        assert stop >= _startIndex;
        final int correctedStart = Math.max(start, _startIndex);
        final int correctedStop = Math.min(stop, _columns + _startIndex - 1);
        HiLoData hld = null;
        for (int i = correctedStart; i <= correctedStop; i++) {
            final float value = (float) getValue(row, i);
            hld = HiLoData.mergeFromValue(hld, value);
        }
        assert hld != null : correctedStart + " " + correctedStop + " " + start + " " + stop + " " + _startIndex + " " + _columns;
        return hld;
    }
        

    private AverageValue calculateRealAverageData(final int row, final double startTime, final double stopTime) {
        double localMean = 0;
        double totalTime = 0;
        final int columnStart = Math.max(_startIndex, _timeSerie.findTimeIndex(startTime));
        final int columnStop = Math.min(_timeSerie.findTimeIndex(stopTime), _startIndex + _columns);

        for (int i = columnStart; i < columnStop && i < _timeSerie.getMaximumIndex() - 1; i++) {
            final double deltaT = _timeSerie.getValue(i + 1) - _timeSerie.getValue(i);
            final double value = getValue(row, i);
            localMean += value * deltaT;
            totalTime += deltaT;
        }
        if (totalTime > 0) {
            return new AverageValue(localMean / totalTime, startTime, stopTime);
        } else {
            return null;
        }

    }

    @Override
    public int getJunkSizeInBytes() {
        return _memInBytes;
    }

    @Override
    public int getCacheSizeInBytes() {
        int returnValue = 0;

        if (_hiLoData != null && _hiLoData.length > 0) {
            returnValue += _hiLoData.length * _hiLoData[0].length * DOUBLE_BYTES;
        }
        return returnValue;
    }

    class CompressThread implements Runnable {

        private final Object _parent;

        /**
         * @param parent compress thread is synchronized on parent!
         */
        CompressThread(final Object parent) {
            _parent = parent;
        }

        @SuppressWarnings("PMD")
        @Override
        public void run() {
            synchronized (_parent) {
                calculateHiLoData();

                //calculateAvgData();
                try {

                    final int[][] compressData = calculateDifferenceCompression(_data);
                    _compressor = new CompressorIntMatrix(compressData);
                    //System.out.println("compressor:  " + _compressor.compressionRatio + " " + _compressor.compressionTime);
                    _memInBytes = _compressor.getCompressedMemory();

//                compressionCounter++;
//                compressionSum += 1.0 * byteLength / originalLength;
//                System.out.println("compression ratio: " + compressionSum / compressionCounter);
                    IntegerMatrixCache.recycleIntArray(compressData);
                } catch (java.lang.OutOfMemoryError ex) {
                    JOptionPane.showMessageDialog(null,
                            "Could not allocate enough memory for Fourier transformation!",
                            "Memory error!",
                            JOptionPane.ERROR_MESSAGE);
                }

                for (int i = 0; i < _data.length; i++) {
                    _container.recycleArray(_data[i]);
                }

                _data = null;
                _dataSoftRef = null;
            }
        }

        private int[][] calculateDifferenceCompression(final float[][] data) {
            int[][] compressData = IntegerMatrixCache.getCachedIntArray(_rows, _columns);

            for (int i = 0; i < _rows; i++) {
                for (int j = 0; j < _columns; j++) {
                    compressData[i][j] = Float.floatToIntBits(roundFloat(data[j][i]));
                }
            }

            for (int i = 0; i < _rows; i++) {
                for (int k = 0; k < MORDER_DIFF; k++) {
                    for (int j = _columns - 1; j >= MORDER_DIFF; j--) {
                        compressData[i][j] = compressData[i][j] - compressData[i][j - 1];
                    }
                }
            }
            return compressData;
        }
    }

    private int findCachedIndex(final int searchColumn) {
        int returnValue = ((searchColumn - _startIndex) * VALUE_CACHE_SIZE) / _columns;
        returnValue = Math.max(0, returnValue);
        returnValue = Math.min(VALUE_CACHE_SIZE, returnValue);
        return returnValue;
    }
}
