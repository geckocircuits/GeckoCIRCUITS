package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import gecko.core.allg.SparseSolverSettings;
import gecko.core.allg.SparseSolverSettings.Mode;
import gecko.core.circuit.matrix.ResistorStamper;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.simulation.solver.MatrixSolver.Backend;
import gecko.core.testutil.SyntheticCircuitBuilder;
import gecko.core.testutil.TraceEquivalence;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Backend selection, dense/sparse equivalence, factor reuse, storage growth and failure handling of the sparse LU. */
class SparseMatrixSolverTest {
    private static final double R = 1000.0;
    private static final double C = 1e-6;
    private static final double DT = 1e-5;
    private static final double TOLERANCE = 1e-9;
    private static final SparseSolverSettings AUTO = new SparseSolverSettings();

    private static CircuitNetlist ladder(int nodes) {
        return SyntheticCircuitBuilder.rcLadder(nodes, 1.0, R, C);
    }

    private static MatrixSolver solver(CircuitNetlist netlist, SparseSolverSettings settings) {
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE, settings);
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());
        return solver;
    }

    private static void step(MatrixSolver solver, CircuitNetlist netlist, double time) {
        solver.buildMatrixA(netlist, DT, time, false);
        solver.buildVectorB(netlist, DT, time, false);
        solver.solve();
        solver.shiftHistory();
    }

    /**
     * Compares one solution vector in its two physical units, each as a max norm against its own scale: the node
     * potentials at indices 0 to {@code nodes} in volts, then the source branch currents in amperes. A block whose
     * expected values are all numerically zero uses the absolute bound of {@link TraceEquivalence}.
     *
     * @return the potential norm and the current norm
     */
    static TraceEquivalence.Norm[] assertUnits(String label, double[] expected, double[] actual, int nodes) {
        TraceEquivalence.Norm volts = TraceEquivalence.norm(List.of(expected), List.of(actual), 0, nodes + 1);
        TraceEquivalence.Norm amperes = TraceEquivalence.norm(List.of(expected), List.of(actual), nodes + 1, expected.length);
        TraceEquivalence.assertWithin(label + " node potentials [V]", volts, TOLERANCE);
        TraceEquivalence.assertWithin(label + " source currents [A]", amperes, TOLERANCE);
        return new TraceEquivalence.Norm[] {volts, amperes};
    }

    @Test
    void aCurrentErrorCannotHideBehindTheVoltageScale() {
        // 1 V and 1 mA with the current off by 5e-7 relative: a mixed max norm sees 5e-10 of 1 V and would pass.
        double[] expected = {1.0, 1e-3};
        double[] actual = {1.0, 1.0000005e-3};
        assertThrows(AssertionError.class, () -> assertUnits("mixed", expected, actual, 0));
        assertUnits("exact", expected, expected.clone(), 0);
        assertThrows(AssertionError.class, () -> assertUnits("voltage", expected, new double[] {1.000001, 1e-3}, 0));
    }

    @Test
    void thePhysicalNodeCountBoundaryIs99DenseAnd100Sparse() {
        assertEquals(100, SparseSolverSettings.DEFAULT_THRESHOLD_NODES);
        CircuitNetlist below = ladder(99);
        CircuitNetlist at = ladder(100);
        assertEquals(99, below.getNodeMax(), "node count excludes ground and source unknowns");
        assertEquals(Backend.DENSE, solver(below, AUTO).getBackend());
        assertEquals(Backend.SPARSE, solver(at, AUTO).getBackend());
        assertEquals(Backend.DENSE, solver(ladder(5), new SparseSolverSettings(Mode.AUTO, 6)).getBackend());
        assertEquals(Backend.SPARSE, solver(ladder(6), new SparseSolverSettings(Mode.AUTO, 6)).getBackend());
    }

    @Test
    void theExplicitModesOverrideTheThreshold() {
        assertEquals(Backend.SPARSE, solver(ladder(10), new SparseSolverSettings(Mode.SPARSE)).getBackend());
        assertEquals(Backend.DENSE, solver(ladder(300), new SparseSolverSettings(Mode.DENSE)).getBackend());
        assertEquals(Backend.DENSE, solver(ladder(300), null).getBackend(), "no settings keeps the dense default");
        MatrixSolver plain = new MatrixSolver(SolverType.SOLVER_BE);
        plain.initializeMatrices(300, 1, 10);
        assertEquals(Backend.DENSE, plain.getBackend());
        assertNull(plain.getSparseSettings());
        assertEquals(Mode.SPARSE, solver(ladder(10), new SparseSolverSettings(Mode.SPARSE)).getSparseSettings().mode());
    }

    @Test
    void settingsRejectInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new SparseSolverSettings(null, 100));
        assertThrows(IllegalArgumentException.class, () -> new SparseSolverSettings(Mode.AUTO, 0));
        assertThrows(IllegalArgumentException.class, () -> new SparseSolverSettings(Mode.AUTO, -5));
        assertEquals(Mode.AUTO, new SparseSolverSettings().mode());
        assertEquals(new SparseSolverSettings(Mode.SPARSE, 100), new SparseSolverSettings(Mode.SPARSE));
    }

    @Test
    void sparseMatchesDenseOnLaddersOverATransient() {
        for (int nodes : new int[] {10, 99, 100, 250}) {
            CircuitNetlist netlist = ladder(nodes);
            MatrixSolver dense = solver(netlist, new SparseSolverSettings(Mode.DENSE));
            MatrixSolver sparse = solver(netlist, new SparseSolverSettings(Mode.SPARSE));
            assertEquals(Backend.DENSE, dense.getBackend());
            assertEquals(Backend.SPARSE, sparse.getBackend());
            for (int stepIndex = 0; stepIndex < 25; stepIndex++) {
                step(dense, netlist, stepIndex * DT);
                step(sparse, netlist, stepIndex * DT);
                assertUnits(nodes + " nodes, step " + stepIndex, dense.getP(), sparse.getP(), nodes);
            }
        }
    }

    @Test
    void sparseMatchesDenseAtOneThousandNodes() {
        CircuitNetlist netlist = ladder(1000);
        MatrixSolver dense = solver(netlist, new SparseSolverSettings(Mode.DENSE));
        MatrixSolver sparse = solver(netlist, AUTO);
        assertEquals(Backend.SPARSE, sparse.getBackend());
        for (int stepIndex = 0; stepIndex < 3; stepIndex++) {
            step(dense, netlist, stepIndex * DT);
            step(sparse, netlist, stepIndex * DT);
        }
        TraceEquivalence.Norm[] norms = assertUnits("1000 nodes", dense.getP(), sparse.getP(), 1000);
        System.out.printf("SPARSE-LADDER1000 potentials=%.3e sourceCurrent=%.3e (relative, per unit)%n",
                norms[0].relative(), norms[1].relative());
    }

    @Test
    void theWireReferenceReductionAlsoMatchesDense() {
        CircuitNetlist netlist = ladder(120);
        netlist.setWireReference(true);
        MatrixSolver dense = solver(netlist, new SparseSolverSettings(Mode.DENSE));
        MatrixSolver sparse = solver(netlist, new SparseSolverSettings(Mode.SPARSE));
        for (int stepIndex = 0; stepIndex < 5; stepIndex++) {
            step(dense, netlist, stepIndex * DT);
            step(sparse, netlist, stepIndex * DT);
        }
        assertEquals(0.0, sparse.getP()[0]);
        assertUnits("wired 120 nodes", dense.getP(), sparse.getP(), 120);
    }

    @Test
    void theFactorizationIsReusedUntilACoefficientChanges() {
        CircuitNetlist netlist = ladder(150);
        MatrixSolver sparse = solver(netlist, AUTO);
        sparse.buildMatrixA(netlist, DT, 0, false);
        sparse.buildVectorB(netlist, DT, 0, false);
        sparse.solve();
        assertEquals(1, sparse.getFactorizationCount());
        sparse.solve();
        sparse.solve();
        assertEquals(1, sparse.getFactorizationCount(), "cached solves keep the factors");

        sparse.buildMatrixA(netlist, DT, DT, false);
        sparse.buildVectorB(netlist, DT, DT, false);
        sparse.solve();
        assertEquals(1, sparse.getFactorizationCount(), "a rebuild with identical coefficients keeps the factors");

        sparse.buildMatrixA(netlist, DT / 2, DT, false);
        sparse.buildVectorB(netlist, DT / 2, DT, false);
        sparse.solve();
        assertEquals(2, sparse.getFactorizationCount(), "a new step width changes the capacitor conductance");

        sparse.addCoefficient(3, 3, 0.5);
        sparse.setMatrixChanged();
        sparse.solve();
        assertEquals(3, sparse.getFactorizationCount(), "a patch such as a diode state change refactors");
        sparse.addCoefficient(3, 3, -0.5);
        sparse.setMatrixChanged();
        sparse.solve();
        assertEquals(4, sparse.getFactorizationCount(), "undoing a patch refactors to the original numbers");

        sparse.setMatrixChanged();
        sparse.solve();
        assertEquals(4, sparse.getFactorizationCount(), "an unchanged flagged matrix reuses the factors");

        sparse.addCoefficient(4, 7, 2.0); // a structurally new coefficient, as a source-limit patch adds
        sparse.setMatrixChanged();
        sparse.solve();
        assertEquals(5, sparse.getFactorizationCount());
        assertEquals(2.0, sparse.getSystemMatrix()[4][7]);
    }

    @Test
    void theDenseDefaultRefactorsOnEveryRebuildAsBefore() {
        CircuitNetlist netlist = ladder(20);
        MatrixSolver dense = solver(netlist, null);
        for (int i = 0; i < 3; i++) step(dense, netlist, i * DT);
        assertEquals(3, dense.getFactorizationCount());
        dense.solve();
        assertEquals(3, dense.getFactorizationCount());
        assertSame(dense.getSystemMatrix(), dense.getSystemMatrix(), "dense access stays the live array");
        double before = dense.getSystemMatrix()[2][2];
        dense.addCoefficient(2, 2, 1.5);
        assertEquals(before + 1.5, dense.getSystemMatrix()[2][2]);
        assertEquals(22, dense.getMatrixSize(), "order is nodes + 1 source + 1");
        assertEquals((long) dense.getMatrixSize() * dense.getMatrixSize(), dense.getStoredCoefficientCount());
        assertEquals(8L * dense.getMatrixSize() * dense.getMatrixSize(), dense.getCoefficientStorageBytes());
    }

    @Test
    void theSparseSystemMatrixGetterReturnsADetachedSnapshot() {
        CircuitNetlist netlist = ladder(30);
        MatrixSolver sparse = solver(netlist, new SparseSolverSettings(Mode.SPARSE));
        sparse.buildMatrixA(netlist, DT, 0, false);
        double[][] first = sparse.getSystemMatrix();
        double[][] second = sparse.getSystemMatrix();
        assertNotSame(first, second);
        assertEquals(sparse.getMatrixSize(), first.length);
        assertArrayEquals(first[5], second[5]);
        double kept = first[5][5];
        first[5][5] = 12345.0;
        assertEquals(kept, sparse.getSystemMatrix()[5][5], "writes to the snapshot do not reach the solver");
        MatrixSolver dense = solver(netlist, new SparseSolverSettings(Mode.DENSE));
        dense.buildMatrixA(netlist, DT, 0, false);
        for (int row = 0; row < first.length; row++) {
            assertArrayEquals(dense.getSystemMatrix()[row], sparse.getSystemMatrix()[row], 0.0,
                    "the sparse coefficients equal the dense matrix, row " + row);
        }
    }

    @Test
    void storedCoefficientsAndBytesGrowLinearlyWithTheNonzeroCount() {
        long[] bytes = new long[5];
        long[] factors = new long[5];
        int[] sizes = {100, 200, 400, 800, 1600};
        for (int i = 0; i < sizes.length; i++) {
            CircuitNetlist netlist = ladder(sizes[i]);
            MatrixSolver sparse = solver(netlist, AUTO);
            step(sparse, netlist, 0);
            long nonzero = sparse.getNonzeroCoefficientCount();
            // Tridiagonal node block (3n - 2) plus ground row and column (2n) and the source row and column.
            assertTrue(nonzero >= 3L * sizes[i] - 2 && nonzero <= 5L * sizes[i] + 6, sizes[i] + " nodes: " + nonzero);
            assertEquals(nonzero, sparse.getStoredCoefficientCount(), "no slots beyond the nonzero coefficients");
            // Eliminating the ground row last keeps the factors from filling in.
            assertTrue(sparse.getFactorNonzeroCount() <= 9L * sizes[i] + 8, sizes[i] + " nodes: factor nonzeros "
                    + sparse.getFactorNonzeroCount());
            factors[i] = sparse.getFactorNonzeroCount();
            bytes[i] = sparse.getCoefficientStorageBytes();
            assertTrue(bytes[i] < 800L * sizes[i], sizes[i] + " nodes use " + bytes[i] + " bytes");
        }
        // Doubling the circuit must roughly double the bytes (dense would quadruple them).
        for (int i = 1; i < bytes.length; i++) {
            double growth = (double) bytes[i] / bytes[i - 1];
            assertTrue(growth > 1.2 && growth < 3.0, "doubling nodes grew bytes by " + growth);
            assertTrue((double) factors[i] / factors[i - 1] < 2.2, "factor nonzeros " + java.util.Arrays.toString(factors));
        }
        CircuitNetlist netlist = ladder(1000);
        MatrixSolver dense = solver(netlist, new SparseSolverSettings(Mode.DENSE));
        MatrixSolver sparse = solver(netlist, new SparseSolverSettings(Mode.SPARSE));
        step(sparse, netlist, 0);
        assertTrue(dense.getCoefficientStorageBytes() > 10 * sparse.getCoefficientStorageBytes(),
                "numeric-storage estimate (primitive arrays, no JVM overhead, not total heap): dense "
                        + dense.getCoefficientStorageBytes() + " bytes, sparse " + sparse.getCoefficientStorageBytes());
    }

    @Test
    void aVeryLargeLadderRunsWithoutAnyOrderSquaredArray() {
        // A dense matrix of this order needs 3.2 GB, more than the 2 GB test heap.
        int nodes = 20_000;
        CircuitNetlist netlist = ladder(nodes);
        MatrixSolver sparse = solver(netlist, AUTO);
        for (int i = 0; i < 3; i++) step(sparse, netlist, i * DT);
        assertEquals(Backend.SPARSE, sparse.getBackend());
        assertTrue(sparse.getStoredCoefficientCount() < 6L * nodes);
        assertTrue(sparse.getFactorNonzeroCount() < 9L * nodes, "factor nonzeros " + sparse.getFactorNonzeroCount());
        assertTrue(sparse.getCoefficientStorageBytes() < 800L * nodes);
        assertEquals(1.0, sparse.getP()[1], 1e-9, "the source node sits at the 1 V input");
        assertTrue(sparse.getP()[2] > 0, "the first capacitor has started to charge");
    }

    @Test
    void aSingularMatrixIsReportedAndNeverFallsBackToDense() {
        for (Mode mode : new Mode[] {Mode.SPARSE, Mode.DENSE}) {
            MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE, new SparseSolverSettings(mode));
            solver.initializeMatrices(3, 0, 0);
            solver.addCoefficient(0, 0, 1.0);
            solver.addCoefficient(1, 1, 1.0); // rows 2 and 3 stay empty
            solver.setMatrixChanged();
            RuntimeException failure = assertThrows(RuntimeException.class, solver::solve, mode.name());
            assertTrue(failure.getMessage().contains("singular"), failure.getMessage());
            assertEquals(mode == Mode.SPARSE ? Backend.SPARSE : Backend.DENSE, solver.getBackend());
        }
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE, new SparseSolverSettings(Mode.SPARSE));
        solver.initializeMatrices(3, 0, 0);
        solver.addCoefficient(0, 0, 1.0);
        solver.setMatrixChanged();
        assertThrows(RuntimeException.class, solver::solve);
        assertThrows(RuntimeException.class, solver::solve, "a failed factorization is retried, not cached");
        for (int i = 0; i < 4; i++) solver.addCoefficient(i, i, 1.0);
        solver.setMatrixChanged();
        solver.solve();
        assertEquals(Backend.SPARSE, solver.getBackend());
    }

    @Test
    void nonfiniteValuesAreRejectedLikeTheDensePath() {
        for (Mode mode : new Mode[] {Mode.SPARSE, Mode.DENSE}) {
            MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE, new SparseSolverSettings(mode));
            solver.initializeMatrices(2, 0, 0);
            for (int i = 0; i < 3; i++) solver.addCoefficient(i, i, 1.0);
            solver.addCoefficient(1, 2, Double.NaN);
            solver.setMatrixChanged();
            IllegalStateException matrix = assertThrows(IllegalStateException.class, solver::solve);
            assertEquals("Nonfinite system matrix at row 1", matrix.getMessage(), mode.name());
            solver.initializeMatrices(2, 0, 0);
            for (int i = 0; i < 3; i++) solver.addCoefficient(i, i, 1.0);
            solver.getb()[2] = Double.POSITIVE_INFINITY;
            IllegalStateException rhs = assertThrows(IllegalStateException.class, solver::solve);
            assertEquals("Nonfinite right-hand side at row 2", rhs.getMessage(), mode.name());
        }
    }

    @Test
    void solveRequiresInitialization() {
        assertThrows(IllegalStateException.class,
                () -> new MatrixSolver(SolverType.SOLVER_BE, new SparseSolverSettings(Mode.SPARSE)).solve());
    }

    @Test
    void coefficientsOutsideTheOrderAreRejected() {
        MatrixSolver sparse = new MatrixSolver(SolverType.SOLVER_BE, new SparseSolverSettings(Mode.SPARSE));
        sparse.initializeMatrices(2, 0, 0);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> sparse.addCoefficient(3, 0, 1.0));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> sparse.addCoefficient(0, -1, 1.0));
        sparse.addCoefficient(0, 1, 0.0);
        assertEquals(0, sparse.getStoredCoefficientCount(), "a zero contribution creates no slot");
    }

    @Test
    void stampersWriteTheSameCoefficientsThroughEitherSeam() {
        double[][] direct = new double[3][3];
        new ResistorStamper().stampMatrixA(direct, 1, 2, -1, new double[] {50.0}, DT, SolverType.SOLVER_BE);
        MatrixSolver sparse = new MatrixSolver(SolverType.SOLVER_BE, new SparseSolverSettings(Mode.SPARSE));
        sparse.initializeMatrices(2, 0, 0);
        new ResistorStamper().stampMatrixA(sparse::addCoefficient, 1, 2, -1, new double[] {50.0}, DT, SolverType.SOLVER_BE);
        for (int row = 0; row < 3; row++) assertArrayEquals(direct[row], sparse.getSystemMatrix()[row], 0.0);
    }

    @Test
    void repeatedClearingKeepsStorageBounded() {
        CircuitNetlist netlist = ladder(200);
        MatrixSolver sparse = solver(netlist, AUTO);
        step(sparse, netlist, 0);
        long stored = sparse.getStoredCoefficientCount();
        long bytes = sparse.getCoefficientStorageBytes();
        for (int i = 1; i < 50; i++) step(sparse, netlist, i * DT);
        assertEquals(stored, sparse.getStoredCoefficientCount());
        assertEquals(bytes, sparse.getCoefficientStorageBytes());
    }

    /** Solves a 2x2 system on the given backend and returns the potentials. */
    private static double[] solve2(Mode mode, double[][] a, double[] b) {
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE, new SparseSolverSettings(mode));
        solver.initializeMatrices(1, 0, 0);
        for (int r = 0; r < 2; r++) for (int c = 0; c < 2; c++) solver.addCoefficient(r, c, a[r][c]);
        System.arraycopy(b, 0, solver.getb(), 0, 2);
        solver.setMatrixChanged();
        solver.solve();
        return solver.getP().clone();
    }

    private static double normalizedResidual(double[][] a, double[] b, double[] x) {
        double residual = 0;
        double scale = 0;
        for (int r = 0; r < 2; r++) {
            double sum = -b[r];
            for (int c = 0; c < 2; c++) {
                sum += a[r][c] * x[c];
                scale = Math.max(scale, Math.abs(a[r][c] * x[c]));
            }
            scale = Math.max(scale, Math.abs(b[r]));
            residual = Math.max(residual, Math.abs(sum));
        }
        return residual / scale;
    }

    @Test
    void thePinnedFamilySolvesTheSameEquationsAsDense() {
        for (double s : new double[] {1, 1e-3, 1e6, 1e15}) {
            double[][] a = {{s, s}, {s, 2 * s}};
            double[] b = {0, s};
            for (Mode mode : new Mode[] {Mode.SPARSE, Mode.DENSE}) {
                double[] x = solve2(mode, a, b);
                assertEquals(-1.0, x[0], 1e-9, mode + " s=" + s);
                assertEquals(1.0, x[1], 1e-9, mode + " s=" + s);
                assertTrue(normalizedResidual(a, b, x) < 1e-9, mode + " residual, s=" + s);
            }
        }
    }

    @Test
    void aSingularGroundedSubblockStillSolvesTheFullSystem() {
        double[][] a = {{2, 1}, {1, 0}};
        double[] b = {1, 2};
        for (Mode mode : new Mode[] {Mode.SPARSE, Mode.DENSE}) {
            double[] x = solve2(mode, a, b);
            assertEquals(2.0, x[0], 1e-12, mode.name());
            assertEquals(-3.0, x[1], 1e-12, mode.name());
        }
        double[][] scaled = {{2e12, 1e12}, {1e12, 0}};
        double[] x = solve2(Mode.SPARSE, scaled, new double[] {1e12, 2e12});
        assertEquals(2.0, x[0], 1e-9);
        assertEquals(-3.0, x[1], 1e-9);
    }

    @Test
    void aRandomFullRankSystemMatchesTheAnalyticSolution() {
        java.util.Random random = new java.util.Random(7);
        for (int trial = 0; trial < 50; trial++) {
            double[][] a = {{random.nextGaussian(), random.nextGaussian()}, {random.nextGaussian(), random.nextGaussian()}};
            double[] b = {random.nextGaussian(), random.nextGaussian()};
            double det = a[0][0] * a[1][1] - a[0][1] * a[1][0];
            if (Math.abs(det) < 0.1) continue;
            double[] x = solve2(Mode.SPARSE, a, b);
            assertEquals((b[0] * a[1][1] - a[0][1] * b[1]) / det, x[0], 1e-9);
            assertEquals((a[0][0] * b[1] - a[1][0] * b[0]) / det, x[1], 1e-9);
        }
    }

    @Test
    void theWireReferenceModeExcludesRowAndColumnZeroOnlyWhenRequested() {
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE, new SparseSolverSettings(Mode.SPARSE));
        MatrixSolver dense = new MatrixSolver(SolverType.SOLVER_BE, new SparseSolverSettings(Mode.DENSE));
        CircuitNetlist netlist = ladder(2);
        netlist.setWireReference(true);
        for (MatrixSolver each : new MatrixSolver[] {solver, dense}) {
            each.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());
            each.buildMatrixA(netlist, DT, 0, false);
            each.buildVectorB(netlist, DT, 0, false);
            each.solve();
        }
        assertEquals(0.0, solver.getP()[0]);
        assertUnits("wired 2 nodes", dense.getP(), solver.getP(), 2);
        // Switching reference mode on the same solver must refactor rather than reuse the other mode's factors.
        netlist.setWireReference(false);
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());
        solver.buildMatrixA(netlist, DT, 0, false);
        solver.buildVectorB(netlist, DT, 0, false);
        solver.solve();
        assertEquals(1, solver.getFactorizationCount());
    }

    private static CircuitNetlist wired(int nodes) {
        CircuitNetlist netlist = ladder(nodes);
        netlist.setWireReference(true);
        return netlist;
    }

    @Test
    void forcedSparseBelowTheThresholdOnAWiredCircuitRepeatsTheDenseBitsAndReportsLegacyOrder() {
        CircuitNetlist netlist = wired(40);
        MatrixSolver dense = solver(netlist, new SparseSolverSettings(Mode.DENSE));
        MatrixSolver sparse = solver(netlist, new SparseSolverSettings(Mode.SPARSE));
        assertNull(sparse.getSparseFactorization(), "nothing is factored yet");
        for (int stepIndex = 0; stepIndex < 50; stepIndex++) {
            for (MatrixSolver each : new MatrixSolver[] {dense, sparse}) {
                each.buildMatrixA(netlist, DT, stepIndex * DT, false);
                each.buildVectorB(netlist, DT, stepIndex * DT, false);
                // A diode-like patch that toggles, so the factors are invalidated and rebuilt.
                if (stepIndex % 2 == 1) {
                    each.addCoefficient(5, 5, 3e-3);
                    each.addCoefficient(5, 6, -3e-3);
                    each.addCoefficient(6, 5, -3e-3);
                    each.addCoefficient(6, 6, 3e-3);
                    each.setMatrixChanged();
                }
                each.solve();
                each.shiftHistory();
            }
            for (int i = 0; i < dense.getP().length; i++) {
                assertTrue(dense.getP()[i] == sparse.getP()[i], "step " + stepIndex + " entry " + i);
            }
        }
        assertEquals(MatrixSolver.SparseFactorization.LEGACY_ORDER, sparse.getSparseFactorization());
        assertEquals(Backend.SPARSE, sparse.getBackend());
        assertTrue(sparse.getFactorizationCount() > 20, "the patches refactored: " + sparse.getFactorizationCount());
    }

    @Test
    void thePolicyUsesEjmlForPinnedSystemsAndForWiredSystemsAtOrAboveTheThreshold() {
        CircuitNetlist pinned = ladder(40);
        MatrixSolver pinnedSolver = solver(pinned, new SparseSolverSettings(Mode.SPARSE));
        step(pinnedSolver, pinned, 0);
        assertEquals(MatrixSolver.SparseFactorization.EJML, pinnedSolver.getSparseFactorization(), "pinned, below the threshold");

        CircuitNetlist wiredAbove = wired(120);
        MatrixSolver above = solver(wiredAbove, new SparseSolverSettings(Mode.SPARSE));
        step(above, wiredAbove, 0);
        assertEquals(MatrixSolver.SparseFactorization.EJML, above.getSparseFactorization(), "wired, above the threshold");

        CircuitNetlist wiredAt = wired(100);
        MatrixSolver auto = solver(wiredAt, AUTO);
        step(auto, wiredAt, 0);
        assertEquals(Backend.SPARSE, auto.getBackend());
        assertEquals(MatrixSolver.SparseFactorization.EJML, auto.getSparseFactorization(), "AUTO selects sparse only at the threshold");

        CircuitNetlist wiredBelow = wired(99);
        MatrixSolver forced = solver(wiredBelow, new SparseSolverSettings(Mode.SPARSE));
        step(forced, wiredBelow, 0);
        assertEquals(MatrixSolver.SparseFactorization.LEGACY_ORDER, forced.getSparseFactorization(), "wired, 99 nodes");

        CircuitNetlist small = wired(10);
        MatrixSolver raised = solver(small, new SparseSolverSettings(Mode.SPARSE, 5));
        step(raised, small, 0);
        assertEquals(MatrixSolver.SparseFactorization.EJML, raised.getSparseFactorization(), "a lowered threshold moves the limit");
        assertNull(solver(small, null).getSparseFactorization(), "dense has no sparse factorization");
        MatrixSolver dense = solver(small, null);
        step(dense, small, 0);
        assertNull(dense.getSparseFactorization());
    }

    @Test
    void aWiredNinetyNineNodeLegacyOrderFactorStaysSparse() {
        CircuitNetlist netlist = wired(99);
        MatrixSolver sparse = solver(netlist, new SparseSolverSettings(Mode.SPARSE));
        step(sparse, netlist, 0);
        long order = sparse.getMatrixSize();
        assertEquals(MatrixSolver.SparseFactorization.LEGACY_ORDER, sparse.getSparseFactorization());
        assertTrue(sparse.getFactorNonzeroCount() <= 7 * order, "factor nonzeros " + sparse.getFactorNonzeroCount());
        assertTrue(sparse.getCoefficientStorageBytes() < 8L * order * order,
                "numeric storage " + sparse.getCoefficientStorageBytes() + " bytes against dense " + 8L * order * order);
    }

    @Test
    void aWiredSingularMatrixIsReportedWithTheDenseMessageOnBothBackends() {
        CircuitNetlist netlist = wired(3);
        String[] messages = new String[2];
        int index = 0;
        for (Mode mode : new Mode[] {Mode.DENSE, Mode.SPARSE}) {
            MatrixSolver solver = solver(netlist, new SparseSolverSettings(mode));
            solver.buildMatrixA(netlist, DT, 0, false);
            solver.buildVectorB(netlist, DT, 0, false);
            double[][] a = solver.getSystemMatrix();
            // Remove node 3's row and column, so the reduced matrix is singular.
            for (int other = 0; other < a.length; other++) {
                if (a[3][other] != 0) solver.addCoefficient(3, other, -a[3][other]);
                if (other != 3 && a[other][3] != 0) solver.addCoefficient(other, 3, -a[other][3]);
            }
            solver.setMatrixChanged();
            messages[index++] = assertThrows(RuntimeException.class, solver::solve).getMessage();
            assertEquals(mode == Mode.SPARSE ? Backend.SPARSE : Backend.DENSE, solver.getBackend(), "no fallback");
        }
        assertEquals(messages[0], messages[1]);
        assertTrue(messages[0].contains("singular"), messages[0]);
    }

    private static MatrixSolver solverOf(CircuitNetlist netlist, SolverType method, SparseSolverSettings settings) {
        MatrixSolver solver = new MatrixSolver(method, settings);
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());
        return solver;
    }

    private static void assertSameBits(double[] expected, double[] actual, String label) {
        for (int i = 0; i < expected.length; i++) {
            assertEquals(Double.doubleToRawLongBits(expected[i]), Double.doubleToRawLongBits(actual[i]), label + " entry " + i);
        }
    }

    @Test
    void enabledDenseOnAPinnedLadderSolvesTheSameSystemAsSparseIncludingTheSourceBranch() {
        CircuitNetlist netlist = ladder(50);
        MatrixSolver dense = solver(netlist, new SparseSolverSettings(Mode.DENSE));
        MatrixSolver sparse = solver(netlist, new SparseSolverSettings(Mode.SPARSE));
        step(dense, netlist, 0);
        step(sparse, netlist, 0);
        assertEquals(Backend.DENSE, dense.getBackend());
        assertEquals(MatrixSolver.SparseFactorization.EJML, sparse.getSparseFactorization());
        assertUnits("pinned 50 nodes", dense.getP(), sparse.getP(), 50);
        assertEquals(1.0, dense.getP()[1], 1e-12, "the source node sits at the input");
    }

    @Test
    void enabledDenseOnAWiredLadderEqualsTheNullPathBitForBitForEveryMethod() {
        for (SolverType method : SolverType.values()) {
            CircuitNetlist netlist = wired(40);
            MatrixSolver legacy = solverOf(netlist, method, null);
            MatrixSolver enabled = solverOf(netlist, method, new SparseSolverSettings(Mode.DENSE));
            for (int stepIndex = 0; stepIndex < 20; stepIndex++) {
                for (MatrixSolver each : new MatrixSolver[] {legacy, enabled}) {
                    each.buildMatrixA(netlist, DT, stepIndex * DT, false);
                    each.buildVectorB(netlist, DT, stepIndex * DT, false);
                    each.solve();
                    each.shiftHistory();
                }
                assertSameBits(legacy.getP(), enabled.getP(), method + " step " + stepIndex);
            }
            assertEquals(8L * enabled.getMatrixSize() * enabled.getMatrixSize(), enabled.getCoefficientStorageBytes(),
                    "a wired solve never allocates the node-0-last copy");
        }
    }

    @Test
    void theNullPinnedPathStillFactorsTheGroundFirstMatrixOfTheDenseReference() {
        CircuitNetlist netlist = ladder(100);
        MatrixSolver legacy = solverOf(netlist, SolverType.SOLVER_BE, null);
        legacy.buildMatrixA(netlist, DT, 0, false);
        legacy.buildVectorB(netlist, DT, 0, false);
        int order = legacy.getMatrixSize();
        double[][] copy = new double[order][];
        for (int i = 0; i < order; i++) copy[i] = legacy.getSystemMatrix()[i].clone();
        double[][] rhs = new double[order][1];
        for (int i = 0; i < order; i++) rhs[i][0] = legacy.getb()[i];
        double[][] expected = new gecko.core.math.Matrix(copy).solve(new gecko.core.math.Matrix(rhs)).getArray();
        legacy.solve();
        for (int i = 0; i < order; i++) {
            assertEquals(Double.doubleToRawLongBits(expected[i][0]), Double.doubleToRawLongBits(legacy.getP()[i]),
                    "null settings must keep the legacy ground-first order, entry " + i);
        }
        assertEquals(8L * order * order, legacy.getCoefficientStorageBytes());
    }

    @Test
    void enabledDensePinnedKeepsTheLiveArrayAndRefactorsOnlyWhenFlagged() {
        CircuitNetlist netlist = ladder(30);
        SparseSolverSettings enabled = new SparseSolverSettings(Mode.DENSE);
        MatrixSolver solver = solverOf(netlist, SolverType.SOLVER_BE, enabled);
        solver.buildMatrixA(netlist, DT, 0, false);
        solver.buildVectorB(netlist, DT, 0, false);
        solver.solve();
        double[][] live = solver.getSystemMatrix();
        assertSame(live, solver.getSystemMatrix(), "the matrix stays the same live array");
        assertTrue(live[0][0] >= 1e15, "and keeps the original order, with the pin at index 0");
        double[] first = solver.getP().clone();
        assertEquals(1, solver.getFactorizationCount());

        solver.addCoefficient(4, 4, 0.5);
        solver.solve();
        assertSameBits(first, solver.getP(), "an unflagged patch keeps the cached factors");
        assertEquals(1, solver.getFactorizationCount());

        solver.addCoefficient(7, 7, 0.25);
        solver.setMatrixChanged();
        solver.solve();
        assertEquals(2, solver.getFactorizationCount());
        MatrixSolver fresh = solverOf(netlist, SolverType.SOLVER_BE, enabled);
        fresh.buildMatrixA(netlist, DT, 0, false);
        fresh.buildVectorB(netlist, DT, 0, false);
        fresh.addCoefficient(4, 4, 0.5);
        fresh.addCoefficient(7, 7, 0.25);
        fresh.solve();
        assertSameBits(fresh.getP(), solver.getP(), "a flagged patch equals a fresh solver with the same patch");

        solver.getSystemMatrix()[7][7] -= 0.25;
        solver.setMatrixChanged();
        solver.solve();
        assertEquals(3, solver.getFactorizationCount());
        MatrixSolver onlyFirstPatch = solverOf(netlist, SolverType.SOLVER_BE, enabled);
        onlyFirstPatch.buildMatrixA(netlist, DT, 0, false);
        onlyFirstPatch.buildVectorB(netlist, DT, 0, false);
        onlyFirstPatch.addCoefficient(4, 4, 0.5);
        onlyFirstPatch.solve();
        assertUnits("a flagged write to the live array refactors", onlyFirstPatch.getP(), solver.getP(), 30);
    }

    @Test
    void automaticSelectionBelowTheThresholdUsesTheEnabledDenseOrderOnPinnedSystems() {
        CircuitNetlist netlist = ladder(60);
        MatrixSolver auto = solver(netlist, AUTO);
        MatrixSolver enabled = solver(netlist, new SparseSolverSettings(Mode.DENSE));
        assertEquals(Backend.DENSE, auto.getBackend());
        for (int stepIndex = 0; stepIndex < 10; stepIndex++) {
            step(auto, netlist, stepIndex * DT);
            step(enabled, netlist, stepIndex * DT);
            assertSameBits(enabled.getP(), auto.getP(), "step " + stepIndex);
        }
    }

    @Test
    void theStorageEstimateCountsTheNodeZeroLastCopyOnlyForEnabledPinnedDenseSolves() {
        CircuitNetlist netlist = ladder(20);
        MatrixSolver enabled = solver(netlist, new SparseSolverSettings(Mode.DENSE));
        MatrixSolver legacy = solver(netlist, null);
        long square = (long) enabled.getMatrixSize() * enabled.getMatrixSize();
        assertEquals(8L * square, enabled.getCoefficientStorageBytes(), "nothing is allocated before the first solve");
        step(enabled, netlist, 0);
        step(legacy, netlist, 0);
        assertEquals(16L * square, enabled.getCoefficientStorageBytes());
        assertEquals(8L * square, legacy.getCoefficientStorageBytes());
        enabled.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());
        assertEquals(8L * square, enabled.getCoefficientStorageBytes(), "reinitializing drops the copy");
        MatrixSolver sparse = solver(netlist, new SparseSolverSettings(Mode.SPARSE));
        step(sparse, netlist, 0);
        assertTrue(sparse.getCoefficientStorageBytes() < 8L * square * 2);
    }

    @Test
    void anOverflowingFiniteSystemReportsOriginalRowsOnEveryDenseOrder() {
        // Finite inputs: row 1 overflows to infinity, and the back substitution then makes row 0 NaN (infinity times 0).
        // The legacy order scans ascending rows, so it reports row 0 as before. The enabled order scans node 0 last,
        // finds the infinite entry first and must report its original row 1, not its permuted position 0.
        String[] expected = {"Nonfinite solution at row 0", "Nonfinite solution at row 1"};
        SparseSolverSettings[] settings = {null, new SparseSolverSettings(Mode.DENSE)};
        for (int variant = 0; variant < settings.length; variant++) {
            MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE, settings[variant]);
            solver.initializeMatrices(1, 0, 0);
            solver.addCoefficient(0, 0, 1.0);
            solver.addCoefficient(1, 1, Double.MIN_NORMAL);
            solver.getb()[1] = Double.MAX_VALUE;
            solver.setMatrixChanged();
            double[] before = solver.getP().clone();
            IllegalStateException failure = assertThrows(IllegalStateException.class, solver::solve);
            assertEquals(expected[variant], failure.getMessage(), "variant " + variant);
            assertSameBits(before, solver.getP(), "a failed solve publishes nothing");
        }
    }
}
