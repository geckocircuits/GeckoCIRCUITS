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

import java.awt.Font;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for GlobalFonts - font constant definitions for UI elements.
 */
public class GlobalFontsTest {

    // ====================================================
    // Font Constant Null Tests
    // ====================================================

    @Test
    public void testFoAUSWAHL_NotNull() {
        assertNotNull("foAUSWAHL should not be null", GlobalFonts.foAUSWAHL);
    }

    @Test
    public void testFoGRAFER_NotNull() {
        assertNotNull("foGRAFER should not be null", GlobalFonts.foGRAFER);
    }

    @Test
    public void testLabFontDialog1_NotNull() {
        assertNotNull("LAB_FONT_DIALOG_1 should not be null", GlobalFonts.LAB_FONT_DIALOG_1);
    }

    @Test
    public void testFormelDialogGross_NotNull() {
        assertNotNull("FORMEL_DIALOG_GROSS should not be null", GlobalFonts.FORMEL_DIALOG_GROSS);
    }

    @Test
    public void testFormelDialogKlein_NotNull() {
        assertNotNull("FORMEL_DIALOG_KLEIN should not be null", GlobalFonts.FORMEL_DIALOG_KLEIN);
    }

    // ====================================================
    // Font Name Tests
    // ====================================================

    @Test
    public void testFoAUSWAHL_FontName() {
        assertEquals("foAUSWAHL should use Arial", "Arial", GlobalFonts.foAUSWAHL.getName());
    }

    @Test
    public void testFoGRAFER_FontName() {
        assertEquals("foGRAFER should use Arial", "Arial", GlobalFonts.foGRAFER.getName());
    }

    @Test
    public void testLabFontDialog1_FontName() {
        assertEquals("LAB_FONT_DIALOG_1 should use Arial", "Arial", GlobalFonts.LAB_FONT_DIALOG_1.getName());
    }

    @Test
    public void testFormelDialogGross_FontName() {
        assertEquals("FORMEL_DIALOG_GROSS should use Times New Roman", "Times New Roman", GlobalFonts.FORMEL_DIALOG_GROSS.getName());
    }

    @Test
    public void testFormelDialogKlein_FontName() {
        assertEquals("FORMEL_DIALOG_KLEIN should use Times New Roman", "Times New Roman", GlobalFonts.FORMEL_DIALOG_KLEIN.getName());
    }

    // ====================================================
    // Font Style Tests
    // ====================================================

    @Test
    public void testFoAUSWAHL_FontStyle() {
        assertEquals("foAUSWAHL should be PLAIN", Font.PLAIN, GlobalFonts.foAUSWAHL.getStyle());
    }

    @Test
    public void testFoGRAFER_FontStyle() {
        assertEquals("foGRAFER should be PLAIN", Font.PLAIN, GlobalFonts.foGRAFER.getStyle());
    }

    @Test
    public void testLabFontDialog1_FontStyle() {
        assertEquals("LAB_FONT_DIALOG_1 should be PLAIN", Font.PLAIN, GlobalFonts.LAB_FONT_DIALOG_1.getStyle());
    }

    @Test
    public void testFormelDialogGross_FontStyle() {
        assertEquals("FORMEL_DIALOG_GROSS should be ITALIC", Font.ITALIC, GlobalFonts.FORMEL_DIALOG_GROSS.getStyle());
    }

    @Test
    public void testFormelDialogKlein_FontStyle() {
        assertEquals("FORMEL_DIALOG_KLEIN should be ITALIC", Font.ITALIC, GlobalFonts.FORMEL_DIALOG_KLEIN.getStyle());
    }

    // ====================================================
    // Font Size Tests
    // ====================================================

    @Test
    public void testFoAUSWAHL_FontSize() {
        assertEquals("foAUSWAHL should be size 12", 12, GlobalFonts.foAUSWAHL.getSize());
    }

    @Test
    public void testFoGRAFER_FontSize() {
        assertEquals("foGRAFER should be size 11", 11, GlobalFonts.foGRAFER.getSize());
    }

    @Test
    public void testLabFontDialog1_FontSize() {
        assertEquals("LAB_FONT_DIALOG_1 should be size 12", 12, GlobalFonts.LAB_FONT_DIALOG_1.getSize());
    }

    @Test
    public void testFormelDialogGross_FontSize() {
        assertEquals("FORMEL_DIALOG_GROSS should be size 16", 16, GlobalFonts.FORMEL_DIALOG_GROSS.getSize());
    }

    @Test
    public void testFormelDialogKlein_FontSize() {
        assertEquals("FORMEL_DIALOG_KLEIN should be size 12", 12, GlobalFonts.FORMEL_DIALOG_KLEIN.getSize());
    }

    // ====================================================
    // Font Grouping Tests
    // ====================================================

    @Test
    public void testArial_Fonts_UseSameFontFamily() {
        // foAUSWAHL, foGRAFER, and LAB_FONT_DIALOG_1 should all be Arial
        assertEquals("All Arial fonts should match", GlobalFonts.foAUSWAHL.getName(), GlobalFonts.foGRAFER.getName());
        assertEquals("All Arial fonts should match", GlobalFonts.foAUSWAHL.getName(), GlobalFonts.LAB_FONT_DIALOG_1.getName());
    }

    @Test
    public void testArialFonts_DifferInSize() {
        // Arial fonts should have different sizes for hierarchy
        assertNotEquals("foAUSWAHL and foGRAFER should have different sizes",
            GlobalFonts.foAUSWAHL.getSize(), GlobalFonts.foGRAFER.getSize());
    }

