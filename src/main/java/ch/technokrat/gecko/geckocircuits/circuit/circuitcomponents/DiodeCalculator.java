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


public final class DiodeCalculator extends CircuitComponent<Diode> implements AStampable, BStampable, CurrentCalculatable {

    private double _uForward = DEFAULT_U_FORWARD;
    private double _rOn = DEFAULT_R_ON;
    private double _rOff = DEFAULT_R_OFF;
    /**
     * varying component resistance (small when conducting, large when blocking
     */
    private double _rDt = _rOff;
    static boolean diodeSwitchError = false;
    static boolean inSwitchErrorMode = false;
    static boolean diodeErrorOccurred = false;
    private BVector _bVector;

    public DiodeCalculator(final Diode parent) {
        super(parent);
        
        _uForward = parent._forwardVoltageDrop.getValue();        
        _rOn = parent._onResistance.getValue();
        _rOff = parent._offResistance.getValue();

    }

    public void setOnResistance(final double res) {
        _rOn = res;
    }

    public void setOffResistance(final double res) {
        _rOff = res;
    }

    @Override
    public void stampMatrixA(final double[][] matrix, final double deltaT) {
        final double aValue = 1.0 / _rDt;  //  +1/r
        assert aValue == aValue;
        matrix[matrixIndices[0]][matrixIndices[0]] += (+aValue);
        matrix[matrixIndices[1]][matrixIndices[1]] += (+aValue);
        matrix[matrixIndices[0]][matrixIndices[1]] += (-aValue);
        matrix[matrixIndices[1]][matrixIndices[0]] += (-aValue);
    }

    @Override
    public void stampVectorB(final double[] bVector, final double time, final double deltaT) {
        final double bValue = _uForward / _rDt;
        //System.out.println(_element._elementName.getValue() + " stamp B: " + bValue + "  VF: " + _uForward + ", rDt: " + _rDt);
        bVector[matrixIndices[0]] += (+bValue);
        bVector[matrixIndices[1]] += (-bValue);
    }

    @Override
    public void calculateCurrent(final double[] pVector, final double deltaT, final double time) {
        _potential1 = pVector[matrixIndices[0]];
        _potential2 = pVector[matrixIndices[1]];
        _voltage = _potential1 - _potential2;

        final double resistorVoltage = _voltage - _uForward;
        _current = resistorVoltage / _rDt;
                       
        // Andy bugfix/modification: _rDt * current was probably missing, also in the old GeckoCIRCUITS
        if (_voltage + _rDt * _current <= disturbanceValue * _uForward) {
            if (_rDt < _rOff) {
                //System.out.println("voltage less than VF for " + _element._elementName.getValue() + " when in ON state");
                //System.out.println("_voltage = " + _voltage + ", _current = " + _current + ", _rDt = " + _rDt);
                diodeSwitchError = true;
                _rDt = _rOff;
                _bVector.setUpdateAllFlag();
            }
        } else {
            if (_rDt > _rOn) {
                //System.out.println("voltage greater than VF for " + _element._elementName.getValue() + " when in OFF state");
                //System.out.println("_voltage = " + _voltage + ", _current = " + _current + ", _rDt = " + _rDt);
                diodeSwitchError = true;
                _rDt = _rOn;
                _bVector.setUpdateAllFlag();
            }
        }




//        if (((p[matrixIndices[0]] - p[matrixIndices[1]]) < (Diode.stoerGroesse * _uForward)) && (_rD_t < _rOff)) {  // (uD < uf) und Diode "ON"
//            _rD_t = _rOff;

//            diodeSwitchError = true;
//        }
//
//        if ((((p[matrixIndices[0]] - p[matrixIndices[1]]) > (Diode.stoerGroesse * _uForward)) && (_rD_t > _rOn))) {  // (uD > uf) und Diode "OFF"
//            _rD_t = _rOn;

//            diodeSwitchError = true;
//        }

    }

    public boolean isBasisStampable() {
        return true;
    }

    @Override
    public void registerBVector(final BVector bvector) {
        _bVector = bvector;
    }

    public void setUForward(final double value) {
        _uForward = value;
    }

    public SwitchState getState(final double time) {

        SwitchState state;
        SwitchState.State componentState;

        if (_rDt > _rOn) {
            componentState = SwitchState.State.OFF;
        } else {
            componentState = SwitchState.State.ON;
        }

        state = new SwitchState(_parent, componentState, time);

        return state;

    }
}
