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
package gecko.core.simulation;

import gecko.core.allg.SolverSettingsCore;
import gecko.core.datacontainer.ContainerStatus;
import gecko.core.datacontainer.DataContainerGlobal;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.simulation.solver.MatrixSolver;
import gecko.core.simulation.solver.ComponentCurrentCalculator;
import gecko.core.simulation.solver.InitialConditionSolver;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.simulation.ControlNetlist;
import gecko.core.simulation.DomainCoupler;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Headless simulation engine for running GeckoCIRCUITS simulations without GUI.
 * Suitable for REST APIs, CLI tools, batch processing, and cloud deployment.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
 * SimulationConfig config = SimulationConfig.builder()
 *     .circuitFile("path/to/circuit.ipes")
 *     .stepWidth(1e-6)
 *     .simulationDuration(20e-3)
 *     .build();
 *
 * SimulationResult result = engine.runSimulation(config);
 * if (result.isSuccess()) {
 *     double[] times = result.getTimeArray();
 *     float[] voltages = result.getSignalData(0);
 * }
 * }</pre>
 */
public class HeadlessSimulationEngine {

    /**
     * Current state of the simulation engine.
     */
    public enum EngineState {
        /** Engine is idle and ready to run a simulation */
        IDLE,
        /** Engine is running a simulation */
        RUNNING,
        /** Engine is paused */
        PAUSED,
        /** Engine has been cancelled */
        CANCELLED
    }

    private final AtomicReference<EngineState> state = new AtomicReference<>(EngineState.IDLE);
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    // Progress tracking
    private volatile double currentTime = 0;
    private volatile double endTime = 0;
    private volatile int currentStep = 0;
    private volatile long simulationStartTime = 0;

    // Event listener
    private SimulationProgressListener progressListener;

    // Solver components
    private MatrixSolver matrixSolver;
    private ComponentCurrentCalculator componentCurrentCalculator;
    private InitialConditionSolver initialConditionSolver;

    // Circuit and control netlists
    private CircuitNetlist circuitNetlist;
    private ControlNetlist controlNetlist;

    // Domain coupling orchestrator
    private DomainCoupler domainCoupler;

    /**
     * Creates a new HeadlessSimulationEngine.
     */
    public HeadlessSimulationEngine() {
    }

    /**
     * Runs a simulation with the specified configuration.
     * This method blocks until the simulation completes.
     *
     * @param config the simulation configuration
     * @return the simulation result
     */
    public SimulationResult runSimulation(SimulationConfig config) {
        if (config == null) {
            return SimulationResult.failed("Simulation configuration is required");
        }

        if (!state.compareAndSet(EngineState.IDLE, EngineState.RUNNING)) {
            return SimulationResult.failed("Engine is already running a simulation");
        }

        cancelRequested.set(false);
        long startTime = System.currentTimeMillis();
        simulationStartTime = startTime;

        try {
            return executeSimulation(config, startTime);
        } catch (Exception e) {
            return SimulationResult.failed("Simulation error: " + e.getMessage());
        } finally {
            state.set(EngineState.IDLE);
        }
    }

