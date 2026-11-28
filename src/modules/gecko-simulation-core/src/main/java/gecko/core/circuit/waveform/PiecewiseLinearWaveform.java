package gecko.core.circuit.waveform;

import java.nio.ByteBuffer;
import java.util.Arrays;

/** An electrical source waveform defined at absolute simulation times. */
public final class PiecewiseLinearWaveform extends TimeFunction {

    public static final String RESOURCE_ID_TOKEN = "pwlResourceId";
    public static final int MAX_POINTS = 2_000_000;
    private static final int MAGIC = 0x50574C31; // PWL1
    private static final int HEADER_BYTES = 2 * Integer.BYTES;
    private static final int PAIR_BYTES = 2 * Double.BYTES;

    private final double[] times;
    private final double[] values;

    public PiecewiseLinearWaveform(double[] times, double[] values) {
        requirePointCount(times.length);
        if (times.length != values.length) {
            throw new IllegalArgumentException("Use the same number of PWL times and values.");
        }
        this.times = times.clone();
        this.values = values.clone();
        for (int i = 0; i < this.times.length; i++) {
            double time = this.times[i];
            if (!Double.isFinite(time) || time < 0 || !Double.isFinite(this.values[i])
                    || (i > 0 && time <= this.times[i - 1])) {
                throw new IllegalArgumentException(
                        "Use finite PWL values and finite, nonnegative, strictly increasing times.");
            }
        }
    }

    private static void requirePointCount(int count) {
        if (count < 1 || count > MAX_POINTS) {
            throw new IllegalArgumentException("Use one to two million PWL time and value pairs.");
        }
    }

    public static boolean hasHeader(byte[] bytes) {
        return bytes.length >= Integer.BYTES && ByteBuffer.wrap(bytes).getInt() == MAGIC;
    }

    public static PiecewiseLinearWaveform fromBytes(byte[] bytes) {
        if (bytes.length < HEADER_BYTES || !hasHeader(bytes)) {
            throw new IllegalArgumentException("Expected a PWL1 resource header.");
        }
        ByteBuffer data = ByteBuffer.wrap(bytes);
        data.getInt();
        int count = data.getInt();
        requirePointCount(count);
        if (bytes.length != HEADER_BYTES + count * PAIR_BYTES) {
            throw new IllegalArgumentException("The PWL resource size does not match its point count.");
        }
        double[] times = new double[count];
        double[] values = new double[count];
        for (int i = 0; i < count; i++) {
            times[i] = data.getDouble();
            values[i] = data.getDouble();
        }
        return new PiecewiseLinearWaveform(times, values);
    }

    public byte[] toBytes() {
        ByteBuffer data = ByteBuffer.allocate(HEADER_BYTES + times.length * PAIR_BYTES);
        data.putInt(MAGIC).putInt(times.length);
        for (int i = 0; i < times.length; i++) {
            data.putDouble(times[i]).putDouble(values[i]);
        }
        return data.array();
    }

    public double[] timeKnots() {
        return times.clone();
    }

    public double[] values() {
        return values.clone();
    }

    @Override
    public double calculate(double time, double dt) {
        if (Double.isNaN(time)) {
            throw new IllegalArgumentException("Use a numeric simulation time.");
        }
        if (time <= times[0]) {
            return values[0];
        }
        int last = times.length - 1;
        if (time >= times[last]) {
            return values[last];
        }
        int index = Arrays.binarySearch(times, time);
        if (index >= 0) {
            return values[index];
        }
        int right = -index - 1;
        int left = right - 1;
        double fraction = (time - times[left]) / (times[right] - times[left]);
        return (1 - fraction) * values[left] + fraction * values[right];
    }

    @Override
    public void stepBack() {
        // The waveform has no simulation state.
    }
}
