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
package ch.technokrat.gecko.geckocircuits.newscope;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Tests for AxisDesignSettings - axis appearance and caption configuration.
 */
public class AxisDesignSettingsTest {

    private AxisDesignSettings settings;

    @Before
    public void setUp() {
        settings = new AxisDesignSettings();
    }

    // ====================================================
    // Default Values Tests
    // ====================================================

    @Test
    public void testDefaultAxisColor_IsBlack() {
        assertEquals("Default axis color should be BLACK",
            GeckoColor.BLACK, settings.getColor());
    }

    @Test
    public void testDefaultAxisStyle_IsSolidPlain() {
        assertEquals("Default axis style should be SOLID_PLAIN",
            GeckoLineStyle.SOLID_PLAIN, settings.getStroke());
    }

    @Test
    public void testDefaultAxisCaption_IsEmpty() {
        assertNotNull("Default axis caption should not be null", settings.getAchseBeschriftung());
        assertEquals("Default axis caption should be empty", "", settings.getAchseBeschriftung());
    }

    @Test
    public void testDefaultTransparency_IsOne() {
        assertEquals("Default transparency should be 1.0 (opaque)", 1.0f, settings.getTransparency(), 1e-6f);
    }

    // ====================================================
    // Axis Color Tests
    // ====================================================

    @Test
    public void testSetColor_Red() {
        settings.setColor(GeckoColor.RED);
        assertEquals("Axis color should be RED", GeckoColor.RED, settings.getColor());
    }

    @Test
    public void testSetColor_Blue() {
        settings.setColor(GeckoColor.BLUE);
        assertEquals("Axis color should be BLUE", GeckoColor.BLUE, settings.getColor());
    }

    @Test
    public void testSetColor_Green() {
        settings.setColor(GeckoColor.GREEN);
        assertEquals("Axis color should be GREEN", GeckoColor.GREEN, settings.getColor());
    }

    @Test
    public void testSetColor_Gray() {
        settings.setColor(GeckoColor.GRAY);
        assertEquals("Axis color should be GRAY", GeckoColor.GRAY, settings.getColor());
    }

    @Test
    public void testSetColor_BackToDefault() {
        settings.setColor(GeckoColor.RED);
        settings.setColor(GeckoColor.BLACK);
        assertEquals("Axis color should be back to BLACK", GeckoColor.BLACK, settings.getColor());
    }

    @Test
    public void testSetColor_AllColors() {
        GeckoColor[] colors = {GeckoColor.BLACK, GeckoColor.RED, GeckoColor.GREEN,
            GeckoColor.BLUE, GeckoColor.CYAN, GeckoColor.MAGENTA, GeckoColor.YELLOW};

        for (GeckoColor color : colors) {
            settings.setColor(color);
            assertEquals("Axis color consistency for " + color, color, settings.getColor());
        }
    }

    // ====================================================
    // Axis Line Style Tests
    // ====================================================

    @Test
    public void testSetStroke_SolidThin() {
        settings.setStroke(GeckoLineStyle.SOLID_THIN);
        assertEquals("Axis style should be SOLID_THIN",
            GeckoLineStyle.SOLID_THIN, settings.getStroke());
    }

    @Test
    public void testSetStroke_DashedPlain() {
        settings.setStroke(GeckoLineStyle.DOTTED_PLAIN);
        assertEquals("Axis style should be DASHED_PLAIN",
            GeckoLineStyle.DOTTED_PLAIN, settings.getStroke());
    }

    @Test
    public void testSetStroke_BackToDefault() {
        settings.setStroke(GeckoLineStyle.DOTTED_PLAIN);
        settings.setStroke(GeckoLineStyle.SOLID_PLAIN);
        assertEquals("Axis style should be back to SOLID_PLAIN",
            GeckoLineStyle.SOLID_PLAIN, settings.getStroke());
    }

    // ====================================================
    // Axis Caption Tests
    // ====================================================

    @Test
    public void testSetAchseBeschriftung_SimpleName() {
        settings.setAchseBeschriftung("Voltage");
        assertEquals("Axis caption should be 'Voltage'", "Voltage", settings.getAchseBeschriftung());
    }

    @Test
    public void testSetAchseBeschriftung_EmptyString() {
        settings.setAchseBeschriftung("Test");
        settings.setAchseBeschriftung("");
        assertEquals("Axis caption should be empty", "", settings.getAchseBeschriftung());
    }

    @Test
    public void testSetAchseBeschriftung_WithUnit() {
        settings.setAchseBeschriftung("Voltage [V]");
        assertEquals("Axis caption should include unit", "Voltage [V]", settings.getAchseBeschriftung());
    }

