/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.allg;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Tests for TechFormat - engineering notation number formatting and parsing.
 * TechFormat handles SI prefixes (k, M, m, u, n, p) and engineering notation.
 */
public class TechFormatTest {

    private TechFormat techFormat;
    private static final double DELTA = 1e-9;
    private static final double PERCENT_DELTA = 0.01;  // 1% tolerance for formatting

    @Before
    public void setUp() {
        techFormat = new TechFormat();
    }

    // ====================================================
    // ParseT Tests - Basic Number Parsing
    // ====================================================

    @Test
    public void testParseT_Integer() {
        assertEquals(123.0, techFormat.parseT("123"), DELTA);
    }

    @Test
    public void testParseT_Decimal() {
        assertEquals(123.456, techFormat.parseT("123.456"), DELTA);
    }

    @Test
    public void testParseT_Negative() {
        assertEquals(-123.0, techFormat.parseT("-123"), DELTA);
    }

    @Test
    public void testParseT_ScientificNotation() {
        assertEquals(1.5e6, techFormat.parseT("1.5e6"), DELTA);
    }

    @Test
    public void testParseT_ScientificNotation_Negative() {
        assertEquals(1.5e-6, techFormat.parseT("1.5e-6"), DELTA);
    }

    @Test
    public void testParseT_Zero() {
        assertEquals(0.0, techFormat.parseT("0"), DELTA);
    }

    // ====================================================
    // ParseT Tests - SI Prefix Parsing (Engineering Notation)
    // ====================================================

    @Test
    public void testParseT_Kilo() {
        // 2k = 2000
        assertEquals(2000.0, techFormat.parseT("2k"), DELTA);
    }

    @Test
    public void testParseT_Kilo_WithDecimal() {
        // 2k2 = 2200
        assertEquals(2200.0, techFormat.parseT("2k2"), DELTA);
    }

    @Test
    public void testParseT_Mega() {
        // 5M = 5,000,000
        assertEquals(5e6, techFormat.parseT("5M"), DELTA);
    }

    @Test
    public void testParseT_Milli() {
        // 2m = 0.002
        assertEquals(0.002, techFormat.parseT("2m"), DELTA);
    }

    @Test
    public void testParseT_Milli_WithDecimal() {
        // 2m2 = 2.2e-3
        assertEquals(2.2e-3, techFormat.parseT("2m2"), DELTA);
    }

    @Test
    public void testParseT_Micro() {
        // 2u = 2e-6
        assertEquals(2e-6, techFormat.parseT("2u"), DELTA);
    }

    @Test
    public void testParseT_Micro_WithDecimal() {
        // 2u4 = 2.4e-6
        assertEquals(2.4e-6, techFormat.parseT("2u4"), DELTA);
    }

    @Test
    public void testParseT_Nano() {
        // 7n = 7e-9
        assertEquals(7e-9, techFormat.parseT("7n"), DELTA);
    }

    @Test
    public void testParseT_Pico() {
        // 3p = 3e-12
        assertEquals(3e-12, techFormat.parseT("3p"), DELTA);
    }

    // ====================================================
    // ParseT Tests - Common Component Values
    // ====================================================

    @Test
    public void testParseT_CommonResistorValue_10k() {
        assertEquals(10000.0, techFormat.parseT("10k"), DELTA);
    }

    @Test
    public void testParseT_CommonResistorValue_4k7() {
        assertEquals(4700.0, techFormat.parseT("4k7"), DELTA);
    }

    @Test
    public void testParseT_CommonCapacitorValue_100n() {
        assertEquals(100e-9, techFormat.parseT("100n"), DELTA);
    }

    @Test
    public void testParseT_CommonCapacitorValue_10u() {
        assertEquals(10e-6, techFormat.parseT("10u"), DELTA);
    }

    @Test
    public void testParseT_CommonInductorValue_1m() {
        assertEquals(0.001, techFormat.parseT("1m"), DELTA);
    }

    // ====================================================
    // FormatENG Tests - Engineering Notation Output
    // ====================================================

    @Test
    public void testFormatENG_SmallNumber() {
        String result = techFormat.formatENG(0.001, 4);
        assertNotNull(result);
        assertTrue("Should contain exponent", result.contains("e"));
    }

