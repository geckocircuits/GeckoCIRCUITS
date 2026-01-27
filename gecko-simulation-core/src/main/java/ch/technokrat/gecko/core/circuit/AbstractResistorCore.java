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
 * Core resistor simulation logic - NO GUI dependencies.
 * Pure simulation contract extractable to gecko-simulation-core.
 * Subclasses like ResistorCalculator implement actual calculation.
 */
public abstract class AbstractResistorCore extends CircuitComponentCore {
    
    protected double _resistance = 1.0;
    protected double _conductance = 1.0;
    
    protected AbstractResistorCore(SolverType solverType) {
        super(solverType);
    }
    
    /**
     * Get current resistance value in ohms
     */
    public double getResistance() {
        return _resistance;
    }
    
    /**
     * Set resistance value and update conductance
     */
    public void setResistance(double resistance) {
        if (resistance < 1e-9) {
            this._resistance = 1e-9;  // Prevent singular matrix
        } else {
            this._resistance = resistance;
        }
        this._conductance = 1.0 / this._resistance;
    }
    
    /**
     * Get conductance (1/R)
     */
    public double getConductance() {
        return _conductance;
    }
}
