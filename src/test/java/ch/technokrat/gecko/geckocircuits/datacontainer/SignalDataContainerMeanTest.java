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
package ch.technokrat.gecko.geckocircuits.datacontainer;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Test suite for AverageValue class and SignalDataContainerMean integration.
 * Tests mean/average functionality for signal data containers.
 */
public class SignalDataContainerMeanTest {

    private static final double DELTA = 1e-9;

    // ====================================================
    // AverageValue Constructor Tests
    // ====================================================

    @Test
    public void testAverageValueConstructor() {
        AverageValue avg = new AverageValue(5.0, 0.0, 1.0);
        assertEquals(5.0, avg.getAverageValue(), DELTA);
        assertEquals(0.0, avg.getIntervalStart(), DELTA);
        assertEquals(1.0, avg.getIntervalStop(), DELTA);
    }

    @Test
    public void testAverageValueConstructor_NegativeValues() {
        AverageValue avg = new AverageValue(-10.5, -5.0, 5.0);
        assertEquals(-10.5, avg.getAverageValue(), DELTA);
        assertEquals(-5.0, avg.getIntervalStart(), DELTA);
        assertEquals(5.0, avg.getIntervalStop(), DELTA);
    }

    @Test
    public void testAverageValueConstructor_ZeroMean() {
        AverageValue avg = new AverageValue(0.0, 0.0, 2.0);
        assertEquals(0.0, avg.getAverageValue(), DELTA);
        assertEquals(2.0, avg.getIntervalStop() - avg.getIntervalStart(), DELTA);
    }

    @Test
    public void testAverageValueConstructor_LargeValues() {
        AverageValue avg = new AverageValue(1e15, 0.0, 1e10);
        assertEquals(1e15, avg.getAverageValue(), DELTA);
        assertEquals(1e10, avg.getIntervalStop() - avg.getIntervalStart(), DELTA);
    }

    // ====================================================
    // AverageValue Copy Constructor Tests
    // ====================================================

    @Test
    public void testAverageValueCopy() {
        AverageValue original = new AverageValue(3.5, 0.0, 2.0);
        AverageValue copy = new AverageValue(original);

        assertEquals(original.getAverageValue(), copy.getAverageValue(), DELTA);
        assertEquals(original.getIntervalStart(), copy.getIntervalStart(), DELTA);
        assertEquals(original.getIntervalStop(), copy.getIntervalStop(), DELTA);
    }

    @Test
    public void testAverageValueCopy_Independence() {
        AverageValue original = new AverageValue(5.0, 0.0, 10.0);
        AverageValue copy = new AverageValue(original);

        // Modify copy by appending a new average
        copy.appendAverage(new AverageValue(10.0, 10.0, 20.0));

        // Original should remain unchanged
        assertEquals(5.0, original.getAverageValue(), DELTA);
        assertEquals(10.0, original.getIntervalStop(), DELTA);
    }

    @Test
    public void testAverageValueCopy_NegativeValues() {
        AverageValue original = new AverageValue(-7.5, -10.0, -5.0);
        AverageValue copy = new AverageValue(original);

        assertEquals(original.getAverageValue(), copy.getAverageValue(), DELTA);
        assertEquals(-7.5, copy.getAverageValue(), DELTA);
    }

    // ====================================================
    // AverageValue Interval Tests
    // ====================================================

    @Test
    public void testAverageValueInterval() {
        AverageValue avg = new AverageValue(5.0, 1.5, 3.5);
        // Interval width = 3.5 - 1.5 = 2.0
        assertEquals(2.0, avg.getIntervalStop() - avg.getIntervalStart(), DELTA);
    }

    @Test
    public void testAverageValueGetAverageSpan() {
        AverageValue avg = new AverageValue(5.0, 0.0, 10.0);
        assertEquals(10.0, avg.getAverageSpan(), DELTA);
    }

