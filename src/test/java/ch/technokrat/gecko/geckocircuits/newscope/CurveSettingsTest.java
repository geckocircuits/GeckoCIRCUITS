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
 * Tests for CurveSettings - curve display configuration.
 */
public class CurveSettingsTest {

    private CurveSettings settings;
    private static final double DELTA = 1e-9;

    @Before
    public void setUp() {
        settings = new CurveSettings();
    }

    // ====================================================
    // Default Values Tests
    // ====================================================

    @Test
    public void testDefaultCurveShowPtSymbols_IsFalse() {
        assertFalse("Default show point symbols should be false", settings._curveShowPtSymbols);
    }

    @Test
    public void testDefaultCrvTransparency_Is0Point85() {
        assertEquals("Default transparency should be 0.85", 0.85, settings._crvTransparency, DELTA);
    }

    @Test
    public void testDefaultAverageSpan_Is1e6() {
        assertEquals("Default average span should be 1e-6", 1e-6, settings._averageSpan, DELTA);
    }

    @Test
    public void testDefaultLineType_IsConnectNeighbours() {
        assertEquals("Default line type should be CONNECT_NEIGHBOURS",
            GeckoLineType.CONNECT_NEIGHBOURS, settings._lineType);
    }

    @Test
    public void testDefaultCrvSymbFrequ_IsOne() {
        assertEquals("Default symbol frequency should be 1", 1, settings._crvSymbFrequ);
    }

    @Test
    public void testDefaultCrvFillDigitalCurves_IsTrue() {
        assertTrue("Default fill digital curves should be true", settings._crvFillDigitalCurves);
    }

    @Test
    public void testDefaultCurveLineStyle_IsSolidPlain() {
        assertEquals("Default curve line style should be SOLID_PLAIN",
            GeckoLineStyle.SOLID_PLAIN, settings._curveLineStyle);
    }

    @Test
    public void testDefaultCrvSymbShape_IsCross() {
        assertEquals("Default symbol shape should be CROSS",
            GeckoSymbol.CROSS, settings._crvSymbShape);
    }

    @Test
    public void testDefaultCrvFillingDigColor_IsLightGray() {
        assertEquals("Default digital fill color should be LIGHTGRAY",
            GeckoColor.LIGHTGRAY, settings._crvFillingDigColor);
    }

    // ====================================================
    // Curve Color Tests
    // ====================================================

    @Test
    public void testCurveColor_IsNotNull() {
        assertNotNull("Curve color should not be null", settings._curveColor);
    }

    @Test
    public void testSymbolColor_IsNotNull() {
        assertNotNull("Symbol color should not be null", settings._crvSymbFarbe);
    }

    @Test
    public void testSetCurveColor_UpdatesValue() {
        GeckoColor originalColor = settings._curveColor;
        settings._curveColor = GeckoColor.RED;
        assertEquals("Curve color should be updated to RED", GeckoColor.RED, settings._curveColor);
    }

    @Test
    public void testSetSymbolColor_UpdatesValue() {
        GeckoColor originalColor = settings._crvSymbFarbe;
        settings._crvSymbFarbe = GeckoColor.BLUE;
        assertEquals("Symbol color should be updated to BLUE", GeckoColor.BLUE, settings._crvSymbFarbe);
    }

    // ====================================================
    // Line Style Tests
    // ====================================================

    @Test
    public void testSetCurveLineStyle_SolidThin() {
        settings._curveLineStyle = GeckoLineStyle.SOLID_THIN;
        assertEquals("Curve line style should be SOLID_THIN",
            GeckoLineStyle.SOLID_THIN, settings._curveLineStyle);
    }

    @Test
    public void testSetCurveLineStyle_Dotted() {
        settings._curveLineStyle = GeckoLineStyle.DOTTED_PLAIN;
        assertEquals("Curve line style should be DOTTED_PLAIN",
            GeckoLineStyle.DOTTED_PLAIN, settings._curveLineStyle);
    }

