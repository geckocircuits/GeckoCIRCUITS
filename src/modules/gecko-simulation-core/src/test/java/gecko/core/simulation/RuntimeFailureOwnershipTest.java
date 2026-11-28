package gecko.core.simulation;

import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.InitializableAtSimulationStart;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeFailureOwnershipTest {
    @Test
    void initializationFailureClosesResourcesAndPreservesTheOriginalFailure() {
        var primary = new IllegalStateException("initialization");
        var calculator = new FailingCalculator();
        calculator.initialization = primary;
        var runtime = runtime(calculator);
        assertSame(primary, assertThrows(IllegalStateException.class, runtime::initializeAtSimulationStart));
        assertEquals(1, calculator.closes);
        runtime.close();
        assertEquals(1, calculator.closes);
    }

    @Test
    void executionErrorClosesResourcesWithoutSelfSuppression() {
        var primary = new AssertionError("execution");
        var calculator = new FailingCalculator();
        calculator.execution = primary;
        calculator.cleanup = primary;
        var runtime = runtime(calculator);
        assertSame(primary, assertThrows(AssertionError.class,
                () -> runtime.advanceAfterElectricalStep(new CircuitNetlist(), 0)));
        assertEquals(0, primary.getSuppressed().length);
        assertEquals(1, calculator.closes);
        assertThrows(IllegalStateException.class, () -> runtime.applyPendingGates(new CircuitNetlist()));
    }

    @Test
    void checkedResourceFailuresAreReportedAfterEveryResourceCloses() {
        var first = new FailingCalculator();
        var second = new FailingCalculator();
        first.cleanup = new IOException("first close");
        second.cleanup = new IOException("second close");
        var build = new ControlCalculatorFactory.BuildResult(
                ControlNetlist.fromOrderedCalculators(List.of(first, second)), new ControlBlockMapping());
        var runtime = ControlRuntime.fromDeferred(build, 0.1);
        var failure = assertThrows(IllegalStateException.class, runtime::close);
        assertSame(first.cleanup, failure.getCause());
        assertArrayEquals(new Throwable[]{second.cleanup}, failure.getCause().getSuppressed());
        assertEquals(1, first.closes);
        assertEquals(1, second.closes);
    }

    private static ControlRuntime runtime(FailingCalculator calculator) {
        return ControlRuntime.fromDeferred(new ControlCalculatorFactory.BuildResult(
                ControlNetlist.fromOrderedCalculators(List.of(calculator)), new ControlBlockMapping()), 0.1);
    }

    private static final class FailingCalculator extends AbstractControlCalculatable
            implements InitializableAtSimulationStart, AutoCloseable {
        RuntimeException initialization;
        Error execution;
        Throwable cleanup;
        int closes;
        FailingCalculator() { super(0, 1); }
        @Override public void initializeAtSimulationStart(double dt) { if (initialization != null) throw initialization; }
        @Override public void berechneYOUT(double dt) { if (execution != null) throw execution; }
        @Override public void close() throws Exception {
            closes++;
            if (cleanup instanceof Error error) throw error;
            if (cleanup instanceof Exception exception) throw exception;
        }
    }
}
