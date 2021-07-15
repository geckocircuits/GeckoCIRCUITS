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

public final class SignalCalculatorExternalWrapperTest {    
    private SignalCalculatorExternalWrapper _signalCalcWrapper;
    private SignalCalculatorRectangle _wrapped;       
    private static final double END_TIME = 1;
    private static final double DELTA_T = 0.001;
    private static final double SWITCHING_TIME1 = 0.03300000000000002;
    private static final double SWITCHING_TIME2 = 0.1330000000000001;
    private static final double TOLERANCE = 1e-6;
    
    @Before
    public void setUp() {
        /// the input arguments are not used, anyway!
        _wrapped = new SignalCalculatorRectangle(5, 0, 0, 0, 0, 0);
        _signalCalcWrapper = new SignalCalculatorExternalWrapper(_wrapped);
        for(int i = 0; i < 5; i++) {
            _signalCalcWrapper._inputSignal[i] = new double[1];
        }
        
        _signalCalcWrapper.initializeAtSimulationStart(DELTA_T);
    }        
    
    @Test
    public void testBerechneYOUT() {
        _signalCalcWrapper.setAmplitudeAC(2);
        _signalCalcWrapper.setAnteilDC(3);
        _signalCalcWrapper.setDuty(0.3);
        _signalCalcWrapper.setFrequency(3);
        _signalCalcWrapper.setPhase(-Math.PI);
        
        for(double time = 0; time < END_TIME/2; time+=DELTA_T) {
            double oldValue = _wrapped._outputSignal[0][0];
            _signalCalcWrapper.berechneYOUT(DELTA_T);
            double value = _signalCalcWrapper._outputSignal[0][0];
            if(value != oldValue) {
                //System.out.println("swithcing time: " + time);                
            }
            if(time == SWITCHING_TIME1) {
                assertEquals(3, oldValue, TOLERANCE);
                assertEquals(5, value, TOLERANCE);
            }            
        }
        
        _signalCalcWrapper.setDuty(0.5);
        _signalCalcWrapper.setAnteilDC(5);
        _signalCalcWrapper.setAmplitudeAC(3);
        for(double time = END_TIME/2; time < END_TIME; time+=DELTA_T) {
            double oldValue = _signalCalcWrapper._outputSignal[0][0];
            _signalCalcWrapper.berechneYOUT(DELTA_T);
            double value = _signalCalcWrapper._outputSignal[0][0];
            
            if(time == SWITCHING_TIME2) {
                assertEquals(5, oldValue, TOLERANCE);
                assertEquals(8, value, TOLERANCE);
            }
        }
        
    }        
}
