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

import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckoscript.GeckoInvalidArgumentException;
import java.util.Arrays;
import java.util.List;

/**
 * Calculates the characteristic values of a datacontainer, e.g. average, rms,
 * klirr, ... Implemented as a static fabric, so that values can only be
 * obtained via the calculateFabric
 *
 * @author andy
 */
public final class CharacteristicsCalculator {

    private static CharacteristicsCalculator _valueCache = null;
    private static double _startTime;
    private static double _endTime;
    private static int[] _rows;
    private static int _dataHashCode;
    private static int _maxTimeIndex;

    private static boolean isAlreadyCalculated(AbstractDataContainer worksheet, int[] rows, double rng1, double rng2) {
        if (_valueCache == null) {
            return false;
        } else {
            if (worksheet.hashCode() != _dataHashCode) {
                return false;
            }

            if (rows.length == _rows.length) {
                for (int index = 0; index < rows.length; index++) {
                    if (rows[index] != _rows[index]) {
                        return false;
                    }
                }
            } else {
                return false;
            }

            if (rng1 != _startTime || rng2 != _endTime) {
                return false;
            }
            if(worksheet.getMaximumTimeIndex(0) != _maxTimeIndex) {
                return false;
            }
            
            return true;
        }
    }
    private final double[] _avg;
    private final double[] _rms2;
    private final double[] _min;
    private final double[] _max;
    private final double[] _peakPeak;
    private final double[] _shape;
    private final double[] _thd;
    private final double[] _klirr;
    private final double[] _ripple;
    private final double[] _gleichrichtwert;
    private final double[] _gsA1;  // Grundschwingungsanteil A1
    private final double[] _gsB1;  // Grundschwingungsanteil B1
    private static final int AVG_CHANNEL = 0;
    private static final int RMS2_CHANNEL = 1;
    private static final int THD_CHANNEL = 2;
    private static final int MIN_CHANNEL = 3;
    private static final int MAX_CHANNEL = 4;
    private static final int RIPPLE_CHANNEL = 5;
    private static final int KLIRR_CHANNEL = 6;
    private static final int SHAPE_CHANNEL = 7;
    private static final int PEAK_PEAK_CHANNEL = 8;
    private static final int TOT_CHANNELS = 9;
    private static final double TIMETHRESHOLD = 1e-2;
    private boolean _isValid = false;

    private CharacteristicsCalculator(final AbstractDataContainer worksheet, final int[] rows,
            final double rng1, final double rng2) {
        final int numberOfDataRows = worksheet.getRowLength();
        _avg = new double[numberOfDataRows];
        _rms2 = new double[numberOfDataRows];
        _min = new double[numberOfDataRows];
        _max = new double[numberOfDataRows];
        _peakPeak = new double[numberOfDataRows];
        _shape = new double[numberOfDataRows];
        _thd = new double[numberOfDataRows];
        _klirr = new double[numberOfDataRows];
        _ripple = new double[numberOfDataRows];
        _gleichrichtwert = new double[numberOfDataRows];
        _gsA1 = new double[numberOfDataRows];  // Grundschwingungsanteil A1
        _gsB1 = new double[numberOfDataRows];  // Grundschwingungsanteil B1

        for (int i = 0; i < _max.length; i++) {
            _max[i] = -Double.MAX_VALUE;
            _min[i] = Double.MAX_VALUE;
        }

        calculate(worksheet, rows, rng1, rng2);
        _isValid = true;
    }

    public double getAVGValue(final int index) {
        return _avg[index];
    }

    public double getRMS2Value(final int index) {
        return _rms2[index];
    }

    public double getMinValue(final int index) {
        return _min[index];
    }
    
    public double getPeakToPeakValue(final int index) {
        return _peakPeak[index];
    }

    public double getMaxValue(final int index) {
        return _max[index];
    }

    public double getShapeValue(final int index) {
        return _shape[index];
    }

    public double getTHDValue(final int index) {
        return _thd[index];
    }

    public double getKlirrValue(final int index) {
        return _klirr[index];
    }

    public double getRippleValue(final int index) {
        return _ripple[index];
    }

    public boolean isValid() {
        return _isValid;
    }

    public void setInvalid() {
        _isValid = false;
    }

    private double[] checkCalculationBounds(final AbstractDataContainer worksheet, final double start, final double end) {
        final int startIndex = worksheet.findTimeIndex(start, 0);
        final int endIndex = worksheet.findTimeIndex(end, 0);

        final double[] bounderies = {start, end};

        if (startIndex > endIndex) {
            throw new RuntimeException("Lower bound > Upper bound");
        }
        if (start < 0) {
            bounderies[0] = worksheet.getTimeValue(startIndex, 0);
            System.out.println("Changed startTime to: " + bounderies[0]);
        }

        if (Math.abs(end - worksheet.getTimeValue(endIndex, 0)) > TIMETHRESHOLD) {
            bounderies[1] = worksheet.getTimeValue(endIndex, 0);
            System.out.println("Changed endTime to: " + bounderies[1]);

            System.out.println("Difference range: " + Math.abs(bounderies[1] - worksheet.getTimeValue(endIndex, 0)));
        }

        if (start != bounderies[0] && end != bounderies[1]) {
            throw new IndexOutOfBoundsException("Error: Invalid bounds");
        } else {
            if (start != bounderies[0]) {
                throw new IndexOutOfBoundsException("Error: Invalid lower bound");
            }
            if (end != bounderies[1]) {
                throw new IndexOutOfBoundsException("Error: Invalid upper bound");
            }
        }


        return bounderies;
    }

