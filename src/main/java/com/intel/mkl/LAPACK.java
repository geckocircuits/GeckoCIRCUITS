/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.mkl;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

/**
 * Wrappers to LAPACK functions from Intel MKL.
 *
 * <p>The LAPACK Interface for C/C++ is used for accessing to
 * the LAPACK functions. See MKL Reference Manual, Chapters 3,4,5.
 *
 * <p>Both Java and C parts of the wrapper LAPACK demonstrate the
 * straightforward approach similar to CBLAS wrapper.
 *
 * <p>To make interface more suitable some modification of original
 * LAPACK subroutines has been performed. This is mainly removing
 * work arrays and replacing chars arguments with integers ones,
 * similar to CBLAS interface to BLAS subroutines. Also "info" is
 * returned as function value. For this purposes one more layer
 * has been created, see files clapack.h and clapack.c.
 *
 * <p>These wrappers assume using 1-dimensional Java arrays of
 * the type double[] or float[] to process floating-point data.
 * A multi-dimensional data series is stored into 1-dimensional
 * array as column-major 1-dimensional data series.
 *
 * <p>A complex number is stored as 2-elements array: real part
 * of the number as the 1st element of the array, and imaginary
 * part as the 2nd.
 *
 * <p>A complex data series of the length N is stored as a real
 * series of the length 2*N. The n-th complex element is stored
 * as the pair of 2*n-th and (2*n+1)-th real elements: the even
 * indexed for real part and the odd indexed for imaginary part.
 *
 * <p>For more details, please see the MKL User's Guide.
 *
 */
public final class LAPACK {
    
    /** Instantiation is disabled. */
    private LAPACK() {
    }

    //////////////////////////////////////////////////////////
    /** Constants for LAPACK_JOB enum. */
    public final static class JOB {

        private JOB() {
        }
        /** job ='N' */
        public final static int JobN = 201;
        /** job ='V' */
        public final static int JobV = 202;
        /** job ='A' */
        public final static int JobA = 203;
        /** job ='S' */
        public final static int JobS = 204;
        /** job ='O' */
        public final static int JobO = 205;
    }

    //////////////////////////////////////////////////////////
    //--------- andy -----------
    /**
     * Computes the Cholesky factorization of a symmetric positive definite Matrix
     * @param uplo Indicates whether the upper or lower triangular part of A is
     * stored and how A is factored: If uplo = 'U', the array a stores the upper
     * triangular part of the matrix A. If uplo = 'L', the array a stores the lower
     * triangular part of the matrix A.
     * @param n The order of matrix A; n ≥ 0.
     * @param a The array a contains either the upper or the lower triangular part of the matrix A (see uplo).
     * @param lda The first dimension of a.
     * @return If info=0, the execution is successful. If info = -i, the i-th
     * parameter had an illegal value. If info = i, the leading minor of order i
     * (and therefore the matrix A itself) is not positive-definite, and the
     * factorization could not becompleted. This may indicate an error in forming
     * the matrix A.
     */
    public static int dpotrf(int uplo, int n, double[] a, int lda) {
        return LAPACKNative.dpotrf(uplo, n, a, lda);
    }

    /**
     * Computes the inverse of a symmetric (Hermitian) positive-definite matrix.
     * @param uplo  Must be 'U' or 'L'. Indicates whether A is upper or lower triangular:
     * If uplo = 'U', then A is upper triangular. If uplo = 'L', then A is lower triangular.
     * @param n The order of the matrix A; n ≥ 0.
     * @param a Contains the factorization of the matrix A, as returned by ?potrf.
     * @param lda The first dimension of a; lda ≥ max(1, n)
     * @return matrix a is overwritten. Be careful, only half of the matrix containes the
     * information, i.e. the matrix a is not yet symmetric!
     */
    public static int dpotri(int uplo, int n, double[] a, int lda) {
        return LAPACKNative.dpotri(uplo, n, a, lda);
    }




