package gecko.core.preparation;

import gecko.core.io.CircuitModel;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class NormalizationEvidencePreparationTest {
    private static CircuitModel saved(String sourceTokens, String resistorTokens) throws Exception {
        return SourcePreparationTest.parse(SourcePreparationTest.element("LK", 4, "U.1", "401 6", "/1", "/0", sourceTokens)
                + SourcePreparationTest.element("LK", 1, "R.1", "10", "/1", "/0", resistorTokens));
    }

    private static CircuitModel invalid(String kind) throws Exception {
        return switch (kind) {
            case "malformedNamed" -> saved("", "resistance broken");
            case "missingBinding" -> saved("", "nameOpt[] /missing");
            case "namedConflict" -> {
                var model = CircuitPreparationTest.resistorCircuit();
                model.getCircuitComponents().get(1).setParameter("resistance", 20.0);
                yield model;
            }
            case "indexedConflict" -> {
                var model = CircuitPreparationTest.resistorCircuit();
                model.getCircuitComponents().get(1).setParameter("param0", 20.0);
                yield model;
            }
            default -> throw new IllegalArgumentException(kind);
        };
    }

    private static PreparationResult.Inspectable inspect(CircuitPreparation preparation, CircuitSnapshot source) {
        var result = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(source, PreparationRequest.defaults()));
        assertEquals(ReadinessReport.Outcome.INVALID, result.report().outcome());
        assertTrue(result.report().diagnostics().stream().allMatch(d -> d.category() == Diagnostic.Category.CONFIGURATION));
        return result;
    }

    @ParameterizedTest
    @ValueSource(strings = {"malformedNamed", "missingBinding", "namedConflict", "indexedConflict"})
    void normalizationFindingsSurviveReturnedSnapshotsUnrelatedAssignmentsAndModelRecapture(String kind) throws Exception {
        var preparation = new CircuitPreparation();
        var base = preparation.capture(invalid(kind));
        var first = inspect(preparation, base);
        String code = kind.endsWith("Conflict") ? "INPUT_REPRESENTATION_CONFLICT" : "INVALID_PARAMETER";
        String path = kind.equals("indexedConflict") ? "R.1.param0" : "R.1.resistance";
        assertTrue(first.report().diagnostics().stream().anyMatch(d -> d.code().equals(code) && path.equals(d.parameterPath())));
        assertEquals(first.report(), inspect(preparation, first.circuit()).report());
        var changed = assertInstanceOf(PreparationResult.Inspectable.class, preparation.prepare(first.circuit(),
                new PreparationRequest(SolverOverrides.none(), Map.of("U.1.dcValue", 12.0))));
        assertEquals(first.report(), changed.report());
        assertEquals(first.report(), inspect(preparation, changed.circuit()).report());
        assertEquals(first.report(), inspect(preparation, preparation.capture(first.circuit().toModel())).report());
        assertEquals(first.report(), inspect(preparation, base).report());
    }

    @ParameterizedTest
    @ValueSource(strings = {"malformedNamed", "missingBinding", "namedConflict", "indexedConflict"})
    void aValidMatchingLogicalFieldRepairsRawAndEffectiveSnapshotsAndTheRepairPersists(String kind) throws Exception {
        var preparation = new CircuitPreparation();
        var base = preparation.capture(invalid(kind));
        var first = inspect(preparation, base);
        for (var source : new CircuitSnapshot[]{base, first.circuit()}) {
            for (String path : new String[]{"R.1.resistance", "R.1.param0"}) {
                var repaired = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(source,
                        new PreparationRequest(SolverOverrides.none(), Map.of(path, 20.0))));
                assertTrue(repaired.report().diagnostics().isEmpty(), repaired.report().toString());
                for (double current : CircuitPreparationTest.currents(repaired.execution())) assertEquals(0.3, current, 1e-10);
                var repeated = assertInstanceOf(PreparationResult.Ready.class,
                        preparation.prepare(repaired.circuit(), PreparationRequest.defaults()));
                assertEquals(repaired.report(), repeated.report());
                assertEquals(CircuitPreparationTest.currents(repaired.execution()), CircuitPreparationTest.currents(repeated.execution()));
            }
        }
        assertEquals(first.report(), inspect(preparation, base).report());
        assertEquals(first.report(), inspect(preparation, first.circuit()).report());
    }

    @Test
    void repairingOneFieldRetainsIndependentEvidenceAndRejectedAssignmentsDoNotRepairTheBase() throws Exception {
        var preparation = new CircuitPreparation();
        var base = preparation.capture(saved("dcValue broken", "nameOpt[] /missing"));
        var first = inspect(preparation, base);
        assertEquals(2, first.report().diagnostics().size());
        var partial = assertInstanceOf(PreparationResult.Inspectable.class, preparation.prepare(first.circuit(),
                new PreparationRequest(SolverOverrides.none(), Map.of("R.1.param0", 20.0))));
        assertEquals(1, partial.report().diagnostics().size());
        assertEquals("U.1.dcValue", partial.report().diagnostics().getFirst().parameterPath());
        assertEquals(partial.report(), inspect(preparation, partial.circuit()).report());
        var repaired = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(partial.circuit(),
                new PreparationRequest(SolverOverrides.none(), Map.of("U.1.param1", 6.0))));
        for (double current : CircuitPreparationTest.currents(repaired.execution())) assertEquals(0.3, current, 1e-10);
        assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(repaired.circuit(), PreparationRequest.defaults()));
        assertInstanceOf(PreparationResult.Rejected.class, preparation.prepare(first.circuit(),
                new PreparationRequest(SolverOverrides.none(), Map.of("R.1.resistance", -1.0, "U.1.dcValue", 12.0))));
        assertEquals(first.report(), inspect(preparation, base).report());
        assertEquals(first.report(), inspect(preparation, first.circuit()).report());
    }
    @ParameterizedTest
    @ValueSource(strings = {"phaseDegrees", "phase", "param3"})
    void aControlPhaseAliasRepairsItsLogicalFieldWithoutChangingItsUnits(String name) {
        var model = CircuitPreparationTest.resistorCircuit();
        var signal = CircuitPreparationTest.component(4, "signal", new double[]{400, 2, 1, 0, 0, 0.5}, "", "wave");
        signal.setParameter("phase", 90.0);
        model.addControlComponent(signal);
        model.addControlComponent(CircuitPreparationTest.component(5, "scope", new double[0], "wave", ""));
        var preparation = new CircuitPreparation();
        var initial = inspect(preparation, preparation.capture(model));
        assertEquals("signal.phase", initial.report().diagnostics().getFirst().parameterPath());
        double value = name.equals("param3") ? Math.PI / 4 : 45;
        var repaired = assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(initial.circuit(),
                new PreparationRequest(SolverOverrides.none(), Map.of("signal." + name, value))));
        var run = new gecko.core.simulation.HeadlessSimulationEngine().runSimulation(repaired.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(run.isSuccess(), run.getErrorMessage());
        assertEquals(-Math.sqrt(2), run.getSignalData(0)[0], 1e-6);
        assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(repaired.circuit(), PreparationRequest.defaults()));
        assertEquals(initial.report(), inspect(preparation, initial.circuit()).report());
    }

    @Test
    void persistentConfigurationEvidenceDoesNotSuppressIndependentTopologyFindings() throws Exception {
        var model = saved("", "resistance broken");
        model.addCircuitComponent(CircuitPreparationTest.component(1, "floating", new double[]{10}, "x", "y"));
        var preparation = new CircuitPreparation();
        var first = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        assertTrue(first.report().diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_PARAMETER")
                && "R.1.resistance".equals(d.parameterPath()) && d.category() == Diagnostic.Category.CONFIGURATION));
        assertTrue(first.report().diagnostics().stream().anyMatch(d -> d.code().equals("UNCONNECTED_GROUND")
                && "floating".equals(d.component()) && d.category() == Diagnostic.Category.TOPOLOGY));
        var repeated = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(first.circuit(), PreparationRequest.defaults()));
        assertEquals(first.report(), repeated.report());
        var partial = assertInstanceOf(PreparationResult.Inspectable.class, preparation.prepare(first.circuit(),
                new PreparationRequest(SolverOverrides.none(), Map.of("R.1.resistance", 20.0))));
        assertEquals(1, partial.report().diagnostics().size());
        assertEquals("UNCONNECTED_GROUND", partial.report().diagnostics().getFirst().code());
    }
}
