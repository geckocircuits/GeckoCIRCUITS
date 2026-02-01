package ch.technokrat.gecko.geckocircuits.control.calculators;

import ch.technokrat.gecko.geckocircuits.control.SSAShape;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for SmallSignalCalculator - Small Signal Analysis for Bode plots.
 * This calculator generates small signal perturbations and computes frequency responses.
 */
public class SmallSignalCalculatorTest {

    private static final double TOLERANCE = 1e-6;
    private static final double DELTA_T = 0.0001;

    @Test
    public void testConstructorSine() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0,    // amplitude
                10.0,   // freqLow
                1000.0, // freqHigh
                SSAShape.SINE,
                2,      // noInputs
                1,      // noOutput
                false   // addOutput
        );

        assertNotNull("Calculator should be created", calc);
        assertEquals("Should have correct input count", 2, calc._inputSignal.length);
        assertEquals("Should have correct output count", 1, calc._outputSignal.length);
    }

    @Test
    public void testConstructorRectangle() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                0.5,    // amplitude
                50.0,   // freqLow
                5000.0, // freqHigh
                SSAShape.RECTANGLE,
                2,      // noInputs
                1,      // noOutput
                false   // addOutput
        );

        assertNotNull("Calculator should be created", calc);
        assertNotNull("Signal calculator should be created for rectangle", calc._signalTypeCalculator);
    }

    @Test
    public void testConstructorTriangle() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                0.5,    // amplitude
                50.0,   // freqLow
                5000.0, // freqHigh
                SSAShape.TRIANGLE,
                2,      // noInputs
                1,      // noOutput
                false   // addOutput
        );

        assertNotNull("Calculator should be created", calc);
        assertNotNull("Signal calculator should be created for triangle", calc._signalTypeCalculator);
    }

    @Test
    public void testConstructorExternal() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                0.5,    // amplitude
                50.0,   // freqLow
                5000.0, // freqHigh
                SSAShape.EXTERNAL,
                3,      // noInputs (needs 3rd input for external signal)
                1,      // noOutput
                false   // addOutput
        );

        assertNotNull("Calculator should be created", calc);
    }

    @Test
    public void testInitializeAtSimulationStart() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 100.0, 1000.0, SSAShape.SINE, 2, 1, false);

        calc.initializeAtSimulationStart(DELTA_T);

        assertTrue("N should be positive", calc._N > 0);
        assertNotNull("Measured values should be initialized", calc._measuredValues);
        assertNotNull("Small signal values should be initialized", calc._smallSignalValues);
        assertEquals("Measured values array should match N", calc._N, calc._measuredValues.length);
        assertEquals("Small signal values array should match N", calc._N, calc._smallSignalValues.length);
    }

    @Test
    public void testInitWithNewDt() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 100.0, 1000.0, SSAShape.SINE, 2, 1, false);

        calc.initializeAtSimulationStart(DELTA_T);
        int originalN = calc._N;

        // Change to a different dt
        calc.initWithNewDt(DELTA_T / 2);

        // N should change with different dt
        assertNotEquals("N should change with different dt", originalN, calc._N);
    }

    @Test
    public void testBerechneYOUTSine() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 100.0, 1000.0, SSAShape.SINE, 2, 1, false);

        // Initialize input signals
        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calc._outputSignal.length; i++) {
            calc._outputSignal[i] = new double[]{0};
        }

        calc.initializeAtSimulationStart(DELTA_T);

        // Set input (measured signal)
        calc._inputSignal[0][0] = 5.0;

        calc.berechneYOUT(DELTA_T);

        // Output should be the small signal (not NaN/Infinite)
        assertFalse("Output should be valid", Double.isNaN(calc._outputSignal[0][0]));
        assertFalse("Output should not be infinite", Double.isInfinite(calc._outputSignal[0][0]));
    }

    @Test
    public void testBerechneYOUTWithAddOutput() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 100.0, 1000.0, SSAShape.SINE, 2, 1, true); // addOutput = true

        // Initialize input signals
        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calc._outputSignal.length; i++) {
            calc._outputSignal[i] = new double[]{0};
        }

        calc.initializeAtSimulationStart(DELTA_T);

        // Set inputs
        calc._inputSignal[0][0] = 5.0;  // measured signal
        calc._inputSignal[1][0] = 2.0;  // signal to add

        calc.berechneYOUT(DELTA_T);

        // Output should include the added signal
        assertFalse("Output should be valid", Double.isNaN(calc._outputSignal[0][0]));
    }

    @Test
    public void testBerechneYOUTRectangle() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 100.0, 1000.0, SSAShape.RECTANGLE, 2, 1, false);

        // Initialize input signals
        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calc._outputSignal.length; i++) {
            calc._outputSignal[i] = new double[]{0};
        }

        calc.initializeAtSimulationStart(DELTA_T);
        calc._inputSignal[0][0] = 5.0;

        calc.berechneYOUT(DELTA_T);

        assertFalse("Output should be valid", Double.isNaN(calc._outputSignal[0][0]));
    }

    @Test
    public void testBerechneYOUTTriangle() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 100.0, 1000.0, SSAShape.TRIANGLE, 2, 1, false);

        // Initialize input signals
        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calc._outputSignal.length; i++) {
            calc._outputSignal[i] = new double[]{0};
        }

        calc.initializeAtSimulationStart(DELTA_T);
        calc._inputSignal[0][0] = 5.0;

        calc.berechneYOUT(DELTA_T);

        assertFalse("Output should be valid", Double.isNaN(calc._outputSignal[0][0]));
    }

    @Test
    public void testBerechneYOUTExternal() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 100.0, 1000.0, SSAShape.EXTERNAL, 3, 1, false);

        // Initialize input signals
        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calc._outputSignal.length; i++) {
            calc._outputSignal[i] = new double[]{0};
        }

        calc.initializeAtSimulationStart(DELTA_T);
        calc._inputSignal[0][0] = 5.0;  // measured signal
        calc._inputSignal[2][0] = 0.1;  // external small signal

        calc.berechneYOUT(DELTA_T);

        assertFalse("Output should be valid", Double.isNaN(calc._outputSignal[0][0]));
    }

    @Test
    public void testCalculateSmallSignalSine() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 100.0, 1000.0, SSAShape.SINE, 2, 1, false);

        calc.initializeAtSimulationStart(DELTA_T);

        double result = calc.calculateSmallSignal(DELTA_T);

        assertFalse("Result should be valid", Double.isNaN(result));
        assertFalse("Result should not be infinite", Double.isInfinite(result));
    }

    @Test
    public void testCalculateSmallSignalRectangle() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 100.0, 1000.0, SSAShape.RECTANGLE, 2, 1, false);

        // Initialize signal calculator input/output
        for (int i = 0; i < calc._signalTypeCalculator._inputSignal.length; i++) {
            calc._signalTypeCalculator._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calc._signalTypeCalculator._outputSignal.length; i++) {
            calc._signalTypeCalculator._outputSignal[i] = new double[]{0};
        }

        calc.initializeAtSimulationStart(DELTA_T);

        double result = calc.calculateSmallSignal(DELTA_T);

        assertFalse("Result should be valid", Double.isNaN(result));
    }

    @Test
    public void testCalculateSmallSignalTriangle() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 100.0, 1000.0, SSAShape.TRIANGLE, 2, 1, false);

        // Initialize signal calculator input/output
        for (int i = 0; i < calc._signalTypeCalculator._inputSignal.length; i++) {
            calc._signalTypeCalculator._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calc._signalTypeCalculator._outputSignal.length; i++) {
            calc._signalTypeCalculator._outputSignal[i] = new double[]{0};
        }

        calc.initializeAtSimulationStart(DELTA_T);

        double result = calc.calculateSmallSignal(DELTA_T);

        assertFalse("Result should be valid", Double.isNaN(result));
    }

    @Test
    public void testCalculateSmallSignalExternal() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 100.0, 1000.0, SSAShape.EXTERNAL, 3, 1, false);

        // Initialize input signals
        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc._inputSignal[i] = new double[]{0};
        }

        calc._inputSignal[2][0] = 0.5;  // External signal
        calc.initializeAtSimulationStart(DELTA_T);

        double result = calc.calculateSmallSignal(DELTA_T);

        assertEquals("External signal should be returned", 0.5, result, TOLERANCE);
    }

    @Test
    public void testMultipleTimeSteps() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 100.0, 1000.0, SSAShape.SINE, 2, 1, false);

        // Initialize input signals
        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calc._outputSignal.length; i++) {
            calc._outputSignal[i] = new double[]{0};
        }

        calc.initializeAtSimulationStart(DELTA_T);

        // Run multiple steps
        for (int step = 0; step < 100; step++) {
            calc._inputSignal[0][0] = Math.sin(0.01 * step);
            calc.berechneYOUT(DELTA_T);

            assertFalse("Output should be valid at step " + step,
                       Double.isNaN(calc._outputSignal[0][0]));
        }
    }

    @Test
    public void testCircularBufferFilling() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 100.0, 1000.0, SSAShape.SINE, 2, 1, false);

        // Initialize input signals
        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calc._outputSignal.length; i++) {
            calc._outputSignal[i] = new double[]{0};
        }

        calc.initializeAtSimulationStart(DELTA_T);

        int initialIndex = calc.circularIndex;
        assertEquals("Initial index should be 0", 0, initialIndex);

        // Run a few steps
        for (int i = 0; i < 5; i++) {
            calc._inputSignal[0][0] = i * 0.1;
            calc.berechneYOUT(DELTA_T);
        }

        // Circular index should have advanced
        assertTrue("Circular index should have changed", calc.circularIndex != 0 || calc._N <= 5);
    }

    @Test
    public void testDtChange() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 100.0, 1000.0, SSAShape.SINE, 2, 1, false);

        // Initialize input signals
        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calc._outputSignal.length; i++) {
            calc._outputSignal[i] = new double[]{0};
        }

        calc.initializeAtSimulationStart(DELTA_T);

        // Run with one dt
        calc.berechneYOUT(DELTA_T);
        int samples1 = calc._numberSamples;

        // Run with same dt
        calc.berechneYOUT(DELTA_T);
        int samples2 = calc._numberSamples;

        assertEquals("Sample count should increment with same dt", samples1 + 1, samples2);

        // Run with different dt
        calc.berechneYOUT(DELTA_T * 2);
        int samples3 = calc._numberSamples;

        assertEquals("Sample count should reset with different dt", 1, samples3);
    }

    @Test
    public void testExternalSetTime() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 100.0, 1000.0, SSAShape.RECTANGLE, 2, 1, false);

        double testTime = 0.025;
        calc.externalSetTime(testTime);

        assertEquals("Time should be set correctly", testTime, calc._time, TOLERANCE);
        assertEquals("Signal calculator time should be set", testTime,
                    calc._signalTypeCalculator._time, TOLERANCE);
    }

    @Test
    public void testLowFrequencyRange() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 1.0, 10.0, SSAShape.SINE, 2, 1, false);

        assertNotNull("Should handle low frequency range", calc);

        // Initialize and run
        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calc._outputSignal.length; i++) {
            calc._outputSignal[i] = new double[]{0};
        }

        calc.initializeAtSimulationStart(0.001); // 1ms time step
        calc.berechneYOUT(0.001);

        assertFalse("Output should be valid", Double.isNaN(calc._outputSignal[0][0]));
    }

    @Test
    public void testHighFrequencyRange() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                0.1, 1000.0, 100000.0, SSAShape.SINE, 2, 1, false);

        assertNotNull("Should handle high frequency range", calc);

        // Initialize and run
        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calc._outputSignal.length; i++) {
            calc._outputSignal[i] = new double[]{0};
        }

        calc.initializeAtSimulationStart(1e-6); // 1us time step
        calc.berechneYOUT(1e-6);

        assertFalse("Output should be valid", Double.isNaN(calc._outputSignal[0][0]));
    }

    @Test
    public void testSmallAmplitude() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                0.001, 100.0, 1000.0, SSAShape.SINE, 2, 1, false);

        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calc._outputSignal.length; i++) {
            calc._outputSignal[i] = new double[]{0};
        }

        calc.initializeAtSimulationStart(DELTA_T);
        calc.berechneYOUT(DELTA_T);

        // Output should be small but valid
        assertFalse("Output should be valid", Double.isNaN(calc._outputSignal[0][0]));
        assertTrue("Output magnitude should be small",
                  Math.abs(calc._outputSignal[0][0]) < 10);
    }

    @Test
    public void testLargeAmplitude() {
        SmallSignalCalculator calc = new SmallSignalCalculator(
                100.0, 100.0, 1000.0, SSAShape.SINE, 2, 1, false);

        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calc._outputSignal.length; i++) {
            calc._outputSignal[i] = new double[]{0};
        }

        calc.initializeAtSimulationStart(DELTA_T);
        calc.berechneYOUT(DELTA_T);

        assertFalse("Output should be valid", Double.isNaN(calc._outputSignal[0][0]));
        assertFalse("Output should not be infinite", Double.isInfinite(calc._outputSignal[0][0]));
    }

    @Test
    public void testBodeArrayInitialization() {
        // _bode array should be initialized on instance
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 100.0, 1000.0, SSAShape.SINE, 2, 1, false);

        assertNotNull("Bode array should be initialized", calc._bode);
        assertEquals("Bode array should have 3 elements", 3, calc._bode.length);
        assertNotNull("Bode frequency array should be set", calc._bode[0]);
    }

    @Test
    public void testMinimumNValue() {
        // With very small base frequency and large dt, N might be calculated as very small
        SmallSignalCalculator calc = new SmallSignalCalculator(
                1.0, 10000.0, 100000.0, SSAShape.SINE, 2, 1, false);

        for (int i = 0; i < calc._inputSignal.length; i++) {
            calc._inputSignal[i] = new double[]{0};
        }
        for (int i = 0; i < calc._outputSignal.length; i++) {
            calc._outputSignal[i] = new double[]{0};
        }

        // Large dt relative to frequency -> small N, but should be at least 2
        calc.initializeAtSimulationStart(0.01); // 10ms

        assertTrue("N should be at least 2", calc._N >= 2);
    }
}
