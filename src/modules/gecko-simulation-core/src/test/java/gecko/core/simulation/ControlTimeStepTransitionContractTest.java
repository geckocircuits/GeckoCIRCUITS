package gecko.core.simulation;

import gecko.core.control.IsDtChangeSensitive;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.DelayCalculator;
import gecko.core.control.calculators.TimeCalculator;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ControlTimeStepTransitionContractTest {
    @Test
    void preparationLeavesHistoryAndClockUnchangedUntilCommit() throws Exception {
        var fixture = ramp();
        Runnable transition = fixture.netlist.prepareTimeStepChange(0.25);
        assertEquals(1, fixture.netlist.getSimulationTime());
        assertEquals(0, fixture.delay._outputSignal[0][0]);
        transition.run();
        assertEquals(1, fixture.netlist.getSimulationTime());
        fixture.netlist.executeTimeStep(0.25, 1.25);
        assertEquals(0.25, fixture.delay._outputSignal[0][0], 1e-12);
    }

    @Test
    void advancingTheOldGridMakesThePreparedTransitionStale() throws Exception {
        var fixture = ramp();
        Runnable transition = fixture.netlist.prepareTimeStepChange(0.25);
        fixture.netlist.executeTimeStep(0.5, 1.5);
        assertEquals(0.5, fixture.delay._outputSignal[0][0], 1e-12);
        assertThrows(IllegalStateException.class, transition::run);
        fixture.netlist.executeTimeStep(0.5, 2);
        assertEquals(1, fixture.delay._outputSignal[0][0], 1e-12);
    }

    @Test
    void aCommittedTransitionCannotChangeTheHistoryAgain() throws Exception {
        var fixture = ramp();
        Runnable transition = fixture.netlist.prepareTimeStepChange(0.25);
        transition.run();
        assertThrows(IllegalStateException.class, transition::run);
        fixture.netlist.executeTimeStep(0.25, 1.25);
        assertEquals(0.25, fixture.delay._outputSignal[0][0], 1e-12);
    }

    @Test
    void legacyCallbacksRemainAvailableOutsideTheTransactionalSeam() {
        var calculator = new LegacySensitiveCalculator();
        var netlist = ControlNetlist.fromOrderedCalculators(List.of(calculator));
        netlist.initializeAtSimulationStart(0.5);
        assertThrows(UnsupportedOperationException.class, () -> netlist.prepareTimeStepChange(0.25));
        assertEquals(0, calculator.callbacks);
        netlist.executeTimeStep(0.25, 0.25);
        assertEquals(1, calculator.callbacks);
    }

    private static Fixture ramp() throws Exception {
        var time = new TimeCalculator();
        var delay = new DelayCalculator(1);
        delay.setInputSignal(0, time, 0);
        var netlist = ControlNetlist.fromOrderedCalculators(List.of(time, delay));
        netlist.initializeAtSimulationStart(0.5);
        netlist.executeTimeStep(0.5, 0);
        netlist.executeTimeStep(0.5, 0.5);
        netlist.executeTimeStep(0.5, 1);
        return new Fixture(netlist, delay);
    }

    private record Fixture(ControlNetlist netlist, DelayCalculator delay) {}

    private static final class LegacySensitiveCalculator extends AbstractControlCalculatable
            implements IsDtChangeSensitive {
        private int callbacks;
        LegacySensitiveCalculator() { super(0, 0); }
        @Override public void initWithNewDt(double dt) { callbacks++; }
        @Override public void berechneYOUT(double dt) {}
    }
}