    /**
     *
     * @param uplo Must be 'U' or 'L'. Indicates whether the upper or lower triangular part of A is stored and
     * how A is factored: If uplo = 'U', the array a stores the upper triangular part of the matrix A.
     * If uplo = 'L', the array a stores the lower triangular part of the matrix A.
     * @param n The order of matrix A; n ≥ 0.
     * @param a Array, DIMENSION (lda,*). The array a contains either the upper or the lower triangular part of the matrix A (see uplo). The second
     * dimension of a must be at least max(1, n).
     * @param lda The first dimension of a.
     * @return INTEGER. If info=0, the execution is successful. If info = -i, the i-th parameter had an illegal value.
     * If info = i, the leading minor of order i (and therefore the matrix A itself) is not positive-definite, and the factorization could not be
     * completed. This may indicate an error in forming the matrix A.
     */
    public static int spotrf(int uplo, int n, float[] a, int lda) {
        return LAPACKNative.spotrf(uplo, n, a, lda);
    }

    public static int spotri(int uplo, int n, float[] a, int lda) {
        return LAPACKNative.spotri(uplo, n, a, lda);
    }

    public static int spptrf(int uplo, int n, float[] a, int lda) {
        return LAPACKNative.spptrf(uplo, n, a, lda);
    }

    public static int spptri(int uplo, int n, float[] a, int lda) {
        return LAPACKNative.spptri(uplo, n, a, lda);
    }

    public static int spptrf2(int uplo, int n, FloatBuffer fb, int lda) {
        return LAPACKNative.spptrf2(uplo, n, fb, lda);
    }

    public static int spptri2(int uplo, int n, FloatBuffer fb, int lda) {
        return LAPACKNative.spptri2(uplo, n, fb, lda);
    }


    /**
     * Wrapper for MKL function dgetrf().
     */
    public static int zgetrs(int trans, int n, int nrhs, double[] a, int lda, int[] ipiv, double[] b, int ldb) {
        return LAPACKNative.zgetrs(trans, n, nrhs, a, lda, ipiv, b, ldb);
    }

    /**
     * Wrapper for MKL function dgetrf().
     */
    public static int zgetrs2(int trans, int n, int nrhs, DoubleBuffer a, int lda, int[] ipiv, double[] b, int ldb) {
        return LAPACKNative.zgetrs2(trans, n, nrhs, a, lda, ipiv, b, ldb);
    }

    /**
     * Wrapper for MKL function dgetrf().
     */
    public static int cgetrs(int trans, int n, int nrhs, float[] a, int lda, int[] ipiv, float[] b, int ldb) {
        return LAPACKNative.cgetrs(trans, n, nrhs, a, lda, ipiv, b, ldb);
    }

    /**
     * Wrapper for MKL function dgetrf().
     */
    public static int sgetrs(int trans, int n, int nrhs, float[] a, int lda, int[] ipiv, float[] b, int ldb) {
        return LAPACKNative.sgetrs(trans, n, nrhs, a, lda, ipiv, b, ldb);
    }

    /**
     * Wrapper for MKL function dgetrf().
     */
    public static int dgetrs(int trans, int n, int nrhs, double[] a, int lda, int[] ipiv, double[] b, int ldb) {
        return LAPACKNative.dgetrs(trans, n, nrhs, a, lda, ipiv, b, ldb);
    }

    public static int cpptri(char uplo, int n, float[] a) {
        return LAPACKNative.cpptri(uplo, n, a);
    }

    public static int cpptrf(char uplo, int n, float[] a) {
        return LAPACKNative.cpptrf(uplo, n, a);
    }

    public static int cgetrf(int m, int n, float[] a, int lda, int[] ipiv) {
        return LAPACKNative.cgetrf(m, n, a, lda, ipiv);
    }

    /**
     * Factorize the given matrix (LU Decomposition)
     * @param m The number of rows in the matrix A (m ≥ 0).
     * @param n The number of columns in A; n ≥ 0.
     * @param a Array, DIMENSION (lda,*). Contains the matrix A. The second dimension of a must be at least max(1, n).
     * @param lda The first dimension of array a.
     * @param ipiv Array, DIMENSION at least max(1,min(m, n)). The pivot indices; for 1 ≤ i ≤ min(m, n) , row i was interchanged with row ipiv(i).
     * @return  INTEGER. If info=0, the execution is successful. If info = -i, the i-th parameter had an illegal value.
     * If info = i, uii is 0. The factorization has been completed, but U is exactly singular. Division by 0 will occur if you use the factor U for solving a system of linear equations.
     */
    public static int zgetrf(int m, int n, double[] a, int lda, int[] ipiv) {
        return LAPACKNative.zgetrf(m, n, a, lda, ipiv);
    }

