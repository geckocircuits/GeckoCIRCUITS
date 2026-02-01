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
package ch.technokrat.gecko.geckocircuits.circuit.losscalculation;

/**
 * Calculator for semiconductor switching losses (turn-on and turn-off).
 * 
 * <p>Switching losses occur during the transition between on and off states.
 * Energy is dissipated due to the finite switching time during which both
 * voltage and current are non-zero simultaneously.
 * 
 * <p>Loss models supported:
 * <ul>
 *   <li><b>Energy Model</b>: P_sw = (E_on + E_off) × f_sw</li>
 *   <li><b>Scaled Energy Model</b>: E = E_ref × (I/I_ref) × (V/V_ref)</li>
 *   <li><b>Polynomial Model</b>: E(I) = a + b×I + c×I²</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>
 * SwitchingLossCalculator calc = SwitchingLossCalculator.fromEnergies(1e-3, 0.5e-3, 10, 600);
 * double energy = calc.calculateTurnOnEnergy(15, 400);  // scaled for different I and V
 * </pre>
 * 
 * @see ConductionLossCalculator
 * @see AbstractLossCalculatorSwitch
 */
public final class SwitchingLossCalculator {
    
    /** Turn-on energy at reference conditions in joules */
    private final double turnOnEnergyRef;
    
    /** Turn-off energy at reference conditions in joules */
    private final double turnOffEnergyRef;
    
    /** Reference current for energy measurements in amperes */
    private final double referenceCurrent;
    
    /** Reference voltage for energy measurements in volts */
    private final double referenceVoltage;
    
    /** Temperature coefficient for switching energy (per °C) */
    private final double temperatureCoefficient;
    
    /** Reference temperature for parameters (°C) */
    private static final double REFERENCE_TEMPERATURE = 25.0;
    
    /** Small current threshold for turn-on/off detection */
    public static final double CURRENT_THRESHOLD = 1e-2;
    
    /**
     * Creates a switching loss calculator with all parameters.
     * 
     * @param turnOnEnergy turn-on energy E_on at reference conditions in joules
     * @param turnOffEnergy turn-off energy E_off at reference conditions in joules
     * @param referenceCurrent reference current I_ref in amperes
     * @param referenceVoltage reference voltage V_ref in volts
     * @param temperatureCoefficient temperature coefficient (per °C)
     */
    public SwitchingLossCalculator(double turnOnEnergy, double turnOffEnergy,
                                   double referenceCurrent, double referenceVoltage,
                                   double temperatureCoefficient) {
        if (turnOnEnergy < 0) {
            throw new IllegalArgumentException("Turn-on energy cannot be negative: " + turnOnEnergy);
        }
        if (turnOffEnergy < 0) {
            throw new IllegalArgumentException("Turn-off energy cannot be negative: " + turnOffEnergy);
        }
        if (referenceCurrent <= 0) {
            throw new IllegalArgumentException("Reference current must be positive: " + referenceCurrent);
        }
        if (referenceVoltage <= 0) {
            throw new IllegalArgumentException("Reference voltage must be positive: " + referenceVoltage);
        }
        
        this.turnOnEnergyRef = turnOnEnergy;
        this.turnOffEnergyRef = turnOffEnergy;
        this.referenceCurrent = referenceCurrent;
        this.referenceVoltage = referenceVoltage;
        this.temperatureCoefficient = temperatureCoefficient;
    }
    
    /**
     * Creates a switching loss calculator from datasheet energy values.
     * 
     * @param turnOnEnergy turn-on energy E_on in joules
     * @param turnOffEnergy turn-off energy E_off in joules
     * @param referenceCurrent reference current I_ref in amperes
     * @param referenceVoltage reference voltage V_ref in volts
     * @return calculator instance
     */
    public static SwitchingLossCalculator fromEnergies(double turnOnEnergy, double turnOffEnergy,
                                                       double referenceCurrent, double referenceVoltage) {
        return new SwitchingLossCalculator(turnOnEnergy, turnOffEnergy, 
                                           referenceCurrent, referenceVoltage, 0.0);
    }
    
    /**
     * Creates a switching loss calculator from total energy and switching frequency.
     * Assumes equal turn-on and turn-off energies.
     * 
     * @param totalPower total switching power P_sw in watts
     * @param switchingFrequency switching frequency f_sw in Hz
     * @param referenceCurrent reference current in amperes
     * @param referenceVoltage reference voltage in volts
     * @return calculator instance
     */
    public static SwitchingLossCalculator fromPowerAndFrequency(double totalPower, 
                                                                 double switchingFrequency,
                                                                 double referenceCurrent,
                                                                 double referenceVoltage) {
        double totalEnergy = totalPower / switchingFrequency;
        double energyPerEvent = totalEnergy / 2.0;  // Assume equal E_on and E_off
        return fromEnergies(energyPerEvent, energyPerEvent, referenceCurrent, referenceVoltage);
    }
    
    /**
     * Calculates turn-on energy scaled for operating conditions.
     * E_on = E_on_ref × (I/I_ref) × (V/V_ref)
     * 
     * @param current switch current at turn-on in amperes
     * @param voltage blocking voltage at turn-on in volts
     * @return turn-on energy in joules
     */
    public double calculateTurnOnEnergy(double current, double voltage) {
        return calculateTurnOnEnergy(current, voltage, REFERENCE_TEMPERATURE);
    }
    
