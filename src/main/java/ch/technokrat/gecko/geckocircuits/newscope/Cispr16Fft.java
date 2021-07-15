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
package ch.technokrat.gecko.geckocircuits.newscope;

import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerSimple;

public final class Cispr16Fft {

    public float[] _zvResampled;    
    public float[] _magnitudes;
    public int _maximumNN;
    public int _resampledN = 0;
    private static final double BLACKMAN_A0 = 0.42;
    private static final double BLACKMAN_A1 = 0.5;
    private static final double BLACKMAN_A2 = 0.08;
    private static final double TWO_PI = Math.PI * 2;
    private final static double COEFF_1 = 0.5;
    public double baseFrequency;

    public Cispr16Fft(final DataContainerSimple data, final boolean useBlackman) {
        doResampling(data);        
        data.deleteDataReference();
        System.gc();
        rescaleAmplitudeToRMS(_zvResampled);        

        if (useBlackman) {
            blackmanFiltering(_zvResampled);
        }

        realft(_zvResampled, 1);
        normalizeFFTArray(_zvResampled);                        

        double maxValue = -1e30;
        for (int i = 0; i < _zvResampled.length/ 2 - 1; i++) {
            double abs = Math.sqrt(_zvResampled[2 * i] * _zvResampled[2 * i] + _zvResampled[2 * i + 1] * _zvResampled[2 * i + 1]);
            if (abs > maxValue) {
                maxValue = abs;
            }
        }
        
        _magnitudes = new float[_zvResampled.length/2];
        for(int i = 0; i < _zvResampled.length/2; i++) {
            _magnitudes[i] = getMagnitude(i);
        }
    }

    /**
     * this is not a "real" resampling. We need a 2^N number of samples,
     * therefore we just continue calculation with the last 2^N values, where N
     * is as big as possible.
     *
     * @param data
     * @param reductor additional data reduction factor. If reductor = 0, no
     * further reduction is done. If freductor = 1, the data size is halved...
     */
    private void doResampling(final AbstractDataContainer data) {

        _resampledN = 1;
        while (_resampledN < data.getMaximumTimeIndex(0) / 2) {
            _resampledN *= 2;
        }
        _maximumNN = _resampledN;


        _zvResampled = new float[_resampledN];
        int endShift = 0;

        while (data.getTimeValue(data.getMaximumTimeIndex(0) - endShift, 0) == 0) {
            endShift++;
        }

        for (int i = 1; i <= _resampledN; i++) {
            final int index = data.getMaximumTimeIndex(0) - i - endShift;
            _zvResampled[_resampledN - i] += (float) data.getValue(0, index);
        }

        double lastTime = data.getTimeValue(data.getMaximumTimeIndex(0), 0);
        int resampledStartIndex = data.getMaximumTimeIndex(0) - _resampledN;
        double resampledStart = data.getTimeValue(resampledStartIndex, 0);
        baseFrequency = 1.0 / (lastTime - resampledStart);
    }
    public static final double BLACKMAN_NORM = 2.38;

    private static void blackmanFiltering(final float[] data) {
        assert data.length % 2 == 0 : "Error: data array must be even length!";
        final int NNN = data.length -1;
        
        final double const1 = 2 * Math.PI / NNN;
        
        // the blackman curve is symmetric... speed it up by calculation only half the values.
        for (int ii = 0; ii < NNN / 2+1; ii++) {            
            double factor = calculateBlackmanFactor(const1, ii);
            data[ii] *= factor;
            data[NNN - ii] *= factor;            
        }
    }
    
    static double calculateBlackmanFactor(final double constValue, final int index) {
        final double cosX = Math.cos(index * constValue);
        
        // the cos calculation is quite expensive. We need cos (2* x) here, which can be
        // calculated via the cosine addition theorem!
        final double cos2X = 2 * cosX * cosX -1;            
        return BLACKMAN_NORM * (BLACKMAN_A0 - BLACKMAN_A1 * cosX + BLACKMAN_A2 * cos2X);
    }
        

    public static void inverseBlackman(final float[] data) {
        assert data.length % 2 == 0 : "Error: data array must be even length!";
        final int NNN = data.length -1;        
        final double const1 = 2 * Math.PI / NNN;        
        
        // the blackman curve is symmetric... speed it up by calculation only half the values.
        for (int ii = 0; ii < NNN/2+1; ii++) {
            final double factor = 1.0 / calculateBlackmanFactor(const1, ii);
            data[ii] *= factor;
            data[NNN - ii] *= factor;
        }
    }    
    
