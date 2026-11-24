package gecko.core.simulation;

import gecko.core.allg.AdaptiveStepSettings;
import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import gecko.core.allg.SparseSolverSettings;
import gecko.core.allg.SparseSolverSettings.Mode;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.io.CircuitFileParser;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.preparation.PreparedCircuit;
import gecko.core.preparation.SolverFeatures;
import gecko.core.preparation.SolverOverrides;
import gecko.core.simulation.solver.MatrixSolver;
import gecko.core.simulation.solver.MatrixSolver.Backend;
import gecko.core.testutil.SyntheticCircuitBuilder;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** The sparse option through settings, preparation, sessions, engine runs, event location and the adaptive step. */
class SparseSolverSessionTest {
    private static final SparseSolverSettings FORCED = new SparseSolverSettings(Mode.SPARSE);
    private static final AdaptiveStepSettings ADAPTIVE = new AdaptiveStepSettings(1e-3, 1e-9, Double.NaN);
    private static final double TOLERANCE = 1e-9;

    private static String circuit(String name) throws Exception {
        return Path.of(Objects.requireNonNull(SparseSolverSessionTest.class.getResource("/ipes/" + name + ".ipes")).toURI())
                .toString();
    }

    private static PreparedCircuit prepare(String name, SolverFeatures features) throws Exception {
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(new CircuitFileParser().parse(circuit(name)));
        var result = preparation.prepare(snapshot,
                new PreparationRequest(new SolverOverrides(null, null, null, features), Map.of()));
        return assertInstanceOf(PreparationResult.Ready.class, result, () -> result.report().toString()).execution();
    }

    @Test
    void theOptionFlowsFromTheBuilderThroughFeaturesAndPreparationToSessions() throws Exception {
        assertNull(SimulationConfig.builder().build().getSolverSettings().getSparseSolver(), "off by default");
        assertNull(SimulationConfig.builder().build().explicitSolverOverrides().features().sparseSolver());
        var config = SimulationConfig.builder().sparseSolver(FORCED).build();
        assertEquals(FORCED, config.getSolverSettings().getSparseSolver());
        assertEquals(FORCED, config.explicitSolverOverrides().features().sparseSolver());
        assertEquals(FORCED, config.getSolverSettings().copy().getSparseSolver(), "copy keeps the setting");
        assertNull(SolverFeatures.none().sparseSolver());
        assertNull(new SolverFeatures(1e-5).sparseSolver());
        assertNull(new SolverFeatures(1e-5, true).sparseSolver());
        assertNull(new SolverFeatures(1e-5, true, true).sparseSolver());
        assertNull(new SolverFeatures(1e-5, true, true, null).sparseSolver());
        assertNull(new SolverFeatures(1e-5, true, true, null, ADAPTIVE).sparseSolver());
        assertEquals(ADAPTIVE, new SolverFeatures(1e-5, true, true, null, ADAPTIVE, FORCED).adaptiveStep());
        assertEquals(FORCED, new SolverSettingsCore().withSparseSolver(FORCED).getSparseSolver());

        var prepared = prepare("buck_simple", new SolverFeatures(0, false, false, null, null, FORCED));
        assertEquals(FORCED, prepared.solverSettings().getSparseSolver());
        prepared.solverSettings().withSparseSolver(null);
        assertEquals(FORCED, prepared.solverSettings().getSparseSolver(), "prepared settings are detached");
        assertNull(prepare("buck_simple", SolverFeatures.none()).solverSettings().getSparseSolver());
    }

    @Test
    void independentSessionsOwnTheirSolversAndBackends() throws Exception {
        var sparse = prepare("buck_simple", new SolverFeatures(0, false, false, null, null, FORCED));
        var dense = prepare("buck_simple", SolverFeatures.none());
        try (var first = SimulationSession.open(sparse, ExecutionOptions.defaults());
                var second = SimulationSession.open(sparse, ExecutionOptions.defaults());
                var plain = SimulationSession.open(dense, ExecutionOptions.defaults())) {
            assertEquals(Backend.SPARSE, first.linearSolverReport().backend());
            assertEquals(Backend.SPARSE, second.linearSolverReport().backend());
            assertEquals(Backend.DENSE, plain.linearSolverReport().backend());
            assertEquals(0, first.linearSolverReport().companionStoredCoefficients());
            assertNull(first.linearSolverReport().companionBackend());
            first.run();
            assertTrue(first.linearSolverReport().factorizations() > 0);
            assertEquals(0, second.linearSolverReport().factorizations(), "an untouched session shares no solver state");
            assertEquals(0, plain.linearSolverReport().factorizations());
            plain.run();
            second.run();
            assertEquals(first.status().committedSteps(), second.status().committedSteps());
            assertArrayEquals(first.observe(0, 10000).times(), second.observe(0, 10000).times());
            assertEquals(first.linearSolverReport().storedCoefficients(), second.linearSolverReport().storedCoefficients());
            assertEquals(Backend.DENSE, plain.linearSolverReport().backend());
        }
    }

