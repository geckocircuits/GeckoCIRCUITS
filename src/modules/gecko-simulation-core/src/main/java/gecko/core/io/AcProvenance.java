package gecko.core.io;

import java.util.Map;

/** Import digests retained for independent execution evidence. */
public record AcProvenance(String sourceSha256, String importRevision, boolean importSourceDirty,
                           String importSourceSnapshotSha256, String importRuntimeJarSha256,
                           Map<String, String> dependencySha256) {
    public AcProvenance {
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
