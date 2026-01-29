/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for CircuitTyp enumeration and type identification.
 * Sprint 3: Circuit Components Coverage
 */
public class CircuitTypEnumTest {

    // ========== Basic Passive Components Tests ==========

    @Test
    public void testResistorType() {
        assertEquals("Resistor type", 1, CircuitTyp.LK_R.getTypeNumber());
    }

    @Test
    public void testInductorType() {
        assertEquals("Inductor type", 2, CircuitTyp.LK_L.getTypeNumber());
    }

    @Test
    public void testCapacitorType() {
        assertEquals("Capacitor type", 3, CircuitTyp.LK_C.getTypeNumber());
    }

    // ========== Source Component Tests ==========

    @Test
    public void testVoltageSourceType() {
        assertEquals("Voltage source type", 4, CircuitTyp.LK_U.getTypeNumber());
    }

    @Test
    public void testCurrentSourceType() {
        assertEquals("Current source type", 5, CircuitTyp.LK_I.getTypeNumber());
    }

    // ========== Semiconductor Component Tests ==========

    @Test
    public void testDiodeType() {
        assertEquals("Diode type", 6, CircuitTyp.LK_D.getTypeNumber());
    }

    @Test
    public void testIdealSwitchType() {
        assertEquals("Ideal switch type", 7, CircuitTyp.LK_S.getTypeNumber());
    }

    @Test
    public void testThyristorType() {
        assertEquals("Thyristor type", 8, CircuitTyp.LK_THYR.getTypeNumber());
    }

    @Test
    public void testIGBTType() {
        assertEquals("IGBT type", 10, CircuitTyp.LK_IGBT.getTypeNumber());
    }

    @Test
    public void testMOSFETType() {
        assertEquals("MOSFET type", 28, CircuitTyp.LK_MOSFET.getTypeNumber());
    }

    @Test
    public void testBJTType() {
        assertEquals("BJT type", 33, CircuitTyp.LK_BJT.getTypeNumber());
    }

    // ========== Magnetic Coupling Tests ==========

    @Test
    public void testMutualInductanceType() {
        assertEquals("Mutual inductance type", 9, CircuitTyp.LK_M.getTypeNumber());
    }

    @Test
    public void testCoupleableInductorType() {
        assertEquals("Coupleable inductor type", 12, CircuitTyp.LK_LKOP2.getTypeNumber());
    }

    @Test
    public void testTransformerType() {
        assertEquals("Ideal transformer type", 23, CircuitTyp.LK_TRANS.getTypeNumber());
    }

    // ========== Motor Component Tests ==========

    @Test
    public void testDCMotorType() {
        assertEquals("DC motor type", 14, CircuitTyp.LK_MOTOR.getTypeNumber());
    }

    @Test
    public void testPMSMType() {
        assertEquals("PMSM motor type", 15, CircuitTyp.LK_MOTOR_PMSM.getTypeNumber());
    }

    @Test
    public void testSMSalientType() {
        assertEquals("Salient PM motor type", 16, CircuitTyp.LK_MOTOR_SMSALIENT.getTypeNumber());
    }

    @Test
    public void testSMRoundType() {
        assertEquals("Round PM motor type", 17, CircuitTyp.LK_MOTOR_SMROUND.getTypeNumber());
    }

    @Test
    public void testIMACageType() {
        assertEquals("IM cage motor type", 18, CircuitTyp.LK_MOTOR_IMA.getTypeNumber());
    }

    @Test
    public void testInductionMachineType() {
        assertEquals("Induction machine type", 20, CircuitTyp.LK_MOTOR_IMC.getTypeNumber());
    }

    @Test
    public void testIMSatType() {
        assertEquals("IM saturated type", 21, CircuitTyp.LK_MOTOR_IMSAT.getTypeNumber());
    }

    @Test
    public void testPermanentMotorType() {
        assertEquals("Permanent motor type", 51, CircuitTyp.LK_MOTOR_PERM.getTypeNumber());
    }

    // ========== Special Components Tests ==========

    @Test
    public void testOperationalAmplifierType() {
        assertEquals("Op-amp type", 22, CircuitTyp.LK_OPV1.getTypeNumber());
    }

    @Test
    public void testLISNType() {
        assertEquals("LISN type", 13, CircuitTyp.LK_LISN.getTypeNumber());
    }

    @Test
    public void testTerminalType() {
        assertEquals("Terminal type", 29, CircuitTyp.LK_TERMINAL.getTypeNumber());
    }

    @Test
    public void testGlobalTerminalType() {
        assertEquals("Global terminal type", 31, CircuitTyp.LK_GLOBAL_TERMINAL.getTypeNumber());
    }

