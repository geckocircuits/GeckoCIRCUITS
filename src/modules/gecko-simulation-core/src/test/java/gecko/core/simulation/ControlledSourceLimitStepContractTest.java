package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class ControlledSourceLimitStepContractTest {
    @ParameterizedTest
    @EnumSource(SolverType.class)
    void voltageLimitsReleaseAndCommitCapacitorHistoryAndTraceOnce(SolverType integration) {
        for (int mode : new int[]{5, 399}) {
            boolean legacy = mode == 399;
            double[] expectedCapacitorCurrents = switch (integration) {
                case SOLVER_BE -> legacy ? new double[]{12, -20, 14} : new double[]{10, -18, 14};
                case SOLVER_TRZ -> legacy ? new double[]{24, -64, 92} : new double[]{20, -56, 84};
                case SOLVER_GS -> legacy ? new double[]{18, -36, 31} : new double[]{15, -32, 30};
            };
            double[][] parameters = new double[4][21];
            parameters[0][1] = 2;
            parameters[1][0] = mode;
            parameters[1][11] = 3;
            parameters[1][12] = -4;
            parameters[1][13] = 5;
            parameters[2][0] = 10;
            parameters[3][0] = 0.25;
            parameters[3][6] = 0.25;
            parameters[3][7] = 0.25;
            var circuit = new CircuitNetlist();
            circuit.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_U,
                    CircuitTypCore.LK_R, CircuitTypCore.LK_C},
                    new int[]{1, 2, 2, 2}, new int[]{0, 0, 0, 0}, new int[]{1, 2, -1, -1},
                    parameters, 2, 2, 4);
            circuit.setControlElementIndex(1, 0);
            var electricalStep = new ElectricalTimeStep(circuit, integration);
            List<Double> tracedVoltages = new ArrayList<>();
            HeadlessSimulationEngine.StepTraceListener trace = (time, volts, currents, observed) -> {
                assertArrayEquals(observed.getLastNodeVoltages(), volts);
                assertArrayEquals(observed.getLastComponentCurrents(), currents);
                tracedVoltages.add(volts[2]);
            };
            double[] controls = {2, -2, 1};
            double[] expectedVoltages = legacy ? new double[]{6, -4, 3} : new double[]{5, -4, 3};
            double[] expectedLoadCurrents = legacy ? new double[]{0.6, -0.4, 0.3} : new double[]{0.5, -0.4, 0.3};
            double[] expectedLimitStates = legacy ? new double[]{0, -1, 0} : new double[]{1, -1, 0};

            for (int index = 0; index < controls.length; index++) {
                circuit.getParameter(0)[1] = controls[index];
                electricalStep.advance(0.125, index * 0.125, trace);

                assertEquals(expectedVoltages[index], circuit.getLastNodeVoltages()[2], 1e-12);
                assertEquals(expectedLoadCurrents[index], circuit.getLastComponentCurrents()[2], 1e-12);
                assertEquals(expectedCapacitorCurrents[index], circuit.getLastComponentCurrents()[3], 1e-12);
                assertEquals(expectedLimitStates[index], circuit.getParameter(1)[14]);
                assertEquals(index + 1, tracedVoltages.size());
                assertEquals(expectedVoltages[index], tracedVoltages.get(index), 1e-12);
                assertEquals(3, circuit.getParameter(1)[11]);
                assertEquals(-4, circuit.getParameter(1)[12]);
                assertEquals(5, circuit.getParameter(1)[13]);
            }
        }
    }
}
