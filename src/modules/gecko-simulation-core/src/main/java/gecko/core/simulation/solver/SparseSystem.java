package gecko.core.simulation.solver;

import gecko.core.circuit.matrix.MatrixCoefficients;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.interfaces.decomposition.LUSparseDecomposition_F64;
import org.ejml.interfaces.linsol.LinearSolverSparse;
import org.ejml.sparse.FillReducing;
import org.ejml.sparse.csc.factory.LinearSolverFactory_DSCC;

import java.util.Arrays;

/**
 * Sparse storage of the MNA matrix plus a retained EJML sparse LU. Memory is proportional to the number of
 * stored coefficients, and no order-by-order dense array exists.
 *
 * <p>The complete pinned matrix is factored with node 0 permuted last. Its row and column touch every grounded
 * element, so eliminating it first would fill the factors, while last it adds no fill. The permutation keeps every
 * coefficient, so the solution is that of the same system. Only the wire-reference mode excludes row and column 0.
 *
 * <p>Coefficients accumulate in a coordinate store with a hash index, so repeated stamps and later patches
 * hit the same slot. Clearing keeps the slots and zeroes the values. The factorization is rebuilt only when
 * a stored value differs from the values that were factored, so a rebuild with identical numbers keeps the factors.
 */
final class SparseSystem implements MatrixCoefficients {
    private final int order;
    private int[] rows = new int[64];
    private int[] cols = new int[64];
    private double[] values = new double[64];
    private int count;
    private int[] table = new int[256]; // slot + 1, zero when empty
    private int mask = table.length - 1;

    private final LinearSolverSparse<DMatrixSparseCSC, DMatrixRMaj> lu = LinearSolverFactory_DSCC.lu(FillReducing.NONE);
    private final DMatrixSparseCSC csc;
    private final DMatrixRMaj rhs;
    private final DMatrixRMaj solution;
    private double[] factoredValues = new double[0];
    private int factoredCount = -1;
    private int factoredOffset = -1;
    private int factorizations;

    private final boolean legacyOrderEligible;
    private LegacyOrderSparseLu legacy;
    private boolean factoredLegacy;

    /**
     * @param order the matrix order
     * @param legacyOrderEligible true when a wire-reference system may use {@link LegacyOrderSparseLu}, which repeats the
     *        dense LU's arithmetic, and false keeps EJML for every system
     */
    SparseSystem(int order, boolean legacyOrderEligible) {
        this.order = order;
        this.legacyOrderEligible = legacyOrderEligible;
        csc = new DMatrixSparseCSC(order, order, 0);
        rhs = new DMatrixRMaj(order, 1);
        solution = new DMatrixRMaj(order, 1);
    }

    @Override
    public void add(int row, int col, double value) {
        if (row < 0 || row >= order || col < 0 || col >= order) {
            throw new ArrayIndexOutOfBoundsException("Coefficient (" + row + ", " + col + ") outside order " + order);
        }
        int slot = find(row, col);
        if (slot >= 0) {
            values[slot] += value;
        } else if (value != 0) {
            insert(row, col, value);
        }
    }

    private int find(int row, int col) {
        int index = hash(row, col) & mask;
        while (table[index] != 0) {
            int slot = table[index] - 1;
            if (rows[slot] == row && cols[slot] == col) return slot;
            index = (index + 1) & mask;
        }
        return -1;
    }

    private static int hash(int row, int col) {
        int h = row * 0x9E3779B1 ^ col * 0x85EBCA6B;
        return h ^ (h >>> 15);
    }

    private void insert(int row, int col, double value) {
        if (count == rows.length) {
            int grown = count * 2;
            rows = Arrays.copyOf(rows, grown);
            cols = Arrays.copyOf(cols, grown);
            values = Arrays.copyOf(values, grown);
        }
        if ((count + 1) * 2 > table.length) rehash(table.length * 2);
        rows[count] = row;
        cols[count] = col;
        values[count] = value;
        place(count++);
    }

