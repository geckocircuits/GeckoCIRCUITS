package gecko.core.circuit.netlist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class LabelMappingContractTest {
    static void assertConsistent(LabelResolver resolver) {
        String[] labels=resolver.getLabelList();
        assertEquals(resolver.getLabelCount(),resolver.getAllIndices().size());
        for(String label:resolver.getAllLabels()) {
            int index=resolver.getIndex(label);
            assertEquals(label,resolver.getLabel(index));
            assertEquals(label,labels[index]);
        }
        for(int index:resolver.getAllIndices()) assertEquals(index,resolver.getIndex(resolver.getLabel(index)));
    }

    @Test
    void duplicateArrayLabelsUseTheSameReplacementRulesAsAddLabel() {
        LabelResolver resolver=new LabelResolver(new String[]{"a",null,"a",""});
        assertEquals(2,resolver.getIndex("a"));
        assertFalse(resolver.hasLabelAtIndex(0));
        assertNull(resolver.getLabelList()[0]);
        assertConsistent(resolver);
    }

    @Test
    void emptyMapHasNoPhantomIndexAndMapStorageIsDetached() {
        LabelResolver empty=new LabelResolver(Map.of());
        assertArrayEquals(new String[0],empty.getLabelList());
        Map<String,Integer> map=new HashMap<>(Map.of("a",2));
        LabelResolver resolver=new LabelResolver(map);
        map.put("b",3);
        assertFalse(resolver.hasLabel("b"));
        String[] copy=resolver.getLabelList();copy[2]="other";
        assertEquals("a",resolver.getLabel(2));
        assertConsistent(resolver);
    }

    static Stream<Arguments> invalidMaps() {
        Map<String,Integer> nullIndex=new HashMap<>();nullIndex.put("a",null);
        Map<String,Integer> nullLabel=new HashMap<>();nullLabel.put(null,0);
        return Stream.of(Arguments.of((Object)null),Arguments.of(Map.of("a",-1)),
                Arguments.of(nullIndex),Arguments.of(nullLabel),Arguments.of(Map.of("",0)),
                Arguments.of(Map.of("a",0,"b",0)),Arguments.of(Map.of("a",Integer.MAX_VALUE)));
    }

    @ParameterizedTest
    @MethodSource("invalidMaps")
    void ambiguousOrInvalidTerminalMapsAreRejected(Map<String,Integer> mapping) {
        assertThrows(IllegalArgumentException.class,()->new LabelResolver(mapping));
    }

    @Test
    void mergeResolvesBothLabelAndIndexConflictsWithoutChangingEitherInput() {
        LabelResolver first=new LabelResolver(Map.of("a",0,"b",1));
        LabelResolver other=new LabelResolver(Map.of("c",0,"a",2));
        LabelResolver result=first.merge(other);
        assertEquals(Set.of("a","b","c"),result.getAllLabels());
        assertEquals("c",result.getLabel(0));
        assertEquals(2,result.getIndex("a"));
        assertEquals(0,first.getIndex("a"));
        assertFalse(first.hasLabel("c"));
        assertFalse(other.hasLabel("b"));
        assertConsistent(result);
    }

    @Test
    void movingReplacingAndRemovingLabelsKeepsAllViewsConsistent() {
        LabelResolver resolver=new LabelResolver(new String[]{"a","b","c"});
        resolver.addLabel("a",3);
        resolver.addLabel("new",1);
        assertFalse(resolver.hasLabel("b"));
        assertNull(resolver.getLabel(0));
        assertConsistent(resolver);
        assertTrue(resolver.removeLabel("c"));
        assertFalse(resolver.removeLabel("missing"));
        assertTrue(resolver.removeLabelAtIndex(3));
        assertFalse(resolver.removeLabelAtIndex(99));
        assertConsistent(resolver);
        resolver.clear();
        assertEquals(0,resolver.getLabelCount());
        assertArrayEquals(new String[0],resolver.getLabelList());
    }

    @Test
    void lookupValidationAndSearchUseActualLabelIdentity() {
        LabelResolver resolver=new LabelResolver();
        resolver.addLabel("phase-A",2);resolver.addLabel("phase-B",4);
        assertEquals(Set.of("phase-A","phase-B"),Set.copyOf(resolver.findLabelsMatching("phase-.*")));
        assertEquals(Set.of("phase-A","phase-B"),Set.copyOf(resolver.findLabelsWithPrefix("phase")));
        assertEquals(List.of("missing"),resolver.validateLabels(List.of("phase-A","missing")));
        assertEquals(-1,resolver.getIndex("missing"));
        assertFalse(resolver.hasLabel(null));
        assertFalse(resolver.hasLabelAtIndex(3));
        assertEquals(2,resolver.getIndexOrThrow("phase-A","probe"));
        RuntimeException error=assertThrows(RuntimeException.class,()->resolver.getIndexOrThrow("missing","probe"));
        assertTrue(error.getMessage().contains("probe"));
        assertTrue(error.getMessage().contains("missing"));
        assertThrows(UnsupportedOperationException.class,()->resolver.getAllLabels().clear());
        assertThrows(IllegalArgumentException.class,()->resolver.addLabel(null,0));
        assertThrows(IllegalArgumentException.class,()->resolver.addLabel("",0));
        assertThrows(IllegalArgumentException.class,()->resolver.addLabel("x",-1));
    }
    @Test
    void bindingIdentityDoesNotDependOnConstructionOrder() {
        LabelResolver array=new LabelResolver(new String[]{"a","b"});
        LabelResolver map=new LabelResolver(Map.of("b",1,"a",0));
        assertEquals(array,map);
        assertEquals(array.hashCode(),map.hashCode());
        assertEquals(array,array);
        assertNotEquals(array,null);
        assertNotEquals(array,"a");
        assertNotEquals(array,new LabelResolver(Map.of("a",1)));
        assertTrue(Set.of(array).contains(map), "Equivalent terminal bindings can be used as value keys");
    }

    @Test
    void nullArrayIsAnEmptyResolverAndInputArraysAreDetached() {
        assertArrayEquals(new String[0],new LabelResolver((String[])null).getLabelList());
        String[] labels={"a","b"};
        LabelResolver resolver=new LabelResolver(labels);
        labels[0]="changed";
        assertEquals(0,resolver.getIndex("a"));
        assertFalse(resolver.hasLabel("changed"));
        assertConsistent(resolver);
    }

}
