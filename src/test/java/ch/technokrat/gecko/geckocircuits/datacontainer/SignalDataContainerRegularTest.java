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

import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Comprehensive test suite for SignalDataContainerRegular.
 * Tests signal naming, value access, and wrapper functionality.
 */
public class SignalDataContainerRegularTest {

    private static final float EPSILON = 1e-6f;

    private DataContainerSimple dataContainer;
    private SignalDataContainerRegular signalContainer;

    @Before
    public void setUp() {
        dataContainer = new DataContainerSimple(5, 100);
        // Create a signal container wrapping row 1 of the data container
        signalContainer = new SignalDataContainerRegular(dataContainer, 1);
    }

    // ==================== Signal Name Tests ====================

    @Test
    public void testGetSignalNameDefault() {
        // Should return the name from underlying data container
        String name = signalContainer.getSignalName();
        assertNotNull(name);
        assertFalse(name.isEmpty());
    }

    @Test
    public void testSetAndGetSignalName() {
        String customName = "CustomVoltage";
        signalContainer.setSignalName(customName);
        assertEquals(customName, signalContainer.getSignalName());
    }

    @Test
    public void testSetSignalNameOverridesDefault() {
        String originalName = signalContainer.getSignalName();
        String newName = "OverriddenName";
        signalContainer.setSignalName(newName);
        
        assertNotEquals(originalName, signalContainer.getSignalName());
        assertEquals(newName, signalContainer.getSignalName());
    }

    @Test
    public void testMultipleSignalContainers() {
        SignalDataContainerRegular signal1 = new SignalDataContainerRegular(dataContainer, 1);
        SignalDataContainerRegular signal2 = new SignalDataContainerRegular(dataContainer, 2);

        signal1.setSignalName("Signal1");
        signal2.setSignalName("Signal2");

        assertEquals("Signal1", signal1.getSignalName());
        assertEquals("Signal2", signal2.getSignalName());
    }

    // ==================== Container Data Access Tests ====================

    @Test
    public void testGetValueViaContainer() {
        dataContainer.setValue(123.45f, 1, 0);
        float value = dataContainer.getValue(1, 0);
        assertEquals(123.45f, value, EPSILON);
    }

    @Test
    public void testGetValueMultiple() {
        float[] testValues = {1.0f, 2.5f, -3.7f, 0.0f, 100.5f};
        for (int i = 0; i < testValues.length; i++) {
            dataContainer.setValue(testValues[i], 1, i);
        }
        
        for (int i = 0; i < testValues.length; i++) {
            assertEquals(testValues[i], dataContainer.getValue(1, i), EPSILON);
        }
    }

    @Test
    public void testGetValueZero() {
        dataContainer.setValue(0.0f, 1, 0);
        assertEquals(0.0f, dataContainer.getValue(1, 0), EPSILON);
    }

    @Test
    public void testGetValueNegative() {
        dataContainer.setValue(-99.99f, 1, 5);
        assertEquals(-99.99f, dataContainer.getValue(1, 5), EPSILON);
    }

    @Test
    public void testGetValueLarge() {
        float largeValue = 1e8f;
        dataContainer.setValue(largeValue, 1, 10);
        assertEquals(largeValue, dataContainer.getValue(1, 10), largeValue * EPSILON);
    }

    // ==================== Min/Max Tests ====================

    @Test
    public void testGetHiLoValue() {
        dataContainer.setValue(1.0f, 1, 0);
        dataContainer.setValue(5.0f, 1, 1);
        dataContainer.setValue(3.0f, 1, 2);
        dataContainer.setValue(8.0f, 1, 3);
        dataContainer.setValue(2.0f, 1, 4);

        HiLoData hiLo = dataContainer.getHiLoValue(1, 0, 5);
        assertNotNull(hiLo);
        assertEquals(1.0f, hiLo._yLo, EPSILON);
        assertEquals(8.0f, hiLo._yHi, EPSILON);
    }

    @Test
    public void testGetHiLoValueNegatives() {
        dataContainer.setValue(-10.0f, 1, 0);
        dataContainer.setValue(5.0f, 1, 1);
        dataContainer.setValue(-3.0f, 1, 2);
        dataContainer.setValue(8.0f, 1, 3);
        dataContainer.setValue(-5.0f, 1, 4);

        HiLoData hiLo = dataContainer.getHiLoValue(1, 0, 5);
        assertNotNull(hiLo);
        assertEquals(-10.0f, hiLo._yLo, EPSILON);
        assertEquals(8.0f, hiLo._yHi, EPSILON);
    }

    @Test
    public void testGetAbsoluteMinMaxValue() {
        dataContainer.setValue(-10.0f, 1, 0);
        dataContainer.setValue(5.0f, 1, 1);
        dataContainer.setValue(20.0f, 1, 2);
        dataContainer.setValue(-5.0f, 1, 3);
        dataContainer.setValue(0.0f, 1, 4);

        HiLoData absMinMax = dataContainer.getAbsoluteMinMaxValue(1);
        assertNotNull(absMinMax);
        assertEquals(-10.0f, absMinMax._yLo, EPSILON);
        assertEquals(20.0f, absMinMax._yHi, EPSILON);
    }

    @Test
    public void testGetAbsoluteMinMaxValueAllPositive() {
        dataContainer.setValue(1.0f, 1, 0);
        dataContainer.setValue(5.0f, 1, 1);
        dataContainer.setValue(20.0f, 1, 2);
        dataContainer.setValue(3.0f, 1, 3);
        dataContainer.setValue(10.0f, 1, 4);

        HiLoData absMinMax = dataContainer.getAbsoluteMinMaxValue(1);
        assertNotNull(absMinMax);
        assertEquals(1.0f, absMinMax._yLo, EPSILON);
        assertEquals(20.0f, absMinMax._yHi, EPSILON);
    }

