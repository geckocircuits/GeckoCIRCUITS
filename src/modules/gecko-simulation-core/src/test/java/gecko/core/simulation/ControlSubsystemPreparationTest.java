package gecko.core.simulation;

import static gecko.core.simulation.ControlSubsystemRateTest.block;
import static gecko.core.simulation.ControlSubsystemRateTest.parse;
import static gecko.core.simulation.ControlSubsystemRateTest.row;
import static gecko.core.simulation.ControlSubsystemRateTest.sheet;
import static org.junit.jupiter.api.Assertions.*;

import gecko.core.allg.SolverSettingsCore;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import gecko.core.io.importer.LtspiceImporter;
import gecko.core.preparation.*;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Per-subsystem CONTROL periods: validation, identity, persistence and settings plumbing. */
class ControlSubsystemPreparationTest {
    @TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.NEVER) Path sources;

    private static String clock(long sheetId, String name, long id) {
        return block(58, name + "Time", id, sheetId, "", "", "/" + name + "clock")
                + block(5, name + "Scope", id + 1, sheetId, "", "/" + name + "clock", "");
    }

    private static CircuitModel twoSheets() throws Exception {
        return parse(sheet(101, 0, "fast") + sheet(202, 0, "slow") + clock(101, "fast", 1) + clock(202, "slow", 3));
    }

    private static PreparationResult prepare(CircuitModel model, double global, Map<String, Double> periods) {
        var preparation = new CircuitPreparation();
        return preparation.prepare(preparation.capture(model), new PreparationRequest(
                new SolverOverrides(null, null, null, new SolverFeatures(global, false, false, null, null, null, periods)),
                Map.of()));
    }

    private static List<String> rejectedPaths(PreparationResult result) {
        var report = assertInstanceOf(PreparationResult.Rejected.class, result, result.report().toString()).report();
        return report.diagnostics().stream().filter(item -> item.code().equals("INVALID_SETTING"))
                .map(Diagnostic::parameterPath).toList();
    }

    @Test
    void aValidMapIsReadyAndAnEmptyMapIsUnchanged() throws Exception {
        assertInstanceOf(PreparationResult.Ready.class, prepare(twoSheets(), 0, Map.of("fast", 10e-6, "slow", 100e-6)));
        assertInstanceOf(PreparationResult.Ready.class, prepare(twoSheets(), 0, Map.of()));
        assertInstanceOf(PreparationResult.Ready.class, prepare(twoSheets(), 0, null));
    }

    @Test
    void anUnknownPathIsRejectedAtItsParameterPath() throws Exception {
        assertEquals(List.of("controlSubsystemPeriods.nowhere"), rejectedPaths(prepare(twoSheets(), 0, Map.of("nowhere", 1e-5))));
    }

    @Test
    void invalidKeysAndValuesAreRejectedWithoutBeingIgnored() throws Exception {
        for (String key : List.of("a//b", "/fast", "fast/", " ")) {
            assertEquals(List.of("controlSubsystemPeriods." + key), rejectedPaths(prepare(twoSheets(), 0, Map.of(key, 1e-5))), key);
        }
        for (double value : new double[]{Double.NaN, -1e-6, Double.POSITIVE_INFINITY}) {
            assertEquals(List.of("controlSubsystemPeriods.fast"), rejectedPaths(prepare(twoSheets(), 0, Map.of("fast", value))));
        }
    }

    @Test
    void legacySiblingsThatDifferOnlyInCaseMakeAPathAmbiguous() throws Exception {
        var model = parse(sheet(101, 0, "Fast") + sheet(202, 0, "fast") + clock(101, "a", 1) + clock(202, "b", 3));
        assertEquals(List.of("controlSubsystemPeriods.FAST"), rejectedPaths(prepare(model, 0, Map.of("FAST", 1e-5))));
    }

    @Test
    void twoSpellingsOfOneSheetCannotSelectConflictingPeriods() throws Exception {
        var paths = rejectedPaths(prepare(twoSheets(), 0, Map.of("fast", 10e-6, "FAST", 20e-6)));
        assertEquals(1, paths.size(), paths.toString());
        assertTrue(paths.get(0).equals("controlSubsystemPeriods.fast") || paths.get(0).equals("controlSubsystemPeriods.FAST"));
    }

    @Test
    void aSheetWithoutControlBlocksIsRejected() throws Exception {
        var model = parse(sheet(101, 0, "fast") + sheet(303, 0, "empty") + clock(101, "fast", 1));
        assertEquals(List.of("controlSubsystemPeriods.empty"), rejectedPaths(prepare(model, 0, Map.of("empty", 1e-5))));
    }

    @Test
    void aBrokenParentChainIsRejectedOnlyWhenPeriodsAreConfigured() throws Exception {
        var model = parse(sheet(101, 0, "fast") + clock(101, "fast", 1) + clock(999, "lost", 3));
        assertEquals(List.of("controlSubsystemPeriods"), rejectedPaths(prepare(model, 0, Map.of("fast", 1e-5))));
        assertEquals(List.of(), prepare(model, 0, Map.of()).report().diagnostics().stream()
                .filter(item -> item.code().equals("INVALID_SETTING")).toList());
    }

    @Test
    void aCycleThatExistsOnlyAtTheSlowStepIsRejected() throws Exception {
        // The 20 us delay breaks the loop at the 1 us step, but not at the 100 us controller step of the slow group.
        var model = parse(sheet(202, 0, "slow")
                + block(25, "delay", 1, 202, "2.0E-5", "/back", "/delayed")
                + block(7, "gain", 2, 202, "1", "/delayed", "/back")
                + block(5, "scope", 3, 202, "", "/back", ""));
        assertInstanceOf(PreparationResult.Ready.class, prepare(model, 0, Map.of()));
        assertEquals(List.of("controlSubsystemPeriods"), rejectedPaths(prepare(model, 0, Map.of("slow", 100e-6))));
        assertInstanceOf(PreparationResult.Ready.class, prepare(model, 0, Map.of("slow", 5e-6)));
    }

    @Test
    void aNestedPathInheritsFromTheNearestConfiguredSheetByIdentifier() throws Exception {
        var model = parse(sheet(101, 0, "outer") + sheet(102, 101, "inner") + sheet(103, 102, "deep")
                + clock(101, "outer", 1) + clock(103, "deep", 3));
        var plan = ControlSubsystems.plan(model, new ControlSubsystems.Spec(0, Map.of("outer", 100e-6, "outer/INNER", 10e-6)));
        assertEquals(List.of(), plan.issues());
        var groups = new HashMap<String, Double>();
        for (var component : model.getControlComponents()) {
            groups.put(component.getName(), plan.periods()[ControlSubsystems.groupOf(plan, component)]);
        }
        assertEquals(100e-6, groups.get("outerTime"));
        assertEquals(10e-6, groups.get("deepTime"), "a block three sheets down takes the nearest configured period");
        // The bare name of a nested sheet is not a path.
        assertEquals(1, ControlSubsystems.plan(model, new ControlSubsystems.Spec(0, Map.of("inner", 1e-5))).issues().size());
        try (var session = ControlSubsystemRateTest.open(model, ControlSubsystemRateTest.settings(1e-6, 3e-4, 0,
                Map.of("outer", 100e-6, "outer/inner", 10e-6)))) {
            session.run();
            assertEquals(100e-6, row(session, "outerclock")[150], 1e-9);
            assertEquals(140e-6, row(session, "deepclock")[149], 1e-9);
        }
    }

    @Test
    void aSubsystemPeriodMustAlsoDivideThePeriodicSteadyStatePeriod() {
        var pss = new gecko.core.allg.PeriodicSteadyStateSettings(1e-3, 1e-4, 10);
        assertEquals(List.of(), SimulationTiming.periodicSteadyStateIssues(pss, 1e-6, 1e-3, 0, 0, Map.of("a", 100e-6)));
        var issues = SimulationTiming.periodicSteadyStateIssues(pss, 1e-6, 1e-3, 0, 0, Map.of("a", 300e-6));
        assertEquals("periodicSteadyState.period", issues.get(0).parameterPath());
        assertTrue(issues.get(0).message().contains("'a'"));
    }

    @Test
    void theWriterKeepsEveryBlocksGroup() throws Exception {
        var model = twoSheets();
        var again = new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(model)), "detached.ipes");
        var spec = new ControlSubsystems.Spec(0, Map.of("fast", 10e-6, "slow", 100e-6));
        var before = ControlSubsystems.plan(model, spec);
        var after = ControlSubsystems.plan(again, spec);
        assertEquals(List.of(), after.issues());
        var groups = new HashMap<String, Integer>();
        for (var component : model.getControlComponents()) groups.put(component.getName(), ControlSubsystems.groupOf(before, component));
        assertEquals(4, groups.size());
        for (var component : again.getControlComponents()) {
            assertEquals(groups.get(component.getName()), ControlSubsystems.groupOf(after, component), component.getName());
        }
        try (var original = ControlSubsystemRateTest.open(model, ControlSubsystemRateTest.settings(1e-6, 3e-4, 0,
                spec.periods())); var reread = ControlSubsystemRateTest.open(again, ControlSubsystemRateTest.settings(1e-6, 3e-4, 0,
                spec.periods()))) {
            original.run();
            reread.run();
            assertArrayEquals(row(original, "slowclock"), row(reread, "slowclock"));
            assertArrayEquals(row(original, "fastclock"), row(reread, "fastclock"));
        }
    }

    @Test
    void aNormalizedAndAnUnnormalizedModelAssignTheSameGroups() throws Exception {
        var unresolved = twoSheets();
        CircuitModel normalized = twoSheets();
        normalized = CircuitConfiguration.resolveSavedControl(normalized);
        var periods = Map.of("fast", 10e-6, "slow", 100e-6);
        try (var left = ControlSubsystemRateTest.open(unresolved, ControlSubsystemRateTest.settings(1e-6, 3e-4, 0, periods));
             var right = ControlSubsystemRateTest.open(normalized, ControlSubsystemRateTest.settings(1e-6, 3e-4, 0, periods))) {
            left.run();
            right.run();
            assertArrayEquals(row(left, "slowclock"), row(right, "slowclock"));
            assertEquals(100e-6, row(left, "slowclock")[150], 1e-9);
        }
    }

    // --- native hierarchy ---------------------------------------------------------------------------------------

    private CircuitModel nativeHierarchy() throws Exception {
        Path directory = Files.createTempDirectory(sources, "hierarchy");
        for (String name : List.of("leaf.asc", "leaf.asy", "wrapper.asc", "wrapper.asy", "root.asc", "res.asy", "cap.asy",
                "voltage.asy")) {
            try (var input = java.util.Objects.requireNonNull(getClass().getResourceAsStream("/ltspice-hierarchy/" + name))) {
                Files.write(directory.resolve(name), input.readAllBytes(), java.nio.file.StandardOpenOption.CREATE_NEW);
            }
        }
        return new LtspiceImporter().convert(directory.resolve("root.asc"), directory, List.of(directory));
    }

    @Test
    void nativeOccurrencePathsResolveWithoutRegardToCaseAndABareLeafNameIsUnknown() throws Exception {
        var model = nativeHierarchy();
        // Native sheets hold no CONTROL block, so a resolved path reports an empty subsystem and an unresolved one is unknown.
        for (String path : List.of("X1", "X1/Xleaf", "X2", "X2/Xleaf", "x1/XLEAF")) {
            var issues = ControlSubsystems.plan(model, new ControlSubsystems.Spec(0, Map.of(path, 1e-5))).issues();
            assertEquals(1, issues.size(), path);
            assertTrue(issues.get(0).message().contains("has no CONTROL block"), path + ": " + issues.get(0).message());
        }
        var unknown = ControlSubsystems.plan(model, new ControlSubsystems.Spec(0, Map.of("Xleaf", 1e-5))).issues();
        assertTrue(unknown.get(0).message().contains("No subcircuit sheet"), unknown.toString());
        // The transient gate for native hierarchy documents stays in front of every CONTROL check.
        model.setAcSettings(null);
        var result = prepare(model, 0, Map.of("X1/Xleaf", 1e-5));
        assertInstanceOf(PreparationResult.Rejected.class, result);
        assertTrue(result.report().diagnostics().stream().anyMatch(item -> item.code().equals("UNSUPPORTED_TRANSIENT_LAW")),
                result.report().toString());
    }

    @Test
    void aControlBlockInsideANativeSheetStaysUnparseable() throws Exception {
        var model = nativeHierarchy();
        var bytes = new CircuitFileWriter().write(model);
        try (var unzipped = new java.util.zip.GZIPInputStream(new ByteArrayInputStream(bytes))) {
            bytes = unzipped.readAllBytes();
        }
        String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        // Put a CONTROL block into a native sheet: the hierarchy gate requires a member key that CONTROL cannot carry.
        long sheetId = model.getSpecialComponents().stream().filter(component -> component.getType() == 27)
                .mapToLong(component -> component.getSavedBlock().uniqueObjectIdentifier()).findFirst().orElseThrow();
        String injected = text + "\n" + block(58, "inside", 900, sheetId, "", "", "/x");
        CircuitModel parsed;
        try {
            parsed = new CircuitFileParser().parse(
                    new ByteArrayInputStream(injected.getBytes(java.nio.charset.StandardCharsets.UTF_8)), "inject.ipes");
        } catch (Exception failure) {
            assertTrue(String.valueOf(failure.getMessage()).contains("Invalid circuit hierarchy"), String.valueOf(failure.getMessage()));
            return;
        }
        fail("parsed with " + parsed.getControlComponents().size() + " control blocks; tail: "
                + text.substring(Math.max(0, text.length() - 200)));
    }

    // --- settings plumbing --------------------------------------------------------------------------------------

    @Test
    void everyOldConstructorHasNoSubsystemPeriodsAndTheMapIsDetachedAndSorted() {
        assertEquals(Map.of(), SolverFeatures.none().controlSubsystemPeriods());
        assertEquals(Map.of(), new SolverFeatures(1e-5).controlSubsystemPeriods());
        assertEquals(Map.of(), new SolverFeatures(1e-5, true).controlSubsystemPeriods());
        assertEquals(Map.of(), new SolverFeatures(1e-5, true, true).controlSubsystemPeriods());
        assertEquals(Map.of(), new SolverFeatures(1e-5, true, true, null).controlSubsystemPeriods());
        assertEquals(Map.of(), new SolverFeatures(1e-5, true, true, null, null).controlSubsystemPeriods());
        assertEquals(Map.of(), new SolverFeatures(1e-5, true, true, null, null, null).controlSubsystemPeriods());
        assertEquals(SolverFeatures.none(), new SolverFeatures(0, false, false, null, null, null, Map.of()));
        assertEquals(SolverFeatures.none(), new SolverFeatures(0, false, false, null, null, null, null));
        var source = new HashMap<String, Double>(Map.of("b", 2e-5, "a", 1e-5));
        var features = new SolverFeatures(0, false, false, null, null, null, source);
        source.put("c", 3e-5);
        assertEquals(List.of("a", "b"), List.copyOf(features.controlSubsystemPeriods().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> features.controlSubsystemPeriods().put("c", 1.0));
        assertNotEquals(SolverFeatures.none(), features);
    }

    @Test
    void theMapSurvivesSettingsCopiesConfigAndPreparedCircuits() throws Exception {
        var settings = new SolverSettingsCore().withControlSubsystemPeriods(Map.of("fast", 10e-6));
        assertEquals(Map.of("fast", 10e-6), settings.copy().getControlSubsystemPeriods());
        assertEquals(Map.of(), new SolverSettingsCore().getControlSubsystemPeriods());
        var config = SimulationConfig.builder().controlSubsystemPeriod("slow", 100e-6).controlSubsystemPeriod("fast", 10e-6).build();
        assertEquals(List.of("fast", "slow"), List.copyOf(config.getSolverSettings().getControlSubsystemPeriods().keySet()));
        assertEquals(config.getSolverSettings().getControlSubsystemPeriods(),
                config.explicitSolverOverrides().features().controlSubsystemPeriods());
        assertEquals(Map.of(), SimulationConfig.builder().build().explicitSolverOverrides().features().controlSubsystemPeriods());
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                prepare(twoSheets(), 0, Map.of("fast", 10e-6, "slow", 100e-6)));
        var prepared = ready.execution();
        assertEquals(Map.of("fast", 10e-6, "slow", 100e-6), prepared.solverSettings().getControlSubsystemPeriods());
        prepared.solverSettings().withControlSubsystemPeriods(Map.of());
        assertEquals(2, prepared.solverSettings().getControlSubsystemPeriods().size(), "settings are detached");
        for (int run = 0; run < 2; run++) {
            try (var session = SimulationSession.open(prepared, ExecutionOptions.defaults())) {
                session.run();
                assertEquals(100e-6, row(session, "slowclock")[150], 1e-9, "run " + run);
                assertEquals(150e-6, row(session, "fastclock")[150], 1e-9, "run " + run);
            }
        }
    }
}
