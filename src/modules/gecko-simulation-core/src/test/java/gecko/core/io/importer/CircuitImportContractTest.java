package gecko.core.io.importer;

import gecko.core.circuit.TokenMap;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitTopology;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class CircuitImportContractTest {
    @TempDir Path directory;

    private CircuitModel convert(String text) { return new SpiceImporter().convert(text, "test.cir"); }
    private CircuitModel roundTrip(CircuitModel model) throws Exception {
        return new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(model)), "roundtrip.ipes");
    }
    private Path write(CircuitModel model, String name) throws Exception {
        Path path = directory.resolve(name);
        new CircuitFileWriter().write(model, path);
        return path;
    }

    @Test void importedPulseRcMatchesAnalyticVoltageWithinOnePercent() throws Exception {
        String source;
        try (var stream = getClass().getResourceAsStream("/importer/rc.cir")) {
            source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        var model = convert(source);
        assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
        var engine = new HeadlessSimulationEngine();
        var times = new ArrayList<Double>();
        var voltages = new ArrayList<Double>();
        engine.setStepTraceListener((time, potentials, currents, netlist) -> {
            int capacitor = netlist.getElementIndexByName("C1");
            times.add(time);
            voltages.add(potentials[netlist.getNodeX(capacitor)] - potentials[netlist.getNodeY(capacitor)]);
        });
        var result = engine.runSimulation(SimulationConfig.builder().circuitFile(write(model, "rc.ipes").toString()).build());
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertTrue(times.size() > 5000);
        for (int i = 0; i < times.size(); i++) {
            double elapsed = Math.max(0, times.get(i) - 100.5e-6);
            double expected = 5 * (1 - Math.exp(-elapsed / 1e-3));
            assertEquals(expected, voltages.get(i), 0.05, "t=" + times.get(i));
        }
    }

    @Test void repeatedSerializationPreservesTopologySlotsSettingsAndEmbeddedWaveforms() throws Exception {
        var model = convert("V1 in 0 PULSE(0 5 10u 1u 1u 100u 200u)\nR1 in out 1k\nC1 out 0 1u\n.tran 1u 1m\n.end");
        var first = roundTrip(model);
        var second = roundTrip(first);
        assertEquals(3, second.getCircuitComponents().size());
        assertEquals(first.getTimeStep(), second.getTimeStep());
        assertEquals(first.getSimulationDuration(), second.getSimulationDuration());
        assertEquals(first.getImportIssues(), second.getImportIssues());
        assertEquals(topology(model), topology(first));
        assertEquals(topology(first), topology(second));
        for (int i = 0; i < first.getCircuitComponents().size(); i++) {
            var a = first.getCircuitComponents().get(i);
            var b = second.getCircuitComponents().get(i);
            assertEquals(a.getType(), b.getType());
            assertEquals(a.getName(), b.getName());
            assertArrayEquals(a.getNumericParameters(), b.getNumericParameters());
            assertArrayEquals(a.getPosition(), b.getPosition());
        }
        assertEquals(1, second.getResources().size());
        model.getResources().forEach((id, resource) -> {
            assertArrayEquals(resource.bytes(), first.getResources().get(id).bytes());
            assertArrayEquals(resource.bytes(), second.getResources().get(id).bytes());
            assertEquals(resource.ownerIds(), first.getResources().get(id).ownerIds());
            assertEquals(resource.ownerIds(), second.getResources().get(id).ownerIds());
        });
        try (var gzip = new GZIPInputStream(new ByteArrayInputStream(new CircuitFileWriter().write(second)))) {
            var tokens = new TokenMap(new String(gzip.readAllBytes(), StandardCharsets.UTF_8).split("\\R"), true);
            assertTrue(tokens.readDataLine("worksheetSizeX", 0) > model.getControlComponents().getLast().getPosition()[0]);
            assertTrue(tokens.readDataLine("worksheetSizeY", 0) > model.getControlComponents().getLast().getPosition()[1]);
            var files = tokens.getSpecialBlockTokenMap("GeckoFileManager");
            assertNotNull(files);
            var embedded = files.getBlockTokenMap("<GeckoFile>");
            var resource = second.getResources().values().iterator().next();
            assertArrayEquals(resource.bytes(), embedded.readDataLine("fileContents[]", new byte[0]));
            assertArrayEquals(new String[]{resource.ownerIds().getFirst().toString()},
                    embedded.readDataLine("usageList[]", new String[0]));
            assertNull(tokens.getSpecialBlockTokenMap("GeckoFileManager"));
        }
    }

    private static List<String> topology(CircuitModel model) {
        var topology = CircuitTopology.resolve(model);
        var names = new java.util.HashMap<CircuitTopology.ComponentIdentity, String>();
        Stream.of(model.getCircuitComponents(), model.getControlComponents(), model.getThermalComponents())
                .flatMap(List::stream).forEach(component -> names.put(topology.identity(component), component.getName()));
        return topology.groups().values().stream().map(group -> group.domain() + ":" + group.canonicalLabel()
                + ":" + group.terminals().stream().map(terminal -> names.get(terminal.component())
                        + "/" + terminal.side() + "/" + terminal.port()).sorted().toList()).sorted().toList();
    }

    @Test void unsupportedBjtIsReportedAndRetainedAcrossReopening() throws Exception {
        var model = convert("BJT fixture with supported components\nV1 in 0 5\nQ1 in base 0 NPN\nR1 in 0 1k\n.tran 1u 1m\n.end");
        assertEquals(2, model.getCircuitComponents().size());
        assertTrue(model.getImportIssues().stream().anyMatch(issue -> "Q1".equals(issue.component()) && issue.message().contains("Q")));
        var parsed = roundTrip(model);
        assertEquals(model.getImportIssues(), parsed.getImportIssues());
        var result = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder()
                .circuitFile(write(parsed, "partial.ipes").toString()).build());
        assertFalse(result.isSuccess(), "A partial conversion must not silently simulate.");
    }

    @Test void skipsSubcircuitBodiesAndInputAfterEnd() throws Exception {
        var model = convert("* SPICE title\nQload a b 0 NPN\n.param\tres=10\nV1 a 0 5\nR1 a 0 {res}\n"
                + ".subckt hidden a 0\n.param res=20\n.tran 10u 2m\nRinner a 0 1\n.ends\n"
                + ".tran\t2u 1m\n.end\n.param res=30\nRafter a 0 2");
        assertEquals(List.of("V1", "R1"), model.getCircuitComponents().stream().map(CircuitModel.ComponentData::getName).toList());
        assertEquals(10, model.getCircuitComponents().getLast().getNumericParameters()[0]);
        assertEquals(2e-6, model.getTimeStep());
        assertEquals(1e-3, model.getSimulationDuration());
        assertEquals(List.of(".subckt", "Qload"), model.getConversionIssues().stream()
                .map(gecko.core.io.ImportIssue::component).sorted().toList());
    }

    @Test void retainsWaveformExpressionsAndReportsMalformedAndOversizedSources() {
        var model = convert("R1 a 0 unknown\n.param a=2\nV1 a 0 PULSE(0 {(a+1)*sqrt(a*a)} 0 1u 1u 1m 3m)\n"
                + "V2 b 0 PULSE(0 1 0 1e-16 1e-16 1e-16 1e-15)\n.tran 1u 1m\n"
                + "L1 a 0 1m IC=1 TEMP=120\nD1 a 0 BAD\n.model BAD D(Ron=)");
        assertEquals(List.of("R1", "V2", "L1", "D1"), model.getConversionIssues().stream()
                .map(gecko.core.io.ImportIssue::component).toList());
        assertEquals(1, model.getCircuitComponents().size());
        String contents = new String(model.getResources().values().iterator().next().bytes(), StandardCharsets.UTF_8);
        assertTrue(contents.contains("1.0E-6 6.0"), contents);
        assertThrows(IllegalArgumentException.class,
                () -> SpiceValue.evaluate("(".repeat(500) + "1" + ")".repeat(500), java.util.Map.of()));
        var oversizedSin = convert("V1 a 0 SIN(1 2 3)\n.tran 1p 1");
        assertEquals(1, oversizedSin.getConversionIssues().size());
        assertTrue(oversizedSin.getResources().isEmpty());
    }

    @Test void ignoresDiagnosticTagsInsideProgramsAndRejectsInvalidRootDiagnostics() throws Exception {
        var parser = new CircuitFileParser();
        String plain = "<scripterCode>\n<ImportIssues>\nexisting script text\n<\\scripterCode>\n";
        var model = parser.parse(new ByteArrayInputStream(plain.getBytes(StandardCharsets.UTF_8)), "script.ipes");
        assertTrue(model.getConversionIssues().isEmpty());
        assertThrows(CircuitFileParser.CircuitParseException.class, () -> parser.parse(new ByteArrayInputStream(
                "<ImportIssues>\n".getBytes(StandardCharsets.UTF_8)), "unclosed.ipes"));
        assertThrows(CircuitFileParser.CircuitParseException.class, () -> parser.parse(new ByteArrayInputStream(
                "<ImportIssues>\nissue - - - - - - -\n<\\ImportIssues>\n".getBytes(StandardCharsets.UTF_8)), "invalid.ipes"));
    }

    @Test void reservesExplicitIdentifiersAndRejectsUnsavedParameters() throws Exception {
        var model = new CircuitModel();
        var explicit = SpiceImporter.component(1, "R1", 0, 0, "N1", "0");
        explicit.setParameter("uniqueObjectIdentifier", 2L);
        explicit.setNumericParameters(new double[]{1, 0, 0});
        model.addCircuitComponent(explicit);
        var generated = SpiceImporter.component(1, "R2", 8, 0, "N2", "0");
        generated.setNumericParameters(new double[]{2, 0, 0});
        model.addCircuitComponent(generated);
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{20, 20}, {21, 20}}, ""));
        var parsed = roundTrip(model);
        assertEquals(2L, parsed.getCircuitComponents().getFirst().getParameters().get("uniqueObjectIdentifier"));
        assertEquals(1L, parsed.getCircuitComponents().getLast().getParameters().get("uniqueObjectIdentifier"));
        assertEquals("", parsed.getConnections().getFirst().getLabel());
        generated.setParameter("resistance", 3.0);
        assertThrows(java.io.IOException.class, () -> new CircuitFileWriter().write(model));
    }

    @ParameterizedTest @CsvSource({"1k,1000", "1MEG,1000000", "2m,0.002", "3uF,0.000003", "{a*2+1},7", "2**3,8", "-2^2,-4"})
    void scalarParametersAndSpiceSuffixesAreEvaluated(String input, double expected) {
        assertEquals(expected, SpiceValue.evaluate(input, java.util.Map.of("a", "3")), 1e-12);
    }

    @Test void badElementAndCircularParametersDoNotAbortOtherElements() throws Exception {
        var model = convert("Example\n.param a={b} b={a}\nRbad 1 0 {a}\nCbad 1\nV1 1 0 5\nR1 1 0 10\n.tran 1u 1m");
        assertEquals(2, model.getCircuitComponents().size());
        assertEquals(2, model.getImportIssues().size());
        assertEquals(model.getImportIssues(), roundTrip(model).getImportIssues());
    }

    @Test void writerAndCliRefuseToOverwriteExistingFiles() throws Exception {
        var model = convert("V1 1 0 1\nR1 1 0 1\n.tran 1u 1m");
        Path target = write(model, "existing.ipes");
        byte[] before = Files.readAllBytes(target);
        assertThrows(java.nio.file.FileAlreadyExistsException.class, () -> new CircuitFileWriter().write(model, target));
        Path input = directory.resolve("input.cir");
        Files.writeString(input, "V1 1 0 2\nR1 1 0 1\n.tran 1u 1m");
        var diagnostics = new ByteArrayOutputStream();
        assertEquals(1, Main.run(new String[]{input.toString(), target.toString()}, new PrintStream(diagnostics), new PrintStream(diagnostics)));
        assertArrayEquals(before, Files.readAllBytes(target));
    }

    @Test void cliWritesAReadableFileAndStructuredPartialDiagnostics() throws Exception {
        Path input = directory.resolve("partial.cir"), target = directory.resolve("partial.ipes");
        Files.writeString(input, "Test\nV1 1 0 1\nR1 1 0 1\nQ1 1 2 0 NPN\n.tran 1u 1m");
        var diagnostics = new ByteArrayOutputStream();
        assertEquals(2, Main.run(new String[]{input.toString(), target.toString()}, new PrintStream(diagnostics), System.err));
        assertTrue(diagnostics.toString(StandardCharsets.UTF_8).contains("\"code\":\"UNSUPPORTED_ELEMENT\""));
        assertEquals(2, new CircuitFileParser().parse(target.toString()).getCircuitComponents().size());
    }

    @ParameterizedTest @ValueSource(strings = {"V", "I"})
    void sampledSourcesPreservePolarityAndShapeWithTheExistingCouplingDelay(String kind) throws Exception {
        for (String waveform : List.of("PULSE(-1 3 20u 10u 10u 50u 100u)", "SIN(1 2 1k 20u 100 30)")) {
            boolean pulse = waveform.startsWith("PULSE");
            var model = convert(kind + "1 " + (kind.equals("V") ? "in 0 " : "0 in ")
                    + waveform + "\nR1 in 0 1\n.tran 1u 500u");
            assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
            var engine = new HeadlessSimulationEngine();
            var values = new ArrayList<Double>();
            var clock = new ArrayList<Double>();
            engine.setStepTraceListener((time, volts, amps, netlist) -> {
                int resistor = netlist.getElementIndexByName("R1");
                values.add(volts[netlist.getNodeX(resistor)] - volts[netlist.getNodeY(resistor)]);
                clock.add(time);
            });
            var result = engine.runSimulation(SimulationConfig.builder().circuitFile(write(model,
                    kind + (pulse ? "pulse" : "sin") + ".ipes").toString()).build());
            assertTrue(result.isSuccess(), result.getErrorMessage());
            for (int i = 5; i < clock.size(); i++) {
                // The desktop and core sample SIGNAL-controlled electrical sources two steps later.
                double time = Math.max(0, clock.get(i) - 2e-6);
                double elapsed = time - 20e-6;
                double expected;
                if (pulse) {
                    double phase = elapsed < 0 ? -1 : elapsed % 100e-6;
                    expected = phase < 0 ? -1 : phase < 10e-6 ? -1 + 4 * phase / 10e-6
                            : phase < 60e-6 ? 3 : phase < 70e-6 ? 3 - 4 * (phase - 60e-6) / 10e-6 : -1;
                } else {
                    elapsed = Math.max(0, elapsed);
                    expected = 1 + 2 * Math.exp(-100 * elapsed) * Math.sin(2 * Math.PI * 1e3 * elapsed + Math.PI / 6);
                }
                assertEquals(expected, values.get(i), pulse ? 0.02 : 0.03, kind + " " + waveform + " t=" + clock.get(i));
            }
        }
    }

    @Test void voltageControlledSwitchConductsFromTheImportedComparator() throws Exception {
        var model = convert("Switch\nV1 supply 0 10\nVcontrol ctl 0 PULSE(0 5 50u 1u 1u 200u 500u)\n"
                + "S1 supply out ctl 0 SWMODEL\nR1 out 0 9\n.model SWMODEL SW(Ron=1 Roff=1e9 Vt=2 Vh=0)\n.tran 1u 400u");
        assertTrue(model.getImportIssues().isEmpty(), model.getImportIssues().toString());
        var engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, volts, amps, netlist) -> {
            if (time > 60e-6 && time < 240e-6) assertEquals(1, amps[netlist.getElementIndexByName("R1")], 1e-10);
            if (time > 270e-6) assertEquals(0, amps[netlist.getElementIndexByName("R1")], 1e-7);
        });
        var result = engine.runSimulation(SimulationConfig.builder().circuitFile(write(model, "switch.ipes").toString()).build());
        assertTrue(result.isSuccess(), result.getErrorMessage());
    }

    @Test void hystereticSwitchUsesTheSpecifiedUpperAndLowerThresholds() throws Exception {
        var model = convert("V1 supply 0 10\nVctl ctl 0 PULSE(0 3.5 20u 10u 10u 50u 200u)\n"
                + "S1 supply out ctl 0 SWMODEL\nR1 out 0 9\n.model SWMODEL SW(Ron=1,Roff=1e9,Vt=2,Vh=1)\n"
                + ".tran 1u 120u");
        assertTrue(model.getConversionIssues().isEmpty(), model.getConversionIssues().toString());
        var engine = new HeadlessSimulationEngine();
        var trace = new java.util.TreeMap<Double, Double>();
        engine.setStepTraceListener((time, volts, amps, netlist) -> trace.put(time, amps[netlist.getElementIndexByName("R1")]));
        var result = engine.runSimulation(SimulationConfig.builder().circuitFile(write(model, "hysteresis.ipes").toString()).build());
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(0, trace.ceilingEntry(19e-6).getValue(), 1e-7);
        assertEquals(1, trace.ceilingEntry(50e-6).getValue(), 1e-10);
        assertEquals(1, trace.ceilingEntry(86e-6).getValue(), 1e-10);
        assertEquals(0, trace.ceilingEntry(100e-6).getValue(), 1e-7);
    }

    @Test void educationCorpusRetainsSavedTopologyAndRawConfigurationAfterWriting() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("pom-reactor.xml"))) root = root.getParent();
        assertNotNull(root);
        try (var paths = Files.list(root.resolve("src/test/resources/ipes/education"))) {
            var files = paths.filter(p -> p.toString().endsWith(".ipes")).toList();
            assertEquals(21, files.size());
            for (var file : files) {
                var original = new CircuitFileParser().parse(file.toString());
                var parsed = roundTrip(original);
                assertEquals(original.getCircuitComponents().size(), parsed.getCircuitComponents().size(), file.toString());
                assertEquals(original.getControlComponents().size(), parsed.getControlComponents().size(), file.toString());
                assertEquals(topology(original), topology(parsed), file.toString());
                for (int i = 0; i < original.getCircuitComponents().size(); i++)
                    assertArrayEquals(original.getCircuitComponents().get(i).getNumericParameters(),
                            parsed.getCircuitComponents().get(i).getNumericParameters(), file.toString());
                for (int i = 0; i < original.getControlComponents().size(); i++)
                    assertArrayEquals(original.getControlComponents().get(i).getNumericParameters(),
                            parsed.getControlComponents().get(i).getNumericParameters(), file.toString());
            }
        }
    }
}
