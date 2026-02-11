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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for AxisLinLog enum - linear/logarithmic axis scaling.
 */
public class AxisLinLogTest {

    // ====================================================
    // Enum Values Tests
    // ====================================================

    @Test
    public void testAllEnumValues_Exist() {
        AxisLinLog[] values = AxisLinLog.values();
        assertEquals(2, values.length);
    }

    @Test
    public void testLinear_Exists() {
        assertNotNull(AxisLinLog.ACHSE_LIN);
    }

    @Test
    public void testLogarithmic_Exists() {
        assertNotNull(AxisLinLog.ACHSE_LOG);
    }

    // ====================================================
    // ValueOf Tests
    // ====================================================

    @Test
    public void testValueOf_Linear() {
        assertEquals(AxisLinLog.ACHSE_LIN, AxisLinLog.valueOf("ACHSE_LIN"));
    }

    @Test
    public void testValueOf_Logarithmic() {
        assertEquals(AxisLinLog.ACHSE_LOG, AxisLinLog.valueOf("ACHSE_LOG"));
    }

    // ====================================================
    // GetCode Tests
    // ====================================================

    @Test
    public void testGetCode_Linear_NotNull() {
        int code = AxisLinLog.ACHSE_LIN.getCode();
        assertNotEquals(0, code);
    }

    @Test
    public void testGetCode_Logarithmic_NotNull() {
        int code = AxisLinLog.ACHSE_LOG.getCode();
        assertNotEquals(0, code);
    }

    @Test
    public void testGetCode_UniqueValues() {
        assertNotEquals(AxisLinLog.ACHSE_LIN.getCode(), AxisLinLog.ACHSE_LOG.getCode());
    }

    @Test
    public void testGetCode_NegativeValues() {
        // Codes are intentionally negative
        assertTrue("Linear code should be negative", AxisLinLog.ACHSE_LIN.getCode() < 0);
        assertTrue("Log code should be negative", AxisLinLog.ACHSE_LOG.getCode() < 0);
    }

    // ====================================================
    // GetFromOrdinal Tests
    // ====================================================

    @Test
    public void testGetFromOrdinal_Linear() {
        assertEquals(AxisLinLog.ACHSE_LIN, AxisLinLog.getFromOrdinal(0));
    }

    @Test
    public void testGetFromOrdinal_Logarithmic() {
        assertEquals(AxisLinLog.ACHSE_LOG, AxisLinLog.getFromOrdinal(1));
    }

    @Test
    public void testGetFromOrdinal_AllValues() {
        for (AxisLinLog value : AxisLinLog.values()) {
            AxisLinLog retrieved = AxisLinLog.getFromOrdinal(value.ordinal());
            assertEquals(value, retrieved);
        }
    }

    // ====================================================
    // GetFromCode Tests
    // ====================================================

    @Test
    public void testGetFromCode_Linear() {
        int code = AxisLinLog.ACHSE_LIN.getCode();
        assertEquals(AxisLinLog.ACHSE_LIN, AxisLinLog.getFromCode(code));
    }

    @Test
    public void testGetFromCode_Logarithmic() {
        int code = AxisLinLog.ACHSE_LOG.getCode();
        assertEquals(AxisLinLog.ACHSE_LOG, AxisLinLog.getFromCode(code));
    }

    @Test
    public void testGetFromCode_RoundTrip() {
        for (AxisLinLog original : AxisLinLog.values()) {
            int code = original.getCode();
            AxisLinLog restored = AxisLinLog.getFromCode(code);
            assertEquals(original, restored);
        }
    }

    @Test
    public void testGetFromCode_InvalidCode_ReturnsLinear() {
        // Default for unknown code should be linear
        AxisLinLog result = AxisLinLog.getFromCode(999999);
        assertEquals("Invalid code should return ACHSE_LIN as default",
            AxisLinLog.ACHSE_LIN, result);
    }

    // ====================================================
    // Ordinal Tests
    // ====================================================

    @Test
    public void testOrdinal_LinearIsZero() {
        assertEquals(0, AxisLinLog.ACHSE_LIN.ordinal());
    }

    @Test
    public void testOrdinal_LogarithmicIsOne() {
        assertEquals(1, AxisLinLog.ACHSE_LOG.ordinal());
    }

    // ====================================================
    // Use Case Tests
    // ====================================================

    @Test
    public void testDefaultAxis_ShouldBeLinear() {
        // Linear is typically the default for most scope applications
        assertEquals(0, AxisLinLog.ACHSE_LIN.ordinal());
    }

    @Test
    public void testAxisTypeSwitching() {
        // Simulate switching between linear and log
        AxisLinLog current = AxisLinLog.ACHSE_LIN;
        
        // Switch to log
        if (current == AxisLinLog.ACHSE_LIN) {
            current = AxisLinLog.ACHSE_LOG;
        }
        assertEquals(AxisLinLog.ACHSE_LOG, current);
        
        // Switch back to linear
        if (current == AxisLinLog.ACHSE_LOG) {
            current = AxisLinLog.ACHSE_LIN;
        }
        assertEquals(AxisLinLog.ACHSE_LIN, current);
    }
}
