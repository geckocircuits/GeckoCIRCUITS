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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ch.technokrat.gecko.geckocircuits.control.calculators;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andy
 */
public class NotEqualCalculatorTest extends AbstractTransitiveTwoInputs {

    @Override
    AbstractControlCalculatable calculatorFabricTwoInputs() {
        return new NotEqualCalculator();
    }

    @Override
    @Test
    public void testInputTrueTrue() {
        double val = getValue(1,1);
        assertWithTol(0, val);
    }

    @Override
    @Test
    public void testInputTrueFalse() {
        double val = getValue(1,0);
        assertWithTol(1, val);
    }

    @Override
    @Test
    public void testInputFalseFalse() {
        double val = getValue(-1,1);
        assertWithTol(1, val);
    }
    
}
