package gecko.core.preparation;

import gecko.core.io.CircuitModel;
import gecko.core.io.ImportIssue;
import gecko.core.simulation.ControlBlockDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** One table binds saved slots and public names to configured fields. */
public final class CircuitConfiguration {
    private CircuitConfiguration() {}

    /**
     * Resolves unnormalized saved CONTROL settings on a detached model.
     * The copy retains file version, optimizer definitions, and captured resources.
     * Electrical and thermal settings do not pass through normalization or validation.
     * Resolved and synthetic descriptions retain their existing numeric settings.
     *
     * @param model the model with its saved configuration context
     * @return a detached resolved model, or the original model when no resolution is necessary
     * @throws IllegalArgumentException if a saved CONTROL setting is invalid
     */
    public static CircuitModel resolveSavedControl(CircuitModel model) {
        if (unresolvedControl(model).isEmpty()) return model;
        var resolved = new CircuitSnapshot(model).toModel();
        var entries = unresolvedControl(resolved);
        var diagnostics = normalize(resolved, entries);
        diagnostics.addAll(validate(resolved, entries));
        for (var diagnostic : diagnostics) {
            if (diagnostic.severity() == Diagnostic.Severity.ERROR) {
                throw new IllegalArgumentException(diagnostic.message() + " [" + diagnostic.parameterPath() + "]");
            }
        }
        return resolved;
    }

    private static List<Component> unresolvedControl(CircuitModel model) {
        return model.getControlComponents().stream().map(data -> new Component("CONTROL", data))
                .filter(entry -> entry.data().isParsedInput() && !entry.data().isConfigurationNormalized())
                .filter(entry -> !fields(entry).isEmpty()).toList();
    }


    enum Rule {
        FINITE, NON_NEGATIVE, POSITIVE, BOOLEAN, INTEGER, UNIT_INTERVAL, SIGNED_UNIT_INTERVAL, SOURCE_MODE, SIGNAL_MODE;
        boolean accepts(double value) {
            return Double.isFinite(value) && switch (this) {
                case FINITE -> true;
                case NON_NEGATIVE -> value >= 0;
                case POSITIVE -> value > 0;
                case BOOLEAN -> value == 0 || value == 1;
                case INTEGER -> value == Math.rint(value);
                case UNIT_INTERVAL -> value >= 0 && value <= 1;
                case SIGNED_UNIT_INTERVAL -> value >= -1 && value <= 1;
                case SOURCE_MODE -> value == Math.rint(value) && (value >= 0 && value <= 6 || value >= 396 && value <= 402);
                case SIGNAL_MODE -> value == Math.rint(value) && (value >= 0 && value <= 4 || value >= 400 && value <= 406);
            };
        }
    }
    record Field(String name, int slot, Rule rule, List<String> aliases) {
        Field(String name, int slot, Rule rule, String... aliases) { this(name, slot, rule, List.of(aliases)); }
        boolean matches(String key) { return name.equals(key) || slot >= 0 && ("param" + slot).equals(key) || aliases.contains(key); }
    }
    record Component(String domain, CircuitModel.ComponentData data) {}
    record Binding(Component component, Field field) {}

    static List<Field> fields(Component component) {
        if (component.domain().equals("LK")) {
            return switch (component.data().getType()) {
                case 1, 24, 46 -> List.of(new Field("resistance", 0, Rule.NON_NEGATIVE));
                case 2, 12 -> List.of(new Field("inductance", 0, component.data().getType() == 12 ? Rule.POSITIVE : Rule.NON_NEGATIVE),
                        new Field("initialCurrent", 1, Rule.FINITE));
                case 3, 47 -> List.of(new Field("capacitance", 0, Rule.POSITIVE), new Field("initVoltage", 1, Rule.FINITE, "initialVoltage"));
                case 6, 8, 10 -> List.of(new Field("forwardVoltageDrop", 1, Rule.NON_NEGATIVE),
                        new Field("onResistance", 2, Rule.POSITIVE), new Field("offResistance", 3, Rule.POSITIVE));
                case 28 -> List.of(new Field("onResistance", 2, Rule.POSITIVE), new Field("offResistance", 3, Rule.POSITIVE));
                case 7 -> List.of(new Field("onResistance", 1, Rule.POSITIVE), new Field("offResistance", 2, Rule.POSITIVE));
                case 9 -> List.of(new Field("couplingCoefficient", 0, Rule.SIGNED_UNIT_INTERVAL, "k"));
                case 4, 5, 26, 44, 45 -> List.of(new Field("sourceType", 0, Rule.SOURCE_MODE), new Field("dcValue", 1, Rule.FINITE),
                        new Field("frequency", 2, Rule.NON_NEGATIVE), new Field("offset", 3, Rule.FINITE),
                        new Field("phaseDegrees", 4, Rule.FINITE, "phase"), new Field("amplitude", 20, Rule.FINITE));
                default -> List.of();
            };
        }
        if (component.domain().equals("CONTROL")) {
            var descriptor = ControlBlockDescriptor.find(component.data().getType());
            return descriptor == null ? List.of() : descriptor.settings().stream()
                    .map(setting -> new Field(setting.name(), setting.slot(), Rule.valueOf(setting.rule().name()), setting.aliases()))
                    .toList();
        }
        return List.of();
    }

