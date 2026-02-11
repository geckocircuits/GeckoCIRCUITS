package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for LimitCalculator - Signal limiter/clamp with upper and lower bounds.
 * Tests internal limiter with fixed bounds.
 */
public class LimitCalculatorTest {
    
    private LimitCalculatorInternal internalLimiter;
    private LimitCalculatorExternal externalLimiter;
    private static final double TOLERANCE = 1e-10;
    
    @Before
    public void setUp() {
        internalLimiter = new LimitCalculatorInternal(-10.0, 10.0);
        externalLimiter = new LimitCalculatorExternal();
        
        // Initialize input/output arrays
        internalLimiter._inputSignal[0] = new double[]{0};
        internalLimiter._outputSignal[0] = new double[]{0};
        
        externalLimiter._inputSignal[0] = new double[]{0};
        externalLimiter._inputSignal[1] = new double[]{0};
        externalLimiter._inputSignal[2] = new double[]{0};
        externalLimiter._outputSignal[0] = new double[]{0};
    }
    
    @Test
    public void testInternalLimiterUpperClamp() {
        // Test upper limit clamping
        internalLimiter._inputSignal[0][0] = 15.0;  // Above limit of 10.0
        internalLimiter.berechneYOUT(0.001);
        assertEquals("Should clamp to upper limit", 10.0, 
                    internalLimiter._outputSignal[0][0], TOLERANCE);
    }
    
    @Test
    public void testInternalLimiterLowerClamp() {
        // Test lower limit clamping
        internalLimiter._inputSignal[0][0] = -15.0;  // Below limit of -10.0
        internalLimiter.berechneYOUT(0.001);
        assertEquals("Should clamp to lower limit", -10.0, 
                    internalLimiter._outputSignal[0][0], TOLERANCE);
    }
    
    @Test
    public void testInternalLimiterPassthrough() {
        // Test that values within range pass through unchanged
        internalLimiter._inputSignal[0][0] = 5.0;  // Within [-10, 10]
        internalLimiter.berechneYOUT(0.001);
        assertEquals("Should pass through unchanged", 5.0, 
                    internalLimiter._outputSignal[0][0], TOLERANCE);
    }
    
    @Test
    public void testInternalLimiterZero() {
        // Test zero input
        internalLimiter._inputSignal[0][0] = 0.0;
        internalLimiter.berechneYOUT(0.001);
        assertEquals("Zero should pass through", 0.0, 
                    internalLimiter._outputSignal[0][0], TOLERANCE);
    }
    
    @Test
    public void testInternalLimiterBoundaries() {
        // Test exact boundary values
        internalLimiter._inputSignal[0][0] = 10.0;
        internalLimiter.berechneYOUT(0.001);
        assertEquals("Upper boundary should pass", 10.0, 
                    internalLimiter._outputSignal[0][0], TOLERANCE);
        
        internalLimiter._inputSignal[0][0] = -10.0;
        internalLimiter.berechneYOUT(0.001);
        assertEquals("Lower boundary should pass", -10.0, 
                    internalLimiter._outputSignal[0][0], TOLERANCE);
    }
    
    @Test
    public void testExternalLimiterBasic() {
        // External limiter with dynamic limits
        // Note: inputSignal[1] = minimum, inputSignal[2] = maximum
        externalLimiter._inputSignal[0][0] = 15.0;    // Signal
        externalLimiter._inputSignal[1][0] = -10.0;   // Minimum
        externalLimiter._inputSignal[2][0] = 10.0;    // Maximum
        
        externalLimiter.berechneYOUT(0.001);
        assertEquals("Should clamp to external maximum", 10.0, 
                    externalLimiter._outputSignal[0][0], TOLERANCE);
    }
    
    @Test
    public void testExternalLimiterAsymmetric() {
        // Test asymmetric limits
        externalLimiter._inputSignal[0][0] = 20.0;
        externalLimiter._inputSignal[1][0] = -15.0;   // Wider minimum
        externalLimiter._inputSignal[2][0] = 5.0;     // Narrower maximum
        
        externalLimiter.berechneYOUT(0.001);
        assertEquals("Should respect asymmetric maximum", 5.0, 
                    externalLimiter._outputSignal[0][0], TOLERANCE);
    }
    
    @Test
    public void testInternalLimiterSequence() {
        // Test multiple sequential values
        double[] testValues = {5.0, 15.0, -5.0, -15.0, 0.0};
        double[] expected = {5.0, 10.0, -5.0, -10.0, 0.0};
        
        for (int i = 0; i < testValues.length; i++) {
            internalLimiter._inputSignal[0][0] = testValues[i];
            internalLimiter.berechneYOUT(0.001);
            assertEquals("Value " + i + " should be clamped correctly", 
                        expected[i], internalLimiter._outputSignal[0][0], TOLERANCE);
        }
    }
}
