/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core.simulation;

import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.io.CircuitFileParser;
import gecko.core.io.CircuitModel;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ControlCalculatorFactoryTest {

    private static File findIpesFile(String name) {
        // Try from module root (Maven runs tests from here)
        File f = new File("../../test/resources/ipes/education/" + name);
        if (f.exists()) return f;
        // Try from project root (IDE or manual runs)
        f = new File("src/test/resources/ipes/education/" + name);
        return f;
    }

    @Test
    void buildEmpty_returnsEmptyNetlist() {
        ControlCalculatorFactory factory = new ControlCalculatorFactory();
        ControlCalculatorFactory.BuildResult result = factory.build(List.of());

        assertNotNull(result);
        assertNotNull(result.getControlNetlist());
        assertFalse(result.getControlNetlist().hasCalculators());
    }

    @Test
    void buildNull_returnsEmptyNetlist() {
        ControlCalculatorFactory factory = new ControlCalculatorFactory();
        ControlCalculatorFactory.BuildResult result = factory.build(null);

        assertNotNull(result);
        assertFalse(result.getControlNetlist().hasCalculators());
    }

    @Test
    void buildCircuitResolvesVOLTLabelsThroughTheElectricalNetlist() {
        var model = new CircuitModel();
        var resistor = new CircuitModel.ComponentData(1, "R.highLabel");
        resistor.setTerminalXLabels(new String[]{"99"});
        resistor.setTerminalYLabels(new String[]{"0"});
        resistor.setParameter("param0", 10.0);
        model.addCircuitComponent(resistor);
        var voltage = new CircuitModel.ComponentData(1, "VOLT.1");
        voltage.setTerminalYLabels(new String[]{"voltage"});
        voltage.setParameterStrings(new String[]{"99", "0", "0"});
        model.addControlComponent(voltage);

        var electricalNetlist = gecko.core.circuit.netlist.NetlistBuilder.buildFromCircuitModel(model);
        int expectedNode = electricalNetlist.getNodeX(electricalNetlist.getElementIndexByName("R.highLabel"));
        assertTrue(expectedNode < 99);

        var result = new ControlCalculatorFactory().buildCircuit(model, 0.1);

        assertEquals(expectedNode, result.getMapping().getVoltProbeMappings().getFirst().getNodeA());
        assertEquals(0, result.getMapping().getVoltProbeMappings().getFirst().getNodeB());
    }

    @Test
    void buildSimpleChain_signalToGate() throws Exception {
        // Create a SIGNAL → GATE chain wired through label "gate"
        List<CircuitModel.ComponentData> components = new ArrayList<>();

        // SIGNAL source outputting on label "gate" (typ=4, sub-type=404 rect)
        CircuitModel.ComponentData signal = new CircuitModel.ComponentData(4, "SIGNAL.1");
        signal.setNumericParameters(new double[]{404.0, 1.0, 35000.0, 0.0, 0.0, 0.4, 0.0, 1.0});
        signal.setTerminalXLabels(new String[0]);
        signal.setTerminalYLabels(new String[]{"gate"});
        signal.setParameterStrings(new String[]{"", "", "0"});
        components.add(signal);

        // GATE receiving on label "gate" (typ=6)
        CircuitModel.ComponentData gate = new CircuitModel.ComponentData(6, "GATE.1");
        gate.setNumericParameters(new double[]{1.0});
        gate.setTerminalXLabels(new String[]{"gate"});
        gate.setTerminalYLabels(new String[0]);
        gate.setParameterStrings(new String[]{"S.1", "", "0"});
        components.add(gate);

        ControlCalculatorFactory factory = new ControlCalculatorFactory();
        ControlCalculatorFactory.BuildResult result = factory.build(components);

        ControlNetlist netlist = result.getControlNetlist();
        assertTrue(netlist.hasCalculators());
        assertEquals(2, netlist.getCalculatorCount());

        // Verify GATE's input signal is wired to SIGNAL's output
        AbstractControlCalculatable[] calcs = netlist.getSortedCalculators();
        // Find the gate calculator (has 1 input, 0 outputs)
        AbstractControlCalculatable gateCalc = null;
        for (AbstractControlCalculatable c : calcs) {
            if (c._inputSignal.length == 1 && c._outputSignal.length == 0) {
                gateCalc = c;
            }
        }
        assertNotNull(gateCalc, "Should find GATE calculator");
        assertNotNull(gateCalc._inputSignal[0], "GATE input should be wired");

        // Verify gate mapping
        ControlBlockMapping mapping = result.getMapping();
        assertEquals(1, mapping.getGateMappings().size());
        assertEquals("S.1", mapping.getGateMappings().get(0).getSwitchName());
    }

    @Test
    void parseBuckSimple_8controlComponents() throws Exception {
        File f = findIpesFile("buck_simple.ipes");
        assumeTrue(f.exists(), "buck_simple.ipes not found at " + f.getAbsolutePath());

        CircuitFileParser parser = new CircuitFileParser();
        CircuitModel model = parser.parse(f.getAbsolutePath());

        // Verify 8 control components are parsed
        assertEquals(8, model.getControlComponents().size(),
                "buck_simple should have 8 control components");

        // Verify types: [4, 6, 1, 1, 1, 2, 5, 2]
        int[] expectedTypes = {4, 6, 1, 1, 1, 2, 5, 2};
        for (int i = 0; i < expectedTypes.length; i++) {
            assertEquals(expectedTypes[i], model.getControlComponents().get(i).getType(),
                    "Control component " + i + " type mismatch");
        }

        // Verify SIGNAL.1 parameters
        CircuitModel.ComponentData signal = model.getControlComponents().get(0);
        assertEquals("SIGNAL.1", signal.getName());
        double[] params = signal.getNumericParameters();
        assertTrue(params.length >= 6, "SIGNAL should have at least 6 parameters");
        assertEquals(404.0, params[0], 0.01, "Sub-type should be rectangle");
        assertEquals(1.0, params[1], 0.01, "Amplitude should be 1.0");
        assertEquals(35000.0, params[2], 0.01, "Frequency should be 35kHz");

        // Verify VOLT.1 parameterString
        CircuitModel.ComponentData volt1 = model.getControlComponents().get(2);
        assertEquals("VOLT.1", volt1.getName());
        String[] voltParamStrings = volt1.getParameterStrings();
        assertTrue(voltParamStrings.length >= 2, "VOLT should have parameterStrings");
        assertEquals("1", voltParamStrings[0], "VOLT.1 nodeA should be 1");
        assertEquals("0", voltParamStrings[1], "VOLT.1 nodeB should be 0");

        // Verify SCOPE.1 has 6 input labels
        CircuitModel.ComponentData scope = model.getControlComponents().get(6);
        assertEquals("SCOPE.1", scope.getName());
        assertEquals(6, scope.getTerminalXLabels().length,
                "SCOPE.1 should have 6 input labels");
    }

    @Test
    void buildBuckSimple_fullChain() throws Exception {
        File f = findIpesFile("buck_simple.ipes");
        assumeTrue(f.exists(), "buck_simple.ipes not found at " + f.getAbsolutePath());

        CircuitFileParser parser = new CircuitFileParser();
        CircuitModel model = parser.parse(f.getAbsolutePath());

        ControlCalculatorFactory factory = new ControlCalculatorFactory();
        ControlCalculatorFactory.BuildResult result = factory.build(model.getControlComponents());

        ControlNetlist netlist = result.getControlNetlist();
        assertTrue(netlist.hasCalculators());
        assertEquals(8, netlist.getCalculatorCount(), "Should have 8 calculators");

        // Verify mappings
        ControlBlockMapping mapping = result.getMapping();
        assertEquals(3, mapping.getVoltProbeMappings().size(), "Should have 3 VOLT probes");
        assertEquals(2, mapping.getAmpProbeMappings().size(), "Should have 2 AMP probes");
        assertEquals(1, mapping.getGateMappings().size(), "Should have 1 GATE mapping");
        assertNotNull(mapping.getScopeInfo(), "Should have SCOPE info");
        assertEquals(6, mapping.getScopeInfo().getSignalCount(), "SCOPE should have 6 signals");

        // Verify VOLT probe node mappings
        ControlBlockMapping.VoltProbeMapping voltProbe1 = mapping.getVoltProbeMappings().get(0);
        assertEquals(1, voltProbe1.getNodeA(), "VOLT.1 nodeA should be 1");
        assertEquals(0, voltProbe1.getNodeB(), "VOLT.1 nodeB should be 0 (ground)");

        // Verify GATE mapping
        assertEquals("S.1", mapping.getGateMappings().get(0).getSwitchName());

        // Verify AMP probe component names
        assertEquals("L.1", mapping.getAmpProbeMappings().get(0).getComponentName());
        assertEquals("C.1", mapping.getAmpProbeMappings().get(1).getComponentName());
    }

    @Test
    void scopeExtractor_readsWiredSignals() throws Exception {
        // Build a simple chain: VOLT probe → SCOPE
        List<CircuitModel.ComponentData> components = new ArrayList<>();

        // VOLT probe outputting on "u1"
        CircuitModel.ComponentData volt = new CircuitModel.ComponentData(1, "VOLT.1");
        volt.setNumericParameters(new double[]{0.0});
        volt.setTerminalXLabels(new String[0]);
        volt.setTerminalYLabels(new String[]{"u1"});
        volt.setParameterStrings(new String[]{"1", "0", "0"});
        components.add(volt);

        // SCOPE with 1 input "u1"
        CircuitModel.ComponentData scope = new CircuitModel.ComponentData(5, "SCOPE.1");
        scope.setNumericParameters(new double[0]);
        scope.setTerminalXLabels(new String[]{"u1"});
        scope.setTerminalYLabels(new String[0]);
        scope.setParameterStrings(new String[]{"", "", "0"});
        components.add(scope);

        ControlCalculatorFactory factory = new ControlCalculatorFactory();
        ControlCalculatorFactory.BuildResult result = factory.build(components);

        // Manually set VOLT probe output to 48.0V
        ControlBlockMapping mapping = result.getMapping();
        mapping.getVoltProbeMappings().get(0).getCalculator()._outputSignal[0][0] = 48.0;

        // Extract via ScopeDataExtractor
        ScopeDataExtractor extractor = ScopeDataExtractor.fromScopeInfo(mapping.getScopeInfo());
        assertNotNull(extractor);
        float[] values = extractor.extractCurrentValues();
        assertEquals(1, values.length);
        assertEquals(48.0f, values[0], 0.01f, "SCOPE should read VOLT output");
    }

    @Test
    void parseNodeLabel_handlesVariousFormats() {
        assertEquals(0, ControlCalculatorFactory.parseNodeLabel("0"));
        assertEquals(1, ControlCalculatorFactory.parseNodeLabel("1"));
        assertEquals(3, ControlCalculatorFactory.parseNodeLabel("3"));
        assertEquals(0, ControlCalculatorFactory.parseNodeLabel(""));
        assertEquals(0, ControlCalculatorFactory.parseNodeLabel(null));
        assertEquals(0, ControlCalculatorFactory.parseNodeLabel("abc"));  // non-numeric → ground
    }
}
