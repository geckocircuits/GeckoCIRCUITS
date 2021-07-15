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
package ch.technokrat.gecko.geckocircuits.control.javablock;

import ch.technokrat.gecko.geckocircuits.control.ControlTypeInfo;
import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.SystemOutputRedirect;
import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFile;
import ch.technokrat.gecko.geckocircuits.allg.GlobalColors;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.TextInfoType;
import ch.technokrat.gecko.geckocircuits.control.Operationable;
import ch.technokrat.gecko.geckocircuits.control.RegelBlock;
import ch.technokrat.gecko.geckocircuits.control.SpecialNameVisible;
import ch.technokrat.gecko.geckocircuits.control.VariableTerminalNumber;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import static ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable._time;
import ch.technokrat.gecko.geckocircuits.control.calculators.InitializableAtSimulationStart;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JOptionPane;

/**
 * Contains only the graphic representation of the ControlBlock. Everything else
 * should be delegated to the JavaBlock-Class
 *
 * @author andreas
 */
public final class ReglerJavaFunction extends RegelBlock implements VariableTerminalNumber, SpecialNameVisible,
        GeckoFileable, Operationable {

    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerJavaFunction.class, "JAVA", I18nKeys.JAVA_FUNCTION);
    private final ReglerJavaTriangles _inputTri = new ReglerJavaTriangles();
    private final ReglerJavaTriangles _outputTri = new ReglerJavaTriangles();
    private CodeWindow _codeWindow;

    final UserParameter<Integer> _inputTerminalNumber = UserParameter.Builder.
            <Integer>start("anzXIN", 3).
            longName(I18nKeys.NO_INPUT_TERMINALS).
            shortName("numberInputTerminals").
            arrayIndex(this, -1).
            build();

    final UserParameter<Integer> _outputTerminalNumber = UserParameter.Builder.
            <Integer>start("anzYOUT", 2).
            longName(I18nKeys.NO_OUTPUT_TERMINALS).
            shortName("numberOutputTerminals").
            arrayIndex(this, -1).
            build();

    final UserParameter<Boolean> _showName = UserParameter.Builder.
            <Boolean>start("showName", true).
            longName(I18nKeys.DISPLAY_COMPONENT_NAME_IN_CIRCUIT_SHEET).
            shortName("showName").
            showInTextInfo(TextInfoType.SHOW_NEVER).
            arrayIndex(this, -1).
            build();

    /*final UserParameter<Boolean> _doDebug = UserParameter.Builder.
     <Boolean>start("doDebug", false).
     longName(I18nKeys.ENABLE_DEBUGGING_OPTIONS).
     shortName("doDebug").
     showInTextInfo(TextInfoType.SHOW_NEVER).
     arrayIndex(this, -1).
     build();
     */
    final VariableBusWidth _variableBusWidth = new VariableBusWidth(this);

    @SuppressWarnings("PMD")
    private final StringBuffer _outputStringBuffer = new StringBuffer();
    private boolean _populateFileList = false;
    private static final int THREE = 3;
    private static final int DEF_IN_TERMS = 3;
    private static final int DEF_OUT_TERMS = 2;
    private AbstractJavaBlock _javaBlock = new JavaBlockVector(this);
    private final Set<String> _additionalFilesHashKeys = new TreeSet();
    private boolean _isConsoleOutput = true;
    private static final int DIAMETER = 4;
    private static final double HEIGHT = 0.6, WIDTH = 1.4;
    private boolean _clearOutput = true;

    public ReglerJavaFunction() {
        super(DEF_IN_TERMS, DEF_OUT_TERMS);

        _variableBusWidth._useMatrix.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                _javaBlock = _javaBlock.createOtherBlockTypeCopy();
            }
        });

        _outputTerminalNumber.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setOutputTerminalNumber(_outputTerminalNumber.getValue());
            }
        });

        _inputTerminalNumber.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setInputTerminalNumber(_inputTerminalNumber.getValue());
            }
        });

    }

    @Override
    public String[] getOutputNames() {
        String[] returnValue = new String[YOUT.size()];
        for (int i = 0; i < returnValue.length; i++) {
            returnValue[i] = Integer.toString(i);
        }
        return returnValue;
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        I18nKeys[] returnValue = new I18nKeys[YOUT.size()];
        for (int i = 0; i < returnValue.length; i++) {
            returnValue[i] = I18nKeys.JAVA_FUNCTION_OUTPUT_SIGNAL;
        }
        return returnValue;
    }

    @Override
    public void deleteActionIndividual() {
        if (_codeWindow != null && _codeWindow.isVisible()) {
            _codeWindow.setVisible(false);
            _codeWindow.dispose();
        }
        super.deleteActionIndividual();
    }

    @Override
    public List<OperationInterface> getOperationEnumInterfaces() {

        OperationInterface codeOperation = new OperationInterface("setSourceCode", I18nKeys.SOURCE_CODE) {

            @Override
            public Object doOperation(Object parameterValue) {
                if (!(parameterValue instanceof String)) {
                    throw new IllegalArgumentException("Parameter type must be a String!");
                }
                String code = (String) parameterValue;
                _javaBlock._javaBlockSource = new JavaBlockSource.Builder().sourceCode(
                        code).
                        importsCode(_javaBlock._javaBlockSource._importsCode).
                        initCode(_javaBlock._javaBlockSource._initCode).variablesCode(
                                _javaBlock._javaBlockSource._variablesCode).
                        build();
                if (_codeWindow != null) {
                    _codeWindow.loadSourcesText();
                }
                return null;
            }
        };

        OperationInterface importsOperation = new OperationInterface("setImportCode", I18nKeys.IMPORTS_EXAMPLE) {

            @Override
            public Object doOperation(Object parameterValue) {
                if (!(parameterValue instanceof String)) {
                    throw new IllegalArgumentException("Parameter type must be a String!");
                }
                String code = (String) parameterValue;

                _javaBlock._javaBlockSource = new JavaBlockSource.Builder().sourceCode(
                        _javaBlock._javaBlockSource._sourceCode).
                        importsCode(code).
                        initCode(_javaBlock._javaBlockSource._initCode).variablesCode(
                                _javaBlock._javaBlockSource._variablesCode).
                        build();
                if (_codeWindow != null) {
                    _codeWindow.loadSourcesText();
                }
                return null;
            }
        };

        OperationInterface initOperation = new OperationInterface("setInitCode", I18nKeys.INIT_CODE) {

            @Override
            public Object doOperation(Object parameterValue) {
                if (!(parameterValue instanceof String)) {
                    throw new IllegalArgumentException("Parameter type must be a String!");
                }
                String code = (String) parameterValue;

                _javaBlock._javaBlockSource = new JavaBlockSource.Builder().sourceCode(
                        _javaBlock._javaBlockSource._sourceCode).
                        importsCode(_javaBlock._javaBlockSource._importsCode).
                        initCode(code).variablesCode(
                                _javaBlock._javaBlockSource._variablesCode).
                        build();
                if (_codeWindow != null) {
                    _codeWindow.loadSourcesText();
                }

                return null;
            }
        };

        OperationInterface variablesOperation = new OperationInterface("setVariablesCode", I18nKeys.VARIABLES_CODE) {

            @Override
            public Object doOperation(Object parameterValue) {
                if (!(parameterValue instanceof String)) {
                    throw new IllegalArgumentException("Parameter type must be a String!");
                }
                String code = (String) parameterValue;

                _javaBlock._javaBlockSource = new JavaBlockSource.Builder().sourceCode(
                        _javaBlock._javaBlockSource._sourceCode).
                        importsCode(_javaBlock._javaBlockSource._importsCode).
                        initCode(_javaBlock._javaBlockSource._initCode).variablesCode(
                                code).
                        build();
                if (_codeWindow != null) {
                    _codeWindow.loadSourcesText();
                }
                return null;
            }
        };

        return Arrays.asList(codeOperation, importsOperation, variablesOperation, initOperation);
    }

    private class JavaBlockCalculator extends AbstractControlCalculatable implements InitializableAtSimulationStart {

        public JavaBlockCalculator(final int noInputs, final int noOutput) {
            super(noInputs, noOutput);
        }

        @Override
        protected double[][] createOutputSignal(int noOutputs) {
            if (_javaBlock._compileObject.getCompileStatus() == CompileStatus.COMPILED_SUCCESSFULL) {
                _javaBlock.findAndLoadClass();
            }

            if (_javaBlock instanceof JavaBlockMatrix) {
                return ((JavaBlockMatrix) _javaBlock).getOutputVectorFromBlock();
            } else {
                return super.createOutputSignal(noOutputs);
            }
        }

        @Override
        public void berechneYOUT(final double deltaT) {

            if (_isConsoleOutput) {
                SystemOutputRedirect.setConsoleOutput(getStringID());
            } else {
                SystemOutputRedirect.setAlternativeOutput(_outputStringBuffer, getStringID());
            }

            try {
                _javaBlock.calculateYOUT(_time, deltaT, _inputSignal, _outputSignal);
            } catch (InvocationTargetException ex) {
                System.err.println(ex.getTargetException());
                final StackTraceElement[] ste = ex.getTargetException().getStackTrace();
                if (ste.length > 0) {
                    System.err.println(ste[0] + "\n");
                }

                // Exception in the main method that we just tried to run
                //showMsg("Exception in main: " + ex.getTargetException());
                //ex.getTargetException().printStackTrace();
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                final StackTraceElement[] ste = ex.getStackTrace();
                if (ste.length > 0) {
                    System.err.println(ste[0] + "\n");
                }

            }
            SystemOutputRedirect.setOriginalOutput();
        }

        @Override
        public void initializeAtSimulationStart(double deltaT) {
            if (_isConsoleOutput) {
                SystemOutputRedirect.setConsoleOutput(getStringID());
            } else {
                SystemOutputRedirect.setAlternativeOutput(_outputStringBuffer, getStringID());
            }
            try {
                _javaBlock.initialize(_inputSignal, _outputSignal);
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                final StackTraceElement[] ste = ex.getStackTrace();
                if (ste.length > 0) {
                    System.err.println(ste[0] + "\n");
                }
            }
            SystemOutputRedirect.setOriginalOutput();
        }
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {

        if (_clearOutput) {
            _outputStringBuffer.delete(0, _outputStringBuffer.length());
        }

        return new JavaBlockCalculator(XIN.size(), YOUT.size()) {
        };
    }

    @Override
    public void initExtraFiles() {
        if (_populateFileList) {
            long hashValue;
            GeckoFile file;
            boolean fileMissing = false;
            int filesMissing = 0;

            for (String hash : _additionalFilesHashKeys) {
                hashValue = Long.valueOf(hash);
                try {
                    file = Fenster._fileManager.getFile(hashValue);
                    _javaBlock._additionalSourceFiles.add(file);
                } catch (Exception e) {
                    fileMissing = true;
                    filesMissing++;
                }
            }
            if (fileMissing) {
                final String errorMessage = filesMissing + " additional source files missing in Java block " + getStringID();
                final String errorTitle = getStringID() + ": ERROR - File(s) not found";
                JOptionPane.showMessageDialog(null, errorMessage, errorTitle, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public int istAngeklickt(final int mouseX, final int mouseY) {
        if ((xKlickMin <= mouseX) && (mouseX <= xKlickMax) && (yKlickMin <= mouseY) && (mouseY <= yKlickMax)) {
            return 1;  // SCOPE-Symbol ist angeklickt worden --> Dialog oder Bearbeitungs-Modus
        }
        if (_inputTri.isIncreaseClicked(mouseX, mouseY)) {
            setInputTerminalNumber(XIN.size() + 1);
            return 2;
        }
        if (_outputTri.isIncreaseClicked(mouseX, mouseY)) {
            setOutputTerminalNumber(YOUT.size() + 1);
            _javaBlock.resetCompileObject();
            return 2;
        }
        if (_inputTri.isDecreaseClicked(mouseX, mouseY)) {
            setInputTerminalNumber(Math.max(0, XIN.size() - 1));// decrea
            return 2;
        }
        if (_outputTri.isDecreaseClicked(mouseX, mouseY)) {
            setOutputTerminalNumber(Math.max(0, YOUT.size() - 1));
            _javaBlock.resetCompileObject();
            return 2;
        }
        return 0;
    }

    @Override
    public void drawBlockRectangle(final Graphics2D graphics) {
        final int xPos = getSheetPosition().x;
        final int yPos = getSheetPosition().y;

        final Color origColor = graphics.getColor();
        graphics.setColor(getBackgroundColor());  // default
        if (getModus() == ComponentState.FINISHED) {
            if (_javaBlock.getCompileStatus() == CompileStatus.NOT_COMPILED) {
                graphics.setColor(Color.WHITE);
            } else if (_javaBlock.getCompileStatus() == CompileStatus.COMPILED_SUCCESSFULL) {
                graphics.setColor(GlobalColors.farbeElementCONTROLHintergrund);
            } else if (_javaBlock.getCompileStatus() == CompileStatus.COMPILE_ERROR) {
                graphics.setColor(Color.orange);
            }
        } else {
            graphics.setColor(Color.white);
        }

        xKlickMin = (int) (dpix * (xPos - WIDTH));
        yKlickMin = (int) (dpix * (yPos - WIDTH));
        xKlickMax = xKlickMin + (int) (dpix * (2 * WIDTH));
        yKlickMax = yKlickMin + (int) (dpix * (1.0 * Math.max(XIN.size(), YOUT.size())));

        graphics.fillRect(xKlickMin, yKlickMin,
                xKlickMax - xKlickMin, yKlickMax - yKlickMin);

        graphics.setColor(origColor);

        graphics.drawRect(xKlickMin, yKlickMin,
                xKlickMax - xKlickMin, yKlickMax - yKlickMin);

        // Rote Dreiecke zum Klicken --> Aenderung der Terminal-Anzahl:
        graphics.setColor(Color.red);
        final int delta = THREE;  // Abstand vom roten Dreieck vom SCOPE-Block (nach oben bzw. nach unten)
        final int xd0 = (dpix * xPos) - dpix, xd1 = (dpix * (xPos) + DIAMETER) - dpix, xd2 = (dpix * (xPos)) - DIAMETER - dpix;
        final int yp0 = (int) (dpix * (yPos - WIDTH - HEIGHT) - delta), yp1 = (int) (dpix * (yPos - WIDTH) - delta);
        final int ym1 = (int) (dpix * (yPos - WIDTH + Math.max(XIN.size(), YOUT.size())) + delta),
                ym0 = (int) (dpix * (yPos - WIDTH + Math.max(XIN.size(), YOUT.size()) + HEIGHT) + delta);

        graphics.fillPolygon(new int[]{xd0, xd1, xd2}, new int[]{yp0, yp1, yp1}, THREE);
        graphics.fillPolygon(new int[]{xd0, xd1, xd2}, new int[]{ym0, ym1, ym1}, THREE);

        final int xdOUT0 = (dpix * xPos) + dpix, xdOUT1 = (dpix * (xPos) + DIAMETER) + dpix,
                xdOUT2 = (dpix * (xPos)) - DIAMETER + dpix;
        final int ypOUT0 = (int) (dpix * (yPos - WIDTH - HEIGHT) - delta), ypOUT1 = (int) (dpix * (yPos - WIDTH) - delta);
        final int ymOUT1 = (int) (dpix * (yPos - WIDTH + Math.max(XIN.size(), YOUT.size())) + delta),
                ymOUT0 = (int) (dpix * (yPos - WIDTH + Math.max(XIN.size(), YOUT.size()) + HEIGHT) + delta);

        graphics.fillPolygon(new int[]{xdOUT0, xdOUT1, xdOUT2}, new int[]{ypOUT0, ypOUT1, ypOUT1}, THREE);
        graphics.fillPolygon(new int[]{xdOUT0, xdOUT1, xdOUT2}, new int[]{ymOUT0, ymOUT1, ymOUT1}, THREE);

        // Klickbereich rote Dreiecke fuer Terminal-Anzahl-Aenderung:
        _inputTri._xKlickMinTerminal = xd2;
        _inputTri._xKlickMaxTerminal = xd1;
        _inputTri._yKlickMinTerminalSUB = yp0;  // oberes Dreieck  --> SUB / Reduktion der Terminal-Anzahl
        _inputTri._yKlickMaxTerminalSUB = yp1;
        _inputTri._yKlickMinTerminalADD = ym1;  // unteres Dreieck --> ADD / Erhoehung der Terminal-Anzahl
        _inputTri._yKlickMaxTerminalADD = ym0;

        // das gleiche f\FCr die Ausg\E4nge:
        _outputTri._xKlickMinTerminal = xdOUT2;
        _outputTri._xKlickMaxTerminal = xdOUT1;
        _outputTri._yKlickMinTerminalSUB = ypOUT0;  // oberes Dreieck  --> SUB / Reduktion der Terminal-Anzahl
        _outputTri._yKlickMaxTerminalSUB = ypOUT1;
        _outputTri._yKlickMinTerminalADD = ymOUT1;  // unteres Dreieck --> ADD / Erhoehung der Terminal-Anzahl
        _outputTri._yKlickMaxTerminalADD = ymOUT0;
        graphics.setColor(origColor);
    }

    @Override
    public void copyAdditionalParameters(final AbstractBlockInterface originalBlock) {
        super.copyAdditionalParameters(originalBlock);

        final ReglerJavaFunction other = (ReglerJavaFunction) originalBlock;

        this.XIN.clear();
        this.YOUT.clear();

        for (int i = 0; i < other.XIN.size(); i++) {
            this.XIN.add(other.XIN.get(i).createCopy(this));
        }

        for (int i = 0; i < other.YOUT.size(); i++) {
            this.YOUT.add(other.YOUT.get(i).createCopy(this));
        }

        this._variableBusWidth.busMap.putAll(other._variableBusWidth.busMap);

        this._javaBlock._javaBlockSource = new JavaBlockSource.Builder().sourceCode(
                other._javaBlock._javaBlockSource._sourceCode).
                importsCode(other._javaBlock._javaBlockSource._importsCode).
                initCode(other._javaBlock._javaBlockSource._initCode).variablesCode(
                        other._javaBlock._javaBlockSource._variablesCode).
                build();
    }

    @Override
    protected void exportAsciiIndividual(final StringBuffer ascii) {
        _javaBlock.exportIndividualCONTROL(ascii);
        DatenSpeicher.appendAsString(ascii.append("\nisConsoleOutput"), _isConsoleOutput);
        DatenSpeicher.appendAsString(ascii.append("\nclearOutput"), _clearOutput);
        _variableBusWidth.exportAsciiIndividual(ascii);
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {

        if (tokenMap.containsToken("clearOutput")) {
            _clearOutput = tokenMap.readDataLine("clearOutput", _clearOutput);
        }

        if (tokenMap.containsToken("anzXIN")) {
            int inNumber = XIN.size();
            inNumber = tokenMap.readDataLine("anzXIN", inNumber);
            setInputTerminalNumber(inNumber);
        }
        // these two blocks are for backward-compatibility with versions before 1.60. Here,
        // we have to correct the number of input/output terminals!
        if (tokenMap.containsToken("anzYOUT")) {
            int outNumber = YOUT.size();
            outNumber = tokenMap.readDataLine("anzYOUT", outNumber);
            setOutputTerminalNumber(outNumber);
        }

        if (tokenMap.containsToken("isConsoleOutput")) {
            _isConsoleOutput = tokenMap.readDataLine("isConsoleOutput", _isConsoleOutput);
        }

        _javaBlock.importIndividualCONTROL(tokenMap);

        final TokenMap extraSourceMap = tokenMap.getBlockTokenMap("<extraSourceFiles>");

        if (extraSourceMap != null) {
            _additionalFilesHashKeys.addAll(Arrays.asList(extraSourceMap.getLines()));
            _populateFileList = true;
        }

        _variableBusWidth.importAscii(tokenMap);

    }

    public AbstractJavaBlock getJavaBlock() {
        return _javaBlock;
    }

    @Override
    public List<GeckoFile> getFiles() {
        return _javaBlock._additionalSourceFiles;
    }

    @Override
    public void addFiles(final List<GeckoFile> newFiles) {
        for (GeckoFile newFile : newFiles) {
            _javaBlock._additionalSourceFiles.add(newFile);
            newFile.setUser(getUniqueObjectIdentifier());
            Fenster._fileManager.addFile(newFile);
        }
        _codeWindow.addNewExtraFiles(newFiles);
    }

    @Override
    public void removeLocalComponentFiles(final List<GeckoFile> filesToRemove) {
        for (GeckoFile removedFile : filesToRemove) {
            _javaBlock._additionalSourceFiles.remove(removedFile);
            removedFile.removeUser(getUniqueObjectIdentifier());
            Fenster._fileManager.maintain(removedFile);
        }

        if (_codeWindow != null) {
            _codeWindow._extSourceWindow.removeFilesFromList(filesToRemove);
        }
    }

    public boolean isConsoleOutput() {
        return _isConsoleOutput;
    }

    public void setConsoleOutput(final boolean value) {
        _isConsoleOutput = value;
    }

    @Override
    public void setInputTerminalNumber(final int number) {
        while (XIN.size() > number) {
            XIN.pop();
        }

        while (XIN.size() < number) {
            XIN.add(new TerminalControlInput(this, -2, -XIN.size() + 1));
        }
        if (_inputTerminalNumber != null) {
            int newsize = XIN.size();
            if (_inputTerminalNumber.getValue() != newsize) {
                _inputTerminalNumber.setUserValue(newsize);
            }
        }

    }

    @Override
    public void setOutputTerminalNumber(final int number) {

        while (YOUT.size() > number) {
            YOUT.pop();
        }

        while (YOUT.size() < number) {
            YOUT.add(new TerminalControlOutput(this, 2, -YOUT.size() + 1));
        }

        if (_outputTerminalNumber != null) {
            int newsize = YOUT.size();
            if (_outputTerminalNumber.getValue() != newsize) {
                _outputTerminalNumber.setUserValue(newsize);
            }
        }
    }

    @Override
    public boolean isNameVisible() {
        return _showName.getValue();
    }

    @Override
    public void setNameVisible(final boolean newValue) {
        _showName.setUserValue(newValue);
    }

    boolean isClearOutput() {
        return _clearOutput;
    }

    void setClearOutput(final boolean value) {
        _clearOutput = value;
    }

    @Override
    protected final Window openDialogWindow() {
        if (GeckoSim.compiler_toolsjar_missing) {
            // tools.jar ist nicht vorhanden --> der Compiler, der fuer den JAVA-Block notwendig ist, fehlt --> 
            // es wurde daher ein 'Dummy'-Block hochgefahren, und hier wird ein Warnungs-Dialog gezeigt --> 
            JOptionPane.showMessageDialog(null, "No tools.jar library found!", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        } else {
            // alles OK, 'tools.jar' ist vorhanden und der JAVA-Block kann korrekt hochgefahren werden 
            if (_codeWindow == null) {
                _codeWindow = new CodeWindow(this, _outputStringBuffer);
                _codeWindow.loadSourcesText();
            } else {
                if (_codeWindow.isVisible()) {
                    _codeWindow.toFront();
                } else {
                    _codeWindow.loadSourcesText();
                }
            }
            return _codeWindow;
        }
    }
}
