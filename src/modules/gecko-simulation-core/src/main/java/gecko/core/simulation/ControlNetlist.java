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

import gecko.core.control.NotCalculateableMarker;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.DelayCalculator;
import gecko.core.control.calculators.PT2Calculator;
import gecko.core.control.calculators.InitializableAtSimulationStart;
import gecko.core.control.IsDtChangeSensitive;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Headless control block netlist for executing control calculators.
 * Manages execution order of control blocks in a simulation without GUI dependencies.
 *
 * <p>This class was extracted from {@code gecko.geckocircuits.control.NetzlisteCONTROL} to provide
 * GUI-free control block execution for REST APIs, batch processing, and headless simulations.
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * List<AbstractControlCalculatable> orderedCalcs = buildCalculators();
 * ControlNetlist netlist = ControlNetlist.fromOrderedCalculators(orderedCalcs);
 *
 * double dt = 1e-6;  // time step
 * for (double time = 0; time <= 0.01; time += dt) {
 *     netlist.executeTimeStep(dt, time);
 *     // Read output signals from calculators
 * }
 * }</pre>
 */
public class ControlNetlist {

    private AbstractControlCalculatable[] sortedCalculators = new AbstractControlCalculatable[0];
    private double simulationTime;
    /** The controller step each sample-clock group last ran with. One group unless {@link #configureGroups} says more. */
    private double[] lastSteps = {Double.NaN};
    private long stateVersion;
    private IdentityHashMap<AbstractControlCalculatable, Integer> groups;
    private double[] scratchSteps = new double[1];
    private boolean[] allDue = {true};
    private BiFunction<double[], Boolean, List<AbstractControlCalculatable>> dependencyOrder;

    /** Sets the planner, which receives the controller step of every group and returns the global execution order. */
    void setDependencyOrder(BiFunction<double[], Boolean, List<AbstractControlCalculatable>> order) {
        dependencyOrder = order;
        stateVersion++;
    }

    /**
     * Splits the calculators into sample-clock groups. A calculator that the map omits belongs to group 0.
     * Every calculator still runs in the one global order. A group has its own controller step and delay clocks.
     */
    void configureGroups(IdentityHashMap<AbstractControlCalculatable, Integer> assignment, int count) {
        groups = assignment;
        lastSteps = new double[count];
        java.util.Arrays.fill(lastSteps, Double.NaN);
        scratchSteps = new double[count];
        allDue = new boolean[count];
        java.util.Arrays.fill(allDue, true);
        stateVersion++;
    }

    int groupOf(AbstractControlCalculatable calculator) {
        return groups == null ? 0 : groups.getOrDefault(calculator, 0);
    }

    private double[] uniform(double dt) {
        java.util.Arrays.fill(scratchSteps, dt);
        return scratchSteps;
    }

    /**
     * Starts calculator histories and clocks for a new run.
     * All delay sizes and staged buffers are ready before lifecycle callbacks change state.
     *
     * @param dt the finite, positive step in seconds
     * @throws IllegalArgumentException if the step or a configured delay size is invalid
     * @throws ControlCalculatorFactory.DependencyCycleException if the selected step creates an instantaneous cycle
     */
    public void initializeAtSimulationStart(double dt) {
        prepareInitialization(dt).run();
    }

    /**
     * Stages dependency checks and delay buffers before a simulation start.
     * Preparation does not invoke initialization callbacks or change clocks.
     * The caller must serialize calculator access until commit.
     * Commit invokes initialization callbacks and starts a new history.
     * This interface does not roll back arbitrary extension callback failures.
     *
     * @param dt the finite, positive step in seconds
     * @return a single-use start action
     * @throws IllegalArgumentException if the step, input widths, or a delay size is invalid
     * @throws IllegalStateException if the action is stale or has already committed
     * @throws ControlCalculatorFactory.DependencyCycleException if the selected step creates an instantaneous cycle
     */
    public Runnable prepareInitialization(double dt) {
        return prepareInitialization(uniform(dt).clone());
    }

