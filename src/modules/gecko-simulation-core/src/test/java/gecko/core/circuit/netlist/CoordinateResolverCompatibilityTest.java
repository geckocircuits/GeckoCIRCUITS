package gecko.core.circuit.netlist;

import gecko.core.io.CircuitModel;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CoordinateResolverCompatibilityTest {
    private static CircuitModel.ComponentData component(String name, int x, int y, int orientation) {
        return new CircuitModel.ComponentData(1, name, x, y, orientation);
    }

    @Test
    void coincidentTerminalsShareLabelsInEachDesktopOrientation() {
        for (int direction : new int[]{501, 502, 503, 504, 999}) {
            var components = List.of(component("source", 0, 0, direction), component("load", 0, 0, direction));
            var resolved = CoordinateTopologyResolver.resolve(components, List.of(), new String[][]{{"hot", "0"}, {"", ""}});
            assertArrayEquals(new String[]{"hot", "0"}, resolved[0]);
            assertArrayEquals(resolved[0], resolved[1]);
        }
    }

    @Test
    void wireChainsCarryLabelsAndIgnoreOtherDomains() {
        var components = List.of(component("left", 0, 0, 503), component("right", 10, 0, 503));
        var wires = List.of(new CircuitModel.ConnectionData("LK", horizontal(-2), null),
                new CircuitModel.ConnectionData("LK", horizontal(2), " gnd "),
                new CircuitModel.ConnectionData("CONTROL", new int[][]{{0, -2}, {0, 2}}, "ignored"));
        var result = CoordinateTopologyResolver.resolve(components, wires, new String[][]{{"hot", "return"}, {"", ""}});
        assertArrayEquals(new String[]{"hot", "0"}, result[0]);
        assertArrayEquals(result[0], result[1]);
    }

    @Test
    void remoteLabelsConnectAndGroundWinsOverAnAlias() {
        var components = List.of(component("left", 0, 0, 503), component("right", 10, 0, 503));
        var result = CoordinateTopologyResolver.resolve(components,
                List.of(new CircuitModel.ConnectionData("LK", horizontal(2), "0")),
                new String[][]{{"shared", "return"}, {"shared", "otherReturn"}});
        assertEquals(result[0][0], result[1][0]);
        assertEquals("0", result[0][1]);
        assertEquals("0", result[1][1]);
    }

    @Test
    void generatedNamesDoNotAliasUserLabels() {
        var components = List.of(component("first", 0, 0, 503), component("second", 0, 0, 503),
                component("named", 20, 0, 503));
        var result = CoordinateTopologyResolver.resolve(components, List.of(),
                new String[][]{{"", ""}, {"", ""}, {"@wire:1", "@wire:0"}});
        assertArrayEquals(result[0], result[1]);
        assertNotEquals(result[0][0], result[0][1]);
        for (String generated : result[0]) {
            assertNotEquals("@wire:1", generated);
            assertNotEquals("@wire:0", generated);
        }
        assertArrayEquals(new String[]{"@wire:1", "@wire:0"}, result[2]);
    }

    @Test
    void endpointJunctionsJoinButUnconnectedInteriorCrossingsDoNot() {
        var components = List.of(component("first", 0, 0, 503), component("second", 10, 0, 503));
        var top = new CircuitModel.ConnectionData("LK", horizontal(-2), "top");
        var bottom = new CircuitModel.ConnectionData("LK", horizontal(2), "bottom");
        var crossing = new CircuitModel.ConnectionData("LK", new int[][]{{5, -3}, {5, -2}, {5, 0}, {5, 2}, {5, 3}}, "cross");
        var labels = new String[][]{{"", ""}, {"", ""}};
        var separate = CoordinateTopologyResolver.resolve(components, List.of(top, bottom, crossing), labels);
        assertNotEquals(separate[0][0], separate[0][1]);
        var junction = new CircuitModel.ConnectionData("LK", new int[][]{{5, -2}, {5, 0}, {5, 2}}, "");
        var joined = CoordinateTopologyResolver.resolve(components, List.of(top, bottom, junction), labels);
        assertEquals(joined[0][0], joined[0][1]);
    }

    @Test
    void malformedWiresAndDanglingTerminalsFailWithTheirCause() {
        var components = List.of(component("R", 0, 0, 503));
        for (int[][] points : new int[][][]{null, {{0, 0}}, {null, {0, 1}}, {{0, 0, 0}, {0, 1}}}) {
            assertThrows(IllegalArgumentException.class, () -> CoordinateTopologyResolver.resolve(components,
                    List.of(new CircuitModel.ConnectionData("LK", points)), new String[][]{{"a", "b"}}));
        }
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CoordinateTopologyResolver.resolve(components,
                List.of(), new String[][]{{"", "b"}})).getMessage().contains("input"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CoordinateTopologyResolver.resolve(components,
                List.of(), new String[][]{{"a", ""}})).getMessage().contains("output"));
    }

    private static int[][] horizontal(int y) {
        return IntStream.rangeClosed(0, 10).mapToObj(x -> new int[]{x, y}).toArray(int[][]::new);
    }
}
