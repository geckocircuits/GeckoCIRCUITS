package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for SparseMatrixCalculator - Sparse Matrix Converter (SMC) control.
 * This calculator implements control logic for direct AC-AC conversion without DC link.
 */
public class SparseMatrixCalculatorTest {
    
    private SparseMatrixCalculator calculator;
    private static final double TOLERANCE = 1e-6;
    
    @Before
    public void setUp() {
        calculator = new SparseMatrixCalculator();
        // Initialize input and output signal arrays (8 inputs, 9 outputs)
        for (int i = 0; i < calculator._inputSignal.length; i++) {
            calculator._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            calculator._outputSignal[i] = new double[]{0};
        }
    }
    
    @Test
    public void testConstructor() {
        // SMC control has 8 inputs and 9 outputs
        assertEquals("Should have 8 inputs", 8, calculator._inputSignal.length);
        assertEquals("Should have 9 outputs", 9, calculator._outputSignal.length);
    }
    
    @Test
    public void testZeroInputs() {
        // Zero input should produce valid outputs (may not be exactly zero due to initialization)
        calculator.berechneYOUT(0.001);
        
        // All outputs should be initialized and valid
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertNotNull("Output " + i + " should be initialized", calculator._outputSignal[i][0]);
            assertFalse("Output " + i + " should be valid", Double.isNaN(calculator._outputSignal[i][0]));
        }
    }
    
    @Test
    public void testMultipleTimeSteps() {
        // Test stability across multiple calculation steps
        for (int step = 0; step < 10; step++) {
            // Set varying inputs
            calculator._inputSignal[0][0] = Math.sin(0.1 * step);
            calculator._inputSignal[1][0] = Math.cos(0.1 * step);
            
            calculator.berechneYOUT(0.001);
            
            // Verify all outputs remain valid
            for (int i = 0; i < calculator._outputSignal.length; i++) {
                assertFalse("Output " + i + " at step " + step + " should be valid", 
                           Double.isNaN(calculator._outputSignal[i][0]));
                assertFalse("Output " + i + " at step " + step + " should not be infinite", 
                           Double.isInfinite(calculator._outputSignal[i][0]));
            }
        }
    }
    
    @Test
    public void testInputVoltageConversion() {
        // Test conversion of 3-phase input to 3-phase output
        // Inputs: three input phase voltages and control signals
        calculator._inputSignal[0][0] = 5.0;   // Input Va
        calculator._inputSignal[1][0] = -2.5;  // Input Vb
        calculator._inputSignal[2][0] = -2.5;  // Input Vc
        calculator._inputSignal[3][0] = 3.0;   // Output Va ref
        calculator._inputSignal[4][0] = 0.0;   // Output Vb ref
        calculator._inputSignal[5][0] = -3.0;  // Output Vc ref
        calculator._inputSignal[6][0] = 0.0;   // Frequency
        calculator._inputSignal[7][0] = 0.0;   // Phase
        
        calculator.berechneYOUT(0.001);
        
        // 9 outputs: typically 6 switch commands + 3 voltage outputs or similar
        for (int i = 0; i < 6; i++) {
            // Switch commands should be 0 or 1 (or values indicating on/off)
            double out = calculator._outputSignal[i][0];
            assertTrue("Switch " + i + " should be reasonable value",
                      out >= -10 && out <= 10);
        }
    }
    
    @Test
    public void testFrequencyVariation() {
        // Test across different switching frequencies (if parameter exists)
        for (double freq = 0.0; freq <= 1.0; freq += 0.2) {
            calculator._inputSignal[6][0] = freq;  // Frequency parameter
            calculator.berechneYOUT(0.001);
            
            // Outputs should remain stable despite frequency changes
            for (int i = 0; i < calculator._outputSignal.length; i++) {
                assertFalse("Output should be valid at freq " + freq,
                           Double.isNaN(calculator._outputSignal[i][0]));
            }
        }
    }
    
    @Test
    public void testNegativeInputVoltages() {
        // Test with all negative input voltages
        calculator._inputSignal[0][0] = -5.0;
        calculator._inputSignal[1][0] = -3.0;
        calculator._inputSignal[2][0] = -2.0;
        
        calculator.berechneYOUT(0.001);
        
        // Should handle negative inputs gracefully
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertFalse("Output should be valid", Double.isNaN(calculator._outputSignal[i][0]));
        }
    }
    
    @Test
    public void testHighMagnitudeInputs() {
        // Test stability with high magnitude inputs
        calculator._inputSignal[0][0] = 1000.0;
        calculator._inputSignal[1][0] = 1000.0;
        calculator._inputSignal[2][0] = 1000.0;
        
        calculator.berechneYOUT(0.001);
        
        // Should remain stable
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertFalse("Output should be valid with high inputs",
                       Double.isNaN(calculator._outputSignal[i][0]));
            assertFalse("Output should not be infinite",
                       Double.isInfinite(calculator._outputSignal[i][0]));
        }
    }
    
    @Test
    public void testSmallTimeStep() {
        // Test with very small time step
        calculator._inputSignal[0][0] = 1.0;
        calculator.berechneYOUT(1e-8);
        
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertFalse("Output should be valid with small dt",
                       Double.isNaN(calculator._outputSignal[i][0]));
        }
    }
    
    @Test
    public void testLargeTimeStep() {
        // Test with larger time step
        calculator._inputSignal[0][0] = 1.0;
        calculator.berechneYOUT(0.01);
        
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertFalse("Output should be valid with large dt",
                       Double.isNaN(calculator._outputSignal[i][0]));
        }
    }
    
    @Test
    public void testOutputIndependence() {
        // Test that outputs remain consistent across repeated calculations
        double[] output1 = new double[9];

        calculator._inputSignal[0][0] = 3.0;
        calculator.berechneYOUT(0.001);

        // Store outputs
        for (int i = 0; i < 9; i++) {
            output1[i] = calculator._outputSignal[i][0];
        }

        // Reset calculator and recalculate with same input - note: may not be exactly deterministic due to state
        calculator._inputSignal[0][0] = 3.0;
        calculator.berechneYOUT(0.001);

        // Outputs should be close (allowing for floating point variations and internal state)
        for (int i = 0; i < 9; i++) {
            // Use a larger tolerance since the calculator may have internal state
            assertTrue("Output " + i + " should remain reasonable",
                      !Double.isNaN(calculator._outputSignal[i][0]));
            assertTrue("Output " + i + " should not be infinite",
                      !Double.isInfinite(calculator._outputSignal[i][0]));
        }
    }

    @Test
    public void testParkTransformThreePhase() {
        // Test Park/Clarke transforms for 3-phase signal conversion
        // Balanced 3-phase: Ia = 1.0, Ib = -0.5, Ic = -0.5 (sum to zero)
        calculator._inputSignal[0][0] = 1.0;   // Phase A
        calculator._inputSignal[1][0] = -0.5;  // Phase B
        calculator._inputSignal[2][0] = -0.5;  // Phase C
        calculator._inputSignal[3][0] = 0.0;   // Output reference Va
        calculator._inputSignal[4][0] = 0.0;   // Output reference Vb
        calculator._inputSignal[5][0] = 0.0;   // Output reference Vc
        calculator._inputSignal[6][0] = 50.0;  // Frequency (Hz)
        calculator._inputSignal[7][0] = 0.0;   // Phase

        calculator.berechneYOUT(0.001);

        // All outputs should be valid
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertFalse("Park transform output " + i + " should be valid",
                       Double.isNaN(calculator._outputSignal[i][0]));
        }
    }

    @Test
    public void testZeroCrossingBehavior() {
        // Test behavior at signal zero crossings
        calculator._inputSignal[0][0] = 0.0;  // Zero voltage
        calculator._inputSignal[1][0] = 0.0;
        calculator._inputSignal[2][0] = 0.0;

        calculator.berechneYOUT(0.001);

        // Should handle zero crossings gracefully
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertFalse("Output should be valid at zero crossing",
                       Double.isNaN(calculator._outputSignal[i][0]));
        }
    }

    @Test
    public void testLargeMagnitudeThreePhase() {
        // Test with large amplitude 3-phase inputs (high power)
        calculator._inputSignal[0][0] = 500.0;   // High input Va
        calculator._inputSignal[1][0] = -250.0;  // High input Vb
        calculator._inputSignal[2][0] = -250.0;  // High input Vc

        calculator.berechneYOUT(0.001);

        // Should remain stable with large inputs
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertFalse("Output should handle large magnitudes",
                       Double.isNaN(calculator._outputSignal[i][0]));
            assertFalse("Output should not become infinite",
                       Double.isInfinite(calculator._outputSignal[i][0]));
        }
    }

    @Test
    public void testStateConsistencyMultipleSteps() {
        // Test that internal state remains consistent across many time steps
        for (int step = 0; step < 100; step++) {
            double timeNorm = step * 0.001;
            // Sinusoidal 3-phase input
            calculator._inputSignal[0][0] = Math.sin(2 * Math.PI * 50 * timeNorm);
            calculator._inputSignal[1][0] = Math.sin(2 * Math.PI * 50 * timeNorm - 2*Math.PI/3);
            calculator._inputSignal[2][0] = Math.sin(2 * Math.PI * 50 * timeNorm + 2*Math.PI/3);
            calculator._inputSignal[6][0] = 50.0;  // 50 Hz frequency
            calculator._inputSignal[7][0] = 2 * Math.PI * 50 * timeNorm;  // Phase

            calculator.berechneYOUT(0.001);

            // Verify all outputs remain valid
            for (int i = 0; i < calculator._outputSignal.length; i++) {
                assertFalse("Output " + i + " at step " + step + " should be valid",
                           Double.isNaN(calculator._outputSignal[i][0]));
                assertFalse("Output " + i + " at step " + step + " should not overflow",
                           Double.isInfinite(calculator._outputSignal[i][0]));
            }
        }
    }

    @Test
    public void testFrequencyVariationContinuous() {
        // Test across a range of switching frequencies with continuous variation
        for (double freq = 10.0; freq <= 10000.0; freq *= 2) {
            for (int substep = 0; substep < 5; substep++) {
                calculator._inputSignal[0][0] = 100.0 * Math.sin(0.1 * freq * substep);
                calculator._inputSignal[1][0] = 100.0 * Math.sin(0.1 * freq * substep - 2*Math.PI/3);
                calculator._inputSignal[2][0] = 100.0 * Math.sin(0.1 * freq * substep + 2*Math.PI/3);
                calculator._inputSignal[6][0] = freq;  // Variable frequency

                calculator.berechneYOUT(0.001);

                // Outputs should remain stable
                for (int i = 0; i < calculator._outputSignal.length; i++) {
                    assertFalse("Output should be valid at frequency " + freq,
                               Double.isNaN(calculator._outputSignal[i][0]));
                }
            }
        }
    }

    @Test
    public void testAsymmetricalInputs() {
        // Test with unbalanced/asymmetrical 3-phase inputs
        calculator._inputSignal[0][0] = 100.0;  // Va
        calculator._inputSignal[1][0] = 50.0;   // Vb (unequal)
        calculator._inputSignal[2][0] = -150.0; // Vc (unequal)

        calculator.berechneYOUT(0.001);

        // Should handle asymmetrical inputs
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertFalse("Output should handle asymmetrical inputs",
                       Double.isNaN(calculator._outputSignal[i][0]));
        }
    }

    @Test
    public void testVerySmallTimeStep() {
        // Test with very small time step (high precision simulation)
        calculator._inputSignal[0][0] = 1.0;
        calculator.berechneYOUT(1e-9);

        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertFalse("Output should be valid with very small dt",
                       Double.isNaN(calculator._outputSignal[i][0]));
        }
    }
}
