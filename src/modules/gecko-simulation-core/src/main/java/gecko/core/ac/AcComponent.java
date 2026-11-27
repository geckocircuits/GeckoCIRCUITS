package gecko.core.ac;

import gecko.core.io.importer.SpiceValue;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Source identity and active constitutive law owned by one component. */
public record AcComponent(String instance, List<Integer> spiceOrders, List<String> aliases, boolean mirrored, Law law) {
    public AcComponent {
        if (instance.isBlank() || law == null) throw new IllegalArgumentException("Require an instance and its law.");
        spiceOrders = List.copyOf(spiceOrders);
        var expectedOrders = terminalOrders(law);
        if (!spiceOrders.equals(expectedOrders)) throw new IllegalArgumentException("Invalid source terminal order.");
        aliases = List.copyOf(aliases);
    }

    public AcComponent(String instance, List<String> aliases, boolean mirrored, Law law) {
        this(instance, terminalOrders(law), aliases, mirrored, law);
    }

    private static List<Integer> terminalOrders(Law law) {
        if (law instanceof Bipolar || law instanceof Mosfet) return List.of(1, 2, 3);
        return law instanceof Controlled || law instanceof Transconductance ? List.of(1, 2, 3, 4) : List.of(1, 2);
    }

    public sealed interface Law permits Passive, Inductance, Voltage, Current, Controlled, Transconductance, IdealDiode, ExponentialDiode, Bipolar, Mosfet { }

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
    public record IdealDiode(String modelName, String forwardVoltage, String onResistance, String offResistance,
                             AcInputOrigin origin) implements Law {
        public IdealDiode {
            Objects.requireNonNull(origin);
            if (!modelName.matches("[A-Za-z0-9_.$+-]+") || forwardVoltage.isBlank()) {
                throw new IllegalArgumentException("Require an ideal diode model name and forward threshold.");
            }
            modelName = modelName.toLowerCase(Locale.ROOT);
            if (offResistance.isBlank() && !origin.tested()) {
                throw new IllegalArgumentException("Specify Roff or select the LTspice profile with known Gmin.");
            }
        }

        public EvaluatedDiode evaluate(Map<String, String> parameters) {
            double vfwd = SpiceValue.evaluate(forwardVoltage, parameters);
            double ron = onResistance.isBlank() ? 1e-3 : SpiceValue.evaluate(onResistance, parameters);
            double roff = offResistance.isBlank() ? 1e12 : SpiceValue.evaluate(offResistance, parameters);
            double gon = 1 / ron, goff = 1 / roff;
            if (vfwd < 0 || ron <= 0 || roff <= ron || !Double.isFinite(gon)
                    || !(goff > 0) || !Double.isFinite(vfwd * gon)) {
                throw new IllegalArgumentException("Require finite ideal diode coefficients and 0 < Ron < Roff.");
            }
            return new EvaluatedDiode(vfwd, ron, roff);
        }
    }

    /** Imported nominal-temperature AC bipolar transistor with collector, base, and emitter terminals. */
    public record Mosfet(String modelName, boolean pChannel, String width, String length,
                         Map<String, String> parameters) implements Law {
        public Mosfet {
            requireLocalModelName(modelName);
            parameters = Map.copyOf(parameters);
        }

        public MosfetModel evaluate(Map<String, String> scope) {
            return MosfetModel.from(this, scope);
        }
    }

    private static void requireLocalModelName(String modelName) {
        if (!modelName.matches("[A-Za-z0-9_.$+-]+")) {
            throw new IllegalArgumentException("Require a local semiconductor model name.");
        }
    }

    public record Bipolar(String modelName, boolean pnp, String area, Map<String, String> parameters) implements Law {
        public Bipolar {
            requireLocalModelName(modelName);
            parameters = Map.copyOf(parameters);
        }

        public void validate(Map<String, String> scope) { BipolarJunction.from(this, scope); }
    }

    /** An explicitly selected, nominal-temperature AC diode law. */
    public record ExponentialDiode(String modelName, String saturationCurrent, String emissionCoefficient,
                                   String seriesResistance, String transitTime, String junctionCapacitance,
                                   String junctionPotential, String gradingCoefficient, boolean transientLaw) implements Law {
        public ExponentialDiode(String modelName, String saturationCurrent, String emissionCoefficient,
                                String seriesResistance, String transitTime) {
            this(modelName, saturationCurrent, emissionCoefficient, seriesResistance, transitTime, false);
        }

        public ExponentialDiode(String modelName, String saturationCurrent, String emissionCoefficient,
                                String seriesResistance, String transitTime, boolean transientLaw) {
            this(modelName, saturationCurrent, emissionCoefficient, seriesResistance, transitTime, "0", "1", "0.5", transientLaw);
        }

        public ExponentialDiode(String modelName, String saturationCurrent, String emissionCoefficient,
                                String seriesResistance, String transitTime, String junctionCapacitance,
                                String junctionPotential, String gradingCoefficient) {
            this(modelName, saturationCurrent, emissionCoefficient, seriesResistance, transitTime,
                    junctionCapacitance, junctionPotential, gradingCoefficient, false);
        }

        public ExponentialDiode {
            if (!modelName.matches("[A-Za-z0-9_.$+-]+")) {
                throw new IllegalArgumentException("Require a local exponential diode model name.");
            }
            if (transientLaw && !("0".equals(junctionCapacitance) && "1".equals(junctionPotential)
                    && "0.5".equals(gradingCoefficient))) {
                throw new IllegalArgumentException("Transient exponential diodes support diffusion charge only.");
            }
            modelName = modelName.toLowerCase(Locale.ROOT);
        }

        public void validate(Map<String, String> parameters) {
            evaluate(parameters);
        }

        public ShockleyDiode evaluate(Map<String, String> parameters) {
            return new ShockleyDiode(SpiceValue.evaluate(saturationCurrent, parameters),
                    SpiceValue.evaluate(emissionCoefficient, parameters), SpiceValue.evaluate(seriesResistance, parameters),
                    SpiceValue.evaluate(transitTime, parameters), SpiceValue.evaluate(junctionCapacitance, parameters),
                    SpiceValue.evaluate(junctionPotential, parameters), SpiceValue.evaluate(gradingCoefficient, parameters));
        }
    }

    public record EvaluatedDiode(double forwardVoltage, double onResistance, double offResistance) {
        double conductance(boolean on) { return 1 / (on ? onResistance : offResistance); }
    }

    public record Controlled(String expression, boolean laplace) implements Law { }
    public record Transconductance(String expression) implements Law {
        public Transconductance {
            if (expression.isBlank()) throw new IllegalArgumentException("Enter a transconductance expression.");
        }
    }
}
