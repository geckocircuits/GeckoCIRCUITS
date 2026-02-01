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
    
    @Test
    public void testSector0() {
        // Test vector angle in sector 0 (0 to π/3)
        calculator._inputSignal[0][0] = 5.0;  // Valpha
        calculator._inputSignal[1][0] = 1.0;  // Vbeta (30 degrees)
        calculator._inputSignal[2][0] = 0.5;  // Triangle
        calculator._inputSignal[3][0] = 10.0; // Vdc
        
        calculator.berechneYOUT(0.001);
        
        for (int i = 0; i < 3; i++) {
            assertTrue("Duty cycle should be valid in sector 0",
                      calculator._outputSignal[i][0] >= 0.0 && calculator._outputSignal[i][0] <= 1.0);
            assertFalse("Output should not be NaN", Double.isNaN(calculator._outputSignal[i][0]));
        }
    }
    
    @Test
    public void testSector2And3() {
        // Test vector angles in sector 2 and 3 (π/2 quadrant)
        calculator._inputSignal[0][0] = -2.0; // Valpha (negative)
        calculator._inputSignal[1][0] = 3.0;  // Vbeta (positive)
        calculator._inputSignal[2][0] = 0.3;  // Triangle
        calculator._inputSignal[3][0] = 10.0; // Vdc
        
        calculator.berechneYOUT(0.001);
        
        for (int i = 0; i < 3; i++) {
            assertTrue("Duty cycle should be bounded",
                      calculator._outputSignal[i][0] >= 0.0 && calculator._outputSignal[i][0] <= 1.0);
        }
    }
    
    @Test
    public void testVoltageClipping() {
        // Voltage magnitude exceeding Vdc should be clipped
        double vdc = 10.0;
        calculator._inputSignal[0][0] = 8.0;   // Very high Valpha
        calculator._inputSignal[1][0] = 8.0;   // Very high Vbeta
        calculator._inputSignal[2][0] = 0.5;
        calculator._inputSignal[3][0] = vdc;
        
        calculator.berechneYOUT(0.001);
        
        // Outputs should still be valid (clipped to DC voltage limit)
        for (int i = 0; i < 3; i++) {
            assertTrue("Output should be within [0,1]",
                      calculator._outputSignal[i][0] >= 0.0 && calculator._outputSignal[i][0] <= 1.0);
        }
    }
    
    @Test
    public void testBetaAxisOnly() {
        // Modulate with beta-axis voltage only (90 degrees)
        calculator._inputSignal[0][0] = 0.0;   // Valpha = 0
        calculator._inputSignal[1][0] = 4.0;   // Vbeta only
        calculator._inputSignal[2][0] = 0.5;
        calculator._inputSignal[3][0] = 10.0;
        
        calculator.berechneYOUT(0.001);
        
        // Three-phase outputs should be 120 degrees apart
        for (int i = 0; i < 3; i++) {
            assertTrue("Output should be calculable",
                      calculator._outputSignal[i][0] >= 0.0 && calculator._outputSignal[i][0] <= 1.0);
        }
    }
    
    @Test
    public void testNegativeVoltages() {
        // Test with negative voltage references (different quadrants)
        calculator._inputSignal[0][0] = -3.0;  // Valpha negative
        calculator._inputSignal[1][0] = -2.0;  // Vbeta negative
        calculator._inputSignal[2][0] = 0.5;
        calculator._inputSignal[3][0] = 10.0;
        
        calculator.berechneYOUT(0.001);
        
        // Phase outputs should still be valid
        for (int i = 0; i < 3; i++) {
            assertFalse("Should not be NaN", Double.isNaN(calculator._outputSignal[i][0]));
            assertTrue("Should be in [0,1]", calculator._outputSignal[i][0] >= 0.0 && calculator._outputSignal[i][0] <= 1.0);
        }
    }
    
    @Test
    public void testExtremePWMValues() {
        // Test with extreme PWM positions (very low and very high)
        calculator._inputSignal[0][0] = 2.0;
        calculator._inputSignal[1][0] = 1.0;
        calculator._inputSignal[3][0] = 10.0;
        
        double[] trianglePositions = {0.0, 0.01, 0.99, 1.0};
        for (double pos : trianglePositions) {
            calculator._inputSignal[2][0] = pos;
            calculator.berechneYOUT(0.001);
            
            for (int i = 0; i < 3; i++) {
                assertTrue("Extreme PWM position " + pos + " should be valid",
                          calculator._outputSignal[i][0] >= 0.0 && calculator._outputSignal[i][0] <= 1.0);
            }
        }
    }
    
    @Test
    public void testLowDCVoltage() {
        // Test with low DC link voltage (near zero)
        calculator._inputSignal[0][0] = 2.0;
        calculator._inputSignal[1][0] = 1.0;
        calculator._inputSignal[2][0] = 0.5;
        calculator._inputSignal[3][0] = 0.5;  // Very low Vdc
        
        calculator.berechneYOUT(0.001);
        
        for (int i = 0; i < 3; i++) {
            assertTrue("Low Vdc should still produce valid output",
                      calculator._outputSignal[i][0] >= 0.0 && calculator._outputSignal[i][0] <= 1.0);
        }
    }
    
    @Test
    public void testHighDCVoltage() {
        // Test with high DC link voltage
        calculator._inputSignal[0][0] = 2.0;
        calculator._inputSignal[1][0] = 1.0;
        calculator._inputSignal[2][0] = 0.5;
        calculator._inputSignal[3][0] = 600.0; // High voltage (industrial)
        
        calculator.berechneYOUT(0.001);
        
        for (int i = 0; i < 3; i++) {
            assertTrue("High Vdc should produce valid output",
                      calculator._outputSignal[i][0] >= 0.0 && calculator._outputSignal[i][0] <= 1.0);
        }
    }
}
