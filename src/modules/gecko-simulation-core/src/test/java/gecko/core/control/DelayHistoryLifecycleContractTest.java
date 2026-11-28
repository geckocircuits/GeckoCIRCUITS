package gecko.core.control;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class DelayHistoryLifecycleContractTest {
    private DelayHistory filled() {
        var history = new DelayHistory(0.5);
        history.restart(0.125);
        for (int i = 1; i <= 5; i++) history.step(i, 0.125);
        return history;
    }

    @Test
    void publicationDoesNotCaptureOrAdvanceTheSettledInput() {
        var history = new DelayHistory(0.25);
        history.restart(0.125);
        assertEquals(0, history.publish());
        assertEquals(0, history.publish());
        history.capture(3);
        assertEquals(0, history.publish());
        history.capture(4);
        assertEquals(3, history.publish());
        assertEquals(3, history.publish());
        history.capture(5);
        assertEquals(4, history.publish());
    }

    @Test
    void configurationActivatesOnlyOnRestartOrStepChange() {
        var history = filled();
        history.setDelayTime(0.25);
        assertEquals(0.5, history.getDelayTime());
        assertEquals(2, history.step(6, 0.125));
        history.changeTimeStep(0.125);
        assertEquals(0.25, history.getDelayTime());
        assertEquals(5, history.step(7, 0.125));
        history.restart(0.125);
        assertEquals(0, history.publish());
        assertEquals(0, history.step(8, 0.125));
    }

    @Test
    void stepInitializationBeforeTheFirstRunStartsWithNoKnownSamples() {
        var history = new DelayHistory(0.25);
        assertEquals(0, history.getDelayTime());
        history.changeTimeStep(0.125);
        assertEquals(0, history.step(1, 0.125));
        assertEquals(0, history.step(2, 0.125));
        assertEquals(1, history.step(3, 0.125));
    }

    @Test
    void refinementDuringWarmupNeverInterpolatesBeforeTheFirstSample() {
        var history = new DelayHistory(0.5);
        history.restart(0.125);
        history.step(1, 0.125); history.step(2, 0.125);
        history.changeTimeStep(0.0625);
        double[] expected = {0, 0, 0, 0, 0, 1, 1.5, 2};
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], history.step(2.5 + i * 0.5, 0.0625), 1e-12);
    }

    @Test
    void resizingTwiceWithoutAdvancingPreservesKnownChronology() {
        var history = filled();
        history.changeTimeStep(0.25);
        history.changeTimeStep(0.125);
        for (int i = 0; i < 4; i++) assertEquals(2 + i, history.step(6 + i, 0.125));
    }

    @Test
    void passThroughKeepsTheTwoKnownSamplesForLaterInterpolation() {
        var history = filled();
        history.changeTimeStep(1);
        assertEquals(100, history.step(100, 1));
        assertEquals(200, history.step(200, 1));
        history.changeTimeStep(0.125);
        for (int i = 0; i < 4; i++) assertEquals(162.5 + i * 12.5, history.step(212.5 + i * 12.5, 0.125));
    }

    @Test
    void extendingDelayUsesZeroForOlderUnretainedSamples() {
        var history = filled();
        history.setDelayTime(1);
        history.changeTimeStep(0.125);
        for (double expected : new double[]{0, 0, 0, 1, 2, 3, 4, 5}) assertEquals(expected, history.step(9, 0.125));
    }

    @Test
    void uninitializedHistoryHasExplicitLifecycleErrors() {
        var history = new DelayHistory(0.5);
        assertThrows(IllegalStateException.class, history::publish);
        assertThrows(IllegalStateException.class, () -> history.capture(3));
        assertThrows(IllegalStateException.class, () -> history.step(3, 0.125));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
    void invalidDelayAssignmentsPreserveTheExistingRun(double invalid) {
        assertThrows(IllegalArgumentException.class, () -> new DelayHistory(invalid));
        var history = filled();
        assertThrows(IllegalArgumentException.class, () -> history.setDelayTime(invalid));
        assertEquals(0.5, history.getDelayTime());
        assertEquals(2, history.step(6, 0.125));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
    void invalidTimeStepsCannotResetOrAdvanceHistory(double invalid) {
        var history = filled();
        assertThrows(IllegalArgumentException.class, () -> history.restart(invalid));
        assertThrows(IllegalArgumentException.class, () -> history.changeTimeStep(invalid));
        assertThrows(IllegalArgumentException.class, () -> history.step(999, invalid));
        assertEquals(2, history.step(6, 0.125));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.MAX_VALUE, 268435456})
    void unrepresentableHistorySizesRejectBeforeAllocationAndPreserveTheRun(double delay) {
        var history = filled();
        history.setDelayTime(delay);
        assertThrows(IllegalArgumentException.class, () -> history.restart(0.125));
        assertThrows(IllegalArgumentException.class, () -> history.changeTimeStep(0.125));
        assertEquals(0.5, history.getDelayTime());
        assertEquals(2, history.step(6, 0.125));
    }

    @Test
    void aTinyStepCannotAllocateAnUnrepresentableBuffer() {
        var history = filled();
        assertThrows(IllegalArgumentException.class, () -> history.restart(Double.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> history.changeTimeStep(Double.MIN_VALUE));
        assertEquals(2, history.step(6, 0.125));
    }

    @Test
    void extremeFiniteStepRatiosDoNotIntroduceNanHistory() {
        var history = new DelayHistory(Double.MIN_NORMAL);
        history.restart(Double.MIN_NORMAL);
        assertEquals(0, history.step(1, Double.MIN_NORMAL));
        assertEquals(1, history.step(2, Double.MIN_NORMAL));
        history.changeTimeStep(Double.MAX_VALUE);
        assertEquals(3, history.step(3, Double.MAX_VALUE));
        history.changeTimeStep(Double.MIN_NORMAL);
        assertEquals(3, history.step(4, Double.MIN_NORMAL));
    }

    @Test
    void separateInstancesNeverShareHistoryAcrossRuns() {
        var first = filled();
        var second = new DelayHistory(0.5);
        second.restart(0.125);
        assertEquals(0, second.step(99, 0.125));
        assertEquals(2, first.step(6, 0.125));
        second.restart(0.125);
        assertEquals(3, first.step(7, 0.125));
    }
}
