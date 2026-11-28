package gecko.core.circuit.netlist;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import java.util.Map;
import java.util.HashMap;
import java.util.function.IntConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class NetlistValidationContractTest {
    static CircuitNetlist resistor(int x,int y,double[] parameters) {
        CircuitNetlist netlist=new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_R},new int[]{x},new int[]{y},
                new int[]{-1},new double[][]{parameters},1,0,1);
        return netlist;
    }

    @ParameterizedTest
    @CsvSource({"-1,0","0,-1","2,0","0,2"})
    void outOfRangeTerminalsAreNotValid(int x,int y) {
        assertFalse(resistor(x,y,new double[]{2}).isValid());
    }

    @Test
    void missingAndEmptyParametersAreNotReadyForMatrixStamping() {
        assertFalse(resistor(1,0,null).isValid());
        assertFalse(resistor(1,0,new double[0]).isValid());
        assertTrue(resistor(1,0,new double[]{2}).isValid());
    }

    @ParameterizedTest
    @CsvSource({"-1","0","2"})
    void voltageConstraintsRequireAnInRangeAuxiliaryIndex(int index) {
        CircuitNetlist netlist=new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U},new int[]{1},new int[]{0},
                new int[]{index},new double[][]{{401,6}},1,1,1);
        assertFalse(netlist.isValid());
    }

    @Test
    void twoSourcesCannotShareTheSameAuxiliaryUnknown() {
        CircuitNetlist netlist=new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U,CircuitTypCore.LK_U},
                new int[]{1,2},new int[]{0,0},new int[]{1,1},new double[][]{{401,6},{401,3}},2,2,2);
        assertFalse(netlist.isValid());
    }

    @Test
    void reinitializationDropsMappingsResultsTimeAndCouplingsFromThePriorCircuit() {
        CircuitNetlist netlist=new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_LKOP2,CircuitTypCore.LK_LKOP2},
                new int[]{1,2},new int[]{0,0},new int[]{1,2},new double[][]{{1},{2}},2,2,2);
        netlist.registerMutualCoupling(0,1,0.5,1,2);
        netlist.setComponentNameToIndex(Map.of("old",1));
        netlist.storeResults(new double[]{0,1,2},new double[]{3,4});
        netlist.setSimulationTime(5);
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_R},new int[]{1},new int[]{0},
                new int[]{-1},new double[][]{{2}},1,0,1);
        assertEquals(-1,netlist.getElementIndexByName("old"));
        assertArrayEquals(new double[0],netlist.getLastNodeVoltages());
        assertArrayEquals(new double[0],netlist.getLastComponentCurrents());
        assertEquals(0,netlist.getSimulationTime());
        assertTrue(netlist.getAllCouplings().isEmpty());
        assertTrue(netlist.isValid());
    }

    @ParameterizedTest
    @CsvSource({"-1","1"})
    void nameMappingCannotPointOutsideTheNewCircuit(int index) {
        CircuitNetlist netlist=resistor(1,0,new double[]{2});
        assertThrows(IllegalArgumentException.class,()->netlist.setComponentNameToIndex(Map.of("R",index)));
    }

    @Test
    void failedInitializationLeavesThePreviousCircuitIntact() {
        CircuitNetlist netlist=resistor(1,0,new double[]{2});
        netlist.setComponentNameToIndex(Map.of("R",0));
        netlist.storeResults(new double[]{0,4},new double[]{2});
        assertThrows(IllegalArgumentException.class,()->netlist.initNetlist(
                new CircuitTypCore[]{CircuitTypCore.LK_R},new int[]{1},new int[]{0},
                new int[]{-1},new double[][]{{3}},-1,0,1));
        assertEquals(2,netlist.getParameter(0)[0]);
        assertEquals(0,netlist.getElementIndexByName("R"));
        assertArrayEquals(new double[]{0,4},netlist.getLastNodeVoltages());
    }
    static Stream<Arguments> metadataBounds() {
        CircuitNetlist netlist=resistor(1,0,new double[]{2});
        IntConsumer[] readers={index->netlist.getType(index), index->netlist.getNodeX(index),
                index->netlist.getNodeY(index), index->netlist.getParameter(index),
                index->netlist.getVoltageSourceNumber(index)};
        return Stream.of(readers).flatMap(reader->Stream.of(Arguments.of(reader,-1),Arguments.of(reader,1)));
    }

    @ParameterizedTest
    @MethodSource("metadataBounds")
    void elementMetadataRejectsIndicesOutsideTheCircuit(IntConsumer reader,int index) {
        assertThrows(IndexOutOfBoundsException.class,()->reader.accept(index));
    }

    @ParameterizedTest
    @CsvSource({"-1,0","1,0","0,-1","0,1"})
    void parameterAccessRejectsInvalidElementAndParameterIndices(int element,int parameter) {
        CircuitNetlist netlist=resistor(1,0,new double[]{2});
        assertThrows(IndexOutOfBoundsException.class,()->netlist.getElementParam(element,parameter));
        assertThrows(IndexOutOfBoundsException.class,()->netlist.setElementParam(element,parameter,3));
        assertEquals(2,netlist.getParameter(0)[0]);
    }

    @Test
    void unassignedTypeOrParameterRowsCannotBeReadAsAValidCircuit() {
        CircuitNetlist partial=resistor(1,0,null);
        assertThrows(NullPointerException.class,()->partial.getElementParam(0,0));
        assertThrows(NullPointerException.class,()->partial.setElementParam(0,0,3));
        partial.initNetlist(new CircuitTypCore[]{null},new int[]{1},new int[]{0},
                new int[]{-1},new double[][]{{2}},1,0,1);
        assertFalse(partial.isValid());
    }

    @ParameterizedTest
    @EnumSource(value=CircuitTypCore.class,names={"LK_U","LK_LKOP2","TH_TEMP","REL_MMF"})
    void allConstraintTypesValidateTheirAuxiliaryIndex(CircuitTypCore type) {
        CircuitNetlist netlist=new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{type},new int[]{1},new int[]{0},
                new int[]{1},new double[][]{{1}},1,1,1);
        assertTrue(netlist.isValid());
        netlist.initNetlist(new CircuitTypCore[]{type},new int[]{1},new int[]{0},
                new int[]{0},new double[][]{{1}},1,1,1);
        assertFalse(netlist.isValid());
    }

    @Test
    void nameBindingsAreDetachedReadOnlyAndCanBeCleared() {
        CircuitNetlist netlist=resistor(1,0,new double[]{2});
        Map<String,Integer> names=new HashMap<>(Map.of("R",0));
        netlist.setComponentNameToIndex(names);
        names.put("wrong",99);
        assertEquals(-1,netlist.getElementIndexByName("wrong"));
        assertThrows(UnsupportedOperationException.class,()->netlist.getComponentNameToIndex().clear());
        netlist.setComponentNameToIndex(null);
        assertTrue(netlist.getComponentNameToIndex().isEmpty());
    }

}
