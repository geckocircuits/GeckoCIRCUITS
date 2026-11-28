package gecko.core.io;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import static org.junit.jupiter.api.Assertions.*;

class ResourceProvenanceTest {
    @Test
    void trustedLocalResourcesAreCapturedOnceAndUnusedUploadedAttachmentsDoNotBlockPreparation() throws Exception {
        Files.createDirectories(Path.of("target"));
        Path directory = Files.createTempDirectory(Path.of("target"), "local-resource-").toAbsolutePath();
        Path resourceFile = directory.resolve("values.txt");
        Files.writeString(resourceFile, "10 3\n11 7\n");
        String text = """
                e
                <ElementLK>
                typ 4
                idStringDialog source
                parameter[] 401 6
                labelAnfangsKnoten[] /1
                labelEndKnoten[] /0
                <\\ElementLK>
                e
                <ElementLK>
                typ 1
                idStringDialog resistor
                parameter[] 10
                labelAnfangsKnoten[] /1
                labelEndKnoten[] /0
                <\\ElementLK>
                dt 0.1
                tDURATION 0.2
                GeckoFileManager
                <GeckoFileManager>
                <GeckoFile>
                hashValue 42
                isExternal 1
                """ + "absPath " + resourceFile + "\n<\\GeckoFile>\n<\\GeckoFileManager>\n";
        Path circuitFile = directory.resolve("circuit.ipes");
        Files.writeString(circuitFile, text);
        var parser = new CircuitFileParser();
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(parser.parse(circuitFile.toString()));
        Files.writeString(resourceFile, "CHANGED AFTER CAPTURE");
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(snapshot, PreparationRequest.defaults()));
        assertArrayEquals("10 3\n11 7\n".getBytes(StandardCharsets.UTF_8),
                ready.execution().newRunModel().getResources().get(42L).bytes());
        var supplied = parser.parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), circuitFile.toString());
        assertTrue(supplied.getResources().isEmpty());
        assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(supplied), PreparationRequest.defaults()),
                "An unused unavailable attachment must not reject an executable circuit");
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void suppliedCircuitCannotReadHostAttachmentsEvenWithALocalSourceName(boolean stream, boolean relative) throws Exception {
        Path target = Path.of("target");
        Files.createDirectories(target);
        Path directory = Files.createTempDirectory(target, "resource-provenance-").toAbsolutePath();
        Path privateFile = directory.resolve("private.txt");
        Files.writeString(privateFile, "HOST-PRIVATE-CONTENT");
        Path uploads = Files.createDirectory(directory.resolve("uploads"));
        String sourceName = uploads.resolve("upload.ipes").toString();
        String text = "GeckoFileManager\n<GeckoFileManager>\n<GeckoFile>\nhashValue 1\nisExternal 1\n"
                + (relative ? "absPath unavailable.txt\nrelPath ../private.txt\n" : "absPath " + privateFile + "\n")
                + "<\\GeckoFile>\n<\\GeckoFileManager>\n";
        var parser = new CircuitFileParser();
        var model = stream ? parser.parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), sourceName)
                : parser.parse(new BufferedReader(new StringReader(text)), sourceName);
        assertFalse(model.getResources().containsKey(1L), "Supplied circuit text must not grant host filesystem access");
        assertTrue(model.getResourceFailures().containsKey(1L));
        assertTrue(model.getImportIssues().isEmpty(), "An unused attachment must not reject the circuit");
    }
}
