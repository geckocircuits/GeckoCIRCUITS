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

public final class SignalCalculatorTriangleTest {
    private static final int NO_INPUTS = 1;
    
    /**
     * WARNING: giving a negative amplitude makes probems in this test.
     * Then, the output signal is just zero? but in the simulation model,
     * the triangle still works, even with a negative amplude... strange. Andy.
     */
    private static final double AMPL = 2;
    private static final double FREQUENCY = 3;
    private static final double PHASE = -Math.PI/2;
    private static final double DC_OFFSET = 3;
    private static final double DUTY = 0.6;
    private SignalCalculatorTriangle _signalCalc;
    private static final double DELTA_T = 1e-4;
    private static final double END_TIME = 1; // sec
    private static final double TOLERANCE = 1e-6;
    private static final double EXPECTED_MIN = 1;
    private static final double EXPECTED_MAX = 5;
    private static final double EXPECTED_END_VAL = 4.664;
    
    @Before
    public void setUp() {
        _signalCalc = new SignalCalculatorTriangle(NO_INPUTS, AMPL, FREQUENCY, PHASE, DC_OFFSET, DUTY);
        _signalCalc.initializeAtSimulationStart(DELTA_T);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void negativeAmpltudeError() {
        _signalCalc = new SignalCalculatorTriangle(NO_INPUTS, -AMPL, FREQUENCY, PHASE, DC_OFFSET, DUTY);
    }
    
    @Test
    public void testBerechneYOUT() {           
        double maxValue = -Double.MAX_VALUE;
        double minValue = Double.MAX_VALUE;
                                
        for(double time = 0; time < END_TIME; time+= DELTA_T) {
            AbstractSignalCalculator.setTime(time);
            _signalCalc.berechneYOUT(DELTA_T);
            final double result = _signalCalc._outputSignal[0][0];
            maxValue = Math.max(result, maxValue);
            minValue = Math.min(result, minValue);            
        }
        assertEquals(EXPECTED_END_VAL, _signalCalc._outputSignal[0][0], TOLERANCE);
        assertEquals(EXPECTED_MIN, minValue, TOLERANCE);
        assertEquals(EXPECTED_MAX, maxValue, TOLERANCE);        
    }    
}
