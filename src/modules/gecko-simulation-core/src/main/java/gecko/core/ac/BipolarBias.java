package gecko.core.ac;

import gecko.core.math.LUDecomposition;
import gecko.core.math.Matrix;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.function.DoubleFunction;
import java.util.function.Function;

/** Source stepping for nonlinear bipolar feedback circuits. */
final class BipolarBias {
    private static final int SOURCE_STEPS = 150;
    private static final int MAX_ITERATIONS = 160;
    private static final double RESIDUAL_LIMIT = 1e-10;

    record Equations(Complex[][] matrix, Complex[] rhs) {
        double[] residual(Complex[] bias, double[] weights) {
            double[] result = new double[bias.length];
            for (int row = 0; row < bias.length; row++) {
                double voltageOrCurrent = -rhs[row].real();
                for (int column = 0; column < bias.length; column++) {
                    voltageOrCurrent = Math.fma(matrix[row][column].real(), bias[column].real(), voltageOrCurrent);
                }
                result[row] = voltageOrCurrent * weights[row];
            }
            return result;
        }

        double[][] jacobian(double[] weights) {
            var result = new double[weights.length][weights.length];
            for (int row = 0; row < weights.length; row++) {
                for (int column = 0; column < weights.length; column++) {
                    result[row][column] = matrix[row][column].real() * weights[row];
                }
            }
            return result;
        }
    }

    private record Trial(Complex[] bias, Equations equations, double[] residual, double error) { }

    private BipolarBias() { }

    static Complex[] solve(int size, double[] weights, DoubleFunction<Function<Complex[], Equations>> equations) {
        var bias = new Complex[size];
        Arrays.fill(bias, Complex.ZERO);
        for (int stage = 0; stage <= SOURCE_STEPS; stage++) {
            double fraction = stage / (double) SOURCE_STEPS;
            var evaluate = equations.apply(fraction);
            var trial = trial(evaluate, weights, bias);
            for (int iteration = 0; trial.error > RESIDUAL_LIMIT && iteration < MAX_ITERATIONS; iteration++) {
                if (Thread.currentThread().isInterrupted()) throw new CancellationException("The AC bias solve was stopped.");
                var jacobian = trial.equations.jacobian(weights);
                var direction = linearSolve(jacobian, trial.residual);
                var next = improve(evaluate, weights, trial, direction);
                if (next == null) next = regularizedStep(evaluate, weights, trial, jacobian);
                if (next == null) break;
                trial = next;
            }
            if (!(trial.error <= RESIDUAL_LIMIT)) {
                throw new IllegalArgumentException("The bipolar DC bias did not converge at source fraction "
                        + fraction + " (weighted residual " + trial.error + ").");
            }
            double[] correction = linearSolve(trial.equations.jacobian(weights), trial.residual);
            if (correction == null) throw new IllegalArgumentException("The bipolar DC bias has a singular Jacobian.");
            if (stage == SOURCE_STEPS) {
                for (int i = 0; i < size; i++) {
                    if (Math.abs(correction[i]) > 1e-9 * (1 + trial.bias[i].abs())) {
                        throw new IllegalArgumentException("The bipolar DC bias voltage correction is too large.");
                    }
                }
            }
            bias = trial.bias;
        }
        return bias;
    }

    private static Trial trial(Function<Complex[], Equations> evaluate, double[] weights, Complex[] bias) {
        var equations = evaluate.apply(bias);
        var residual = equations.residual(bias, weights);
        double error = 0;
        for (double value : residual) error = Math.hypot(error, value);
        return new Trial(bias, equations, residual, error);
    }

    private static Trial improve(Function<Complex[], Equations> evaluate, double[] weights, Trial previous,
            double[] direction) {
        if (direction == null) return null;
        double scale = 1;
        for (int half = 0; half < 20; half++, scale /= 2) {
            var bias = new Complex[weights.length];
            for (int row = 0; row < bias.length; row++) {
                bias[row] = new Complex(Math.fma(scale, direction[row], previous.bias[row].real()), 0);
            }
            Trial next;
            try {
                next = trial(evaluate, weights, bias);
            } catch (IllegalArgumentException outOfRange) {
                continue;
            }
            if (next.error < previous.error * (1 - 1e-7)) return next;
        }
        return null;
    }

    private static Trial regularizedStep(Function<Complex[], Equations> evaluate, double[] weights, Trial previous,
            double[][] jacobian) {
        int n = weights.length;
        var normal = new double[n][n];
        var rhs = new double[n];
        for (int row = 0; row < n; row++) {
            for (int i = 0; i < n; i++) {
                rhs[i] = Math.fma(jacobian[row][i], previous.residual[row], rhs[i]);
                for (int j = 0; j <= i; j++) normal[i][j] = Math.fma(jacobian[row][i], jacobian[row][j], normal[i][j]);
            }
        }
        var diagonal = new double[n];
        for (int i = 0; i < n; i++) {
            diagonal[i] = normal[i][i];
            for (int j = 0; j < i; j++) normal[j][i] = normal[i][j];
        }
        for (double factor = 1e-12; factor <= 1; factor *= 10) {
            for (int i = 0; i < n; i++) normal[i][i] = diagonal[i] + factor * Math.max(1, diagonal[i]);
            var next = improve(evaluate, weights, previous, linearSolve(normal, rhs));
            if (next != null) return next;
        }
        return null;
    }

    private static double[] linearSolve(double[][] matrix, double[] residual) {
        var rhs = residual.clone();
        for (int i = 0; i < rhs.length; i++) rhs[i] = -rhs[i];
        var factor = new LUDecomposition(new Matrix(matrix));
        if (!factor.isNonsingular()) return null;
        var direction = factor.solve(new Matrix(rhs, rhs.length)).getColumnPackedCopy();
        for (double value : direction) if (!Double.isFinite(value)) return null;
        return direction;
    }
}
