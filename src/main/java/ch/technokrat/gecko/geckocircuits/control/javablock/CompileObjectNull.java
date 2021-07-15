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


/**
 * NULL-Pattern: if no compilation is done, the compile object can be initialized with
 * the CompileObjectNull
 * @author andreas
 */
public final class CompileObjectNull extends AbstractCompileObject {
    
    private CompileStatus _compileStatus = CompileStatus.NOT_COMPILED;

    @Override
    public CompileStatus getCompileStatus() {
        return _compileStatus;
    }
    

    @Override
    public String getCompilerMessage() {
        return "This class is not yet compiled!";
    }

    @Override
    public String getClassName() {
        return "NotCompiled";
    }

    @Override
    public String getSourceCode() {
        return "This class is not yet compiled!";
    }

    @Override
    void setErrorStatus() {
        _compileStatus = CompileStatus.COMPILE_ERROR;
    }
    
}
