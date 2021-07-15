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

public final class NiceScale {

    private double _minPoint;
    private double _maxPoint;
    private static final double MAX_TICKS = 10;
    private double _tickSpacing;
    private double _range;
    private double _niceMin;
    private double _niceMax;
    private static final double ONE_PT_FIFE = 1.5;
    private static final int THREE = 3;
    private static final double SEVEN = 7;
    private static final double FIVE = 5;
    private static final double TEN = 10;
    private static final double ZERO_OFFSET = 1e-5;
    

    public NiceScale(final HiLoData minMaxData, boolean isXAxis) {
        assert minMaxData.isValidNumber();

        double SMALL_INCREASE = 1.001;
        if(isXAxis) {
            SMALL_INCREASE = 1.0;
        }
        
        float min = -1;
        float max = 1;

        if (minMaxData != null) {
            min = minMaxData._yLo;
            max = minMaxData._yHi;
        }

        if (min == max && min > 0) {
            if (min > 0) {
                min *= 0.9;
                max *= 1.1;
            } else {
                max *= 0.9;
                min *= 1.1;
            }
        }

        assert max >= min : "Error, max < min! " + min + " " + max;

        if (min == 0) {
            this._minPoint = 0;//(min - max) * ZERO_OFFSET;
        } else {
            if (min > 0) {
                this._minPoint = min / SMALL_INCREASE;
            } else {
                this._minPoint = min * SMALL_INCREASE;
            }

        }

        if (max == 0) {
            this._maxPoint = (max - min) * ZERO_OFFSET;
        } else {
            if (max > 0) {
                this._maxPoint = max * SMALL_INCREASE;
            } else {
                this._maxPoint = max / SMALL_INCREASE;
            }
        }

        if (min == 0 && max == 0) {
            this._minPoint = -1;
            this._maxPoint = 1000;
        }

        calculate();
    }
    
    
    /**
     * Instantiates a new instance of the NiceScale class.
     *
     * @param min the minimum data point on the axis
     * @param max the maximum data point on the axis
     */
    public NiceScale(final HiLoData minMaxData) {
        this(minMaxData, false);
    }

    /**
     * Calculate and update values for tick spacing and nice minimum and maximum data points on the axis.
     */
    private void calculate() {
        this._range = niceNum(_maxPoint - _minPoint, false);
        this._tickSpacing = niceNum(_range / (MAX_TICKS - 1), true);
        this._niceMin =
                Math.floor(_minPoint / _tickSpacing) * _tickSpacing;
        this._niceMax =
                Math.ceil(_maxPoint / _tickSpacing) * _tickSpacing;        
        assert _niceMin == _niceMin : _tickSpacing + " " + _niceMin + " " + _maxPoint + " " + _minPoint + " range " + _range + " "
                + (_maxPoint - _minPoint) + " " + _maxPoint + " " + _minPoint;
        assert _niceMax == _niceMax;        
                
    }

    /**
     * Returns a "nice" number approximately equal to range Rounds the number if round = true Takes the ceiling if round = false.
     *
     * @param range the data range
     * @param round whether to round the result
     * @return a "nice" number to be used for the data range
     */
    public static double niceNum(final double range, final boolean round) {
        double exponent;
        /**
         * exponent of range
         */
        double fraction;
        /**
         * fractional part of range
         */
        double niceFraction;
        /**
         * nice, rounded fraction
         */
        exponent = Math.floor(Math.log10(range));
        fraction = range / Math.pow(TEN, exponent);

        if (round) {
            if (fraction < ONE_PT_FIFE) {
                niceFraction = 1;
            } else if (fraction < THREE) {
                niceFraction = 2;
            } else if (fraction < SEVEN) {
                niceFraction = FIVE;
            } else {
                niceFraction = TEN;
            }
        } else {
            if (fraction <= 1) {
                niceFraction = 1;
            } else if (fraction <= 2) {
                niceFraction = 2;
            } else if (fraction <= FIVE) {
                niceFraction = FIVE;
            } else {
                niceFraction = TEN;
            }
        }

        return niceFraction * Math.pow(TEN, exponent);
    }

    /**
     * Gets the tick spacing.
     *
     * @return the tick spacing
     */
    public double getTickSpacing() {
        return _tickSpacing;
    }

    public HiLoData getNiceLimits() {
        return HiLoData.hiLoDataFabric((float) _niceMin, (float) _niceMax);        
    }
}
