/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.math;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

//package Jama;
/** LU Decomposition.
<P>
For an m-by-n matrix A with m >= n, the LU decomposition is an m-by-n
unit lower triangular matrix L, an n-by-n upper triangular matrix U,
and a permutation vector piv of length m so that A(piv,:) = L*U.
If m < n, then L is m-by-m and U is m-by-n.
<P>
The LU decompostion with pivoting always exists, even if the matrix is
singular, so the constructor will never fail.  The primary use of the
LU decomposition is in the solution of square systems of simultaneous
linear equations.  This will fail if isNonsingular() returns false.
 */
public class BigLUDecomposition implements java.io.Serializable {

    private static final MathContext mc = new MathContext(20, RoundingMode.HALF_EVEN);
//test
/* ------------------------
    Class variables
     * ------------------------ */
    /** Array for internal storage of decomposition.
    @serial internal array storage.
     */
    private BigDecimal[][] LU;
    /** Row and column dimensions, and pivot sign.
    @serial column dimension.
    @serial row dimension.
    @serial pivot sign.
     */
    private int m, n, pivsign;
    /** Internal storage of pivot vector.
    @serial pivot vector.
     */
    private int[] piv;

    /* ------------------------
    Constructor
     * ------------------------ */
    /** LU Decomposition
    @param  A   Rectangular matrix
    @return     Structure to access L, U and piv.
     */
    public BigLUDecomposition(BigMatrix A) {

        // Use a "left-looking", dot-product, Crout/Doolittle algorithm.

        LU = A.getArrayCopy();

        m = A.getRowDimension();
        n = A.getColumnDimension();

        piv = new int[m];
        for (int i = 0; i < m; i++) {
            piv[i] = i;
        }
        pivsign = 1;
        BigDecimal[] LUrowi;
        BigDecimal[] LUcolj = new BigDecimal[m];

        // Outer loop.

        for (int j = 0; j < n; j++) {
            // Make a copy of the j-th column to localize references.
            for (int i = 0; i < m; i++) {
                LUcolj[i] = new BigDecimal(0);
                LUcolj[i] = LUcolj[i].add(LU[i][j]);
            }

            // Apply previous transformations.

            for (int i = 0; i < m; i++) {
                LUrowi = LU[i];

                // Most of the time is spent in the following dot product.

                int kmax = Math.min(i, j);
                BigDecimal s = new BigDecimal(0.0);

                for (int k = 0; k < kmax; k++) {
                    s = s.add(LUrowi[k].multiply(LUcolj[k]));
                }

                LUcolj[i] = LUcolj[i].subtract(s);
                LUrowi[j] = new BigDecimal(0);
                LUrowi[j] = LUrowi[j].add(LUcolj[i]);
                LU[i][j] = LUrowi[j];

            }

            // Find pivot and exchange if necessary.

            int p = j;
            for (int i = j + 1; i < m; i++) {
                if (LUcolj[i].abs().doubleValue() > LUcolj[p].abs().doubleValue()) {
                    p = i;
                }
            }
            if (p != j) {
                for (int k = 0; k < n; k++) {
                    BigDecimal t = LU[p][k];
                    LU[p][k] = LU[j][k];
                    LU[j][k] = t;
                }
                int k = piv[p];
                piv[p] = piv[j];
                piv[j] = k;
                pivsign = -pivsign;
            }

            // Compute multipliers.

            if (j < m & LU[j][j].abs().doubleValue() > 1e-30) {
                for (int i = j + 1; i < m; i++) {
                    LU[i][j] = LU[i][j].divide(LU[j][j], mc);
                }
            }
        }

    }

