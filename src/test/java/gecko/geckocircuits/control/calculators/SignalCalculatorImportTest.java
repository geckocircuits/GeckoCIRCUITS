/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  Foobar is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.geckocircuits.control.calculators;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class SignalCalculatorImportTest {

    private final double DELTA_T = 0.0123;
    final double[][] testTable = new double[][]{
        new double[]{0.0, 2.17E-4, 5.0E-4, 6.0E-4, 8.0E-4, 0.0010, 0.0011, 0.0013, 0.0040, 0.0040015, 0.0050, 0.0050015, 0.0060, 0.006003, 0.0070, 0.007003, 0.0080, 0.008003, 0.0090, 0.0090015, 0.01, 0.0100015, 0.011, 0.011003, 0.012, 0.012003, 0.013},
        new double[]{0.0, 20.0, 34.0, 36.0, 38.5, 39.5, 39.7, 40.0, 40.0, 20.0, 20.0, 40.0, 40.0, 0.5, 0.5, 40.0, 40.0, 15.0, 15.0, 25.0, 25.0, 40.0, 40.0, 4.0, 4.0, 40.0, 40.0}
    };
    private double signalDuration;
    private SignalCalculatorImport _signalCalculator;

    @Before
    public void setUp() {
        _signalCalculator = new SignalCalculatorImport(testTable);
        _signalCalculator.initializeAtSimulationStart(DELTA_T);
    }

    @Test
    public void testBerechneYOUT() {
        for (double time = 0; time < 0.03; time += DELTA_T) {
            AbstractControlCalculatable.setTime(time);
            _signalCalculator.berechneYOUT(DELTA_T);
            final double result = _signalCalculator._outputSignal[0][0];

            if (time == 0) {
                assertEquals(0, result, 1e-9);
            }

            if (time == 0.0123) {
                assertEquals(40, result, 1e-9);
            }

            if (time == 0.0246) {
                assertEquals(4.0, result, 1e-9);
            }
        }
    }

    @Test(expected=Throwable.class)
    public void testNullData() {
            new SignalCalculatorImport(null);
    }

    @Test(expected=Exception.class)
    public void testCorruptedData() {
        final double[][] wrongData = new double[][]{new double[]{0,1,3,2}, new double[]{0,0,0,0}};
            SignalCalculatorImport sci = new SignalCalculatorImport(wrongData);
            sci.initializeAtSimulationStart(DELTA_T);
    }
}
