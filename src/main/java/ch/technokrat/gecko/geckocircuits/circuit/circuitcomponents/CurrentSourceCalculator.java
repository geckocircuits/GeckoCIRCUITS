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

import ch.technokrat.gecko.geckocircuits.circuit.TimeFunction;
import ch.technokrat.gecko.geckocircuits.circuit.TimeFunctionConstant;
import java.util.ArrayList;


public class CurrentSourceCalculator extends CircuitComponent implements AStampable, BStampable, HistoryUpdatable {

    private TimeFunction _function;
    private ArrayList<BStampable> _stampUpdateList;

    public CurrentSourceCalculator(final AbstractCurrentSource parent) {
        super(parent);
    }

    @Override
    public final void stampMatrixA(final double[][] matrix, final double deltaT) {
    }

    @Override
    public final void stampVectorB(final double[] bVector, double time, double deltaT) {        
        _current = _function.calculate(time, deltaT);                
        bVector[matrixIndices[0]] -= _current;
        bVector[matrixIndices[1]] += _current;

    }


    public final void setFunction(final TimeFunction function) {
        _function = function;
    }

    @Override
    public boolean isBasisStampable() {
        return  _function instanceof TimeFunctionConstant;
    }

    @Override
    public void registerBVector(final BVector bvector) {
    }

    @Override
    public void stepBack()
    {
        if ((!stepped_back && (steps_reversed == 0)) || (stepped_back && (steps_reversed < steps_saved)))
        {
            if (stepped_back)
                historyBackward();
            prev_time = var_history[0][0];
            //System.out.println("before: _potential 1 = " + _potential1 + " _potential 2 = " + _potential2);
            _potential1 = var_history[0][1];
            _potential2 = var_history[0][2];
            //System.out.println("after: _potential 1 = " + _potential1 + " _potential 2 = " + _potential2);
            _current = var_history[0][3];
            _voltage = var_history[0][4];
            /*if (_needsOldPotCurrent)
            {
                _potOld1 = var_history[0][5];
                _potOld2 = var_history[0][6];
                //System.out.println("before: _oldCurrent = " + _oldCurrent);
                _oldCurrent = var_history[0][7];
                //System.out.println("after: _oldCurrent = " + _oldCurrent);
                _oldOldCurrent = var_history[0][8];
            }*/

            _function.stepBack();

            stepped_back = true;
            steps_reversed++;
        }
    }
    
    @Override
    public final void updateHistory(final double[] potentials) {
        _potential1 = potentials[matrixIndices[0]];
        _potential2 = potentials[matrixIndices[1]];        
    }

}
