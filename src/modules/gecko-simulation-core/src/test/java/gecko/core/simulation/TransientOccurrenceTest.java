package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitFileWriter;
import gecko.core.io.CircuitModel;
import gecko.core.io.HierarchyIndex;
import gecko.core.io.SavedCircuitBlock;
import gecko.core.io.importer.LtspiceImporter;
import gecko.core.io.importer.SpiceImporter;
import gecko.core.preparation.CircuitPreparation;
import gecko.core.preparation.ExecutionOptions;
import gecko.core.preparation.PreparationRequest;
import gecko.core.preparation.PreparationResult;
import gecko.core.preparation.SolverOverrides;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class TransientOccurrenceTest {
    private static final double CAPACITANCE = 10e-9;
    private static final double SHUNT_RESISTANCE = 2000;
    private static final double INITIAL_VOLTAGE = 1;
    private static final double SUPPLY = 5;
    private static final double STEP = 1e-6;
    private static final String R1 = "X1:Xleaf:Rseries";
    private static final String R2 = "X2:Xleaf:Rseries";
    private static final String C1 = "X1:Xleaf:Csh";
    private static final String C2 = "X2:Xleaf:Csh";

    @TempDir(cleanup = org.junit.jupiter.api.io.CleanupMode.NEVER) Path inputs;

    @Test
    void savedNativeAscExecutesEachOccurrenceWithItsOwnParametersAndSignedCurrents() throws Exception {
        var ac = importFixture(Files.createDirectory(inputs.resolve("ac")), false);
        assertReturnConnection(ac);
        var preparation = new CircuitPreparation();
        assertTrue(preparation.configurationFields(preparation.capture(ac)).stream()
                .anyMatch(field -> field.component().equals("Rseries")));
        ac.setAcSettings(null);
        assertUnsupported(ac, Map.of());
        var model = importTransient();
        assertTrue(model.getConversionIssues().isEmpty(), model.getConversionIssues().toString());
        var probe = model.getControlComponents().stream().filter(component -> component.getType() == 1)
                .findFirst().orElseThrow();
        // This label appears on a root occurrence pin, with no root electrical stamp.
        probe.setParameterStrings(new String[]{"outA", "0", "0"});
        var writer = new CircuitFileWriter();
        byte[] saved = writer.write(model);
        var reopened = new CircuitFileParser().parse(new ByteArrayInputStream(saved), "nested-rc.ipes");
        assertEquals(2, reopened.getCircuitComponents().stream().filter(c -> c.getName().equals("Rseries")).count());
        assertNotNull(reopened.getHierarchy());
        byte[] reopenedBytes = writer.write(reopened);
        var members = nativeIdentifiers(reopened);
        assertEquals(nativeIdentifiers(model), members);
        assertSlots(reopened, 1000, 4000);

        var baseline = ready(reopened, Map.of());
        var overridden = ready(reopened, Map.of(R1 + ".resistance", 2000.0));
        assertTrue(preparation.configurationFields(baseline.circuit()).stream()
                .anyMatch(field -> field.component().equals(R1)));
        ready(reopened, Map.of("Vdrive.sourceType", 0.0));
        String observedSignal = probe.getTerminalYLabels()[0];
        var reopenedProbe = reopened.getControlComponents().stream().filter(c -> c.getName().equals(probe.getName()))
                .findFirst().orElseThrow();
        assertArrayEquals(new String[]{"outA", "0", "0"}, reopenedProbe.getParameterStrings());
        var topology = gecko.core.io.CircuitTopology.resolve(reopened);
        var electrical = gecko.core.circuit.netlist.NetlistBuilder.buildFromCircuitModel(reopened);
        int capacitor = electrical.getElementIndexByName(C1);
        var rootOut = topology.labelGroup("LK", 0, "outA");
        var rootGround = topology.labelGroup("LK", 0, "0");
        var childOut = topology.group(component(reopened, new HierarchyIndex(reopened), C1),
                gecko.core.io.CircuitTopology.Side.INPUT, 0);
        assertEquals(childOut, rootOut, "Root boundary outA must share the first capacitor node");
        assertEquals(electrical.getNodeX(capacitor), electrical.getHierarchyGroupNode(rootOut));
        assertEquals(0, electrical.getHierarchyGroupNode(rootGround));
        int firstReturn = electrical.getElementIndexByName("Vret1");
        int drive = electrical.getElementIndexByName("Vdrive");
        int resistor = electrical.getElementIndexByName(R1);
        assertEquals(electrical.getNodeX(firstReturn), electrical.getNodeY(capacitor), "Return incidence");
        assertEquals(0, electrical.getNodeY(firstReturn), "Return supply negative terminal");
        assertEquals(electrical.getNodeX(drive), electrical.getNodeX(resistor), "Drive incidence");
        assertEquals(electrical.getNodeY(resistor), electrical.getNodeX(capacitor), "RC junction incidence");
        assertSlots(overridden.circuit().toModel(), 2000, 4000);
        assertSlots(reopened, 1000, 4000);
        assertEquals(members, nativeIdentifiers(overridden.circuit().toModel()));

        var flat = new SpiceImporter().convert("""
                Vdrive in 0 5
                RA in outA 1k
                CA outA 0 10n IC=1
                RSA outA 0 2k
                RB in outB 4k
                CB outB 0 10n IC=1
                RSB outB 0 2k
                .tran 1u 8u
                """, "flat-rc.cir");
        List<double[]> flatTrace = trace(ready(flat, Map.of()), "RA", "RB", "CA", "CB", null);
        List<double[]> hierarchyTrace = trace(baseline, R1, R2, C1, C2, observedSignal);
        List<double[]> overrideTrace = trace(overridden, R1, R2, C1, C2, observedSignal);
        verifyBackwardEuler(hierarchyTrace, 1000, 4000);
        verifyBackwardEuler(overrideTrace, 2000, 4000);
        for (int i = 0; i < hierarchyTrace.size(); i++) {
            assertArrayEquals(flatTrace.get(i), hierarchyTrace.get(i), 1e-10);
            assertEquals(hierarchyTrace.get(i)[3], overrideTrace.get(i)[3], 1e-10);
            assertEquals(hierarchyTrace.get(i)[4], overrideTrace.get(i)[4], 1e-10);
        }

        assertArrayEquals(reopenedBytes, writer.write(reopened));
        var invalid = preparation.prepare(preparation.capture(reopened),
                new PreparationRequest(solver(), Map.of("Rseries.resistance", 2000.0)));
        assertInstanceOf(PreparationResult.Rejected.class, invalid);
        probe.setParameterStrings(new String[]{"IN", "0", "0"});
        var unresolved = preparation.prepare(preparation.capture(model), new PreparationRequest(solver(), Map.of()));
        assertTrue(unresolved.report().diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_PROBE_REFERENCE")),
                unresolved.report().diagnostics().toString());
    }

    enum UnsupportedChange {
        INDUCTOR, NON_CONSTANT_SOURCE, UNCAPTURED_RESOURCE, UIC, DISABLED_COMPONENT, DISABLED_OCCURRENCE,
        CHILD_PROBE, ROOT_CONTROL_ALGORITHM, ROOT_LEGACY_CONTAINER
    }

    @ParameterizedTest
    @EnumSource(UnsupportedChange.class)
    void keepsUnsupportedSavedHierarchyConfigurationsOutOfTransientExecution(UnsupportedChange change)
            throws Exception {
        var model = roundTrip(importTransient());
        var hierarchy = new HierarchyIndex(model);
        switch (change) {
            case INDUCTOR -> {
                var inductor = new CircuitModel.ComponentData(2, "Lload");
                inductor.setNumericParameters(new double[]{1e-6, 0, 0, 0, 0});
                inductor.setTerminalXLabels(new String[]{"outA"});
                inductor.setTerminalYLabels(new String[]{"0"});
                model.getCircuitComponents().add(inductor);
            }
            case NON_CONSTANT_SOURCE -> {
                var source = component(model, hierarchy, "Vdrive");
                var values = source.getNumericParameters().clone();
                values[0] = 402;
                source.setNumericParameters(values);
            }
            case UNCAPTURED_RESOURCE -> model.getResourceFailures().put(7L,
                    new gecko.core.io.CircuitResourceFailure(7, "waveform.txt", List.of(), "Resource bytes unavailable"));
            case UIC -> model.setTransientUic(true);
            case DISABLED_COMPONENT -> replaceSavedToken(component(model, hierarchy, R1), "enabledShorted", "0");
            case DISABLED_OCCURRENCE -> replaceSavedToken(model.getSpecialComponents().getFirst(), "enabledShorted", "0");
            case CHILD_PROBE -> replaceSavedToken(model.getControlComponents().getFirst(), "parentSheetIdentifier",
                    Long.toString(model.getSpecialComponents().getFirst().getSavedBlock().uniqueObjectIdentifier()));
            case ROOT_CONTROL_ALGORITHM -> model.getControlComponents().add(new CircuitModel.ComponentData(4, "SIGNAL.test"));
            case ROOT_LEGACY_CONTAINER -> model.getSpecialComponents().add(new CircuitModel.ComponentData(27, "LocalSheet"));
        }
        assertUnsupported(model, Map.of());
    }

    @Test
    void doesNotAdmitAConstantSourceSwitchedToAWaveformByAnOverride() throws Exception {
        assertUnsupported(roundTrip(importTransient()), Map.of("Vdrive.sourceType", 1.0));
    }

    private static void assertUnsupported(CircuitModel model, Map<String, Double> overrides) {
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), new PreparationRequest(solver(), overrides));
        assertInstanceOf(PreparationResult.Rejected.class, result);
        assertTrue(result.report().diagnostics().stream().anyMatch(d -> d.code().equals("UNSUPPORTED_TRANSIENT_LAW")),
                result.report().diagnostics().toString());
    }

    private static CircuitModel roundTrip(CircuitModel model) throws Exception {
        return new CircuitFileParser().parse(new ByteArrayInputStream(new CircuitFileWriter().write(model)), "native.ipes");
    }

    private static void replaceSavedToken(CircuitModel.ComponentData component, String field, String value) {
        var block = component.getSavedBlock();
        var rows = new ArrayList<>(block.serializedText().lines().filter(row -> !row.startsWith(field + " ")).toList());
        rows.add(field + " " + value);
        component.setSavedBlock(SavedCircuitBlock.parse(block.tag(), rows.toArray(String[]::new)));
    }

    @Test
    void rejectsAnExecutionAliasCollisionAndUnsupportedResources() throws Exception {
        var model = importTransient();
        var conflicting = new CircuitModel.ComponentData(1, R1);
        conflicting.setNumericParameters(new double[]{1000, 0, 0});
        conflicting.setTerminalXLabels(new String[]{"outA"});
        conflicting.setTerminalYLabels(new String[]{"0"});
        model.getCircuitComponents().add(conflicting);
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), new PreparationRequest(solver(), Map.of()));
        assertTrue(result.report().diagnostics().stream().anyMatch(d -> d.code().equals("INVALID_TOPOLOGY")
                && d.message().contains("Duplicate component name")), result.report().diagnostics().toString());

        model.getResources().put(7L, new gecko.core.io.CircuitResource(7L, "waveform.txt", "waveform.txt",
                new byte[]{1}, List.of()));
        var resource = preparation.prepare(preparation.capture(model), new PreparationRequest(solver(), Map.of()));
        assertInstanceOf(PreparationResult.Rejected.class, resource);
        assertTrue(resource.report().diagnostics().stream().anyMatch(d -> d.code().equals("UNSUPPORTED_TRANSIENT_LAW")));
    }

    private static PreparationResult.Ready ready(CircuitModel model, Map<String, Double> overrides) {
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), new PreparationRequest(solver(), overrides));
        return assertInstanceOf(PreparationResult.Ready.class, result, result.report().diagnostics().toString());
    }

    private static SolverOverrides solver() {
        return new SolverOverrides(SolverType.SOLVER_BE, STEP, 8 * STEP);
    }

    private static List<Long> nativeIdentifiers(CircuitModel model) {
        return java.util.stream.Stream.concat(model.getCircuitComponents().stream(), model.getSpecialComponents().stream())
                .map(component -> component.getSavedBlock() == null
                        ? Long.parseLong(component.getImportTokens().get("uniqueObjectIdentifier"))
                        : component.getSavedBlock().uniqueObjectIdentifier()).toList();
    }

    private static void assertSlots(CircuitModel model, double firstResistance, double secondResistance) {
        var hierarchy = new HierarchyIndex(model);
        assertEquals(firstResistance, component(model, hierarchy, R1).getNumericParameters()[0]);
        assertEquals(secondResistance, component(model, hierarchy, R2).getNumericParameters()[0]);
        assertEquals(INITIAL_VOLTAGE, component(model, hierarchy, C1).getNumericParameters()[1]);
        assertEquals(INITIAL_VOLTAGE, component(model, hierarchy, C2).getNumericParameters()[1]);
        assertEquals(Map.of(0, "{series}"), component(model, hierarchy, R1).getScalarBindings());
    }

    private static CircuitModel.ComponentData component(CircuitModel model, HierarchyIndex hierarchy, String name) {
        return model.getCircuitComponents().stream().filter(component -> name.equals(hierarchy.executionName(component)))
                .findFirst().orElseThrow();
    }

    private static List<double[]> trace(PreparationResult.Ready ready, String rA, String rB, String cA, String cB,
            String observedSignal) {
        var trace = new ArrayList<double[]>();
        try (var session = SimulationSession.open(ready.execution(), ExecutionOptions.defaults())) {
            int observedRow = observedSignal == null ? -1 : session.scopeChannels().stream()
                    .filter(channel -> channel.signalName().equals(observedSignal)).findFirst().orElseThrow().row();
            for (int sample = 0; sample < 4; sample++) {
                var circuitState = new StringBuilder();
                var data = session.step((time, voltages, currents, netlist) -> {
                    int firstR = netlist.getElementIndexByName(rA);
                    int secondR = netlist.getElementIndexByName(rB);
                    int firstC = netlist.getElementIndexByName(cA);
                    int secondC = netlist.getElementIndexByName(cB);
                    assertTrue(firstR >= 0 && secondR >= 0 && firstC >= 0 && secondC >= 0);
                    assertNotEquals(firstR, secondR);
                    assertNotEquals(firstC, secondC);
                    assertEquals(-1, netlist.getElementIndexByName("Rseries"));
                    trace.add(new double[]{time,
                            voltages[netlist.getNodeX(firstC)] - voltages[netlist.getNodeY(firstC)], currents[firstR],
                            voltages[netlist.getNodeX(secondC)] - voltages[netlist.getNodeY(secondC)], currents[secondR],
                            currents[firstC], currents[secondC]});
                    if (observedRow >= 0) {
                        int returnSource = netlist.getElementIndexByName("Vret1");
                        circuitState.append(" t=").append(time)
                                .append(" capX=").append(netlist.getNodeX(firstC)).append(':').append(voltages[netlist.getNodeX(firstC)])
                                .append(" capY=").append(netlist.getNodeY(firstC)).append(':').append(voltages[netlist.getNodeY(firstC)])
                                .append(" ground=").append(voltages[0])
                                .append(" returnCurrent=").append(currents[returnSource])
                                .append(" publicVoltages=").append(java.util.Arrays.toString(voltages))
                                .append(" numericalOrder=").append(java.util.Arrays.toString(netlist.getNumericalNodeOrder()))
                                .append(" channels=").append(session.scopeChannels());
                    }
                });
                if (observedRow >= 0) {
                    assertEquals(trace.getLast()[1], data.values()[observedRow], 1e-6,
                            () -> "Root pin probe must see the first occurrence voltage." + circuitState);
                }
            }
        }
        assertEquals(4, trace.size());
        return trace;
    }

    private static void verifyBackwardEuler(List<double[]> trace, double rA, double rB) {
        double a = INITIAL_VOLTAGE;
        double b = INITIAL_VOLTAGE;
        for (int index = 0; index < trace.size(); index++) {
            var actual = trace.get(index);
            a = next(a, rA);
            b = next(b, rB);
            // The non-UIC transient publishes its first advanced sample at t = 0.
            double seriesA = (SUPPLY - a) / rA;
            double seriesB = (SUPPLY - b) / rB;
            assertArrayEquals(new double[]{index * STEP, a, seriesA, b, seriesB,
                    seriesA - a / SHUNT_RESISTANCE, seriesB - b / SHUNT_RESISTANCE}, actual, 1e-10);
        }
    }

    private static double next(double previous, double resistance) {
        double conductance = 1 / resistance + 1 / SHUNT_RESISTANCE;
        double equivalentVoltage = SUPPLY / resistance / conductance;
        double ratio = STEP * conductance / CAPACITANCE;
        return (previous + ratio * equivalentVoltage) / (1 + ratio);
    }

    private CircuitModel importTransient() throws Exception {
        return importFixture(inputs, true);
    }

    private static void assertReturnConnection(CircuitModel model) {
        var topology = gecko.core.io.CircuitTopology.resolve(model);
        var hierarchy = new HierarchyIndex(model);
        var cap = component(model, hierarchy, C1);
        var source = component(model, hierarchy, "Vret1");
        assertEquals(topology.group(source, gecko.core.io.CircuitTopology.Side.INPUT, 0),
                topology.group(cap, gecko.core.io.CircuitTopology.Side.OUTPUT, 0),
                "The original AC fixture connects its native return and capacitor");
    }

    private CircuitModel importFixture(Path target, boolean transientAnalysis) throws Exception {
        for (String file : List.of("root.asc", "leaf.asc", "leaf.asy", "wrapper.asc", "wrapper.asy",
                "res.asy", "cap.asy", "voltage.asy")) {
            String text;
            try (var resource = getClass().getResourceAsStream("/ltspice-hierarchy/" + file)) {
                assertNotNull(resource, file);
                text = new String(resource.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            }
            if (transientAnalysis && file.equals("root.asc")) {
                text = text.replace("FLAG 192 228 0", "FLAG 288 100 outA\nFLAG 528 100 outB\nFLAG 192 228 0")
                        .replace("SYMATTR Value AC 1", "SYMATTR Value 5")
                        .replace(".ac dec 2 10 100k", ".tran 0 8u 0 1u");
            } else if (transientAnalysis && file.equals("leaf.asc")) {
                text = text.replace("SYMATTR Value 10n", "SYMATTR Value 10n IC=1");
            }
            Files.writeString(target.resolve(file), text);
        }
        return new LtspiceImporter().convert(target.resolve("root.asc"), target, List.of(target));
    }
}
