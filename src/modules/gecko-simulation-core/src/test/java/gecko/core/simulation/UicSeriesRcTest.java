package gecko.core.simulation;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.importer.SpiceImporter;
import gecko.core.preparation.*;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class UicSeriesRcTest {
    private static final double DT = 1e-6;

    private static String deck(double initial) {
        return "V1 supply 0 1\nR1 supply out 1k\nC1 out 0 1u IC=" + initial;
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, .25})
    void observesConstrainedInitialVoltageAndSignedCurrentsBeforeTheFirstAdvance(double initial) throws Exception {
        var source = new SpiceImporter().convert(deck(initial) + "\n.tran 1u 2u uic", "uic.cir");
        assertTrue(source.getConversionIssues().isEmpty(), source.getConversionIssues().toString());
        var model = new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(source)), "uic.ipes");
        assertTrue(model.isTransientUic());
        var preparation = new CircuitPreparation();
        var prepared = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults())).execution();
        var trace = new ArrayList<double[]>();
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, volts, currents, netlist) -> {
            int sourceIndex = netlist.getElementIndexByName("V1");
            int resistor = netlist.getElementIndexByName("R1");
            int capacitor = netlist.getElementIndexByName("C1");
            trace.add(new double[]{time, volts[netlist.getNodeX(sourceIndex)], volts[netlist.getNodeX(capacitor)],
                    currents[sourceIndex], currents[resistor], currents[capacitor]});
        });
        var result = engine.runSimulation(prepared, ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(3, trace.size());
        for (int i = 0; i < trace.size(); i++) {
            double capacitorV = 1 + (initial - 1) / Math.pow(1 + DT / 1e-3, i);
            double current = (1 - capacitorV) / 1000;
            assertArrayEquals(new double[]{i * DT, 1, capacitorV, -current, current, current}, trace.get(i), 1e-12);
            assertEquals(i * DT, result.getDataContainer().getTimeValue(i, 0), 1e-16);
        }
        assertEquals(2, result.getTotalTimeSteps());
    }

    @Test
    void retainsTheAbsoluteOutputWindowThroughPauseAndContinuation() {
        var source = new SpiceImporter().convert(deck(.25) + "\n.tran 1u 2u 1.5u uic", "window.cir");
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(source), PreparationRequest.defaults()));
        try (var session = SimulationSession.open(ready.execution(), new ExecutionOptions(true, 2))) {
            assertEquals(0, session.step().time());
            assertEquals(0, session.status().committedSteps());
            assertEquals(0, session.observe(0, 8).times().length);
            session.run();
            assertArrayEquals(new double[]{2 * DT}, session.observe(0, 8).times(), 1e-16);
            var originalStatus = session.status();
            assertThrows(IllegalArgumentException.class, () -> session.extend(4 * DT, 2 * DT));
            assertEquals(originalStatus, session.status());
            assertArrayEquals(new double[]{2 * DT}, session.observe(0, 8).times(), 1e-16);
            session.extend(4 * DT);
            session.run();
            assertArrayEquals(new double[]{2 * DT, 4 * DT}, session.observe(0, 8).times(), 1e-16);
            assertEquals(4, session.status().committedSteps());
        }
    }

    @Test
    void rejectsIncompatibleTopologyAndExplicitOperatingPointBeforeExecution() {
        var prep = new CircuitPreparation();
        var conflicting = new SpiceImporter().convert(
                "V1 supply 0 1\nR1 supply 0 1k\nC1 supply 0 1u\n.tran 1u 2u uic", "conflict.cir");
        assertInstanceOf(PreparationResult.Rejected.class,
                prep.prepare(prep.capture(conflicting), PreparationRequest.defaults()));
        var source = new SpiceImporter().convert(deck(0) + "\n.tran 1u 2u uic", "uic.cir");
        var dc = new PreparationRequest(new SolverOverrides(null, null, null,
                new SolverFeatures(0, false, true)), Map.of());
        var rejection = assertInstanceOf(PreparationResult.Rejected.class, prep.prepare(prep.capture(source), dc));
        assertTrue(rejection.report().diagnostics().stream().anyMatch(d -> "transientUic".equals(d.parameterPath())));
    }

    @Test
    void rejectsFixedPolicyConflictsWithoutChangingTheCapturedCircuit() {
        var initializer = new SpiceImporter();
        var model = initializer.convert(deck(.25) + "\n.tran 1u 2u uic", "uic.cir");
        var prep = new CircuitPreparation();
        var snapshot = prep.capture(model);
        var requests = java.util.List.of(
                new PreparationRequest(new SolverOverrides(gecko.core.allg.SolverType.SOLVER_TRZ, null, null), Map.of()),
                new PreparationRequest(new SolverOverrides(null, null, null, new SolverFeatures(0, true)), Map.of()),
                new PreparationRequest(new SolverOverrides(null, null, null,
                        new SolverFeatures(0, false, false, new gecko.core.allg.PeriodicSteadyStateSettings(1e-6))), Map.of()),
                new PreparationRequest(new SolverOverrides(null, null, null,
                        new SolverFeatures(0, false, false, null, new gecko.core.allg.AdaptiveStepSettings())), Map.of()),
                new PreparationRequest(SolverOverrides.none(), Map.of("R1.resistance", 0.0)));
        for (var request : requests) {
            var rejected = assertInstanceOf(PreparationResult.Rejected.class, prep.prepare(snapshot, request));
            assertTrue(rejected.report().diagnostics().stream().anyMatch(d -> "transientUic".equals(d.parameterPath())),
                    rejected.report().diagnostics().toString());
        }
        assertInstanceOf(PreparationResult.Ready.class, prep.prepare(snapshot, PreparationRequest.defaults()));
        model.setPreSimulationTime(1e-6);
        model.setPreSimulationTimeStep(1e-6);
        var warmup = assertInstanceOf(PreparationResult.Rejected.class,
                prep.prepare(prep.capture(model), PreparationRequest.defaults()));
        assertTrue(warmup.report().diagnostics().stream().anyMatch(d -> "transientUic".equals(d.parameterPath())));

        var ac = initializer.convert(deck(0) + "\n.ac dec 10 1 100", "ac.cir");
        ac.setTransientUic(true);
        var invalidAc = assertInstanceOf(PreparationResult.Rejected.class,
                prep.prepare(prep.capture(ac), PreparationRequest.defaults()));
        assertTrue(invalidAc.report().diagnostics().stream().anyMatch(d -> "transientUic".equals(d.parameterPath())));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "V1 supply 0 PULSE(0 1 0 1n 1n 1m 2m)\nR1 supply out 1k\nC1 out 0 1u",
            "I1 supply 0 1m\nR1 supply out 1k\nC1 out 0 1u",
            "V1 supply 0 1\nI1 supply out 1m\nC1 out 0 1u",
            "V1 supply 0 1\nR1 supply out 1k\nR2 out 0 1k",
            "V1 supply 0 1\nC1 out 0 1u",
            "V1 0 supply 1\nR1 supply out 1k\nC1 out 0 1u",
            "V1 supply 0 1\nR1 supply out 1k\nC1 0 out 1u",
            "V1 supply out 1\nR1 supply out 1k\nC1 out 0 1u",
            "V1 supply 0 1\nR1 supply out 1k\nC1 out supply 1u",
            "V1 supply 0 1\nR1 out supply 1k\nC1 out 0 1u",
            "V1 supply 0 1\nR1 supply 0 1k\nC1 out 0 1u"
    })
    void rejectsSourcesOrSignedTopologiesOutsideThePreparedRcContract(String circuit) {
        var model = new SpiceImporter().convert(circuit + "\n.tran 1u 2u uic", "unsupported.cir");
        var prep = new CircuitPreparation();
        var rejected = assertInstanceOf(PreparationResult.Rejected.class,
                prep.prepare(prep.capture(model), PreparationRequest.defaults()));
        assertTrue(rejected.report().diagnostics().stream().anyMatch(d -> "transientUic".equals(d.parameterPath())),
                rejected.report().diagnostics().toString());
    }

    @Test
    void duplicateTransientCannotSilentlyChangeTheUicTimings() {
        var model = new SpiceImporter().convert(deck(0) + "\n.tran 1u 2u uic\n.tran 1u 3u", "duplicate.cir");
        assertTrue(model.getConversionIssues().stream().anyMatch(issue -> issue.code().equals("MULTIPLE_ANALYSES")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"transientUic 2", "transientUic 1 0", "transientUic 1\ntransientUic 0"})
    void malformedNativeUicMarkerCannotPrepare(String token) throws Exception {
        assertThrows(CircuitFileParser.CircuitParseException.class,
                () -> new CircuitFileParser().parse(new BufferedReader(new StringReader(
                        "tDURATION 0.002\ndt 0.000001\n" + token)), "bad.ipes"));
    }
}
