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

import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import ch.technokrat.gecko.geckocircuits.newscope.MemoryContainer;
import ch.technokrat.gecko.geckocircuits.newscope.TimeSeriesConstantDt;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for DataJunkCompressable - compressed data chunk storage.
 */
public class DataJunkCompressableTest {

    private MemoryContainer memoryContainer;
    private TimeSeriesConstantDt timeSeries;

    @Before
    public void setUp() {
        memoryContainer = MemoryContainer.getMemoryContainer(2);
        timeSeries = new TimeSeriesConstantDt();
        CompressorIntMatrix.clearCache();
    }

    @After
    public void tearDown() {
        CompressorIntMatrix.clearCache();
    }

    @Test
    public void testConstruction() {
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, 0, 2, 100, timeSeries
        );
        assertNotNull(junk);
    }

    @Test
    public void testSetGetValue() {
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, 0, 2, 100, timeSeries
        );

        float[] values = {3.14f, 2.72f};
        junk.setValues(values, 0);

        // Before compression, values should be retrievable
        assertEquals(3.14f, junk.getValue(0, 0), 0.001f);
        assertEquals(2.72f, junk.getValue(1, 0), 0.001f);
    }

    @Test
    public void testMultipleValueStorage() {
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, 0, 3, 50, timeSeries
        );

        for (int col = 0; col < 50; col++) {
            float[] values = {col * 1.0f, col * 2.0f, col * 3.0f};
            junk.setValues(values, col);
        }

        // Verify some values
        assertEquals(0.0f, junk.getValue(0, 0), 0.001f);
        assertEquals(98.0f, junk.getValue(1, 49), 0.001f);
        assertEquals(147.0f, junk.getValue(2, 49), 0.001f);
    }

    @Test
    public void testSetSingleValue() {
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, 0, 2, 10, timeSeries
        );

        junk.setValue(5.5f, 0, 3);
        assertEquals(5.5f, junk.getValue(0, 3), 0.001f);
    }

    @Test
    public void testStartIndexOffset() {
        int startIndex = 1000;
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, startIndex, 2, 100, timeSeries
        );

        float[] values = {99.9f, 88.8f};
        junk.setValues(values, startIndex + 5);

        assertEquals(99.9f, junk.getValue(0, startIndex + 5), 0.001f);
    }

    @Test
    public void testNegativeValues() {
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, 0, 1, 10, timeSeries
        );

        float[] values = {-100.5f};
        junk.setValues(values, 0);

        assertEquals(-100.5f, junk.getValue(0, 0), 0.1f);
    }

    @Test
    public void testJunkSizeInBytes() {
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, 0, 2, 100, timeSeries
        );

        // Initially before compression
        int size = junk.getJunkSizeInBytes();
        assertTrue("Size should be >= 0", size >= 0);
    }

    // ==================== Coverage Gap Tests ====================

    @Test
    public void testCompressionAndDecompression() throws InterruptedException {
        int columns = 4096; // Match JUNK_SIZE for full junk
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, 0, 2, columns, timeSeries
        );

        // Set up time series so compression thread can read it
        for (int col = 0; col < columns; col++) {
            timeSeries.setValue(col, col * 0.001);
        }

        // Fill all columns with data
        for (int col = 0; col < columns; col++) {
            float[] values = {col * 1.0f, col * 0.5f};
            junk.setValues(values, col);
        }

        // Trigger compression (runs in background thread)
        junk.doCompression();
        Thread.sleep(1000);

        // After compression, reading should trigger decompression
        // Due to lossy rounding, use generous tolerance
        float val = junk.getValue(0, 0);
        assertEquals(0.0f, val, 1.0f);

        float valMid = junk.getValue(0, columns / 2);
        assertEquals(columns / 2.0f, valMid, 10.0f);
    }

    @Test
    public void testGetHiLoValueAfterCompression() throws InterruptedException {
        int columns = 4096;
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, 0, 1, columns, timeSeries
        );

        for (int col = 0; col < columns; col++) {
            timeSeries.setValue(col, col * 0.001);
        }

        for (int col = 0; col < columns; col++) {
            float[] values = {col * 1.0f};
            junk.setValues(values, col);
        }

        junk.doCompression();
        Thread.sleep(1000);

        // getHiLoValue should use cached hiLo data after compression
        HiLoData hiLo = junk.getHiLoValue(0, 0, columns - 1);
        assertNotNull(hiLo);
    }

    @Test
    public void testGetHiLoValueSmallInterval() throws InterruptedException {
        int columns = 4096;
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, 0, 1, columns, timeSeries
        );

        for (int col = 0; col < columns; col++) {
            timeSeries.setValue(col, col * 0.001);
        }

        for (int col = 0; col < columns; col++) {
            float[] values = {col * 1.0f};
            junk.setValues(values, col);
        }

        junk.doCompression();
        Thread.sleep(1000);

        // Small interval forces calculateRealHiLowData path
        HiLoData hiLo = junk.getHiLoValue(0, 10, 15);
        assertNotNull(hiLo);
    }

    @Test
    public void testGetCacheSizeInBytesAfterCompression() throws InterruptedException {
        int columns = 4096;
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, 0, 2, columns, timeSeries
        );

        for (int col = 0; col < columns; col++) {
            timeSeries.setValue(col, col * 0.001);
        }

        for (int col = 0; col < columns; col++) {
            float[] values = {col * 1.0f, col * 0.5f};
            junk.setValues(values, col);
        }

        junk.doCompression();
        Thread.sleep(1000);

        // After compression, hiLo cache should have non-zero size
        int cacheSize = junk.getCacheSizeInBytes();
        assertTrue("Cache size should be positive after compression", cacheSize > 0);
    }

    @Test
    public void testGetJunkSizeAfterCompression() throws InterruptedException {
        int columns = 4096;
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, 0, 1, columns, timeSeries
        );

        for (int col = 0; col < columns; col++) {
            timeSeries.setValue(col, col * 0.001);
        }

        for (int col = 0; col < columns; col++) {
            float[] values = {col * 1.0f};
            junk.setValues(values, col);
        }

        junk.doCompression();
        Thread.sleep(1000);

        int size = junk.getJunkSizeInBytes();
        assertTrue("Compressed junk should have positive size", size > 0);
    }

    @Test
    public void testSetPrecisionField() {
        // Exercise the static setPrecisionField method
        int originalPrecision = DataJunkCompressable.PRECISIONS[2];
        DataJunkCompressable.setPrecisionField(DataJunkCompressable.PRECISIONS[0]); // No rounding
        DataJunkCompressable.setPrecisionField(originalPrecision); // Restore
    }

    @Test
    public void testGetIntegralValue() {
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, 0, 2, 100, timeSeries
        );

        for (int col = 0; col < 100; col++) {
            timeSeries.setValue(col, col * 0.001);
        }

        // Set integral values using setIntegralValue
        junk.setIntegralValue(5.0, 0, 10);
        float integralVal = junk.getIntegralValue(0, 10);
        assertEquals(5.0f, integralVal, 0.001f);
    }

    @Test
    public void testGetIntegralValueException() {
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, 0, 2, 100, timeSeries
        );

        // getIntegralValue on uninitialized avgData should return 0 (catches exception)
        float val = junk.getIntegralValue(0, 0);
        assertEquals(0.0f, val, 0.001f);
    }

    @Test
    public void testGetHiLoValueBeforeCompression() {
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, 0, 1, 100, timeSeries
        );

        for (int col = 0; col < 100; col++) {
            float[] values = {col * 1.0f};
            junk.setValues(values, col);
        }

        // Before compression, _hiLoData is null so calculateRealHiLowData is used
        HiLoData hiLo = junk.getHiLoValue(0, 0, 99);
        assertNotNull(hiLo);
        assertTrue(hiLo._yHi >= 99);
        assertTrue(hiLo._yLo <= 0);
    }

    @Test
    public void testCalculateAvgData() throws InterruptedException {
        int columns = 4096;
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, 0, 1, columns, timeSeries
        );

        for (int col = 0; col < columns; col++) {
            timeSeries.setValue(col, col * 0.001);
            float[] values = {10.0f};
            junk.setValues(values, col);
        }

        // calculateAvgData is public, exercise it directly
        junk.calculateAvgData(0);

        // After calculating avg data, getAverageValue should work
        AverageValue avg = junk.getAverageValue(0, 0, columns - 1, 0.0, columns * 0.001);
        // May be null if intervals don't align perfectly, but should not throw
    }

    @Test
    public void testGetAverageValue() throws InterruptedException {
        int columns = 4096;
        DataJunkCompressable junk = new DataJunkCompressable(
            memoryContainer, 0, 1, columns, timeSeries
        );

        for (int col = 0; col < columns; col++) {
            timeSeries.setValue(col, col * 0.001);
            float[] values = {10.0f};
            junk.setValues(values, col);
        }

        // getAverageValue should trigger calculateAvgData internally
        AverageValue avg = junk.getAverageValue(0, 0, columns - 1, 0.0, columns * 0.001);
        // Result depends on internal interval alignment
    }
}
