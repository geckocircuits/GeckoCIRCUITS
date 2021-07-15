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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.newscope.Cispr16Fft;

public class QuasiPeakCalculator {

    private static final double A_LOWER_LIMIT = 9000;
    private static final double B_LOWER_LIMIT = 150000;
    private static final double A_NARROW_WIDTH = 150;
    private static final double A_WIDE_WIDTH = 200;
    private int _NN, _i;
    private float[] _fourierTransform;
    private final double _baseFreq;
    public double _peakValue;
    public double _quasiPeak;
    public double _avgValue;
    private double _dt = 0;
    private double _tau_c;
    private double _tau_d;
    private final Cispr16Settings _settings;

    public QuasiPeakCalculator(int calculationIndex, Cispr16Fft fftNew, Cispr16Settings settings) {
        _NN = fftNew._zvResampled.length;
        _settings = settings;
        _i = calculationIndex;
        _fourierTransform = new float[_NN];
        for (int k = 0; k < _NN; k++) {
            _fourierTransform[k] = fftNew._zvResampled[k];
        }

        _baseFreq = fftNew.baseFrequency;
        _dt = 0.5 / (fftNew.baseFrequency * fftNew._resampledN);
        doCalculation();
    }

    private void doCalculation() {
        try {
            float[] bandSpectrum = calculateBandSpectrum(_i, _fourierTransform, _baseFreq);

            Cispr16Fft.realft(bandSpectrum, -1);
            _peakValue = 0;

            if (_settings._useBlackman.getValue()) {
                Cispr16Fft.inverseBlackman(bandSpectrum);
            }

            if (_settings._peak.getValue()) {
                for (int k = 2 * bandSpectrum.length / 5; k < 4 * bandSpectrum.length / 5; k++) {
                    _peakValue = Math.max(_peakValue, Math.abs(bandSpectrum[k]));
                }
            }


            int counter = 0;
            _avgValue = 0;
            if (_settings._average.getValue()) {
                for (int k = bandSpectrum.length / 5; k < 4 * bandSpectrum.length / 5; k++) {
                    _avgValue += Math.abs(bandSpectrum[k]);
                    counter++;
                }
                _avgValue /= counter;
                _avgValue *= Math.PI / 2;
            }


            if (_settings._qpeak.getValue()) {
                double oldQuasiPeak = 0;

                _tau_c = 1E-3;
                _tau_d = 160E-3;
                double normalizationFactor = 1 / 0.928;
                // set time constants to CISPR band A:
                if (_i * _baseFreq < B_LOWER_LIMIT) {
                    _tau_c = 45e-3;
                    _tau_d = 0.5;
                    normalizationFactor = 1 / 0.626;
                }

                _quasiPeak = quasiPeakDetector(bandSpectrum, 0, _dt);

                int minimumRunNumber = (int) (8 * _tau_c / (_dt * _NN / 2));
                // check for convergence:
                int runCounter = 1;
                while (Math.abs((oldQuasiPeak - _quasiPeak) / (oldQuasiPeak + _quasiPeak)) > 0.1
                        || runCounter < minimumRunNumber) {
                    oldQuasiPeak = _quasiPeak;
                    _quasiPeak = quasiPeakDetector(bandSpectrum, _quasiPeak, _dt);
                    runCounter++;
                }

                _quasiPeak *= normalizationFactor;
            }
            System.gc();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        _fourierTransform = null;
    }

    public double quasiPeakDetector(final float[] inputSignalTD, final double startValue, final double dt) {

        final int NNN = inputSignalTD.length;
        //double[] outputSignal = new double[N];
        //outputSignal[0] = startValue;
        final double cap = 1E-9;


        double res2 = _tau_d / cap;
        double res1 = _tau_c / cap; // approximation, but should not matter too much

        // in the first run (no starting value) set the time-constants
        // smaler - this brings everything faster to "steady state"
        if (startValue == 0) {
            res2 *= 0.2;
            res1 *= 0.2;
        }


        double RCdT = res1 * cap / dt;
        double R1_R2 = res1 / res2;
        double Denominator = 1.0 / (1 + RCdT + R1_R2);
        double Denominator2 = 1.0 / (1 + dt / (res2 * cap));
        double outOld = startValue;
        double outValue;

        double meanValue = 0;

        int counter = 0;
        for (int i = NNN / 4; i < 3 * NNN / 4; i++) {
            outValue = (inputSignalTD[i] + outOld * RCdT) * Denominator;
            // solve DGL with backward Euler discretization
            // now we did a wrong guess, correct the value:
            if (inputSignalTD[i] < outValue) {
                outValue = outOld * Denominator2;
            }

            //outputSignal[i] = outValue;
            if (i > NNN / 2) {
                counter++;
                meanValue += outValue;
            }
            outOld = outValue;
        }
        meanValue /= counter;

        return meanValue;

    }

    /**
     * cut off a window of the given spectrum
     *
     * @param NN
     * @param fourIndex
     * @param fn
     * @param fourierTransform careful: parameter values are overwritten /
     * changed
     * @param baseFreq
     * @return
     */
    private float[] calculateBandSpectrum(final int fourIndex, final float[] fourierTransform,
            final double baseFreq) {

        int intervalSpan = (int) (A_NARROW_WIDTH / baseFreq);
        double frequency = fourIndex * baseFreq;

        if (frequency > B_LOWER_LIMIT) {
            intervalSpan = (int) (8e3 / baseFreq);
        }

        for (int deleteIndex = 0; deleteIndex < fourierTransform.length / 2; deleteIndex++) {
            if (deleteIndex <= fourIndex - intervalSpan || deleteIndex >= fourIndex + intervalSpan) {
                fourierTransform[2 * deleteIndex] = 0;
                fourierTransform[2 * deleteIndex + 1] = 0;
            }
        }

//          Here, we just leave the original values!  
//            for (int j = -intervalSpan / 2; j <= intervalSpan / 2; j++) {
//                final int index = fourIndex + j;
//                if (index < 0) {
//                    continue;
//                }                
//            }
//
//
        // make the window with edges that do not fall immediately. Go from zero to 1
        // within 1/2 intervalspan
        for (int j = -intervalSpan; j < -intervalSpan / 2; j++) {
            final double shapeFactor = 2.0 + j * 2.0 / intervalSpan;
            assert shapeFactor <= 1 && shapeFactor >= 0 : shapeFactor;
            final int index = fourIndex + j;
            if (index < 0) {
                continue;
            }
            fourierTransform[2 * index] *= shapeFactor;
            fourierTransform[2 * index + 1] *= shapeFactor;
        }


        for (int j = intervalSpan / 2 + 1; j < intervalSpan; j++) {
            final double shapeFactor = 2.0 + -j * 2.0 / intervalSpan;
            assert shapeFactor <= 1 && shapeFactor >= 0 : shapeFactor;
            final int index = fourIndex + j;
            fourierTransform[2 * index] *= shapeFactor;
            fourierTransform[2 * index + 1] *= shapeFactor;
        }

        return fourierTransform;
    }

    static void calculateMinMaxEstimation(final float[] min, final float[] max, boolean useBlackman, final Cispr16Fft fft) {
        int NN = fft._zvResampled.length;
        int startIndex = 1;
        final double baseFrequency = fft.baseFrequency;
        while (baseFrequency * startIndex < A_LOWER_LIMIT) {
            startIndex++;
        }


        for (int i = startIndex; i < NN / 2; i++) {
            double maxValue = 0;
            double minValue = 0;

            int intervalSpan = (int) (9e3 / baseFrequency);

            // CISPR band A
            if (baseFrequency * i < B_LOWER_LIMIT) {
                intervalSpan = (int) (A_WIDE_WIDTH / baseFrequency);
            }

            for (int j = -intervalSpan / 2; j <= intervalSpan / 2; j++) {
                int index = i + j;
                if (index >= NN / 2) {
                    return;
                }


                double magnitude = fft._magnitudes[index];
                double magnitudeSquared = magnitude * magnitude;

                maxValue += magnitude;
                minValue += magnitudeSquared;
            }

            max[i] = (float) maxValue;
            min[i] = (float) Math.sqrt(minValue);

            if (useBlackman) {
                min[i] /= Cispr16Fft.BLACKMAN_NORM;
                max[i] /= Cispr16Fft.BLACKMAN_NORM;
            }
        }
    }
}