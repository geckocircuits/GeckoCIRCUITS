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
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.preparation.PreparedCircuit;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.circuit.netlist.CircuitNetlist;

import java.io.IOException;
import java.util.function.BooleanSupplier;
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
    private final Object pauseMonitor = new Object();

    // Progress tracking
    private volatile double currentTime = 0;
    private volatile double endTime = 0;
    private volatile double overallEndTime = 0;
    private volatile boolean hasPreCalculation;
    private volatile SimulationSession.Phase phase = SimulationSession.Phase.MAIN;
    private volatile int currentStep = 0;
    private volatile int expectedStepCount = 0;
    private volatile long simulationStartTime = 0;

    // Event listener
    private SimulationProgressListener progressListener;

    // Step trace listener for golden reference comparison
    private StepTraceListener stepTraceListener;

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
        return runSimulation(config, () -> false);
    }

    /**
     * Runs a simulation with a durable cancellation signal from its owning job.
     * The signal can be set before the engine starts and is checked before each step.
     *
     * @param config the simulation configuration
     * @param cancellationSignal true when the owning job requests cancellation
     * @return the simulation result
     */
    public SimulationResult runSimulation(SimulationConfig config, BooleanSupplier cancellationSignal) {
        if (config == null) return SimulationResult.failed("Simulation configuration is required");
        return runOwnedSimulation(start -> executeSimulation(config, start, cancellationSignal));
    }

    /**
     * Runs an immutable prepared description with independent numerical and CONTROL state.
     * Logging options and cancellation do not change its circuit configuration.
     * This method blocks until completion. Numerical failure remains possible after successful preflight.
     * Use separate engine instances for concurrent runs of the same Prepared circuit.
     *
     * @param circuit the immutable execution description from preparation
     * @param options the data logging choices for this run
     * @param cancellationSignal true when the owning job requests cancellation
     * @return the successful, cancelled, or failed run result
     */
    public SimulationResult runSimulation(PreparedCircuit circuit, ExecutionOptions options,
            BooleanSupplier cancellationSignal) {
        return runOwnedSimulation(start -> {
            var settings = circuit.solverSettings();
            var config = SimulationConfig.builder().solverSettings(settings)
                    .enableDataLogging(options.loggingEnabled()).dataLoggingInterval(options.loggingInterval()).build();
            return executeSimulation(config, start, cancellationSignal, circuit.newRunModel(), settings);
        });
    }

    private SimulationResult runOwnedSimulation(java.util.function.LongFunction<SimulationResult> execution) {
        synchronized (pauseMonitor) {
            if (!state.compareAndSet(EngineState.IDLE, EngineState.RUNNING)) {
                return SimulationResult.failed("Engine is already running a simulation");
            }
            cancelRequested.set(false);
            currentTime = 0;
            currentStep = 0;
            endTime = 0;
            overallEndTime = 0;
            hasPreCalculation = false;
            phase = SimulationSession.Phase.MAIN;
            expectedStepCount = 0;
        }
        long startTime = System.currentTimeMillis();
        simulationStartTime = startTime;
        try {
            return execution.apply(startTime);
        } catch (Exception error) {
            error.printStackTrace();
            return SimulationResult.failed("Simulation error: " + error.getMessage());
        } finally {
            synchronized (pauseMonitor) {
                state.set(EngineState.IDLE);
                pauseMonitor.notifyAll();
            }
        }
    }

    /**
     * Executes the actual simulation loop.
     *
     * <p>Simulation order matches the proven SimulationsKern:
     * <ol>
     *   <li>Apply gate signals from PREVIOUS step's CONTROL output</li>
     *   <li>Build A matrix</li>
     *   <li>Build b vector</li>
     *   <li>Solve Ax=b</li>
     *   <li>Diode iteration loop (re-solve until converged)</li>
     *   <li>Shift history (pALT←p, iALTALT←iALT) then store converged currents</li>
     *   <li>Store results into netlist (for probes)</li>
     *   <li>Capture the electrical step trace</li>
     *   <li>LK → CONTROL transfer (VOLT/AMP probes)</li>
     *   <li>Execute CONTROL calculators</li>
     *   <li>CONTROL → LK gate signals (stored for step 1 of NEXT iteration)</li>
     *   <li>Extract SCOPE values</li>
     * </ol>
     */
    private SimulationResult executeSimulation(SimulationConfig config, long startTime,
            BooleanSupplier cancellationSignal) {
        CircuitModel circuitModel = parseCircuitModel(config);
        SolverSettingsCore settings = config.resolveSolverSettings(circuitModel);
        validateSimulationSettings(settings.getStepWidth(), settings.getSimulationDuration());

        if (circuitModel != null) {
            var preparation = new CircuitPreparation();
            var prepared = preparation.prepare(preparation.capture(circuitModel),
                    new PreparationRequest(config.explicitSolverOverrides(), config.getParameterOverrides()));
            if (!(prepared instanceof PreparationResult.Ready ready)) {
                String findings = prepared.report().diagnostics().stream()
                        .map(item -> item.code() + ": " + item.message())
                        .collect(java.util.stream.Collectors.joining("; "));
                return SimulationResult.failed(findings);
            }
            circuitModel = ready.execution().newRunModel();
            settings = ready.execution().solverSettings();
        }
        return executeSimulation(config, startTime, cancellationSignal, circuitModel, settings);
    }

    private SimulationResult executeSimulation(SimulationConfig config, long startTime,
            BooleanSupplier cancellationSignal, CircuitModel circuitModel, SolverSettingsCore settings) {
        double dt = settings.getStepWidth();
        double duration = settings.getSimulationDuration();
        validateSimulationSettings(dt, duration);

        endTime = duration;
        currentTime = 0;
        currentStep = 0;

        expectedStepCount = (int) Math.floor(duration / dt) + 1;
        try (var session = SimulationSession.fromModel(circuitModel, settings,
                new ExecutionOptions(config.isDataLoggingEnabled(), config.getDataLoggingInterval()))) {
            phase = session.status().phase();
            hasPreCalculation = phase == SimulationSession.Phase.WARMUP;
            overallEndTime = hasPreCalculation ? settings.getPreCalculationTime() + duration : duration;
            endTime = session.status().endTime();
            expectedStepCount = expectedSegmentSteps(session.status());
            while (session.status().state() != SimulationSession.State.COMPLETED) {
                if (state.get() == EngineState.PAUSED) session.pauseAtCommittedStep();
                awaitResumeOrCancellation(cancellationSignal);
                if (cancelRequested.get() || Thread.currentThread().isInterrupted() || cancellationSignal.getAsBoolean()) {
                    session.pauseAtCommittedStep();
                    currentTime = session.status().time();
                    return SimulationResult.builder()
                            .status(SimulationResult.Status.CANCELLED)
                            .dataContainer(session.currentSegmentRecording())
                            .executionTimeMs(System.currentTimeMillis() - startTime)
                            .totalTimeSteps(currentStep)
                            .simulatedTime(currentTime)
                            .metadata("scopeChannels", session.scopeChannels())
                            .build();
                }
                currentTime = session.status().nextTime();
                StepTraceListener listener = stepTraceListener;
                var sample = session.step(listener == null ? null : (time, volts, currents, netlist) -> {
                    var snapshot = session.status();
                    phase = snapshot.phase();
                    endTime = snapshot.endTime();
                    currentTime = time;
                    currentStep = Math.max(0, snapshot.segmentSteps() - 1);
                    expectedStepCount = expectedSegmentSteps(snapshot);
                    listener.onStep(time, volts, currents, netlist);
                });
                currentStep = session.status().segmentSteps();
                currentTime = session.status().nextTime();
                endTime = session.status().endTime();
                phase = session.status().phase();
                expectedStepCount = expectedSegmentSteps(session.status());
                if (sample == null) break;
                if (progressListener != null && currentStep % 1000 == 0) {
                    progressListener.onProgress(sample.time(), endTime, currentStep);
                }
            }
            currentTime = session.status().segmentSteps() == 0 ? 0 : session.status().time();
            var success = SimulationResult.builder()
                    .status(SimulationResult.Status.SUCCESS)
                    .dataContainer(session.currentSegmentRecording())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .totalTimeSteps(currentStep)
                    .simulatedTime(currentTime)
                    .metadata("solver", settings.getSolverType().toString())
                    .metadata("dt", dt)
                    .metadata("scopeChannels", session.scopeChannels())
                    .metadata("circuitFile", config.getCircuitFilePath())
                    .metadata("parameterOverrides", config.getParameterOverrides().size());
            if (settings.isZeroCrossing()) {
                var report = session.eventLocationReport();
                success.metadata("zeroCrossingLocated", report.located())
                        .metadata("zeroCrossingUnlocated", report.unlocated())
                        .metadata("zeroCrossingFirstUnlocatedTime", report.firstUnlocatedTime())
                        .metadata("zeroCrossingRetained", report.retained())
                        .metadata("zeroCrossingDropped", report.dropped());
            }
            return success.build();
        }
    }

    /**
     * Requests cancellation of the running simulation.
     * The simulation will stop at the next opportunity.
     */
    public void cancel() {
        synchronized (pauseMonitor) {
            if (state.get() != EngineState.IDLE) {
                cancelRequested.set(true);
                pauseMonitor.notifyAll();
            }
        }
    }

    private void awaitResumeOrCancellation(BooleanSupplier cancellationSignal) {
        // Invoke the owner's signal outside this monitor to avoid lock inversion.
        while (state.get() == EngineState.PAUSED && !cancelRequested.get()
                && !Thread.currentThread().isInterrupted() && !cancellationSignal.getAsBoolean()) {
            synchronized (pauseMonitor) {
                if (state.get() == EngineState.PAUSED && !cancelRequested.get()) {
                    try {
                        // The external cancellation signal does not own this monitor.
                        pauseMonitor.wait(50);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
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
        if (overallEndTime <= 0) {
            return 0;
        }
        return Math.min(1.0, currentTime / overallEndTime);
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
        synchronized (pauseMonitor) {
            return !cancelRequested.get() && state.compareAndSet(EngineState.RUNNING, EngineState.PAUSED);
        }
    }

    /**
     * Resumes a paused simulation.
     * Has no effect if the simulation is not paused.
     *
     * @return true if resume was successful, false if simulation was not paused
     */
    public boolean resume() {
        synchronized (pauseMonitor) {
            boolean resumed = !cancelRequested.get() && state.compareAndSet(EngineState.PAUSED, EngineState.RUNNING);
            if (resumed) {
                pauseMonitor.notifyAll();
            }
            return resumed;
        }
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

        double segmentProgress = expectedStepCount == 0 ? 0 : Math.min(1.0, (double) currentStep / expectedStepCount);
        double preCalcProgress = !hasPreCalculation ? 0
                : phase == SimulationSession.Phase.WARMUP ? segmentProgress : 1;
        double mainSimProgress = phase == SimulationSession.Phase.MAIN ? segmentProgress : 0;

        // Estimate remaining time based on current progress
        Long estimatedRemainingMs = null;
        if (overallProgress > 0.01) { // Only estimate after 1% to avoid division by zero
            long elapsedMs = System.currentTimeMillis() - simulationStartTime;
            long totalEstimatedMs = (long) (elapsedMs / overallProgress);
            estimatedRemainingMs = totalEstimatedMs - elapsedMs;
        }

        int totalSteps = expectedStepCount;

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

    /**
     * Listener interface for per-step trace capture.
     * Used for golden reference comparison between SimulationsKern and HeadlessSimulationEngine.
     */
    @FunctionalInterface
    public interface StepTraceListener {
        /**
         * Called after each LK time step with the complete simulation state.
         *
         * @param time current simulation time
         * @param nodeVoltages node potentials (p[])
         * @param currents component currents (iALT[])
         * @param netlist the circuit netlist (for switch/diode state access)
         */
        void onStep(double time, double[] nodeVoltages, double[] currents, CircuitNetlist netlist);
    }

    /**
     * Sets a step trace listener for per-step golden reference comparison.
     *
     * @param listener the listener, or null to remove
     */
    public void setStepTraceListener(StepTraceListener listener) {
        this.stepTraceListener = listener;
    }

    private static int expectedSegmentSteps(SimulationSession.Status snapshot) {
        double remaining = snapshot.nextTime() > snapshot.endTime() ? 0
                : Math.floor((snapshot.endTime() - snapshot.nextTime()) / snapshot.stepWidth()) + 1;
        return snapshot.segmentSteps() + (int) remaining;
    }

    private static void validateSimulationSettings(double dt, double duration) {
        SimulationSession.validateTiming(dt, duration);
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

}
