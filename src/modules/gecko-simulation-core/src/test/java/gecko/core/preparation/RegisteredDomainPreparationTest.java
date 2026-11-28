package gecko.core.preparation;

import gecko.core.io.CircuitModel;
import java.util.Map;
import org.junit.jupiter.api.Test;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class RegisteredDomainPreparationTest {
    static CircuitModel circuit(int sourceType, int resistorType, double[] sourceParameters) {
        var model = new CircuitModel();
        model.setTimeStep(0.25);
        model.setSimulationDuration(0.5);
        model.addCircuitComponent(CircuitPreparationTest.component(sourceType, "source", sourceParameters, "1", "0"));
        model.addCircuitComponent(CircuitPreparationTest.component(resistorType, "R.1", new double[]{10}, "1", "0"));
        return model;
    }

    static void assertCurrents(PreparedCircuit circuit, double... expected) {
        var actual = CircuitPreparationTest.currents(circuit);
        assertEquals(expected.length, actual.size());
        for (int index = 0; index < expected.length; index++) assertEquals(expected[index], actual.get(index), 1e-10);
    }

    @Test
    void programmaticThermalCircuitExecutesItsDcAndResistanceOverrides() {
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(circuit(45, 46, new double[]{401, 6}));
        var original = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        assertCurrents(original.execution(), 0.6, 0.6, 0.6);
        var source = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), Map.of("source.dcValue", 12.0))));
        var resistance = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), Map.of("R.1.resistance", 20.0))));
        assertCurrents(source.execution(), 1.2, 1.2, 1.2);
        assertCurrents(resistance.execution(), 0.3, 0.3, 0.3);
        assertCurrents(original.execution(), 0.6, 0.6, 0.6);
    }

    @ParameterizedTest
    @CsvSource({"26,24,0.6,1.2,0.3", "44,46,-6,-12,-6"})
    void otherRegisteredDcPathsExecuteTheirOwnEffectiveConfiguration(int type, int resistorType,
            double initialCurrent, double changedSourceCurrent, double changedResistanceCurrent) {
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(circuit(type, resistorType, new double[]{401, 6}));
        var original = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        var source = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), Map.of("source.dcValue", 12.0))));
        var resistance = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), Map.of("R.1.resistance", 20.0))));
        assertCurrents(original.execution(), initialCurrent, initialCurrent, initialCurrent);
        assertCurrents(source.execution(), changedSourceCurrent, changedSourceCurrent, changedSourceCurrent);
        assertCurrents(resistance.execution(), changedResistanceCurrent, changedResistanceCurrent, changedResistanceCurrent);
        assertCurrents(original.execution(), initialCurrent, initialCurrent, initialCurrent);
    }

    @ParameterizedTest
    @CsvSource({"45,amplitude,8,0,0.8,0", "26,amplitude,8,0,0.8,0", "44,amplitude,8,0,-8,0",
            "45,frequency,2,0,0,0", "26,frequency,2,0,0,0", "44,frequency,2,0,0,0",
            "45,offset,2,0.2,0.6,0.2", "26,offset,2,0.2,0.6,0.2", "44,offset,2,2,-2,2",
            "45,phaseDegrees,90,-0.4,0,0.4", "26,phaseDegrees,90,-0.4,0,0.4", "44,phaseDegrees,90,4,0,-4",
            "45,sourceType,401,0.6,0.6,0.6", "26,sourceType,401,0.6,0.6,0.6", "44,sourceType,401,-6,-6,-6"})
    void registeredSinusoidalSourcesExecuteEachConfiguredField(int type, String field, double value,
            double first, double second, double third) {
        double[] parameters = new double[21];
        parameters[0] = 402;
        parameters[1] = 6;
        parameters[2] = 1;
        parameters[20] = 4;
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(circuit(type, type == 26 ? 24 : 46, parameters));
        var original = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        var changed = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(SolverOverrides.none(), Map.of("source." + field, value))));
        if (type == 44) assertCurrents(original.execution(), 0, -4, 0);
        else assertCurrents(original.execution(), 0, 0.4, 0);
        assertCurrents(changed.execution(), first, second, third);
        var repeated = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(changed.circuit(), PreparationRequest.defaults()));
        assertCurrents(repeated.execution(), first, second, third);
    }

    static CircuitModel savedCircuit(int type, String parameters, String tokens) throws Exception {
        var model = SourcePreparationTest.parse(SourcePreparationTest.element("LK", type, "source", parameters, "/1", "/0", tokens)
                + SourcePreparationTest.element("LK", type == 26 ? 24 : 46, "R.1", "10", "/1", "/0", ""));
        model.setTimeStep(0.25);
        model.setSimulationDuration(0.5);
        return model;
    }

    static Stream<Arguments> savedLayouts() {
        return Stream.of(26, 44, 45).flatMap(type -> Stream.of(
                Arguments.of(type, "0 6", "", type == 44 ? -6.0 : 0.6, type == 44 ? -6.0 : 0.6, type == 44 ? -6.0 : 0.6),
                Arguments.of(type, "401 6", "", type == 44 ? -6.0 : 0.6, type == 44 ? -6.0 : 0.6, type == 44 ? -6.0 : 0.6),
                Arguments.of(type, "1 6 1 2 1.5707963267948966", "", type == 44 ? 8.0 : -0.4, type == 44 ? 2.0 : 0.2, type == 44 ? -4.0 : 0.8),
                Arguments.of(type, "402 6 1 2 1.5707963267948966", "", type == 44 ? 8.0 : -0.4, type == 44 ? 2.0 : 0.2, type == 44 ? -4.0 : 0.8),
                Arguments.of(type, "1 6 1 2 1.5707963267948966 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 9",
                        "amplitude 0\nphaseDegrees 0", type == 44 ? 2.0 : 0.2, type == 44 ? 2.0 : 0.2, type == 44 ? 2.0 : 0.2),
                Arguments.of(type, "1 6 1 2 1.5707963267948966", "amplitude 8\nfrequency 0\noffset -2\nphaseDegrees 90",
                        type == 44 ? 6.0 : -1.0, type == 44 ? 6.0 : -1.0, type == 44 ? 6.0 : -1.0)));
    }

    @ParameterizedTest
    @MethodSource("savedLayouts")
    void parsedLkAdapterPreservesRegisteredSourceLayoutsAndNamedPrecedence(int type, String parameters,
            String tokens, double first, double second, double third) throws Exception {
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(savedCircuit(type, parameters, tokens)), PreparationRequest.defaults()));
        assertCurrents(ready.execution(), first, second, third);
        var repeated = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(ready.circuit(), PreparationRequest.defaults()));
        assertCurrents(repeated.execution(), first, second, third);
    }

    static Stream<Arguments> controlledModes() {
        return Stream.of(26, 44, 45).flatMap(type -> Stream.of(2, 3, 4, 5, 6, 396, 397, 398, 399, 400)
                .map(mode -> Arguments.of(type, mode)));
    }

    @ParameterizedTest
    @MethodSource("controlledModes")
    void registeredControlledModesRemainInspectableAcrossBothInputPaths(int type, int mode) throws Exception {
        var preparation = new CircuitPreparation();
        for (var model : new CircuitModel[]{circuit(type, type == 26 ? 24 : 46, new double[]{mode, 6}),
                savedCircuit(type, mode + " 6", "")}) {
            var snapshot = preparation.capture(model);
            var result = assertInstanceOf(PreparationResult.Inspectable.class,
                    preparation.prepare(snapshot, PreparationRequest.defaults()));
            assertEquals(ReadinessReport.Outcome.UNSUPPORTED, result.report().outcome());
            assertTrue(result.report().diagnostics().stream().anyMatch(item ->
                    item.code().equals("UNSUPPORTED_SOURCE_MODE") && "source".equals(item.component())
                            && "source.sourceType".equals(item.parameterPath())), result.report().toString());
        }
    }
}
