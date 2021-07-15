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
package ch.technokrat.gecko.geckocircuits.control.calculators;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class ThyristorControlCalculatorTest {

    private static final double DELTA_T = 1000e-9;
    private static final double END_TIME = 39e-3;
    private ThyristorControlCalculator calculator;
    private static final double PHASE = 30;

    @Before
    public void setUp() {
        calculator = new ThyristorControlCalculator(PHASE, 50, 4e-3);
        calculator._inputSignal[0] = new double[1];
        calculator._inputSignal[1] = new double[1];
    }

    @Test
    public void testBerechneYOUT() {

        calculator._inputSignal[0][0] = 30; // phase
        int[] eventCounter = new int[6];
        
        for (double time = 0; time < END_TIME; time += DELTA_T) {
            AbstractControlCalculatable.setTime(time);
            double[] oldOutput = new double[6];
            for(int i = 0; i < 6; i++) {
                oldOutput[i] = calculator._outputSignal[i][0];
            }
            calculator._inputSignal[1][0] = 100 * Math.sin(2 * Math.PI * time / 50);
            calculator.berechneYOUT(DELTA_T);
            for(int i = 0; i < 6; i++) {
                if(oldOutput[i] != calculator._outputSignal[i][0]) {
                    eventCounter[i]++;
                }
            }
        }

        assertEquals(2, eventCounter[0]);
        assertEquals(4, eventCounter[1]);
        assertEquals(4, eventCounter[2]);        
        assertEquals(4, eventCounter[3]);
        assertEquals(2, eventCounter[4]);
        assertEquals(4, eventCounter[5]);
    }
}
