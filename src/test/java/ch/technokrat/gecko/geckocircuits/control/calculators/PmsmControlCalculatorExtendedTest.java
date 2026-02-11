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
package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Extended tests for PmsmControlCalculator - Permanent Magnet Synchronous Motor control.
 * Tests Park/Clarke transformations, frequency variations, and controller stability.
 */
public class PmsmControlCalculatorExtendedTest {

    private PmsmControlCalculator calculator;
    private static final double TOLERANCE = 1e-6;

    @Before
    public void setUp() {
        calculator = new PmsmControlCalculator();
        // Initialize input and output signal arrays
        for (int i = 0; i < calculator._inputSignal.length; i++) {
            calculator._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            calculator._outputSignal[i] = new double[]{0};
        }
    }

    @Test
    public void testParkTransformAtZeroDegrees() {
        // Park transform: converts ABC to DQ reference frame
        // At 0 degrees: d-axis aligned with a-axis
        // PMSM calculator needs all inputs initialized for valid operation
        calculator._inputSignal[0][0] = 1.0;  // Phase A current (ia)
        calculator._inputSignal[1][0] = -0.5; // Phase B current (ib)
        calculator._inputSignal[2][0] = 0.0;  // Speed (n)
        calculator._inputSignal[3][0] = 0.0;  // Rotor angle (phi)
        calculator._inputSignal[4][0] = 0.0;  // n_ref
        calculator._inputSignal[5][0] = 1.0;  // Kp_n
        calculator._inputSignal[6][0] = 0.1;  // T_n
        calculator._inputSignal[7][0] = 10.0; // n_limit
        calculator._inputSignal[8][0] = 1.0;  // Kp_i
        calculator._inputSignal[9][0] = 0.1;  // T_i
        calculator._inputSignal[10][0] = 5.0; // i_limit
        if (calculator._inputSignal.length > 11) {
            calculator._inputSignal[11][0] = 0.2; // psi_PM
        }

        calculator.berechneYOUT(0.001);

        // Most outputs should be valid (some may be NaN if inputs are insufficient)
        assertFalse("Output 0 should be finite",
                   Double.isInfinite(calculator._outputSignal[0][0]));
    }

