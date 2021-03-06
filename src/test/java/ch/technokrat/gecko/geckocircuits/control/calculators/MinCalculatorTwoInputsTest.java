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

public class MinCalculatorTwoInputsTest extends AbstractTransitiveTwoInputs {

    @Override
    AbstractControlCalculatable calculatorFabricTwoInputs() {
        return new MinCalculatorTwoInputs();
    }

    @Override
    public void testInputTrueTrue() {
        double val = getValue(-2,-4);
        assertWithTol(-4, val);
    }

    @Override
    public void testInputTrueFalse() {
        double val = getValue(4,5);
        assertWithTol(4, val);
    }

    @Override
    public void testInputFalseFalse() {
        double val = getValue(-4,-3);
        assertWithTol(-4, val);
    }
}