    /**
     * Factorize the given matrix (LU Decomposition)
     * @param m The number of rows in the matrix A (m ≥ 0).
     * @param n The number of columns in A; n ≥ 0.
     * @param a Array, DIMENSION (lda,*). Contains the matrix A. The second dimension of a must be at least max(1, n).
     * @param lda The first dimension of array a.
     * @param ipiv Array, DIMENSION at least max(1,min(m, n)). The pivot indices; for 1 ≤ i ≤ min(m, n) , row i was interchanged with row ipiv(i).
     * @return  INTEGER. If info=0, the execution is successful. If info = -i, the i-th parameter had an illegal value.
     * If info = i, uii is 0. The factorization has been completed, but U is exactly singular. Division by 0 will occur if you use the factor U for solving a system of linear equations.
     */
    public static int zgetrf2(int m, int n, DoubleBuffer a, int lda, int[] ipiv) {
        return LAPACKNative.zgetrf2(m, n, a, lda, ipiv);
    }


    /**
     * Wrapper for MKL function dgetrf().
     */
    public static int zsytrf(char uplo, int n, double[] a, int lda, int[] ipiv, double[] work, int lwork) {
        return LAPACKNative.zsytrf(uplo, n, a, lda, ipiv, work, lwork);
    }

    /**
     * Wrapper for MKL function dgetrf().
     */
    public static int zsytrs(char uplo, int n, int nrhs, double[] a, int lda, int[] ipiv, double[] b, int ldb) {
        return LAPACKNative.zsytrs(uplo, n, nrhs, a, lda, ipiv, b, ldb);
    }


    /**
     * Wrapper for MKL function dgetrf().
     */
    public static int zsptrf(char uplo, int n, double[] a, int[] ipiv) {
        return LAPACKNative.zsptrf(uplo, n, a, ipiv);
    }

    /**
     * Wrapper for MKL function zgetrs().
     */
    public static int zsptrs(char uplo, int n, int nrhs, double[] a, int[] ipiv, double[] b, int ldb) {
        return LAPACKNative.zsptrs(uplo, n, nrhs, a, ipiv, b, ldb);
    }


    /**
     * Factorize the given matrix (LU Decomposition)
     * @param m The number of rows in the matrix A (m ≥ 0).
     * @param n The number of columns in A; n ≥ 0.
     * @param a Array, DIMENSION (lda,*). Contains the matrix A. The second dimension of a must be at least max(1, n).
     * @param lda The first dimension of array a.
     * @param ipiv Array, DIMENSION at least max(1,min(m, n)). The pivot indices; for 1 ≤ i ≤ min(m, n) , row i was interchanged with row ipiv(i).
     * @return  INTEGER. If info=0, the execution is successful. If info = -i, the i-th parameter had an illegal value.
     * If info = i, uii is 0. The factorization has been completed, but U is exactly singular. Division by 0 will occur if you use the factor U for solving a system of linear equations.
     */
    public static int dgetrf(int m, int n, double[] a, int lda, int[] ipiv) {
        return LAPACKNative.dgetrf(m, n, a, lda, ipiv);
    }


    /**
     * Factorize the given matrix (LU Decomposition)
     * @param m The number of rows in the matrix A (m ≥ 0).
     * @param n The number of columns in A; n ≥ 0.
     * @param a Array, DIMENSION (lda,*). Contains the matrix A. The second dimension of a must be at least max(1, n).
     * @param lda The first dimension of array a.
     * @param ipiv Array, DIMENSION at least max(1,min(m, n)). The pivot indices; for 1 ≤ i ≤ min(m, n) , row i was interchanged with row ipiv(i).
     * @return  INTEGER. If info=0, the execution is successful. If info = -i, the i-th parameter had an illegal value.
     * If info = i, uii is 0. The factorization has been completed, but U is exactly singular. Division by 0 will occur if you use the factor U for solving a system of linear equations.
     */
    public static int sgetrf(int m, int n, float[] a, int lda, int[] ipiv) {
        return LAPACKNative.sgetrf(m, n, a, lda, ipiv);
    }


