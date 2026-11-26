package gecko.core.ac;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitHierarchy;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitFileParser.CircuitParseException;
import gecko.core.io.CircuitTopology;
import gecko.core.io.HierarchyIndex;
import gecko.core.io.HierarchyMetadataCodec;
import gecko.core.io.ImportIssue;
import gecko.core.io.SavedCircuitBlock;
import gecko.core.io.importer.LtspiceImporter;
import gecko.core.io.importer.SpiceValue;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeAcHierarchyTest {
    @TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.NEVER) Path sources;
    private final HashMap<String, java.util.function.UnaryOperator<String>> edits = new HashMap<>();

    @ParameterizedTest
    @ValueSource(strings = {".tran 0 20m 0 100u", ".fra 10 1000", ".dc Vdrive 0 2 0.1", ""})
    void traversesNonAcRootsAndPersistsBoundChildValues(String analysis) throws Exception {
        change("root.asc", source -> source.replace("Value AC 1", "Value 1")
                .replace("first=1k", "base=2k next={base*2}")
                .replace("caller={first}", "caller={base}")
                .replace("caller=4k", "caller={next}")
                .replace("TEXT 40 256", """
                        SYMBOL res 560 32 R0
                        SYMATTR InstName Rmain
                        SYMATTR Value {base}
                        TEXT 40 256""")
                .replace("TEXT 40 272 Left 2 !.ac dec 2 10 100k",
                        analysis.isEmpty() ? "" : "TEXT 40 272 Left 2 !" + analysis));
        change("leaf.asc", source -> source.replace("series=3k shunt=2k", "shunt=2k"));
        var imported = fixture();
        assertEquals(2000, rootResistor(imported).getNumericParameters()[0]);
        var issues = imported.getConversionIssues();
        if (analysis.startsWith(".fra") || analysis.startsWith(".dc")) {
            assertEquals(1, issues.size(), issues.toString());
            assertEquals("UNSUPPORTED_DIRECTIVE", issues.getFirst().code());
            assertEquals(analysis.split(" ")[0], issues.getFirst().component());
            assertTrue(issues.getFirst().sourceLocation().matches(".*root\\.asc:[0-9]+"));
        } else {
            assertTrue(issues.isEmpty(), issues.toString());
        }
        var captured = new CircuitPreparation().capture(imported).toModel();
        var parsed = roundTrip(captured);
        assertEquals(2000, rootResistor(parsed).getNumericParameters()[0]);
        assertNull(parsed.getAcSettings());
        assertTrue(parsed.getCircuitComponents().stream().allMatch(component -> component.getAcComponent() == null));
        assertEquals(List.of(new ParameterDefinition("base", "2k"), new ParameterDefinition("next", "{base*2}")),
                parsed.getRootParameterDefinitions());
        assertTrue(parsed.getAcProvenance().dependencySha256().containsKey("leaf.asc"));
        if (analysis.startsWith(".tran")) {
            assertEquals(0.02, parsed.getSimulationDuration());
            assertEquals(0.0001, parsed.getTimeStep(), 1e-18);
        }
        var index = new HierarchyIndex(parsed);
        var scopes = index.parameterScopes(parsed.rootParameterMap());
        var topology = CircuitTopology.resolve(parsed);
        assertEquals(12, index.boundaries().size());
        var resistors = parsed.getCircuitComponents().stream().filter(c -> c.getName().equals("Rseries")).toList();
        assertEquals(2, resistors.size());
        assertEquals(1, resistors.stream()
                .map(resistor -> index.sheet(HierarchyIndex.parent(resistor.getSavedBlock())).definition().id())
                .distinct().count());
        for (var resistor : resistors) {
            long sheet = HierarchyIndex.parent(resistor.getSavedBlock());
            boolean first = index.path(sheet).equals(List.of("X1", "Xleaf"));
            double expected = first ? 2000 : 4000;
            assertEquals(expected, resistor.getNumericParameters()[0]);
            assertEquals(Map.of(0, "{series}"), resistor.getScalarBindings());
            assertEquals(List.of("series"), index.sheet(sheet).definition().requiredInputs().stream()
                    .map(CircuitHierarchy.RequiredInput::name).toList());
            assertEquals(expected, Double.parseDouble(scopes.get(sheet).get("series")));
            assertEquals("{caller}", index.sheet(sheet).call().overrides().getFirst().expression());
            assertEquals(first ? "{base}" : "{next}",
                    index.sheet(index.sheet(sheet).parent()).call().overrides().getFirst().expression());
            var input = index.boundaries().stream()
                    .filter(boundary -> boundary.sheet().id() == sheet && boundary.formal().name().equals("IN"))
                    .findFirst().orElseThrow().terminal();
            assertEquals(topology.group(resistor, CircuitTopology.Side.INPUT, 0).id(),
                    topology.group(input, CircuitTopology.Side.INPUT, 0).id());
        }
        if (analysis.startsWith(".tran")) {
            var first = resistors.getFirst();
            var second = resistors.getLast();
            var previous = parsed.getRootParameterDefinitions();
            assertThrows(IllegalArgumentException.class, () -> parsed.setRootParameterDefinitions(
                    List.of(new ParameterDefinition("base", "-1"), new ParameterDefinition("next", "4k"))));
            assertEquals(previous, parsed.getRootParameterDefinitions());
            assertEquals(2000, first.getNumericParameters()[0]);
            assertEquals(2000, rootResistor(parsed).getNumericParameters()[0]);
            parsed.setRootParameterDefinitions(List.of(new ParameterDefinition("base", "3k"),
                    new ParameterDefinition("next", "4k")));
            assertEquals(3000, first.getNumericParameters()[0]);
            assertEquals(4000, second.getNumericParameters()[0]);
            assertEquals(3000, rootResistor(parsed).getNumericParameters()[0]);
            var savedAgain = roundTrip(parsed);
            assertEquals(3000, rootResistor(savedAgain).getNumericParameters()[0]);
            assertEquals(List.of(3000.0, 4000.0), savedAgain.getCircuitComponents().stream()
                    .filter(component -> component.getName().equals("Rseries"))
                    .map(component -> component.getNumericParameters()[0]).toList());
            first.getNumericParameters()[1] = 1;
            assertThrows(IllegalArgumentException.class, () -> new CircuitFileWriter().write(parsed));
            first.getNumericParameters()[1] = 0;
            first.setScalarBindings(java.util.Map.of(0, "{series*1}"));
            assertThrows(IllegalArgumentException.class, () -> new CircuitFileWriter().write(parsed));
        }
    }

    private static CircuitModel.ComponentData rootResistor(CircuitModel model) {
        return model.getCircuitComponents().stream().filter(c -> c.getName().equals("Rmain")).findFirst().orElseThrow();
    }

    @Test void rejectsRemovingRootParameterUsedOnlyByOccurrence() throws Exception {
        change("root.asc", source -> source.replace("Value AC 1", "Value 1")
                .replace(".ac dec 2 10 100k", ".tran 1u 1m"));
        change("leaf.asc", source -> source.replace("Value {series}", "Value 3k").replace("Value {shunt}", "Value 2k"));
        var model = roundTrip(fixture());
        assertTrue(model.getCircuitComponents().stream().allMatch(component -> component.getScalarBindings().isEmpty()));
        var before = model.getRootParameterDefinitions();
        var beforeBytes = new CircuitFileWriter().write(model);
        assertThrows(IllegalArgumentException.class, () -> model.setRootParameterDefinitions(List.of()));
        assertEquals(before, model.getRootParameterDefinitions());
        assertArrayEquals(beforeBytes, new CircuitFileWriter().write(model));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PULSE(0 1 0 1u 1u 1m 2m)", "PWL(0 0 1m 1)"})
    void cliReportsChildWaveformResourcesWithTheirOccurrencePaths(String waveform) throws Exception {
        change("root.asc", source -> source.replace("Value AC 1", "Value 1")
                .replace(".ac dec 2 10 100k", ".tran 0 20m 0 100u"));
        change("leaf.asc", source -> source + """
                SYMBOL voltage 432 64 R0
                SYMATTR InstName Vchild
                SYMATTR Value %s
                """.formatted(waveform));
        fixture();
        var target = sources.resolve("partial.ipes");
        var output = new ByteArrayOutputStream();
        try (var messages = new PrintStream(output)) {
            assertEquals(2, gecko.core.io.importer.Main.run(new String[]{sources.resolve("root.asc").toString(),
                target.toString(), "--symbols", sources.toString(), "--source-root", sources.toString()},
                messages, messages), output.toString());
        }
        assertTrue(output.toString().contains("UNSUPPORTED_HIERARCHY_CONTROL"));
        var parsed = new CircuitFileParser().parse(target.toString());
        assertNull(parsed.getAcSettings());
        assertTrue(parsed.getResources().isEmpty());
        assertTrue(parsed.getCircuitComponents().stream().noneMatch(component -> component.getName().equals("Vchild")));
        var issues = parsed.getConversionIssues().stream()
                .filter(issue -> issue.code().equals("UNSUPPORTED_HIERARCHY_CONTROL")).toList();
        assertEquals(2, issues.size(), parsed.getConversionIssues().toString());
        assertTrue(issues.getFirst().sourceLocation().contains("leaf.asc [X1, Xleaf]:"));
        assertTrue(issues.getLast().sourceLocation().contains("leaf.asc [X2, Xleaf]:"));
    }

    @Test void rebindsRequiredInputsInTheImmediateParentAfterSaveAndRootEdit() throws Exception {
        var model = fixture(Map.of(
                "wrapper.asc", text -> text.replace("series={caller}", "SeRiEs={CaLlEr} ShUnT={sErIeS}")
                        .replace("caller=3k", "caller=3k series={offset} offset=2k"),
                "leaf.asc", text -> text.replace("series=3k shunt=2k", "caller=9k leafAlias={series}")));
        assertEquals(List.of(1000.0, 4000.0), seriesSlots(model));
        var parsed = roundTrip(model);
        assertEquals(model.getHierarchy(), parsed.getHierarchy());
        assertEquals(model.getSpecialComponents().stream().map(component -> component.getSavedBlock().tokens()
                        .get(HierarchyMetadataCodec.OCCURRENCE)).toList(),
                parsed.getSpecialComponents().stream().map(component -> component.getSavedBlock().tokens()
                        .get(HierarchyMetadataCodec.OCCURRENCE)).toList());
        var hierarchy = new HierarchyIndex(parsed);
        var root = parsed.getAcSettings();
        var scopes = hierarchy.parameterScopes(root.parameterMap());
        for (var owner : parsed.getSpecialComponents()) {
            long id = owner.getSavedBlock().uniqueObjectIdentifier();
            if (!owner.getName().equals("Xleaf")) continue;
            assertEquals(hierarchy.path(id).getFirst().equals("X1") ? "1000.0" : "4000.0", scopes.get(id).get("series"));
            assertEquals("2000.0", scopes.get(id).get("shunt"));
            assertEquals("{series}", scopes.get(id).get("leafalias"));
            assertEquals(Double.parseDouble(scopes.get(id).get("series")),
                    SpiceValue.evaluate("{leafalias}", scopes.get(id)));
        }
        var before = solve(parsed);
        assertEquals(12, before.hierarchy().ports().size());
        assertNotEquals(voltage(before, "X1:Xleaf:Csh", 1, 0), voltage(before, "X2:Xleaf:Csh", 1, 0));
        parsed.setAcSettings(new AcDocumentSettings(root.sweep(),
                List.of(new ParameterDefinition("FIRST", "5k")), root.analysisId()));
        var rebound = roundTrip(parsed);
        assertEquals(List.of(5000.0, 4000.0), seriesSlots(rebound));
        assertTrue(rebound.getCircuitComponents().stream().filter(component -> component.getName().equals("Rseries"))
                .allMatch(component -> ((AcComponent.Passive) component.getAcComponent().law()).expression().equals("{series}")));
        var after = solve(rebound);
        assertNotEquals(voltage(before, "X1:Xleaf:Csh", 1, 0), voltage(after, "X1:Xleaf:Csh", 1, 0));
        assertEquals(voltage(before, "X2:Xleaf:Csh", 1, 0), voltage(after, "X2:Xleaf:Csh", 1, 0));
    }

    private static List<Double> seriesSlots(CircuitModel model) {
        return model.getCircuitComponents().stream().filter(component -> component.getName().equals("Rseries"))
                .map(component -> component.getNumericParameters()[0]).toList();
    }

    @Test void evaluatesVccsInTwoOccurrencesOfOneDefinition() throws Exception {
        change("wrapper.asc", text -> text.replace("series={caller}", "series={caller} gm={caller/1meg}"));
        change("leaf.asc", text -> text.replace("shunt=2k", "shunt=2k gm=3m") + """
                FLAG 432 160 OUT
                FLAG 432 80 RET
                FLAG 384 96 IN
                FLAG 384 144 RET
                SYMBOL g 432 64 R0
                SYMATTR InstName G1
                SYMATTR Value {gm}
                """);
        var parsed = roundTrip(fixture());
        assertEquals(List.of(.001, .004), parsed.getCircuitComponents().stream()
                .filter(component -> component.getName().equals("G1"))
                .map(component -> component.getNumericParameters()[0]).toList());
        var result = solve(parsed);
        for (String occurrence : List.of("X1", "X2")) {
            String instance = occurrence + ":Xleaf:G1";
            double gm = occurrence.equals("X1") ? .001 : .004;
            assertEquals(0, voltage(result, instance, 3, 0).subtract(voltage(result, instance, 4, 0))
                    .scale(gm).subtract(current(result, instance, 0)).abs(), 1e-12);
        }
    }

    @Test void checksRequiredOverridesWhenReadingNativeAcData() throws Exception {
        var model = fixture(Map.of(
                "leaf.asc", text -> text.replace("series=3k shunt=2k", "shunt=2k") + """
                        SYMBOL voltage 432 64 R0
                        SYMATTR InstName Vbias
                        SYMATTR Value {VLOW}
                        """,
                "wrapper.asc", text -> text.replace("series={caller}", "series={caller} Vlow=100m")));
        var target = model.getSpecialComponents().stream().filter(component -> component.getName().equals("Xleaf"))
                .findFirst().orElseThrow();
        String original = target.getSavedBlock().tokens().get(HierarchyMetadataCodec.OCCURRENCE);
        var call = HierarchyMetadataCodec.decodeOccurrence(original);
        byte[] bytes = new CircuitFileWriter().write(model);
        String text;
        try (var gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            text = new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (String value : List.of("", "0", "-1")) {
            var error = assertThrows(CircuitParseException.class,
                    () -> parseNativeOverride(text, original, call, value, "100m"));
            assertTrue(error.getMessage().contains(value.isEmpty() ? "Missing required input series" : "positive passive"));
            assertTrue(error.getMessage().contains("[X1, Xleaf]"));
        }
        var parsed = parseNativeOverride(text, original, call, "{caller+2k}", "-1");
        assertEquals(List.of(3000.0, 4000.0), seriesSlots(parsed));
        var resistor = parsed.getCircuitComponents().stream().filter(c -> c.getName().equals("Rseries"))
                .findFirst().orElseThrow();
        assertEquals(3000.0, resistor.getParameters().get("param0"));
        assertEquals(3000.0, resistor.getParameters().get("resistance"));
        var voltage = parsed.getCircuitComponents().stream().filter(c -> c.getName().equals("Vbias"))
                .findFirst().orElseThrow();
        assertEquals(401, voltage.getNumericParameters()[0]);
        assertEquals(-1, voltage.getNumericParameters()[1]);
        assertEquals(401.0, voltage.getParameters().get("param0"));
        assertEquals(-1.0, voltage.getParameters().get("param1"));
        assertEquals(List.of(3000.0, 4000.0), seriesSlots(roundTrip(parsed)));

        assertTruncatedNativeSlot(text, "Vbias", "401", 1);
        assertTruncatedNativeSlot(text, "Rseries", "null", 0);
        assertTruncatedNativeSlot(text, "Rseries", "", 0);
    }

    private static void assertTruncatedNativeSlot(String text, String member, String values, int slot) {
        int start = text.indexOf("\nidStringDialog " + member + "\n");
        assertTrue(start >= 0);
        int parameter = text.indexOf("\nparameter[] ", start);
        int end = text.indexOf('\n', parameter + 1);
        assertTrue(parameter > start && end < text.indexOf("<\\ElementLK>", start));
        String edited = text.substring(0, parameter) + "\nparameter[] " + values + text.substring(end);
        var error = assertThrows(CircuitParseException.class, () ->
                new CircuitFileParser().parse(new BufferedReader(new StringReader(edited)), "truncated.ipes"));
        assertTrue(error.getMessage().contains("Require compatibility slot " + slot + " on " + member));
        assertTrue(error.getMessage().contains("[X1, Xleaf]"));
    }

    private static CircuitModel parseNativeOverride(String text, String original, CircuitHierarchy.Occurrence call,
                                                    String series, String voltage) throws CircuitParseException, java.io.IOException {
        var overrides = new java.util.ArrayList<ParameterDefinition>();
        if (!series.isEmpty()) overrides.add(new ParameterDefinition("series", series));
        overrides.add(new ParameterDefinition("Vlow", voltage));
        var edit = new CircuitHierarchy.Occurrence(call.definitionId(), call.instance(), overrides);
        int at = text.indexOf(original);
        assertTrue(at >= 0);
        String edited = text.substring(0, at) + HierarchyMetadataCodec.encode(edit) + text.substring(at + original.length());
        return new CircuitFileParser().parse(new BufferedReader(new StringReader(edited)), "edited.ipes");
    }

    @Test void requiresFinitePositiveOccurrencePassivesBeforePublishingOrSaving() throws Exception {
        var model = fixture(Map.of("leaf.asc", text -> text.replace("series=3k shunt=2k", "shunt=2k")));
        var root = model.getAcSettings();
        var target = sources.resolve("existing.ipes");
        var writer = new CircuitFileWriter();
        writer.write(model, target);
        byte[] original = Files.readAllBytes(target);
        for (String invalid : List.of("0", "-1")) {
            model.setAcSettings(new AcDocumentSettings(root.sweep(),
                    List.of(new ParameterDefinition("FIRST", invalid)), root.analysisId()));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> writer.write(model, target))
                    .getMessage().contains("positive passive"));
            assertArrayEquals(original, Files.readAllBytes(target));
            assertEquals(List.of(1000.0, 4000.0), seriesSlots(model));
        }
        for (String invalid : List.of("{unknown}", "1e999", "")) {
            String assignment = invalid.isEmpty() ? "" : "series=" + invalid;
            Files.writeString(sources.resolve("wrapper.asc"),
                    fixtureText("wrapper.asc").replace("series={caller}", assignment));
            assertThrows(IllegalArgumentException.class,
                    () -> new LtspiceImporter().convert(sources.resolve("root.asc"), sources, List.of(sources)));
        }
    }

    @Test void validatesCapacitanceAndUnrelatedDeclaredDefaultsInRequiredInputDefinitions() throws Exception {
        var model = fixture(Map.of(
                "leaf.asc", text -> text.replace("series=3k shunt=2k", "shunt=2k")
                        .replace("SYMATTR Value 10n", "SYMATTR Value {filterCap}"),
                "wrapper.asc", text -> text.replace("series={caller}", "series={caller} filterCap=10n")));
        var writer = new CircuitFileWriter();
        for (String invalid : List.of("0", "-1")) {
            Files.writeString(sources.resolve("wrapper.asc"),
                    fixtureText("wrapper.asc").replace("series={caller}", "series={caller} filterCap=" + invalid));
            var invalidImport = new LtspiceImporter().convert(sources.resolve("root.asc"), sources, List.of(sources));
            assertTrue(invalidImport.getConversionIssues().stream().anyMatch(issue ->
                    issue.component().equals("Csh") && issue.message().contains("positive passive component value on Csh")));
            assertFalse(invalidImport.getCircuitComponents().stream().anyMatch(component -> component.getName().equals("Csh")));
            for (var component : model.getCircuitComponents()) {
                if (!component.getName().equals("Csh")) continue;
                var law = component.getAcComponent();
                component.setAcComponent(new AcComponent(law.instance(), law.spiceOrders(), law.aliases(),
                        law.mirrored(), new AcComponent.Passive(invalid)));
            }
            assertTrue(assertThrows(IllegalArgumentException.class, () -> writer.write(model))
                    .getMessage().contains("positive passive component value on Csh"));
        }
        Files.writeString(sources.resolve("wrapper.asc"), fixtureText("wrapper.asc"));
        Files.writeString(sources.resolve("leaf.asc"),
                fixtureText("leaf.asc").replace("series=3k shunt=2k", "shunt=-1"));
        var invalidDefault = new LtspiceImporter().convert(sources.resolve("root.asc"), sources, List.of(sources));
        assertTrue(invalidDefault.getConversionIssues().stream().anyMatch(issue ->
                issue.component().equals("Rsh") && issue.message().contains("positive passive")));
        assertFalse(invalidDefault.getCircuitComponents().stream().anyMatch(component -> component.getName().equals("Rsh")));

        Files.writeString(sources.resolve("leaf.asc"),
                fixtureText("leaf.asc").replace("series=3k shunt=2k", "shunt=2k")
                        .replace("Value {shunt}", "Value {1+}"));
        var malformed = new LtspiceImporter().convert(sources.resolve("root.asc"), sources, List.of(sources));
        assertTrue(malformed.getConversionIssues().stream().anyMatch(issue ->
                issue.component().equals("Rsh") && issue.code().equals("MALFORMED_ELEMENT")));
        assertFalse(malformed.getCircuitComponents().stream().anyMatch(component -> component.getName().equals("Rsh")));
    }

    @Test void evaluatesOneProfiledDefinitionInDistinctOccurrenceScopes() throws Exception {
        change("leaf.asc", source -> source.replace("SYMBOL res 224 64 R0", "SYMBOL ind 224 64 R0")
                .replace("InstName Rsh\nSYMATTR Value {shunt}",
                        "InstName Lsh\nSYMATTR Value 1u\nSYMATTR SpiceLine Rser={series-1k}"));
        fixture();
        var origin = AcInputOrigin.LTSPICE_26_1_1_NORMAL_27C;
        var model = roundTrip(new LtspiceImporter().convert(sources.resolve("root.asc"), sources, List.of(sources), origin));
        assertEquals(2, model.getAcProvenance().memberSha256().size());
        assertEquals(2, model.getAcProvenance().definitionSourceSha256().size());
        var result = solve(model);
        for (int occurrence = 1; occurrence <= 2; occurrence++) {
            String instance = "X" + occurrence + ":Xleaf:Lsh";
            Complex impedance = voltage(result, instance, 1, 0).subtract(voltage(result, instance, 2, 0))
                    .divide(current(result, instance, 0));
            assertEquals(occurrence == 1 ? 0 : 3000, impedance.real(), 1e-8);
            assertEquals(2 * Math.PI * result.frequencyHz().getFirst() * 1e-6, impedance.imaginary(), 1e-10);
        }
        var settings = model.getAcSettings();
        model.setAcSettings(new AcDocumentSettings(settings.sweep(),
                List.of(new ParameterDefinition("first", "990")), settings.analysisId()));
        var preparation = new CircuitPreparation();
        assertFalse(preparation.prepare(preparation.capture(model), PreparationRequest.defaults()).report().readyForHeadless());
    }

    @Test void resolvesNestedParametersAndOrderedSignedPortsAfterNativeRoundTrip() throws Exception {
        CircuitModel imported = fixture();
        assertTrue(imported.getConversionIssues().isEmpty(), imported.getConversionIssues().toString());
        var saved = new CircuitFileWriter().write(imported);
        CircuitModel parsed = new CircuitFileParser().parse(new ByteArrayInputStream(saved), "detached.ipes");
        var ac = solve(parsed);
        assertEquals(12, ac.hierarchy().ports().size());
        checkPartitions(ac);
        for (int instance = 1; instance <= 2; instance++) {
            String wrapper = "X" + instance;
            assertEquals(List.of("RET", "IN", "OUT"), formals(ac, List.of(wrapper)));
            assertEquals(List.of("IN", "OUT", "RET"), formals(ac, List.of(wrapper, "Xleaf")));
            double series = instance == 1 ? 1000 : 4000;
            for (int i = 0; i < ac.frequencyHz().size(); i++) {
                double omega = 2 * Math.PI * ac.frequencyHz().get(i);
                var expected = new Complex(2000, 0).divide(new Complex(series + 2000, omega * series * 2000 * 1e-8));
                var input = Complex.ONE.subtract(expected).scale(1 / series);
                check(expected, voltage(ac, wrapper + ":Xleaf:Csh", 1, i));
                check(input, current(ac, "Vret" + instance, i));
                for (var path : List.of(List.of(wrapper), List.of(wrapper, "Xleaf"))) {
                    check(input, port(ac, path, "IN", i));
                    check(input.scale(-1), port(ac, path, "RET", i));
                    check(Complex.ZERO, port(ac, path, "OUT", i));
                }
            }
        }
        assertNotEquals(group(ac, "X1:Xleaf:Raux", 2).id(), group(ac, "X2:Xleaf:Raux", 2).id());
        assertNotEquals(group(ac, "Vdrive", 1).id(), group(ac, "X1:Xleaf:Raux", 2).id());
        assertEquals(group(ac, "Vret1", 2).id(), group(ac, "Vret2", 2).id());
    }

    private record Terminal(List<String> path, String instance, int order, AcResult.TerminalKind kind) { }
    private record Partition(boolean ground, Set<Terminal> members) { }

    private static void checkPartitions(AcResult ac) {
        var drive = new java.util.HashSet<Terminal>();
        drive.add(primitive(List.of(), "Vdrive", 1));
        var finalGroups = new java.util.HashSet<Partition>();
        var localGroups = new java.util.HashSet<Partition>();
        var rootGround = new Partition(true, Set.of(primitive(List.of(), "Vdrive", 2),
                primitive(List.of(), "Vret1", 2), primitive(List.of(), "Vret2", 2)));
        finalGroups.add(rootGround);
        localGroups.add(rootGround);
        localGroups.add(new Partition(false, Set.of(primitive(List.of(), "Vdrive", 1),
                pin(List.of(), "X1", 2), pin(List.of(), "X2", 2))));
        for (int i = 1; i <= 2; i++) {
            String instance = "X" + i;
            var wrapper = List.of(instance);
            var leaf = List.of(instance, "Xleaf");
            var rootRet = Set.of(primitive(List.of(), "Vret" + i, 1), pin(List.of(), instance, 1));
            var rootOut = Set.of(pin(List.of(), instance, 3));
            var wrapperIn = Set.of(boundary(wrapper, "IN", 2), pin(wrapper, "Xleaf", 1));
            var wrapperOut = Set.of(boundary(wrapper, "OUT", 3), pin(wrapper, "Xleaf", 2));
            var wrapperRet = Set.of(boundary(wrapper, "RET", 1), pin(wrapper, "Xleaf", 3));
            var leafIn = Set.of(boundary(leaf, "IN", 1), primitive(leaf, "Rseries", 1));
            var leafOut = Set.of(boundary(leaf, "OUT", 2), primitive(leaf, "Rseries", 2), primitive(leaf, "Csh", 1),
                    primitive(leaf, "Rsh", 1), primitive(leaf, "Raux", 1));
            var leafRet = Set.of(boundary(leaf, "RET", 3), primitive(leaf, "Csh", 2), primitive(leaf, "Rsh", 2));
            var leafInternal = Set.of(primitive(leaf, "Raux", 2));
            for (var group : List.of(rootRet, rootOut, wrapperIn, wrapperOut, wrapperRet, leafIn, leafOut, leafRet, leafInternal)) {
                localGroups.add(new Partition(false, group));
            }
            drive.add(pin(List.of(), instance, 2));
            drive.addAll(wrapperIn);
            drive.addAll(leafIn);
            finalGroups.add(new Partition(false, union(rootRet, wrapperRet, leafRet)));
            finalGroups.add(new Partition(false, union(rootOut, wrapperOut, leafOut)));
            finalGroups.add(new Partition(false, leafInternal));
        }
        finalGroups.add(new Partition(false, Set.copyOf(drive)));
        assertEquals(finalGroups, ac.hierarchy().groups().stream().map(group -> partition(group.ground(), group.members()))
                .collect(java.util.stream.Collectors.toSet()));
        assertEquals(localGroups, ac.hierarchy().localGroups().stream().map(group -> partition(group.ground(), group.members()))
                .collect(java.util.stream.Collectors.toSet()));
    }

    private static Partition partition(boolean ground, List<AcResult.SourceMember> members) {
        return new Partition(ground, members.stream().map(member -> new Terminal(
                member.occurrencePath(), member.instance(), member.spiceOrder(), member.kind()))
                .collect(java.util.stream.Collectors.toSet()));
    }

    private static Terminal primitive(List<String> path, String name, int order) {
        return new Terminal(path, name, order, AcResult.TerminalKind.PRIMITIVE);
    }

    private static Terminal pin(List<String> path, String name, int order) {
        return new Terminal(path, name, order, AcResult.TerminalKind.PARENT_PIN);
    }

    private static Terminal boundary(List<String> path, String name, int order) {
        return new Terminal(path, name, order, AcResult.TerminalKind.CHILD_BOUNDARY);
    }

    private static Set<Terminal> union(Set<Terminal> root, Set<Terminal> wrapper, Set<Terminal> leaf) {
        var result = new java.util.HashSet<>(root);
        result.addAll(wrapper);
        result.addAll(leaf);
        return Set.copyOf(result);
    }

    @Test void retainsForeignConversionEvidenceForARecognizedNativeContainer() throws Exception {
        var model = roundTrip(fixture());
        var owner = model.getSpecialComponents().getFirst();
        assertTrue(model.getConversionIssues().isEmpty());
        model.addConversionIssue(new ImportIssue("UNSUPPORTED_SIMULATION_BLOCK", "SPECIAL", owner.getName(),
                "The simulation block is unsupported by the headless engine.", owner.getSourceLocation(), true));
        var parsed = roundTrip(model);
        assertEquals(model.getConversionIssues(), parsed.getConversionIssues());
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(parsed), PreparationRequest.defaults());
        assertFalse(result.report().readyForHeadless());
        assertTrue(result.report().diagnostics().stream().anyMatch(issue -> issue.code().equals("UNSUPPORTED_SIMULATION_BLOCK")
                && issue.sourceLocation().equals(owner.getSourceLocation())));
    }

    @Test void rejectsDivergentPeerBodiesBeforeNativeSave() throws Exception {
        var model = fixture();
        model.getCircuitComponents().stream().filter(component -> component.getName().equals("Rsh")).findFirst()
                .orElseThrow().getPosition()[0]++;
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new CircuitFileWriter().write(model))
                .getMessage().contains("Divergent copies"));
    }

    @Test void reportsDuplicatePinOrders() throws Exception {
        change("wrapper.asy", text -> text.replace("SpiceOrder 2", "SpiceOrder 1"));
        var model = fixture();
        assertTrue(model.getConversionIssues().stream().anyMatch(issue -> issue.message().contains("unique contiguous SpiceOrder")
                && issue.sourceLocation().contains("root.asc")));
    }

    @Test void rejectsMissingNamedFlag() {
        change("leaf.asc", text -> text.replace("FLAG 160 176 RET", "FLAG 160 176 different"));
        assertTrue(assertThrows(IllegalArgumentException.class, this::fixture).getMessage().contains("Missing named FLAG RET"));
    }

    @Test void rejectsActiveDefinitionRecursion() {
        change("wrapper.asc", text -> text.replace("SYMBOL leaf", "SYMBOL wrapper"));
        assertTrue(assertThrows(IllegalArgumentException.class, this::fixture).getMessage().contains("Recursive ASC definition"));
    }

    @Test void retainsUnsupportedDirectiveWithOccurrenceSource() throws Exception {
        change("leaf.asc", text -> text + "TEXT 0 264 Left 2 !.include remote.lib\n");
        var model = roundTrip(fixture());
        assertTrue(model.getConversionIssues().stream().anyMatch(issue -> issue.component().equals(".include")
                && issue.sourceLocation().contains("leaf.asc") && issue.sourceLocation().contains("X2, Xleaf")));
        var preparation = new CircuitPreparation();
        assertFalse(preparation.prepare(preparation.capture(model), PreparationRequest.defaults()).report().readyForHeadless());
    }

    @Test void rejectsAnUnresolvedElectricalAsyAttribute() throws Exception {
        change("leaf.asy", text -> text + "SYMATTR ModelFile alternate.lib\n");
        var model = fixture();
        assertTrue(model.getConversionIssues().stream().anyMatch(issue -> issue.message().contains("ModelFile")
                && issue.message().contains("leaf.asy") && issue.sourceLocation().contains("X1")));
    }

    @Test void recordsDistinctCallerRelativeDependenciesWithTheSameLiteralSpelling() throws Exception {
        for (String name : List.of("res.asy", "cap.asy", "voltage.asy")) {
            Files.writeString(sources.resolve(name), fixtureText(name));
        }
        for (String directory : List.of("a", "b")) {
            var destination = Files.createDirectory(sources.resolve(directory));
            for (String name : List.of("wrapper.asc", "wrapper.asy", "leaf.asc", "leaf.asy")) {
                String text = fixtureText(name);
                if (directory.equals("b") && name.equals("leaf.asc")) text = text.replace("shunt=2k", "shunt=5k");
                Files.writeString(destination.resolve(name), text);
            }
        }
        var root = Files.writeString(sources.resolve("root.asc"), fixtureText("root.asc")
                .replace("SYMBOL wrapper 240", "SYMBOL a/wrapper 240")
                .replace("SYMBOL wrapper 480", "SYMBOL b/wrapper 480"));
        var model = new LtspiceImporter().convert(root, sources, List.of(sources));
        assertTrue(model.getConversionIssues().isEmpty(), model.getConversionIssues().toString());
        var hashes = model.getAcProvenance().dependencySha256();
        assertTrue(hashes.keySet().containsAll(Set.of("a/leaf.asc", "b/leaf.asc", "a/leaf.asy", "b/leaf.asy")));
        assertNotEquals(hashes.get("a/leaf.asc"), hashes.get("b/leaf.asc"));
        assertEquals(12, solve(roundTrip(model)).hierarchy().ports().size());
    }

    @Test void rejectsASwappedFormalMemberKey() throws Exception {
        var model = fixture();
        model.setHierarchy(new CircuitHierarchy(model.getHierarchy().definitions().stream().map(definition -> {
            if (!definition.source().endsWith("leaf.asc")) return definition;
            var formals = new java.util.ArrayList<>(definition.formals());
            var first = formals.get(0);
            var second = formals.get(1);
            formals.set(0, new CircuitHierarchy.Formal(first.order(), first.name(), second.boundaryMemberKey()));
            formals.set(1, new CircuitHierarchy.Formal(second.order(), second.name(), first.boundaryMemberKey()));
            return new CircuitHierarchy.Definition(definition.id(), definition.source(), formals, definition.defaults());
        }).toList()));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new CircuitFileWriter().write(model))
                .getMessage().contains("boundary in"));
    }

    @Test void rejectsTwoFormalPinsAtTheSameParentPosition() throws Exception {
        var model = fixture();
        var boundaries = model.getCircuitComponents().stream().filter(component -> component.getType() == 29).limit(2).toList();
        String key = HierarchyMetadataCodec.BOUNDARY_LOCATION;
        updateToken(boundaries.get(1), key, boundaries.get(0).getSavedBlock().tokens().get(key));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new CircuitFileWriter().write(model))
                .getMessage().contains("Duplicate parent pin position"));
    }

    @Test void rejectsAnOccurrenceFieldOnAnElectricalComponent() throws Exception {
        var model = fixture();
        String key = HierarchyMetadataCodec.OCCURRENCE;
        String occurrence = model.getSpecialComponents().getFirst().getSavedBlock().tokens().get(key);
        updateToken(model.getCircuitComponents().stream().filter(component -> component.getName().equals("Rseries"))
                .findFirst().orElseThrow(), key, occurrence);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new CircuitFileWriter().write(model))
                .getMessage().contains("Unsupported hierarchy field on ElementLK"));
    }

    private static void updateToken(CircuitModel.ComponentData component, String key, String value) {
        var saved = component.getSavedBlock();
        var fields = new java.util.LinkedHashMap<>(saved.tokens());
        fields.put(key, value);
        component.setSavedBlock(SavedCircuitBlock.parse(saved.tag(), fields.entrySet().stream()
                .map(entry -> entry.getKey() + " " + entry.getValue()).toArray(String[]::new)));
    }

    private CircuitModel roundTrip(CircuitModel model) throws Exception {
        return new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(model)), "detached.ipes");
    }

    private void change(String name, java.util.function.UnaryOperator<String> mutation) {
        edits.put(name, mutation);
    }

    private CircuitModel fixture() throws Exception {
        return fixture(edits);
    }

    private CircuitModel fixture(Map<String, java.util.function.UnaryOperator<String>> mutations) throws Exception {
        for (String name : List.of("leaf.asc", "leaf.asy", "wrapper.asc", "wrapper.asy", "root.asc",
                "res.asy", "cap.asy", "voltage.asy", "ind.asy", "g.asy")) {
            String text = fixtureText(name);
            Files.writeString(sources.resolve(name), mutations.getOrDefault(name, java.util.function.UnaryOperator.identity()).apply(text),
                    java.nio.file.StandardOpenOption.CREATE_NEW);
        }
        return new LtspiceImporter().convert(sources.resolve("root.asc"), sources, List.of(sources));
    }

    private String fixtureText(String name) throws java.io.IOException {
        try (var input = java.util.Objects.requireNonNull(getClass().getResourceAsStream("/ltspice-hierarchy/" + name))) {
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    static AcResult solve(CircuitModel model) {
        var preparation = new CircuitPreparation();
        var prepared = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        return assertInstanceOf(PreparationResult.Ready.class, prepared, prepared.report().diagnostics().toString())
                .execution().acPlan().execute();
    }

    private static List<String> formals(AcResult ac, List<String> path) {
        return ac.hierarchy().ports().stream().filter(port -> port.internal().occurrencePath().equals(path))
                .map(port -> port.internal().instance()).toList();
    }

    private static Complex port(AcResult ac, List<String> path, String formal, int sample) {
        return ac.hierarchy().ports().stream().filter(port -> port.internal().occurrencePath().equals(path)
                && port.internal().instance().equals(formal)).findFirst().orElseThrow().currentIntoChildA().get(sample);
    }

    private static AcResult.VoltageTrace group(AcResult ac, String name, int order) {
        return ac.groups().stream().filter(trace -> trace.members().contains(new AcResult.Member(name, order)))
                .findFirst().orElseThrow();
    }

    private static Complex voltage(AcResult ac, String name, int order, int sample) {
        return group(ac, name, order).voltageV().get(sample);
    }

    private static Complex current(AcResult ac, String name, int sample) {
        return ac.currents().stream().filter(trace -> trace.instance().equals(name)).findFirst().orElseThrow()
                .currentA().get(sample);
    }

    private static void check(Complex expected, Complex actual) {
        assertTrue(expected.subtract(actual).abs() <= 1e-12 + 1e-9 * expected.abs(), expected + " != " + actual);
    }
}
