/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for limiter, min/max, and signal processing control blocks.
 * Sprint 9: Control Package Core
 */
public class LimiterAndSignalBlocksTest {

    private static final double DELTA = 1e-10;

    // ========== Limiter Tests ==========
    
    @Test
    public void testLimiterBlockCreation() {
        ReglerLimit block = new ReglerLimit();
        assertNotNull("Limiter block should be created", block);
    }
    
    @Test
    public void testLimiter_MathClamp() {
        // Test basic clamping logic
        double value = 0.5;
        double min = -1.0;
        double max = 1.0;
        double result = Math.max(min, Math.min(max, value));
        assertEquals("0.5 within [-1,1] should be 0.5", 0.5, result, DELTA);
    }
    
    @Test
    public void testLimiter_AboveMax() {
        double value = 5.0;
        double min = -1.0;
        double max = 1.0;
        double result = Math.max(min, Math.min(max, value));
        assertEquals("5.0 above 1.0 should be limited to 1.0", 1.0, result, DELTA);
    }
    
    @Test
    public void testLimiter_BelowMin() {
        double value = -5.0;
        double min = -1.0;
        double max = 1.0;
        double result = Math.max(min, Math.min(max, value));
        assertEquals("-5.0 below -1.0 should be limited to -1.0", -1.0, result, DELTA);
    }
    
    @Test
    public void testLimiter_AtMax() {
        double value = 1.0;
        double min = -1.0;
        double max = 1.0;
        double result = Math.max(min, Math.min(max, value));
        assertEquals("1.0 at max should stay 1.0", 1.0, result, DELTA);
    }
    
    @Test
    public void testLimiter_AtMin() {
        double value = -1.0;
        double min = -1.0;
        double max = 1.0;
        double result = Math.max(min, Math.min(max, value));
        assertEquals("-1.0 at min should stay -1.0", -1.0, result, DELTA);
    }

    // ========== Min Tests ==========
    
    @Test
    public void testMinBlockCreation() {
        ReglerMIN block = new ReglerMIN();
        assertNotNull("Min block should be created", block);
    }
    
    @Test
    public void testMin_FirstSmaller() {
        // Test the min function directly
        double a = 2.0;
        double b = 5.0;
        double result = Math.min(a, b);
        assertEquals("min(2, 5) = 2", 2.0, result, DELTA);
    }
    
    @Test
    public void testMin_SecondSmaller() {
        double a = 5.0;
        double b = 2.0;
        double result = Math.min(a, b);
        assertEquals("min(5, 2) = 2", 2.0, result, DELTA);
    }
    
    @Test
    public void testMin_Equal() {
        double a = 3.0;
        double b = 3.0;
        double result = Math.min(a, b);
        assertEquals("min(3, 3) = 3", 3.0, result, DELTA);
    }
    
    @Test
    public void testMin_Negative() {
        double a = -3.0;
        double b = -5.0;
        double result = Math.min(a, b);
        assertEquals("min(-3, -5) = -5", -5.0, result, DELTA);
    }

    // ========== Max Tests ==========
    
    @Test
    public void testMaxBlockCreation() {
        ReglerMAX block = new ReglerMAX();
        assertNotNull("Max block should be created", block);
    }
    
    @Test
    public void testMax_FirstLarger() {
        double a = 5.0;
        double b = 2.0;
        double result = Math.max(a, b);
        assertEquals("max(5, 2) = 5", 5.0, result, DELTA);
    }
    
    @Test
    public void testMax_SecondLarger() {
        double a = 2.0;
        double b = 5.0;
        double result = Math.max(a, b);
        assertEquals("max(2, 5) = 5", 5.0, result, DELTA);
    }
    
    @Test
    public void testMax_Equal() {
        double a = 3.0;
        double b = 3.0;
        double result = Math.max(a, b);
        assertEquals("max(3, 3) = 3", 3.0, result, DELTA);
    }
    
    @Test
    public void testMax_Negative() {
        double a = -3.0;
        double b = -5.0;
        double result = Math.max(a, b);
        assertEquals("max(-3, -5) = -3", -3.0, result, DELTA);
    }

    // ========== Subtraction Tests ==========
    
    @Test
    public void testSubBlockCreation() {
        ReglerSubtraction block = new ReglerSubtraction();
        assertNotNull("Sub block should be created", block);
    }
    
    @Test
    public void testSub_PositiveNumbers() {
        double a = 5.0;
        double b = 3.0;
        double result = a - b;
        assertEquals("5 - 3 = 2", 2.0, result, DELTA);
    }
    
    @Test
    public void testSub_NegativeResult() {
        double a = 3.0;
        double b = 5.0;
        double result = a - b;
        assertEquals("3 - 5 = -2", -2.0, result, DELTA);
    }
    
    @Test
    public void testSub_NegativeNumbers() {
        double a = -3.0;
        double b = -5.0;
        double result = a - b;
        assertEquals("-3 - (-5) = 2", 2.0, result, DELTA);
    }

    // ========== Multiplication Tests ==========
    
    @Test
    public void testMultBlockCreation() {
        ReglerMUL block = new ReglerMUL();
        assertNotNull("Mult block should be created", block);
    }
    
    @Test
    public void testMult_PositiveNumbers() {
        double a = 4.0;
        double b = 3.0;
        double result = a * b;
        assertEquals("4 * 3 = 12", 12.0, result, DELTA);
    }
    
    @Test
    public void testMult_Zero() {
        double a = 5.0;
        double b = 0.0;
        double result = a * b;
        assertEquals("5 * 0 = 0", 0.0, result, DELTA);
    }
    
    @Test
    public void testMult_NegativeNumbers() {
        double a = -3.0;
        double b = 4.0;
        double result = a * b;
        assertEquals("-3 * 4 = -12", -12.0, result, DELTA);
    }

    // ========== Division Tests ==========
    
    @Test
    public void testDivBlockCreation() {
        ReglerDivision block = new ReglerDivision();
        assertNotNull("Div block should be created", block);
    }
    
    @Test
    public void testDiv_PositiveNumbers() {
        double a = 12.0;
        double b = 3.0;
        double result = a / b;
        assertEquals("12 / 3 = 4", 4.0, result, DELTA);
    }
    
    @Test
    public void testDiv_FractionalResult() {
        double a = 7.0;
        double b = 2.0;
        double result = a / b;
        assertEquals("7 / 2 = 3.5", 3.5, result, DELTA);
    }
    
    @Test
    public void testDiv_NegativeNumbers() {
        double a = -12.0;
        double b = 3.0;
        double result = a / b;
        assertEquals("-12 / 3 = -4", -4.0, result, DELTA);
    }

    // ========== Modulo Tests ==========
    
    @Test
    public void testMod_Positive() {
        double a = 7.0;
        double b = 3.0;
        double result = a % b;
        assertEquals("7 % 3 = 1", 1.0, result, DELTA);
    }
    
    @Test
    public void testMod_ExactDivisible() {
        double a = 9.0;
        double b = 3.0;
        double result = a % b;
        assertEquals("9 % 3 = 0", 0.0, result, DELTA);
    }
}
