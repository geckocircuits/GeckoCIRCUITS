package gecko.core.io;

import java.util.List;

/** Missing resource evidence. Execution checks decide whether an enabled owner needs it. */
public record CircuitResourceFailure(long id, String provenance, List<Long> ownerIds, String message) {
    public CircuitResourceFailure {
        ownerIds = List.copyOf(ownerIds);
    }
}
