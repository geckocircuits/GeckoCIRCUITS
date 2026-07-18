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
package gecko.core.circuit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gecko.core.circuit.CircuitFileConstants;
import gecko.core.datacontainer.HiLoData;
import java.util.*;
/**
 *
 * @author andreas
 */
public final class TokenMap {
    private static final Logger LOGGER = LogManager.getLogger(TokenMap.class);


    private final Map<String, Integer> _map = new LinkedHashMap<String, Integer>();
    private final Map<String, Deque<BlockInfo>> _duplicateMap = new LinkedHashMap<String, Deque<BlockInfo>>();
    private final Map<String, Deque<BlockInfo>> _specialMap = new LinkedHashMap<String, Deque<BlockInfo>>();
    public final String[] asciiLines;
    private final Map<String, SpecialPair> _specialTokens = new LinkedHashMap<String, SpecialPair>();

    public TokenMap(final String[] ascii) {
        this(ascii, false);
    }

    public TokenMap(final String[] ascii, final boolean makeSpecialPairs) {
        asciiLines = ascii;
        if (makeSpecialPairs) {
            _specialTokens.put("verbindungLK", new SpecialPair("<Connection>", "<\\Connection>"));
            _specialTokens.put("verbindungCONTROL", new SpecialPair("<Connection>", "<\\Connection>"));
            _specialTokens.put("verbindungTHERM", new SpecialPair("<Connection>", "<\\Connection>"));
            _specialTokens.put("e", new SpecialPair("<ElementLK>", "<\\ElementLK>"));
            _specialTokens.put("sp", new SpecialPair("<ElementSPECIAL>", "<\\ElementSPECIAL>"));
            _specialTokens.put("eTH", new SpecialPair("<ElementTHERM>", "<\\ElementTHERM>"));
            _specialTokens.put("c", new SpecialPair("<ElementCONTROL>", "<\\ElementCONTROL>"));
            _specialTokens.put("GeckoFileManager", new SpecialPair("<GeckoFileManager>", "<\\GeckoFileManager>"));
        }
        makeTokenMap(ascii, makeSpecialPairs);
    }

    public String[] getLines() {
        return asciiLines;
    }

    private class SpecialPair {

        final String _startToken;
        final String _stopToken;

        public SpecialPair(final String startToken, final String stopToken) {
            _startToken = startToken;
            _stopToken = stopToken;
        }
    }

    public boolean containsToken(final String token) {
        return getLineNumber(token) != null;
    }

    public String getLineString(final String identifier, final String targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            return asciiLines[lineNumber];

        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }

