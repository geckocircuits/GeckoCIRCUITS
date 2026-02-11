package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for AbstractControlCalculatable base class.
 */
public class AbstractControlCalculatableTest {

    private static final double TOLERANCE = 1e-10;

    // Concrete implementation for testing abstract class
    private static class TestCalculator extends AbstractControlCalculatable {
        public TestCalculator(int noInputs, int noOutputs) {
            super(noInputs, noOutputs);
        }

        @Override
        public void berechneYOUT(double deltaT) {
            // Simple pass-through: output[0] = input[0]
            if (_inputSignal[0] != null) {
                _outputSignal[0][0] = _inputSignal[0][0];
            }
        }
    }

    @Before
    public void setUp() {
        // Reset static time before each test
        AbstractControlCalculatable.setTime(0);
    }

    @Test
    public void testConstructorInitializesArrays() {
        TestCalculator calc = new TestCalculator(3, 2);

        assertEquals("Should have 3 inputs", 3, calc._inputSignal.length);
        assertEquals("Should have 2 outputs", 2, calc._outputSignal.length);

        // Input signals should be null initially (set when connected)
        for (int i = 0; i < 3; i++) {
            assertNull("Input " + i + " should be null initially", calc._inputSignal[i]);
        }

        // Output signals should be initialized with arrays
        for (int i = 0; i < 2; i++) {
            assertNotNull("Output " + i + " should be initialized", calc._outputSignal[i]);
            assertEquals("Output " + i + " should have length 1", 1, calc._outputSignal[i].length);
        }
    }

    @Test
    public void testSetTime() {
        AbstractControlCalculatable.setTime(5.0);
        assertEquals("Time should be set", 5.0, AbstractControlCalculatable._time, TOLERANCE);

        AbstractControlCalculatable.setTime(-1.5);
        assertEquals("Time should handle negative", -1.5, AbstractControlCalculatable._time, TOLERANCE);

        AbstractControlCalculatable.setTime(0);
        assertEquals("Time should reset to zero", 0, AbstractControlCalculatable._time, TOLERANCE);
    }

    @Test
    public void testSetInputSignalSuccess() throws Exception {
        TestCalculator source = new TestCalculator(0, 1);
        TestCalculator dest = new TestCalculator(1, 1);

        // Set output value on source
        source._outputSignal[0][0] = 42.0;

        // Connect source output to dest input
        dest.setInputSignal(0, source, 0);

        // Now dest's input should point to source's output
        assertSame("Input should reference source output",
                  source._outputSignal[0], dest._inputSignal[0]);
        assertEquals("Value should be accessible", 42.0, dest._inputSignal[0][0], TOLERANCE);
    }

    @Test(expected = Exception.class)
    public void testSetInputSignalAlreadyConnected() throws Exception {
        TestCalculator source1 = new TestCalculator(0, 1);
        TestCalculator source2 = new TestCalculator(0, 1);
        TestCalculator dest = new TestCalculator(1, 1);

        // First connection should succeed
        dest.setInputSignal(0, source1, 0);

        // Second connection to same input should throw exception
        dest.setInputSignal(0, source2, 0);
    }

    @Test
    public void testCheckInputWithoutConnectionAndFillNull() {
        TestCalculator calc = new TestCalculator(2, 1);

        // Input is null, should fill and return true
        boolean result = calc.checkInputWithoutConnectionAndFill(0);

        assertTrue("Should return true for null input", result);
        assertNotNull("Input should now be filled", calc._inputSignal[0]);
        assertEquals("Filled input should have length 1", 1, calc._inputSignal[0].length);
    }

    @Test
    public void testCheckInputWithoutConnectionAndFillAlreadyConnected() throws Exception {
        TestCalculator source = new TestCalculator(0, 1);
        TestCalculator dest = new TestCalculator(1, 1);

        // Connect first
        dest.setInputSignal(0, source, 0);

        // Now check - should return false since already connected
        boolean result = dest.checkInputWithoutConnectionAndFill(0);

        assertFalse("Should return false for connected input", result);
        assertSame("Input should still point to source", source._outputSignal[0], dest._inputSignal[0]);
    }

    @Test
    public void testTearDownOnPauseDoesNotThrow() {
        TestCalculator calc = new TestCalculator(1, 1);

        // Should complete without exception (empty method)
        calc.tearDownOnPause();
    }

    @Test
    public void testSignalThreshold() {
        assertEquals("Signal threshold should be 0.5",
                    0.5, AbstractControlCalculatable.SIGNAL_THRESHOLD, TOLERANCE);
    }

    @Test
    public void testBerechneYOUTPassthrough() {
        TestCalculator calc = new TestCalculator(1, 1);

        // Fill input
        calc.checkInputWithoutConnectionAndFill(0);
        calc._inputSignal[0][0] = 7.5;

        calc.berechneYOUT(0.001);

        assertEquals("Output should equal input", 7.5, calc._outputSignal[0][0], TOLERANCE);
    }

    @Test
    public void testMultipleOutputs() {
        TestCalculator calc = new TestCalculator(0, 5);

        assertEquals("Should have 5 outputs", 5, calc._outputSignal.length);
        for (int i = 0; i < 5; i++) {
            assertNotNull("Output " + i + " should be initialized", calc._outputSignal[i]);
            assertEquals("Output " + i + " should be zero", 0.0, calc._outputSignal[i][0], TOLERANCE);
        }
    }

    @Test
    public void testZeroInputsZeroOutputs() {
        TestCalculator calc = new TestCalculator(0, 0);

        assertEquals("Should have 0 inputs", 0, calc._inputSignal.length);
        assertEquals("Should have 0 outputs", 0, calc._outputSignal.length);
    }

    @Test
    public void testSignalConnectionChain() throws Exception {
        // Create a chain: source -> middle -> dest
        TestCalculator source = new TestCalculator(0, 1);
        TestCalculator middle = new TestCalculator(1, 1);
        TestCalculator dest = new TestCalculator(1, 1);

        // Connect chain
        middle.setInputSignal(0, source, 0);
        dest.setInputSignal(0, middle, 0);

        // Set value at source
        source._outputSignal[0][0] = 100.0;

        // Propagate through chain
        middle.berechneYOUT(0.001);
        dest.berechneYOUT(0.001);

        assertEquals("Value should propagate to middle", 100.0, middle._outputSignal[0][0], TOLERANCE);
        assertEquals("Value should propagate to dest", 100.0, dest._outputSignal[0][0], TOLERANCE);
    }

    @Test
    public void testTimeIsStatic() {
        TestCalculator calc1 = new TestCalculator(1, 1);
        TestCalculator calc2 = new TestCalculator(1, 1);

        AbstractControlCalculatable.setTime(99.9);

        // Both should see the same time
        assertEquals("calc1 should see updated time", 99.9, AbstractControlCalculatable._time, TOLERANCE);
        assertEquals("calc2 should see updated time", 99.9, AbstractControlCalculatable._time, TOLERANCE);
    }
}
