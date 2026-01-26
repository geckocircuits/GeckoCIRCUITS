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

import java.awt.Color;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for GeckoColor enum - color definitions for scope visualization.
 */
public class GeckoColorTest {

    // ====================================================
    // Enum Values Tests
    // ====================================================

    @Test
    public void testAllEnumValues_Exist() {
        GeckoColor[] values = GeckoColor.values();
        assertEquals(13, values.length);
    }

    @Test
    public void testBasicColors_Exist() {
        assertNotNull(GeckoColor.BLACK);
        assertNotNull(GeckoColor.RED);
        assertNotNull(GeckoColor.GREEN);
        assertNotNull(GeckoColor.BLUE);
        assertNotNull(GeckoColor.WHITE);
    }

    @Test
    public void testGrayColors_Exist() {
        assertNotNull(GeckoColor.DARKGRAY);
        assertNotNull(GeckoColor.GRAY);
        assertNotNull(GeckoColor.LIGHTGRAY);
    }

    @Test
    public void testAdditionalColors_Exist() {
        assertNotNull(GeckoColor.MAGENTA);
        assertNotNull(GeckoColor.CYAN);
        assertNotNull(GeckoColor.ORANGE);
        assertNotNull(GeckoColor.YELLOW);
        assertNotNull(GeckoColor.DARKGREEN);
    }

    // ====================================================
    // ValueOf Tests
    // ====================================================

    @Test
    public void testValueOf_AllColors() {
        assertEquals(GeckoColor.BLACK, GeckoColor.valueOf("BLACK"));
        assertEquals(GeckoColor.RED, GeckoColor.valueOf("RED"));
        assertEquals(GeckoColor.GREEN, GeckoColor.valueOf("GREEN"));
        assertEquals(GeckoColor.BLUE, GeckoColor.valueOf("BLUE"));
        assertEquals(GeckoColor.MAGENTA, GeckoColor.valueOf("MAGENTA"));
    }

    // ====================================================
    // Java Color Mapping Tests
    // ====================================================

    @Test
    public void testGetJavaColor_Black() {
        assertEquals(Color.BLACK, GeckoColor.BLACK.getJavaColor());
    }

    @Test
    public void testGetJavaColor_Red() {
        assertEquals(Color.RED, GeckoColor.RED.getJavaColor());
    }

    @Test
    public void testGetJavaColor_Green() {
        assertEquals(Color.GREEN, GeckoColor.GREEN.getJavaColor());
    }

    @Test
    public void testGetJavaColor_Blue() {
        assertEquals(Color.BLUE, GeckoColor.BLUE.getJavaColor());
    }

    @Test
    public void testGetJavaColor_White() {
        assertEquals(Color.WHITE, GeckoColor.WHITE.getJavaColor());
    }

    @Test
    public void testGetJavaColor_Cyan() {
        assertEquals(Color.CYAN, GeckoColor.CYAN.getJavaColor());
    }

    @Test
    public void testGetJavaColor_Magenta() {
        assertEquals(Color.MAGENTA, GeckoColor.MAGENTA.getJavaColor());
    }

    @Test
    public void testGetJavaColor_Orange() {
        assertEquals(Color.ORANGE, GeckoColor.ORANGE.getJavaColor());
    }

    @Test
    public void testGetJavaColor_Yellow() {
        assertEquals(Color.YELLOW, GeckoColor.YELLOW.getJavaColor());
    }

    @Test
    public void testGetJavaColor_DarkGray() {
        assertEquals(Color.DARK_GRAY, GeckoColor.DARKGRAY.getJavaColor());
    }

    @Test
    public void testGetJavaColor_Gray() {
        assertEquals(Color.GRAY, GeckoColor.GRAY.getJavaColor());
    }

    @Test
    public void testGetJavaColor_LightGray() {
        assertEquals(Color.LIGHT_GRAY, GeckoColor.LIGHTGRAY.getJavaColor());
    }

