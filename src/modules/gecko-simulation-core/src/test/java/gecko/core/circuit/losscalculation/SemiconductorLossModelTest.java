package gecko.core.circuit.losscalculation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SemiconductorLossModelTest {
    private static final double DT = 1e-6;

    @Test
    void conductionLossIsForwardDropPlusResistiveLoss() {
        var model = SemiconductorLossModel.simple(0.6, 0.3, 3e-5, 1.5e-5, 600);
        model.update(2.0, 10, 25, DT);
        // |2 A * 0.6 V| + 4 A^2 * 0.3 Ohm. The first step from the -1 history is neither a turn-on nor a turn-off.
        assertEquals(1.2 + 1.2, model.conduction(), 1e-12);
        assertEquals(0, model.switching(), 0);
        assertEquals(2.4, model.total(), 1e-12);
    }

    @Test
    void turnOnUsesTheBlockingVoltageOfTheStepBeforeAndTurnOffUsesTheVoltageOfThisStep() {
        var model = SemiconductorLossModel.simple(0, 0, 3e-5, 1.5e-5, 600);
        model.update(0, 300, 25, DT); // old history -1 turns this first sample into a turn-off edge
        model.update(0, 300, 25, DT);
        model.update(4.0, 150, 25, DT);
        // Turn-on: |kON i / dt| * |oldVoltage / uK| with the voltage of the step before.
        assertEquals(Math.abs(3e-5 * 4.0 / DT) * Math.abs(300.0 / 600), model.switching(), 1e-9);
        model.update(4.0, 150, 25, DT);
        assertEquals(0, model.switching(), 0);
        model.update(0.0, 450, 25, DT);
        // Turn-off: |kOFF i_old / dt| * |voltage / uK| with this step's voltage.
        assertEquals(Math.abs(1.5e-5 * 4.0 / DT) * Math.abs(450.0 / 600), model.switching(), 1e-9);
    }

    @Test
    void theFirstStepFromTheInitialHistoryCountsAsTurnOffWhenTheCurrentIsZero() {
        var model = SemiconductorLossModel.simple(0, 0, 3e-5, 1.5e-5, 600);
        model.update(0, 300, 25, DT);
        assertEquals(Math.abs(1.5e-5 * -1.0 / DT) * Math.abs(300.0 / 600), model.switching(), 1e-9);
    }

    @Test
    void currentsBelowTheThresholdDoNotSwitch() {
        var model = SemiconductorLossModel.simple(0, 0, 3e-5, 1.5e-5, 600);
        model.update(0, 0, 25, DT);
        model.update(0.005, 100, 25, DT);
        assertEquals(0, model.switching(), 0);
    }

    @Test
    void aResistorDissipatesCurrentTimesVoltage() {
        var model = SemiconductorLossModel.resistive();
        model.update(2.0, 10.0, 25, DT);
        assertEquals(20, model.total(), 0);
        assertEquals(0, model.switching(), 0);
    }

    @Test
    void aDetailedFileThatHasNoCurvesIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> DetailedLossCurves.parse("no curves".getBytes()));
    }
}
