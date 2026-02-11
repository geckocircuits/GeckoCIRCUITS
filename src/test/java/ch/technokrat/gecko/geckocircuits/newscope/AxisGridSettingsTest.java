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
 * Tests for AxisGridSettings - grid line configuration.
 */
public class AxisGridSettingsTest {

    private AxisGridSettings settings;

    @Before
    public void setUp() {
        settings = new AxisGridSettings();
    }

    // ====================================================
    // Default Values Tests
    // ====================================================

    @Test
    public void testDefaultAutoGrids_IsTrue() {
        assertTrue("Default auto grids should be true", settings.isAutoGrids());
    }

    @Test
    public void testDefaultUserShowGridMin_IsFalse() {
        assertFalse("Default user show grid minor should be false", settings.isUserShowGridMin());
    }

    @Test
    public void testDefaultUserShowGridMaj_IsTrue() {
        assertTrue("Default user show grid major should be true", settings.isUserShowGridMaj());
    }

    @Test
    public void testDefaultColorGridMaj_IsLightGray() {
        assertEquals("Default major grid color should be LIGHTGRAY",
            GeckoColor.LIGHTGRAY, settings.getColorGridMaj());
    }

    @Test
    public void testDefaultColorGridMin_IsLightGray() {
        assertEquals("Default minor grid color should be LIGHTGRAY",
            GeckoColor.LIGHTGRAY, settings.getColorGridMin());
    }

    @Test
    public void testDefaultLinStyleGridMaj_IsSolidThin() {
        assertEquals("Default major grid line style should be SOLID_THIN",
            GeckoLineStyle.SOLID_THIN, settings.getLinStyleMaj());
    }

    @Test
    public void testDefaultLinStyleGridMin_IsSolidThin() {
        assertEquals("Default minor grid line style should be SOLID_THIN",
            GeckoLineStyle.SOLID_THIN, settings.getLinStyleMin());
    }

    // ====================================================
    // Auto Grids Tests
    // ====================================================

    @Test
    public void testSetAutoGrids_False() {
        settings.setAutoGrids(false);
        assertFalse("Auto grids should be false", settings.isAutoGrids());
    }

    @Test
    public void testSetAutoGrids_True() {
        settings.setAutoGrids(false);
        settings.setAutoGrids(true);
        assertTrue("Auto grids should be true", settings.isAutoGrids());
    }

    @Test
    public void testSetAutoGrids_Toggle() {
        assertTrue("Initial auto grids should be true", settings.isAutoGrids());
        settings.setAutoGrids(false);
        assertFalse("After toggle to false", settings.isAutoGrids());
        settings.setAutoGrids(true);
        assertTrue("After toggle back to true", settings.isAutoGrids());
    }

    // ====================================================
    // User Show Grid Tests
    // ====================================================

    @Test
    public void testSetUserShowGridMaj_True() {
        settings.setUserShowGridMaj(true);
        assertTrue("User show grid major should be true", settings.isUserShowGridMaj());
    }

    @Test
    public void testSetUserShowGridMaj_False() {
        settings.setUserShowGridMaj(false);
        assertFalse("User show grid major should be false", settings.isUserShowGridMaj());
    }

    @Test
    public void testSetUserShowGridMin_True() {
        settings.setUserShowGridMin(true);
        assertTrue("User show grid minor should be true", settings.isUserShowGridMin());
    }

    @Test
    public void testSetUserShowGridMin_False() {
        settings.setUserShowGridMin(false);
        assertFalse("User show grid minor should be false", settings.isUserShowGridMin());
    }

    // ====================================================
    // Line Style Tests
    // ====================================================

    @Test
    public void testSetLinStyleMaj_DashedPlain() {
        settings.setLinStyleMaj(GeckoLineStyle.DOTTED_PLAIN);
        assertEquals("Major grid line style should be DASHED_PLAIN",
            GeckoLineStyle.DOTTED_PLAIN, settings.getLinStyleMaj());
    }

    @Test
    public void testSetLinStyleMaj_SolidPlain() {
        settings.setLinStyleMaj(GeckoLineStyle.SOLID_PLAIN);
        assertEquals("Major grid line style should be SOLID_PLAIN",
            GeckoLineStyle.SOLID_PLAIN, settings.getLinStyleMaj());
    }

    @Test
    public void testSetLinStyleMin_DashedPlain() {
        settings.setLinStyleMin(GeckoLineStyle.DOTTED_PLAIN);
        assertEquals("Minor grid line style should be DASHED_PLAIN",
            GeckoLineStyle.DOTTED_PLAIN, settings.getLinStyleMin());
    }

    @Test
    public void testSetLinStyleMin_Independent() {
        settings.setLinStyleMaj(GeckoLineStyle.DOTTED_PLAIN);
        settings.setLinStyleMin(GeckoLineStyle.SOLID_THIN);

        assertEquals("Major style should be DASHED_PLAIN",
            GeckoLineStyle.DOTTED_PLAIN, settings.getLinStyleMaj());
        assertEquals("Minor style should be SOLID_THIN",
            GeckoLineStyle.SOLID_THIN, settings.getLinStyleMin());
    }

