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
package ch.technokrat.gecko.core.io;

import ch.technokrat.gecko.core.allg.SolverType;

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

    private static final String NIX = "NIX_NIX_NIX";
    private static final String SEPARATOR_ASCII_STRINGARRAY = "/";

    /**
     * Parses a .ipes circuit file and returns the circuit model.
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

        String[] lines = readFileLines(file);
        return parseLines(lines, filePath);
    }

    /**
     * Parses circuit data from a BufferedReader.
     *
     * @param reader the reader containing circuit data
     * @param sourceName name/path for error messages
     * @return parsed circuit model
     * @throws IOException if reading fails
     * @throws CircuitParseException if the format is invalid
     */
    public CircuitModel parse(BufferedReader reader, String sourceName) throws IOException, CircuitParseException {
        List<String> lineList = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            lineList.add(line);
        }
        String[] lines = lineList.toArray(new String[0]);
        return parseLines(lines, sourceName);
    }

    /**
     * Parses circuit data from an InputStream.
     *
     * @param inputStream the input stream containing circuit data
     * @param sourceName name/path for error messages
     * @return parsed circuit model
     * @throws IOException if reading fails
     * @throws CircuitParseException if the format is invalid
     */
    public CircuitModel parse(InputStream inputStream, String sourceName) throws IOException, CircuitParseException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return parse(reader, sourceName);
        }
    }

    /**
     * Reads all lines from a .ipes file (handles gzip compression).
     */
    private String[] readFileLines(File file) throws IOException {
        try (InputStream fis = new FileInputStream(file)) {
            InputStream inputStream;

            // Check if file is gzip compressed
            if (isGzipCompressed(file)) {
                inputStream = new GZIPInputStream(fis);
            } else {
                inputStream = fis;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                return lines.toArray(new String[0]);
            }
        }
    }

    /**
     * Checks if a file is gzip compressed by examining magic bytes.
     */
    private boolean isGzipCompressed(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] signature = new byte[2];
            int read = fis.read(signature);
            if (read < 2) {
                return false;
            }
            // GZIP magic number: 0x1f 0x8b
            return (signature[0] == (byte) 0x1f) && (signature[1] == (byte) 0x8b);
        }
    }

    /**
     * Parses the array of lines into a CircuitModel.
     */
    private CircuitModel parseLines(String[] lines, String filePath) throws CircuitParseException {
        CircuitModel model = new CircuitModel();
        model.setFilePath(filePath);

        // Build token map for fast lookup
        Map<String, Integer> tokenMap = buildTokenMap(lines);

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

            char firstChar = line.charAt(0);
            if (Character.isDigit(firstChar) || Character.isWhitespace(firstChar)) {
                continue;
            }

            int spaceIndex = line.indexOf(' ');
            if (spaceIndex < 1) {
                spaceIndex = line.length();
            }

            String token = line.substring(0, spaceIndex);
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
            model.setSimulationDuration(readDouble(lines, tokenMap, "tDURATION", 0.02));
        }

        if (tokenMap.containsKey("dt")) {
            model.setTimeStep(readDouble(lines, tokenMap, "dt", 1e-6));
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
            int solverIndex = readInt(lines, tokenMap, "solverType", 0);
            model.setSolverType(SolverType.fromOldGeckoIndex(solverIndex));
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

            int count = Math.min(names.size(), values.size());
            for (int i = 0; i < count; i++) {
                String name = names.get(i);
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

        String[] parts = arrayPart.split(SEPARATOR_ASCII_STRINGARRAY);
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                if (trimmed.equals(NIX)) {
                    result.add("");
                } else {
                    result.add(trimmed);
                }
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

        // Accept legacy space-separated data and slash-separated variants.
        String[] parts = arrayPart.split("[/\\s]+");
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            try {
                result.add(Double.parseDouble(part));
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
