package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class TimeStepInitializationContractTest {
    @ParameterizedTest
    @EnumSource(SolverType.class)
    void currentSourceCutsetUsesTheLegacyPreStepBeforeRestoringConfiguredCurrents(SolverType method) {
        double[][] parameters = new double[3][13];
        parameters[0][0] = 0.001;
        parameters[1][0] = 0.002;
        parameters[0][1] = parameters[1][1] = 100;
        parameters[2][0] = gecko.core.circuit.SourceType.QUELLE_DC_NEW;
        parameters[2][1] = -300;
        var netlist = new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_L, CircuitTypCore.LK_L,
                CircuitTypCore.LK_I}, new int[]{1, 1, 1}, new int[]{0, 0, 0},
                new int[]{-1, -1, -1}, parameters, 1, 0, 3);

        var step = new ElectricalTimeStep(netlist, method);
        step.advance(1e-4, 0, null);

        // LKMatrices uses zero histories for its 1 ns pre-step, then restores both 100 A currents.
        // Its first TRZ step includes the pre-step voltage. BE and GS use only current histories.
        double expectedVoltage = switch (method) {
            case SOLVER_BE -> 2000.0 / 3;
            case SOLVER_TRZ -> 4000.0 / 3 - 4e8;
            case SOLVER_GS -> 1000;
        };
        double[] currents = netlist.getLastComponentCurrents();
        assertEquals(expectedVoltage, netlist.getLastNodeVoltages()[1]
                - netlist.getLastNodeVoltages()[0], 1e-6);
        assertEquals(500.0 / 3, currents[0], 1e-6);
        assertEquals(400.0 / 3, currents[1], 1e-6);
        assertEquals(-300, currents[2], 1e-10);
        assertEquals(0, Arrays.stream(currents).sum(), 1e-6);
        assertEquals(100, netlist.getParameter(0)[1]);
        assertEquals(100, netlist.getParameter(1)[1]);
    }

    static CircuitNetlist discharge(CircuitTypCore type,double initial) {
        double[][] parameters=new double[2][13];
        parameters[0][0]=type==CircuitTypCore.LK_C?0.001:0.01;
        parameters[0][1]=initial;
        // Stale runtime values must not override a fresh dialog start.
        parameters[0][2]=77;parameters[0][4]=88;parameters[0][5]=99;
        if(type==CircuitTypCore.LK_C){parameters[0][6]=0.001;parameters[0][7]=0.001;}
        parameters[1][0]=10;
        CircuitNetlist netlist=new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{type,CircuitTypCore.LK_R},
            new int[]{1,1},new int[]{0,0},new int[]{type==CircuitTypCore.LK_LKOP2?1:-1,-1},
            parameters,1,type==CircuitTypCore.LK_LKOP2?1:0,2);
        return netlist;
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void cutsetUsesDesktopStartupForAnIndependentInductor(SolverType method) {
        var netlist = cutsetWithBranch(new CircuitTypCore[]{CircuitTypCore.LK_L, CircuitTypCore.LK_R},
                new double[][]{new double[]{0.01, 2}, new double[]{10}}, new int[]{2, 2});
        var step = new ElectricalTimeStep(netlist, method);
        step.advance(1e-4, 0, null);
        double expected = switch (method) {
            case SOLVER_BE -> 2 / 1.1;
            case SOLVER_TRZ -> 2 / 1.05;
            case SOLVER_GS -> 3 / 1.6;
        };
        assertEquals(expected, netlist.getLastComponentCurrents()[3], 1e-10);
        assertEquals(-10 * expected, netlist.getLastNodeVoltages()[2], 1e-10);
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void cutsetUsesDesktopStartupForAShuntedInductorWithinAFloatingIsland(SolverType method) {
        double[][] parameters = new double[5][13];
        parameters[0][0] = 0.001;
        parameters[1][0] = 0.002;
        parameters[0][1] = parameters[1][1] = 100;
        parameters[2][0] = gecko.core.circuit.SourceType.QUELLE_DC_NEW;
        parameters[2][1] = -300;
        parameters[3][0] = 0.01;
        parameters[3][1] = 2;
        parameters[4][0] = 10;
        var netlist = new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_L, CircuitTypCore.LK_L,
                CircuitTypCore.LK_I, CircuitTypCore.LK_L, CircuitTypCore.LK_R},
                new int[]{1, 1, 1, 2, 2}, new int[]{0, 0, 0, 1, 1},
                new int[]{-1, -1, -1, -1, -1}, parameters, 2, 0, 5);
        var step = new ElectricalTimeStep(netlist, method);
        step.advance(1e-4, 0, null);
        double expected = switch (method) {
            case SOLVER_BE -> 2 / 1.1;
            case SOLVER_TRZ -> 2 / 1.05;
            case SOLVER_GS -> 3 / 1.6;
        };
        // TRZ retains a large common-mode pre-step voltage. Subtraction limits absolute precision here.
        assertEquals(expected, netlist.getLastComponentCurrents()[3], 1e-6);
        assertEquals(-10 * expected, netlist.getLastNodeVoltages()[2]
                - netlist.getLastNodeVoltages()[1], 1e-6);
    }

    @org.junit.jupiter.api.Test
    void cutsetUsesDesktopStartupForAnIndependentFastRlSineSource() {
        double[][] parameters = new double[5][21];
        parameters[0][0] = 0.001;
        parameters[0][1] = 1;
        parameters[1][0] = gecko.core.circuit.SourceType.QUELLE_DC_NEW;
        parameters[1][1] = -1;
        parameters[2][0] = 1e-8;
        parameters[2][1] = 2;
        parameters[3][0] = 10;
        parameters[4][0] = gecko.core.circuit.SourceType.QUELLE_SIN_NEW;
        parameters[4][1] = parameters[4][20] = 1;
        parameters[4][2] = 250e6;
        var netlist = new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_L, CircuitTypCore.LK_I,
                CircuitTypCore.LK_L, CircuitTypCore.LK_R, CircuitTypCore.LK_I},
                new int[]{1, 1, 2, 2, 2}, new int[5], new int[]{-1, -1, -1, -1, -1},
                parameters, 2, 0, 5);
        new ElectricalTimeStep(netlist, SolverType.SOLVER_TRZ).advance(1e-9, 0, null);
        // Actual desktop dialog start yields V(0)=20/3, followed by V(1)=-140/9 and iL(1)=14/9.
        assertEquals(-140.0 / 9, netlist.getLastNodeVoltages()[2], 1e-10);
        double[] currents = netlist.getLastComponentCurrents();
        assertEquals(14.0 / 9, currents[2], 1e-10);
        assertEquals(0, currents[2] + currents[3] + currents[4], 1e-10);
        assertEquals(0, currents[0] + currents[1], 1e-10);
    }

    @org.junit.jupiter.api.Test
    void cutsetDoesNotResampleAnIndependentSineSourceBeforeTimeZero() {
        double[] capacitor = new double[13], sine = new double[21];
        capacitor[0] = capacitor[6] = capacitor[7] = 1e-9;
        capacitor[1] = 5;
        sine[0] = gecko.core.circuit.SourceType.QUELLE_SIN_NEW;
        sine[1] = sine[20] = 1;
        sine[2] = 250e6;
        var baseline = new CircuitNetlist();
        baseline.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_C, CircuitTypCore.LK_I},
                new int[]{1, 1}, new int[]{0, 0}, new int[]{-1, -1},
                new double[][]{capacitor.clone(), sine.clone()}, 1, 0, 2);
        new ElectricalTimeStep(baseline, SolverType.SOLVER_TRZ).advance(1e-9, 0, null);
        assertEquals(5, baseline.getLastNodeVoltages()[1], 1e-10);

        var mixed = cutsetWithBranch(new CircuitTypCore[]{CircuitTypCore.LK_C, CircuitTypCore.LK_I},
                new double[][]{capacitor, sine}, new int[]{2, 2});
        new ElectricalTimeStep(mixed, SolverType.SOLVER_TRZ).advance(1e-9, 0, null);
        assertEquals(5, mixed.getLastNodeVoltages()[2], 1e-10);
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void cutsetKeepsParallelZeroStartCapacitorsDynamicAndUsesLegacyCapacitorHistory(SolverType method) {
        double[] charged = new double[13], zero1 = new double[13], zero2 = new double[13];
        charged[0] = charged[6] = charged[7] = 0.001;
        charged[1] = 5;
        zero1[0] = zero1[6] = zero1[7] = 0.001;
        zero2[0] = zero2[6] = zero2[7] = 0.002;
        var netlist = cutsetWithBranch(new CircuitTypCore[]{CircuitTypCore.LK_C, CircuitTypCore.LK_R,
                CircuitTypCore.LK_C, CircuitTypCore.LK_C, CircuitTypCore.LK_R, CircuitTypCore.LK_I},
                new double[][]{charged, new double[]{10}, zero1, zero2, new double[]{10},
                    new double[]{gecko.core.circuit.SourceType.QUELLE_DC_NEW, -1}},
                new int[]{2, 2, 3, 3, 3, 3});
        var step = new ElectricalTimeStep(netlist, method);
        step.advance(1e-4, 0, null);
        double chargedExpected = switch (method) {
            case SOLVER_BE -> 5 / 1.01;
            case SOLVER_TRZ -> 5 / 1.005;
            case SOLVER_GS -> 7.5 / 1.51;
        };
        assertEquals(chargedExpected, netlist.getLastNodeVoltages()[2], 1e-10);
        double alpha = method == SolverType.SOLVER_TRZ ? 2 : method == SolverType.SOLVER_GS ? 1.5 : 1;
        double preVoltage = 1 / (alpha * 0.003 / 1e-9 + 0.1);
        // The zero-start capacitors keep zero current history after the legacy pre-step.
        double zeroExpected = (1 + alpha * 0.003 / 1e-4 * preVoltage)
                / (alpha * 0.003 / 1e-4 + 0.1);
        assertEquals(zeroExpected, netlist.getLastNodeVoltages()[3], 1e-10);
        double[] currents = netlist.getLastComponentCurrents();
        assertEquals(0, currents[5] + currents[6] + currents[7] + currents[8], 1e-10);
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void cutsetDefaultsAMissingInductorDialogCurrentToZero(SolverType method) {
        var netlist = cutsetWithBranch(new CircuitTypCore[0], new double[0][], new int[0]);
        netlist.getParameter(1)[1] = 0;
        // initNetlist preserves the supplied parameter width.
        var shortNetlist = new CircuitNetlist();
        shortNetlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_L, CircuitTypCore.LK_L,
                CircuitTypCore.LK_I}, new int[]{1, 1, 1}, new int[]{0, 0, 0},
                new int[]{-1, -1, -1}, new double[][]{netlist.getParameter(0),
                    new double[]{0.002}, netlist.getParameter(2)}, 1, 0, 3);
        var matrix = new gecko.core.simulation.solver.MatrixSolver(method);
        matrix.initializeMatrices(1, 0, 3);
        new gecko.core.simulation.solver.InitialConditionSolver(method)
                .setDialogInitialConditions(matrix, shortNetlist);
        assertEquals(100, matrix.getIALT()[0]);
        assertEquals(0, matrix.getIALT()[1]);
    }

    private static CircuitNetlist cutsetWithBranch(CircuitTypCore[] branchTypes,
            double[][] branchParameters, int[] branchNodes) {
        int count = 3 + branchTypes.length;
        CircuitTypCore[] types = Arrays.copyOf(new CircuitTypCore[]{CircuitTypCore.LK_L,
            CircuitTypCore.LK_L, CircuitTypCore.LK_I}, count);
        double[][] parameters = Arrays.copyOf(new double[][]{new double[]{0.001, 100},
            new double[]{0.002, 100}, new double[]{gecko.core.circuit.SourceType.QUELLE_DC_NEW, -300}}, count);
        int[] x = Arrays.copyOf(new int[]{1, 1, 1}, count), sources = new int[count];
        Arrays.fill(sources, -1);
        System.arraycopy(branchTypes, 0, types, 3, branchTypes.length);
        System.arraycopy(branchParameters, 0, parameters, 3, branchParameters.length);
        System.arraycopy(branchNodes, 0, x, 3, branchNodes.length);
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = Arrays.copyOf(parameters[i], Math.max(13, parameters[i].length));
        }
        var netlist = new CircuitNetlist();
        netlist.initNetlist(types, x, new int[count], sources, parameters,
                Arrays.stream(x).max().orElseThrow(), 0, count);
        return netlist;
    }
    @ParameterizedTest @EnumSource(SolverType.class)
    void prechargedCapacitorStartsFromItsConfiguredVoltage(SolverType method) {
        var netlist=discharge(CircuitTypCore.LK_C,5);var step=new ElectricalTimeStep(netlist,method);
        double dt=1e-4;
        step.advance(dt,0,null);
        double h=dt/(10*0.001);
        double expected=method==SolverType.SOLVER_BE?5/(1+h):
            method==SolverType.SOLVER_TRZ?5*(1-h/2)/(1+h/2):5*1.5/(1.5+h);
        assertEquals(expected,netlist.getLastNodeVoltages()[1],1e-10);
        assertEquals(5,netlist.getParameter(0)[1],"Configured initial voltage remains reusable");
        assertEquals(-netlist.getLastComponentCurrents()[1],netlist.getLastComponentCurrents()[0],1e-10);
    }
    @ParameterizedTest @EnumSource(SolverType.class)
    void ordinaryInductorStartsFromItsConfiguredCurrent(SolverType method) {
        checkInductor(CircuitTypCore.LK_L,method);
    }
    @ParameterizedTest @EnumSource(SolverType.class)
    void auxiliaryCurrentWindingStartsFromItsConfiguredCurrent(SolverType method) {
        checkInductor(CircuitTypCore.LK_LKOP2,method);
    }
    static void checkInductor(CircuitTypCore type,SolverType method) {
        var netlist=discharge(type,2);var step=new ElectricalTimeStep(netlist,method);double dt=1e-4;
        step.advance(dt,0,null);
        double h=dt*10/0.01;
        double expected=method==SolverType.SOLVER_BE?2/(1+h):
            method==SolverType.SOLVER_TRZ?2*(1-h/2)/(1+h/2):2*1.5/(1.5+h);
        assertEquals(expected,netlist.getLastComponentCurrents()[0],1e-10);
        assertEquals(-10*expected,netlist.getLastNodeVoltages()[1],1e-10);
        assertEquals(2,netlist.getParameter(0)[1],"Runtime current must not overwrite the dialog initial current");
    }
    @ParameterizedTest @ValueSource(doubles={0,-1,Double.NaN,Double.POSITIVE_INFINITY})
    void invalidStepWidthCannotCommitOrNotifyAnElectricalStep(double dt) {
        var netlist=discharge(CircuitTypCore.LK_C,0);var step=new ElectricalTimeStep(netlist,SolverType.SOLVER_BE);
        List<Double> notifications=new ArrayList<>();
        assertThrows(IllegalArgumentException.class,()->step.advance(dt,0,(t,p,i,n)->notifications.add(t)));
        assertTrue(notifications.isEmpty());
    }
    @ParameterizedTest
    @EnumSource(SolverType.class)
    void decayHistoriesAdvanceExactlyOnceAndRestartFromConfiguredState(SolverType method) {
        for (CircuitTypCore type : new CircuitTypCore[]{
                CircuitTypCore.LK_C, CircuitTypCore.LK_L, CircuitTypCore.LK_LKOP2}) {
            boolean capacitor = type == CircuitTypCore.LK_C;
            double initial = capacitor ? 5 : 2;
            var netlist = discharge(type, initial);
            double dt = 1e-4;
            double h = capacitor ? dt / (10 * 0.001) : dt * 10 / 0.01;
            double previous = initial;
            double older = initial;
            double first = 0;
            for (int run = 0; run < 2; run++) {
                var step = new ElectricalTimeStep(netlist, method);
                previous = initial;
                older = initial;
                List<Double> times = new ArrayList<>();
                for (int index = 0; index < 8; index++) {
                    double expected = switch (method) {
                        case SOLVER_BE -> previous / (1 + h);
                        case SOLVER_TRZ -> previous * (1 - h / 2) / (1 + h / 2);
                        case SOLVER_GS -> (2 * previous - 0.5 * older) / (1.5 + h);
                    };
                    step.advance(dt, index * dt, (t, p, i, n) -> times.add(t));
                    double actual = capacitor ? netlist.getLastNodeVoltages()[1]
                            : netlist.getLastComponentCurrents()[0];
                    assertEquals(expected, actual, 1e-10, type + " step " + index);
                    assertEquals(0, Arrays.stream(netlist.getLastComponentCurrents()).sum(), 1e-10);
                    assertEquals(initial, netlist.getParameter(0)[1]);
                    if (index == 0) {
                        if (run == 0) first = actual;
                        else assertEquals(first, actual, 1e-12);
                    }
                    older = previous;
                    previous = expected;
                }
                assertEquals(8, times.size());
                assertEquals(7 * dt, times.getLast());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY})
    void invalidSampleTimeCannotAdvanceHistory(double time) {
        var netlist = discharge(CircuitTypCore.LK_C, 5);
        var step = new ElectricalTimeStep(netlist, SolverType.SOLVER_BE);
        assertThrows(IllegalArgumentException.class, () -> step.advance(1e-4, time, null));
        step.advance(1e-4, 0, null);
        assertEquals(5 / 1.01, netlist.getLastNodeVoltages()[1], 1e-10);
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void engineLoadsConfiguredCapacitorStateAndRepeatsTheSameTrace(SolverType method) throws Exception {
        var file = java.nio.file.Files.createTempFile("solver-initial-capacitor-", ".ipes");
        java.nio.file.Files.writeString(file, """
                tDURATION 0.0003
                dt 0.0001
                e
                <ElementLK>
                typ 3
                idStringDialog C1
                parameter[] 0.001 5 77 0 88 99 0.001 0.001 0 0 0 0 0
                labelAnfangsKnoten[] /n
                labelEndKnoten[] /0
                <\\ElementLK>
                e
                <ElementLK>
                typ 1
                idStringDialog R1
                parameter[] 10
                labelAnfangsKnoten[] /n
                labelEndKnoten[] /0
                <\\ElementLK>
                """);
        var engine = new HeadlessSimulationEngine();
        var config = SimulationConfig.builder().circuitFile(file.toString()).solverType(method).build();
        List<Double> first = new ArrayList<>();
        for (int run = 0; run < 2; run++) {
            List<Double> values = new ArrayList<>();
            engine.setStepTraceListener((t, p, i, n) -> {
                int capacitor = n.getType(0) == CircuitTypCore.LK_C ? 0 : 1;
                values.add(p[n.getNodeX(capacitor)] - p[n.getNodeY(capacitor)]);
            });
            var result = engine.runSimulation(config);
            assertTrue(result.isSuccess(), result.getErrorMessage());
            double expected = switch (method) {
                case SOLVER_BE -> 5 / 1.01;
                case SOLVER_TRZ -> 5 * 0.995 / 1.005;
                case SOLVER_GS -> 7.5 / 1.51;
            };
            assertEquals(expected, values.getFirst(), 1e-10);
            if (run == 0) first = values;
            else assertEquals(first, values);
        }
    }

}