    @Test
    public void testGetJavaColor_DarkGreen() {
        Color darkGreen = GeckoColor.DARKGREEN.getJavaColor();
        assertNotNull(darkGreen);
        // Dark green: RGB(0, 100, 0)
        assertEquals(0, darkGreen.getRed());
        assertEquals(100, darkGreen.getGreen());
        assertEquals(0, darkGreen.getBlue());
    }

    // ====================================================
    // Code Tests
    // ====================================================

    @Test
    public void testCode_UniqueForAllColors() {
        GeckoColor[] values = GeckoColor.values();
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals("Codes should be unique: " + values[i] + " vs " + values[j],
                    values[i].code(), values[j].code());
            }
        }
    }

    @Test
    public void testCode_NegativeValues() {
        // Codes are intentionally negative to distinguish from RGB values
        for (GeckoColor color : GeckoColor.values()) {
            assertTrue("Code should be negative: " + color, color.code() < 0);
        }
    }

    // ====================================================
    // GetFromOrdinal Tests
    // ====================================================

    @Test
    public void testGetFromOrdinal_AllValues() {
        for (GeckoColor color : GeckoColor.values()) {
            GeckoColor retrieved = GeckoColor.getFromOrdinal(color.ordinal());
            assertEquals(color, retrieved);
        }
    }

    @Test
    public void testGetFromOrdinal_Black() {
        assertEquals(GeckoColor.BLACK, GeckoColor.getFromOrdinal(0));
    }

    @Test
    public void testGetFromOrdinal_Red() {
        assertEquals(GeckoColor.RED, GeckoColor.getFromOrdinal(1));
    }

    // ====================================================
    // GetFromCode Tests
    // ====================================================

    @Test
    public void testGetFromCode_AllValues() {
        for (GeckoColor color : GeckoColor.values()) {
            GeckoColor retrieved = GeckoColor.getFromCode(color.code());
            assertEquals(color, retrieved);
        }
    }

    @Test
    public void testGetFromCode_RoundTrip() {
        for (GeckoColor original : GeckoColor.values()) {
            int code = original.code();
            GeckoColor restored = GeckoColor.getFromCode(code);
            assertEquals(original, restored);
        }
    }

    // ====================================================
    // GetNextColor Tests
    // ====================================================

    @Test
    public void testGetNextColor_ReturnsNonNull() {
        GeckoColor next = GeckoColor.getNextColor();
        assertNotNull(next);
    }

    @Test
    public void testGetNextColor_SkipsWhiteAndYellow() {
        // Call many times and verify WHITE/YELLOW never returned
        for (int i = 0; i < 50; i++) {
            GeckoColor next = GeckoColor.getNextColor();
            assertNotEquals("Should skip WHITE", GeckoColor.WHITE, next);
            assertNotEquals("Should skip YELLOW", GeckoColor.YELLOW, next);
        }
    }

    @Test
    public void testGetNextColor_FromPrevious() {
        GeckoColor next = GeckoColor.getNextColor(GeckoColor.BLACK);
        assertNotNull(next);
        assertNotEquals(GeckoColor.BLACK, next);
    }

    @Test
    public void testGetNextColor_FromPrevious_SkipsWhiteAndYellow() {
        GeckoColor color = GeckoColor.BLACK;
        for (int i = 0; i < 20; i++) {
            color = GeckoColor.getNextColor(color);
            assertNotEquals("Should skip WHITE", GeckoColor.WHITE, color);
            assertNotEquals("Should skip YELLOW", GeckoColor.YELLOW, color);
        }
    }

    // ====================================================
    // Ordinal Stability Tests (Important for serialization)
    // ====================================================

    @Test
    public void testOrdinal_BlackIsZero() {
        assertEquals(0, GeckoColor.BLACK.ordinal());
    }

    @Test
    public void testOrdinal_RedIsOne() {
        assertEquals(1, GeckoColor.RED.ordinal());
    }

    @Test
    public void testOrdinal_GreenIsTwo() {
        assertEquals(2, GeckoColor.GREEN.ordinal());
    }

    @Test
    public void testOrdinal_BlueIsThree() {
        assertEquals(3, GeckoColor.BLUE.ordinal());
    }
}
