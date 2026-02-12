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
package gecko.geckocircuits.control.calculators;

public final class PDCalculator extends AbstractControlCalculatable {

    private double _gain;
    private double _oldValue = 0;
    
    public PDCalculator(final double gain) {
        super(1, 1);
        setGain(gain);
    }    

    @Override
    public void berechneYOUT(final double deltaT) {
        // vereinfachte Formel ohne yalt --> wird numerisch viel robuster
        _outputSignal[0][0] = _gain / deltaT * (_inputSignal[0][0] - _oldValue);  
        _oldValue = _inputSignal[0][0];
    }

    public void setGain(final double gain) {
        _gain = gain;
    }
}