    /**
     * Invert the factorized matrix
     * @param n The order of the matrix A; n ≥ 0.
     * @param a a(lda,*) contains the factorization of the matrix A, as returned by ?getrf: A = P*L*U.
     * @param lda The first dimension of a; lda ≥ max(1, n).
     * @param ipiv Array, DIMENSION at least max(1, n). The ipiv array, as returned by ?getrf.
     * @param work  is a workspace array of dimension at least max(1,lwork).
     * @param lwork The size of the work array; lwork ≥ n. If lwork = -1, then a workspace query is assumed; the routine only
     * calculates the optimal size of the work array, returns this value as the first entry of the work array, and no error message related to lwork is
     * issued by xerbla.
     * @return
     */
    public static int sgetri(int n, float[] a, int lda, int[] ipiv, float[] work, int lwork) {
        return LAPACKNative.sgetri(n, a, lda, ipiv, work, lwork);
    }

    /**
     * Invert the factorized matrix
     * @param n The order of the matrix A; n ≥ 0.
     * @param a a(lda,*) contains the factorization of the matrix A, as returned by ?getrf: A = P*L*U.
     * @param lda The first dimension of a; lda ≥ max(1, n).
     * @param ipiv Array, DIMENSION at least max(1, n). The ipiv array, as returned by ?getrf.
     * @param work  is a workspace array of dimension at least max(1,lwork).
     * @param lwork The size of the work array; lwork ≥ n. If lwork = -1, then a workspace query is assumed; the routine only
     * calculates the optimal size of the work array, returns this value as the first entry of the work array, and no error message related to lwork is
     * issued by xerbla.
     * @return
     */
    public static int dgetri(int n, double[] a, int lda, int[] ipiv, double[] work, int lwork) {
        return LAPACKNative.dgetri(n, a, lda, ipiv, work, lwork);
    }

    /**
     * Invert the factorized matrix
     * @param n The order of the matrix A; n ≥ 0.
     * @param a a(lda,*) contains the factorization of the matrix A, as returned by ?getrf: A = P*L*U.
     * @param lda The first dimension of a; lda ≥ max(1, n).
     * @param ipiv Array, DIMENSION at least max(1, n). The ipiv array, as returned by ?getrf.
     * @param work  is a workspace array of dimension at least max(1,lwork).
     * @param lwork The size of the work array; lwork ≥ n. If lwork = -1, then a workspace query is assumed; the routine only
     * calculates the optimal size of the work array, returns this value as the first entry of the work array, and no error message related to lwork is
     * issued by xerbla.
     * @return
     */
    public static int zgetri(int n, double[] a, int lda, int[] ipiv, double[] work, int lwork) {
        return LAPACKNative.zgetri(n, a, lda, ipiv, work, lwork);
    }

    /**
     * Wrapper for MKL function cgetri().
     */
    public static int cgetri(int n, float[] a, int lda, int[] ipiv, float[] work, int lwork) {
        return LAPACKNative.cgetri(n, a, lda, ipiv, work, lwork);
    }

    /**
     * Wrapper for MKL function cgesv().
     */
    public static int cgesv(int n, int nrhs, float[] a, int lda, int[] ipiv, float[] b, int ldb) {
        return LAPACKNative.cgesv(n, nrhs, a, lda, ipiv, b, ldb);
    }

    /**
     * Wrapper for MKL function sgesv().
     */
    public static int sgesv(int n, int nrhs, float[] a, int lda, int[] ipiv, float[] b, int ldb) {
        return LAPACKNative.sgesv(n, nrhs, a, lda, ipiv, b, ldb);
    }

    /**
     * Wrapper for MKL function dgesv().
     */
    public static int dgesv(int n, int nrhs, double[] a, int lda, int[] ipiv, double[] b, int ldb) {
        return LAPACKNative.dgesv(n, nrhs, a, lda, ipiv, b, ldb);
    }

    /**
     * Wrapper for MKL function zgesv().
     */
    public static int zgesv(int n, int nrhs, double[] a, int lda, int[] ipiv, double[] b, int ldb) {
        return LAPACKNative.zgesv(n, nrhs, a, lda, ipiv, b, ldb);
    }

