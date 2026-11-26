package gecko.core.io;

import gecko.core.circuit.waveform.PiecewiseLinearWaveform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Captures resource bytes once. No execution path needs the source file. */
final class CircuitResourceReader {
    private CircuitResourceReader() { }

    static void retain(CircuitModel model, String[] lines, String[] structure,
            CircuitFileParser.ResourceAccess resourceAccess) {
        retain(model, lines, structure, resourceAccess, CircuitResourceReader::externalBytes);
    }

    @FunctionalInterface
    interface ExternalResourceReader {
        byte[] read(CircuitModel model, SavedCircuitBlock block) throws IOException;
    }

    static void retain(CircuitModel model, String[] lines, String[] structure,
            CircuitFileParser.ResourceAccess resourceAccess, ExternalResourceReader externalReader) {
        var sources = new ArrayList<SavedPwlResources.Source>();
        for (var component : components(model)) {
            var saved = component.getSavedBlock();
            if (saved == null) continue;
            boolean electricalSource = model.getCircuitComponents().contains(component)
                    && (component.getType() == 4 || component.getType() == 5);
            var source = SavedPwlResources.source(saved.serializedText().lines().toArray(String[]::new), electricalSource);
            if (source != null) {
                if (!model.hasUniqueComponentIdentifier(source.ownerId())) {
                    throw new IllegalArgumentException("Use a unique numeric owner for the PWL resource.");
                }
                sources.add(source);
            }
        }
        var blocks = new ArrayList<String[]>();
        for (int start = 0; start < lines.length; start++) {
            if (!structure[start].trim().equals("<GeckoFileManager>")) continue;
            int managerEnd = SavedCircuitBlock.closingLine(lines, start, "GeckoFileManager");
            for (int i = start + 1; i < managerEnd; i++) {
                if (!lines[i].trim().equals("<GeckoFile>")) continue;
                int end = SavedCircuitBlock.closingLine(lines, i, "GeckoFile");
                blocks.add(Arrays.copyOfRange(lines, i + 1, end));
                i = end;
            }
            start = managerEnd;
        }
        var embeddedPwl = SavedPwlResources.preflight(sources, blocks);
        for (int i = 0; i < blocks.size(); i++) {
            var block = SavedCircuitBlock.parse("GeckoFile", blocks.get(i));
            var pwlRecord = embeddedPwl.get(i);
            if (pwlRecord != null) {
                retainEmbedded(model, block, pwlRecord);
            } else {
                retainOrdinary(model, block, resourceAccess, externalReader);
            }
        }
    }

    private static void retainEmbedded(CircuitModel model, SavedCircuitBlock block, SavedPwlResources.Embedded pwl) {
        String path = block.tokens().getOrDefault("absPath", "");
        model.getResources().put(pwl.resourceId(), new CircuitResource(pwl.resourceId(), name(path),
                path, pwl.bytes(), List.of(pwl.ownerId())));
    }

    private static String name(String path) {
        String normalized = path.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private static void retainOrdinary(CircuitModel model, SavedCircuitBlock block,
            CircuitFileParser.ResourceAccess resourceAccess, ExternalResourceReader externalReader) {
                try {
                    long id = Long.parseLong(block.tokens().get("hashValue"));
                    String path = block.tokens().getOrDefault("absPath", "");
                    String embedded = block.tokens().get("fileContents[]");
                    boolean internal = "0".equals(block.tokens().get("isExternal"));
                    boolean hasEmbedded = embedded != null && !embedded.equals("null");
                    byte[] bytes;
                    if (hasEmbedded) {
                        bytes = embeddedBytes(embedded);
                    } else if (resourceAccess == CircuitFileParser.ResourceAccess.LOCAL_FILES
                            && !block.tokens().getOrDefault("isExternal", "1").equals("0")) {
                        bytes = externalReader.read(model, block);
                    } else {
                        throw new IOException("The supplied circuit does not include resource bytes");
                    }
                    boolean pwl = PiecewiseLinearWaveform.hasHeader(bytes);
                    if (pwl && (!internal || !hasEmbedded)) {
                        throw new IllegalArgumentException("Declare the PWL resource internal and embed its data.");
                    }
                    List<Long> ownerIds = pwl
                            ? pwlOwner(block) : owners(model, block);
                    if (pwl && !model.hasUniqueComponentIdentifier(ownerIds.getFirst())) {
                        throw new IllegalArgumentException("Use a unique numeric owner for the PWL resource.");
                    }
                    model.getResources().put(id, new CircuitResource(id, name(path), path, bytes, ownerIds));
                } catch (IOException | RuntimeException error) {
                    long id;
                    try { id = Long.parseLong(block.tokens().getOrDefault("hashValue", "0")); }
                    catch (NumberFormatException invalidId) { id = 0; }
                    model.getResourceFailures().put(id, new CircuitResourceFailure(id,
                            block.tokens().getOrDefault("absPath", ""), owners(model, block),
                            "The circuit resource cannot be captured: " + error.getMessage()));
                }
    }

    private static byte[] embeddedBytes(String text) {
        if (text == null || text.equals("null")) throw new IllegalArgumentException("Resource bytes are missing");
        String[] values = text.isBlank() ? new String[0] : text.trim().split("\\s+");
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) bytes[i] = Byte.parseByte(values[i]);
        return bytes;
    }

    private static byte[] externalBytes(CircuitModel model, SavedCircuitBlock block) throws IOException {
        String separator = block.tokens().getOrDefault("fileSep", "/");
        String absolute = block.tokens().getOrDefault("absPath", "").replace(separator, java.io.File.separator);
        String relative = block.tokens().getOrDefault("relPath", "").replace(separator, java.io.File.separator);
        if (!absolute.isBlank() && Files.isRegularFile(Path.of(absolute))) return Files.readAllBytes(Path.of(absolute));
        if (!relative.isBlank() && model.getFilePath() != null) {
            Path source = Path.of(model.getFilePath()).toAbsolutePath();
            Path resolved = source.getParent().resolve(relative).normalize();
            if (Files.isRegularFile(resolved)) return Files.readAllBytes(resolved);
        }
        throw new IOException("Resource file is missing: " + absolute);
    }

    private static List<Long> pwlOwner(SavedCircuitBlock block) {
        String owner = block.tokens().getOrDefault("usageList[]", "").strip();
        if (!owner.matches("/?-?[0-9]+")) {
            throw new IllegalArgumentException("A PWL resource requires exactly one numeric owner.");
        }
        return List.of(Long.parseLong(owner.startsWith("/") ? owner.substring(1) : owner));
    }

    private static List<Long> owners(CircuitModel model, SavedCircuitBlock block) {
        String text = block.tokens().getOrDefault("usageList[]", block.sections().getOrDefault("usageList", ""));
        var owners = new ArrayList<Long>();
        for (String item : text.split("[\\s/]+")) {
            if (item.isBlank() || item.equals("null")) continue;
            try {
                owners.add(Long.parseLong(item));
            } catch (NumberFormatException error) {
                // Versions before 1.62 bind resource owners by component name.
                for (var component : components(model)) {
                    if (component.getName().equals(item) && component.getSavedBlock() != null) {
                        owners.add(component.getSavedBlock().uniqueObjectIdentifier());
                    }
                }
            }
        }
        return owners;
    }

    private static List<CircuitModel.ComponentData> components(CircuitModel model) {
        var result = new ArrayList<>(model.getCircuitComponents());
        result.addAll(model.getControlComponents());
        result.addAll(model.getThermalComponents());
        result.addAll(model.getSpecialComponents());
        return result;
    }
}
