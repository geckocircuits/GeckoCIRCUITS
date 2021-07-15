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

import ch.technokrat.gecko.geckocircuits.allg.DatenSpeicher;
import ch.technokrat.gecko.geckocircuits.allg.UserParameter;
import ch.technokrat.gecko.geckocircuits.circuit.TokenMap;
import ch.technokrat.gecko.i18n.resources.I18nKeys;

public abstract class LossCurve {
    
    public double[][] data;
    
    final UserParameter<Double> tj = UserParameter.Builder.
            <Double>start("tj", 0.0).            
            longName(I18nKeys.TEMP_AT_WHICH).
            shortName("curveTemperature").
            unit("C").            
            build();            

    
    final void importASCII(final TokenMap tokenMap) {        
        data = tokenMap.readDataLine("data[][]", data);
                
        importIndividual(tokenMap);
        tj.readFromTokenMap(tokenMap);        
    }

    final void exportASCII(final StringBuffer ascii) {
        
        ascii.append("\n<" + getXMLTag() + ">");
        DatenSpeicher.appendAsString(ascii.append("\ndata"), data);
        tj.writeXMLToFile(ascii);
        exportIndividual(ascii);
        ascii.append("\n<\\" + getXMLTag() + ">");                        
    }
    
    public String getName() {
        return ((int) (double) tj.getValue()) + "°C";
    }

    abstract String getXMLTag();

    protected void exportIndividual(final StringBuffer ascii) {        
        // nothing todo - template method pattern
    }

    protected void importIndividual(final TokenMap tokenMap) {
        // nothing todo - template method pattern
    }
    
    
    public void setCurveData(double[][] newData) {
        this.data = new double[newData.length][];
        for(int i = 0; i < newData.length; i++) {
            data[i] = new double[newData[i].length];
            System.arraycopy(newData[i], 0, data[i], 0, newData[i].length);
        }
    }
    
    public double[][] getCurveData() {
        double[][] returnValue = new double[data.length][];
        for(int i = 0; i < data.length; i++) {
            returnValue[i] = new double[data[i].length];
            System.arraycopy(data[i], 0, returnValue[i], 0, data[i].length);
        }
        return returnValue;
    }
    
}
