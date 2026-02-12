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

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.SimpleJavaFileObject;

/**
 * Class permits the compilation without a physical existing file (e.g. when loaded from
 * Applet. The Java-Compiler gets a handle to RamJavaFileObjects, where it writes its output
 * bytes and / or the compilation status/error messages
 * 
 * @author andreas
 */
class RamJavaFileObject extends SimpleJavaFileObject {

    RamJavaFileObject(final String name, final Kind kind) {
        super(toURI(name), kind);
    }
    
    private ByteArrayOutputStream _baos;

    @Override
    public CharSequence getCharContent(final boolean ignoreEncodingErrors)
            throws IOException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(_baos.toByteArray());
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        _baos = new ByteArrayOutputStream();
        return _baos;
    }
        

    public static URI toURI(final String name) {
        try {
            return new URI(name);
        } catch (URISyntaxException ex) {
            Logger.getLogger(RamJavaFileObject.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public byte[] getByteArray() {
        return _baos.toByteArray();
    }
    
}
