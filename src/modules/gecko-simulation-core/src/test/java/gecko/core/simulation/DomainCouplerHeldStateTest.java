package gecko.core.simulation;

import static org.junit.jupiter.api.Assertions.*;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.control.calculators.GateCalculator;
import gecko.core.control.calculators.TimeCalculator;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The held gate and source values that the periodic steady-state search reads. The export only reads. A sentinel
 * value that never changes makes an export with no held value valid, and the monitor shows what changed.
 */
class DomainCouplerHeldStateTest {
    private static final int PARAM_GATE = 8;

    private static PeriodicStateMonitor monitor() { return new PeriodicStateMonitor(1e-4, 50, 1); }

    /** Exports the held state and a sentinel as one period, and returns what the monitor decides. */
    private static PeriodicStateMonitor.Outcome exportPeriod(PeriodicStateMonitor monitor, DomainCoupler coupler,
            CircuitNetlist netlist, int period) {
        monitor.begin();
        coupler.exportHeldGates(monitor.sink());
        coupler.exportHeldSources(netlist, monitor.sink());
        monitor.sink().group(PeriodicStateMonitor.Unit.CONTROL, "sentinel");
        monitor.sink().value(1);
        return monitor.commit(period, period);
    }

    private static void exportThreePeriods(DomainCoupler coupler, CircuitNetlist netlist,
            PeriodicStateMonitor monitor, int first) {
        for (int period = first; period < first + 3; period++) exportPeriod(monitor, coupler, netlist, period);
    }

    private static DomainCoupler gateCoupler(GateCalculator gate) {
        var mapping = new ControlBlockMapping();
        mapping.addGateMapping(gate, "S.1");
        var coupler = new DomainCoupler();
        coupler.configureFromMapping(mapping);
        return coupler;
    }

    private static GateCalculator gateWithLiveInput(double value) {
        var gate = new GateCalculator();
        gate._inputSignal[0] = new double[]{value};
        return gate;
    }

    @Test
    void anUnconfiguredCouplerExportsNothingAndIsASafeNoOp() {
        var coupler = new DomainCoupler();
        var monitor = monitor();
        for (int period = 1; period <= 2; period++) exportPeriod(monitor, coupler, null, period);
        assertEquals(PeriodicStateMonitor.Outcome.CONVERGED, exportPeriod(monitor, coupler, null, 3));
        assertEquals(PeriodicStateMonitor.Outcome.CONVERGED, exportPeriod(monitor, coupler, ControlSubsystemRateTest.gateSwitch(), 4));
    }

    @Test
    void aGateHasNoHeldStateUntilItPublishesAndThenExportsItsPublishedValue() {
        var gate = gateWithLiveInput(1);
        var coupler = gateCoupler(gate);
        var netlist = ControlSubsystemRateTest.gateSwitch();
        var unpublished = monitor();
        exportThreePeriods(coupler, netlist, unpublished, 1);
        assertTrue(unpublished.report().converged(), "a gate with no publication exports only the sentinel");

        coupler.transferGateSignalsToLk(netlist, calculator -> true);
        // The held value is new state, so the exported layout changes against the unpublished monitor.
        assertThrows(IllegalStateException.class, () -> exportPeriod(unpublished, coupler, netlist, 4));
        var published = monitor();
        exportThreePeriods(coupler, netlist, published, 1);
        assertTrue(published.report().converged(), published.report().toString());
    }

    @Test
    void theLiveInputBetweenSamplesDoesNotChangeTheExportButAPublicationDoes() {
        var gate = gateWithLiveInput(1);
        var coupler = gateCoupler(gate);
        var netlist = ControlSubsystemRateTest.gateSwitch();
        coupler.transferGateSignalsToLk(netlist, calculator -> true);
        var monitor = monitor();
        exportThreePeriods(coupler, netlist, monitor, 1);
        assertTrue(monitor.report().converged(), monitor.report().toString());

        // The producer changes between two samples. The gate does not publish, so the held value stays.
        gate._inputSignal[0] = new double[]{0};
        exportThreePeriods(coupler, netlist, monitor, 4);
        assertTrue(monitor.report().converged(), "the live input is not exported: " + monitor.report());
        assertEquals(0, monitor.report().residual());
        assertEquals(1.0, netlist.getParameter(0)[PARAM_GATE], "the switch keeps the published gate");

        // The next sample publishes the new value. The export shows the change in the held gate value.
        coupler.transferGateSignalsToLk(netlist, calculator -> true);
        exportPeriod(monitor, coupler, netlist, 7);
        assertFalse(monitor.report().converged());
        assertTrue(monitor.report().worstState().startsWith("held gate value"), monitor.report().toString());
        assertTrue(monitor.report().residual() > 1, monitor.report().toString());
    }

    @Test
    void anExportNeverChangesAGateOrASourceOfTheCircuit() {
        var gate = gateWithLiveInput(1);
        var producer = new TimeCalculator();
        producer._outputSignal[0][0] = 7;
        var mapping = new ControlBlockMapping();
        mapping.addGateMapping(gate, "S.1");
        mapping.addSourceMapping(producer, 0, "U.2");
        var coupler = new DomainCoupler();
        coupler.configureFromMapping(mapping);
        var switches = ControlSubsystemRateTest.gateSwitch();
        var sources = new CircuitNetlist();
        sources.initNetlist(new CircuitTypCore[]{CircuitTypCore.LK_U}, new int[]{1}, new int[]{0}, new int[]{1},
                new double[][]{new double[21]}, 1, 1, 1);
        sources.setComponentNameToIndex(Map.of("U.2", 0));
        coupler.transferGateSignalsToLk(switches, calculator -> true);
        coupler.transferControlToSources(sources, calculator -> true);
        double[] switchBefore = switches.getParameter(0).clone();
        double[] sourceBefore = sources.getParameter(0).clone();

        var monitor = monitor();
        exportThreePeriods(coupler, sources, monitor, 1);
        assertTrue(monitor.report().converged(), monitor.report().toString());
        assertArrayEquals(switchBefore, switches.getParameter(0), "the export left the switch alone");
        assertArrayEquals(sourceBefore, sources.getParameter(0), "the export left the source alone");
        assertEquals(7, sources.getParameter(0)[1]);
        assertEquals(1.0, switches.getParameter(0)[PARAM_GATE]);

        // The producer moves between samples. The held source value does not follow until a transfer.
        producer._outputSignal[0][0] = 9;
        exportPeriod(monitor, coupler, sources, 4);
        assertTrue(monitor.report().converged(), "the held source value stays: " + monitor.report());
        coupler.transferControlToSources(sources, calculator -> true);
        exportPeriod(monitor, coupler, sources, 5);
        assertFalse(monitor.report().converged());
        assertTrue(monitor.report().worstState().startsWith("held source value"), monitor.report().toString());
        assertEquals(7, sourceBefore[1], "the earlier copy is unchanged");
    }
}
