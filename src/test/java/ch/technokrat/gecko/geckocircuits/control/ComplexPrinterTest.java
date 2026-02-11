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

import ch.technokrat.gecko.geckocircuits.math.NComplex;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for ComplexPrinter utility class.
 * Tests string representation of complex numbers.
 */
public class ComplexPrinterTest {

    @Test
    public void testComplexPrinterConstruction() {
        NComplex value = new NComplex(3.0f, 4.0f);
        ComplexPrinter printer = new ComplexPrinter(value);
        assertNotNull("ComplexPrinter should be created", printer);
    }

    @Test
    public void testComplexPrinterValueStorage() {
        NComplex value = new NComplex(5.0f, 2.0f);
        ComplexPrinter printer = new ComplexPrinter(value);
        assertSame("Stored value should be the same object", value, printer._value);
    }

    @Test
    public void testPureRealNumber() {
        NComplex value = new NComplex(5.0f, 0.0f);
        ComplexPrinter printer = new ComplexPrinter(value);
        String result = printer.toString();

        assertNotNull("Should return non-null string", result);
        assertTrue("Should contain real part", result.contains("5"));
        assertFalse("Should not contain imaginary unit for pure real", result.contains("i"));
    }

    @Test
    public void testPureRealNegative() {
        NComplex value = new NComplex(-3.0f, 0.0f);
        ComplexPrinter printer = new ComplexPrinter(value);
        String result = printer.toString();

        assertNotNull("Should return non-null string", result);
        assertTrue("Should contain negative real part", result.contains("-3"));
    }

    @Test
    public void testPureRealZero() {
        NComplex value = new NComplex(0.0f, 0.0f);
        ComplexPrinter printer = new ComplexPrinter(value);
        String result = printer.toString();

        assertNotNull("Should return non-null string", result);
        assertTrue("Zero should return '0' or '0.0'", result.equals("0") || result.equals("0.0"));
    }

    @Test
    public void testComplexNumberWithBothParts() {
        NComplex value = new NComplex(3.0f, 4.0f);
        ComplexPrinter printer = new ComplexPrinter(value);
        String result = printer.toString();

        assertNotNull("Should return non-null string", result);
        assertTrue("Should contain HTML tag", result.contains("<html>"));
        assertTrue("Should contain real part", result.contains("3"));
        assertTrue("Should contain imaginary part", result.contains("4"));
        assertTrue("Should contain imaginary unit", result.contains("i"));
        assertTrue("Should contain plus sign", result.contains("&plusmn;"));
    }

    @Test
    public void testComplexNumberWithNegativeImaginary() {
        NComplex value = new NComplex(2.0f, -5.0f);
        ComplexPrinter printer = new ComplexPrinter(value);
        String result = printer.toString();

        assertNotNull("Should return non-null string", result);
        assertTrue("Should contain HTML tag", result.contains("<html>"));
        assertTrue("Should contain real part", result.contains("2"));
        assertTrue("Should contain negative imaginary part", result.contains("-5"));
        assertTrue("Should contain imaginary unit", result.contains("i"));
    }

    @Test
    public void testComplexNumberWithBothNegative() {
        NComplex value = new NComplex(-1.0f, -1.0f);
        ComplexPrinter printer = new ComplexPrinter(value);
        String result = printer.toString();

        assertNotNull("Should return non-null string", result);
        assertTrue("Should contain HTML tag", result.contains("<html>"));
        assertTrue("Should contain negative values", result.contains("-1"));
        assertTrue("Should contain imaginary unit", result.contains("i"));
    }

    @Test
    public void testSmallComplexNumber() {
        NComplex value = new NComplex(0.1f, 0.2f);
        ComplexPrinter printer = new ComplexPrinter(value);
        String result = printer.toString();

        assertNotNull("Should return non-null string", result);
        assertTrue("Should contain HTML tag", result.contains("<html>"));
        assertTrue("Should contain real part", result.contains("0.1"));
        assertTrue("Should contain imaginary part", result.contains("0.2"));
    }

