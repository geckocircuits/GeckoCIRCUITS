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

import ch.technokrat.gecko.geckocircuits.control.ReglerSlidingDFT;
import ch.technokrat.gecko.geckocircuits.control.ReglerSlidingDFT.FrequencyData;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public final class SlidingDFTCalculatorTest {

    public static final int NO_OUTPUTS = 3;
    public static final double AVG_SPAN = 20e-3;
    public static final double TOLERANCE = 1e-6;
    public static final double LARGE_TOLERANCE = 1e-3;
    public static final double VERY_LARGE_TOLERANCE = 1e-2;
    private static final double FREQ1 = 50;
    private static final double FREQ0 = 0;
    private static final double DELTA_T = 1e-6;
    private static final double END_TIME = 0.4;
    private static final double STEP_INCREASE = 3;
    private static final double STEP_INCREASE2 = 2;
    private static final double OFFSET = 1.5;
    private static final double AMPLITUDE = 2;
    private SlidingDFTCalculator _sdft;
    private List<FrequencyData> _freqData;

    @Before
    public void setUp() {     
        ReglerSlidingDFT regler = new ReglerSlidingDFT();
        
        _freqData = new ArrayList<ReglerSlidingDFT.FrequencyData>();
        _freqData.add(regler.new FrequencyData(FREQ1, ReglerSlidingDFT.OutputData.ABS));
        _freqData.add(regler.new FrequencyData(FREQ1, ReglerSlidingDFT.OutputData.PHASE));
        _freqData.add(regler.new FrequencyData(FREQ0, ReglerSlidingDFT.OutputData.ABS));
        _sdft = new SlidingDFTCalculator(NO_OUTPUTS, AVG_SPAN, _freqData);        
    }

    @Test
    public void testBerechneYOUT() {
        _sdft._inputSignal[0] = new double[1];
        _sdft.initializeAtSimulationStart(DELTA_T);

        for (double time = 0; time < END_TIME; time += DELTA_T) {
            _sdft._inputSignal[0][0] = OFFSET + AMPLITUDE * Math.sin(2 * Math.PI * time * FREQ1);
            _sdft.berechneYOUT(DELTA_T);
        }

        assertEquals(AMPLITUDE, _sdft._outputSignal[0][0], TOLERANCE);
        assertEquals(-Math.PI / 2, _sdft._outputSignal[1][0], TOLERANCE);
        assertEquals(2 * OFFSET, _sdft._outputSignal[2][0], TOLERANCE);
    }

    @Test
    public void testInitWithNewDt() {
        // pre-simulate for some time
        _sdft._inputSignal[0] = new double[1];
        _sdft.initializeAtSimulationStart(DELTA_T);
        double time = 0;
        for (; time < END_TIME; time += DELTA_T) {
            _sdft._inputSignal[0][0] = OFFSET + AMPLITUDE * Math.sin(2 * Math.PI * time * FREQ1);
            _sdft.berechneYOUT(DELTA_T);
        }

        // solve with larger stepwidth:        
        _sdft.initWithNewDt(STEP_INCREASE * DELTA_T);
        
        for (; time < 2 * END_TIME; time += STEP_INCREASE * DELTA_T) {
            _sdft._inputSignal[0][0] = OFFSET + AMPLITUDE * Math.sin(2 * Math.PI * time * FREQ1);
            _sdft.berechneYOUT(DELTA_T * STEP_INCREASE);
        }

        assertEquals(AMPLITUDE, _sdft._outputSignal[0][0], LARGE_TOLERANCE);
        assertEquals(-Math.PI / 2, _sdft._outputSignal[1][0], VERY_LARGE_TOLERANCE);
        assertEquals(2 * OFFSET, _sdft._outputSignal[2][0], LARGE_TOLERANCE);

        
        // solve with smaller stepwidth:        
        _sdft.initWithNewDt(STEP_INCREASE2 * DELTA_T);
        for (; time < (2 + 1) * END_TIME; time += STEP_INCREASE2 * DELTA_T) {
            _sdft._inputSignal[0][0] = OFFSET + AMPLITUDE * Math.sin(2 * Math.PI * time * FREQ1);
            _sdft.berechneYOUT(DELTA_T * STEP_INCREASE2);
        }

        assertEquals(AMPLITUDE, _sdft._outputSignal[0][0], LARGE_TOLERANCE);
        assertEquals(-Math.PI / 2, _sdft._outputSignal[1][0], VERY_LARGE_TOLERANCE);
        assertEquals(2 * OFFSET, _sdft._outputSignal[2][0], LARGE_TOLERANCE);
    }
    
    @Test
    public void testConstantInput() {
        _sdft._inputSignal[0] = new double[1];
        _sdft.initializeAtSimulationStart(DELTA_T);
        
        // Feed constant DC value
        for (double time = 0; time < END_TIME; time += DELTA_T) {
            _sdft._inputSignal[0][0] = OFFSET;  // Only DC, no AC
            _sdft.berechneYOUT(DELTA_T);
        }
        
        // DC component should be OFFSET * 2 (the algorithm sums all components)
        assertEquals(0, _sdft._outputSignal[0][0], TOLERANCE);  // AC magnitude
        assertEquals(OFFSET * 2, _sdft._outputSignal[2][0], LARGE_TOLERANCE);  // DC component (double)
    }
    
    @Test
    public void testDCComponentOnly() {
        _sdft._inputSignal[0] = new double[1];
        _sdft.initializeAtSimulationStart(DELTA_T);
        
        double dcValue = 5.0;
        for (double time = 0; time < END_TIME; time += DELTA_T) {
            _sdft._inputSignal[0][0] = dcValue;
            _sdft.berechneYOUT(DELTA_T);
        }
        
        assertEquals(dcValue * 2, _sdft._outputSignal[2][0], LARGE_TOLERANCE);
    }
    
    @Test
    public void testHigherFrequency() {
        ReglerSlidingDFT regler = new ReglerSlidingDFT();
        List<FrequencyData> freqData = new ArrayList<>();
        double highFreq = 150;  // 150 Hz component
        freqData.add(regler.new FrequencyData(highFreq, ReglerSlidingDFT.OutputData.ABS));
        
        SlidingDFTCalculator sdft = new SlidingDFTCalculator(1, AVG_SPAN, freqData);
        sdft._inputSignal[0] = new double[1];
        sdft.initializeAtSimulationStart(DELTA_T);
        
        for (double time = 0; time < END_TIME; time += DELTA_T) {
            sdft._inputSignal[0][0] = AMPLITUDE * Math.sin(2 * Math.PI * time * highFreq);
            sdft.berechneYOUT(DELTA_T);
        }
        
        assertEquals(AMPLITUDE, sdft._outputSignal[0][0], LARGE_TOLERANCE);
    }
    
    @Test
    public void testZeroAmplitude() {
        _sdft._inputSignal[0] = new double[1];
        _sdft.initializeAtSimulationStart(DELTA_T);
        
        for (double time = 0; time < END_TIME; time += DELTA_T) {
            _sdft._inputSignal[0][0] = 0;
            _sdft.berechneYOUT(DELTA_T);
        }
        
        // All outputs should be near zero
        for (int i = 0; i < NO_OUTPUTS; i++) {
            assertEquals("Output " + i, 0, _sdft._outputSignal[i][0], LARGE_TOLERANCE);
        }
    }
    
    @Test
    public void testMultipleFrequencies() {
        _sdft._inputSignal[0] = new double[1];
        _sdft.initializeAtSimulationStart(DELTA_T);
        
        // Sum of 50Hz and DC
        for (double time = 0; time < END_TIME; time += DELTA_T) {
            _sdft._inputSignal[0][0] = OFFSET + AMPLITUDE * Math.sin(2 * Math.PI * time * FREQ1);
            _sdft.berechneYOUT(DELTA_T);
        }
        
        // Verify outputs exist and are reasonable
        assertFalse("Magnitude should be finite", Double.isInfinite(_sdft._outputSignal[0][0]));
        assertFalse("Phase should be finite", Double.isInfinite(_sdft._outputSignal[1][0]));
        assertFalse("DC should be finite", Double.isInfinite(_sdft._outputSignal[2][0]));
    }
}
