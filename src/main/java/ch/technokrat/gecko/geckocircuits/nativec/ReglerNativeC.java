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
package ch.technokrat.gecko.geckocircuits.nativec;

import ch.technokrat.gecko.GeckoSim;
import ch.technokrat.gecko.SystemOutputRedirect;
import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.ComponentState;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalControlInput;
import ch.technokrat.gecko.geckocircuits.circuit.TerminalControlOutput;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.TextInfoType;
import ch.technokrat.gecko.geckocircuits.control.ControlTypeInfo;
import ch.technokrat.gecko.geckocircuits.control.RegelBlock;
import ch.technokrat.gecko.geckocircuits.control.SpecialNameVisible;
import ch.technokrat.gecko.geckocircuits.control.VariableTerminalNumber;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import static ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable._time;
import ch.technokrat.gecko.geckocircuits.control.javablock.ReglerJavaTriangles;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Window;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Pattern;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;

/**
 * Contains only the graphic representation of the ControlBlock. Everything else
 * should be delegated to the JavaBlock-Class
 *
 * @author andreas
 */
public final class ReglerNativeC extends RegelBlock implements VariableTerminalNumber {

    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerNativeC.class, "C DLL", I18nKeys.C_DLL);

    private final ReglerJavaTriangles _inputTri = new ReglerJavaTriangles();
    private final ReglerJavaTriangles _outputTri = new ReglerJavaTriangles();
    private NativeCDialog _guiWindow;
    private NativeCBlock _nativeCBlock;
    private NativeCLibraryFile _libFile;
    private DefaultListModel _libFileList;
    private static final String PATH_SPLITTER = ";";
    
    
    private final UserParameter<String> _paramSelectedLibName = UserParameter.Builder.<String>start("nativeCLibrary", "null").
            longName(I18nKeys.NATIVE_LIB).
            shortName("nativeLib").
            showInTextInfo(TextInfoType.SHOW_NEVER).
            arrayIndex(this, -1).
            build();
    
    private final UserParameter<String> _paramLibNames = UserParameter.Builder.<String>start("nativeCLibraries", "null").
            longName(I18nKeys.NATIVE_LIBS).
            shortName("nativeLibs").
            showInTextInfo(TextInfoType.SHOW_NEVER).
            arrayIndex(this, -1).
            build();
    
        
    @SuppressWarnings("PMD")
    private final StringBuffer _outputStringBuffer = new StringBuffer();
    private static final int THREE = 3;
    private static final int DEF_IN_TERMS = 3;
    private static final int DEF_OUT_TERMS = 2;
        
    private boolean _isConsoleOutput = true;
    private static final int DIAMETER = 4;
    private static final double HEIGHT = 0.6, WIDTH = 1.4;
    private boolean _clearOutput = true;

    public ReglerNativeC() {
        super(DEF_IN_TERMS, DEF_OUT_TERMS);
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
        if(_guiWindow != null && _guiWindow.isVisible()) {
            _guiWindow.setVisible(false);
            _guiWindow.dispose();
        }
        super.deleteActionIndividual();
    }
            
    
    class CCalculator extends AbstractControlCalculatable {
            private boolean severeErrorOccured = false; // save error to jump out of berechneYOUT
            
            CCalculator() {
                super(XIN.size(), YOUT.size());
            }
            
            /**
             *  Use this function to unload the Native Library if the Simulation is paused or finished
             */
            @Override
            public void tearDownOnPause() {
                if (_nativeCBlock != null) {
                    _nativeCBlock.unloadLibraries();
                    _nativeCBlock = null;
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
                    if (_time == 0) {
                        severeErrorOccured = false;
                    }
                    if (!severeErrorOccured) {  // if the native library or function was not found, there is no need to execute it!
                        
                        // OBSERVATION: UserParameter _paramLib*** was filled lazy with data from loaded File,
                        //      too lazy to check them in the Constructor. 
                        //      Therefore, loading them at the latest possible chance! I couldn't find better solutions.
                        //      Inits here are usually unwanted, because of testing them on every Simulation Timestep
                        if (_libFile == null || _libFileList == null) {
                            loadUserData ();
                        }
                        if (_nativeCBlock == null) {
                            _nativeCBlock = new NativeCBlock();
                            _nativeCBlock.loadLibraries(_libFile.savegetFileName());
                        }
                        if (!severeErrorOccured) {
                            _nativeCBlock.calculateYOUT(_time, deltaT, _inputSignal, _outputSignal);
                        }
                    }
                } catch (InvocationTargetException ex) {
                    severeErrorOccured = true;  // library couldn't be loaded or native function couldn't be executed
                    if (_nativeCBlock != null) {
                        _nativeCBlock.unloadLibraries();
                        _nativeCBlock = null;
                    }
                    System.err.println(ex.getTargetException());
                    final StackTraceElement[] ste = ex.getTargetException().getStackTrace();
                    if (ste.length > 0) {
                        System.err.println(ste[0] + "\n");
                    }
                    // Exception in the main method that we just tried to run
                    //showMsg("Exception in main: " + ex.getTargetException());
                    //ex.getTargetException().printStackTrace();
                } catch (FileNotFoundException ex) {
                    severeErrorOccured = true; // native library was not found
                    if (_nativeCBlock != null) {
                        _nativeCBlock.unloadLibraries();
                        _nativeCBlock = null;
                    }
                    String errorStr;
                    if (_libFile != null && _libFile.getFileName() != null) {
                        errorStr = "Could not find Native Library " + _libFile.getFileName() + " !";
                    } else {
                        errorStr = "No valid Native Library selected!";
                    }
                    System.err.println(errorStr);
                    JOptionPane.showMessageDialog(null, errorStr, "Error", JOptionPane.ERROR_MESSAGE);
                } catch (UnsatisfiedLinkError ex) {
                severeErrorOccured = true; // native library was not found
                if (_nativeCBlock != null) {
                    _nativeCBlock.unloadLibraries();
                    _nativeCBlock = null;
                }
                String errorStr = "Could not load Native Library " + _libFile.getFileName() + " !";
                System.err.println(errorStr);
                JOptionPane.showMessageDialog(null, errorStr, "Error", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    severeErrorOccured = true;
                    ex.printStackTrace();
                    System.err.println(ex.getMessage());
                    final StackTraceElement[] ste = ex.getStackTrace();
                    if (ste.length > 0) {
                        System.err.println(ste[0] + "\n");
                    }
                }    
                SystemOutputRedirect.setOriginalOutput();
            }
        };

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        if (_clearOutput) {
            _outputStringBuffer.delete(0, _outputStringBuffer.length());
        }

        return new CCalculator();
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
            return 2;
        }
        if (_inputTri.isDecreaseClicked(mouseX, mouseY)) {
            setInputTerminalNumber(Math.max(0, XIN.size() - 1));// decrea
            return 2;
        }
        if (_outputTri.isDecreaseClicked(mouseX, mouseY)) {
            setOutputTerminalNumber(Math.max(0, YOUT.size() - 1));            
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
//            if (_javaBlock.getCompileStatus() == CompileStatus.NOT_COMPILED) {
//                graphics.setColor(Color.WHITE);
//            } else if (_javaBlock.getCompileStatus() == CompileStatus.COMPILED_SUCCESSFULL) {
//                graphics.setColor(GlobalColors.farbeElementCONTROLHintergrund);
//            } else if (_javaBlock.getCompileStatus() == CompileStatus.COMPILE_ERROR) {
//                graphics.setColor(Color.orange);
//            }
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

        
    }

    @Override
    protected void exportAsciiIndividual(final StringBuffer ascii) {                
        DatenSpeicher.appendAsString(ascii.append("\nisConsoleOutput"), _isConsoleOutput);
        DatenSpeicher.appendAsString(ascii.append("\nclearOutput"), _clearOutput);
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
    }

    @Override
    public void setOutputTerminalNumber(final int number) {

        while (YOUT.size() > number) {
            YOUT.pop();
        }

        while (YOUT.size() < number) {
            YOUT.add(new TerminalControlOutput(this, 2, -YOUT.size() + 1));
        }
    }
    

    boolean isClearOutput() {
        return _clearOutput;
    }

    void setClearOutput(final boolean value) {
        _clearOutput = value;
    }
    
    @Override
    protected final Window openDialogWindow() {         
            // alles OK, 'tools.jar' ist vorhanden und der JAVA-Block kann korrekt hochgefahren werden 
            
            // if Native Library has already been loaded, it's time to unload it
            // the User might want to load a different Native Library
            if (_nativeCBlock != null) {
                _nativeCBlock.unloadLibraries();
                _nativeCBlock = null;
            }
            // OBSERVATION: _paramLib*** was filled lazy with data from loaded File,
            //      too lazy to check them in the Constructor. 
            //      Therefore, loading them at the latest possible chance!
            if (_libFile == null || _libFileList == null) {
                loadUserData ();
            }
            if (_guiWindow == null) {
                _guiWindow = new NativeCDialog(this, GeckoSim._win, false, 
                            _libFile, _libFileList);
//                if (_paramSelectedLibName.getValue().equals("null")) {
//                    _guiWindow = new NativeCDialog(this, GeckoSim._win, false);
//                } else {
//                    String test = (_paramLibNames.getValue());
//                    
//                }
                //_guiWindow.setVisible(true);
                //_guiWindow.loadSourcesText();
            } else {
                if (_guiWindow.isVisible()) {
                    _guiWindow.toFront();
                } else {
                    //_guiWindow.loadSourcesText();
                }
            }
            return (Window) _guiWindow;        
    }
    
    public void triggerUpdate () {
        _paramLibNames.setUserValue(convertList2String(_libFileList));
        if (_libFile.getFile() == null ||_libFile.getFileName() == null) {
            _paramSelectedLibName.setUserValue("null");
        } else {
            _paramSelectedLibName.setUserValue(_libFile.getFileName());
        }
        
    }
    
    public DefaultListModel convertString2List (final String list) {
        DefaultListModel result = new DefaultListModel();
        String[] elements = list.split(Pattern.quote(PATH_SPLITTER));
        for (int i=0; i<elements.length; i++) {
            result.addElement(elements[i]);
        }
        return result;
    }
    
    public String convertList2String (final DefaultListModel listVec) {
        StringBuffer result = new StringBuffer();
        for (int i=0; i<listVec.size(); i++) {
            result.append(listVec.get(i) + PATH_SPLITTER);
        }
        return result.toString();
    }
    
    public void loadUserData () {
        String selLibName = _paramSelectedLibName.getValue();
        if (selLibName.isEmpty() || selLibName.equals("null")) {
            _libFile = new NativeCLibraryFile();
        } else {
            try {
                _libFile = new NativeCLibraryFile(selLibName);
            } catch (FileNotFoundException exc) {
                //String errTxt = "Native Library " + selLibName + " does not exist anymore.";
                //System.err.println(errTxt);
                //JOptionPane.showMessageDialog(null, errTxt, "Error", JOptionPane.ERROR_MESSAGE);
                _libFile = new NativeCLibraryFile();
            }
        }
        String libNamesList = _paramLibNames.getValue();
        if (libNamesList.isEmpty() || libNamesList.equals("null")) {
            _libFileList = new DefaultListModel();
        } else {
            _libFileList = convertString2List(libNamesList);
        }
    }
}
