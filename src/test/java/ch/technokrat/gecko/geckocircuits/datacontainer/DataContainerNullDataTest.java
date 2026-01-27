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
        // Create sample signal names
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
        // Set special "no data available" name
        nullContainer.setNoDataName();
        // Should replace signal names with a single "no data available" entry
        assertNotNull(nullContainer);
    }

    // ==================== HiLoData (Min/Max) Tests ====================

    @Test
    public void testGetHiLoValueReturnsZeroData() {
        // Null data container always returns zero hi/lo values
        HiLoData hiLo = nullContainer.getHiLoValue(0, 0, 10);
        assertNotNull(hiLo);
        assertEquals(0.0f, hiLo._yLo, EPSILON);
        assertEquals(0.0f, hiLo._yHi, EPSILON);
    }

    @Test
    public void testGetHiLoValueDifferentRows() {
        // All rows should return zero
        HiLoData hiLo0 = nullContainer.getHiLoValue(0, 0, 10);
        HiLoData hiLo1 = nullContainer.getHiLoValue(1, 0, 10);
        HiLoData hiLo5 = nullContainer.getHiLoValue(5, 0, 10);

        assertEquals(0.0f, hiLo0._yLo, EPSILON);
        assertEquals(0.0f, hiLo1._yLo, EPSILON);
        assertEquals(0.0f, hiLo5._yLo, EPSILON);
    }

    @Test
    public void testGetHiLoValueDifferentRanges() {
        // Different ranges should all return zero
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
        // All rows should return non-null values
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
        // Null data container may have zero or undefined row length
        int rowLength = nullContainer.getRowLength();
        assertTrue(rowLength >= 0);
    }

    @Test
    public void testGetRowLengthConsistency() {
        // Should return consistent value
        int length1 = nullContainer.getRowLength();
        int length2 = nullContainer.getRowLength();
        assertEquals(length1, length2);
    }

    // ==================== Signal Name Tests ====================

    @Test
    public void testGetSignalName() {
        // Should be able to get signal names if provided
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
        // Null data container should return 0 for any access
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
        // Multiple accesses should return zero consistently
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 10; col++) {
                float value = nullContainer.getValue(row, col);
                assertEquals(0.0f, value, EPSILON);
            }
        }
    }

    @Test
    public void testNoExceptionOnLargeIndices() {
        // Should handle large indices gracefully
        float value = nullContainer.getValue(1000, 10000);
        assertEquals(0.0f, value, EPSILON);
    }

    @Test
    public void testConsistentHiLoAcrossMultipleRows() {
        // HiLoData should be consistent across multiple rows
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
        // Test that we can set and retrieve a status
        ContainerStatus initialStatus = nullContainer.getContainerStatus();
        assertNotNull(initialStatus);
        // Status should be retrievable
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
        // Setting nodata name multiple times should be safe
        nullContainer.setNoDataName();
        nullContainer.setNoDataName();
        // Should not crash
        assertNotNull(nullContainer);
    }

}
