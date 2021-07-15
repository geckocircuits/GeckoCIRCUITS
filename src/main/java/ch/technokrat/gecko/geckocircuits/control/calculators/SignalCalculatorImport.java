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


public final class SignalCalculatorImport extends AbstractSignalCalculator implements InitializableAtSimulationStart {
    // Periode des Signals --> wichtig, wenn Repeat==ON / tSigStart ... lokale Zeit innerhalb der importierten Signal-Periode

    private final double _signalDuration;
    private double _tSigStart = 0;
    private final double[][] _xy;

    public SignalCalculatorImport(final double[][] dataTable) {
        super(0);
        assert dataTable != null;
        _xy = new double[dataTable.length][dataTable[0].length];
        for(int i = 0; i < dataTable.length; i++) {
            System.arraycopy(dataTable[i], 0, _xy[i], 0, dataTable[0].length);
        }
        
        // Periode (Wiederholrate) des Signals;                        
        _signalDuration = dataTable[0][dataTable[0].length - 1] - dataTable[0][0];  
    }

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        while (_tSigStart + _signalDuration < _time) {
            _tSigStart += _signalDuration;
        }
        doDataTests();
    }

    @Override
    public void berechneYOUT(final double deltaT) {

        if (_tSigStart > _time) {
            _tSigStart = 0;  // zB. bei Neustarten der Simulation
        }

        calculateSigStartTimeEstimation();
                        // ungefaehre Position bestimmen:
        
        final int timePointer = calculateAccurateTimePointer();
        if (timePointer == 0) {
            _outputSignal[0][0] = _xy[1][0];
        } else if (_tSigStart + _xy[0][timePointer] >= _time) {
            final double time1 = _xy[0][timePointer - 1];
            final double time2 = _xy[0][timePointer];
            final double value1 = _xy[1][timePointer - 1];
            final double value2 = _xy[1][timePointer];
            _outputSignal[0][0] = value1 + (value2 - value1) * (_time - _tSigStart - time1) / (time2 - time1);
        } else {
            final double time1 = _xy[0][timePointer];
            final double time2 = _xy[0][timePointer + 1];
            final double value1 = _xy[1][timePointer];
            final double value2 = _xy[1][timePointer + 1];

            _outputSignal[0][0] = value1 + (value2 - value1) * (_time - _tSigStart - time1) / (time2 - time1);
        }
    }

    private void doDataTests() {
        if(_signalDuration <= 0) {
            throw new IllegalArgumentException("The data table in the signal data file is invalid.");
        }
        
        for(int i = 1; i < _xy[0].length; i++) {
            final double newTime = _xy[0][i];
            final double previousTime = _xy[0][i-1];
            if(newTime <= previousTime) {
                throw new IllegalArgumentException("The data table in the signal data file has an error:\n"
                                         + "The time values are not strictly monotonicaly increasing!\n"
                                         + "Error values: " + previousTime + " " + newTime);
            }            
        }
    }

    private void calculateSigStartTimeEstimation() {
        while (_tSigStart + _signalDuration < _time) {
            _tSigStart += _signalDuration;  // tLokal 'zeigt' immer auf den Zeitpunkt des Beginns einer Signal-Periode
        }
    }

    private int calculateAccurateTimePointer() {
        int timePointer = (int) (_xy[0].length * ((_time - _tSigStart) / _signalDuration));
        // Feinadjustierung: jetzt die exakte Posistion bestimmen
        if (_tSigStart + _xy[0][timePointer] < _time) {
            while ((timePointer < _xy[0].length - 1) && (_tSigStart + _xy[0][timePointer] < _time)) {
                timePointer++;  // Erg. -->  xy[0][zeiger] >= t  oder  zeiger ==> xy[0][end]
            }
        } else {
            while ((timePointer > 0) && (_tSigStart + _xy[0][timePointer] > _time)) {
                timePointer--;  // Erg. -->  xy[0][zeiger] <= t  oder  zeiger ==> xy[0][0]
            }
        }
        return timePointer;
    }
}
