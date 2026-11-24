/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.io;

import gecko.core.allg.SolverType;
import gecko.core.circuit.TokenMap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * GUI-free parser for GeckoCIRCUITS .ipes circuit files.
 * Extracts simulation parameters and component data for headless operation.
 *
 * <p>This parser is designed for REST APIs, CLI tools, and batch processing,
 * containing no Swing/AWT dependencies.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * CircuitFileParser parser = new CircuitFileParser();
 * CircuitModel model = parser.parse("path/to/circuit.ipes");
 * double dt = model.getTimeStep();
 * double duration = model.getSimulationDuration();
 * }</pre>
 */
public class CircuitFileParser {
    enum ResourceAccess { SUPPLIED_BYTES_ONLY, LOCAL_FILES }

    private static final String NIX = "NIX_NIX_NIX";
    private static final String SEPARATOR_ASCII_STRINGARRAY = "/";

    /**
     * Parses a trusted local file and captures its local resource references.
     *
     * @param filePath path to the .ipes file
     * @return parsed circuit model
     * @throws IOException if the file cannot be read
     * @throws CircuitParseException if the file format is invalid
     */
    public CircuitModel parse(String filePath) throws IOException, CircuitParseException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("Circuit file not found: " + filePath);
        }

        return parse(new FileInputStream(file), filePath, ResourceAccess.LOCAL_FILES);
    }

    /**
     * Parses supplied circuit data. Resource references cannot read local files.
     *
     * @param reader the reader containing circuit data
     * @param sourceName name/path for error messages
     * @return parsed circuit model
     * @throws IOException if reading fails
     * @throws CircuitParseException if the format is invalid
     */
    public CircuitModel parse(BufferedReader reader, String sourceName) throws IOException, CircuitParseException {
        return parse(reader, sourceName, ResourceAccess.SUPPLIED_BYTES_ONLY);
    }

    private CircuitModel parse(BufferedReader reader, String sourceName, ResourceAccess resourceAccess)
            throws IOException, CircuitParseException {
        List<String> lineList = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            lineList.add(line);
        }
        String[] lines = lineList.toArray(new String[0]);
        return parseLines(lines, sourceName, resourceAccess);
    }

    /**
     * Parses supplied circuit data. The source name is diagnostic provenance only.
     *
     * @param inputStream the input stream containing circuit data
     * @param sourceName name/path for error messages
     * @return parsed circuit model
     * @throws IOException if reading fails
     * @throws CircuitParseException if the format is invalid
     */
    public CircuitModel parse(InputStream inputStream, String sourceName) throws IOException, CircuitParseException {
        return parse(inputStream, sourceName, ResourceAccess.SUPPLIED_BYTES_ONLY);
    }

    private CircuitModel parse(InputStream inputStream, String sourceName, ResourceAccess resourceAccess)
            throws IOException, CircuitParseException {
        try (BufferedInputStream buffered = new BufferedInputStream(inputStream)) {
            buffered.mark(2);
            int first = buffered.read();
            int second = buffered.read();
            buffered.reset();
            InputStream decoded = first == 0x1f && second == 0x8b
                    ? new GZIPInputStream(buffered) : buffered;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(decoded, StandardCharsets.UTF_8))) {
                return parse(reader, sourceName, resourceAccess);
            }
        }
    }

    /**
     * Parses the array of lines into a CircuitModel.
     */
    private CircuitModel parseLines(String[] lines, String filePath, ResourceAccess resourceAccess) throws CircuitParseException {
        CircuitModel model = new CircuitModel();
        model.setFilePath(filePath);

        // Build token map for fast lookup
        String[] simulationLines = inspectSimulationStructure(maskOpaqueContent(lines), model);
        Map<String, Integer> tokenMap = buildTokenMap(simulationLines);
        try {
            if (tokenMap.containsKey(AcMetadataCodec.SETTINGS)) {
                model.setAcSettings(AcMetadataCodec.decodeSettings(savedSettingValue(lines, tokenMap, AcMetadataCodec.SETTINGS)));
            }
            if (tokenMap.containsKey(HierarchyMetadataCodec.DOCUMENT)) {
                model.setHierarchy(HierarchyMetadataCodec.decodeDocument(
                        savedSettingValue(lines, tokenMap, HierarchyMetadataCodec.DOCUMENT)));
            }
            if (tokenMap.containsKey(AcMetadataCodec.PROVENANCE)) {
                model.setAcProvenance(AcMetadataCodec.decodeProvenance(savedSettingValue(lines, tokenMap, AcMetadataCodec.PROVENANCE)));
            }
        } catch (IllegalArgumentException error) {
            throw new CircuitParseException(filePath + ": " + error.getMessage(), error);
        }

        // Parse simulation parameters
        parseSimulationParameters(model, lines, tokenMap);

        // Parse display settings
        parseDisplaySettings(model, lines, tokenMap);

        // Parse file metadata
        parseFileMetadata(model, lines, tokenMap);

        // Parse optimizer parameters
        parseOptimizerParameters(model, lines, tokenMap);

        // Parse scripting blocks
        parseScripterBlocks(model, lines, tokenMap);

        // Parse signal names
        parseSignalNames(model, lines, tokenMap);

        // Parse circuit components from special blocks
        parseCircuitComponents(simulationLines, model);
        parseConnections(simulationLines, model, filePath);

        // Parse control components from special blocks
        parseControlComponents(simulationLines, model);
        parseThermalComponents(simulationLines, model);
        CircuitDescriptionReader.retain(model, lines, simulationLines);
        HierarchyIndex hierarchy = null;
        try {
            hierarchy = new HierarchyIndex(model);
        } catch (IllegalArgumentException error) {
            throw new CircuitParseException(filePath + ": " + error.getMessage(), error);
        }
        inspectUnrecognizedSimulationBlocks(simulationLines, model, hierarchy);
        CircuitResourceReader.retain(model, lines, simulationLines, resourceAccess);

        // Validate pre-simulation time step
        if (model.getPreSimulationTimeStep() <= 0) {
            model.setPreSimulationTimeStep(model.getTimeStep());
        }

        return model;
    }

    /**
     * Builds a map from token names to line numbers.
     */
    private Map<String, Integer> buildTokenMap(String[] lines) {
        Map<String, Integer> map = new LinkedHashMap<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                continue;
            }

            int tokenStart = 0;
            while (tokenStart < line.length() && Character.isWhitespace(line.charAt(tokenStart))) {
                tokenStart++;
            }
            if (tokenStart >= line.length()) {
                continue;
            }

            char firstChar = line.charAt(tokenStart);
            if (Character.isDigit(firstChar)) {
                continue;
            }

            int tokenEnd = tokenStart;
            while (tokenEnd < line.length() && !Character.isWhitespace(line.charAt(tokenEnd))) {
                tokenEnd++;
            }

            String token = line.substring(tokenStart, tokenEnd);
            if (!map.containsKey(token)) {
                map.put(token, i);
            }
        }

        return map;
    }

    /**
     * Parses simulation-related parameters.
     */
    private void parseSimulationParameters(CircuitModel model, String[] lines,
                                           Map<String, Integer> tokenMap) throws CircuitParseException {
        if (tokenMap.containsKey("tDURATION")) {
            model.setSimulationDuration(readSavedTiming(model, lines, tokenMap, "tDURATION", "duration", 0.02));
        }

        if (tokenMap.containsKey("dt")) {
            model.setTimeStep(readSavedTiming(model, lines, tokenMap, "dt", "timeStep", 1e-6));
        }

        if (tokenMap.containsKey("tPAUSE")) {
            model.setPauseTime(readDouble(lines, tokenMap, "tPAUSE", -1));
        }

        if (tokenMap.containsKey("T_pre")) {
            model.setPreSimulationTime(readDouble(lines, tokenMap, "T_pre", -1));
        }

        if (tokenMap.containsKey("dt_pre")) {
            model.setPreSimulationTimeStep(readDouble(lines, tokenMap, "dt_pre", 0));
        }

        if (tokenMap.containsKey("solverType")) {
            SolverType solver = null;
            try {
                int index = Integer.parseInt(savedSettingValue(lines, tokenMap, "solverType"));
                solver = Arrays.stream(SolverType.values()).filter(value -> value.getOldGeckoIndex() == index)
                        .findFirst().orElse(null);
            } catch (NumberFormatException error) {
                // Keep the inspection default and retain the invalid saved input.
            }
            if (solver == null) addInvalidSavedSetting(model, tokenMap, "solverType", "solver");
            else model.setSolverType(solver);
        }
    }

    /**
     * Parses display settings (preserved for compatibility).
     */
    private void parseDisplaySettings(CircuitModel model, String[] lines,
                                      Map<String, Integer> tokenMap) {
        if (tokenMap.containsKey("dpix")) {
            model.setDisplayPixels(readInt(lines, tokenMap, "dpix", 16));
        }

        if (tokenMap.containsKey("fontSize")) {
            model.setFontSize(readInt(lines, tokenMap, "fontSize", 12));
        }

        if (tokenMap.containsKey("fontTyp")) {
            String fontLine = lines[tokenMap.get("fontTyp")];
            String fontType = fontLine.substring("fontTyp ".length()).trim();
            model.setFontType(fontType);
        }

        if (tokenMap.containsKey("fensterWidth")) {
            model.setWindowWidth(readInt(lines, tokenMap, "fensterWidth", -1));
        }

        if (tokenMap.containsKey("fensterHeight")) {
            model.setWindowHeight(readInt(lines, tokenMap, "fensterHeight", -1));
        }
    }

    /**
     * Parses file metadata.
     */
    private void parseFileMetadata(CircuitModel model, String[] lines,
                                   Map<String, Integer> tokenMap) {
        if (tokenMap.containsKey("FileVersion")) {
            model.setFileVersion(readInt(lines, tokenMap, "FileVersion", -1));
        }

        if (tokenMap.containsKey("UniqueFileId")) {
            model.setUniqueFileId(readInt(lines, tokenMap, "UniqueFileId", 0));
        }

        if (tokenMap.containsKey("DtStor")) {
            String dateLine = lines[tokenMap.get("DtStor")];
            String[] parts = dateLine.split("\\s+");
            if (parts.length >= 2) {
                model.setCreationDate(parts[1]);
            }
        }
    }

    /**
     * Parses optimizer parameters.
     */
    private void parseOptimizerParameters(CircuitModel model, String[] lines,
                                          Map<String, Integer> tokenMap) {
        if (tokenMap.containsKey("optimizerName[]")) {
            List<String> names = readStringArray(lines, tokenMap, "optimizerName[]");
            List<Double> values = readDoubleArray(lines, tokenMap, "optimizerValue[]");

            int nameOffset = 0;
            // Keep backward compatibility with mixed formats:
            // names often contain a leading slash slot while space-separated values do not.
            if (names.size() == values.size() + 1 && !names.isEmpty() && names.get(0).isEmpty()) {
                nameOffset = 1;
            }

            int count = Math.min(names.size() - nameOffset, values.size());
            for (int i = 0; i < count; i++) {
                String name = names.get(i + nameOffset);
                Double value = values.get(i);
                if (!name.isEmpty() && !name.equals(NIX) && value != null && !value.isNaN()) {
                    model.setOptimizerParameter(name, value);
                }
            }
        }
    }

    /**
     * Parses scripting code blocks.
     */
    private void parseScripterBlocks(CircuitModel model, String[] lines,
                                     Map<String, Integer> tokenMap) {
        model.setScripterCode(readBlockContent(lines, tokenMap, "<scripterCode>", "<\\scripterCode>"));
        model.setScripterImports(readBlockContent(lines, tokenMap, "<scripterImports>", "<\\scripterImports>"));
        model.setScripterDeclarations(readBlockContent(lines, tokenMap,
                "<scripterDeclarations>", "<\\scripterDeclarations>"));
    }

    /**
     * Parses data container signal names.
     */
    private void parseSignalNames(CircuitModel model, String[] lines,
                                  Map<String, Integer> tokenMap) {
        if (tokenMap.containsKey("dataContainerSignals[]")) {
            List<String> signals = readStringArray(lines, tokenMap, "dataContainerSignals[]");
            model.setDataContainerSignals(signals.toArray(new String[0]));
        }
    }

    /**
     * Parses all <ElementLK> component blocks from the circuit file.
     * Each block contains: typ (component type), parameter[] (values),
     * idStringDialog (name), x, y, labelAnfangsKnoten[], labelEndKnoten[].
     *
     * @param lines the raw circuit file lines
     * @param model the circuit model to populate with components
     */
    private void parseCircuitComponents(String[] lines, CircuitModel model) throws CircuitParseException {
        TokenMap tokenMap = new TokenMap(lines, true);

        TokenMap elementBlock;
        while ((elementBlock = tokenMap.getSpecialBlockTokenMap("e")) != null) {
            try {
                int type = elementBlock.readDataLine("typ", -1);
                if (type < 0) {
                    addComponentIssue(model, elementBlock, "LK", "The component type is missing or invalid.");
                    continue;
                }

                String name = elementBlock.readDataLine("idStringDialog", "");
                int x = elementBlock.readDataLine("x", 0);
                int y = elementBlock.readDataLine("y", 0);
                int orientation = elementBlock.readDataLine("orientierung", 0);

                // Read parameter array (component values: R, L, C, etc.)
                // Real .ipes files use space-separated values; test/legacy data may use slash format
                List<Double> paramsList = readComponentParameters(elementBlock);
                double[] params = paramsList.stream()
                        .mapToDouble(v -> v != null ? v : Double.NaN)
                        .toArray();

                // Read terminal node labels for connectivity
                String[] xLabels = elementBlock.readDataLine("labelAnfangsKnoten[]", new String[0]);
                String[] yLabels = elementBlock.readDataLine("labelEndKnoten[]", new String[0]);

                // Build ComponentData
                CircuitModel.ComponentData comp = new CircuitModel.ComponentData(type, name, x, y, orientation);

                retainImportEvidence(comp, elementBlock, params, model);
                if (elementBlock.containsToken(AcMetadataCodec.COMPONENT)) {
                    comp.setAcComponent(AcMetadataCodec.decodeComponent(
                            elementBlock.readDataLine(AcMetadataCodec.COMPONENT, "")));
                }

                // Store numeric parameters by index
                for (int i = 0; i < params.length; i++) {
                    comp.setParameter("param" + i, params[i]);
                }

                // Also store primary value with a semantic key based on type
                if (params.length > 0) {
                    comp.setParameter(resolveParameterKey(type), params[0]);
                }

                comp.setTerminalXLabels(filterNix(xLabels));
                comp.setTerminalYLabels(filterNix(yLabels));
                comp.setParameterStrings(filterNixStrings(
                        elementBlock.readDataLine("parameterString[]", new String[0])));
                if (elementBlock.containsToken("uniqueObjectIdentifier")) {
                    String value = elementBlock.getLineString("uniqueObjectIdentifier", "")
                            .substring("uniqueObjectIdentifier".length()).trim();
                    comp.setParameter("uniqueObjectIdentifier", Long.parseLong(value));
                }
                if (type == 9 && elementBlock.containsToken("coupledReferenceID[]")) {
                    String[] references = elementBlock.getLineString("coupledReferenceID[]", "")
                            .substring("coupledReferenceID[]".length()).trim().split("\\s+");
                    if (references.length != 2) {
                        throw new IllegalArgumentException("A magnetic coupling requires exactly two winding references");
                    }
                    comp.setParameter("coupledReferenceID0", Long.parseLong(references[0]));
                    comp.setParameter("coupledReferenceID1", Long.parseLong(references[1]));
                }

                model.addCircuitComponent(comp);
            } catch (Exception e) {
                throw new CircuitParseException(model.getFilePath() + ": invalid electrical component: "
                        + e.getMessage(), e);
            }
        }
    }

    private static final Map<String, String> SIMULATION_MARKERS = Map.of(
            "e", "ElementLK", "c", "ElementCONTROL", "eTH", "ElementTHERM", "sp", "ElementSPECIAL",
            "verbindungLK", "Verbindung", "verbindungCONTROL", "Verbindung", "verbindungTHERM", "Verbindung");

    private String[] maskOpaqueContent(String[] lines) {
        String[] result = lines.clone();
        Set<String> tags = Set.of("scripterCode", "scripterImports", "scripterDeclarations",
                "extraScriptSourceFiles", "sourceCode", "staticCode", "importCode", "staticVariables",
                "extraSourceFiles", "GeckoFileManager", "GeckoFile");
        String closing = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (closing != null) {
                if (line.equals(closing)) {
                    closing = null;
                } else {
                    result[i] = "";
                }
            } else if (line.startsWith("<") && line.endsWith(">")
                    && tags.contains(line.substring(1, line.length() - 1))) {
                closing = "<\\" + line.substring(1);
            }
        }
        return result;
    }

    private String[] inspectSimulationStructure(String[] lines, CircuitModel model) {
        String[] result = lines.clone();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            String marker = line.split("\\s+", 2)[0];
            String markedTag = SIMULATION_MARKERS.get(marker);
            if (markedTag != null && (i + 1 == lines.length
                    || !lines[i + 1].trim().equals("<" + markedTag + ">"))) {
                addStructureIssue(model, markedTag, marker, i, "The simulation marker has no opening tag.");
                result[i] = "";
            }
            boolean element = line.startsWith("<Element") && line.endsWith(">");
            boolean wire = line.equals("<Verbindung>");
            if (element || wire) {
                String tag = line.substring(1, line.length() - 1);
                String previous = i > 0 ? lines[i - 1].trim().split("\\s+", 2)[0] : "";
                boolean knownTag = SIMULATION_MARKERS.containsValue(tag);
                boolean hasMarker = tag.equals(SIMULATION_MARKERS.get(previous));
                int start = hasMarker ? i - 1 : i;
                if (knownTag && !hasMarker) {
                    addStructureIssue(model, tag, previous, i, "The simulation block has no matching marker.");
                }
                String closing = "<\\" + tag + ">";
                int end = i + 1;
                while (end < lines.length && !lines[end].trim().equals(closing)) {
                    String next = lines[end].trim();
                    if (!tag.equals("ElementSPECIAL") && knownTag
                            && (next.startsWith("<Element") || SIMULATION_MARKERS.containsKey(next))) {
                        break;
                    }
                    end++;
                }
                boolean closed = end < lines.length && lines[end].trim().equals(closing);
                if (!closed) {
                    addStructureIssue(model, tag, previous, i, "The simulation block has no closing tag.");
                }
                if (!closed || (knownTag && !hasMarker)) {
                    Arrays.fill(result, start, closed ? end + 1 : end, "");
                }
                i = closed ? end : end - 1;
            } else if ((line.startsWith("<\\Element") && line.endsWith(">"))
                    || line.equals("<\\Verbindung>")) {
                String tag = line.substring(2, line.length() - 1);
                addStructureIssue(model, tag, "", i, "The closing tag has no simulation block.");
                result[i] = "";
            }
        }
        return result;
    }

    private void addStructureIssue(CircuitModel model, String tag, String marker, int line, String message) {
        String domain = tag.startsWith("Element") ? tag.substring("Element".length())
                : marker.startsWith("verbindung") ? marker.substring("verbindung".length()) : "";
        if (domain.equals("THERM")) {
            domain = "THERMAL";
        }
        model.getImportIssues().add(new ImportIssue("MALFORMED_SIMULATION_BLOCK", domain, "", message,
                model.getFilePath() + ": line " + (line + 1) + " " + tag, false));
    }
    private void inspectUnrecognizedSimulationBlocks(String[] lines, CircuitModel model, HierarchyIndex hierarchy) {
        for (int start = 0; start < lines.length; start++) {
            String opening = lines[start].trim();
            if (!opening.startsWith("<Element") || !opening.endsWith(">")) {
                continue;
            }
            String tag = opening.substring(1, opening.length() - 1);
            if (Set.of("ElementLK", "ElementCONTROL", "ElementTHERM").contains(tag)) {
                continue;
            }
            String closing = "<\\" + tag + ">";
            int end = start + 1;
            while (end < lines.length && !lines[end].trim().equals(closing)) {
                end++;
            }
            TokenMap block = new TokenMap(Arrays.copyOfRange(lines, start + 1, end));
            String name = block.containsToken("idStringDialog") ? block.readDataLine("idStringDialog", "") : "";
            int type = block.readDataLine("typ", -1);
            if (hierarchy != null && tag.equals("ElementSPECIAL") && type == 27
                    && block.containsToken(HierarchyMetadataCodec.OCCURRENCE)
                    && hierarchy.sheet(block.readDataLine("uniqueObjectIdentifier", 0L)) != null) continue;
            var issue = SimulationBlockClassification.savedBlockIssue(tag, type, name,
                    model.getFilePath() + ": line " + (start + 1) + " " + tag);
            if (issue != null) model.getImportIssues().add(issue);
        }
    }
    private void parseThermalComponents(String[] lines, CircuitModel model) {
        TokenMap tokens = new TokenMap(lines, true);
        TokenMap block;
        while ((block = tokens.getSpecialBlockTokenMap("eTH")) != null) {
            try {
                int type = block.readDataLine("typ", -1);
                if (type < 0) {
                    addComponentIssue(model, block, "THERMAL", "The component type is missing or invalid.");
                    continue;
                }
                String name = block.readDataLine("idStringDialog", "");
                CircuitModel.ComponentData component = new CircuitModel.ComponentData(type, name,
                        block.readDataLine("x", 0), block.readDataLine("y", 0),
                        block.readDataLine("orientierung", 0));
                double[] parameters = readComponentParameters(block).stream()
                        .mapToDouble(value -> value != null ? value : Double.NaN).toArray();
                retainImportEvidence(component, block, parameters, model);
                for (int i = 0; i < parameters.length; i++) {
                    component.setParameter("param" + i, parameters[i]);
                }
                component.setTerminalXLabels(filterNix(block.readDataLine("labelAnfangsKnoten[]", new String[0])));
                component.setTerminalYLabels(filterNix(block.readDataLine("labelEndKnoten[]", new String[0])));
                component.setParameterStrings(filterNixStrings(block.readDataLine("parameterString[]", new String[0])));
                if (block.containsToken("uniqueObjectIdentifier")) {
                    component.setParameter("uniqueObjectIdentifier", Long.parseLong(
                            component.getImportTokens().get("uniqueObjectIdentifier")));
                }
                model.addThermalComponent(component);
                model.getImportIssues().add(new ImportIssue("UNSUPPORTED_DOMAIN", "THERMAL", name,
                        "Saved thermal elements cannot run in the headless engine.",
                        component.getSourceLocation(), true));
            } catch (RuntimeException error) {
                addComponentIssue(model, block, "THERMAL", "The component block is invalid: " + error.getMessage());
            }
        }
    }
    private void addComponentIssue(CircuitModel model, TokenMap block, String domain, String message) {
        String name = block.containsToken("idStringDialog") ? block.readDataLine("idStringDialog", "") : "";
        model.getImportIssues().add(new ImportIssue("MALFORMED_COMPONENT", domain, name, message,
                model.getFilePath() + ": " + domain + " component " + name, false));
    }

    private void retainImportEvidence(CircuitModel.ComponentData component, TokenMap block,
                                      double[] parameters, CircuitModel model) {
        component.setParsedInput(true);
        component.setNumericParameters(parameters);
        component.setImportedParameterCount(parameters.length);
        component.setSourceLocation(model.getFilePath() + ": component " + component.getName());
        for (String line : block.getLines()) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("<")) {
                continue;
            }
            String[] pair = trimmed.split("\\s+", 2);
            component.getImportTokens().put(pair[0], pair.length == 2 ? pair[1].trim() : "");
        }
    }

    private void parseConnections(String[] lines, CircuitModel model, String sourceName)
            throws CircuitParseException {
        TokenMap tokens = new TokenMap(lines, true);
        String[] domains = {"LK", "CONTROL", "THERM"};
        for (String domain : domains) {
            TokenMap block;
            while ((block = tokens.getSpecialBlockTokenMap("verbindung" + domain)) != null) {
                if (!block.readDataLine("enabled", true)) {
                    continue;
                }
                try {
                    int[] xs = readWireCoordinates(block, "x[]");
                    int[] ys = readWireCoordinates(block, "y[]");
                    if (xs.length < 2 || xs.length != ys.length) {
                        throw new IllegalArgumentException("coordinate arrays must have equal lengths of at least two");
                    }
                    int[][] points = new int[xs.length][2];
                    for (int i = 0; i < xs.length; i++) {
                        points[i][0] = xs[i];
                        points[i][1] = ys[i];
                    }
                    String label = block.readDataLine("label", "").trim();
                    if (label.equals(NIX)) {
                        label = "";
                    }
                    var connection = new CircuitModel.ConnectionData(
                            domain.equals("THERM") ? "THERMAL" : domain, points, label);
                    connection.setSavedBlock(SavedCircuitBlock.parse("Verbindung", block.getLines()));
                    model.addConnection(connection);
                } catch (IllegalArgumentException error) {
                    throw new CircuitParseException(sourceName + ": invalid " + domain
                            + " wire: " + error.getMessage(), error);
                }
            }
        }
    }

    private int[] readWireCoordinates(TokenMap block, String key) {
        String line = block.getLineString(key, "");
        if (line.isEmpty()) {
            throw new IllegalArgumentException("missing " + key);
        }
        String value = line.substring(key.length()).trim();
        if (value.isEmpty()) {
            return new int[0];
        }
        String[] parts = value.split("\\s+");
        int[] coordinates = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            coordinates[i] = Integer.parseInt(parts[i]);
        }
        return coordinates;
    }

    /**
     * Parses all {@code <ElementCONTROL>} blocks from the circuit file.
     * Mirrors {@link #parseCircuitComponents} but uses TokenMap special token {@code "c"}.
     *
     * @param lines the raw circuit file lines
     * @param model the circuit model to populate with control components
     */
    private void parseControlComponents(String[] lines, CircuitModel model) {
        TokenMap tokenMap = new TokenMap(lines, true);

        TokenMap elementBlock;
        while ((elementBlock = tokenMap.getSpecialBlockTokenMap("c")) != null) {
            try {
                // typ field: for SIGNAL sources this is the sub-type (e.g. 404)
                // but the real control type comes from a mapping
                int rawTyp = elementBlock.readDataLine("typ", -1);
                if (rawTyp < 0) {
                    addComponentIssue(model, elementBlock, "CONTROL", "The component type is missing or invalid.");
                    continue;
                }

                String name = elementBlock.readDataLine("idStringDialog", "");
                int x = elementBlock.readDataLine("x", 0);
                int y = elementBlock.readDataLine("y", 0);
                int orientation = elementBlock.readDataLine("orientierung", 0);

                // Read parameter array (space-separated in real .ipes files)
                List<Double> paramsList = readComponentParameters(elementBlock);
                double[] params = paramsList.stream()
                        .mapToDouble(v -> v != null ? v : Double.NaN)
                        .toArray();

                // Read parameterString[] — slash-separated string values
                // Format: "parameterString[] /value1/value2/value3"
                String[] paramStrings = elementBlock.readDataLine("parameterString[]", new String[0]);

                // For SIGNAL sources, parameter[0] contains the sub-type (e.g. 404)
                // The actual control type is determined by the typ field
                CircuitModel.ComponentData comp = new CircuitModel.ComponentData(rawTyp, name, x, y, orientation);

                retainImportEvidence(comp, elementBlock, params, model);

                // Store numeric parameters by index
                for (int i = 0; i < params.length; i++) {
                    comp.setParameter("param" + i, params[i]);
                }
                comp.setNumericParameters(params);

                // Store parameterString values (critical for VOLT/AMP probe coupling)
                comp.setParameterStrings(filterNixStrings(paramStrings));

                comp.setTerminalXLabels(readControlPortLabels(elementBlock, "labelAnfangsKnoten[]"));
                comp.setTerminalYLabels(readControlPortLabels(elementBlock, "labelEndKnoten[]"));
                if (rawTyp == 22 || rawTyp == 23) {
                    int savedInputWidth = comp.getTerminalXLabels().length;
                    comp.setParameter("externalWidth", rawTyp == 23
                            ? elementBlock.readDataLine("tn", comp.getTerminalYLabels().length)
                            : savedInputWidth > 0 ? savedInputWidth
                                    : elementBlock.readDataLine("anzXIN", savedInputWidth));
                    if (rawTyp == 22) {
                        int labeledInputCount = (int) Arrays.stream(comp.getTerminalXLabels())
                                .filter(label -> label != null && !label.isBlank()).count();
                        comp.setParameter("activeInputWidth",
                                elementBlock.readDataLine("anzXIN", labeledInputCount));
                    }
                    comp.setParameter("externalOrder", elementBlock.readDataLine("torder", 0));
                }

                model.addControlComponent(comp);
            } catch (Exception e) {
                addComponentIssue(model, elementBlock, "CONTROL", "The component block is invalid: " + e.getMessage());
            }
        }
    }

    /** Preserves the saved indices of fixed CONTROL ports. */
    private static String[] readControlPortLabels(TokenMap block, String token) {
        String line = block.getLineString(token, "");
        int separator = line.indexOf(SEPARATOR_ASCII_STRINGARRAY);
        return separator < 0 ? new String[0]
                : filterNixStrings(line.substring(separator + 1).split(SEPARATOR_ASCII_STRINGARRAY, -1));
    }

    /** Replaces saved placeholder strings with empty strings. */
    private static String[] filterNixStrings(String[] values) {
        if (values == null) {
            return new String[0];
        }
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null && !values[i].equals("NIX_NIX_NIX")) {
                result[i] = values[i].trim();
            } else {
                result[i] = "";
            }
        }
        return result;
    }

    /**
     * Reads a double array from a TokenMap, handling "/" separator format used in .ipes files.
     */
    private List<Double> readDoubleArrayFromTokenMap(TokenMap tokenMap, String identifier) {
        List<Double> result = new ArrayList<>();
        try {
            String line = tokenMap.getLineString(identifier, "");
            if (line == null || line.isEmpty()) {
                return result;
            }

            // Format: "identifier /value1/value2/value3"
            int arrayStart = line.indexOf("[] ");
            if (arrayStart < 0) {
                return result;
            }

            String arrayPart = line.substring(arrayStart + 3).trim();
            if (arrayPart.equals("null")) {
                return result;
            }

            if (arrayPart.startsWith(SEPARATOR_ASCII_STRINGARRAY)) {
                arrayPart = arrayPart.substring(1);
            }

            // Split by "/" separator
            String[] parts = arrayPart.split(SEPARATOR_ASCII_STRINGARRAY, -1);
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    result.add(Double.NaN);
                } else {
                    try {
                        result.add(Double.parseDouble(trimmed));
                    } catch (NumberFormatException e) {
                        result.add(Double.NaN);
                    }
                }
            }
        } catch (Exception e) {
            // Return empty list if error
        }
        return result;
    }

    /**
     * Reads the parameter[] array from a component block, handling both formats:
     * - Space-separated (real .ipes files): {@code parameter[] 0.0024 0.0 0.6886}
     * - Slash-prefixed (legacy/test format): {@code parameter[] /100.0}
     */
    private List<Double> readComponentParameters(TokenMap elementBlock) {
        // Try space-separated first (real .ipes format via StringTokenizer)
        List<Double> result = elementBlock.readDataLineDoubleArray("parameter[]");
        if (result != null && !result.isEmpty()) {
            return result;
        }
        // Fall back to slash-separated format (test/legacy synthetic data)
        return readDoubleArrayFromTokenMap(elementBlock, "parameter[]");
    }

    /**
     * Returns the semantic parameter key for the primary value of a component type.
     */
    private static String resolveParameterKey(int type) {
        return switch (type) {
            case 1 -> "resistance";     // LK_R
            case 2 -> "inductance";     // LK_L
            case 3 -> "capacitance";    // LK_C
            case 4 -> "amplitude";      // LK_U voltage source
            case 5 -> "amplitude";      // LK_I current source
            case 6 -> "forwardVoltage"; // LK_D diode
            case 7 -> "resistance";     // LK_S ideal switch (on-resistance)
            default -> "value";
        };
    }

    /**
     * Filters out "NIX_NIX_NIX" placeholder labels, returning real label names only.
     */
    private static String[] filterNix(String[] labels) {
        if (labels == null) {
            return new String[0];
        }
        List<String> filtered = new ArrayList<>();
        for (String label : labels) {
            if (label != null && !label.equals("NIX_NIX_NIX") && !label.isBlank()) {
                filtered.add(label.trim());
            }
        }
        return filtered.toArray(new String[0]);
    }

    private double readSavedTiming(CircuitModel model, String[] lines, Map<String, Integer> tokenMap,
                                   String token, String path, double fallback) {
        try {
            double value = Double.parseDouble(savedSettingValue(lines, tokenMap, token));
            if (Double.isFinite(value)) return value;
        } catch (NumberFormatException error) {
            // Keep a finite inspection value and retain the invalid saved input.
        }
        addInvalidSavedSetting(model, tokenMap, token, path);
        return fallback;
    }

    private String savedSettingValue(String[] lines, Map<String, Integer> tokenMap, String token) {
        String[] parts = lines[tokenMap.get(token)].trim().split("\\s+");
        return parts.length > 1 ? parts[1] : "";
    }

    private void addInvalidSavedSetting(CircuitModel model, Map<String, Integer> tokenMap, String token, String path) {
        model.addSavedTransientSettingIssue(new ImportIssue("INVALID_SETTING", null, null,
                "The saved setting is invalid.", model.getFilePath() + ": line " + (tokenMap.get(token) + 1) + " " + token,
                false, path));
    }

    // ==================== Utility methods ====================

    private double readDouble(String[] lines, Map<String, Integer> tokenMap,
                              String key, double defaultValue) {
        try {
            if (!tokenMap.containsKey(key)) {
                return defaultValue;
            }
            String line = lines[tokenMap.get(key)];
            String[] parts = line.split("\\s+");
            if (parts.length >= 2) {
                return Double.parseDouble(parts[1]);
            }
        } catch (NumberFormatException e) {
            // Return default
        }
        return defaultValue;
    }

    private int readInt(String[] lines, Map<String, Integer> tokenMap,
                        String key, int defaultValue) {
        try {
            if (!tokenMap.containsKey(key)) {
                return defaultValue;
            }
            String line = lines[tokenMap.get(key)];
            String[] parts = line.split("\\s+");
            if (parts.length >= 2) {
                return Integer.parseInt(parts[1]);
            }
        } catch (NumberFormatException e) {
            // Return default
        }
        return defaultValue;
    }

    private List<String> readStringArray(String[] lines, Map<String, Integer> tokenMap, String key) {
        List<String> result = new ArrayList<>();
        if (!tokenMap.containsKey(key)) {
            return result;
        }

        String line = lines[tokenMap.get(key)];
        // Format: "key[] /value1/value2/value3"
        int startIndex = line.indexOf("[] ");
        if (startIndex < 0) {
            return result;
        }

        String arrayPart = line.substring(startIndex + 3).trim();
        if (arrayPart.equals("null")) {
            return result;
        }

        String[] parts = arrayPart.split(SEPARATOR_ASCII_STRINGARRAY, -1);
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.equals(NIX)) {
                result.add("");
            } else {
                result.add(trimmed);
            }
        }

        return result;
    }

    private List<Double> readDoubleArray(String[] lines, Map<String, Integer> tokenMap, String key) {
        List<Double> result = new ArrayList<>();
        if (!tokenMap.containsKey(key)) {
            return result;
        }

        String line = lines[tokenMap.get(key)];
        int startIndex = line.indexOf("[] ");
        if (startIndex < 0) {
            return result;
        }

        String arrayPart = line.substring(startIndex + 3).trim();
        if (arrayPart.equals("null")) {
            return result;
        }

        String[] parts;
        if (arrayPart.contains(SEPARATOR_ASCII_STRINGARRAY)) {
            // Preserve empty slots for index alignment with corresponding string arrays.
            parts = arrayPart.split(SEPARATOR_ASCII_STRINGARRAY, -1);
        } else {
            // Legacy variant with plain whitespace separation.
            parts = arrayPart.split("\\s+");
        }

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                result.add(Double.NaN);
                continue;
            }
            try {
                result.add(Double.parseDouble(trimmed));
            } catch (NumberFormatException e) {
                // Preserve positional alignment with optimizerName[].
                result.add(Double.NaN);
            }
        }

        return result;
    }

    private String readBlockContent(String[] lines, Map<String, Integer> tokenMap,
                                    String startTag, String endTag) {
        if (!tokenMap.containsKey(startTag)) {
            return "";
        }

        int startLine = tokenMap.get(startTag);
        StringBuilder content = new StringBuilder();

        for (int i = startLine + 1; i < lines.length; i++) {
            if (lines[i].startsWith(endTag)) {
                break;
            }
            if (content.length() > 0) {
                content.append("\n");
            }
            content.append(lines[i]);
        }

        return content.toString().trim();
    }

    /**
     * Exception thrown when parsing fails.
     */
    public static class CircuitParseException extends Exception {
        public CircuitParseException(String message) {
            super(message);
        }

        public CircuitParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
