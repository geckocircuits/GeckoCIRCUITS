package gecko.core.ac;

import gecko.core.io.AcMetadataCodec;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitTopology;
import gecko.core.io.ImportIssue;
import gecko.core.io.importer.LtspiceImporter;
import gecko.core.io.importer.SpiceImporter;
import gecko.core.io.importer.SpiceValue;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.preparation.SolverOverrides;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class NativeAcTest {
    private static final String PHASOR = """
            .ac dec 3 10 100
            .param LoadOhms={2*Base}
            .param Base=500
            Vexc input 0 AC 2 37
            Voffset sense 0 DC 5 AC .5 -20
            Edrive output 0 sense input 2
            Rload output 0 {LoadOhms}
            Cload output 0 2u
            """;

    @Test
    void solvesSignedPhasorsAndCurrentsAfterRoundTrip() throws Exception {
        var model = roundTrip(spice(PHASOR));
        model.setTimeStep(Double.NaN);
        model.setSimulationDuration(-1);
        AcResult result = solve(model);
        assertEquals(4, result.frequencyHz().size());
        assertEquals(6, result.complexUnknowns());
        for (int sample = 0; sample < result.frequencyHz().size(); sample++) {
            double frequency = result.frequencyHz().get(sample);
            Complex vin = polar(2, 37);
            Complex vSense = polar(.5, -20);
            Complex out = vSense.subtract(vin).scale(2);
            Complex resistor = out.scale(1e-3);
            Complex capacitor = new Complex(0, 2 * Math.PI * frequency * 2e-6).multiply(out);
            close(vin, voltage(result, "Vexc", 1, sample));
            close(vSense, voltage(result, "Voffset", 1, sample));
            close(out, voltage(result, "Edrive", 1, sample));
            close(resistor, current(result, "Rload", sample));
            close(capacitor, current(result, "Cload", sample));
            close(resistor.add(capacitor).scale(-1), current(result, "Edrive", sample));
            close(Complex.ZERO, current(result, "Vexc", sample));
            close(Complex.ZERO, current(result, "Voffset", sample));
            assertTrue(result.residualEta().get(sample) <= 1e-12);
        }
        var reversed = spice(PHASOR.replace("sense input 2", "input sense 2"));
        close(voltage(result, "Edrive", 1, 0).scale(-1), voltage(solve(reversed), "Edrive", 1, 0));
    }

    @Test
    void preservesLiveLawsAndParameterSpelling() throws Exception {
        var model = roundTrip(spice(PHASOR));
        assertEquals(List.of(new ParameterDefinition("LoadOhms", "{2*Base}"), new ParameterDefinition("Base", "500")),
                model.getAcSettings().parameters());
        Complex original = current(solve(model), "Rload", 0);
        var settings = model.getAcSettings();
        model.setAcSettings(new AcDocumentSettings(settings.sweep(),
                List.of(settings.parameters().getFirst(), new ParameterDefinition("bAsE", "1000")), settings.analysisId()));
        close(original.scale(.5), current(solve(model), "Rload", 0));
        var resistor = model.getCircuitComponents().stream().filter(c -> c.getName().equals("Rload")).findFirst().orElseThrow();
        var owned = resistor.getAcComponent();
        resistor.setAcComponent(new AcComponent(owned.instance(), owned.aliases(), owned.mirrored(), new AcComponent.Passive("250")));
        close(original.scale(4), current(solve(model), "Rload", 0));
        var saved = roundTrip(model);
        close(original.scale(4), current(solve(saved), "Rload", 0));
        assertEquals(250, saved.getCircuitComponents().get(3).getNumericParameters()[0]);
        resistor.setAcComponent(new AcComponent(owned.instance(), owned.aliases(), owned.mirrored(),
                new AcComponent.Passive("{missing}")));
        assertInstanceOf(PreparationResult.Inspectable.class, prepare(model, PreparationRequest.defaults()));
    }

    @Test
    void rejectsTransientOverridesAndMalformedVersionedData() {
        var model = spice(PHASOR);
        assertInstanceOf(PreparationResult.Rejected.class,
                prepare(model, new PreparationRequest(new SolverOverrides(null, 1e-4, null), Map.of())));
        assertThrows(IllegalArgumentException.class, () -> AcMetadataCodec.decodeSettings("2.MA"));
        assertThrows(IllegalArgumentException.class, () -> AcMetadataCodec.decodeComponent("1.eA.MA.Vk9MVEFHRQ"));
        assertInstanceOf(PreparationResult.Inspectable.class,
                prepare(spice(PHASOR.replace("Rload output 0 {LoadOhms}", "Lload output 0 10u")), PreparationRequest.defaults()));
    }

    @Test
    void preparationSeparatesSavedTransientFieldsFromAcReadiness() throws Exception {
        var model = spice(PHASOR);
        model.setTimeStep(Double.NaN);
        model = roundTrip(model);
        assertEquals(1, model.getSavedTransientSettingIssues().size());
        assertEquals(4, solve(model).frequencyHz().size());
        model.getImportIssues().add(new ImportIssue("INVALID_SETTING", null, null, "Unknown saved setting",
                "synthetic.ipes", false));
        model.getImportIssues().add(new ImportIssue("INVALID_SETTING", null, null, "An AC parameter is invalid",
                "synthetic.ipes", false, "analysis"));
        model.getImportIssues().add(new ImportIssue("UNSUPPORTED_DIRECTIVE", "SPICE", null, "An item is unsupported",
                "synthetic.ipes", true));
        var inspectable = assertInstanceOf(PreparationResult.Inspectable.class, prepare(model, PreparationRequest.defaults()));
        assertEquals(List.of("Unknown saved setting", "An AC parameter is invalid", "An item is unsupported"),
                inspectable.report().diagnostics().stream().map(diagnostic -> diagnostic.message()).toList());

        var transientModel = spice(".tran 1u 1m\nV1 input 0 1\nR1 input 0 1k\n");
        var ready = assertInstanceOf(PreparationResult.Ready.class, prepare(transientModel, PreparationRequest.defaults()));
        assertFalse(ready.execution().isAc());
        assertThrows(IllegalStateException.class, ready.execution()::acPlan);
        assertEquals(1e-6, ready.execution().newRunModel().getTimeStep());
    }

    @Test
    void preservesForeignTransientNamedDiagnosticsInAcPreparation() throws Exception {
        var model = spice(PHASOR);
        var issue = new ImportIssue("INVALID_SETTING", "SPICE", "Vexc", "Unsupported device setting",
                "foreign fixture", true, "solver");
        model.addConversionIssue(issue);
        for (var circuit : List.of(model, roundTrip(model))) {
            assertEquals(List.of(issue), circuit.getConversionIssues());
            var prepared = assertInstanceOf(PreparationResult.Inspectable.class, prepare(circuit, PreparationRequest.defaults()));
            var found = prepared.report().diagnostics().getFirst();
            assertEquals("INVALID_SETTING", found.code());
            assertEquals("SPICE", found.domain());
            assertEquals("Vexc", found.component());
            assertEquals("solver", found.parameterPath());
            assertEquals("foreign fixture", found.sourceLocation());
        }
    }

    @Test
    void grammarSharesPrecedenceScalarsAndLaplaceFunctions() {
        assertEquals(-4, SpiceValue.evaluate("-2^2", Map.of()));
        assertEquals(512, SpiceValue.evaluate("2**3**2", Map.of()));
        assertEquals(2_000_004, SpiceValue.evaluate("{Forward+2Meg}", Map.of("FoRWARD", "4")));
        assertEquals(10, SpiceValue.evaluate("s+sqrt(9)", Map.of("S", "7")));
        assertThrows(IllegalArgumentException.class, () -> SpiceValue.evaluate("s", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> SpiceValue.evaluate("a", Map.of("a", "b", "b", "A")));
        assertThrows(IllegalArgumentException.class, () -> SpiceValue.evaluate("(".repeat(130) + "1" + ")".repeat(130), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> SpiceValue.evaluate("1/(1/0)", Map.of()));
        var s = new Complex(0, 20);
        close(new Complex(0, 19.95), SpiceValue.transfer("1/s+s", Map.of("S", "8")).evaluate(s));
        close(new Complex(1.0 / 401, -20.0 / 401), SpiceValue.transfer("1/(1+s)", Map.of()).evaluate(s));
    }

    @Test
    void acceptsLongFlatChainsWithoutRelaxingNestedLimits() {
        String sum = "1+".repeat(130) + "1";
        String product = "1*".repeat(130) + "2";
        assertEquals(131, SpiceValue.evaluate(sum, Map.of()));
        assertEquals(2, SpiceValue.evaluate(product, Map.of()));
        assertEquals(70, SpiceValue.evaluate("200" + "-1".repeat(130), Map.of()));
        assertEquals(1, SpiceValue.evaluate("2/2" + "/1".repeat(130), Map.of()));
        close(new Complex(131, 2), SpiceValue.transfer(sum + "+s", Map.of()).evaluate(new Complex(0, 2)));
        close(new Complex(0, 4), SpiceValue.transfer(product + "*s", Map.of()).evaluate(new Complex(0, 2)));
        assertThrows(IllegalArgumentException.class, () -> SpiceValue.evaluate("sqrt(".repeat(130) + "1" + ")".repeat(130), Map.of()));
    }

    @Test
    void rejectsFloatingResistorTriangleEvenWithSeparatelyGroundedDrivenNetwork() throws Exception {
        var floating = spice(PHASOR + "Rf1 a b 1k\nRf2 b c 2.2k\nRf3 c a 3.3k\n");
        var prepared = assertInstanceOf(PreparationResult.Inspectable.class, prepare(roundTrip(floating), PreparationRequest.defaults()));
        assertTrue(prepared.report().diagnostics().getFirst().message().contains("floating AC group"));
        var grounded = spice(PHASOR + "Rf1 a b 1k\nRf2 b c 2.2k\nRf3 c a 3.3k\nRf4 c 0 1t\n");
        close(Complex.ZERO, voltage(solve(grounded), "Rf1", 2, 0));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void identifiesGroundedFeedbackRankLossWithAndWithoutExcitation(boolean reverse) throws Exception {
        for (String scale : List.of("", "e-100", "e100")) {
            for (String magnitude : List.of("0", "1")) {
                var singular = roundTrip(spice(feedback("4", scale, magnitude, reverse)));
                var e1 = singular.getCircuitComponents().stream().filter(part -> part.getName().equals("E1"))
                        .findFirst().orElseThrow();
                var graph = CircuitTopology.resolve(singular);
                assertEquals(reverse, graph.group(e1, CircuitTopology.Side.OUTPUT, 0).id()
                        < graph.group(e1, CircuitTopology.Side.INPUT, 0).id());
                var prepared = assertInstanceOf(PreparationResult.Ready.class, prepare(singular, PreparationRequest.defaults()));
                var error = assertThrows(IllegalArgumentException.class, prepared.execution().acPlan()::execute);
                assertTrue(error.getMessage().contains("numerically singular"), error.getMessage());
            }
            double gain = 3.9999;
            var unique = solve(roundTrip(spice(feedback(Double.toString(gain), scale, "1", reverse))));
            double expected = 1 / (1 - gain / 4);
            for (int sample = 0; sample < unique.frequencyHz().size(); sample++) {
                close(new Complex(expected, 0), voltage(unique, "E1", 1, sample));
                assertTrue(unique.residualEta().get(sample) <= 1e-12);
            }
        }
    }

    private static String feedback(String gain, String scale, String magnitude, boolean reverse) {
        String source = "E1 out bias sense 0 " + gain + "\n";
        String feedbackResistor = "R1 out sense 297" + scale + "\n";
        String groundResistor = "R2 sense 0 99" + scale + "\n";
        return ".ac dec 1 10 100\nV1 bias 0 AC " + magnitude + "\n"
                + (reverse ? feedbackResistor + groundResistor + source : source + groundResistor + feedbackResistor);
    }

    @Test
    void stopsAnInProgressAcSweepOnInterrupt() throws Exception {
        var model = spice(PHASOR.replace(".ac dec 3 10 100", ".ac dec 30000 10 10000"));
        var ready = assertInstanceOf(PreparationResult.Ready.class, prepare(model, PreparationRequest.defaults()));
        var plan = ready.execution().acPlan();
        var entered = new CountDownLatch(1);
        var outcome = new AtomicReference<Throwable>();
        var thread = Thread.ofPlatform().daemon().start(() -> {
            entered.countDown();
            try {
                plan.execute();
                outcome.set(new AssertionError("The AC sweep finished after cancellation."));
            } catch (Throwable error) {
                outcome.set(error);
            }
        });
        try {
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            boolean executing = false;
            while (thread.isAlive() && System.nanoTime() < deadline && !executing) {
                executing = java.util.Arrays.stream(thread.getStackTrace()).anyMatch(frame ->
                        frame.getClassName().equals(AcExecutionPlan.class.getName()) && frame.getMethodName().equals("solve"));
                if (!executing) Thread.sleep(1);
            }
            assertTrue(executing, "The solver did not enter factorization.");
        } finally {
            thread.interrupt();
            thread.join(2000);
        }
        assertFalse(thread.isAlive(), "The AC solve did not stop within two seconds.");
        assertInstanceOf(CancellationException.class, outcome.get());
    }

    @Test
    void matrixResidualChecksIllScaledAndCorruptedSolutions() {
        var model = spice(PHASOR.replace("LoadOhms={2*Base}", "LoadOhms=1p"));
        assertTrue(solve(model).residualEta().stream().allMatch(eta -> eta <= 1e-12));
        Complex[] b = {new Complex(1, 0), new Complex(2e-12, 0)};
        Complex[][] a = {{Complex.ONE, Complex.ZERO}, {Complex.ZERO, new Complex(1e-12, 0)}};
        assertEquals(0, AcExecutionPlan.residual(a, b, new Complex[]{Complex.ONE, new Complex(2, 0)}));
        assertTrue(AcExecutionPlan.residual(a, b, new Complex[]{new Complex(0.9, 0), new Complex(2, 0)}) > 1e-12);
        assertThrows(IllegalArgumentException.class, () -> solve(spice(PHASOR + "Vduplicate output 0 AC 1\n")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"R0", "R90", "R180", "R270", "M0", "M90", "M180", "M270"})
    void tracksEveryPinAndOffCenterFlagsAfterRotationAndReflection(String orientation) throws Exception {
        String source = orientedSource(orientation);
        var model = roundTrip(new LtspiceImporter().convert(source, "oriented.asc"));
        assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
        var result = solve(model);
        close(polar(3, 17), voltage(result, "E1", 3, 0));
        close(polar(1, -28), voltage(result, "E1", 4, 0));
        close(voltage(result, "E1", 3, 0).subtract(voltage(result, "E1", 4, 0)).scale(3),
                voltage(result, "E1", 1, 0).subtract(voltage(result, "E1", 2, 0)));
        close(polar(.4, 71), voltage(result, "E1", 2, 0));
        var e = model.getCircuitComponents().stream().filter(c -> c.getName().equals("E1")).findFirst().orElseThrow();
        assertEquals(orientation.startsWith("M"), e.getAcComponent().mirrored());
        assertEquals(List.of(1, 2, 3, 4), e.getAcComponent().spiceOrders());
        assertTrue(List.of(e.getTerminalXLabels()).stream().allMatch(String::isEmpty));
        var records = new ArrayList<>(source.lines().filter(line -> line.startsWith("WIRE") || line.startsWith("FLAG")).toList());
        Collections.reverse(records);
        records.addAll(source.lines().filter(line -> !line.startsWith("WIRE") && !line.startsWith("FLAG")).toList());
        var shuffled = new LtspiceImporter().convert(String.join("\n", records), "shuffled.asc");
        assertEquals(partition(result), partition(solve(shuffled)));
    }

    @Test
    void severingAnUnlabeledWireSeparatesItsPhysicalMembers() throws Exception {
        var model = roundTrip(new LtspiceImporter().convert("""
                Version 4
                SHEET 1 400 400
                WIRE 0 16 192 16
                FLAG 0 96 0
                FLAG 192 96 0
                SYMBOL voltage 0 0 R0
                SYMATTR InstName V1
                SYMATTR Value ""
                SYMATTR Value2 AC 1 30
                SYMBOL res 176 0 R0
                SYMATTR InstName R1
                SYMATTR Value 1k
                TEXT 0 200 Left 2 !.ac dec 10 10 100
                """, "rewire.asc"));
        assertEquals(0.001, current(solve(model), "R1", 0).abs(), 1e-14);
        var wire = model.getConnections().stream().filter(w -> w.getLabel().isEmpty()
                && Math.abs(w.getPoints()[0][0] - w.getPoints()[1][0]) >= 24).findFirst().orElseThrow();
        model.getConnections().remove(wire);
        var source = model.getCircuitComponents().getFirst();
        var load = model.getCircuitComponents().getLast();
        var topology = CircuitTopology.resolve(model);
        assertNotEquals(topology.group(source, CircuitTopology.Side.INPUT, 0).id(),
                topology.group(load, CircuitTopology.Side.INPUT, 0).id());
        close(Complex.ZERO, current(solve(roundTrip(model)), "R1", 0));
        model.addConnection(wire);
        assertEquals(.001, current(solve(roundTrip(model)), "R1", 0).abs(), 1e-14);
    }

    private static String orientedSource(String orientation) {
        String[] labels = {"output", "return", "positive", "negative"};
        int[][] pins = {{0, 16}, {0, 96}, {-48, 32}, {-48, 80}};
        int[][] direction = {{0, -16}, {0, 16}, {-16, 0}, {-16, 0}};
        var text = new StringBuilder("Version 4\nSHEET 1 1500 500\n");
        for (int i = 0; i < pins.length; i++) {
            int[] a = transform(orientation, pins[i][0], pins[i][1]);
            int[] b = transform(orientation, pins[i][0] + 2 * direction[i][0], pins[i][1] + 2 * direction[i][1]);
            text.append("WIRE ").append(a[0]).append(' ').append(a[1]).append(' ').append(b[0]).append(' ').append(b[1]).append('\n');
            text.append("FLAG ").append((a[0] + b[0]) / 2).append(' ').append((a[1] + b[1]) / 2).append(' ').append(labels[i]).append('\n');
        }
        return text.append("SYMBOL e 0 0 ").append(orientation).append("""

                SYMATTR InstName E1
                SYMATTR Value 3
                FLAG 400 16 positive
                FLAG 400 96 0
                SYMBOL voltage 400 0 R0
                SYMATTR InstName V1
                SYMATTR Value ""
                SYMATTR Value2 AC 3 17
                FLAG 600 16 negative
                FLAG 600 96 0
                SYMBOL voltage 600 0 R0
                SYMATTR InstName V2
                SYMATTR Value AC 1 -28
                FLAG 800 16 return
                FLAG 800 96 0
                SYMBOL voltage 800 0 R0
                SYMATTR InstName V3
                SYMATTR Value AC .4 71
                FLAG 1000 16 output
                FLAG 1000 96 0
                SYMBOL res 984 0 R0
                SYMATTR InstName R1
                SYMATTR Value 1k
                TEXT 0 300 Left 2 !.ac dec 4 10 100
                """).toString();
    }

    private static int[] transform(String orientation, int x, int y) {
        for (int turn = Integer.parseInt(orientation.substring(1)) / 90; turn > 0; turn--) {
            int oldX = x;
            x = -y;
            y = oldX;
        }
        return new int[]{orientation.startsWith("M") ? -x : x, y};
    }

    private static Set<Set<AcResult.Member>> partition(AcResult result) {
        return result.groups().stream().map(g -> Set.copyOf(g.members())).collect(Collectors.toSet());
    }

    private static Complex polar(double magnitude, double degrees) {
        double angle = Math.PI * degrees / 180;
        return new Complex(magnitude * Math.cos(angle), magnitude * Math.sin(angle));
    }

    private static PreparationResult prepare(CircuitModel model, PreparationRequest request) {
        var preparation = new CircuitPreparation();
        return preparation.prepare(preparation.capture(model), request);
    }

    private static AcResult solve(CircuitModel model) {
        var prepared = prepare(model, PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, prepared, prepared.report().toString());
        assertThrows(IllegalArgumentException.class, ready.execution()::solverSettings);
        assertThrows(IllegalArgumentException.class, ready.execution()::newRunModel);
        return ready.execution().acPlan().execute();
    }

    private static CircuitModel spice(String text) { return new SpiceImporter().convert(text, "synthetic.cir"); }

    private static CircuitModel roundTrip(CircuitModel model) throws Exception {
        return new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(model)), "synthetic.ipes");
    }

    private static Complex voltage(AcResult result, String instance, int terminal, int sample) {
        return result.groups().stream().filter(g -> g.members().contains(new AcResult.Member(instance, terminal)))
                .findFirst().orElseThrow().voltageV().get(sample);
    }

    private static Complex current(AcResult result, String instance, int sample) {
        return result.currents().stream().filter(i -> i.instance().equals(instance)).findFirst().orElseThrow().currentA().get(sample);
    }

    private static void close(Complex expected, Complex actual) {
        assertTrue(expected.subtract(actual).abs() <= 1e-12 + 1e-9 * expected.abs(), expected + " != " + actual);
    }
}
