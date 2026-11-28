package gecko.core.simulation;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ControlFactorySavedConfigurationTest {
    @Test
    void rawNamedSavedValuesReachThePublicFactoryWithoutMutatingTheCaller() throws Exception {
        var model = saved();
        var constant = model.getControlComponents().get(0);
        var gain = model.getControlComponents().get(1);
        var tokens = new LinkedHashMap<>(constant.getImportTokens());
        assertEquals(0, constant.getNumericParameters()[0]);
        assertEquals("2", constant.getImportTokens().get("constantValue"));
        assertFalse(constant.isConfigurationNormalized());
        var build = new ControlCalculatorFactory().buildCircuit(model, 0.1);
        assertArrayEquals(new float[]{6}, sample(build));
        assertEquals(0, constant.getNumericParameters()[0]);
        assertEquals(0, gain.getNumericParameters()[0]);
        assertFalse(constant.isConfigurationNormalized());
        assertFalse(gain.isConfigurationNormalized());
        assertEquals(tokens, constant.getImportTokens());
    }

    @Test
    void preparedOverridesRemainEffectiveAndDetachedFromRawSavedValues() throws Exception {
        var model = saved();
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), new PreparationRequest(
                SolverOverrides.none(), Map.of("constant.value", 5.0, "gain.gain", 4.0)));
        var ready = assertInstanceOf(PreparationResult.Ready.class, outcome, outcome.report().toString());
        var runModel = ready.execution().newRunModel();
        assertTrue(runModel.getControlComponents().get(0).isConfigurationNormalized());
        assertEquals(5, runModel.getControlComponents().get(0).getNumericParameters()[0]);
        assertEquals(4, runModel.getControlComponents().get(1).getNumericParameters()[0]);
        var build = new ControlCalculatorFactory().buildCircuit(runModel, 0.1);
        assertArrayEquals(new float[]{20}, sample(build));
        assertEquals(5, runModel.getControlComponents().get(0).getNumericParameters()[0]);
        assertEquals(4, runModel.getControlComponents().get(1).getNumericParameters()[0]);
        assertEquals(5, ready.execution().newRunModel().getControlComponents().get(0).getNumericParameters()[0]);
        assertEquals(0, model.getControlComponents().get(0).getNumericParameters()[0]);
        assertEquals(0, model.getControlComponents().get(1).getNumericParameters()[0]);
        assertFalse(model.getControlComponents().get(0).isConfigurationNormalized());
        assertEquals("2", model.getControlComponents().get(0).getImportTokens().get("constantValue"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "402,3,4.4142137,5",
        "403,4,5,4",
        "404,5,5,5"
    })
    void programmaticGuiSignalAliasesExecuteThroughFactoryAndPreparation(
            int subtype, float first, float second, float third) {
        var model = new CircuitModel();
        model.setTimeStep(0.125);
        model.setSimulationDuration(0.25);
        model.addControlComponent(ControlReviewRegressionTest.component(4, "source",
                new double[]{subtype,2,1,0,3,0.5}, new String[0], "wave"));
        model.addControlComponent(ControlReviewRegressionTest.component(5, "scope",
                new double[0], new String[]{"wave"}));
        var build = assertDoesNotThrow(() -> new ControlCalculatorFactory().buildCircuit(model, 0.125));
        var scope = ScopeDataExtractor.fromScopes(build.getMapping().getScopeInfos());
        build.getControlNetlist().initializeAtSimulationStart(0.125);
        float[] actual = new float[3];
        for (int step = 0; step < actual.length; step++) {
            build.getControlNetlist().executeTimeStep(0.125, step * 0.125);
            actual[step] = scope.extractCurrentValues()[0];
        }
        float[] expected = {first,second,third};
        assertArrayEquals(expected, actual, 1e-6f);
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, outcome, outcome.report().toString());
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(expected, result.getSignalData(0), 1e-6f);
        assertEquals(subtype, model.getControlComponents().get(0).getNumericParameters()[0]);
    }

    private static float[] sample(ControlCalculatorFactory.BuildResult build) {
        build.getControlNetlist().initializeAtSimulationStart(0.1);
        build.getControlNetlist().executeTimeStep(0.1, 0);
        return ScopeDataExtractor.fromScopes(build.getMapping().getScopeInfos()).extractCurrentValues();
    }

    private static CircuitModel saved() throws Exception {
        String text = "dt 0.1\ntDURATION 0.2\n"
                + block(3, "constant", "", "drive", "constantValue 2")
                + block(7, "gain", "drive", "output", "gain 3")
                + block(5, "scope", "output", "", "");
        return new CircuitFileParser().parse(new BufferedReader(new StringReader(text)), "named-control.ipes");
    }

    private static String block(int type, String name, String input, String output, String named) {
        return "c\n<ElementCONTROL>\ntyp " + type + "\nidStringDialog " + name
                + "\nx 0\ny 0\norientierung 503\nparameter[] 0\nparameterString[] /\n"
                + "labelAnfangsKnoten[] /" + input + "\nlabelEndKnoten[] /" + output + "\n"
                + named + "\n<" + (char) 92 + "ElementCONTROL>\n";
    }
}
