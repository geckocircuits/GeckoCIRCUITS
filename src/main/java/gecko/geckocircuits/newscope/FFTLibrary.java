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

package gecko.geckocircuits.newscope;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

/**
 *
 * @author andy
 */
public class FFTLibrary {

    static void calculateForwardFFT(float[] data) {
        FloatFFT_1D forwardFFT = new FloatFFT_1D(data.length);
        forwardFFT.realForward(data);
    }
    
    public static void calculateForwardFFT(double[] data) {
        DoubleFFT_1D forwardFFT = new DoubleFFT_1D(data.length);        
        forwardFFT.realForward(data);
    }

    static void calculateInverseFFT(float[] data) {
        FloatFFT_1D inverseFFT = new FloatFFT_1D(data.length);
        inverseFFT.realInverse(data, false);
    }    
}
