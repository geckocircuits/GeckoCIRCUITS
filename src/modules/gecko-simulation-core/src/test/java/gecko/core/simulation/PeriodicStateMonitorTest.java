package gecko.core.simulation;

import static gecko.core.simulation.PeriodicStateMonitor.Outcome.*;
import static org.junit.jupiter.api.Assertions.*;

import gecko.core.simulation.PeriodicStateMonitor.Unit;
import org.junit.jupiter.api.Test;

/** The convergence criterion of ADR-0017, driven with one committed step per period. */
class PeriodicStateMonitorTest {
    private static PeriodicStateMonitor.Outcome feed(PeriodicStateMonitor monitor, int period, Unit unit, double... values) {
        monitor.begin();
        monitor.sink().group(unit, "x");
        for (double value : values) monitor.sink().value(value);
        return monitor.commit(period, period * 1e-6);
    }

    @Test
    void aHarmonicDriftNeverPassesEvenWhenEachRelativeChangeIsBelowTheTolerance() {
        var monitor = new PeriodicStateMonitor(1e-4, 1_000_000, 1);
        double harmonic = 0;
        double lastChange = 0;
        for (int period = 1; period <= 200_000; period++) {
            double previous = harmonic;
            harmonic += 1.0 / period;
            lastChange = period == 1 ? 1 : (harmonic - previous) / harmonic;
            assertEquals(CONTINUE, feed(monitor, period, Unit.CONTROL, harmonic), "period " + period);
        }
        assertTrue(lastChange < 1e-4, "a plain change test would have accepted this drift");
        assertFalse(monitor.report().converged());
        assertTrue(monitor.report().estimate() > 1e-4);
    }

    @Test
    void aSlowContractionIsRejectedBecauseTheRemainingErrorIsLarge() {
        var monitor = new PeriodicStateMonitor(1e-4, 1_000_000, 1);
        double value = 1;
        for (int period = 1; period <= 500; period++) {
            value += 1e-5 * Math.pow(0.9999, period);
            assertEquals(CONTINUE, feed(monitor, period, Unit.VOLT, value));
        }
        assertTrue(monitor.report().residual() <= 1e-4, "the plain change is within the tolerance");
        assertTrue(monitor.report().estimate() > 1e-4, "the corrected estimate is not");
    }

    @Test
    void aGeometricDecayPassesWhenTheCorrectedEstimateIsWithinTheToleranceTwiceInARow() {
        var monitor = new PeriodicStateMonitor(1e-4, 1000, 1);
        int converged = -1;
        for (int period = 1; period <= 100 && converged < 0; period++) {
            if (feed(monitor, period, Unit.VOLT, 1 - Math.pow(0.5, period)) == CONVERGED) converged = period;
        }
        assertTrue(converged >= 14 && converged <= 20, "converged at " + converged);
        assertTrue(monitor.report().converged());
        assertEquals(converged, monitor.report().periods());
        assertTrue(monitor.report().estimate() <= 1e-4);
    }

    @Test
    void aSingleSmallChangeIsNotEnoughBecauseTwoConsecutivePeriodsMustPass() {
        var monitor = new PeriodicStateMonitor(1e-4, 1000, 1);
        feed(monitor, 1, Unit.VOLT, 5);
        assertEquals(CONTINUE, feed(monitor, 2, Unit.VOLT, 5), "an exact repeat is one pass");
        assertEquals(CONVERGED, feed(monitor, 3, Unit.VOLT, 5));
    }

    @Test
    void aRelativeClassFloorStopsACurrentNearZeroFromDominatingTheResidual() {
        var monitor = new PeriodicStateMonitor(1e-4, 1000, 1);
        int converged = -1;
        for (int period = 1; period <= 12 && converged < 0; period++) {
            // One 10 A branch, and one branch that decays from 1e-6 A towards zero like a current near a crossing.
            if (feed(monitor, period, Unit.AMPERE, 10, 1e-6 * Math.pow(0.5, period)) == CONVERGED) converged = period;
        }
        assertTrue(converged > 0 && converged <= 6, "converged at " + converged);
    }

    @Test
    void anAbsoluteFloorStopsNoiseBelowTheInstrumentResolution() {
        var monitor = new PeriodicStateMonitor(1e-4, 1000, 1);
        int converged = -1;
        for (int period = 1; period <= 12 && converged < 0; period++) {
            if (feed(monitor, period, Unit.AMPERE, 1e-12 * Math.pow(0.5, period)) == CONVERGED) converged = period;
        }
        assertTrue(converged > 0 && converged <= 6, "converged at " + converged);
    }

