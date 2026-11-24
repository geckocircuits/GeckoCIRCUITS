package gecko.systemtests;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Max-norm comparison of two complete traces of one physical quantity (one unit block, such as all node potentials).
 * The scale is the largest magnitude of the expected values over every step and every channel of the block, so a
 * channel that is small next to the others is judged against the block, and the whole run is judged at once.
 * The relative criterion needs no floor: the scale comes from the trace. Only a block that is numerically zero, with
 * every expected value at or below {@link #ZERO_BLOCK_ABSOLUTE} in the block's unit (an exactly zero vector, or the
 * rounding residue of a DC state such as the source current of an open circuit), has no usable scale. Its difference
 * must then stay below that same absolute bound. A block with any larger value is always judged relatively.
 */
public final class TraceEquivalence {
    /** The relative equivalence required between the dense and sparse solvers. */
    public static final double TOLERANCE = 1e-9;
    /** Absolute bound, in the block's unit, for a block whose expected values are all numerically zero (1 pV or 1 pA). */
    public static final double ZERO_BLOCK_ABSOLUTE = 1e-12;

    private TraceEquivalence() { }

    /**
     * @param scale largest expected magnitude over the whole block
     * @param difference largest absolute difference over the whole block
     */
    public record Norm(double scale, double difference) {
        /** @return the difference relative to the scale, or the absolute difference for a numerically zero block */
        public double relative() { return scale <= ZERO_BLOCK_ABSOLUTE ? difference : difference / scale; }

        /** @return true when the traces agree to the tolerance, or to the zero-block bound when the block is numerically zero */
        public boolean within(double tolerance) {
            return scale <= ZERO_BLOCK_ABSOLUTE ? difference <= ZERO_BLOCK_ABSOLUTE : difference <= tolerance * scale;
        }
    }

    /**
     * Compares channels {@code from} (inclusive) to {@code to} (exclusive) of every step.
     *
     * @param expected the reference vectors, one per step
     * @param actual the compared vectors, same count and lengths
     */
    public static Norm norm(List<double[]> expected, List<double[]> actual, int from, int to) {
        assertEquals(expected.size(), actual.size(), "step count");
        double scale = 0;
        double difference = 0;
        for (int step = 0; step < expected.size(); step++) {
            double[] e = expected.get(step);
            double[] a = actual.get(step);
            assertEquals(e.length, a.length, "vector length, step " + step);
            for (int i = from; i < Math.min(to, e.length); i++) {
                scale = Math.max(scale, Math.abs(e[i]));
                difference = Math.max(difference, Math.abs(e[i] - a[i]));
                if (Double.isNaN(e[i]) || Double.isNaN(a[i])) difference = Double.NaN;
            }
        }
        return new Norm(scale, difference);
    }

    /** Fails with the measured numbers when the block is outside the tolerance. */
    public static void assertWithin(String label, Norm norm, double tolerance) {
        assertTrue(norm.within(tolerance), label + ": relative max-norm difference " + norm.relative() + " (difference "
                + norm.difference() + ", scale " + norm.scale() + ") exceeds " + tolerance);
    }
}
