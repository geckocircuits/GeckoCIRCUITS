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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.LeitverlusteMesskurve;
import ch.technokrat.gecko.geckocircuits.circuit.losscalculation.VerlustBerechnungDetailed;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author andy
 */
public class DiodeCharacteristic {
    private DiodeSegment[] _diodeSegments;

    public DiodeSegment activeSegment;
    int activeIndex = 0;
    
    DiodeCharacteristic(VerlustBerechnungDetailed lossCalculationDetailed) {
        
        LeitverlusteMesskurve curve = lossCalculationDetailed._messkurvePvCOND.get(0);
        double[][] data = curve.data;
        
        final List<DiodeSegment> diodeSegments = new ArrayList<DiodeSegment>();
        for(int i = 0; i < data[0].length; i++) {                       
            if( i > 0) {
                DiodeSegment ds = new DiodeSegment(data[0][i-1], data[0][i], data[1][i-1], data[1][i]);                
                if(i == 1) {
                    ds.setLargeLowerVoltage();
                }
                if(i == data[0].length -1) {
                    ds.setLargeUpperVoltage();                    
                }
                diodeSegments.add(ds);
            }
        }        
        
        _diodeSegments = diodeSegments.toArray(new DiodeSegment[diodeSegments.size()]);        
        activeSegment = _diodeSegments[0];
        activeIndex = 0;
    }    

    public boolean testIfWrongSegment(double time, double voltage, double stoergroesse, double acceptanceThreshold) {
        int result = activeSegment.testIfInInterval(time, voltage, stoergroesse, acceptanceThreshold);
        if(result == 0) return false;                           
        activeIndex += result;                
        activeSegment = _diodeSegments[activeIndex];                
       
        return true;
    }
}


