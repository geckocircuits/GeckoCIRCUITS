package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for TimeCalculator - outputs the simulation time.
 */
public class TimeCalculatorTest {
    
    private TimeCalculator calculator;
    
    @Before
    public void setUp() {
        calculator = new TimeCalculator();
        // Initialize output signal array
        calculator._outputSignal[0] = new double[]{0};
    }
    
    @Test
    public void testConstructor() {
        assertEquals("Should have 0 inputs", 0, calculator._inputSignal.length);
        assertEquals("Should have 1 output", 1, calculator._outputSignal.length);
    }
    
    @Test
    public void testTimeOutput() {
        // Set time to 0
        AbstractControlCalculatable.setTime(0.0);
        calculator.berechneYOUT(0.001);
        assertEquals("Time should be 0 at start", 0.0, calculator._outputSignal[0][0], 1e-10);
    }
    
    @Test
    public void testTimeAccumulation() {
        // Simulate multiple timesteps with increasing time
        for (int i = 0; i < 10; i++) {
            double time = (i + 1) * 0.001;
            AbstractControlCalculatable.setTime(time);
            calculator.berechneYOUT(0.001);
            assertEquals("Time accumulation at step " + i, time, 
                        calculator._outputSignal[0][0], 1e-10);
        }
    }
    
    @Test
    public void testLargeTimeValues() {
        // Test with large time value (e.g., 1000 seconds)
        AbstractControlCalculatable.setTime(1000.0);
        calculator.berechneYOUT(0.001);
        assertEquals("Should handle large time values", 1000.0, 
                    calculator._outputSignal[0][0], 1e-10);
    }
}
