package gecko.core.circuit.matrix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class ThyristorSwitchingContractTest {
    @ParameterizedTest
    @CsvSource({"0,3,false", "1,3,true", "1,-3,false", "0,-3,false", "1,0,false"})
    void triggerRequiresGateAndForwardBias(double gate, double voltage, boolean expected) {
        ThyristorStamper stamper = new ThyristorStamper(0.25, 1e6, 1.5, 0.1);
        stamper.updateStateWithGate(gate, voltage, 0, 0);
        assertEquals(expected, stamper.isOn());
        assertEquals(expected, stamper.isStateChanged());
    }

    @Test
    void thyristorLatchesAfterGateRemovalAndVoltageOnlyCannotFireIt() {
        ThyristorStamper stamper = new ThyristorStamper(0.25, 1e6, 1.5, 0.1);
        stamper.updateState(3, 0, 0, 0);
        assertFalse(stamper.isOn());
        stamper.fireTrigger(3, 0, 0);
        assertTrue(stamper.isOn());
        stamper.resetStateChange();
        assertFalse(stamper.isStateChanged());
        stamper.updateStateWithGate(0, 3, 0, 10);
        assertTrue(stamper.isOn());
        assertFalse(stamper.isStateChanged());
        assertEquals(6, stamper.calculateCurrent(3, 0, null, 1e-6, 0));
        assertEquals(0.25, stamper.getCurrentResistance());
    }

    @Test
    void legacyRecoveryIntervalStartsAtFirstReverseBiasObservation() {
        ThyristorStamper stamper = new ThyristorStamper(0.25, 1e6, 1.5, 0.1);
        stamper.fireTrigger(3, 0, 0);
        stamper.updateStateWithGate(0, -3, 0, 1);
        assertTrue(stamper.isOn(), "Retain the legacy finite recovery interval");
        assertEquals(1, stamper.getLastSwitchOffTime());
        stamper.updateStateWithGate(0, -3, 0, 1.05);
        assertTrue(stamper.isOn());
        stamper.updateStateWithGate(0, -3, 0, 1.11);
        assertFalse(stamper.isOn());
        assertTrue(stamper.isStateChanged());
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    void coarseTimestepsMustNotRestartAnAlreadyElapsedRecoveryInterval(boolean gateMethod) {
        ThyristorStamper stamper = new ThyristorStamper(0.25, 1e6, 1.5, 0.1);
        stamper.fireTrigger(3, 0, 0);
        if (gateMethod) {
            stamper.updateStateWithGate(0, -3, 0, 1);
            stamper.updateStateWithGate(0, -3, 0, 2);
        } else {
            stamper.updateState(-3, 0, -1, 1);
            stamper.updateState(-3, 0, -1, 2);
        }
        assertFalse(stamper.isOn(), "A one-second step exceeds the 0.1-second recovery interval");
    }

    @Test
    void restoredForwardConductionCancelsThePriorRecoveryEpisode() {
        ThyristorStamper stamper = new ThyristorStamper(0.25, 1e6, 1.5, 0.1);
        stamper.fireTrigger(3, 0, 0);
        stamper.updateStateWithGate(0, -3, 0, 1);
        stamper.updateStateWithGate(0, 3, 0, 1.05);
        stamper.updateStateWithGate(0, -3, 0, 1.08);
        stamper.updateStateWithGate(0, -3, 0, 1.12);
        assertTrue(stamper.isOn(), "Only 0.04 seconds of the new recovery interval have elapsed");
        stamper.updateStateWithGate(0, -3, 0, 1.19);
        assertFalse(stamper.isOn());
    }

    @Test
    void restoredRecoveryHistoryBlocksPrematureRetriggering() {
        ThyristorStamper stamper = new ThyristorStamper();
        double[] parameters = ThyristorStamper.createParameters(1e6, 1.5, 0.25, 1e6, 1, 0.1, 2);
        stamper.updateStateFromParameters(null, 3, 0, 2);
        stamper.updateStateFromParameters(new double[8], 3, 0, 2);
        stamper.updateStateFromParameters(parameters, 3, 0, 2.05);
        assertFalse(stamper.isOn());
        stamper.updateStateFromParameters(parameters, 3, 0, 2.2);
        assertTrue(stamper.isOn());
    }

    @Test
    void zeroRecoveryTimeAllowsImmediateTurnOffAndManualRestart() {
        ThyristorStamper stamper = new ThyristorStamper();
        stamper.setRecoveryTime(-1);
        assertEquals(0, stamper.getRecoveryTime());
        stamper.fireTrigger(3, 0, 0);
        stamper.updateState(-3, 0, -1, 1);
        assertFalse(stamper.isOn());
        stamper.setLastSwitchOffTime(Double.NEGATIVE_INFINITY);
        stamper.setState(true);
        assertTrue(stamper.isStateChanged());
        stamper.setState(true);
        assertFalse(stamper.isStateChanged());
        stamper.setState(false);
        assertTrue(stamper.isStateChanged());
    }
}
