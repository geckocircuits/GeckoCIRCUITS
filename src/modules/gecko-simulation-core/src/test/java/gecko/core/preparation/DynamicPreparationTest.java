package gecko.core.preparation;

import gecko.core.allg.SolverType;
import gecko.core.io.CircuitModel;
import gecko.core.simulation.HeadlessSimulationEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class DynamicPreparationTest {
    static Stream<Arguments> dynamicInputs() {
        return Stream.of(2, 3, 12, 47).flatMap(type -> Stream.of(SolverType.values()).flatMap(solver ->
                Stream.of(false, true).flatMap(parsed -> Stream.of(false, true).map(initial -> Arguments.of(type, solver, parsed, initial)))));
    }
    @ParameterizedTest
    @MethodSource("dynamicInputs")
    void configuredDynamicsAndInitialConditionsReachTheMeasuredCircuit(int type, SolverType solver,
            boolean parsed, boolean initial) throws Exception {
        boolean capacitor = type == 3 || type == 47;
        double[] parameters = capacitor ? new double[]{0.01, 0, 9, 9, 9, 9, 0.03, 0.04, 9, 9, 9}
                : new double[]{1, 0, 9, 9, 9};
        CircuitModel model;
        String resistorEnd = initial ? "0" : "2";
        String stateStart = initial ? "1" : "2";
        if (parsed) {
            String text = initial ? "" : SourcePreparationTest.element("LK", 4, "U.1", "401 6", "/1", "/0", "");
            text += SourcePreparationTest.element("LK", 1, "R.1", "10", "/1", "/" + resistorEnd, "");
            String values = java.util.Arrays.stream(parameters).mapToObj(Double::toString).collect(java.util.stream.Collectors.joining(" "));
            text += SourcePreparationTest.element("LK", type, "state", values, "/" + stateStart, "/0", "");
            model = SourcePreparationTest.parse(text);
        } else {
            model = new CircuitModel();
            if (!initial) model.addCircuitComponent(CircuitPreparationTest.component(4, "U.1", new double[]{401, 6}, "1", "0"));
            model.addCircuitComponent(CircuitPreparationTest.component(1, "R.1", new double[]{10}, "1", resistorEnd));
            model.addCircuitComponent(CircuitPreparationTest.component(type, "state", parameters, stateStart, "0"));
        }
        model.setTimeStep(0.1);
        model.setSimulationDuration(0.2);
        String field = initial ? (capacitor ? "initVoltage" : "initialCurrent") : (capacitor ? "capacitance" : "inductance");
        double value = initial ? (capacitor ? 6 : 1) : (capacitor ? 0.02 : 2);
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(preparation.capture(model),
                new PreparationRequest(new SolverOverrides(solver, null, null), Map.of("state." + field, value))));
        List<Double> currents = new ArrayList<>();
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, volts, amps, netlist) -> {
            double resistorCurrent = amps[netlist.getElementIndexByName("R.1")];
            double stateCurrent = amps[netlist.getElementIndexByName("state")];
            currents.add(resistorCurrent);
            assertEquals(initial ? -resistorCurrent : resistorCurrent, stateCurrent, 1e-10);
        });
        var result = engine.runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        double expected;
        if (initial && capacitor) expected = switch (solver) { case SOLVER_BE -> 0.3; case SOLVER_TRZ -> 0.2; case SOLVER_GS -> 0.36; };
        else if (initial) expected = switch (solver) { case SOLVER_BE -> -0.5; case SOLVER_TRZ -> -1.0 / 3.0; case SOLVER_GS -> -0.6; };
        else if (capacitor) expected = switch (solver) { case SOLVER_BE -> 0.4; case SOLVER_TRZ -> 0.48; case SOLVER_GS -> 0.45; };
        else expected = switch (solver) { case SOLVER_BE -> 0.2; case SOLVER_TRZ -> 0.12; case SOLVER_GS -> 0.15; };
        assertEquals(expected, currents.get(0), 1e-10);
        var state = ready.circuit().toModel().getCircuitComponents().stream().filter(item -> item.getName().equals("state")).findFirst().orElseThrow();
        assertEquals(initial ? value : 0.0, ((Number) state.getParameters().get("param1")).doubleValue());
    }
}
