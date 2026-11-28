package gecko.core.simulation;

import gecko.core.ac.AcComponent;
import gecko.core.ac.ParameterDefinition;
import gecko.core.allg.SolverType;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.circuit.waveform.PiecewiseLinearWaveform;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitResource;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static gecko.core.simulation.ControlReviewRegressionTest.component;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Behavioural sources solved in the electrical step (ADR-0033), on two archive schematics built by hand:
 * {@code Tof/Test files/Transformer coupling.asc} (id 438) and {@code Tof/Test files/test comparator.asc} (id 451).
 * Each keeps its authored values; only the load of 438 changes in the derived in-loop variants.
 */
class BehaviouralSourceTest {
    private static final double STEP = 100e-9;

    /**
     * Id 438: the XFMR subcircuit (B1 {@code I=I(V1)*ratio}, B2 {@code V=V(1,2)*ratio}) beside the authored
     * coupled-inductor twin L1, L2, k12. Both secondaries carry {@code load}; the authored value is 1k.
     */
    static CircuitModel transformer(double dt, double load, boolean behavioural) {
        var model = model(dt, 100e-6);
        model.setRootParameterDefinitions(List.of(new ParameterDefinition("k", "0.995"),
                new ParameterDefinition("l1", "606u"), new ParameterDefinition("l2", "37.5u"),
                new ParameterDefinition("ratio", "(1/k)*sqrt(l2/l1)")));
        double k = 0.995, l1 = 606e-6, l2 = 37.5e-6;
        var sine = new double[21];
        sine[0] = 402;  // V2 in 0 SINE(0 1 100k)
        sine[20] = 1;
        sine[2] = 100e3;
        model.addCircuitComponent(component(4, "V2", sine, new String[]{"in"}, "0"));
        model.addCircuitComponent(dc("IV1", 0, "in", "n001"));
        model.addCircuitComponent(resistor("R2", 10e-3, "n002", "n001"));
        model.addCircuitComponent(component(2, "Lf", new double[]{l1 * (1 - k * k), 0}, new String[]{"n002"}, "n003"));
        model.addCircuitComponent(component(2, "Lm", new double[]{l1 * k * k, 0}, new String[]{"n003"}, "0"));
        model.addCircuitComponent(resistor("R1", load, "out1", "0"));
        // X1 = XFMR(1=n003, 2=0, 3=out1, 4=0), flattened with the subcircuit nodes x1n001 and x1n002.
        var b1 = component(5, "X1_B1", dc(0), new String[]{"n003"}, "0");
        var b2 = component(4, "X1_B2", dc(0), new String[]{"x1n001"}, "0");
        if (behavioural) {
            law(b1, 1, "I(X1_V1)*{ratio}", true);
            law(b2, 2, "V(n003,0)*{ratio}", false);
        }
        model.addCircuitComponent(b1);
        model.addCircuitComponent(b2);
        model.addCircuitComponent(dc("X1_V1", 0, "x1n001", "x1n002"));
        model.addCircuitComponent(resistor("X1_R1", 1e-6, "out1", "x1n002"));
        model.addCircuitComponent(resistor("X1_R2", 1e6, "n003", "0"));
        // The authored twin: V§IV2 in N007 0, R4 N008 N007 10m, L1 N008 0, L2 out2 0, k12 L1 L2 {k}, R3 out2 0.
        model.addCircuitComponent(dc("IV2", 0, "in", "n007"));
        model.addCircuitComponent(resistor("R4", 10e-3, "n008", "n007"));
        model.addCircuitComponent(component(12, "L1", new double[]{l1}, new String[]{"n008"}, "0"));
        model.addCircuitComponent(component(12, "L2", new double[]{l2}, new String[]{"out2"}, "0"));
        model.addCircuitComponent(resistor("R3", load, "out2", "0"));
        var coupling = new CircuitModel.ComponentData(9, "k12");
        coupling.setParameter("param0", k);
        coupling.setParameterStrings(new String[]{"L1", "L2"});
        model.addCircuitComponent(coupling);
        model.addControlComponent(volt("P.in", "in", "0", "V(in)"));
        model.addControlComponent(volt("P.n003", "n003", "0", "V(n003)"));
        model.addControlComponent(volt("P.out1", "out1", "0", "V(out1)"));
        model.addControlComponent(amp("P.lf", "Lf", "I(Lf)"));
        model.addControlComponent(volt("P.out2", "out2", "0", "V(out2)"));
        model.addControlComponent(component(5, "scope", new double[0],
                new String[]{"V(in)", "V(n003)", "V(out1)", "I(Lf)", "V(out2)"}));
        return model;
    }

