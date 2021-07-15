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

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.Diode;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

class DetailedConductionLossPanel extends DetailledLossPanel<LeitverlusteMesskurve> {

    public double uMaxCOND, iMaxCOND;  // maximale Bereichsgrenze aller 'messkurvePvCOND[]' bzw 'messkurvePvSWITCH[]' --> korekte Darstellung im Grafer
    public double[] tjGrenzenCOND, b0COND, b1COND, c0COND, c1COND, d0COND, d1COND;  // Koeffizienten der Naeherungspoloynome (siehe Publikation IPEC'05) --> Econd(i,tj)
    final JCheckBox useInSolver = new JCheckBox("<html>Use curve in electric<br>model characteristic</html>");
    Diode nonlinearDiode = null;
        
    
    public void useNonlinearInElectric(final Diode diode) {        
        this.nonlinearDiode = diode;
        _leftPanelTempAndBlocking.add(useInSolver);
        useInSolver.setSelected(diode.useNonlinearChar.getValue());
        useInSolver.setToolTipText("Option is only available if curve for only 1 temperature is given.");
        useInSolver.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                diode.useNonlinearChar.setUserValue(useInSolver.isSelected());                
            }
        });
    }
    
    
    
    @Override
    LossCurve createNewCurve(final double curveTemperatureParameter) {        
        return new LeitverlusteMesskurve(curveTemperatureParameter);                
    }

    @Override
    void addBlockingVoltageButton(final JPanel parent) {
        // no blocking voltage button!
    }

    @Override
    String[] getTableCaptions() {
        return new String[]{"U [V]", "I [A]"};
    }


    @Override
    LossCurve calculateNewTestCurve(double temperature, double measuredVoltage) {
        final LossCurve returnValue = new LeitverlusteMesskurve(temperature);
        double currentStepper = 0;
        final double currentStepwidth = calculateMaximumCurrentInAllCurves() / DIVISIONS_TEST_CURVE;
        
        DetailedLossLookupTable lookupTable = DetailedLossLookupTable.fabric(_lossCurves, 1);
        double[][] data = new double[2][DIVISIONS_TEST_CURVE];
        for (int i1 = 0; i1 < DIVISIONS_TEST_CURVE; i1++) {
            data[1][i1] = currentStepper;  // i
            data[0][i1] = lookupTable.getInterpolatedXValue(temperature, currentStepper);
            currentStepper += currentStepwidth;
        }
        returnValue.setCurveData(data);        
        return returnValue;
    }      

    @Override 
    public final double calculateMaximumCurrentInAllCurves() {
        double returnValue = -1;
        for (LossCurve curve : _lossCurves) {
            final int indexLast = curve.getCurveData()[0].length - 1;            
            double iLast = curve.getCurveData()[1][indexLast];
            returnValue = Math.max(returnValue, iLast);
        }
        return returnValue;
    }

    @Override
    void updateGuiAndGrafer() {
        super.updateGuiAndGrafer();
        useInSolver.setEnabled(_lossCurves.size() == 1);        
    }
    
    
    
}