    @Test
    void aWiredCircuitReportsTheLegacyOrderKernelForEverySolverItBuilds() throws Exception {
        var features = new SolverFeatures(0, true, true, null, ADAPTIVE, FORCED);
        try (var session = SimulationSession.open(prepare("wired_divider", features), ExecutionOptions.defaults())) {
            session.run();
            var report = session.linearSolverReport();
            assertEquals(Backend.SPARSE, report.backend());
            assertEquals(MatrixSolver.SparseFactorization.LEGACY_ORDER, report.factorization());
            assertEquals(MatrixSolver.SparseFactorization.LEGACY_ORDER, report.initializationFactorization(),
                    "the DC solver inherits the wire reference and the settings");
            if (report.companionBackend() != null) {
                assertEquals(MatrixSolver.SparseFactorization.LEGACY_ORDER, report.companionFactorization());
            }
        }
        var dense = prepare("wired_divider", new SolverFeatures(0, true, true, null, ADAPTIVE));
        try (var session = SimulationSession.open(dense, ExecutionOptions.defaults())) {
            session.run();
            var report = session.linearSolverReport();
            assertNull(report.factorization(), "dense has no sparse factorization");
            assertNull(report.initializationFactorization());
        }
    }

    @Test
    void automaticSelectionFollowsTheThresholdOnAPhysicalNodeCount() throws Exception {
        // buck_simple has far fewer than 100 nodes, so the default AUTO threshold keeps the dense LU.
        try (var session = SimulationSession.open(prepare("buck_simple",
                new SolverFeatures(0, false, false, null, null, new SparseSolverSettings())), ExecutionOptions.defaults())) {
            assertEquals(Backend.DENSE, session.linearSolverReport().backend());
        }
        try (var session = SimulationSession.open(prepare("buck_simple",
                new SolverFeatures(0, false, false, null, null, new SparseSolverSettings(Mode.AUTO, 1))),
                ExecutionOptions.defaults())) {
            assertEquals(Backend.SPARSE, session.linearSolverReport().backend());
        }
        try (var session = SimulationSession.open(prepare("buck_simple",
                new SolverFeatures(0, false, false, null, null, new SparseSolverSettings(Mode.DENSE))),
                ExecutionOptions.defaults())) {
            assertEquals(Backend.DENSE, session.linearSolverReport().backend());
        }
    }