    @Test
    public void testAverageValueGetAverageSpan_SmallInterval() {
        AverageValue avg = new AverageValue(5.0, 0.0, 0.001);
        assertEquals(0.001, avg.getAverageSpan(), DELTA);
    }

    @Test
    public void testAverageValueGetAverageSpan_ZeroSpan() {
        AverageValue avg = new AverageValue(5.0, 5.0, 5.0);
        assertEquals(0.0, avg.getAverageSpan(), DELTA);
    }

    // ====================================================
    // AverageValue Append Tests
    // ====================================================

    @Test
    public void testAverageValueAppend_EqualSpans() {
        AverageValue avg1 = new AverageValue(10.0, 0.0, 1.0);
        AverageValue avg2 = new AverageValue(20.0, 1.0, 2.0);

        avg1.appendAverage(avg2);

        // (10*1 + 20*1) / (1+1) = 30/2 = 15
        assertEquals(15.0, avg1.getAverageValue(), DELTA);
        assertEquals(2.0, avg1.getIntervalStop(), DELTA);
        assertEquals(0.0, avg1.getIntervalStart(), DELTA);
    }

    @Test
    public void testAverageValueAppend_UnequalSpans() {
        AverageValue avg1 = new AverageValue(10.0, 0.0, 2.0);  // span=2, avg=10
        AverageValue avg2 = new AverageValue(20.0, 2.0, 10.0); // span=8, avg=20

        avg1.appendAverage(avg2);

        // (10*2 + 20*8) / (2+8) = (20+160)/10 = 18
        assertEquals(18.0, avg1.getAverageValue(), DELTA);
        assertEquals(10.0, avg1.getIntervalStop(), DELTA);
    }

    @Test
    public void testAverageValueAppend_Null() {
        AverageValue avg = new AverageValue(10.0, 0.0, 5.0);
        avg.appendAverage(null);

        // Should not change
        assertEquals(10.0, avg.getAverageValue(), DELTA);
        assertEquals(0.0, avg.getIntervalStart(), DELTA);
        assertEquals(5.0, avg.getIntervalStop(), DELTA);
    }

    @Test
    public void testAverageValueAppend_MultipleSequential() {
        AverageValue avg = new AverageValue(10.0, 0.0, 1.0);
        avg.appendAverage(new AverageValue(20.0, 1.0, 2.0));
        avg.appendAverage(new AverageValue(30.0, 2.0, 3.0));

        // (10*1 + 20*1 + 30*1) / 3 = 60/3 = 20
        assertEquals(20.0, avg.getAverageValue(), DELTA);
        assertEquals(3.0, avg.getIntervalStop(), DELTA);
    }

    @Test
    public void testAverageValueAppend_DominantFirstValue() {
        AverageValue avg1 = new AverageValue(10.0, 0.0, 100.0);  // span=100, avg=10
        AverageValue avg2 = new AverageValue(1000.0, 100.0, 100.001); // span=0.001, avg=1000

        avg1.appendAverage(avg2);

        // (10*100 + 1000*0.001) / (100+0.001) ≈ 9.99...
        // Large span dominates - result should be close to 10
        assertTrue(avg1.getAverageValue() > 9.0 && avg1.getAverageValue() < 11.0);
    }

    @Test
    public void testAverageValueAppend_DominantSecondValue() {
        AverageValue avg1 = new AverageValue(10.0, 0.0, 0.001);  // span=0.001, avg=10
        AverageValue avg2 = new AverageValue(100.0, 0.001, 100.0); // span≈100, avg=100

        avg1.appendAverage(avg2);

        // Result should be close to 100
        assertTrue(avg1.getAverageValue() > 99.0 && avg1.getAverageValue() < 101.0);
    }

    @Test
    public void testAverageValueAppend_NegativeValues() {
        AverageValue avg1 = new AverageValue(-10.0, 0.0, 5.0);
        AverageValue avg2 = new AverageValue(-20.0, 5.0, 10.0);

        avg1.appendAverage(avg2);

        // (-10*5 + -20*5) / 10 = -150/10 = -15
        assertEquals(-15.0, avg1.getAverageValue(), DELTA);
    }