    /**
     *
     * @param data input-output array of length N + 1 (!)
     * @param n
     * @param isign 1 if from Timedomain to frequency domain, FD -> TD sign =
     * -1;
     */
    public static void realft(final float[] data, final int isign) {

        try {
            if (isign > 0) {
                FFTLibrary.calculateForwardFFT(data);
                return;
            } else {
                FFTLibrary.calculateInverseFFT(data);
                return;
            }
        } catch (NoClassDefFoundError error) {
            System.err.println("Error: could not find JTransforms class library!\n"
                    + "Continue with built-in FFT algorithm.");

        }

        assert isign == 1 || isign == -1;
        final int nnn = data.length;

        if (nnn % 2 != 0) {
            throw new IllegalArgumentException("Fast Fourier Transform (FFT) is only possible with n = 2^x  data size!");
        }

        int iii, index1, index2, index3, index4, np3;
        double coeff2, h1r, h1i, h2r, h2i;
        double wrValue, wiValue, wpr, wpi, wtemp, theta;

        theta = Math.PI / (float) (nnn >> 1);
        if (isign == 1) {
            coeff2 = -0.5;
            ffour1(data, nnn >> 1, 1);
        } else {
            coeff2 = 0.5;
            theta = -theta;
        }
        wtemp = Math.sin(0.5 * theta);
        wpr = -2.0 * wtemp * wtemp;
        wpi = Math.sin(theta);
        wrValue = 1.0 + wpr;
        wiValue = wpi;
        np3 = nnn + 3;
        for (iii = 2; iii <= (nnn >> 2); iii++) {
            index4 = 1 + (index3 = -2 + np3 - (index2 = 1 + (index1 = iii + iii - 2)));
            h1r = COEFF_1 * (data[index1] + data[index3]);
            h1i = COEFF_1 * (data[index2] - data[index4]);
            h2r = -coeff2 * (data[index2] + data[index4]);
            h2i = coeff2 * (data[index1] - data[index3]);
            data[index1] = (float) (h1r + wrValue * h2r - wiValue * h2i);
            data[index2] = (float) (h1i + wrValue * h2i + wiValue * h2r);
            data[index3] = (float) (h1r - wrValue * h2r + wiValue * h2i);
            data[index4] = (float) (-h1i + wrValue * h2i + wiValue * h2r);
            wrValue = (wtemp = wrValue) * wpr - wiValue * wpi + wrValue;
            wiValue = wiValue * wpr + wtemp * wpi + wiValue;
        }
        if (isign == 1) {
            data[-1 + 1] = (float) (h1r = data[-1 + 2]) + data[-1 + 2];
            data[-1 + 2] = (float) h1r - data[-1 + 2];
        } else {
            data[-1 + 1] = (float) (COEFF_1 * ((h1r = data[-1 + 1] + data[-1 + 2])));
            data[-1 + 2] = (float) (COEFF_1 * (h1r - data[-1 + 2]));
            ffour1(data, nnn >> 1, -1);
        }
    }

    // float-Version von four1()
    //
    public static void ffour1(final float[] data, final int numberPoints, final int isign) {
        int mmm;
        int istep, iii;
        double wtemp, wrr, wpr, wpi, wiii, theta;  // float precision for the trigonometric recurrences.
        float tempr, tempi;
        final int nnn = numberPoints << 1;

        int jjj = 1;

        for (iii = 1; iii < nnn; iii += 2) {  // This is the bit-reversal section of the routine.
            if (jjj > iii) {  // Exchange the two complex numbers.
                float temp = data[jjj - 1];
                data[jjj - 1] = data[iii - 1];
                data[iii - 1] = temp;
                temp = data[jjj];
                data[jjj] = data[iii];
                data[iii] = temp;
            }
            //m = nn;
            mmm = nnn >> 1;
            while ((mmm >= 2) && (jjj > mmm)) {
                jjj -= mmm;
                mmm >>= 1;
            }
            jjj += mmm;
        }

        // Here begins the Danielson-Lanczos section of the routine.
        int mmax = 2;
        while (nnn > mmax) {  // Outer loop executed log2 nn times.
            istep = mmax << 1;
            theta = isign * (TWO_PI / mmax);  // Initialize the trigonometric recurrence.
            wtemp = Math.sin(theta / 2);
            wpr = -wtemp * wtemp * 2;
            wpi = Math.sin(theta);
            wrr = 1.0;
            wiii = 0.0;
            for (mmm = 1; mmm < mmax; mmm += 2) {  // Here are the two nested inner loops.
                for (iii = mmm; iii <= nnn; iii += istep) {
                    jjj = iii + mmax;  // This is the Danielson-Lanczos for mula:
                    tempr = (float) (wrr * data[jjj - 1] - wiii * data[jjj]);
                    tempi = (float) (wrr * data[jjj] + wiii * data[jjj - 1]);
                    data[jjj - 1] = data[iii - 1] - tempr;
                    data[jjj] = data[iii] - tempi;
                    data[iii - 1] += tempr;
                    data[iii] += tempi;
                }
                wrr = (wtemp = wrr) * wpr - wiii * wpi + wrr;  // Trigonometric recurrence.
                wiii = wiii * wpr + wtemp * wpi + wiii;
            }
            mmax = istep;
        }
    }
    
    private static void normalizeFFTArray(float[] data) {
        final int dataSize = data.length;
        final float normalizeFactor = (float) (2.0 / dataSize);
        for(int i = 0; i < dataSize; i++) {
            data[i] *= normalizeFactor; 
        }
    }

    private void rescaleAmplitudeToRMS(final float[] real) {
        final float sqrt2 = (float) Math.sqrt(2);
        for (int i = 0; i < real.length; i++) {
            real[i] /= sqrt2;
        }
    }

    static long magnitudeCounter = 0;
    private float getMagnitude(final int frequencyIndex) {        
        final double imag = _zvResampled[2 * frequencyIndex + 1];
        final double real = _zvResampled[2 * frequencyIndex];
        return (float) Math.sqrt(real * real + imag * imag);
    }
}
