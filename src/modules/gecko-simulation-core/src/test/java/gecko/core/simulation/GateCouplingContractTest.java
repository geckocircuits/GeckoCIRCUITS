package gecko.core.simulation;

import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;
import gecko.core.control.calculators.GateCalculator;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class GateCouplingContractTest {
    static CircuitNetlist device(CircuitTypCore type, double[] parameters) {
        var netlist = new CircuitNetlist();
        netlist.initNetlist(new CircuitTypCore[]{type}, new int[]{1}, new int[]{0},
                new int[]{-1}, new double[][]{parameters}, 1, 0, 1);
        netlist.setComponentNameToIndex(Map.of("device", 0));
        return netlist;
    }

    static DomainCoupler coupler(GateCalculator gate) {
        var mapping = new ControlBlockMapping();
        mapping.addGateMapping(gate, "device");
        var coupler = new DomainCoupler();
        coupler.configureFromMapping(mapping);
        return coupler;
    }

    @ParameterizedTest
    @EnumSource(value = CircuitTypCore.class, names = {"LK_S", "LK_MOSFET"})
    void documentedSwitchParameterLayoutsCanBeDrivenWithoutAGateStorageField(CircuitTypCore type) {
        double[] parameters = type == CircuitTypCore.LK_S
                ? new double[]{1000, 0.25, 1000} : new double[]{1000, 0, 0.25, 1000};
        var netlist = device(type, parameters);
        var gate = new GateCalculator();
        gate._inputSignal[0] = new double[]{1};
        var coupler = coupler(gate);

        assertTrue(coupler.applyGateSignals(netlist));
        assertEquals(0.25, netlist.getParameter(0)[0]);
        assertFalse(coupler.applyGateSignals(netlist), "An unchanged resistance does not need a new factorization");
        gate._inputSignal[0][0] = 0.5;
        assertTrue(coupler.applyGateSignals(netlist));
        assertEquals(1000, netlist.getParameter(0)[0]);
    }
    static double[] parameters(CircuitTypCore type) {
        double[] values = new double[9];
        values[0] = 1000;
        values[1] = type == CircuitTypCore.LK_S ? 0.25 : 1.5;
        values[2] = type == CircuitTypCore.LK_S ? 1000 : 0.25;
        values[3] = 1000;
        return values;
    }

    static Stream<Arguments> thresholds() {
        return Stream.of(CircuitTypCore.LK_S, CircuitTypCore.LK_MOSFET,
                CircuitTypCore.LK_THYR, CircuitTypCore.LK_IGBT)
                .flatMap(type -> Stream.of(-2.0, 0.0, Math.nextDown(0.5), 0.5, Math.nextUp(0.5), 1.0)
                        .map(value -> Arguments.of(type, value)));
    }

    @ParameterizedTest
    @MethodSource("thresholds")
    void thresholdAndParameterOwnershipMatchTheDeviceType(CircuitTypCore type, double value) {
        var netlist = device(type, parameters(type));
        double[] configured = netlist.getParameter(0).clone();
        var gate = new GateCalculator();
        gate._inputSignal[0] = new double[]{value};
        var coupler = coupler(gate);
        coupler.transferGateSignalsToLk(netlist);
        assertEquals(value, netlist.getParameter(0)[8]);
        assertEquals(1000, netlist.getParameter(0)[0]);
        boolean changed = coupler.applyGateSignals(netlist);
        boolean on = value > 0.5;
        boolean resistiveSwitch = type == CircuitTypCore.LK_S || type == CircuitTypCore.LK_MOSFET;
        assertEquals(resistiveSwitch && on, changed);
        assertEquals(resistiveSwitch && on ? 0.25 : 1000, netlist.getParameter(0)[0]);
        if (!resistiveSwitch) assertEquals(on ? 1 : 0, netlist.getParameter(0)[8]);
        for (int index = 1; index < 8; index++) {
            assertEquals(configured[index], netlist.getParameter(0)[index], "Configured device field " + index);
        }
        assertFalse(coupler.applyGateSignals(netlist));
    }

    @ParameterizedTest
    @EnumSource(value = CircuitTypCore.class, names = {"LK_S", "LK_MOSFET", "LK_THYR", "LK_IGBT"})
    void disconnectedAndEmptyGateInputsAreOff(CircuitTypCore type) {
        for (double[] input : new double[][]{null, new double[0]}) {
            var netlist = device(type, parameters(type));
            var gate = new GateCalculator();
            gate._inputSignal[0] = input;
            var coupler = coupler(gate);
            coupler.transferGateSignalsToLk(netlist);
            assertEquals(0, netlist.getParameter(0)[8]);
            assertFalse(coupler.applyGateSignals(netlist));
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void nonfiniteGateValuesFailBeforeChangingDeviceParameters(double value) {
        var netlist = device(CircuitTypCore.LK_S, parameters(CircuitTypCore.LK_S));
        double[] original = netlist.getParameter(0).clone();
        var gate = new GateCalculator();
        gate._inputSignal[0] = new double[]{value};
        var coupler = coupler(gate);
        assertThrows(IllegalArgumentException.class, () -> coupler.transferGateSignalsToLk(netlist));
        assertArrayEquals(original, netlist.getParameter(0));
        assertThrows(IllegalArgumentException.class, () -> coupler.applyGateSignals(netlist));
        assertArrayEquals(original, netlist.getParameter(0));
    }

    @Test
    void absentMappingsUnknownTargetsAndUnsupportedDevicesLeaveStateUnchanged() {
        var netlist = device(CircuitTypCore.LK_R, new double[]{10});
        var coupler = new DomainCoupler();
        assertFalse(coupler.applyGateSignals(netlist));
        assertFalse(coupler.applyGateSignals(null));
        var gate = new GateCalculator();
        gate._inputSignal[0] = new double[]{1};
        var mapping = new ControlBlockMapping();
        mapping.addGateMapping(gate, "missing");
        mapping.addGateMapping(gate, "device");
        coupler.configureFromMapping(mapping);
        assertFalse(coupler.applyGateSignals(null));
        coupler.saveInitialSwitchParameters(netlist);
        coupler.transferGateSignalsToLk(null);
        coupler.transferGateSignalsToLk(netlist);
        assertFalse(coupler.applyGateSignals(netlist));
        assertArrayEquals(new double[]{10}, netlist.getParameter(0));
        coupler.configureFromMapping(null);
        coupler.transferGateSignalsToLk(netlist);
        assertFalse(coupler.applyGateSignals(netlist));
    }

    @ParameterizedTest
    @EnumSource(value = CircuitTypCore.class, names = {"LK_S", "LK_MOSFET", "LK_THYR", "LK_IGBT"})
    void incompleteDeviceParametersAreIgnoredWithoutMutation(CircuitTypCore type) {
        for (double[] values : new double[][]{null, new double[0], new double[]{1000}}) {
            var netlist = device(type, values);
            var gate = new GateCalculator();
            gate._inputSignal[0] = new double[]{1};
            var coupler = coupler(gate);
            assertFalse(coupler.applyGateSignals(netlist));
            coupler.transferGateSignalsToLk(netlist);
            assertArrayEquals(values, netlist.getParameter(0));
        }
    }

    @Test
    void aMappingWithoutAnInputPortIsDisconnected() {
        var mapping = new ControlBlockMapping();
        mapping.addGateMapping(new gecko.core.control.calculators.NothingToDoCalculator(0, 0), "device");
        var coupler = new DomainCoupler();
        coupler.configureFromMapping(mapping);
        var netlist = device(CircuitTypCore.LK_S, parameters(CircuitTypCore.LK_S));
        coupler.transferGateSignalsToLk(netlist);
        assertEquals(0, netlist.getParameter(0)[8]);
        assertFalse(coupler.applyGateSignals(netlist));
    }

}
