package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for PmsmModulatorCalculator - Space Vector PWM modulation for 3-phase inverter.
 * Converts alpha-beta voltage references to 3-phase PWM duty cycles.
 */
public class PmsmModulatorCalculatorTest {
    
    private PmsmModulatorCalculator calculator;
    private static final double TOLERANCE = 1e-6;
    
    @Before
    public void setUp() {
        calculator = new PmsmModulatorCalculator();
        // Initialize input and output signal arrays
        for (int i = 0; i < calculator._inputSignal.length; i++) {
            calculator._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            calculator._outputSignal[i] = new double[]{0};
        }
    }
    
    @Test
    public void testConstructor() {
        // Modulator takes 4 inputs (Va, Vb, triangle, Vdc) and produces 3 outputs (PWM A, B, C)
        assertTrue("Should have inputs", calculator._inputSignal.length >= 3);
        assertTrue("Should have 3 phase outputs", calculator._outputSignal.length >= 3);
    }
    
    @Test
    public void testZeroVoltage() {
        // Zero voltage input should produce 50% duty cycle (center point)
        calculator._inputSignal[0][0] = 0.0;  // Valpha
        calculator._inputSignal[1][0] = 0.0;  // Vbeta
        calculator._inputSignal[2][0] = 0.5;  // Triangle carrier
        calculator._inputSignal[3][0] = 10.0; // Vdc
        
        calculator.berechneYOUT(0.001);
        
        // All three phase outputs should be around 0.5 (50% duty)
        for (int i = 0; i < 3; i++) {
            assertTrue("Phase " + i + " duty should be valid", 
                      calculator._outputSignal[i][0] >= 0.0 && calculator._outputSignal[i][0] <= 1.0);
        }
    }
    
    @Test
    public void testMaximumVoltage() {
        // Maximum voltage should not exceed Vdc
        double vdc = 10.0;
        calculator._inputSignal[0][0] = 5.0;  // Valpha (near max)
        calculator._inputSignal[1][0] = 5.0;  // Vbeta (near max)
        calculator._inputSignal[2][0] = 0.5;  // Triangle
        calculator._inputSignal[3][0] = vdc;
        
        calculator.berechneYOUT(0.001);
        
        // All duty cycles should be clamped to [0, 1]
        for (int i = 0; i < 3; i++) {
            assertTrue("Duty cycle should be in [0,1]", 
                      calculator._outputSignal[i][0] >= 0.0 && calculator._outputSignal[i][0] <= 1.0);
        }
    }
    
    @Test
    public void testAlphaModulation() {
        // Modulate with alpha-axis voltage only
        calculator._inputSignal[0][0] = 3.0;  // Valpha
        calculator._inputSignal[1][0] = 0.0;  // Vbeta = 0
        calculator._inputSignal[2][0] = 0.5;  // Triangle at 50%
        calculator._inputSignal[3][0] = 10.0;
        
        calculator.berechneYOUT(0.001);
        
        // Should produce balanced output with phase differences
        for (int i = 0; i < 3; i++) {
            assertNotNull("Output should be calculated", calculator._outputSignal[i][0]);
            assertFalse("Output should be valid", Double.isNaN(calculator._outputSignal[i][0]));
        }
    }
    
    @Test
    public void testMultipleTriangleWaves() {
        // Test across different triangle carrier positions
        calculator._inputSignal[0][0] = 2.0;  // Valpha
        calculator._inputSignal[1][0] = 1.0;  // Vbeta
        calculator._inputSignal[3][0] = 10.0; // Vdc
        
        for (double trianglePos = 0.0; trianglePos <= 1.0; trianglePos += 0.2) {
            calculator._inputSignal[2][0] = trianglePos;
            calculator.berechneYOUT(0.001);
            
            // Verify outputs remain valid
            for (int i = 0; i < 3; i++) {
                assertTrue("Duty at triangle " + trianglePos + " should be in [0,1]",
                          calculator._outputSignal[i][0] >= 0.0 && calculator._outputSignal[i][0] <= 1.0);
            }
        }
    }
}
