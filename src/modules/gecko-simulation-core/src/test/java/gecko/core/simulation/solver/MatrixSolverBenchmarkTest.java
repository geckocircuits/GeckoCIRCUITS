package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import gecko.core.allg.SparseSolverSettings;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.testutil.SyntheticCircuitBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Times {@link MatrixSolver} on RC ladders of 10, 100 and 1,000 nodes. Excluded from default runs;
 * run with {@code mvn -f pom-reactor.xml test -pl src/modules/gecko-simulation-core -Dgroups=benchmark} (no -Dtest needed).
 *
 * <p>Fixture: DC source 1 V at node 1, R = 1 kOhm series, C = 1 uF shunt, Backward Euler, dt = 10 us.
 * Every figure is a median over the repetitions after the warmup. Output lines start with {@code BENCH}.
 * The factor column changes one conductance by the same small amount on both backends before each solve, so every
 * measured request really factors; the test asserts the factorization counts. A sparse solver keeps its factors when a
 * rebuild leaves the coefficients unchanged, which the transient column shows. The only timing assertion is sparse
 * against dense on the 1,000-node transient. The fixture is an RC ladder and says nothing about other circuits.
 */
@Tag("benchmark")
class MatrixSolverBenchmarkTest {
    static final double R = 1000.0;
    static final double C = 1e-6;
    static final double DT = 1e-5;
    private static final int[] SIZES = {10, 100, 1000};
    // Per size: warmup and measured repetitions for {assemble, factor+solve, cached solve, transient run}.
    private static final int[][] WARMUP = {{3000, 3000, 20000, 20}, {300, 100, 2000, 5}, {5, 3, 100, 2}};
    private static final int[][] REPS = {{10000, 10000, 50000, 41}, {1000, 300, 5000, 21}, {15, 7, 300, 5}};
    /** Conductance added and removed alternately, small against the ladder's 1e-3 to 1e-1 S coefficients. */
    private static final double PATCH = 1e-6;
    private static final int[] TRANSIENT_STEPS = {2000, 500, 20};

    /** One timed result set for one solver variant and size. */
    record Result(String variant, int nodes, int order, double assembleUs, double factorSolveUs,
            double cachedSolveUs, int steps, double transientMs, double[] finalPotentials,
            long storedCoefficients, long factorNonzeros, long storageBytes, int factorRequests, int factorizations,
            int transientFactorizations, MatrixSolver.SparseFactorization factorization) { }

    @Test
    void benchmarkRcLadders() {
        System.out.println("BENCH fixture: RC ladder, V=1, R=" + R + ", C=" + C + ", dt=" + DT + ", solver=BE, java="
                + System.getProperty("java.version") + ", cores=" + Runtime.getRuntime().availableProcessors());
        for (int s = 0; s < SIZES.length; s++) {
            Result dense = measure("dense-default", SIZES[s], s, () -> new MatrixSolver(SolverType.SOLVER_BE));
            print(dense, s);
            Result sparse = measure("sparse-forced", SIZES[s], s, () -> new MatrixSolver(SolverType.SOLVER_BE,
                    new SparseSolverSettings(SparseSolverSettings.Mode.SPARSE)));
            print(sparse, s);
            var norms = SparseMatrixSolverTest.assertUnits("benchmarked solvers, " + SIZES[s] + " nodes",
                    dense.finalPotentials, sparse.finalPotentials, SIZES[s]);
            System.out.printf("BENCH check nodes=%d potentials_relative=%.3e source_current_relative=%.3e%n", SIZES[s],
                    norms[0].relative(), norms[1].relative());
            if (SIZES[s] == 1000) {
                // Timing claim of the issue, checked only here, under the benchmark tag.
                assertTrue(sparse.transientMs < dense.transientMs, "sparse " + sparse.transientMs + " ms vs dense "
                        + dense.transientMs + " ms over " + dense.steps + " steps");
            }
        }
    }