    // ========== Reluctance/Magnetic Domain Tests ==========

    @Test
    public void testReluctanceResistorType() {
        assertEquals("Reluctance resistor type", 24, CircuitTyp.REL_RELUCTANCE.getTypeNumber());
    }

    @Test
    public void testReluctanceInductorType() {
        assertEquals("Reluctance inductor type", 25, CircuitTyp.REL_INDUCTOR.getTypeNumber());
    }

    @Test
    public void testReluctanceMMFSourceType() {
        assertEquals("Reluctance MMF source type", 26, CircuitTyp.REL_MMF.getTypeNumber());
    }

    @Test
    public void testReluctanceTerminalType() {
        assertEquals("Reluctance terminal type", 30, CircuitTyp.REL_TERMINAL.getTypeNumber());
    }

    @Test
    public void testReluctanceGlobalTerminalType() {
        assertEquals("Reluctance global terminal type", 32, CircuitTyp.REL_GLOBAL_TERMINAL.getTypeNumber());
    }

    @Test
    public void testNonlinearReluctanceType() {
        assertEquals("Nonlinear reluctance type", 52, CircuitTyp.NONLIN_REL.getTypeNumber());
    }

    // ========== Thermal Domain Tests ==========

    @Test
    public void testThermPVChipType() {
        assertEquals("Thermal PV chip type", 41, CircuitTyp.TH_PvCHIP.getTypeNumber());
    }

    @Test
    public void testThermModuleType() {
        assertEquals("Thermal module type", 42, CircuitTyp.TH_MODUL.getTypeNumber());
    }

    @Test
    public void testThermalFlowType() {
        assertEquals("Thermal flow type", 44, CircuitTyp.TH_FLOW.getTypeNumber());
    }

    @Test
    public void testThermalTemperatureType() {
        assertEquals("Thermal temperature source type", 45, CircuitTyp.TH_TEMP.getTypeNumber());
    }

    @Test
    public void testThermalResistorType() {
        assertEquals("Thermal resistor type", 46, CircuitTyp.TH_RTH.getTypeNumber());
    }

    @Test
    public void testThermalCapacitorType() {
        assertEquals("Thermal capacitor type", 47, CircuitTyp.TH_CTH.getTypeNumber());
    }

    @Test
    public void testThermalAmbientType() {
        assertEquals("Thermal ambient type", 48, CircuitTyp.TH_AMBIENT.getTypeNumber());
    }

    @Test
    public void testThermalTerminalType() {
        assertEquals("Thermal terminal type", 49, CircuitTyp.TH_TERMINAL.getTypeNumber());
    }

    @Test
    public void testThermalGlobalTerminalType() {
        assertEquals("Thermal global terminal type", 50, CircuitTyp.TH_GLOBAL_TERMINAL.getTypeNumber());
    }

    // ========== Reverse Lookup Tests ==========

    @Test
    public void testGetFromIntNumber_Resistor() {
        CircuitTyp typ = CircuitTyp.getFromIntNumber(1);
        assertEquals("Lookup resistor type", CircuitTyp.LK_R, typ);
    }

    @Test
    public void testGetFromIntNumber_Inductor() {
        CircuitTyp typ = CircuitTyp.getFromIntNumber(2);
        assertEquals("Lookup inductor type", CircuitTyp.LK_L, typ);
    }

    @Test
    public void testGetFromIntNumber_Capacitor() {
        CircuitTyp typ = CircuitTyp.getFromIntNumber(3);
        assertEquals("Lookup capacitor type", CircuitTyp.LK_C, typ);
    }

    @Test
    public void testGetFromIntNumber_VoltageSource() {
        CircuitTyp typ = CircuitTyp.getFromIntNumber(4);
        assertEquals("Lookup voltage source type", CircuitTyp.LK_U, typ);
    }

    @Test
    public void testGetFromIntNumber_CurrentSource() {
        CircuitTyp typ = CircuitTyp.getFromIntNumber(5);
        assertEquals("Lookup current source type", CircuitTyp.LK_I, typ);
    }

