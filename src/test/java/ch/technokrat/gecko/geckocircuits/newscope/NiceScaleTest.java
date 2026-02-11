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
 * Tests for NiceScale - the axis scaling calculator for scope display.
 */
public class NiceScaleTest {

    private static final double DELTA = 1e-9;

    @Test
    public void testNiceNum_RoundFalse_ReturnsNiceRange() {
        // niceNum should return nice numbers for axis ranges
        double result = NiceScale.niceNum(7.5, false);
        assertEquals("Range 7.5 should round to 10", 10.0, result, DELTA);
    }

    @Test
    public void testNiceNum_RoundTrue_ReturnsNiceTickSpacing() {
        double result = NiceScale.niceNum(0.7, true);
        // 0.7: exponent 0, fraction 0.7 -> less than 1.5 -> niceFraction = 1? No, 0.7 has exponent -1
        // Actually 0.7: log10(0.7) = -0.15, floor = -1, fraction = 0.7 / 0.1 = 7 -> niceFraction = 5
        assertEquals("Tick 0.7 should round to 0.5", 0.5, result, DELTA);
    }

    @Test
    public void testNiceNum_SmallValue() {
        double result = NiceScale.niceNum(0.003, true);
        // 0.003: log10(0.003) = -2.52, floor = -3, fraction = 0.003 / 0.001 = 3 -> niceFraction = 5
        assertEquals("Small value should work", 0.005, result, DELTA);
    }

    @Test
    public void testNiceNum_LargeValue() {
        double result = NiceScale.niceNum(75000, false);
        // 75000: exponent 4, fraction 7.5 -> 10
        assertEquals("Large value should work", 100000.0, result, DELTA);
    }

    @Test
    public void testNiceNum_Fraction_LessThan1Point5_Round() {
        // fraction < 1.5 -> niceFraction = 1
        double result = NiceScale.niceNum(1.2, true);
        assertEquals("1.2 rounds to 1", 1.0, result, DELTA);
    }

    @Test
    public void testNiceNum_Fraction_Between1Point5And3_Round() {
        // 1.5 <= fraction < 3 -> niceFraction = 2
        double result = NiceScale.niceNum(2.5, true);
        assertEquals("2.5 rounds to 2", 2.0, result, DELTA);
    }

    @Test
    public void testNiceNum_Fraction_Between3And7_Round() {
        // 3 <= fraction < 7 -> niceFraction = 5
        double result = NiceScale.niceNum(4.5, true);
        assertEquals("4.5 rounds to 5", 5.0, result, DELTA);
    }

    @Test
    public void testNiceNum_Fraction_GreaterThan7_Round() {
        // fraction >= 7 -> niceFraction = 10
        double result = NiceScale.niceNum(8.5, true);
        assertEquals("8.5 rounds to 10", 10.0, result, DELTA);
    }

    @Test
    public void testNiceScale_PositiveRange() {
        HiLoData data = HiLoData.hiLoDataFabric(0.0f, 100.0f);
        NiceScale scale = new NiceScale(data);

        HiLoData niceLimits = scale.getNiceLimits();
        assertNotNull("Nice limits should not be null", niceLimits);
        assertTrue("Nice min should be <= actual min", niceLimits._yLo <= 0.0f);
        assertTrue("Nice max should be >= actual max", niceLimits._yHi >= 100.0f);
    }

    @Test
    public void testNiceScale_NegativeRange() {
        HiLoData data = HiLoData.hiLoDataFabric(-50.0f, -10.0f);
        NiceScale scale = new NiceScale(data);

        HiLoData niceLimits = scale.getNiceLimits();
        assertTrue("Nice min should be <= actual min", niceLimits._yLo <= -50.0f);
        assertTrue("Nice max should be >= actual max", niceLimits._yHi >= -10.0f);
    }

    @Test
    public void testNiceScale_CrossingZero() {
        HiLoData data = HiLoData.hiLoDataFabric(-30.0f, 70.0f);
        NiceScale scale = new NiceScale(data);

        HiLoData niceLimits = scale.getNiceLimits();
        assertTrue("Nice min should be <= -30", niceLimits._yLo <= -30.0f);
        assertTrue("Nice max should be >= 70", niceLimits._yHi >= 70.0f);
    }

    @Test
    public void testGetTickSpacing_PositiveRange() {
        HiLoData data = HiLoData.hiLoDataFabric(0.0f, 100.0f);
        NiceScale scale = new NiceScale(data);

        double tickSpacing = scale.getTickSpacing();
        assertTrue("Tick spacing should be positive", tickSpacing > 0);
    }

    @Test
    public void testGetTickSpacing_SmallRange() {
        HiLoData data = HiLoData.hiLoDataFabric(0.0f, 0.001f);
        NiceScale scale = new NiceScale(data);

        double tickSpacing = scale.getTickSpacing();
        assertTrue("Tick spacing should be positive for small range", tickSpacing > 0);
        assertTrue("Tick spacing should be small", tickSpacing < 0.001);
    }

    @Test
    public void testNiceScale_XAxisMode() {
        HiLoData data = HiLoData.hiLoDataFabric(0.0f, 1.0f);
        NiceScale scale = new NiceScale(data, true);

        HiLoData niceLimits = scale.getNiceLimits();
        assertNotNull("X-axis mode should produce valid limits", niceLimits);
    }

    @Test
    public void testNiceScale_YAxisMode() {
        HiLoData data = HiLoData.hiLoDataFabric(0.0f, 1.0f);
        NiceScale scale = new NiceScale(data, false);

        HiLoData niceLimits = scale.getNiceLimits();
        assertNotNull("Y-axis mode should produce valid limits", niceLimits);
    }

    @Test
    public void testNiceScale_EqualMinMax_Positive() {
        HiLoData data = HiLoData.hiLoDataFabric(5.0f, 5.0f);
        NiceScale scale = new NiceScale(data);

        HiLoData niceLimits = scale.getNiceLimits();
        assertTrue("Nice range should be expanded", niceLimits._yHi > niceLimits._yLo);
    }
}
