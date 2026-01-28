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
package ch.technokrat.gecko.core.datacontainer;

import java.util.List;

/**
 * HiLo-Data represents a maximum-minimum value pair and supplies some
 * insertion/detection methods. This is an immutable object - once created,
 * the final object fields cannot be changed (similar to Double, Integer).
 *
 * This object has a private constructor. Create objects via the factory method.
 *
 * GUI-free version for use in headless simulation core.
 */
public final class HiLoData {

    private static final float LARGE_VALUE = 1E30f;

    // This is a final class with only final members
    public final float _yLo;
    public final float _yHi;

    // Cached common instances for performance
    private static final HiLoData ZERO_DATA = new HiLoData(0, 0);
    private static final HiLoData ZERO_ONE_DATA = new HiLoData(0, 1);
    private static final HiLoData ONE_DATA = new HiLoData(1, 1);

    /**
     * Factory method to create HiLoData instances.
     * Very often, HiLo-Data is filled with common integer values {0, 1} {0,0}, ...
     * Therefore, return static cached objects when possible.
     *
     * @param lowValue minimum value
     * @param highValue maximum value
     * @return HiLoData instance
     */
    public static HiLoData hiLoDataFabric(final float lowValue, final float highValue) {
        if (lowValue == 0) {
            if (highValue == 0) {
                return ZERO_DATA;
            }
            if (highValue == 1) {
                return ZERO_ONE_DATA;
            }
        }

        if (lowValue == 1 && highValue == 1) {
            return ONE_DATA;
        }

        return new HiLoData(lowValue, highValue);
    }

    private HiLoData(final float minValue, final float maxValue) {
        _yLo = minValue;
        _yHi = maxValue;
    }

    public boolean compare(final HiLoData toCompare) {
        return toCompare._yLo == _yLo && toCompare._yHi == _yHi;
    }

    /**
     * Merges a new value into an existing HiLoData, extending the interval if needed.
     *
     * @param data existing HiLoData (can be null)
     * @param value value that may extend the current data interval
     * @return new HiLoData containing the merged interval
     */
    public static HiLoData mergeFromValue(final HiLoData data, final float value) {
        HiLoData returnValue = data;
        if (returnValue == null || returnValue._yHi != returnValue._yHi || returnValue._yLo != returnValue._yLo) {
            returnValue = hiLoDataFabric(value, value);
            return returnValue;
        }

        if (value < returnValue._yLo) {
            return hiLoDataFabric(value, data._yHi);
        }

        if (value > returnValue._yHi) {
            return hiLoDataFabric(data._yLo, value);
        }

        return returnValue;
    }

    /**
     * Merges two HiLoData instances into one encompassing both intervals.
     *
     * @param hilo1 first HiLoData
     * @param hilo2 second HiLoData
     * @return merged HiLoData
     */
    public static HiLoData merge(final HiLoData hilo1, final HiLoData hilo2) {
        if (hilo1 == null) {
            assert hilo2 != null;
            return hilo2;
        }
        if (hilo2 == null) {
            assert hilo1 != null;
            return hilo1;
        }

        float low1 = hilo1._yLo;
        float low2 = hilo2._yLo;

        float high1 = hilo1._yHi;
        float high2 = hilo2._yHi;

        float returnLow = Float.NaN;
        float returnHigh = Float.NaN;

        if (low1 == low1 && low2 == low2) {
            returnLow = Math.min(low1, low2);
        } else {
            if (low1 == low1) {
                returnLow = low1;
            }
            if (low2 == low2) {
                returnLow = low2;
            }
        }

        if (high1 == high1 && high2 == high2) {
            returnHigh = Math.max(high1, high2);
        } else {
            if (high1 == high1) {
                returnHigh = high1;
            }
            if (high2 == high2) {
                returnHigh = high2;
            }
        }

        return hiLoDataFabric(returnLow, returnHigh);
    }

    /**
     * Merges a list of HiLoData into one encompassing all intervals.
     *
     * @param list list of HiLoData to merge
     * @return merged HiLoData
     */
    public static HiLoData getMergedFromList(final List<HiLoData> list) {
        assert list.size() > 0;
        float minValue = Float.NaN;
        float maxValue = Float.NaN;

        for (HiLoData hilo : list) {
            assert hilo != null : hilo + " list size: " + list.size();

            float compareMax = hilo._yHi;
            float compareMin = hilo._yLo;

            if (compareMax == compareMax) {
                if (maxValue == maxValue) {
                    maxValue = Math.max(compareMax, maxValue);
                } else {
                    maxValue = compareMax;
                }
            }

            if (compareMin == compareMin) {
                if (minValue == minValue) {
                    minValue = Math.min(compareMin, minValue);
                } else {
                    minValue = compareMin;
                }
            }
        }
        return hiLoDataFabric(minValue, maxValue);
    }

    public float getIntervalRange() {
        return _yHi - _yLo;
    }

    @Override
    public String toString() {
        return "HiLoData[max=" + _yHi + ", min=" + _yLo + "]";
    }

    public boolean isValidNumber() {
        return _yHi == _yHi && _yLo == _yLo && !Double.isInfinite(_yHi) && !Double.isInfinite(_yLo);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Float.floatToIntBits(this._yLo);
        hash = 47 * hash + Float.floatToIntBits(this._yHi);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HiLoData other = (HiLoData) obj;
        if (Float.floatToIntBits(this._yLo) != Float.floatToIntBits(other._yLo)) {
            return false;
        }
        if (Float.floatToIntBits(this._yHi) != Float.floatToIntBits(other._yHi)) {
            return false;
        }
        return true;
    }

    public boolean containsNumber(float isInRange) {
        return _yLo <= isInRange && isInRange <= _yHi;
    }
}
