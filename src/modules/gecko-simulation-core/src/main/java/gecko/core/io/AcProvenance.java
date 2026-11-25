package gecko.core.io;

import gecko.core.ac.AcInputOrigin;
import java.util.Map;
import java.util.Objects;

/** Import digests retained for independent execution evidence. */
public record AcProvenance(String sourceSha256, String importRevision, boolean importSourceDirty,
                           String importSourceSnapshotSha256, String importRuntimeJarSha256,
                           Map<String, String> dependencySha256, AcInputOrigin origin,
                           Map<String, String> definitionSourceSha256, Map<Long, String> memberSha256) {
    public AcProvenance(String sourceSha256, String importRevision, boolean importSourceDirty,
                        String importSourceSnapshotSha256, String importRuntimeJarSha256,
                        Map<String, String> dependencySha256) {
        this(sourceSha256, importRevision, importSourceDirty, importSourceSnapshotSha256,
                importRuntimeJarSha256, dependencySha256, AcInputOrigin.UNSELECTED_ASC, Map.of(), Map.of());
    }

    public AcProvenance {
        Objects.requireNonNull(origin);
        definitionSourceSha256 = Map.copyOf(definitionSourceSha256);
        memberSha256 = Map.copyOf(memberSha256);
        if (origin != AcInputOrigin.UNSELECTED_ASC && !origin.tested()
                || !origin.tested() && (!definitionSourceSha256.isEmpty() || !memberSha256.isEmpty())) {
            throw new IllegalArgumentException("Invalid AC provenance origin.");
        }
        for (var entry : definitionSourceSha256.entrySet()) {
            if (entry.getKey().isBlank() || !dependencySha256.containsValue(entry.getValue())) {
                throw new IllegalArgumentException("Invalid ASC definition digest.");
            }
        }
        for (var entry : memberSha256.entrySet()) {
            if (entry.getKey() <= 0 || !entry.getValue().matches("[a-f0-9]{64}")) {
                throw new IllegalArgumentException("Invalid ASC component membership digest.");
            }
        }
        if (!sourceSha256.matches("[a-f0-9]{64}") || !importRevision.matches("(?:[a-f0-9]{40})?")
                || !importRuntimeJarSha256.matches("(?:[a-f0-9]{64})?")
                || !importSourceSnapshotSha256.matches("(?:[a-f0-9]{64})?")) {
            throw new IllegalArgumentException("Invalid AC import digests.");
        }
        dependencySha256 = Map.copyOf(dependencySha256);
        dependencySha256.forEach((name, hash) -> {
            if (name.isBlank() || !hash.matches("[a-f0-9]{64}")) {
                throw new IllegalArgumentException("Invalid AC dependency digest.");
            }
        });
    }
}