    @Test
    public void testFormatENG_LargeNumber() {
        String result = techFormat.formatENG(1000000.0, 4);
        assertNotNull(result);
        assertTrue("Should contain exponent", result.contains("e"));
    }

    @Test
    public void testFormatENG_Zero() {
        String result = techFormat.formatENG(0.0, 4);
        assertEquals("0", result);
    }

    @Test
    public void testFormatENG_One() {
        String result = techFormat.formatENG(1.0, 4);
        assertEquals("1", result);
    }

    @Test
    public void testFormatENG_NegativeNumber() {
        String result = techFormat.formatENG(-1000.0, 4);
        assertNotNull(result);
        assertTrue("Should start with minus", result.startsWith("-"));
    }

    @Test
    public void testFormatENG_Infinity() {
        String result = techFormat.formatENG(Double.POSITIVE_INFINITY, 4);
        assertNotNull(result);
        assertTrue("Should handle infinity", result.contains("Infinity"));
    }

    @Test
    public void testFormatENG_NaN() {
        String result = techFormat.formatENG(Double.NaN, 4);
        assertNotNull(result);
        assertTrue("Should handle NaN", result.contains("NaN"));
    }

    // ====================================================
    // FormatT Tests - Pattern-Based Formatting
    // ====================================================

    @Test
    public void testFormatT_AutoFormat() {
        String result = techFormat.formatT(1234.5, TechFormat.FORMAT_AUTO);
        assertNotNull(result);
    }

    @Test
    public void testFormatT_CustomPattern() {
        String result = techFormat.formatT(1234.567, "0.00");
        assertNotNull(result);
        assertTrue("Should format to 2 decimal places", result.contains("."));
    }

    // ====================================================
    // Round-Trip Tests (Parse then Format)
    // ====================================================

    @Test
    public void testRoundTrip_BasicNumber() {
        double original = 12345.678;
        String formatted = techFormat.formatENG(original, 6);
        double parsed = techFormat.parseT(formatted);
        
        assertEquals(original, parsed, original * PERCENT_DELTA);
    }

    @Test
    public void testRoundTrip_SmallNumber() {
        double original = 1.5e-9;
        String formatted = techFormat.formatENG(original, 6);
        double parsed = techFormat.parseT(formatted);
        
        assertEquals(original, parsed, Math.abs(original * PERCENT_DELTA));
    }

    @Test
    public void testRoundTrip_LargeNumber() {
        double original = 2.5e9;
        String formatted = techFormat.formatENG(original, 6);
        double parsed = techFormat.parseT(formatted);
        
        assertEquals(original, parsed, original * PERCENT_DELTA);
    }

    // ====================================================
    // Invalid Input Tests
    // ====================================================

    @Test(expected = NumberFormatException.class)
    public void testParseT_InvalidCharacter() {
        techFormat.parseT("12x34");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseT_MultipleDecimalPoints() {
        techFormat.parseT("12.34.56");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseT_MultiplePrefixes() {
        techFormat.parseT("2km");  // Can't have both k and m
    }

    @Test(expected = NumberFormatException.class)
    public void testParseT_TrailingExponent() {
        techFormat.parseT("123e");  // Incomplete exponent
    }

    // ====================================================
    // SetMaximumDigits Tests
    // ====================================================

    @Test
    public void testSetMaximumDigits_AffectsFormat() {
        techFormat.setMaximumDigits(2);
        String result = techFormat.formatENG(123.456789, 2);
        assertNotNull(result);
    }

    // ====================================================
    // Format Constants Tests
    // ====================================================

    @Test
    public void testFormatAuto_Constant() {
        assertEquals("AUTO", TechFormat.FORMAT_AUTO);
    }

    // ====================================================
    // Edge Case Tests
    // ====================================================

    @Test
    public void testParseT_VerySmallNumber() {
        assertEquals(1e-12, techFormat.parseT("1e-12"), 1e-18);
    }

    @Test
    public void testParseT_VeryLargeNumber() {
        assertEquals(1e12, techFormat.parseT("1e12"), 1e6);
    }

    @Test
    public void testFormatENG_VerySmallNumber() {
        String result = techFormat.formatENG(1e-15, 4);
        assertNotNull(result);
        assertTrue("Should format very small numbers", result.contains("e"));
    }
}
