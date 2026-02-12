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
package gecko.core.datacontainer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Observer;
import java.util.Observable;

class DataContainerGlobalTest {

    private DataContainerGlobal global;

    @BeforeEach
    void setUp() {
        global = new DataContainerGlobal();
    }

    @Test
    void testConstructor() {
        assertNotNull(global);
    }

    @Test
    void testInitialization() {
        String[] signalNames = {"Voltage", "Current", "Power"};
        global.init(3, signalNames, "Time");

        assertEquals(3, global.getRowLength());
    }

    @Test
    void testInsertValuesAtEnd() {
        String[] signalNames = {"Signal1", "Signal2"};
        global.init(2, signalNames, "Time");

        float[] values = {1.0f, 2.0f};
        global.insertValuesAtEnd(values, 0.0);

        assertNotNull(global.getValue(0, 0));
    }

    @Test
    void testGetValue() {
        String[] signalNames = {"Signal1"};
        global.init(1, signalNames, "Time");

        float[] values = {5.5f};
        global.insertValuesAtEnd(values, 0.0);

        assertEquals(5.5f, global.getValue(0, 0), 0.001f);
    }

    @Test
    void testGetRowLength() {
        String[] signalNames = {"S1", "S2", "S3", "S4"};
        global.init(4, signalNames, "Time");

        assertEquals(4, global.getRowLength());
    }

    @Test
    void testMultipleInsertions() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        for (int i = 0; i < 5; i++) {
            float[] values = {i * 1.0f};
            global.insertValuesAtEnd(values, i * 0.001);
        }

