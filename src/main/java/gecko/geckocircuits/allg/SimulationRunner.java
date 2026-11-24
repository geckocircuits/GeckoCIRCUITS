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
package gecko.geckocircuits.allg;

import gecko.core.simulation.SimulationSession;
import gecko.geckocircuits.circuit.NetListContainer;
import gecko.geckocircuits.circuit.SchematicEditor2;
import gecko.geckocircuits.circuit.SimulationsKern;
import gecko.geckocircuits.circuit.SimulationTraceLogger;
import gecko.geckocircuits.circuit.SolverSettings;
import gecko.geckocircuits.control.DataSaver;
import gecko.geckocircuits.control.NetzlisteCONTROL;
import gecko.geckocircuits.control.RegelBlock;
import gecko.geckocircuits.control.ReglerOSZI;
import gecko.geckocircuits.datacontainer.ContainerStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressFBWarnings(value = {"PA_PUBLIC_PRIMITIVE_ATTRIBUTE", "EI_EXPOSE_REP2"},
        justification = "Public kernel alias and frontend references preserve the legacy desktop interface")
public final class SimulationRunner {
    private static final Logger LOGGER = Logger.getLogger(SimulationRunner.class.getName());
    final MainWindow _mainwindow;
    final SchematicEditor2 _se;
    public SimulationsKern simKern;
    private final Object lifecycleMonitor = new Object();
    // Serialize numerical preparation and publication. Swing delivery always follows this monitor's release.
    private final Object preparationMonitor = new Object();
    private final LatestOwnedPresentation presentationUpdates = new LatestOwnedPresentation();
    private volatile PreparationOwner preparationOwner = new PreparationOwner(new Object(), null);
    private volatile Execution execution;
    private SimulationTraceLogger pendingTraceLogger;
    private final DesktopSimulationController controller;

    private record PreparationOwner(Object token, SolverSettings settings) { }
    private record Preparation(Object token, Execution previous, SimulationTraceLogger logger) { }
    private record Execution(Object token, SimulationsKern kernel, NetListContainer netlist,
            LegacySimulationFrontend presentation, SolverSettings settings,
            Consumer<ContainerStatus> originStatus, SimulationsKern.ExecutionTicket ticket,
            boolean preCalculation, Runnable activation) { }

    /** Sets a trace logger for the next calculation before its thread starts. */
    public void setPendingTraceLogger(SimulationTraceLogger logger) {
        synchronized (lifecycleMonitor) {
            pendingTraceLogger = logger;
        }
    }

    public SimulationRunner(final MainWindow mainwindow, final SchematicEditor2 schematicEntry) {
        this(mainwindow, schematicEntry, () -> NetListContainer.fabricStartSimulation(schematicEntry, null));
    }

    SimulationRunner(MainWindow mainwindow, SchematicEditor2 schematicEntry,
            java.util.function.Supplier<NetListContainer> prepareCorePresentation) {
        _mainwindow = mainwindow;
        _se = schematicEntry;
        controller = new DesktopSimulationController(mainwindow, schematicEntry, prepareCorePresentation);
    }

    private Preparation beginPreparation(Execution expected, boolean requireCurrent, boolean takeLogger,
            SolverSettings settings) {
        Preparation preparation;
        synchronized (lifecycleMonitor) {
            if (requireCurrent && !isCurrent(expected)) {
                return null;
            }
            Object token = new Object();
            preparationOwner = new PreparationOwner(token, settings);
            preparation = new Preparation(token, execution, takeLogger ? pendingTraceLogger : null);
            if (takeLogger) {
                pendingTraceLogger = null;
            }
        }
        return preparation;
    }

    private boolean isPreparing(Object token) {
        return preparationOwner.token() == token;
    }

    private void clearPreCalculationMode(Object failedToken, SolverSettings failedSettings) {
        synchronized (lifecycleMonitor) {
            PreparationOwner current = preparationOwner;
            if (current.token() == failedToken || current.settings() != failedSettings) {
                failedSettings.inPreCalculationMode = false;
            }
        }
    }

    private boolean isCurrent(Execution candidate) {
        return candidate != null && execution == candidate && isPreparing(candidate.token());
    }

