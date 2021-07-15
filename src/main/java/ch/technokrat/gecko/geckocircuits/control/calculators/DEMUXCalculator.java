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
package ch.technokrat.gecko.geckocircuits.control.calculators;

import ch.technokrat.gecko.geckocircuits.control.ReglerDemux;

public final class DEMUXCalculator extends AbstractControlCalculatable implements InitializableAtSimulationStart {
    private final ReglerDemux _parent;

    public DEMUXCalculator(final int noOutputs, final ReglerDemux parent) {
        super(1, noOutputs);
        _parent = parent;
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        for (int i = 0; i < _outputSignal.length; i++) {
            _outputSignal[i][0] = _inputSignal[0][i];  // Signal-Quelle
        }
    }

    @Override
    public String toString() {
        return super.toString() + " " + _parent.getStringID();
    }
    
    

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        if(_parent._connectedJavaBlock != null) {
            _inputSignal[0] = _parent._connectedJavaBlock._calculator._outputSignal[_parent._connectedJavaOutputIndex];
        }
        if (_outputSignal.length != _inputSignal[0].length) {            
            throw new RuntimeException("Signals of DEMUX \"" + _parent.getStringID() +"\" input are not consistent:\n"
                    + "Input size: " + _inputSignal[0].length + "\n"
                    + "Output size: " + _outputSignal.length);
        }
    }
    
    public boolean checkInputWithoutConnectionAndFill(final int inputIndex) {        
        if(_inputSignal[inputIndex] == null) {
            final int dummyLength = _outputSignal.length;            
            _inputSignal[inputIndex] = new double[dummyLength];
            return true;
        } else {
            return false;
        }                        
    }
    
        
}
