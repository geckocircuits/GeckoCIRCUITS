/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package ch.technokrat.gecko.geckocircuits.circuit;

import com.intel.mkl.LAPACK;
import java.util.logging.Level;
import java.util.logging.Logger;




/**
 *
 * @author muesinga
 */
public class Paradiso {

    
    private final static int nrhs = 1;
    private final static int idum = 0;              /* Integer dummy. */
    /* Pardiso control parameters. */
    public int[] iparm = new int[64];

    /* Internal solver memory pointer pt,                  */
        /* 32-bit: int pt[64]; 64-bit: long int pt[64]         */
        /* or void *pt[64] should be OK on both architectures  */
    public int[] pt = new int[64];
    private static int mnum;
    private static int maxfct;
    private static int msglvl;


    public Paradiso() {
        
    }
    
   
    
    public static void factorize(double[] values, int[] ai, int[] aj, int n, int mtype, Paradiso paradiso) {
        /* Auxiliary variables. */

        double[] ddum = new double[2];        /* Double dummy */
        

        /* -------------------------------------------------------------------- */
        /* .. Setup Pardiso control parameters. */
        /* -------------------------------------------------------------------- */
        for (int i = 0; i < 64; i++) {
            paradiso.iparm[i] = 0;
        }
        paradiso.iparm[0] = 1; /* No solver default */
        paradiso.iparm[1] = 2; /* Fill-in reordering from METIS */
                        
        paradiso.iparm[2] = 4; /* Numbers of processors, value of OMP_NUM_THREADS */
        paradiso.iparm[3] = 0; /* 0No iterative-direct algorithm */
        paradiso.iparm[4] = 0; /* No user fill-in reducing permutation */
        paradiso.iparm[5] = 0; /* Write solution into x */
        paradiso.iparm[6] = 0; /* Not in use */
        paradiso.iparm[7] = 2; /* Max numbers of iterative refinement steps */
        paradiso.iparm[8] = 0; /* Not in use */
        paradiso.iparm[9] = 13; /* Perturb the pivot elements with 1E-13 */
        paradiso.iparm[10] = 1; /* Use nonsymmetric permutation and scaling MPS */
        paradiso.iparm[11] = 0; /* Not in use */
        paradiso.iparm[12] = 0; /* Maximum weighted matching algorithm is switched-on (default for non-symmetric) */
        paradiso.iparm[13] = 0; /* Output: Number of perturbed pivots */
        paradiso.iparm[14] = 0; /* Not in use */
        paradiso.iparm[15] = 0; /* Not in use */
        paradiso.iparm[16] = 0; /* Not in use */
        paradiso.iparm[17] = -1; /* Output: Number of nonzeros in the factor LU */
        paradiso.iparm[18] = -1; /* Output: Mflops for LU factorization */
        paradiso.iparm[19] = 0; /* Output: Numbers of CG Iterations */
        paradiso.iparm[28] = 1;
        maxfct = 1;         /* Maximum number of numerical factorizations.  */
        mnum = 1;         /* Which factorization to use. */
        
        msglvl = 0;         /* Print statistical information  */
        int error = 0;         /* Initialize error flag */

        /* -------------------------------------------------------------------- */
        /* .. Initialize the internal solver memory pointer. This is only */
        /* necessary for the FIRST call of the PARDISO solver. */
        /* -------------------------------------------------------------------- */
        for (int i = 0; i < 64; i++) {
            paradiso.pt[i] = 0;
        }
        
        
        /* -------------------------------------------------------------------- */
        /* ..  Reordering and Symbolic Factorization.  This step also allocates */
        /*     all memory that is necessary for the factorization.              */
        /* -------------------------------------------------------------------- */
        int phase = 11;        
        
        //long facstart = System.currentTimeMillis();        
        LAPACK.PARDISO(paradiso.pt, maxfct, mnum, mtype, phase,
                n, values, ai, aj, idum, nrhs,
                paradiso.iparm, msglvl, ddum, ddum, error);

        if (error != 0) {
                Logger.getLogger(Paradiso.class.getName()).log(Level.SEVERE, "\nERROR during symbolic factorization: " + error);
        }

        //System.out.println("\nReordering completed ... ");
        //System.out.println("\nNumber of nonzeros in factors  = " + iparm[17]);
        //System.out.println("\nNumber of factorization MFLOPS = " + iparm[18]);

        /* -------------------------------------------------------------------- */
        /* ..  Numerical factorization.                                         */
        /* -------------------------------------------------------------------- */
        phase = 22;
        LAPACK.PARDISO(paradiso.pt, maxfct, mnum, mtype, phase,
                n, values, ai, aj, idum, nrhs,
                paradiso.iparm, msglvl, ddum, ddum, error);

        if (error != 0) {
            Logger.getLogger(Paradiso.class.getName()).log(Level.SEVERE, "ERROR during numerical factorization: " + error);
        }
        //long facstop = System.currentTimeMillis();
        //System.out.println("factorization time: " + (facstop - facstart)/1000.0);
        //System.exit(45);
    }
    
    
    public static double[] solve(double[] values, int[] ai, int[] aj, double[] rhs, int n, int mtype, int nRHS, Paradiso paradiso) {
        /* RHS and solution vectors. */
        double[] x = null;
        if(mtype == 13) {
             x = new double[2 * n * nRHS];
        } else {
            x = new double[n * nRHS];
        }
        

        /* /\* -------------------------------------------------------------------- *\/     */
        /* /\* ..  Back substitution and iterative refinement.                      *\/ */
        /* -------------------------------------------------------------------- */
        int phase = 33;
        int error = 0;
        
        
        LAPACK.PARDISO(paradiso.pt, maxfct, mnum, mtype, phase,
                n, values, ai, aj, idum, nRHS,
                paradiso.iparm, msglvl, rhs, x, error);


        if (error != 0) {
            Logger.getLogger(Paradiso.class.getName()).log(Level.SEVERE, "\nERROR during solution: " + error);
        }
                
        /* -------------------------------------------------------------------- */
        /* ..  Termination and release of memory.                               */
        /* -------------------------------------------------------------------- */
        //phase = -1;                 /* Release internal memory. */
        /* Release internal memory. */ 
        //int dummy = LAPACK.PARDISO(pt, maxfct, mnum, mtype, phase, n, ddum, ai, aj, idum, nrhs, iparm, msglvl, ddum, ddum, error);       
        return x;
    }
    
}
