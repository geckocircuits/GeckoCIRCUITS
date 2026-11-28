package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class MagneticMatrixContractTest {
    static CircuitNetlist windings(double k) {
        CircuitNetlist netlist=new CircuitNetlist();
        double[] primary=new double[13],secondary=new double[13];
        primary[0]=4;secondary[0]=9;
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_LKOP2,CircuitTypCore.LK_LKOP2},
            new int[]{1,2},new int[]{0,0},new int[]{2,1},new double[][]{primary,secondary},2,2,2);
        netlist.registerMutualCoupling(0,1,k,4,9);
        return netlist;
    }

    @ParameterizedTest @EnumSource(SolverType.class)
    void matrixContainsSymmetricSelfAndMutualFluxConstraints(SolverType method) {
        CircuitNetlist netlist=windings(-0.5);double dt=0.1;
        MatrixSolver solver=new MatrixSolver(method);solver.initializeMatrices(2,2,2);
        solver.buildMatrixA(netlist,dt,0,false);
        double alpha=method==SolverType.SOLVER_BE?1:method==SolverType.SOLVER_TRZ?2:1.5;
        double[][] a=solver.getSystemMatrix();
        assertEquals(1,a[1][4]);assertEquals(-1,a[0][4]);assertEquals(1,a[4][1]);
        assertEquals(-alpha*4/dt,a[4][4],1e-12);
        assertEquals(-alpha*9/dt,a[3][3],1e-12);
        assertEquals(alpha*3/dt,a[4][3],1e-12);
        assertEquals(a[4][3],a[3][4]);
        solver.buildMatrixA(netlist,dt,0,false);
        assertEquals(alpha*3/dt,a[4][3],1e-12,"Rebuilding must not accumulate coupling");
    }

    @ParameterizedTest @EnumSource(SolverType.class)
    void rhsUsesBothWindingHistoriesWithoutMutatingThem(SolverType method) {
        CircuitNetlist netlist=windings(0.5);double dt=0.1;
        MatrixSolver solver=new MatrixSolver(method);solver.initializeMatrices(2,2,2);
        solver.getIALT()[0]=2;solver.getIALT()[1]=3;
        solver.getIALTALT()[0]=0.5;solver.getIALTALT()[1]=-1;
        solver.getPALT()[1]=7;solver.getPALT()[2]=11;
        // Current solution is deliberately different: nonlinear re-solves must still use committed history.
        solver.getP()[3]=1000;solver.getP()[4]=-1000;
        solver.buildMatrixA(netlist,dt,0,false);solver.buildVectorB(netlist,dt,0,false);
        double expected=method==SolverType.SOLVER_BE?-(4*2+3*3)/dt:
            method==SolverType.SOLVER_TRZ?-7-2*(4*2+3*3)/dt:
            (4*(-2*2+0.5*0.5)+3*(-2*3+0.5*-1))/dt;
        assertEquals(expected,solver.getb()[4],1e-10);
        assertArrayEquals(new double[]{2,3},solver.getIALT());
        assertArrayEquals(new double[]{0.5,-1},solver.getIALTALT());
    }

    @org.junit.jupiter.api.Test
    void registrationRejectsNonWindingsAndStaleInductanceValues() {
        CircuitNetlist netlist=windings(0.5);
        assertThrows(IllegalArgumentException.class,()->netlist.registerMutualCoupling(0,1,0.5,3,9));
        assertThrows(IllegalArgumentException.class,()->netlist.registerMutualCoupling(0,1,0.5,4,8));
        assertThrows(IndexOutOfBoundsException.class,()->netlist.registerMutualCoupling(-1,1,0.5,4,9));
        assertThrows(IndexOutOfBoundsException.class,()->netlist.registerMutualCoupling(0,2,0.5,4,9));
        CircuitNetlist plain=new CircuitNetlist();
        plain.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_R,CircuitTypCore.LK_L},
            new int[]{1,2},new int[]{0,0},new int[]{-1,-1},new double[][]{{4},{9}},2,0,2);
        assertThrows(IllegalArgumentException.class,()->plain.registerMutualCoupling(0,1,0.5,4,9));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"3,2","1,3","1,1"})
    void invalidAuxiliaryNumbersCannotAliasOrEscapeTheWindingEquations(int first,int second) {
        CircuitNetlist netlist=new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_LKOP2,CircuitTypCore.LK_LKOP2},
            new int[]{1,2},new int[]{0,0},new int[]{first,second},new double[][]{{4},{9}},2,2,2);
        assertThrows(IllegalArgumentException.class,()->netlist.registerMutualCoupling(0,1,0.5,4,9));
    }
}
