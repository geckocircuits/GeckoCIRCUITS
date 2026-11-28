package gecko.geckocircuits.control;

import gecko.geckocircuits.circuit.AbstractBlockInterface;
import gecko.geckocircuits.circuit.CircuitSheet;
import gecko.geckocircuits.circuit.NetzlisteAllg;
import gecko.core.preparation.*;
import gecko.core.simulation.HeadlessSimulationEngine;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DesktopBusParityTest {
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void muxAndDemuxKeepTheDesktopChannelOrder(boolean namedSettings) throws Exception {
        var left = new ReglerConstant();
        left.setTyp(ControlTyp.C_CONST);
        left._constValue.setValueWithoutUndo(2.0);
        left.YOUT.get(0).getLabelObject().setLabelWithoutUndo("left");
        var middle = new ReglerConstant();
        middle.setTyp(ControlTyp.C_CONST);
        middle._constValue.setValueWithoutUndo(5.0);
        middle.YOUT.get(0).getLabelObject().setLabelWithoutUndo("middle");
        var right = new ReglerConstant();
        right.setTyp(ControlTyp.C_CONST);
        right._constValue.setValueWithoutUndo(9.0);
        right.YOUT.get(0).getLabelObject().setLabelWithoutUndo("right");
        var mux = new ReglerMUX();
        mux.setTyp(ControlTyp.C_MUX);
        var demux = new ReglerDemux();
        demux.setTyp(ControlTyp.C_DEMUX);
        var scope = new ReglerOSZI();
        scope.setTyp(ControlTyp.C_SCOPE);
        scope.setInputTerminalNumber(3);
        String[] inputs = {"left", "middle", "right"};
        String[] outputs = {"out2", "out0", "out1"};
        for (int port = 0; port < 3; port++) {
            mux.XIN.get(port).getLabelObject().setLabelWithoutUndo(inputs[port]);
            demux.YOUT.get(port).getLabelObject().setLabelWithoutUndo(outputs[port]);
            scope.XIN.get(port).getLabelObject().setLabelWithoutUndo("out" + port);
        }
        mux.YOUT.get(0).getLabelObject().setLabelWithoutUndo("bus");
        demux.XIN.get(0).getLabelObject().setLabelWithoutUndo("bus");
        List<AbstractBlockInterface> blocks = List.of(scope, demux, mux, right, left, middle);
        var sheet = new CircuitSheet(null);
        for (int index = 0; index < blocks.size(); index++) {
            blocks.get(index).setParentCircuitSheet(sheet);
            blocks.get(index).setSheetPositionWithoutUndo(new Point(10 + 20 * index, 10));
        }
        var desktop = NetzlisteCONTROL.fabricRunSimulation(NetzlisteAllg.fabricNetzlistComplete(List.of(), blocks));
        desktop.initializeAtSimulationStart(0.1);
        desktop.berechneZeitschritt(0.1, 0);

        var saved = new StringBuffer("dt 0.1\ntDURATION 0.2\n");
        for (int index = 0; index < blocks.size(); index++) {
            var block = new StringBuffer();
            blocks.get(index).exportASCII(block);
            saved.append("c\n").append(block.toString().replaceAll("(?m)^idStringDialog[^\\r\\n]*", "idStringDialog block" + index));
        }
        String savedText = saved.toString();
        if (!namedSettings) savedText = savedText.replaceAll("(?m)^(constantValue|tn) [^\\r\\n]*\\R", "");
        var model = new gecko.core.io.CircuitFileParser().parse(
                new java.io.BufferedReader(new java.io.StringReader(savedText)), "desktop-bus.ipes");
        assertEquals(6, model.getControlComponents().size(), model.getImportIssues().toString());
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, outcome, outcome.report().toString());
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        float[] expected = {5, 9, 2};
        assertEquals(3, NetzlisteCONTROL.globalData.getRowLength());
        for (int row = 0; row < 3; row++) {
            assertEquals(expected[row], NetzlisteCONTROL.globalData.getValue(row, 0));
            assertEquals(NetzlisteCONTROL.globalData.getSignalName(row), result.getSignalNames()[row]);
            assertArrayEquals(new float[]{expected[row], expected[row], expected[row]}, result.getSignalData(row));
        }
    }

}
