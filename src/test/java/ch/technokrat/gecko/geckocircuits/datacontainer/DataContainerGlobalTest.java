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
        
        // Verify some values were inserted
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
        // Before initialization, should use NullData
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
}
