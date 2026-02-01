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
package ch.technokrat.gecko.core.math;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

/**
 * FFT library wrapper for JTransforms.
 *
 * Provides convenient methods for calculating forward and inverse FFT
 * operations using the JTransforms library.
 *
 * @author GeckoCIRCUITS Team
 */
public final class FFTLibrary {

    private FFTLibrary() {
        // Utility class - prevent instantiation
    }

    /**
     * Calculates the forward FFT of float data in-place.
     *
     * @param data the input data array (modified in-place)
     */
    public static void calculateForwardFFT(float[] data) {
        FloatFFT_1D forwardFFT = new FloatFFT_1D(data.length);
        forwardFFT.realForward(data);
    }

    /**
     * Calculates the forward FFT of double data in-place.
     *
     * @param data the input data array (modified in-place)
     */
    public static void calculateForwardFFT(double[] data) {
        DoubleFFT_1D forwardFFT = new DoubleFFT_1D(data.length);
        forwardFFT.realForward(data);
    }

    /**
     * Calculates the inverse FFT of float data in-place.
     *
     * @param data the input data array (modified in-place)
     */
    public static void calculateInverseFFT(float[] data) {
        FloatFFT_1D inverseFFT = new FloatFFT_1D(data.length);
        inverseFFT.realInverse(data, false);
    }

    /**
     * Calculates the inverse FFT of double data in-place.
     *
     * @param data the input data array (modified in-place)
     */
    public static void calculateInverseFFT(double[] data) {
        DoubleFFT_1D inverseFFT = new DoubleFFT_1D(data.length);
        inverseFFT.realInverse(data, false);
    }
}
