/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.newscope;

import java.awt.Stroke;
import java.awt.BasicStroke;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for GeckoLineStyle enum - line styles for scope curves.
 */
public class GeckoLineStyleTest {

    // ====================================================
    // Enum Values Tests
    // ====================================================

    @Test
    public void testAllEnumValues_Exist() {
        GeckoLineStyle[] values = GeckoLineStyle.values();
        assertEquals(7, values.length);
    }

    @Test
    public void testSolidStyles_Exist() {
        assertNotNull(GeckoLineStyle.SOLID_PLAIN);
        assertNotNull(GeckoLineStyle.SOLID_FAT_1);
        assertNotNull(GeckoLineStyle.SOLID_FAT_2);
        assertNotNull(GeckoLineStyle.SOLID_THIN);
    }

    @Test
    public void testDottedStyles_Exist() {
        assertNotNull(GeckoLineStyle.DOTTED_PLAIN);
        assertNotNull(GeckoLineStyle.DOTTED_FAT);
    }

    @Test
    public void testInvisible_Exists() {
        assertNotNull(GeckoLineStyle.INVISIBLE);
    }

    // ====================================================
    // ValueOf Tests
    // ====================================================

    @Test
    public void testValueOf_SolidPlain() {
        assertEquals(GeckoLineStyle.SOLID_PLAIN, GeckoLineStyle.valueOf("SOLID_PLAIN"));
    }

    @Test
    public void testValueOf_Invisible() {
        assertEquals(GeckoLineStyle.INVISIBLE, GeckoLineStyle.valueOf("INVISIBLE"));
    }

    @Test
    public void testValueOf_DottedPlain() {
        assertEquals(GeckoLineStyle.DOTTED_PLAIN, GeckoLineStyle.valueOf("DOTTED_PLAIN"));
    }

    // ====================================================
    // Code Tests
    // ====================================================

    @Test
    public void testCode_UniqueForAllStyles() {
        GeckoLineStyle[] values = GeckoLineStyle.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals("Codes should be unique: " + values[i] + " vs " + values[j],
                    values[i].code(), values[j].code());
            }
        }
    }

    @Test
    public void testCode_NegativeValues() {
        for (GeckoLineStyle style : GeckoLineStyle.values()) {
            assertTrue("Code should be negative: " + style, style.code() < 0);
        }
    }

    // ====================================================
    // Stroke Tests
    // ====================================================

    @Test
    public void testStroke_SolidPlain_NotNull() {
        assertNotNull(GeckoLineStyle.SOLID_PLAIN.stroke());
    }

    @Test
    public void testStroke_AllStylesHaveStrokes() {
        for (GeckoLineStyle style : GeckoLineStyle.values()) {
            assertNotNull("Stroke should not be null for " + style, style.stroke());
        }
    }

    @Test
    public void testStroke_SolidPlain_IsBasicStroke() {
        Stroke stroke = GeckoLineStyle.SOLID_PLAIN.stroke();
        assertTrue(stroke instanceof BasicStroke);
    }

    @Test
    public void testStroke_SolidFat1_ThickerThanPlain() {
        BasicStroke plain = (BasicStroke) GeckoLineStyle.SOLID_PLAIN.stroke();
        BasicStroke fat1 = (BasicStroke) GeckoLineStyle.SOLID_FAT_1.stroke();
        
        assertTrue("FAT_1 should be thicker than PLAIN",
            fat1.getLineWidth() > plain.getLineWidth());
    }

    @Test
    public void testStroke_SolidFat2_ThickerThanFat1() {
        BasicStroke fat1 = (BasicStroke) GeckoLineStyle.SOLID_FAT_1.stroke();
        BasicStroke fat2 = (BasicStroke) GeckoLineStyle.SOLID_FAT_2.stroke();
        
        assertTrue("FAT_2 should be thicker than FAT_1",
            fat2.getLineWidth() > fat1.getLineWidth());
    }

    @Test
    public void testStroke_SolidThin_ThinnerThanPlain() {
        BasicStroke plain = (BasicStroke) GeckoLineStyle.SOLID_PLAIN.stroke();
        BasicStroke thin = (BasicStroke) GeckoLineStyle.SOLID_THIN.stroke();
        
        assertTrue("THIN should be thinner than PLAIN",
            thin.getLineWidth() < plain.getLineWidth());
    }

    @Test
    public void testStroke_Invisible_VeryThin() {
        BasicStroke invisible = (BasicStroke) GeckoLineStyle.INVISIBLE.stroke();
        assertTrue("INVISIBLE should be very thin",
            invisible.getLineWidth() < 0.2f);
    }

    @Test
    public void testStroke_DottedPlain_HasDashArray() {
        BasicStroke dotted = (BasicStroke) GeckoLineStyle.DOTTED_PLAIN.stroke();
        assertNotNull("DOTTED_PLAIN should have dash array", dotted.getDashArray());
    }

    @Test
    public void testStroke_DottedFat_HasDashArray() {
        BasicStroke dotted = (BasicStroke) GeckoLineStyle.DOTTED_FAT.stroke();
        assertNotNull("DOTTED_FAT should have dash array", dotted.getDashArray());
    }

    // ====================================================
    // GetFromOrdinal Tests
    // ====================================================

    @Test
    public void testGetFromOrdinal_AllValues() {
        for (GeckoLineStyle style : GeckoLineStyle.values()) {
            GeckoLineStyle retrieved = GeckoLineStyle.getFromOrdinal(style.ordinal());
            assertEquals(style, retrieved);
        }
    }

    @Test
    public void testGetFromOrdinal_SolidPlainIsZero() {
        assertEquals(GeckoLineStyle.SOLID_PLAIN, GeckoLineStyle.getFromOrdinal(0));
    }

    // ====================================================
    // GetFromCode Tests
    // ====================================================

    @Test
    public void testGetFromCode_AllValues() {
        for (GeckoLineStyle style : GeckoLineStyle.values()) {
            GeckoLineStyle retrieved = GeckoLineStyle.getFromCode(style.code());
            assertEquals(style, retrieved);
        }
    }

    @Test
    public void testGetFromCode_RoundTrip() {
        for (GeckoLineStyle original : GeckoLineStyle.values()) {
            int code = original.code();
            GeckoLineStyle restored = GeckoLineStyle.getFromCode(code);
            assertEquals(original, restored);
        }
    }

    @Test
    public void testGetFromCode_InvalidCode_ReturnsDefault() {
        GeckoLineStyle result = GeckoLineStyle.getFromCode(999999);
        assertEquals("Invalid code should return SOLID_PLAIN as default",
            GeckoLineStyle.SOLID_PLAIN, result);
    }

    // ====================================================
    // SetzeLinienstilSelektiert Tests (Alias for getFromOrdinal)
    // ====================================================

    @Test
    public void testSetzeLinienstilSelektiert_AllValues() {
        for (GeckoLineStyle style : GeckoLineStyle.values()) {
            GeckoLineStyle retrieved = GeckoLineStyle.setzeLinienstilSelektiert(style.ordinal());
            assertEquals(style, retrieved);
        }
    }

    // ====================================================
    // Ordinal Stability Tests
    // ====================================================

    @Test
    public void testOrdinal_SolidPlainIsZero() {
        assertEquals(0, GeckoLineStyle.SOLID_PLAIN.ordinal());
    }

    @Test
    public void testOrdinal_InvisibleIsOne() {
        assertEquals(1, GeckoLineStyle.INVISIBLE.ordinal());
    }
}