    static void print(Result r, int sizeIndex) {
        System.out.printf("BENCH variant=%s nodes=%d order=%d warmup(assemble/factor/cached/transient)=%s reps=%s"
                + " assemble_us=%.2f factor_solve_us=%.2f cached_solve_us=%.2f transient_steps=%d"
                + " transient_ms=%.3f transient_us_per_step=%.2f stored_coefficients=%d factor_nonzeros=%d"
                + " numeric_storage_estimate_bytes=%d factor_requests=%d factorizations_in_factor_column=%d"
                + " factorizations_in_transient=%d factorization=%s%n",
                r.variant, r.nodes, r.order, Arrays.toString(WARMUP[sizeIndex]), Arrays.toString(REPS[sizeIndex]),
                r.assembleUs, r.factorSolveUs, r.cachedSolveUs, r.steps, r.transientMs,
                r.transientMs * 1000 / r.steps, r.storedCoefficients, r.factorNonzeros, r.storageBytes,
                r.factorRequests, r.factorizations, r.transientFactorizations,
                r.factorization == null ? "dense LU" : r.factorization);
    }

    static Result measure(String variant, int nodes, int sizeIndex, Supplier<MatrixSolver> factory) {
        CircuitNetlist netlist = SyntheticCircuitBuilder.rcLadder(nodes, 1.0, R, C);
        MatrixSolver solver = factory.get();
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());

        double assemble = median(WARMUP[sizeIndex][0], REPS[sizeIndex][0], () -> solver.buildMatrixA(netlist, DT, 0, false));

        solver.buildMatrixA(netlist, DT, 0, false);
        solver.buildVectorB(netlist, DT, 0, false);
        int[] toggle = {0};
        int factorRequests = WARMUP[sizeIndex][1] + REPS[sizeIndex][1];
        int factorsBefore = solver.getFactorizationCount();
        double factor = median(WARMUP[sizeIndex][1], REPS[sizeIndex][1], () -> {
            solver.addCoefficient(1, 1, (toggle[0]++ & 1) == 0 ? PATCH : -PATCH);
            solver.setMatrixChanged();
            solver.solve();
        });
        int factorizations = solver.getFactorizationCount() - factorsBefore;
        assertEquals(factorRequests, factorizations, variant + " must factor on every measured request");

        int cachedBefore = solver.getFactorizationCount();
        double cached = median(WARMUP[sizeIndex][2], REPS[sizeIndex][2], solver::solve);
        assertEquals(cachedBefore, solver.getFactorizationCount(), variant + " cached solves must reuse the factors");

        int steps = TRANSIENT_STEPS[sizeIndex];
        double[] finalPotentials = null;
        int transientFactorizations = 0;
        double[] runs = new double[WARMUP[sizeIndex][3] + REPS[sizeIndex][3]];
        for (int i = 0; i < runs.length; i++) {
            MatrixSolver fresh = factory.get();
            fresh.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());
            long start = System.nanoTime();
            for (int step = 0; step < steps; step++) {
                fresh.buildMatrixA(netlist, DT, step * DT, false);
                fresh.buildVectorB(netlist, DT, step * DT, false);
                fresh.solve();
                fresh.shiftHistory();
            }
            runs[i] = (System.nanoTime() - start) / 1e6;
            finalPotentials = fresh.getP().clone();
            transientFactorizations = fresh.getFactorizationCount();
        }
        double transientMs = medianOf(Arrays.copyOfRange(runs, WARMUP[sizeIndex][3], runs.length));
        return new Result(variant, nodes, solver.getMatrixSize(), assemble, factor, cached, steps, transientMs,
                finalPotentials, solver.getStoredCoefficientCount(), solver.getFactorNonzeroCount(),
                solver.getCoefficientStorageBytes(), factorRequests, factorizations, transientFactorizations,
                solver.getSparseFactorization());
    }

    private static double median(int warmup, int reps, Runnable action) {
        for (int i = 0; i < warmup; i++) action.run();
        double[] us = new double[reps];
        for (int i = 0; i < reps; i++) {
            long start = System.nanoTime();
            action.run();
            us[i] = (System.nanoTime() - start) / 1e3;
        }
        return medianOf(us);
    }

    private static double medianOf(double[] values) {
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }
}