    @Test
    public void testSetAchseBeschriftung_LongName() {
        String longName = "This is a very long axis name with many characters";
        settings.setAchseBeschriftung(longName);
        assertEquals("Axis caption should handle long strings", longName, settings.getAchseBeschriftung());
    }

    @Test
    public void testSetAchseBeschriftung_SpecialCharacters() {
        String specialName = "Axis_1-Test@#$%";
        settings.setAchseBeschriftung(specialName);
        assertEquals("Axis caption should handle special characters", specialName, settings.getAchseBeschriftung());
    }

    @Test
    public void testSetAchseBeschriftung_WithGreekLetters() {
        String greekName = "Phase [°]";
        settings.setAchseBeschriftung(greekName);
        assertEquals("Axis caption should handle special symbols", greekName, settings.getAchseBeschriftung());
    }

    @Test
    public void testSetAchseBeschriftung_Numeric() {
        settings.setAchseBeschriftung("1.5");
        assertEquals("Axis caption should accept numeric values", "1.5", settings.getAchseBeschriftung());
    }

    @Test
    public void testSetAchseBeschriftung_MultipleUpdates() {
        settings.setAchseBeschriftung("First");
        assertEquals("First update", "First", settings.getAchseBeschriftung());

        settings.setAchseBeschriftung("Second");
        assertEquals("Second update", "Second", settings.getAchseBeschriftung());

        settings.setAchseBeschriftung("Third");
        assertEquals("Third update", "Third", settings.getAchseBeschriftung());
    }

    // ====================================================
    // Transparency Tests (from LineSettable interface)
    // ====================================================

    @Test
    public void testSetTransparency_HasNoEffect() {
        // Axis transparency cannot be set per the implementation
        settings.setTransparency(0.5f);
        assertEquals("Transparency should always remain 1.0", 1.0f, settings.getTransparency(), 1e-6f);
    }

    @Test
    public void testSetTransparency_Various() {
        float[] values = {0.0f, 0.25f, 0.5f, 0.75f, 1.0f};
        for (float value : values) {
            settings.setTransparency(value);
            assertEquals("Transparency should remain 1.0 regardless of input",
                1.0f, settings.getTransparency(), 1e-6f);
        }
    }

    // ====================================================
    // LineSettable Interface Tests
    // ====================================================

    @Test
    public void testLineSettable_GetColor() {
        LineSettable settable = settings;
        assertEquals("LineSettable color should match axis color",
            GeckoColor.BLACK, settable.getColor());
    }

    @Test
    public void testLineSettable_SetColor() {
        LineSettable settable = settings;
        settable.setColor(GeckoColor.RED);
        assertEquals("LineSettable should update axis color", GeckoColor.RED, settings.getColor());
    }

    @Test
    public void testLineSettable_GetStroke() {
        LineSettable settable = settings;
        assertEquals("LineSettable stroke should match axis style",
            GeckoLineStyle.SOLID_PLAIN, settable.getStroke());
    }

    @Test
    public void testLineSettable_SetStroke() {
        LineSettable settable = settings;
        settable.setStroke(GeckoLineStyle.DOTTED_PLAIN);
        assertEquals("LineSettable should update axis style",
            GeckoLineStyle.DOTTED_PLAIN, settings.getStroke());
    }

    @Test
    public void testLineSettable_GetTransparency() {
        LineSettable settable = settings;
        assertEquals("LineSettable transparency should be 1.0", 1.0f, settable.getTransparency(), 1e-6f);
    }

    // ====================================================
    // State Persistence Tests
    // ====================================================

    @Test
    public void testMultipleStateChanges_AllIndependent() {
        settings.setColor(GeckoColor.RED);
        settings.setStroke(GeckoLineStyle.DOTTED_PLAIN);
        settings.setAchseBeschriftung("Current [A]");

        assertEquals("Color should be RED", GeckoColor.RED, settings.getColor());
        assertEquals("Stroke should be DASHED_PLAIN",
            GeckoLineStyle.DOTTED_PLAIN, settings.getStroke());
        assertEquals("Caption should be 'Current [A]'", "Current [A]", settings.getAchseBeschriftung());

        // Change one property and verify others unchanged
        settings.setColor(GeckoColor.BLUE);
        assertEquals("Color should update to BLUE", GeckoColor.BLUE, settings.getColor());
        assertEquals("Stroke should remain DASHED_PLAIN",
            GeckoLineStyle.DOTTED_PLAIN, settings.getStroke());
        assertEquals("Caption should remain", "Current [A]", settings.getAchseBeschriftung());
    }

