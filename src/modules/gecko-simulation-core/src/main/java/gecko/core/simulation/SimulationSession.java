package gecko.core.simulation;

import gecko.core.allg.SolverSettingsCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.datacontainer.ContainerStatus;
import gecko.core.datacontainer.DataContainerGlobal;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparedCircuit;
import gecko.core.preparation.SolverFeatures;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Retains one circuit's numerical state, CONTROL state, and recordings across steps. */
public final class SimulationSession implements AutoCloseable {
    /** Describes execution admission, numerical completion, and resource closure. */
    public enum State { READY, RUNNING, PAUSED, COMPLETED, FAILED, CLOSED }
    /** Identifies the phase that owns the current segment. */
    public enum Phase { WARMUP, MAIN }

    /** A detached view of the last committed step and the next requested timestamp. */
    public record Status(State state, int committedSteps, double time, double nextTime,
            double endTime, double stepWidth, Phase phase, int segmentSteps) {
        /** Creates main-phase status with the cumulative count as its segment count. */
        public Status(State state, int committedSteps, double time, double nextTime, double endTime, double stepWidth) {
            this(state, committedSteps, time, nextTime, endTime, stepWidth, Phase.MAIN, committedSteps);
        }
    }

    /** A detached sample after both electrical and CONTROL calculations commit. */
    public record Sample(double time, float[] values) {
        /** Copies the captured values into this sample. */
        public Sample { values = values.clone(); }
        /** @return detached values in recording row order */
        @Override public float[] values() { return values.clone(); }
    }

    /** A bounded, detached window of recorded samples. */
    public record Observation(int firstIndex, double[] times, float[][] values) {
        /** Copies timestamps and rows into this observation. */
        public Observation {
            times = times.clone();
            values = copy(values);
        }
        /** @return detached timestamps in recorded order */
        @Override public double[] times() { return times.clone(); }
        /** @return detached rows of signal values */
        @Override public float[][] values() { return copy(values); }
        private static float[][] copy(float[][] input) {
            float[][] copy = new float[input.length][];
            for (int index = 0; index < input.length; index++) copy[index] = input[index].clone();
            return copy;
        }
    }

    private static final int MAX_OBSERVATION_SAMPLES = 10000;
    private final CircuitNetlist netlist;
    private final ElectricalTimeStep electrical;
    private final boolean zeroCrossing;
    private final ControlRuntime control;
    private final ExecutionOptions options;
    private final String[] signalNames;
    private DataContainerGlobal recording;
    private final AtomicBoolean operation = new AtomicBoolean();
    private final AtomicBoolean closeRequested = new AtomicBoolean();
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicLong pauseRequests = new AtomicLong();
    private volatile Thread runOwner;
    private final ThreadLocal<Boolean> callbackDelivery = new ThreadLocal<>();
    private volatile Status status;
    private int recordedSamples;
    private Phase phase = Phase.MAIN;
    private int segmentFirstStep;
    private int segmentFirstRecorded;
    private final double mainStep;
    private final double mainDuration;
    private double mainEndTime;

