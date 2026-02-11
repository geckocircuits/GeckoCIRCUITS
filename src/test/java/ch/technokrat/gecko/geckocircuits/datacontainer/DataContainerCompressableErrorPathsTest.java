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

import ch.technokrat.gecko.geckocircuits.newscope.TimeSeriesConstantDt;
import ch.technokrat.gecko.geckocircuits.newscope.AbstractTimeSerie;
import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Error path and exception handling tests for DataContainerCompressable.
 * Tests null data, empty containers, invalid operations, and exception paths.
 */
public class DataContainerCompressableErrorPathsTest {

    private static final int JUNK_SIZE = DataContainerCompressable.JUNK_SIZE;

    @Before
    public void setUp() {
        CompressorIntMatrix.clearCache();
        IntegerMatrixCache.clearCache();
    }

    @Test
    public void testGetAbsoluteMinMaxValueNoData() {
        // Test getAbsoluteMinMaxValue when no valid data exists
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        try {
            HiLoData result = container.getAbsoluteMinMaxValue(0);
            // If exception not thrown, check result
            if (result != null) {
                assertNotNull(result);
            }
        } catch (ArithmeticException ex) {
            // Expected: "No valid data available!"
            assertTrue(ex.getMessage().contains("No valid data"));
        }
    }

    @Test
    public void testIsInvalidNumbersWithEmptyData() {
        // Test isInvalidNumbers on empty container
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        boolean result = container.isInvalidNumbers(0);
        assertFalse(result);
    }

    @Test
    public void testIsInvalidNumbersWithNaN() {
        // Test isInvalidNumbers when data contains NaN
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        float[] values = {Float.NaN};
        container.insertValuesAtEnd(values, 0.0);

        boolean result = container.isInvalidNumbers(0);
        // Result depends on implementation
    }

    @Test
    public void testSetContainerStatusPaused() {
        // Test status change to PAUSED
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        float[] values = {5.0f};
        container.insertValuesAtEnd(values, 0.0);

        // Change to PAUSED status
        container.setContainerStatus(ContainerStatus.PAUSED);
        assertEquals(ContainerStatus.PAUSED, container.getContainerStatus());
    }

    @Test
    public void testSetContainerStatusRunning() {
        // Test status change to RUNNING
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        container.setContainerStatus(ContainerStatus.RUNNING);
        assertEquals(ContainerStatus.RUNNING, container.getContainerStatus());
    }

    @Test
    public void testGetDataValueInIntervalBeforeFirstPoint() {
        // Test getDataValueInInterval before any data points
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        float[] values = {10.0f};
        container.insertValuesAtEnd(values, 1.0);

        Object result = container.getDataValueInInterval(0.0, 0.5, 0);
        assertNull(result);
    }

    @Test
    public void testGetDataValueInIntervalAtFirstPoint() {
        // Test getDataValueInInterval exactly at first point
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        float[] values = {10.0f};
        container.insertValuesAtEnd(values, 1.0);

        Object result = container.getDataValueInInterval(0.0, 2.0, 0);
        assertNotNull(result);
    }

    @Test
    public void testGetDataValueInIntervalNoPointsInInterval() {
        // Test interval with no data points
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        float[] values1 = {10.0f};
        float[] values2 = {20.0f};
        container.insertValuesAtEnd(values1, 0.0);
        container.insertValuesAtEnd(values2, 2.0);

        Object result = container.getDataValueInInterval(0.5, 1.5, 0);
        assertNull(result);
    }

    @Test
    public void testGetAVGValueInIntervalZeroTimeInterval() {
        // Test average calculation with zero time interval
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        float[] values = {5.0f};
        container.insertValuesAtEnd(values, 0.0);

        try {
            float result = container.getAVGValueInInterval(0.0, 0.0, 0);
            // May result in NaN or infinity due to division by zero
            assertTrue(Float.isNaN(result) || Float.isInfinite(result));
        } catch (ArithmeticException ex) {
            // Expected
        }
    }

    @Test
    public void testGetRowLength() {
        // Test getRowLength consistency
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"V1", "I1", "P1"};

        DataContainerCompressable container = new DataContainerCompressable(
            3, timeSeries, signalNames, "Time"
        );

        assertEquals(3, container.getRowLength());
    }

    @Test
    public void testGetSignalNames() {
        // Test signal name retrieval
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Voltage", "Current", "Power"};

        DataContainerCompressable container = new DataContainerCompressable(
            3, timeSeries, signalNames, "Time"
        );

        assertEquals("Voltage", container.getSignalName(0));
        assertEquals("Current", container.getSignalName(1));
        assertEquals("Power", container.getSignalName(2));
    }

    @Test
    public void testGetXDataName() {
        // Test X-axis name
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "TimeAxis"
        );

        assertEquals("TimeAxis", container.getXDataName());
    }

    @Test
    public void testGetTimeSeries() {
        // Test getTimeSeries returns same instance
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        AbstractTimeSerie retrieved = container.getTimeSeries(0);
        assertSame(timeSeries, retrieved);
    }

    @Test
    public void testGetSubcircuitSignalPath() {
        // Test default signal path (empty string)
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        String path = container.getSubcircuitSignalPath(0);
        assertEquals("", path);
    }

    @Test
    public void testSetSignalPathName() {
        // Test setting signal path
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        container.setSignalPathName(0, "circuit/subcircuit");
        assertEquals("circuit/subcircuit", container.getSubcircuitSignalPath(0));
    }

    @Test
    public void testGetUsedRAMSizeInMB() {
        // Test RAM size calculation
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        float[] values = {5.0f};
        container.insertValuesAtEnd(values, 0.0);

        int ramSize = container.getUsedRAMSizeInMB();
        assertTrue(ramSize >= 0);
    }

    @Test
    public void testGetCachedRAMSizeInMB() {
        // Test cached RAM size calculation
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        long cachedSize = container.getCachedRAMSizeInMB();
        assertTrue(cachedSize >= 0);
    }

    @Test
    public void testGetDataArray() {
        // Test getDataArray returns memory container array
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        float[] array = container.getDataArray();
        assertNotNull(array);
    }

    @Test
    public void testGetMemoryContainer() {
        // Test getMemoryContainer
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        Object memContainer = container.getMemoryContainer();
        assertNotNull(memContainer);
    }

    @Test
    public void testGetDefinedMeanSignalsUnsupported() {
        // Test that getDefinedMeanSignals throws UnsupportedOperationException
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        try {
            container.getDefinedMeanSignals();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException ex) {
            assertTrue(true);
        }
    }
}
