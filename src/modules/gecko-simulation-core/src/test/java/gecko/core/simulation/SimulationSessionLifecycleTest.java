package gecko.core.simulation;

import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.InitializableAtSimulationStart;
import gecko.core.control.calculators.NothingToDoCalculator;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.ExecutionOptions;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SimulationSessionLifecycleTest {
    @Test
    void closingAnUnusedSessionReleasesResourcesWithoutStartingCalculations() throws Exception {
        var calculator = new LifecycleCalculator();
        var session = session(calculator);
        assertEquals(0, calculator.initializations);
        session.close();
        session.close();
        assertEquals(0, calculator.initializations);
        assertEquals(0, calculator.teardowns);
        assertEquals(1, calculator.closes);
        assertEquals(SimulationSession.State.CLOSED, session.status().state());
    }

    @Test
    void pauseResumeAndExtensionRetainHistoryAndEndEachActiveSegmentOnce() throws Exception {
        var calculator = new LifecycleCalculator();
        var session = session(calculator);
        assertEquals(0, calculator.initializations);
        session.run(() -> calculator._outputSignal[0][0] == 1);
        assertEquals(1, calculator.initializations);
        assertEquals(1, calculator.teardowns);
        assertEquals(0, calculator.closes);
        session.run();
        assertEquals(1, calculator.initializations);
        assertEquals(2, calculator.teardowns);
        assertArrayEquals(new float[]{1, 2, 3}, session.observe(0, 3).values()[0]);
        session.extend(0.5);
        session.run();
        assertEquals(1, calculator.initializations);
        assertEquals(3, calculator.teardowns);
        assertArrayEquals(new float[]{1, 2, 3, 4, 5}, session.observe(0, 5).values()[0]);
        session.close();
        session.close();
        assertEquals(3, calculator.teardowns);
        assertEquals(1, calculator.closes);
    }

    @Test
    void failedInitializationFinalizesItsRecordingAndKeepsThePrimaryFailure() throws Exception {
        var calculator = new LifecycleCalculator();
        var primary = new IllegalStateException("initializer failed");
        var teardown = new IllegalStateException("teardown failed");
        var close = new IllegalStateException("close failed");
        calculator.initializationFailure = primary;
        calculator.teardownFailure = teardown;
        calculator.closeFailure = close;
        var session = session(calculator);
        calculator.initializing = () -> assertEquals(gecko.core.datacontainer.ContainerStatus.RUNNING,
                session.recording().getContainerStatus());
        assertSame(primary, assertThrows(IllegalStateException.class, session::step));
        assertEquals(0, session.status().committedSteps());
        assertEquals(SimulationSession.State.FAILED, session.status().state());
        assertEquals(gecko.core.datacontainer.ContainerStatus.PAUSED, session.recording().getContainerStatus());
        assertEquals(1, primary.getSuppressed().length);
        assertSame(teardown, primary.getSuppressed()[0]);
        assertSame(close, teardown.getSuppressed()[0]);
        session.close();
        session.close();
        assertEquals(1, calculator.initializations);
        assertEquals(1, calculator.teardowns);
        assertEquals(1, calculator.closes);
    }

    @Test
    void aDeliveryFailureKeepsTheCommittedSampleAndSuppressesTeardownFailure() throws Exception {
        var calculator = new LifecycleCalculator();
        var primary = new IllegalStateException("delivery failed");
        var teardown = new IllegalStateException("teardown failed");
        var close = new IllegalStateException("close failed");
        calculator.teardownFailure = teardown;
        calculator.closeFailure = close;
        var session = session(calculator);
        var failure = assertThrows(IllegalStateException.class,
                () -> session.run(() -> false, sample -> { throw primary; }));
        assertSame(primary, failure);
        assertSame(teardown, primary.getSuppressed()[0]);
        assertSame(close, teardown.getSuppressed()[0]);
        assertEquals(1, session.status().committedSteps());
        assertArrayEquals(new float[]{1}, session.observe(0, 1).values()[0]);
        session.close();
        assertEquals(1, calculator.teardowns);
        assertEquals(1, calculator.closes);
    }

    @Test
    void aCloseRequestDoesNotWaitForAnActiveInitializationCallback() throws Exception {
        var calculator = new LifecycleCalculator();
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        calculator.initializing = () -> {
            entered.countDown();
            try { assertTrue(release.await(5, java.util.concurrent.TimeUnit.SECONDS)); }
            catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
        };
        var session = session(calculator);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var step = executor.submit((java.util.concurrent.Callable<SimulationSession.Sample>) session::step);
            assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(SimulationSession.State.RUNNING, session.status().state());
            var close = executor.submit(session::close);
            close.get(2, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(0, calculator.closes);
            release.countDown();
            assertEquals(1, step.get(5, java.util.concurrent.TimeUnit.SECONDS).values()[0]);
            assertEquals(SimulationSession.State.CLOSED, session.status().state());
            assertEquals(1, calculator.teardowns);
            assertEquals(1, calculator.closes);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));
            session.close();
        }
    }

    @Test
    void anElectricalFailureRemainsPrimaryWhenControlTeardownAlsoFails() throws Exception {
        var calculator = new LifecycleCalculator();
        var teardown = new IllegalStateException("teardown failed");
        var close = new IllegalStateException("close failed");
        calculator.teardownFailure = teardown;
        calculator.closeFailure = close;
        var model = new CircuitModel();
        model.addCircuitComponent(ControlReviewRegressionTest.component(4, "source1",
                new double[]{401, 10}, new String[]{"1"}, "0"));
        model.addCircuitComponent(ControlReviewRegressionTest.component(4, "source2",
                new double[]{401, 20}, new String[]{"1"}, "0"));
        var session = session(calculator, model);
        var primary = assertThrows(RuntimeException.class, session::step);
        assertNotSame(teardown, primary);
        assertTrue(primary.getMessage().toLowerCase(java.util.Locale.ROOT).contains("singular"), primary.getMessage());
        assertSame(teardown, primary.getSuppressed()[0]);
        assertSame(close, teardown.getSuppressed()[0]);
        assertEquals(0, session.status().committedSteps());
        assertEquals(gecko.core.datacontainer.ContainerStatus.PAUSED, session.recording().getContainerStatus());
        session.close();
        assertEquals(1, calculator.teardowns);
        assertEquals(1, calculator.closes);
    }

    @Test
    void failedConstructionReleasesAnUnusedRuntimeAndKeepsTheTopologyFailure() {
        var calculator = new LifecycleCalculator();
        var close = new IllegalStateException("close failed");
        calculator.closeFailure = close;
        var model = new CircuitModel();
        model.addCircuitComponent(ControlReviewRegressionTest.component(1, "floating resistor",
                new double[]{10}, new String[]{"1"}, "2"));
        var primary = assertThrows(gecko.core.circuit.netlist.TopologyValidationException.class,
                () -> session(calculator, model));
        assertSame(close, primary.getSuppressed()[0]);
        assertEquals(0, calculator.initializations);
        assertEquals(0, calculator.teardowns);
        assertEquals(1, calculator.closes);
    }

    @Test
    void aFailedResourceCloseIsAttemptedOnceAndLeavesTheSessionTerminal() throws Exception {
        var calculator = new LifecycleCalculator();
        var close = new IllegalStateException("close failed");
        calculator.closeFailure = close;
        var session = session(calculator);
        assertSame(close, assertThrows(IllegalStateException.class, session::close));
        assertEquals(SimulationSession.State.CLOSED, session.status().state());
        assertDoesNotThrow(session::close);
        assertThrows(IllegalStateException.class, session::step);
        assertEquals(1, calculator.closes);
        assertEquals(0, calculator.teardowns);
    }

    @Test
    void automaticWarmupKeepsOneActiveControlSegmentUntilMainCompletion() throws Exception {
        var calculator = new LifecycleCalculator();
        var settings = new SolverSettingsCore(SolverType.SOLVER_BE, 0.125, 0.5).withPreCalculation(0.3, 0.2);
        var session = session(calculator, new CircuitModel(), settings);
        var teardowns = new java.util.ArrayList<Integer>();
        session.run(() -> false, sample -> teardowns.add(calculator.teardowns));
        assertEquals(List.of(0, 0, 0, 0, 0, 1), teardowns);
        assertEquals(1, calculator.initializations);
        assertEquals(1, calculator.teardowns);
        assertArrayEquals(new float[]{3, 4, 5, 6}, session.observeCurrentSegment(0, 10).values()[0]);
        session.close();
        assertEquals(1, calculator.teardowns);
        assertEquals(1, calculator.closes);
    }

    @Test
    void traceDeliveryReleasesOperationOwnershipAndRejectsReentrantSteps() throws Exception {
        var calculator = new LifecycleCalculator();
        var model = new CircuitModel();
        model.addCircuitComponent(ControlReviewRegressionTest.component(4, "source", new double[]{401, 6}, new String[]{"1"}, "0"));
        model.addCircuitComponent(ControlReviewRegressionTest.component(1, "load", new double[]{10}, new String[]{"1"}, "0"));
        var session = session(calculator, model);
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var step = executor.submit(() -> session.step((time, volts, currents, netlist) -> {
                assertThrows(IllegalStateException.class, session::step);
                entered.countDown();
                try { assertTrue(release.await(5, java.util.concurrent.TimeUnit.SECONDS)); }
                catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
            }));
            assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS));
            var observed = executor.submit(() -> session.observeCurrentSegment(0, 1));
            assertArrayEquals(new float[]{1}, observed.get(2, java.util.concurrent.TimeUnit.SECONDS).values()[0]);
            release.countDown();
            assertEquals(1, step.get(5, java.util.concurrent.TimeUnit.SECONDS).values()[0]);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));
            session.close();
        }
    }

    @Test
    void aPredictablyInvalidMainDelayGridRejectsBeforeWarmupCallbacks() throws Exception {
        var calculator = new LifecycleCalculator();
        var delay = new gecko.core.control.calculators.DelayCalculator(1000);
        delay.setInputSignal(0, calculator, 0);
        var runtime = observingDelay(calculator, delay);
        var settings = new SolverSettingsCore(SolverType.SOLVER_BE, 1e-9, 1e-9).withPreCalculation(1, 1);
        var opened = new java.util.concurrent.atomic.AtomicReference<SimulationSession>();
        try {
            assertThrows(IllegalArgumentException.class, () -> opened.set(SimulationSession.fromModel(
                    new CircuitModel(), settings, ExecutionOptions.defaults(), runtime)));
            assertEquals(0, calculator.initializations);
            assertEquals(0, calculator.teardowns);
            assertEquals(1, calculator.closes);
        } finally {
            if (opened.get() != null) opened.get().close();
            else runtime.close();
        }
    }

    @Test
    void anUnexpectedMainTransitionFailureFinalizesAndClosesTheWarmup() throws Exception {
        var calculator = new LifecycleCalculator();
        var delay = new gecko.core.control.calculators.DelayCalculator(1);
        delay.setInputSignal(0, calculator, 0);
        calculator.calculating = () -> { if (calculator._outputSignal[0][0] == 2) delay.setDelayTime(Double.MAX_VALUE); };
        var runtime = observingDelay(calculator, delay);
        var settings = new SolverSettingsCore(SolverType.SOLVER_BE, 0.125, 0.5).withPreCalculation(1, 1);
        var session = SimulationSession.fromModel(new CircuitModel(), settings, ExecutionOptions.defaults(), runtime);
        assertThrows(IllegalArgumentException.class, session::run);
        assertEquals(SimulationSession.State.FAILED, session.status().state());
        assertEquals(2, session.status().committedSteps());
        assertEquals(gecko.core.datacontainer.ContainerStatus.PAUSED, session.recording().getContainerStatus());
        assertEquals(1, calculator.teardowns);
        assertEquals(1, calculator.closes);
        session.close();
        assertEquals(1, calculator.closes);
    }

    private static ControlRuntime observingDelay(LifecycleCalculator calculator,
            gecko.core.control.calculators.DelayCalculator delay) throws Exception {
        var scope = new NothingToDoCalculator(1, 0);
        scope.setInputSignal(0, calculator, 0);
        var mapping = new ControlBlockMapping();
        mapping.addScopeInfo(scope, new String[]{"value"}, "scope", 91);
        return ControlRuntime.fromDeferred(new ControlCalculatorFactory.BuildResult(
                ControlNetlist.fromOrderedCalculators(List.of(calculator, delay, scope)), mapping), 1);
    }

    private static SimulationSession session(LifecycleCalculator calculator) throws Exception {
        return session(calculator, new CircuitModel());
    }

    private static SimulationSession session(LifecycleCalculator calculator, CircuitModel model) throws Exception {
        return session(calculator, model, new SolverSettingsCore(SolverType.SOLVER_BE, 0.125, 0.25));
    }

    private static SimulationSession session(LifecycleCalculator calculator, CircuitModel model,
            SolverSettingsCore settings) throws Exception {
        var scope = new NothingToDoCalculator(1, 0);
        scope.setInputSignal(0, calculator, 0);
        var mapping = new ControlBlockMapping();
        mapping.addScopeInfo(scope, new String[]{"value"}, "scope", 91);
        var build = new ControlCalculatorFactory.BuildResult(
                ControlNetlist.fromOrderedCalculators(List.of(calculator, scope)), mapping);
        double initialStep = settings.getPreCalculationTime() > 0 ? settings.getPreCalculationStepWidth() : settings.getStepWidth();
        return SimulationSession.fromModel(model, settings, ExecutionOptions.defaults(),
                ControlRuntime.fromDeferred(build, initialStep));
    }

    private static final class LifecycleCalculator extends AbstractControlCalculatable
            implements InitializableAtSimulationStart, AutoCloseable {
        private int initializations;
        private int teardowns;
        private int closes;
        private RuntimeException initializationFailure;
        private RuntimeException teardownFailure;
        private RuntimeException closeFailure;
        private Runnable initializing = () -> { };
        private Runnable calculating = () -> { };

        LifecycleCalculator() { super(0, 1); }
        @Override public void initializeAtSimulationStart(double dt) {
            initializations++;
            initializing.run();
            if (initializationFailure != null) throw initializationFailure;
            _outputSignal[0][0] = 0;
        }
        @Override public void berechneYOUT(double dt) { _outputSignal[0][0]++; calculating.run(); }
        @Override public void tearDownOnPause() {
            teardowns++;
            if (teardownFailure != null) throw teardownFailure;
        }
        @Override public void close() {
            closes++;
            if (closeFailure != null) throw closeFailure;
        }
    }
}