    private SimulationSession(CircuitModel model, SolverSettingsCore settings, ExecutionOptions options,
            ControlRuntime suppliedControl) {
        this.options = Objects.requireNonNull(options, "Execution options are required.");
        settings = Objects.requireNonNull(settings, "Solver settings are required.").copy();
        SimulationTiming.validateSettings(settings);
        double dt = settings.getStepWidth();
        double duration = settings.getSimulationDuration();
        zeroCrossing = settings.isZeroCrossing();
        mainStep = dt;
        mainDuration = duration;
        double preTime = settings.getPreCalculationTime();
        mainEndTime = preTime > 0 ? preTime + duration : duration;
        if (preTime > 0) {
            phase = Phase.WARMUP;
            dt = settings.getPreCalculationStepWidth();
            duration = preTime;
        }
        ControlRuntime constructedControl = suppliedControl;
        try {
            CircuitNetlist constructedNetlist = NetlistBuilder.buildFromCircuitModel(model);
            if (constructedControl == null) {
                constructedControl = ControlRuntime.fromDeferred(model, dt, constructedNetlist,
                        settings.getControlSamplePeriod());
            }
            // Validate the main grid now. Stage its live histories again after warmup.
            if (phase == Phase.WARMUP && mainStep != dt) constructedControl.prepareTimeStepChange(mainStep);
            String[] constructedSignalNames = constructedControl.hasScope()
                    ? constructedControl.getSignalNames() : fallbackSignalNames(model);
            DataContainerGlobal constructedRecording = new DataContainerGlobal();
            constructedRecording.init(constructedSignalNames.length, recordingCapacity(dt, duration),
                    constructedSignalNames, "time [s]");
            constructedRecording.setContainerStatus(ContainerStatus.PAUSED);
            Status constructedStatus = statusFor(State.READY, 0, 0, 0, duration, dt);
            boolean hasElectrical = constructedNetlist != null && constructedNetlist.getElementCount() > 0;
            if (hasElectrical) constructedControl.seedSources(constructedNetlist);
            if (hasElectrical && settings.isDcOperatingPoint()) {
                // The DC solve needs the gates the first step will use. Initializing CONTROL here is the same
                // call the first step makes, so the default order is unchanged when the feature is off.
                constructedControl.initializeAtSimulationStart();
                constructedControl.applyPendingGates(constructedNetlist);
            }
            ElectricalTimeStep constructedElectrical = hasElectrical
                    ? new ElectricalTimeStep(constructedNetlist, settings.getSolverType(),
                            new SolverFeatures(0, settings.isZeroCrossing(), settings.isDcOperatingPoint())) : null;
            netlist = constructedNetlist;
            control = constructedControl;
            signalNames = constructedSignalNames;
            recording = constructedRecording;
            status = constructedStatus;
            electrical = constructedElectrical;
        } catch (RuntimeException | Error failure) {
            closeControlAfterFailure(constructedControl, failure);
            throw failure;
        }
    }

    /**
     * Opens independent execution state from an immutable prepared circuit.
     *
     * @param circuit the prepared circuit whose configuration remains unchanged
     * @param options the logging choices for this session
     * @return a session whose caller must close its resources
     * @throws NullPointerException if a required argument is null
     * @throws IllegalArgumentException if its captured settings cannot execute
     */
    public static SimulationSession open(PreparedCircuit circuit, ExecutionOptions options) {
        Objects.requireNonNull(circuit, "Prepared circuit is required.");
        return fromModel(circuit.newRunModel(), circuit.solverSettings(), options);
    }

    static SimulationSession fromModel(CircuitModel model, SolverSettingsCore settings, ExecutionOptions options) {
        return new SimulationSession(model, settings, options, null);
    }

    /** Takes ownership of an assembled runtime after validating the execution arguments. */
    static SimulationSession fromModel(CircuitModel model, SolverSettingsCore settings, ExecutionOptions options,
            ControlRuntime control) {
        return new SimulationSession(model, settings, options, Objects.requireNonNull(control));
    }

    /**
     * Commits one step. A concurrent or reentrant step fails without waiting.
     * The first sample uses timestamp zero. Later samples use the retained next timestamp.
     *
     * @return detached values for the committed step
     * @throws IllegalStateException if the session is ended, failed, closed, busy, or called reentrantly
     * @throws IllegalArgumentException if its absolute clock cannot advance
     */
    public Sample step() {
        Sample sample = step(null, true);
        if (sample == null) throw new IllegalStateException("The simulation segment has ended.");
        return sample;
    }

    Sample step(HeadlessSimulationEngine.StepTraceListener listener) { return step(listener, false); }

