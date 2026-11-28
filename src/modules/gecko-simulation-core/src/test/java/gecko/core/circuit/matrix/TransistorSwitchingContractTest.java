package gecko.core.circuit.matrix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class TransistorSwitchingContractTest {
    @ParameterizedTest
    @CsvSource({"0,3,false", "1,3,true", "1,-3,false", "0,-3,false", "1,0,false"})
    void igbtRequiresBothGateAndForwardBias(double gate, double voltage, boolean expected) {
        IGBTStamper stamper = new IGBTStamper(0.25, 1e6, 1.5);
        stamper.updateStateWithGate(gate, voltage, 0);
        assertEquals(expected, stamper.isOn());
        assertEquals(expected, stamper.isStateChanged());
        assertEquals(expected ? 0.25 : 1e6, stamper.getCurrentResistance());
    }

    @Test
    void igbtTurnsOffImmediatelyWithGateRemovalAndCanRestart() {
        IGBTStamper stamper = new IGBTStamper(0.25, 1e6, 1.5);
        stamper.updateStateWithGate(1, 5, 2);
        assertTrue(stamper.isOn());
        assertEquals(6, stamper.calculateCurrent(5, 2, null, 1e-6, 0));
        stamper.resetStateChange();
        assertFalse(stamper.isStateChanged());
        stamper.updateStateWithGate(1, 5, 2);
        assertFalse(stamper.isStateChanged(), "Repeated gate and voltage do not request re-factorization");
        stamper.updateStateWithGate(0, 5, 2);
        assertFalse(stamper.isOn());
        assertTrue(stamper.isStateChanged());
        assertEquals(3e-6, stamper.calculateCurrent(5, 2, null, 1e-6, 0));
        stamper.updateStateWithGate(1, 5, 2);
        assertTrue(stamper.isOn());
    }

    @Test
    void igbtVoltageIterationTurnsOffReverseConductionWithoutFiringAnOffDevice() {
        IGBTStamper stamper = new IGBTStamper();
        stamper.updateState(10, 0, 0, 0);
        assertFalse(stamper.isOn(), "Forward voltage alone cannot fire an IGBT");
        stamper.setGateSignal(1);
        assertFalse(stamper.isOn(), "Gate alone cannot bypass the forward voltage check");
        stamper.updateStateWithGate(1, 10, 0);
        stamper.updateState(10, 0, 1, 0);
        assertTrue(stamper.isOn());
        assertFalse(stamper.isStateChanged());
        stamper.updateState(-10, 0, -1, 0);
        assertFalse(stamper.isOn());
        assertTrue(stamper.isStateChanged());
        stamper.updateStateWithGate(1, 10, 0);
        stamper.setGateSignal(0);
        assertFalse(stamper.isOn());
    }

    @Test
    void igbtGateParametersAndShortArraysFollowThePublicContract() {
        IGBTStamper stamper = new IGBTStamper();
        stamper.updateStateFromParameters(null, 10, 0);
        stamper.updateStateFromParameters(new double[8], 10, 0);
        assertFalse(stamper.isOn());
        stamper.updateStateFromParameters(IGBTStamper.createDefaultParameters(1), 10, 0);
        assertTrue(stamper.isOn());
        stamper.updateStateFromParameters(IGBTStamper.createDefaultParameters(0), 10, 0);
        assertFalse(stamper.isOn());
    }

    @ParameterizedTest
    @CsvSource({"1,3,12", "1,-3,-12", "0,3,0.000003", "0,-3,-0.000003", "-1,-3,-12", "0.5,3,12"})
    void mosfetChannelIsBidirectionalAndControlledOnlyByGate(double gate, double voltage, double expected) {
        MOSFETStamper stamper = new MOSFETStamper(0.25, 1e6);
        stamper.setGateSignal(gate);
        assertEquals(gate != 0, stamper.isOn());
        assertEquals(expected, stamper.calculateCurrent(voltage, 0, null, 1e-6, 99), 1e-12);
        stamper.resetStateChange();
        stamper.updateState(-100, 100, -20, 1);
        assertEquals(gate != 0, stamper.isOn());
        assertFalse(stamper.isStateChanged());
        stamper.setGateSignal(gate);
        assertFalse(stamper.isStateChanged());
    }

    @Test
    void mosfetGateParametersCycleAndRejectNoPartialArray() {
        MOSFETStamper stamper = new MOSFETStamper();
        stamper.updateStateFromParameters(null);
        stamper.updateStateFromParameters(new double[8]);
        assertFalse(stamper.isOn());
        stamper.updateStateFromParameters(MOSFETStamper.createDefaultParameters(1));
        assertTrue(stamper.isOn());
        assertTrue(stamper.isStateChanged());
        stamper.updateStateFromParameters(MOSFETStamper.createDefaultParameters(0));
        assertFalse(stamper.isOn());
        assertTrue(stamper.isStateChanged());
        stamper.setState(false);
        assertFalse(stamper.isStateChanged());
        stamper.setState(true);
        assertTrue(stamper.isStateChanged());
    }
    @Test
    void assertedIgbtGateCannotHoldReverseConduction() {
        IGBTStamper stamper = new IGBTStamper();
        stamper.updateStateWithGate(1, 3, 0);
        assertTrue(stamper.isOn());
        stamper.updateStateWithGate(1, -3, 0);
        assertFalse(stamper.isOn());
        assertTrue(stamper.isStateChanged());
        stamper.updateStateWithGate(1, -3, 0);
        assertFalse(stamper.isStateChanged());
    }

}
