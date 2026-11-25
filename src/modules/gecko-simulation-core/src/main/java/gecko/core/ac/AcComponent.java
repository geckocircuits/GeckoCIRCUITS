package gecko.core.ac;

import gecko.core.io.importer.SpiceValue;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Source identity and active constitutive law owned by one component. */
public record AcComponent(String instance, List<Integer> spiceOrders, List<String> aliases, boolean mirrored, Law law) {
    public AcComponent {
        if (instance.isBlank() || law == null) throw new IllegalArgumentException("Require an instance and its law.");
        spiceOrders = List.copyOf(spiceOrders);
        var expectedOrders = law instanceof Controlled ? List.of(1, 2, 3, 4) : List.of(1, 2);
        if (!spiceOrders.equals(expectedOrders)) throw new IllegalArgumentException("Invalid source terminal order.");
        aliases = List.copyOf(aliases);
    }

    public AcComponent(String instance, List<String> aliases, boolean mirrored, Law law) {
        this(instance, law instanceof Controlled ? List.of(1, 2, 3, 4) : List.of(1, 2), aliases, mirrored, law);
    }

    public sealed interface Law permits Passive, Inductance, Voltage, Current, Controlled { }

    public record Passive(String expression) implements Law { }
    public record Inductance(String expression, Resistance resistance, AcInputOrigin origin) implements Law {
        public Inductance {
            Objects.requireNonNull(resistance);
            Objects.requireNonNull(origin);
            if (expression.isBlank() || resistance instanceof ImportedDefault && !origin.tested()) {
                throw new IllegalArgumentException("Require a valid AC inductor law.");
            }
        }

        public Inductance(String expression, String seriesResistance) {
            this(expression, new ExplicitResistance(seriesResistance), AcInputOrigin.LEGACY);
        }

        public EvaluatedInductance evaluate(Map<String, String> parameters) {
            double inductance = SpiceValue.evaluate(expression, parameters);
            double ohms = resistance instanceof ExplicitResistance explicit
                    ? SpiceValue.evaluate(explicit.expression(), parameters) : origin.defaultSeriesResistance();
            if (!Double.isFinite(inductance) || inductance <= 0 || !Double.isFinite(ohms)
                    || ohms < 0 || ohms == 0 && !origin.tested()) {
                throw new IllegalArgumentException("Inductance or series resistance is outside the AC law domain.");
            }
            ResistanceMode mode = resistance instanceof ImportedDefault ? ResistanceMode.IMPORTED_DEFAULT
                    : ohms == 0 ? ResistanceMode.EXPLICIT_ZERO : ResistanceMode.EXPLICIT_POSITIVE;
            return new EvaluatedInductance(inductance, ohms, mode);
        }
    }

    public sealed interface Resistance permits ImportedDefault, ExplicitResistance { }
    public record ImportedDefault() implements Resistance { }
    public record ExplicitResistance(String expression) implements Resistance {
        public ExplicitResistance {
            if (expression.isBlank()) throw new IllegalArgumentException("Enter a series resistance expression.");
        }
    }
    public enum ResistanceMode {
        IMPORTED_DEFAULT("Imported default"),
        EXPLICIT_ZERO("Explicit zero"),
        EXPLICIT_POSITIVE("Explicit positive");

        private final String label;
        ResistanceMode(String label) { this.label = label; }
        public String label() { return label; }
    }
    public record EvaluatedInductance(double henries, double ohms, ResistanceMode mode) { }

    public record Voltage(String dc, String waveform, String magnitude, String phaseDegrees) implements Law { }
    public record Current(String dc, String magnitude, String phaseDegrees) implements Law { }
    public record Controlled(String expression, boolean laplace) implements Law { }
}