    @Test
    void voltsAndAmperesAreScaledInsideTheirOwnClass() {
        var monitor = new PeriodicStateMonitor(1e-4, 1000, 1);
        for (int period = 1; period <= 5; period++) {
            monitor.begin();
            monitor.sink().group(Unit.VOLT, "v");
            monitor.sink().value(1000);
            monitor.sink().group(Unit.AMPERE, "i");
            monitor.sink().value(period < 5 ? 1e-6 : 2e-6);
            monitor.commit(period, period * 1e-6);
        }
        // Mixed classes would divide the 1e-6 A change by a 1000 V peak and report 1e-9.
        assertEquals(0.5, monitor.report().residual(), 1e-9);
        assertEquals("i[0]", monitor.report().worstState());
    }

    @Test
    void aDiscreteMismatchBlocksAPassAndNamesTheState() {
        var monitor = new PeriodicStateMonitor(1e-4, 1000, 1);
        for (int period = 1; period <= 6; period++) {
            monitor.begin();
            monitor.sink().group(Unit.CONTROL, "level");
            monitor.sink().value(3);
            monitor.sink().group(Unit.DISCRETE, "switch");
            monitor.sink().value(period % 2);
            assertEquals(CONTINUE, monitor.commit(period, period * 1e-6));
        }
        assertEquals(Double.POSITIVE_INFINITY, monitor.report().residual());
        assertEquals("switch[0]", monitor.report().worstState());
        // After the state settles, two exact repeats converge.
        PeriodicStateMonitor.Outcome last = CONTINUE;
        for (int period = 7; period <= 10 && last != CONVERGED; period++) {
            monitor.begin();
            monitor.sink().group(Unit.CONTROL, "level");
            monitor.sink().value(3);
            monitor.sink().group(Unit.DISCRETE, "switch");
            monitor.sink().value(1);
            last = monitor.commit(period, period * 1e-6);
        }
        assertEquals(CONVERGED, last);
    }

    @Test
    void aBooleanFlagIsComparedExactly() {
        var monitor = new PeriodicStateMonitor(1e-4, 1000, 1);
        for (int period = 1; period <= 4; period++) {
            monitor.begin();
            monitor.sink().group(Unit.CONTROL, "held");
            monitor.sink().value(1);
            monitor.sink().flag(period % 2 == 0);
            assertEquals(CONTINUE, monitor.commit(period, period * 1e-6));
        }
        assertEquals(Double.POSITIVE_INFINITY, monitor.report().residual());
    }

    @Test
    void aNonFiniteValueIsAnInfiniteResidual() {
        var monitor = new PeriodicStateMonitor(1e-4, 1000, 1);
        feed(monitor, 1, Unit.VOLT, 1);
        feed(monitor, 2, Unit.VOLT, Double.NaN);
        assertEquals(Double.POSITIVE_INFINITY, monitor.report().residual());
    }

    @Test
    void theSearchEndsAtTheLimitWithoutConverging() {
        var monitor = new PeriodicStateMonitor(1e-4, 5, 1);
        PeriodicStateMonitor.Outcome outcome = CONTINUE;
        for (int period = 1; period <= 5; period++) outcome = feed(monitor, period, Unit.VOLT, period);
        assertEquals(EXHAUSTED, outcome);
        assertFalse(monitor.report().converged());
        assertEquals(5, monitor.report().periods());
    }

    @Test
    void onlyPeriodBoundariesAreCompared() {
        var monitor = new PeriodicStateMonitor(1e-4, 1000, 4);
        for (int step = 1; step <= 3; step++) assertEquals(CONTINUE, feed(monitor, step, Unit.VOLT, 7));
        assertEquals(0, monitor.report().periods());
        feed(monitor, 4, Unit.VOLT, 7);
        assertEquals(1, monitor.report().periods());
    }

    @Test
    void theExportLayoutCannotChangeBetweenSteps() {
        var monitor = new PeriodicStateMonitor(1e-4, 1000, 1);
        feed(monitor, 1, Unit.VOLT, 1, 2);
        assertThrows(IllegalStateException.class, () -> feed(monitor, 2, Unit.VOLT, 1));
        assertThrows(IllegalStateException.class, () -> feed(monitor, 3, Unit.VOLT, 1, 2, 3));
    }
}
