package gecko.core.circuit.netlist;

import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class MutualCouplingContractTest {
    @ParameterizedTest
    @ValueSource(doubles={-1,-0.5,0,0.5,1})
    void reciprocalCouplingsRetainTheirSignAndWindingValues(double k) {
        MutualCouplingRegistry registry=new MutualCouplingRegistry();
        var coupling=registry.registerCoupling(2,5,k,4,9);
        assertEquals(6*k,coupling.getMutualInductance(),1e-14);
        assertEquals(k,coupling.getCouplingCoefficient());
        assertEquals(4,coupling.getInductance1());
        assertEquals(9,coupling.getInductance2());
        assertEquals(2,coupling.getInductor1Index());
        assertEquals(5,coupling.getInductor2Index());
        assertTrue(coupling.involves(2));assertTrue(coupling.involves(5));assertFalse(coupling.involves(7));
        assertEquals(5,coupling.getPartner(2));assertEquals(2,coupling.getPartner(5));
        assertEquals(-1,coupling.getPartner(7));
        assertEquals(6*k,registry.getMutualInductance(2,5),1e-14);
        assertEquals(6*k,registry.getMutualInductance(5,2),1e-14);
        assertEquals(k,registry.getCouplingCoefficient(5,2));
        assertTrue(registry.areCoupled(2,5));assertTrue(registry.isCoupled(5));
        assertEquals(List.of(5),registry.getCouplingPartners(2));
        assertEquals(Set.of(2,5),registry.getAllCoupledInductors());
        assertEquals(List.of(coupling),registry.getCouplingsFor(2));
        assertThrows(UnsupportedOperationException.class,()->registry.getAllCouplings().clear());
        assertThrows(UnsupportedOperationException.class,()->registry.getCouplingsFor(2).clear());
        assertThrows(UnsupportedOperationException.class,()->registry.getAllCoupledInductors().clear());
        assertTrue(coupling.toString().contains("L2-L5"));
        assertTrue(registry.toString().contains("count=1"));
    }

    static Stream<Arguments> invalidValues() {
        return Stream.of(Arguments.of(-1,1,0.5,1,1),Arguments.of(0,-1,0.5,1,1),
            Arguments.of(1,1,0.5,1,1),Arguments.of(0,1,1.01,1,1),Arguments.of(0,1,-1.01,1,1),
            Arguments.of(0,1,Double.NaN,1,1),Arguments.of(0,1,Double.POSITIVE_INFINITY,1,1),
            Arguments.of(0,1,0.5,0,1),Arguments.of(0,1,0.5,1,-1),
            Arguments.of(0,1,0.5,Double.NaN,1),Arguments.of(0,1,0.5,1,Double.NaN),
            Arguments.of(0,1,0.5,Double.POSITIVE_INFINITY,1),Arguments.of(0,1,0.5,1,Double.POSITIVE_INFINITY));
    }

    @ParameterizedTest @MethodSource("invalidValues")
    void invalidCouplingCannotEnterTheRegistryOrBypassItsConstructor(int a,int b,double k,double l1,double l2) {
        MutualCouplingRegistry registry=new MutualCouplingRegistry();
        assertThrows(IllegalArgumentException.class,()->registry.registerCoupling(a,b,k,l1,l2));
        assertEquals(0,registry.getCouplingCount());
        assertThrows(IllegalArgumentException.class,()->new MutualCouplingRegistry.Coupling(a,b,k,l1,l2));
    }

    @ParameterizedTest @ValueSource(doubles={1e-300,1e300})
    void finiteMutualInductanceDoesNotUnderflowOrOverflowTheInductanceProduct(double inductance) {
        var c=new MutualCouplingRegistry().registerCoupling(0,1,0.5,inductance,inductance);
        assertEquals(0.5*inductance,c.getMutualInductance(),inductance*1e-14);
    }

    @Test void selfQueriesCannotMatchOrRemoveARealPair() {
        MutualCouplingRegistry registry=new MutualCouplingRegistry();
        registry.registerCoupling(1,2,0.5,1,1);
        assertFalse(registry.areCoupled(1,1));
        assertEquals(0,registry.getMutualInductance(1,1));
        assertEquals(0,registry.getCouplingCoefficient(1,1));
        assertFalse(registry.removeCoupling(1,1));
        assertEquals(1,registry.getCouplingCount());
    }

    @Test void windingRemovalPreservesOtherPairsAndSupportsReuse() {
        MutualCouplingRegistry registry=new MutualCouplingRegistry();
        registry.registerCoupling(1,2,0.5,1,1);
        registry.registerCoupling(1,3,-0.5,1,1);
        assertThrows(IllegalArgumentException.class,()->registry.registerCoupling(2,1,0.25,1,1));
        assertTrue(registry.removeCoupling(2,1)); assertFalse(registry.isCoupled(2));
        assertTrue(registry.isCoupled(1));assertEquals(List.of(3),registry.getCouplingPartners(1));
        assertTrue(registry.removeCoupling(3,1));assertFalse(registry.isCoupled(1));
        assertFalse(registry.removeCoupling(9,10));
        assertFalse(registry.areCoupled(1,3));assertEquals(0,registry.getMutualInductance(1,3));
        assertEquals(0,registry.getCouplingCoefficient(1,3));
        assertTrue(registry.getCouplingsFor(1).isEmpty());assertTrue(registry.getCouplingPartners(1).isEmpty());
        registry.registerCoupling(1,2,0.5,1,1);registry.clear();
        assertEquals(0,registry.getCouplingCount());assertTrue(registry.getAllCoupledInductors().isEmpty());
    }

    @Test void exportedArraysUseElementIndicesAndActualAuxiliaryNumbers() {
        MutualCouplingRegistry registry=new MutualCouplingRegistry();
        registry.registerCoupling(1,3,0.5,4,9);registry.registerCoupling(1,4,-0.25,4,16);
        double[][][] arrays=registry.buildCouplingArrays(new int[]{-1,5,-1,7,2,-1});
        assertEquals(6,arrays[0].length);
        assertArrayEquals(new double[]{7,2},arrays[0][1]);assertArrayEquals(new double[]{3,-2},arrays[1][1]);
        assertArrayEquals(new double[]{5},arrays[0][3]);assertNull(arrays[0][5]);
        arrays[1][1][0]=999;
        assertEquals(3,registry.getMutualInductance(1,3));
        assertEquals(3,registry.getCouplingCoefficient(1,3)*6);
        assertEquals(6,new MutualCouplingRegistry().buildCouplingArrays(new int[6])[0].length);
    }

    @ParameterizedTest @ValueSource(strings={"null","short","zero","negative","duplicate"})
    void invalidAuxiliaryMappingsFailWithAContractError(String mode) {
        MutualCouplingRegistry registry=new MutualCouplingRegistry();
        registry.registerCoupling(1,3,0.5,1,1);
        int[] mapping=switch(mode) {
            case "null" -> null; case "short" -> new int[]{-1,1};
            case "zero" -> new int[]{-1,0,-1,2};
            case "negative" -> new int[]{-1,1,-1,-1};
            default -> new int[]{-1,1,-1,1};
        };
        assertThrows(IllegalArgumentException.class,()->registry.buildCouplingArrays(mapping));
    }

    @Test void pairEqualityIsSymmetricAndOrderIndependent() {
        var a=new MutualCouplingRegistry.Coupling(1,2,0.5,1,1);
        var b=new MutualCouplingRegistry.Coupling(2,1,0.5,1,1);
        var c=new MutualCouplingRegistry.Coupling(2,3,0.5,1,1);
        assertEquals(a,a);assertEquals(a,b);assertEquals(b,a);assertEquals(a.hashCode(),b.hashCode());
        assertNotEquals(a,c);assertNotEquals(a,null);assertNotEquals(a,"pair");
    }
}
