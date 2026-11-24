package gecko.core.ac;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitHierarchy;
import gecko.core.io.CircuitModel;
import gecko.core.io.HierarchyMetadataCodec;
import gecko.core.io.ImportIssue;
import gecko.core.io.SavedCircuitBlock;
import gecko.core.io.importer.LtspiceImporter;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeAcHierarchyTest {
    @TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.NEVER) Path sources;
    private String changedSource;
    private java.util.function.UnaryOperator<String> edit;

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
        changedSource = name;
        edit = mutation;
    }

    private CircuitModel fixture() throws Exception {
        for (String name : List.of("leaf.asc", "leaf.asy", "wrapper.asc", "wrapper.asy", "root.asc",
                "res.asy", "cap.asy", "voltage.asy")) {
            String text = fixtureText(name);
            Files.writeString(sources.resolve(name), name.equals(changedSource) ? edit.apply(text) : text,
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
