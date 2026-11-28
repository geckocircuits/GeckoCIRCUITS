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
package gecko.core.testutil;

/**
 * Exact analytical solutions for simple circuit transients.
 * Used to verify numerical simulation results against known closed-form answers.
 */
public final class AnalyticalSolutions {

    private AnalyticalSolutions() {
    }

    // ========================================================================
    // Resistor divider (DC steady-state)
    // ========================================================================

    /**
     * Voltage at midpoint of a resistive divider: Vin → R1 → node → R2 → GND.
     *
     * @param vin input voltage [V]
     * @param r1  upper resistor [Ω]
     * @param r2  lower resistor [Ω]
     * @return V_mid = Vin * R2 / (R1 + R2)
     */
    public static double resistorDividerVoltage(double vin, double r1, double r2) {
        return vin * r2 / (r1 + r2);
    }

    /**
     * Current through a resistive divider.
     *
     * @return I = Vin / (R1 + R2)
     */
    public static double resistorDividerCurrent(double vin, double r1, double r2) {
        return vin / (r1 + r2);
    }

    // ========================================================================
    // RL step response: Vin → R → L → GND  (voltage source applied at t=0)
    // ========================================================================

    /**
     * RL series current step response (DC source).
     * <pre>
     *   I(t) = (Vin/R) * (1 - exp(-R*t/L))
     * </pre>
     *
     * @param vin input voltage [V]
     * @param r   resistance [Ω]
     * @param l   inductance [H]
     * @param t   time [s]
     * @return inductor current at time t
     */
    public static double rlStepCurrent(double vin, double r, double l, double t) {
        double tau = l / r;
        return (vin / r) * (1.0 - Math.exp(-t / tau));
    }

    /**
     * RL series steady-state current.
     *
     * @return Iss = Vin / R
     */
    public static double rlSteadyStateCurrent(double vin, double r) {
        return vin / r;
    }

    /**
     * RL time constant.
     *
     * @return τ = L / R
     */
    public static double rlTimeConstant(double r, double l) {
        return l / r;
    }

    // ========================================================================
    // RC step response: Vin → R → node → C → GND  (voltage source applied at t=0)
    // ========================================================================

    /**
     * RC series capacitor voltage step response (DC source).
     * <pre>
     *   V_C(t) = Vin * (1 - exp(-t / (R*C)))
     * </pre>
     *
     * @param vin input voltage [V]
     * @param r   resistance [Ω]
     * @param c   capacitance [F]
     * @param t   time [s]
     * @return capacitor voltage at time t
     */
    public static double rcStepVoltage(double vin, double r, double c, double t) {
        double tau = r * c;
        return vin * (1.0 - Math.exp(-t / tau));
    }

    /**
     * RC series current step response (DC source).
     * <pre>
     *   I(t) = (Vin/R) * exp(-t / (R*C))
     * </pre>
     *
     * @param vin input voltage [V]
     * @param r   resistance [Ω]
     * @param c   capacitance [F]
     * @param t   time [s]
     * @return current at time t
     */
    public static double rcStepCurrent(double vin, double r, double c, double t) {
        double tau = r * c;
        return (vin / r) * Math.exp(-t / tau);
    }

    /**
     * RC time constant.
     *
     * @return τ = R * C
     */
    public static double rcTimeConstant(double r, double c) {
        return r * c;
    }

    // ========================================================================
    // Diode forward/reverse
    // ========================================================================

    /**
     * Forward current through R + ideal-diode (Vin > Uf).
     *
     * @param vin input voltage [V]
     * @param r   series resistance [Ω]
     * @param uf  diode forward voltage [V]
     * @return I = (Vin - Uf) / R  (clamped to 0 if Vin < Uf)
     */
    public static double diodeForwardCurrent(double vin, double r, double uf) {
        if (vin <= uf) {
            return 0.0;
        }
        return (vin - uf) / r;
    }

    // ========================================================================
    // Buck converter (CCM, ideal components)
    // ========================================================================

    /**
     * Ideal buck converter output voltage in CCM.
     *
     * @param vin  input voltage [V]
     * @param duty duty cycle (0..1)
     * @return Vo = D * Vin
     */
    public static double buckOutputVoltage(double vin, double duty) {
        return duty * vin;
    }

