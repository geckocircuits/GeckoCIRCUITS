package gecko.core.allg;

/**
 * Chooses the linear solver behind the electrical MNA system. Null in the settings that carry it means the
 * dense LU, the default, and is the only behavior that is bit-identical to earlier releases.
 *
 * <p>The size measure is the node count: the number of circuit nodes without the ground node 0, which is
 * {@code CircuitNetlist.getNodeMax()}. Voltage-source and winding unknowns are not nodes.
 *
 * @param mode which solver to use
 * @param thresholdNodes in {@link Mode#AUTO}, the smallest node count that selects the sparse LU. At least 1.
 */
public record SparseSolverSettings(Mode mode, int thresholdNodes) {
    /** Default value of {@code thresholdNodes}. */
    public static final int DEFAULT_THRESHOLD_NODES = 100;

    /** Solver selection. */
    public enum Mode {
        /** Dense below {@code thresholdNodes}, sparse at or above it. */
        AUTO,
        /** Always dense LU. */
        DENSE,
        /** Always sparse LU. A failure of the sparse solver is reported, never retried with the dense LU. */
        SPARSE
    }

    /** Rejects a missing mode and a threshold below 1. */
    public SparseSolverSettings {
        if (mode == null) throw new IllegalArgumentException("Sparse solver mode cannot be null");
        if (thresholdNodes < 1) throw new IllegalArgumentException("sparseThresholdNodes must be at least 1");
    }

    /** Mode with the default threshold. */
    public SparseSolverSettings(Mode mode) { this(mode, DEFAULT_THRESHOLD_NODES); }

    /** Automatic selection with the default threshold of 100 nodes. */
    public SparseSolverSettings() { this(Mode.AUTO, DEFAULT_THRESHOLD_NODES); }

    /** @return true when a circuit of this many nodes (without ground) uses the sparse LU */
    public boolean usesSparse(int nodeCount) {
        return switch (mode) {
            case SPARSE -> true;
            case DENSE -> false;
            case AUTO -> nodeCount >= thresholdNodes;
        };
    }
}