    private Sample step(HeadlessSimulationEngine.StepTraceListener listener, boolean pauseAfterStep) {
        Trace[] trace = new Trace[1];
        Sample sample;
        enterOperation();
        try {
            Status before = status;
            if (before.state() == State.COMPLETED) throw new IllegalStateException("The simulation segment has ended.");
            if (before.state() == State.FAILED) throw new IllegalStateException("The simulation session has failed.");
            if (phase == Phase.WARMUP && before.nextTime() > before.endTime()) {
                try { extendAtBoundary(mainEndTime, mainStep, Phase.MAIN, true); }
                catch (RuntimeException | Error failure) {
                    failExecution(failure);
                    throw failure;
                }
                before = status;
                if (before.state() == State.COMPLETED) {
                    control.tearDownOnPause();
                    return null;
                }
            }
            double time = before.nextTime();
            status = statusFor(State.RUNNING, before.committedSteps(), before.time(), before.nextTime(),
                    before.endTime(), before.stepWidth());
            recording.setContainerStatus(ContainerStatus.RUNNING);
            try {
                double nextTime = SimulationTiming.nextTime(time, before.stepWidth());
                control.initializeAtSimulationStart();
                if (netlist != null) netlist.setSimulationTime(time);
                if (electrical != null) {
                    control.applyPendingGates(netlist);
                    HeadlessSimulationEngine.StepTraceListener capture = listener == null ? null :
                            (t, volts, currents, circuit) -> trace[0] = new Trace(t, volts.clone(), currents.clone(),
                                    circuit.detachedSnapshot());
                    if (zeroCrossing) electrical.advanceInterval(before.stepWidth(), time, capture);
                    else electrical.advance(before.stepWidth(), time, capture);
                }
                control.advanceAfterElectricalStep(netlist, time);
                float[] values = new float[signalNames.length];
                if (control.hasScope()) values = control.sampleScope();
                else if (electrical != null) electrical.copyNodeVoltagesTo(values);
                if (options.loggingEnabled() && before.committedSteps() % options.loggingInterval() == 0) {
                    recording.insertValuesAtEnd(values, time);
                    recordedSamples++;
                }
                State nextState = nextTime > before.endTime() && phase == Phase.MAIN ? State.COMPLETED
                        : pauseAfterStep ? State.PAUSED : State.RUNNING;
                status = statusFor(nextState, before.committedSteps() + 1, time, nextTime,
                        before.endTime(), before.stepWidth());
                recording.setContainerStatus(nextState == State.COMPLETED ? ContainerStatus.FINISHED
                        : nextState == State.RUNNING ? ContainerStatus.RUNNING : ContainerStatus.PAUSED);
                if (pauseAfterStep || nextState == State.COMPLETED) control.tearDownOnPause();
                sample = new Sample(time, values);
            } catch (RuntimeException | Error failure) {
                failExecution(failure);
                throw failure;
            }
        } finally {
            leaveOperation();
        }
        if (trace[0] != null) {
            callbackDelivery.set(true);
            try { listener.onStep(trace[0].time(), trace[0].volts(), trace[0].currents(), trace[0].netlist()); }
            finally { callbackDelivery.remove(); }
        }
        return sample;
    }

    public void setExternalInputs(int port, double[] values) {
        enterOperation();
        try { control.setExternalInputs(port, values == null ? null : values.clone()); }
        finally { leaveOperation(); }
    }

    public double[] externalOutputs(int port) {
        enterOperation();
        try { return control.externalOutputs(port); }
        finally { leaveOperation(); }
    }

    public int externalInputPorts() { enterOperation(); try { return control.externalInputPorts(); } finally { leaveOperation(); } }
    public int externalOutputPorts() { enterOperation(); try { return control.externalOutputPorts(); } finally { leaveOperation(); } }
    public int externalInputWidth(int port) { enterOperation(); try { return control.externalInputWidth(port); } finally { leaveOperation(); } }
    public int externalOutputWidth(int port) { enterOperation(); try { return control.externalOutputWidth(port); } finally { leaveOperation(); } }

    /**
     * Runs the remaining phases on the caller thread.
     *
     * @return the status after completion, pause, or closure
     * @throws IllegalStateException if the session is failed, busy, or called reentrantly
     */
    public Status run() { return run(() -> false); }

    /**
     * Runs until completion, pause, close, or an external stop request.
     * The stop callback runs outside step ownership. A later run retains all committed histories.
     *
     * @param stopRequested a signal checked before each numerical step
     * @return the status after completion, pause, or closure
     * @throws NullPointerException if the stop signal is null
     * @throws IllegalStateException if the session is failed, busy, or called reentrantly
     */
    public Status run(BooleanSupplier stopRequested) { return run(stopRequested, sample -> { }); }

