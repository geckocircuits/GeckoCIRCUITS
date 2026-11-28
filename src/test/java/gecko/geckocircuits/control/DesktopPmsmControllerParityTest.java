package gecko.geckocircuits.control;

import gecko.geckocircuits.circuit.AbstractBlockInterface;
import gecko.geckocircuits.circuit.CircuitSheet;
import gecko.geckocircuits.circuit.NetzlisteAllg;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import gecko.core.preparation.*;
import gecko.core.simulation.ControlCalculatorFactory;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.ScopeDataExtractor;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class DesktopPmsmControllerParityTest {
    private static final String[] OUTPUTS = {"alpha", "beta", "vq", "vd", "iqRef", "idRef", "iq", "id"};

    private record Trial(String name, double[] inputs, float[][] expected) {
        @Override public String toString() { return name; }
    }

    static Stream<Trial> trials() {
        return Stream.of(
            new Trial("normal", inputs(0, 0, 0, 1, 10, 10), new float[][]{
                {0,0,0}, {2.25f,5.25f,9.25f}, {2.25f,5.25f,9.25f}, {0,0,0},
                {1.5f,2.5f,3.5f}, {0,0,0}, {0,0,0}, {0,0,0}
            }),
            new Trial("sample-disabled", inputs(0, 0, 0, 0.999, 10, 10), new float[][]{
                {0,0,0}, {0,0,0}, {2.25f,5.25f,9.25f}, {0,0,0},
                {1.5f,2.5f,3.5f}, {0,0,0}, {0,0,0}, {0,0,0}
            }),
            new Trial("saturated", inputs(0, 0, 0, 1, 2, 3), new float[][]{
                {0,0,0}, {2.25f,3,3}, {2.25f,3,3}, {0,0,0},
                {1.5f,2,2}, {0,0,0}, {0,0,0}, {0,0,0}
            }),
            new Trial("d-current", inputs(1, -0.5, 0, 1, 10, 10), new float[][]{
                {-1.5f,-2.5f,-3.5f}, {2.25f,5.25f,9.25f}, {2.25f,5.25f,9.25f},
                {-1.5f,-2.5f,-3.5f}, {1.5f,2.5f,3.5f}, {0,0,0}, {0,0,0}, {1,1,1}
            }),
            new Trial("rotated-current", inputs(1, -0.5, 1.5707963267948966, 1, 10, 10), new float[][]{
                {-3.75f,-7.75f,-10}, {0,0,0}, {3.75f,7.75f,10}, {0,0,0},
                {1.5f,2.5f,3.5f}, {0,0,0}, {-1,-1,-1}, {0,0,0}
            })
        );
    }

    static Stream<Trial> restartTrials() {
        return trials().filter(trial -> trial.name.equals("normal") || trial.name.equals("d-current"));
    }


    @ParameterizedTest
    @MethodSource("trials")
    void savedControllerMatchesAllEightDesktopOutputHistories(Trial trial) throws Exception {
        var legacy = legacy(trial.inputs);
        for (int step = 0; step < 3; step++) {
            legacy.netlist.berechneZeitschritt(1, step);
            assertDesktop(column(trial.expected, step), step);
        }
        var model = savedModel(legacy);
        var outcome = prepare(model);
        var result = new HeadlessSimulationEngine().runSimulation(outcome.execution(), ExecutionOptions.defaults(), () -> false);
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertArrayEquals(OUTPUTS, result.getSignalNames());
        for (int row = 0; row < OUTPUTS.length; row++) {
            assertArrayEquals(trial.expected[row], result.getSignalData(row), 1e-6f, OUTPUTS[row]);
        }
    }

    @ParameterizedTest
    @MethodSource("restartTrials")
    void explicitBackendRestartMatchesAFreshDesktopStart(Trial trial) throws Exception {
        var legacy = legacy(trial.inputs);
        var build = build(legacy);
        var netlist = build.getControlNetlist();
        var scope = ScopeDataExtractor.fromScopes(build.getMapping().getScopeInfos());
        netlist.initializeAtSimulationStart(1);
        for (int step = 0; step < 3; step++) {
            legacy.netlist.berechneZeitschritt(1, step);
            netlist.executeTimeStep(1, step);
        }
        assertArrayEquals(column(trial.expected, 2), scope.extractCurrentValues(), 1e-6f);
        var fresh = legacy(trial.inputs);
        netlist.initializeAtSimulationStart(1);
        assertArrayEquals(new float[8], scope.extractCurrentValues());
        fresh.netlist.berechneZeitschritt(1, 0);
        netlist.executeTimeStep(1, 0);
        assertDesktop(column(trial.expected, 0), 0);
        assertArrayEquals(desktopSample(0), scope.extractCurrentValues(), 1e-6f);
    }

    @Test
    void continuedNewGridMatchesDesktopWithoutResettingControllerHistory() throws Exception {
        var legacy = legacy(inputs(0, 0, 0, 1, 10, 10));
        var build = build(legacy);
        var netlist = build.getControlNetlist();
        var scope = ScopeDataExtractor.fromScopes(build.getMapping().getScopeInfos());
        netlist.initializeAtSimulationStart(1);
        for (int step = 0; step < 2; step++) {
            legacy.netlist.berechneZeitschritt(1, step);
            netlist.executeTimeStep(1, step);
        }
        var continuation = NetzlisteCONTROL.fabricContinueSimulation(legacy.netlist);
        continuation.doDtChangeInit(0.5);
        netlist.prepareTimeStepChange(0.5).run();
        // Desktop continuation starts a recorder segment at the previous grid's next time.
        continuation.berechneZeitschritt(0.5, 2);
        netlist.executeTimeStep(0.5, 2);
        assertDesktop(new float[]{0,7.125f,7.125f,0,3,0,0,0}, 0);
        assertArrayEquals(desktopSample(0), scope.extractCurrentValues(), 1e-6f);
        continuation.berechneZeitschritt(0.5, 2.5);
        netlist.executeTimeStep(0.5, 2.5);
        assertDesktop(new float[]{0,9.25f,9.25f,0,3.5f,0,0,0}, 1);
        assertArrayEquals(desktopSample(1), scope.extractCurrentValues(), 1e-6f);
    }

    @Test
    void changingTheLiveSampleFlagLatchesOnlyVoltageInBothRuntimes() throws Exception {
        var legacy = legacy(inputs(0, 0, 0, 1, 10, 10));
        var build = build(legacy);
        var netlist = build.getControlNetlist();
        var scope = ScopeDataExtractor.fromScopes(build.getMapping().getScopeInfos());
        var controller = Arrays.stream(netlist.getSortedCalculators())
                .filter(gecko.core.control.calculators.PmsmControlCalculator.class::isInstance)
                .map(gecko.core.control.calculators.PmsmControlCalculator.class::cast).findFirst().orElseThrow();
        var flag = Arrays.stream(netlist.getSortedCalculators())
                .filter(gecko.core.control.calculators.ConstantCalculator.class::isInstance)
                .map(gecko.core.control.calculators.ConstantCalculator.class::cast)
                .filter(source -> source._outputSignal[0] == controller._inputSignal[11]).findFirst().orElseThrow();
        netlist.initializeAtSimulationStart(1);
        legacy.netlist.berechneZeitschritt(1, 0);
        netlist.executeTimeStep(1, 0);
        legacy.sources[11]._constValue.setValueWithoutUndo(0.999);
        flag.setConst(0.999);
        legacy.netlist.berechneZeitschritt(1, 1);
        netlist.executeTimeStep(1, 1);
        assertDesktop(new float[]{0,2.25f,5.25f,0,2.5f,0,0,0}, 1);
        assertArrayEquals(desktopSample(1), scope.extractCurrentValues(), 1e-6f);
        legacy.netlist.berechneZeitschritt(1, 2);
        netlist.executeTimeStep(1, 2);
        assertDesktop(new float[]{0,2.25f,9.25f,0,3.5f,0,0,0}, 2);
        assertArrayEquals(desktopSample(2), scope.extractCurrentValues(), 1e-6f);
        legacy.sources[11]._constValue.setValueWithoutUndo(1.0);
        flag.setConst(1);
        legacy.netlist.berechneZeitschritt(1, 3);
        netlist.executeTimeStep(1, 3);
        assertDesktop(new float[]{0,10,10,0,4.5f,0,0,0}, 3);
        assertArrayEquals(desktopSample(3), scope.extractCurrentValues(), 1e-6f);
    }

    private record LegacyRun(ReglerConstant[] sources, List<AbstractBlockInterface> blocks, NetzlisteCONTROL netlist) {}

    private static LegacyRun legacy(double[] values) {
        var controller = new ReglerPMSMCONTROL();
        controller.setTyp(ControlTyp.C_PMSM_CONTROL);
        var scope = new ReglerOSZI();
        scope.setTyp(ControlTyp.C_SCOPE);
        scope.setInputTerminalNumber(8);
        var blocks = new ArrayList<AbstractBlockInterface>();
        blocks.add(scope);
        blocks.add(controller);
        var sources = new ReglerConstant[12];
        for (int port = 0; port < 12; port++) {
            var source = new ReglerConstant();
            source.setTyp(ControlTyp.C_CONST);
            source._constValue.setValueWithoutUndo(values[port]);
            source.YOUT.get(0).getLabelObject().setLabelWithoutUndo("input" + port);
            controller.XIN.get(port).getLabelObject().setLabelWithoutUndo("input" + port);
            sources[port] = source;
        }
        for (int port = 0; port < 8; port++) {
            controller.YOUT.get(port).getLabelObject().setLabelWithoutUndo(OUTPUTS[port]);
            scope.XIN.get(port).getLabelObject().setLabelWithoutUndo(OUTPUTS[port]);
        }
        for (int port = 11; port >= 0; port--) blocks.add(sources[port]);
        var sheet = new CircuitSheet(null);
        for (int index = 0; index < blocks.size(); index++) {
            blocks.get(index).setParentCircuitSheet(sheet);
            blocks.get(index).setSheetPositionWithoutUndo(new Point(10 + 20 * index, 10));
        }
        var netlist = NetzlisteCONTROL.fabricRunSimulation(NetzlisteAllg.fabricNetzlistComplete(List.of(), blocks));
        netlist.initializeAtSimulationStart(1);
        return new LegacyRun(sources, List.copyOf(blocks), netlist);
    }

    private static CircuitModel savedModel(LegacyRun legacy) throws Exception {
        var saved = new StringBuffer("dt 1\ntDURATION 2\n");
        for (int index = 0; index < legacy.blocks.size(); index++) {
            var block = new StringBuffer();
            legacy.blocks.get(index).exportASCII(block);
            saved.append("c\n").append(block.toString().replaceAll(
                    "(?m)^idStringDialog[^\\r\\n]*", "idStringDialog block" + index));
        }
        var model = new CircuitFileParser().parse(new BufferedReader(new StringReader(saved.toString())), "desktop-pmsm-controller.ipes");
        assertEquals(14, model.getControlComponents().size(), model.getImportIssues().toString());
        return model;
    }

    private static PreparationResult.Ready prepare(CircuitModel model) {
        var preparation = new CircuitPreparation();
        var outcome = preparation.prepare(preparation.capture(model), PreparationRequest.defaults());
        return assertInstanceOf(PreparationResult.Ready.class, outcome, outcome.report().toString());
    }

    private static ControlCalculatorFactory.BuildResult build(LegacyRun legacy) throws Exception {
        var model = savedModel(legacy);
        prepare(model);
        return new ControlCalculatorFactory().buildCircuit(model, 1);
    }

    private static void assertDesktop(float[] expected, int sample) {
        assertEquals(8, NetzlisteCONTROL.globalData.getRowLength());
        assertArrayEquals(expected, desktopSample(sample), 1e-6f);
    }

    private static float[] desktopSample(int sample) {
        float[] result = new float[8];
        for (int row = 0; row < result.length; row++) {
            assertEquals(OUTPUTS[row], NetzlisteCONTROL.globalData.getSignalName(row));
            result[row] = NetzlisteCONTROL.globalData.getValue(row, sample);
        }
        return result;
    }

    private static double[] inputs(double a, double b, double angle, double sample, double speedLimit, double currentLimit) {
        return new double[]{a,b,0,angle,9.549296585513721,1,1,speedLimit,1,1,currentLimit,sample};
    }

    private static float[] column(float[][] values, int sample) {
        float[] result = new float[values.length];
        for (int row = 0; row < values.length; row++) result[row] = values[row][sample];
        return result;
    }
}