    /** Id 451: COMPTOF_G with hysteresis B1 in the loop through its own output, and comparator B2. */
    static CircuitModel comparator(double dt) {
        var model = model(dt, 200e-6);
        model.setRootParameterDefinitions(List.of(new ParameterDefinition("vhigh", "5"),
                new ParameterDefinition("vlow", "100m"), new ParameterDefinition("vhys", "100m")));
        model.addCircuitComponent(dc("V1", 1, "n001", "0"));
        // V2 in 0 PULSE(0 2 0 99u 99u 2u 200u), one period, as the identical PWL.
        model.addCircuitComponent(pwl(model, "V2", new double[]{0, 99e-6, 101e-6, 200e-6}, new double[]{0, 2, 2, 0}, "in", "0"));
        var b1 = law(component(4, "X1_B1", dc(0), new String[]{"x1hys"}, "in"), 1,
                "IF(V(out) > ({VHIGH}+{VLOW})/2,{VHYS},0)", false);
        var b2 = law(component(4, "X1_B2", dc(0), new String[]{"x1n001"}, "0"), 2, "IF(V(x1hys,n001)>0,{VHIGH},{VLOW})", false);
        model.addCircuitComponent(b1);
        model.addCircuitComponent(b2);
        model.addCircuitComponent(resistor("X1_R1", 10, "x1n001", "out"));
        model.addCircuitComponent(component(3, "X1_C1", new double[]{10e-12, 0}, new String[]{"out"}, "0"));
        model.addCircuitComponent(resistor("X1_R2", 1e6, "n001", "0"));
        model.addControlComponent(volt("P.in", "in", "0", "V(in)"));
        model.addControlComponent(volt("P.o", "out", "0", "V(out)"));
        model.addControlComponent(component(5, "scope", new double[0], new String[]{"V(in)", "V(out)"}));
        return model;
    }

    @Test
    void feedForwardTransformerMatchesTheCoupledInductorTwinAndLtspice() throws IOException {
        var result = run(transformer(STEP, 1e3, true));
        assertTwin(result, 1e-4);
        assertReference(result, "438", 0.01);
    }

    /** Derived loads close the loop through B1: CONTROL coupling diverges for 30, 10 and 1 ohm at every step (#162). */
    @ParameterizedTest
    @ValueSource(doubles = {100, 30, 10, 1})
    void inLoopTransformerStaysStableAndMatchesTheTwin(double load) throws IOException {
        var result = run(transformer(STEP, load, true));
        assertTwin(result, 1e-4);
        if (load == 10 || load == 1) assertReference(result, "438r" + (int) load, 0.01);
    }

    @Test
    void hysteresisComparatorSwitchesWithinOneStepOfItsThresholds() {
        var result = run(comparator(STEP));
        float[] out = result.getSignalData(1);
        double[] time = result.getTimeArray();
        // Exact thresholds: V(in) = 1 V rising at 49.5 us, V(in) + 0.1 V = 1 V falling at 155.45 us.
        assertEquals(0.1, at(time, out, 49.3e-6), 0.05);
        assertEquals(5, at(time, out, 49.7e-6), 0.05);
        assertEquals(5, at(time, out, 155.3e-6), 0.05);
        assertEquals(0.1, at(time, out, 155.6e-6), 0.05);
        for (float value : out) assertTrue(Float.isFinite(value));
    }

