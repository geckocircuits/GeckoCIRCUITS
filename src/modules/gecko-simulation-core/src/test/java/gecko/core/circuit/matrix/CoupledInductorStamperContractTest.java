package gecko.core.circuit.matrix;

import gecko.core.allg.SolverType;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class CoupledInductorStamperContractTest {
    @ParameterizedTest @EnumSource(SolverType.class)
    void shortParameterArraysStillStampAValidWinding(SolverType method) {
        double[][] a=new double[4][4];
        new CoupledInductorStamper().stampMatrixA(a,1,0,3,new double[]{2},0.01,method);
        double alpha=method==SolverType.SOLVER_BE?1:method==SolverType.SOLVER_TRZ?2:1.5;
        assertEquals(-200*alpha,a[3][3]);
        assertEquals(1,a[1][3]);assertEquals(-1,a[0][3]);
        assertEquals(1,a[3][1]);assertEquals(-1,a[3][0]);
        assertEquals(0,a[1][1],"No independent conductance may bypass the winding constraint");
    }

    @ParameterizedTest @ValueSource(strings={"missing","empty","zero","negative","nan","infinite","zero-dt","negative-dt","nan-dt","infinite-dt"})
    void invalidInputsFailBeforeChangingAMatrixOrVector(String input) {
        double[] parameters=switch(input) {
            case "missing"->null;case "empty"->new double[0];case "zero"->new double[]{0};
            case "negative"->new double[]{-1};case "nan"->new double[]{Double.NaN};
            case "infinite"->new double[]{Double.POSITIVE_INFINITY};default->new double[]{1};
        };
        double dt=switch(input) {case "zero-dt"->0;case "negative-dt"->-1;
            case "nan-dt"->Double.NaN;case "infinite-dt"->Double.POSITIVE_INFINITY;default->0.01;};
        var stamper=new CoupledInductorStamper();double[][] a=new double[3][3];double[] b=new double[3];
        assertThrows(IllegalArgumentException.class,()->stamper.stampMatrixA(a,1,0,2,parameters,dt,SolverType.SOLVER_BE));
        assertThrows(IllegalArgumentException.class,()->stamper.stampVectorB(b,1,0,2,parameters,dt,0,new double[]{1}));
        for(double[] row:a) assertArrayEquals(new double[3],row);
        assertArrayEquals(new double[3],b);
    }

    static Stream<Arguments> histories() {
        return Stream.of(Arguments.of((Object)null,0.0),Arguments.of(new double[0],0.0),
            Arguments.of(new double[]{2},-400.0),Arguments.of(new double[]{-2},400.0));
    }
    @ParameterizedTest @MethodSource("histories")
    void standaloneBackwardEulerHistoryIsInTheAuxiliaryEquation(double[] history,double expected) {
        double[] b=new double[4];
        new CoupledInductorStamper().stampVectorB(b,1,0,3,new double[]{2},0.01,0,history);
        assertArrayEquals(new double[]{0,0,0,expected},b);
    }
}
