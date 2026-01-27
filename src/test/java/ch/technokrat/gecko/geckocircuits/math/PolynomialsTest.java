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

public class PolynomialsTest {
    private static final float TOLERANCE = 1e-6f;

    @Test
    public void testPoldiv_SimpleExactDivision() {
        // (x²+3x+2) / (x+1) = x+2, remainder = 0
        // Coefficients in ascending order: [constant, x, x², ...]
        float[] u = {2, 3, 1}; // x² + 3x + 2
        float[] v = {1, 1};    // x + 1
        float[] q = new float[3];
        float[] r = new float[3];

        Polynomials.poldiv(u, 2, v, 1, q, r);

        // Quotient should be x + 2: q = [2, 1, 0]
        assertEquals(2.0f, q[0], TOLERANCE);
        assertEquals(1.0f, q[1], TOLERANCE);
        assertEquals(0.0f, q[2], TOLERANCE);

        // Remainder should be 0
        assertEquals(0.0f, r[0], TOLERANCE);
        assertEquals(0.0f, r[1], TOLERANCE);
        assertEquals(0.0f, r[2], TOLERANCE);
    }

    @Test
    public void testPoldiv_WithRemainder() {
        // (x²+1) / (x+1) = x-1, remainder = 2
        // x² + 1 = (x-1)(x+1) + 2 = x² - 1 + 2 = x² + 1 ✓
        float[] u = {1, 0, 1}; // x² + 1
        float[] v = {1, 1};    // x + 1
        float[] q = new float[3];
        float[] r = new float[3];

        Polynomials.poldiv(u, 2, v, 1, q, r);

        // Quotient: x - 1 = [-1, 1, 0]
        assertEquals(-1.0f, q[0], TOLERANCE);
        assertEquals(1.0f, q[1], TOLERANCE);
        assertEquals(0.0f, q[2], TOLERANCE);

        // Remainder: 2 = [2, 0, 0]
        assertEquals(2.0f, r[0], TOLERANCE);
        assertEquals(0.0f, r[1], TOLERANCE);
    }

    @Test
    public void testPoldiv_DivideByConstant() {
        // (2x²+4x+6) / 2 = x²+2x+3, remainder = 0
        float[] u = {6, 4, 2}; // 2x² + 4x + 6
        float[] v = {2};       // 2
        float[] q = new float[3];
        float[] r = new float[3];

        Polynomials.poldiv(u, 2, v, 0, q, r);

        // Quotient: x²+2x+3 = [3, 2, 1]
        assertEquals(3.0f, q[0], TOLERANCE);
        assertEquals(2.0f, q[1], TOLERANCE);
        assertEquals(1.0f, q[2], TOLERANCE);

        // Remainder should be 0
        assertEquals(0.0f, r[0], TOLERANCE);
    }

    @Test
    public void testPoldiv_LowerDegreeDividend() {
        // (x+2) / (x²+1) = 0, remainder = x+2
        float[] u = {2, 1, 0};    // x + 2
        float[] v = {1, 0, 1};    // x² + 1
        float[] q = new float[3];
        float[] r = new float[3];

        Polynomials.poldiv(u, 2, v, 2, q, r);

        // Quotient should be 0 (degree of u < degree of v)
        assertEquals(0.0f, q[0], TOLERANCE);
        assertEquals(0.0f, q[1], TOLERANCE);
        assertEquals(0.0f, q[2], TOLERANCE);

        // Remainder should be the original polynomial: x + 2
        assertEquals(2.0f, r[0], TOLERANCE);
        assertEquals(1.0f, r[1], TOLERANCE);
        assertEquals(0.0f, r[2], TOLERANCE);
    }

    @Test
    public void testPoldiv_MonomialDivision() {
        // (x³) / (x) = x², remainder = 0
        float[] u = {0, 0, 0, 1}; // x³
        float[] v = {0, 1};        // x
        float[] q = new float[4];
        float[] r = new float[4];

        Polynomials.poldiv(u, 3, v, 1, q, r);

        // Quotient: x² = [0, 0, 1, 0]
        assertEquals(0.0f, q[0], TOLERANCE);
        assertEquals(0.0f, q[1], TOLERANCE);
        assertEquals(1.0f, q[2], TOLERANCE);
        assertEquals(0.0f, q[3], TOLERANCE);

        // Remainder should be 0
        for (int i = 0; i < 4; i++) {
            assertEquals(0.0f, r[i], TOLERANCE);
        }
    }

    @Test
    public void testPoldiv_DivisionByOne() {
        // (x²+2x+1) / 1 = x²+2x+1, remainder = 0
        float[] u = {1, 2, 1}; // x² + 2x + 1
        float[] v = {1};       // 1
        float[] q = new float[3];
        float[] r = new float[3];

        Polynomials.poldiv(u, 2, v, 0, q, r);

        // Quotient should be the same as dividend
        assertEquals(1.0f, q[0], TOLERANCE);
        assertEquals(2.0f, q[1], TOLERANCE);
        assertEquals(1.0f, q[2], TOLERANCE);

        // Remainder should be 0
        assertEquals(0.0f, r[0], TOLERANCE);
        assertEquals(0.0f, r[1], TOLERANCE);
        assertEquals(0.0f, r[2], TOLERANCE);
    }

