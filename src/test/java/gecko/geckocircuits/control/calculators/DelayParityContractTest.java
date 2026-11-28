package gecko.geckocircuits.control.calculators;

import java.util.function.DoubleConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class DelayParityContractTest {
    enum RuntimePath { DESKTOP, CORE }

    private record Probe(double[][] input, double[][] output, DoubleConsumer start,
                         DoubleConsumer resize, DoubleConsumer configure, DoubleConsumer calculate, Runnable pause) {
        double step(double value, double dt) {
            input[0][0] = value;
            calculate.accept(dt);
            return output[0][0];
        }
    }

    private Probe create(RuntimePath path, double delay) {
        if (path == RuntimePath.DESKTOP) {
            var calculator = new DelayCalculator(delay);
            calculator.checkInputWithoutConnectionAndFill(0);
            return new Probe(calculator._inputSignal, calculator._outputSignal,
                    ((InitializableAtSimulationStart) calculator)::initializeAtSimulationStart,
                    ((gecko.geckocircuits.control.IsDtChangeSensitive) calculator)::initWithNewDt,
                    calculator::setDelayTime, calculator::berechneYOUT, calculator::tearDownOnPause);
        }
        var calculator = new gecko.core.control.calculators.DelayCalculator(delay);
        calculator.checkInputWithoutConnectionAndFill(0);
        return new Probe(calculator._inputSignal, calculator._outputSignal,
                ((gecko.core.control.calculators.InitializableAtSimulationStart) calculator)::initializeAtSimulationStart,
                ((gecko.core.control.IsDtChangeSensitive) calculator)::initWithNewDt,
                calculator::setDelayTime, calculator::berechneYOUT, calculator::tearDownOnPause);
    }

    private Probe started(RuntimePath path, double delay, double dt) {
        var probe = create(path, delay);
        probe.start.accept(dt);
        return probe;
    }

    @ParameterizedTest
    @EnumSource(RuntimePath.class)
    void ringWrapsRetainTheFourSampleDelay(RuntimePath path) {
        var delay = started(path, 0.5, 0.125);
        for (int i = 0; i < 100; i++) assertEquals(i < 4 ? 0 : i - 3, delay.step(i + 1, 0.125));
    }

    @ParameterizedTest
    @EnumSource(RuntimePath.class)
    void coarserStepsReadChronologicalHistoryAfterAWrap(RuntimePath path) {
        var delay = started(path, 0.5, 0.125);
        for (int i = 1; i <= 5; i++) delay.step(i, 0.125);
        delay.resize.accept(0.25);
        double[] expected = {3, 5, 7, 9};
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], delay.step(7 + 2 * i, 0.25));
    }

    @ParameterizedTest
    @EnumSource(RuntimePath.class)
    void finerStepsInterpolateKnownChronologicalHistory(RuntimePath path) {
        var delay = started(path, 0.5, 0.125);
        for (int i = 1; i <= 5; i++) delay.step(i, 0.125);
        delay.resize.accept(0.0625);
        for (int i = 0; i < 8; i++) assertEquals(1.5 + i * 0.5, delay.step(5.5 + i * 0.5, 0.0625), 1e-12);
    }

    @ParameterizedTest
    @EnumSource(RuntimePath.class)
    void repeatedStepChangesFollowTheSamePhysicalRamp(RuntimePath path) {
        var delay = started(path, 0.5, 0.125);
        double time = 0;
        for (int i = 0; i < 40; i++, time += 0.125) delay.step(1 + 8 * time, 0.125);
        time -= 0.125;
        for (double dt : new double[]{0.25, 0.0625, 0.125, 0.25, 0.0625}) {
            delay.resize.accept(dt);
            for (int i = 0; i < 6; i++) {
                time += dt;
                assertEquals(1 + 8 * (time - 0.5), delay.step(1 + 8 * time, dt), 1e-12);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(RuntimePath.class)
    void restartImmediatelyClearsOutputAndActivatesThePendingDelay(RuntimePath path) {
        var delay = started(path, 0.5, 0.125);
        for (int i = 0; i < 10; i++) delay.step(7, 0.125);
        assertEquals(7, delay.output[0][0]);
        delay.configure.accept(0.25);
        double[] outputReference = delay.output[0];
        delay.start.accept(0.125);
        assertSame(outputReference, delay.output[0]);
        assertEquals(0, delay.output[0][0]);
        assertEquals(0, delay.step(2, 0.125));
        assertEquals(0, delay.step(3, 0.125));
        assertEquals(2, delay.step(4, 0.125));
    }

    @ParameterizedTest
    @EnumSource(RuntimePath.class)
    void restartAndRefinementCannotResurrectAnOldRun(RuntimePath path) {
        var delay = started(path, 0.5, 0.125);
        for (int i = 0; i < 12; i++) delay.step(7, 0.125);
        delay.start.accept(0.125);
        delay.resize.accept(0.0625);
        for (int i = 0; i < 10; i++) assertEquals(0, delay.step(0, 0.0625));
    }

    static Stream<Arguments> continuations() {
        return Stream.of(RuntimePath.values()).flatMap(path -> Stream.of(2, 7).flatMap(count ->
                Stream.of(false, true).map(resize -> Arguments.of(path, count, resize))));
    }

    @ParameterizedTest
    @MethodSource("continuations")
    void pauseAndContinuationPreserveSamplesDuringAndAfterWarmup(RuntimePath path, int pauseAfter, boolean resize) {
        var delay = started(path, 0.5, 0.125);
        for (int i = 1; i <= pauseAfter; i++) delay.step(i, 0.125);
        double previousOutput = delay.output[0][0];
        delay.pause.run();
        assertEquals(previousOutput, delay.output[0][0]);
        if (resize) delay.resize.accept(0.125);
        for (int i = pauseAfter + 1; i <= pauseAfter + 8; i++) assertEquals(Math.max(0, i - 4), delay.step(i, 0.125));
    }

    @ParameterizedTest
    @EnumSource(RuntimePath.class)
    void parameterChangesWaitForAnExplicitLifecycleCallback(RuntimePath path) {
        var delay = started(path, 0.5, 0.125);
        for (int i = 1; i <= 5; i++) delay.step(i, 0.125);
        delay.configure.accept(0.25);
        assertEquals(2, delay.step(6, 0.125));
        delay.resize.accept(0.125);
        assertEquals(5, delay.step(7, 0.125));
    }

    @ParameterizedTest
    @EnumSource(RuntimePath.class)
    void longerDelaysPadUnavailableHistoryAndShorterDelaysUseRecentSamples(RuntimePath path) {
        var delay = started(path, 0.5, 0.125);
        for (int i = 1; i <= 5; i++) delay.step(i, 0.125);
        delay.configure.accept(1);
        delay.resize.accept(0.125);
        for (double expected : new double[]{0, 0, 0, 1, 2, 3, 4, 5}) assertEquals(expected, delay.step(9, 0.125));
        delay.configure.accept(0.25);
        delay.resize.accept(0.125);
        assertEquals(9, delay.step(42, 0.125));
    }

    @ParameterizedTest
    @EnumSource(RuntimePath.class)
    void passThroughRetainsTheRecentSamplesNeededWhenBufferingReturns(RuntimePath path) {
        var delay = started(path, 0.5, 0.125);
        for (int i = 1; i <= 8; i++) delay.step(i, 0.125);
        delay.resize.accept(1);
        assertEquals(100, delay.step(100, 1));
        assertEquals(200, delay.step(200, 1));
        delay.resize.accept(0.125);
        double[] expected = {162.5, 175, 187.5, 200};
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], delay.step(212.5 + 12.5 * i, 0.125), 1e-12);
    }

    @ParameterizedTest
    @EnumSource(RuntimePath.class)
    void zeroDelayDoesNotReuseAnOlderBufferedRunWhenTheDelayGrows(RuntimePath path) {
        var delay = started(path, 0, 0.125);
        for (int i = 1; i <= 5; i++) assertEquals(i, delay.step(i, 0.125));
        delay.configure.accept(0.5);
        delay.resize.accept(0.125);
        for (double expected : new double[]{0, 0, 4, 5}) assertEquals(expected, delay.step(9, 0.125));
    }

    @ParameterizedTest
    @EnumSource(RuntimePath.class)
    void lifecycleInitializationBeforeTheFirstStartProducesZeroHistory(RuntimePath path) {
        var delay = create(path, 0.25);
        delay.resize.accept(0.125);
        assertEquals(0, delay.step(1, 0.125));
        assertEquals(0, delay.step(2, 0.125));
        assertEquals(1, delay.step(3, 0.125));
    }

    static Stream<Arguments> invalidDelays() {
        return Stream.of(RuntimePath.values()).flatMap(path -> Stream.of(-1.0, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).map(value -> Arguments.of(path, value)));
    }

    @ParameterizedTest
    @MethodSource("invalidDelays")
    void nonfiniteAndNegativeDelaySettingsRejectWithoutChangingHistory(RuntimePath path, double invalid) {
        assertThrows(IllegalArgumentException.class, () -> create(path, invalid));
        var delay = started(path, 0.5, 0.125);
        for (int i = 1; i <= 5; i++) delay.step(i, 0.125);
        assertThrows(IllegalArgumentException.class, () -> delay.configure.accept(invalid));
        assertEquals(2, delay.step(6, 0.125));
    }

    @Test
    void desktopBlockFactoryUsesTheSameRestartAndHistoryContract() {
        var block = new gecko.geckocircuits.control.ReglerDelay();
        block._tDelay.setValueWithoutUndo(0.5);
        var calculator = assertInstanceOf(DelayCalculator.class, block.getInternalControlCalculatableForSimulationStart());
        calculator.checkInputWithoutConnectionAndFill(0);
        calculator.initializeAtSimulationStart(0.125);
        for (int i = 1; i <= 5; i++) {
            calculator._inputSignal[0][0] = i;
            calculator.berechneYOUT(0.125);
        }
        calculator.initWithNewDt(0.25);
        calculator._inputSignal[0][0] = 7;
        calculator.berechneYOUT(0.25);
        assertEquals(3, calculator._outputSignal[0][0]);
        calculator.initializeAtSimulationStart(0.125);
        assertEquals(0, calculator._outputSignal[0][0]);
    }
    @ParameterizedTest
    @EnumSource(RuntimePath.class)
    void refinementDuringWarmupCannotInventSamplesBeforeTheRun(RuntimePath path) {
        var delay = started(path, 0.5, 0.125);
        delay.step(1, 0.125);
        delay.step(2, 0.125);
        delay.resize.accept(0.0625);
        double[] expected = {0, 0, 0, 0, 0, 1, 1.5, 2};
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], delay.step(2.5 + 0.5 * i, 0.0625), 1e-12);
    }
    static Stream<Arguments> invalidSteps() {
        return Stream.of(RuntimePath.values()).flatMap(path -> Stream.of(0.0, -1.0, Double.NaN,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY).map(dt -> Arguments.of(path, dt)));
    }

    @ParameterizedTest
    @MethodSource("invalidSteps")
    void invalidLifecycleStepsPreserveTheSignalAndBufferedSamples(RuntimePath path, double invalid) {
        var delay = started(path, 0.5, 0.125);
        for (int i = 1; i <= 5; i++) delay.step(i, 0.125);
        assertEquals(1, delay.output[0][0]);
        assertThrows(IllegalArgumentException.class, () -> delay.start.accept(invalid));
        assertThrows(IllegalArgumentException.class, () -> delay.resize.accept(invalid));
        assertThrows(IllegalArgumentException.class, () -> delay.step(999, invalid));
        assertEquals(1, delay.output[0][0]);
        assertEquals(2, delay.step(6, 0.125));
    }
}
