package gecko.core.ac;

import java.util.Arrays;

/** Numerical rank test for the real block of a complex AC system. */
final class EquilibratedRank {
    private static final int NO_EDGE = Integer.MAX_VALUE;

    private EquilibratedRank() { }

    static void requireFullRank(double[][] original) {
        double[][] scaled = equilibrate(original);
        int order = scaled.length;
        double growth = 1;
        for (int step = 0; step < order; step++) {
            int pivotRow = step;
            int pivotColumn = step;
            double largest = 0;
            for (int row = step; row < order; row++) {
                for (int column = step; column < order; column++) {
                    double magnitude = Math.abs(scaled[row][column]);
                    if (magnitude > largest) {
                        largest = magnitude;
                        pivotRow = row;
                        pivotColumn = column;
                    }
                }
            }
            // Compare the largest available pivot to roundoff on the equilibrated matrix.
            growth = Math.max(growth, largest);
            if (largest <= order * Math.ulp(1.0) * growth) throw singular();

            double[] pivot = scaled[pivotRow];
            scaled[pivotRow] = scaled[step];
            scaled[step] = pivot;
            if (pivotColumn != step) {
                for (int row = step; row < order; row++) {
                    double displaced = scaled[row][step];
                    scaled[row][step] = scaled[row][pivotColumn];
                    scaled[row][pivotColumn] = displaced;
                }
            }

            for (int row = step + 1; row < order; row++) {
                double[] target = scaled[row];
                double multiplier = target[step] / pivot[step];
                if (multiplier == 0) continue;
                for (int column = step + 1; column < order; column++) {
                    target[column] = Math.fma(-multiplier, pivot[column], target[column]);
                }
            }
        }
    }

    private static double[][] equilibrate(double[][] original) {
        int order = original.length;
        int[][] costs = new int[order][order];
        for (int row = 0; row < order; row++) {
            for (int column = 0; column < order; column++) {
                double value = original[row][column];
                costs[row][column] = value == 0 ? NO_EDGE : -binaryExponent(value);
            }
        }
        // The assignment dual gives a unit-order matching and bounds every scaled entry.
        int[] rowScale = new int[order + 1], columnScale = new int[order + 1];
        assignScales(costs, rowScale, columnScale);
        double[][] scaled = new double[order][order];
        for (int row = 0; row < order; row++) {
            for (int column = 0; column < order; column++) {
                scaled[row][column] = Math.scalb(original[row][column], rowScale[row + 1] + columnScale[column + 1]);
            }
        }
        return scaled;
    }

    private static int binaryExponent(double value) {
        int exponent = Math.getExponent(value);
        return exponent == Double.MIN_EXPONENT - 1 ? Math.getExponent(Math.scalb(value, 54)) - 54 : exponent;
    }

    /** Finds assignment potentials with augmenting paths in the Hungarian algorithm. */
    private static void assignScales(int[][] costs, int[] rowScale, int[] columnScale) {
        int order = costs.length;
        var matchedRow = new int[order + 1];
        var predecessor = new int[order + 1];
        for (int row = 1; row <= order; row++) {
            var slack = new int[order + 1];
            var visited = new boolean[order + 1];
            Arrays.fill(slack, NO_EDGE);
            int current = 0;
            matchedRow[0] = row;
            do {
                visited[current] = true;
                int source = matchedRow[current];
                int adjustment = NO_EDGE;
                int next = 0;
                for (int column = 1; column <= order; column++) {
                    if (visited[column]) continue;
                    int cost = costs[source - 1][column - 1];
                    if (cost != NO_EDGE) {
                        int reduced = cost - rowScale[source] - columnScale[column];
                        if (reduced < slack[column]) {
                            slack[column] = reduced;
                            predecessor[column] = current;
                        }
                    }
                    if (slack[column] < adjustment) {
                        adjustment = slack[column];
                        next = column;
                    }
                }
                if (adjustment == NO_EDGE) throw singular();
                for (int column = 0; column <= order; column++) {
                    if (visited[column]) {
                        rowScale[matchedRow[column]] += adjustment;
                        columnScale[column] -= adjustment;
                    } else if (slack[column] != NO_EDGE) {
                        slack[column] -= adjustment;
                    }
                }
                current = next;
            } while (matchedRow[current] != 0);
            do {
                int previous = predecessor[current];
                matchedRow[current] = matchedRow[previous];
                current = previous;
            } while (current != 0);
        }
    }

    private static IllegalArgumentException singular() {
        return new IllegalArgumentException("The AC network is numerically singular.");
    }
}