    private Execution prepareExecution(Execution expected, boolean requireCurrent, boolean takeLogger,
            SolverSettings settings, Function<Preparation, Execution> initialize) {
        Preparation preparation = null;
        try {
            synchronized (preparationMonitor) {
                preparation = beginPreparation(expected, requireCurrent, takeLogger, settings);
                if (preparation == null) return null;
                if (preparation.previous() != null) preparation.previous().ticket().supersede();
                Execution initialized = initialize.apply(preparation);
                if (initialized == null) clearPreCalculationMode(preparation.token(), settings);
                return initialized;
            }
        } catch (RuntimeException | Error error) {
            if (preparation != null) {
                Object token = preparation.token();
                clearPreCalculationMode(token, settings);
                try {
                    LegacySimulationFrontend.onEventThread(() -> presentationUpdates.update(() -> isPreparing(token),
                            () -> _mainwindow.setMenuDuringSimulation(false, true, () -> isPreparing(token)),
                            () -> _mainwindow.jtfStatus.setText("Simulation aborted.")));
                } catch (RuntimeException presentationError) {
                    LOGGER.log(Level.WARNING, "Could not report simulation initialization failure", presentationError);
                }
            }
            throw error;
        }
    }

    private Execution publish(Preparation preparation, SimulationsKern kernel, NetListContainer netlist,
            LegacySimulationFrontend presentation, SolverSettings settings,
            Consumer<ContainerStatus> originStatus, SimulationsKern.ExecutionTicket ticket, boolean activate) {
        Object token = preparation.token();
        if (!isPreparing(token)) {
            ticket.supersede();
            return null;
        }
        Runnable activation = presentation.prepare(netlist, () -> isPreparing(token), activate);
        ticket.bindCompletion();
        Execution prepared = new Execution(token, kernel, netlist, presentation, settings,
                originStatus, ticket, settings.inPreCalculationMode, activation);
        boolean published;
        synchronized (lifecycleMonitor) {
            published = isPreparing(token);
            if (published) {
                execution = prepared;
                simKern = kernel;
            }
        }
        if (!published) {
            ticket.supersede();
            return null;
        }
        return prepared;
    }

    public void startCalculation(boolean createNewSimThread, SolverSettings settings) throws Exception {
        _se.requireCompleteConversion();
        controller.reset();
        if (createNewSimThread && controller.startAsync(settings)) {
            execution = null;
            simKern = null;
            return;
        }
        if (!createNewSimThread && controller.start(settings)) {
            execution = null;
            simKern = null;
            return;
        }
        Execution prepared = prepareExecution(null, false, true, settings, preparation -> prepareStart(preparation, settings));
        if (prepared != null) {
            updatePresentation(prepared, () -> setMenus(prepared, true), prepared.activation());
            startWorker(prepared, createNewSimThread, !createNewSimThread);
        }
    }

    public double getCurrentTime() {
        return controller.hasCoreExecution() ? controller.getCurrentTime()
                : simKern == null ? 0 : simKern.getZeitAktuell();
    }

    /** Returns core lifecycle status for GUI clients; null while the legacy route is active. */
    public SimulationSession.Status getCoreStatus() {
        return controller.getCoreStatus();
    }

    private Execution prepareStart(Preparation preparation, SolverSettings settings) {
        Object token = preparation.token();
        LegacySimulationFrontend presentation = new LegacySimulationFrontend(() -> isPreparing(token));
        SimulationsKern kernel = new SimulationsKern(settings.SOLVER_TYPE.getValue(), presentation);
        SimulationsKern.ExecutionTicket ticket = kernel.reserveExecution(() -> isPreparing(token));
        double end = settings._tDURATION.getValue();
        double dt = settings.dt.getValue();
        settings.inPreCalculationMode = settings._T_pre.getValue() > 0;
        if (settings.inPreCalculationMode) {
            end = settings._T_pre.getValue();
            dt = settings._dt_pre.getValue();
        }
        settings._dt_ALT = dt;
        NetListContainer netlist = NetListContainer.fabricStartSimulation(_se, kernel);
        Consumer<ContainerStatus> originStatus = NetzlisteCONTROL.globalData.captureStatusUpdater();
        if (!isPreparing(token)) {
            ticket.supersede();
            return null;
        }
        kernel.initSimulation(dt, 0, 0, end, settings._tPAUSE.getValue(), true, netlist, false);
        kernel.initialisiereCONTROLatSimulationStart(dt);
        if (preparation.logger() != null) {
            kernel.setTraceLogger(preparation.logger());
        }
        return publish(preparation, kernel, netlist, presentation, settings, originStatus, ticket, true);

    }

