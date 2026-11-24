package gecko.geckocircuits.circuit;

import gecko.core.allg.GeckoFile;
import gecko.core.circuit.TokenMap;
import gecko.geckocircuits.allg.GeckoFileManager;
import gecko.geckocircuits.allg.OptimizerParameterData;
import gecko.geckocircuits.circuit.circuitcomponents.ResistorCircuit;
import gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;
import gecko.core.io.CircuitTopology;
import gecko.geckocircuits.control.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class EditorCircuitCaptureTest {
    @Test
    void capturedEmbeddedResourceRetainsBytesAndOwnerAfterChanges(@TempDir Path directory) throws Exception {
        Path model = directory.resolve("circuit.ipes");
        var file = new GeckoFile(directory.resolve("pulse.txt").toFile(), model.toString(), new byte[]{-1, 0, 127});
        file.setUser(17);

        var snapshot = new EditorCircuitCapture().capture(List.of(), new SolverSettings(),
                new OptimizerParameterData(), new GeckoFileManager(List.of(file)),
                EditorCircuitCapture.ScriptSources.EMPTY, model.toString());
        file.getContentsByte()[0] = 0;
        file.setUser(22);

        var resource = snapshot.toModel().getResources().get(file.getHashValue());
        assertEquals("pulse.txt", resource.name());
        assertEquals(List.of(17L), resource.ownerIds());
        assertArrayEquals(new byte[]{-1, 0, 127}, resource.bytes());
        byte[] returnedBytes = resource.bytes();
        returnedBytes[0] = 0;
        assertArrayEquals(new byte[]{-1, 0, 127}, resource.bytes());
    }

    @Test
    void largeExternalAliasesKeepDistinctOwnersAndReportMissingResources(@TempDir Path directory) throws Exception {
        Path model = directory.resolve("circuit.ipes");
        byte[] expected = new byte[8 * 1024 * 1024];
        Arrays.fill(expected, (byte) -126);
        expected[expected.length - 1] = 127;
        Path waveform = Files.write(directory.resolve("waveform.dat"), expected);
        var first = externalResource(model, waveform, 31, 101);
        var second = externalResource(model, waveform, 32, 102);
        Path missingPath = Files.write(directory.resolve("missing.dat"), new byte[]{1});
        var missing = externalResource(model, missingPath, 33, 103);
        Files.move(missingPath, directory.resolve("moved.dat"));

        var captured = new EditorCircuitCapture().capture(List.of(), new SolverSettings(),
                new OptimizerParameterData(), new GeckoFileManager(List.of(first, second, missing)),
                EditorCircuitCapture.ScriptSources.EMPTY, model.toString()).toModel();

        var firstResource = captured.getResources().get(31L);
        var secondResource = captured.getResources().get(32L);
        assertEquals(List.of(101L), firstResource.ownerIds());
        assertEquals(List.of(102L), secondResource.ownerIds());
        assertArrayEquals(expected, firstResource.bytes());
        assertArrayEquals(expected, secondResource.bytes());
        assertFalse(captured.getResources().containsKey(33L));
        var failure = captured.getResourceFailures().get(33L);
        assertNotNull(failure);
        assertEquals(List.of(103L), failure.ownerIds());
        assertEquals(missingPath.toString(), failure.provenance());
    }

    private static GeckoFile externalResource(Path model, Path file, long id, long owner) throws FileNotFoundException {
        var tokens = new TokenMap(new String[]{"fileSep " + File.separator, "hashValue " + id, "isExternal 1",
            "absPath " + file, "relPath " + file.getFileName(), "usageList[] /" + owner});
        return new GeckoFile(tokens, List.of(), model.toString(), null);
    }

    @ParameterizedTest
    @CsvSource({"endpoints,20,20,true", "terminal,15,10,true", "tee,15,20,true", "crossing,15,20,false"})
    void capturedJunctionsMatchDesktopEndpointAndTerminalRules(String kind, int secondX, int secondY,
            boolean connected) throws Exception {
        var sheet = new CircuitSheet(null);
        var first = CircuitTyp.LK_R.getTypeInfo().fabric();
        var second = CircuitTyp.LK_R.getTypeInfo().fabric();
        first.getIdentifier().createNewIdentifier(1);
        second.getIdentifier().createNewIdentifier(2);
        first.setParentCircuitSheet(sheet);
        second.setParentCircuitSheet(sheet);
        first.setComponentDirection(ComponentDirection.NORTH_SOUTH);
        second.setComponentDirection(ComponentDirection.NORTH_SOUTH);
        first.setSheetPositionWithoutUndo(new Point(10, 12));
        second.setSheetPositionWithoutUndo(new Point(secondX, secondY + 2));
        var wires = new ArrayList<Verbindung>();
        wires.add(wire(sheet, 10, 10, 20, 10));
        switch (kind) {
            case "endpoints" -> wires.add(wire(sheet, 20, 10, 20, 20));
            case "tee" -> wires.add(wire(sheet, 15, 10, 15, 20));
            case "crossing" -> wires.add(wire(sheet, 15, 5, 15, 20));
            default -> { }
        }
        var legacy = NetzlisteAllg.fabricNetzlistComplete(wires, List.of(first, second));
        boolean desktopConnected = Arrays.stream(legacy.getPotentiale()).anyMatch(potential ->
                potential.isTerminalOnPotential(first.XIN.getFirst()) && potential.isTerminalOnPotential(second.XIN.getFirst()));
        assertEquals(connected, desktopConnected);
        var elements = new ArrayList<AbstractCircuitSheetComponent>(List.of(first, second));
        elements.addAll(wires);
        var captured = new EditorCircuitCapture().capture(elements, new SolverSettings(), new OptimizerParameterData(),
                new GeckoFileManager(), EditorCircuitCapture.ScriptSources.EMPTY, "junction-" + kind).toModel();
        var graph = CircuitTopology.resolve(captured);
        assertEquals(desktopConnected, graph.group(captured.getCircuitComponents().getFirst(), CircuitTopology.Side.INPUT, 0)
                .equals(graph.group(captured.getCircuitComponents().getLast(), CircuitTopology.Side.INPUT, 0)));
    }

    private static Verbindung wire(CircuitSheet sheet, int x1, int y1, int x2, int y2) {
        var wire = new Verbindung(ConnectorType.LK, sheet);
        wire.setzeStartKnoten(new Point(x1, y1));
        wire.setzeAktuellenPunktAufVerbindung(new Point(x2, y2));
        wire.setzeEndKnoten(x2, y2);
        return wire;
    }

    @Test
    void fourPortPhysicalShortKeepsTheSameTwoSeparatePairsAsDesktopConnectors() throws Exception {
        var transformer = CircuitTyp.LK_TRANS.getTypeInfo().fabric();
        transformer.getIdentifier().createNewIdentifier(9100000001L);
        transformer.setComponentDirection(ComponentDirection.NORTH_SOUTH);
        transformer.setSheetPositionWithoutUndo(new Point(10, 10));
        transformer.setCircuitEnabled(Enabled.DISABLED_SHORT);
        var probes = new ArrayList<AbstractBlockInterface>();
        var terminals = List.of(transformer.XIN.get(0), transformer.XIN.get(1), transformer.YOUT.get(0), transformer.YOUT.get(1));
        for (int i = 0; i < terminals.size(); i++) {
            var probe = CircuitTyp.LK_R.getTypeInfo().fabric();
            probe.getIdentifier().createNewIdentifier(2000 + i);
            probe.setComponentDirection(ComponentDirection.WEST_EAST);
            var point = terminals.get(i).getPosition();
            probe.setSheetPositionWithoutUndo(new Point(point.x + 2, point.y));
            probes.add(probe);
        }
        var legacy = NetzlisteAllg.fabricNetzlistComplete(transformer.getShortConnectors(), probes);
        var firstPair = Arrays.stream(legacy.getPotentiale())
                .filter(potential -> potential.isTerminalOnPotential(probes.get(0).XIN.getFirst())).findFirst().orElseThrow();
        var secondPair = Arrays.stream(legacy.getPotentiale())
                .filter(potential -> potential.isTerminalOnPotential(probes.get(1).XIN.getFirst())).findFirst().orElseThrow();
        assertTrue(firstPair.isTerminalOnPotential(probes.get(2).XIN.getFirst()));
        assertTrue(secondPair.isTerminalOnPotential(probes.get(3).XIN.getFirst()));
        assertNotSame(firstPair, secondPair, "Desktop short connectors preserve two separate potential groups");

        var elements = new ArrayList<AbstractCircuitSheetComponent>(List.of(transformer));
        elements.addAll(probes);
        var snapshot = new EditorCircuitCapture().capture(elements, new SolverSettings(),
                new OptimizerParameterData(), new GeckoFileManager(), EditorCircuitCapture.ScriptSources.EMPTY, "four-port-short");
        var model = snapshot.toModel();
        var component = model.getCircuitComponents().getFirst();
        var graph = CircuitTopology.resolve(model);
        assertEquals(graph.group(component, CircuitTopology.Side.INPUT, 0), graph.group(component, CircuitTopology.Side.OUTPUT, 0));
        assertEquals(graph.group(component, CircuitTopology.Side.INPUT, 1), graph.group(component, CircuitTopology.Side.OUTPUT, 1));
        assertNotEquals(graph.group(component, CircuitTopology.Side.INPUT, 0), graph.group(component, CircuitTopology.Side.INPUT, 1));
    }

    @Test
    void unsavedTerminalGeometryConnectsTouchingPortsWithoutInventingLabels() throws Exception {
        var first = (ResistorCircuit) CircuitTyp.LK_R.getTypeInfo().fabric();
        var second = (ResistorCircuit) CircuitTyp.LK_R.getTypeInfo().fabric();
        first.getIdentifier().createNewIdentifier(1);
        second.getIdentifier().createNewIdentifier(2);
        first.setComponentDirection(ComponentDirection.NORTH_SOUTH);
        second.setComponentDirection(ComponentDirection.NORTH_SOUTH);
        first.setSheetPositionWithoutUndo(new Point(10, 10));
        second.setSheetPositionWithoutUndo(new Point(10, 14));
        var snapshot = new EditorCircuitCapture().capture(List.of(first, second), new SolverSettings(),
                new OptimizerParameterData(), new GeckoFileManager(), EditorCircuitCapture.ScriptSources.EMPTY, "unsaved");
        second.setSheetPositionWithoutUndo(new Point(50, 50));
        var model = snapshot.toModel();
        var graph = CircuitTopology.resolve(model);
        assertEquals(graph.group(model.getCircuitComponents().getFirst(), CircuitTopology.Side.OUTPUT, 0),
                graph.group(model.getCircuitComponents().getLast(), CircuitTopology.Side.INPUT, 0));
        assertNotEquals(graph.group(model.getCircuitComponents().getFirst(), CircuitTopology.Side.INPUT, 0),
                graph.group(model.getCircuitComponents().getLast(), CircuitTopology.Side.OUTPUT, 0));
        assertEquals("/NIX_NIX_NIX", model.getCircuitComponents().getFirst().getSavedBlock().tokens().get("labelEndKnoten[]"));
    }

    @Test
    void unsavedEditorValuesBecomeIndependentBackendInputWithoutDesktopGlobals() throws Exception {
        var resistor = (ResistorCircuit) CircuitTyp.LK_R.getTypeInfo().fabric();
        resistor.getIdentifier().createNewIdentifier(9100000001L);
        resistor.setAccessibleParameter("R", 37);
        resistor.XIN.getFirst().getLabelObject().setLabelWithoutUndo("n");
        resistor.YOUT.getFirst().getLabelObject().setLabelWithoutUndo("0");
        var settings = new SolverSettings();
        settings.dt.setValueWithoutUndo(0.0001);
        settings._tDURATION.setValueWithoutUndo(0.02);
        settings._T_pre.setValueWithoutUndo(-0.003);
        settings._dt_pre.setValueWithoutUndo(0.00001);
        var captured = new EditorCircuitCapture().capture(List.of(resistor), settings,
                new OptimizerParameterData(), new GeckoFileManager(), EditorCircuitCapture.ScriptSources.EMPTY, "unsaved");
        resistor.setAccessibleParameter("R", 93);
        settings.dt.setValueWithoutUndo(0.1);
        var model = captured.toModel();
        assertEquals(37, model.getCircuitComponents().getFirst().getNumericParameters()[0]);
        assertEquals(9100000001L, model.getCircuitComponents().getFirst().getSavedBlock().uniqueObjectIdentifier());
        assertEquals(0.0001, model.getTimeStep());
        assertEquals(-0.003, model.getPreSimulationTime());
        assertEquals(0.00001, model.getPreSimulationTimeStep());
        assertNull(model.getDataContainerSignals(), "A capture must not read an old recording");
    }
}