    /** As above, with one controller step per sample-clock group. */
    Runnable prepareInitialization(double[] steps) {
        validateSteps(steps);
        var planned = dependencyOrder == null ? sortedCalculators
                : dependencyOrder.apply(steps, true).toArray(AbstractControlCalculatable[]::new);
        for (var calculator : sortedCalculators) {
            if (calculator != null) calculator.validateInputSignalWidths();
        }
        var delayUpdates = prepareDelayUpdates(steps, true, null);
        return stagedTransition(() -> {
            for (var calculator : sortedCalculators) {
                if (calculator == null) continue;
                calculator.setSimulationTime(0);
                if (!(calculator instanceof DelayCalculator) && calculator instanceof InitializableAtSimulationStart initializable) {
                    initializable.initializeAtSimulationStart(steps[groupOf(calculator)]);
                }
            }
            for (int index = 0; index < delayUpdates.size(); index++) delayUpdates.get(index).run();
            simulationTime = 0;
            lastSteps = steps.clone();
            sortedCalculators = planned;
        });
    }

    /**
     * Creates an empty ControlNetlist with no calculators.
     * Suitable for circuits with no control blocks.
     */
    public ControlNetlist() {
        this.simulationTime = 0.0;
    }

    /**
     * Sets the ordered list of calculators to execute.
     * Must be called before executeTimeStep() to execute any calculations.
     * Replaces factory dependency planning. The caller supplies the execution order.
     *
     * @param calculators the list of control calculators in execution order
     * @throws NullPointerException if calculators is null
     */
    public void setSortedCalculators(List<AbstractControlCalculatable> calculators) {
        if (calculators == null) {
            throw new NullPointerException("Calculators list cannot be null");
        }
        this.sortedCalculators = calculators.toArray(new AbstractControlCalculatable[0]);
        dependencyOrder = null;
        stateVersion++;
    }

    /**
     * Execute all control calculators for one simulation time step.
     * Combinational calculators execute in the supplied order.
     * Buffered delays publish history first and capture settled inputs last.
     *
     * @param dt the time step size in seconds
     * @param time the current simulation time in seconds
     * @throws IllegalArgumentException if the step or a configured delay size is invalid
     * @throws ControlCalculatorFactory.DependencyCycleException if a step change creates an instantaneous cycle
     */
    public void executeTimeStep(double dt, double time) {
        executeDue(uniform(dt), allDue, time);
    }

    /**
     * Executes the calculators of the due groups in the one global order, each with the step of its group.
     * A calculator of a group that is not due keeps its output, history and clock.
     * The array arguments hold one entry per group and are not retained.
     */
    void executeDue(double[] steps, boolean[] due, double time) {
        validateSteps(steps);
        boolean anyUninitialized = false;
        boolean anyChange = false;
        for (int group = 0; group < steps.length; group++) {
            if (Double.isNaN(lastSteps[group])) anyUninitialized = true;
            else if (lastSteps[group] != steps[group]) anyChange = true;
        }
        boolean[] changed = null;
        if (anyChange) {
            // Every write replaces the whole array: all NaN before initialization, all finite after. So here all are finite.
            changed = new boolean[steps.length];
            for (int group = 0; group < steps.length; group++) changed[group] = lastSteps[group] != steps[group];
        }
        var planned = dependencyOrder != null && (anyUninitialized || anyChange)
                ? dependencyOrder.apply(steps, anyChange).toArray(AbstractControlCalculatable[]::new) : sortedCalculators;
        var delayUpdates = anyChange ? prepareDelayUpdates(steps, false, changed) : List.<Runnable>of();
        stateVersion++;
        if (anyChange) {
            for (var calculator : sortedCalculators) {
                if (!(calculator instanceof DelayCalculator) && calculator instanceof IsDtChangeSensitive sensitive
                        && changed[groupOf(calculator)]) sensitive.initWithNewDt(steps[groupOf(calculator)]);
            }
        }
        delayUpdates.forEach(Runnable::run);
        sortedCalculators = planned;
        System.arraycopy(steps, 0, lastSteps, 0, steps.length);
        this.simulationTime = time;
        for (AbstractControlCalculatable calc : sortedCalculators) {
            if (calc != null && due[groupOf(calc)]) calc.setSimulationTime(time);
        }
        for (AbstractControlCalculatable calc : sortedCalculators) {
            int group = groupOf(calc);
            if (!due[group]) continue;
            double dt = steps[group];
            if (calc instanceof PT2Calculator pt2) {
                pt2.publishPreviousInputOutput(dt);
            }
            if (calc instanceof DelayCalculator delay && dt <= delay.getDelayTime()) {
                delay.publishDelayedOutput();
            }
        }
        for (AbstractControlCalculatable calc : sortedCalculators) {
            int group = groupOf(calc);
            if (!due[group]) continue;
            double dt = steps[group];
            if (calc instanceof PT2Calculator) continue;
            if (calc instanceof DelayCalculator delay && dt <= delay.getDelayTime()) {
                continue;
            }
            if (calc != null && !(calc instanceof NotCalculateableMarker)) {
                calc.berechneYOUT(dt);
            }
        }
        for (AbstractControlCalculatable calc : sortedCalculators) {
            int group = groupOf(calc);
            if (!due[group]) continue;
            double dt = steps[group];
            if (calc instanceof PT2Calculator pt2) {
                pt2.captureCurrentInput();
            }
            if (calc instanceof DelayCalculator delay && dt <= delay.getDelayTime()) {
                delay.captureCurrentInput();
            }
        }
    }

