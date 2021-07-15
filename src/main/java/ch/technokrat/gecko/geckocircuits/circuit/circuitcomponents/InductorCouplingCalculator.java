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

import ch.technokrat.gecko.geckocircuits.allg.SolverType;
import java.util.ArrayList;

public class InductorCouplingCalculator extends InductorCalculator {

    private ArrayList<InductorCouplingCalculator> allInductorsTmp = new ArrayList<InductorCouplingCalculator>();
    private CoupledInductorsGroup couplingGroup; //this to make the trapezoidal solver work, perhaps ugly -> maybe need better way to do it

    public InductorCouplingCalculator(final AbstractInductor parent) {
        super(parent);
    }        

    public void addNewCurrent(double addCurrent) {
        if (_solverType == SolverType.SOLVER_BE)
            _current = _oldCurrent + addCurrent;
        else if (_solverType == SolverType.SOLVER_TRZ)
            _current = _oldCurrent + 0.5*addCurrent; 
        else if (_solverType == SolverType.SOLVER_GS)
            _current = (2.0 / 3.0) * addCurrent + (4.0 / 3.0) * _oldCurrent - (1.0 / 3.0) * _oldOldCurrent; 
    }
    
    //for making TRZ working with coupled inductors - this is ugly, should be implemented in a better way later
    @Override
    protected double stampVectorBTRZ(double dt) {
        double LPproduct = couplingGroup.getLPproductForTRZ(this); //really ugly - that's why it should be implemented in a better way!
        
        return (-_oldCurrent - 0.5*dt*LPproduct);
    }
    
    public void setGroup(CoupledInductorsGroup group) {
        couplingGroup = group;
    }


}
