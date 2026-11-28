package gecko.core.simulation;

import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitTopology;
import gecko.core.preparation.*;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedControlTopologyExecutionTest {
    @Test
    void aBoundaryAliasCarriesCurrentStepValuesIntoAChildGain() throws Exception {
        var model = parse(subcircuit(101)
                + block(5, "scope", 5, 101, "", "/amplified", "")
                + block(7, "gain", 4, 101, "3", "/inside", "/amplified")
                + block(78, "boundary", 3, 101, "", "/inside", "/outside")
                + block(58, "time", 2, 0, "", "", "/outside"));
        var ready = ready(model);
        var result = run(ready);
        assertArrayEquals(new String[]{"amplified"}, result.getSignalNames());
        assertArrayEquals(new float[]{0,0.3f,0.6f}, result.getSignalData(0), 1e-6f);
    }

    @Test
    void equalLocalLabelsAndNamesInDifferentSheetsKeepTheirOwnProducers() throws Exception {
        var model = parse(subcircuit(101) + subcircuit(202)
                + block(5, "scope", 5, 101, "", "/scaled", "")
                + block(5, "scope", 6, 202, "", "/scaled", "")
                + block(7, "gain", 4, 202, "4", "/drive", "/scaled")
                + block(7, "gain", 3, 101, "3", "/drive", "/scaled")
                + block(3, "source", 2, 202, "7", "", "/drive")
                + block(3, "source", 1, 101, "2", "", "/drive"));
        var result = run(ready(model));
        var channels = channels(result);
        var left = channels.stream().filter(channel -> channel.scopeIdentifier() == 5).findFirst().orElseThrow();
        var right = channels.stream().filter(channel -> channel.scopeIdentifier() == 6).findFirst().orElseThrow();
        assertNotEquals(left.row(), right.row());
        assertEquals(List.of(5L,6L), channels.stream().map(ScopeDataExtractor.ScopeChannel::scopeIdentifier).toList());
        assertArrayEquals(new float[]{6,6,6}, result.getSignalData(left.row()));
        assertArrayEquals(new float[]{28,28,28}, result.getSignalData(right.row()));
    }

    @Test
    void aMatchingLabelOnAnotherSheetDoesNotSupplyAnUnconnectedInput() throws Exception {
        var model = parse(subcircuit(101) + subcircuit(202)
                + block(5, "scope", 2, 101, "", "/ghost", "")
                + block(3, "source", 1, 202, "9", "", "/ghost"));
        var ready = ready(model);
        var result = run(ready);
        assertAll(
                () -> assertTrue(ready.report().diagnostics().stream().anyMatch(diagnostic ->
                        diagnostic.code().equals("MISSING_CONTROL_INPUT") && "scope".equals(diagnostic.component()))),
                () -> assertArrayEquals(new float[]{0,0,0}, result.getSignalData(0)));
    }

    @Test
    void aBoundaryGroupWithTwoProducersIsRejectedBeforeExecution() throws Exception {
        var model = parse(subcircuit(101)
                + block(5, "scope", 3, 101, "", "/inside", "")
                + block(78, "boundary", 2, 101, "", "/inside", "/outside")
                + block(3, "child", 4, 101, "7", "", "/inside")
                + block(3, "parent", 1, 0, "2", "", "/outside"));
        var preparation = new CircuitPreparation();
        var outcome = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        assertEquals(ReadinessReport.Outcome.INVALID, outcome.report().outcome());
        assertTrue(outcome.report().diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("AMBIGUOUS_CONTROL_SIGNAL") && "scope".equals(diagnostic.component())),
                outcome.report().toString());
    }

    @Test
    void aBlankScopeInputWithTwoGeometricProducersIsRejectedBeforeExecution() throws Exception {
        var model = parse(subcircuit(101)
                + block(3, "first", 1, 101, "2", "", "/first")
                + block(3, "second", 2, 101, "7", "", "/second")
                + block(5, "scope", 3, 101, "", "/placeholder", ""));
        model.getControlComponents().get(2).setTerminalXLabels(new String[]{""});
        for (int index = 0; index < 3; index++) {
            var side = index == 2 ? CircuitTopology.Side.INPUT : CircuitTopology.Side.OUTPUT;
            model.getControlComponents().get(index).getTerminalData().add(
                    new CircuitModel.TerminalData("CONTROL", 101, side, 0, 12, 10, true));
        }
        var preparation = new CircuitPreparation();
        var outcome = assertInstanceOf(PreparationResult.Inspectable.class,
                preparation.prepare(preparation.capture(model), PreparationRequest.defaults()));
        assertEquals(ReadinessReport.Outcome.INVALID, outcome.report().outcome());
        assertTrue(outcome.report().diagnostics().stream().anyMatch(diagnostic ->
                diagnostic.code().equals("AMBIGUOUS_CONTROL_SIGNAL") && "scope".equals(diagnostic.component())),
                outcome.report().toString());
    }

    private static CircuitModel parse(String blocks) throws Exception {
        return new CircuitFileParser().parse(new BufferedReader(new StringReader(
                "dt 0.1\ntDURATION 0.2\n" + blocks)), "control-groups.ipes");
    }

    private static PreparationResult.Ready ready(CircuitModel model) {
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        return assertInstanceOf(PreparationResult.Ready.class, outcome, outcome.report().toString());
    }

    private static SimulationResult run(PreparationResult.Ready ready) {
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<ScopeDataExtractor.ScopeChannel> channels(SimulationResult result) {
        return (List<ScopeDataExtractor.ScopeChannel>) result.getMetadata().get("scopeChannels");
    }

    private static String block(int type, String name, long id, long sheet, String parameters, String inputs, String outputs) {
        return "c\n<ElementCONTROL>\ntyp " + type + "\nidStringDialog " + name
                + "\nuniqueObjectIdentifier " + id + "\nparentSheetIdentifier " + sheet
                + "\nparameter[] " + parameters + "\nparameterString[] /\nlabelAnfangsKnoten[] " + inputs
                + "\nlabelEndKnoten[] " + outputs + "\n<" + (char)92 + "ElementCONTROL>\n";
    }

    private static String subcircuit(long id) {
        return "sp\n<ElementSPECIAL>\ntyp 27\nidStringDialog sub" + id + "\nuniqueObjectIdentifier " + id
                + "\nparentSheetIdentifier 0\nenabledShorted 1\n<" + (char)92 + "ElementSPECIAL>\n";
    }
}