    public void continueCalculation(boolean createNewSimThread, SolverSettings settings) throws Exception {
        _se.requireCompleteConversion();
        if (controller.hasAsyncExecution()) {
            controller.continueAsync(settings);
            return;
        }
        Execution prepared = prepareExecution(null, false, false, settings, preparation -> prepareContinuation(preparation, settings));
        activateContinuation(prepared, createNewSimThread);
    }

    private Execution prepareContinuation(Preparation preparation, SolverSettings settings) {
        Object token = preparation.token();
        Execution previous = preparation.previous();
        if (previous == null) {
            throw new IllegalStateException("Initialize simulation before continuation");
        }
        SimulationsKern kernel = previous.kernel();
        SimulationsKern.ExecutionTicket ticket = kernel.reserveExecution(() -> isPreparing(token));
        if (!isPreparing(token)) {
            return null;
        }
        NetListContainer netlist = previous.netlist();
        Consumer<ContainerStatus> originStatus = previous.originStatus();
        double current = kernel.getZeitAktuell();
        double start = kernel.getTSTART();
        double end = kernel.getTEND();
        if (current >= end) {
            start = end;
            end += settings._tDURATION.getValue();
            kernel.setZeiten(start, end, settings.dt.getValue());
            boolean recalculate = settings.dt.getValue() != settings._dt_ALT;
            settings._dt_ALT = settings.dt.getValue();
            netlist = NetListContainer.fabricContinueSimulation(_se, kernel, netlist);
            originStatus = NetzlisteCONTROL.globalData.captureStatusUpdater();
            if (!isPreparing(token)) {
                ticket.supersede();
                return null;
            }
            kernel.initSimulation(settings.dt.getValue(), start, current, end,
                    settings._tPAUSE.getValue(), false, netlist, recalculate);
            kernel.setInitialConditionsFromContinue();
        } else {
            kernel._simulationStatus = SimulationsKern.SimulationStatus.RUNNING;
        }
        return publish(preparation, kernel, netlist, previous.presentation(), settings,
                originStatus, ticket, true);
    }

    private void activateContinuation(Execution prepared, boolean createNewSimThread) {
        if (prepared != null) {
            commitStatus(prepared, ContainerStatus.RUNNING);
            updatePresentation(prepared, () -> setMenus(prepared, true), prepared.activation());
            startWorker(prepared, createNewSimThread, false);
        }
    }

    /** Stops the captured execution without pausing a replacement started by its completion callbacks. */
    public void endSimulation() {
        if (controller.endAsync()) return;
        Execution owner = execution;
        if (owner == null || !owner.ticket().endSimulation()) return;
        if (owner.ticket().pause()) {
            commitStatus(owner, ContainerStatus.PAUSED);
            updatePresentation(owner,
                    () -> _mainwindow.setzeSTATUS("Stopped after  "),
                    () -> setMenus(owner, false));
        }
    }

    public void pauseSimulation() {
        if (controller.pauseAsync()) return;
        Execution current = execution;
        if (current != null && current.ticket().pause()) {
            commitStatus(current, ContainerStatus.PAUSED);
            updatePresentation(current, () -> setMenus(current, false));
        }
    }

    public void initSim(double dt, double end) {
        _se.requireCompleteConversion();
        SolverSettings settings = MainWindow._solverSettings;
        settings._tDURATION.setValueWithoutUndo(end);
        settings.dt.setValueWithoutUndo(dt);
        controller.reset();
        try {
            if (controller.initializeScript(settings, dt, end)) {
                execution = null;
                simKern = null;
                return;
            }
        } catch (Exception error) {
            throw new IllegalStateException("Could not initialize core script simulation", error);
        }
        initializeScript(settings, end, dt, true);
    }

