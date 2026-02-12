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
package gecko.geckocircuits.control.calculators;

import gecko.geckocircuits.control.IsDtChangeSensitive;

public final class DelayCalculator extends AbstractSingleInputSingleOutputCalculator
        implements InitializableAtSimulationStart, IsDtChangeSensitive {

    private double _originalDt;
    private double[] _youtVerzoegert = null;
    private int _zeigerYOUT = -1;
    private boolean _speicherLeer;
    
    /*
     * the delay time value can only be changed at simulation start or at stop->continue.
     * This is the reason for this helper "init" variable!
     */
    private double _initDelayTime;
    private double _delayTime;
    
    public DelayCalculator(final double delayTime) {
        super();
        setDelayTime(delayTime);
    }

    @Override
    public void initWithNewDt(final double deltaT) {
        _delayTime = _initDelayTime;
        double[] youtVerzoegertNew = new double[(int) (_delayTime / deltaT)];
        final double ratio = deltaT / _originalDt;
        final int newStartIndex = Math.min(Math.max(0, (int) (_zeigerYOUT / ratio)), youtVerzoegertNew.length - 1);

        for (int i = 0; i < youtVerzoegertNew.length; i++) {
            try {
                youtVerzoegertNew[i] = _youtVerzoegert[(int) (ratio * i)];
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        _zeigerYOUT = newStartIndex;
        _youtVerzoegert = youtVerzoegertNew;
    }

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        _delayTime = _initDelayTime;
        _originalDt = deltaT;
        _speicherLeer = true;
        _youtVerzoegert = new double[(int) (_delayTime / deltaT)];
        _zeigerYOUT = 0;

    }

    @Override
    public void berechneYOUT(final double deltaT) {
        if (_speicherLeer) {  // Speicher initial auffuellen
            if (deltaT > _delayTime) { // minimal delay, just feed the input signal to the output!
                _outputSignal[0][0] = _inputSignal[0][0];
                return;
            }
            _outputSignal[0][0] = 0;
            _youtVerzoegert[_zeigerYOUT] = _inputSignal[0][0];
            _zeigerYOUT++;
            if (_zeigerYOUT == _youtVerzoegert.length) {
                _speicherLeer = false;
                _zeigerYOUT = 0;
            }

        } else {
            _outputSignal[0][0] = _youtVerzoegert[_zeigerYOUT];
            _youtVerzoegert[_zeigerYOUT] = _inputSignal[0][0];  // laufendes Nachfuellen des Speichers
            _zeigerYOUT++;
            _zeigerYOUT %= _youtVerzoegert.length; // Zeiger laeuft 'im Kreis', damit wird verhindert, dass die 
            // Daten im Speicher bei jedem Zeitschritt geshiftet werden muessen
        }
    }

    public void setDelayTime(final double delayTime) {
        if (delayTime < 0) {
            throw new IllegalArgumentException("Error: Delay time must be positive!");
        }
        _initDelayTime = delayTime;
    }
}
