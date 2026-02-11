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
 * Tests for ResistorStamper - verifies correct MNA matrix stamping for resistors.
 */
public class ResistorStamperTest {

    private static final double TOLERANCE = 1e-12;
    private ResistorStamper stamper;

    @Before
    public void setUp() {
        stamper = new ResistorStamper();
    }

    @Test
    public void testGetAdmittanceWeight_1Ohm() {
        double admittance = stamper.getAdmittanceWeight(1.0, 1e-6);
        assertEquals("1 Ohm should give admittance of 1", 1.0, admittance, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeight_10Ohm() {
        double admittance = stamper.getAdmittanceWeight(10.0, 1e-6);
        assertEquals("10 Ohm should give admittance of 0.1", 0.1, admittance, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeight_100kOhm() {
        double admittance = stamper.getAdmittanceWeight(100000.0, 1e-6);
        assertEquals("100k Ohm should give admittance of 1e-5", 1e-5, admittance, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeight_ZeroResistance_ClampsToMinimum() {
        double admittance = stamper.getAdmittanceWeight(0.0, 1e-6);
        // Should clamp to minimum resistance (1e-9), giving admittance of 1e9
        assertEquals("Zero resistance should clamp to 1e9 admittance", 1e9, admittance, 1.0);
    }

    @Test
    public void testStampMatrixA_TwoNodeResistor() {
        double[][] a = new double[3][3];
        double[] parameter = {1000.0}; // 1k Ohm

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        double expectedAdmittance = 1.0 / 1000.0; // 0.001 S

        // Check diagonal elements (self-admittance)
        assertEquals("a[1][1] should be +1/R", expectedAdmittance, a[1][1], TOLERANCE);
        assertEquals("a[2][2] should be +1/R", expectedAdmittance, a[2][2], TOLERANCE);

        // Check off-diagonal elements (mutual admittance)
        assertEquals("a[1][2] should be -1/R", -expectedAdmittance, a[1][2], TOLERANCE);
        assertEquals("a[2][1] should be -1/R", -expectedAdmittance, a[2][1], TOLERANCE);

        // Check that node 0 (ground) is not affected
        assertEquals("a[0][0] should be 0", 0.0, a[0][0], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_MatrixIsSymmetric() {
        double[][] a = new double[4][4];
        double[] parameter = {500.0}; // 500 Ohm

        stamper.stampMatrixA(a, 1, 3, 0, parameter, 1e-6);

        // For a resistor, the matrix should be symmetric
        assertEquals("Matrix should be symmetric: a[1][3] == a[3][1]", a[1][3], a[3][1], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_AddsToExistingValues() {
        double[][] a = new double[3][3];
        a[1][1] = 0.005; // Pre-existing value
        double[] parameter = {1000.0}; // 1k Ohm

        stamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        // Should add 0.001 to existing 0.005
        assertEquals("Should add to existing value", 0.006, a[1][1], TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_Ohm_Law() {
        double[] parameter = {100.0}; // 100 Ohm
        double vx = 10.0; // 10V at node X
        double vy = 5.0;  // 5V at node Y

        double current = stamper.calculateCurrent(vx, vy, parameter, 1e-6, 0.0);

        // I = (Vx - Vy) / R = (10 - 5) / 100 = 0.05 A
        assertEquals("Current should follow Ohm's law", 0.05, current, TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_ReversedVoltage() {
        double[] parameter = {100.0}; // 100 Ohm
        double vx = 5.0;
        double vy = 10.0;

        double current = stamper.calculateCurrent(vx, vy, parameter, 1e-6, 0.0);

        // I = (5 - 10) / 100 = -0.05 A (negative, flows other direction)
        assertEquals("Reversed voltage should give negative current", -0.05, current, TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_EqualVoltages() {
        double[] parameter = {100.0}; // 100 Ohm
        double vx = 5.0;
        double vy = 5.0;

        double current = stamper.calculateCurrent(vx, vy, parameter, 1e-6, 0.0);

        assertEquals("Equal voltages should give zero current", 0.0, current, TOLERANCE);
    }

    @Test
    public void testStampVectorB_NoContribution() {
        double[] b = new double[3];
        b[1] = 1.0; // Pre-existing value
        double[] parameter = {100.0};
        double[] previousValues = new double[10];

        stamper.stampVectorB(b, 1, 2, 0, parameter, 1e-6, 0.0, previousValues);

        // Resistor should not modify b vector
        assertEquals("b[1] should be unchanged", 1.0, b[1], TOLERANCE);
        assertEquals("b[2] should remain 0", 0.0, b[2], TOLERANCE);
    }

    @Test
    public void testImplementsIMatrixStamper() {
        assertTrue("ResistorStamper should implement IMatrixStamper",
                stamper instanceof IMatrixStamper);
    }
}
