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


import java.util.List;

public class MutualCouplingCalculator {
    private double _M;
    private double _k;
    private InductorCouplingCalculator _l1;
    private InductorCouplingCalculator _l2;

    
    public MutualCouplingCalculator(InductorCouplingCalculator l1, InductorCouplingCalculator l2, double k) {
        _k = k;

        _l1 = l1;
        _l2 = l2;

    }

    public MutualCouplingCalculator() {
    }

    public InductorCouplingCalculator getL1() {
        return _l1;
    }

    public InductorCouplingCalculator getL2() {
        return _l2;
    }

    private double getMutualInductance() {
        return _k * Math.sqrt(_l1.getInductance() * _l2.getInductance() );
    }


    void stampInductanceMatrix(double[][] inductanceMatrix, final List<InductorCouplingCalculator> allInductors) {
        int index1 = allInductors.indexOf(_l1);
        int index2 = allInductors.indexOf(_l2);
        inductanceMatrix[index1][index2] = getMutualInductance();
        inductanceMatrix[index2][index1] = getMutualInductance();
    }


}
