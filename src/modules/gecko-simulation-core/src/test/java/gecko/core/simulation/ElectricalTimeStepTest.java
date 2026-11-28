package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.circuit.SourceType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.testutil.SyntheticCircuitBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ElectricalTimeStepTest {

    @Test
    void voltageSourceCurrentBalancesItsLoadWithoutChangingSourceVoltage() {
        CircuitNetlist netlist = SyntheticCircuitBuilder.rcCircuit(10.0, 1000.0, 1e-6);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, SolverType.SOLVER_BE);

        for (int index = 0; index < 3; index++) {
            step.advance(1e-5, index * 1e-5, null);
            double[] currents = netlist.getLastComponentCurrents();
            assertEquals(-currents[1], currents[0], 1e-12,
                    "The voltage source must supply the resistor current");
            assertEquals(10.0, netlist.getParameter(0)[1], 0.0,
                    "Current reconstruction must preserve the source voltage");
        }
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void capacitorHistoryProducesTheExpectedTimeConstant(SolverType solverType) {
        double voltage = 10.0;
        double resistance = 1000.0;
        double capacitance = 1e-6;
        double timeConstant = resistance * capacitance;
        double dt = timeConstant / 100.0;
        CircuitNetlist netlist = SyntheticCircuitBuilder.rcCircuit(voltage, resistance, capacitance);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, solverType);

        for (int index = 0; index < 100; index++) {
            step.advance(dt, index * dt, null);
            double[] currents = netlist.getLastComponentCurrents();
            assertEquals(currents[1], currents[2], 1e-10,
                    "Resistor and capacitor currents must match at each committed step");
        }

        assertEquals(voltage * (1.0 - Math.exp(-1.0)), netlist.getLastNodeVoltages()[2], 0.05);
        float[] logged = new float[3];
        step.copyNodeVoltagesTo(logged);
        assertEquals(netlist.getLastNodeVoltages()[2], logged[2], 1e-6);
    }

    @ParameterizedTest
    @EnumSource(SolverType.class)
    void inductorHistoryProducesTheExpectedTimeConstant(SolverType solverType) {
        double voltage = 10.0;
        double resistance = 10.0;
        double inductance = 0.01;
        double timeConstant = inductance / resistance;
        double dt = timeConstant / 100.0;
        CircuitNetlist netlist = SyntheticCircuitBuilder.rlCircuit(voltage, resistance, inductance);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, solverType);

        for (int index = 0; index < 100; index++) {
            step.advance(dt, index * dt, null);
            double[] currents = netlist.getLastComponentCurrents();
            assertEquals(currents[1], currents[2], 1e-10,
                    "Resistor and inductor currents must match at each committed step");
        }

        assertEquals(voltage / resistance * (1.0 - Math.exp(-1.0)),
                netlist.getLastComponentCurrents()[2], 0.005);
    }

    @Test
    void nonlinearRetryCommitsInductorHistoryAndTraceExactlyOnce() {
        double voltage = 10.0;
        double forwardVoltage = 0.7;
        double onResistance = 0.001;
        double offResistance = 1e9;
        double inductance = 0.01;
        double dt = 1e-6;
        double[][] parameters = new double[3][21];
        parameters[0][0] = SourceType.QUELLE_DC_NEW;
        parameters[0][1] = voltage;
        parameters[1][0] = offResistance;
        parameters[1][1] = forwardVoltage;
        parameters[1][2] = onResistance;
        parameters[1][3] = offResistance;
        parameters[2][0] = inductance;
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_D, CircuitTypCore.LK_L},
                new int[]{1, 1, 2}, new int[]{0, 2, 0}, new int[]{1, -1, -1}, parameters, 2, 1, 3);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, SolverType.SOLVER_BE);
        List<Double> traceTimes = new ArrayList<>();
        List<Double> traceCurrents = new ArrayList<>();
        HeadlessSimulationEngine.StepTraceListener listener = (time, potentials, currents, circuit) -> {
            assertArrayEquals(circuit.getLastNodeVoltages(), potentials);
            assertArrayEquals(circuit.getLastComponentCurrents(), currents);
            traceTimes.add(time);
            traceCurrents.add(currents[2]);
        };

        double expectedCurrent = 0.0;
        for (int index = 0; index < 4; index++) {
            step.advance(dt, index * dt, listener);
            expectedCurrent = (expectedCurrent + dt / inductance * (voltage - forwardVoltage))
                    / (1.0 + onResistance * dt / inductance);
            assertEquals(expectedCurrent, netlist.getLastComponentCurrents()[2], 1e-9);
            assertEquals(onResistance, netlist.getParameter(1)[0]);
            assertEquals(forwardVoltage, netlist.getParameter(1)[1]);
            assertEquals(index + 1, traceTimes.size());
            assertEquals(index * dt, traceTimes.get(index));
            assertEquals(expectedCurrent, traceCurrents.get(index), 1e-9);
        }
    }
}
