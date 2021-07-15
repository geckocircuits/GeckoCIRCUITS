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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.DeflaterOutputStream;

public class UndoRedoManager {

    //------------------------------------------------------
    private int undoZustaendeMAX = 20;
    private byte[][] undoZustaende, redoZustaende;
    private int zeigerAufUndoZustaende, zeigerAufRedoZustaende;
    private boolean undoRingspeicherErstmalsUeberschritten;
    private int undoAnzahl, redoAnzahl;  // soviele Undo- und Redo-Aktionen sind moeglich 
    //------------------------------------------------------

    public UndoRedoManager() {
        this.init();
    }

    // Neue Datei, Open File usw. --> 
    public void init() {
        undoZustaende = new byte[undoZustaendeMAX][];
        redoZustaende = new byte[undoZustaendeMAX][];
        zeigerAufUndoZustaende = 0;
        zeigerAufRedoZustaende = 0;
        undoRingspeicherErstmalsUeberschritten = false;
        undoAnzahl = 0;
        redoAnzahl = 0;
    }

    public void speichereAutomatischAktuellenZustandFuerUndoRedo(Object daten) {
        zeigerAufRedoZustaende = 0;  // dh. Redo nicht moeglich (Redo nur nach Undo moeglich) 
        redoAnzahl = 0;
        //--------------
        // ACHTUNG: Der folgende Bereich wird voruebergehend auskommentiert, damit die Undo/Redo-Buttons deaktiviert sind 
        // der komplette Undo/Redo-Mechanismus muss ueberarbeitet werden, weil die hier implementierte Speicherung des kompletten 
        // Zustandes immer wieder zu massiven Problemen (va. RAM-Speicher) fuehren, weil der RAM-Speicher mit dem OSZI-ReglerBlock 
        // verknuepft ist 
        // --> unteren Bereich NICHT  LOESCHEN!!!
        /*
        try {
        ByteArrayOutputStream outByteArray= new ByteArrayOutputStream();
        ObjectOutputStream out= new ObjectOutputStream(new DeflaterOutputStream(outByteArray));
        out.writeObject(daten); 
        out.flush();
        out.close();
        byte[] zustand= outByteArray.toByteArray();
        //---------
        if (undoAnzahl<undoZustaendeMAX-1) undoAnzahl++; 
        undoZustaende[zeigerAufUndoZustaende]= zustand; 
        zeigerAufUndoZustaende++; 
        if (zeigerAufUndoZustaende==undoZustaendeMAX) {
        undoRingspeicherErstmalsUeberschritten= true; 
        zeigerAufUndoZustaende= 0; 
        }
        //---------
        } catch (Exception e) { 
        // hier landet man, wenn das SCOPE offen ist, und man Neues hinzufuegt, weil SCOPE ein Swing-Element ist 
        System.out.println(e+"  e0finv'");  
        this.init();  
        }
         */
        //System.out.println("zeigerAufUndoZustaende= "+zeigerAufUndoZustaende+"\t\tzeigerAufRedoZustaende= "+zeigerAufRedoZustaende); 
        //--------------
    }

    public Object undo() {
        undoAnzahl--;
        zeigerAufUndoZustaende--;
        int zeiger = zeigerAufUndoZustaende - 1;
        if ((undoRingspeicherErstmalsUeberschritten) && (zeigerAufUndoZustaende == -1)) {
            zeigerAufUndoZustaende = undoZustaendeMAX - 1;
            zeiger = zeigerAufUndoZustaende - 1;
        } else if ((undoRingspeicherErstmalsUeberschritten) && (zeigerAufUndoZustaende == 0)) {
            zeiger = undoZustaendeMAX - 1;
        }
        byte[] zustand = undoZustaende[zeiger];
        Object daten = null;
        //---------
        try {
            ByteArrayInputStream inByteArray = new ByteArrayInputStream(zustand);
            ObjectInputStream in = new ObjectInputStream(new InflaterInputStream(inByteArray));
            daten = in.readObject();
            in.close();
        } catch (Exception e) {
            System.out.println(e + "   e0oiv00'er");
        }
        //---------
        //System.out.println("zeigerAufUndoZustaende= "+zeigerAufUndoZustaende+"\t\tzeigerAufRedoZustaende= "+zeigerAufRedoZustaende); 
        if (redoAnzahl < undoZustaendeMAX - 1) {
            redoAnzahl++;
        }
        redoZustaende[zeigerAufRedoZustaende] = undoZustaende[zeigerAufUndoZustaende];
        zeigerAufRedoZustaende++;
        //---------
        return daten;
    }

    public Object redo() {
        redoAnzahl--;
        zeigerAufRedoZustaende--;
        byte[] zustand = redoZustaende[zeigerAufRedoZustaende];
        Object daten = null;
        //---------
        try {
            ByteArrayInputStream inByteArray = new ByteArrayInputStream(zustand);
            ObjectInputStream in = new ObjectInputStream(new InflaterInputStream(inByteArray));
            daten = in.readObject();
            in.close();
        } catch (Exception e) {
            System.out.println(e + "   e0oiv00'er");
        }
        //---------
        if (undoAnzahl < undoZustaendeMAX - 1) {
            undoAnzahl++;
        }
        undoZustaende[zeigerAufUndoZustaende] = redoZustaende[zeigerAufRedoZustaende];
        zeigerAufUndoZustaende++;
        if (zeigerAufUndoZustaende == undoZustaendeMAX) {
            zeigerAufUndoZustaende = 0;
        }
        //---------
        return daten;
    }

    public boolean undoMoeglich() {
        if ((!undoRingspeicherErstmalsUeberschritten) && (undoAnzahl <= 1)) {
            return false;
        }
        if (undoAnzahl > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean redoMoeglich() {
        if (redoAnzahl > 0) {
            return true;
        } else {
            return false;
        }
    }
}
