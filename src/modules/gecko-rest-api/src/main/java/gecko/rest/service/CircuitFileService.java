package gecko.rest.service;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.rest.model.circuit.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for loading and parsing .ipes circuit files.
 * Uses CircuitFileParser and CircuitModel from gecko-simulation-core.
 */
@Service
public class CircuitFileService {

    private final CircuitFileParser parser = new CircuitFileParser();

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

        CircuitModel model = parsed.model;

        // Build simulation parameters
        CircuitInfo.SimulationParameters simParams = new CircuitInfo.SimulationParameters(
            model.getSimulationDuration(),
            model.getTimeStep(),
            solverTypeToString(model.getSolverType()),
            model.getPreSimulationTime(),
            model.getPreSimulationTimeStep()
        );

        // Build component counts
        CircuitInfo.ComponentCounts counts = new CircuitInfo.ComponentCounts(
            model.getCircuitComponents().size(),
            model.getControlComponents().size(),
            model.getThermalComponents().size(),
            model.getConnections().size()
        );

        // Build display settings
        CircuitInfo.DisplaySettings displaySettings = new CircuitInfo.DisplaySettings(
            model.getWindowWidth() > 0 ? model.getWindowWidth() : null,
            model.getWindowHeight() > 0 ? model.getWindowHeight() : null,
            model.getFontSize()
        );

        // Build metadata
        CircuitInfo.Metadata metadata = new CircuitInfo.Metadata(
            model.getCreationDate(),
            model.getUniqueFileId()
        );

        return new CircuitInfo(
            circuitId,
            parsed.filename,
            model.getFileVersion(),
            simParams,
            counts,
            displaySettings,
            metadata
        );
    }

    /**
     * Get component list for a circuit.
     */
    public ComponentListResponse getComponents(String circuitId) {
        ParsedCircuit parsed = circuits.get(circuitId);
        if (parsed == null) {
            return null;
        }

        CircuitModel model = parsed.model;
        List<ComponentInfo> components = new ArrayList<>();

        // Add circuit components
        for (CircuitModel.ComponentData comp : model.getCircuitComponents()) {
            components.add(componentDataToInfo(comp, "circuit"));
        }

        // Add control components
        for (CircuitModel.ComponentData comp : model.getControlComponents()) {
            components.add(componentDataToInfo(comp, "control"));
        }

        // Add thermal components
        for (CircuitModel.ComponentData comp : model.getThermalComponents()) {
            components.add(componentDataToInfo(comp, "thermal"));
        }

        return new ComponentListResponse(circuitId, components);
    }

    /**
     * Validate a circuit.
     */
    public ValidationResponse validateCircuit(String circuitId) {
        ParsedCircuit parsed = circuits.get(circuitId);
        if (parsed == null) {
            return null;
        }

        CircuitModel model = parsed.model;
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Check simulation parameters
        if (!model.hasValidSimulationParameters()) {
            errors.add("Invalid simulation parameters: time step must be positive and less than duration");
        }

        // Note: Component parsing not yet implemented in CircuitFileParser
        // Component count will be 0 for now - this is expected
        if (model.getTotalComponentCount() == 0) {
            warnings.add("Component extraction not yet implemented (circuit file parsed for metadata only)");
        }

        // Check for disconnected components (future enhancement)
        // For now, skip this check since components aren't parsed yet

        if (errors.isEmpty()) {
            return warnings.isEmpty()
                ? ValidationResponse.success()
                : ValidationResponse.successWithWarnings(warnings);
        } else {
            return ValidationResponse.failure(warnings, errors);
        }
    }

    /**
     * Get raw circuit file content (decompressed ASCII).
     */
    public String getRawCircuit(String circuitId) {
        ParsedCircuit parsed = circuits.get(circuitId);
        if (parsed == null) {
            return null;
        }
        return parsed.model.toString(); // CircuitModel doesn't store raw content, return string representation
    }

    /**
     * Delete circuit from memory.
     */
    public boolean deleteCircuit(String circuitId) {
        return circuits.remove(circuitId) != null;
    }

    /**
     * Get all loaded circuits.
     */
    public CircuitListResponse getAllCircuits() {
        List<CircuitListResponse.CircuitSummary> summaries = circuits.entrySet().stream()
            .map(entry -> {
                String id = entry.getKey();
                ParsedCircuit parsed = entry.getValue();
                return new CircuitListResponse.CircuitSummary(
                    id,
                    parsed.filename,
                    parsed.model.getTotalComponentCount(),
                    parsed.loadedAt.toString()
                );
            })
            .collect(Collectors.toList());

        return new CircuitListResponse(summaries, summaries.size());
    }

    // ========== Private Helper Methods ==========

    private CircuitLoadResponse loadCircuitFromBytes(byte[] content, String filename) {
        try {
            // Parse using CircuitFileParser
            CircuitModel model;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
                model = parser.parse(bais, filename);
            }

            // Generate unique circuit ID
            String circuitId = UUID.randomUUID().toString();

            // Create parsed circuit with timestamp
            ParsedCircuit parsed = new ParsedCircuit(
                filename,
                model,
                Instant.now()
            );

            // Store in memory
            circuits.put(circuitId, parsed);

            return CircuitLoadResponse.success(circuitId, filename, model.getTotalComponentCount());

        } catch (CircuitFileParser.CircuitParseException e) {
            return CircuitLoadResponse.failure(filename,
                    "Failed to parse circuit: " + e.getMessage());
        } catch (IOException e) {
            return CircuitLoadResponse.failure(filename,
                    "Failed to read file: " + e.getMessage());
        } catch (Exception e) {
            return CircuitLoadResponse.failure(filename,
                    "Unexpected error: " + e.getMessage());
        }
    }

    private ComponentInfo componentDataToInfo(CircuitModel.ComponentData comp, String domain) {
        return new ComponentInfo(
            comp.getType(),
            comp.getName(),
            domain,
            comp.getPosition(),
            comp.getOrientation(),
            comp.getParameters()
        );
    }

    private String solverTypeToString(gecko.core.allg.SolverType solverType) {
        return switch (solverType) {
            case SOLVER_BE -> "backward-euler";
            case SOLVER_TRZ -> "trapezoidal";
            case SOLVER_GS -> "gear-shichman";
        };
    }

    // ========== Internal Data Structure ==========

    private record ParsedCircuit(
        String filename,
        CircuitModel model,
        Instant loadedAt
    ) {}
}
