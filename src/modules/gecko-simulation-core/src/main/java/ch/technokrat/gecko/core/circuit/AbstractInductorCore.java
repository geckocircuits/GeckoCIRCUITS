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
package ch.technokrat.gecko.core.circuit;

import ch.technokrat.gecko.core.allg.SolverType;

/**
 * Core inductor simulation logic - NO GUI dependencies.
 * Pure simulation contract extractable to gecko-simulation-core.
 * Handles inductance value and initial current state.
 * Subclasses implement actual calculation methods.
 */
public abstract class AbstractInductorCore extends CircuitComponentCore {
    
    protected double _inductance = 3.0E-4;  // Default 0.3 mH
    protected double _initialCurrent = 0.0;
    protected double _linearizedInductance;
    
    protected AbstractInductorCore(SolverType solverType) {
        super(solverType);
        _linearizedInductance = _inductance;
    }
    
    /**
     * Get inductance value in Henries
     */
    public double getInductance() {
        return _inductance;
    }
    
    /**
     * Set inductance value
     */
    public void setInductance(double inductance) {
        if (inductance < 1e-12) {
            this._inductance = 1e-12;  // Prevent numerical issues
        } else {
            this._inductance = inductance;
        }
        this._linearizedInductance = this._inductance;
    }
    
    /**
     * Get initial current in Amperes
     */
    public double getInitialCurrent() {
        return _initialCurrent;
    }
    
    /**
     * Set initial current condition
     */
    public void setInitialCurrent(double initialCurrent) {
        this._initialCurrent = initialCurrent;
    }
    
    /**
     * Get linearized inductance value (for nonlinear inductors)
     */
    public double getLinearizedInductance() {
        return _linearizedInductance;
    }
    
    /**
     * Set linearized inductance
     */
    public void setLinearizedInductance(double value) {
        this._linearizedInductance = value;
    }
}
