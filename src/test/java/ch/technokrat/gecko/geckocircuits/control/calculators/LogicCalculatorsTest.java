package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Batch test for logic operation calculators.
 * Tests boolean gates (AND, OR, NOT, XOR) and comparison operators.
 * Conventions: 0.0 = false, non-zero (esp 1.0) = true
 */
public class LogicCalculatorsTest {
    
    private static final double TOLERANCE = 1e-9;
    private static final double TRUE = 1.0;
    private static final double FALSE = 0.0;
    
    // Test AndTwoPortCalculator (AND gate with 2 inputs)
    @Test
    public void testAndTwoPort() {
        AndTwoPortCalculator calc = new AndTwoPortCalculator();
        calc._inputSignal[0] = new double[]{0};
        calc._inputSignal[1] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        // Test truth table
        // 0 AND 0 = 0
        calc._inputSignal[0][0] = FALSE;
        calc._inputSignal[1][0] = FALSE;
        calc.berechneYOUT(0.001);
        assertEquals("FALSE AND FALSE = FALSE", FALSE, calc._outputSignal[0][0], TOLERANCE);
        
        // 0 AND 1 = 0
        calc._inputSignal[0][0] = FALSE;
        calc._inputSignal[1][0] = TRUE;
        calc.berechneYOUT(0.001);
        assertEquals("FALSE AND TRUE = FALSE", FALSE, calc._outputSignal[0][0], TOLERANCE);
        
        // 1 AND 0 = 0
        calc._inputSignal[0][0] = TRUE;
        calc._inputSignal[1][0] = FALSE;
        calc.berechneYOUT(0.001);
        assertEquals("TRUE AND FALSE = FALSE", FALSE, calc._outputSignal[0][0], TOLERANCE);
        
        // 1 AND 1 = 1
        calc._inputSignal[0][0] = TRUE;
        calc._inputSignal[1][0] = TRUE;
        calc.berechneYOUT(0.001);
        assertEquals("TRUE AND TRUE = TRUE", TRUE, calc._outputSignal[0][0], TOLERANCE);
    }
    
    // Test OrCalculatorTwoInputs (OR gate with 2 inputs)
    @Test
    public void testOrTwoInputs() {
        OrCalculatorTwoInputs calc = new OrCalculatorTwoInputs();
        calc._inputSignal[0] = new double[]{0};
        calc._inputSignal[1] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        // Test truth table
        // 0 OR 0 = 0
        calc._inputSignal[0][0] = FALSE;
        calc._inputSignal[1][0] = FALSE;
        calc.berechneYOUT(0.001);
        assertEquals("FALSE OR FALSE = FALSE", FALSE, calc._outputSignal[0][0], TOLERANCE);
        
        // 0 OR 1 = 1
        calc._inputSignal[0][0] = FALSE;
        calc._inputSignal[1][0] = TRUE;
        calc.berechneYOUT(0.001);
        assertEquals("FALSE OR TRUE = TRUE", TRUE, calc._outputSignal[0][0], TOLERANCE);
        
        // 1 OR 0 = 1
        calc._inputSignal[0][0] = TRUE;
        calc._inputSignal[1][0] = FALSE;
        calc.berechneYOUT(0.001);
        assertEquals("TRUE OR FALSE = TRUE", TRUE, calc._outputSignal[0][0], TOLERANCE);
        
        // 1 OR 1 = 1
        calc._inputSignal[0][0] = TRUE;
        calc._inputSignal[1][0] = TRUE;
        calc.berechneYOUT(0.001);
        assertEquals("TRUE OR TRUE = TRUE", TRUE, calc._outputSignal[0][0], TOLERANCE);
    }
    
    // Test NotCalculator (NOT/Inverter gate)
    @Test
    public void testNotCalculator() {
        NotCalculator calc = new NotCalculator();
        calc._inputSignal[0] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        // NOT 0 = 1
        calc._inputSignal[0][0] = FALSE;
        calc.berechneYOUT(0.001);
        assertEquals("NOT FALSE = TRUE", TRUE, calc._outputSignal[0][0], TOLERANCE);
        
        // NOT 1 = 0
        calc._inputSignal[0][0] = TRUE;
        calc.berechneYOUT(0.001);
        assertEquals("NOT TRUE = FALSE", FALSE, calc._outputSignal[0][0], TOLERANCE);
    }
    