    @Test
    public void testGetFromIntNumber_AllPassiveComponents() {
        for (int i = 1; i <= 5; i++) {
            CircuitTyp typ = CircuitTyp.getFromIntNumber(i);
            assertNotNull("Should find type for number " + i, typ);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFromIntNumber_InvalidType() {
        // Should throw exception for unknown type
        CircuitTyp.getFromIntNumber(999);
    }

    // ========== Type Info Tests ==========

    @Test
    public void testTypeInfoExists() {
        for (CircuitTyp typ : CircuitTyp.values()) {
            assertNotNull("Type info should exist for " + typ.name(), typ.getTypeInfo());
        }
    }

    @Test
    public void testResistorTypeInfo() {
        assertNotNull("Resistor should have type info", CircuitTyp.LK_R.getTypeInfo());
    }

    @Test
    public void testInductorTypeInfo() {
        assertNotNull("Inductor should have type info", CircuitTyp.LK_L.getTypeInfo());
    }

    @Test
    public void testCapacitorTypeInfo() {
        assertNotNull("Capacitor should have type info", CircuitTyp.LK_C.getTypeInfo());
    }

    // ========== Type Uniqueness Tests ==========

    @Test
    public void testAllTypeNumbersUnique() {
        // Collect all type numbers
        java.util.Set<Integer> typeNumbers = new java.util.HashSet<>();
        for (CircuitTyp typ : CircuitTyp.values()) {
            int num = typ.getTypeNumber();
            assertFalse("Type number " + num + " appears multiple times",
                typeNumbers.contains(num));
            typeNumbers.add(num);
        }
    }

    // ========== Type Count Tests ==========

    @Test
    public void testEnumCount() {
        // Total number of circuit types
        int count = CircuitTyp.values().length;
        assertTrue("Should have multiple types", count > 20);
    }

    // ========== Domain Classification Tests ==========

    @Test
    public void testElectricalDomainBasic() {
        // Basic electrical components: R, L, C, U, I
        assertEquals("Resistor is electrical", 1, CircuitTyp.LK_R.getTypeNumber());
        assertEquals("Inductor is electrical", 2, CircuitTyp.LK_L.getTypeNumber());
        assertEquals("Capacitor is electrical", 3, CircuitTyp.LK_C.getTypeNumber());
        assertEquals("Voltage source is electrical", 4, CircuitTyp.LK_U.getTypeNumber());
        assertEquals("Current source is electrical", 5, CircuitTyp.LK_I.getTypeNumber());
    }

    @Test
    public void testSemiconductorDomain() {
        // Semiconductor components: diode, switch, thyristor, power devices
        assertTrue("Diode is semiconductor", CircuitTyp.LK_D.getTypeNumber() > 0);
        assertTrue("IGBT is semiconductor", CircuitTyp.LK_IGBT.getTypeNumber() > 0);
        assertTrue("MOSFET is semiconductor", CircuitTyp.LK_MOSFET.getTypeNumber() > 0);
    }

    @Test
    public void testThermalDomain() {
        // Thermal components: TH_*
        assertTrue("Thermal RTH >= 40", CircuitTyp.TH_RTH.getTypeNumber() >= 40);
        assertTrue("Thermal CTH >= 40", CircuitTyp.TH_CTH.getTypeNumber() >= 40);
        assertTrue("Thermal TEMP >= 40", CircuitTyp.TH_TEMP.getTypeNumber() >= 40);
    }

    @Test
    public void testReluctanceDomain() {
        // Reluctance components: REL_*
        assertTrue("Reluctance resistor >= 24", CircuitTyp.REL_RELUCTANCE.getTypeNumber() >= 24);
        assertTrue("Reluctance inductor >= 24", CircuitTyp.REL_INDUCTOR.getTypeNumber() >= 24);
    }

    // ========== Motor Types Classification ==========

    @Test
    public void testMotorTypesSequential() {
        // Motor types are grouped
        assertEquals("DC motor is base motor", 14, CircuitTyp.LK_MOTOR.getTypeNumber());
        assertEquals("PMSM is motor variant", 15, CircuitTyp.LK_MOTOR_PMSM.getTypeNumber());
        assertEquals("Salient SM is motor variant", 16, CircuitTyp.LK_MOTOR_SMSALIENT.getTypeNumber());
        assertEquals("Round SM is motor variant", 17, CircuitTyp.LK_MOTOR_SMROUND.getTypeNumber());
    }

    // ========== Enum Values Tests ==========

    @Test
    public void testEnumIterability() {
        // All enum values should be iterable
        int count = 0;
        for (CircuitTyp typ : CircuitTyp.values()) {
            assertNotNull("Type should not be null", typ);
            count++;
        }
        assertTrue("Should have iterated over types", count > 0);
    }

    @Test
    public void testEnumToString() {
        // Enum names should be accessible
        String resistorName = CircuitTyp.LK_R.name();
        assertEquals("Resistor enum name", "LK_R", resistorName);

        String inductorName = CircuitTyp.LK_L.name();
        assertEquals("Inductor enum name", "LK_L", inductorName);
    }
}
