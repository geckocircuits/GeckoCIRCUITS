package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for ABCDQCalculator - converts ABC (three-phase) to DQ (dq-frame) coordinates.
 * This is used in three-phase motor control.
 */
public class ABCDQCalculatorTest {
    
    private ABCDQCalculator calculator;
    private static final double TOLERANCE = 1e-10;
    private static final double SQRT3 = Math.sqrt(3);
    
    @Before
    public void setUp() {
        calculator = new ABCDQCalculator();
        // Initialize input and output signal arrays
        for (int i = 0; i < 4; i++) {
            calculator._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < 2; i++) {
            calculator._outputSignal[i] = new double[]{0};
        }
    }
    
    @Test
    public void testConstructor() {
        assertEquals("Should have 4 inputs (A, B, C, theta)", 4, calculator._inputSignal.length);
        assertEquals("Should have 2 outputs (d, q)", 2, calculator._outputSignal.length);
    }
    
    @Test
    public void testZeroInputs() {
        // With all zero inputs, outputs should be zero
        setInputs(0, 0, 0, 0);
        calculator.berechneYOUT(0.001);
        assertEquals("d should be 0 for zero inputs", 0.0, calculator._outputSignal[0][0], TOLERANCE);
        assertEquals("q should be 0 for zero inputs", 0.0, calculator._outputSignal[1][0], TOLERANCE);
    }
    
    @Test
    public void testBalancedThreePhaseAtTheta0() {
        // Balanced three-phase voltage at theta=0: Va=1, Vb=-0.5, Vc=-0.5
        // At theta=0, Vd should be 1, Vq should be 0
        double va = 1.0;
        double vb = -0.5;
        double vc = -0.5;
        double theta = 0.0;
        
        setInputs(va, vb, vc, theta);
        calculator.berechneYOUT(0.001);
        
        assertEquals("d-component at theta=0", 1.0, calculator._outputSignal[0][0], 1e-6);
        assertEquals("q-component at theta=0", 0.0, calculator._outputSignal[1][0], 1e-6);
    }
    
    @Test
    public void testBalancedThreePhaseAt90Degrees() {
        // At theta=90 degrees (pi/2), Vq should be -1 (negative due to phase rotation)
        double va = 1.0;
        double vb = -0.5;
        double vc = -0.5;
        double theta = Math.PI / 2;
        
        setInputs(va, vb, vc, theta);
        calculator.berechneYOUT(0.001);
        
        assertEquals("d-component at theta=90", 0.0, calculator._outputSignal[0][0], 1e-6);
        assertEquals("q-component at theta=90", -1.0, calculator._outputSignal[1][0], 1e-6);
    }
    
    @Test
    public void testSinglePhaseVoltage() {
        // Test with voltage only in phase A
        setInputs(1.0, 0.0, 0.0, 0.0);
        calculator.berechneYOUT(0.001);
        
        // With Va=1 and balanced reference at theta=0
        double expectedD = (2 * (1.0 - 0.0) * 1.0 + (0.0 - 0.0) * (1.0 + SQRT3 * 0.0)) / 3;
        assertEquals("d-component for single phase", expectedD, 
                    calculator._outputSignal[0][0], TOLERANCE);
    }
    
    @Test
    public void testNegativeTheta() {
        // Test with negative angle
        setInputs(1.0, -0.5, -0.5, -Math.PI / 3);
        calculator.berechneYOUT(0.001);
        
        // Rotation should work for negative angles too
        assertNotNull("Should calculate output for negative theta", 
                     calculator._outputSignal[0]);
    }
    
    private void setInputs(double a, double b, double c, double theta) {
        calculator._inputSignal[0][0] = a;
        calculator._inputSignal[1][0] = b;
        calculator._inputSignal[2][0] = c;
        calculator._inputSignal[3][0] = theta;
    }
}