    private String getLegacyKey(final String identifier) {
        switch (identifier) {
            case "_measuredLossesFilename": return "datnamGemesseneVerluste";
            case "numAxesX": return "anzahlAchsenX";
            case "numAxesY": return "anzahlAchsenY";
            case "widthPix": return "breitePix";
            case "heightPix": return "hoehePix";
            case "xAxisType": return "xAchseTyp";
            case "yAxisType": return "yAchseTyp";
            case "colorAxesX": return "farbeAchsenX";
            case "colorAxesY": return "farbeAchsenY";
            case "lineStyleAxesX": return "linienStilAchsenX";
            case "lineStyleAxesY": return "linienStilAchsenY";
            case "axisXmin": return "achseXmin";
            case "axisXmax": return "achseXmax";
            case "axisYmin": return "achseYmin";
            case "axisYmax": return "achseYmax";
            case "autoAxisXmin": return "autoAchseXmin";
            case "autoAxisXmax": return "autoAchseXmax";
            case "autoAxisYmin": return "autoAchseYmin";
            case "autoAxisYmax": return "autoAchseYmax";
            case "xAxisLabel": return "xAchseBeschriftung";
            case "yAxisLabel": return "yAchseBeschriftung";
            case "colorGridNormalX": return "farbeGridNormalX";
            case "colorGridNormalY": return "farbeGridNormalY";
            case "colorGridNormalXminor": return "farbeGridNormalXminor";
            case "colorGridNormalYminor": return "farbeGridNormalYminor";
            case "lineStyleGridNormalX": return "linStilGridNormalX";
            case "lineStyleGridNormalY": return "linStilGridNormalY";
            case "lineStyleGridNormalXminor": return "linStilGridNormalXminor";
            case "lineStyleGridNormalYminor": return "linStilGridNormalYminor";
            case "gridNormalX_associatedXAxis": return "gridNormalX_zugeordneteXAchse";
            case "gridNormalX_associatedYAxis": return "gridNormalX_zugeordneteYAchse";
            case "gridNormalY_associatedXAxis": return "gridNormalY_zugeordneteXAchse";
            case "gridNormalY_associatedYAxis": return "gridNormalY_zugeordneteYAchse";
            case "xNumTicksMinor": return "xAnzTicksMinor";
            case "yNumTicksMinor": return "yAnzTicksMinor";
            case "xTickLength": return "xTickLaenge";
            case "yTickLength": return "yTickLaenge";
            case "xTickLengthMinor": return "xTickLaengeMinor";
            case "yTickLengthMinor": return "yTickLaengeMinor";
            case "showXTicksBottom": return "zeigeXticksUnten";
            case "showYTicksLeft": return "zeigeYticksLinks";
            case "showLabelsXmaj": return "zeigeLabelsXmaj";
            case "showLabelsXmin": return "zeigeLabelsXmin";
            case "showLabelsYmaj": return "zeigeLabelsYmaj";
            case "showLabelsYmin": return "zeigeLabelsYmin";
            case "valueTickX": return "wertTickX";
            case "valueTickY": return "wertTickY";
            case "valueTickXminor": return "wertTickXminor";
            case "valueTickYminor": return "wertTickYminor";
            case "worksheetData": return "worksheetDaten";
            case "indexCurveAssociatedXAxis": return "indexZurKurveGehoerigeXachse";
            case "indexCurveAssociatedYAxis": return "indexZurKurveGehoerigeYachse";
            case "numCurves": return "anzahlKurven";
            case "curve_index_worksheetColumns_XY": return "kurve_index_worksheetKolonnen_XY";
            case "showCurvePointSymbol": return "kurvenPunktSymbolAnzeigen";
            case "curveClipping_xmin": return "kurveClippling_xmin";
            case "curveClipping_xmax": return "kurveClippling_xmax";
            case "curveClipping_ymin": return "kurveClippling_ymin";
            case "curveClipping_ymax": return "kurveClippling_ymax";
            case "mouseMode": return "mausModus";
            case "xSliderActive": return "xSchieberAktiv";
            case "xSliderPixels": return "xSchieberPix";
            case "xSliderValue": return "xSchieberWert";
            default: return null;
        }
    }

    public Integer getLineNumber(final String identifier) {
        try {
            Integer lineNum = _map.get(identifier);
            if (lineNum == null) {
                String baseId = identifier;
                String suffix = "";
                if (identifier.endsWith("[][]")) {
                    baseId = identifier.substring(0, identifier.length() - 4);
                    suffix = "[][]";
                } else if (identifier.endsWith("[]")) {
                    baseId = identifier.substring(0, identifier.length() - 2);
                    suffix = "[]";
                }
                String legacyBase = getLegacyKey(baseId);
                if (legacyBase != null) {
                    lineNum = _map.get(legacyBase + suffix);
                }
            }
            return lineNum;
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return null;
        }
    }

    private void makeTokenMap(final String[] ascii, final boolean makeSpecialPairs) {
        int readLineNumber = 0;
        while (readLineNumber < ascii.length) {
            final String line = ascii[readLineNumber];
            //System.out.println(readLineNumber + " " + line);

            int spaceIndex = line.indexOf(' ');
            if (spaceIndex < 1) {
                spaceIndex = line.length();
            }


            if (spaceIndex > 0) {
                char firstChar = line.charAt(0);
                if (Character.isDigit(firstChar) || Character.isWhitespace(firstChar)) {
                    readLineNumber++;
                    continue;
                }


                final String lineToken = line.substring(0, spaceIndex);
                //System.out.println("line: " + line);
                if (makeSpecialPairs) {
                    if (_specialTokens.containsKey(lineToken)) {
                        final SpecialPair pair = _specialTokens.get(lineToken);
//                        System.out.println("--------------");
//                        System.err.println("linetoken: " + lineToken + " " + pair._startToken + " " + pair._stopToken);
//                        System.err.println("reading: " + ascii[readLineNumber + 0]);
//                        System.err.println("reading: xxx " + ascii[readLineNumber + 1]);
//                        System.err.println("reading: " + ascii[readLineNumber + 2]);

                        final String nextLine = ascii[readLineNumber + 1];
                        if (nextLine.equals(pair._startToken)
                                || (pair._startToken.equals("<Connection>") && nextLine.equals("<Verbindung>"))
                                || (pair._startToken.equals("<Verbindung>") && nextLine.equals("<Connection>"))) {
                            final String endToken = nextLine.equals("<Verbindung>") ? "<\\Verbindung>" : pair._stopToken;
                            int j = readLineNumber;
                            for (; j < ascii.length && (ascii[j].isEmpty()
                                    || ascii[j].charAt(0) != '<'
                                    || !ascii[j].startsWith(endToken)); j++) {
                                continue; // scan forward to find end token
                            }

                            final BlockInfo blockInfo = new BlockInfo(readLineNumber, j, this);
                            if (_specialMap.containsKey(lineToken)) {
                                _specialMap.get(lineToken).add(blockInfo);
                            } else {
                                Deque<BlockInfo> list = new LinkedList<BlockInfo>();
                                list.add(blockInfo);
                                _specialMap.put(lineToken, list);
                            }

                            readLineNumber = j;
                        }
                    }
                } else {
                    try {
                    if (lineToken.charAt(0) == '<' && lineToken.length() > 1 && lineToken.charAt(1) != '\\') {
                        final String endToken = "<\\" + lineToken.substring(1, lineToken.length());
                        int j = readLineNumber;
                        for (; j < ascii.length
                                && (ascii[j].isEmpty() || ascii[j].charAt(0) != '<'
                                || !ascii[j].startsWith(endToken)); j++) {
                            continue; // scan forward to find end token
                        }

                        final BlockInfo tmpBlockInfo = new BlockInfo(readLineNumber, j, this, false);

                        if (_duplicateMap.containsKey(lineToken)) {
                            _duplicateMap.get(lineToken).add(tmpBlockInfo);
                        } else {
                            Deque<BlockInfo> list = new LinkedList<BlockInfo>();
                            list.add(tmpBlockInfo);
                            _duplicateMap.put(lineToken, list);
                        }
                    }
                    } catch (Exception ex) {
                        LOGGER.error("line token: " + lineToken);
                        ex.printStackTrace();
                    }
                }
                //System.out.println(lineToken);
                readTokenLine(lineToken, readLineNumber, _map);


            }
            readLineNumber++;
        }
    }