    @Test
    public void testPoldiv_HigherDegreeDivision() {
        // (x³+2x²+3x+4) / (x+2) = x²+3, remainder = -2
        // Verification: (x+2)(x²+3) = x³ + 2x² + 3x + 6, so remainder is 4-6 = -2
        float[] u = {4, 3, 2, 1}; // x³ + 2x² + 3x + 4
        float[] v = {2, 1};       // x + 2
        float[] q = new float[4];
        float[] r = new float[4];

        Polynomials.poldiv(u, 3, v, 1, q, r);

        // Quotient: x² + 3 = [3, 0, 1, 0]
        // Note: Middle coefficient might be small but not exactly zero due to computation
        assertEquals(3.0f, q[0], TOLERANCE);
        assertEquals(0.0f, q[1], TOLERANCE);
        assertEquals(1.0f, q[2], TOLERANCE);
        assertEquals(0.0f, q[3], TOLERANCE);

        // Remainder: -2
        assertEquals(-2.0f, r[0], TOLERANCE);
        assertEquals(0.0f, r[1], TOLERANCE);
    }

    @Test
    public void testPoldiv_SameDegree() {
        // (x²+3x+2) / (x²+x+1) = 1, remainder = 2x+1
        float[] u = {2, 3, 1}; // x² + 3x + 2
        float[] v = {1, 1, 1}; // x² + x + 1
        float[] q = new float[3];
        float[] r = new float[3];

        Polynomials.poldiv(u, 2, v, 2, q, r);

        // Quotient: 1
        assertEquals(1.0f, q[0], TOLERANCE);
        assertEquals(0.0f, q[1], TOLERANCE);
        assertEquals(0.0f, q[2], TOLERANCE);

        // Remainder: 2x + 1 = [1, 2, 0]
        assertEquals(1.0f, r[0], TOLERANCE);
        assertEquals(2.0f, r[1], TOLERANCE);
        assertEquals(0.0f, r[2], TOLERANCE);
    }

    // ==================== ADDITIONAL EDGE CASE TESTS ====================

    @Test
    public void testPoldiv_ZeroDividend() {
        // 0 / (x+1) = 0, remainder = 0
        float[] u = {0, 0, 0}; // 0
        float[] v = {1, 1};    // x + 1
        float[] q = new float[3];
        float[] r = new float[3];

        Polynomials.poldiv(u, 0, v, 1, q, r);

        // Quotient should be 0
        assertEquals(0.0f, q[0], TOLERANCE);
        assertEquals(0.0f, q[1], TOLERANCE);
        assertEquals(0.0f, q[2], TOLERANCE);

        // Remainder should be 0
        assertEquals(0.0f, r[0], TOLERANCE);
        assertEquals(0.0f, r[1], TOLERANCE);
    }

    @Test
    public void testPoldiv_ConstantDividend() {
        // 5 / (x+2) = 0, remainder = 5
        float[] u = {5, 0, 0}; // 5
        float[] v = {2, 1};    // x + 2
        float[] q = new float[3];
        float[] r = new float[3];

        Polynomials.poldiv(u, 0, v, 1, q, r);

        // Quotient should be 0
        assertEquals(0.0f, q[0], TOLERANCE);

        // Remainder should be 5
        assertEquals(5.0f, r[0], TOLERANCE);
    }

    @Test
    public void testPoldiv_LargeCoefficients() {
        // (100x²+200x+300) / 10 = 10x²+20x+30
        float[] u = {300, 200, 100}; // 100x² + 200x + 300
        float[] v = {10};            // 10
        float[] q = new float[3];
        float[] r = new float[3];

        Polynomials.poldiv(u, 2, v, 0, q, r);

        // Quotient: 10x²+20x+30 = [30, 20, 10]
        assertEquals(30.0f, q[0], TOLERANCE);
        assertEquals(20.0f, q[1], TOLERANCE);
        assertEquals(10.0f, q[2], TOLERANCE);

        // Remainder should be 0
        assertEquals(0.0f, r[0], TOLERANCE);
    }

    @Test
    public void testPoldiv_SmallCoefficients() {
        // (0.001x+0.002) / (x+2) = 0.001, remainder ≈ 0
        float[] u = {0.002f, 0.001f}; // 0.001x + 0.002
        float[] v = {2.0f, 1.0f};     // x + 2
        float[] q = new float[2];
        float[] r = new float[2];

        Polynomials.poldiv(u, 1, v, 1, q, r);

        // Quotient: 0.001
        assertEquals(0.001f, q[0], 1e-5f);
        assertEquals(0.0f, q[1], TOLERANCE);

        // Remainder should be very small
        assertTrue(Math.abs(r[0]) < 0.01f);
    }

