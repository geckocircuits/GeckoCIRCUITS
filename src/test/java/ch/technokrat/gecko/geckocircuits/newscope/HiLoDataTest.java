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
 * Tests for the HiLoData class used in scope rendering.
 */
public class HiLoDataTest {

    private static final double DELTA = 1e-9;

    @Test
    public void testHiLoDataCreation() {
        HiLoData data = HiLoData.hiLoDataFabric(1.0f, 5.0f);
        assertEquals(1.0f, data._yLo, DELTA);
        assertEquals(5.0f, data._yHi, DELTA);
    }

    @Test
    public void testHiLoDataWithReversedValues() {
        // HiLoData stores values as given (does not normalize)
        HiLoData data = HiLoData.hiLoDataFabric(5.0f, 1.0f);
        // Values are stored as given: _yLo=5.0, _yHi=1.0
        assertEquals(5.0f, data._yLo, DELTA);
        assertEquals(1.0f, data._yHi, DELTA);
    }

    @Test
    public void testHiLoDataWithEqualValues() {
        HiLoData data = HiLoData.hiLoDataFabric(3.0f, 3.0f);
        assertEquals(3.0f, data._yLo, DELTA);
        assertEquals(3.0f, data._yHi, DELTA);
    }

    @Test
    public void testHiLoDataWithNegativeValues() {
        HiLoData data = HiLoData.hiLoDataFabric(-5.0f, -1.0f);
        assertEquals(-5.0f, data._yLo, DELTA);
        assertEquals(-1.0f, data._yHi, DELTA);
    }

    @Test
    public void testHiLoDataRange() {
        HiLoData data = HiLoData.hiLoDataFabric(2.0f, 8.0f);
        float range = data._yHi - data._yLo;
        assertEquals(6.0f, range, DELTA);
    }

    @Test
    public void testHiLoDataMidpoint() {
        HiLoData data = HiLoData.hiLoDataFabric(0.0f, 10.0f);
        float midpoint = (data._yHi + data._yLo) / 2.0f;
        assertEquals(5.0f, midpoint, DELTA);
    }

    @Test
    public void testHiLoDataWithZeroRange() {
        HiLoData data = HiLoData.hiLoDataFabric(0.0f, 0.0f);
        assertEquals(0.0f, data._yLo, DELTA);
        assertEquals(0.0f, data._yHi, DELTA);
    }

    @Test
    public void testHiLoDataWithLargeValues() {
        HiLoData data = HiLoData.hiLoDataFabric(-1e6f, 1e6f);
        assertEquals(-1e6f, data._yLo, DELTA);
        assertEquals(1e6f, data._yHi, DELTA);
    }
}
