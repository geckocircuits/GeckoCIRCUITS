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
package gecko.core.circuit.losscalculation;

import java.util.Objects;

/**
 * Power loss of one semiconductor or resistor, ported unchanged from the legacy loss calculators
 * (SIMPLE, DETAILED and resistor), including their thresholds and history quirks.
 *
 * <p>The model keeps the previous current and voltage. Call {@link #update} once per grid step.</p>
 */
public final class SemiconductorLossModel {
    /** Current threshold below which a device counts as off. */
    static final double EPS = 1e-2;
    /** Temperature used when none is connected or it is not finite, like the legacy DETAILED model. */
    static final double FALLBACK_TEMPERATURE = 100;
    private static final double LARGE_TEMPERATURE = 1E4;

    private enum Kind { SIMPLE, DETAILED, RESISTIVE }

    private final Kind kind;
    private final double uf;
    private final double rOn;
    private final double kOn;
    private final double kOff;
    private final double uSwitchNorm;
    private final DetailedLossCurves curves;
    private double oldCurrent = -1;
    private double oldVoltage = -1;
    private double conduction;
    private double switching;

    private SemiconductorLossModel(Kind kind, double uf, double rOn, double kOn, double kOff, double uSwitchNorm,
            DetailedLossCurves curves) {
        this.kind = kind;
        this.uf = uf;
        this.rOn = rOn;
        this.kOn = kOn;
        this.kOff = kOff;
        this.uSwitchNorm = uSwitchNorm;
        this.curves = curves;
    }

    /** The saved SIMPLE model: forward drop, on-resistance, switching energy coefficients and blocking voltage. */
    public static SemiconductorLossModel simple(double uf, double rOn, double kOn, double kOff, double uSwitchNorm) {
        return new SemiconductorLossModel(Kind.SIMPLE, uf, rOn, kOn, kOff, uSwitchNorm, null);
    }

    /** The saved DETAILED model, with curves from the loss file. */
    public static SemiconductorLossModel detailed(DetailedLossCurves curves) {
        return new SemiconductorLossModel(Kind.DETAILED, 0, 0, 0, 0, 0, Objects.requireNonNull(curves, "Loss curves are required."));
    }

    /** A resistor: its loss is current times voltage. */
    public static SemiconductorLossModel resistive() {
        return new SemiconductorLossModel(Kind.RESISTIVE, 0, 0, 0, 0, 0, null);
    }

    /**
     * Computes the losses of one step. The temperature is the junction temperature of the previous thermal step.
     *
     * @param current the committed component current of this step
     * @param voltage the committed component voltage of this step
     * @param temperature the junction temperature in °C
     * @param dt the step width
     */
    public void update(double current, double voltage, double temperature, double dt) {
        if (kind == Kind.RESISTIVE) {
            conduction = current * voltage;
            switching = 0;
            return;
        }
        double temp = temperature;
        if (kind == Kind.DETAILED) {
            temp = temperature > LARGE_TEMPERATURE || temperature != temperature ? FALLBACK_TEMPERATURE : temperature;
        }
        double conductionLoss = kind == Kind.SIMPLE ? Math.abs(current * uf) + current * current * rOn
                : curves.conduction().getInterpolatedXValue(temp, current) * current;
        double switchingLoss = 0;
        boolean turnOn = Math.abs(oldCurrent) < EPS && Math.abs(current) > EPS;
        boolean turnOff = Math.abs(oldCurrent) > EPS && Math.abs(current) < EPS;
        if (turnOn) {
            double energy = kind == Kind.SIMPLE ? Math.abs(kOn * current / dt)
                    : curves.turnOn().getInterpolatedYValue(temp, current) / dt;
            switchingLoss += energy * voltageFactor(oldVoltage);
        }
        if (turnOff) {
            double energy = kind == Kind.SIMPLE ? Math.abs(kOff * oldCurrent / dt)
                    : curves.turnOff().getInterpolatedYValue(temp, oldCurrent) / dt;
            switchingLoss += energy * voltageFactor(voltage);
        }
        oldCurrent = current;
        oldVoltage = voltage;
        conduction = conductionLoss;
        switching = switchingLoss;
    }

    private double voltageFactor(double voltage) {
        return kind == Kind.SIMPLE ? Math.abs(voltage / uSwitchNorm) : Math.abs(voltage);
    }

    /** @return conduction plus switching loss in W */
    public double total() { return conduction + switching; }

    /** @return the conduction loss in W */
    public double conduction() { return conduction; }

    /** @return the switching loss in W */
    public double switching() { return switching; }
}