    private void calculate(final AbstractDataContainer worksheet, final int[] rows, final double start, final double end) {
        assert _avg != null;
        final double[] bounderies = checkCalculationBounds(worksheet, start, end);
        final double rng1 = bounderies[0];
        final double rng2 = bounderies[1];

        boolean isContinusRowsCalculation = true;
        
        for(int i = 0; i < rows.length; i++) {
            if(rows[i] != i) {
                isContinusRowsCalculation = false;
            }
        }
        
        final int dataRowLength = rows.length;

        final double totalT = rng2 - rng1;

        final int startIndex = worksheet.findTimeIndex(rng1, 0);
        long tick = System.currentTimeMillis();
        
        for (int i1 = startIndex; i1 < worksheet.getMaximumTimeIndex(0)
                && worksheet.getTimeValue(i1 + 1, 0) > worksheet.getTimeValue(i1, 0)
                && worksheet.getTimeValue(i1, 0) < rng2; i1++) {
            final double deltaT = worksheet.getTimeValue(i1 + 1, 0) - worksheet.getTimeValue(i1, 0);
            final double tAktuell = worksheet.getTimeValue(i1, 0);
            final double omegaT = 2 * Math.PI / totalT * tAktuell;
            final double sinOmegaT = Math.sin(omegaT);
            final double cosOmegaT = Math.cos(omegaT);
                       
            for (int i2 = 0; i2 < dataRowLength; i2++) {
                final double wert = worksheet.getValue(rows[i2], i1);                
                _avg[i2] += (wert * deltaT);
                _rms2[i2] += (wert * wert * deltaT);
                _min[i2] = Math.min(_min[i2], wert);
                _max[i2] = Math.max(_max[i2], wert);
                _gleichrichtwert[i2] += Math.abs(wert);
                _gsA1[i2] += wert * cosOmegaT * deltaT;
                _gsB1[i2] += wert * sinOmegaT * deltaT;
            }
        }        
        
        //-------------------
        // Auswertung:
        for (int i2 = 0; i2 < dataRowLength; i2++) {
            _peakPeak[i2] = _max[i2] - _min[i2];
            _avg[i2] /= totalT;
            _rms2[i2] /= totalT;
            _rms2[i2] = Math.sqrt(_rms2[i2]);  // ab hier ist rms2 nicht mehr der quadratische Wert!!
            _gleichrichtwert[i2] /= totalT;
            if (_gleichrichtwert[i2] > 0) {
                _shape[i2] = _rms2[i2] / _gleichrichtwert[i2];
            } else {
                _shape[i2] = 0;
            }

            _gsA1[i2] /= totalT / 2.0;
            _gsB1[i2] /= totalT / 2.0;
            final double sqrtHighHarm = Math.sqrt(_rms2[i2] * _rms2[i2]
                    - (_gsA1[i2] * _gsA1[i2] + _gsB1[i2] * _gsB1[i2]) / 2.0 - (_avg[i2] * _avg[i2]));

            if (_gsA1[i2] * _gsA1[i2] + _gsB1[i2] * _gsB1[i2] > 0) {
                _thd[i2] = sqrtHighHarm / Math.sqrt((_gsA1[i2] * _gsA1[i2] + _gsB1[i2] * _gsB1[i2]) / 2.0);
            } else {
                _thd[i2] = 0;
            }

            if (_rms2[i2] > 0) {
                _klirr[i2] = sqrtHighHarm / _rms2[i2];
            } else {
                _klirr[i2] = 0;
            }

            if (_avg[i2] == 0) {
                _ripple[i2] = 0;
            } else {
                _ripple[i2] = Math.sqrt(_rms2[i2] * _rms2[i2] - _avg[i2] * _avg[i2]) / _avg[i2];
            }


        }

        //Cache Object for further calculations
        _valueCache = this;
        _startTime = start;
        _endTime = end;
        _rows = rows;
        _dataHashCode = worksheet.hashCode();
        _maxTimeIndex = worksheet.getMaximumTimeIndex(0);


    }

    /**
     *
     * @param worksheet Container which holds the data.
     * @param rng1 time value to start the calculation
     * @param rng2 time value where to stop the calculation
     * @return data structure which contains the results
     * @throws Exception
     */
    public static CharacteristicsCalculator calculateFabric(final AbstractDataContainer worksheet,
            final double rng1, final double rng2) {
        int[] rows = new int[worksheet.getRowLength()];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = i;
        }
        return CharacteristicsCalculator.calculateFabric(worksheet, rows, rng1, rng2);
    }

    public static CharacteristicsCalculator calculateFabric(final AbstractDataContainer worksheet, final int[] rows,
            final double rng1, final double rng2) {
        if (isAlreadyCalculated(worksheet, rows, rng1, rng2)) {
            return _valueCache;
        } else {
            return new CharacteristicsCalculator(worksheet, rows, rng1, rng2);
        }
    }

    //to return a particular set of parameters (for a particular scope channel)
    public double[] getChannelCharacteristics(final int channel) throws GeckoInvalidArgumentException {
        final double[] characteristics = new double[TOT_CHANNELS];

        if (channel >= _avg.length) {
            throw new GeckoInvalidArgumentException("non-existant scope channel: " + channel);
        } else {
            characteristics[AVG_CHANNEL] = _avg[channel];
            characteristics[RMS2_CHANNEL] = _rms2[channel];
            characteristics[THD_CHANNEL] = _thd[channel];
            characteristics[MIN_CHANNEL] = _min[channel];
            characteristics[MAX_CHANNEL] = _max[channel];
            characteristics[RIPPLE_CHANNEL] = _ripple[channel];
            characteristics[KLIRR_CHANNEL] = _klirr[channel];
            characteristics[SHAPE_CHANNEL] = _shape[channel];
            characteristics[PEAK_PEAK_CHANNEL] = _peakPeak[channel];

            return characteristics;
        }

    }
}
