package gecko.core.preparation;

import java.util.Map;
import gecko.core.simulation.HeadlessSimulationEngine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ControlPreparationTest {
    @Test
    void constantAndGainOverridesReachScopeAndKeepVisibleConfigurationAligned() throws Exception {
        var model = SourcePreparationTest.parse(
                SourcePreparationTest.element("LK", 1, "R", "1", "/1", "/0", "")
                + SourcePreparationTest.element("CONTROL", 3, "constant", "2", "", "/raw", "")
                + SourcePreparationTest.element("CONTROL", 7, "gain", "3", "/raw", "/scaled", "")
                + SourcePreparationTest.element("CONTROL", 5, "scope", "", "/scaled", "", ""));
        var preparation = new CircuitPreparation();
        var result = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(preparation.capture(model),
                new PreparationRequest(SolverOverrides.none(), Map.of("constant.value", 4.0, "gain.gain", -2.0))));
        var run = new HeadlessSimulationEngine().runSimulation(result.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(run.isSuccess(), run.getErrorMessage());
        assertEquals(3, run.getSignalData(0).length);
        for (float sample : run.getSignalData(0)) assertEquals(-8, sample, 1e-6);
        var effective = result.circuit().toModel().getControlComponents();
        assertEquals(4, effective.get(0).getNumericParameters()[0]);
        assertEquals(4.0, effective.get(0).getParameters().get("value"));
        assertEquals(-2.0, effective.get(1).getParameters().get("param0"));
    }
    @Test
    void savedSineNormalizationUsesPhysicalPhaseAndIsIdempotent() throws Exception {
        for (boolean degrees : new boolean[]{false, true}) {
            var model = SourcePreparationTest.parse(
                    SourcePreparationTest.element("LK", 1, "R", "1", "/1", "/0", "")
                    + SourcePreparationTest.element("CONTROL", 4, "signal", "402 2 0 3 "
                            + (degrees ? "90" : "1.5707963267948966") + " 0.5", "", "/wave", "")
                    + SourcePreparationTest.element("CONTROL", 5, "scope", "", "/wave", "", ""));
            model.setFileVersion(degrees ? 170 : 160);
            var preparation = new CircuitPreparation();
            var result = assertInstanceOf(PreparationResult.Ready.class,
                    preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
            var effective = result.circuit().toModel().getControlComponents().get(0);
            assertEquals(400, effective.getNumericParameters()[0]);
            assertEquals(Math.PI / 2, effective.getNumericParameters()[3], 1e-12);
            assertEquals(3, effective.getNumericParameters()[4]);
            assertEquals(90.0, effective.getParameters().get("phase"));
            var repeated = assertInstanceOf(PreparationResult.Ready.class,
                    preparation.prepare(result.circuit(), PreparationRequest.defaults()));
            for (var recipe : new PreparedCircuit[]{result.execution(), repeated.execution()}) {
                var run = new HeadlessSimulationEngine().runSimulation(recipe, ExecutionOptions.defaults(), () -> false);
                assertTrue(run.isSuccess(), run.getErrorMessage());
                for (float sample : run.getSignalData(0)) assertEquals(1, sample, 1e-6);
            }
        }
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("signalFields")
    void eachSignalFieldReachesTheScope(String field, double value, double[] expected) {
        var model = CircuitPreparationTest.resistorCircuit();
        model.setTimeStep(0.125);
        model.setSimulationDuration(0.25);
        model.addControlComponent(CircuitPreparationTest.component(4, "signal", new double[]{400, 2, 1, 0, 0, 0.5}, "", "wave"));
        model.addControlComponent(CircuitPreparationTest.component(5, "scope", new double[0], "wave", ""));
        var preparation = new CircuitPreparation();
        var result = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(preparation.capture(model),
                new PreparationRequest(SolverOverrides.none(), Map.of("signal." + field, value))));
        var run = new HeadlessSimulationEngine().runSimulation(result.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(run.isSuccess(), run.getErrorMessage());
        for (int index = 0; index < expected.length; index++) assertEquals(expected[index], run.getSignalData(0)[index], 1e-6);
    }
    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> signalFields() {
        return java.util.stream.Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("amplitude", 4.0, new double[]{0, 2.82842712474619, 4}),
                org.junit.jupiter.params.provider.Arguments.of("frequency", 2.0, new double[]{0, 2, 0}),
                org.junit.jupiter.params.provider.Arguments.of("phase", 90.0, new double[]{-2, -1.414213562373095, 0}),
                org.junit.jupiter.params.provider.Arguments.of("offset", 3.0, new double[]{3, 4.414213562373095, 5}),
                org.junit.jupiter.params.provider.Arguments.of("subtype", 401.0, new double[]{1, 2, 1}),
                org.junit.jupiter.params.provider.Arguments.of("subtype", 404.0, new double[]{2, 2, 2}));
    }

    @Test
    void phaseAliasesCoalesceInPhysicalUnitsAndDutyZeroIsAValidRectangleValue() {
        var model = CircuitPreparationTest.resistorCircuit();
        model.addControlComponent(CircuitPreparationTest.component(4, "signal", new double[]{404, 2, 1, 0, 0, 0.5}, "", "wave"));
        model.addControlComponent(CircuitPreparationTest.component(5, "scope", new double[0], "wave", ""));
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);
        for (boolean reverse : new boolean[]{false, true}) {
            var assignments = new java.util.LinkedHashMap<String, Double>();
            assignments.put(reverse ? "signal.param3" : "signal.phase", reverse ? Math.PI / 2 : 90);
            assignments.put(reverse ? "signal.phase" : "signal.param3", reverse ? 90 : Math.PI / 2);
            assignments.put("signal.duty", 0.0);
            var result = assertInstanceOf(PreparationResult.Ready.class,
                    preparation.prepare(snapshot, new PreparationRequest(SolverOverrides.none(), assignments)));
            var run = new HeadlessSimulationEngine().runSimulation(result.execution(), ExecutionOptions.defaults(), () -> false);
            assertTrue(run.isSuccess(), run.getErrorMessage());
            for (float sample : run.getSignalData(0)) assertEquals(0, sample, 1e-6);
            assignments.put("signal.phase", 180.0);
            assertInstanceOf(PreparationResult.Rejected.class,
                    preparation.prepare(snapshot, new PreparationRequest(SolverOverrides.none(), assignments)));
        }
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"0,400", "402,400", "1,401", "403,401", "2,404", "404,404"})
    void savedSignalModesNormalizeAndExecuteTheirWaveforms(int savedMode, int effectiveMode) throws Exception {
        var model = SourcePreparationTest.parse(
                SourcePreparationTest.element("LK", 1, "R", "1", "/1", "/0", "")
                + SourcePreparationTest.element("CONTROL", 4, "signal", savedMode + " 2 1 3 0 0.5", "", "/wave", "")
                + SourcePreparationTest.element("CONTROL", 5, "scope", "", "/wave", "", ""));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        assertEquals(effectiveMode, ready.circuit().toModel().getControlComponents().get(0).getNumericParameters()[0]);
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        double[] expected = switch (effectiveMode) {
            case 400 -> new double[]{3, 4.414213562373095, 5};
            case 401 -> new double[]{4, 5, 4};
            default -> new double[]{5, 5, 5};
        };
        for (int index = 0; index < expected.length; index++) assertEquals(expected[index], result.getSignalData(0)[index], 1e-6);
    }

    @Test
    void rectangleDutyUpperLimitReachesTheScope() {
        var model = CircuitPreparationTest.resistorCircuit();
        model.addControlComponent(CircuitPreparationTest.component(4, "signal", new double[]{404, 2, 1, 0, 0, 0.5}, "", "wave"));
        model.addControlComponent(CircuitPreparationTest.component(5, "scope", new double[0], "wave", ""));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(preparation.capture(model),
                new PreparationRequest(SolverOverrides.none(), Map.of("signal.duty", 1.0))));
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(new float[]{0, 2, 2}, result.getSignalData(0), 1e-6f);
    }
}