    private static void readTokenLine(final String lineToken, final int lineNumber, Map<String, Integer> map) {
        if (!map.containsKey(lineToken)) {
            map.put(lineToken, lineNumber);
        }

        // This is for repairing a severe file format bug in old versions of GeckoCIRCUITS. Could maybe
        // removed in the future (current date: Octorber 2012)
        if(lineToken.equalsIgnoreCase("orientierung")) {
            map.put(lineToken, lineNumber);
        }

    }

    public TokenMap getBlockMap() {
        return null;
    }

    public void makeBlockTokenMap(final String[] ascii) {
        // No-op: block token map construction not needed for this implementation
    }

    public TokenMap getSpecialBlockTokenMap(final String identifier) {
        if (_specialMap.containsKey(identifier)) {
            Deque<BlockInfo> blockInfoList = _specialMap.get(identifier);
            if (blockInfoList.isEmpty()) {
                return null;
            }
            BlockInfo block = blockInfoList.pollFirst();
            return block._tokenMap;
        }
        return null;
    }

    public String[] getSpecialBlockToken(final String identifier) {
        if (_specialMap.containsKey(identifier)) {
            Deque<BlockInfo> blockInfoList = _specialMap.get(identifier);
            if (blockInfoList.isEmpty()) {
                return null;
            }
            BlockInfo block = blockInfoList.pollFirst();
            final String[] returnValue = new String[block._stopIndex - block._startIndex + 1];
            for (int i = block._startIndex, j = 0; i <= block._stopIndex; i++, j++) {
                returnValue[j] = asciiLines[i];
            }
            return returnValue;
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder returnBuilder = new StringBuilder();
        for (int i = 0; i < asciiLines.length; i++) {
            returnBuilder.append(asciiLines[i]);
            if (i < asciiLines.length - 1) {
                returnBuilder.append("\n");
            }
        }
        return returnBuilder.toString();
    }

    public TokenMap getBlockTokenMap(final String identifier) {
        if (_duplicateMap.containsKey(identifier)) {
            Deque<BlockInfo> blockInfoList = _duplicateMap.get(identifier);
            if (blockInfoList.isEmpty()) {
                return null;
            }
            BlockInfo block = blockInfoList.pollFirst();
            return block._tokenMap;
        }
        return null;
    }

    private static class BlockInfo {

        final int _startIndex;
        final int _stopIndex;
        final TokenMap _tokenMap;

        public BlockInfo(final int startIndex, final int stopIndex, TokenMap parent) {
            int shiftedStartIndex = startIndex;
            while (!parent.asciiLines[shiftedStartIndex].startsWith("<")) {
                shiftedStartIndex++;
            }

            _startIndex = shiftedStartIndex + 1; // remove the first <token>
            _stopIndex = stopIndex;
            String[] subBlock = new String[_stopIndex - _startIndex];
            for (int i = _startIndex, j = 0; i < stopIndex; i++, j++) {
                subBlock[j] = parent.asciiLines[i];
            }
            _tokenMap = new TokenMap(subBlock);
        }

        public BlockInfo(final int startIndex, final int stopIndex, TokenMap parent, boolean dummy) {
            int shiftedStartIndex = startIndex;
            while (!parent.asciiLines[shiftedStartIndex].startsWith("<")) {
                shiftedStartIndex++;
            }

            _startIndex = shiftedStartIndex + 1; // remove the first <token>
            _stopIndex = stopIndex;
            final String[] subBlock = new String[_stopIndex - _startIndex];
            for (int i = _startIndex, j = 0; i < stopIndex; i++, j++) {
                subBlock[j] = parent.asciiLines[i];
            }
            _tokenMap = new TokenMap(subBlock);
        }
    }

    public long readDataLine(final String identifier, final long targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];
            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            return Long.parseLong(stk.nextToken());
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }

