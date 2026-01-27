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

public class SignalDataContainerTest {

    private DataContainerSimple dataContainer;

    @Before
    public void setUp() {
        dataContainer = DataContainerSimple.fabricConstantDtTimeSeries(5, 10);
    }

    @Test
    public void testSignalDataContainerRegularCreation() {
        SignalDataContainerRegular signal = new SignalDataContainerRegular(dataContainer, 0);
        assertNotNull(signal);
        assertEquals(dataContainer, signal.getDataContainer());
        assertEquals(0, signal.getContainerSignalIndex());
    }

    @Test
    public void testSignalDataContainerRegularGetSignalName() {
        SignalDataContainerRegular signal = new SignalDataContainerRegular(dataContainer, 0);
        String signalName = signal.getSignalName();
        assertNotNull(signalName);
    }

    @Test
    public void testSignalDataContainerRegularSetSignalName() {
        SignalDataContainerRegular signal = new SignalDataContainerRegular(dataContainer, 0);
        signal.setSignalName("TestSignal");
        assertEquals("TestSignal", signal.getSignalName());
    }

    @Test
    public void testSignalDataContainerRegularToString() {
        SignalDataContainerRegular signal = new SignalDataContainerRegular(dataContainer, 2);
        String toStringResult = signal.toString();
        assertNotNull(toStringResult);
    }

    @Test
    public void testSignalDataContainerRegularIndex() {
        SignalDataContainerRegular signal = new SignalDataContainerRegular(dataContainer, 3);
        assertEquals(3, signal.getContainerSignalIndex());
    }

    @Test
    public void testMultipleSignalsRegular() {
        SignalDataContainerRegular signal1 = new SignalDataContainerRegular(dataContainer, 0);
        SignalDataContainerRegular signal2 = new SignalDataContainerRegular(dataContainer, 1);
        
        signal1.setSignalName("Signal1");
        signal2.setSignalName("Signal2");
        
        assertEquals("Signal1", signal1.getSignalName());
        assertEquals("Signal2", signal2.getSignalName());
    }

    @Test
    public void testSignalDataContainerRegularWithMultipleDataContainers() {
        DataContainerSimple container1 = DataContainerSimple.fabricConstantDtTimeSeries(5, 10);
        DataContainerSimple container2 = DataContainerSimple.fabricConstantDtTimeSeries(3, 8);
        
        SignalDataContainerRegular signal1 = new SignalDataContainerRegular(container1, 0);
        SignalDataContainerRegular signal2 = new SignalDataContainerRegular(container2, 1);
        
        assertEquals(container1, signal1.getDataContainer());
        assertEquals(container2, signal2.getDataContainer());
    }

    @Test
    public void testSignalIndependenceRegular() {
        SignalDataContainerRegular signal = new SignalDataContainerRegular(dataContainer, 4);
        signal.setSignalName("IndependentSignal");
        
        assertEquals("IndependentSignal", signal.getSignalName());
        assertEquals(4, signal.getContainerSignalIndex());
    }

    @Test
    public void testSignalDataContainerRegularChangeSignalName() {
        SignalDataContainerRegular signal = new SignalDataContainerRegular(dataContainer, 0);
        signal.setSignalName("FirstName");
        assertEquals("FirstName", signal.getSignalName());
        signal.setSignalName("SecondName");
        assertEquals("SecondName", signal.getSignalName());
    }
}
