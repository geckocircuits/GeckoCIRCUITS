/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package ch.technokrat.gecko.geckocircuits.allg;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Tests for FormatJTextField - number formatting and parsing in text field.
 * Tests focus on numeric value handling and field state management.
 */
public class FormatJTextFieldTest {

    private FormatJTextField field;
    private static final double DELTA = 1e-9;

    @Before
    public void setUp() {
        field = new FormatJTextField();
    }

    // ====================================================
    // Constructor Tests
    // ====================================================

    @Test
    public void testConstructor_Default() {
        FormatJTextField newField = new FormatJTextField();
        assertNotNull(newField);
    }

    @Test
    public void testConstructor_WithString() {
        FormatJTextField newField = new FormatJTextField("test");
        assertEquals("test", newField.getText());
    }

    @Test
    public void testConstructor_WithDouble() {
        FormatJTextField newField = new FormatJTextField(42.0);
        assertNotNull(newField);
        String text = newField.getText();
        assertNotNull(text);
        assertTrue("Should contain numeric value", text.length() > 0);
    }

    @Test
    public void testConstructor_WithDoubleAndDigits() {
        FormatJTextField newField = new FormatJTextField(123.456, 2);
        assertNotNull(newField);
        String text = newField.getText();
        assertNotNull(text);
    }

    @Test
    public void testConstructor_WithDoubleAndPattern() {
        FormatJTextField newField = new FormatJTextField(100.0, TechFormat.FORMAT_AUTO);
        assertNotNull(newField);
    }

    // ====================================================
    // IsNumberOK Tests
    // ====================================================

    @Test
    public void testIsNumberOK_InitialState() {
        assertFalse("Initial state should indicate no valid number", field.isNumberOK());
    }

    @Test
    public void testIsNumberOK_ValidNumber() {
        field.setText("123");
        field.getNumberFromField();
        assertTrue("Should be OK after parsing valid number", field.isNumberOK());
    }

    @Test
    public void testIsNumberOK_InvalidNumber() {
        field.setText("not_a_number");
        try {
            field.getNumberFromField();
        } catch (RuntimeException e) {
            // Expected
        }
        assertFalse("Should not be OK after parsing invalid number", field.isNumberOK());
    }

    // ====================================================
    // SetNumberToField Tests - Single Value
    // ====================================================

    @Test
    public void testSetNumberToField_Integer() {
        field.setNumberToField(42.0);
        String text = field.getText();
        assertNotNull(text);
        assertTrue("Should contain text", text.length() > 0);
    }

    @Test
    public void testSetNumberToField_Decimal() {
        field.setNumberToField(3.14159);
        String text = field.getText();
        assertNotNull(text);
    }

    @Test
    public void testSetNumberToField_Negative() {
        field.setNumberToField(-100.5);
        String text = field.getText();
        assertNotNull(text);
        assertTrue("Should contain minus sign", text.contains("-"));
    }

    @Test
    public void testSetNumberToField_Zero() {
        field.setNumberToField(0.0);
        String text = field.getText();
        assertNotNull(text);
    }

    @Test
    public void testSetNumberToField_VerySmall() {
        field.setNumberToField(1e-12);
        String text = field.getText();
        assertNotNull(text);
        assertTrue("Should contain some representation", text.length() > 0);
    }

    @Test
    public void testSetNumberToField_VeryLarge() {
        field.setNumberToField(1e12);
        String text = field.getText();
        assertNotNull(text);
        assertTrue("Should contain some representation", text.length() > 0);
    }

    // ====================================================
    // SetNumberToField Tests - With Max Fraction Digits
    // ====================================================

    @Test
    public void testSetNumberToField_WithMaxFractionDigits_Zero() {
        field.setNumberToField(123.456789, 0);
        String text = field.getText();
        assertNotNull(text);
        // Should be formatted to integer
    }

    @Test
    public void testSetNumberToField_WithMaxFractionDigits_Two() {
        field.setNumberToField(123.456789, 2);
        String text = field.getText();
        assertNotNull(text);
    }

