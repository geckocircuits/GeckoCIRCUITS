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
package gecko.geckocircuits.circuit.losscalculation;

import java.io.Serializable;

// Datenbehaelter fuer eine Messkurve -->
public class LeitverlusteMesskurve extends LossCurve implements Serializable {    
   
    // Datenbehaelter mit folgendem Format fuer  data[][] -->
    // U [V] - I [A]
    // ..      ..
    // ..      ..
    // usw.
    // Parameter: T_junction --> bei der Messung vorgegeben
    //
    public LeitverlusteMesskurve(double tj) {
        this.tj.setValueWithoutUndo(tj);
    }

    public LeitverlusteMesskurve copy() {
        LeitverlusteMesskurve copy = new LeitverlusteMesskurve(-1);
        copy.data = new double[this.data.length][this.data[0].length];
        for (int i1 = 0; i1 < this.data.length; i1++) {
            for (int i2 = 0; i2 < this.data[0].length; i2++) {
                copy.data[i1][i2] = this.data[i1][i2];
            }
        }
        copy.tj.setValueWithoutUndo(this.tj.getValue());
        return copy;
    }            

    @Override
    String getXMLTag() {
        return "LeitverlusteMesskurve";
    }                
}
