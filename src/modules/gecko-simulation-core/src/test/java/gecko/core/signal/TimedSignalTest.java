package gecko.core.signal;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class TimedSignalTest {
    private TimedSignal signal() { return new TimedSignal(new double[]{10, 11, 14, 16}, new double[]{2, 4, 8, 99}); }

    @Test
    void ownsInputArraysAndEachMaterializedClock() {
        double[] time = {10, 11, 14, 16}, values = {2, 4, 8, 99};
        TimedSignal signal = new TimedSignal(time, values);
        time[1] = 100; values[0] = 100;
        var first = signal.toContainer();
        assertEquals(2, first.getValue(0, 0));
        assertEquals(1, first.getTimeValue(1, 0));
        first.getTimeSeries(0).setValue(0, 1000);
        assertEquals(0, signal.toContainer().getTimeValue(0, 0));
        assertEquals(6, signal.duration());
        assertEquals(4, signal.sampleCount());
    }

    @Test
    void fractionalSelectionPreservesInteriorSamplesAndExactBounds() {
        TimedSignal selected = signal().window(10.5, 15.0);
        var data = selected.toContainer();
        assertEquals(4.5, selected.duration());
        assertEquals(4, selected.sampleCount());
        double[] times = {0, 0.5, 3.5, 4.5};
        double[] values = {2, 4, 8, 8};
        for (int i = 0; i < times.length; i++) {
            assertEquals(times[i], data.getTimeValue(i, 0));
            assertEquals(values[i], data.getValue(0, i));
        }
        assertEquals(6, signal().duration());
    }

    @Test
    void selectionWithinOneIntervalRetainsThePrecedingValue() {
        var selected = signal().window(11.5, 12.0);
        assertEquals(2, selected.sampleCount());
        assertEquals(4, selected.toContainer().getValue(0, 0));
        assertEquals(4, selected.toContainer().getValue(0, 1));
    }

    @Test
    void omittedWindowBoundsAndUniformRawDataHaveIndependentClocks() {
        assertEquals(6, signal().window(null, null).duration());
        assertEquals(4, signal().window(null, 14.0).duration());
        assertEquals(5, signal().window(11.0, null).duration());
        var raw = TimedSignal.evenlySampled(new double[]{1, 2, 3}, 4.0);
        assertEquals(0.5, raw.duration());
        assertEquals(0.25, raw.toContainer().getTimeValue(1, 0));
    }

    static Stream<double[]> invalidClocks() {
        return Stream.of(new double[]{}, new double[]{0}, new double[]{0, 0}, new double[]{1, 0},
                new double[]{0, Double.NaN}, new double[]{0, Double.POSITIVE_INFINITY},
                new double[]{-Double.MAX_VALUE, Double.MAX_VALUE}, new double[]{0, Double.MIN_VALUE});
    }

    @ParameterizedTest
    @MethodSource("invalidClocks")
    void rejectsInvalidRecordedClocks(double[] time) {
        assertThrows(IllegalArgumentException.class, () -> new TimedSignal(time, new double[time.length]));
    }

    @Test
    void rejectsMissingAndMismatchedArrays() {
        assertThrows(IllegalArgumentException.class, () -> new TimedSignal(null, new double[]{1, 2}));
        assertThrows(IllegalArgumentException.class, () -> new TimedSignal(new double[]{0, 1}, null));
        assertThrows(IllegalArgumentException.class, () -> new TimedSignal(new double[]{0, 1}, new double[]{1}));
        assertThrows(IllegalArgumentException.class, () -> TimedSignal.evenlySampled(null, 1.0));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.MIN_VALUE})
    void rawDataRequiresARepresentablePositiveRate(Double rate) {
        assertThrows(IllegalArgumentException.class, () -> TimedSignal.evenlySampled(new double[]{1, 2}, rate));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE})
    void rejectsValuesThatCannotReachTheAnalysisStorage(double value) {
        assertThrows(IllegalArgumentException.class, () -> new TimedSignal(new double[]{0, 1}, new double[]{0, value}));
    }

    static Stream<double[]> invalidWindows() {
        return Stream.of(new double[]{10, 10}, new double[]{12, 11}, new double[]{9, 11},
                new double[]{10, 17}, new double[]{Double.NaN, 16}, new double[]{10, Double.POSITIVE_INFINITY});
    }

    @ParameterizedTest
    @MethodSource("invalidWindows")
    void rejectsInvalidWindowBounds(double[] bounds) {
        assertThrows(IllegalArgumentException.class, () -> signal().window(bounds[0], bounds[1]));
    }
}
