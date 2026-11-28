package gecko.core.io;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Separates saved-data integrity from headless engine capability (issue #95). */
class ParserCorpusContractTest {
    @Test void everySavedCircuitParsesAndRetainsThermalAndTransformerParameters() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("pom-reactor.xml"))) root = root.getParent();
        assertNotNull(root, "The repository root must contain pom-reactor.xml.");
        List<Path> files = new ArrayList<>();
        for (String directory : List.of("src/test/resources/ipes", "src/modules/gecko-simulation-core/src/test/resources/ipes")) {
            try (var paths = Files.walk(root.resolve(directory))) {
                files.addAll(paths.filter(path -> path.toString().endsWith(".ipes")).sorted().toList());
            }
        }
        assertTrue(files.size() >= 26, "The corpus must include both resource directories.");
        Map<String, Integer> unsupported = new TreeMap<>();
        List<String> failures = new ArrayList<>();
        int thermal = 0;
        int magnetic = 0;
        int transformerFiles = 0;
        for (Path file : files) {
            CircuitModel model = assertDoesNotThrow(() -> new CircuitFileParser().parse(file.toString()), file.toString());
            for (ImportIssue issue : model.getImportIssues()) {
                if (issue.code().startsWith("MALFORMED_") || issue.code().equals("INVALID_SETTING")) {
                    failures.add(file + ": " + issue);
                }
                if (issue.code().equals("UNSUPPORTED_DOMAIN")) unsupported.merge(issue.domain(), 1, Integer::sum);
            }
            String text = savedText(file);
            if (model.getCircuitComponents().stream().anyMatch(component -> component.getType() == 9)) transformerFiles++;
            thermal += compareSavedParameters(text, "ElementTHERM", model.getThermalComponents(), false, file);
            magnetic += compareSavedParameters(text, "ElementLK", model.getCircuitComponents(), true, file);
        }
        System.out.printf("Parser corpus: %d files, %d parse-failure issues, %d thermal and %d magnetic components verified.%n",
                files.size(), failures.size(), thermal, magnetic);
        System.out.println("UNSUPPORTED_DOMAIN by domain: " + unsupported + ". Engine capability is tracked by #95.");
        assertTrue(failures.isEmpty(), String.join("\n", failures));
        assertTrue(thermal > 0, "A thermal corpus fixture must retain its saved parameters.");
        assertTrue(magnetic > 0, "Transformer corpus files must retain their saved parameters.");
        assertTrue(transformerFiles > 0, "At least one corpus file must contain transformer coupling metadata.");
    }

    private static String savedText(Path file) throws Exception {
        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length >= 2 && (bytes[0] & 255) == 31 && (bytes[1] & 255) == 139) {
            try (var gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) { bytes = gzip.readAllBytes(); }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static int compareSavedParameters(String text, String tag, List<CircuitModel.ComponentData> components,
            boolean magneticOnly, Path source) {
        var blocks = Pattern.compile("(?s)<" + tag + ">(.*?)<\\\\" + tag + ">").matcher(text);
        int compared = 0;
        while (blocks.find()) {
            String block = blocks.group(1);
            int type = Integer.parseInt(savedLine(block, "typ"));
            // Type 9 stores magnetic coupling. Type 12 stores its winding inductance, including leakage.
            if (magneticOnly && type != 9 && type != 12 && type != 23) continue;
            String name = savedLine(block, "idStringDialog");
            String raw = savedLine(block, "parameter[]").replace('/', ' ').trim();
            double[] expected = raw.isEmpty() ? new double[0] : Arrays.stream(raw.split("\\s+"))
                    .mapToDouble(Double::parseDouble).toArray();
            var component = components.stream().filter(candidate -> candidate.getType() == type
                    && candidate.getName().equals(name)).findFirst().orElseThrow(() ->
                    new AssertionError(source + ": missing " + tag + " " + name));
            assertArrayEquals(expected, component.getNumericParameters(), source + ": saved parameters for " + name);
            assertEquals(expected.length, component.getImportedParameterCount(), source + ": parameter count for " + name);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], component.getParameters().get("param" + i), source + ": " + name + " param" + i);
            }
            compared++;
        }
        return compared;
    }

    private static String savedLine(String block, String token) {
        var line = Pattern.compile("(?m)^\\s*" + Pattern.quote(token) + "[ \\t]+([^\\r\\n]*)").matcher(block);
        assertTrue(line.find(), "Missing saved token: " + token);
        return line.group(1).trim();
    }
}
