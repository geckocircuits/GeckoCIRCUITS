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

import java.util.Map;

/**
 * This object is read in from the model file. 
 * @author andreas
 */
public final class CompileObjectSavedFile extends AbstractCompileObject {

    private final String _sourceString;
    private final String _sourceFileName;
    private final CompileStatus _compileStatus;
    
    /**
     * Immutable object creation
     * @param sourceString complete .java sourcecode
     * @param sourceFileName javacode object file name
     * @param classNameFileMap
     * @param compileStatus 
     */
    public CompileObjectSavedFile(final String sourceFileName, 
            final Map<String, CompiledClassContainer> classNameFileMap, final CompileStatus compileStatus) {
        super();
        
        if(classNameFileMap != null && classNameFileMap.containsKey(sourceFileName)) {
            _sourceString = classNameFileMap.get(sourceFileName).getSourceString();
        } else {
            _sourceString = "";
        }
        
        _sourceFileName = sourceFileName;
        
        if(classNameFileMap != null) {
            _classMap.putAll(classNameFileMap);
        }
        
        _compileStatus = compileStatus;
    }
        
    @Override
    public CompileStatus getCompileStatus() {
        return _compileStatus;
    }

    @Override
    void setErrorStatus() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getCompilerMessage() {
        return "File was not changed from last compilation!";
    }

    @Override
    public String getClassName() {
        return _sourceFileName;
    }

    @Override
    public String getSourceCode() {
        return _sourceString;
    }
    
}
