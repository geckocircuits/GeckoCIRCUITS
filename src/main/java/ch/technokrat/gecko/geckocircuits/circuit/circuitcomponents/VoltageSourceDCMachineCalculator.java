/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.circuit.TimeFunctionConstant;

// TODO: Ath the moment, the machine equations are implemented somewhere else
// We have to merge the two approaches, soon!
      
public class VoltageSourceDCMachineCalculator extends VoltageSourceCalculator implements BStampable, PostProcessable {

    private double phi;
    private double emk;
    private double drehzahl;
    private double omegaALT;
    private double Fr;
    private double omega;
    private double momentElektr;
    private double _J;
    private double momentLast;
    private double _cM;
    private InductorCouplingCalculator _le;
    private InductorCouplingCalculator _la;
    private final TimeFunctionConstant _timeFunction;
    private double _Ne;

    public VoltageSourceDCMachineCalculator(TimeFunctionConstant timeFunction, InductorCouplingCalculator le, 
            InductorCouplingCalculator la, final AbstractVoltageSource parent) {        
        super(timeFunction, parent);
        _timeFunction = timeFunction;
        _la = la;
        _le = le;
    }

    public void setInertia(double value) {
        _J = value;
    }

    public void setFr(double value){
        Fr = value;
    }

    public void setNe(double value) {
        _Ne = value;
    }

    public void setCm(double value) {
        _cM = value;
    }

    public void setTorque(double value) {
        momentLast = value;
    }

    public void doPostProcess(double dt, double time) {
        // aus dem internen Subcircuit -->
        double ia = - _la._current;  // Ankerstrom
        double ie = _le._current;  // Erregerstrom

        // Motor-Gleichungen durchrechnen -->
        phi = _le.getInductance() / _Ne * ie;  // Erregerfluss
        momentElektr = _cM * phi * ia;  // elektrisches Moment
        omega = (_J / dt * omegaALT + momentElektr - momentLast) / (_J / dt + Fr);

        drehzahl = (60.0 / (2 * Math.PI)) * omega;
        
        emk = _cM * phi * omega;  // innere Spannung der Maschine
        _timeFunction.setValue(emk);  // DC-Wert der internen WSpg.Quelle
        //if (t==0) System.out.println(t+"   "+dt+"   "+ia+"   "+ie+"   "+momentLast+"   "+phi+"   "+momentElektr+"   "+omega+"   "+drehzahl+"   "+emk+"   omegaALT="+omegaALT+"   J="+J+"   Fr="+Fr);
        omegaALT = omega;
    }
}
