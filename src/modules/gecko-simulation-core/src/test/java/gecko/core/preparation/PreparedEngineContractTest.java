package gecko.core.preparation;

import gecko.core.simulation.HeadlessSimulationEngine;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PreparedEngineContractTest {
    @Test
    void invalidPreparedArgumentsReleaseTheEngineAndResetTheNextRunsProgress() {
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(CircuitPreparationTest.resistorCircuit());
        var ready = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot, PreparationRequest.defaults()));
        var engine = new HeadlessSimulationEngine();
        var missingCircuit = assertDoesNotThrow(() -> engine.runSimulation((PreparedCircuit) null, ExecutionOptions.defaults(), () -> false));
        assertFalse(missingCircuit.isSuccess());
        assertEquals(HeadlessSimulationEngine.EngineState.IDLE, engine.getState());
        assertTrue(engine.runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false).isSuccess());
        var missingOptions = assertDoesNotThrow(() -> engine.runSimulation(ready.execution(), null, () -> false));
        assertFalse(missingOptions.isSuccess());
        assertEquals(HeadlessSimulationEngine.EngineState.IDLE, engine.getState());
        assertEquals(0, engine.getEndTime());
        assertEquals(0, engine.getCurrentStep());
        var shorter = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(new SolverOverrides(null, null, 0.1), Map.of())));
        engine.setStepTraceListener((time, volts, amps, netlist) -> {
            if (time == 0) {
                assertEquals(0, engine.getCurrentStep());
                assertEquals(0.1, engine.getEndTime());
                assertEquals(2, engine.getDetailedProgress().getTotalSteps());
            }
        });
        var result = engine.runSimulation(shorter.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(2, result.getTotalTimeSteps());
    }

    @Test
    void legacyFileAdapterUsesTheSameNormalizationAndRejectsUnknownOverrides(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        String text = "dt 0.125\ntDURATION 0.25\n"
                + SourcePreparationTest.element("LK", 4, "source", "402 6 1 2 1.5707963267948966", "/1", "/0", "")
                + SourcePreparationTest.element("LK", 1, "R", "10", "/1", "/0", "");
        var file = directory.resolve("saved.ipes");
        java.nio.file.Files.writeString(file, text);
        var engine = new HeadlessSimulationEngine();
        var values = new java.util.ArrayList<Double>();
        engine.setStepTraceListener((time, volts, amps, netlist) ->
                values.add(volts[netlist.getNodeX(netlist.getElementIndexByName("source"))]));
        var run = engine.runSimulation(gecko.core.simulation.SimulationConfig.builder().circuitFile(file.toString()).build());
        assertTrue(run.isSuccess(), run.getErrorMessage());
        assertEquals(-4, values.get(0), 1e-10);
        assertEquals(-2.242640687119285, values.get(1), 1e-10);
        assertEquals(2, values.get(2), 1e-10);
        var rejected = engine.runSimulation(gecko.core.simulation.SimulationConfig.builder()
                .circuitFile(file.toString()).withParameter("R.unknown", 2).build());
        assertFalse(rejected.isSuccess());
        assertTrue(rejected.getErrorMessage().contains("UNKNOWN_PARAMETER"));
        assertEquals(HeadlessSimulationEngine.EngineState.IDLE, engine.getState());
    }
    static gecko.core.io.CircuitModel historyAndGateCircuit(gecko.core.allg.SolverType solver) {
        var model = CircuitPreparationTest.resistorCircuit();
        model.setSolverType(solver);
        model.setTimeStep(0.125);
        model.setSimulationDuration(0.5);
        var resistor = model.getCircuitComponents().get(1);
        resistor.setNumericParameters(new double[]{1});
        resistor.setParameter("param0", 1.0);
        resistor.setTerminalYLabels(new String[]{"2"});
        model.addCircuitComponent(CircuitPreparationTest.component(2, "L", new double[]{0.5, 0.2}, "2", "3"));
        model.addCircuitComponent(CircuitPreparationTest.component(3, "C", new double[]{0.1, 1}, "3", "0"));
        model.addCircuitComponent(CircuitPreparationTest.component(7, "switch", new double[]{1000, 1, 1000}, "3", "0"));
        model.addControlComponent(CircuitPreparationTest.component(4, "command", new double[]{404, 1, 2, 0, 0, 0.5}, "", "drive"));
        var gate = CircuitPreparationTest.component(6, "gate", new double[0], "drive", "");
        gate.setParameterStrings(new String[]{"switch"});
        model.addControlComponent(gate);
        model.addControlComponent(CircuitPreparationTest.component(5, "scope", new double[0], "drive", ""));
        return model;
    }

    static java.util.List<double[]> historyTrace(PreparedCircuit prepared, java.util.concurrent.CyclicBarrier barrier) {
        var trace = new java.util.ArrayList<double[]>();
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, volts, amps, netlist) -> {
            if (time == 0 && barrier != null) {
                try { barrier.await(10, java.util.concurrent.TimeUnit.SECONDS); }
                catch (Exception error) { throw new AssertionError("Concurrent runs did not reach the first sample", error); }
            }
            int capacitor = netlist.getElementIndexByName("C");
            trace.add(new double[]{time, volts[netlist.getNodeX(capacitor)],
                    amps[netlist.getElementIndexByName("L")], amps[netlist.getElementIndexByName("R.1")],
                    netlist.getParameter(netlist.getElementIndexByName("switch"))[0], 0});
        });
        var result = engine.runSimulation(prepared, ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        var controlSamples = result.getSignalData(0);
        assertEquals(5, controlSamples.length);
        for (int index = 0; index < trace.size(); index++) trace.get(index)[5] = controlSamples[index];
        assertEquals(5, trace.size());
        return trace;
    }

    static void assertTraceEquals(java.util.List<double[]> expected, java.util.List<double[]> actual) {
        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++) assertArrayEquals(expected.get(index), actual.get(index), 1e-10);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(gecko.core.allg.SolverType.class)
    void repeatedAndConcurrentRunsOwnTheirElectricalHistoryAndGateState(gecko.core.allg.SolverType solver) throws Exception {
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(historyAndGateCircuit(solver));
        var ready = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot, PreparationRequest.defaults()));
        var expected = historyTrace(ready.execution(), null);
        assertTrue(expected.stream().anyMatch(row -> row[4] == 1));
        assertTrue(expected.stream().anyMatch(row -> row[4] == 1000));
        assertTrue(expected.stream().anyMatch(row -> row[5] == 0));
        assertTrue(expected.stream().anyMatch(row -> row[5] == 1));
        assertTrue(Math.abs(expected.getFirst()[1] - expected.getLast()[1]) > 0.1);
        assertTraceEquals(expected, historyTrace(ready.execution(), null));
        var barrier = new java.util.concurrent.CyclicBarrier(2);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> historyTrace(ready.execution(), barrier));
            var second = executor.submit(() -> historyTrace(ready.execution(), barrier));
            assertTraceEquals(expected, first.get(20, java.util.concurrent.TimeUnit.SECONDS));
            assertTraceEquals(expected, second.get(20, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS));
        }
        assertTraceEquals(expected, historyTrace(ready.execution(), null));
        assertEquals(0.2, ready.execution().newRunModel().getCircuitComponents().get(2).getNumericParameters()[1]);
        assertEquals(1, ready.execution().newRunModel().getCircuitComponents().get(3).getNumericParameters()[1]);
    }
}
