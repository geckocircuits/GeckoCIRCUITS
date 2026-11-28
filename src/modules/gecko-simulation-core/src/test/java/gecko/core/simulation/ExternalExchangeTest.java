package gecko.core.simulation;

import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExternalExchangeTest {
    @Test
    void exchangesValuesInTheSameStepAndQueuesItsSample() throws Exception {
        var model = model();
        try (var host = SessionHost.open(prepare(model), ExecutionOptions.defaults())) {
            assertEquals(1, host.externalInputPorts());
            assertEquals(1, host.externalOutputPorts());
            assertEquals(2, host.externalInputWidth(0));
            assertEquals(2, host.externalOutputWidth(0));
            assertArrayEquals(new double[][]{{2, 6}}, host.stepExternal(new double[][]{{1, 3}}).get(10, TimeUnit.SECONDS));
            assertArrayEquals(new double[][]{{8, -4}}, host.stepExternal(new double[][]{{4, -2}}).get(10, TimeUnit.SECONDS));
            assertEquals(2, host.drainSamples(sample -> { }));
            assertThrows(IllegalArgumentException.class, () -> host.stepExternal(null));
            assertThrows(IllegalArgumentException.class, () -> host.stepExternal(new double[0][]));
            assertThrows(IllegalArgumentException.class, () -> host.stepExternal(new double[][]{{1}}));
            assertThrows(IllegalArgumentException.class, () -> host.externalInputWidth(-1));
            assertThrows(IllegalArgumentException.class, () -> host.externalOutputWidth(1));
        }
    }

    @Test
    void metadataReadsAndExternalSubmissionAreSafeDuringAnOwnerRun() throws Exception {
        var model = model();
        model.setSimulationDuration(1000);
        try (var host = SessionHost.open(prepare(model), new ExecutionOptions(false, 1))) {
            var run = host.run();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (host.status().state() != SimulationSession.State.RUNNING) {
                assertFalse(run.isDone(), "The long run must still be active while metadata is read.");
                assertTrue(System.nanoTime() < deadline, "The owner run must start promptly.");
                Thread.onSpinWait();
            }
            assertEquals(1, host.externalInputPorts());
            assertEquals(1, host.externalOutputPorts());
            assertEquals(2, host.externalInputWidth(0));
            assertEquals(2, host.externalOutputWidth(0));
            var externalStep = host.stepExternal(new double[][]{{5, 7}});
            host.pause();
            assertEquals(SimulationSession.State.PAUSED, run.get(10, TimeUnit.SECONDS).state());
            assertArrayEquals(new double[][]{{10, 14}}, externalStep.get(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void externalInputPortsFollowSavedOrderRatherThanFileOrder() throws Exception {
        var model = new CircuitModel();
        model.setTimeStep(0.1); model.setSimulationDuration(0.3);
        var later = component(23, "later", new String[0], new String[]{"late"});
        later.setParameter("externalWidth", 1); later.setParameter("externalOrder", 1);
        var earlier = component(23, "earlier", new String[0], new String[]{"early"});
        earlier.setParameter("externalWidth", 1); earlier.setParameter("externalOrder", 0);
        var sink = component(22, "sink", new String[]{"early", "late"}, new String[0]);
        var scope = component(5, "scope", new String[]{"early", "late"}, new String[0]);
        model.addControlComponent(later); model.addControlComponent(earlier); model.addControlComponent(sink); model.addControlComponent(scope);
        try (var host = SessionHost.open(prepare(model), ExecutionOptions.defaults())) {
            assertArrayEquals(new double[][]{{10, 20}}, host.stepExternal(new double[][]{{10}, {20}}).get(10, TimeUnit.SECONDS));
        }
    }

    private static CircuitModel model() {
        var model = new CircuitModel(); model.setTimeStep(0.1); model.setSimulationDuration(0.3);
        var from = component(23, "from", new String[0], new String[]{"a", "b"});
        from.setParameter("externalWidth", 2); from.setParameter("externalOrder", 0);
        model.addControlComponent(from);
        model.addControlComponent(component(7, "gainA", new String[]{"a"}, new String[]{"aa"}, 2));
        model.addControlComponent(component(7, "gainB", new String[]{"b"}, new String[]{"bb"}, 2));
        var to = component(22, "to", new String[]{"aa", "bb"}, new String[0]); to.setParameter("externalOrder", 0);
        model.addControlComponent(to);
        model.addControlComponent(component(5, "scope", new String[]{"aa", "bb"}, new String[0]));
        return model;
    }
    private static CircuitModel.ComponentData component(int type, String name, String[] inputs, String[] outputs, double... params) {
        var c = new CircuitModel.ComponentData(type, name); c.setTerminalXLabels(inputs); c.setTerminalYLabels(outputs); c.setNumericParameters(params); return c;
    }
    private static PreparedCircuit prepare(CircuitModel model) {
        var prep = new CircuitPreparation(); var result = prep.prepare(prep.capture(model), PreparationRequest.defaults());
        return assertPrepared(result);
    }
    private static PreparedCircuit assertPrepared(PreparationResult result) {
        if (result instanceof PreparationResult.Ready ready) return ready.execution();
        throw new AssertionError(result.report().toString());
    }
}
