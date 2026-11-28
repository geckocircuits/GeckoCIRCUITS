package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class MatrixLifecycleContractTest {
    static MatrixSolver solved(int nodes) {
        MatrixSolver solver=new MatrixSolver(SolverType.SOLVER_BE);
        solver.initializeMatrices(nodes,0,nodes+1);
        for(int i=0;i<solver.getMatrixSize();i++) {solver.getSystemMatrix()[i][i]=2;solver.getb()[i]=6;}
        solver.solve();return solver;
    }
    @ParameterizedTest @ValueSource(ints={0,1,3})
    void reinitializationReplacesFactorizationAndAllStateArrays(int nodes) {
        MatrixSolver solver=solved(1);double[][] old=solver.getSystemMatrix();
        solver.initializeMatrices(nodes,0,nodes+1);
        assertNotSame(old,solver.getSystemMatrix());
        for(double[] state:List.of(solver.getP(),solver.getPALT(),solver.getPALTALT(),solver.getPALTALTALT(),
                solver.getIALT(),solver.getIALTALT(),solver.getIALTALTALT()))
            assertArrayEquals(new double[state.length],state);
        for(int i=0;i<solver.getMatrixSize();i++){solver.getSystemMatrix()[i][i]=5;solver.getb()[i]=10;}
        solver.solve();
        for(double v:solver.getP())assertEquals(2,v,1e-12);
    }
    @Test void aReinitializedZeroMatrixCannotReuseAnEarlierSuccessfulSolution() {
        MatrixSolver solver=solved(1);solver.initializeMatrices(1,0,2);
        assertThrows(RuntimeException.class,solver::solve);
        assertArrayEquals(new double[2],solver.getP());
    }
    static Stream<Arguments> badDimensions() {
        return Stream.of(Arguments.of(-1,0,0),Arguments.of(0,-1,0),Arguments.of(0,0,-1),
            Arguments.of(Integer.MAX_VALUE,1,0));
    }
    @ParameterizedTest @MethodSource("badDimensions")
    void invalidDimensionsFailBeforeDiscardingThePreviousValidState(int nodes,int sources,int elements) {
        MatrixSolver solver=solved(1);double[][] original=solver.getSystemMatrix();double[] p=solver.getP().clone();
        assertThrows(IllegalArgumentException.class,()->solver.initializeMatrices(nodes,sources,elements));
        assertSame(original,solver.getSystemMatrix());assertArrayEquals(p,solver.getP());
    }
    @ParameterizedTest @ValueSource(doubles={Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY})
    void nonfiniteRightHandSideCannotPublishNonfiniteResults(double value) {
        MatrixSolver solver=solved(1);double[] original=solver.getP().clone();solver.getb()[1]=value;
        assertThrows(IllegalStateException.class,solver::solve);assertArrayEquals(original,solver.getP());
    }
    @ParameterizedTest @ValueSource(doubles={Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY})
    void nonfiniteSystemMatrixCannotPublishNonfiniteResults(double value) {
        MatrixSolver solver=solved(1);double[] original=solver.getP().clone();
        solver.getSystemMatrix()[1][1]=value;solver.setMatrixChanged();
        assertThrows(IllegalStateException.class,solver::solve);assertArrayEquals(original,solver.getP());
    }
    @Test void changingOnlyTheRightHandSideUsesTheSameValidMatrix() {
        MatrixSolver solver=solved(1);solver.getb()[0]=8;solver.getb()[1]=10;solver.solve();
        assertArrayEquals(new double[]{4,5},solver.getP(),1e-12);
        solver.getSystemMatrix()[0][0]=4;solver.setMatrixChanged();solver.solve();
        assertArrayEquals(new double[]{2,5},solver.getP(),1e-12);
    }
    @Test void nullNumericalMethodIsRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class,()->new MatrixSolver(null));
    }
    @Test
    void anOverflowedSolutionCannotPartiallyReplaceThePreviousSolution() {
        var solver = solved(1);
        double[] original = solver.getP().clone();
        solver.getSystemMatrix()[1][1] = 0.5;
        solver.getb()[1] = Double.MAX_VALUE;
        solver.setMatrixChanged();
        assertThrows(IllegalStateException.class, solver::solve);
        assertArrayEquals(original, solver.getP());
    }

    @Test
    void uninitializedAndMalformedMatrixStorageIsRejected() {
        assertThrows(IllegalStateException.class, new MatrixSolver(SolverType.SOLVER_BE)::solve);
        for (double[] row : new double[][]{null, new double[1]}) {
            var solver = solved(1);
            solver.getSystemMatrix()[1] = row;
            solver.setMatrixChanged();
            assertThrows(IllegalStateException.class, solver::solve);
            assertArrayEquals(new double[]{3, 3}, solver.getP());
        }
    }

}
