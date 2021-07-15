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

import java.util.Random;
import static org.junit.Assert.*;
import org.junit.Test;

// these tests works the following way: the DQ-ABCD and ABCD-DQ functions
// should be inverses (careful: only in the drection dqABC->ABDdq).
// Therefore, test Both bloc by executing them subequently! The idea is:
// when one of them breaks, the result will not be valid in any case.
public class DQABCDCalculatorTest {

    @Test
    public void testForwardInverse() {

        DQABCDCalculator dqABC = new DQABCDCalculator();
        ABCDQCalculator ABCdq = new ABCDQCalculator();
        double[] theta = new double[1];

        double[] a = dqABC._outputSignal[0];
        double[] b = dqABC._outputSignal[1];
        double[] c = dqABC._outputSignal[2];

        ABCdq._inputSignal[0] = a;
        ABCdq._inputSignal[1] = b;
        ABCdq._inputSignal[2] = c;
        ABCdq._inputSignal[3] = theta;
        for (int iterator = 0; iterator < 5; iterator++) {
            for (int i = 0; i < 3; i++) {
                dqABC._inputSignal[i] = new double[1];
            }
            dqABC._inputSignal[2] = theta;

            Random random = new Random();
            for (int j = 0; j < 3; j++) { // set some random input!
                dqABC._inputSignal[j][0] = random.nextDouble();
            }

            dqABC.berechneYOUT(1e-8);
            ABCdq.berechneYOUT(1e-8);
            // compare the output with input. should be identical, since 
            // the functions are inverse to each other!
            for (int j = 0; j < 2; j++) {
                assertEquals(dqABC._inputSignal[j][0], ABCdq._outputSignal[j][0], 1e-8);
            }
        }
    }
}
