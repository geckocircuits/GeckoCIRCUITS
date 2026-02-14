package gecko.rest.service;

import gecko.rest.model.circuit.CircuitInfo;
import gecko.rest.model.circuit.CircuitLoadResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CircuitFileService.
 */
class CircuitFileServiceTest {

    private final CircuitFileService service = new CircuitFileService();

    @Test
    void testLoadCircuit_fromBase64() throws IOException {
        // Create a simple .ipes file content
        String ipesContent = """
            version 1.0
            <Element>
            componentType RESISTOR
            xPos 100
            yPos 200
            </Element>
            """;

        // Compress with GZIP
        byte[] compressed = compressGzip(ipesContent);

        // Encode to base64
        String base64 = Base64.getEncoder().encodeToString(compressed);

        // Load circuit
        CircuitLoadResponse response = service.loadCircuit(base64, "test.ipes");

        // Verify success
        assertNotNull(response);
        assertEquals("loaded", response.status());
        assertEquals("test.ipes", response.filename());
        assertNotNull(response.circuitId());
        assertTrue(response.componentCount() >= 0);
    }

    @Test
    void testLoadCircuit_invalidBase64() {
        CircuitLoadResponse response = service.loadCircuit("invalid-base64!", "test.ipes");

        assertNotNull(response);
        assertEquals("failed", response.status());
        assertNotNull(response.errorMessage());
    }

    @Test
    void testGetCircuitInfo_found() throws IOException {
        // Load a circuit first
        String ipesContent = """
            version 1.0
            <Element>
            componentType MOSFET
            </Element>
            """;

        byte[] compressed = compressGzip(ipesContent);
        String base64 = Base64.getEncoder().encodeToString(compressed);

        CircuitLoadResponse loadResponse = service.loadCircuit(base64, "test.ipes");
        String circuitId = loadResponse.circuitId();

        // Get info
        CircuitInfo info = service.getCircuitInfo(circuitId);

        assertNotNull(info);
        assertEquals(circuitId, info.circuitId());
        assertEquals("test.ipes", info.filename());
        assertNotNull(info.components());
    }

    @Test
    void testGetCircuitInfo_notFound() {
        CircuitInfo info = service.getCircuitInfo("nonexistent-id");
        assertNull(info);
    }

    @Test
    void testGetRawCircuit() throws IOException {
        String ipesContent = "version 1.0\ntest content";

        byte[] compressed = compressGzip(ipesContent);
        String base64 = Base64.getEncoder().encodeToString(compressed);

        CircuitLoadResponse loadResponse = service.loadCircuit(base64, "test.ipes");
        String circuitId = loadResponse.circuitId();

        String raw = service.getRawCircuit(circuitId);

        assertNotNull(raw);
        assertTrue(raw.contains("version"));
    }

    @Test
    void testDeleteCircuit() throws IOException {
        String ipesContent = "version 1.0";

        byte[] compressed = compressGzip(ipesContent);
        String base64 = Base64.getEncoder().encodeToString(compressed);

        CircuitLoadResponse loadResponse = service.loadCircuit(base64, "test.ipes");
        String circuitId = loadResponse.circuitId();

        // Delete
        boolean deleted = service.deleteCircuit(circuitId);
        assertTrue(deleted);

        // Verify deleted
        CircuitInfo info = service.getCircuitInfo(circuitId);
        assertNull(info);

        // Try deleting again
        boolean deletedAgain = service.deleteCircuit(circuitId);
        assertFalse(deletedAgain);
    }

    // Helper method
    private byte[] compressGzip(String content) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
            gzipStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return byteStream.toByteArray();
    }
}