    @Test
    public void testGetAbsoluteMinMaxValueAllNegative() {
        dataContainer.setValue(-20.0f, 1, 0);
        dataContainer.setValue(-5.0f, 1, 1);
        dataContainer.setValue(-1.0f, 1, 2);
        dataContainer.setValue(-15.0f, 1, 3);
        dataContainer.setValue(-8.0f, 1, 4);

        HiLoData absMinMax = dataContainer.getAbsoluteMinMaxValue(1);
        assertNotNull(absMinMax);
        assertEquals(-20.0f, absMinMax._yLo, EPSILON);
        assertEquals(-1.0f, absMinMax._yHi, EPSILON);
    }

    @Test
    public void testGetDataContainer() {
        AbstractDataContainer retrieved = signalContainer.getDataContainer();
        assertSame(dataContainer, retrieved);
    }

    @Test
    public void testGetDataContainerIndex() {
        assertEquals(1, signalContainer.getContainerSignalIndex());
    }

    @Test
    public void testDifferentRowIndices() {
        SignalDataContainerRegular signal0 = new SignalDataContainerRegular(dataContainer, 0);
        SignalDataContainerRegular signal2 = new SignalDataContainerRegular(dataContainer, 2);
        SignalDataContainerRegular signal4 = new SignalDataContainerRegular(dataContainer, 4);

        assertEquals(0, signal0.getContainerSignalIndex());
        assertEquals(2, signal2.getContainerSignalIndex());
        assertEquals(4, signal4.getContainerSignalIndex());
    }

    // ==================== Wrapper Functionality Tests ====================

    @Test
    public void testModifyUnderlying() {
        // Change value in underlying container
        dataContainer.setValue(99.99f, 1, 5);
        
        // Should be visible through signal container's parent
        assertEquals(99.99f, signalContainer.getDataContainer().getValue(1, 5), EPSILON);
    }

    @Test
    public void testMultipleWrappers() {
        // Create two wrappers for same underlying data
        SignalDataContainerRegular wrapper1 = new SignalDataContainerRegular(dataContainer, 1);
        SignalDataContainerRegular wrapper2 = new SignalDataContainerRegular(dataContainer, 1);

        dataContainer.setValue(42.0f, 1, 0);

        // Both wrappers should see the same value through parent
        assertEquals(42.0f, wrapper1.getDataContainer().getValue(1, 0), EPSILON);
        assertEquals(42.0f, wrapper2.getDataContainer().getValue(1, 0), EPSILON);
    }

    @Test
    public void testIndependentRows() {
        SignalDataContainerRegular row1Signal = new SignalDataContainerRegular(dataContainer, 1);
        SignalDataContainerRegular row2Signal = new SignalDataContainerRegular(dataContainer, 2);

        dataContainer.setValue(111.0f, 1, 0);
        dataContainer.setValue(222.0f, 2, 0);

        assertEquals(111.0f, row1Signal.getDataContainer().getValue(1, 0), EPSILON);
        assertEquals(222.0f, row2Signal.getDataContainer().getValue(2, 0), EPSILON);
    }

    // ==================== Edge Case Tests ====================

    @Test
    public void testHiLoValueSinglePoint() {
        dataContainer.setValue(7.5f, 1, 0);
        HiLoData hiLo = dataContainer.getHiLoValue(1, 0, 1);
        assertNotNull(hiLo);
        assertEquals(7.5f, hiLo._yLo, EPSILON);
        assertEquals(7.5f, hiLo._yHi, EPSILON);
    }

    @Test
    public void testHiLoValueAllSame() {
        float sameValue = 5.5f;
        for (int i = 0; i < 10; i++) {
            dataContainer.setValue(sameValue, 1, i);
        }

        HiLoData hiLo = dataContainer.getHiLoValue(1, 0, 10);
        assertNotNull(hiLo);
        assertEquals(sameValue, hiLo._yLo, EPSILON);
        assertEquals(sameValue, hiLo._yHi, EPSILON);
    }

    @Test
    public void testSignalNameEmptyString() {
        signalContainer.setSignalName("");
        assertEquals("", signalContainer.getSignalName());
    }

    @Test
    public void testSignalNameSpecialCharacters() {
        String specialName = "Signal@123#$%";
        signalContainer.setSignalName(specialName);
        assertEquals(specialName, signalContainer.getSignalName());
    }

    @Test
    public void testSignalNameVeryLong() {
        String longName = "A".repeat(1000);
        signalContainer.setSignalName(longName);
        assertEquals(longName, signalContainer.getSignalName());
    }

    // ==================== Integration Tests ====================

    @Test
    public void testComplexScenario() {
        // Set up a signal with custom name
        signalContainer.setSignalName("InputVoltage");

        // Fill with data
        float[] waveform = {0, 5, 10, 5, 0, -5, -10, -5};
        for (int i = 0; i < waveform.length; i++) {
            dataContainer.setValue(waveform[i], 1, i);
        }

        // Verify name
        assertEquals("InputVoltage", signalContainer.getSignalName());

        // Verify all values via dataContainer
        for (int i = 0; i < waveform.length; i++) {
            assertEquals(waveform[i], dataContainer.getValue(1, i), EPSILON);
        }

        // Verify min/max
        HiLoData hiLo = dataContainer.getHiLoValue(1, 0, waveform.length);
        assertEquals(-10.0f, hiLo._yLo, EPSILON);
        assertEquals(10.0f, hiLo._yHi, EPSILON);
    }

    @Test
    public void testIsAbstractDataContainerSignal() {
        assertTrue("Should be instance of AbstractDataContainerSignal",
                   signalContainer instanceof AbstractDataContainerSignal);
    }
}
