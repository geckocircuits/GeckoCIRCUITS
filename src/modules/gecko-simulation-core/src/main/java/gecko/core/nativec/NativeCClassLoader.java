/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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

package gecko.core.nativec;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Custom classloader for native C/C++ integration that can be garbage collected.
 *
 * <p>This classloader loads Java wrapper classes for native libraries and can be
 * discarded to allow native library unloading through garbage collection.
 *
 * @author DIEHL Controls Ricardo Richter
 * @since Core Module Extraction Sprint - Phase 2
 */
public class NativeCClassLoader extends ClassLoader {



    @Override
    public String toString() {
        return NativeCClassLoader.class.getName();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        final byte[] classBytes;
        java.io.InputStream resourceStream = getSystemResourceAsStream(
                name.replace(".", "/") + ".class");

        if (resourceStream == null) {
            throw new ClassNotFoundException("Could not find class: " + name);
        }

        BufferedInputStream inBuff = new BufferedInputStream(resourceStream);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int i;
        try {
            while ((i = inBuff.read()) != -1) {
                out.write(i);
            }
            inBuff.close();
            classBytes = out.toByteArray();
            out.close();
            return defineClass(name, classBytes, 0, classBytes.length);
        } catch (IOException ioe) {
            throw new ClassNotFoundException("Error reading class: " + name, ioe);
        }
    }

}
