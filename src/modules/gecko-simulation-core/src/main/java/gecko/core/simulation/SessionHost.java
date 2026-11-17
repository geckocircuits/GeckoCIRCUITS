package gecko.core.simulation;

import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparedCircuit;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Owns one session on one thread. Other threads submit commands and pull results (ADR-0010). */
public final class SessionHost implements AutoCloseable {
    private static final AtomicInteger IDS = new AtomicInteger();
    private final SimulationSession session;
    private final int[] externalInputWidths;
    private final int[] externalOutputWidths;
    private final ExecutorService owner;
    private final AtomicBoolean pauseRequested = new AtomicBoolean();
    // ponytail: unbounded. The desktop drains every tick. Bound it if a non-draining caller appears.
    private final ConcurrentLinkedQueue<SimulationSession.Sample> samples = new ConcurrentLinkedQueue<>();

    private SessionHost(SimulationSession session) {
        this.session = session;
        externalInputWidths = new int[session.externalInputPorts()];
        for (int port = 0; port < externalInputWidths.length; port++) externalInputWidths[port] = session.externalInputWidth(port);
        externalOutputWidths = new int[session.externalOutputPorts()];
        for (int port = 0; port < externalOutputWidths.length; port++) externalOutputWidths[port] = session.externalOutputWidth(port);
        int id = IDS.incrementAndGet();
        this.owner = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "gecko-session-" + id);
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Opens independent execution state and its command executor. */
    public static SessionHost open(PreparedCircuit circuit, ExecutionOptions options) {
        return new SessionHost(SimulationSession.open(circuit, options));
    }

    /** Runs until completion or a pause request. */
    public Future<SimulationSession.Status> run() {
        return owner.submit(() -> runWhile(() -> false));
    }

    /** Runs until the committed time reaches the target, completion, or a pause request. */
    public Future<SimulationSession.Status> runUntil(double time) {
        return owner.submit(() -> runWhile(() -> session.status().time() >= time));
    }

    private SimulationSession.Status runWhile(BooleanSupplier reached) {
        return session.run(() -> pauseRequested.getAndSet(false) || reached.getAsBoolean(), samples::add);
    }

    /** Commits one step on the owner thread and queues its sample. */
    public Future<SimulationSession.Sample> step() {
        return owner.submit(() -> {
            SimulationSession.Sample sample = session.step();
            if (sample != null) samples.add(sample);
            return sample;
        });
    }

    /** Sets every external input, commits one step, queues the sample, and returns same-step outputs. */
    public Future<double[][]> stepExternal(double[][] inputs) {
        double[][] detached = validateExternalInputs(inputs);
        return owner.submit(() -> {
            for (int port = 0; port < detached.length; port++) session.setExternalInputs(port, detached[port]);
            SimulationSession.Sample sample = session.step();
            samples.add(sample);
            double[][] outputs = new double[externalOutputWidths.length][];
            for (int port = 0; port < outputs.length; port++) outputs[port] = session.externalOutputs(port);
            return outputs;
        });
    }

    private double[][] validateExternalInputs(double[][] inputs) {
        int ports = externalInputWidths.length;
        if (inputs == null || inputs.length != ports) throw new IllegalArgumentException("External input port count does not match.");
        double[][] detached = new double[ports][];
        for (int port = 0; port < ports; port++) {
            if (inputs[port] == null || inputs[port].length != externalInputWidths[port])
                throw new IllegalArgumentException("External input width does not match at port " + port + ".");
            detached[port] = inputs[port].clone();
        }
        return detached;
    }

    public int externalInputPorts() { return externalInputWidths.length; }
    public int externalOutputPorts() { return externalOutputWidths.length; }
    public int externalInputWidth(int port) { return portWidth(externalInputWidths, port); }
    public int externalOutputWidth(int port) { return portWidth(externalOutputWidths, port); }

    private static int portWidth(int[] widths, int port) {
        if (port < 0 || port >= widths.length) throw new IllegalArgumentException("External port is out of range.");
        return widths[port];
    }

    /** Extends execution without resetting committed histories. */
    public Future<SimulationSession.Status> extend(double endTime, double stepWidth) {
        return owner.submit(() -> {
            session.extend(endTime, stepWidth);
            return session.status();
        });
    }

    /** Queues a bounded observation behind earlier execution commands. */
    public Future<SimulationSession.Observation> observe(int first, int count) {
        return owner.submit(() -> session.observe(first, count));
    }

    /** Requests a pause for the current or next run. */
    public void pause() { pauseRequested.set(true); }

    /** Reads committed status without waiting for the owner thread. */
    public SimulationSession.Status status() { return session.status(); }

    /** Returns detached signal names in sample order. */
    public String[] signalNames() { return session.signalNames(); }

    /** Returns detached SCOPE channel aliases and their sample-row indexes. */
    public List<ScopeDataExtractor.ScopeChannel> scopeChannels() { return session.scopeChannels(); }

    /** Drains samples in order. Use one consumer because concurrent consumers split the sequence. */
    public int drainSamples(Consumer<SimulationSession.Sample> sink) {
        int drained = 0;
        for (SimulationSession.Sample sample; (sample = samples.poll()) != null; drained++) sink.accept(sample);
        return drained;
    }

    /** Requests session closure and shuts down the command executor without waiting. */
    @Override public void close() {
        pauseRequested.set(true);
        session.close();
        owner.shutdown();
    }
}
