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

    // === Tests for merge methods ===

    @Test
    public void testMerge_TwoValidData() {
        HiLoData data1 = HiLoData.hiLoDataFabric(1.0f, 5.0f);
        HiLoData data2 = HiLoData.hiLoDataFabric(3.0f, 8.0f);

        HiLoData merged = HiLoData.merge(data1, data2);

        assertEquals("Merged min should be lowest", 1.0f, merged._yLo, DELTA);
        assertEquals("Merged max should be highest", 8.0f, merged._yHi, DELTA);
    }

    @Test
    public void testMerge_FirstNull() {
        HiLoData data2 = HiLoData.hiLoDataFabric(3.0f, 8.0f);

        HiLoData merged = HiLoData.merge(null, data2);

        assertEquals("Should return data2", 3.0f, merged._yLo, DELTA);
        assertEquals("Should return data2", 8.0f, merged._yHi, DELTA);
    }

    @Test
    public void testMerge_SecondNull() {
        HiLoData data1 = HiLoData.hiLoDataFabric(1.0f, 5.0f);

        HiLoData merged = HiLoData.merge(data1, null);

        assertEquals("Should return data1", 1.0f, merged._yLo, DELTA);
        assertEquals("Should return data1", 5.0f, merged._yHi, DELTA);
    }

    @Test
    public void testMergeFromValue_ExtendLow() {
        HiLoData data = HiLoData.hiLoDataFabric(5.0f, 10.0f);

        HiLoData merged = HiLoData.mergeFromValue(data, 2.0f);

        assertEquals("Low should be extended", 2.0f, merged._yLo, DELTA);
        assertEquals("High should remain", 10.0f, merged._yHi, DELTA);
    }

    @Test
    public void testMergeFromValue_ExtendHigh() {
        HiLoData data = HiLoData.hiLoDataFabric(5.0f, 10.0f);

        HiLoData merged = HiLoData.mergeFromValue(data, 15.0f);

        assertEquals("Low should remain", 5.0f, merged._yLo, DELTA);
        assertEquals("High should be extended", 15.0f, merged._yHi, DELTA);
    }

    @Test
    public void testMergeFromValue_ValueInRange() {
        HiLoData data = HiLoData.hiLoDataFabric(5.0f, 10.0f);

        HiLoData merged = HiLoData.mergeFromValue(data, 7.0f);

        assertEquals("Low should remain unchanged", 5.0f, merged._yLo, DELTA);
        assertEquals("High should remain unchanged", 10.0f, merged._yHi, DELTA);
    }

    @Test
    public void testMergeFromValue_NullData() {
        HiLoData merged = HiLoData.mergeFromValue(null, 5.0f);

        assertEquals("Should create new data with value", 5.0f, merged._yLo, DELTA);
        assertEquals("Should create new data with value", 5.0f, merged._yHi, DELTA);
    }

    // === Tests for getIntervalRange ===

    @Test
    public void testGetIntervalRange() {
        HiLoData data = HiLoData.hiLoDataFabric(3.0f, 8.0f);
        assertEquals("Range should be 5", 5.0f, data.getIntervalRange(), DELTA);
    }

    @Test
    public void testGetIntervalRange_Zero() {
        HiLoData data = HiLoData.hiLoDataFabric(5.0f, 5.0f);
        assertEquals("Range should be 0", 0.0f, data.getIntervalRange(), DELTA);
    }

    // === Tests for isValidNumber ===

    @Test
    public void testIsValidNumber_True() {
        HiLoData data = HiLoData.hiLoDataFabric(1.0f, 5.0f);
        assertTrue("Normal values should be valid", data.isValidNumber());
    }

    @Test
    public void testIsValidNumber_NaN() {
        HiLoData data = HiLoData.hiLoDataFabric(Float.NaN, 5.0f);
        assertFalse("NaN should be invalid", data.isValidNumber());
    }

    @Test
    public void testIsValidNumber_Infinity() {
        HiLoData data = HiLoData.hiLoDataFabric(1.0f, Float.POSITIVE_INFINITY);
        assertFalse("Infinity should be invalid", data.isValidNumber());
    }

    // === Tests for containsNumber ===

    @Test
    public void testContainsNumber_InRange() {
        HiLoData data = HiLoData.hiLoDataFabric(0.0f, 10.0f);
        assertTrue("5 should be in range [0,10]", data.containsNumber(5.0f));
    }

    @Test
    public void testContainsNumber_AtLowBoundary() {
        HiLoData data = HiLoData.hiLoDataFabric(0.0f, 10.0f);
        assertTrue("0 should be in range [0,10]", data.containsNumber(0.0f));
    }

    @Test
    public void testContainsNumber_AtHighBoundary() {
        HiLoData data = HiLoData.hiLoDataFabric(0.0f, 10.0f);
        assertTrue("10 should be in range [0,10]", data.containsNumber(10.0f));
    }

    @Test
    public void testContainsNumber_BelowRange() {
        HiLoData data = HiLoData.hiLoDataFabric(0.0f, 10.0f);
        assertFalse("-1 should not be in range [0,10]", data.containsNumber(-1.0f));
    }

    @Test
    public void testContainsNumber_AboveRange() {
        HiLoData data = HiLoData.hiLoDataFabric(0.0f, 10.0f);
        assertFalse("11 should not be in range [0,10]", data.containsNumber(11.0f));
    }

    // === Tests for compare ===

    @Test
    public void testCompare_Equal() {
        HiLoData data1 = HiLoData.hiLoDataFabric(1.0f, 5.0f);
        HiLoData data2 = HiLoData.hiLoDataFabric(1.0f, 5.0f);
        assertTrue("Same values should compare equal", data1.compare(data2));
    }

    @Test
    public void testCompare_NotEqual() {
        HiLoData data1 = HiLoData.hiLoDataFabric(1.0f, 5.0f);
        HiLoData data2 = HiLoData.hiLoDataFabric(1.0f, 6.0f);
        assertFalse("Different values should not compare equal", data1.compare(data2));
    }

    // === Tests for equals and hashCode ===

    @Test
    public void testEquals_SameValues() {
        HiLoData data1 = HiLoData.hiLoDataFabric(2.5f, 7.5f);
        HiLoData data2 = HiLoData.hiLoDataFabric(2.5f, 7.5f);
        assertEquals("Equal HiLoData should be equal", data1, data2);
    }

    @Test
    public void testEquals_DifferentValues() {
        HiLoData data1 = HiLoData.hiLoDataFabric(2.5f, 7.5f);
        HiLoData data2 = HiLoData.hiLoDataFabric(2.5f, 8.5f);
        assertNotEquals("Different HiLoData should not be equal", data1, data2);
    }

    @Test
    public void testEquals_Null() {
        HiLoData data = HiLoData.hiLoDataFabric(1.0f, 5.0f);
        assertNotEquals("HiLoData should not equal null", data, null);
    }

    @Test
    public void testHashCode_ConsistentWithEquals() {
        HiLoData data1 = HiLoData.hiLoDataFabric(3.0f, 9.0f);
        HiLoData data2 = HiLoData.hiLoDataFabric(3.0f, 9.0f);
        assertEquals("Equal objects should have same hashCode", data1.hashCode(), data2.hashCode());
    }

    // === Tests for toString ===

    @Test
    public void testToString_ContainsValues() {
        HiLoData data = HiLoData.hiLoDataFabric(2.0f, 8.0f);
        String str = data.toString();
        assertTrue("toString should contain max value", str.contains("8.0"));
        assertTrue("toString should contain min value", str.contains("2.0"));
    }

    // === Tests for factory method caching ===

    @Test
    public void testFabric_ReturnsStaticForZeroZero() {
        HiLoData data1 = HiLoData.hiLoDataFabric(0.0f, 0.0f);
        HiLoData data2 = HiLoData.hiLoDataFabric(0.0f, 0.0f);
        assertSame("Zero-zero should return cached instance", data1, data2);
    }

    @Test
    public void testFabric_ReturnsStaticForZeroOne() {
        HiLoData data1 = HiLoData.hiLoDataFabric(0.0f, 1.0f);
        HiLoData data2 = HiLoData.hiLoDataFabric(0.0f, 1.0f);
        assertSame("Zero-one should return cached instance", data1, data2);
    }

    @Test
    public void testFabric_ReturnsStaticForOneOne() {
        HiLoData data1 = HiLoData.hiLoDataFabric(1.0f, 1.0f);
        HiLoData data2 = HiLoData.hiLoDataFabric(1.0f, 1.0f);
        assertSame("One-one should return cached instance", data1, data2);
    }
}
