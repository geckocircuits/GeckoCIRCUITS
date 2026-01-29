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

import ch.technokrat.gecko.geckocircuits.newscope.AbstractScopeSignal;
import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class for DataContainerNullData.
 * Tests the null data container which represents a container with no actual data.
 */
public class DataContainerNullDataTest {

    private DataContainerNullData nullContainer;
    private List<AbstractScopeSignal> signalNames;

    private static final float EPSILON = 1e-6f;

    @Before
    public void setUp() {
        signalNames = new ArrayList<>();
        signalNames.add(new AbstractScopeSignal() {
            @Override
            public String getSignalName() {
                return "Signal1";
            }
        });
        signalNames.add(new AbstractScopeSignal() {
            @Override
            public String getSignalName() {
                return "Signal2";
            }
        });

        nullContainer = new DataContainerNullData(signalNames);
    }

    // ==================== Basic Construction & Initialization ====================

    @Test
    public void testConstructorWithoutSignalNames() {
        DataContainerNullData emptyContainer = new DataContainerNullData();
        assertNotNull(emptyContainer);
    }

    @Test
    public void testConstructorWithSignalNames() {
        assertNotNull(nullContainer);
        assertEquals(2, signalNames.size());
    }

    @Test
    public void testSetNoDataName() {
        nullContainer.setNoDataName();
        assertNotNull(nullContainer);
    }

    // ==================== HiLoData (Min/Max) Tests ====================

    @Test
    public void testGetHiLoValueReturnsZeroData() {
        HiLoData hiLo = nullContainer.getHiLoValue(0, 0, 10);
        assertNotNull(hiLo);
        assertEquals(0.0f, hiLo._yLo, EPSILON);
        assertEquals(0.0f, hiLo._yHi, EPSILON);
    }

    @Test
    public void testGetHiLoValueDifferentRows() {
        HiLoData hiLo0 = nullContainer.getHiLoValue(0, 0, 10);
        HiLoData hiLo1 = nullContainer.getHiLoValue(1, 0, 10);
        HiLoData hiLo5 = nullContainer.getHiLoValue(5, 0, 10);

        assertEquals(0.0f, hiLo0._yLo, EPSILON);
        assertEquals(0.0f, hiLo1._yLo, EPSILON);
        assertEquals(0.0f, hiLo5._yLo, EPSILON);
    }

    @Test
    public void testGetHiLoValueDifferentRanges() {
        HiLoData hiLo1 = nullContainer.getHiLoValue(0, 0, 5);
        HiLoData hiLo2 = nullContainer.getHiLoValue(0, 5, 10);
        HiLoData hiLo3 = nullContainer.getHiLoValue(0, 0, 100);

        assertEquals(0.0f, hiLo1._yLo, EPSILON);
        assertEquals(0.0f, hiLo2._yLo, EPSILON);
        assertEquals(0.0f, hiLo3._yLo, EPSILON);
    }

    // ==================== AbsoluteMinMax Tests ====================

    @Test
    public void testGetAbsoluteMinMaxValueNotNull() {
        HiLoData absMinMax = nullContainer.getAbsoluteMinMaxValue(0);
        assertNotNull(absMinMax);
    }

    @Test
    public void testGetAbsoluteMinMaxValueMultipleRows() {
        HiLoData minMax0 = nullContainer.getAbsoluteMinMaxValue(0);
        HiLoData minMax1 = nullContainer.getAbsoluteMinMaxValue(1);
        HiLoData minMax10 = nullContainer.getAbsoluteMinMaxValue(10);

        assertNotNull(minMax0);
        assertNotNull(minMax1);
        assertNotNull(minMax10);
    }

    // ==================== Dimension Tests ====================

    @Test
    public void testGetRowLength() {
        int rowLength = nullContainer.getRowLength();
        assertTrue(rowLength >= 0);
    }

