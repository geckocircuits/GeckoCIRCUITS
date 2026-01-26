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
package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for AverageValue class - weighted average calculations over intervals.
 */
public class AverageValueTest {

    private static final double DELTA = 1e-9;

    // ====================================================
    // Constructor Tests
    // ====================================================

    @Test
    public void testConstructor_BasicValues() {
        AverageValue avg = new AverageValue(5.0, 0.0, 10.0);
        assertEquals(5.0, avg.getAverageValue(), DELTA);
        assertEquals(0.0, avg.getIntervalStart(), DELTA);
        assertEquals(10.0, avg.getIntervalStop(), DELTA);
    }

    @Test
    public void testConstructor_NegativeInterval() {
        AverageValue avg = new AverageValue(2.5, -5.0, 5.0);
        assertEquals(2.5, avg.getAverageValue(), DELTA);
        assertEquals(-5.0, avg.getIntervalStart(), DELTA);
        assertEquals(5.0, avg.getIntervalStop(), DELTA);
    }

    @Test
    public void testConstructor_ZeroSpan() {
        AverageValue avg = new AverageValue(1.0, 5.0, 5.0);
        assertEquals(0.0, avg.getAverageSpan(), DELTA);
    }

    @Test
    public void testCopyConstructor() {
        AverageValue original = new AverageValue(10.0, 1.0, 3.0);
        AverageValue copy = new AverageValue(original);
        
        assertEquals(original.getAverageValue(), copy.getAverageValue(), DELTA);
        assertEquals(original.getIntervalStart(), copy.getIntervalStart(), DELTA);
        assertEquals(original.getIntervalStop(), copy.getIntervalStop(), DELTA);
    }

    // ====================================================
    // AverageSpan Tests
    // ====================================================

    @Test
    public void testGetAverageSpan_PositiveInterval() {
        AverageValue avg = new AverageValue(5.0, 0.0, 10.0);
        assertEquals(10.0, avg.getAverageSpan(), DELTA);
    }

    @Test
    public void testGetAverageSpan_SmallInterval() {
        AverageValue avg = new AverageValue(5.0, 0.0, 0.001);
        assertEquals(0.001, avg.getAverageSpan(), DELTA);
    }

    @Test
    public void testGetAverageSpan_LargeInterval() {
        AverageValue avg = new AverageValue(5.0, 0.0, 1000000.0);
        assertEquals(1000000.0, avg.getAverageSpan(), DELTA);
    }

    // ====================================================
    // AppendAverage Tests - Weighted Average Combination
    // ====================================================

    @Test
    public void testAppendAverage_EqualSpans() {
        // Two intervals of same span - result should be arithmetic mean
        AverageValue avg1 = new AverageValue(10.0, 0.0, 5.0);  // span=5, avg=10
        AverageValue avg2 = new AverageValue(20.0, 5.0, 10.0); // span=5, avg=20
        
        avg1.appendAverage(avg2);
        
        // (10*5 + 20*5) / (5+5) = 150/10 = 15
        assertEquals(15.0, avg1.getAverageValue(), DELTA);
        assertEquals(0.0, avg1.getIntervalStart(), DELTA);
        assertEquals(10.0, avg1.getIntervalStop(), DELTA);
    }

    @Test
    public void testAppendAverage_UnequalSpans() {
        // Weighted by interval length
        AverageValue avg1 = new AverageValue(10.0, 0.0, 2.0);  // span=2, avg=10
        AverageValue avg2 = new AverageValue(20.0, 2.0, 10.0); // span=8, avg=20
        
        avg1.appendAverage(avg2);
        
        // (10*2 + 20*8) / (2+8) = (20+160)/10 = 18
        assertEquals(18.0, avg1.getAverageValue(), DELTA);
        assertEquals(10.0, avg1.getAverageSpan(), DELTA);
    }

