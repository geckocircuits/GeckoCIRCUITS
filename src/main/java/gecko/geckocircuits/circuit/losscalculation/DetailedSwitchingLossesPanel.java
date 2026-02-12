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

import gecko.geckocircuits.allg.FormatJTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class DetailedSwitchingLossesPanel extends DetailledLossPanel<SwitchingLossCurve> {

    private final FormatJTextField _jtfUb = new FormatJTextField();;   
        
            

    @Override
    String[] getTableCaptions() {
        return new String[]{"I [A]", "Eon [J]", "Eoff [J]"};
    }

    @Override
    void loadSelectedCurveIntoTable() {
        super.loadSelectedCurveIntoTable();
        _jtfUb.setNumberToField(((SwitchingLossCurve) _selectedCurve)._uBlock.getValue());        
    }

    @Override
    void addBlockingVoltageButton(final JPanel parent) {
        final JLabel jLabBlockingVoltage = new JLabel("U_b [V] =");        
        _jtfUb.setText("600");

        _jtfUb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                for (SwitchingLossCurve curve : _lossCurves) {
                    curve._uBlock.setUserValue(_jtfUb.getNumberFromField());
                }
            }
        });

        parent.add(jLabBlockingVoltage);
        parent.add(_jtfUb);
    }
    
    
    @Override
    LossCurve createNewCurve(final double curveTemperatureParameter) {        
        final double blockingVoltage = _jtfUb.getNumberFromField();
        final SwitchingLossCurve messkurve = new SwitchingLossCurve(curveTemperatureParameter, blockingVoltage);
        return messkurve;        
    }

    @Override
    LossCurve calculateNewTestCurve(final double temperature, final double blockingVoltage) {
        final LossCurve returnValue = new SwitchingLossCurve(temperature, blockingVoltage);
        DetailedLossLookupTable onTable = DetailedLossLookupTable.fabric(_lossCurves, 1);
        DetailedLossLookupTable offTable = DetailedLossLookupTable.fabric(_lossCurves, 2);
        double i = 0, di = calculateMaximumCurrentInAllCurves() / DIVISIONS_TEST_CURVE;
        double[][] data = new double[3][DIVISIONS_TEST_CURVE];
        for (int i1 = 0; i1 < DIVISIONS_TEST_CURVE; i1++) {
            data[0][i1] = i;  // i
            data[1][i1] = onTable.getInterpolatedYValue(temperature, i) * blockingVoltage;
            data[2][i1] = offTable.getInterpolatedYValue(temperature, i) * blockingVoltage;
            i += di;
        }
        returnValue.setCurveData(data);
        
        return returnValue;
    }
    
    public final double calculateMaximumCurrentInAllCurves() {
        double returnValue = -1;
        for (LossCurve curve : _lossCurves) {
            final int indexLast = curve.getCurveData()[0].length - 1;            
            double iLast = curve.getCurveData()[0][indexLast];
            returnValue = Math.max(returnValue, iLast);
        }
        return returnValue;
    }
}
