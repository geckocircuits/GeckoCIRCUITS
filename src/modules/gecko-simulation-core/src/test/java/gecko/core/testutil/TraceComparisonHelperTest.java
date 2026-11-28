package gecko.core.testutil;

import gecko.core.testutil.TraceComparisonHelper.StepData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TraceComparisonHelperTest {
    private static final double TOLERANCE = 0.01;

    private static StepData step(double time, double[] voltages, Map<String, Double> gates) {
        return new StepData(time, voltages, new double[]{-2}, Map.of("S.1", 0.01),
                Map.of("D.1", 1e7), gates);
    }

    private static StepData step(double time) {
        return step(time, new double[]{0, 48}, Map.of("S.1", 1.0));
    }

    @Test
    void matchingCompleteTracesPass() {
        assertTrue(TraceComparisonHelper.compare(List.of(step(0)), List.of(step(0)), TOLERANCE).match());
    }

    @Test
    void emptyTracesCannotPassVerification() {
        assertFalse(TraceComparisonHelper.compare(List.of(), List.of(), TOLERANCE).match());
    }

    @Test
    void truncatedTraceCannotPassItsMatchingPrefix() {
        assertFalse(TraceComparisonHelper.compare(List.of(step(0), step(1e-6)),
                List.of(step(0)), TOLERANCE).match());
    }

    @Test
    void samplesMustReferToTheSameTime() {
        assertFalse(TraceComparisonHelper.compare(List.of(step(1e-6)),
                List.of(step(2e-6)), TOLERANCE).match());
    }

    @Test
    void missingNodeCannotPassItsMatchingPrefix() {
        assertFalse(TraceComparisonHelper.compare(List.of(step(0)),
                List.of(step(0, new double[]{0}, Map.of("S.1", 1.0))), TOLERANCE).match());
    }

    @Test
    void missingNamedSignalCannotBeSkipped() {
        assertFalse(TraceComparisonHelper.compare(List.of(step(0)),
                List.of(step(0, new double[]{0, 48}, Map.of())), TOLERANCE).match());
    }

    @Test
    void changedGateMustFailEvenWhenResistanceMatches() {
        assertFalse(TraceComparisonHelper.compare(List.of(step(0)),
                List.of(step(0, new double[]{0, 48}, Map.of("S.1", 0.0))), TOLERANCE).match());
    }
}