    /**
     * Buck converter output (load) current in CCM.
     *
     * @param vin   input voltage [V]
     * @param duty  duty cycle (0..1)
     * @param rLoad load resistance [Ω]
     * @return Io = D * Vin / R_load
     */
    public static double buckOutputCurrent(double vin, double duty, double rLoad) {
        return duty * vin / rLoad;
    }

    /**
     * Buck inductor peak-to-peak ripple current in CCM.
     *
     * @param vin  input voltage [V]
     * @param vout output voltage [V]
     * @param l    inductance [H]
     * @param freq switching frequency [Hz]
     * @param duty duty cycle (0..1)
     * @return ΔI = (Vin - Vout) * D / (L * f)
     */
    public static double buckInductorRipple(double vin, double vout, double l, double freq, double duty) {
        return (vin - vout) * duty / (l * freq);
    }

    // ========================================================================
    // RLC series circuit
    // ========================================================================

    /**
     * Natural (undamped) angular frequency of a series RLC circuit.
     *
     * @param l inductance [H]
     * @param c capacitance [F]
     * @return ω₀ = 1/√(LC) [rad/s]
     */
    public static double rlcSeriesNaturalFrequency(double l, double c) {
        return 1.0 / Math.sqrt(l * c);
    }

    /**
     * Damping ratio of a series RLC circuit.
     *
     * @param r resistance [Ω]
     * @param l inductance [H]
     * @param c capacitance [F]
     * @return ζ = R / (2√(L/C))
     */
    public static double rlcSeriesDampingRatio(double r, double l, double c) {
        return r / (2.0 * Math.sqrt(l / c));
    }

    /**
     * Capacitor voltage step response of a series RLC circuit (DC source applied at t=0).
     * Handles underdamped (ζ &lt; 1), critically damped (ζ = 1), and overdamped (ζ &gt; 1) cases.
     *
     * <pre>
     * Underdamped (ζ &lt; 1):
     *   Vc(t) = Vin * (1 - e^(-σt) * (cos(ωd*t) + σ/ωd * sin(ωd*t)))
     *   where σ = ζ*ω₀, ωd = ω₀*√(1-ζ²)
     *
     * Critically damped (ζ = 1):
     *   Vc(t) = Vin * (1 - (1 + ω₀*t) * e^(-ω₀*t))
     *
     * Overdamped (ζ &gt; 1):
     *   Vc(t) = Vin * (1 - e^(-σt) * (cosh(ωn*t) + σ/ωn * sinh(ωn*t)))
     *   where ωn = ω₀*√(ζ²-1)
     * </pre>
     *
     * @param vin input voltage [V]
     * @param r   resistance [Ω]
     * @param l   inductance [H]
     * @param c   capacitance [F]
     * @param t   time [s]
     * @return capacitor voltage at time t
     */
    public static double rlcSeriesStepVoltage(double vin, double r, double l, double c, double t) {
        if (t <= 0) return 0.0;

        double omega0 = rlcSeriesNaturalFrequency(l, c);
        double zeta = rlcSeriesDampingRatio(r, l, c);
        double sigma = zeta * omega0;

        if (zeta < 0.999) {
            // Underdamped
            double omegaD = omega0 * Math.sqrt(1.0 - zeta * zeta);
            return vin * (1.0 - Math.exp(-sigma * t)
                    * (Math.cos(omegaD * t) + (sigma / omegaD) * Math.sin(omegaD * t)));
        } else if (zeta > 1.001) {
            // Overdamped
            double omegaN = omega0 * Math.sqrt(zeta * zeta - 1.0);
            return vin * (1.0 - Math.exp(-sigma * t)
                    * (Math.cosh(omegaN * t) + (sigma / omegaN) * Math.sinh(omegaN * t)));
        } else {
            // Critically damped
            return vin * (1.0 - (1.0 + omega0 * t) * Math.exp(-omega0 * t));
        }
    }

    // ========================================================================
    // AC frequency response: RL low-pass
    // ========================================================================

