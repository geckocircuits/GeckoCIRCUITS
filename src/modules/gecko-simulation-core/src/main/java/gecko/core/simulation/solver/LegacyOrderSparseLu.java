package gecko.core.simulation.solver;

import java.util.Arrays;
import org.ejml.data.DMatrixSparseCSC;

/**
 * Sparse LU that repeats, nonzero by nonzero, the scalar arithmetic of {@code gecko.core.math.LUDecomposition} and
 * its {@code solve}: left-looking Crout columns, dot products summed from 0.0 in ascending {@code k}, the first
 * strictly largest pivot, whole-row swaps, and the same triangular solve order. For the same A and b it returns
 * values that are exactly equal under Java {@code ==} to the dense LU's, with the sign of a zero ignored, so a
 * forced-sparse run of a small circuit follows the dense trace numerically exactly.
 *
 * <p>L is kept by rows, U by columns, and no order-by-order array exists. Only products with an exact zero factor
 * are skipped, which changes at most the sign of a zero. Nothing is reordered, fused or compensated.
 */
final class LegacyOrderSparseLu {
    private final int n;
    private int[][] lCols;
    private double[][] lVals;
    private int[] lLen;
    private int[] uStart;
    private int[] uRow = new int[16];
    private double[] uVal = new double[16];
    private int uNz;
    private int[] perm;
    private double[] diag;
    private double[] column;
    private boolean singular;

    LegacyOrderSparseLu(int n) {
        this.n = n;
    }

    /** @return true when a zero pivot was found, so {@link #solve} would reject the matrix as the dense LU does */
    boolean factor(DMatrixSparseCSC a) {
        lCols = new int[n][];
        lVals = new double[n][];
        lLen = new int[n];
        for (int i = 0; i < n; i++) {
            lCols[i] = new int[4];
            lVals[i] = new double[4];
        }
        uStart = new int[n + 1];
        uNz = 0;
        perm = new int[n];
        diag = new double[n];
        int[] posOf = new int[n];
        for (int i = 0; i < n; i++) {
            perm[i] = i;
            posOf[i] = i;
        }
        singular = false;
        if (column == null) column = new double[n];
        double[] x = column;
        for (int j = 0; j < n; j++) {
            // ponytail: this scan makes factoring O(n^2 + n * nnz(L)), and it only runs below thresholdNodes (default 100).
            Arrays.fill(x, 0.0);
            for (int e = a.col_idx[j]; e < a.col_idx[j + 1]; e++) x[posOf[a.nz_rows[e]]] = a.nz_values[e];
            for (int i = 0; i < n; i++) {
                int kmax = Math.min(i, j);
                double s = 0.0;
                int[] cols = lCols[i];
                double[] lv = lVals[i];
                for (int t = 0, len = lLen[i]; t < len; t++) {
                    int k = cols[t];
                    if (k >= kmax) break;
                    double u = x[k];
                    if (u != 0.0) s += lv[t] * u;
                }
                x[i] -= s;
            }
            int p = j;
            for (int i = j + 1; i < n; i++) if (Math.abs(x[i]) > Math.abs(x[p])) p = i;
            if (p != j) {
                int[] tc = lCols[p]; lCols[p] = lCols[j]; lCols[j] = tc;
                double[] tv = lVals[p]; lVals[p] = lVals[j]; lVals[j] = tv;
                int tl = lLen[p]; lLen[p] = lLen[j]; lLen[j] = tl;
                int r = perm[p]; perm[p] = perm[j]; perm[j] = r;
                posOf[perm[p]] = p;
                posOf[perm[j]] = j;
                double t = x[p]; x[p] = x[j]; x[j] = t;
            }
            uStart[j] = uNz;
            for (int i = 0; i <= j; i++) if (x[i] != 0.0) appendU(i, x[i]);
            double pivot = x[j];
            diag[j] = pivot;
            if (pivot == 0.0) {
                singular = true;
                continue;
            }
            for (int i = j + 1; i < n; i++) if (x[i] != 0.0) appendL(i, j, x[i] / pivot);
        }
        uStart[n] = uNz;
        return singular;
    }

    /** Solves with the dense LU's loop order. {@code b} and {@code x} hold the reduced system in their first n entries. */
    void solve(double[] b, double[] x) {
        for (int i = 0; i < n; i++) x[i] = b[perm[i]];
        for (int i = 0; i < n; i++) {
            int[] cols = lCols[i];
            double[] lv = lVals[i];
            for (int t = 0, len = lLen[i]; t < len; t++) x[i] -= x[cols[t]] * lv[t];
        }
        for (int k = n - 1; k >= 0; k--) {
            x[k] /= diag[k];
            double xk = x[k];
            for (int e = uStart[k]; e < uStart[k + 1]; e++) if (uRow[e] < k) x[uRow[e]] -= xk * uVal[e];
        }
    }

    /** @return strict L, the unit diagonal and U, counted as EJML's lower and upper factors are */
    long factorNonzeros() {
        long count = (long) n + uNz;
        for (int len : lLen) count += len;
        return count;
    }

    /** @return an estimate of the primitive-array bytes of this factorization, without JVM object overhead */
    long storageBytes() {
        long bytes = 4L * (uRow.length + uStart.length + perm.length) + 8L * (uVal.length + diag.length + column.length);
        for (int i = 0; i < n; i++) bytes += 12L * lCols[i].length;
        return bytes;
    }

    private void appendU(int row, double value) {
        if (uNz == uRow.length) {
            uRow = Arrays.copyOf(uRow, uNz * 2);
            uVal = Arrays.copyOf(uVal, uNz * 2);
        }
        uRow[uNz] = row;
        uVal[uNz++] = value;
    }

    private void appendL(int position, int col, double value) {
        if (lLen[position] == lCols[position].length) {
            lCols[position] = Arrays.copyOf(lCols[position], lLen[position] * 2);
            lVals[position] = Arrays.copyOf(lVals[position], lLen[position] * 2);
        }
        lCols[position][lLen[position]] = col;
        lVals[position][lLen[position]++] = value;
    }
}
