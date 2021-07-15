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

import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import java.io.Serializable;

/**
 *
 * @author andreas
 */
public final class CompiledClassContainer implements Serializable {
    private static final long serialVersionUID = 3647247362711L;
    
    private final byte[] _classBytes;
    private final String _sourceString;

    public CompiledClassContainer() {
        _classBytes = null;
        _sourceString = "";
    }
    
    public CompiledClassContainer(final byte[] classBytes, final String sourceString) {
        _classBytes = new byte[classBytes.length];
        _sourceString = sourceString;
        System.arraycopy(classBytes, 0, _classBytes, 0, classBytes.length);  
    }
    
    
    public CompiledClassContainer(final TokenMap tokenMap) {
        if (tokenMap.containsToken("classBytesNew[]")) {
            _classBytes = tokenMap.readDataLine("classBytesNew[]", new byte[0]);
        } else {
            _classBytes = null;
        }
        _sourceString = "";
    }
    
    public byte[] getClassBytes() {
        final byte[] returnValue = new byte[_classBytes.length];
        System.arraycopy(_classBytes, 0, returnValue, 0, _classBytes.length);
        return returnValue;
    }
    
    public String getSourceString() {
        return _sourceString;
    }
}
