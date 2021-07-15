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

public abstract class AbstractSignalCalculatorPeriodic extends AbstractSignalCalculator
        implements InitializableAtSimulationStart {

    private static final double THOUSAND = 1000;
    protected double _amplitudeAC;
    protected double _frequency;
    protected double _dutyRatio;
    protected double _anteilDC;
    protected double _phase;
    protected double _triangle = 0;
    protected boolean _aufsteigend;
    protected double _dyUP = 0, _dyDOWN = 0;

    public AbstractSignalCalculatorPeriodic(final int noInputs, final double amplitudeAC,
            final double frequency, final double phase, final double anteilDC, final double duty) {
        super(noInputs);
        _frequency = frequency;
        _amplitudeAC = amplitudeAC;
        if (amplitudeAC < 0) {
            throw new IllegalArgumentException("Amplitude value of signal source has to be positive!");
        }
        _anteilDC = anteilDC;
        _dutyRatio = duty;
        _phase = phase;

    }

    @Override
    public final void initializeAtSimulationStart(final double deltaT) {
        final double txEnd = 1.0 / _frequency;
        final double dtx = txEnd / THOUSAND;
        double phaseX = calculatePhaseX();
        _triangle = 0;
        _aufsteigend = true;
        calculateStartSignal(dtx, txEnd, phaseX);
    }

    protected abstract void calculateStartSignal(final double dtx,
            final double txEnd, final double phaseX);

    protected final double calculatePhaseX() {
        double phaseX = _phase;
        while (phaseX > 2 * Math.PI) {
            phaseX -= 2 * Math.PI;
        }
        while (phaseX < 0) {
            phaseX += 2 * Math.PI;
        }
        return phaseX;
    }

    final void setDuty(final double value) {
        // limit between 0 and 1!
        _dutyRatio = Math.min(1, Math.max(0, value));
    }

    final void setAmplitudeAC(final double amplitudeAC) {
        this._amplitudeAC = amplitudeAC;
    }

    final void setFrequency(final double frequency) {
        this._frequency = frequency;
    }

    final void setAnteilDC(final double anteilDC) {
        _anteilDC = anteilDC;
    }

    final void setPhase(final double phase) {
        _phase = phase;
    }
}
