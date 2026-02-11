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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import static org.junit.Assert.*;

/**
 * Abstract base test class for two-input math function calculators.
 *
 * IGNORED at class level: This is an abstract base class that provides common test
 * infrastructure for concrete two-input math function tests. The @Ignore prevents
 * JUnit from attempting to instantiate and run this abstract class directly.
 * Concrete subclasses (e.g., AddCalculatorTest, MultiplyCalculatorTest) extend this
 * class and provide actual test implementations that DO run.
 */
@Ignore("Abstract base class - concrete subclasses run the actual tests")
public abstract class AbstractTwoInputsMathFunctionTest {
    static final double TEST_DT = 1e-9;
    public AbstractControlCalculatable _controlCalculatableTwoInputs;
    
    @Before
    public final void setUp() {
            _controlCalculatableTwoInputs = calculatorFabricTwoInputs();
            _controlCalculatableTwoInputs._inputSignal[0] = new double[1];
            _controlCalculatableTwoInputs._inputSignal[1] = new double[1];
            assert _controlCalculatableTwoInputs._inputSignal.length == 2;
            assert _controlCalculatableTwoInputs._outputSignal.length == 1;
            assert _controlCalculatableTwoInputs._inputSignal[0] != null;
            assert _controlCalculatableTwoInputs._outputSignal[0] != null;                
    }
    
    @After
    public final void tearDown() {
        // nothing todo for a simple math function!
    }
    
    abstract AbstractControlCalculatable calculatorFabricTwoInputs();        

    public abstract void testInputTrueTrue();
    public abstract void testInputTrueFalse();
    public abstract void testInputFalseFalse();
    
    public double getValue(final double arg1, final double arg2) {
        _controlCalculatableTwoInputs._inputSignal[0][0] = arg1;
        _controlCalculatableTwoInputs._inputSignal[1 ][0] = arg2;
        _controlCalculatableTwoInputs.berechneYOUT(TEST_DT);
        return _controlCalculatableTwoInputs._outputSignal[0][0];
    }
    
    public void assertWithTol(final double expected, final double value) {
        assertEquals(expected, value, 1e-9);
    }
}
