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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for CircuitTyp enum - component types used in MNA matrix stamping.
 *
 * Each CircuitTyp corresponds to a specific circuit component and defines
 * how it contributes to the Modified Nodal Analysis (MNA) matrices.
 */
public class CircuitTypTest {

    @Test
    public void testResistor_TypeNumber() {
        assertEquals("Resistor should have type number 1", 1, CircuitTyp.LK_R.getTypeNumber());
    }

    @Test
    public void testInductor_TypeNumber() {
        assertEquals("Inductor should have type number 2", 2, CircuitTyp.LK_L.getTypeNumber());
    }

    @Test
    public void testCapacitor_TypeNumber() {
        assertEquals("Capacitor should have type number 3", 3, CircuitTyp.LK_C.getTypeNumber());
    }

    @Test
    public void testVoltageSource_TypeNumber() {
        assertEquals("Voltage source should have type number 4", 4, CircuitTyp.LK_U.getTypeNumber());
    }

    @Test
    public void testCurrentSource_TypeNumber() {
        assertEquals("Current source should have type number 5", 5, CircuitTyp.LK_I.getTypeNumber());
    }

    @Test
    public void testDiode_TypeNumber() {
        assertEquals("Diode should have type number 6", 6, CircuitTyp.LK_D.getTypeNumber());
    }

    @Test
    public void testSwitch_TypeNumber() {
        assertEquals("Switch should have type number 7", 7, CircuitTyp.LK_S.getTypeNumber());
    }

    @Test
    public void testIGBT_TypeNumber() {
        assertEquals("IGBT should have type number 10", 10, CircuitTyp.LK_IGBT.getTypeNumber());
    }

    @Test
    public void testMOSFET_TypeNumber() {
        assertEquals("MOSFET should have type number 28", 28, CircuitTyp.LK_MOSFET.getTypeNumber());
    }

    @Test
    public void testGetFromIntNumber_Resistor() {
        CircuitTyp typ = CircuitTyp.getFromIntNumber(1);
        assertEquals("Type 1 should be resistor", CircuitTyp.LK_R, typ);
    }

    @Test
    public void testGetFromIntNumber_Capacitor() {
        CircuitTyp typ = CircuitTyp.getFromIntNumber(3);
        assertEquals("Type 3 should be capacitor", CircuitTyp.LK_C, typ);
    }

    @Test
    public void testGetFromIntNumber_Inductor() {
        CircuitTyp typ = CircuitTyp.getFromIntNumber(2);
        assertEquals("Type 2 should be inductor", CircuitTyp.LK_L, typ);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFromIntNumber_InvalidNumber_ThrowsException() {
        CircuitTyp.getFromIntNumber(9999);
    }

    @Test
    public void testThermalResistor_TypeNumber() {
        assertEquals("Thermal resistor should have type number 46", 46, CircuitTyp.TH_RTH.getTypeNumber());
    }

    @Test
    public void testThermalCapacitor_TypeNumber() {
        assertEquals("Thermal capacitor should have type number 47", 47, CircuitTyp.TH_CTH.getTypeNumber());
    }

    @Test
    public void testReluctance_TypeNumber() {
        assertEquals("Reluctance should have type number 24", 24, CircuitTyp.REL_RELUCTANCE.getTypeNumber());
    }

    @Test
    public void testAllTypesHaveTypeInfo() {
        for (CircuitTyp typ : CircuitTyp.values()) {
            assertNotNull("Type " + typ.name() + " should have type info", typ.getTypeInfo());
        }
    }

    @Test
    public void testTypeNumbersAreUnique() {
        java.util.Set<Integer> seenNumbers = new java.util.HashSet<>();
        for (CircuitTyp typ : CircuitTyp.values()) {
            int typeNumber = typ.getTypeNumber();
            assertFalse("Type number " + typeNumber + " is duplicated",
                    seenNumbers.contains(typeNumber));
            seenNumbers.add(typeNumber);
        }
    }
}
