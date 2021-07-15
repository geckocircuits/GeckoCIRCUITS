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
package ch.technokrat.gecko.geckocircuits.control.calculators;

import ch.technokrat.gecko.geckocircuits.control.IsDtChangeSensitive;
import ch.technokrat.gecko.geckocircuits.control.SSAShape;
import static ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable._time;
import static ch.technokrat.gecko.geckocircuits.control.calculators.AbstractSignalCalculator.TWO_PI;
import ch.technokrat.gecko.geckocircuits.newscope.Cispr16Fft;
import ch.technokrat.gecko.geckocircuits.newscope.FFTLibrary;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SmallSignalCalculator extends AbstractControlCalculatable implements InitializableAtSimulationStart, IsDtChangeSensitive {

    //static boolean isSimulationDC;
    private static final int THREE = 3;
    private static final int FOUR = 4;
    private static final int NOFREQSMAX = 50;

    public static double[][] _bode = new double[THREE][];

    private final SSAShape _signalType;
    private final double _amplitude;
    private final double _freqStart;
    private final double _freqEnd;

    private final int _nMax;

    public AbstractSignalCalculatorPeriodic _signalTypeCalculator;
    private int _noFreqs;
    private double[] ss_aVals;
    private double[] ss_bVals;
    private double[] data_aVals;
    private double[] data_bVals;
    private double[] magnitudeValues;

    public int _N;
    public double[] _measuredValues;
    public double[] _smallSignalValues;
    public int circularIndex = 0;
    private final boolean _addOutput;
    private boolean _circularArrayFilled = false;
    public double _calculationDeltaT;
    public int _numberSamples = 0;
    
    public SmallSignalCalculator(final double amplitude, final double freqLow, final double freqHigh,
            final SSAShape signalShape, final int noInputs, final int noOutput, boolean addOutput) {
        super(noInputs, noOutput);

        _amplitude = amplitude;
        _freqStart = freqLow;
        _freqEnd = freqHigh;
        _signalType = signalShape;
        _addOutput = addOutput;

        _noFreqs = NOFREQSMAX;
        calculateSimFreqs();
        _nMax = (int) Math.round(_bode[0][_bode[0].length - 1] / _freqStart);

        switch (_signalType) {
            case SINE:
            case EXTERNAL:
                break;
            case RECTANGLE:
                _signalTypeCalculator = new SignalCalculatorRectangle(1, 2 * _amplitude, _freqStart, 0, -_amplitude, 0.5);
                _signalTypeCalculator.initializeAtSimulationStart(0);
                break;
            case TRIANGLE:
                _signalTypeCalculator = new SignalCalculatorTriangle(1, _amplitude, _freqStart, 0, 0, 0.5);
                _signalTypeCalculator.initializeAtSimulationStart(0);
                break;
            default:
                assert false;
        }
    }

    public void externalSetTime(double time) {
        _time = time;
        _signalTypeCalculator._time = time;
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        double smallSignal = calculateSmallSignal(deltaT);

        
        if(_calculationDeltaT == deltaT) {
            _numberSamples ++;
        } else {
            _numberSamples = 1;
            circularIndex = 0;
            _circularArrayFilled = false;
        }
        _calculationDeltaT = deltaT;
        
        _outputSignal[0][0] = smallSignal;
        if (_addOutput) {
            _outputSignal[0][0] += _inputSignal[1][0];
        }

        
        _smallSignalValues[circularIndex] = smallSignal;
        _measuredValues[circularIndex] = _inputSignal[0][0];

        if (circularIndex < _N - 1) {
            circularIndex++;
        } else {
            circularIndex = 0;
            _circularArrayFilled = true;
        }

    }

    @Override
    public void tearDownOnPause() {

        
        System.out.println("xxx " + _nMax + " " + _numberSamples);
        if (_circularArrayFilled) {            

            try {
                calculateFourier();
            } catch (java.lang.OutOfMemoryError er) {
                throw new RuntimeException("Could not allocate enough memory for Fourier transformation!");
            }

            calculateBode();
        }

    }

    private void calculateSimFreqs() {
        // Calculation of possible simulation frequencies
        // for sine wave excitation, try to limit he amount of sinus evaluations
        // for all other, do all uneven (odd) harmonics

        int noPossibleFreqs = (int) Math.ceil((Math.floor(_freqEnd / _freqStart)) / 2);;
        double[] possibleSimFreqs = new double[noPossibleFreqs];
        for (int i = 0; i < noPossibleFreqs; i++) {
            possibleSimFreqs[i] = _freqStart * (2 * i + 1);
        }

        if (_signalType != SSAShape.SINE) {
            _bode[0] = possibleSimFreqs;
            return;
        } else {
            // Calculation of logarithmically spaced index
            int[] index1 = new int[_noFreqs];
            for (int i = 0; i < index1.length; i++) {
                index1[i] = (int) Math.pow(noPossibleFreqs, ((double) i / (_noFreqs - 1))) - 1;
            }

            // Make sure that no index appears more than once
            int[] index2 = removeDuplicates(index1, noPossibleFreqs);
            // Calculation of logarithmically spaced simulation frequencies
            _noFreqs = index2.length;
            _bode[0] = new double[_noFreqs];
            for (int i = 0; i < _bode[0].length; i++) {
                _bode[0][i] = possibleSimFreqs[index2[i]];
            }
        }

    }

    public double calculateSmallSignal(final double deltaT) {
        double returnValue = 0;
        switch (_signalType) {
            case SINE:
                for (int i = 0; i < _bode[0].length; i++) {
                    returnValue += _amplitude * Math.sin(TWO_PI * _bode[0][i] * _time);
                }
                break;
            case RECTANGLE:
                _signalTypeCalculator.berechneYOUT(deltaT);
                returnValue = _signalTypeCalculator._outputSignal[0][0];
                break;
            case TRIANGLE:
                _signalTypeCalculator.berechneYOUT(deltaT);
                returnValue = _signalTypeCalculator._outputSignal[0][0];
                break;
            case EXTERNAL:
                returnValue = _inputSignal[2][0];
                break;
            default:
                assert false;
        }
        return returnValue;
    }

    private void printResults(float[] data, float[] smallData) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter("/home/andy/data.txt"));
            if (writer != null) {
                for (int i = 0; i < data.length; i++) {
                    writer.print(data[i] + " " + smallData[i] + "\n");                    
                }
            }   if (writer != null) {
                writer.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(SmallSignalCalculator.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            writer.close();
        }
    }
    

    private void calculateFourier() {
        
        magnitudeValues = new double[_N];
        ss_aVals = new double[_N];
        ss_bVals = new double[_N];
        
        data_aVals = new double[_N];
        data_bVals = new double[_N];

        double[] ddata = new double[_N];
        double[] dsmallSignalData = new double[_N];

        
        int cIndex = circularIndex;
        
        for(int i = 0; i < _N; i++) {
            ddata[i] = _measuredValues[cIndex];            
            dsmallSignalData[i] = this._smallSignalValues[cIndex];                        
            cIndex++;
            if(cIndex == _N) {
                cIndex = 0;
            }
        }
        
        
        
        //printResults(data, smallSignalData);

        FFTLibrary.calculateForwardFFT (ddata);
        FFTLibrary.calculateForwardFFT (dsmallSignalData);                

        for (int n = 0; n <= _nMax; n++) {
            data_aVals[n] = 2 * ddata[2 * n] / _N;
            data_bVals[n] = 2 * ddata[2 * n + 1] / _N;
            ss_aVals[n] = 2 * dsmallSignalData[2 * n] / _N;
            ss_bVals[n] = 2 * dsmallSignalData[2 * n + 1] / _N;
        }
        
        for (int n = 0; n <= _nMax; n++) {

            if (n == 0) {  // DC-Gleichanteil
                magnitudeValues[n] = data_aVals[n] / 2.0f;
            } else {
                magnitudeValues[n] = Math.sqrt(data_aVals[n] * data_aVals[n]
                        + data_bVals[n] * data_bVals[n]);
            }
        }
        

    }
    

    private void calculateBode() {

        _bode[1] = new double[_bode[0].length];
        _bode[2] = new double[_bode[0].length];

        for (int p = 0; p < _bode[0].length; p++) {
            int harmonic = (int) Math.round(_bode[0][p] / _freqStart);

            float magnitude = (float) Math.sqrt(ss_bVals[harmonic] * ss_bVals[harmonic] + ss_aVals[harmonic] * ss_aVals[harmonic]);
            

            _bode[1][p] = 20 * Math.log10(magnitudeValues[harmonic] / magnitude);

            double a = data_aVals[harmonic];
            double b = data_bVals[harmonic];
            double c = ss_aVals[harmonic];
            double d = ss_bVals[harmonic];

            
            
            double x = (a * c + b * d) / (c * c - d * d);
            double y = (c * b - a * d) / (c * c - d * d);
            
            
            //double phaseAngle = 180.0 / Math.PI * Math.abs(Math.atan2(y, x));
            
            double phaseAngle = 180 / Math.PI * Math.atan(y/x);                        
            _bode[2][p] = phaseAngle;
        }

        
        
        // printResults();

    }

    private void printResults() {
        System.out.println("printing results ------------------------");
        for (int i = 0; i < _bode[0].length; i++) {
            System.out.println("bode " + i + " " + _bode[0][i] + "\t" + _bode[1][i] + "\t" + _bode[2][i]);
        }
    }

    private int[] removeDuplicates(final int[] arr, final int noValues) {
        boolean[] set = new boolean[noValues]; //values must default to false
        int totalItems = 0;

        for (int i = 0; i < arr.length; ++i) {
            if (set[arr[i]] == false) {
                set[arr[i]] = true;
                totalItems++;
            }
        }

        int[] ret = new int[totalItems];
        int c = 0;
        for (int i = 0; i < set.length; ++i) {
            if (set[i] == true) {
                ret[c++] = i;
            }
        }
        return ret;
    }

    @Override
    public void initializeAtSimulationStart(double deltaT) {
        
        double baseFreq = _freqStart;
        double baseTime = 1.0 / baseFreq;
        _N = (int) Math.round(baseTime / deltaT);
        
        _N = Math.max(2, _N);
        _measuredValues = new double[_N];
        _smallSignalValues = new double[_N];
        _circularArrayFilled = false;        
    }

    @Override
    public void initWithNewDt(double dt) {
        initializeAtSimulationStart(dt);
    }
    
}
