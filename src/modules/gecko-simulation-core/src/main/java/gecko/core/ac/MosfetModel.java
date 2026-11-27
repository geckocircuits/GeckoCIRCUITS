package gecko.core.ac;

import gecko.core.io.importer.SpiceValue;
import java.util.Map;
import java.util.Set;

/** Level 1 channel and source-tied bulk diode at nominal temperature. */
public record MosfetModel(boolean pChannel, double threshold, double beta, double lambda, ShockleyDiode bodyDiode) {
    private static final Set<String> SUPPORTED = Set.of("level", "vto", "kp", "lambda", "is");

    static MosfetModel from(AcComponent.Mosfet law, Map<String, String> scope) {
        var parameters = law.parameters();
        if (!SUPPORTED.containsAll(parameters.keySet())) {
            throw new IllegalArgumentException("The Level 1 MOSFET model has unsupported parameters.");
        }
        if (value(parameters, "level", "1", scope) != 1) {
            throw new IllegalArgumentException("Use a Level 1 MOSFET model.");
        }
        double width = SpiceValue.evaluate(law.width(), scope);
        double length = SpiceValue.evaluate(law.length(), scope);
        double beta = value(parameters, "kp", "", scope) * (width / length);
        double threshold = value(parameters, "vto", "", scope) * (law.pChannel() ? -1 : 1);
        double lambda = value(parameters, "lambda", "0", scope);
        double saturation = value(parameters, "is", "1e-14", scope);
        if (!(width > 0 && length > 0 && beta > 0 && Double.isFinite(beta) && lambda >= 0
                && Double.isFinite(lambda) && Double.isFinite(threshold))) {
            throw new IllegalArgumentException("Require finite Level 1 MOSFET parameters with positive W, L, and Kp.");
        }
        return new MosfetModel(law.pChannel(), threshold, beta, lambda, new ShockleyDiode(saturation, 1, 0, 0));
    }

    private static double value(Map<String, String> parameters, String name, String fallback, Map<String, String> scope) {
        return SpiceValue.evaluate(parameters.getOrDefault(name, fallback), scope);
    }

    public record Point(double current, double drainSlope, double gateSlope) { }

    /** Evaluate the current into the drain and its derivatives with respect to drain and gate voltage. */
    public Point at(double drainSource, double gateSource) {
        double polarity = pChannel ? -1 : 1;
        double normalizedDrain = polarity * drainSource;
        double normalizedGate = polarity * gateSource;
        boolean reverse = normalizedDrain < 0;
        double span = Math.abs(normalizedDrain);
        double overdrive = normalizedGate + (reverse ? span : 0) - threshold;
        double channel = 0;
        double drainSlope = 0;
        double gateSlope = 0;
        if (overdrive > 0) {
            double limitedDrain = Math.min(span, overdrive);
            double baseCurrent = beta * limitedDrain * (overdrive - 0.5 * limitedDrain);
            double modulation = 1 + lambda * span;
            channel = modulation * baseCurrent;
            gateSlope = modulation * beta * limitedDrain;
            drainSlope = modulation * beta * (overdrive - limitedDrain) + lambda * baseCurrent;
        }
        var diode = bodyDiode.at(-normalizedDrain);
        var point = reverse
                ? new Point(polarity * (-channel - diode.current()), drainSlope + gateSlope + diode.conductance(), -gateSlope)
                : new Point(polarity * (channel - diode.current()), drainSlope + diode.conductance(), gateSlope);
        if (!(Double.isFinite(point.current()) && Double.isFinite(point.drainSlope()) && Double.isFinite(point.gateSlope()))) {
            throw new IllegalArgumentException("The MOSFET trial exceeds the numerical range.");
        }
        return point;
    }
}
