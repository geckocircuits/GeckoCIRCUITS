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
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.FromExternalCalculator;
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
    private double dt;
    private long stateVersion;
    private Runnable pendingInitialization;
    private boolean initialized;
    private boolean activeSegment;
    private boolean closed;

    private ControlRuntime(ControlCalculatorFactory.BuildResult build, double dt) {
        if (!Double.isFinite(dt) || dt <= 0) {
            throw new IllegalArgumentException("Time step must be finite and positive.");
        }
        this.dt = dt;
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
        return fromDeferred(new ControlCalculatorFactory().buildCircuit(model, dt, electricalNetlist), dt);
    }

    /** Takes ownership of the supplied calculators and their closeable resources. */
    static ControlRuntime fromDeferred(ControlCalculatorFactory.BuildResult build, double dt) {
        return new ControlRuntime(build, dt);
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
            if (initialization == null) controlNetlist.initializeAtSimulationStart(dt);
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
        Runnable staged = beforeStart ? controlNetlist.prepareInitialization(newDt)
                : controlNetlist.prepareTimeStepChange(newDt);
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

    void applyPendingGates(CircuitNetlist circuitNetlist) {
        requireOpen();
        domainCoupler.applyGateSignals(circuitNetlist);
    }

    void advanceAfterElectricalStep(CircuitNetlist circuitNetlist, double time) {
        initializeAtSimulationStart();
        stateVersion++;
        activeSegment = true;
        try {
            // Desktop dataTransferLK_Control order: sources take the previous CONTROL outputs,
            // including probe outputs, before the probes publish this step's measurements.
            domainCoupler.transferControlToSources(circuitNetlist);
            domainCoupler.transferLkToControlProbes(circuitNetlist);
            if (controlNetlist.hasCalculators()) controlNetlist.executeTimeStep(dt, time);
            domainCoupler.transferGateSignalsToLk(circuitNetlist);
        } catch (RuntimeException | Error failure) {
            closeAfterFailure(failure);
            throw failure;
        }
    }

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
