package gecko.core.control.calculators;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class ExternalSignalContractTest {
    private final double oldTime = AbstractControlCalculatable._time;
    @AfterEach void restoreClock() { AbstractControlCalculatable.setTime(oldTime); }

    private SignalCalculatorExternalWrapper wrapper(AbstractSignalCalculatorPeriodic signal) {
        SignalCalculatorExternalWrapper wrapper = new SignalCalculatorExternalWrapper(signal);
        for (int i = 0; i < 5; i++) { wrapper.checkInputWithoutConnectionAndFill(i); }
        parameters(wrapper, 2, 1, 3, 90, .5);
        return wrapper;
    }
    private void parameters(SignalCalculatorExternalWrapper wrapper, double amplitude, double frequency,
                            double offset, double phase, double duty) {
        double[] values = {amplitude, frequency, offset, phase, duty};
        for (int i = 0; i < values.length; i++) { wrapper._inputSignal[i][0] = values[i]; }
    }
    private double value(SignalCalculatorExternalWrapper wrapper, double time, double dt) {
        AbstractControlCalculatable.setTime(time);
        wrapper.berechneYOUT(dt);
        return wrapper._outputSignal[0][0];
    }

    @Test
    void sineParametersApplyAmplitudeFrequencyOffsetAndPhaseInDegrees() {
        SignalCalculatorExternalWrapper wrapper = wrapper(new SignalCalculatorSinus(0, 1, 1, 0, 0, .5));
        assertEquals(3, value(wrapper, .25, .001), 1e-12);
        assertEquals(5, value(wrapper, .5, .001), 1e-12);
        parameters(wrapper, 4, 2, -1, 0, .5);
        assertEquals(3, value(wrapper, .125, .001), 1e-12);
        assertEquals(-1, value(wrapper, .25, .001), 1e-12);
        parameters(wrapper, 4, 0, 3, 90, .5);
        assertEquals(-1, value(wrapper, 100, .001), 1e-12);
    }

    @Test
    void rectangleDutyAndVoltageLevelsCanChangeWhileTheSignalRuns() {
        SignalCalculatorExternalWrapper wrapper = wrapper(new SignalCalculatorRectangle(0, 0, 0, 0, 0, 0));
        parameters(wrapper, 2, 1, 3, 0, .25);
        double dt = 1.0 / 1024;
        int high = 0;
        for (int i = 1; i <= 4096; i++) {
            double value = value(wrapper, i * dt, dt);
            assertTrue(value == 3 || value == 5);
            if (i > 3072 && value == 5) { high++; }
        }
        assertEquals(.25, high / 1024.0, .003);
        parameters(wrapper, 4, 1, -1, 0, .75);
        high = 0;
        for (int i = 4097; i <= 8192; i++) {
            double value = value(wrapper, i * dt, dt);
            assertTrue(value == -1 || value == 3);
            if (i > 7168 && value == 3) { high++; }
        }
        assertEquals(.75, high / 1024.0, .003);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-2, 0, 1, 2})
    void extremeDutyInputsRemainFiniteAndBounded(double duty) {
        SignalCalculatorExternalWrapper wrapper = wrapper(new SignalCalculatorTriangle(0, 1, 1, 0, 0, .5));
        parameters(wrapper, 2, 1, 0, 0, duty);
        double dt = 1.0 / 1024;
        for (int i = 1; i <= 2048; i++) {
            double value = value(wrapper, i * dt, dt);
            assertTrue(Double.isFinite(value) && value >= -2 && value <= 2);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void delayedInitializationUsesExternalPhaseAndParameters(boolean triangle) {
        AbstractSignalCalculatorPeriodic wrapped = triangle
                ? new SignalCalculatorTriangle(0, 0, 0, 0, 0, 0)
                : new SignalCalculatorRectangle(0, 0, 0, 0, 0, 0);
        AbstractSignalCalculatorPeriodic reference = triangle
                ? new SignalCalculatorTriangle(0, 2, 1, Math.PI / 2, 3, .25)
                : new SignalCalculatorRectangle(0, 2, 1, Math.PI / 2, 3, .25);
        SignalCalculatorExternalWrapper wrapper = wrapper(wrapped);
        parameters(wrapper, 2, 1, 3, 90, .25);
        double dt = 1.0 / 1024;
        reference.initializeAtSimulationStart(dt);
        wrapper.initializeAtSimulationStart(dt);
        for (int i = 1; i <= 128; i++) {
            double actual = value(wrapper, i * dt, dt);
            reference.berechneYOUT(dt);
            assertEquals(reference._outputSignal[0][0], actual, 1e-12);
        }
    }

    @Test
    void negativeFrequencyIsRejected() {
        SignalCalculatorExternalWrapper wrapper = wrapper(new SignalCalculatorSinus(0, 1, 1, 0, 0, .5));
        wrapper._inputSignal[1][0] = -1;
        assertThrows(RuntimeException.class, () -> value(wrapper, 0, .001));
    }

    static Stream<Arguments> nonfiniteInputs() {
        return IntStream.range(0, 5).boxed().flatMap(input -> Stream.of(Double.NaN, Double.POSITIVE_INFINITY)
                .map(value -> Arguments.of(input, value)));
    }

    @ParameterizedTest
    @MethodSource("nonfiniteInputs")
    void nonfiniteInputsAreRejectedBeforeTheyCanReachWaveformInitialization(int input, double badValue) {
        SignalCalculatorExternalWrapper wrapper = wrapper(new SignalCalculatorSinus(0, 1, 1, 0, 0, .5));
        wrapper._inputSignal[input][0] = badValue;
        // Time differs from dt so baseline infinite-phase initialization cannot hang this regression.
        assertThrows(IllegalArgumentException.class, () -> value(wrapper, 0, .001));
    }

    @Test
    void largeEquivalentDegreePhasesDoNotLoseTheirFractionalTurn() {
        SignalCalculatorExternalWrapper wrapper = wrapper(new SignalCalculatorSinus(0,1,1,0,0,.5));
        parameters(wrapper, 2, 1, 3, 360000000000090.0, .5);
        assertEquals(3, value(wrapper, .25, .001), 1e-12);
        parameters(wrapper, 2, 1, 3, -360000000000270.0, .5);
        assertEquals(3, value(wrapper, .25, .001), 1e-12);
    }
}
