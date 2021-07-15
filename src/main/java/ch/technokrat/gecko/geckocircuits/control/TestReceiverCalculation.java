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

import ch.technokrat.gecko.geckocircuits.datacontainer.ContainerStatus;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerSimple;
import ch.technokrat.gecko.geckocircuits.newscope.Cispr16Fft;

class TestReceiverCalculation {                
    
    private final Cispr16Settings _settings;
    final Cispr16Fft _fftOrig;

    public TestReceiverCalculation(final DataContainerSimple dataContainer, final Cispr16Settings settings) {
        if (dataContainer == null) {
            throw new RuntimeException("Error: no data available for testreceiver calculation!");
        }

        if (dataContainer.getContainerStatus() != ContainerStatus.FINISHED) {
            throw new RuntimeException("Error: cannot start testreceiver calculation during simulation!");
        }
        
        _settings = settings;        
        _fftOrig = new Cispr16Fft(dataContainer, _settings._useBlackman.getValue());        
    }

    
    private int getIndexFromFrequency(final double frequency) {
        if (frequency > _fftOrig.baseFrequency * _fftOrig._resampledN / 2.5) {
            throw new RuntimeException("Error: frequency " + frequency + " out of range. Please Decrease your simulation stepwidth.");
        }
        return (int) Math.round(frequency / _fftOrig.baseFrequency);
    }

    double calculateQpAtFrequency(final double frequency) {
        final QuasiPeakCalculator thread = new QuasiPeakCalculator(getIndexFromFrequency(frequency), _fftOrig, _settings);
        return thread._quasiPeak;
    }

    double[] calculateAtFrequency(final double frequency) {
        final QuasiPeakCalculator thread = new QuasiPeakCalculator(getIndexFromFrequency(frequency), _fftOrig, _settings);
        return new double[]{thread._peakValue, thread._quasiPeak, thread._avgValue};
    }

    double calculateAvgAtFrequency(final double frequency) {
        QuasiPeakCalculator thread = new QuasiPeakCalculator(getIndexFromFrequency(frequency), _fftOrig, _settings);
        return thread._avgValue;
    }

    double calculatePeakAtFrequency(final double frequency) {
        final QuasiPeakCalculator thread = new QuasiPeakCalculator(getIndexFromFrequency(frequency), _fftOrig, _settings);
        return thread._peakValue;
    }
    
}
