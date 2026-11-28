package gecko.geckocircuits.allg;

import java.util.List;
import java.util.function.BooleanSupplier;

/** Applies the newest owned widget request after reentrant Swing setters return. */
public final class LatestOwnedPresentation {
    private Request latest;
    private int deliveryDepth;

    private record Request(BooleanSupplier stillCurrent, List<Runnable> changes) { }

    /** Creates an idle presentation updater. */
    public LatestOwnedPresentation() {
    }

    /**
     * Applies ordered widget changes while their request owns the presentation.
     * Callers serialize requests on the Swing event dispatch thread.
     * Widget exceptions propagate to the caller for independent reporting.
     *
     * @param stillCurrent checks ownership before each widget change
     * @param changes the ordered widget changes for this request
     * @throws NullPointerException if an argument or widget change is null
     */
    public void update(BooleanSupplier stillCurrent, Runnable... changes) {
        if (!stillCurrent.getAsBoolean()) {
            return;
        }
        Request request = new Request(stillCurrent, List.of(changes));
        latest = request;
        deliveryDepth++;
        try {
            apply(request);
        } finally {
            if (--deliveryDepth == 0) {
                latest = null;
            }
        }
    }

    private void apply(Request request) {
        for (Runnable change : request.changes()) {
            if (latest != request || !request.stillCurrent().getAsBoolean()) {
                return;
            }
            change.run();
            if (latest != request) {
                apply(latest);
                return;
            }
        }
    }
}
