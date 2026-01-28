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

import ch.technokrat.gecko.core.control.FrequencyDataCore;
import ch.technokrat.gecko.core.control.IsDtChangeSensitive;
import ch.technokrat.gecko.core.control.OutputDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculator implementing the Sliding Discrete Fourier Transform algorithm.
 *
 * This is an efficient DFT implementation that updates the frequency content
 * incrementally as each new sample arrives, without recalculating the full
 * transform each time step.
 *
 * Features:
 * - Configurable averaging window size
 * - Multiple frequency outputs with selectable output type (magnitude, real, imag, phase)
 * - Handles time step changes via IsDtChangeSensitive
 *
 * @author GeckoCIRCUITS Team
 */
public final class SlidingDFTCalculator extends AbstractControlCalculatable
        implements InitializableAtSimulationStart, IsDtChangeSensitive {

    private int[] _frequencyIndicesMap;
    private int[] _frequencyIndicesSet;
    private final double _averageSpanSecs;
    private int _size;
    private int _idx;

    // Input signal time data buffer
    private double[] _timeData;
    private double _oldestDataReal;
    private double _newestDataReal;

    // Frequency bins (real and imaginary parts)
    private double[] _freqsReal;
    private double[] _freqsImag;

    private final List<FrequencyDataCore> _data;

    /**
     * Creates a sliding DFT calculator.
     *
     * @param noOutputs number of output signals
     * @param avgSpan averaging window size in seconds
     * @param freqData list of frequency configurations
     */
    public SlidingDFTCalculator(final int noOutputs, final double avgSpan,
                                final List<FrequencyDataCore> freqData) {
        super(1, noOutputs);
        _averageSpanSecs = avgSpan;
        _data = new ArrayList<>(freqData);
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        _oldestDataReal = _timeData[_idx];
        _timeData[_idx] = _inputSignal[0][0];
        _newestDataReal = _inputSignal[0][0];

        doSlidingFourierStep();

        if (++_idx == _size) {
            _idx = 0;
        }

        for (int i = 0; i < _frequencyIndicesMap.length; i++) {
            final int index = _frequencyIndicesSet[_frequencyIndicesMap[i]];

            switch (_data.get(i).getOutputData()) {
                case ABS:
                    _outputSignal[i][0] = 2 * Math.sqrt((_freqsReal[index] * _freqsReal[index]
                            + _freqsImag[index] * _freqsImag[index])) / _size;
                    break;
                case REAL:
                    _outputSignal[i][0] = 2 * _freqsReal[index] / _size;
                    break;
                case IMAG:
                    _outputSignal[i][0] = 2 * _freqsImag[index] / _size;
                    break;
                case PHASE:
                    _outputSignal[i][0] = Math.atan2(_freqsImag[index], _freqsReal[index]);
                    break;
                default:
                    _outputSignal[0][0] = 0;
            }
        }
    }

    private void doSlidingFourierStep() {
        final double deltaReal = _newestDataReal - _oldestDataReal;
        for (int i : _frequencyIndicesSet) {
            final int index = (i * _idx) % _size;
            final double argument = -Math.PI * 2 * index / ((double) _size);
            _freqsReal[i] += deltaReal * Math.cos(argument);
            _freqsImag[i] += deltaReal * Math.sin(argument);
        }
    }

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        _size = (int) Math.round(_averageSpanSecs / deltaT);
        _timeData = new double[_size];
        _freqsReal = new double[_size + 1];
        _freqsImag = new double[_size + 1];
        calculateFrequencyIndices();

        _oldestDataReal = 0;
        _newestDataReal = 0;
        _idx = 0;
    }

    private void calculateFrequencyIndices() {
        _frequencyIndicesMap = new int[_data.size()];
        final List<Integer> freqIndicesReduced = new ArrayList<>();

        for (int i = 0; i < _data.size(); i++) {
            final double freq = _data.get(i).getFrequency();
            final int index = (int) Math.round((freq * _averageSpanSecs));
            if (!freqIndicesReduced.contains(index)) {
                freqIndicesReduced.add(index);
            }
            _frequencyIndicesMap[i] = freqIndicesReduced.indexOf(index);
        }

        _frequencyIndicesSet = new int[freqIndicesReduced.size()];
        for (int i = 0; i < freqIndicesReduced.size(); i++) {
            _frequencyIndicesSet[i] = freqIndicesReduced.get(i);
        }
    }

    @Override
    public void initWithNewDt(final double deltaT) {
        final int oldSize = _size;
        _size = (int) Math.round(_averageSpanSecs / deltaT);
        final double scalingFactor = 1.0 * _size / oldSize;
        final double[] inRealOld = _timeData;
        final double[] freqsRealOld = _freqsReal;
        final double[] freqsImagOld = _freqsImag;

        initializeAtSimulationStart(deltaT);
        _freqsReal = freqsRealOld;
        _freqsImag = freqsImagOld;

        for (int i = 0; i < _freqsReal.length; i++) {
            _freqsReal[i] *= scalingFactor;
            _freqsImag[i] *= scalingFactor;
        }

        if (inRealOld.length < _size) {
            for (int i = 0; i < _size; i++) {
                _timeData[i] = inRealOld[(int) (i / scalingFactor)];
            }
        } else {
            for (int i = 0; i < _size; i++) {
                _timeData[i] = inRealOld[(int) (i / scalingFactor)];
            }
        }
    }
}
