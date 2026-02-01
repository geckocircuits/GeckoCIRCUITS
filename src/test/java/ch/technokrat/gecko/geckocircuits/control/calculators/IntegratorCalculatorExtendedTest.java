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
 * Extended tests for integrator calculator with various conditions.
 * Tests integration accuracy, state consistency, and boundary conditions.
 */
public class IntegratorCalculatorExtendedTest {

    private IntegratorCalculation calculator;
    private static final double DT = 0.0001;

    @Before
    public void setUp() {
        calculator = new IntegratorCalculation(1.0, 0.0, -1000, 1000);
        calculator._inputSignal[0] = new double[]{0};
        calculator._inputSignal[1] = new double[]{0};
        calculator._outputSignal[0] = new double[]{0};
    }

    @Test
    public void testIntegrationOfConstant() {
        // Integrate constant 1.0 over time -> should accumulate linearly
        calculator.initializeAtSimulationStart(DT);
        double expectedSum = 0.0;

        for (int i = 0; i < 100; i++) {
            calculator._inputSignal[0][0] = 1.0;
            expectedSum += 1.0 * DT;
            calculator.berechneYOUT(DT);
        }

        // Result should be close to expected integral
        assertEquals("Constant integration", expectedSum, calculator._outputSignal[0][0], 1e-4);
    }

    @Test
    public void testIntegrationOfSine() {
        // Integrate sin over full period -> should be ~0
        calculator.initializeAtSimulationStart(DT);
        double period = 2 * Math.PI;
        int steps = (int)(period / DT);

        for (int i = 0; i < steps; i++) {
            calculator._inputSignal[0][0] = Math.sin(i * DT);
            calculator.berechneYOUT(DT);
        }

        // Result should be close to zero
        assertEquals("Sine integration over period", 0.0, calculator._outputSignal[0][0], 0.05);
    }

    @Test
    public void testIntegrationOfCosine() {
        // Integrate cos -> result should oscillate
        calculator.initializeAtSimulationStart(DT);
        double maxValue = 0.0;
        double minValue = 0.0;

        for (int i = 0; i < 10000; i++) {
            calculator._inputSignal[0][0] = Math.cos(i * DT);
            calculator.berechneYOUT(DT);
            maxValue = Math.max(maxValue, calculator._outputSignal[0][0]);
            minValue = Math.min(minValue, calculator._outputSignal[0][0]);
        }

        // Should see oscillations
        assertTrue("Cosine integration should oscillate", Math.abs(maxValue - minValue) > 0.5);
    }

    @Test
    public void testIntegrationReset() {
        // Test that reset signal clears integrator
        calculator.initializeAtSimulationStart(DT);

        // Integrate for a while
        for (int i = 0; i < 100; i++) {
            calculator._inputSignal[0][0] = 5.0;
            calculator.berechneYOUT(DT);
        }

        double valueBeforeReset = calculator._outputSignal[0][0];
        assertTrue("Should accumulate before reset", valueBeforeReset > 0);

        // Now apply reset signal
        calculator._inputSignal[1][0] = 1.0;
        calculator.berechneYOUT(DT);

        // After reset, output should return to initial value
        assertEquals("Should reset to initial value", 0.0, calculator._outputSignal[0][0], 1e-6);
    }

    @Test
    public void testSmallTimeStep() {
        calculator.initializeAtSimulationStart(1e-9);
        calculator._inputSignal[0][0] = 100.0;
        calculator.berechneYOUT(1e-9);

        assertFalse("Should handle very small time steps",
                   Double.isNaN(calculator._outputSignal[0][0]));
    }

    @Test
    public void testLargeTimeStep() {
        calculator.initializeAtSimulationStart(100.0);
        calculator._inputSignal[0][0] = 1.0;
        calculator.berechneYOUT(100.0);

        assertFalse("Should handle large time steps",
                   Double.isNaN(calculator._outputSignal[0][0]));
    }

    @Test
    public void testNegativeInput() {
        calculator.initializeAtSimulationStart(DT);
        double expectedSum = 0.0;

        for (int i = 0; i < 100; i++) {
            calculator._inputSignal[0][0] = -1.0;
            expectedSum -= 1.0 * DT;
            calculator.berechneYOUT(DT);
        }

        assertEquals("Negative integration", expectedSum, calculator._outputSignal[0][0], 1e-4);
    }

