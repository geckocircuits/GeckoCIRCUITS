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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractSemiconductor;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.ForwardVoltageDropable;

public final class LossCalculationSimple implements AbstractLossCalculatorFabric {    
    public static final double UK_DEFAULT_VALUE = 400.0;
        
    double _kON;
    double _kOFF;
    double _uSWnorm = UK_DEFAULT_VALUE;
    
    private final AbstractSemiconductor _parent;

    public LossCalculationSimple(final AbstractSemiconductor parent) {
        super();
        _parent = parent;        
    }    

    @Override
    public AbstractLossCalculator lossCalculatorFabric() {        
        assert _uSWnorm != 0: "Voltage definition must be != 0!";        
        return new LossCalculatorSwitchSimple(_parent);
    }

    void copyPropertiesFrom(final LossCalculationSimple origLosses) {        
        _kON = origLosses._kON;
        _kOFF = origLosses._kOFF;        
        _uSWnorm = origLosses._uSWnorm;
    }

    public final class LossCalculatorSwitchSimple extends AbstractLossCalculatorSwitch {
        private final double _uf;
        private final double _rON;    
        
        public LossCalculatorSwitchSimple(final AbstractSemiconductor parent) {
            super(parent);
            if(parent instanceof ForwardVoltageDropable) {                
                _uf = ((ForwardVoltageDropable) parent).getForwardVoltageDropParameter().getDoubleValue();
            } else {
                _uf = 0;
            }
            _rON = parent._onResistance.getDoubleValue();            
            _uSWnorm = parent.uK.getValue();
            _kON = parent.kOn.getValue();
            _kOFF = parent.kOff.getValue();
        }
               
        @Override
        double calcConductionLoss() {                                    
            return Math.abs(_current * _uf) + (_current * _current * _rON);            
        }

        @Override
        double calcTurnOnSwitchingLoss() {                   
            return Math.abs(_kON * _current / _deltaT);  
        }
        
        @Override
        double calcTurnOffSwitchingLoss() {                                                   
            return Math.abs(_kOFF * _oldCurrent / _deltaT);
        }

        @Override
        double calculateRelativeVoltageFactor(final double appliedVoltage) {            
            double returnValue = Math.abs(appliedVoltage / _uSWnorm);
            if(returnValue != returnValue) {
                System.out.println("xxxxxxxx " + _uSWnorm);
            }
            return returnValue;
        }
        
    }
}
