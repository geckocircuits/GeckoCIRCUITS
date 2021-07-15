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

import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.allg.GlobalFilePathes;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Our custorm class loader, which allows to load the class files that were compiled internally.
 *
 * @author andreas
 */
public final class JavaBlockClassLoader extends ClassLoader {
    
    private final Map<String, CompiledClassContainer> _classMap;

    public JavaBlockClassLoader(final Map<String, CompiledClassContainer> classMap) {
        /**
         * be careful here! At the beginning, I tried the ClassLoader.getSystemClassLoader()
         * as parent for this classLoader. This works, but when GeckoCIRCUITS is run as
         * applet in the browser, the classloader won't find ControlCalculatable.jar.
         * But, when using the getContextClassLoader(), it will work.
         * 
         */
        
        super(Thread.currentThread().getContextClassLoader());
        
        _classMap = classMap;
        
    }

    /**
     * this ensables to create local classes in the .ipes-folder, this classload will find them, then.
     */
    private void extendClassPath() {


        if (!Fenster.IS_APPLET) {
            final URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            final Class<URLClassLoader> sysclass = URLClassLoader.class;

            final File tmpfile = new File(GlobalFilePathes.DATNAM);
            final File file = new File(tmpfile.getAbsolutePath());
            final File directory = file.getParentFile();
            if (directory.isDirectory()) {
                try {
                    final String path = directory.getAbsolutePath();
                    final URL url = new URL("file://" + path + "/");
                    final Method method = sysclass.getDeclaredMethod("addURL", URL.class);
                    method.setAccessible(true);
                    method.invoke(sysloader, new Object[]{url});
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalArgumentException ex) {
                    Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SecurityException ex) {
                    Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
                } catch (MalformedURLException ex) {
                    Logger.getLogger(ReglerJavaFunction.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    @Override
    protected Class<?> findClass(final String name) throws
            ClassNotFoundException {

        // extendClassPath();
        if (_classMap.containsKey(name)) {   
            final byte[] classBytes = _classMap.get(name).getClassBytes();
            return defineClass(name, classBytes, 0, classBytes.length);
        } else {
            try {
                final File tmpfile = new File(GlobalFilePathes.DATNAM);
                final File file = new File(tmpfile.getAbsolutePath());
                final String path = file.getAbsolutePath();
                final URL url = new URL("file://" + path + "/");
                System.out.print(url);

                return Class.forName(name, true, new URLClassLoader(new URL[] { url }));
            } catch (Exception ex) {

            }
            return null;
            // return super.findClass(name);
        }
    }
}
