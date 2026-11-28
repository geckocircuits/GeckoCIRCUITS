package gecko.geckocircuits.control;

import gecko.geckocircuits.circuit.AbstractBlockInterface;
import gecko.geckocircuits.circuit.CircuitSheet;
import gecko.geckocircuits.circuit.NetzlisteAllg;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class DesktopScopeParityTest {
    @ParameterizedTest
    @CsvSource({"0,0", "1,6", "2,6"})
    void controlShortedStateKeepsTheGainCalculationRatherThanPhysicalBypass(int state, float expected) {
        var sheet = new CircuitSheet(null);
        var source = new ReglerConstant();
        source._constValue.setValueWithoutUndo(2.0);
        source.YOUT.get(0).getLabelObject().setLabelWithoutUndo("drive");
        var gain = new ReglerGain();
        gain._gain.setValueWithoutUndo(3.0);
        gain._isEnabled.setValueWithoutUndo(gecko.geckocircuits.circuit.Enabled.getFromOrdinal(state));
        gain.XIN.get(0).getLabelObject().setLabelWithoutUndo("drive");
        gain.YOUT.get(0).getLabelObject().setLabelWithoutUndo("scaled");
        var scope = new ReglerOSZI();
        scope.setInputTerminalNumber(1);
        scope.XIN.get(0).getLabelObject().setLabelWithoutUndo("scaled");
        List<AbstractBlockInterface> blocks = List.of(source, gain, scope);
        for (int index = 0; index < blocks.size(); index++) {
            blocks.get(index).setParentCircuitSheet(sheet);
            blocks.get(index).setSheetPositionWithoutUndo(new Point(10 + 20 * index, 10));
        }
        var topology = NetzlisteAllg.fabricNetzlistDisabledParentSubsRemoved(List.of(), blocks);
        var runtime = NetzlisteCONTROL.fabricRunSimulation(topology);
        runtime.initializeAtSimulationStart(0.1);
        runtime.berechneZeitschritt(0.1, 0);
        assertEquals(1, NetzlisteCONTROL.globalData.getRowLength());
        assertEquals(expected, NetzlisteCONTROL.globalData.getValue(0, 0));
    }

    @Test
    void unequalDepthScopesAndAnotherSinkKeepDesktopRecordingOrder() {
        var sheet = new CircuitSheet(null);
        var left = new ReglerConstant();
        left._constValue.setValueWithoutUndo(2.0);
        var right = new ReglerConstant();
        right._constValue.setValueWithoutUndo(7.0);
        var firstGain = new ReglerGain();
        firstGain._gain.setValueWithoutUndo(3.0);
        var secondGain = new ReglerGain();
        secondGain._gain.setValueWithoutUndo(4.0);
        var deepScope = new ReglerOSZI();
        deepScope.setInputTerminalNumber(1);
        var shallowScope = new ReglerOSZI();
        shallowScope.setInputTerminalNumber(1);
        var gate = new ReglerGate();
        left.YOUT.get(0).getLabelObject().setLabelWithoutUndo("left");
        right.YOUT.get(0).getLabelObject().setLabelWithoutUndo("right");
        firstGain.XIN.get(0).getLabelObject().setLabelWithoutUndo("left");
        firstGain.YOUT.get(0).getLabelObject().setLabelWithoutUndo("middle");
        secondGain.XIN.get(0).getLabelObject().setLabelWithoutUndo("middle");
        secondGain.YOUT.get(0).getLabelObject().setLabelWithoutUndo("deep");
        deepScope.XIN.get(0).getLabelObject().setLabelWithoutUndo("deep");
        shallowScope.XIN.get(0).getLabelObject().setLabelWithoutUndo("right");
        gate.XIN.get(0).getLabelObject().setLabelWithoutUndo("left");
        List<AbstractBlockInterface> blocks = List.of(left, right, deepScope, gate, shallowScope, firstGain, secondGain);
        for (int index = 0; index < blocks.size(); index++) {
            blocks.get(index).setParentCircuitSheet(sheet);
            blocks.get(index).setSheetPositionWithoutUndo(new Point(10 + 20 * index, 10));
        }
        var desktop = NetzlisteCONTROL.fabricRunSimulation(NetzlisteAllg.fabricNetzlistComplete(List.of(), blocks));
        desktop.initializeAtSimulationStart(0.1);
        desktop.berechneZeitschritt(0.1, 0);
        var backend = new gecko.core.simulation.ControlCalculatorFactory().build(List.of(
                component(3, "left", 2, new String[0], "left"),
                component(3, "right", 7, new String[0], "right"),
                component(5, "deepScope", 0, new String[]{"deep"}),
                component(6, "gate", 0, new String[]{"left"}),
                component(5, "shallowScope", 0, new String[]{"right"}),
                component(7, "firstGain", 3, new String[]{"left"}, "middle"),
                component(7, "secondGain", 4, new String[]{"middle"}, "deep")));
        backend.getControlNetlist().executeTimeStep(0.1, 0);
        var samples = gecko.core.simulation.ScopeDataExtractor.fromScopes(backend.getMapping().getScopeInfos());
        assertEquals(2, NetzlisteCONTROL.globalData.getRowLength());
        assertEquals("right", NetzlisteCONTROL.globalData.getSignalName(0));
        assertEquals("deep", NetzlisteCONTROL.globalData.getSignalName(1));
        assertEquals(7, NetzlisteCONTROL.globalData.getValue(0, 0));
        assertEquals(24, NetzlisteCONTROL.globalData.getValue(1, 0));
        for (int row = 0; row < samples.getSignalCount(); row++) {
            assertEquals(NetzlisteCONTROL.globalData.getSignalName(row), samples.getSignalNames()[row]);
            assertEquals(NetzlisteCONTROL.globalData.getValue(row, 0), samples.extractCurrentValues()[row]);
        }
        assertEquals(List.of(1, 0), samples.getChannels().stream()
                .map(gecko.core.simulation.ScopeDataExtractor.ScopeChannel::row).toList());
    }

    private static gecko.core.io.CircuitModel.ComponentData component(int type, String name, double value,
            String[] inputs, String... outputs) {
        var data = new gecko.core.io.CircuitModel.ComponentData(type, name);
        data.setNumericParameters(new double[]{value});
        data.setTerminalXLabels(inputs);
        data.setTerminalYLabels(outputs);
        return data;
    }

    @Test
    void sharedScopePotentialsUseOneGlobalRowAndKeepAnUnconnectedPort() {
        var sheet = new CircuitSheet(null);
        var left = new ReglerConstant();
        left._constValue.setValueWithoutUndo(2.0);
        left.YOUT.get(0).getLabelObject().setLabelWithoutUndo("leftValue");
        var right = new ReglerConstant();
        right._constValue.setValueWithoutUndo(7.0);
        right.YOUT.get(0).getLabelObject().setLabelWithoutUndo("rightValue");
        var firstScope = new ReglerOSZI();
        firstScope.setInputTerminalNumber(3);
        firstScope.XIN.get(0).getLabelObject().setLabelWithoutUndo("rightValue");
        firstScope.XIN.get(2).getLabelObject().setLabelWithoutUndo("leftValue");
        var secondScope = new ReglerOSZI();
        secondScope.setInputTerminalNumber(2);
        secondScope.XIN.get(0).getLabelObject().setLabelWithoutUndo("leftValue");
        secondScope.XIN.get(1).getLabelObject().setLabelWithoutUndo("rightValue");
        List<AbstractBlockInterface> blocks = List.of(left, right, firstScope, secondScope);
        for (int index = 0; index < blocks.size(); index++) {
            var block = blocks.get(index);
            block.setParentCircuitSheet(sheet);
            block.setSheetPositionWithoutUndo(new Point(10 + 20 * index, 10));
        }
        var topology = NetzlisteAllg.fabricNetzlistComplete(List.of(), blocks);
        var runtime = NetzlisteCONTROL.fabricRunSimulation(topology);
        runtime.initializeAtSimulationStart(0.1);
        runtime.berechneZeitschritt(0.1, 0);
        var data = NetzlisteCONTROL.globalData;
        assertEquals(3, data.getRowLength());
        assertEquals("leftValue", data.getSignalName(0));
        assertEquals("rightValue", data.getSignalName(1));
        assertEquals("", data.getSignalName(2));
        assertEquals(2, data.getValue(0, 0));
        assertEquals(7, data.getValue(1, 0));
        assertEquals(0, data.getValue(2, 0));
    }
}
