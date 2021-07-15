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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

import java.io.Serializable;

// Datenbehaelter fuer eine Messkurve -->
public class SwitchingLossCurve extends LossCurve implements Serializable {
    private static final double DEFAULT_BLOCKING_VOLTAGE = 300;

    final UserParameter<Double> _uBlock = UserParameter.Builder.
            <Double>start("uBlock", DEFAULT_BLOCKING_VOLTAGE).            
            longName(I18nKeys.CURVE_MEASURED_AT_VOLTAGE).
            shortName("uMeasure").
            unit("V").            
            build();            
    
    
    // Datenbehaelter mit folgendem Format fuer  data[][] -->
    // I [A] - Eon [Ws] - Eoff [Ws] - Etotal
    // ..      ..         ..          ..
    // ..      ..         ..          ..
    // usw.
    // Parameter: T_junction, uBlock --> bei der Messung vorgegeben
    //
    public SwitchingLossCurve(double tj, double uBlock) {        
        this.tj.setValueWithoutUndo(tj);
        _uBlock.setValueWithoutUndo(uBlock);
    }

    public SwitchingLossCurve copy() {
        SwitchingLossCurve copy = new SwitchingLossCurve(-1, -1);
        copy.setCurveData(getCurveData());
                
        copy.tj.setValueWithoutUndo(this.tj.getValue());
        copy._uBlock.setValueWithoutUndo(this._uBlock.getValue());
        return copy;
    }
               
    
    String getXMLTag() {
        return "SchaltverlusteMesskurve";
    }        

    @Override
    protected void exportIndividual(final StringBuffer ascii) {
        _uBlock.writeXMLToFile(ascii);
    }

    @Override
    protected void importIndividual(final TokenMap tokenMap) {        
        _uBlock.readFromTokenMap(tokenMap);                        
        
        if(data.length == 4) { // repair wrong data count...            
            double[][] tmpData = new double[3][data[0].length];
            for(int i = 0; i < tmpData.length; i++) {
                for(int j = 0; j < tmpData[i].length; j++) {
                    tmpData[i][j] = data[i][j];
                }
            }
            data = tmpData;
        }                
    }                                     
}
