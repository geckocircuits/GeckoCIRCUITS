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

import gecko.geckocircuits.control.NotCalculateableMarker;

public final class ConstantCalculator extends AbstractControlCalculatable implements NotCalculateableMarker {

    public ConstantCalculator(final double constValue) {
        super(0, 1);
        setConst(constValue);
    }

    public void setConst(final double constValue) {
        // since this calculator is "not calculatable), yout will never be executed. Therefore,
        // when setting the const value, the output has to be updated immediately!
        _outputSignal[0][0] = constValue;
    }

    @Override
    public void berechneYOUT(final double deltaT) { // this is notCalculatable!
        assert false;
    }
}
