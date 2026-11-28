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

/** Adapts the shared sparse matrix calculation to the desktop signals and clock. */
public final class SparseMatrixCalculator extends AbstractControlCalculatable implements InitializableAtSimulationStart {
    private final gecko.core.control.calculators.SparseMatrixCalculator calculator =
            new gecko.core.control.calculators.SparseMatrixCalculator();

    public SparseMatrixCalculator() {
        super(8, 9);
    }

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        calculator.initializeAtSimulationStart(deltaT);
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        System.arraycopy(_inputSignal, 0, calculator._inputSignal, 0, _inputSignal.length);
        calculator.setSimulationTime(_time);
        calculator.berechneYOUT(deltaT);
        for (int port = 0; port < _outputSignal.length; port++) {
            _outputSignal[port][0] = calculator._outputSignal[port][0];
        }
    }

    /** Preserves the desktop pulse-width entry point on the shared calculation owner. */
    public void setPulseWidths(double d1, double d2, double d3, double d4, double d5, double da, double db, double fDR) {
        calculator.setPulseWidths(d1, d2, d3, d4, d5, da, db, fDR);
    }
}