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
 * @author Zimmi
 */
public final class DisplayScale  {

    private final int _attenuation;
    private final String _unit;
    private final int[] _stepValues = new int[]{40, 50, 100, 200, 500, 1, 2, 4, 5, 10, 12};
    private final int _curIndex;
    private final String _displayValue;
    private static final int BASE = 10;
    private static final int UNITCONDITION = 1000;
    private static final int MAXSWITCHINDEX = 5;
    
    
    public DisplayScale() {
        _attenuation = 0;
        _unit = "V";
        _displayValue = "500 mV/div";
        _curIndex = MAXSWITCHINDEX;
    }

    public DisplayScale(final int index, final int attenuation, final String unit) {
        _attenuation = attenuation;
        _unit = unit;

        if (index > _stepValues.length - 2 && _attenuation != 1) {
            _curIndex = _stepValues.length - 2;
        } else {
            _curIndex = index;
        }

        final int scaleFactor = (int) Math.pow(BASE, _attenuation);

        if ((_stepValues[_curIndex] * scaleFactor < UNITCONDITION) && _curIndex < MAXSWITCHINDEX) {
            _displayValue = (_stepValues[_curIndex] * scaleFactor) + " m" + _unit + "/div";
        } else {
            _displayValue = (_stepValues[_curIndex] * scaleFactor) + _unit + "/div";
        }
    }
    
    @Override
    public String toString() {
        return _displayValue;
    }
    
    public int getAttenuation() {
        return _attenuation;
    }
    
    public String getUnit() {
        return _unit;
    }
    
    public int getCurIndex() {
        return _curIndex;
    }
    
    public int getMaxIndex() {
        return _stepValues.length;
    }
}
