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

import ch.technokrat.gecko.geckocircuits.api.IScopeData;
import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for data container implementations and IScopeData interface.
 */
public class DataContainerTest {

    private static final double DELTA = 1e-6;

    @Test
    public void testDataContainerSimpleImplementsIScopeData() {
        // Verify that DataContainerSimple implements IScopeData through AbstractDataContainer
        DataContainerSimple container = new DataContainerSimple(2, 100);
        assertTrue("DataContainerSimple should implement IScopeData",
                   container instanceof IScopeData);
    }

    @Test
    public void testDataContainerSimpleRowLength() {
        DataContainerSimple container = new DataContainerSimple(3, 100);
        assertEquals(3, container.getRowLength());
    }

    @Test
    public void testDataContainerSimpleSetAndGetValue() {
        // Note: DataContainerSimple requires row > 0 for setValue
        DataContainerSimple container = new DataContainerSimple(3, 10);

        // Set values (row must be > 0)
        container.setValue(5.0f, 1, 0);  // row 1, col 0
        container.setValue(10.0f, 1, 1); // row 1, col 1
        container.setValue(15.0f, 2, 0); // row 2, col 0

        // Get values
        assertEquals(5.0f, container.getValue(1, 0), DELTA);
        assertEquals(10.0f, container.getValue(1, 1), DELTA);
        assertEquals(15.0f, container.getValue(2, 0), DELTA);
    }

    @Test
    public void testDataContainerSimpleHiLoValue() {
        // Note: DataContainerSimple requires row > 0 for setValue
        DataContainerSimple container = new DataContainerSimple(2, 5);

        // Set a range of values (row must be > 0)
        container.setValue(1.0f, 1, 0);
        container.setValue(5.0f, 1, 1);
        container.setValue(3.0f, 1, 2);
        container.setValue(8.0f, 1, 3);
        container.setValue(2.0f, 1, 4);

        // Get hi-lo for range
        HiLoData hiLo = container.getHiLoValue(1, 0, 5);
        assertNotNull(hiLo);
        assertEquals(1.0f, hiLo._yLo, DELTA);
        assertEquals(8.0f, hiLo._yHi, DELTA);
    }

    @Test
    public void testDataContainerSimpleAbsoluteMinMax() {
        // Note: DataContainerSimple requires row > 0 for setValue
        DataContainerSimple container = new DataContainerSimple(2, 5);

        container.setValue(-10.0f, 1, 0);
        container.setValue(5.0f, 1, 1);
        container.setValue(20.0f, 1, 2);
        container.setValue(-5.0f, 1, 3);
        container.setValue(0.0f, 1, 4);

        HiLoData absMinMax = container.getAbsoluteMinMaxValue(1);
        assertNotNull(absMinMax);
        assertEquals(-10.0f, absMinMax._yLo, DELTA);
        assertEquals(20.0f, absMinMax._yHi, DELTA);
    }

    @Test
    public void testDataContainerSimpleSignalName() {
        DataContainerSimple container = new DataContainerSimple(2, 10);

        // Signal names should be set or have defaults
        String signalName = container.getSignalName(0);
        assertNotNull(signalName);
    }

    @Test
    public void testDataContainerSimpleXDataName() {
        DataContainerSimple container = new DataContainerSimple(1, 10);

        String xDataName = container.getXDataName();
        assertNotNull(xDataName);
    }

    @Test
    public void testDataContainerSimpleContainerStatus() {
        DataContainerSimple container = new DataContainerSimple(1, 10);

        // Set and get status
        container.setContainerStatus(ContainerStatus.RUNNING);
        assertEquals(ContainerStatus.RUNNING, container.getContainerStatus());

        container.setContainerStatus(ContainerStatus.PAUSED);
        assertEquals(ContainerStatus.PAUSED, container.getContainerStatus());
    }

    @Test
    public void testDataContainerGlobalImplementsIScopeData() {
        // Verify that DataContainerGlobal implements IScopeData
        DataContainerGlobal container = new DataContainerGlobal();
        assertTrue("DataContainerGlobal should implement IScopeData",
                   container instanceof IScopeData);
    }

    @Test
    public void testDataContainerNullDataImplementsIScopeData() {
        // Verify that DataContainerNullData implements IScopeData
        DataContainerNullData container = new DataContainerNullData();
        assertTrue("DataContainerNullData should implement IScopeData",
                   container instanceof IScopeData);
    }
}
