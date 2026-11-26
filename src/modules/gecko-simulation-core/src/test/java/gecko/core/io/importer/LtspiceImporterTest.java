package gecko.core.io.importer;

import gecko.core.ac.AcComponent;
import gecko.core.ac.AcInputOrigin;
import gecko.core.io.AcMetadataCodec;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitTopology;
import gecko.core.io.CircuitTopology.Side;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class LtspiceImporterTest {
    @TempDir Path directory;

    @ParameterizedTest
    @ValueSource(strings = {
        "SYMATTR SpiceLine Rser=0\\nSYMATTR SpiceLine Rser=1m",
        "SYMATTR SpiceLine Rser=0\\nSYMATTR Value2 Rser=1m",
        "SYMATTR SpiceLine Rpar=1k",
        "SYMATTR SpiceLine Cpar=1p",
        "SYMATTR ModelFile model.lib",
        "SYMATTR SpiceModel model",
        "SYMATTR Value2 Flux=I",
        "TEXT 0 264 Left 2 !.option temp=85",
        "TEXT 0 264 Left 2 !.options logparams",
        "TEXT 0 264 Left 2 !.model test IND",
        "TEXT 0 264 Left 2 !K1 L1 L2 .99",
        "SYMBOL diode 320 0 R0\\nSYMATTR InstName D1\\nSYMATTR Value D"
    })
    void rejectsProfiledInductorsWithUnsupportedElectricalInput(String extra) throws Exception {
        var input = writeProfileFixture(extra.replace("\\n", "\n"));
        assertThrows(IllegalArgumentException.class, () -> new LtspiceImporter().convert(input, directory, List.of(),
                AcInputOrigin.LTSPICE_26_1_1_NORMAL_27C));
    }

    @Test void rejectsProfileOnPortableOrWrongVersionAndKeepsUnselectedAsc() throws Exception {
        var input = writeProfileFixture("");
        var converted = new LtspiceImporter().convert(input, directory);
        assertFalse(converted.getConversionIssues().isEmpty());
        assertEquals(AcInputOrigin.UNSELECTED_ASC, converted.getAcProvenance().origin());
        var portable = Files.writeString(directory.resolve("portable.cir"), ".ac dec 1 80 80\nL1 n 0 1u");
        var bytes = new ByteArrayOutputStream();
        try (var output = new PrintStream(bytes)) {
            Path target = directory.resolve("no-import.ipes");
            assertEquals(1, Main.run(new String[]{portable.toString(), target.toString(), "--symbols",
                    directory.toString(), "--profile", AcInputOrigin.LTSPICE_26_1_1_NORMAL_27C.profileId()}, output, output));
            assertEquals(1, Main.run(new String[]{input.toString(), target.toString(), "--symbols", directory.toString(),
                    "--profile", "ltspice-26.1.1-normal-27c-v2"}, output, output));
            assertFalse(Files.exists(target));
            assertEquals(0, Main.run(new String[]{input.toString(), target.toString(), "--symbols", directory.toString(),
                    "--profile", AcInputOrigin.LTSPICE_26_1_1_NORMAL_27C.profileId()}, output, output), bytes.toString());
            assertTrue(new CircuitFileParser().parse(target.toString()).getAcProvenance().origin().tested());
        }
    }

    @Test void keepsLegacyAliasOffsetAndRejectsMalformedProfiledRecords() {
        var origin = AcInputOrigin.LTSPICE_26_1_1_NORMAL_27C;
        var legacy = new AcComponent("L1", List.of("A", "B", "extra"), false, new AcComponent.Inductance("1u", "4m"));
        assertEquals(legacy, AcMetadataCodec.decodeComponent(AcMetadataCodec.encode(legacy)));
        var valid = new AcComponent("L1", List.of("A", "B"), false, new AcComponent.Inductance("1u",
                new AcComponent.ImportedDefault(), origin));
        var fields = AcMetadataCodec.encode(valid).split("\\.", -1);
        for (String[] mutation : List.of(
                new String[]{"4", "INDUCTANCE_ORIGIN_V2"},
                new String[]{"7", "unknown-profile"},
                new String[]{"8", "0".repeat(64)},
                new String[]{"9", "UNKNOWN"},
                new String[]{"9", "EXPLICIT"},
                new String[]{"10", "1m"})) {
            String[] invalid = fields.clone();
            invalid[Integer.parseInt(mutation[0])] = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mutation[1].getBytes(StandardCharsets.UTF_8));
            assertThrows(IllegalArgumentException.class, () -> AcMetadataCodec.decodeComponent(String.join(".", invalid)));
        }
        assertThrows(IllegalArgumentException.class,
                () -> AcMetadataCodec.decodeComponent(AcMetadataCodec.encode(valid) + "."));
    }

    private Path writeProfileFixture(String extra) throws Exception {
        for (String name : List.of("voltage", "ind")) {
            try (var resource = getClass().getResourceAsStream("/ltspice-hierarchy/" + name + ".asy")) {
                Files.copy(java.util.Objects.requireNonNull(resource), directory.resolve(name + ".asy"));
            }
        }
        return Files.writeString(directory.resolve("inductor.asc"), """
                Version 4
                SHEET 1 640 480
                WIRE 0 16 160 16
                FLAG 0 96 0
                FLAG 160 96 0
                SYMBOL voltage 0 0 R0
                SYMATTR InstName V1
                SYMATTR Value AC 1
                SYMBOL ind 144 0 R0
                SYMATTR InstName L1
                SYMATTR Value 1u
                %s
                TEXT 32 180 Left 2 !.ac dec 1 80 80
                """.formatted(extra));
    }

    @Test void importsTheSameRcWaveformAsTheSpiceNetlistAndKeepsItsLayout() throws Exception {
        var schematic = convert(fixture("rc.asc"));
        var netlist = new SpiceImporter().convert(fixture("rc.cir"), "rc.cir");
        assertTrue(schematic.getConversionIssues().isEmpty(), schematic.getConversionIssues().toString());
        assertEquals(netlist.getTimeStep(), schematic.getTimeStep());
        assertEquals(netlist.getSimulationDuration(), schematic.getSimulationDuration());
        var saved = roundTrip(schematic);
        assertArrayEquals(new int[]{6, 11}, component(saved, "V1").getPosition());
        assertArrayEquals(new int[]{19, 6}, component(saved, "R1").getPosition());
        assertArrayEquals(new int[]{30, 10}, component(saved, "C1").getPosition());
        assertEquals(502, component(saved, "R1").getOrientation());
        assertEquals(11, saved.getConnections().size());
        assertArrayEquals(new int[][]{{14, 6}, {6, 6}}, saved.getConnections().getFirst().getPoints());

        var expected = trace(netlist, "netlist.ipes", "C1");
        var actual = trace(schematic, "schematic.ipes", "C1");
        assertEquals(expected.times, actual.times);
        assertTrue(expected.values.size() > 5000);
        for (int i = 0; i < expected.values.size(); i++) {
            assertEquals(expected.values.get(i), actual.values.get(i), 0.05, "t=" + actual.times.get(i));
        }
    }

    @ParameterizedTest
    @CsvSource({
        "R0,   16,16,    16,96,  503",
        "R90, -16,16,   -96,16,  504",
        "R180,-16,-16,  -16,-96, 501",
        "R270, 16,-16,   96,-16, 502",
        "M0,  -16,16,   -16,96,  503",
        "M90,  16,16,    96,16,  502",
        "M180, 16,-16,   16,-96, 501",
        "M270,-16,-16,  -96,-16, 504"
    })
    void rotatesAndMirrorsEachPinBeforeConnectingIt(String rotation, int x1, int y1, int x2, int y2, int expected)
            throws Exception {
        var model = roundTrip(convert("""
                Version 4
                FLAG %d %d positive
                FLAG %d %d 0
                SYMBOL res 0 0 %s
                WINDOW 0 0 0 Left 2
                SYMATTR InstName R1
                SYMATTR Value 1k
                """.formatted(x1, y1, x2, y2, rotation)));
        assertTrue(model.getConversionIssues().isEmpty(), model.getConversionIssues().toString());
        var resistor = component(model, "R1");
        assertEquals(expected, resistor.getOrientation());
        assertArrayEquals(new String[]{"positive"}, resistor.getTerminalXLabels());
        assertArrayEquals(new String[]{"0"}, resistor.getTerminalYLabels());
    }

    @Test void connectsTheTJunctionAndFlagAliasAndKeepsTheUnjoinedCrossingSeparate() throws Exception {
        var source = fixture("ltspice-layout.asc");
        var model = roundTrip(convert(source));
        assertTrue(model.getConversionIssues().isEmpty(), model.getConversionIssues().toString());
        assertEquals(1e-3, model.getSimulationDuration());
        assertEquals(1e-7, model.getTimeStep());
        var topology = CircuitTopology.resolve(model);
        var bus = topology.group(component(model, "R1"), Side.INPUT, 0);
        assertEquals("bus", bus.canonicalLabel());
        assertEquals(bus, topology.group(component(model, "L1"), Side.INPUT, 0));
        assertNotEquals(bus, topology.group(component(model, "C1"), Side.INPUT, 0));
        var horizontal = model.getConnections().getFirst();
        assertArrayEquals(new int[][]{{22, 14}, {18, 14}, {6, 14}}, horizontal.getPoints());

        var joined = roundTrip(convert(source.replace("FLAG -32 0 bus", "FLAG 0 0 bus")));
        var joinedTopology = CircuitTopology.resolve(joined);
        assertEquals(joinedTopology.group(component(joined, "R1"), Side.INPUT, 0),
                joinedTopology.group(component(joined, "C1"), Side.INPUT, 0));
    }

    @Test void mapsTheMirroredSwitchSensePinsAndItsPowerPins() throws Exception {
        var asc = convert("""
                Version 4
                FLAG 16 0 supply
                FLAG 96 0 out
                FLAG 80 -48 ctl
                FLAG 32 -48 0
                FLAG 0 112 supply
                FLAG 0 192 0
                FLAG 192 112 ctl
                FLAG 192 192 0
                FLAG 96 112 out
                FLAG 96 192 0
                SYMBOL sw 0 0 M90
                SYMATTR InstName S1
                SYMATTR Value SWMODEL
                SYMBOL voltage 0 96 R0
                SYMATTR InstName V1
                SYMATTR Value 10
                SYMBOL voltage 192 96 R0
                SYMATTR InstName Vctl
                SYMATTR Value PULSE(0 5 50u 1u 1u 200u 500u)
                SYMBOL res 80 96 R0
                SYMATTR InstName R1
                SYMATTR Value 9
                TEXT 0 224 Left 2 !.model SWMODEL SW(Ron=1 Roff=1e9 Vt=2 Vh=0)\n.tran 1u 400u
                """.replace("\n.tran", "\\n.tran"));
        assertTrue(asc.getConversionIssues().isEmpty(), asc.getConversionIssues().toString());
        assertEquals(502, component(asc, "S1").getOrientation());
        var probe = asc.getControlComponents().stream().filter(c -> c.getName().equals("VOLT.S1")).findFirst().orElseThrow();
        assertArrayEquals(new String[]{"ctl", "0", "0"}, probe.getParameterStrings());
        var trace = trace(asc, "switch.ipes", "R1");
        assertEquals(9, trace.values.get(100), 1e-7);
        assertEquals(0, trace.values.get(300), 1e-7);
    }

    @Test void importsTheCurrentAndDiodeSymbolsFromTheirPinOrder() throws Exception {
        var model = roundTrip(convert("""
                FLAG -64 0 0
                FLAG -64 80 output
                FLAG 16 0 output
                FLAG 16 64 0
                SYMBOL current -64 0 R0
                SYMATTR InstName I1
                SYMATTR Value SINE(1 2 1k)
                SYMBOL diode 0 0 R0
                SYMATTR InstName D1
                SYMATTR Value Dtest
                TEXT 0 144 Left 2 !.model Dtest D(Ron=0.01 Roff=1g Vfwd=0.7)
                TEXT 0 176 Left 2 !.tran 1u 1m
                """));
        assertTrue(model.getConversionIssues().isEmpty(), model.getConversionIssues().toString());
        var source = component(model, "I1");
        var diode = component(model, "D1");
        assertEquals(5, source.getType());
        assertEquals(6, diode.getType());
        assertArrayEquals(new String[]{"0"}, source.getTerminalXLabels());
        assertArrayEquals(new String[]{"output"}, source.getTerminalYLabels());
        assertArrayEquals(new String[]{"output"}, diode.getTerminalXLabels());
        assertArrayEquals(new String[]{"0"}, diode.getTerminalYLabels());
        assertEquals(0.7, diode.getNumericParameters()[1]);
        assertEquals(1, model.getResources().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GND", "gnd"})
    void groundFlagAliasesUseTheSameReferenceAsZero(String ground) throws Exception {
        var model = convert("""
                FLAG 0 16 0
                FLAG 0 96 out
                FLAG 96 16 %s
                FLAG 96 96 out
                SYMBOL voltage 0 0 R0
                SYMATTR InstName V1
                SYMATTR Value 1
                SYMBOL res 80 0 R0
                SYMATTR InstName R1
                SYMATTR Value 1k
                TEXT 0 200 Left 2 !.tran 1u 4u
                """.formatted(ground));
        var reopened = roundTrip(model);
        var topology = CircuitTopology.resolve(reopened);
        var groundGroup = topology.group(component(reopened, "R1"), Side.INPUT, 0);
        assertEquals("0", groundGroup.canonicalLabel());
        assertEquals(groundGroup, topology.group(component(reopened, "V1"), Side.INPUT, 0));
        var resistor = trace(model, "ground.ipes", "R1");
        assertEquals(1.0, resistor.values.getLast(), 1e-9);
    }

    @Test void nativeLeadAvoidsTheEndpointOfAnIndependentSourceWire() throws Exception {
        var model = roundTrip(convert("""
                FLAG 16 0 A
                FLAG 16 64 0
                FLAG 96 16 B
                FLAG 96 96 0
                WIRE 16 16 96 16
                SYMBOL cap 0 0 R0
                SYMATTR InstName C1
                SYMATTR Value 1u
                SYMBOL res 80 0 R0
                SYMATTR InstName R1
                SYMATTR Value 1k
                """));
        assertTrue(model.getConversionIssues().isEmpty(), model.getConversionIssues().toString());
        var topology = CircuitTopology.resolve(model);
        var capacitorNet = topology.group(component(model, "C1"), Side.INPUT, 0);
        var resistorNet = topology.group(component(model, "R1"), Side.INPUT, 0);
        assertEquals("A", capacitorNet.canonicalLabel());
        assertEquals("B", resistorNet.canonicalLabel());
        assertNotEquals(capacitorNet, resistorNet);
        var originalWire = model.getConnections().getFirst();
        assertEquals("B", originalWire.getLabel());
        assertArrayEquals(new int[][]{{6, 10}, {26, 10}}, originalWire.getPoints());
        assertEquals(5, model.getConnections().size());
    }

    @Test void impossibleCoincidentPlacementReportsALayoutIssueAndPreservesSourceWires() throws Exception {
        var model = convert("""
                FLAG 16 0 cap
                FLAG 16 64 0
                FLAG 80 -8 resistor
                FLAG 16 72 0
                WIRE 16 -8 80 -8
                SYMBOL cap 0 0 R0
                SYMATTR InstName C1
                SYMATTR Value 1u
                SYMBOL res 0 -24 R0
                SYMATTR InstName R1
                SYMATTR Value 1k
                """);
        var reopened = roundTrip(model);
        assertEquals(model.getConversionIssues(), reopened.getConversionIssues());
        assertTrue(reopened.getConversionIssues().stream().anyMatch(issue -> issue.code().equals("UNSUPPORTED_LAYOUT")
                && issue.unsupported() && issue.message().contains("cap") && issue.message().contains("resistor")));
        assertEquals("resistor", reopened.getConnections().getFirst().getLabel());
        assertEquals(2, reopened.getConnections().getFirst().getPoints().length);
        assertSimulationBlocked(model, "overlap.ipes");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1u 4u 0 1u loadstate=previous.ic", "4u loadstate=previous.ic", "4u\\n+ loadstate=previous.ic"})
    void retainsTheTransientStateModifierAsAnUnsupportedIssue(String arguments) throws Exception {
        var source = fixture("rc.asc").replace(".tran 1u 6m", ".tran " + arguments);
        var model = roundTrip(convert(source));
        assertEquals(4e-6, model.getSimulationDuration());
        assertEquals(1, model.getConversionIssues().size(), model.getConversionIssues().toString());
        var issue = model.getConversionIssues().getFirst();
        assertEquals("UNSUPPORTED_TRANSIENT_OPTION", issue.code());
        assertTrue(issue.message().contains("loadstate"), issue.message());
        assertEquals("test.asc:21", issue.sourceLocation());
        assertSimulationBlocked(model, "loadstate.ipes");
    }

    @Test void acceptsAnExplicitStockPrefixAndDiagnosesAChangedPrefix() throws Exception {
        String source = fixture("rc.asc").replace("SYMATTR InstName R1", "SYMATTR InstName R1\nSYMATTR Prefix r");
        var accepted = roundTrip(convert(source));
        assertTrue(accepted.getConversionIssues().isEmpty(), accepted.getConversionIssues().toString());
        assertEquals(1, component(accepted, "R1").getType());
        var rejected = roundTrip(convert(source.replace("Prefix r", "Prefix X")));
        assertEquals(2, rejected.getCircuitComponents().size());
        assertEquals("UNSUPPORTED_SYMBOL_ATTRIBUTE", rejected.getConversionIssues().getFirst().code());
        assertEquals("R1", rejected.getConversionIssues().getFirst().component());
    }

    @Test void retainsPartialDiagnosticsAndOriginalLineNumbers() throws Exception {
        var model = convert("""
                Version 4
                SYMBOL npn 192 0 R0
                SYMATTR InstName Q1
                WIRE 0 16 broken 96
                SYMBOL ind 0 0 R10
                SYMATTR InstName L1
                SYMBOL res 0 0 R0
                SYMATTR InstName Rbroken
                SYMATTR Value nope
                SYMBOL voltage -64 0 R0
                SYMATTR InstName V1
                SYMATTR Value 1
                TEXT 0 144 Left 2 !.tran 1m startup
                TEXT 0 176 Left 2 ;.ac dec 10 1 1000
                """);
        assertEquals(List.of("V1"), model.getCircuitComponents().stream().map(CircuitModel.ComponentData::getName).toList());
        var issues = model.getConversionIssues();
        assertEquals(5, issues.size(), issues.toString());
        assertTrue(issues.stream().anyMatch(issue -> issue.code().equals("UNSUPPORTED_SYMBOL")
                && issue.component().equals("Q1") && issue.sourceLocation().equals("test.asc:2")));
        assertTrue(issues.stream().anyMatch(issue -> issue.code().equals("MALFORMED_ELEMENT")
                && issue.component().equals("Rbroken") && issue.sourceLocation().equals("test.asc:7")));
        assertTrue(issues.stream().anyMatch(issue -> issue.code().equals("UNSUPPORTED_TRANSIENT_OPTION")
                && issue.sourceLocation().equals("test.asc:13")));
        assertEquals(issues, roundTrip(model).getConversionIssues());
        assertEquals(1e-3, model.getSimulationDuration());
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTF-8", "UTF-16", "UTF-16LE", "UTF-16BE", "windows-1252"})
    void dispatchesAscFilesInNativeEncodings(String encoding) throws Exception {
        Path input = directory.resolve("sample.ASC");
        Path target = directory.resolve("sample.ipes");
        var source = fixture("rc.asc").replace("Cload=1u", "Cload=1\u00B5");
        Files.write(input, source.getBytes(Charset.forName(encoding)));
        var messages = new ByteArrayOutputStream();
        try (var stream = new PrintStream(messages)) {
            assertEquals(0, Main.run(new String[]{input.toString(), target.toString()}, stream, stream), messages.toString());
        }
        assertEquals(1e-6, component(new CircuitFileParser().parse(target.toString()), "C1").getNumericParameters()[0]);
    }

    @Test void cliWritesTheSupportedPartsAndReportsStructuredDiagnostics() throws Exception {
        Path input = directory.resolve("partial.asc");
        Path target = directory.resolve("partial.ipes");
        Files.writeString(input, fixture("rc.asc") + "\nSYMBOL custom 10 30 R0\nSYMATTR InstName U1\n");
        var messages = new ByteArrayOutputStream();
        try (var stream = new PrintStream(messages)) {
            assertEquals(2, Main.run(new String[]{input.toString(), target.toString()}, stream, stream));
        }
        assertTrue(messages.toString(StandardCharsets.UTF_8).contains("\"code\":\"UNSUPPORTED_SYMBOL\""));
        assertEquals(3, new CircuitFileParser().parse(target.toString()).getCircuitComponents().size());
    }

    @Test void checksStockSymbolsForTransientImportAndKeepsUnsupportedOptions() throws Exception {
        Path input = writeProfileFixture("");
        String source = Files.readString(input).replace("Value AC 1", "Value 1")
                .replaceFirst("(?m)!\\.ac[^\\r\\n]*", "!.tran 0 6m 1m 1u uic");
        Files.writeString(input, source);
        var importer = new LtspiceImporter();
        var model = importer.convert(input, directory);
        assertNull(model.getAcSettings());
        assertEquals(2, model.getAcProvenance().dependencySha256().size());
        assertEquals(2, model.getConversionIssues().stream()
                .filter(issue -> issue.code().equals("UNSUPPORTED_TRANSIENT_OPTION")).count());
        var profile = assertThrows(IllegalArgumentException.class, () -> importer.convert(input, directory, List.of(),
                AcInputOrigin.LTSPICE_26_1_1_NORMAL_27C));
        assertTrue(profile.getMessage().contains("requires a complete import"));

        Files.writeString(directory.resolve("ind.asy"), "Version 4\nSymbolType CELL\n");
        assertThrows(IllegalArgumentException.class, () -> importer.convert(input, directory));
    }

    @Test void reportsAnExplicitStockVerificationFindingForDiodes() throws Exception {
        Path input = writeProfileFixture("");
        String source = Files.readString(input).replace("Value AC 1", "Value 1")
                .replaceFirst("(?m)!\\.ac[^\\r\\n]*", "!.tran 1u 6m")
                + "\nSYMBOL diode 320 16 R0\nSYMATTR InstName D1\nSYMATTR Value D\n";
        Files.writeString(input, source);
        var model = new LtspiceImporter().convert(input, directory);
        assertTrue(model.getConversionIssues().stream().anyMatch(issue ->
                issue.code().equals("UNSUPPORTED_STOCK_SYMBOL_VERIFICATION") && issue.component().equals("D1")
                        && issue.sourceLocation().startsWith(input + ":")));
    }

    private record Trace(List<Double> times, List<Double> values) { }

    private Trace trace(CircuitModel model, String file, String component) throws Exception {
        Path path = directory.resolve(file);
        new CircuitFileWriter().write(model, path);
        var engine = new HeadlessSimulationEngine();
        var times = new ArrayList<Double>();
        var values = new ArrayList<Double>();
        engine.setStepTraceListener((time, potentials, currents, netlist) -> {
            int index = netlist.getElementIndexByName(component);
            times.add(time);
            values.add(potentials[netlist.getNodeX(index)] - potentials[netlist.getNodeY(index)]);
        });
        var result = engine.runSimulation(SimulationConfig.builder().circuitFile(path.toString()).build());
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return new Trace(times, values);
    }

    private void assertSimulationBlocked(CircuitModel model, String file) throws Exception {
        Path path = directory.resolve(file);
        new CircuitFileWriter().write(model, path);
        var result = new HeadlessSimulationEngine().runSimulation(SimulationConfig.builder().circuitFile(path.toString()).build());
        assertFalse(result.isSuccess());
    }

    private static String fixture(String name) throws Exception {
        try (var resource = LtspiceImporterTest.class.getResourceAsStream("/importer/" + name)) {
            return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static CircuitModel convert(String source) {
        return new LtspiceImporter().convert(source, "test.asc");
    }

    private static CircuitModel roundTrip(CircuitModel model) throws Exception {
        return new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(model)), "roundtrip.ipes");
    }

    private static CircuitModel.ComponentData component(CircuitModel model, String name) {
        return model.getCircuitComponents().stream().filter(c -> c.getName().equals(name)).findFirst().orElseThrow();
    }
}