    /**
     * Wrapper for MKL function ssyev().
     *
     * <p>Unlike the original LAPACK subroutine argument jobz is integer type.
     * Also original LAPACK arguments work and lwork are skipped.
     */
    public static int ssyev(int jobz, int uplo, int n, float[] a, int lda, float[] w) {
        return LAPACKNative.ssyev(jobz, uplo, n, a, lda, w);
    }

    /**
     * Wrapper for MKL function dsyev().
     *
     * <p>Unlike the original LAPACK subroutine argument jobz is integer type.
     * Also original LAPACK arguments work and lwork are skipped.
     */
    public static int dsyev(int jobz, int uplo, int n, double[] a, int lda, double[] w) {
        return LAPACKNative.dsyev(jobz, uplo, n, a, lda, w);
    }


    public static int csptrf(char c, int n, float[] array, int[] ipiv) {
        return LAPACKNative.csptrf(c, n, array, ipiv);
    }

    public static int csptri(char c, int n, float[] array, float[] work, int[] ipiv) {
        return LAPACKNative.csptri(c, n, array, work, ipiv);
    }

    /**
     * Wrapper for MKL function zgetrs().
     */
    public static int csptrs(char uplo, int n, int nrhs, float[] a, int[] ipiv, float[] b, int ldb) {
        return LAPACKNative.csptrs(uplo, n, nrhs, a, ipiv, b, ldb);
    }



    /**
     *
     * @param uplo Must be 'U' or 'L'. If uplo = 'U', the upper triangle of A is stored. If uplo = 'L', the lower triangle of A is stored.
     * @param n The order of the matrix A; n ≥ 0.
     * @param nrhs  The number of right-hand sides; nrhs ≥ 0.
     * @param a contains the original packed matrix A, as supplied to ?sptrf.
     * @param ipiv Array, DIMENSION at least max(1, n). The ipiv array, as returned by ?sptrf.
     * @param b contains the right-hand side matrix B.
     * @param ldb The first dimension of b; ldb ≥ max(1, n).
     * @param x The refined solution matrix X.
     * @param ldx The first dimension of x; ldx ≥ max(1, n).
     * @param ferr Arrays, DIMENSION at least max(1, nrhs). Contain the component-wise forward error
     * @param berr Arrays, DIMENSION at least max(1, nrhs). Contain the component-wise backward error
     * @param work work(*) is a workspace array. The dimension of arrays ap and afp must be at least max(1,n(n+1)/2);
     * the second dimension of b and x must be at least max(1,nrhs); the dimension of work must be at least max(1, 3*n)
     * for real flavors and max(1, 2*n) for complex flavors.
     * @param rwork Workspace array, DIMENSION at least max(1, n).
     * @return info INTEGER. If info = 0, the execution is successful. If info = -i, the i-th parameter had an illegal valu
     */
    public static int csprfs(char uplo, int n, int nrhs, float[] af, float[] afp, int[] ipiv, float[] b, int ldb, float[] x,int ldx, float[] ferr, float[] berr,
            float[] work, float[] rwork) {
        return LAPACKNative.csprfs(uplo, n, nrhs, af, afp, ipiv, b, ldb, x, ldx, ferr, berr, work, rwork);
    }

    /**
     *
     * @param uplo Must be 'U' or 'L'. If uplo = 'U', the upper triangle of A is stored. If uplo = 'L', the lower triangle of A is stored.
     * @param n The order of the matrix A; n ≥ 0.
     * @param nrhs  The number of right-hand sides; nrhs ≥ 0.
     * @param a contains the original packed matrix A, as supplied to ?sptrf.
     * @param ipiv Array, DIMENSION at least max(1, n). The ipiv array, as returned by ?sptrf.
     * @param b contains the right-hand side matrix B.
     * @param ldb The first dimension of b; ldb ≥ max(1, n).
     * @param x The refined solution matrix X.
     * @param ldx The first dimension of x; ldx ≥ max(1, n).
     * @param ferr Arrays, DIMENSION at least max(1, nrhs). Contain the component-wise forward error
     * @param berr Arrays, DIMENSION at least max(1, nrhs). Contain the component-wise backward error
     * @param work work(*) is a workspace array. The dimension of arrays ap and afp must be at least max(1,n(n+1)/2);
     * the second dimension of b and x must be at least max(1,nrhs); the dimension of work must be at least max(1, 3*n)
     * for real flavors and max(1, 2*n) for complex flavors.
     * @param rwork Workspace array, DIMENSION at least max(1, n).
     * @return info INTEGER. If info = 0, the execution is successful. If info = -i, the i-th parameter had an illegal valu
     */
    public static int zsprfs(char uplo, int n, int nrhs, double[] af, double[] afp, int[] ipiv, double[] b, int ldb,
            double[] x,int ldx, double[] ferr, double[] berr, double[] work, double[] rwork) {
        return LAPACKNative.zsprfs(uplo, n, nrhs, af, afp, ipiv, b, ldb, x, ldx, ferr, berr, work, rwork);
    }


