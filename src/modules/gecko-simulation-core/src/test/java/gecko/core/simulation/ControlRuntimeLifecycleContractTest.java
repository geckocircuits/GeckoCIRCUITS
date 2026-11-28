package gecko.core.simulation;

import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.InitializableAtSimulationStart;
import gecko.core.control.calculators.NothingToDoCalculator;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ControlRuntimeLifecycleContractTest {
    @Test
    void deferredStartAndPausePreserveHistoryAndTearDownEachActiveSegmentOnce() throws Exception {
        var calculator = new LifecycleCalculator();
        var runtime = runtime(calculator);
        assertEquals(0, calculator.initializations);
        runtime.initializeAtSimulationStart();
        runtime.initializeAtSimulationStart();
        assertEquals(1, calculator.initializations);
        runtime.advanceAfterElectricalStep(new CircuitNetlist(), 0);
        assertArrayEquals(new float[]{1}, runtime.sampleScope());

        runtime.tearDownOnPause();
        runtime.tearDownOnPause();
        assertEquals(1, calculator.teardowns);
        runtime.initializeAtSimulationStart();
        assertEquals(1, calculator.initializations);
        runtime.advanceAfterElectricalStep(new CircuitNetlist(), 0.1);
        assertArrayEquals(new float[]{2}, runtime.sampleScope());

        runtime.close();
        runtime.close();
        assertEquals(2, calculator.teardowns);
        assertEquals(1, calculator.closes);
        assertThrows(IllegalStateException.class,
                () -> runtime.advanceAfterElectricalStep(new CircuitNetlist(), 0.2));
    }

    @Test
    void closingAnUnusedRuntimeDoesNotStartOrTearDownCalculators() throws Exception {
        var calculator = new LifecycleCalculator();
        var runtime = runtime(calculator);
        runtime.close();
        runtime.close();
        assertEquals(0, calculator.initializations);
        assertEquals(0, calculator.teardowns);
        assertEquals(1, calculator.closes);
        assertThrows(IllegalStateException.class, runtime::initializeAtSimulationStart);
    }

    private static ControlRuntime runtime(LifecycleCalculator calculator) throws Exception {
        var scope = new NothingToDoCalculator(1, 0);
        scope.setInputSignal(0, calculator, 0);
        var mapping = new ControlBlockMapping();
        mapping.addScopeInfo(scope, new String[]{"value"}, "scope", 91);
        var build = new ControlCalculatorFactory.BuildResult(
                ControlNetlist.fromOrderedCalculators(List.of(calculator, scope)), mapping);
        return ControlRuntime.fromDeferred(build, 0.1);
    }

    private static final class LifecycleCalculator extends AbstractControlCalculatable
            implements InitializableAtSimulationStart, AutoCloseable {
        private int initializations;
        private int teardowns;
        private int closes;

        LifecycleCalculator() { super(0, 1); }
        @Override public void initializeAtSimulationStart(double dt) {
            initializations++;
            _outputSignal[0][0] = 0;
        }
        @Override public void berechneYOUT(double dt) { _outputSignal[0][0]++; }
        @Override public void tearDownOnPause() { teardowns++; }
        @Override public void close() { closes++; }
    }
}
