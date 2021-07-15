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

import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerNullData;
import ch.technokrat.gecko.geckocircuits.datacontainer.DataContainerScopeWrapper;
import ch.technokrat.gecko.geckocircuits.datacontainer.ScopeWrapperIndices;
import ch.technokrat.gecko.geckocircuits.newscope.*;
import ch.technokrat.gecko.geckoscript.GeckoInvalidArgumentException;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * TODO: please clean anybody up this mess!
 *
 * @author andreas
 */
public final class ReglerOSZI extends RegelBlock implements VariableTerminalNumber,
        SpecialNameVisible {

    private static final int TERM_POS_X = -2;
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerOSZI.class, "SCOPE", I18nKeys.SCOPE, I18nKeys.COMPONENT_FOR_DATA_VISUALIZATION);
    
    final UserParameter<Integer> _inputTerminalNumber = UserParameter.Builder.
            <Integer>start("tn", 0).
            longName(I18nKeys.NO_INPUT_TERMINALS).
            shortName("numberInputTerminals").
            arrayIndex(this, -1).
            build();
    /*
     * Klickbereiche fuer rote Dreiecke --> Aenderung der Terminal-Anzahl
     */
    private int _xKlickMinTerm, _xKlickMaxTerm, _yKlickMinTermADD,
            _yKlickMaxTermADD, _yKlickMinTermSUB, _yKlickMaxTermSUB;
    // fuer Zugriff auf SCOPE und die Moeglichkeit zum Update der Labels wenn Terminal-Anzahl geaendert wird
    // alle ZV-Daten nicht komprimiert fuer eventuelle Festplattenspeicherung --> Speicherkritisch
    private AbstractDataContainer _zvDatenRAM;
    //for use with GeckoSCRIPT - waveform characteristic
    private transient CharacteristicsCalculator _waveformChar;
    private double _charStart = 0;
    private double _charEnd = 1;
    //for use with GeckoSCRIPT - Fourier analysis
    private double[][][] _fourier = null;
    private double _fourStart = 0;
    private double _fourEnd = 1;
    private static final int DELTA = 3;  // Abstand vom roten Dreieck vom SCOPE-Block (nach oben bzw. nach unten)
    private static final int INSIDE_RECT = 2;
    private static final int FOUR_CHAN_DEPTH = 4;
    public static final int DEF_TERM_NUMBER = 3;
    //for reading the correct rows from the global DataContainer
    private ScopeWrapperIndices _scopeWrapperIndices;
    private String[] _saveLoadSignalNames;
    private final ScopeSettings _scopeSettings = new ScopeSettings();  // initiale ScopeSettings definieren   ;    
    private final GraferV4 _grafer = new GraferV4(_scopeSettings);
    public ScopeFrame _scopeFrame = new ScopeFrame(_grafer);
    private boolean _isShowName;
    Stack<AbstractScopeSignal> _scopeInputSignals = new Stack<AbstractScopeSignal>();
    private final DefinedMeanSignals _meanSignals = new DefinedMeanSignals(_scopeInputSignals);
    
    private static final int DIAMETER = 4;
    private static final double HEIGHT = 0.6;
    private static final double WIDTH = 0.5;
    ///* externalSignals test
    private int _testcounter = 0; // a variable used for the signal name when testing external signals
    //*/

    public ReglerOSZI() {
        super(DEF_TERM_NUMBER, 0);
        _inputTerminalNumber.setValueWithoutUndo(DEF_TERM_NUMBER);                        
        
        for (int i = 0; i < DEF_TERM_NUMBER; i++) {
            _scopeInputSignals.push(new ScopeSignalRegular(_scopeInputSignals.size(), this));
        }
        _grafer.getManager().setInputSignals(_scopeInputSignals);
        _meanSignals.setGrafer(_grafer);        
        ///* externalSignals test
        this._testcounter = 0;
        // */                
        _inputTerminalNumber.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setInputTerminalNumber(_inputTerminalNumber.getValue());
            }
        });
        initScope();
    }

    ///* externalSignals test
    ExternalSignal getSin(final double length, final double period, final double weight) {
        double[] times = new double[10000];
        double[] values = new double[10000];
        ExternalSignal returnValue;
        for (int i = 0; i < 10000; i++) {
            times[i] = length / 10000.0 * i;
            values[i] = weight * Math.sin(Math.PI / period * length / 10000.0 * i);
        }
        returnValue = new ExternalSignal("Sig." + this._testcounter, times, values, getSubCircuitPath());
        this._testcounter++;
        return returnValue;
    }

    ExternalSignal getCos(final double length, final double period, final double weight) {
        double[] times = new double[5678];
        double[] values = new double[5678];
        ExternalSignal returnValue;
        for (int i = 0; i < 5678; i++) {
            times[i] = length / 5678.0 * i;
            values[i] = weight * Math.cos(Math.PI / period * length / 5678.0 * i);
        }
        returnValue = new ExternalSignal("TestSignal" + this._testcounter, times, values, getSubCircuitPath());
        this._testcounter++;
        return returnValue;
    }
    //*/

    @Override
    public String[] getOutputNames() {
        return new String[0];
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[0];
    }

    @Override
    protected String getCenteredDrawString() {
        return "";
    }

    public void initScope() {
        // wird einmalig bei der SCOPE-Initialisierung aufgerufen
        // Referenzen fuer SCOPE werden angemeldet ... 
        final DataContainerNullData nullData = new DataContainerNullData(_grafer.getManager().getAllScopeSignals());
        nullData.setDefinedMeanSignals(_meanSignals);        
        _zvDatenRAM = nullData;
        _scopeFrame._scope.setDataContainer(_zvDatenRAM);

        _scopeFrame.setReferenzAufRegelBlock(this);
        _scopeFrame.setTitle(" " + getStringID());
        if (_waveformChar != null) {
            _waveformChar.setInvalid();
        }
        _fourier = null;
        _grafer.refreshComponentPane();
    }

    public AbstractDataContainer getZVDatenImRAM() {
        return _zvDatenRAM;
    }

    public void setTerminalKnotenLabel(final String newLabel, final int knotenIndex) {
        // TODO ??? _zvDatenRAM.setSignalName(knotenIndex, newLabel);
    }

    public void setDataContainerIndices(final int[] indices) {                
        final List<Integer> globalIndices = new ArrayList<Integer>();
        for (int index : indices) {
            globalIndices.add(index);
        }
        _scopeWrapperIndices = new ScopeWrapperIndices(globalIndices, NetzlisteCONTROL.globalData);
    }

    // wird von ReglerOSZI ueberschrieben, um als Zusatzfunktionalitaet die Terminal-Anzahl direkt aendern zu koennen
    @Override
    public int istAngeklickt(final int mouseX, final int mouseY) {

        final boolean symbolClicked = xKlickMin <= mouseX && mouseX <= xKlickMax
                && yKlickMin <= mouseY && mouseY <= yKlickMax;
        final boolean upperTriClicked = _xKlickMinTerm <= mouseX && mouseX <= _xKlickMaxTerm
                && _yKlickMinTermSUB <= mouseY && mouseY <= _yKlickMaxTermSUB;
        final boolean lowerTriClicked = _xKlickMinTerm <= mouseX && mouseX <= _xKlickMaxTerm
                && _yKlickMinTermADD <= mouseY && mouseY <= _yKlickMaxTermADD;
        if (symbolClicked) {
            return 1;  // SCOPE-Symbol ist angeklickt worden --> Dialog oder Bearbeitungs-Modus
        } else {
            if (lowerTriClicked) {
                // erhoehe Zahl der Terminals um Eins und aktualisiere SCOPE                
                _inputTerminalNumber.setUserValue(_inputTerminalNumber.getValue() + 1);
                setInputTerminalNumber(_inputTerminalNumber.getValue());
                _grafer.setInitalCurveConnection(_inputTerminalNumber.getValue());
                return 2;
            } else if (upperTriClicked && _inputTerminalNumber.getValue() >= 2) {
                // reduziere Zahl der Terminals um Eins und aktualisiere SCOPE
                _inputTerminalNumber.setUserValue(_inputTerminalNumber.getValue() - 1);
                setInputTerminalNumber(_inputTerminalNumber.getValue());
                return 2;
            }
            return 0;  // SCOPE-Symbol ist nicht angeklickt worden, daher 'false'
        }
    }

    @Override
    public void deleteActionIndividual() {
        try {
            _scopeFrame.dispose();
            if (_zvDatenRAM instanceof DataContainerScopeWrapper) {
                ((DataContainerScopeWrapper) _zvDatenRAM).deregisterObserver();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.deleteActionIndividual();
    }

    /**
     * the scope has many, many settings, Therefore, we just use the export
     * function/save to file to create a copy of the scope.
     *
     * @return a copy object of this scope
     */
    @Override
    public AbstractBlockInterface copyFabric(final long shiftValue) {
        final ReglerOSZI returnValue = new ReglerOSZI();
        returnValue.getIdentifier().createNewIdentifier(this.getUniqueObjectIdentifier() + shiftValue);
        returnValue.setTyp(_controlTyp);
        final StringBuffer exportString = new StringBuffer();
        this.exportASCII(exportString);
        final TokenMap tokenMap = new TokenMap(exportString.toString().split("\n"), false);
        returnValue.setParentCircuitSheet(this.getParentCircuitSheet());
        returnValue.importASCII(tokenMap);
        return returnValue;
    }

    public String getSubCircuitPath() {
        String pathToComponent = "";
        CircuitSheet cs = getParentCircuitSheet();

        while (cs != null && cs instanceof SubCircuitSheet) {
            SubCircuitSheet subSheet = (SubCircuitSheet) cs;
            pathToComponent = subSheet._subBlock.getStringID() + "#" + pathToComponent;
            cs = subSheet._subBlock.getParentCircuitSheet();
        }
        return pathToComponent;
    }

    public class ReglerOSZICalculator extends AbstractControlCalculatable implements NotCalculateableMarker, MemoryInitializable {

        public ReglerOSZICalculator(final int noInputs) {
            super(noInputs, 0);
        }

        @Override
        public void berechneYOUT(final double deltaT) {
            assert false;
        }

        @Override
        public void doInit(double deltaT) {
            doInitialCalculation();
        }
    }

    @Override
    public void doOperationAfterNewConstruction() {
        if (_grafer._manager.getDiagrams().isEmpty()) {
            createInitialDiagram();
        }
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new ReglerOSZICalculator(XIN.size());
    }

    @Override
    public void drawBlockRectangle(final Graphics2D graphics) {

        int x = getSheetPosition().x;
        int y = getSheetPosition().y;

        // Klickbereich SCOPE-Symbol:
        xKlickMin = (int) (dpix * (x - WIDTH));
        xKlickMax = (int) (dpix * (x + WIDTH));
        yKlickMin = (int) (dpix * (y - WIDTH));
        yKlickMax = (int) (dpix * (y - WIDTH + _inputTerminalNumber.getValue()));
        Color origColor = graphics.getColor();
        graphics.setColor(getBackgroundColor());

        graphics.fillRect((int) (dpix * (x - WIDTH)), (int) (dpix * (y - WIDTH)), (int) (dpix * (2 * WIDTH)), (int) (dpix * (1.0 * _inputTerminalNumber.getValue())));
        graphics.setColor(origColor);
        graphics.drawRect((int) (dpix * (x - WIDTH)), (int) (dpix * (y - WIDTH)), (int) (dpix * (2 * WIDTH)), (int) (dpix * (1.0 * _inputTerminalNumber.getValue())));

        graphics.drawRect((int) (dpix * (x - WIDTH)) + INSIDE_RECT, (int) (dpix * (y - WIDTH)) + 2 * INSIDE_RECT, (int) (dpix * (2 * WIDTH)) - 2 * INSIDE_RECT,
                (int) (dpix * (1.0 * _inputTerminalNumber.getValue())) - 4 * INSIDE_RECT);

        // Rote Dreiecke zum Klicken --> Aenderung der Terminal-Anzahl:
        graphics.setColor(Color.red);


        final int[] triXCoords = new int[]{(int) (dpix * x), (int) (dpix * (x) + DIAMETER), (int) (dpix * (x)) - DIAMETER};
        final int yp0 = (int) (dpix * (y - WIDTH - HEIGHT) - DELTA), yp1 = (int) (dpix * (y - WIDTH) - DELTA);
        final int ym1 = (int) (dpix * (y - WIDTH + _inputTerminalNumber.getValue()) + DELTA),
                ym0 = (int) (dpix * (y - WIDTH + _inputTerminalNumber.getValue() + HEIGHT) + DELTA);
        final int[] triYCoords = new int[]{(int) (dpix * (y - WIDTH - HEIGHT) - DELTA),
            (int) (dpix * (y - WIDTH) - DELTA), (int) (dpix * (y - WIDTH) - DELTA)};

        graphics.fillPolygon(triXCoords, triYCoords, triYCoords.length);
        graphics.fillPolygon(triXCoords, new int[]{ym0, ym1, ym1}, 3);
        // Klickbereich rote Dreiecke fuer Terminal-Anzahl-Aenderung:
        _xKlickMinTerm = triXCoords[2];
        _xKlickMaxTerm = triXCoords[1];
        _yKlickMinTermSUB = yp0;  // oberes Dreieck  --> SUB / Reduktion der Terminal-Anzahl
        _yKlickMaxTermSUB = yp1;
        _yKlickMinTermADD = ym1;  // unteres Dreieck --> ADD / Erhoehung der Terminal-Anzahl
        _yKlickMaxTermADD = ym0;
        graphics.setColor(origColor);
    }

    @Override
    public void exportAsciiIndividual(final StringBuffer ascii) {
        // somewhere is a bug hidden in the save routine for the scope
        // this was probably the reason why the data file was corrupted
        // to 10 bytes. Here, I append the final scope String only
        // at the end, when everything went fine!
        final StringBuffer appendLater = new StringBuffer();
        try {
            _scopeSettings.exportASCII(appendLater);
            super.exportAsciiIndividual(appendLater);
            appendLater.append("\ntn");
            DatenSpeicher.appendAsString(appendLater.append("\nisShowName"), _isShowName);

            _saveLoadSignalNames = new String[_zvDatenRAM.getRowLength()];
            for (int i = 0; i < _zvDatenRAM.getRowLength(); i++) {
                _saveLoadSignalNames[i] = _zvDatenRAM.getSignalName(i);
            }
            DatenSpeicher.appendAsString(appendLater.append("\nsavedSignalNames"), _saveLoadSignalNames);

            _meanSignals.exportIndividualCONTROL(appendLater);
            appendLater.append("\n<ScopeSettings>\n");
            _grafer.exportIndividualCONTROL(appendLater);
            appendLater.append("\n<\\ScopeSettings>\n");

            appendLater.append("\n<ScopeWindowSettings>\n");
            _scopeFrame.exportIndividualCONTROL(appendLater);
            appendLater.append("\n<\\ScopeWindowSettings>\n");
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        ascii.append(appendLater);
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {

        _scopeInputSignals.clear();
        for (int i = 0; i < _inputTerminalNumber.getValue(); i++) {
            _scopeInputSignals.push(new ScopeSignalRegular(_scopeInputSignals.size(), this));
        }

        _grafer.getManager().setInputSignals(_scopeInputSignals);

        _meanSignals.importIndividualCONTROL(tokenMap);
        // JMC

        if (tokenMap.containsToken("savedSignalNames[]")) {
            _saveLoadSignalNames = tokenMap.readDataLine("savedSignalNames[]", _saveLoadSignalNames);
        } else { //otherwise we get a null pointer exception during loading old version of the file
            _saveLoadSignalNames = new String[0];
        }

        if (tokenMap.containsToken("isShowName")) {
            _isShowName = tokenMap.readDataLine("isShowName", _isShowName);
        }

        final TokenMap settingsMap = tokenMap.getBlockTokenMap("<ScopeSettings>");
        if (settingsMap != null) {
            _grafer.importIndividualCONTROL(settingsMap);
        }

        final TokenMap windowSettingsMap = tokenMap.getBlockTokenMap("<ScopeWindowSettings>");
        if (settingsMap != null) {
            _scopeFrame.importIndividualCONTROL(windowSettingsMap);
        }

        final TokenMap scopeMap = tokenMap.getBlockTokenMap("<scopeSettings>");

        if (scopeMap != null) {
            importScopeSettings(scopeMap);
        }

        if (Fenster.IS_APPLET && !Fenster.IS_BRANDED) {
            _scopeFrame.setVisible(true);
        }
    }

    //for use with GeckoSCRIPT - get waveform characteristics for a particular channel
    public double[] getChannelCharacteristics(final int channel,
            final double start, final double end) throws GeckoInvalidArgumentException {

        if (_waveformChar == null || !_waveformChar.isValid() || (start != _charStart) || (end != _charEnd)) {
            _waveformChar = CharacteristicsCalculator.calculateFabric(_zvDatenRAM, start, end);
            _charStart = start;
            _charEnd = end;
        }

        return _waveformChar.getChannelCharacteristics(channel);
    }

    //for use with GeckoSCRIPT - Fourier analysis
    public double[][] doFourierAnalysis(final int channel,
            final double start, final double end, final int harmonics) throws Exception {

        if ((_fourier == null) || (start != _fourStart) || (end != _fourEnd) || (harmonics > _fourier[0][0].length + 1)) {
            final FourierGUIless fourier = new FourierGUIless(_zvDatenRAM, start, end, harmonics);
            _fourier = fourier.doFourier();
            _fourStart = start;
            _fourEnd = end;
        }

        if (channel >= (XIN.size()) || (channel < 0)) {
            throw new Exception("Invalid scope port supplied for Fourier analysis!");
        }

        double[][] channelFourier = new double[FOUR_CHAN_DEPTH][harmonics + 1];

        // _fourier is a double[][][] of format [coefficient][channel][value for nth harmonic]
        // channelFourier is double[][] of format [coefficient][value for nth harmonic]

        for (int i = 0; i < FOUR_CHAN_DEPTH; i++) {
            for (int j = 0; j <= harmonics; j++) {
                channelFourier[i][j] = _fourier[i][channel][j];
            }
        }
        return channelFourier;
    }

    public void doInitialCalculation() {
        List<ExternalSignal> externalSignals;
        // (b) wenn die Simulation ein zweites, drittes usw. weiteres Mal neugestartet wird
        if (_zvDatenRAM != null) {
            _zvDatenRAM.deleteObservers();
        }

        final DataContainerScopeWrapper scopeWrapper;
        scopeWrapper = new DataContainerScopeWrapper(NetzlisteCONTROL.globalData,
                _scopeWrapperIndices,
                _meanSignals,                
                _grafer.getManager().getAllScopeSignals());
        

        if (_zvDatenRAM instanceof DataContainerScopeWrapper) {
            ((DataContainerScopeWrapper) _zvDatenRAM).deregisterObserver();
        }

        _zvDatenRAM = scopeWrapper;

        _scopeFrame.clearZVDaten();
        _scopeFrame._scope.setDataContainer(_zvDatenRAM);
        //------------
        // jedesmal, wenn ein neues SCOPE-Fenster 'laeuft', beginnt die ZV-Datenspeicherung von Neuem
        if (_waveformChar != null) {
            _waveformChar.setInvalid();
        }
        _fourier = null;
    }

    public void setSimulationTimeBoundaries(final double tStart, final double tEnd) {
        _grafer.setSimulationTimeBoundaries(tStart, tEnd);
    }

    void importScopeSettings(final TokenMap scopeMap) {
        _scopeSettings.importASCII(scopeMap);
        _scopeSettings.loadSettings(_scopeFrame.getGrafer());  // hier wird 'this' parametrisiert                
    }

    public boolean isAntiAliasing() {
        return _grafer.isAntiAliasing();
    }

    public void setAntiAliasing(final boolean value) {
        _grafer.setAntiAliasing(value);
    }

    @Override
    public void setInputTerminalNumber(final int number) {
        while (XIN.size() > number) {
            XIN.pop();
        }

        while (XIN.size() < number) {
            XIN.add(new TerminalControlInput(this, TERM_POS_X, -XIN.size()));
        }

        try {
            if (_grafer != null) {
                _grafer.getManager().defineNewSignalNumber(this, XIN.size(), _meanSignals);
                _scopeFrame.setNewTerminalNumber(XIN.size());
            }

        } catch (Exception ex) {
            //ex.printStackTrace();
        }


    }

    @Override
    public void setOutputTerminalNumber(final int number) {
        // scope does not have output terminals
    }

    void createInitialDiagram() {
        _grafer.createInitialDiagram();
    }

    @Override
    public boolean isNameVisible() {
        return _isShowName;
    }

    @Override
    public void setNameVisible(final boolean newValue) {
        _isShowName = newValue;
    }

    @Override
    protected final Window openDialogWindow() {
        _scopeFrame.setTitle(" " + getStringID());
        return _scopeFrame;
    }
}
