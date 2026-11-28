package gecko.rest.service;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/** Keeps one pending progress update and one terminal event for an ordered job stream. */
final class ProgressMailbox {
    private final Executor executor;
    private Runnable progress;
    private Runnable terminal;
    private Runnable rejectedTerminal;
    private boolean scheduled;
    private boolean closed;

    ProgressMailbox(Executor executor) {
        this.executor = executor;
    }

    synchronized void progress(Runnable delivery) {
        if (!closed) {
            progress = delivery;
            schedule();
        }
    }

    synchronized void terminal(Runnable delivery) {
        terminal(delivery, () -> { });
    }

    synchronized void terminal(Runnable delivery, Runnable onRejected) {
        if (!closed) {
            closed = true;
            terminal = delivery;
            rejectedTerminal = onRejected;
            schedule();
        }
    }

    private void schedule() {
        if (!scheduled) {
            scheduled = true;
            try {
                executor.execute(this::drain);
            } catch (RejectedExecutionException rejected) {
                scheduled = false;
                progress = null;
                terminal = null;
                if (rejectedTerminal != null) {
                    Runnable notifyRejected = rejectedTerminal;
                    rejectedTerminal = null;
                    notifyRejected.run();
                }
            }
        }
    }

    private void drain() {
        while (true) {
            Runnable delivery;
            synchronized (this) {
                if (progress != null) {
                    delivery = progress;
                    progress = null;
                } else if (terminal != null) {
                    delivery = terminal;
                    terminal = null;
                    rejectedTerminal = null;
                } else {
                    scheduled = false;
                    return;
                }
            }
            try {
                delivery.run();
            } catch (RuntimeException ignored) {
                // A disconnected transport must not prevent the terminal event.
            }
        }
    }
}