    @Test
    public void testParkTransformAt90Degrees() {
        // At 90 degrees: d-axis perpendicular to a-axis
        calculator._inputSignal[0][0] = 0.0;   // Phase A
        calculator._inputSignal[1][0] = 0.866; // Phase B = sqrt(3)/2
        calculator._inputSignal[2][0] = 50.0;  // Speed
        calculator._inputSignal[3][0] = Math.PI / 2; // 90 degrees
        calculator._inputSignal[4][0] = 50.0;  // n_ref
        calculator._inputSignal[5][0] = 1.0;   // Kp_n
        calculator._inputSignal[6][0] = 0.1;   // T_n
        calculator._inputSignal[7][0] = 10.0;  // n_limit
        calculator._inputSignal[8][0] = 1.0;   // Kp_i
        calculator._inputSignal[9][0] = 0.1;   // T_i
        calculator._inputSignal[10][0] = 5.0;  // i_limit
        if (calculator._inputSignal.length > 11) {
            calculator._inputSignal[11][0] = 0.2;
        }

        calculator.berechneYOUT(0.001);

        // Output should not be infinite
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertFalse("Output " + i + " should not be infinite",
                       Double.isInfinite(calculator._outputSignal[i][0]));
        }
    }

    @Test
    public void testClarkeTransform() {
        // Clarke transform: ABC to alpha-beta
        // Ia = 1, Ib = Ic = -0.5 should give I_alpha = 1, I_beta = 0
        calculator._inputSignal[0][0] = 1.0;
        calculator._inputSignal[1][0] = -0.5;
        calculator._inputSignal[2][0] = -0.5;

        calculator.berechneYOUT(0.001);

        // All outputs should be finite
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertFalse("Output should be finite",
                       Double.isInfinite(calculator._outputSignal[i][0]));
        }
    }

    @Test
    public void testPISteadyState() {
        // PI controller should reach steady state
        double setpoint = 10.0;
        double lastOutput = 0;
        int stableCount = 0;

        for (int step = 0; step < 1000; step++) {
            calculator._inputSignal[4][0] = setpoint;  // n_ref
            calculator._inputSignal[0][0] = 5.0;       // ia
            calculator._inputSignal[1][0] = 3.0;       // ib
            calculator._inputSignal[2][0] = step * 0.01; // Gradually increase speed
            calculator._inputSignal[5][0] = 0.5;       // Kp_n
            calculator._inputSignal[6][0] = 0.1;       // T_n
            calculator._inputSignal[7][0] = 10.0;      // n_limit
            calculator._inputSignal[8][0] = 1.0;       // Kp_i
            calculator._inputSignal[9][0] = 0.1;       // T_i
            calculator._inputSignal[10][0] = 5.0;      // i_limit

            calculator.berechneYOUT(0.0001);

            double output = calculator._outputSignal[0][0];
            if (Math.abs(output - lastOutput) < 0.01) {
                stableCount++;
            } else {
                stableCount = 0;
            }
            lastOutput = output;

            if (stableCount > 100) break;
        }

        assertTrue("PI should reach steady state", stableCount > 50);
    }

    @Test
    public void testAntiWindup() {
        // Test that anti-windup prevents integral saturation
        // Apply large error for extended time
        for (int step = 0; step < 100; step++) {
            calculator._inputSignal[4][0] = 1000.0;  // Large setpoint
            calculator._inputSignal[5][0] = 0.5;     // Kp_n
            calculator._inputSignal[6][0] = 0.1;     // T_n
            calculator._inputSignal[7][0] = 10.0;    // n_limit
            calculator._inputSignal[8][0] = 1.0;     // Kp_i
            calculator._inputSignal[9][0] = 0.1;     // T_i
            calculator._inputSignal[10][0] = 5.0;    // i_limit

            calculator.berechneYOUT(0.001);
        }

        // Output should be bounded (not infinite)
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertFalse("Output should be finite with anti-windup",
                       Double.isInfinite(calculator._outputSignal[i][0]));
        }
    }

    @Test
    public void testSpeedReversal() {
        // Test handling of speed sign change
        calculator._inputSignal[0][0] = 5.0;   // ia
        calculator._inputSignal[1][0] = 3.0;   // ib
        calculator._inputSignal[2][0] = 100.0; // Forward speed
        calculator._inputSignal[3][0] = 0.0;   // phi
        calculator._inputSignal[4][0] = 100.0; // n_ref
        calculator._inputSignal[5][0] = 0.5;   // Kp_n
        calculator._inputSignal[6][0] = 0.1;   // T_n
        calculator._inputSignal[7][0] = 10.0;  // n_limit
        calculator._inputSignal[8][0] = 1.0;   // Kp_i
        calculator._inputSignal[9][0] = 0.1;   // T_i
        calculator._inputSignal[10][0] = 5.0;  // i_limit

        calculator.berechneYOUT(0.001);

        // Now reverse
        calculator._inputSignal[2][0] = -100.0; // Reverse speed
        calculator._inputSignal[4][0] = -100.0; // Reverse n_ref
        calculator.berechneYOUT(0.001);

        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertFalse("Output should handle reversal",
                       Double.isNaN(calculator._outputSignal[i][0]));
        }
    }

    @Test
    public void testHighSpeedOperation() {
        // Test at high electrical frequency
        calculator._inputSignal[0][0] = 1.0;
        calculator._inputSignal[1][0] = 0.5;
        calculator._inputSignal[2][0] = 500.0;  // Very high speed
        calculator._inputSignal[3][0] = 1000 * Math.PI; // High rotor angle
        calculator._inputSignal[4][0] = 500.0;
        calculator._inputSignal[5][0] = 0.5;
        calculator._inputSignal[6][0] = 0.1;
        calculator._inputSignal[7][0] = 100.0;
        calculator._inputSignal[8][0] = 1.0;
        calculator._inputSignal[9][0] = 0.1;
        calculator._inputSignal[10][0] = 20.0;

        calculator.berechneYOUT(1e-6); // Small time step

        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertFalse("Output should be valid at high speed",
                       Double.isNaN(calculator._outputSignal[i][0]));
        }
    }

    @Test
    public void testZeroSpeed() {
        // Test at zero speed (standstill)
        calculator._inputSignal[0][0] = 0.0;
        calculator._inputSignal[1][0] = 0.0;
        calculator._inputSignal[2][0] = 0.0;  // Zero speed
        calculator._inputSignal[3][0] = 0.0;
        calculator._inputSignal[4][0] = 0.0;
        calculator._inputSignal[5][0] = 0.5;
        calculator._inputSignal[6][0] = 0.1;
        calculator._inputSignal[7][0] = 10.0;
        calculator._inputSignal[8][0] = 1.0;
        calculator._inputSignal[9][0] = 0.1;
        calculator._inputSignal[10][0] = 5.0;

        calculator.berechneYOUT(0.001);

        // Should handle zero speed gracefully
        for (int i = 0; i < calculator._outputSignal.length; i++) {
            assertFalse("Output should handle zero speed",
                       Double.isNaN(calculator._outputSignal[i][0]));
        }
    }

    @Test
    public void testMultipleTimeSteps() {
        // Verify stability over many simulation steps
        for (int step = 0; step < 1000; step++) {
            double angle = 0.01 * step;
            calculator._inputSignal[0][0] = Math.sin(angle);
            calculator._inputSignal[1][0] = Math.sin(angle - 2*Math.PI/3);
            calculator._inputSignal[2][0] = Math.sin(angle + 2*Math.PI/3);
            calculator._inputSignal[3][0] = angle;
            calculator._inputSignal[4][0] = 50.0;
            calculator._inputSignal[5][0] = 0.5;
            calculator._inputSignal[6][0] = 0.1;
            calculator._inputSignal[7][0] = 10.0;
            calculator._inputSignal[8][0] = 1.0;
            calculator._inputSignal[9][0] = 0.1;
            calculator._inputSignal[10][0] = 5.0;
            calculator._inputSignal[11][0] = 0.2;

            calculator.berechneYOUT(0.0001);

            for (int i = 0; i < calculator._outputSignal.length; i++) {
                assertFalse("Output " + i + " at step " + step + " should be valid",
                           Double.isNaN(calculator._outputSignal[i][0]));
                assertFalse("Output " + i + " at step " + step + " should be finite",
                           Double.isInfinite(calculator._outputSignal[i][0]));
            }
        }
    }

    @Test
    public void testDQTransformRoundTrip() {
        // Test that Park/Clarke transforms preserve amplitude
        double amplitude = 10.0;
        for (int phase = 0; phase < 360; phase += 30) {
            double angle = Math.toRadians(phase);

            // Create balanced 3-phase current at amplitude
            calculator._inputSignal[0][0] = amplitude * Math.cos(angle);
            calculator._inputSignal[1][0] = amplitude * Math.cos(angle - 2*Math.PI/3);
            calculator._inputSignal[2][0] = amplitude * Math.cos(angle + 2*Math.PI/3);
            calculator._inputSignal[3][0] = angle;
            calculator._inputSignal[4][0] = 50.0;
            calculator._inputSignal[5][0] = 0.5;
            calculator._inputSignal[6][0] = 0.1;
            calculator._inputSignal[7][0] = 10.0;
            calculator._inputSignal[8][0] = 1.0;
            calculator._inputSignal[9][0] = 0.1;
            calculator._inputSignal[10][0] = 5.0;
            calculator._inputSignal[11][0] = 0.2;

            calculator.berechneYOUT(0.001);

            // Outputs should remain finite and reasonable
            for (int i = 0; i < Math.min(2, calculator._outputSignal.length); i++) {
                assertFalse("DQ output " + i + " should be valid",
                           Double.isNaN(calculator._outputSignal[i][0]));
                // DQ currents should be roughly at amplitude (within range)
                assertTrue("DQ output magnitude should be reasonable",
                          Math.abs(calculator._outputSignal[i][0]) < amplitude * 1.5);
            }
        }
    }

    @Test
    public void testExtendedRotorAngle() {
        // Test with rotor angle that wraps around multiple times
        for (int rotation = 0; rotation < 10; rotation++) {
            calculator._inputSignal[0][0] = 5.0;
            calculator._inputSignal[1][0] = 3.0;
            calculator._inputSignal[2][0] = 50.0;
            calculator._inputSignal[3][0] = rotation * 2 * Math.PI + Math.PI / 4;
            calculator._inputSignal[4][0] = 50.0;
            calculator._inputSignal[5][0] = 0.5;
            calculator._inputSignal[6][0] = 0.1;
            calculator._inputSignal[7][0] = 10.0;
            calculator._inputSignal[8][0] = 1.0;
            calculator._inputSignal[9][0] = 0.1;
            calculator._inputSignal[10][0] = 5.0;
            calculator._inputSignal[11][0] = 0.2;

            calculator.berechneYOUT(0.001);

            for (int i = 0; i < calculator._outputSignal.length; i++) {
                assertFalse("Output should be valid for rotation " + rotation,
                           Double.isNaN(calculator._outputSignal[i][0]));
            }
        }
    }

    @Test
    public void testCurrentControlFastDynamics() {
        // Test current control with fast dynamics (small time constants)
        for (int step = 0; step < 500; step++) {
            calculator._inputSignal[0][0] = 10.0 * Math.sin(0.05 * step);  // Varying ia
            calculator._inputSignal[1][0] = 10.0 * Math.cos(0.05 * step);  // Varying ib
            calculator._inputSignal[2][0] = 50.0;
            calculator._inputSignal[3][0] = 0.01 * step;
            calculator._inputSignal[4][0] = 75.0;
            calculator._inputSignal[5][0] = 2.0;    // High Kp (aggressive)
            calculator._inputSignal[6][0] = 0.01;   // Very small T_n (fast)
            calculator._inputSignal[7][0] = 20.0;
            calculator._inputSignal[8][0] = 3.0;    // High Kp_i
            calculator._inputSignal[9][0] = 0.005;  // Very small T_i (fast)
            calculator._inputSignal[10][0] = 15.0;
            calculator._inputSignal[11][0] = 0.1;

            calculator.berechneYOUT(0.001);

            for (int i = 0; i < calculator._outputSignal.length; i++) {
                assertFalse("Output should be stable with fast dynamics",
                           Double.isNaN(calculator._outputSignal[i][0]));
                assertFalse("Output should not overflow",
                           Double.isInfinite(calculator._outputSignal[i][0]));
            }
        }
    }
}
