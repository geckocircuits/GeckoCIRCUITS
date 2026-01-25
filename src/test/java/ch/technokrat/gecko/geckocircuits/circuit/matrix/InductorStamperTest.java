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
 * Tests for InductorStamper - verifies correct MNA matrix stamping for inductors.
 */
public class InductorStamperTest {

    private static final double TOLERANCE = 1e-12;
    private InductorStamper stamper;

    @Before
    public void setUp() {
        stamper = new InductorStamper();
    }

    @Test
    public void testGetAdmittanceWeight_1mH_1us() {
        double inductance = 1e-3; // 1 mH
        double dt = 1e-6; // 1 us

        double admittance = stamper.getAdmittanceWeight(inductance, dt);

        // G = dt/L = 1e-6 / 1e-3 = 1e-3
        assertEquals("1mH @ 1us should give admittance of 1e-3", 1e-3, admittance, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeight_100uH_10us() {
        double inductance = 100e-6; // 100 uH
        double dt = 10e-6; // 10 us

        double admittance = stamper.getAdmittanceWeight(inductance, dt);

        // G = dt/L = 10e-6 / 100e-6 = 0.1
        assertEquals("100uH @ 10us should give admittance of 0.1", 0.1, admittance, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeight_SmallInductance_ClampsToMinimum() {
        double admittance = stamper.getAdmittanceWeight(0.0, 1e-6);
        // Should clamp to minimum inductance (1e-15)
        assertEquals("Zero inductance should clamp", 1e-6 / 1e-15, admittance, 1e6);
    }

    @Test
    public void testGetAdmittanceWeightTrapezoidal() {
        double inductance = 1e-3;
        double dt = 1e-6;

        double admittanceTrap = stamper.getAdmittanceWeightTrapezoidal(inductance, dt);

        // Trapezoidal: G = dt/(2L)
        double expected = 1e-6 / (2.0 * 1e-3);
        assertEquals("Trapezoidal should give half admittance", expected, admittanceTrap, TOLERANCE);
    }

    @Test
    public void testStampMatrixA_TwoNodeInductor() {
        double[][] a = new double[3][3];
        double[] parameter = {1e-3}; // 1 mH
        double dt = 1e-6;

        stamper.stampMatrixA(a, 1, 2, 0, parameter, dt);

        double expectedAdmittance = 1e-3; // dt/L = 1e-6/1e-3 = 1e-3

        // Check diagonal elements
        assertEquals("a[1][1] should be +dt/L", expectedAdmittance, a[1][1], TOLERANCE);
        assertEquals("a[2][2] should be +dt/L", expectedAdmittance, a[2][2], TOLERANCE);

        // Check off-diagonal elements
        assertEquals("a[1][2] should be -dt/L", -expectedAdmittance, a[1][2], TOLERANCE);
        assertEquals("a[2][1] should be -dt/L", -expectedAdmittance, a[2][1], TOLERANCE);

        // Check ground node unaffected
        assertEquals("a[0][0] should be 0", 0.0, a[0][0], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_MatrixIsSymmetric() {
        double[][] a = new double[4][4];
        double[] parameter = {47e-6}; // 47 uH
        double dt = 1e-6;

        stamper.stampMatrixA(a, 1, 3, 0, parameter, dt);

        assertEquals("Matrix should be symmetric: a[1][3] == a[3][1]", a[1][3], a[3][1], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_AddsToExistingValues() {
        double[][] a = new double[3][3];
        a[1][1] = 0.005; // Pre-existing value
        double[] parameter = {1e-3};
        double dt = 1e-6;

        stamper.stampMatrixA(a, 1, 2, 0, parameter, dt);

        // Should add 1e-3 to existing 0.005
        assertEquals("Should add to existing value", 0.006, a[1][1], TOLERANCE);
    }

    @Test
    public void testStampVectorB_WithPreviousCurrent() {
        double[] b = new double[3];
        double[] parameter = {1e-3}; // 1 mH
        double dt = 1e-6;
        double[] previousValues = {2.0}; // Previous current = 2A

        stamper.stampVectorB(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // History current = i_prev = 2.0
        assertEquals("b[1] should have +previous current", 2.0, b[1], TOLERANCE);
        assertEquals("b[2] should have -previous current", -2.0, b[2], TOLERANCE);
    }

    @Test
    public void testStampVectorB_NoPreviousValues() {
        double[] b = new double[3];
        double[] parameter = {1e-3};
        double dt = 1e-6;

        stamper.stampVectorB(b, 1, 2, 0, parameter, dt, 0.0, null);

        // With no previous values, history current should be 0
        assertEquals("b[1] should be 0 with no history", 0.0, b[1], TOLERANCE);
        assertEquals("b[2] should be 0 with no history", 0.0, b[2], TOLERANCE);
    }

    @Test
    public void testStampVectorBTrapezoidal() {
        double[] b = new double[3];
        double[] parameter = {1e-3}; // 1 mH
        double dt = 1e-6;
        double[] previousValues = {2.0, 10.0}; // i_prev=2A, v_prev=10V

        stamper.stampVectorBTrapezoidal(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // G = dt/(2L) = 1e-6 / 2e-3 = 5e-4
        // I_hist = i_prev + G * v_prev = 2.0 + 5e-4 * 10 = 2.005
        double expectedHistory = 2.0 + (1e-6 / 2e-3) * 10.0;
        assertEquals("b[1] should have trapezoidal history", expectedHistory, b[1], TOLERANCE);
        assertEquals("b[2] should have negative trapezoidal history", -expectedHistory, b[2], TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_FromVoltage() {
        double[] parameter = {1e-3}; // 1 mH
        double dt = 1e-6;
        double vx = 10.0;
        double vy = 0.0;
        double previousCurrent = 1.0;

        double current = stamper.calculateCurrent(vx, vy, parameter, dt, previousCurrent);

        // i = i_prev + G * v = 1.0 + 1e-3 * 10 = 1.01 A
        assertEquals("Current should follow inductor equation", 1.01, current, TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_ZeroVoltageDifference() {
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double previousCurrent = 5.0;

        double current = stamper.calculateCurrent(10.0, 10.0, parameter, dt, previousCurrent);

        // Zero voltage means current stays the same
        assertEquals("Zero voltage should maintain current", previousCurrent, current, TOLERANCE);
    }

    @Test
    public void testImplementsIMatrixStamper() {
        assertTrue("InductorStamper should implement IMatrixStamper",
                stamper instanceof IMatrixStamper);
    }

    @Test
    public void testAdmittanceIncreasesWithLargerDt() {
        double inductance = 1e-3;

        double admittance1 = stamper.getAdmittanceWeight(inductance, 1e-6);
        double admittance2 = stamper.getAdmittanceWeight(inductance, 10e-6);

        assertTrue("Larger dt should give larger admittance", admittance2 > admittance1);
        assertEquals("Admittance should scale linearly with dt", admittance1 * 10, admittance2, TOLERANCE);
    }

    @Test
    public void testAdmittanceDecreasesWithLargerInductance() {
        double dt = 1e-6;

        double admittance1 = stamper.getAdmittanceWeight(1e-3, dt);
        double admittance2 = stamper.getAdmittanceWeight(10e-3, dt);

        assertTrue("Larger inductance should give smaller admittance", admittance2 < admittance1);
        assertEquals("Admittance should scale inversely with L", admittance1 / 10, admittance2, TOLERANCE);
    }
}
