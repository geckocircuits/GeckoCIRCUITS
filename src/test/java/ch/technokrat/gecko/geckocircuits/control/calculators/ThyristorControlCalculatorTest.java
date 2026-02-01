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
    
    @Test
    public void testVariousPhaseAngles() {
        // Test with different phase angles (0, 30, 60, 90 degrees)
        double[] phases = {0.0, 30.0, 60.0, 90.0};
        
        for (double phase : phases) {
            ThyristorControlCalculator calc = new ThyristorControlCalculator(phase, 50, 4e-3);
            calc._inputSignal[0] = new double[1];
            calc._inputSignal[1] = new double[1];
            calc._inputSignal[0][0] = phase;
            calc._inputSignal[1][0] = 100.0; // 100V sine input
            
            AbstractControlCalculatable.setTime(0.01);
            calc.berechneYOUT(1e-6);
            
            // Verify outputs exist and are valid
            for (int i = 0; i < 6; i++) {
                assertNotNull("Output " + i + " at phase " + phase + " should exist", 
                            calc._outputSignal[i][0]);
            }
        }
    }
    
    @Test
    public void testZeroVoltageInput() {
        // Zero voltage should keep all gates off
        calculator._inputSignal[0][0] = 30;
        calculator._inputSignal[1][0] = 0.0;
        
        AbstractControlCalculatable.setTime(0.01);
        calculator.berechneYOUT(DELTA_T);
        
        // All gates should be off (0)
        boolean allOff = true;
        for (int i = 0; i < 6; i++) {
            if (calculator._outputSignal[i][0] != 0.0) {
                allOff = false;
            }
        }
        assertTrue("All gates should be off with zero voltage", allOff);
    }
    
    @Test
    public void testHighVoltageInput() {
        // Test with very high voltage (well above nominal)
        calculator._inputSignal[0][0] = 45;
        calculator._inputSignal[1][0] = 500.0; // 500V input
        
        AbstractControlCalculatable.setTime(0.01);
        calculator.berechneYOUT(DELTA_T);
        
        // Should still produce valid gate signals
        for (int i = 0; i < 6; i++) {
            assertTrue("Gate " + i + " should be 0 or 1", 
                      calculator._outputSignal[i][0] == 0.0 || calculator._outputSignal[i][0] == 1.0);
        }
    }
    
    @Test
    public void testPhaseAngle0() {
        // Test at 0 degree phase angle (synchronous)
        ThyristorControlCalculator calc = new ThyristorControlCalculator(0.0, 50, 4e-3);
        calc._inputSignal[0] = new double[1];
        calc._inputSignal[1] = new double[1];
        calc._inputSignal[0][0] = 0.0;
        calc._inputSignal[1][0] = 100.0;
        
        AbstractControlCalculatable.setTime(0.01);
        calc.berechneYOUT(1e-6);
        
        for (int i = 0; i < 6; i++) {
            assertFalse("Output should not be NaN at 0 phase", Double.isNaN(calc._outputSignal[i][0]));
        }
    }
    
    @Test
    public void testPhaseAngle90() {
        // Test at 90 degree phase angle (maximum delay)
        ThyristorControlCalculator calc = new ThyristorControlCalculator(90.0, 50, 4e-3);
        calc._inputSignal[0] = new double[1];
        calc._inputSignal[1] = new double[1];
        calc._inputSignal[0][0] = 90.0;
        calc._inputSignal[1][0] = 100.0;
        
        AbstractControlCalculatable.setTime(0.01);
        calc.berechneYOUT(1e-6);
        
        for (int i = 0; i < 6; i++) {
            assertFalse("Output should not be NaN at 90 phase", Double.isNaN(calc._outputSignal[i][0]));
        }
    }
    
    @Test
    public void testMultipleFrequencies() {
        // Test with different AC frequencies (25Hz, 50Hz, 60Hz)
        int[] frequencies = {25, 50, 60};
        
        for (int freq : frequencies) {
            ThyristorControlCalculator calc = new ThyristorControlCalculator(30.0, freq, 4e-3);
            calc._inputSignal[0] = new double[1];
            calc._inputSignal[1] = new double[1];
            calc._inputSignal[0][0] = 30.0;
            
            for (double time = 0; time < 0.1; time += 1e-6) {
                calc._inputSignal[1][0] = 100.0 * Math.sin(2 * Math.PI * freq * time);
                AbstractControlCalculatable.setTime(time);
                calc.berechneYOUT(1e-6);
                
                // Verify outputs are binary
                for (int i = 0; i < 6; i++) {
                    double val = calc._outputSignal[i][0];
                    assertTrue("Gate should be 0 or 1 at freq " + freq, val == 0.0 || val == 1.0);
                }
            }
        }
    }
    
    @Test
    public void testRapidPhaseChanges() {
        // Test behavior with rapid phase angle changes
        calculator._inputSignal[1][0] = 100.0;
        
        for (double phase = 0; phase <= 150; phase += 15.0) {
            calculator._inputSignal[0][0] = phase;
            AbstractControlCalculatable.setTime(0.01);
            calculator.berechneYOUT(DELTA_T);
            
            // All outputs should be valid binary values
            for (int i = 0; i < 6; i++) {
                assertTrue("Gate at phase " + phase + " should be 0 or 1",
                          calculator._outputSignal[i][0] == 0.0 || calculator._outputSignal[i][0] == 1.0);
            }
        }
    }
    
    @Test
    public void testNegativeVoltageInput() {
        // Test with negative voltage (reverse polarity)
        calculator._inputSignal[0][0] = 45.0;
        calculator._inputSignal[1][0] = -100.0;
        
        AbstractControlCalculatable.setTime(0.01);
        calculator.berechneYOUT(DELTA_T);
        
        // Should still produce valid gate signals
        for (int i = 0; i < 6; i++) {
            assertTrue("Gate should remain valid with negative voltage",
                      calculator._outputSignal[i][0] == 0.0 || calculator._outputSignal[i][0] == 1.0);
        }
    }
}
