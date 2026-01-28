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
}