    @Test
    public void testGetRowLengthConsistency() {
        int length1 = nullContainer.getRowLength();
        int length2 = nullContainer.getRowLength();
        assertEquals(length1, length2);
    }

    // ==================== Signal Name Tests ====================

    @Test
    public void testGetSignalName() {
        String signalName = nullContainer.getSignalName(0);
        assertNotNull(signalName);
    }

    @Test
    public void testGetSignalNameMultipleIndices() {
        String name0 = nullContainer.getSignalName(0);
        String name1 = nullContainer.getSignalName(1);

        assertNotNull(name0);
        assertNotNull(name1);
    }

    // ==================== Data Access Tests ====================

    @Test
    public void testGetValue() {
        float value = nullContainer.getValue(0, 0);
        assertEquals(0.0f, value, EPSILON);
    }

    @Test
    public void testGetValueMultipleCoordinates() {
        float v1 = nullContainer.getValue(0, 0);
        float v2 = nullContainer.getValue(1, 5);
        float v3 = nullContainer.getValue(10, 100);

        assertEquals(0.0f, v1, EPSILON);
        assertEquals(0.0f, v2, EPSILON);
        assertEquals(0.0f, v3, EPSILON);
    }

    // ==================== Interface Compliance Tests ====================

    @Test
    public void testImplementsDataContainerValuesSettable() {
        assertTrue(nullContainer instanceof DataContainerValuesSettable);
    }

    @Test
    public void testImplementsAbstractDataContainer() {
        assertTrue(nullContainer instanceof AbstractDataContainer);
    }

    @Test
    public void testImplementsDataContainerIntegralCalculatable() {
        assertTrue(nullContainer instanceof DataContainerIntegralCalculatable);
    }

    // ==================== Consistency Tests ====================