    @Test
    void circuitWithoutTheLawKeepsTheUnchangedStep() {
        var model = prepare(transformer(STEP, 1e3, false)).execution().newRunModel();
        assertNull(TransientSemiconductors.from(model, NetlistBuilder.buildFromCircuitModel(model)));
        float[] out1 = run(transformer(STEP, 1e3, false)).getSignalData(2);
        for (float value : out1) assertEquals(0, value, 1e-12);
    }

    @Test
    void rejectsAnUnknownNodeAndACurrentOfANonSource() {
        var model = prepare(transformer(STEP, 1e3, true)).execution().newRunModel();
        var b2 = model.getCircuitComponents().get(7);
        law(b2, 2, "V(nowhere)", false);
        var error = assertThrows(IllegalArgumentException.class,
                () -> TransientSemiconductors.from(model, NetlistBuilder.buildFromCircuitModel(model)));
        assertTrue(error.getMessage().contains("nowhere"), error.getMessage());
        law(b2, 2, "I(R1)", false);
        error = assertThrows(IllegalArgumentException.class,
                () -> TransientSemiconductors.from(model, NetlistBuilder.buildFromCircuitModel(model)));
        assertTrue(error.getMessage().contains("r1"), error.getMessage());
    }

    /** Writes the traces that {@code scripts/m6} scores with the #191 policy; skipped unless BEHAVIOURAL205_OUT is set. */
    @Test
    void writeMeasurementTraces() throws IOException {
        String out = System.getenv("BEHAVIOURAL205_OUT");
        assumeTrue(out != null, "set BEHAVIOURAL205_OUT to write the measurement traces");
        for (double dt : new double[]{100e-9, 50e-9}) {
            String suffix = "-gecko-dt" + (dt == 100e-9 ? "100n" : "50n") + ".csv";
            write(run(comparator(dt)), Path.of(out, "451" + suffix));
            write(run(transformer(dt, 1e3, true)), Path.of(out, "438" + suffix));
            for (int load : new int[]{100, 30, 10, 1}) write(run(transformer(dt, load, true)), Path.of(out, "438r" + load + suffix));
        }
    }

    private static void assertTwin(SimulationResult result, double tolerance) {
        float[] out1 = result.getSignalData(2), out2 = result.getSignalData(4);
        double peak = 0, error = 0;
        for (int i = 0; i < out1.length; i++) {
            assertTrue(Float.isFinite(out1[i]), "finite at sample " + i);
            peak = Math.max(peak, Math.abs(out2[i]));
            error = Math.max(error, Math.abs(out1[i] - out2[i]));
        }
        assertTrue(peak > 0.05, "the secondary carries signal: " + peak);
        assertTrue(error <= tolerance * peak + 1e-6, "V(out1) differs from the coupled twin by " + error + " of " + peak);
    }

    /** Compares V(out1) and I(Lf) on a 1 us grid of the fresh LTspice reference, relative to peak-to-peak. */
    private static void assertReference(SimulationResult result, String id, double fraction) throws IOException {
        List<String> lines;
        try (InputStream stream = BehaviouralSourceTest.class.getResourceAsStream("/behavioural/" + id + "-ltspice.csv")) {
            lines = new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines().skip(1).toList();
        }
        double[] time = result.getTimeArray();
        int[] signals = {2, 3};
        for (int column = 1; column <= 2; column++) {
            double low = Double.POSITIVE_INFINITY, high = Double.NEGATIVE_INFINITY, error = 0;
            for (String line : lines) {
                String[] fields = line.split(",");
                double reference = Double.parseDouble(fields[column]);
                low = Math.min(low, reference);
                high = Math.max(high, reference);
                error = Math.max(error, Math.abs(reference - at(time, result.getSignalData(signals[column - 1]), Double.parseDouble(fields[0]))));
            }
            assertTrue(error <= fraction * (high - low), id + " column " + column + " error " + error + " of " + (high - low));
        }
    }

