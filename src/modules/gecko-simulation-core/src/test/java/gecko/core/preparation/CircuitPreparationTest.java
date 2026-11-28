package gecko.core.preparation;

import gecko.core.io.CircuitModel;
import gecko.core.io.ImportIssue;
import gecko.core.simulation.HeadlessSimulationEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CircuitPreparationTest {
    static CircuitModel resistorCircuit() {
        CircuitModel model = new CircuitModel();
        model.setTimeStep(0.1);
        model.setSimulationDuration(0.2);
        model.addCircuitComponent(component(4, "U.1", new double[]{401, 6}, "1", "0"));
        model.addCircuitComponent(component(1, "R.1", new double[]{10}, "1", "0"));
        return model;
    }

    static CircuitModel.ComponentData component(int type, String name, double[] parameters, String x, String y) {
        var component = new CircuitModel.ComponentData(type, name);
        component.setNumericParameters(parameters);
        for (int i = 0; i < parameters.length; i++) component.setParameter("param" + i, parameters[i]);
        component.setTerminalXLabels(new String[]{x});
        component.setTerminalYLabels(new String[]{y});
        return component;
    }

    static List<Double> currents(PreparedCircuit circuit) {
        List<Double> currents = new ArrayList<>();
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, voltages, amps, netlist) ->
                currents.add(amps[netlist.getElementIndexByName("R.1")]));
        var result = engine.runSimulation(circuit, ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return currents;
    }

    @Test
    void prefixRelatedComponentNamesBindNamedAndIndexedOverridesIndependently() {
        var model = resistorCircuit();
        model.addCircuitComponent(component(1, "R", new double[]{10}, "1", "0"));
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);
        List<Map<String, Double>> assignments = List.of(
                Map.of("R.1.resistance", 20.0),
                Map.of("R.param0", 30.0),
                Map.of("R.resistance", 20.0, "R.1.param0", 30.0),
                Map.of());
        double[][] expected = {{0.6, 0.3}, {0.2, 0.6}, {0.3, 0.2}, {0.6, 0.6}};
        for (int run = 0; run < assignments.size(); run++) {
            var ready = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                    new PreparationRequest(SolverOverrides.none(), assignments.get(run))));
            List<double[]> samples = new ArrayList<>();
            var engine = new HeadlessSimulationEngine();
            engine.setStepTraceListener((time, volts, amps, netlist) -> samples.add(new double[]{
                    amps[netlist.getElementIndexByName("R")], amps[netlist.getElementIndexByName("R.1")]}));
            var result = engine.runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
            assertTrue(result.isSuccess(), result.getErrorMessage());
            assertEquals(3, samples.size());
            for (double[] sample : samples) {
                assertEquals(expected[run][0], sample[0], 1e-10);
                assertEquals(expected[run][1], sample[1], 1e-10);
            }
        }
    }

    @Test
    void capturedCloneUsesChangedResistanceAndOwnsItsSource() {
        var model = resistorCircuit();
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);
        var original = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        var clone = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, new PreparationRequest(SolverOverrides.none(), Map.of("R.1.resistance", 20.0))));
        model.getCircuitComponents().get(1).setParameter("param0", 999.0);
        model.getCircuitComponents().get(1).getNumericParameters()[0] = 999;
        model.getCircuitComponents().get(1).getTerminalXLabels()[0] = "broken";
        assertEquals(List.of(0.6, 0.6, 0.6), currents(original.execution()));
        assertEquals(List.of(0.3, 0.3, 0.3), currents(clone.execution()));
        var inspected = snapshot.toModel();
        inspected.getCircuitComponents().get(1).setParameter("param0", 222.0);
        assertEquals(List.of(0.6, 0.6, 0.6), currents(original.execution()));
    }
    @Test
    void conflictingAliasesRejectTheWholeRequestInEitherOrder() {
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(resistorCircuit());
        for (boolean reverse : new boolean[]{false, true}) {
            Map<String, Double> parameters = new java.util.LinkedHashMap<>();
            parameters.put(reverse ? "R.1.param0" : "R.1.resistance", 20.0);
            parameters.put(reverse ? "R.1.resistance" : "R.1.param0", 30.0);
            var result = assertInstanceOf(PreparationResult.Rejected.class,
                    preparation.prepare(snapshot, new PreparationRequest(SolverOverrides.none(), parameters)));
            assertTrue(result.report().diagnostics().stream().anyMatch(item -> item.code().equals("OVERRIDE_CONFLICT")));
            assertEquals(List.of(0.6, 0.6, 0.6), currents(assertInstanceOf(PreparationResult.Ready.class,
                    preparation.prepare(snapshot, PreparationRequest.defaults())).execution()));
        }
        var equal = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), Map.of("R.1.resistance", 20.0, "R.1.param0", 20.0))));
        assertEquals(List.of(0.3, 0.3, 0.3), currents(equal.execution()));
    }
    @Test
    void invalidAssignmentsReportEveryPathAndLeaveTheBaseExecutable() {
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(resistorCircuit());
        Map<String, Double> parameters = new java.util.LinkedHashMap<>();
        parameters.put("R.1.resistance", -1.0);
        parameters.put("R.1.param2", 2.0);
        parameters.put("U.1.uniqueObjectIdentifier", 3.0);
        parameters.put("missing.value", 4.0);
        parameters.put("R.1.unrecognized", null);
        var rejected = assertInstanceOf(PreparationResult.Rejected.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), parameters)));
        assertEquals(5, rejected.report().diagnostics().size());
        assertTrue(rejected.report().diagnostics().stream().anyMatch(item -> item.code().equals("INVALID_PARAMETER")
                && item.parameterPath().equals("R.1.resistance")));
        assertTrue(rejected.report().diagnostics().stream().anyMatch(item -> item.code().equals("READ_ONLY_PARAMETER")
                && item.parameterPath().equals("R.1.param2")));
        assertEquals(List.of(0.6, 0.6, 0.6), currents(assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults())).execution()));
        for (Double value : java.util.Arrays.asList(null, Double.NaN, Double.POSITIVE_INFINITY)) {
            parameters.clear();
            parameters.put("R.1.resistance", value);
            assertInstanceOf(PreparationResult.Rejected.class, preparation.prepare(snapshot,
                    new PreparationRequest(SolverOverrides.none(), parameters)));
        }
        var model = resistorCircuit();
        model.addControlComponent(component(3, "R.1", new double[]{1}, "", "out"));
        var ambiguous = assertInstanceOf(PreparationResult.Rejected.class, preparation.prepare(preparation.capture(model),
                new PreparationRequest(SolverOverrides.none(), Map.of("R.1.resistance", 20.0))));
        assertTrue(ambiguous.report().diagnostics().stream().anyMatch(item -> item.code().equals("AMBIGUOUS_PARAMETER")));
    }
    @Test
    void explicitSettingsInheritIndependentlyAndInvalidValuesNeverFallBack() {
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(resistorCircuit());
        var ready = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(new SolverOverrides(gecko.core.allg.SolverType.SOLVER_GS, null, 0.4), Map.of())));
        assertEquals(5, currents(ready.execution()).size());
        assertEquals(gecko.core.allg.SolverType.SOLVER_GS, ready.execution().solverSettings().getSolverType());
        assertEquals(0.1, ready.execution().solverSettings().getStepWidth());
        for (double invalid : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            var rejected = assertInstanceOf(PreparationResult.Rejected.class, preparation.prepare(snapshot,
                    new PreparationRequest(new SolverOverrides(null, invalid, invalid), Map.of())));
            assertEquals(2, rejected.report().diagnostics().stream()
                    .filter(item -> item.code().equals("INVALID_SETTING")).count());
        }
        assertEquals(3, currents(assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults())).execution()).size());
    }
    @Test
    void readinessRetainsUnsupportedAndTopologyFindingsWithoutRunning() {
        var preparation = new CircuitPreparation();
        var model = resistorCircuit();
        model.getCircuitComponents().clear();
        model.addCircuitComponent(component(1, "floating", new double[]{10}, "x", "y"));
        model.addCircuitComponent(component(777, "unknown", new double[]{1}, "0", "0"));
        var snapshot = preparation.capture(model);
        var result = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        assertEquals(ReadinessReport.Outcome.INVALID, result.report().outcome());
        assertFalse(result.report().readyForHeadless());
        assertTrue(result.report().diagnostics().stream().anyMatch(item -> item.code().equals("UNCONNECTED_GROUND")
                && item.component().equals("floating") && item.domain().equals("LK")));
        assertTrue(result.report().diagnostics().stream().anyMatch(item -> item.category() == Diagnostic.Category.CAPABILITY
                && item.component().equals("unknown")));
        assertEquals(result.report(), preparation.prepare(snapshot, PreparationRequest.defaults()).report());
        var unsupportedOnly = resistorCircuit();
        unsupportedOnly.addCircuitComponent(component(777, "unknown", new double[]{1}, "0", "0"));
        assertEquals(ReadinessReport.Outcome.UNSUPPORTED,
                preparation.prepare(preparation.capture(unsupportedOnly), PreparationRequest.defaults()).report().outcome());
    }
    @Test
    void malformedWireRemainsInspectableAndInvalidOverridesStillReject() {
        var model = new CircuitModel();
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{4, 5}}));
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);
        model.getConnections().getFirst().getPoints()[0][0] = 99;
        var inspectable = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        assertEquals(ReadinessReport.Outcome.INVALID, inspectable.report().outcome());
        assertTrue(inspectable.report().diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("INVALID_TOPOLOGY") && diagnostic.message().contains("wire")));
        assertArrayEquals(new int[]{4, 5}, inspectable.circuit().toModel().getConnections().getFirst().getPoints()[0]);
        assertArrayEquals(new int[]{4, 5}, snapshot.toModel().getConnections().getFirst().getPoints()[0]);
        var rejected = assertInstanceOf(PreparationResult.Rejected.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), Map.of("missing.value", 1.0))));
        assertTrue(rejected.report().diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("UNKNOWN_PARAMETER")));
        assertTrue(rejected.report().diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("INVALID_TOPOLOGY")));
        assertEquals(inspectable.report(), preparation.prepare(snapshot, PreparationRequest.defaults()).report());
    }

    @Test
    void malformedTopologyRetainsIndependentEffectiveSettingsAndImportFindings() {
        var model = resistorCircuit();
        model.setTimeStep(-1);
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{4, 5}}));
        model.getImportIssues().add(new ImportIssue("MALFORMED_SIMULATION_BLOCK", "CONTROL", "broken",
                "The closing tag is missing.", "fixture.ipes: line 14", false));
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);
        var invalid = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        assertEquals(List.of("MALFORMED_SIMULATION_BLOCK", "INVALID_TOPOLOGY", "INVALID_SETTING"),
                invalid.report().diagnostics().stream().map(Diagnostic::code).toList());
        assertEquals(Diagnostic.Category.PARSING, invalid.report().diagnostics().getFirst().category());
        assertEquals("fixture.ipes: line 14", invalid.report().diagnostics().getFirst().sourceLocation());
        assertEquals("timeStep", invalid.report().diagnostics().getLast().parameterPath());
        var repairedStep = assertInstanceOf(PreparationResult.Inspectable.class, preparation.prepare(snapshot,
                new PreparationRequest(new SolverOverrides(null, 0.1, null), Map.of())));
        assertEquals(List.of("MALFORMED_SIMULATION_BLOCK", "INVALID_TOPOLOGY"),
                repairedStep.report().diagnostics().stream().map(Diagnostic::code).toList());
        assertEquals(0.1, repairedStep.circuit().toModel().getTimeStep());
        var rejected = assertInstanceOf(PreparationResult.Rejected.class, preparation.prepare(snapshot,
                new PreparationRequest(new SolverOverrides(null, 0.1, 0.0), Map.of())));
        assertEquals(List.of("INVALID_SETTING", "MALFORMED_SIMULATION_BLOCK", "INVALID_TOPOLOGY"),
                rejected.report().diagnostics().stream().map(Diagnostic::code).toList());
        assertEquals("duration", rejected.report().diagnostics().getFirst().parameterPath());
        assertEquals(-1, snapshot.toModel().getTimeStep());
        assertEquals(1, snapshot.toModel().getImportIssues().size());
        assertArrayEquals(new int[]{4, 5}, snapshot.toModel().getConnections().getFirst().getPoints()[0]);
        assertEquals(invalid.report(), preparation.prepare(snapshot, PreparationRequest.defaults()).report());
    }

}