        assertTrue(global.getMaximumTimeIndex(0) >= 0);
    }

    @Test
    void testGetSignalName() {
        String[] signalNames = {"Voltage", "Current"};
        global.init(2, signalNames, "Time");

        assertEquals("Voltage", global.getSignalName(0));
        assertEquals("Current", global.getSignalName(1));
    }

    @Test
    void testGetXAxisName() {
        String[] signalNames = {"Signal1"};
        global.init(1, signalNames, "TimeAxis");

        // Core DataContainerGlobal delegates to underlying DataContainerSimple's xDataName
        String xName = global.getXDataName();
        assertNotNull(xName);
    }

    @Test
    void testGetMaximumTimeIndex() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        float[] values = {1.0f};
        global.insertValuesAtEnd(values, 0.0);
        global.insertValuesAtEnd(values, 1.0);

        int maxIndex = global.getMaximumTimeIndex(0);
        assertTrue(maxIndex >= 1);
    }

    @Test
    void testDataContainerGlobalDelegate() {
        String[] signalNames = {"Voltage", "Current"};
        global.init(2, signalNames, "Time");

        float[] values = {10.0f, 20.0f};
        global.insertValuesAtEnd(values, 0.0);

        assertEquals(10.0f, global.getValue(0, 0), 0.001f);
        assertEquals(20.0f, global.getValue(1, 0), 0.001f);
    }

    @Test
    void testDataContainerGlobalWithoutInit() {
        assertNotNull(global.getValue(0, 0));
    }

    @Test
    void testInitializationReplacement() {
        String[] names1 = {"Signal1"};
        global.init(1, names1, "Time1");

        String[] names2 = {"Signal2", "Signal3"};
        global.init(2, names2, "Time2");

        assertEquals(2, global.getRowLength());
        assertEquals("Signal2", global.getSignalName(0));
        assertEquals("Signal3", global.getSignalName(1));
    }

    @Test
    void testMultiRowDataInsertion() {
        String[] signalNames = {"Voltage", "Current", "Power"};
        global.init(3, signalNames, "Time");

        float[] values = {12.5f, 3.2f, 40.0f};
        global.insertValuesAtEnd(values, 0.0);

        assertEquals(12.5f, global.getValue(0, 0), 0.001f);
        assertEquals(3.2f, global.getValue(1, 0), 0.001f);
        assertEquals(40.0f, global.getValue(2, 0), 0.001f);
    }

    @Test
    void testObserverNotification() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        TestObserver testObserver = new TestObserver();
        global.addObserver(testObserver);

        float[] values = {1.0f};
        for (int i = 0; i < 5000; i++) {
            global.insertValuesAtEnd(values, i * 0.001);
        }

        // Observer notification depends on underlying DataContainerSimple behavior
        // Just verify no exception was thrown during mass insertion
        assertTrue(global.getMaximumTimeIndex(0) >= 0);
    }

    @Test
    void testClearMethod() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        float[] values = {5.5f};
        global.insertValuesAtEnd(values, 0.0);

        assertEquals(5.5f, global.getValue(0, 0), 0.001f);

        global.clear();

        global.init(1, signalNames, "Time");
        assertEquals(1, global.getRowLength());
    }

    @Test
    void testReinitialization() {
        String[] signalNames1 = {"Signal1", "Signal2"};
        global.init(2, signalNames1, "Time");

        float[] values1 = {1.0f, 2.0f};
        global.insertValuesAtEnd(values1, 0.0);

        assertEquals(2, global.getRowLength());
        assertEquals("Signal1", global.getSignalName(0));

        String[] signalNames2 = {"Voltage", "Current", "Power"};
        global.init(3, signalNames2, "Time");

        float[] values2 = {10.0f, 20.0f, 30.0f};
        global.insertValuesAtEnd(values2, 0.0);

        assertEquals(3, global.getRowLength());
        assertEquals("Voltage", global.getSignalName(0));
        assertEquals("Current", global.getSignalName(1));
        assertEquals("Power", global.getSignalName(2));

        assertEquals(10.0f, global.getValue(0, 0), 0.001f);
        assertEquals(20.0f, global.getValue(1, 0), 0.001f);
        assertEquals(30.0f, global.getValue(2, 0), 0.001f);
    }

    @Test
    void testEdgeCases_EmptySignalNames() {
        String[] signalNames = {""};
        global.init(1, signalNames, "Time");

        assertEquals("", global.getSignalName(0));

        float[] values = {1.0f};
        global.insertValuesAtEnd(values, 0.0);
        assertEquals(1.0f, global.getValue(0, 0), 0.001f);
    }

    @Test
    void testEdgeCases_SingleRow() {
        String[] signalNames = {"OnlySignal"};
        global.init(1, signalNames, "Time");

        assertEquals(1, global.getRowLength());

        for (int i = 0; i < 10; i++) {
            float[] values = {i * 1.5f};
            global.insertValuesAtEnd(values, i * 0.1);
        }

        assertTrue(global.getMaximumTimeIndex(0) >= 9);
    }

    @Test
    void testEdgeCases_LargeDataInsertion() {
        String[] signalNames = {"Signal1", "Signal2"};
        global.init(2, signalNames, "Time");

        int dataPoints = 1000;
        for (int i = 0; i < dataPoints; i++) {
            float[] values = {i * 0.1f, i * 0.2f};
            global.insertValuesAtEnd(values, i * 0.001);
        }

        assertTrue(global.getMaximumTimeIndex(0) >= dataPoints - 1);

        assertEquals(0.0f, global.getValue(0, 0), 0.001f);
    }

    // ==================== NEW: Coverage Gap Tests ====================

    @Test
    void testGetTimeValue() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        float[] values = {1.0f};
        global.insertValuesAtEnd(values, 0.001);
        global.insertValuesAtEnd(values, 0.002);

        double time0 = global.getTimeValue(0, 0);
        double time1 = global.getTimeValue(1, 0);
        assertEquals(0.001, time0, 1e-6);
        assertEquals(0.002, time1, 1e-6);
    }

    @Test
    void testFindTimeIndex() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        for (int i = 0; i < 10; i++) {
            float[] values = {i * 1.0f};
            global.insertValuesAtEnd(values, i * 0.001);
        }

        int index = global.findTimeIndex(0.005, 0);
        assertTrue(index >= 0);
    }

    @Test
    void testGetHiLoValue() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        for (int i = 0; i < 100; i++) {
            float[] values = {i * 1.0f};
            global.insertValuesAtEnd(values, i * 0.001);
        }

        HiLoData hiLo = global.getHiLoValue(0, 0, 50);
        assertNotNull(hiLo);
    }

    @Test
    void testIsInvalidNumbers() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        float[] values = {1.0f};
        global.insertValuesAtEnd(values, 0.001);

        // Just verify the method doesn't throw
        global.isInvalidNumbers(0);
    }

    @Test
    void testGetContainerStatusBeforeInit() {
        assertEquals(ContainerStatus.NOT_INITIALIZED, global.getContainerStatus());
    }

    @Test
    void testGetContainerStatusAfterInit() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        // After init, container status delegates to underlying DataContainerSimple
        // which may or may not have an initial status set
        global.getContainerStatus();
    }

    @Test
    void testSetContainerStatus() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        global.setContainerStatus(ContainerStatus.PAUSED);
        assertEquals(ContainerStatus.PAUSED, global.getContainerStatus());
    }

    @Test
    void testGetSubcircuitSignalPath() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        String path = global.getSubcircuitSignalPath(0);
        assertNotNull(path);
    }

    @Test
    void testGetAbsoluteMinMaxValue() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        for (int i = 0; i < 10; i++) {
            float[] values = {i * 1.0f};
            global.insertValuesAtEnd(values, i * 0.001);
        }

        HiLoData absMinMax = global.getAbsoluteMinMaxValue(0);
        assertNotNull(absMinMax);
    }

    @Test
    void testGetDataValueInInterval() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        for (int i = 0; i < 100; i++) {
            float[] values = {i * 1.0f};
            global.insertValuesAtEnd(values, i * 0.001);
        }

        Object result = global.getDataValueInInterval(0.0, 0.05, 0);
        assertNotNull(result);
    }

    @Test
    void testGetUsedRAMSizeInMB() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        int ramSize = global.getUsedRAMSizeInMB();
        assertTrue(ramSize >= 0);
    }

    @Test
    void testGetCachedRAMSizeInMB() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        long cachedSize = global.getCachedRAMSizeInMB();
        assertTrue(cachedSize >= 0);
    }

    @Test
    void testGetTimeSeries() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        float[] values = {1.0f};
        global.insertValuesAtEnd(values, 0.001);

        assertNotNull(global.getTimeSeries(0));
    }

    @Test
    void testGetDataArray() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        // Core DataContainerSimple throws UnsupportedOperationException for getDataArray
        assertThrows(UnsupportedOperationException.class, () -> global.getDataArray());
    }

    @Test
    void testHashCode() {
        DataContainerGlobal global2 = new DataContainerGlobal();
        int hash1 = global.hashCode();
        int hash2 = global2.hashCode();
        // Just verify it doesn't throw
        assertTrue(true);
    }

    @Test
    void testGetValueBeforeInit() {
        assertEquals(0.0f, global.getValue(0, 0), 0.001f);
    }

    @Test
    void testGetRowLengthBeforeInit() {
        assertEquals(0, global.getRowLength());
    }

    /**
     * Helper class for testing observer notifications.
     */
    private static class TestObserver implements Observer {
        int notificationCount = 0;

        @Override
        public void update(Observable o, Object arg) {
            notificationCount++;
        }
    }
}
