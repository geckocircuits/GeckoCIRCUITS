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
package gecko.geckocircuits.circuit.losscalculation;

import gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;

abstract class AbstractLossCalculatorSwitch implements AbstractLossCalculator, LossCalculationSplittable {

    static final double EPS = 1e-2;  // praktisch Null --> Schwelle zur Umgehung numerischer Minimalfehler
    double _oldCurrent = -1; // history values of current and voltage
    double _oldVoltage = -1; // ... used for switchingLoss-calculation
    double _voltage;
    double _current;
    double _deltaT;
    private final AbstractCircuitBlockInterface _parent;
    double _conductionLoss;
    double _switchingLoss;

    public AbstractLossCalculatorSwitch(final AbstractCircuitBlockInterface parent) {
        _parent = parent;
    }

    abstract double calcConductionLoss();

    abstract double calcTurnOnSwitchingLoss();

    abstract double calcTurnOffSwitchingLoss();

    @Override
    public void calcLosses(final double current, final double temperature, final double deltaT) {
        _voltage = _parent._voltage;
        _current = current;
        _deltaT = deltaT;        
        final double conductionLoss = calcConductionLoss();
        double switchingLoss = 0;
        if (detectTurnOn()) {                        
            switchingLoss += calcTurnOnSwitchingLoss() * calculateRelativeVoltageFactor(_oldVoltage);                        
        }

        if (detectTurnOff()) {
            switchingLoss += calcTurnOffSwitchingLoss() * calculateRelativeVoltageFactor(_voltage);                                    
        }

        _oldCurrent = _current;
        _oldVoltage = _voltage;
        _conductionLoss = conductionLoss;
        _switchingLoss = switchingLoss;                
    }

    boolean detectTurnOff() {
        return (Math.abs(_oldCurrent) > EPS) && (Math.abs(_current) < EPS);
    }

    boolean detectTurnOn() {
        return (Math.abs(_oldCurrent) < EPS) && (Math.abs(_current) > EPS);
    }

    abstract double calculateRelativeVoltageFactor(final double appliedVoltage);

    @Override
    public final double getTotalLosses() {
        return _switchingLoss + _conductionLoss;
    }
        

    @Override
    public final double getSwitchingLoss() {
        return _switchingLoss;
    }

    @Override
    public final double getConductionLoss() {
        return _conductionLoss;
    }                      
}
