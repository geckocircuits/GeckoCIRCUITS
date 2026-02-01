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
 * Tests for CapacitorStamper - verifies correct MNA matrix stamping for capacitors.
 */
public class CapacitorStamperTest {

    private static final double TOLERANCE = 1e-12;
    private CapacitorStamper stamper;

    @Before
    public void setUp() {
        stamper = new CapacitorStamper();
    }

    @Test
    public void testGetAdmittanceWeight_1uF_1us() {
        double capacitance = 1e-6; // 1 uF
        double dt = 1e-6; // 1 us

        double admittance = stamper.getAdmittanceWeight(capacitance, dt);

        // G = C/dt = 1e-6 / 1e-6 = 1
        assertEquals("1uF @ 1us should give admittance of 1", 1.0, admittance, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeight_100nF_10us() {
        double capacitance = 100e-9; // 100 nF
        double dt = 10e-6; // 10 us

        double admittance = stamper.getAdmittanceWeight(capacitance, dt);

        // G = C/dt = 100e-9 / 10e-6 = 0.01
        assertEquals("100nF @ 10us should give admittance of 0.01", 0.01, admittance, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeight_SmallCapacitance_ClampsToMinimum() {
        double admittance = stamper.getAdmittanceWeight(0.0, 1e-6);
        // Should clamp to minimum capacitance (1e-15)
        assertEquals("Zero capacitance should clamp", 1e-15 / 1e-6, admittance, 1e-20);
    }

    @Test
    public void testGetAdmittanceWeightTrapezoidal() {
        double capacitance = 1e-6;
        double dt = 1e-6;

        double admittanceTrap = stamper.getAdmittanceWeightTrapezoidal(capacitance, dt);

        // Trapezoidal: G = 2C/dt
        assertEquals("Trapezoidal should give 2x admittance", 2.0, admittanceTrap, TOLERANCE);
    }

    @Test
    public void testStampMatrixA_TwoNodeCapacitor() {
        double[][] a = new double[3][3];
        double[] parameter = {1e-6}; // 1 uF
        double dt = 1e-6;

        stamper.stampMatrixA(a, 1, 2, 0, parameter, dt);

        double expectedAdmittance = 1.0; // C/dt = 1e-6/1e-6 = 1

        // Check diagonal elements
        assertEquals("a[1][1] should be +C/dt", expectedAdmittance, a[1][1], TOLERANCE);
        assertEquals("a[2][2] should be +C/dt", expectedAdmittance, a[2][2], TOLERANCE);

        // Check off-diagonal elements
        assertEquals("a[1][2] should be -C/dt", -expectedAdmittance, a[1][2], TOLERANCE);
        assertEquals("a[2][1] should be -C/dt", -expectedAdmittance, a[2][1], TOLERANCE);

        // Check ground node unaffected
        assertEquals("a[0][0] should be 0", 0.0, a[0][0], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_MatrixIsSymmetric() {
        double[][] a = new double[4][4];
        double[] parameter = {47e-9}; // 47 nF
        double dt = 1e-6;

        stamper.stampMatrixA(a, 1, 3, 0, parameter, dt);

        assertEquals("Matrix should be symmetric: a[1][3] == a[3][1]", a[1][3], a[3][1], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_AddsToExistingValues() {
        double[][] a = new double[3][3];
        a[1][1] = 0.005; // Pre-existing value
        double[] parameter = {1e-6};
        double dt = 1e-6;

        stamper.stampMatrixA(a, 1, 2, 0, parameter, dt);

        // Should add 1.0 to existing 0.005
        assertEquals("Should add to existing value", 1.005, a[1][1], TOLERANCE);
    }

    @Test
    public void testStampVectorB_WithPreviousVoltage() {
        double[] b = new double[3];
        double[] parameter = {1e-6}; // 1 uF
        double dt = 1e-6;
        double[] previousValues = {5.0}; // Previous voltage = 5V

        stamper.stampVectorB(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // History current = (C/dt) * v_prev = 1.0 * 5.0 = 5.0
        assertEquals("b[1] should have +history current", 5.0, b[1], TOLERANCE);
        assertEquals("b[2] should have -history current", -5.0, b[2], TOLERANCE);
    }

    @Test
    public void testStampVectorB_NoPreviousValues() {
        double[] b = new double[3];
        double[] parameter = {1e-6};
        double dt = 1e-6;

        stamper.stampVectorB(b, 1, 2, 0, parameter, dt, 0.0, null);

        // With no previous values, history current should be 0
        assertEquals("b[1] should be 0 with no history", 0.0, b[1], TOLERANCE);
        assertEquals("b[2] should be 0 with no history", 0.0, b[2], TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_FromVoltage() {
        double[] parameter = {1e-6}; // 1 uF
        double dt = 1e-6;
        double vx = 10.0;
        double vy = 5.0;

        double current = stamper.calculateCurrent(vx, vy, parameter, dt, 0.0);

        // I = G * (Vx - Vy) = 1.0 * 5.0 = 5.0 A
        assertEquals("Current should be G * deltaV", 5.0, current, TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_ZeroVoltageDifference() {
        double[] parameter = {1e-6};
        double dt = 1e-6;

        double current = stamper.calculateCurrent(5.0, 5.0, parameter, dt, 0.0);

        assertEquals("Zero voltage difference should give zero current", 0.0, current, TOLERANCE);
    }

    @Test
    public void testImplementsIMatrixStamper() {
        assertTrue("CapacitorStamper should implement IMatrixStamper",
                stamper instanceof IMatrixStamper);
    }

    @Test
    public void testAdmittanceIncreasesWithSmallerDt() {
        double capacitance = 1e-6;

        double admittance1 = stamper.getAdmittanceWeight(capacitance, 1e-6);
        double admittance2 = stamper.getAdmittanceWeight(capacitance, 1e-7);

        assertTrue("Smaller dt should give larger admittance", admittance2 > admittance1);
        assertEquals("Admittance should scale inversely with dt", admittance1 * 10, admittance2, TOLERANCE);
    }

    @Test
    public void testAdmittanceIncreasesWithLargerCapacitance() {
        double dt = 1e-6;

        double admittance1 = stamper.getAdmittanceWeight(1e-6, dt);
        double admittance2 = stamper.getAdmittanceWeight(10e-6, dt);

        assertTrue("Larger capacitance should give larger admittance", admittance2 > admittance1);
        assertEquals("Admittance should scale linearly with C", admittance1 * 10, admittance2, TOLERANCE);
    }
}
