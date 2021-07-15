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
import org.junit.Test;
import static org.junit.Assert.*;

@Ignore
public abstract class AbstractSimpleMathFunctionTest {
    private static final double TEST_DT = 1e-9;
    private AbstractControlCalculatable _controlCalculatable;    

    @Before
    public final void setUp() {
            _controlCalculatable = calculatorFabric();            
            _controlCalculatable._inputSignal[0] = new double[1];
            assert _controlCalculatable._inputSignal.length == 1;
            assert _controlCalculatable._outputSignal.length == 1;
            assert _controlCalculatable._inputSignal[0] != null;
            assert _controlCalculatable._outputSignal[0] != null;            
    }
    
    @After
    public final void tearDown() {
        // nothing todo for a simple math function!
    }
    
    abstract AbstractControlCalculatable calculatorFabric();    
    
    /*
     * test the input with a zer-value
     */    
    @Test
    abstract public void testBerechneYOUTResult0();
    
    /*
     * use some other characteristic input value (e.g. Pi)
     */  
    @Test
    abstract public void testBerechneYOUTValue();
    
    /*
     * test things like a negative number in Ln-Functions...
     */  
    @Test
    abstract public void testErrorValue();
    
    public double getValue(final double testValue) {
        _controlCalculatable._inputSignal[0][0] = testValue;
        _controlCalculatable.berechneYOUT(TEST_DT);
        return _controlCalculatable._outputSignal[0][0];
    }
    
    public void assertWithTol(final double expected, final double value) {
        assertEquals(expected, value, 1e-9);
    }        
}
