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

import gecko.geckocircuits.circuit.TimeFunction;
import gecko.geckocircuits.circuit.TimeFunctionConstant;

public class VoltageSourceCalculator extends AbstractVoltageSourceCalculator implements BStampable,
        DirectCurrentCalculatable, HistoryUpdatable {

    private TimeFunction _function;
    private static final int THREE = 3;
    private static final int FOUR = 4;
    
    public VoltageSourceCalculator(final TimeFunction timeFunction, final AbstractVoltageSource parent) {
        super(parent);
        _function = timeFunction;
        _z = -1;
    }

    VoltageSourceCalculator(final double initialValue, final int mat0, final int mat1, 
            final int zValue, final int compNumber, final AbstractTwoPortLKreisBlock parent) {
        super(parent);
        _function = new TimeFunctionConstant(-initialValue);
        matrixIndices[0] = mat0;
        matrixIndices[1] = mat1;
        _componentNumber = compNumber;
        _z = zValue;
    }

    @Override
    public final void stampVectorB(final double[] bVector, final double time, final double deltaT) {
        bVector[_z] += _function.calculate(time, deltaT);
    }

    public final void setFunction(final TimeFunction function) {
        _function = function;
    }

    @Override
    public final boolean isBasisStampable() {
        return false;
    }

    @Override
    public final void registerBVector(final BVector bvector) {
        // nothing todo!
    }

    @Override
    public final void stepBack() {
        if ((!stepped_back && (steps_reversed == 0)) || (stepped_back && (steps_reversed < steps_saved))) {
            if (stepped_back) {
                historyBackward();
            }
            prev_time = var_history[0][0];
            //System.out.println("before: _potential 1 = " + _potential1 + " _potential 2 = " + _potential2);
            _potential1 = var_history[0][1];
            _potential2 = var_history[0][2];
            //System.out.println("after: _potential 1 = " + _potential1 + " _potential 2 = " + _potential2);
            _current = var_history[0][THREE];
            _voltage = var_history[0][FOUR];
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
}
