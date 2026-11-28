package gecko.core.preparation;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.simulation.HeadlessSimulationEngine;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class SourcePreparationTest {
    static String element(String domain, int type, String name, String parameters, String inputs, String outputs, String extra) {
        return (domain.equals("LK") ? "e" : "c") + "\n<Element" + domain + ">\ntyp " + type
                + "\nidStringDialog " + name + "\nx 0\ny 0\norientierung 503\nparameterString[] /\nparameter[] " + parameters
                + "\nlabelAnfangsKnoten[] " + inputs + "\nlabelEndKnoten[] " + outputs + "\n" + extra
                + "\n<" + (char) 92 + "Element" + domain + ">\n";
    }
    static CircuitModel parse(String components) throws Exception {
        return new CircuitFileParser().parse(new BufferedReader(new StringReader("dt 0.125\ntDURATION 0.25\n" + components)), "uploaded.ipes");
    }
    static List<Double> sourceValues(PreparedCircuit circuit, int type) {
        List<Double> values = new ArrayList<>();
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, volts, amps, netlist) -> {
            int index = netlist.getElementIndexByName("source");
            values.add(type == 4 ? volts[netlist.getNodeX(index)] : amps[index]);
        });
        var result = engine.runSimulation(circuit, ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return values;
    }
    static Stream<Arguments> savedSources() {
        return Stream.of(4, 5).flatMap(type -> Stream.of(
                Arguments.of(type, "401 6", "", Map.of(), List.of(6.0, 6.0, 6.0)),
                Arguments.of(type, "0 6", "", Map.of(), List.of(6.0, 6.0, 6.0)),
                Arguments.of(type, "402 6 1 2 1.5707963267948966", "", Map.of(), List.of(-4.0, -2.242640687119285, 2.0)),
                Arguments.of(type, "1 6 1 2 1.5707963267948966", "", Map.of(), List.of(-4.0, -2.242640687119285, 2.0)),
                Arguments.of(type, "1 6 1 2 1.5707963267948966 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 9",
                        "amplitude 0\nphaseDegrees 0", Map.of(), List.of(2.0, 2.0, 2.0)),
                Arguments.of(type, "1 6 1 2 1.5707963267948966", "amplitude 8\nfrequency 0\noffset -2\nphaseDegrees 90",
                        Map.of(), List.of(-10.0, -10.0, -10.0)),
                Arguments.of(type, "401 6", "", Map.of("source.sourceType", 1.0, "source.dcValue", 9.0,
                        "source.amplitude", 4.0, "source.frequency", 0.0, "source.offset", 2.0, "source.phaseDegrees", 90.0), List.of(-2.0, -2.0, -2.0))));
    }
    @ParameterizedTest
    @MethodSource("savedSources")
    void savedSourceLayoutsAndOverridesProduceTheConfiguredWaveform(int type, String parameters, String tokens,
            Map<String, Double> overrides, List<Double> expected) throws Exception {
        var model = parse(element("LK", type, "source", parameters, "/1", "/0", tokens)
                + element("LK", 1, "R.1", "10", "/1", "/0", ""));
        var preparation = new CircuitPreparation();
        var result = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(preparation.capture(model),
                new PreparationRequest(SolverOverrides.none(), overrides)));
        var actual = sourceValues(result.execution(), type);
        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++) assertEquals(expected.get(index), actual.get(index), 1e-10);
        var repeated = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(result.circuit(), PreparationRequest.defaults()));
        assertEquals(actual, sourceValues(repeated.execution(), type));
    }

    @org.junit.jupiter.api.Test
    void savedOptimizerBindingsResolveAfterNamedValuesAndOverridesPersist() throws Exception {
        for (String binding : new String[]{"unchanged", "replacement", "cleared"}) {
            String tokens = "nameOpt[] /resistorValue\nresistance 30";
            if (!binding.equals("unchanged")) tokens += "\nresistance$ParameterName " + (binding.equals("replacement") ? "replacementValue" : "");
            var model = parse(element("LK", 4, "source", "401 6", "/1", "/0", "")
                    + element("LK", 1, "R.1", "10", "/1", "/0", tokens));
            model.getOptimizerParameters().put("resistorValue", 20.0);
            model.getOptimizerParameters().put("replacementValue", 40.0);
            var preparation = new CircuitPreparation();
            var base = preparation.capture(model);
            var result = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(base, PreparationRequest.defaults()));
            double expected = binding.equals("unchanged") ? 0.3 : binding.equals("replacement") ? 0.15 : 0.2;
            for (double current : CircuitPreparationTest.currents(result.execution())) assertEquals(expected, current, 1e-10);
            var changed = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(result.circuit(),
                    new PreparationRequest(SolverOverrides.none(), Map.of("R.1.resistance", 60.0))));
            var repeated = assertInstanceOf(PreparationResult.Ready.class,
                    preparation.prepare(changed.circuit(), PreparationRequest.defaults()));
            for (double current : CircuitPreparationTest.currents(repeated.execution())) assertEquals(0.1, current, 1e-10);
        }
    }
    @org.junit.jupiter.api.Test
    void savedNullOptimizerArrayMeansNoBinding() throws Exception {
        var model = parse(element("LK", 4, "source", "401 6", "/1", "/0", "nameOpt[] null")
                + element("LK", 1, "R.1", "10", "/1", "/0", "nameOpt[] null"));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        for (double current : CircuitPreparationTest.currents(ready.execution())) assertEquals(0.6, current, 1e-10);
    }
    @org.junit.jupiter.api.Test
    void delimitedOptimizerNameNullRemainsAnOpaqueBindingName() throws Exception {
        var model = parse(element("LK", 4, "source", "401 6", "/1", "/0", "")
                + element("LK", 1, "R.1", "10", "/1", "/0", "nameOpt[] /null"));
        model.getOptimizerParameters().put("null", 20.0);
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        for (double current : CircuitPreparationTest.currents(ready.execution())) assertEquals(0.3, current, 1e-10);
    }
}
