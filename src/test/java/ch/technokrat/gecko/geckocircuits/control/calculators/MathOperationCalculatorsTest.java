package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Batch test for basic math operation calculators.
 * Tests transcendental functions (sin, cos, tan, sqrt, exp, ln, abs, etc.)
 */
public class MathOperationCalculatorsTest {
    
    private static final double TOLERANCE = 1e-6;
    
    // Test SinCalculator
    @Test
    public void testSinCalculator() {
        SinCalculator calc = new SinCalculator();
        calc._inputSignal[0] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        calc._inputSignal[0][0] = 0.0;
        calc.berechneYOUT(0.001);
        assertEquals("sin(0) = 0", 0.0, calc._outputSignal[0][0], TOLERANCE);
        
        calc._inputSignal[0][0] = Math.PI / 2;
        calc.berechneYOUT(0.001);
        assertEquals("sin(π/2) ≈ 1", 1.0, calc._outputSignal[0][0], TOLERANCE);
        
        calc._inputSignal[0][0] = Math.PI;
        calc.berechneYOUT(0.001);
        assertEquals("sin(π) ≈ 0", 0.0, calc._outputSignal[0][0], TOLERANCE);
    }
    
    // Test CosCalculator
    @Test
    public void testCosCalculator() {
        CosCalculator calc = new CosCalculator();
        calc._inputSignal[0] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        calc._inputSignal[0][0] = 0.0;
        calc.berechneYOUT(0.001);
        assertEquals("cos(0) = 1", 1.0, calc._outputSignal[0][0], TOLERANCE);
        
        calc._inputSignal[0][0] = Math.PI / 2;
        calc.berechneYOUT(0.001);
        assertEquals("cos(π/2) ≈ 0", 0.0, calc._outputSignal[0][0], TOLERANCE);
        
        calc._inputSignal[0][0] = Math.PI;
        calc.berechneYOUT(0.001);
        assertEquals("cos(π) = -1", -1.0, calc._outputSignal[0][0], TOLERANCE);
    }
    
    // Test TanCalculator
    @Test
    public void testTanCalculator() {
        TanCalculator calc = new TanCalculator();
        calc._inputSignal[0] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        calc._inputSignal[0][0] = 0.0;
        calc.berechneYOUT(0.001);
        assertEquals("tan(0) = 0", 0.0, calc._outputSignal[0][0], TOLERANCE);
        
        calc._inputSignal[0][0] = Math.PI / 4;
        calc.berechneYOUT(0.001);
        assertEquals("tan(π/4) = 1", 1.0, calc._outputSignal[0][0], 1e-5);
    }
    
    // Test SqrtCalculator
    @Test
    public void testSqrtCalculator() {
        SqrtCalculator calc = new SqrtCalculator();
        calc._inputSignal[0] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        calc._inputSignal[0][0] = 0.0;
        calc.berechneYOUT(0.001);
        assertEquals("√0 = 0", 0.0, calc._outputSignal[0][0], TOLERANCE);
        
        calc._inputSignal[0][0] = 4.0;
        calc.berechneYOUT(0.001);
        assertEquals("√4 = 2", 2.0, calc._outputSignal[0][0], TOLERANCE);
        
        calc._inputSignal[0][0] = 9.0;
        calc.berechneYOUT(0.001);
        assertEquals("√9 = 3", 3.0, calc._outputSignal[0][0], TOLERANCE);
    }
    
    // Test SquareCalculator
    @Test
    public void testSquareCalculator() {
        SquareCalculator calc = new SquareCalculator();
        calc._inputSignal[0] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        calc._inputSignal[0][0] = 2.0;
        calc.berechneYOUT(0.001);
        assertEquals("2² = 4", 4.0, calc._outputSignal[0][0], TOLERANCE);
        
        calc._inputSignal[0][0] = -3.0;
        calc.berechneYOUT(0.001);
        assertEquals("(-3)² = 9", 9.0, calc._outputSignal[0][0], TOLERANCE);
    }
    
    // Test ExpCalculator
    @Test
    public void testExpCalculator() {
        ExpCalculator calc = new ExpCalculator();
        calc._inputSignal[0] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        calc._inputSignal[0][0] = 0.0;
        calc.berechneYOUT(0.001);
        assertEquals("e^0 = 1", 1.0, calc._outputSignal[0][0], TOLERANCE);
        
        calc._inputSignal[0][0] = 1.0;
        calc.berechneYOUT(0.001);
        assertEquals("e^1 ≈ e", Math.E, calc._outputSignal[0][0], 1e-5);
    }
    
    // Test LnCalculator
    @Test
    public void testLnCalculator() {
        LnCalculator calc = new LnCalculator();
        calc._inputSignal[0] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        calc._inputSignal[0][0] = 1.0;
        calc.berechneYOUT(0.001);
        assertEquals("ln(1) = 0", 0.0, calc._outputSignal[0][0], TOLERANCE);
        
        calc._inputSignal[0][0] = Math.E;
        calc.berechneYOUT(0.001);
        assertEquals("ln(e) = 1", 1.0, calc._outputSignal[0][0], 1e-5);
    }
    
    // Test AbsCalculator
    @Test
    public void testAbsCalculator() {
        AbsCalculator calc = new AbsCalculator();
        calc._inputSignal[0] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        calc._inputSignal[0][0] = -5.0;
        calc.berechneYOUT(0.001);
        assertEquals("|−5| = 5", 5.0, calc._outputSignal[0][0], TOLERANCE);
        
        calc._inputSignal[0][0] = 3.5;
        calc.berechneYOUT(0.001);
        assertEquals("|3.5| = 3.5", 3.5, calc._outputSignal[0][0], TOLERANCE);
    }
    
    // Test SignumCalculator
    @Test
    public void testSignumCalculator() {
        SignumCalculator calc = new SignumCalculator();
        calc._inputSignal[0] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        calc._inputSignal[0][0] = 5.0;
        calc.berechneYOUT(0.001);
        assertEquals("sgn(5) = 1", 1.0, calc._outputSignal[0][0], TOLERANCE);
        
        calc._inputSignal[0][0] = -5.0;
        calc.berechneYOUT(0.001);
        assertEquals("sgn(−5) = −1", -1.0, calc._outputSignal[0][0], TOLERANCE);
        
        calc._inputSignal[0][0] = 0.0;
        calc.berechneYOUT(0.001);
        assertEquals("sgn(0) = 0", 0.0, calc._outputSignal[0][0], TOLERANCE);
    }
    
    // Test RoundCalculator
    @Test
    public void testRoundCalculator() {
        RoundCalculator calc = new RoundCalculator();
        calc._inputSignal[0] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        calc._inputSignal[0][0] = 3.7;
        calc.berechneYOUT(0.001);
        assertEquals("round(3.7) = 4", 4.0, calc._outputSignal[0][0], TOLERANCE);
        
        calc._inputSignal[0][0] = 3.2;
        calc.berechneYOUT(0.001);
        assertEquals("round(3.2) = 3", 3.0, calc._outputSignal[0][0], TOLERANCE);
    }
}