    /** Advances one script step through the selected backend. */
    public void simulateScriptOneStep() throws Exception {
        if (controller.hasCoreExecution()) {
            controller.scriptStep(MainWindow._solverSettings.dt.getValue(),
                    MainWindow._solverSettings._tDURATION.getValue());
        } else {
            simKern.simulateOneStep();
        }
    }

    /** Advances a script simulation by relative time through the selected backend. */
    public void simulateScriptTime(double time) throws Exception {
        if (controller.hasCoreExecution()) {
            double end = MainWindow._solverSettings._tDURATION.getValue();
            double requestedEnd = controller.getCurrentTime() + time;
            boolean overReach = requestedEnd > end;
            double target = Math.min(requestedEnd, end);
            controller.scriptRunUntil(target);
            if (overReach) {
                throw new Exception("Specified simulation time goes beyond specified simulated end time; simulated only up to end time");
            }
        } else {
            simKern.simulateTime(time);
        }
    }

    public void initSim() {
        initSim(0, 0);
    }

    public void external_init(double end) {
        _se.requireCompleteConversion();
        SolverSettings settings = MainWindow._solverSettings;
        controller.reset();
        try {
            if (controller.initializeExternal(settings, end)) {
                execution = null;
                simKern = null;
                return;
            }
        } catch (Exception error) {
            throw new IllegalStateException("Could not initialize core external simulation", error);
        }
        initializeScript(settings, end, settings.dt.getValue(), false);
    }

    public void external_step(double time) {
        if (controller.hasCoreExecution()) {
            try {
                controller.externalStep(time);
            } catch (Exception error) {
                throw new IllegalStateException("Could not advance core external simulation", error);
            }
        } else {
            simKern.external_step(time);
        }
    }

    public double external_getdt() {
        return controller.hasCoreExecution() ? controller.externalStepWidth() : simKern.getdt();
    }

    public void external_end() {
        if (controller.hasCoreExecution()) controller.endAsync();
        else simKern.external_end();
    }

    private void initializeScript(SolverSettings settings, double end, double dt, boolean usePreCalculation) {
        Execution prepared = prepareExecution(null, false, false, settings,
                preparation -> prepareScript(preparation, settings, end, dt, usePreCalculation));
        if (prepared != null) {
            List<Runnable> changes = new ArrayList<>();
            if (usePreCalculation) {
                changes.add(() -> setMenus(prepared, true));
            }
            changes.add(prepared.activation());
            changes.add(() -> _mainwindow.jtfStatus.setText("Starting Simulation ... "));
            addScopeBoundaries(prepared, changes);
            updatePresentation(prepared, changes.toArray(Runnable[]::new));
        }
    }

    private Execution prepareScript(Preparation preparation, SolverSettings settings, double end, double dt,
            boolean usePreCalculation) {
        Object token = preparation.token();
        LegacySimulationFrontend presentation = new LegacySimulationFrontend(() -> isPreparing(token));
        SimulationsKern kernel = new SimulationsKern(settings.SOLVER_TYPE.getValue(), presentation);
        SimulationsKern.ExecutionTicket ticket = kernel.reserveExecution(() -> isPreparing(token));
        if (usePreCalculation && settings.inPreCalculationMode) {
            end = settings._T_pre.getValue();
            dt = settings._dt_pre.getValue();
        }
        if (usePreCalculation) {
            settings._dt_ALT = dt;
        }
        NetListContainer netlist = NetListContainer.fabricStartSimulation(_se, kernel);
        Consumer<ContainerStatus> originStatus = NetzlisteCONTROL.globalData.captureStatusUpdater();
        if (!isPreparing(token)) {
            ticket.supersede();
            return null;
        }
        kernel.initSimulation(dt, 0, 0, end, settings._tPAUSE.getValue(), true, netlist, false);
        kernel.initialisiereCONTROLatSimulationStart(dt);
        return publish(preparation, kernel, netlist, presentation, settings, originStatus, ticket, false);
    }

