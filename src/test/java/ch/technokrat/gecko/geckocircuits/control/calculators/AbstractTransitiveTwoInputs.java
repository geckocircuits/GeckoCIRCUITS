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

/**
 * any function, where the input arguments can be exchanged and the result should
 * not change is here called "Transitive". Addition is transitive. Subtraction not!
 * @author andy
 */
public abstract class AbstractTransitiveTwoInputs extends AbstractTwoInputsMathFunctionTest {
    @Test
    public void testTransitivity() {        
        testInputTrueFalse();
        double oldResult = _controlCalculatableTwoInputs._outputSignal[0][0];
        double swapTmp = _controlCalculatableTwoInputs._inputSignal[1][0];
        _controlCalculatableTwoInputs._inputSignal[1][0] = _controlCalculatableTwoInputs._inputSignal[0][0];
        _controlCalculatableTwoInputs._inputSignal[0][0] = swapTmp;
        _controlCalculatableTwoInputs.berechneYOUT(TEST_DT);
        assertTrue(oldResult == _controlCalculatableTwoInputs._outputSignal[0][0]);
    }
}
