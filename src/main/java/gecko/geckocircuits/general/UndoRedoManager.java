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

import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.util.zip.InflaterInputStream;

public class UndoRedoManager {

    //------------------------------------------------------
    private int undoZustaendeMAX = 20;
    private byte[][] undoZustaende, redoZustaende;
    private int pointerToUndoStates, zeigerAufRedoZustaende;
    private boolean undoRingmemoryFirstExceeded;
    private int undoNumber, redoAnzahl;  // soviele Undo- und Redo-Aktionen sind moeglich
    //------------------------------------------------------

    public UndoRedoManager() {
        this.init();
    }

    // Neue Datei, Open File usw. -->
    public void init() {
        undoZustaende = new byte[undoZustaendeMAX][];
        redoZustaende = new byte[undoZustaendeMAX][];
        pointerToUndoStates = 0;
        zeigerAufRedoZustaende = 0;
        undoRingmemoryFirstExceeded = false;
        undoNumber = 0;
        redoAnzahl = 0;
    }

    public void speichereAutomatischAktuellenZustandFuerUndoRedo(Object data) {
        zeigerAufRedoZustaende = 0;  // dh. Redo nicht moeglich (Redo nur nach Undo moeglich)
        redoAnzahl = 0;
        //--------------
        //--------------
        //--------------
        //--------------
        //--------------
        //--------------
        /*
        try {
        ByteArrayOutputStream outByteArray= new ByteArrayOutputStream();
        ObjectOutputStream out= new ObjectOutputStream(new DeflaterOutputStream(outByteArray));
        out.writeObject(data);
        out.flush();
        out.close();
        byte[] zustand= outByteArray.toByteArray();
        //---------
        if (undoNumber<undoZustaendeMAX-1) undoNumber++;
        undoZustaende[pointerToUndoStates]= zustand;
        pointerToUndoStates++;
        if (pointerToUndoStates==undoZustaendeMAX) {
        undoRingmemoryFirstExceeded= true;
        pointerToUndoStates= 0;
        }
        //---------
        } catch (Exception e) {
        //---------
        System.out.println(e+"  e0finv'");
        this.init();
        }
         */
        //System.out.println("pointerToUndoStates= "+pointerToUndoStates+"\t\tzeigerAufRedoZustaende= "+zeigerAufRedoZustaende);
        //--------------
    }

    public Object undo() {
        undoNumber--;
        pointerToUndoStates--;
        int zeiger = pointerToUndoStates - 1;
        if (undoRingmemoryFirstExceeded && (pointerToUndoStates == -1)) {
            pointerToUndoStates = undoZustaendeMAX - 1;
            zeiger = pointerToUndoStates - 1;
        } else if (undoRingmemoryFirstExceeded && (pointerToUndoStates == 0)) {
            zeiger = undoZustaendeMAX - 1;
        }
        byte[] zustand = undoZustaende[zeiger];
        Object data = null;
        //---------
        try {
            ByteArrayInputStream inByteArray = new ByteArrayInputStream(zustand);
            ObjectInputStream in = new ObjectInputStream(new InflaterInputStream(inByteArray));
            data = in.readObject();
            in.close();
        } catch (Exception e) {
            System.out.println(e + "   e0oiv00'er");
        }
        //---------
        //System.out.println("pointerToUndoStates= "+pointerToUndoStates+"\t\tzeigerAufRedoZustaende= "+zeigerAufRedoZustaende);
        if (redoAnzahl < undoZustaendeMAX - 1) {
            redoAnzahl++;
        }
        redoZustaende[zeigerAufRedoZustaende] = undoZustaende[pointerToUndoStates];
        zeigerAufRedoZustaende++;
        //---------
        return data;
    }

    public Object redo() {
        redoAnzahl--;
        zeigerAufRedoZustaende--;
        byte[] zustand = redoZustaende[zeigerAufRedoZustaende];
        Object data = null;
        //---------
        try {
            ByteArrayInputStream inByteArray = new ByteArrayInputStream(zustand);
            ObjectInputStream in = new ObjectInputStream(new InflaterInputStream(inByteArray));
            data = in.readObject();
            in.close();
        } catch (Exception e) {
            System.out.println(e + "   e0oiv00'er");
        }
        //---------
        if (undoNumber < undoZustaendeMAX - 1) {
            undoNumber++;
        }
        undoZustaende[pointerToUndoStates] = redoZustaende[zeigerAufRedoZustaende];
        pointerToUndoStates++;
        if (pointerToUndoStates == undoZustaendeMAX) {
            pointerToUndoStates = 0;
        }
        //---------
        return data;
    }

    public boolean undoMoeglich() {
        if ((!undoRingmemoryFirstExceeded) && (undoNumber <= 1)) {
            return false;
        }
        return undoNumber > 0;
    }

    public boolean redoMoeglich() {
        return redoAnzahl > 0;
    }
}