    /**
     * Calculates turn-on energy with temperature adjustment.
     * 
     * @param current switch current at turn-on in amperes
     * @param voltage blocking voltage at turn-on in volts
     * @param temperature junction temperature in °C
     * @return turn-on energy in joules
     */
    public double calculateTurnOnEnergy(double current, double voltage, double temperature) {
        double scaledEnergy = scaleEnergy(turnOnEnergyRef, current, voltage);
        return applyTemperatureCorrection(scaledEnergy, temperature);
    }
    
    /**
     * Calculates turn-off energy scaled for operating conditions.
     * E_off = E_off_ref × (I/I_ref) × (V/V_ref)
     * 
     * @param current switch current at turn-off in amperes
     * @param voltage blocking voltage after turn-off in volts
     * @return turn-off energy in joules
     */
    public double calculateTurnOffEnergy(double current, double voltage) {
        return calculateTurnOffEnergy(current, voltage, REFERENCE_TEMPERATURE);
    }
    
    /**
     * Calculates turn-off energy with temperature adjustment.
     * 
     * @param current switch current at turn-off in amperes
     * @param voltage blocking voltage after turn-off in volts
     * @param temperature junction temperature in °C
     * @return turn-off energy in joules
     */
    public double calculateTurnOffEnergy(double current, double voltage, double temperature) {
        double scaledEnergy = scaleEnergy(turnOffEnergyRef, current, voltage);
        return applyTemperatureCorrection(scaledEnergy, temperature);
    }
    
    /**
     * Calculates total switching energy per cycle (turn-on + turn-off).
     * 
     * @param current operating current in amperes
     * @param voltage blocking voltage in volts
     * @return total switching energy per cycle in joules
     */
    public double calculateTotalEnergy(double current, double voltage) {
        return calculateTurnOnEnergy(current, voltage) + calculateTurnOffEnergy(current, voltage);
    }
    
    /**
     * Calculates average switching power at given frequency.
     * P_sw = (E_on + E_off) × f_sw
     * 
     * @param current operating current in amperes
     * @param voltage blocking voltage in volts
     * @param switchingFrequency switching frequency in Hz
     * @return average switching power in watts
     */
    public double calculateSwitchingPower(double current, double voltage, double switchingFrequency) {
        return calculateTotalEnergy(current, voltage) * switchingFrequency;
    }
    
    /**
     * Calculates instantaneous switching power for a discrete event.
     * P_inst = E / Δt
     * 
     * @param energy switching energy in joules
     * @param deltaT simulation time step in seconds
     * @return instantaneous power in watts
     */
    public double calculateInstantaneousPower(double energy, double deltaT) {
        if (deltaT <= 0) {
            throw new IllegalArgumentException("Time step must be positive: " + deltaT);
        }
        return energy / deltaT;
    }
    
    /**
     * Detects a turn-on event (current rises from near-zero).
     * 
     * @param oldCurrent previous current value in amperes
     * @param newCurrent current value in amperes
     * @return true if turn-on detected
     */
    public boolean detectTurnOn(double oldCurrent, double newCurrent) {
        return Math.abs(oldCurrent) < CURRENT_THRESHOLD && Math.abs(newCurrent) >= CURRENT_THRESHOLD;
    }
    
    /**
     * Detects a turn-off event (current falls to near-zero).
     * 
     * @param oldCurrent previous current value in amperes
     * @param newCurrent current value in amperes
     * @return true if turn-off detected
     */
    public boolean detectTurnOff(double oldCurrent, double newCurrent) {
        return Math.abs(oldCurrent) >= CURRENT_THRESHOLD && Math.abs(newCurrent) < CURRENT_THRESHOLD;
    }
    
    /**
     * Scales reference energy to operating conditions.
     * E = E_ref × (I/I_ref) × (V/V_ref)
     */
    private double scaleEnergy(double referenceEnergy, double current, double voltage) {
        double currentRatio = Math.abs(current) / referenceCurrent;
        double voltageRatio = Math.abs(voltage) / referenceVoltage;
        return referenceEnergy * currentRatio * voltageRatio;
    }
    
    /**
     * Applies temperature correction to energy.
     * E(T) = E × (1 + α × (T - T_ref))
     */
    private double applyTemperatureCorrection(double energy, double temperature) {
        double deltaT = temperature - REFERENCE_TEMPERATURE;
        return energy * (1.0 + temperatureCoefficient * deltaT);
    }
    
    // Getters
    
    /**
     * Gets the reference turn-on energy.
     * @return turn-on energy in joules
     */
    public double getTurnOnEnergyRef() {
        return turnOnEnergyRef;
    }
    
    /**
     * Gets the reference turn-off energy.
     * @return turn-off energy in joules
     */
    public double getTurnOffEnergyRef() {
        return turnOffEnergyRef;
    }
    
    /**
     * Gets the reference current.
     * @return reference current in amperes
     */
    public double getReferenceCurrent() {
        return referenceCurrent;
    }
    
    /**
     * Gets the reference voltage.
     * @return reference voltage in volts
     */
    public double getReferenceVoltage() {
        return referenceVoltage;
    }
    
    /**
     * Gets the temperature coefficient.
     * @return temperature coefficient per °C
     */
    public double getTemperatureCoefficient() {
        return temperatureCoefficient;
    }
    
    /**
     * Calculates the ratio of turn-on to turn-off energy.
     * @return E_on / E_off ratio
     */
    public double getTurnOnToTurnOffRatio() {
        if (turnOffEnergyRef == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return turnOnEnergyRef / turnOffEnergyRef;
    }
}
