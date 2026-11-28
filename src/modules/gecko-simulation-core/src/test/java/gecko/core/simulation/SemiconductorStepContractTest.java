package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.circuit.SourceType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class SemiconductorStepContractTest {
    static CircuitNetlist circuit(CircuitTypCore type, boolean on, double sourceVoltage) {
        double[][] parameters = new double[3][21];
        parameters[0][0] = SourceType.QUELLE_DC_NEW;
        parameters[0][1] = sourceVoltage;
        parameters[1][0] = 1.75;
        parameters[2][0] = on ? 0.25 : 1e6;
        parameters[2][1] = type == CircuitTypCore.LK_MOSFET ? 0 : 1.5;
        parameters[2][2] = 0.25;
        parameters[2][3] = 1e6;
        parameters[2][8] = on ? 1 : 0;
        parameters[2][11] = Double.NEGATIVE_INFINITY;
        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U, CircuitTypCore.LK_R, type},
                new int[]{1, 1, 2}, new int[]{0, 2, 0}, new int[]{1, -1, -1}, parameters, 2, 1, 3);
        return netlist;
    }

    static Stream<Arguments> operatingPoints() {
        return Stream.of(CircuitTypCore.LK_THYR, CircuitTypCore.LK_IGBT, CircuitTypCore.LK_MOSFET)
                .flatMap(type -> Stream.of(SolverType.values()).flatMap(method ->
                    Stream.of(false, true).flatMap(on ->
                        Stream.of(-6.0, 6.0).map(voltage -> Arguments.of(type, method, on, voltage)))));
    }

    static void assertSeriesCurrent(CircuitNetlist netlist, double expected) {
        double[] currents = netlist.getLastComponentCurrents();
        assertEquals(expected, currents[2], 1e-10, "Switch current agrees with the worked circuit");
        assertEquals(currents[1], currents[2], 1e-10, "Series resistor and switch must carry the same current");
        assertEquals(-currents[1], currents[0], 1e-10, "The source supplies the same branch current");
    }

    @ParameterizedTest
    @MethodSource("operatingPoints")
    void actualTimestepPreservesKclInBlockingAndConductingStates(
            CircuitTypCore type, SolverType method, boolean initiallyOn, double voltage) {
        CircuitNetlist netlist = circuit(type, initiallyOn, voltage);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, method);
        step.advance(0.001, 0, null);
        boolean conducting = initiallyOn && (type == CircuitTypCore.LK_MOSFET || voltage > 0);
        // Rload + Ron = 2 ohms. The 1.5 V drop gives 2.25 A; MOSFET gives +/-3 A.
        double expected = conducting ? (type == CircuitTypCore.LK_MOSFET ? voltage / 2 : 2.25)
                : voltage / 1000001.75;
        assertSeriesCurrent(netlist, expected);
        assertEquals(conducting ? 0.25 : 1e6, netlist.getParameter(2)[0]);
        step.advance(0.001, 0.001, null);
        assertSeriesCurrent(netlist, expected);
    }

    @Test
    void thyristorRetainsConductionAfterGateRemovalInTheRealTimestep() {
        CircuitNetlist netlist = circuit(CircuitTypCore.LK_THYR, false, 6);
        ElectricalTimeStep step = new ElectricalTimeStep(netlist, SolverType.SOLVER_BE);
        netlist.getParameter(2)[8] = 1;
        step.advance(0.001, 0, null);
        assertSeriesCurrent(netlist, 2.25);
        netlist.getParameter(2)[8] = 0;
        step.advance(0.001, 0.001, null);
        assertSeriesCurrent(netlist, 2.25);
    }
}
