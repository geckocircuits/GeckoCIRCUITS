package gecko.geckocircuits.allg;

import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.simulation.SessionHost;
import gecko.core.simulation.SimulationSession;
import gecko.geckocircuits.circuit.EditorCircuitCapture;
import gecko.geckocircuits.circuit.NetListContainer;
import gecko.geckocircuits.circuit.SchematicEditor2;
import gecko.geckocircuits.circuit.SolverSettings;
import gecko.geckocircuits.control.NetzlisteCONTROL;
import gecko.geckocircuits.control.ReglerOSZI;
import gecko.geckocircuits.control.ReglerSaveData;
import gecko.geckocircuits.control.ReglerFromEXTERNAL;
import gecko.geckocircuits.control.ReglerToEXTERNAL;
import gecko.geckocircuits.datacontainer.ContainerStatus;
import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Routes synchronous desktop starts to an independently owned core session. */
public final class DesktopSimulationController {
    public enum Backend { LEGACY, CORE, AUTO }

    private static final Logger FALLBACK = Logger.getLogger("gecko.backend.fallback");
    private static final Logger EXTERNAL = Logger.getLogger("gecko.backend.external");
    private final MainWindow window;
    private final SchematicEditor2 editor;
    private final Supplier<NetListContainer> preparePresentation;
    private volatile SessionHost currentHost;
    private volatile boolean coreExecution;
    private volatile double completedTime;
    private volatile CoreAsyncExecution asyncExecution;
    private volatile CoreScriptExecution scriptExecution;

    private static final class CoreScriptExecution {
        private final SessionHost host;
        private final NetListContainer netlist;
        private final Consumer<ContainerStatus> origin;
        private final LegacySimulationFrontend presentation;
        private final int[] rows;
        private final double dt;
        private boolean warnedExternalTime;
        private boolean warnedPastEnd;

        private CoreScriptExecution(SessionHost host, NetListContainer netlist, Consumer<ContainerStatus> origin,
                LegacySimulationFrontend presentation, int[] rows, double dt) {
            this.host = host;
            this.netlist = netlist;
            this.origin = origin;
            this.presentation = presentation;
            this.rows = rows;
            this.dt = dt;
        }
    }

    private static final long SCRIPT_COMMAND_TIMEOUT_SECONDS = 120;

    private static final class CoreAsyncExecution {
        private final SessionHost host;
        private final NetListContainer netlist;
        private final Consumer<ContainerStatus> origin;
        private final LegacySimulationFrontend presentation;
        private final int[] rows;
        private volatile Timer timer;
        private volatile long startedAtMillis;
        private volatile boolean awaitingRunAdmission;
        private volatile boolean ending;
        private volatile Future<SimulationSession.Status> runFuture;

        private CoreAsyncExecution(SessionHost host, NetListContainer netlist, Consumer<ContainerStatus> origin,
                LegacySimulationFrontend presentation, int[] rows) {
            this.host = host;
            this.netlist = netlist;
            this.origin = origin;
            this.presentation = presentation;
            this.rows = rows;
        }
    }

    DesktopSimulationController(MainWindow window, SchematicEditor2 editor, Supplier<NetListContainer> preparePresentation) {
        this.window = window;
        this.editor = editor;
        this.preparePresentation = preparePresentation;
    }

    void reset() {
        retireAsyncForReplacement();
        retireScriptForReplacement();
        coreExecution = false;
        completedTime = 0;
        currentHost = null;
    }

    boolean hasCoreExecution() { return coreExecution; }

    double getCurrentTime() {
        SessionHost host = currentHost;
        if (host == null) return completedTime;
        SimulationSession.Status status = host.status();
        return scriptExecution == null ? status.time() : status.nextTime();
    }

    boolean hasAsyncExecution() { return asyncExecution != null; }

