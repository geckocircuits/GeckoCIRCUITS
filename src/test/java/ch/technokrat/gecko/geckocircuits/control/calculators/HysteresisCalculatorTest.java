package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for HysteresisCalculator - Hysteresis (Schmitt trigger) with memory and distinct thresholds.
 * HysteresisCalculatorInternal uses a hysteresis band (hValue).
 */
public class HysteresisCalculatorTest {
    
    private HysteresisCalculatorInternal internalHyst;
    private HysteresisCalculatorExternal externalHyst;
    private static final double TOLERANCE = 1e-10;
    
    @Before
    public void setUp() {
        // Internal hysteresis with band ±2.0
        internalHyst = new HysteresisCalculatorInternal(2.0);
        
        // External hysteresis (parameter-based)
        externalHyst = new HysteresisCalculatorExternal();
        
        // Initialize arrays for internal
        internalHyst._inputSignal[0] = new double[]{0};
        internalHyst._outputSignal[0] = new double[]{0};
        
        // Initialize arrays for external (typically 3 inputs: input, upper threshold, lower threshold)
        for (int i = 0; i < externalHyst._inputSignal.length; i++) {
            externalHyst._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < externalHyst._outputSignal.length; i++) {
            externalHyst._outputSignal[i] = new double[]{0};
        }
    }
    
    // Test rising edge transition (crossing upper threshold)
    @Test
    public void testInternalHysteresisRisingEdge() {
        // Start below hysteresis band
        internalHyst._inputSignal[0][0] = -5.0;
        internalHyst.berechneYOUT(0.001);
        double initialOutput = internalHyst._outputSignal[0][0];
        
        // Sweep through band to above +2.0 (upper threshold)
        internalHyst._inputSignal[0][0] = 0.0;
        internalHyst.berechneYOUT(0.001);
        
        internalHyst._inputSignal[0][0] = 3.0;  // Above upper threshold
        internalHyst.berechneYOUT(0.001);
        double risingOutput = internalHyst._outputSignal[0][0];
        
        assertNotEquals("Output should change on rising edge", initialOutput, risingOutput, TOLERANCE);
    }
    
    // Test falling edge transition (crossing lower threshold)
    @Test
    public void testInternalHysteresisFallingEdge() {
        // Start above hysteresis band
        internalHyst._inputSignal[0][0] = 5.0;
        internalHyst.berechneYOUT(0.001);
        double highOutput = internalHyst._outputSignal[0][0];
        
        // Sweep through band to below -2.0 (lower threshold)
        internalHyst._inputSignal[0][0] = 0.0;
        internalHyst.berechneYOUT(0.001);
        
        internalHyst._inputSignal[0][0] = -3.0;  // Below lower threshold
        internalHyst.berechneYOUT(0.001);
        double fallingOutput = internalHyst._outputSignal[0][0];
        
        assertNotEquals("Output should change on falling edge", highOutput, fallingOutput, TOLERANCE);
    }
    
    // Test hysteresis band behavior (no output change within band)
    @Test
    public void testInternalHysteresisHysteresisBand() {
        // Set to positive region
        internalHyst._inputSignal[0][0] = 5.0;
        internalHyst.berechneYOUT(0.001);
        double positiveOutput = internalHyst._outputSignal[0][0];
        
        // Move within hysteresis band ±2.0 - should retain output
        internalHyst._inputSignal[0][0] = 1.5;
        internalHyst.berechneYOUT(0.001);
        assertEquals("Output should remain stable within band", positiveOutput, 
                     internalHyst._outputSignal[0][0], TOLERANCE);
        
        internalHyst._inputSignal[0][0] = 0.5;
        internalHyst.berechneYOUT(0.001);
        assertEquals("Output should remain stable within band", positiveOutput, 
                     internalHyst._outputSignal[0][0], TOLERANCE);
    }
    
    // Test state memory across multiple cycles
    @Test
    public void testInternalHysteresisMemory() {
        // Initialize to positive state
        internalHyst._inputSignal[0][0] = 5.0;
        internalHyst.berechneYOUT(0.001);
        double positiveOutput = internalHyst._outputSignal[0][0];
        
        // Multiple transitions staying within band
        for (int i = 0; i < 5; i++) {
            internalHyst._inputSignal[0][0] = 1.0 + (i * 0.1);
            internalHyst.berechneYOUT(0.001);
            assertEquals("Should maintain state throughout cycles", positiveOutput, 
                         internalHyst._outputSignal[0][0], TOLERANCE);
        }
    }
    
    // Test stability across many cycles
    @Test
    public void testInternalHysteresisStability() {
        // Create a signal that oscillates within the hysteresis band
        internalHyst._inputSignal[0][0] = 3.0;  // Start above upper threshold
        internalHyst.berechneYOUT(0.001);
        double initialOutput = internalHyst._outputSignal[0][0];
        
        // Oscillate within band for 10 steps
        for (int i = 0; i < 10; i++) {
            internalHyst._inputSignal[0][0] = 1.0 + (i % 2) * 0.5;  // Oscillate between 1.0 and 1.5
            internalHyst.berechneYOUT(0.001);
            assertEquals("Output should remain stable within band", initialOutput, 
                         internalHyst._outputSignal[0][0], TOLERANCE);
        }
    }
}