    @Test
    public void testConsistentZeroValues() {
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 10; col++) {
                float value = nullContainer.getValue(row, col);
                assertEquals(0.0f, value, EPSILON);
            }
        }
    }

    @Test
    public void testNoExceptionOnLargeIndices() {
        float value = nullContainer.getValue(1000, 10000);
        assertEquals(0.0f, value, EPSILON);
    }

    @Test
    public void testConsistentHiLoAcrossMultipleRows() {
        for (int row = 0; row < 3; row++) {
            HiLoData hiLo = nullContainer.getHiLoValue(row, 0, 100);
            assertEquals(0.0f, hiLo._yLo, EPSILON);
            assertEquals(0.0f, hiLo._yHi, EPSILON);
        }
    }

    // ==================== State Management Tests ====================

    @Test
    public void testContainerStatus() {
        ContainerStatus status = nullContainer.getContainerStatus();
        assertNotNull(status);
    }

    @Test
    public void testSetContainerStatusAndRetrieve() {
        ContainerStatus initialStatus = nullContainer.getContainerStatus();
        assertNotNull(initialStatus);
        assertEquals(initialStatus, nullContainer.getContainerStatus());
    }

    // ==================== Edge Cases ====================

    @Test
    public void testMultipleNullContainers() {
        DataContainerNullData container1 = new DataContainerNullData();
        DataContainerNullData container2 = new DataContainerNullData();

        assertEquals(container1.getValue(0, 0), container2.getValue(0, 0), EPSILON);
    }

    @Test
    public void testEmptySignalNamesList() {
        List<AbstractScopeSignal> emptyList = new ArrayList<>();
        DataContainerNullData emptyContainer = new DataContainerNullData(emptyList);
        assertNotNull(emptyContainer);
    }

    @Test
    public void testMultipleSetNoDataName() {
        nullContainer.setNoDataName();
        nullContainer.setNoDataName();
        assertNotNull(nullContainer);
    }

    // ==================== NEW: Coverage Gap Tests ====================

    @Test
    public void testGetSignalNameFallbackToDefault() {
        List<AbstractScopeSignal> emptyNameSignals = new ArrayList<>();
        emptyNameSignals.add(new AbstractScopeSignal() {
            @Override
            public String getSignalName() { return ""; }
        });
        DataContainerNullData container = new DataContainerNullData(emptyNameSignals);
        assertEquals("sg.0", container.getSignalName(0));
    }

    @Test
    public void testSetNoDataNameSignalName() {
        nullContainer.setNoDataName();
        assertEquals("no data available", nullContainer.getSignalName(0));
    }

    @Test
    public void testSetNoDataNameRowLength() {
        nullContainer.setNoDataName();
        assertEquals(1, nullContainer.getRowLength());
    }

    @Test
    public void testGetRowLengthWithSignals() {
        assertEquals(2, nullContainer.getRowLength());
    }

    @Test
    public void testGetRowLengthWithoutInit() {
        DataContainerNullData emptyContainer = new DataContainerNullData();
        assertEquals(0, emptyContainer.getRowLength());
    }

    @Test
    public void testGetContainerStatusAlwaysNotInitialized() {
        assertEquals(ContainerStatus.NOT_INITIALIZED, nullContainer.getContainerStatus());
    }

    @Test
    public void testSetContainerStatusDeletedDoesNotThrow() {
        nullContainer.setContainerStatus(ContainerStatus.DELETED);
    }

    @Test
    public void testGetTimeValueAlwaysZero() {
        assertEquals(0.0, nullContainer.getTimeValue(0, 0), 1e-10);
        assertEquals(0.0, nullContainer.getTimeValue(100, 5), 1e-10);
    }

    @Test
    public void testGetMaximumTimeIndexAlwaysZero() {
        assertEquals(0, nullContainer.getMaximumTimeIndex(0));
        assertEquals(0, nullContainer.getMaximumTimeIndex(5));
    }

    @Test
    public void testFindTimeIndexAlwaysZero() {
        assertEquals(0, nullContainer.findTimeIndex(0.5, 0));
        assertEquals(0, nullContainer.findTimeIndex(100.0, 3));
    }

    @Test
    public void testIsInvalidNumbersAlwaysFalse() {
        assertFalse(nullContainer.isInvalidNumbers(0));
        assertFalse(nullContainer.isInvalidNumbers(1));
    }

    @Test
    public void testGetDataValueInIntervalReturnsNull() {
        assertNull(nullContainer.getDataValueInInterval(0.0, 1.0, 0));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testInsertValuesAtEndThrows() {
        nullContainer.insertValuesAtEnd(new float[]{1.0f}, 0.0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetTimeSeriesThrows() {
        nullContainer.getTimeSeries(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetAVGValueInIntervalThrows() {
        nullContainer.getAVGValueInInterval(0.0, 1.0, 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetDataArrayThrows() {
        nullContainer.getDataArray();
    }

    @Test
    public void testGetAbsoluteMinMaxValues() {
        HiLoData minMax = nullContainer.getAbsoluteMinMaxValue(0);
        assertEquals(0.0f, minMax._yLo, EPSILON);
        assertEquals(1.0f, minMax._yHi, EPSILON);
    }

    @Test
    public void testGetUsedRAMSizeInMB() {
        assertEquals(0, nullContainer.getUsedRAMSizeInMB());
    }

    @Test
    public void testGetCachedRAMSizeInMB() {
        assertEquals(0, nullContainer.getCachedRAMSizeInMB());
    }

    @Test
    public void testGetXDataName() {
        assertEquals("t", nullContainer.getXDataName());
    }

    @Test
    public void testDefineAvgCalculationNoOp() {
        nullContainer.defineAvgCalculation(new ArrayList<>());
    }

    @Test
    public void testSetAndGetDefinedMeanSignals() {
        assertNull(nullContainer.getDefinedMeanSignals());
    }

    @Test
    public void testGetSignalNameWithValidName() {
        assertEquals("Signal1", nullContainer.getSignalName(0));
        assertEquals("Signal2", nullContainer.getSignalName(1));
    }

}
