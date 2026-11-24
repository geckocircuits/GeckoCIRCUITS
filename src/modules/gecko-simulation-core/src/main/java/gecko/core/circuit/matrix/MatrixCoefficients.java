package gecko.core.circuit.matrix;

/**
 * Write access to the coefficients of the MNA system matrix A. Stampers add their contributions through this seam,
 * so one stamping routine feeds either a dense array or a sparse store.
 */
public interface MatrixCoefficients {
    /**
     * Adds a value to one coefficient.
     *
     * @param row row index
     * @param col column index
     * @param value the amount to add, where a subtraction passes the negated amount
     */
    void add(int row, int col, double value);

    /**
     * Wraps a dense matrix. {@link #add} performs {@code a[row][col] += value}, so a dense run keeps
     * the arithmetic and operation order of direct array stamping.
     *
     * @param a the dense matrix, written in place
     * @return coefficients that write into {@code a}
     */
    static MatrixCoefficients dense(double[][] a) {
        return (row, col, value) -> a[row][col] += value;
    }
}