    /**
     * Wrapper for MKL function sgeev().
     *
     * <p>Unlike the original LAPACK subroutine argument jobvl and jobvr are integer type.
     * Also original LAPACK arguments work and lwork are skipped.
     */
    public static int sgeev(int jobvl, int jobvr, int n, float[] a, int lda, float[] wr, float[] wi, float[] vl, int ldvl, float[] vr, int ldvr) {
        return LAPACKNative.sgeev(jobvl, jobvr, n, a, lda, wr, wi, vl, ldvl, vr, ldvr);
    }

    /**
     * Wrapper for MKL function dgeev().
     *
     * <p>Unlike the original LAPACK subroutine argument jobvl and jobvr are integer type.
     * Also original LAPACK arguments work and lwork are skipped.
     */
    public static int dgeev(int jobvl, int jobvr, int n, double[] a, int lda, double[] wr, double[] wi, double[] vl, int ldvl, double[] vr, int ldvr) {
        return LAPACKNative.dgeev(jobvl, jobvr, n, a, lda, wr, wi, vl, ldvl, vr, ldvr);
    }

    /**
     * Wrapper for MKL function cgeev().
     *
     * <p>Unlike the original LAPACK subroutine argument jobvl and jobvr are integer type.
     * Also original LAPACK arguments work and lwork are skipped.
     */
    public static int cgeev(int jobvl, int jobvr, int n, float[] a, int lda, float[] w, float[] vl, int ldvl, float[] vr, int ldvr) {
        return LAPACKNative.cgeev(jobvl, jobvr, n, a, lda, w, vl, ldvl, vr, ldvr);
    }

    /**
     * Wrapper for MKL function zgeev().
     *
     * <p>Unlike the original LAPACK subroutine argument jobvl and jobvr are integer type.
     * Also original LAPACK arguments work and lwork are skipped.
     */
    public static int zgeev(int jobvl, int jobvr, int n, double[] a, int lda, double[] w, double[] vl, int ldvl, double[] vr, int ldvr) {
        return LAPACKNative.zgeev(jobvl, jobvr, n, a, lda, w, vl, ldvl, vr, ldvr);
    }

    /**
     * Wrapper for MKL function sgesvd().
     *
     * <p>Unlike the original LAPACK subroutine argument jobu and jobvt are integer type.
     * Also original LAPACK arguments work and lwork are skipped.
     * Additional argument sd contains the unconverged
     * superdiagonal elements of an upper bidiagonal matrix B
     * whose diagonal is in s if result (info) > 0 (i.e work(2:min(m,n)))
     */
    public static int sgesvd(int jobu, int jobvt, int m, int n, float[] a, int lda, float[] s, float[] u, int ldu, float[] vt, int ldvt, float[] sd) {
        return LAPACKNative.sgesvd(jobu, jobvt, m, n, a, lda, s, u, ldu, vt, ldvt, sd);
    }

    /**
     * Wrapper for MKL function dgesvd().
     *
     * <p>Unlike the original LAPACK subroutine argument jobu and jobvt are integer type.
     * Also original LAPACK arguments work and lwork are skipped.
     * Additional argument sd contains the unconverged
     * superdiagonal elements of an upper bidiagonal matrix B
     * whose diagonal is in s if result (info) > 0 (i.e work(2:min(m,n)))
     */
    public static int dgesvd(int jobu, int jobvt, int m, int n, double[] a, int lda, double[] s, double[] u, int ldu, double[] vt, int ldvt, double[] sd) {
        return LAPACKNative.dgesvd(jobu, jobvt, m, n, a, lda, s, u, ldu, vt, ldvt, sd);
    }

