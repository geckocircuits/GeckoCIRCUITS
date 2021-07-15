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
package ch.technokrat.gecko.geckocircuits.allg;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.SubcircuitBlock;
import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.geckocircuits.control.ControlTyp;
import ch.technokrat.gecko.geckocircuits.control.NetzlisteCONTROL;
import ch.technokrat.gecko.geckocircuits.control.Point;
import ch.technokrat.gecko.geckocircuits.control.RegelBlock;
import ch.technokrat.gecko.geckocircuits.newscope.HiLoData;
import ch.technokrat.gecko.geckoscript.SimulationAccess;
import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.JOptionPane;

// Hilfsklasse: Format fuer die Daten-Speicherung
public class DatenSpeicher implements Serializable {

    public final List<AbstractCircuitSheetComponent> _allSheetComponents = new ArrayList<AbstractCircuitSheetComponent>();
    public List<SubcircuitBlock> allSubCircuitBlocks = new ArrayList<SubcircuitBlock>();
    private OptimizerParameterData optimizerParameterData;
    public String geckoOpt_code_ascii;
    //------------------
    // Simulationsparameter
    public double _dt, _tDURATION;
    public double _tPAUSE = -1;
    //------------------
    // Ansicht im SchematicEntry
    public int dpix = 16;
    public int fontSize;
    public String _fontTyp;
    public int _fensterWidth = -1, _fensterHeight = -1;  // Speicherung der individuellen Fenstergroesse 
    //------------------
    private DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd");
    double _dt_pre;
    double _T_pre;
    public int _uniqueFileId;
    //------------------
    private SimulationAccess _scripter;
    public String _scripterCode = "";
    public String _scripterImports = "";
    public String _scripterDeclarations = "";
    public String _scripterExtraFiles = "";
    public int solverType;
    public boolean saveAsApplet = false;
    private GeckoFileManager _fileManager;
    public ArrayList<GeckoFile> fileMgrFiles;
    public static int readFileVersion;
    public int sizeX = 30;
    public int sizeY = 30;
    public static final String SEPARATOR_ASCII_STRINGARRAY = "/";
    // NULL-Symbol ("") fuer Labels --> notwendig, weil " " als Separator bei der ASCII-Speicherung genutzt wird, und "" schwer zum Wiederherstellen ist
    public static final String NIX = "NIX_NIX_NIX";
    public List<String> _optimizerNames = new ArrayList<String>();
    public List<Double> _optimizerData = new ArrayList<Double>();
    private OptimizerParameterData _optimizer;

    public DatenSpeicher(
            Dimension windowDimension,
            OptimizerParameterData optimizerParameterData, 
            int uniqueFileId, SimulationAccess scripter, GeckoFileManager fileMgr, SchematischeEingabe2 se,
            SolverSettings solverSettings) {
        this._allSheetComponents.addAll(se._circuitSheet.getAllElements());
        this._dt = solverSettings.dt.getValue();
        this._tDURATION = solverSettings._tDURATION.getValue();
        this._T_pre = solverSettings._T_pre.getValue();
        this._dt_pre = solverSettings._dt_pre.getValue();
        this.solverType = solverSettings.SOLVER_TYPE.getValue().getOldGeckoIndex();

        this.dpix = AbstractCircuitSheetComponent.dpix;
        this.fontSize = SchematischeEingabe2.circuitFont.getSize();
        this._fontTyp = SchematischeEingabe2.circuitFont.getFontName();
        this._fensterWidth = windowDimension.width;
        this._fensterHeight = windowDimension.height;

        this.optimizerParameterData = optimizerParameterData;
        this._tPAUSE = solverSettings._tPAUSE.getValue();
        this._uniqueFileId = uniqueFileId;
        _scripter = scripter;
        _fileManager = fileMgr;

    }

    public DatenSpeicher(String[] ascii, final boolean isBackupRead, OptimizerParameterData optimizer) {
//        if(!isBackupRead) {            
//            for(String tmp : ascii) {
//                System.out.println(tmp);
//            }
//        }
        _optimizer = optimizer;
        optimizerParameterData = new OptimizerParameterData();
        _uniqueFileId = this.importASCII(ascii, isBackupRead);
    }

