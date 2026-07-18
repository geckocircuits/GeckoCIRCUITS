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
package gecko.geckocircuits.control.javablock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gecko.core.circuit.TokenMap;

import gecko.geckocircuits.control.ControlTypeInfo;
import gecko.GeckoSim;
import gecko.SystemOutputRedirect;
import gecko.geckocircuits.general.ProjectData;
import gecko.geckocircuits.general.MainWindow;
import gecko.core.allg.GeckoFile;
import gecko.geckocircuits.general.GlobalColors;
import gecko.geckocircuits.general.UserParameter;
import gecko.geckocircuits.circuit.*;
import gecko.geckocircuits.circuit.circuitcomponents.TextInfoType;
import gecko.geckocircuits.control.Operationable;
import gecko.geckocircuits.control.RegelBlock;
import gecko.geckocircuits.control.SpecialNameVisible;
import gecko.geckocircuits.control.VariableTerminalNumber;
import gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import gecko.geckocircuits.control.calculators.InitializableAtSimulationStart;
import gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
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
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "UR_UNINIT_READ_CALLED_FROM_SUPER_CONSTRUCTOR",
        justification = "_inputTerminalNumber may be read before field initialization when setInputTerminalNumber is called from superclass constructor - handled safely with null checks")
public final class ControlJavaFunction extends RegelBlock implements VariableTerminalNumber, SpecialNameVisible,
        GeckoFileable, Operationable {
    private static final Logger LOGGER = LogManager.getLogger(ControlJavaFunction.class);

    private static final long serialVersionUID = 1L;

    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ControlJavaFunction.class, "JAVA", I18nKeys.JAVA_FUNCTION);
    private transient final ControlJavaTriangles _inputTri = new ControlJavaTriangles();
    private transient final ControlJavaTriangles _outputTri = new ControlJavaTriangles();
    private CodeWindowModern _codeWindow;

    transient final UserParameter<Integer> _inputTerminalNumber = UserParameter.Builder.
            <Integer>start("numberInputTerminals", 3).
            longName(I18nKeys.NO_INPUT_TERMINALS).
            shortName("numberInputTerminals").
            arrayIndex(this, -1).
            build();

    transient final UserParameter<Integer> _outputTerminalNumber = UserParameter.Builder.
            <Integer>start("numberOutputTerminals", 2).
            longName(I18nKeys.NO_OUTPUT_TERMINALS).
            shortName("numberOutputTerminals").
            arrayIndex(this, -1).
            build();

    transient final UserParameter<Boolean> _showName = UserParameter.Builder.
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
    transient final VariableBusWidth _variableBusWidth = new VariableBusWidth(this);

    @SuppressWarnings("PMD")
    private final StringBuffer _outputStringBuffer = new StringBuffer();
    private boolean _populateFileList = false;
    private static final int THREE = 3;
    private static final int DEF_IN_TERMS = 3;
    private static final int DEF_OUT_TERMS = 2;
    private transient AbstractJavaBlock _javaBlock = new JavaBlockVector(this);
    private final Set<String> _additionalFilesHashKeys = new TreeSet();
    private boolean _isConsoleOutput = true;
    private static final int DIAMETER = 4;
    private static final double HEIGHT = 0.6, WIDTH = 1.4;
    private boolean _clearOutput = true;

    public ControlJavaFunction() {
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
        public void calculateYOUT(final double deltaT) {

            if (_isConsoleOutput) {
                SystemOutputRedirect.setConsoleOutput(getStringID());
            } else {
                SystemOutputRedirect.setAlternativeOutput(_outputStringBuffer, getStringID());
            }

            try {
                _javaBlock.calculateYOUT(_time, deltaT, _inputSignal, _outputSignal);
            } catch (InvocationTargetException ex) {
                LOGGER.error(ex.getTargetException());
                final StackTraceElement[] ste = ex.getTargetException().getStackTrace();
                if (ste.length > 0) {
                    LOGGER.error(ste[0] + "\n");
                }

                // Exception in the main method that we just tried to run
                //showMsg("Exception in main: " + ex.getTargetException());
                //ex.getTargetException().printStackTrace();
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage());
                final StackTraceElement[] ste = ex.getStackTrace();
                if (ste.length > 0) {
                    LOGGER.error(ste[0] + "\n");
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
                LOGGER.error(ex.getMessage());
                final StackTraceElement[] ste = ex.getStackTrace();
                if (ste.length > 0) {
                    LOGGER.error(ste[0] + "\n");
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
                hashValue = Long.parseLong(hash);
                try {
                    file = MainWindow._fileManager.getFile(hashValue);
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
            return 1;  // SCOPE symbol has been clicked --> Dialog or editing mode
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
        xKlickMax = xKlickMin + (int) (dpix * 2 * WIDTH);
        yKlickMax = yKlickMin + (int) (dpix * 1.0 * Math.max(XIN.size(), YOUT.size()));

        graphics.fillRect(xKlickMin, yKlickMin,
                xKlickMax - xKlickMin, yKlickMax - yKlickMin);

        graphics.setColor(origColor);

        graphics.drawRect(xKlickMin, yKlickMin,
                xKlickMax - xKlickMin, yKlickMax - yKlickMin);

        // Red triangles to click --> Change the number of terminals:
        graphics.setColor(Color.red);
        final int delta = THREE;  // Abstand vom roten Dreieck vom SCOPE-Block (nach oben bzw. nach unten)
        final int xd0 = (dpix * xPos) - dpix, xd1 = dpix * xPos + DIAMETER - dpix, xd2 = (dpix * xPos) - DIAMETER - dpix;
        final int yp0 = (int) (dpix * (yPos - WIDTH - HEIGHT) - delta), yp1 = (int) (dpix * (yPos - WIDTH) - delta);
        final int ym1 = (int) (dpix * (yPos - WIDTH + Math.max(XIN.size(), YOUT.size())) + delta),
                ym0 = (int) (dpix * (yPos - WIDTH + Math.max(XIN.size(), YOUT.size()) + HEIGHT) + delta);

        graphics.fillPolygon(new int[]{xd0, xd1, xd2}, new int[]{yp0, yp1, yp1}, THREE);
        graphics.fillPolygon(new int[]{xd0, xd1, xd2}, new int[]{ym0, ym1, ym1}, THREE);

        final int xdOUT0 = (dpix * xPos) + dpix, xdOUT1 = dpix * xPos + DIAMETER + dpix,
                xdOUT2 = (dpix * xPos) - DIAMETER + dpix;
        final int ypOUT0 = (int) (dpix * (yPos - WIDTH - HEIGHT) - delta), ypOUT1 = (int) (dpix * (yPos - WIDTH) - delta);
        final int ymOUT1 = (int) (dpix * (yPos - WIDTH + Math.max(XIN.size(), YOUT.size())) + delta),
                ymOUT0 = (int) (dpix * (yPos - WIDTH + Math.max(XIN.size(), YOUT.size()) + HEIGHT) + delta);

        graphics.fillPolygon(new int[]{xdOUT0, xdOUT1, xdOUT2}, new int[]{ypOUT0, ypOUT1, ypOUT1}, THREE);
        graphics.fillPolygon(new int[]{xdOUT0, xdOUT1, xdOUT2}, new int[]{ymOUT0, ymOUT1, ymOUT1}, THREE);

        // Click area red triangles for terminal number change:
        _inputTri._xKlickMinTerminal = xd2;
        _inputTri._xKlickMaxTerminal = xd1;
        _inputTri._yKlickMinTerminalSUB = yp0;  // upper triangle --> SUB / reduction of the number of terminals
        _inputTri._yKlickMaxTerminalSUB = yp1;
        _inputTri._yKlickMinTerminalADD = ym1;  // lower triangle --> ADD / increase the number of terminals
        _inputTri._yKlickMaxTerminalADD = ym0;

        // the same for the outputs:
        _outputTri._xKlickMinTerminal = xdOUT2;
        _outputTri._xKlickMaxTerminal = xdOUT1;
        _outputTri._yKlickMinTerminalSUB = ypOUT0;  // upper triangle --> SUB / reduction of the number of terminals
        _outputTri._yKlickMaxTerminalSUB = ypOUT1;
        _outputTri._yKlickMinTerminalADD = ymOUT1;  // lower triangle --> ADD / increase the number of terminals
        _outputTri._yKlickMaxTerminalADD = ymOUT0;
        graphics.setColor(origColor);
    }

    @Override
    public void copyAdditionalParameters(final AbstractBlockInterface originalBlock) {
        super.copyAdditionalParameters(originalBlock);

        final ControlJavaFunction other = (ControlJavaFunction) originalBlock;

        this.XIN.clear();
        this.YOUT.clear();

        for (AbstractTerminal input : other.XIN) {
            this.XIN.add(input.createCopy(this));
        }

        for (AbstractTerminal output : other.YOUT) {
            this.YOUT.add(output.createCopy(this));
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
        ProjectData.appendAsString(ascii.append("\nisConsoleOutput"), _isConsoleOutput);
        ProjectData.appendAsString(ascii.append("\nclearOutput"), _clearOutput);
        _variableBusWidth.exportAsciiIndividual(ascii);
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {

        if (tokenMap.containsToken("clearOutput")) {
            _clearOutput = tokenMap.readDataLine("clearOutput", _clearOutput);
        }

        if (tokenMap.containsToken("numberInputTerminals")) {
            int inNumber = XIN.size();
            inNumber = tokenMap.readDataLine("numberInputTerminals", inNumber);
            setInputTerminalNumber(inNumber);
        }
        // these two blocks are for backward-compatibility with versions before 1.60. Here,
        // we have to correct the number of input/output terminals!
        if (tokenMap.containsToken("numberOutputTerminals")) {
            int outNumber = YOUT.size();
            outNumber = tokenMap.readDataLine("numberOutputTerminals", outNumber);
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
            MainWindow._fileManager.addFile(newFile);
        }
        _codeWindow.addNewExtraFiles(newFiles);
    }

    @Override
    public void removeLocalComponentFiles(final List<GeckoFile> filesToRemove) {
        for (GeckoFile removedFile : filesToRemove) {
            _javaBlock._additionalSourceFiles.remove(removedFile);
            removedFile.removeUser(getUniqueObjectIdentifier());
            MainWindow._fileManager.maintain(removedFile);
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
        // Null check required because this method may be called from superclass constructor
        // before _inputTerminalNumber field is initialized (UR_UNINIT_READ_CALLED_FROM_SUPER_CONSTRUCTOR)
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

        // Null check required because this method may be called from superclass constructor
        // before _outputTerminalNumber field is initialized (UR_UNINIT_READ_CALLED_FROM_SUPER_CONSTRUCTOR)
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
    protected Window openDialogWindow() {
        if (GeckoSim.compiler_toolsjar_missing) {
            // tools.jar ist nicht vorhanden --> der Compiler, der fuer den JAVA-Block notwendig ist, fehlt -->
            // tools.jar is not available --> the compiler, which is necessary for the JAVA block, is missing -->
            JOptionPane.showMessageDialog(null, "No tools.jar library found!", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        } else {
            // tools.jar is not available --> the compiler, which is necessary for the JAVA block, is missing -->
            if (_codeWindow == null) {
                _codeWindow = new CodeWindowModern(this, _outputStringBuffer);
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
