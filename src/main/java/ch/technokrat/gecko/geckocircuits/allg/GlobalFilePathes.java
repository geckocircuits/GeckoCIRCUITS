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

import java.net.URL;

/**
 *
 * @author andy
 */
public class GlobalFilePathes {
    // die zuletzt geoeffneten Dateien, RECENT_1 ist der juengste Eintrag 
    // die Pfade werden in den Properties gespeichert, und beim Programmstart geladen --> 
    public static String RECENT_CIRCUITS_1 = "", RECENT_CIRCUITS_2 = "", RECENT_CIRCUITS_3 = "", RECENT_CIRCUITS_4 = "";    
    //------------------------
    // Pfad fuer die Ablage aller verwendeten Bilder:
    public static URL PFAD_PICS_URL;  // gleich wie 'PFAD_PICS'
    // Pfad, in dem das aktuelle JAR-File liegt --> 
    public static String PFAD_JAR_HOME;
    
    // Pfad und Name der aktuellen Datei fuer die Schaltungssimulation (*.ipes):
    public static String DATNAM;
    // Pfad und Name der beim letzten Mal geladenen Datei fuer die Schaltungssimulation (*.ipes):
    // --> ist wichtig, wenn die Pfadstruktur geaendert wurde --> damit werden lokale Pfade aktualisiert, siehe DatenSpeicher.lokalisiereRelativenPfad()
    public static String DATNAM_NOT_DEFINED = "not_defined";
        
    // this is the file path from where the original ipes file was loades. Be cautious, here:
    // this file path does not change when the user saves the file to another location. It shows
    // only the file path from where the stuff was originally loades.
    // This can maybe removed in the future. I keep it here for backwards-compatibility, since
    // somebody is using this field at the moment in a Java-Block.
    public static String datnamAbsLoadIPES;
}
