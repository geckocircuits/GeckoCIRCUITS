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

public final class PT2Calculator extends AbstractPTCalculator {

    private double _yalt = 0;
    private double _yaltalt = 0;
    private double _xalt = 0;  // Berechnung aus einem LC-ESB mit z als Strom i(t)
    // G(s)= a1/(1 +(sT)^2)    

    public PT2Calculator(final double timeConstant, final double a1Val) {
        super(timeConstant, a1Val);        
    }
    

    @Override
    public void berechneYOUT(final double deltaT) {
        //        if (t==0) { xalt=0;  yalt=0;  yaltalt=0;  zalt=0; }  // re-init
        //
        //double yout= yalt*((4*T/dt*T/dt-1)/(4*T/dt*T/dt+1)) +a1/(1+4*T/dt*T/dt)*(xIN[0]+xalt) +zalt;
        //double z= -zalt +2*T/dt*(yout-yalt);
        final double dt_dt_T_T = deltaT * deltaT / (_TVal * _TVal);
        final double yout = _yalt * (2 - dt_dt_T_T) - _yaltalt + _a1Val * dt_dt_T_T * _xalt;
        _xalt = _inputSignal[0][0];
        _yaltalt = _yalt;
        _yalt = yout;
        //zalt= z;
        _outputSignal[0][0] = yout;

    }
}
