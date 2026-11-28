package gecko.core.signal;

import gecko.core.datacontainer.DataContainerSimple;
import java.util.Arrays;

/** Owns independent samples and their recorded clock for time-based analysis. */
public final class TimedSignal {
    private final double[] time;
    private final double[] values;

    /**
     * Captures a signal with a finite, strictly increasing clock.
     *
     * @param time the recorded timestamps, with at least two entries.
     * @param values the corresponding finite values within the float storage range.
     * @throws IllegalArgumentException if the arrays or their duration are invalid.
     */
    public TimedSignal(double[] time, double[] values) {
        if (time == null || values == null || time.length < 2 || time.length != values.length) {
            throw new IllegalArgumentException("Signal values require a matching recorded clock with at least two samples");
        }
        this.time = time.clone();
        this.values = values.clone();
        for (int i = 0; i < time.length; i++) {
            if (!Double.isFinite(this.time[i]) || (i > 0 && this.time[i] <= this.time[i - 1])) {
                throw new IllegalArgumentException("Recorded timestamps must be finite and strictly increasing");
            }
            if (!Float.isFinite((float) this.values[i])) {
                throw new IllegalArgumentException("Signal values must be finite and within the float storage range");
            }
        }
        double span = duration();
        if (!Double.isFinite(span) || !Double.isFinite(1 / span)) {
            throw new IllegalArgumentException("Signal duration and its reciprocal must be finite");
        }
    }

    /**
     * Captures raw values with an explicitly supplied uniform sample rate.
     *
     * @param values the raw samples.
     * @param sampleRate the finite positive rate in samples per second.
     * @return an independent timed signal.
     * @throws IllegalArgumentException if the rate, values, or duration are invalid.
     */
    public static TimedSignal evenlySampled(double[] values, Double sampleRate) {
        if (sampleRate == null || !Double.isFinite(sampleRate) || sampleRate <= 0) {
            throw new IllegalArgumentException("A finite sampleRate > 0 is required for raw data");
        }
        if (values == null) throw new IllegalArgumentException("Signal values are required");
        double[] time = new double[values.length];
        for (int i = 0; i < time.length; i++) time[i] = i / sampleRate;
        return new TimedSignal(time, values);
    }

    /**
     * Selects exact bounds using the preceding sample between recorded timestamps.
     * Omitted bounds use the first and last recorded timestamps independently.
     *
     * @param start the inclusive lower time bound, or null.
     * @param end the upper integration bound, or null.
     * @return an independent selection with boundary samples and interior recorded samples.
     * @throws IllegalArgumentException if the bounds are invalid or outside the recorded clock.
     */
    public TimedSignal window(Double start, Double end) {
        double lower = start == null ? time[0] : start;
        double upper = end == null ? time[time.length - 1] : end;
        if (!Double.isFinite(lower) || !Double.isFinite(upper) || lower >= upper
                || lower < time[0] || upper > time[time.length - 1]) {
            throw new IllegalArgumentException("Analysis bounds must be finite, increasing, and within the recorded clock");
        }
        int first = Arrays.binarySearch(time, lower);
        if (first < 0) first = -first - 2;
        int last = Arrays.binarySearch(time, upper);
        if (last < 0) last = -last - 1;
        int count = last - first + 1;
        double[] selectedTime = new double[count];
        double[] selectedValues = new double[count];
        selectedTime[0] = lower;
        selectedValues[0] = values[first];
        for (int i = 1; i < count - 1; i++) {
            selectedTime[i] = time[first + i];
            selectedValues[i] = values[first + i];
        }
        selectedTime[count - 1] = upper;
        selectedValues[count - 1] = values[time[last] == upper ? last : last - 1];
        return new TimedSignal(selectedTime, selectedValues);
    }

    /** @return the selected duration in seconds. */
    public double duration() { return time[time.length - 1] - time[0]; }

    /** @return the sample count, including selected boundary samples. */
    public int sampleCount() { return time.length; }

    /**
     * Copies the samples into an analysis container with a relative clock starting at zero.
     * The relative clock preserves recorded intervals and avoids large-origin precision loss.
     *
     * @return an independent analysis container.
     */
    public DataContainerSimple toContainer() {
        var container = DataContainerSimple.fabricArrayTimeSeries(1, time.length);
        for (int i = 0; i < time.length; i++) {
            container.insertValuesAtEnd(new float[]{(float) values[i]}, time[i] - time[0]);
        }
        return container;
    }
}
