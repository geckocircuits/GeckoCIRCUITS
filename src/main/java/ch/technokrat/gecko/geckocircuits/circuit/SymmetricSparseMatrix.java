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


import java.util.ArrayList;
import java.util.Collections;

/**
 * Sparse matrix formulation as defined in the csr format
 * See Intel mkl user's guide for more details
 * 
 */
public class SymmetricSparseMatrix {

    public double[] Acsr;
    double[] AcsrComplex;
    public int[] AI, AIT;
    public int[] AJ, AJT;
    public int[][] columnRowIndices;
    public double[] Adns;
    private Paradiso _paradiso;
    private final int _N;

    
    public int getMatrixSize() {
        return _N;
    }
        

    /**
     * 
     * @param fullComplexMatrix
     * @param rows
     * @param cols
     * @param rhs
     * @return
     */
    public void factorize(final SymmetricDoubleSparseMatrix matrix, final int rows, final int cols) {        
        _paradiso = new Paradiso();                        
        Paradiso.factorize(Adns, AI, AJ, _N, -2, _paradiso);                        
    }

    public double[] solve(double[] rhs) {        
        return Paradiso.solve(Adns, AI, AJ, rhs, rhs.length, -2, 1, _paradiso);        
    }

    
    /**
     * constructs a sparse matrix from the byte[][] representation
     * @param fullMatrix
     */
    public SymmetricSparseMatrix(final SymmetricDoubleSparseMatrix matrix) {        
        _N = matrix.getMatrixSize();
        int nzmax = 0;
            
        // determine number of nonzero entries:

        nzmax = matrix.getNumberOfNonZeros();
        Adns = new double[nzmax];
        AI = new int[_N + 1];
        AJ = new int[nzmax];

        int index = 0;

        //for (int i = 0; i < matrix.getRowNumber(); i++) {
        //    if (matrix.rowEntries.containsKey(i)) {
        //        for (int j : matrix.rowEntries.get(i)) {
        for (int i = 0; i < _N; i++) {
            if (matrix.rowEntries.containsKey(i)) {
                int counter = -1;
                ArrayList<Integer> rowEntries = new ArrayList<Integer>();
                rowEntries.addAll(matrix.rowEntries.get(i));
                Collections.sort(rowEntries);

                for (int j : rowEntries) {
                    assert j > counter;
                    counter = j;
                    double re = matrix.getValue(i, j);
                    if (re != 0) {
                        Adns[index] = re;
                        int column = j + 1;
                        int row = i;
                        AJ[index] = column;

                        if (AI[row] == 0) {
                            AI[row] = index + 1;
                        }
                        index++;
                    }
                }
            }
        }
        AI[AI.length - 1] = nzmax + 1;        
    }
                
}
