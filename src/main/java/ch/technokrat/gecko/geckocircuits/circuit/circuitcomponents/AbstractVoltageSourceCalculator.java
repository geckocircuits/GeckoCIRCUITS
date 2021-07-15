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


public abstract class AbstractVoltageSourceCalculator extends CircuitComponent<AbstractTwoPortLKreisBlock>
    implements AStampable, DirectCurrentCalculatable, HistoryUpdatable {

    protected int _z = -1;


    public AbstractVoltageSourceCalculator(final AbstractTwoPortLKreisBlock parent) {
        super(parent);
    }
    
    
    @Override
    public void stampMatrixA(final double[][] matrix, final double deltaT) {
        assert _z > 0;
        matrix[matrixIndices[0]][_z] += (+1.0);
        matrix[matrixIndices[1]][_z] += (-1.0);
        matrix[_z][matrixIndices[0]] += (+1.0);
        matrix[_z][matrixIndices[1]] += (-1.0);

    }

    @Override
    public final int getZValue() {
        return _z;
    }

    @Override
    public final void setZValue(final int value) {
        _z = value;
    }

    @Override
    public final void updateHistory(final double[] potentials) {
        // System.out.println("function: " + _function + " " + _z);
        _current = potentials[_z];  // SpgQuellen-Stroeme stehen als Unbekannte ausnahmsweise auch im Knotenpotetial-Vektor
        _potential1 = potentials[matrixIndices[0]];
        _potential2 = potentials[matrixIndices[1]];
    }            
}
