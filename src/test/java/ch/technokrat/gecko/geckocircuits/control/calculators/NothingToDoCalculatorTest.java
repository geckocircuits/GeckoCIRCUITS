package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for NothingToDoCalculator - a placeholder calculator that performs no operation.
 * Used for circuit elements that don't need control block calculations.
 */
public class NothingToDoCalculatorTest {
    
    private NothingToDoCalculator calculator;
    
    @Before
    public void setUp() {
        // Create with 3 inputs and 2 outputs
        calculator = new NothingToDoCalculator(3, 2);
        // Initialize input and output signal arrays
        for (int i = 0; i < 3; i++) {
            calculator._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < 2; i++) {
            calculator._outputSignal[i] = new double[]{0};
        }
    }
    
    @Test
    public void testConstructorWithParameters() {
        assertEquals("Should have 3 inputs", 3, calculator._inputSignal.length);
        assertEquals("Should have 2 outputs", 2, calculator._outputSignal.length);
    }
    
    @Test
    public void testNoOperation() {
        // Set some input values
        calculator._inputSignal[0][0] = 5.0;
        calculator._inputSignal[1][0] = 10.0;
        calculator._inputSignal[2][0] = 15.0;
        
        // Call berechneYOUT (should do nothing)
        calculator.berechneYOUT(0.001);
        
        // Verify no calculation occurred (outputs remain unchanged)
        // Output should still be 0 (uninitialized)
        assertEquals("First output should be 0", 0.0, calculator._outputSignal[0][0], 1e-10);
        assertEquals("Second output should be 0", 0.0, calculator._outputSignal[1][0], 1e-10);
    }
    
    @Test
    public void testMultipleTimeSteps() {
        // Test that repeated calls do nothing
        for (int i = 0; i < 10; i++) {
            calculator._inputSignal[0][0] = i * 2.0;
            calculator.berechneYOUT(0.001);
            assertEquals("Output should remain 0 after step " + i, 
                        0.0, calculator._outputSignal[0][0], 1e-10);
        }
    }
    
    @Test
    public void testWithZeroInputsOutputs() {
        // Edge case: zero inputs and outputs
        NothingToDoCalculator zeroCalc = new NothingToDoCalculator(0, 0);
        
        // Should not throw exception
        zeroCalc.berechneYOUT(0.001);
        assertEquals("Should handle 0 inputs", 0, zeroCalc._inputSignal.length);
        assertEquals("Should handle 0 outputs", 0, zeroCalc._outputSignal.length);
    }
    
    @Test
    public void testWithLargeInputOutputCount() {
        // Test with many inputs and outputs
        NothingToDoCalculator largeCalc = new NothingToDoCalculator(10, 5);
        
        // Initialize input and output signal arrays
        for (int i = 0; i < 10; i++) {
            largeCalc._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < 5; i++) {
            largeCalc._outputSignal[i] = new double[]{0};
        }
        
        // Fill all inputs
        for (int i = 0; i < 10; i++) {
            largeCalc._inputSignal[i][0] = i * 1.5;
        }
        
        // Execute calculation
        largeCalc.berechneYOUT(0.001);
        
        // All outputs should remain 0
        for (int i = 0; i < 5; i++) {
            assertEquals("Output " + i + " should be 0", 
                        0.0, largeCalc._outputSignal[i][0], 1e-10);
        }
    }
}
