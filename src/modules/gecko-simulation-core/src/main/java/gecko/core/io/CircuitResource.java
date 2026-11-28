package gecko.core.io;

import java.util.List;
import java.util.Objects;

/** Owns resource bytes captured before preparation. Paths are provenance only. */
public final class CircuitResource {
    private final long id;
    private final String name;
    private final String provenance;
    private final byte[] bytes;
    private final List<Long> ownerIds;

    public CircuitResource(long id, String name, String provenance, byte[] bytes, List<Long> ownerIds) {
        this.id = id;
        this.name = Objects.requireNonNull(name);
        this.provenance = Objects.requireNonNull(provenance);
        this.bytes = Objects.requireNonNull(bytes).clone();
        this.ownerIds = List.copyOf(ownerIds);
    }

    public long id() { return id; }
    public String name() { return name; }
    public String provenance() { return provenance; }
    public byte[] bytes() { return bytes.clone(); }
    public List<Long> ownerIds() { return ownerIds; }
}
