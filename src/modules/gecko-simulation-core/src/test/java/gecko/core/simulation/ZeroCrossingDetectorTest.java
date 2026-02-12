/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package gecko.core.simulation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZeroCrossingDetectorTest {

    private ZeroCrossingDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ZeroCrossingDetector();
    }

    // --- Constructor tests ---

    @Test
    void defaultConstructor_usesDefaultParameters() {
        assertEquals(ZeroCrossingDetector.DEFAULT_TOLERANCE, detector.getTolerance());
        assertEquals(ZeroCrossingDetector.DEFAULT_MAX_ITERATIONS, detector.getMaxIterations());
    }

    @Test
    void customConstructor_setsParameters() {
        ZeroCrossingDetector custom = new ZeroCrossingDetector(1e-9, 100);
        assertEquals(1e-9, custom.getTolerance());
        assertEquals(100, custom.getMaxIterations());
    }

    @Test
    void constructor_rejectsZeroTolerance() {
        assertThrows(IllegalArgumentException.class, () -> new ZeroCrossingDetector(0, 50));
    }

    @Test
    void constructor_rejectsNegativeTolerance() {
        assertThrows(IllegalArgumentException.class, () -> new ZeroCrossingDetector(-1e-12, 50));
    }

    @Test
    void constructor_rejectsNaNTolerance() {
        assertThrows(IllegalArgumentException.class, () -> new ZeroCrossingDetector(Double.NaN, 50));
    }

    @Test
    void constructor_rejectsInfiniteTolerance() {
        assertThrows(IllegalArgumentException.class,
                () -> new ZeroCrossingDetector(Double.POSITIVE_INFINITY, 50));
    }

    @Test
    void constructor_rejectsZeroMaxIterations() {
        assertThrows(IllegalArgumentException.class, () -> new ZeroCrossingDetector(1e-12, 0));
    }

    // --- Rising edge detection ---

    @Nested
    class RisingEdgeDetection {

        @Test
        void detectsRisingEdge_belowToAboveThreshold() {
            double[] prev = {0.2};
            double[] curr = {0.8};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertEquals(1, events.size());
            assertEquals(ZeroCrossingEvent.Direction.RISING, events.get(0).getDirection());
            assertEquals(0, events.get(0).getSignalIndex());
        }

        @Test
        void risingEdge_crossingTimeIsAccurate() {
            // Signal goes from 0.0 to 1.0 with threshold 0.5
            // Linear interpolation: crossing at t = 0.5e-6
            double[] prev = {0.0};
            double[] curr = {1.0};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertEquals(1, events.size());
            assertEquals(0.5e-6, events.get(0).getCrossingTime(), 1e-12);
        }

        @Test
        void risingEdge_asymmetricCrossing() {
            // Signal goes from 0.1 to 0.9 with threshold 0.5
            // Crossing at t = (0.4/0.8) * 1e-6 = 0.5e-6
            double[] prev = {0.1};
            double[] curr = {0.9};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertEquals(1, events.size());
            assertEquals(0.5e-6, events.get(0).getCrossingTime(), 1e-12);
        }

        @Test
        void risingEdge_storesCorrectValues() {
            double[] prev = {0.3};
            double[] curr = {0.7};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertEquals(0.3, events.get(0).getValueBefore(), 1e-15);
            assertEquals(0.7, events.get(0).getValueAfter(), 1e-15);
        }
    }

    // --- Falling edge detection ---

    @Nested
    class FallingEdgeDetection {

        @Test
        void detectsFallingEdge_aboveToBelow() {
            double[] prev = {0.8};
            double[] curr = {0.2};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertEquals(1, events.size());
            assertEquals(ZeroCrossingEvent.Direction.FALLING, events.get(0).getDirection());
        }

        @Test
        void fallingEdge_crossingTimeIsAccurate() {
            // Signal goes from 1.0 to 0.0 with threshold 0.5
            // Linear interpolation: crossing at t = 0.5e-6
            double[] prev = {1.0};
            double[] curr = {0.0};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertEquals(1, events.size());
            assertEquals(0.5e-6, events.get(0).getCrossingTime(), 1e-12);
        }

        @Test
        void fallingEdge_nearThreshold() {
            // Signal barely crosses from just above to just below
            double[] prev = {0.5001};
            double[] curr = {0.4999};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertEquals(1, events.size());
            assertEquals(ZeroCrossingEvent.Direction.FALLING, events.get(0).getDirection());
        }
    }

    // --- No-crossing cases ---

    @Nested
    class NoCrossingDetection {

        @Test
        void noCrossing_bothBelowThreshold() {
            double[] prev = {0.1};
            double[] curr = {0.3};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertTrue(events.isEmpty());
        }

        @Test
        void noCrossing_bothAboveThreshold() {
            double[] prev = {0.7};
            double[] curr = {0.9};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertTrue(events.isEmpty());
        }

        @Test
        void noCrossing_emptyArrays() {
            double[] prev = {};
            double[] curr = {};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertTrue(events.isEmpty());
        }

        @Test
        void noCrossing_signalExactlyAtThreshold() {
            // When signal is exactly at threshold on one side, no sign change occurs
            // (0.0 * positive = 0, which is not < 0)
            double[] prev = {0.5};
            double[] curr = {0.8};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertTrue(events.isEmpty());
        }

        @Test
        void noCrossing_bothExactlyAtThreshold() {
            double[] prev = {0.5};
            double[] curr = {0.5};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertTrue(events.isEmpty());
        }
    }

    // --- Multiple signals ---

    @Nested
    class MultipleSignals {

        @Test
        void detectsCrossingsInMultipleChannels() {
            double[] prev = {0.2, 0.8, 0.5};
            double[] curr = {0.8, 0.2, 0.5};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertEquals(2, events.size());
        }

        @Test
        void multipleChannels_sortedByTime() {
            // Channel 0: crosses near end of interval (0.1 -> 0.6, threshold 0.5 -> crossing at 0.8e-6)
            // Channel 1: crosses near start of interval (0.4 -> 0.6, threshold 0.5 -> crossing at 0.5e-6)
            double[] prev = {0.1, 0.4};
            double[] curr = {0.6, 0.6};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertEquals(2, events.size());
            // Channel 1 crossing should come first (earlier time)
            assertTrue(events.get(0).getCrossingTime() <= events.get(1).getCrossingTime());
        }

        @Test
        void simultaneousCrossings_allDetected() {
            // All channels cross at exactly the same point
            double[] prev = {0.0, 0.0, 0.0};
            double[] curr = {1.0, 1.0, 1.0};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertEquals(3, events.size());
            for (ZeroCrossingEvent event : events) {
                assertEquals(ZeroCrossingEvent.Direction.RISING, event.getDirection());
            }
        }

        @Test
        void mixedDirections_bothDetected() {
            // Channel 0: rising, Channel 1: falling
            double[] prev = {0.2, 0.8};
            double[] curr = {0.8, 0.2};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertEquals(2, events.size());
            boolean hasRising = events.stream().anyMatch(e -> e.getDirection() == ZeroCrossingEvent.Direction.RISING);
            boolean hasFalling = events.stream().anyMatch(e -> e.getDirection() == ZeroCrossingEvent.Direction.FALLING);
            assertTrue(hasRising);
            assertTrue(hasFalling);
        }
    }

    // --- Bisection convergence ---

    @Nested
    class BisectionConvergence {

        @Test
        void bisection_convergesForSymmetricCrossing() {
            // Shifted values: -0.5 and +0.5 -> crossing at midpoint
            double tCross = detector.bisectCrossingTime(-0.5, 0.5, 0.0, 1e-6);
            assertEquals(0.5e-6, tCross, 1e-12);
        }

        @Test
        void bisection_convergesForAsymmetricCrossing() {
            // Shifted values: -0.1 and +0.9 -> crossing at 0.1e-6
            double tCross = detector.bisectCrossingTime(-0.1, 0.9, 0.0, 1e-6);
            assertEquals(0.1e-6, tCross, 1e-12);
        }

        @Test
        void bisection_convergesNearStart() {
            // Crossing very close to tPrev
            double tCross = detector.bisectCrossingTime(-0.001, 0.999, 0.0, 1e-6);
            assertEquals(0.001e-6, tCross, 1e-12);
        }

        @Test
        void bisection_convergesNearEnd() {
            // Crossing very close to tCurr
            double tCross = detector.bisectCrossingTime(-0.999, 0.001, 0.0, 1e-6);
            assertEquals(0.999e-6, tCross, 1e-12);
        }

        @Test
        void bisection_worksWithLargeTimestep() {
            double tCross = detector.bisectCrossingTime(-1.0, 1.0, 0.0, 0.001);
            assertEquals(0.0005, tCross, 1e-12);
        }

        @Test
        void bisection_worksWithVerySmallTimestep() {
            double tCross = detector.bisectCrossingTime(-1.0, 1.0, 0.0, 1e-9);
            assertEquals(0.5e-9, tCross, 1e-15);
        }

        @Test
        void bisection_limitedIterations_stillReasonable() {
            // With only 1 iteration, result should still be within the interval
            ZeroCrossingDetector limited = new ZeroCrossingDetector(1e-12, 1);
            double tCross = limited.bisectCrossingTime(-0.5, 0.5, 0.0, 1e-6);
            assertTrue(tCross >= 0.0 && tCross <= 1e-6);
        }
    }

    // --- hasCrossing convenience method ---

    @Nested
    class HasCrossing {

        @Test
        void hasCrossing_risingEdge() {
            assertTrue(detector.hasCrossing(0.2, 0.8, 0.5));
        }

        @Test
        void hasCrossing_fallingEdge() {
            assertTrue(detector.hasCrossing(0.8, 0.2, 0.5));
        }

        @Test
        void hasCrossing_noCrossing() {
            assertFalse(detector.hasCrossing(0.1, 0.3, 0.5));
        }

        @Test
        void hasCrossing_atThreshold() {
            assertFalse(detector.hasCrossing(0.5, 0.8, 0.5));
        }

        @Test
        void hasCrossing_negativeThreshold() {
            assertTrue(detector.hasCrossing(-1.0, 1.0, 0.0));
        }

        @Test
        void hasCrossing_zeroThreshold() {
            assertTrue(detector.hasCrossing(-0.1, 0.1, 0.0));
        }
    }

    // --- Input validation ---

    @Nested
    class InputValidation {

        @Test
        void rejectsNullPrevArray() {
            assertThrows(IllegalArgumentException.class,
                    () -> detector.detectCrossings(null, new double[]{0.5}, 0.5, 0.0, 1e-6));
        }

        @Test
        void rejectsNullCurrArray() {
            assertThrows(IllegalArgumentException.class,
                    () -> detector.detectCrossings(new double[]{0.5}, null, 0.5, 0.0, 1e-6));
        }

        @Test
        void rejectsMismatchedArrayLengths() {
            assertThrows(IllegalArgumentException.class,
                    () -> detector.detectCrossings(new double[]{0.1}, new double[]{0.2, 0.3}, 0.5, 0.0, 1e-6));
        }

        @Test
        void rejectsEqualTimes() {
            assertThrows(IllegalArgumentException.class,
                    () -> detector.detectCrossings(new double[]{0.1}, new double[]{0.8}, 0.5, 1e-6, 1e-6));
        }

        @Test
        void rejectsReversedTimes() {
            assertThrows(IllegalArgumentException.class,
                    () -> detector.detectCrossings(new double[]{0.1}, new double[]{0.8}, 0.5, 1e-6, 0.0));
        }

        @Test
        void rejectsNaNTime() {
            assertThrows(IllegalArgumentException.class,
                    () -> detector.detectCrossings(new double[]{0.1}, new double[]{0.8}, 0.5, Double.NaN, 1e-6));
        }

        @Test
        void rejectsInfiniteTime() {
            assertThrows(IllegalArgumentException.class,
                    () -> detector.detectCrossings(new double[]{0.1}, new double[]{0.8}, 0.5, 0.0, Double.POSITIVE_INFINITY));
        }
    }

    // --- Event immutability ---

    @Test
    void resultList_isUnmodifiable() {
        double[] prev = {0.2};
        double[] curr = {0.8};
        List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

        assertThrows(UnsupportedOperationException.class, () -> events.add(
                new ZeroCrossingEvent(0, 0, ZeroCrossingEvent.Direction.RISING, 0, 0)));
    }

    // --- Power electronics use cases ---

    @Nested
    class PowerElectronicsUseCases {

        @Test
        void gateSignalSwitchOn_detectedAtCorrectTime() {
            // MOSFET gate signal: 0V to 15V, threshold 5V
            double[] prev = {0.0};
            double[] curr = {15.0};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 5.0, 0.0, 1e-7);

            assertEquals(1, events.size());
            assertEquals(ZeroCrossingEvent.Direction.RISING, events.get(0).getDirection());
            // Crossing at 1/3 of the interval
            assertEquals(1e-7 / 3.0, events.get(0).getCrossingTime(), 1e-15);
        }

        @Test
        void pwmComparatorCrossing_triangleVsReference() {
            // Triangle carrier at two samples, crossing a DC reference
            double carrierPrev = 0.3;  // Below reference
            double carrierCurr = 0.7;  // Above reference
            double reference = 0.5;    // DC reference

            assertTrue(detector.hasCrossing(carrierPrev, carrierCurr, reference));
        }

        @Test
        void multipleGateSignals_sortedByEventTime() {
            // Three-phase inverter: gates switch at different times within the step
            // Gate A: early crossing, Gate B: late crossing, Gate C: no crossing
            double[] prev = {0.4, 0.1, 0.8};
            double[] curr = {0.6, 0.9, 0.9};
            List<ZeroCrossingEvent> events = detector.detectCrossings(prev, curr, 0.5, 0.0, 1e-6);

            assertEquals(2, events.size());
            // Gate A crosses at 0.5e-6, Gate B crosses at 0.5e-6
            // Both should be detected
            assertTrue(events.get(0).getCrossingTime() <= events.get(1).getCrossingTime());
        }
    }

    // --- ZeroCrossingEvent ---

    @Nested
    class EventTests {

        @Test
        void event_toStringContainsRelevantInfo() {
            ZeroCrossingEvent event = new ZeroCrossingEvent(
                    2, 1.5e-6, ZeroCrossingEvent.Direction.RISING, 0.3, 0.7);

            String str = event.toString();
            assertTrue(str.contains("signal=2"));
            assertTrue(str.contains("RISING"));
        }

        @Test
        void event_gettersReturnConstructorValues() {
            ZeroCrossingEvent event = new ZeroCrossingEvent(
                    3, 2.5e-6, ZeroCrossingEvent.Direction.FALLING, 0.9, 0.1);

            assertEquals(3, event.getSignalIndex());
            assertEquals(2.5e-6, event.getCrossingTime(), 1e-20);
            assertEquals(ZeroCrossingEvent.Direction.FALLING, event.getDirection());
            assertEquals(0.9, event.getValueBefore(), 1e-15);
            assertEquals(0.1, event.getValueAfter(), 1e-15);
        }
    }
}
