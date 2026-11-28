package gecko.core.circuit.netlist;

import gecko.core.io.CircuitModel;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GenericTopologyOrderingTest {
    @Test
    void flatLabelsKeepLegacyFirstAppearanceNodeAndSourceBranchOrder() {
        var model = new CircuitModel();
        model.addCircuitComponent(component(1, "R0", 0, "nodeZ", "0", 10));
        model.addCircuitComponent(component(4, "U0", 0, "nodeB", "0", 401, 6));
        model.addCircuitComponent(component(1, "R1", 0, "nodeA", "nodeB", 20));
        var netlist = NetlistBuilder.buildFromCircuitModel(model);
        assertArrayEquals(new int[]{1, 2, 3}, IntStream.range(0, 3).map(netlist::getNodeX).toArray());
        assertArrayEquals(new int[]{0, 0, 2}, IntStream.range(0, 3).map(netlist::getNodeY).toArray());
        assertArrayEquals(new int[]{-1, 1, -1}, IntStream.range(0, 3).map(netlist::getVoltageSourceNumber).toArray());
        assertEquals(0, netlist.getElementIndexByName("R0"));
        assertEquals(1, netlist.getElementIndexByName("U0"));
        assertEquals(2, netlist.getElementIndexByName("R1"));
    }

    @Test
    void unlabeledWireFixtureKeepsLegacyNodeAndBranchArrayOrder() {
        var model = new CircuitModel();
        model.addCircuitComponent(component(4, "U0", 0, "hot", "0", 401, 6));
        model.addCircuitComponent(component(1, "R0", 10, null, null, 10));
        model.addConnection(new CircuitModel.ConnectionData("LK", gridWire(-2)));
        model.addConnection(new CircuitModel.ConnectionData("LK", gridWire(2)));
        var netlist = NetlistBuilder.buildFromCircuitModel(model);
        assertArrayEquals(new int[]{1, 1}, IntStream.range(0, 2).map(netlist::getNodeX).toArray());
        assertArrayEquals(new int[]{0, 0}, IntStream.range(0, 2).map(netlist::getNodeY).toArray());
        assertArrayEquals(new int[]{1, -1}, IntStream.range(0, 2).map(netlist::getVoltageSourceNumber).toArray());
        assertEquals(0, netlist.getElementIndexByName("U0"));
        assertEquals(1, netlist.getElementIndexByName("R0"));
    }

    private static int[][] gridWire(int y) {
        return IntStream.rangeClosed(0, 10).mapToObj(x -> new int[]{x, y}).toArray(int[][]::new);
    }

    private static CircuitModel.ComponentData component(int type, String name, int x,
            String input, String output, double... parameters) {
        var component = new CircuitModel.ComponentData(type, name, x, 0, 503);
        component.setTerminalXLabels(input == null ? new String[0] : new String[]{input});
        component.setTerminalYLabels(output == null ? new String[0] : new String[]{output});
        component.setNumericParameters(parameters);
        for (int i = 0; i < parameters.length; i++) component.setParameter("param" + i, parameters[i]);
        return component;
    }
}