    static List<Field> unsupportedFields(Component component) {
        if (!component.domain().equals("LK")) return List.of();
        int type = component.data().getType();
        List<Field> result = new ArrayList<>();
        if (List.of(6, 7, 8, 10, 28).contains(type)) {
            result.addAll(List.of(new Field("kON", 6, Rule.FINITE), new Field("kOFF", 7, Rule.FINITE),
                    new Field("numberParalleled", 12, Rule.FINITE), new Field("uSWnorm", -1, Rule.FINITE)));
        }
        result.addAll(switch (type) {
            case 28 -> List.of(new Field("antiParallelDiodeRon", -1, Rule.FINITE),
                    new Field("antiParallelDiodeRoff", -1, Rule.FINITE), new Field("antiParallelDiodeUF", -1, Rule.FINITE));
            case 6 -> List.of(new Field("useNonlinearChar", -1, Rule.FINITE));
            case 8 -> List.of(new Field("reverserecoverydelay", 9, Rule.FINITE, "reverseRecoveryDelay"));
            case 10 -> List.of(new Field("satCurEnabled", 10, Rule.FINITE), new Field("saturationCurrent", 11, Rule.FINITE));
            default -> List.of();
        });
        return List.copyOf(result);
    }

    static List<Component> components(CircuitModel model) {
        List<Component> components = new ArrayList<>();
        model.getCircuitComponents().forEach(item -> components.add(new Component("LK", item)));
        model.getControlComponents().forEach(item -> components.add(new Component("CONTROL", item)));
        model.getThermalComponents().forEach(item -> components.add(new Component("THERMAL", item)));
        return components;
    }

    static List<Diagnostic> normalize(CircuitModel model) {
        return normalize(model, components(model));
    }