    @Test
    void theEngineReportsTheBackendOnlyWhenTheOptionIsSet() throws Exception {
        var off = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder().circuitFile(circuit("buck_simple")).build());
        assertTrue(off.isSuccess(), off.getErrorMessage());
        assertFalse(off.getMetadata().containsKey("linearSolver"), "the default result keeps its metadata");
        var on = new HeadlessSimulationEngine().runSimulation(
                SimulationConfig.builder().circuitFile(circuit("buck_simple")).sparseSolver(FORCED).build());
        assertTrue(on.isSuccess(), on.getErrorMessage());
        var report = (LinearSolverReport) on.getMetadata().get("linearSolver");
        assertEquals(Backend.SPARSE, report.backend());
        assertTrue(report.storedCoefficients() < (long) report.matrixOrder() * report.matrixOrder());
    }

    private static PhysicalTrace trace(String name, SimulationConfig.Builder builder) throws Exception {
        PhysicalTrace trace = new PhysicalTrace();
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, volts, currents, netlist) -> trace.add(time, volts, currents, netlist));
        var result = engine.runSimulation(builder.circuitFile(circuit(name)).build());
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return trace;
    }

    private static void assertSameTrace(PhysicalTrace dense, PhysicalTrace sparse, String label) {
        PhysicalTrace.assertEquivalent(dense, sparse, label, TOLERANCE);
        System.out.printf("SPARSE-EQUIV %s steps=%d switchTransitions=%d%n", label, dense.times.size(), dense.transitions());
    }

    private static SimulationConfig.Builder feature(SimulationConfig.Builder builder, String feature) {
        return switch (feature) {
            case "plain" -> builder;
            case "zeroCrossing" -> builder.zeroCrossing(true);
            case "adaptive" -> builder.adaptiveStep(ADAPTIVE);
            case "dcStart" -> builder.dcOperatingPoint(true);
            case "all" -> builder.zeroCrossing(true).adaptiveStep(ADAPTIVE).dcOperatingPoint(true);
            default -> throw new IllegalArgumentException(feature);
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"plain", "zeroCrossing", "adaptive", "dcStart", "all"})
    void forcedSparseReproducesTheDenseTraceOfSwitchingCircuitsWithEveryFeatureCombination(String feature) throws Exception {
        for (String name : new String[] {"buck_simple", "boost_simple"}) {
            var dense = trace(name, feature(SimulationConfig.builder(), feature));
            var sparse = trace(name, feature(SimulationConfig.builder().sparseSolver(FORCED), feature));
            assertSameTrace(dense, sparse, name + "/" + feature);
        }
    }

    @Test
    void forcedSparseReproducesDenseOnCoupledAndWireReferenceCircuits() throws Exception {
        for (String name : new String[] {"wired_divider", "legacy_ex1"}) {
            assertSameTrace(trace(name, SimulationConfig.builder()),
                    trace(name, SimulationConfig.builder().sparseSolver(FORCED)), name);
        }
    }

    @Test
    void forcedSparseTakesTheDcSeededPathOnTheSparseBackendAndMatchesDense() throws Exception {
        var prepared = prepare("buck_simple", new SolverFeatures(0, true, true, null, ADAPTIVE, FORCED));
        try (var session = SimulationSession.open(prepared, ExecutionOptions.defaults())) {
            var report = session.linearSolverReport();
            assertEquals(Backend.SPARSE, report.backend());
            assertEquals(Backend.SPARSE, report.companionBackend(), "the adaptive companion inherits the backend");
            assertEquals(Backend.SPARSE, report.initializationBackend(), "the DC solver inherits the backend");
            assertTrue(report.companionStoredCoefficients() < (long) report.matrixOrder() * report.matrixOrder());
            assertTrue(report.initializationStoredCoefficients() < (long) report.matrixOrder() * report.matrixOrder());
            session.run();
            assertTrue(session.linearSolverReport().companionStoredCoefficients() > 0, "the companion actually ran");
        }
        var denseDefault = prepare("buck_simple", new SolverFeatures(0, true, true, null, ADAPTIVE));
        try (var session = SimulationSession.open(denseDefault, ExecutionOptions.defaults())) {
            var report = session.linearSolverReport();
            assertEquals(Backend.DENSE, report.backend());
            assertEquals(Backend.DENSE, report.companionBackend());
            assertEquals(Backend.DENSE, report.initializationBackend());
        }
    }

    /** A large ladder through the electrical step itself, with every M5 option that builds a second solver. */
    private static final int LARGE = 300;

    private static CircuitNetlist ladder() {
        return SyntheticCircuitBuilder.rcLadder(LARGE, 1.0, 1000.0, 1e-6);
    }

    @ParameterizedTest
    @EnumSource(value = SolverType.class, names = {"SOLVER_BE", "SOLVER_TRZ"})
    void aLargeDcSeededAdaptiveLadderKeepsEverySolverSparseAndMatchesDense(SolverType method) {
        double grid = 1e-5;
        var sparseFeatures = new SolverFeatures(0, true, true, null, new AdaptiveStepSettings(1e-3, 1e-8, Double.NaN),
                new SparseSolverSettings());
        var denseFeatures = new SolverFeatures(0, true, true, null, new AdaptiveStepSettings(1e-3, 1e-8, Double.NaN),
                new SparseSolverSettings(Mode.DENSE));
        CircuitNetlist sparseNetlist = ladder();
        CircuitNetlist denseNetlist = ladder();
        var sparse = new ElectricalTimeStep(sparseNetlist, method, sparseFeatures);
        var dense = new ElectricalTimeStep(denseNetlist, method, denseFeatures);

        var report = sparse.linearSolverReport();
        long order = report.matrixOrder();
        assertEquals(LARGE + 2, order);
        assertEquals(Backend.SPARSE, report.backend(), "AUTO selects sparse at 300 nodes");
        assertEquals(Backend.SPARSE, report.companionBackend());
        assertEquals(Backend.SPARSE, report.initializationBackend(), "the DC solver is sparse too, not a hidden dense matrix");
        assertTrue(report.companionStoredCoefficients() == 0 || report.companionStoredCoefficients() < 6 * order);
        assertTrue(report.initializationStoredCoefficients() < 6 * order, "initialization stored "
                + report.initializationStoredCoefficients() + " for order " + order);
        assertEquals(Backend.DENSE, dense.linearSolverReport().backend());
        assertEquals(order * order, dense.linearSolverReport().storedCoefficients());

        // The DC seed is the 1 V steady state; stepping the source to 2 V afterwards makes real charging currents.
        sparseNetlist.getParameter(0)[1] = 2.0;
        denseNetlist.getParameter(0)[1] = 2.0;
        PhysicalTrace denseTrace = new PhysicalTrace();
        PhysicalTrace sparseTrace = new PhysicalTrace();
        for (int index = 0; index < 6; index++) {
            sparse.advanceInterval(grid, index * grid, null);
            dense.advanceInterval(grid, index * grid, null);
            denseTrace.add(index * grid, denseNetlist.getLastNodeVoltages(), denseNetlist.getLastComponentCurrents(), denseNetlist);
            sparseTrace.add(index * grid, sparseNetlist.getLastNodeVoltages(), sparseNetlist.getLastComponentCurrents(),
                    sparseNetlist);
        }
        PhysicalTrace.assertEquivalent(denseTrace, sparseTrace, method.toString(), TOLERANCE);
        var after = sparse.linearSolverReport();
        assertEquals(MatrixSolver.SparseFactorization.EJML, after.factorization(), "a large pinned system keeps EJML");
        assertEquals(MatrixSolver.SparseFactorization.EJML, after.companionFactorization());
        assertEquals(MatrixSolver.SparseFactorization.EJML, after.initializationFactorization());
        assertTrue(after.storedCoefficients() < 6 * order);
        assertTrue(after.companionStoredCoefficients() > 0, "the companion solver ran");
        assertTrue(after.companionStoredCoefficients() < 6 * order, "the companion stays sparse");
        assertTrue(after.storageBytes() < 800 * order);
        assertTrue(sparse.adaptiveReport().accepted() >= 6, sparse.adaptiveReport().toString());
        assertEquals(dense.adaptiveReport().accepted(), sparse.adaptiveReport().accepted(), "same adaptive decisions");
    }

    @Test
    void largeCircuitDialogInitializationStaysSparse() {
        var features = new SolverFeatures(0, false, false, null, null, new SparseSolverSettings(Mode.AUTO, 100));
        CircuitNetlist netlist = ladder();
        netlist.getParameter(2)[10] = 0; // keep the capacitor history term zero
        var step = new ElectricalTimeStep(netlist, SolverType.SOLVER_BE, features);
        var report = step.linearSolverReport();
        assertEquals(Backend.SPARSE, report.backend());
        if (report.initializationBackend() != null) assertEquals(Backend.SPARSE, report.initializationBackend());
        assertTrue(report.initializationStoredCoefficients() < 6L * report.matrixOrder());
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void controlledSourceLimitPatchesRefactorTheSparseSolverAndMatchDense(SolverType integration) {
        for (int mode : new int[] {5, 399}) {
            CircuitNetlist denseCircuit = limitCircuit(mode);
            CircuitNetlist sparseCircuit = limitCircuit(mode);
            var dense = new ElectricalTimeStep(denseCircuit, integration);
            var sparse = new ElectricalTimeStep(sparseCircuit, integration,
                    new SolverFeatures(0, false, false, null, null, FORCED));
            assertEquals(Backend.SPARSE, sparse.solverForTest().getBackend());
            double[] controls = {2, -2, 1, 2, -2};
            int factorizations = 0;
            PhysicalTrace denseTrace = new PhysicalTrace();
            PhysicalTrace sparseTrace = new PhysicalTrace();
            for (int index = 0; index < controls.length; index++) {
                denseCircuit.getParameter(0)[1] = controls[index];
                sparseCircuit.getParameter(0)[1] = controls[index];
                dense.advance(0.125, index * 0.125, null);
                sparse.advance(0.125, index * 0.125, null);
                denseTrace.add(index * 0.125, denseCircuit.getLastNodeVoltages(), denseCircuit.getLastComponentCurrents(),
                        denseCircuit);
                sparseTrace.add(index * 0.125, sparseCircuit.getLastNodeVoltages(), sparseCircuit.getLastComponentCurrents(),
                        sparseCircuit);
                factorizations = sparse.solverForTest().getFactorizationCount();
            }
            PhysicalTrace.assertEquivalent(denseTrace, sparseTrace, "limit mode " + mode + " " + integration, TOLERANCE);
            assertTrue(denseTrace.transitions() > 0, "the limit state must change");
            assertTrue(factorizations >= 4, "limit changes invalidate the factors: " + factorizations);
        }
    }

    private static CircuitNetlist limitCircuit(int mode) {
        double[][] parameters = new double[4][21];
        parameters[0][1] = 2;
        parameters[1][0] = mode;
        parameters[1][11] = 3;
        parameters[1][12] = -4;
        parameters[1][13] = 5;
        parameters[2][0] = 10;
        parameters[3][0] = 0.25;
        parameters[3][6] = 0.25;
        parameters[3][7] = 0.25;
        var circuit = new CircuitNetlist();
        circuit.initNetlist(new CircuitTypCore[] {CircuitTypCore.LK_U, CircuitTypCore.LK_U,
                CircuitTypCore.LK_R, CircuitTypCore.LK_C},
                new int[] {1, 2, 2, 2}, new int[] {0, 0, 0, 0}, new int[] {1, 2, -1, -1},
                parameters, 2, 2, 4);
        circuit.setControlElementIndex(1, 0);
        return circuit;
    }
}
