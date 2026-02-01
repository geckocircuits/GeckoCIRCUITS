/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.core.control.calculators;

import ch.technokrat.gecko.core.control.IsDtChangeSensitive;

/**
 * Calculator that delays an input signal by a specified time.
 *
 * This block implements a pure delay (transport delay) using a circular buffer.
 * The output at time t equals the input at time (t - delayTime).
 *
 * Features:
 * - Configurable delay time
 * - Handles time step changes via IsDtChangeSensitive
 * - Uses circular buffer for efficient memory usage
 * - Zero output during initial buffer filling period
 *
 * @author GeckoCIRCUITS Team
 */
public final class DelayCalculator extends AbstractSingleInputSingleOutputCalculator
        implements InitializableAtSimulationStart, IsDtChangeSensitive {

    /** Original time step used when delay buffer was created */
    private double _originalDt;

    /** Circular buffer storing delayed values */
    private double[] _delayBuffer = null;

    /** Current write position in circular buffer */
    private int _bufferIndex = -1;

    /** Flag indicating buffer is still being filled */
    private boolean _bufferEmpty;

    /** Delay time to use at next initialization */
    private double _initDelayTime;

    /** Current active delay time */
    private double _delayTime;

    /**
     * Creates a delay calculator with the specified delay time.
     *
     * @param delayTime delay time in seconds (must be non-negative)
     * @throws IllegalArgumentException if delayTime is negative
     */
    public DelayCalculator(final double delayTime) {
        super();
        setDelayTime(delayTime);
    }

    @Override
    public void initWithNewDt(final double deltaT) {
        _delayTime = _initDelayTime;

        // Calculate new buffer size
        int newBufferSize = Math.max(1, (int) (_delayTime / deltaT));
        double[] newBuffer = new double[newBufferSize];

        // Interpolate old buffer values into new buffer
        if (_delayBuffer != null && _delayBuffer.length > 0) {
            final double ratio = deltaT / _originalDt;
            final int newStartIndex = Math.min(Math.max(0, (int) (_bufferIndex / ratio)), newBuffer.length - 1);

            for (int i = 0; i < newBuffer.length; i++) {
                int oldIndex = (int) (ratio * i);
                if (oldIndex >= 0 && oldIndex < _delayBuffer.length) {
                    newBuffer[i] = _delayBuffer[oldIndex];
                }
            }
            _bufferIndex = newStartIndex;
        } else {
            _bufferIndex = 0;
        }

        _delayBuffer = newBuffer;
        _originalDt = deltaT;
    }

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        _delayTime = _initDelayTime;
        _originalDt = deltaT;
        _bufferEmpty = true;

        // Create buffer sized for delay time
        int bufferSize = Math.max(1, (int) (_delayTime / deltaT));
        _delayBuffer = new double[bufferSize];
        _bufferIndex = 0;
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        if (_bufferEmpty) {
            // Buffer is still being filled
            if (deltaT > _delayTime) {
                // Minimal delay - just pass through input
                _outputSignal[0][0] = _inputSignal[0][0];
                return;
            }

            // Output zero while filling
            _outputSignal[0][0] = 0;

            // Store input in buffer
            _delayBuffer[_bufferIndex] = _inputSignal[0][0];
            _bufferIndex++;

            // Check if buffer is now full
            if (_bufferIndex >= _delayBuffer.length) {
                _bufferEmpty = false;
                _bufferIndex = 0;
            }
        } else {
            // Normal operation - output oldest value, store new input
            _outputSignal[0][0] = _delayBuffer[_bufferIndex];
            _delayBuffer[_bufferIndex] = _inputSignal[0][0];

            // Advance circular buffer index
            _bufferIndex++;
            _bufferIndex %= _delayBuffer.length;
        }
    }

    /**
     * Sets the delay time.
     *
     * The new delay time will take effect at the next simulation start
     * or when initWithNewDt() is called.
     *
     * @param delayTime delay time in seconds (must be non-negative)
     * @throws IllegalArgumentException if delayTime is negative
     */
    public void setDelayTime(final double delayTime) {
        if (delayTime < 0) {
            throw new IllegalArgumentException("Error: Delay time must be positive!");
        }
        _initDelayTime = delayTime;
    }

    /**
     * Gets the current delay time.
     *
     * @return the active delay time in seconds
     */
    public double getDelayTime() {
        return _delayTime;
    }
}