    @Test
    public void testLargeComplexNumber() {
        NComplex value = new NComplex(1000.0f, 2000.0f);
        ComplexPrinter printer = new ComplexPrinter(value);
        String result = printer.toString();

        assertNotNull("Should return non-null string", result);
        assertTrue("Should contain HTML tag", result.contains("<html>"));
        assertTrue("Should contain real part", result.contains("1000"));
        assertTrue("Should contain imaginary part", result.contains("2000"));
    }

    @Test
    public void testComplexNumberToStringFormat() {
        NComplex value = new NComplex(1.0f, 2.0f);
        ComplexPrinter printer = new ComplexPrinter(value);
        String result = printer.toString();

        // Should follow format: <html>real&plusmn;imaginaryi
        assertNotNull("Should return non-null string", result);
        assertTrue("Should be properly formatted HTML", result.startsWith("<html>"));
    }

    @Test
    public void testRealOnlyNotHTML() {
        // Real-only numbers should NOT contain HTML tags
        NComplex value = new NComplex(7.0f, 0.0f);
        ComplexPrinter printer = new ComplexPrinter(value);
        String result = printer.toString();

        assertNotNull("Should return non-null string", result);
        assertFalse("Real-only should not contain HTML", result.contains("<html>"));
        assertTrue("Real-only should return the number", result.equals("7") || result.equals("7.0"));
    }

    @Test
    public void testMultiplePrintersIndependent() {
        NComplex value1 = new NComplex(1.0f, 1.0f);
        NComplex value2 = new NComplex(2.0f, 2.0f);

        ComplexPrinter printer1 = new ComplexPrinter(value1);
        ComplexPrinter printer2 = new ComplexPrinter(value2);

        String result1 = printer1.toString();
        String result2 = printer2.toString();

        assertNotSame("Results should be different", result1, result2);
        assertTrue("First should contain 1", result1.contains("1"));
        assertTrue("Second should contain 2", result2.contains("2"));
    }

    @Test
    public void testDecimalValues() {
        NComplex value = new NComplex(1.5f, 2.7f);
        ComplexPrinter printer = new ComplexPrinter(value);
        String result = printer.toString();

        assertNotNull("Should return non-null string", result);
        assertTrue("Should contain real decimal", result.contains("1.5"));
        assertTrue("Should contain imaginary decimal", result.contains("2.7"));
    }

    @Test
    public void testImaginaryUnitAlways() {
        NComplex value = new NComplex(0.0f, 1.0f);
        ComplexPrinter printer = new ComplexPrinter(value);
        String result = printer.toString();

        assertNotNull("Should return non-null string", result);
        assertTrue("Should contain HTML", result.contains("<html>"));
        assertTrue("Should contain imaginary unit", result.contains("i"));
    }

    @Test
    public void testSpecialCharacterHandling() {
        NComplex value = new NComplex(3.0f, 4.0f);
        ComplexPrinter printer = new ComplexPrinter(value);
        String result = printer.toString();

        assertNotNull("Should return non-null string", result);
        // &plusmn; is HTML entity for plus-minus symbol
        assertTrue("Should use HTML entity for plus-minus", result.contains("&plusmn;"));
    }

    @Test
    public void testToStringMultipleCalls() {
        NComplex value = new NComplex(2.0f, 3.0f);
        ComplexPrinter printer = new ComplexPrinter(value);

        String result1 = printer.toString();
        String result2 = printer.toString();

        assertEquals("Multiple toString calls should return same result", result1, result2);
    }

    @Test
    public void testNullComplexValueHandling() {
        // Test with null to check error handling
        try {
            ComplexPrinter printer = new ComplexPrinter(null);
            // If it doesn't throw, that's fine, but toString should handle it gracefully or throw
            assertNull("Value should be null", printer._value);
        } catch (NullPointerException e) {
            // Expected behavior - NPE is acceptable for null input
            assertTrue("NPE is acceptable for null input", true);
        }
    }

    @Test
    public void testFloatPrecision() {
        // Test with float precision edge case
        NComplex value = new NComplex(1.0f / 3.0f, 2.0f / 3.0f);
        ComplexPrinter printer = new ComplexPrinter(value);
        String result = printer.toString();

        assertNotNull("Should handle float precision", result);
    }
}
