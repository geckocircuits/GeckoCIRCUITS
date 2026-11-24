package gecko.geckocircuits.circuit;

import gecko.core.allg.GeckoFile;
import gecko.core.ac.AcDocumentSettings;
import gecko.core.io.AcProvenance;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitHierarchy;
import gecko.core.io.HierarchyIndex;
import gecko.core.io.HierarchyMetadataCodec;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitResource;
import gecko.core.io.CircuitResourceFailure;
import gecko.core.io.CircuitTopology;
import gecko.core.io.ImportIssue;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.CircuitSnapshot;
import gecko.geckocircuits.allg.GeckoFileManager;
import gecko.geckocircuits.allg.OptimizerParameterData;
import gecko.geckocircuits.allg.ProjectData;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import gecko.geckocircuits.control.SubCircuitSheet;

/** Captures unsaved editor configuration. The caller owns editor-thread synchronization. */
public final class EditorCircuitCapture {
    public record ScriptSources(String code, String imports, String declarations, String extraFiles) {
        public static final ScriptSources EMPTY = new ScriptSources("", "", "", "");
    }

    /**
     * Captures the complete flattened sheet list with its parent identifiers.
     * Use CircuitSheet.getAllElements to include all nested sheets.
     * This method does not read results or desktop globals.
     */
    public CircuitSnapshot capture(Iterable<? extends AbstractCircuitSheetComponent> elements,
            SolverSettings settings, OptimizerParameterData optimizer, GeckoFileManager files,
            ScriptSources scripts, String sourceName) throws IOException, CircuitFileParser.CircuitParseException {
        return capture(elements, settings, optimizer, files, scripts, sourceName, null, null);
    }

    public CircuitSnapshot capture(Iterable<? extends AbstractCircuitSheetComponent> elements,
            SolverSettings settings, OptimizerParameterData optimizer, GeckoFileManager files,
            ScriptSources scripts, String sourceName, List<ImportIssue> conversionIssues)
            throws IOException, CircuitFileParser.CircuitParseException {
        return capture(elements, settings, optimizer, files, scripts, sourceName, null, null, conversionIssues);
    }

    public CircuitSnapshot capture(Iterable<? extends AbstractCircuitSheetComponent> elements,
            SolverSettings settings, OptimizerParameterData optimizer, GeckoFileManager files,
            ScriptSources scripts, String sourceName, AcDocumentSettings acSettings, AcProvenance provenance)
            throws IOException, CircuitFileParser.CircuitParseException {
        return capture(elements, settings, optimizer, files, scripts, sourceName, acSettings, provenance, List.of());
    }

    public CircuitSnapshot capture(Iterable<? extends AbstractCircuitSheetComponent> elements,
            SolverSettings settings, OptimizerParameterData optimizer, GeckoFileManager files,
            ScriptSources scripts, String sourceName, AcDocumentSettings acSettings, AcProvenance provenance,
            List<ImportIssue> conversionIssues) throws IOException, CircuitFileParser.CircuitParseException {
        return capture(elements, settings, optimizer, files, scripts, sourceName, acSettings, provenance, conversionIssues, null);
    }