    @Test
    public void testFormuleFonts_UseSameFontFamily() {
        // FORMEL_DIALOG_GROSS and FORMEL_DIALOG_KLEIN should both be Times New Roman
        assertEquals("Formula fonts should both be Times New Roman",
            GlobalFonts.FORMEL_DIALOG_GROSS.getName(), GlobalFonts.FORMEL_DIALOG_KLEIN.getName());
    }

    @Test
    public void testFormuleFonts_BothItalic() {
        // Both formula fonts should be italic
        assertEquals("FORMEL_DIALOG_GROSS should be ITALIC", Font.ITALIC, GlobalFonts.FORMEL_DIALOG_GROSS.getStyle());
        assertEquals("FORMEL_DIALOG_KLEIN should be ITALIC", Font.ITALIC, GlobalFonts.FORMEL_DIALOG_KLEIN.getStyle());
    }

    @Test
    public void testFormuleFonts_DifferInSize() {
        // Formula fonts should differ in size for hierarchy
        assertNotEquals("Formula fonts should have different sizes",
            GlobalFonts.FORMEL_DIALOG_GROSS.getSize(), GlobalFonts.FORMEL_DIALOG_KLEIN.getSize());
    }

    @Test
    public void testFormulaGross_LargerThanKlein() {
        // GROSS should be larger than KLEIN
        assertTrue("FORMEL_DIALOG_GROSS should be larger than FORMEL_DIALOG_KLEIN",
            GlobalFonts.FORMEL_DIALOG_GROSS.getSize() > GlobalFonts.FORMEL_DIALOG_KLEIN.getSize());
    }

    // ====================================================
    // Font Validity Tests
    // ====================================================

    @Test
    public void testAllFonts_HaveValidSize() {
        Font[] fonts = {
            GlobalFonts.foAUSWAHL,
            GlobalFonts.foGRAFER,
            GlobalFonts.LAB_FONT_DIALOG_1,
            GlobalFonts.FORMEL_DIALOG_GROSS,
            GlobalFonts.FORMEL_DIALOG_KLEIN
        };

        for (Font font : fonts) {
            assertTrue("Font size should be positive", font.getSize() > 0);
            assertTrue("Font size should be reasonable (< 100)", font.getSize() < 100);
        }
    }

    @Test
    public void testAllFonts_HaveValidStyle() {
        Font[] fonts = {
            GlobalFonts.foAUSWAHL,
            GlobalFonts.foGRAFER,
            GlobalFonts.LAB_FONT_DIALOG_1,
            GlobalFonts.FORMEL_DIALOG_GROSS,
            GlobalFonts.FORMEL_DIALOG_KLEIN
        };

        for (Font font : fonts) {
            int style = font.getStyle();
            assertTrue("Font style should be PLAIN or ITALIC or BOLD",
                style == Font.PLAIN || style == Font.ITALIC || style == Font.BOLD ||
                style == (Font.ITALIC | Font.BOLD));
        }
    }

    @Test
    public void testAllFonts_HaveFamilyName() {
        Font[] fonts = {
            GlobalFonts.foAUSWAHL,
            GlobalFonts.foGRAFER,
            GlobalFonts.LAB_FONT_DIALOG_1,
            GlobalFonts.FORMEL_DIALOG_GROSS,
            GlobalFonts.FORMEL_DIALOG_KLEIN
        };

        for (Font font : fonts) {
            assertNotNull("Font family name should not be null", font.getFamily());
            assertTrue("Font family name should not be empty", font.getFamily().length() > 0);
        }
    }

    // ====================================================
    // Use Case Tests
    // ====================================================

    @Test
    public void testFoAUSWAHL_SuitableForSelection() {
        // Used for selection/choice UI elements - medium size, readable
        Font font = GlobalFonts.foAUSWAHL;
        assertEquals(12, font.getSize());
        assertEquals(Font.PLAIN, font.getStyle());
    }

    @Test
    public void testFoGRAFER_SuitableForGraphics() {
        // Used for graphics/diagram text - slightly smaller
        Font font = GlobalFonts.foGRAFER;
        assertEquals(11, font.getSize());
        assertEquals(Font.PLAIN, font.getStyle());
    }

    @Test
    public void testFormelDialogGross_ForLargeFormulas() {
        // Used for large formula display
        Font font = GlobalFonts.FORMEL_DIALOG_GROSS;
        assertEquals(16, font.getSize());
        assertEquals(Font.ITALIC, font.getStyle());
    }

    @Test
    public void testFormelDialogKlein_ForSmallFormulas() {
        // Used for small formula display
        Font font = GlobalFonts.FORMEL_DIALOG_KLEIN;
        assertEquals(12, font.getSize());
        assertEquals(Font.ITALIC, font.getStyle());
    }

    // ====================================================
    // Immutability/Consistency Tests
    // ====================================================

    @Test
    public void testFonts_AreConsistent() {
        // Multiple accesses should return consistent results
        Font font1 = GlobalFonts.foAUSWAHL;
        Font font2 = GlobalFonts.foAUSWAHL;
        assertEquals("Font names should match", font1.getName(), font2.getName());
        assertEquals("Font sizes should match", font1.getSize(), font2.getSize());
        assertEquals("Font styles should match", font1.getStyle(), font2.getStyle());
    }
}