    /** Linear interpolation of a recorded trace. */
    private static double at(double[] time, float[] values, double t) {
        int i = 1;
        while (i < time.length - 1 && time[i] < t) i++;
        double w = (t - time[i - 1]) / (time[i] - time[i - 1]);
        return values[i - 1] + w * (values[i] - values[i - 1]);
    }

    private static CircuitModel model(double dt, double duration) {
        var model = new CircuitModel();
        model.setSolverType(SolverType.SOLVER_TRZ);
        model.setTimeStep(dt);
        model.setSimulationDuration(duration + dt);  // one extra step so the trace covers the declared stop
        return model;
    }

    private static CircuitModel.ComponentData resistor(String name, double ohms, String a, String b) {
        return component(1, name, new double[]{ohms}, new String[]{a}, b);
    }

    private static double[] dc(double value) {
        var values = new double[21];
        values[0] = 401;
        values[1] = value;
        return values;
    }

    private static CircuitModel.ComponentData dc(String name, double volts, String a, String b) {
        return component(4, name, dc(volts), new String[]{a}, b);
    }

    private static CircuitModel.ComponentData pwl(CircuitModel model, String name, double[] t, double[] v, String a, String b) {
        var values = new double[21];
        values[0] = 403;
        var data = component(4, name, values, new String[]{a}, b);
        long owner = 10000L + model.getCircuitComponents().size(), resource = 300000L + model.getResources().size();
        data.setParameter("uniqueObjectIdentifier", owner);
        data.getImportTokens().put("uniqueObjectIdentifier", Long.toString(owner));
        data.getImportTokens().put(PiecewiseLinearWaveform.RESOURCE_ID_TOKEN, Long.toString(resource));
        model.getResources().put(resource, new CircuitResource(resource, "pwl.bin", "pwl.bin",
                new PiecewiseLinearWaveform(t, v).toBytes(), List.of(owner)));
        return data;
    }

    private static CircuitModel.ComponentData volt(String name, String a, String b, String output) {
        var probe = component(1, name, new double[0], new String[0], output);
        probe.setParameterStrings(new String[]{a, b});
        return probe;
    }

    private static CircuitModel.ComponentData amp(String name, String element, String output) {
        var probe = component(2, name, new double[0], new String[0], output);
        probe.setParameterStrings(new String[]{element});
        return probe;
    }

    /**
     * Selects the law. A component with a law has geometric pins at its position, so each source sits apart,
     * as an imported one does.
     */
    private static CircuitModel.ComponentData law(CircuitModel.ComponentData source, int slot, String expression,
            boolean current) {
        source.getPosition()[0] = 100 * slot;
        source.setAcComponent(new AcComponent(source.getName(), List.of(), false, new AcComponent.Behavioural(expression, current)));
        return source;
    }

    private static PreparationResult.Ready prepare(CircuitModel model) {
        var preparation = new CircuitPreparation();
        var prepared = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        return assertInstanceOf(PreparationResult.Ready.class, prepared, prepared.toString());
    }

    private static SimulationResult run(CircuitModel model) {
        var ready = prepare(model);
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return result;
    }

    private static void write(SimulationResult result, Path path) throws IOException {
        String[] names = result.getSignalNames();
        double[] time = result.getTimeArray();
        var text = new StringBuilder("time");
        for (String name : names) text.append(',').append(name);
        text.append('\n');
        for (int i = 0; i < time.length; i++) {
            text.append(time[i]);
            for (int s = 0; s < names.length; s++) text.append(',').append(result.getSignalData(s)[i]);
            text.append('\n');
        }
        Files.createDirectories(path.getParent());
        Files.writeString(path, text);
    }
}
