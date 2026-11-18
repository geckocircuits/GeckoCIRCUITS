package gecko.core.io;

import gecko.core.circuit.netlist.*;
import gecko.core.allg.SolverType;
import gecko.core.simulation.solver.MatrixSolver;
import java.io.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static gecko.core.io.IpesTopologyContractTest.*;
import static org.junit.jupiter.api.Assertions.*;

class CoordinateWireContractTest {
    private static CircuitModel parse(String content) throws Exception {
        return new CircuitFileParser().parse(new BufferedReader(new StringReader(content)), "wire-contract.ipes");
    }

    private static CircuitNetlist solvedDivider(String content) throws Exception {
        CircuitModel model = parse(content);
        String[][] before = model.getCircuitComponents().stream()
                .map(c -> new String[]{Arrays.toString(c.getTerminalXLabels()), Arrays.toString(c.getTerminalYLabels())})
                .toArray(String[][]::new);
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        for (int i = 0; i < before.length; i++) {
            assertEquals(before[i][0], Arrays.toString(model.getCircuitComponents().get(i).getTerminalXLabels()));
            assertEquals(before[i][1], Arrays.toString(model.getCircuitComponents().get(i).getTerminalYLabels()));
        }
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());
        solver.buildMatrixA(netlist, 0.001, 0, false);
        solver.buildVectorB(netlist, 0.001, 0, false);
        solver.solve();
        int lower = netlist.getElementIndexByName("R2");
        assertEquals(4, solver.getP()[netlist.getNodeX(lower)], 1e-9);
        assertEquals(0, netlist.getNodeY(lower));
        return netlist;
    }

    @Test
    void interiorWireCrossingsDoNotShortTheOutputToGround() throws Exception {
        solvedDivider(divider(502, false, false)
                + wire("LK", "0", "6 6 6 6 6", "-2 -1 0 1 2"));
    }

    @Test
    void endpointOnWireInteriorMakesATJunction() throws Exception {
        String text = divider(502, false, false)
                + wire("LK", "", "6 6 6", "0 -1 -2")
                + component(1, "branch", 6, -4, 503, "0", "NIX_NIX_NIX", "4");
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(parse(text));
        int branch = netlist.getElementIndexByName("branch");
        int lower = netlist.getElementIndexByName("R2");
        assertEquals(netlist.getNodeX(lower), netlist.getNodeY(branch));
        assertEquals(0, netlist.getNodeX(branch));
    }

    @Test
    void aTerminalAtAnInteriorCrossingJoinsBothWires() throws Exception {
        String text = divider(502, false, false)
                + wire("LK", "", "6 6 6 6 6", "-2 -1 0 1 2")
                + component(1, "junction", 6, 2, 503, "", "0", "4");
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(parse(text));
        int junction = netlist.getElementIndexByName("junction");
        int lower = netlist.getElementIndexByName("R2");
        assertEquals(netlist.getNodeX(lower), netlist.getNodeX(junction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONTROL", "THERM"})
    void coincidentWiresInAnotherDomainCannotGroundElectricalNodes(String domain) throws Exception {
        CircuitNetlist netlist = solvedDivider(divider(502, false, false)
                + wire(domain, "0", "4 5 6 7 8", "0 0 0 0 0"));
        assertEquals(2, netlist.getNodeMax());
    }

    @Test
    void disabledWireCannotShortTheCircuit() throws Exception {
        solvedDivider(divider(502, false, false)
                + wire("LK", "0", "4 5 6 7 8", "0 0 0 0 0")
                        .replace("enabled true", "enabled false"));
    }

    @Test
    void disabledMalformedWireIsNotPartOfTheTopology() throws Exception {
        solvedDivider(divider(502, false, false)
                + wire("LK", "0", "not a coordinate", "").replace("enabled true", "enabled false"));
    }

    @Test
    void touchingComponentTerminalsConnectWithoutAnInterveningWire() throws Exception {
        CircuitNetlist netlist = solvedDivider(divider(502, false, false));
        assertEquals(netlist.getNodeX(netlist.getElementIndexByName("V1")),
                netlist.getNodeX(netlist.getElementIndexByName("R1")));
    }

    static Stream<Arguments> wireOrders() {
        int[][] orders = {{0,1,2},{0,2,1},{1,0,2},{1,2,0},{2,0,1},{2,1,0}};
        return Stream.of(orders).flatMap(order -> Stream.of(false,true).map(reverse -> Arguments.of(order,reverse)));
    }

    @ParameterizedTest
    @MethodSource("wireOrders")
    void connectedWireChainsResolveRegardlessOfOrderAndDirection(int[] order, boolean reverse) throws Exception {
        String text = divider(502, false, false);
        String original = wire("LK", "out", "4 5 6 7 8", "0 0 0 0 0");
        String[] chain = reverse
                ? new String[]{wire("LK", "", "5 4", "0 0"), wire("LK", "", "7 6 5", "0 0 0"),
                        wire("LK", "", "8 7", "0 0")}
                : new String[]{wire("LK", "", "4 5", "0 0"), wire("LK", "", "5 6 7", "0 0 0"),
                        wire("LK", "", "7 8", "0 0")};
        solvedDivider(text.replace(original, chain[order[0]] + chain[order[1]] + chain[order[2]]));
    }

    @Test
    void matchingWireLabelsConnectSeparatedSchematicRegions() throws Exception {
        String original = wire("LK", "out", "4 5 6 7 8", "0 0 0 0 0");
        solvedDivider(divider(502, false, false).replace(original,
                wire("LK", "remote", "4 5", "0 0") + wire("LK", "remote", "7 8", "0 0")));
    }

    @Test
    void explicitTerminalLabelsConnectRemoteComponents() throws Exception {
        solvedDivider(divider(502, false, false)
                + component(1, "remote", 100, 100, 503, "out", "0", "1e30"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "NIX_NIX_NIX", "return"})
    void absentGroundUsesTheDesktopWireReference(String ground) throws Exception {
        CircuitModel model = parse(divider(502, false, false).replace("label 0\n", "label " + ground + "\n"));
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        assertNotEquals(0, netlist.getNumericalNodeOrder().length, "a wired model keeps the desktop reference");
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());
        solver.buildMatrixA(netlist, 0.001, 0, false);
        solver.buildVectorB(netlist, 0.001, 0, false);
        solver.solve();
        int lower = netlist.getElementIndexByName("R2");
        assertEquals(4, solver.getP()[netlist.getNodeX(lower)] - solver.getP()[netlist.getNodeY(lower)], 1e-9);
    }

    // A 1-ohm island between labels isl/isl2 with no conductive path to the divider.
    private static final String ISLAND = component(1, "Risland", 100, 100, 503, "isl", "isl2", "1");

    @Test
    void currentSourceIsNotAReferenceForAnIsland() throws Exception {
        CircuitModel model = parse(divider(502, false, false) + ISLAND
                + component(5, "I1", 200, 200, 503, "out", "isl", "401 1"));
        var error = assertThrows(TopologyValidationException.class, () -> NetlistBuilder.buildFromCircuitModel(model));
        assertEquals("UNCONNECTED_GROUND", error.code());
        assertTrue(error.getMessage().contains("'I1'"), error.getMessage());
    }

    @Test
    void isolatedIslandWithoutCurrentSourceStaysAccepted() throws Exception {
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(parse(divider(502, false, false) + ISLAND));
        MatrixSolver solver = new MatrixSolver(SolverType.SOLVER_BE);
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());
        solver.buildMatrixA(netlist, 0.001, 0, false);
        solver.buildVectorB(netlist, 0.001, 0, false);
        solver.solve();
        int lower = netlist.getElementIndexByName("R2");
        assertEquals(4, solver.getP()[netlist.getNodeX(lower)] - solver.getP()[netlist.getNodeY(lower)], 1e-9);
        int island = netlist.getElementIndexByName("Risland");
        assertEquals(0, solver.getP()[netlist.getNodeX(island)] - solver.getP()[netlist.getNodeY(island)], 1e-9);
    }

    @ParameterizedTest
    @ValueSource(strings = {" 0 ", "\t0\t", "gnd", "GND", " gNd "})
    void whitespaceAroundWireGroundDoesNotCreateAnotherNode(String label) throws Exception {
        solvedDivider(divider(502, false, false).replace("label 0\n", "label " + label + "\n"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false,true})
    void aSingleMissingTerminalReportsItsComponentAndEnd(boolean input) throws Exception {
        String text = divider(502, false, false)
                + component(1, "dangling", 20, 20, 503,
                        input ? "" : "0", input ? "0" : "", "2");
        var error = assertThrows(IllegalArgumentException.class,
                () -> NetlistBuilder.buildFromCircuitModel(parse(text)));
        assertTrue(error.getMessage().contains("dangling"));
        assertTrue(error.getMessage().contains(input ? "input" : "output"));
    }

    @Test
    void generatedNodeNamesCannotCollideWithUserLabels() throws Exception {
        String text = divider(502, false, false).replace("label out\n", "label NIX_NIX_NIX\n");
        for (int i = 0; i < 20; i++) {
            text += wire("LK", "@wire:" + i, "100 101", (100+i) + " " + (100+i));
        }
        assertEquals(2, solvedDivider(text).getNodeMax());
    }

    static Stream<Arguments> malformedConnectionModels() {
        return Stream.of(
                Arguments.of((Object) null), Arguments.of((Object) new int[0][]),
                Arguments.of((Object) new int[][]{{0,0}}),
                Arguments.of((Object) new int[][]{null,{1,1}}),
                Arguments.of((Object) new int[][]{{0},{1,1}}),
                Arguments.of((Object) new int[][]{{0,0,0},{1,1}}));
    }

    @ParameterizedTest
    @MethodSource("malformedConnectionModels")
    void programmaticMalformedWiresFailBeforeMatrixConstruction(int[][] points) throws Exception {
        CircuitModel model = parse(divider(502, false, false));
        model.addConnection(new CircuitModel.ConnectionData("LK", points));
        var error = assertThrows(IllegalArgumentException.class, () -> NetlistBuilder.buildFromCircuitModel(model));
        assertTrue(error.getMessage().contains("wire"), error.getMessage());
    }

    @Test
    void anUnnamedProgrammaticWireCanExtendAnExistingNode() throws Exception {
        CircuitModel model = parse(divider(502, false, false));
        model.addConnection(new CircuitModel.ConnectionData("LK", new int[][]{{6,0},{6,-1}}, null));
        assertEquals(2, NetlistBuilder.buildFromCircuitModel(model).getNodeMax());
    }

    @Test
    void legacyUnknownDirectionUsesTheDesktopNorthSouthDefault() throws Exception {
        solvedDivider(divider(503, false, false).replace("orientierung 503", "orientierung 0"));
    }

    @Test
    void geometricallyJoinedLabelAliasesShareOneNode() throws Exception {
        solvedDivider(divider(502, false, false)
                + wire("LK", "another-output-name", "6 6 6", "0 -1 -2"));
    }
}
