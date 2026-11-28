package gecko.core.simulation;

import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.InitializableAtSimulationStart;
import gecko.core.control.calculators.NothingToDoCalculator;
import gecko.core.io.CircuitModel;
import java.util.List;
import org.junit.jupiter.api.Test;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;

class ControlRuntimeTimeStepContractTest {
    @Test
    void aBeforeStartStepChangeKeepsInitializationDeferred() throws Exception {
        var observer = new ObservedCalculator();
        var runtime = observing(observer);
        Runnable transition = runtime.prepareTimeStepChange(0.25);
        assertEquals(0, observer.initializations);
        transition.run();
        assertEquals(0, observer.initializations);
        runtime.initializeAtSimulationStart();
        assertEquals(1, observer.initializations);
        assertEquals(0.25, observer.initialStep);
        runtime.advanceAfterElectricalStep(new CircuitNetlist(), 0);
        assertArrayEquals(new float[]{0.25f}, runtime.sampleScope());
        runtime.close();
    }

    @Test
    void aCommittedBeforeStartPlanInvalidatesOtherPlansAndCannotCommitTwice() throws Exception {
        var observer = new ObservedCalculator();
        var runtime = observing(observer);
        Runnable selected = runtime.prepareTimeStepChange(0.25);
        Runnable stale = runtime.prepareTimeStepChange(0.125);
        selected.run();
        assertThrows(IllegalStateException.class, stale::run);
        assertThrows(IllegalStateException.class, selected::run);
        runtime.initializeAtSimulationStart();
        runtime.advanceAfterElectricalStep(new CircuitNetlist(), 0);
        assertEquals(0.25, observer.initialStep);
        assertArrayEquals(new float[]{0.25f}, runtime.sampleScope());
        runtime.close();
    }

    @Test
    void pausedHistoryResamplesAndContinuesOnTheCommittedGrid() {
        var model = new CircuitModel();
        model.addControlComponent(component(58, "time", new double[0], new String[0], "clock"));
        model.addControlComponent(component(25, "delay", new double[]{1}, new String[]{"clock"}, "delayed"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"delayed"}));
        var runtime = ControlRuntime.fromDeferred(model, 0.5);
        var electrical = new CircuitNetlist();
        runtime.advanceAfterElectricalStep(electrical, 0);
        runtime.advanceAfterElectricalStep(electrical, 0.5);
        runtime.advanceAfterElectricalStep(electrical, 1);
        runtime.tearDownOnPause();
        Runnable transition = runtime.prepareTimeStepChange(0.25);
        assertArrayEquals(new float[]{0}, runtime.sampleScope());
        transition.run();
        runtime.initializeAtSimulationStart();
        runtime.advanceAfterElectricalStep(electrical, 1.25);
        assertArrayEquals(new float[]{0.25f}, runtime.sampleScope());
        runtime.close();
    }

    @Test
    void aPreparedPlanCannotInitializeAClosedRuntime() throws Exception {
        var observer = new ObservedCalculator();
        var runtime = observing(observer);
        Runnable transition = runtime.prepareTimeStepChange(0.25);
        runtime.close();
        assertThrows(IllegalStateException.class, transition::run);
        assertEquals(0, observer.initializations);
        assertEquals(1, observer.closes);
    }

    private static ControlRuntime observing(ObservedCalculator observer) throws Exception {
        var scope = new NothingToDoCalculator(1, 0);
        scope.setInputSignal(0, observer, 0);
        var mapping = new ControlBlockMapping();
        mapping.addScopeInfo(scope, new String[]{"step"}, "scope", 92);
        return ControlRuntime.fromDeferred(new ControlCalculatorFactory.BuildResult(
                ControlNetlist.fromOrderedCalculators(List.of(observer, scope)), mapping), 0.5);
    }

    private static final class ObservedCalculator extends AbstractControlCalculatable
            implements InitializableAtSimulationStart, AutoCloseable {
        private int initializations;
        private int closes;
        private double initialStep;

        ObservedCalculator() { super(0, 1); }
        @Override public void initializeAtSimulationStart(double dt) {
            initializations++;
            initialStep = dt;
            _outputSignal[0][0] = 0;
        }
        @Override public void berechneYOUT(double dt) { _outputSignal[0][0] = dt; }
        @Override public void close() { closes++; }
    }
}
