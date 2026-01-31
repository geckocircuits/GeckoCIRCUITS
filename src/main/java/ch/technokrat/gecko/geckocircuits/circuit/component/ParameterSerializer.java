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
package ch.technokrat.gecko.geckocircuits.circuit.component;

import java.util.*;
import java.util.Arrays;

/**
 * Handles serialization and deserialization of component parameters.
 * Extracted from AbstractBlockInterface to separate serialization concerns.
 * 
 * <p>Supports ASCII import/export format used in .ipes files:
 * <pre>
 * parameterCount
 * param1 param2 param3 ... paramN
 * nameOptCount
 * name1
 * name2
 * ...
 * </pre>
 * 
 * <p>Thread-safety: Not thread-safe. External synchronization required.
 * 
 * @author Sprint 3 refactoring
 */
public final class ParameterSerializer {
    
    /** Default parameter array size (matches legacy AbstractBlockInterface). */
    public static final int DEFAULT_ARRAY_SIZE = 40;
    
    /** Current format version for future compatibility. */
    private static final int FORMAT_VERSION = 1;
    
    private final int arraySize;
    
    /**
     * Creates a serializer with default array size (40).
     */
    public ParameterSerializer() {
        this(DEFAULT_ARRAY_SIZE);
    }
    
    /**
     * Creates a serializer with custom array size.
     * 
     * @param arraySize Maximum number of parameters
     */
    public ParameterSerializer(int arraySize) {
        if (arraySize <= 0) {
            throw new IllegalArgumentException("Array size must be positive: " + arraySize);
        }
        this.arraySize = arraySize;
    }
    
    /**
     * Exports parameters to ASCII string format.
     * Format: count followed by space-separated values.
     * 
     * @param values Parameter values array
     * @return ASCII representation
     */
    public String exportValuesToASCII(double[] values) {
        if (values == null) {
            return "0\n";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(values.length).append("\n");
        
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(formatDouble(values[i]));
        }
        sb.append("\n");
        
        return sb.toString();
    }
    
