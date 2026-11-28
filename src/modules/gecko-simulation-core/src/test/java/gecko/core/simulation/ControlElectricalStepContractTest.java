package gecko.core.simulation;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.control.calculators.GateCalculator;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class ControlElectricalStepContractTest {
    @ParameterizedTest
    @EnumSource(SolverType.class)
    void gateRemovalTurnsOffAnIgbtWhileAThyristorKeepsConducting(SolverType method) {
        for (CircuitTypCore type : new CircuitTypCore[]{CircuitTypCore.LK_IGBT, CircuitTypCore.LK_THYR}) {
            var netlist = SemiconductorStepContractTest.circuit(type, false, 6);
            netlist.setComponentNameToIndex(Map.of("device", 2));
            var gate = new GateCalculator();
            gate._inputSignal[0] = new double[]{1};
            var coupler = GateCouplingContractTest.coupler(gate);
            var electrical = new ElectricalTimeStep(netlist, method);

            coupler.transferGateSignalsToLk(netlist);
            assertEquals(1e6, netlist.getParameter(2)[0], "Publishing CONTROL must not change the solved resistance");
            coupler.applyGateSignals(netlist);
            electrical.advance(0.001, 0, null);
            SemiconductorStepContractTest.assertSeriesCurrent(netlist, 2.25);

            gate._inputSignal[0][0] = 0;
            coupler.transferGateSignalsToLk(netlist);
            assertEquals(0.25, netlist.getParameter(2)[0]);
            coupler.applyGateSignals(netlist);
            assertEquals(0, netlist.getParameter(2)[8], "The OFF flag reaches the electrical device");
            electrical.advance(0.001, 0.001, null);
            double expected = type == CircuitTypCore.LK_IGBT ? 6 / 1000001.75 : 2.25;
            SemiconductorStepContractTest.assertSeriesCurrent(netlist, expected);
            assertEquals(type == CircuitTypCore.LK_IGBT ? 1e6 : 0.25, netlist.getParameter(2)[0]);
        }
    }
}
