package gecko.core.ac;

/** Evaluation of a Shockley junction at the nominal LTspice temperature of 27 °C. */
public record ShockleyDiode(double saturationCurrent, double emissionCoefficient, double resistance, double transitTime) {
    private static final double THERMAL_VOLTAGE = 1.380649e-23 * 300.15 / 1.602176634e-19;
    private static final double GMIN = 1e-12;

    public ShockleyDiode {
        if (!(saturationCurrent > 0 && Double.isFinite(saturationCurrent)
                && emissionCoefficient * THERMAL_VOLTAGE > 0 && Double.isFinite(emissionCoefficient)
                && resistance >= 0 && Double.isFinite(resistance)
                && transitTime >= 0 && Double.isFinite(transitTime))) {
            throw new IllegalArgumentException("Require finite exponential diode parameters with positive Is and N.");
        }
    }

    record Point(double current, double junctionConductance, double seriesResistance, double transitTime) {
        double conductance() {
            double slope = junctionConductance + GMIN;
            return slope / (1 + seriesResistance * slope);
        }

        Complex admittance(Complex s) {
            var junction = new Complex(junctionConductance + GMIN, 0).add(s.scale(transitTime * junctionConductance));
            return junction.divide(Complex.ONE.add(junction.scale(seriesResistance)));
        }
    }

    Point at(double voltage) {
        double scale = emissionCoefficient * THERMAL_VOLTAGE;
        double junctionVoltage = voltage;
        if (resistance > 0 && voltage != 0) {
            // The residual is monotonic. The series drop puts the junction between zero and the terminal voltage.
            double low = Math.min(voltage, 0);
            double high = Math.max(voltage, 0);
            for (int i = 0; i < 96; i++) {
                junctionVoltage = low + (high - low) / 2;
                if (junctionVoltage == low || junctionVoltage == high) break;
                double error = Math.fma(resistance, current(junctionVoltage, scale), junctionVoltage) - voltage;
                if (error == 0) break;
                if (error > 0) high = junctionVoltage;
                else low = junctionVoltage;
            }
        }
        double current = current(junctionVoltage, scale);
        double conductance = saturationCurrent * Math.exp(junctionVoltage / scale) / scale;
        if (!Double.isFinite(current) || !Double.isFinite(conductance)) {
            throw new IllegalArgumentException("The exponential diode trial exceeds the numerical range.");
        }
        return new Point(current, conductance, resistance, transitTime);
    }

    /** Terminal current and charge for the linearization of one transient trial. */
    public record TransientPoint(double current, double conductance, double junctionVoltage,
                                 double charge, double chargeCurrent) { }

    /** Evaluate a series resistor and a junction with a Backward Euler or trapezoidal charge history. */
    public TransientPoint transientAt(double terminalVoltage, double historyWeight, double oldCharge, double oldChargeCurrent) {
        double scale = emissionCoefficient * THERMAL_VOLTAGE;
        double history = Math.fma(historyWeight, oldCharge, oldChargeCurrent);
        double transportScale = Math.fma(historyWeight, transitTime, 1);
        double junction = terminalVoltage;
        if (resistance > 0) {
            // With zero exponential current, this expression bounds the junction on the far side of zero.
            double bound = Math.fma(resistance, history, terminalVoltage) / (1 + resistance * GMIN);
            double low = Math.min(0, bound), high = Math.max(0, bound);
            for (int i = 0; i < 96; i++) {
                junction = low + (high - low) / 2;
                if (junction == low || junction == high) break;
                double ampere = transportScale * saturationCurrent * Math.expm1(junction / scale) + GMIN * junction - history;
                double residual = Math.fma(resistance, ampere, junction) - terminalVoltage;
                if (residual == 0) break;
                if (residual > 0) high = junction;
                else low = junction;
            }
        }
        double transport = saturationCurrent * Math.expm1(junction / scale);
        double charge = transitTime * transport;
        double chargeCurrent = Math.fma(historyWeight, charge - oldCharge, -oldChargeCurrent);
        double current = transport + GMIN * junction + chargeCurrent;
        double tangent = transportScale * saturationCurrent * Math.exp(junction / scale) / scale + GMIN;
        double conductance = tangent / Math.fma(resistance, tangent, 1);
        if (!(Double.isFinite(current) && Double.isFinite(conductance) && Double.isFinite(charge)
                && Double.isFinite(chargeCurrent))) {
            throw new IllegalArgumentException("The transient exponential diode trial exceeds the numerical range.");
        }
        return new TransientPoint(current, conductance, junction, charge, chargeCurrent);
    }

    private double current(double voltage, double scale) {
        return saturationCurrent * Math.expm1(voltage / scale) + GMIN * voltage;
    }
}