    @Test
    public void testSaturationAtMaxLimit() {
        // Test upper saturation limit
        calculator = new IntegratorCalculation(10.0, 0.0, -100, 100);
        calculator._inputSignal[0] = new double[]{0};
        calculator._inputSignal[1] = new double[]{0};
        calculator.initializeAtSimulationStart(0.01);

        // Integrate with large gain to quickly reach limit
        for (int i = 0; i < 200; i++) {
            calculator._inputSignal[0][0] = 100.0;  // Large positive input
            calculator.berechneYOUT(0.01);

            if (calculator._outputSignal[0][0] >= 99.9) {
                break;  // Reached saturation
            }
        }

        // Output should not exceed max limit
        assertTrue("Should not exceed max limit", calculator._outputSignal[0][0] <= 100.0);
    }

    @Test
    public void testSaturationAtMinLimit() {
        // Test lower saturation limit
        calculator = new IntegratorCalculation(10.0, 0.0, -100, 100);
        calculator._inputSignal[0] = new double[]{0};
        calculator._inputSignal[1] = new double[]{0};
        calculator.initializeAtSimulationStart(0.01);

        // Integrate with large gain to quickly reach negative limit
        for (int i = 0; i < 200; i++) {
            calculator._inputSignal[0][0] = -100.0;  // Large negative input
            calculator.berechneYOUT(0.01);

            if (calculator._outputSignal[0][0] <= -99.9) {
                break;  // Reached saturation
            }
        }

        // Output should not go below min limit
        assertTrue("Should not go below min limit", calculator._outputSignal[0][0] >= -100.0);
    }

    @Test
    public void testInitialValue() {
        // Test integration with non-zero initial value
        double initValue = 50.0;
        calculator = new IntegratorCalculation(1.0, initValue, -1000, 1000);
        calculator._inputSignal[0] = new double[]{0};
        calculator._inputSignal[1] = new double[]{0};
        calculator.initializeAtSimulationStart(DT);

        // First output should be initial value
        calculator.berechneYOUT(DT);
        assertEquals("Should start at initial value", initValue, calculator._outputSignal[0][0], 1e-6);

        // Then accumulate
        double expectedSum = initValue;
        for (int i = 0; i < 100; i++) {
            calculator._inputSignal[0][0] = 1.0;
            expectedSum += 1.0 * DT;
            calculator.berechneYOUT(DT);
        }

        assertEquals("Should accumulate from initial value", expectedSum, calculator._outputSignal[0][0], 1e-4);
    }

    @Test
    public void testGainFactor() {
        // Test with different integration gains
        for (double gain : new double[]{0.5, 1.0, 2.0, 5.0}) {
            calculator = new IntegratorCalculation(gain, 0.0, -10000, 10000);
            calculator._inputSignal[0] = new double[]{0};
            calculator._inputSignal[1] = new double[]{0};
            calculator.initializeAtSimulationStart(DT);

            for (int i = 0; i < 100; i++) {
                calculator._inputSignal[0][0] = 1.0;
                calculator.berechneYOUT(DT);
            }

            double result = calculator._outputSignal[0][0];
            double expected = gain * 100 * DT;
            assertEquals("Should scale with gain " + gain, expected, result, expected * 0.05 + 1e-6);
        }
    }

    @Test
    public void testAlternatingInput() {
        // Test with alternating positive/negative input
        calculator.initializeAtSimulationStart(DT);
        double expectedSum = 0.0;

        for (int i = 0; i < 200; i++) {
            double input = (i % 2 == 0) ? 1.0 : -1.0;
            calculator._inputSignal[0][0] = input;
            expectedSum += input * DT;
            calculator.berechneYOUT(DT);
        }

        // Result should be close to zero (alternating cancels out)
        assertEquals("Alternating input should cancel", expectedSum, calculator._outputSignal[0][0], 1e-4);
    }

    @Test
    public void testRampInput() {
        // Test with linearly increasing input (ramp)
        calculator.initializeAtSimulationStart(DT);

        for (int i = 0; i < 1000; i++) {
            calculator._inputSignal[0][0] = i * 0.001;  // Increasing ramp
            calculator.berechneYOUT(DT);
        }

        // Integral of ramp is parabolic growth
        assertFalse("Should handle ramp input", Double.isNaN(calculator._outputSignal[0][0]));
        assertTrue("Ramp integration should grow", calculator._outputSignal[0][0] > 0);
    }