    private void place(int slot) {
        int index = hash(rows[slot], cols[slot]) & mask;
        while (table[index] != 0) index = (index + 1) & mask;
        table[index] = slot + 1;
    }

    private void rehash(int size) {
        table = new int[size];
        mask = size - 1;
        for (int slot = 0; slot < count; slot++) place(slot);
    }

    /** Zeroes every coefficient and keeps the slots. */
    void clear() {
        Arrays.fill(values, 0, count, 0.0);
    }

    /** @return the index of the first nonfinite stored coefficient's row, or -1 when all are finite */
    int firstNonfiniteRow() {
        for (int slot = 0; slot < count; slot++) {
            if (!Double.isFinite(values[slot])) return rows[slot];
        }
        return -1;
    }

    /** @return the number of stored coefficients, including slots that currently hold zero */
    int storedCount() {
        return count;
    }

    /** @return the number of stored coefficients that are not zero */
    int nonzeroCount() {
        int nonzero = 0;
        for (int slot = 0; slot < count; slot++) if (values[slot] != 0) nonzero++;
        return nonzero;
    }

    /** @return nonzeros in the L and U factors of the last factorization, or 0 before the first one */
    long factorNonzeros() {
        if (factorizations == 0) return 0;
        if (factoredLegacy) return legacy.factorNonzeros();
        LUSparseDecomposition_F64<DMatrixSparseCSC> decomposition = lu.getDecomposition();
        return (long) decomposition.getLower(null).nz_length + decomposition.getUpper(null).nz_length;
    }

    /**
     * @return an estimate of the primitive-array bytes of the coefficient store, the hash index, the CSC copy, the LU
     *         factors and the vectors. EJML factors count as 12 bytes per nonzero. This is neither retained capacity nor
     *         total heap, and it excludes JVM object overhead.
     */
    long storageBytes() {
        long bytes = 4L * (rows.length + cols.length + table.length) + 8L * values.length + 8L * factoredValues.length;
        bytes += 4L * (csc.nz_rows.length + csc.col_idx.length) + 8L * csc.nz_values.length;
        bytes += factoredLegacy ? legacy.storageBytes() : 12L * factorNonzeros();
        return bytes + 8L * (rhs.data.length + solution.data.length);
    }

    int factorizations() {
        return factorizations;
    }

    /** @return true when the last successful factorization was the legacy-order kernel, false for EJML */
    boolean factoredWithLegacyOrder() {
        return factoredLegacy;
    }

    double get(int row, int col) {
        int slot = find(row, col);
        return slot < 0 ? 0.0 : values[slot];
    }

    /** Add the sparse matrix-vector product to an existing result vector. */
    void accumulateProduct(double[] solution, double[] result) {
        for (int slot = 0; slot < count; slot++) {
            int row = rows[slot];
            result[row] = Math.fma(values[slot], solution[cols[slot]], result[row]);
        }
    }

    /** @return a new dense copy of the coefficients */
    double[][] toDense() {
        double[][] dense = new double[order][order];
        for (int slot = 0; slot < count; slot++) dense[rows[slot]][cols[slot]] = values[slot];
        return dense;
    }

    /**
     * Solves the system for a right-hand side, factoring first when a coefficient differs from the factored values.
     * With {@code dropReference} 0 the LU covers the complete matrix. Otherwise row and column 0 are excluded, as the
     * wire-reference dense path does, and entry 0 of the result is 0.
     *
     * @param b the full-length right-hand side
     * @param x the full-length result
     * @param recheck true to compare the coefficients with the factored values; or false to trust the factors when present
     * @param dropReference 1 to exclude row and column 0, else 0
     * @throws RuntimeException when the matrix is singular
     */
    void solve(double[] b, double[] x, boolean recheck, int dropReference) {
        int n = order - dropReference;
        if (n > 0) {
            if ((recheck || factoredCount < 0 || factoredOffset != dropReference) && !factoredCurrent(dropReference)) {
                factor(dropReference);
            }
            for (int i = dropReference; i < order; i++) rhs.data[position(i, dropReference)] = b[i];
            if (factoredLegacy) legacy.solve(rhs.data, solution.data);
            else lu.solve(rhs, solution);
            for (int i = dropReference; i < order; i++) x[i] = solution.data[position(i, dropReference)];
        }
        if (dropReference == 1) x[0] = 0;
    }