    @Test
    public void testSetNumberToField_WithMaxFractionDigits_Six() {
        field.setNumberToField(123.456789, 6);
        String text = field.getText();
        assertNotNull(text);
    }

    // ====================================================
    // GetNumberFromField Tests - Valid Inputs
    // ====================================================

    @Test
    public void testGetNumberFromField_SimpleInteger() {
        field.setText("100");
        double value = field.getNumberFromField();
        assertEquals(100.0, value, DELTA);
        assertTrue("Should be OK", field.isNumberOK());
    }

    @Test
    public void testGetNumberFromField_Decimal() {
        field.setText("42.5");
        double value = field.getNumberFromField();
        assertEquals(42.5, value, DELTA);
        assertTrue("Should be OK", field.isNumberOK());
    }

    @Test
    public void testGetNumberFromField_Negative() {
        field.setText("-50");
        double value = field.getNumberFromField();
        assertEquals(-50.0, value, DELTA);
    }

    @Test
    public void testGetNumberFromField_EngineeringNotation() {
        field.setText("1e3");
        double value = field.getNumberFromField();
        assertEquals(1000.0, value, DELTA);
    }

    @Test
    public void testGetNumberFromField_SIPrefix_Kilo() {
        field.setText("2k");
        double value = field.getNumberFromField();
        assertEquals(2000.0, value, DELTA);
    }

    @Test
    public void testGetNumberFromField_SIPrefix_Milli() {
        field.setText("500m");
        double value = field.getNumberFromField();
        assertEquals(0.5, value, DELTA);
    }

    @Test
    public void testGetNumberFromField_Empty() {
        field.setText("");
        double value = field.getNumberFromField();
        assertEquals(0.0, value, DELTA);
    }

    @Test
    public void testGetNumberFromField_Zero() {
        field.setText("0");
        double value = field.getNumberFromField();
        assertEquals(0.0, value, DELTA);
    }

    // ====================================================
    // GetNumberFromField Tests - Variable Indicator
    // ====================================================

    @Test
    public void testGetNumberFromField_VariablePrefix() {
        field.setText("$myVariable");
        double value = field.getNumberFromField();
        assertEquals(FormatJTextField.IS_VARIABLE, value, DELTA);
    }

    // ====================================================
    // GetNumberFromField Tests - With Apostrophe Filtering
    // ====================================================

    @Test
    public void testGetNumberFromField_WithApostrophes() {
        field.setText("1'000");  // European notation
        double value = field.getNumberFromField();
        assertEquals(1000.0, value, DELTA);
    }

    @Test
    public void testGetNumberFromField_MultipleApostrophes() {
        field.setText("1'234'567");
        double value = field.getNumberFromField();
        // After removing apostrophes: "1234567"
        assertEquals(1234567.0, value, DELTA);
    }

    // ====================================================
    // GetNumberFromField Tests - Invalid Inputs
    // ====================================================

    @Test(expected = RuntimeException.class)
    public void testGetNumberFromField_InvalidCharacter() {
        field.setText("12x34");
        field.getNumberFromField();
    }

    @Test(expected = RuntimeException.class)
    public void testGetNumberFromField_InvalidCharacter_MultipleApostrophes() {
        field.setText("xxx");
        field.getNumberFromField();
    }

    // ====================================================
    // SetTechFormatPattern Tests
    // ====================================================

    @Test
    public void testSetTechFormatPattern() {
        field.setTechFormatPattern("0.00");
        field.setNumberToField(123.456);
        // Should use the new pattern
        String text = field.getText();
        assertNotNull(text);
    }

    @Test
    public void testSetTechFormatPattern_Auto() {
        field.setTechFormatPattern(TechFormat.FORMAT_AUTO);
        field.setNumberToField(1000.0);
        String text = field.getText();
        assertNotNull(text);
    }

    // ====================================================
    // SetMaximumDigits Tests
    // ====================================================

    @Test
    public void testSetMaximumDigits() {
        field.setMaximumDigits(3);
        field.setNumberToField(123456.789);
        // Should limit digits
    }

