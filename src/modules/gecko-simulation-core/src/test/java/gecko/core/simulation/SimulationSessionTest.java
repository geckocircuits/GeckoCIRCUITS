package gecko.core.simulation;

import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static gecko.core.simulation.ControlReviewRegressionTest.component;

class SimulationSessionTest {
    @Test
    void singleStepsRetainControlHistoryAndMatchTheBatchEngine() {
        var model = new CircuitModel();
        model.setTimeStep(0.125);
        model.setSimulationDuration(0.5);
        model.addControlComponent(component(3, "constant", new double[]{1}, new String[0], "drive"));
        model.addControlComponent(component(10, "integrator", new double[]{0, 1}, new String[]{"drive"}, "integrated"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"integrated"}));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        var expected = new HeadlessSimulationEngine().runSimulation(
                ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(expected.isSuccess(), expected.getErrorMessage());
        try (var session = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
            var first = session.step();
            var second = session.step();
            assertEquals(0, first.time());
            assertEquals(0.125, second.time());
            assertEquals(expected.getSignalData(0)[0], first.values()[0]);
            assertEquals(expected.getSignalData(0)[1], second.values()[0]);
            assertEquals(0.0625f, first.values()[0]);
            assertEquals(0.1875f, second.values()[0]);
            assertEquals(2, session.status().committedSteps());
            assertArrayEquals(new double[]{0, 0.125}, session.observe(0, 2).times());
        }
    }
    @Test
    void completedSegmentsCanExtendWithoutResettingHistoryOrChangingOldResults() {
        var model = new CircuitModel();
        model.setTimeStep(0.125);
        model.setSimulationDuration(0.125);
        model.addControlComponent(component(3, "constant", new double[]{1}, new String[0], "drive"));
        model.addControlComponent(component(10, "integrator", new double[]{0, 1}, new String[]{"drive"}, "integrated"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"integrated"}));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        try (var session = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
            session.step();
            var second = session.step();
            var oldRecording = session.recording();
            assertEquals(SimulationSession.State.COMPLETED, session.status().state());
            session.extend(0.5);
            var third = session.step();
            session.step();
            session.step();
            assertEquals(0.25, third.time());
            assertEquals(0.1875f, second.values()[0]);
            assertEquals(0.3125f, third.values()[0]);
            assertEquals(5, session.status().committedSteps());
            assertArrayEquals(new double[]{0, 0.125, 0.25, 0.375, 0.5}, session.observe(0, 20).times());
            assertEquals(1, oldRecording.getMaximumTimeIndex(0));
            assertEquals(SimulationSession.State.COMPLETED, session.status().state());
        }
    }

    @Test
    void aPausedRunContinuesTheSameSegment() {
        var model = new CircuitModel();
        model.setTimeStep(0.125);
        model.setSimulationDuration(0.5);
        model.addControlComponent(component(4, "time", new double[]{400, 1, 1, 0, 0, 0.5}, new String[0], "clock"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"clock"}));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        try (var session = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
            var polls = new java.util.concurrent.atomic.AtomicInteger();
            var paused = session.run(() -> polls.incrementAndGet() > 2);
            assertEquals(SimulationSession.State.PAUSED, paused.state());
            assertEquals(2, paused.committedSteps());
            var completed = session.run();
            assertEquals(SimulationSession.State.COMPLETED, completed.state());
            assertEquals(5, completed.committedSteps());
            assertArrayEquals(new double[]{0, 0.125, 0.25, 0.375, 0.5}, session.observe(0, 10).times());
        }
    }

    @Test
    void backwardEulerCapacitorHistorySurvivesManualStepsAndContinuation() {
        var model = new CircuitModel();
        model.setTimeStep(0.125);
        model.setSimulationDuration(0.125);
        model.setSolverType(gecko.core.allg.SolverType.SOLVER_BE);
        model.setDataContainerSignals(new String[]{"ground", "source", "capacitor"});
        model.addCircuitComponent(component(4, "source", new double[]{401, 10}, new String[]{"1"}, "0"));
        model.addCircuitComponent(component(1, "resistor", new double[]{1}, new String[]{"1"}, "2"));
        model.addCircuitComponent(component(3, "capacitor", new double[]{1, 0}, new String[]{"2"}, "0"));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        try (var session = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
            // A 10 V source drives 1 ohm and 1 farad. Backward Euler gives V=(Vprevious+1.25)/1.125.
            assertEquals(1.1111111111111112f, session.step().values()[2], 1e-6);
            assertEquals(2.0987654320987654f, session.step().values()[2], 1e-6);
            session.extend(0.375);
            assertEquals(2.976680384087792f, session.step().values()[2], 1e-6);
            assertEquals(3.75704923030026f, session.step().values()[2], 1e-6);
        }
    }

    @Test
    void aFrontendCallbackCanCloseItsSessionAndRunAnIndependentReplacement() {
        var model = new CircuitModel();
        model.setTimeStep(0.125);
        model.setSimulationDuration(0.25);
        model.addControlComponent(component(3, "constant", new double[]{7}, new String[0], "signal"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"signal"}));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        try (var original = SimulationSession.open(ready.execution(), ExecutionOptions.defaults());
                var replacement = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
            var callbacks = new java.util.concurrent.atomic.AtomicInteger();
            var stopped = original.run(() -> false, sample -> {
                assertEquals(1, original.status().committedSteps());
                assertEquals(7, sample.values()[0]);
                assertThrows(IllegalStateException.class, original::step);
                original.close();
                assertEquals(SimulationSession.State.COMPLETED, replacement.run().state());
                callbacks.incrementAndGet();
            });
            assertEquals(1, callbacks.get());
            assertEquals(SimulationSession.State.CLOSED, stopped.state());
            assertEquals(3, replacement.status().committedSteps());
            assertArrayEquals(new float[]{7, 7, 7}, replacement.observe(0, 3).values()[0]);
        }
    }

    @Test
    void closedSessionsKeepDetachedBoundedObservations() {
        var model = new CircuitModel();
        model.setTimeStep(0.125);
        model.setSimulationDuration(0.25);
        model.addControlComponent(component(3, "constant", new double[]{7}, new String[0], "signal"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"signal"}));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        var session = SimulationSession.open(ready.execution(), ExecutionOptions.defaults());
        session.run();
        session.close();
        session.close();
        assertEquals(SimulationSession.State.CLOSED, session.status().state());
        var window = session.observe(1, 1);
        window.times()[0] = 999;
        window.values()[0][0] = 999;
        assertArrayEquals(new double[]{0.125}, window.times());
        assertArrayEquals(new float[]{7}, window.values()[0]);
        assertArrayEquals(new float[]{7}, session.observe(1, 1).values()[0]);
        assertEquals(0, session.observe(99, 10).times().length);
        assertThrows(IllegalArgumentException.class, () -> session.observe(0, 10001));
        assertThrows(IllegalStateException.class, session::step);
    }

    @Test
    void independentSessionsCanCalculateTheSamePreparedCircuitConcurrently() throws Exception {
        var model = new CircuitModel();
        model.setTimeStep(0.125);
        model.setSimulationDuration(0.25);
        model.addControlComponent(component(3, "constant", new double[]{1}, new String[0], "drive"));
        model.addControlComponent(component(10, "integrator", new double[]{0, 1}, new String[]{"drive"}, "integrated"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"integrated"}));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var barrier = new java.util.concurrent.CyclicBarrier(2);
        try (var first = SimulationSession.open(ready.execution(), ExecutionOptions.defaults());
                var second = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
            java.util.function.Consumer<SimulationSession.Sample> listener = sample -> {
                if (sample.time() == 0) {
                    try { barrier.await(5, java.util.concurrent.TimeUnit.SECONDS); }
                    catch (Exception failure) { throw new AssertionError(failure); }
                }
            };
            var firstRun = executor.submit(() -> first.run(() -> false, listener));
            var secondRun = executor.submit(() -> second.run(() -> false, listener));
            assertEquals(SimulationSession.State.COMPLETED, firstRun.get(10, java.util.concurrent.TimeUnit.SECONDS).state());
            assertEquals(SimulationSession.State.COMPLETED, secondRun.get(10, java.util.concurrent.TimeUnit.SECONDS).state());
            assertArrayEquals(new float[]{0.0625f, 0.1875f, 0.3125f}, first.observe(0, 3).values()[0]);
            assertArrayEquals(new float[]{0.0625f, 0.1875f, 0.3125f}, second.observe(0, 3).values()[0]);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));
        }
    }

    @Test
    void deferredElectricalTracesOwnTheirNetlistAndTimestamp() {
        var model = new CircuitModel();
        model.setTimeStep(0.125);
        model.setSimulationDuration(0.25);
        model.addCircuitComponent(component(4, "source", new double[]{401, 10}, new String[]{"1"}, "0"));
        model.addCircuitComponent(component(1, "load", new double[]{10}, new String[]{"1"}, "0"));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        var captured = new java.util.ArrayList<gecko.core.circuit.netlist.CircuitNetlist>();
        var values = new java.util.ArrayList<Double>();
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, volts, currents, netlist) -> {
            assertEquals(time, netlist.getSimulationTime());
            int source = netlist.getElementIndexByName("source");
            values.add(volts[netlist.getNodeX(source)]);
            captured.add(netlist);
            netlist.setElementParam(source, 1, 999);
            volts[netlist.getNodeX(source)] = 999;
            currents[netlist.getElementIndexByName("load")] = 999;
        });
        var result = engine.runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(java.util.List.of(10.0, 10.0, 10.0), values);
        assertNotSame(captured.get(0), captured.get(1));
        assertEquals(0, captured.get(0).getSimulationTime());
        assertEquals(0.125, captured.get(1).getSimulationTime());
    }

}
