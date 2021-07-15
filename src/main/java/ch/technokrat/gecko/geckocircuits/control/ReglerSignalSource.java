/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.allg.*;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.ControlSourceType;
import ch.technokrat.gecko.geckocircuits.circuit.GeckoFileable;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalControlInput;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractSignalCalculator;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractSignalCalculatorPeriodic;
import ch.technokrat.gecko.geckocircuits.control.calculators.SignalCalculatorExternalWrapper;
import ch.technokrat.gecko.geckocircuits.control.calculators.SignalCalculatorImport;
import ch.technokrat.gecko.geckocircuits.control.calculators.SignalCalculatorRandom;
import ch.technokrat.gecko.geckocircuits.control.calculators.SignalCalculatorRectangle;
import ch.technokrat.gecko.geckocircuits.control.calculators.SignalCalculatorSinus;
import ch.technokrat.gecko.geckocircuits.control.calculators.SignalCalculatorTriangle;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.FontRenderContext;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class ReglerSignalSource extends RegelBlock implements ControlInputTwoTerminalStateable,
        GeckoFileable, Operationable {

    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerSignalSource.class, "SIGNAL", I18nKeys.SIGNAL_SOURCE);

    private static final String PHASE = "phase";
    private static final int IN_TERM_NUMBER_EXTERNAL = 5;
    private static final int IN_TERM_NUMBER_NORMAL = 0;
    private static final int BLOCK_WIDTH = 3;
    final UserParameter<ControlSourceType> _typQuelle = UserParameter.Builder.
            <ControlSourceType>start("typQuelle", ControlSourceType.QUELLE_RECHTECK).
            longName(I18nKeys.TYPE_OF_SIGNAL_SOURCE).
            shortName("type").
            arrayIndex(this, 0).
            build();
    private static final double DEFAULT_AMPLITUDE = 10.0;
    final UserParameter<Double> _amplitudeAC = UserParameter.Builder.
            <Double>start("amplitudeAC", DEFAULT_AMPLITUDE).
            longName(I18nKeys.PEAK_AMPLITUDE).
            shortName("amplMAX").
            arrayIndex(this, 1).
            build();
    private static final double DEFAULT_FREQUENCY = 50.0;
    final UserParameter<Double> _frequency = UserParameter.Builder.
            <Double>start("frequenz", DEFAULT_FREQUENCY).
            longName(I18nKeys.FREQUENCY).
            unit("Hz").
            shortName("f").
            arrayIndex(this, 2).
            build();
    private static final int OFFSET_PAR_INDEX = 3;
    final UserParameter<Double> _offsetDC = UserParameter.Builder.
            <Double>start("anteilDC", 0.0).
            longName(I18nKeys.OFFSET_OF_WAVEFORM_FROM_ZERO).
            shortName("offset").
            arrayIndex(this, OFFSET_PAR_INDEX).
            build();
    private static final int PHASE_PAR_INDEX = 4;
    final UserParameter<Double> _phase = UserParameter.Builder.
            <Double>start(PHASE, 0.0).
            longName(I18nKeys.SIGNAL_PHASE_DELAY).
            shortName(PHASE).
            arrayIndex(this, PHASE_PAR_INDEX).
            build();
    private static final int DUTY_PAR_INDEX = 5;
    private static final double DEFAULT_DUTY = 0.5;
    final UserParameter<Double> _dutyRatio = UserParameter.Builder.
            <Double>start("tastverhaeltnis", DEFAULT_DUTY).
            longName(I18nKeys.DUTY_RATIO).
            shortName("d").
            arrayIndex(this, DUTY_PAR_INDEX).
            build();
    private static final int EXTERNAL_PAR_INDEX = 6;
    final UserParameter<Boolean> _useExternal = UserParameter.Builder.
            <Boolean>start("useExternal", false).
            longName(I18nKeys.IF_TRUE_EXTERNAL_TERMINALS).
            shortName("useExternal").
            arrayIndex(this, EXTERNAL_PAR_INDEX).
            build();
    private static final int DISP_PAR_INDEX = 7;
    final UserParameter<Boolean> _displayDetails = UserParameter.Builder.
            <Boolean>start("displayDetails", false).
            longName(I18nKeys.IF_TRUE_MORE_INFORMATION).
            shortName("display").
            arrayIndex(this, DISP_PAR_INDEX).
            build();
    private double[][] _xy;  // Importierter ZV (als ASCII-Datei)
    private String _datnamXY = GlobalFilePathes.DATNAM_NOT_DEFINED;
    // for TRI, RECHT-states we simple store variables 'aufsteigend' and '_dreieck'
    private GeckoFile _externalDataFile = null;
    private long _externalDataFileHashValue = 0;
    private String[] _labelsBeforeFold;
    private Stack<TerminalControlInput> _terminalStack = new Stack<TerminalControlInput>();

    ;

    public ReglerSignalSource() {
        super(0, 1);
        setExpandedParameterListener(_useExternal);
    }

    @Override
    public double getXShift() {
        return 1 / 2.0;
    }

    @Override
    public int getBlockWidth() {
        return BLOCK_WIDTH * dpix;
    }

    @Override
    public int getBlockHeight() {
        return dpix;
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"src"};
    }

    @Override
    protected String getCenteredDrawString() {
        switch (_typQuelle.getValue()) {
            case QUELLE_DREIECK:
                return "TRI";
            case QUELLE_IMPORT:
                return "FILE";
            case QUELLE_RANDOM:
                return "RAND";
            case QUELLE_SIN:
                return "SINE";
            case QUELLE_RECHTECK:
                return "RECT";
            default:
                return "SIGNAL";
        }
    }

    @Override
    public void copyAdditionalParameters(final AbstractBlockInterface originalBlock) {
        super.copyAdditionalParameters(originalBlock);
        final ReglerSignalSource other = (ReglerSignalSource) originalBlock;

        this._datnamXY = other._datnamXY;
        if (!(_datnamXY.equals(GlobalFilePathes.DATNAM_NOT_DEFINED)) && other._externalDataFile != null) {
            final List<GeckoFile> externalFile = new ArrayList<GeckoFile>();
            externalFile.add(other._externalDataFile);
            addFiles(externalFile);
            readExternalDataFromFile();
        }

    }

    public String getDatnam() {
        return _datnamXY;
    }

    private AbstractSignalCalculator fabricSignalCalculator(final ControlSourceType typQuelle) {
        switch (typQuelle) {
            case QUELLE_SIN:
                return new SignalCalculatorSinus(XIN.size(), _amplitudeAC.getValue(), _frequency.getValue(),
                        Math.toRadians(_phase.getValue()), _offsetDC.getValue(), _dutyRatio.getValue());
            case QUELLE_DREIECK:
                return new SignalCalculatorTriangle(XIN.size(), _amplitudeAC.getValue(), _frequency.getValue(),
                        Math.toRadians(_phase.getValue()), _offsetDC.getValue(), _dutyRatio.getValue());
            case QUELLE_RECHTECK:
                return new SignalCalculatorRectangle(XIN.size(), _amplitudeAC.getValue(), _frequency.getValue(),
                        Math.toRadians(_phase.getValue()), _offsetDC.getValue(), _dutyRatio.getValue());
            case QUELLE_RANDOM:
                return new SignalCalculatorRandom();
            case QUELLE_IMPORT:
                readExternalDataFromFile();
                return new SignalCalculatorImport(_xy);
            default:
                assert false;
                throw new IllegalArgumentException(getStringID() + " Signal type not known: " + typQuelle);
        }
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        final AbstractSignalCalculator calculator = fabricSignalCalculator(_typQuelle.getValue());
        if (_useExternal.getValue()) {
            assert calculator instanceof AbstractSignalCalculatorPeriodic;
            return new SignalCalculatorExternalWrapper((AbstractSignalCalculatorPeriodic) calculator);
        } else {
            return calculator;
        }

    }
    private static final int X_OFFSET = 3;
    private static final int Y_SIZE = 6;

    @Override
    public void drawBlockRectangle(final Graphics2D graphics) {
        final int xPos = getSheetPosition().x;
        final int yPos = getSheetPosition().y;
        final Color origColor = graphics.getColor();
        super.drawBlockRectangle(graphics);

        final FontRenderContext frc = graphics.getFontRenderContext();
        if (_useExternal.getValue()) {
            graphics.drawLine((int) (dpix * xPos), (int) (dpix * (yPos + 1 / 2.0)),
                    (int) (dpix * xPos), (int) (dpix * (yPos + Y_SIZE)));
            final int yShift = (int) (graphics.getFont().getStringBounds("xxx", frc).getHeight() * 0.25);
            graphics.setColor(GlobalColors.farbeInBearbeitungCONTROL);
            int strYPos = 2;
            graphics.drawString("ac", (int) (dpix * xPos) + X_OFFSET, (int) (dpix * (yPos + strYPos)) + yShift);
            strYPos++;
            graphics.drawString("f", (int) (dpix * xPos) + X_OFFSET, (int) (dpix * (yPos + strYPos)) + yShift);
            strYPos++;
            graphics.drawString("dc", (int) (dpix * xPos) + X_OFFSET, (int) (dpix * (yPos + strYPos)) + yShift);
            strYPos++;
            graphics.drawString(PHASE, (int) (dpix * xPos) + X_OFFSET, (int) (dpix * (yPos + strYPos)) + yShift);
            strYPos++;
            graphics.drawString("duty", (int) (dpix * xPos) + X_OFFSET, (int) (dpix * (yPos + strYPos)) + yShift);
            graphics.setColor(origColor);
        }

    }

    @Override
    protected void addTextInfoParameters() {
        super.addTextInfoParameters();

        if (isExternalSet()) { // don't show any parameters when they are given from external!
            return;
        }

        if (_typQuelle.getValue() == ControlSourceType.QUELLE_RANDOM) {
            final String typus = "Random";
            _textInfo.addParameter(typus);
        } else if (_typQuelle.getValue() == ControlSourceType.QUELLE_IMPORT) {
            addImportParameters();
        } else {
            if (_displayDetails.getValue()) {
                addDetailedTextInfo();
            } else {
                final StringBuffer typus = new StringBuffer("I");
                switch (_typQuelle.getValue()) {
                    case QUELLE_SIN:
                        typus.append("_sin");
                        break;
                    case QUELLE_RECHTECK:
                        typus.append("_rec");
                        break;
                    case QUELLE_DREIECK:
                        typus.append("_tri");
                        break;
                    default:
                        assert false;
                }
                typus.append((" = " + tcf.formatENG(_amplitudeAC.getValue(), 2)));
                _textInfo.addParameter(typus.toString());
            }
        }
    }

    public void readExternalDataFromFile() {
        if (_externalDataFile == null) {
            throw new GeckoRuntimeException("could not read data file in SIGNAL source block!");
        }
        try {
            
            final BufferedReader bufRead = _externalDataFile.getBufferedReader();//new BufferedReader(new FileReader(datnamXY));
            final List<String> datVec = new ArrayList<String>();            
            
            for (String line = bufRead.readLine(); line != null; line = bufRead.readLine()) {
                datVec.add(line);
            }

            bufRead.close();
            _xy = new double[2][datVec.size()];

            final StringTokenizer stk = new StringTokenizer(datVec.get(0), " ");
            final double tStart = Double.parseDouble(stk.nextToken());  // time t==0
            
            for (int i1 = 0; i1 < _xy[0].length; i1++) {                
                final String lineSTring = datVec.get(i1);                
                final StringTokenizer tokenizer = new StringTokenizer(lineSTring, " ");                
                _xy[0][i1] = Double.parseDouble(tokenizer.nextToken()) - tStart;  // time (verschoben, sodass Zeit bei 0 beginnt)
                _xy[1][i1] = Double.parseDouble(tokenizer.nextToken());      // value
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            Logger.getLogger(ReglerSignalSource.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
            final String errorMessage = "External data file not formatted properly in " + getStringID() + "\n" + nfe.getMessage();
            final String errorTitle = getStringID() + ": ERROR - Number format exception";
            JOptionPane.showMessageDialog(null, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addImportParameters() {
        final String typus = "Import-Data";
        _textInfo.addParameter(typus);

        if (_datnamXY.equals(GlobalFilePathes.DATNAM_NOT_DEFINED)) {
            _textInfo.addErrorValue(typus);
        } else {
            String txt;
            final int index = _datnamXY.lastIndexOf(System.getProperty("file.separator"));
            if (index == -1) {
                txt = _datnamXY;
            } else {
                txt = _datnamXY.substring(index + 1);
            }
            _textInfo.addParameter(txt);
        }
    }

    @Override
    protected void exportAsciiIndividual(final StringBuffer ascii) {
        DatenSpeicher.appendAsString(ascii.append("\ndatnamXY"), _datnamXY);
        if (_externalDataFile == null) {
            DatenSpeicher.appendAsString(ascii.append("\nexternalDataFileHashValue"), _externalDataFileHashValue);
        } else {
            DatenSpeicher.appendAsString(ascii.append("\nexternalDataFileHashValue"), _externalDataFile.getHashValue());
        }

        DatenSpeicher.appendAsString(ascii.append("\nversion170"), 1);

    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        final String lineString = tokenMap.getLineString("datnamXY", "");
        _datnamXY = lineString.substring("datnamXY ".length());  // wichtig, bei Pfadname Leerzeichen

        if (tokenMap.containsToken("externalDataFileHashValue")) {
            _externalDataFileHashValue = tokenMap.readDataLine("externalDataFileHashValue", _externalDataFileHashValue);
        }

        if (!tokenMap.containsToken("version170")) { // repair from version 1.62 to later version: phase 
            // was changed from radians to degrees!
            _phase.setValueWithoutUndo(Math.toDegrees(_phase.getValue()));
        }
    }

    @Override
    public void initExtraFiles() {
        if (!_datnamXY.equals(GlobalFilePathes.DATNAM_NOT_DEFINED)) {
            //first check if this is a file loaded from previous versions
            try {
                _externalDataFile = Fenster._fileManager.getFile(_externalDataFileHashValue);                                                
            } catch (FileNotFoundException e) {
                final String errorMessage = "External data file missing in signal source "
                        + getStringID() + ":\n" + e.getMessage();
                final String errorTitle = getStringID() + ": ERROR - File not found!";
                JOptionPane.showMessageDialog(null, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);                
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<GeckoFile> getFiles() {
        final List<GeckoFile> externalFile = new ArrayList<GeckoFile>();

        if (_externalDataFile != null) {
            externalFile.add(_externalDataFile);
        }

        return externalFile;
    }

    @Override
    public void addFiles(final List<GeckoFile> newFiles) {
        //should be only one file, therefore take first one
        //check first if file already exists, and remove it
        if (_externalDataFile != null) {
            _externalDataFile.removeUser(getUniqueObjectIdentifier());
            Fenster._fileManager.maintain(_externalDataFile);
        }
        _externalDataFile = newFiles.get(0);
        _externalDataFile.setUser(getUniqueObjectIdentifier());
        Fenster._fileManager.addFile(_externalDataFile);
        _datnamXY = _externalDataFile.getCurrentAbsolutePath();
    }

    @SuppressWarnings("PMD")
    @Override
    public void removeLocalComponentFiles(final List<GeckoFile> filesToRemove) {
        //since there is only one file, we just remove it
        if (!filesToRemove.isEmpty() && _externalDataFile != null) {
            _externalDataFile.removeUser(getUniqueObjectIdentifier());
            Fenster._fileManager.maintain(_externalDataFile);
            _externalDataFile = null;
            _datnamXY = GlobalFilePathes.DATNAM_NOT_DEFINED;
        }
    }

    @Override
    protected Window openDialogWindow() {
        switch (_typQuelle.getValue()) {
            case QUELLE_RANDOM:
                return new ReglerRandomDialog(this);
            case QUELLE_IMPORT:
                return new ReglerImportDialog(this);
            default:
                return new ReglerSignalSourceDialog(this);

        }
    }

    private void addDetailedTextInfo() {
        String typus = null;
        if (_typQuelle.getValue() == ControlSourceType.QUELLE_SIN) {
            typus = "Sin.-Type";
        } else if (_typQuelle.getValue() == ControlSourceType.QUELLE_RECHTECK) {
            typus = "Rect.-Type";
        } else if (_typQuelle.getValue() == ControlSourceType.QUELLE_DREIECK) {
            typus = "Tri.-Type";
        }
        _textInfo.addParameter(typus);
        _textInfo.addParameter("I= " + tcf.formatENG(_amplitudeAC.getValue(), DISP_DIGITS));
        _textInfo.addParameter("f= " + tcf.formatENG(_frequency.getValue(), DISP_DIGITS));
        _textInfo.addParameter("offset= " + tcf.formatENG(_offsetDC.getValue(), DISP_DIGITS));
        final double degreesPhase = _phase.getValue();
        _textInfo.addParameter("phase= " + tcf.formatENG(Math.round(degreesPhase), DISP_DIGITS));
        if (_typQuelle.getValue() != ControlSourceType.QUELLE_SIN) {
            _textInfo.addParameter("duty= " + tcf.formatENG(_dutyRatio.getValue(), DISP_DIGITS));
        }
    }

    @Override
    public void setFolded() {
        while (XIN.size() > IN_TERM_NUMBER_NORMAL) {
            _terminalStack.push((TerminalControlInput) XIN.pop());
        }
    }

    @Override
    public void setExpanded() {
        for (int i = 0, popSize = _terminalStack.size(); i < popSize; i++) {
            XIN.add(_terminalStack.pop());
        }
        while (XIN.size() < IN_TERM_NUMBER_EXTERNAL) {
            XIN.add(new TerminalControlInput(this, -1, -XIN.size() - 2));
        }
    }

    @Override
    public boolean isExternalSet() {
        return _useExternal.getValue();
    }

    @Override
    public void setExternalUsed(final boolean value) {
        _useExternal.setUserValue(value);
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{getTypeDescription()};
    }

    @Override
    public List<OperationInterface> getOperationEnumInterfaces() {
        final List<OperationInterface> returnValue = new ArrayList<OperationInterface>();

        returnValue.add(new OperationInterface("setFileName", I18nKeys.FILENAME) {
            @Override
            public Object doOperation(final Object parameterValue) {

                try {
                    _externalDataFile = new GeckoFile(new File((String) parameterValue), GeckoFile.StorageType.EXTERNAL, Fenster.getOpenFileName());
                    _externalDataFile.setUser(getUniqueObjectIdentifier());
                    _datnamXY = (String) parameterValue;
                    Fenster._fileManager.addFile(_externalDataFile);
                    return true;
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(ReglerSignalSource.class.getName()).log(Level.SEVERE, null, ex);
                    throw new RuntimeException("File not found! " + ex);
                }
            }
        });
        return Collections.unmodifiableList(returnValue);
    }
}
