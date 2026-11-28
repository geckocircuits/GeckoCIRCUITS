package gecko.core.preparation;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitTopology;
import gecko.core.io.SavedCircuitBlock;
import gecko.core.simulation.HeadlessSimulationEngine;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExecutionProjectionPreparationTest {
    @Test
    void knownSubcircuitCannotHideAnUnsupportedSpecialBlockWithTheSameName() throws Exception {
        String text = """
                dt 0.1
                tDURATION 0.2
                e
                <ElementLK>
                typ 4
                idStringDialog U.1
                parameter[] 401 6
                labelAnfangsKnoten[] /hot
                labelEndKnoten[] /0
                <\\ElementLK>
                e
                <ElementLK>
                typ 1
                idStringDialog R.1
                parameter[] 10
                labelAnfangsKnoten[] /hot
                labelEndKnoten[] /0
                <\\ElementLK>
                sp
                <ElementSPECIAL>
                typ 27
                idStringDialog shared
                uniqueObjectIdentifier 101
                enabledShorted 1
                <\\ElementSPECIAL>
                sp
                <ElementSPECIAL>
                typ 999
                idStringDialog shared
                uniqueObjectIdentifier 202
                enabledShorted 1
                <\\ElementSPECIAL>
                """;
        var model = new CircuitFileParser().parse(new BufferedReader(new StringReader(text)), "special-provenance.ipes");
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var inspectable = assertInstanceOf(PreparationResult.Inspectable.class, result, result.report().toString());
        assertEquals(ReadinessReport.Outcome.UNSUPPORTED, inspectable.report().outcome());
        var unsupported = inspectable.report().diagnostics().stream()
                .filter(diagnostic -> diagnostic.code().equals("UNSUPPORTED_SIMULATION_BLOCK")).toList();
        assertEquals(1, unsupported.size());
        assertEquals("SPECIAL", unsupported.getFirst().domain());
        assertEquals("shared", unsupported.getFirst().component());
        assertEquals("special-provenance.ipes: line 27 ElementSPECIAL", unsupported.getFirst().sourceLocation());
    }

    @Test
    void disabledAncestorWireCannotChangeImplicitReferenceOrSurviveExecutionProjection() throws Exception {
        String text = SourcePreparationTest.element("LK", 4, "U.1", "401 6", "/hot", "", "")
                + SourcePreparationTest.element("LK", 1, "R.1", "10", "/hot", "", "") + """
                sp
                <ElementSPECIAL>
                typ 27
                idStringDialog disabled
                uniqueObjectIdentifier 101
                enabledShorted 0
                <\\ElementSPECIAL>
                verbindungLK
                <Verbindung>
                x[] 100 101
                y[] 100 100
                parentSheetIdentifier 101
                enabledShorted 1
                <\\Verbindung>
                dt 0.1
                tDURATION 0.2
                """;
        var model = new CircuitFileParser().parse(new BufferedReader(new StringReader(text)), "disabled-wire.ipes");
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, result, result.report().toString());
        assertEquals(1, ready.circuit().toModel().getConnections().size());
        assertTrue(ready.execution().newRunModel().getConnections().isEmpty());
        assertEquals(List.of(0.6, 0.6, 0.6), CircuitPreparationTest.currents(ready.execution()));
    }

    @Test
    void ownControlShortedStateRunsTheGainInsteadOfPhysicalBypassOrOmission() throws Exception {
        var model = SourcePreparationTest.parse(
                SourcePreparationTest.element("LK", 1, "R.1", "10", "/1", "/0", "")
                + SourcePreparationTest.element("CONTROL", 3, "constant", "2", "", "/drive", "")
                + SourcePreparationTest.element("CONTROL", 7, "gain", "3", "/drive", "/scaled", "enabledShorted 2")
                + SourcePreparationTest.element("CONTROL", 5, "scope", "", "/scaled", "", ""));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        assertEquals(3, ready.execution().newRunModel().getControlComponents().size());
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(new float[]{6, 6, 6}, result.getSignalData(0));
    }

    @Test
    void shortedSeriesComponentBypassesItsConfiguredResistanceInEveryRun() throws Exception {
        var model = SourcePreparationTest.parse(
                SourcePreparationTest.element("LK", 4, "U.1", "401 6", "/hot", "/0", "")
                + SourcePreparationTest.element("LK", 1, "bypassed", "1000", "/hot", "/load", "enabledShorted 2")
                + SourcePreparationTest.element("LK", 1, "R.1", "10", "/load", "/0", ""));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        assertEquals(3, ready.circuit().toModel().getCircuitComponents().size());
        assertEquals(2, ready.execution().newRunModel().getCircuitComponents().size());
        assertEquals(List.of(0.6, 0.6, 0.6), CircuitPreparationTest.currents(ready.execution()));
        assertEquals(List.of(0.6, 0.6, 0.6), CircuitPreparationTest.currents(ready.execution()));
    }

    @Test
    void componentsWithoutSavedIdentifiersKeepTheirSourceOrdinalAfterFiltering() {
        var model = CircuitPreparationTest.resistorCircuit();
        var disabled = new CircuitModel.ComponentData(999, "disabled");
        disabled.setSavedBlock(SavedCircuitBlock.parse("ElementCONTROL", new String[]{"enabledShorted 0"}));
        model.addControlComponent(disabled);
        model.addControlComponent(CircuitPreparationTest.component(3, "constant", new double[]{2}, "", "out"));
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        var inspection = ready.circuit().toModel();
        var run = ready.execution().newRunModel();
        var sourceIdentity = CircuitTopology.resolve(inspection).identity(inspection.getControlComponents().getLast());
        var runIdentity = CircuitTopology.resolve(run).identity(run.getControlComponents().getFirst());
        assertEquals(new CircuitTopology.ComponentIdentity("CONTROL", 0, 1), sourceIdentity);
        assertEquals(sourceIdentity, runIdentity);
    }

    @Test
    void unsupportedChildOfDisabledAncestorStaysInspectableButCannotChangeAnAcceptedRun() throws Exception {
        String text = SourcePreparationTest.element("LK", 4, "U.1", "401 6", "/1", "/0", "")
                + SourcePreparationTest.element("LK", 1, "R.1", "10", "/1", "/0", "")
                + """
                sp
                <ElementSPECIAL>
                typ 27
                idStringDialog disabled
                uniqueObjectIdentifier 101
                enabledShorted 0
                <\\ElementSPECIAL>
                sp
                <ElementSPECIAL>
                typ 27
                idStringDialog nested
                uniqueObjectIdentifier 202
                parentSheetIdentifier 101
                enabledShorted 1
                <\\ElementSPECIAL>
                c
                <ElementCONTROL>
                typ 999
                idStringDialog unsupportedChild
                parentSheetIdentifier 202
                <\\ElementCONTROL>
                dt 0.1
                tDURATION 0.2
                """;
        var model = new CircuitFileParser().parse(new BufferedReader(new StringReader(text)), "disabled-child.ipes");
        var preparation = new CircuitPreparation();
        var ready = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        assertEquals(1, ready.circuit().toModel().getControlComponents().size());
        assertTrue(ready.execution().newRunModel().getControlComponents().isEmpty());
        assertEquals(List.of(0.6, 0.6, 0.6), CircuitPreparationTest.currents(ready.execution()));
        var again = assertInstanceOf(PreparationResult.Ready.class,
                preparation.prepare(ready.circuit(), PreparationRequest.defaults()));
        assertEquals(List.of(0.6, 0.6, 0.6), CircuitPreparationTest.currents(again.execution()));
    }
}
