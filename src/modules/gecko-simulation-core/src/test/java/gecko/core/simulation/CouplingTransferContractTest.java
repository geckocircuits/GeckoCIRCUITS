package gecko.core.simulation;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.control.calculators.NothingToDoCalculator;
import gecko.core.control.calculators.GainCalculator;
import gecko.core.control.calculators.GateCalculator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CouplingTransferContractTest {
    @Test
    void sourceAndGateTransfersRespectMappingsAndClearMissingOutputSlots() {
        var coupler = new DomainCoupler();
        int[] mapping = {2, 0, -1, 9};
        coupler.configureControlToLkSourceMapping(mapping);
        coupler.configureControlToLkSwitchMapping(mapping);
        mapping[0] = 0;
        coupler.transferControlToLk(new double[]{0.5, -2, 1});
        assertArrayEquals(new double[]{1, 0.5, 0, 0}, coupler.getControlSignalsForLkSources());
        assertArrayEquals(new boolean[]{true, false, false, false}, coupler.getSwitchGateSignals());
        coupler.transferControlToLk(new double[]{0.25});
        assertArrayEquals(new double[]{0, 0.25, 0, 0}, coupler.getControlSignalsForLkSources());
        assertArrayEquals(new boolean[4], coupler.getSwitchGateSignals());
        coupler.transferControlToLk(null);
        double[] copy = coupler.getControlSignalsForLkSources();
        copy[0] = 12;
        boolean[] gates = coupler.getSwitchGateSignals();
        gates[0] = true;
        assertEquals(0, coupler.getControlSignalsForLkSources()[0]);
        assertFalse(coupler.getSwitchGateSignals()[0]);
        coupler.configureControlToLkSourceMapping(new int[]{0});
        coupler.configureControlToLkSwitchMapping(new int[]{0});
        assertArrayEquals(new double[1], coupler.getControlSignalsForLkSources());
        assertArrayEquals(new boolean[1], coupler.getSwitchGateSignals());
    }

    @Test
    void voltageTransferClearsUnavailableNodesAfterNetlistReuse() {
        var netlist = GateCouplingContractTest.device(CircuitTypCore.LK_R, new double[]{10});
        var coupler = new DomainCoupler();
        int[] nodes = {1, 0, -1, 8};
        coupler.configureLkToControlMapping(nodes);
        nodes[0] = 0;
        netlist.storeResults(new double[]{0, 12}, new double[]{1.2});
        coupler.transferLkToControl(netlist);
        assertArrayEquals(new double[]{12, 0, 0, 0}, coupler.getLkNodeVoltagesForControl());
        double[] copy = coupler.getLkNodeVoltagesForControl();
        copy[0] = -1;
        assertEquals(12, coupler.getLkNodeVoltagesForControl()[0]);
        netlist.clear();
        coupler.transferLkToControl(netlist);
        assertArrayEquals(new double[4], coupler.getLkNodeVoltagesForControl());
    }

    @Test
    void signedProbeSamplesReachOutputsAndMissingCurrentDoesNotRetainAnOldSample() {
        var netlist = GateCouplingContractTest.device(CircuitTypCore.LK_R, new double[]{10});
        var forward = new NothingToDoCalculator(0, 1);
        var reverse = new NothingToDoCalculator(0, 1);
        var ground = new NothingToDoCalculator(0, 1);
        var absent = new NothingToDoCalculator(0, 1);
        var current = new NothingToDoCalculator(0, 1);
        var mapping = new ControlBlockMapping();
        mapping.addVoltProbe(forward, 1, 0, "forward");
        mapping.addVoltProbe(reverse, 0, 1, "reverse");
        mapping.addVoltProbe(ground, 0, 0, "ground");
        mapping.addVoltProbe(absent, -1, 10, "absent");
        mapping.addVoltProbe(absent, 10, -1, "absentReversed");
        mapping.addAmpProbe(current, "device", "current");
        var coupler = new DomainCoupler();
        coupler.configureFromMapping(mapping);
        netlist.storeResults(new double[]{0, 12}, new double[]{-1.2});
        coupler.transferLkToControlProbes(netlist);
        assertEquals(12, forward._outputSignal[0][0]);
        assertEquals(-12, reverse._outputSignal[0][0]);
        assertEquals(0, ground._outputSignal[0][0]);
        assertEquals(0, absent._outputSignal[0][0]);
        assertEquals(-1.2, current._outputSignal[0][0]);

        netlist.setComponentNameToIndex(Map.of());
        coupler.transferLkToControlProbes(netlist);
        assertEquals(0, current._outputSignal[0][0], "An unresolved current probe must not keep its previous sample");
        netlist.setComponentNameToIndex(Map.of("device", 0));
        netlist.storeResults(new double[]{0, 2}, new double[0]);
        coupler.transferLkToControlProbes(netlist);
        assertEquals(0, current._outputSignal[0][0]);
        coupler.transferLkToControlProbes(null);
        coupler.configureFromMapping(null);
        coupler.transferLkToControlProbes(netlist);
    }

    @Test
    void currentProbeOnVoltageSourceMatchesLegacyZeroCurrentReporting() {
        var netlist = new gecko.core.circuit.netlist.CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U}, new int[]{1}, new int[]{0},
                new int[]{1}, new double[][]{new double[13]}, 1, 1, 1);
        netlist.setComponentNameToIndex(Map.of("source", 0));
        netlist.storeResults(new double[]{0, 120}, new double[]{18});

        var amp = new NothingToDoCalculator(0, 1);
        var mapping = new ControlBlockMapping();
        mapping.addAmpProbe(amp, "source", "AMP.1");
        var coupler = new DomainCoupler();
        coupler.configureFromMapping(mapping);

        coupler.transferLkToControlProbes(netlist);

        assertEquals(0, amp._outputSignal[0][0]);
        assertEquals(18, netlist.getLastComponentCurrents()[0]);
    }

    @Test
    void thermalPlaceholderArraysAndEmptyMappingsDoNotLeakMutableBuffers() {
        var coupler = new DomainCoupler();
        coupler.configurePowerLossArray(2);
        coupler.configureThermArray(3);
        coupler.getLkPowerLosses()[0] = 7;
        coupler.getThermTemperatures()[0] = 8;
        assertArrayEquals(new double[2], coupler.getLkPowerLosses());
        assertArrayEquals(new double[3], coupler.getThermTemperatures());
        coupler.transferLkToControl(null);
        coupler.transferControlToLk(new double[0]);
        coupler.coupleDomainsForTimeStep(null, null, 0.125, 0);
    }
    @Test
    void publicCouplingCycleSamplesProbesBeforeExecutingControlAndPublishingGates() throws Exception {
        var netlist = GateCouplingContractTest.device(CircuitTypCore.LK_S,
                GateCouplingContractTest.parameters(CircuitTypCore.LK_S));
        var probe = new NothingToDoCalculator(0, 1);
        var gain = new GainCalculator(0.5);
        var gate = new GateCalculator();
        gain.setInputSignal(0, probe, 0);
        gate.setInputSignal(0, gain, 0);
        var mapping = new ControlBlockMapping();
        mapping.addVoltProbe(probe, 1, 0, "voltage");
        mapping.addGateMapping(gate, "device");
        var coupler = new DomainCoupler();
        coupler.configureFromMapping(mapping);
        var control = ControlNetlist.fromOrderedCalculators(List.of(probe, gain, gate));

        netlist.storeResults(new double[]{0, 12}, new double[]{0});
        coupler.coupleDomainsForTimeStep(netlist, control, 0.125, 0);
        assertEquals(6, gain._outputSignal[0][0]);
        assertEquals(6, netlist.getParameter(0)[8]);
        assertEquals(1000, netlist.getParameter(0)[0]);
        assertTrue(coupler.applyGateSignals(netlist));
        assertEquals(0.25, netlist.getParameter(0)[0]);

        netlist.storeResults(new double[]{0, 0.5}, new double[]{0});
        coupler.coupleDomainsForTimeStep(netlist, control, 0.125, 0.125);
        assertEquals(0.25, gain._outputSignal[0][0]);
        assertEquals(0.25, netlist.getParameter(0)[0]);
        assertTrue(coupler.applyGateSignals(netlist));
        assertEquals(1000, netlist.getParameter(0)[0]);
        assertEquals(0.125, control.getSimulationTime());
    }

}
