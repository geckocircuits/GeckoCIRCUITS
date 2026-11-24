package gecko.core.preparation;

import gecko.core.allg.SolverSettingsCore;
import gecko.core.io.CircuitModel;

/** Fixed configuration with no live solver or CONTROL state. */
public final class PreparedCircuit {
    private final CircuitSnapshot circuit;
    private final SolverFeatures features;
    PreparedCircuit(CircuitSnapshot circuit, SolverFeatures features) {
        this.circuit = circuit;
        this.features = features;
    }
    /**
     * Creates a detached model for one execution.
     * Each run must create its own netlist, solver histories, and CONTROL state from this model.
     *
     * @return an independently writable copy of the prepared configuration
     */
    public CircuitModel newRunModel() { return circuit.executionModel(); }
    /**
     * Returns detached settings with all captured solver and simulation values.
     * Changes to this object cannot change the prepared circuit.
     *
     * @return detached settings for one execution
     */
    public SolverSettingsCore solverSettings() {
        var model = circuit.toModel();
        return features.applyTo(new SolverSettingsCore(model.getSolverType(), model.getTimeStep(),
                model.getSimulationDuration())
                .withPreCalculation(model.getPreSimulationTime(), model.getPreSimulationTimeStep())
                .withPauseTime(model.getPauseTime()));
    }
}