    /**
     * Exports parameter names to ASCII string format.
     * Format: count followed by one name per line.
     * 
     * @param names Parameter names array (may contain nulls)
     * @return ASCII representation
     */
    public String exportNamesToASCII(String[] names) {
        if (names == null) {
            return "0\n";
        }
        
        // Count non-null names from the end to find actual count
        int actualCount = names.length;
        while (actualCount > 0 && names[actualCount - 1] == null) {
            actualCount--;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(actualCount).append("\n");
        
        for (int i = 0; i < actualCount; i++) {
            sb.append(names[i] != null ? names[i] : "").append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Exports both values and names in combined format.
     * 
     * @param values Parameter values
     * @param names Parameter names (optimizer names)
     * @return Combined ASCII representation
     */
    public String exportToASCII(double[] values, String[] names) {
        return exportValuesToASCII(values) + exportNamesToASCII(names);
    }
    
    /**
     * Imports parameter values from tokenizer.
     * Expects: count followed by values.
     * 
     * @param tokenizer Input tokenizer
     * @return Parsed values array
     * @throws ParseException if format is invalid
     */
    public double[] importValuesFromASCII(TokenizerAdapter tokenizer) throws ParseException {
        int count = parseCount(tokenizer, "parameter count");
        
        if (count > arraySize) {
            throw new ParseException("Parameter count exceeds maximum: " + count + " > " + arraySize);
        }
        
        double[] values = new double[Math.max(count, arraySize)];
        
        for (int i = 0; i < count; i++) {
            values[i] = parseDouble(tokenizer, "parameter " + i);
        }
        
        return values;
    }
    
    /**
     * Imports parameter names from tokenizer.
     * Expects: count followed by one name per line.
     * 
     * @param tokenizer Input tokenizer
     * @return Parsed names array
     * @throws ParseException if format is invalid
     */
    public String[] importNamesFromASCII(TokenizerAdapter tokenizer) throws ParseException {
        int count = parseCount(tokenizer, "name count");
        
        if (count > arraySize) {
            throw new ParseException("Name count exceeds maximum: " + count + " > " + arraySize);
        }
        
        String[] names = new String[Math.max(count, arraySize)];
        
        for (int i = 0; i < count; i++) {
            String line = tokenizer.nextLine();
            if (line == null) {
                throw new ParseException("Unexpected end of input reading name " + i);
            }
            names[i] = line.isEmpty() ? null : line;
        }
        
        return names;
    }
    
    /**
     * Imports both values and names from tokenizer.
     * 
     * @param tokenizer Input tokenizer
     * @return Result containing values and names
     * @throws ParseException if format is invalid
     */
    public ImportResult importFromASCII(TokenizerAdapter tokenizer) throws ParseException {
        double[] values = importValuesFromASCII(tokenizer);
        String[] names = importNamesFromASCII(tokenizer);
        return new ImportResult(values, names);
    }
    
    /**
     * Creates a token-based adapter from lines.
     * 
     * @param lines Input lines
     * @return Tokenizer adapter
     */
    public TokenizerAdapter createTokenizer(List<String> lines) {
        return new ListTokenizer(lines);
    }
    
    /**
     * Creates a token-based adapter from string.
     * 
     * @param input Input string
     * @return Tokenizer adapter
     */
    public TokenizerAdapter createTokenizer(String input) {
        return new ListTokenizer(Arrays.asList(input.split("\n")));
    }
    
    // ===== Helper Methods =====
    
    private int parseCount(TokenizerAdapter tokenizer, String context) throws ParseException {
        String token = tokenizer.nextToken();
        if (token == null) {
            throw new ParseException("Unexpected end of input reading " + context);
        }
        
        try {
            int count = Integer.parseInt(token);
            if (count < 0) {
                throw new ParseException("Negative " + context + ": " + count);
            }
            return count;
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid " + context + ": " + token);
        }
    }
    
    private double parseDouble(TokenizerAdapter tokenizer, String context) throws ParseException {
        String token = tokenizer.nextToken();
        if (token == null) {
            throw new ParseException("Unexpected end of input reading " + context);
        }
        
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException e) {
            throw new ParseException("Invalid " + context + ": " + token);
        }
    }
    
    private String formatDouble(double value) {
        // Use scientific notation for very large/small values
        if (value != 0 && (Math.abs(value) >= 1e7 || Math.abs(value) < 1e-5)) {
            return String.format(Locale.US, "%.6E", value);
        }
        // Remove trailing zeros for cleaner output
        String formatted = String.format(Locale.US, "%.10f", value);
        formatted = formatted.replaceAll("(\\.\\d*?)0+$", "$1");
        formatted = formatted.replaceAll("\\.$", ".0");
        return formatted;
    }
    
    /**
     * Returns the array size used by this serializer.
     */
    public int getArraySize() {
        return arraySize;
    }
    
    // ===== Inner Classes =====
    
    /**
     * Adapter interface for different tokenizer implementations.
     */
    public interface TokenizerAdapter {
        /**
         * Gets the next token (whitespace-delimited).
         * @return Next token or null if exhausted
         */
        String nextToken();
        
        /**
         * Gets the next complete line.
         * @return Next line or null if exhausted
         */
        String nextLine();
        
        /**
         * Checks if more tokens are available.
         * @return true if tokens remain
         */
        boolean hasMore();
    }
    
    /**
     * Simple tokenizer based on list of lines.
     */
    public static class ListTokenizer implements TokenizerAdapter {
        private final List<String> lines;
        private int lineIndex = 0;
        private StringTokenizer currentTokenizer;
        
        public ListTokenizer(List<String> lines) {
            this.lines = new ArrayList<>(lines);
        }
        
        @Override
        public String nextToken() {
            while (currentTokenizer == null || !currentTokenizer.hasMoreTokens()) {
                if (lineIndex >= lines.size()) {
                    return null;
                }
                currentTokenizer = new StringTokenizer(lines.get(lineIndex++));
            }
            return currentTokenizer.nextToken();
        }
        
        @Override
        public String nextLine() {
            // If we have pending tokens on current line, skip to next
            currentTokenizer = null;
            
            if (lineIndex >= lines.size()) {
                return null;
            }
            return lines.get(lineIndex++);
        }
        
        @Override
        public boolean hasMore() {
            if (currentTokenizer != null && currentTokenizer.hasMoreTokens()) {
                return true;
            }
            return lineIndex < lines.size();
        }
    }
    
    /**
     * Result of import operation containing values and names.
     */
    public static class ImportResult {
        private final double[] values;
        private final String[] names;

        public ImportResult(double[] values, String[] names) {
            this.values = values != null ? Arrays.copyOf(values, values.length) : null;
            this.names = names != null ? Arrays.copyOf(names, names.length) : null;
        }

        public double[] getValues() {
            return values != null ? Arrays.copyOf(values, values.length) : null;
        }

        public String[] getNames() {
            return names != null ? Arrays.copyOf(names, names.length) : null;
        }

        public double getValue(int index) {
            return values[index];
        }

        public String getName(int index) {
            return names[index];
        }
    }
    
    /**
     * Exception thrown during parsing.
     */
    public static class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }
        
        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
