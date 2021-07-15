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

public class LnCalculatorTest extends AbstractSimpleMathFunctionTest {

    @Override
    AbstractControlCalculatable calculatorFabric() {
        return new LnCalculator();
    }

    @Test
    public void testBerechneYOUTResult0() {
                
        try {
            double val = getValue(0);
        } catch (AssertionError err) {
            return;
        }
        fail();
    }

    @Override
    @Test
    public void testBerechneYOUTValue() {
        double val = getValue(1);
        assertWithTol(0, val);        
    }

    @Override
    @Test
    public void testErrorValue() {
        try {
            double val = getValue(-1);            
        } catch (AssertionError err) {
            return;
        }
        fail();
    }
}
