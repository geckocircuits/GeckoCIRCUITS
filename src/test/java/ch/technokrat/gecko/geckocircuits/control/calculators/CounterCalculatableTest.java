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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andy
 */
public class CounterCalculatableTest extends AbstractTwoInputsMathFunctionTest {

    @Override
    AbstractControlCalculatable calculatorFabricTwoInputs() {
        return new CounterCalculatable();
    }

    @Override
    @Test        
    public void testInputTrueTrue() {
        _controlCalculatableTwoInputs._inputSignal[1][0] = 1; // reset;        
        _controlCalculatableTwoInputs.berechneYOUT(TEST_DT);
        _controlCalculatableTwoInputs._inputSignal[1][0] = 0; 
        _controlCalculatableTwoInputs._inputSignal[0][0] = 0; 
        _controlCalculatableTwoInputs.berechneYOUT(TEST_DT);
        _controlCalculatableTwoInputs._inputSignal[0][0] = 1; 
        _controlCalculatableTwoInputs.berechneYOUT(TEST_DT);        
        _controlCalculatableTwoInputs._inputSignal[0][0] = 0; 
        _controlCalculatableTwoInputs.berechneYOUT(TEST_DT);
        _controlCalculatableTwoInputs._inputSignal[0][0] = 1; 
        _controlCalculatableTwoInputs.berechneYOUT(TEST_DT);
        assertEquals(2, _controlCalculatableTwoInputs._outputSignal[0][0], 1e-6);
    }

    @Override
    @Test
    public void testInputTrueFalse() {                
        _controlCalculatableTwoInputs._inputSignal[0][0] = 1; 
        _controlCalculatableTwoInputs.berechneYOUT(TEST_DT);
        _controlCalculatableTwoInputs._inputSignal[0][0] = 0; 
        _controlCalculatableTwoInputs.berechneYOUT(TEST_DT);
        _controlCalculatableTwoInputs._inputSignal[0][0] = 1; 
        _controlCalculatableTwoInputs.berechneYOUT(TEST_DT);
        _controlCalculatableTwoInputs._inputSignal[1][0] = 1; // reset;
        _controlCalculatableTwoInputs.berechneYOUT(TEST_DT);
        assertEquals(0, _controlCalculatableTwoInputs._outputSignal[0][0], 1e-6);
    }

    @Override
    @Test
    public void testInputFalseFalse() {        
        _controlCalculatableTwoInputs._inputSignal[0][0] = 1; 
        _controlCalculatableTwoInputs._inputSignal[1][0] = 1; // reset;
        _controlCalculatableTwoInputs.berechneYOUT(TEST_DT);
        _controlCalculatableTwoInputs._inputSignal[1][0] = 0; // reset;
        _controlCalculatableTwoInputs.berechneYOUT(TEST_DT);
        _controlCalculatableTwoInputs.berechneYOUT(TEST_DT); // counter should 
        // count only "flanks"
        assertEquals(0, _controlCalculatableTwoInputs._outputSignal[0][0], 1e-6);
    }        
}
