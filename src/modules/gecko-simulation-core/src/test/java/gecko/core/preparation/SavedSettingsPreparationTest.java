package gecko.core.preparation;

import gecko.core.allg.SolverType;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import static gecko.core.preparation.SourcePreparationTest.element;
import static org.junit.jupiter.api.Assertions.*;

class SavedSettingsPreparationTest {
    private static CircuitModel parse(String settings) throws Exception {
        String circuit = element("LK", 4, "source", "401 6", "/1", "/0", "")
                + element("LK", 1, "R", "10", "/1", "/0", "");
        return new CircuitFileParser().parse(new BufferedReader(new StringReader(settings + circuit)), "saved-settings.ipes");
    }

    static Stream<Arguments> malformedSettings() {
        return Stream.concat(
                Stream.of("dt", "tDURATION").flatMap(token -> Stream.of("", "broken", "NaN", "Infinity", "-Infinity")
                        .map(value -> Arguments.of(token, value, token.equals("dt") ? "timeStep" : "duration"))),
                Stream.of("", "broken", "NaN", "Infinity", "1.5", "-1", "3", "999999999999999999999")
                        .map(value -> Arguments.of("solverType", value, "solver")));
    }

    private static Set<String> invalidPaths(PreparationResult result) {
        return result.report().diagnostics().stream().filter(d -> d.code().equals("INVALID_SETTING"))
                .map(Diagnostic::parameterPath).collect(Collectors.toSet());
    }

    @ParameterizedTest
    @MethodSource("malformedSettings")
    void malformedSavedSettingsRemainInspectableAndAValidExplicitValueRepairsOnlyTheSelectedField(
            String token, String value, String path) throws Exception {
        String text = token + " " + value + "\n"
                + (token.equals("dt") ? "" : "dt 0.125\n")
                + (token.equals("tDURATION") ? "" : "tDURATION 0.25\n");
        var model = assertDoesNotThrow(() -> parse(text));
        assertTrue(Double.isFinite(model.getTimeStep()));
        assertTrue(Double.isFinite(model.getSimulationDuration()));
        assertNotNull(model.getSolverType());
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);
        var invalid = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        assertEquals(ReadinessReport.Outcome.INVALID, invalid.report().outcome());
        assertEquals(Set.of(path), invalidPaths(invalid));
        assertTrue(invalid.report().diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_SETTING")
                && d.category() == Diagnostic.Category.CONFIGURATION && path.equals(d.parameterPath())
                && ("saved-settings.ipes: line 1 " + token).equals(d.sourceLocation())), invalid.report().toString());
        var settings = new SolverOverrides(path.equals("solver") ? SolverType.SOLVER_TRZ : null,
                path.equals("timeStep") ? 0.125 : null, path.equals("duration") ? 0.25 : null);
        var repaired = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, new PreparationRequest(settings, Map.of())));
        assertTrue(invalidPaths(repaired).isEmpty());
        assertEquals(0.125, repaired.execution().solverSettings().getStepWidth());
        assertEquals(0.25, repaired.execution().solverSettings().getSimulationDuration());
        assertEquals(path.equals("solver") ? SolverType.SOLVER_TRZ : SolverType.SOLVER_BE,
                repaired.execution().solverSettings().getSolverType());
        assertEquals(invalid.report(), preparation.prepare(snapshot, PreparationRequest.defaults()).report());
        assertEquals(invalid.report(), preparation.prepare(preparation.capture(model), PreparationRequest.defaults()).report());
        assertEquals(repaired.report(), preparation.prepare(repaired.circuit(), PreparationRequest.defaults()).report());
    }

    @Test
    void independentSavedErrorsRemainAfterAPartialRepairAndInvalidExplicitValuesAreRejected() throws Exception {
        var model = parse("dt broken\ntDURATION broken\nsolverType 9\n");
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);
        var partial = assertInstanceOf(PreparationResult.Inspectable.class, preparation.prepare(snapshot,
                new PreparationRequest(new SolverOverrides(null, 0.125, null), Map.of())));
        assertEquals(Set.of("duration", "solver"), invalidPaths(partial));
        for (double value : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            var rejected = assertInstanceOf(PreparationResult.Rejected.class, preparation.prepare(snapshot,
                    new PreparationRequest(new SolverOverrides(SolverType.SOLVER_GS, value, 0.25), Map.of())));
            assertEquals(Set.of("timeStep"), invalidPaths(rejected));
        }
        var repaired = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(new SolverOverrides(SolverType.SOLVER_GS, 0.125, 0.25), Map.of())));
        assertTrue(invalidPaths(repaired).isEmpty());
        assertEquals(Set.of("timeStep", "duration", "solver"), invalidPaths(preparation.prepare(snapshot, PreparationRequest.defaults())));
    }

    @ParameterizedTest
    @CsvSource({"0,SOLVER_BE", "1,SOLVER_TRZ", "2,SOLVER_GS"})
    void validSavedLegacySolverIndicesAndTimingRemainReady(int index, SolverType expected) throws Exception {
        var model = parse("dt 0.125\ntDURATION 0.25\nsolverType " + index + "\n");
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        assertEquals(expected, ready.execution().solverSettings().getSolverType());
        assertEquals(0.125, ready.execution().solverSettings().getStepWidth());
        assertEquals(0.25, ready.execution().solverSettings().getSimulationDuration());
    }

    @Test
    void omittedFieldsKeepTheirDefaultsAndCanReceiveExplicitSettingsWithoutParseErrors() throws Exception {
        var model = parse("");
        assertEquals(0, model.getTimeStep());
        assertEquals(0, model.getSimulationDuration());
        assertEquals(SolverType.SOLVER_BE, model.getSolverType());
        assertTrue(model.getImportIssues().isEmpty());
        var preparation = new CircuitPreparation();
        assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(preparation.capture(model),
                new PreparationRequest(new SolverOverrides(null, 0.125, 0.25), Map.of())));
        var omittedSolver = parse("dt 0.125\ntDURATION 0.25\n");
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(omittedSolver), PreparationRequest.defaults()));
        assertEquals(SolverType.SOLVER_BE, ready.execution().solverSettings().getSolverType());
    }

    @ParameterizedTest
    @CsvSource({"dt,0,timeStep", "tDURATION,-1,duration"})
    void finiteInvalidSavedTimingCanBeRepairedWithoutChangingTheCapturedInput(String token, double value, String path) throws Exception {
        var model = parse(token + " " + value + "\n" + (token.equals("dt") ? "tDURATION 0.25\n" : "dt 0.125\n"));
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model);
        var invalid = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        assertEquals(Set.of(path), invalidPaths(invalid));
        assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot,
                new PreparationRequest(new SolverOverrides(null, 0.125, 0.25), Map.of())));
        assertEquals(invalid.report(), preparation.prepare(snapshot, PreparationRequest.defaults()).report());
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"scripterCode", "sourceCode", "GeckoFileManager"})
    void malformedSettingTextInOpaqueSavedContentDoesNotOverrideGlobalSettings(String tag) throws Exception {
        var model = parse("<" + tag + ">\ndt broken\ntDURATION NaN\nsolverType 999\n<" + (char) 92 + tag + ">\n"
                + "dt 0.125\ntDURATION 0.25\nsolverType 1\n");
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        assertEquals(0.125, ready.execution().solverSettings().getStepWidth());
        assertEquals(0.25, ready.execution().solverSettings().getSimulationDuration());
        assertEquals(SolverType.SOLVER_TRZ, ready.execution().solverSettings().getSolverType());
        assertTrue(model.getImportIssues().isEmpty());
    }
}