    // ====================================================
    // Color Tests
    // ====================================================

    @Test
    public void testSetColorGridMaj_Red() {
        settings.setColorGridMaj(GeckoColor.RED);
        assertEquals("Major grid color should be RED", GeckoColor.RED, settings.getColorGridMaj());
    }

    @Test
    public void testSetColorGridMaj_Blue() {
        settings.setColorGridMaj(GeckoColor.BLUE);
        assertEquals("Major grid color should be BLUE", GeckoColor.BLUE, settings.getColorGridMaj());
    }

    @Test
    public void testSetColorGridMaj_Black() {
        settings.setColorGridMaj(GeckoColor.BLACK);
        assertEquals("Major grid color should be BLACK", GeckoColor.BLACK, settings.getColorGridMaj());
    }

    @Test
    public void testSetColorGridMin_Red() {
        settings.setColorGridMin(GeckoColor.RED);
        assertEquals("Minor grid color should be RED", GeckoColor.RED, settings.getColorGridMin());
    }

    @Test
    public void testSetColorGridMin_Gray() {
        settings.setColorGridMin(GeckoColor.GRAY);
        assertEquals("Minor grid color should be GRAY", GeckoColor.GRAY, settings.getColorGridMin());
    }

    @Test
    public void testSetColorGrid_IndependentColors() {
        settings.setColorGridMaj(GeckoColor.RED);
        settings.setColorGridMin(GeckoColor.BLUE);

        assertEquals("Major color should be RED", GeckoColor.RED, settings.getColorGridMaj());
        assertEquals("Minor color should be BLUE", GeckoColor.BLUE, settings.getColorGridMin());
    }

    // ====================================================
    // Auto Blending Tests
    // ====================================================

    @Test
    public void testBlendeEventuellGridLinienAus_LargeAxis() {
        settings.setAutoGrids(true);
        settings.setUserShowGridMaj(true);
        settings.setUserShowGridMin(true);

        // Large axis length should show both
        settings.blendeEventuellGridLinienAus(1000);

        assertTrue("Major grid should be shown for large axis", settings.isShowGridNormalMajor());
        assertTrue("Minor grid should be shown for large axis", settings.isShowGridNormalMinor());
    }

    @Test
    public void testBlendeEventuellGridLinienAus_SmallAxis() {
        settings.setAutoGrids(true);
        settings.setUserShowGridMaj(true);
        settings.setUserShowGridMin(true);

        // Small axis length should hide both
        settings.blendeEventuellGridLinienAus(50);

        assertFalse("Major grid should be hidden for small axis", settings.isShowGridNormalMajor());
        assertFalse("Minor grid should be hidden for small axis", settings.isShowGridNormalMinor());
    }

    @Test
    public void testBlendeEventuellGridLinienAus_MediumAxis_MajorOnly() {
        settings.setAutoGrids(true);
        settings.setUserShowGridMaj(true);
        settings.setUserShowGridMin(true);

        // Medium axis should show major only
        settings.blendeEventuellGridLinienAus(300);

        assertTrue("Major grid should be shown for medium axis", settings.isShowGridNormalMajor());
        assertFalse("Minor grid should be hidden for medium axis", settings.isShowGridNormalMinor());
    }

    @Test
    public void testBlendeEventuellGridLinienAus_ManualMode() {
        settings.setAutoGrids(false);
        settings.setUserShowGridMaj(true);
        settings.setUserShowGridMin(false);

        // Manual mode: use user settings regardless of axis size
        settings.blendeEventuellGridLinienAus(50);

        assertTrue("Major grid should follow user setting", settings.isShowGridNormalMajor());
        assertFalse("Minor grid should follow user setting", settings.isShowGridNormalMinor());
    }

    @Test
    public void testBlendeEventuellGridLinienAus_ManualMode_LargeAxis() {
        settings.setAutoGrids(false);
        settings.setUserShowGridMaj(false);
        settings.setUserShowGridMin(true);

        // Manual mode with large axis
        settings.blendeEventuellGridLinienAus(1000);

        assertFalse("Major grid should follow user setting", settings.isShowGridNormalMajor());
        assertTrue("Minor grid should follow user setting", settings.isShowGridNormalMinor());
    }

    // ====================================================
    // LineSettable Interface Tests
    // ====================================================

    @Test
    public void testGetSettableMaj_ReturnsNotNull() {
        LineSettable settable = settings.getSettableMaj();
        assertNotNull("Major settable should not be null", settable);
    }

    @Test
    public void testGetSettableMin_ReturnsNotNull() {
        LineSettable settable = settings.getSettableMin();
        assertNotNull("Minor settable should not be null", settable);
    }

