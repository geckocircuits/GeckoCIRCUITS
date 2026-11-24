package gecko.core.simulation;

import gecko.core.control.StatelessCalculation;
import gecko.core.control.calculators.*;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.CircuitConfiguration;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.BiFunction;

/**
 * Describes saved settings, port counts, and calculator assembly for a CONTROL type.
 *
 * @param type the saved CONTROL type identifier
 * @param inputPorts the default input count, or -1 for a configured count
 * @param outputPorts the output count, or -1 for a configured count
 * @param settings immutable numeric setting descriptions
 * @param calculator the calculator assembly function
 */
public record ControlBlockDescriptor(int type, int inputPorts, int outputPorts, List<Setting> settings,
        BiFunction<CircuitModel.ComponentData, CircuitModel, AbstractControlCalculatable> calculator) {
    public ControlBlockDescriptor { settings = List.copyOf(settings); }
    public enum ValueRule { FINITE, NON_NEGATIVE, POSITIVE, BOOLEAN, UNIT_INTERVAL, SIGNAL_MODE }
    public record Setting(String name, int slot, double defaultValue, ValueRule rule, List<String> aliases) {
        public Setting { aliases = List.copyOf(aliases); }
        public Setting(String name, int slot, double defaultValue, ValueRule rule, String... aliases) {
            this(name, slot, defaultValue, rule, List.of(aliases));
        }
        public Setting(String name, int slot, double defaultValue, String... aliases) {
            this(name, slot, defaultValue, ValueRule.FINITE, List.of(aliases));
        }
    }

    /**
     * Gets the input count for the selected mode and saved terminal layout.
     *
     * @param data the component settings and terminal labels
     * @return the effective input count
     */
    public int inputs(CircuitModel.ComponentData data) {
        if (type == 22) return externalWidth(data, data.getTerminalXLabels().length);
        if (type == 23) return 0;
        if (type == 4) return signalMode(data).inputCount();
        if (type == 11 && value(data, 1, 0) != 0) return 2;
        if (type == 27 && value(data, 2, 0) != 0) return 3;
        return inputPorts < 0 ? data.getTerminalXLabels().length : inputPorts;
    }

    /**
     * Gets the output count for the saved terminal layout.
     *
     * @param data the component settings and terminal labels
     * @return the effective output count
     */
    public int outputs(CircuitModel.ComponentData data) {
        if (type == 22) return 0;
        if (type == 23) return externalWidth(data, data.getTerminalYLabels().length);
        return outputPorts < 0 ? data.getTerminalYLabels().length : outputPorts;
    }

    /** @return the minimum configured input count supported by the calculation. */
    public int minimumInputs() { return type == 13 || type == 49 || type == 50 ? 1 : 0; }

    /**
     * Checks mode-specific configuration and assembles one calculator.
     *
     * @param data the component settings and terminal labels
     * @param model the model with captured resources, or null when resources are unnecessary
     * @return a calculator with independent runtime state
     * @throws IllegalArgumentException if mode-specific configuration or required resources are invalid
     */
    public AbstractControlCalculatable createCalculator(CircuitModel.ComponentData data, CircuitModel model) {
        var errors = validateConfiguration(data, model);
        if (!errors.isEmpty()) throw new IllegalArgumentException(errors.get(0));
        return calculator.apply(data, model);
    }

    private static final Map<Integer, ControlBlockDescriptor> DESCRIPTORS = descriptors();

    private static Map<Integer, ControlBlockDescriptor> descriptors() {
        Map<Integer, ControlBlockDescriptor> result = new LinkedHashMap<>();
        simple(result, 1, () -> new NothingToDoCalculator(0, 1));
        simple(result, 2, () -> new NothingToDoCalculator(0, 1));
        add(result, 3, 0, 1, data -> new ConstantCalculator(value(data, 0, 1)),
                new Setting("value", 0, 1, "constant", "constantValue"));
        contextual(result, 4, 0, 1, ControlBlockDescriptor::signal,
                new Setting("subtype", 0, 404, ValueRule.SIGNAL_MODE, "typQuelle"),
                new Setting("amplitude", 1, 10, ValueRule.NON_NEGATIVE, "amplitudeAC"),
                new Setting("frequency", 2, 50, ValueRule.NON_NEGATIVE, "frequenz"),
                new Setting("phaseDegrees", 3, 0, "phase"), new Setting("offset", 4, 0, "anteilDC"),
                new Setting("duty", 5, 0.5, ValueRule.UNIT_INTERVAL, "dutyRatio", "tastverhaeltnis"),
                new Setting("useExternal", 6, 0, ValueRule.BOOLEAN));
        add(result, 5, -1, 0, data -> new NothingToDoCalculator(data.getTerminalXLabels().length, 0));
        simple(result, 6, GateCalculator::new);
        add(result, 7, 1, 1, data -> new GainCalculator(value(data, 0, 1)), new Setting("gain", 0, 1, "k"));
        add(result, 8, 1, 1, data -> new PT1Calculator(value(data, 0, 1), value(data, 1, 1)),
                new Setting("T", 0, 1, ValueRule.NON_NEGATIVE), new Setting("a1", 1, 1));
        add(result, 9, 1, 1, data -> new PT2Calculator(value(data, 0, 1), value(data, 1, 1)),
                new Setting("T", 0, 1, ValueRule.POSITIVE), new Setting("a1", 1, 1));
        add(result, 10, 1, 1, data -> new PICalculator(value(data, 0, -1), value(data, 1, -1)),
                new Setting("r0", 0, -1), new Setting("a1", 1, -1));
        add(result, 11, 1, 1, data -> value(data, 1, 0) != 0 ? new HysteresisCalculatorExternal()
                : new HysteresisCalculatorInternal(value(data, 0, -1)),
                new Setting("h", 0, -1), new Setting("useExternal", 1, 0, ValueRule.BOOLEAN));
        add(result, 12, -1, 1, data -> arithmetic(data.getTerminalXLabels().length, false));
        add(result, 13, -1, 1, data -> data.getTerminalXLabels().length == 2 ? new SubtractionTwoParameter()
                : new SubtractionMoreParameter(data.getTerminalXLabels().length));
        add(result, 14, -1, 1, data -> arithmetic(data.getTerminalXLabels().length, true));
        simple(result, 15, DivCalculator::new);
        simple(result, 18, NotCalculator::new);
        add(result, 19, -1, 1, data -> data.getTerminalXLabels().length == 2 ? new AndTwoPortCalculator()
                : new AndMultiInputCalculator(data.getTerminalXLabels().length));
        add(result, 20, -1, 1, data -> data.getTerminalXLabels().length == 2 ? new OrCalculatorTwoInputs()
                : new OrCalculatorMultipleInputs(data.getTerminalXLabels().length));
        simple(result, 21, XORCalculator::new);
        add(result, 22, -1, 0, data -> new ToExternalCalculator(externalWidth(data, data.getTerminalXLabels().length)));
        add(result, 23, 0, -1, data -> new FromExternalCalculator(externalWidth(data, data.getTerminalYLabels().length)));
        add(result, 25, 1, 1, data -> new DelayCalculator(value(data, 0, 10e-6)),
                new Setting("tDelay", 0, 10e-6, ValueRule.NON_NEGATIVE));
        simple(result, 26, SampleHoldCalculator::new);
        add(result, 27, 1, 1, data -> value(data, 2, 0) != 0 ? new LimitCalculatorExternal()
                : new LimitCalculatorInternal(value(data, 0, -1), value(data, 1, 1)),
                new Setting("minLimit", 0, -1, "min"), new Setting("maxLimit", 1, 1, "max"),
                new Setting("useExternal", 2, 0, ValueRule.BOOLEAN));
        add(result, 29, 1, 1, data -> new PDCalculator(value(data, 0, 1)), new Setting("a1", 0, 1, "gain"));
        simple(result, 32, AbsCalculator::new);
        simple(result, 33, RoundCalculator::new);
        simple(result, 34, SinCalculator::new);
        simple(result, 35, ASinCalculator::new);
        simple(result, 36, CosCalculator::new);
        simple(result, 37, ACosCalculator::new);
        simple(result, 38, TanCalculator::new);
        simple(result, 39, ATanCalculator::new);
        simple(result, 40, ExpCalculator::new);
        simple(result, 41, LnCalculator::new);
        simple(result, 42, SquareCalculator::new);
        simple(result, 43, SqrtCalculator::new);
        simple(result, 44, PowerCalculator::new);
        simple(result, 45, GreaterEqualCalculator::new);
        simple(result, 46, GreaterThanCalculator::new);
        add(result, 47, -1, 1, data -> data.getTerminalXLabels().length == 2 ? new EqualCalculatorTwoInputs()
                : new EqualCalculatorMultiInput(data.getTerminalXLabels().length));
        simple(result, 48, NotEqualCalculator::new);
        add(result, 49, -1, 1, data -> data.getTerminalXLabels().length == 2 ? new MinCalculatorTwoInputs()
                : new MinCalculatorMultiInputs(data.getTerminalXLabels().length));
        add(result, 50, -1, 1, data -> data.getTerminalXLabels().length == 2 ? new MaxCalculatorTwoInputs()
                : new MaxCalculatorMultiInputs(data.getTerminalXLabels().length));
        simple(result, 51, SignumCalculator::new);
        simple(result, 53, CounterCalculatable::new);
        simple(result, 58, TimeCalculator::new);
        simple(result, 59, SparseMatrixCalculator::new);
        add(result, 64, 2, 1, data -> new IntegratorCalculation(value(data, 0, 1), value(data, 1, 0),
                value(data, 2, -1), value(data, 3, 1)), new Setting("a1", 0, 1), new Setting("y0", 1, 0),
                new Setting("min", 2, -1), new Setting("max", 3, 1));
        simple(result, 65, ABCDQCalculator::new);
        simple(result, 66, DQABCDCalculator::new);
        simple(result, 76, PmsmControlCalculator::new);
        simple(result, 77, PmsmModulatorCalculator::new);
        add(result, 84, -1, 1, data -> new MUXControlCalculatable(data.getTerminalXLabels().length));
        add(result, 85, 1, -1, data -> new DEMUXCalculator(data.getTerminalYLabels().length));
        return Map.copyOf(result);
    }

    private static void simple(Map<Integer, ControlBlockDescriptor> registry, int type,
            Supplier<AbstractControlCalculatable> calculator) {
        var prototype = calculator.get();
        add(registry, type, prototype._inputSignal.length, prototype._outputSignal.length, data -> calculator.get());
    }

    private static void add(Map<Integer, ControlBlockDescriptor> registry, int type, int inputPorts, int outputPorts,
            Function<CircuitModel.ComponentData, AbstractControlCalculatable> calculator, Setting... settings) {
        contextual(registry, type, inputPorts, outputPorts, (data, model) -> calculator.apply(data), settings);
    }

    private static void contextual(Map<Integer, ControlBlockDescriptor> registry, int type, int inputPorts, int outputPorts,
            BiFunction<CircuitModel.ComponentData, CircuitModel, AbstractControlCalculatable> calculator, Setting... settings) {
        registry.put(type, new ControlBlockDescriptor(type, inputPorts, outputPorts, List.of(settings), calculator));
    }

    /**
     * Checks port limits, source modes, required resources, and related numeric settings.
     *
     * @param data the component settings and terminal labels
     * @param model the model with captured resources, or null when resources are unnecessary
     * @return immutable error messages, or an empty list when these checks pass
     */
    public List<String> validateConfiguration(CircuitModel.ComponentData data, CircuitModel model) {
        try {
            if (inputs(data) < minimumInputs()) return List.of("The CONTROL block requires at least one input port.");
            if (type == 25 && model != null && Double.isFinite(model.getTimeStep()) && model.getTimeStep() > 0) {
                gecko.core.control.DelayHistory.validateHistorySize(value(data, 0, 10e-6), model.getTimeStep());
            }
            if (type == 4) {
                var mode = signalMode(data);
                mode.requireCompatible();
                if (mode.waveform() == SignalWaveform.IMPORT) new SignalCalculatorImport(importTable(data, model));
            }
            if (type == 27 && value(data, 2, 0) == 0 && value(data, 0, -1) > value(data, 1, 1)
                    || type == 64 && value(data, 2, -1) > value(data, 3, 1)) {
                return List.of("The lower limit must not exceed the upper limit.");
            }
            return List.of();
        } catch (IllegalArgumentException error) {
            return List.of(error.getMessage());
        }
    }

    private enum SignalWaveform {
        SINE, TRIANGLE, RECTANGLE, RANDOM, IMPORT, UNKNOWN;
        boolean periodic() { return this == SINE || this == TRIANGLE || this == RECTANGLE; }
    }

    private record SignalMode(SignalWaveform waveform, boolean external) {
        int inputCount() { return external && waveform.periodic() ? 5 : 0; }
        void requireCompatible() {
            if (external && (waveform == SignalWaveform.RANDOM || waveform == SignalWaveform.IMPORT)) {
                throw new IllegalArgumentException("Random and imported sources cannot use external modulation inputs.");
            }
        }
    }

    private static SignalMode signalMode(CircuitModel.ComponentData data) {
        var waveform = switch ((int) CircuitConfiguration.canonicalSignalSubtype(value(data, 0, 404))) {
            case 400 -> SignalWaveform.SINE;
            case 401 -> SignalWaveform.TRIANGLE;
            case 404 -> SignalWaveform.RECTANGLE;
            case 405 -> SignalWaveform.RANDOM;
            case 406 -> SignalWaveform.IMPORT;
            default -> SignalWaveform.UNKNOWN;
        };
        return new SignalMode(waveform, value(data, 6, 0) != 0);
    }

    private static AbstractControlCalculatable signal(CircuitModel.ComponentData data, CircuitModel model) {
        if (data.getNumericParameters().length < 6) {
            return new SignalCalculatorRectangle(0, 1, 1000, 0, 0, 0.5);
        }
        var mode = signalMode(data);
        mode.requireCompatible();
        if (mode.waveform() == SignalWaveform.RANDOM) return new SignalCalculatorRandom();
        if (mode.waveform() == SignalWaveform.IMPORT) return new SignalCalculatorImport(importTable(data, model));
        double amplitude = value(data, 1, 10);
        double frequency = value(data, 2, 50);
        double phase = value(data, 3, 0);
        double offset = value(data, 4, 0);
        double duty = value(data, 5, 0.5);
        AbstractSignalCalculatorPeriodic calculator = switch (mode.waveform()) {
            case SINE -> new SignalCalculatorSinus(0, amplitude, frequency, phase, offset, duty);
            case TRIANGLE -> new SignalCalculatorTriangle(0, amplitude, frequency, phase, offset, duty);
            case RECTANGLE -> new SignalCalculatorRectangle(0, amplitude, frequency, phase, offset, duty);
            default -> throw new IllegalArgumentException("Unknown signal subtype: " + value(data, 0, 404));
        };
        return mode.external() ? new SignalCalculatorExternalWrapper(calculator) : calculator;
    }

    private static double[][] importTable(CircuitModel.ComponentData data, CircuitModel model) {
        String token = savedToken(data, "externalDataFileHashValue");
        if (model == null || token.isBlank()) throw new IllegalArgumentException("A signal import requires a captured resource identifier.");
        var resource = model.getResources().get(Long.parseLong(token.trim()));
        if (resource == null) throw new IllegalArgumentException("The signal import resource is missing: " + token);
        var lines = new String(resource.bytes(), java.nio.charset.StandardCharsets.UTF_8).lines()
                .filter(line -> !line.isBlank()).toList();
        double[][] table = new double[2][lines.size()];
        double start = 0;
        for (int index = 0; index < lines.size(); index++) {
            var fields = lines.get(index).trim().split("\\s+");
            if (fields.length < 2) throw new IllegalArgumentException("Each signal import row requires a time and a value.");
            double time = Double.parseDouble(fields[0]);
            if (index == 0) start = time;
            table[0][index] = time - start;
            table[1][index] = Double.parseDouble(fields[1]);
        }
        return table;
    }

    static String savedToken(CircuitModel.ComponentData data, String name) {
        if (data.getImportTokens().containsKey(name)) return data.getImportTokens().get(name);
        return data.getSavedBlock() == null ? "" : data.getSavedBlock().tokens().getOrDefault(name, "");
    }

    private static AbstractControlCalculatable arithmetic(int inputs, boolean multiply) {
        return new ArithmeticCalculator(inputs, multiply);
    }

    private static final class PowerCalculator extends AbstractControlCalculatable implements StatelessCalculation {
        PowerCalculator() { super(2, 1); }
        @Override public void berechneYOUT(double dt) {
            _outputSignal[0][0] = Math.pow(_inputSignal[0][0], _inputSignal[1][0]);
        }
    }

    private static final class ArithmeticCalculator extends AbstractControlCalculatable implements StatelessCalculation {
        private final boolean multiply;
        ArithmeticCalculator(int inputs, boolean multiply) { super(inputs, 1); this.multiply = multiply; }
        @Override public void berechneYOUT(double dt) {
            double result = multiply ? 1 : 0;
            for (double[] input : _inputSignal) result = multiply ? result * input[0] : result + input[0];
            _outputSignal[0][0] = result;
        }
    }

    /**
     * Gets the assembly description for a supported CONTROL type.
     *
     * @param type the saved CONTROL type identifier
     * @return the description, or null if no assembly exists
     */
    public static ControlBlockDescriptor find(int type) { return DESCRIPTORS.get(type); }

    /** @return every registered description, so a test can classify each calculator for the steady-state search */
    static java.util.Collection<ControlBlockDescriptor> registered() { return DESCRIPTORS.values(); }

    private static int externalWidth(CircuitModel.ComponentData data, int fallback) {
        Object saved = data.getParameters().get("externalWidth");
        return saved instanceof Number number ? number.intValue() : fallback;
    }

    static double value(CircuitModel.ComponentData data, int slot, double fallback) {
        double[] values = data.getNumericParameters();
        return slot < values.length ? values[slot] : fallback;
    }
}
