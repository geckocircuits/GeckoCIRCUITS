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
package gecko.core.signal;

//for use with GeckoSCRIPT - like the DialogFourier object but without the GUI

import gecko.core.datacontainer.AbstractDataContainer;
import gecko.core.GeckoInvalidArgumentException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Stores worksheet reference for GUI-less Fourier analysis")
public final class FourierGUIless {
    private final AbstractDataContainer _worksheet;
    private final String[] _header;
    private final double _baseFreq;  // Grundfrequenz fuer Fourieranalyse
    private final int _nMin, _nMax;  // Grundfrequenz-Vielfache fuer Fourieranalyse
    private final double _rng1, _rng2;  // Bereichsgrenze fuer Berechnung
    private int _nValues;
    private static final int FOUR = 4;

    public FourierGUIless(final AbstractDataContainer worksheet, final double startTime,
            final double endTime, final int harmonics) {
        this._worksheet = worksheet;
        _header = new String[_worksheet.getRowLength()];
        for(int i = 0; i < _worksheet.getRowLength(); i++) {
            _header[i] = _worksheet.getSignalName(i);
        }
        _nMin = 0;
        _nMax = harmonics;
        _rng1 = startTime;
        _rng2 = endTime;
        _baseFreq = 1.0 / (_rng2 - _rng1);
    }

   @SuppressWarnings("PMD")
   public double[][][] doFourier() throws GeckoInvalidArgumentException {
        if ((_nMin < 0) || (_nMax < 0) || (_nMin > _nMax)) {
              throw new GeckoInvalidArgumentException("Invalid harmonics value for Fourier analysis.");
        }
        if (!Double.isFinite(_rng1) || !Double.isFinite(_rng2) || !Double.isFinite(_baseFreq) || _baseFreq <= 0) {
              throw new GeckoInvalidArgumentException("Invalid range supplied for Fourier analysis.");
        }
        double[][][] erg;
        try {
              erg = calculate();
              return erg;
        } catch (java.lang.OutOfMemoryError er) {
              throw new RuntimeException("Could not allocate enough memory for Fourier transformation!");
        }
    }

    private double[][][] calculate() throws GeckoInvalidArgumentException {
        final int maximum = _worksheet.getMaximumTimeIndex(0);
        if (maximum < 1 || _rng1 < _worksheet.getTimeValue(0, 0)
                || _rng2 > _worksheet.getTimeValue(maximum, 0)) {
            throw new GeckoInvalidArgumentException("Analysis bounds must be within a recorded clock with at least two samples.");
        }
        for (int i = 0; i <= maximum; i++) {
            double time = _worksheet.getTimeValue(i, 0);
            if (!Double.isFinite(time) || (i > 0 && time <= _worksheet.getTimeValue(i - 1, 0))) {
                throw new GeckoInvalidArgumentException("Recorded timestamps must be finite and strictly increasing.");
            }
        }
        int startIndex = 0;
        while (startIndex < maximum && _worksheet.getTimeValue(startIndex + 1, 0) <= _rng1) startIndex++;
        int stopIndex = startIndex + 1;
        while (stopIndex < maximum && _worksheet.getTimeValue(stopIndex, 0) < _rng2) stopIndex++;
        _nValues = Integer.highestOneBit(stopIndex - startIndex);
        if (_nMax > 0 && _nMax >= _nValues / 2) {
            throw new GeckoInvalidArgumentException("Harmonic count exceeds the selected signal resolution.");
        }
        final double[][] anVals = new double[_header.length][_nMax + 1];
        final double[][] bnVals = new double[_header.length][_nMax + 1];
        final double[][] cnVals = new double[_header.length][_nMax + 1];
        final double[][] jnVals = new double[_header.length][_nMax + 1];
        final double duration = _rng2 - _rng1;
        for (int row = 0; row < _header.length; row++) {
            float[] data = new float[_nValues];
            int source = startIndex;
            for (int i = 0; i < _nValues; i++) {
                double relativeTime = i * (duration / _nValues);
                while (source < maximum && _worksheet.getTimeValue(source + 1, 0) - _rng1 <= relativeTime) source++;
                data[i] = _worksheet.getValue(row, source);
            }
            if (_nValues > 1) Cispr16Fft.realft(data, 1);
            anVals[row][0] = 2 * data[0] / _nValues;
            for (int n = 1; n <= _nMax; n++) {
                anVals[row][n] = 2 * data[2 * n] / _nValues;
                bnVals[row][n] = 2 * data[2 * n + 1] / _nValues;
            }
        }
        return evaluate(anVals, bnVals, cnVals, jnVals);
    }

    private double[][][] evaluate(final double[][] anVals, final double[][] bnVals,
            final double[][] cnVals, final double[][] jnVals) {
        for (int i2 = 0; i2 < _header.length; i2++) {
            for (int n = _nMin; n <= _nMax; n++) {

                if (n == 0) {  // DC-Gleichanteil
                    cnVals[i2][n - _nMin] = anVals[i2][n - _nMin] / 2.0;
                    jnVals[i2][n - _nMin] = 0;
                } else {
                    cnVals[i2][n - _nMin] = Math.sqrt(anVals[i2][n - _nMin] * anVals[i2][n - _nMin]
                            + bnVals[i2][n - _nMin] * bnVals[i2][n - _nMin]);
                    jnVals[i2][n - _nMin] = Math.atan2(anVals[i2][n - _nMin], bnVals[i2][n - _nMin]);
                 }
            }
        }

        double[][][] erg = new double[FOUR][][];
        int index = 0;
        erg[0] = anVals;
        erg[++index] = bnVals;
        erg[++index] = cnVals;
        erg[++index] = jnVals;


        return erg;
    }

}
