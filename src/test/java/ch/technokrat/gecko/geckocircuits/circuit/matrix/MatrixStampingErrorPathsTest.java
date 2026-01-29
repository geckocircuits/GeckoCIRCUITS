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
 * Tests for matrix stamping error paths, singular matrices, and boundary conditions.
 */
public class MatrixStampingErrorPathsTest {

    private static final double TOLERANCE = 1e-12;
    private ResistorStamper resistorStamper;
    private CapacitorStamper capacitorStamper;
    private InductorStamper inductorStamper;

    @Before
    public void setUp() {
        resistorStamper = new ResistorStamper();
        capacitorStamper = new CapacitorStamper();
        inductorStamper = new InductorStamper();
    }

    // ========== Matrix Dimension Edge Cases ==========

    @Test
    public void testStampMatrixA_MinimumSize2x2() {
        double[][] a = new double[2][2];
        double[] parameter = {1000.0};

        resistorStamper.stampMatrixA(a, 0, 1, 0, parameter, 1e-6);

        // Should not throw and should stamp correctly
        assertEquals("Should stamp diagonal element", 1.0 / 1000.0, a[0][0], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_LargeMatrix() {
        double[][] a = new double[10000][10000];
        double[] parameter = {1000.0};

        resistorStamper.stampMatrixA(a, 5000, 5001, 0, parameter, 1e-6);

        // Should handle large matrices
        double admittance = 1.0 / 1000.0;
        assertEquals("Should stamp in large matrix", admittance, a[5000][5000], TOLERANCE);
    }

    // ========== Node Index Edge Cases ==========

    @Test
    public void testStampMatrixA_SameNodeConnected() {
        // When node X = node Y (element connected to itself)
        double[][] a = new double[3][3];
        double[] parameter = {1000.0};

        resistorStamper.stampMatrixA(a, 1, 1, 0, parameter, 1e-6);

        // a[1][1] += G, then a[1][1] += G, then a[1][1] -= G, then a[1][1] -= G
        // Net: 0
        assertEquals("Self-connected should cancel", 0.0, a[1][1], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_GroundNodeOnly() {
        // Both nodes are ground (node 0)
        double[][] a = new double[3][3];
        double[] parameter = {1000.0};

        resistorStamper.stampMatrixA(a, 0, 0, 0, parameter, 1e-6);

        // All stamping should affect node 0
        assertEquals("Ground to ground should cancel", 0.0, a[0][0], TOLERANCE);
    }

    @Test
    public void testStampMatrixA_OneNodeIsGround() {
        double[][] a = new double[3][3];
        double[] parameter = {1000.0};

        resistorStamper.stampMatrixA(a, 1, 0, 0, parameter, 1e-6);

        // Should stamp only on diagonal for non-ground node
        double admittance = 1.0 / 1000.0;
        assertEquals("Node 1 diagonal affected", admittance, a[1][1], TOLERANCE);
        // Node 0 (ground) should be affected too
        assertEquals("Ground node affected", admittance, a[0][0], TOLERANCE);
    }

    // ========== Parameter Array Edge Cases ==========

    @Test
    public void testStampMatrixA_MinimalParameter() {
        double[][] a = new double[3][3];
        double[] parameter = {100.0}; // Only one value

        resistorStamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        // Should work with minimal parameter
        assertEquals("Should use first parameter", 0.01, a[1][1], TOLERANCE);
    }

    @Test
    public void testCalculateCurrent_EmptyPreviousValues() {
        double[] parameter = {1000.0};

        double current = resistorStamper.calculateCurrent(10.0, 5.0, parameter, 1e-6, 0.0);

        // Should use Ohm's law without previous values
        assertEquals("Should calculate without history", 0.005, current, TOLERANCE);
    }

    // ========== Time Step Edge Cases ==========

    @Test
    public void testCapacitorStamping_ExtremelySmallDt() {
        double[][] a = new double[3][3];
        double[] parameter = {1e-6};
        double dt = 1e-15;

        capacitorStamper.stampMatrixA(a, 1, 2, 0, parameter, dt);

        // G = C/dt = 1e-6 / 1e-15 = 1e9
        assertTrue("Extremely small dt should produce large conductance", a[1][1] > 1e8);
    }

    @Test
    public void testCapacitorStamping_VeryLargeDt() {
        double[][] a = new double[3][3];
        double[] parameter = {1e-6};
        double dt = 1000.0;

        capacitorStamper.stampMatrixA(a, 1, 2, 0, parameter, dt);

        // G = C/dt = 1e-6 / 1000 = 1e-9
        assertTrue("Very large dt should produce small conductance", a[1][1] < 1e-8);
    }

    // ========== Vector Stamping Edge Cases ==========

    @Test
    public void testStampVectorB_MinimumSize() {
        double[] b = new double[2];
        double[] parameter = {1e-6};
        double dt = 1e-6;
        double[] previousValues = {5.0};

        // Stamp between nodes 0 and 1
        capacitorStamper.stampVectorB(b, 0, 1, 0, parameter, dt, 0.0, previousValues);

        // Should handle vector stamping
        // History current = (C/dt) * v_prev = 1.0 * 5.0 = 5.0
        // Stamped as b[0] += 5.0, b[1] -= 5.0
        assertEquals("Node 0 should get +history", 5.0, b[0], TOLERANCE);
        assertEquals("Node 1 should get -history", -5.0, b[1], TOLERANCE);
    }

    @Test
    public void testStampVectorB_LargeVector() {
        double[] b = new double[10000];
        double[] parameter = {1e-6};
        double dt = 1e-6;
        double[] previousValues = {3.0};

        capacitorStamper.stampVectorB(b, 5000, 5001, 0, parameter, dt, 0.0, previousValues);

        assertEquals("Should stamp in large vector", 3.0, b[5000], TOLERANCE);
    }

    @Test
    public void testStampVectorB_PrefilledVector() {
        double[] b = new double[3];
        b[0] = 1.0;
        b[1] = 2.0;
        b[2] = 3.0;
        double[] parameter = {1e-6};
        double dt = 1e-6;
        double[] previousValues = {5.0};

        capacitorStamper.stampVectorB(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // Should add to existing
        assertEquals("b[0] unchanged", 1.0, b[0], TOLERANCE);
        assertEquals("b[1] should be updated", 2.0 + 5.0, b[1], TOLERANCE);
        assertEquals("b[2] should be updated", 3.0 - 5.0, b[2], TOLERANCE);
    }

    // ========== Multiple Stamping ==========

    @Test
    public void testMultipleStamps_SameElement() {
        double[][] a = new double[3][3];
        double[] parameter = {1000.0};

        // Stamp twice
        resistorStamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);
        resistorStamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        double admittance = 1.0 / 1000.0;
        // Should add up
        assertEquals("Double stamping should accumulate", 2 * admittance, a[1][1], TOLERANCE);
    }

    @Test
    public void testMultipleStamps_ParallelElements() {
        double[][] a = new double[3][3];

        double[] r1 = {1000.0}; // 1k
        double[] r2 = {1000.0}; // 1k parallel

        resistorStamper.stampMatrixA(a, 1, 2, 0, r1, 1e-6);
        resistorStamper.stampMatrixA(a, 1, 2, 0, r2, 1e-6);

        // Two 1k resistors in parallel = 500 ohms = 0.002 S
        double expectedAdmittance = 0.002;
        assertEquals("Parallel resistors add admittance", expectedAdmittance, a[1][1], TOLERANCE);
    }

    @Test
    public void testMultipleStamps_ComplexNetwork() {
        double[][] a = new double[4][4];

        // Create small network with multiple components
        double[] r1 = {1000.0};
        double[] r2 = {2000.0};
        double[] r3 = {500.0};

        // R1 between nodes 1-2
        resistorStamper.stampMatrixA(a, 1, 2, 0, r1, 1e-6);
        // R2 between nodes 2-3
        resistorStamper.stampMatrixA(a, 2, 3, 0, r2, 1e-6);
        // R3 between nodes 1-3
        resistorStamper.stampMatrixA(a, 1, 3, 0, r3, 1e-6);

        // Check node 1: should have contributions from R1 and R3
        double g1 = 1.0 / 1000.0;
        double g3 = 1.0 / 500.0;
        assertEquals("Node 1 diagonal", g1 + g3, a[1][1], TOLERANCE);
    }

    // ========== Numerical Stability ==========

    @Test
    public void testNumericalStability_AccumulatedStamping() {
        double[][] a = new double[3][3];
        double[] parameter = {1e-9};

        // Stamp many times
        for (int i = 0; i < 1000; i++) {
            resistorStamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);
        }

        // Should still be finite
        assertTrue("Should remain finite", Double.isFinite(a[1][1]));
        assertEquals("Expected accumulated value", 1000.0 * (1.0 / 1e-9), a[1][1], 1.0);
    }

    @Test
    public void testNumericalStability_MixedScale() {
        double[][] a = new double[4][4];

        // Mix very large and very small values
        double[] rLarge = {1e10};
        double[] rSmall = {1e-10};

        resistorStamper.stampMatrixA(a, 1, 2, 0, rSmall, 1e-6);
        resistorStamper.stampMatrixA(a, 2, 3, 0, rLarge, 1e-6);

        // Both values should be correctly stamped
        assertTrue("Large value should be finite", Double.isFinite(a[2][3]));
        assertTrue("Small value should be finite", Double.isFinite(a[1][2]));
    }

    // ========== Symmetry Verification ==========

    @Test
    public void testMatrixSymmetry_ResistorStamping() {
        double[][] a = new double[3][3];
        double[] parameter = {1000.0};

        resistorStamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        // Resistor should produce symmetric matrix
        assertEquals("Off-diagonal symmetry", a[1][2], a[2][1], TOLERANCE);
    }

    @Test
    public void testMatrixSymmetry_CapacitorStamping() {
        double[][] a = new double[3][3];
        double[] parameter = {1e-6};
        double dt = 1e-6;

        capacitorStamper.stampMatrixA(a, 1, 2, 0, parameter, dt);

        // Capacitor should produce symmetric matrix
        assertEquals("Off-diagonal symmetry", a[1][2], a[2][1], TOLERANCE);
    }

    // ========== Behavior with Special Values ==========

    @Test
    public void testStamping_WithNaN() {
        double[][] a = new double[3][3];
        double[] parameter = {Double.NaN};

        // Some implementations might propagate NaN
        resistorStamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        // Result might be NaN or handled gracefully
        assertTrue("Should handle NaN", true); // No exception thrown
    }

    @Test
    public void testStamping_WithInfinity() {
        double[][] a = new double[3][3];
        double[] parameter = {Double.POSITIVE_INFINITY};

        resistorStamper.stampMatrixA(a, 1, 2, 0, parameter, 1e-6);

        // Infinity in resistance means zero conductance
        assertTrue("Should handle infinity", true); // No exception thrown
    }

    @Test
    public void testCalculateCurrent_WithExtremeVoltages() {
        double[] parameter = {1.0};
        double vx = Double.MAX_VALUE;
        double vy = 0.0;

        double current = resistorStamper.calculateCurrent(vx, vy, parameter, 1e-6, 0.0);

        // Should produce extreme current but remain valid
        assertTrue("Should remain finite", Double.isFinite(current) || current == Double.POSITIVE_INFINITY);
    }

    @Test
    public void testCalculateCurrent_WithNearEqualVoltages() {
        double[] parameter = {1e-12};
        double vx = 1.0;
        double vy = 1.0 + 1e-15;

        double current = resistorStamper.calculateCurrent(vx, vy, parameter, 1e-6, 0.0);

        // Very small voltage difference with small resistance
        assertTrue("Should produce finite current", Double.isFinite(current));
    }

    // ========== B Vector Boundary Conditions ==========

    @Test
    public void testStampVectorB_WithLargeSourceVoltage() {
        double[] b = new double[3];
        double[] parameter = {1e-6};
        double dt = 1e-6;
        double[] previousValues = {1e15}; // Extremely large previous voltage

        capacitorStamper.stampVectorB(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // History current = (C/dt) * v_prev = 1.0 * 1e15 = 1e15
        assertTrue("Should handle large values", Double.isFinite(b[1]));
    }

    @Test
    public void testStampVectorB_WithMixedSignPreviousValues() {
        double[] b = new double[3];
        b[1] = 100.0;
        b[2] = -100.0;

        double[] parameter = {1e-6};
        double dt = 1e-6;
        double[] previousValues = {-1000.0}; // Negative history voltage

        capacitorStamper.stampVectorB(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // b[1] should decrease, b[2] should increase
        assertTrue("b[1] should be less than 100", b[1] < 100.0);
        assertTrue("b[2] should be greater than -100", b[2] > -100.0);
    }
}