    /**
     * Stages dependency planning and calculator updates without changing active state.
     * The caller must serialize preparation, commit, and calculator access.
     * A netlist mutation invalidates every pending transition.
     * Staged callbacks must commit without allocation or failure.
     * This interface does not roll back arbitrary extension callback failures.
     *
     * @param dt the finite, positive step in seconds
     * @return a single-use commit action that leaves the simulation clock unchanged
     * @throws IllegalArgumentException if the step or a configured history size is invalid
     * @throws IllegalStateException if initialization is missing, or the commit is stale or repeated
     * @throws UnsupportedOperationException if a calculator cannot stage its time-step callback
     * @throws ControlCalculatorFactory.DependencyCycleException if the selected step creates an instantaneous cycle
     */
    public Runnable prepareTimeStepChange(double dt) {
        return prepareTimeStepChange(uniform(dt).clone(), null);
    }

    /**
     * As above, with one controller step per group. Only groups that the mask selects stage callbacks and
     * resample delay histories. A null mask selects every group.
     */
    Runnable prepareTimeStepChange(double[] steps, boolean[] only) {
        validateSteps(steps);
        for (double last : lastSteps) {
            if (Double.isNaN(last)) {
                throw new IllegalStateException("The CONTROL netlist requires initialization before a staged step change.");
            }
        }
        var planned = dependencyOrder == null ? sortedCalculators
                : dependencyOrder.apply(steps, true).toArray(AbstractControlCalculatable[]::new);
        for (var calculator : sortedCalculators) {
            if (calculator instanceof DelayCalculator delay) delay.validateTimeStep(steps[groupOf(calculator)]);
        }
        var updates = new ArrayList<Runnable>();
        for (var calculator : sortedCalculators) {
            int group = groupOf(calculator);
            if (calculator instanceof IsDtChangeSensitive sensitive && (only == null || only[group])) {
                updates.add(java.util.Objects.requireNonNull(sensitive.prepareTimeStepChange(steps[group]),
                        "A staged CONTROL callback must supply a commit action."));
            }
        }
        return stagedTransition(() -> {
            for (int index = 0; index < updates.size(); index++) updates.get(index).run();
            sortedCalculators = planned;
            lastSteps = steps.clone();
        });
    }

    private Runnable stagedTransition(Runnable commit) {
        long expectedVersion = stateVersion;
        return new Runnable() {
            private boolean used;

            @Override
            public void run() {
                if (used || expectedVersion != stateVersion) {
                    throw new IllegalStateException("The CONTROL transition is stale or has already committed.");
                }
                used = true;
                stateVersion++;
                commit.run();
            }
        };
    }

