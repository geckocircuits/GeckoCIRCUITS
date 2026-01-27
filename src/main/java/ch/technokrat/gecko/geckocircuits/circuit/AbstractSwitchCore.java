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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.allg.SolverType;

/**
 * Core switch simulation logic - NO GUI dependencies.
 * Pure simulation contract extractable to gecko-simulation-core.
 * Manages switch state (on/off) and resistance characteristics.
 */
public abstract class AbstractSwitchCore extends CircuitComponentCore {
    
    protected double _onResistance = 10e-3;    // On-state resistance in Ohms
    protected double _offResistance = 1e7;     // Off-state resistance in Ohms
    protected double _forwardVoltage = 0.60;   // Forward voltage drop
    protected double _kOn = 20e-6;             // Current-dependent on coefficient (Ws/A)
    protected double _kOff = 30e-6;            // Current-dependent off coefficient (Ws/A)
    protected int _numberParalleled = 1;       // Number of devices in parallel
    protected boolean _isOn = false;           // Current switch state
    
    protected AbstractSwitchCore(SolverType solverType) {
        super(solverType);
    }
    
    /**
     * Get on-state resistance
     */
    public double getOnResistance() {
        return _onResistance;
    }
    
    /**
     * Set on-state resistance
     */
    public void setOnResistance(double value) {
        this._onResistance = value;
    }
    
    /**
     * Get off-state resistance
     */
    public double getOffResistance() {
        return _offResistance;
    }
    
    /**
     * Set off-state resistance
     */
    public void setOffResistance(double value) {
        this._offResistance = value;
    }
    
    /**
     * Get forward voltage drop
     */
    public double getForwardVoltage() {
        return _forwardVoltage;
    }
    
    /**
     * Set forward voltage drop
     */
    public void setForwardVoltage(double value) {
        this._forwardVoltage = value;
    }
    
    /**
     * Get on-state energy loss coefficient
     */
    public double getKOn() {
        return _kOn;
    }
    
    /**
     * Set on-state energy loss coefficient
     */
    public void setKOn(double value) {
        this._kOn = value;
    }
    
    /**
     * Get off-state energy loss coefficient
     */
    public double getKOff() {
        return _kOff;
    }
    
    /**
     * Set off-state energy loss coefficient
     */
    public void setKOff(double value) {
        this._kOff = value;
    }
    
    /**
     * Get number of devices in parallel
     */
    public int getNumberParalleled() {
        return _numberParalleled;
    }
    
    /**
     * Set number of devices in parallel
     */
    public void setNumberParalleled(int value) {
        this._numberParalleled = value;
    }
    
    /**
     * Check if switch is on
     */
    public boolean isOn() {
        return _isOn;
    }
    
    /**
     * Set switch state
     */
    public void setOn(boolean value) {
        this._isOn = value;
    }
    
    /**
     * Get current resistance based on switch state
     */
    public double getResistance() {
        return _isOn ? _onResistance : _offResistance;
    }
}