    /**
     * Gain of an RL low-pass filter at a given frequency.
     * Transfer function: H(jω) = R / (R + jωL), measuring voltage across R.
     * Wait — for Vin → R → L → GND, the "output" across L is:
     *   H(jω) = jωL / (R + jωL)
     * But measuring across R:
     *   H(jω) = R / (R + jωL)
     *
     * This method returns |H| for the voltage across R (the low-pass response
     * when output is taken across R in a series RL driven by Vin).
     *
     * Actually, for Vin → R → L → GND:
     * - Voltage across L: V_L = Vin * jωL / (R + jωL)  [high-pass]
     * - Voltage across R: V_R = Vin * R / (R + jωL)     [low-pass]
     *
     * For an RL low-pass (output across R): |H| = R / sqrt(R² + (ωL)²)
     *
     * @param r    resistance [Ω]
     * @param l    inductance [H]
     * @param freq frequency [Hz]
     * @return |H(jω)| = R / √(R² + (ωL)²)
     */
    public static double rlGainAtFrequency(double r, double l, double freq) {
        double omega = 2.0 * Math.PI * freq;
        double xl = omega * l;
        return r / Math.sqrt(r * r + xl * xl);
    }

    /**
     * Phase of RL low-pass response (output across R).
     *
     * @param r    resistance [Ω]
     * @param l    inductance [H]
     * @param freq frequency [Hz]
     * @return phase angle [rad] = -atan(ωL/R)
     */
    public static double rlPhaseAtFrequency(double r, double l, double freq) {
        double omega = 2.0 * Math.PI * freq;
        return -Math.atan(omega * l / r);
    }

    // ========================================================================
    // AC frequency response: RC low-pass
    // ========================================================================

    /**
     * Gain of an RC low-pass filter at a given frequency.
     * For Vin → R → C → GND, output across C:
     *   H(jω) = 1/(jωC) / (R + 1/(jωC)) = 1 / (1 + jωRC)
     *
     * @param r    resistance [Ω]
     * @param c    capacitance [F]
     * @param freq frequency [Hz]
     * @return |H(jω)| = 1 / √(1 + (ωRC)²)
     */
    public static double rcGainAtFrequency(double r, double c, double freq) {
        double omega = 2.0 * Math.PI * freq;
        double wrc = omega * r * c;
        return 1.0 / Math.sqrt(1.0 + wrc * wrc);
    }

    /**
     * Phase of RC low-pass response (output across C).
     *
     * @param r    resistance [Ω]
     * @param c    capacitance [F]
     * @param freq frequency [Hz]
     * @return phase angle [rad] = -atan(ωRC)
     */
    public static double rcPhaseAtFrequency(double r, double c, double freq) {
        double omega = 2.0 * Math.PI * freq;
        return -Math.atan(omega * r * c);
    }

    // ========================================================================
    // Boost converter (CCM, ideal components)
    // ========================================================================

    /**
     * Ideal boost converter output voltage in CCM.
     *
     * @param vin  input voltage [V]
     * @param duty duty cycle (0..1)
     * @return Vo = Vin / (1 - D)
     */
    public static double boostOutputVoltage(double vin, double duty) {
        return vin / (1.0 - duty);
    }

    /**
     * Boost converter output (load) current in CCM.
     *
     * @param vin   input voltage [V]
     * @param duty  duty cycle (0..1)
     * @param rLoad load resistance [Ω]
     * @return Io = Vin / ((1 - D) * R)
     */
    public static double boostOutputCurrent(double vin, double duty, double rLoad) {
        return vin / ((1.0 - duty) * rLoad);
    }

    // ========================================================================
    // Buck-boost converter (CCM, ideal components)
    // ========================================================================

    /**
     * Ideal buck-boost converter output voltage in CCM.
     * Output is inverted (negative w.r.t. GND).
     *
     * @param vin  input voltage [V]
     * @param duty duty cycle (0..1)
     * @return Vo = -Vin * D / (1 - D)
     */
    public static double buckBoostOutputVoltage(double vin, double duty) {
        return -vin * duty / (1.0 - duty);
    }

