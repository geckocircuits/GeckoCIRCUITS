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
        
        // After insertion, should have values
        float realValue = fourier.getValue(0, 0);
        assertNotNull(realValue);
    }

    @Test
    public void testGetValueAfterInsert() {
        float[] magnitude = {5.0f, 10.0f};
        float[] phase = {0.0f, 1.57f}; // ~pi/2
        
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
        assertTrue(maxIndex >= 2); // At least 3 values inserted
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
        
        // Verify first harmonic
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
        
        // Should not throw
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
}
