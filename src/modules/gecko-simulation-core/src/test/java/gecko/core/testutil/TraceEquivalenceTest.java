package gecko.core.testutil;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The comparator must reject small-signal errors that a floor-based comparison would accept. */
class TraceEquivalenceTest {
    private static TraceEquivalence.Norm norm(double expected, double actual) {
        return TraceEquivalence.norm(List.of(new double[] {expected}), List.of(new double[] {actual}), 0, 1);
    }

    @Test
    void aMicroampereWithAOneInTenThousandErrorFails() {
        assertFalse(norm(1e-6, 1.0001e-6).within(TraceEquivalence.TOLERANCE));
        assertThrows(AssertionError.class,
                () -> TraceEquivalence.assertWithin("current", norm(1e-6, 1.0001e-6), TraceEquivalence.TOLERANCE));
    }

    @Test
    void aMillivoltWithAOneInTenMillionErrorFails() {
        assertFalse(norm(1e-3, 1.0000001e-3).within(TraceEquivalence.TOLERANCE));
    }

    @Test
    void agreementWithinTheToleranceAtAnyScalePasses() {
        assertTrue(norm(1e-6, 1e-6 * (1 + 5e-10)).within(TraceEquivalence.TOLERANCE));
        assertTrue(norm(300.0, 300.0 * (1 + 5e-10)).within(TraceEquivalence.TOLERANCE));
        assertTrue(norm(2e-9, 2e-9).within(TraceEquivalence.TOLERANCE));
    }

    @Test
    void theScaleIsTheWholeBlockSoASmallChannelIsJudgedAgainstTheLargeOne() {
        var expected = List.of(new double[] {100.0, 1e-6}, new double[] {-50.0, 2e-6});
        var actual = List.of(new double[] {100.0, 1.0001e-6}, new double[] {-50.0, 2e-6});
        assertTrue(TraceEquivalence.norm(expected, actual, 0, 2).within(TraceEquivalence.TOLERANCE),
                "a 1e-10 V error on a 100 V block is within 1e-9 of the block");
        assertFalse(TraceEquivalence.norm(expected, actual, 1, 2).within(TraceEquivalence.TOLERANCE),
                "the same channel alone, as its own block, is not");
    }

    @Test
    void anIdenticallyZeroBlockUsesTheStatedAbsoluteBound() {
        assertTrue(norm(0, 0.5e-12).within(TraceEquivalence.TOLERANCE));
        assertFalse(norm(0, 1e-6).within(TraceEquivalence.TOLERANCE));
        assertEquals(1e-6, norm(0, 1e-6).relative(), "a zero scale reports the absolute difference");
        assertTrue(norm(6.6e-17, 6.7e-17).within(TraceEquivalence.TOLERANCE), "rounding residue is numerically zero");
        assertFalse(norm(6.6e-17, 1e-6).within(TraceEquivalence.TOLERANCE), "but a real difference is still caught");
        assertFalse(norm(2e-12, 2.1e-12).within(TraceEquivalence.TOLERANCE), "above the bound the block is judged relatively");
    }

    @Test
    void aNaNIsNeverEquivalent() {
        assertFalse(norm(1.0, Double.NaN).within(TraceEquivalence.TOLERANCE));
        assertFalse(norm(Double.NaN, 1.0).within(TraceEquivalence.TOLERANCE));
    }
}
