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

import java.awt.Color;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for GlobalColors - color constant definitions for circuit diagram elements.
 */
public class GlobalColorsTest {

    // ====================================================
    // Basic Color Constant Tests
    // ====================================================

    @Test
    public void testLabColorDialog1_NotNull() {
        assertNotNull("LAB_COLOR_DIALOG_1 should not be null", GlobalColors.LAB_COLOR_DIALOG_1);
    }

    @Test
    public void testLabColorDialog1_IsBlack() {
        assertEquals("LAB_COLOR_DIALOG_1 should be black", Color.black, GlobalColors.LAB_COLOR_DIALOG_1);
    }

    @Test
    public void testFarbeGecko_NotNull() {
        assertNotNull("farbeGecko should not be null", GlobalColors.farbeGecko);
    }

    @Test
    public void testFarbeGecko_IsGreenish() {
        Color geckoColor = GlobalColors.farbeGecko;
        assertNotNull("farbeGecko should be a valid Color", geckoColor);
        // Verify it's the characteristic gecko green (0x99bb33)
        assertTrue("farbeGecko should have significant green component", geckoColor.getGreen() > 100);
    }

    // ====================================================
    // Electrical Circuit Colors (LK - Leiterkarte)
    // ====================================================

    @Test
    public void testFarbeTextLinie_NotNull() {
        assertNotNull("farbeTextLinie should not be null", GlobalColors.farbeTextLinie);
    }

    @Test
    public void testFarbeInBearbeitungLK_NotNull() {
        assertNotNull("farbeInBearbeitungLK should not be null", GlobalColors.farbeInBearbeitungLK);
    }

    @Test
    public void testFarbeFertigElementLK_NotNull() {
        assertNotNull("farbeFertigElementLK should not be null", GlobalColors.farbeFertigElementLK);
    }

    @Test
    public void testFarbeFertigVerbindungLK_NotNull() {
        assertNotNull("farbeFertigVerbindungLK should not be null", GlobalColors.farbeFertigVerbindungLK);
    }

    @Test
    public void testFarbeLabelLK_NotNull() {
        assertNotNull("farbeLabelLK should not be null", GlobalColors.farbeLabelLK);
    }

    @Test
    public void testFarbeParallelLK_NotNull() {
        assertNotNull("farbeParallelLK should not be null", GlobalColors.farbeParallelLK);
    }

    @Test
    public void testFarbeElementLKHintergrund_NotNull() {
        assertNotNull("farbeElementLKHintergrund should not be null", GlobalColors.farbeElementLKHintergrund);
    }

    // ====================================================
    // Reluctance (REL) Colors
    // ====================================================

    @Test
    public void testFarbeElementRELFOREGROUND_NotNull() {
        assertNotNull("farbeElementRELFOREGROUND should not be null", GlobalColors.farbeElementRELFOREGROUND);
    }

    @Test
    public void testFarbeElementRELBACKGROUND_NotNull() {
        assertNotNull("farbeElementRELBACKGROUND should not be null", GlobalColors.farbeElementRELBACKGROUND);
    }

    // ====================================================
    // Control Block Colors (CONTROL)
    // ====================================================

    @Test
    public void testFarbeInBearbeitungCONTROL_NotNull() {
        assertNotNull("farbeInBearbeitungCONTROL should not be null", GlobalColors.farbeInBearbeitungCONTROL);
    }

    @Test
    public void testFarbeFertigElementCONTROL_NotNull() {
        assertNotNull("farbeFertigElementCONTROL should not be null", GlobalColors.farbeFertigElementCONTROL);
    }

    @Test
    public void testFarbeFertigVerbindungCONTROL_NotNull() {
        assertNotNull("farbeFertigVerbindungCONTROL should not be null", GlobalColors.farbeFertigVerbindungCONTROL);
    }

    @Test
    public void testFarbeLabelCONTROL_NotNull() {
        assertNotNull("farbeLabelCONTROL should not be null", GlobalColors.farbeLabelCONTROL);
    }

    @Test
    public void testFarbeParallelCONTROL_NotNull() {
        assertNotNull("farbeParallelCONTROL should not be null", GlobalColors.farbeParallelCONTROL);
    }

    @Test
    public void testFarbeElementCONTROLHintergrund_NotNull() {
        assertNotNull("farbeElementCONTROLHintergrund should not be null", GlobalColors.farbeElementCONTROLHintergrund);
    }

    // ====================================================
    // External Terminal Color
    // ====================================================

    @Test
    public void testFarbeEXTERNAL_TERMINAL_NotNull() {
        assertNotNull("farbeEXTERNAL_TERMINAL should not be null", GlobalColors.farbeEXTERNAL_TERMINAL);
    }

    @Test
    public void testFarbeEXTERNAL_TERMINAL_IsMagenta() {
        assertEquals("farbeEXTERNAL_TERMINAL should be magenta", Color.magenta, GlobalColors.farbeEXTERNAL_TERMINAL);
    }

    // ====================================================
    // Thermal (THERM) Colors
    // ====================================================

    @Test
    public void testFarbeInBearbeitungTHERM_NotNull() {
        assertNotNull("farbeInBearbeitungTHERM should not be null", GlobalColors.farbeInBearbeitungTHERM);
    }

    @Test
    public void testFarbeFertigElementTHERM_NotNull() {
        assertNotNull("farbeFertigElementTHERM should not be null", GlobalColors.farbeFertigElementTHERM);
    }

    @Test
    public void testFarbeFertigElementRELUCTANCE_NotNull() {
        assertNotNull("farbeFertigElementRELUCTANCE should not be null", GlobalColors.farbeFertigElementRELUCTANCE);
    }

