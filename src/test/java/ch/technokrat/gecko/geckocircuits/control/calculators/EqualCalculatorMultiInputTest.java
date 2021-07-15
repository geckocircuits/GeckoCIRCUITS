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
package ch.technokrat.gecko.geckocircuits.control.calculators;

import static org.junit.Assert.*;
import org.junit.Test;

public class EqualCalculatorMultiInputTest extends AbstractMultiInputFunctionTest {

    @Override
    public AbstractControlCalculatable calculatorFabricTwoInputs() {
        return new EqualCalculatorMultiInput(2);
    }

    @Override
    public AbstractControlCalculatable calculatorFabricThreeInputs() {
        return new EqualCalculatorMultiInput(3);
    }

    @Override
    public AbstractControlCalculatable matchingTwoInputFabric() {
        return new EqualCalculatorTwoInputs();
    }

    @Override
    @Test
    public void testWithThreeInputs() {
        _controlCalculatableThreeInputs._inputSignal[0][0] = 2; 
        _controlCalculatableThreeInputs._inputSignal[1][0] = 2; 
        _controlCalculatableThreeInputs._inputSignal[2][0] = 2; 
        _controlCalculatableThreeInputs.berechneYOUT(TEST_DT);
        assertEquals(1.0, _controlCalculatableThreeInputs._outputSignal[0][0], 1e-8);
        
        _controlCalculatableThreeInputs._inputSignal[0][0] = 2; 
        _controlCalculatableThreeInputs._inputSignal[1][0] = 1; 
        _controlCalculatableThreeInputs._inputSignal[2][0] = 2; 
        _controlCalculatableThreeInputs.berechneYOUT(TEST_DT);
        assertEquals(0, _controlCalculatableThreeInputs._outputSignal[0][0], 1e-8);
    }
    
    
}
