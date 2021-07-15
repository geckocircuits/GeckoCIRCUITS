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

import ch.technokrat.gecko.GeckoRuntimeException;
import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.allg.GeckoFile;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.SchematischeEingabe2;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractJavaBlock {       
    
    protected final ReglerJavaFunction _reglerJavaBlock;
    protected AbstractCompileObject _compileObject = new CompileObjectNull();
    JavaBlockSource _javaBlockSource = new JavaBlockSource.Builder().build();
    final List<GeckoFile> _additionalSourceFiles = new ArrayList<GeckoFile>();
    protected Map<String, CompiledClassContainer> _classNameFileMap;

    AbstractJavaBlock(final ReglerJavaFunction reglerJavaBlock) {
        _reglerJavaBlock = reglerJavaBlock;
    }

    abstract AbstractJavaBlock createOtherBlockTypeCopy();

    public CompileStatus getCompileStatus() {
        return _compileObject.getCompileStatus();
    }

    public abstract void findAndLoadClass();

    public void doCompilationIfRequired() throws IOException {
        if (!checkIfCompilationRequired()) {
            return;
        }
        SchematischeEingabe2.zustandGeaendert = true;
        
        String className = CompileObject.findUniqueClassName();
        String sourceString = SourceFileGenerator.createSourceCode(_javaBlockSource, className, _reglerJavaBlock.YOUT.size(), _reglerJavaBlock._variableBusWidth);
                
        _compileObject = new CompileObject(sourceString, className, _additionalSourceFiles);
        
        if (_compileObject.getCompileStatus() == CompileStatus.COMPILED_SUCCESSFULL) {
            findAndLoadClass();
        }

        // repaint schematic entry - because color of JavaCode-Block could change            
        SchematischeEingabe2.Singleton._circuitSheet.repaint();
    }

    private boolean checkIfCompilationRequired() {

        // test if the java block code changed from last compilation
        final String newSourceString = SourceFileGenerator.createSourceCode(_javaBlockSource,
                _compileObject.getClassName(), _reglerJavaBlock.YOUT.size(),
                _reglerJavaBlock._variableBusWidth);
        final String oldSourceString = _compileObject.getSourceCode();

        if (!newSourceString.equals(oldSourceString)) {
            return true;
        }

        // test if one of the external files changed:        
        final Map<String, CompiledClassContainer> nameClassMap = _compileObject.getClassNameFileMap();

        final Set<String> compiledFileNames = nameClassMap.keySet();

        // first we check if any file was added / removed, by emptying the removeList:
        final Set<String> removeList = new TreeSet<String>();
        for (String fileName : compiledFileNames) {
            removeList.add(fileName + ".java");
        }

        removeList.remove(_compileObject.getClassName() + ".java");

        for (GeckoFile geckoFile : _additionalSourceFiles) {
            if (!removeList.contains(geckoFile.getName())) {
                return true; // a external file was newly added to the javablock... recompile required
            }
            removeList.remove(geckoFile.getName());
        }

        if (!removeList.isEmpty()) { // a external file was removed... recompile required
            return true;
        }

        return false;
    }

    void compileNewBlockSource(final JavaBlockSource newSourceCode) {
        _javaBlockSource = newSourceCode;
        SchematischeEingabe2.Singleton.setDirtyFlag();

        try {
            doCompilationIfRequired();
        } catch (IOException ex) {
            Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getCompilerSource() {
        return _compileObject.getSourceCode();
    }

    public String getCompilerMessage() {
        return _compileObject.getCompilerMessage();
    }

    JavaBlockSource getBlockSourceCode() {
        return _javaBlockSource;
    }

    void initialize(final double[][] inputSignals,
            final double[][] outputSignals) throws Exception {
        if (_compileObject.getCompileStatus() == CompileStatus.NOT_COMPILED) {
            doCompilationIfRequired();
        } else if (_compileObject.getCompileStatus() == CompileStatus.COMPILE_ERROR) {
            throw new GeckoRuntimeException("Could not compile Java-Block!");
        }

        doInitialize(inputSignals, outputSignals);
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException"})
    abstract void calculateYOUT(final double time, final double deltaT, final double[][] inputSignals,
            final double[][] outputSignals) throws Exception;

    abstract void doInitialize(final double[][] xIN, final double[][] yOUT);

    void exportIndividualCONTROL(final StringBuffer ascii) {
        try {
            if (_compileObject.getCompileStatus() == CompileStatus.NOT_COMPILED) {
                doCompilationIfRequired();
            }
        } catch (Exception ex) {
            Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, "could not find class.", ex);
        }

        _javaBlockSource.exportIndividualCONTROL(ascii);

        ascii.append("\n<extraSourceFiles>");
        if (!_additionalSourceFiles.isEmpty()) {
            for (GeckoFile file : _additionalSourceFiles) {
                ascii.append('\n');
                ascii.append(file.getHashValue());
            }
        }
        ascii.append("\n<\\extraSourceFiles>");

        DatenSpeicher.appendAsString(ascii.append("\nclassName"), _compileObject.getClassName());
        DatenSpeicher.appendAsString(ascii.append("\nCompileStatus"), _compileObject.getCompileStatus().ordinal());

        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ObjectOutputStream oOutStream = new ObjectOutputStream(baos);
            oOutStream.writeObject(_classNameFileMap);
            oOutStream.close();
            final byte[] outBytes = baos.toByteArray();
            DatenSpeicher.appendAsString(ascii.append("\nclassMapBytes"), outBytes);
        } catch (IOException ex) {
            Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    void importIndividualCONTROL(final TokenMap tokenMap) {
        _javaBlockSource = new JavaBlockSource(tokenMap);
        if (!tokenMap.containsToken("classMapBytes[]")) {
            return;
        }

        try {
            byte[] inBytes = new byte[0];
            inBytes = tokenMap.readDataLine("classMapBytes[]", inBytes);

            final ByteArrayInputStream bais = new ByteArrayInputStream(inBytes);
            final ObjectInputStream oInStream = new ObjectInputStream(bais);
            final Map<String, CompiledClassContainer> classMap = (Map<String, CompiledClassContainer>) oInStream.readObject();
            _classNameFileMap = classMap;
        } catch (IOException ex) {
            Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);            
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        int compileOrdinal = CompileStatus.NOT_COMPILED.ordinal();
        compileOrdinal = tokenMap.readDataLine("CompileStatus", compileOrdinal);
        final CompileStatus compStatus = CompileStatus.getFromOrdinal(compileOrdinal);
        String className = "";
        className = tokenMap.readDataLine("className", className);
        _compileObject = new CompileObjectSavedFile(className, _classNameFileMap, compStatus);
        try {
            if (_compileObject.getCompileStatus() == CompileStatus.COMPILED_SUCCESSFULL) {
                findAndLoadClass();
            }
        } catch (java.lang.UnsupportedClassVersionError classVersionError) {
            resetCompileObject();
            System.err.println(classVersionError.getMessage());
            //classVersionError.printStackTrace();            
        }

    }

    void resetCompileObject() {
        _compileObject = new CompileObjectNull();
    }

    private void saveSourcesForDebug(final String className, final String sourceCode) {
        FileWriter fstream = null;
        try {
            fstream = new FileWriter("/home/andy/tmp/" + className + ".java");
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(sourceCode);
            out.close();
            fstream.close();
        } catch (IOException ex) {
            Logger.getLogger(AbstractJavaBlock.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fstream.close();
            } catch (IOException ex) {
                Logger.getLogger(AbstractJavaBlock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    final void createNewJavaSourceCopy(AbstractJavaBlock returnValue) {
        returnValue._javaBlockSource = new JavaBlockSource.Builder().sourceCode(
                this._javaBlockSource._sourceCode).
                importsCode(this._javaBlockSource._importsCode).
                initCode(this._javaBlockSource._initCode).variablesCode(
                        this._javaBlockSource._variablesCode).
                build();
    }

}
