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

import ch.technokrat.gecko.geckocircuits.datacontainer.ShortArrayCache;
import ch.technokrat.gecko.geckocircuits.datacontainer.ShortMatrixCache;

public final class CachedMatrix extends AbstractCachedMatrix {

    private double[][] _LUDecomp;
    private boolean[][] _luMatrixNotZero;
    private short[][] rowEntryIndices;
    private short[] numberOfRowEntries;
    private short _nn;
    private short[] _piv;
    private short[][] _lowerLUIndices;
    private short[][] _upperLUIndices;

    private double[] _XCol;

    private double[] LUcolj;

    public CachedMatrix(final double[][] matrix) {
        super(matrix);
    }

    public void initLUDecomp() {
        double[][] tmpMatrix = _originalMatrix;
        _originalMatrix = new double[_originalMatrix.length][_originalMatrix.length];

        // make a local copy of the original matrix
        // this is necessary, since the original matrix is overwritten by the LU-decomposition
        for (int i = 0; i < _originalMatrix.length; i++) {
            System.arraycopy(tmpMatrix[i], 0, _originalMatrix[i], 0, _originalMatrix[0].length);
        }

        if (_originalMatrix.length > 50) { // at some point, the overhead with the
            // sparse calculation is negligible in comparison to the speed-up...
            doLUDecompositionSparse(_originalMatrix);
        } else {
            doLUDecomposition(_originalMatrix);
        }

        calculateLowerSparseLUDecompositionIndices();
        calculateUpperSparseLUDecompositionIndices();
    }

    @Override
    int calculateMemoryRequirement() {
        return 2 * _originalMatrix.length * _originalMatrix.length * Double.SIZE / 8;
    }

    public void initHelperArrays(final int size) {
        if (LUcolj == null || _nn != size) {
            LUcolj = new double[size];
            _LUDecomp = new double[size][size];
            _piv = new short[size];
            _XCol = new double[_piv.length + 1];
        }
    }

    public void doLUDecomposition(final double[][] aMatrix) {

//        boolean isSymmetric = true;
//        boolean isStructureSymmetric = true;        
        // Use a "left-looking", dot-product, Crout/Doolittle algorithm.
        assert aMatrix.length == aMatrix[0].length;

        initHelperArrays(aMatrix[0].length - 1);
        _nn = (short) (aMatrix[0].length - 1);

        for (int i = 0; i < _nn; i++) {
            for (int j = 0; j < _nn; j++) {
                _LUDecomp[i][j] = aMatrix[i + 1][j + 1];
            }
        }

        for (short i = 0; i < _nn; i++) {
            _piv[i] = i;
        }
        int pivsign = 1;
        double[] LUrowi;

        // Outer loop.
        for (int j = 0; j < _nn; j++) {
            // Make a copy of the j-th column to localize references.
            for (int i = 0; i < _nn; i++) {
                LUcolj[i] = _LUDecomp[i][j];
            }

            // Apply previous transformations.
            for (int i = 0; i < _nn; i++) {
                LUrowi = _LUDecomp[i];

                // Most of the time is spent in the following dot product.
                int kmax = Math.min(i, j);
                double s = 0.0;
                for (int k = 0; k < kmax; k++) {
                    s += LUrowi[k] * LUcolj[k];
                }

                LUrowi[j] = LUcolj[i] -= s;
            }

            // Find pivot and exchange if necessary.
            int p = j;
            for (int i = j + 1; i < _nn; i++) {
                if (Math.abs(LUcolj[i]) > Math.abs(LUcolj[p])) {
                    p = i;
                }
            }
            if (p != j) {
                for (int k = 0; k < _nn; k++) {
                    double t = _LUDecomp[p][k];
                    _LUDecomp[p][k] = _LUDecomp[j][k];
                    _LUDecomp[j][k] = t;
                }
                short k = _piv[p];
                _piv[p] = _piv[j];
                _piv[j] = k;
                pivsign = -pivsign;
            }

            // Compute multipliers.
            if (j < _nn & _LUDecomp[j][j] != 0.0) {
                for (int i = j + 1; i < _nn; i++) {
                    _LUDecomp[i][j] /= _LUDecomp[j][j];
                }
            }
        }

        for (int j = 0; j < _LUDecomp.length; j++) {
            if (_LUDecomp[j][j] == 0) {
                throw new RuntimeException("Matrix is singular.");
            }
        }

        // shift piv + 1
        for (int i = 0; i < _piv.length; i++) {
            _piv[i]++;
        }
    }