    @Test
    public void testFarbeFertigVerbindungTHERM_NotNull() {
        assertNotNull("farbeFertigVerbindungTHERM should not be null", GlobalColors.farbeFertigVerbindungTHERM);
    }

    @Test
    public void testFarbeLabelTHERM_NotNull() {
        assertNotNull("farbeLabelTHERM should not be null", GlobalColors.farbeLabelTHERM);
    }

    @Test
    public void testFarbeParallelTHERM_NotNull() {
        assertNotNull("farbeParallelTHERM should not be null", GlobalColors.farbeParallelTHERM);
    }

    @Test
    public void testFarbeElementTHERMHintergrund_NotNull() {
        assertNotNull("farbeElementTHERMHintergrund should not be null", GlobalColors.farbeElementTHERMHintergrund);
    }

    // ====================================================
    // Tool Colors
    // ====================================================

    @Test
    public void testFarbeZoomRechteck_NotNull() {
        assertNotNull("farbeZoomRechteck should not be null", GlobalColors.farbeZoomRechteck);
    }

    @Test
    public void testFarbeZoomRechteck_IsRed() {
        assertEquals("farbeZoomRechteck should be red", Color.red, GlobalColors.farbeZoomRechteck);
    }

    @Test
    public void testFarbeOPT_NotNull() {
        assertNotNull("farbeOPT should not be null", GlobalColors.farbeOPT);
    }

    @Test
    public void testFarbeOPT_IsMagenta() {
        assertEquals("farbeOPT should be magenta", Color.magenta, GlobalColors.farbeOPT);
    }

    // ====================================================
    // Test Mode Colors
    // ====================================================

    @Test
    public void testFarbeConnectorTestMode_NotNull() {
        assertNotNull("farbeConnectorTestMode should not be null", GlobalColors.farbeConnectorTestMode);
    }

    @Test
    public void testFarbeConnectorTestMode_IsMagenta() {
        assertEquals("farbeConnectorTestMode should be magenta", Color.magenta, GlobalColors.farbeConnectorTestMode);
    }

    @Test
    public void testFarbeConnectorTestModeInternal_NotNull() {
        assertNotNull("farbeConnectorTestModeInternal should not be null", GlobalColors.farbeConnectorTestModeInternal);
    }

    @Test
    public void testFarbeConnectorTestModeInternal_IsYellow() {
        assertEquals("farbeConnectorTestModeInternal should be yellow", Color.yellow, GlobalColors.farbeConnectorTestModeInternal);
    }

    // ====================================================
    // Color Consistency Tests
    // ====================================================

    @Test
    public void testAllStaticColorsAreValid() {
        // Verify that RGB components are within valid range (0-255)
        Color[] allColors = {
            GlobalColors.LAB_COLOR_DIALOG_1,
            GlobalColors.farbeGecko,
            GlobalColors.farbeTextLinie,
            GlobalColors.farbeInBearbeitungLK,
            GlobalColors.farbeFertigElementLK,
            GlobalColors.farbeFertigVerbindungLK,
            GlobalColors.farbeLabelLK,
            GlobalColors.farbeParallelLK,
            GlobalColors.farbeElementLKHintergrund,
            GlobalColors.farbeElementRELFOREGROUND,
            GlobalColors.farbeElementRELBACKGROUND,
            GlobalColors.farbeInBearbeitungCONTROL,
            GlobalColors.farbeFertigElementCONTROL,
            GlobalColors.farbeFertigVerbindungCONTROL,
            GlobalColors.farbeLabelCONTROL,
            GlobalColors.farbeParallelCONTROL,
            GlobalColors.farbeElementCONTROLHintergrund,
            GlobalColors.farbeEXTERNAL_TERMINAL,
            GlobalColors.farbeInBearbeitungTHERM,
            GlobalColors.farbeFertigElementTHERM,
            GlobalColors.farbeFertigElementRELUCTANCE,
            GlobalColors.farbeFertigVerbindungTHERM,
            GlobalColors.farbeLabelTHERM,
            GlobalColors.farbeParallelTHERM,
            GlobalColors.farbeElementTHERMHintergrund,
            GlobalColors.farbeZoomRechteck,
            GlobalColors.farbeOPT,
            GlobalColors.farbeConnectorTestMode,
            GlobalColors.farbeConnectorTestModeInternal
        };

        for (Color color : allColors) {
            assertNotNull("Color should not be null", color);
            assertTrue("Red component should be in range [0,255]", color.getRed() >= 0 && color.getRed() <= 255);
            assertTrue("Green component should be in range [0,255]", color.getGreen() >= 0 && color.getGreen() <= 255);
            assertTrue("Blue component should be in range [0,255]", color.getBlue() >= 0 && color.getBlue() <= 255);
        }
    }

    // ====================================================
    // Visual Distinction Tests
    // ====================================================

    @Test
    public void testLKColorsVisuallyDistinct_FromCONTROLColors() {
        // Verify that circuit and control colors are visually different
        assertNotEquals("LK finished element should differ from CONTROL finished element",
            GlobalColors.farbeFertigElementLK, GlobalColors.farbeFertigElementCONTROL);
    }

    @Test
    public void testTHERMColorsAreDistinct_FromOtherDomains() {
        // Thermal colors should be distinct
        assertNotEquals("THERM and LK finished connections should differ",
            GlobalColors.farbeFertigVerbindungTHERM, GlobalColors.farbeFertigVerbindungLK);
    }
}