    @Test
    public void testAppendAverage_NullAppend() {
        AverageValue avg = new AverageValue(10.0, 0.0, 5.0);
        avg.appendAverage(null);
        
        // Should not change
        assertEquals(10.0, avg.getAverageValue(), DELTA);
        assertEquals(0.0, avg.getIntervalStart(), DELTA);
        assertEquals(5.0, avg.getIntervalStop(), DELTA);
    }

    @Test
    public void testAppendAverage_VerySmallSpan() {
        AverageValue avg1 = new AverageValue(10.0, 0.0, 100.0);  // span=100
        AverageValue avg2 = new AverageValue(1000.0, 100.0, 100.001); // span=0.001
        
        avg1.appendAverage(avg2);
        
        // Large span dominates - result should be close to 10
        assertTrue(avg1.getAverageValue() > 9.0 && avg1.getAverageValue() < 11.0);
    }

    @Test
    public void testAppendAverage_MultipleAppends() {
        AverageValue avg = new AverageValue(10.0, 0.0, 1.0);
        avg.appendAverage(new AverageValue(20.0, 1.0, 2.0));
        avg.appendAverage(new AverageValue(30.0, 2.0, 3.0));
        
        // (10*1 + 20*1 + 30*1) / 3 = 60/3 = 20
        assertEquals(20.0, avg.getAverageValue(), DELTA);
        assertEquals(3.0, avg.getAverageSpan(), DELTA);
    }

    @Test
    public void testAppendAverage_NegativeValues() {
        AverageValue avg1 = new AverageValue(-10.0, 0.0, 5.0);
        AverageValue avg2 = new AverageValue(-20.0, 5.0, 10.0);
        
        avg1.appendAverage(avg2);
        
        assertEquals(-15.0, avg1.getAverageValue(), DELTA);
    }

    @Test
    public void testAppendAverage_PositiveAndNegative() {
        AverageValue avg1 = new AverageValue(10.0, 0.0, 5.0);
        AverageValue avg2 = new AverageValue(-10.0, 5.0, 10.0);
        
        avg1.appendAverage(avg2);
        
        // (10*5 + (-10)*5) / 10 = 0
        assertEquals(0.0, avg1.getAverageValue(), DELTA);
    }

    // ====================================================
    // ToString Tests
    // ====================================================

    @Test
    public void testToString_ContainsAllValues() {
        AverageValue avg = new AverageValue(5.5, 1.0, 3.0);
        String str = avg.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("5.5"));
        assertTrue(str.contains("1.0"));
        assertTrue(str.contains("3.0"));
    }

    // ====================================================
    // Edge Cases
    // ====================================================

    @Test
    public void testZeroAverage() {
        AverageValue avg = new AverageValue(0.0, 0.0, 10.0);
        assertEquals(0.0, avg.getAverageValue(), DELTA);
    }

    @Test
    public void testVeryLargeValues() {
        AverageValue avg = new AverageValue(1e15, 0.0, 1e10);
        assertEquals(1e15, avg.getAverageValue(), DELTA);
        assertEquals(1e10, avg.getAverageSpan(), DELTA);
    }

    @Test
    public void testVerySmallValues() {
        AverageValue avg = new AverageValue(1e-15, 0.0, 1e-10);
        assertEquals(1e-15, avg.getAverageValue(), DELTA);
        assertEquals(1e-10, avg.getAverageSpan(), DELTA);
    }

    // ====================================================
    // Physical Interpretation Tests (Signal Processing)
    // ====================================================

    @Test
    public void testAverageValue_DCSignal() {
        // DC signal has same average over any interval
        AverageValue dc1 = new AverageValue(5.0, 0.0, 10.0);
        AverageValue dc2 = new AverageValue(5.0, 10.0, 100.0);
        
        dc1.appendAverage(dc2);
        assertEquals(5.0, dc1.getAverageValue(), DELTA);
    }

    @Test
    public void testAverageValue_RampSignal() {
        // Average of a linear ramp from 0 to 10 over interval 0-10 is 5
        AverageValue ramp = new AverageValue(5.0, 0.0, 10.0);
        assertEquals(5.0, ramp.getAverageValue(), DELTA);
    }
}