    /**
     * Position of an original index in the permuted system. With the complete matrix node 0 moves last: its row
     * and column touch every grounded element, so eliminating it first would fill the factors, while eliminating it
     * last adds no fill. The permutation is symmetric, so every coefficient is kept and the system is the same.
     */
    private int position(int index, int dropReference) {
        if (dropReference == 1) return index - 1;
        return index == 0 ? order - 1 : index - 1;
    }

    private boolean factoredCurrent(int dropReference) {
        if (factoredCount < 0 || factoredOffset != dropReference || count < factoredCount) return false;
        for (int slot = 0; slot < factoredCount; slot++) if (values[slot] != factoredValues[slot]) return false;
        for (int slot = factoredCount; slot < count; slot++) if (values[slot] != 0) return false;
        return true;
    }

    private void factor(int dropReference) {
        int n = order - dropReference;
        int[] rowOf = new int[count];
        int[] colOf = new int[count];
        int[] colStart = new int[n + 1];
        int nonzero = 0;
        for (int slot = 0; slot < count; slot++) {
            if (values[slot] != 0 && rows[slot] >= dropReference && cols[slot] >= dropReference) {
                rowOf[slot] = position(rows[slot], dropReference);
                colOf[slot] = position(cols[slot], dropReference);
                colStart[colOf[slot] + 1]++;
                nonzero++;
            } else {
                rowOf[slot] = -1;
            }
        }
        // Stable counting sort by row, then by column, leaves each column's rows ascending.
        int[] rowStart = new int[n + 1];
        for (int slot = 0; slot < count; slot++) if (rowOf[slot] >= 0) rowStart[rowOf[slot] + 1]++;
        for (int i = 0; i < n; i++) rowStart[i + 1] += rowStart[i];
        int[] byRow = new int[nonzero];
        for (int slot = 0; slot < count; slot++) if (rowOf[slot] >= 0) byRow[rowStart[rowOf[slot]]++] = slot;
        for (int i = 0; i < n; i++) colStart[i + 1] += colStart[i];
        csc.reshape(n, n, Math.max(nonzero, 1));
        csc.nz_length = nonzero;
        int[] next = Arrays.copyOf(colStart, n);
        for (int slot : byRow) {
            int at = next[colOf[slot]]++;
            csc.nz_rows[at] = rowOf[slot];
            csc.nz_values[at] = values[slot];
        }
        System.arraycopy(colStart, 0, csc.col_idx, 0, n + 1);
        csc.indicesSorted = true;
        rhs.reshape(n, 1);
        solution.reshape(n, 1);
        factoredCount = -1;
        boolean useLegacy = legacyOrderEligible && dropReference == 1;
        if (useLegacy) {
            // The kernel needs the natural order of the reduced matrix, which is the wire-reference position map.
            legacy = new LegacyOrderSparseLu(n);
            if (legacy.factor(csc)) throw new RuntimeException("Matrix of size " + n + " is singular.");
        } else {
            boolean ok;
            try {
                ok = lu.setA(csc);
            } catch (RuntimeException failure) {
                throw new RuntimeException("Matrix of size " + order + " is singular.", failure);
            }
            if (!ok) throw new RuntimeException("Matrix of size " + order + " is singular.");
        }
        factoredLegacy = useLegacy;
        factorizations++;
        if (factoredValues.length < count) factoredValues = new double[values.length];
        System.arraycopy(values, 0, factoredValues, 0, count);
        factoredCount = count;
        factoredOffset = dropReference;
    }
}