    private void doLUDecompositionSparse(final double[][] aMatrix) {
        // Use a "left-looking", dot-product, Crout/Doolittle algorithm.
        assert aMatrix.length == aMatrix[0].length;

        initHelperArrays(aMatrix[0].length - 1);
        _nn = (short) (aMatrix[0].length - 1);

        _luMatrixNotZero = new boolean[_nn][_nn];

        rowEntryIndices = ShortMatrixCache.getCachedMatrix(_nn, _nn);
        numberOfRowEntries = new short[_nn];

        for (int i = 0; i < _nn; i++) {
            for (int j = i; j < _nn; j++) {
                rowEntryIndices[i][j] = 0;
                rowEntryIndices[j][i] = 0;
                double value1 = aMatrix[i + 1][j + 1];
                double value2 = aMatrix[j + 1][i + 1];
                setLUMatrixValue(i, j, value1);
                setLUMatrixValue(j, i, value2);
            }
        }

        for (short i = 0; i < _nn; i++) {
            _piv[i] = i;
        }
        int pivsign = 1;

        // Outer loop.
        for (int j = 0; j < _nn; j++) {
            // Make a copy of the j-th column to localize references.            
            for (int i = 0; i < _nn; i++) {
                LUcolj[i] = _LUDecomp[i][j];
            }

            // Apply previous transformations.            
            for (int i = 0; i < _nn; i++) {
                double[] LUrowi = _LUDecomp[i];
                // Most of the time is spent in the following dot product.

                int kmax = Math.min(i, j);

                double s = 0.0;
                //for (int k = 0; k < kmax; k++) {
                short[] entryindices = rowEntryIndices[i];
                int numberOfEntries = numberOfRowEntries[i];
                for (short l = 0; l < numberOfEntries; l++) {
                    int k = entryindices[l];
                    if (k >= kmax) {
                        continue;
                    }
//                    if(k >= kmax) {
//                        break;
//                    }

                    double rowValue = LUrowi[k];
                    s += rowValue * LUcolj[k];
                }
                LUcolj[i] -= s;
                setLUMatrixValue(i, j, LUcolj[i]);
            }

            // Find pivot and exchange if necessary.
            int p = j;
            for (int i = j + 1; i < _nn; i++) {
                if (Math.abs(LUcolj[i]) > Math.abs(LUcolj[p])) {
                    p = i;
                }
            }
            if (p != j) {
                swapColumns(p, j);

                short k = _piv[p];
                _piv[p] = _piv[j];
                _piv[j] = k;
                pivsign = -pivsign;
            }

            // Compute multipliers.
            if (_LUDecomp[j][j] != 0.0) {
                for (int i = j + 1; i < _nn; i++) {
                    _LUDecomp[i][j] /= _LUDecomp[j][j];
                }
            }
        }

        for (int j = 0; j < _LUDecomp.length; j++) {
            if (_LUDecomp[j][j] == 0) {
                throw new RuntimeException("Matrix is singular.");
            }
        }

        // shift piv + 1
        for (int i = 0; i < _piv.length; i++) {
            _piv[i]++;
        }

        ShortMatrixCache.recycleMatrix(rowEntryIndices);

        //System.out.println("conuter:  " + counter + " " + counter2);        
    }

    private void setLUMatrixValue(int i, int j, double value) {
        if (value != 0) {
            if (!_luMatrixNotZero[i][j]) {
                rowEntryIndices[i][numberOfRowEntries[i]] = (short) j;
                numberOfRowEntries[i]++;
                _luMatrixNotZero[i][j] = true;
            }
        }
        _LUDecomp[i][j] = value;
    }

    private void swapColumns(int p, int j) {
        double[] t = _LUDecomp[p];

        short[] exchange = rowEntryIndices[p];
        rowEntryIndices[p] = rowEntryIndices[j];
        rowEntryIndices[j] = exchange;

        boolean[] boolExchange = _luMatrixNotZero[p];
        _luMatrixNotZero[p] = _luMatrixNotZero[j];
        _luMatrixNotZero[j] = boolExchange;

        short exchangeNumber = numberOfRowEntries[p];
        numberOfRowEntries[p] = numberOfRowEntries[j];
        numberOfRowEntries[j] = exchangeNumber;

        _LUDecomp[p] = _LUDecomp[j];
        _LUDecomp[j] = t;
    }

    public double[] solve(final double[] bVector) {
        for (int i = 0; i < _piv.length; i++) {
            _XCol[i + 1] = bVector[_piv[i]];
        }
        // Solve L*Y = B(piv,:)
        for (int k = 0; k < _nn; k++) {
            for (int i = 0, nMax = _lowerLUIndices[k].length; i < nMax; i++) {
                final int index = _lowerLUIndices[k][i];
                _XCol[index + 1] -= _XCol[k + 1] * _LUDecomp[index][k];
            }
        }

        // Solve U*X = Y;
        for (int k = _nn - 1; k >= 0; k--) {
            _XCol[k + 1] /= _LUDecomp[k][k];
            for (int i = 0, nMax = _upperLUIndices[k].length; i < nMax; i++) {
                final int index = _upperLUIndices[k][i];
                _XCol[index + 1] -= _XCol[k + 1] * _LUDecomp[index][k];
            }
        }       

        return _XCol;
    }

    private void calculateLowerSparseLUDecompositionIndices() {
        short[][] lowerLUIndices = new short[_LUDecomp.length][];

        for (short k = 0; k < _nn; k++) {
            int counter = 0;
            for (int i = k + 1; i < _nn; i++) {
                if (_LUDecomp[i][k] != 0) {
                    counter++;
                }
            }

            lowerLUIndices[k] = new short[counter];//ShortArrayCache.getCachedArray(counter);

            for (short i = (short) (k + 1), tmpCounter = 0; i < _nn; i++) {
                if (_LUDecomp[i][k] != 0) {
                    lowerLUIndices[k][tmpCounter] = i;
                    tmpCounter++;
                }
            }

        }
        _lowerLUIndices = lowerLUIndices;
    }

    private void calculateUpperSparseLUDecompositionIndices() {

        short[][] upper = new short[_LUDecomp.length][];

        for (int k = _nn - 1; k >= 0; k--) {
            int counter = 0;

            for (int i = 0; i < k; i++) {
                if (_LUDecomp[i][k] != 0) {
                    counter++;
                }
            }

            upper[k] = new short[counter];//ShortArrayCache.getCachedArray(counter);

            for (short i = 0, tmpCounter = 0; i < k; i++) {
                if (_LUDecomp[i][k] != 0) {
                    upper[k][tmpCounter] = i;
                    tmpCounter++;
                }
            }
        }
        _upperLUIndices = upper;
    }

    public void deleteCache() {
        for (short[] toRecycle : _lowerLUIndices) {
            ShortArrayCache.recycleArray(toRecycle);
        }
        for (short[] toRecycle : _upperLUIndices) {
            ShortArrayCache.recycleArray(toRecycle);
        }
    }
}
