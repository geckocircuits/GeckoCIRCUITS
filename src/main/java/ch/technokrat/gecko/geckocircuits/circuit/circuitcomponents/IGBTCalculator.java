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

 public final class IGBTCalculator extends AbstractSwitchCalculator implements CurrentCalculatable {


    public IGBTCalculator(final IGBT parent) {
        super(parent);
    }

    @Override
    public void stampMatrixA(final double[][] matrix, final double deltaT) {
        final double aVAlue = 1.0 / _rDt;  //  +1/r
        matrix[matrixIndices[0]][matrixIndices[0]] += (+aVAlue);
        matrix[matrixIndices[1]][matrixIndices[1]] += (+aVAlue);
        matrix[matrixIndices[0]][matrixIndices[1]] += (-aVAlue);
        matrix[matrixIndices[1]][matrixIndices[0]] += (-aVAlue);
    }

    @Override
    public void stampVectorB(final double[] bVector, final double time, final double deltaT) {
        final double bValue = _uForward / _rDt;
        bVector[matrixIndices[0]] += (+bValue);
        bVector[matrixIndices[1]] += (-bValue);
    }

    @Override
    public void calculateCurrent(final double[] pVector, final double deltaT, final double time) {
        _potential1 = pVector[matrixIndices[0]];
        _potential2 = pVector[matrixIndices[1]];
        _voltage = _potential1 - _potential2;
        _current = (_voltage - _uForward) / _rDt;
        //System.out.println("IGBT " + _element._elementName.getValue() + " voltage: " + _voltage + ", current: " + _current);
        if (_gateValue) {
            // Andy bugfix/modification: _rDt * current was probably missing, also in the old GeckoCIRCUITS
            if (_voltage + _rDt * _current <= disturbanceValue * _uForward) {
                if (_rDt < _rOFF) {
                    //System.out.println("voltage less than VF for " + _element._elementName.getValue() + " when in ON state");
                    //System.out.println("_voltage = " + _voltage + ", _current = " + _current + ", _rDt = " + _rDt);
                    DiodeCalculator.diodeSwitchError = true;
                    _rDt = _rOFF;
                    _bVector.setUpdateAllFlag();
                }
            } else {
                if (_rDt > _rON) {
                    //System.out.println("voltage greater than VF for " + _element._elementName.getValue() + " when in OFF state");
                    //System.out.println("_voltage = " + _voltage + ", _current = " + _current + ", _rDt = " + _rDt);
                    DiodeCalculator.diodeSwitchError = true;
                    _rDt = _rON;
                    _bVector.setUpdateAllFlag();
                }
            }
        }

    }
}
