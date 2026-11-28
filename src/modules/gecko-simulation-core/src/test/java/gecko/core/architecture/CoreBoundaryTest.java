package gecko.core.architecture;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class CoreBoundaryTest {
    private static final Pattern FORBIDDEN_IMPORT = Pattern.compile(
            "(?m)^\\s*import\\s+(?:static\\s+)?(?:java\\.awt\\.|javax\\.swing\\.|java\\.applet\\.|gecko\\.geckocircuits\\.)");
    private static final Pattern FORBIDDEN_DEPENDENCY = Pattern.compile(
            "->\\s+(?:java\\.awt\\.|javax\\.swing\\.|java\\.applet\\.|gecko\\.geckocircuits\\.)");

    @Test
    void allExtractedCoreSourcesRespectTheHeadlessBoundary() throws Exception {
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            List<Path> sources = paths.filter(path -> path.toString().endsWith(".java")).toList();
            assertTrue(sources.size() > 200, "Scan the actual extracted module, not a missing source directory");
            for (Path source : sources) {
                assertFalse(FORBIDDEN_IMPORT.matcher(Files.readString(source)).find(), source.toString());
            }
        }
    }

    @Test
    void compiledCoreHasNoGuiOrDesktopDependencies() throws Exception {
        assertNoForbiddenDependencies(dependencies(Path.of("target/classes")));
    }

    @Test
    void compiledBoundaryCheckDetectsFullyQualifiedGuiReferences() throws Exception {
        String output = dependencies(Path.of("target/test-classes/gecko/core/architecture/CoreBoundaryTest$GuiLeak.class"));
        assertThrows(AssertionError.class, () -> assertNoForbiddenDependencies(output));
        assertTrue(output.contains("java.awt.Color"));
    }

    private String dependencies(Path classes) throws Exception {
        String executable = System.getProperty("os.name").startsWith("Windows") ? "jdeps.exe" : "jdeps";
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", executable).toString(),
                "--ignore-missing-deps", "-verbose:class", classes.toString()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "jdeps did not finish");
        assertEquals(0, process.exitValue(), output);
        return output;
    }

    private void assertNoForbiddenDependencies(String output) {
        assertFalse(FORBIDDEN_DEPENDENCY.matcher(output).find(), output);
    }

    private static class GuiLeak { java.awt.Color color; }
}