    /**
     * Buck-boost converter output (load) current magnitude in CCM.
     *
     * @param vin   input voltage [V]
     * @param duty  duty cycle (0..1)
     * @param rLoad load resistance [Ω]
     * @return |Io| = Vin * D / ((1 - D) * R)
     */
    public static double buckBoostOutputCurrent(double vin, double duty, double rLoad) {
        return vin * duty / ((1.0 - duty) * rLoad);
    }

    // ========================================================================
    // Thermal domain
    // ========================================================================

    /**
     * Temperature rise of a thermal RC network with constant power input.
     * <pre>
     *   T(t) = T_ambient + P * Rth * (1 - e^(-t / (Rth * Cth)))
     * </pre>
     *
     * @param power    heat flow [W]
     * @param rth      thermal resistance [K/W]
     * @param cth      thermal capacitance [J/K]
     * @param tAmbient ambient temperature [°C or K]
     * @param t        time [s]
     * @return temperature at time t
     */
    public static double thermalRCStepTemperature(double power, double rth, double cth,
                                                    double tAmbient, double t) {
        double tau = rth * cth;
        return tAmbient + power * rth * (1.0 - Math.exp(-t / tau));
    }

    /**
     * Steady-state temperature of a thermal RC network.
     *
     * @param power    heat flow [W]
     * @param rth      thermal resistance [K/W]
     * @param tAmbient ambient temperature [°C or K]
     * @return T_ss = T_ambient + P * Rth
     */
    public static double thermalSteadyStateTemperature(double power, double rth, double tAmbient) {
        return tAmbient + power * rth;
    }

    /**
     * Thermal time constant.
     *
     * @param rth thermal resistance [K/W]
     * @param cth thermal capacitance [J/K]
     * @return τ = Rth * Cth
     */
    public static double thermalTimeConstant(double rth, double cth) {
        return rth * cth;
    }

    // ========================================================================
    // RLC bandpass
    // ========================================================================

    /**
     * Resonant frequency of a series RLC circuit.
     *
     * @param l inductance [H]
     * @param c capacitance [F]
     * @return f₀ = 1 / (2π√(LC)) [Hz]
     */
    public static double rlcResonantFrequency(double l, double c) {
        return 1.0 / (2.0 * Math.PI * Math.sqrt(l * c));
    }

    /**
     * Quality factor of a series RLC circuit.
     *
     * @param r resistance [Ω]
     * @param l inductance [H]
     * @param c capacitance [F]
     * @return Q = (1/R) * √(L/C)
     */
    public static double rlcQualityFactor(double r, double l, double c) {
        return Math.sqrt(l / c) / r;
    }

    /**
     * Impedance magnitude of a series RLC circuit at a given frequency.
     * Z = R + j(ωL - 1/(ωC)), |Z| = √(R² + (ωL - 1/(ωC))²)
     *
     * @param r    resistance [Ω]
     * @param l    inductance [H]
     * @param c    capacitance [F]
     * @param freq frequency [Hz]
     * @return |Z| at the given frequency
     */
    public static double rlcSeriesImpedanceMagnitude(double r, double l, double c, double freq) {
        double omega = 2.0 * Math.PI * freq;
        double xl = omega * l;
        double xc = 1.0 / (omega * c);
        double reactance = xl - xc;
        return Math.sqrt(r * r + reactance * reactance);
    }

    /**
     * Gain (current amplitude ratio) of a series RLC bandpass at a given frequency.
     * The current through the series RLC is I = V / |Z|.
     * The bandpass gain normalized to peak (at resonance where |Z| = R) is:
     *   G(f) = R / |Z(f)| = R / √(R² + (ωL - 1/(ωC))²)
     *
     * @param r    resistance [Ω]
     * @param l    inductance [H]
     * @param c    capacitance [F]
     * @param freq frequency [Hz]
     * @return normalized gain (0..1), peak = 1 at resonance
     */
    public static double rlcBandpassGain(double r, double l, double c, double freq) {
        return r / rlcSeriesImpedanceMagnitude(r, l, c, freq);
    }

    /**
     * -3dB bandwidth of a series RLC bandpass filter.
     *
     * @param r resistance [Ω]
     * @param l inductance [H]
     * @return BW = R / (2πL) [Hz]
     */
    public static double rlcBandpassBandwidth(double r, double l) {
        return r / (2.0 * Math.PI * l);
    }
}