    @Test
    public void testAverageValueAppend_PositiveAndNegative() {
        AverageValue avg1 = new AverageValue(10.0, 0.0, 5.0);
        AverageValue avg2 = new AverageValue(-10.0, 5.0, 10.0);

        avg1.appendAverage(avg2);

        // (10*5 + (-10)*5) / 10 = 0
        assertEquals(0.0, avg1.getAverageValue(), DELTA);
    }

    @Test
    public void testAverageValueAppend_ZeroValues() {
        AverageValue avg1 = new AverageValue(0.0, 0.0, 5.0);
        AverageValue avg2 = new AverageValue(0.0, 5.0, 10.0);

        avg1.appendAverage(avg2);

        assertEquals(0.0, avg1.getAverageValue(), DELTA);
    }

    @Test
    public void testAverageValueAppend_AssymmetricWeights() {
        AverageValue avg1 = new AverageValue(5.0, 0.0, 1.0);   // 1/10 weight
        AverageValue avg2 = new AverageValue(95.0, 1.0, 10.0); // 9/10 weight

        avg1.appendAverage(avg2);

        // (5*1 + 95*9) / 10 = (5+855)/10 = 86
        assertEquals(86.0, avg1.getAverageValue(), DELTA);
    }

    // ====================================================
    // AverageValue toString() Tests
    // ====================================================

    @Test
    public void testAverageValueToString() {
        AverageValue avg = new AverageValue(5.5, 1.0, 3.0);
        String str = avg.toString();

        assertNotNull(str);
        assertTrue(str.contains("5.5") || str.contains("5,5"));
        assertTrue(str.contains("1.0") || str.contains("1,0"));
        assertTrue(str.contains("3.0") || str.contains("3,0"));
    }

    @Test
    public void testAverageValueToString_NegativeValues() {
        AverageValue avg = new AverageValue(-3.5, -10.0, 10.0);
        String str = avg.toString();

        assertNotNull(str);
        assertTrue(str.length() > 0);
    }

    // ====================================================
    // Edge Cases and Special Values
    // ====================================================

    @Test
    public void testAverageValue_VerySmallNumbers() {
        AverageValue avg = new AverageValue(1e-15, 0.0, 1e-10);
        assertEquals(1e-15, avg.getAverageValue(), DELTA);
        assertEquals(1e-10, avg.getAverageSpan(), DELTA);
    }

    @Test
    public void testAverageValue_VeryLargeNumbers() {
        AverageValue avg = new AverageValue(1e15, 0.0, 1e10);
        assertEquals(1e15, avg.getAverageValue(), DELTA);
        assertEquals(1e10, avg.getAverageSpan(), DELTA);
    }

    @Test
    public void testAverageValue_MixedScales() {
        AverageValue avg1 = new AverageValue(1e-5, 0.0, 1.0);
        AverageValue avg2 = new AverageValue(1e5, 1.0, 2.0);

        avg1.appendAverage(avg2);

        // Result should handle mixed scales gracefully
        assertTrue(avg1.getAverageValue() > 0);
    }

    @Test
    public void testAverageValue_ConsecutiveIntervals() {
        AverageValue avg1 = new AverageValue(10.0, 0.0, 1.0);
        AverageValue avg2 = new AverageValue(20.0, 1.0, 2.0);
        AverageValue avg3 = new AverageValue(30.0, 2.0, 3.0);
        AverageValue avg4 = new AverageValue(40.0, 3.0, 4.0);

        avg1.appendAverage(avg2);
        avg1.appendAverage(avg3);
        avg1.appendAverage(avg4);

        // (10+20+30+40) / 4 = 25
        assertEquals(25.0, avg1.getAverageValue(), DELTA);
        assertEquals(4.0, avg1.getIntervalStop(), DELTA);
        assertEquals(0.0, avg1.getIntervalStart(), DELTA);
    }

