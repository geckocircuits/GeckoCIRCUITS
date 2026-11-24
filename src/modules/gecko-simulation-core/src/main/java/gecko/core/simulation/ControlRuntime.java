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
    private final double samplePeriod;
    private double dt;
    private long nextSampleIndex;
    private boolean outputsPublishedLastStep = true;
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
        this.samplePeriod = samplePeriod;
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
    private double controlStep(double electricalStep) {
        return Math.max(samplePeriod, electricalStep);
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
            if (initialization == null) controlNetlist.initializeAtSimulationStart(controlStep(dt));
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
        Runnable staged = beforeStart ? controlNetlist.prepareInitialization(controlStep(newDt))
                : samplePeriod > 0 && controlStep(newDt) == controlStep(dt) ? () -> { }
                : controlNetlist.prepareTimeStepChange(controlStep(newDt));
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

    void applyPendingGates(CircuitNetlist circuitNetlist) {
        requireOpen();
        domainCoupler.applyGateSignals(circuitNetlist);
    }

    void advanceAfterElectricalStep(CircuitNetlist circuitNetlist, double time) {
        advanceAfterElectricalStep(circuitNetlist, null, time);
    }

    /** As above, with the thermal network that solved after the electrical step. Null runs the electrical path alone. */
    void advanceAfterElectricalStep(CircuitNetlist circuitNetlist, ThermalNetwork thermal, double time) {
        initializeAtSimulationStart();
        stateVersion++;
        activeSegment = true;
        try {
            // Desktop dataTransferLK_Control order: sources take the previous CONTROL outputs,
            // including probe outputs, before the probes publish this step's measurements.
            // A sampled CONTROL publishes to sources and gates only after a sample, so live probe
            // outputs wired straight to them cannot leak between samples.
            if (samplePeriod <= 0 || outputsPublishedLastStep) {
                domainCoupler.transferControlToSources(circuitNetlist);
                domainCoupler.transferControlToThermalSources(thermal);
            }
            domainCoupler.transferLkToControlProbes(circuitNetlist);
            domainCoupler.transferThermalToControlProbes(thermal);
            boolean sampled = !controlNetlist.hasCalculators() || executeDueSample(time);
            outputsPublishedLastStep = sampled;
            if (samplePeriod <= 0 || sampled) domainCoupler.transferGateSignalsToLk(circuitNetlist);
        } catch (RuntimeException | Error failure) {
            closeAfterFailure(failure);
            throw failure;
        }
    }

    /** Runs CONTROL every step, or only when a sample is due. Due times are exactly k*Ts, so they do not drift. */
    private boolean executeDueSample(double time) {
        if (samplePeriod <= 0) {
            controlNetlist.executeTimeStep(dt, time);
            return true;
        }
        double slack = SAMPLE_TOLERANCE * Math.min(dt, samplePeriod);
        if (time < nextSampleIndex * samplePeriod - slack) return false;
        controlNetlist.executeTimeStep(controlStep(dt), time);
        nextSampleIndex = (long) Math.floor((time + slack) / samplePeriod) + 1;
        return true;
    }

    /**
     * Exports the held CONTROL values and the private calculator state after a committed step. It only reads.
     * Absolute time and the time calculator are not state. The sample phase is exported when CONTROL is sampled.
     *
     * @param circuitNetlist the electrical netlist whose sources hold CONTROL values
     * @param time the time of the last committed step
     * @param sink receives the values
     */
    void exportPeriodicState(CircuitNetlist circuitNetlist, double time, PeriodicStateMonitor.Sink sink) {
        domainCoupler.exportHeldSources(circuitNetlist, sink);
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
        if (samplePeriod > 0) {
            sink.group(PeriodicStateMonitor.Unit.TIME, "sample phase");
            sink.value(nextSampleIndex * samplePeriod - time);
            sink.flag(outputsPublishedLastStep);
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