    // Test XORCalculator (XOR/exclusive-or gate)
    @Test
    public void testXORCalculator() {
        XORCalculator calc = new XORCalculator();
        calc._inputSignal[0] = new double[]{0};
        calc._inputSignal[1] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        // 0 XOR 0 = 0
        calc._inputSignal[0][0] = FALSE;
        calc._inputSignal[1][0] = FALSE;
        calc.berechneYOUT(0.001);
        assertEquals("FALSE XOR FALSE = FALSE", FALSE, calc._outputSignal[0][0], TOLERANCE);
        
        // 0 XOR 1 = 1
        calc._inputSignal[0][0] = FALSE;
        calc._inputSignal[1][0] = TRUE;
        calc.berechneYOUT(0.001);
        assertEquals("FALSE XOR TRUE = TRUE", TRUE, calc._outputSignal[0][0], TOLERANCE);
        
        // 1 XOR 0 = 1
        calc._inputSignal[0][0] = TRUE;
        calc._inputSignal[1][0] = FALSE;
        calc.berechneYOUT(0.001);
        assertEquals("TRUE XOR FALSE = TRUE", TRUE, calc._outputSignal[0][0], TOLERANCE);
        
        // 1 XOR 1 = 0
        calc._inputSignal[0][0] = TRUE;
        calc._inputSignal[1][0] = TRUE;
        calc.berechneYOUT(0.001);
        assertEquals("TRUE XOR TRUE = FALSE", FALSE, calc._outputSignal[0][0], TOLERANCE);
    }
    
    // Test GreaterThanCalculator (>)
    @Test
    public void testGreaterThan() {
        GreaterThanCalculator calc = new GreaterThanCalculator();
        calc._inputSignal[0] = new double[]{0};
        calc._inputSignal[1] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        // 5 > 3 = true
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 3.0;
        calc.berechneYOUT(0.001);
        assertEquals("5 > 3 = TRUE", TRUE, calc._outputSignal[0][0], TOLERANCE);
        
        // 2 > 3 = false
        calc._inputSignal[0][0] = 2.0;
        calc._inputSignal[1][0] = 3.0;
        calc.berechneYOUT(0.001);
        assertEquals("2 > 3 = FALSE", FALSE, calc._outputSignal[0][0], TOLERANCE);
        
        // 3 > 3 = false (boundary)
        calc._inputSignal[0][0] = 3.0;
        calc._inputSignal[1][0] = 3.0;
        calc.berechneYOUT(0.001);
        assertEquals("3 > 3 = FALSE", FALSE, calc._outputSignal[0][0], TOLERANCE);
    }
    
    // Test GreaterEqualCalculator (>=)
    @Test
    public void testGreaterEqual() {
        GreaterEqualCalculator calc = new GreaterEqualCalculator();
        calc._inputSignal[0] = new double[]{0};
        calc._inputSignal[1] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        // 5 >= 3 = true
        calc._inputSignal[0][0] = 5.0;
        calc._inputSignal[1][0] = 3.0;
        calc.berechneYOUT(0.001);
        assertEquals("5 >= 3 = TRUE", TRUE, calc._outputSignal[0][0], TOLERANCE);
        
        // 2 >= 3 = false
        calc._inputSignal[0][0] = 2.0;
        calc._inputSignal[1][0] = 3.0;
        calc.berechneYOUT(0.001);
        assertEquals("2 >= 3 = FALSE", FALSE, calc._outputSignal[0][0], TOLERANCE);
        
        // 3 >= 3 = true (boundary)
        calc._inputSignal[0][0] = 3.0;
        calc._inputSignal[1][0] = 3.0;
        calc.berechneYOUT(0.001);
        assertEquals("3 >= 3 = TRUE", TRUE, calc._outputSignal[0][0], TOLERANCE);
    }
    
    // Test EqualCalculatorTwoInputs (== comparison)
    @Test
    public void testEqualTwoInputs() {
        EqualCalculatorTwoInputs calc = new EqualCalculatorTwoInputs();
        calc._inputSignal[0] = new double[]{0};
        calc._inputSignal[1] = new double[]{0};
        calc._outputSignal[0] = new double[]{0};
        
        // 3 == 3 = true
        calc._inputSignal[0][0] = 3.0;
        calc._inputSignal[1][0] = 3.0;
        calc.berechneYOUT(0.001);
        assertEquals("3 == 3 = TRUE", TRUE, calc._outputSignal[0][0], TOLERANCE);
        
        // 2 == 3 = false
        calc._inputSignal[0][0] = 2.0;
        calc._inputSignal[1][0] = 3.0;
        calc.berechneYOUT(0.001);
        assertEquals("2 == 3 = FALSE", FALSE, calc._outputSignal[0][0], TOLERANCE);
        
        // -5 == -5 = true
        calc._inputSignal[0][0] = -5.0;
        calc._inputSignal[1][0] = -5.0;
        calc.berechneYOUT(0.001);
        assertEquals("−5 == −5 = TRUE", TRUE, calc._outputSignal[0][0], TOLERANCE);
    }
}
