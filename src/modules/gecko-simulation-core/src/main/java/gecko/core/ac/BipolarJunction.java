package gecko.core.ac;

import gecko.core.io.importer.SpiceValue;
import java.util.Map;
import java.util.Set;

/** Nominal-temperature Gummel–Poon subset for an explicitly selected AC bipolar transistor. */
record BipolarJunction(int polarity, double area, double saturation, double betaForward, double betaReverse,
                       double inverseEarly, double inverseKnee, double collectorResistance,
                       double baseResistance, double emitterResistance, double emitterCapacitance,
                       double collectorCapacitance, double forwardTime, double reverseTime,
                       double transitBias, double transitVoltage, double transitCurrent) {
    private static final double VT = 1.380649e-23 * 300.15 / 1.602176634e-19;
    private static final double GMIN = 1e-12;
    private static final Set<String> PARAMETERS = Set.of("is", "bf", "br", "vaf", "ikf", "rc", "rb", "re",
            "cje", "cjc", "tf", "tr", "xtf", "vtf", "itf", "xtb", "vceo", "icrating", "mfg");

    static BipolarJunction from(AcComponent.Bipolar law, Map<String, String> scope) {
        if (!PARAMETERS.containsAll(law.parameters().keySet())) {
            throw new IllegalArgumentException("Unsupported AC bipolar transistor model parameter.");
        }
        var values = law.parameters();
        double area = positive(SpiceValue.evaluate(law.area(), scope), "area");
        return new BipolarJunction(law.pnp() ? -1 : 1, area,
                area * positive(read(values, scope, "is", 1e-16), "Is"),
                positive(read(values, scope, "bf", 100), "Bf"),
                positive(read(values, scope, "br", 1), "Br"),
                inverse(read(values, scope, "vaf", 0)), inverse(area * read(values, scope, "ikf", 0)),
                read(values, scope, "rc", 0) / area, read(values, scope, "rb", 0) / area,
                read(values, scope, "re", 0) / area, area * read(values, scope, "cje", 0),
                area * read(values, scope, "cjc", 0), read(values, scope, "tf", 0), read(values, scope, "tr", 0),
                read(values, scope, "xtf", 0), read(values, scope, "vtf", 0), area * read(values, scope, "itf", 0));
    }

    private static double read(Map<String, String> values, Map<String, String> scope, String key, double fallback) {
        String expression = values.get(key);
        if (expression == null) return fallback;
        double value = SpiceValue.evaluate(expression, scope);
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException("Require a finite nonnegative bipolar parameter " + key + ".");
        }
        return value;
    }

    private static double positive(double value, String name) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException("Require a finite positive bipolar parameter " + name + ".");
        }
        return value;
    }

    private static double inverse(double value) { return value == 0 ? 0 : 1 / value; }

    double[] seriesResistance() { return new double[]{collectorResistance, baseResistance, emitterResistance}; }

    /** Three terminal currents and their derivatives with respect to Vbe and Vbc. */
    record Point(double[] current, double[] dBe, double[] dBc, double cBe, double cBc, double cBeBc) { }

    Point at(double vBe, double vBc) {
        double be = polarity * vBe;
        double bc = polarity * vBc;
        double injectionBe = saturation * Math.expm1(be / VT);
        double injectionBc = saturation * Math.expm1(bc / VT);
        double derivativeBe = saturation * Math.exp(be / VT) / VT;
        double derivativeBc = saturation * Math.exp(bc / VT) / VT;
        double early = 1 - inverseEarly * bc;
        double highCurrent = Math.sqrt(1 + 4 * inverseKnee * injectionBe);
        double inverseCharge = 2 * early / (1 + highCurrent);
        double inverseChargeBe = -inverseCharge * 2 * inverseKnee * derivativeBe / (highCurrent * (1 + highCurrent));
        double inverseChargeBc = -2 * inverseEarly / (1 + highCurrent);
        double transfer = (injectionBe - injectionBc) * inverseCharge;
        double transferBe = derivativeBe * inverseCharge + (injectionBe - injectionBc) * inverseChargeBe;
        double transferBc = -derivativeBc * inverseCharge + (injectionBe - injectionBc) * inverseChargeBc;
        double baseBe = injectionBe / betaForward + GMIN * be;
        double baseBc = injectionBc / betaReverse + GMIN * bc;
        double baseBeSlope = derivativeBe / betaForward + GMIN;
        double baseBcSlope = derivativeBc / betaReverse + GMIN;
        double[] current = {polarity * (transfer - baseBc), polarity * (baseBe + baseBc),
                polarity * (-transfer - baseBe)};
        double[] dBe = {transferBe, baseBeSlope, -transferBe - baseBeSlope};
        double[] dBc = {transferBc - baseBcSlope, baseBcSlope, -transferBc};

        double diffusionBe = 0;
        double diffusionBeBc = 0;
        if (be > 0) {
            double biasFactor = 0, derivative = 0, crossDerivative = 0;
            if (transitBias != 0) {
                double factor = transitVoltage > 0 ? Math.exp(bc / (1.44 * transitVoltage)) : 1;
                double ratio = transitCurrent > 0 ? injectionBe / (injectionBe + transitCurrent) : 1;
                biasFactor = transitBias * factor * ratio * ratio;
                if (transitCurrent > 0) {
                    derivative = 2 * transitBias * factor * ratio * (1 - ratio) / (injectionBe + transitCurrent);
                }
                if (transitVoltage > 0) crossDerivative = biasFactor / (1.44 * transitVoltage);
            }
            double storedCurrent = injectionBe * inverseCharge;
            diffusionBe = forwardTime * ((1 + biasFactor)
                    * (derivativeBe * inverseCharge + injectionBe * inverseChargeBe) + storedCurrent * derivative * derivativeBe);
            diffusionBeBc = forwardTime * ((1 + biasFactor) * injectionBe * inverseChargeBc + storedCurrent * crossDerivative);
        }
        double cBe = ShockleyDiode.depletion(emitterCapacitance, be, .75, .33) + diffusionBe;
        double cBc = ShockleyDiode.depletion(collectorCapacitance, bc, .75, .33) + reverseTime * derivativeBc;
        if (!(early > 0 && Double.isFinite(cBe) && Double.isFinite(cBc) && Double.isFinite(diffusionBeBc))) {
            throw new IllegalArgumentException("The bipolar trial exceeds the model range.");
        }
        for (int i = 0; i < 3; i++) {
            if (!Double.isFinite(current[i]) || !Double.isFinite(dBe[i]) || !Double.isFinite(dBc[i])) {
                throw new IllegalArgumentException("The bipolar trial exceeds the numerical range.");
            }
        }
        return new Point(current, dBe, dBc, cBe, cBc, diffusionBeBc);
    }

}
