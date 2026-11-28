package gecko.rest.service;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class CircuitFileTopologyContractTest {
    private static byte[] fixture() throws IOException {
        return Files.readAllBytes(Path.of("src/test/resources/test-circuit.ipes"));
    }

    @Test
    void plainAndGzipUploadsExposeTheSameCircuit() throws Exception {
        byte[] compressed = fixture();
        byte[] plain;
        try (InputStream input = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            plain = input.readAllBytes();
        }
        CircuitFileService service = new CircuitFileService();
        var gzip = service.loadCircuit(Base64.getEncoder().encodeToString(compressed), "compressed.ipes");
        var text = service.loadCircuit(Base64.getEncoder().encodeToString(plain), "plain.ipes");
        assertEquals("loaded", gzip.status(), gzip.errorMessage());
        assertEquals("loaded", text.status(), text.errorMessage());
        assertEquals(24, gzip.componentCount());
        assertEquals(gzip.componentCount(), text.componentCount());
        var gzipInfo = service.getCircuitInfo(gzip.circuitId());
        var textInfo = service.getCircuitInfo(text.circuitId());
        assertEquals(gzipInfo.componentCounts(), textInfo.componentCounts());
        assertEquals(gzipInfo.simulationParameters(), textInfo.simulationParameters());
        assertEquals(service.getComponents(gzip.circuitId()).components().stream().map(c -> c.name()).toList(),
                service.getComponents(text.circuitId()).components().stream().map(c -> c.name()).toList());
    }

    @Test
    void multipartGzipUploadExtractsComponentsAndWires() throws Exception {
        CircuitFileService service = new CircuitFileService();
        var upload = new MockMultipartFile("file", "upload.ipes", "application/gzip", fixture());
        var response = service.loadCircuit(upload);
        assertEquals("loaded", response.status(), response.errorMessage());
        assertEquals(24, response.componentCount());
        assertEquals(10, service.getCircuitInfo(response.circuitId()).componentCounts().connections());
    }

    @Test
    void truncatedGzipUploadFailsWithoutPublishingAnEmptyCircuit() throws Exception {
        CircuitFileService service = new CircuitFileService();
        var response = service.loadCircuit(Base64.getEncoder().encodeToString(Arrays.copyOf(fixture(), 10)), "broken.ipes");
        assertEquals("failed", response.status());
        assertTrue(response.errorMessage().contains("read file"), response.errorMessage());
        assertEquals(0, service.getAllCircuits().total());
    }

    @Test
    void aClonedParsedCircuitRetainsIndependentTopologyAndControlData() throws Exception {
        CircuitModel source = new CircuitFileParser().parse(
                new ByteArrayInputStream(fixture()), "uploaded.ipes");
        CircuitModel copy = new CircuitFileService().copyCircuitModel(source);
        List<CircuitModel.ComponentData> originals = new ArrayList<>(source.getCircuitComponents());
        originals.addAll(source.getControlComponents());
        List<CircuitModel.ComponentData> clones = new ArrayList<>(copy.getCircuitComponents());
        clones.addAll(copy.getControlComponents());
        assertEquals(24, clones.size());
        for (int i = 0; i < originals.size(); i++) {
            var original = originals.get(i);
            var clone = clones.get(i);
            assertArrayEquals(original.getTerminalXLabels(), clone.getTerminalXLabels(), original.getName());
            assertArrayEquals(original.getTerminalYLabels(), clone.getTerminalYLabels(), original.getName());
            assertArrayEquals(original.getParameterStrings(), clone.getParameterStrings(), original.getName());
            assertArrayEquals(original.getNumericParameters(), clone.getNumericParameters(), original.getName());
            if (clone.getTerminalXLabels().length > 0) {
                String saved = original.getTerminalXLabels()[0];
                clone.getTerminalXLabels()[0] = "changed";
                assertEquals(saved, original.getTerminalXLabels()[0]);
            }
            if (clone.getParameterStrings().length > 0) {
                String saved = original.getParameterStrings()[0];
                clone.getParameterStrings()[0] = "changed";
                assertEquals(saved, original.getParameterStrings()[0]);
            }
            if (clone.getNumericParameters().length > 0) {
                double saved = original.getNumericParameters()[0];
                clone.getNumericParameters()[0] = -123;
                assertEquals(saved, original.getNumericParameters()[0]);
            }
        }
        assertEquals(10, copy.getConnections().size());
        for (int i = 0; i < source.getConnections().size(); i++) {
            var original = source.getConnections().get(i);
            var clone = copy.getConnections().get(i);
            assertEquals(original.getLabel(), clone.getLabel());
            assertTrue(Arrays.deepEquals(original.getPoints(), clone.getPoints()));
            int saved = original.getPoints()[0][0];
            clone.getPoints()[0][0] = -123;
            assertEquals(saved, original.getPoints()[0][0]);
        }
    }
}
