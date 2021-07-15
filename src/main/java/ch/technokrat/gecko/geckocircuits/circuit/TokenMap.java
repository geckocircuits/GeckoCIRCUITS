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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author andreas
 */
public final class TokenMap {

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
            _specialTokens.put("verbindungLK", new SpecialPair("<Verbindung>", "<\\Verbindung>"));
            _specialTokens.put("verbindungCONTROL", new SpecialPair("<Verbindung>", "<\\Verbindung>"));
            _specialTokens.put("verbindungTHERM", new SpecialPair("<Verbindung>", "<\\Verbindung>"));
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
        return _map.containsKey(token);
    }

    public String getLineString(final String identifier, final String targetObject) {
        try {
            final Integer lineNumber = _map.get(identifier);
            return asciiLines[lineNumber];

        } catch (Exception ex) {
            logErrorString(identifier, ex);
            return targetObject;
        }
    }

    public Integer getLineNumber(final String identifier) {
        try {
            return _map.get(identifier);
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

                
                final String lineToken = line.substring(0, spaceIndex);;
                //System.out.println("line: " + line);
                if (makeSpecialPairs) {
                    if (_specialTokens.containsKey(lineToken)) {
                        final SpecialPair pair = _specialTokens.get(lineToken);
//                        System.out.println("--------------");
//                        System.err.println("linetoken: " + lineToken + " " + pair._startToken + " " + pair._stopToken);
//                        System.err.println("reading: " + ascii[readLineNumber + 0]);
//                        System.err.println("reading: xxx " + ascii[readLineNumber + 1]);
//                        System.err.println("reading: " + ascii[readLineNumber + 2]);
                        
                        if (ascii[readLineNumber + 1].equals(pair._startToken)) {
                            final String endToken = pair._stopToken;
                            int j = readLineNumber;
                            for (; j < ascii.length && (ascii[j].isEmpty()
                                    || ascii[j].charAt(0) != '<'
                                    || !ascii[j].startsWith(endToken)); j++) {
                            }

                            final BlockInfo blockInfo = new BlockInfo(readLineNumber, j, this);
                            if (_specialMap.containsKey(lineToken)) {
                                _specialMap.get(lineToken).add(blockInfo);
                            } else {
                                Deque<BlockInfo> list = new LinkedList<BlockInfo>();
                                list.add(blockInfo);
                                _specialMap.put(lineToken, list);
                            }

                            int nrLines = j - readLineNumber;
                            readLineNumber = j;
                        }
                    } 
                } else try {
                    if (lineToken.charAt(0) == '<' && lineToken.length() > 1 && lineToken.charAt(1) != '\\') {
                        final String endToken = "<\\" + lineToken.substring(1, lineToken.length());
                        int j = readLineNumber;
                        for (; j < ascii.length
                                && (ascii[j].isEmpty() || ascii[j].charAt(0) != '<'
                                || !ascii[j].startsWith(endToken)); j++) {
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
                    System.err.println("line token: " + lineToken);
                    ex.printStackTrace();
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
        for (int i = 0; i < ascii.length; i++) {
        }
    }

    public TokenMap getSpecialBlockTokenMap(final String identifier) {        
        if (_specialMap.containsKey(identifier)) {
            Deque<BlockInfo> blockInfoList = _specialMap.get(identifier);
            if (blockInfoList.isEmpty()) {
                return null;
            }
            BlockInfo block = blockInfoList.pollFirst();
            return block._tokenMap;
        } else {
            // nothing todo - no element found!
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
        } else {
//            System.out.println("not found: " + identifier);
//            System.out.println("available:");
//            for(String key : _duplicateMap.keySet()) {
//                System.out.println("key: " + key);
//            }
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
            final Integer lineNumber = _map.get(identifier);
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
            final Integer lineNumber = _map.get(identifier);
            final String ascii = asciiLines[lineNumber];
            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen            
            String wert = stk.nextToken();
            if (wert.equals(DatenSpeicher.NIX)) {
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
            final Integer lineNumber = _map.get(identifier);
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
            final Integer lineNumber = _map.get(identifier);
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
            final Integer lineNumber = _map.get(identifier);
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
            final Integer lineNumber = _map.get(identifier);
            final String ascii = asciiLines[lineNumber];
            final String asciiDaten = ascii.substring(ascii.indexOf(' '));
            final StringTokenizer stk = new StringTokenizer(asciiDaten, DatenSpeicher.SEPARATOR_ASCII_STRINGARRAY);
            stk.nextToken();  // erster Wert wird uebersprungen
            String[] wert = new String[stk.countTokens()];
            for (int i1 = 0; i1 < wert.length; i1++) {
                wert[i1] = stk.nextToken();
                if (wert[i1].equals(DatenSpeicher.NIX)) {
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
            final Integer lineNumber = _map.get(identifier);
            final String ascii = asciiLines[lineNumber];            
            final StringTokenizer stk = new StringTokenizer(ascii, DatenSpeicher.SEPARATOR_ASCII_STRINGARRAY);
            stk.nextToken();  // erster Wert wird uebersprungen
            final int numberTokens = stk.countTokens();            
            List<String> wert = new ArrayList<String>();
            for (int i1 = 0; i1 < numberTokens; i1++) {
                String token = stk.nextToken();
                if (token.equals(DatenSpeicher.NIX)) {
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
            final Integer lineNumber = _map.get(identifier);
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
            final Integer lineNumber = _map.get(identifier);
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
            final Integer lineNumber = _map.get(identifier);
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
            final Integer lineNumber = _map.get(identifier);
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
            final Integer lineNumber = _map.get(identifier);
            final String ascii = asciiLines[lineNumber];

            final StringTokenizer stk = new StringTokenizer(ascii, " ");
            final String identifierNew = stk.nextToken();  // 1.Eintrag ist ID-String --> wird uebersprungen
            String wert = ascii;
            if (stk.hasMoreElements()) {
                // remove first token, the rest is the String to read in.
                wert = ascii.substring(identifierNew.length() + 1);
                wert = wert.replaceAll("\\\\n", "\n");
            }
            if (wert.equals(DatenSpeicher.NIX)) {
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
            final Integer lineNumber = _map.get(identifier);
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
            final Integer lineNumber = _map.get(identifier);
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
            final Integer lineNumber = _map.get(identifier);
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
            final Integer lineNumber = _map.get(identifier);
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
            final Integer lineNumber = _map.get(identifier);
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
            final Integer lineNumber = _map.get(identifier);
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
        final String messageString = "Could not read data line with identifier " + identifier;
        Logger.getLogger(TokenMap.class.getName()).log(Level.WARNING,
                messageString);
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