    /* ------------------------
    Temporary, experimental code.
    ------------------------ *\

    \** LU Decomposition, computed by Gaussian elimination.
    <P>
    This constructor computes L and U with the "daxpy"-based elimination
    algorithm used in LINPACK and MATLAB.  In Java, we suspect the dot-product,
    Crout algorithm will be faster.  We have temporarily included this
    constructor until timing experiments confirm this suspicion.
    <P>
    @param  A             Rectangular matrix
    @param  linpackflag   Use Gaussian elimination.  Actual value ignored.
    @return               Structure to access L, U and piv.
     *\

    public LUDecomposition (Matrix A, int linpackflag) {
    // Initialize.
    LU = A.getArrayCopy();
    m = A.getRowDimension();
    n = A.getColumnDimension();
    piv = new int[m];
    for (int i = 0; i < m; i++) {
    piv[i] = i;
    }
    pivsign = 1;
    // Main loop.
    for (int k = 0; k < n; k++) {
    // Find pivot.
    int p = k;
    for (int i = k+1; i < m; i++) {
    if (Math.abs(LU[i][k]) > Math.abs(LU[p][k])) {
    p = i;
    }
    }
    // Exchange if necessary.
    if (p != k) {
    for (int j = 0; j < n; j++) {
    double t = LU[p][j]; LU[p][j] = LU[k][j]; LU[k][j] = t;
    }
    int t = piv[p]; piv[p] = piv[k]; piv[k] = t;
    pivsign = -pivsign;
    }
    // Compute multipliers and eliminate k-th column.
    if (LU[k][k] != 0.0) {
    for (int i = k+1; i < m; i++) {
    LU[i][k] /= LU[k][k];
    for (int j = k+1; j < n; j++) {
    LU[i][j] -= LU[i][k]*LU[k][j];
    }
    }
    }
    }
    }

    \* ------------------------
    End of temporary code.
     * ------------------------ */

    /* ------------------------
    Public Methods
     * ------------------------ */
    /** Is the matrix nonsingular?
    @return     true if U, and hence A, is nonsingular.
     */
    public boolean isNonsingular() {

        for (int j = 0; j < n; j++) {
            if (LU[j][j].abs().doubleValue() == 0) {
                System.err.println(" j: " + j);
                return false;
            }
        }
        return true;
    }

    /** Return pivot permutation vector
    @return     piv
     */
    public int[] getPivot() {
        int[] p = new int[m];
        for (int i = 0; i < m; i++) {
            p[i] = piv[i];
        }
        return p;
    }

    /** Return pivot permutation vector as a one-dimensional double array
    @return     (double) piv
     */
    public double[] getDoublePivot() {
        double[] vals = new double[m];
        for (int i = 0; i < m; i++) {
            vals[i] = (double) piv[i];
        }
        return vals;
    }

    /** Solve A*X = B
    @param  B   A Matrix with as many rows as A and any number of columns.
    @return     X so that L*U*X = B(piv,:)
    @exception  IllegalArgumentException Matrix row dimensions must agree.
    @exception  RuntimeException  Matrix is singular.
     */
    public BigMatrix solve(final BigMatrix B) {
        if (B.getRowDimension() != m) {
            throw new IllegalArgumentException("Matrix row dimensions must agree.");
        }
        if (!this.isNonsingular()) {
            throw new RuntimeException("Matrix is singular.");
        }

        // Copy right hand side with pivoting
        int nx = B.getColumnDimension();
        BigMatrix Xmat = B.getMatrix(piv, 0, nx - 1);
        BigDecimal[][] X = Xmat.getArray();

        // Solve L*Y = B(piv,:)
        for (int k = 0; k < n; k++) {
            for (int i = k + 1; i < n; i++) {
                for (int j = 0; j < nx; j++) {
                    X[i][j] = X[i][j].subtract(X[k][j].multiply(LU[i][k]));
                }
            }
        }
        // Solve U*X = Y;
        for (int k = n - 1; k >= 0; k--) {
            for (int j = 0; j < nx; j++) {
                X[k][j] = X[k][j].divide(LU[k][k], mc);
            }
            for (int i = 0; i < k; i++) {
                for (int j = 0; j < nx; j++) {
                    X[i][j] = X[i][j].subtract(X[k][j].multiply(LU[i][k]));
                }
            }
        }
        return Xmat;
    }
}
