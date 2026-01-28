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
import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for DataContainerCompressable - compressed simulation data storage.
 * These tests verify data integrity through compression/decompression cycles.
 */
public class DataContainerCompressableTest {

    private static final int JUNK_SIZE = DataContainerCompressable.JUNK_SIZE; // 4096

    @Before
    public void setUp() {
        // Clear compression caches
        CompressorIntMatrix.clearCache();
    }

    @After
    public void tearDown() {
        CompressorIntMatrix.clearCache();
    }

    @Test
    public void testBasicConstruction() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"V1", "I1"};

        DataContainerCompressable container = new DataContainerCompressable(
            2, timeSeries, signalNames, "Time"
        );

        assertEquals(2, container.getRowLength());
        assertEquals("V1", container.getSignalName(0));
        assertEquals("I1", container.getSignalName(1));
        assertEquals("Time", container.getXDataName());
    }

    @Test
    public void testSingleValueInsertRetrieve() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        float[] values = {42.5f};
        container.insertValuesAtEnd(values, 0.0);

        assertEquals(42.5f, container.getValue(0, 0), 0.001f);
    }

    @Test
    public void testMultipleRowsInsertRetrieve() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"V", "I", "P"};

        DataContainerCompressable container = new DataContainerCompressable(
            3, timeSeries, signalNames, "Time"
        );

        float[] values = {10.0f, 2.5f, 25.0f};
        container.insertValuesAtEnd(values, 0.0);

        assertEquals(10.0f, container.getValue(0, 0), 0.001f);
        assertEquals(2.5f, container.getValue(1, 0), 0.001f);
        assertEquals(25.0f, container.getValue(2, 0), 0.001f);
    }

    @Test
    public void testDataIntegrityAcrossJunkBoundary() {
        // Insert enough data to cross junk boundary (JUNK_SIZE = 4096)
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        int dataPoints = JUNK_SIZE + 100; // Cross boundary
        for (int i = 0; i < dataPoints; i++) {
            float[] values = {i * 0.1f};
            container.insertValuesAtEnd(values, i * 0.0001);
        }

        // Verify data across boundary
        assertEquals(0.0f, container.getValue(0, 0), 0.001f);
        assertEquals((JUNK_SIZE - 1) * 0.1f, container.getValue(0, JUNK_SIZE - 1), 0.1f);
        assertEquals(JUNK_SIZE * 0.1f, container.getValue(0, JUNK_SIZE), 0.1f);
    }

    @Test
    public void testContainerStatus() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        assertEquals(ContainerStatus.RUNNING, container.getContainerStatus());

        container.setContainerStatus(ContainerStatus.PAUSED);
        assertEquals(ContainerStatus.PAUSED, container.getContainerStatus());
    }

    @Test
    public void testTimeValueRetrieval() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        for (int i = 0; i < 10; i++) {
            float[] values = {1.0f};
            container.insertValuesAtEnd(values, i * 0.001);
        }

        assertEquals(0.005, container.getTimeValue(5, 0), 0.0001);
    }

    @Test
    public void testMaximumTimeIndex() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        for (int i = 0; i < 100; i++) {
            float[] values = {1.0f};
            container.insertValuesAtEnd(values, i * 0.001);
        }

        assertEquals(99, container.getMaximumTimeIndex(0));
    }

    @Test
    public void testHiLoValueCalculation() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        // Insert values 0-99
        for (int i = 0; i < 100; i++) {
            float[] values = {(float)i};
            container.insertValuesAtEnd(values, i * 0.001);
        }

        HiLoData hiLo = container.getHiLoValue(0, 10, 50);
        assertNotNull(hiLo);
        assertTrue(hiLo._yHi >= 50);
        assertTrue(hiLo._yLo <= 10);
    }

    @Test
    public void testNegativeValues() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        float[] values = {-123.456f};
        container.insertValuesAtEnd(values, 0.0);

        assertEquals(-123.456f, container.getValue(0, 0), 0.01f);
    }

    @Test
    public void testZeroValues() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        float[] values = {0.0f};
        container.insertValuesAtEnd(values, 0.0);

        assertEquals(0.0f, container.getValue(0, 0), 0.001f);
    }

    @Test
    public void testUsedRAMSizeReported() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        // Insert some data
        for (int i = 0; i < 1000; i++) {
            float[] values = {(float)i};
            container.insertValuesAtEnd(values, i * 0.001);
        }

        int ramSize = container.getUsedRAMSizeInMB();
        assertTrue("RAM size should be non-negative", ramSize >= 0);
    }

    @Test
    public void testFindTimeIndex() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        for (int i = 0; i < 100; i++) {
            float[] values = {1.0f};
            container.insertValuesAtEnd(values, i * 0.001);
        }

        int index = container.findTimeIndex(0.050, 0);
        assertEquals(50, index);
    }
}
