/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
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
package gecko.geckocircuits.control.calculators;

/**
 *
 * @author andreas
 */
public abstract class AbstractPTCalculator extends AbstractSingleInputSingleOutputCalculator {

    protected double _TVal;
    protected double _a1Val;

    public AbstractPTCalculator(final double timeConstant, final double gainFactor) {
        super();
        _TVal = timeConstant;
        _a1Val = gainFactor;
    }

    public void setTimeConstant(final double value) {
        _TVal = value;
    }

    public void setGain(final double value) {
        _a1Val = value;
    }
}