    public String exportASCII() {
        // Verbindung -->
        StringBuffer asc = new StringBuffer();
        //asc.append("\nverbindungLeistungskreisANZAHL ").append(connectors.size());
        int elementCounter = 0;
        for (AbstractCircuitSheetComponent elem : _allSheetComponents) {
            asc.append("\n" + elem.getExportImportCharacters() + " (" + elementCounter + ")\n");
            elem.exportASCII(asc);
            elementCounter++;
        }
        
        DatenSpeicher.appendAsString(asc.append("\noptimizerName"), optimizerParameterData.getNameOpt());
        DatenSpeicher.appendAsString(asc.append("\noptimizerValue"), optimizerParameterData.getValueOpt());
        DatenSpeicher.appendAsString(asc, "\n<scripterCode>\n" + _scripter.getScriptCode() + "\n<\\scripterCode>");
        DatenSpeicher.appendAsString(asc, "\n<scripterImports>\n" + _scripter.getImportCode() + "\n<\\scripterImports>");
        DatenSpeicher.appendAsString(asc, "\n<scripterDeclarations>\n" + _scripter.getDeclarationCode() + " " + "\n<\\scripterDeclarations>");
        DatenSpeicher.appendAsString(asc, "\n<extraScriptSourceFiles>\n" + _scripter.getExtraFilesHashes() + " " + "\n<\\extraScriptSourceFiles>");                
        
        asc.append("\n");
        if (saveAsApplet) {
            _fileManager.exportASCIIApplet(asc);
        } else {
            _fileManager.exportASCII(asc);
        }
//        geckoOpt.exportASCII(asc); 
        asc.append("\n\n");
        //------------------
        // aktuelles Datum: 
        asc.append("\nDtStor ").append(dFormat.format(new Date()));
        //------------------
        // Simulations-Einstellungen:
        asc.append("\ntDURATION ").append(_tDURATION);
        asc.append("\ndt ").append(_dt);
        asc.append("\ntPAUSE ").append(_tPAUSE);
        asc.append("\nT_pre ").append(_T_pre);
        asc.append("\ndt_pre ").append(_dt_pre);
        asc.append("\nsolverType ").append(solverType);
        asc.append("\npath ").append(GlobalFilePathes.DATNAM);
        //
        asc.append("\n\ndpix ").append(dpix);
        asc.append("\nfontSize ").append(fontSize);
        asc.append("\nfontTyp ").append(_fontTyp);
        asc.append("\nfensterWidth ").append(_fensterWidth);
        asc.append("\nfensterHeight ").append(_fensterHeight);
        Fenster._se._circuitSheet._worksheetSize.exportAscii(asc);
        // 
        asc.append("\nANSICHT_SHOW_LK_NAME ").append(SchematischeEingabe2._lkDisplayMode.showName);
        asc.append("\nANSICHT_SHOW_LK_PARAMETER ").append(SchematischeEingabe2._lkDisplayMode.showParameter);
        asc.append("\nANSICHT_SHOW_LK_FLOWDIR ").append(SchematischeEingabe2._lkDisplayMode.showFlowSymbol);
        asc.append("\nANSICHT_SHOW_LK_TEXTLINIE ").append(SchematischeEingabe2._lkDisplayMode.showTextLine);
        asc.append("\nANSICHT_SHOW_THERM_NAME ").append(SchematischeEingabe2._thermDisplayMode.showName);
        asc.append("\nANSICHT_SHOW_THERM_PARAMETER ").append(SchematischeEingabe2._thermDisplayMode.showParameter);
        asc.append("\nANSICHT_SHOW_THERM_FLOWDIR ").append(SchematischeEingabe2._thermDisplayMode.showFlowSymbol);
        asc.append("\nANSICHT_SHOW_THERM_TEXTLINIE ").append(SchematischeEingabe2._thermDisplayMode.showTextLine);
        asc.append("\nANSICHT_SHOW_CONTROL_NAME ").append(SchematischeEingabe2._controlDisplayMode.showName);
        asc.append("\nANSICHT_SHOW_CONTROL_PARAMETER ").append(SchematischeEingabe2._controlDisplayMode.showParameter);
        asc.append("\nANSICHT_SHOW_CONTROL_TEXTLINIE ").append(SchematischeEingabe2._controlDisplayMode.showTextLine);
        asc.append("\nFileVersion " + DialogAbout.RELEASENUMBER);
        asc.append("\nUniqueFileId " + _uniqueFileId);
        
        final int globalDataSize = NetzlisteCONTROL.globalData.getRowLength();
        String[] signalNames = new String[globalDataSize];
        for (int i = 0; i < globalDataSize; i++) {
            signalNames[i] = NetzlisteCONTROL.globalData.getSignalName(i);
        }

        DatenSpeicher.appendAsString(asc.append("\ndataContainerSignals[] "), signalNames);

        //
        asc.append("\n=======================\n ");

        return asc.toString();
    }