    // ====================================================
    // Symbol Tests
    // ====================================================

    @Test
    public void testSetCrvSymbShape_Circle() {
        settings._crvSymbShape = GeckoSymbol.CIRCLE;
        assertEquals("Symbol shape should be CIRCLE", GeckoSymbol.CIRCLE, settings._crvSymbShape);
    }

    @Test
    public void testSetCrvSymbShape_Rect() {
        settings._crvSymbShape = GeckoSymbol.RECT;
        assertEquals("Symbol shape should be RECT", GeckoSymbol.RECT, settings._crvSymbShape);
    }

    @Test
    public void testSetCrvSymbShape_Triang() {
        settings._crvSymbShape = GeckoSymbol.TRIANG;
        assertEquals("Symbol shape should be TRIANG", GeckoSymbol.TRIANG, settings._crvSymbShape);
    }

    // ====================================================
    // Transparency Tests
    // ====================================================

    @Test
    public void testSetTransparency_Full() {
        settings._crvTransparency = 1.0;
        assertEquals("Transparency should be 1.0", 1.0, settings._crvTransparency, DELTA);
    }

    @Test
    public void testSetTransparency_Half() {
        settings._crvTransparency = 0.5;
        assertEquals("Transparency should be 0.5", 0.5, settings._crvTransparency, DELTA);
    }

    @Test
    public void testSetTransparency_Zero() {
        settings._crvTransparency = 0.0;
        assertEquals("Transparency should be 0.0", 0.0, settings._crvTransparency, DELTA);
    }

    @Test
    public void testTransparency_InValidRange() {
        // Test typical transparency values used in scope applications
        double[] validTransparencies = {0.0, 0.25, 0.5, 0.75, 0.85, 1.0};
        for (double transparency : validTransparencies) {
            settings._crvTransparency = transparency;
            assertEquals("Transparency should match set value",
                transparency, settings._crvTransparency, DELTA);
        }
    }

    // ====================================================
    // Average Span Tests
    // ====================================================

    @Test
    public void testSetAverageSpan_Microseconds() {
        settings._averageSpan = 1e-6;
        assertEquals("Average span should be 1e-6", 1e-6, settings._averageSpan, DELTA);
    }

    @Test
    public void testSetAverageSpan_Milliseconds() {
        settings._averageSpan = 1e-3;
        assertEquals("Average span should be 1e-3", 1e-3, settings._averageSpan, DELTA);
    }

    @Test
    public void testSetAverageSpan_Seconds() {
        settings._averageSpan = 1.0;
        assertEquals("Average span should be 1.0", 1.0, settings._averageSpan, DELTA);
    }

    // ====================================================
    // Digital Fill Tests
    // ====================================================

    @Test
    public void testSetCrvFillDigitalCurves_False() {
        settings._crvFillDigitalCurves = false;
        assertFalse("Fill digital curves should be false", settings._crvFillDigitalCurves);
    }

    @Test
    public void testSetCrvFillingDigColor_Red() {
        settings._crvFillingDigColor = GeckoColor.RED;
        assertEquals("Digital fill color should be RED", GeckoColor.RED, settings._crvFillingDigColor);
    }

    @Test
    public void testSetCrvFillingDigColor_Blue() {
        settings._crvFillingDigColor = GeckoColor.BLUE;
        assertEquals("Digital fill color should be BLUE", GeckoColor.BLUE, settings._crvFillingDigColor);
    }

    // ====================================================
    // Symbol Frequency Tests
    // ====================================================

    @Test
    public void testSetCrvSymbFrequ_CustomValue() {
        settings._crvSymbFrequ = 5;
        assertEquals("Symbol frequency should be 5", 5, settings._crvSymbFrequ);
    }

