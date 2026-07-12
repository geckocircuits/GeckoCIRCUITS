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
package gecko.geckocircuits.general;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URL;

/**
 *
 * @author andy
 */
@SuppressFBWarnings(value = "MS_CANNOT_BE_FINAL",
        justification = "Static fields are intentionally mutable - they store runtime file paths and recent circuits loaded during application lifecycle")
public class GlobalFilePathes {
    //------------------------
    // // Path for storing all images used:
    public static String RECENT_CIRCUITS_1 = "", RECENT_CIRCUITS_2 = "", RECENT_CIRCUITS_3 = "", RECENT_CIRCUITS_4 = "";
    //------------------------
    // // Path in which the current JAR file is located -->
    public static URL PFAD_PICS_URL;  // gleich wie 'PFAD_PICS'
    // // Path and name of the current file for the circuit simulation (*.ipes):
    public static String PFAD_JAR_HOME;

    // // --> is important if the path structure has been changed --> this will update local paths, see ProjectData.localizeRelativePath()
    public static String DATNAM;
    // this is the file path from where the original ipes file was loades. Be cautious, here:
    // this is the file path from where the original ipes file was loades. Be cautious, here:
    public static final String DATNAM_NOT_DEFINED = "not_defined";

    // this is the file path from where the original ipes file was loades. Be cautious, here:
    // this file path does not change when the user saves the file to another location. It shows
    // only the file path from where the stuff was originally loades.
    // This can maybe removed in the future. I keep it here for backwards-compatibility, since
    // somebody is using this field at the moment in a Java-Block.
    public static String datnamAbsLoadIPES;
}
