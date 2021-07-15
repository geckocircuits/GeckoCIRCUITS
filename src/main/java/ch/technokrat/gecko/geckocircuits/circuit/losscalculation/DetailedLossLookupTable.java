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

import java.util.List;

public class DetailedLossLookupTable {

    static DetailedLossLookupTable fabric(final List<? extends LossCurve> messkurvePvSWITCH, 
            final int dataIndex) {
        double[][] currentValues = new double[messkurvePvSWITCH.size()][];
        double[][] energyValues = new double[messkurvePvSWITCH.size()][];
        double[] temperatures = new double[messkurvePvSWITCH.size()];
        for(int i = 0; i < messkurvePvSWITCH.size(); i++) {
            LossCurve curve = messkurvePvSWITCH.get(i);
            currentValues[i] = curve.data[0];            
            energyValues[i] = new double[curve.data[dataIndex].length];            
            System.arraycopy(curve.data[dataIndex], 0, energyValues[i], 0, energyValues[i].length);                        
            if(messkurvePvSWITCH.get(0) instanceof SwitchingLossCurve) {
                for(int j = 0; j < energyValues[i].length; j++) {
                    double curveBlockingVoltage = ((SwitchingLossCurve) curve)._uBlock.getDoubleValue();
                    energyValues[i][j] /= curveBlockingVoltage;                    
                }
            }
            temperatures[i] = curve.tj.getDoubleValue();
        }
        
        return new DetailedLossLookupTable(currentValues, energyValues, temperatures);
    }

    private final double[][] _energyValues;
    private final double[][] _currentValues;
    private final double[] _temperatures;
    private final boolean _allCurrentsPositive;
    
    private DetailedLossLookupTable(final double[][] currentValues, final double[][] energyValues, final double[] temperatures) {
        _energyValues = energyValues;
        _currentValues = currentValues;
        
        boolean hasNegativeCurrent = false;
        for(int i = 0; i < _currentValues.length; i++) {
            for(int j = 0; j < _currentValues[i].length; j++) {
                if(_currentValues[i][j] < 0) {
                    hasNegativeCurrent = true;
                }
            }
        }
        _allCurrentsPositive = !hasNegativeCurrent;
        
        _temperatures = temperatures;
        assert _temperatures.length == _currentValues.length;
        assert _energyValues.length == _temperatures.length;        
    }

    public double getInterpolatedYValue(double temp, double current) {
        int upperTempIndex = 0;
        while (upperTempIndex < _temperatures.length - 1 && temp > _temperatures[upperTempIndex]) {
            upperTempIndex++;
        }

        int lowerTempIndex = upperTempIndex - 1;
        if (lowerTempIndex < 0) {
            lowerTempIndex = 0;
        }
                
        double[] data = _energyValues[upperTempIndex];
        
        double signCorrectedCurrent = current;
        
        if(_allCurrentsPositive) {
            signCorrectedCurrent = Math.abs(current);
        }
        
        double upperTempEnergy = findLossValueOnCurve(signCorrectedCurrent,  _currentValues[upperTempIndex], data);

        if (upperTempIndex == lowerTempIndex) {
            return upperTempEnergy;
        }
        
        data = _energyValues[lowerTempIndex];
        double lowerTempEnergy = findLossValueOnCurve(signCorrectedCurrent, _currentValues[lowerTempIndex], data);

        double upperTemp = _temperatures[upperTempIndex];
        double lowerTemp = _temperatures[lowerTempIndex];

        double wheigt2 = (temp - lowerTemp) / (upperTemp - lowerTemp);
        double wheigt1 = (upperTemp - temp) / (upperTemp - lowerTemp);

        assert wheigt1 + wheigt2 < 1.01 && wheigt1 + wheigt2 > 0.99;
        double returnValue = (lowerTempEnergy * wheigt1 + upperTempEnergy * wheigt2);
        // only return positive energies!        
        return Math.max(returnValue, 0);
    }
    
    public double getInterpolatedXValue(double temp, double yValue) {        
        int upperTempIndex = 0;
        while (upperTempIndex < _temperatures.length - 1 && temp > _temperatures[upperTempIndex]) {            
            upperTempIndex++;
        }

        int lowerTempIndex = upperTempIndex - 1;
        if (lowerTempIndex < 0) {
            lowerTempIndex = 0;
        }
                        
        double upperTempEnergy = findLossValueOnCurve(yValue, _energyValues[upperTempIndex], _currentValues[upperTempIndex]);        
        if (upperTempIndex == lowerTempIndex) {
            return upperTempEnergy;
        }
                
        double lowerTempEnergy = findLossValueOnCurve(yValue, _energyValues[lowerTempIndex], _currentValues[lowerTempIndex]);
                
        double upperTemp = _temperatures[upperTempIndex];
        double lowerTemp = _temperatures[lowerTempIndex];

        double wheigt2 = (temp - lowerTemp) / (upperTemp - lowerTemp);
        double wheigt1 = (upperTemp - temp) / (upperTemp - lowerTemp);

        assert wheigt1 + wheigt2 < 1.01 && wheigt1 + wheigt2 > 0.99;
        double returnValue = (lowerTempEnergy * wheigt1 + upperTempEnergy * wheigt2);
        // changed: negative values are possible here, since then both voltage and current could be negative!
        return returnValue;
    }

    private double findLossValueOnCurve(double searchXValue, double[] xValues, double[] yValues) {
        int rightXIndex = 0;

        while (xValues[rightXIndex] <= searchXValue && rightXIndex < xValues.length - 1) {            
            rightXIndex++;
        }                
                
        
        int leftXIndex = rightXIndex - 1;

        if(leftXIndex < 0) {
            leftXIndex = 0;
            rightXIndex = 1;
        }                        
        
        double en1 = yValues[leftXIndex];                                       
        double en2 = yValues[rightXIndex];
        double xLeft = xValues[leftXIndex];
        double xRight = xValues[rightXIndex];                                
        
        double wheigt2 = (searchXValue - xLeft) / (xRight - xLeft);
        double wheigt1 = (xRight - searchXValue) / (xRight - xLeft);

        assert wheigt1 + wheigt2 < 1.01 && wheigt1 + wheigt2 > 0.99;
        double returnValue = (en1 * wheigt1 + en2 * wheigt2);                
        return returnValue;
    }
        
}
