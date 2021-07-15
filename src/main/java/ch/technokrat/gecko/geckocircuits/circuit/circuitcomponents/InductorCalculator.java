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

public class InductorCalculator extends CircuitComponent implements BStampable,
        HistoryUpdatable {

    private static final double FAST_NULL_L = 1e-12;
    protected double _inductance;
    protected double _initialCurrent = 0;
    private double _maxAbsVal = 0; //for fast steady state solving

    public InductorCalculator(final AbstractInductor parent) {
        super(parent);
        _needsOldPotCurrent = true;
    }

    public void setInductance(double inductance) {
        if (inductance < FAST_NULL_L) {
            _inductance = FAST_NULL_L;
        } else {
            _inductance = inductance;
        }
    }

    public double getInductance() {
        return _inductance;
    }

    public void stampVectorB(double[] b, double t, double dt) {

        double bW = 0;
        if (_solverType == SolverType.SOLVER_BE) {
            bW = -_oldCurrent;
        } else if (_solverType == SolverType.SOLVER_TRZ) {
            bW = stampVectorBTRZ(dt);
        } else if (_solverType == SolverType.SOLVER_GS) {
            bW = (-4.0 / 3.0) * _oldCurrent + (1.0 / 3.0) * _oldOldCurrent;
        }

        //b[matrixIndices[0]] -= _oldCurrent;
        //b[matrixIndices[1]] += _oldCurrent;
        b[matrixIndices[0]] += bW;
        b[matrixIndices[1]] -= bW;
    }

    //because of coupled inductors, so that inductor coupling can override this method - also UGLY, just a temporary solution
    protected double stampVectorBTRZ(double dt) {
        return (-_oldCurrent - dt * (_potential1 - _potential2) / (2 * _inductance));
    }

    public void registerBVector(BVector bvector) {
    }

    public boolean isBasisStampable() {
        return false;
    }

    public void setInitialCurrent(Double value) {
        _initialCurrent = value;
        _current = _initialCurrent;
        _oldCurrent = _initialCurrent;
        _oldOldCurrent = _initialCurrent;
        _maxAbsVal = Math.abs(_initialCurrent);
    }

    public double getInitialValue() {
        return _initialCurrent;
    }

    @Override
    public final void updateHistory(double[] p) {
        _potOld1 = _potential1;
        _potOld2 = _potential2;
        _potential1 = p[matrixIndices[0]];
        _potential2 = p[matrixIndices[1]];
        _oldOldCurrent = _oldCurrent;
        _oldCurrent = _current;
        if (_maxAbsVal < Math.abs(_current)) {
            _maxAbsVal = Math.abs(_current);
        }
    }

    //for fast steady state solving - need max. abs. value of state variable during a simulation
    public double getMaxAbsVal() {
        return _maxAbsVal;
    }

    @Override
    public void init() {
        _current = _initialCurrent;
        _oldCurrent = _initialCurrent;
        _oldOldCurrent = _initialCurrent;

        _voltage = 0;
        _potential1 = 0;
        _potential2 = 0;
        _potOld1 = 0;
        _potOld2 = 0;
    }
}
