package gecko.core.circuit.netlist;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class NodeConnectivityContractTest {
    @ParameterizedTest
    @CsvSource({"1,2,1,3,1","1,2,3,1,1","1,2,2,3,2","1,2,3,2,2","1,2,3,4,-1","0,2,3,0,0"})
    void sharedNodeLookupChecksBothTerminalsAndGround(int x1, int y1, int x2, int y2, int shared) {
        NodeIndexer nodes = new NodeIndexer(new int[]{x1,x2}, new int[]{y1,y2});
        assertEquals(shared >= 0, nodes.shareCommonNode(0,1));
        assertEquals(shared, nodes.getSharedNode(0,1));
    }

    @Test
    void adjacencyIncludesSelfLoopsOnceAndPreservesElementIndices() {
        NodeIndexer nodes = new NodeIndexer(new int[]{0,2,2}, new int[]{2,2,3});
        assertEquals(Map.of(0,List.of(0),2,List.of(0,1,2),3,List.of(2)), nodes.buildNodeToElementsMap());
        assertEquals(List.of(0,1,2), nodes.findElementsAtNode(2));
        assertEquals(List.of(), nodes.findElementsAtNode(99));
        assertEquals(3, nodes.getKnotenMAX());
        assertEquals(4, nodes.getTotalNodeCount());
        assertEquals(3, nodes.getElementCount());
    }

    @Test
    void changingTheHighestTerminalRecomputesDimensionsWithoutPhantomNodes() {
        NodeIndexer nodes = new NodeIndexer(new int[]{8,8}, new int[]{1,2});
        nodes.setNodes(0,1,1);
        assertEquals(8,nodes.getKnotenMAX(), "The other element still uses node 8");
        nodes.setNodes(1,2,0);
        assertEquals(2,nodes.getKnotenMAX());
        assertEquals(3,nodes.getTotalNodeCount());
        nodes.setNodes(0,9,0);
        assertEquals(9,nodes.getKnotenMAX());
        nodes.recalculateMaxNode();
        assertEquals(9,nodes.getKnotenMAX());
    }

    @Test
    void topologyArraysAreDetachedAndInvalidInputCanBeIdentifiedAndRepaired() {
        int[] x={-1,3}; int[] y={0,2};
        NodeIndexer nodes=new NodeIndexer(x,y);
        x[1]=100; y[1]=100;
        assertFalse(nodes.isValid());
        assertEquals(3,nodes.getNodeX(1));
        assertEquals(2,nodes.getNodeY(1));
        nodes.setNodes(0,1,0);
        assertTrue(nodes.isValid());
        int[] copy=nodes.getAllNodeX();copy[0]=100;
        copy=nodes.getAllNodeY();copy[0]=100;
        assertEquals(1,nodes.getNodeX(0));
        assertEquals(0,nodes.getNodeY(0));
    }

    @ParameterizedTest
    @CsvSource({"-1,0,0","2,0,0","0,-1,0","0,0,-1"})
    void invalidNodeUpdatesLeaveTopologyUnchanged(int element, int x, int y) {
        NodeIndexer nodes = new NodeIndexer(2);
        assertThrows(RuntimeException.class, () -> nodes.setNodes(element,x,y));
        assertArrayEquals(new int[]{0,0},nodes.getAllNodeX());
        assertArrayEquals(new int[]{0,0},nodes.getAllNodeY());
    }

    @Test
    void constructorsRejectMismatchedArraysAndNegativeCapacity() {
        assertThrows(IllegalArgumentException.class,()->new NodeIndexer(null,new int[0]));
        assertThrows(IllegalArgumentException.class,()->new NodeIndexer(new int[0],null));
        assertThrows(IllegalArgumentException.class,()->new NodeIndexer(new int[1],new int[0]));
        assertThrows(IllegalArgumentException.class,()->new NodeIndexer(-1));
        NodeIndexer empty = new NodeIndexer(0);
        assertTrue(empty.isValid());
        assertEquals(1,empty.getTotalNodeCount());
    }
}
