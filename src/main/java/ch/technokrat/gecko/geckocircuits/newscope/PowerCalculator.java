/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.newscope;

import ch.technokrat.gecko.geckocircuits.datacontainer.AbstractDataContainer;

/**
 *
 * @author andy
 */
public final class PowerCalculator {

    private final double[] _powerP;
    private final double[] _powerQ;
    private final double[] _powerD;
    private double[] _powerS;
    private final double[] _lambda;
    private final double[] _cosPhi;
    // just needed internally as helper, therefore private
    private final double[] _rms2U;
    private final double[] _rms2I;
    private final int _noOfPowerAnals;
    private static final int CALC_NUM_HARM = 50;
        
    private PowerCalculator(final AbstractDataContainer worksheet,
            final PowerAnalysisPanel.PowerCalculatorSelectionIndex selectedIndices) {
        
        _noOfPowerAnals = selectedIndices._selCurrentInd.size();
        _powerP = new double[_noOfPowerAnals];
        _powerQ = new double[_noOfPowerAnals];
        _powerD = new double[_noOfPowerAnals];
        _powerS = new double[_noOfPowerAnals];
        _lambda = new double[_noOfPowerAnals];
        _cosPhi = new double[_noOfPowerAnals];
        _rms2U = new double[_noOfPowerAnals];
        _rms2I = new double[_noOfPowerAnals];

        
        int[] voltageIndices = new int[_noOfPowerAnals];
        int[] currentIndices = new int[_noOfPowerAnals];
        
        for(int i = 0; i < _noOfPowerAnals; i++) {
            voltageIndices[i] = selectedIndices._selVoltageInd.get(i)-1;
            currentIndices[i] = selectedIndices._selCurrentInd.get(i)-1;
        }
        
        calculate(worksheet, voltageIndices, currentIndices, selectedIndices.startTime, selectedIndices.stopTime);
    }

    public double getPowerP(final int index) {
        return _powerP[index];
    }
    
    public double getPowerQ(final int index) {
        return _powerQ[index];
    }
    
    public double getPowerD(final int index) {
        return _powerD[index];
    }
    
    public double getPowerS(final int index) {
        return _powerS[index];
    }
    
    public double getLambda(final int index) {
        return _lambda[index];
    }
    
    public double getCosPhi(final int index) {
        return _cosPhi[index];
    }
    
    private void calculate(final AbstractDataContainer worksheet, final int[] voltageIndices, final int[] currentIndices, 
            final double rng1, final double rng2) {

        // Startpunkt finden:
        int startIndex = 0;
        while (worksheet.getTimeValue(startIndex, 0) <= rng1) {
            startIndex++;
        }
        
        
        double deltaT = worksheet.getTimeValue(startIndex + 1, 0) - worksheet.getTimeValue(startIndex, 0);
        final double totalT = rng2 - rng1;

        
        for (int row = 0; row < _noOfPowerAnals; row++) {
            double[] anU = new double[CALC_NUM_HARM], anI = new double[CALC_NUM_HARM];
            double[] bnU = new double[CALC_NUM_HARM], bnI = new double[CALC_NUM_HARM];
            double[] cnU = new double[CALC_NUM_HARM], cnI = new double[CALC_NUM_HARM], dphiUI = new double[CALC_NUM_HARM];
            
            // Rechnen bis zum Endpunkt:
            for (int i1 = startIndex; (i1 < worksheet.getMaximumTimeIndex(0)) 
                    && (worksheet.getTimeValue(i1 + 1, 0) > worksheet.getTimeValue(i1, 0))
                    && (worksheet.getTimeValue(i1, 0) <= rng2); i1++) {
                deltaT = worksheet.getTimeValue(i1 + 1, 0) - worksheet.getTimeValue(i1, 0);
                final double time = worksheet.getTimeValue(i1, 0);
                final double voltage = worksheet.getValue(voltageIndices[row], i1);
                final double current = worksheet.getValue(currentIndices[row], i1);
                for (int n = 0; n < CALC_NUM_HARM; n++) {
                    final double arg = 2 * Math.PI / totalT * time * n;
                    final double cosDt = Math.cos(arg) * deltaT;
                    final double sinDt = Math.sin(arg) * deltaT;
                    anU[n] += voltage * cosDt;
                    bnU[n] += voltage * sinDt;
                    anI[n] += current * cosDt;
                    bnI[n] += current * sinDt;
                }
                
                _powerP[row] += (voltage * current * deltaT);
                _rms2U[row] += (voltage * voltage * deltaT);
                _rms2I[row] += (current * current * deltaT);
            }
            //--------
            for (int n = 0; n < CALC_NUM_HARM; n++) {
                anU[n] /= totalT/2;
                bnU[n] /= totalT/2;
                anI[n] /= totalT/2;
                bnI[n] /= totalT/2;
                cnU[n] = Math.sqrt(anU[n] * anU[n] + bnU[n] * bnU[n]);
                cnI[n] = Math.sqrt(anI[n] * anI[n] + bnI[n] * bnI[n]);
                dphiUI[n] = Math.atan2(bnU[n], anU[n]) - Math.atan2(bnI[n], anI[n]);
                _powerQ[row] += cnU[n] * cnI[n] * Math.sin(dphiUI[n]) / 2;
            }
            _powerP[row] /= totalT;
            _rms2U[row] /= totalT;
            _rms2I[row] /= totalT;
            _powerS[row] = Math.sqrt(_rms2U[row] * _rms2I[row]);
            _powerD[row] = Math.sqrt(_powerS[row] * _powerS[row] - (_powerP[row] * _powerP[row] + _powerQ[row] * _powerQ[row]));
            _lambda[row] = _powerP[row] / _powerS[row];
            _cosPhi[row] = Math.cos(dphiUI[1]);
        }
    }

    public static PowerCalculator calculatorFabric(final AbstractDataContainer worksheet, 
            final PowerAnalysisPanel.PowerCalculatorSelectionIndex selectedIndices) {
        return new PowerCalculator(worksheet, selectedIndices);        
    }
}
