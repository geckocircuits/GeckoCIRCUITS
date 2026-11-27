package gecko.core.simulation;

import static org.junit.jupiter.api.Assertions.*;

import gecko.core.allg.SolverType;
import gecko.core.io.CircuitFileParser;
import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

/** A coupler that was never configured couples nothing. The thermal exchange must be safe before and after configuration. */
class DomainCouplerThermalLifecycleTest {
    private static final double HEAT = 5;

    /** A heat flow source into a node with a thermal resistance to the reference. */
    private static ThermalNetwork network() throws Exception {
        String blocks = ThermalFixture.thermal(44, "P", 1, "0", "a", "401.0 " + HEAT + " 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0", "")
                + ThermalFixture.thermal(46, "Rth", 2, "a", "0", "2.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0 0.0", "");
        var model = new CircuitFileParser().parse(new BufferedReader(new StringReader("dt 0.001\ntDURATION 1\n" + blocks)), "coupler.ipes");
        return ThermalNetwork.build(model, null, SolverType.SOLVER_BE);
    }

    private static double heat(ThermalNetwork network) {
        var netlist = network.netlist();
        return netlist.getParameter(netlist.getElementIndexByName("P"))[1];
    }

    @Test
    void anUnconfiguredCouplerLeavesTheThermalNetworkUntouchedInBothDirections() throws Exception {
        var network = network();
        assertEquals(HEAT, heat(network), 0, "the saved heat flow is the starting value");
        var coupler = new DomainCoupler();
        coupler.transferControlToThermalSources(network);
        coupler.transferThermalToControlProbes(network);
        assertEquals(HEAT, heat(network), 0, "no CONTROL output was mapped, so the source keeps its value");
        // The network still solves after the no-op exchanges.
        network.advance(1e-3, 0, null);
        assertEquals(1, network.committedSteps());
    }

    @Test
    void theSameExchangeChangesTheSourceOnceTheCouplerIsConfigured() throws Exception {
        var network = network();
        var mapping = new ControlBlockMapping();
        // A mapped source without a producer holds zero, like a signal-controlled source with no signal.
        mapping.addThermalSourceMapping(null, 0, "P");
        var coupler = new DomainCoupler();
        coupler.configureFromMapping(mapping);
        coupler.transferControlToThermalSources(network);
        assertEquals(0, heat(network), 0);
        // A missing network is a no-op even when configured.
        coupler.transferControlToThermalSources(null);
        coupler.transferThermalToControlProbes(null);
    }
}
