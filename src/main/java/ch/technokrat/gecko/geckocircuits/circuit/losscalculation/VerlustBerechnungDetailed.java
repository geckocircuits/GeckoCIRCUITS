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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFile;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import ch.technokrat.gecko.geckocircuits.circuit.GeckoFileable;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public final class VerlustBerechnungDetailed implements GeckoFileable, AbstractLossCalculatorFabric {

    final AbstractCircuitBlockInterface _parent;
    private final LossProperties _lossParent;
    private DetailedLossLookupTable _onLossesLookupTable;
    private DetailedLossLookupTable _offLossesLookupTable;
    
    private DetailedLossLookupTable _conductionTable;
    
    // Name der Datei (inkl. Pfad), die die Detail-Daten, d.h. Messkurven der Leit- und Schaltverluste enthaelt
    private String datnamGemesseneVerluste = GlobalFilePathes.DATNAM_NOT_DEFINED;
    private long lossFileHashValue = 0;
    public GeckoFile lossFile = null;
    //
    private List<SwitchingLossCurve> _messkurvePvSWITCH = new ArrayList<SwitchingLossCurve>();
    public final List<LeitverlusteMesskurve> _messkurvePvCOND = new ArrayList<LeitverlusteMesskurve>();
        
    public VerlustBerechnungDetailed(final AbstractCircuitBlockInterface parent, final LossProperties lossParent) {
        _parent = parent;
        _lossParent = lossParent;

        SwitchingLossCurve initSwitch25 = new SwitchingLossCurve(25, 400);
        _messkurvePvSWITCH.add(initSwitch25);  // Tj=25°C, Ub=400V
        initSwitch25.setCurveData(new double[][]{{0, 10, 12}, {0, 10e-3, 12e-3}, {0, 5e-3, 7e-3}});

        SwitchingLossCurve initSwitch110 = new SwitchingLossCurve(110, 400);  // Tj=110°C, Ub=400V;
        _messkurvePvSWITCH.add(initSwitch110);
        initSwitch110.setCurveData(new double[][]{{0, 5, 9}, {0, 6e-3, 12e-3}, {0, 4e-3, 7e-3}});
        _onLossesLookupTable = DetailedLossLookupTable.fabric(_messkurvePvSWITCH, 1);
        _offLossesLookupTable = DetailedLossLookupTable.fabric(_messkurvePvSWITCH, 2);

        LeitverlusteMesskurve initCurve25 = new LeitverlusteMesskurve(25);
        _messkurvePvCOND.add(initCurve25);  // Tj=25°C
        initCurve25.setCurveData(new double[][]{{0, 0.7, 1.7}, {0, 0.5, 7}});

        LeitverlusteMesskurve initCurve115 = new LeitverlusteMesskurve(115);  // Tj=115°C
        _messkurvePvCOND.add(initCurve115);
        initCurve115.setCurveData(new double[][]{{0, 0.8, 2.1}, {0, 0.6, 8}});        
        _conductionTable = DetailedLossLookupTable.fabric(_messkurvePvCOND, 1);
    }

    public AbstractLossCalculator lossCalculatorFabric() {
        return new LossCalculatorDetailed(_parent);
    }

    public List<LeitverlusteMesskurve> getCopyOfLeitverlusteMesskurvenArray() {
        List<LeitverlusteMesskurve> returnValue = new ArrayList<LeitverlusteMesskurve>();
        // herausgegeben wird ein Kopie, damit man mit 'Cancel' im uebergeordneten Fenster die Aenderungen nicht zwingend uebernehmen muss -->        
        for (LeitverlusteMesskurve toCopy : _messkurvePvCOND) {
            LeitverlusteMesskurve theCopy = new LeitverlusteMesskurve(toCopy.tj.getValue());
            double[][] dataCopy = new double[toCopy.getCurveData().length][toCopy.getCurveData()[0].length];
            for (int i2 = 0; i2 < dataCopy.length; i2++) {
                for (int i3 = 0; i3 < dataCopy[0].length; i3++) {
                    dataCopy[i2][i3] = toCopy.getCurveData()[i2][i3];
                }
            }
            theCopy.setCurveData(dataCopy);
            returnValue.add(theCopy);
        }
        return returnValue;
    }

    // herausgegeben wird ein Kopie, damit man mit 'Cancel' im uebergeordneten Fenster die Aenderungen nicht zwingend uebernehmen muss -->
    public List<SwitchingLossCurve> getCopyOfSchaltverlusteMesskurvenArray() {
        List<SwitchingLossCurve> returnValue = new ArrayList<SwitchingLossCurve>();
        for (int i1 = 0; i1 < _messkurvePvSWITCH.size(); i1++) {
            SwitchingLossCurve curveCopy = new SwitchingLossCurve(_messkurvePvSWITCH.get(i1).tj.getValue(),
                    _messkurvePvSWITCH.get(i1)._uBlock.getValue());
            returnValue.add(curveCopy);
            double[][] dataCopy = new double[_messkurvePvSWITCH.get(i1).getCurveData().length][_messkurvePvSWITCH.get(i1).getCurveData()[0].length];
            for (int i2 = 0; i2 < dataCopy.length; i2++) {
                for (int i3 = 0; i3 < dataCopy[0].length; i3++) {
                    dataCopy[i2][i3] = _messkurvePvSWITCH.get(i1).getCurveData()[i2][i3];
                }
            }
            curveCopy.setCurveData(dataCopy);
        }
        return returnValue;
    }

    public void copyPropertiesFrom(VerlustBerechnungDetailed origLosses) {
        datnamGemesseneVerluste = origLosses.datnamGemesseneVerluste;
        _onLossesLookupTable = DetailedLossLookupTable.fabric(origLosses._messkurvePvSWITCH, 1);

        lossFile = origLosses.lossFile;

        for (SwitchingLossCurve origCurve : origLosses._messkurvePvSWITCH) {
            _messkurvePvSWITCH.add(origCurve.copy());
        }
        _offLossesLookupTable = DetailedLossLookupTable.fabric(origLosses._messkurvePvSWITCH, 2);

        for (LeitverlusteMesskurve toCopy : origLosses._messkurvePvCOND) {
            _messkurvePvCOND.add(toCopy.copy());
        }
        _conductionTable = DetailedLossLookupTable.fabric(_messkurvePvCOND, 1);
                        
    }

    public void importASCII(TokenMap tokenMap) {
        if (tokenMap.containsToken("lossFileHashValue")) {
            lossFileHashValue = tokenMap.readDataLine("lossFileHashValue", lossFileHashValue);
        }                
        
        datnamGemesseneVerluste = tokenMap.readDataLine("datnamGemesseneVerluste", datnamGemesseneVerluste);

        
        try {
            // Relative Pfadangaben pruefen und gegebenfalls aktualisieren:
            if (!datnamGemesseneVerluste.equals(GlobalFilePathes.DATNAM_NOT_DEFINED)) {
                String aktualisierterPfad = DatenSpeicher.lokalisiereRelativenPfad(GlobalFilePathes.DATNAM, datnamGemesseneVerluste);
                datnamGemesseneVerluste = aktualisierterPfad;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void exportASCII(StringBuffer ascii) {
        DatenSpeicher.appendAsString(ascii.append("\ndatnamGemesseneVerluste"), datnamGemesseneVerluste);
        if (lossFile != null) {
            DatenSpeicher.appendAsString(ascii.append("\nlossFileHashValue"), lossFile.getHashValue());
        } else {
            DatenSpeicher.appendAsString(ascii.append("\nlossFileHashValue"), lossFileHashValue);
        }
    }

    public void removeLossFile() {
        if (lossFile != null) {
            lossFile.removeUser(_parent.getUniqueObjectIdentifier());
            Fenster._fileManager.maintain(lossFile);
        }

        datnamGemesseneVerluste = GlobalFilePathes.DATNAM_NOT_DEFINED;
        lossFile = null;
    }

    public boolean leseDetailVerlusteVonDatei(final GeckoFile newLossFile) {        
        //------------------
        // Datei einlesen -->
        List<String> datVec = new ArrayList<String>();
        //
        // GZIP-Format (March 2009) - ganz neu! --> 
        try {
            GZIPInputStream in1 = new GZIPInputStream(newLossFile.getInputStream());
            /*
             * if (Fenster.IS_APPLET) { in1 = new GZIPInputStream((new URL(GeckoSim.urlApplet, fyomu)).openStream()); } else { in1 =
             * new GZIPInputStream(new FileInputStream(fyomu)); }
             */
            BufferedReader in = new BufferedReader(new InputStreamReader(in1));
            String z = null;
            while ((z = in.readLine()) != null) {
                datVec.add(z);
            }
            in.close();
        } catch (Exception e0) {
            // neue gezipte Version -->
            try {
                InflaterInputStream in1 = new InflaterInputStream(newLossFile.getInputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(in1));
                String z = null;
                while ((z = in.readLine()) != null) {
                    datVec.add(z);
                }
                in.close();
            } catch (Exception e) {
                // Fehler, vielleicht ist es eine alte Version in reinem ASCII -->
                try {
                    BufferedReader in = newLossFile.getBufferedReader(); //new BufferedReader(new FileReader(fyomu));
                    String z = null;
                    while ((z = in.readLine()) != null) {
                        datVec.add(z);
                    }
                    in.close();
                } catch (Exception e2) {
                    return false;
                }
            }
        }
        //
        String[] ascii = new String[datVec.size()];
        for (int i1 = 0; i1 < datVec.size(); i1++) {
            ascii[i1] = (String) datVec.get(i1);
        }
                
        final TokenMap tokenMap = new TokenMap(ascii);
        StringTokenizer stk = null;
        //------------------
        
        _messkurvePvCOND.clear();
        //------------------        
        boolean needClearSwitchCurves = true;        
        
        for (TokenMap lossBlock = tokenMap.getBlockTokenMap("<SchaltverlusteMesskurve>"); lossBlock != null;
                lossBlock = tokenMap.getBlockTokenMap("<SchaltverlusteMesskurve>")) {            
            SwitchingLossCurve newCurve = new SwitchingLossCurve(-1, -1);
            if(needClearSwitchCurves) {
                _messkurvePvSWITCH.clear();
                needClearSwitchCurves = false;
            }
            _messkurvePvSWITCH.add(newCurve);
            newCurve.importASCII(lossBlock);  // Initialisierung der entsprechenden gemessenen Kurve
        }

        _onLossesLookupTable = DetailedLossLookupTable.fabric(_messkurvePvSWITCH, 1);
        _offLossesLookupTable = DetailedLossLookupTable.fabric(_messkurvePvSWITCH, 2);
        
        for (TokenMap lossBlock = tokenMap.getBlockTokenMap("<LeitverlusteMesskurve>"); lossBlock != null;
                lossBlock = tokenMap.getBlockTokenMap("<LeitverlusteMesskurve>")) {
            LeitverlusteMesskurve newCurve = new LeitverlusteMesskurve(-1);
            _messkurvePvCOND.add(newCurve);
            newCurve.importASCII(lossBlock);  // Initialisierung der entsprechenden gemessenen Kurve            
        }
        
        _conductionTable = DetailedLossLookupTable.fabric(_messkurvePvCOND, 1);

        datnamGemesseneVerluste = newLossFile.getCurrentAbsolutePath();
        //remove old loss file, set new
        if (lossFile != null) {
            lossFile.removeUser(_parent.getUniqueObjectIdentifier());
            Fenster._fileManager.maintain(lossFile);
        }
        newLossFile.setUser(_parent.getUniqueObjectIdentifier());
        lossFile = newLossFile;
        Fenster._fileManager.addFile(lossFile);
                
        return true;
    }

    public void initLossFile() {
        if (lossFileHashValue != 0) {            
            try {                
                GeckoFile detailedLossFile = Fenster._fileManager.getFile(lossFileHashValue);
                leseDetailVerlusteVonDatei(detailedLossFile);
            } catch (FileNotFoundException e) {
                //this means this is probably an old .ipes file without a valid hash key for the GeckoFile (i.e. saved in an older version)
                //here try to recover from the old file name                
                readLossesFromFileAndSetDetailedLossType(datnamGemesseneVerluste);
            }
        }
    }

    public void readLossesFromFileAndSetDetailedLossType(final String fyomu) {
        //here we are passed a String file path
        //generate GeckoFile object from this path, then pass to method below
        GeckoFile file;
        //first assume given path is absolute
        try {
            file = new GeckoFile(new File(fyomu), GeckoFile.StorageType.INTERNAL, Fenster.getOpenFileName());
            System.out.println("try to load file " + file);
            leseDetailVerlusteVonDatei(file);
            _lossParent._lossType.setValueWithoutUndo(LossCalculationDetail.DETAILED);
        } catch (FileNotFoundException e) { //if not, see if it is a relative path to the .ipes file location
            String modelFilePath = Fenster.getOpenFileName();
            if(!modelFilePath.equals("Untitled")) {
                modelFilePath = modelFilePath.substring(0, modelFilePath.lastIndexOf(File.separator));
            String newPath = modelFilePath + File.separator + fyomu;
            try {
                file = new GeckoFile(new File(newPath), GeckoFile.StorageType.INTERNAL, Fenster.getOpenFileName());
                _lossParent._lossType.setValueWithoutUndo(LossCalculationDetail.SIMPLE);
            } catch (FileNotFoundException e2) {
                System.err.println("Loss file " + fyomu + " for component " + _parent.getStringID() + " not found!");
            }
            }            

        }
    }

    public boolean schreibeDetailVerlusteAufDatei(final String fkaku, final List<SwitchingLossCurve> messkurvePvSWITCH,
            final List<LeitverlusteMesskurve> messkurvePvCOND, final GeckoFile.StorageType storageType) {
        //added boolean flag "external" - true if losses always looked up from external file, 
        // false if loss file is to be saved with .ipes file

        StringBuffer ascii = new StringBuffer();
        DatenSpeicher.appendAsString(ascii.append("\nanzMesskurvenPvSWITCH"), messkurvePvSWITCH.size());

        for (SwitchingLossCurve curve : messkurvePvSWITCH) {
            curve.exportASCII(ascii);
        }

        DatenSpeicher.appendAsString(ascii.append("\nanzMesskurvenPvCOND"), messkurvePvCOND.size());
        for (LeitverlusteMesskurve curve : messkurvePvCOND) {
            curve.exportASCII(ascii);
        }
        GeckoFile newLossFile = null;
        
        //
        try {
            // jetzt wird 'ascii' in eine Datei geschrieben -->
            //
            // Plain-Test Variante in ASCII --> 
            /// BufferedWriter out= new BufferedWriter(new FileWriter(fkaku));
            //--------
            File lossesFile;
            if (fkaku == null) {
                lossesFile = new File(datnamGemesseneVerluste);
            } else {
                lossesFile = new File(fkaku);
            }
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(lossesFile)));
            //
            out.write(ascii.toString());
            out.flush();
            out.close();
            //blocks using this loss file can also see the changes
            newLossFile = new GeckoFile(lossesFile, storageType, Fenster.getOpenFileName());
            newLossFile.setUser(_parent.getUniqueObjectIdentifier());
            datnamGemesseneVerluste = fkaku;
        } catch (Exception e) {
            return false;
        }
        if (newLossFile != null) { //means new loss file was successfully generated
            //check first if a loss file already exists, and remove it if it does
            if (lossFile != null) {
                lossFile.removeUser(_parent.getUniqueObjectIdentifier());
                Fenster._fileManager.maintain(lossFile);
            }
            lossFile = newLossFile;
            Fenster._fileManager.addFile(lossFile);
        }

        return true;
    }

    /**
     *
     * @return String to be displayed in circuit sheet (file name). When link
     * could not be found, return null!
     */
    public boolean pruefeLinkAufHalbleiterDatei() {

        if (Fenster.IS_APPLET) {
            return false;
        }

        if (lossFile == null) {
            return false;
        }

        if (lossFile.getStorageType() == GeckoFile.StorageType.EXTERNAL) {
            return new File(lossFile.getCurrentAbsolutePath()).exists();
        } else {
            for (GeckoFile gFile : Fenster._fileManager.getFilesByExtension(".scl")) {
                if (gFile.getCurrentAbsolutePath().equals(lossFile.getCurrentAbsolutePath())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setzeNeueParameter(final List<SwitchingLossCurve> messkurvePvSWITCH,
            final List<LeitverlusteMesskurve> messkurvePvCOND) {
        _messkurvePvSWITCH.clear();
        _messkurvePvSWITCH.addAll(messkurvePvSWITCH);
        _messkurvePvCOND.clear();
        _messkurvePvCOND.addAll(messkurvePvCOND);
        _onLossesLookupTable = DetailedLossLookupTable.fabric(messkurvePvSWITCH, 1);
        _offLossesLookupTable = DetailedLossLookupTable.fabric(messkurvePvSWITCH, 2);
        _conductionTable = DetailedLossLookupTable.fabric(messkurvePvCOND, 1);
    }

    @Override
    public void initExtraFiles() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addFiles(List<GeckoFile> newFilesToAdd) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<GeckoFile> getFiles() {
        return Arrays.asList(lossFile);
    }

    @Override
    public void removeLocalComponentFiles(final List<GeckoFile> filesToRemove) {
        assert false : "Remove files cannot be called here. Loss components"
                + "have a loss file as single selection!";
    }

    public final class LossCalculatorDetailed extends AbstractLossCalculatorSwitch {

        /**
         * the curves reference voltage is rescaled to this voltage before the
         * Lookup-table.
         */
        public static final double DEFAULT_REFERENCE_VOLTAGE = 100;
        private static final double LARGE_TEMPERATURE = 1E4;
        private double _temperature;

        public LossCalculatorDetailed(final AbstractCircuitBlockInterface parent) {
            super(parent);
        }

        @Override
        public void calcLosses(final double current, final double temperature, final double deltaT) {
            if (temperature > LARGE_TEMPERATURE || temperature != temperature) {
                // when no thermal model is connected, we set the temperature to some default value.
                _temperature = DEFAULT_REFERENCE_VOLTAGE;
            } else {
                _temperature = temperature;
            }            
            super.calcLosses(current, temperature, deltaT);
        }

        @Override
        double calcConductionLoss() {          
            return _conductionTable.getInterpolatedXValue(_temperature, _current) * _current;                        
        }

        @Override
        double calcTurnOnSwitchingLoss() {
            return _onLossesLookupTable.getInterpolatedYValue(_temperature, _current) / _deltaT;                        
        }

        @Override
        double calcTurnOffSwitchingLoss() {
            return _offLossesLookupTable.getInterpolatedYValue(_temperature, _oldCurrent) / _deltaT;                                    
        }

        @Override
        double calculateRelativeVoltageFactor(final double appliedVoltage) {
            return Math.abs(appliedVoltage);
        }
    }
}