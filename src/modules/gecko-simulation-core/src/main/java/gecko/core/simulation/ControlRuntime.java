/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.simulation;

import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.control.PeriodicStateSource;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.FromExternalCalculator;
import gecko.core.control.calculators.TimeCalculator;
import gecko.core.control.calculators.ToExternalCalculator;
import gecko.core.io.CircuitModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Owns an assembled CONTROL chain and its electrical coupling phases.
 * Apply pending gates before the electrical solve. Advance CONTROL after
 * electrical results and the step trace have been published.
 * Call lifecycle methods from the session owner without holding session locks.
 */
final class ControlRuntime implements AutoCloseable {
    private final ControlNetlist controlNetlist;
    private final DomainCoupler domainCoupler;
    private final ScopeDataExtractor scopeExtractor;
    private final List<AbstractControlCalculatable> owners;
    private final List<FromExternalCalculator> externalInputs;
    private final List<ToExternalCalculator> externalOutputs;
    private static final double SAMPLE_TOLERANCE = 1e-9;
    private static final double FIRST_INEXACT_SAMPLE_INDEX = 0x1.0p53;
    private static final long SAMPLE_INDEX_BEYOND_PRECISION = -1;
    /** The sample period of each CONTROL group on the absolute clock. Group 0 is the default at the global period. */
    private final double[] periods;
    private final long[] nextSampleIndex;
    private final boolean[] outputsPublishedLastStep;
    private final boolean[] sampledNow;
    private final double[] groupSteps;
    private double dt;
    private long stateVersion;
    private Runnable pendingInitialization;
    private boolean initialized;
    private boolean activeSegment;
    private boolean closed;

    private ControlRuntime(ControlCalculatorFactory.BuildResult build, double dt, double samplePeriod) {
        if (!Double.isFinite(dt) || dt <= 0) {
            throw new IllegalArgumentException("Time step must be finite and positive.");
        }
        requireValidSamplePeriod(samplePeriod);
        this.dt = dt;
        double[] configured = build.groupPeriods();
        periods = configured == null ? new double[]{samplePeriod} : configured;
        for (double period : periods) requireValidSamplePeriod(period);
        nextSampleIndex = new long[periods.length];
        outputsPublishedLastStep = new boolean[periods.length];
        java.util.Arrays.fill(outputsPublishedLastStep, true);
        sampledNow = new boolean[periods.length];
        groupSteps = new double[periods.length];
        controlNetlist = build.getControlNetlist();
        var unique = Collections.newSetFromMap(new IdentityHashMap<AbstractControlCalculatable, Boolean>());
        var collected = new ArrayList<AbstractControlCalculatable>();
        for (var calculator : controlNetlist.getSortedCalculators()) {
            if (calculator != null && unique.add(calculator)) collected.add(calculator);
        }
        owners = List.copyOf(collected);
        externalInputs = build.externalInputs();
        externalOutputs = build.externalOutputs();
        domainCoupler = new DomainCoupler();
        domainCoupler.configureFromMapping(build.getMapping());
        scopeExtractor = ScopeDataExtractor.fromScopes(build.getMapping().getScopeInfos());
    }

    /**
     * The step that CONTROL calculators see. A sampled controller runs on its nominal period Ts, whatever
     * electrical step quantizes the sample instants, so delays and integrators count controller ticks and
     * the sample instants never accumulate error. CONTROL cannot tick faster than the electrical step,
     * so a period at or below the step runs every step with the electrical step.
     */
    private double controlStep(int group, double electricalStep) {
        return Math.max(periods[group], electricalStep);
    }

    private double[] controlSteps(double electricalStep) {
        double[] steps = new double[periods.length];
        for (int group = 0; group < steps.length; group++) steps[group] = controlStep(group, electricalStep);
        return steps;
    }

    private int groupOf(AbstractControlCalculatable calculator) {
        return controlNetlist.groupOf(calculator);
    }

    private static void requireValidSamplePeriod(double samplePeriod) {
        if (!Double.isFinite(samplePeriod) || samplePeriod < 0) {
            throw new IllegalArgumentException("CONTROL sample period must be finite and non-negative.");
        }
    }

    static ControlRuntime from(CircuitModel model, double dt) {
        var runtime = fromDeferred(model, dt);
        runtime.initializeAtSimulationStart();
        return runtime;
    }

    /** Assembles the chain without invoking calculator initialization callbacks. */
    static ControlRuntime fromDeferred(CircuitModel model, double dt) {
        return fromDeferred(model, dt, null);
    }

    static ControlRuntime fromDeferred(CircuitModel model, double dt, CircuitNetlist electricalNetlist) {
        return fromDeferred(model, dt, electricalNetlist, 0);
    }

