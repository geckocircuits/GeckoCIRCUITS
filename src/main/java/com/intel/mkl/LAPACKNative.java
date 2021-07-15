/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.mkl;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

/**
 * Native methods for LAPACK class.
 *
 * <p>This class is added to differentiate native names from
 * Java ones in the LAPACK class.
 *
 * @see LAPACK
 */
 class LAPACKNative {

    /** Load native library */

    static {                
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            System.loadLibrary( "libiomp5ui" );
        }
        System.loadLibrary("mkl_java_stubs");
        //System.loadLibrary( "mkl_java_stubs" );
    }

    static native int PARADISO(int[] pt, int maxfct, int mnum, int mtype, int phase, int n, double[] values, int[] ai, int[] aj, int idum, int nrhs, int[] iparm, int msglvl, double[] ddum, double[] ddum0, int error);
        
    static native int cgesv(int n, int nrhs, float[] a, int lda, int[] ipiv, float[] b, int ldb);
    static native int sgesv(int n, int nrhs, float[] a, int lda, int[] ipiv, float[] b, int ldb);
    static native int dgesv(int n, int nrhs, double[] a,int lda, int[] ipiv, double[] b,int ldb);
    
        
    
    static native int zgesv(int n, int nrhs, double[] a,int lda, int[] ipiv, double[] b,int ldb);

    static native int ssyev(int jobz, int uplo, int n, float[] a,int lda, float[] w);
    static native int dsyev(int jobz, int uplo, int n, double[] a,int lda, double[] w);
    
    static native int sgeev(int jobvl, int jobvr, int n, float[] a, int lda, float[] wr, float[] wi, float[] vl, int ldvl, float[] vr, int ldvr);
    static native int dgeev(int jobvl, int jobvr, int n, double[] a, int lda, double[] wr, double[] wi, double[] vl, int ldvl, double[] vr, int ldvr);
    static native int cgeev(int jobvl, int jobvr, int n, float[] a, int lda, float[] w, float[] vl, int ldvl, float[] vr, int ldvr);
    static native int zgeev(int jobvl, int jobvr, int n, double[] a, int lda, double[] w, double[] vl, int ldvl, double[] vr, int ldvr);

    static native int sgesvd(int jobu, int jobvt, int m, int n, float[] a, int lda, float[] s, float[] u, int ldu, float[] vt, int ldvt, float[] sd);
    static native int dgesvd(int jobu, int jobvt, int m, int n, double[] a, int lda, double[] s, double[] u, int ldu, double[] vt, int ldvt, double[] sd);
    static native int cgesvd(int jobu, int jobvt, int m, int n, float[] a, int lda, float[] s, float[] u, int ldu, float[] vt, int ldvt, float[] sd);
    static native int zgesvd(int jobu, int jobvt, int m, int n, double[] a, int lda, double[] s, double[] u, int ldu, double[] vt, int ldvt, double[] sd);


    // ----- andy ----
    static native int zgetrs(int trans, int n, int nrhs, double[] a, int lda , int[] ipiv, double[] b, int ldb);
    static native int zgetrs2(int trans, int n, int nrhs, DoubleBuffer a, int lda , int[] ipiv, double[] b, int ldb);
    static native int cgetrs(int trans, int n, int nrhs, float[] a, int lda , int[] ipiv, float[] b, int ldb);
    static native int sgetrs(int trans, int n, int nrhs, float[] a, int lda , int[] ipiv, float[] b, int ldb);
    static native int dgetrs(int trans, int n, int nrhs, double[] a, int lda , int[] ipiv, double[] b, int ldb);
    static native int zgetrf(int m, int n, double[] a, int lda, int[] ipiv);
    static native int zgetrf2(int m, int n, DoubleBuffer a, int lda, int[] ipiv);
    static native int cgetrf(int m, int n, float[] a, int lda, int[] ipiv);
    static native int dgetrf(int m, int n, double[] a, int lda, int[] ipiv);
    static native int dgetri(int n, double[] a, int lda, int[] ipiv, double[] work, int lwork);
    static native int zgetri(int n, double[] a, int lda, int[] ipiv, double[] work, int lwork);
    static native int cgetri(int n, float[] a, int lda, int[] ipiv, float[] work, int lwork);

    static native int sgetrf(int m, int n, float[] a, int lda, int[] ipiv);
    static native int sgetri(int n, float[] a, int lda, int[] ipiv, float[] work, int lwork);

    static native int zsytri(char uplo, int n, double[] a, int lda, int[] ipiv, double[] work);
    static native int zsytrf(char uplo, int n, double[] a, int lda, int[] ipiv, double[] work, int lwork);
    static native int zsytrs(char uplo, int n, int nrhs, double[] a, int lda, int[] ipiv, double[] b, int ldb);

    static native int spotrf(int uplo, int n, float[] a, int lda);
    static native int spotri(int uplo, int n, float[] a, int lda);

    static native int spptrf(int uplo, int n, float[] a, int lda);
    static native int spptri(int uplo, int n, float[] a, int lda);

    static native int spptrf2(int uplo, int n, FloatBuffer fb, int lda);
    static native int spptri2(int uplo, int n, FloatBuffer fb, int lda);

    static native int dpotri(int uplo, int n, double[] a, int lda);
    static native int dpotrf(int uplo, int n, double[] a, int lda);

    static native int cpptrf(char uplo, int n, float[] a);
    static native int cpptri(char uplo, int n, float[] a);

    static native int csptrf(char c, int n, float[] array, int[] ipiv);
    static native int csptri(char c, int n, float[] array, float[] work, int[] ipiv);
    static native int csptrs(char uplo, int n, int nrhs, float[] a, int[] ipiv, float[] b, int ldb);

    static native int zsptri(char uplo, int n, double[] a, int[] ipiv, double[] work);
    static native int zsptrf(char uplo, int n, double[] a, int[] ipiv);
    static native int zsptrs(char uplo, int n, int nrhs, double[] a, int[] ipiv, double[] b, int ldb);

    static native int sgecon(char norm, int n, float[] a, int lda, float anorm, float[] rcond);
    static native int dgecon(char norm, int n, double[] a, int lda, double anorm, double[] rcond);
    static native int zgecon(char norm, int n, double[] a, int lda, double anorm, double[] rcond);

    static native int zgeequ(int m, int n, double[] a, int lda, double[] r, double[] c, double[] rowcnd, double[] colcnd, double[] amax);
    static native int zgeequ2(int m, int n, DoubleBuffer a, int lda, double[] r, double[] c, double[] rowcnd, double[] colcnd, double[] amax);

    static native int zlaqge(int m, int n, double[] a, int lda, double[] r, double[] c, double[] rowcnd, double[] colcnd, double[] amax, char[] equed);

    static native int zsprfs(char uplo, int n, int nrhs, double[] af, double[] afp, int[] ipiv, double[] b, int ldb, double[] x, int ldx, double[] ferr, double[] berr, double[] work, double[] rwork);
    static native int csprfs(char uplo, int n, int nrhs, float[] af, float[] afp, int[] ipiv, float[] b, int ldb, float[] x, int ldx, float[] ferr, float[] berr, float[] work, float[] rwork);

    static native int claqge(int m, int n, float[] a, int lda, float[] r, float[] c, float[] rowcnd, float[] colcnd, float[] amax, char[] equed);
    static native int cgeequ(int m, int n, float[] a, int lda, float[] r, float[] c, float[] rowcnd, float[] colcnd, float[] amax);
 }
