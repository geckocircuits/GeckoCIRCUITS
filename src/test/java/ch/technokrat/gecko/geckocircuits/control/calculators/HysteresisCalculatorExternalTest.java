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

public class HysteresisCalculatorExternalTest extends AbstractTwoInputsMathFunctionTest {

    @Override
    AbstractControlCalculatable calculatorFabricTwoInputs() {
        return new HysteresisCalculatorExternal();
    }

    @Override
    @Test
    public void testInputTrueTrue() {        
        // here, we test with a negative input for hValue
        double hVal = -1.5;
        double inputVal = -1;
        
        double val = getValue(inputVal, hVal);
        assertWithTol(1, val);
        
        // transition from -1 to 1
        inputVal = -2;
        val = getValue(inputVal, hVal);        
        assertWithTol(-1, val);
        
        // no transition
        inputVal = -1.6;
        val = getValue(inputVal, hVal);        
        assertWithTol(-1, val);
        
        // no transition (idential vale == hVal ->Signum function is used internally
        inputVal = hVal;
        val = getValue(inputVal, hVal);        
        assertWithTol(-1, val);                                
        
        // from -1 to 1
        inputVal = 2;
        val = getValue(inputVal, hVal);        
        assertWithTol(1, val);
    }

    @Override
    @Test
    public void testInputTrueFalse() {
        // here, we test with a positive input for hValue
        double hVal = 1.5;
        double inputVal = 2;
        
        double val = getValue(inputVal, hVal);
        assertWithTol(1, val);
        
        // no transition!
        inputVal = 1.4;
        val = getValue(inputVal, hVal);        
        assertWithTol(1, val);
        
        // transition from +1 to -1
        inputVal = -1.6;
        val = getValue(inputVal, hVal);        
        assertWithTol(-1, val);                                
        
        // no transition (idential vale == hVal ->Signum function is used internally
        inputVal = -hVal;
        val = getValue(inputVal, hVal);        
        assertWithTol(-1, val);                                
        
    }

    @Override
    @Test
    public void testInputFalseFalse() {
        // what happens if hVal is set to zero?
        double hVal = 0;
        double inputVal = -1;
        double val = getValue(inputVal, hVal);        
        assertWithTol(-1, val);
        
        inputVal = 1;
        val = getValue(inputVal, hVal);        
        assertWithTol(1, val);
    }
    
}
