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

import java.util.Random;

/**
 *
 * @author andy
 */
public class PardisoCachedMatrix extends AbstractCachedMatrix {
    private final int N;
    private SymmetricSparseMatrix sparseMatrix;

    public PardisoCachedMatrix(double[][] matrix) {
        super(matrix);
        N = matrix.length;
    }

    @Override
    int calculateMemoryRequirement() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    

    @Override
    void initLUDecomp() {                
        
        SymmetricDoubleSparseMatrix sysMatrix = new SymmetricDoubleSparseMatrix(N);

        for (int i = 0; i < N; i++) {
            for (int j = i; j < N; j++) {
                if (i == 0 || j == 0) {
                    if (i == 0 && j == 0) {
                        sysMatrix.setValue(i, j, 1);
                    }
                } else {
                    if (_originalMatrix[i][j] != 0) {
                        sysMatrix.setValue(i, j, _originalMatrix[i][j]);
                        if(_originalMatrix[j][i] != _originalMatrix[i][j]) {
                            System.err.println("nonsymmetric: " + i + " " + j + " " + _originalMatrix[i][j] + " " + _originalMatrix[j][i]);
                            throw new RuntimeException("Error in sparse matrix solver: system matrix is not symmetric!");
                        }
                    }
                }

            }
        }
        
        sparseMatrix = new SymmetricSparseMatrix(sysMatrix);

        sparseMatrix.factorize(sysMatrix, N, N);        
    }

    @Override
    public void deleteCache() {
        sparseMatrix = null;
    }


    @Override
    public double[] solve(double[] bVector) {
        return sparseMatrix.solve(bVector);
    }
}