    /**
     * Delivers committed samples outside numerical ownership.
     * A callback can request pause or close. Other reentrant operations fail without waiting.
     *
     * @param stopRequested a signal checked before each numerical step
     * @param listener receives each detached sample after its step commits
     * @return the status after completion, pause, or closure
     * @throws NullPointerException if a callback is null
     * @throws IllegalStateException if the session is failed, busy, or called reentrantly
     */
    public Status run(BooleanSupplier stopRequested, Consumer<Sample> listener) {
        Objects.requireNonNull(stopRequested, "Stop signal is required.");
        Objects.requireNonNull(listener, "Sample listener is required.");
        long pauseAtAdmission = pauseRequests.get();
        if (!running.compareAndSet(false, true)) throw new IllegalStateException("A simulation run is already active.");
        runOwner = Thread.currentThread();
        boolean admitted = false;
        Throwable primary = null;
        try {
            enterOperation(true);
            try {
                Status before = status;
                if (before.state() == State.FAILED) throw new IllegalStateException("The simulation session has failed.");
                if (before.state() != State.COMPLETED && before.state() != State.CLOSED) {
                    status = statusFor(State.RUNNING, before.committedSteps(), before.time(), before.nextTime(),
                            before.endTime(), before.stepWidth());
                    admitted = true;
                }
            } finally { leaveOperation(); }
            while (admitted && !closeRequested.get() && pauseRequests.get() == pauseAtAdmission
                    && !Thread.currentThread().isInterrupted() && status.state() != State.COMPLETED) {
                boolean stop;
                callbackDelivery.set(true);
                try { stop = stopRequested.getAsBoolean(); }
                finally { callbackDelivery.remove(); }
                if (stop || closeRequested.get() || pauseRequests.get() != pauseAtAdmission) break;
                Sample sample = step(null, false);
                if (sample == null) break;
                callbackDelivery.set(true);
                try { listener.accept(sample); }
                finally { callbackDelivery.remove(); }
            }
        } catch (RuntimeException | Error failure) {
            primary = failure;
            throw failure;
        } finally {
            try {
                if (admitted && !closeRequested.get()) pauseAtCommittedStep();
            } catch (RuntimeException | Error cleanup) {
                if (primary == null) throw cleanup;
                if (primary != cleanup) primary.addSuppressed(cleanup);
            } finally {
                runOwner = null;
                running.set(false);
            }
        }
        return status;
    }

    void pauseAtCommittedStep() {
        enterOperation(true);
        try {
            control.tearDownOnPause();
        } catch (RuntimeException | Error failure) {
            Status previous = status;
            status = statusFor(State.FAILED, previous.committedSteps(), previous.time(), previous.nextTime(),
                    previous.endTime(), previous.stepWidth());
            closeControlAfterFailure(failure);
            throw failure;
        } finally {
            Status previous = status;
            if (previous.state() == State.RUNNING) {
                status = statusFor(State.PAUSED, previous.committedSteps(), previous.time(), previous.nextTime(),
                        previous.endTime(), previous.stepWidth());
            }
            if (recording.getContainerStatus() == ContainerStatus.RUNNING) {
                recording.setContainerStatus(ContainerStatus.PAUSED);
            }
            leaveOperation();
        }
    }

    /** Requests a pause at the next committed step without waiting for caller code. */
    public void pause() { pauseRequests.incrementAndGet(); }

    /**
     * Extends the current phase without resetting histories.
     * During warmup, the configured main duration follows the new warmup end.
     *
     * @param endTime the new absolute phase end, after the current end
     * @throws IllegalArgumentException if the remaining grid cannot execute or fit its counters
     * @throws IllegalStateException if the session is closed, failed, busy, or called reentrantly
     */
    public void extend(double endTime) { extend(endTime, 0, false); }

    /**
     * Stages and commits a new step and end time without resetting histories.
     * The first continued step retains the previous next timestamp, as desktop continuation does.
     * Validation and recording allocation finish before the CONTROL transition commits.
     * During warmup, the configured main duration follows the new warmup end.
     *
     * @param endTime the new absolute phase end, after the current end
     * @param stepWidth the finite, positive step for subsequent calculations
     * @throws IllegalArgumentException if the remaining grid cannot execute or fit its counters
     * @throws IllegalStateException if the session is closed, failed, busy, or called reentrantly
     */
    public void extend(double endTime, double stepWidth) { extend(endTime, stepWidth, true); }

    private void extend(double endTime, double requestedStep, boolean changeStep) {
        enterOperation();
        try { extendAtBoundary(endTime, changeStep ? requestedStep : status.stepWidth(), phase, false); }
        finally { leaveOperation(); }
    }

