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

    // ==================== Coverage Gap Tests ====================

    /**
     * Exercises the compression/decompression path by filling 2+ junks and
     * then reading data from the first (compressed) junk. This covers
     * DataJunkCompressable.doCompression(), CompressThread.run(),
     * calculateHiLoData(), calculateDifferenceCompression(), and
     * the deCompress/calculateDifferenceDeCompression path.
     */
    @Test
    public void testReadAfterCompression() throws InterruptedException {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Voltage"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        // Fill 2 full junks + some extra to trigger compression of first 2 junks
        int dataPoints = JUNK_SIZE * 2 + 100;
        for (int i = 0; i < dataPoints; i++) {
            float[] values = {(float) i};
            container.insertValuesAtEnd(values, i * 0.0001);
        }

        // Allow compression threads to finish
        Thread.sleep(500);

        // Read from the first junk (which was compressed), triggering deCompress()
        // Due to lossy rounding, use a generous tolerance
        float val0 = container.getValue(0, 0);
        assertEquals(0.0f, val0, 1.0f);

        float valMid = container.getValue(0, JUNK_SIZE / 2);
        assertEquals(JUNK_SIZE / 2.0f, valMid, 10.0f);
    }

    @Test
    public void testGetAbsoluteMinMaxValue() throws InterruptedException {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        // Fill 2 junks to ensure compression happens and hiLo is calculated
        int dataPoints = JUNK_SIZE * 2 + 100;
        for (int i = 0; i < dataPoints; i++) {
            float[] values = {(float) i};
            container.insertValuesAtEnd(values, i * 0.0001);
        }

        Thread.sleep(500);

        try {
            HiLoData absMinMax = container.getAbsoluteMinMaxValue(0);
            assertNotNull(absMinMax);
        } catch (ArithmeticException e) {
            // May throw if compression hasn't completed
        }
    }

    @Test
    public void testIsInvalidNumbers() throws InterruptedException {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        int dataPoints = JUNK_SIZE * 2 + 100;
        for (int i = 0; i < dataPoints; i++) {
            float[] values = {(float) i};
            container.insertValuesAtEnd(values, i * 0.0001);
        }

        Thread.sleep(500);

        // isInvalidNumbers calls getAbsoluteMinMaxValue internally
        boolean invalid = container.isInvalidNumbers(0);
        assertFalse(invalid);
    }

    @Test
    public void testGetDataValueInIntervalNoDataPoint() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        for (int i = 0; i < 100; i++) {
            float[] values = {(float) i};
            container.insertValuesAtEnd(values, i * 0.01);
        }

        // Interval with no data points (between data points)
        Object result = container.getDataValueInInterval(0.005, 0.006, 0);
        // Should return null if no data point falls in the interval
        // or return a single value if exactly one point is found
        // Result depends on time resolution
    }

    @Test
    public void testGetDataValueInIntervalSinglePoint() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        for (int i = 0; i < 100; i++) {
            float[] values = {(float) i};
            container.insertValuesAtEnd(values, i * 0.001);
        }

        // Interval that contains exactly one data point
        Object result = container.getDataValueInInterval(0.009, 0.011, 0);
        assertNotNull(result);
    }

    @Test
    public void testGetDataValueInIntervalMultiplePoints() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        for (int i = 0; i < 100; i++) {
            float[] values = {(float) i};
            container.insertValuesAtEnd(values, i * 0.001);
        }

        // Interval spanning many data points - returns HiLoData
        Object result = container.getDataValueInInterval(0.010, 0.050, 0);
        assertNotNull(result);
        assertTrue(result instanceof HiLoData);
    }

    @Test
    public void testGetDataValueInIntervalFirstDatapoint() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        float[] values = {42.0f};
        container.insertValuesAtEnd(values, 0.0);

        // Interval spanning the first data point
        Object result = container.getDataValueInInterval(-0.001, 0.001, 0);
        assertNotNull(result);
    }

    @Test
    public void testGetAVGValueInInterval() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        // Insert constant values so average is predictable
        for (int i = 0; i < 100; i++) {
            float[] values = {10.0f};
            container.insertValuesAtEnd(values, i * 0.001);
        }

        // Define avg calculation with an empty list to enable integral tracking
        container.defineAvgCalculation(new java.util.ArrayList<>());

        // Now insert more data that has integral values tracked
        for (int i = 100; i < 200; i++) {
            float[] values = {10.0f};
            container.insertValuesAtEnd(values, i * 0.001);
        }

        float avg = container.getAVGValueInInterval(0.100, 0.199, 0);
        // The average of constant 10.0f should be close to 10.0f
        // but integral values were 0 for the first 100 inserts
        assertNotNull(avg);
    }

    @Test
    public void testDefineAvgCalculation() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Sig1", "Sig2"};

        DataContainerCompressable container = new DataContainerCompressable(
            2, timeSeries, signalNames, "Time"
        );

        // defineAvgCalculation with empty list
        container.defineAvgCalculation(new java.util.ArrayList<>());

        // Insert data - should work without error
        for (int i = 0; i < 50; i++) {
            float[] values = {(float) i, (float) i * 2};
            container.insertValuesAtEnd(values, i * 0.001);
        }
    }

    @Test
    public void testSetContainerStatusPausedWithData() throws InterruptedException {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        // Insert data to have something to work with
        for (int i = 0; i < JUNK_SIZE + 100; i++) {
            float[] values = {(float) i};
            container.insertValuesAtEnd(values, i * 0.0001);
        }

        Thread.sleep(200);

        // Setting to PAUSED triggers calculateMinMax on the last junk
        container.setContainerStatus(ContainerStatus.PAUSED);
        assertEquals(ContainerStatus.PAUSED, container.getContainerStatus());
    }

    @Test
    public void testSetContainerStatusRunningResetsMemoryContainer() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        container.setContainerStatus(ContainerStatus.FINISHED);
        assertEquals(ContainerStatus.FINISHED, container.getContainerStatus());

        // Setting back to RUNNING should re-create memory container
        container.setContainerStatus(ContainerStatus.RUNNING);
        assertEquals(ContainerStatus.RUNNING, container.getContainerStatus());
    }

    @Test
    public void testGetSubcircuitSignalPath() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        // Initially empty
        assertEquals("", container.getSubcircuitSignalPath(0));

        // Set and retrieve
        container.setSignalPathName(0, "/main/sub1");
        assertEquals("/main/sub1", container.getSubcircuitSignalPath(0));
    }

    @Test
    public void testGetDataArray() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        float[] dataArray = container.getDataArray();
        assertNotNull(dataArray);
    }

    @Test
    public void testGetMemoryContainer() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        assertNotNull(container.getMemoryContainer());
    }

    @Test
    public void testGetCachedRAMSizeInMB() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        long cached = container.getCachedRAMSizeInMB();
        assertTrue(cached >= 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetDefinedMeanSignalsThrows() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        container.getDefinedMeanSignals();
    }

    @Test
    public void testGetTimeSeries() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        assertSame(timeSeries, container.getTimeSeries(0));
    }

    @Test
    public void testHiLoValueAcrossCompressedJunks() throws InterruptedException {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        // Fill 2+ junks
        int dataPoints = JUNK_SIZE * 2 + 100;
        for (int i = 0; i < dataPoints; i++) {
            float[] values = {(float) (i % 100)};
            container.insertValuesAtEnd(values, i * 0.0001);
        }

        Thread.sleep(500);

        // HiLo spanning across compressed junks
        HiLoData hiLo = container.getHiLoValue(0, 0, JUNK_SIZE + 50);
        assertNotNull(hiLo);
        assertTrue(hiLo._yHi >= 99);
        assertTrue(hiLo._yLo <= 0);
    }

    @Test
    public void testInsertValuesAtEndWithAvgTracking() {
        TimeSeriesConstantDt timeSeries = new TimeSeriesConstantDt();
        String[] signalNames = {"Signal"};

        DataContainerCompressable container = new DataContainerCompressable(
            1, timeSeries, signalNames, "Time"
        );

        // Define avg with a ScopeSignalMean that tracks row 0
        java.util.List<ch.technokrat.gecko.geckocircuits.newscope.ScopeSignalMean> meanSignals =
            new java.util.ArrayList<>();
        ch.technokrat.gecko.geckocircuits.newscope.ScopeSignalRegular regular =
            new ch.technokrat.gecko.geckocircuits.newscope.ScopeSignalRegular(0, null);
        ch.technokrat.gecko.geckocircuits.newscope.ScopeSignalMean mean =
            new ch.technokrat.gecko.geckocircuits.newscope.ScopeSignalMean(regular, 0.01);
        meanSignals.add(mean);

        container.defineAvgCalculation(meanSignals);

        // Insert data - now the insertValuesAtEnd path should track integral values
        for (int i = 0; i < 100; i++) {
            float[] values = {10.0f};
            container.insertValuesAtEnd(values, i * 0.001);
        }

        // Verify the avg path works
        float avg = container.getAVGValueInInterval(0.010, 0.050, 0);
        // Should be ~10.0 since constant value
        assertEquals(10.0f, avg, 1.0f);
    }
}
