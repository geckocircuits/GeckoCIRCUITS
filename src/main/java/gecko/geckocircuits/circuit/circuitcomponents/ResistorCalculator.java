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
package gecko.geckocircuits.circuit.circuitcomponents;


public final class ResistorCalculator extends CircuitComponent<AbstractResistor> implements AStampable, HistoryUpdatable {

    private static final double FAST_NULL_R = 1e-9;

    private double _resistance = 1;

    public ResistorCalculator(final AbstractResistor parent) {
        super(parent);        
        _resistance = parent._resistance.getValue();
    }

    public void setResistance(final double resistance) {
        if(_resistance < FAST_NULL_R) {
            _resistance = FAST_NULL_R;
        } else {
            _resistance = resistance;
        }
    }    

    @Override
    public void stampMatrixA(final double[][] matrix, final double deltaT) {
        final double addValue = 1.0 / _resistance;  //  +1/R        
        matrix[matrixIndices[0]][matrixIndices[0]] += (+addValue);
        matrix[matrixIndices[1]][matrixIndices[1]] += (+addValue);
        matrix[matrixIndices[0]][matrixIndices[1]] += (-addValue);
        matrix[matrixIndices[1]][matrixIndices[0]] += (-addValue);
    }


    @Override
    public String toString() {
        return super.toString() + getClass().getName() + "  " + _resistance + " " + matrixIndices[0] + " " + matrixIndices[1];
    }

    @Override
    public void updateHistory(final double[] potentials) {
        _potential1 = potentials[matrixIndices[0]];
        _potential2 = potentials[matrixIndices[1]];
        _voltage = _potential1 - _potential2;
        // old current is not needed for Resistor, but the current has not yet been
        // calculated:
        _current = (_potential1 - _potential2) / _resistance;
        //_oldCurrent = _current;
    }
    
}