    @Test
    public void testSetCrvSymbFrequ_LargeValue() {
        settings._crvSymbFrequ = 100;
        assertEquals("Symbol frequency should be 100", 100, settings._crvSymbFrequ);
    }

    @Test
    public void testSetCrvSymbFrequ_One() {
        settings._crvSymbFrequ = 1;
        assertEquals("Symbol frequency should be 1", 1, settings._crvSymbFrequ);
    }

    // ====================================================
    // Show Symbols Tests
    // ====================================================

    @Test
    public void testSetCurveShowPtSymbols_True() {
        settings._curveShowPtSymbols = true;
        assertTrue("Show point symbols should be true", settings._curveShowPtSymbols);
    }

    @Test
    public void testSetCurveShowPtSymbols_Toggle() {
        assertFalse("Initial show symbols should be false", settings._curveShowPtSymbols);
        settings._curveShowPtSymbols = true;
        assertTrue("After toggle to true", settings._curveShowPtSymbols);
        settings._curveShowPtSymbols = false;
        assertFalse("After toggle back to false", settings._curveShowPtSymbols);
    }

    // ====================================================
    // Line Type Tests
    // ====================================================

    @Test
    public void testSetLineType_ConnectNeighbours() {
        settings._lineType = GeckoLineType.CONNECT_NEIGHBOURS;
        assertEquals("Line type should be CONNECT_NEIGHBOURS",
            GeckoLineType.CONNECT_NEIGHBOURS, settings._lineType);
    }

    @Test
    public void testSetLineType_Bar() {
        settings._lineType = GeckoLineType.BAR;
        assertEquals("Line type should be BAR",
            GeckoLineType.BAR, settings._lineType);
    }

    // ====================================================
    // State Persistence Tests
    // ====================================================

    @Test
    public void testMultipleStateChanges_AllIndependent() {
        settings._curveColor = GeckoColor.RED;
        settings._crvTransparency = 0.5;
        settings._curveShowPtSymbols = true;
        settings._crvSymbFrequ = 3;
        settings._averageSpan = 0.001;

        assertEquals("Curve color should remain RED", GeckoColor.RED, settings._curveColor);
        assertEquals("Transparency should remain 0.5", 0.5, settings._crvTransparency, DELTA);
        assertTrue("Show symbols should remain true", settings._curveShowPtSymbols);
        assertEquals("Symbol frequency should remain 3", 3, settings._crvSymbFrequ);
        assertEquals("Average span should remain 0.001", 0.001, settings._averageSpan, DELTA);
    }

    @Test
    public void testMultipleInstances_Independent() {
        CurveSettings settings2 = new CurveSettings();

        settings._crvTransparency = 0.3;
        settings2._crvTransparency = 0.7;

        assertEquals("First instance should have 0.3", 0.3, settings._crvTransparency, DELTA);
        assertEquals("Second instance should have 0.7", 0.7, settings2._crvTransparency, DELTA);
    }

    // ====================================================
    // Boundary Tests
    // ====================================================

    @Test
    public void testSetCrvSymbFrequ_MaxInt() {
        settings._crvSymbFrequ = Integer.MAX_VALUE;
        assertEquals("Symbol frequency should handle max int", Integer.MAX_VALUE, settings._crvSymbFrequ);
    }

    @Test
    public void testSetCrvSymbFrequ_MinInt() {
        settings._crvSymbFrequ = Integer.MIN_VALUE;
        assertEquals("Symbol frequency should handle min int", Integer.MIN_VALUE, settings._crvSymbFrequ);
    }

    @Test
    public void testSetAverageSpan_VerySmall() {
        settings._averageSpan = 1e-15;
        assertEquals("Average span should handle very small values", 1e-15, settings._averageSpan, DELTA);
    }

    @Test
    public void testSetAverageSpan_VeryLarge() {
        settings._averageSpan = 1e15;
        assertEquals("Average span should handle very large values", 1e15, settings._averageSpan, DELTA);
    }
}
