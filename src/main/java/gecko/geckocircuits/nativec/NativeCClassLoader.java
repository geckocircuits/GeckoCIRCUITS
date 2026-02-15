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

package gecko.geckocircuits.nativec;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Implements a Custom Classloader that can be garbage collected
 * @author DIEHL Controls Ricardo Richter
 */
public class NativeCClassLoader extends ClassLoader {



    @Override
    public String toString() {
        return NativeCClassLoader.class.getName();
    }

    @Override
    @SuppressWarnings("PMD.CloseResource") // resourceStream is wrapped by BufferedInputStream which is closed in try-with-resources
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        final byte[] classBytes;
        java.io.InputStream resourceStream = getSystemResourceAsStream(
                name.replace(".", "/") + ".class");

        if (resourceStream == null) {
            throw new ClassNotFoundException("Could not find class: " + name);
        }

        try (BufferedInputStream inBuff = new BufferedInputStream(resourceStream);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int i;
            while ((i = inBuff.read()) != -1) {
                out.write(i);
            }
            classBytes = out.toByteArray();
            return defineClass(name, classBytes, 0, classBytes.length);
        } catch (IOException ioe) {
            throw new ClassNotFoundException("Error reading class: " + name, ioe);
        }
    }

}