    @Test
    public void testMultipleInstances_Independent() {
        AxisDesignSettings settings2 = new AxisDesignSettings();

        settings.setColor(GeckoColor.RED);
        settings.setAchseBeschriftung("Voltage");

        settings2.setColor(GeckoColor.BLUE);
        settings2.setAchseBeschriftung("Current");

        assertEquals("First instance color", GeckoColor.RED, settings.getColor());
        assertEquals("First instance caption", "Voltage", settings.getAchseBeschriftung());
        assertEquals("Second instance color", GeckoColor.BLUE, settings2.getColor());
        assertEquals("Second instance caption", "Current", settings2.getAchseBeschriftung());
    }

    // ====================================================
    // Static Constants Tests
    // ====================================================

    @Test
    public void testZeroLineStyle_IsSolidThin() {
        assertEquals("Zero line style constant should be SOLID_THIN",
            GeckoLineStyle.SOLID_THIN, AxisDesignSettings.ZERO_LINE_STYLE);
    }

    @Test
    public void testZeroLineColor_IsLightGray() {
        assertEquals("Zero line color constant should be LIGHTGRAY",
            GeckoColor.LIGHTGRAY, AxisDesignSettings.ZERO_LINE_COL);
    }

    // ====================================================
    // Use Case Scenarios Tests
    // ====================================================

    @Test
    public void testScenario_ConfigureVoltageAxis() {
        settings.setColor(GeckoColor.RED);
        settings.setStroke(GeckoLineStyle.SOLID_PLAIN);
        settings.setAchseBeschriftung("Voltage [V]");

        assertEquals("Color configured", GeckoColor.RED, settings.getColor());
        assertEquals("Style configured", GeckoLineStyle.SOLID_PLAIN, settings.getStroke());
        assertEquals("Caption configured", "Voltage [V]", settings.getAchseBeschriftung());
    }

    @Test
    public void testScenario_ConfigureCurrentAxis() {
        settings.setColor(GeckoColor.BLUE);
        settings.setStroke(GeckoLineStyle.DOTTED_PLAIN);
        settings.setAchseBeschriftung("Current [A]");

        assertEquals("Color configured", GeckoColor.BLUE, settings.getColor());
        assertEquals("Style configured", GeckoLineStyle.DOTTED_PLAIN, settings.getStroke());
        assertEquals("Caption configured", "Current [A]", settings.getAchseBeschriftung());
    }

    @Test
    public void testScenario_ResetToDefaults() {
        // Modify settings
        settings.setColor(GeckoColor.RED);
        settings.setStroke(GeckoLineStyle.DOTTED_PLAIN);
        settings.setAchseBeschriftung("Custom");

        // Create new instance (effectively reset)
        AxisDesignSettings defaults = new AxisDesignSettings();

        assertEquals("Reset color", GeckoColor.BLACK, defaults.getColor());
        assertEquals("Reset stroke", GeckoLineStyle.SOLID_PLAIN, defaults.getStroke());
        assertEquals("Reset caption", "", defaults.getAchseBeschriftung());
    }

    // ====================================================
    // Boundary Tests
    // ====================================================

    @Test
    public void testSetAchseBeschriftung_VeryLongString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("A");
        }
        String veryLongName = sb.toString();
        settings.setAchseBeschriftung(veryLongName);
        assertEquals("Should handle very long strings", veryLongName, settings.getAchseBeschriftung());
    }

    @Test
    public void testSetAchseBeschriftung_SingleCharacter() {
        settings.setAchseBeschriftung("X");
        assertEquals("Should handle single character", "X", settings.getAchseBeschriftung());
    }

    @Test
    public void testSetAchseBeschriftung_WithWhitespace() {
        String withWhitespace = "  Axis With Spaces  ";
        settings.setAchseBeschriftung(withWhitespace);
        assertEquals("Should preserve whitespace", withWhitespace, settings.getAchseBeschriftung());
    }

    // ====================================================
    // Getter/Setter Consistency Tests
    // ====================================================

    @Test
    public void testGetSetConsistency_AllProperties() {
        settings.setColor(GeckoColor.MAGENTA);
        settings.setStroke(GeckoLineStyle.SOLID_THIN);
        settings.setAchseBeschriftung("Test Axis");

        assertEquals("Color consistency", GeckoColor.MAGENTA, settings.getColor());
        assertEquals("Stroke consistency", GeckoLineStyle.SOLID_THIN, settings.getStroke());
        assertEquals("Caption consistency", "Test Axis", settings.getAchseBeschriftung());
    }
}
