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

class DesktopTransformParityTest {
    @ParameterizedTest
    @CsvSource({
        "1,-0.5,-0.5,0,1,0,1,-0.5,-0.5",
        "1,-0.5,-0.5,1.5707963267948966,0,-1,1,-0.5,-0.5",
        "1,2,3,0,-1,-0.5773502691896257,-1,0,1"
    })
    void savedTransformsMatchTheIndependentDesktopChain(double a, double b, double c, double angle,
            double d, double q, double reconstructedA, double reconstructedB, double reconstructedC) throws Exception {
        var sourceA = constant("a", a);
        var sourceB = constant("b", b);
        var sourceC = constant("c", c);
        var theta = constant("angle", angle);
        var forward = new ReglerABCDQ();
        forward.setTyp(ControlTyp.C_ABCDQ);
        var inverse = new ReglerDQABC();
        inverse.setTyp(ControlTyp.C_DQABC);
        var scope = new ReglerOSZI();
        scope.setTyp(ControlTyp.C_SCOPE);
        scope.setInputTerminalNumber(5);
        String[] forwardInputs = {"a", "b", "c", "angle"};
        String[] dqLabels = {"d", "q"};
        String[] inverseInputs = {"d", "q", "angle"};
        String[] phases = {"phaseA", "phaseB", "phaseC"};
        String[] names = {"d", "q", "phaseA", "phaseB", "phaseC"};
        for (int port = 0; port < 4; port++) {
            forward.XIN.get(port).getLabelObject().setLabelWithoutUndo(forwardInputs[port]);
        }
        for (int port = 0; port < 2; port++) {
            forward.YOUT.get(port).getLabelObject().setLabelWithoutUndo(dqLabels[port]);
        }
        for (int port = 0; port < 3; port++) {
            inverse.XIN.get(port).getLabelObject().setLabelWithoutUndo(inverseInputs[port]);
            inverse.YOUT.get(port).getLabelObject().setLabelWithoutUndo(phases[port]);
        }
        for (int port = 0; port < 5; port++) {
            scope.XIN.get(port).getLabelObject().setLabelWithoutUndo(names[port]);
        }
        List<AbstractBlockInterface> blocks = List.of(scope, inverse, forward, theta, sourceC, sourceB, sourceA);
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
            saved.append("c\n").append(block.toString().replaceAll(
                    "(?m)^idStringDialog[^\\r\\n]*", "idStringDialog block" + index));
        }
        var model = new CircuitFileParser().parse(new BufferedReader(new StringReader(saved.toString())), "desktop-transforms.ipes");
        assertEquals(7, model.getControlComponents().size(), model.getImportIssues().toString());
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        var ready = assertInstanceOf(PreparationResult.Ready.class, outcome, outcome.report().toString());
        var result = new HeadlessSimulationEngine().runSimulation(ready.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());

        double[] expected = {d, q, reconstructedA, reconstructedB, reconstructedC};
        assertEquals(5, NetzlisteCONTROL.globalData.getRowLength());
        assertArrayEquals(names, result.getSignalNames());
        for (int row = 0; row < 5; row++) {
            assertEquals(expected[row], NetzlisteCONTROL.globalData.getValue(row, 0), 1e-6);
            assertEquals(NetzlisteCONTROL.globalData.getSignalName(row), result.getSignalNames()[row]);
            float value = (float) expected[row];
            assertArrayEquals(new float[]{value, value, value}, result.getSignalData(row), 1e-6f);
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