    public CircuitSnapshot capture(Iterable<? extends AbstractCircuitSheetComponent> elements,
            SolverSettings settings, OptimizerParameterData optimizer, GeckoFileManager files,
            ScriptSources scripts, String sourceName, AcDocumentSettings acSettings, AcProvenance provenance,
            List<ImportIssue> conversionIssues, CircuitHierarchy hierarchy)
            throws IOException, CircuitFileParser.CircuitParseException {
        var ascii = new StringBuffer();
        var blocks = new ArrayList<AbstractBlockInterface>();
        for (var element : elements) {
            ascii.append('\n').append(element.getExportImportCharacters()).append('\n');
            if (element instanceof AbstractBlockInterface block) {
                block.exportForCapture(ascii, acSettings);
                blocks.add(block);
            } else element.exportASCII(ascii);
        }
        ProjectData.appendAsString(ascii.append("\noptimizerName"), optimizer.getNameOpt());
        ProjectData.appendAsString(ascii.append("\noptimizerValue"), optimizer.getValueOpt());
        section(ascii, "scripterCode", scripts.code());
        section(ascii, "scripterImports", scripts.imports());
        section(ascii, "scripterDeclarations", scripts.declarations());
        section(ascii, "extraScriptSourceFiles", scripts.extraFiles());
        ascii.append("\ntDURATION ").append(settings._tDURATION.getValue());
        ascii.append("\ndt ").append(settings.dt.getValue());
        ascii.append("\ntPAUSE ").append(settings._tPAUSE.getValue());
        ascii.append("\nT_pre ").append(settings._T_pre.getValue());
        ascii.append("\ndt_pre ").append(settings._dt_pre.getValue());
        ascii.append("\nsolverType ").append(settings.SOLVER_TYPE.getValue().getOldGeckoIndex());
        appendHierarchy(ascii, hierarchy);
        var model = new CircuitFileParser().parse(new BufferedReader(new StringReader(ascii.toString())), sourceName);
        conversionIssues.forEach(model::addConversionIssue);
        model.setAcSettings(acSettings);
        model.setAcProvenance(provenance);
        model.setHierarchy(hierarchy);
        for (var file : files._allAvailableFiles) captureResource(model, file);
        var ordinals = new HashMap<String, Integer>();
        for (var block : blocks) {
            String marker = block.getExportImportCharacters().trim();
            List<CircuitModel.ComponentData> components = switch (marker) {
                case "e" -> model.getCircuitComponents();
                case "c" -> model.getControlComponents();
                case "eTH" -> model.getThermalComponents();
                case "sp" -> model.getSpecialComponents();
                default -> List.of();
            };
            int ordinal = ordinals.getOrDefault(marker, 0);
            ordinals.put(marker, ordinal + 1);
            var component = components.get(ordinal);
            captureTerminals(component, block.XIN, CircuitTopology.Side.INPUT);
            captureTerminals(component, block.YOUT, CircuitTopology.Side.OUTPUT);
        }
        return new CircuitPreparation().capture(model);
    }

    static HierarchyIndex inspectHierarchy(Iterable<? extends AbstractCircuitSheetComponent> elements,
                                           CircuitHierarchy hierarchy) {
        var ascii = new StringBuffer();
        for (var element : elements) {
            ascii.append('\n').append(element.getExportImportCharacters()).append('\n');
            element.exportASCII(ascii);
        }
        appendHierarchy(ascii, hierarchy);
        try {
            var model = new CircuitFileParser().parse(new BufferedReader(new StringReader(ascii.toString())), "editor");
            model.setHierarchy(hierarchy);
            return new HierarchyIndex(model);
        } catch (IOException | CircuitFileParser.CircuitParseException error) {
            throw new IllegalArgumentException(error.getMessage(), error);
        }
    }

    private static void appendHierarchy(StringBuffer ascii, CircuitHierarchy hierarchy) {
        if (hierarchy != null) ascii.append('\n').append(HierarchyMetadataCodec.DOCUMENT).append(' ')
                .append(HierarchyMetadataCodec.encode(hierarchy));
    }

    private static void captureTerminals(CircuitModel.ComponentData component, List<AbstractTerminal> terminals,
            CircuitTopology.Side side) {
        for (int i = 0; i < terminals.size(); i++) {
            var terminal = terminals.get(i);
            var position = terminal.getPosition();
            long sheet = terminal.getCircuitSheet() instanceof SubCircuitSheet sub
                    ? sub._subBlock.getUniqueObjectIdentifier() : 0;
            component.getTerminalData().add(new CircuitModel.TerminalData(terminal.getCategory().name(), sheet,
                    side, i, position.x, position.y, !(terminal instanceof TerminalHiddenSubcircuit)));
        }
    }

    private static void captureResource(CircuitModel model, GeckoFile file) {
        long id = file.getHashValue();
        String path = file.getCurrentAbsolutePath();
        List<Long> owners = file.getOwnerIds();
        try {
            byte[] bytes = resourceBytes(file);
            if (bytes == null) throw new IOException("Resource bytes are missing");
            model.getResources().put(id, new CircuitResource(id, file.getName(), path, bytes, owners));
        } catch (IOException error) {
            model.getResourceFailures().put(id, new CircuitResourceFailure(id, path, owners,
                    "The circuit resource cannot be captured: " + error.getMessage()));
        }
    }

    private static byte[] resourceBytes(GeckoFile file) throws IOException {
        if (file.getStorageType() == GeckoFile.StorageType.INTERNAL) return file.getContentsByte();
        try (var input = file.getInputStream()) {
            return input == null ? null : input.readAllBytes();
        }
    }

    private static void section(StringBuffer ascii, String tag, String text) {
        ascii.append("\n<").append(tag).append(">\n").append(text).append("\n<\\").append(tag).append('>');
    }
}
