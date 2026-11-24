package gecko.core.io;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.jar.JarFile;

/** Build identity read from the running Gecko artifact. */
public record BuildIdentity(String revision, boolean dirty, String sourceSnapshotSha256, String jarSha256) {
    private static final String ENTRY = "META-INF/gecko-build.properties";

    public static BuildIdentity current() throws IOException {
        var codeSource = BuildIdentity.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) return checkAssertion(new BuildIdentity("", true, "", ""));
        try {
            Path artifact = Path.of(codeSource.getLocation().toURI());
            var fields = new Properties();
            String digest = "";
            if (Files.isRegularFile(artifact)) {
                try (var jar = new JarFile(artifact.toFile())) {
                    var entry = jar.getJarEntry(ENTRY);
                    if (entry != null) {
                        try (var input = jar.getInputStream(entry)) { fields.load(input); }
                    }
                }
                digest = ArtifactDigest.sha256(artifact);
            } else {
                Path entry = artifact.resolve(ENTRY);
                if (Files.isRegularFile(entry)) {
                    try (var input = Files.newInputStream(entry)) { fields.load(input); }
                }
            }
            String revision = fields.getProperty("revision", "").strip();
            if (!revision.matches("[a-f0-9]{40}")) revision = "";
            boolean dirty = !"false".equals(fields.getProperty("dirty"));
            String snapshot = fields.getProperty("sourceSnapshotSha256", "").strip();
            if (!snapshot.matches("[a-f0-9]{64}")) snapshot = "";
            return checkAssertion(new BuildIdentity(revision, dirty, snapshot, digest));
        } catch (URISyntaxException error) {
            throw new IOException("Cannot read the Gecko build identity.", error);
        }
    }

    private static BuildIdentity checkAssertion(BuildIdentity identity) {
        String asserted = System.getProperty("gecko.revision");
        if (asserted != null && (identity.revision.isEmpty() || !asserted.equals(identity.revision))) {
            throw new IllegalArgumentException("The requested Gecko revision differs from the packaged build.");
        }
        return identity;
    }
}