    private void extendAtBoundary(double endTime, double step, Phase nextPhase, boolean automaticPhase) {
        Status before = status;
        if (before.state() == State.FAILED) throw new IllegalStateException("The simulation session has failed.");
        int additional = SimulationTiming.remainingSteps(before.nextTime(), endTime, step, before.committedSteps());
        if ((!automaticPhase && endTime <= before.endTime()) || (automaticPhase && endTime < before.endTime())
                || !automaticPhase && endTime < before.nextTime()) {
            throw new IllegalArgumentException("The extended end time must include the next step and exceed the previous end time.");
        }
        double followingMainEnd = nextPhase == Phase.WARMUP ? endTime + mainDuration : endTime;
        if (!Double.isFinite(followingMainEnd)) throw new IllegalArgumentException("The simulation end time must be finite.");
        if (nextPhase == Phase.WARMUP) {
            SimulationTiming.validateTotalSteps((long) before.committedSteps() + additional
                    + recordingCapacity(mainStep, mainDuration));
        }
        Runnable transition = step == before.stepWidth() ? null : control.prepareTimeStepChange(step);
        boolean empty = before.nextTime() > endTime;
        var extended = new DataContainerGlobal();
        extended.initArrayTimeSeries(signalNames.length, recordedSamples + additional + 1, signalNames, "time [s]");
        for (int index = 0; index < recordedSamples; index++) {
            float[] values = new float[signalNames.length];
            for (int row = 0; row < values.length; row++) values[row] = recording.getValue(row, index);
            extended.insertValuesAtEnd(values, recording.getTimeValue(index, 0));
        }
        extended.setContainerStatus(empty ? ContainerStatus.FINISHED : ContainerStatus.PAUSED);
        var continued = new Status(empty ? State.COMPLETED : State.PAUSED, before.committedSteps(), before.time(),
                before.nextTime(), endTime, step, nextPhase, 0);
        if (closeRequested.get()) throw new IllegalStateException("The simulation session is closed.");
        if (transition != null) transition.run();
        recording = extended;
        phase = nextPhase;
        mainEndTime = followingMainEnd;
        segmentFirstStep = before.committedSteps();
        segmentFirstRecorded = recordedSamples;
        status = continued;
    }

    /**
     * Reads status without waiting for an active step or callback.
     *
     * @return an immutable snapshot with cumulative and current-segment counts
     */
    public Status status() { return status; }

    /**
     * Returns at most 10000 cumulative recorded samples.
     *
     * @param firstIndex the nonnegative cumulative sample index
     * @param count the number of samples, from zero through 10000
     * @return a detached window, empty when the first index exceeds the recording
     * @throws IllegalArgumentException if an index or count is invalid
     * @throws IllegalStateException if an operation is active or the call is reentrant
     */
    public Observation observe(int firstIndex, int count) { return observeRange(firstIndex, count, false); }

    /**
     * Returns at most 10000 samples from the current segment.
     *
     * @param firstIndex the nonnegative sample index relative to this segment
     * @param count the number of samples, from zero through 10000
     * @return a detached window, empty when the first index exceeds the segment recording
     * @throws IllegalArgumentException if an index or count is invalid
     * @throws IllegalStateException if an operation is active or the call is reentrant
     */
    public Observation observeCurrentSegment(int firstIndex, int count) { return observeRange(firstIndex, count, true); }

    private Observation observeRange(int firstIndex, int count, boolean currentSegment) {
        if (firstIndex < 0 || count < 0 || count > MAX_OBSERVATION_SAMPLES) {
            throw new IllegalArgumentException("Observation indices must be non-negative. Count must not exceed 10000.");
        }
        enterOperation(true);
        try {
            long start = (long) firstIndex + (currentSegment ? segmentFirstRecorded : 0);
            int length = (int) Math.min(count, Math.max(0L, recordedSamples - start));
            double[] times = new double[length];
            float[][] values = new float[signalNames.length][length];
            for (int index = 0; index < length; index++) {
                int recordedIndex = (int) start + index;
                times[index] = recording.getTimeValue(recordedIndex, 0);
                for (int row = 0; row < values.length; row++) values[row][index] = recording.getValue(row, recordedIndex);
            }
            return new Observation(firstIndex, times, values);
        } finally { leaveOperation(); }
    }

    /**
     * Returns at most 10000 located switching events, oldest first. The log is empty when zero-crossing
     * location is off. Its signal index is the circuit element index.
     *
     * @param firstIndex the nonnegative index of the first event
     * @param count the number of events, from zero through 10000
     * @return a detached window, empty when the first index exceeds the log
     * @throws IllegalArgumentException if an index or count is invalid
     * @throws IllegalStateException if an operation is active or the call is reentrant
     */
    public List<ZeroCrossingEvent> zeroCrossingEvents(int firstIndex, int count) {
        if (firstIndex < 0 || count < 0 || count > MAX_OBSERVATION_SAMPLES) {
            throw new IllegalArgumentException("Event indices must be non-negative. Count must not exceed 10000.");
        }
        enterOperation(true);
        try { return electrical == null ? List.of() : electrical.events(firstIndex, count); }
        finally { leaveOperation(); }
    }

