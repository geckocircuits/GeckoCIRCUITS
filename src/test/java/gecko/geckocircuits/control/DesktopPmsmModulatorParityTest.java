package gecko.geckocircuits.control;

import gecko.geckocircuits.circuit.AbstractBlockInterface;
import gecko.geckocircuits.circuit.CircuitSheet;
import gecko.geckocircuits.circuit.NetzlisteAllg;
import gecko.core.io.CircuitFileParser;
import gecko.core.preparation.*;
import gecko.core.simulation.HeadlessSimulationEngine;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class DesktopPmsmModulatorParityTest {
    @ParameterizedTest
    @CsvSource({
        "0.4330127018922193,0.25,0.4,1,0,1,1",
        "0.4330127018922193,0.25,0.6,1,0,0,1",
        "0,0.5,0.4,1,1,0,1",
        "0,0.5,0.6,1,0,0,1",
        "-0.4330127018922193,0.25,0.4,1,1,0,1",
        "-0.4330127018922193,0.25,0.6,1,1,0,0",
        "-0.4330127018922193,-0.25,0.4,1,1,1,0",
        "-0.4330127018922193,-0.25,0.6,1,1,0,0",
        "0,-0.5,0.4,1,1,1,0",
        "0,-0.5,0.6,1,0,1,0",
        "0.4330127018922193,-0.25,0.4,1,0,1,1",
        "0.4330127018922193,-0.25,0.6,1,0,1,0",
        "0,0,0.4,1,1,1,1",
        "0,0,0.5,1,0,0,0",
        "0,0,0.6,1,0,0,0",
        "2,0,0.98,1,0,0,0",
        "0,0,0.6,0,1,1,1"
    })
    void savedModulationMatchesDesktopSectorsClippingAndInactiveComparatorBranches(
            double alpha, double beta, double carrier, double dc, int u, int v, int w) throws Exception {
        var sourceAlpha = constant("alpha", alpha);
        var sourceBeta = constant("beta", beta);
        var sourceCarrier = constant("carrier", carrier);
        var sourceDc = constant("dc", dc);
        var modulator = new ReglerPMSM_Modulator();
        modulator.setTyp(ControlTyp.C_PMSM_MODULATOR);
        var scope = new ReglerOSZI();
        scope.setTyp(ControlTyp.C_SCOPE);
        scope.setInputTerminalNumber(3);
        String[] inputs = {"alpha", "beta", "carrier", "dc"};
        String[] outputs = {"gateU", "gateV", "gateW"};
        String[] names = {"gateW", "gateU", "gateV"};
        for (int port = 0; port < 4; port++) {
            modulator.XIN.get(port).getLabelObject().setLabelWithoutUndo(inputs[port]);
        }
        for (int port = 0; port < 3; port++) {
            modulator.YOUT.get(port).getLabelObject().setLabelWithoutUndo(outputs[port]);
            scope.XIN.get(port).getLabelObject().setLabelWithoutUndo(names[port]);
        }
        List<AbstractBlockInterface> blocks = List.of(scope, modulator, sourceDc, sourceCarrier, sourceBeta, sourceAlpha);
        var sheet = new CircuitSheet(null);
        for (int index = 0; index < blocks.size(); index++) {
            blocks.get(index).setParentCircuitSheet(sheet);
            blocks.get(index).setSheetPositionWithoutUndo(new Point(10 + 20 * index, 10));
        }
        var legacy = NetzlisteCONTROL.fabricRunSimulation(NetzlisteAllg.fabricNetzlistComplete(List.of(), blocks));
        legacy.initializeAtSimulationStart(0.1);
        legacy.berechneZeitschritt(0.1, 0);
        var saved = new StringBuffer("dt 0.1\ntDURATION 0.2\n");
        for (int index = 0; index < blocks.size(); index++) {
            var block = new StringBuffer();
            blocks.get(index).exportASCII(block);
            saved.append("c\n").append(block.toString().replaceAll(
                    "(?m)^idStringDialog[^\\r\\n]*", "idStringDialog block" + index));
        }
        var model = new CircuitFileParser().parse(new BufferedReader(new StringReader(saved.toString())), "desktop-pmsm-modulator.ipes");
        assertEquals(6, model.getControlComponents().size(), model.getImportIssues().toString());
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, outcome, outcome.report().toString());
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertEquals(3, NetzlisteCONTROL.globalData.getRowLength());
        assertArrayEquals(names, result.getSignalNames());
        float[] expected = {w, u, v};
        for (int row = 0; row < 3; row++) {
            assertEquals(expected[row], NetzlisteCONTROL.globalData.getValue(row, 0));
            assertEquals(NetzlisteCONTROL.globalData.getSignalName(row), result.getSignalNames()[row]);
            assertArrayEquals(new float[]{expected[row], expected[row], expected[row]}, result.getSignalData(row));
        }
    }

    private static ReglerConstant constant(String label, double value) {
        var source = new ReglerConstant();
        source.setTyp(ControlTyp.C_CONST);
        source._constValue.setValueWithoutUndo(value);
        source.YOUT.get(0).getLabelObject().setLabelWithoutUndo(label);
        return source;
    }
}
