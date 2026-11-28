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
package gecko.core.control.calculators;

import gecko.core.control.StatelessCalculation;

/** Splits one vector signal into ordered scalar outputs. */
public final class DEMUXCalculator extends AbstractControlCalculatable implements InitializableAtSimulationStart, StatelessCalculation {
    /** @param outputCount the vector width and scalar output count */
    public DEMUXCalculator(int outputCount) {
        super(1, outputCount);
    }

    @Override
    public void berechneYOUT(double deltaT) {
        for (int port = 0; port < _outputSignal.length; port++) {
            _outputSignal[port][0] = _inputSignal[0][port];
        }
    }

    @Override
    public void initializeAtSimulationStart(double deltaT) {
        validateInputSignalWidths();
    }

    @Override
    public void validateInputSignalWidths() {
        if (_inputSignal[0].length != _outputSignal.length) {
            throw new IllegalArgumentException("The DEMUX input width must equal its output count.");
        }
    }

    @Override
    public boolean checkInputWithoutConnectionAndFill(int inputIndex) {
        if (_inputSignal[inputIndex] != null) return false;
        _inputSignal[inputIndex] = new double[_outputSignal.length];
        return true;
    }
}
