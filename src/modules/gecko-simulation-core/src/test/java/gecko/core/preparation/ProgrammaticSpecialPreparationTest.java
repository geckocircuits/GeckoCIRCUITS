package gecko.core.preparation;

import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.SavedCircuitBlock;
import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgrammaticSpecialPreparationTest {
    @Test
    void activeUnknownSpecialWithoutSavedBlockCannotBeReadyAndUnexecuted() {
        var model = CircuitPreparationTest.resistorCircuit();
        var special = new CircuitModel.ComponentData(999, "unknownEditorBlock");
        special.setSourceLocation("unsaved editor component");
        model.getSpecialComponents().add(special);
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var inspectable = assertInstanceOf(PreparationResult.Inspectable.class, result, result.report().toString());
        assertEquals(ReadinessReport.Outcome.UNSUPPORTED, inspectable.report().outcome());
        var issues = inspectable.report().diagnostics().stream()
                .filter(issue -> issue.code().equals("UNSUPPORTED_SIMULATION_BLOCK")).toList();
        assertEquals(1, issues.size());
        assertEquals("SPECIAL", issues.getFirst().domain());
        assertEquals("unknownEditorBlock", issues.getFirst().component());
        assertEquals("unsaved editor component", issues.getFirst().sourceLocation());
        assertEquals(999, inspectable.circuit().toModel().getSpecialComponents().getFirst().getType());
    }

    @Test
    void knownContainerAndOptimizerMetadataWithoutSavedBlocksRemainSupported() {
        var model = CircuitPreparationTest.resistorCircuit();
        model.getSpecialComponents().add(new CircuitModel.ComponentData(27, "container"));
        model.getSpecialComponents().add(new CircuitModel.ComponentData(70, "optimizerMetadata"));
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, result, result.report().toString());
        assertEquals(2, ready.circuit().toModel().getSpecialComponents().size());
        assertEquals(java.util.List.of(0.6, 0.6, 0.6), CircuitPreparationTest.currents(ready.execution()));
    }

    @Test
    void unsupportedDescendantOfDisabledContainerStaysInspectableWithoutBlockingExecution() {
        var model = CircuitPreparationTest.resistorCircuit();
        var container = new CircuitModel.ComponentData(27, "disabledContainer");
        container.setSavedBlock(SavedCircuitBlock.parse("ElementSPECIAL",
                new String[]{"uniqueObjectIdentifier 101", "enabledShorted 0"}));
        var unknown = new CircuitModel.ComponentData(999, "disabledUnknown");
        unknown.setSavedBlock(SavedCircuitBlock.parse("ElementSPECIAL",
                new String[]{"parentSheetIdentifier 101", "enabledShorted 1"}));
        model.getSpecialComponents().add(container);
        model.getSpecialComponents().add(unknown);
        var preparation = new CircuitPreparation();
        var result = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, result, result.report().toString());
        assertEquals(2, ready.circuit().toModel().getSpecialComponents().size());
        assertEquals(1, ready.circuit().toModel().getImportIssues().stream()
                .filter(issue -> issue.code().equals("UNSUPPORTED_SIMULATION_BLOCK")).count());
        assertTrue(ready.execution().newRunModel().getSpecialComponents().isEmpty());
        assertEquals(java.util.List.of(0.6, 0.6, 0.6), CircuitPreparationTest.currents(ready.execution()));
    }

    @Test
    void savedUnknownSpecialRetainsOneDiagnosticAfterCaptureAndRecapture() throws Exception {
        String text = SourcePreparationTest.element("LK", 4, "U.1", "401 6", "/hot", "/0", "")
                + SourcePreparationTest.element("LK", 1, "R.1", "10", "/hot", "/0", "")
                + "sp\n<ElementSPECIAL>\ntyp 999\nidStringDialog unknownSaved\n<\\ElementSPECIAL>\n"
                + "dt 0.1\ntDURATION 0.2\n";
        var model = new CircuitFileParser().parse(new BufferedReader(new StringReader(text)), "unknown-special.ipes");
        assertEquals(1, model.getImportIssues().size());
        var expectedLocation = model.getImportIssues().getFirst().sourceLocation();
        var preparation = new CircuitPreparation();
        var snapshot = preparation.capture(preparation.capture(model).toModel());
        var result = preparation.prepare(snapshot, PreparationRequest.defaults());
        var inspectable = assertInstanceOf(PreparationResult.Inspectable.class, result, result.report().toString());
        var issues = inspectable.report().diagnostics().stream()
                .filter(issue -> issue.code().equals("UNSUPPORTED_SIMULATION_BLOCK")).toList();
        assertEquals(1, issues.size());
        assertEquals(expectedLocation, issues.getFirst().sourceLocation());
        assertEquals("unknownSaved", issues.getFirst().component());
    }
}