    @Test
    public void testImpulseResponse() {
        // Test response to single impulse (large spike)
        calculator.initializeAtSimulationStart(DT);
        calculator._inputSignal[0][0] = 0.0;
        calculator.berechneYOUT(DT);

        double beforeImpulse = calculator._outputSignal[0][0];

        // Apply large impulse
        calculator._inputSignal[0][0] = 1000.0;
        calculator.berechneYOUT(DT);
        double afterImpulse = calculator._outputSignal[0][0];

        // Should accumulate the impulse
        assertTrue("Should accumulate impulse", afterImpulse > beforeImpulse);

        // Continue with zero input - trapezoidal integration uses average of
        // current and previous input, so one more step with 0 input still adds
        // half the previous impulse value. After that, output should stabilize.
        calculator._inputSignal[0][0] = 0.0;
        calculator.berechneYOUT(DT);
        double afterZero1 = calculator._outputSignal[0][0];

        // Second zero step should not change output further
        calculator.berechneYOUT(DT);
        double afterZero2 = calculator._outputSignal[0][0];

        assertEquals("Should stabilize after impulse", afterZero1, afterZero2, 1e-6);
    }

    @Test
    public void testStepResponse() {
        // Test response to step input
        calculator.initializeAtSimulationStart(DT);

        // First half: zero input
        for (int i = 0; i < 500; i++) {
            calculator._inputSignal[0][0] = 0.0;
            calculator.berechneYOUT(DT);
        }
        double atStep = calculator._outputSignal[0][0];

        // Second half: unit step
        for (int i = 0; i < 500; i++) {
            calculator._inputSignal[0][0] = 1.0;
            calculator.berechneYOUT(DT);
        }
        double afterStep = calculator._outputSignal[0][0];

        // Output should grow linearly with unit step
        assertTrue("Should respond to step", afterStep > atStep);
        assertTrue("Step response should be linear", (afterStep - atStep) > 0.04);
    }

    @Test
    public void testNumericalAccuracy() {
        // Test numerical accuracy with many small steps
        calculator = new IntegratorCalculation(1.0, 0.0, -10000, 10000);
        calculator._inputSignal[0] = new double[]{0};
        calculator._inputSignal[1] = new double[]{0};
        calculator.initializeAtSimulationStart(1e-6);

        double target = 1.0;
        for (int i = 0; i < 1000000; i++) {
            calculator._inputSignal[0][0] = 1.0;
            calculator.berechneYOUT(1e-6);
        }

        double result = calculator._outputSignal[0][0];
        // With 1e-6 steps, accumulating 1.0 for 1M steps should give ~1.0
        assertEquals("Numerical accuracy test", target, result, 0.01);
    }

    @Test
    public void testZeroGain() {
        // Test with zero gain (should accumulate nothing)
        calculator = new IntegratorCalculation(0.0, 0.0, -1000, 1000);
        calculator._inputSignal[0] = new double[]{0};
        calculator._inputSignal[1] = new double[]{0};
        calculator.initializeAtSimulationStart(DT);

        for (int i = 0; i < 100; i++) {
            calculator._inputSignal[0][0] = 1000.0;  // Large input
            calculator.berechneYOUT(DT);
        }

        // With zero gain, output should remain at initial value
        assertEquals("Zero gain should not accumulate", 0.0, calculator._outputSignal[0][0], 1e-6);
    }

    @Test
    public void testVeryNarrowLimits() {
        // Test with very tight saturation limits
        calculator = new IntegratorCalculation(1.0, 0.0, -0.1, 0.1);
        calculator._inputSignal[0] = new double[]{0};
        calculator._inputSignal[1] = new double[]{0};
        calculator.initializeAtSimulationStart(0.01);

        for (int i = 0; i < 50; i++) {
            calculator._inputSignal[0][0] = 1.0;
            calculator.berechneYOUT(0.01);
        }

        // Output should be saturated at limit
        assertTrue("Should saturate at narrow limit", calculator._outputSignal[0][0] <= 0.1);
        assertTrue("Should not go negative", calculator._outputSignal[0][0] >= -0.1);
    }
}
