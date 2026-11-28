package gecko.core.simulation.solver;

import gecko.core.math.Matrix;
import java.util.Arrays;
import java.util.Random;
import org.ejml.data.DMatrixSparseCSC;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The legacy-order sparse LU must return entries exactly equal, under {@code ==} so a signed zero is ignored, to those
 * of {@link gecko.core.math.LUDecomposition} through {@link Matrix#solve}. The dense arrays below are the reference
 * only, and the kernel under test reads sparse columns.
 */
class LegacyOrderSparseLuTest {
    private static final double[] PALETTE = {1e-7, 1e-7, 1e-7, 100, 100, 1.0, 0.5, 2.2e-2, 1e-3, 1e3};

    private static void stamp(double[][] a, int x, int y, double g) {
        a[x][x] += g;
        a[y][y] += g;
        a[x][y] -= g;
        a[y][x] -= g;
    }

    /** A wire-reference MNA matrix: conductance network with row and column 0 dropped, plus +-1 source rows. */
    private static double[][] mna(Random random, int nodes, int sources) {
        int order = nodes + sources + 1;
        double[][] a = new double[order][order];
        for (int node = 1; node <= nodes; node++) stamp(a, node, node - 1, PALETTE[random.nextInt(PALETTE.length)]);
        for (int extra = 0; extra < nodes * 2; extra++) {
            int x = random.nextInt(nodes + 1);
            int y = random.nextInt(nodes + 1);
            if (x != y) {
                stamp(a, x, y, random.nextBoolean() ? PALETTE[random.nextInt(PALETTE.length)] : Math.exp(random.nextGaussian() * 4));
            }
        }
        for (int s = 0; s < sources; s++) {
            int z = nodes + 1 + s;
            int x = 1 + random.nextInt(nodes);
            int y = random.nextInt(nodes + 1);
            if (x == y) y = 0;
            a[x][z] += 1;
            a[y][z] -= 1;
            a[z][x] += 1;
            a[z][y] -= 1;
        }
        double[][] reduced = new double[order - 1][];
        for (int r = 1; r < order; r++) reduced[r - 1] = Arrays.copyOfRange(a[r], 1, order);
        return reduced;
    }

    private static DMatrixSparseCSC csc(double[][] a) {
        int n = a.length;
        int nonzero = 0;
        for (double[] row : a) for (double value : row) if (value != 0) nonzero++;
        DMatrixSparseCSC matrix = new DMatrixSparseCSC(n, n, Math.max(nonzero, 1));
        int at = 0;
        for (int col = 0; col < n; col++) {
            matrix.col_idx[col] = at;
            for (int row = 0; row < n; row++) {
                if (a[row][col] != 0) {
                    matrix.nz_rows[at] = row;
                    matrix.nz_values[at++] = a[row][col];
                }
            }
        }
        matrix.col_idx[n] = at;
        matrix.nz_length = at;
        return matrix;
    }

    /** @return the dense reference solution, or null when the dense LU reports the matrix singular */
    private static double[] dense(double[][] a, double[] b) {
        double[][] copy = new double[a.length][];
        for (int i = 0; i < a.length; i++) copy[i] = a[i].clone();
        double[][] rhs = new double[a.length][1];
        for (int i = 0; i < a.length; i++) rhs[i][0] = b[i];
        try {
            double[][] x = new Matrix(copy).solve(new Matrix(rhs)).getArray();
            double[] result = new double[a.length];
            for (int i = 0; i < a.length; i++) result[i] = x[i][0];
            return result;
        } catch (RuntimeException singular) {
            assertEquals("Matrix of size " + a.length + " is singular.", singular.getMessage());
            return null;
        }
    }

    /** @return 1 for a nonsingular system whose entries are equal under {@code ==} (signed zero ignored), 0 for a shared singular verdict */
    private static int check(double[][] a, double[] b, String label) {
        double[] expected = dense(a, b);
        LegacyOrderSparseLu lu = new LegacyOrderSparseLu(a.length);
        boolean singular = lu.factor(csc(a));
        if (expected == null) {
            assertTrue(singular, label + ": the dense LU is singular, the sparse LU must be too");
            return 0;
        }
        assertFalse(singular, label + ": the dense LU is nonsingular, the sparse LU must be too");
        double[] x = new double[a.length];
        lu.solve(b, x);
        for (int i = 0; i < x.length; i++) assertTrue(x[i] == expected[i], label + " entry " + i + ": " + x[i] + " vs " + expected[i]);
        return 1;
    }

    @Test
    void sparseSolutionsEqualTheDenseLuEntryForEntry() {
        Random random = new Random(60);
        int solved = 0;
        int singular = 0;
        for (int trial = 0; trial < 1500; trial++) {
            int nodes = 3 + random.nextInt(60);
            double[][] a = mna(random, nodes, random.nextInt(5));
            double[] b = new double[a.length];
            for (int i = 0; i < b.length; i++) b[i] = random.nextInt(4) == 0 ? 0.0 : random.nextGaussian() * 300;
            if (check(a, b, "trial " + trial) == 1) solved++; else singular++;
        }
        assertTrue(solved > 1000, "most random systems are nonsingular: " + solved);
        assertTrue(singular > 0, "and some are singular, so both verdicts are exercised");
    }

    @Test
    void pivotTiesTakeTheFirstRowAsTheDenseLuDoes() {
        double[][] tie = {{2, 1, 0}, {-2, 3, 1}, {2, 0, 5}};
        check(tie, new double[] {1, 2, 3}, "equal magnitudes in the first column");
        double[][] swap = {{0, 1}, {1, 0}};
        check(swap, new double[] {3, 4}, "a zero leading entry forces a swap");
        double[][] sources = {{1e-7 + 100, -100, 1, 0}, {-100, 100, 0, 1}, {1, 0, 0, 0}, {0, 1, 0, 0}};
        check(sources, new double[] {0, 0, 5, -2}, "source rows with +-1 and equal conductances");
        double[][] equal = {{100, -100, 1}, {-100, 100, -1}, {1, -1, 0}};
        check(equal, new double[] {1, 1, 0}, "equal conductances");
    }

    @Test
    void singularMatricesAreReportedAsTheDenseLuReportsThem() {
        check(new double[][] {{1, 2}, {2, 4}}, new double[] {1, 2}, "rank one, nonzero entries");
        check(new double[][] {{0, 0}, {0, 1}}, new double[] {1, 1}, "an empty row");
        check(new double[][] {{1, 0, 0}, {0, 0, 0}, {0, 0, 1}}, new double[] {1, 1, 1}, "an empty middle row and column");
    }

    @Test
    void factorsAreSparseAndNeverOrderSquared() {
        Random random = new Random(7);
        double[][] a = mna(random, 60, 3);
        LegacyOrderSparseLu lu = new LegacyOrderSparseLu(a.length);
        lu.factor(csc(a));
        assertTrue(lu.factorNonzeros() > 0 && lu.factorNonzeros() < (long) a.length * a.length);
        assertTrue(lu.storageBytes() > 0);
    }
}
