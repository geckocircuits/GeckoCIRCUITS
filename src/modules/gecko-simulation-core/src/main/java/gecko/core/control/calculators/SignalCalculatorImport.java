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


/**
 * Repeats an immutable time/value table with linear interpolation.
 * Times before the first sample hold its value. Exact period ends retain the final sample.
 */
public final class SignalCalculatorImport extends AbstractSignalCalculator implements InitializableAtSimulationStart, StatelessCalculation {
    private final double _signalDuration;
    private final double[][] _xy;

    public SignalCalculatorImport(final double[][] dataTable) {
        super(0);
        if (dataTable == null || dataTable.length < 2 || dataTable[0] == null || dataTable[0].length < 2) {
            throw new IllegalArgumentException("A signal table requires time and value rows with at least two samples.");
        }
        int samples = dataTable[0].length;
        _xy = new double[dataTable.length][];
        for (int row = 0; row < dataTable.length; row++) {
            if (dataTable[row] == null || dataTable[row].length != samples) {
                throw new IllegalArgumentException("All signal table rows must have the same sample count.");
            }
            _xy[row] = dataTable[row].clone();
        }
        for (int i = 0; i < samples; i++) {
            if (!Double.isFinite(_xy[0][i]) || !Double.isFinite(_xy[1][i])
                    || (i > 0 && _xy[0][i] <= _xy[0][i - 1])) {
                throw new IllegalArgumentException("Signal values must be finite and sample times must strictly increase.");
            }
        }
        _signalDuration = _xy[0][samples - 1] - _xy[0][0];
        if (!Double.isFinite(_signalDuration) || _signalDuration <= 0) {
            throw new IllegalArgumentException("The signal duration must be finite and positive.");
        }
    }

    /**
     * Tells whether the output depends on absolute time only through a pattern that repeats every {@code period}.
     * A table with one constant value repeats in any positive period. Any other table must start at time zero or
     * earlier, so that no flat hold prefix exists, and the period must be a whole multiple of the table duration.
     *
     * @param period the period in seconds
     * @return true when the output at time t and at t plus the period is the same for every t of a run
     */
    public boolean repeatsEvery(final double period) {
        if (!Double.isFinite(period) || period <= 0) {
            return false;
        }
        boolean constant = true;
        for (double value : _xy[1]) {
            constant &= value == _xy[1][0];
        }
        if (constant) {
            return true;
        }
        double cycles = period / _signalDuration;
        double whole = Math.rint(cycles);
        return _xy[0][0] <= 0 && whole >= 1 && Math.abs(cycles - whole) <= 1e-9;
    }

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        // The immutable table is validated at construction. Lookup has no cached repeat offset.
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        if (!Double.isFinite(getTime())) {
            throw new IllegalArgumentException("Signal lookup time must be finite.");
        }
        double firstTime = _xy[0][0];
        double lastTime = _xy[0][_xy[0].length - 1];
        double time = getTime();
        if (time <= firstTime) {
            _outputSignal[0][0] = _xy[1][0];
            return;
        }
        if (time > lastTime) {
            double remainder = (time - lastTime) % _signalDuration;
            // Preserve the final sample at exact period ends, matching the repeat convention.
            time = remainder == 0 ? lastTime : firstTime + remainder;
        }
        int exact = java.util.Arrays.binarySearch(_xy[0], time);
        if (exact >= 0) {
            _outputSignal[0][0] = _xy[1][exact];
            return;
        }
        int upper = -exact - 1;
        int lower = upper - 1;
        double fraction = (time - _xy[0][lower]) / (_xy[0][upper] - _xy[0][lower]);
        _outputSignal[0][0] = (1 - fraction) * _xy[1][lower] + fraction * _xy[1][upper];
    }
}
