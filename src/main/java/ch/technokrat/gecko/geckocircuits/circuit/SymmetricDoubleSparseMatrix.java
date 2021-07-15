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

import java.util.HashMap;
import java.util.HashSet;

public class SymmetricDoubleSparseMatrix {

    public HashMap<Integer, HashSet<Integer>> rowEntries = new HashMap<Integer, HashSet<Integer>>();
    public HashMap<Integer, HashMap<Integer, Double>> rowEntriesValue = new HashMap<Integer, HashMap<Integer, Double>>();
    private final int _N;

    public SymmetricDoubleSparseMatrix(int N) {
        _N = N;

        // set the diagonal to non-zero values - this is required for the
        // pardiso sparse solver - otherwise the solver will not work!
        for (int i = 0; i < N; i++) {
            setValue(i, i, 1e-70);
        }
    }

    public void setValue(int row, int column, double value) {
        // assert row >= 0 : "row:  " + row;
        // assert column >= 0 : "column: " + column;
        assert row < _N;
        assert column < _N;
            if (column < row) {
                assert false;
                return;
            }

        HashSet<Integer> rowE = rowEntries.get(row);
        HashMap<Integer, Double> rowEValue = rowEntriesValue.get(row);
        if (rowE == null) {
            rowE = new HashSet<Integer>();
        }

        if (rowEValue == null) {
            rowEValue = new HashMap<Integer, Double>();
        }

        rowE.add(column);
        rowEntries.put(row, rowE);

        rowEValue.put(column, value);
        rowEntriesValue.put(row, rowEValue);

    }

    public double getValue(int row, int column) {
        HashMap<Integer, Double> rowValues = rowEntriesValue.get(row);
        if (rowValues == null) {
            return 0;
        } else {
            if (rowValues.containsKey(column)) {
                return rowValues.get(column);
            } else {
                return 0;
            }
        }
    }

    void removeZeroEntry(int row, int column) {

        if (row == column) {// in symmetric matrices, the diagonal value MUST be present,
                // even if it is "0".
                return;
        }

        if (rowEntriesValue.containsKey(row)) {
            HashMap<Integer, Double> roweValues = rowEntriesValue.get(row);
            if (roweValues.containsKey(column)) {
                roweValues.remove(roweValues.get(column));
                HashSet<Integer> rows = rowEntries.get(row);
                HashMap<Integer, Double> rowsValue = rowEntriesValue.get(row);
                rowsValue.remove(column);
                rows.remove(column);
                if (rows.isEmpty()) {
                    rowEntries.remove(rows);
                    rowEntriesValue.remove(rowsValue);
                }
            }
        }
    }

    public void print() {

        System.out.println("----------------------");
        for (int i = 0; i < _N; i++) {
            System.out.print(i + "\t");
        }

        System.out.println("\n [");

        for (int i = 0; i < _N; i++) {
            for (int j = 0; j < _N; j++) {
                System.out.print(getValue(i, j) + ",\t");
            }
            System.out.println(";");
        }
        System.out.println("]");
    }

    public int getNumberOfNonZeros() {

        int counter = 0;
        for (int i = 0; i < _N; i++) {
            if (rowEntries.containsKey(i)) {
                for (int j : rowEntries.get(i)) {
                    counter++;
                }
            }
        }

        return counter;
    }

    int getMatrixSize() {
        return _N;
    }
}
