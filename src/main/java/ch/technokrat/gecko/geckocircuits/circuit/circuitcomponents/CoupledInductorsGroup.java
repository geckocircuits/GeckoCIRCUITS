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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.allg.SolverType;
import java.util.List;

public class CoupledInductorsGroup implements AStampable, CurrentCalculatable {

    private double[][] inductanceMatrix;
    private double[][] _inverseInductanceMatrix = null;
    private int[] globalAdjIndicesPlus; // some kind of "sparse" storage, for faster matrix-vector-products
    private int[] globalAdjIndicesMinus; // some kind of "sparse" storage, for faster matrix-vector-products
    private List<InductorCouplingCalculator> _allInductors;
    private double[] pOld; //for the trapezoidal solver - ugly, I know, fix later

    CoupledInductorsGroup(final List<InductorCouplingCalculator> _inductors, final List<MutualCouplingCalculator> _mutualCouplings, int N) {
        initInductanceMatrix(_inductors, _mutualCouplings, N);
        _allInductors = _inductors;
        for (InductorCouplingCalculator ind : _allInductors) {
            ind.setGroup(this);
        }
    }

    public void initInductanceMatrix(final List<InductorCouplingCalculator> allInductors, final List<MutualCouplingCalculator> couplings, int N) {
        inductanceMatrix = null;
        if (allInductors.size() == 0) {
            return;
        }

        _allInductors = allInductors;

        inductanceMatrix = new double[allInductors.size()][allInductors.size()];

        for (int i = 0; i < allInductors.size(); i++) {
            InductorCouplingCalculator ind = allInductors.get(i);
            inductanceMatrix[i][i] = ind.getInductance();
        }

        initAdjMatrices(N, allInductors, couplings);

        for (MutualCouplingCalculator mut : couplings) {
            mut.stampInductanceMatrix(inductanceMatrix, allInductors);
        }

        int NN = inductanceMatrix.length;
        _inverseInductanceMatrix = choleskyInverse(inductanceMatrix, NN);
    }

    private void initAdjMatrices(int N, final List<InductorCouplingCalculator> allInductors, final List<MutualCouplingCalculator> couplings) {

        globalAdjIndicesPlus = new int[allInductors.size()];
        globalAdjIndicesMinus = new int[allInductors.size()];


        for (int i = 0; i < allInductors.size(); i++) {
            globalAdjIndicesPlus[i] = allInductors.get(i).getTerminalIndex(0);
            globalAdjIndicesMinus[i] = allInductors.get(i).getTerminalIndex(1);
        }
    }

