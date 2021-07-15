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

import java.util.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

@Ignore
public abstract class AbstractMultiInputFunctionTest {
    public static final double TEST_DT = 1e-9;
    private AbstractControlCalculatable _controlCalculatableTwoInputs;
    public AbstractControlCalculatable _controlCalculatableThreeInputs;
    private AbstractControlCalculatable _matchingTwoInputCalculator;                
    
    @Before
    public final void setUp() {
            _controlCalculatableTwoInputs = calculatorFabricTwoInputs();
            _controlCalculatableTwoInputs._inputSignal[0] = new double[1];
            _controlCalculatableTwoInputs._inputSignal[1] = new double[1];
            assert _controlCalculatableTwoInputs._inputSignal.length == 2;
            assert _controlCalculatableTwoInputs._outputSignal.length == 1;
            assert _controlCalculatableTwoInputs._inputSignal[0] != null;
            assert _controlCalculatableTwoInputs._outputSignal[0] != null;
            
            _controlCalculatableThreeInputs = calculatorFabricThreeInputs();
            _controlCalculatableThreeInputs._inputSignal[0] = new double[1];                        
            _controlCalculatableThreeInputs._inputSignal[1] = new double[1];
            _controlCalculatableThreeInputs._inputSignal[2] = new double[1];
            assert _controlCalculatableThreeInputs._inputSignal.length == 3;
            assert _controlCalculatableThreeInputs._outputSignal.length == 1;
            assert _controlCalculatableThreeInputs._inputSignal[0] != null;
            assert _controlCalculatableThreeInputs._outputSignal[0] != null;
            
            _matchingTwoInputCalculator = matchingTwoInputFabric();
            _matchingTwoInputCalculator._inputSignal[0] = new double[1];
            _matchingTwoInputCalculator._inputSignal[1] = new double[1];
            assert _matchingTwoInputCalculator._inputSignal.length == 2;
            assert _matchingTwoInputCalculator._outputSignal.length == 1;
            assert _matchingTwoInputCalculator._inputSignal[0] != null;
            assert _matchingTwoInputCalculator._outputSignal[0] != null;
            
            testCompareTwoInputsWithPlainOldObject();
    }
    
    @After
    public final void tearDown() {
        // nothing todo for a simple math function!
    }
    
    public abstract AbstractControlCalculatable calculatorFabricTwoInputs();        
    public abstract AbstractControlCalculatable calculatorFabricThreeInputs();    
    public abstract AbstractControlCalculatable matchingTwoInputFabric();        
    
    @Test
    public void testCompareTwoInputsWithPlainOldObject() {
        Random rand = new Random();
        double in1 = rand.nextDouble();
        double in2 = rand.nextDouble();
        _matchingTwoInputCalculator._inputSignal[0][0] = in1;
        _matchingTwoInputCalculator._inputSignal[1][0] = in2;
        
        _controlCalculatableTwoInputs._inputSignal[0][0] = in1;
        _controlCalculatableTwoInputs._inputSignal[1][0] = in2;
        
        _matchingTwoInputCalculator.berechneYOUT(TEST_DT);
        _controlCalculatableTwoInputs.berechneYOUT(TEST_DT);
        assertEquals(_matchingTwoInputCalculator._outputSignal[0][0], _controlCalculatableTwoInputs._outputSignal[0][0], 1e-6);
        
    }
    
    public double getValue(final double arg1, final double arg2, final double arg3) {
        _controlCalculatableThreeInputs._inputSignal[0][0] = arg1;
        _controlCalculatableThreeInputs._inputSignal[1][0] = arg2;
        _controlCalculatableThreeInputs._inputSignal[2][0] = arg3;
        _controlCalculatableThreeInputs.berechneYOUT(TEST_DT);
        return _controlCalculatableThreeInputs._outputSignal[0][0];
    }
    
    public abstract void testWithThreeInputs();
}