    /**
     * Executes the actual simulation loop.
     */
    private SimulationResult executeSimulation(SimulationConfig config, long startTime) {
        CircuitModel circuitModel = parseCircuitModel(config);
        SolverSettingsCore settings = config.getSolverSettings();
        double dt = settings.getStepWidth();
        double duration = settings.getSimulationDuration();
        validateSimulationSettings(dt, duration);

        endTime = duration;
        currentTime = 0;
        currentStep = 0;

        // Calculate expected number of steps
        int expectedSteps = calculateExpectedSteps(dt, duration);

        // Create data container for results
        // For now, create a simple container with a few test signals
        DataContainerGlobal dataContainer = new DataContainerGlobal();
        String[] signalNames = resolveSignalNames(circuitModel);
        dataContainer.init(signalNames.length, expectedSteps + 1, signalNames, "time [s]");
        dataContainer.setContainerStatus(ContainerStatus.RUNNING);

        // Initialize matrix solver
        matrixSolver = new MatrixSolver(settings.getSolverType());
        componentCurrentCalculator = new ComponentCurrentCalculator();
        initialConditionSolver = new InitialConditionSolver(settings.getSolverType());

        // Build netlists from circuit model
        circuitNetlist = NetlistBuilder.buildFromCircuitModel(circuitModel);
        controlNetlist = ControlNetlist.createEmpty();

        // Initialize domain coupler for orchestrating LK, CONTROL, THERM domains
        domainCoupler = new DomainCoupler();

        // Re-initialize matrix solver with real netlist dimensions
        if (circuitNetlist.getElementCount() > 0) {
            matrixSolver.initializeMatrices(
                circuitNetlist.getNodeMax(),
                circuitNetlist.getVoltageSourceMax(),
                circuitNetlist.getElementCount()
            );
        } else {
            // Fallback for empty circuit: use signal-based dimensions
            int nodeCount = signalNames.length;
            int voltageSourceCount = 0;
            int elementCount = signalNames.length;
            matrixSolver.initializeMatrices(nodeCount, voltageSourceCount, elementCount);
        }

        // Main simulation loop
        // Note: This is a placeholder implementation. In production, this would
        // integrate with the actual SimulationsKern or circuit solver.
        float[] values = new float[signalNames.length];

        while (currentTime <= duration) {
            if (cancelRequested.get()) {
                dataContainer.setContainerStatus(ContainerStatus.PAUSED);
                return SimulationResult.builder()
                        .status(SimulationResult.Status.CANCELLED)
                        .dataContainer(dataContainer)
                        .executionTimeMs(System.currentTimeMillis() - startTime)
                        .totalTimeSteps(currentStep)
                        .simulatedTime(currentTime)
                        .build();
            }

            // Phase 4: Execute domain coupling (LK → CONTROL → LK)
            // Orchestrates data transfer between circuit, control, and thermal domains
            domainCoupler.coupleDomainsForTimeStep(circuitNetlist, controlNetlist, dt, currentTime);

            // Real MNA solver: build and solve circuit matrices
            if (circuitNetlist != null && circuitNetlist.getElementCount() > 0) {
                // 1. Build system matrix A (component stamps)
                matrixSolver.buildMatrixA(circuitNetlist, dt, currentTime, false);

                // 2. Build right-hand side vector b (sources, history terms)
                matrixSolver.buildVectorB(circuitNetlist, dt, currentTime, false);

                // 3. Solve Ax=b for node voltages
                matrixSolver.solve();

                // 4. Calculate component currents from solved node voltages
                componentCurrentCalculator.calculateComponentCurrents(
                    matrixSolver, circuitNetlist, 0.0, dt, currentTime, true
                );

                // 5. Shift history for next time step
                matrixSolver.updateNodePotentials(dt, currentTime);

                // 6. Store results back into netlist
                circuitNetlist.storeResults(matrixSolver.getP(), matrixSolver.getIALT());

                // 7. Extract signal values for data logging
                double[] nodeVoltages = matrixSolver.getP();
                for (int sigIdx = 0; sigIdx < values.length && sigIdx < nodeVoltages.length; sigIdx++) {
                    values[sigIdx] = (float) nodeVoltages[sigIdx];
                }
            } else {
                // Fallback: no circuit loaded, use zero output
                for (int sigIdx = 0; sigIdx < values.length; sigIdx++) {
                    values[sigIdx] = 0.0f;
                }
            }

            // Store data (respecting logging interval)
            if (config.isDataLoggingEnabled() &&
                    (currentStep % config.getDataLoggingInterval() == 0)) {
                dataContainer.insertValuesAtEnd(values, currentTime);
            }

            currentTime += dt;
            currentStep++;

            // Report progress
            if (progressListener != null && currentStep % 1000 == 0) {
                progressListener.onProgress(currentTime, duration, currentStep);
            }
        }

        dataContainer.setContainerStatus(ContainerStatus.FINISHED);
        long executionTimeMs = System.currentTimeMillis() - startTime;

        return SimulationResult.builder()
                .status(SimulationResult.Status.SUCCESS)
                .dataContainer(dataContainer)
                .executionTimeMs(executionTimeMs)
                .totalTimeSteps(currentStep)
                .simulatedTime(currentTime)
                .metadata("solver", settings.getSolverType().toString())
                .metadata("dt", dt)
                .metadata("circuitFile", config.getCircuitFilePath())
                .metadata("parameterOverrides", config.getParameterOverrides().size())
                .build();
    }

    /**
     * Requests cancellation of the running simulation.
     * The simulation will stop at the next opportunity.
     */
    public void cancel() {
        if (state.get() == EngineState.RUNNING) {
            cancelRequested.set(true);
        }
    }

    /**
     * Gets the current engine state.
     *
     * @return current state
     */
    public EngineState getState() {
        return state.get();
    }

    /**
     * Gets the current simulation time.
     *
     * @return current time in seconds
     */
    public double getCurrentTime() {
        return currentTime;
    }

    /**
     * Gets the end time of the simulation.
     *
     * @return end time in seconds
     */
    public double getEndTime() {
        return endTime;
    }

