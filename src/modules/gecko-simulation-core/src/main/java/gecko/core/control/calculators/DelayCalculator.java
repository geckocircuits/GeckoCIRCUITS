/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package gecko.core.control.calculators;

import gecko.core.control.PeriodicStateSource;
import gecko.core.control.StateSink;

import gecko.core.control.IsDtChangeSensitive;
import gecko.core.control.DelayHistory;

/** Adapts shared delay history to this runtime's signal arrays and lifecycle interfaces. */
public final class DelayCalculator extends AbstractSingleInputSingleOutputCalculator
        implements InitializableAtSimulationStart, IsDtChangeSensitive, PeriodicStateSource {
    private final DelayHistory history;

    /**
     * Creates a calculator with a deferred initial delay.
     *
     * @param delayTime the finite, nonnegative delay in seconds.
     * @throws IllegalArgumentException if the delay is invalid.
     */
    public DelayCalculator(final double delayTime) { history = new DelayHistory(delayTime); }

    @Override
    public void initWithNewDt(final double deltaT) { history.changeTimeStep(deltaT); }

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        history.restart(deltaT);
        _outputSignal[0][0] = 0;
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        _outputSignal[0][0] = history.step(_inputSignal[0][0], deltaT);
    }

    /**
     * Defers a delay change until the next restart or time-step callback.
     *
     * @param delayTime the finite, nonnegative delay in seconds.
     * @throws IllegalArgumentException if the delay is invalid.
     */
    public void setDelayTime(final double delayTime) { history.setDelayTime(delayTime); }

    /** Publishes the current historical sample without capturing an input. Use after initialization. */
    public void publishDelayedOutput() { _outputSignal[0][0] = history.publish(); }

    /** Captures the settled input after combinational execution. Use after publishing the delayed output. */
    public void captureCurrentInput() { history.capture(_inputSignal[0][0]); }

    /** @return the delay activated by the most recent lifecycle callback. */
    public double getDelayTime() { return history.getDelayTime(); }

    /** @return the configured delay without changing the active history. */
    public double getConfiguredDelayTime() { return history.getConfiguredDelayTime(); }

    /**
     * Checks the configured history size without changing active state.
     *
     * @param dt the finite, positive step in seconds
     * @throws IllegalArgumentException if the step or history size is invalid
     */
    public void validateTimeStep(double dt) { DelayHistory.validateHistorySize(getConfiguredDelayTime(), dt); }

    /**
     * Allocates a restart update without changing history or output.
     *
     * @param dt the finite, positive step in seconds
     * @return the action that commits this staged restart once
     * @throws IllegalArgumentException if the step or history size is invalid
     */
    public Runnable prepareInitialization(double dt) {
        var update = history.prepareRestart(dt);
        return () -> {
            update.commit();
            _outputSignal[0][0] = 0;
        };
    }

    /**
     * Allocates a resampling update without changing active history.
     *
     * @param dt the finite, positive step in seconds
     * @return the action that commits this staged step change once
     * @throws IllegalArgumentException if the step or history size is invalid
     */
    public Runnable prepareStepChange(double dt) { return history.prepareTimeStepChange(dt)::commit; }

    @Override
    public Runnable prepareTimeStepChange(double dt) { return prepareStepChange(dt); }

    @Override
    public void exportState(final StateSink sink) { history.exportChronological(sink); }
}