    /** Initializes a script-owned core session without advancing its first step. */
    boolean initializeScript(SolverSettings settings, double dt, double end) throws Exception {
        Backend backend = Backend.valueOf(System.getProperty("gecko.backend", "auto")
                .trim().toUpperCase(Locale.ROOT));
        if (backend == Backend.LEGACY) return false;
        Route route = route(editor, settings);
        String circuit = route.circuit();
        if (!route.core()) return fallback(backend, circuit, route.fallback());
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Script simulation initialization must run outside the Swing event thread");
        }

        SessionHost host = SessionHost.open(route.ready().execution(), ExecutionOptions.defaults());
        NetListContainer netlist = null;
        boolean published = false;
        try {
            netlist = preparePresentation.get();
            Consumer<ContainerStatus> origin = NetzlisteCONTROL.globalData.captureStatusUpdater();
            int[] rows = new int[NetzlisteCONTROL.globalData.getRowLength()];
            String missingSignal = unmappedSignal(coreRows(host, true), rows);
            if (missingSignal != null) return fallback(backend, circuit, "SIGNAL_MAP:" + missingSignal);
            netlist._nlControl.doMemorInits(dt);
            LegacySimulationFrontend presentation = new LegacySimulationFrontend(() -> true);
            presentation.prepare(netlist, () -> true, true).run();
            setScopeBoundaries(netlist, 0, end);
            CoreScriptExecution execution = new CoreScriptExecution(host, netlist, origin, presentation, rows, dt);
            scriptExecution = execution;
            currentHost = host;
            coreExecution = true;
            completedTime = 0;
            LegacySimulationFrontend.onEventThread(() -> origin.accept(ContainerStatus.RUNNING));
            published = true;
            return true;
        } finally {
            if (!published) {
                host.close();
                if (netlist != null) cancelExporters(netlist);
            }
        }
    }

    /** Prepares external execution with the requested end time and no pre-calculation. */
    boolean initializeExternal(SolverSettings settings, double end) throws Exception {
        SolverSettings externalSettings = new SolverSettings();
        externalSettings.dt.setValueWithoutUndo(settings.dt.getValue());
        externalSettings._tDURATION.setValueWithoutUndo(end);
        externalSettings._tPAUSE.setValueWithoutUndo(settings._tPAUSE.getValue());
        externalSettings.SOLVER_TYPE.setValueWithoutUndo(settings.SOLVER_TYPE.getValue());
        return initializeScript(externalSettings, settings.dt.getValue(), end);
    }

    double externalStepWidth() {
        CoreScriptExecution execution = scriptExecution;
        if (execution == null) throw new IllegalStateException("No core external simulation is active");
        return execution.dt;
    }

    void externalStep(double time) throws Exception {
        requireOffEventThread();
        CoreScriptExecution execution = scriptExecution;
        if (execution == null) throw new IllegalStateException("No core external simulation is active");
        SessionHost host = execution.host;
        if (!execution.warnedExternalTime && Math.abs(time - host.status().nextTime()) > execution.dt / 2) {
            execution.warnedExternalTime = true;
            EXTERNAL.warning("External time differs from the core fixed-step grid. The core retains its configured step width.");
        }
        if (ReglerFromEXTERNAL.fromExternals.size() != host.externalInputPorts()
                || ReglerToEXTERNAL.toExternals.size() != host.externalOutputPorts()) {
            throw new IllegalStateException("External port counts do not match the core session");
        }
        double[][] inputs = new double[host.externalInputPorts()][];
        for (int port = 0; port < inputs.length; port++) {
            inputs[port] = ((ReglerFromEXTERNAL) ReglerFromEXTERNAL.fromExternals.get(port)).dataVector;
            if (inputs[port] == null || inputs[port].length != host.externalInputWidth(port)) {
                throw new IllegalStateException("External input width does not match at port " + port);
            }
        }
        for (int port = 0; port < host.externalOutputPorts(); port++) {
            double[] target = ((ReglerToEXTERNAL) ReglerToEXTERNAL.toExternals.get(port)).dataVector;
            if (target == null || target.length != host.externalOutputWidth(port)) {
                throw new IllegalStateException("External output width does not match at port " + port);
            }
        }
        SimulationSession.Status status = host.status();
        if (status.state() == SimulationSession.State.COMPLETED) {
            // Legacy external_step has no end limit (SimulationsKern.external_step). Doubling the end amortizes
            // extend(), which copies the recording on every call.
            double end = Math.max(status.nextTime(), status.endTime() + Math.max(status.endTime(), status.stepWidth()));
            await(host.extend(end, status.stepWidth()));
            if (!execution.warnedPastEnd) {
                execution.warnedPastEnd = true;
                EXTERNAL.info("External stepping continues past the configured end time.");
            }
        }
        double[][] outputs = await(host.stepExternal(inputs));
        if (outputs.length != host.externalOutputPorts()) {
            throw new IllegalStateException("Core external output port count does not match");
        }
        for (int port = 0; port < outputs.length; port++) {
            double[] target = ((ReglerToEXTERNAL) ReglerToEXTERNAL.toExternals.get(port)).dataVector;
            if (outputs[port] == null || outputs[port].length != target.length) {
                throw new IllegalStateException("Core external output width does not match at port " + port);
            }
            System.arraycopy(outputs[port], 0, target, 0, target.length);
        }
        drainScriptSamples(execution);
    }

    void scriptStep(double dt, double end) throws Exception {
        requireOffEventThread();
        CoreScriptExecution execution = scriptExecution;
        if (execution == null) throw new IllegalStateException("No core script simulation is active");
        double current = execution.host.status().nextTime();
        if (current + dt > end) {
            throw new Exception("Specified end of simulation reached! Cannot simulate another step.");
        }
        await(execution.host.step());
        drainScriptSamples(execution);
    }

    void scriptRunUntil(double target) throws Exception {
        requireOffEventThread();
        CoreScriptExecution execution = scriptExecution;
        if (execution == null) throw new IllegalStateException("No core script simulation is active");
        SimulationSession.Status status = execution.host.status();
        while (status.nextTime() <= target && status.state() != SimulationSession.State.COMPLETED) {
            if (status.state() == SimulationSession.State.FAILED) {
                throw new IllegalStateException("The core script simulation has failed.");
            }
            if (status.state() == SimulationSession.State.CLOSED) {
                throw new IllegalStateException("The core script simulation is closed.");
            }
            // Match the legacy loop's cursor condition exactly: it steps while t <= target.
            await(execution.host.step());
            status = execution.host.status();
        }
        if (status.state() == SimulationSession.State.FAILED) {
            throw new IllegalStateException("The core script simulation has failed.");
        }
        if (status.state() == SimulationSession.State.CLOSED) {
            throw new IllegalStateException("The core script simulation is closed.");
        }
        drainScriptSamples(execution);
    }

    private static void requireOffEventThread() {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Script simulation stepping must run outside the Swing event thread");
        }
    }

    private static <T> T await(Future<T> future) throws Exception {
        try {
            return future.get(SCRIPT_COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException timeout) {
            future.cancel(true);
            throw new IllegalStateException("Timed out waiting for the core script simulation command", timeout);
        } catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof Exception exception) throw exception;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("Core script simulation command failed", cause);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw interrupted;
        }
    }

    private static void drainScriptSamples(CoreScriptExecution execution) {
        execution.host.drainSamples(sample -> {
            float[] sourceValues = sample.values();
            float[] values = new float[execution.rows.length];
            for (int row = 0; row < execution.rows.length; row++) values[row] = sourceValues[execution.rows[row]];
            NetzlisteCONTROL.globalData.insertValuesAtEnd(values, sample.time());
        });
    }

    private void retireScriptForReplacement() {
        CoreScriptExecution execution = scriptExecution;
        if (execution == null) return;
        scriptExecution = null;
        execution.host.close();
        drainScriptSamples(execution);
        currentHost = null;
        coreExecution = false;
        LegacySimulationFrontend.onEventThread(() -> {
            execution.origin.accept(ContainerStatus.DELETED);
            execution.presentation.accept(false);
            cancelExporters(execution.netlist);
        });
    }

    SimulationSession.Status getCoreStatus() {
        CoreAsyncExecution run = asyncExecution;
        if (run != null) return run.host.status();
        CoreScriptExecution script = scriptExecution;
        return script == null ? null : script.host.status();
    }

    /** Returns false when GUI execution must use the legacy backend. */
    boolean startAsync(SolverSettings settings) throws Exception {
        Backend backend = Backend.valueOf(System.getProperty("gecko.backend", "auto")
                .trim().toUpperCase(Locale.ROOT));
        if (backend == Backend.LEGACY) return false;
        Route route = route(editor, settings);
        String circuit = route.circuit();
        if (!route.core()) return fallback(backend, circuit, route.fallback());

        SessionHost host = SessionHost.open(route.ready().execution(), ExecutionOptions.defaults());
        NetListContainer netlist = null;
        LegacySimulationFrontend presentation = null;
        Consumer<ContainerStatus> origin = null;
        boolean published = false;
        boolean statusStarted = false;
        try {
            netlist = preparePresentation.get();
            origin = NetzlisteCONTROL.globalData.captureStatusUpdater();
            int[] rows = new int[NetzlisteCONTROL.globalData.getRowLength()];
            String missingSignal = unmappedSignal(coreRows(host, false), rows);
            if (missingSignal != null) {
                cancelExporters(netlist);
                host.close();
                return fallback(backend, circuit, "SIGNAL_MAP:" + missingSignal);
            }

            netlist._nlControl.doMemorInits(settings.dt.getValue());
            presentation = new LegacySimulationFrontend(() -> true);
            presentation.prepare(netlist, () -> true, true).run();
            setScopeBoundaries(netlist, 0, settings._tDURATION.getValue());

            CoreAsyncExecution run = new CoreAsyncExecution(host, netlist, origin, presentation, rows);
            run.startedAtMillis = System.currentTimeMillis();
            asyncExecution = run;
            currentHost = host;
            coreExecution = true;
            completedTime = 0;
            LegacySimulationFrontend.onEventThread(() -> {
                run.origin.accept(ContainerStatus.RUNNING);
                window.setMenuDuringSimulation(true, false);
                run.timer = new Timer(40, event -> tick(run));
                run.runFuture = host.run();
                run.timer.start();
            });
            statusStarted = true;
            published = true;
            return true;
        } finally {
            if (!published) {
                if (host.status().state() != SimulationSession.State.CLOSED) host.close();
                if (netlist != null) cancelExporters(netlist);
                if (statusStarted && origin != null) {
                    Consumer<ContainerStatus> capturedOrigin = origin;
                    LegacySimulationFrontend.onEventThread(() -> capturedOrigin.accept(ContainerStatus.DELETED));
                }
                asyncExecution = null;
                currentHost = null;
                coreExecution = false;
            }
        }
    }

    boolean pauseAsync() {
        CoreAsyncExecution run = asyncExecution;
        if (run == null) return false;
        SimulationSession.State state = run.host.status().state();
        if (state == SimulationSession.State.RUNNING || state == SimulationSession.State.READY) run.host.pause();
        return true;
    }

    boolean continueAsync(SolverSettings settings) {
        CoreAsyncExecution run = asyncExecution;
        if (run == null) return false;
        SimulationSession.Status status = run.host.status();
        if (status.state() == SimulationSession.State.CLOSED || status.state() == SimulationSession.State.FAILED) {
            return true;
        }
        if (status.state() == SimulationSession.State.RUNNING) return true;

        double end = status.endTime();
        boolean reachedEnd = status.state() == SimulationSession.State.COMPLETED || status.time() >= end;
        if (reachedEnd) {
            end += settings._tDURATION.getValue();
            run.host.extend(end, settings.dt.getValue());
            setScopeBoundaries(run.netlist, status.time(), end);
        }
        completedTime = 0;
        run.startedAtMillis = System.currentTimeMillis();
        run.awaitingRunAdmission = true;
        // Queue the next run before restarting the EDT timer. In the extension case, the
        // previous COMPLETED status remains visible while extend() copies the recording;
        // a timer tick in that window would stop itself before the queued run starts.
        run.runFuture = run.host.run();
        LegacySimulationFrontend.onEventThread(() -> {
            run.origin.accept(ContainerStatus.RUNNING);
            run.presentation.accept(true);
            window.setMenuDuringSimulation(true, false);
            if (!run.timer.isRunning()) run.timer.start();
        });
        return true;
    }

    boolean endAsync() {
        CoreScriptExecution script = scriptExecution;
        if (script != null) {
            scriptExecution = null;
            drainScriptSamples(script);
            completedTime = script.host.status().nextTime();
            script.host.close();
            currentHost = null;
            LegacySimulationFrontend.onEventThread(() -> {
                script.origin.accept(ContainerStatus.PAUSED);
                script.presentation.accept(false);
                cancelExporters(script.netlist);
            });
            return true;
        }
        CoreAsyncExecution run = asyncExecution;
        if (run == null) return false;
        run.ending = true;
        if (run.runFuture != null && !run.runFuture.isDone()) run.host.pause();
        LegacySimulationFrontend.onEventThread(() -> {
            if (run.runFuture == null || run.runFuture.isDone()) finishEnd(run);
            else if (run.timer != null && !run.timer.isRunning()) run.timer.start();
        });
        return true;
    }

    private void retireAsyncForReplacement() {
        CoreAsyncExecution run = asyncExecution;
        if (run == null) return;
        asyncExecution = null;
        run.host.close();
        LegacySimulationFrontend.onEventThread(() -> {
            if (run.timer != null) run.timer.stop();
            cancelExporters(run.netlist);
            run.origin.accept(ContainerStatus.DELETED);
            run.presentation.accept(false);
        });
    }

    private void finishEnd(CoreAsyncExecution run) {
        if (asyncExecution != run) return;
        if (run.runFuture != null && !run.runFuture.isDone()) return;
        if (run.timer != null) run.timer.stop();
        SimulationSession.Status terminalStatus = run.host.status();
        double finalTime = terminalStatus.time();
        run.host.close();
        drainSamples(run);
        completedTime = finalTime;
        currentHost = null;
        asyncExecution = null;
        if (terminalStatus.state() == SimulationSession.State.FAILED) {
            cancelExporters(run.netlist);
            run.origin.accept(ContainerStatus.DELETED);
            run.presentation.accept(false);
            window.setMenusAfterSimulationFailure();
            window.jtfStatus.setText("Simulation aborted.");
            return;
        }
        run.origin.accept(ContainerStatus.PAUSED);
        run.presentation.accept(false);
        window.setMenusAfterSimulationFailure();
        window.jtfStatus.setText("Stopped after  ");
    }

    private void tick(CoreAsyncExecution run) {
        if (asyncExecution != run) {
            run.timer.stop();
            return;
        }
        SimulationSession.Status status = run.host.status();
        if (run.ending) {
            if (run.runFuture == null || run.runFuture.isDone()) finishEnd(run);
            return;
        }
        if (run.awaitingRunAdmission) {
            if (run.runFuture != null && !run.runFuture.isDone()
                    && status.state() != SimulationSession.State.RUNNING) return;
            run.awaitingRunAdmission = false;
        }
        drainSamples(run);
        SimulationSession.State state = status.state();
        if (state != SimulationSession.State.PAUSED && state != SimulationSession.State.COMPLETED
                && state != SimulationSession.State.FAILED) return;

        drainSamples(run);
        run.timer.stop();
        completedTime = status.time();
        if (state == SimulationSession.State.FAILED) {
            // A PAUSED status can trigger DATA_EXPORT. Cancel first and invalidate the
            // captured recording so a failed partial core run cannot be saved as complete.
            cancelExporters(run.netlist);
            run.origin.accept(ContainerStatus.DELETED);
            run.presentation.accept(false);
            window.setMenusAfterSimulationFailure();
        } else {
            run.origin.accept(ContainerStatus.PAUSED);
            run.presentation.accept(false);
            // Paused and completed executions can both be continued in the legacy UI.
            window.setMenuDuringSimulation(false, true);
            if (state == SimulationSession.State.COMPLETED) {
                window.jtfStatus.setzeStatusRechenzeit(System.currentTimeMillis() - run.startedAtMillis);
            }
        }
    }

    private static void drainSamples(CoreAsyncExecution run) {
        run.host.drainSamples(sample -> insertSample(run, sample));
    }

    private static void insertSample(CoreAsyncExecution run, SimulationSession.Sample sample) {
        float[] sourceValues = sample.values();
        float[] values = new float[run.rows.length];
        for (int row = 0; row < run.rows.length; row++) values[row] = sourceValues[run.rows[row]];
        NetzlisteCONTROL.globalData.insertValuesAtEnd(values, sample.time());
    }

    private static void setScopeBoundaries(NetListContainer netlist, double start, double end) {
        LegacySimulationFrontend.onEventThread(() -> {
            for (var block : netlist._nlControl._orderedControlBlocks) {
                if (block instanceof ReglerOSZI scope) scope.setSimulationTimeBoundaries(start, end);
            }
        });
    }

    private static void cancelExporters(NetListContainer netlist) {
        LegacySimulationFrontend.onEventThread(() -> {
            for (var block : netlist._nlControl._orderedControlBlocks) {
                if (block instanceof ReglerSaveData exporter) exporter.cancelSimulationExport();
            }
        });
    }

    /** Returns false when the caller must use the existing legacy start path. */
    boolean start(SolverSettings settings) throws Exception {
        Backend backend = Backend.valueOf(System.getProperty("gecko.backend", "auto")
                .trim().toUpperCase(Locale.ROOT));
        if (backend == Backend.LEGACY) return false;
        Route route = route(editor, settings);
        String circuit = route.circuit();
        if (!route.core()) return fallback(backend, circuit, route.fallback());
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Run synchronous core calculations outside the Swing event thread");
        }

        try (var host = SessionHost.open(route.ready().execution(), ExecutionOptions.defaults())) {
            var netlist = preparePresentation.get();
            var origin = NetzlisteCONTROL.globalData.captureStatusUpdater();
            var presentation = new LegacySimulationFrontend(() -> true);
            int[] rows = new int[NetzlisteCONTROL.globalData.getRowLength()];
            String missingSignal = unmappedSignal(coreRows(host, false), rows);
            if (missingSignal != null && backend == Backend.AUTO) {
                // Retire unused preparation resources before the legacy factory creates their replacements.
                LegacySimulationFrontend.onEventThread(() -> {
                    for (var block : netlist._nlControl._orderedControlBlocks) {
                        if (block instanceof ReglerSaveData exporter) exporter.cancelSimulationExport();
                    }
                });
                return fallback(backend, circuit, "SIGNAL_MAP:" + missingSignal);
            }
            boolean completed = false;
            Throwable failure = null;
            try {
                if (missingSignal != null) return fallback(backend, circuit, "SIGNAL_MAP:" + missingSignal);
                netlist._nlControl.doMemorInits(settings.dt.getValue());
                presentation.prepare(netlist, () -> true, true).run();
                LegacySimulationFrontend.onEventThread(() -> {
                    for (var block : netlist._nlControl._orderedControlBlocks) {
                        if (block instanceof ReglerOSZI scope) {
                            scope.setSimulationTimeBoundaries(0, settings._tDURATION.getValue());
                        }
                    }
                    window.setMenuDuringSimulation(true, false);
                });
                coreExecution = true;
                currentHost = host;
                try {
                    host.run().get();
                } catch (ExecutionException error) {
                    if (error.getCause() instanceof Error cause) throw cause;
                    if (error.getCause() instanceof RuntimeException cause) throw cause;
                    throw new IllegalStateException("Core calculation failed", error.getCause());
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw error;
                }
                host.drainSamples(sample -> {
                    float[] sourceValues = sample.values();
                    float[] values = new float[rows.length];
                    for (int row = 0; row < rows.length; row++) values[row] = sourceValues[rows[row]];
                    NetzlisteCONTROL.globalData.insertValuesAtEnd(values, sample.time());
                });
                completed = true;
                return true;
            } catch (Exception | Error error) {
                failure = error;
                throw error;
            } finally {
                try {
                    completedTime = completed ? host.status().time() : 0;
                    currentHost = null;
                    if (completed) {
                        LegacySimulationFrontend.onEventThread(() -> origin.accept(ContainerStatus.PAUSED));
                    } else {
                        coreExecution = false;
                        host.close();
                        LegacySimulationFrontend.onEventThread(() -> {
                            for (var block : netlist._nlControl._orderedControlBlocks) {
                                if (block instanceof ReglerSaveData exporter) exporter.cancelSimulationExport();
                            }
                            origin.accept(ContainerStatus.DELETED);
                        });
                    }
                    presentation.accept(false);
                    boolean successful = completed;
                    LegacySimulationFrontend.onEventThread(() -> {
                        if (successful) window.setMenuDuringSimulation(false, true);
                        else window.setMenusAfterSimulationFailure();
                    });
                } catch (RuntimeException | Error cleanupError) {
                    if (failure == null) throw cleanupError;
                    if (failure != cleanupError) failure.addSuppressed(cleanupError);
                }
            }
        }
    }

    /** A desktop routing decision: {@code ready} for core, otherwise the legacy fallback reason. */
    public record Route(String circuit, PreparationResult.Ready ready, String fallback) {
        public boolean core() { return ready != null; }
    }

    /**
     * Decides from solver policy and the editor's prepared circuit whether a start can use core. Starts nothing;
     * a core route still needs {@link #unmappedSignal} to find no gap in the presentation signal map.
     */
    public static Route route(SchematicEditor2 editor, SolverSettings settings) throws Exception {
        String source = MainWindow.getOpenFileName();
        String circuit = new File(source).getName().replaceFirst("\\.ipes$", "");
        if (settings._T_pre.getValue() > 0) return new Route(circuit, null, "PRECALC");
        if (settings._tPAUSE.getValue() >= 0) return new Route(circuit, null, "PAUSE_TIME");
        var scripts = MainWindow._scripter;
        var sources = new EditorCircuitCapture.ScriptSources(scripts.getScriptCode(), scripts.getImportCode(),
                scripts.getDeclarationCode(), scripts.getExtraFilesHashes());
        var snapshot = new EditorCircuitCapture().capture(editor._circuitSheet.getAllElements(), settings,
                MainWindow.optimizerParameterData, MainWindow._fileManager, sources, source);
        var preparation = new CircuitPreparation().prepare(snapshot, PreparationRequest.defaults());
        if (preparation instanceof PreparationResult.Ready ready) return new Route(circuit, ready, null);
        return new Route(circuit, null, preparation.report().diagnostics().stream()
                .map(diagnostic -> diagnostic.code().toString()).distinct().collect(Collectors.joining(",")));
    }

    /** Core sample rows by signal name: the first core row wins; script runs also accept scope channel names. */
    public static Map<String, Integer> coreRows(SessionHost host, boolean withScopeChannels) {
        Map<String, Integer> rowBySignal = new HashMap<>();
        String[] names = host.signalNames();
        for (int row = 0; row < names.length; row++) rowBySignal.putIfAbsent(names[row], row);
        if (withScopeChannels) {
            for (var channel : host.scopeChannels()) rowBySignal.putIfAbsent(channel.signalName(), channel.row());
        }
        return rowBySignal;
    }

    /** Fills each presentation row's core row; returns the first presentation signal core lacks, or null. */
    public static String unmappedSignal(Map<String, Integer> coreRows, int[] rows) {
        for (int row = 0; row < rows.length; row++) {
            String name = NetzlisteCONTROL.globalData.getSignalName(row);
            rows[row] = coreRows.getOrDefault(name, -1);
            if (rows[row] < 0) return name;
        }
        return null;
    }

    private static boolean fallback(Backend backend, String circuit, String reason) {
        if (backend == Backend.CORE) throw new IllegalStateException("Core cannot run " + circuit + ": " + reason);
        FALLBACK.info(circuit + ": " + reason);
        return false;
    }
}
