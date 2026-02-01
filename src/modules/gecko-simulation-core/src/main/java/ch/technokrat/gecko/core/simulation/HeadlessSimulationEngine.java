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
package ch.technokrat.gecko.core.simulation;

import ch.technokrat.gecko.core.allg.SolverSettingsCore;
import ch.technokrat.gecko.core.datacontainer.ContainerStatus;
import ch.technokrat.gecko.core.datacontainer.DataContainerGlobal;

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

    // Event listener
    private SimulationProgressListener progressListener;

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
        if (!state.compareAndSet(EngineState.IDLE, EngineState.RUNNING)) {
            return SimulationResult.failed("Engine is already running a simulation");
        }

        cancelRequested.set(false);
        long startTime = System.currentTimeMillis();

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
        SolverSettingsCore settings = config.getSolverSettings();
        double dt = settings.getStepWidth();
        double duration = settings.getSimulationDuration();
        endTime = duration;
        currentTime = 0;
        currentStep = 0;

        // Calculate expected number of steps
        int expectedSteps = (int) Math.ceil(duration / dt);

        // Create data container for results
        // For now, create a simple container with a few test signals
        DataContainerGlobal dataContainer = new DataContainerGlobal();
        String[] signalNames = {"V_out", "I_in", "P_loss"};
        dataContainer.init(signalNames.length, expectedSteps + 1, signalNames, "time [s]");
        dataContainer.setContainerStatus(ContainerStatus.RUNNING);

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

            // Placeholder: Generate test waveforms
            // In production, this would call the actual solver
            values[0] = (float) (5.0 * Math.sin(2 * Math.PI * 1000 * currentTime)); // V_out
            values[1] = (float) (0.5 * Math.sin(2 * Math.PI * 1000 * currentTime + 0.5)); // I_in
            values[2] = (float) Math.abs(values[0] * values[1]); // P_loss

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
                .build();
    }

    /**
     * Requests cancellation of the running simulation.
     * The simulation will stop at the next opportunity.
     */
    public void cancel() {
        cancelRequested.set(true);
        state.set(EngineState.CANCELLED);
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
}
