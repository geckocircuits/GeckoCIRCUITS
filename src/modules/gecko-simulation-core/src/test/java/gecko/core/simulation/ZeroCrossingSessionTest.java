package gecko.core.simulation;

import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

/** Settings, prepared-circuit, session and engine paths of zero-crossing location. */
class ZeroCrossingSessionTest {
    private static final SolverFeatures ON = new SolverFeatures(0, true);

    private static CircuitModel model(String circuit) throws Exception {
        var resource = Objects.requireNonNull(ZeroCrossingSessionTest.class.getResource("/ipes/" + circuit + ".ipes"));
        return new CircuitFileParser().parse(Path.of(resource.toURI()).toString());
    }

    private static PreparedCircuit prepare(String circuit, SolverOverrides overrides) throws Exception {
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model(circuit)), new PreparationRequest(overrides, Map.of()));
        return assertInstanceOf(PreparationResult.Ready.class, result).execution();
    }

    private static SolverOverrides features(SolverFeatures features) { return new SolverOverrides(null, null, null, features); }

    @Test
    void theSettingFlowsFromTheBuilderThroughPreparationToTheSession() throws Exception {
        assertFalse(SimulationConfig.builder().build().getSolverSettings().isZeroCrossing(), "off by default");
        assertFalse(SimulationConfig.builder().build().explicitSolverOverrides().features().zeroCrossing());
        var config = SimulationConfig.builder().zeroCrossing(true).build();
        assertTrue(config.getSolverSettings().isZeroCrossing());
        assertTrue(config.explicitSolverOverrides().features().zeroCrossing());
        assertTrue(config.getSolverSettings().copy().isZeroCrossing(), "copy keeps the setting");
        assertFalse(SolverFeatures.none().zeroCrossing());
        assertEquals(new SolverFeatures(1e-5, false), new SolverFeatures(1e-5));

        var prepared = prepare("buck_simple", features(ON));
        assertTrue(prepared.solverSettings().isZeroCrossing());
        prepared.solverSettings().withZeroCrossing(false);
        assertTrue(prepared.solverSettings().isZeroCrossing(), "settings are detached");
        assertFalse(prepare("buck_simple", SolverOverrides.none()).solverSettings().isZeroCrossing());
        assertFalse(prepare("buck_simple", features(new SolverFeatures(0, false))).solverSettings().isZeroCrossing());

        try (var session = SimulationSession.open(prepared, ExecutionOptions.defaults())) {
            session.run();
            var report = session.eventLocationReport();
            assertTrue(report.located() > 0, "the buck diode switches inside steps: " + report);
            assertEquals(report.retained(), session.zeroCrossingEvents(0, 10000).size());
        }
        try (var session = SimulationSession.open(prepare("buck_simple", SolverOverrides.none()), ExecutionOptions.defaults())) {
            session.run();
            assertEquals(EventLocationReport.empty(), session.eventLocationReport(), "no location when off");
            assertTrue(session.zeroCrossingEvents(0, 10).isEmpty());
        }
    }

    @Test
    void theEngineReportsTheEventCountsOnlyWhenTheFeatureIsOn() throws Exception {
        var path = Path.of(Objects.requireNonNull(ZeroCrossingSessionTest.class.getResource("/ipes/buck_simple.ipes")).toURI()).toString();
        var on = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder().circuitFile(path).zeroCrossing(true).build());
        assertTrue(on.isSuccess(), on.getErrorMessage());
        var metadata = on.getMetadata();
        long located = (Long) metadata.get("zeroCrossingLocated");
        assertTrue(located > 0);
        assertEquals(0L, metadata.get("zeroCrossingUnlocated"));
        assertEquals(0L, metadata.get("zeroCrossingDropped"));
        assertEquals((int) located, metadata.get("zeroCrossingRetained"));
        assertTrue(Double.isNaN((Double) metadata.get("zeroCrossingFirstUnlocatedTime")));
        try (var session = SimulationSession.open(prepare("buck_simple", features(ON)), ExecutionOptions.defaults())) {
            session.run();
            assertEquals(located, session.eventLocationReport().located(), "engine and session agree");
        }
        var off = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder().circuitFile(path).build());
        assertFalse(off.getMetadata().containsKey("zeroCrossingLocated"));
    }

    @Test
    void gearShichmanIsRejectedWhileTheFeatureIsOnAndAcceptedWhenItIsOff() throws Exception {
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(model("buck_simple"));
        var rejected = preparation.prepare(snapshot, new PreparationRequest(
                new SolverOverrides(SolverType.SOLVER_GS, null, null, ON), Map.of()));
        var report = assertInstanceOf(PreparationResult.Rejected.class, rejected).report();
        assertTrue(report.diagnostics().stream().anyMatch(item -> item.code().equals("INVALID_SETTING")
                && "zeroCrossing".equals(item.parameterPath())), report.toString());
        assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot, new PreparationRequest(
                new SolverOverrides(SolverType.SOLVER_GS, null, null, new SolverFeatures(0, false)), Map.of())));
        for (var method : List.of(SolverType.SOLVER_BE, SolverType.SOLVER_TRZ)) {
            assertInstanceOf(PreparationResult.Ready.class, preparation.prepare(snapshot, new PreparationRequest(
                    new SolverOverrides(method, null, null, ON), Map.of())));
        }

        var fileModel = model("buck_simple");
        fileModel.setSolverType(SolverType.SOLVER_GS);
        var fromFile = preparation.prepare(preparation.capture(fileModel), new PreparationRequest(features(ON), Map.of()));
        assertInstanceOf(PreparationResult.Rejected.class, fromFile, "a file solver of GS is rejected as well");

        var settings = new SolverSettingsCore(SolverType.SOLVER_GS, 1e-6, 1e-4).withZeroCrossing(true);
        var issues = SimulationTiming.settingsIssues(settings);
        assertEquals(List.of("zeroCrossing"), issues.stream().map(SimulationTiming.SettingIssue::parameterPath).toList());
        assertThrows(IllegalArgumentException.class, () -> SimulationTiming.validateSettings(settings));
        assertThrows(IllegalArgumentException.class,
                () -> SimulationSession.fromModel(model("buck_simple"), settings, ExecutionOptions.defaults()));
        assertTrue(SimulationTiming.settingsIssues(settings.copy().withZeroCrossing(false)).isEmpty());
        var config = SimulationConfig.builder().circuitFile(Path.of(ZeroCrossingSessionTest.class
                .getResource("/ipes/buck_simple.ipes").toURI()).toString()).solverType(SolverType.SOLVER_GS).zeroCrossing(true).build();
        assertFalse(new HeadlessSimulationEngine().runSimulation(config).isSuccess());
    }

    private static List<long[]> trace(SimulationConfig config) {
        List<long[]> rows = new ArrayList<>();
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, volts, currents, netlist) -> {
            long[] row = new long[1 + volts.length + currents.length];
            row[0] = Double.doubleToRawLongBits(time);
            for (int index = 0; index < volts.length; index++) row[1 + index] = Double.doubleToRawLongBits(volts[index]);
            for (int index = 0; index < currents.length; index++) row[1 + volts.length + index] = Double.doubleToRawLongBits(currents[index]);
            rows.add(row);
        });
        assertTrue(engine.runSimulation(config).isSuccess());
        return rows;
    }

    @Test
    void offIsBitIdenticalToTheDefaultOnRawDoubleBuckTraces() throws Exception {
        String path = Path.of(Objects.requireNonNull(ZeroCrossingSessionTest.class.getResource("/ipes/buck_simple.ipes")).toURI()).toString();
        var defaults = trace(SimulationConfig.builder().circuitFile(path).build());
        var explicitOff = trace(SimulationConfig.builder().circuitFile(path).zeroCrossing(false).build());
        assertFalse(defaults.isEmpty());
        assertEquals(defaults.size(), explicitOff.size());
        for (int step = 0; step < defaults.size(); step++) {
            assertArrayEquals(defaults.get(step), explicitOff.get(step), "step " + step);
        }
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void offIsBitIdenticalToTheDefaultOnRawDoubleRectifierTraces(SolverType method) {
        double dt = ZeroCrossingLocationTest.PERIOD / 50;
        var defaultNetlist = ZeroCrossingLocationTest.rectifier(ZeroCrossingLocationTest.UF, 20e-3);
        var offNetlist = ZeroCrossingLocationTest.rectifier(ZeroCrossingLocationTest.UF, 20e-3);
        var defaults = new ElectricalTimeStep(defaultNetlist, method);
        var explicitOff = new ElectricalTimeStep(offNetlist, method, false);
        for (int step = 0; step < 150; step++) {
            defaults.advance(dt, step * dt, null);
            explicitOff.advanceInterval(dt, step * dt, null);
            assertArrayEquals(rawBits(defaultNetlist.getLastNodeVoltages()), rawBits(offNetlist.getLastNodeVoltages()), "potentials " + step);
            assertArrayEquals(rawBits(defaultNetlist.getLastComponentCurrents()), rawBits(offNetlist.getLastComponentCurrents()), "currents " + step);
            assertEquals(defaultNetlist.getParameter(1)[0], offNetlist.getParameter(1)[0], "diode state " + step);
        }
        assertEquals(EventLocationReport.empty(), explicitOff.eventReport());
    }

    private static long[] rawBits(double[] values) {
        return java.util.Arrays.stream(values).mapToLong(Double::doubleToRawLongBits).toArray();
    }

    @Test
    void onModeKeepsOneRecordedRowAndOnePublishedValuePerGridStep() throws Exception {
        try (var off = SimulationSession.open(prepare("buck_simple", SolverOverrides.none()), ExecutionOptions.defaults());
             var on = SimulationSession.open(prepare("buck_simple", features(ON)), ExecutionOptions.defaults())) {
            off.run();
            on.run();
            var left = off.observe(0, 10000);
            var right = on.observe(0, 10000);
            assertArrayEquals(left.times(), right.times(), "recordings stay on the fixed grid");
            assertEquals(left.values().length, right.values().length);
            assertEquals(off.status().committedSteps(), on.status().committedSteps());
        }
    }

    @Test
    void aSessionContinuesAcrossExtendAndManualSteppingWithTheSameEventLog() throws Exception {
        var base = prepare("buck_simple", features(ON));
        double duration = base.solverSettings().getSimulationDuration();
        var straightPrepared = prepare("buck_simple", new SolverOverrides(null, null, 2 * duration, ON));
        List<ZeroCrossingEvent> straight;
        float[][] straightValues;
        try (var session = SimulationSession.open(straightPrepared, ExecutionOptions.defaults())) {
            session.run();
            straight = session.zeroCrossingEvents(0, 10000);
            straightValues = session.observe(0, 10000).values();
            assertEquals(session.eventLocationReport().located(), straight.size());
        }
        assertFalse(straight.isEmpty());

        try (var session = SimulationSession.open(base, ExecutionOptions.defaults())) {
            for (int step = 0; step < 25; step++) session.step(); // manual steps, then a pause, then a run
            session.run();
            int firstSegment = session.zeroCrossingEvents(0, 10000).size();
            session.extend(2 * duration);
            session.run();
            var events = session.zeroCrossingEvents(0, 10000);
            assertTrue(events.size() > firstSegment, "the continuation locates more events");
            assertEquals(straight.size(), events.size());
            for (int index = 0; index < straight.size(); index++) {
                assertEquals(straight.get(index).getCrossingTime(), events.get(index).getCrossingTime(), "event " + index);
                assertEquals(straight.get(index).getSignalIndex(), events.get(index).getSignalIndex());
                assertEquals(straight.get(index).getDirection(), events.get(index).getDirection());
            }
            assertArrayEquals(straightValues[0], session.observe(0, 10000).values()[0]);
        }
    }

    @Test
    void theEventWindowIsBoundedAndValidated() throws Exception {
        try (var session = SimulationSession.open(prepare("buck_simple", features(ON)), ExecutionOptions.defaults())) {
            session.run();
            var all = session.zeroCrossingEvents(0, 10000);
            assertTrue(all.size() >= 3);
            assertEquals(all.subList(1, 3), session.zeroCrossingEvents(1, 2));
            assertTrue(session.zeroCrossingEvents(all.size() + 5, 10).isEmpty());
            assertTrue(session.zeroCrossingEvents(0, 0).isEmpty());
            assertThrows(IllegalArgumentException.class, () -> session.zeroCrossingEvents(-1, 1));
            assertThrows(IllegalArgumentException.class, () -> session.zeroCrossingEvents(0, 10001));
            assertThrows(IllegalArgumentException.class, () -> session.zeroCrossingEvents(0, -1));
            assertThrows(UnsupportedOperationException.class, () -> all.add(all.get(0)));
        }
    }

    @Test
    void cancellationAndResumeKeepTheLocatedEvents() throws Exception {
        var prepared = prepare("buck_simple", features(ON));
        List<ZeroCrossingEvent> straight;
        try (var session = SimulationSession.open(prepared, ExecutionOptions.defaults())) {
            session.run();
            straight = session.zeroCrossingEvents(0, 10000);
        }
        try (var session = SimulationSession.open(prepared, ExecutionOptions.defaults())) {
            int[] seen = {0};
            session.run(() -> seen[0] >= 40, sample -> seen[0]++);
            assertEquals(40, session.status().committedSteps());
            session.run();
            var events = session.zeroCrossingEvents(0, 10000);
            assertEquals(straight.size(), events.size());
            for (int index = 0; index < events.size(); index++) {
                assertEquals(straight.get(index).getCrossingTime(), events.get(index).getCrossingTime());
            }
        }
    }
}