    private static List<Diagnostic> normalize(CircuitModel model, List<Component> entries) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (var entry : entries) {
            var component = entry.data();
            if (fields(entry).isEmpty()) continue;
            double[] original = component.getNumericParameters();
            int count = Math.max(original.length, isSource(entry) ? 21 : 13);
            for (String key : component.getParameters().keySet()) {
                if (key.matches("param[0-9]+")) count = Math.max(count, Integer.parseInt(key.substring(5)) + 1);
            }
            double[] values = Arrays.copyOf(original, count);
            boolean[] supplied = new boolean[count];
            Arrays.fill(supplied, 0, original.length, true);
            if (entry.domain().equals("CONTROL")) {
                var descriptor = ControlBlockDescriptor.find(component.getType());
                if (descriptor != null) for (var setting : descriptor.settings()) {
                    if (!supplied[setting.slot()]) values[setting.slot()] = setting.defaultValue();
                }
            }
            for (int slot = 0; slot < count; slot++) {
                Object indexed = component.getParameters().get("param" + slot);
                if (indexed instanceof Number number) {
                    if ((!component.isParsedInput() || component.isConfigurationNormalized()) && slot < original.length && original[slot] != number.doubleValue()) {
                        diagnostics.add(configuration("INPUT_REPRESENTATION_CONFLICT", entry, "param" + slot,
                                "The numeric array and indexed value must agree"));
                    }
                    values[slot] = number.doubleValue();
                    supplied[slot] = true;
                }
            }
            double[] importedValues = values.clone();
            if (isSignal(entry) && component.isParsedInput() && !component.isConfigurationNormalized()) {
                double offset = values[3];
                values[3] = model.getFileVersion() == 170 ? Math.toRadians(values[4]) : values[4];
                values[4] = offset;
            }
            if (isSignal(entry)) values[0] = canonicalSignalSubtype(values[0]);
            for (var field : fields(entry)) {
                double value = values[field.slot()];
                if (component.isParsedInput() && !component.isConfigurationNormalized()) {
                    List<String> names = new ArrayList<>();
                    names.add(field.name());
                    names.addAll(field.aliases());
                    String raw = null;
                    String rawName = null;
                    for (String name : names) {
                        if (component.getImportTokens().containsKey(name)) {
                            raw = component.getImportTokens().get(name);
                            rawName = name;
                            break;
                        }
                    }
                    int savedSlot = isSignal(entry) && field.slot() == 3 ? 4
                            : isSignal(entry) && field.slot() == 4 ? 3 : field.slot();
                    String binding = legacyBinding(component, savedSlot);
                    if (raw != null) {
                        try { value = fromNamed(entry, field, field.rule() == Rule.BOOLEAN && (raw.equals("true") || raw.equals("false"))
                                ? Boolean.parseBoolean(raw) ? 1 : 0 : Double.parseDouble(raw)); }
                        catch (NumberFormatException error) {
                            diagnostics.add(configuration("INVALID_PARAMETER", entry, field.name(), "The saved value must be numeric"));
                        }
                        String bindingToken = rawName + "$ParameterName";
                        if (component.getImportTokens().containsKey(bindingToken)) {
                            binding = component.getImportTokens().get(bindingToken).trim();
                        }
                    } else if (isSource(entry) && field.slot() == 20) {
                        value = importedValues[1];
                        String oldBinding = legacyBinding(component, 1);
                        if (!oldBinding.isEmpty()) binding = oldBinding;
                    } else if (isSource(entry) && field.slot() == 4) value = Math.toDegrees(value);
                    if (!binding.isEmpty()) {
                        Double boundValue = model.getOptimizerParameters().get(binding);
                        if (boundValue == null) {
                            diagnostics.add(configuration("INVALID_PARAMETER", entry, field.name(), "The optimizer binding does not exist"));
                        } else value = fromNamed(entry, field, boundValue);
                    }
                } else {
                    List<String> names = new ArrayList<>(field.aliases());
                    names.add(0, field.name());
                    boolean named = false;
                    for (String name : names) {
                        Object candidate = component.getParameters().get(name);
                        if (candidate instanceof Number || candidate instanceof Boolean) {
                            double candidateValue = fromNamed(entry, field, candidate instanceof Number number
                                    ? number.doubleValue() : Boolean.TRUE.equals(candidate) ? 1 : 0);
                            if ((supplied[field.slot()] || named) && !(isSignal(entry) && field.slot() == 3 ? sameValue(value, candidateValue) : value == candidateValue)) {
                                diagnostics.add(configuration("INPUT_REPRESENTATION_CONFLICT", entry, name,
                                        "The named and indexed configuration values must agree"));
                            }
                            value = candidateValue;
                            named = true;
                        }
                    }
                }
                values[field.slot()] = value;
            }
            if (isSource(entry)) Arrays.fill(values, 6, 10, 0);
            if (entry.domain().equals("LK") && (component.getType() == 7 || component.getType() == 28
                    || component.getType() == 6 || component.getType() == 8 || component.getType() == 10)) {
                values[0] = values[component.getType() == 7 ? 2 : 3];
                values[4] = 0;
                values[5] = 0;
                values[8] = 0;
                if (component.getType() == 8) values[11] = 0;
            }
            if (entry.domain().equals("LK") && (component.getType() == 2 || component.getType() == 12)) {
                Arrays.fill(values, 2, 5, 0);
            }
            if (entry.domain().equals("LK") && (component.getType() == 3 || component.getType() == 47)) {
                Arrays.fill(values, 2, 11, 0);
                values[6] = values[0];
                values[7] = values[0];
            }
            component.setNumericParameters(values);
            for (int slot = 0; slot < values.length; slot++) component.setParameter("param" + slot, values[slot]);
            for (var field : fields(entry)) write(component, field, values[field.slot()]);
            component.setConfigurationNormalized(true);
        }
        return diagnostics;
    }

    static void derive(CircuitModel model) {
        for (var component : model.getCircuitComponents()) {
            if (component.getType() == 7 || component.getType() == 28
                    || component.getType() == 6 || component.getType() == 8 || component.getType() == 10) {
                double off = component.getNumericParameters()[component.getType() == 7 ? 2 : 3];
                component.getNumericParameters()[0] = off;
                component.setParameter("param0", off);
            }
            if (component.getType() == 3 || component.getType() == 47) {
                double capacitance = ((Number) component.getParameters().get("param0")).doubleValue();
                component.setParameter("param6", capacitance);
                component.setParameter("param7", capacitance);
                double[] values = component.getNumericParameters();
                values[6] = capacitance;
                values[7] = capacitance;
            }
        }
    }

    static boolean isSource(Component component) {
        return component.domain().equals("LK") && List.of(4, 5, 26, 44, 45).contains(component.data().getType());
    }

    static List<Diagnostic> validate(CircuitModel model) {
        return validate(model, components(model));
    }

    private static List<Diagnostic> validate(CircuitModel model, List<Component> entries) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (var component : entries) {
            for (var field : fields(component)) {
                Object value = component.data().getParameters().get("param" + field.slot());
                if (!(value instanceof Number number) || !field.rule().accepts(number.doubleValue())) {
                    diagnostics.add(configuration("INVALID_PARAMETER", component, field.name(), "The configured value is outside its permitted range"));
                }
            }
            if (component.domain().equals("CONTROL")) {
                var descriptor = ControlBlockDescriptor.find(component.data().getType());
                if (descriptor != null) for (String error : descriptor.validateConfiguration(component.data(), model)) {
                    diagnostics.add(configuration("INVALID_PARAMETER", component, "configuration", error));
                }
            }
            if (isSource(component)) {
                double mode = component.data().getNumericParameters()[0];
                // Signal control reaches LK voltage and current sources only (ControlCalculatorFactory source mappings).
                boolean signal = (mode == 2 || mode == 400) && List.of(4, 5).contains(component.data().getType());
                if (mode != 0 && mode != 1 && mode != 401 && mode != 402 && !signal) {
                    diagnostics.add(new Diagnostic("UNSUPPORTED_SOURCE_MODE", Diagnostic.Category.CAPABILITY, Diagnostic.Severity.ERROR,
                            "The headless engine requires a DC, sinusoidal or signal-controlled source", component.domain(),
                            component.data().getName(), component.data().getName() + ".sourceType", component.data().getSourceLocation()));
                }
            }
        }
        return diagnostics;
    }

    static List<Diagnostic> apply(CircuitModel model, Map<String, Double> parameters) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        Map<Binding, Double> assignments = new LinkedHashMap<>();
        for (var entry : parameters.entrySet()) {
            String path = entry.getKey();
            List<Binding> matches = new ArrayList<>();
            if (path != null) {
                for (var component : components(model)) {
                    String prefix = component.data().getName() + ".";
                    if (path.startsWith(prefix)) {
                        String key = path.substring(prefix.length());
                        for (var field : fields(component)) {
                            if (field.matches(key)) matches.add(new Binding(component, field));
                        }
                    }
                }
            }
            boolean duplicateName = matches.size() == 1 && components(model).stream()
                    .filter(item -> Objects.equals(item.data().getName(), matches.get(0).component().data().getName())).count() > 1;
            if (matches.size() != 1 || duplicateName) {
                diagnostics.add(override(matches.isEmpty() ? readOnlyCode(model, path) : "AMBIGUOUS_PARAMETER", path, null,
                        "The parameter path must identify one configured field"));
                continue;
            }
            var binding = matches.get(0);
            Double value = entry.getValue();
            if (value == null || !binding.field().rule().accepts(value)) {
                diagnostics.add(override("INVALID_PARAMETER", path, binding.component(), "The parameter value is outside its permitted range"));
                continue;
            }
            if (!path.endsWith(".param" + binding.field().slot())) value = fromNamed(binding.component(), binding.field(), value);
            Double previous = assignments.putIfAbsent(binding, value);
            if (previous != null && !(isSignal(binding.component()) && binding.field().slot() == 3
                    ? sameValue(previous, value) : previous.doubleValue() == value.doubleValue())) {
                diagnostics.add(override("OVERRIDE_CONFLICT", path, binding.component(), "Assignments to the same field must agree"));
            }
        }
        if (diagnostics.isEmpty()) assignments.forEach((binding, value) -> {
            write(binding.component().data(), binding.field(), value);
            String prefix = binding.component().data().getName() + ".";
            model.getImportIssues().removeIf(issue -> isNormalizationIssue(issue)
                    && binding.component().domain().equals(issue.domain())
                    && Objects.equals(binding.component().data().getName(), issue.component())
                    && issue.parameterPath() != null && issue.parameterPath().startsWith(prefix)
                    && binding.field().matches(issue.parameterPath().substring(prefix.length())));
        });
        return diagnostics;
    }

    static boolean isNormalizationIssue(ImportIssue issue) {
        return issue.code().equals("INVALID_PARAMETER") || issue.code().equals("INPUT_REPRESENTATION_CONFLICT");
    }
    static void write(CircuitModel.ComponentData component, Field field, double value) {
        component.setParameter("param" + field.slot(), value);
        double namedValue = component.getType() == 4 && field.name().equals("phaseDegrees") && field.slot() == 3
                ? Math.toDegrees(value) : value;
        component.setParameter(field.name(), namedValue);
        for (String alias : field.aliases()) component.setParameter(alias, namedValue);
        double[] values = Arrays.copyOf(component.getNumericParameters(), Math.max(component.getNumericParameters().length, field.slot() + 1));
        values[field.slot()] = value;
        component.setNumericParameters(values);
    }

    private static String legacyBinding(CircuitModel.ComponentData component, int slot) {
        String raw = component.getImportTokens().getOrDefault("nameOpt[]", "");
        if (raw.equals("null")) return "";
        if (raw.startsWith("/")) raw = raw.substring(1);
        String[] names = raw.contains("/") ? raw.split("/", -1) : raw.split("\\s+", -1);
        String name = slot < names.length ? names[slot].trim() : "";
        return name.equals("NIX_NIX_NIX") ? "" : name;
    }

    static boolean isSignal(Component component) {
        return component.domain().equals("CONTROL") && component.data().getType() == 4;
    }
    private static double fromNamed(Component component, Field field, double value) {
        if (isSignal(component) && field.slot() == 0) return canonicalSignalSubtype(value);
        return isSignal(component) && field.slot() == 3 ? Math.toRadians(value) : value;
    }
    /**
     * Maps accepted CONTROL waveform identifiers to their calculator identifiers.
     *
     * @param value the saved or programmatic waveform identifier
     * @return the canonical identifier, or the original value when it is unknown
     */
    public static double canonicalSignalSubtype(double value) {
        if (value != Math.rint(value)) return value;
        return switch ((int) value) {
            case 0, 402 -> 400; case 1, 403 -> 401; case 2 -> 404; case 3 -> 405; case 4 -> 406; default -> value;
        };
    }
    private static boolean sameValue(double first, double second) {
        return Double.compare(first, second) == 0 || Math.abs(first - second) <= 1e-12 * Math.max(1, Math.max(Math.abs(first), Math.abs(second)));
    }

    private static String readOnlyCode(CircuitModel model, String path) {
        if (path != null) {
            for (var component : components(model)) {
                String prefix = component.data().getName() + ".";
                if (path.startsWith(prefix) && unsupportedFields(component).stream()
                        .anyMatch(field -> field.matches(path.substring(prefix.length())))) return "UNSUPPORTED_PARAMETER";
                if (path.startsWith(prefix) && path.substring(prefix.length())
                        .matches("param\\d+|uniqueObjectIdentifier|coupledReferenceID\\d+")) return "READ_ONLY_PARAMETER";
            }
        }
        return "UNKNOWN_PARAMETER";
    }

    private static Diagnostic override(String code, String path, Component component, String message) {
        return new Diagnostic(code, Diagnostic.Category.OVERRIDES, Diagnostic.Severity.ERROR, message,
                component == null ? null : component.domain(), component == null ? null : component.data().getName(), path,
                component == null ? null : component.data().getSourceLocation());
    }
    private static Diagnostic configuration(String code, Component component, String field, String message) {
        return new Diagnostic(code, Diagnostic.Category.CONFIGURATION, Diagnostic.Severity.ERROR, message,
                component.domain(), component.data().getName(), component.data().getName() + "." + field, component.data().getSourceLocation());
    }
}
