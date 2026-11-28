package gecko.core.io;

import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.circuit.netlist.NetlistBuilder;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class IpesTopologyContractTest {
    static String component(int type, String name, int x, int y, int direction,
                            String input, String output, String parameters) {
        return """
                e
                <ElementLK>
                typ %d
                idStringDialog %s
                x %d
                y %d
                orientierung %d
                parameter[] %s
                labelAnfangsKnoten[] /%s
                labelEndKnoten[] /%s
                <\\ElementLK>
                """.formatted(type, name, x, y, direction, parameters, input, output);
    }

    static String wire(String domain, String label, String xs, String ys) {
        return """
                verbindung%s
                <Verbindung>
                label %s
                x[] %s
                y[] %s
                enabled true
                <\\Verbindung>
                """.formatted(domain, label, xs, ys);
    }

    // A six-volt source and two/four-ohm divider, saved with the desktop file syntax.
    static String divider(int direction, boolean reverse, boolean labels) {
        String nil = "NIX_NIX_NIX";
        String source = component(4, "V1", 0, 2, 503, labels ? "in" : nil, nil, "401 6");
        int x = direction == 501 || direction == 503 ? 4 : 2;
        int y = direction == 501 || direction == 503 ? 2 : 0;
        // Input terminal at (0,0) or (4,0); output at (4,0) or (4,4).
        String r1 = component(1, "R1", x, y, direction, nil, labels ? "out" : nil, "2");
        String r2 = component(1, "R2", 8, 2, 503, nil, nil, "4");
        String first = direction == 501 || direction == 503
                ? wire("LK", "in", "0 1 2 3 4", "0 0 0 0 0")
                : "";
        String middle = direction == 501 || direction == 503
                ? wire("LK", "out", "4 5 6 6 6 6 6 7 8", "4 4 4 3 2 1 0 0 0")
                : wire("LK", "out", "4 5 6 7 8", "0 0 0 0 0");
        if (direction == 501 || direction == 504) {
            // These orientations reverse R1's input/output terminal order, not its resistance.
            r1 = component(1, "R1", x, y, direction, labels ? "out" : nil, nil, "2");
        }
        String ground = wire("LK", "0", "0 1 2 3 4 5 6 7 8", "4 4 4 4 4 4 4 4 4");
        // Route ground below the vertical R1 to avoid shorting its output.
        if (direction == 501 || direction == 503) {
            ground = wire("LK", "0", "0 0 0 1 2 3 4 5 6 7 8 8 8",
                    "4 5 6 6 6 6 6 6 6 6 6 5 4");
        }
        return "tDURATION 0.003\ndt 0.001\nsolverType 0\n"
                + (reverse ? r2 + r1 + source : source + r1 + r2) + first + middle + ground;
    }

    static Stream<Arguments> dividerVariants() {
        return Stream.of(501, 502, 503, 504).flatMap(direction ->
                Stream.of(false, true).flatMap(reverse ->
                        Stream.of(false, true).flatMap(labels ->
                                Stream.of(false, true).map(gzip ->
                                        Arguments.of(direction, reverse, labels, gzip)))));
    }

    static Path saved(String content, boolean gzip) throws IOException {
        // Preserve diagnostic inputs; do not ask JUnit to delete temporary directories.
        Path file = Files.createTempFile("gecko-ipes-topology-", ".ipes");
        if (gzip) {
            try (OutputStream output = new GZIPOutputStream(Files.newOutputStream(file))) {
                output.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        } else {
            Files.writeString(file, content);
        }
        return file;
    }

    @ParameterizedTest
    @MethodSource("dividerVariants")
    void savedGeometryRunsThroughTheEngineWithCorrectVoltagesAndCurrents(
            int direction, boolean reverse, boolean labels, boolean gzip) throws Exception {
        Path file = saved(divider(direction, reverse, labels), gzip);
        CircuitModel model = new CircuitFileParser().parse(file.toString());
        assertFalse(model.getConnections().isEmpty(), "Saved wires must reach the model");
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        List<Double> outputs = new ArrayList<>();
        engine.setStepTraceListener((time, voltages, currents, netlist) -> {
            int source = netlist.getElementIndexByName("V1");
            int upper = netlist.getElementIndexByName("R1");
            int lower = netlist.getElementIndexByName("R2");
            assertEquals(0, netlist.getNodeY(source));
            assertEquals(0, netlist.getNodeY(lower));
            assertEquals(6, voltages[netlist.getNodeX(source)], 1e-9);
            outputs.add(voltages[netlist.getNodeX(lower)]);
            assertEquals(1, Math.abs(currents[upper]), 1e-9);
            assertEquals(1, currents[lower], 1e-9);
            assertEquals(-1, currents[source], 1e-9);
        });
        SimulationResult result = engine.runSimulation(
                SimulationConfig.builder().circuitFile(file.toString()).build());
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertFalse(outputs.isEmpty());
        outputs.forEach(value -> assertEquals(4, value, 1e-9));
    }

    @Test
    void realLegacyEducationCircuitResolvesItsUnlabelledInternalJunction() throws Exception {
        Path file = Path.of(Objects.requireNonNull(getClass().getResource("/ipes/legacy_ex1.ipes")).toURI());
        CircuitModel model = new CircuitFileParser().parse(file.toString());
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        int source = netlist.getElementIndexByName("U.1");
        int switchElement = netlist.getElementIndexByName("S.2");
        int inductor = netlist.getElementIndexByName("L.3");
        int diode = netlist.getElementIndexByName("D.2");
        assertEquals(7, netlist.getElementCount());
        assertEquals(4, netlist.getNodeMax());
        assertEquals(0, netlist.getNodeY(source), "The saved gnd return is the reference");
        assertEquals(netlist.getNodeY(switchElement), netlist.getNodeX(inductor));
        assertEquals(netlist.getNodeX(inductor), netlist.getNodeY(diode));
        assertNotEquals(0, netlist.getNodeX(inductor), "An unlabelled switch junction is not ground");
        assertEquals(0, netlist.getNodeX(diode));
    }

    @Test
    void trackedWireOnlyFixtureProducesFourVoltsWithoutTerminalLabels() throws Exception {
        Path file = Path.of(Objects.requireNonNull(getClass().getResource("/ipes/wired_divider.ipes")).toURI());
        List<Double> outputs = new ArrayList<>();
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
        engine.setStepTraceListener((time, voltages, currents, netlist) ->
                outputs.add(voltages[netlist.getNodeX(netlist.getElementIndexByName("R2"))]));
        SimulationResult result = engine.runSimulation(SimulationConfig.builder().circuitFile(file.toString()).build());
        assertTrue(result.isSuccess(), result.getErrorMessage());
        assertFalse(outputs.isEmpty());
        outputs.forEach(value -> assertEquals(4, value, 1e-9));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void inputStreamsAcceptTheSamePlainAndGzipFiles(boolean gzip) throws Exception {
        Path file = saved(divider(502, false, false), gzip);
        CircuitModel model;
        try (InputStream input = Files.newInputStream(file)) {
            model = new CircuitFileParser().parse(input, file.toString());
        }
        assertEquals(3, model.getCircuitComponents().size());
        assertEquals(2, model.getConnections().size());
        assertEquals(2, NetlistBuilder.buildFromCircuitModel(model).getNodeMax());
    }

    @Test
    void existingDesktopFixturesRetainTheirWiresAndLabelledTopology() throws Exception {
        for (String name : List.of("buck_simple", "boost_simple")) {
            Path file = Path.of(Objects.requireNonNull(getClass().getResource("/ipes/" + name + ".ipes")).toURI());
            CircuitModel model = new CircuitFileParser().parse(file.toString());
            assertFalse(model.getConnections().isEmpty(), name);
            assertTrue(model.getConnections().stream().anyMatch(c -> c.getType().equals("LK")));
            assertTrue(model.getConnections().stream().anyMatch(c -> c.getType().equals("CONTROL")));
            CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
            assertEquals(6, netlist.getElementCount());
            assertEquals(3, netlist.getNodeMax());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"NIX_NIX_NIX", ""})
    void aMissingTerminalIsNotSilentlyGrounded(String label) throws Exception {
        String content = divider(502, false, false)
                + component(1, "disconnected-R", 20, 20, 503, label, label, "3");
        SimulationResult result = new HeadlessSimulationEngine().runSimulation(
                SimulationConfig.builder().circuitFile(saved(content, false).toString()).build());
        assertEquals(SimulationResult.Status.FAILED, result.getStatus());
        assertTrue(result.getErrorMessage().contains("disconnected-R"), result.getErrorMessage());
        assertEquals(0, result.getTotalTimeSteps());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1 2", "1 bad 3", "null", ""})
    void malformedElectricalWireCoordinatesFailWithSourceContext(String ys) throws Exception {
        String content = divider(502, false, false) + wire("LK", "broken", "1 2 3", ys);
        Path file = saved(content, false);
        var error = assertThrows(CircuitFileParser.CircuitParseException.class,
                () -> new CircuitFileParser().parse(file.toString()));
        assertTrue(error.getMessage().contains(file.toString()), error.getMessage());
        assertTrue(error.getMessage().contains("wire"), error.getMessage());
    }
}