    public String readDataLine(final String identifier, final String targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];
            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            String wert = stk.nextToken();
            if (wert.equals(CircuitFileConstants.NIX)) {
                return "";
            }

            int firstSpaceIndex = ascii.indexOf(" ");
            return ascii.substring(firstSpaceIndex + 1);
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }

    public boolean readDataLine(final String identifier, final boolean targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];
            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            return Boolean.parseBoolean(stk.nextToken());
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }

    public double readDataLine(final String identifier, final double targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];
            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            return Double.parseDouble(stk.nextToken());
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }

    public HiLoData readDataLine(final String identifier, final HiLoData targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];
            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            float lowValue = Float.parseFloat(stk.nextToken());
            float hiValue = Float.parseFloat(stk.nextToken());
            return HiLoData.hiLoDataFabric(lowValue, hiValue);
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }

    public String[] readDataLine(final String identifier, final String[] targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];
            final String asciiDaten = ascii.substring(ascii.indexOf(' '));
            final StringTokenizer stk = new StringTokenizer(asciiDaten, CircuitFileConstants.SEPARATOR_ASCII_STRINGARRAY);
            stk.nextToken();  // erster Wert wird uebersprungen
            String[] wert = new String[stk.countTokens()];
            for (int i1 = 0; i1 < wert.length; i1++) {
                wert[i1] = stk.nextToken();
                if (wert[i1].equals(CircuitFileConstants.NIX)) {
                    wert[i1] = "";
                }
            }
            return wert;
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }

    public List<String> readDataLineStringArray(final String identifier) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];
            final StringTokenizer stk = new StringTokenizer(ascii, CircuitFileConstants.SEPARATOR_ASCII_STRINGARRAY);
            stk.nextToken();  // erster Wert wird uebersprungen
            final int numberTokens = stk.countTokens();
            List<String> wert = new ArrayList<String>();
            for (int i1 = 0; i1 < numberTokens; i1++) {
                String token = stk.nextToken();
                if (token.equals(CircuitFileConstants.NIX)) {
                    wert.add("");
                } else {
                    wert.add(token);
                }
            }
            return wert;
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return Collections.EMPTY_LIST;
        }
    }


    public boolean[][] readDataLine(final String identifier, final boolean[][] targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];
            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            final int size1 = Integer.parseInt(stk.nextToken());
            final int size2 = Integer.parseInt(stk.nextToken());
            final boolean[][] wert = new boolean[size1][size2];
            for (int i1 = 0; i1 < size1; i1++) {
                for (int i2 = 0; i2 < size2; i2++) {
                    wert[i1][i2] = Boolean.parseBoolean(stk.nextToken());
                }
            }
            return wert;
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }

    public int[][] readDataLine(final String identifier, final int[][] targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];

            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            final int size1 = Integer.parseInt(stk.nextToken());
            final int size2 = Integer.parseInt(stk.nextToken());
            int[][] wert = new int[size1][size2];
            for (int i1 = 0; i1 < size1; i1++) {
                for (int i2 = 0; i2 < size2; i2++) {
                    wert[i1][i2] = Integer.parseInt(stk.nextToken());
                }
            }
            return wert;
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }

    public double[][] readDataLine(final String identifier, final double[][] targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];

            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            final int size1 = Integer.parseInt(stk.nextToken());
            final int size2 = Integer.parseInt(stk.nextToken());
            double[][] wert = new double[size1][size2];
            for (int i1 = 0; i1 < size1; i1++) {
                for (int i2 = 0; i2 < size2; i2++) {
                    wert[i1][i2] = Double.parseDouble(stk.nextToken());
                }
            }
            return wert;
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }

    public boolean[] readDataLine(final String identifier, final boolean[] targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];

            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            final boolean[] wert = new boolean[stk.countTokens() - 1];
            stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            for (int i1 = 0; i1 < wert.length; i1++) {
                final String zzString = stk.nextToken();
                if (i1 == 0 && "null".equals(zzString)) {
                    return null;
                }
                wert[i1] = Boolean.parseBoolean(zzString);
            }
            return wert;
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }

    /**
     * reads a textblock, including spaces. In contradiction, LeseAsciiString would return only the first token! If a \n
     * character appears, a newline is done.
     *
     * @param ascii
     * @return
     */
    public String leseASCIITextBlock(final String identifier, final String targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];

            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            final String identifierNew = stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            String wert = ascii;
            if (stk.hasMoreElements()) {
                // remove first token, the rest is the String to read in.
                wert = ascii.substring(identifierNew.length() + 1);
                wert = wert.replaceAll("\\\\n", "\n");
            }
            if (wert.equals(CircuitFileConstants.NIX)) {
                wert = "";
            }
            return wert;
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }

    public double[] readDataLine(final String identifier, final double[] targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];

            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            final double[] wert = new double[stk.countTokens() - 1];
            stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            for (int i1 = 0; i1 < wert.length; i1++) {
                final String zzString = stk.nextToken();
                if (i1 == 0 && "null".equals(zzString)) {
                    return null;
                }
                wert[i1] = Double.parseDouble(zzString);
            }
            return wert;
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }

    }

    public List<Double> readDataLineDoubleArray(final String identifier) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];

            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            final int numberReadTokens = stk.countTokens()-1;
            final List<Double> wert = new ArrayList<Double>();
            stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            for (int i1 = 0; i1 < numberReadTokens; i1++) {
                final String zzString = stk.nextToken();
                if (i1 == 0 && "null".equals(zzString)) {
                    return null;
                }
                wert.add(Double.parseDouble(zzString));
            }
            return wert;
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return Collections.EMPTY_LIST;
        }
    }


    public int[] readDataLine(final String identifier, final int[] targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];

            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            final int[] wert = new int[stk.countTokens() - 1];
            stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            for (int i1 = 0; i1 < wert.length; i1++) {
                final String zzString = stk.nextToken();
                if (i1 == 0 && "null".equals(zzString)) {
                    return null;
                }
                wert[i1] = Integer.parseInt(zzString);
            }
            return wert;
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }

    public long[] readDataLine(final String identifier, final long[] targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];

            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            final long[] wert = new long[stk.countTokens() - 1];
            stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            for (int i1 = 0; i1 < wert.length; i1++) {
                final String zzString = stk.nextToken();
                if (i1 == 0 && "null".equals(zzString)) {
                    return null;
                }
                wert[i1] = Long.parseLong(zzString);
            }
            return wert;
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }



    public byte[] readDataLine(final String identifier, final byte[] targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];

            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            final byte[] wert = new byte[stk.countTokens() - 1];
            stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            for (int i1 = 0; i1 < wert.length; i1++) {
                final String zzString = stk.nextToken();
                if (i1 == 0 && "null".equals(zzString)) {
                    return null;
                }
                wert[i1] = Byte.parseByte(zzString);
            }
            return wert;
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }

    public int readDataLine(final String identifier, final int targetObject) {
        try {
            final Integer lineNumber = getLineNumber(identifier);
            final String ascii = asciiLines[lineNumber];

            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            return Integer.parseInt(stk.nextToken());
        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }

    private static void logErrorString(final String identifier, final Exception exception) {
        final String messageString = "Could not read data line with identifier " + identifier;LogManager.getLogger(TokenMap.class).warn(messageString);
        exception.printStackTrace();
    }

    public String[] findSubBlock(String startIdentifier, String stopIdentifier) {
        try {
            final Integer startLine = getLineNumber(startIdentifier);
            final int shiftedStartLine = startLine + 1;
            final Integer stopLine = getLineNumber(stopIdentifier);

            final String[] returnValue = new String[stopLine - shiftedStartLine];
            for (int i = shiftedStartLine, j = 0; i < stopLine; i++, j++) {
                returnValue[j] = asciiLines[i];
            }
            return returnValue;
        } catch (Exception ex) {
            logErrorString(startIdentifier, ex);
            return new String[0];
        }
    }

    public String createSubBlock(final String startIdentifier, final String stopIdentifier) {
        final String[] subBlock = findSubBlock(startIdentifier, stopIdentifier);
        final StringBuilder builder = new StringBuilder(4048);

        for (int i = 0; i < subBlock.length; i++) {
            builder.append(subBlock[i]);
            if (i < subBlock.length - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }
}
