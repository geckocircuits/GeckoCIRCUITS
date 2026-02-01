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
package ch.technokrat.gecko.geckocircuits.circuit.matrix;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for VoltageSourceStamper - verifies correct MNA matrix stamping for voltage sources.
 */
public class VoltageSourceStamperTest {

    private static final double TOLERANCE = 1e-12;
    private VoltageSourceStamper stamper;

    @Before
    public void setUp() {
        stamper = new VoltageSourceStamper();
    }

    @Test
    public void testStampMatrixA_VoltageEquation() {
        double[][] a = new double[4][4];
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, 12.0}; // 12V DC
        double dt = 1e-6;

        // nodeZ=3 is the current variable row
        stamper.stampMatrixA(a, 1, 2, 3, parameter, dt);

        // Voltage equation: a[nodeZ][nodeX] = +1, a[nodeZ][nodeY] = -1
        assertEquals("a[3][1] should be +1", 1.0, a[3][1], TOLERANCE);
        assertEquals("a[3][2] should be -1", -1.0, a[3][2], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_KCLEquations() {
        double[][] a = new double[4][4];
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, 12.0};
        double dt = 1e-6;

        stamper.stampMatrixA(a, 1, 2, 3, parameter, dt);

        // KCL: a[nodeX][nodeZ] = +1, a[nodeY][nodeZ] = -1
        assertEquals("a[1][3] should be +1", 1.0, a[1][3], TOLERANCE);
        assertEquals("a[2][3] should be -1", -1.0, a[2][3], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_AddsToExisting() {
        double[][] a = new double[4][4];
        a[1][3] = 0.5; // Pre-existing
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, 12.0};
        double dt = 1e-6;

        stamper.stampMatrixA(a, 1, 2, 3, parameter, dt);

        assertEquals("Should add to existing a[1][3]", 1.5, a[1][3], TOLERANCE);
    }

    @Test
    public void testStampVectorB_DCSource() {
        double[] b = new double[4];
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, 12.0};
        double dt = 1e-6;

        stamper.stampVectorB(b, 1, 2, 3, parameter, dt, 0.0, null);

        assertEquals("b[3] should be source voltage", 12.0, b[3], TOLERANCE);
    }

    @Test
    public void testStampVectorB_NegativeVoltage() {
        double[] b = new double[4];
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, -5.0};
        double dt = 1e-6;

        stamper.stampVectorB(b, 1, 2, 3, parameter, dt, 0.0, null);

        assertEquals("b[3] should be negative voltage", -5.0, b[3], TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_DC() {
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, 24.0};

        double voltage = stamper.calculateSourceVoltage(parameter, 0.0);
        assertEquals("DC voltage should be constant", 24.0, voltage, TOLERANCE);

        voltage = stamper.calculateSourceVoltage(parameter, 1.0);
        assertEquals("DC voltage should be same at any time", 24.0, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_AC_AtZero() {
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, 100.0, 50.0, 0.0}; // 100V, 50Hz, 0 phase

        double voltage = stamper.calculateSourceVoltage(parameter, 0.0);
        assertEquals("AC at t=0 with zero phase should be 0", 0.0, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_AC_AtQuarterPeriod() {
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, 100.0, 50.0, 0.0}; // 100V, 50Hz
        double quarterPeriod = 1.0 / 50.0 / 4.0; // T/4 = 5ms

        double voltage = stamper.calculateSourceVoltage(parameter, quarterPeriod);
        // At T/4, sin(2*pi*f*t) = sin(pi/2) = 1
        assertEquals("AC at T/4 should be peak", 100.0, voltage, 1e-9);
    }

    @Test
    public void testCalculateSourceVoltage_AC_WithPhase() {
        double[] parameter = {VoltageSourceStamper.SOURCE_AC, 100.0, 50.0, Math.PI / 2}; // 90 deg phase

        double voltage = stamper.calculateSourceVoltage(parameter, 0.0);
        // At t=0 with 90 deg phase, sin(pi/2) = 1
        assertEquals("AC at t=0 with 90 deg phase should be peak", 100.0, voltage, 1e-9);
    }

    @Test
    public void testCalculateSourceVoltage_NullParameter() {
        double voltage = stamper.calculateSourceVoltage(null, 0.0);
        assertEquals("Null parameter should return 0", 0.0, voltage, TOLERANCE);
    }

    @Test
    public void testCalculateSourceVoltage_ShortParameter() {
        double[] parameter = {VoltageSourceStamper.SOURCE_DC};

        double voltage = stamper.calculateSourceVoltage(parameter, 0.0);
        assertEquals("Short parameter should return 0", 0.0, voltage, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeight_ReturnsZero() {
        double admittance = stamper.getAdmittanceWeight(12.0, 1e-6);
        assertEquals("Voltage source has no simple admittance", 0.0, admittance, TOLERANCE);
    }

    @Test
    public void testGetAdditionalMatrixSize() {
        assertEquals("Voltage source adds 1 to matrix size", 1, stamper.getAdditionalMatrixSize());
    }

    @Test
    public void testStampMatrixAGrounded() {
        double[][] a = new double[4][4];

        stamper.stampMatrixAGrounded(a, 1, 3);

        // Grounded: nodeY is implicit ground
        assertEquals("a[3][1] should be +1", 1.0, a[3][1], TOLERANCE);
        assertEquals("a[1][3] should be +1", 1.0, a[1][3], TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_ReturnsPrevious() {
        double[] parameter = {VoltageSourceStamper.SOURCE_DC, 12.0};
        double previousCurrent = 3.5;

        double current = stamper.calculateCurrent(10.0, 5.0, parameter, 1e-6, previousCurrent);

        // Voltage source current is determined by circuit, not by method
        assertEquals("Should return previous current", previousCurrent, current, TOLERANCE);
    }

    @Test
    public void testImplementsIMatrixStamper() {
        assertTrue("VoltageSourceStamper should implement IMatrixStamper",
                stamper instanceof IMatrixStamper);
    }

    @Test
    public void testMultipleVoltageSourcesInMatrix() {
        double[][] a = new double[6][6];
        double[] param1 = {VoltageSourceStamper.SOURCE_DC, 12.0};
        double[] param2 = {VoltageSourceStamper.SOURCE_DC, 5.0};
        double dt = 1e-6;

        // First source between nodes 1-2, current var at 4
        stamper.stampMatrixA(a, 1, 2, 4, param1, dt);
        // Second source between nodes 2-3, current var at 5
        stamper.stampMatrixA(a, 2, 3, 5, param2, dt);

        // Verify first source
        assertEquals("First source a[4][1]", 1.0, a[4][1], TOLERANCE);
        assertEquals("First source a[4][2]", -1.0, a[4][2], TOLERANCE);

        // Verify second source
        assertEquals("Second source a[5][2]", 1.0, a[5][2], TOLERANCE);
        assertEquals("Second source a[5][3]", -1.0, a[5][3], TOLERANCE);

        // Verify KCL at shared node 2
        // Both sources contribute to node 2's KCL equation
        assertEquals("KCL at node 2 from first source", -1.0, a[2][4], TOLERANCE);
        assertEquals("KCL at node 2 from second source", 1.0, a[2][5], TOLERANCE);
    }
}