    /**
     * A positive sample period runs CONTROL at the first electrical step at or after each multiple of it
     * and holds gate, source, and calculator outputs between samples. Measurement probes stay live, so a scope
     * that records a probe directly shows the physical signal, while every calculator output it feeds is held.
     */
    static ControlRuntime fromDeferred(CircuitModel model, double dt, CircuitNetlist electricalNetlist,
            double samplePeriod) {
        return fromDeferred(model, dt, electricalNetlist, samplePeriod, null);
    }
    static ControlRuntime fromDeferred(CircuitModel model, double dt, CircuitNetlist electricalNetlist,
            double samplePeriod, CircuitNetlist thermalNetlist) {
        requireValidSamplePeriod(samplePeriod);
        double planningStep = Math.max(samplePeriod, dt);
        return new ControlRuntime(new ControlCalculatorFactory().buildCircuit(model, planningStep, electricalNetlist,
                thermalNetlist), dt, samplePeriod);
    }

    /**
     * As above, with a sample period for each named CONTROL subsystem sheet path. A block takes the period of its
     * nearest configured sheet and otherwise the global period. An empty map takes the single-clock path.
     *
     * @throws IllegalArgumentException if a path or period cannot execute for this circuit
     */
    static ControlRuntime fromDeferred(CircuitModel model, double dt, CircuitNetlist electricalNetlist,
            double samplePeriod, CircuitNetlist thermalNetlist, java.util.Map<String, Double> subsystemPeriods) {
        requireValidSamplePeriod(samplePeriod);
        var spec = new ControlSubsystems.Spec(samplePeriod, subsystemPeriods);
        if (!spec.grouped()) return fromDeferred(model, dt, electricalNetlist, samplePeriod, thermalNetlist);
        return new ControlRuntime(new ControlCalculatorFactory().buildCircuit(model, dt, electricalNetlist,
                thermalNetlist, spec), dt, samplePeriod);
    }

    /** Takes ownership of the supplied calculators and their closeable resources. */
    static ControlRuntime fromDeferred(ControlCalculatorFactory.BuildResult build, double dt) {
        return new ControlRuntime(build, dt, 0);
    }

    /** Initializes once. A continuation preserves calculator histories and clocks. */
    void initializeAtSimulationStart() {
        requireOpen();
        if (initialized) return;
        stateVersion++;
        activeSegment = true;
        try {
            Runnable initialization = pendingInitialization;
            pendingInitialization = null;
            if (initialization == null) controlNetlist.prepareInitialization(controlSteps(dt)).run();
            else initialization.run();
            initialized = true;
        } catch (RuntimeException | Error failure) {
            closeAfterFailure(failure);
            throw failure;
        }
    }

    /**
     * Stages a step change without invoking callbacks or changing active state.
     * Before the first step, commit selects a deferred initialization plan.
     * After initialization, commit resamples existing calculator histories.
     * The session owner must serialize calculator and lifecycle access.
     *
     * @param newDt the finite, positive step in seconds
     * @return a single-use commit action
     * @throws IllegalArgumentException if the step or configured delay size is invalid
     * @throws IllegalStateException if the runtime is closed, or the action is stale or repeated
     * @throws UnsupportedOperationException if a calculator cannot stage its callback
     */
    Runnable prepareTimeStepChange(double newDt) {
        requireOpen();
        long expectedVersion = stateVersion;
        boolean beforeStart = !initialized;
        if (!Double.isFinite(newDt) || newDt <= 0) {
            throw new IllegalArgumentException("Time step must be finite and positive.");
        }
        double[] newSteps = controlSteps(newDt);
        Runnable staged;
        if (beforeStart) {
            staged = controlNetlist.prepareInitialization(newSteps);
        } else {
            // A sampled group whose controller step does not change keeps its history. Others restage.
            boolean[] restage = new boolean[periods.length];
            boolean any = false;
            for (int group = 0; group < restage.length; group++) {
                restage[group] = !(periods[group] > 0 && newSteps[group] == controlStep(group, dt));
                any |= restage[group];
            }
            staged = any ? controlNetlist.prepareTimeStepChange(newSteps, restage) : () -> { };
        }
        return new Runnable() {
            private boolean used;

            @Override
            public void run() {
                requireOpen();
                if (used || expectedVersion != stateVersion) {
                    throw new IllegalStateException("The CONTROL transition is stale or has already committed.");
                }
                used = true;
                if (beforeStart) pendingInitialization = staged;
                else staged.run();
                dt = newDt;
                stateVersion++;
            }
        };
    }

