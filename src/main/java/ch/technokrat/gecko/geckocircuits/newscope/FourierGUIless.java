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

//for use with GeckoSCRIPT - like the DialogFourier object but without the GUI

import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckoscript.GeckoInvalidArgumentException;
import javax.swing.JOptionPane;

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
        if (_baseFreq <= 0) {
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

    private double[][][] calculate() {  // eventuell OutOfMemoryError bei zuvielen Oberschwingungen
        final double[][] anVals = new double[_header.length][_nMax - _nMin + 1];
        final double[][] bnVals = new double[_header.length][_nMax - _nMin + 1];
        final double[][] cnVals = new double[_header.length][_nMax - _nMin + 1];  // Amplitude
        final double[][] jnVals = new double[_header.length][_nMax - _nMin + 1];  // Winkel [rad]

        
        
        int index1 = 0; // Startpunkt finden:
        while (_worksheet.getTimeValue(index1, 0) <= _rng1) {
            index1++;
        }
        final int startIndex = findStartIndex();
        
        final int stopIndex = findStopIndex(startIndex);
        

        final int numberOfSamples = stopIndex - startIndex;
        
        _nValues = 1;
        while (_nValues < numberOfSamples) {
            _nValues *= 2;
        }

        if (_nValues > numberOfSamples) {
            _nValues /= 2;
        }

        final float stopTime = (float) _worksheet.getTimeValue(stopIndex, 0);
        final float startTime = (float) _worksheet.getTimeValue(startIndex, 0);

        final double timeSpan = stopTime - startTime;

        for (int i2 = 0; i2 < _header.length; i2++) {  // Schleife ueber alle Fourier-zu-zerlegenden Kurven

                float[] data = new float[_nValues];
                int jjj = startIndex;
                for (int i = 0; i < _nValues; i++) {
                    while (_worksheet.getTimeValue(jjj, 0) < startTime + i * timeSpan / _nValues) {
                        jjj++;
                    }
                    data[i] = (float) _worksheet.getValue(i2, jjj);                    
                }
                
                
                
                Cispr16Fft.realft(data, 1);
                for (int n = _nMin; n <= _nMax; n++) {
                    anVals[i2][n - _nMin] = 2 * data[2 * n] / _nValues;
                    bnVals[i2][n - _nMin] = 2 * data[2 * n + 1] / _nValues ;
                }
            
        }                        
        // Auswertung:
        double[][][] returnValue = evaluate(anVals, bnVals, cnVals, jnVals);                 
        return returnValue;
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

    private int findStartIndex() {
        int index1 = 0; // Startpunkt finden:
        while (_worksheet.getTimeValue(index1, 0) <= _rng1) {
            index1++;
        }
        return index1;
    }

    private int findStopIndex(final int startIndex) {
        int index1 = startIndex;
        int returnValue = 0;
        while ((index1 < _worksheet.getMaximumTimeIndex(0)) 
                && (_worksheet.getTimeValue(index1 + 1, 0) > _worksheet.getTimeValue(index1, 0))
                && (_rng1 <= _worksheet.getTimeValue(index1, 0)) 
                && (_worksheet.getTimeValue(index1, 0) <= _rng2)) {  // Schleife Zeitbereich [t1...t2]
            returnValue = index1;
            index1++;
        }
        return returnValue;
    }
}
