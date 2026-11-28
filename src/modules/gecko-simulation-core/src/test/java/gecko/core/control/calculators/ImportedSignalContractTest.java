package gecko.core.control.calculators;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class ImportedSignalContractTest {
    private final double oldTime = AbstractControlCalculatable._time;
    @AfterEach void restoreClock() { AbstractControlCalculatable.setTime(oldTime); }

    private double[][] ramp(double origin) {
        return new double[][]{{origin, origin + .125, origin + .5, origin + 1}, {1, 2, 5, 9}};
    }
    private double value(SignalCalculatorImport calculator, double time) {
        AbstractControlCalculatable.setTime(time);
        calculator.berechneYOUT(.001);
        return calculator._outputSignal[0][0];
    }

    @ParameterizedTest
    @CsvSource({"0, 1", ".0625, 1.5", ".125, 2", ".25, 3", ".875, 8", "1, 9"})
    void nonuniformSamplesInterpolateTheKnownRampIncludingItsEndpoints(double time, double expected) {
        assertEquals(expected, value(new SignalCalculatorImport(ramp(0)), time), 1e-12);
    }

    @ParameterizedTest
    @CsvSource({"1.25, 3", "2.125, 2", "10.875, 8", "3, 9"})
    void repeatingSignalPreservesInterpolationAndTheFinalSampleAtExactPeriodEnds(double time, double expected) {
        assertEquals(expected, value(new SignalCalculatorImport(ramp(0)), time), 1e-12);
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "2, 1", "2.25, 3", "3, 9", "4.25, 3"})
    void nonzeroTimeOriginIsPreservedAndPrehistoryHoldsTheFirstValue(double time, double expected) {
        assertEquals(expected, value(new SignalCalculatorImport(ramp(2)), time), 1e-12);
    }

    @Test
    void restartAndBackwardTimeDoNotRetainAnOldRepeatOffset() {
        SignalCalculatorImport calculator = new SignalCalculatorImport(ramp(0));
        assertEquals(3, value(calculator, 10.25), 1e-12);
        AbstractControlCalculatable.setTime(0);
        calculator.initializeAtSimulationStart(.001);
        assertEquals(3, value(calculator, .25), 1e-12);
        assertEquals(1, value(calculator, -1), 1e-12);
    }

    @Test
    void importedSamplesAreOwnedRatherThanChangedByTheCaller() {
        double[][] samples = ramp(0);
        SignalCalculatorImport calculator = new SignalCalculatorImport(samples);
        samples[0][1] = 100;
        samples[1][1] = -100;
        assertEquals(2, value(calculator, .125), 1e-12);
    }

    static Stream<Arguments> invalidTables() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of((Object) new double[0][]),
                Arguments.of((Object) new double[][]{{0, 1}}),
                Arguments.of((Object) new double[][]{null, {1, 2}}),
                Arguments.of((Object) new double[][]{{0, 1}, null}),
                Arguments.of((Object) new double[][]{{}, {}}),
                Arguments.of((Object) new double[][]{{0}, {1}}),
                Arguments.of((Object) new double[][]{{0, 1}, {1}}),
                Arguments.of((Object) new double[][]{{0, 0}, {1, 2}}),
                Arguments.of((Object) new double[][]{{0, 2, 1}, {1, 2, 3}}),
                Arguments.of((Object) new double[][]{{0, Double.NaN}, {1, 2}}),
                Arguments.of((Object) new double[][]{{0, Double.POSITIVE_INFINITY}, {1, 2}}),
                Arguments.of((Object) new double[][]{{0, 1}, {1, Double.NaN}}),
                Arguments.of((Object) new double[][]{{0, 1}, {1, Double.NEGATIVE_INFINITY}}));
    }

    @ParameterizedTest
    @MethodSource("invalidTables")
    void malformedDataIsRejectedBeforeItCanEnterTheRepeatLoop(double[][] samples) {
        assertThrows(IllegalArgumentException.class, () -> new SignalCalculatorImport(samples));
    }

    @ParameterizedTest
    @CsvSource({".25,3", ".5,5", ".75,7"})
    void twoSampleInterpolationDoesNotHoldTheFirstValueInsideTheInterval(double time, double expected) {
        SignalCalculatorImport calculator = new SignalCalculatorImport(new double[][]{{0,1},{1,9}});
        assertEquals(expected, value(calculator, time), 1e-12);
    }

    @Test
    void interpolationBetweenOppositeFiniteExtremesDoesNotOverflow() {
        SignalCalculatorImport calculator = new SignalCalculatorImport(new double[][]{
                {0,.25,.75,1}, {-Double.MAX_VALUE,-Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE}});
        assertEquals(0, value(calculator, .5));
    }

    @Test
    void aLargeClockJumpRetainsTheCorrectRepeatPhase() {
        SignalCalculatorImport calculator = new SignalCalculatorImport(ramp(0));
        assertEquals(3, value(calculator, 1000000000000.25), 1e-12);
    }

    @Test
    void nonfiniteClockAndUnrepresentableDurationAreRejected() {
        SignalCalculatorImport calculator = new SignalCalculatorImport(ramp(0));
        for (double time : new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> value(calculator, time));
        }
        assertThrows(IllegalArgumentException.class, () -> new SignalCalculatorImport(
                new double[][]{{-Double.MAX_VALUE,Double.MAX_VALUE},{0,1}}));
    }
}
