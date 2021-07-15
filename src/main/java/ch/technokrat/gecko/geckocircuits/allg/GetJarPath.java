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
package ch.technokrat.gecko.geckocircuits.allg;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * Factor out all Typ path information here.
 *
 * @todo Clean this up -> Merged with Andy's get rid of the variables etc.
 * @todo _unsaved belongs in Style.
 * @todo Maybe we want diffrent stuff for default and current fname?
 * @todo I suspect _undefined should be gotten rid of when time allows.
 *
 * WARNING: Somebody told me, that it would also be possible to get the jarpath
 * via: final String path =
 * GetJarPath.class.getProtectionDomain().getCodeSource().getLocation().getPath();
 * final String decodedPath = URLDecoder.decode(path, "UTF-8"); However, this
 * does not work, when using GeckoCIRCUITS as applet, since the security manager
 * does not allow this approach. Therefore, the way I do this is a bit more
 * complicated.
 *
 */
public class GetJarPath {

    private static boolean _initialized;
    private static String _JARpath;
    private static String _JARFilePath;
    private static Class _refToCallingPackage;
    private static GetJarPath _gjp;
    private static final String BUILD_CLASSES_STRING = "/build/classes";
    private static final String TARGET_CLASSES_STRING = "/target/classes";

    private static void initializeWithOwnClassRef() {
        if (!_initialized) {
            _gjp = new GetJarPath(GetJarPath.class);
        }
    }

    //public static final String spTitle = "  ";
    //public static final String spTitleX = "  -  ";
    /**
     * we moved the class to another package - therefore the whole
     * getJarPath-Procedure needs to have a class from the calling package as
     * initialization. therefore private constructor.
     *
     * @param clazz
     */
    public GetJarPath(final Class clazz) {
        synchronized (this) {
            _refToCallingPackage = clazz;
            _initialized = true;
            setJarPath();
        }
    }

    /**
     * This bit of code seems to occur in a lot of places, so I factor it out
     * here. The commented out try/catch seems unnecessary, but put it back in
     * if it's needed.
     *
     * @return a valid dat name.
     */
    /*
     public static String getDatNam() {
     String retval;
     //try {
     retval = _datName;
     if (retval.equals(DATNAM_NOT_DEFINED)) {
     retval = JAR;
     }
     return retval;
     //} catch (Exception e) {
     //    dialog.setCurrentDirectory(new File(Paths.JAR));
     // }
     }
    
     */
    /**
     * i made this function private... it is only used from getJarPath, and
     * returns a mixture between URL and filepath... nobody outside should see
     * and use this here!
     *
     * @return
     */
    private static String getJarPathInsideJAR() {
        String path = "";
        try {
            path = URLDecoder.decode(_refToCallingPackage.getResource(_refToCallingPackage.getSimpleName() + ".class").toString(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(GetJarPath.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (path.startsWith("jar")) {
            path = path.replaceAll("jar:", "");
        } else {
            path = path.replaceAll(_refToCallingPackage.getSimpleName() + ".class", "");
        }

        System.out.println(path);

        // path is coming from an URL - when a space is encountered here, it will
        // be converted to a %20-Character!?!
        path = path.replace("%20", " ");
        return path;
    }

    private void setJarPath() {
        String path = getJarPathInsideJAR();

        // remove preceeding file:-String. Be careful, Linux and Windows require
        // different handling:
        if (path.startsWith("file:")) {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                path = path.substring("file:/".length());
            } else {
                path = path.substring("file:".length());
            }
        }

        // this indicates that we run the program out of the development IDE,
        // directly from the classes:



        if (path.contains(BUILD_CLASSES_STRING)) {
            final int index = path.indexOf(BUILD_CLASSES_STRING);
            path = path.substring(0, index + BUILD_CLASSES_STRING.length() + 1);
        } else if (path.contains(TARGET_CLASSES_STRING)) {
            final int index = path.indexOf(TARGET_CLASSES_STRING);
            path = path.substring(0, index + TARGET_CLASSES_STRING.length() + 1);
        } else {
            // this program must have been started from a jar-file. Remove
            // everything of the path inside the jar:
            final int jarIndex = path.indexOf(".jar!");
            _JARFilePath = path.substring(0, jarIndex + 4);
            System.out.println(path);
            path = path.substring(0, jarIndex);
            final int lastDelimiterIndex = path.lastIndexOf('/');
            path = path.substring(0, lastDelimiterIndex + 1);
        }


        // finally, make a test if the directory is exisiting. If not write out an
        // error message!
        if (!Fenster.IS_APPLET && !Fenster.IS_BRANDED) {
            final File testFile = new File(path);
            if (!testFile.isDirectory()) {
                System.err.println("Error: jar-Path is not a directory!");
            }
        }
        _JARpath = path;
    }

    /**
     * If initialized, returns _JARpath. Otherwise, it determines the correct
     * jarpath.
     *
     * @return path to the DIRECTORY of our own jar-class. If we run the program
     * from the IDE, the corresponding path to the build-directory is returned.
     */
    public static String getJarPath() {
        initializeWithOwnClassRef();

        final File file = new File(_JARpath);
        if (!Fenster.IS_APPLET && !Fenster.IS_BRANDED && !file.isDirectory()) {
            JOptionPane.showMessageDialog(null,
                    "Could not find path to GeckoCIRCUITS installation: \n" + file.getAbsolutePath() + "\n"
                    + "Probably non-ASCII-Characters are not resolved properly. "
                    + "\nProgram is exiting now.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(4);
        }
        return _JARpath;
    }

    /**
     * If initialized, returns _JARFilePath - this is the path to the
     * GeckkoCIRCUITS.jar, including the jar-File name. If started from within
     * netbeans, an empty string is returned.
     *
     * @return path to the FILE of our own jar-class. If we run the program from
     * the IDE, an empty string is returned.
     */
    public static String getJarFilePath() {
        initializeWithOwnClassRef();
        return _JARFilePath;
    }

    public static URL getPathPICS() {
        URL pfadURL = null;
        try {
            pfadURL = GetJarPath.class.getResource(GetJarPath.class.getSimpleName() + ".class").toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pfadURL;
    }
}
