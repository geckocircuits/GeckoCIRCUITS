package gecko.core.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

/** Writes an owned circuit description without frontend objects or external resource reads. */
public final class CircuitFileWriter {
    private static final String EMPTY_LABEL = "NIX_NIX_NIX";

    /** Creates a new file. Existing paths are never overwritten. */
    public void write(CircuitModel model, Path path) throws IOException {
        byte[] bytes = write(model);
        Files.write(path, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    /** Returns a gzip-compressed UTF-8 circuit with embedded resource bytes. */
    public byte[] write(CircuitModel model) throws IOException {
        if (!model.getResourceFailures().isEmpty()) {
            throw new IOException("Capture all circuit resources before writing the circuit.");
        }
        var identifiers = identifiers(model);
        var text = new StringBuilder();
        int ordinal = 0;
        for (var data : model.getCircuitComponents()) component(text, model, data, "LK", "e", ++ordinal, identifiers.get(data));
        for (var data : model.getControlComponents()) component(text, model, data, "CONTROL", "c", ++ordinal, identifiers.get(data));
        for (var data : model.getThermalComponents()) component(text, model, data, "THERM", "eTH", ++ordinal, identifiers.get(data));
        for (var data : model.getSpecialComponents()) component(text, model, data, "SPECIAL", "sp", ++ordinal, identifiers.get(data));
        for (var wire : model.getConnections()) {
            String domain = wire.getType().equals("THERMAL") ? "THERM" : wire.getType();
            text.append("\nverbindung").append(domain).append(" (").append(++ordinal).append(")\n<Verbindung>\n");
            var tokens = tokens(wire.getSavedBlock());
            tokens.put("label", wire.getLabel().isEmpty() ? EMPTY_LABEL : scalar(wire.getLabel()));
            tokens.put("x[]", Arrays.stream(wire.getPoints()).map(p -> Integer.toString(p[0])).collect(Collectors.joining(" ")));
            tokens.put("y[]", Arrays.stream(wire.getPoints()).map(p -> Integer.toString(p[1])).collect(Collectors.joining(" ")));
            tokens.putIfAbsent("enabledShorted", "1");
            tokens.putIfAbsent("parentSheetIdentifier", "0");
            tokens.putIfAbsent("connectorType", domain.equals("CONTROL") ? "1" : domain.equals("THERM") ? "4" : "0");
            appendTokens(text, tokens);
            appendSections(text, wire.getSavedBlock());
            text.append("<\\Verbindung>\n");
        }
        var settings = new TreeMap<>(model.getSavedSettings());
        settings.remove(AcMetadataCodec.SETTINGS);
        settings.remove(AcMetadataCodec.PROVENANCE);
        if (model.getAcSettings() != null) {
            settings.put(AcMetadataCodec.SETTINGS, AcMetadataCodec.encode(model.getAcSettings()));
        }
        if (model.getAcProvenance() != null) {
            settings.put(AcMetadataCodec.PROVENANCE, AcMetadataCodec.encode(model.getAcProvenance()));
        }
        settings.keySet().removeIf(key -> key.startsWith("<") || key.startsWith("=")
                || Set.of("e", "c", "eTH", "sp", "verbindungLK", "verbindungCONTROL",
                        "verbindungTHERM", "GeckoFileManager").contains(key));
        settings.put("tDURATION", Double.toString(model.getSimulationDuration()));
        settings.put("dt", Double.toString(model.getTimeStep()));
        settings.put("T_pre", Double.toString(model.getPreSimulationTime()));
        settings.put("dt_pre", Double.toString(model.getPreSimulationTimeStep() > 0 ? model.getPreSimulationTimeStep() : model.getTimeStep()));
        settings.put("tPAUSE", Double.toString(model.getPauseTime()));
        settings.put("solverType", Integer.toString(model.getSolverType().getOldGeckoIndex()));
        settings.put("FileVersion", Integer.toString(model.getFileVersion() > 0 ? model.getFileVersion() : 202));
        settings.put("UniqueFileId", Integer.toString(model.getUniqueFileId()));
        settings.put("dpix", Integer.toString(model.getDisplayPixels()));
        settings.put("fontSize", Integer.toString(model.getFontSize()));
        settings.put("fontTyp", scalar(model.getFontType()));
        settings.put("fensterWidth", Integer.toString(model.getWindowWidth()));
        settings.put("fensterHeight", Integer.toString(model.getWindowHeight()));
        settings.putIfAbsent("path", ".");
        if (!settings.containsKey("worksheetSize")) {
            settings.putIfAbsent("worksheetSizeX", worksheetExtent(model, identifiers.keySet(), 0));
            settings.putIfAbsent("worksheetSizeY", worksheetExtent(model, identifiers.keySet(), 1));
        }
        if (model.getDataContainerSignals() != null) settings.put("dataContainerSignals[]", strings(model.getDataContainerSignals()));
        if (!model.getOptimizerParameters().isEmpty()) {
            var parameters = new TreeMap<>(model.getOptimizerParameters());
            settings.put("optimizerName[]", strings(parameters.keySet().toArray(String[]::new)));
            settings.put("optimizerValue[]", parameters.values().stream().map(Object::toString).collect(Collectors.joining(" ")));
        }
        appendTokens(text, settings);
        for (var section : model.getSavedSections()) {
            if (!section.tag().equals(ImportIssueCodec.TAG)) section(text, section.tag(), section.text());
        }
        if (!model.getResources().isEmpty()) {
            text.append("GeckoFileManager\n<GeckoFileManager>\n");
            for (var resource : new TreeMap<>(model.getResources()).values()) {
                text.append("<GeckoFile>\nfileSep /\nhashValue ").append(resource.id())
                        .append("\nisExternal 0\nabsPath ").append(scalar(resource.name()))
                        .append("\nrelPath ").append(scalar(resource.name())).append("\nusageList[] ");
                for (long owner : resource.ownerIds()) text.append('/').append(owner);
                text.append("\nfileContents[]");
                for (byte value : resource.bytes()) text.append(' ').append(value);
                text.append("\n<\\GeckoFile>\n");
            }
            text.append("<\\GeckoFileManager>\n");
        }
        text.append(ImportIssueCodec.encodeSection(model.getConversionIssues()));
        var bytes = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(bytes)) { gzip.write(text.toString().getBytes(StandardCharsets.UTF_8)); }
        return bytes.toByteArray();
    }

    private static void component(StringBuilder text, CircuitModel model, CircuitModel.ComponentData data,
            String domain, String marker, int ordinal, long identifier) throws IOException {
        if (!data.isParsedInput() && data.getParameters().keySet().stream().anyMatch(key -> !key.equals("uniqueObjectIdentifier"))) {
            throw new IOException("Set the saved parameter slots and tokens for " + data.getName() + ".");
        }
        text.append('\n').append(marker).append(" (").append(ordinal).append(")\n<Element").append(domain).append(">\n");
        var tokens = tokens(data.getSavedBlock());
        tokens.putAll(data.getImportTokens());
        tokens.remove(AcMetadataCodec.COMPONENT);
        if (data.getAcComponent() != null) {
            tokens.put(AcMetadataCodec.COMPONENT, AcMetadataCodec.encode(data.getAcComponent()));
        }
        tokens.put("typ", Integer.toString(data.getType()));
        tokens.put("idStringDialog", scalar(data.getName()));
        tokens.put("x", Integer.toString(data.getPosition()[0]));
        tokens.put("y", Integer.toString(data.getPosition()[1]));
        tokens.put("orientierung", Integer.toString(data.getOrientation() == 0 ? 503 : data.getOrientation()));
        if (!domain.equals("SPECIAL") || data.getSavedBlock() == null) {
            tokens.put("labelAnfangsKnoten[]", strings(data.getTerminalXLabels()));
            tokens.put("labelEndKnoten[]", strings(data.getTerminalYLabels()));
            double[] values = data.getAcComponent() == null ? data.getNumericParameters()
                    : AcMetadataCodec.compatibilityValues(data.getAcComponent(), data.getNumericParameters(), model.getAcSettings());
            tokens.put("parameter[]", Arrays.stream(values).mapToObj(Double::toString)
                    .collect(Collectors.joining(" ")));
            tokens.put("parameterString[]", strings(data.getParameterStrings()));
        }
        tokens.putIfAbsent("dxTxt", "2.0");
        tokens.putIfAbsent("dyTxt", "2.0");
        tokens.putIfAbsent("nameOpt[]", "null");
        tokens.putIfAbsent("enabledShorted", "1");
        tokens.putIfAbsent("parentSheetIdentifier", "0");
        tokens.put("uniqueObjectIdentifier", Long.toString(identifier));
        appendTokens(text, tokens);
        appendSections(text, data.getSavedBlock());
        text.append("<\\Element").append(domain).append(">\n");
    }

    private static Map<CircuitModel.ComponentData, Long> identifiers(CircuitModel model) throws IOException {
        var components = Stream.of(model.getCircuitComponents(), model.getControlComponents(),
                model.getThermalComponents(), model.getSpecialComponents()).flatMap(java.util.Collection::stream).toList();
        var identifiers = new HashMap<CircuitModel.ComponentData, Long>();
        var blocks = new HashSet<SavedCircuitBlock>();
        var used = new HashSet<Long>();
        for (var data : components) {
            if (data.getSavedBlock() != null) blocks.add(data.getSavedBlock());
            String saved = data.getImportTokens().get("uniqueObjectIdentifier");
            if (saved == null && data.getSavedBlock() != null) saved = data.getSavedBlock().tokens().get("uniqueObjectIdentifier");
            Object explicit = data.getParameters().get("uniqueObjectIdentifier");
            if (saved == null && explicit == null) continue;
            try {
                long id = Long.parseLong(saved == null ? explicit.toString() : saved);
                if (id != 0 && !used.add(id)) throw new IOException("The component identifier " + id + " is duplicated.");
                if (explicit != null && Long.parseLong(explicit.toString()) != id) {
                    throw new IOException("The component identifiers disagree for " + data.getName() + ".");
                }
                identifiers.put(data, id);
            } catch (NumberFormatException error) {
                throw new IOException("The component identifier is invalid for " + data.getName() + ".", error);
            }
        }
        for (var block : model.getSavedBlocks()) {
            if (!block.tag().equals("Verbindung") && !blocks.contains(block)) {
                throw new IOException("This writer cannot serialize the saved " + block.tag() + " block.");
            }
        }
        long generated = 1;
        for (var data : components) {
            if (identifiers.containsKey(data)) continue;
            while (used.contains(generated)) generated++;
            identifiers.put(data, generated++);
        }
        return identifiers;
    }

    private static String worksheetExtent(CircuitModel model, Set<CircuitModel.ComponentData> components, int axis) {
        int max = 0;
        for (var data : components) max = Math.max(max, data.getPosition()[axis]);
        for (var wire : model.getConnections()) {
            for (int[] point : wire.getPoints()) max = Math.max(max, point[axis]);
        }
        return Long.toString(Math.max(30, max + 16L));
    }

    private static TreeMap<String, String> tokens(SavedCircuitBlock block) {
        return block == null ? new TreeMap<>() : new TreeMap<>(block.tokens());
    }

    private static void appendTokens(StringBuilder text, Map<String, String> tokens) {
        tokens.forEach((key, value) -> text.append(scalar(key)).append(' ').append(scalar(value)).append('\n'));
    }

    private static void appendSections(StringBuilder text, SavedCircuitBlock block) {
        if (block == null) return;
        for (var section : block.orderedSections()) section(text, section.tag(), section.text());
    }

    private static void section(StringBuilder text, String tag, String content) {
        text.append('<').append(tag).append(">\n").append(content).append("\n<\\").append(tag).append(">\n");
    }

    private static String strings(String[] values) {
        var text = new StringBuilder();
        for (String value : values) {
            if (value != null && value.contains("/")) throw new IllegalArgumentException("A circuit label cannot contain a slash.");
            text.append('/').append(value == null || value.isEmpty() ? EMPTY_LABEL : scalar(value));
        }
        return text.toString();
    }

    private static String scalar(String value) {
        if (value == null) return "";
        if (value.contains("\n") || value.contains("\r")) throw new IllegalArgumentException("A circuit token cannot contain a line break.");
        return value;
    }
}
