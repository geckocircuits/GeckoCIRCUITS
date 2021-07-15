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

import java.util.List;

/**
 * HiLo-Data represents a maximum-minimum value and supplies some
 * insertion/detection methods. Careful: this is a non-volatile (immutable)
 * object, i.e. created once, you cannot change the final
 * object fields, similar to the Double, Integer Objects.
 * This object has a private constructor, do not add any public
 * constructor! Create objects via the Fabric.
 *
 * @author andy
 */
public final class HiLoData {

    private static final float LARGE_VALUE = 1E30f;    

    
    // this is a final class with only final members
    // CHECKSTYLE:OFF
    public final float _yLo; 
    public final float _yHi;
    // CHECKSTYLE:ON
    
    private static final HiLoData ZERO_DATA = new HiLoData(0, 0);
    private static final HiLoData ZERO_ONE_DATA = new HiLoData(0, 1);
    private static final HiLoData ONE_DATA = new HiLoData(1, 1);
    
    /**
     * very often, HiLo-Data is filled with integer objects {0, 1} {0,0}, ...
     * Therefore, just return a static object.
     *
     * @return
     */
    public static HiLoData hiLoDataFabric(final float lowValue, final float highValue) {
        if(lowValue == 0) {
            if(highValue == 0) {
                return ZERO_DATA;
            }
            if(highValue == 1) {
                return ZERO_ONE_DATA;
            }
         }
        
        if(lowValue == 1 && highValue == 1) {
            return ONE_DATA;            
         }                            
        
        return new HiLoData(lowValue, highValue);
    }
    
    
    private HiLoData(final float minValue, final float maxValue) {
        //assert minValue <= maxValue : minValue + " not <! " + maxValue;        
        _yLo = minValue;
        _yHi = maxValue;                
    }
        

    public boolean compare(final HiLoData toCompare) {
        return toCompare._yLo == _yLo && toCompare._yHi == _yHi;
    }
    
    /**
     * 
     * @param data if null, a new HiLo-Object is created
     * @param value value that extends the current data interval
     * @return 
     */
    public static HiLoData mergeFromValue(final HiLoData data, final float value) {
        HiLoData returnValue = data;
        if(returnValue == null || returnValue._yHi != returnValue._yHi || returnValue._yLo != returnValue._yLo) {
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

    public static HiLoData merge(final HiLoData hilo1, final HiLoData hilo2) {
        if(hilo1 == null) {
            assert hilo2 != null;
            return hilo2;
        }
        if(hilo2 == null) {
            assert hilo1 != null;
            return hilo1;
        }
        
        float low1 = hilo1._yLo;
        float low2 = hilo2._yLo;
        
        float high1 = hilo1._yHi;
        float high2 = hilo2._yHi;
        
        float returnLow = Float.NaN;
        float returnHigh = Float.NaN;
        
        if(low1 == low1 && low2 == low2) {
            returnLow = Math.min(low1, low2);
        } else {
            if(low1 == low1) {
                returnLow = low1;
            }
            
            if(low2 == low2) {
                returnLow = low2;
            }
        }
        
        if(high1 == high1 && high2 == high2) {
            returnHigh = Math.max(high1, high2);                      
        } else {
            if(high1 == high1) {
                returnHigh = high1;
            }
            if(high2 == high2) {
                returnHigh = high2;                         
            }
        }
                                
        return hiLoDataFabric(returnLow, returnHigh);
    }        

    public static HiLoData getMergedFromList(final List<HiLoData> list) {
        assert list.size() > 0;
        float minValue = Float.NaN;
        float maxValue = Float.NaN;

        
        for (HiLoData hilo : list) {
            assert hilo != null : hilo + " list size: " + list.size();
            
            float compareMax = hilo._yHi;
            float compareMin = hilo._yLo;
            
            if(compareMax == compareMax) {                
                if(maxValue == maxValue) {
                    maxValue= Math.max(compareMax, maxValue);
                } else {
                    maxValue = compareMax;
                }
                
            }
            
            if(compareMin == compareMin) {
                if(minValue == minValue) {
                    minValue= Math.min(compareMin, minValue);
                } else {
                    minValue = compareMin;
                }                
            }                                                
        }
        return hiLoDataFabric(minValue, maxValue);                
    }        
    
    private static float correctWithNotANumber(float value1, float value2) {
        if(value1 != value1 && value2 != value2) {
            return Float.NaN;
        }
        if(value1 != value1) {
            return value2;
        } else {
        // value2 != value2 {
            return value2;
        }
    }

    public float getIntervalRange() {
        return _yHi - _yLo;
    }

    @Override
    public String toString() {
        return super.toString() + " max: " + _yHi + " min: " + _yLo;
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