    private List<Runnable> prepareDelayUpdates(double[] steps, boolean restart, boolean[] only) {
        var updates = new ArrayList<Runnable>();
        for (var calculator : sortedCalculators) {
            if (calculator instanceof DelayCalculator delay) delay.validateTimeStep(steps[groupOf(calculator)]);
        }
        for (var calculator : sortedCalculators) {
            int group = groupOf(calculator);
            if (calculator instanceof DelayCalculator delay && (only == null || only[group])) {
                updates.add(restart ? delay.prepareInitialization(steps[group]) : delay.prepareStepChange(steps[group]));
            }
        }
        return updates;
    }

    private void validateSteps(double[] steps) {
        if (steps.length != lastSteps.length) throw new IllegalArgumentException("One step per CONTROL group is required.");
        for (double step : steps) validateStep(step);
    }

    private static void validateStep(double dt) {
        if (!Double.isFinite(dt) || dt <= 0) throw new IllegalArgumentException("Time step must be finite and positive.");
    }

    /**
     * Returns the number of calculators managed by this netlist.
     *
     * @return the number of control calculators
     */
    public int getCalculatorCount() {
        return sortedCalculators.length;
    }

    /**
     * Returns the current simulation time from the last executeTimeStep() call.
     *
     * @return the simulation time in seconds
     */
    public double getSimulationTime() {
        return simulationTime;
    }

    /**
     * Returns true if this netlist has any calculators to execute.
     *
     * @return true if calculator count > 0, false otherwise
     */
    public boolean hasCalculators() {
        return sortedCalculators.length > 0;
    }

    /**
     * Returns a copy of the internal calculator array.
     * Useful for inspection and debugging.
     *
     * @return array of sorted calculators
     */
    public AbstractControlCalculatable[] getSortedCalculators() {
        AbstractControlCalculatable[] copy = new AbstractControlCalculatable[sortedCalculators.length];
        System.arraycopy(sortedCalculators, 0, copy, 0, sortedCalculators.length);
        return copy;
    }

    // ========== Factory Methods ==========

    /**
     * Create an empty ControlNetlist with no calculators.
     * Suitable for circuits that have no control blocks.
     *
     * @return a new ControlNetlist with 0 calculators
     */
    public static ControlNetlist createEmpty() {
        return new ControlNetlist();
    }

    /**
     * Create a ControlNetlist from a pre-ordered list of calculators.
     * The calculators must already be sorted in the correct execution order.
     *
     * @param orderedCalculators list of calculators in execution order
     * @return a new ControlNetlist configured with the provided calculators
     * @throws NullPointerException if orderedCalculators is null
     */
    public static ControlNetlist fromOrderedCalculators(List<AbstractControlCalculatable> orderedCalculators) {
        if (orderedCalculators == null) {
            throw new NullPointerException("Ordered calculators list cannot be null");
        }
        ControlNetlist netlist = new ControlNetlist();
        netlist.setSortedCalculators(orderedCalculators);
        return netlist;
    }

    /**
     * Create a ControlNetlist from a pre-ordered array of calculators.
     * The calculators must already be sorted in the correct execution order.
     *
     * @param orderedCalculators array of calculators in execution order
     * @return a new ControlNetlist configured with the provided calculators
     * @throws NullPointerException if orderedCalculators is null
     */
    public static ControlNetlist fromOrderedCalculators(AbstractControlCalculatable[] orderedCalculators) {
        if (orderedCalculators == null) {
            throw new NullPointerException("Ordered calculators array cannot be null");
        }
        ControlNetlist netlist = new ControlNetlist();
        netlist.sortedCalculators = new AbstractControlCalculatable[orderedCalculators.length];
        System.arraycopy(orderedCalculators, 0, netlist.sortedCalculators, 0, orderedCalculators.length);
        return netlist;
    }

    /**
     * Returns an unmodifiable list of the current calculators.
     *
     * @return list view of the sorted calculators
     */
    public List<AbstractControlCalculatable> getCalculatorsList() {
        List<AbstractControlCalculatable> list = new ArrayList<>(sortedCalculators.length);
        for (AbstractControlCalculatable calc : sortedCalculators) {
            list.add(calc);
        }
        return Collections.unmodifiableList(list);
    }
}
