package gecko.core.control.calculators;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DelayHistoryContractTest {
    private DelayCalculator initialized(double delay, double dt) {
        DelayCalculator calculator = new DelayCalculator(delay);
        calculator.checkInputWithoutConnectionAndFill(0);
        calculator.initializeAtSimulationStart(dt);
        return calculator;
    }

    private double step(DelayCalculator calculator, double value, double dt) {
        calculator._inputSignal[0][0] = value;
        calculator.berechneYOUT(dt);
        return calculator._outputSignal[0][0];
    }

    @Test
    void rampIsDelayedByFourSamplesAcrossManyBufferWraps() {
        DelayCalculator calculator = initialized(0.5, 0.125);
        for (int i = 0; i < 40; i++) {
            assertEquals(i < 4 ? 0 : i - 3, step(calculator, i + 1, 0.125));
        }
    }

    @Test
    void coarserTimestepPreservesChronologicalHistoryAfterAWrap() {
        DelayCalculator calculator = initialized(0.5, 0.125);
        for (int i = 0; i < 5; i++) { step(calculator, i + 1, 0.125); }
        // Last sample: t=.5, value=5. Next steps: t=.75 and 1, delayed values at t=.25 and .5.
        calculator.initWithNewDt(0.25);
        assertEquals(3, step(calculator, 7, 0.25));
        assertEquals(5, step(calculator, 9, 0.25));
        assertEquals(7, step(calculator, 11, 0.25));
    }

    @Test
    void finerTimestepInterpolatesTheKnownRampHistory() {
        DelayCalculator calculator = initialized(0.5, 0.125);
        for (int i = 0; i < 5; i++) { step(calculator, i + 1, 0.125); }
        calculator.initWithNewDt(0.0625);
        for (int i = 0; i < 8; i++) {
            assertEquals(1.5 + i * 0.5, step(calculator, 5.5 + i * 0.5, 0.0625), 1e-12);
        }
    }

    @Test
    void changingToZeroDelayPassesThroughImmediately() {
        DelayCalculator calculator = initialized(0.5, 0.125);
        for (int i = 0; i < 5; i++) { step(calculator, i + 1, 0.125); }
        calculator.setDelayTime(0);
        assertEquals(0.5, calculator.getDelayTime(), "new delay is deferred until initialization");
        calculator.initWithNewDt(0.125);
        assertEquals(0, calculator.getDelayTime());
        assertEquals(99, step(calculator, 99, 0.125));
    }

    @Test
    void restartClearsPreviousSamplesAndActivatesTheNewDelay() {
        DelayCalculator calculator = initialized(0.5, 0.125);
        for (int i = 0; i < 8; i++) { step(calculator, 10, 0.125); }
        calculator.setDelayTime(0.25);
        calculator.initializeAtSimulationStart(0.125);
        assertEquals(0.25, calculator.getDelayTime());
        assertEquals(0, step(calculator, 2, 0.125));
        assertEquals(0, step(calculator, 3, 0.125));
        assertEquals(2, step(calculator, 4, 0.125));
    }

    @Test
    void zeroAndSubstepDelayPassThroughAndNegativeDelayIsRejected() {
        assertEquals(42, step(initialized(0, 0.125), 42, 0.125));
        assertEquals(42, step(initialized(0.01, 0.125), 42, 0.125));
        assertThrows(IllegalArgumentException.class, () -> new DelayCalculator(-1));
    }

    @Test
    void longerDelayPadsUnavailableHistoryAndShorterDelayUsesRecentSamples() {
        DelayCalculator calculator = initialized(0.5, 0.125);
        for (int i = 0; i < 5; i++) { step(calculator, i + 1, 0.125); }
        calculator.setDelayTime(1);
        calculator.initWithNewDt(0.125);
        for (double expected : new double[]{0, 0, 0, 1, 2, 3, 4, 5}) {
            assertEquals(expected, step(calculator, 9, 0.125));
        }
        calculator.setDelayTime(0.25);
        calculator.initWithNewDt(0.125);
        assertEquals(9, step(calculator, 42, 0.125));
    }

    @Test
    void timestepInitializationAlsoWorksBeforeTheFirstSimulationStart() {
        DelayCalculator calculator = new DelayCalculator(0.25);
        calculator.checkInputWithoutConnectionAndFill(0);
        calculator.initWithNewDt(0.125);
        assertEquals(0, step(calculator, 1, 0.125));
        assertEquals(0, step(calculator, 2, 0.125));
        assertEquals(1, step(calculator, 3, 0.125));
    }

    @Test
    void restartThenTimestepRefinementCannotResurrectAnOldRun() {
        DelayCalculator calculator = initialized(0.5, 0.125);
        for (int i = 0; i < 8; i++) { step(calculator, 10, 0.125); }
        calculator.initializeAtSimulationStart(0.125);
        calculator.initWithNewDt(0.0625);
        for (int i = 0; i < 8; i++) { assertEquals(0, step(calculator, 0, 0.0625)); }
    }
}