    /**
     * Wrapper for MKL function cgesvd().
     *
     * <p>Unlike the original LAPACK subroutine argument jobu and jobvt are integer type.
     * Also original LAPACK arguments work, rwork and lwork are skipped.
     * Additional argument sd contains the unconverged
     * superdiagonal elements of an upper bidiagonal matrix B
     * whose diagonal is in s if result (info) > 0 (i.e rwork(1:min(m,n)-1))
     */
    public static int cgesvd(int jobu, int jobvt, int m, int n, float[] a, int lda, float[] s, float[] u, int ldu, float[] vt, int ldvt, float[] sd) {
        return LAPACKNative.cgesvd(jobu, jobvt, m, n, a, lda, s, u, ldu, vt, ldvt, sd);
    }

    /**
     * Wrapper for MKL function zgesvd().
     *
     * <p>Unlike the original LAPACK subroutine argument jobu and jobvt are integer type.
     * Also original LAPACK arguments work, rwork and lwork are skipped.
     * Additional argument sd contains the unconverged
     * superdiagonal elements of an upper bidiagonal matrix B
     * whose diagonal is in s if result (info) > 0 (i.e rwork(1:min(m,n)-1))
     */
    public static int zgesvd(int jobu, int jobvt, int m, int n, double[] a, int lda, double[] s, double[] u, int ldu, double[] vt, int ldvt, double[] sd) {
        return LAPACKNative.zgesvd(jobu, jobvt, m, n, a, lda, s, u, ldu, vt, ldvt, sd);
    }

    public static int PARDISO(int[] pt, int maxfct, int mnum, int mtype, int phase, int n, double[] values, int[] ai, int[] aj, int idum, int nrhs, int[] iparm, int msglvl, double[] b, double[] x, int error) {
        return LAPACKNative.PARADISO(pt, maxfct, mnum, mtype, phase, n, values, ai, aj, idum, nrhs, iparm, msglvl, b, x, error);
    }

    public static int sgecon(char norm, int n, float[] a, int lda, float anorm, float[] rcond) {
        return LAPACKNative.sgecon(norm, n, a, lda, anorm, rcond);
    }

    public static int dgecon(char norm, int n, double[] a, int lda, double anorm, double[] rcond) {
        return LAPACKNative.dgecon(norm, n, a, lda, anorm, rcond);
    }

    public static int zgecon(char norm, int n, double[] a, int lda, double anorm, double[] rcond) {
        return LAPACKNative.zgecon(norm, n, a, lda, anorm, rcond);
    }


    public static int zgeequ(int m, int n, double[] a, int lda, double[] r, double[] c, double[] rowcnd, double[] colcnd, double[] amax) {
        return LAPACKNative.zgeequ(m, n, a, lda, r, c, rowcnd, colcnd, amax);
    }

    public static int zgeequ2(int m, int n, DoubleBuffer a, int lda, double[] r, double[] c, double[] rowcnd, double[] colcnd, double[] amax) {
        return LAPACKNative.zgeequ2(m, n, a, lda, r, c, rowcnd, colcnd, amax);
    }

    public static int zlaqge(int m,int n,double[] a,int lda,double[] r, double[] c, double[] rowcnd,double[] colcnd, double[] amax, char[] equed ) {
        return LAPACKNative.zlaqge(m, n, a, lda, r, c, rowcnd, colcnd, amax, equed);
    }

    public static int claqge(int m,int n,float[] a,int lda,float[] r, float[] c, float[] rowcnd, float[] colcnd, float[] amax, char[] equed ) {
        return LAPACKNative.claqge(m, n, a, lda, r, c, rowcnd, colcnd, amax, equed);
    }

    public static int cgeequ(int m, int n, float[] a, int lda, float[] r, float[] c, float[] rowcnd, float[] colcnd, float[] amax) {
        return LAPACKNative.cgeequ(m, n, a, lda, r, c, rowcnd, colcnd, amax);
    }


}