    @Override
    public void stampMatrixA(double[][] matrix, double dt) {

        double[][] tmp0 = new double[_inverseInductanceMatrix.length][matrix.length];

        // Matrix-Vector product L * Adj (Adj is incidence matrix)
        for (int k = 0; k < _inverseInductanceMatrix.length; k++) {
            int indexPlus = globalAdjIndicesPlus[k];
            int indexMinus = globalAdjIndicesMinus[k];
            for (int i = 0; i < _inverseInductanceMatrix.length; i++) {
                tmp0[i][indexPlus] += _inverseInductanceMatrix[i][k];
                tmp0[i][indexMinus] -= _inverseInductanceMatrix[i][k];
            }
        }


        double[][] tmp2 = new double[matrix.length][matrix.length];

        // matrix-Vector-Product matrix += AdjT * L * Adj
        for (int k = 0; k < tmp0.length; k++) {
            int indexPlus = globalAdjIndicesPlus[k];
            int indexMinus = globalAdjIndicesMinus[k];
            for (int j = 0; j < matrix.length; j++) {
                tmp2[indexPlus][j] += tmp0[k][j];
                tmp2[indexMinus][j] -= tmp0[k][j];
            }
        }
        
        double solverCoeff = 1.0; //coefficient to multiply dt*tmp2, depends on solver type used
        
        SolverType solver = getSolverType();
        if (solver == SolverType.SOLVER_BE) {
            solverCoeff = 1.0;
        }
        else if (solver == SolverType.SOLVER_TRZ) {
            solverCoeff = 0.5;
        }
        else if (solver == SolverType.SOLVER_GS) {
            solverCoeff = 2.0 / 3.0;
        }
        
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                matrix[i][j] += solverCoeff * dt * tmp2[i][j];
            }
        }

    }

     public void calculateCurrent(double[] p, double dt, double t) {

        SolverType solver = getSolverType();
        
        if (pOld == null)
            pOld = new double[p.length]; //ugly, should be removed later!
           
        int n = inductanceMatrix.length;
        double[] AP2 = new double[n];
        for (int i = 0; i < n; i++) {
            AP2[i] += p[globalAdjIndicesPlus[i]];
            AP2[i] -= p[globalAdjIndicesMinus[i]];
        }
//            this here is easier to understand, but the above one is faster
//            for (int i = 0; i < n; i++) {
//                for (int j = 0; j < p.length; j++) {
//                    AP2[i] += adjGlobal.get(i, j) * p[j];
//                }
//            }

        double[] oldAP2 = new double[n];
        if (solver == SolverType.SOLVER_TRZ) {
            for (int i = 0; i < n; i++) {
                oldAP2[i] += pOld[globalAdjIndicesPlus[i]];
                oldAP2[i] -= pOld[globalAdjIndicesMinus[i]];
            }
        }
        
        for (int i = 0; i < _allInductors.size(); i++) {
            InductorCouplingCalculator ind = _allInductors.get(i);
            double outValue = 0;
            for (int j = 0; j < n; j++) {
                outValue += _inverseInductanceMatrix[i][j] * AP2[j];
                if (solver == SolverType.SOLVER_TRZ) {
                    outValue += _inverseInductanceMatrix[i][j] * oldAP2[j];
                }
            }

            ind.addNewCurrent(dt * outValue);
        }
        
        if (solver == SolverType.SOLVER_TRZ)
             pOld = p; //we need the old potential for the trapezoidal solver - yes, this is UGLY and should be replaced with a better implementation
        

    }

    public static double[][] choleskyInverse(final double[][] a, final int n) {
        double[][] el = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                el[i][j] = a[i][j];
            }
        }

        double sum = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                sum = el[i][j];
                for (int k = i - 1; k >= 0; k--) {
                    sum -= el[i][k] * el[j][k];
                }
                if (i == j) {
                    if (sum <= 0.0) {
                        throw new IllegalArgumentException("matrix is not SPD!");
                    }
                    el[i][j] = Math.sqrt(sum);
                } else {
                    el[j][i] = sum / el[i][i];
                }

            }
        }
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                el[j][i] = 0;
            }
        }
        double[][] ainv = new double[n][n];
        // -------- inversion
        sum = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= i; j++) {
                sum = i == j ? 1 : 0;

                for (int k = i - 1; k >= j; k--) {
                    sum -= el[i][k] * ainv[j][k];
                }

                ainv[j][i] = sum / el[i][i];
            }
        }


        for (int i = n - 1; i >= 0; i--) {
            for (int j = 0; j <= i; j++) {
                sum = i < j ? 0 : ainv[j][i];
                for (int k = i + 1; k < n; k++) {
                    sum -= el[k][j] * ainv[j][k];
                }
                ainv[i][j] = ainv[j][i] = sum / el[i][i];
            }
        }

        return ainv;
    }
    
    private SolverType getSolverType() {
        return _allInductors.get(0)._solverType;
    }
    
    //to make the TRZ solver work - yes, ugly, should be redone better!
    public double getLPproductForTRZ(InductorCouplingCalculator inductor) {
        
        int n = inductanceMatrix.length;
        double[] oldAP2 = new double[n];
        if (pOld != null) {
            for (int i = 0; i < n; i++) {
                oldAP2[i] += pOld[globalAdjIndicesPlus[i]];
                oldAP2[i] -= pOld[globalAdjIndicesMinus[i]];
            }   
        }
        else { //QUESTION: this should somehow perhaps be initialized to the initial value of the voltages??? -> or do we never set a non-zero initial voltage for coupled inductors?
            for (int i = 0; i < n; i++) {
                oldAP2[i] += 0.0;
                oldAP2[i] -= 0.0;
            }
        }
        
        for (int i = 0; i < _allInductors.size(); i++) {
            InductorCouplingCalculator ind = _allInductors.get(i);
            if (ind == inductor) {
                double outValue = 0;
                for (int j = 0; j < n; j++) {
                    outValue += _inverseInductanceMatrix[i][j] * oldAP2[j];
                }
                return outValue;
            }
        }
        return 0.0;        
    }
    
    @Override 
    public double getCurrent() {
        return 0;
    }               
}