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

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for TimeSeriesConstantDt - time series with constant time step.
 */
public class TimeSeriesConstantDtTest {

    private static final double DELTA = 1e-12;
    private TimeSeriesConstantDt timeSeries;

    @Before
    public void setUp() {
        timeSeries = new TimeSeriesConstantDt();
    }

    @Test
    public void testSetValue_FirstValue_SetsMinTime() {
        timeSeries.setValue(0, 0.0);
        assertEquals("First value sets min time", 0.0, timeSeries.getValue(0), DELTA);
    }

    @Test
    public void testSetValue_SecondValue_CalculatesDt() {
        timeSeries.setValue(0, 0.0);
        timeSeries.setValue(1, 1e-6);

        assertEquals("Value at index 0", 0.0, timeSeries.getValue(0), DELTA);
        assertEquals("Value at index 1", 1e-6, timeSeries.getValue(1), DELTA);
    }

    @Test
    public void testGetValue_CalculatesFromDt() {
        double dt = 1e-5;
        timeSeries.setValue(0, 0.0);
        timeSeries.setValue(1, dt);
        timeSeries.setValue(10, 10 * dt);  // Need to set max index to access up to 10

        // Value at index n should be n * dt (only if index <= maxDefinedIndex)
        assertEquals("Value at index 5", 5 * dt, timeSeries.getValue(5), DELTA);
        assertEquals("Value at index 10", 10 * dt, timeSeries.getValue(10), DELTA);
    }

    @Test
    public void testGetMaximumIndex_InitiallyNegative() {
        assertEquals("Initial max index should be -1", -1, timeSeries.getMaximumIndex());
    }

    @Test
    public void testGetMaximumIndex_AfterSetValue() {
        timeSeries.setValue(0, 0.0);
        assertEquals("Max index after first set", 0, timeSeries.getMaximumIndex());

        timeSeries.setValue(5, 5e-6);
        assertEquals("Max index after setting index 5", 5, timeSeries.getMaximumIndex());
    }

    @Test
    public void testGetMaximumIndex_TracksHighestIndex() {
        // TimeSeriesConstantDt expects values to be set in order with constant dt
        // Setting out of order triggers internal assertions, so test in-order setting
        timeSeries.setValue(0, 0.0);
        timeSeries.setValue(1, 1e-6);
        timeSeries.setValue(5, 5e-6);
        timeSeries.setValue(10, 10e-6);

        assertEquals("Max index should be 10", 10, timeSeries.getMaximumIndex());
    }

    @Test
    public void testFindTimeIndex_ExactMatch() {
        // Use index < 100 (ADAPT_THRESHOLD) so dt gets calculated
        timeSeries.setValue(0, 0.0);
        timeSeries.setValue(50, 1.0);  // dt = 1.0/50 = 0.02

        int index = timeSeries.findTimeIndex(0.5);
        // 0.5 / 0.02 = 25
        assertEquals("Index for time 0.5 should be 25", 25, index);
    }

    @Test
    public void testFindTimeIndex_BeforeStart() {
        timeSeries.setValue(0, 0.0);
        timeSeries.setValue(50, 1.0);

        int index = timeSeries.findTimeIndex(-0.5);
        assertEquals("Negative time should clamp to 0", 0, index);
    }

    @Test
    public void testFindTimeIndex_AfterEnd() {
        timeSeries.setValue(0, 0.0);
        timeSeries.setValue(50, 1.0);

        int index = timeSeries.findTimeIndex(2.0);
        assertEquals("Time beyond end should clamp to max", 50, index);
    }

    @Test
    public void testGetLastTimeInterval() {
        timeSeries.setValue(0, 0.0);
        timeSeries.setValue(1, 1e-6);

        assertEquals("Last time interval should be dt", 1e-6, timeSeries.getLastTimeInterval(), DELTA);
    }

    @Test
    public void testGetValue_BeyondMaxIndex_ReturnsZero() {
        timeSeries.setValue(0, 0.0);
        timeSeries.setValue(10, 10e-6);

        // Access beyond max defined index
        assertEquals("Value beyond max should be 0", 0.0, timeSeries.getValue(100), DELTA);
    }

    @Test
    public void testConstantDt_WithNonZeroStart() {
        double startTime = 1.0;
        double dt = 0.001;

        timeSeries.setValue(0, startTime);
        timeSeries.setValue(1, startTime + dt);
        timeSeries.setValue(10, startTime + 10 * dt);

        assertEquals("Value at index 5", startTime + 5 * dt, timeSeries.getValue(5), DELTA);
    }

    @Test
    public void testFindTimeIndex_AtStartTime() {
        timeSeries.setValue(0, 0.0);
        timeSeries.setValue(50, 1.0);

        assertEquals("Index at start time should be 0", 0, timeSeries.findTimeIndex(0.0));
    }

    @Test
    public void testFindTimeIndex_AtEndTime() {
        timeSeries.setValue(0, 0.0);
        timeSeries.setValue(50, 1.0);

        assertEquals("Index at end time should be max", 50, timeSeries.findTimeIndex(1.0));
    }
}
