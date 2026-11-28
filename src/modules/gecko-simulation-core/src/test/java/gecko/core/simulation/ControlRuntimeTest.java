/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS. If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.simulation;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ControlRuntimeTest {

    @Test
    void emptyRuntimeHasNoScopeAndLeavesElectricalStateUnchanged() {
        CircuitNetlist electrical = electricalNetlist(CircuitTypCore.LK_S, "S.1",
                new double[]{1e9, 0.01, 1e9, 0, 0, 0, 0, 0, 0});

        for (CircuitModel model : new CircuitModel[]{null, new CircuitModel()}) {
            ControlRuntime runtime = ControlRuntime.from(model, 1e-6);
            assertFalse(runtime.hasScope());
            assertArrayEquals(new String[0], runtime.getSignalNames());
            assertArrayEquals(new float[0], runtime.sampleScope());
            runtime.applyPendingGates(electrical);
            runtime.advanceAfterElectricalStep(electrical, 0);
            assertEquals(1e9, electrical.getParameter(0)[0]);
        }
    }

    @Test
    void solvedVoltageAndCurrentReachScopeInConfiguredOrder() {
        CircuitModel model = new CircuitModel();
        model.addControlComponent(block(1, "VOLT.1", new double[0],
                new String[0], new String[]{"voltage"}, new String[]{"1", "2"}));
        model.addControlComponent(block(2, "AMP.1", new double[0],
                new String[0], new String[]{"current"}, new String[]{"R.1"}));
        model.addControlComponent(block(7, "GAIN.1", new double[]{2},
                new String[]{"voltage"}, new String[]{"scaled"}, new String[0]));
        model.addControlComponent(scope("current", "scaled"));
        ControlRuntime runtime = ControlRuntime.from(model, 1e-6);
        CircuitNetlist electrical = electricalNetlist(CircuitTypCore.LK_R, "R.1", new double[]{10});

        electrical.storeResults(new double[]{0, 11, 3}, new double[]{-0.75});
        runtime.advanceAfterElectricalStep(electrical, 0);

        assertTrue(runtime.hasScope());
        assertArrayEquals(new String[]{"current", "scaled"}, runtime.getSignalNames());
        assertArrayEquals(new float[]{-0.75f, 16}, runtime.sampleScope(), 1e-6f);

        electrical.storeResults(new double[]{0, 7, 5}, new double[]{0.25});
        runtime.advanceAfterElectricalStep(electrical, 1e-6);
        assertArrayEquals(new float[]{0.25f, 4}, runtime.sampleScope(), 1e-6f);
    }

    @Test
    void controlGateChangesResistanceOnlyAtTheNextPreSolvePhase() {
        CircuitModel model = new CircuitModel();
        model.addControlComponent(block(1, "VOLT.1", new double[0],
                new String[0], new String[]{"gate"}, new String[]{"1", "0"}));
        model.addControlComponent(block(6, "GATE.1", new double[0],
                new String[]{"gate"}, new String[0], new String[]{"S.1"}));
        model.addControlComponent(scope("gate"));
        ControlRuntime runtime = ControlRuntime.from(model, 1e-6);
        CircuitNetlist electrical = electricalNetlist(CircuitTypCore.LK_S, "S.1",
                new double[]{1e9, 0.01, 1e9, 0, 0, 0, 0, 0, 0});

        runtime.applyPendingGates(electrical);
        assertEquals(1e9, electrical.getParameter(0)[0]);

        electrical.storeResults(new double[]{0, 1}, new double[]{0});
        runtime.advanceAfterElectricalStep(electrical, 0);
        assertEquals(1, electrical.getParameter(0)[8]);
        assertEquals(1e9, electrical.getParameter(0)[0]);
        assertArrayEquals(new float[]{1}, runtime.sampleScope());

        runtime.applyPendingGates(electrical);
        assertEquals(0.01, electrical.getParameter(0)[0]);

        electrical.storeResults(new double[]{0, 0}, new double[]{0});
        runtime.advanceAfterElectricalStep(electrical, 1e-6);
        assertEquals(0, electrical.getParameter(0)[8]);
        assertEquals(0.01, electrical.getParameter(0)[0]);

        runtime.applyPendingGates(electrical);
        assertEquals(1e9, electrical.getParameter(0)[0]);
    }

    @Test
    void signalPhaseInitializesOnceAndProgressesAcrossSteps() {
        CircuitModel model = new CircuitModel();
        model.addControlComponent(block(4, "SIGNAL.1", new double[]{401, 1, 1, Math.PI / 2, 0, 0.5},
                new String[0], new String[]{"triangle"}, new String[0]));
        model.addControlComponent(scope("triangle"));
        ControlRuntime runtime = ControlRuntime.from(model, 0.01);
        CircuitNetlist electrical = new CircuitNetlist();

        runtime.advanceAfterElectricalStep(electrical, 0);
        assertArrayEquals(new float[]{-1}, runtime.sampleScope(), 1e-6f);
        runtime.advanceAfterElectricalStep(electrical, 0.01);
        assertArrayEquals(new float[]{-0.96f}, runtime.sampleScope(), 1e-6f);
        runtime.advanceAfterElectricalStep(electrical, 0.02);
        assertArrayEquals(new float[]{-0.92f}, runtime.sampleScope(), 1e-6f);
    }

    @Test
    void scopeNamesAndSamplesDoNotExposeRuntimeArrays() {
        CircuitModel model = new CircuitModel();
        model.addControlComponent(block(3, "CONST.1", new double[]{4.5},
                new String[0], new String[]{"constant"}, new String[0]));
        model.addControlComponent(scope("constant"));
        ControlRuntime runtime = ControlRuntime.from(model, 1e-6);

        String[] names = runtime.getSignalNames();
        float[] sample = runtime.sampleScope();
        names[0] = "changed";
        sample[0] = -1;

        assertArrayEquals(new String[]{"constant"}, runtime.getSignalNames());
        assertArrayEquals(new float[]{4.5f}, runtime.sampleScope());
    }

    @Test
    void runtimeWithoutScopeStillTransfersGateValues() {
        CircuitModel model = new CircuitModel();
        model.addControlComponent(block(3, "CONST.1", new double[]{1},
                new String[0], new String[]{"gate"}, new String[0]));
        model.addControlComponent(block(6, "GATE.1", new double[0],
                new String[]{"gate"}, new String[0], new String[]{"S.1"}));
        ControlRuntime runtime = ControlRuntime.from(model, 1e-6);
        CircuitNetlist electrical = electricalNetlist(CircuitTypCore.LK_S, "S.1",
                new double[]{1e9, 0.01, 1e9, 0, 0, 0, 0, 0, 0});

        runtime.advanceAfterElectricalStep(electrical, 0);
        runtime.applyPendingGates(electrical);

        assertFalse(runtime.hasScope());
        assertArrayEquals(new String[0], runtime.getSignalNames());
        assertArrayEquals(new float[0], runtime.sampleScope());
        assertEquals(0.01, electrical.getParameter(0)[0]);
    }

    private static CircuitModel.ComponentData scope(String... inputs) {
        return block(5, "SCOPE.1", new double[0], inputs, new String[0], new String[0]);
    }

    private static CircuitModel.ComponentData block(int type, String name, double[] parameters,
            String[] inputs, String[] outputs, String[] parameterStrings) {
        CircuitModel.ComponentData block = new CircuitModel.ComponentData(type, name);
        block.setNumericParameters(parameters);
        block.setTerminalXLabels(inputs);
        block.setTerminalYLabels(outputs);
        block.setParameterStrings(parameterStrings);
        return block;
    }

    private static CircuitNetlist electricalNetlist(CircuitTypCore type, String name, double[] parameters) {
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{type}, new int[]{1}, new int[]{0}, new int[]{-1},
                new double[][]{parameters}, 2, 0, 1);
        netlist.setComponentNameToIndex(Map.of(name, 0));
        return netlist;
    }
}
