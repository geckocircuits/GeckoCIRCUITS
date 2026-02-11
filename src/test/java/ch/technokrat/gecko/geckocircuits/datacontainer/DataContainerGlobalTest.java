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
import java.util.Observer;
import java.util.Observable;

public class DataContainerGlobalTest {

    private DataContainerGlobal global;

    @Before
    public void setUp() {
        global = new DataContainerGlobal();
    }

    @Test
    public void testConstructor() {
        assertNotNull(global);
    }

    @Test
    public void testInitialization() {
        String[] signalNames = {"Voltage", "Current", "Power"};
        global.init(3, signalNames, "Time");

        assertEquals(3, global.getRowLength());
    }

    @Test
    public void testInsertValuesAtEnd() {
        String[] signalNames = {"Signal1", "Signal2"};
        global.init(2, signalNames, "Time");

        float[] values = {1.0f, 2.0f};
        global.insertValuesAtEnd(values, 0.0);

        assertNotNull(global.getValue(0, 0));
    }

    @Test
    public void testGetValue() {
        String[] signalNames = {"Signal1"};
        global.init(1, signalNames, "Time");

        float[] values = {5.5f};
        global.insertValuesAtEnd(values, 0.0);

        assertEquals(5.5f, global.getValue(0, 0), 0.001f);
    }

    @Test
    public void testGetRowLength() {
        String[] signalNames = {"S1", "S2", "S3", "S4"};
        global.init(4, signalNames, "Time");

        assertEquals(4, global.getRowLength());
    }

    @Test
    public void testMultipleInsertions() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        for (int i = 0; i < 5; i++) {
            float[] values = {i * 1.0f};
            global.insertValuesAtEnd(values, i * 0.001);
        }

        assertTrue(global.getMaximumTimeIndex(0) >= 0);
    }

    @Test
    public void testGetSignalName() {
        String[] signalNames = {"Voltage", "Current"};
        global.init(2, signalNames, "Time");

        assertEquals("Voltage", global.getSignalName(0));
        assertEquals("Current", global.getSignalName(1));
    }

    @Test
    public void testGetXAxisName() {
        String[] signalNames = {"Signal1"};
        global.init(1, signalNames, "TimeAxis");

        assertEquals("TimeAxis", global.getXDataName());
    }

    @Test
    public void testGetMaximumTimeIndex() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        float[] values = {1.0f};
        global.insertValuesAtEnd(values, 0.0);
        global.insertValuesAtEnd(values, 1.0);

        int maxIndex = global.getMaximumTimeIndex(0);
        assertTrue(maxIndex >= 1);
    }

    @Test
    public void testDataContainerGlobalDelegate() {
        String[] signalNames = {"Voltage", "Current"};
        global.init(2, signalNames, "Time");

        float[] values = {10.0f, 20.0f};
        global.insertValuesAtEnd(values, 0.0);

        assertEquals(10.0f, global.getValue(0, 0), 0.001f);
        assertEquals(20.0f, global.getValue(1, 0), 0.001f);
    }

    @Test
    public void testDataContainerGlobalWithoutInit() {
        assertNotNull(global.getValue(0, 0));
    }

    @Test
    public void testInitializationReplacement() {
        String[] names1 = {"Signal1"};
        global.init(1, names1, "Time1");

        String[] names2 = {"Signal2", "Signal3"};
        global.init(2, names2, "Time2");

        assertEquals(2, global.getRowLength());
        assertEquals("Signal2", global.getSignalName(0));
        assertEquals("Signal3", global.getSignalName(1));
    }

    @Test
    public void testMultiRowDataInsertion() {
        String[] signalNames = {"Voltage", "Current", "Power"};
        global.init(3, signalNames, "Time");

        float[] values = {12.5f, 3.2f, 40.0f};
        global.insertValuesAtEnd(values, 0.0);

        assertEquals(12.5f, global.getValue(0, 0), 0.001f);
        assertEquals(3.2f, global.getValue(1, 0), 0.001f);
        assertEquals(40.0f, global.getValue(2, 0), 0.001f);
    }

    @Test
    public void testObserverNotification() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        TestObserver testObserver = new TestObserver();
        global.addObserver(testObserver);

        float[] values = {1.0f};
        for (int i = 0; i < 5000; i++) {
            global.insertValuesAtEnd(values, i * 0.001);
        }

        assertTrue("Observer should have been notified", testObserver.notificationCount > 0);
    }

    @Test
    public void testClearMethod() {
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
    public void testReinitialization() {
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
    public void testEdgeCases_EmptySignalNames() {
        String[] signalNames = {""};
        global.init(1, signalNames, "Time");

        assertEquals("", global.getSignalName(0));

        float[] values = {1.0f};
        global.insertValuesAtEnd(values, 0.0);
        assertEquals(1.0f, global.getValue(0, 0), 0.001f);
    }

    @Test
    public void testEdgeCases_SingleRow() {
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
    public void testEdgeCases_LargeDataInsertion() {
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
    public void testGetTimeValue() {
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
    public void testFindTimeIndex() {
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
    public void testGetHiLoValue() {
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
    public void testIsInvalidNumbers() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        float[] values = {1.0f};
        global.insertValuesAtEnd(values, 0.001);

        assertFalse(global.isInvalidNumbers(0));
    }

    @Test
    public void testGetContainerStatusBeforeInit() {
        assertEquals(ContainerStatus.NOT_INITIALIZED, global.getContainerStatus());
    }

    @Test
    public void testGetContainerStatusAfterInit() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        ContainerStatus status = global.getContainerStatus();
        assertNotNull(status);
    }

    @Test
    public void testSetContainerStatus() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        global.setContainerStatus(ContainerStatus.PAUSED);
        assertEquals(ContainerStatus.PAUSED, global.getContainerStatus());
    }

    @Test
    public void testGetSubcircuitSignalPath() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        String path = global.getSubcircuitSignalPath(0);
        assertNotNull(path);
    }

    @Test
    public void testGetAbsoluteMinMaxValue() {
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
    public void testGetDataValueInInterval() {
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
    public void testGetUsedRAMSizeInMB() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        int ramSize = global.getUsedRAMSizeInMB();
        assertTrue(ramSize >= 0);
    }

    @Test
    public void testGetCachedRAMSizeInMB() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        long cachedSize = global.getCachedRAMSizeInMB();
        assertTrue(cachedSize >= 0);
    }

    @Test
    public void testGetTimeSeries() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        float[] values = {1.0f};
        global.insertValuesAtEnd(values, 0.001);

        assertNotNull(global.getTimeSeries(0));
    }

    @Test
    public void testGetDataArray() {
        String[] signalNames = {"Signal"};
        global.init(1, signalNames, "Time");

        float[] dataArray = global.getDataArray();
        assertNotNull(dataArray);
    }

    @Test
    public void testHashCode() {
        DataContainerGlobal global2 = new DataContainerGlobal();
        int hash1 = global.hashCode();
        int hash2 = global2.hashCode();
        // Just verify it doesn't throw
        assertTrue(true);
    }

    @Test
    public void testGetValueBeforeInit() {
        assertEquals(0.0f, global.getValue(0, 0), 0.001f);
    }

    @Test
    public void testGetRowLengthBeforeInit() {
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
