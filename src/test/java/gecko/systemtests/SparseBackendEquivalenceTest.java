package gecko.systemtests;

import gecko.GeckoExternal;
import gecko.GeckoSim;
import gecko.core.allg.SparseSolverSettings;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.preparation.PreparedCircuit;
import gecko.core.preparation.SolverFeatures;
import gecko.core.preparation.SolverOverrides;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.LinearSolverReport;
import gecko.core.simulation.solver.MatrixSolver;
import gecko.core.simulation.solver.MatrixSolver.Backend;
import gecko.geckocircuits.allg.MainWindow;
import gecko.geckocircuits.circuit.EditorCircuitCapture;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs each of the 21 education circuits through core twice, once on the dense LU and once with the sparse LU
 * forced, and compares the complete physical state of every electrical step: node potentials, component
 * currents and every element parameter, which carries switch and source-limit states. It lives in the
 * application module because the circuits are prepared from desktop editor captures; core stays GUI-free.
 * Needs a display; run with {@code xvfb-run -a mvn test -Pregression}.
 */
@Tag("regression")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SparseBackendEquivalenceTest {
    private static final SparseSolverSettings FORCED = new SparseSolverSettings(SparseSolverSettings.Mode.SPARSE);
    private double previousDuration;
    private double previousDt;

    /** The complete electrical trace of one run, kept as primitive vectors per step. */
    private record Run(List<Double> times, List<double[]> potentials, List<double[]> sourceCurrents,
            List<double[]> componentCurrents, List<double[]> switchStates, LinearSolverReport report) { }

    /** Conduction resistance of every switching element and limit state of every limited source, exactly as stored. */
    private static double[] switchStates(gecko.core.circuit.netlist.INetList netlist) {
        double[] states = new double[netlist.getElementCount()];
        int count = 0;
        for (int element = 0; element < states.length; element++) {
            var type = netlist.getType(element);
            double[] parameters = netlist.getParameter(element);
            switch (type) {
                case LK_D, LK_THYR, LK_IGBT, LK_S, LK_MOSFET -> states[count++] = parameters[0];
                case LK_U -> { if (parameters.length > 14) states[count++] = parameters[14]; }
                default -> { }
            }
        }
        return java.util.Arrays.copyOf(states, count);
    }

    @BeforeAll
    void startApplication() throws Exception {
        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[] {});
        Thread.sleep(2000);
        previousDuration = MainWindow._solverSettings._tDURATION.getValue();
        previousDt = MainWindow._solverSettings.dt.getValue();
    }

    @AfterAll
    void restoreSolverSettings() {
        MainWindow._solverSettings._tDURATION.setValueWithoutUndo(previousDuration);
        MainWindow._solverSettings.dt.setValueWithoutUndo(previousDt);
    }

    static java.util.stream.Stream<String> circuits() throws Exception {
        return RegressionTest.circuitFiles();
    }

    private PreparationResult.Ready prepare(String circuit, SolverFeatures features) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        Path source = root.resolve("src/test/resources/ipes/education/" + circuit + ".ipes");
        GeckoExternal.openFile(source.toString());
        Thread.sleep(200);
        double[] settings = GoldenReferenceHelper.readSimulationParams(
                root.resolve("src/test/resources/golden/" + circuit + ".golden.csv"));
        MainWindow._solverSettings._tDURATION.setValueWithoutUndo(settings[0]);
        MainWindow._solverSettings.dt.setValueWithoutUndo(settings[1]);
        var scripts = MainWindow._scripter;
        var snapshot = new EditorCircuitCapture().capture(MainWindow._se._circuitSheet.getAllElements(),
                MainWindow._solverSettings, MainWindow.optimizerParameterData, MainWindow._fileManager,
                new EditorCircuitCapture.ScriptSources(scripts.getScriptCode(), scripts.getImportCode(),
                        scripts.getDeclarationCode(), scripts.getExtraFilesHashes()), source.toString());
        var result = new CircuitPreparation().prepare(snapshot,
                new PreparationRequest(new SolverOverrides(null, null, null, features), Map.of()));
        return assertInstanceOf(PreparationResult.Ready.class, result,
                () -> circuit + " is not core-ready: " + result.report().diagnostics());
    }

    private Run run(PreparedCircuit prepared, String circuit) {
        List<Double> times = new ArrayList<>();
        List<double[]> potentials = new ArrayList<>();
        List<double[]> sourceCurrents = new ArrayList<>();
        List<double[]> componentCurrents = new ArrayList<>();
        List<double[]> switches = new ArrayList<>();
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, volts, currents, netlist) -> {
            int nodes = netlist.getNodeMax() + 1;
            times.add(time);
            potentials.add(java.util.Arrays.copyOfRange(volts, 0, nodes));
            sourceCurrents.add(java.util.Arrays.copyOfRange(volts, nodes, volts.length));
            componentCurrents.add(currents.clone());
            switches.add(switchStates(netlist));
        });
        var result = engine.runSimulation(prepared, ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), circuit + ": " + result.getErrorMessage());
        return new Run(times, potentials, sourceCurrents, componentCurrents, switches,
                (LinearSolverReport) result.getMetadata().get("linearSolver"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("circuits")
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void forcedSparseReproducesTheDensePhysicalTrace(String circuit) throws Exception {
        Run dense = run(prepare(circuit, SolverFeatures.none()).execution(), circuit);
        Run sparse = run(prepare(circuit, new SolverFeatures(0, false, false, null, null, FORCED)).execution(), circuit);

        assertNull(dense.report(), "the default run reports no solver choice");
        assertNotNull(sparse.report(), "a forced run reports the backend it used");
        assertEquals(Backend.SPARSE, sparse.report().backend(), circuit + " must really run on the sparse LU");
        assertTrue(sparse.report().storedCoefficients() < (long) sparse.report().matrixOrder() * sparse.report().matrixOrder());
        assertEquals(MatrixSolver.SparseFactorization.LEGACY_ORDER, sparse.report().factorization(),
                circuit + " is a small wired capture, so its sparse LU repeats the dense arithmetic");
        if (sparse.report().initializationBackend() != null) {
            assertEquals(MatrixSolver.SparseFactorization.LEGACY_ORDER, sparse.report().initializationFactorization());
        }
        assertEquals(dense.times().size(), sparse.times().size(), circuit + " step count");
        assertFalse(dense.times().isEmpty(), circuit);
        assertEquals(dense.times(), sparse.times(), circuit + " the electrical time grid is exactly the same");
        int channels = dense.potentials().get(0).length;
        var potentialNorm = TraceEquivalence.norm(dense.potentials(), sparse.potentials(), 0, channels);
        System.out.printf("SPARSE-MAXDIFF %s potentials=%.3e sourceCurrents=%.3e componentCurrents=%.3e (relative, complete trace)%n",
                circuit, potentialNorm.relative(),
                TraceEquivalence.norm(dense.sourceCurrents(), sparse.sourceCurrents(), 0, Integer.MAX_VALUE).relative(),
                TraceEquivalence.norm(dense.componentCurrents(), sparse.componentCurrents(), 0, Integer.MAX_VALUE).relative());
        System.out.printf("SPARSE-DIVERGENCE %s relative=%.3e firstStepAbove1e-12=%d firstStepAbove1e-9=%d of %d%n", circuit,
                potentialNorm.relative(), firstStepAbove(dense.potentials(), sparse.potentials(), potentialNorm.scale() * 1e-12),
                firstStepAbove(dense.potentials(), sparse.potentials(), potentialNorm.scale() * 1e-9), dense.times().size());
        double blockTolerance = TraceEquivalence.TOLERANCE;
        TraceEquivalence.assertWithin(circuit + " node potentials [V]",
                TraceEquivalence.norm(dense.potentials(), sparse.potentials(), 0, channels), blockTolerance);
        TraceEquivalence.assertWithin(circuit + " source branch currents [A]",
                TraceEquivalence.norm(dense.sourceCurrents(), sparse.sourceCurrents(), 0, Integer.MAX_VALUE),
                blockTolerance);
        TraceEquivalence.assertWithin(circuit + " component currents [A]",
                TraceEquivalence.norm(dense.componentCurrents(), sparse.componentCurrents(), 0, Integer.MAX_VALUE),
                blockTolerance);
        int transitions = 0;
        for (int i = 0; i < dense.times().size(); i++) {
            assertArrayEquals(dense.switchStates().get(i), sparse.switchStates().get(i),
                    circuit + " switch and limit states must agree exactly, step " + i);
            if (i > 0 && !java.util.Arrays.equals(dense.switchStates().get(i - 1), dense.switchStates().get(i))) transitions++;
        }
        System.out.printf("SPARSE-21 %s steps=%d order=%d stored=%d switchTransitions=%d%n",
                circuit, dense.times().size(), sparse.report().matrixOrder(), sparse.report().storedCoefficients(),
                transitions);
    }

    /** @return the first step whose largest potential difference exceeds the bound, or -1 */
    private static int firstStepAbove(List<double[]> expected, List<double[]> actual, double bound) {
        for (int step = 0; step < expected.size(); step++) {
            for (int i = 0; i < expected.get(step).length; i++) {
                if (Math.abs(expected.get(step)[i] - actual.get(step)[i]) > bound) return step;
            }
        }
        return -1;
    }
}
