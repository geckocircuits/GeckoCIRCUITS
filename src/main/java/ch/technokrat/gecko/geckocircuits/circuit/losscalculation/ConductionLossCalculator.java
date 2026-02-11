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
 * Calculator for semiconductor conduction (on-state) losses.
 * 
 * <p>Conduction losses represent the power dissipated when current flows through
 * a semiconductor device in its conducting state. The loss calculation uses
 * the device's on-state voltage drop characteristic.
 * 
 * <p>Loss models supported:
 * <ul>
 *   <li><b>Resistive Model</b>: P = I² × R_on</li>
 *   <li><b>Threshold + Resistive Model</b>: P = V_th × I + R_on × I²</li>
 *   <li><b>Lookup Table Model</b>: P = V_on(I, T_j) × I</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>
 * ConductionLossCalculator calc = ConductionLossCalculator.resistiveModel(0.01);
 * double loss = calc.calculateLoss(10.0);  // 1W at 10A
 * </pre>
 * 
 * @see SwitchingLossCalculator
 * @see AbstractLossCalculatorSwitch
 */
public final class ConductionLossCalculator {
    
    /** On-state resistance in ohms */
    private final double onResistance;
    
    /** Threshold voltage (forward voltage drop at zero current) in volts */
    private final double thresholdVoltage;
    
    /** Temperature coefficient for resistance (per °C) */
    private final double temperatureCoefficient;
    
    /** Reference temperature for parameters (°C) */
    private static final double REFERENCE_TEMPERATURE = 25.0;
    
    /**
     * Creates a conduction loss calculator with threshold and resistive model.
     * 
     * @param thresholdVoltage threshold voltage V_th in volts (e.g., 0.7V for diode)
     * @param onResistance on-state resistance R_on in ohms
     * @param temperatureCoefficient temperature coefficient for R_on (per °C)
     */
    public ConductionLossCalculator(double thresholdVoltage, double onResistance, 
                                    double temperatureCoefficient) {
        if (onResistance < 0) {
            throw new IllegalArgumentException("On-resistance cannot be negative: " + onResistance);
        }
        if (thresholdVoltage < 0) {
            throw new IllegalArgumentException("Threshold voltage cannot be negative: " + thresholdVoltage);
        }
        this.thresholdVoltage = thresholdVoltage;
        this.onResistance = onResistance;
        this.temperatureCoefficient = temperatureCoefficient;
    }
    
    /**
     * Creates a purely resistive conduction loss calculator (P = I² × R).
     * 
     * @param onResistance on-state resistance R_on in ohms
     * @return calculator instance
     */
    public static ConductionLossCalculator resistiveModel(double onResistance) {
        return new ConductionLossCalculator(0.0, onResistance, 0.0);
    }
    
    /**
     * Creates a threshold + resistive conduction loss calculator.
     * 
     * @param thresholdVoltage threshold voltage V_th in volts
     * @param onResistance on-state resistance R_on in ohms
     * @return calculator instance
     */
    public static ConductionLossCalculator thresholdResistiveModel(double thresholdVoltage, 
                                                                    double onResistance) {
        return new ConductionLossCalculator(thresholdVoltage, onResistance, 0.0);
    }
    
    /**
     * Calculates conduction loss at reference temperature.
     * P = V_th × |I| + R_on × I²
     * 
     * @param current device current in amperes
     * @return power loss in watts
     */
    public double calculateLoss(double current) {
        return calculateLoss(current, REFERENCE_TEMPERATURE);
    }
    
    /**
     * Calculates conduction loss with temperature adjustment.
     * P = V_th × |I| + R_on(T) × I²
     * where R_on(T) = R_on × (1 + α × (T - T_ref))
     * 
     * @param current device current in amperes
     * @param temperature junction temperature in °C
     * @return power loss in watts
     */
    public double calculateLoss(double current, double temperature) {
        double absCurrent = Math.abs(current);
        double adjustedResistance = getTemperatureAdjustedResistance(temperature);
        
        // P = V_th × I + R_on × I²
        return thresholdVoltage * absCurrent + adjustedResistance * current * current;
    }
    
    /**
     * Calculates the on-state voltage drop.
     * V_on = V_th + R_on × |I|
     * 
     * @param current device current in amperes
     * @return on-state voltage in volts
     */
    public double calculateOnStateVoltage(double current) {
        return calculateOnStateVoltage(current, REFERENCE_TEMPERATURE);
    }
    
    /**
     * Calculates the on-state voltage drop with temperature adjustment.
     * V_on = V_th + R_on(T) × |I|
     * 
     * @param current device current in amperes
     * @param temperature junction temperature in °C
     * @return on-state voltage in volts
     */
    public double calculateOnStateVoltage(double current, double temperature) {
        double adjustedResistance = getTemperatureAdjustedResistance(temperature);
        return thresholdVoltage + adjustedResistance * Math.abs(current);
    }
    
    /**
     * Calculates resistance adjusted for temperature.
     * R_on(T) = R_on × (1 + α × (T - T_ref))
     * 
     * @param temperature junction temperature in °C
     * @return temperature-adjusted resistance in ohms
     */
    public double getTemperatureAdjustedResistance(double temperature) {
        double deltaT = temperature - REFERENCE_TEMPERATURE;
        return onResistance * (1.0 + temperatureCoefficient * deltaT);
    }
    
    /**
     * Calculates average conduction loss over a period.
     * P_avg = V_th × I_avg + R_on × I_rms²
     * 
     * @param avgCurrent average current in amperes
     * @param rmsCurrent RMS current in amperes
     * @return average power loss in watts
     */
    public double calculateAverageLoss(double avgCurrent, double rmsCurrent) {
        return thresholdVoltage * Math.abs(avgCurrent) + onResistance * rmsCurrent * rmsCurrent;
    }
    
    /**
     * Calculates energy dissipated over a time interval.
     * E = P × Δt
     * 
     * @param current device current in amperes
     * @param deltaT time interval in seconds
     * @return energy in joules
     */
    public double calculateEnergy(double current, double deltaT) {
        return calculateLoss(current) * deltaT;
    }
    
    /**
     * Gets the on-state resistance.
     * @return on-state resistance in ohms
     */
    public double getOnResistance() {
        return onResistance;
    }
    
    /**
     * Gets the threshold voltage.
     * @return threshold voltage in volts
     */
    public double getThresholdVoltage() {
        return thresholdVoltage;
    }
    
    /**
     * Gets the temperature coefficient.
     * @return temperature coefficient per °C
     */
    public double getTemperatureCoefficient() {
        return temperatureCoefficient;
    }
    
    /**
     * Checks if this is a purely resistive model (no threshold voltage).
     * @return true if threshold voltage is zero
     */
    public boolean isResistiveOnly() {
        return thresholdVoltage == 0.0;
    }
}
