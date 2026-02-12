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
package gecko.geckocircuits.control;

import gecko.geckocircuits.math.NComplex;

/**
     * This helper class is only used for making a nicer "toString" Method for complex numbers
     */
    class ComplexPrinter {
        public final NComplex _value; // NOPMD
        
        public ComplexPrinter(final NComplex value) {
            _value = value;
        }
        
        @Override
        public String toString() {
            if (_value.getIm() == 0) {
                return "" + _value.getRe();
            } else {
                return "<html>" + _value.getRe() + "&plusmn;" + _value.getIm() + "i";
            }

        }
    }