    @Test
    public void testAverageValue_PhysicalInterpretation_DCSignal() {
        // DC signal has same average over any interval
        AverageValue dc1 = new AverageValue(5.0, 0.0, 10.0);
        AverageValue dc2 = new AverageValue(5.0, 10.0, 100.0);

        dc1.appendAverage(dc2);
        assertEquals(5.0, dc1.getAverageValue(), DELTA);
    }

    @Test
    public void testAverageValue_PhysicalInterpretation_RampSignal() {
        // Average of linear ramp from 0 to 10 is 5
        AverageValue ramp = new AverageValue(5.0, 0.0, 10.0);
        assertEquals(5.0, ramp.getAverageValue(), DELTA);
    }

    @Test
    public void testAverageValue_LongSequence() {
        AverageValue result = new AverageValue(1.0, 0.0, 1.0);

        // Append many values
        for (int i = 1; i <= 99; i++) {
            result.appendAverage(new AverageValue((double) (i + 1), i, i + 1.0));
        }

        // Average of 1,2,3,...,100 = 50.5
        assertEquals(50.5, result.getAverageValue(), DELTA);
        assertEquals(100.0, result.getIntervalStop(), DELTA);
    }

    @Test
    public void testAverageValue_WeightedAveragePrecision() {
        // Test precision in weighted average calculation
        AverageValue avg1 = new AverageValue(100.0, 0.0, 1.0);
        AverageValue avg2 = new AverageValue(101.0, 1.0, 2.0);

        avg1.appendAverage(avg2);

        // (100 + 101) / 2 = 100.5
        assertEquals(100.5, avg1.getAverageValue(), DELTA);
    }

    // ====================================================
    // SignalDataContainerMean Tests
    // ====================================================

    @Test
    public void testSignalDataContainerMeanConstruction() {
        DataContainerGlobal global = new DataContainerGlobal();
        global.init(1, new String[]{"V1"}, "t");
        SignalDataContainerRegular origSignal = new SignalDataContainerRegular(global, 0);
        java.util.List<Integer> indices = java.util.Arrays.asList(0);
        ScopeWrapperIndices scopeIndices = new ScopeWrapperIndices(indices, global);
        DataContainerMeanWrapper meanWrapper = new DataContainerMeanWrapper(global, scopeIndices);
        SignalDataContainerMean mean = new SignalDataContainerMean(origSignal, meanWrapper, 0);
        assertNotNull(mean);
    }

    @Test
    public void testSignalDataContainerMeanSignalName() {
        DataContainerGlobal global = new DataContainerGlobal();
        global.init(1, new String[]{"Voltage"}, "t");
        SignalDataContainerRegular origSignal = new SignalDataContainerRegular(global, 0);
        java.util.List<Integer> indices = java.util.Arrays.asList(0);
        ScopeWrapperIndices scopeIndices = new ScopeWrapperIndices(indices, global);
        DataContainerMeanWrapper meanWrapper = new DataContainerMeanWrapper(global, scopeIndices);
        SignalDataContainerMean mean = new SignalDataContainerMean(origSignal, meanWrapper, 0);
        assertEquals("Voltage_mean", mean.getSignalName());
    }

    @Test
    public void testSignalDataContainerMeanDataContainer() {
        DataContainerGlobal global = new DataContainerGlobal();
        global.init(1, new String[]{"V1"}, "t");
        SignalDataContainerRegular origSignal = new SignalDataContainerRegular(global, 0);
        java.util.List<Integer> indices = java.util.Arrays.asList(0);
        ScopeWrapperIndices scopeIndices = new ScopeWrapperIndices(indices, global);
        DataContainerMeanWrapper meanWrapper = new DataContainerMeanWrapper(global, scopeIndices);
        SignalDataContainerMean mean = new SignalDataContainerMean(origSignal, meanWrapper, 0);
        assertSame(meanWrapper, mean.getDataContainer());
        assertEquals(0, mean.getContainerSignalIndex());
    }
}