    @Test
    public void testSetMaximumDigits_Zero() {
        field.setMaximumDigits(0);
        field.setNumberToField(123456.789);
    }

    // ====================================================
    // SetState Tests
    // ====================================================

    @Test
    public void testSetState_Enabled() {
        field.setState(true);
        assertTrue("Should be editable", field.isEditable());
        assertTrue("Should be enabled", field.isEnabled());
    }

    @Test
    public void testSetState_Disabled() {
        field.setState(false);
        assertFalse("Should not be editable", field.isEditable());
        assertFalse("Should not be enabled", field.isEnabled());
    }

    // ====================================================
    // Round-Trip Tests
    // ====================================================

    @Test
    public void testRoundTrip_SimpleNumber() {
        double original = 42.0;
        field.setNumberToField(original);
        String text = field.getText();

        field.setText(text);
        double retrieved = field.getNumberFromField();

        assertEquals(original, retrieved, 0.1);  // Allow some tolerance for formatting
    }

    @Test
    public void testRoundTrip_DecimalNumber() {
        double original = 3.14159;
        field.setNumberToField(original);
        String text = field.getText();

        field.setText(text);
        double retrieved = field.getNumberFromField();

        assertEquals(original, retrieved, 0.01);
    }

    @Test
    public void testRoundTrip_SmallNumber() {
        double original = 1e-6;
        field.setNumberToField(original);
        String text = field.getText();

        field.setText(text);
        double retrieved = field.getNumberFromField();

        assertEquals(original, retrieved, 1e-12);
    }

    @Test
    public void testRoundTrip_LargeNumber() {
        double original = 1e6;
        field.setNumberToField(original);
        String text = field.getText();

        field.setText(text);
        double retrieved = field.getNumberFromField();

        assertEquals(original, retrieved, 100.0);
    }

    // ====================================================
    // Consecutive Operations Tests
    // ====================================================

    @Test
    public void testConsecutiveSetOperations() {
        field.setNumberToField(10.0);
        assertEquals(10.0, field.getNumberFromField(), DELTA);

        field.setNumberToField(20.0);
        assertEquals(20.0, field.getNumberFromField(), DELTA);

        field.setNumberToField(30.0);
        assertEquals(30.0, field.getNumberFromField(), DELTA);
    }

    @Test
    public void testConsecutiveGetOperations() {
        field.setText("123");
        double value1 = field.getNumberFromField();
        double value2 = field.getNumberFromField();

        assertEquals(value1, value2, DELTA);
    }

    // ====================================================
    // IS_VARIABLE Constant Tests
    // ====================================================

    @Test
    public void testISVariableConstant() {
        assertTrue("IS_VARIABLE should be negative", FormatJTextField.IS_VARIABLE < 0);
        assertTrue("IS_VARIABLE should be a specific sentinel value",
                  FormatJTextField.IS_VARIABLE == -1e95);
    }

    // ====================================================
    // State Transitions Tests
    // ====================================================

    @Test
    public void testStateTransitions_DisabledThenEnabled() {
        field.setState(false);
        assertFalse("Should be disabled", field.isEnabled());

        field.setState(true);
        assertTrue("Should be enabled", field.isEnabled());
    }

    @Test
    public void testStateTransitions_EnabledThenDisabled() {
        field.setState(true);
        assertTrue("Should be enabled", field.isEnabled());

        field.setState(false);
        assertFalse("Should be disabled", field.isEnabled());
    }

    // ====================================================
    // Text Modification Tests
    // ====================================================

    @Test
    public void testTextChange_PreservesValue() {
        field.setText("100");
        double value1 = field.getNumberFromField();

        field.setText("100");
        double value2 = field.getNumberFromField();

        assertEquals(value1, value2, DELTA);
    }

    @Test
    public void testTextChange_UpdatesValue() {
        field.setText("100");
        double value1 = field.getNumberFromField();

        field.setText("200");
        double value2 = field.getNumberFromField();

        assertNotEquals(value1, value2, DELTA);
    }
}
