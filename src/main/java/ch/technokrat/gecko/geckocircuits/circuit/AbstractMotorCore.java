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
 * Core motor simulation logic - NO GUI dependencies.
 * Pure simulation contract extractable to gecko-simulation-core.
 * Manages motor mechanical and electrical parameters.
 */
public abstract class AbstractMotorCore extends CircuitComponentCore {
    
    // Electrical parameters
    protected double _polePairs = 1.0;          // Number of pole pairs
    protected double _omegaElectric = 0.0;      // Electrical angular velocity (rad/s)
    protected double _thetaElectric = 0.0;      // Electrical angle (rad)
    
    // Mechanical parameters
    protected double _omegaMechanic = 0.0;      // Mechanical angular velocity (rad/s)
    protected double _omegaMechanicOld = 0.0;   // Previous mechanical angular velocity
    protected double _thetaMechanic = 0.0;      // Mechanical rotor position (rad)
    protected double _thetaMechanicOld = 0.0;   // Previous rotor position
    protected double _inertia = 1.0;            // Moment of inertia (kg*m^2)
    protected double _frictionCoefficient = 0.1; // Friction coefficient (N*m*s)
    protected double _initialRotationSpeed = 0.0; // Initial rotation speed (1/s)
    protected double _initialRotorPosition = 0.0; // Initial rotor position (rad)
    
    // Torque parameters
    protected double _torqueElectrical = 0.0;   // Electrical torque (N*m)
    protected double _torqueMechanical = 0.0;   // Mechanical load torque (N*m)
    
    protected AbstractMotorCore(SolverType solverType) {
        super(solverType);
    }
    
    /**
     * Get number of pole pairs
     */
    public double getPolePairs() {
        return _polePairs;
    }
    
    /**
     * Set number of pole pairs
     */
    public void setPolePairs(double value) {
        this._polePairs = value;
    }
    
    /**
     * Get inertia (moment of inertia)
     */
    public double getInertia() {
        return _inertia;
    }
    
    /**
     * Set inertia
     */
    public void setInertia(double value) {
        this._inertia = value;
    }
    
    /**
     * Get friction coefficient
     */
    public double getFrictionCoefficient() {
        return _frictionCoefficient;
    }
    
    /**
     * Set friction coefficient
     */
    public void setFrictionCoefficient(double value) {
        this._frictionCoefficient = value;
    }
    
    /**
     * Get mechanical angular velocity
     */
    public double getMechanicalAngularVelocity() {
        return _omegaMechanic;
    }
    
    /**
     * Set mechanical angular velocity
     */
    public void setMechanicalAngularVelocity(double value) {
        this._omegaMechanic = value;
    }
    
    /**
     * Get mechanical rotor position
     */
    public double getMechanicalRotorPosition() {
        return _thetaMechanic;
    }
    
    /**
     * Set mechanical rotor position
     */
    public void setMechanicalRotorPosition(double value) {
        this._thetaMechanic = value;
    }
    
    /**
     * Get electrical angular velocity
     */
    public double getElectricalAngularVelocity() {
        return _omegaElectric;
    }
    
    /**
     * Set electrical angular velocity
     */
    public void setElectricalAngularVelocity(double value) {
        this._omegaElectric = value;
    }
    
    /**
     * Get electrical torque
     */
    public double getElectricalTorque() {
        return _torqueElectrical;
    }
    
    /**
     * Set electrical torque
     */
    public void setElectricalTorque(double value) {
        this._torqueElectrical = value;
    }
    
    /**
     * Get mechanical load torque
     */
    public double getMechanicalTorque() {
        return _torqueMechanical;
    }
    
    /**
     * Set mechanical load torque
     */
    public void setMechanicalTorque(double value) {
        this._torqueMechanical = value;
    }
}