    int externalInputPorts() { return externalInputs.size(); }
    int externalOutputPorts() { return externalOutputs.size(); }
    int externalInputWidth(int port) { return externalInputs.get(requirePort(port, externalInputs.size()))._outputSignal.length; }
    int externalOutputWidth(int port) { return externalOutputs.get(requirePort(port, externalOutputs.size()))._inputSignal.length; }

    void setExternalInputs(int port, double[] values) {
        int index = requirePort(port, externalInputs.size());
        if (values == null || values.length != externalInputWidth(index))
            throw new IllegalArgumentException("External input width does not match.");
        externalInputs.get(index).setValues(values);
    }

    double[] externalOutputs(int port) {
        return externalOutputs.get(requirePort(port, externalOutputs.size())).values();
    }

    private static int requirePort(int port, int count) {
        if (port < 0 || port >= count) throw new IllegalArgumentException("External port is out of range.");
        return port;
    }

    boolean hasScope() { return scopeExtractor != null; }

    String[] getSignalNames() {
        return hasScope() ? scopeExtractor.getSignalNames() : new String[0];
    }

    List<ScopeDataExtractor.ScopeChannel> getScopeChannels() {
        return hasScope() ? scopeExtractor.getChannels() : List.of();
    }

    /**
     * Sets signal-controlled sources from the current CONTROL outputs.
     * The desktop does this once before CONTROL initialization and before the initial-condition solve.
     */
    void seedSources(CircuitNetlist circuitNetlist) {
        requireOpen();
        domainCoupler.transferControlToSources(circuitNetlist);
    }

    void seedThermalSources(ThermalNetwork thermal) {
        requireOpen();
        domainCoupler.transferControlToThermalSources(thermal);
    }

    /**
     * A gate applies the value it published at its own sample, not its live input, in two cases. The gate belongs to
     * a configured subsystem. Or subsystems are configured and the default group has a positive period, so a faster
     * producer cannot reach the switch between two samples of the default group. Otherwise the gate reads its live
     * input: without configured subsystems as with a single global period, and in a default group with period zero.
     */
    void applyPendingGates(CircuitNetlist circuitNetlist) {
        requireOpen();
        boolean defaultGroupSampled = periods.length > 1 && periods[0] > 0;
        domainCoupler.applyGateSignals(circuitNetlist, calculator -> groupOf(calculator) != 0 || defaultGroupSampled);
    }

    void advanceAfterElectricalStep(CircuitNetlist circuitNetlist, double time) {
        advanceAfterElectricalStep(circuitNetlist, null, time, time);
    }

    /** Advances CONTROL using the committed grid time for outputs and the compensated time for sample decisions. */
    void advanceAfterElectricalStep(CircuitNetlist circuitNetlist, ThermalNetwork thermal, double time, double samplingTime) {
        initializeAtSimulationStart();
        stateVersion++;
        activeSegment = true;
        try {
            // Desktop dataTransferLK_Control order: sources take the previous CONTROL outputs,
            // including probe outputs, before the probes publish this step's measurements.
            // A sampled CONTROL publishes to sources and gates only after a sample, so live probe
            // outputs wired straight to them cannot leak between samples.
            // Each group publishes by its own clock: a source after a sample of its producer's group,
            // a gate at a sample of its own group.
            domainCoupler.transferControlToSources(circuitNetlist, this::producerPublished);
            domainCoupler.transferControlToThermalSources(thermal, this::producerPublished);
            domainCoupler.transferLkToControlProbes(circuitNetlist);
            domainCoupler.transferThermalToControlProbes(thermal);
            if (controlNetlist.hasCalculators()) executeDueGroups(time, samplingTime);
            else java.util.Arrays.fill(sampledNow, true);
            System.arraycopy(sampledNow, 0, outputsPublishedLastStep, 0, periods.length);
            domainCoupler.transferGateSignalsToLk(circuitNetlist, gate -> periods[groupOf(gate)] <= 0 || sampledNow[groupOf(gate)]);
        } catch (RuntimeException | Error failure) {
            closeAfterFailure(failure);
            throw failure;
        }
    }

    private boolean producerPublished(AbstractControlCalculatable producer) {
        int group = groupOf(producer);
        return periods[group] <= 0 || outputsPublishedLastStep[group];
    }