    private void setMenus(Execution owner, boolean running) {
        _mainwindow.setMenuDuringSimulation(running, !running, () -> isCurrent(owner));
    }

    private void addScopeBoundaries(Execution owner, List<Runnable> changes) {
        for (RegelBlock block : owner.netlist()._nlControl._orderedControlBlocks) {
            if (block instanceof ReglerOSZI scope) {
                changes.add(() -> scope.setSimulationTimeBoundaries(owner.kernel().getTSTART(), owner.kernel().getTEND()));
            }
        }
    }

    private void startWorker(Execution owner, boolean asynchronous, boolean propagateErrors) {
        RunThreadRun worker = new RunThreadRun(owner, propagateErrors);
        if (asynchronous) {
            Thread thread = new Thread(worker);
            thread.setName("simulationThread");
            thread.start();
        } else {
            worker.run();
        }
    }

    private void commitStatus(Execution owner, ContainerStatus status) {
        try {
            LegacySimulationFrontend.onEventThread(() -> {
                if (isCurrent(owner)) {
                    owner.originStatus().accept(status);
                }
            });
        } catch (RuntimeException error) {
            LOGGER.log(Level.WARNING, "Could not update simulation recording", error);
        }
    }

    private void updatePresentation(Execution owner, Runnable... changes) {
        try {
            LegacySimulationFrontend.onEventThread(() -> presentationUpdates.update(() -> isCurrent(owner), changes));
        } catch (RuntimeException error) {
            LOGGER.log(Level.WARNING, "Could not update simulation presentation", error);
        }
    }

    private void waitForDataSavers() {
        int counter = 0;
        while (DataSaver.WAIT_COUNTER.get() != 0 && counter++ < 100) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException error) {
                LOGGER.log(Level.SEVERE, "Could not wait for simulation data", error);
            }
        }
    }

    private final class RunThreadRun implements Runnable {
        private final Execution owner;
        private final boolean propagateErrors;
        private long started;

        private RunThreadRun(Execution owner, boolean propagateErrors) {
            this.owner = owner;
            this.propagateErrors = propagateErrors;
        }

        @Override
        public void run() {
            Throwable failure = null;
            try {
                List<Runnable> changes = new ArrayList<>();
                addScopeBoundaries(owner, changes);
                changes.add(() -> _mainwindow.jtfStatus.setText("Starting Simulation ... "));
                updatePresentation(owner, changes.toArray(Runnable[]::new));
                started = System.currentTimeMillis();
                try {
                    owner.ticket().run();
                } catch (OutOfMemoryError cause) {
                    OutOfMemoryError error = new OutOfMemoryError("Could not allocate enough java RAM memory for the simulation!");
                    error.initCause(cause);
                    throw error;
                }
                if (owner.preCalculation() && isCurrent(owner)) {
                    Execution next = prepareExecution(owner, true, false, owner.settings(), preparation -> {
                            owner.settings().inPreCalculationMode = false;
                            return prepareContinuation(preparation, owner.settings());
                    });
                    activateContinuation(next, false);
                    return;
                }
            } catch (Throwable error) {
                failure = error;
            } finally {
                if (owner.preCalculation()) {
                    clearPreCalculationMode(owner.token(), owner.settings());
                }
            }
            try {
                if (owner.ticket().finish()) {
                    commitStatus(owner, ContainerStatus.PAUSED);
                }
            } catch (Throwable teardownError) {
                if (failure == null) {
                    failure = teardownError;
                } else if (failure != teardownError) {
                    failure.addSuppressed(teardownError);
                }
            }
            if (failure != null) {
                commitStatus(owner, ContainerStatus.PAUSED);
                updatePresentation(owner, () -> setMenus(owner, false),
                        () -> _mainwindow.jtfStatus.setText("Simulation aborted."));
                if (propagateErrors) {
                    throw new RuntimeException(failure);
                }
                failure.printStackTrace();
            } else {
                long elapsed = System.currentTimeMillis() - started;
                updatePresentation(owner, () -> _mainwindow.jtfStatus.setzeStatusRechenzeit(elapsed),
                        () -> setMenus(owner, false));
            }
            waitForDataSavers();
        }
    }
}
