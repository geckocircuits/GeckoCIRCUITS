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
package ch.technokrat.gecko.geckocircuits.control.calculators;

public final class PT1Calculator extends AbstractPTCalculator {
        
        private double _yOld = 0;  // Speicherung des I-Anteils
        private double _xOld = 0;

        public PT1Calculator(final double timeConstant, final double gainFactor) {            
            super(timeConstant, gainFactor);            
        }        

        @Override
        public void berechneYOUT(final double deltaT) {
            //if (t==0) { xalt=yaltInit;  yalt=xaltInit; }  // re-init
            _outputSignal[0][0] = _yOld * (2 * _TVal - deltaT) / (2 * _TVal + deltaT) + _a1Val / (1 + 2 * _TVal / deltaT)
                    * (_inputSignal[0][0] + _xOld);
            _xOld = _inputSignal[0][0];
            _yOld = _outputSignal[0][0];
        }        
    }
