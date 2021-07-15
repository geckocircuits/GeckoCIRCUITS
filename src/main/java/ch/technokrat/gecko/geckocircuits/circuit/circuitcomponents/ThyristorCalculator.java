/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import static ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractSwitchCalculator.NEARLY_ZERO_R;
import static ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.CircuitComponent.disturbanceValue;


public class ThyristorCalculator extends AbstractSwitchCalculator implements CurrentCalculatable {

    private double _tReverse;
    private double _lastSwitchEvent;
    private static final double REVERSE_FACTOR = 3.0;
    
    public ThyristorCalculator(final Thyristor parent) {
        super(parent);
    }

    @Override
    public final void stampMatrixA(final double[][] matrix, final double deltaT) {
        final double aValue = 1.0 / _rDt;  //  +1/r
        matrix[matrixIndices[0]][matrixIndices[0]] += (+aValue);
        matrix[matrixIndices[1]][matrixIndices[1]] += (+aValue);
        matrix[matrixIndices[0]][matrixIndices[1]] += (-aValue);
        matrix[matrixIndices[1]][matrixIndices[0]] += (-aValue);
    }

    @Override
    public final void stampVectorB(final double[] bVector, final double time, final double deltaT) {
        final double bValue = _uForward / _rDt;
        bVector[matrixIndices[0]] += (+bValue);
        bVector[matrixIndices[1]] += (-bValue);
    }

    @Override
    public final void calculateCurrent(final double[] pVector, final double deltaT, final double time) {

        _voltage = pVector[matrixIndices[0]] - pVector[matrixIndices[1]];
        _current = (_voltage - _uForward) / _rDt;

        if (isTurnOffNeeded()) {
            toTurnOff(time);        
        }
        if(isTurnOnNeeded()) {
            doTurnOn();        
        }        
    }

    /**
     * function is overwritten, since the thyristor behaves different:
     * - switch off only after _tReverse
     * - switch off only when current is < 0
     * @param value
     */
    @Override
    public final void setGateSignal(final boolean value) {
        _gateValue = value;
    }

    public final void setTRR(final double value) {
        _tReverse = value;
    }

    private void doTurnOn() {
        
            // gate==1  und  (uD > uf) und Thyristor "OFF"
            _rDt = _rON;
            if (_bVector != null) {
                _bVector.setUpdateAllFlag();
            }
            DiodeCalculator.diodeSwitchError = true;
            _rDt = Math.max(_rDt, NEARLY_ZERO_R);
            switchAction = true;        
    }

    private void toTurnOff(final double time) {        
            if (time - _lastSwitchEvent > REVERSE_FACTOR * _tReverse) {
                _lastSwitchEvent = time;
            }

            if (time - _lastSwitchEvent >= _tReverse) {
                _rDt = _rOFF;
                DiodeCalculator.diodeSwitchError = true;
                if (_bVector != null) {
                    _bVector.setUpdateAllFlag();
                }
                _rDt = Math.max(_rDt, NEARLY_ZERO_R);
                switchAction = true;

            }       
    }

    private boolean isTurnOffNeeded() {
        return _voltage + _current * _rDt < (disturbanceValue * _uForward) && (_rDt < _rOFF);
    }

    private boolean isTurnOnNeeded() {
        return _gateValue && (((_voltage) > (disturbanceValue * _uForward)) && (_rDt > _rON));
    }
}