    /**
     * Runs each group every step, or only when a sample of that group is due. Due times are exactly k*Ts of the group,
     * so they do not drift. All due groups run in one pass of the global dependency order.
     */
    private void executeDueGroups(double time, double samplingTime) {
        boolean any = false;
        for (int group = 0; group < periods.length; group++) {
            double period = periods[group];
            groupSteps[group] = controlStep(group, dt);
            if (period <= 0 || nextSampleIndex[group] == SAMPLE_INDEX_BEYOND_PRECISION) {
                sampledNow[group] = true;
            } else {
                double gridWidth = Math.min(dt, period);
                // Equal grid and sample times can differ by a few ulps. Keep the margin below one grid step.
                double slack = Math.min(gridWidth / 4, Math.max(SAMPLE_TOLERANCE * gridWidth, 4 * Math.ulp(samplingTime)));
                double dueThrough = samplingTime + slack;
                sampledNow[group] = dueThrough >= nextSampleIndex[group] * period;
                if (sampledNow[group]) {
                    double index = Math.floor(dueThrough / period);
                    // Adjacent sample indices no longer fit in a double. The grid can publish only once per step.
                    nextSampleIndex[group] = index >= FIRST_INEXACT_SAMPLE_INDEX
                            ? SAMPLE_INDEX_BEYOND_PRECISION : (long) index + 1;
                }
            }
            any |= sampledNow[group];
        }
        if (any) controlNetlist.executeDue(groupSteps, sampledNow, time);
    }

    /**
     * Exports the held CONTROL values and the private calculator state after a committed step. It only reads.
     * Absolute time and the time calculator are not state. The sample phase is exported when CONTROL is sampled.
     *
     * @param circuitNetlist the electrical netlist whose sources hold CONTROL values
     * @param samplingTime the compensated time of the last committed step
     * @param sink receives the values
     */
    void exportPeriodicState(CircuitNetlist circuitNetlist, double samplingTime, PeriodicStateMonitor.Sink sink) {
        domainCoupler.exportHeldSources(circuitNetlist, sink);
        if (periods.length > 1) domainCoupler.exportHeldGates(sink);
        for (int index = 0; index < owners.size(); index++) {
            var owner = owners.get(index);
            String name = owner.getClass().getSimpleName() + "#" + index;
            if (!(owner instanceof TimeCalculator)) {
                sink.group(PeriodicStateMonitor.Unit.CONTROL, "CONTROL output of " + name);
                for (double[] output : owner._outputSignal) for (double value : output) sink.value(value);
            }
            if (owner instanceof PeriodicStateSource source) {
                sink.group(PeriodicStateMonitor.Unit.CONTROL, "CONTROL state of " + name);
                source.exportState(sink);
            }
        }
        for (int group = 0; group < periods.length; group++) {
            if (periods[group] <= 0) continue;
            sink.group(PeriodicStateMonitor.Unit.TIME, group == 0 ? "sample phase" : "sample phase of CONTROL group " + group);
            double period = periods[group];
            double phase = nextSampleIndex[group] == SAMPLE_INDEX_BEYOND_PRECISION
                    ? period - samplingTime % period : nextSampleIndex[group] * period - samplingTime;
            sink.value(phase);
            sink.flag(outputsPublishedLastStep[group]);
        }
    }

    /** @return the assembled calculators, for classification before a search starts */
    List<AbstractControlCalculatable> calculators() { return owners; }

    float[] sampleScope() {
        return hasScope() ? scopeExtractor.extractCurrentValues() : new float[0];
    }

    /** Ends the active segment once without resetting histories or closing owned resources. */
    void tearDownOnPause() {
        if (closed || !activeSegment) return;
        stateVersion++;
        activeSegment = false;
        throwFailure(tearDown(null));
    }

    /** Closes resources once, including resources acquired before calculator initialization. */
    @Override
    public void close() {
        if (closed) return;
        stateVersion++;
        closed = true;
        pendingInitialization = null;
        boolean tearDownSegment = activeSegment;
        activeSegment = false;
        Throwable failure = tearDownSegment ? tearDown(null) : null;
        for (var owner : owners) {
            if (owner instanceof AutoCloseable resource) {
                try {
                    resource.close();
                } catch (Throwable error) {
                    failure = appendFailure(failure, error);
                }
            }
        }
        throwFailure(failure);
    }

    private Throwable tearDown(Throwable failure) {
        for (var owner : owners) {
            try {
                owner.tearDownOnPause();
            } catch (Throwable error) {
                failure = appendFailure(failure, error);
            }
        }
        return failure;
    }

    private void closeAfterFailure(Throwable primary) {
        try {
            close();
        } catch (Throwable cleanup) {
            appendFailure(primary, cleanup);
        }
    }

    private static Throwable appendFailure(Throwable primary, Throwable next) {
        if (primary == null) return next;
        if (primary != next) primary.addSuppressed(next);
        return primary;
    }

    private static void throwFailure(Throwable failure) {
        if (failure instanceof Error error) throw error;
        if (failure instanceof RuntimeException error) throw error;
        if (failure != null) throw new IllegalStateException("A CONTROL resource could not close.", failure);
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("The CONTROL runtime is closed.");
    }
}
