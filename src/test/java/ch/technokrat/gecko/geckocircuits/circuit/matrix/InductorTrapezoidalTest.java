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
 * Tests for InductorStamper trapezoidal integration methods and edge cases.
 */
public class InductorTrapezoidalTest {

    private static final double TOLERANCE = 1e-12;
    private InductorStamper stamper;

    @Before
    public void setUp() {
        stamper = new InductorStamper();
    }

    // ========== Trapezoidal Admittance Weight ==========

    @Test
    public void testGetAdmittanceWeightTrapezoidal_Basic() {
        double inductance = 1e-3; // 1 mH
        double dt = 1e-6; // 1 us

        double admittance = stamper.getAdmittanceWeightTrapezoidal(inductance, dt);

        // Trapezoidal: G = dt/(2*L) = 1e-6 / (2*1e-3) = 5e-4
        assertEquals("Trapezoidal inductor admittance", 5e-4, admittance, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeightTrapezoidal_TwiceBackwardEuler() {
        double inductance = 1e-3;
        double dt = 1e-6;

        double admittanceBE = stamper.getAdmittanceWeight(inductance, dt);
        double admittanceTRZ = stamper.getAdmittanceWeightTrapezoidal(inductance, dt);

        // Trapezoidal should be exactly half of BE (dt/(2L) vs dt/L)
        assertEquals("TRZ should be half BE", admittanceBE * 0.5, admittanceTRZ, TOLERANCE);
    }

    @Test
    public void testGetAdmittanceWeightTrapezoidal_VerySmallInductance() {
        double inductance = 1e-20;
        double dt = 1e-6;

        double admittance = stamper.getAdmittanceWeightTrapezoidal(inductance, dt);

        // Should clamp to MIN_INDUCTANCE = 1e-15
        double expected = 1e-6 / (2 * 1e-15); // dt / (2 * L_min)
        assertEquals("Should use clamped minimum", expected, admittance, 1.0);
    }

    @Test
    public void testGetAdmittanceWeightTrapezoidal_NegativeInductance() {
        double inductance = -1e-3;
        double dt = 1e-6;

        double admittance = stamper.getAdmittanceWeightTrapezoidal(inductance, dt);

        // Should clamp to MIN_INDUCTANCE
        double expected = 1e-6 / (2 * 1e-15);
        assertEquals("Negative inductance should clamp", expected, admittance, 1.0);
    }

    @Test
    public void testGetAdmittanceWeightTrapezoidal_VeryLargeInductance() {
        double inductance = 1e6;
        double dt = 1e-6;

        double admittance = stamper.getAdmittanceWeightTrapezoidal(inductance, dt);

        // G = dt/(2*L) = 1e-6 / (2*1e6) = 5e-13
        assertEquals("Very large inductance gives small admittance", 5e-13, admittance, 1e-20);
    }

    @Test
    public void testGetAdmittanceWeightTrapezoidal_VerySmallDt() {
        double inductance = 1e-3;
        double dt = 1e-15;

        double admittance = stamper.getAdmittanceWeightTrapezoidal(inductance, dt);

        // G = dt/(2*L) = 1e-15 / (2*1e-3) = 5e-13
        assertEquals("Very small dt gives small admittance", 5e-13, admittance, 1e-20);
    }

    @Test
    public void testGetAdmittanceWeightTrapezoidal_VeryLargeDt() {
        double inductance = 1e-3;
        double dt = 1.0;

        double admittance = stamper.getAdmittanceWeightTrapezoidal(inductance, dt);

        // G = dt/(2*L) = 1 / (2*1e-3) = 500
        assertEquals("Very large dt gives large admittance", 500.0, admittance, 0.1);
    }

    // ========== Trapezoidal B Vector Stamping ==========

    @Test
    public void testStampVectorBTrapezoidal_WithHistoryCurrentAndVoltage() {
        double[] b = new double[3];
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double[] previousValues = {100.0, 5.0}; // i_prev, v_prev

        stamper.stampVectorBTrapezoidal(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // I_hist = i_prev + (dt/2L) * v_prev = 100 + (1e-6/2e-3) * 5 = 100 + 0.0025
        double expected = 100.0 + 0.0025;
        assertEquals("B[1] should have history current + voltage term", expected, b[1], TOLERANCE);
        assertEquals("B[2] should be negative", -expected, b[2], TOLERANCE);
    }

    @Test
    public void testStampVectorBTrapezoidal_NullPreviousValues() {
        double[] b = new double[3];
        double[] parameter = {1e-3};
        double dt = 1e-6;

        stamper.stampVectorBTrapezoidal(b, 1, 2, 0, parameter, dt, 0.0, null);

        // With null previousValues, should assume 0 for both
        assertEquals("B[1] should be 0", 0.0, b[1], TOLERANCE);
        assertEquals("B[2] should be 0", 0.0, b[2], TOLERANCE);
    }

    @Test
    public void testStampVectorBTrapezoidal_EmptyPreviousValues() {
        double[] b = new double[3];
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double[] previousValues = {}; // Empty array

        stamper.stampVectorBTrapezoidal(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // Should default to 0
        assertEquals("B[1] should be 0 with empty array", 0.0, b[1], TOLERANCE);
    }

    @Test
    public void testStampVectorBTrapezoidal_OnlyCurrentInHistory() {
        double[] b = new double[3];
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double[] previousValues = {100.0}; // Only i_prev, no v_prev

        stamper.stampVectorBTrapezoidal(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // Should use only current, voltage defaults to 0
        assertEquals("B[1] should have only history current", 100.0, b[1], TOLERANCE);
    }

    @Test
    public void testStampVectorBTrapezoidal_WithLargeHistoryCurrent() {
        double[] b = new double[3];
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double[] previousValues = {1e9, 0.0}; // Large current

        stamper.stampVectorBTrapezoidal(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        assertEquals("Should handle large history current", 1e9, b[1], 1.0);
    }

    @Test
    public void testStampVectorBTrapezoidal_WithNegativeHistoryCurrent() {
        double[] b = new double[3];
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double[] previousValues = {-1e6, 0.0}; // Negative current

        stamper.stampVectorBTrapezoidal(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        assertEquals("Should preserve negative current", -1e6, b[1], 1.0);
    }

    @Test
    public void testStampVectorBTrapezoidal_WithLargePreviousVoltage() {
        double[] b = new double[3];
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double[] previousValues = {0.0, 1e8}; // Large voltage

        stamper.stampVectorBTrapezoidal(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // Voltage term = (dt/2L) * v_prev = (1e-6/(2*1e-3)) * 1e8 = 0.5e-3 * 1e8 = 50000
        double expected = 50000.0;
        assertEquals("Should handle large voltage term", expected, b[1], 100.0);
    }

    @Test
    public void testStampVectorBTrapezoidal_WithNegativePreviousVoltage() {
        double[] b = new double[3];
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double[] previousValues = {0.0, -1e6}; // Negative voltage

        stamper.stampVectorBTrapezoidal(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // Voltage term = (dt/2L) * v_prev = (1e-6/(2*1e-3)) * (-1e6) = 0.5e-3 * (-1e6) = -500
        double expected = -500.0;
        assertEquals("Should preserve negative voltage term", expected, b[1], 1.0);
    }

    @Test
    public void testStampVectorBTrapezoidal_VerySmallInductance() {
        double[] b = new double[3];
        double[] parameter = {1e-20}; // Will clamp to 1e-15
        double dt = 1e-6;
        double[] previousValues = {100.0, 10.0};

        stamper.stampVectorBTrapezoidal(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // Should use clamped inductance
        // Voltage term = (dt/2L_min) * v_prev = (1e-6/(2*1e-15)) * 10 = 5e8 * 10 = 5e9
        double expected = 100.0 + 5e9;
        assertEquals("Should use clamped inductance", expected, b[1], 1e8);
    }

    @Test
    public void testStampVectorBTrapezoidal_HighNodeIndices() {
        double[] b = new double[1000];
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double[] previousValues = {50.0, 2.0};

        stamper.stampVectorBTrapezoidal(b, 500, 501, 0, parameter, dt, 0.0, previousValues);

        double expected = 50.0 + 0.001; // i_prev + voltage term
        assertEquals("Should work at high indices", expected, b[500], TOLERANCE);
        assertEquals("Negative at adjacent index", -expected, b[501], TOLERANCE);
    }

    @Test
    public void testStampVectorBTrapezoidal_VersusBackwardEuler() {
        double[] bBE = new double[3];
        double[] bTRZ = new double[3];
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double[] previousValues = {100.0, 5.0};

        stamper.stampVectorB(bBE, 1, 2, 0, parameter, dt, 0.0, previousValues);
        stamper.stampVectorBTrapezoidal(bTRZ, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // BE only has history current: 100
        assertEquals("BE should have only i_prev", 100.0, bBE[1], TOLERANCE);

        // TRZ has history current + voltage term
        double expected = 100.0 + 0.0025;
        assertEquals("TRZ should have current + voltage", expected, bTRZ[1], TOLERANCE);
    }

    // ========== Cross-Method Consistency ==========

    @Test
    public void testConsistency_BackwardEulerVsTrapezoidal_InductorConductance() {
        double inductance = 1e-3;
        double dt = 1e-6;

        double beAdmittance = stamper.getAdmittanceWeight(inductance, dt);
        double trzAdmittance = stamper.getAdmittanceWeightTrapezoidal(inductance, dt);

        // Trapezoidal should be exactly half of BE
        assertEquals("Trapezoidal should be half of BE", beAdmittance / 2, trzAdmittance, TOLERANCE);
    }

    @Test
    public void testConsistency_AdmittanceWeightWithHistoryVector() {
        double[] b = new double[3];
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double[] previousValues = {1e6, 10.0};

        double admittance = stamper.getAdmittanceWeightTrapezoidal(parameter[0], dt);

        stamper.stampVectorBTrapezoidal(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // The voltage term should equal admittance * v_prev
        double voltageTermExpected = admittance * previousValues[1];
        double volumeTerm = b[1] - previousValues[0];

        assertEquals("Voltage term should match admittance * v_prev", voltageTermExpected, volumeTerm, 1e-3);
    }

    @Test
    public void testEdgeCase_TrapezoidalWithZeroPreviousValues() {
        double[] b = new double[3];
        b[1] = 1.0; // Pre-existing value
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double[] previousValues = {0.0, 0.0}; // Zero history

        stamper.stampVectorBTrapezoidal(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // Should add 0 to existing value
        assertEquals("Should add zero history", 1.0, b[1], TOLERANCE);
    }

    @Test
    public void testEdgeCase_TrapezoidalAddsToExistingB() {
        double[] b = new double[3];
        b[1] = 500.0; // Pre-existing value
        double[] parameter = {1e-3};
        double dt = 1e-6;
        double[] previousValues = {100.0, 5.0};

        stamper.stampVectorBTrapezoidal(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // Should add to existing
        double expected = 500.0 + 100.0 + 0.0025;
        assertEquals("Should add to existing b value", expected, b[1], TOLERANCE);
    }

    @Test
    public void testNumericalStability_TrapezoidalWithMixedScales() {
        double[] b = new double[3];
        double[] parameter = {1e-12}; // Very small inductance
        double dt = 1e-9; // Small time step
        double[] previousValues = {1e-3, 1e-3}; // Small values

        stamper.stampVectorBTrapezoidal(b, 1, 2, 0, parameter, dt, 0.0, previousValues);

        // Should produce numerically stable result
        assertTrue("Result should be finite", Double.isFinite(b[1]));
        assertTrue("Result should be finite", Double.isFinite(b[2]));
    }
}
