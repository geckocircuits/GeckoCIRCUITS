/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  Foobar is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.IGBT;
import ch.technokrat.gecko.i18n.LangInit;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;

public final class VerlustBerechnungSimpleTest {
    private static double DEF_CURRENT = 2;// Amps
    private static final double DELTA_T = 1e-6;
    private static final double TOLERANCE = 1e-6;
    private static final double UF = 0.8;
    private static final double KON = 15e-6;
    private static final double KOFF = 20e-6;
    private static final double USWNORM = 200;
    private static final double R_ON = 0.04;
    private IGBT _igbt;
    private LossCalculationSimple _verlustBerechnung;
        
    
    @Before
    public void setUp() {
        LangInit.initEnglish();
        _igbt = new IGBT();
        _igbt._onResistance.setValueWithoutUndo(R_ON);
        _igbt.getForwardVoltageDropParameter().setValueWithoutUndo(UF);
        _verlustBerechnung = new LossCalculationSimple(_igbt);
        _verlustBerechnung._kON = KON;
        _verlustBerechnung._kOFF = KOFF;
        _verlustBerechnung._uSWnorm = USWNORM;        
        
    }

    @Test(expected=AssertionError.class)
    public void testLossCalculatorErrorFabric() {                
        _verlustBerechnung._uSWnorm = 0;
        final AbstractLossCalculator result = _verlustBerechnung.lossCalculatorFabric();        
    }
    
    @Test
    @Ignore
    public void testLossCalculatorSwitch() { 
        LossCalculationSimple.LossCalculatorSwitchSimple lossCalc = (LossCalculationSimple.LossCalculatorSwitchSimple) _verlustBerechnung.lossCalculatorFabric();                
        lossCalc.calcLosses(DEF_CURRENT, 0, DELTA_T);
        final double initLoss = lossCalc.getTotalLosses();        
        lossCalc.calcLosses(DEF_CURRENT, 0, DELTA_T);
        final double conductionLoss = lossCalc.getTotalLosses();
        assertEquals(UF * DEF_CURRENT + DEF_CURRENT * DEF_CURRENT * R_ON, conductionLoss, TOLERANCE);
        _igbt._voltage = 100;
        lossCalc.calcLosses(0, 0, DELTA_T);
        final double turnOffLoss = lossCalc.getTotalLosses();
        assertEquals(KOFF * _igbt._voltage * DEF_CURRENT / (USWNORM * DELTA_T), turnOffLoss, TOLERANCE);        
        _igbt._voltage = 0;
        lossCalc.calcLosses(DEF_CURRENT, 0, DELTA_T);
        final double turnOnLoss = lossCalc.getTotalLosses();
        assertEquals(turnOnLoss, 16.76, TOLERANCE);     
    }

    @Test
    public void testCopyPropertiesFrom() {
        
    }
}