    @Test
    public void testMajorSettable_GetColor() {
        LineSettable settable = settings.getSettableMaj();
        assertEquals("Settable color should match major grid color",
            GeckoColor.LIGHTGRAY, settable.getColor());
    }

    @Test
    public void testMajorSettable_SetColor() {
        LineSettable settable = settings.getSettableMaj();
        settable.setColor(GeckoColor.RED);
        assertEquals("Major grid color should be updated via settable",
            GeckoColor.RED, settings.getColorGridMaj());
    }

    @Test
    public void testMinorSettable_GetColor() {
        LineSettable settable = settings.getSettableMin();
        assertEquals("Settable color should match minor grid color",
            GeckoColor.LIGHTGRAY, settable.getColor());
    }

    @Test
    public void testMinorSettable_SetColor() {
        LineSettable settable = settings.getSettableMin();
        settable.setColor(GeckoColor.BLUE);
        assertEquals("Minor grid color should be updated via settable",
            GeckoColor.BLUE, settings.getColorGridMin());
    }

    @Test
    public void testSettable_GetStroke() {
        LineSettable majSettable = settings.getSettableMaj();
        LineSettable minSettable = settings.getSettableMin();

        assertEquals("Major settable stroke", GeckoLineStyle.SOLID_THIN, majSettable.getStroke());
        assertEquals("Minor settable stroke", GeckoLineStyle.SOLID_THIN, minSettable.getStroke());
    }

    @Test
    public void testSettable_SetStroke() {
        LineSettable majSettable = settings.getSettableMaj();
        majSettable.setStroke(GeckoLineStyle.DOTTED_PLAIN);

        assertEquals("Major line style updated via settable",
            GeckoLineStyle.DOTTED_PLAIN, settings.getLinStyleMaj());
    }

    @Test
    public void testSettable_GetTransparency() {
        LineSettable settable = settings.getSettableMaj();
        assertEquals("Grid transparency should be 1.0 (opaque)", 1.0f, settable.getTransparency(), 1e-6f);
    }

    // ====================================================
    // State Persistence Tests
    // ====================================================

    @Test
    public void testMultipleStateChanges_AllIndependent() {
        settings.setAutoGrids(false);
        settings.setUserShowGridMaj(false);
        settings.setUserShowGridMin(true);
        settings.setColorGridMaj(GeckoColor.RED);
        settings.setColorGridMin(GeckoColor.BLUE);
        settings.setLinStyleMaj(GeckoLineStyle.DOTTED_PLAIN);

        assertFalse("Auto grids should remain false", settings.isAutoGrids());
        assertFalse("User show major should remain false", settings.isUserShowGridMaj());
        assertTrue("User show minor should remain true", settings.isUserShowGridMin());
        assertEquals("Major color should remain RED", GeckoColor.RED, settings.getColorGridMaj());
        assertEquals("Minor color should remain BLUE", GeckoColor.BLUE, settings.getColorGridMin());
        assertEquals("Major line style should remain DASHED_PLAIN",
            GeckoLineStyle.DOTTED_PLAIN, settings.getLinStyleMaj());
    }

    @Test
    public void testMultipleInstances_Independent() {
        AxisGridSettings settings2 = new AxisGridSettings();

        settings.setColorGridMaj(GeckoColor.RED);
        settings2.setColorGridMaj(GeckoColor.BLUE);

        assertEquals("First instance color", GeckoColor.RED, settings.getColorGridMaj());
        assertEquals("Second instance color", GeckoColor.BLUE, settings2.getColorGridMaj());
    }

    // ====================================================
    // Boundary Tests
    // ====================================================

    @Test
    public void testBlendeEventuellGridLinienAus_ZeroAxis() {
        settings.setAutoGrids(true);
        settings.setUserShowGridMaj(true);
        settings.setUserShowGridMin(true);

        settings.blendeEventuellGridLinienAus(0);

        assertFalse("Major grid should be hidden for zero axis", settings.isShowGridNormalMajor());
        assertFalse("Minor grid should be hidden for zero axis", settings.isShowGridNormalMinor());
    }

    @Test
    public void testBlendeEventuellGridLinienAus_VeryLargeAxis() {
        settings.setAutoGrids(true);
        settings.setUserShowGridMaj(true);
        settings.setUserShowGridMin(true);

        settings.blendeEventuellGridLinienAus(10000);

        assertTrue("Major grid should be shown for very large axis", settings.isShowGridNormalMajor());
        assertTrue("Minor grid should be shown for very large axis", settings.isShowGridNormalMinor());
    }

    @Test
    public void testBlendeEventuellGridLinienAus_NegativeAxis() {
        settings.setAutoGrids(true);
        settings.setUserShowGridMaj(true);
        settings.setUserShowGridMin(true);

        // Negative axis length should hide grids
        settings.blendeEventuellGridLinienAus(-100);

        assertFalse("Major grid should be hidden for negative axis", settings.isShowGridNormalMajor());
        assertFalse("Minor grid should be hidden for negative axis", settings.isShowGridNormalMinor());
    }
}