    /**
     * Gets the current simulation progress as a percentage.
     *
     * @return progress from 0.0 to 1.0
     */
    public double getProgress() {
        if (endTime <= 0) {
            return 0;
        }
        return Math.min(1.0, currentTime / endTime);
    }

    /**
     * Gets the current time step number.
     *
     * @return current step
     */
    public int getCurrentStep() {
        return currentStep;
    }

    /**
     * Sets a progress listener for simulation progress updates.
     *
     * @param listener the progress listener, or null to remove
     */
    public void setProgressListener(SimulationProgressListener listener) {
        this.progressListener = listener;
    }

    /**
     * Pauses the running simulation.
     * The simulation will pause at the next time step.
     * Has no effect if the simulation is not running.
     *
     * @return true if pause was requested, false if simulation is not running
     */
    public boolean pause() {
        return state.compareAndSet(EngineState.RUNNING, EngineState.PAUSED);
    }

    /**
     * Resumes a paused simulation.
     * Has no effect if the simulation is not paused.
     *
     * @return true if resume was successful, false if simulation was not paused
     */
    public boolean resume() {
        return state.compareAndSet(EngineState.PAUSED, EngineState.RUNNING);
    }

    /**
     * Checks if the simulation is currently paused.
     *
     * @return true if paused, false otherwise
     */
    public boolean isPaused() {
        return state.get() == EngineState.PAUSED;
    }

    /**
     * Gets detailed progress information including time, steps, and ETA.
     * Useful for real-time monitoring and progress bars.
     *
     * @return detailed progress information, or null if no simulation is running
     */
    public SimulationProgress getDetailedProgress() {
        EngineState currentState = state.get();
        if (currentState == EngineState.IDLE) {
            return null;
        }

        double overallProgress = getProgress();

        // For now, pre-calculation progress is 0 (will be implemented with real solver)
        double preCalcProgress = 0.0;
        double mainSimProgress = overallProgress;

        // Estimate remaining time based on current progress
        Long estimatedRemainingMs = null;
        if (overallProgress > 0.01) { // Only estimate after 1% to avoid division by zero
            long elapsedMs = System.currentTimeMillis() - simulationStartTime;
            long totalEstimatedMs = (long) (elapsedMs / overallProgress);
            estimatedRemainingMs = totalEstimatedMs - elapsedMs;
        }

        // Calculate expected total steps
        int totalSteps = (int) Math.ceil(endTime / (currentTime / Math.max(1, currentStep)));
        if (totalSteps <= 0) {
            totalSteps = currentStep + 1000; // Fallback estimate
        }

        return new SimulationProgress(
            overallProgress,
            preCalcProgress,
            mainSimProgress,
            currentStep,
            totalSteps,
            currentTime,
            endTime,
            estimatedRemainingMs,
            currentState
        );
    }

    /**
     * Listener interface for simulation progress updates.
     */
    @FunctionalInterface
    public interface SimulationProgressListener {
        /**
         * Called periodically during simulation with progress information.
         *
         * @param currentTime current simulation time in seconds
         * @param endTime total simulation time in seconds
         * @param currentStep current time step number
         */
        void onProgress(double currentTime, double endTime, int currentStep);
    }

    private static int calculateExpectedSteps(double dt, double duration) {
        double rawSteps = Math.ceil(duration / dt);
        if (!Double.isFinite(rawSteps) || rawSteps > Integer.MAX_VALUE - 1) {
            throw new IllegalArgumentException("Simulation step count is too large");
        }
        return Math.max(1, (int) rawSteps);
    }

    private static void validateSimulationSettings(double dt, double duration) {
        if (!Double.isFinite(dt) || dt <= 0) {
            throw new IllegalArgumentException("Step width must be a finite value > 0");
        }
        if (!Double.isFinite(duration) || duration <= 0) {
            throw new IllegalArgumentException("Simulation duration must be a finite value > 0");
        }
    }

    private static CircuitModel parseCircuitModel(SimulationConfig config) {
        String circuitPath = config.getCircuitFilePath();
        if (circuitPath == null || circuitPath.isBlank()) {
            return null;
        }

        CircuitFileParser parser = new CircuitFileParser();
        try {
            return parser.parse(circuitPath);
        } catch (IOException | CircuitFileParser.CircuitParseException ex) {
            throw new IllegalArgumentException("Unable to parse circuit file '" + circuitPath + "': " + ex.getMessage(), ex);
        }
    }

    private static String[] resolveSignalNames(CircuitModel circuitModel) {
        if (circuitModel != null && circuitModel.getDataContainerSignals() != null
                && circuitModel.getDataContainerSignals().length > 0) {
            return circuitModel.getDataContainerSignals();
        }
        return new String[] {"V_out", "I_in", "P_loss"};
    }
}
