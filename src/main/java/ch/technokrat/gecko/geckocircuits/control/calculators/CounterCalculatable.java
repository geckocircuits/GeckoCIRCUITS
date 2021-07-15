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

/**
 * Der internen Zaehler kann durch den zweiten (unteren) RESET-Eingang auf '0' zurueckgesetzt werden:
 * solange der RESET-Eingang auf Null steht, ist der interne Zaehle auf Null gesetzt,
 * sobald der RESET-Eingang auf Eins gesetzt wird, beginnt der interne Zaehler zu laufen und wird direkt am Ausgang ausgegeben
 * @author andreas
 */
public final class CounterCalculatable extends AbstractTwoInputsOneOutputCalculator {
    private double _lastValue = 0;            

    @Override
    public void berechneYOUT(final double deltaT) {
        if ((_inputSignal[0][0] >= SIGNAL_THRESHOLD) && (_lastValue < SIGNAL_THRESHOLD)) {
            _outputSignal[0][0]++;
        }
        if (_inputSignal[1][0] > SIGNAL_THRESHOLD) {
            _outputSignal[0][0] = 0;  // Logik-Schwelle --> 0.5;  RESET bei Input '1' (somit braucht man 
            // den Anschluss nicht extra mit einem const=1 - Block belegen, damit der Counter laeuft)
        }
        _lastValue = _inputSignal[0][0];
        
    }
}
