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


public class IdealSwitchCalculator extends AbstractSwitchCalculator implements HistoryUpdatable {

    public IdealSwitchCalculator(final IdealSwitch parent) {
        super(parent);
    }

    @Override
    public void stampMatrixA(double[][] matrix, double dt) {
        double aW = 1.0 / _rDt;  //  +1/R        
        assert aW < 1E60;
        matrix[matrixIndices[0]][matrixIndices[0]] += (+aW);
        matrix[matrixIndices[1]][matrixIndices[1]] += (+aW);
        matrix[matrixIndices[0]][matrixIndices[1]] += (-aW);
        matrix[matrixIndices[1]][matrixIndices[0]] += (-aW);
    }

    @Override
    public final void stampVectorB(double[] b, double t, double dt) {
    }


    public boolean isBasisStampable() {
        return true;
    }

    @Override
    public void updateHistory(double[] p) {

        _potential1 = p[matrixIndices[0]];
        _potential2 = p[matrixIndices[1]];

        // old current is not needed for Resistor, but the current has not yet been
        // calculated:
        _current = (p[matrixIndices[0]] - p[matrixIndices[1]]) / _rDt;
        //_oldCurrent = _current;
    }
}
