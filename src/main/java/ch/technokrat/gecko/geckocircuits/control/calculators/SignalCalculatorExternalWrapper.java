/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.control.calculators;

public final class SignalCalculatorExternalWrapper extends AbstractControlCalculatable
        implements InitializableAtSimulationStart {

    private static final int NO_INPUTS = 5;
    private final AbstractSignalCalculatorPeriodic _wrapped;

    private static final int AMPLITUDE_INDEX = 0;    
    private static final int FREQUENCY_INDEX = 1;
    private static final int OFFSET_INDEX = 2;
    private static final int PHASE_INDEX = 3;
    private static final int DUTY_INDEX = 4;               
    private static final double MAX_DUTY = 0.99999999;
    private static final double MIN_DUTY = 1e-7;
    
    public SignalCalculatorExternalWrapper(final AbstractSignalCalculatorPeriodic toWrap) {
        super(NO_INPUTS, 1);
        _wrapped = toWrap;
        // connect the output!
        _outputSignal[0] = _wrapped._outputSignal[0];
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        _wrapped.setAmplitudeAC(_inputSignal[AMPLITUDE_INDEX][0]);
        _wrapped.setAnteilDC(_inputSignal[OFFSET_INDEX][0]);
        _wrapped.setPhase(Math.toRadians(_inputSignal[PHASE_INDEX][0]));
        _wrapped.setDuty(Math.min(MAX_DUTY, Math.max(MIN_DUTY, _inputSignal[DUTY_INDEX][0])));
        if (_inputSignal[FREQUENCY_INDEX][0] < 0) {
            throw new RuntimeException("Frequency value: " + _inputSignal[FREQUENCY_INDEX][0] + " is not valid!");
        }
        _wrapped.setFrequency(_inputSignal[1][0]);

        if (_time == deltaT) { // when external parameters are used, the initialization is done 1 timestep later.
            // the reason is that, for e.g. for setting a proper phaseshift value, we first have to calculate a 
            // proper input signal.
            initializeAtSimulationStart(deltaT);
        }
        _wrapped.berechneYOUT(deltaT);
    }

    @Override
    public void initializeAtSimulationStart(final double deltaT) {
        if (_wrapped instanceof InitializableAtSimulationStart) {
            ((InitializableAtSimulationStart) _wrapped).initializeAtSimulationStart(deltaT);
        }
    }

    // the following getters and setters are only used in the testing
    // routines. They make the code more readable. In the simulation
    // we use direct access of _inputSignal so that the function
    // call overhead is reduced.
    void setAmplitudeAC(final double amp) {
        _inputSignal[AMPLITUDE_INDEX][0] = amp;
    }

    void setFrequency(final double freq) {
        _inputSignal[FREQUENCY_INDEX][0] = freq;
    }

    void setAnteilDC(final double offset) {
        _inputSignal[OFFSET_INDEX][0] = offset;
    }

    void setPhase(final double phase) {
        _inputSignal[PHASE_INDEX][0] = phase;
    }
    
    void setDuty(final double duty) {
        _inputSignal[DUTY_INDEX][0] = duty;
    }
}
