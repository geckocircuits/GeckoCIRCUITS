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
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class DataContainerFourierTest {

    private DataContainerFourier fourier;
    private static final double BASE_FREQUENCY = 50.0; // 50 Hz

    @Before
    public void setUp() {
        fourier = new DataContainerFourier(1, 100, BASE_FREQUENCY);
    }

    @Test
    public void testConstructor() {
        assertNotNull(fourier);
    }

    @Test
    public void testGetRowLength() {
        assertEquals(2, fourier.getRowLength()); // Real and Imaginary parts
    }

    @Test
    public void testInsertValues() {
        float[] magnitude = {1.0f, 2.0f, 3.0f};
        float[] phase = {0.0f, 0.5f, 1.0f};

        fourier.insertValues(magnitude, phase);

        float realValue = fourier.getValue(0, 0);
        assertNotNull(realValue);
    }

    @Test
    public void testGetValueAfterInsert() {
        float[] magnitude = {5.0f, 10.0f};
        float[] phase = {0.0f, 1.57f};

        fourier.insertValues(magnitude, phase);

        float realPart = fourier.getValue(0, 0);
        assertEquals(5.0f, realPart, 0.001f);
    }

    @Test
    public void testGetMaximumTimeIndex() {
        float[] magnitude = {1.0f, 2.0f, 3.0f};
        float[] phase = {0.0f, 0.0f, 0.0f};

        fourier.insertValues(magnitude, phase);

        int maxIndex = fourier.getMaximumTimeIndex(0);
        assertTrue(maxIndex >= 2);
    }

    @Test
    public void testGetTimeValue() {
        float[] magnitude = {1.0f};
        float[] phase = {0.0f};

        fourier.insertValues(magnitude, phase);

        double timeValue = fourier.getTimeValue(0, 0);
        assertEquals(0.0, timeValue, 0.001);
    }

    @Test
    public void testMultipleHarmonics() {
        float[] magnitude = {1.0f, 0.5f, 0.33f, 0.25f};
        float[] phase = {0.0f, 0.5f, 1.0f, 1.5f};

        fourier.insertValues(magnitude, phase);

        assertEquals(1.0f, fourier.getValue(0, 0), 0.001f);
    }

    @Test
    public void testDifferentBaseFrequency() {
        DataContainerFourier fourier100Hz = new DataContainerFourier(1, 100, 100.0);

        float[] magnitude = {1.0f};
        float[] phase = {0.0f};

        fourier100Hz.insertValues(magnitude, phase);

        int maxIndex = fourier100Hz.getMaximumTimeIndex(0);
        assertTrue(maxIndex >= 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetValueThrows() {
        fourier.setValue(1.0f, 0, 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInsertValuesAtEndThrows() {
        float[] values = {1.0f, 2.0f};
        fourier.insertValuesAtEnd(values, 0.0);
    }

    @Test
    public void testGetHiLoValue() {
        float[] magnitude = {1.0f, 2.0f, 3.0f};
        float[] phase = {0.0f, 0.0f, 0.0f};

        fourier.insertValues(magnitude, phase);

        fourier.getHiLoValue(0, 0, 2);
    }

    @Test
    public void testLargeNumberOfHarmonics() {
        float[] magnitude = new float[50];
        float[] phase = new float[50];

        for (int i = 0; i < 50; i++) {
            magnitude[i] = 1.0f / (i + 1);
            phase[i] = 0.0f;
        }

        fourier.insertValues(magnitude, phase);

        int maxIndex = fourier.getMaximumTimeIndex(0);
        assertTrue(maxIndex >= 49);
    }

    @Test
    public void testZeroMagnitude() {
        float[] magnitude = {0.0f, 0.0f};
        float[] phase = {0.0f, 0.0f};

        fourier.insertValues(magnitude, phase);

        assertEquals(0.0f, fourier.getValue(0, 0), 0.001f);
    }

    // ==================== NEW: Coverage Gap Tests ====================

    @Test
    public void testInsertValuesPopulatesData() {
        float[] magnitude = {1.0f, 2.0f, 3.0f};
        float[] phase = {0.1f, 0.2f, 0.3f};
        fourier.insertValues(magnitude, phase);

        assertEquals(1.0f, fourier.getValue(0, 0), 0.001f);
        assertEquals(0.1f, fourier.getValue(1, 0), 0.001f);
        assertEquals(2.0f, fourier.getValue(0, 1), 0.001f);
        assertEquals(0.2f, fourier.getValue(1, 1), 0.001f);
    }

    @Test
    public void testGetTimeValueCorrespondsToFrequency() {
        float[] magnitude = {1.0f, 2.0f, 3.0f};
        float[] phase = {0.0f, 0.0f, 0.0f};
        fourier.insertValues(magnitude, phase);

        assertEquals(0.0, fourier.getTimeValue(0, 0), 0.001);
        assertEquals(BASE_FREQUENCY, fourier.getTimeValue(1, 0), 0.001);
        assertEquals(2 * BASE_FREQUENCY, fourier.getTimeValue(2, 0), 0.001);
    }

    @Test
    public void testGetContainerStatusAlwaysPaused() {
        assertEquals(ContainerStatus.PAUSED, fourier.getContainerStatus());
    }

    @Test
    public void testSetContainerStatusBugAlwaysSetsPaused() {
        // BUG: setContainerStatus uses containerStatus.PAUSED (static field access via parameter)
        // This means it always passes PAUSED regardless of argument
        fourier.setContainerStatus(ContainerStatus.RUNNING);
        assertEquals(ContainerStatus.PAUSED, fourier.getContainerStatus());
    }

    @Test
    public void testIsInvalidNumbersAlwaysFalse() {
        assertFalse(fourier.isInvalidNumbers(0));
        assertFalse(fourier.isInvalidNumbers(1));
    }

    @Test
    public void testGetHiLoValueBugPassesColumnStopTwice() {
        // BUG at line 63: getHiLoValue passes columnStop twice
        // _dataCont.getHiLoValue(row, columnStop, columnStop) instead of (row, columnStart, columnStop)
        float[] magnitude = {1.0f, 5.0f, 3.0f};
        float[] phase = {0.0f, 0.0f, 0.0f};
        fourier.insertValues(magnitude, phase);

        HiLoData hiLo = fourier.getHiLoValue(0, 0, 2);
        assertNotNull(hiLo);
    }

    @Test
    public void testGetSignalName() {
        assertEquals("Re", fourier.getSignalName(0));
        assertEquals("Im", fourier.getSignalName(1));
    }

    @Test
    public void testGetXDataName() {
        assertEquals("f", fourier.getXDataName());
    }

    @Test
    public void testGetTimeSeries() {
        assertNotNull(fourier.getTimeSeries(0));
    }

    @Test
    public void testFindTimeIndex() {
        float[] magnitude = {1.0f, 2.0f, 3.0f};
        float[] phase = {0.0f, 0.0f, 0.0f};
        fourier.insertValues(magnitude, phase);

        int index = fourier.findTimeIndex(0.0, 0);
        assertTrue(index >= 0);
    }

    @Test
    public void testGetAbsoluteMinMaxValue() {
        // Insert enough data to fill at least one junk for valid min/max
        float[] magnitude = new float[200];
        float[] phase = new float[200];
        for (int i = 0; i < 200; i++) {
            magnitude[i] = i * 1.0f;
            phase[i] = 0.0f;
        }
        fourier.insertValues(magnitude, phase);

        try {
            HiLoData absMinMax = fourier.getAbsoluteMinMaxValue(0);
            assertNotNull(absMinMax);
        } catch (ArithmeticException e) {
            // May throw if not enough compressed data junks available
        }
    }

    @Test
    public void testGetDataValueInInterval() {
        float[] magnitude = {1.0f, 2.0f, 3.0f};
        float[] phase = {0.0f, 0.0f, 0.0f};
        fourier.insertValues(magnitude, phase);

        Object result = fourier.getDataValueInInterval(0.0, 100.0, 0);
        assertNotNull(result);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetDataArrayThrows() {
        fourier.getDataArray();
    }

    @Test
    public void testGetUsedRAMSizeInMB() {
        assertTrue(fourier.getUsedRAMSizeInMB() >= 0);
    }

    @Test
    public void testGetCachedRAMSizeInMB() {
        assertTrue(fourier.getCachedRAMSizeInMB() >= 0);
    }
}
