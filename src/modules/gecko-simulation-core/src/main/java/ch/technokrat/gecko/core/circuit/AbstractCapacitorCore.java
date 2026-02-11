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
 * Core capacitor simulation logic - NO GUI dependencies.
 * Pure simulation contract extractable to gecko-simulation-core.
 * Handles capacitance value and initial voltage state.
 */
public abstract class AbstractCapacitorCore extends CircuitComponentCore {
    
    protected double _capacitance = 100e-9;  // Default 100 nF
    protected double _initialValue = 0.0;     // Initial voltage/temperature
    protected double _linearizedCapacitance;
    
    protected AbstractCapacitorCore(SolverType solverType) {
        super(solverType);
        _linearizedCapacitance = _capacitance;
    }
    
    /**
     * Get capacitance value in Farads
     */
    public double getCapacitance() {
        return _capacitance;
    }
    
    /**
     * Set capacitance value
     */
    public void setCapacitance(double capacitance) {
        if (capacitance < 1e-15) {
            this._capacitance = 1e-15;  // Prevent numerical issues
        } else {
            this._capacitance = capacitance;
        }
        this._linearizedCapacitance = this._capacitance;
    }
    
    /**
     * Get initial voltage value in Volts
     */
    public double getInitialValue() {
        return _initialValue;
    }
    
    /**
     * Set initial voltage condition
     */
    public void setInitialValue(double initialValue) {
        this._initialValue = initialValue;
    }
    
    /**
     * Get linearized capacitance (for nonlinear capacitors)
     */
    public double getLinearizedCapacitance() {
        return _linearizedCapacitance;
    }
    
    /**
     * Set linearized capacitance
     */
    public void setLinearizedCapacitance(double value) {
        this._linearizedCapacitance = value;
    }
}
