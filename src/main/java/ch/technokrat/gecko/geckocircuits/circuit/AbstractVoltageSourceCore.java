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
 * Core voltage source simulation logic - NO GUI dependencies.
 * Pure simulation contract extractable to gecko-simulation-core.
 * Manages voltage source parameters and waveform generation.
 */
public abstract class AbstractVoltageSourceCore extends CircuitComponentCore {
    
    protected double _dcValue = 10.0;          // DC component in Volts
    protected double _amplitude = 1.0;         // Peak amplitude
    protected double _offset = 0.0;            // Offset from zero
    protected double _frequency = 50.0;        // Frequency in Hz
    protected double _phase = 0.0;             // Phase in radians
    protected double _directPotentialGain = 1.0;  // Gain for controlled source
    
    protected AbstractVoltageSourceCore(SolverType solverType) {
        super(solverType);
    }
    
    /**
     * Get DC value
     */
    public double getDCValue() {
        return _dcValue;
    }
    
    /**
     * Set DC value
     */
    public void setDCValue(double value) {
        this._dcValue = value;
    }
    
    /**
     * Get amplitude for AC/sinusoidal sources
     */
    public double getAmplitude() {
        return _amplitude;
    }
    
    /**
     * Set amplitude
     */
    public void setAmplitude(double value) {
        this._amplitude = value;
    }
    
    /**
     * Get offset
     */
    public double getOffset() {
        return _offset;
    }
    
    /**
     * Set offset
     */
    public void setOffset(double value) {
        this._offset = value;
    }
    
    /**
     * Get frequency in Hz
     */
    public double getFrequency() {
        return _frequency;
    }
    
    /**
     * Set frequency in Hz
     */
    public void setFrequency(double value) {
        this._frequency = value;
    }
    
    /**
     * Get phase in radians
     */
    public double getPhase() {
        return _phase;
    }
    
    /**
     * Set phase in radians
     */
    public void setPhase(double value) {
        this._phase = value;
    }
    
    /**
     * Get gain for voltage-controlled sources
     */
    public double getDirectPotentialGain() {
        return _directPotentialGain;
    }
    
    /**
     * Set gain for voltage-controlled sources
     */
    public void setDirectPotentialGain(double value) {
        this._directPotentialGain = value;
    }
}