    private int importASCII(String[] ascii, final boolean isBackupRead) {
        long tic = System.currentTimeMillis();
        final TokenMap tokenMap = new TokenMap(ascii, true);
        //tokenMap.makeBlockTokenMap(ascii, true);        

        Random generator = new Random(System.currentTimeMillis());

        int uniqueFileId = generator.nextInt();
        if (tokenMap.containsToken("UniqueFileId")) {
            uniqueFileId = tokenMap.readDataLine("UniqueFileId", uniqueFileId);
        }

        if (isBackupRead) {
            return uniqueFileId;
        }

        /**
         * local import/export does not show this fields!
         */
        if (tokenMap.containsToken("tDURATION")) {
            _tDURATION = tokenMap.readDataLine("tDURATION", _tDURATION);
            _dt = tokenMap.readDataLine("dt", _dt);
            String nameString = tokenMap.getLineString("path", "path ");
            GlobalFilePathes.datnamAbsLoadIPES = nameString.substring((new String("path ").length()));  // wichtig, weil Pfadname Leerzeichen enthalten kann
            fontSize = tokenMap.readDataLine("fontSize", fontSize);
            final String fontString = tokenMap.getLineString("fontTyp", "fontTyp ");
            _fontTyp = fontString.substring((new String("fontTyp ")).length());  // wichtig, falls FontName Leerzeichen enthaelt!
            _fensterWidth = tokenMap.readDataLine("fensterWidth", _fensterWidth);
            _fensterHeight = tokenMap.readDataLine("fensterHeight", _fensterHeight);
        }

        if (tokenMap.containsToken("worksheetSize")) { // old format
            String worksheetSize = tokenMap.readDataLine("worksheetSize", "600x600");
            sizeX = WorksheetSize.getOldFormatWSSize(worksheetSize);
            sizeY = WorksheetSize.getOldFormatWSSize(worksheetSize);
        } else {
            Point size = WorksheetSize.getSize(tokenMap);
            sizeX = size.x;
            sizeY = size.y;
        }



        if (tokenMap.containsToken("dt_pre")) {
            _dt_pre = tokenMap.readDataLine("dt_pre", _dt_pre);
        }

        if (tokenMap.containsToken("solverType")) {
            solverType = tokenMap.readDataLine("solverType", solverType);
        }

        if (tokenMap.containsToken("T_pre")) {
            _T_pre = tokenMap.readDataLine("T_pre", _T_pre);
        } else {
            _T_pre = -1;
        }

        if (tokenMap.containsToken("tPAUSE")) {
            _tPAUSE = tokenMap.readDataLine("tPAUSE", _tPAUSE);
        }


        if (tokenMap.containsToken("dpix")) {
            dpix = tokenMap.readDataLine("dpix", dpix);
        } else {
            dpix = 10;
        }


        if (tokenMap.containsToken("ANSICHT_SHOW_LK_NAME")) {
            SchematischeEingabe2._lkDisplayMode.showName = tokenMap.readDataLine("ANSICHT_SHOW_LK_NAME",
                    SchematischeEingabe2._lkDisplayMode.showName);


            SchematischeEingabe2._lkDisplayMode.showParameter = tokenMap.readDataLine("ANSICHT_SHOW_LK_PARAMETER",
                    SchematischeEingabe2._lkDisplayMode.showParameter);

            SchematischeEingabe2._lkDisplayMode.showFlowSymbol = tokenMap.readDataLine("ANSICHT_SHOW_LK_FLOWDIR",
                    SchematischeEingabe2._lkDisplayMode.showFlowSymbol);

            SchematischeEingabe2._lkDisplayMode.showTextLine = tokenMap.readDataLine("ANSICHT_SHOW_LK_TEXTLINIE",
                    SchematischeEingabe2._lkDisplayMode.showTextLine);

            SchematischeEingabe2._thermDisplayMode.showName = tokenMap.readDataLine("ANSICHT_SHOW_THERM_NAME",
                    SchematischeEingabe2._thermDisplayMode.showName);

            SchematischeEingabe2._thermDisplayMode.showParameter = tokenMap.readDataLine("ANSICHT_SHOW_THERM_PARAMETER",
                    SchematischeEingabe2._thermDisplayMode.showParameter);

            SchematischeEingabe2._thermDisplayMode.showFlowSymbol = tokenMap.readDataLine("ANSICHT_SHOW_THERM_FLOWDIR",
                    SchematischeEingabe2._thermDisplayMode.showFlowSymbol);

            SchematischeEingabe2._thermDisplayMode.showTextLine = tokenMap.readDataLine("ANSICHT_SHOW_THERM_TEXTLINIE",
                    SchematischeEingabe2._thermDisplayMode.showTextLine);
            SchematischeEingabe2._controlDisplayMode.showName = tokenMap.readDataLine("ANSICHT_SHOW_CONTROL_NAME",
                    SchematischeEingabe2._controlDisplayMode.showName);

            SchematischeEingabe2._controlDisplayMode.showParameter = tokenMap.readDataLine("ANSICHT_SHOW_CONTROL_PARAMETER",
                    SchematischeEingabe2._controlDisplayMode.showParameter);

            SchematischeEingabe2._controlDisplayMode.showTextLine = tokenMap.readDataLine("ANSICHT_SHOW_CONTROL_TEXTLINIE",
                    SchematischeEingabe2._controlDisplayMode.showTextLine);

        }

        GeckoSim._win.vItemShowParLK.setSelected(SchematischeEingabe2._lkDisplayMode.showParameter);
        GeckoSim._win.vItemShowFlowLK.setSelected(SchematischeEingabe2._lkDisplayMode.showFlowSymbol);
        GeckoSim._win.vItemShowTextLineLK.setSelected(SchematischeEingabe2._lkDisplayMode.showTextLine);
        GeckoSim._win.vItemShowNameLK.setSelected(SchematischeEingabe2._lkDisplayMode.showName);

        GeckoSim._win.vItemShowParCONTROL.setSelected(SchematischeEingabe2._controlDisplayMode.showParameter);
        GeckoSim._win.vItemShowTextLineCONTROL.setSelected(SchematischeEingabe2._controlDisplayMode.showTextLine);
        GeckoSim._win.vItemShowNameCONTROL.setSelected(SchematischeEingabe2._controlDisplayMode.showName);

        GeckoSim._win.vItemShowParTHERM.setSelected(SchematischeEingabe2._thermDisplayMode.showParameter);
        GeckoSim._win.vItemShowFlowTHERM.setSelected(SchematischeEingabe2._thermDisplayMode.showFlowSymbol);
        GeckoSim._win.vItemShowTextLineTHERM.setSelected(SchematischeEingabe2._thermDisplayMode.showTextLine);
        GeckoSim._win.vItemShowNameTHERM.setSelected(SchematischeEingabe2._thermDisplayMode.showName);

                
        
        if (tokenMap.containsToken("optimizerName[]")) {
            _optimizerNames = tokenMap.readDataLineStringArray("optimizerName[]");
            _optimizerData = tokenMap.readDataLineDoubleArray("optimizerValue[]");
        }
        
        if(_optimizer != null) {            
            _optimizer.clearAndInitializeWithoutUndo(_optimizerNames, _optimizerData);        
        }

        if (tokenMap.containsToken("<scripterCode>")) {
            _scripterCode = tokenMap.createSubBlock("<scripterCode>", "<\\scripterCode>");
            _scripterImports = tokenMap.createSubBlock("<scripterImports>", "<\\scripterImports>");
            _scripterDeclarations = tokenMap.createSubBlock("<scripterDeclarations>", "<\\scripterDeclarations>");
            if(tokenMap.containsToken("<extraScriptSourceFiles>")) {
                _scripterExtraFiles = tokenMap.createSubBlock("<extraScriptSourceFiles>", "<\\extraScriptSourceFiles>");
            }            
        }



        readFileVersion = -1;

        if (tokenMap.containsToken("FileVersion")) {
            readFileVersion = tokenMap.readDataLine("FileVersion", readFileVersion);                                    
            if (readFileVersion < 160) {
                JOptionPane.showMessageDialog(null,
                        "This Model file was created with an older version of GeckoCIRCUITS.\n"
                        + "When you save model files, please consider that you cannot open files\n"
                        + "generated by this Version of GeckoCIRCUITS with older releases.",
                        "Info",
                        JOptionPane.WARNING_MESSAGE);
            }
            if (readFileVersion > DialogAbout.RELEASENUMBER) {
                JOptionPane.showMessageDialog(null,
                        "This Model file was created with a newer version of GeckoCIRCUITS.\n"
                        + "Please consider to update your Software to the newest version.\n"
                        + "You can find update information in the menu Help -> Updates.",
                        "Info",
                        JOptionPane.WARNING_MESSAGE);
            }
        }



        if (!isBackupRead && tokenMap.containsToken("dataContainerSignals[]")) {
            String[] sigNames = new String[0];
            sigNames = tokenMap.readDataLine("dataContainerSignals[]", sigNames);
            for (int row = 0; row < sigNames.length; row++) {
                // TODO ??? NetzlisteCONTROL.globalData.setSignalName(row, sigNames[row]);
            }
        }

        if (_dt_pre <= 0) {
            _dt_pre = _dt;
        }

        _allSheetComponents.clear();

        for (TokenMap verbindungMap = tokenMap.getSpecialBlockTokenMap("verbindungLK");
                verbindungMap != null; verbindungMap = tokenMap.getSpecialBlockTokenMap("verbindungLK")) {
            _allSheetComponents.add(new Verbindung(verbindungMap, ConnectorType.LK));
        }

        for (TokenMap verbindungMap = tokenMap.getSpecialBlockTokenMap("verbindungCONTROL");
                verbindungMap != null; verbindungMap = tokenMap.getSpecialBlockTokenMap("verbindungCONTROL")) {
            _allSheetComponents.add(new Verbindung(verbindungMap, ConnectorType.CONTROL));
        }

        for (TokenMap verbindungMap = tokenMap.getSpecialBlockTokenMap("verbindungTHERM");
                verbindungMap != null; verbindungMap = tokenMap.getSpecialBlockTokenMap("verbindungTHERM")) {
            _allSheetComponents.add(new Verbindung(verbindungMap, ConnectorType.THERMAL));
        }


        for (TokenMap elementLKMap = tokenMap.getSpecialBlockTokenMap("e");
                elementLKMap != null; elementLKMap = tokenMap.getSpecialBlockTokenMap("e")) {
            int typ = elementLKMap.readDataLine("typ", -1);
            AbstractBlockInterface newBlock = AbstractTypeInfo.fabricFromFile(CircuitTyp.getFromIntNumber(typ), elementLKMap);
            _allSheetComponents.add(newBlock);
        }

        for (TokenMap thermTokenMap = tokenMap.getSpecialBlockTokenMap("eTH"); thermTokenMap != null;
                thermTokenMap = tokenMap.getSpecialBlockTokenMap("eTH")) {
            int typ = thermTokenMap.readDataLine("typ", -1);
            _allSheetComponents.add(AbstractTypeInfo.fabricFromFile(CircuitTyp.getFromIntNumber(typ), thermTokenMap));
        }

        for (TokenMap controlTokenMap = tokenMap.getSpecialBlockTokenMap("c");
                controlTokenMap != null; controlTokenMap = tokenMap.getSpecialBlockTokenMap("c")) {
            int typ = controlTokenMap.readDataLine("typ", -1);
            try {
                if (typ == SpecialTyp.TEXTFIELD.getTypeNumber()) { // this is for backwards-compatiblity before v 1.62
                    _allSheetComponents.add(AbstractTypeInfo.fabricFromFile(SpecialTyp.getFromIntNumber(typ), controlTokenMap));
                } else {
                    _allSheetComponents.add(AbstractTypeInfo.fabricFromFile(ControlTyp.getFromIntNumber(typ), controlTokenMap));
                }

            } catch (Exception ex) {
                System.err.println("Error! Could not create control block with id: " + typ);
                ex.printStackTrace();
            }
        }

        for (TokenMap specialTokenMap = tokenMap.getSpecialBlockTokenMap("sp");
                specialTokenMap != null; specialTokenMap = tokenMap.getSpecialBlockTokenMap("sp")) {
            int typ = specialTokenMap.readDataLine("typ", -1);
            try {
                AbstractBlockInterface newBlock = AbstractTypeInfo.fabricFromFile(SpecialTyp.getFromIntNumber(typ), specialTokenMap);
                _allSheetComponents.add(newBlock);

                if (newBlock instanceof SubcircuitBlock) {
                    allSubCircuitBlocks.add((SubcircuitBlock) newBlock);
                }
            } catch (Exception ex) {
                System.err.println("Error! Could not create control block with id: " + typ);
                ex.printStackTrace();
            }

        }

        // the GeckoFile-Object are created after the single block creation!
        TokenMap fileBlock = tokenMap.getSpecialBlockTokenMap("GeckoFileManager");        
        if (fileBlock != null) {
            fileMgrFiles = new ArrayList<GeckoFile>();
            for (TokenMap fileSubBlock = fileBlock.getBlockTokenMap("<GeckoFile>"); fileSubBlock != null;
                    fileSubBlock = fileBlock.getBlockTokenMap("<GeckoFile>")) {
                try {
                    GeckoFile newFile = new GeckoFile(fileSubBlock, _allSheetComponents);                    
                    fileMgrFiles.add(newFile);
                } catch (FileNotFoundException e) {
                    JOptionPane.showMessageDialog(null, e.getMessage(), "ERROR: File not found", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        long toc = System.currentTimeMillis();        
        return uniqueFileId;
    }

    // Hilfsfunktionen:
    // Daten werden in einen ASCII-String umgeschrieben -->
    public static void appendAsString(StringBuffer ascii, int wert) {
        ascii.append(' ');
        ascii.append(wert);
    }

    public static void appendAsString(StringBuffer ascii, double wert) {
        ascii.append(' ');
        ascii.append(wert);
    }

    public static void appendAsString(StringBuffer ascii, long wert) {
        ascii.append(' ');
        ascii.append(wert);
    }

    public static void appendAsString(StringBuffer ascii, boolean wert) {
        ascii.append(' ');
        ascii.append(wert);
    }

    public static void appendAsString(StringBuffer ascii, HiLoData wert) {
        ascii.append(' ');
        ascii.append(wert._yLo);
        ascii.append(' ');
        ascii.append(wert._yHi);
    }

    public static void appendAsString(StringBuffer ascii, String wert) {
        if (wert.equals("")) {
            ascii.append(" " + NIX);
        } else {
            ascii.append(' ');
            ascii.append(wert);
        }
    }

    public static void appendAsString(StringBuffer ascii, byte[] wert) {
        ascii.append("[] ");
        if (wert == null) {
            ascii.append("null");
        } else {
            for (int i1 = 0; i1 < wert.length; i1++) {
                ascii.append(wert[i1]);
                ascii.append(' ');
            }
        }
    }

    public static void appendAsString(StringBuffer ascii, int[] wert) {
        ascii.append("[] ");
        if (wert == null) {
            ascii.append("null");
        } else {
            for (int i1 = 0; i1 < wert.length; i1++) {
                ascii.append(wert[i1]);
                ascii.append(' ');
            }
        }
    }

    public static void appendAsString(StringBuffer ascii, long[] wert) {
        ascii.append("[] ");
        if (wert == null) {
            ascii.append("null");
        } else {
            for (int i1 = 0; i1 < wert.length; i1++) {
                ascii.append(wert[i1]);
                ascii.append(' ');
            }
        }
    }

    public static void appendAsString(StringBuffer ascii, Long[] wert) {
        ascii.append("[] ");
        if (wert == null) {
            ascii.append("null");
        } else {
            for (int i1 = 0; i1 < wert.length; i1++) {
                ascii.append(wert[i1]);
                ascii.append(' ');
            }
        }
    }
        

    
    public static void appendAsString(StringBuffer ascii, List<? extends Object> wert) {
        if(wert.size() > 0 && wert.get(0) instanceof String) {
            appendStringArray(ascii, (List<String>) wert);
            return;
        }
        ascii.append("[] ");
        if (wert == null) {
            ascii.append("null");
        } else {
            for (Object value : wert) {
                ascii.append(value);
                ascii.append(' ');
            }
        }
    }
    
    public static void appendStringArray(StringBuffer ascii, List<String> wert) {
        try {
            ascii.append("[]");
            if (wert == null) {
                ascii.append(" null");
            } else {
                ascii.append(" ");
                for (String item : wert) {
                    ascii.append(SEPARATOR_ASCII_STRINGARRAY + (item.trim().equals("") ? NIX : item));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    

    public static void appendAsString(StringBuffer ascii, double[] wert) {
        ascii.append("[] ");
        if (wert == null) {
            ascii.append("null");
        } else {
            for (int i1 = 0; i1 < wert.length; i1++) {
                ascii.append(wert[i1]);
                ascii.append(' ');
            }
        }
    }

    public static void appendAsString(StringBuffer ascii, boolean[] wert) {
        ascii.append("[] ");
        if (wert == null) {
            ascii.append("null");
        } else {
            for (int i1 = 0; i1 < wert.length; i1++) {
                ascii.append(wert[i1]);
                ascii.append(' ');
            }
        }
    }

    public static void appendAsString(StringBuffer ascii, String[] wert) {
        try {
            ascii.append("[]");
            if (wert == null) {
                ascii.append(" null");
            } else {
                ascii.append(" ");
                for (int i1 = 0; i1 < wert.length; i1++) {
                    ascii.append(SEPARATOR_ASCII_STRINGARRAY + (wert[i1].trim().equals("") ? NIX : wert[i1]));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void appendAsString(StringBuffer ascii, int[][] wert) {
        ascii.append("[][] " + wert.length + " " + wert[0].length);
        if (wert == null) {
            ascii.append(" null");
        } else {
            for (int i1 = 0; i1 < wert.length; i1++) {
                for (int i2 = 0; i2 < wert[0].length; i2++) {
                    ascii.append(' ');
                    ascii.append(wert[i1][i2]);
                }
            }
        }
    }

    public static void appendAsString(StringBuffer ascii, double[][] wert) {
        ascii.append("[][] " + wert.length + " " + wert[0].length);
        if (wert == null) {
            ascii.append(" null");
        } else {
            for (int i1 = 0; i1 < wert.length; i1++) {
                for (int i2 = 0; i2 < wert[0].length; i2++) {
                    ascii.append(' ');
                    ascii.append(wert[i1][i2]);
                }
            }
        }
    }

    public static void appendAsString(StringBuffer ascii, boolean[][] wert) {
        ascii.append("[][] " + wert.length + " " + wert[0].length);
        if (wert == null) {
            ascii.append(" null");
        } else {
            for (int i1 = 0; i1 < wert.length; i1++) {
                for (int i2 = 0; i2 < wert[0].length; i2++) {
                    ascii.append(' ');
                    ascii.append(wert[i1][i2]);
                }
            }
        }
    }

    public static void appendAsTextBlock(StringBuffer ascii, final String wert) {
        if (wert.equals("")) {
            ascii.append(" " + NIX);
            return;
        } else {
            String singleLine = wert.replaceAll("\n", "\\\\n");
            ascii.append(' ');
            ascii.append(singleLine);
        }
    }

    // ************************************************************************************
    // Hilfsfunktionen:
    // Aenderungen in relativen und absoluten Pfaden werden beruecksichtigt -->
    // datnamAbsolutIPES   ... neuer aktueller absoluter pfad+name der *.ipes-Datei, die die Topologie/Schaltungssimulation enthaelt
    // datnamAbsLoadIPES   ... gespeicherter pfad+name der *.ipes-Datei, die die Topologie/Schaltungssimulation enthaelt
    // datnamAbsLoadDETAIL ... gespeicherter pfad+name einer Datei, die Zusatzinfo enhaelt, zB. Halbleiter-Info oder Ersatzmodelle wie TH_MODUL
    // return-String       ... neuer aktueller absoluter pfad+name der Datei mit den Zusatzinfos
    //
    @Deprecated
    public static String lokalisiereRelativenPfad(String datnamAbsolutIPES, String datnamAbsLoadDETAIL) {

        String datnamAbsLoadIPES = GlobalFilePathes.datnamAbsLoadIPES;
        //-------------------------
        // (1) Ist die Pfadstruktur unveraendert? Kann man den alten (gespeicherten) absoluten Pfad der Zusatzdatei verwenden?
        if ((new File(datnamAbsLoadDETAIL)).exists()) {
            return datnamAbsLoadDETAIL;
        }
        //-------------------------
        // (2) Die alte Datei 'datnamAbsLoadDETAIL' wurde nicht gefunden.
        // Hat sich die Pfadstruktur geaendert und muss der neue Pfad gefunden werden?
        // (a) Durchsuche die Unterverzeichnisses von *.ipes -->
        try {
            if (File.separatorChar == '/' && datnamAbsLoadDETAIL.contains("\\")) {
                datnamAbsLoadDETAIL = datnamAbsLoadDETAIL.replace('\\', '/');
            }

            if (File.separatorChar == '/' && datnamAbsLoadIPES.contains("\\")) {
                datnamAbsLoadIPES = datnamAbsLoadIPES.replace('\\', '/');
            }
            String localRootAlt = (new File(datnamAbsLoadIPES)).getParent();
            String localRootNeu = (new File(datnamAbsolutIPES)).getParent();
            String relativerPfadDETAIL = datnamAbsLoadDETAIL.substring(localRootAlt.length());
            String neuerPfadDETAIL = localRootNeu + relativerPfadDETAIL;

            if ((new File(neuerPfadDETAIL)).exists()) {
                return neuerPfadDETAIL;
            }
        } catch (Exception e) {
        }
        return new String(GlobalFilePathes.DATNAM_NOT_DEFINED);
    }

    /**
     * if the same model is imported several times, the unique identifiers are
     * not unique, anymore. Therefore, we shift all identifiers by a constant,
     * as well as component references.
     */
    public void shiftComponentReferences() {
        Random generator = new Random(System.currentTimeMillis());

        long shiftValue = generator.nextLong();

        for (AbstractCircuitSheetComponent comp : _allSheetComponents) {
            comp.shiftAllIdentifiers(shiftValue);
        }
    }

    void updateSolverSettings(final SolverSettings solverSettings) {
        solverSettings.dt.setValueWithoutUndo(_dt);
        solverSettings._tDURATION.setValueWithoutUndo(_tDURATION);
        solverSettings._T_pre.setValueWithoutUndo(_T_pre);
        solverSettings._dt_pre.setValueWithoutUndo(_dt_pre);
        solverSettings.SOLVER_TYPE.setValueWithoutUndo(SolverType.getFromOldGeckoIndex(solverType));
        solverSettings._tPAUSE.setValueWithoutUndo(_tPAUSE);
    }
}
