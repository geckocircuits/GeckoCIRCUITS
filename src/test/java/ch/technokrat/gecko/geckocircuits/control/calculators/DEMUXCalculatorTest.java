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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.technokrat.gecko.geckocircuits.control.calculators;

import ch.technokrat.gecko.geckocircuits.control.ReglerDemux;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andy
 */
public class DEMUXCalculatorTest {
    private static final int NO_OUPUTS = 5;
    
    private DEMUXCalculator _demux;
    private ReglerDemux _reglerDemux;
    
    
    @Before
    public void setUp() {
        _reglerDemux = new ReglerDemux();
        _demux = new DEMUXCalculator(NO_OUPUTS, _reglerDemux);
        _demux._inputSignal[0] = new double[NO_OUPUTS];
    }

    @Test
    public void testBerechneYOUT() {
        Random rand = new Random();
        
        for(int i = 0; i < _demux._outputSignal.length; i++) {
            _demux._inputSignal[0][i] = rand.nextDouble();
        }
                    
        _demux.berechneYOUT(1);
        
        for(int i = 0; i < _demux._outputSignal.length; i++) {
            assertEquals(_demux._inputSignal[0][i], _demux._outputSignal[i][0], 1e-9);
        }
    }

    @Test(expected=Exception.class)
    public void testInitializeAtSimulationStart() {       
        DEMUXCalculator demux = new DEMUXCalculator(NO_OUPUTS, _reglerDemux);
        // the +1 should produce an error!
        demux._inputSignal[0] = new double[NO_OUPUTS+1];
        demux.initializeAtSimulationStart(1);        
    }
    
    @Test
    public void testZeroInputValues() {
        for (int i = 0; i < _demux._inputSignal[0].length; i++) {
            _demux._inputSignal[0][i] = 0.0;
        }
        
        _demux.berechneYOUT(1);
        
        for (int i = 0; i < _demux._outputSignal.length; i++) {
            assertEquals("Output " + i + " should be zero", 0.0, _demux._outputSignal[i][0], 1e-9);
        }
    }
    
    @Test
    public void testNegativeInputValues() {
        for (int i = 0; i < _demux._inputSignal[0].length; i++) {
            _demux._inputSignal[0][i] = -1.0 - i;  // -1, -2, -3, -4, -5
        }
        
        _demux.berechneYOUT(1);
        
        for (int i = 0; i < _demux._outputSignal.length; i++) {
            assertEquals("Output " + i + " should be negative", 
                        -1.0 - i, _demux._outputSignal[i][0], 1e-9);
        }
    }
    
    @Test
    public void testMixedSignInputValues() {
        _demux._inputSignal[0][0] = 1.5;
        _demux._inputSignal[0][1] = -2.5;
        _demux._inputSignal[0][2] = 0.0;
        _demux._inputSignal[0][3] = -0.5;
        _demux._inputSignal[0][4] = 3.3;
        
        _demux.berechneYOUT(1);
        
        assertEquals(1.5, _demux._outputSignal[0][0], 1e-9);
        assertEquals(-2.5, _demux._outputSignal[1][0], 1e-9);
        assertEquals(0.0, _demux._outputSignal[2][0], 1e-9);
        assertEquals(-0.5, _demux._outputSignal[3][0], 1e-9);
        assertEquals(3.3, _demux._outputSignal[4][0], 1e-9);
    }
    
    @Test
    public void testLargeValues() {
        for (int i = 0; i < _demux._inputSignal[0].length; i++) {
            _demux._inputSignal[0][i] = 1e6 + i;
        }
        
        _demux.berechneYOUT(1);
        
        for (int i = 0; i < _demux._outputSignal.length; i++) {
            assertEquals("Output " + i + " should preserve large value",
                        1e6 + i, _demux._outputSignal[i][0], 1e-9);
        }
    }
    
    @Test
    public void testSmallDecimalValues() {
        for (int i = 0; i < _demux._inputSignal[0].length; i++) {
            _demux._inputSignal[0][i] = 1e-6 * (i + 1);
        }
        
        _demux.berechneYOUT(1);
        
        for (int i = 0; i < _demux._outputSignal.length; i++) {
            assertEquals("Output " + i + " should preserve small value",
                        1e-6 * (i + 1), _demux._outputSignal[i][0], 1e-12);
        }
    }
    
    @Test
    public void testAlternatingSignPattern() {
        // Alternating positive and negative pattern
        for (int i = 0; i < _demux._inputSignal[0].length; i++) {
            _demux._inputSignal[0][i] = (i % 2 == 0) ? 5.5 : -5.5;
        }
        
        _demux.berechneYOUT(1);
        
        for (int i = 0; i < _demux._outputSignal.length; i++) {
            double expected = (i % 2 == 0) ? 5.5 : -5.5;
            assertEquals("Alternating pattern should be preserved", expected, _demux._outputSignal[i][0], 1e-9);
        }
    }
    
    @Test
    public void testConsecutiveCalculations() {
        // Test that multiple consecutive calculations work correctly
        double[] input1 = {1.0, 2.0, 3.0, 4.0, 5.0};
        double[] input2 = {5.0, 4.0, 3.0, 2.0, 1.0};
        
        // First calculation
        System.arraycopy(input1, 0, _demux._inputSignal[0], 0, NO_OUPUTS);
        _demux.berechneYOUT(1);
        for (int i = 0; i < _demux._outputSignal.length; i++) {
            assertEquals("First calculation output " + i, input1[i], _demux._outputSignal[i][0], 1e-9);
        }
        
        // Second calculation
        System.arraycopy(input2, 0, _demux._inputSignal[0], 0, NO_OUPUTS);
        _demux.berechneYOUT(1);
        for (int i = 0; i < _demux._outputSignal.length; i++) {
            assertEquals("Second calculation output " + i, input2[i], _demux._outputSignal[i][0], 1e-9);
        }
    }
    
    @Test
    public void testVeryLargeValues() {
        // Test with maximum safe double values
        for (int i = 0; i < _demux._inputSignal[0].length; i++) {
            _demux._inputSignal[0][i] = 1e300;
        }
        
        _demux.berechneYOUT(1);
        
        for (int i = 0; i < _demux._outputSignal.length; i++) {
            assertEquals("Output " + i + " should preserve very large value",
                        1e300, _demux._outputSignal[i][0], 1e290);
        }
    }
}
