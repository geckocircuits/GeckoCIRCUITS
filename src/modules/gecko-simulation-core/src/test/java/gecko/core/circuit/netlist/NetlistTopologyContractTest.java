package gecko.core.circuit.netlist;

import gecko.core.allg.SolverType;
import gecko.core.circuit.SourceType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.io.CircuitModel;
import gecko.core.io.CircuitTopology;
import gecko.core.io.SavedCircuitBlock;
import gecko.core.simulation.solver.MatrixSolver;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class NetlistTopologyContractTest {
    static CircuitModel.ComponentData component(CircuitTypCore type, String name, String x, String y, double value) {
        CircuitModel.ComponentData component = new CircuitModel.ComponentData(type.getTypeNumber(), name);
        component.setTerminalXLabels(new String[]{x});
        component.setTerminalYLabels(new String[]{y});
        component.setParameter("param0", value);
        return component;
    }

    static CircuitModel divider(String ground, int[] order) {
        CircuitModel.ComponentData source = component(CircuitTypCore.LK_U, "V1", "in", ground, SourceType.QUELLE_DC_NEW);
        source.setParameter("param1", 6.0);
        List<CircuitModel.ComponentData> components = List.of(source,
                component(CircuitTypCore.LK_R, "R1", "in", "out", 2),
                component(CircuitTypCore.LK_R, "R2", "out", ground, 4));
        CircuitModel model = new CircuitModel();
        for (int index : order) model.addCircuitComponent(components.get(index));
        return model;
    }

    static Stream<Arguments> permutations() {
        int[][] orders = {{0,1,2},{0,2,1},{1,0,2},{1,2,0},{2,0,1},{2,1,0}};
        return Stream.of("0", "", " 0 ", " ").flatMap(ground ->
                Stream.of(orders).flatMap(order -> Stream.of(SolverType.values())
                        .map(method -> Arguments.of(ground, order, method))));
    }

    @ParameterizedTest
    @MethodSource("permutations")
    void topologyAndSolvedDividerAreIndependentOfElementOrder(String ground, int[] order, SolverType method) {
        CircuitModel model = divider(ground, order);
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        int source = netlist.getElementIndexByName("V1");
        int r1 = netlist.getElementIndexByName("R1");
        int r2 = netlist.getElementIndexByName("R2");
        assertEquals(3, netlist.getElementCount());
        assertEquals(2, netlist.getNodeMax(), "Only in, out and the shared ground node exist");
        assertEquals(0, netlist.getNodeY(source));
        assertEquals(0, netlist.getNodeY(r2));
        assertEquals(netlist.getNodeX(source), netlist.getNodeX(r1));
        assertEquals(netlist.getNodeY(r1), netlist.getNodeX(r2));
        assertNotEquals(netlist.getNodeX(r1), netlist.getNodeY(r1));
        assertEquals(1, netlist.getVoltageSourceMax());
        assertEquals(1, netlist.getVoltageSourceNumber(source));
        assertEquals(-1, netlist.getVoltageSourceNumber(r1));
        assertEquals(CircuitTypCore.LK_U, netlist.getType(source));

        MatrixSolver solver = new MatrixSolver(method);
        solver.initializeMatrices(netlist.getNodeMax(), netlist.getVoltageSourceMax(), netlist.getElementCount());
        solver.buildMatrixA(netlist, 1e-4, 0, false);
        solver.buildVectorB(netlist, 1e-4, 0, false);
        solver.solve();
        double[] voltages = solver.getP();
        assertEquals(0, voltages[0], 1e-10);
        assertEquals(6, voltages[netlist.getNodeX(source)], 1e-10);
        assertEquals(4, voltages[netlist.getNodeX(r2)], 1e-10, "6 V across 2 + 4 ohms gives a 4 V divider output");
        assertEquals(-1, voltages[netlist.getNodeMax() + 1], 1e-10, "The voltage source supplies 1 A");
        assertEquals(ground, model.getCircuitComponents().get(source).getTerminalYLabels()[0],
                "Canonicalizing labels must not mutate the parsed model");
    }

    @Test
    void explicitGroundAliasesAndIndependentBranchesShareNodeZero() {
        CircuitModel model = divider("0", new int[]{0,1,2});
        model.getCircuitComponents().get(2).setTerminalYLabels(new String[]{""});
        model.addCircuitComponent(component(CircuitTypCore.LK_R, "R3", "other", "0", 3));
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        assertEquals(0, netlist.getNodeY(0));
        assertEquals(0, netlist.getNodeY(2));
        assertEquals(0, netlist.getNodeY(3));
        assertEquals(3, netlist.getNodeMax());
        assertNotEquals(netlist.getNodeX(3), netlist.getNodeX(0));
    }

    @ParameterizedTest
    @EnumSource(value=CircuitTypCore.class, names={"LK_U","LK_LKOP2","TH_TEMP","REL_MMF"})
    void everyMnaConstraintGetsAnAuxiliaryUnknown(CircuitTypCore type) {
        CircuitModel model = new CircuitModel();
        model.addCircuitComponent(component(type, "constraint", "p", "0", 1));
        CircuitNetlist netlist = NetlistBuilder.buildFromCircuitModel(model);
        assertEquals(1, netlist.getVoltageSourceMax());
        assertEquals(1, netlist.getVoltageSourceNumber(0));
    }

    static Stream<Arguments> invalidModels() {
        CircuitModel duplicate = divider("0", new int[]{0,1,2});
        duplicate.addCircuitComponent(component(CircuitTypCore.LK_R, "R1", "in", "0", 2));
        CircuitModel nullLabel = divider("0", new int[]{0,1,2});
        nullLabel.getCircuitComponents().get(1).setTerminalXLabels(new String[]{null});
        CircuitModel manyX = divider("0", new int[]{0,1,2});
        manyX.getCircuitComponents().get(1).setTerminalXLabels(new String[]{"in","unmapped"});
        CircuitModel manyY = divider("0", new int[]{0,1,2});
        manyY.getCircuitComponents().get(1).setTerminalYLabels(new String[]{"out","unmapped"});
        CircuitModel floating = divider("0", new int[]{0,1,2});
        floating.addCircuitComponent(component(CircuitTypCore.LK_R, "island", "floating-a", "floating-b", 2));
        CircuitModel noGround = divider("return", new int[]{0,1,2});
        CircuitModel shortedSource = divider("0", new int[]{0,1,2});
        shortedSource.getCircuitComponents().get(0).setTerminalXLabels(new String[]{"0"});
        CircuitModel missingTopology = new CircuitModel();
        CircuitModel.ComponentData resistor = new CircuitModel.ComponentData(1, "unwired");
        resistor.setParameter("resistance", 2.0);
        missingTopology.addCircuitComponent(resistor);
        CircuitModel nullComponent = new CircuitModel();
        nullComponent.addCircuitComponent(null);
        return Stream.of(Arguments.of(duplicate,"R1"), Arguments.of(nullLabel,"R1"),
                Arguments.of(manyX,"R1"), Arguments.of(manyY,"R1"), Arguments.of(floating,"island"),
                Arguments.of(noGround,"ground"), Arguments.of(shortedSource,"V1"),
                Arguments.of(missingTopology,"unwired"), Arguments.of(nullComponent,"component"));
    }

    @ParameterizedTest
    @MethodSource("invalidModels")
    void invalidTopologyFailsBeforeTheSolverWithUsefulContext(CircuitModel model, String context) {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> NetlistBuilder.buildFromCircuitModel(model));
        assertTrue(error.getMessage().toLowerCase().contains(context.toLowerCase()), error.getMessage());
    }

    @ParameterizedTest
    @EnumSource(value=CircuitTypCore.class,names={"LK_I","TH_FLOW"})
    void idealCurrentSourcesDoNotCreateAPotentialReference(CircuitTypCore type) {
        CircuitModel model=new CircuitModel();
        CircuitModel.ComponentData source=component(type,"current-only","floating","0",SourceType.QUELLE_DC_NEW);
        source.setParameter("param1",3);
        model.addCircuitComponent(source);
        IllegalArgumentException error=assertThrows(IllegalArgumentException.class,
                ()->NetlistBuilder.buildFromCircuitModel(model));
        assertTrue(error.getMessage().contains("current-only"));
    }

    @ParameterizedTest
    @EnumSource(value=CircuitTypCore.class,names={"LK_I","TH_FLOW"})
    void aResistivePathProvidesTheCurrentDrivenNodesReference(CircuitTypCore type) {
        CircuitModel model=new CircuitModel();
        model.addCircuitComponent(component(type,"source","live","0",SourceType.QUELLE_DC_NEW));
        model.addCircuitComponent(component(CircuitTypCore.LK_R,"load","live","0",2));
        CircuitNetlist netlist=NetlistBuilder.buildFromCircuitModel(model);
        assertTrue(netlist.isValid());
        assertEquals(netlist.getNodeX(0),netlist.getNodeX(1));
        assertEquals(0,netlist.getVoltageSourceMax());
    }
    @Test
    void legacyLabelsRemainIndependentAcrossSavedSheets() {
        var model = new CircuitModel();
        var first = component(CircuitTypCore.LK_R, "first", "local", "0", 2);
        var second = component(CircuitTypCore.LK_R, "second", "local", "0", 3);
        first.setSavedBlock(SavedCircuitBlock.parse("ElementLK", new String[]{"parentSheetIdentifier 101"}));
        second.setSavedBlock(SavedCircuitBlock.parse("ElementLK", new String[]{"parentSheetIdentifier 202"}));
        model.addCircuitComponent(first);
        model.addCircuitComponent(second);
        var topology = CircuitTopology.resolve(model);
        assertNotEquals(topology.group(first, CircuitTopology.Side.INPUT, 0),
                topology.group(second, CircuitTopology.Side.INPUT, 0));
        var netlist = NetlistBuilder.buildFromCircuitModel(model);
        assertNotEquals(netlist.getNodeX(0), netlist.getNodeX(1));
        assertEquals(0, netlist.getNodeY(0));
        assertEquals(0, netlist.getNodeY(1));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 101})
    void sameSheetLegacyAnalogueRowsShareTheirNamedNode(long sheet) {
        var model = new CircuitModel();
        var resistor = component(CircuitTypCore.LK_R, "load", "local", "0", 10);
        var capacitor = component(CircuitTypCore.TH_CTH, "state", "local", "0", 0.02);
        var saved = SavedCircuitBlock.parse("ElementLK", new String[]{"parentSheetIdentifier " + sheet});
        resistor.setSavedBlock(saved);
        capacitor.setSavedBlock(saved);
        model.addCircuitComponent(resistor);
        model.addCircuitComponent(capacitor);
        var netlist = NetlistBuilder.buildFromCircuitModel(model);
        assertEquals(netlist.getNodeX(0), netlist.getNodeX(1));
        assertEquals(1, netlist.getNodeMax());
    }

}