    /**
     * Reports located, unlocated and dropped switching events. A nonzero unlocated or dropped count means
     * the event log is incomplete.
     *
     * @return the counts, all zero when location is off
     * @throws IllegalStateException if an operation is active or the call is reentrant
     */
    public EventLocationReport eventLocationReport() {
        enterOperation(true);
        try { return electrical == null ? EventLocationReport.empty() : electrical.eventReport(); }
        finally { leaveOperation(); }
    }

    /**
     * Returns signal names in recording row order.
     *
     * @return a detached array whose changes cannot affect the session
     */
    public String[] signalNames() { return signalNames.clone(); }

    List<ScopeDataExtractor.ScopeChannel> scopeChannels() { return control.getScopeChannels(); }
    DataContainerGlobal recording() { return recording; }

    DataContainerGlobal currentSegmentRecording() {
        enterOperation(true);
        try {
            int length = recordedSamples - segmentFirstRecorded;
            var snapshot = new DataContainerGlobal();
            snapshot.initArrayTimeSeries(signalNames.length, Math.max(1, length), signalNames.clone(), "time [s]");
            for (int index = 0; index < length; index++) {
                float[] values = new float[signalNames.length];
                for (int row = 0; row < values.length; row++) values[row] = recording.getValue(row, segmentFirstRecorded + index);
                snapshot.insertValuesAtEnd(values, recording.getTimeValue(segmentFirstRecorded + index, 0));
            }
            snapshot.setContainerStatus(recording.getContainerStatus());
            return snapshot;
        } finally { leaveOperation(); }
    }

    /**
     * Requests release without waiting for an active step or caller callback.
     * An idle session closes resources on the caller thread. Repeated calls do not repeat resource callbacks.
     *
     * @throws RuntimeException if a resource callback fails during immediate closure
     */
    @Override public void close() {
        closeRequested.set(true);
        finishClose();
    }

    private void enterOperation() { enterOperation(false); }

    private void enterOperation(boolean allowClosed) {
        if (Boolean.TRUE.equals(callbackDelivery.get())) {
            throw new IllegalStateException("Reentrant session operations are not allowed during a callback.");
        }
        if (running.get() && runOwner != Thread.currentThread()) {
            throw new IllegalStateException("A simulation run is already active.");
        }
        if (!operation.compareAndSet(false, true)) throw new IllegalStateException("A session operation is already active.");
        if (closeRequested.get() && !allowClosed) {
            leaveOperation();
            throw new IllegalStateException("The simulation session is closed.");
        }
    }

    private void leaveOperation() {
        operation.set(false);
        if (closeRequested.get()) finishClose();
    }

    private void finishClose() {
        if (!operation.compareAndSet(false, true)) return;
        try {
            Status previous = status;
            if (previous.state() != State.CLOSED) {
                status = statusFor(State.CLOSED, previous.committedSteps(), previous.time(), previous.nextTime(),
                        previous.endTime(), previous.stepWidth());
                control.close();
            }
        } finally { operation.set(false); }
    }

    private void failExecution(Throwable failure) {
        Status previous = status;
        status = statusFor(State.FAILED, previous.committedSteps(), previous.time(), previous.nextTime(),
                previous.endTime(), previous.stepWidth());
        recording.setContainerStatus(ContainerStatus.PAUSED);
        closeControlAfterFailure(failure);
    }

    private void closeControlAfterFailure(Throwable failure) {
        closeControlAfterFailure(control, failure);
    }

    private static void closeControlAfterFailure(ControlRuntime runtime, Throwable failure) {
        if (runtime == null) return;
        try { runtime.close(); }
        catch (Throwable cleanup) {
            if (failure != cleanup) failure.addSuppressed(cleanup);
        }
    }

    private Status statusFor(State state, int committedSteps, double time, double nextTime,
            double endTime, double stepWidth) {
        return new Status(state, committedSteps, time, nextTime, endTime, stepWidth,
                phase, committedSteps - segmentFirstStep);
    }

    private record Trace(double time, double[] volts, double[] currents, CircuitNetlist netlist) { }

    static int recordingCapacity(double dt, double duration) { return SimulationTiming.recordingCapacity(dt, duration); }

    static void validateTiming(double dt, double duration) { SimulationTiming.validateInitial(dt, duration); }

    private static String[] fallbackSignalNames(CircuitModel model) {
        if (model != null && model.getDataContainerSignals() != null && model.getDataContainerSignals().length > 0) {
            return model.getDataContainerSignals().clone();
        }
        return new String[]{"V_out", "I_in", "P_loss"};
    }
}
