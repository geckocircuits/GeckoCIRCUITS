package gecko.core.simulation;

import static org.junit.jupiter.api.Assertions.*;

import gecko.core.control.IsDtChangeSensitive;
import gecko.core.control.calculators.AbstractControlCalculatable;
import gecko.core.control.calculators.DelayCalculator;
import gecko.core.control.calculators.TimeCalculator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The step lifecycle of a netlist with two sample-clock groups. The runtime stages a step change before it runs the
 * netlist, so these tests call the netlist directly. A step that differs in one group only changes that group.
 */
class ControlNetlistGroupLifecycleTest {
    private static final double FINE = 1e-6;
    private static final double COARSE = 2e-6;
    private static final double DELAY = 20e-6;

    /** A step-sensitive calculator that records what the netlist asks of it. */
    private static final class Probe extends AbstractControlCalculatable implements IsDtChangeSensitive {
        final List<Double> newSteps = new ArrayList<>();
        final List<Double> stagedSteps = new ArrayList<>();
        final List<Double> executedSteps = new ArrayList<>();

        Probe() { super(0, 1); }

        @Override public void berechneYOUT(double deltaT) { executedSteps.add(deltaT); }
        @Override public void initWithNewDt(double dt) { newSteps.add(dt); }
        @Override public Runnable prepareTimeStepChange(double dt) {
            stagedSteps.add(dt);
            return () -> newSteps.add(dt);
        }
    }

    /** One group: a clock, a delay fed by it, and a probe. */
    private record Group(TimeCalculator clock, DelayCalculator delay, Probe probe) {
        static Group create() throws Exception {
            var group = new Group(new TimeCalculator(), new DelayCalculator(DELAY), new Probe());
            group.delay.setInputSignal(0, group.clock, 0);
            return group;
        }
    }

    private static ControlNetlist netlist(Group first, Group second) {
        var calculators = new ArrayList<AbstractControlCalculatable>(List.of(first.clock, first.delay, first.probe,
                second.clock, second.delay, second.probe));
        var assignment = new IdentityHashMap<AbstractControlCalculatable, Integer>();
        for (var calculator : calculators) assignment.put(calculator, calculators.indexOf(calculator) < 3 ? 0 : 1);
        var netlist = ControlNetlist.fromOrderedCalculators(calculators);
        netlist.configureGroups(assignment, 2);
        return netlist;
    }

    private static int stateValues(DelayCalculator delay) {
        int[] count = {0};
        delay.exportState(new gecko.core.control.StateSink() {
            @Override public void value(double value) { count[0]++; }
            @Override public void flag(boolean value) { count[0]++; }
        });
        return count[0];
    }

    private static void run(ControlNetlist netlist, double firstStep, double secondStep, int from, int to) {
        for (int index = from; index < to; index++) {
            netlist.executeDue(new double[]{firstStep, secondStep}, new boolean[]{true, true}, index * FINE);
        }
    }

    @Test
    void aStepThatChangesInOneGroupOnlyChangesThatGroup() throws Exception {
        var changed = Group.create();
        var changedOther = Group.create();
        var reference = Group.create();
        var referenceOther = Group.create();
        var withChange = netlist(changed, changedOther);
        var withoutChange = netlist(reference, referenceOther);
        withChange.prepareInitialization(new double[]{FINE, FINE}).run();
        withoutChange.prepareInitialization(new double[]{FINE, FINE}).run();
        run(withChange, FINE, FINE, 0, 40);
        run(withoutChange, FINE, FINE, 0, 40);
        int stateBefore = stateValues(changedOther.delay);
        assertEquals(stateBefore, stateValues(referenceOther.delay));

        // Only the second group's controller step changes. Both groups are due.
        for (int index = 40; index < 80; index++) {
            withChange.executeDue(new double[]{FINE, COARSE}, new boolean[]{true, true}, index * FINE);
            withoutChange.executeDue(new double[]{FINE, FINE}, new boolean[]{true, true}, index * FINE);
            assertEquals(Double.doubleToRawLongBits(reference.delay._outputSignal[0][0]),
                    Double.doubleToRawLongBits(changed.delay._outputSignal[0][0]), "the unchanged group keeps its history " + index);
        }

        assertEquals(List.of(), changed.probe.newSteps, "no callback for the unchanged group");
        assertEquals(List.of(COARSE), changedOther.probe.newSteps, "one callback, with the new step of its own group");
        assertEquals(stateValues(reference.delay), stateValues(changed.delay), "the unchanged delay is not resampled");
        assertTrue(stateValues(changedOther.delay) < stateBefore, "the changed delay is resampled to fewer samples");
        assertEquals(stateValues(referenceOther.delay), stateBefore);
        assertEquals(Set.of(FINE), new HashSet<>(changed.probe.executedSteps));
        assertEquals(Set.of(FINE, COARSE), new HashSet<>(changedOther.probe.executedSteps),
                "each calculator sees the step of its own group");
        assertEquals(COARSE, changedOther.probe.executedSteps.get(changedOther.probe.executedSteps.size() - 1));
    }

    @Test
    void aStagedStepChangeStagesOnlyTheSelectedGroups() throws Exception {
        var first = Group.create();
        var second = Group.create();
        var netlist = netlist(first, second);
        netlist.prepareInitialization(new double[]{FINE, FINE}).run();
        run(netlist, FINE, FINE, 0, 30);
        int firstState = stateValues(first.delay);
        int secondState = stateValues(second.delay);

        var commit = netlist.prepareTimeStepChange(new double[]{COARSE, COARSE}, new boolean[]{false, true});
        assertEquals(List.of(), first.probe.stagedSteps);
        assertEquals(List.of(COARSE), second.probe.stagedSteps);
        commit.run();

        assertEquals(List.of(), first.probe.newSteps);
        assertEquals(List.of(COARSE), second.probe.newSteps);
        assertEquals(firstState, stateValues(first.delay), "the unselected delay keeps its history");
        assertTrue(stateValues(second.delay) < secondState, "the selected delay is resampled");
    }

    @Test
    void oneStepPerGroupIsRequiredBeforeAnyCallbackOrStateChange() throws Exception {
        var first = Group.create();
        var second = Group.create();
        var netlist = netlist(first, second);
        for (double[] wrong : new double[][]{{FINE}, {FINE, FINE, FINE}}) {
            assertThrows(IllegalArgumentException.class, () -> netlist.prepareInitialization(wrong));
            assertThrows(IllegalArgumentException.class, () -> netlist.prepareTimeStepChange(wrong, null));
            assertThrows(IllegalArgumentException.class, () -> netlist.executeDue(wrong, new boolean[]{true, true}, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> netlist.prepareInitialization(new double[]{FINE, Double.NaN}));
        assertEquals(List.of(), first.probe.newSteps);
        assertEquals(List.of(), second.probe.newSteps);
        assertEquals(List.of(), first.probe.executedSteps);
        assertEquals(List.of(), second.probe.executedSteps);

        // Nothing changed, so a valid call still works: it initializes both groups.
        netlist.prepareInitialization(new double[]{FINE, COARSE}).run();
        run(netlist, FINE, COARSE, 0, 3);
        assertEquals(List.of(FINE, FINE, FINE), first.probe.executedSteps);
        assertEquals(List.of(COARSE, COARSE, COARSE), second.probe.executedSteps);
        assertTrue(stateValues(first.delay) > stateValues(second.delay), "each delay uses the step of its own group");
    }
}