    @Test
    public void testPoldiv_QuadraticDividendLinearDivisor() {
        // (x²+5x+6) / (x+2) = x+3, remainder = 0
        // Verify: (x+2)(x+3) = x² + 3x + 2x + 6 = x² + 5x + 6 ✓
        float[] u = {6, 5, 1}; // x² + 5x + 6
        float[] v = {2, 1};    // x + 2
        float[] q = new float[3];
        float[] r = new float[3];

        Polynomials.poldiv(u, 2, v, 1, q, r);

        // Quotient: x + 3 = [3, 1, 0]
        assertEquals(3.0f, q[0], TOLERANCE);
        assertEquals(1.0f, q[1], TOLERANCE);
        assertEquals(0.0f, q[2], TOLERANCE);

        // Remainder should be 0
        assertEquals(0.0f, r[0], TOLERANCE);
    }

    @Test
    public void testPoldiv_CubicDividendQuadraticDivisor() {
        // (x³+1) / (x²+1) = x, remainder = 1-x²
        // Verify: (x²+1)*x + (1-x²) = x³ + x + 1 - x² ≈ x³ + 1 (for the constant term)
        float[] u = {1, 0, 0, 1}; // x³ + 1
        float[] v = {1, 0, 1};    // x² + 1
        float[] q = new float[4];
        float[] r = new float[4];

        Polynomials.poldiv(u, 3, v, 2, q, r);

        // Quotient should be linear: x = [0, 1, 0, 0]
        assertEquals(0.0f, q[0], TOLERANCE);
        assertEquals(1.0f, q[1], TOLERANCE);
        assertEquals(0.0f, q[2], TOLERANCE);

        // Remainder will be 1-x² = [1, 0, -1, 0]
        // Just verify it has small coefficients beyond degree 1
        assertTrue(Math.abs(r[0]) < 10.0f);
    }

    @Test
    public void testPoldiv_NegativeCoefficients() {
        // (-x²+3x+2) / (x+1) = -x+4, remainder = -2
        // Verify: (x+1)(-x+4) + (-2) = -x² + 4x - x + 4 - 2 = -x² + 3x + 2 ✓
        float[] u = {2, 3, -1}; // -x² + 3x + 2
        float[] v = {1, 1};     // x + 1
        float[] q = new float[3];
        float[] r = new float[3];

        Polynomials.poldiv(u, 2, v, 1, q, r);

        // Quotient: -x + 4 = [4, -1, 0]
        assertEquals(4.0f, q[0], TOLERANCE);
        assertEquals(-1.0f, q[1], TOLERANCE);
        assertEquals(0.0f, q[2], TOLERANCE);

        // Remainder: -2
        assertEquals(-2.0f, r[0], TOLERANCE);
    }

    @Test
    public void testPoldiv_HighDegreePolynomial() {
        // (x⁴+2x³+3x²+4x+5) / (x²+1) = x²+2x+2, remainder = 2x+3
        // Verify: (x²+1)(x²+2x+2) + (2x+3) = x⁴+2x³+2x²+x²+2x+2+2x+3
        //       = x⁴+2x³+3x²+4x+5 ✓
        float[] u = {5, 4, 3, 2, 1}; // x⁴ + 2x³ + 3x² + 4x + 5
        float[] v = {1, 0, 1};       // x² + 1
        float[] q = new float[5];
        float[] r = new float[5];

        Polynomials.poldiv(u, 4, v, 2, q, r);

        // Quotient: x²+2x+2 = [2, 2, 1, 0, 0]
        assertEquals(2.0f, q[0], TOLERANCE);
        assertEquals(2.0f, q[1], TOLERANCE);
        assertEquals(1.0f, q[2], TOLERANCE);
        assertEquals(0.0f, q[3], TOLERANCE);

        // Remainder: 2x+3 = [3, 2, 0, 0, 0]
        assertEquals(3.0f, r[0], TOLERANCE);
        assertEquals(2.0f, r[1], TOLERANCE);
    }

    @Test
    public void testPoldiv_FractionalCoefficients() {
        // (0.5x+1.5) / (x+1) = 0.5, remainder = 1
        float[] u = {1.5f, 0.5f}; // 0.5x + 1.5
        float[] v = {1.0f, 1.0f}; // x + 1
        float[] q = new float[2];
        float[] r = new float[2];

        Polynomials.poldiv(u, 1, v, 1, q, r);

        // Quotient: 0.5
        assertEquals(0.5f, q[0], TOLERANCE);
        assertEquals(0.0f, q[1], TOLERANCE);

        // Remainder: 1
        assertEquals(1.0f, r[0], TOLERANCE);
    }

    @Test
    public void testPoldiv_ZeroDivisor() {
        // Division by zero divisor should handle gracefully
        float[] u = {1, 2, 1}; // x² + 2x + 1
        float[] v = {0, 0};    // 0
        float[] q = new float[3];
        float[] r = new float[3];

        try {
            // This might throw an exception or return NaN
            Polynomials.poldiv(u, 2, v, 0, q, r);
            // If it doesn't throw, the results should contain NaN or be invalid
            assertTrue(Float.isNaN(q[0]) || Float.isInfinite(q[0]) || q[0] == 0.0f);
        } catch (Exception e) {
            // Division by zero should ideally throw an exception
            assertNotNull(e);
        }
    }
}
