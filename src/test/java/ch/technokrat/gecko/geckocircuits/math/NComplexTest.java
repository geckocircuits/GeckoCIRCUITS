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
package ch.technokrat.gecko.geckocircuits.math;

import org.junit.Test;
import static org.junit.Assert.*;

public class NComplexTest {
    private static final float TOLERANCE = 1e-6f;

    // Constructor Tests
    @Test
    public void testConstructor_RealAndImaginary() {
        NComplex c = new NComplex(3.0f, 4.0f);
        assertEquals(3.0f, c.getRe(), TOLERANCE);
        assertEquals(4.0f, c.getIm(), TOLERANCE);
    }

    @Test
    public void testConstructor_RealOnly() {
        NComplex c = new NComplex(5.0f);
        assertEquals(5.0f, c.getRe(), TOLERANCE);
        assertEquals(0.0f, c.getIm(), TOLERANCE);
    }

    @Test
    public void testConstructor_Default() {
        NComplex c = new NComplex();
        assertEquals(0.0f, c.getRe(), TOLERANCE);
        assertEquals(0.0f, c.getIm(), TOLERANCE);
    }

    // Arithmetic Operations Tests
    @Test
    public void testAdd() {
        NComplex a = new NComplex(1.0f, 2.0f);
        NComplex b = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.add(a, b);
        assertEquals(4.0f, result.getRe(), TOLERANCE);
        assertEquals(6.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testSub() {
        NComplex a = new NComplex(5.0f, 7.0f);
        NComplex b = new NComplex(2.0f, 3.0f);
        NComplex result = NComplex.sub(a, b);
        assertEquals(3.0f, result.getRe(), TOLERANCE);
        assertEquals(4.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testMul() {
        // (1+2i)(3+4i) = 3+4i+6i+8i² = 3+10i-8 = -5+10i
        NComplex a = new NComplex(1.0f, 2.0f);
        NComplex b = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.mul(a, b);
        assertEquals(-5.0f, result.getRe(), TOLERANCE);
        assertEquals(10.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testDiv() {
        // (4+2i)/(1+i) = (4+2i)(1-i)/(1+1) = (4-4i+2i-2i²)/2 = (4-2i+2)/2 = (6-2i)/2 = 3-i
        NComplex a = new NComplex(4.0f, 2.0f);
        NComplex b = new NComplex(1.0f, 1.0f);
        NComplex result = NComplex.div(a, b);
        assertEquals(3.0f, result.getRe(), TOLERANCE);
        assertEquals(-1.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testConj() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.conj(a);
        assertEquals(3.0f, result.getRe(), TOLERANCE);
        assertEquals(-4.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testRCmul() {
        NComplex a = new NComplex(2.0f, 3.0f);
        NComplex result = NComplex.RCmul(2.0f, a);
        assertEquals(4.0f, result.getRe(), TOLERANCE);
        assertEquals(6.0f, result.getIm(), TOLERANCE);
    }

    // Advanced Operations Tests
    @Test
    public void testAbs() {
        // |3+4i| = sqrt(9+16) = sqrt(25) = 5
        NComplex a = new NComplex(3.0f, 4.0f);
        float result = NComplex.abs(a);
        assertEquals(5.0f, result, TOLERANCE);
    }

    @Test
    public void testSqrt() {
        // sqrt(3+4i) ≈ 2+i (since (2+i)² = 4+4i+i² = 4+4i-1 = 3+4i)
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.sqrt(a);
        assertEquals(2.0f, result.getRe(), 0.01f);
        assertEquals(1.0f, result.getIm(), 0.01f);
    }

    // Edge Cases Tests
    @Test
    public void testAdd_WithZero() {
        NComplex a = new NComplex(2.0f, 3.0f);
        NComplex zero = new NComplex(0.0f, 0.0f);
        NComplex result = NComplex.add(a, zero);
        assertEquals(2.0f, result.getRe(), TOLERANCE);
        assertEquals(3.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testMul_WithZero() {
        NComplex a = new NComplex(2.0f, 3.0f);
        NComplex zero = new NComplex(0.0f, 0.0f);
        NComplex result = NComplex.mul(a, zero);
        assertEquals(0.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testAbs_Zero() {
        NComplex zero = new NComplex(0.0f, 0.0f);
        float result = NComplex.abs(zero);
        assertEquals(0.0f, result, TOLERANCE);
    }

    @Test
    public void testAbs_PureReal() {
        NComplex a = new NComplex(4.0f, 0.0f);
        float result = NComplex.abs(a);
        assertEquals(4.0f, result, TOLERANCE);
    }

    @Test
    public void testAbs_PureImaginary() {
        NComplex a = new NComplex(0.0f, 3.0f);
        float result = NComplex.abs(a);
        assertEquals(3.0f, result, TOLERANCE);
    }

    @Test
    public void testSqrt_Zero() {
        NComplex zero = new NComplex(0.0f, 0.0f);
        NComplex result = NComplex.sqrt(zero);
        assertEquals(0.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testDiv_WithImaginaryDominant() {
        // Test the else branch in div where Math.abs(b.im) > Math.abs(b.re)
        NComplex a = new NComplex(2.0f, 4.0f);
        NComplex b = new NComplex(1.0f, 3.0f);
        NComplex result = NComplex.div(a, b);
        // Verify by multiplying back
        NComplex check = NComplex.mul(result, b);
        assertEquals(a.getRe(), check.getRe(), 0.01f);
        assertEquals(a.getIm(), check.getIm(), 0.01f);
    }

    @Test
    public void testSqrt_NegativeReal() {
        // sqrt(-4) = 2i
        NComplex a = new NComplex(-4.0f, 0.0f);
        NComplex result = NComplex.sqrt(a);
        assertEquals(0.0f, result.getRe(), 0.01f);
        assertEquals(2.0f, result.getIm(), 0.01f);
    }

    @Test
    public void testSqrt_NegativeImaginary() {
        // Test the negative imaginary branch in sqrt
        NComplex a = new NComplex(3.0f, -4.0f);
        NComplex result = NComplex.sqrt(a);
        // Verify by squaring the result
        NComplex check = NComplex.mul(result, result);
        assertEquals(a.getRe(), check.getRe(), 0.01f);
        assertEquals(a.getIm(), check.getIm(), 0.01f);
    }

    // Equality & String Tests
    @Test
    public void testEquals_Same() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex b = new NComplex(3.0f, 4.0f);
        assertTrue(a.equals(b));
    }

    @Test
    public void testEquals_Different() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex b = new NComplex(5.0f, 6.0f);
        assertFalse(a.equals(b));
    }

    @Test
    public void testEquals_DifferentType() {
        NComplex a = new NComplex(3.0f, 4.0f);
        String b = "not a complex number";
        assertFalse(a.equals(b));
    }

    @Test
    public void testHashCode_Consistent() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex b = new NComplex(3.0f, 4.0f);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testToString() {
        NComplex a = new NComplex(3.0f, 4.0f);
        String result = a.toString();
        assertTrue(result.contains("3.0"));
        assertTrue(result.contains("4.0"));
        assertTrue(result.contains("i"));
    }

    @Test
    public void testNicePrint_BothNonZero() {
        NComplex a = new NComplex(3.0f, 4.0f);
        String result = a.nicePrint();
        assertNotNull(result);
        assertTrue(result.contains("i"));
    }

    @Test
    public void testNicePrint_RealOnly() {
        NComplex a = new NComplex(5.0f, 0.0f);
        String result = a.nicePrint();
        assertNotNull(result);
    }

    @Test
    public void testNicePrint_ImaginaryOnly() {
        NComplex a = new NComplex(0.0f, 3.0f);
        String result = a.nicePrint();
        assertNotNull(result);
        assertTrue(result.contains("i"));
    }

    // ==================== ADDITIONAL EDGE CASE TESTS ====================

    @Test
    public void testDiv_ByZero() {
        // Division by zero should handle gracefully (may return NaN or Infinity)
        NComplex a = new NComplex(4.0f, 2.0f);
        NComplex zero = new NComplex(0.0f, 0.0f);
        NComplex result = NComplex.div(a, zero);
        assertNotNull(result);
        // Result should be infinity or NaN
        assertTrue(Float.isInfinite(result.getRe()) || Float.isNaN(result.getRe()) ||
                   Float.isInfinite(result.getIm()) || Float.isNaN(result.getIm()));
    }

    @Test
    public void testMul_PureImaginaryNumbers() {
        // i * i = -1
        NComplex i1 = new NComplex(0.0f, 1.0f);
        NComplex i2 = new NComplex(0.0f, 1.0f);
        NComplex result = NComplex.mul(i1, i2);
        assertEquals(-1.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testMul_PureImaginaryWithReal() {
        // (2i) * (3+4i) = 6i + 8i² = 6i - 8 = -8 + 6i
        NComplex a = new NComplex(0.0f, 2.0f);
        NComplex b = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.mul(a, b);
        assertEquals(-8.0f, result.getRe(), TOLERANCE);
        assertEquals(6.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testAbs_VeryLargeNumbers() {
        // |1e6 + 1e6i| = sqrt(1e12 + 1e12) = sqrt(2e12)
        NComplex a = new NComplex(1e6f, 1e6f);
        float result = NComplex.abs(a);
        assertEquals((float)Math.sqrt(2e12), result, 1e5f);
    }

    @Test
    public void testAbs_VerySmallNumbers() {
        // |1e-6 + 1e-6i| = sqrt(1e-12 + 1e-12)
        NComplex a = new NComplex(1e-6f, 1e-6f);
        float result = NComplex.abs(a);
        assertTrue(result > 0);
        assertTrue(result < 1e-5f);
    }

    @Test
    public void testEquals_WithZero() {
        NComplex a = new NComplex(0.0f, 0.0f);
        NComplex b = new NComplex(0.0f, 0.0f);
        assertTrue(a.equals(b));
    }

    @Test
    public void testEquals_RealOnlyDifferent() {
        NComplex a = new NComplex(5.0f, 0.0f);
        NComplex b = new NComplex(3.0f, 0.0f);
        assertFalse(a.equals(b));
    }

    @Test
    public void testHashCode_DifferentValues() {
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex b = new NComplex(5.0f, 6.0f);
        // Different values should (usually) have different hash codes
        // Though hash collisions are possible, they should be rare
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testSqrt_PureRealPositive() {
        // sqrt(4.0) = 2.0
        NComplex a = new NComplex(4.0f, 0.0f);
        NComplex result = NComplex.sqrt(a);
        assertEquals(2.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testSqrt_ComplexNumber_Verification() {
        // Test by squaring the result to verify correctness
        NComplex a = new NComplex(2.0f, 3.0f);
        NComplex sqrtA = NComplex.sqrt(a);
        NComplex squared = NComplex.mul(sqrtA, sqrtA);
        
        // squared should equal a (within tolerance)
        assertEquals(a.getRe(), squared.getRe(), 0.01f);
        assertEquals(a.getIm(), squared.getIm(), 0.01f);
    }

    @Test
    public void testConj_RealOnly() {
        NComplex a = new NComplex(5.0f, 0.0f);
        NComplex result = NComplex.conj(a);
        assertEquals(5.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testConj_NegativeImaginary() {
        NComplex a = new NComplex(3.0f, -4.0f);
        NComplex result = NComplex.conj(a);
        assertEquals(3.0f, result.getRe(), TOLERANCE);
        assertEquals(4.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testRCmul_ByZero() {
        NComplex a = new NComplex(2.0f, 3.0f);
        NComplex result = NComplex.RCmul(0.0f, a);
        assertEquals(0.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testRCmul_ByOne() {
        NComplex a = new NComplex(2.0f, 3.0f);
        NComplex result = NComplex.RCmul(1.0f, a);
        assertEquals(2.0f, result.getRe(), TOLERANCE);
        assertEquals(3.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testRCmul_ByNegative() {
        NComplex a = new NComplex(2.0f, 3.0f);
        NComplex result = NComplex.RCmul(-1.0f, a);
        assertEquals(-2.0f, result.getRe(), TOLERANCE);
        assertEquals(-3.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testDiv_RealByComplex() {
        // 4 / (1+i) = 4(1-i) / 2 = 2(1-i) = 2-2i
        NComplex a = new NComplex(4.0f, 0.0f);
        NComplex b = new NComplex(1.0f, 1.0f);
        NComplex result = NComplex.div(a, b);
        assertEquals(2.0f, result.getRe(), TOLERANCE);
        assertEquals(-2.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testDiv_SelfDivision() {
        // z / z = 1
        NComplex a = new NComplex(3.0f, 4.0f);
        NComplex result = NComplex.div(a, a);
        assertEquals(1.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testSub_ZeroResult() {
        // 5+3i - 5-3i = 0
        NComplex a = new NComplex(5.0f, 3.0f);
        NComplex b = new NComplex(5.0f, 3.0f);
        NComplex result = NComplex.sub(a, b);
        assertEquals(0.0f, result.getRe(), TOLERANCE);
        assertEquals(0.0f, result.getIm(), TOLERANCE);
    }

    @Test
    public void testNicePrint_Zero() {
        NComplex a = new NComplex(0.0f, 0.0f);
        String result = a.nicePrint();
        assertNotNull(result);
        // Should contain representation of zero
        assertTrue(result.length() > 0);
    }
}
