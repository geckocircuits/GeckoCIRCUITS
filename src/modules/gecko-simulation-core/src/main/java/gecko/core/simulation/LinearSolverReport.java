package gecko.core.simulation;

import gecko.core.simulation.solver.MatrixSolver;

/**
 * Which linear solver an electrical session actually used, read from the live solvers.
 *
 * @param backend the main MNA solver's backend
 * @param matrixOrder the order of the MNA matrix
 * @param storedCoefficients coefficient slots held by the main solver: order squared when dense
 * @param storageBytes estimated primitive-array bytes of the main solver's coefficient storage, not total heap
 * @param factorizations LU computations of the main solver so far
 * @param companionBackend the adaptive companion solver's backend, or null when no companion exists
 * @param companionStoredCoefficients coefficient slots of the companion, or 0 when none exists
 * @param initializationBackend backend of the solver that computed the initial state, or null when none was needed
 * @param initializationStoredCoefficients coefficient slots of that solver at the end of initialization
 * @param factorization how the main solver factors a sparse matrix, null when dense or before its first factorization
 * @param companionFactorization the same for the companion solver
 * @param initializationFactorization the same for the solver that computed the initial state
 */
public record LinearSolverReport(MatrixSolver.Backend backend, int matrixOrder, long storedCoefficients,
        long storageBytes, int factorizations, MatrixSolver.Backend companionBackend,
        long companionStoredCoefficients, MatrixSolver.Backend initializationBackend,
        long initializationStoredCoefficients, MatrixSolver.SparseFactorization factorization,
        MatrixSolver.SparseFactorization companionFactorization,
        MatrixSolver.SparseFactorization initializationFactorization) { }
