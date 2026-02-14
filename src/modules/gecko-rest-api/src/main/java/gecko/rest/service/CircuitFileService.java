package gecko.rest.service;

import gecko.core.allg.GeckoFile;
import gecko.core.circuit.TokenMap;
import gecko.rest.model.circuit.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

/**
 * Service for loading and parsing .ipes circuit files.
 * Uses GeckoFile and TokenMap from gecko-simulation-core.
 */
@Service
public class CircuitFileService {

    // In-memory storage of parsed circuits (circuit ID -> parsed data)
    private final Map<String, ParsedCircuit> circuits = new ConcurrentHashMap<>();

    /**
     * Load circuit from multipart file upload.
     */
    public CircuitLoadResponse loadCircuit(MultipartFile file) {
        try {
            // Read file content
            byte[] content = file.getBytes();
            String filename = file.getOriginalFilename();

            return loadCircuitFromBytes(content, filename);
        } catch (IOException e) {
            return CircuitLoadResponse.failure(file.getOriginalFilename(),
                    "Failed to read file: " + e.getMessage());
        }
    }

    /**
     * Load circuit from base64 encoded content.
     */
    public CircuitLoadResponse loadCircuit(String base64Content, String filename) {
        try {
            // Decode base64
            byte[] content = Base64.getDecoder().decode(base64Content);
            return loadCircuitFromBytes(content, filename);
        } catch (IllegalArgumentException e) {
            return CircuitLoadResponse.failure(filename,
                    "Invalid base64 encoding: " + e.getMessage());
        }
    }

    /**
     * Get detailed circuit information.
     */
    public CircuitInfo getCircuitInfo(String circuitId) {
        ParsedCircuit parsed = circuits.get(circuitId);
        if (parsed == null) {
            return null;
        }

        return new CircuitInfo(
            circuitId,
            parsed.filename,
            parsed.version,
            parsed.components,
            parsed.connections,
            parsed.simulationParameters
        );
    }

    /**
     * Get raw circuit file content (decompressed ASCII).
     */
    public String getRawCircuit(String circuitId) {
        ParsedCircuit parsed = circuits.get(circuitId);
        if (parsed == null) {
            return null;
        }
        return parsed.rawContent;
    }

    /**
     * Delete circuit from memory.
     */
    public boolean deleteCircuit(String circuitId) {
        return circuits.remove(circuitId) != null;
    }

    /**
     * Get all circuit IDs.
     */
    public List<String> getAllCircuitIds() {
        return new ArrayList<>(circuits.keySet());
    }

    // ========== Private Helper Methods ==========

    private CircuitLoadResponse loadCircuitFromBytes(byte[] content, String filename) {
        try {
            // Decompress .ipes file (gzip compressed)
            String[] lines = decompressIpesFile(content);

            // Parse with TokenMap
            TokenMap tokenMap = new TokenMap(lines);

            // Extract circuit info
            ParsedCircuit parsed = parseCircuit(tokenMap, filename, String.join("\n", lines));

            // Generate unique circuit ID
            String circuitId = UUID.randomUUID().toString();

            // Store in memory
            circuits.put(circuitId, parsed);

            return CircuitLoadResponse.success(circuitId, filename, parsed.components.size());

        } catch (IOException e) {
            return CircuitLoadResponse.failure(filename,
                    "Failed to decompress file: " + e.getMessage());
        } catch (Exception e) {
            return CircuitLoadResponse.failure(filename,
                    "Failed to parse circuit: " + e.getMessage());
        }
    }

    private String[] decompressIpesFile(byte[] content) throws IOException {
        List<String> lines = new ArrayList<>();

        try (GZIPInputStream gzipStream = new GZIPInputStream(new ByteArrayInputStream(content));
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzipStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        return lines.toArray(new String[0]);
    }

    private ParsedCircuit parseCircuit(TokenMap tokenMap, String filename, String rawContent) {
        // Extract version
        String version = "1.0"; // Default
        if (tokenMap.containsToken("version")) {
            version = tokenMap.readDataLine("version", version);
        }

        // Extract components
        List<ComponentInfo> components = new ArrayList<>();
        int componentIndex = 0;

        // Iterate through all component blocks
        // Note: TokenMap.getBlockTokenMap() removes the block after reading, so we need to read all at once
        while (true) {
            TokenMap componentBlock = tokenMap.getBlockTokenMap("<Element>");
            if (componentBlock == null) {
                break;
            }

            ComponentInfo component = parseComponent(componentBlock, componentIndex++);
            if (component != null) {
                components.add(component);
            }
        }

        // Extract connections (simplified - actual connection parsing is complex)
        List<ConnectionInfo> connections = new ArrayList<>();

        // Extract simulation parameters
        CircuitInfo.SimulationParameters simParams = parseSimulationParameters(tokenMap);

        return new ParsedCircuit(filename, version, components, connections, simParams, rawContent);
    }

    private ComponentInfo parseComponent(TokenMap componentBlock, int index) {
        try {
            // Extract component type (simplified)
            String type = "UNKNOWN";
            if (componentBlock.containsToken("componentType")) {
                type = componentBlock.readDataLine("componentType", type);
            }

            // Extract ID/label (simplified)
            String id = "C" + index;
            String label = id;

            // Extract position (simplified)
            int xPos = componentBlock.readDataLine("xPos", 0);
            int yPos = componentBlock.readDataLine("yPos", 0);

            return new ComponentInfo(type, id, label, xPos, yPos);

        } catch (Exception e) {
            // Skip malformed components
            return null;
        }
    }

    private CircuitInfo.SimulationParameters parseSimulationParameters(TokenMap tokenMap) {
        try {
            Double endTime = null;
            Double timeStep = null;
            String solverType = null;

            if (tokenMap.containsToken("t_end")) {
                endTime = tokenMap.readDataLine("t_end", 0.0);
            }
            if (tokenMap.containsToken("dt")) {
                timeStep = tokenMap.readDataLine("dt", 0.0);
            }
            if (tokenMap.containsToken("solver")) {
                solverType = tokenMap.readDataLine("solver", "SOLVER_BE");
            }

            return new CircuitInfo.SimulationParameters(endTime, timeStep, solverType);

        } catch (Exception e) {
            return null;
        }
    }

    // ========== Internal Data Structure ==========

    private record ParsedCircuit(
        String filename,
        String version,
        List<ComponentInfo> components,
        List<ConnectionInfo> connections,
        CircuitInfo.SimulationParameters simulationParameters,
        String rawContent
    ) {}
}
